import { deleteData, getData, postData, putData } from '../request'

import type { PageQuery, PageResult } from '../pagination'



export interface EcFactory {

  id: number

  name: string

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

  contactName?: string

  contactPhone?: string

  address?: string

  remark?: string

  status?: string

}



export function fetchFactories(keyword?: string, pageQuery?: PageQuery) {

  return getData<PageResult<EcFactory>>('/api/ecommerce/factories', {

    ...(keyword ? { keyword } : {}),

    ...(pageQuery ?? {}),

  })

}



export function fetchFactoryOptions() {

  return getData<EcFactory[]>('/api/ecommerce/factories/options')

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

