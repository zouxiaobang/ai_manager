<template>
  <V2Page>
    <div class="v2-ec">
      <div class="v2-ec-search">
        <svg class="v2-ec-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input
          v-model="products.searchQuery.value"
          class="v2-ec-search__input"
          :placeholder="products.t('ecommerce.product.searchPlaceholder')"
          type="search"
        />
      </div>

      <div v-if="products.stats" class="v2-ec-product-stats">
        <div class="v2-ec-product-stats__row">
          <div class="v2-ec-product-stats__item">
            <span class="v2-ec-product-stats__val__1">{{ products.stats.totalProducts }}</span>
            <span class="v2-ec-product-stats__lbl">SPU</span>
          </div>
          <div class="v2-ec-product-stats__item">
            <span class="v2-ec-product-stats__val__2">{{ products.stats.totalSkus }}</span>
            <span class="v2-ec-product-stats__lbl">SKU</span>
          </div>
          <div class="v2-ec-product-stats__item">
            <span class="v2-ec-product-stats__val__3">{{ products.stats.totalFactories }}</span>
            <span class="v2-ec-product-stats__lbl">工厂</span>
          </div>
        </div>
        <div class="v2-ec-product-stats__divider" />
        <div class="v2-ec-product-stats__health">
          <span class="v2-ec-product-stats__health-label">启用率</span>
          <div class="v2-ec-product-stats__health-bar">
            <div class="v2-ec-product-stats__health-fill" :style="{ width: healthEnabledPct + '%' }" />
          </div>
          <span class="v2-ec-product-stats__health-pct">{{ healthEnabledPct }}%</span>
        </div>
      </div>

      <div class="v2-ec-tabs">
        <button
          v-for="opt in filteredFactoryOptions"
          :key="opt.value"
          class="v2-ec-tab"
          :class="{ 'is-active': products.selectedFactoryId.value === opt.value }"
          @click="products.setFactoryFilter(opt.value)"
        >
          {{ opt.label }}
          <span class="v2-ec-tab__count">{{ opt.productCount }}</span>
        </button>
      </div>

      <div v-if="products.groupedByFactory.value.length" class="v2-ec-product-groups">
        <div v-for="group in products.groupedByFactory.value" :key="group.factoryId ?? 'no-factory'" class="v2-ec-factory-group">
          <div class="v2-ec-factory-group__header">
            <h3 class="v2-ec-factory-group__title">{{ group.factoryName }}</h3>
            <span class="v2-ec-factory-group__count">{{ group.products?.length }} 个商品</span>
          </div>
          <div class="v2-ec-product-list">
            <div
              v-for="item in group.products"
              :key="item.id"
              class="v2-ec-product-card"
              @click="handleDetail(item)"
            >
              <div class="v2-ec-product-card__img">
                <img v-if="item.imageName" :src="productImageUrl(item.imageName)" :alt="item.name" />
                <span v-else class="v2-ec-product-card__emoji">{{ productEmoji(item.name) }}</span>
              </div>
              <div class="v2-ec-product-card__info">
                <div class="v2-ec-product-card__name">{{ item.name }}</div>
                <div class="v2-ec-product-card__skus">{{ item.skuCount ?? 0 }} 个SKU · 退点: {{ item.rebatePct ?? 0 }}%</div>
                <span
                  class="v2-ec-product-card__status"
                  :class="item.status === 'ENABLED' ? 'status--enabled' : 'status--disabled'"
                >
                  {{ item.status === 'ENABLED' ? '已启用' : '已禁用' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="products.hasMore.value" style="margin-top: 4px;">
        <button class="v2-ec-load-more" @click="products.loadMore()">
          {{ products.t('loadMore') }}
        </button>
      </div>

      <div v-if="!products.groupedByFactory.value.length && !products.loading.value" class="v2-ec-empty">
        {{ products.searchQuery.value ? '未找到匹配的商品' : '暂无商品数据' }}
      </div>

      <Teleport to="body">
        <Transition name="v2-ec-slide">
          <div v-if="detailVisible" class="v2-ec-detail-overlay" @click.self="detailVisible = false">
            <div class="v2-ec-detail-sheet">
              <div class="v2-ec-detail-sheet__handle" />
              <div v-if="detail" class="v2-ec-detail-sheet__body">
                <div class="v2-ec-detail-sheet__header">
                  <div class="v2-ec-detail-sheet__img">
                    <img v-if="detail.imageName" :src="productImageUrl(detail.imageName)" :alt="detail.name" />
                    <span v-else style="font-size: 36px;">{{ productEmoji(detail.name) }}</span>
                  </div>
                  <div class="v2-ec-detail-sheet__info">
                    <h3 class="v2-ec-detail-sheet__title">{{ detail.name }}</h3>
                    <p v-if="detail.factoryName" class="v2-ec-detail-sheet__factory">{{ detail.factoryName }}</p>
                    <div class="v2-ec-detail-sheet__badges">
                      <span
                        class="v2-ec-detail-sheet__badge"
                        :class="detail.status === 'ENABLED' ? 'badge--green' : 'badge--gray'"
                      >
                        {{ detail.status === 'ENABLED' ? '可售' : '禁售' }}
                      </span>
                      <span class="v2-ec-detail-sheet__badge badge--rebate">退点: {{ detail.rebatePct }}%</span>
                    </div>
                  </div>
                </div>
                <div class="v2-ec-detail-sheet__label-row">
                  <div class="v2-ec-detail-sheet__label">SKU 列表</div>
                  <span class="v2-ec-detail-sheet__label-count">{{ detail.skus?.length ?? 0 }} 个SKU</span>
                </div>
                <div v-if="!detail.skus?.length" class="v2-ec-detail-sheet__no-data">
                  暂无 SKU 数据
                </div>
                <div
                  v-for="sku in detail.skus"
                  :key="sku.id"
                  class="v2-ec-detail-sheet__sku-card"
                  @click="selectedSku = sku; skuSheetVisible = true"
                >
                  <div class="v2-ec-detail-sheet__sku-top">
                    <span class="v2-ec-detail-sheet__sku-code">{{ sku.skuCode }}</span>
                    <span
                      class="v2-ec-detail-sheet__sku-chip"
                      :class="skuStatusChipClass(sku.status)"
                    >
                      {{ sku.status }}
                    </span>
                  </div>
                  <div class="v2-ec-detail-sheet__sku-grid">
                    <div class="v2-ec-detail-sheet__sku-field">
                      <span class="v2-ec-detail-sheet__sku-label">规格</span>
                      <span class="v2-ec-detail-sheet__sku-val v2-ec-detail-sheet__sku-val--bold">{{ sku.specName || '-' }}</span>
                    </div>
                    <div class="v2-ec-detail-sheet__sku-field">
                      <span class="v2-ec-detail-sheet__sku-label">售价</span>
                      <span class="v2-ec-detail-sheet__sku-val v2-ec-detail-sheet__sku-val--price">¥{{ sku.salePrice ?? '-' }}</span>
                    </div>
                    <div class="v2-ec-detail-sheet__sku-field">
                      <span class="v2-ec-detail-sheet__sku-label">退点</span>
                      <span class="v2-ec-detail-sheet__sku-val v2-ec-detail-sheet__sku-val--rebate">{{ sku.rebatePct ?? '-' }}%</span>
                    </div>
                    <div class="v2-ec-detail-sheet__sku-field">
                      <span class="v2-ec-detail-sheet__sku-label">尺寸</span>
                      <span class="v2-ec-detail-sheet__sku-val">{{ dimStr(sku) }}</span>
                    </div>
                  </div>
                  <div v-if="sku.cartonName" class="v2-ec-detail-sheet__sku-carton-row">
                    📦 {{ sku.cartonName }} · {{ sku.unitsPerCarton }}件/箱
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>

      <Teleport to="body">
        <Transition name="v2-ec-slide">
          <div v-if="skuSheetVisible" class="v2-ec-detail-overlay v2-ec-detail-overlay--nested" @click.self="skuSheetVisible = false">
            <div class="v2-ec-detail-sheet v2-ec-detail-sheet--nested">
              <div class="v2-ec-detail-sheet__handle" />
              <div class="v2-ec-detail-sheet__body">
                <div v-if="selectedSku" class="v2-ec-detail-sheet__sku-top-header">
                  <div class="v2-ec-detail-sheet__sku-top-img">
                    <img v-if="selectedSku.imageName" :src="productImageUrl(selectedSku.imageName)" :alt="selectedSku.skuCode" />
                    <span v-else class="v2-ec-detail-sheet__sku-top-emoji">{{ productEmoji(selectedSku.specName || selectedSku.skuCode) }}</span>
                  </div>
                  <div class="v2-ec-detail-sheet__sku-top-info">
                    <div class="v2-ec-detail-sheet__sku-top-row">
                      <span class="v2-ec-detail-sheet__sku-top-code">{{ selectedSku.skuCode }}</span>
                      <div class="v2-ec-detail-sheet__sku-top-actions">
                        <span
                          class="v2-ec-detail-sheet__sku-chip"
                          :class="selectedSku.status === 'ON_SALE' ? 'chip--green' : selectedSku.status === 'DRAFT' ? 'chip--yellow' : 'chip--gray'"
                        >
                          {{ selectedSku.status === 'ON_SALE' ? '在售' : selectedSku.status === 'DRAFT' ? '草稿' : '下架' }}
                        </span>
                        <button
                          type="button"
                          class="v2-ec-detail-sheet__card-btn"
                          title="生成 SKU 名片"
                          @click="skuCardVisible = true"
                        >
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                            <circle cx="8.5" cy="8.5" r="1.5"/>
                            <polyline points="21 15 16 10 5 21"/>
                          </svg>
                        </button>
                      </div>
                    </div>
                    <div class="v2-ec-detail-sheet__sku-top-name">{{ selectedSku.specName || '-' }}</div>
                    <div class="v2-ec-detail-sheet__sku-top-prices">
                      <span class="v2-ec-detail-sheet__sku-top-price">¥{{ selectedSku.salePrice ?? '-' }}</span>
                      <span class="v2-ec-detail-sheet__sku-top-rebate">退点: {{ selectedSku.rebatePct ?? 0 }}%</span>
                    </div>
                  </div>
                </div>

                <div v-if="selectedSku" class="v2-ec-detail-sheet__sku-section">
                  <div class="v2-ec-detail-sheet__sku-section-label">⭐单品尺寸</div>
                  <div class="v2-ec-detail-sheet__sku-dim-grid">
                    <div class="v2-ec-detail-sheet__sku-dim-item">
                      <span class="v2-ec-detail-sheet__sku-dim-label">长(L)</span>
                      <span class="v2-ec-detail-sheet__sku-dim-val">{{ selectedSku.productLengthCm ?? '-' }} cm</span>
                    </div>
                    <div class="v2-ec-detail-sheet__sku-dim-item">
                      <span class="v2-ec-detail-sheet__sku-dim-label">宽(W)</span>
                      <span class="v2-ec-detail-sheet__sku-dim-val">{{ selectedSku.productWidthCm ?? '-' }} cm</span>
                    </div>
                    <div class="v2-ec-detail-sheet__sku-dim-item">
                      <span class="v2-ec-detail-sheet__sku-dim-label">高(H)</span>
                      <span class="v2-ec-detail-sheet__sku-dim-val">{{ selectedSku.productHeightCm ?? '-' }} cm</span>
                    </div>
                  </div>
                  <div class="v2-ec-detail-sheet__sku-weight-row">
                    <span class="v2-ec-detail-sheet__sku-weight-label">单品重</span>
                    <span class="v2-ec-detail-sheet__sku-weight-val">{{ computeUnitWeight(selectedSku) }}</span>
                  </div>
                </div>

                <div v-if="selectedSku" class="v2-ec-detail-sheet__sku-section">
                  <div class="v2-ec-detail-sheet__sku-section-label">⭐外箱信息</div>
                  <div class="v2-ec-detail-sheet__sku-carton-grid">
                    <div class="v2-ec-detail-sheet__sku-carton-cell">
                      <span class="v2-ec-detail-sheet__sku-carton-key">外箱尺寸</span>
                      <span class="v2-ec-detail-sheet__sku-carton-val">{{ formatCartonSize(selectedSku) }}</span>
                    </div>
                    <div class="v2-ec-detail-sheet__sku-carton-cell">
                      <span class="v2-ec-detail-sheet__sku-carton-key">箱装数</span>
                      <span class="v2-ec-detail-sheet__sku-carton-val">{{ selectedSku.unitsPerCarton ?? '-' }} 件/箱</span>
                    </div>
                    <div class="v2-ec-detail-sheet__sku-carton-cell">
                      <span class="v2-ec-detail-sheet__sku-carton-key">毛重</span>
                      <span class="v2-ec-detail-sheet__sku-carton-val">{{ formatWeight(selectedSku.cartonGrossWeightKg) }}</span>
                    </div>
                    <div class="v2-ec-detail-sheet__sku-carton-cell">
                      <span class="v2-ec-detail-sheet__sku-carton-key">净重</span>
                      <span class="v2-ec-detail-sheet__sku-carton-val">{{ formatWeight(selectedSku.cartonNetWeightKg) }}</span>
                    </div>
                  </div>
                  <div v-if="selectedSku.cartonName" class="v2-ec-detail-sheet__sku-carton-name">
                    📦 {{ selectedSku.cartonName }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>

      <V2ProductsSkuCard
        :visible="skuCardVisible"
        :sku="selectedSku"
        @close="skuCardVisible = false"
      />
    </div>
  </V2Page>
</template>

<script setup lang="ts">
import { computed, onMounted, provide, ref } from 'vue'
import V2Page from '@/mobile-v2/components/V2Page.vue'
import { MOBILE_PRODUCTS_KEY } from '@/mobile/views/products/productsContext'
import { useMobileProducts } from '@/mobile/views/products/useMobileProducts'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'
import type { EcSku } from '@/api/ecommerce/product'
import type { EcFactoryType } from '@/api/ecommerce/factory'
import V2ProductsSkuCard from '@/mobile-v2/views/products/components/V2ProductsSkuCard.vue'

import '@/mobile-v2/views/ecommerce/styles/v2-ecommerce.scss'

const products = useMobileProducts()
provide(MOBILE_PRODUCTS_KEY, products)

const detail = computed(() => products.detailProduct.value)
const healthEnabledPct = computed(() => {
  const t = products.stats.enabledCount + products.stats.disabledCount
  return t === 0 ? 0 : Math.round((products.stats.enabledCount / t) * 100)
})

const selectedSku = ref<EcSku | null>(null)
const skuSheetVisible = ref(false)
const skuCardVisible = ref(false)

const filteredFactoryOptions = computed(() => {
  const allCount = products.records.value.length
  const productionFactories = products.factories.value.filter(
    (f) => f.factoryType === 'PRODUCTION' as EcFactoryType,
  )
  return [
    { value: 'all' as const, label: '全部', productCount: allCount },
    ...productionFactories.map((f) => {
      const count = products.records.value.filter((p) => p.factoryId === f.id).length
      return { value: f.id, label: f.name, productCount: count }
    }),
  ]
})

function productImageUrl(imageName: string): string {
  return getEcommerceImageUrl(imageName)
}

function productEmoji(name: string): string {
  const m: Record<string, string> = {
    'T恤': '👕', '衬衫': '👔', '外套': '🧥', '裙': '👗', '裤': '👖',
    '鞋': '👟', '帽': '🧢', '包': '👜', '箱': '🧳', '玩具': '🧸',
    '恐龙': '🦕', '笔': '✏️', '杯': '☕', '袋': '🛍️', '收纳': '📦',
    '挂': '🧷', '机壳': '📱', '壳': '📱', '数据线': '🔌', '充电': '🔋',
    '书': '📖', '灯': '💡', '装饰': '🎀', '钥匙': '🔑',
  }
  for (const [kw, emoji] of Object.entries(m)) {
    if (name.includes(kw)) return emoji
  }
  return '📦'
}

function formatCartonSize(sku: EcSku): string {
  const { cartonLengthCm: l, cartonWidthCm: w, cartonHeightCm: h } = sku
  if (l == null && w == null && h == null) return '-'
  return `${l ?? '-'} × ${w ?? '-'} × ${h ?? '-'} cm`
}

function formatWeight(v?: number | null): string {
  if (v == null) return '-'
  return `${Number(v).toFixed(3)} kg`
}

function computeUnitWeight(sku: EcSku): string {
  const gross = sku.cartonGrossWeightKg
  const units = sku.unitsPerCarton
  if (gross == null || units == null || units < 1) return '-'
  return `${(Number(gross) / Number(units)).toFixed(3)} kg`
}

function dimStr(sku: EcSku): string {
  const { productLengthCm: l, productWidthCm: w, productHeightCm: h } = sku
  if (l == null && w == null && h == null) return '-'
  return `${l ?? '-'} × ${w ?? '-'} × ${h ?? '-'} cm`
}

function skuStatusChipClass(status?: string): string {
  if (status === 'ON_SALE') return 'chip--green'
  if (status === 'DRAFT') return 'chip--yellow'
  return 'chip--gray'
}

const detailVisible = ref(false)

async function handleDetail(item: any) {
  await products.loadProductDetail(item.id)
  detailVisible.value = true
}

onMounted(() => {
  products.init()
})
</script>

<style scoped lang="scss">
.v2-ec-product-stats {
  background: #fff;
  border-radius: 12px;
  border: 1px solid var(--wr-border, #e8ecef);
  padding: 16px 20px;
  margin-bottom: 12px;

  &__row {
    display: flex;
    justify-content: space-around;
  }

  &__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
  }

  &__val {
    &__1 {
      color: #9b0000;
      font-size: 24px;
      font-weight: bold;
    }
    &__2 {
      color: #009b00;
      font-size: 24px;
      font-weight: bold;
    }
    &__3 {
      color: #00009b;
      font-size: 24px;
      font-weight: bold;
    }
  }

  &__lbl {
    font-size: 11px;
    color: var(--wr-muted, #999);
    font-weight: 600;
  }

  &__divider {
    height: 1px;
    background: var(--wr-border, #e8ecef);
    margin: 12px 0;
  }

  &__health {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  &__health-label {
    font-size: 12px;
    color: var(--wr-text-secondary, #666);
    white-space: nowrap;
    font-weight: 600;
  }

  &__health-bar {
    flex: 1;
    height: 6px;
    background: var(--wr-border, #e8ecef);
    border-radius: 999px;
    overflow: hidden;
  }

  &__health-fill {
    height: 100%;
    background: linear-gradient(90deg, #16a34a, #22c55e);
    border-radius: 999px;
    transition: width 0.4s ease;
  }

  &__health-pct {
    font-size: 12px;
    font-weight: 700;
    color: #16a34a;
    min-width: 36px;
    text-align: right;
  }
}

.v2-ec-tab__count {
  font-size: 10px;
  opacity: 0.7;
  margin-left: 4px;
}

.v2-ec-product-groups {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.v2-ec-factory-group {
  display: flex;
  flex-direction: column;
  gap: 8px;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 2px;
  }

  &__title {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    color: var(--wr-text, #333);
  }

  &__count {
    font-size: 11px;
    color: var(--wr-muted, #999);
    font-weight: 600;
  }
}

.v2-ec-product-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.v2-ec-product-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--wr-card, #fff);
  border-radius: 12px;
  border: 1px solid var(--wr-border, #e8ecef);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.15s;

  &:active {
    transform: scale(0.98);
  }

  &__img {
    width: 64px;
    height: 64px;
    border-radius: 8px;
    background: #f8f9fa;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: contain;
      display: block;
    }
  }

  &__emoji {
    font-size: 28px;
    line-height: 1;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 14px;
    font-weight: 600;
    color: var(--wr-text, #333);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-bottom: 4px;
  }

  &__skus {
    font-size: 12px;
    color: var(--wr-text-secondary, #666);
    margin-bottom: 6px;
  }

  &__status {
    display: inline-block;
    font-size: 10px;
    font-weight: 600;
    padding: 2px 10px;
    border-radius: 999px;

    &.status--enabled {
      background: #f0fdf4;
      color: var(--ec-stat-green);
    }

    &.status--disabled {
      background: #f3f4f6;
      color: var(--ec-stat-gray);
    }
  }
}

.v2-ec-load-more {
  width: 100%;
  padding: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ec-stat-blue);
  background: var(--wr-card, #fff);
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
  font-family: inherit;

  &:active {
    background: #f8f9fa;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.v2-ec-empty {
  text-align: center;
  padding: 40px 0;
  color: var(--wr-muted, #999);
  font-size: 14px;
}

.v2-ec-detail-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  justify-content: center;

  &--nested {
    z-index: 1001;
  }
}

.v2-ec-detail-sheet {
  width: 100%;
  max-width: 500px;
  max-height: 80vh;
  background: var(--wr-card, #fff);
  border-radius: 24px 24px 0 0;
  padding: 12px 20px 32px;
  overflow-y: auto;
  position: relative;

  &--nested {
    max-height: 70vh;
  }

  &__handle {
    width: 36px;
    height: 4px;
    background: var(--wr-border, #e8ecef);
    border-radius: 999px;
    margin: 0 auto 16px;
  }

  &__loading {
    text-align: center;
    padding: 40px 0;
    color: var(--wr-muted, #999);
    font-size: 14px;
  }

  &__header {
    display: flex;
    gap: 14px;
    margin-bottom: 20px;
  }

  &__header-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__img {
    width: 72px;
    height: 72px;
    border-radius: 10px;
    background: #f8f9fa;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: 16px;
    font-weight: 700;
    color: var(--wr-text, #333);
    margin: 0 0 4px;
  }

  &__factory {
    font-size: 12px;
    color: var(--wr-text-secondary, #666);
    margin: 0 0 6px;
  }

  &__badges {
    display: flex;
    gap: 6px;
  }

  &__badge {
    display: inline-block;
    font-size: 10px;
    font-weight: 600;
    padding: 2px 10px;
    border-radius: 999px;

    &.badge--green {
      background: #f0fdf4;
      color: var(--ec-stat-green);
    }

    &.badge--gray {
      background: #f3f4f6;
      color: var(--ec-stat-gray);
    }

    &.badge--rebate {
      background: #fffbeb;
      color: #d97706;
    }
  }

  &__label-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }

  &__label-count {
    font-size: 12px;
    font-weight: 600;
    color: var(--wr-muted, #999);
  }

  &__label {
    font-size: 12px;
    font-weight: 600;
    color: var(--wr-muted, #999);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  &__no-data {
    text-align: center;
    padding: 24px 0;
    color: var(--wr-muted, #999);
    font-size: 13px;
  }

  &__sku-card {
    padding: 12px 14px;
    background: #fff;
    border-radius: 12px;
    border: 1px solid var(--wr-border, #e8ecef);
    margin-bottom: 10px;
    cursor: pointer;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
    transition: box-shadow 0.15s, transform 0.15s;

    &:last-child {
      margin-bottom: 0;
    }

    &:active {
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
      transform: scale(0.985);
    }
  }

  &__sku-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  &__sku-code {
    font-size: 14px;
    font-weight: 700;
    color: var(--wr-text, #333);
    letter-spacing: 0.3px;
  }

  &__sku-chip {
    font-size: 10px;
    font-weight: 700;
    padding: 3px 10px;
    border-radius: 999px;
    letter-spacing: 0.5px;
    text-transform: uppercase;

    &.chip--green {
      background: #dcfce7;
      color: #16a34a;
    }

    &.chip--yellow {
      background: #fef9c3;
      color: #ca8a04;
    }

    &.chip--gray {
      background: #f3f4f6;
      color: #6b7280;
    }
  }

  &__sku-title {
    font-size: 15px;
    font-weight: 700;
    color: var(--wr-text, #333);
    margin: 0;
  }

  &__sku-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 6px 16px;
  }

  &__sku-field {
    display: flex;
    align-items: baseline;
    gap: 4px;
  }

  &__sku-label {
    font-size: 11px;
    color: var(--wr-muted, #999);
    white-space: nowrap;
  }

  &__sku-val {
    font-size: 13px;
    font-weight: 600;
    color: var(--wr-text, #333);
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    &--price {
      color: #b91c1c;
    }

    &--rebate {
      color: #ea580c;
    }

    &--bold {
      font-weight: 700;
    }
  }

  &__sku-carton-row {
    margin-top: 8px;
    padding: 6px 10px;
    background: #f8fafc;
    border-radius: 8px;
    font-size: 12px;
    color: var(--wr-text-secondary, #666);
    border: 1px dashed #e2e8f0;
  }

  &__sku-top-header {
    display: flex;
    gap: 14px;
    margin-bottom: 16px;
    padding: 12px;
    background: #fff;
    border-radius: 12px;
    border: 1px solid var(--wr-border, #e8ecef);
  }

  &__sku-top-img {
    width: 64px;
    height: 64px;
    border-radius: 10px;
    background: #f8f9fa;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
  }

  &__sku-top-emoji {
    font-size: 28px;
    line-height: 1;
  }

  &__sku-top-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__sku-top-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  &__sku-top-code {
    font-size: 15px;
    font-weight: 700;
    color: var(--wr-text, #333);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__sku-top-actions {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
  }

  &__card-btn {
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    border-radius: 6px;
    background: #dbeafe;
    color: #2563eb;
    cursor: pointer;
    transition: transform 0.15s;

    &:active {
      transform: scale(0.85);
    }
  }

  &__sku-chip {
    font-size: 10px;
    font-weight: 700;
    padding: 2px 8px;
    border-radius: 999px;
    letter-spacing: 0.5px;

    &.chip--green {
      background: #dcfce7;
      color: #16a34a;
    }

    &.chip--yellow {
      background: #fef9c3;
      color: #ca8a04;
    }

    &.chip--gray {
      background: #f3f4f6;
      color: #6b7280;
    }
  }

  &__sku-top-name {
    font-size: 13px;
    color: var(--wr-text-secondary, #666);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__sku-top-prices {
    display: flex;
    gap: 16px;
    align-items: center;
  }

  &__sku-top-price {
    font-size: 16px;
    font-weight: 700;
    color: #b91c1c;
  }

  &__sku-top-rebate {
    font-size: 12px;
    font-weight: 600;
    color: #ea580c;
  }

  &__sku-dim-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    margin-bottom: 8px;
  }

  &__sku-dim-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 8px 4px;
    background: #fff;
    border-radius: 8px;
    border: 1px dashed #e2e8f0;
  }

  &__sku-dim-label {
    font-size: 10px;
    color: var(--wr-muted, #999);
  }

  &__sku-dim-val {
    font-size: 12px;
    font-weight: 700;
    color: var(--wr-text, #333);
  }

  &__sku-weight-row {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 8px;
    background: #fff;
    border-radius: 8px;
    border: 1px dashed #e2e8f0;
  }

  &__sku-weight-label {
    font-size: 10px;
    color: var(--wr-muted, #999);
  }

  &__sku-weight-val {
    font-size: 13px;
    font-weight: 700;
    color: var(--wr-text, #333);
  }

  &__sku-weight-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
    margin-bottom: 8px;
  }

  &__sku-weight-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 8px 4px;
    background: #fff;
    border-radius: 8px;
    border: 1px dashed #e2e8f0;
  }

  &__sku-carton-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
    margin-bottom: 8px;
  }

  &__sku-carton-cell {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 8px 10px;
    background: #fff;
    border-radius: 8px;
    border: 1px dashed #e2e8f0;
    min-width: 0;
  }

  &__sku-carton-key {
    font-size: 10px;
    color: var(--wr-muted, #999);
  }

  &__sku-carton-val {
    font-size: 13px;
    font-weight: 700;
    color: var(--wr-text, #333);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__sku-carton-name {
    margin-top: 8px;
    padding: 8px 10px;
    background: #fff;
    border-radius: 8px;
    border: 1px dashed #e2e8f0;
    font-size: 12px;
    color: var(--wr-text-secondary, #666);
  }
}

.v2-ec-slide-enter-active,
.v2-ec-slide-leave-active {
  transition: opacity 0.2s ease;

  .v2-ec-detail-sheet {
    transition: transform 0.35s cubic-bezier(0.32, 0.72, 0, 1);
  }
}

.v2-ec-slide-enter-from,
.v2-ec-slide-leave-to {
  opacity: 0;

  .v2-ec-detail-sheet {
    transform: translateY(100%);
  }
}
.v2-ec-detail-sheet__sku-section {
  margin-top: 4px;
}

.v2-ec-detail-sheet__sku-section-label {
  font-size: 12px;
  margin-bottom: 4px;
}
</style>
