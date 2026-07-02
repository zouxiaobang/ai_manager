# 网易云歌词同步说明

ESP32 副屏通过局域网 HTTP 与 PC 守护进程 `pc_daemon` 同步**本机**网易云音乐（默认 PC：`192.168.0.114:8765`）。

## 已实现行为

1. **首页歌词卡片**与**全屏歌词**均从 PC 实时拉取当前行歌词（播放中约 800ms 刷新，空闲约 2s）。
2. **ESP 启动时**：若 PC 网易云已打开（播放或暂停），自动同步歌名、歌手、进度与当前歌词行。
3. **PC 未开网易云**：全屏播放按钮显示「播放」；点击后提示「正在启动网易云音乐…」，并通知 PC 启动客户端；开始播放后按钮变为「暂停」，歌词与进度同步。
4. **上一首 / 下一首 / 暂停**：通过 Windows 系统媒体会话（SMTC）控制桌面版网易云。

## 配置

| 位置 | 说明 |
|------|------|
| TF 卡 `config/media_host.txt` | 第 1 行 PC IP（如 `192.168.0.114`），第 2 行端口（默认 `8765`） |
| menuconfig `NetEase Media Sync` | 固件内置默认 IP/端口 |
| `pc_daemon` 环境变量 | `MEDIA_DAEMON_HOST` / `MEDIA_DAEMON_PORT` |

PC 端需先安装并启动守护进程，见 `pc_daemon/README.md`。

## 还需注意的细节

1. **同一局域网**：ESP 与 PC 须在同一 WiFi；ESP 不能使用 `127.0.0.1`（那是设备自身）。
2. **PC 防火墙**：放行 Python/守护进程入站 **TCP 8765**（专用网络/局域网）。
3. **PC IP 稳定**：建议路由器对 `192.168.0.114` 做 DHCP 静态绑定，或 SD 卡改 `media_host.txt` 免重烧录。
4. **仅本机网易云**：守护进程只读取/控制**运行守护进程这台 PC**上的网易云，不会控制其他设备。
5. **歌词来源**：PC 根据 SMTC 曲名+歌手搜索网易云 LRC；无版权或纯音乐可能只显示歌名。
6. **进度对齐**：歌词行按 `position_ms` 匹配 LRC 时间轴；网络延迟约 0.5–1s，属正常范围。
7. **切歌**：换歌后 PC 会重新拉取 LRC，ESP 下一次轮询更新标题与歌词。
8. **暂停**：暂停时仍同步当前歌词行与按钮状态；PC 进程在但无播放会话时显示「播放」。
9. **WiFi 断开**：ESP 回退 SD 卡静态歌词占位，恢复联网后自动再同步。
10. **番茄钟共用 WiFi**：媒体同步依赖 `POMO_SYNC_ENABLE` 与相同 WiFi 配置；媒体端口与番茄钟 HTTP 端口相互独立。
11. **网易云路径**：PC 需安装桌面版；常见路径 `%LOCALAPPDATA%\Netease\CloudMusic\cloudmusic.exe`。
12. **开机自启**：PC 运行 `pc_daemon/scripts/install_autostart.ps1` 注册计划任务。

## 验证

```powershell
# PC
curl http://127.0.0.1:8765/api/media/status

# ESP 串口
# media_sync: Media sync with http://192.168.0.114:8765
```
