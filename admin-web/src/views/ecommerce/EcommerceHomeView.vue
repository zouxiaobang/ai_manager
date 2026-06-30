<template>
  <WarRoomPage :title="t('ecommerce.title')">
    <template #meta>
      <div class="ec-home-toolbar">
        <el-date-picker
          v-model="settlementMonth"
          type="month"
          value-format="YYYY-MM"
          :placeholder="t('ecommerce.home.monthPlaceholder')"
          class="ec-home-toolbar__month"
        />
        <div class="ec-home-toolbar__shops-wrap">
          <el-select
            v-model="selectedShopIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            filterable
            :placeholder="t('ecommerce.home.allShops')"
            class="ec-home-toolbar__shops"
            :class="{ 'has-value': selectedShopIds.length > 0 }"
          >
            <el-option v-for="shop in shopOptions" :key="shop.id" :label="shop.name" :value="shop.id" />
          </el-select>
          <span v-show="!selectedShopIds.length" class="ec-home-toolbar__shops-placeholder">
            {{ t('ecommerce.home.allShops') }}
          </span>
        </div>
      </div>
    </template>

    <section v-loading="loading" class="ec-home-stats">
      <div
        v-for="card in statCards"
        :key="card.key"
        class="ec-home-stat-card"
        :class="[`is-${card.tone}`, { 'is-clickable': card.tab }]"
        @click="card.tab && goManage(card.tab)"
      >
        <div class="ec-home-stat-card__icon" :class="`is-${card.tone}`">
          <el-icon><component :is="card.icon" /></el-icon>
        </div>
        <div class="ec-home-stat-card__body">
          <div class="ec-home-stat-card__label">{{ card.label }}</div>
          <div class="ec-home-stat-card__value" :style="{ color: card.color }">
            <CnyAmount v-if="card.moneyValue != null" variant="display" compact :value="card.moneyValue" />
            <span v-else>{{ card.value }}</span>
          </div>
          <p v-if="card.hintMoney != null" class="ec-home-stat-card__hint">
            <CnyAmount :value="card.hintMoney" />
          </p>
          <p v-else-if="card.hint" class="ec-home-stat-card__hint">{{ card.hint }}</p>
        </div>
      </div>
    </section>

    <el-alert
      v-if="settlementRemindVisible"
      class="ec-home-settlement-remind"
      type="warning"
      :title="t('ecommerce.home.settlementRemindTitle')"
      :description="t('ecommerce.home.settlementRemindDesc', { month: settlementMonth })"
      show-icon
      :closable="false"
    />

    <section class="ec-home-row">
      <div class="war-room-panel ec-home-chart-panel">
        <div class="ec-home-panel-head">
          <h2 class="ec-home-section-title">{{ t('ecommerce.home.settlementOverview') }}</h2>
          <span class="ec-home-panel-sub">{{ t('ecommerce.home.settlementOverviewSub') }}</span>
        </div>
        <div ref="chartRef" v-loading="loading" class="ec-home-chart" />
      </div>

      <div class="war-room-panel ec-home-import-panel">
        <h2 class="ec-home-section-title">{{ t('ecommerce.home.importProgress') }}</h2>
        <div v-loading="loading" class="ec-home-import-box">
          <div class="ec-home-import-timeline">
            <div
              v-for="(item, index) in importProgress"
              :key="item.key"
              class="ec-home-import-step"
              :class="{ 'is-last': index === importProgress.length - 1 }"
            >
              <div class="ec-home-import-step__track">
                <span class="ec-home-import-step__dot" :class="item.done ? 'is-done' : 'is-pending'">
                  <el-icon v-if="item.done"><Check /></el-icon>
                </span>
                <span v-if="index < importProgress.length - 1" class="ec-home-import-step__line" />
              </div>
              <div class="ec-home-import-step__title">{{ item.title }}</div>
              <div class="ec-home-import-step__date">{{ item.dateLabel }}</div>
              <button
                v-if="!item.done"
                type="button"
                class="ec-home-import-step__badge is-action"
                @click="goManage(item.tab)"
              >
                {{ t('ecommerce.home.progressActionBadge') }}
              </button>
              <span v-else class="ec-home-import-step__badge" :class="`is-${item.badgeTone}`">
                {{ item.badgeLabel }}
              </span>
            </div>
          </div>
        </div>
        <div v-if="lastRefreshAt" class="ec-home-import-footer">
          <el-icon><Clock /></el-icon>
          <span>{{ t('ecommerce.home.lastFullRefresh', { time: lastRefreshRelative }) }}</span>
        </div>
      </div>
    </section>

    <section class="ec-home-row ec-home-row--inventory">
      <div class="war-room-panel ec-home-inventory-panel">
        <div class="ec-home-panel-head">
          <h2 class="ec-home-section-title">{{ t('ecommerce.home.inventoryMonitor') }}</h2>
          <el-button link type="primary" @click="goManage('inventory')">
            {{ t('ecommerce.home.viewAllInventory') }}
          </el-button>
        </div>

        <div class="ec-home-inventory-metrics">
          <div
            v-for="metric in inventoryMetrics"
            :key="metric.key"
            class="ec-home-inventory-metric"
            :class="[`is-${metric.tone}`, { 'is-clickable': metric.clickable }]"
            @click="metric.clickable && openInventoryDialog(metric.key as 'alert' | 'zero')"
          >
            <div class="ec-home-inventory-metric__icon" :class="`is-${metric.tone}`">
              <el-icon><component :is="metric.icon" /></el-icon>
            </div>
            <div class="ec-home-inventory-metric__body">
              <div class="ec-home-inventory-metric__label">{{ metric.label }}</div>
              <div class="ec-home-inventory-metric__value" :style="{ color: metric.color }">
                <CnyAmount v-if="metric.moneyValue != null" variant="display" compact :value="metric.moneyValue" />
                <template v-else>
                  {{ metric.value }}<span v-if="metric.unit" class="ec-home-inventory-metric__unit">{{ metric.unit }}</span>
                </template>
              </div>
            </div>
          </div>
        </div>

        <div v-loading="inventoryLoading" class="ec-home-inventory-monitor-body">
          <div class="ec-home-inventory-visual">
            <div ref="inventoryChartRef" class="ec-home-inventory-donut" />
            <div class="ec-home-inventory-breakdown">
              <div
                v-for="item in inventoryBreakdown"
                :key="item.key"
                class="ec-home-inventory-breakdown__row"
              >
                <span class="ec-home-inventory-breakdown__label">{{ item.label }}</span>
                <div class="ec-home-inventory-breakdown__bar-wrap">
                  <span
                    class="ec-home-inventory-breakdown__bar"
                    :style="{ width: `${item.pct}%`, background: item.color }"
                  />
                </div>
                <span class="ec-home-inventory-breakdown__count">{{ item.count }}</span>
              </div>
            </div>
          </div>

          <div class="ec-home-inventory-alert">
            <h3 class="ec-home-inventory-alert__title">{{ t('ecommerce.home.inventoryAlert') }}</h3>
            <ul v-if="inventoryWarningList.length" class="ec-home-inventory-alert__list">
              <li v-for="row in inventoryWarningList" :key="row.id" class="ec-home-inventory-alert__item">
                <el-icon class="ec-home-inventory-alert__icon"><WarnTriangleFilled /></el-icon>
                <div class="ec-home-inventory-alert__body">
                  <div class="ec-home-inventory-alert__sku">{{ row.skuCode }}</div>
                  <div class="ec-home-inventory-alert__status">{{ inventoryWarningStatus(row) }}</div>
                </div>
                <button type="button" class="ec-home-inventory-alert__action" @click="goRestock(row)">
                  {{ t('ecommerce.home.restockAction') }}
                </button>
              </li>
            </ul>
            <p v-else-if="!inventoryLoading" class="ec-home-inventory-alert__empty">
              {{ t('ecommerce.home.inventoryAlertEmpty') }}
            </p>
          </div>
        </div>
      </div>

      <div class="war-room-panel ec-home-shortcuts-panel">
        <h2 class="ec-home-section-title">{{ t('ecommerce.home.shortcuts') }}</h2>
        <div class="ec-home-shortcuts">
          <button
            v-for="entry in shortcuts"
            :key="entry.tab"
            type="button"
            class="ec-home-shortcut"
            :class="`is-${entry.tone}`"
            @click="goManage(entry.tab)"
          >
            <span class="ec-home-shortcut__icon-wrap">
              <el-icon class="ec-home-shortcut__icon"><component :is="entry.icon" /></el-icon>
            </span>
            <span class="ec-home-shortcut__label">{{ t(entry.labelKey) }}</span>
          </button>
        </div>
      </div>
    </section>

    <el-dialog
      v-model="inventoryDialogVisible"
      :title="inventoryDialogTitle"
      width="720px"
      destroy-on-close
    >
      <el-table v-loading="inventoryDialogLoading" :data="inventoryDialogRows" stripe border size="small" max-height="420">
        <el-table-column prop="productName" :label="t('ecommerce.inventory.productName')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="skuCode" :label="t('ecommerce.inventory.skuCode')" width="120" />
        <el-table-column prop="specName" :label="t('ecommerce.inventory.specName')" width="110" show-overflow-tooltip />
        <el-table-column prop="quantity" :label="t('ecommerce.inventory.quantity')" width="90" align="right" />
        <el-table-column :label="t('ecommerce.inventory.alertThreshold')" width="90" align="right">
          <template #default="{ row }">{{ row.alertThreshold ?? 0 }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </WarRoomPage>
