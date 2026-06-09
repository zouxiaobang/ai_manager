#!/usr/bin/env python3
"""Regenerate 20px and 28px Chinese LVGL fonts from the configured TTF."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent


def main() -> int:
    scripts = [
        ROOT / "generate_font_chinese_20.py",
        ROOT / "generate_font_chinese_28.py",
    ]
    for script in scripts:
        print(f"\n=== {script.name} ===")
        result = subprocess.run([sys.executable, str(script)], check=False)
        if result.returncode != 0:
            return result.returncode
    print("\nAll fonts generated.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
