<template>
  <div v-loading="loading" class="mobile-order-view">
    <!-- 顶部导航栏 -->
    <div class="mobile-order-view__header">
      <div class="mobile-order-view__header-left">
        <MobileDoodleChip
          tag="button" type="button"
          shape="pill" color="#3b82f6"
          class="mobile-order-view__back"
          @click="$router.back()"
        >
          <span>←</span>
        </MobileDoodleChip>
        <h1 class="mobile-order-view__title">订单中心</h1>
      </div>
    </div>

    <div class="mobile-order-view__content">
      <!-- ===== 月份 + 店铺筛选行 ===== -->
      <div class="filter-bar">
        <div class="month-picker">
          <button class="month-arrow" @click="shiftMonth(-1)" :disabled="loadingList">‹</button>
          <MobileMonthPicker v-model="orderMonth" class="month-label" />
          <button class="month-arrow" @click="shiftMonth(1)" :disabled="loadingList">›</button>
        </div>
        <div class="shop-picker">
          <select v-model="shopFilter" class="shop-select" @change="onShopFilterChange">
            <option value="">全部店铺</option>
            <option v-for="s in shopOptions" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>
      </div>

      <!-- ===== 搜索框 ===== -->
      <MobileDoodleSearch
          v-model="keyword"
          placeholder="搜索订单号 / 买家 / 平台单号 / 商品名称 / SKU..."
          color="#3b82f6"
      />

      <!-- ===== 统计数据方块 ===== -->
      <SchemeADoodleFrame color="#3b82f6" class="stats-overview" sketch :stroke-width="2">
        <div class="stats-overview__inner">
          <div class="stats-grid">
            <div
              v-for="stat in statCards"
              :key="stat.key"
              class="stat-block"
              :style="{ '--block-color': stat.color }"
            >
              <div class="stat-block__value">{{ stat.value }}</div>
              <div class="stat-block__label">{{ stat.label }}</div>
            </div>
          </div>
        </div>
      </SchemeADoodleFrame>

      <!-- ===== 状态标签 ===== -->
      <MobileCategoryTabs
        :categories="statusTabs"
        v-model:active-value="activeStatus"
        active-color="#3b82f6"
        inactive-color="#94a3b8"
      />

      <!-- ===== 订单列表 ===== -->
      <div class="order-section">
        <MobileSectionHeader
          iconText="📋"
          title="订单列表"
          :count="total"
          count-unit="单"
        />

        <div v-if="loadingList && !orders.length" class="list-loading">
          <span>加载中...</span>
        </div>

        <template v-else>
          <div class="order-list">
            <SchemeADoodleFrame
              v-for="order in displayOrders"
              :key="order.id"
              tag="button"
              type="button"
              class="order-card"
              :seed="order.id"
              :color="statusColor(order.status)"
              sketch
              :stroke-width="2"
            >
              <div class="order-card__inner">
                <!-- 头部：订单号 + 店铺 -->
                <div class="order-card__head">
                  <span class="order-card__no">#{{ order.platformOrderNo }}</span>
                  <span class="order-card__shop">{{ order.shopName || order.platformName || '—' }}</span>
                </div>

                <!-- 运单号 -->
                <div v-if="order.trackingNumber" class="order-card__head">
                  <span class="order-card__no">#{{ order.trackingNumber }}</span>
                  <span class="order-card__express">{{ order.expressStationName || '—' }}</span>
                </div>

                <!-- 买家信息 + 金额 -->
                <div class="order-card__body">
                  <span class="order-card__link">{{ order.linkName || order.lines?.[0]?.linkName || '' }}</span>
                  <span class="order-card__amount">￥{{ order.receivedAmount ?? '—' }}</span>
                </div>

                <!-- 商品信息 -->
                <div class="order-card__products" v-if="order.linkName || order.lines?.length">
                  <span class="order-card__link">{{ order.skuSpecName || order.lines?.[0]?.skuSpecName || '' }}</span>
                  <span v-if="order.lineCount" class="order-card__line-count">共{{ order.lineCount }}件</span>
                </div>

                <!-- 尾部：时间 + 状态 -->
                <div class="order-card__footer">
                  <span class="order-card__time">{{ formatTime(order.orderTime) }}</span>
                  <span class="order-card__status" :style="{ background: statusColor(order.status) }">
                    {{ statusLabel(order.status) }}
                  </span>
                </div>
              </div>
            </SchemeADoodleFrame>
          </div>

          <!-- 加载更多 -->
          <div v-if="hasMore" class="load-more">
            <MobileDoodleChip
              tag="button" type="button"
              color="#3b82f6"
              :disabled="loadingMore"
              @click="loadMore"
            >
              {{ loadingMore ? '加载中...' : '加载更多' }}
            </MobileDoodleChip>
          </div>

          <!-- 空状态 -->
          <div v-if="!displayOrders.length" class="empty-state">
            <span class="empty-state__icon">📭</span>
            <span class="empty-state__text">暂无订单数据</span>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import MobileDoodleSearch from '@/mobile/components/MobileDoodleSearch.vue'
