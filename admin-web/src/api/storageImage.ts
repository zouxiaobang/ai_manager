import { getData } from './request'
import type { PageResult } from './pagination'

export type StorageImageZone = 'ECOMMERCE_IMAGES' | 'NOTEBOOK_IMAGES'

export interface StorageImageItem {
  zone: StorageImageZone
  fileName: string
  relativePath: string
  sizeBytes: number
  modifiedAt?: string
}

export function getStorageImageUrl(zone: StorageImageZone, pathOrName: string): string {
  if (!pathOrName?.trim()) return ''
  const base = import.meta.env.VITE_API_BASE || ''
  const encoded = pathOrName.split('/').map(encodeURIComponent).join('/')
  if (zone === 'NOTEBOOK_IMAGES') {
    return `${base}/uploads/notebook/images/${encoded}`
  }
  return `${base}/uploads/ecommerce/${encoded}`
}

export function resolveStorageImageValue(item: StorageImageItem): string {
  return item.relativePath?.trim() || item.fileName
}

export async function browseProjectImages(params: {
  zone: StorageImageZone
  keyword?: string
  page?: number
  pageSize?: number
}): Promise<PageResult<StorageImageItem>> {
  const search = new URLSearchParams()
  search.set('zone', params.zone)
  if (params.keyword?.trim()) search.set('keyword', params.keyword.trim())
  search.set('page', String(params.page ?? 1))
  search.set('pageSize', String(params.pageSize ?? 10))
  return getData<PageResult<StorageImageItem>>(
    `/api/storage-center/images?${search.toString()}`,
  )
}
