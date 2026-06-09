from __future__ import annotations

import asyncio
import io
import json
import logging
import re
import threading
import time
import urllib.parse
import urllib.request
from ctypes import windll
from dataclasses import dataclass
from pathlib import Path
from typing import AsyncIterator

from PIL import Image

from ai_manager_daemon.netease_cdp_progress import CdpProgressSnapshot, NeteaseCdpProgressReader
from ai_manager_daemon.zh_util import to_simplified

logger = logging.getLogger("uvicorn.error")

VK_MEDIA_NEXT_TRACK = 0xB0
VK_MEDIA_PREV_TRACK = 0xB1
VK_MEDIA_PLAY_PAUSE = 0xB3
VK_VOLUME_DOWN = 0xAE
VK_VOLUME_UP = 0xAF
KEYEVENTF_KEYUP = 0x0002
DEFAULT_DURATION_MS = 240_000
PLAYBACK_STATE_PATH = Path(__file__).resolve().parents[1] / ".runtime" / "playback_state.json"
NETEASE_SEARCH_URL = "https://music.163.com/api/search/get/web"
NETEASE_LYRIC_URL = "https://music.163.com/api/song/lyric"
NETEASE_ALBUM_URL = "https://music.163.com/api/album/{album_id}"
NETEASE_SONG_DETAIL_URL = "https://music.163.com/api/song/detail/?ids=[{song_id}]"
HTTP_HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "Referer": "https://music.163.com/",
    "Cookie": "os=pc; appver=2.10.12;",
}
LRC_TIME_RE = re.compile(r"\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]")
COVER_SIZE = 96
SEEK_JUMP_THRESHOLD_MS = 2500
SEEK_BACKWARD_THRESHOLD_MS = 500
CDP_SEEK_JUMP_THRESHOLD_MS = 400
CDP_SEEK_BACKWARD_THRESHOLD_MS = 200
USER_SEEK_GRACE_SECONDS = 8.0
USER_SEEK_CDP_CONFIRM_TOLERANCE_MS = 3500
CONTROL_COMMAND_DEBOUNCE_SECONDS = 2.0
MIN_SONG_MATCH_SCORE = 9
NETEASE_APP_ID_HINTS = ("cloudmusic", "netease", "orpheus", "163")
LYRIC_META_PREFIXES = ("作词", "作曲", "编曲", "制作", "混音", "母带", "出品", "发行")
LYRICS_FETCH_RETRY_SECONDS = 15.0
LYRICS_CACHE_TRIGGER_PATH = Path(__file__).resolve().parents[1] / ".runtime" / "clear_lyrics_cache.json"


@dataclass(frozen=True)
class PlaybackState:
    state: str
    title: str
    artist: str
    album: str
    position_ms: int
    duration_ms: int


@dataclass(frozen=True)
class LyricLine:
    prev2_line: str
    prev_line: str
    line: str
    next_line: str
    next2_line: str
    position_ms: int
    duration_ms: int
    line_start_ms: int
    line_end_ms: int


@dataclass(frozen=True)
class TrackInfo:
    state: str
    title: str
    artist: str
    album: str
    position_ms: int
    duration_ms: int


@dataclass(frozen=True)
class TimedLyric:
    start_ms: int
    text: str


@dataclass(frozen=True)
class CoverArt:
    track_key: str
    width: int
    height: int
    rgb565_le: bytes


