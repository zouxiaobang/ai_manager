from __future__ import annotations

import asyncio
import base64
import logging
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import uvicorn
from fastapi import Body, FastAPI, WebSocket, WebSocketDisconnect

from ai_manager_daemon.cloudmusic import NeteaseCloudMusicController
from ai_manager_daemon.config import AppConfig
from ai_manager_daemon.desktop_app_control import DesktopAppController
from ai_manager_daemon.netease_cdp_progress import process_has_cdp_flag
from ai_manager_daemon.logging_setup import build_uvicorn_log_config
from ai_manager_daemon.protocol import ProtocolMessage, ack, error, make_message

logger = logging.getLogger("uvicorn.error")


@dataclass
class ConnectionContext:
    websocket: WebSocket
    session_id: str = ""
    device_id: str = ""
    connected: bool = True
    push_tasks: list[asyncio.Task[None]] = field(default_factory=list)
    send_lock: asyncio.Lock = field(default_factory=asyncio.Lock)
    high_send_queue: asyncio.Queue[dict[str, Any]] = field(default_factory=asyncio.Queue)
    low_send_queue: asyncio.Queue[dict[str, Any]] = field(default_factory=asyncio.Queue)
    send_task: asyncio.Task[None] | None = None
    cover_push_lock: asyncio.Lock = field(default_factory=asyncio.Lock)
    cover_push_inflight: set[str] = field(default_factory=set)
    last_cover_key: str = ""
    push_generation: int = 0

    def start_send_task(self) -> None:
        if self.send_task is None:
            self.send_task = asyncio.create_task(self._send_loop())

    async def _send_loop(self) -> None:
        while self.connected:
            message: dict[str, Any] | None = None
            try:
                message = self.high_send_queue.get_nowait()
            except asyncio.QueueEmpty:
                try:
                    message = self.low_send_queue.get_nowait()
                except asyncio.QueueEmpty:
                    try:
                        message = await asyncio.wait_for(self.high_send_queue.get(), timeout=0.02)
                    except asyncio.TimeoutError:
                        try:
                            message = await asyncio.wait_for(self.low_send_queue.get(), timeout=0.02)
                        except asyncio.TimeoutError:
                            continue

            try:
                async with self.send_lock:
                    await self.websocket.send_json(message)
            except (WebSocketDisconnect, RuntimeError, OSError):
                self.connected = False
                break

    async def send(self, message: dict[str, Any], *, high_priority: bool = False) -> None:
        if not self.connected:
            raise WebSocketDisconnect()
        if high_priority:
            await self.high_send_queue.put(message)
        else:
            await self.low_send_queue.put(message)

    def mark_disconnected(self) -> None:
        self.connected = False
        self.cancel_push_tasks()
        if self.send_task is not None:
            self.send_task.cancel()
            self.send_task = None

    def cancel_push_tasks(self) -> None:
        # Bump generation so running tasks exit naturally without task.cancel(),
        # which races with winsdk async callbacks on Windows.
        self.push_generation += 1
        self.push_tasks.clear()


async def start_push_tasks(context: ConnectionContext, music: NeteaseCloudMusicController) -> None:
    context.cancel_push_tasks()
    generation = context.push_generation
    logger.info("Starting playback push tasks for session=%s generation=%s", context.session_id, generation)
    context.push_tasks.extend(
        [
            asyncio.create_task(push_playback_state(context, music, generation)),
            asyncio.create_task(push_lyrics(context, music, generation)),
        ]
    )