import MobileCategoryTabs from '@/mobile/components/MobileCategoryTabs.vue'
import MobileSectionHeader from '@/mobile/components/MobileSectionHeader.vue'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import type { CategoryItem } from '@/mobile/components/MobileCategoryTabs.vue'
import MobileMonthPicker from '@/mobile/components/MobileMonthPicker.vue'
import {
  fetchSalesOrders,
  fetchSalesOrderMonthlyOverview,
  type EcSalesOrder,
  type EcSalesOrderStatus,
} from '@/api/ecommerce/salesOrder.ts'
import { fetchShopOptions, type EcShop } from '@/api/ecommerce/shop.ts'

// ─── 工具函数 ─────────────────────────
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

// ─── 订单状态配置 ──────────────────────
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

// ─── 响应式状态 ────────────────────────
const loading = ref(true)
const loadingList = ref(false)
const loadingMore = ref(false)

// 筛选条件
const keyword = ref('')
const activeStatus = ref<string | number | null>('all')
const orderMonth = ref(formatMonth(new Date()))
const shopFilter = ref<number | ''>('')

// 店铺选项
const shopOptions = ref<EcShop[]>([])

// 分页 & 订单数据
const page = ref(1)
const pageSize = 20
const total = ref(0)
const orders = ref<EcSalesOrder[]>([])

// 统计
const statCards = ref<Array<{ key: string; value: string; label: string; color: string }>>([
  { key: 'total',       value: '—', label: '总订单',   color: '#3b82f6' },
  { key: 'pendingShip', value: '—', label: '待发货',   color: '#f97316' },
  { key: 'completed',   value: '—', label: '已完成',   color: '#22c55e' },
  { key: 'revenue',     value: '—', label: '总营收',   color: '#8b5cf6' },
])

// ─── 状态标签 ──────────────────────────
const statusTabs = computed<CategoryItem[]>(() => {
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

// ─── 全月各状态订单数（从 loadStats 中获取准确值） ──
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

// ─── 筛选 & 展示 ───────────────────────
const displayOrders = computed(() => {
  let items = orders.value
  // 关键词过滤（客户端补充）
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

// ─── 月份切换 ──────────────────────────
function shiftMonth(delta: number) {
  const [y, m] = orderMonth.value.split('-').map(Number)
  const d = new Date(y, m - 1 + delta, 1)
  orderMonth.value = formatMonth(d)
}

// 状态标签切换时重新加载
watch(activeStatus, () => {
  loadOrders(true)
})

// 月份变化时重新加载数据
watch(orderMonth, () => {
  loadOrders(true)
  loadStats()
})

function onShopFilterChange() {
  loadOrders(true)
  loadStats()
}

// ─── API 请求 ──────────────────────────
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
    // 静默
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
      // 从后端获取各状态订单数
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
    // 静默
  }
}

function formatCompactMoney(value: number) {
  if (value >= 10000) {
    return `¥${(value / 10000).toFixed(1)}万`
  }
  return `¥${Math.round(value).toLocaleString('zh-CN')}`
}

// ─── 搜索防抖 ───────────────────────────
let kwTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (kwTimer) clearTimeout(kwTimer)
  kwTimer = setTimeout(() => loadOrders(true), 300)
})

// ─── 初始化 ─────────────────────────────
async function loadAll() {
  loading.value = true
  await Promise.all([loadOrders(true), loadStats()])
}

onMounted(async () => {
  try {
    const shops = await fetchShopOptions()
    shopOptions.value = shops ?? []
  } catch {
    // 静默
  }
  await loadAll()
})
</script>

