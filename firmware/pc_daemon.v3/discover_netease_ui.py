"""发现网易云音乐窗口的进度条控件（增强版）。"""

import time
import uiautomation as auto


def main():
    root = auto.GetRootControl()

    # 通过进程名找网易云窗口
    import psutil
    netease_pids = set()
    for proc in psutil.process_iter(["pid", "name"]):
        try:
            name = (proc.info.get("name") or "").lower()
            if name == "cloudmusic.exe":
                netease_pids.add(proc.info["pid"])
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass

    if not netease_pids:
        print("[-] cloudmusic.exe 进程未运行")
        return

    print(f"[+] NetEase PIDs: {netease_pids}")

    # 找到匹配的窗口
    win = None
    for w in root.GetChildren():
        try:
            pid = w.ProcessId
        except Exception:
            continue
        if pid in netease_pids and w.Name:
            win = w
            print(f"[+] Found window: '{win.Name}' PID={pid}")
            break

    if not win:
        print("[-] 未找到网易云窗口（可能最小化到托盘）")
        return

    try:
        win.SetActive()
        time.sleep(0.5)
    except Exception:
        pass

    # 遍历控件树，寻找进度条
    def find_sliders(control, depth=0):
        if depth > 6:
            return
        try:
            children = control.GetChildren()
        except Exception:
            return
        for child in children:
            try:
                ctrl_type = child.ControlTypeName
                name = child.Name or ""
                auto_id = child.AutomationId or ""
                rect = child.BoundingRectangle
                class_name = child.ClassName or ""

                is_slider = ctrl_type in ("SliderControl", "ProgressBarControl", "ThumbControl")
                is_progress_like = any(k in name.lower() for k in ["progress", "slider", "seek", "time", "track"])
                is_wide_short = rect and (rect.right - rect.left) > 100 and (rect.bottom - rect.top) < 40

                if is_slider or is_progress_like or (is_wide_short and depth >= 2):
                    print(f"\n[!] type={ctrl_type} name='{name}' id='{auto_id}'")
                    print(f"    Rect: {rect}  ClassName: {class_name}  Depth: {depth}")
                    print(f"    IsOffscreen: {child.IsOffscreen}  IsEnabled: {child.IsEnabled}")
                    try:
                        rv = child.GetRangeValuePattern()
                        print(f"    RangeValue: {rv.Value:.0f}/{rv.Maximum:.0f} <= SEEK via SetValue!")
                    except Exception:
                        pass
                    try:
                        vp = child.GetValuePattern()
                        print(f"    Value: {vp.Value}")
                    except Exception:
                        pass
                    print()

                if depth <= 2:
                    indent = "  " * depth
                    print(f"{indent}[{ctrl_type}] '{name}' cls={class_name} rect={rect}")

                find_sliders(child, depth + 1)
            except Exception:
                continue

    find_sliders(win)


if __name__ == "__main__":
    main()
