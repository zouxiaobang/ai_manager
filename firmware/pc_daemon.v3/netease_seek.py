"""通过模拟鼠标点击网易云音乐窗口的进度条来实现 seek。

网易云音乐使用 CEF (Chromium) 渲染 UI，进度条不是原生 Windows 控件，
无法通过 UIA 直接操作。只能通过模拟鼠标点击来实现跳转。
"""

from __future__ import annotations

import logging
import time

import uiautomation as auto
import win32api
import win32con

logger = logging.getLogger("netease_seek")

# 进度条在窗口底部的位置（从窗口底部向上偏移的像素量）
# 不同版本/皮肤可能需要调整
PROGRESS_BAR_BOTTOM_OFFSET = 60  # 从窗口底部向上偏移


def _find_netease_cef_window():
    """找到网易云音乐的 CEF 渲染窗口。"""
    import psutil

    netease_pids = set()
    for proc in psutil.process_iter(["pid", "name"]):
        try:
            if (proc.info.get("name") or "").lower() == "cloudmusic.exe":
                netease_pids.add(proc.info["pid"])
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass

    if not netease_pids:
        return None

    root = auto.GetRootControl()
    for w in root.GetChildren():
        try:
            if w.ProcessId in netease_pids and w.Name:
                return w
        except Exception:
            continue
    return None


def seek_to(position_ms: int, duration_ms: int) -> bool:
    """通过点击进度条 seek 到指定位置。

    Args:
        position_ms: 目标位置（毫秒）
        duration_ms: 歌曲总时长（毫秒）
    """
    if duration_ms <= 0:
        return False

    win = _find_netease_cef_window()
    if win is None:
        logger.warning("Seek skipped: NetEase window not found")
        return False

    rect = win.BoundingRectangle
    if not rect or rect.right <= rect.left or rect.bottom <= rect.top:
        logger.warning("Seek skipped: window has no valid rect")
        return False

    # 计算进度条位置
    win_width = rect.right - rect.left
    win_height = rect.bottom - rect.top

    # X: 根据 seek 比例计算
    ratio = max(0.0, min(1.0, position_ms / duration_ms))
    click_x = int(rect.left + win_width * ratio)

    # Y: 窗口底部向上偏移
    click_y = rect.bottom - PROGRESS_BAR_BOTTOM_OFFSET

    logger.info(
        "Seek to %.1f%% (pos=%d/%d ms) -> click (%d, %d) in window [%d\u00d7%d]",
        ratio * 100, position_ms, duration_ms,
        click_x, click_y, win_width, win_height,
    )

    # 确保窗口是激活状态
    try:
        win.SetActive()
        time.sleep(0.05)
    except Exception:
        pass

    # 模拟鼠标点击
    old_pos = win32api.GetCursorPos()
    try:
        win32api.SetCursorPos((click_x, click_y))
        win32api.mouse_event(win32con.MOUSEEVENTF_LEFTDOWN, click_x, click_y, 0, 0)
        time.sleep(0.02)
        win32api.mouse_event(win32con.MOUSEEVENTF_LEFTUP, click_x, click_y, 0, 0)
        return True
    except Exception as exc:
        logger.warning("Mouse click failed: %s", exc)
        return False
    finally:
        try:
            win32api.SetCursorPos(old_pos)
        except Exception:
            pass
