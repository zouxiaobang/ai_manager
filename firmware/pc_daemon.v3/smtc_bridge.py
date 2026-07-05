"""SMTC 桥接模块 —— 通过 Windows SMTC 读取网易云音乐状态并发送控制命令。"""

from __future__ import annotations

import asyncio
import logging
import threading
import time
from dataclasses import dataclass, field

logger = logging.getLogger("smtc_bridge")

# winsdk 延迟导入（仅在 Windows 上可用）
_SessionManager = None
_PBStatus = None


def _import_smtc():
    global _SessionManager, _PBStatus
    if _SessionManager is None:
        from winsdk.windows.media.control import (
            GlobalSystemMediaTransportControlsSessionManager as Mgr,
            GlobalSystemMediaTransportControlsSessionPlaybackStatus as PB,
        )
        _SessionManager = Mgr
        _PBStatus = PB
    return _SessionManager, _PBStatus


@dataclass
class PlaybackState:
    available: bool = False
    playing: bool = False
    title: str = ""
    artist: str = ""
    album: str = ""
    position_ms: int = 0
    duration_ms: int = 0
    updated_at_ms: int = 0


class SMTCBridge:
    """SMTC 桥接：在后台线程中轮询系统 SMTC 会话，读取网易云音乐播放状态。"""

    NETBASE_APP_TOKENS = (
        "cloudmusic.exe",
        "cloudmusic",
        "netease",
        "网易云音乐",
    )

    def __init__(self, poll_interval: float = 0.5) -> None:
        self._poll_interval = poll_interval
        self._lock = threading.Lock()
        self._state = PlaybackState()
        self._loop: asyncio.AbstractEventLoop | None = None
        self._thread = threading.Thread(target=self._run_loop, name="smtc", daemon=True)
        self._thread.start()

    # ── 公开接口 ──────────────────────────────────────────────

    def get_state(self) -> PlaybackState:
        with self._lock:
            return PlaybackState(**self._state.__dict__)

    def play(self) -> bool:
        return self._run_coro(self._send_command("play"))

    def pause(self) -> bool:
        return self._run_coro(self._send_command("pause"))

    def toggle(self) -> bool:
        return self._run_coro(self._send_command("toggle"))

    def next_track(self) -> bool:
        return self._run_coro(self._send_command("next"))

    def prev_track(self) -> bool:
        return self._run_coro(self._send_command("previous"))

    def seek(self, position_ms: int) -> bool:
        """跳转到指定毫秒位置。"""
        return self._run_coro(self._seek_to(position_ms))

    # ── 内部实现 ──────────────────────────────────────────────

    def _run_loop(self) -> None:
        self._loop = asyncio.new_event_loop()
        asyncio.set_event_loop(self._loop)
        self._loop.run_until_complete(self._poll_forever())

    def _run_coro(self, coro) -> bool:
        if self._loop is None:
            return False
        try:
            future = asyncio.run_coroutine_threadsafe(coro, self._loop)
            return bool(future.result(timeout=3.0))
        except Exception as exc:
            logger.warning("Command failed: %s", exc)
            return False

    async def _poll_forever(self) -> None:
        while True:
            try:
                await self._poll_once()
            except Exception as exc:
                logger.exception("SMTC poll error: %s", exc)
            await asyncio.sleep(self._poll_interval)

    async def _poll_once(self) -> None:
        session = await self._find_session()
        if session is None:
            with self._lock:
                self._state = PlaybackState(
                    available=False,
                    updated_at_ms=int(time.time() * 1000),
                )
            return

        try:
            props = await session.try_get_media_properties_async()
        except Exception:
            props = None

        playback = session.get_playback_info()
        timeline = session.get_timeline_properties()

        # 解析播放状态
        raw = playback.playback_status
        if isinstance(raw, int):
            playing = raw == 4
        else:
            _, PB = _import_smtc()
            playing = raw == PB.PLAYING

        # 解析进度
        position_ms = self._td_ms(timeline.position)
        duration_ms = self._td_ms(timeline.end_time)

        title = (props.title or "").strip() if props else ""
        artist = (props.artist or "").strip() if props else ""
        album = (props.album_title or "").strip() if props else ""

        with self._lock:
            self._state = PlaybackState(
                available=True,
                playing=playing,
                title=title,
                artist=artist,
                album=album,
                position_ms=position_ms,
                duration_ms=duration_ms,
                updated_at_ms=int(time.time() * 1000),
            )

    async def _find_session(self):
        """找到网易云音乐的 SMTC 会话。"""
        Mgr, _ = _import_smtc()
        manager = await Mgr.request_async()
        for session in manager.get_sessions():
            app_id = (session.source_app_user_model_id or "").lower()
            if any(tok.lower() in app_id for tok in self.NETBASE_APP_TOKENS):
                return session
            # 兜底：通过媒体属性判断
            try:
                props = await session.try_get_media_properties_async()
            except OSError:
                continue
            if props:
                artist = (props.artist or "").lower()
                title = (props.title or "").lower()
                if "netease" in artist or "网易" in artist or "cloudmusic" in title:
                    return session
        return None

    async def _send_command(self, command: str) -> bool:
        session = await self._find_session()
        if session is None:
            return False

        if command == "play":
            return bool(await session.try_play_async())
        if command == "pause":
            return bool(await session.try_pause_async())
        if command == "toggle":
            _, PB = _import_smtc()
            raw = session.get_playback_info().playback_status
            is_playing = raw == PB.PLAYING if not isinstance(raw, int) else raw == 4
            if is_playing:
                return bool(await session.try_pause_async())
            return bool(await session.try_play_async())
        if command == "next":
            return bool(await session.try_skip_next_async())
        if command == "previous":
            return bool(await session.try_skip_previous_async())
        return False

    async def _seek_to(self, position_ms: int) -> bool:
        session = await self._find_session()
        if session is None:
            logger.warning("Seek skipped: no SMTC session found")
            return False

        # 某些 App 在暂停状态下不允许 seek，先尝试恢复播放
        _, PB = _import_smtc()
        raw = session.get_playback_info().playback_status
        was_paused = (raw == PB.PAUSED if not isinstance(raw, int) else raw == 5)
        if was_paused:
            logger.info("Seek: session was paused, sending play first")
            await session.try_play_async()

        try:
            # Windows.Foundation.TimeSpan 在 winsdk 中映射为 int ticks
            # 1 ms = 10000 ticks (100 纳秒单位)
            ticks = position_ms * 10000
            result = bool(await session.try_change_playback_position_async(ticks))
            logger.info("Seek to %d ms -> %s", position_ms, "OK" if result else "FAILED (not supported)")
            return result
        except Exception as exc:
            logger.warning("Seek to %d ms failed: %s", position_ms, exc)
            return False

    @staticmethod
    def _td_ms(td) -> int:
        if td is None:
            return 0
        try:
            return int(td.total_seconds() * 1000)
        except Exception:
            return 0
