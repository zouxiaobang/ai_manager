"""Shared settings for LVGL bitmap font generation."""

from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FONT_SOURCE_DIR = ROOT / "main" / "fonts" / "source"
SYMBOLS_FILE = ROOT / "scripts" / "gb2312_symbols.txt"

# 阿里妈妈刀隶体（免费商用）：https://www.alibabafonts.com
FONT_CANDIDATES = [
    FONT_SOURCE_DIR / "AlimamaDaoLiTi.ttf",
    FONT_SOURCE_DIR / "AlimamaDaoLiTi-Regular.ttf",
    FONT_SOURCE_DIR / "阿里妈妈刀隶体.ttf",
    Path(r"C:\Windows\Fonts\AlimamaDaoLiTi.ttf"),
    Path(r"C:\Windows\Fonts\阿里妈妈刀隶体.ttf"),
    # 未安装刀隶体时的回退（生成结果仍为 bitmap，不是刀隶风格）
    Path(r"C:\Windows\Fonts\simhei.ttf"),
]


def resolve_font_file() -> Path | None:
    for path in FONT_CANDIDATES:
        if path.is_file():
            return path
    return None


def font_install_hint() -> str:
    return (
        "请将阿里妈妈刀隶体 TTF 放到:\n"
        f"  {FONT_SOURCE_DIR / 'AlimamaDaoLiTi.ttf'}\n"
        "官方下载: https://www.alibabafonts.com （搜索「刀隶体」）\n"
        "或安装到 Windows 字体目录后重新运行生成脚本。"
    )
