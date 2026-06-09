import { deleteData, getData, postData, putData } from '../request'
import type { PageQuery, PageResult } from '../pagination'

export interface EcShop {
  id: number
  name: string
  nameEn?: string
  platformId: number
  platformName?: string
  platformCode?: number
  channelType?: string
  remark?: string
  categoryCommissionPct?: number | null
  techServiceFeePct?: number | null
  paymentFeePct?: number | null
  promotionFeePct?: number | null
  fulfillmentFeePct?: number | null
  returnServiceFeePct?: number | null
  installmentFeePct?: number | null
  activityServiceFeePct?: number | null
  annualPlatformFee?: number | null
  depositAmount?: number | null
  shippingInsuranceFee?: number | null
  otherFeePct?: number | null
  otherFeeRemark?: string
  defaultReceiveProvince?: string
  status: 'ENABLED' | 'DISABLED'
  updateTime?: string
}

export type EcShopSaveRequest = Omit<EcShop, 'id' | 'platformName' | 'platformCode' | 'channelType' | 'updateTime'>

export function fetchShops(keyword?: string, platformId?: number, pageQuery?: PageQuery) {
  return getData<PageResult<EcShop>>('/api/ecommerce/shops', {
    ...(keyword ? { keyword } : {}),
    ...(platformId ? { platformId } : {}),
    ...(pageQuery ?? {}),
  })
}

export function fetchShopOptions(platformId?: number) {
  return getData<EcShop[]>('/api/ecommerce/shops/options', {
    ...(platformId ? { platformId } : {}),
  })
}

export function fetchShop(id: number) {
  return getData<EcShop>(`/api/ecommerce/shops/${id}`)
}

export function createShop(data: EcShopSaveRequest) {
  return postData<EcShop>('/api/ecommerce/shops', data)
}

export function updateShop(id: number, data: EcShopSaveRequest) {
  return putData<EcShop>(`/api/ecommerce/shops/${id}`, data)
}

export function deleteShop(id: number) {
  return deleteData(`/api/ecommerce/shops/${id}`)
}
