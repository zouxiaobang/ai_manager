#!/usr/bin/env python3
"""Render 800x480 home UI preview PNG (matches ui_home_static_layout.h)."""

from __future__ import annotations

import random
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
HEADER = ROOT / "main" / "include" / "ui_home_static_layout.h"
LAYOUT_C = ROOT / "main" / "ui_home_static_layout.c"
ASSETS = ROOT / "sdcard_assets" / "assets"
OUT_DIR = ROOT / "preview"
OUT_FILE = OUT_DIR / "ui_home_preview.png"

PANEL_W, PANEL_H = 800, 480

COL = {
    "bg": (8, 8, 26),
    "card": (16, 16, 40),
    "green": (139, 195, 74),
    "blue": (41, 182, 246),
    "lyric": (144, 164, 174),
    "dock_bg": (12, 12, 32),
    "dock_edge": (42, 42, 80),
    "white": (236, 239, 241),
    "dot_idle": (64, 64, 96),
    "orange": (255, 152, 0),
    "cyan": (66, 165, 245),
}


def parse_int_define(text: str, name: str, default: int) -> int:
    m = re.search(rf"#define\s+{re.escape(name)}\s+(\d+)", text)
    return int(m.group(1)) if m else default


def parse_card_width(text: str, side_margin: int, gap: int) -> int:
    m = re.search(
        r"#define\s+UI_HOME_CARD_W\s+\(\(PANEL_WIDTH\s*-\s*UI_HOME_CARD_SIDE_MARGIN\s*\*\s*2\s*-\s*UI_HOME_CARD_GAP\)\s*/\s*2\)",
        text,
    )
    if m:
        return (PANEL_W - side_margin * 2 - gap) // 2
    m = re.search(rf"#define\s+UI_HOME_CARD_W\s+(\d+)", text)
    return int(m.group(1)) if m else 338


def load_layout() -> dict:
    h = HEADER.read_text(encoding="utf-8")
    c = LAYOUT_C.read_text(encoding="utf-8")
    side = parse_int_define(h, "UI_HOME_CARD_SIDE_MARGIN", 52)
    gap = parse_int_define(h, "UI_HOME_CARD_GAP", 20)
    card_border_p = parse_int_define(h, "UI_CARD_BORDER_P", parse_int_define(c, "UI_CARD_BORDER_P", 3))
    card_corner_steps = parse_int_define(h, "UI_CARD_CORNER_STEPS", parse_int_define(c, "UI_CARD_CORNER_STEPS", 4))
    dock_sel_corner_steps = parse_int_define(
        h, "UI_DOCK_SEL_CORNER_STEPS", parse_int_define(c, "UI_DOCK_SEL_CORNER_STEPS", 3)
    )
    return {
        "margin": parse_int_define(h, "UI_HOME_MARGIN", 16),
        "side_margin": side,
        "status_y": parse_int_define(h, "UI_HOME_STATUS_Y", 6),
        "cards_y": parse_int_define(h, "UI_HOME_CARDS_Y", 44),
        "card_w": parse_card_width(h, side, gap),
        "card_h": parse_int_define(h, "UI_HOME_CARD_H", 296),
        "card_gap": gap,
        "dock_y": parse_int_define(h, "UI_HOME_DOCK_Y", 362),
        "dock_h": parse_int_define(h, "UI_HOME_DOCK_H", 92),
        "card_border_p": card_border_p,
        "card_corner_inset": card_border_p * card_corner_steps,
        "card_inner_pad": card_border_p * card_corner_steps + card_border_p,
        "dock_sel_corner_inset": card_border_p * dock_sel_corner_steps,
    }


def hex_rgb(color: tuple[int, int, int]) -> tuple[int, int, int]:
    return color


