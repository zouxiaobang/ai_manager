# PC 后台守护进程

Python 守护进程负责接收 ESP32-S3 的 WebSocket 连接，启动桌面版网易云音乐，执行播放控制，并向副屏推送播放状态和歌词。

## 程序文件

- `main.py`: 启动入口，加载配置并启动 WebSocket 服务；可用 `--monitor` 在终端监听网易云播放进度与歌词。
- `ai_manager_daemon/server.py`: FastAPI 应用和 WebSocket 路由，负责处理 ESP32 消息。
- `ai_manager_daemon/config.py`: 读取 `config.yaml`，没有该文件时会回退到 `config.example.yaml`。
- `ai_manager_daemon/protocol.py`: 协议消息解析、响应生成和时间戳工具。
- `ai_manager_daemon/desktop_app_control.py`: 检查并启动桌面版网易云音乐进程。
- `ai_manager_daemon/cloudmusic.py`: 网易云音乐适配层，使用系统媒体热键控制，通过 Windows 媒体会话读取当前歌曲，并从网易云接口拉取 LRC 歌词。
- `config.example.yaml`: 配置模板。
- `requirements.txt`: Python 依赖。

## 安装

```powershell
cd pc_daemon
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item config.example.yaml config.yaml
python main.py
```

本地调试播放进度与歌词（不启动 WebSocket）：

```powershell
python main.py --monitor
```

服务启动后默认监听：

```text
ws://0.0.0.0:8765/ws
http://0.0.0.0:8765/health
```

再次启动时只需要：

```powershell
cd pc_daemon
.\.venv\Scripts\Activate.ps1
python main.py
```

## 后台运行与开机自启

日志默认写入：

```text
pc_daemon/.runtime/logs/daemon.log
```

单文件最大 5MB，保留最近 3 个历史文件。

### 后台启动（无窗口）

```powershell
cd pc_daemon
powershell -ExecutionPolicy Bypass -File .\scripts\start_background.ps1
```

### 停止后台进程

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop_background.ps1
```

### 查看状态与最近日志

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\status.ps1
```

### 安装开机自启（登录后自动后台运行）

```powershell
cd pc_daemon
powershell -ExecutionPolicy Bypass -File .\scripts\install_autostart.ps1
```

取消开机自启：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\uninstall_autostart.ps1
```

前台调试仍可直接运行 `python main.py`，日志同样会写入上述文件。

## 配置

修改 `config.yaml`：

- `host`: 监听地址。局域网访问通常使用 `0.0.0.0`。
- `port`: WebSocket 端口。
- `desktop_app.executable`: 桌面版网易云音乐程序路径，例如 `C:\Program Files\NetEase\CloudMusic\cloudmusic.exe`。如果路径不准确，后台会再尝试常见安装目录。
- `desktop_app.process_name`: 桌面版网易云音乐进程名，通常是 `cloudmusic.exe`。
- `netease.playback_poll_interval_seconds`: 播放状态推送间隔（秒），建议 **1.0**；切歌、暂停会立即推送。
- `netease.lyric_poll_interval_seconds`: 歌词检查间隔（秒），建议 **0.5**；仅歌词换行时推送 `lyrics.line`。
- `netease.lyric_offset_ms`: 歌词时间校准。歌曲比歌词快时增大；歌词比歌曲快时减小或设为负数。
- `netease.progress_source`: 播放进度来源。`auto`（默认）优先用 CDP 读取客户端内部进度，失败时回退 `winsdk`；`cdp` 仅 CDP；`winsdk` 仅 Windows 媒体会话。
- `netease.cdp_port`: Chrome DevTools 调试端口，默认 `9222`。
- `netease.cdp_auto_launch_debug`: 为 `true` 时，由守护进程启动网易云会自动附加 `--remote-debugging-port`。
- `security.allowed_device_ids`: 允许连接的 ESP32 设备 ID。

## 网易云音乐控制

当前 `cloudmusic.py` 使用系统媒体热键作为基础控制方式。

- **`desktop_app.auto_launch_on_startup: true`（默认）**：运行 `python main.py` 时会自动检查并启动网易云；若配置了 CDP，会附带 `--remote-debugging-port`。
- ESP32 发送 `playback.start` 时也会再次确保网易云已运行。
- 若网易云**已在运行**且是旧进程（没有调试端口），守护进程**不会**自动杀进程重启，需你手动完全退出后重开。

歌词同步流程：

- PC 端推送前会将歌曲名、歌手和歌词统一转换为**简体中文**（`zhconv`）。
- ESP32 连接或重连后会自动发送 `playback.sync`，PC 端立即推送当前播放状态、进度和 5 行歌词；之后持续监听变化。
- 优先选择网易云桌面版的 Windows 媒体会话（避免误读浏览器等其他播放器）。
- 通过 `winsdk` 读取媒体会话，获取标题、歌手、专辑、播放状态。
- **拖动进度/最小化到托盘**：Windows 媒体会话的 `position` 在拖动时常常不更新，因此默认启用 **CDP 进度**（连接网易云 Electron 内部 `audioplayer.onPlayProgress`），最小化时也能跟歌词对齐。
- 首次启用 CDP：请**完全退出**网易云后，由守护进程重新拉起（或手动带参数启动：`cloudmusic.exe --remote-debugging-port=9222`）。若网易云已在运行且未带该参数，日志会提示需重启一次。
- CDP 不可用时回退：`winsdk` 时间轴 + 本地时钟估算，并从网易云 API 拉取歌曲时长。
- 优先使用 CDP 提供的**桌面端歌曲 ID** 拉取歌词（避免搜到同名歌）；无 ID 时用标题+歌手搜索，并校验 Live/伴奏等标签与时长。
- 拖动进度条时：CDP 检测 seek 并立即重推 `lyrics.line`（约 0.2s 轮询）。
- 根据当前播放进度向 ESP32 推送 5 行歌词和播放进度。

如果屏幕显示“未找到该歌曲的时间轴歌词”，通常是网易云搜索没有匹配到同一首歌，或该歌曲没有可用 LRC。
