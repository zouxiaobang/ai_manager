<template>
  <Teleport to="body">
    <Transition name="mpr-detail">
      <div v-if="products.detailVisible.value" class="mpr-detail-overlay" @click.self="products.closeDetail()">
        <div class="mpr-detail-sheet">
          <div class="mpr-detail-sheet__handle" />

          <!-- 装饰 -->
          <img class="mpr-detail-sheet__deco mpr-detail-sheet__deco--star" :src="schemeAAssets.starYellow" alt="" />
          <img class="mpr-detail-sheet__deco mpr-detail-sheet__deco--squiggle" :src="schemeAAssets.squiggleBlue" alt="" />

          <div v-if="products.detailLoading.value" class="mpr-detail-sheet__loading">
            <span>加载中...</span>
          </div>

          <template v-if="detail">
            <!-- SPU 头部 -->
            <div class="mpr-detail-sheet__header">
              <SchemeADoodleFrame
                shape="rect"
                color="#f59e0b"
                :seed="42"
                sketch
                :stroke-width="2.5"
                :shadow="false"
                tag="div"
                class="mpr-detail-sheet__header-img-frame"
              >
                <div class="mpr-detail-sheet__header-img">
                  <img
                    v-if="detail.imageName"
                    :src="productImageUrl(detail.imageName)"
                    :alt="detail.name"
                    class="mpr-detail-sheet__header-real-img"
                  />
                  <span v-else class="mpr-detail-sheet__header-emoji">{{ productEmoji(detail.name) }}</span>
                </div>
              </SchemeADoodleFrame>
              <div class="mpr-detail-sheet__header-info">
                <h3 class="mpr-detail-sheet__title">{{ detail.name }}</h3>
                <p class="mpr-detail-sheet__factory" v-if="detail.factoryName">🏭 {{ detail.factoryName }}</p>
                <div class="mpr-detail-sheet__badges">
                  <MobileDoodleChip
                    shape="pill"
                    :color="detail.status === 'ENABLED' ? '#009B00' : '#94a3b8'"
                    :seed="42"
                  >
                    <div style="padding: 12px; text-align: center">{{ detail.status ? "可售" : "禁售" }}</div>
                  </MobileDoodleChip>
                  <MobileDoodleChip
                    v-if="detail.rebatePct"
                    shape="pill"
                    color="#f59e0b"
                    :seed="43"
                  >
                    <div style="padding: 8px">退点 <span style="color: #9b0000; font-size: 14px">{{ detail.rebatePct }}</span> %</div>

                  </MobileDoodleChip>
                </div>
              </div>
            </div>

            <!-- SKU 列表 -->
            <div class="mpr-detail-sheet__section">
              <MobileSectionHeader
                title="SKU 列表"
                :count="detail.skus?.length ?? 0"
                count-unit="项"
              />

              <div v-if="!detail.skus?.length" class="mpr-detail-sheet__no-skus">
                暂无 SKU 数据
              </div>

              <SchemeADoodleFrame
                v-for="sku in detail.skus"
                :key="sku.id"
                tag="button"
                type="button"
                class="mpr-detail-sheet__sku-card"
                :color="skuCardColor(sku.status)"
                sketch
                :stroke-width="2"
                :seed="sku.id"
                :shadow="false"
                @click="openSkuDetail(sku)"
              >
                <div class="mpr-detail-sheet__sku-inner">
                  <div class="mpr-detail-sheet__sku-top">
                    <span class="mpr-detail-sheet__sku-code">{{ sku.skuCode }}</span>
                    <MobileDoodleChip
                      shape="pill"
                      :color="skuStatusColor(sku.status)"
                      :seed="(sku.id ?? 0) + 200"
                      class="mpr-detail-sheet__sku-status-chip"
                    >
                      {{ sku.status }}
                    </MobileDoodleChip>
                  </div>

                  <div class="mpr-detail-sheet__sku-grid">
                    <div class="mpr-detail-sheet__sku-item">
                      <span class="mpr-detail-sheet__sku-label">规格</span>
                      <span class="mpr-detail-sheet__sku-val">{{ sku.specName || '-' }}</span>
                    </div>
                    <div class="mpr-detail-sheet__sku-item">
                      <span class="mpr-detail-sheet__sku-label">售价</span>
                      <span class="mpr-detail-sheet__sku-val mpr-detail-sheet__sku-val--price">
                        ¥{{ sku.salePrice ?? '-' }}
                      </span>
                    </div>
                    <div class="mpr-detail-sheet__sku-item">
                      <span class="mpr-detail-sheet__sku-label">退点</span>
                      <span class="mpr-detail-sheet__sku-val mpr-detail-sheet__sku-val--rebate">
                        {{ sku.rebatePct ?? '-' }}%
                      </span>
                    </div>
                    <div class="mpr-detail-sheet__sku-item">
                      <span class="mpr-detail-sheet__sku-label">尺寸</span>
                      <span class="mpr-detail-sheet__sku-val">{{ dimStr(sku) }}</span>
                    </div>
                  </div>

                  <div class="mpr-detail-sheet__sku-carton" v-if="sku.cartonName">
                    📦 {{ sku.cartonName }} · {{ sku.unitsPerCarton }}件/箱
                  </div>
                </div>
              </SchemeADoodleFrame>
            </div>
          </template>
        </div>

        <!-- SKU 详情弹窗 -->
        <ProductsSkuSheet
          :visible="skuSheetVisible"
          :sku="selectedSku"
          @update:visible="skuSheetVisible = $event"
        />
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { MOBILE_PRODUCTS_KEY } from '../productsContext'
import type { EcSku } from '@/api/ecommerce/product'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import MobileSectionHeader from '@/mobile/components/MobileSectionHeader.vue'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'
import ProductsSkuSheet from './ProductsSkuSheet.vue'

