from __future__ import annotations

import atexit
import os
from pathlib import Path

RUNTIME_DIR = Path(__file__).resolve().parents[1] / ".runtime"
PID_PATH = RUNTIME_DIR / "daemon.pid"


def write_pid_file() -> None:
    RUNTIME_DIR.mkdir(parents=True, exist_ok=True)
    PID_PATH.write_text(str(os.getpid()), encoding="utf-8")


def remove_pid_file() -> None:
    if PID_PATH.exists():
        PID_PATH.unlink(missing_ok=True)


def register_pid_cleanup() -> None:
    write_pid_file()
    atexit.register(remove_pid_file)
