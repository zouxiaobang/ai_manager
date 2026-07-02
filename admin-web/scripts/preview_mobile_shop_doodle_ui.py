#!/usr/bin/env python3
"""Generate mobile shop-management mockups aligned with MobileFactoryView poster style.

Mirrors factory UI: poster header, back btn, yellow dashed pill, purple→green accent,
search pill, filter chips, 2-col doodle cards with edit/delete, FAB. Four layout variants.
"""

from __future__ import annotations

import subprocess
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "preview" / "mobile-shop"
ASSETS = ROOT / "public" / "mobile-home" / "scheme-a"

W, H = 860, 1864
PAD = 28
CONTENT_W = W - PAD * 2
SAFE_TOP = 52

ACCENT = "#10b981"
ACCENT_RGB = (16, 185, 129)

DOODLE_RECT_PATH = (
    "M13.8 9.1 C7.2 7.5 3.8 11.9 4.9 17.8 L3.5 80.2 C2.6 88.5 8.9 94.8 17.2 93.1 "
    "L81.5 95.8 C89.8 97.2 96.1 90.8 94.8 82.9 L95.9 19.1 C97.2 11.2 90.5 4.9 "
    "82.1 6.8 L13.8 9.1 Z"
)

COL = {
    "white": (255, 255, 255),
    "ink": (30, 41, 59),
    "text_sec": (71, 85, 105),
    "text_dim": (148, 163, 184),
    "green": (34, 197, 94),
    "green_dark": (22, 163, 74),
    "red": (239, 68, 68),
    "red_dark": (153, 27, 27),
    "orange": (249, 115, 22),
    "yellow_bg": (253, 230, 138),
    "yellow_border": (217, 119, 6),
    "grey_border": (203, 213, 225),
}

PLATFORM_FILTERS = ["全部", "淘宝", "京东", "拼多多", "抖音"]

SHOPS = [
    ("旗舰店", "ENABLED", "淘宝", "#f97316", "12.3%", "广东省"),
    ("品牌专营店", "ENABLED", "京东", "#e1251b", "9.8%", "北京市"),
    ("分销小店", "DISABLED", "拼多多", "#e02e24", "6.2%", "浙江省"),
    ("直播店", "ENABLED", "抖音", "#111827", "10.1%", "上海市"),
    ("优选小店", "ENABLED", "淘宝", "#f97316", "11.0%", "江苏省"),
    ("自营旗舰", "ENABLED", "京东", "#e1251b", "9.2%", "广东省"),
]

_FONT_CACHE: dict[tuple[int, bool], ImageFont.FreeTypeFont | ImageFont.ImageFont] = {}
_SVG_CACHE: dict[tuple[str, int], Image.Image] = {}


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    key = (size, bold)
    if key in _FONT_CACHE:
        return _FONT_CACHE[key]
    for path, idx in [
        ("C:/Windows/Fonts/msyhbd.ttc", 0) if bold else ("C:/Windows/Fonts/msyh.ttc", 0),
        ("C:/Windows/Fonts/simhei.ttf", None),
    ]:
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
            ["npx", "--yes", "@resvg/resvg-js-cli", str(path), str(out), "--fit-width", str(size)],
            check=True, capture_output=True, cwd=ROOT,
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
    with tempfile.NamedTemporaryFile(suffix=".svg", delete=False, mode="w", encoding="utf-8") as f:
        f.write(svg)
        svg_path = Path(f.name)
    with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as f:
        png_path = Path(f.name)
    try:
        subprocess.run(
            ["npx", "--yes", "@resvg/resvg-js-cli", str(svg_path), str(png_path), "--fit-width", str(w)],
            check=True, capture_output=True, cwd=ROOT,
        )
        frame = Image.open(png_path).convert("RGBA")
        if frame.height != h:
            frame = frame.resize((w, h), Image.Resampling.LANCZOS)
    except (subprocess.CalledProcessError, OSError):
        frame = Image.new("RGBA", (w, h), (255, 255, 255, 255))
        ImageDraw.Draw(frame).rounded_rectangle((2, 2, w - 2, h - 2), 14, outline=hex_rgb(color), width=stroke)
    finally:
        svg_path.unlink(missing_ok=True)
        png_path.unlink(missing_ok=True)
    return frame


