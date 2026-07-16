<template>
  <V2Page>
    <div v-loading="loading" class="v2-ec">
      <div class="v2-ec-order-search-row">
        <div class="v2-ec-search" style="flex: 1; margin-bottom: 0;">
          <svg class="v2-ec-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input v-model="keyword" class="v2-ec-search__input" placeholder="搜索订单号 / 买家 / 平台单号 / 商品名称 / SKU..." type="search" />
        </div>
        <button type="button" class="v2-ec-order-more-btn" @click="filterDrawerVisible = true">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="18" x2="20" y2="18"/>
          </svg>
          <span>更多</span>
        </button>
      </div>

      <div class="v2-ec-stats" style="margin-bottom: 12px;">
        <div v-for="stat in statCards" :key="stat.key" class="v2-ec-stat-card" :style="{ background: getCardBg(stat.color) }" style="cursor: default;">
          <div class="v2-ec-stat-card__info">
            <div class="v2-ec-stat-card__value" :style="{ color: stat.color }">{{ stat.value }}</div>
            <div class="v2-ec-stat-card__label">{{ stat.label }}</div>
          </div>
        </div>
      </div>

      <div class="v2-ec-tabs">
        <button
          v-for="tab in statusTabs"
          :key="String(tab.id)"
          type="button"
          class="v2-ec-tab"
          :class="{ 'is-active': activeStatus === tab.id }"
          @click="activeStatus = tab.id"
        >
          {{ tab.icon }} {{ tab.name }} ({{ tab.count }})
        </button>
      </div>

      <div class="v2-ec-section-title">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--wr-text, #333)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
        </svg>
        <span>订单列表</span>
        <span style="font-size: 12px; color: var(--wr-muted, #999); font-weight: 400; margin-left: 4px;">共 {{ total }} 单</span>
      </div>

      <div v-if="loadingList && !orders.length" class="v2-ec-order-loading">
        <span>加载中...</span>
      </div>

      <template v-else>
        <div class="v2-ec-order-list">
          <div
            v-for="order in displayOrders"
            :key="order.id"
            class="v2-ec-order-card"
          >
            <div class="v2-ec-order-card__head">
              <span class="v2-ec-order-card__no">#{{ order.platformOrderNo }}</span>
              <span class="v2-ec-order-card__shop">{{ order.shopName || order.platformName || '—' }}</span>
            </div>

            <div v-if="order.trackingNumber" class="v2-ec-order-card__head">
              <span class="v2-ec-order-card__no">#{{ order.trackingNumber }}</span>
              <span class="v2-ec-order-card__express">{{ order.expressStationName || '—' }}</span>
            </div>

            <div class="v2-ec-order-card__body">
              <span class="v2-ec-order-card__link">{{ order.linkName || order.lines?.[0]?.linkName || '' }}</span>
              <span class="v2-ec-order-card__amount">￥{{ order.receivedAmount ?? '—' }}</span>
            </div>

            <div v-if="order.linkName || order.lines?.length" class="v2-ec-order-card__products">
              <span class="v2-ec-order-card__link">{{ order.skuSpecName || order.lines?.[0]?.skuSpecName || '' }}</span>
              <span v-if="order.lineCount" class="v2-ec-order-card__line-count">共{{ order.lineCount }}件</span>
            </div>

            <div class="v2-ec-order-card__footer">
              <span class="v2-ec-order-card__time">{{ formatTime(order.orderTime) }}</span>
              <span class="v2-ec-order-card__status" :style="{ background: statusColor(order.status) }">
                {{ statusLabel(order.status) }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="hasMore" class="v2-ec-order-loadmore">
          <button
            type="button"
            class="v2-ec-order-loadmore__btn"
            :disabled="loadingMore"
            @click="loadMore"
          >
            {{ loadingMore ? '加载中...' : '加载更多' }}
          </button>
        </div>

        <div v-if="!displayOrders.length" class="v2-ec-order-empty">
          <span class="v2-ec-order-empty__icon">📭</span>
          <span class="v2-ec-order-empty__text">暂无订单数据</span>
        </div>
      </template>
    </div>

    <Teleport to="body">
      <Transition name="v2-ec-fade">
        <div v-if="filterDrawerVisible" class="v2-ec-order-drawer-overlay" @click.self="filterDrawerVisible = false">
          <div class="v2-ec-order-drawer">
            <div class="v2-ec-order-drawer__handle" />
            <h3 class="v2-ec-order-drawer__title">筛选条件</h3>

            <div class="v2-ec-order-drawer__section">
              <div class="v2-ec-order-drawer__label">选择月份</div>
              <V2MonthPicker v-model="orderMonth" :disabled="loadingList" />
            </div>

            <div class="v2-ec-order-drawer__section">
              <div class="v2-ec-order-drawer__label">选择店铺</div>
              <div class="v2-ec-order-drawer__shops">
                <button
                  type="button"
                  class="v2-ec-order-drawer__shop-btn"
                  :class="{ 'is-active': shopFilter === '' }"
                  @click="shopFilter = ''; onShopFilterChange()"
                >全部店铺</button>
                <button
                  v-for="s in shopOptions"
                  :key="s.id"
                  type="button"
                  class="v2-ec-order-drawer__shop-btn"
                  :class="{ 'is-active': shopFilter === s.id }"
                  @click="shopFilter = s.id; onShopFilterChange()"
                >{{ s.name }}</button>
              </div>
            </div>

            <div class="v2-ec-order-drawer__actions">
              <button type="button" class="v2-ec-order-drawer__submit" @click="filterDrawerVisible = false">
                确定
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </V2Page>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import V2Page from '@/mobile-v2/components/V2Page.vue'
import V2MonthPicker from '@/mobile-v2/components/V2MonthPicker.vue'
import {
  fetchSalesOrders,
  fetchSalesOrderMonthlyOverview,
  type EcSalesOrder,
  type EcSalesOrderStatus,
} from '@/api/ecommerce/salesOrder.ts'
import { fetchShopOptions, type EcShop } from '@/api/ecommerce/shop.ts'

import './styles/v2-ecommerce.scss'

function formatMonth(d: Date) {
  const y = d.getFullYear()
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  return `${y}-${m}`
}

function formatTime(t?: string) {
  if (!t) return '—'
  try {
    const d = new Date(t)
    const MM = `${d.getMonth() + 1}`.padStart(2, '0')
    const DD = `${d.getDate()}`.padStart(2, '0')
    const hh = `${d.getHours()}`.padStart(2, '0')
    const mm = `${d.getMinutes()}`.padStart(2, '0')
    return `${MM}-${DD} ${hh}:${mm}`
  } catch {
    return t
  }
}

const STATUS_CFG: Record<EcSalesOrderStatus, { label: string; color: string }> = {
  DRAFT:          { label: '草稿',     color: '#94a3b8' },
  PAID:           { label: '待发货',   color: '#f97316' },
  PARTIAL_SHIPPED:{ label: '部分发货', color: '#3b82f6' },
  SHIPPED:        { label: '已发货',   color: '#3b82f6' },
  PARTIAL_REFUND: { label: '部分退款', color: '#f59e0b' },
  COMPLETED:      { label: '已完成',   color: '#22c55e' },
  CANCELLED:      { label: '已取消',   color: '#94a3b8' },
  REFUNDED:       { label: '已退款',   color: '#ef4444' },
}

function statusColor(status: EcSalesOrderStatus): string {
  return STATUS_CFG[status]?.color ?? '#94a3b8'
}

function statusLabel(status: EcSalesOrderStatus): string {
  return STATUS_CFG[status]?.label ?? status
}

const loading = ref(true)
const loadingList = ref(false)
const loadingMore = ref(false)
const filterDrawerVisible = ref(false)

const keyword = ref('')
const activeStatus = ref<string | number | null>('all')
const orderMonth = ref(formatMonth(new Date()))
const shopFilter = ref<number | ''>('')

const shopOptions = ref<EcShop[]>([])

const page = ref(1)
const pageSize = 20
const total = ref(0)
const orders = ref<EcSalesOrder[]>([])

const statCards = ref<Array<{ key: string; value: string; label: string; color: string }>>([
  { key: 'total',       value: '—', label: '总订单',   color: '#3b82f6' },
  { key: 'pendingShip', value: '—', label: '待发货',   color: '#f97316' },
  { key: 'completed',   value: '—', label: '已完成',   color: '#22c55e' },
  { key: 'revenue',     value: '—', label: '总营收',   color: '#8b5cf6' },
])

interface StatusCounts {
  total: number
  draft: number
  paid: number
  partialShipped: number
  shipped: number
  partialRefund: number
  completed: number
  refunded: number
  cancelled: number
}

const monthlyStatusCounts = ref<StatusCounts>({
  total: 0,
  draft: 0,
  paid: 0,
  partialShipped: 0,
  shipped: 0,
  partialRefund: 0,
  completed: 0,
  refunded: 0,
  cancelled: 0,
})

const statusTabs = computed(() => {
  const s = monthlyStatusCounts.value
  return [
    { id: 'all', name: '全部', icon: '📋', count: s.total },
    { id: 'DRAFT', name: '草稿', icon: '📝', count: s.draft },
    { id: 'PAID', name: '待发货', icon: '📦', count: s.paid },
    { id: 'PARTIAL_SHIPPED', name: '部分发货', icon: '🚚', count: s.partialShipped },
    { id: 'SHIPPED', name: '已发货', icon: '🚚', count: s.shipped },
    { id: 'PARTIAL_REFUND', name: '部分退款', icon: '🔙', count: s.partialRefund },
    { id: 'COMPLETED', name: '已完成', icon: '✅', count: s.completed },
    { id: 'REFUNDED', name: '已退款', icon: '↩️', count: s.refunded },
    { id: 'CANCELLED', name: '已取消', icon: '❌', count: s.cancelled },
  ]
})

const displayOrders = computed(() => {
  let items = orders.value
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    items = items.filter(o =>
      (o.orderNo?.toLowerCase() ?? '').includes(kw) ||
      (o.buyerName?.toLowerCase() ?? '').includes(kw) ||
      (o.platformOrderNo?.toLowerCase() ?? '').includes(kw) ||
      (o.linkName?.toLowerCase() ?? '').includes(kw) ||
      (o.skuSpecName?.toLowerCase() ?? '').includes(kw)
    )
  }
  return items
})

