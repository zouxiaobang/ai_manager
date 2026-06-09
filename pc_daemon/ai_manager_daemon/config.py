from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any

import yaml


@dataclass(frozen=True)
class DesktopAppConfig:
    executable: str
    process_name: str
    auto_launch_on_startup: bool


@dataclass(frozen=True)
class NeteaseConfig:
    control_mode: str
    playback_poll_interval_seconds: float
    lyric_poll_interval_seconds: float
    lyric_offset_ms: int
    progress_source: str
    cdp_port: int
    cdp_auto_launch_debug: bool


@dataclass(frozen=True)
class SecurityConfig:
    allowed_device_ids: list[str]


@dataclass(frozen=True)
class AppConfig:
    host: str
    port: int
    desktop_app: DesktopAppConfig
    netease: NeteaseConfig
    security: SecurityConfig


def _read_yaml(path: Path) -> dict[str, Any]:
    if not path.exists():
        path = path.with_name("config.example.yaml")

    with path.open("r", encoding="utf-8") as file:
        data = yaml.safe_load(file) or {}

    if not isinstance(data, dict):
        raise ValueError(f"Invalid config file: {path}")

    return data


def _normalize_windows_path(value: str) -> str:
    text = str(value or "").strip()
    # YAML/手误常写成中文全角冒号，导致 Path.exists() 永远为 false
    text = text.replace("：", ":")
    return text


def load_config(path: str | Path | None = None) -> AppConfig:
    base_path = Path(__file__).resolve().parents[1]
    config_path = Path(path) if path else base_path / "config.yaml"
    data = _read_yaml(config_path)

    desktop_app = data.get("desktop_app", {})
    netease = data.get("netease", {})
    security = data.get("security", {})
    return AppConfig(
        host=str(data.get("host", "0.0.0.0")),
        port=int(data.get("port", 8765)),
        desktop_app=DesktopAppConfig(
            executable=_normalize_windows_path(str(desktop_app.get("executable", ""))),
            process_name=str(desktop_app.get("process_name", "cloudmusic.exe")),
            auto_launch_on_startup=bool(desktop_app.get("auto_launch_on_startup", True)),
        ),
        netease=NeteaseConfig(
            control_mode=str(netease.get("control_mode", "hotkey")),
            playback_poll_interval_seconds=float(
                netease.get("playback_poll_interval_seconds", netease.get("lyric_poll_interval_seconds", 1.0))
            ),
            lyric_poll_interval_seconds=float(netease.get("lyric_poll_interval_seconds", 0.5)),
            lyric_offset_ms=int(netease.get("lyric_offset_ms", 0)),
            progress_source=str(netease.get("progress_source", "auto")).strip().lower(),
            cdp_port=int(netease.get("cdp_port", 9222)),
            cdp_auto_launch_debug=bool(netease.get("cdp_auto_launch_debug", True)),
        ),
        security=SecurityConfig(
            allowed_device_ids=list(security.get("allowed_device_ids", [])),
        ),
    )
