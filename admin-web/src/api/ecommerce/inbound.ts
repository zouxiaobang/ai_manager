import { deleteData, getData, postData, putData } from '../request'
import type { PageQuery, PageResult } from '../pagination'



export interface EcInboundOrderLine {

  id?: number

  skuCode: string

  specName?: string

  productName?: string

  quantity: number

  receivedQuantity?: number | null

}



export interface EcInboundOrder {

  id: number

  orderNo: string

  factoryId?: number | null

  factoryName?: string

  status: 'DRAFT' | 'CONFIRMED' | 'CANCELLED'

  remark?: string

  orderTime?: string

  expectedDeliveryTime?: string

  actualReceiptTime?: string | null

  createTime?: string

  updateTime?: string

  lines?: EcInboundOrderLine[]

}



export interface EcInboundOrderSaveRequest {

  factoryId?: number | null

  remark?: string

  orderTime: string

  expectedDeliveryTime: string

  lines: Array<{ skuCode: string; quantity: number }>

}



export interface EcInboundOrderConfirmLineItem {

  lineId: number

  receivedQuantity: number

}



export interface EcInboundOrderConfirmRequest {

  lines: EcInboundOrderConfirmLineItem[]

}



export function fetchInboundOrders(
  keyword?: string,
  status?: string,
  factoryId?: number,
  pageQuery?: PageQuery,
) {
  return getData<PageResult<EcInboundOrder>>('/api/ecommerce/inbound-orders', {
    ...(keyword ? { keyword } : {}),
    ...(status ? { status } : {}),
    ...(factoryId ? { factoryId } : {}),
    ...(pageQuery ?? {}),
  })
}



export function fetchInboundOrder(id: number) {

  return getData<EcInboundOrder>(`/api/ecommerce/inbound-orders/${id}`)

}



export function createInboundOrder(data: EcInboundOrderSaveRequest) {

  return postData<EcInboundOrder>('/api/ecommerce/inbound-orders', data)

}



export function updateInboundOrder(id: number, data: EcInboundOrderSaveRequest) {

  return putData<EcInboundOrder>(`/api/ecommerce/inbound-orders/${id}`, data)

}



export function confirmInboundOrder(id: number, data: EcInboundOrderConfirmRequest) {

  return postData<EcInboundOrder>(`/api/ecommerce/inbound-orders/${id}/confirm`, data)

}



export function cancelInboundOrder(id: number) {

  return postData<void>(`/api/ecommerce/inbound-orders/${id}/cancel`)

}



export function deleteInboundOrder(id: number) {

  return deleteData(`/api/ecommerce/inbound-orders/${id}`)

}

