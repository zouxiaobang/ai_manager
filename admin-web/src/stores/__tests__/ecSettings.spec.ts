import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useEcSettingsStore } from '@/stores/ecSettings'
import {
  fetchDataRetentionSettings,
  fetchExpressSettings,
  fetchInventorySettings,
  fetchNotificationSettings,
  fetchOrderImportSettings,
  fetchOrderImportStatusSettings,
  fetchRebateSettings,
  fetchSettlementSettings,
} from '@/api/ecommerce/ecSettings'

vi.mock('@/api/ecommerce/ecSettings', () => ({
  fetchInventorySettings: vi.fn(),
  fetchOrderImportSettings: vi.fn(),
  fetchOrderImportStatusSettings: vi.fn(),
  fetchExpressSettings: vi.fn(),
  fetchSettlementSettings: vi.fn(),
  fetchRebateSettings: vi.fn(),
  fetchNotificationSettings: vi.fn(),
  fetchDataRetentionSettings: vi.fn(),
}))

function mockAllSettings() {
  vi.mocked(fetchInventorySettings).mockResolvedValue({
    defaultAlertThreshold: 5,
    slowMovingDays: 30,
    slowMovingFallbackDays: 60,
  })
  vi.mocked(fetchOrderImportSettings).mockResolvedValue({
    headerRow: 1,
    dataStartRow: 2,
    dateFormat: 'yyyy-MM-dd HH:mm:ss',
  })
  vi.mocked(fetchOrderImportStatusSettings).mockResolvedValue({
    defaultLineStatus: 'PAID',
    statusMapping: { paid: 'PAID' },
  })
  vi.mocked(fetchExpressSettings).mockResolvedValue({
    headerRow: 1,
    dataStartRow: 2,
    includeLabelPriceDefault: false,
  })
  vi.mocked(fetchSettlementSettings).mockResolvedValue({
    profitDisplayMode: 'ACTUAL_PREFERRED',
    costIncludesFreight: false,
  })
  vi.mocked(fetchRebateSettings).mockResolvedValue({ defaultRebatePct: 2 })
  vi.mocked(fetchNotificationSettings).mockResolvedValue({
    inventoryAlertEnabled: true,
    zeroStockAlertEnabled: true,
    settlementRemindEnabled: true,
    settlementRemindDayOfMonth: 25,
  })
  vi.mocked(fetchDataRetentionSettings).mockResolvedValue({
    importHistoryRetentionDays: 90,
    inventoryLogRetentionDays: 180,
    autoCleanupEnabled: false,
  })
}

describe('ecSettings store', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('ensureLoaded 并行加载 8 类设置', async () => {
    mockAllSettings()
    const store = useEcSettingsStore()

    await store.ensureLoaded()

    expect(fetchInventorySettings).toHaveBeenCalledTimes(1)
    expect(store.loaded).toBe(true)
    expect(store.loading).toBe(false)
    expect(store.inventory.defaultAlertThreshold).toBe(5)
    expect(store.rebate.defaultRebatePct).toBe(2)
  })

  it('已加载后再次 ensureLoaded 不重复请求', async () => {
    mockAllSettings()
    const store = useEcSettingsStore()

    await store.ensureLoaded()
    await store.ensureLoaded()

    expect(fetchInventorySettings).toHaveBeenCalledTimes(1)
  })

  it('force 强制重新加载', async () => {
    mockAllSettings()
    const store = useEcSettingsStore()

    await store.ensureLoaded()
    await store.ensureLoaded(true)

    expect(fetchInventorySettings).toHaveBeenCalledTimes(2)
  })

  it('applyXxx 更新对应设置并标记已加载', () => {
    const store = useEcSettingsStore()
    store.applyInventory({ defaultAlertThreshold: 1, slowMovingDays: 2, slowMovingFallbackDays: 3 })

    expect(store.inventory.defaultAlertThreshold).toBe(1)
    expect(store.loaded).toBe(true)
  })

  it('statusMappingForImport 归一化状态映射', () => {
    const store = useEcSettingsStore()
    store.applyOrderImportStatus({
      defaultLineStatus: 'PAID',
      statusMapping: { paid: 'paid', refunded: 'REFUNDED' },
    })

    const mapping = store.statusMappingForImport
    // 未知/非法值回退 PAID，合法值归一化保留
    expect(mapping.paid).toBe('PAID')
    expect(mapping.refunded).toBe('REFUNDED')
  })

  it('invalidate 重置加载标记', async () => {
    mockAllSettings()
    const store = useEcSettingsStore()

    await store.ensureLoaded()
    store.invalidate()
    expect(store.loaded).toBe(false)

    await store.ensureLoaded()
    expect(fetchInventorySettings).toHaveBeenCalledTimes(2)
  })
})