</template>

<script setup lang="ts">
import { computed, markRaw, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  Box,
  Check,
  Clock,
  Coin,
  DataAnalysis,
  GoodsFilled,
  OfficeBuilding,
  RemoveFilled,
  Shop,
  TakeawayBox,
  Tickets,
  TrendCharts,
  Van,
  WalletFilled,
  WarnTriangleFilled,
} from '@element-plus/icons-vue'
import type { ECharts } from 'echarts/core'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
import CnyAmount from '@/components/CnyAmount.vue'
import { fetchShopOptions, type EcShop } from '@/api/ecommerce/shop'
import {
  fetchExpressBillImported,
  fetchExpressBillRecords,
  fetchMonthlySettlement,
  type MonthlySettlementShopSummary,
} from '@/api/ecommerce/monthlySettlement'
import { fetchInventories, fetchInventoryFactorySummary, type EcInventory } from '@/api/ecommerce/inventory'
import { echarts } from '@/utils/echarts'
import { ecommercePathForModule, type EcommerceWorkbenchModule } from '@/data/ecommerce-nav'
import {
  computeInventoryHealthScore,
  computeInventoryStats,
  type InventoryStatusKey,
} from '@/utils/inventoryStats'
import { useEcSettingsStore } from '@/stores/ecSettings'

