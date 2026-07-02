#!/usr/bin/env python3
"""Generate mobile homepage design mockups (iPhone 15 Pro Max, 4 schemes)."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "preview" / "mobile-home"

# iPhone 15 Pro Max logical @2x
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
    "shadow": (15, 23, 42, 18),
}

_FONT_CACHE: dict[tuple[int, bool], ImageFont.FreeTypeFont | ImageFont.ImageFont] = {}


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
    # device bezel
    rounded_rect(draw, (20, 16, W - 20, H - 16), 56, (20, 20, 22))
    rounded_rect(draw, (28, 24, W - 28, H - 24), 48, COL["bg"])
    # dynamic island
    rounded_rect(draw, (W // 2 - 62, 36, W // 2 + 62, 68), 18, (12, 12, 14))
    return draw


def draw_status_bar(draw: ImageDraw.ImageDraw) -> None:
    f = load_font(22)
    draw.text((52, 52), "9:41", fill=COL["text"], font=f)
    # signal dots + battery simplified
    bx = W - 72
    rounded_rect(draw, (bx, 54, bx + 28, 68), 4, COL["text"])
    rounded_rect(draw, (bx + 32, 56, bx + 48, 66), 2, COL["text"])


def draw_app_header(draw: ImageDraw.ImageDraw, title: str, subtitle: str) -> None:
    y = SAFE_TOP
    draw.text((40, y), title, fill=COL["navy"], font=load_font(34, True))
    draw.text((40, y + 44), subtitle, fill=COL["text_sec"], font=load_font(22))
    # todo badge button
    rounded_rect(draw, (W - 100, y + 8, W - 40, y + 56), 12, COL["blue_light"])
    draw.text((W - 82, y + 22), "待办", fill=COL["blue"], font=load_font(20, True))
    rounded_rect(draw, (W - 52, y + 4, W - 36, y + 20), 8, COL["red"])
    draw.text((W - 50, y + 2), "3", fill=COL["white"], font=load_font(16, True))


def draw_tabbar(draw: ImageDraw.ImageDraw, active: int = 0) -> None:
    y = H - SAFE_BOTTOM - TAB_H
    rounded_rect(draw, (28, y, W - 28, H - 24), 0, COL["white"], outline=COL["border"])
    tabs = [("首页", True), ("笔记", False), ("待办", False), ("更多", False)]
    slot = (W - 56) // 4
    for i, (label, _) in enumerate(tabs):
        cx = 28 + slot * i + slot // 2
        color = COL["blue"] if i == active else COL["text_dim"]
        rounded_rect(draw, (cx - 18, y + 14, cx + 18, y + 50), 8, COL["blue_light"] if i == active else COL["grey_light"])
        draw.text((cx - 14, y + 58), label, fill=color, font=load_font(18, i == active), anchor="mm")


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


def draw_module_chip(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, color: tuple, name: str, badge: str = "") -> None:
    draw_card(draw, x, y, w, h, 12)
    rounded_rect(draw, (x + 12, y + 12, x + 44, y + 44), 8, color)
    draw.text((x + 12, y + 54), name, fill=COL["text"], font=load_font(18, True))
    if badge:
        rounded_rect(draw, (x + w - 52, y + 10, x + w - 8, y + 30), 6, COL["orange_light"])
        draw.text((x + w - 30, y + 12), badge, fill=COL["orange"], font=load_font(14), anchor="mm")


def draw_timeline_item(draw: ImageDraw.ImageDraw, x: int, y: int, time: str, title: str, tag: str, tag_color: tuple) -> int:
    draw.ellipse((x, y + 4, x + 12, y + 16), fill=COL["blue"])
    draw.line((x + 5, y + 18, x + 5, y + 58), fill=COL["border"], width=2)
    draw.text((x + 20, y), time, fill=COL["text_dim"], font=load_font(18))
    draw.text((x + 20, y + 22), title, fill=COL["text"], font=load_font(20))
    tw = len(tag) * 14 + 16
    rounded_rect(draw, (x + 20, y + 46, x + 20 + tw, y + 68), 6, tag_color)
    draw.text((x + 28, y + 50), tag, fill=COL["white"], font=load_font(16))
    return y + 78


def scheme_a_dashboard(img: Image.Image) -> None:
    """方案 A：数据驾驶舱 — 左右分栏 + 进度条 + 模块网格"""
    draw = draw_phone_frame(img)
    draw_status_bar(draw)
    draw_app_header(draw, "AI Manager 工作台", "2026年7月1日 · 星期三")
    content_y = SAFE_TOP + HEADER_H

    # main title card
    draw_card(draw, 40, content_y, W - 80, 200, 18)
    draw.text((60, content_y + 16), "今日工作概览", fill=COL["navy"], font=load_font(28, True))
    draw.text((60, content_y + 54), "统一管理笔记、待办、电商与系统运维", fill=COL["text_sec"], font=load_font(20))

    metrics = [
        ((219, 234, 254), "今日待办", "3 项", "已完成 1 项"),
        ((209, 250, 229), "笔记文档", "12 篇", "本周新增 2 篇"),
        ((254, 243, 199), "电商待处理", "5 单", "库存预警 1 条"),
        ((224, 231, 255), "系统状态", "正常", "后端 · MySQL · Redis"),
    ]
    my = content_y + 88
    for i, m in enumerate(metrics):
        col = i % 2
        row = i // 2
        draw_metric_row(draw, 60 + col * 360, my + row * 52, m[0], m[1], m[2], m[3])

    # split section
    split_y = content_y + 220
    left_w = 360
    draw_card(draw, 40, split_y, left_w, 420, 16)
    draw.text((56, split_y + 16), "核心指标", fill=COL["navy"], font=load_font(24, True))
    ly = split_y + 56
    items = [
        ("待办完成率", 0.33, COL["green"]),
        ("笔记活跃度", 0.72, COL["blue"]),
        ("电商履约率", 0.85, COL["orange"]),
        ("存储使用率", 0.48, COL["purple"]),
    ]
    for label, ratio, color in items:
        draw.text((56, ly), label, fill=COL["text_sec"], font=load_font(18))
        draw_progress_bar(draw, 56, ly + 24, left_w - 32, ratio, color)
        draw.text((left_w - 16, ly + 8), f"{int(ratio*100)}%", fill=color, font=load_font(18, True), anchor="rt")
        ly += 58

    draw.text((56, ly + 8), "特别提醒", fill=COL["orange"], font=load_font(20, True))
    draw.text((56, ly + 36), "确认 Q2 库存盘点清单", fill=COL["text"], font=load_font(18))
    draw.text((56, ly + 60), "更新电商平台 SKU 映射", fill=COL["text"], font=load_font(18))

    # right timeline
    rx = 40 + left_w + 16
    rw = W - 80 - left_w - 16
    draw_card(draw, rx, split_y, rw, 420, 16)
    draw.text((rx + 16, split_y + 16), "今日动态", fill=COL["navy"], font=load_font(24, True))
    ty = split_y + 56
    for time, title, tag, tc in [
        ("09:00", "周会纪要整理", "笔记", COL["blue"]),
        ("11:30", "发货单 #2048", "电商", COL["orange"]),
        ("14:00", "权限审批待办", "待办", COL["green"]),
        ("16:20", "图片空间备份", "存储", COL["purple"]),
    ]:
        ty = draw_timeline_item(draw, rx + 16, ty, time, title, tag, tc)

    # module grid
    grid_y = split_y + 440
    draw.text((40, grid_y), "功能模块", fill=COL["navy"], font=load_font(24, True))
    modules = [
        (COL["blue_light"], "笔记本", ""),
        (COL["green_light"], "待办", ""),
        (COL["orange_light"], "电商平台", "5"),
        (COL["purple"], "像素狗", ""),
        (COL["blue_light"], "用户中心", ""),
        (COL["blue_light"], "权限中心", ""),
    ]
    gw, gh = 120, 88
    gx0, gy0 = 40, grid_y + 36
    for i, (c, name, badge) in enumerate(modules):
        col = i % 3
        row = i // 3
        draw_module_chip(draw, gx0 + col * (gw + 12), gy0 + row * (gh + 12), gw, gh, c, name, badge)

    draw.text((40, H - SAFE_BOTTOM - TAB_H - 36), "AI Manager 统一管理平台 · 2026", fill=COL["text_dim"], font=load_font(16))
    draw_tabbar(draw, 0)


def scheme_b_cards(img: Image.Image) -> None:
    """方案 B：卡片瀑布 — 大统计卡 + 模块横滑 + 待办列表"""
    draw = draw_phone_frame(img)
    draw_status_bar(draw)
    draw_app_header(draw, "首页", "欢迎回来，管理员")
    y = SAFE_TOP + HEADER_H

    # hero stats row
    stats = [
        ("3", "今日待办", COL["green"], COL["green_light"]),
        ("12", "笔记总数", COL["blue"], COL["blue_light"]),
        ("UP", "后端服务", COL["green"], COL["green_light"]),
        ("5", "电商待办", COL["orange"], COL["orange_light"]),
    ]
    sw = (W - 80 - 36) // 4
    for i, (val, label, vc, bg) in enumerate(stats):
        x = 40 + i * (sw + 12)
        draw_card(draw, x, y, sw, 110, 14)
        rounded_rect(draw, (x + 12, y + 12, x + 44, y + 44), 8, bg)
        draw.text((x + 12, y + 54), val, fill=vc, font=load_font(30, True))
        draw.text((x + 12, y + 88), label, fill=COL["text_sec"], font=load_font(16))

    y += 130
    draw.text((40, y), "快捷入口", fill=COL["navy"], font=load_font(26, True))
    y += 40
    modules = [
        ("笔记本", COL["blue"], "记录与整理"),
        ("待办", COL["green"], "任务与提醒"),
        ("电商平台", COL["orange"], "订单与库存"),
        ("像素狗", COL["purple"], "趣味互动"),
        ("AI 知识库", COL["green"], "即将上线"),
        ("存储中心", COL["grey"], "文件与同步"),
    ]
    cw = 248
    for i, (name, color, desc) in enumerate(modules):
        x = 40 + i * (cw + 16)
        draw_card(draw, x, y, cw, 120, 14)
        rounded_rect(draw, (x + 16, y + 16, x + 56, y + 56), 10, color)
        draw.text((x + 16, y + 68), name, fill=COL["text"], font=load_font(22, True))
        draw.text((x + 16, y + 96), desc, fill=COL["text_dim"], font=load_font(16))
    # scroll hint
    draw.text((W - 56, y + 50), "›", fill=COL["text_dim"], font=load_font(36))

    y += 140
    draw_card(draw, 40, y, W - 80, 280, 16)
    draw.text((60, y + 16), "今日待办", fill=COL["navy"], font=load_font(24, True))
    draw.text((W - 120, y + 20), "查看全部 ›", fill=COL["blue"], font=load_font(18))
    todos = [
        ("确认库存盘点清单", "14:00", False),
        ("回复客户询盘邮件", "16:30", False),
        ("更新笔记：周会纪要", "已完成", True),
    ]
    ty = y + 56
    for title, meta, done in todos:
        rounded_rect(draw, (60, ty + 4, 76, ty + 20), 4, COL["green"] if done else COL["grey_light"])
        color = COL["text_dim"] if done else COL["text"]
        draw.text((88, ty), title, fill=color, font=load_font(20))
        draw.text((88, ty + 28), meta, fill=COL["text_dim"], font=load_font(16))
        ty += 58

    y += 300
    draw_card(draw, 40, y, W - 80, 200, 16)
    draw.text((60, y + 16), "系统状态", fill=COL["navy"], font=load_font(24, True))
    nodes = [("后端 API", "UP", COL["green"]), ("MySQL", "UP", COL["green"]), ("Redis", "UP", COL["green"]), ("Nginx", "UP", COL["green"])]
    nx = 60
    for name, status, c in nodes:
        rounded_rect(draw, (nx, y + 60, nx + 160, y + 130), 12, COL["bg"])
        draw.text((nx + 12, y + 72), name, fill=COL["text_sec"], font=load_font(18))
        rounded_rect(draw, (nx + 12, y + 98, nx + 60, y + 122), 6, c)
        draw.text((nx + 20, y + 100), status, fill=COL["white"], font=load_font(16, True))
        nx += 176

    y += 220
    draw_card(draw, 40, y, W - 80, 160, 16)
    draw.text((60, y + 16), "更多功能", fill=COL["navy"], font=load_font(22, True))
    more = ["权限中心", "图片空间", "部署中心", "全局设置"]
    mx = 60
    for m in more:
        rounded_rect(draw, (mx, y + 56, mx + 120, y + 100), 20, COL["blue_light"])
        draw.text((mx + 60, y + 70), m, fill=COL["blue"], font=load_font(16), anchor="mm")
        mx += 132

    draw_tabbar(draw, 0)


def scheme_c_hub(img: Image.Image) -> None:
    """方案 C：模块中枢 — 大模块宫格 + 紧凑数据条"""
    draw = draw_phone_frame(img)
    draw_status_bar(draw)
    draw_app_header(draw, "AI Manager", "统一管理平台 · 移动版")
    y = SAFE_TOP + HEADER_H

    # compact summary strip
    draw_card(draw, 40, y, W - 80, 72, 12)
    parts = [("待办 3", COL["green"]), ("笔记 12", COL["blue"]), ("电商 5", COL["orange"]), ("系统 正常", COL["green"])]
    px = 56
    for text, c in parts:
        draw.ellipse((px, y + 28, px + 8, y + 36), fill=c)
        draw.text((px + 14, y + 22), text, fill=COL["text"], font=load_font(20))
        px += 180

    y += 92
    draw.text((40, y), "全部功能", fill=COL["navy"], font=load_font(28, True))
    y += 40

    modules = [
        ("笔记本", COL["blue"], "撰写与归档"),
        ("待办", COL["green"], "提醒与星标"),
        ("电商平台", COL["orange"], "多平台运营"),
        ("像素狗", COL["purple"], "休闲小游戏"),
        ("用户中心", (99, 102, 241), "账号与资料"),
        ("权限中心", (14, 165, 233), "用户与角色"),
        ("AI 知识库", COL["green"], "开发中"),
        ("图书馆", COL["grey"], "开发中"),
        ("存储中心", COL["grey"], "本地与网盘"),
        ("图片空间", COL["blue"], "素材管理"),
        ("部署中心", COL["orange"], "发布运维"),
        ("全局设置", COL["grey"], "主题与语言"),
    ]
    mw, mh = 186, 130
    for i, (name, color, desc) in enumerate(modules):
        col = i % 2
        row = i // 2
        x = 40 + col * (mw + 20)
        cy = y + row * (mh + 16)
        draw_card(draw, x, cy, mw, mh, 14)
        rounded_rect(draw, (x + 14, cy + 14, x + 54, cy + 54), 10, color)
        if "开发中" in desc:
            rounded_rect(draw, (x + mw - 58, cy + 10, x + mw - 10, cy + 32), 6, COL["orange_light"])
            draw.text((x + mw - 34, cy + 14), "Soon", fill=COL["orange"], font=load_font(14), anchor="mm")
        draw.text((x + 14, cy + 64), name, fill=COL["navy"], font=load_font(22, True))
        draw.text((x + 14, cy + 94), desc, fill=COL["text_dim"], font=load_font(16))

    y += 6 * (mh + 16) + 8
    draw_card(draw, 40, y, W - 80, 180, 14)
    draw.text((60, y + 14), "今日焦点", fill=COL["navy"], font=load_font(22, True))
    draw.text((60, y + 50), "★ 确认 Q2 固定资产盘点", fill=COL["text"], font=load_font(20))
    draw.text((60, y + 82), "○ 处理电商平台待发货订单", fill=COL["text"], font=load_font(20))
    draw.text((60, y + 114), "○ 整理本周会议笔记", fill=COL["text"], font=load_font(20))

    draw_tabbar(draw, 0)


def scheme_d_schedule(img: Image.Image) -> None:
    """方案 D：日程工作台 — 最接近参考图的行程式布局"""
    draw = draw_phone_frame(img)
    draw_status_bar(draw)
    draw_app_header(draw, "工作台日程", "AI Manager · 移动工作台")
    y = SAFE_TOP + HEADER_H

    # top summary like reference
    draw_card(draw, 40, y, W - 80, 320, 18)
    draw.text((60, y + 16), "今日管理日程", fill=COL["navy"], font=load_font(30, True))

    left_x = 60
    summary = [
        ("📋", "待办事项", "3 项待处理"),
        ("📝", "笔记更新", "2 篇本周"),
        ("🛒", "电商运营", "5 单待发货"),
        ("⚙️", "系统巡检", "全部正常"),
        ("📦", "存储同步", "百度网盘已连接"),
        ("🔒", "权限审批", "1 条待确认"),
    ]
    sy = y + 60
    for icon, label, val in summary:
        draw.text((left_x, sy), icon, fill=COL["text"], font=load_font(20))
        draw.text((left_x + 32, sy), label, fill=COL["text_sec"], font=load_font(18))
        draw.text((left_x + 140, sy), val, fill=COL["navy"], font=load_font(18, True))
        sy += 36

    # right mini timeline
    tx = 420
    draw.text((tx, y + 60), "时间线", fill=COL["blue"], font=load_font(20, True))
    times = [
        ("08:30", "晨间待办梳理", "待办"),
        ("10:00", "电商订单复核", "电商"),
        ("13:30", "笔记：产品规划", "笔记"),
        ("15:00", "用户权限审核", "权限"),
    ]
    tyy = y + 92
    for time, evt, tag in times:
        draw.text((tx, tyy), time, fill=COL["text_dim"], font=load_font(16))
        draw.text((tx + 72, tyy), evt, fill=COL["text"], font=load_font(17))
        rounded_rect(draw, (tx + 72, tyy + 22, tx + 72 + len(tag) * 15 + 12, tyy + 42), 4, COL["blue_light"])
        draw.text((tx + 80, tyy + 24), tag, fill=COL["blue"], font=load_font(14))
        tyy += 52

    y += 340
    # middle bar chart strip
    draw_card(draw, 40, y, W - 80, 100, 14)
    draw.text((60, y + 14), "工作负荷分布", fill=COL["navy"], font=load_font(22, True))
    bars = [("待办", 0.35, COL["green"]), ("笔记", 0.25, COL["blue"]), ("电商", 0.55, COL["orange"]), ("运维", 0.15, COL["purple"])]
    bx = 60
    for label, ratio, color in bars:
        bw = 160
        draw.text((bx, y + 48), label, fill=COL["text_sec"], font=load_font(16))
        bh = int(36 * ratio)
        rounded_rect(draw, (bx, y + 68 + (36 - bh), bx + bw, y + 104), 4, color)
        bx += 176

    y += 120
    # budget-style section
    draw_card(draw, 40, y, W - 80, 200, 14)
    draw.text((60, y + 14), "模块资源分配", fill=COL["navy"], font=load_font(22, True))
    alloc = [
        ("电商平台 · 订单履约", 0.62, "5 单待处理"),
        ("笔记本 · 文档整理", 0.40, "12 篇归档"),
        ("存储中心 · 空间占用", 0.48, "剩余 52%"),
    ]
    ay = y + 50
    for label, ratio, sub in alloc:
        draw.text((60, ay), label, fill=COL["text"], font=load_font(18))
        draw.text((W - 100, ay), sub, fill=COL["text_dim"], font=load_font(16), anchor="rt")
        draw_progress_bar(draw, 60, ay + 26, W - 120, ratio, COL["blue"])
        ay += 52

    y += 220
    draw.text((40, y), "快捷指引", fill=COL["navy"], font=load_font(24, True))
    y += 36
    quick = [
        ("会议准备", "笔记"),
        ("发货确认", "电商"),
        ("权限审核", "权限"),
        ("素材上传", "图片"),
        ("备份检查", "存储"),
        ("系统设置", "设置"),
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

    draw.text((40, H - SAFE_BOTTOM - TAB_H - 30), "智能管理工作台 · 2026 版", fill=COL["text_dim"], font=load_font(16))
    draw_tabbar(draw, 0)


SCHEMES = [
    ("scheme-a-dashboard", "方案 A · 数据驾驶舱", scheme_a_dashboard),
    ("scheme-b-cards", "方案 B · 卡片瀑布", scheme_b_cards),
    ("scheme-c-hub", "方案 C · 模块中枢", scheme_c_hub),
    ("scheme-d-schedule", "方案 D · 日程工作台", scheme_d_schedule),
]


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for slug, label, fn in SCHEMES:
        img = Image.new("RGBA", (W, H), (30, 30, 32, 255))
        fn(img)
        # title watermark outside phone
        draw = ImageDraw.Draw(img)
        draw.text((W // 2, 8), label, fill=(200, 200, 200), font=load_font(18), anchor="mt")
        out = OUT_DIR / f"mobile-home-{slug}.png"
        img.convert("RGB").save(out, quality=95)
        print(f"Wrote {out}")


if __name__ == "__main__":
    main()
