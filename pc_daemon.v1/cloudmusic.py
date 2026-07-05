from __future__ import annotations

import asyncio
import logging
import threading
import time
from dataclasses import dataclass, field
from typing import Any

import httpx

from config import Settings
from desktop_app_control import is_netease_running, launch_netease, wait_until_running
from lyrics_parser import LyricLine, LyricSnapshot, parse_lrc, pick_lines

logger = logging.getLogger(__name__)


@dataclass
class PlaybackSnapshot:
    app_running: bool = False
    state: str = "closed"  # closed | paused | playing | stopped
    title: str = ""
    artist: str = ""
    album: str = ""
    position_ms: int = 0
    duration_ms: int = 0
    starting: bool = False
    lyrics: LyricSnapshot = field(default_factory=LyricSnapshot)
    updated_at_ms: int = 0


class NeteaseBridge:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._lock = threading.Lock()
        self._snapshot = PlaybackSnapshot()
        self._starting = False
        self._lyric_lines: list[LyricLine] = []
        self._lyric_key = ""
        self._http = httpx.Client(timeout=8.0, headers=settings.netease_headers)
        self._loop: asyncio.AbstractEventLoop | None = None
        self._thread = threading.Thread(target=self._thread_main, name="smtc_poll", daemon=True)
        self._thread.start()
        self._last_play_time: float = 0.0
        self._last_position_ms: int = 0
        self._last_state: str = "closed"
        self._paused_position_ms: int = 0
        self._lyric_raw_text: str = ""

    def close(self) -> None:
        self._http.close()

    def get_snapshot(self) -> PlaybackSnapshot:
        with self._lock:
            return PlaybackSnapshot(**self._snapshot.__dict__)

    def get_current_lyric_raw(self) -> str:
        with self._lock:
            return self._lyric_raw_text

    def get_current_title(self) -> str:
        with self._lock:
            return self._snapshot.title

    def get_current_artist(self) -> str:
        with self._lock:
            return self._snapshot.artist

    def request_start(self) -> bool:
        with self._lock:
            if self._starting:
                return True
            self._starting = True
            self._snapshot.starting = True
        threading.Thread(target=self._start_worker, name="netease_start", daemon=True).start()
        return True

    def send_command(self, command: str) -> bool:
        if self._loop is None:
            return False
        future = asyncio.run_coroutine_threadsafe(self._handle_command(command), self._loop)
        try:
            return bool(future.result(timeout=3.0))
        except Exception as exc:  # noqa: BLE001
            logger.warning("Control command failed: %s (%s)", command, exc)
            return False

    def _start_worker(self) -> None:
        try:
            if not is_netease_running(self.settings):
                if not launch_netease():
                    return
                wait_until_running(self.settings, timeout_sec=25.0)
            time.sleep(1.0)
            self.send_command("play")
        finally:
            with self._lock:
                self._starting = False
                self._snapshot.starting = False

    def _thread_main(self) -> None:
        self._loop = asyncio.new_event_loop()
        asyncio.set_event_loop(self._loop)
        self._loop.run_until_complete(self._poll_forever())

    async def _poll_forever(self) -> None:
        interval = self.settings.poll_interval_ms / 1000.0
        while True:
            try:
                await self._poll_once()
            except Exception as exc:  # noqa: BLE001
                logger.exception("SMTC poll failed: %s", exc)
            await asyncio.sleep(interval)

    async def _poll_once(self) -> None:
        app_running = is_netease_running(self.settings)
        if not app_running:
            self._apply_snapshot(
                PlaybackSnapshot(
                    app_running=False,
                    state="closed",
                    starting=self._starting,
                    updated_at_ms=int(time.time() * 1000),
                )
            )
            self._lyric_lines = []
            self._lyric_key = ""
            return

        session = await self._find_netease_session()
        if session is None:
            self._apply_snapshot(
                PlaybackSnapshot(
                    app_running=True,
                    state="stopped",
                    starting=self._starting,
                    updated_at_ms=int(time.time() * 1000),
                )
            )
            return

        props = await session.try_get_media_properties_async()
        playback = session.get_playback_info()
        timeline = session.get_timeline_properties()

        title = (props.title or "").strip()
        artist = (props.artist or "").strip()
        album = (props.album_title or "").strip()
        from winsdk.windows.media.control import (
            GlobalSystemMediaTransportControlsSessionPlaybackStatus as _PBStatus,
        )

        raw = playback.playback_status
        if isinstance(raw, int):
            # winsdk returns raw int on some Windows builds
            state = "playing" if raw == 4 else "paused" if raw == 5 else "stopped"
        else:
            state = (
                "playing"
                if raw == _PBStatus.PLAYING
                else "paused"
                if raw == _PBStatus.PAUSED
                else "stopped"
            )

        position_ms = int(timeline.position.total_seconds() * 1000) if timeline.position else 0
        duration_ms = int(timeline.end_time.total_seconds() * 1000) if timeline.end_time else 0

        now = time.time()
        if state == "playing":
            if self._last_state == "paused":
                # 从暂停恢复，用暂停前记住的位置
                estimated_ms = self._paused_position_ms
                self._last_position_ms = estimated_ms
                self._last_play_time = now
                logger.info(
                    "Resumed from pause: restoring position=%d", estimated_ms
                )
            elif self._last_state == "playing" and self._last_play_time > 0:
                # 持续播放中，基于上一轮位置 + 经过时间推算
                elapsed_ms = int((now - self._last_play_time) * 1000)
                estimated_ms = self._last_position_ms + elapsed_ms
                self._last_position_ms = estimated_ms
                self._last_play_time = now
            else:
                # 刚进入播放状态，使用 SMTC position（可能为0）
                estimated_ms = position_ms
                self._last_position_ms = position_ms
                self._last_play_time = now
        elif state == "paused":
            # 暂停时记住当前播放位置
            self._paused_position_ms = self._last_position_ms
            estimated_ms = self._last_position_ms
            self._last_play_time = 0.0
            logger.info(
                "Paused: freezing position=%d", self._paused_position_ms
            )
        else:
            estimated_ms = 0
            self._last_position_ms = 0
            self._last_play_time = 0.0
            self._paused_position_ms = 0

        # 如果 SMTC 返回了可信的 position，仅在大偏差时校准
        if state == "playing" and position_ms > 0:
            diff = abs(position_ms - estimated_ms)
            if diff > 3000:
                logger.info(
                    "Position drift detected: estimated=%d smtc=%d drift=%d, calibrating",
                    estimated_ms, position_ms, diff,
                )
                self._last_position_ms = position_ms
                self._last_play_time = time.time()
        self._last_state = state

        lyrics = LyricSnapshot()
        if title:
            self._ensure_lyrics(title, artist, duration_ms)
            lyrics = pick_lines(self._lyric_lines, estimated_ms)
            logger.info(
                "Lyric sync: title='%s' state=%s position=%d estimated=%d "
                "last_pos=%d last_time=%.3f line='%s' next='%s'",
                title, state, position_ms, estimated_ms,
                self._last_position_ms, self._last_play_time,
                lyrics.line, lyrics.next_line
            )

        # 暂停时保留最后已知的 position/duration，不写入 SMTC 返回的 0
        final_position = position_ms
        final_duration = duration_ms
        if state == "paused":
            if position_ms <= 0 and self._paused_position_ms > 0:
                final_position = self._paused_position_ms
            if duration_ms <= 0:
                final_duration = self._snapshot.duration_ms
        # 停止/关闭时也清零
        elif state in ("closed", "stopped"):
            self._paused_position_ms = 0

        self._apply_snapshot(
            PlaybackSnapshot(
                app_running=True,
                state=state,
                title=title,
                artist=artist,
                album=album,
                position_ms=final_position,
                duration_ms=final_duration,
                starting=self._starting,
                lyrics=lyrics,
                updated_at_ms=int(time.time() * 1000),
            )
        )

    def _apply_snapshot(self, snapshot: PlaybackSnapshot) -> None:
        with self._lock:
            self._snapshot = snapshot

    async def _find_netease_session(self) -> Any | None:
        from winsdk.windows.media.control import GlobalSystemMediaTransportControlsSessionManager

        manager = await GlobalSystemMediaTransportControlsSessionManager.request_async()
        for session in manager.get_sessions():
            app_id = (session.source_app_user_model_id or "").lower()
            if any(token.lower() in app_id for token in self.settings.netease_app_ids):
                return session
            try:
                props = await session.try_get_media_properties_async()
            except OSError:
                continue
            artist = (props.artist or "").lower()
            title = (props.title or "").lower()
            if "netease" in artist or "网易" in artist or "cloudmusic" in title:
                return session
        return None

    async def _handle_command(self, command: str) -> bool:
        session = await self._find_netease_session()
        if session is None and command in {"play", "toggle"}:
            if not is_netease_running(self.settings):
                launch_netease()
                await asyncio.sleep(2.0)
            session = await self._find_netease_session()
        if session is None:
            return False

        normalized = command.lower()
        if normalized == "play":
            return bool(await session.try_play_async())
        if normalized == "pause":
            return bool(await session.try_pause_async())
        if normalized == "toggle":
            from winsdk.windows.media.control import (
                GlobalSystemMediaTransportControlsSessionPlaybackStatus as _PBStatus,
            )

            raw = session.get_playback_info().playback_status
            is_playing = (
                raw == _PBStatus.PLAYING
                if not isinstance(raw, int)
                else raw == 4
            )
            if is_playing:
                return bool(await session.try_pause_async())
            return bool(await session.try_play_async())
        if normalized == "next":
            return bool(await session.try_skip_next_async())
        if normalized == "previous":
            return bool(await session.try_skip_previous_async())
        return False

    def _ensure_lyrics(self, title: str, artist: str, duration_ms: int) -> None:
        key = f"{title}\0{artist}"
        if key == self._lyric_key and self._lyric_lines:
            return
        raw_text, lines = self._fetch_lyrics(title, artist, duration_ms)
        self._lyric_key = key
        self._lyric_lines = lines
        self._lyric_raw_text = raw_text

    def _fetch_lyrics(self, title: str, artist: str, duration_ms: int) -> tuple[str, list[LyricLine]]:
        song_id = self._search_song_id(title, artist)
        if song_id is None:
            logger.warning("Song search failed: title='%s' artist='%s'", title, artist)
            fallback = f"[00:00.00]{title}\n"
            return fallback, [LyricLine(start_ms=0, text=title)]
        logger.info("Fetching lyrics for song_id=%d", song_id)
        try:
            response = self._http.get(
                self.settings.netease_lyric_url,
                params={"id": song_id, "lv": 1, "kv": 1, "tv": -1},
            )
            response.raise_for_status()
            payload = response.json()
        except httpx.HTTPError as exc:
            logger.warning("Lyric fetch failed: %s", exc)
            fallback = f"[00:00.00]{title}\n"
            return fallback, [LyricLine(start_ms=0, text=title)]

        lrc_text = ""
        lrc = payload.get("lrc") or {}
        if isinstance(lrc, dict):
            lrc_text = lrc.get("lyric") or ""
        logger.info("LRC text length: %d", len(lrc_text))
        if not lrc_text:
            tlyric = payload.get("tlyric") or {}
            if isinstance(tlyric, dict):
                lrc_text = tlyric.get("lyric") or ""
            logger.info("TLYRIC text length: %d", len(lrc_text))
        if not lrc_text:
            logger.info("Raw lyric payload keys: %s", list(payload.keys()))
            if isinstance(lrc, dict):
                logger.info("LRC dict keys: %s", list(lrc.keys()))
                lyric_val = lrc.get("lyric")
                logger.info("LRC lyric type: %s, repr: %s", type(lyric_val).__name__, repr(str(lyric_val)[:200]))
            if isinstance(tlyric, dict):
                logger.info("TLYRIC dict keys: %s", list(tlyric.keys()))
            klyric = payload.get("klyric") or {}
            if isinstance(klyric, dict):
                klyric_text = klyric.get("lyric") or ""
                logger.info("KLYRIC text length: %d, repr: %s", len(klyric_text), repr(str(klyric_text)[:200]))
        lines = parse_lrc(lrc_text)
        logger.info("Parsed %d lyric lines", len(lines))
        if lines:
            return lrc_text, lines
        fallback = f"[00:00.00]{title}\n"
        return fallback, [LyricLine(start_ms=0, text=title)]

    def _search_song_id(self, title: str, artist: str) -> int | None:
        query = f"{title} {artist}".strip()
        try:
            response = self._http.post(
                self.settings.netease_search_url,
                params={"s": query, "type": 1, "limit": 5},
            )
            response.raise_for_status()
            payload = response.json()
        except httpx.HTTPError as exc:
            logger.warning("Song search failed: %s", exc)
            return None

        songs = (((payload.get("result") or {}).get("songs")) or [])
        if not songs:
            return None
        for song in songs:
            song_title = (song.get("name") or "").strip()
            if song_title == title or title in song_title:
                return int(song["id"])
        return int(songs[0]["id"])