interface AggregatedSettlement {
  totalRevenue: number
  actualTotalCost: number
  estimatedTotalProfit: number
  actualTotalProfit: number
  totalActualFreight: number
  includedOrderCount: number
  excludedOrderCount: number
  pendingOrderCount: number
  totalOrderCount: number
}

const { t } = useI18n()
const router = useRouter()
const ecSettings = useEcSettingsStore()

const inventoryClassificationOptions = computed(() => ({
  defaultAlertThreshold: ecSettings.inventory.defaultAlertThreshold,
  slowMovingDays: ecSettings.inventory.slowMovingDays,
  slowMovingFallbackDays: ecSettings.inventory.slowMovingFallbackDays,
}))

const settlementMonth = ref(shiftMonth(formatMonth(new Date()), -1))
const selectedShopIds = ref<number[]>([])
const shopOptions = ref<EcShop[]>([])
const loading = ref(false)
const inventoryLoading = ref(false)
const lastRefreshAt = ref<Date | null>(null)

const currentShops = ref<MonthlySettlementShopSummary[]>([])
const previousShops = ref<MonthlySettlementShopSummary[]>([])
const expressBillImported = ref(false)
const expressBillImportTime = ref<string | null>(null)
const inventoryWarningList = ref<EcInventory[]>([])
const inventoryAllItems = ref<EcInventory[]>([])
const zeroStockCount = ref(0)
const inventoryStockValue = ref(0)
const inventoryAlertCount = ref(0)
const inventorySkuCount = ref(0)

const inventoryDialogVisible = ref(false)
const inventoryDialogLoading = ref(false)
const inventoryDialogType = ref<'alert' | 'zero'>('alert')
const inventoryDialogRows = ref<EcInventory[]>([])

const chartRef = ref<HTMLElement | null>(null)
const inventoryChartRef = ref<HTMLElement | null>(null)
let chart: ECharts | null = null
let inventoryChart: ECharts | null = null

const filteredCurrentShops = computed(() => filterShops(currentShops.value))
const filteredPreviousShops = computed(() => filterShops(previousShops.value))
const currentAgg = computed(() => aggregateSettlement(filteredCurrentShops.value))
const previousAgg = computed(() => aggregateSettlement(filteredPreviousShops.value))

const displayNetProfit = computed(() => {
  if (ecSettings.settlement.profitDisplayMode === 'ESTIMATED') {
    return currentAgg.value.estimatedTotalProfit
  }
  return currentAgg.value.actualTotalProfit
})

const netProfitHint = computed(() => {
  if (ecSettings.settlement.profitDisplayMode === 'ESTIMATED') {
    return t('ecommerce.home.netProfitEstimatedHint')
  }
  return t('ecommerce.home.netProfitHint')
})

const settlementRemindVisible = computed(() => {
  if (!ecSettings.notification.settlementRemindEnabled) return false
  if (settlementStatus.value.tone === 'green') return false
  const today = new Date().getDate()
  return today >= ecSettings.notification.settlementRemindDayOfMonth
})

const grossMarginPct = computed(() => {
  const revenue = currentAgg.value.totalRevenue
  if (revenue <= 0) return null
  return ((revenue - currentAgg.value.actualTotalCost) / revenue) * 100
})

const settlementStatus = computed(() => {
  if (currentAgg.value.totalOrderCount <= 0) {
    return { label: t('ecommerce.home.statusNoData'), tone: 'gray' as const }
  }
  if (!expressBillImported.value) {
    return { label: t('ecommerce.home.statusPendingExpress'), tone: 'orange' as const }
  }
  if (currentAgg.value.pendingOrderCount > 0) {
    return { label: t('ecommerce.home.statusPendingSettlement'), tone: 'orange' as const }
  }
  return { label: t('ecommerce.home.statusReady'), tone: 'green' as const }
})

const statCards = computed(() => [
  {
    key: 'revenue',
    label: t('ecommerce.home.monthRevenue'),
    moneyValue: currentAgg.value.totalRevenue,
    value: '',
    hint: t('ecommerce.home.importedOrdersHint', { count: currentAgg.value.includedOrderCount }),
    color: '#d97706',
    tone: 'amber',
    icon: markRaw(WalletFilled),
    tab: 'monthlySettlement' as EcommerceWorkbenchModule,
  },
  {
    key: 'gross',
    label: t('ecommerce.home.grossProfit'),
    value: grossMarginPct.value == null ? '—' : `${grossMarginPct.value.toFixed(1)}%`,
    hintMoney: currentAgg.value.totalRevenue - currentAgg.value.actualTotalCost,
    color: '#16a34a',
    tone: 'green',
    icon: markRaw(TrendCharts),
    tab: 'monthlySettlement' as EcommerceWorkbenchModule,
  },
  {
    key: 'net',
    label: t('ecommerce.home.netProfit'),
    moneyValue: displayNetProfit.value,
    value: '',
    hint: netProfitHint.value,
    color: '#2563eb',
    tone: 'blue',
    icon: markRaw(Coin),
    tab: 'monthlySettlement' as EcommerceWorkbenchModule,
  },
  {
    key: 'status',
    label: t('ecommerce.home.settlementStatus'),
    value: settlementStatus.value.label,
    hint: undefined,
    color: settlementStatus.value.tone === 'green' ? '#16a34a' : settlementStatus.value.tone === 'orange' ? '#ea580c' : '#6b7280',
    tone: settlementStatus.value.tone,
    icon: markRaw(DataAnalysis),
    tab: 'monthlySettlement' as EcommerceWorkbenchModule,
  },
])

