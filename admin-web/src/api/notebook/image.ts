import request from '../request'
import type { ApiResult } from '../types'

export interface NotebookImageUploadResult {
  fileName: string
}

export function getNotebookImageUrl(imageName?: string | null): string {
  if (!imageName?.trim()) return ''
  const base = import.meta.env.VITE_API_BASE || ''
  const encoded = imageName.split('/').map(encodeURIComponent).join('/')
  return `${base}/uploads/notebook/images/${encoded}`
}

export async function uploadNotebookImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const response = await request.post<ApiResult<NotebookImageUploadResult>>(
    '/api/notebook/images/upload',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return response.data.data.fileName
}