async def push_snapshot(context: ConnectionContext, music: NeteaseCloudMusicController) -> None:
    playback, lyric = await music.fetch_playback_snapshot()
    if playback is None:
        logger.info("No active media session for snapshot")
        return

    logger.info(
        "Push snapshot: state=%s title=%s artist=%s position_ms=%s",
        playback.state,
        playback.title,
        playback.artist,
        playback.position_ms,
    )
    await context.send(
        make_message(
            "playback.state",
            {
                "state": playback.state,
                "title": playback.title,
                "artist": playback.artist,
                "album": playback.album,
                "position_ms": playback.position_ms,
                "duration_ms": playback.duration_ms,
            },
            session_id=context.session_id,
        ),
        high_priority=True,
    )
    if lyric is not None:
        await context.send(
            make_message(
                "lyrics.line",
                {
                    "prev2_line": lyric.prev2_line,
                    "prev_line": lyric.prev_line,
                    "line": lyric.line,
                    "next_line": lyric.next_line,
                    "next2_line": lyric.next2_line,
                    "position_ms": lyric.position_ms,
                    "duration_ms": lyric.duration_ms,
                    "line_start_ms": lyric.line_start_ms,
                    "line_end_ms": lyric.line_end_ms,
                },
                session_id=context.session_id,
            ),
            high_priority=True,
        )
    asyncio.create_task(push_cover_if_current(context, music, playback.title, playback.artist))


async def push_cover_if_current(
    context: ConnectionContext,
    music: NeteaseCloudMusicController,
    title: str,
    artist: str,
) -> None:
    expected_key = music._track_key(title, artist)
    if not expected_key or not context.connected:
        return
    if context.last_cover_key == expected_key:
        return
    if expected_key in context.cover_push_inflight:
        return

    async with context.cover_push_lock:
        if context.last_cover_key == expected_key or expected_key in context.cover_push_inflight:
            return
        context.cover_push_inflight.add(expected_key)
        try:
            await asyncio.sleep(0.15)
            cover = await music.fetch_cover_art(title, artist)
            if cover is None:
                return

            track = await music._get_current_track()
            if track is None:
                return

            current_key = music._track_key(track.title, track.artist)
            if current_key != expected_key or cover.track_key != expected_key:
                logger.info("Skip stale cover push for %s", expected_key)
                return

            music.drop_cover_cache_except(expected_key)
            logger.info("Push cover: track_key=%s bytes=%s", cover.track_key, len(cover.rgb565_le))
            await context.send(
                make_message(
                    "playback.cover",
                    {
                        "track_key": cover.track_key,
                        "width": cover.width,
                        "height": cover.height,
                        "format": "rgb565",
                        "data_b64": base64.b64encode(cover.rgb565_le).decode("ascii"),
                    },
                    session_id=context.session_id,
                )
            )
            context.last_cover_key = expected_key
        except (WebSocketDisconnect, RuntimeError, OSError) as exc:
            logger.info("Stop cover push task: %s", exc)
        except Exception:
            logger.exception("Cover push failed")
        finally:
            context.cover_push_inflight.discard(expected_key)


def _cdp_launch_port(config: AppConfig) -> int:
    if config.netease.progress_source == "winsdk":
        return 0
    if not config.netease.cdp_auto_launch_debug:
        return 0
    return config.netease.cdp_port


def _cdp_port_is_listening(port: int) -> bool:
    import socket

    try:
        with socket.create_connection(("127.0.0.1", port), timeout=1.0):
            return True
    except OSError:
        return False


