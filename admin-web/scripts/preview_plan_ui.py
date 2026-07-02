#!/usr/bin/env python3
"""Generate pixel-style Pomodoro plan page design mockups (4 schemes)."""

from __future__ import annotations

import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "preview" / "plan"
W, H = 1280, 720

_FONT_CACHE: dict[tuple[int, bool], ImageFont.FreeTypeFont | ImageFont.ImageFont] = {}

COL = {
    "bg": (8, 8, 26),
    "card": (16, 16, 40),
    "frame": (61, 90, 128),
    "frame_blue": (92, 159, 212),
    "green": (139, 195, 74),
    "red": (239, 83, 80),
    "blue": (92, 159, 212),
    "cyan": (41, 182, 246),
    "purple": (126, 87, 194),
    "text": (224, 232, 240),
    "dim": (96, 112, 128),
    "nav_border": (42, 42, 80),
}

PLANS = [
    {
        "title": "默认专注计划",
        "work": 25,
        "short": 5,
        "long": 30,
        "interval": 4,
        "goal_r": 8,
        "goal_m": 200,
        "default": True,
    },
    {
        "title": "深度学习",
        "work": 50,
        "short": 10,
        "long": 20,
        "interval": 3,
        "goal_r": 4,
        "goal_m": 200,
        "default": False,
    },
    {
        "title": "晚间轻量",
        "work": 15,
        "short": 3,
        "long": 10,
        "interval": 4,
        "goal_r": 6,
        "goal_m": 90,
        "default": False,
    },
]


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    key = (size, bold)
    if key in _FONT_CACHE:
        return _FONT_CACHE[key]
    candidates = [
        ("C:/Windows/Fonts/msyhbd.ttc", 0) if bold else ("C:/Windows/Fonts/msyh.ttc", 0),
        ("C:/Windows/Fonts/simhei.ttf", None),
    ]
    for path, idx in candidates:
        p = Path(path)
        if p.is_file():
            try:
                font = (
                    ImageFont.truetype(str(p), size, index=idx)
                    if idx is not None
                    else ImageFont.truetype(str(p), size)
                )
                _FONT_CACHE[key] = font
                return font
            except OSError:
                continue
    font = ImageFont.load_default()
    _FONT_CACHE[key] = font
    return font


def draw_stars(img: Image.Image) -> None:
    draw = ImageDraw.Draw(img)
    rng = random.Random(11)
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
    ix, iy, iw, ih = x + inset, y + inset, w - inset * 2, h - inset * 2
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


def draw_nav(draw: ImageDraw.ImageDraw) -> None:
    ny = H - 72
    draw.rectangle((0, ny, W, H), fill=COL["bg"])
    draw.rectangle((0, ny, W, ny + 2), fill=COL["nav_border"])
    labels = [("🏠", "计时"), ("📊", "统计"), ("📋", "计划")]
    font = load_font(14, bold=True)
    gap = 8
    total_w = len(labels) * 96 + (len(labels) - 1) * gap
    sx = (W - total_w) // 2
    for i, (icon, label) in enumerate(labels):
        bx = sx + i * (96 + gap)
        by = ny + 10
        if i == 2:
            draw_jagged_frame(draw, bx, by, 96, 52, COL["green"], COL["card"], p=3, steps=3)
            color = COL["green"]
        else:
            color = COL["dim"]
        text_center(draw, (bx, by + 4, 96, 24), icon, load_font(18), color)
        text_center(draw, (bx, by + 26, 96, 22), label, font, color)


def draw_toolbar(draw: ImageDraw.ImageDraw, scheme_label: str) -> None:
    draw.text((24, 16), f"方案 {scheme_label}", fill=COL["cyan"], font=load_font(13))
    draw_jagged_frame(draw, 24, 48, 108, 36, COL["green"], (20, 48, 20), p=3, steps=3)
    text_center(draw, (24, 48, 108, 36), "+ 新建计划", load_font(13, bold=True), COL["green"])
    draw_jagged_frame(draw, 140, 48, 72, 36, COL["frame"], COL["card"], p=3, steps=3)
    text_center(draw, (140, 48, 72, 36), "刷新", load_font(13), COL["text"])


def draw_durations(draw: ImageDraw.ImageDraw, x: int, y: int, plan: dict) -> None:
    parts = [
        (str(plan["work"]), COL["red"]),
        ("+", COL["dim"]),
        (str(plan["short"]), COL["green"]),
        ("+", COL["dim"]),
        (str(plan["long"]), COL["cyan"]),
    ]
    cx = x
    for txt, color in parts:
        draw.text((cx, y), txt, fill=color, font=load_font(20, bold=True))
        cx += draw.textlength(txt, font=load_font(20, bold=True)) + 8