const importProgress = computed(() => {
  const orderCount = currentAgg.value.totalOrderCount
  const orderDone = orderCount > 0
  const expressDone = expressBillImported.value
  const settlementDone = expressDone && orderCount > 0 && currentAgg.value.pendingOrderCount === 0

  return [
    {
      key: 'orders',
      tab: 'order' as EcommerceWorkbenchModule,
      done: orderDone,
      title: t('ecommerce.home.progressOrders'),
      dateLabel: orderDone ? formatMonthDay(lastRefreshAt.value) : '—',
      badgeLabel: t('ecommerce.home.progressOrdersBadge', { count: orderCount }),
      badgeTone: 'success',
    },
    {
      key: 'express',
      tab: 'express' as EcommerceWorkbenchModule,
      done: expressDone,
      title: t('ecommerce.home.progressExpress'),
      dateLabel: expressDone ? formatMonthDay(expressBillImportTime.value) : '—',
      badgeLabel: t('ecommerce.home.progressExpressBadge'),
      badgeTone: 'success',
    },
    {
      key: 'settlement',
      tab: 'monthlySettlement' as EcommerceWorkbenchModule,
      done: settlementDone,
      title: t('ecommerce.home.progressSettlement'),
      dateLabel: settlementDone ? formatMonthDay(lastRefreshAt.value) : '—',
      badgeLabel: t('ecommerce.home.progressSettlementBadge'),
      badgeTone: 'success',
    },
  ]
})

const lastRefreshRelative = computed(() => formatRelativeTime(lastRefreshAt.value))

const inventoryDialogTitle = computed(() =>
  inventoryDialogType.value === 'alert'
    ? t('ecommerce.home.inventoryAlertDialog')
    : t('ecommerce.home.inventoryZeroDialog'),
)

const inventoryStatusStats = computed(() =>
  computeInventoryStats(inventoryAllItems.value, inventoryClassificationOptions.value),
)

const inventoryBreakdown = computed(() => {
  const stats = inventoryStatusStats.value
  const max = Math.max(stats.normal, stats.low, stats.zero, stats.slow, 1)
  const rows: Array<{ key: InventoryStatusKey; label: string; count: number; color: string; pct: number }> = [
    { key: 'normal', label: t('ecommerce.home.inventoryNormal'), count: stats.normal, color: '#2563eb', pct: 0 },
    { key: 'low', label: t('ecommerce.home.inventoryLow'), count: stats.low, color: '#ea580c', pct: 0 },
    { key: 'zero', label: t('ecommerce.home.inventoryZeroShort'), count: stats.zero, color: '#dc2626', pct: 0 },
    { key: 'slow', label: t('ecommerce.home.inventorySlow'), count: stats.slow, color: '#9ca3af', pct: 0 },
  ]
  for (const row of rows) {
    row.pct = (row.count / max) * 100
  }
  return rows
})

const inventoryMetrics = computed(() => [
  {
    key: 'alert',
    tone: 'red',
    label: t('ecommerce.home.inventoryAlert'),
    value: String(inventoryAlertCount.value),
    unit: 'SKU',
    color: '#dc2626',
    icon: markRaw(WarnTriangleFilled),
    clickable: true,
  },
  {
    key: 'zero',
    tone: 'orange',
    label: t('ecommerce.home.inventoryZero'),
    value: String(zeroStockCount.value),
    unit: 'SKU',
    color: '#ea580c',
    icon: markRaw(RemoveFilled),
    clickable: true,
  },
  {
    key: 'value',
    tone: 'blue',
    label: t('ecommerce.home.inventoryValue'),
    moneyValue: inventoryStockValue.value,
    value: '',
    unit: '',
    color: '#2563eb',
    icon: markRaw(WalletFilled),
    clickable: false,
  },
  {
    key: 'sku',
    tone: 'gray',
    label: t('ecommerce.home.inventorySku'),
    value: String(inventorySkuCount.value),
    unit: '',
    color: '#4b5563',
    icon: markRaw(GoodsFilled),
    clickable: false,
  },
])

const shortcuts = [
  { tab: 'monthlySettlement' as EcommerceWorkbenchModule, labelKey: 'ecommerce.tabs.monthlySettlement', icon: markRaw(DataAnalysis), tone: 'purple' },
  { tab: 'order' as EcommerceWorkbenchModule, labelKey: 'ecommerce.tabs.order', icon: markRaw(Tickets), tone: 'blue' },
  { tab: 'inventory' as EcommerceWorkbenchModule, labelKey: 'ecommerce.tabs.inventory', icon: markRaw(Box), tone: 'red' },
  { tab: 'product' as EcommerceWorkbenchModule, labelKey: 'ecommerce.tabs.product', icon: markRaw(GoodsFilled), tone: 'green' },
  { tab: 'express' as EcommerceWorkbenchModule, labelKey: 'ecommerce.tabs.express', icon: markRaw(Van), tone: 'orange' },
  { tab: 'factory' as EcommerceWorkbenchModule, labelKey: 'ecommerce.tabs.factory', icon: markRaw(OfficeBuilding), tone: 'blue' },
  { tab: 'platformShop' as EcommerceWorkbenchModule, labelKey: 'ecommerce.tabs.platformShop', icon: markRaw(Shop), tone: 'gray' },
  { tab: 'carton' as EcommerceWorkbenchModule, labelKey: 'ecommerce.tabs.carton', icon: markRaw(TakeawayBox), tone: 'orange' },
]

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

