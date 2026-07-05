import asyncio
import threading
import tkinter as tk
from tkinter import ttk
from winsdk.windows.media.control import GlobalSystemMediaTransportControlsSessionManager

class MediaControllerApp:
    def __init__(self, root):
        self.root = root
        self.root.title("网易云进度测试")
        self.root.geometry("400x120")
        self.root.attributes("-topmost", True)  # 让窗口总在最前方便测试

        self.current_session = None
        self.total_duration_seconds = 0
        self.is_dragging = False  # 标记是否正在拖动进度条

        # UI 组件
        self.label_info = tk.Label(root, text="正在检测播放源...", font=("Microsoft YaHei", 10))
        self.label_info.pack(pady=5)

        # 进度条 (Scale)
        self.slider = ttk.Scale(root, from_=0, to=100, orient="horizontal", length=350)
        self.slider.pack(pady=5)
        
        # 绑定鼠标事件：按下时暂停刷新，松开时跳转时间
        self.slider.bind("<ButtonPress-1>", self.on_slider_press)
        self.slider.bind("<ButtonRelease-1>", self.on_slider_release)

        # 启动后台异步任务来获取媒体会话和刷新进度
        self.loop = asyncio.new_event_loop()
        threading.Thread(target=self.start_async_loop, daemon=True).start()
        
        # 启动定时刷新 UI
        self.root.after(1000, self.update_ui_loop)

    def start_async_loop(self):
        asyncio.set_event_loop(self.loop)
        self.loop.run_until_complete(self.init_media_session())
        self.loop.run_forever()

    async def init_media_session(self):
        """初始化 Windows 媒体控制会话"""
        manager = await GlobalSystemMediaTransportControlsSessionManager.request_async()
        self.current_session = manager.get_current_session()
        
        # 监听当前会话改变（比如网易云关了又开）
        manager.add_current_session_changed(lambda s, e: self.on_session_changed(s))

    def on_session_changed(self, manager):
        self.current_session = manager.get_current_session()

    def on_slider_press(self, event):
        self.is_dragging = True

    def on_slider_release(self, event):
        """鼠标松开时，计算绝对时间并发送跳转指令"""
        if self.current_session and self.total_duration_seconds > 0:
            slider_value = self.slider.get()
            # 计算目标秒数
            target_seconds = (slider_value / 100.0) * self.total_duration_seconds
            
            # 异步发送跳转请求
            asyncio.run_coroutine_threadsafe(self.jump_to_position(target_seconds), self.loop)
        
        # 延迟恢复刷新，防止网易云进度未更新导致进度条闪烁
        self.root.after(500, self.reset_dragging_flag)

    def reset_dragging_flag(self):
        self.is_dragging = False

    async def jump_to_position(self, seconds):
        """调用 Windows API 跳转到指定绝对时间"""
        import datetime
        try:
            # Windows API 需要 TimeSpan 格式，Python 中用 datetime.timedelta 表示
            time_span = datetime.timedelta(seconds=seconds)
            await self.current_session.try_change_playback_position_async(time_span.ticks)
        except Exception as e:
            print(f"跳转失败: {e}")

    def update_ui_loop(self):
        """定时从网易云同步当前的播放进度"""
        if self.current_session and not self.is_dragging:
            try:
                # 获取歌曲标题和时间属性
                info = self.current_session.get_now_playing_properties()
                timeline = self.current_session.get_timeline_properties()
                
                title = info.title if info.title else "未知歌曲"
                
                # 转换时间（Windows 的 Duration 单位是 100 纳秒，需要除以 10,000,000 得到秒）
                total_seconds = timeline.end_time.duration / 10000000
                current_seconds = timeline.position.duration / 10000000
                
                self.total_duration_seconds = total_seconds
                
                if total_seconds > 0:
                    self.label_info.config(text=f"正在播放: {title}")
                    # 计算百分比并更新进度条
                    percent = (current_seconds / total_seconds) * 100
                    self.slider.set(percent)
            except Exception:
                self.label_info.config(text="未能获取到播放信息")
        elif not self.current_session:
            self.label_info.config(text="未检测到网易云音乐在播放")

        # 每 500 毫秒刷新一次进度
        self.root.after(500, self.update_ui_loop)

if __name__ == "__main__":
    root = tk.Tk()
    app = MediaControllerApp(root)
    root.mainloop()