def draw_default_badge(draw: ImageDraw.ImageDraw, x: int, y: int) -> None:
    draw_jagged_frame(draw, x, y, 52, 22, COL["green"], (24, 56, 24), p=2, steps=2)
    text_center(draw, (x, y, 52, 22), "默认", load_font(11, bold=True), COL["green"])


def draw_action_icons(draw: ImageDraw.ImageDraw, x: int, y: int) -> None:
    for i, (label, color) in enumerate([("✎", COL["blue"]), ("🗑", COL["red"])]):
        bx = x + i * 36
        draw_jagged_frame(draw, bx, y, 30, 28, COL["frame"], COL["card"], p=2, steps=2)
        text_center(draw, (bx, y, 30, 28), label, load_font(14), color)


def draw_plan_card(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, plan: dict, highlight: bool = False) -> None:
    frame = COL["frame_blue"] if highlight or plan["default"] else COL["frame"]
    ix, iy, iw, ih = draw_jagged_frame(draw, x, y, w, h, frame, COL["card"])
    draw.text((ix + 12, iy + 10), plan["title"], fill=COL["text"], font=load_font(16, bold=True))
    if plan["default"]:
        draw_default_badge(draw, ix + iw - 64, iy + 8)
    draw_durations(draw, ix + 12, iy + 38, plan)
    goal = f"每日目标  {plan['goal_r']} 轮 / {plan['goal_m']} 分钟"
    draw.text((ix + 12, iy + 68), goal, fill=COL["dim"], font=load_font(12))
    interval = f"每 {plan['interval']} 轮长休息"
    draw.text((ix + 12, iy + 88), interval, fill=COL["dim"], font=load_font(11))
    draw_action_icons(draw, ix + iw - 80, iy + ih - 38)


def scheme_a_cards(img: Image.Image) -> None:
    draw = ImageDraw.Draw(img)
    draw_toolbar(draw, "A · 计划卡片")
    y = 100
    for plan in PLANS:
        draw_plan_card(draw, 24, y, W - 48, 118, plan)
        y += 128
    draw_nav(draw)


def scheme_b_master_detail(img: Image.Image) -> None:
    draw = ImageDraw.Draw(img)
    draw_toolbar(draw, "B · 主从分栏")
    lx, ly, lw, lh = 24, 96, 260, H - 180
    rx, ry, rw, rh = lx + lw + 16, ly, W - 48 - lw - 16, lh
    draw_jagged_frame(draw, lx, ly, lw, lh, COL["frame"], COL["card"])
    draw.text((lx + 16, ly + 12), "◆ 计划列表 ◆", fill=COL["green"], font=load_font(13, bold=True))
    item_y = ly + 40
    for i, plan in enumerate(PLANS):
        ih = 52
        frame = COL["green"] if i == 0 else COL["frame"]
        inner = (24, 48, 24) if i == 0 else COL["card"]
        ix, iy, iw, item_h = draw_jagged_frame(draw, lx + 10, item_y, lw - 20, ih, frame, inner, p=3, steps=3)
        draw.text((ix + 8, iy + 6), plan["title"], fill=COL["text"] if i else COL["green"], font=load_font(13, bold=True))
        draw.text((ix + 8, iy + 26), f"{plan['work']}+{plan['short']}+{plan['long']}", fill=COL["dim"], font=load_font(11))
        if plan["default"]:
            draw_default_badge(draw, ix + iw - 58, iy + 6)
        item_y += ih + 8
    sel = PLANS[0]
    ix, iy, iw, ih = draw_jagged_frame(draw, rx, ry, rw, rh, COL["frame_blue"], COL["card"])
    draw.text((ix + 16, iy + 14), sel["title"], fill=COL["text"], font=load_font(22, bold=True))
    draw_default_badge(draw, ix + 16, iy + 48)
    draw_durations(draw, ix + 16, iy + 82, sel)
    rows = [
        ("专注时长", f"{sel['work']} 分钟", COL["red"]),
        ("短休息", f"{sel['short']} 分钟", COL["green"]),
        ("长休息", f"{sel['long']} 分钟", COL["cyan"]),
        ("长休息间隔", f"每 {sel['interval']} 轮", COL["text"]),
        ("每日目标轮次", f"{sel['goal_r']} 轮", COL["green"]),
        ("每日目标专注", f"{sel['goal_m']} 分钟", COL["blue"]),
    ]
    ry2 = iy + 120
    for label, val, ac in rows:
        draw_jagged_frame(draw, ix + 12, ry2, iw - 24, 40, COL["frame"], (12, 12, 32), p=2, steps=2)
        draw.text((ix + 24, ry2 + 12), label, fill=COL["dim"], font=load_font(12))
        draw.text((ix + iw - 24 - draw.textlength(val, font=load_font(16, bold=True)), ry2 + 10), val, fill=ac, font=load_font(16, bold=True))
        ry2 += 48
    draw_jagged_frame(draw, ix + 12, ry2 + 8, 120, 40, COL["green"], (20, 48, 20), p=3, steps=3)
    text_center(draw, (ix + 12, ry2 + 8, 120, 40), "设为默认", load_font(13, bold=True), COL["green"])
    draw_jagged_frame(draw, ix + 140, ry2 + 8, 88, 40, COL["frame"], COL["card"], p=3, steps=3)
    text_center(draw, (ix + 140, ry2 + 8, 88, 40), "编辑", load_font(13), COL["blue"])
    draw_nav(draw)