def draw_jagged_border(
    draw: ImageDraw.ImageDraw,
    x: int,
    y: int,
    w: int,
    h: int,
    color: tuple[int, int, int],
    thickness: int,
    corner_inset: int,
) -> None:
    p = max(1, thickness)
    steps = max(1, corner_inset // p)
    inset = steps * p

    if w > inset * 2:
        draw.rectangle((x + inset, y, x + w - inset, y + p), fill=color)
        draw.rectangle((x + inset, y + h - p, x + w - inset, y + h), fill=color)
    if h > inset * 2:
        draw.rectangle((x, y + inset, x + p, y + h - inset), fill=color)
        draw.rectangle((x + w - p, y + inset, x + w, y + h - inset), fill=color)

    for s in range(steps):
        dx = (steps - 1 - s) * p
        dy = s * p
        draw.rectangle((x + dx, y + dy, x + dx + p, y + dy + p), fill=color)
        draw.rectangle((x + w - dx - p, y + dy, x + w - dx, y + dy + p), fill=color)
        draw.rectangle((x + dx, y + h - dy - p, x + dx + p, y + h - dy), fill=color)
        draw.rectangle((x + w - dx - p, y + h - dy - p, x + w - dx, y + h - dy), fill=color)


def draw_stars(img: Image.Image) -> None:
    draw = ImageDraw.Draw(img)
    rng = random.Random(42)
    palette = [(126, 184, 232), (90, 138, 170), (144, 202, 249), (79, 195, 247)]
    for _ in range(88):
        sx = rng.randint(4, PANEL_W - 8)
        sy = rng.randint(4, PANEL_H - 100)
        c = palette[rng.randint(0, len(palette) - 1)]
        sz = rng.choice([1, 1, 2])
        draw.rectangle((sx, sy, sx + sz - 1, sy + sz - 1), fill=c)


def load_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        "C:/Windows/Fonts/msyh.ttc",
        "C:/Windows/Fonts/simhei.ttf",
        "C:/Windows/Fonts/simsun.ttc",
    ]
    for path in candidates:
        if Path(path).is_file():
            try:
                return ImageFont.truetype(path, size)
            except OSError:
                continue
    return ImageFont.load_default()


def paste_asset(img: Image.Image, name: str, x: int, y: int, size: int) -> None:
    path = ASSETS / name
    if not path.is_file():
        return
    icon = Image.open(path).convert("RGBA").resize((size, size), Image.NEAREST)
    img.paste(icon, (x, y), icon)


def draw_card(
    img: Image.Image,
    draw: ImageDraw.ImageDraw,
    x: int,
    y: int,
    layout: dict,
    border_color: tuple[int, int, int],
    title: str,
    body_lines: list[str],
    accent: tuple[int, int, int],
    show_tomato: bool = False,
) -> None:
    w, h = layout["card_w"], layout["card_h"]
    pad = layout["card_inner_pad"]
    draw_jagged_border(draw, x, y, w, h, border_color, layout["card_border_p"], layout["card_corner_inset"])
    draw.rectangle((x + pad, y + pad, x + w - pad, y + h - pad), fill=COL["card"])

    font_title = load_font(16)
    font_body = load_font(14)
    font_time = load_font(28)

    ty = y + pad + 8
    tw = draw.textlength(title, font=font_title)
    draw.text((x + (w - tw) / 2, ty), title, fill=border_color, font=font_title)
    draw.line((x + pad + 8, ty + 22, x + w - pad - 8, ty + 22), fill=border_color, width=1)

    cy = ty + 36
    if show_tomato:
        ts = 88
        tx = x + (w - ts) // 2
        paste_asset(img, "tomato.png", tx, cy, ts)
        cy += ts + 8
        time_txt = "25:00"
        tw2 = draw.textlength(time_txt, font=font_time)
        draw.text((x + (w - tw2) / 2, cy), time_txt, fill=border_color, font=font_time)
        cy += 34
        act = "▶ 开始专注 ◀"
        aw = draw.textlength(act, font=font_body)
        draw.text((x + (w - aw) / 2, cy), act, fill=border_color, font=font_body)
    else:
        song = body_lines[0] if body_lines else ""
        sw = draw.textlength(song, font=font_body)
        draw.text((x + (w - sw) / 2, cy), song, fill=accent, font=font_body)
        cy += 24
        for line in body_lines[1:]:
            lw = draw.textlength(line, font=font_body)
            draw.text((x + (w - lw) / 2, cy), line, fill=COL["lyric"], font=font_body)
            cy += 20


