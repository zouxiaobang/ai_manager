import { getData, postData, putData } from './request'

export type StorageOverLimitStrategy = 'REJECT' | 'CLEANUP_OLDEST' | 'CLEANUP_LARGEST'

export interface StorageZone {
  key: string
  label: string
  localPath: string
  cloudPath: string
  usedBytes: number
  quotaBytes: number
  fileCount: number
  usagePercent: number
  dualStorageEnabled: boolean
  cloudAvailable: boolean
  overLimitStrategy: StorageOverLimitStrategy
}

export interface StorageCenterOverview {
  totalLocalUsedBytes: number
  totalLocalQuotaBytes: number
  totalLocalUsagePercent: number
  cacheUsedBytes: number
  cacheMaxBytes: number
  cacheTtlSeconds: number
  baiduPanAuthorized: boolean
  baiduPanAuthorizeUrl?: string
  /** 当前环境百度网盘根目录 */
  baiduPanCloudRoot?: string
  dualStorageEnabled: boolean
  zones: StorageZone[]
}

export interface StorageCenterConfig {
  localQuotaMb: number
  ecommerceImagesQuotaMb: number
  notebookImagesQuotaMb: number
  notebookContentQuotaMb: number
  importFilesQuotaMb: number
  cacheMaxMb: number
  cacheTtlSeconds: number
  overLimitStrategy: StorageOverLimitStrategy
  localQuotaOverLimitStrategy: StorageOverLimitStrategy
  ecommerceImagesOverLimitStrategy: StorageOverLimitStrategy
  notebookImagesOverLimitStrategy: StorageOverLimitStrategy
  notebookContentOverLimitStrategy: StorageOverLimitStrategy
  importFilesOverLimitStrategy: StorageOverLimitStrategy
  cacheOverLimitStrategy: StorageOverLimitStrategy
  dualStorageEnabled: boolean
  updateTime?: string
}

export interface StorageCleanupResult {
  zone: string
  dryRun: boolean
  scannedCount: number
  removedCount: number
  freedBytes: number
  sampleFiles: string[]
}

export interface StorageOrphanFileItem {
  fileName: string
  relativePath: string
  sizeBytes: number
  orphanedAt: string
}

export interface StorageOrphanZonePreview {
  zoneKey: string
  zoneLabel: string
  zonePurpose: string
  supported: boolean
  localPath: string
  scannedCount: number
  orphanCount: number
  freedBytes: number
  zoneQuotaBytes: number
  files: StorageOrphanFileItem[]
}

export interface StorageOrphanPreview {
  dryRun: boolean
  totalScanned: number
  totalOrphanCount: number
  totalFreedBytes: number
  lastOrphanCleanupAt?: string | null
  zones: StorageOrphanZonePreview[]
}

export async function fetchStorageOverview(): Promise<StorageCenterOverview> {
  return getData<StorageCenterOverview>('/api/storage-center/overview')
}

export async function fetchStorageConfig(): Promise<StorageCenterConfig> {
  return getData<StorageCenterConfig>('/api/storage-center/config')
}

export async function saveStorageConfig(
  payload: Partial<StorageCenterConfig>,
): Promise<StorageCenterConfig> {
  return putData<StorageCenterConfig>('/api/storage-center/config', payload)
}

export async function cleanupOrphanFiles(
  zone: string,
  dryRun: boolean,
): Promise<StorageCleanupResult> {
  return postData<StorageCleanupResult>(
    `/api/storage-center/cleanup/orphans?zone=${encodeURIComponent(zone)}&dryRun=${dryRun}`,
  )
}

export async function previewAllOrphanFiles(): Promise<StorageOrphanPreview> {
  return postData<StorageOrphanPreview>('/api/storage-center/cleanup/orphans/preview')
}

export async function previewOrphanZone(zone: string): Promise<StorageOrphanZonePreview> {
  return postData<StorageOrphanZonePreview>(
    `/api/storage-center/cleanup/orphans/preview-zone?zone=${encodeURIComponent(zone)}`,
  )
}

export async function executeAllOrphanCleanup(): Promise<StorageOrphanPreview> {
  return postData<StorageOrphanPreview>('/api/storage-center/cleanup/orphans/execute-all')
}

export async function deleteOrphanFile(
  zone: string,
  relativePath: string,
): Promise<StorageCleanupResult> {
  return postData<StorageCleanupResult>(
    `/api/storage-center/cleanup/orphans/file?zone=${encodeURIComponent(zone)}&relativePath=${encodeURIComponent(relativePath)}`,
  )
}

export async function cleanupStorageCache(dryRun: boolean): Promise<StorageCleanupResult> {
  return postData<StorageCleanupResult>(
    `/api/storage-center/cleanup/cache?dryRun=${dryRun}`,
  )
}

export async function syncNoteContentReconcile(): Promise<void> {
  await postData<void>('/api/storage-center/sync/note-content')
}

export function formatStorageBytes(bytes: number): string {
  if (!bytes || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let value = bytes
  let index = 0
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index++
  }
  return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

export function formatStorageBytesCompact(bytes: number): string {
  return formatStorageBytes(bytes).replace(' ', '')
}

export function formatStorageUsageRatio(usedBytes: number, quotaBytes: number): string {
  const used = formatStorageBytesCompact(usedBytes)
  if (!quotaBytes || quotaBytes <= 0) {
    return formatStorageBytes(usedBytes)
  }
  return `${used}/${formatStorageBytesCompact(quotaBytes)}`
}

/** 将 MB 配额显示为「512MB (0.5GB)」形式 */
export function formatStorageMbWithHigherUnit(mb: number): string {
  if (!mb || mb <= 0) return ''
  const parts = splitStorageMbWithHigherUnit(mb)
  return parts ? `${parts.primary}${parts.secondary}` : ''
}

export function splitStorageMbWithHigherUnit(
  mb: number,
): { primary: string; secondary: string } | null {
  if (!mb || mb <= 0) return null
  const gb = mb / 1024
  const gbText = Number.isInteger(gb) ? `${gb}GB` : `${parseFloat(gb.toFixed(1))}GB`
  return { primary: `${mb}MB`, secondary: ` (${gbText})` }
}