const hasMore = computed(() => orders.value.length < total.value)

watch(activeStatus, () => {
  loadOrders(true)
})

watch(orderMonth, () => {
  loadOrders(true)
  loadStats()
})

function onShopFilterChange() {
  loadOrders(true)
  loadStats()
}

async function loadOrders(resetPage = false) {
  if (resetPage) {
    page.value = 1
    orders.value = []
  }
  loadingList.value = true
  try {
    const prefix = orderMonth.value
    const from = `${prefix}-01`
    const lastDay = new Date(Number(prefix.split('-')[0]), Number(prefix.split('-')[1]), 0).getDate()
    const to = `${prefix}-${lastDay}`
    const result = await fetchSalesOrders(
      keyword.value.trim() || undefined,
      activeStatus.value !== 'all' ? String(activeStatus.value) : undefined,
      shopFilter.value || undefined,
      from,
      to,
      { page: page.value, pageSize },
    )
    if (resetPage) {
      orders.value = result.records ?? []
    } else {
      orders.value = [...orders.value, ...(result.records ?? [])]
    }
    total.value = result.total
    page.value = result.page
  } catch {
    //
  } finally {
    loadingList.value = false
    loading.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  page.value += 1
  try {
    const prefix = orderMonth.value
    const from = `${prefix}-01`
    const lastDay = new Date(Number(prefix.split('-')[0]), Number(prefix.split('-')[1]), 0).getDate()
    const to = `${prefix}-${lastDay}`
    const result = await fetchSalesOrders(
      keyword.value.trim() || undefined,
      activeStatus.value !== 'all' ? String(activeStatus.value) : undefined,
      shopFilter.value || undefined,
      from,
      to,
      { page: page.value, pageSize },
    )
    orders.value = [...orders.value, ...(result.records ?? [])]
    total.value = result.total
    page.value = result.page
  } finally {
    loadingMore.value = false
  }
}

async function loadStats() {
  try {
    const overview = await fetchSalesOrderMonthlyOverview(orderMonth.value, shopFilter.value || undefined)
    if (overview) {
      const sc = overview.statusCounts ?? {}
      monthlyStatusCounts.value = {
        total: (sc.DRAFT ?? 0) + (sc.PAID ?? 0) + (sc.PARTIAL_SHIPPED ?? 0) + (sc.SHIPPED ?? 0)
              + (sc.PARTIAL_REFUND ?? 0) + (sc.COMPLETED ?? 0) + (sc.REFUNDED ?? 0) + (sc.CANCELLED ?? 0),
        draft: sc.DRAFT ?? 0,
        paid: sc.PAID ?? 0,
        partialShipped: sc.PARTIAL_SHIPPED ?? 0,
        shipped: sc.SHIPPED ?? 0,
        partialRefund: sc.PARTIAL_REFUND ?? 0,
        completed: sc.COMPLETED ?? 0,
        refunded: sc.REFUNDED ?? 0,
        cancelled: sc.CANCELLED ?? 0,
      }

      const paid = sc.PAID ?? 0
      const completed = sc.COMPLETED ?? 0
      const revenue = overview.totalRevenue ?? 0

      statCards.value = [
        { key: 'total',       value: String(overview.totalOrderCount),            label: '总订单',   color: '#3b82f6' },
        { key: 'pendingShip', value: String(paid),                               label: '待发货',   color: '#f97316' },
        { key: 'completed',   value: String(completed),                          label: '已完成',   color: '#22c55e' },
        { key: 'revenue',     value: formatCompactMoney(revenue),                 label: '总营收',   color: '#8b5cf6' },
      ]
    }
  } catch {
    //
  }
}

function formatCompactMoney(value: number) {
  if (value >= 10000) {
    return `¥${(value / 10000).toFixed(1)}万`
  }
  return `¥${Math.round(value).toLocaleString('zh-CN')}`
}

const cardBgMap: Record<string, string> = {
  '#3b82f6': '#eff6ff',
  '#f97316': '#fff7ed',
  '#22c55e': '#f0fdf4',
  '#8b5cf6': '#f5f3ff',
}
function getCardBg(color: string): string {
  return cardBgMap[color] || '#f8fafc'
}

let kwTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (kwTimer) clearTimeout(kwTimer)
  kwTimer = setTimeout(() => loadOrders(true), 300)
})

