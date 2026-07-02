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
app = FastAPI(title="NetEase Media Bridge", version="1.0.0")


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
            "prev_line": snapshot.lyrics.prev_line,
            "line": snapshot.lyrics.line,
            "next_line": snapshot.lyrics.next_line,
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
    return ok(snapshot_payload(bridge.get_snapshot()))


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
    await websocket.send_json(
        {
            "type": "hello.ack",
            "session_id": session_id,
            "payload": {"ok": True},
        }
    )
    push_interval = settings.lyric_push_interval_ms / 1000.0
    last_line = ""
    try:
        while True:
            try:
                raw = await asyncio.wait_for(websocket.receive_text(), timeout=push_interval)
                message = json.loads(raw)
                await _handle_ws_message(websocket, session_id, message)
            except asyncio.TimeoutError:
                pass

            snapshot = bridge.get_snapshot()
            line = snapshot.lyrics.line
            if line != last_line or snapshot.state == "playing":
                last_line = line
                await websocket.send_json(
                    {
                        "type": "lyrics.line",
                        "session_id": session_id,
                        "timestamp_ms": int(time.time() * 1000),
                        "payload": {
                            **snapshot_payload(snapshot)["lyrics"],
                            "position_ms": snapshot.position_ms,
                            "duration_ms": snapshot.duration_ms,
                            "title": snapshot.title,
                            "artist": snapshot.artist,
                            "state": snapshot.state,
                            "app_running": snapshot.app_running,
                        },
                    }
                )
                await websocket.send_json(
                    {
                        "type": "playback.state",
                        "session_id": session_id,
                        "timestamp_ms": int(time.time() * 1000),
                        "payload": snapshot_payload(snapshot),
                    }
                )
    except WebSocketDisconnect:
        logger.info("WebSocket disconnected: %s", session_id)


async def _handle_ws_message(websocket: WebSocket, session_id: str, message: dict[str, Any]) -> None:
    msg_type = message.get("type")
    request_id = message.get("request_id")
    payload = message.get("payload") or {}

    if msg_type == "ping":
        await websocket.send_json(
            {"type": "pong", "request_id": request_id, "session_id": session_id, "payload": {}}
        )
        return

    if msg_type == "playback.start":
        bridge.request_start()
        await websocket.send_json(
            {
                "type": "ack",
                "request_id": request_id,
                "session_id": session_id,
                "payload": {"ok": True},
            }
        )
        return

    if msg_type == "control.command":
        command = str(payload.get("command") or "")
        ok_flag = bridge.send_command(command)
        await websocket.send_json(
            {
                "type": "ack" if ok_flag else "error",
                "request_id": request_id,
                "session_id": session_id,
                "payload": {"ok": ok_flag, "command": command},
            }
        )
        return

    if msg_type == "hello":
        await websocket.send_json(
            {
                "type": "ack",
                "request_id": request_id,
                "session_id": session_id,
                "payload": {"ok": True},
            }
        )


@app.on_event("shutdown")
def on_shutdown() -> None:
    bridge.close()


def main() -> None:
    import uvicorn

    logger.info("NetEase bridge listening on %s:%s", settings.host, settings.port)
    uvicorn.run("server:app", host=settings.host, port=settings.port, reload=False, log_level="info")


if __name__ == "__main__":
    main()
