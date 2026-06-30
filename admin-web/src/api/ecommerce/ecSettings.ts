import { getData, putData } from '../request'
import type { ImportLineStatus } from '@/constants/importStatusMapping'

export interface EcInventorySettings {
  defaultAlertThreshold: number
  slowMovingDays: number
  slowMovingFallbackDays: number
  updateTime?: string
}

export interface EcOrderImportSettings {
  headerRow: number
  dataStartRow: number
  dateFormat: string
  updateTime?: string
}

export interface EcOrderImportStatusSettings {
  defaultLineStatus: ImportLineStatus
  statusMapping: Record<string, string>
  updateTime?: string
}

export interface EcExpressSettings {
  headerRow: number
  dataStartRow: number
  includeLabelPriceDefault: boolean
  updateTime?: string
}

export interface EcDeliveryNoteConfig {
  title: string
  address?: string
  tel?: string
  preparedBy?: string
  shipFromName?: string
  shipFromPhone?: string
  shipFromAddress?: string
  requirementItems: string[]
  noteItems: string[]
  updateTime?: string
}

export interface EcOutboundOrderConfig {
  title: string
  address?: string
  tel?: string
  preparedBy?: string
  approvedBy?: string
  warehouseKeeper?: string
  requirementItems: string[]
  noteItems: string[]
  updateTime?: string
}

export interface EcSettlementSettings {
  profitDisplayMode: 'ESTIMATED' | 'ACTUAL_PREFERRED'
  costIncludesFreight: boolean
  updateTime?: string
}

export interface EcRebateSettings {
  defaultRebatePct: number
  updateTime?: string
}

export interface EcNotificationSettings {
  inventoryAlertEnabled: boolean
  zeroStockAlertEnabled: boolean
  settlementRemindEnabled: boolean
  settlementRemindDayOfMonth: number
  updateTime?: string
}

export interface EcDataRetentionSettings {
  importHistoryRetentionDays: number
  inventoryLogRetentionDays: number
  autoCleanupEnabled: boolean
  updateTime?: string
}

export interface EcCompanyInfo {
  companyName?: string
  address?: string
  tel?: string
  contactName?: string
  contactPhone?: string
  updateTime?: string
}

export interface EcSettingsSummaryItem {
  key: string
  label: string
  configured?: boolean
  updateTime?: string
}

export function fetchSettingsSummary() {
  return getData<EcSettingsSummaryItem[]>('/api/ecommerce/settings/summary')
}

export function fetchInventorySettings() {
  return getData<EcInventorySettings>('/api/ecommerce/settings/inventory')
}

export function saveInventorySettings(data: Omit<EcInventorySettings, 'updateTime'>) {
  return putData<EcInventorySettings>('/api/ecommerce/settings/inventory', data)
}

export function fetchOrderImportSettings() {
  return getData<EcOrderImportSettings>('/api/ecommerce/settings/order-import')
}

export function saveOrderImportSettings(data: Omit<EcOrderImportSettings, 'updateTime'>) {
  return putData<EcOrderImportSettings>('/api/ecommerce/settings/order-import', data)
}

export function fetchOrderImportStatusSettings() {
  return getData<EcOrderImportStatusSettings>('/api/ecommerce/settings/order-import-status')
}

export function saveOrderImportStatusSettings(data: Omit<EcOrderImportStatusSettings, 'updateTime'>) {
  return putData<EcOrderImportStatusSettings>('/api/ecommerce/settings/order-import-status', data)
}

export function fetchExpressSettings() {
  return getData<EcExpressSettings>('/api/ecommerce/settings/express')
}

export function saveExpressSettings(data: Omit<EcExpressSettings, 'updateTime'>) {
  return putData<EcExpressSettings>('/api/ecommerce/settings/express', data)
}

export function fetchDeliveryNoteConfig() {
  return getData<EcDeliveryNoteConfig>('/api/ecommerce/settings/delivery-note')
}

export function saveDeliveryNoteConfig(data: Omit<EcDeliveryNoteConfig, 'updateTime'>) {
  return putData<EcDeliveryNoteConfig>('/api/ecommerce/settings/delivery-note', data)
}

export function fetchOutboundOrderConfig() {
  return getData<EcOutboundOrderConfig>('/api/ecommerce/settings/outbound-order')
}

export function saveOutboundOrderConfig(data: Omit<EcOutboundOrderConfig, 'updateTime'>) {
  return putData<EcOutboundOrderConfig>('/api/ecommerce/settings/outbound-order', data)
}

export function fetchSettlementSettings() {
  return getData<EcSettlementSettings>('/api/ecommerce/settings/settlement')
}

export function saveSettlementSettings(data: Omit<EcSettlementSettings, 'updateTime'>) {
  return putData<EcSettlementSettings>('/api/ecommerce/settings/settlement', data)
}

export function fetchRebateSettings() {
  return getData<EcRebateSettings>('/api/ecommerce/settings/rebate')
}

export function saveRebateSettings(data: Omit<EcRebateSettings, 'updateTime'>) {
  return putData<EcRebateSettings>('/api/ecommerce/settings/rebate', data)
}

export function fetchNotificationSettings() {
  return getData<EcNotificationSettings>('/api/ecommerce/settings/notification')
}

export function saveNotificationSettings(data: Omit<EcNotificationSettings, 'updateTime'>) {
  return putData<EcNotificationSettings>('/api/ecommerce/settings/notification', data)
}

export function fetchDataRetentionSettings() {
  return getData<EcDataRetentionSettings>('/api/ecommerce/settings/data-retention')
}

export function saveDataRetentionSettings(data: Omit<EcDataRetentionSettings, 'updateTime'>) {
  return putData<EcDataRetentionSettings>('/api/ecommerce/settings/data-retention', data)
}

export function fetchCompanyInfo() {
  return getData<EcCompanyInfo>('/api/ecommerce/settings/company')
}

export function saveCompanyInfo(data: Omit<EcCompanyInfo, 'updateTime'>) {
  return putData<EcCompanyInfo>('/api/ecommerce/settings/company', data)
}
