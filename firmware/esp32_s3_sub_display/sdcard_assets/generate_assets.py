#!/usr/bin/env python3
"""Generate pixel-art PNG assets for SD card and LVGL embedded fallbacks."""

from __future__ import annotations

from pathlib import Path

try:
    from PIL import Image, ImageDraw
except ImportError:
    raise SystemExit("Install Pillow: pip install pillow")

ROOT = Path(__file__).resolve().parent
ASSETS = ROOT / "assets"
LYRICS = ROOT / "lyrics"
EMBED_DIR = ROOT.parent / "main" / "assets_embed"
REFERENCE_TOMATO = ROOT / "reference" / "tomato_ref.png"
REFERENCE_DOCK_LYRICS = ROOT / "reference" / "dock_lyrics_ref.png"
REFERENCE_DOCK_SLEEP = ROOT / "reference" / "dock_sleep_ref.png"
REFERENCE_DOCK_LOCK = ROOT / "reference" / "dock_lock_ref.png"
REFERENCE_DOCK_SETTINGS = ROOT / "reference" / "dock_settings_ref.png"
REFERENCE_ICON_LOCK = ROOT / "reference" / "icon_lock_ref.png"
REFERENCE_ICON_WIFI = ROOT / "reference" / "icon_wifi_ref.png"

# Palette aligned with reference mockup
GREEN = (139, 195, 74, 255)
DGREEN = (76, 175, 80, 255)
LGREEN = (129, 199, 132, 255)
STEM = (46, 125, 50, 255)
RED = (229, 57, 53, 255)
DRED = (198, 40, 40, 255)
HILITE = (255, 205, 210, 255)
BLUE = (41, 182, 246, 255)
DBLUE = (13, 71, 161, 255)
CYAN = (66, 165, 245, 255)
ORANGE = (255, 152, 0, 255)
DORANGE = (230, 81, 0, 255)
GREY = (120, 144, 156, 255)
WHITE = (236, 239, 241, 255)
LAVENDER = (206, 147, 216, 255)
TRANSPARENT = (0, 0, 0, 0)
NIGHT = (20, 30, 60, 255)

EMBED_FILES: list[tuple[str, str]] = []


def new_rgba(w: int, h: int) -> Image.Image:
    return Image.new("RGBA", (w, h), TRANSPARENT)


def blit(img: Image.Image, x: int, y: int, color: tuple[int, int, int, int]) -> None:
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), color)


def fill_rect(img: Image.Image, x: int, y: int, w: int, h: int, color: tuple[int, int, int, int]) -> None:
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            blit(img, xx, yy, color)


def paste_center(dst: Image.Image, src: Image.Image) -> None:
    ox = (dst.width - src.width) // 2
    oy = (dst.height - src.height) // 2
    dst.paste(src, (ox, oy), src)


def tomato_from_reference(card_size: int = 96, dock_size: int = 28) -> tuple[Image.Image, Image.Image]:
    """Build transparent tomato PNGs from reference art (图2)."""
    if not REFERENCE_TOMATO.is_file():
        card = draw_tomato(card_size)
        dock = card.resize((dock_size, dock_size), Image.NEAREST)
        return card, dock

    im = Image.open(REFERENCE_TOMATO).convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if r <= 12 and g <= 18 and 10 <= b <= 40:
                px[x, y] = TRANSPARENT
                continue
            if r + g + b < 75 and r < 80:
                px[x, y] = TRANSPARENT

    minx, miny, maxx, maxy = w, h, 0, 0
    for y in range(h):
        for x in range(w):
            if px[x, y][3] > 0:
                minx = min(minx, x)
                miny = min(miny, y)
                maxx = max(maxx, x)
                maxy = max(maxy, y)
    pad = 4
    im = im.crop((max(0, minx - pad), max(0, miny - pad), min(w, maxx + pad + 1), min(h, maxy + pad + 1)))

    side = max(im.width, im.height)
    square = new_rgba(side, side)
    paste_center(square, im)
    card = square.resize((card_size, card_size), Image.NEAREST)
    dock = square.resize((dock_size, dock_size), Image.NEAREST)
    return card, dock


