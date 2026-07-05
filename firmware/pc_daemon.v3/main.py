"""网易云音乐 SMTC 桌面控制器 v3 —— Windows EXE 应用。"""

from __future__ import annotations

import logging
import os
import sys
import threading
import tkinter as tk
from tkinter import ttk

from smtc_bridge import SMTCBridge, PlaybackState
from seek_server import SeekServer

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
logger = logging.getLogger("main")

# ── 常量 ────────────────────────────────────────────────────
POLL_INTERVAL_MS = 300   # GUI 轮询间隔（毫秒）
SEEK_DEBOUNCE_MS = 200   # 拖动停止后等待防抖再发 seek（毫秒）
SEEK_HOLD_LOCK_MS = 1500 # seek 后锁定进度条不被轮询覆盖（毫秒）


def _fmt_time(ms: int) -> str:
    """毫秒 -> mm:ss"""
    if ms <= 0:
        return "0:00"
    total_sec = ms // 1000
    minute = total_sec // 60
    second = total_sec % 60
    return f"{minute}:{second:02d}"


class App(tk.Tk):
    def __init__(self) -> None:
        super().__init__()
        self.title("网易云音乐控制器")
        self.resizable(False, False)

        # 窗口图标（可选）
        try:
            self.iconbitmap(default=self._icon_path())
        except Exception:
            pass

        # ── 数据 ──
        self._bridge = SMTCBridge(poll_interval=0.3)
        self._state = PlaybackState()
        self._seek_server = SeekServer()
        self._hold_slider = False    # 锁定进度条不被轮询覆盖（拖动中 + seek 后冷却期）
        self._last_seek_ms = 0
        self._debounce_timer_id: str | None = None  # seek 防抖定时器
        self._hold_timer_id: str | None = None      # seek 后锁定定时器

        # 启动 HTTP 服务器（BetterNCM 通信）
        self._seek_server.start()

        # ── 界面 ──
        self._build_ui()
        self._poll_loop()

    # ── UI 构建 ──────────────────────────────────────────────

    def _build_ui(self) -> None:
        # 窗口置顶与关闭行为
        self.protocol("WM_DELETE_WINDOW", self._on_close)

        # 主容器
        main = ttk.Frame(self, padding=16)
        main.pack(fill=tk.BOTH, expand=True)

        # ── 歌曲信息 ──
        self._title_var = tk.StringVar(value="未检测到网易云音乐")
        self._artist_var = tk.StringVar(value="")
        title_lbl = ttk.Label(
            main, textvariable=self._title_var,
            font=("Segoe UI", 11, "bold"), wraplength=300,
        )
        title_lbl.pack(anchor=tk.W, pady=(0, 2))
        artist_lbl = ttk.Label(
            main, textvariable=self._artist_var,
            font=("Segoe UI", 9), foreground="#666",
        )
        artist_lbl.pack(anchor=tk.W, pady=(0, 10))

        # ── 进度条 ──
        progress_frame = ttk.Frame(main)
        progress_frame.pack(fill=tk.X, pady=(0, 4))

        self._time_cur_var = tk.StringVar(value="0:00")
        self._time_end_var = tk.StringVar(value="0:00")
        self._progress = ttk.Scale(
            progress_frame,
            from_=0, to=1000,
            orient=tk.HORIZONTAL,
            command=self._on_progress_drag,
        )
        self._progress.pack(fill=tk.X, side=tk.TOP, pady=(0, 2))
        self._progress.bind("<ButtonRelease-1>", self._on_progress_release)

        time_row = ttk.Frame(progress_frame)
        time_row.pack(fill=tk.X)
        ttk.Label(time_row, textvariable=self._time_cur_var,
                  font=("Segoe UI", 8)).pack(side=tk.LEFT)
        ttk.Label(time_row, textvariable=self._time_end_var,
                  font=("Segoe UI", 8)).pack(side=tk.RIGHT)

        # ── 控制按钮 ──
        btn_frame = ttk.Frame(main)
        btn_frame.pack(pady=(6, 0))

        self._prev_btn = ttk.Button(
            btn_frame, text="⏮ 上一首", width=10,
            command=self._on_prev,
        )
        self._prev_btn.pack(side=tk.LEFT, padx=(0, 6))

        self._play_btn = ttk.Button(
            btn_frame, text="▶ 播放", width=10,
            command=self._on_toggle,
        )
        self._play_btn.pack(side=tk.LEFT, padx=6)

        self._next_btn = ttk.Button(
            btn_frame, text="⏭ 下一首", width=10,
            command=self._on_next,
        )
        self._next_btn.pack(side=tk.LEFT, padx=(6, 0))

        # ── 状态栏 ──
        self._status_var = tk.StringVar(value="等待 SMTC 连接 ...")
        status_bar = ttk.Label(
            self, textvariable=self._status_var,
            font=("Segoe UI", 8), foreground="#999",
            anchor=tk.W, padding=(16, 2),
        )
        status_bar.pack(fill=tk.X, side=tk.BOTTOM)

    # ── 事件处理 ──────────────────────────────────────────────

    def _on_progress_drag(self, value: str) -> None:
        """进度条拖动回调（每次值变化都会触发）。"""
        self._hold_slider = True
        # 取消之前的锁定定时器（seek 后冷却期不应阻止用户再次拖动）
        if self._hold_timer_id is not None:
            self.after_cancel(self._hold_timer_id)
            self._hold_timer_id = None

        ratio = float(value) / 1000.0
        dur_ms = self._state.duration_ms
        if dur_ms <= 0:
            return
        pos_ms = int(ratio * dur_ms)
        self._time_cur_var.set(_fmt_time(pos_ms))
        self._last_seek_ms = pos_ms

        # 防抖：取消之前的定时器，重新设定
        if self._debounce_timer_id is not None:
            self.after_cancel(self._debounce_timer_id)
        self._debounce_timer_id = self.after(SEEK_DEBOUNCE_MS, self._do_seek)

    def _on_progress_release(self, event: tk.Event | None = None) -> None:
        """进度条释放（鼠标松开）：取消防抖立即 seek。"""
        if self._debounce_timer_id is not None:
            self.after_cancel(self._debounce_timer_id)
            self._debounce_timer_id = None
        self._do_seek()

    def _do_seek(self) -> None:
        """发送 seek 命令，并锁定进度条一段时间避免被轮询覆盖。"""
        self._debounce_timer_id = None
        if not self._hold_slider:
            return
        pos_ms = self._last_seek_ms
        dur_ms = self._state.duration_ms
        if dur_ms <= 0:
            logger.warning("Seek skipped: duration=%d (SMTC data not ready)", dur_ms)
            self._release_slider_hold()
            return
        logger.info("Seeking to %d ms (duration=%d)", pos_ms, dur_ms)
        # 通过 BetterNCM 插件 seek（插件轮询 HTTP 接口获取命令）
        self._seek_server.set_pending_seek(pos_ms)

        # seek 后保留锁定，等待 SMTC 进度更新
        if self._hold_timer_id is not None:
            self.after_cancel(self._hold_timer_id)
        self._hold_timer_id = self.after(SEEK_HOLD_LOCK_MS, self._release_slider_hold)

    def _release_slider_hold(self) -> None:
        """冷却结束，允许轮询覆盖进度条。"""
        self._hold_timer_id = None
        self._hold_slider = False
        logger.debug("Slider hold released")

    def _on_toggle(self) -> None:
        threading.Thread(target=self._bridge.toggle, daemon=True).start()

    def _on_prev(self) -> None:
        threading.Thread(target=self._bridge.prev_track, daemon=True).start()

    def _on_next(self) -> None:
        threading.Thread(target=self._bridge.next_track, daemon=True).start()

    def _on_close(self) -> None:
        self._seek_server.stop()
        self.destroy()
        sys.exit(0)

    # ── 轮询 ──────────────────────────────────────────────────

    def _poll_loop(self) -> None:
        """定期从 SMTC 桥读取状态并刷新 UI。"""
        smtc = self._bridge.get_state()
        plugin = self._seek_server.get_plugin_state()

        # SMTC 有数据则用 SMTC（标题、艺人、播放状态），
        # 但位置/时长用插件（比 SMTC 更准确），互补使用
        if smtc.available:
            if plugin.get("available") and plugin.get("duration_ms", 0) > 0:
                smtc.position_ms = plugin["position_ms"]
                smtc.duration_ms = plugin["duration_ms"]
                smtc.playing = plugin["playing"]
            elif smtc.duration_ms <= 0:
                # SMTC 无 duration，插件也无 -> 保持现状
                pass
        elif plugin.get("available"):
            # SMTC 不可用但插件可用：用插件数据填充
            smtc.available = True
            smtc.position_ms = plugin["position_ms"]
            smtc.duration_ms = plugin["duration_ms"]
            smtc.playing = plugin["playing"]

        self._state = smtc
        self._update_ui()
        self.after(POLL_INTERVAL_MS, self._poll_loop)

    def _update_ui(self) -> None:
        state = self._state

        if not state.available:
            self._title_var.set("网易云音乐未运行")
            self._artist_var.set("请打开网易云音乐")
            self._status_var.set("SMTC 未检测到网易云音乐会话")
            self._play_btn.configure(text="▶ 播放")
            self._time_cur_var.set("0:00")
            self._time_end_var.set("0:00")
            if not self._hold_slider:
                self._progress.set(0)
            return

        # 标题 / 歌手
        if state.title:
            self._title_var.set(state.title)
            self._artist_var.set(f"{state.artist} · {state.album}" if state.album else state.artist)
        else:
            self._title_var.set("网易云音乐")
            self._artist_var.set("等待播放 ...")

        # 播放 / 暂停按钮文字
        self._play_btn.configure(text="⏸ 暂停" if state.playing else "▶ 播放")

        # 状态栏
        self._status_var.set(
            f"{'▶ 播放中' if state.playing else '⏸ 暂停'} · "
            f"{state.title or '无歌曲'}"
        )

        # 时间
        self._time_cur_var.set(_fmt_time(state.position_ms))
        self._time_end_var.set(_fmt_time(state.duration_ms))

        # 状态栏提示插件状态
        plugin = self._seek_server.get_plugin_state()
        plugin_ok = plugin.get("duration_ms", 0) > 0
        if state.available and not plugin_ok and state.duration_ms <= 0:
            self._status_var.set(
                f"{'▶ 播放中' if state.playing else '⏸ 暂停'} · "
                f"{state.title or '无歌曲'} "
                f"[等待插件连接...]"
            )

        # 进度条（锁定期间不覆盖，避免回弹）
        if not self._hold_slider and state.duration_ms > 0:
            ratio = state.position_ms / state.duration_ms
            self._progress.set(ratio * 1000.0)
        elif state.duration_ms <= 0:
            if not self._hold_slider:
                self._progress.set(0)

    # ── 辅助 ──────────────────────────────────────────────────

    @staticmethod
    def _icon_path() -> str:
        """返回 exe 同目录下的图标路径（如存在）。"""
        base = os.path.dirname(sys.executable if getattr(sys, 'frozen', False) else __file__)
        ico = os.path.join(base, "app.ico")
        return ico if os.path.isfile(ico) else ""


def main() -> None:
    app = App()
    app.mainloop()


if __name__ == "__main__":
    main()
