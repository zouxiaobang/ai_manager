import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { fetchShopOptions } from '@/api/ecommerce/shop'
import {
  fetchExpressBillImported,
  fetchMonthlySettlement,
  type MonthlySettlementShopSummary,
} from '@/api/ecommerce/monthlySettlement'
import { fetchInventories } from '@/api/ecommerce/inventory'
import { fetchSalesOrders, type EcSalesOrder } from '@/api/ecommerce/salesOrder'
import type { EcommerceWorkbenchModule } from '@/data/ecommerce-nav'
import { useEcSettingsStore } from '@/stores/ecSettings'
import { computeInventoryStats } from '@/utils/inventoryStats'
import {
  mobileEcommerceModules,
  mobileEcommercePathForModule,
  type MobileEcommerceModule,
} from './data/mobile-ecommerce-modules'

function formatMonth(d: Date) {
  const y = d.getFullYear()
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  return `${y}-${m}`
}

function shiftMonth(month: string, delta: number) {
  const [y, m] = month.split('-').map(Number)
  const d = new Date(y, m - 1 + delta, 1)
  return formatMonth(d)
}

function aggregateSettlement(shops: MonthlySettlementShopSummary[]) {
  return shops.reduce(
    (acc, shop) => {
      const included = shop.includedOrderCount ?? 0
      const excluded = shop.excludedOrderCount ?? 0
      const pending = shop.pendingOrderCount ?? 0
      acc.totalRevenue += Number(shop.totalRevenue ?? 0)
      acc.actualTotalCost += Number(shop.actualTotalCost ?? 0)
      acc.pendingOrderCount += pending
      acc.totalOrderCount += included + excluded + pending
      return acc
    },
    {
      totalRevenue: 0,
      actualTotalCost: 0,
      pendingOrderCount: 0,
      totalOrderCount: 0,
    },
  )
}

function formatCompactMoney(value: number) {
  if (value >= 10000) {
    return `¥${(value / 10000).toFixed(1)}万`
  }
  return `¥${Math.round(value).toLocaleString('zh-CN')}`
}