def draw_tomato(size: int = 80) -> Image.Image:
    img = new_rgba(size, size)
    p = max(2, size // 20)
    cx, cy = size // 2, size // 2 + p // 2
    radius = size // 2 - p * 2

    for y in range(size):
        for x in range(size):
            dx = x - cx
            dy = y - cy
            dist2 = dx * dx + dy * dy
            if dist2 <= radius * radius:
                shade = DRED if dx * 0.6 + dy > 6 else RED
                if dx < -p * 2 and dy < -p:
                    shade = HILITE
                blit(img, x, y, shade)

    stem_w = max(3, p * 2)
    fill_rect(img, cx - stem_w // 2, cy - radius - p * 4, stem_w, p * 4, STEM)
    fill_rect(img, cx - p * 5, cy - radius - p * 3, p * 4, p * 2, LGREEN)
    fill_rect(img, cx + p, cy - radius - p * 4, p * 4, p * 2, DGREEN)
    fill_rect(img, cx - p * 2, cy - radius - p, p * 2, p, LGREEN)
    return img


def draw_wifi(w: int = 24, h: int = 20) -> Image.Image:
    img = new_rgba(w, h)
    p = 2
    cx = w // 2
    bands = [(h - p * 2, 9), (h - p * 4, 6), (h - p * 6, 3)]
    for base_y, span in bands:
        for i in range(span):
            x0 = cx - i * p
            x1 = cx + i * p
            blit(img, x0, base_y - i, GREEN)
            blit(img, x1, base_y - i, GREEN)
    fill_rect(img, cx - p, h - p * 2, p * 2, p * 2, GREEN)
    return img


def draw_lock_status(w: int = 18, h: int = 22) -> Image.Image:
    img = new_rgba(w, h)
    p = 2
    fill_rect(img, 5 * p, 0, 4 * p, p, GREEN)
    fill_rect(img, 4 * p, p, p, 2 * p, GREEN)
    fill_rect(img, 9 * p, p, p, 2 * p, GREEN)
    fill_rect(img, 3 * p, 3 * p, 6 * p, 7 * p, GREEN)
    fill_rect(img, 5 * p, 5 * p, 2 * p, 3 * p, TRANSPARENT)
    return img


def unlock_status_from_lock(locked: Image.Image) -> Image.Image:
    """Derive open-lock icon from locked icon: left shackle stays, right lifts."""
    im = locked.copy().convert("RGBA")
    px = im.load()
    w, h = im.size
    green = GREEN
    for y in range(h):
        for x in range(w):
            if px[x, y][3] > 0:
                green = px[x, y]
                break
        else:
            continue
        break

    body_y = 9
    for y in range(body_y):
        for x in range(w):
            px[x, y] = TRANSPARENT

    for y in range(3):
        for x in range(6, 12):
            blit(im, x, y, green)
    for y in range(3, body_y):
        for x in range(3, 6):
            blit(im, x, y, green)
    for y in range(3):
        for x in range(13, 16):
            blit(im, x, y, green)
    for y in range(1, 4):
        blit(im, 14, y, green)
        blit(im, 15, y, green)
    return im


def draw_unlock_status(w: int = 18, h: int = 22) -> Image.Image:
    return unlock_status_from_lock(draw_lock_status(w, h))


def draw_eq(w: int = 16, h: int = 14) -> Image.Image:
    img = new_rgba(w, h)
    p = 2
    bars = [(2, 10), (6, 6), (10, 12)]
    for x, bh in bars:
        fill_rect(img, x, h - bh, p, bh, BLUE)
    return img


def draw_diamond(size: int = 8, color: tuple[int, int, int, int] = GREEN) -> Image.Image:
    img = new_rgba(size, size)
    mid = size // 2
    for y in range(size):
        for x in range(size):
            if abs(x - mid) + abs(y - mid) <= mid:
                blit(img, x, y, color)
    return img


def draw_dock_pomo(tomato_dock: Image.Image | None = None) -> Image.Image:
    if tomato_dock is not None:
        return tomato_dock.copy()
    img = new_rgba(28, 28)
    sub = draw_tomato(24).resize((24, 24), Image.NEAREST)
    paste_center(img, sub)
    return img


def is_green_icon(r: int, g: int, b: int) -> bool:
    return g > 120 and r > 60 and b < 120 and (g - b) > 30


def status_icon_from_reference(
    ref_path: Path,
    is_foreground,
    fallback,
    width: int,
    height: int,
) -> Image.Image:
    if not ref_path.is_file():
        return fallback()

    im = Image.open(ref_path).convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if is_foreground(r, g, b):
                continue
            px[x, y] = TRANSPARENT

    minx, miny, maxx, maxy = w, h, 0, 0
    for y in range(h):
        for x in range(w):
            if px[x, y][3] > 0:
                minx = min(minx, x)
                miny = min(miny, y)
                maxx = max(maxx, x)
                maxy = max(maxy, y)
    pad = 1
    im = im.crop((max(0, minx - pad), max(0, miny - pad), min(w, maxx + pad + 1), min(h, maxy + pad + 1)))
    return im.resize((width, height), Image.NEAREST)


def lock_status_from_reference(width: int = 18, height: int = 22) -> Image.Image:
    return status_icon_from_reference(REFERENCE_ICON_LOCK, is_green_icon, lambda: draw_lock_status(width, height), width, height)


def unlock_status_from_reference(width: int = 18, height: int = 22) -> Image.Image:
    return unlock_status_from_lock(lock_status_from_reference(width, height))


def wifi_status_from_reference(width: int = 24, height: int = 20) -> Image.Image:
    return status_icon_from_reference(REFERENCE_ICON_WIFI, is_green_icon, lambda: draw_wifi(width, height), width, height)


def dock_icon_from_reference(
    ref_path: Path,
    is_foreground,
    fallback,
    dock_size: int = 28,
) -> Image.Image:
    if not ref_path.is_file():
        return fallback()

    im = Image.open(ref_path).convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if is_foreground(r, g, b):
                continue
            px[x, y] = TRANSPARENT

    minx, miny, maxx, maxy = w, h, 0, 0
    for y in range(h):
        for x in range(w):
            if px[x, y][3] > 0:
                minx = min(minx, x)
                miny = min(miny, y)
                maxx = max(maxx, x)
                maxy = max(maxy, y)
    pad = 2
    im = im.crop((max(0, minx - pad), max(0, miny - pad), min(w, maxx + pad + 1), min(h, maxy + pad + 1)))

    side = max(im.width, im.height)
    square = new_rgba(side, side)
    paste_center(square, im)
    return square.resize((dock_size, dock_size), Image.NEAREST)


def is_cyan_icon(r: int, g: int, b: int) -> bool:
    return b > 100 and g > 70 and (b - r) > 25


def is_orange_icon(r: int, g: int, b: int) -> bool:
    return r > 170 and g > 120 and b < 170


def lyrics_from_reference(dock_size: int = 28) -> Image.Image:
    """Transparent dock lyrics icon from reference art."""
    return dock_icon_from_reference(REFERENCE_DOCK_LYRICS, is_cyan_icon, draw_dock_lyrics, dock_size)


def sleep_from_reference(dock_size: int = 28) -> Image.Image:
    return dock_icon_from_reference(REFERENCE_DOCK_SLEEP, is_orange_icon, draw_dock_sleep, dock_size)


def lock_from_reference(dock_size: int = 28) -> Image.Image:
    return dock_icon_from_reference(REFERENCE_DOCK_LOCK, is_cyan_icon, draw_dock_lock, dock_size)


def settings_from_reference(dock_size: int = 28) -> Image.Image:
    def is_settings(r: int, g: int, b: int) -> bool:
        return b > 90 and g > 50 and (b - r) > 20 and (r + g + b) > 80

    return dock_icon_from_reference(REFERENCE_DOCK_SETTINGS, is_settings, draw_dock_settings, dock_size)


def draw_dock_home() -> Image.Image:
    """Dual-card home: left green + right blue panes with jagged lavender roof steps."""
    img = new_rgba(28, 28)
    fill_rect(img, 3, 12, 10, 12, GREEN)
    fill_rect(img, 3, 10, 3, 2, LAVENDER)
    fill_rect(img, 5, 8, 3, 2, LAVENDER)
    fill_rect(img, 7, 6, 6, 2, LAVENDER)
    fill_rect(img, 15, 12, 10, 12, BLUE)
    fill_rect(img, 15, 10, 3, 2, LAVENDER)
    fill_rect(img, 17, 8, 3, 2, LAVENDER)
    fill_rect(img, 19, 6, 6, 2, LAVENDER)
    for y in range(12, 24):
        blit(img, 13, y, LAVENDER)
    return img


def draw_dock_lyrics() -> Image.Image:
    img = new_rgba(28, 28)
    p = 2
    fill_rect(img, 6, 14, 3, 10, BLUE)
    fill_rect(img, 12, 10, 3, 14, BLUE)
    fill_rect(img, 18, 16, 3, 8, BLUE)
    fill_rect(img, 16, 6, 3, 8, WHITE)
    fill_rect(img, 19, 6, 6, 3, WHITE)
    return img


def draw_dock_sleep() -> Image.Image:
    img = new_rgba(28, 28)
    p = 2
    # Crescent moon
    for y in range(6, 22):
        for x in range(8, 22):
            dx, dy = x - 14, y - 14
            if 36 <= dx * dx + dy * dy <= 64 and x > 12:
                blit(img, x, y, ORANGE)
    fill_rect(img, 16, 12, 4, 4, NIGHT)
    blit(img, 20, 8, WHITE)
    blit(img, 22, 12, WHITE)
    return img


def draw_dock_lock() -> Image.Image:
    img = new_rgba(24, 24)
    p = 2
    fill_rect(img, 8, 4, 8, 2, CYAN)
    fill_rect(img, 7, 6, 2, 4, CYAN)
    fill_rect(img, 15, 6, 2, 4, CYAN)
    fill_rect(img, 6, 10, 12, 10, CYAN)
    fill_rect(img, 10, 13, 4, 4, TRANSPARENT)
    return img


def draw_dock_settings() -> Image.Image:
    img = new_rgba(24, 24)
    cx, cy = 12, 12
    draw = ImageDraw.Draw(img)
    draw.ellipse((cx - 7, cy - 7, cx + 7, cy + 7), outline=CYAN, width=2)
    for i in range(8):
        ang = i * 45
        import math

        rad = math.radians(ang)
        x0 = cx + int(math.cos(rad) * 5)
        y0 = cy + int(math.sin(rad) * 5)
        x1 = cx + int(math.cos(rad) * 9)
        y1 = cy + int(math.sin(rad) * 9)
        draw.line((x0, y0, x1, y1), fill=CYAN, width=2)
    fill_rect(img, cx - 3, cy - 3, 6, 6, CYAN)
    fill_rect(img, cx - 1, cy - 1, 2, 2, TRANSPARENT)
    return img


def save_asset(name: str, image: Image.Image) -> None:
    path = ASSETS / name
    image.save(path)
    EMBED_FILES.append((name, path.stem))


def png_to_lvgl_c(png_path: Path, var: str) -> str:
    im = Image.open(png_path).convert("RGBA")
    w, h = im.size
    px = im.load()
    rows: list[str] = []
    for y in range(h):
        chunk: list[str] = []
        for x in range(w):
            r, g, b, a = px[x, y]
            chunk.append(f"0x{b:02x}, 0x{g:02x}, 0x{r:02x}, 0x{a:02x}")
        rows.append("    " + ", ".join(chunk))
    data = ",\n".join(rows)
    stride = w * 4
    return f"""#include "lvgl.h"

static const uint8_t {var}_map[] = {{
{data}
}};

const lv_image_dsc_t {var} = {{
    .header = {{
        .magic = LV_IMAGE_HEADER_MAGIC,
        .cf = LV_COLOR_FORMAT_ARGB8888,
        .flags = 0,
        .w = {w},
        .h = {h},
        .stride = {stride},
    }},
    .data_size = sizeof({var}_map),
    .data = {var}_map,
}};
"""


SEED_RELATIVE_PATHS = [
    "assets/tomato.png",
    "assets/icon_wifi.png",
    "assets/icon_lock.png",
    "assets/icon_unlock.png",
    "assets/icon_eq.png",
    "assets/deco_diamond.png",
    "assets/deco_diamond_blue.png",
    "assets/dock_pomo.png",
    "assets/dock_home.png",
    "assets/dock_lyrics.png",
    "assets/dock_lock.png",
    "assets/dock_settings.png",
    "lyrics/current.meta",
    "lyrics/current.txt",
]


def c_var_name(relative_path: str) -> str:
    return "k_seed_" + relative_path.replace("/", "_").replace(".", "_")


def write_sdcard_seed_manifest() -> None:
    entries: list[str] = []
    for rel in SEED_RELATIVE_PATHS:
        src = ROOT / rel
        if not src.is_file():
            raise FileNotFoundError(f"Missing seed asset: {src}")
        data = src.read_bytes()
        var = c_var_name(rel)
        hex_lines = []
        for i in range(0, len(data), 16):
            chunk = data[i : i + 16]
            hex_lines.append("    " + ", ".join(f"0x{b:02x}" for b in chunk))
        body = ",\n".join(hex_lines)
        (EMBED_DIR / f"{var}.c").write_text(
            f"""#include <stddef.h>
#include <stdint.h>

const uint8_t {var}[] = {{
{body}
}};

const size_t {var}_len = sizeof({var});
""",
            encoding="utf-8",
        )
        entries.append(f'    {{"{rel}", {var}, {var}_len}},')

    decls = "\n".join(
        f"extern const uint8_t {c_var_name(rel)}[];\nextern const size_t {c_var_name(rel)}_len;"
        for rel in SEED_RELATIVE_PATHS
    )
    table = "\n".join(entries)
    (EMBED_DIR / "assets_seed.h").write_text(
        f"""#pragma once

#include <stddef.h>
#include <stdint.h>

#include "esp_err.h"

{decls}

size_t assets_seed_expected_len(const char *relative_path);
esp_err_t assets_seed_sdcard(void);
""",
        encoding="utf-8",
    )
    (EMBED_DIR / "assets_seed.cpp").write_text(
        f"""#include "assets_seed.h"

#include "panel_config.h"
#include "sd_storage.h"

#include <cerrno>
#include <cstdio>
#include <cstring>

#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "sys/stat.h"
#include "unistd.h"

namespace {{
constexpr char TAG[] = "assets_seed";
constexpr int kWriteRetries = 4;
constexpr int kRetryDelayMs = 40;

struct SeedEntry {{
  const char *relative_path;
  const uint8_t *data;
  size_t data_len;
}};

const SeedEntry kSeedFiles[] = {{
{table}
}};

bool ensure_dir(const char *path) {{
  struct stat st = {{}};
  if (stat(path, &st) == 0 && S_ISDIR(st.st_mode)) {{
    return true;
  }}
  if (mkdir(path, 0755) == 0) {{
    return true;
  }}
  return stat(path, &st) == 0 && S_ISDIR(st.st_mode);
}}

bool seed_file_size_ok(const char *full_path, size_t expected_len) {{
  struct stat st = {{}};
  if (stat(full_path, &st) != 0 || !S_ISREG(st.st_mode)) {{
    return false;
  }}
  return static_cast<size_t>(st.st_size) == expected_len;
}}

bool write_seed_file(const char *full_path, const uint8_t *data, size_t data_len) {{
  for (int attempt = 0; attempt < kWriteRetries; ++attempt) {{
    if (attempt > 0) {{
      vTaskDelay(pdMS_TO_TICKS(kRetryDelayMs));
      unlink(full_path);
    }}

    FILE *fp = std::fopen(full_path, "wb");
    if (fp == nullptr) {{
      ESP_LOGW(TAG, "fopen %s failed (errno=%d %s), attempt %d/%d", full_path, errno, std::strerror(errno),
               attempt + 1, kWriteRetries);
      continue;
    }}

    const size_t n = std::fwrite(data, 1, data_len, fp);
    std::fflush(fp);
    const int fd = fileno(fp);
    if (fd >= 0) {{
      fsync(fd);
    }}
    std::fclose(fp);

    if (n != data_len) {{
      ESP_LOGW(TAG, "Short write %s (%u/%u), attempt %d/%d", full_path, static_cast<unsigned>(n),
               static_cast<unsigned>(data_len), attempt + 1, kWriteRetries);
      continue;
    }}
    if (!seed_file_size_ok(full_path, data_len)) {{
      ESP_LOGW(TAG, "Size verify failed %s, attempt %d/%d", full_path, attempt + 1, kWriteRetries);
      continue;
    }}
    return true;
  }}
  return false;
}}
}}  // namespace

size_t assets_seed_expected_len(const char *relative_path) {{
  if (relative_path == nullptr) {{
    return 0;
  }}
  for (const auto &entry : kSeedFiles) {{
    if (std::strcmp(entry.relative_path, relative_path) == 0) {{
      return entry.data_len;
    }}
  }}
  return 0;
}}

esp_err_t assets_seed_sdcard() {{
  if (!sd_storage_is_mounted()) {{
    return ESP_ERR_INVALID_STATE;
  }}

  if (!ensure_dir(SD_MOUNT_POINT "/assets") || !ensure_dir(SD_MOUNT_POINT "/lyrics")) {{
    ESP_LOGE(TAG, "Failed to create SD asset directories");
    return ESP_FAIL;
  }}

  int written = 0;
  int failed = 0;
  for (const auto &entry : kSeedFiles) {{
    char full_path[160];
    std::snprintf(full_path, sizeof(full_path), "%s/%s", SD_MOUNT_POINT, entry.relative_path);
    if (seed_file_size_ok(full_path, entry.data_len)) {{
      continue;
    }}

    if (write_seed_file(full_path, entry.data, entry.data_len)) {{
      ++written;
      ESP_LOGI(TAG, "Wrote SD asset: %s (%u bytes)", entry.relative_path,
               static_cast<unsigned>(entry.data_len));
    }} else {{
      ++failed;
      ESP_LOGE(TAG, "Cannot write %s after %d attempts", full_path, kWriteRetries);
    }}

    vTaskDelay(pdMS_TO_TICKS(10));
  }}

  if (written > 0) {{
    ESP_LOGI(TAG, "Seeded %d file(s) to SD card", written);
  }}
  if (failed > 0) {{
    ESP_LOGE(TAG, "Failed to seed %d file(s) on SD card", failed);
    return ESP_FAIL;
  }}
  if (written == 0) {{
    ESP_LOGI(TAG, "SD card assets already present");
  }}
  return ESP_OK;
}}
""",
        encoding="utf-8",
    )


def write_embedded_assets() -> None:
    EMBED_DIR.mkdir(parents=True, exist_ok=True)
    decls: list[str] = []
    sources: list[str] = []
    for fname, stem in EMBED_FILES:
        var = f"ui_img_{stem}"
        png = ASSETS / fname
        c_src = png_to_lvgl_c(png, var)
        c_path = EMBED_DIR / f"{var}.c"
        c_path.write_text(c_src, encoding="utf-8")
        decls.append(f"extern const lv_image_dsc_t {var};")
        sources.append(f'    {{"assets/{fname}", &{var}}},')

    (EMBED_DIR / "ui_embed_images.h").write_text(
        "#pragma once\n\n#include \"lvgl.h\"\n\n"
        + "\n".join(decls)
        + "\n\nconst lv_image_dsc_t *ui_embed_lookup(const char *relative_path);\n",
        encoding="utf-8",
    )

    table = "\n".join(sources)
    (EMBED_DIR / "ui_embed_images.cpp").write_text(
        f"""#include "ui_embed_images.h"

#include <cstring>

namespace {{
struct Entry {{
  const char *path;
  const lv_image_dsc_t *dsc;
}};

const Entry kEntries[] = {{
{table}
}};
}}  // namespace

const lv_image_dsc_t *ui_embed_lookup(const char *relative_path) {{
  if (relative_path == nullptr) {{
    return nullptr;
  }}
  for (const auto &e : kEntries) {{
    if (std::strcmp(e.path, relative_path) == 0) {{
      return e.dsc;
    }}
  }}
  return nullptr;
}}
""",
        encoding="utf-8",
    )


def main() -> int:
    ASSETS.mkdir(parents=True, exist_ok=True)
    LYRICS.mkdir(parents=True, exist_ok=True)

    tomato_card, tomato_dock = tomato_from_reference(card_size=96, dock_size=28)
    save_asset("tomato.png", tomato_card)
    save_asset("dock_pomo.png", draw_dock_pomo(tomato_dock))
    save_asset("dock_home.png", draw_dock_home())
    save_asset("icon_wifi.png", wifi_status_from_reference(24, 20))
    save_asset("icon_lock.png", lock_status_from_reference(18, 22))
    save_asset("icon_unlock.png", unlock_status_from_reference(18, 22))
    save_asset("icon_eq.png", draw_eq())
    save_asset("deco_diamond.png", draw_diamond(8, GREEN))
    save_asset("deco_diamond_blue.png", draw_diamond(8, BLUE))
    save_asset("dock_lyrics.png", lyrics_from_reference(28))
    save_asset("dock_lock.png", lock_from_reference(28))
    save_asset("dock_settings.png", settings_from_reference(28))

    (LYRICS / "current.meta").write_text("夜空中最亮的星", encoding="utf-8")
    (LYRICS / "current.txt").write_text(
        "能否听清\n"
        "那仰望的人\n"
        "心底的孤独和叹息\n"
        "夜空中最亮的星\n"
        "能否记起\n"
        "曾与我同行\n"
        "消失在风里的身影\n"
        "...",
        encoding="utf-8",
    )

    write_embedded_assets()
    write_sdcard_seed_manifest()
    print(f"Assets written to {ROOT}")
    print(f"Embedded C sources written to {EMBED_DIR}")
    print("On boot: missing files are auto-written to /sdcard (assets/ + lyrics/).")
    print("Optional: copy sdcard_assets/assets/ and lyrics/ to TF card root manually.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
