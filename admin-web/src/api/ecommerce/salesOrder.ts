import { deleteData, getData, postData, putData } from '../request'
import request from '../request'
import type { ApiResult } from '../types'
import type { PageQuery, PageResult } from '../pagination'

export type EcSalesOrderStatus =
  | 'DRAFT'
  | 'PAID'
  | 'PARTIAL_SHIPPED'
  | 'SHIPPED'
  | 'PARTIAL_REFUND'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REFUNDED'

export type EcSalesOrderLineStatus =
  | 'PAID'
  | 'SHIPPED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REFUNDED'
  | 'RETURNED'

export interface EcSalesOrderLine {
  id?: number
  listingLinkSkuId?: number | null
  linkName?: string
  skuSpecName?: string
  skuCodes?: string
  skuQuantity?: number
  shippedQuantity?: number
  shortQuantity?: number
  status?: EcSalesOrderLineStatus
  refundType?: string
  refundAmount?: number | null
  lossAmount?: number | null
  unitPrice?: number | null
  lineReceivedAmount?: number | null
  costPrice?: number | null
  profit?: number | null
  pricingRisk?: string
  platformLineNo?: string
  shortages?: { skuCode: string; shortQty: number; deductedQty: number }[]
}

export interface EcSalesOrder {
  id: number
  orderNo: string
  shopId: number
  shopName?: string
  platformName?: string
  platformOrderNo?: string
  source?: string
  status: EcSalesOrderStatus
  expressStationId?: number | null
  expressStationName?: string
  orderTime?: string
  payTime?: string
  shipTime?: string
  completeTime?: string
  platformStatus?: string
  receivedAmount?: number | null
  totalCostAmount?: number | null
  actualFreightAmount?: number | null
  estimatedFreightAmount?: number | null
  trackingNumber?: string
  profitAmount?: number | null
  totalLossAmount?: number | null
  hasShortage?: boolean
  buyerName?: string
  receiveProvince?: string
  receiveAddress?: string
  buyerRemark?: string
  sellerRemark?: string
  lines?: EcSalesOrderLine[]
  lineCount?: number
}

export interface EcSalesOrderSaveRequest {
  shopId: number
  platformOrderNo?: string
  expressStationId?: number | null
  orderTime?: string
  payTime?: string
  buyerName?: string
  buyerPhone?: string
  receiveAddress?: string
  trackingNumber?: string
  buyerRemark?: string
  sellerRemark?: string
  receivedAmount?: number | null
  freightAmount?: number | null
  orderCouponAmount?: number | null
  lines: EcSalesOrderLineSaveItem[]
}

export interface EcSalesOrderLineSaveItem {
  listingLinkSkuId?: number | null
  linkName?: string
  skuSpecName?: string
  skuQuantity?: number
  unitPrice?: number | null
  lineReceivedAmount?: number | null
  lineCouponAmount?: number | null
  platformLineNo?: string
  platformItemName?: string
  sortOrder?: number
}

export interface EcSalesOrderImportRow {
  id?: number
  rowNo?: number
  parseStatus?: string
  platformOrderNo?: string
  linkName?: string
  skuSpecName?: string
  matchStatus?: string
  listingLinkSkuId?: number
  manualCostPrice?: number | null
  platformLineStatus?: string | null
  lineStatus?: string | null
  statusMatchStatus?: string | null
  sellerRemark?: string | null
  lineReceivedAmount?: number | null
  errorMessage?: string
}

export interface EcSalesOrderImportPreview {
  batchId: number
  batchNo: string
  totalRows: number
  matchedRows: number
  unmatchedRows: number
  statusUnmatchedRows?: number
  errorRows: number
  rows: EcSalesOrderImportRow[]
}

export type ImportRowPatchItem = {
  rowId: number
  manualCostPrice?: number | null
  lineStatus?: string | null
}

export function fetchSalesOrders(
  keyword?: string,
  status?: string,
  shopId?: number,
  pageQuery?: PageQuery,
) {
  return getData<PageResult<EcSalesOrder>>('/api/ecommerce/sales-orders', {
    ...(keyword ? { keyword } : {}),
    ...(status ? { status } : {}),
    ...(shopId ? { shopId } : {}),
    ...(pageQuery ?? {}),
  })
}

export function fetchSalesOrder(id: number) {
  return getData<EcSalesOrder>(`/api/ecommerce/sales-orders/${id}`)
}

export function createSalesOrder(data: EcSalesOrderSaveRequest) {
  return postData<EcSalesOrder>('/api/ecommerce/sales-orders', data)
}

export function updateSalesOrder(id: number, data: EcSalesOrderSaveRequest) {
  return putData<EcSalesOrder>(`/api/ecommerce/sales-orders/${id}`, data)
}

export function confirmSalesOrder(id: number) {
  return postData<EcSalesOrder>(`/api/ecommerce/sales-orders/${id}/confirm`, {})
}

export function shipSalesOrder(id: number) {
  return postData<EcSalesOrder>(`/api/ecommerce/sales-orders/${id}/ship`, {})
}

export function shipSalesOrderLine(orderId: number, lineId: number) {
  return postData<EcSalesOrder>(`/api/ecommerce/sales-orders/${orderId}/lines/${lineId}/ship`, {})
}

export function refundSalesOrderLine(
  orderId: number,
  lineId: number,
  data: { refundType?: string; refundAmount?: number | null },
) {
  return postData<EcSalesOrder>(`/api/ecommerce/sales-orders/${orderId}/lines/${lineId}/refund`, data)
}

export function cancelSalesOrderLine(orderId: number, lineId: number) {
  return postData<EcSalesOrder>(`/api/ecommerce/sales-orders/${orderId}/lines/${lineId}/cancel`, {})
}

export function deleteSalesOrder(id: number) {
  return deleteData(`/api/ecommerce/sales-orders/${id}`)
}

export function previewSalesOrderImport(data: {
  shopId: number
  profileId?: number
  fileName?: string
  rows: Record<string, string>[]
}) {
  return postData<EcSalesOrderImportPreview>('/api/ecommerce/sales-orders/import/preview', data)
}

export async function uploadSalesOrderImport(file: File, shopId: number, profileId?: number | null) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('shopId', String(shopId))
  if (profileId != null) {
    formData.append('profileId', String(profileId))
  }
  const response = await request.post<ApiResult<EcSalesOrderImportPreview>>(
    '/api/ecommerce/sales-orders/import/upload',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return response.data.data
}

export function commitSalesOrderImport(
  batchId: number,
  data?: { items: ImportRowPatchItem[] },
) {
  return postData<EcSalesOrderImportPreview>(
    `/api/ecommerce/sales-orders/import/${batchId}/commit`,
    data ?? {},
  )
}

export function updateImportManualCosts(
  batchId: number,
  data: { items: ImportRowPatchItem[] },
) {
  return postData<EcSalesOrderImportPreview>(
    `/api/ecommerce/sales-orders/import/${batchId}/manual-costs`,
    data,
  )
}
