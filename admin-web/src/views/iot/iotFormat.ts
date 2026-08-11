import type { IotDeviceStatus, IotOtaState } from '@/api/iot'

export type TagType = 'success' | 'info' | 'warning' | 'danger' | 'primary'

/** 设备状态 → 文案 + 标签类型（未知状态兜底离线） */
export function resolveDeviceStatusMeta(status?: IotDeviceStatus): { label: string; tagType: TagType } {
  if (status === 'ONLINE') return { label: 'iot.device.statusOnline', tagType: 'success' }
  if (status === 'INACTIVE') return { label: 'iot.device.statusInactive', tagType: 'warning' }
  return { label: 'iot.device.statusOffline', tagType: 'info' }
}

/** OTA 状态 → 文案 + 标签类型 */
export function resolveOtaStateMeta(state?: IotOtaState): { label: string; tagType: TagType } {
  if (state === 'SUCCESS') return { label: 'iot.ota.stateSuccess', tagType: 'success' }
  if (state === 'FAILED') return { label: 'iot.ota.stateFailed', tagType: 'danger' }
  if (state === 'UPGRADING') return { label: 'iot.ota.stateUpgrading', tagType: 'warning' }
  return { label: 'iot.ota.statePending', tagType: 'info' }
}

/** 固件发布状态 → 文案 + 标签类型（对应后端 FirmwareVO.status：PUBLISHED/DRAFT，未知兜底草稿） */
export function resolveFirmwareStatusMeta(status?: string): { label: string; tagType: TagType } {
  if (status === 'PUBLISHED') return { label: 'iot.firmware.statusPublished', tagType: 'success' }
  return { label: 'iot.firmware.statusDraft', tagType: 'info' }
}

/** 时间字符串格式化：空值/非法返回占位符，合法返回 yyyy-MM-dd HH:mm:ss */
export function formatIotTime(value?: string | null): string {
  if (!value) return '—'
  const normalized = value.replace('T', ' ').slice(0, 19)
  if (!/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(normalized)) return '—'
  return normalized
}

/** 字节数 → 人类可读大小；空值返回占位符 */
export function formatBytes(size?: number | null): string {
  if (size == null || !Number.isFinite(size) || size < 0) return '—'
  const units = ['B', 'KB', 'MB', 'GB']
  let value = size
  let unitIndex = 0
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex += 1
  }
  return `${unitIndex === 0 ? value : value.toFixed(1)} ${units[unitIndex]}`
}

/** 会话在线判定：显式 online 优先，否则按 endedAt 缺失 + sessionId 存在推断 */
export function resolveSessionOnline(session: { online?: boolean; endedAt?: string; sessionId?: string }): boolean {
  if (session.online !== undefined) return session.online
  return Boolean(session.sessionId) && !session.endedAt
}

/** 设备/会话展示名：name 优先，否则 MAC，否则 UUID，最后兜底 #id */
export function resolveDeviceName(
  item: { deviceName?: string; mac?: string; uuid?: string; id: number },
  t: (key: string) => string,
): string {
  const name = item.deviceName?.trim() || item.mac?.trim() || item.uuid?.trim()
  return name || `${t('iot.device.unnamed')}#${item.id}`
}