export function useMobileEcommerce() {
  const { t } = useI18n()
  const router = useRouter()
  const ecSettings = useEcSettingsStore()

  const loading = ref(false)
  const searchQuery = ref('')
  const settlementMonth = ref(shiftMonth(formatMonth(new Date()), -1))
  const expressBillImported = ref(false)
  const currentShops = ref<MonthlySettlementShopSummary[]>([])
  const pendingShipCount = ref(0)
  const inventoryAlertCount = ref(0)
  const pendingOrders = ref<EcSalesOrder[]>([])
  const inventoryWarnings = ref<Array<{ id: number; title: string; meta: string; tag: string }>>([])

  const settlementAgg = computed(() => aggregateSettlement(currentShops.value))

  const settlementStatusLabel = computed(() => {
    if (settlementAgg.value.totalOrderCount <= 0) {
      return t('ecommerce.home.statusNoData')
    }
    if (!expressBillImported.value) {
      return t('ecommerce.home.statusPendingExpress')
    }
    if (settlementAgg.value.pendingOrderCount > 0) {
      return t('ecommerce.home.statusPendingSettlement')
    }
    return t('ecommerce.home.statusReady')
  })

  const summaryPillText = computed(() =>
    t('mobile.ecommerce.summaryPill', {
      ship: pendingShipCount.value,
      settlement: expressBillImported.value
        ? t('mobile.ecommerce.settlementOk')
        : t('mobile.ecommerce.settlementPending'),
    }),
  )

  const filteredModules = computed(() => {
    const q = searchQuery.value.trim().toLowerCase()
    if (!q) return mobileEcommerceModules
    return mobileEcommerceModules.filter((item) => {
      const label = t(item.labelKey).toLowerCase()
      return label.includes(q) || item.key.toLowerCase().includes(q) || item.segment.includes(q)
    })
  })

  const overviewCards = computed(() => [
    {
      key: 'ship',
      color: '#22c55e',
      kind: 'ring' as const,
      value: t('mobile.ecommerce.pendingShipValue', { count: pendingShipCount.value }),
      label: t('mobile.ecommerce.pendingShip'),
      module: 'order' as EcommerceWorkbenchModule,
      icon: '/mobile-home/scheme-a/icon-order.svg',
    },
    {
      key: 'revenue',
      color: '#3b82f6',
      kind: 'money' as const,
      value: formatCompactMoney(settlementAgg.value.totalRevenue),
      label: t('ecommerce.home.monthRevenue'),
      module: 'monthlySettlement' as EcommerceWorkbenchModule,
      icon: '/mobile-home/scheme-a/icon-money.svg',
    },
    {
      key: 'settlement',
      color: '#f59e0b',
      kind: 'text' as const,
      value: t('mobile.ecommerce.settlementShort'),
      label: expressBillImported.value
        ? settlementStatusLabel.value
        : t('mobile.ecommerce.importExpressLink'),
      labelLink: !expressBillImported.value,
      module: 'express' as EcommerceWorkbenchModule,
      icon: '/mobile-home/scheme-a/icon-settlement.svg',
    },
    {
      key: 'inventory',
      color: '#94a3b8',
      kind: 'text' as const,
      value: t('mobile.ecommerce.inventoryShort'),
      label: t('mobile.ecommerce.inventoryAlertCount', { count: inventoryAlertCount.value }),
      module: 'inventory' as EcommerceWorkbenchModule,
      icon: '/mobile-home/scheme-a/icon-warehouse.svg',
    },
  ])

  type PendingRow = EcSalesOrder & { _warningTag?: string; _isWarning?: boolean }

  const displayPendingOrders = computed<PendingRow[]>(() => {
    const rows: PendingRow[] = [...pendingOrders.value]
    for (const warn of inventoryWarnings.value.slice(0, Math.max(0, 3 - rows.length))) {
      rows.push({
        id: -warn.id,
        orderNo: warn.title,
        shopName: warn.meta,
        status: 'PAID',
        shopId: 0,
        _warningTag: warn.tag,
        _isWarning: true,
      })
    }
    return rows.slice(0, 3)
  })

  function orderTitle(order: PendingRow) {
    if (order._isWarning) {
      return order.orderNo
    }
    const shop = order.shopName ?? order.platformName ?? '—'
    return `#${order.orderNo} · ${shop}`
  }

  function orderMeta(order: PendingRow) {
    if (order._isWarning) {
      return order.shopName ?? ''
    }
    const lineCount = order.lineCount ?? order.lines?.length ?? 0
    const amount = order.receivedAmount != null ? formatCompactMoney(Number(order.receivedAmount)) : '—'
    return t('mobile.ecommerce.orderMeta', { count: lineCount, amount })
  }

  function orderTag(order: PendingRow) {
    if (order._warningTag) return order._warningTag
    return t('mobile.ecommerce.orderTagToday')
  }

  function openModule(item: MobileEcommerceModule | EcommerceWorkbenchModule) {
    const key = typeof item === 'string' ? item : item.key
    router.push(mobileEcommercePathForModule(key))
  }

  async function loadPendingShip() {
    const [paidPage, partialPage, paidList, partialList] = await Promise.all([
      fetchSalesOrders(undefined, 'PAID', undefined, undefined, undefined, { page: 1, pageSize: 1 }),
      fetchSalesOrders(undefined, 'PARTIAL_SHIPPED', undefined, undefined, undefined, { page: 1, pageSize: 1 }),
      fetchSalesOrders(undefined, 'PAID', undefined, undefined, undefined, { page: 1, pageSize: 3 }),
      fetchSalesOrders(undefined, 'PARTIAL_SHIPPED', undefined, undefined, undefined, { page: 1, pageSize: 3 }),
    ])
    pendingShipCount.value = (paidPage.total ?? 0) + (partialPage.total ?? 0)
    const merged = [...(paidList.records ?? []), ...(partialList.records ?? [])]
    pendingOrders.value = merged.slice(0, 3)
  }

  async function loadInventory() {
    const [alertPage, allPage] = await Promise.all([
      fetchInventories(undefined, true, undefined, { page: 1, pageSize: 8 }),
      fetchInventories(undefined, false, undefined, { page: 1, pageSize: 200 }),
    ])
    inventoryAlertCount.value = alertPage.total ?? 0
    const options = {
      defaultAlertThreshold: ecSettings.inventory.defaultAlertThreshold,
      slowMovingDays: ecSettings.inventory.slowMovingDays,
      slowMovingFallbackDays: ecSettings.inventory.slowMovingFallbackDays,
    }
    computeInventoryStats(allPage.records ?? [], options)
    const warnings: typeof inventoryWarnings.value = []
    for (const row of alertPage.records ?? []) {
      if ((row.quantity ?? 0) <= 0) {
        warnings.push({
          id: row.id,
          title: row.skuCode ?? row.productName ?? '—',
          meta: t('ecommerce.home.zeroStock'),
          tag: t('mobile.ecommerce.tagWarning'),
        })
      } else if (row.alertActive) {
        warnings.push({
          id: row.id,
          title: row.skuCode ?? row.productName ?? '—',
          meta: t('ecommerce.home.availableQty', { count: row.quantity ?? 0 }),
          tag: t('mobile.ecommerce.tagWarning'),
        })
      }
      if (warnings.length >= 2) break
    }
    inventoryWarnings.value = warnings
  }

  async function loadSettlement() {
    if (!settlementMonth.value) return
    const [current, imported] = await Promise.all([
      fetchMonthlySettlement(settlementMonth.value),
      fetchExpressBillImported(settlementMonth.value),
    ])
    currentShops.value = current?.shops ?? []
    expressBillImported.value = !!imported
  }

  async function refresh() {
    loading.value = true
    try {
      await ecSettings.ensureLoaded()
      await Promise.all([loadSettlement(), loadPendingShip(), loadInventory()])
    } finally {
      loading.value = false
    }
  }

  async function init() {
    await fetchShopOptions()
    await refresh()
  }

  return {
    t,
    loading,
    searchQuery,
    summaryPillText,
    filteredModules,
    overviewCards,
    displayPendingOrders,
    pendingShipCount,
    settlementStatusLabel,
    orderTitle,
    orderMeta,
    orderTag,
    openModule,
    refresh,
    init,
  }
}