<style scoped lang="scss">
.mobile-order-view {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  font-family: 'ZCOOL KuaiLe', 'Alibaba PuHuiTi', 'PingFang SC', sans-serif;
  padding-bottom: 100px;
}

/* ===== 顶部导航 ===== */
.mobile-order-view__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: max(16px, env(safe-area-inset-top)) 16px 12px;
  background: #fff;
}

.mobile-order-view__header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-order-view__back {
  width: 36px;
  height: 36px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #3b82f6;
  font-weight: 700;
  cursor: pointer;
  background: #fff;
  transition: transform 0.2s ease;

  :deep(.sa-doodle-frame__body) {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    padding: 0;
  }

  &:active {
    transform: scale(0.9);
  }
}

.mobile-order-view__title {
  font-size: 24px;
  margin: 0;
  color: #1e293b;
}

.mobile-order-view__content {
  flex: 1;
  padding: 0 16px 20px;
  overflow-y: auto;
}

/* ===== 统计方块 ===== */
.stats-overview {
  margin-bottom: 14px;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.stats-overview__inner {
  padding: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.stat-block {
  text-align: center;
  padding: 10px 4px;
  border-radius: 12px;
  background: #f8fafc;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: var(--block-color);
  }
}

.stat-block__value {
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.2;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-block__label {
  font-size: 11px;
  color: #64748b;
}

/* ===== 筛选栏 ===== */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.month-picker {
  display: flex;
  align-items: center;
  gap: 6px;
  background: white;
  border: 2px solid #e2e8f0;
  border-radius: 999px;
  padding: 4px 2px;
}

.month-arrow {
  width: 30px;
  height: 30px;
  border: none;
  background: transparent;
  font-size: 18px;
  font-weight: 700;
  color: #3b82f6;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &:hover {
    background: #eff6ff;
  }

  &:disabled {
    color: #cbd5e1;
    cursor: not-allowed;
  }
}

.month-label {
  /* overrides MobileMonthPicker trigger to match picker layout */
  :deep(.mobile-month-picker__trigger) {
    border: none;
    background: transparent;
    padding: 2px 4px;
    font-size: 15px;
    cursor: pointer;
  }

  :deep(.mobile-month-picker__trigger-text) {
    min-width: 80px;
  }

  :deep(.mobile-month-picker__arrow) {
    display: none;
  }
}

.shop-picker {
  flex: 1;
  min-width: 0;
}

.shop-select {
  width: 100%;
  padding: 8px 12px;
  border: 2px solid #e2e8f0;
  border-radius: 999px;
  background: white;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 13px;
  color: #1e293b;
  outline: none;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  padding-right: 30px;

  &:focus {
    border-color: #3b82f6;
  }
}

/* ===== 订单区域 ===== */
.order-section {
  margin-top: 4px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-card {
  cursor: pointer;
  transition: transform 0.15s ease;
  text-decoration: none;
  text-align: left;
  font-family: inherit;

  &:active {
    transform: scale(0.98);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.order-card__inner {
  padding: 24px;
}

.order-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.order-card__no {
  font-size: 14px;
  font-weight: 800;
  color: #1e293b;
}

.order-card__shop {
  font-size: 10px;
  color: #fff;
  background: #f97316;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
}
.order-card__express {
  font-size: 10px;
  color: #fff;
  background: #2563eb;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
}

.order-card__body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.order-card__buyer {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #475569;
}

.order-card__amount {
  font-size: 16px;
  font-weight: 800;
  color: #9b0000;
}

.order-card__products {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #64748b;
  min-width: 0;
}

.order-card__link {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.order-card__line-count {
  flex-shrink: 0;
  color: #94a3b8;
  font-size: 11px;
}

.order-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-card__time {
  font-size: 11px;
  color: #94a3b8;
}

.order-card__status {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 11px;
  color: white;
  font-weight: 700;
}

/* ===== 加载 & 空状态 ===== */
.list-loading,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  gap: 12px;
}

.empty-state__icon {
  font-size: 48px;
  opacity: 0.6;
}

.empty-state__text {
  font-size: 14px;
  color: #94a3b8;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 16px;

  .mobile-doodle-chip {
    font-family: inherit;
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;

    :deep(.sa-doodle-frame__body) {
      padding: 10px 24px;
    }
  }
}
</style>
