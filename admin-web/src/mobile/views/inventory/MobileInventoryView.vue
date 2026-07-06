<template>
  <div v-loading="loading" class="mobile-inventory-view">
    <div class="mobile-inventory-view__header">
      <div class="mobile-inventory-view__header-left">
        <MobileDoodleChip
          tag="button"
          type="button"
          shape="pill"
          color="#2563eb"
          class="mobile-inventory-view__back"
          @click="$router.back()"
        >
          <span>←</span>
        </MobileDoodleChip>
        <h1 class="mobile-inventory-view__title">🏫 库存中心</h1>
      </div>
    </div>

    <div class="mobile-inventory-view__content">
      <!-- 数据概览卡片 -->
      <SchemeADoodleFrame color="#2563eb" class="data-overview-card" sketch :stroke-width="2">
        <div class="data-overview-card__inner">
          <div class="data-overview-card__header">
            <span class="data-overview-card__icon">📊</span>
            <span class="data-overview-card__title">库存概览</span>
          </div>

          <div class="data-overview-card__stats">
            <div class="data-overview-card__stat">
              <div class="data-overview-card__stat-value">{{ summary.skuCount }}</div>
              <div class="data-overview-card__stat-label">SKU 数</div>
            </div>
            <div class="data-overview-card__stat">
              <div class="data-overview-card__stat-value">{{ summary.totalQty }}</div>
              <div class="data-overview-card__stat-label">总数量</div>
            </div>
            <div class="data-overview-card__stat">
              <div class="data-overview-card__stat-value">¥<span style="color: #9b0000">{{ summary.stockValue }}</span></div>
              <div class="data-overview-card__stat-label">库存价值</div>
            </div>
          </div>

          <div class="data-overview-card__divider"></div>

          <div class="data-overview-card__progress">
            <div class="data-overview-card__progress-row">
              <span class="data-overview-card__progress-label">正常</span>
              <div class="data-overview-card__progress-bar">
                <div
                  class="data-overview-card__progress-fill data-overview-card__progress-fill--green"
                  :style="{ width: healthStats.normalPct + '%' }"
                ></div>
              </div>
              <span class="data-overview-card__progress-value">{{ healthStats.normalPct }}%</span>
            </div>
            <div class="data-overview-card__progress-row">
              <span class="data-overview-card__progress-label">不足</span>
              <div class="data-overview-card__progress-bar">
                <div
                  class="data-overview-card__progress-fill data-overview-card__progress-fill--orange"
                  :style="{ width: healthStats.lowPct + '%' }"
                ></div>
              </div>
              <span class="data-overview-card__progress-value">{{ healthStats.lowPct }}%</span>
            </div>
            <div class="data-overview-card__progress-row">
              <span class="data-overview-card__progress-label">缺货</span>
              <div class="data-overview-card__progress-bar">
                <div
                  class="data-overview-card__progress-fill data-overview-card__progress-fill--red"
                  :style="{ width: healthStats.zeroPct + '%' }"
                ></div>
              </div>
              <span class="data-overview-card__progress-value">{{ healthStats.zeroPct }}%</span>
            </div>
          </div>
        </div>
      </SchemeADoodleFrame>


      <!-- 库存预警区 -->
      <div v-if="alertItems.length > 0" class="mobile-inventory-view__section">
        <div class="collapsible-header" @click="alertExpanded = !alertExpanded">
          <MobileSectionHeader :icon="schemeAAssets.squiggleRed" title="库存预警">
            <template #actions>
              <span class="collapsible-header__count">{{ alertItems.length }}件</span>
              <img
                  class="collapsible-header__toggle"
                  :class="{ 'is-expanded': alertExpanded }"
                  :src="schemeAAssets.chevronDown"
                  alt=""
              />
            </template>
          </MobileSectionHeader>
        </div>

        <transition name="alert-collapse">
          <div v-show="alertExpanded">
            <MobileCardGrid :items="alertItems" empty-text="">
              <template #card="{ item }">
                <SchemeADoodleFrame
                    :key="item.id"
                    :seed="item.id"
                    tag="button" type="button"
                    class="alert-card"
                    :color="(item.quantity ?? 0) <= 0 ? '#ef4444' : '#f59e0b'"
                    sketch :stroke-width="2"
                    :shadow="false"
                    @click="handleItemClick(item)"
                >
                  <div class="alert-card__inner">
                    <div class="alert-card__icon">{{ (item.quantity ?? 0) <= 0 ? '🚨' : '⚠️' }}</div>
                    <div class="alert-card__content">
                      <div class="alert-card__name">{{ item.productName || item.skuCode }}</div>
                      <div class="alert-card__spec">{{ item.specName }}</div>
                      <div class="alert-card__qty">
                        <span class="alert-card__qty-num">{{ item.quantity }}</span>件
                      </div>
                    </div>
                  </div>
                </SchemeADoodleFrame>
              </template>
            </MobileCardGrid>
          </div>
        </transition>
      </div>

      <!-- 状态筛选标签 -->
      <MobileCategoryTabs
          :categories="statusTabs"
          v-model:active-value="activeStatus"
          active-color="#2563eb"
          inactive-color="#94a3b8"
      />

      <!-- 搜索框 -->
      <MobileDoodleSearch
        v-model="keyword"
        placeholder="搜索 SKU 编码或产品名称..."
      />

      <!-- 商品库存列表 -->
      <div class="mobile-inventory-view__section">
        <div class="collapsible-header" @click="inventoryListExpanded = !inventoryListExpanded">
          <MobileSectionHeader
            :icon="schemeAAssets.starBlue"
            title="库存列表"
          >
            <template #actions>
              <span class="collapsible-header__count">{{ displayItems.length }}项</span>
              <img
                class="collapsible-header__toggle"
                :class="{ 'is-expanded': inventoryListExpanded }"
                :src="schemeAAssets.chevronDown"
                alt=""
              />
            </template>
          </MobileSectionHeader>
        </div>

        <template v-if="inventoryListExpanded">
          <div v-if="loadingList && !displayItems.length" class="list-loading">
            <span>加载中...</span>
          </div>

          <template v-else>
            <div class="inventory-list">
            <SchemeADoodleFrame
              v-for="item in displayItems"
              :key="item.listKey ?? item.id"
              tag="button" type="button"
              class="inventory-card"
              :seed="Number(item.id)"
              :color="getCardColor(item)"
              sketch :stroke-width="2"
              :shadow="false"
              @click="handleItemClick(item)"
            >
              <div class="inventory-card__inner">
                <!-- SKU 编码 + 名称 -->
                <div class="inventory-card__head">
                  <div class="inventory-card__sku">
                    <span class="inventory-card__code">{{ item.skuCode }}</span>
                    <span class="inventory-card__name">
                      {{ item.spuSkuCount && item.spuSkuCount > 1
                        ? `共 ${item.spuSkuCount} 个规格`
                        : (item.productName || item.specName || '—')
                      }}
                    </span>
                  </div>
                  <span
                    class="inventory-card__status-tag"
                    :class="getStatusTagClass(item)"
                  >
                    {{ getStatusLabel(item) }}
                  </span>
                </div>

                <!-- 库存数量 -->
                <div class="inventory-card__qty">{{ item.quantity ?? 0 }}</div>

                <!-- 在途 + 库存价值 -->
                <div class="inventory-card__meta">
                  <span class="inventory-card__meta-item">
                    在途 {{ item.inTransitQty ?? 0 }}
                  </span>
                  <span class="inventory-card__meta-sep">·</span>
                  <span class="inventory-card__meta-item">库存价值 </span>
                  <span class="inventory-card__value">¥{{ stockValue(item) }}</span>
                </div>

                <!-- 库存水位进度条 -->
                <div class="inventory-card__progress">
                  <div class="inventory-card__progress-bar">
                    <span
                      class="inventory-card__progress-fill"
                      :class="{ 'is-danger': item.alertActive }"
                      :style="{ width: stockLevelPct(item) + '%' }"
                    />
                  </div>
                  <span class="inventory-card__progress-label">
                    {{ item.quantity ?? 0 }}
                  </span>
                </div>

              </div>
            </SchemeADoodleFrame>
          </div>

          <div v-if="!displayItems.length" class="empty-state">
            <span class="empty-state__icon">📭</span>
            <span class="empty-state__text">暂无库存数据</span>
          </div>

          <!-- 加载更多 -->
          <div v-if="hasMore" class="load-more">
            <MobileDoodleChip
              tag="button" type="button"
              color="#2563eb"
              :disabled="loadingMore"
              @click="loadMore"
            >
              {{ loadingMore ? '加载中...' : '加载更多' }}
            </MobileDoodleChip>
          </div>
        </template>
        </template>
      </div>
    </div>

    <!-- SPU 详情弹窗 -->
    <MobileBottomSheet v-model="skuSheetVisible" :title="skuSheetTitle" :loading="skuSheetLoading">
      <template v-if="skuSheetItems.length">
        <div class="sku-sheet-list">
          <SchemeADoodleFrame
            v-for="sku in skuSheetItems"
            :key="sku.id"
            :seed="sku.id"
            tag="div"
            class="sku-sheet-card"
            :color="getCardColor(sku)"
            sketch :stroke-width="2"
            :shadow="false"
          >
             <div class="sku-sheet-card__inner">
               <img
                 v-if="sku.imageName"
                 :src="getEcommerceImageUrl(sku.imageName)"
                 class="sku-sheet-card__thumb"
                 alt=""
               />
               <div v-else class="sku-sheet-card__thumb sku-sheet-card__thumb--empty">
                 <span>📷</span>
               </div>
               <div class="sku-sheet-card__main">
                 <div class="sku-sheet-card__head">
                   <span class="sku-sheet-card__code">{{ sku.skuCode }}</span>
                   <span class="sku-sheet-card__spec">{{ sku.specName || '—' }}</span>
                 </div>
                 <div class="sku-sheet-card__body">
                   <div class="sku-sheet-card__info">
                     <span class="sku-sheet-card__label">库存</span>
                     <strong class="sku-sheet-card__qty">{{ sku.quantity ?? 0 }}</strong>
                     <span class="sku-sheet-card__sep">·</span>
                     <span class="sku-sheet-card__label">在途</span>
                     <strong class="sku-sheet-card__qty">{{ sku.inTransitQty ?? 0 }}</strong>
                   </div>
                   <button
                     type="button"
                     class="sku-sheet-card__adjust-btn"
                     @click.stop="openSkuAdjust(sku)"
                   >
                     调整
                   </button>
                 </div>
                 <div class="sku-sheet-card__footer">
                   <span class="sku-sheet-card__label">价值</span>
                   <strong class="sku-sheet-card__value">¥{{ stockValue(sku) }}</strong>
                 </div>
               </div>
             </div>
          </SchemeADoodleFrame>
        </div>
      </template>
      <template v-else>
        <div class="sku-sheet-empty">该 SPU 下暂无库存数据</div>
      </template>
    </MobileBottomSheet>

    <!-- SKU 调整弹窗 -->
    <MobileBottomSheet v-model="adjustSheetVisible" title="库存调整">
      <template v-if="adjustTarget">
        <div class="sku-adjust">
          <div class="sku-adjust__preview">
            <span class="sku-adjust__preview-label">当前库存</span>
            <strong class="sku-adjust__preview-qty">{{ adjustTarget.quantity ?? 0 }}</strong>
          </div>

          <div class="sku-adjust__type">
            <button
              type="button"
              class="sku-adjust__type-btn"
              :class="{ 'is-active': adjustForm.changeType === 'DEDUCT' }"
              @click="adjustForm.changeType = 'DEDUCT'"
            >扣除</button>
            <button
              type="button"
              class="sku-adjust__type-btn"
              :class="{ 'is-active': adjustForm.changeType === 'RECLAIM' }"
              @click="adjustForm.changeType = 'RECLAIM'"
            >回收</button>
          </div>

          <div class="sku-adjust__stepper">
            <button type="button" class="sku-adjust__stepper-btn" :disabled="adjustForm.changeQty <= 1" @click="adjustForm.changeQty--">−</button>
            <span class="sku-adjust__stepper-val">{{ adjustForm.changeQty }}</span>
            <button type="button" class="sku-adjust__stepper-btn" @click="adjustForm.changeQty++">+</button>
          </div>

          <div class="sku-adjust__after">
            调整后：<strong>{{ adjustAfterQty }}</strong> 件
          </div>

          <button
            type="button"
            class="sku-adjust__submit"
            :disabled="adjustSubmitting"
            @click="submitAdjust"
          >
            {{ adjustSubmitting ? '提交中...' : '确认调整' }}
          </button>
        </div>
      </template>
    </MobileBottomSheet>

    <!-- 回到顶部 -->
    <transition name="back-top-fade">
      <div v-show="showBackToTop" class="back-to-top">
        <SchemeADoodleFrame
          tag="button" type="button"
          color="#9b0000"
          sketch :stroke-width="2"
          :shadow="true"
          @click="scrollToTop"
        >
          <span class="back-to-top__icon">↑</span>
        </SchemeADoodleFrame>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, reactive, ref, watch} from 'vue'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import {schemeAAssets} from '@/mobile/views/home/themes/scheme-a/assets'
