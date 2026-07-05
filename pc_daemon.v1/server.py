from __future__ import annotations

import asyncio
import json
import logging
import time
from typing import Any

from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

from cloudmusic import NeteaseBridge
from config import load_settings

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
logger = logging.getLogger("server")

settings = load_settings()
bridge = NeteaseBridge(settings)
app = FastAPI(title="NetEase Media Bridge", version="2.0.0")


class ControlBody(BaseModel):
    command: str = Field(description="play|pause|toggle|next|previous")


def ok(data: dict[str, Any]) -> JSONResponse:
    return JSONResponse({"code": 0, "data": data})


def snapshot_payload(snapshot) -> dict[str, Any]:
    return {
        "app_running": snapshot.app_running,
        "state": snapshot.state,
        "title": snapshot.title,
        "artist": snapshot.artist,
        "album": snapshot.album,
        "position_ms": snapshot.position_ms,
        "duration_ms": snapshot.duration_ms,
        "starting": snapshot.starting,
        "lyrics": {
            "prev_prev_line": snapshot.lyrics.prev_prev_line,
            "prev_line": snapshot.lyrics.prev_line,
            "line": snapshot.lyrics.line,
            "next_line": snapshot.lyrics.next_line,
            "next_next_line": snapshot.lyrics.next_next_line,
            "line_start_ms": snapshot.lyrics.line_start_ms,
            "line_end_ms": snapshot.lyrics.line_end_ms,
        },
        "updated_at_ms": snapshot.updated_at_ms,
    }


@app.get("/health")
def health() -> JSONResponse:
    return ok({"status": "ok"})


@app.get("/api/media/status")
def media_status() -> JSONResponse:
    snap = bridge.get_snapshot()
    resp = {"cmd": "sync", "position": snap.position_ms, "playing": snap.state == "playing"}
    logger.info("API response [status]: %s", json.dumps(resp))
    return resp


@app.get("/api/media/lyrics")
def media_lyrics() -> JSONResponse:
    """返回当前歌曲的 LRC"""
    resp = {
        "cmd": "song",
        "title": bridge.get_current_title(),
        "artist": bridge.get_current_artist(),
        "lyrics": bridge.get_current_lyric_raw(),
    }
    logger.info("API response [lyrics]: cmd=song title='%s' artist='%s' lyrics_len=%d",
                resp["title"], resp["artist"], len(resp.get("lyrics", "")))
    return resp


@app.post("/api/media/start")
def media_start() -> JSONResponse:
    accepted = bridge.request_start()
    return ok({"accepted": accepted, "message": "starting_netease"})


@app.post("/api/media/control")
def media_control(body: ControlBody) -> JSONResponse:
    ok_flag = bridge.send_command(body.command)
    return ok({"ok": ok_flag, "command": body.command})


@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket) -> None:
    await websocket.accept()
    session_id = f"esp32-{int(time.time())}"
    await websocket.send_json({
        "type": "hello.ack",
        "session_id": session_id,
        "payload": {"ok": True},
    })

    # 记录上一首歌的 key，检测切歌
    last_song_key = ""
    # sync 推送间隔 500ms
    sync_interval = 0.5

    try:
        while True:
            try:
                raw = await asyncio.wait_for(websocket.receive_text(), timeout=sync_interval)
                message = json.loads(raw)
                await _handle_ws_message(websocket, session_id, message)
            except asyncio.TimeoutError:
                pass

            snapshot = bridge.get_snapshot()
            title = snapshot.title.strip()
            artist = snapshot.artist.strip()

            # --- 检测切歌：歌曲变化时推送完整 LRC ---
            song_key = f"{title}\0{artist}" if title else ""
            if song_key and song_key != last_song_key:
                last_song_key = song_key
                raw_lyrics = bridge.get_current_lyric_raw()
                logger.info(
                    "Song changed: '%s - %s' lyric_len=%d, pushing to ESP",
                    title, artist, len(raw_lyrics),
                )
                await websocket.send_json({
                    "type": "song",
                    "session_id": session_id,
                    "timestamp_ms": int(time.time() * 1000),
                    "payload": {
                        "title": title,
                        "artist": artist,
                        "lyrics": raw_lyrics,
                    },
                })

            # --- 每轮推送同步进度 ---
            snapshot = bridge.get_snapshot()
            estimated_ms = snapshot.lyrics.line_start_ms  # 用 pick_lines 的结果
            # 更准确：从 snapshot 取 position_ms
            pos = snapshot.position_ms
            await websocket.send_json({
                "type": "sync",
                "session_id": session_id,
                "timestamp_ms": int(time.time() * 1000),
                "payload": {
                    "position": pos,
                    "playing": snapshot.state == "playing",
                    "state": snapshot.state,
                    "duration_ms": snapshot.duration_ms,
                    "line": snapshot.lyrics.line,
                    "line_start_ms": snapshot.lyrics.line_start_ms,
                    "line_end_ms": snapshot.lyrics.line_end_ms,
                },
            })

    except WebSocketDisconnect:
        logger.info("WebSocket disconnected: %s", session_id)