function formatMonthDay(value?: Date | string | null) {
  if (!value) return '—'
  const d = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

function formatRelativeTime(value?: Date | null) {
  if (!value) return '—'
  const diffMs = Date.now() - value.getTime()
  const minutes = Math.floor(diffMs / 60000)
  if (minutes < 1) return t('ecommerce.home.justNow')
  if (minutes < 60) return t('ecommerce.home.minutesAgo', { count: minutes })
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return t('ecommerce.home.hoursAgo', { count: hours })
  const days = Math.floor(hours / 24)
  return t('ecommerce.home.daysAgo', { count: days })
}

function filterShops(shops: MonthlySettlementShopSummary[]) {
  if (!selectedShopIds.value.length) return shops
  const idSet = new Set(selectedShopIds.value)
  return shops.filter((shop) => shop.shopId != null && idSet.has(shop.shopId))
}

function aggregateSettlement(shops: MonthlySettlementShopSummary[]): AggregatedSettlement {
  return shops.reduce<AggregatedSettlement>(
    (acc, shop) => {
      const included = shop.includedOrderCount ?? 0
      const excluded = shop.excludedOrderCount ?? 0
      const pending = shop.pendingOrderCount ?? 0
      acc.totalRevenue += Number(shop.totalRevenue ?? 0)
      acc.actualTotalCost += Number(shop.actualTotalCost ?? 0)
      acc.estimatedTotalProfit += Number(shop.estimatedTotalProfit ?? 0)
      acc.actualTotalProfit += Number(shop.actualTotalProfit ?? 0)
      acc.totalActualFreight += Number(shop.totalActualFreight ?? 0)
      acc.includedOrderCount += included
      acc.excludedOrderCount += excluded
      acc.pendingOrderCount += pending
      acc.totalOrderCount += included + excluded + pending
      return acc
    },
    {
      totalRevenue: 0,
      actualTotalCost: 0,
      estimatedTotalProfit: 0,
      actualTotalProfit: 0,
      totalActualFreight: 0,
      includedOrderCount: 0,
      excludedOrderCount: 0,
      pendingOrderCount: 0,
      totalOrderCount: 0,
    },
  )
}

function goManage(tab: EcommerceWorkbenchModule) {
  router.push(ecommercePathForModule(tab))
}

function inventoryWarningStatus(row: EcInventory) {
  if ((row.quantity ?? 0) <= 0) return t('ecommerce.home.zeroStock')
  return t('ecommerce.home.availableQty', { count: row.quantity ?? 0 })
}

function goRestock(_row: EcInventory) {
  goManage('inventory')
}

function buildInventoryWarningList(alertItems: EcInventory[], allItems: EcInventory[]) {
  const warningMap = new Map<number, EcInventory>()
  for (const row of allItems) {
    if ((row.quantity ?? 0) <= 0) {
      warningMap.set(row.id, row)
    }
  }
  for (const row of alertItems) {
    if ((row.quantity ?? 0) > 0 && row.alertActive) {
      warningMap.set(row.id, row)
    }
  }
  return Array.from(warningMap.values())
    .sort((a, b) => {
      const aZero = (a.quantity ?? 0) <= 0
      const bZero = (b.quantity ?? 0) <= 0
      if (aZero !== bZero) return aZero ? -1 : 1
      return (a.quantity ?? 0) - (b.quantity ?? 0)
    })
    .slice(0, 8)
}

function renderInventoryChart() {
  const el = inventoryChartRef.value
  if (!el) return
  if (!inventoryChart) inventoryChart = echarts.init(el)

  const stats = inventoryStatusStats.value
  const centerLabel = t('ecommerce.home.totalInventoryLabel')
  const centerTotal = inventorySkuCount.value
  const centerFormatter = () => `{total|${centerTotal}}\n{label|${centerLabel}}`
  const centerRich = {
    total: {
      fontSize: 28,
      fontWeight: 700,
      color: '#111827',
      lineHeight: 34,
    },
    label: {
      fontSize: 12,
      color: '#6b7280',
      lineHeight: 18,
    },
  }
  const segments = [
    { value: stats.normal, name: t('ecommerce.home.inventoryNormal'), color: '#2563eb' },
    { value: stats.low, name: t('ecommerce.home.inventoryLow'), color: '#ea580c' },
    { value: stats.zero, name: t('ecommerce.home.inventoryZeroShort'), color: '#dc2626' },
    { value: stats.slow, name: t('ecommerce.home.inventorySlow'), color: '#9ca3af' },
  ].filter((item) => item.value > 0)

  inventoryChart.setOption(
    {
      animation: true,
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['58%', '78%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: true,
          label: {
            show: true,
            position: 'center',
            formatter: centerFormatter,
            rich: centerRich,
          },
          emphasis: {
            scale: true,
            label: {
              show: true,
              formatter: centerFormatter,
              rich: centerRich,
            },
          },
          labelLine: { show: false },
          data: segments.length
            ? segments.map((item) => ({
                value: item.value,
                name: item.name,
                itemStyle: { color: item.color },
              }))
            : [{ value: 1, name: '', itemStyle: { color: '#eef2f7' }, tooltip: { show: false } }],
        },
      ],
    },
    true,
  )
}