class NeteaseCloudMusicController:
    def __init__(
        self,
        playback_poll_interval_seconds: float,
        lyric_poll_interval_seconds: float,
        lyric_offset_ms: int = 0,
        process_name: str = "cloudmusic.exe",
        progress_source: str = "auto",
        cdp_port: int = 9222,
    ) -> None:
        self.playback_poll_interval_seconds = max(playback_poll_interval_seconds, 0.5)
        self.lyric_poll_interval_seconds = max(lyric_poll_interval_seconds, 0.15)
        self.lyric_offset_ms = lyric_offset_ms
        self.process_name = process_name.strip().lower()
        self.progress_source = progress_source.strip().lower() or "auto"
        self.cdp_port = max(int(cdp_port), 1)
        self._cdp_reader: NeteaseCdpProgressReader | None = None
        if self.progress_source in {"auto", "cdp"}:
            self._cdp_reader = NeteaseCdpProgressReader(
                port=self.cdp_port,
                poll_interval_seconds=min(self.lyric_poll_interval_seconds, 0.2),
            )
            self._cdp_reader.start()
        self._last_track: TrackInfo | None = None
        self._lyrics_cache: dict[str, list[TimedLyric]] = {}
        self._lyrics_fetch_failed_at: dict[str, float] = {}
        self._song_meta_cache: dict[str, tuple[int, str, int]] = {}
        self._last_lyric_song_id = ""
        self._meta_resolve_lock = threading.Lock()
        self._cover_cache: dict[str, bytes] = {}
        self._last_logged_track_key = ""
        self._logged_no_session = False
        self._logged_empty_title = False
        self._fallback_track_key = ""
        self._position_anchor_ms = 0
        self._position_anchor_mono = 0.0
        self._last_reported_media_ms = -1
        self._last_timeline_signature = ""
        self._lyric_seek_revision = 0
        self._manual_pause_override: bool | None = None
        self._last_control_command = ""
        self._last_control_command_mono = 0.0
        self._last_state_save_at = 0.0
        self._timing_lock = threading.RLock()
        self._wired_session: object | None = None
        self._wired_session_id = ""
        self._timeline_changed_token = None
        self._playback_changed_token = None
        self._timeline_changed_handler = None
        self._playback_changed_handler = None
        self._last_timeline_poll_ms = -1
        self._latest_timeline_ms = -1
        self._timeline_poll_interval_seconds = min(self.lyric_poll_interval_seconds, 0.25)
        self._user_seek_target_ms = -1
        self._user_seek_grace_until_mono = 0.0

    def handle_command(self, command: str, position_ms: int | None = None) -> None:
        if command in {"toggle", "play", "pause"} and self._should_ignore_control_command(command):
            logger.info("Ignore duplicate music command within debounce window: %s", command)
            return

        logger.info("Handle music command: %s position_ms=%s", command, position_ms)

        if command == "toggle":
            self._send_media_key(VK_MEDIA_PLAY_PAUSE)
            with self._timing_lock:
                if self._manual_pause_override is not None:
                    self._manual_pause_override = not self._manual_pause_override
                else:
                    currently_playing = False
                    if self._last_track is not None:
                        currently_playing = self._is_track_playing(self._last_track)
                    elif self._fallback_track_key:
                        persisted = self._read_persisted_is_playing(self._fallback_track_key)
                        if persisted is not None:
                            currently_playing = persisted
                    self._manual_pause_override = currently_playing
                self._persist_current_position_now(not self._manual_pause_override)
        elif command == "play":
            with self._timing_lock:
                send_key = self._manual_pause_override is not False
                self._manual_pause_override = False
            if send_key:
                self._send_media_key(VK_MEDIA_PLAY_PAUSE)
            self._persist_current_position_now(True)
        elif command == "pause":
            with self._timing_lock:
                send_key = self._manual_pause_override is not True
                self._manual_pause_override = True
            if send_key:
                self._send_media_key(VK_MEDIA_PLAY_PAUSE)
            self._persist_current_position_now(False)
        elif command == "next":
            self._send_media_key(VK_MEDIA_NEXT_TRACK)
            self._reset_fallback_timing()
        elif command == "previous":
            self._send_media_key(VK_MEDIA_PREV_TRACK)
            self._reset_fallback_timing()
        elif command == "volume_up":
            self._send_media_key(VK_VOLUME_UP)
        elif command == "volume_down":
            self._send_media_key(VK_VOLUME_DOWN)
        elif command == "seek":
            raise ValueError("seek must be handled via apply_user_seek()")
        else:
            raise ValueError(f"Unsupported command: {command}")

    def peek_last_track(self) -> TrackInfo | None:
        with self._timing_lock:
            return self._last_track

    async def apply_user_seek(self, position_ms: int) -> LyricLine | None:
        duration_ms = DEFAULT_DURATION_MS
        track: TrackInfo | None = None
        with self._timing_lock:
            track = self._last_track
            if track is not None and track.duration_ms > 0:
                duration_ms = track.duration_ms
            position_ms = min(max(int(position_ms), 0), duration_ms)
            is_playing = self._manual_pause_override is not False
            if track is not None:
                is_playing = self._is_track_playing(track)
            self._arm_user_seek_grace_unlocked(position_ms)
            self._apply_seek_resync_unlocked(position_ms, "user seek", position_ms)
            key = ""
            if track is not None:
                key = self._track_key(track.title, track.artist)
            elif self._fallback_track_key:
                key = self._fallback_track_key
            if key:
                self._save_persisted_position(key, duration_ms, is_playing, time.time(), position_ms=position_ms)

        asyncio.create_task(self._run_seek_transport(position_ms))

        if track is None:
            return None
        return self._build_lyric_line_at_cached(track, position_ms)

    async def _run_seek_transport(self, position_ms: int) -> None:
        if self._cdp_reader is not None:
            cdp_ok, media_ok = await asyncio.gather(
                asyncio.to_thread(self._cdp_reader.seek_to_ms, position_ms),
                self._seek_media_session_async(position_ms),
            )
        else:
            cdp_ok = False
            media_ok = await self._seek_media_session_async(position_ms)
        cdp_ok = bool(cdp_ok)
        media_ok = bool(media_ok)
        if not cdp_ok and not media_ok:
            logger.warning("Seek transport failed (CDP + media session): %sms", position_ms)
            if self._cdp_reader is not None:
                retry_ok = await asyncio.to_thread(self._cdp_reader.seek_to_ms, position_ms)
                if retry_ok:
                    logger.info("Seek transport retry succeeded: %sms", position_ms)

    async def _seek_media_session_async(self, position_ms: int) -> bool:
        session = self._wired_session
        if session is None:
            return False
        try:
            operation = session.try_change_playback_position_async(int(position_ms))
            result = await operation
            logger.info("Media session seek to %sms ok=%s", position_ms, result)
            return bool(result)
        except Exception:
            logger.debug("Media session seek failed", exc_info=True)
            return False

    def _build_lyric_line_at_cached(self, track: TrackInfo, position_ms: int) -> LyricLine:
        song_id_str = self._preferred_song_id()
        track_key = self._track_key(track.title, track.artist)
        cache_key = self._lyrics_cache_key(track_key, track.duration_ms, song_id_str)
        lyrics = self._lyrics_cache.get(cache_key, [])
        return self._lyric_line_from_timed(lyrics, track, position_ms)

    def _lyric_line_from_timed(self, lyrics: list[TimedLyric], track: TrackInfo, position_ms: int) -> LyricLine:
        lyric_position_ms = max(position_ms + self.lyric_offset_ms, 0)
        index = self._find_lyric_index(lyrics, lyric_position_ms)
        if not lyrics:
            return LyricLine(
                prev2_line="",
                prev_line="",
                line="未找到该歌曲的时间轴歌词",
                next_line="",
                next2_line="",
                position_ms=lyric_position_ms,
                duration_ms=track.duration_ms,
                line_start_ms=lyric_position_ms,
                line_end_ms=track.duration_ms,
            )

        line_start = lyrics[index].start_ms
        if index + 1 < len(lyrics):
            line_end = max(line_start, lyrics[index + 1].start_ms - 100)
        else:
            line_end = track.duration_ms

        return LyricLine(
            prev2_line=self._lyric_text_at(lyrics, index - 2),
            prev_line=self._lyric_text_at(lyrics, index - 1),
            line=lyrics[index].text,
            next_line=self._lyric_text_at(lyrics, index + 1),
            next2_line=self._lyric_text_at(lyrics, index + 2),
            position_ms=lyric_position_ms,
            duration_ms=track.duration_ms,
            line_start_ms=line_start,
            line_end_ms=line_end,
        )

    async def _build_lyric_line_at(self, track: TrackInfo, position_ms: int) -> LyricLine:
        lyrics = await self._get_lyrics(track.title, track.artist, track.duration_ms)
        return self._lyric_line_from_timed(lyrics, track, position_ms)

    def _should_ignore_control_command(self, command: str) -> bool:
        now = time.monotonic()
        if (
            command == self._last_control_command
            and now - self._last_control_command_mono < CONTROL_COMMAND_DEBOUNCE_SECONDS
        ):
            return True
        self._last_control_command = command
        self._last_control_command_mono = now
        return False

    def _send_media_key(self, virtual_key: int) -> None:
        windll.user32.keybd_event(virtual_key, 0, 0, 0)
        windll.user32.keybd_event(virtual_key, 0, KEYEVENTF_KEYUP, 0)

    async def playback_states(self) -> AsyncIterator[PlaybackState]:
        last_emitted_key = ""
        last_emitted_state = ""
        last_emitted_at = 0.0

        while True:
            track = await self._get_current_track()
            if track is not None:
                self._last_track = track
                key = self._track_key(track.title, track.artist)
                if key != self._last_logged_track_key:
                    logger.info(
                        "Current media session: title=%s artist=%s album=%s duration_ms=%s position_ms=%s state=%s",
                        track.title,
                        track.artist,
                        track.album,
                        track.duration_ms,
                        track.position_ms,
                        track.state,
                    )
                    self._last_logged_track_key = key
                    asyncio.create_task(
                        asyncio.to_thread(self._resolve_song_meta, track.title, track.artist, track.duration_ms)
                    )

                now = time.monotonic()
                urgent = key != last_emitted_key or track.state != last_emitted_state
                periodic = now - last_emitted_at >= self.playback_poll_interval_seconds
                if urgent or periodic:
                    last_emitted_key = key
                    last_emitted_state = track.state
                    last_emitted_at = now
                    yield PlaybackState(
                        state=track.state,
                        title=track.title,
                        artist=track.artist,
                        album=track.album,
                        position_ms=track.position_ms,
                        duration_ms=track.duration_ms,
                    )

            await asyncio.sleep(self.playback_poll_interval_seconds)

    async def timeline_poll_loop(self) -> None:
        logger.info("Timeline poll loop started (interval=%.2fs)", self._timeline_poll_interval_seconds)
        while True:
            await asyncio.sleep(self._timeline_poll_interval_seconds)
            self._poll_lyrics_cache_trigger()
            session = self._wired_session
            if session is None:
                continue
            try:
                timeline = session.get_timeline_properties()
                position_ms = self._read_timeline_position_ms(timeline)
                if position_ms <= 0:
                    continue
                with self._timing_lock:
                    self._latest_timeline_ms = position_ms
                self._check_timeline_poll_seek(position_ms)
            except Exception:
                logger.exception("Timeline poll failed")

    def _lyric_text_at(self, lyrics: list[TimedLyric], index: int) -> str:
        if 0 <= index < len(lyrics):
            return lyrics[index].text
        return ""

    async def _build_lyric_line(self, track: TrackInfo) -> LyricLine:
        lyrics = await self._get_lyrics(track.title, track.artist, track.duration_ms)
        lyric_position_ms = max(track.position_ms + self.lyric_offset_ms, 0)
        index = self._find_lyric_index(lyrics, lyric_position_ms)
        if not lyrics:
            return LyricLine(
                prev2_line="",
                prev_line="",
                line="未找到该歌曲的时间轴歌词",
                next_line="",
                next2_line="",
                position_ms=lyric_position_ms,
                duration_ms=track.duration_ms,
                line_start_ms=lyric_position_ms,
                line_end_ms=track.duration_ms,
            )

        line_start = lyrics[index].start_ms
        if index + 1 < len(lyrics):
            line_end = max(line_start, lyrics[index + 1].start_ms - 100)
        else:
            line_end = track.duration_ms

        return LyricLine(
            prev2_line=self._lyric_text_at(lyrics, index - 2),
            prev_line=self._lyric_text_at(lyrics, index - 1),
            line=lyrics[index].text,
            next_line=self._lyric_text_at(lyrics, index + 1),
            next2_line=self._lyric_text_at(lyrics, index + 2),
            position_ms=lyric_position_ms,
            duration_ms=track.duration_ms,
            line_start_ms=line_start,
            line_end_ms=line_end,
        )

    async def fetch_playback_snapshot(self) -> tuple[PlaybackState | None, LyricLine | None]:
        track = await self.resync_playback_timing()
        if track is None:
            return None, None

        playback = PlaybackState(
            state=track.state,
            title=track.title,
            artist=track.artist,
            album=track.album,
            position_ms=track.position_ms,
            duration_ms=track.duration_ms,
        )
        lyric = await self._build_lyric_line(track)
        return playback, lyric

    async def fetch_cover_art(self, title: str, artist: str) -> CoverArt | None:
        key = self._track_key(title, artist)
        if key in self._cover_cache:
            rgb565 = self._cover_cache[key]
            return CoverArt(track_key=key, width=COVER_SIZE, height=COVER_SIZE, rgb565_le=rgb565)

        cover = await asyncio.to_thread(self._fetch_cover_rgb565, title, artist)
        if cover is None:
            return None

        self._cover_cache[key] = cover
        if len(self._cover_cache) > 3:
            oldest_key = next(iter(self._cover_cache))
            if oldest_key != key:
                del self._cover_cache[oldest_key]

        return CoverArt(track_key=key, width=COVER_SIZE, height=COVER_SIZE, rgb565_le=cover)

    def drop_cover_cache_except(self, keep_key: str = "") -> None:
        if not keep_key:
            self._cover_cache.clear()
            return
        self._cover_cache = {keep_key: self._cover_cache[keep_key]} if keep_key in self._cover_cache else {}

    async def resync_playback_timing(self) -> TrackInfo | None:
        track = await self._read_media_session_track()
        if track is None:
            return None

        with self._timing_lock:
            normalized = self._normalize_track_timing(track)
            self._save_persisted_position(
                self._track_key(track.title, track.artist),
                normalized.duration_ms,
                normalized.state == "playing",
                time.monotonic(),
                force=True,
                position_ms=normalized.position_ms,
            )
            logger.info(
                "Resynced playback timing: %s - %s position_ms=%s duration_ms=%s",
                normalized.title,
                normalized.artist,
                normalized.position_ms,
                normalized.duration_ms,
            )
            return normalized

    def _lyric_poll_interval(self) -> float:
        if self._cdp_reader is not None and self._cdp_reader.connected:
            return min(self.lyric_poll_interval_seconds, 0.2)
        return self.lyric_poll_interval_seconds

    @staticmethod
    def _parse_desktop_song_id(raw: str) -> int | None:
        text = str(raw or "").strip()
        if not text:
            return None
        if text.isdigit():
            return int(text)
        match = re.match(r"^(\d+)", text)
        if match:
            return int(match.group(1))
        return None

    def _preferred_song_id(self) -> str:
        cdp = self._cdp_snapshot()
        if cdp is None:
            return ""
        parsed = self._parse_desktop_song_id(str(cdp.song_id or ""))
        if parsed is not None:
            return str(parsed)
        return ""

    def _lyrics_cache_key(self, track_key: str, duration_ms: int, song_id: str = "") -> str:
        if song_id.isdigit():
            return f"id:{song_id}"
        if duration_ms > 1000:
            return f"{track_key}:d{duration_ms // 1000}"
        return f"{track_key}:pending"

    def _should_fetch_lyrics(self, cache_key: str) -> bool:
        if cache_key in self._lyrics_cache:
            return False
        failed_at = self._lyrics_fetch_failed_at.get(cache_key)
        if failed_at is None:
            return True
        return time.monotonic() - failed_at >= LYRICS_FETCH_RETRY_SECONDS

    def _store_lyrics_cache(self, cache_key: str, lyrics: list[TimedLyric], *, fetch_ok: bool) -> list[TimedLyric]:
        if not fetch_ok:
            self._lyrics_fetch_failed_at[cache_key] = time.monotonic()
            logger.warning("Lyrics fetch failed for cache_key=%s, will retry in %ss", cache_key, LYRICS_FETCH_RETRY_SECONDS)
            return []
        self._lyrics_fetch_failed_at.pop(cache_key, None)
        self._lyrics_cache[cache_key] = lyrics
        return lyrics

    def _drop_lyrics_cache_keys(self, cache_keys: list[str]) -> int:
        removed = 0
        for cache_key in cache_keys:
            if cache_key in self._lyrics_cache:
                del self._lyrics_cache[cache_key]
                removed += 1
            if cache_key in self._lyrics_fetch_failed_at:
                del self._lyrics_fetch_failed_at[cache_key]
                removed += 1
        return removed

    def clear_lyrics_cache(
        self,
        *,
        song_ids: list[int] | None = None,
        tracks: list[dict[str, str]] | None = None,
        clear_all: bool = False,
    ) -> int:
        if clear_all:
            removed = len(self._lyrics_cache) + len(self._lyrics_fetch_failed_at)
            self._lyrics_cache.clear()
            self._lyrics_fetch_failed_at.clear()
            logger.info("Cleared all lyrics cache entries (%s)", removed)
            return removed

        removed = 0
        if song_ids:
            for raw_id in song_ids:
                try:
                    cache_key = f"id:{int(raw_id)}"
                except (TypeError, ValueError):
                    continue
                removed += self._drop_lyrics_cache_keys([cache_key])

        if tracks:
            for item in tracks:
                title = str(item.get("title", "")).strip()
                artist = str(item.get("artist", "")).strip()
                if not title:
                    continue
                track_key = self._track_key(title, artist)
                stale_keys = [
                    cache_key
                    for cache_key in list(self._lyrics_cache)
                    if cache_key == track_key
                    or cache_key.startswith(f"{track_key}:")
                    or cache_key.startswith("id:")
                ]
                stale_keys.extend(
                    cache_key
                    for cache_key in list(self._lyrics_fetch_failed_at)
                    if cache_key == track_key
                    or cache_key.startswith(f"{track_key}:")
                    or cache_key.startswith("id:")
                )
                removed += self._drop_lyrics_cache_keys(list(dict.fromkeys(stale_keys)))

        if removed:
            logger.info("Cleared lyrics cache entries: %s", removed)
        return removed

    def _poll_lyrics_cache_trigger(self) -> None:
        if not LYRICS_CACHE_TRIGGER_PATH.exists():
            return
        try:
            payload = json.loads(LYRICS_CACHE_TRIGGER_PATH.read_text(encoding="utf-8-sig"))
        except (OSError, json.JSONDecodeError):
            logger.exception("Failed to read lyrics cache trigger: %s", LYRICS_CACHE_TRIGGER_PATH)
            LYRICS_CACHE_TRIGGER_PATH.unlink(missing_ok=True)
            return

        removed = self.clear_lyrics_cache(
            song_ids=payload.get("song_ids"),
            tracks=payload.get("tracks"),
            clear_all=bool(payload.get("clear_all")),
        )
        LYRICS_CACHE_TRIGGER_PATH.unlink(missing_ok=True)
        logger.info("Processed lyrics cache trigger, removed=%s", removed)

    def _invalidate_lyrics_for_track(self, track_key: str) -> None:
        stale_keys = [
            cache_key
            for cache_key in list(self._lyrics_cache)
            if cache_key == track_key
            or cache_key.startswith(f"{track_key}:")
            or cache_key.startswith("id:")
        ]
        stale_keys.extend(
            cache_key
            for cache_key in list(self._lyrics_fetch_failed_at)
            if cache_key == track_key
            or cache_key.startswith(f"{track_key}:")
            or cache_key.startswith("id:")
        )
        self._drop_lyrics_cache_keys(list(dict.fromkeys(stale_keys)))
        stale_meta = [
            meta_key
            for meta_key in list(self._song_meta_cache)
            if meta_key == track_key or meta_key.startswith(f"{track_key}:") or meta_key.startswith("id:")
        ]
        for meta_key in stale_meta:
            del self._song_meta_cache[meta_key]

    async def lyric_lines(self) -> AsyncIterator[LyricLine]:
        last_key = ""
        last_song_id = ""
        last_duration_ms = -1
        last_index = -1
        last_seek_revision = -1
        while True:
            track = await self._get_current_track()
            if track is None:
                await asyncio.sleep(self._lyric_poll_interval())
                continue

            key = self._track_key(track.title, track.artist)
            song_id = self._preferred_song_id()
            duration_ms = track.duration_ms if track.duration_ms > 1000 else 0

            song_id_became_available = bool(song_id) and not last_song_id
            if key != last_key or song_id_became_available or (song_id and song_id != last_song_id):
                if key == last_key and song_id_became_available:
                    self._invalidate_lyrics_for_track(key)
                    logger.info(
                        "Desktop song id now available for %s - %s: id=%s, refetch lyrics",
                        track.title,
                        track.artist,
                        song_id,
                    )
                logger.info(
                    "Resolve lyrics for: %s - %s (song_id=%s duration_ms=%s)",
                    track.title,
                    track.artist,
                    song_id or "search",
                    duration_ms,
                )
                last_key = key
                last_song_id = song_id
                last_duration_ms = duration_ms
                last_index = -2
            elif (
                duration_ms > 1000
                and last_duration_ms > 1000
                and abs(duration_ms - last_duration_ms) > 8000
            ):
                logger.info(
                    "Duration changed for %s - %s: %sms -> %sms, refetch lyrics",
                    track.title,
                    track.artist,
                    last_duration_ms,
                    duration_ms,
                )
                self._invalidate_lyrics_for_track(key)
                last_duration_ms = duration_ms
                last_index = -2
            elif duration_ms > 1000 and last_duration_ms <= 1000:
                logger.info(
                    "Duration now known for %s - %s: %sms, refetch lyrics",
                    track.title,
                    track.artist,
                    duration_ms,
                )
                self._invalidate_lyrics_for_track(key)
                last_duration_ms = duration_ms
                last_index = -2

            lyric = await self._build_lyric_line(track)
            lyrics = await self._get_lyrics(track.title, track.artist, track.duration_ms)
            lyric_position_ms = max(track.position_ms + self.lyric_offset_ms, 0)
            index = self._find_lyric_index(lyrics, lyric_position_ms)

            with self._timing_lock:
                seek_revision = self._lyric_seek_revision

            if index != last_index or seek_revision != last_seek_revision:
                yield lyric
                last_index = index
                last_seek_revision = seek_revision
                logger.info(
                    "Push lyric @%sms (idx=%s seek_rev=%s): %s / %s / %s / %s / %s",
                    lyric_position_ms,
                    index,
                    seek_revision,
                    lyric.prev2_line,
                    lyric.prev_line,
                    lyric.line,
                    lyric.next_line,
                    lyric.next2_line,
                )

            await asyncio.sleep(self._lyric_poll_interval())

    async def _get_current_track(self) -> TrackInfo | None:
        try:
            track = await asyncio.wait_for(self._read_media_session_track(), timeout=2.0)
        except asyncio.TimeoutError:
            logger.debug("Timed out reading Windows media session")
            return None
        except asyncio.CancelledError:
            raise
        if track is None:
            return None

        with self._timing_lock:
            normalized = self._normalize_track_timing(track)
            self._last_track = normalized
            return normalized

    async def _read_media_session_track(self) -> TrackInfo | None:
        try:
            from winsdk.windows.media.control import GlobalSystemMediaTransportControlsSessionManager
        except ImportError:
            logger.warning("winsdk is not installed; run: pip install -r requirements.txt")
            return None

        try:
            from winsdk.windows.media.control import GlobalSystemMediaTransportControlsSessionPlaybackStatus

            manager = await GlobalSystemMediaTransportControlsSessionManager.request_async()
            session = self._pick_media_session(manager, GlobalSystemMediaTransportControlsSessionPlaybackStatus)
            if session is None:
                if not self._logged_no_session:
                    logger.warning("No Windows media session found; start playback in NetEase Cloud Music first")
                    self._logged_no_session = True
                return None

            self._ensure_timeline_listeners(session)

            properties = await session.try_get_media_properties_async()
            timeline = session.get_timeline_properties()
            playback_info = session.get_playback_info()
            position_ms = self._read_timeline_position_ms(timeline)
            self._check_timeline_signature(position_ms, timeline)

            title = to_simplified((properties.title or "").strip())
            artist = to_simplified((properties.artist or "").strip())
            album = to_simplified((properties.album_title or "").strip())
            if not title:
                if not self._logged_empty_title:
                    logger.warning("Windows media session exists but title is empty")
                    self._logged_empty_title = True
                return None

            self._logged_no_session = False
            self._logged_empty_title = False
            duration_ms = self._read_timeline_duration_ms(timeline)
            track = TrackInfo(
                state=self._playback_status_name(playback_info),
                title=title,
                artist=artist,
                album=album,
                position_ms=position_ms,
                duration_ms=duration_ms,
            )
            return track
        except Exception:
            logger.exception("Failed to read Windows media session")
            return None

    def _pick_media_session(self, manager: object, playback_status_enum: object) -> object | None:
        sessions = manager.get_sessions()
        netease_sessions: list[object] = []
        if sessions is not None:
            for session in sessions:
                if self._is_netease_session(session):
                    netease_sessions.append(session)

        if netease_sessions:
            playing_status = getattr(playback_status_enum, "PLAYING", 4)
            for session in netease_sessions:
                playback_info = session.get_playback_info()
                if playback_info is not None and playback_info.playback_status == playing_status:
                    return session
            return netease_sessions[0]

        return manager.get_current_session()

    def _is_netease_session(self, session: object) -> bool:
        app_id = str(getattr(session, "source_app_user_model_id", "") or "").lower()
        if self.process_name and self.process_name.replace(".exe", "") in app_id:
            return True
        return any(hint in app_id for hint in NETEASE_APP_ID_HINTS)

    def _teardown_timeline_listeners(self) -> None:
        session = self._wired_session
        if session is None:
            return
        try:
            if self._timeline_changed_token is not None:
                session.remove_timeline_properties_changed(self._timeline_changed_token)
            if self._playback_changed_token is not None:
                session.remove_playback_info_changed(self._playback_changed_token)
        except Exception:
            logger.debug("Failed to remove media session listeners", exc_info=True)
        self._wired_session = None
        self._wired_session_id = ""
        self._timeline_changed_token = None
        self._playback_changed_token = None
        self._timeline_changed_handler = None
        self._playback_changed_handler = None

    def _ensure_timeline_listeners(self, session: object) -> None:
        session_id = str(getattr(session, "source_app_user_model_id", "") or "")
        if session_id and session_id == self._wired_session_id:
            return

        self._teardown_timeline_listeners()

        def on_timeline_changed(sender: object, _args: object) -> None:
            self._on_session_timeline_changed(sender)

        def on_playback_changed(sender: object, _args: object) -> None:
            self._on_session_playback_changed(sender)

        self._timeline_changed_handler = on_timeline_changed
        self._playback_changed_handler = on_playback_changed

        try:
            self._timeline_changed_token = session.add_timeline_properties_changed(self._timeline_changed_handler)
            self._playback_changed_token = session.add_playback_info_changed(self._playback_changed_handler)
            self._wired_session = session
            self._wired_session_id = session_id
            logger.info("Registered media session listeners: %s", session_id)
        except Exception:
            logger.exception("Failed to register media session listeners")

    def _read_timeline_position_ms(self, timeline: object) -> int:
        return max(self._timespan_to_ms(getattr(timeline, "position", 0)), 0)

    def _read_timeline_duration_ms(self, timeline: object) -> int:
        return max(self._timespan_to_ms(getattr(timeline, "end_time", 0)), 1)

    def _timeline_signature(self, timeline: object, position_ms: int) -> str:
        updated = getattr(timeline, "last_updated_time", None)
        return f"{position_ms}:{updated}"

    def _arm_user_seek_grace_unlocked(self, position_ms: int) -> None:
        self._user_seek_target_ms = int(position_ms)
        self._user_seek_grace_until_mono = time.monotonic() + USER_SEEK_GRACE_SECONDS

    def _in_user_seek_grace_unlocked(self, now: float) -> bool:
        return self._user_seek_target_ms >= 0 and now < self._user_seek_grace_until_mono

    def _clear_user_seek_grace_unlocked(self) -> None:
        self._user_seek_target_ms = -1
        self._user_seek_grace_until_mono = 0.0

    def _should_ignore_cdp_progress_resync_unlocked(self, cdp_position_ms: int, now: float) -> bool:
        if not self._in_user_seek_grace_unlocked(now):
            return False
        target = self._user_seek_target_ms
        if abs(cdp_position_ms - target) <= USER_SEEK_CDP_CONFIRM_TOLERANCE_MS:
            self._clear_user_seek_grace_unlocked()
            return False
        return True

    def _apply_seek_resync_unlocked(self, position_ms: int, reason: str, estimated_ms: int | None = None) -> None:
        if reason == "cdp progress":
            now = time.monotonic()
            with self._timing_lock:
                if self._should_ignore_cdp_progress_resync_unlocked(position_ms, now):
                    logger.debug(
                        "Ignore CDP progress resync during user seek grace (cdp=%sms target=%sms)",
                        position_ms,
                        self._user_seek_target_ms,
                    )
                    return
        if estimated_ms is None:
            estimated_ms = self._estimated_position_ms(True, DEFAULT_DURATION_MS)
        self._set_position_anchor(position_ms)
        self._last_reported_media_ms = position_ms
        self._lyric_seek_revision += 1
        logger.info(
            "Seek detected (%s): resync to %sms (estimated %sms)",
            reason,
            position_ms,
            estimated_ms,
        )

    def _apply_seek_resync(self, position_ms: int, reason: str, estimated_ms: int | None = None) -> None:
        with self._timing_lock:
            self._apply_seek_resync_unlocked(position_ms, reason, estimated_ms)

    def _check_timeline_poll_seek(self, position_ms: int) -> None:
        with self._timing_lock:
            is_playing = self._last_track is None or self._last_track.state == "playing"
            estimated_ms = self._estimated_position_ms(is_playing, DEFAULT_DURATION_MS)

            if self._last_timeline_poll_ms < 0:
                self._last_timeline_poll_ms = position_ms
                return

            step_ms = position_ms - self._last_timeline_poll_ms
            self._last_timeline_poll_ms = position_ms

            seek = False
            reason = ""
            if step_ms <= -SEEK_BACKWARD_THRESHOLD_MS:
                seek = True
                reason = "timeline poll backward"
            elif step_ms >= SEEK_JUMP_THRESHOLD_MS:
                seek = True
                reason = "timeline poll forward"

            if not seek:
                return

            self._apply_seek_resync_unlocked(position_ms, reason, estimated_ms)

    def _check_timeline_signature(self, position_ms: int, timeline: object) -> None:
        signature = self._timeline_signature(timeline, position_ms)
        with self._timing_lock:
            if not self._last_timeline_signature:
                self._last_timeline_signature = signature
                return

            if signature == self._last_timeline_signature:
                return

            self._last_timeline_signature = signature

    def _on_session_timeline_changed(self, session: object) -> None:
        try:
            timeline = session.get_timeline_properties()
            position_ms = self._read_timeline_position_ms(timeline)
            logger.info(
                "Media timeline event: position_ms=%s anchor_ms=%s",
                position_ms,
                self._position_anchor_ms,
            )
            if position_ms <= 0:
                return

            with self._timing_lock:
                step_ms = position_ms - self._position_anchor_ms
                if step_ms <= -SEEK_BACKWARD_THRESHOLD_MS or step_ms >= SEEK_JUMP_THRESHOLD_MS:
                    self._last_timeline_signature = self._timeline_signature(timeline, position_ms)
                    self._last_timeline_poll_ms = position_ms
                    estimated_ms = self._estimated_position_ms(True, DEFAULT_DURATION_MS)
                    self._apply_seek_resync_unlocked(position_ms, "timeline event", estimated_ms)
        except Exception:
            logger.exception("Timeline event handler failed")

    def _on_session_playback_changed(self, session: object) -> None:
        try:
            from winsdk.windows.media.control import GlobalSystemMediaTransportControlsSessionPlaybackStatus

            playback_info = session.get_playback_info()
            if playback_info is None:
                return
            if playback_info.playback_status != GlobalSystemMediaTransportControlsSessionPlaybackStatus.CHANGING:
                return
            self._on_session_timeline_changed(session)
        except Exception:
            logger.exception("Playback event handler failed")

    def _set_position_anchor(self, position_ms: int) -> None:
        self._position_anchor_ms = max(position_ms, 0)
        self._position_anchor_mono = time.monotonic()

    def _estimated_position_ms(self, is_playing: bool, duration_ms: int) -> int:
        position_ms = self._position_anchor_ms
        if is_playing:
            elapsed_ms = int((time.monotonic() - self._position_anchor_mono) * 1000)
            position_ms += max(elapsed_ms, 0)
        if duration_ms > 0:
            position_ms = min(position_ms, duration_ms)
        return max(position_ms, 0)

    def _track_duration_ms(self, track: TrackInfo, key: str) -> int:
        api_duration_ms = self._song_duration_ms(key)
        if api_duration_ms > 1000:
            return api_duration_ms
        if track.duration_ms > 1000:
            return track.duration_ms
        return DEFAULT_DURATION_MS

    def _media_position_ms(self, track: TrackInfo) -> int | None:
        if track.position_ms > 0:
            return max(track.position_ms, 0)
        return None

    def _detect_seek(self, media_ms: int, estimated_ms: int, is_playing: bool) -> bool:
        del estimated_ms, is_playing
        if self._last_reported_media_ms < 0:
            return False

        step_ms = media_ms - self._last_reported_media_ms
        if step_ms <= -SEEK_BACKWARD_THRESHOLD_MS:
            return True
        if step_ms >= SEEK_JUMP_THRESHOLD_MS:
            return True
        return False

    def _cdp_snapshot(self) -> CdpProgressSnapshot | None:
        if self._cdp_reader is None:
            return None
        if self.progress_source == "winsdk":
            return None
        return self._cdp_reader.get_snapshot(max_age_seconds=2.0)

    def _normalize_track_timing(self, track: TrackInfo) -> TrackInfo:
        key = self._track_key(track.title, track.artist)
        now = time.monotonic()
        cdp = self._cdp_snapshot()
        if cdp is not None and cdp.position_ms >= 0:
            return self._normalize_track_timing_from_cdp(track, key, now, cdp)

        duration_ms = self._track_duration_ms(track, key)
        timeline_valid = track.duration_ms > 1000 and track.position_ms > 0
        is_playing = self._is_track_playing(track)
        media_ms = self._media_position_ms(track)
        if media_ms is None and self._latest_timeline_ms > 0:
            media_ms = self._latest_timeline_ms
        estimated_ms = self._estimated_position_ms(is_playing, duration_ms)

        if key != self._fallback_track_key:
            self._fallback_track_key = key
            self._last_reported_media_ms = -1
            self._last_timeline_signature = ""
            self._last_timeline_poll_ms = -1
            self._manual_pause_override = None
            if timeline_valid and media_ms is not None:
                self._set_position_anchor(media_ms)
            else:
                persisted_ms = self._read_persisted_position(key, duration_ms)
                self._set_position_anchor(persisted_ms)
                logger.info("Restored local playback position: %sms", persisted_ms)
            if not timeline_valid:
                logger.warning(
                    "Media timeline is invalid for %s - %s; lyrics use estimated progress",
                    track.title,
                    track.artist,
                )

        seeked = track.state == "changing"
        if media_ms is not None:
            seeked = seeked or self._detect_seek(media_ms, estimated_ms, is_playing)
            self._last_reported_media_ms = media_ms

        if media_ms is not None and (timeline_valid or seeked):
            position_ms = media_ms
            if seeked:
                self._apply_seek_resync_unlocked(
                    position_ms,
                    f"{track.title} - {track.artist}",
                    estimated_ms,
                )
            else:
                self._set_position_anchor(position_ms)
        elif not is_playing:
            position_ms = self._estimated_position_ms(False, duration_ms)
            self._set_position_anchor(position_ms)
        else:
            position_ms = estimated_ms

        self._save_persisted_position(key, duration_ms, is_playing, now, position_ms=position_ms)

        return TrackInfo(
            state="playing" if is_playing else "paused",
            title=track.title,
            artist=track.artist,
            album=track.album,
            position_ms=position_ms,
            duration_ms=duration_ms,
        )

    def _normalize_track_timing_from_cdp(
        self,
        track: TrackInfo,
        key: str,
        now: float,
        cdp: CdpProgressSnapshot,
    ) -> TrackInfo:
        duration_ms = self._track_duration_ms(track, key)
        if cdp.duration_ms > 1000:
            duration_ms = cdp.duration_ms
        elif track.duration_ms > 1000:
            duration_ms = track.duration_ms

        is_playing = self._is_track_playing(track)
        live_position_ms = min(max(cdp.position_ms, 0), duration_ms) if duration_ms > 0 else max(cdp.position_ms, 0)
        if is_playing:
            position_ms = live_position_ms
            with self._timing_lock:
                if self._in_user_seek_grace_unlocked(now):
                    target = self._user_seek_target_ms
                    if abs(live_position_ms - target) > USER_SEEK_CDP_CONFIRM_TOLERANCE_MS:
                        position_ms = target
                        live_position_ms = target
        else:
            position_ms = self._position_anchor_ms if self._position_anchor_ms > 0 else live_position_ms

        if key != self._fallback_track_key:
            self._fallback_track_key = key
            self._last_reported_media_ms = -1
            self._last_timeline_signature = ""
            self._last_timeline_poll_ms = -1
            self._set_position_anchor(position_ms)
            logger.info(
                "CDP playback timing for %s - %s: position_ms=%s duration_ms=%s",
                track.title,
                track.artist,
                position_ms,
                duration_ms,
            )

        if is_playing and self._last_reported_media_ms >= 0:
            step_ms = position_ms - self._last_reported_media_ms
            if step_ms <= -CDP_SEEK_BACKWARD_THRESHOLD_MS or step_ms >= CDP_SEEK_JUMP_THRESHOLD_MS:
                self._apply_seek_resync_unlocked(position_ms, "cdp progress", position_ms)

        self._last_reported_media_ms = position_ms
        if is_playing:
            self._set_position_anchor(position_ms)
        self._save_persisted_position(key, duration_ms, is_playing, now, position_ms=position_ms)

        return TrackInfo(
            state="playing" if is_playing else "paused",
            title=track.title,
            artist=track.artist,
            album=track.album,
            position_ms=position_ms,
            duration_ms=duration_ms,
        )

    def _is_track_playing(self, track: TrackInfo) -> bool:
        if self._manual_pause_override is not None:
            return not self._manual_pause_override

        return self._raw_track_is_playing(track)

    def _raw_track_is_playing(self, track: TrackInfo) -> bool:
        state = track.state.lower()
        if state in {"paused", "stopped", "closed"}:
            return False

        key = self._track_key(track.title, track.artist)
        persisted = self._read_persisted_is_playing(key)
        timeline_valid = track.duration_ms > 1000 and track.position_ms > 0

        # NetEase often keeps winsdk status at playing while timeline stays at 0.
        if not timeline_valid and persisted is not None:
            return persisted

        if state in {"playing", "changing"}:
            return True
        if persisted is not None:
            return persisted

        return False

    def _reset_fallback_timing(self) -> None:
        with self._timing_lock:
            self._last_track = None
            self._fallback_track_key = ""
            self._last_reported_media_ms = -1
            self._last_timeline_signature = ""
            self._last_timeline_poll_ms = -1
            self._latest_timeline_ms = -1
            self._set_position_anchor(0)
            self._manual_pause_override = None
            self._save_persisted_position(
                "",
                DEFAULT_DURATION_MS,
                False,
                time.monotonic(),
                force=True,
                position_ms=0,
            )

    def _persist_current_position_now(self, is_playing: bool) -> None:
        with self._timing_lock:
            if not self._fallback_track_key:
                return

            duration_ms = self._last_track.duration_ms if self._last_track is not None else DEFAULT_DURATION_MS
            position_ms = self._estimated_position_ms(is_playing, duration_ms)
            self._set_position_anchor(position_ms)
            self._save_persisted_position(
                self._fallback_track_key,
                duration_ms,
                is_playing,
                time.monotonic(),
                force=True,
                position_ms=position_ms,
            )

    def _read_persisted_is_playing(self, key: str) -> bool | None:
        if not key:
            return None

        try:
            with PLAYBACK_STATE_PATH.open("r", encoding="utf-8") as file:
                data = json.load(file)
        except (FileNotFoundError, OSError, json.JSONDecodeError):
            return None

        if not isinstance(data, dict) or data.get("track_key") != key:
            return None

        try:
            return bool(data.get("is_playing", False))
        except (TypeError, ValueError):
            return None

    def _read_persisted_position(self, key: str, duration_ms: int) -> int:
        try:
            with PLAYBACK_STATE_PATH.open("r", encoding="utf-8") as file:
                data = json.load(file)
        except (FileNotFoundError, OSError, json.JSONDecodeError):
            return 0

        if not isinstance(data, dict) or data.get("track_key") != key:
            return 0

        try:
            position_ms = int(data.get("position_ms", 0))
            saved_at = float(data.get("saved_at", 0.0))
            was_playing = bool(data.get("is_playing", False))
        except (TypeError, ValueError):
            return 0

        if was_playing and saved_at > 0:
            position_ms += max(int((time.time() - saved_at) * 1000), 0)

        position_ms = max(position_ms, 0)
        if duration_ms > 0:
            position_ms = min(position_ms, duration_ms)

        return position_ms

    def _save_persisted_position(
        self,
        key: str,
        duration_ms: int,
        is_playing: bool,
        now: float,
        force: bool = False,
        position_ms: int | None = None,
    ) -> None:
        if not force and now - self._last_state_save_at < 1.0:
            return

        saved_position_ms = position_ms
        if saved_position_ms is None:
            saved_position_ms = self._estimated_position_ms(is_playing, duration_ms)

        self._last_state_save_at = now
        try:
            PLAYBACK_STATE_PATH.parent.mkdir(parents=True, exist_ok=True)
            with PLAYBACK_STATE_PATH.open("w", encoding="utf-8") as file:
                json.dump(
                    {
                        "track_key": key,
                        "position_ms": saved_position_ms,
                        "duration_ms": duration_ms,
                        "is_playing": is_playing,
                        "saved_at": time.time(),
                    },
                    file,
                    ensure_ascii=False,
                )
            if force:
                logger.info(
                    "Persisted local playback position: key=%s position_ms=%s playing=%s",
                    key,
                    saved_position_ms,
                    is_playing,
                )
        except OSError:
            logger.exception("Failed to persist local playback position")

    def _playback_status_name(self, playback_info: object) -> str:
        status = getattr(playback_info, "playback_status", None)
        name = getattr(status, "name", "")
        return str(name).lower() if name else "unknown"

    def _timespan_to_ms(self, value: object) -> int:
        if hasattr(value, "total_seconds"):
            return int(value.total_seconds() * 1000)
        if hasattr(value, "duration"):
            value = getattr(value, "duration")
        try:
            return int(value) // 10_000
        except (TypeError, ValueError):
            return 0

    async def _get_lyrics(self, title: str, artist: str, duration_ms: int = 0) -> list[TimedLyric]:
        song_id_str = self._preferred_song_id()
        if not song_id_str.isdigit() and self._cdp_reader is not None:
            for _ in range(8):
                await asyncio.sleep(0.05)
                song_id_str = self._preferred_song_id()
                if song_id_str.isdigit():
                    break

        track_key = self._track_key(title, artist)
        cache_key = self._lyrics_cache_key(track_key, duration_ms, song_id_str)

        if cache_key in self._lyrics_cache:
            return self._lyrics_cache[cache_key]

        if not self._should_fetch_lyrics(cache_key):
            return []

        if song_id_str.isdigit():
            logger.info("Fetch lyrics by desktop song id=%s for: %s - %s", song_id_str, title, artist)
            lyrics, fetch_ok = await asyncio.to_thread(self._fetch_lyrics_by_id, int(song_id_str))
            stored = self._store_lyrics_cache(cache_key, lyrics, fetch_ok=fetch_ok)
            if fetch_ok:
                self._last_lyric_song_id = song_id_str
            return stored

        if duration_ms <= 1000:
            logger.debug("Defer lyric search until duration is known: %s - %s", title, artist)
            return []

        logger.info(
            "Fetch lyrics from NetEase search for: %s - %s (duration_ms=%s)",
            title,
            artist,
            duration_ms,
        )
        lyrics, fetch_ok = await asyncio.to_thread(self._fetch_lyrics, title, artist, duration_ms)
        return self._store_lyrics_cache(cache_key, lyrics, fetch_ok=fetch_ok)

    def _fetch_lyrics_by_id(self, song_id: int) -> tuple[list[TimedLyric], bool]:
        try:
            query = urllib.parse.urlencode({"id": song_id, "lv": 1, "kv": 1, "tv": -1})
            data = self._http_json(f"{NETEASE_LYRIC_URL}?{query}")
            lyric_text = str((data.get("lrc") or {}).get("lyric") or "")
            lyrics = self._parse_lrc(lyric_text)
            if not lyrics:
                logger.warning("No timed lyric found for song id: %s", song_id)
            else:
                logger.info("Parsed %s lyric lines for song id: %s", len(lyrics), song_id)
            return lyrics, True
        except Exception:
            logger.exception("Failed to fetch lyrics for song id=%s", song_id)
            return [], False

    def _fetch_lyrics(self, title: str, artist: str, duration_ms: int = 0) -> tuple[list[TimedLyric], bool]:
        try:
            meta = self._resolve_song_meta(title, artist, duration_ms)
            if meta is None:
                logger.warning("No NetEase song match for: %s - %s", title, artist)
                return [], True

            song_id, _pic_url, _duration_ms = meta
            return self._fetch_lyrics_by_id(song_id)
        except Exception:
            logger.exception("Failed to fetch lyrics for: %s - %s", title, artist)
            return [], False

    def _fetch_cover_rgb565(self, title: str, artist: str) -> bytes | None:
        try:
            meta = self._resolve_song_meta(title, artist, 0)
            if meta is None:
                logger.warning("No NetEase song match for cover: %s - %s", title, artist)
                return None

            _song_id, pic_url, _duration_ms = meta
            if not pic_url:
                logger.warning("No album cover URL for: %s - %s", title, artist)
                return None

            request = urllib.request.Request(pic_url, headers=HTTP_HEADERS, method="GET")
            with urllib.request.urlopen(request, timeout=8) as response:
                image_bytes = response.read()

            return self._image_bytes_to_rgb565_le(image_bytes)
        except Exception:
            logger.exception("Failed to fetch cover for: %s - %s", title, artist)
            return None

    def _image_bytes_to_rgb565_le(self, image_bytes: bytes) -> bytes:
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        image = image.resize((COVER_SIZE, COVER_SIZE), Image.Resampling.LANCZOS)
        pixels = image.load()
        buffer = bytearray(COVER_SIZE * COVER_SIZE * 2)

        offset = 0
        for y in range(COVER_SIZE):
            for x in range(COVER_SIZE):
                red, green, blue = pixels[x, y]
                rgb565 = ((red & 0xF8) << 8) | ((green & 0xFC) << 3) | (blue >> 3)
                buffer[offset] = rgb565 & 0xFF
                buffer[offset + 1] = (rgb565 >> 8) & 0xFF
                offset += 2

        return bytes(buffer)

    def _song_duration_ms(self, key: str) -> int:
        cached = self._song_meta_cache.get(key)
        if cached is None:
            return 0
        try:
            return int(cached[2])
        except (TypeError, ValueError, IndexError):
            return 0

    def _resolve_song_meta(self, title: str, artist: str, duration_ms: int = 0) -> tuple[int, str, int] | None:
        song_id_str = self._preferred_song_id()
        if song_id_str.isdigit():
            return self._resolve_song_meta_by_id(int(song_id_str), title, artist)

        key = self._track_key(title, artist)
        with self._meta_resolve_lock:
            cached = self._song_meta_cache.get(key)
            if cached is not None and cached[0] > 0:
                if duration_ms <= 1000 or abs(cached[2] - duration_ms) <= 8000:
                    return cached
                logger.info(
                    "Drop cached song meta id=%s: cached_duration=%s request_duration=%s",
                    cached[0],
                    cached[2],
                    duration_ms,
                )
                del self._song_meta_cache[key]

            best_song = self._search_best_song(title, artist, duration_ms)
            if best_song is None:
                return None

            song_id = int(best_song["id"])
            pic_url = self._resolve_cover_url(best_song, song_id)
            if pic_url:
                pic_url = re.sub(r"\?param=.*", "", pic_url) + "?param=100y100"

            resolved_duration_ms = int(best_song.get("duration", 0) or 0)
            if resolved_duration_ms <= 1000:
                resolved_duration_ms = self._fetch_song_duration_ms(song_id)

            self._song_meta_cache[key] = (song_id, pic_url, resolved_duration_ms)
            if len(self._song_meta_cache) > 8:
                oldest_key = next(iter(self._song_meta_cache))
                if oldest_key != key:
                    del self._song_meta_cache[oldest_key]

            matched_name = to_simplified(str(best_song.get("name") or ""))
            logger.info(
                "NetEase song meta: title=%s artist=%s id=%s duration_ms=%s matched=%s",
                title,
                artist,
                song_id,
                resolved_duration_ms,
                matched_name,
            )
            return song_id, pic_url, resolved_duration_ms

    def _resolve_song_meta_by_id(self, song_id: int, title: str, artist: str) -> tuple[int, str, int] | None:
        id_key = f"id:{song_id}"
        cached = self._song_meta_cache.get(id_key)
        if cached is not None and cached[0] > 0:
            return cached

        track_key = self._track_key(title, artist)
        cached = self._song_meta_cache.get(track_key)
        if cached is not None and cached[0] == song_id:
            self._song_meta_cache[id_key] = cached
            return cached

        duration_ms = self._fetch_song_duration_ms(song_id)
        pic_url = self._fetch_song_detail_pic_url(song_id)
        if pic_url:
            pic_url = re.sub(r"\?param=.*", "", pic_url) + "?param=100y100"

        meta = (song_id, pic_url, duration_ms)
        self._song_meta_cache[id_key] = meta
        self._song_meta_cache[track_key] = meta
        logger.info(
            "NetEase song meta from desktop id: title=%s artist=%s id=%s duration_ms=%s",
            title,
            artist,
            song_id,
            duration_ms,
        )
        return meta

    def _fetch_song_duration_ms(self, song_id: int) -> int:
        try:
            data = self._http_json(NETEASE_SONG_DETAIL_URL.format(song_id=song_id))
            songs = data.get("songs") or []
            if not isinstance(songs, list) or not songs:
                return 0
            first_song = songs[0]
            if not isinstance(first_song, dict):
                return 0
            return int(first_song.get("duration", 0) or 0)
        except Exception:
            logger.exception("Failed to fetch song duration for id=%s", song_id)
            return 0

    def _resolve_cover_url(self, song: dict[str, object], song_id: int) -> str:
        album = song.get("album") if isinstance(song.get("album"), dict) else {}
        pic_url = str(album.get("picUrl") or "")
        if pic_url:
            return pic_url

        album_id = album.get("id")
        if album_id is not None:
            try:
                pic_url = self._fetch_album_pic_url(int(album_id))
                if pic_url:
                    logger.info("Resolved album cover via album id=%s", album_id)
                    return pic_url
            except (TypeError, ValueError):
                pass

        pic_url = self._fetch_song_detail_pic_url(song_id)
        if pic_url:
            logger.info("Resolved album cover via song id=%s", song_id)
        return pic_url

    def _fetch_album_pic_url(self, album_id: int) -> str:
        data = self._http_json(NETEASE_ALBUM_URL.format(album_id=album_id))
        album = data.get("album") if isinstance(data.get("album"), dict) else {}
        return str(album.get("picUrl") or "")

    def _fetch_song_detail_pic_url(self, song_id: int) -> str:
        data = self._http_json(NETEASE_SONG_DETAIL_URL.format(song_id=song_id))
        songs = data.get("songs") or []
        if not isinstance(songs, list) or not songs:
            return ""

        first_song = songs[0]
        if not isinstance(first_song, dict):
            return ""

        album = first_song.get("album") if isinstance(first_song.get("album"), dict) else {}
        return str(album.get("picUrl") or album.get("blurPicUrl") or "")

    def _normalize_search_title(self, title: str) -> str:
        text = to_simplified(title.strip())
        text = re.sub(r"\s*[\(（][^）\)]*[\)）]\s*", " ", text)
        return re.sub(r"\s+", " ", text).strip()

    def _title_core(self, title: str) -> str:
        text = to_simplified(title.strip())
        text = re.sub(r"\s*[\(（][^）\)]*[\)）]\s*", "", text)
        return re.sub(r"[\s\-—_·•.]+", "", text).lower()

    def _song_name_matches_title(self, requested_title: str, song_name: str) -> bool:
        core = self._title_core(requested_title)
        if len(core) < 2:
            return True
        candidate = self._title_core(song_name)
        if not candidate:
            return False
        return core in candidate or candidate in core

    def _title_version_markers(self, title: str) -> set[str]:
        lower = to_simplified(title).lower()
        markers: set[str] = set()
        for tag in ("live", "现场", "伴奏", "inst", "remix", "acoustic", "demo", "mv", "翻唱", "cover"):
            if tag in lower:
                markers.add(tag)
        return markers

    def _normalize_search_artist(self, artist: str) -> str:
        text = to_simplified(artist.strip())
        for sep in ("/", "、", ",", "，", "&", " feat.", " ft.", " featuring "):
            if sep in text.lower():
                text = text.split(sep, 1)[0]
        return text.strip()

    def _score_song_match(
        self,
        song: dict[str, object],
        title: str,
        artist: str,
        duration_ms: int = 0,
    ) -> int:
        song_name = to_simplified(str(song.get("name") or ""))
        artists = song.get("artists") or []
        artist_names = " ".join(
            to_simplified(str(item.get("name") or "")) for item in artists if isinstance(item, dict)
        )
        title_l = self._normalize_search_title(title).lower()
        artist_l = self._normalize_search_artist(artist).lower()
        song_name_l = self._normalize_search_title(song_name).lower()
        artist_names_l = artist_names.lower()

        score = 0
        if song_name_l == title_l:
            score += 8
        elif title_l and title_l in song_name_l:
            score += 4
        elif title_l and song_name_l in title_l:
            score += 2
        if artist_l and artist_l in artist_names_l:
            score += 5
        elif artist_l:
            for part in re.split(r"[/,&、，]", artist_l):
                part = part.strip()
                if part and part in artist_names_l:
                    score += 3
                    break

        wanted_markers = self._title_version_markers(title)
        candidate_markers = self._title_version_markers(song_name)
        if wanted_markers:
            if wanted_markers & candidate_markers:
                score += 3
            else:
                score -= 4
        elif candidate_markers:
            score -= 2

        if duration_ms > 1000:
            try:
                candidate_duration = int(song.get("duration", 0) or 0)
            except (TypeError, ValueError):
                candidate_duration = 0
            if candidate_duration > 1000:
                delta = abs(candidate_duration - duration_ms)
                if delta <= 3000:
                    score += 4
                elif delta <= 8000:
                    score += 2
                elif delta > 20_000:
                    score -= 3

        return score

    def _search_best_song(self, title: str, artist: str, duration_ms: int = 0) -> dict[str, object] | None:
        queries: list[str] = []
        full_title = to_simplified(title.strip())
        normalized_title = self._normalize_search_title(title)
        normalized_artist = self._normalize_search_artist(artist)
        if full_title and normalized_artist:
            queries.append(f"{full_title} {normalized_artist}")
        if normalized_title and normalized_artist and f"{normalized_title} {normalized_artist}" not in queries:
            queries.append(f"{normalized_title} {normalized_artist}")
        if full_title and full_title not in queries:
            queries.append(full_title)
        if normalized_title and normalized_title not in queries:
            queries.append(normalized_title)

        best_song: dict[str, object] | None = None
        best_score = -1
        duration_best_song: dict[str, object] | None = None
        duration_best_score = -1
        total_candidates = 0

        for query in queries:
            payload = urllib.parse.urlencode(
                {
                    "s": query,
                    "type": 1,
                    "offset": 0,
                    "total": "true",
                    "limit": 10,
                }
            ).encode("utf-8")
            data = self._http_json(NETEASE_SEARCH_URL, payload)
            songs = (data.get("result") or {}).get("songs") or []
            if not isinstance(songs, list):
                continue

            total_candidates += len(songs)
            for song in songs:
                if not isinstance(song, dict):
                    continue
                song_name = str(song.get("name") or "")
                if not self._song_name_matches_title(title, song_name):
                    continue
                score = self._score_song_match(song, title, artist, duration_ms)
                if score > best_score:
                    best_score = score
                    best_song = song
                if duration_ms > 1000:
                    try:
                        candidate_duration = int(song.get("duration", 0) or 0)
                    except (TypeError, ValueError):
                        candidate_duration = 0
                    if candidate_duration > 1000 and abs(candidate_duration - duration_ms) <= 8000:
                        if score > duration_best_score:
                            duration_best_score = score
                            duration_best_song = song

        if duration_ms > 1000 and duration_best_song is not None and duration_best_score >= MIN_SONG_MATCH_SCORE:
            best_song = duration_best_song
            best_score = duration_best_score

        if best_song is None or best_score < MIN_SONG_MATCH_SCORE:
            logger.warning(
                "NetEase search rejected for %s - %s: candidates=%s best_score=%s (need >= %s)",
                title,
                artist,
                total_candidates,
                best_score,
                MIN_SONG_MATCH_SCORE,
            )
            return None

        matched_name = to_simplified(str(best_song.get("name") or ""))
        if not self._song_name_matches_title(title, matched_name):
            logger.warning(
                "NetEase search title mismatch for %s - %s: matched=%s id=%s",
                title,
                artist,
                matched_name,
                best_song.get("id"),
            )
            return None
        logger.info(
            "NetEase search picked for %s - %s: candidates=%s best_score=%s matched=%s id=%s",
            title,
            artist,
            total_candidates,
            best_score,
            matched_name,
            best_song.get("id"),
        )
        return best_song

    def _search_song_id(self, title: str, artist: str, duration_ms: int = 0) -> int | None:
        meta = self._resolve_song_meta(title, artist, duration_ms)
        if meta is None:
            return None
        return meta[0]

    def _http_json(self, url: str, payload: bytes | None = None) -> dict[str, object]:
        request = urllib.request.Request(url, data=payload, headers=HTTP_HEADERS, method="POST" if payload else "GET")
        with urllib.request.urlopen(request, timeout=8) as response:
            body = response.read().decode("utf-8", errors="replace")
        parsed = json.loads(body)
        return parsed if isinstance(parsed, dict) else {}

    def _parse_lrc(self, text: str) -> list[TimedLyric]:
        lines: list[TimedLyric] = []
        for raw_line in text.splitlines():
            matches = list(LRC_TIME_RE.finditer(raw_line))
            if not matches:
                continue

            lyric_text = to_simplified(LRC_TIME_RE.sub("", raw_line).strip())
            if not lyric_text:
                continue
            if any(lyric_text.startswith(prefix) for prefix in LYRIC_META_PREFIXES):
                continue

            for match in matches:
                lines.append(TimedLyric(start_ms=self._lrc_time_to_ms(match), text=lyric_text))

        return sorted(lines, key=lambda item: item.start_ms)

    def _lrc_time_to_ms(self, match: re.Match[str]) -> int:
        minutes = int(match.group(1))
        seconds = int(match.group(2))
        fraction = match.group(3) or "0"
        fraction_ms = int(fraction.ljust(3, "0")[:3])
        return minutes * 60_000 + seconds * 1000 + fraction_ms

    def _find_lyric_index(self, lyrics: list[TimedLyric], position_ms: int) -> int:
        if not lyrics:
            return -1

        index = 0
        for i, lyric in enumerate(lyrics):
            if lyric.start_ms <= position_ms:
                index = i
            else:
                break
        return index

    def _track_key(self, title: str, artist: str) -> str:
        return f"{title.strip().lower()}::{artist.strip().lower()}"