async def _handle_ws_message(websocket: WebSocket, session_id: str, message: dict[str, Any]) -> None:
    msg_type = message.get("type")
    request_id = message.get("request_id")
    payload = message.get("payload") or {}

    if msg_type == "ping":
        await websocket.send_json({
            "type": "pong",
            "request_id": request_id,
            "session_id": session_id,
            "payload": {},
        })
        return

    if msg_type in ("playback.start", "hello"):
        # ESP 连接/启动时，立即返回当前歌曲的 LRC 和状态
        title = bridge.get_current_title()
        artist = bridge.get_current_artist()
        raw_lyrics = bridge.get_current_lyric_raw()
        snapshot = bridge.get_snapshot()

        await websocket.send_json({
            "type": "song",
            "request_id": request_id,
            "session_id": session_id,
            "timestamp_ms": int(time.time() * 1000),
            "payload": {
                "title": title,
                "artist": artist,
                "lyrics": raw_lyrics,
            },
        })
        # 同步当前状态
        await websocket.send_json({
            "type": "sync",
            "request_id": request_id,
            "session_id": session_id,
            "timestamp_ms": int(time.time() * 1000),
            "payload": {
                "position": snapshot.position_ms,
                "playing": snapshot.state == "playing",
                "state": snapshot.state,
                "duration_ms": snapshot.duration_ms,
                "line": snapshot.lyrics.line,
                "line_start_ms": snapshot.lyrics.line_start_ms,
                "line_end_ms": snapshot.lyrics.line_end_ms,
            },
        })
        # 回复 ack
        await websocket.send_json({
            "type": "ack",
            "request_id": request_id,
            "session_id": session_id,
            "payload": {"ok": True},
        })
        return

    if msg_type == "request_lyrics":
        # ESP 主动请求当前歌词
        title = bridge.get_current_title()
        artist = bridge.get_current_artist()
        raw_lyrics = bridge.get_current_lyric_raw()
        await websocket.send_json({
            "type": "song",
            "request_id": request_id,
            "session_id": session_id,
            "timestamp_ms": int(time.time() * 1000),
            "payload": {
                "title": title,
                "artist": artist,
                "lyrics": raw_lyrics,
            },
        })
        return

    if msg_type == "control.command":
        command = str(payload.get("command") or "")
        ok_flag = bridge.send_command(command)
        await websocket.send_json({
            "type": "ack" if ok_flag else "error",
            "request_id": request_id,
            "session_id": session_id,
            "payload": {"ok": ok_flag, "command": command},
        })
        return


@app.on_event("shutdown")
def on_shutdown() -> None:
    bridge.close()


def main() -> None:
    import uvicorn

    logger.info("NetEase bridge v2 listening on %s:%s", settings.host, settings.port)
    uvicorn.run("server:app", host=settings.host, port=settings.port, reload=False, log_level="info")


if __name__ == "__main__":
    main()