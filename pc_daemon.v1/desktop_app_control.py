from __future__ import annotations

import logging
import os
import subprocess
import time
from pathlib import Path

import psutil

from config import Settings, default_netease_paths

logger = logging.getLogger(__name__)


def is_netease_running(settings: Settings) -> bool:
    names = {name.lower() for name in settings.netease_process_names}
    for proc in psutil.process_iter(["name"]):
        try:
            name = (proc.info.get("name") or "").lower()
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            continue
        if name in names:
            return True
    return False


def find_netease_executable() -> Path | None:
    for path in default_netease_paths():
        if path.is_file():
            return path
    return None


def launch_netease() -> bool:
    exe = find_netease_executable()
    if exe is None:
        logger.error("NetEase Cloud Music executable not found")
        return False
    try:
        subprocess.Popen(
            [str(exe)],
            cwd=str(exe.parent),
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            creationflags=subprocess.DETACHED_PROCESS | subprocess.CREATE_NEW_PROCESS_GROUP,
        )
        logger.info("Launched NetEase Cloud Music: %s", exe)
        return True
    except OSError as exc:
        logger.error("Failed to launch NetEase: %s", exc)
        return False


def wait_until_running(settings: Settings, timeout_sec: float = 20.0) -> bool:
    deadline = time.time() + timeout_sec
    while time.time() < deadline:
        if is_netease_running(settings):
            return True
        time.sleep(0.5)
    return is_netease_running(settings)
