#!/usr/bin/env python3
"""Generate GB2312 Chinese font for LVGL — full CJK + punctuation."""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FONTS_DIR = ROOT / "main" / "fonts"
MAIN_DIR = ROOT / "main"
GLYPHS_OUT = Path(__file__).resolve().parent / "glyphs_gb2312.txt"
NPX = shutil.which("npx") or shutil.which("npx.cmd")
SIMHEI = Path(__file__).resolve().parent / "msyh.ttf"
SEGUISYM = Path(r"C:\Windows\Fonts\seguisym.ttf")
BATCH_SIZE = 900
SYMBOL_RANGES = "0x25B6,0x25C0,0x266A,0x2026"


def _is_chinese_char(cp: int) -> bool:
    """Check whether a Unicode codepoint is a Chinese character or punctuation."""
    # CJK Unified Ideographs
    if 0x4E00 <= cp <= 0x9FFF:
        return True
    # CJK Extension A
    if 0x3400 <= cp <= 0x4DBF:
        return True
    # CJK Symbols and Punctuation （。、等）
    if 0x3000 <= cp <= 0x303F:
        return True
    # Fullwidth Forms （，。！？：；等）
    if 0xFF00 <= cp <= 0xFFEF:
        return True
    # General Punctuation subset（·—…）
    if 0x2000 <= cp <= 0x206F:
        return True
    # Middle dot used in UI labels
    if cp in (0x00B7,):
        return True
    return False


def collect_source_chars() -> set[str]:
    """Scan main/ source files for any Chinese / CJK-punctuation characters used."""
    chars: set[str] = set()
    for dirpath, _dirnames, filenames in os.walk(MAIN_DIR):
        for fn in filenames:
            if not fn.endswith((".cpp", ".h", ".c")):
                continue
            path = os.path.join(dirpath, fn)
            try:
                with open(path, "r", encoding="utf-8") as f:
                    for line in f:
                        for ch in line:
                            if _is_chinese_char(ord(ch)):
                                chars.add(ch)
            except Exception as exc:
                print(f"  [warn] skipping {fn}: {exc}", file=sys.stderr)
    if chars:
        print(f"Source scan: {len(chars)} unique CJK/punctuation chars found.")
    return chars


def collect_gb2312_chars() -> str:
    chars: list[str] = []
    for row in range(16, 88):
        for col in range(1, 95):
            try:
                chars.append(bytes([row + 0xA0, col + 0xA0]).decode("gb2312"))
            except UnicodeDecodeError:
                pass
    # ASCII helpers for UI labels
    extra = " -.0123456789"
    merged = extra + "".join(chars)
    return "".join(dict.fromkeys(merged))


def run_lv_font_conv(symbols: str, out_c: Path, font_name: str) -> None:
    if NPX is None:
        raise RuntimeError("npx not found in PATH")
    if not SIMHEI.is_file():
        raise RuntimeError(f"Missing font file: {SIMHEI}")

    out_c.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        NPX,
        "--yes",
        "lv_font_conv",
        "--font",
        str(SIMHEI),
        "--symbols",
        symbols,
        "--size",
        "16",
        "--bpp",
        "4",
        "--format",
        "lvgl",
        "--no-compress",
        "--no-prefilter",
        "--force-fast-kern-format",
        "--lv-font-name",
        font_name,
        "-o",
        str(out_c),
    ]
    if SEGUISYM.is_file():
        cmd.extend(["--font", str(SEGUISYM), "-r", SYMBOL_RANGES])

    print(f"  -> {out_c.name} ({len(symbols)} glyphs)")
    result = subprocess.run(cmd, check=False, text=True, capture_output=True)
    if result.returncode != 0:
        print(result.stderr or result.stdout, file=sys.stderr)
        raise RuntimeError(f"lv_font_conv failed for {out_c.name}")


def write_gb2312_header(part_count: int) -> None:
    header = ROOT / "main" / "include" / "lv_font_cn_gb2312.h"
    lines = [
        "#pragma once",
        "",
        '#include "lvgl.h"',
        "",
        "#ifdef __cplusplus",
        'extern "C" {',
        "#endif",
        "",
        "/* GB2312 font parts (fallback chain). Auto-generated. */",
    ]
    for i in range(part_count):
        lines.append(f"extern const lv_font_t lv_font_cn_gb2312_16_{i};")
    lines.extend(["", "#ifdef __cplusplus", "}", "#endif", ""])
    header.write_text("\n".join(lines), encoding="utf-8")


def patch_include_header(c_path: Path) -> None:
    text = c_path.read_text(encoding="utf-8")
    marker = '#include "lv_font_cn_gb2312.h"'
    if marker in text:
        return
    needle = "#endif\n\n#ifndef LV_FONT_CN_GB2312"
    insert = f'#endif\n\n{marker}\n\n#ifndef LV_FONT_CN_GB2312'
    if needle not in text:
        raise RuntimeError(f"include injection point not found in {c_path.name}")
    c_path.write_text(text.replace(needle, insert, 1), encoding="utf-8")


def patch_fallback(c_path: Path, fallback_name: str | None) -> None:
    if fallback_name is None:
        return
    text = c_path.read_text(encoding="utf-8")
    old = ".fallback = NULL,"
    new = f".fallback = &{fallback_name},"
    if old not in text:
        raise RuntimeError(f"fallback slot not found in {c_path.name}")
    c_path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--batch-size", type=int, default=BATCH_SIZE)
    args = parser.parse_args()

    # Collect all characters from GB2312 charset (CJK ideographs)
    symbols = collect_gb2312_chars()
    # Merge in any Chinese/CJK-punctuation used in source files
    source_chars = collect_source_chars()
    for ch in source_chars:
        if ch not in symbols:
            symbols += ch
    symbols = "".join(dict.fromkeys(symbols))

    GLYPHS_OUT.write_text(symbols, encoding="utf-8")
    print(f"Total symbols: {len(symbols)} chars (GB2312 + source scan) -> {GLYPHS_OUT}")

    batches: list[str] = []
    ascii_chars = [c for c in symbols if ord(c) < 128]
    han_chars = [c for c in symbols if ord(c) >= 128]
    ascii_blob = "".join(ascii_chars)

    for i in range(0, len(han_chars), args.batch_size):
        chunk = ascii_blob if i == 0 else ""
        chunk += "".join(han_chars[i : i + args.batch_size])
        batches.append(chunk)

    print(f"Generating {len(batches)} font part(s)...")
    part_paths: list[Path] = []
    for idx, chunk in enumerate(batches):
        name = f"lv_font_cn_gb2312_16_{idx}"
        out_c = FONTS_DIR / f"{name}.c"
        run_lv_font_conv(chunk, out_c, name)
        part_paths.append(out_c)
        print(f"     {out_c.name}: {out_c.stat().st_size // 1024} KB")

    for i in range(len(part_paths) - 1):
        patch_fallback(part_paths[i], part_paths[i + 1].stem)

    write_gb2312_header(len(part_paths))
    for p in part_paths:
        patch_include_header(p)

    manifest = FONTS_DIR / "gb2312_font_parts.cmake"
    lines = ["# Auto-generated by fonts/generate_gb2312_font.py", "set(GB2312_FONT_SRCS"]
    for p in part_paths:
        lines.append(f'    "fonts/{p.name}"')
    lines.append(")")
    manifest.write_text("\n".join(lines) + "\n", encoding="utf-8")

    total_kb = sum(p.stat().st_size for p in part_paths) // 1024
    print(f"Done. Total ~{total_kb} KB in flash. Rebuild: idf.py build flash")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