async def _startup_prepare_desktop_app(config: AppConfig, desktop_app: DesktopAppController) -> None:
    logger.info(
        "Startup desktop app check: auto_launch=%s executable=%s cdp_port=%s",
        config.desktop_app.auto_launch_on_startup,
        config.desktop_app.executable or "(empty)",
        _cdp_launch_port(config) or "off",
    )
    if not config.desktop_app.auto_launch_on_startup:
        logger.info("desktop_app.auto_launch_on_startup=false, skip launching NetEase on daemon startup")
        return

    configured = Path(config.desktop_app.executable) if config.desktop_app.executable else None
    if configured is not None and not configured.exists():
        logger.warning("Configured executable does not exist: %s", configured)

    cdp_port = _cdp_launch_port(config)
    try:
        await asyncio.to_thread(desktop_app.launch_if_needed, cdp_port)
        logger.info("Desktop app launch check finished (cdp_port=%s)", cdp_port or "off")
    except FileNotFoundError as exc:
        logger.error("NetEase executable not found: %s", exc)
        return
    except OSError as exc:
        logger.exception("Failed to launch desktop app: %s", exc)
        return

    if cdp_port <= 0:
        return

    for delay in (2.0, 3.0, 5.0):
        await asyncio.sleep(delay)
        if _cdp_port_is_listening(cdp_port):
            logger.info("CDP debug port %s is listening", cdp_port)
            return

    if desktop_app.is_running() and not process_has_cdp_flag(
        config.desktop_app.process_name,
        cdp_port,
    ):
        logger.warning(
            "NetEase is running but port %s is closed. The client may ignore "
            "--remote-debugging-port on the command line. Quit NetEase completely, then run manually: "
            "\"%s\" --remote-debugging-port=%s",
            cdp_port,
            config.desktop_app.executable,
            cdp_port,
        )
    else:
        logger.warning(
            "CDP port %s is not listening yet. If NetEase is starting, wait and open "
            "http://127.0.0.1:%s/json/list again.",
            cdp_port,
            cdp_port,
        )


def create_app(config: AppConfig) -> FastAPI:
    app = FastAPI(title="ESP32-S3 Smart Display Daemon")
    desktop_app = DesktopAppController(config.desktop_app.executable, config.desktop_app.process_name)
    music = NeteaseCloudMusicController(
        config.netease.playback_poll_interval_seconds,
        config.netease.lyric_poll_interval_seconds,
        config.netease.lyric_offset_ms,
        config.desktop_app.process_name,
        config.netease.progress_source,
        config.netease.cdp_port,
    )

    @app.on_event("startup")
    async def configure_asyncio_exception_handler() -> None:
        loop = asyncio.get_running_loop()

        def _asyncio_exception_handler(
            running_loop: asyncio.AbstractEventLoop,
            context: dict[str, object],
        ) -> None:
            exception = context.get("exception")
            if isinstance(exception, asyncio.InvalidStateError):
                return
            running_loop.default_exception_handler(context)

        loop.set_exception_handler(_asyncio_exception_handler)
        asyncio.create_task(_startup_prepare_desktop_app(config, desktop_app))
        asyncio.create_task(music.timeline_poll_loop())

    @app.get("/health")
    async def health() -> dict[str, str]:
        return {"status": "ok"}

    @app.post("/admin/lyrics/cache/clear")
    async def clear_lyrics_cache(payload: dict[str, Any] = Body(default_factory=dict)) -> dict[str, object]:
        removed = music.clear_lyrics_cache(
            song_ids=payload.get("song_ids"),
            tracks=payload.get("tracks"),
            clear_all=bool(payload.get("clear_all")),
        )
        return {"ok": True, "removed": removed}

    @app.websocket("/ws")
    async def websocket_endpoint(websocket: WebSocket) -> None:
        await websocket.accept()
        context = ConnectionContext(websocket=websocket)
        context.start_send_task()

        try:
            while True:
                raw = await websocket.receive_json()
                message = ProtocolMessage.from_dict(raw)
                context.session_id = message.session_id or context.session_id
                if message.type != "ping":
                    logger.info("Received message: %s payload=%s", message.type, message.payload)
                await handle_message(config, desktop_app, music, context, message)
        except WebSocketDisconnect:
            logger.info("WebSocket client disconnected")
            context.mark_disconnected()
        except OSError as exc:
            logger.info("WebSocket connection lost: %s", exc)
            context.mark_disconnected()
        except Exception as exc:
            logger.exception("WebSocket session failed")
            context.mark_disconnected()
            try:
                await websocket.close(code=1011, reason=str(exc))
            except Exception:
                pass

    return app