async function openInventoryDialog(type: 'alert' | 'zero') {
  inventoryDialogType.value = type
  inventoryDialogVisible.value = true
  inventoryDialogLoading.value = true
  try {
    if (type === 'alert') {
      const page = await fetchInventories(undefined, true, undefined, { page: 1, pageSize: 200 })
      inventoryDialogRows.value = page.records ?? []
    } else {
      const page = await fetchInventories(undefined, false, undefined, { page: 1, pageSize: 500 })
      inventoryDialogRows.value = (page.records ?? []).filter((row) => (row.quantity ?? 0) <= 0)
    }
  } finally {
    inventoryDialogLoading.value = false
  }
}

function renderChart() {
  const el = chartRef.value
  if (!el) return
  if (!chart) chart = echarts.init(el)

  const cur = currentAgg.value
  const prev = previousAgg.value
  const hasData =
    cur.totalRevenue > 0 ||
    prev.totalRevenue > 0 ||
    cur.totalActualFreight > 0 ||
    prev.totalActualFreight > 0

  chart.setOption({
    animation: true,
    grid: { left: 52, right: 16, top: 36, bottom: 28 },
    legend: {
      data: [t('ecommerce.home.chartCurrent'), t('ecommerce.home.chartPrevious')],
      top: 0,
    },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: [
        t('ecommerce.home.chartRevenue'),
        t('ecommerce.home.chartCost'),
        t('ecommerce.home.chartExpress'),
        t('ecommerce.home.chartProfit'),
      ],
    },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: (v: number) => (v >= 10000 ? `${v / 10000}万` : String(v)) },
    },
    series: [
      {
        name: t('ecommerce.home.chartCurrent'),
        type: 'bar',
        barMaxWidth: 32,
        itemStyle: { color: '#f59e0b', borderRadius: [4, 4, 0, 0] },
        data: hasData
          ? [cur.totalRevenue, cur.actualTotalCost, cur.totalActualFreight, cur.actualTotalProfit]
          : [0, 0, 0, 0],
      },
      {
        name: t('ecommerce.home.chartPrevious'),
        type: 'bar',
        barMaxWidth: 32,
        itemStyle: { color: '#94a3b8', borderRadius: [4, 4, 0, 0] },
        data: hasData
          ? [prev.totalRevenue, prev.actualTotalCost, prev.totalActualFreight, prev.actualTotalProfit]
          : [0, 0, 0, 0],
      },
    ],
  })
}

async function loadShops() {
  shopOptions.value = await fetchShopOptions()
}

async function loadSettlement() {
  if (!settlementMonth.value) return
  const prevMonth = shiftMonth(settlementMonth.value, -1)
  const [current, previous, imported, expressRecords] = await Promise.all([
    fetchMonthlySettlement(settlementMonth.value),
    fetchMonthlySettlement(prevMonth),
    fetchExpressBillImported(settlementMonth.value),
    fetchExpressBillRecords(settlementMonth.value),
  ])
  currentShops.value = current?.shops ?? []
  previousShops.value = previous?.shops ?? []
  expressBillImported.value = !!imported
  const latestRecord = [...(expressRecords ?? [])].sort((a, b) => {
    const ta = a.createTime ? new Date(a.createTime).getTime() : 0
    const tb = b.createTime ? new Date(b.createTime).getTime() : 0
    return tb - ta
  })[0]
  expressBillImportTime.value = latestRecord?.createTime ?? null
}

async function loadInventory() {
  inventoryLoading.value = true
  try {
    const [alertPage, allPage, factorySummary] = await Promise.all([
      fetchInventories(undefined, true, undefined, { page: 1, pageSize: 50 }),
      fetchInventories(undefined, false, undefined, { page: 1, pageSize: 500 }),
      fetchInventoryFactorySummary(),
    ])
    const alertItems = alertPage.records ?? []
    const allItems = allPage.records ?? []
    inventoryAllItems.value = allItems
    inventoryWarningList.value = buildInventoryWarningList(alertItems, allItems)
    inventoryAlertCount.value = alertPage.total ?? 0
    zeroStockCount.value = allItems.filter((row) => (row.quantity ?? 0) <= 0).length
    inventorySkuCount.value = allPage.total ?? allItems.length
    inventoryStockValue.value = (factorySummary ?? []).reduce(
      (sum, row) => sum + Number(row.totalStockValue ?? 0),
      0,
    )
  } finally {
    inventoryLoading.value = false
    await nextTick()
    renderInventoryChart()
  }
}

async function load() {
  loading.value = true
  try {
    await ecSettings.ensureLoaded()
    await Promise.all([loadSettlement(), loadInventory()])
    lastRefreshAt.value = new Date()
    renderChart()
  } finally {
    loading.value = false
  }
}

function onResize() {
  chart?.resize()
  inventoryChart?.resize()
}

watch([settlementMonth, selectedShopIds], () => {
  void load()
}, { deep: true })

watch([filteredCurrentShops, filteredPreviousShops], () => renderChart())

watch(inventoryStatusStats, () => renderInventoryChart())

onMounted(async () => {
  await loadShops()
  await load()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
  inventoryChart?.dispose()
  inventoryChart = null
})
</script>

