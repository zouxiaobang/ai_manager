import kraftTexture from '@/assets/ecommerce/materials/carton-material-kraft.jpg'
import whiteTexture from '@/assets/ecommerce/materials/carton-material-white.jpg'
import corrugatedTexture from '@/assets/ecommerce/materials/carton-material-corrugated.jpg'
import expressTexture from '@/assets/ecommerce/materials/carton-material-express.svg'

export type CartonFaceKey = 'front' | 'back' | 'left' | 'right' | 'top' | 'bottom'

export interface CartonMaterialDef {
  id: number
  className: string
  nameKey: string
  subKey: string
  textureUrl: string
  tapeOnTop: boolean
}

/** 0 牛皮 / 1 白卡 / 2 瓦楞 / 3 普通快递盒 —— 与 illustrationVariant 一一对应 */
export const CARTON_MATERIALS: readonly CartonMaterialDef[] = [
  {
    id: 0,
    className: 'material-kraft',
    nameKey: 'ecommerce.carton.materialKraft',
    subKey: 'ecommerce.carton.materialKraftSub',
    textureUrl: kraftTexture,
    tapeOnTop: true,
  },
  {
    id: 1,
    className: 'material-white',
    nameKey: 'ecommerce.carton.materialWhite',
    subKey: 'ecommerce.carton.materialWhiteSub',
    textureUrl: whiteTexture,
    tapeOnTop: false,
  },
  {
    id: 2,
    className: 'material-corrugated',
    nameKey: 'ecommerce.carton.materialCorrugated',
    subKey: 'ecommerce.carton.materialCorrugatedSub',
    textureUrl: corrugatedTexture,
    tapeOnTop: false,
  },
  {
    id: 3,
    className: 'material-express',
    nameKey: 'ecommerce.carton.materialExpress',
    subKey: 'ecommerce.carton.materialExpressSub',
    textureUrl: expressTexture,
    tapeOnTop: true,
  },
] as const

export const CARTON_MATERIAL_COUNT = CARTON_MATERIALS.length
/** 默认材质：普通纸箱（黄色快递盒） */
export const DEFAULT_CARTON_MATERIAL_VARIANT = 3

export function normalizeCartonMaterialVariant(value: number | null | undefined) {
  if (value == null || !Number.isInteger(value)) return null
  if (value >= 0 && value < CARTON_MATERIAL_COUNT) return value
  return null
}

export function resolveCartonMaterialVariant(
  explicit: number | null | undefined,
  _seed?: number | string,
) {
  const normalized = normalizeCartonMaterialVariant(explicit)
  if (normalized != null) return normalized

  return DEFAULT_CARTON_MATERIAL_VARIANT
}

export function resolveCartonMaterial(
  explicit: number | null | undefined,
  seed?: number | string,
): CartonMaterialDef {
  const index = resolveCartonMaterialVariant(explicit, seed)
  return CARTON_MATERIALS[index] ?? CARTON_MATERIALS[DEFAULT_CARTON_MATERIAL_VARIANT]
}

export function isValidCartonMaterialId(value: number | null | undefined) {
  return normalizeCartonMaterialVariant(value) != null
}
