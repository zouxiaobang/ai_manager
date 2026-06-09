from __future__ import annotations

import json
import logging
import threading
import time
import urllib.error
import urllib.request
from dataclasses import dataclass

logger = logging.getLogger("uvicorn.error")

# Adapted from NeteaseHookSDK CDPController (audioplayer.onPlayProgress IPC hook).
_REGISTER_JS = """
(function() {
    if (!window.channel || !window.channel.registerCall) {
        return { success: false, error: "NO_CHANNEL" };
    }
    window.channel.registerCall("audioplayer.onPlayProgress", function(songId, currentTime) {
        window.__NCM_PROGRESS__ = window.__NCM_PROGRESS__ || {};
        window.__NCM_PROGRESS__.songId = String(songId || "");
        window.__NCM_PROGRESS__.currentTime = Number(currentTime) || 0;
        window.__NCM_PROGRESS__.timestamp = Date.now();
    });
    return { success: true };
})()
"""

_POLL_JS = """
(function() {
    if (!window.__NCM_PROGRESS__ || (Date.now() - (window.__NCM_PROGRESS__.timestamp || 0) > 5000)) {
        if (window.channel && window.channel.registerCall) {
            window.channel.registerCall("audioplayer.onPlayProgress", function(songId, currentTime) {
                window.__NCM_PROGRESS__ = window.__NCM_PROGRESS__ || {};
                window.__NCM_PROGRESS__.songId = String(songId || "");
                window.__NCM_PROGRESS__.currentTime = Number(currentTime) || 0;
                window.__NCM_PROGRESS__.timestamp = Date.now();
            });
        }
    }
    var p = window.__NCM_PROGRESS__ || {};
    var currentTime = p.currentTime || 0;
    var songId = p.songId || "";
    var duration = 0;
    try {
        var slider = document.querySelector('[class*="slider"][class*="StyledSliderContainer"]');
        if (!slider) slider = document.querySelector('[class*="slider"]');
        if (slider) {
            var input = slider.querySelector('input[type="range"]');
            if (!input) input = slider.querySelector("input");
            if (input) {
                if (input.max) {
                    duration = parseFloat(input.max);
                } else {
                    for (var key in input) {
                        if (key.startsWith("__reactInternalInstance") || key.startsWith("__reactFiber")) {
                            var fiber = input[key];
                            if (fiber) {
                                var props = fiber.pendingProps || fiber.memoizedProps;
                                if (props && typeof props.max === "number") {
                                    duration = props.max;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    } catch (e) {}
    return { songId: songId, currentTime: currentTime, duration: duration };
})()
"""

_CLICK_COORDS_JS_TEMPLATE = """
(function(targetMs) {
    var targetSec = targetMs / 1000.0;
    function findRangeInput() {
        var slider = document.querySelector('[class*="StyledSliderContainer"]');
        if (!slider) slider = document.querySelector('[class*="slider"]');
        if (!slider) return null;
        return slider.querySelector('input[type="range"]') || slider.querySelector("input");
    }
    var input = findRangeInput();
    if (!input) return { success: false, error: "no_input" };
    var max = parseFloat(input.max) || 0;
    if (max <= 0) return { success: false, error: "no_max" };
    var value = max >= 100000 ? targetMs : (max < 36000 ? targetSec : targetMs);
    value = Math.max(0, Math.min(max, value));
    var rect = input.getBoundingClientRect();
    var ratio = max > 0 ? value / max : 0;
    return {
        success: true,
        x: rect.left + rect.width * ratio,
        y: rect.top + rect.height / 2,
        value: value,
        max: max
    };
})(%d)
"""

