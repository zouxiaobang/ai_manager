import { getData, postData, putData, deleteData } from '../request'
import type { PageQuery, PageResult } from '../pagination'

export interface EcInventory {
  id: number
  skuCode: string
  specName?: string
  productName?: string
  skuId?: number
  productId?: number
  factoryId?: number | null
  factoryName?: string
  skuStatus?: string
  quantity: number
  inTransitQty?: number
  salePrice?: number | null
  ignoreAlert?: boolean
  alertThreshold?: number
  alertActive?: boolean
  imageName?: string
  updateTime?: string
  recentLogs?: EcInventoryLog[]
  relatedInboundOrders?: EcInventoryInboundBrief[]
  relatedOutboundOrders?: EcInventoryOutboundBrief[]
  packingEstimate?: EcInventoryPackingEstimate
  outboundPackingEstimate?: EcInventoryPackingEstimate
}

export interface EcInventoryLog {
  id: number
  inventoryId: number
  changeType: 'DEDUCT' | 'RECLAIM' | 'INBOUND' | 'STOCKTAKE'
  changeQty: number
  refType?: string
  refId?: number
  remark?: string
  createTime?: string
}

export interface EcInventoryGlobalLog extends EcInventoryLog {
  skuCode?: string
  specName?: string
  productName?: string
  factoryId?: number | null
  factoryName?: string
}

export interface EcInventoryInboundBrief {
  id: number
  orderNo: string
  status: string
  quantity?: number
  receivedQuantity?: number | null
  orderTime?: string
  expectedDeliveryTime?: string
  actualReceiptTime?: string | null
}

export interface EcInventoryOutboundBrief {
  id: number
  orderNo: string
  status: string
  quantity?: number
  shippedQuantity?: number | null
  orderTime?: string
  expectedShipTime?: string
  actualShipTime?: string | null
}

export interface EcInventoryPackingEstimate {
  outboundQty?: number
  unitsPerCarton?: number
  cartonsNeeded?: number
  cartonId?: number | null
  cartonName?: string
  cartonVolumeCm3?: number | null
  totalVolumeCm3?: number | null
}

export interface EcInventoryFactorySummary {
  factoryId: number
  factoryName?: string
  skuCount: number
  totalQuantity: number
  totalStockValue?: number
  alertSkuCount: number
}

export interface EcInventorySkuOption {
  skuCode: string
  specName?: string
  productName?: string
  productId?: number
  factoryId?: number | null
  factoryName?: string
  skuStatus?: string
  inboundAllowed?: boolean
  hasInventory?: boolean
  quantity?: number
  alertThreshold?: number
  ignoreAlert?: boolean
  imageName?: string
}

export interface EcInventoryInboundRequest {
  skuCode: string
  quantity: number
  alertThreshold?: number
  ignoreAlert?: boolean
  remark?: string
}

export interface EcInventorySaveRequest {
  skuCode: string
  quantity?: number
  ignoreAlert?: boolean
  alertThreshold?: number
}

export interface EcInventoryAdjustRequest {
  changeType: 'DEDUCT' | 'RECLAIM'
  changeQty: number
}

export function fetchInventories(
  keyword?: string,
  alertOnly?: boolean,
  factoryId?: number,
  pageQuery?: PageQuery,
  inStockOnly?: boolean,
) {
  return getData<PageResult<EcInventory>>('/api/ecommerce/inventories', {
    ...(keyword ? { keyword } : {}),
    ...(alertOnly ? { alertOnly: true } : {}),
    ...(inStockOnly ? { inStockOnly: true } : {}),
    ...(factoryId ? { factoryId } : {}),
    ...(pageQuery ?? {}),
  })
}

export function fetchInventory(id: number) {
  return getData<EcInventory>(`/api/ecommerce/inventories/${id}`)
}

export function fetchInventoryLogs(id: number, pageQuery?: PageQuery) {
  return getData<PageResult<EcInventoryLog>>(`/api/ecommerce/inventories/${id}/logs`, {
    ...(pageQuery ?? {}),
  })
}

export function fetchGlobalInventoryLogs(params: {
  keyword?: string
  skuCode?: string
  factoryId?: number
  changeType?: string
  refType?: string
  startTime?: string
  endTime?: string
  page?: number
  pageSize?: number
}) {
  return getData<PageResult<EcInventoryGlobalLog>>('/api/ecommerce/inventories/logs', params)
}

export function fetchInventoryFactorySummary(factoryId?: number) {
  return getData<EcInventoryFactorySummary[]>('/api/ecommerce/inventories/factory-summary', {
    ...(factoryId ? { factoryId } : {}),
  })
}

export interface EcInventoryInboundValueSummary {
  totalInboundValue?: number | null
}

export function fetchInventoryInboundValueSummary(factoryId?: number) {
  return getData<EcInventoryInboundValueSummary>('/api/ecommerce/inventories/inbound-value-summary', {
    ...(factoryId ? { factoryId } : {}),
  })
}

export function fetchPackingEstimate(skuCode: string, outboundQty?: number) {
  return getData<EcInventoryPackingEstimate>('/api/ecommerce/inventories/packing-estimate', {
    skuCode,
    ...(outboundQty != null ? { outboundQty } : {}),
  })
}

export function fetchAvailableSkuCodes() {
  return getData<string[]>('/api/ecommerce/inventories/available-sku-codes')
}

export function fetchInventorySkuOptions(
  factoryId?: number,
  keyword?: string,
  productIds?: number | number[],
) {
  const ids = productIds == null ? [] : Array.isArray(productIds) ? productIds : [productIds]
  return getData<EcInventorySkuOption[]>('/api/ecommerce/inventories/sku-options', {
    ...(factoryId ? { factoryId } : {}),
    ...(ids.length ? { productIds: ids.join(',') } : {}),
    ...(keyword ? { keyword } : {}),
  })
}

export function quickInbound(data: EcInventoryInboundRequest) {
  return postData<EcInventory>('/api/ecommerce/inventories/inbound', data)
}

export function createInventory(data: EcInventorySaveRequest) {
  return postData<EcInventory>('/api/ecommerce/inventories', data)
}

export function updateInventory(id: number, data: EcInventorySaveRequest) {
  return putData<EcInventory>(`/api/ecommerce/inventories/${id}`, data)
}

export function adjustInventory(id: number, data: EcInventoryAdjustRequest) {
  return postData<EcInventory>(`/api/ecommerce/inventories/${id}/adjust`, data)
}

export function deleteInventory(id: number) {
  return deleteData(`/api/ecommerce/inventories/${id}`)
}
