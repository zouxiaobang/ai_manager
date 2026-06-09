from __future__ import annotations

import logging

logger = logging.getLogger("uvicorn.error")

try:
    from zhconv import convert as _zh_convert
except ImportError:  # pragma: no cover - optional at runtime until deps installed
    _zh_convert = None
    logger.warning("zhconv is not installed; Chinese text will not be converted to Simplified. Run: pip install -r requirements.txt")


def to_simplified(text: str) -> str:
    if not text or _zh_convert is None:
        return text
    return _zh_convert(text, "zh-cn")
