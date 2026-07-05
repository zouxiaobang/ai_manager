"""Apple Music 后端：SMTC 读状态/进度并控制，lrclib.net 取 LRC 歌词。

类名保留 NeteaseBridge 以兼容 server.py（命名向后兼容）。
Apple Music UWP 版 SMTC 完整实现：timeline.position 真实含手拖，控制全通。
"""
from __future__ import annotations

import asyncio
import json
import logging
import threading
import time
import urllib.parse
import urllib.request
from dataclasses import dataclass

logger = logging.getLogger("cloudmusic")

LRCLIB_SEARCH_URL = "https://lrclib.net/api/search"
POLL_INTERVAL_SEC = 0.5
APPLE_KEYWORD = "apple"  # 匹配 SourceAppUserModelId（AppleInc.AppleMusic_*）

# winsdk 延迟导入
_SessionMgr = None
_PB = None


def _import_smtc():
    global _SessionMgr, _PB
    if _SessionMgr is None:
        from winsdk.windows.media.control import (
            GlobalSystemMediaTransportControlsSessionManager as Mgr,
            GlobalSystemMediaTransportControlsSessionPlaybackStatus as PB,
        )
        _SessionMgr = Mgr
        _PB = PB
    return _SessionMgr, _PB


def _td_ms(td) -> int:
    """timedelta -> 毫秒（兼容异常输入）"""
    if td is None:
        return 0
    try:
        return int(td.total_seconds() * 1000)
    except Exception:
        try:
            return int(td)
        except Exception:
            return 0


@dataclass
class Snapshot:
    playing: bool = False
    title: str = ""
    artist: str = ""
    album: str = ""
    position_ms: int = 0
    duration_ms: int = 0
    lyrics: str = ""
    updated_at_ms: int = 0
    available: bool = False  # Apple Music 会话是否存在


class NeteaseBridge:
    """Apple Music SMTC 桥接（类名兼容 server.py）。"""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._snap = Snapshot()
        self._lyric_key = ""
        self._lyric_retry_at = 0.0
        self._thread = threading.Thread