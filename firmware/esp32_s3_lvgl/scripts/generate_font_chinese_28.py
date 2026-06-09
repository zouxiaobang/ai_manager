#!/usr/bin/env python3
"""Generate simplified-Chinese LVGL font (28px) using Alimama DaoLiTi when available."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

from font_config import SYMBOLS_FILE, font_install_hint, resolve_font_file

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "main" / "fonts" / "font_chinese_28.c"
FONT_SIZE = 28
FONT_NAME = "font_chinese_28"


def load_symbols() -> str:
    if SYMBOLS_FILE.exists():
        return SYMBOLS_FILE.read_text(encoding="utf-8")
    from generate_font_chinese_20 import collect_gb2312_symbols

    symbols = collect_gb2312_symbols()
    SYMBOLS_FILE.write_text(symbols, encoding="utf-8")
    return symbols


def main() -> int:
    font_file = resolve_font_file()
    if font_file is None:
        print(font_install_hint(), file=sys.stderr)
        return 1

    symbols = load_symbols()
    print(f"Font: {font_file}")
    print(f"Characters: {len(symbols)}, size={FONT_SIZE}px")

    cmd = [
        "npx.cmd",
        "--yes",
        "lv_font_conv",
        "--font",
        str(font_file),
        "-r",
        "0x20-0x7F",
        "-r",
        "0x3000-0x303F",
        "-r",
        "0xFF00-0xFFEF",
        "--symbols",
        symbols,
        "--size",
        str(FONT_SIZE),
        "--bpp",
        "1",
        "--format",
        "lvgl",
        "--no-compress",
        "--no-prefilter",
        "--lv-font-name",
        FONT_NAME,
        "-o",
        str(OUTPUT),
    ]

    print("Running lv_font_conv ...")
    subprocess.run(cmd, check=True)

    content = OUTPUT.read_text(encoding="utf-8")
    include_block = (
        "#ifdef LV_LVGL_H_INCLUDE_SIMPLE\n"
        "#include \"lvgl.h\"\n"
        "#else\n"
        "#include \"lvgl/lvgl.h\"\n"
        "#endif\n"
    )
    if "#include \"lvgl.h\"" not in content.split("BITMAPS", 1)[0]:
        content = content.replace("#include \"lvgl/lvgl.h\"\n", include_block, 1)
        OUTPUT.write_text(content, encoding="utf-8")

    print(f"Wrote {OUTPUT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
