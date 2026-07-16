<template>
  <V2Page>
    <div v-loading="loading" class="v2-ec v2-ec-inventory">
      <div class="v2-ec-inventory__content">
        <div class="v2-ec-stats">
          <div class="v2-ec-stat-card" style="background: #eff6ff;">
            <div class="v2-ec-stat-card__icon" style="background: #2563eb;">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M16 4h2a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V6a2 2 0 012-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/>
              </svg>
            </div>
            <div class="v2-ec-stat-card__info">
              <div class="v2-ec-stat-card__value" style="color: #2563eb;">{{ summary.skuCount }}</div>
              <div class="v2-ec-stat-card__label">SKU 数</div>
            </div>
          </div>
          <div class="v2-ec-stat-card" style="background: #f0fdf4;">
            <div class="v2-ec-stat-card__icon" style="background: #16a34a;">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/>
              </svg>
            </div>
            <div class="v2-ec-stat-card__info">
              <div class="v2-ec-stat-card__value" style="color: #16a34a;">{{ summary.totalQty }}</div>
              <div class="v2-ec-stat-card__label">总数量</div>
            </div>
          </div>
          <div class="v2-ec-stat-card" style="background: #fef2f2;">
            <div class="v2-ec-stat-card__icon" style="background: #dc2626;">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
              </svg>
            </div>
            <div class="v2-ec-stat-card__info">
              <div class="v2-ec-stat-card__value" style="color: #dc2626;">¥{{ summary.stockValue }}</div>
              <div class="v2-ec-stat-card__label">库存价值</div>
            </div>
          </div>
        </div>

        <div class="v2-ec-inventory__health">
          <div class="v2-ec-inventory__health-title">库存健康度</div>
          <div class="v2-ec-inventory__health-row">
            <span class="v2-ec-inventory__health-label">正常</span>
            <div class="v2-ec-inventory__health-bar">
              <div class="v2-ec-inventory__health-fill v2-ec-inventory__health-fill--green" :style="{ width: healthStats.normalPct + '%' }"></div>
            </div>
            <span class="v2-ec-inventory__health-value">{{ healthStats.normalPct }}%</span>
          </div>
          <div class="v2-ec-inventory__health-row">
            <span class="v2-ec-inventory__health-label">不足</span>
            <div class="v2-ec-inventory__health-bar">
              <div class="v2-ec-inventory__health-fill v2-ec-inventory__health-fill--orange" :style="{ width: healthStats.lowPct + '%' }"></div>
            </div>
            <span class="v2-ec-inventory__health-value">{{ healthStats.lowPct }}%</span>
          </div>
          <div class="v2-ec-inventory__health-row">
            <span class="v2-ec-inventory__health-label">缺货</span>
            <div class="v2-ec-inventory__health-bar">
              <div class="v2-ec-inventory__health-fill v2-ec-inventory__health-fill--red" :style="{ width: healthStats.zeroPct + '%' }"></div>
            </div>
            <span class="v2-ec-inventory__health-value">{{ healthStats.zeroPct }}%</span>
          </div>
        </div>

        <div v-if="alertItems.length > 0" class="v2-ec-inventory__section">
          <div class="v2-ec-inventory__collapse-header" @click="alertExpanded = !alertExpanded">
            <div class="v2-ec-section-title">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#dc2626" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
              <span>库存预警</span>
            </div>
            <div class="v2-ec-inventory__collapse-actions">
              <span class="v2-ec-inventory__collapse-count">{{ alertItems.length }}件</span>
              <svg class="v2-ec-inventory__collapse-arrow" :class="{ 'is-expanded': alertExpanded }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="6 9 12 15 18 9"/>
              </svg>
            </div>
          </div>
          <transition name="v2-ec-collapse">
            <div v-show="alertExpanded" class="v2-ec-inventory__alert-list">
              <div
                v-for="item in alertItems"
                :key="item.id"
                class="v2-ec-inventory__alert-card"
                @click="handleItemClick(asInventory(item))"
              >
                <div class="v2-ec-inventory__alert-icon" :class="(asInventory(item).quantity ?? 0) <= 0 ? 'is-danger' : 'is-warning'">
                  <svg v-if="(asInventory(item).quantity ?? 0) <= 0" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
                  </svg>
                  <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
                  </svg>
                </div>
                <div class="v2-ec-inventory__alert-info">
                  <div class="v2-ec-inventory__alert-name">{{ asInventory(item).productName || asInventory(item).skuCode }}</div>
                  <div class="v2-ec-inventory__alert-spec">{{ asInventory(item).specName }}</div>
                </div>
                <div class="v2-ec-inventory__alert-qty">
                  <strong>{{ asInventory(item).quantity }}</strong>件
                </div>
              </div>
            </div>
          </transition>
        </div>

        <div class="v2-ec-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.id"
            type="button"
            class="v2-ec-tab"
            :class="{ 'is-active': activeStatus === tab.id }"
            @click="activeStatus = tab.id"
          >
            {{ tab.icon }} {{ tab.name }} ({{ tab.count }})
          </button>
        </div>

        <div class="v2-ec-search">
          <svg class="v2-ec-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input
            v-model="keyword"
            class="v2-ec-search__input"
            placeholder="搜索 SKU 编码或产品名称..."
            type="search"
          />
        </div>

        <div class="v2-ec-inventory__section">
          <div class="v2-ec-inventory__collapse-header" @click="inventoryListExpanded = !inventoryListExpanded">
            <div class="v2-ec-section-title">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--wr-text, #333)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
              </svg>
              <span>库存列表</span>
            </div>
            <div class="v2-ec-inventory__collapse-actions">
              <span class="v2-ec-inventory__collapse-count">{{ displayItems.length }}项</span>
              <svg class="v2-ec-inventory__collapse-arrow" :class="{ 'is-expanded': inventoryListExpanded }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="6 9 12 15 18 9"/>
              </svg>
            </div>
          </div>

          <template v-if="inventoryListExpanded">
            <div v-if="loadingList && !displayItems.length" class="v2-ec-inventory__loading">
              <span>加载中...</span>
            </div>

            <template v-else>
              <div class="v2-ec-inventory__list">
                <div
                  v-for="item in displayItems"
                  :key="item.id"
                  class="v2-ec-inventory__card"
                  @click="handleItemClick(item)"
                >
                  <div class="v2-ec-inventory__card-head">
                    <div class="v2-ec-inventory__card-sku">
                      <div class="v2-ec-inventory__card-code">{{ item.skuCode }}</div>
                      <div class="v2-ec-inventory__card-name">{{ item.productName || item.specName || '—' }}</div>
                    </div>
                    <span
                      class="v2-ec-inventory__card-tag"
                      :class="getStatusTagClass(item)"
                    >
                      {{ getStatusLabel(item) }}
                    </span>
                  </div>

                  <div class="v2-ec-inventory__card-qty">{{ item.quantity ?? 0 }}</div>

                  <div class="v2-ec-inventory__card-meta">
                    <span>在途 {{ item.inTransitQty ?? 0 }}</span>
                    <span class="v2-ec-inventory__card-meta-sep">·</span>
                    <span>库存价值 </span>
                    <span class="v2-ec-inventory__card-value">¥{{ stockValue(item) }}</span>
                  </div>

                  <div class="v2-ec-inventory__card-progress">
                    <div class="v2-ec-inventory__card-progress-bar">
                      <span
                        class="v2-ec-inventory__card-progress-fill"
                        :class="{ 'is-danger': item.alertActive }"
                        :style="{ width: stockLevelPct(item) + '%' }"
                      />
                    </div>
                    <span class="v2-ec-inventory__card-progress-label">{{ item.quantity ?? 0 }}</span>
                  </div>
                </div>
              </div>

              <div v-if="!displayItems.length" class="v2-ec-inventory__empty">
                暂无库存数据
              </div>

              <div v-if="hasMore" class="v2-ec-inventory__load-more">
                <button
                  type="button"
                  class="v2-ec-inventory__load-more-btn"
                  :disabled="loadingMore"
                  @click="loadMore"
                >
                  {{ loadingMore ? '加载中...' : '加载更多' }}
                </button>
              </div>
            </template>
          </template>
        </div>
      </div>

      <div v-if="showBackToTop" class="v2-ec-inventory__back-top" @click="scrollToTop">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="18 15 12 9 6 15"/>
        </svg>
      </div>

      <div v-if="skuSheetVisible" class="v2-ec-inventory__sheet-overlay" @click="skuSheetVisible = false"></div>
      <div v-if="skuSheetVisible" class="v2-ec-inventory__sheet">
        <div class="v2-ec-inventory__sheet-header">
          <span class="v2-ec-inventory__sheet-title">{{ skuSheetTitle }}</span>
          <button type="button" class="v2-ec-inventory__sheet-close" @click="skuSheetVisible = false">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div v-if="skuSheetLoading" class="v2-ec-inventory__sheet-loading">加载中...</div>
        <div v-else-if="skuSheetItems.length" class="v2-ec-inventory__sheet-list">
          <div
            v-for="sku in skuSheetItems"
            :key="sku.id"
            class="v2-ec-inventory__sheet-card"
          >
            <div class="v2-ec-inventory__sheet-card-inner">
              <div v-if="sku.imageName" class="v2-ec-inventory__sheet-card-thumb">
                <img :src="getEcommerceImageUrl(sku.imageName)" alt="" />
              </div>
              <div v-else class="v2-ec-inventory__sheet-card-thumb v2-ec-inventory__sheet-card-thumb--empty">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#cbd5e1" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/>
                </svg>
              </div>
              <div class="v2-ec-inventory__sheet-card-main">
                <div class="v2-ec-inventory__sheet-card-head">
                  <span class="v2-ec-inventory__sheet-card-code">{{ sku.skuCode }}</span>
                  <span class="v2-ec-inventory__sheet-card-spec">{{ sku.specName || '—' }}</span>
                </div>
                <div class="v2-ec-inventory__sheet-card-body">
                  <div class="v2-ec-inventory__sheet-card-info">
                    <span class="v2-ec-inventory__sheet-card-label">库存</span>
                    <strong class="v2-ec-inventory__sheet-card-qty">{{ sku.quantity ?? 0 }}</strong>
                    <span class="v2-ec-inventory__sheet-card-sep">·</span>
                    <span class="v2-ec-inventory__sheet-card-label">在途</span>
                    <strong class="v2-ec-inventory__sheet-card-qty">{{ sku.inTransitQty ?? 0 }}</strong>
                  </div>
                  <button
                    type="button"
                    class="v2-ec-inventory__sheet-card-adjust"
                    @click.stop="openSkuAdjust(sku)"
                  >
                    调整
                  </button>
                </div>
                <div class="v2-ec-inventory__sheet-card-footer">
                  <span class="v2-ec-inventory__sheet-card-label">价值</span>
                  <strong class="v2-ec-inventory__sheet-card-value">¥{{ stockValue(sku) }}</strong>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="v2-ec-inventory__sheet-empty">该 SPU 下暂无库存数据</div>
      </div>

      <div v-if="adjustSheetVisible" class="v2-ec-inventory__sheet-overlay" @click="adjustSheetVisible = false"></div>
      <div v-if="adjustSheetVisible" class="v2-ec-inventory__sheet">
        <div class="v2-ec-inventory__sheet-header">
          <span class="v2-ec-inventory__sheet-title">库存调整</span>
          <button type="button" class="v2-ec-inventory__sheet-close" @click="adjustSheetVisible = false">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div v-if="adjustTarget" class="v2-ec-inventory__adjust">
          <div class="v2-ec-inventory__adjust-preview">
            <span class="v2-ec-inventory__adjust-preview-label">当前库存</span>
            <strong class="v2-ec-inventory__adjust-preview-qty">{{ adjustTarget.quantity ?? 0 }}</strong>
          </div>
          <div class="v2-ec-inventory__adjust-type">
            <button
              type="button"
              class="v2-ec-inventory__adjust-type-btn"
              :class="{ 'is-active': adjustForm.changeType === 'DEDUCT' }"
              @click="adjustForm.changeType = 'DEDUCT'"
            >扣除</button>
            <button
              type="button"
              class="v2-ec-inventory__adjust-type-btn"
              :class="{ 'is-active': adjustForm.changeType === 'RECLAIM' }"
              @click="adjustForm.changeType = 'RECLAIM'"
            >回收</button>
          </div>
          <div class="v2-ec-inventory__adjust-stepper">
            <button type="button" class="v2-ec-inventory__adjust-stepper-btn" :disabled="adjustForm.changeQty <= 1" @click="adjustForm.changeQty--">−</button>
            <span class="v2-ec-inventory__adjust-stepper-val">{{ adjustForm.changeQty }}</span>
            <button type="button" class="v2-ec-inventory__adjust-stepper-btn" @click="adjustForm.changeQty++">+</button>
          </div>
          <div class="v2-ec-inventory__adjust-after">
            调整后：<strong>{{ adjustAfterQty }}</strong> 件
          </div>
          <button
            type="button"
            class="v2-ec-inventory__adjust-submit"
            :disabled="adjustSubmitting"
            @click="submitAdjust"
          >
            {{ adjustSubmitting ? '提交中...' : '确认调整' }}
          </button>
        </div>
      </div>
    </div>
  </V2Page>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, reactive, ref, watch} from 'vue'
