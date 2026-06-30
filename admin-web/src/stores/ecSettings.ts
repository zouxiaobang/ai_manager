import { defineStore } from 'pinia'
import {
  fetchDataRetentionSettings,
  fetchExpressSettings,
  fetchInventorySettings,
  fetchNotificationSettings,
  fetchOrderImportSettings,
  fetchOrderImportStatusSettings,
  fetchRebateSettings,
  fetchSettlementSettings,
  type EcDataRetentionSettings,
  type EcExpressSettings,
  type EcInventorySettings,
  type EcNotificationSettings,
  type EcOrderImportSettings,
  type EcOrderImportStatusSettings,
  type EcRebateSettings,
  type EcSettlementSettings,
} from '@/api/ecommerce/ecSettings'
import {
  DEFAULT_STATUS_MAPPING,
  normalizeLineStatus,
  type ImportLineStatus,
} from '@/constants/importStatusMapping'

const DEFAULT_INVENTORY: EcInventorySettings = {
  defaultAlertThreshold: 10,
  slowMovingDays: 45,
  slowMovingFallbackDays: 90,
}

const DEFAULT_ORDER_IMPORT: EcOrderImportSettings = {
  headerRow: 1,
  dataStartRow: 2,
  dateFormat: 'yyyy-MM-dd HH:mm:ss',
}

const DEFAULT_ORDER_IMPORT_STATUS: EcOrderImportStatusSettings = {
  defaultLineStatus: 'PAID',
  statusMapping: { ...DEFAULT_STATUS_MAPPING },
}

const DEFAULT_EXPRESS: EcExpressSettings = {
  headerRow: 1,
  dataStartRow: 2,
  includeLabelPriceDefault: false,
}

const DEFAULT_SETTLEMENT: EcSettlementSettings = {
  profitDisplayMode: 'ACTUAL_PREFERRED',
  costIncludesFreight: true,
}

const DEFAULT_REBATE: EcRebateSettings = {
  defaultRebatePct: 0,
}

const DEFAULT_NOTIFICATION: EcNotificationSettings = {
  inventoryAlertEnabled: true,
  zeroStockAlertEnabled: true,
  settlementRemindEnabled: true,
  settlementRemindDayOfMonth: 25,
}

const DEFAULT_DATA_RETENTION: EcDataRetentionSettings = {
  importHistoryRetentionDays: 365,
  inventoryLogRetentionDays: 180,
  autoCleanupEnabled: false,
}

function toImportLineStatusMapping(map: Record<string, string>): Record<string, ImportLineStatus> {
  const result: Record<string, ImportLineStatus> = {}
  const source = map && Object.keys(map).length ? map : DEFAULT_STATUS_MAPPING
  for (const [key, value] of Object.entries(source)) {
    result[key] = normalizeLineStatus(value)
  }
  return result
}

export const useEcSettingsStore = defineStore('ecSettings', {
  state: () => ({
    loaded: false,
    loading: false,
    inventory: { ...DEFAULT_INVENTORY } as EcInventorySettings,
    orderImport: { ...DEFAULT_ORDER_IMPORT } as EcOrderImportSettings,
    orderImportStatus: { ...DEFAULT_ORDER_IMPORT_STATUS } as EcOrderImportStatusSettings,
    express: { ...DEFAULT_EXPRESS } as EcExpressSettings,
    settlement: { ...DEFAULT_SETTLEMENT } as EcSettlementSettings,
    rebate: { ...DEFAULT_REBATE } as EcRebateSettings,
    notification: { ...DEFAULT_NOTIFICATION } as EcNotificationSettings,
    dataRetention: { ...DEFAULT_DATA_RETENTION } as EcDataRetentionSettings,
  }),
  getters: {
    statusMappingForImport(): Record<string, ImportLineStatus> {
      return toImportLineStatusMapping(this.orderImportStatus.statusMapping)
    },
  },
  actions: {
    async ensureLoaded(force = false) {
      if (this.loaded && !force) return
      if (this.loading) return
      this.loading = true
      try {
        const [
          inventory,
          orderImport,
          orderImportStatus,
          express,
          settlement,
          rebate,
          notification,
          dataRetention,
        ] = await Promise.all([
          fetchInventorySettings(),
          fetchOrderImportSettings(),
          fetchOrderImportStatusSettings(),
          fetchExpressSettings(),
          fetchSettlementSettings(),
          fetchRebateSettings(),
          fetchNotificationSettings(),
          fetchDataRetentionSettings(),
        ])
        this.inventory = inventory
        this.orderImport = orderImport
        this.orderImportStatus = orderImportStatus
        this.express = express
        this.settlement = settlement
        this.rebate = rebate
        this.notification = notification
        this.dataRetention = dataRetention
        this.loaded = true
      } finally {
        this.loading = false
      }
    },
    applyInventory(settings: EcInventorySettings) {
      this.inventory = settings
      this.loaded = true
    },
    applyOrderImport(settings: EcOrderImportSettings) {
      this.orderImport = settings
      this.loaded = true
    },
    applyOrderImportStatus(settings: EcOrderImportStatusSettings) {
      this.orderImportStatus = settings
      this.loaded = true
    },
    applyExpress(settings: EcExpressSettings) {
      this.express = settings
      this.loaded = true
    },
    applySettlement(settings: EcSettlementSettings) {
      this.settlement = settings
      this.loaded = true
    },
    applyRebate(settings: EcRebateSettings) {
      this.rebate = settings
      this.loaded = true
    },
    applyNotification(settings: EcNotificationSettings) {
      this.notification = settings
      this.loaded = true
    },
    applyDataRetention(settings: EcDataRetentionSettings) {
      this.dataRetention = settings
      this.loaded = true
    },
    invalidate() {
      this.loaded = false
    },
  },
})
