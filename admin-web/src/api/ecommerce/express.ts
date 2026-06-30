import { deleteData, getData, postData, putData } from '../request'
import type { PageQuery, PageResult } from '../pagination'

export interface EcExpressStation {
  id: number
  name: string
  avatarUrl?: string | null
  contact?: string
  address?: string
  labelPrice?: number | null
  isDefault?: boolean
  updateTime?: string
  nameAliases?: string[]
  prices?: EcExpressPrice[]
  notices?: EcExpressNotice[]
  priceCount?: number
  noticeCount?: number
}

export interface EcExpressPrice {
  id: number
  stationId: number
  provinceName: string
  priceW03Kg?: number | null
  priceW05Kg?: number | null
  priceW1Kg?: number | null
  priceW15Kg?: number | null
  priceW2Kg?: number | null
  priceW25Kg?: number | null
  priceW3Kg?: number | null
  over3FirstPrice?: number | null
  over3AdditionalPrice?: number | null
  updateTime?: string
}

export interface EcExpressNotice {
  id: number
  stationId: number
  content: string
  highlightRed?: boolean
  sortOrder?: number
  updateTime?: string
}

export interface EcExpressStationSaveRequest {
  name: string
  avatarUrl?: string | null
  contact?: string
  address?: string
  labelPrice?: number | null
  isDefault?: boolean
  nameAliases?: string[]
}

export interface EcExpressPriceSaveRequest {
  stationId: number
  provinceName: string
  priceW03Kg?: number | null
  priceW05Kg?: number | null
  priceW1Kg?: number | null
  priceW15Kg?: number | null
  priceW2Kg?: number | null
  priceW25Kg?: number | null
  priceW3Kg?: number | null
  over3FirstPrice?: number | null
  over3AdditionalPrice?: number | null
}

export interface EcExpressNoticeSaveRequest {
  stationId: number
  content: string
  highlightRed?: boolean
  sortOrder?: number
}

export function fetchExpressStations(
  keyword?: string,
  pageQuery?: PageQuery,
  options?: { defaultOnly?: boolean; regionNames?: string[] },
) {
  return getData<PageResult<EcExpressStation>>('/api/ecommerce/express/stations', {
    ...(keyword ? { keyword } : {}),
    ...(options?.defaultOnly ? { defaultOnly: true } : {}),
    ...(options?.regionNames?.length ? { regionNames: options.regionNames.join(',') } : {}),
    ...(pageQuery ?? {}),
  })
}

export function fetchExpressRegions() {
  return getData<string[]>('/api/ecommerce/express/stations/regions')
}

export function fetchExpressStation(id: number) {
  return getData<EcExpressStation>(`/api/ecommerce/express/stations/${id}`)
}

export function createExpressStation(data: EcExpressStationSaveRequest) {
  return postData<EcExpressStation>('/api/ecommerce/express/stations', data)
}

export function updateExpressStation(id: number, data: EcExpressStationSaveRequest) {
  return putData<EcExpressStation>(`/api/ecommerce/express/stations/${id}`, data)
}

export function deleteExpressStation(id: number) {
  return deleteData(`/api/ecommerce/express/stations/${id}`)
}

export function copyExpressStation(id: number) {
  return postData<EcExpressStation>(`/api/ecommerce/express/stations/${id}/copy`)
}

export function fetchExpressPrices(stationId: number) {
  return getData<EcExpressPrice[]>('/api/ecommerce/express/prices', { stationId })
}

export function createExpressPrice(data: EcExpressPriceSaveRequest) {
  return postData<EcExpressPrice>('/api/ecommerce/express/prices', data)
}

export function updateExpressPrice(id: number, data: EcExpressPriceSaveRequest) {
  return putData<EcExpressPrice>(`/api/ecommerce/express/prices/${id}`, data)
}

export function deleteExpressPrice(id: number) {
  return deleteData(`/api/ecommerce/express/prices/${id}`)
}

export function fetchExpressNotices(stationId: number) {
  return getData<EcExpressNotice[]>('/api/ecommerce/express/notices', { stationId })
}

export function createExpressNotice(data: EcExpressNoticeSaveRequest) {
  return postData<EcExpressNotice>('/api/ecommerce/express/notices', data)
}

export function updateExpressNotice(id: number, data: EcExpressNoticeSaveRequest) {
  return putData<EcExpressNotice>(`/api/ecommerce/express/notices/${id}`, data)
}

export function deleteExpressNotice(id: number) {
  return deleteData(`/api/ecommerce/express/notices/${id}`)
}