import {getEcommerceImageUrl} from '@/api/ecommerce/image'
import {type EcInventory, adjustInventory, fetchInventories, fetchInventoryByProduct, fetchInventoryInboundValueSummary, fetchInventoryOverview, fetchSpuStatusCounts, type EcInventoryOverview, type EcInventorySpuStatus} from '@/api/ecommerce/inventory'
import {
  classifyInventory,
  type InventoryStatusKey,
  type InventoryStatusStats,
} from '@/utils/inventoryStats'
import V2Page from '@/mobile-v2/components/V2Page.vue'

import '@/mobile-v2/views/ecommerce/styles/v2-ecommerce.scss'

const loading = ref(true)
const loadingList = ref(false)
const loadingMore = ref(false)
const keyword = ref('')
const activeStatus = ref<string | number | null>('all')
const alertExpanded = ref(false)
const inventoryListExpanded = ref(false)

const page = ref(1)
const pageSize = 20
const total = ref(0)
const records = ref<EcInventory[]>([])
const extra = ref<Record<string, unknown> | undefined>()

const statsItems = ref<EcInventory[]>([])
const overview = ref<EcInventoryOverview | undefined>()
const spuStatus = ref<EcInventorySpuStatus | undefined>()
const totalInboundValue = ref<number | null>(null)