async def handle_message(
    config: AppConfig,
    desktop_app: DesktopAppController,
    music: NeteaseCloudMusicController,
    context: ConnectionContext,
    message: ProtocolMessage,
) -> None:
    if message.type == "hello":
        await handle_hello(config, context, message)
        return

    if message.type == "ping":
        await context.send(make_message("pong", {}, session_id=message.session_id, request_id=message.request_id), high_priority=True)
        return

    if message.type == "playback.start":
        if message.payload.get("open_desktop_app", True):
            try:
                logger.info("Opening desktop app: %s", config.desktop_app.executable)
                desktop_app.launch_if_needed(_cdp_launch_port(config))
                logger.info("Desktop app is running or focused")
            except (FileNotFoundError, RuntimeError, OSError) as exc:
                logger.exception("Desktop app launch failed")
                await context.send(error(message, "desktop_app_launch_failed", str(exc)), high_priority=True)
                return
        if message.payload.get("sync_state", True):
            await push_snapshot(context, music)
        await start_push_tasks(context, music)
        await context.send(ack(message), high_priority=True)
        return

    if message.type == "playback.sync":
        await push_snapshot(context, music)
        await start_push_tasks(context, music)
        await context.send(ack(message), high_priority=True)
        return

    if message.type == "playback.pause":
        context.cancel_push_tasks()
        await context.send(ack(message), high_priority=True)
        return

    if message.type == "session.close":
        context.cancel_push_tasks()
        await context.send(ack(message), high_priority=True)
        await context.websocket.close(code=1000)
        return

    if message.type == "control.command":
        command = str(message.payload.get("command", ""))
        raw_position_ms = message.payload.get("position_ms")
        position_ms = int(raw_position_ms) if raw_position_ms is not None else None
        try:
            lyric = None
            if command == "seek":
                if position_ms is None:
                    raise ValueError("seek requires position_ms")
                lyric = await music.apply_user_seek(position_ms)
                logger.info("Music seek applied: position_ms=%s", position_ms)
            else:
                # 先执行媒体键，避免 launch_if_needed 在「已运行但无可见窗口」时阻塞约 2.5s
                await asyncio.to_thread(music.handle_command, command, None)
                logger.info("Music command sent: %s", command)

            if not desktop_app.is_running():
                asyncio.create_task(
                    asyncio.to_thread(desktop_app.launch_if_needed, _cdp_launch_port(config))
                )

            if command == "seek" and position_ms is not None:
                track = music.peek_last_track()
                if track is not None:
                    await context.send(
                        make_message(
                            "playback.state",
                            {
                                "state": track.state,
                                "title": track.title,
                                "artist": track.artist,
                                "album": track.album,
                                "position_ms": position_ms,
                                "duration_ms": track.duration_ms,
                            },
                            session_id=context.session_id,
                        ),
                        high_priority=True,
                    )
            else:
                track = await music._get_current_track()
                if track is not None:
                    await context.send(
                        make_message(
                            "playback.state",
                            {
                                "state": track.state,
                                "title": track.title,
                                "artist": track.artist,
                                "album": track.album,
                                "position_ms": track.position_ms,
                                "duration_ms": track.duration_ms,
                            },
                            session_id=context.session_id,
                        ),
                        high_priority=True,
                    )
            if lyric is not None:
                await context.send(
                    make_message(
                        "lyrics.line",
                        {
                            "prev2_line": lyric.prev2_line,
                            "prev_line": lyric.prev_line,
                            "line": lyric.line,
                            "next_line": lyric.next_line,
                            "next2_line": lyric.next2_line,
                            "position_ms": lyric.position_ms,
                            "duration_ms": lyric.duration_ms,
                            "line_start_ms": lyric.line_start_ms,
                            "line_end_ms": lyric.line_end_ms,
                        },
                        session_id=context.session_id,
                    ),
                    high_priority=True,
                )
            await context.send(ack(message, {"command": command}), high_priority=True)
        except ValueError as exc:
            await context.send(error(message, "unsupported_command", str(exc)), high_priority=True)
        except (FileNotFoundError, RuntimeError, OSError) as exc:
            logger.exception("Music command failed")
            await context.send(error(message, "music_command_failed", str(exc)), high_priority=True)
        return

    await context.send(error(message, "unknown_type", f"Unknown message type: {message.type}"), high_priority=True)