def hex_rgb(h: str) -> tuple[int, int, int]:
    h = h.lstrip("#")
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)


def paste_topleft(img: Image.Image, overlay: Image.Image, x: int, y: int) -> None:
    img.paste(overlay, (x, y), overlay)


def draw_doodle_card(base: Image.Image, x: int, y: int, w: int, h: int, color: str, bg: tuple[int, int, int] | None = None) -> None:
    if bg:
        base.paste(Image.new("RGBA", (w, h), (*bg, 255)), (x, y))
    frame = render_doodle_frame(w, h, color)
    base.paste(frame, (x, y), frame)


def draw_status_bar(img: Image.Image) -> None:
    ImageDraw.Draw(img).text((PAD + 8, SAFE_TOP - 8), "9:41", fill=COL["ink"], font=load_font(22))


def draw_back_button(img: Image.Image, x: int, y: int) -> None:
    draw = ImageDraw.Draw(img)
    draw.ellipse((x, y, x + 34, y + 34), fill=COL["white"], outline=ACCENT_RGB, width=2)
    draw.text((x + 17, y + 15), "←", fill=ACCENT_RGB, font=load_font(18, True), anchor="mm")


def draw_poster_header(img: Image.Image, y: int, compact: bool = False) -> int:
    """Hand-drawn poster banner — mirrors FactoryPosterHeader layout."""
    poster_h = 280 if compact else int(CONTENT_W * 387 / 608)
    draw_doodle_card(img, PAD, y, CONTENT_W, poster_h, ACCENT, COL["white"])
    draw = ImageDraw.Draw(img)
    inner_x, inner_y = PAD + 16, y + 16

    draw_back_button(img, inner_x, inner_y)

    title_x = PAD + CONTENT_W - 20
    draw.text((title_x, inner_y + 4), "店铺管理", fill=ACCENT_RGB, font=load_font(38 if compact else 44, True), anchor="rt")
    draw.text((title_x, inner_y + 52), "多平台店铺 · 扣点与状态一览", fill=COL["text_sec"], font=load_font(17), anchor="rt")
    draw.line((title_x - 320, inner_y + 78, title_x, inner_y + 78), fill=COL["ink"], width=2)

    cx = PAD + CONTENT_W // 2
    cy = y + poster_h // 2 + (10 if compact else 20)
    shop_ic = svg_asset("icon-shop.svg", 110 if compact else 140)
    paste_topleft(img, shop_ic, cx - shop_ic.width // 2, cy - shop_ic.height // 2 - 10)
    paste_topleft(img, svg_asset("deco-star-yellow.svg", 32), cx - 120, cy - 50)
    paste_topleft(img, svg_asset("deco-star-blue.svg", 24), cx + 90, cy - 40)
    paste_topleft(img, svg_asset("deco-squiggle-blue.svg", 48), cx - 140, cy + 30)

    pill_y = y + poster_h - 52
    pill_x = PAD + 40
    pill_w = CONTENT_W - 80
    draw.rounded_rectangle((pill_x, pill_y, pill_x + pill_w, pill_y + 38), 19, fill=COL["yellow_bg"], outline=COL["yellow_border"], width=2)
    for dx in range(pill_x + 8, pill_x + pill_w - 8, 14):
        draw.line((dx, pill_y, dx + 6, pill_y), fill=COL["yellow_border"], width=2)
        draw.line((dx, pill_y + 38, dx + 6, pill_y + 38), fill=COL["yellow_border"], width=2)
    draw.text((PAD + CONTENT_W // 2, pill_y + 19), "淘宝 3 · 京东 2 · 拼多多 1 · 抖音 2", fill=(120, 53, 15), font=load_font(16, True), anchor="mm")

    badge_x, badge_y = inner_x + 4, inner_y + 44
    draw.rounded_rectangle((badge_x, badge_y, badge_x + 72, badge_y + 30), 6, fill=(220, 252, 231), outline=ACCENT_RGB, width=2)
    draw.text((badge_x + 36, badge_y + 15), "12 家", fill=COL["ink"], font=load_font(15, True), anchor="mm")

    return y + poster_h + 12


def draw_search_bar(img: Image.Image, y: int) -> int:
    h = 52
    draw_doodle_card(img, PAD, y, CONTENT_W, h, ACCENT)
    paste_topleft(img, svg_asset("icon-search.svg", 26), PAD + 16, y + 13)
    ImageDraw.Draw(img).text((PAD + 52, y + 14), "搜索店铺名称", fill=COL["text_dim"], font=load_font(20))
    return y + h + 12


def draw_platform_chips(img: Image.Image, y: int, active: int = 0) -> int:
    gap = 8
    draw = ImageDraw.Draw(img)
    x = PAD
    for i, label in enumerate(PLATFORM_FILTERS):
        tw = len(label) * 18 + 28
        if i == active:
            draw.rounded_rectangle((x, y, x + tw, y + 40), 20, fill=ACCENT_RGB)
            draw.text((x + tw // 2, y + 20), label, fill=COL["white"], font=load_font(17, True), anchor="mm")
        else:
            draw.rounded_rectangle((x, y, x + tw, y + 40), 20, fill=COL["white"], outline=COL["grey_border"], width=2)
            draw.text((x + tw // 2, y + 20), label, fill=COL["text_sec"], font=load_font(17), anchor="mm")
        x += tw + gap
        if x > W - PAD - 50:
            break
    return y + 52


def draw_section_head(img: Image.Image, y: int, title: str, count: int) -> int:
    paste_topleft(img, svg_asset("deco-star-blue.svg", 24), PAD, y)
    draw = ImageDraw.Draw(img)
    draw.text((PAD + 32, y + 2), title, fill=COL["ink"], font=load_font(22, True))
    draw.text((W - PAD, y + 2), str(count), fill=COL["red_dark"], font=load_font(26, True), anchor="rt")
    return y + 36


def draw_shop_card(img: Image.Image, x: int, y: int, w: int, h: int, shop: tuple) -> None:
    name, status, platform, border, fee, region = shop
    draw_doodle_card(img, x, y, w, h, "#cbd5e1", COL["white"])
    draw = ImageDraw.Draw(img)
    cx = x + w // 2
    draw.text((cx, y + 12), name, fill=COL["ink"], font=load_font(17, True), anchor="mm")
    status_label = "正常营业" if status == "ENABLED" else "休息中"
    status_color = COL["green_dark"] if status == "ENABLED" else COL["text_dim"]
    draw.text((cx, y + 34), status_label, fill=status_color, font=load_font(12, True), anchor="mm")
    tag_w = len(platform) * 14 + 18
    tx = cx - tag_w // 2
    bc = hex_rgb(border)
    draw.rounded_rectangle((tx, y + 48, tx + tag_w, y + 66), 6, fill=(*bc, 30), outline=bc, width=2)
    draw.text((cx, y + 51), platform, fill=bc, font=load_font(12, True), anchor="mm")
    draw.text((cx, y + 76), f"扣点 {fee} · {region}", fill=COL["text_sec"], font=load_font(12), anchor="mm")
    draw.text((cx, y + 94), "综合扣点已配置", fill=COL["text_dim"], font=load_font(11), anchor="mm")
  # action row
    btn_y = y + h - 34
    half = (w - 16) // 2
    draw.rounded_rectangle((x + 6, btn_y, x + 6 + half, btn_y + 26), 8, outline=ACCENT_RGB, width=2)
    draw.rounded_rectangle((x + 10 + half, btn_y, x + w - 6, btn_y + 26), 8, outline=(254, 202, 202), width=2)
    draw.text((x + 6 + half // 2, btn_y + 13), "编辑", fill=ACCENT_RGB, font=load_font(13, True), anchor="mm")
    draw.text((x + 10 + half + half // 2, btn_y + 13), "删除", fill=COL["red"], font=load_font(13, True), anchor="mm")


def draw_shop_grid(img: Image.Image, y: int, count: int = 6, cols: int = 2) -> int:
    y = draw_section_head(img, y, "店铺列表", count)
    gap = 10
    cw = (CONTENT_W - gap) // cols
    ch = 148
    for i, shop in enumerate(SHOPS[:count]):
        col, row = i % cols, i // cols
        cx = PAD + col * (cw + gap)
        cy = y + row * (ch + gap)
        draw_shop_card(img, cx, cy, cw, ch, shop)
    rows = (min(count, len(SHOPS)) + cols - 1) // cols
    return y + rows * (ch + gap) + 8


def draw_shop_list_single(img: Image.Image, y: int, count: int = 4) -> int:
    y = draw_section_head(img, y, "店铺列表", count)
    ch = 120
    gap = 10
    for i, shop in enumerate(SHOPS[:count]):
        name, status, platform, border, fee, region = shop
        cy = y + i * (ch + gap)
        draw_doodle_card(img, PAD, cy, CONTENT_W, ch, "#cbd5e1", COL["white"])
        draw = ImageDraw.Draw(img)
        draw.text((PAD + 16, cy + 14), name, fill=COL["ink"], font=load_font(20, True))
        status_label = "正常营业" if status == "ENABLED" else "休息中"
        sc = COL["green_dark"] if status == "ENABLED" else COL["text_dim"]
        draw.text((W - PAD - 16, cy + 14), status_label, fill=sc, font=load_font(14, True), anchor="rt")
        bc = hex_rgb(border)
        tag_w = len(platform) * 14 + 18
        draw.rounded_rectangle((PAD + 16, cy + 42, PAD + 16 + tag_w, cy + 62), 6, fill=(*bc, 30), outline=bc, width=2)
        draw.text((PAD + 25, cy + 45), platform, fill=bc, font=load_font(13, True))
        draw.text((PAD + 16, cy + 72), f"综合扣点 {fee} · 默认收货 {region}", fill=COL["text_sec"], font=load_font(14))
        ex = W - PAD - 150
        draw.rounded_rectangle((ex, cy + 82, ex + 60, cy + 106), 8, outline=ACCENT_RGB, width=2)
        draw.rounded_rectangle((ex + 68, cy + 82, W - PAD - 16, cy + 106), 8, outline=(254, 202, 202), width=2)
        draw.text((ex + 30, cy + 88), "编辑", fill=ACCENT_RGB, font=load_font(13, True), anchor="mm")
        draw.text((ex + 98, cy + 88), "删除", fill=COL["red"], font=load_font(13, True), anchor="mm")
    return y + count * (ch + gap) + 8


def draw_stat_strip(img: Image.Image, y: int) -> int:
    gap = 8
    cw = (CONTENT_W - gap * 3) // 4
    ch = 72
    stats = [("12", "店铺总数"), ("10", "在线"), ("5", "平台"), ("8.5%", "扣点")]
    borders = [ACCENT, "#22c55e", "#3b82f6", "#f59e0b"]
    draw = ImageDraw.Draw(img)
    for i, ((val, label), border) in enumerate(zip(stats, borders)):
        x = PAD + i * (cw + gap)
        draw_doodle_card(img, x, y, cw, ch, border, COL["white"])
        draw.text((x + cw // 2, y + 22), val, fill=COL["ink"], font=load_font(20, True), anchor="mm")
        draw.text((x + cw // 2, y + 48), label, fill=COL["text_sec"], font=load_font(13), anchor="mm")
    return y + ch + 12


def draw_platform_big_row(img: Image.Image, y: int) -> int:
    y = draw_section_head(img, y, "平台分布", 5)
    gap = 8
    cw = (CONTENT_W - gap * 2) // 3
    ch = 88
    items = [("淘宝", "3", "#f97316"), ("京东", "2", "#e1251b"), ("拼多多", "1", "#e02e24")]
    draw = ImageDraw.Draw(img)
    for i, (name, cnt, border) in enumerate(items):
        x = PAD + i * (cw + gap)
        draw_doodle_card(img, x, y, cw, ch, border, COL["white"])
        draw.text((x + cw // 2, y + 24), name, fill=COL["ink"], font=load_font(17, True), anchor="mm")
        draw.text((x + cw // 2, y + 52), f"{cnt} 家", fill=hex_rgb(border), font=load_font(20, True), anchor="mm")
    return y + ch + 12


def draw_fab(img: Image.Image) -> None:
    draw = ImageDraw.Draw(img)
    cx, cy = W - PAD - 28, H - 80
    draw.ellipse((cx - 28, cy - 28, cx + 28, cy + 28), fill=ACCENT_RGB)
    draw.text((cx, cy), "+", fill=COL["white"], font=load_font(36, True), anchor="mm")
    draw.text((cx, cy + 38), "新建", fill=ACCENT_RGB, font=load_font(13, True), anchor="mm")


def new_canvas() -> Image.Image:
    img = Image.new("RGBA", (W, H), (255, 255, 255, 255))
    draw_status_bar(img)
    return img


def scheme_a_poster_grid(img: Image.Image) -> None:
    """方案 A · 海报双列风 — 同工厂管理参考图"""
    y = draw_poster_header(img, SAFE_TOP + 8)
    y = draw_search_bar(img, y)
    y = draw_platform_chips(img, y, active=0)
    draw_shop_grid(img, y, count=6)
    draw_fab(img)


def scheme_b_poster_stats(img: Image.Image) -> None:
    """方案 B · 海报统计风 — 海报 + 四格统计条 + 双列列表"""
    y = draw_poster_header(img, SAFE_TOP + 8, compact=True)
    y = draw_stat_strip(img, y)
    y = draw_search_bar(img, y)
    y = draw_platform_chips(img, y, active=1)
    draw_shop_grid(img, y, count=4)
    draw_fab(img)


def scheme_c_compact_platform(img: Image.Image) -> None:
    """方案 C · 紧凑平台风 — 短海报 + 平台三格 + 双列卡片"""
    y = draw_poster_header(img, SAFE_TOP + 8, compact=True)
    y = draw_search_bar(img, y)
    y = draw_platform_chips(img, y, active=0)
    y = draw_platform_big_row(img, y)
    draw_shop_grid(img, y, count=4)
    draw_fab(img)


def scheme_d_single_column(img: Image.Image) -> None:
    """方案 D · 单列宽卡风 — 完整海报 + 单列详情卡"""
    y = draw_poster_header(img, SAFE_TOP + 8)
    y = draw_search_bar(img, y)
    y = draw_platform_chips(img, y, active=0)
    draw_shop_list_single(img, y, count=4)
    draw_fab(img)


SCHEMES = [
    ("doodle-scheme-a", "方案 A · 海报双列风", scheme_a_poster_grid),
    ("doodle-scheme-b", "方案 B · 海报统计风", scheme_b_poster_stats),
    ("doodle-scheme-c", "方案 C · 紧凑平台风", scheme_c_compact_platform),
    ("doodle-scheme-d", "方案 D · 单列宽卡风", scheme_d_single_column),
]


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for slug, label, fn in SCHEMES:
        img = new_canvas()
        fn(img)
        ImageDraw.Draw(img).text((W // 2, 10), label, fill=COL["text_dim"], font=load_font(16), anchor="mt")
        out = OUT_DIR / f"mobile-shop-{slug}.png"
        img.convert("RGB").save(out, quality=95)
        print(f"Wrote {out}")


if __name__ == "__main__":
    main()
