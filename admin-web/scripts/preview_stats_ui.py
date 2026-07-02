#!/usr/bin/env python3
"""Generate pixel-style Pomodoro stats page design mockups (4 schemes)."""

from __future__ import annotations

import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "preview" / "stats"
W, H = 1280, 720

_FONT_CACHE: dict[tuple[int, bool], ImageFont.FreeTypeFont | ImageFont.ImageFont] = {}

COL = {
    "bg": (8, 8, 26),
    "card": (16, 16, 40),
    "card_inner": (16, 16, 40, 66),
    "frame": (61, 90, 128),
    "frame_blue": (92, 159, 212),
    "green": (139, 195, 74),
    "red": (239, 83, 80),
    "blue": (92, 159, 212),
    "cyan": (41, 182, 246),
    "purple": (126, 87, 194),
    "text": (224, 232, 240),
    "dim": (96, 112, 128),
    "nav_bg": (8, 8, 26, 184),
    "nav_border": (42, 42, 80),
}

SAMPLE = {
    "total_rounds": 28,
    "total_min": 420,
    "active_days": 5,
    "avg_min": 84.0,
    "days": [
        ("06-24", 3, 75),
        ("06-25", 5, 125),
        ("06-26", 4, 100),
        ("06-27", 6, 150),
        ("06-28", 4, 100),
        ("06-29", 3, 75),
        ("06-30", 3, 75),
    ],
}


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    key = (size, bold)
    if key in _FONT_CACHE:
        return _FONT_CACHE[key]
    candidates = [
        ("C:/Windows/Fonts/msyhbd.ttc", 0) if bold else ("C:/Windows/Fonts/msyh.ttc", 0),
        ("C:/Windows/Fonts/simhei.ttf", None),
        ("C:/Windows/Fonts/simsun.ttc", None),
    ]
    for path, idx in candidates:
        p = Path(path)
        if p.is_file():
            try:
                font = ImageFont.truetype(str(p), size, index=idx) if idx is not None else ImageFont.truetype(str(p), size)
                _FONT_CACHE[key] = font
                return font
            except OSError:
                continue
    font = ImageFont.load_default()
    _FONT_CACHE[key] = font
    return font


def draw_stars(img: Image.Image) -> None:
    draw = ImageDraw.Draw(img)
    rng = random.Random(7)
    palette = [(126, 184, 232), (90, 138, 170), (144, 202, 249), (79, 195, 247), (255, 255, 255)]
    for _ in range(140):
        sx = rng.randint(0, W - 1)
        sy = rng.randint(0, H - 90)
        c = palette[rng.randint(0, len(palette) - 1)]
        sz = rng.choice([1, 1, 2, 3])
        draw.rectangle((sx, sy, sx + sz - 1, sy + sz - 1), fill=c)


def draw_jagged_frame(
    draw: ImageDraw.ImageDraw,
    x: int,
    y: int,
    w: int,
    h: int,
    frame: tuple[int, int, int],
    inner: tuple[int, int, int] | None = None,
    p: int = 4,
    steps: int = 4,
) -> tuple[int, int, int, int]:
    """Draw stepped jagged panel; return inner content rect."""
    inset = p * steps
    draw.rectangle((x, y, x + w, y + h), fill=frame)
    for s in range(steps):
        dx = (steps - 1 - s) * p
        dy = s * p
        for cx, cy in (
            (x + dx, y + dy),
            (x + w - dx - p, y + dy),
            (x + dx, y + h - dy - p),
            (x + w - dx - p, y + h - dy - p),
        ):
            draw.rectangle((cx, cy, cx + p, cy + p), fill=frame)
    ix, iy = x + inset, y + inset
    iw, ih = w - inset * 2, h - inset * 2
    if inner:
        draw.rectangle((ix, iy, ix + iw, iy + ih), fill=inner)
        for s in range(steps):
            dx = (steps - 1 - s) * p
            dy = s * p
            for cx, cy in (
                (ix + dx, iy + dy),
                (ix + iw - dx - p, iy + dy),
                (ix + dx, iy + ih - dy - p),
                (ix + iw - dx - p, iy + ih - dy - p),
            ):
                draw.rectangle((cx, cy, cx + p, cy + p), fill=inner)
    return ix, iy, iw, ih


