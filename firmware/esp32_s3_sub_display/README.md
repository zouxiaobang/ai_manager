# ESP32-S3 副屏

微雪 **ESP32-S3-Touch-LCD-7**（800×480）副屏固件。

## 主界面（方案 C）

- 双卡片预览：番茄钟 / 歌词（mock 数据）
- Dock 5 项：`Pomo` `Lyric` `Sleep` `Lock` `More`
- 「更多」：Weather / PC Stats / Notes / Settings / Media（占位）
- 夜间降亮度默认 **22:00–08:00**（设置页可调）
- 闲置超时变暗；点击屏幕恢复亮度
- Dock「Sleep」完全息屏，点击唤醒
- Dock「Lock」滑动上滑解锁；番茄钟进行中时锁屏显示进度

## 编译

本机 ESP-IDF 路径（已配置）：

- IDF：`G:\projects\iot\Espressif\frameworks\esp-idf-v5.5.2`
- 工具链：`G:\projects\iot\Espressif`

### 方式一：脚本（推荐，Cursor / 普通 PowerShell 均可）

```powershell
cd firmware\esp32_s3_sub_display
.\scripts\build.ps1
```

烧录并打开串口监视：

```powershell
.\scripts\flash-monitor.ps1
# 或指定端口
.\scripts\flash-monitor.ps1 -Port COM5
```

仅加载 ESP-IDF 环境（当前终端后续可直接用 `idf.py`）：

```powershell
. .\scripts\idf-env.ps1
idf.py build
```

### 方式二：ESP-IDF 终端

若已安装 Espressif 的「ESP-IDF PowerShell」，进入工程目录后：

```powershell
idf.py build flash monitor
```

### Cursor / VS Code

已写入 `.vscode/settings.json`，安装 **Espressif IDF** 扩展后可用扩展面板的 Build / Flash。

首次在新终端编译前，若 `python` 找不到，先执行 `. .\scripts\idf-env.ps1`。

## SD 卡资源（FAT32）

1. 生成资源：`py sdcard_assets\generate_assets.py`
2. 将 `sdcard_assets\assets` 与 `sdcard_assets\lyrics` 复制到 **TF 卡根目录**
3. 插入卡后上电；串口应出现 `SD mounted at /sdcard`

| SD 路径 | 用途 |
|---------|------|
| `assets/*.png` | 番茄、WiFi/锁图标、Dock 图标 |
| `lyrics/current.meta` | 歌名（一行 UTF-8） |
| `lyrics/current.txt` | 歌词正文 |

无卡或缺文件时自动回退代码像素绘制与内置歌词。字库仍在 Flash（GB2312）。

详见 [sdcard_assets/README.md](sdcard_assets/README.md)。

## 中文字体（歌词）

**不需要**完整 Unicode 字库（体积过大）。已改用 **GB2312 全集**（约 6700 字，覆盖绝大多数中文歌词），编译进固件 Flash（约 5.7MB），与固件一并烧录。

首次或更新字库后：

```powershell
py fonts\generate_gb2312_font.py
idf.py build flash monitor
```

- 分区：`factory` 已扩至 **8MB**（容纳字库 + 程序）
- 字库分 8 片，LVGL `fallback` 链自动拼接
- 若新增极罕见字，可改 `generate_gb2312_font.py` 或扩充 `glyphs_cn.txt` 后重生成

旧版小字库脚本：`fonts/generate_cn_font.ps1`（仅 UI 少量字，已被 GB2312 取代）

## 模块

| 文件 | 说明 |
|------|------|
| `app_ui.cpp` | 主屏 / 锁屏 / 更多 / 设置 |
| `pomodoro_model.cpp` | 番茄钟 mock（后续接 API） |
| `app_settings.cpp` | NVS 设置 |
| `app_power.cpp` | 变暗 / 息屏 |
| `app_clock.cpp` | 时钟（待 SNTP） |
