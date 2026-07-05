"""校准进度条 - SendInput API + CDP 方式。"""

import logging
import time
import ctypes
import ctypes.wintypes
import win32gui
import uiautomation as auto

logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger("calibrate")


def find_netease_hwnd():
    import psutil, win32process
    netease_pids = set()
    for proc in psutil.process_iter(["pid", "name"]):
        try:
            if (proc.info.get("name") or "").lower() == "cloudmusic.exe":
                netease_pids.add(proc.info["pid"])
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass
    if not netease_pids:
        return None, None
    results = []
    for p in netease_pids:
        def cb(hwnd, _):
            _, pid = win32process.GetWindowThreadProcessId(hwnd)
            if pid == p and win32gui.IsWindowVisible(hwnd) and win32gui.GetWindowText(hwnd):
                results.append((hwnd, p))
        win32gui.EnumWindows(cb, None)
    return results[0] if results else (None, None)


def sendinput_click(client_x, client_y, hwnd):
    """使用 SendInput API（最底层输入模拟）。"""
    rect = win32gui.GetWindowRect(hwnd)
    screen_x = rect[0] + client_x
    screen_y = rect[1] + client_y

    # 获取屏幕尺寸
    screen_w = ctypes.windll.user32.GetSystemMetrics(0)
    screen_h = ctypes.windll.user32.GetSystemMetrics(1)

    # 归一化坐标到 0-65535
    dx = int(screen_x * 65535 // screen_w)
    dy = int(screen_y * 65535 // screen_h)

    class MOUSEINPUT(ctypes.Structure):
        _fields_ = [
            ("dx", ctypes.wintypes.LONG),
            ("dy", ctypes.wintypes.LONG),
            ("mouseData", ctypes.wintypes.DWORD),
            ("dwFlags", ctypes.wintypes.DWORD),
            ("time", ctypes.wintypes.DWORD),
            ("dwExtraInfo", ctypes.POINTER(ctypes.c_ulong)),
        ]

    class INPUT(ctypes.Structure):
        _fields_ = [
            ("type", ctypes.wintypes.DWORD),
            ("mi", MOUSEINPUT),
        ]

    # 鼠标按下
    down = INPUT()
    down.type = 0  # INPUT_MOUSE
    down.mi = MOUSEINPUT(dx, dy, 0, 0x8002, 0, None)  # ABSOLUTE | LEFTDOWN
    ctypes.windll.user32.SendInput(1, ctypes.byref(down), ctypes.sizeof(INPUT))
    time.sleep(0.03)

    # 鼠标抬起
    up = INPUT()
    up.type = 0
    up.mi = MOUSEINPUT(dx, dy, 0, 0x8004, 0, None)  # ABSOLUTE | LEFTUP
    ctypes.windll.user32.SendInput(1, ctypes.byref(up), ctypes.sizeof(INPUT))

    print(f"[SendInput] 点击屏幕 ({screen_x},{screen_y}) 归一化 ({dx},{dy})")


def try_cdp_seek():
    """尝试通过 Chrome DevTools Protocol 连接 CEF 并执行 JS seek。"""
    import socket, json, requests

    # 扫描可能的 CDP 端口
    for port in range(9222, 9232):
        try:
            r = requests.get(f"http://127.0.0.1:{port}/json", timeout=1)
            if r.status_code == 200:
                targets = r.json()
                for t in targets:
                    url = (t.get("url") or "").lower()
                    title = (t.get("title") or "").lower()
                    if "music" in url or "music" in title or "netease" in url:
                        ws_url = t.get("webSocketDebuggerUrl")
                        if ws_url:
                            print(f"[CDP] Found NetEase target at port {port}: {t.get('title')}")
                            return port, ws_url
                print(f"[CDP] Port {port}: no matching target")
            return None, None
        except requests.ConnectionError:
            continue
    print("[CDP] No debugging port found (9222-9231)")
    return None, None


def main():
    print("=" * 55)
    print("网易云进度条校准 v4 - SendInput + CDP")
    print("=" * 55)

    hwnd, pid = find_netease_hwnd()
    if not hwnd:
        print("[-] 窗口未找到")
        return
    rect = win32gui.GetWindowRect(hwnd)
    ww = rect[2] - rect[0]
    wh = rect[3] - rect[1]
    print(f"[+] 窗口: {hwnd} PID={pid}  {ww}x{wh}")

    # 先试 CDP
    print("\n=== 尝试 CDP ===")
    cdp_port, cdp_ws = try_cdp_seek()
    if cdp_port:
        print("[+] CDP 可用！不需要鼠标点击了。")
    else:
        print("[-] CDP 不可用")

    print("\n=== 测试 SendInput 点击 ===")
    print("测试偏移量范围 (底部往上 px):")
    offsets = list(range(20, 161, 10))
    for i, off in enumerate(offsets):
        print(f"  {i+1} → {off}px", end="  " if (i+1) % 5 else "\n")
    print()

    while True:
        try:
            key = input("\n选择 (1-15, 0=退出): ").strip()
            if key == "0":
                break
            idx = int(key) - 1
            if 0 <= idx < len(offsets):
                offset = offsets[idx]
                client_x = int(ww * 0.3)  # 30% 位置
                client_y = wh - offset
                print(f"\n偏移 {offset}px → 点击客户端 ({client_x}, {client_y})")
                print("3 秒后...")
                for s in range(3, 0, -1):
                    print(f"  {s}...")
                    time.sleep(1)

                # 激活窗口
                win32gui.ShowWindow(hwnd, 5)
                win32gui.SetForegroundWindow(hwnd)
                time.sleep(0.15)

                # 发送 3 次点击（间隔 200ms）
                for i in range(3):
                    sendinput_click(client_x, client_y, hwnd)
                    time.sleep(0.2)

                print("✓ 已点击，请观察\n")
            else:
                print("无效")
        except KeyboardInterrupt:
            break
        except Exception as e:
            print(f"错误: {e}")


if __name__ == "__main__":
    main()