const products = inject(MOBILE_PRODUCTS_KEY)!
const detail = computed(() => products.detailProduct.value)

const selectedSku = ref<EcSku | null>(null)
const skuSheetVisible = ref(false)

function openSkuDetail(sku: EcSku) {
  selectedSku.value = sku
  skuSheetVisible.value = true
}

function productImageUrl(imageName: string): string {
  return getEcommerceImageUrl(imageName)
}

function productEmoji(name: string): string {
  const m: Record<string, string> = { 'T恤': '👕', '衬衫': '👔', '外套': '🧥', '裙': '👗', '裤': '👖', '鞋': '👟', '包': '👜', '背包': '🎒', '帽': '🧢' }
  for (const [k, v] of Object.entries(m)) { if (name.includes(k)) return v }
  return '📦'
}

function skuCardColor(status: string): string {
  if (status === 'ON_SALE') return '#009b00'
  if (status === 'OFF_SALE') return '#94a3b8'
  return '#f59e0b'
}

function skuStatusColor(status: string): string {
  if (status === 'ON_SALE') return '#f59e0b'
  if (status === 'OFF_SALE') return '#94a3b8'
  return '#f59e0b'
}

function dimStr(sku: EcSku) {
  const parts = [sku.productLengthCm, sku.productWidthCm, sku.productHeightCm]
  if (parts.every(p => p != null)) return `${parts.join('×')} cm`
  return '-'
}
</script>

<style scoped lang="scss">
.mpr-detail-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  font-family: 'ZCOOL KuaiLe', 'Microsoft YaHei', sans-serif;
}

.mpr-detail-sheet {
  width: 100%;
  max-width: 500px;
  max-height: 80vh;
  background: #faf8f5;
  border-radius: 24px 24px 0 0;
  padding: 12px 20px 32px;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 8px;
    background: #faf8f5;
    border-radius: 24px 24px 0 0;
    z-index: 1;
  }
}

/* ===== 装饰元素 ===== */
.mpr-detail-sheet__deco {
  position: absolute;
  pointer-events: none;
  z-index: 2;

  &--star {
    top: 24px;
    right: 20px;
    width: 28px;
    animation: twinkle 2.5s ease-in-out infinite;
  }

  &--squiggle {
    bottom: 16px;
    left: 12px;
    width: 36px;
    opacity: 0.4;
  }
}

