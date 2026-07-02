#!/usr/bin/env python3
"""Generate mobile e-commerce mockups in Scheme-A doodle style (4 layout variants)."""

from __future__ import annotations

import math
import subprocess
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "preview" / "mobile-ecommerce"
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
    "grey_bg": (248, 250, 252),
}

EC_MODULES = [
    ("月结统计", "icon-cart.svg", "#f59e0b", "#fff7ed"),
    ("订单中心", "icon-ecommerce.svg", "#3b82f6", "#eff6ff"),
    ("库存中心", "icon-shield.svg", "#22c55e", "#f0fdf4"),
    ("商品中心", "icon-notebook.svg", "#6366f1", "#eef2ff"),
    ("快递管理", "icon-todos.svg", "#f97316", "#fff7ed"),
    ("工厂管理", "icon-pixel-dog.svg", "#8b5cf6", "#f5f3ff"),
    ("店铺管理", "icon-cart.svg", "#10b981", "#ecfdf5"),
    ("纸箱管理", "icon-notebook.svg", "#94a3b8", "#f8fafc"),
]

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


def draw_hero_header(img: Image.Image, title: str, badge: str = "AI Manager") -> int:
    y = SAFE_TOP + 28
    draw = ImageDraw.Draw(img)
    draw.text((PAD, y), title, fill=COL["blue"], font=load_font(42))
    badge_w = len(badge) * 14 + 28
    draw.rounded_rectangle((PAD, y + 52, PAD + badge_w, y + 82), 8, fill=COL["red"])
    draw.text((PAD + 14, y + 58), badge, fill=COL["white"], font=load_font(18, True))

    bear = svg_asset("mascot-bear.svg", 110)
    paste_topleft(img, bear, W - PAD - 120, y - 8)

    star = svg_asset("deco-star-yellow.svg", 28)
    paste_topleft(img, star, W - PAD - 48, y + 4)
    paste_topleft(img, svg_asset("deco-star-blue.svg", 22), PAD + 180, y + 8)

    squiggle = svg_asset("deco-squiggle-blue.svg", 56)
    paste_topleft(img, squiggle, W - PAD - 70, y + 88)
    return y + 100


def draw_search_bar(img: Image.Image, y: int, placeholder: str) -> int:
    h = 52
    draw_doodle_card(img, PAD, y, CONTENT_W, h, "#2563eb")
    icon = svg_asset("icon-search.svg", 26)
    paste_topleft(img, icon, PAD + 16, y + 13)
    draw = ImageDraw.Draw(img)
    draw.text((PAD + 52, y + 14), placeholder, fill=COL["text_dim"], font=load_font(20))
    return y + h + 16


def draw_section_head(img: Image.Image, y: int, title: str, icon: str = "deco-star-yellow.svg") -> int:
    star = svg_asset(icon, 24)
    paste_topleft(img, star, PAD, y)
    draw = ImageDraw.Draw(img)
    draw.text((PAD + 32, y + 2), title, fill=COL["blue"], font=load_font(22))
    if icon == "deco-star-yellow.svg":
        sq = svg_asset("deco-squiggle-blue.svg", 52)
        paste_topleft(img, sq, W - PAD - 56, y + 4)
    return y + 36


def draw_progress_ring(img: Image.Image, cx: int, cy: int, r: int, ratio: float) -> None:
    draw = ImageDraw.Draw(img)
    draw.ellipse((cx - r, cy - r, cx + r, cy + r), outline=(209, 250, 229), width=6)
    if ratio > 0:
        start = -90
        end = start + int(360 * ratio)
        draw.arc((cx - r, cy - r, cx + r, cy + r), start, end, fill=COL["green_dark"], width=6)
    inner = r - 8
    draw.ellipse((cx - inner, cy - inner, cx + inner, cy + inner), fill=COL["green_bg"])