def text_center(draw: ImageDraw.ImageDraw, xy: tuple[int, int, int, int], text: str, font, fill) -> None:
    x, y, w, h = xy
    tw = draw.textlength(text, font=font)
    bbox = draw.textbbox((0, 0), text, font=font)
    th = bbox[3] - bbox[1]
    draw.text((x + (w - tw) / 2, y + (h - th) / 2), text, fill=fill, font=font)


def draw_nav(draw: ImageDraw.ImageDraw, active: int = 1) -> None:
    ny = H - 72
    draw.rectangle((0, ny, W, H), fill=(8, 8, 26))
    draw.rectangle((0, ny, W, ny + 2), fill=COL["nav_border"])
    labels = [("🏠", "计时"), ("📊", "统计"), ("📋", "计划")]
    font = load_font(14, bold=True)
    gap = 8
    total_w = len(labels) * 96 + (len(labels) - 1) * gap
    sx = (W - total_w) // 2
    for i, (icon, label) in enumerate(labels):
        bx = sx + i * (96 + gap)
        by = ny + 10
        if i == active:
            draw_jagged_frame(draw, bx, by, 96, 52, COL["green"], (16, 16, 40), p=3, steps=3)
            color = COL["green"]
        else:
            color = COL["dim"]
        text_center(draw, (bx, by + 4, 96, 24), icon, load_font(18), color)
        text_center(draw, (bx, by + 26, 96, 22), label, font, color)