<style scoped lang="scss">
.ec-home-settlement-remind {
  margin-bottom: 16px;
}

.ec-home-toolbar {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.ec-home-toolbar__month {
  width: 140px;
  min-width: 140px;
  flex-shrink: 0;
}

.ec-home-toolbar__shops-wrap {
  position: relative;
  width: 280px;
  min-width: 280px;
  max-width: 280px;
  flex: 0 0 280px;
}

.ec-home-toolbar__shops-placeholder {
  position: absolute;
  left: 12px;
  right: 28px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--el-text-color-placeholder);
  font-size: 14px;
  line-height: 1;
  pointer-events: none;
  z-index: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ec-home-toolbar__shops {
  width: 100%;

  :deep(.el-select__wrapper) {
    width: 100%;
  }

  :deep(.el-select__selection) {
    flex-wrap: nowrap;
  }

  :deep(.el-select__selected-item) {
    max-width: 170px;
  }

  &:not(.has-value) :deep(.el-select__placeholder) {
    opacity: 0;
  }
}

.ec-home-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.ec-home-stat-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 16px;
  background: var(--wr-card);
  border: 1px solid var(--wr-border);
  border-radius: 12px;
  box-shadow: var(--wr-shadow);

  &.is-clickable {
    cursor: pointer;
    transition: border-color 0.15s ease, box-shadow 0.15s ease;

    &:hover {
      border-color: color-mix(in srgb, var(--wr-border) 70%, #f59e0b);
      box-shadow: 0 4px 14px rgb(245 158 11 / 10%);
    }
  }
}

.ec-home-stat-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 22px;

  &.is-amber {
    background: #fff7ed;
    color: #d97706;
  }

  &.is-green {
    background: #f0fdf4;
    color: #16a34a;
  }

  &.is-blue {
    background: #eff6ff;
    color: #2563eb;
  }

  &.is-orange,
  &.is-gray {
    background: #f3f4f6;
    color: #6b7280;
  }
}

.ec-home-stat-card__body {
  min-width: 0;
  flex: 1;
}

.ec-home-stat-card__label {
  font-size: 13px;
  color: var(--wr-muted);
  margin-bottom: 6px;
}

.ec-home-stat-card__value {
  font-size: clamp(18px, 2.4vw, 24px);
  font-weight: 700;
  line-height: 1.2;
  max-width: 100%;
  overflow: hidden;

  :deep(.cny-amount) {
    max-width: 100%;
  }
}

.ec-home-stat-card__hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--wr-muted);
}

.ec-home-row {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.ec-home-row--inventory {
  grid-template-columns: 1.5fr 1fr;
}

.ec-home-section-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.ec-home-import-panel > .ec-home-section-title,
.ec-home-shortcuts-panel > .ec-home-section-title {
  margin-bottom: 18px;
}

.ec-home-panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.ec-home-panel-sub {
  font-size: 12px;
  color: var(--wr-muted);
}

.ec-home-chart {
  height: 300px;
}

.ec-home-import-box {
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #fafbfc;
  padding: 18px 16px 10px;
}

.ec-home-import-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.ec-home-import-step {
  display: grid;
  grid-template-columns: 28px 1fr auto auto;
  align-items: center;
  gap: 12px;
  min-height: 52px;
  padding-bottom: 18px;
  position: relative;

  &.is-last {
    padding-bottom: 0;
  }
}

.ec-home-import-step__track {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  min-height: 52px;
}

.ec-home-import-step__dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  z-index: 1;

  &.is-done {
    background: #22c55e;
    color: #fff;
    border: none;
  }

  &.is-pending {
    background: #fff;
    border: 2px solid #d1d5db;
    color: transparent;
  }
}

.ec-home-import-step__line {
  position: absolute;
  top: 22px;
  bottom: -18px;
  width: 2px;
  background: #e5e7eb;
}

.ec-home-import-step__title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.ec-home-import-step__date {
  font-size: 13px;
  color: #9ca3af;
  text-align: center;
  min-width: 56px;
}

.ec-home-import-step__badge {
  justify-self: end;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;

  &.is-success {
    background: #dcfce7;
    color: #16a34a;
  }

  &.is-warning {
    background: #ffedd5;
    color: #ea580c;
  }

  &.is-action {
    background: #ffedd5;
    color: #ea580c;
    border: none;
    cursor: pointer;
    transition: background 0.15s ease;

    &:hover {
      background: #fed7aa;
    }
  }
}

.ec-home-import-footer {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 14px;
  font-size: 12px;
  color: #9ca3af;
}

.ec-home-import-panel > .ec-home-section-title,
.ec-home-shortcuts-panel > .ec-home-section-title,
.ec-home-inventory-panel > .ec-home-panel-head {
  margin-bottom: 18px;
}

.ec-home-inventory-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.ec-home-inventory-metric {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 96px;
  padding: 16px 12px;
  border-radius: 12px;
  border: 1px solid transparent;
  overflow: hidden;

  &.is-red {
    background: #fef2f2;
    border-color: #fecaca;
  }

  &.is-orange {
    background: #fffbeb;
    border-color: #fde68a;
  }

  &.is-blue {
    background: #eff6ff;
    border-color: #bfdbfe;
  }

  &.is-gray {
    background: #f3f4f6;
    border-color: #d1d5db;
  }

  &.is-clickable {
    cursor: pointer;
    transition: border-color 0.15s ease, box-shadow 0.15s ease;

    &:hover {
      box-shadow: 0 2px 8px rgb(15 23 42 / 6%);
    }
  }
}

