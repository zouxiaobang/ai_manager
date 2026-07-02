#!/usr/bin/env python3
"""Generate mobile e-commerce design mockups (iPhone 15 Pro Max, 4 schemes)."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "preview" / "mobile-ecommerce"

W, H = 860, 1864
SAFE_TOP = 118
SAFE_BOTTOM = 98
TAB_H = 88
HEADER_H = 108

COL = {
    "bg": (245, 247, 250),
    "white": (255, 255, 255),
    "navy": (30, 58, 95),
    "blue": (37, 99, 235),
    "blue_light": (219, 234, 254),
    "green": (16, 185, 129),
    "green_light": (209, 250, 229),
    "orange": (245, 158, 11),
    "orange_light": (254, 243, 199),
    "purple": (139, 92, 246),
    "red": (239, 68, 68),
    "grey": (107, 114, 128),
    "grey_light": (229, 231, 235),
    "text": (17, 24, 39),
    "text_sec": (75, 85, 99),
    "text_dim": (156, 163, 175),
    "border": (226, 232, 240),
}

_FONT_CACHE: dict[tuple[int, bool], ImageFont.FreeTypeFont | ImageFont.ImageFont] = {}

EC_MODULES = [
    (COL["orange_light"], "月结统计", "¥12.8万"),
    (COL["blue_light"], "订单中心", "5 待发货"),
    (COL["green_light"], "库存中心", "1 预警"),
    (COL["blue_light"], "商品中心", "128 SKU"),
    (COL["purple"], "快递管理", "待导入"),
    (COL["orange_light"], "工厂管理", "3 家"),
    (COL["green_light"], "店铺管理", "4 店铺"),
    (COL["grey_light"], "纸箱管理", "24 规格"),
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


def rounded_rect(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int, int, int],
    radius: int,
    fill: tuple[int, ...],
    outline: tuple[int, ...] | None = None,
    width: int = 1,
) -> None:
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def draw_phone_frame(img: Image.Image) -> ImageDraw.ImageDraw:
    draw = ImageDraw.Draw(img, "RGBA")
    rounded_rect(draw, (20, 16, W - 20, H - 16), 56, (20, 20, 22))
    rounded_rect(draw, (28, 24, W - 28, H - 24), 48, COL["bg"])
    rounded_rect(draw, (W // 2 - 62, 36, W // 2 + 62, 68), 18, (12, 12, 14))
    return draw


def draw_status_bar(draw: ImageDraw.ImageDraw) -> None:
    draw.text((52, 52), "9:41", fill=COL["text"], font=load_font(22))
    bx = W - 72
    rounded_rect(draw, (bx, 54, bx + 28, 68), 4, COL["text"])
    rounded_rect(draw, (bx + 32, 56, bx + 48, 66), 2, COL["text"])


def draw_ec_header(draw: ImageDraw.ImageDraw, title: str, subtitle: str) -> None:
    y = SAFE_TOP
    rounded_rect(draw, (40, y, 72, y + 32), 8, COL["orange_light"])
    draw.text((56, y + 6), "电商", fill=COL["orange"], font=load_font(18, True), anchor="mm")
    draw.text((40, y + 40), title, fill=COL["navy"], font=load_font(34, True))
    draw.text((40, y + 84), subtitle, fill=COL["text_sec"], font=load_font(22))
    rounded_rect(draw, (W - 100, y + 48, W - 40, y + 96), 12, COL["blue"])
    draw.text((W - 70, y + 64), "刷新", fill=COL["white"], font=load_font(20, True), anchor="mm")


def draw_tabbar(draw: ImageDraw.ImageDraw, active: int = 0) -> None:
    y = H - SAFE_BOTTOM - TAB_H
    rounded_rect(draw, (28, y, W - 28, H - 24), 0, COL["white"], outline=COL["border"])
    tabs = [("首页", False), ("笔记", False), ("待办", False), ("更多", False)]
    slot = (W - 56) // 4
    for i, (label, _) in enumerate(tabs):
        cx = 28 + slot * i + slot // 2
        color = COL["blue"] if i == active else COL["text_dim"]
        rounded_rect(
            draw,
            (cx - 18, y + 14, cx + 18, y + 50),
            8,
            COL["blue_light"] if i == active else COL["grey_light"],
        )
        draw.text((cx, y + 58), label, fill=color, font=load_font(18, i == active), anchor="mm")


def draw_card(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, radius: int = 16) -> None:
    rounded_rect(draw, (x, y, x + w, y + h), radius, COL["white"], outline=COL["border"], width=1)


def draw_progress_bar(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, ratio: float, color: tuple[int, ...]) -> None:
    h = 10
    rounded_rect(draw, (x, y, x + w, y + h), 5, COL["grey_light"])
    fw = max(8, int(w * ratio))
    rounded_rect(draw, (x, y, x + fw, y + h), 5, color)


def draw_metric_row(draw: ImageDraw.ImageDraw, x: int, y: int, icon_color: tuple, label: str, value: str, sub: str = "") -> int:
    rounded_rect(draw, (x, y, x + 36, y + 36), 8, icon_color)
    draw.text((x + 48, y + 2), label, fill=COL["text_sec"], font=load_font(20))
    draw.text((x + 48, y + 24), value, fill=COL["navy"], font=load_font(24, True))
    if sub:
        draw.text((x + 48, y + 52), sub, fill=COL["text_dim"], font=load_font(18))
        return y + 78
    return y + 56


def draw_module_chip(
    draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, color: tuple, name: str, badge: str = ""
) -> None:
    draw_card(draw, x, y, w, h, 12)
    rounded_rect(draw, (x + 12, y + 12, x + 44, y + 44), 8, color)
    draw.text((x + 12, y + 54), name, fill=COL["text"], font=load_font(18, True))
    if badge:
        rounded_rect(draw, (x + w - 58, y + 10, x + w - 8, y + 30), 6, COL["orange_light"])
        draw.text((x + w - 33, y + 12), badge, fill=COL["orange"], font=load_font(14), anchor="mm")


def draw_timeline_item(draw: ImageDraw.ImageDraw, x: int, y: int, time: str, title: str, tag: str, tag_color: tuple) -> int:
    draw.ellipse((x, y + 4, x + 12, y + 16), fill=COL["orange"])
    draw.line((x + 5, y + 18, x + 5, y + 58), fill=COL["border"], width=2)
    draw.text((x + 20, y), time, fill=COL["text_dim"], font=load_font(18))
    draw.text((x + 20, y + 22), title, fill=COL["text"], font=load_font(20))
    tw = len(tag) * 14 + 16
    rounded_rect(draw, (x + 20, y + 46, x + 20 + tw, y + 68), 6, tag_color)
    draw.text((x + 28, y + 50), tag, fill=COL["white"], font=load_font(16))
    return y + 78


def draw_month_filter(draw: ImageDraw.ImageDraw, y: int) -> int:
    rounded_rect(draw, (40, y, 200, y + 44), 10, COL["white"], outline=COL["border"])
    draw.text((56, y + 10), "2026-07 ▾", fill=COL["text"], font=load_font(20))
    rounded_rect(draw, (252, y, W - 40, y + 44), 10, COL["white"], outline=COL["border"])
    draw.text((268, y + 10), "全部店铺 ▾", fill=COL["text_dim"], font=load_font(20))
    return y + 56


def draw_stat_row(draw: ImageDraw.ImageDraw, y: int) -> int:
    stats = [
        ("¥128,560", "本月销售额", COL["orange"], COL["orange_light"]),
        ("32.5%", "毛利率", COL["green"], COL["green_light"]),
        ("¥18,420", "本月净利", COL["blue"], COL["blue_light"]),
        ("待核对", "月结状态", COL["red"], (254, 226, 226)),
    ]
    sw = (W - 80 - 36) // 4
    for i, (val, label, vc, bg) in enumerate(stats):
        x = 40 + i * (sw + 12)
        draw_card(draw, x, y, sw, 108, 12)
        rounded_rect(draw, (x + 10, y + 10, x + 38, y + 38), 6, bg)
        draw.text((x + 10, y + 46), val, fill=vc, font=load_font(20, True))
        draw.text((x + 10, y + 76), label, fill=COL["text_sec"], font=load_font(15))
    return y + 124


def scheme_a_dashboard(img: Image.Image) -> None:
    """方案 A · 数据驾驶舱 — 指标分栏 + 月结进度 + 模块网格"""
    draw = draw_phone_frame(img)
    draw_status_bar(draw)
    draw_ec_header(draw, "电商工作台", "2026年7月 · 全部店铺")
    y = SAFE_TOP + HEADER_H + 8
    y = draw_month_filter(draw, y)
    y = draw_stat_row(draw, y)

    draw_card(draw, 40, y, W - 80, 56, 12)
    rounded_rect(draw, (56, y + 14, 80, y + 38), 6, COL["orange_light"])
    draw.text((92, y + 16), "月结待办：7月快递账单尚未导入", fill=COL["orange"], font=load_font(18, True))

    split_y = y + 72
    left_w = 360
    draw_card(draw, 40, split_y, left_w, 380, 16)
    draw.text((56, split_y + 16), "月结进度", fill=COL["navy"], font=load_font(24, True))
    ly = split_y + 56
    for label, ratio, color in [
        ("订单导入", 1.0, COL["green"]),
        ("快递账单", 0.0, COL["red"]),
        ("月结核对", 0.62, COL["orange"]),
        ("利润确认", 0.0, COL["grey"]),
    ]:
        draw.text((56, ly), label, fill=COL["text_sec"], font=load_font(18))
        draw_progress_bar(draw, 56, ly + 24, left_w - 32, ratio, color)
        draw.text((left_w - 16, ly + 8), f"{int(ratio * 100)}%", fill=color, font=load_font(18, True), anchor="rt")
        ly += 58

    draw.text((56, ly + 8), "库存摘要", fill=COL["navy"], font=load_font(20, True))
    for label, val in [("总 SKU", "128"), ("预警", "1"), ("零库存", "3")]:
        draw.text((56, ly + 40), f"{label} {val}", fill=COL["text"], font=load_font(18))
        ly += 28

    rx = 40 + left_w + 16
    rw = W - 80 - left_w - 16
    draw_card(draw, rx, split_y, rw, 380, 16)
    draw.text((rx + 16, split_y + 16), "待处理", fill=COL["navy"], font=load_font(24, True))
    ty = split_y + 56
    for time, title, tag, tc in [
        ("今天", "订单 #2048 待发货", "订单", COL["blue"]),
        ("昨天", "SKU-A12 低库存", "库存", COL["orange"]),
        ("7/1", "快递账单导入", "月结", COL["red"]),
        ("6/30", "店铺「旗舰店」对账", "店铺", COL["purple"]),
    ]:
        ty = draw_timeline_item(draw, rx + 16, ty, time, title, tag, tc)

    grid_y = split_y + 400
    draw.text((40, grid_y), "功能模块", fill=COL["navy"], font=load_font(24, True))
    gw, gh = 120, 88
    gx0, gy0 = 40, grid_y + 36
    for i, (c, name, badge) in enumerate(EC_MODULES):
        col = i % 3
        row = i // 3
        draw_module_chip(draw, gx0 + col * (gw + 12), gy0 + row * (gh + 12), gw, gh, c, name, badge if "待" in badge else "")

    draw_tabbar(draw, 0)


def scheme_b_cards(img: Image.Image) -> None:
    """方案 B · 卡片瀑布 — 大统计卡 + 横滑入口 + 订单/库存列表"""
    draw = draw_phone_frame(img)
    draw_status_bar(draw)
    draw_ec_header(draw, "电商平台", "商品、订单与运营数据一站式管理")
    y = SAFE_TOP + HEADER_H + 8
    y = draw_month_filter(draw, y)

    hero_h = 140
    draw_card(draw, 40, y, W - 80, hero_h, 16)
    draw.text((60, y + 16), "本月经营概览", fill=COL["navy"], font=load_font(26, True))
    draw.text((60, y + 52), "销售额", fill=COL["text_sec"], font=load_font(18))
    draw.text((60, y + 76), "¥128,560", fill=COL["orange"], font=load_font(36, True))
    draw.text((320, y + 52), "净利", fill=COL["text_sec"], font=load_font(18))
    draw.text((320, y + 76), "¥18,420", fill=COL["green"], font=load_font(30, True))
    draw.text((520, y + 52), "毛利率", fill=COL["text_sec"], font=load_font(18))
    draw.text((520, y + 76), "32.5%", fill=COL["blue"], font=load_font(30, True))
    rounded_rect(draw, (60, y + 118, W - 80, y + 126), 4, COL["orange_light"])
    rounded_rect(draw, (60, y + 118, 60 + int((W - 140) * 0.65), y + 126), 4, COL["orange"])

    y += hero_h + 16
    draw.text((40, y), "快捷入口", fill=COL["navy"], font=load_font(26, True))
    y += 40
    shortcuts = [
        ("订单中心", COL["blue"], "5 单待发货"),
        ("库存中心", COL["green"], "1 条预警"),
        ("月结统计", COL["orange"], "待导入快递"),
        ("商品中心", COL["purple"], "128 SKU"),
        ("快递管理", COL["orange"], "账单导入"),
        ("店铺管理", COL["green"], "4 家店铺"),
    ]
    cw = 200
    for i, (name, color, desc) in enumerate(shortcuts):
        x = 40 + i * (cw + 14)
        draw_card(draw, x, y, cw, 110, 14)
        rounded_rect(draw, (x + 14, y + 14, x + 50, y + 50), 8, color)
        draw.text((x + 14, y + 60), name, fill=COL["text"], font=load_font(20, True))
        draw.text((x + 14, y + 86), desc, fill=COL["text_dim"], font=load_font(16))
    draw.text((W - 56, y + 44), "›", fill=COL["text_dim"], font=load_font(36))

    y += 130
    draw_card(draw, 40, y, W - 80, 240, 16)
    draw.text((60, y + 16), "待发货订单", fill=COL["navy"], font=load_font(24, True))
    draw.text((W - 120, y + 20), "全部 ›", fill=COL["blue"], font=load_font(18))
    orders = [
        ("#2048 · 旗舰店", "2 件 · ¥368", False),
        ("#2047 · 专营店", "1 件 · ¥129", False),
        ("#2046 · 旗舰店", "3 件 · ¥520", False),
    ]
    oy = y + 56
    for title, meta, done in orders:
        rounded_rect(draw, (60, oy + 4, 76, oy + 20), 4, COL["orange_light"])
        draw.text((88, oy), title, fill=COL["text"], font=load_font(20))
        draw.text((88, oy + 28), meta, fill=COL["text_dim"], font=load_font(16))
        rounded_rect(draw, (W - 120, oy + 8, W - 60, oy + 36), 8, COL["blue_light"])
        draw.text((W - 90, oy + 16), "发货", fill=COL["blue"], font=load_font(16, True), anchor="mm")
        oy += 58

    y += 260
    draw_card(draw, 40, y, W - 80, 200, 16)
    draw.text((60, y + 16), "库存预警", fill=COL["navy"], font=load_font(24, True))
    draw.text((W - 140, y + 20), "查看全部 ›", fill=COL["blue"], font=load_font(18))
    alerts = [
        ("SKU-A12 蓝牙耳机", "可用 8 件", COL["orange"]),
        ("SKU-B05 手机壳", "零库存", COL["red"]),
        ("SKU-C18 数据线", "滞销 45 天", COL["grey"]),
    ]
    ay = y + 56
    for name, qty, c in alerts:
        rounded_rect(draw, (60, ay + 4, 76, ay + 20), 4, c)
        draw.text((88, ay), name, fill=COL["text"], font=load_font(18))
        draw.text((88, ay + 26), qty, fill=COL["text_dim"], font=load_font(16))
        ay += 48

    y += 220
    draw_card(draw, 40, y, W - 80, 120, 14)
    draw.text((60, y + 14), "本月导入进度", fill=COL["navy"], font=load_font(22, True))
    steps = [("订单", True), ("快递", False), ("核对", False)]
    sx = 60
    for label, done in steps:
        color = COL["green"] if done else COL["grey_light"]
        draw.ellipse((sx, y + 52, sx + 24, y + 76), fill=color)
        draw.text((sx + 12, y + 86), label, fill=COL["text_sec"], font=load_font(16), anchor="mm")
        if label != "核对":
            draw.line((sx + 28, y + 64, sx + 80, y + 64), fill=COL["border"], width=3)
        sx += 200

    draw_tabbar(draw, 0)


def scheme_c_hub(img: Image.Image) -> None:
    """方案 C · 模块中枢 — 紧凑数据条 + 电商功能宫格"""
    draw = draw_phone_frame(img)
    draw_status_bar(draw)
    draw_ec_header(draw, "电商管理中心", "多平台运营 · 移动版")
    y = SAFE_TOP + HEADER_H + 8

    draw_card(draw, 40, y, W - 80, 72, 12)
    parts = [
        ("销售 ¥12.8万", COL["orange"]),
        ("净利 ¥1.8万", COL["green"]),
        ("待发货 5", COL["blue"]),
        ("预警 1", COL["red"]),
    ]
    px = 56
    for text, c in parts:
        draw.ellipse((px, y + 30, px + 8, y + 38), fill=c)
        draw.text((px + 14, y + 24), text, fill=COL["text"], font=load_font(19))
        px += 180

    y += 92
    rounded_rect(draw, (40, y, W - 40, y + 48), 10, COL["orange_light"], outline=COL["orange"], width=1)
    draw.text((56, y + 12), "⚠ 7月快递账单尚未导入，月结无法完成", fill=COL["orange"], font=load_font(18, True))

    y += 64
    draw.text((40, y), "全部功能", fill=COL["navy"], font=load_font(28, True))
    y += 40

    modules = [
        ("月结统计", COL["orange"], "销售与利润分析"),
        ("订单中心", COL["blue"], "销售订单管理"),
        ("库存中心", COL["green"], "库存与预警"),
        ("商品中心", COL["blue"], "SKU 与定价"),
        ("快递管理", COL["orange"], "运费与对账"),
        ("工厂管理", COL["purple"], "供应商与产能"),
        ("店铺管理", COL["green"], "多平台店铺"),
        ("纸箱管理", COL["grey"], "包装规格"),
    ]
    mw, mh = 186, 130
    for i, (name, color, desc) in enumerate(modules):
        col = i % 2
        row = i // 2
        x = 40 + col * (mw + 20)
        cy = y + row * (mh + 16)
        draw_card(draw, x, cy, mw, mh, 14)
        rounded_rect(draw, (x + 14, cy + 14, x + 54, cy + 54), 10, color)
        if name == "快递管理":
            rounded_rect(draw, (x + mw - 58, cy + 10, x + mw - 10, cy + 32), 6, COL["orange_light"])
            draw.text((x + mw - 34, cy + 14), "待办", fill=COL["orange"], font=load_font(14), anchor="mm")
        if name == "订单中心":
            rounded_rect(draw, (x + mw - 48, cy + 10, x + mw - 10, cy + 32), 6, COL["blue_light"])
            draw.text((x + mw - 29, cy + 14), "5", fill=COL["blue"], font=load_font(14, True), anchor="mm")
        draw.text((x + 14, cy + 64), name, fill=COL["navy"], font=load_font(22, True))
        draw.text((x + 14, cy + 94), desc, fill=COL["text_dim"], font=load_font(16))

    y += 4 * (mh + 16) + 16
    draw_card(draw, 40, y, W - 80, 160, 14)
    draw.text((60, y + 14), "今日焦点", fill=COL["navy"], font=load_font(22, True))
    draw.text((60, y + 50), "★ 导入 7 月快递账单并完成月结", fill=COL["text"], font=load_font(20))
    draw.text((60, y + 82), "○ 处理 5 单待发货订单", fill=COL["text"], font=load_font(20))
    draw.text((60, y + 114), "○ 补货 SKU-A12 蓝牙耳机", fill=COL["text"], font=load_font(20))

    draw_tabbar(draw, 0)


def scheme_d_workbench(img: Image.Image) -> None:
    """方案 D · 运营工作台 — 月结时间线 + 图表条 + 快捷指引"""
    draw = draw_phone_frame(img)
    draw_status_bar(draw)
    draw_ec_header(draw, "电商运营台", "AI Manager · 移动电商")
    y = SAFE_TOP + HEADER_H + 8
    y = draw_month_filter(draw, y)

    draw_card(draw, 40, y, W - 80, 300, 18)
    draw.text((60, y + 16), "7月运营日程", fill=COL["navy"], font=load_font(30, True))

    left_x = 60
    summary = [
        ("💰", "本月销售额", "¥128,560"),
        ("📈", "毛利率", "32.5%"),
        ("💵", "本月净利", "¥18,420"),
        ("📦", "待发货订单", "5 单"),
        ("⚠️", "库存预警", "1 条"),
        ("🏪", "活跃店铺", "4 家"),
    ]
    sy = y + 60
    for icon, label, val in summary:
        draw.text((left_x, sy), icon, fill=COL["text"], font=load_font(20))
        draw.text((left_x + 32, sy), label, fill=COL["text_sec"], font=load_font(18))
        draw.text((left_x + 160, sy), val, fill=COL["navy"], font=load_font(18, True))
        sy += 36

    tx = 420
    draw.text((tx, y + 60), "月结时间线", fill=COL["orange"], font=load_font(20, True))
    times = [
        ("7/1", "订单已全部导入", "完成"),
        ("待办", "导入快递账单", "月结"),
        ("待办", "核对待确认订单", "3单"),
        ("—", "利润确认锁定", "未开始"),
    ]
    tyy = y + 92
    for time, evt, tag in times:
        draw.text((tx, tyy), time, fill=COL["text_dim"], font=load_font(16))
        draw.text((tx + 72, tyy), evt, fill=COL["text"], font=load_font(17))
        tc = COL["green_light"] if tag == "完成" else COL["orange_light"]
        tc_text = COL["green"] if tag == "完成" else COL["orange"]
        rounded_rect(draw, (tx + 72, tyy + 22, tx + 72 + len(tag) * 15 + 12, tyy + 42), 4, tc)
        draw.text((tx + 80, tyy + 24), tag, fill=tc_text, font=load_font(14))
        tyy += 52

    y += 320
    draw_card(draw, 40, y, W - 80, 110, 14)
    draw.text((60, y + 14), "销售构成", fill=COL["navy"], font=load_font(22, True))
    bars = [("销售", 0.85, COL["orange"]), ("成本", 0.55, COL["red"]), ("快递", 0.25, COL["blue"]), ("利润", 0.35, COL["green"])]
    bx = 60
    for label, ratio, color in bars:
        bw = 160
        draw.text((bx, y + 48), label, fill=COL["text_sec"], font=load_font(16))
        bh = int(40 * ratio)
        rounded_rect(draw, (bx, y + 68 + (40 - bh), bx + bw, y + 108), 4, color)
        bx += 176

    y += 130
    draw_card(draw, 40, y, W - 80, 200, 14)
    draw.text((60, y + 14), "运营任务", fill=COL["navy"], font=load_font(22, True))
    alloc = [
        ("订单履约 · 待发货", 0.72, "5 单"),
        ("月结导入 · 快递账单", 0.0, "未开始"),
        ("库存补货 · 低库存 SKU", 0.35, "1 预警"),
    ]
    ay = y + 50
    for label, ratio, sub in alloc:
        draw.text((60, ay), label, fill=COL["text"], font=load_font(18))
        draw.text((W - 100, ay), sub, fill=COL["text_dim"], font=load_font(16), anchor="rt")
        draw_progress_bar(draw, 60, ay + 26, W - 120, ratio, COL["orange"])
        ay += 52

    y += 220
    draw.text((40, y), "快捷指引", fill=COL["navy"], font=load_font(24, True))
    y += 36
    quick = [
        ("导入订单", "订单"),
        ("发货确认", "订单"),
        ("快递对账", "月结"),
        ("补货清单", "库存"),
        ("商品上架", "商品"),
        ("店铺配置", "店铺"),
    ]
    qw, qh = 248, 72
    for i, (title, sub) in enumerate(quick):
        col = i % 2
        row = i // 2
        x = 40 + col * (qw + 20)
        cy = y + row * (qh + 12)
        draw_card(draw, x, cy, qw, qh, 12)
        draw.text((x + 14, cy + 12), title, fill=COL["navy"], font=load_font(20, True))
        draw.text((x + 14, cy + 40), sub, fill=COL["text_dim"], font=load_font(16))

    draw.text((40, H - SAFE_BOTTOM - TAB_H - 30), "电商运营管理台 · 2026 版", fill=COL["text_dim"], font=load_font(16))
    draw_tabbar(draw, 0)


SCHEMES = [
    ("scheme-a-dashboard", "方案 A · 数据驾驶舱", scheme_a_dashboard),
    ("scheme-b-cards", "方案 B · 卡片瀑布", scheme_b_cards),
    ("scheme-c-hub", "方案 C · 模块中枢", scheme_c_hub),
    ("scheme-d-workbench", "方案 D · 运营工作台", scheme_d_workbench),
]


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for slug, label, fn in SCHEMES:
        img = Image.new("RGBA", (W, H), (30, 30, 32, 255))
        fn(img)
        draw = ImageDraw.Draw(img)
        draw.text((W // 2, 8), label, fill=(200, 200, 200), font=load_font(18), anchor="mt")
        out = OUT_DIR / f"mobile-ecommerce-{slug}.png"
        img.convert("RGB").save(out, quality=95)
        print(f"Wrote {out}")


if __name__ == "__main__":
    main()
