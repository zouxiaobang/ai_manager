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
          :placeholder="products.t('mobile.searchPlaceholder')"
          type="search"
        />
      </div>

      <div v-if="products.stats" class="v2-ec-stats v2-ec-stats--triple">
        <div class="v2-ec-stat-card" style="background: #eef2ff;">
          <div class="v2-ec-stat-card__icon" style="background: #4f46e5;">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="3" x2="9" y2="21"/>
            </svg>
          </div>
          <div class="v2-ec-stat-card__info">
            <div class="v2-ec-stat-card__value" style="color: #4f46e5;">{{ total }}</div>
            <div class="v2-ec-stat-card__label">SPU 总数</div>
          </div>
        </div>
        <div class="v2-ec-stat-card" style="background: #f0fdf4;">
          <div class="v2-ec-stat-card__icon" style="background: #16a34a;">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          </div>
          <div class="v2-ec-stat-card__info">
            <div class="v2-ec-stat-card__value" style="color: #16a34a;">{{ products.stats.enabledCount }}</div>
            <div class="v2-ec-stat-card__label">已启用</div>
          </div>
        </div>
        <div class="v2-ec-stat-card" style="background: #f8fafc;">
          <div class="v2-ec-stat-card__icon" style="background: #6b7280;">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </div>
          <div class="v2-ec-stat-card__info">
            <div class="v2-ec-stat-card__value" style="color: #6b7280;">{{ products.stats.disabledCount }}</div>
            <div class="v2-ec-stat-card__label">已禁用</div>
          </div>
        </div>
      </div>

      <div class="v2-ec-tabs">
        <button
          v-for="f in products.factories.value"
          :key="f.id"
          class="v2-ec-tab"
          :class="{ 'is-active': products.selectedFactoryId.value === f.id }"
          @click="products.selectedFactoryId.value = f.id"
        >
          {{ f.name }}
          <span class="v2-ec-tab__count">{{ f.productCount ?? 0 }}</span>
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
                <div class="v2-ec-product-card__skus">SKU: {{ item.category?.name || '-' }}</div>
                <span
                  class="v2-ec-product-card__status"
                  :class="item.enabled ? 'status--enabled' : 'status--disabled'"
                >
                  {{ item.enabled ? '已启用' : '已禁用' }}
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
        <Transition name="v2-ec-fade">
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
                    <div class="v2-ec-detail-sheet__meta">
                      <span>{{ detail.category?.name || '未分类' }}</span>
                      <span>{{ detail.skus?.length || 0 }} 个SKU</span>
                    </div>
                    <span
                      class="v2-ec-detail-sheet__status"
                      :class="detail.enabled ? 'status--on' : 'status--off'"
                    >
                      {{ detail.enabled ? '已启用' : '已禁用' }}
                    </span>
                  </div>
                </div>
                <div class="v2-ec-detail-sheet__label">SKU 列表</div>
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
                      class="v2-ec-detail-sheet__sku-status"
                      :class="sku.status === 'ON_SALE' ? 'status--on-sale' : 'status--off-sale'"
                    >
                      {{ sku.status === 'ON_SALE' ? '在售' : '下架' }}
                    </span>
                  </div>
                  <div class="v2-ec-detail-sheet__sku-grid">
                    <div class="v2-ec-detail-sheet__sku-item">
                      <span class="v2-ec-detail-sheet__sku-label">规格</span>
                      <span class="v2-ec-detail-sheet__sku-val">{{ sku.specName || '-' }}</span>
                    </div>
                    <div class="v2-ec-detail-sheet__sku-item">
                      <span class="v2-ec-detail-sheet__sku-label">售价</span>
                      <span class="v2-ec-detail-sheet__sku-val v2-ec-detail-sheet__sku-val--price">¥{{ sku.salePrice ?? '-' }}</span>
                    </div>
                    <div class="v2-ec-detail-sheet__sku-item">
                      <span class="v2-ec-detail-sheet__sku-label">退点</span>
                      <span class="v2-ec-detail-sheet__sku-val v2-ec-detail-sheet__sku-val--rebate">{{ sku.rebatePct ?? '-' }}%</span>
                    </div>
                    <div class="v2-ec-detail-sheet__sku-item">
                      <span class="v2-ec-detail-sheet__sku-label">尺寸</span>
                      <span class="v2-ec-detail-sheet__sku-val">{{ dimStr(sku) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>

      <Teleport to="body">
        <Transition name="v2-ec-fade">
          <div v-if="skuSheetVisible" class="v2-ec-detail-overlay v2-ec-detail-overlay--nested" @click.self="skuSheetVisible = false">
            <div class="v2-ec-detail-sheet v2-ec-detail-sheet--nested">
              <div class="v2-ec-detail-sheet__handle" />
              <div class="v2-ec-detail-sheet__body">
                <div class="v2-ec-detail-sheet__header">
                  <div class="v2-ec-detail-sheet__header-row">
                    <h4 class="v2-ec-detail-sheet__sku-title">{{ selectedSku?.skuCode }}</h4>
                    <span
                      v-if="selectedSku"
                      class="v2-ec-detail-sheet__sku-status"
                      :class="selectedSku.status === 'ON_SALE' ? 'status--on-sale' : 'status--off-sale'"
                    >
                      {{ selectedSku.status === 'ON_SALE' ? '在售' : '下架' }}
                    </span>
                  </div>
                </div>
                <div class="v2-ec-detail-sheet__sku-grid">
                  <div class="v2-ec-detail-sheet__sku-item">
                    <span class="v2-ec-detail-sheet__sku-label">规格</span>
                    <span class="v2-ec-detail-sheet__sku-val">{{ selectedSku?.specName || '-' }}</span>
                  </div>
                  <div class="v2-ec-detail-sheet__sku-item">
                    <span class="v2-ec-detail-sheet__sku-label">售价</span>
                    <span class="v2-ec-detail-sheet__sku-val v2-ec-detail-sheet__sku-val--price">¥{{ selectedSku?.salePrice ?? '-' }}</span>
                  </div>
                  <div class="v2-ec-detail-sheet__sku-item">
                    <span class="v2-ec-detail-sheet__sku-label">退点</span>
                    <span class="v2-ec-detail-sheet__sku-val v2-ec-detail-sheet__sku-val--rebate">{{ selectedSku?.rebatePct ?? '-' }}%</span>
                  </div>
                  <div class="v2-ec-detail-sheet__sku-item">
                    <span class="v2-ec-detail-sheet__sku-label">尺寸</span>
                    <span class="v2-ec-detail-sheet__sku-val">{{ dimStr(selectedSku) }}</span>
                  </div>
                </div>
                <div v-if="selectedSku?.cartonName" class="v2-ec-detail-sheet__sku-carton">
                  {{ selectedSku.cartonName }} · {{ selectedSku.unitsPerCarton }}件/箱
                </div>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>
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

import '@/mobile-v2/views/ecommerce/styles/v2-ecommerce.scss'

const products = useMobileProducts()
provide(MOBILE_PRODUCTS_KEY, products)

const detail = computed(() => products.detailProduct.value)
const total = computed(() => products.stats.enabledCount + products.stats.disabledCount)

const selectedSku = ref<EcSku | null>(null)
const skuSheetVisible = ref(false)

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

function dimStr(sku: EcSku | null): string {
  if (!sku) return '-'
  const parts: string[] = []
  if (sku.productLengthCm != null) parts.push(`${sku.productLengthCm}cm`)
  if (sku.productWidthCm != null) parts.push(`${sku.productWidthCm}cm`)
  if (sku.productHeightCm != null) parts.push(`${sku.productHeightCm}cm`)
  return parts.join(' × ') || '-'
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
.v2-ec-stats--triple {
  grid-template-columns: repeat(3, 1fr);
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

  &__meta {
    font-size: 12px;
    color: var(--wr-text-secondary, #666);
    display: flex;
    gap: 12px;
    margin-bottom: 6px;
  }

  &__status {
    display: inline-block;
    font-size: 10px;
    font-weight: 600;
    padding: 2px 10px;
    border-radius: 999px;

    &.status--on {
      background: #f0fdf4;
      color: var(--ec-stat-green);
    }

    &.status--off {
      background: #f3f4f6;
      color: var(--ec-stat-gray);
    }
  }

  &__label {
    font-size: 12px;
    font-weight: 600;
    color: var(--wr-muted, #999);
    margin-bottom: 10px;
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
    padding: 10px 12px;
    background: #f8fafc;
    border-radius: 10px;
    border: 1px solid var(--wr-border, #e8ecef);
    margin-bottom: 8px;
    cursor: pointer;

    &:last-child {
      margin-bottom: 0;
    }

    &:active {
      background: #f1f5f9;
    }
  }

  &__sku-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 6px;
  }

  &__sku-code {
    font-size: 13px;
    font-weight: 600;
    color: var(--wr-text, #333);
  }

  &__sku-title {
    font-size: 15px;
    font-weight: 700;
    color: var(--wr-text, #333);
    margin: 0;
  }

  &__sku-status {
    font-size: 10px;
    font-weight: 600;
    padding: 2px 10px;
    border-radius: 999px;

    &.status--on-sale {
      background: #f0fdf4;
      color: var(--ec-stat-green);
    }

    &.status--off-sale {
      background: #f3f4f6;
      color: var(--ec-stat-gray);
    }
  }

  &__sku-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 6px;
  }

  &__sku-item {
    display: flex;
    flex-direction: column;
    gap: 1px;
  }

  &__sku-label {
    font-size: 10px;
    color: var(--wr-muted, #999);
  }

  &__sku-val {
    font-size: 13px;
    font-weight: 600;
    color: var(--wr-text, #333);

    &--price {
      color: var(--ec-stat-red);
    }

    &--rebate {
      color: var(--ec-stat-orange);
    }
  }

  &__sku-carton {
    margin-top: 10px;
    padding: 8px 10px;
    background: #f8fafc;
    border-radius: 8px;
    font-size: 12px;
    color: var(--wr-text-secondary, #666);
  }
}

.v2-ec-fade-enter-active,
.v2-ec-fade-leave-active {
  transition: opacity 0.25s ease;
}
.v2-ec-fade-enter-from,
.v2-ec-fade-leave-to {
  opacity: 0;
}
</style>
