from __future__ import annotations

import logging
import subprocess
import time
from ctypes import WINFUNCTYPE, windll
from ctypes.wintypes import BOOL, DWORD, HWND, LPARAM
from dataclasses import dataclass
from os import environ
from pathlib import Path

import psutil

from ai_manager_daemon.netease_cdp_progress import process_has_cdp_flag

logger = logging.getLogger("uvicorn.error")

SW_RESTORE = 9


@dataclass
class DesktopAppController:
    executable: str
    process_name: str

    def running_process_ids(self) -> list[int]:
        expected_name = self.process_name.lower()
        if not expected_name:
            return []

        process_ids: list[int] = []

        for process in psutil.process_iter(["name", "pid"]):
            name = process.info.get("name") or ""
            if name.lower() == expected_name:
                process_ids.append(int(process.info["pid"]))

        return process_ids

    def is_running(self) -> bool:
        return bool(self.running_process_ids())

    def launch_if_needed(self, cdp_port: int = 0, *, wait_ready: bool = True) -> None:
        if self.is_running():
            logger.info(
                "NetEase process already running (pids=%s); skip launch",
                ",".join(str(pid) for pid in self.running_process_ids()),
            )
            if cdp_port > 0 and not process_has_cdp_flag(self.process_name, cdp_port):
                logger.warning(
                    "NetEase is already running without --remote-debugging-port=%s. "
                    "Quit and restart the client once so drag-seek lyrics stay in sync when minimized.",
                    cdp_port,
                )
            # 进程已在跑就不要再等 2.5s 或重复拉起（最小化到托盘时 focus 常失败）
            self.focus_running_window()
            return

        executable_path = self._resolve_executable()
        launch_args = [str(executable_path)]
        if cdp_port > 0:
            launch_args.append(f"--remote-debugging-port={cdp_port}")
        logger.info("Launching desktop app: %s", " ".join(launch_args))
        subprocess.Popen(
            launch_args,
            cwd=str(executable_path.parent),
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        if wait_ready:
            time.sleep(2.5)
        self.focus_running_window()

    def focus_running_window(self) -> bool:
        process_ids = set(self.running_process_ids())
        if not process_ids:
            return False

        matched_windows: list[int] = []

        @WINFUNCTYPE(BOOL, HWND, LPARAM)
        def enum_window(hwnd: HWND, lparam: LPARAM) -> bool:
            if not windll.user32.IsWindowVisible(hwnd):
                return True

            process_id = DWORD()
            windll.user32.GetWindowThreadProcessId(hwnd, process_id)
            if int(process_id.value) in process_ids:
                matched_windows.append(int(hwnd))

            return True

        windll.user32.EnumWindows(enum_window, 0)

        if not matched_windows:
            return False

        hwnd = HWND(matched_windows[0])
        windll.user32.ShowWindow(hwnd, SW_RESTORE)
        windll.user32.SetForegroundWindow(hwnd)
        return True

    def _resolve_executable(self) -> Path:
        candidates = self._candidate_paths()
        for candidate in candidates:
            if candidate.exists():
                return candidate

        searched = ", ".join(str(path) for path in candidates)
        raise FileNotFoundError(f"Desktop app executable not found. Searched: {searched}")

    def _candidate_paths(self) -> list[Path]:
        candidates: list[Path] = []

        if self.executable:
            candidates.append(Path(self.executable))

        program_files = environ.get("ProgramFiles")
        program_files_x86 = environ.get("ProgramFiles(x86)")
        local_app_data = environ.get("LOCALAPPDATA")

        for base in (program_files, program_files_x86):
            if base:
                candidates.append(Path(base) / "NetEase" / "CloudMusic" / "cloudmusic.exe")
                candidates.append(Path(base) / "Netease" / "CloudMusic" / "cloudmusic.exe")

        if local_app_data:
            candidates.append(Path(local_app_data) / "Netease" / "CloudMusic" / "cloudmusic.exe")

        return candidates
