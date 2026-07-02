/** 手绘圆角矩形边框 path（viewBox 0 0 100 100）
 * 左右竖边保持恒定 x，避免宽卡片拉伸后竖线倾斜 */
export const DOODLE_RECT_PATH =
  'M 22 8.5 C 14 7 6 11 5.5 19 L 5.5 81 C 5 89 12 94 20 93.5 L 80 93.5 C 88 94.5 95 89 94.5 81 L 94.5 19 C 95 11 88 7 80 8 L 22 8.5 Z'

/** 更明显的手绘抖动变体，用于小卡片等需要素描感的场景 */
export const DOODLE_RECT_VARIANTS = [
  'M 13.8 9.1 C 7.2 7.5 3.8 11.9 4.9 17.8 L 3.5 80.2 C 2.6 88.5 8.9 94.8 17.2 93.1 L 81.5 95.8 C 89.8 97.2 96.1 90.8 94.8 82.9 L 95.9 19.1 C 97.2 11.2 90.5 4.9 82.1 6.8 L 13.8 9.1 Z',
  'M 18 7 C 9 5.5 4.5 10 5 18.5 L 4 79 C 3.5 87.5 10 95 19 94 L 78 95.5 C 87 96.5 96 90 95 80.5 L 96 20 C 96.5 10.5 89 6 79 7.5 L 18 7 Z',
  'M 20 9.5 C 11 6.5 5.5 12 6 21 L 5.5 82 C 5 91 13 96.5 22 95 L 82 94 C 91 92.5 97 86 96.5 77 L 97 18 C 97.5 9 90 5 81 6.5 L 20 9.5 Z',
  DOODLE_RECT_PATH,
] as const

export function resolveDoodleRectPath(seed?: number): string {
  if (seed == null) return DOODLE_RECT_PATH
  const idx = Math.abs(seed) % DOODLE_RECT_VARIANTS.length
  return DOODLE_RECT_VARIANTS[idx]!
}

/** 手绘胶囊形边框 path（viewBox 0 0 200 48）
 * 上下直边保持恒定 y，左右为圆弧 */
export const DOODLE_PILL_PATH =
  'M 24 7.5 C 12 6 5 12 5.5 24 C 5 35 11 41.5 22 41.5 L 178 41.5 C 189 41.5 195 35 194.5 24 C 195 12 189 6 178 7.5 L 24 7.5 Z'

export type DoodleFrameShape = 'rect' | 'pill'

export const DOODLE_FRAME_PATHS: Record<DoodleFrameShape, string> = {
  rect: DOODLE_RECT_PATH,
  pill: DOODLE_PILL_PATH,
}

export const DOODLE_FRAME_VIEWBOX: Record<DoodleFrameShape, string> = {
  rect: '0 0 100 100',
  pill: '0 0 200 48',
}
