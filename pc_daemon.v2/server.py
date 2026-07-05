"""网易云歌词同步桥 v2 —— 对外提供 4 个 GET 接口。

    GET /api?cmd=song    -> 当前播放状态 + 完整歌词
    GET /api?cmd=sync    -> 当前播放进度
    GET /api?cmd=toggle  -> 播放/暂停切换
    GET /api?cmd=prev    -> 上一首
    GET /api?cmd=next    -> 下一首

另保留 v1 兼容路由（/api/media/status|lyrics|control|start），供旧固件免烧录继续使用。
"""
from __future__ import annotations

import json
import logging
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

from cloudmusic import NeteaseBridge

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s [%(name)s] %(message)s")
logger = logging.getLogger("server")

HOST = os.getenv("MEDIA_DAEMON_HOST", "0.0.0.0")
PORT = int(os.getenv("MEDIA_DAEMON_PORT", "8765"))

bridge = NeteaseBridge()


class Handler(BaseHTTPRequestHandler):
    server_version = "NetEaseBridge/2.0"

    def log_message(self, fmt, *args):
        logger.info("%s %s", self.address_string(), fmt % args)

    def _send(self, obj, status=200):
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    # ---------- 新契约：cmd 调度 ----------
    def _dispatch(self, cmd: str):
        if cmd == "song":
            snap = bridge.snapshot()
            return {
                "cmd": "song",
                "playing": snap.playing,
                "title": snap.title,
                "artist": snap.artist,
                "lyrics": snap.lyrics,
            }, 200

        if cmd == "sync":
            return {
                "cmd": "sync",
                "playing": bridge.playing(),
                "position": bridge.estimated_position_ms(),
            }, 200

        if cmd == "toggle":
            old_playing = bridge.playing()
            ok = bridge.send("toggle")
            if ok:
                bridge.wait_for_state_change(old_playing, timeout=2.0)
            snap = bridge.snapshot()
            return {
                "cmd": "toggle",
                "playing": snap.playing,
                "position": bridge.estimated_position_ms(),
            }, 200

        if cmd in ("prev", "next"):
            old_title = bridge.snapshot().title
            ok = bridge.send("previous" if cmd == "prev" else "next")
            snap = bridge.wait_for_song_change(old_title, timeout=3.0) if ok else bridge.snapshot()
            return {
                "cmd": cmd,
                "playing": snap.playing,
                "title": snap.title,
                "artist": snap.artist,
                "lyrics": snap.lyrics,
            }, 200

        return {"cmd": "error", "error": "unknown cmd",
                "supported": ["song", "sync", "toggle", "prev", "next"]}, 400

    def do_OPTIONS(self):
        """CORS 预检：BetterNCM 插件在 orpheus:// 域 fetch 到 127.0.0.1 需跨域。"""
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path
        query = parse_qs(parsed.query)

        # --- v1 兼容：旧固件路径 ---
        if path == "/api/media/status":
            self._send({"cmd": "sync", "playing": bridge.playing(),
                        "position": bridge.estimated_position_ms()})
            return
        if path == "/api/media/lyrics":
            snap = bridge.snapshot()
            self._send({"cmd": "song", "playing": snap.playing, "title": snap.title,
                        "artist": snap.artist, "lyrics": snap.lyrics})
            return

        # --- 新契约 ---
        if path not in ("/api", "/"):
            self._send({"cmd": "error", "error": "not found"}, 404)
            return
        cmd = (query.get("cmd") or [""])[0].lower()
        obj, status = self._dispatch(cmd)
        self._send(obj, status)

    def do_POST(self):
        path = urlparse(self.path).path

        if path == "/api/media/control":
            length = int(self.headers.get("Content-Length", "0") or 0)
            body = self.rfile.read(length).decode("utf-8", "ignore") if length > 0 else ""
            command = ""
            try:
                command = (json.loads(body).get("command") or "").lower()
            except Exception:
                pass
            # v1 用 "previous"，v2 用 "prev"
            mapping = {"previous": "prev", "prev": "prev", "next": "next",
                       "toggle": "toggle", "play": "play", "pause": "pause"}
            mapped = mapping.get(command, command)
            ok = bridge.send(mapped) if mapped else False
            self._send({"code": 0, "data": {"ok": ok, "command": command}})
            return

        if path == "/internal/progress":
            # BetterNCM 插件上报真实进度（含手拖）
            length = int(self.headers.get("Content-Length", "0") or 0)
            body = self.rfile.read(length).decode("utf-8", "ignore") if length > 0 else ""
            try:
                data = json.loads(body) if body else {}
            except Exception:
                data = {}
            if data:
                bridge.set_plugin_progress(data)
            self._send({"ok": True})
            return

        self._send({"cmd": "error", "error": "not found"}, 404)


def main():
    logger.info("NetEase lyrics bridge v2 listening on %s:%s", HOST, PORT)
    httpd = ThreadingHTTPServer((HOST, PORT), Handler)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        bridge.close()
        httpd.server_close()


if __name__ == "__main__":
    main()
