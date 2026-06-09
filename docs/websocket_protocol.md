# WebSocket 通信协议

## 连接

- URL: `ws://<pc-host>:8765/ws`
- 编码: UTF-8 JSON
- 连接方向: ESP32 作为客户端，PC 守护进程作为服务端
- 生命周期: 仅播放中保持连接；暂停或关闭时由 ESP32 主动关闭

## 通用消息格式

```json
{
  "type": "lyrics.line",
  "request_id": "optional-id",
  "session_id": "esp32-001-1710000000",
  "timestamp_ms": 1710000000000,
  "payload": {}
}
```

字段说明：

- `type`: 消息类型。
- `request_id`: 需要响应的请求必须携带。
- `session_id`: 一次播放连接的会话 ID。
- `timestamp_ms`: 发送方本地毫秒时间戳。
- `payload`: 消息体。

## ESP32 -> PC

### hello

连接建立后第一条消息。

```json
{
  "type": "hello",
  "session_id": "esp32-001-1710000000",
  "payload": {
    "device_id": "esp32-001",
    "fw_version": "0.1.0",
    "display": { "width": 800, "height": 480 }
  }
}
```

### playback.start

请求 PC 启动桌面版网易云音乐，并开始监听播放状态。

```json
{
  "type": "playback.start",
  "request_id": "req-1",
  "session_id": "esp32-001-1710000000",
  "payload": {
    "source": "netease_cloud_music",
    "open_desktop_app": true
  }
}
```

### playback.pause

暂停后关闭 WebSocket 前发送。

```json
{
  "type": "playback.pause",
  "request_id": "req-2",
  "session_id": "esp32-001-1710000000",
  "payload": {}
}
```

### control.command

播放控制。

```json
{
  "type": "control.command",
  "request_id": "req-3",
  "session_id": "esp32-001-1710000000",
  "payload": {
    "command": "next"
  }
}
```

支持命令建议：

- `play`
- `pause`
- `toggle`
- `next`
- `previous`
- `volume_up`
- `volume_down`

### ping

心跳。

```json
{
  "type": "ping",
  "request_id": "req-4",
  "session_id": "esp32-001-1710000000",
  "payload": {}
}
```

## PC -> ESP32

### ack

```json
{
  "type": "ack",
  "request_id": "req-1",
  "session_id": "esp32-001-1710000000",
  "payload": {
    "ok": true
  }
}
```

### error

```json
{
  "type": "error",
  "request_id": "req-3",
  "session_id": "esp32-001-1710000000",
  "payload": {
    "code": "unsupported_command",
    "message": "Unsupported command: seek"
  }
}
```

### playback.state

```json
{
  "type": "playback.state",
  "session_id": "esp32-001-1710000000",
  "payload": {
    "state": "playing",
    "title": "Song name",
    "artist": "Artist",
    "album": "Album",
    "position_ms": 42000,
    "duration_ms": 210000
  }
}
```

### lyrics.line

推送上一句、当前句、下一句歌词和播放进度，便于 ESP32 做同步歌词展示。

```json
{
  "type": "lyrics.line",
  "session_id": "esp32-001-1710000000",
  "payload": {
    "prev_line": "上一句歌词",
    "line": "当前歌词",
    "next_line": "下一句歌词",
    "position_ms": 42000,
    "duration_ms": 210000,
    "line_start_ms": 41000,
    "line_end_ms": 45500
  }
}
```

### pong

```json
{
  "type": "pong",
  "request_id": "req-4",
  "session_id": "esp32-001-1710000000",
  "payload": {}
}
```

## 时序建议

- ESP32 心跳间隔: 5 秒。
- PC 歌词推送: 歌词行变化时立即推送，必要时每 1 秒补一次 `playback.state`。
- ESP32 控制命令超时: 2 秒未收到 `ack` 或 `error` 即显示失败。
- 断线重连: 只有本地状态仍为播放中才重连，初始 500 ms，指数退避到 5 秒。