import {getEcommerceImageUrl} from '@/api/ecommerce/image'
import MobileDoodleSearch from '@/mobile/components/MobileDoodleSearch.vue'
import type {CategoryItem} from '@/mobile/components/MobileCategoryTabs.vue'
import MobileCategoryTabs from '@/mobile/components/MobileCategoryTabs.vue'
import MobileSectionHeader from '@/mobile/components/MobileSectionHeader.vue'
import MobileCardGrid from '@/mobile/components/MobileCardGrid.vue'
import MobileBottomSheet from '@/mobile/components/MobileBottomSheet.vue'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import {type EcInventory, adjustInventory, fetchInventories, fetchInventoryByProduct, fetchInventoryInboundValueSummary, fetchInventoryOverview, fetchSpuStatusCounts, type EcInventoryOverview, type EcInventorySpuStatus,} from '@/api/ecommerce/inventory'
import {
  classifyInventory,
  type InventoryStatusKey,
  type InventoryStatusStats,
} from '@/utils/inventoryStats'

// ====== 状态定义 ======
const loading = ref(true)
const loadingList = ref(false)
const loadingMore = ref(false)
const keyword = ref('')
const activeStatus = ref<string | number | null>('all')
const alertExpanded = ref(false) // 库存预警默认收起
const inventoryListExpanded = ref(false) // 库存列表默认收起

