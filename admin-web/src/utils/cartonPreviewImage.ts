import { uploadCartonPreviewImage } from '@/api/ecommerce/image'
import { updateCartonPreviewImage } from '@/api/ecommerce/carton'
import { renderCartonCssToPngBlob } from '@/utils/renderCartonCssToPngBlob'
import { renderCarton3DToPngBlob } from '@/utils/renderCarton3DToPng'

export interface CartonPreviewSource {
  id?: number | null
  name: string
  lengthCm?: number | null
  widthCm?: number | null
  heightCm?: number | null
  illustrationVariant?: number | null
}

export function canGenerateCartonPreview(carton: Pick<CartonPreviewSource, 'lengthCm' | 'widthCm' | 'heightCm'>) {
  const lengthCm = Number(carton.lengthCm) || 0
  const widthCm = Number(carton.widthCm) || 0
  const heightCm = Number(carton.heightCm) || 0
  return lengthCm > 0 && widthCm > 0 && heightCm > 0
}

export async function generateCartonPreviewBlob(carton: CartonPreviewSource) {
  if (!canGenerateCartonPreview(carton)) return null

  const options = {
    lengthCm: Number(carton.lengthCm),
    widthCm: Number(carton.widthCm),
    heightCm: Number(carton.heightCm),
    illustrationVariant: carton.illustrationVariant,
    seed: carton.id ?? undefined,
    backgroundColor: '#ffffff',
  }

  try {
    const threeBlob = await renderCarton3DToPngBlob(options)
    if (threeBlob && threeBlob.size > 0) return threeBlob
  } catch (error) {
    console.warn('[carton-preview] Three.js 渲染失败，改用 CSS 3D 截图', error)
  }

  const cssBlob = await renderCartonCssToPngBlob(options)
  if (!cssBlob || cssBlob.size === 0) {
    throw new Error('预览图渲染结果为空')
  }
  return cssBlob
}

function toPreviewFile(blob: Blob, cartonName: string) {
  return new File([blob], `${cartonName.trim()}-预览.png`, { type: 'image/png' })
}

/** 生成预览图并双写存储（本地 + 网盘），返回文件名 */
export async function storeCartonPreviewImage(carton: CartonPreviewSource): Promise<string | null> {
  const name = carton.name?.trim()
  if (!name || !canGenerateCartonPreview(carton)) return null

  const blob = await generateCartonPreviewBlob(carton)
  if (!blob) return null

  const file = toPreviewFile(blob, name)
  if (carton.id) {
    const updated = await updateCartonPreviewImage(carton.id, file, name)
    return updated.previewImage ?? null
  }
  return uploadCartonPreviewImage(file, name)
}
