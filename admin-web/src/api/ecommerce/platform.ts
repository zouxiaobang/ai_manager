import { deleteData, getData, postData, putData } from '../request'
import type { PageQuery, PageResult } from '../pagination'

export interface EcPlatform {
  id: number
  name: string
  nameEn?: string
  platformCode: number
  channelType: 'ONLINE' | 'OFFLINE'
  remark?: string
  status: 'ENABLED' | 'DISABLED'
  updateTime?: string
}

export interface EcPlatformSaveRequest {
  name: string
  nameEn?: string
  platformCode: number
  channelType?: string
  remark?: string
  status?: string
}

export const PLATFORM_CODE_OPTIONS = [
  { value: 0, labelKey: 'offline' },
  { value: 1, labelKey: 'alibaba1688' },
  { value: 2, labelKey: 'taobao' },
  { value: 3, labelKey: 'tmall' },
  { value: 4, labelKey: 'pinduoduo' },
  { value: 5, labelKey: 'douyin' },
  { value: 6, labelKey: 'jd' },
  { value: 7, labelKey: 'xiaohongshu' },
  { value: 8, labelKey: 'kuaishou' },
  { value: 99, labelKey: 'other' },
] as const

export function fetchPlatforms(keyword?: string, channelType?: string, pageQuery?: PageQuery) {
  return getData<PageResult<EcPlatform>>('/api/ecommerce/platforms', {
    ...(keyword ? { keyword } : {}),
    ...(channelType ? { channelType } : {}),
    ...(pageQuery ?? {}),
  })
}

export function fetchPlatformOptions() {
  return getData<EcPlatform[]>('/api/ecommerce/platforms/options')
}

export function fetchPlatform(id: number) {
  return getData<EcPlatform>(`/api/ecommerce/platforms/${id}`)
}

export function createPlatform(data: EcPlatformSaveRequest) {
  return postData<EcPlatform>('/api/ecommerce/platforms', data)
}

export function updatePlatform(id: number, data: EcPlatformSaveRequest) {
  return putData<EcPlatform>(`/api/ecommerce/platforms/${id}`, data)
}

export function deletePlatform(id: number) {
  return deleteData(`/api/ecommerce/platforms/${id}`)
}
