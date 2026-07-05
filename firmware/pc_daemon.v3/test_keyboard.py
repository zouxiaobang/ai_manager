"""测试键盘模拟 + 强制前台窗口。"""

import logging
import time
import ctypes
import ctypes.wintypes
import win32con
import win32gui
import win32process
import win32api

logging.basicConfig(level=logging.INFO, format="%(message)s")


def find_netease_hwnd():
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
    results = []
    for p in netease_pids:
        def cb(hwnd, _):
            _, pid = win32process.GetWindowThreadProcessId(hwnd)
            if pid == p and win32gui.IsWindowVisible(hwnd) and win32gui.GetWindowText(hwnd):
                results.append(hwnd)
        win32gui.EnumWindows(cb, None)
    return results[0] if results else None


def bring_to_foreground(hwnd):
    """多种方式尝试把窗口带到前台。"""
    methods = []

    # 1. AttachThreadInput 技巧
    try:
        our_tid = win32api.GetCurrentThreadId()
        net_tid, _ = win32process.GetWindowThreadProcessId(hwnd)
        win32process.AttachThreadInput(our_tid, net_tid, True)
        ok = win32gui.SetForegroundWindow(hwnd)
        win32process.AttachThreadInput(our_tid, net_tid, False)
        methods.append(("AttachThreadInput", ok))
    except Exception as e:
        methods.append(("AttachThreadInput", str(e)))

    # 2. SwitchToThisWindow (undocumented but effective)
    try:
        ctypes.windll.user32.SwitchToThisWindow(hwnd, True)
        methods.append(("SwitchToThisWindow", True))
    except Exception as e:
        methods.append(("SwitchToThisWindow", str(e)))

    # 3. ShowWindow + SetForegroundWindow
    try:
        win32gui.ShowWindow(hwnd, 5)  # SW_SHOW
        time.sleep(0.05)
        win32gui.SetForegroundWindow(hwnd)
        methods.append(("ShowWindow+SetForeground", True))
    except Exception as e:
        methods.append(("ShowWindow+SetForeground", str(e)))

    # 4. 最小化再还原
    try:
        win32gui.ShowWindow(hwnd, 6)  # SW_MINIMIZE
        time.sleep(0.1)
        win32gui.ShowWindow(hwnd, 9)  # SW_RESTORE
        methods.append(("Minimize+Restore", True))
    except Exception as e:
        methods.append(("Minimize+Restore", str(e)))

    return methods


def send_key_global(vk_code, scan=0):
    """全局 SendInput 键盘按键。"""
    class KEYBDINPUT(ctypes.Structure):
        _fields_ = [
            ("wVk", ctypes.wintypes.WORD),
            ("wScan", ctypes.wintypes.WORD),
            ("dwFlags", ctypes.wintypes.DWORD),
            ("time", ctypes.wintypes.DWORD),
            ("dwExtraInfo", ctypes.POINTER(ctypes.c_ulong)),
        ]

    class INPUT(ctypes.Structure):
        _fields_ = [
            ("type", ctypes.wintypes.DWORD),
            ("ki", KEYBDINPUT),
        ]

    down = INPUT()
    down.type = 1
    down.ki = KEYBDINPUT(vk_code, scan, 0, 0, None)
    ctypes.windll.user32.SendInput(1, ctypes.byref(down), ctypes.sizeof(INPUT))
    time.sleep(0.02)

    up = INPUT()
    up.type = 1
    up.ki = KEYBDINPUT(vk_code, scan, 2, 0, None)  # KEYEVENTF_KEYUP
    ctypes.windll.user32.SendInput(1, ctypes.byref(up), ctypes.sizeof(INPUT))


def send_input_with_focus(hwnd, vk_code, label):
    """先切窗口再按键。"""
    print(f"\n[{label}] 尝试切换到网易云窗口...")
    methods = bring_to_foreground(hwnd)
    for name, ok in methods:
        print(f"    {name}: {ok}")
    time.sleep(0.3)

    # 检查前台窗口
    fg = win32gui.GetForegroundWindow()
    fg_title = win32gui.GetWindowText(fg)
    print(f"    当前前台窗口: {fg} '{fg_title}'")

    target_title = win32gui.GetWindowText(hwnd)
    if fg == hwnd:
        print(f"    ✓ 成功切换到目标窗口!")
    else:
        print(f"    ✗ 未切换到目标窗口 (当前={fg_title}, 目标={target_title})")
        print(f"    仍然尝试发送按键...")

    # 发送按键
    print(f"    发送 VK=0x{vk_code:02X}...")
    send_key_global(vk_code)
    print(f"    ✓ 已发送")


