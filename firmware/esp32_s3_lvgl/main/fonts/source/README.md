# 中文字体源文件

固件界面中文使用 **阿里妈妈刀隶体**（Alimama DaoLiTi），免费可商用。

## 下载

1. 打开 [阿里妈妈字体](https://www.alibabafonts.com)
2. 找到 **阿里妈妈刀隶体**，下载 TTF
3. 将字体文件复制到本目录，并命名为其一：
   - `AlimamaDaoLiTi.ttf`（推荐）
   - `AlimamaDaoLiTi-Regular.ttf`
   - `阿里妈妈刀隶体.ttf`

## 生成 LVGL 字库

在已安装 Node.js 的环境下，于工程根目录执行：

```powershell
cd firmware\esp32_s3_lvgl
python scripts\generate_font_chinese_20.py
python scripts\generate_font_chinese_28.py
```

或一次性生成两种字号：

```powershell
python scripts\generate_all_fonts.py
```

生成结果写入 `main/fonts/font_chinese_20.c` 与 `font_chinese_28.c`，然后重新 `idf.py build` 烧录。

> 字库包含 GB2312 简中字符，生成较慢、体积较大，属正常现象。
