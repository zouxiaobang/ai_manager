# SD 卡资源（复制到 TF 卡根目录）

将本目录下 **`assets/`** 和 **`lyrics/`** 两个文件夹复制到 FAT32 卡根目录，结构如下：

```
TF卡根目录/
  assets/
    tomato.png
    icon_wifi.png
    icon_lock.png
    dock_pomo.png
    dock_lyrics.png
    dock_sleep.png
    dock_lock.png
    dock_settings.png
  lyrics/
    current.meta    # 歌名一行
    current.txt     # 歌词正文（UTF-8，\n 换行）
```

## 生成 / 更新资源

```powershell
pip install pillow
py sdcard_assets\generate_assets.py
```

然后把 `sdcard_assets\assets` 和 `sdcard_assets\lyrics` 复制到 TF 卡。

## 说明

- 固件挂载点为 `/sdcard`（LVGL 路径 `A:assets/...`）
- 无 SD 卡或文件缺失时，自动回退到代码绘制的像素图标与内置歌词
- 中文字库仍在 Flash，SD 卡主要放 **图片 + 歌词文本**
