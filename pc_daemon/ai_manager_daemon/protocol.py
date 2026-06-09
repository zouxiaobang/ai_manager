from __future__ import annotations

import time
from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class ProtocolMessage:
    type: str
    payload: dict[str, Any]
    session_id: str = ""
    request_id: str | None = None
    timestamp_ms: int = 0

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "ProtocolMessage":
        message_type = data.get("type")
        if not isinstance(message_type, str) or not message_type:
            raise ValueError("Message type is required")

        payload = data.get("payload", {})
        if not isinstance(payload, dict):
            raise ValueError("Message payload must be an object")

        request_id = data.get("request_id")
        if request_id is not None and not isinstance(request_id, str):
            raise ValueError("request_id must be a string")

        return cls(
            type=message_type,
            request_id=request_id,
            session_id=str(data.get("session_id", "")),
            timestamp_ms=int(data.get("timestamp_ms", 0)),
            payload=payload,
        )


def now_ms() -> int:
    return int(time.time() * 1000)


def make_message(
    message_type: str,
    payload: dict[str, Any] | None = None,
    *,
    session_id: str = "",
    request_id: str | None = None,
) -> dict[str, Any]:
    message: dict[str, Any] = {
        "type": message_type,
        "session_id": session_id,
        "timestamp_ms": now_ms(),
        "payload": payload or {},
    }

    if request_id:
        message["request_id"] = request_id

    return message


def ack(message: ProtocolMessage, payload: dict[str, Any] | None = None) -> dict[str, Any]:
    return make_message(
        "ack",
        {"ok": True, **(payload or {})},
        session_id=message.session_id,
        request_id=message.request_id,
    )


def error(message: ProtocolMessage, code: str, detail: str) -> dict[str, Any]:
    return make_message(
        "error",
        {"code": code, "message": detail},
        session_id=message.session_id,
        request_id=message.request_id,
    )
