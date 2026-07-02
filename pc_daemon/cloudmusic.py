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

    def close(self) -> None:
        self._http.close()

    def get_snapshot(self) -> PlaybackSnapshot:
        with self._lock:
            return PlaybackSnapshot(**self._snapshot.__dict__)

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
        status = str(playback.playback_status).split(".")[-1].lower()
        state = "paused" if status == "paused" else "playing" if status == "playing" else "stopped"

        position_ms = int(timeline.position.total_seconds() * 1000) if timeline.position else 0
        duration_ms = int(timeline.end_time.total_seconds() * 1000) if timeline.end_time else 0

        lyrics = LyricSnapshot()
        if title:
            self._ensure_lyrics(title, artist, duration_ms)
            lyrics = pick_lines(self._lyric_lines, position_ms)

        self._apply_snapshot(
            PlaybackSnapshot(
                app_running=True,
                state=state,
                title=title,
                artist=artist,
                album=album,
                position_ms=position_ms,
                duration_ms=duration_ms,
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
            info = session.get_playback_info()
            status = str(info.playback_status).split(".")[-1].lower()
            if status == "playing":
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
        lines = self._fetch_lyrics(title, artist, duration_ms)
        self._lyric_key = key
        self._lyric_lines = lines

    def _fetch_lyrics(self, title: str, artist: str, duration_ms: int) -> list[LyricLine]:
        song_id = self._search_song_id(title, artist)
        if song_id is None:
            return [LyricLine(start_ms=0, text=title)]
        try:
            response = self._http.get(
                self.settings.netease_lyric_url,
                params={"id": song_id, "lv": 1, "kv": 1, "tv": -1},
            )
            response.raise_for_status()
            payload = response.json()
        except httpx.HTTPError as exc:
            logger.warning("Lyric fetch failed: %s", exc)
            return [LyricLine(start_ms=0, text=title)]

        lrc_text = ""
        lrc = payload.get("lrc") or {}
        if isinstance(lrc, dict):
            lrc_text = lrc.get("lyric") or ""
        if not lrc_text:
            tlyric = payload.get("tlyric") or {}
            if isinstance(tlyric, dict):
                lrc_text = tlyric.get("lyric") or ""
        lines = parse_lrc(lrc_text)
        if lines:
            return lines
        if duration_ms > 0:
            return [LyricLine(start_ms=0, text=title)]
        return [LyricLine(start_ms=0, text=title)]

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
