import { deleteData, getData, postData, putData } from '../request'

import type { PageQuery, PageResult } from '../pagination'

export type EcFactoryType = 'PRODUCTION' | 'CUSTOMER' | 'CARTON'

export interface EcFactory {
  id: number
  name: string
  factoryType?: EcFactoryType
  contactName?: string
  contactPhone?: string
  address?: string
  remark?: string
  status: string
  createTime?: string
  updateTime?: string
}

export interface EcFactorySaveRequest {
  name: string
  factoryType?: EcFactoryType
  contactName?: string
  contactPhone?: string
  address?: string
  remark?: string
  status?: string
}

export interface EcFactoryStats {
  productionCount: number
  customerCount: number
  cartonCount: number
  enabledCount: number
  disabledCount: number
}

export function fetchFactories(
  keyword?: string,
  pageQuery?: PageQuery & { factoryType?: EcFactoryType; status?: string },
) {
  return getData<PageResult<EcFactory>>('/api/ecommerce/factories', {
    ...(keyword ? { keyword } : {}),
    ...(pageQuery?.factoryType ? { factoryType: pageQuery.factoryType } : {}),
    ...(pageQuery?.status ? { status: pageQuery.status } : {}),
    ...(pageQuery ?? {}),
  })
}

export function fetchFactoryStats() {
  return getData<EcFactoryStats>('/api/ecommerce/factories/stats')
}

export function fetchFactoryOptions(factoryType?: EcFactoryType) {
  return getData<EcFactory[]>('/api/ecommerce/factories/options', {
    ...(factoryType ? { factoryType } : {}),
  })
}

export function fetchFactory(id: number) {
  return getData<EcFactory>(`/api/ecommerce/factories/${id}`)
}

export function createFactory(data: EcFactorySaveRequest) {
  return postData<EcFactory>('/api/ecommerce/factories', data)
}

export function updateFactory(id: number, data: EcFactorySaveRequest) {
  return putData<EcFactory>(`/api/ecommerce/factories/${id}`, data)
}

export function deleteFactory(id: number) {
  return deleteData(`/api/ecommerce/factories/${id}`)
}
