from __future__ import annotations

from collections import deque
from pathlib import Path

from PIL import Image, ImageFilter

SRC = Path(__file__).resolve().parents[1] / 'preview/mobile-home/mobile-home-doodle-scheme1.png'
OUT = Path(__file__).resolve().parents[1] / 'public/mobile-home/scheme-a'

# 手机屏头部：含完整「工作台」顶缘 + 小熊脚底落在搜索框上沿
PHONE_X1 = 478
PHONE_Y1 = 86
PHONE_X2 = 1062
PHONE_TITLE_Y2 = 244
PHONE_BEAR_Y2 = 258
TITLE_SPLIT_RATIO = 0.58
UPSCALE = 3.0


def bg_like(r: int, g: int, b: int, a: int = 255) -> bool:
    if a < 10:
        return True
    if r > 246 and g > 243 and b > 238:
        return True
    return r > 228 and g > 223 and b > 215 and abs(r - g) < 22 and b > r - 25


def flood_transparent(region: Image.Image) -> Image.Image:
    region = region.copy().convert('RGBA')
    rw, rh = region.size
    px = region.load()
    seen = [[False] * rw for _ in range(rh)]
    q: deque[tuple[int, int]] = deque()

    for x in range(rw):
        for y in (0, rh - 1):
            if bg_like(*px[x, y]):
                seen[y][x] = True
                q.append((x, y))
    for y in range(rh):
        for x in (0, rw - 1):
            if not seen[y][x] and bg_like(*px[x, y]):
                seen[y][x] = True
                q.append((x, y))

    while q:
        x, y = q.popleft()
        px[x, y] = (px[x, y][0], px[x, y][1], px[x, y][2], 0)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < rw and 0 <= ny < rh and not seen[ny][nx] and bg_like(*px[nx, ny]):
                seen[ny][nx] = True
                q.append((nx, ny))
    return region


def upscale(im: Image.Image, scale: float = UPSCALE) -> Image.Image:
    nw, nh = int(im.width * scale), int(im.height * scale)
    up = im.resize((nw, nh), Image.Resampling.LANCZOS)
    return up.filter(ImageFilter.UnsharpMask(radius=1.2, percent=140, threshold=2))


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    img = Image.open(SRC).convert('RGBA')
    base_bear = img.crop((PHONE_X1, PHONE_Y1, PHONE_X2, PHONE_BEAR_Y2))
    base_title = img.crop((PHONE_X1, PHONE_Y1, PHONE_X2, PHONE_TITLE_Y2))
    w_bear, h_bear = base_bear.size
    w_title, h_title = base_title.size

    header = upscale(flood_transparent(base_bear))
    header.save(OUT / 'hero-header.png', optimize=True)

    split = int(w_title * TITLE_SPLIT_RATIO)
    bear_x0 = max(0, split - 20)
    title = upscale(flood_transparent(base_title.crop((0, 0, split, h_title))))
    bear = upscale(flood_transparent(base_bear.crop((bear_x0, 0, w_bear, h_bear))))
    title.save(OUT / 'hero-title-overlay.png', optimize=True)
    bear.save(OUT / 'hero-bear-overlay.png', optimize=True)

    print('exported hero-header.png', header.size)
    print('exported hero-title-overlay.png', title.size)
    print('exported hero-bear-overlay.png', bear.size)


if __name__ == '__main__':
    main()