.ec-home-inventory-metric__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 24px;
  font-size: 24px;
  background: transparent;

  &.is-red {
    color: #dc2626;
  }

  &.is-orange {
    color: #ea580c;
  }

  &.is-blue {
    color: #2563eb;
  }

  &.is-gray {
    color: #4b5563;
  }
}

.ec-home-inventory-metric__body {
  flex: 1;
  min-width: 0;
  text-align: left;
}

.ec-home-inventory-metric__label {
  font-size: 13px;
  color: var(--wr-text);
  margin-bottom: 8px;
  line-height: 1.3;
}

.ec-home-inventory-metric__value {
  font-size: clamp(16px, 2.2vw, 24px);
  font-weight: 700;
  line-height: 1.2;
  max-width: 100%;
  overflow: hidden;

  :deep(.cny-amount) {
    max-width: 100%;
  }
}

.ec-home-inventory-metric__unit {
  margin-left: 4px;
  font-size: 16px;
  font-weight: 600;
}

.ec-home-inventory-monitor-body {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(260px, 0.85fr);
  gap: 14px;
  align-items: stretch;
}

.ec-home-inventory-visual {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 220px;
  padding: 16px;
  border: 1px solid #e8ecf2;
  border-radius: 12px;
  background: #fafbfc;
}

.ec-home-inventory-donut {
  width: 168px;
  height: 168px;
  flex-shrink: 0;
}

.ec-home-inventory-breakdown {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ec-home-inventory-breakdown__row {
  display: grid;
  grid-template-columns: 72px 1fr 36px;
  align-items: center;
  gap: 10px;
}

.ec-home-inventory-breakdown__label {
  font-size: 13px;
  color: #374151;
  white-space: nowrap;
}

.ec-home-inventory-breakdown__bar-wrap {
  height: 8px;
  border-radius: 999px;
  background: #eef2f7;
  overflow: hidden;
}

.ec-home-inventory-breakdown__bar {
  display: block;
  height: 100%;
  border-radius: 999px;
  min-width: 4px;
  transition: width 0.3s ease;
}

.ec-home-inventory-breakdown__count {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  text-align: right;
}

.ec-home-inventory-alert {
  border: 1px solid #e8ecf2;
  border-radius: 12px;
  background: #fafbfc;
  padding: 16px;
  min-height: 220px;
  display: flex;
  flex-direction: column;
}

.ec-home-inventory-alert__title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--wr-text);
}

.ec-home-inventory-alert__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  min-height: 0;
  max-height: 280px;
  overflow: auto;
}

.ec-home-inventory-alert__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 10px;
}

.ec-home-inventory-alert__icon {
  flex-shrink: 0;
  font-size: 20px;
  color: #ea580c;
}

.ec-home-inventory-alert__body {
  flex: 1;
  min-width: 0;
}

.ec-home-inventory-alert__sku {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  line-height: 1.3;
}

.ec-home-inventory-alert__status {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.ec-home-inventory-alert__action {
  flex-shrink: 0;
  padding: 6px 14px;
  border: 1px solid #fdba74;
  border-radius: 8px;
  background: #fff;
  color: #ea580c;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease;

  &:hover {
    background: #fff7ed;
    border-color: #fb923c;
  }
}

.ec-home-inventory-alert__empty {
  margin: 0;
  padding: 32px 12px;
  text-align: center;
  font-size: 13px;
  color: var(--wr-muted);
}

.ec-home-shortcuts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.ec-home-shortcut {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 108px;
  padding: 18px 12px;
  border: 1px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  text-align: center;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &.is-red {
    background: #fef2f2;
    border-color: #fecaca;

    .ec-home-shortcut__icon {
      color: #dc2626;
    }
  }

  &.is-orange {
    background: #fffbeb;
    border-color: #fde68a;

    .ec-home-shortcut__icon {
      color: #ea580c;
    }
  }

  &.is-blue {
    background: #eff6ff;
    border-color: #bfdbfe;

    .ec-home-shortcut__icon {
      color: #2563eb;
    }
  }

  &.is-gray {
    background: #f3f4f6;
    border-color: #d1d5db;

    .ec-home-shortcut__icon {
      color: #4b5563;
    }
  }

  &.is-purple {
    background: #f5f3ff;
    border-color: #ddd6fe;

    .ec-home-shortcut__icon {
      color: #7c3aed;
    }
  }

  &.is-green {
    background: #f0fdf4;
    border-color: #bbf7d0;

    .ec-home-shortcut__icon {
      color: #16a34a;
    }
  }

  &:hover {
    box-shadow: 0 2px 8px rgb(15 23 42 / 6%);
  }
}

.ec-home-shortcut__icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
}

.ec-home-shortcut__icon {
  font-size: 28px;
}

.ec-home-shortcut__label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

@media (max-width: 1100px) {
  .ec-home-stats,
  .ec-home-inventory-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ec-home-inventory-monitor-body {
    grid-template-columns: 1fr;
  }

  .ec-home-row,
  .ec-home-row--inventory {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .ec-home-inventory-visual {
    flex-direction: column;
    align-items: stretch;
  }

  .ec-home-inventory-donut {
    align-self: center;
  }
}
</style>