def draw_dock(img: Image.Image, draw: ImageDraw.ImageDraw, layout: dict) -> None:
    x = layout["margin"]
    y = layout["dock_y"]
    w = PANEL_W - layout["margin"] * 2
    h = layout["dock_h"]
    pad = layout["card_inner_pad"]
    draw_jagged_border(draw, x, y, w, h, COL["dock_edge"], layout["card_border_p"], layout["card_corner_inset"])
    draw.rectangle((x + pad, y + pad, x + w - pad, y + h - pad), fill=COL["dock_bg"])

    labels = ["番茄钟", "歌词", "首页", "专注", "设置"]
    colors = [COL["green"], COL["blue"], 0xCE93D8, COL["cyan"], COL["cyan"]]
    icons = ["dock_pomo.png", "dock_lyrics.png", "dock_home.png", "dock_lock.png", "dock_settings.png"]
    slot_w = w // 5
    frame_w, frame_h = 52, 64
    sel_border_pad_x = 20

    for i, (label, color, icon) in enumerate(zip(labels, colors, icons)):
        sx = x + i * slot_w
        fx = sx + (slot_w - frame_w) // 2
        fy = y + (h - frame_h) // 2
        if i == 2:
            draw_jagged_border(
                draw,
                fx - sel_border_pad_x,
                fy,
                frame_w + sel_border_pad_x * 2,
                frame_h,
                color,
                layout["card_border_p"],
                layout["dock_sel_corner_inset"],
            )
        paste_asset(img, icon, fx + (frame_w - 24) // 2, fy + 6, 24)
        tw = draw.textlength(label, font=load_font(14))
        draw.text((fx + (frame_w - tw) / 2, fy + frame_h - 20), label, fill=color, font=load_font(14))


def render() -> Path:
    layout = load_layout()
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    img = Image.new("RGB", (PANEL_W, PANEL_H), COL["bg"])
    draw_stars(img)
    draw = ImageDraw.Draw(img)

    font_status = load_font(28)
    draw.text((layout["side_margin"], layout["status_y"]), "14:00", fill=COL["white"], font=font_status)

    lock_path = ASSETS / "icon_lock.png"
    wifi_path = ASSETS / "icon_wifi.png"
    icon_y = layout["status_y"] + 4
    if lock_path.is_file():
        lock = Image.open(lock_path).convert("RGBA")
        img.paste(lock, (PANEL_W - layout["side_margin"] - 54, icon_y), lock)
    if wifi_path.is_file():
        wifi = Image.open(wifi_path).convert("RGBA")
        img.paste(wifi, (PANEL_W - layout["side_margin"] - 24, icon_y + 1), wifi)

    cx = layout["side_margin"]
    cy = layout["cards_y"]
    gap = layout["card_gap"]
    cw = layout["card_w"]

    draw_card(
        img,
        draw,
        cx,
        cy,
        layout,
        COL["green"],
        "◆ 番茄钟 ◆",
        [],
        COL["green"],
        show_tomato=True,
    )
    draw_card(
        img,
        draw,
        cx + cw + gap,
        cy,
        layout,
        COL["blue"],
        "◆ 歌词 ◆",
        [
            "夜空中最亮的星",
            "能否听清",
            "那仰望的人",
            "心底的孤独和叹息",
            "夜空中最亮的星",
            "...",
        ],
        COL["blue"],
    )

    draw_dock(img, draw, layout)

    img.save(OUT_FILE)
    return OUT_FILE


if __name__ == "__main__":
    path = render()
    print(f"Preview saved: {path}")
