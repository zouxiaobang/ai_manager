import { deleteData, getData, postData, putData } from '../request'
import type { PageQuery, PageResult } from '../pagination'

export interface EcOutboundOrderLine {
  id?: number
  skuCode: string
  specName?: string
  productName?: string
  quantity: number
  shippedQuantity?: number | null
}

export interface EcOutboundOrder {
  id: number
  orderNo: string
  factoryId?: number | null
  factoryName?: string
  customerFactoryId?: number | null
  customerName?: string
  customerContactName?: string
  customerContactPhone?: string
  customerAddress?: string
  status: 'DRAFT' | 'CONFIRMED' | 'CANCELLED'
  remark?: string
  orderTime?: string
  expectedShipTime?: string
  actualShipTime?: string | null
  createTime?: string
  updateTime?: string
  lines?: EcOutboundOrderLine[]
}

export interface EcOutboundOrderSaveRequest {
  factoryId?: number | null
  customerFactoryId?: number | null
  remark?: string
  orderTime: string
  expectedShipTime: string
  lines: Array<{ skuCode: string; quantity: number }>
}

export interface EcOutboundOrderConfirmLineItem {
  lineId: number
  shippedQuantity: number
}

export interface EcOutboundOrderConfirmRequest {
  lines: EcOutboundOrderConfirmLineItem[]
}

export function fetchOutboundOrders(
  keyword?: string,
  status?: string,
  factoryId?: number,
  pageQuery?: PageQuery,
) {
  return getData<PageResult<EcOutboundOrder>>('/api/ecommerce/outbound-orders', {
    ...(keyword ? { keyword } : {}),
    ...(status ? { status } : {}),
    ...(factoryId ? { factoryId } : {}),
    ...(pageQuery ?? {}),
  })
}

export function fetchOutboundOrder(id: number) {
  return getData<EcOutboundOrder>(`/api/ecommerce/outbound-orders/${id}`)
}

export function createOutboundOrder(data: EcOutboundOrderSaveRequest) {
  return postData<EcOutboundOrder>('/api/ecommerce/outbound-orders', data)
}

export function updateOutboundOrder(id: number, data: EcOutboundOrderSaveRequest) {
  return putData<EcOutboundOrder>(`/api/ecommerce/outbound-orders/${id}`, data)
}

export function confirmOutboundOrder(id: number, data: EcOutboundOrderConfirmRequest) {
  return postData<EcOutboundOrder>(`/api/ecommerce/outbound-orders/${id}/confirm`, data)
}

export function cancelOutboundOrder(id: number) {
  return postData<void>(`/api/ecommerce/outbound-orders/${id}/cancel`)
}

export function deleteOutboundOrder(id: number) {
  return deleteData(`/api/ecommerce/outbound-orders/${id}`)
}
