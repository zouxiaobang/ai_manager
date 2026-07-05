from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Settings:
    host: str = "0.0.0.0"
    port: int = 8765
    poll_interval_ms: int = 500
    lyric_push_interval_ms: int = 1000
    netease_process_names: tuple[str, ...] = ("cloudmusic.exe", "cloudmusic")
    netease_app_ids: tuple[str, ...] = (
        "cloudmusic.exe",
        "CloudMusic.NetEase",
        "Netease.CloudMusic",
        "网易云音乐",
    )
    netease_search_url: str = "https://music.163.com/api/search/get/web"
    netease_lyric_url: str = "https://music.163.com/api/song/lyric"
    netease_headers: dict[str, str] = None  # type: ignore[assignment]

    def __post_init__(self) -> None:
        object.__setattr__(
            self,
            "netease_headers",
            {
                "Referer": "https://music.163.com/",
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            },
        )


def load_settings() -> Settings:
    return Settings(
        host=os.getenv("MEDIA_DAEMON_HOST", "0.0.0.0"),
        port=int(os.getenv("MEDIA_DAEMON_PORT", "8765")),
        poll_interval_ms=int(os.getenv("MEDIA_POLL_INTERVAL_MS", "500")),
        lyric_push_interval_ms=int(os.getenv("MEDIA_LYRIC_PUSH_INTERVAL_MS", "1000")),
    )


def default_netease_paths() -> list[Path]:
    local = os.environ.get("LOCALAPPDATA", "")
    program_files = os.environ.get("ProgramFiles", r"C:\Program Files")
    program_files_x86 = os.environ.get("ProgramFiles(x86)", r"C:\Program Files (x86)")
    candidates = [
        Path(local) / "Netease" / "CloudMusic" / "cloudmusic.exe",
        Path(program_files) / "Netease" / "CloudMusic" / "cloudmusic.exe",
        Path(program_files_x86) / "Netease" / "CloudMusic" / "cloudmusic.exe",
    ]
    return [p for p in candidates if p.is_file()]
