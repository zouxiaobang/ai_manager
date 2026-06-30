import request, { deleteData, getData, postData, putData } from '../request'
import type { ApiResult } from '../types'
import type { PageQuery, PageResult } from '../pagination'

export interface EcCarton {
  id: number
  factoryId?: number | null
  factoryName?: string
  name: string
  lengthCm?: number | null
  widthCm?: number | null
  heightCm?: number | null
  unitPrice?: number | null
  remark?: string
  /** 纸箱材质 0~3（牛皮/白卡/瓦楞/普通快递盒） */
  illustrationVariant?: number | null
  previewImage?: string | null
  updateTime?: string
}

export interface EcCartonCalculateResult {
  matchedCarton: EcCarton | null
  inventoryCarton: EcCarton | null
}

export interface EcCartonBackfillTask {
  taskId: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
  total: number
  processed: number
  updated: number
  message?: string
}

export interface EcCartonSaveRequest {
  name: string
  factoryId: number
  lengthCm?: number | null
  widthCm?: number | null
  heightCm?: number | null
  unitPrice?: number | null
  remark?: string
  illustrationVariant?: number | null
  previewImage?: string | null
}

export function fetchCartons(keyword?: string, pageQuery?: PageQuery) {
  return getData<PageResult<EcCarton>>('/api/ecommerce/cartons', {
    ...(keyword ? { keyword } : {}),
    ...(pageQuery ?? {}),
  })
}

/** 纸箱数量不多，一次拉取全部列表（按体积降序，后端排序） */
export function fetchAllCartons(keyword?: string) {
  return fetchCartons(keyword, { page: 1, pageSize: 100 })
}

export function fetchCarton(id: number) {
  return getData<EcCarton>(`/api/ecommerce/cartons/${id}`)
}

export function createCarton(data: EcCartonSaveRequest) {
  return postData<EcCarton>('/api/ecommerce/cartons', data)
}

export function updateCarton(id: number, data: EcCartonSaveRequest) {
  return putData<EcCarton>(`/api/ecommerce/cartons/${id}`, data)
}

export async function updateCartonPreviewImage(id: number, file: File, cartonName?: string) {
  const formData = new FormData()
  formData.append('file', file)
  if (cartonName?.trim()) {
    formData.append('cartonName', cartonName.trim())
  }
  const response = await request.put<ApiResult<EcCarton>>(`/api/ecommerce/cartons/${id}/preview-image`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
  return response.data.data
}

export function deleteCarton(id: number) {
  return deleteData(`/api/ecommerce/cartons/${id}`)
}

export function calculateCarton(
  lengthCm: number,
  widthCm: number,
  heightCm: number,
  factoryId?: number | null,
) {
  return getData<EcCartonCalculateResult>('/api/ecommerce/cartons/calculate', {
    lengthCm,
    widthCm,
    heightCm,
    ...(factoryId ? { factoryId } : {}),
  })
}

export function startBackfillSkuCartonsAsync() {
  return postData<string>('/api/ecommerce/cartons/backfill-sku-cartons/async')
}

export function fetchBackfillTask(taskId: string) {
  return getData<EcCartonBackfillTask>(`/api/ecommerce/cartons/backfill-sku-cartons/tasks/${taskId}`)
}

export function matchCarton(
  lengthCm: number,
  widthCm: number,
  heightCm: number,
  factoryId?: number | null,
) {
  return getData<EcCarton | null>('/api/ecommerce/cartons/match', {
    lengthCm,
    widthCm,
    heightCm,
    ...(factoryId ? { factoryId } : {}),
  })
}