const showBackToTop = ref(false)

let scrollHandler: (() => void) | null = null
onMounted(() => {
  const scrollEl = document.querySelector('.mobile-v2-app__main')
  if (!scrollEl) return
  scrollHandler = () => { showBackToTop.value = scrollEl.scrollTop > 300 }
  scrollEl.addEventListener('scroll', scrollHandler)
})

onUnmounted(() => {
  const scrollEl = document.querySelector('.mobile-v2-app__main')
  if (scrollEl && scrollHandler) {
    scrollEl.removeEventListener('scroll', scrollHandler)
  }
})

function scrollToTop() {
  document.querySelector('.mobile-v2-app__main')?.scrollTo({ top: 0, behavior: 'smooth' })
}

const statusTabs = computed(() => {
  const s = inventoryStats.value
  return [
    { id: 'all', name: '全部', icon: '📦', count: s.total },
    { id: 'normal', name: '正常', icon: '✅', count: s.normal },
    { id: 'low', name: '不足', icon: '⚠️', count: s.low },
    { id: 'zero', name: '缺货', icon: '🚨', count: s.zero },
  ]
})

const inventoryStats = computed((): InventoryStatusStats => {
  const s = spuStatus.value
  return {
    total: s?.total ?? 0,
    normal: s?.normal ?? 0,
    low: s?.low ?? 0,
    zero: s?.zero ?? 0,
  }
})

