import { deleteData, getData, putData } from './request'
import type { PageResult } from './pagination'
import type { StorageImageZone } from './storageImage'

export interface ImageSpaceCategoryNode {
  id: string
  label: string
  spuId?: number | null
  children?: ImageSpaceCategoryNode[]
}

export interface ImageSpaceImageItem {
  zone: StorageImageZone
  fileName: string
  relativePath: string
  sizeBytes: number
  modifiedAt?: string
  referenceCount: number
  linkedSpuNames: string[]
}

export interface ImageSpaceImageDetail extends ImageSpaceImageItem {
  referenceHints: string[]
}

export interface ImageSpaceNameCheck {
  available: boolean
  message: string
}

export async function fetchImageSpaceCategories(): Promise<ImageSpaceCategoryNode[]> {
  return getData<ImageSpaceCategoryNode[]>('/api/image-space/categories')
}

export async function fetchImageSpaceImages(params: {
  zone: StorageImageZone
  categoryId: string
  keyword?: string
  page?: number
  pageSize?: number
}): Promise<PageResult<ImageSpaceImageItem>> {
  const search = new URLSearchParams()
  search.set('zone', params.zone)
  search.set('categoryId', params.categoryId)
  if (params.keyword?.trim()) search.set('keyword', params.keyword.trim())
  search.set('page', String(params.page ?? 1))
  search.set('pageSize', String(params.pageSize ?? 10))
  return getData<PageResult<ImageSpaceImageItem>>(`/api/image-space/images?${search.toString()}`)
}

export async function fetchImageSpaceDetail(
  zone: StorageImageZone,
  relativePath: string,
): Promise<ImageSpaceImageDetail> {
  const search = new URLSearchParams({ zone, relativePath })
  return getData<ImageSpaceImageDetail>(`/api/image-space/images/detail?${search.toString()}`)
}

export async function checkImageSpaceFileName(
  zone: StorageImageZone,
  relativePath: string,
  newFileName: string,
): Promise<ImageSpaceNameCheck> {
  const search = new URLSearchParams({ zone, relativePath, newFileName })
  return getData<ImageSpaceNameCheck>(`/api/image-space/images/check-name?${search.toString()}`)
}

export async function renameImageSpaceFile(payload: {
  zone: StorageImageZone
  relativePath: string
  newFileName: string
}): Promise<ImageSpaceImageDetail> {
  return putData<ImageSpaceImageDetail>('/api/image-space/images/rename', payload)
}

export async function deleteImageSpaceFile(
  zone: StorageImageZone,
  relativePath: string,
): Promise<void> {
  const search = new URLSearchParams({ zone, relativePath })
  return deleteData(`/api/image-space/images?${search.toString()}`)
}
