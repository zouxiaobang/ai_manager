import { deleteData, getData, postData, putData } from '../request'

import type { PageQuery, PageResult } from '../pagination'



export type EcListingLinkPricingRisk = 'OK' | 'BELOW_MIN' | 'NEGATIVE_PROFIT'



export interface EcListingLinkSkuInventory {

  skuCode: string

  specName?: string

  skuStatus?: string

  quantity?: number

  alertThreshold?: number

  alertActive?: boolean

  inboundAllowed?: boolean

}



export interface EcListingLinkSku {

  id?: number

  skuName: string

  skuCodes: string

  discountPct?: number

  couponAmount?: number

  minSetAmount?: number | null

  costPrice?: number | null

  baseCostAmount?: number | null

  platformFeeAmount?: number | null

  actualSetAmount?: number | null

  profit?: number | null

  skuAmount?: number | null

  cartonAmount?: number | null

  expressAmount?: number | null

  platformFeePct?: number | null

  provinceName?: string

  pricingRisk?: EcListingLinkPricingRisk

  inventories?: EcListingLinkSkuInventory[]

  sortOrder?: number

}



export interface EcListingLinkProduct {

  productId: number

  productName?: string

  sortOrder?: number

}



export interface EcListingLink {

  id: number

  name: string

  shopId: number

  shopName?: string

  platformId?: number

  platformName?: string

  platformUrl?: string

  products?: EcListingLinkProduct[]

  productNames?: string

  listingTime?: string

  remark?: string

  status: 'ENABLED' | 'DISABLED'

  costFormula?: string

  createTime?: string

  updateTime?: string

  skus?: EcListingLinkSku[]

  skuCount?: number

}



export interface EcListingLinkSaveRequest {

  name: string

  shopId: number

  platformUrl?: string

  productIds?: number[]

  listingTime?: string

  remark?: string

  status?: string

  skus: EcListingLinkSku[]

}



export interface EcListingLinkPricingResult {

  skuCodes: string

  skuAmount: number

  cartonAmount: number

  expressAmount: number

  baseCostAmount: number

  platformFeeAmount: number

  platformFeePct: number

  fixedPlatformFee?: number

  costPrice: number

  minSetAmount: number

  actualSetAmount?: number | null

  profit?: number | null

  shipWeightKg?: number

  provinceName?: string

  pricingRisk?: EcListingLinkPricingRisk

  costFormula?: string

}



export function fetchListingLinks(

  keyword?: string,

  shopId?: number,

  platformId?: number,

  pageQuery?: PageQuery,

) {

  return getData<PageResult<EcListingLink>>('/api/ecommerce/listing-links', {

    ...(keyword ? { keyword } : {}),

    ...(shopId ? { shopId } : {}),

    ...(platformId ? { platformId } : {}),

    ...(pageQuery ?? {}),

  })

}



export function fetchListingLinksByProduct(productId: number) {

  return getData<EcListingLink[]>(`/api/ecommerce/listing-links/by-product/${productId}`)

}



export function fetchListingLink(id: number) {

  return getData<EcListingLink>(`/api/ecommerce/listing-links/${id}`)

}



export function calculateListingPricing(
  params: {
    shopId: number
    skuCodes: string
    discountPct?: number
    couponAmount?: number
    actualSetAmount?: number | null
  },
  options?: { silent?: boolean },
) {
  return postData<EcListingLinkPricingResult>(
    '/api/ecommerce/listing-links/calculate-pricing',
    params,
    { silent: options?.silent },
  )
}



export function recalculateAllListingPricing() {

  return postData<{ updated: number }>('/api/ecommerce/listing-links/recalculate-all', {})

}



export function createListingLink(data: EcListingLinkSaveRequest) {

  return postData<EcListingLink>('/api/ecommerce/listing-links', data)

}



export function copyListingLink(id: number) {

  return postData<EcListingLink>(`/api/ecommerce/listing-links/${id}/copy`)

}



export function updateListingLink(id: number, data: EcListingLinkSaveRequest) {

  return putData<EcListingLink>(`/api/ecommerce/listing-links/${id}`, data)

}



export function deleteListingLink(id: number) {

  return deleteData(`/api/ecommerce/listing-links/${id}`)

}