// 分页
const page = ref(1)
const pageSize = 20
const total = ref(0)
const records = ref<EcInventory[]>([])
const extra = ref<Record<string, unknown> | undefined>()

// 统计
const statsItems = ref<EcInventory[]>([])
const overview = ref<EcInventoryOverview | undefined>()
const spuStatus = ref<EcInventorySpuStatus | undefined>()
const totalInboundValue = ref<number | null>(null)

// ====== 回到顶部 ======
const showBackToTop = ref(false)

onMounted(() => {
  const scrollEl = document.querySelector('.mobile-app__main')
  if (!scrollEl) return
  const handler = () => { showBackToTop.value = scrollEl.scrollTop > 300 }
  scrollEl.addEventListener('scroll', handler)
})

onUnmounted(() => {
  // onUnmounted 在 keep-alive 切换时不会触发
})

function scrollToTop() {
  document.querySelector('.mobile-app__main')?.scrollTo({ top: 0, behavior: 'smooth' })
}

// ====== 状态筛选 tabs ======
const statusTabs = computed<CategoryItem[]>(() => {
  const s = inventoryStats.value
  return [
    { id: 'all', name: '全部', icon: '📦', count: s.total },
    { id: 'normal', name: '正常', icon: '✅', count: s.normal },
    { id: 'low', name: '不足', icon: '⚠️', count: s.low },
    { id: 'zero', name: '缺货', icon: '🚨', count: s.zero },
  ]
})

