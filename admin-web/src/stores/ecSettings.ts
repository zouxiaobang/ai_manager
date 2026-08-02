import { computed, ref } from 'vue'
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

/**
 * 电商设置 store（组合式）
 * 维护 8 类设置的本地缓存与加载状态，ensureLoaded 保证并行加载一次。
 */
export const useEcSettingsStore = defineStore('ecSettings', () => {
  const loaded = ref(false)
  const loading = ref(false)
  const inventory = ref<EcInventorySettings>({ ...DEFAULT_INVENTORY })
  const orderImport = ref<EcOrderImportSettings>({ ...DEFAULT_ORDER_IMPORT })
  const orderImportStatus = ref<EcOrderImportStatusSettings>({ ...DEFAULT_ORDER_IMPORT_STATUS })
  const express = ref<EcExpressSettings>({ ...DEFAULT_EXPRESS })
  const settlement = ref<EcSettlementSettings>({ ...DEFAULT_SETTLEMENT })
  const rebate = ref<EcRebateSettings>({ ...DEFAULT_REBATE })
  const notification = ref<EcNotificationSettings>({ ...DEFAULT_NOTIFICATION })
  const dataRetention = ref<EcDataRetentionSettings>({ ...DEFAULT_DATA_RETENTION })

  /** 导入状态映射，兼容默认值 */
  const statusMappingForImport = computed(() =>
    toImportLineStatusMapping(orderImportStatus.value.statusMapping),
  )

  async function ensureLoaded(force = false) {
    if (loaded.value && !force) return
    if (loading.value) return
    loading.value = true
    try {
      const [inv, imp, impStatus, exp, sett, reb, notif, retention] = await Promise.all([
        fetchInventorySettings(),
        fetchOrderImportSettings(),
        fetchOrderImportStatusSettings(),
        fetchExpressSettings(),
        fetchSettlementSettings(),
        fetchRebateSettings(),
        fetchNotificationSettings(),
        fetchDataRetentionSettings(),
      ])
      inventory.value = inv
      orderImport.value = imp
      orderImportStatus.value = impStatus
      express.value = exp
      settlement.value = sett
      rebate.value = reb
      notification.value = notif
      dataRetention.value = retention
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  function applyInventory(settings: EcInventorySettings) {
    inventory.value = settings
    loaded.value = true
  }

  function applyOrderImport(settings: EcOrderImportSettings) {
    orderImport.value = settings
    loaded.value = true
  }

  function applyOrderImportStatus(settings: EcOrderImportStatusSettings) {
    orderImportStatus.value = settings
    loaded.value = true
  }

  function applyExpress(settings: EcExpressSettings) {
    express.value = settings
    loaded.value = true
  }

  function applySettlement(settings: EcSettlementSettings) {
    settlement.value = settings
    loaded.value = true
  }

  function applyRebate(settings: EcRebateSettings) {
    rebate.value = settings
    loaded.value = true
  }

  function applyNotification(settings: EcNotificationSettings) {
    notification.value = settings
    loaded.value = true
  }

  function applyDataRetention(settings: EcDataRetentionSettings) {
    dataRetention.value = settings
    loaded.value = true
  }

  function invalidate() {
    loaded.value = false
  }

  return {
    loaded,
    loading,
    inventory,
    orderImport,
    orderImportStatus,
    express,
    settlement,
    rebate,
    notification,
    dataRetention,
    statusMappingForImport,
    ensureLoaded,
    applyInventory,
    applyOrderImport,
    applyOrderImportStatus,
    applyExpress,
    applySettlement,
    applyRebate,
    applyNotification,
    applyDataRetention,
    invalidate,
  }
})