async def handle_hello(config: AppConfig, context: ConnectionContext, message: ProtocolMessage) -> None:
    device_id = str(message.payload.get("device_id", ""))
    allowed = config.security.allowed_device_ids

    if allowed and device_id not in allowed:
        await context.send(error(message, "device_not_allowed", f"Device is not allowed: {device_id}"), high_priority=True)
        await context.websocket.close(code=1008)
        return

    context.device_id = device_id
    await context.send(ack(message, {"server": "ai-manager-daemon"}), high_priority=True)


async def push_playback_state(
    context: ConnectionContext,
    music: NeteaseCloudMusicController,
    generation: int,
) -> None:
    logger.info("Playback state push task started generation=%s", generation)
    try:
        async for state in music.playback_states():
            if not context.connected or generation != context.push_generation:
                break
            track_key = music._track_key(state.title, state.artist)
            if track_key and track_key != context.last_cover_key:
                asyncio.create_task(push_cover_if_current(context, music, state.title, state.artist))

            logger.info(
                "Push playback state: state=%s title=%s artist=%s position_ms=%s duration_ms=%s",
                state.state,
                state.title,
                state.artist,
                state.position_ms,
                state.duration_ms,
            )
            await context.send(
                make_message(
                    "playback.state",
                    {
                        "state": state.state,
                        "title": state.title,
                        "artist": state.artist,
                        "album": state.album,
                        "position_ms": state.position_ms,
                        "duration_ms": state.duration_ms,
                    },
                    session_id=context.session_id,
                ),
                high_priority=True,
            )
    except (WebSocketDisconnect, RuntimeError, OSError):
        logger.info("Stop playback state push task")
    except asyncio.CancelledError:
        logger.info("Stop playback state push task (cancelled)")
    except Exception:
        logger.exception("Playback state push task crashed")


async def push_lyrics(
    context: ConnectionContext,
    music: NeteaseCloudMusicController,
    generation: int,
) -> None:
    logger.info("Lyrics push task started generation=%s", generation)
    try:
        async for lyric in music.lyric_lines():
            if not context.connected or generation != context.push_generation:
                break
            logger.info(
                "Push lyrics.line: prev2=%s prev=%s line=%s next=%s next2=%s position_ms=%s",
                lyric.prev2_line,
                lyric.prev_line,
                lyric.line,
                lyric.next_line,
                lyric.next2_line,
                lyric.position_ms,
            )
            await context.send(
                make_message(
                    "lyrics.line",
                    {
                        "prev2_line": lyric.prev2_line,
                        "prev_line": lyric.prev_line,
                        "line": lyric.line,
                        "next_line": lyric.next_line,
                        "next2_line": lyric.next2_line,
                        "position_ms": lyric.position_ms,
                        "duration_ms": lyric.duration_ms,
                        "line_start_ms": lyric.line_start_ms,
                        "line_end_ms": lyric.line_end_ms,
                    },
                    session_id=context.session_id,
                ),
                high_priority=True,
            )
    except (WebSocketDisconnect, RuntimeError, OSError):
        logger.info("Stop lyrics push task")
    except asyncio.CancelledError:
        logger.info("Stop lyrics push task (cancelled)")
    except Exception:
        logger.exception("Lyrics push task crashed")


def run_server(config: AppConfig) -> None:
    app = create_app(config)
    uvicorn.run(
        app,
        host=config.host,
        port=config.port,
        log_config=build_uvicorn_log_config(),
        ws_ping_interval=20.0,
        ws_ping_timeout=120.0,
        ws_max_size=4 * 1024 * 1024,
    )