const healthStats = computed(() => {
  const counts = overview.value?.statusCounts
  if (!counts) return { normalPct: '0.00', lowPct: '0.00', zeroPct: '0.00' }
  const total = (counts.normal ?? 0) + (counts.low ?? 0) + (counts.zero ?? 0) || 1
  return {
    normalPct: (((counts.normal ?? 0) / total) * 100).toFixed(2),
    lowPct: (((counts.low ?? 0) / total) * 100).toFixed(2),
    zeroPct: (((counts.zero ?? 0) / total) * 100).toFixed(2),
  }
})

const summary = computed(() => {
  const qty = Number(extra.value?.totalQuantity ?? 0)
  const stockVal = Number(extra.value?.totalStockValue ?? 0)
  const valStr = stockVal >= 10000
    ? (stockVal / 10000).toFixed(1) + '万'
    : stockVal.toLocaleString()
  return {
    skuCount: overview.value?.skuCount ?? 0,
    totalQty: qty.toLocaleString(),
    stockValue: valStr,
  }
})

const displayItems = computed(() => {
  let items = records.value
  if (activeStatus.value !== 'all') {
    items = items.filter((row) => classifyInventory(row) === activeStatus.value)
  }
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    items = items.filter((row) =>
      (row.skuCode?.toLowerCase() ?? '').includes(kw) ||
      (row.productName?.toLowerCase() ?? '').includes(kw) ||
      (row.specName?.toLowerCase() ?? '').includes(kw)
    )
  }
  return items
})