def draw_overview_grid(img: Image.Image, y: int) -> int:
    gap = 12
    cw = (CONTENT_W - gap) // 2
    ch = 96
    cards = [
        ("#22c55e", COL["green_bg"], "ring", "5 单", "待发货", None),
        ("#3b82f6", COL["blue_bg"], "icon-notebook.svg", "¥12.8万", "本月销售额", None),
        ("#f59e0b", COL["yellow_bg"], "icon-cart.svg", "月结", "待导入快递 >", COL["orange"]),
        ("#94a3b8", COL["grey_bg"], "icon-shield.svg", "库存", "预警 1 条", None),
    ]
    for i, (border, bg, icon_kind, val, label, val_color) in enumerate(cards):
        col = i % 2
        row = i // 2
        x = PAD + col * (cw + gap)
        cy = y + row * (ch + gap)
        draw_doodle_card(img, x, cy, cw, ch, border, bg)
        draw = ImageDraw.Draw(img)
        if icon_kind == "ring":
            draw_progress_ring(img, x + 36, cy + ch // 2, 26, 0.33)
            draw.text((x + 68, cy + 22), val, fill=COL["green_dark"], font=load_font(22))
            draw.text((x + 68, cy + 52), label, fill=COL["text_sec"], font=load_font(18))
        else:
            ic = svg_asset(icon_kind, 36)
            paste_topleft(img, ic, x + 14, cy + 28)
            vc = val_color or COL["ink"]
            draw.text((x + 58, cy + 22), val, fill=vc, font=load_font(22))
            draw.text((x + 58, cy + 52), label, fill=COL["text_sec"], font=load_font(17))
    return y + 2 * (ch + gap) + 8


def draw_order_todos(img: Image.Image, y: int) -> int:
    y = draw_section_head(img, y, "待处理订单", "deco-star-blue.svg")
    card_h = 200
    draw_doodle_card(img, PAD, y, CONTENT_W, card_h, "#2563eb")
    clip = svg_asset("deco-paperclip.svg", 36)
    paste_topleft(img, clip, W - PAD - 44, y - 8)
    sq = svg_asset("deco-squiggle-red.svg", 72)
    paste_topleft(img, sq, PAD + CONTENT_W - 90, y + card_h - 36)

    orders = [
        ("#2048 · 旗舰店", "2 件 · ¥368", "今天"),
        ("#2047 · 专营店", "1 件 · ¥129", "昨天"),
        ("SKU-A12 低库存", "可用 8 件", "预警"),
    ]
    oy = y + 18
    draw = ImageDraw.Draw(img)
    for title, meta, tag in orders:
        draw.ellipse((PAD + 16, oy + 6, PAD + 30, oy + 20), outline=COL["orange"], width=2)
        draw.text((PAD + 40, oy), title, fill=COL["ink"], font=load_font(20))
        draw.text((PAD + 40, oy + 26), meta, fill=COL["text_dim"], font=load_font(16))
        draw.text((W - PAD - 16, oy + 4), tag, fill=COL["red"], font=load_font(16, True), anchor="rt")
        oy += 54
    return y + card_h + 16


def draw_module_grid(img: Image.Image, y: int, count: int = 8, cols: int = 2) -> int:
    y = draw_section_head(img, y, "全部功能", "deco-star-blue-outline.svg")
    gap = 12
    mw = (CONTENT_W - gap * (cols - 1)) // cols
    mh = 88
    draw = ImageDraw.Draw(img)
    for i, (name, icon, border, bg) in enumerate(EC_MODULES[:count]):
        col = i % cols
        row = i // cols
        x = PAD + col * (mw + gap)
        cy = y + row * (mh + gap)
        draw_doodle_card(img, x, cy, mw, mh, border, hex_rgb(bg))
        ic = svg_asset(icon, 32)
        paste_topleft(img, ic, x + 12, cy + 14)
        draw.text((x + 52, cy + 16), name, fill=COL["ink"], font=load_font(19))
        squiggle_y = cy + 44
        bc = hex_rgb(border)
        draw.line((x + 52, squiggle_y, x + 52 + len(name) * 12, squiggle_y), fill=bc, width=2)
        if name == "订单中心":
            draw.rounded_rectangle((x + mw - 36, cy + 8, x + mw - 8, cy + 28), 6, fill=COL["yellow"])
            draw.text((x + mw - 22, cy + 10), "5", fill=COL["ink"], font=load_font(14, True), anchor="mm")
    rows = math.ceil(count / cols)
    return y + rows * (mh + gap) + 8


def draw_blue_tabbar(img: Image.Image) -> None:
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle((PAD - 4, TAB_Y, W - PAD + 4, H - 12), 20, fill=COL["blue"])
    tabs = [
        ("tab-home.svg", "首页", True),
        ("tab-notebook.svg", "笔记", False),
        ("tab-todos.svg", "待办", False),
        ("tab-more.svg", "更多", False),
    ]
    slot = CONTENT_W // 4
    for i, (icon, label, active) in enumerate(tabs):
        cx = PAD + slot * i + slot // 2
        ic = svg_asset(icon, 30)
        paste_center(img, ic, cx, TAB_Y + 32)
        color = COL["yellow"] if active else (255, 255, 255, 180)
        draw.text((cx, TAB_Y + 58), label, fill=color, font=load_font(17, active), anchor="mm")


def draw_wave(img: Image.Image, y: int) -> int:
    draw = ImageDraw.Draw(img)
    x0 = PAD
    x1 = W - PAD
    pts = []
    for x in range(x0, x1, 8):
        dy = int(6 * math.sin((x - x0) / 18))
        pts.append((x, y + dy))
    if len(pts) > 1:
        draw.line(pts, fill=COL["blue"], width=3)
    return y + 16


def draw_banner(img: Image.Image, y: int, text: str) -> int:
    h = 44
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle((PAD, y, W - PAD, y + h), 22, fill=COL["blue"])
    draw.text((W // 2, y + h // 2), text, fill=COL["white"], font=load_font(20, True), anchor="mm")
    return y + h + 14


def draw_summary_pill(img: Image.Image, y: int, text: str) -> int:
    draw = ImageDraw.Draw(img)
    tw = len(text) * 14 + 36
    x = (W - tw) // 2
    draw.rounded_rectangle((x, y, x + tw, y + 36), 18, fill=COL["yellow"], outline=(245, 158, 11), width=2)
    draw.text((W // 2, y + 18), text, fill=(120, 53, 15), font=load_font(17, True), anchor="mm")
    return y + 48


def draw_quick_row(img: Image.Image, y: int) -> int:
    h = 100
    draw_doodle_card(img, PAD, y, CONTENT_W, h, "#2563eb")
    items = [
        ("icon-ecommerce.svg", "订单"),
        ("icon-shield.svg", "库存"),
        ("icon-cart.svg", "月结"),
        ("icon-notebook.svg", "商品"),
    ]
    slot = CONTENT_W // 4
    draw = ImageDraw.Draw(img)
    for i, (icon, label) in enumerate(items):
        cx = PAD + slot * i + slot // 2
        ic = svg_asset(icon, 34)
        paste_center(img, ic, cx, y + 36)
        if i < 3:
            draw.line((PAD + slot * (i + 1), y + 16, PAD + slot * (i + 1), y + h - 16), fill=(226, 232, 240), width=2)
        draw.text((cx, y + 72), label, fill=COL["blue"], font=load_font(17, True), anchor="mm")
    return y + h + 14


def draw_stamp_row(img: Image.Image, y: int, done: int = 2) -> int:
    y = draw_section_head(img, y, "月结导入进度")
    card_h = 72
    draw_doodle_card(img, PAD, y, CONTENT_W, card_h, "#f59e0b", COL["yellow_bg"])
    labels = ["订单", "快递", "核对", "锁定"]
    draw = ImageDraw.Draw(img)
    sx = PAD + 24
    for i, label in enumerate(labels):
        fill = COL["green_bg"] if i < done else COL["grey_bg"]
        outline = COL["green"] if i < done else COL["grey"]
        draw.ellipse((sx, y + 16, sx + 32, y + 48), fill=fill, outline=outline, width=2)
        if i < done:
            draw.text((sx + 16, y + 24), "★", fill=COL["green_dark"], font=load_font(16), anchor="mm")
        draw.text((sx + 16, y + 56), label, fill=COL["text_sec"], font=load_font(15), anchor="mm")
        sx += 190
    return y + card_h + 14


def draw_status_pills(img: Image.Image, y: int) -> int:
    y = draw_section_head(img, y, "系统状态")
    pills = [("订单 API", COL["green"]), ("库存同步", COL["green"]), ("月结任务", COL["orange"]), ("快递导入", COL["red"])]
    gap = 10
    pw = (CONTENT_W - gap) // 2
    ph = 44
    draw = ImageDraw.Draw(img)
    for i, (name, dot) in enumerate(pills):
        col = i % 2
        row = i // 2
        x = PAD + col * (pw + gap)
        cy = y + row * (ph + gap)
        draw_doodle_card(img, x, cy, pw, ph, "#cbd5e1")
        draw.ellipse((x + 14, cy + 18, x + 22, cy + 26), fill=dot)
        draw.text((x + 30, cy + 12), name, fill=COL["ink"], font=load_font(17, True))
        status = "正常" if dot == COL["green"] else ("待办" if dot == COL["orange"] else "未开始")
        sc = COL["green_dark"] if dot == COL["green"] else (COL["orange"] if dot == COL["orange"] else COL["red"])
        draw.text((x + pw - 14, cy + 12), status, fill=sc, font=load_font(15, True), anchor="rt")
    return y + 2 * (ph + gap) + 8


def draw_module_scroll(img: Image.Image, y: int) -> int:
    y = draw_section_head(img, y, "快捷入口")
    ch = 132
    draw = ImageDraw.Draw(img)
    x = PAD
    for name, icon, border, bg in EC_MODULES[:6]:
        cw = 148
        draw_doodle_card(img, x, y, cw, ch, border, hex_rgb(bg))
        ic = svg_asset(icon, 36)
        paste_topleft(img, ic, x + 14, y + 16)
        draw.text((x + 14, y + 62), name, fill=COL["ink"], font=load_font(18, True))
        draw.text((x + 14, y + 88), "点击进入", fill=COL["text_dim"], font=load_font(14))
        x += cw + 12
    draw.text((W - PAD - 8, y + 56), "›", fill=COL["text_dim"], font=load_font(32))
    return y + ch + 14


def new_canvas() -> Image.Image:
    img = Image.new("RGBA", (W, H), (*COL["cream"], 255))
    draw_status_bar(img)
    return img


def scheme_a_doodle(img: Image.Image) -> None:
    """方案 A · 涂鸦活力风 — 与首页 Scheme A 同构"""
    y = draw_hero_header(img, "电商工作台")
    y = draw_search_bar(img, y, "搜索订单、SKU、店铺...")
    y = draw_section_head(img, y, "今日概览")
    y = draw_overview_grid(img, y)
    y = draw_order_todos(img, y)
    draw_module_grid(img, y, count=4, cols=2)
    draw_blue_tabbar(img)


def scheme_b_poster(img: Image.Image) -> None:
    """方案 B · 像素狗海报风 — 海报头图 + 横幅概览"""
    y = SAFE_TOP + 28
    card_h = 220
    draw_doodle_card(img, PAD, y, CONTENT_W, card_h, "#2563eb")
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle((W - PAD - 72, y + 12, W - PAD - 12, y + 36), 8, fill=COL["red"])
    draw.text((W - PAD - 42, y + 16), "在线", fill=COL["white"], font=load_font(14, True), anchor="mm")
    draw.text((PAD + 16, y + 20), "电商运营台", fill=COL["blue"], font=load_font(36))
    draw.text((PAD + 16, y + 64), "商品、订单与运营数据一站式管理", fill=COL["text_sec"], font=load_font(18))
    dog = svg_asset("icon-pixel-dog.svg", 100)
    frame = render_doodle_frame(124, 124, "#2563eb", 2)
    dog_area = Image.new("RGBA", (124, 124), (0, 0, 0, 0))
    dog_area.paste(frame, (0, 0), frame)
    dog_area.paste(dog, (12, 12), dog)
    paste_topleft(img, dog_area, W // 2 - 62, y + 78)
    y += card_h + 8
    y = draw_summary_pill(img, y, "待发货 5 · 月结待办 1")
    y = draw_search_bar(img, y, "搜索订单、SKU、店铺...")
    y = draw_banner(img, y, "经营概览")
    overview_y = y
    draw_doodle_card(img, PAD, overview_y, CONTENT_W, 220, "#93c5fd", COL["white"])
    draw_overview_grid(img, overview_y + 12)
    y = overview_y + 220 + 12
    y = draw_order_todos(img, y)
    draw_module_grid(img, y, count=8, cols=2)
    draw_blue_tabbar(img)


def scheme_c_wave(img: Image.Image) -> None:
    """方案 C · 波浪横滑风 — 搜索优先 + 横滑模块"""
    y = SAFE_TOP + 20
    y = draw_search_bar(img, y, "搜索订单、SKU、店铺...")
    y = draw_section_head(img, y, "经营概览")
    framed_h = 230
    draw_doodle_card(img, PAD, y, CONTENT_W, framed_h, "#2563eb", COL["white"])
    inner_y = y + 12
    draw_overview_grid(img, inner_y)
    y += framed_h + 8
    y = draw_wave(img, y)
    y = draw_order_todos(img, y)
    y = draw_module_scroll(img, y)
    draw_status_pills(img, y)
    draw_blue_tabbar(img)


def scheme_d_quick(img: Image.Image) -> None:
    """方案 D · 快捷四格风 — 居中海报 + 四格快捷 + 月结印章"""
    y = SAFE_TOP + 24
    card_h = 130
    draw_doodle_card(img, PAD, y, CONTENT_W, card_h, "#2563eb")
    draw = ImageDraw.Draw(img)
    draw.text((W // 2, y + 28), "电商运营台", fill=COL["blue"], font=load_font(34), anchor="mm")
    draw.text((W // 2, y + 68), "多平台运营 · 移动版", fill=COL["text_sec"], font=load_font(18), anchor="mm")
    tw = 120
    draw.rounded_rectangle((W // 2 - tw // 2, y + 88, W // 2 + tw // 2, y + 116), 8, fill=COL["yellow"], outline=(245, 158, 11), width=2)
    draw.text((W // 2, y + 96), "2026 版", fill=(120, 53, 15), font=load_font(15, True), anchor="mm")
    y += card_h + 12
    y = draw_quick_row(img, y)
    y = draw_stamp_row(img, y, done=1)
    y = draw_search_bar(img, y, "搜索订单、SKU、店铺...")
    y = draw_section_head(img, y, "经营概览")
    overview_y = y
    draw_doodle_card(img, PAD, overview_y, CONTENT_W, 220, "#2563eb", COL["white"])
    draw_overview_grid(img, overview_y + 12)
    y = overview_y + 220 + 12
    draw_module_grid(img, y, count=6, cols=2)
    draw_blue_tabbar(img)


SCHEMES = [
    ("doodle-scheme-a", "方案 A · 涂鸦活力风", scheme_a_doodle),
    ("doodle-scheme-b", "方案 B · 像素狗海报风", scheme_b_poster),
    ("doodle-scheme-c", "方案 C · 波浪横滑风", scheme_c_wave),
    ("doodle-scheme-d", "方案 D · 快捷四格风", scheme_d_quick),
]


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for slug, label, fn in SCHEMES:
        img = new_canvas()
        fn(img)
        draw = ImageDraw.Draw(img)
        draw.text((W // 2, 10), label, fill=COL["text_dim"], font=load_font(17), anchor="mt")
        out = OUT_DIR / f"mobile-ecommerce-{slug}.png"
        img.convert("RGB").save(out, quality=95)
        print(f"Wrote {out}")


if __name__ == "__main__":
    main()
