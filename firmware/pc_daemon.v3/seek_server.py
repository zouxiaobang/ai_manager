"""BetterNCM 插件通信 HTTP 服务器。

接收 BetterNCM 插件上报的进度，并向外提供 seek 命令轮询接口。
"""

from __future__ import annotations

import json
import logging
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer
from typing import Optional

logger = logging.getLogger("seek_server")

HOST = "127.0.0.1"
PORT = 9877


# ── 线程安全状态容器 ─────────────────────────────────────

class SeekCommand:
    """待处理的 seek 命令。"""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._pending_ms: Optional[int] = None

    def set(self, position_ms: int) -> None:
        with self._lock:
            self._pending_ms = position_ms

    def poll(self) -> Optional[dict]:
        with self._lock:
            if self._pending_ms is None:
                return None
            cmd = {"position_ms": self._pending_ms}
            self._pending_ms = None
            return cmd

    def clear(self) -> None:
        with self._lock:
            self._pending_ms = None


class PluginProgress:
    """BetterNCM 插件上报的最新播放进度（比 SMTC 更准确）。"""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._position_ms: int = 0
        self._duration_ms: int = 0
        self._playing: bool = False
        self._has_data: bool = False  # False = 插件尚未上报过

    def update(self, position_ms: int, duration_ms: int, playing: bool) -> None:
        with self._lock:
            self._position_ms = position_ms
            self._duration_ms = duration_ms
            self._playing = playing
            self._has_data = True

    def get(self) -> dict:
        with self._lock:
            return {
                "position_ms": self._position_ms,
                "duration_ms": self._duration_ms,
                "playing": self._playing,
                "available": self._has_data,
            }


# 全局实例
command = SeekCommand()
plugin_progress = PluginProgress()


# ── HTTP Handler ─────────────────────────────────────────

class Handler(BaseHTTPRequestHandler):
    server_version = "SeekBridge/1.0"

    def log_message(self, fmt, *args):
        msg = fmt % args
        if "/seek-poll" not in msg and "/progress" not in msg:
            logger.debug("HTTP %s", msg)

    def _send_json(self, obj, status=200):
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _read_body(self) -> dict:
        length = int(self.headers.get("Content-Length", 0))
        if length == 0:
            return {}
        raw = self.rfile.read(length)
        return json.loads(raw.decode("utf-8"))

    def do_GET(self):
        if self.path == "/internal/seek-poll":
            cmd_data = command.poll()
            self._send_json(cmd_data or {})
        else:
            self._send_json({"error": "not found"}, 404)

    def do_POST(self):
        try:
            body = self._read_body()
        except Exception:
            self._send_json({"error": "bad request"}, 400)
            return

        path = self.path

        if path == "/internal/progress":
            pos = body.get("position_ms", 0)
            dur = body.get("duration_ms", 0)
            playing = body.get("playing", False)
            plugin_progress.update(pos, dur, playing)
            if dur > 0:
                logger.debug(
                    "Plugin progress: %d/%d ms playing=%s",
                    pos, dur, playing,
                )
            self._send_json({"ok": True})

        elif path == "/internal/seek-done":
            pos = body.get("position_ms", 0)
            logger.info("Plugin seek executed at %d ms", pos)
            self._send_json({"ok": True})

        else:
            self._send_json({"error": "not found"}, 404)

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()


# ── SeekServer 封装 ──────────────────────────────────────

class SeekServer:
    """HTTP 服务器线程封装。"""

    def __init__(self) -> None:
        self._server: Optional[HTTPServer] = None
        self._thread: Optional[threading.Thread] = None

    def start(self) -> None:
        if self._thread and self._thread.is_alive():
            return
        self._server = HTTPServer((HOST, PORT), Handler)

        def serve():
            logger.info("Seek server listening on %s:%d", HOST, PORT)
            try:
                self._server.serve_forever()
            except Exception:
                pass

        self._thread = threading.Thread(target=serve, daemon=True, name="seek-server")
        self._thread.start()

    def stop(self) -> None:
        if self._server:
            self._server.shutdown()
            logger.info("Seek server stopped")

    def set_pending_seek(self, position_ms: int) -> None:
        """设置待处理的 seek 命令（供 GUI 线程调用）。"""
        command.set(position_ms)
        logger.info("Pending seek set: %d ms", position_ms)

    def get_plugin_state(self) -> dict:
        """获取 BetterNCM 插件上报的最新播放状态（用于 SMTC 数据的补充 / 替换）。"""
        return plugin_progress.get()
