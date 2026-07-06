<template>
  <div class="mpr-grid-section">
    <!-- 空状态 -->
    <SchemeADoodleFrame
      v-if="products.groupedByFactory.value.length === 0 && !products.loading.value"
      color="#cbd5e1" :shadow="false" class="mpr-empty-card"
    >
      <div class="mpr-empty">
        <span class="mpr-empty__icon">📭</span>
        <span class="mpr-empty__text">暂无商品数据</span>
      </div>
    </SchemeADoodleFrame>

    <!-- 按工厂分组 -->
    <div
      v-for="group in products.groupedByFactory.value"
      :key="group.factoryId ?? 'unknown'"
      class="mpr-factory-group"
    >
      <div class="mpr-factory-group__header">
        <div class="mpr-factory-group__head-left">
          <h3 class="mpr-factory-group__title">🏭 {{ group.factoryName }}</h3>
        </div>
        <span class="mpr-factory-group__count">{{ group.products.length }} SPU</span>
      </div>

      <div class="mpr-product-row">
        <SchemeADoodleFrame
          v-for="prod in group.products"
          :key="prod.id"
          class="mpr-product-card"
          :color="cardColor(prod.status)"
          :seed="prod.id"
          sketch
          :stroke-width="2.5"
          :shadow="false"
          tag="button"
          type="button"
          @click="products.loadProductDetail(prod.id)"
        >
          <div class="mpr-product-card__inner">
            <div class="mpr-product-card__img">
              <img
                v-if="prod.imageName"
                :src="productImageUrl(prod.imageName)"
                :alt="prod.name"
                class="mpr-product-card__image"
              />
              <span v-else class="mpr-product-card__emoji">{{ productEmoji(prod.name) }}</span>
            </div>
            <div class="mpr-product-card__name">{{ prod.name }}</div>
            <div class="mpr-product-card__meta">
              <span class="mpr-product-card__skus">{{ prod.skuCount }} SKU</span>
              <span v-if="prod.rebatePct" class="mpr-product-card__rebate">退点 {{ prod.rebatePct }}%</span>
            </div>
            <MobileDoodleChip
              shape="pill"
              :color="prod.status === 'ENABLED' ? '#22c55e' : '#94a3b8'"
              :seed="prod.id + 100"
              class="mpr-product-card__status"
            >
              {{ prod.status }}
            </MobileDoodleChip>
          </div>
        </SchemeADoodleFrame>
      </div>
    </div>

    <!-- 加载更多 -->
    <SchemeADoodleFrame
      v-if="products.hasMore.value"
      tag="button" type="button"
      class="mpr-load-more"
      color="#cbd5e1"
      sketch
      :stroke-width="2.5"
      :shadow="false"
      :disabled="products.loading.value"
      @click="products.loadMore()"
    >
      <span class="mpr-load-more__text">
        <img class="mpr-load-more__icon" :src="schemeAAssets.squiggleBlue" alt="" />
        {{ products.loading.value ? '加载中...' : '加载更多' }}
      </span>
    </SchemeADoodleFrame>
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_PRODUCTS_KEY } from '../productsContext'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'

const products = inject(MOBILE_PRODUCTS_KEY)!

function productImageUrl(imageName: string): string {
  return getEcommerceImageUrl(imageName)
}

function cardColor(status: string): string {
  return status === 'ENABLED' ? '#c2410c' : '#94a3b8'
}

function productEmoji(name: string): string {
  const m: Record<string, string> = {
    'T恤': '👕', '衬衫': '👔', '外套': '🧥', '裙': '👗', '裤': '👖',
    '鞋': '👟', '跑鞋': '👟', '运动鞋': '👟',
    '包': '👜', '背包': '🎒', '箱': '🧳', '帽': '🧢', '袜': '🧦',
    '电子': '📱', '耳机': '🎧', '充电': '🔋',
  }
  for (const [k, v] of Object.entries(m)) { if (name.includes(k)) return v }
  return '📦'
}
</script>

<style scoped lang="scss">
.mpr-grid-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ===== 空状态 ===== */
.mpr-empty-card {
  :deep(.sa-doodle-frame__body) {
    padding: 24px 16px;
  }
}

.mpr-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.mpr-empty__icon {
  font-size: 40px;
}

.mpr-empty__text {
  font-size: 14px;
  color: #94a3b8;
}

/* ===== 工厂分组 ===== */
.mpr-factory-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mpr-factory-group__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 0 2px;
}

.mpr-factory-group__head-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.mpr-factory-group__icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.mpr-factory-group__title {
  margin: 0;
  font-size: 16px;
  color: #1e293b;
  font-family: 'ZCOOL KuaiLe', 'Microsoft YaHei', sans-serif;
}

.mpr-factory-group__count {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 700;
}

/* ===== 商品卡片行 ===== */
.mpr-product-row {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 4px 0 8px;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.mpr-product-card {
  flex-shrink: 0;
  width: 150px;
  cursor: pointer;
  background: #faf8f5;
  transition: transform 0.15s ease;

  &:active {
    transform: scale(0.95);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.mpr-product-card__inner {
  padding: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.mpr-product-card__img {
  width: 100%;
  height: 80px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 8px;
  overflow: hidden;
}

.mpr-product-card__image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.mpr-product-card__emoji {
  font-size: 32px;
  line-height: 1;
}

.mpr-product-card__name {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mpr-product-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.mpr-product-card__skus {
  font-size: 10px;
  color: #64748b;
}

.mpr-product-card__rebate {
  font-size: 10px;
  color: #f59e0b;
}

.mpr-product-card__status {
  display: inline-flex;
  font-size: 9px;
  font-weight: 700;
  background: #faf8f5;

  :deep(.sa-doodle-frame__body) {
    padding: 8px;
  }
}

/* ===== 加载更多 ===== */
.mpr-load-more {
  display: block;
  width: 100%;
  font-family: inherit;
  font-size: 14px;
  font-weight: 700;
  color: #8b5cf6;
  cursor: pointer;
  background: #faf8f5;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.mpr-load-more__text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  text-align: center;
}

.mpr-load-more__icon {
  width: 20px;
  height: 12px;
  opacity: 0.5;
}
</style>