def draw_kpi_tile(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, label: str, value: str, accent: tuple[int, int, int]) -> None:
    ix, iy, iw, ih = draw_jagged_frame(draw, x, y, w, h, COL["frame"], COL["card"])
    text_center(draw, (ix, iy + 8, iw, 20), label, load_font(13), COL["dim"])
    text_center(draw, (ix, iy + ih // 2 - 8, iw, ih // 2), value, load_font(28, bold=True), accent)


def draw_pixel_bars(
    draw: ImageDraw.ImageDraw,
    rect: tuple[int, int, int, int],
    values: list[int],
    labels: list[str],
    color: tuple[int, int, int],
    title: str,
) -> None:
    x, y, w, h = rect
    draw.text((x + 12, y + 8), title, fill=COL["green"], font=load_font(15, bold=True))
    chart_y = y + 36
    chart_h = h - 52
    n = len(values)
    if n == 0:
        return
    max_v = max(values) or 1
    bar_w = max(12, (w - 48) // n - 8)
    gap = max(6, (w - 48 - bar_w * n) // max(1, n - 1))
    cx = x + 24
    for i, v in enumerate(values):
        bh = int((v / max_v) * (chart_h - 28))
        bx = cx + i * (bar_w + gap)
        by = chart_y + chart_h - bh - 18
        for row in range(0, bh, 6):
            rh = min(6, bh - row)
            draw.rectangle((bx, by + bh - row - rh, bx + bar_w, by + bh - row), fill=color)
        text_center(draw, (bx - 4, chart_y + chart_h - 14, bar_w + 8, 14), labels[i], load_font(11), COL["dim"])


def draw_toolbar(draw: ImageDraw.ImageDraw, scheme_label: str) -> None:
    draw.text((24, 16), f"方案 {scheme_label}", fill=COL["cyan"], font=load_font(13))
    ix, iy, iw, ih = draw_jagged_frame(draw, W - 340, 10, 310, 36, COL["frame"], COL["card"], p=3, steps=3)
    text_center(draw, (ix, iy, iw, ih), "2025-06-24  ~  2025-06-30", load_font(13), COL["text"])
    draw_jagged_frame(draw, W - 420, 10, 68, 36, COL["green"], (20, 48, 20), p=3, steps=3)
    text_center(draw, (W - 420, 10, 68, 36), "查询", load_font(13, bold=True), COL["green"])


def scheme_a_dashboard(img: Image.Image) -> None:
    """A: 仪表盘 — 顶部 KPI + 中部像素柱图 + 底部明细表"""
    draw = ImageDraw.Draw(img)
    draw_toolbar(draw, "A · 仪表盘")
    y = 58
    kw = (W - 48 - 36) // 4
    draw_kpi_tile(draw, 24, y, kw, 88, "总轮数", str(SAMPLE["total_rounds"]), COL["red"])
    draw_kpi_tile(draw, 24 + kw + 12, y, kw, 88, "专注分钟", str(SAMPLE["total_min"]), COL["green"])
    draw_kpi_tile(draw, 24 + (kw + 12) * 2, y, kw, 88, "活跃天数", str(SAMPLE["active_days"]), COL["blue"])
    draw_kpi_tile(draw, 24 + (kw + 12) * 3, y, kw, 88, "日均分钟", f"{SAMPLE['avg_min']:.0f}", COL["cyan"])

    ix, iy, iw, ih = draw_jagged_frame(draw, 24, y + 100, W - 48, 260, COL["frame"], COL["card"])
    draw_pixel_bars(
        draw,
        (ix, iy, iw, ih),
        [d[1] for d in SAMPLE["days"]],
        [d[0] for d in SAMPLE["days"]],
        COL["blue"],
        "◆ 每日专注轮数 ◆",
    )

    tx, ty, tw, th = draw_jagged_frame(draw, 24, y + 372, W - 48, 200, COL["frame"], COL["card"])
    draw.text((tx + 12, ty + 8), "◆ 每日明细 ◆", fill=COL["green"], font=load_font(15, bold=True))
    headers = ["日期", "轮数", "专注", "休息", "进度"]
    col_w = [100, 80, 100, 100, tw - 400]
    hy = ty + 36
    hx = tx + 12
    for i, h in enumerate(headers):
        draw.text((hx, hy), h, fill=COL["dim"], font=load_font(12))
        hx += col_w[i]
    draw.line((tx + 8, hy + 20, tx + tw - 8, hy + 20), fill=COL["frame"], width=2)
    for ri, (d, r, m) in enumerate(SAMPLE["days"][:4]):
        ry = hy + 28 + ri * 32
        vals = [d, str(r), f"{m}m", f"{m // 5}m", ""]
        hx = tx + 12
        for i, v in enumerate(vals):
            if i == 4:
                bw = col_w[4] - 20
                pct = min(100, int(m / 200 * 100))
                draw.rectangle((hx, ry + 6, hx + bw, ry + 18), outline=COL["frame"], width=2)
                draw.rectangle((hx + 2, ry + 8, hx + 2 + int(bw * pct / 100) - 4, ry + 16), fill=COL["red"])
            else:
                draw.text((hx, ry), v, fill=COL["text"], font=load_font(12))
            hx += col_w[i]
    draw_nav(draw, 1)


def scheme_b_split(img: Image.Image) -> None:
    """B: 三栏 — 对齐计时页左中右布局"""
    draw = ImageDraw.Draw(img)
    draw_toolbar(draw, "B · 三栏对称")

    y, mh = 58, H - 150
    lw, rw = 220, 240
    cw = W - lw - rw - 72
    lx, cx, rx = 24, 24 + lw + 12, W - rw - 24

    # left
    ix, iy, iw, ih = draw_jagged_frame(draw, lx, y, lw, mh, COL["frame"], COL["card"])
    draw.text((ix + 12, iy + 10), "本周概览", fill=COL["green"], font=load_font(15, bold=True))
    draw.text((ix + 12, iy + 40), "完成轮数", fill=COL["dim"], font=load_font(12))
    draw.text((ix + 12, iy + 58), str(SAMPLE["total_rounds"]), fill=COL["red"], font=load_font(36, bold=True))
    # pixel dots
    dy = iy + 110
    for row in range(2):
        for col in range(5):
            filled = row * 5 + col < SAMPLE["total_rounds"] % 10 + 8
            c = COL["green"] if filled else (48, 48, 72)
            draw.rectangle((ix + 16 + col * 36, dy + row * 20, ix + 28 + col * 36, dy + 12 + row * 20), fill=c)
    draw.text((ix + 12, iy + 170), "连续打卡", fill=COL["dim"], font=load_font(12))
    draw.text((ix + 12, iy + 188), "5 天", fill=COL["cyan"], font=load_font(22, bold=True))
    draw.text((ix + 12, iy + 230), "活跃天数", fill=COL["dim"], font=load_font(12))
    draw.text((ix + 12, iy + 248), str(SAMPLE["active_days"]), fill=COL["blue"], font=load_font(28, bold=True))

    # center chart
    ix, iy, iw, ih = draw_jagged_frame(draw, cx, y, cw, mh, COL["frame_blue"], COL["card"])
    draw_pixel_bars(draw, (ix, iy, iw, ih), [d[2] for d in SAMPLE["days"]], [d[0] for d in SAMPLE["days"]], COL["red"], "◆ 专注时长趋势 ◆")

    # right gauges
    ix, iy, iw, ih = draw_jagged_frame(draw, rx, y, rw, mh // 2 - 6, COL["frame"], COL["card"])
    draw.text((ix + 12, iy + 10), "日均目标", fill=COL["green"], font=load_font(14, bold=True))
    # pixel ring
    cxr, cyr, radius = ix + iw // 2, iy + ih // 2 + 8, 56
    for a in range(0, 360, 12):
        rad = math.radians(a - 90)
        px = cxr + int(radius * math.cos(rad))
        py = cyr + int(radius * math.sin(rad))
        c = COL["green"] if a < 252 else COL["frame"]
        draw.rectangle((px - 3, py - 3, px + 3, py + 3), fill=c)
    text_center(draw, (ix, iy + ih - 44, iw, 30), "84%", load_font(22, bold=True), COL["green"])

    ix2, iy2, iw2, ih2 = draw_jagged_frame(draw, rx, y + mh // 2 + 6, rw, mh // 2 - 6, COL["frame"], COL["card"])
    draw.text((ix2 + 12, iy2 + 10), "周期目标", fill=COL["green"], font=load_font(14, bold=True))
    cxr = ix2 + iw2 // 2
    cyr = iy2 + ih2 // 2 + 8
    for a in range(0, 360, 12):
        rad = math.radians(a - 90)
        px = cxr + int(radius * math.cos(rad))
        py = cyr + int(radius * math.sin(rad))
        c = COL["blue"] if a < 200 else COL["frame"]
        draw.rectangle((px - 3, py - 3, px + 3, py + 3), fill=c)
    text_center(draw, (ix2, iy2 + ih2 - 44, iw2, 30), "67%", load_font(22, bold=True), COL["blue"])
    draw_nav(draw, 1)


def scheme_c_hero(img: Image.Image) -> None:
    """C: 热力像素 — 中央大数字 + 像素热力格"""
    draw = ImageDraw.Draw(img)
    draw_toolbar(draw, "C · 热力像素")

    y = 58
    ix, iy, iw, ih = draw_jagged_frame(draw, 24, y, W - 48, 120, COL["frame_blue"], COL["card"])
    draw.text((ix + 20, iy + 16), "本周专注", fill=COL["dim"], font=load_font(14))
    draw.text((ix + 20, iy + 38), f"{SAMPLE['total_rounds']} 轮", fill=COL["red"], font=load_font(42, bold=True))
    draw.text((ix + 20, iy + 82), f"共 {SAMPLE['total_min']} 分钟", fill=COL["green"], font=load_font(16))

    # mini KPIs inline
    for i, (lbl, val, ac) in enumerate([("活跃", "5天", COL["cyan"]), ("日均", "84m", COL["blue"]), ("休息", "105m", COL["green"])]):
        bx = ix + iw - 320 + i * 108
        draw_jagged_frame(draw, bx, iy + 20, 96, 72, COL["frame"], (12, 12, 32), p=3, steps=3)
        text_center(draw, (bx, iy + 26, 96, 18), lbl, load_font(11), COL["dim"])
        text_center(draw, (bx, iy + 44, 96, 36), val, load_font(18, bold=True), ac)

    # heatmap
    hx, hy, hw, hh = draw_jagged_frame(draw, 24, y + 136, W - 48, 300, COL["frame"], COL["card"])
    draw.text((hx + 12, hy + 10), "◆ 七日像素热力 ◆", fill=COL["green"], font=load_font(15, bold=True))
    cell = 28
    gx = hx + (hw - 7 * (cell + 8)) // 2
    gy = hy + 48
    for i, (d, r, m) in enumerate(SAMPLE["days"]):
        x0 = gx + i * (cell + 8)
        blocks = max(1, r)
        for b in range(blocks):
            by = gy + (8 - b) * (cell + 4)
            shade = COL["red"] if b % 2 == 0 else (200, 70, 68)
            draw.rectangle((x0, by, x0 + cell, by + cell), fill=shade)
        text_center(draw, (x0 - 4, gy + 9 * (cell + 4), cell + 8, 20), d, load_font(11), COL["dim"])

    # streak bar
    sx, sy, sw, sh = draw_jagged_frame(draw, 24, y + 448, W - 48, 124, COL["frame"], COL["card"])
    draw.text((sx + 12, sy + 10), "◆ 连续专注 streak ◆", fill=COL["green"], font=load_font(15, bold=True))
    for i in range(7):
        bx = sx + 20 + i * ((sw - 40) // 7)
        filled = i < 5
        draw_jagged_frame(
            draw, bx, sy + 40, (sw - 40) // 7 - 8, 56,
            COL["green"] if filled else COL["frame"],
            (24, 56, 24) if filled else COL["card"],
            p=3, steps=3,
        )
        if filled:
            text_center(draw, (bx, sy + 40, (sw - 40) // 7 - 8, 56), "✓", load_font(20, bold=True), COL["green"])
    draw_nav(draw, 1)


def scheme_d_cards(img: Image.Image) -> None:
    """D: 日卡片流 — 横向滚动式每日卡片"""
    draw = ImageDraw.Draw(img)
    draw_toolbar(draw, "D · 日卡片流")

    y = 58
    # summary strip
    strip_w = (W - 48 - 24) // 3
    for i, (lbl, val, ac) in enumerate([
        ("总轮数", str(SAMPLE["total_rounds"]), COL["red"]),
        ("总专注", f"{SAMPLE['total_min']}m", COL["green"]),
        ("活跃天", str(SAMPLE["active_days"]), COL["blue"]),
    ]):
        draw_kpi_tile(draw, 24 + i * (strip_w + 12), y, strip_w, 72, lbl, val, ac)

    draw.text((24, y + 88), "◆ 每日记录（横向滑动）◆", fill=COL["green"], font=load_font(15, bold=True))

    card_w, card_h = 168, 280
    gap = 14
    cy = y + 118
    for i, (d, r, m) in enumerate(SAMPLE["days"]):
        cx = 24 + i * (card_w + gap)
        if cx + card_w > W - 24:
            break
        ix, iy, iw, ih = draw_jagged_frame(
            draw, cx, cy, card_w, card_h,
            COL["frame_blue"] if i == len(SAMPLE["days"]) - 1 else COL["frame"],
            COL["card"],
        )
        text_center(draw, (ix, iy + 12, iw, 24), d, load_font(14, bold=True), COL["cyan"])
        text_center(draw, (ix, iy + 48, iw, 48), str(r), load_font(40, bold=True), COL["red"])
        text_center(draw, (ix, iy + 96, iw, 20), "轮", load_font(13), COL["dim"])
        text_center(draw, (ix, iy + 124, iw, 24), f"{m} 分钟", load_font(16, bold=True), COL["green"])
        # mini bar
        bx, by, bw, bh = ix + 20, iy + 168, iw - 40, 80
        draw.rectangle((bx, by, bx + bw, by + bh), outline=COL["frame"], width=2)
        seg_h = 8
        segs = max(1, r * 2)
        for s in range(segs):
            row = s % 8
            col = s // 8
            sx = bx + 4 + col * 14
            sy2 = by + bh - 8 - row * 10
            if sy2 < by + 4:
                break
            draw.rectangle((sx, sy2, sx + 10, sy2 + seg_h), fill=COL["blue"])
        if i == len(SAMPLE["days"]) - 1:
            draw.text((ix + 8, iy + ih - 28), "今日 ▶", fill=COL["green"], font=load_font(12, bold=True))

    # scroll hint
    draw.polygon([(W - 48, cy + card_h // 2), (W - 28, cy + card_h // 2 - 16), (W - 28, cy + card_h // 2 + 16)], fill=COL["green"])
    draw_nav(draw, 1)


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    schemes = [
        ("stats_scheme_a_dashboard.png", scheme_a_dashboard, "仪表盘：KPI + 柱图 + 明细表"),
        ("stats_scheme_b_split.png", scheme_b_split, "三栏对称：对齐计时页左中右"),
        ("stats_scheme_c_hero.png", scheme_c_hero, "热力像素：大数字 + 像素热力格"),
        ("stats_scheme_d_cards.png", scheme_d_cards, "日卡片流：横向每日卡片"),
    ]
    for fname, fn, desc in schemes:
        img = Image.new("RGB", (W, H), COL["bg"])
        draw_stars(img)
        fn(img)
        path = OUT_DIR / fname
        img.save(path, "PNG")
        print(f"Wrote {path} — {desc}")


if __name__ == "__main__":
    main()