_SEEK_JS_TEMPLATE = """
(function(targetMs) {
    var targetSec = targetMs / 1000.0;
    function tryChannelInvoke() {
        if (!window.channel || typeof window.channel.invoke !== "function") {
            return null;
        }
        var names = [
            "audioplayer.seek",
            "audioplayer.setCurrentTime",
            "audioplayer.setPosition",
            "player.seek",
            "player.setCurrentTime"
        ];
        for (var i = 0; i < names.length; i++) {
            try {
                var ret = window.channel.invoke(names[i], targetSec);
                if (ret !== false) {
                    return { success: true, method: names[i], arg: targetSec };
                }
            } catch (e) {}
            try {
                var retMs = window.channel.invoke(names[i], targetMs);
                if (retMs !== false) {
                    return { success: true, method: names[i], arg: targetMs };
                }
            } catch (e) {}
        }
        return null;
    }
    function findRangeInput() {
        var slider = document.querySelector('[class*="StyledSliderContainer"]');
        if (!slider) slider = document.querySelector('[class*="slider"]');
        if (!slider) return null;
        return slider.querySelector('input[type="range"]') || slider.querySelector("input");
    }
    function setNativeValue(input, value) {
        var desc = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value");
        if (desc && desc.set) {
            desc.set.call(input, String(value));
        } else {
            input.value = String(value);
        }
    }
    function fireInput(input) {
        input.dispatchEvent(new InputEvent("input", { bubbles: true, cancelable: true }));
        input.dispatchEvent(new Event("change", { bubbles: true, cancelable: true }));
    }
    function seekRange(input) {
        var max = parseFloat(input.max) || 0;
        if (max <= 0) return null;
        var value = max >= 100000 ? targetMs : (max < 36000 ? targetSec : targetMs);
        value = Math.max(0, Math.min(max, value));
        setNativeValue(input, value);
        fireInput(input);
        var rect = input.getBoundingClientRect();
        var ratio = max > 0 ? value / max : 0;
        var x = rect.left + rect.width * ratio;
        var y = rect.top + rect.height / 2;
        ["mousedown", "mouseup", "click"].forEach(function(type) {
            input.dispatchEvent(new MouseEvent(type, {
                bubbles: true,
                cancelable: true,
                clientX: x,
                clientY: y,
                view: window
            }));
        });
        return { success: true, method: "range", value: value, max: max };
    }
    var channelResult = tryChannelInvoke();
    if (channelResult) return channelResult;
    var audios = document.querySelectorAll("audio");
    for (var i = 0; i < audios.length; i++) {
        try {
            audios[i].currentTime = targetSec;
            return { success: true, method: "audio", value: targetSec };
        } catch (e) {}
    }
    var input = findRangeInput();
    if (input) {
        var rangeResult = seekRange(input);
        if (rangeResult) return rangeResult;
    }
    return { success: false, error: "no_seek_target" };
})(%d)
"""


@dataclass(frozen=True)
class CdpProgressSnapshot:
    position_ms: int
    duration_ms: int
    song_id: str


def _normalize_progress_times(current: float, duration: float) -> tuple[int, int]:
    """NetEase CDP reports progress in seconds; slider max is usually seconds too."""
    current_val = float(current or 0)
    duration_val = float(duration or 0)
    if current_val <= 0 and duration_val <= 0:
        return 0, 0

    samples = [value for value in (current_val, duration_val) if value > 0]
    peak = max(samples)
    # >= 100000 → milliseconds (e.g. 125000 ms for 2:05)
    if peak >= 100_000:
        return int(current_val), int(duration_val)
    # < 36000 → seconds (4 min song ≈ 240 s)
    if peak < 36_000:
        return int(current_val * 1000), int(duration_val * 1000)
    return int(current_val), int(duration_val)


