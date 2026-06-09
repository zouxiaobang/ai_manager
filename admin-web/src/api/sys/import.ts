import { getData, postData, putData } from '../request'

export const BIZ_SALES_ORDER = 'SALES_ORDER'

export const BIZ_SETTLEMENT_EXPRESS_BILL = 'SETTLEMENT_EXPRESS_BILL'

export const BIZ_EXPRESS_STATION_NAME = 'EXPRESS_STATION_NAME'

export interface SysImportField {
  key: string
  labelZh: string
  labelEn: string
  required: boolean
}

export interface SysImportProfile {
  id?: number
  name: string
  bizType: string
  platformId?: number | null
  platformName?: string
  scopeKey?: string
  shopId?: number | null
  fileType?: string
  headerRow?: number
  dataStartRow?: number
  sheetName?: string | null
  columnMapping: Record<string, string>
  valueMapping?: Record<string, string>
  extraConfig?: Record<string, unknown>
  enabled?: number
  remark?: string
  updateTime?: string
}

export interface SysImportProfileSaveRequest {
  id?: number
  name: string
  bizType: string
  platformId?: number | null
  scopeKey?: string
  shopId?: number | null
  fileType?: string
  headerRow?: number
  dataStartRow?: number
  sheetName?: string | null
  columnMapping: Record<string, string>
  valueMapping?: Record<string, string>
  extraConfig?: Record<string, unknown>
  remark?: string
}

export function expressStationScopeKey(stationId: number) {
  return `express_station:${stationId}`
}

export function fetchImportFields(bizType: string) {
  return getData<SysImportField[]>('/api/sys/import/fields', { bizType })
}

export function fetchImportProfiles(bizType: string, platformId?: number, shopId?: number, scopeKey?: string) {
  return getData<SysImportProfile[]>('/api/sys/import/profiles', {
    bizType,
    ...(platformId != null ? { platformId } : {}),
    ...(shopId != null ? { shopId } : {}),
    ...(scopeKey ? { scopeKey } : {}),
  })
}

export function fetchImportProfileByScope(bizType: string, scopeKey: string) {
  return getData<SysImportProfile>('/api/sys/import/profiles/by-scope', { bizType, scopeKey })
}

export function fetchImportProfile(id: number) {
  return getData<SysImportProfile>(`/api/sys/import/profiles/${id}`)
}

export function createImportProfile(data: SysImportProfileSaveRequest) {
  return postData<SysImportProfile>('/api/sys/import/profiles', data)
}

export function updateImportProfile(id: number, data: SysImportProfileSaveRequest) {
  return putData<SysImportProfile>(`/api/sys/import/profiles/${id}`, data)
}

export function defaultPlatformProfileName(platformName: string) {
  return `${platformName}excel模版`
}

export function defaultExpressBillProfileName(stationName: string) {
  return `${stationName}账单列映射`
}
