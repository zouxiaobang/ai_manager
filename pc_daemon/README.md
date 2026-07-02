# NetEase Media Bridge 安装与自启动

PC 端守护进程监听 `8765` 端口，为 ESP32 副屏提供网易云歌词与播放控制。

## 1. 安装依赖

```powershell
cd pc_daemon
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## 2. 手动启动

```powershell
.\scripts\run_daemon.ps1
```

## 3. 开机自启动（推荐）

以**管理员身份**运行：

```powershell
.\scripts\install_autostart.ps1
```

会在「任务计划程序」创建 `NetEaseMediaBridge` 任务，用户登录后自动启动守护进程。

## 4. 防火墙

首次运行若被拦截，请允许 Python 在专用网络（局域网）入站 `8765/TCP`。

## 5. 验证

```powershell
curl http://127.0.0.1:8765/health
curl http://127.0.0.1:8765/api/media/status
```