const alertItems = computed(() => {
  return statsItems.value
    .filter((row) => row.alertActive)
    .sort((a, b) => (a.quantity ?? 0) - (b.quantity ?? 0))
})

const hasMore = computed(() => records.value.length < total.value)

function asInventory(item: { id: string | number }): EcInventory {
  return item as unknown as EcInventory
}

const STATUS_LABELS: Record<InventoryStatusKey, string> = {
  normal: '正常',
  low: '不足',
  zero: '缺货',
}

function getStatusLabel(row: EcInventory): string {
  return STATUS_LABELS[classifyInventory(row)] || '—'
}

function getStatusTagClass(row: EcInventory): string {
  const status = classifyInventory(row)
  return {
    normal: 'is-normal',
    low: 'is-low',
    zero: 'is-zero',
  }[status] || ''
}

function stockValue(row: EcInventory): string {
  const v = (row.quantity ?? 0) * (row.salePrice ?? 0)
  return v >= 10000
    ? (v / 10000).toFixed(1) + '万'
    : v.toLocaleString()
}

function stockLevelPct(row: EcInventory): number {
  const qty = row.quantity ?? 0
  const threshold = Math.max(row.alertThreshold ?? 0, 1)
  const max = Math.max(threshold * 2, qty, 1)
  return Math.min(100, Math.round((qty / max) * 100))
}

async function loadList(resetPage = false) {
  if (resetPage) {
    page.value = 1
    records.value = []
  }
  loadingList.value = true
  try {
    const result = await fetchInventories(
      keyword.value.trim() || undefined,
      false,
      undefined,
      { page: page.value, pageSize },
      undefined,
      true,
    )
    if (resetPage) {
      records.value = result.records
    } else {
      records.value = [...records.value, ...result.records]
    }
    total.value = result.total
    page.value = result.page
    extra.value = result.extra
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
    const result = await fetchInventories(
      keyword.value.trim() || undefined,
      false,
      undefined,
      { page: page.value, pageSize },
      undefined,
      true,
    )
    records.value = [...records.value, ...result.records]
    total.value = result.total
    page.value = result.page
  } finally {
    loadingMore.value = false
  }
}

async function loadStats() {
  try {
    const [ov, spu, alertResult, inboundSummary] = await Promise.all([
      fetchInventoryOverview(),
      fetchSpuStatusCounts(),
      fetchInventories(undefined, true, undefined, { page: 1, pageSize: 500 }),
      fetchInventoryInboundValueSummary(),
    ])
    overview.value = ov
    spuStatus.value = spu
    statsItems.value = alertResult.records
    totalInboundValue.value = inboundSummary.totalInboundValue ?? null
  } catch {
    // 静默失败，不影响主列表
  }
}

async function loadAll() {
  loading.value = true
  await Promise.all([loadList(true), loadStats()])
}

let keywordTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (keywordTimer) clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => loadList(true), 300)
})

const skuSheetVisible = ref(false)
const skuSheetLoading = ref(false)
const skuSheetTitle = ref('')
const skuSheetItems = ref<EcInventory[]>([])
const currentSpuItem = ref<EcInventory | null>(null)

async function handleItemClick(item: EcInventory) {
  currentSpuItem.value = item
  skuSheetTitle.value = item.productName || item.skuCode
  skuSheetVisible.value = true
  skuSheetLoading.value = true
  skuSheetItems.value = []
  try {
    if (item.productId) {
      const result = await fetchInventoryByProduct(item.productId)
      skuSheetItems.value = result
    } else {
      skuSheetItems.value = [item]
    }
  } catch {
    skuSheetItems.value = []
  } finally {
    skuSheetLoading.value = false
  }
}

const adjustSheetVisible = ref(false)
const adjustTarget = ref<EcInventory | null>(null)
const adjustSubmitting = ref(false)
const adjustForm = reactive({ changeType: 'DEDUCT' as 'DEDUCT' | 'RECLAIM', changeQty: 1 })