class NeteaseCdpProgressReader:
    """Read NetEase playback progress via Chrome DevTools Protocol (works when minimized)."""

    def __init__(self, port: int = 9222, poll_interval_seconds: float = 0.2) -> None:
        self.port = max(int(port), 1)
        self.poll_interval_seconds = max(float(poll_interval_seconds), 0.1)
        self._lock = threading.Lock()
        self._position_ms = -1
        self._duration_ms = 0
        self._song_id = ""
        self._updated_mono = 0.0
        self._connected = False
        self._stop = threading.Event()
        self._thread: threading.Thread | None = None
        self._logged_ready = False
        self._logged_unavailable = False
        self._logged_song_id = ""
        self._seek_hold_target_ms = -1
        self._seek_hold_until_mono = 0.0
        self._seek_poll_hold_seconds = 8.0

    def start(self) -> None:
        if self._thread and self._thread.is_alive():
            return
        self._stop.clear()
        self._thread = threading.Thread(target=self._run_loop, name="netease-cdp-progress", daemon=True)
        self._thread.start()

    def stop(self) -> None:
        self._stop.set()

    @property
    def connected(self) -> bool:
        with self._lock:
            return self._connected

    def seek_to_ms(self, position_ms: int) -> bool:
        position_ms = max(int(position_ms), 0)
        self._arm_seek_poll_hold(position_ms)
        try:
            result = self._evaluate_seek_once(position_ms)
        except Exception:
            logger.exception("CDP seek failed")
            return False
        if result is None or not result.get("success"):
            logger.warning("CDP seek rejected: %s", result)
            return False
        with self._lock:
            duration_ms = self._duration_ms
            song_id = self._song_id
        self._set_snapshot(position_ms, duration_ms, song_id, True)
        logger.info(
            "CDP seek to %sms (method=%s value=%s click=%s)",
            position_ms,
            result.get("method"),
            result.get("value"),
            result.get("click"),
        )
        return True

    def _arm_seek_poll_hold(self, position_ms: int) -> None:
        with self._lock:
            self._seek_hold_target_ms = int(position_ms)
            self._seek_hold_until_mono = time.monotonic() + self._seek_poll_hold_seconds

    def _accept_poll_position(self, position_ms: int) -> bool:
        with self._lock:
            if time.monotonic() >= self._seek_hold_until_mono:
                return True
            target = self._seek_hold_target_ms
            if target < 0:
                return True
            if abs(position_ms - target) <= 3500:
                return True
            return False

    def _evaluate_seek_once(self, position_ms: int) -> dict | None:
        try:
            import websocket  # type: ignore[import-untyped]
        except ImportError as exc:
            raise RuntimeError("websocket-client is required for CDP progress") from exc

        targets = self._ordered_targets(self._list_targets())
        if not targets:
            raise RuntimeError("No CDP debug target for NetEase")

        click_expression = _CLICK_COORDS_JS_TEMPLATE % position_ms
        seek_expression = _SEEK_JS_TEMPLATE % position_ms
        last_error: Exception | None = None
        for target in targets:
            ws_url = str(target.get("webSocketDebuggerUrl") or "")
            if not ws_url:
                continue
            try:
                return self._seek_on_target(websocket, ws_url, click_expression, seek_expression)
            except Exception as exc:
                last_error = exc
                logger.debug("CDP seek failed (%s): %s", target.get("title", ""), exc)

        if last_error is not None:
            raise last_error
        raise RuntimeError("No usable CDP target for NetEase")

    def get_snapshot(self, max_age_seconds: float = 2.0) -> CdpProgressSnapshot | None:
        with self._lock:
            if self._position_ms < 0:
                return None
            if time.monotonic() - self._updated_mono > max_age_seconds:
                return None
            return CdpProgressSnapshot(
                position_ms=self._position_ms,
                duration_ms=self._duration_ms,
                song_id=self._song_id,
            )

    def _set_snapshot(self, position_ms: int, duration_ms: int, song_id: str, connected: bool) -> None:
        with self._lock:
            self._connected = connected
            self._position_ms = position_ms
            self._duration_ms = duration_ms
            self._song_id = song_id
            self._updated_mono = time.monotonic()

    def _seek_on_target(
        self,
        websocket_module: object,
        ws_url: str,
        click_expression: str,
        seek_expression: str,
    ) -> dict | None:
        message_id = 0

        def next_id() -> int:
            nonlocal message_id
            message_id += 1
            return message_id

        def call(ws: object, method: str, params: dict | None = None) -> dict:
            request_id = next_id()
            payload = {"id": request_id, "method": method, "params": params or {}}
            ws.send(json.dumps(payload, ensure_ascii=False))
            while True:
                raw = ws.recv()
                if not raw:
                    continue
                data = json.loads(raw)
                if data.get("id") == request_id:
                    return data

        def evaluate(ws: object, expr: str) -> dict | None:
            response = call(
                ws,
                "Runtime.evaluate",
                {"expression": expr, "returnByValue": True, "awaitPromise": False},
            )
            result = response.get("result") or {}
            inner = result.get("result") or {}
            if inner.get("type") == "object" and "value" in inner:
                value = inner.get("value")
                return value if isinstance(value, dict) else None
            return None

        def dispatch_seek_click(ws: object, x: float, y: float) -> None:
            for event_type in ("mouseMoved", "mousePressed", "mouseReleased"):
                call(
                    ws,
                    "Input.dispatchMouseEvent",
                    {
                        "type": event_type,
                        "x": x,
                        "y": y,
                        "button": "left",
                        "clickCount": 1,
                    },
                )

        create_connection = websocket_module.create_connection
        ws = create_connection(ws_url, timeout=3)
        try:
            call(ws, "Runtime.enable")
            click_ok = False
            coords = evaluate(ws, click_expression)
            if coords is not None and coords.get("success"):
                dispatch_seek_click(ws, float(coords["x"]), float(coords["y"]))
                click_ok = True
            seek_result = evaluate(ws, seek_expression)
            if seek_result is not None and seek_result.get("success"):
                seek_result["click"] = click_ok
                return seek_result
            if click_ok:
                return {
                    "success": True,
                    "method": "cdp_click",
                    "value": coords.get("value") if coords else None,
                    "click": True,
                }
            return seek_result
        finally:
            ws.close()

    def _evaluate_once(self, expression: str) -> dict | None:
        try:
            import websocket  # type: ignore[import-untyped]
        except ImportError as exc:
            raise RuntimeError("websocket-client is required for CDP progress") from exc

        targets = self._ordered_targets(self._list_targets())
        if not targets:
            raise RuntimeError("No CDP debug target for NetEase")

        last_error: Exception | None = None
        for target in targets:
            ws_url = str(target.get("webSocketDebuggerUrl") or "")
            if not ws_url:
                continue
            try:
                return self._evaluate_on_target(websocket, ws_url, expression)
            except Exception as exc:
                last_error = exc
                logger.debug("CDP one-shot failed (%s): %s", target.get("title", ""), exc)

        if last_error is not None:
            raise last_error
        raise RuntimeError("No usable CDP target for NetEase")

    def _evaluate_on_target(self, websocket_module: object, ws_url: str, expression: str) -> dict | None:
        message_id = 0

        def next_id() -> int:
            nonlocal message_id
            message_id += 1
            return message_id

        def call(ws: object, method: str, params: dict | None = None) -> dict:
            request_id = next_id()
            payload = {"id": request_id, "method": method, "params": params or {}}
            ws.send(json.dumps(payload, ensure_ascii=False))
            while True:
                raw = ws.recv()
                if not raw:
                    continue
                data = json.loads(raw)
                if data.get("id") == request_id:
                    return data

        def evaluate(ws: object, expr: str) -> dict | None:
            response = call(
                ws,
                "Runtime.evaluate",
                {"expression": expr, "returnByValue": True, "awaitPromise": False},
            )
            result = response.get("result") or {}
            inner = result.get("result") or {}
            if inner.get("type") == "object" and "value" in inner:
                value = inner.get("value")
                return value if isinstance(value, dict) else None
            return None

        create_connection = websocket_module.create_connection
        ws = create_connection(ws_url, timeout=3)
        try:
            call(ws, "Runtime.enable")
            return evaluate(ws, expression)
        finally:
            ws.close()

    def _run_loop(self) -> None:
        while not self._stop.is_set():
            try:
                self._session_loop()
            except Exception:
                with self._lock:
                    self._connected = False
                if not self._logged_unavailable:
                    logger.warning(
                        "NetEase CDP progress unavailable on port %s. "
                        "Restart cloudmusic with --remote-debugging-port=%s (daemon can auto-add on launch).",
                        self.port,
                        self.port,
                    )
                    self._logged_unavailable = True
            if self._stop.wait(3.0):
                break

    def _session_loop(self) -> None:
        try:
            import websocket  # type: ignore[import-untyped]
        except ImportError as exc:
            raise RuntimeError("websocket-client is required for CDP progress") from exc

        targets = self._ordered_targets(self._list_targets())
        if not targets:
            raise RuntimeError("No CDP debug target for NetEase")

        last_error: Exception | None = None
        for target in targets:
            ws_url = str(target.get("webSocketDebuggerUrl") or "")
            if not ws_url:
                continue
            try:
                self._run_target_session(websocket, ws_url, target)
                return
            except Exception as exc:
                last_error = exc
                logger.debug("CDP target failed (%s): %s", target.get("title", ""), exc)

        if last_error is not None:
            raise last_error
        raise RuntimeError("No usable CDP target for NetEase")

    def _run_target_session(self, websocket_module: object, ws_url: str, target: dict) -> None:
        message_id = 0

        def next_id() -> int:
            nonlocal message_id
            message_id += 1
            return message_id

        def call(ws: object, method: str, params: dict | None = None) -> dict:
            request_id = next_id()
            payload = {"id": request_id, "method": method, "params": params or {}}
            ws.send(json.dumps(payload, ensure_ascii=False))
            while True:
                raw = ws.recv()
                if not raw:
                    continue
                data = json.loads(raw)
                if data.get("id") == request_id:
                    return data

        def evaluate(ws: object, expression: str) -> dict | None:
            response = call(
                ws,
                "Runtime.evaluate",
                {"expression": expression, "returnByValue": True, "awaitPromise": False},
            )
            result = response.get("result") or {}
            inner = result.get("result") or {}
            if inner.get("type") == "object" and "value" in inner:
                value = inner.get("value")
                return value if isinstance(value, dict) else None
            return None

        create_connection = websocket_module.create_connection
        ws = create_connection(ws_url, timeout=3)
        try:
            call(ws, "Runtime.enable")
            register_result = evaluate(ws, _REGISTER_JS)
            if register_result is None or not register_result.get("success", False):
                raise RuntimeError(f"CDP hook failed on target: {register_result}")

            self._logged_unavailable = False
            if not self._logged_ready:
                logger.info("NetEase CDP progress connected (port=%s, target=%s)", self.port, target.get("title", ""))
                self._logged_ready = True

            while not self._stop.is_set():
                payload = evaluate(ws, _POLL_JS)
                if payload:
                    current = float(payload.get("currentTime") or 0)
                    duration = float(payload.get("duration") or 0)
                    position_ms, duration_ms = _normalize_progress_times(current, duration)
                    song_id = str(payload.get("songId") or "").strip()
                    if self._accept_poll_position(position_ms):
                        self._set_snapshot(
                            position_ms=position_ms,
                            duration_ms=duration_ms,
                            song_id=song_id,
                            connected=True,
                        )
                    if song_id and song_id != self._logged_song_id:
                        logger.info("CDP song id: %s (position_ms=%s duration_ms=%s)", song_id, position_ms, duration_ms)
                        self._logged_song_id = song_id
                if self._stop.wait(self.poll_interval_seconds):
                    break
        finally:
            ws.close()

    def _list_targets(self) -> list[dict]:
        url = f"http://127.0.0.1:{self.port}/json/list"
        with urllib.request.urlopen(url, timeout=1.5) as response:
            data = json.loads(response.read().decode("utf-8", errors="replace"))
        if not isinstance(data, list):
            return []
        return [item for item in data if isinstance(item, dict)]

    def _ordered_targets(self, targets: list[dict]) -> list[dict]:
        page_targets = [target for target in targets if target.get("type") == "page" and target.get("webSocketDebuggerUrl")]
        preferred_hints = ("orpheus", "music.163", "cloudmusic", "netease", "163")

        def rank(target: dict) -> tuple[int, str]:
            url = str(target.get("url") or "").lower()
            title = str(target.get("title") or "").lower()
            score = 0
            if any(hint in url for hint in preferred_hints):
                score += 4
            if any(hint in title for hint in preferred_hints):
                score += 2
            if "about:blank" in url:
                score -= 2
            return (-score, url)

        return sorted(page_targets, key=rank)


def process_has_cdp_flag(process_name: str, port: int) -> bool:
    import psutil

    expected = process_name.lower()
    flag = f"--remote-debugging-port={port}"
    for process in psutil.process_iter(["name", "cmdline"]):
        name = str(process.info.get("name") or "").lower()
        if expected and name != expected:
            continue
        cmdline = process.info.get("cmdline") or []
        joined = " ".join(str(part) for part in cmdline).lower()
        if flag.lower() in joined:
            return True
    return False
