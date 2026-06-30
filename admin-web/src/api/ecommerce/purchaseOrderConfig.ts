import { getData, putData } from '../request'

export interface EcPurchaseOrderConfig {
  title: string
  address?: string
  tel?: string
  requirementItems: string[]
  noteItems: string[]
  preparedBy?: string
  preparedPhone?: string
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  companyNo?: string
  updateTime?: string
}

export type EcPurchaseOrderConfigSaveRequest = Omit<EcPurchaseOrderConfig, 'updateTime'>

export function fetchPurchaseOrderConfig() {
  return getData<EcPurchaseOrderConfig>('/api/ecommerce/settings/purchase-order-config')
}

export function savePurchaseOrderConfig(data: EcPurchaseOrderConfigSaveRequest) {
  return putData<EcPurchaseOrderConfig>('/api/ecommerce/settings/purchase-order-config', data)
}
