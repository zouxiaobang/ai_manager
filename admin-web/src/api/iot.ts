import request, { deleteData, getData, postData, putData } from './request'
import type { PageQuery, PageResult } from './pagination'
import type { ApiResult } from './types'

/** 设备在线状态（与后端 iot_device.status 对齐） */
export type IotDeviceStatus = 'ONLINE' | 'OFFLINE' | 'INACTIVE'

/** 设备 VO（对应后端 domain/vo/DeviceVO） */
export interface IotDevice {
  id: number
  uuid: string
  clientId?: string
  mac?: string
  model?: string
  chip?: string
  firmwareVersion?: string
  wsToken?: string
  activatedAt?: string
  lastSeenAt?: string
  status?: IotDeviceStatus
  sessionId?: string
  otaState?: string
  createTime?: string
  updateTime?: string
}

export interface IotDeviceQuery extends PageQuery {
  keyword?: string
  status?: IotDeviceStatus
}

/** 在线状态探测结果 */
export interface IotDeviceOnlineStatus {
  online: boolean
  lastSeenAt?: string
}

/** 固件 VO（对应后端 domain/vo/FirmwareVO，status：PUBLISHED/DRAFT） */
export interface IotFirmware {
  id: number
  version: string
  fileName?: string
  filePath?: string
  fileHash?: string
  size?: number
  force?: boolean
  releaseNote?: string
  status?: string
  createTime?: string
}

export interface IotFirmwareQuery extends PageQuery {
  keyword?: string
}

/** OTA 记录状态 */
export type IotOtaState = 'PENDING' | 'UPGRADING' | 'SUCCESS' | 'FAILED'

/** OTA 记录 VO（对应后端 domain/vo/... 之 IotOtaRecord） */
export interface IotOtaRecord {
  id: number
  deviceId?: number
  deviceName?: string
  firmwareId?: number
  firmwareVersion?: string
  state?: IotOtaState
  progress?: number
  startedAt?: string
  finishedAt?: string
}

export interface IotOtaRecordQuery extends PageQuery {
  deviceId?: number
  state?: IotOtaState
}

/** 在线会话 VO（对应后端 domain/vo/OnlineSessionVO） */
export interface IotOnlineSession {
  id: number
  deviceId?: number
  deviceName?: string
  sessionId?: string
  startedAt?: string
  endedAt?: string
  turnCount?: number
  online?: boolean
}

export interface IotSessionQuery extends PageQuery {
  deviceId?: number
  online?: boolean
}

// ========== 设备管理 ==========

export function fetchIotDevices(query?: IotDeviceQuery) {
  return getData<PageResult<IotDevice>>('/api/iot/device', { ...query })
}

export function fetchIotDevice(id: number) {
  return getData<IotDevice>(`/api/iot/device/${id}`)
}

export function fetchIotDeviceOnlineStatus(id: number) {
  return getData<IotDeviceOnlineStatus>(`/api/iot/device/${id}/online`)
}

/** 远程重启设备（设备端 system(reboot) 消息） */
export function rebootIotDevice(id: number) {
  return postData<void>(`/api/iot/device/${id}/reboot`)
}

/** 绑定 / 更新设备备注信息 */
export function updateIotDevice(id: number, payload: { model?: string; remark?: string }) {
  return putData<IotDevice>(`/api/iot/device/${id}`, payload)
}

// ========== 固件管理 ==========

export function fetchIotFirmwares(query?: IotFirmwareQuery) {
  return getData<PageResult<IotFirmware>>('/api/iot/firmware', { ...query })
}

export interface IotFirmwareUploadPayload {
  file: File
  version: string
  force?: boolean
  releaseNote?: string
}

/** 上传固件（multipart：file + version + force + releaseNote） */
export async function uploadIotFirmware(payload: IotFirmwareUploadPayload): Promise<IotFirmware> {
  const formData = new FormData()
  formData.append('file', payload.file)
  formData.append('version', payload.version.trim())
  if (payload.force) formData.append('force', 'true')
  if (payload.releaseNote?.trim()) formData.append('releaseNote', payload.releaseNote.trim())
  const response = await request.post<ApiResult<IotFirmware>>('/api/iot/firmware/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
  return response.data.data
}

/** 发布固件：force=false 仅对新激活设备生效，true 则通知在线设备升级 */
export function publishIotFirmware(id: number, force = false) {
  return postData<void>(`/api/iot/firmware/${id}/publish`, { force })
}

/** 强制升级：置 force=1 并发布，下次 OTA check 对设备强制下发 */
export function forceUpgradeIotFirmware(firmwareId: number) {
  return postData<void>(`/api/iot/firmware/${firmwareId}/force`)
}

export function deleteIotFirmware(id: number) {
  return deleteData(`/api/iot/firmware/${id}`)
}

// ========== OTA 记录 ==========

export function fetchIotOtaRecords(query?: IotOtaRecordQuery) {
  return getData<PageResult<IotOtaRecord>>('/api/iot/firmware/ota-records', { ...query })
}

// ========== 在线会话 ==========

export function fetchIotOnlineSessions(query?: IotSessionQuery) {
  return getData<PageResult<IotOnlineSession>>('/api/iot/session', { ...query })
}
