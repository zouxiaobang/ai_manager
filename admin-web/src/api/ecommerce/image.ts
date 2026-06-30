import request from '../request'
import type { ApiResult } from '../types'

export interface EcImageUploadResult {
  fileName: string
}

export function getEcommerceImageUrl(imageName?: string | null): string {
  if (!imageName?.trim()) return ''
  const base = import.meta.env.VITE_API_BASE || ''
  const encoded = imageName.split('/').map(encodeURIComponent).join('/')
  return `${base}/uploads/ecommerce/${encoded}`
}

export async function uploadEcommerceImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const response = await request.post<ApiResult<EcImageUploadResult>>(
    '/api/ecommerce/images/upload',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return response.data.data.fileName
}

/** 纸箱预览图：按纸箱名命名，本地落盘并双写网盘 */
export async function uploadCartonPreviewImage(file: File, cartonName: string): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('cartonName', cartonName.trim())
  const response = await request.post<ApiResult<EcImageUploadResult>>(
    '/api/ecommerce/images/upload/carton-preview',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' }, timeout: 60000 },
  )
  return response.data.data.fileName
}