async function loadAll() {
  loading.value = true
  await Promise.all([loadOrders(true), loadStats()])
}

onMounted(async () => {
  try {
    const shops = await fetchShopOptions()
    shopOptions.value = shops ?? []
  } catch {
    //
  }
  await loadAll()
})
</script>

<style scoped lang="scss">
.v2-ec-order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.v2-ec-order-card {
  background: var(--wr-card, #ffffff);
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: transform 0.15s;
  cursor: pointer;

  &:active {
    transform: scale(0.98);
  }

  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
  }

  &__no {
    font-size: 13px;
    font-weight: 700;
    color: var(--wr-text, #333);
  }

  &__shop {
    font-size: 10px;
    color: #fff;
    background: #f97316;
    padding: 2px 8px;
    border-radius: 4px;
    white-space: nowrap;
  }

  &__express {
    font-size: 10px;
    color: #fff;
    background: #2563eb;
    padding: 2px 8px;
    border-radius: 4px;
    white-space: nowrap;
  }

  &__body {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 4px;
  }

  &__amount {
    font-size: 15px;
    font-weight: 800;
    color: #9b0000;
  }

  &__products {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 6px;
    font-size: 12px;
    color: var(--wr-text-secondary, #666);
    min-width: 0;
  }

  &__link {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
    min-width: 0;
  }

  &__line-count {
    flex-shrink: 0;
    color: var(--wr-muted, #999);
    font-size: 11px;
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__time {
    font-size: 11px;
    color: var(--wr-muted, #999);
  }

  &__status {
    display: inline-block;
    padding: 2px 10px;
    border-radius: 999px;
    font-size: 11px;
    color: white;
    font-weight: 700;
  }
}

.v2-ec-order-loading,
.v2-ec-order-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  gap: 12px;
}

.v2-ec-order-empty__icon {
  font-size: 48px;
  opacity: 0.6;
}

.v2-ec-order-empty__text {
  font-size: 14px;
  color: var(--wr-muted, #999);
}

.v2-ec-order-loadmore {
  display: flex;
  justify-content: center;
  padding: 8px 0 16px;

  &__btn {
    padding: 10px 32px;
    border: 1px solid var(--wr-border, #e8ecef);
    border-radius: 8px;
    background: var(--wr-card, #fff);
    color: var(--wr-text, #333);
    font-size: 14px;
    font-weight: 600;
    font-family: inherit;
    cursor: pointer;
    transition: background 0.2s;

    &:active {
      background: #f3f4f6;
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}

.v2-ec-order-search-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.v2-ec-order-more-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 8px;
  background: var(--wr-card, #fff);
  color: var(--wr-text, #333);
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.2s;
  flex-shrink: 0;

  &:active {
    background: #f3f4f6;
  }
}

.v2-ec-order-drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  background: rgba(15, 23, 42, 0.45);
}

.v2-ec-order-drawer {
  width: 100%;
  max-width: 500px;
  margin: 0 auto;
  max-height: 75vh;
  background: var(--wr-card, #fff);
  border-radius: 20px 20px 0 0;
  padding: 12px 20px max(24px, env(safe-area-inset-bottom));
  overflow-y: auto;

  &__handle {
    width: 36px;
    height: 4px;
    background: var(--wr-border, #e8ecef);
    border-radius: 999px;
    margin: 0 auto 16px;
  }

  &__title {
    font-size: 17px;
    font-weight: 700;
    color: var(--wr-text, #333);
    margin: 0 0 20px;
    text-align: center;
  }

  &__section {
    margin-bottom: 20px;
  }

  &__label {
    font-size: 12px;
    font-weight: 600;
    color: var(--wr-muted, #999);
    margin-bottom: 10px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  &__shops {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__shop-btn {
    padding: 8px 16px;
    border: 1px solid var(--wr-border, #e8ecef);
    border-radius: 999px;
    background: var(--wr-card, #fff);
    color: var(--wr-text-secondary, #666);
    font-size: 13px;
    font-weight: 500;
    font-family: inherit;
    cursor: pointer;
    transition: all 0.2s;

    &.is-active {
      border-color: var(--ec-stat-blue, #2563eb);
      background: #eff6ff;
      color: var(--ec-stat-blue, #2563eb);
      font-weight: 600;
    }

    &:active {
      transform: scale(0.95);
    }
  }

  &__actions {
    padding-top: 4px;
  }

  &__submit {
    width: 100%;
    padding: 12px;
    border: none;
    border-radius: 10px;
    background: var(--ec-stat-blue, #2563eb);
    color: #fff;
    font-size: 15px;
    font-weight: 700;
    font-family: inherit;
    cursor: pointer;
    transition: opacity 0.2s;

    &:active {
      opacity: 0.85;
    }
  }
}

.v2-ec-fade-enter-active,
.v2-ec-fade-leave-active {
  transition: opacity 0.2s ease;

  .v2-ec-order-drawer {
    transition: transform 0.25s ease;
  }
}

.v2-ec-fade-enter-from,
.v2-ec-fade-leave-to {
  opacity: 0;

  .v2-ec-order-drawer {
    transform: translateY(100%);
  }
}
</style>
