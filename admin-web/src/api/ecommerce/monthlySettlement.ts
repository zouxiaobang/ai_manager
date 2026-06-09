import { deleteData, getData, postData, putData } from '../request'
import request from '../request'
import type { ApiResult } from '../types'

export interface MonthlySettlementShopSummary {
  shopId: number
  shopName?: string
  totalRevenue?: number
  estimatedTotalCost?: number
  actualTotalCost?: number
  estimatedTotalProfit?: number
  actualTotalProfit?: number
  includedOrderCount?: number
  excludedOrderCount?: number
  pendingOrderCount?: number
  maxProfitOrder?: {
    orderId?: number
    orderNo?: string
    platformOrderNo?: string
    profitAmount?: number
    receivedAmount?: number
  }
  pendingOrders?: PendingSettlementOrder[]
}

export interface PendingSettlementOrder {
  orderId?: number
  orderNo?: string
  platformOrderNo?: string
  status?: string
  buyerName?: string
  receivedAmount?: number
  orderTime?: string
  decided?: boolean
  included?: boolean | null
}

export interface MonthlySettlementResult {
  settlementMonth: string
  expressBillImported?: boolean
  shops: MonthlySettlementShopSummary[]
}

export interface SettlementBuyerExclude {
  id?: number
  shopId?: number | null
  shopName?: string | null
  buyerName: string
  remark?: string
  enabled?: number
  createTime?: string
}

export const EXPRESS_STATION_OTHER = 0

export interface ExpressBillImportResult {
  billId: number
  billMonth: string
  expressStationId?: number
  otherExpress?: boolean
  expressStationName?: string
  totalRows: number
  matchedRows: number
  unmatchedRows: number
  gapOrderRows?: number
  manualPendingRows?: number
  manualAppliedRows?: number
  overwrittenRows?: number
}

export interface ExpressBillLine {
  id?: number
  billId?: number
  source?: string
  orderId?: number
  shopName?: string
  platformOrderNo?: string
  orderNo?: string
  trackingNumber?: string
  freightAmount?: number | null
  settlementDestination?: string
  weight?: number | null
  shipTime?: string
  matchStatus?: string
  remark?: string
}

export interface ExpressBillRecord {
  id: number
  billMonth: string
  expressStationId?: number
  otherExpress?: boolean
  expressStationName?: string
  fileName?: string
  importMode?: string
  totalRows?: number
  matchedRows?: number
  unmatchedRows?: number
  gapOrderRows?: number
  manualAppliedRows?: number
  includeLabelPrice?: boolean
  status?: string
  createTime?: string
}

export function fetchMonthlySettlement(month: string, shopId?: number) {
  return getData<MonthlySettlementResult>('/api/ecommerce/monthly-settlement', {
    month,
    ...(shopId ? { shopId } : {}),
  })
}

export function fetchExpressBillImported(month: string) {
  return getData<boolean>('/api/ecommerce/monthly-settlement/express-bill/imported', { month })
}

export function fetchSettlementBuyerExcludes(shopId?: number) {
  return getData<SettlementBuyerExclude[]>('/api/ecommerce/monthly-settlement/buyer-excludes', {
    ...(shopId ? { shopId } : {}),
  })
}

export function saveSettlementBuyerExclude(data: {
  id?: number
  shopId?: number | null
  buyerName: string
  remark?: string
  enabled?: number
}) {
  return postData<SettlementBuyerExclude>('/api/ecommerce/monthly-settlement/buyer-excludes', data)
}

export function deleteSettlementBuyerExclude(id: number) {
  return deleteData(`/api/ecommerce/monthly-settlement/buyer-excludes/${id}`)
}

export function saveSettlementOrderDecisions(data: {
  settlementMonth: string
  items: { orderId: number; included: boolean }[]
}) {
  return postData<MonthlySettlementResult>('/api/ecommerce/monthly-settlement/order-decisions', data)
}

export async function uploadSettlementExpressBill(params: {
  month: string
  expressStationId: number
  file: File
  columnMapping?: Record<string, string>
  headerRow?: number
  dataStartRow?: number
  includeLabelPrice?: boolean
}) {
  const formData = new FormData()
  formData.append('month', params.month)
  formData.append('expressStationId', String(params.expressStationId))
  formData.append('file', params.file)
  formData.append('includeLabelPrice', String(!!params.includeLabelPrice))
  if (params.columnMapping) {
    formData.append('columnMapping', JSON.stringify(params.columnMapping))
  }
  if (params.headerRow != null) formData.append('headerRow', String(params.headerRow))
  if (params.dataStartRow != null) formData.append('dataStartRow', String(params.dataStartRow))
  const response = await request.post<ApiResult<ExpressBillImportResult>>(
    '/api/ecommerce/monthly-settlement/express-bill/import',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return response.data.data
}

export async function prepareManualExpressBill(
  month: string,
  expressStationId: number,
  includeLabelPrice?: boolean,
) {
  const response = await request.post<ApiResult<ExpressBillImportResult>>(
    '/api/ecommerce/monthly-settlement/express-bill/manual/prepare',
    null,
    { params: { month, expressStationId, includeLabelPrice: !!includeLabelPrice } },
  )
  return response.data.data!
}

export function fetchManualExpressBillLines(billId: number) {
  return getData<ExpressBillLine[]>('/api/ecommerce/monthly-settlement/express-bill/manual/lines', { billId })
}

export function saveManualExpressBillLines(data: {
  billId: number
  expressStationId?: number
  lines: {
    lineId?: number
    orderId?: number
    platformOrderNo?: string
    orderNo?: string
    trackingNumber?: string
    freightAmount?: number | null
    remark?: string
  }[]
}) {
  return postData<ExpressBillImportResult>('/api/ecommerce/monthly-settlement/express-bill/manual/lines', data)
}

export function fetchExpressBillRecords(month: string) {
  return getData<ExpressBillRecord[]>('/api/ecommerce/monthly-settlement/express-bill/records', { month })
}

export async function previewExpressBillColumns(file: File, headerRow?: number) {
  const formData = new FormData()
  formData.append('file', file)
  if (headerRow != null) formData.append('headerRow', String(headerRow))
  const response = await request.post<ApiResult<{ columns: string[] }>>(
    '/api/ecommerce/monthly-settlement/express-bill/preview-columns',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return response.data.data?.columns ?? []
}
