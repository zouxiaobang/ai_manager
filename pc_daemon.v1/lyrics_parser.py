from __future__ import annotations

import re
from dataclasses import dataclass


@dataclass(frozen=True)
class LyricLine:
    start_ms: int
    text: str


@dataclass(frozen=True)
class LyricSnapshot:
    prev_prev_line: str = ""
    prev_line: str = ""
    line: str = ""
    next_line: str = ""
    next_next_line: str = ""
    line_start_ms: int = 0
    line_end_ms: int = 0


_TIME_RE = re.compile(r"\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]")
_META_RE = re.compile(r"\[(ti|ar|al|by|offset|re|ve):", re.IGNORECASE)


def is_meta_line(text: str) -> bool:
    if _META_RE.match(text):
        return True
    meta_keywords = [
        "作词", "作曲", "编曲", "演唱", "混音", "录音", "监制", "制作人",
        "出品", "版权", "著作权", "许可", "翻唱", "翻录", "复制",
        "AI", "人工智能", "训练", "模拟", "学习", "研究",
        "Entertainment", "Production", "Company", "OP", "SP",
        "Wajijiwa", "不得", "禁止", "未经", "Copyright",
        "和声", "发行", "录音室", "混音室", "母带", "吉他", "贝斯", "鼓",
        "键盘", "钢琴", "弦乐", "配唱", "伴唱", "录音师", "混音师", "总监",
        "策划", "统筹", "宣发", "推广", "发行人", "经纪", "唱片", "专辑"
    ]
    for kw in meta_keywords:
        if kw in text:
            return True
    return False


def parse_lrc(text: str) -> list[LyricLine]:
    lines: list[LyricLine] = []
    for raw in text.splitlines():
        row = raw.strip()
        if not row:
            continue
        if is_meta_line(row):
            continue
        timestamps = list(_TIME_RE.finditer(row))
        if not timestamps:
            continue
        lyric_text = _TIME_RE.sub("", row).strip()
        if not lyric_text:
            continue
        for match in timestamps:
            minute = int(match.group(1))
            second = int(match.group(2))
            frac = match.group(3) or "0"
            if len(frac) == 1:
                frac_ms = int(frac) * 100
            elif len(frac) == 2:
                frac_ms = int(frac) * 10
            else:
                frac_ms = int(frac[:3])
            start_ms = minute * 60_000 + second * 1_000 + frac_ms
            lines.append(LyricLine(start_ms=start_ms, text=lyric_text))
    lines.sort(key=lambda item: item.start_ms)
    return lines


def pick_lines(lines: list[LyricLine], position_ms: int) -> LyricSnapshot:
    if not lines:
        return LyricSnapshot()

    index = 0
    for i, item in enumerate(lines):
        if item.start_ms <= position_ms:
            index = i
        else:
            break

    current = lines[index]
    prev_prev_text = lines[index - 2].text if index >= 2 else ""
    prev_text = lines[index - 1].text if index > 0 else ""
    next_text = lines[index + 1].text if index + 1 < len(lines) else ""
    next_next_text = lines[index + 2].text if index + 2 < len(lines) else ""
    end_ms = lines[index + 1].start_ms if index + 1 < len(lines) else current.start_ms + 8_000
    return LyricSnapshot(
        prev_prev_line=prev_prev_text,
        prev_line=prev_text,
        line=current.text,
        next_line=next_text,
        next_next_line=next_next_text,
        line_start_ms=current.start_ms,
        line_end_ms=end_ms,
    )
