import illus1 from '@/assets/ecommerce/carton-illus-1.png'
import illus2 from '@/assets/ecommerce/carton-illus-2.png'
import illus3 from '@/assets/ecommerce/carton-illus-3.png'
import illus4 from '@/assets/ecommerce/carton-illus-4.png'

export const CARTON_ILLUSTRATIONS = [illus1, illus2, illus3, illus4] as const

export const CARTON_ILLUSTRATION_COUNT = CARTON_ILLUSTRATIONS.length

export function hashCartonSeed(key: string) {
  let hash = 0
  for (let i = 0; i < key.length; i++) {
    hash = (hash * 31 + key.charCodeAt(i)) | 0
  }
  return Math.abs(hash)
}

export function resolveCartonIllustrationVariant(
  explicit: number | null | undefined,
  seed?: number | string,
) {
  if (
    explicit != null &&
    Number.isInteger(explicit) &&
    explicit >= 0 &&
    explicit < CARTON_ILLUSTRATION_COUNT
  ) {
    return explicit
  }
  return hashCartonSeed(String(seed ?? 'default')) % CARTON_ILLUSTRATION_COUNT
}

export function cartonIllustrationUrl(
  explicit: number | null | undefined,
  seed?: number | string,
) {
  return CARTON_ILLUSTRATIONS[resolveCartonIllustrationVariant(explicit, seed)]
}
