# 项目架构规划

## 总体拓扑

```text
ESP32-S3 副屏
  ├─ RGB 800x480 IPS 面板
  ├─ LVGL UI
  ├─ Wi-Fi
  └─ WebSocket 客户端
        │ 仅播放中保持连接
        ▼
Python PC Daemon
  ├─ WebSocket 服务端
  ├─ 网易云音乐控制适配层
  ├─ 桌面版网易云音乐启动
  └─ 歌词与播放状态推送
```

## 固件端分层

- `display`: ESP-IDF `esp_lcd` RGB 面板、时序和 LVGL 显示缓冲区。
- `touch`: GT911 I2C 触摸驱动和 LVGL pointer 输入设备。
- `ui`: LVGL 页面、歌词组件、播放状态组件。
- `transport`: WebSocket 生命周期、心跳、消息收发和重连。
- `app`: 播放态状态机，决定何时打开或关闭 WebSocket。

固件使用 ESP-IDF C++ 工程，入口为 `app_main()`，代码按 `display`、`touch`、`ui`、`wifi`、`websocket` 模块拆分。

## WebSocket 生命周期

- 播放开始：ESP32 连接 `ws://PC_IP:8765/ws`，发送 `hello` 和 `playback.start`。
- 播放中：PC 端推送 `playback.state`、`lyrics.line`、`lyrics.snapshot`；ESP32 可发送 `control.command`。
- 暂停：ESP32 先发送 `playback.pause`，等待 `ack` 后关闭 WebSocket。
- 关闭：ESP32 发送 `session.close` 后主动断开；PC 清理会话。
- 异常断线：ESP32 只有在本地状态仍为播放中时才重连。

## PC 守护进程分层

- `server.py`: FastAPI WebSocket 服务入口，管理连接与消息路由。
- `protocol.py`: 消息类型、校验和统一响应结构。
- `cloudmusic.py`: 网易云音乐控制适配层。当前提供占位实现，后续可接入窗口自动化、MPRIS/媒体会话、调试接口或插件接口。
- `desktop_app_control.py`: 检查并启动桌面版网易云音乐进程。
- `config.py`: 配置文件加载。

## 关键设计取舍

- WebSocket 只在播放时打开，降低空闲资源占用，也避免 PC 端长期维护无意义连接。
- 恢复播放时需要 ESP32 重新建立 WebSocket；如果暂停状态下仍要接收 PC 主动事件，应增加一个轻量 UDP/HTTP 唤醒通道。
- 歌词使用绝对播放时间戳推送，ESP32 端用 `esp_timer_get_time()` 做短时间平滑，避免每行歌词都依赖网络到达时间。
- 控制命令全部带 `request_id`，PC 端必须返回 `ack` 或 `error`，方便 ESP32 做 UI 反馈。