VK_RIGHT = 0x27
VK_LEFT = 0x25


def main():
    hwnd = find_netease_hwnd()
    if not hwnd:
        print("[-] 窗口未找到")
        return

    rect = win32gui.GetWindowRect(hwnd)
    print(f"[+] 窗口句柄: {hwnd}  {rect[2]-rect[0]}x{rect[3]-rect[1]}")
    print(f"    标题: {win32gui.GetWindowText(hwnd)}")

    # 先做一个简单的测试：用 SMTC 方式观察播放状态
    print("\n观察 SMTC 初始位置...")
    from smtc_bridge import SMTCBridge
    bridge = SMTCBridge(poll_interval=0.3)
    time.sleep(1)

    state = bridge.get_state()
    if state.available:
        print(f"    当前: {_fmt_time(state.position_ms)} / {_fmt_time(state.duration_ms)}")
        initial_pos = state.position_ms
    else:
        initial_pos = 0
        print("    SMTC 不可用")

    # 每 5 秒检查一次进度
    def check_progress(label=""):
        s = bridge.get_state()
        if s.available:
            delta = s.position_ms - initial_pos
            print(f"    [{label}] 位置: {_fmt_time(s.position_ms)}  (变化: {delta/1000:.1f}s)")
            return s
        return None

    # 测试 1: 先试试直接按键（不切前台）
    print("\n" + "=" * 50)
    print("测试 1: 不切前台，直接发 5× 右键")
    print("=" * 50)
    time.sleep(2)
    for i in range(5):
        send_key_global(VK_RIGHT)
        time.sleep(0.1)
    check_progress("不切前台")

    # 测试 2: 切前台再按键
    print("\n" + "=" * 50)
    print("测试 2: 强制切前台 + 10× 右键")
    print("=" * 50)
    time.sleep(2)
    send_input_with_focus(hwnd, VK_RIGHT, "切前台")
    send_input_with_focus(hwnd, VK_RIGHT, "切前台")
    time.sleep(1)
    check_progress("切前台")

    # 测试 3: 主窗口打开 + 多次按键
    print("\n" + "=" * 50)
    print("测试 3: 最大化 + 15× 右键")
    print("=" * 50)
    time.sleep(2)
    win32gui.ShowWindow(hwnd, 3)  # SW_MAXIMIZE
    time.sleep(0.3)
    bring_to_foreground(hwnd)
    time.sleep(0.5)
    for i in range(15):
        send_key_global(VK_RIGHT)
        time.sleep(0.05)
    check_progress("最大化")
    state = check_progress("最终")

    # 测试 4: 手动尝试通过 PostMessage 发送 WM_KEYDOWN 到子窗口
    print("\n" + "=" * 50)
    print("测试 4: PostMessage WM_KEYDOWN 到子窗口")
    print("=" * 50)

    def enum_child(hwnd, depth=0):
        if depth > 3:
            return
        children = []
        win32gui.EnumChildWindows(hwnd, lambda h, _: children.append(h), None)
        for ch in children:
            cls = win32gui.GetClassName(ch)
            rect = win32gui.GetWindowRect(ch)
            print(f"    child: cls='{cls}' rect={rect}")
            if depth == 0:
                # 给第一个子窗口发右键
                win32gui.PostMessage(ch, win32con.WM_KEYDOWN, VK_RIGHT, 0)
                time.sleep(0.02)
                win32gui.PostMessage(ch, win32con.WM_KEYUP, VK_RIGHT, 0)
                print(f"    → 发送右键到 {cls}")

            enum_child(ch, depth + 1)

    enum_child(hwnd)
    time.sleep(1)
    check_progress("PostMessage")


def _fmt_time(ms):
    if ms <= 0:
        return "0:00"
    return f"{ms//60000}:{(ms%60000)//1000:02d}"


if __name__ == "__main__":
    main()
