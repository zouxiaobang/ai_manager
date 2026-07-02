#!/usr/bin/env python3
"""Generate mobile factory management mockups in Scheme-A doodle style (4 layout variants).

Aligned with PC FactoryPanel: stats (production/customer/carton/enabled/disabled),
search, type filter, factory list cards, add action.
"""

from __future__ import annotations

import math
import subprocess
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "preview" / "mobile-factory"
ASSETS = ROOT / "public" / "mobile-home" / "scheme-a"

W, H = 860, 1864
PAD = 28
CONTENT_W = W - PAD * 2
SAFE_TOP = 52
TAB_H = 96
TAB_Y = H - TAB_H - 20

DOODLE_RECT_PATH = (
    "M13.8 9.1 C7.2 7.5 3.8 11.9 4.9 17.8 L3.5 80.2 C2.6 88.5 8.9 94.8 17.2 93.1 "
    "L81.5 95.8 C89.8 97.2 96.1 90.8 94.8 82.9 L95.9 19.1 C97.2 11.2 90.5 4.9 "
    "82.1 6.8 L13.8 9.1 Z"
)

COL = {
    "cream": (250, 248, 245),
    "white": (255, 255, 255),
    "blue": (37, 99, 235),
    "red": (230, 57, 70),
    "yellow": (251, 191, 36),
    "green": (34, 197, 94),
    "green_dark": (22, 163, 74),
    "orange": (249, 115, 22),
    "purple": (139, 92, 246),
    "grey": (148, 163, 184),
    "ink": (30, 41, 59),
    "text_sec": (71, 85, 105),
    "text_dim": (148, 163, 184),
    "green_bg": (240, 253, 244),
    "blue_bg": (239, 246, 255),
    "yellow_bg": (255, 247, 237),
    "purple_bg": (245, 243, 255),
    "orange_bg": (255, 251, 235),
    "grey_bg": (248, 250, 252),
}

STAT_CARDS = [
    ("#f97316", COL["orange_bg"], "icon-factory.svg", "12", "生产工厂", True),
    ("#3b82f6", COL["blue_bg"], "icon-shop.svg", "8", "客户", True),
    ("#8b5cf6", COL["purple_bg"], "icon-box.svg", "5", "纸箱厂", True),
    ("#22c55e", COL["green_bg"], "icon-shield.svg", "22", "启用", False),
    ("#94a3b8", COL["grey_bg"], "icon-todos.svg", "3", "停用", False),
]

FACTORIES = [
    ("深圳光明生产厂", "生产", COL["orange"], "张工 · 138****1234", "深圳市光明区...", "ENABLED"),
    ("东莞纸箱厂 A", "纸箱厂", COL["purple"], "李经理 · 139****5678", "东莞市厚街镇...", "ENABLED"),
    ("广州客户包装", "客户", COL["blue"], "王总 · 136****9012", "广州市白云区...", "ENABLED"),
    ("惠州停产工厂", "生产", COL["orange"], "赵工 · 137****3456", "惠州市惠城区...", "DISABLED"),
]

TYPE_FILTERS = ["全部", "生产", "客户", "纸箱厂"]

_FONT_CACHE: dict[tuple[int, bool], ImageFont.FreeTypeFont | ImageFont.ImageFont] = {}
_SVG_CACHE: dict[tuple[str, int], Image.Image] = {}


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


def svg_asset(name: str, size: int) -> Image.Image:
    key = (name, size)
    if key in _SVG_CACHE:
        return _SVG_CACHE[key].copy()
    path = ASSETS / name
    if not path.is_file():
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        _SVG_CACHE[key] = img
        return img.copy()
    with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tmp:
        out = Path(tmp.name)
    try:
        subprocess.run(
            [
                "npx",
                "--yes",
                "@resvg/resvg-js-cli",
                str(path),
                str(out),
                "--fit-width",
                str(size),
            ],
            check=True,
            capture_output=True,
            cwd=ROOT,
        )
        img = Image.open(out).convert("RGBA")
        if img.height != size:
            img = img.resize((size, size), Image.Resampling.LANCZOS)
    except (subprocess.CalledProcessError, OSError):
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    finally:
        out.unlink(missing_ok=True)
    _SVG_CACHE[key] = img
    return img.copy()