// ====== 库存统计（SPU 维度） ======
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

// ====== 筛选与展示 ======
const displayItems = computed(() => {
  let items = records.value

  // 状态过滤（SPU 维度）
  if (activeStatus.value !== 'all') {
    items = items.filter((row) => classifyInventory(row) === activeStatus.value)
  }

  // 关键词过滤
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

// ====== 工具函数 ======
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

function getCardColor(row: EcInventory): string {
  const status = classifyInventory(row)
  if (status === 'zero') return '#ef4444'
  if (status === 'low') return '#f59e0b'
  return '#22c55e'
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

// ====== API 请求 ======
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

// ====== 搜索防抖 ======
let keywordTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (keywordTimer) clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => loadList(true), 300)
})

// ====== 交互事件 ======
// ====== SPU 详情弹窗 ======
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

// ====== SKU 调整 ======
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

    // 刷新 SPU 详情和数据
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
.mobile-inventory-view {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  font-family: 'ZCOOL KuaiLe', 'Alibaba PuHuiTi', 'PingFang SC', sans-serif;
  padding-bottom: 100px;
}

.mobile-inventory-view__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: max(16px, env(safe-area-inset-top)) 16px 12px;
}

.mobile-inventory-view__header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-inventory-view__back {
  width: 36px;
  height: 36px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #2563eb;
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

.mobile-inventory-view__title {
  font-size: 24px;
  margin: 0;
  color: #1e293b;
}

.mobile-inventory-view__content {
  flex: 1;
  padding: 0 16px 20px;
  overflow-y: auto;
}

.mobile-inventory-view__section {
  margin: 12px 8px;
}

/* ===== 数据概览卡片 ===== */
.data-overview-card {
  margin-bottom: 14px;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.data-overview-card__inner {
  padding: 28px;
}

.data-overview-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.data-overview-card__icon {
  font-size: 20px;
}

.data-overview-card__title {
  font-size: 18px;
  font-weight: 800;
  color: #2563eb;
}

.data-overview-card__stats {
  display: flex;
  justify-content: space-around;
  gap: 8px;
  margin-bottom: 16px;
}

.data-overview-card__stat {
  flex: 1;
  text-align: center;
}

.data-overview-card__stat-value {
  font-size: 24px;
  font-weight: 800;
  color: #1e293b;
}

.data-overview-card__stat-label {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.data-overview-card__divider {
  height: 2px;
  background: repeating-linear-gradient(
    90deg,
    #93c5fd,
    #93c5fd 6px,
    transparent 6px,
    transparent 10px
  );
  margin-bottom: 14px;
}

.data-overview-card__progress {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.data-overview-card__progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.data-overview-card__progress-label {
  font-size: 12px;
  color: #64748b;
  width: 50px;
  flex-shrink: 0;
}

.data-overview-card__progress-bar {
  flex: 1;
  height: 12px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
}

.data-overview-card__progress-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.6s ease;

  &--green { background: linear-gradient(90deg, #22c55e, #4ade80); }
  &--orange { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
  &--red { background: linear-gradient(90deg, #ef4444, #f87171); }
  &--gray { background: linear-gradient(90deg, #94a3b8, #cbd5e1); }
}

.data-overview-card__progress-value {
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;
  width: 40px;
  text-align: right;
}

/* ===== 快捷操作 ===== */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 16px;
}

.quick-action-btn {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:active { transform: scale(0.93); }

  :deep(.sa-doodle-frame__body) { padding: 0; }
}

.quick-action-btn__inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 4px;
}

.quick-action-btn__icon {
  font-size: 24px;
  line-height: 1;
}

.quick-action-btn__text {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 12px;
  color: #1e293b;
}

/* ===== 库存预警 ===== */
.collapsible-header {
  cursor: pointer;
  user-select: none;

  .mobile-section-header {
    margin-bottom: 0;
  }
}

.collapsible-header__count {
  font-size: 12px;
  color: #94a3b8;
  margin-right: 6px;
  white-space: nowrap;
}

.collapsible-header__toggle {
  width: 18px;
  height: 18px;
  transition: transform 0.25s ease;
  flex-shrink: 0;

  &.is-expanded {
    transform: rotate(180deg);
  }
}

.alert-collapse-enter-active,
.alert-collapse-leave-active {
  overflow: hidden;
  transition: max-height 0.3s ease, opacity 0.25s ease;
}

.alert-collapse-enter-from,
.alert-collapse-leave-to {
  max-height: 0;
  opacity: 0;
}

.alert-collapse-enter-to,
.alert-collapse-leave-from {
  max-height: 1000px;
  opacity: 1;
}

.alert-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:active { transform: scale(0.98); }

  :deep(.sa-doodle-frame__body) { padding: 0; }
}

.alert-card__inner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
}

.alert-card__icon {
  font-size: 22px;
  flex-shrink: 0;
}

.alert-card__content {
  flex: 1;
  min-width: 0;
}

.alert-card__name {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 1px;
}

.alert-card__spec {
  font-size: 10px;
  color: #64748b;
  margin-bottom: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.alert-card__qty {
  font-size: 11px;
  color: #64748b;
}

.alert-card__qty-num {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

/* ===== 库存卡片列表 ===== */
.inventory-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.inventory-card {
  cursor: pointer;
  transition: transform 0.15s ease;

  &:active { transform: scale(0.98); }

  :deep(.sa-doodle-frame__body) { padding: 0; }
}

.inventory-card__inner {
  padding: 24px;
}

.inventory-card__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
}

.inventory-card__sku {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.inventory-card__code {
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
}

.inventory-card__name {
  font-size: 12px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inventory-card__status-tag {
  flex-shrink: 0;
  font-size: 10px;
  font-weight: 800;
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

.inventory-card__qty {
  font-size: 36px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.1;
  margin-bottom: 8px;
}

.inventory-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px;
  font-size: 12px;
  margin-bottom: 10px;
}

.inventory-card__meta-item {
  color: #64748b;
}

.inventory-card__meta-sep {
  color: #cbd5e1;
}

.inventory-card__value {
  font-size: 16px;
  font-weight: 700;
  color: #9b0000;
  line-height: 1.2;
}

.inventory-card__progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.inventory-card__progress-bar {
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.inventory-card__progress-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: #2563eb;

  &.is-danger {
    background: #dc2626;
  }
}

.inventory-card__progress-label {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
}

.inventory-card__actions {
  display: flex;
  gap: 8px;

  .mobile-doodle-chip {
    flex: 1;
    font-family: inherit;
    font-size: 12px;
    font-weight: 800;
    cursor: pointer;
    background: #fff;
    text-align: center;

    :deep(.sa-doodle-frame__body) {
      padding: 6px 8px;
      text-align: center;
    }
  }
}

/* ===== 空状态与加载 ===== */
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

/* ===== 新增按钮 ===== */
.mobile-inventory-view__fab {
  position: fixed;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
}

.add-btn {
  padding: 14px 28px;
  cursor: pointer;
  background: #2563eb;
  transition: transform 0.2s ease;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.35);

  &:active {
    transform: translateX(-50%) scale(0.95);
  }
}

.add-btn__text {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 18px;
  color: white;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

/* ===== SPU 详情弹窗 ===== */
.sku-sheet-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sku-sheet-card {
  cursor: default;

  :deep(.sa-doodle-frame__body) { padding: 0; }
}

.sku-sheet-card__inner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
}

.sku-sheet-card__thumb {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  border-radius: 8px;
  object-fit: cover;
  background: #f1f5f9;
  margin-left: 12px;
}

.sku-sheet-card__thumb--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.sku-sheet-card__main {
  flex: 1;
  min-width: 0;
}

.sku-sheet-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.sku-sheet-card__code {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.sku-sheet-card__spec {
  font-size: 12px;
  color: #64748b;
}

.sku-sheet-card__body {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sku-sheet-card__footer {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  padding-top: 6px;
  border-top: 1px dashed #e2e8f0;
}

.sku-sheet-card__footer .sku-sheet-card__value {
  font-size: 14px;
}

.sku-sheet-card__info {
  display: flex;
  align-items: center;
  gap: 4px;
}

.sku-sheet-card__label {
  font-size: 12px;
  color: #94a3b8;
}

.sku-sheet-card__qty {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.sku-sheet-card__value {
  font-size: 16px;
  font-weight: 800;
  color: #ea580c;
}

.sku-sheet-card__sep {
  color: #cbd5e1;
  margin: 0 4px;
}

.sku-sheet-card__adjust-btn {
  padding: 6px 14px;
  border: none;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  transition: transform 0.15s ease;

  &:active { transform: scale(0.94); }
}

.sku-sheet-empty {
  text-align: center;
  padding: 40px 0;
  font-size: 14px;
  color: #94a3b8;
}

/* ===== SKU 调整弹窗 ===== */
.sku-adjust {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 8px 0;
}

.sku-adjust__preview {
  text-align: center;
}

.sku-adjust__preview-label {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.sku-adjust__preview-qty {
  font-size: 40px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.1;
}

.sku-adjust__type {
  display: flex;
  gap: 12px;
}

.sku-adjust__type-btn {
  padding: 10px 28px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  font-size: 15px;
  font-weight: 700;
  font-family: inherit;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s ease;

  &.is-active {
    border-color: #2563eb;
    color: #2563eb;
    background: #eff6ff;
  }

  &:active { transform: scale(0.96); }
}

.sku-adjust__stepper {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sku-adjust__stepper-btn {
  width: 40px;
  height: 40px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;

  &:active:not(:disabled) { transform: scale(0.92); }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}

.sku-adjust__stepper-val {
  font-size: 28px;
  font-weight: 800;
  color: #1e293b;
  min-width: 48px;
  text-align: center;
}

.sku-adjust__after {
  font-size: 14px;
  color: #64748b;

  strong {
    font-size: 20px;
    font-weight: 800;
    color: #ea580c;
  }
}

.sku-adjust__submit {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 12px;
  background: #2563eb;
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  font-family: inherit;
  cursor: pointer;
  transition: transform 0.15s ease;

  &:active:not(:disabled) { transform: scale(0.97); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

/* ===== 回到顶部 ===== */
.back-to-top {
  position: fixed;
  bottom: 42px;
  right: 16px;
  z-index: 100;
  cursor: pointer;
  transition: transform 0.15s ease;

  &:active { transform: scale(0.88); }

  :deep(.sa-doodle-frame__body) {
    width: 44px;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.back-to-top__icon {
  font-size: 22px;
  font-weight: 700;
  color: #009b00;
  line-height: 1;
}

.back-top-fade-enter-active,
.back-top-fade-leave-active {
  transition: opacity 0.3s ease;
}

.back-top-fade-enter-from,
.back-top-fade-leave-to {
  opacity: 0;
}
</style>