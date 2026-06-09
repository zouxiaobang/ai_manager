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