const adjustAfterQty = computed(() => {
  const current = adjustTarget.value?.quantity ?? 0
  if (adjustForm.changeType === 'DEDUCT') return Math.max(0, current - adjustForm.changeQty)
  return current + adjustForm.changeQty
})

function openSkuAdjust(sku: EcInventory) {
  adjustTarget.value = sku
  adjustForm.changeType = 'DEDUCT'
  adjustForm.changeQty = 1
  adjustSheetVisible.value = true
}

async function submitAdjust() {
  if (!adjustTarget.value) return
  if (adjustForm.changeType === 'DEDUCT' && adjustForm.changeQty > (adjustTarget.value.quantity ?? 0)) return
  adjustSubmitting.value = true
  try {
    await adjustInventory(adjustTarget.value.id, {
      changeType: adjustForm.changeType,
      changeQty: adjustForm.changeQty,
    })
    adjustSheetVisible.value = false
    adjustTarget.value = null
    if (currentSpuItem.value) {
      await handleItemClick(currentSpuItem.value)
    }
    await loadAll()
  } finally {
    adjustSubmitting.value = false
  }
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped lang="scss">
.v2-ec-inventory {
  &__header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
  }

  &__back {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--wr-border, #e8ecef);
    border-radius: 10px;
    background: var(--wr-card, #fff);
    color: var(--wr-text, #333);
    cursor: pointer;
    padding: 0;
    transition: background 0.15s;
    flex-shrink: 0;

    &:active {
      background: #f3f4f6;
    }
  }

  &__title {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
    color: var(--wr-text, #333);
  }

  &__content {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  &__health {
    padding: 16px;
    background: var(--wr-card, #fff);
    border-radius: 12px;
    border: 1px solid var(--wr-border, #e8ecef);
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  &__health-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--wr-text, #333);
    margin-bottom: 2px;
  }

  &__health-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__health-label {
    font-size: 12px;
    color: var(--wr-text-secondary, #666);
    width: 36px;
    flex-shrink: 0;
  }

  &__health-bar {
    flex: 1;
    height: 10px;
    background: #e2e8f0;
    border-radius: 999px;
    overflow: hidden;
  }

  &__health-fill {
    height: 100%;
    border-radius: 999px;
    transition: width 0.6s ease;

    &--green { background: linear-gradient(90deg, #22c55e, #4ade80); }
    &--orange { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
    &--red { background: linear-gradient(90deg, #ef4444, #f87171); }
  }

  &__health-value {
    font-size: 12px;
    font-weight: 600;
    color: var(--wr-text, #333);
    width: 42px;
    text-align: right;
  }

  &__section {
    display: flex;
    flex-direction: column;
    gap: 0;
  }

  &__collapse-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    cursor: pointer;
    user-select: none;
    padding: 4px 0;
  }

  &__collapse-actions {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &__collapse-count {
    font-size: 12px;
    color: var(--wr-muted, #999);
  }

  &__collapse-arrow {
    color: var(--wr-muted, #999);
    transition: transform 0.25s ease;

    &.is-expanded {
      transform: rotate(180deg);
    }
  }

  &__alert-list {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 4px 0;
  }

  &__alert-card {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px;
    background: var(--wr-card, #fff);
    border: 1px solid var(--wr-border, #e8ecef);
    border-radius: 10px;
    cursor: pointer;
    transition: transform 0.15s;

    &:active {
      transform: scale(0.98);
    }
  }

  &__alert-icon {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &.is-danger {
      background: #fef2f2;
      color: #dc2626;
    }

    &.is-warning {
      background: #fffbeb;
      color: #d97706;
    }
  }

  &__alert-info {
    flex: 1;
    min-width: 0;
  }

  &__alert-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--wr-text, #333);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__alert-spec {
    font-size: 11px;
    color: var(--wr-text-secondary, #666);
    margin-top: 2px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__alert-qty {
    font-size: 12px;
    color: var(--wr-text-secondary, #666);
    white-space: nowrap;
    flex-shrink: 0;

    strong {
      font-size: 16px;
      font-weight: 700;
      color: var(--wr-text, #333);
    }
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  &__card {
    padding: 16px;
    background: var(--wr-card, #fff);
    border: 1px solid var(--wr-border, #e8ecef);
    border-radius: 12px;
    cursor: pointer;
    transition: transform 0.15s, box-shadow 0.2s;

    &:active {
      transform: scale(0.98);
    }
  }

  &__card-head {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 8px;
  }

  &__card-sku {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
    flex: 1;
  }

  &__card-code {
    font-size: 15px;
    font-weight: 700;
    color: var(--wr-text, #333);
  }

  &__card-name {
    font-size: 12px;
    color: var(--wr-text-secondary, #666);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__card-tag {
    flex-shrink: 0;
    font-size: 10px;
    font-weight: 600;
    padding: 3px 8px;
    border-radius: 999px;

    &.is-normal {
      color: #16a34a;
      background: #dcfce7;
    }

    &.is-low {
      color: #d97706;
      background: #fef3c7;
    }

    &.is-zero {
      color: #dc2626;
      background: #fef2f2;
    }
  }

  &__card-qty {
    font-size: 32px;
    font-weight: 700;
    color: var(--wr-text, #333);
    line-height: 1.1;
    margin-bottom: 8px;
  }

  &__card-meta {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: 6px;
    font-size: 12px;
    color: var(--wr-text-secondary, #666);
    margin-bottom: 10px;
  }

  &__card-meta-sep {
    color: #cbd5e1;
  }

  &__card-value {
    font-size: 15px;
    font-weight: 700;
    color: var(--ec-stat-red, #dc2626);
  }

  &__card-progress {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__card-progress-bar {
    flex: 1;
    height: 6px;
    border-radius: 999px;
    background: #e2e8f0;
    overflow: hidden;
  }

  &__card-progress-fill {
    display: block;
    height: 100%;
    border-radius: 999px;
    background: #2563eb;
    transition: width 0.4s ease;

    &.is-danger {
      background: #dc2626;
    }
  }

  &__card-progress-label {
    font-size: 11px;
    color: var(--wr-muted, #999);
    white-space: nowrap;
  }

  &__loading,
  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 32px 20px;
    gap: 12px;
    font-size: 14px;
    color: var(--wr-muted, #999);
  }

  &__load-more {
    display: flex;
    justify-content: center;
    padding: 16px 0;
  }

  &__load-more-btn {
    padding: 10px 24px;
    border: 1px solid var(--wr-border, #e8ecef);
    border-radius: 8px;
    background: var(--wr-card, #fff);
    color: var(--wr-text, #333);
    font-size: 14px;
    font-weight: 600;
    font-family: inherit;
    cursor: pointer;
    transition: background 0.15s;

    &:active:not(:disabled) {
      background: #f3f4f6;
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  &__back-top {
    position: fixed;
    bottom: 42px;
    right: 16px;
    z-index: 100;
    width: 40px;
    height: 40px;
    border-radius: 10px;
    background: var(--wr-card, #fff);
    border: 1px solid var(--wr-border, #e8ecef);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--wr-text, #333);
    cursor: pointer;
    transition: opacity 0.3s, transform 0.15s;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    &:active {
      transform: scale(0.92);
    }
  }

  &__sheet-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.4);
    z-index: 200;
  }

  &__sheet {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    max-height: 70vh;
    background: var(--wr-card, #fff);
    border-radius: 16px 16px 0 0;
    z-index: 201;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    animation: v2-ec-sheet-up 0.3s ease;
  }

  @keyframes v2-ec-sheet-up {
    from { transform: translateY(100%); }
    to { transform: translateY(0); }
  }

  &__sheet-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 16px 12px;
    border-bottom: 1px solid var(--wr-border, #e8ecef);
    flex-shrink: 0;
  }

  &__sheet-title {
    font-size: 16px;
    font-weight: 700;
    color: var(--wr-text, #333);
  }

  &__sheet-close {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    border-radius: 8px;
    background: transparent;
    color: var(--wr-muted, #999);
    cursor: pointer;

    &:active {
      background: #f3f4f6;
    }
  }

  &__sheet-loading,
  &__sheet-empty {
    padding: 40px 16px;
    text-align: center;
    font-size: 14px;
    color: var(--wr-muted, #999);
  }

  &__sheet-list {
    overflow-y: auto;
    padding: 12px 16px 24px;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  &__sheet-card {
    background: var(--wr-bg, #f9f9fa);
    border-radius: 10px;
    border: 1px solid var(--wr-border, #e8ecef);
  }

  &__sheet-card-inner {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
  }

  &__sheet-card-thumb {
    width: 52px;
    height: 52px;
    border-radius: 8px;
    overflow: hidden;
    flex-shrink: 0;
    background: #f1f5f9;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    &--empty {
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  &__sheet-card-main {
    flex: 1;
    min-width: 0;
  }

  &__sheet-card-head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
  }

  &__sheet-card-code {
    font-size: 14px;
    font-weight: 600;
    color: var(--wr-text, #333);
  }

  &__sheet-card-spec {
    font-size: 11px;
    color: var(--wr-text-secondary, #666);
  }

  &__sheet-card-body {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__sheet-card-info {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  &__sheet-card-label {
    font-size: 11px;
    color: var(--wr-muted, #999);
  }

  &__sheet-card-qty {
    font-size: 15px;
    font-weight: 700;
    color: var(--wr-text, #333);
  }

  &__sheet-card-sep {
    color: #cbd5e1;
    margin: 0 4px;
  }

  &__sheet-card-adjust {
    padding: 6px 14px;
    border: none;
    border-radius: 8px;
    background: var(--ec-stat-blue, #2563eb);
    color: #fff;
    font-size: 12px;
    font-weight: 600;
    font-family: inherit;
    cursor: pointer;
    transition: transform 0.15s;

    &:active {
      transform: scale(0.94);
    }
  }

  &__sheet-card-footer {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-top: 6px;
    padding-top: 6px;
    border-top: 1px dashed #e2e8f0;
  }

  &__sheet-card-value {
    font-size: 14px;
    font-weight: 700;
    color: #ea580c;
  }

  &__adjust {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;
    padding: 20px 16px 28px;
  }

  &__adjust-preview {
    text-align: center;
  }

  &__adjust-preview-label {
    display: block;
    font-size: 12px;
    color: var(--wr-muted, #999);
    margin-bottom: 4px;
  }

  &__adjust-preview-qty {
    font-size: 36px;
    font-weight: 700;
    color: var(--wr-text, #333);
    line-height: 1.1;
  }

  &__adjust-type {
    display: flex;
    gap: 12px;
  }

  &__adjust-type-btn {
    padding: 10px 28px;
    border: 1px solid var(--wr-border, #e8ecef);
    border-radius: 10px;
    background: var(--wr-card, #fff);
    font-size: 15px;
    font-weight: 600;
    font-family: inherit;
    color: var(--wr-text-secondary, #666);
    cursor: pointer;
    transition: all 0.15s;

    &.is-active {
      border-color: var(--ec-stat-blue, #2563eb);
      color: var(--ec-stat-blue, #2563eb);
      background: #eff6ff;
    }

    &:active {
      transform: scale(0.96);
    }
  }

  &__adjust-stepper {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  &__adjust-stepper-btn {
    width: 40px;
    height: 40px;
    border: 1px solid var(--wr-border, #e8ecef);
    border-radius: 10px;
    background: var(--wr-card, #fff);
    font-size: 20px;
    font-weight: 700;
    color: var(--wr-text, #333);
    cursor: pointer;
    font-family: inherit;
    transition: transform 0.15s;

    &:active:not(:disabled) {
      transform: scale(0.92);
    }

    &:disabled {
      opacity: 0.3;
      cursor: not-allowed;
    }
  }

  &__adjust-stepper-val {
    font-size: 28px;
    font-weight: 700;
    color: var(--wr-text, #333);
    min-width: 48px;
    text-align: center;
  }

  &__adjust-after {
    font-size: 14px;
    color: var(--wr-text-secondary, #666);

    strong {
      font-size: 20px;
      font-weight: 700;
      color: #ea580c;
    }
  }

  &__adjust-submit {
    width: 100%;
    padding: 14px;
    border: none;
    border-radius: 10px;
    background: var(--ec-stat-blue, #2563eb);
    color: #fff;
    font-size: 16px;
    font-weight: 600;
    font-family: inherit;
    cursor: pointer;
    transition: transform 0.15s;

    &:active:not(:disabled) {
      transform: scale(0.97);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}

.v2-ec-collapse-enter-active,
.v2-ec-collapse-leave-active {
  overflow: hidden;
  transition: max-height 0.3s ease, opacity 0.25s ease;
}

.v2-ec-collapse-enter-from,
.v2-ec-collapse-leave-to {
  max-height: 0;
  opacity: 0;
}

.v2-ec-collapse-enter-to,
.v2-ec-collapse-leave-from {
  max-height: 1000px;
  opacity: 1;
}
</style>