@keyframes twinkle {
  0%, 100% { opacity: 0.5; transform: scale(1) rotate(0deg); }
  50% { opacity: 1; transform: scale(1.15) rotate(5deg); }
}

.mpr-detail-sheet__handle {
  width: 36px;
  height: 4px;
  background: #cbd5e1;
  border-radius: 999px;
  margin: 0 auto 16px;
  position: relative;
  z-index: 2;
}

.mpr-detail-sheet__loading {
  text-align: center;
  padding: 40px 0;
  color: #94a3b8;
  font-family: 'ZCOOL KuaiLe', 'Microsoft YaHei', sans-serif;
}

.mpr-detail-sheet__header {
  display: flex;
  gap: 14px;
  margin-bottom: 20px;
  position: relative;
  z-index: 3;
}

.mpr-detail-sheet__header-img-frame {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  background: #faf8f5;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 72px;
    height: 72px;
  }
}

.mpr-detail-sheet__header-img {
  width: 100%;
  height: 100%;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 8px;
}

.mpr-detail-sheet__header-real-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.mpr-detail-sheet__header-emoji {
  font-size: 30px;
  line-height: 1;
}

.mpr-detail-sheet__header-info {
  flex: 1;
  min-width: 0;
}

.mpr-detail-sheet__title {
  font-size: 18px;
  color: #1e293b;
  font-family: 'ZCOOL KuaiLe', 'Microsoft YaHei', sans-serif;
  margin-bottom: 2px;
}

.mpr-detail-sheet__factory {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
  font-family: 'ZCOOL KuaiLe', 'Microsoft YaHei', sans-serif;
}

.mpr-detail-sheet__badges {
  display: flex;
  gap: 6px;

  .mobile-doodle-chip {
    font-size: 10px;
    font-weight: 700;
    background: #faf8f5;

    :deep(.sa-doodle-frame__body) {
      padding: 2px 10px;
    }
  }
}

.mpr-detail-sheet__section {
  position: relative;
  z-index: 3;

  :deep(.mobile-section-header__count) {
    color: #8b0000;
    font-weight: 700;
    font-size: 18px;
  }
}

.mpr-detail-sheet__no-skus {
  text-align: center;
  padding: 20px;
  color: #94a3b8;
  font-family: 'ZCOOL KuaiLe', 'Microsoft YaHei', sans-serif;
}

.mpr-detail-sheet__sku-card {
  margin-bottom: 10px;
  background: #faf8f5;
  cursor: pointer;
  transition: transform 0.15s ease;

  &:active {
    transform: scale(0.97);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.mpr-detail-sheet__sku-inner {
  padding: 24px;
}

.mpr-detail-sheet__sku-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1.5px dashed #e2e8f0;
}

.mpr-detail-sheet__sku-code {
  font-size: 16px;
  font-weight: bold;
  color: #00009B;
  font-family: monospace;
  margin-left: 8px;
}

.mpr-detail-sheet__sku-status-chip {
  font-size: 10px;
  font-weight: 700;
  background: #faf8f5;

  :deep(.sa-doodle-frame__body) {
    padding: 8px;
  }
}

.mpr-detail-sheet__sku-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.mpr-detail-sheet__sku-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mpr-detail-sheet__sku-label {
  font-size: 12px;
  color: #94a3b8;
}

.mpr-detail-sheet__sku-val {
  font-size: 14px;
  color: #1e293b;
  font-weight: 600;
  margin-top: 4px;

  &--price { color: #ef4444; font-size: 16px }
  &--rebate { color: #f59e0b; }
}

.mpr-detail-sheet__sku-carton {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1.5px dashed #e2e8f0;
  font-size: 12px;
  color: #64748b;
  font-family: 'ZCOOL KuaiLe', 'Microsoft YaHei', sans-serif;
}

// Transition
.mpr-detail-enter-active,
.mpr-detail-leave-active {
  transition: all 0.3s ease;

  .mpr-detail-sheet {
    transition: transform 0.3s ease;
  }
}

.mpr-detail-enter-from,
.mpr-detail-leave-to {
  opacity: 0;

  .mpr-detail-sheet {
    transform: translateY(100%);
  }
}
</style>
