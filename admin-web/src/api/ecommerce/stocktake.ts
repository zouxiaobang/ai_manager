import { deleteData, getData, postData, putData } from '../request'
import type { PageQuery, PageResult } from '../pagination'

export interface EcStocktakeOrderLine {
  id?: number
  skuCode: string
  specName?: string
  productName?: string
  bookQuantity: number
  actualQuantity?: number | null
}

export interface EcStocktakeOrder {
  id: number
  orderNo: string
  factoryId?: number | null
  factoryName?: string
  status: 'DRAFT' | 'CONFIRMED' | 'CANCELLED'
  remark?: string
  stocktakeTime?: string
  createTime?: string
  updateTime?: string
  lines?: EcStocktakeOrderLine[]
}

export interface EcStocktakeOrderSaveRequest {
  factoryId?: number | null
  remark?: string
  stocktakeTime: string
  lines: Array<{ skuCode: string; actualQuantity?: number | null }>
}

export function fetchStocktakeOrders(
  keyword?: string,
  status?: string,
  factoryId?: number,
  pageQuery?: PageQuery,
) {
  return getData<PageResult<EcStocktakeOrder>>('/api/ecommerce/stocktake-orders', {
    ...(keyword ? { keyword } : {}),
    ...(status ? { status } : {}),
    ...(factoryId ? { factoryId } : {}),
    ...(pageQuery ?? {}),
  })
}

export function fetchStocktakeOrder(id: number) {
  return getData<EcStocktakeOrder>(`/api/ecommerce/stocktake-orders/${id}`)
}

export function createStocktakeOrder(data: EcStocktakeOrderSaveRequest) {
  return postData<EcStocktakeOrder>('/api/ecommerce/stocktake-orders', data)
}

export function updateStocktakeOrder(id: number, data: EcStocktakeOrderSaveRequest) {
  return putData<EcStocktakeOrder>(`/api/ecommerce/stocktake-orders/${id}`, data)
}

export function confirmStocktakeOrder(id: number) {
  return postData<EcStocktakeOrder>(`/api/ecommerce/stocktake-orders/${id}/confirm`)
}

export function cancelStocktakeOrder(id: number) {
  return postData<void>(`/api/ecommerce/stocktake-orders/${id}/cancel`)
}

export function deleteStocktakeOrder(id: number) {
  return deleteData(`/api/ecommerce/stocktake-orders/${id}`)
}