def scheme_c_table(img: Image.Image) -> None:
    draw = ImageDraw.Draw(img)
    draw_toolbar(draw, "C · 像素表格")
    tx, ty, tw, th = draw_jagged_frame(draw, 24, 92, W - 48, H - 168, COL["frame"], COL["card"])
    draw.text((tx + 12, ty + 8), "◆ 计划管理 ◆", fill=COL["green"], font=load_font(14, bold=True))
    headers = ["计划名称", "专注", "短休", "长休", "每日目标", "默认", "操作"]
    col_w = [200, 72, 72, 72, 180, 64, 80]
    hy = ty + 36
    hx = tx + 12
    for i, h in enumerate(headers):
        draw.text((hx, hy), h, fill=COL["dim"], font=load_font(11, bold=True))
        hx += col_w[i]
    draw.line((tx + 8, hy + 18, tx + tw - 8, hy + 18), fill=COL["frame"], width=2)
    row_y = hy + 28
    for plan in PLANS:
        hx = tx + 12
        vals = [
            (plan["title"], COL["text"]),
            (f"{plan['work']}m", COL["red"]),
            (f"{plan['short']}m", COL["green"]),
            (f"{plan['long']}m", COL["cyan"]),
            (f"{plan['goal_r']}轮/{plan['goal_m']}m", COL["dim"]),
            ("默认" if plan["default"] else "—", COL["green"] if plan["default"] else COL["dim"]),
            ("✎ 🗑", COL["blue"]),
        ]
        for i, (v, c) in enumerate(vals):
            f = load_font(15, bold=True) if i in (1, 2, 3) else load_font(12)
            draw.text((hx, row_y), v, fill=c, font=f)
            hx += col_w[i]
        draw.line((tx + 8, row_y + 28, tx + tw - 8, row_y + 28), fill=COL["frame"], width=1)
        row_y += 36
    draw_nav(draw)


def scheme_d_tiles(img: Image.Image) -> None:
    draw = ImageDraw.Draw(img)
    draw_toolbar(draw, "D · 磁贴网格")
    gap = 16
    tw = (W - 48 - gap) // 2
    th = 200
    positions = [(24, 96), (24 + tw + gap, 96), (24, 96 + th + gap)]
    for pos, plan in zip(positions, PLANS):
        x, y = pos
        frame = COL["frame_blue"] if plan["default"] else COL["frame"]
        ix, iy, iw, ih = draw_jagged_frame(draw, x, y, tw, th, frame, COL["card"])
        text_center(draw, (ix, iy + 12, iw, 28), plan["title"], load_font(16, bold=True), COL["text"])
        if plan["default"]:
            draw_default_badge(draw, ix + iw // 2 - 26, iy + 42)
        cy = iy + 72
        for label, val, color in [
            ("专注", str(plan["work"]), COL["red"]),
            ("短休", str(plan["short"]), COL["green"]),
            ("长休", str(plan["long"]), COL["cyan"]),
        ]:
            bx = ix + 16 + (["专注", "短休", "长休"].index(label)) * ((iw - 32) // 3)
            bw = (iw - 48) // 3
            draw_jagged_frame(draw, bx, cy, bw, 56, COL["frame"], (12, 12, 32), p=2, steps=2)
            text_center(draw, (bx, cy + 6, bw, 16), label, load_font(10), COL["dim"])
            text_center(draw, (bx, cy + 22, bw, 28), val, load_font(22, bold=True), color)
        draw.text((ix + 12, iy + ih - 32), f"目标 {plan['goal_r']}轮 · {plan['goal_m']}分", fill=COL["dim"], font=load_font(11))
        draw_action_icons(draw, ix + iw - 76, iy + ih - 36)
    draw_nav(draw)


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    schemes = [
        ("plan_scheme_a_cards.png", scheme_a_cards, "计划卡片：纵向卡片列表"),
        ("plan_scheme_b_master.png", scheme_b_master_detail, "主从分栏：左列表右详情"),
        ("plan_scheme_c_table.png", scheme_c_table, "像素表格：对齐统计页"),
        ("plan_scheme_d_tiles.png", scheme_d_tiles, "磁贴网格：双列大卡片"),
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