def render_doodle_frame(w: int, h: int, color: str, stroke: int = 3) -> Image.Image:
    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="{w}" height="{h}">
  <path d="{DOODLE_RECT_PATH}" fill="white" stroke="{color}" stroke-width="{stroke}"
        stroke-linecap="round" stroke-linejoin="round" vector-effect="non-scaling-stroke"/>
</svg>"""
    with tempfile.NamedTemporaryFile(suffix=".svg", delete=False, mode="w", encoding="utf-8") as svg_f:
        svg_f.write(svg)
        svg_path = Path(svg_f.name)
    with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as png_f:
        png_path = Path(png_f.name)
    try:
        subprocess.run(
            [
                "npx",
                "--yes",
                "@resvg/resvg-js-cli",
                str(svg_path),
                str(png_path),
                "--fit-width",
                str(w),
            ],
            check=True,
            capture_output=True,
            cwd=ROOT,
        )
        frame = Image.open(png_path).convert("RGBA")
        if frame.height != h:
            frame = frame.resize((w, h), Image.Resampling.LANCZOS)
    except (subprocess.CalledProcessError, OSError):
        frame = Image.new("RGBA", (w, h), (255, 255, 255, 255))
        draw = ImageDraw.Draw(frame)
        draw.rounded_rectangle((2, 2, w - 2, h - 2), 14, outline=hex_rgb(color), width=stroke)
    finally:
        svg_path.unlink(missing_ok=True)
        png_path.unlink(missing_ok=True)
    return frame


def hex_rgb(h: str) -> tuple[int, int, int]:
    h = h.lstrip("#")
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)


def paste_center(img: Image.Image, overlay: Image.Image, x: int, y: int) -> None:
    img.paste(overlay, (x - overlay.width // 2, y - overlay.height // 2), overlay)


def paste_topleft(img: Image.Image, overlay: Image.Image, x: int, y: int) -> None:
    img.paste(overlay, (x, y), overlay)


def draw_doodle_card(
    base: Image.Image,
    x: int,
    y: int,
    w: int,
    h: int,
    color: str,
    bg: tuple[int, int, int] | None = None,
) -> None:
    if bg:
        bg_img = Image.new("RGBA", (w, h), (*bg, 255))
        base.paste(bg_img, (x, y))
    frame = render_doodle_frame(w, h, color)
    base.paste(frame, (x, y), frame)


def draw_status_bar(img: Image.Image) -> None:
    draw = ImageDraw.Draw(img)
    draw.text((PAD + 8, SAFE_TOP - 8), "9:41", fill=COL["ink"], font=load_font(22))


def draw_hero_header(img: Image.Image, title: str, subtitle: str, accent: str = "#8b5cf6") -> int:
    y = SAFE_TOP + 28
    draw = ImageDraw.Draw(img)
    draw.text((PAD, y), title, fill=hex_rgb(accent), font=load_font(42))
    draw.text((PAD, y + 50), subtitle, fill=COL["text_sec"], font=load_font(18))

    bear = svg_asset("mascot-bear.svg", 100)
    paste_topleft(img, bear, W - PAD - 110, y - 6)

    star = svg_asset("deco-star-yellow.svg", 26)
    paste_topleft(img, star, W - PAD - 44, y + 6)
    paste_topleft(img, svg_asset("deco-star-blue.svg", 20), PAD + 200, y + 10)

    squiggle = svg_asset("deco-squiggle-blue.svg", 52)
    paste_topleft(img, squiggle, W - PAD - 66, y + 82)
    return y + 96


def draw_search_bar(img: Image.Image, y: int, placeholder: str, accent: str = "#8b5cf6") -> int:
    h = 52
    draw_doodle_card(img, PAD, y, CONTENT_W, h, accent)
    icon = svg_asset("icon-search.svg", 26)
    paste_topleft(img, icon, PAD + 16, y + 13)
    draw = ImageDraw.Draw(img)
    draw.text((PAD + 52, y + 14), placeholder, fill=COL["text_dim"], font=load_font(20))
    return y + h + 14


def draw_section_head(img: Image.Image, y: int, title: str, icon: str = "deco-star-yellow.svg", accent: str = "#8b5cf6") -> int:
    star = svg_asset(icon, 24)
    paste_topleft(img, star, PAD, y)
    draw = ImageDraw.Draw(img)
    draw.text((PAD + 32, y + 2), title, fill=hex_rgb(accent), font=load_font(22))
    if icon == "deco-star-yellow.svg":
        sq = svg_asset("deco-squiggle-blue.svg", 52)
        paste_topleft(img, sq, W - PAD - 56, y + 4)
    return y + 36


def draw_type_filter_chips(img: Image.Image, y: int, active_idx: int = 0, accent: str = "#8b5cf6") -> int:
    gap = 8
    draw = ImageDraw.Draw(img)
    x = PAD
    for i, label in enumerate(TYPE_FILTERS):
        tw = len(label) * 18 + 28
        active = i == active_idx
        if active:
            draw.rounded_rectangle((x, y, x + tw, y + 40), 20, fill=hex_rgb(accent))
            draw.text((x + tw // 2, y + 20), label, fill=COL["white"], font=load_font(17, True), anchor="mm")
        else:
            draw_doodle_card(img, x, y, tw, 40, "#cbd5e1", COL["white"])
            draw.text((x + tw // 2, y + 20), label, fill=COL["text_sec"], font=load_font(17), anchor="mm")
        x += tw + gap
    return y + 52


def draw_stat_mini_grid(img: Image.Image, y: int, cols: int = 3) -> int:
    """5 stat cards: 3 on row 1, 2 on row 2."""
    gap = 10
    cw3 = (CONTENT_W - gap * 2) // 3
    cw2 = (CONTENT_W - gap) // 2
    ch = 88
    draw = ImageDraw.Draw(img)
    for i, (border, bg, icon_name, val, label, _) in enumerate(STAT_CARDS):
        if i < 3:
            col = i
            row = 0
            cw = cw3
            x = PAD + col * (cw3 + gap)
            cy = y + row * (ch + gap)
        else:
            col = i - 3
            row = 1
            cw = cw2
            x = PAD + col * (cw2 + gap)
            cy = y + ch + gap
        draw_doodle_card(img, x, cy, cw, ch, border, bg)
        ic = svg_asset(icon_name, 30)
        paste_topleft(img, ic, x + 10, cy + 12)
        draw.text((x + 48, cy + 14), val, fill=COL["ink"], font=load_font(24, True))
        draw.text((x + 48, cy + 48), label, fill=COL["text_sec"], font=load_font(16))
        if i == 0:
            draw.rounded_rectangle((x + cw - 28, cy + 6, x + cw - 6, cy + 24), 6, fill=COL["yellow"])
            draw.text((x + cw - 17, cy + 8), "●", fill=COL["ink"], font=load_font(12), anchor="mm")
    return y + 2 * (ch + gap) + 6


def draw_stat_scroll_row(img: Image.Image, y: int) -> int:
    y = draw_section_head(img, y, "工厂统计")
    ch = 100
    draw = ImageDraw.Draw(img)
    x = PAD
    for border, bg, icon_name, val, label, _ in STAT_CARDS[:3]:
        cw = 148
        draw_doodle_card(img, x, y, cw, ch, border, bg)
        ic = svg_asset(icon_name, 34)
        paste_topleft(img, ic, x + 14, y + 14)
        draw.text((x + 14, y + 54), label, fill=COL["text_sec"], font=load_font(16))
        draw.text((x + cw - 14, y + 18), val, fill=COL["ink"], font=load_font(28, True), anchor="rt")
        x += cw + 12
    draw.text((W - PAD - 8, y + 44), "›", fill=COL["text_dim"], font=load_font(32))
    return y + ch + 14


def draw_factory_card(
    img: Image.Image,
    x: int,
    y: int,
    w: int,
    name: str,
    type_label: str,
    type_color: tuple[int, int, int],
    contact: str,
    address: str,
    status: str,
) -> int:
    h = 118
    draw_doodle_card(img, x, y, w, h, "#cbd5e1", COL["white"])
    draw = ImageDraw.Draw(img)
    draw.text((x + 16, y + 14), name, fill=COL["ink"], font=load_font(20, True))
    tag_w = len(type_label) * 16 + 20
    draw.rounded_rectangle((x + 16, y + 44, x + 16 + tag_w, y + 66), 8, fill=(*type_color, 30) if len(type_color) == 3 else type_color)
    draw.rounded_rectangle((x + 16, y + 44, x + 16 + tag_w, y + 66), 8, outline=type_color, width=2)
    draw.text((x + 26, y + 48), type_label, fill=type_color, font=load_font(14, True))
    draw.text((x + 16, y + 74), contact, fill=COL["text_sec"], font=load_font(15))
    draw.text((x + 16, y + 96), address, fill=COL["text_dim"], font=load_font(14))
    status_label = "启用" if status == "ENABLED" else "停用"
    status_color = COL["green_dark"] if status == "ENABLED" else COL["grey"]
    draw.text((x + w - 16, y + 14), status_label, fill=status_color, font=load_font(15, True), anchor="rt")
    edit_x = x + w - 72
    draw.rounded_rectangle((edit_x, y + 78, x + w - 16, y + 106), 10, outline=COL["blue"], width=2)
    draw.text((edit_x + 28, y + 84), "编辑", fill=COL["blue"], font=load_font(14, True), anchor="mm")
    return y + h + 12


def draw_factory_list(img: Image.Image, y: int, count: int = 3) -> int:
    y = draw_section_head(img, y, "工厂列表", "deco-star-blue.svg")
    clip = svg_asset("deco-paperclip.svg", 32)
    paste_topleft(img, clip, W - PAD - 40, y - 28)
    for factory in FACTORIES[:count]:
        y = draw_factory_card(img, PAD, y, CONTENT_W, *factory)
    return y + 4


def draw_add_fab(img: Image.Image, accent: str = "#8b5cf6") -> None:
    draw = ImageDraw.Draw(img)
    cx = W - PAD - 36
    cy = TAB_Y - 56
    draw.ellipse((cx - 32, cy - 32, cx + 32, cy + 32), fill=hex_rgb(accent))
    draw.text((cx, cy), "+", fill=COL["white"], font=load_font(40, True), anchor="mm")
    draw.text((cx, cy + 44), "新建", fill=hex_rgb(accent), font=load_font(14, True), anchor="mm")


def draw_add_button_bar(img: Image.Image, y: int, accent: str = "#8b5cf6") -> int:
    h = 52
    draw_doodle_card(img, PAD, y, CONTENT_W, h, accent)
    draw = ImageDraw.Draw(img)
    draw.text((W // 2, y + h // 2), "+  新建工厂", fill=hex_rgb(accent), font=load_font(22, True), anchor="mm")
    return y + h + 12


def draw_blue_tabbar(img: Image.Image) -> None:
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle((PAD - 4, TAB_Y, W - PAD + 4, H - 12), 20, fill=COL["blue"])
    tabs = [
        ("tab-home.svg", "首页", False),
        ("tab-notebook.svg", "笔记", False),
        ("tab-todos.svg", "待办", False),
        ("tab-more.svg", "更多", True),
    ]
    slot = CONTENT_W // 4
    for i, (icon, label, active) in enumerate(tabs):
        cx = PAD + slot * i + slot // 2
        ic = svg_asset(icon, 30)
        paste_center(img, ic, cx, TAB_Y + 32)
        color = COL["yellow"] if active else (255, 255, 255, 180)
        draw.text((cx, TAB_Y + 58), label, fill=color, font=load_font(17, active), anchor="mm")


def draw_summary_pill(img: Image.Image, y: int, text: str) -> int:
    draw = ImageDraw.Draw(img)
    tw = len(text) * 14 + 36
    x = (W - tw) // 2
    draw.rounded_rectangle((x, y, x + tw, y + 36), 18, fill=COL["yellow"], outline=(245, 158, 11), width=2)
    draw.text((W // 2, y + 18), text, fill=(120, 53, 15), font=load_font(17, True), anchor="mm")
    return y + 48


def draw_type_big_cards(img: Image.Image, y: int) -> int:
    y = draw_section_head(img, y, "按类型筛选")
    gap = 10
    cw = (CONTENT_W - gap * 2) // 3
    ch = 108
    types = [
        ("#f97316", COL["orange_bg"], "icon-factory.svg", "12", "生产"),
        ("#3b82f6", COL["blue_bg"], "icon-shop.svg", "8", "客户"),
        ("#8b5cf6", COL["purple_bg"], "icon-box.svg", "5", "纸箱厂"),
    ]
    draw = ImageDraw.Draw(img)
    for i, (border, bg, icon, val, label) in enumerate(types):
        x = PAD + i * (cw + gap)
        draw_doodle_card(img, x, y, cw, ch, border, bg)
        ic = svg_asset(icon, 32)
        paste_topleft(img, ic, x + cw // 2 - 16, y + 14)
        draw.text((x + cw // 2, y + 54), label, fill=COL["ink"], font=load_font(17, True), anchor="mm")
        draw.text((x + cw // 2, y + 78), val, fill=hex_rgb(border), font=load_font(22, True), anchor="mm")
        if i == 0:
            draw.rounded_rectangle((x + cw - 22, y + 8, x + cw - 6, y + 24), 6, fill=COL["yellow"])
            draw.text((x + cw - 14, y + 10), "●", fill=COL["ink"], font=load_font(10), anchor="mm")
    return y + ch + 14


def draw_status_stamp_row(img: Image.Image, y: int) -> int:
    y = draw_section_head(img, y, "启用状态")
    card_h = 68
    draw_doodle_card(img, PAD, y, CONTENT_W, card_h, "#22c55e", COL["green_bg"])
    draw = ImageDraw.Draw(img)
    labels = [("22", "启用", True), ("3", "停用", False)]
    sx = PAD + 80
    for val, label, on in labels:
        fill = COL["green_bg"] if on else COL["grey_bg"]
        outline = COL["green"] if on else COL["grey"]
        draw.ellipse((sx, y + 12, sx + 40, y + 52), fill=fill, outline=outline, width=2)
        draw.text((sx + 20, y + 22), val, fill=COL["ink"], font=load_font(18, True), anchor="mm")
        draw.text((sx + 20, y + 58), label, fill=COL["text_sec"], font=load_font(15), anchor="mm")
        sx += 280
    return y + card_h + 14


def draw_wave(img: Image.Image, y: int) -> int:
    draw = ImageDraw.Draw(img)
    x0 = PAD
    x1 = W - PAD
    pts = []
    for x in range(x0, x1, 8):
        dy = int(6 * math.sin((x - x0) / 18))
        pts.append((x, y + dy))
    if len(pts) > 1:
        draw.line(pts, fill=COL["purple"], width=3)
    return y + 16


def new_canvas() -> Image.Image:
    img = Image.new("RGBA", (W, H), (*COL["cream"], 255))
    draw_status_bar(img)
    return img


def scheme_a_doodle(img: Image.Image) -> None:
    """方案 A · 涂鸦活力风 — 头图 + 统计格 + 筛选 + 列表"""
    y = draw_hero_header(img, "工厂管理", "管理生产工厂与客户信息")
    y = draw_search_bar(img, y, "搜索工厂名称 / 联系人")
    y = draw_section_head(img, y, "数据概览")
    y = draw_stat_mini_grid(img, y)
    y = draw_type_filter_chips(img, y, active_idx=0)
    draw_factory_list(img, y, count=3)
    draw_add_fab(img)
    draw_blue_tabbar(img)


def scheme_b_poster(img: Image.Image) -> None:
    """方案 B · 工厂海报风 — 大卡片头图 + 汇总胶囊"""
    y = SAFE_TOP + 28
    card_h = 210
    draw_doodle_card(img, PAD, y, CONTENT_W, card_h, "#8b5cf6")
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle((W - PAD - 72, y + 12, W - PAD - 12, y + 36), 8, fill=COL["green"])
    draw.text((W - PAD - 42, y + 16), "25 家", fill=COL["white"], font=load_font(14, True), anchor="mm")
    draw.text((PAD + 16, y + 20), "工厂管理", fill=COL["purple"], font=load_font(38))
    draw.text((PAD + 16, y + 64), "生产 · 客户 · 纸箱厂一站式管理", fill=COL["text_sec"], font=load_font(18))
    factory_ic = svg_asset("icon-factory.svg", 96)
    frame = render_doodle_frame(120, 120, "#8b5cf6", 2)
    icon_area = Image.new("RGBA", (120, 120), (0, 0, 0, 0))
    icon_area.paste(frame, (0, 0), frame)
    icon_area.paste(factory_ic, (12, 12), factory_ic)
    paste_topleft(img, icon_area, W // 2 - 60, y + 72)
    star = svg_asset("deco-star-yellow.svg", 28)
    paste_topleft(img, star, PAD + 8, y + 16)
    y += card_h + 8
    y = draw_summary_pill(img, y, "生产 12 · 客户 8 · 纸箱 5")
    y = draw_search_bar(img, y, "搜索工厂名称 / 联系人")
    y = draw_type_filter_chips(img, y, active_idx=1)
    y = draw_factory_list(img, y, count=3)
    draw_add_fab(img)
    draw_blue_tabbar(img)


def scheme_c_wave(img: Image.Image) -> None:
    """方案 C · 横滑统计风 — 搜索优先 + 横滑统计 + 列表"""
    y = SAFE_TOP + 20
    y = draw_search_bar(img, y, "搜索工厂名称 / 联系人")
    y = draw_type_filter_chips(img, y, active_idx=0)
    y = draw_stat_scroll_row(img, y)
    framed_h = 88
    inner_y = y
    draw_doodle_card(img, PAD, inner_y, CONTENT_W, framed_h, "#8b5cf6", COL["white"])
    draw = ImageDraw.Draw(img)
    draw.text((PAD + 20, inner_y + 18), "启用 22", fill=COL["green_dark"], font=load_font(22, True))
    draw.text((PAD + 20, inner_y + 50), "停用 3", fill=COL["grey"], font=load_font(18))
    draw.text((W - PAD - 20, inner_y + 32), "查看全部 ›", fill=COL["purple"], font=load_font(17, True), anchor="rm")
    y += framed_h + 8
    y = draw_wave(img, y)
    draw_factory_list(img, y, count=4)
    draw_add_button_bar(img, TAB_Y - 80)
    draw_blue_tabbar(img)


def scheme_d_type_hub(img: Image.Image) -> None:
    """方案 D · 类型中心风 — 三格类型筛选 + 状态印章 + 紧凑列表"""
    y = SAFE_TOP + 24
    card_h = 100
    draw_doodle_card(img, PAD, y, CONTENT_W, card_h, "#8b5cf6")
    draw = ImageDraw.Draw(img)
    draw.text((W // 2, y + 24), "工厂管理", fill=COL["purple"], font=load_font(32), anchor="mm")
    draw.text((W // 2, y + 58), "管理生产工厂与客户信息", fill=COL["text_sec"], font=load_font(17), anchor="mm")
    y += card_h + 12
    y = draw_type_big_cards(img, y)
    y = draw_status_stamp_row(img, y)
    y = draw_search_bar(img, y, "搜索工厂名称 / 联系人")
    y = draw_section_head(img, y, "工厂列表")
    for factory in FACTORIES:
        y = draw_factory_card(img, PAD, y, CONTENT_W, *factory)
    draw_add_fab(img)
    draw_blue_tabbar(img)


SCHEMES = [
    ("doodle-scheme-a", "方案 A · 涂鸦活力风", scheme_a_doodle),
    ("doodle-scheme-b", "方案 B · 工厂海报风", scheme_b_poster),
    ("doodle-scheme-c", "方案 C · 横滑统计风", scheme_c_wave),
    ("doodle-scheme-d", "方案 D · 类型中心风", scheme_d_type_hub),
]


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for slug, label, fn in SCHEMES:
        img = new_canvas()
        fn(img)
        draw = ImageDraw.Draw(img)
        draw.text((W // 2, 10), label, fill=COL["text_dim"], font=load_font(17), anchor="mt")
        out = OUT_DIR / f"mobile-factory-{slug}.png"
        img.convert("RGB").save(out, quality=95)
        print(f"Wrote {out}")


if __name__ == "__main__":
    main()
