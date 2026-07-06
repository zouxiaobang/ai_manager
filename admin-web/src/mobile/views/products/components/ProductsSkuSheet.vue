<template>
  <Teleport to="body">
    <Transition name="mpr-sku-sheet">
      <div v-if="visible" class="mpr-sku-sheet-overlay" @click.self="close">
        <div class="mpr-sku-sheet">
          <!-- 拖拽手柄 -->
          <div class="mpr-sku-sheet__handle" />

          <!-- 装饰 -->
          <img class="mpr-sku-sheet__deco mpr-sku-sheet__deco--star" :src="schemeAAssets.starYellow" alt="" />

          <!-- Header -->
          <div class="mpr-sku-sheet__header">
            <div class="mpr-sku-sheet__header-left">
              <img class="mpr-sku-sheet__header-icon" :src="schemeAAssets.starBlueOutline" alt="" />
              <h3 class="mpr-sku-sheet__title">SKU 详情</h3>
            </div>
            <div class="mpr-sku-sheet__header-actions">
              <button
                type="button"
                class="mpr-sku-sheet__card-btn"
                :title="'生成 SKU 名片'"
                @click="openCard"
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- Body -->
          <div class="mpr-sku-sheet__body">
            <!-- SKU 基本信息 -->
            <SchemeADoodleFrame
              class="mpr-sku-sheet__section-card"
              color="#2563eb" sketch :stroke-width="2" :shadow="false"
            >
              <div class="mpr-sku-sheet__section-card-inner">
                <div class="mpr-sku-sheet__sku-header">
                  <div class="mpr-sku-sheet__sku-img-wrap">
                    <img
                      v-if="sku.imageName"
                      :src="skuImageUrl"
                      alt=""
                      class="mpr-sku-sheet__sku-img"
                      crossorigin="anonymous"
                    />
                    <span v-else class="mpr-sku-sheet__sku-img-placeholder">📷</span>
                  </div>
                  <div class="mpr-sku-sheet__sku-info">
                    <div class="mpr-sku-sheet__sku-code">{{ sku.skuCode }}</div>
                    <div v-if="sku.specName" class="mpr-sku-sheet__sku-spec">{{ sku.specName }}</div>
                    <div class="mpr-sku-sheet__sku-price">¥{{ sku.salePrice ?? '-' }}</div>
                  </div>
                </div>

                <div class="mpr-sku-sheet__sku-meta">
                  <div class="mpr-sku-sheet__sku-meta-item">
                    <MobileDoodleChip
                      shape="pill"
                      :color="statusColor"
                      :seed="(sku.id ?? 0)"
                      class="mpr-sku-sheet__meta-chip"
                    >
                      {{ sku.status }}
                    </MobileDoodleChip>
                  </div>
                  <div class="mpr-sku-sheet__sku-meta-item">
                    <span class="mpr-sku-sheet__meta-label">退点</span>
                    <span class="mpr-sku-sheet__meta-val mpr-sku-sheet__meta-val--rebate">{{ sku.rebatePct ?? '-' }}</span>%
                  </div>
                </div>
              </div>
            </SchemeADoodleFrame>

            <!-- 单品尺寸 -->
            <SchemeADoodleFrame
              class="mpr-sku-sheet__section-card"
              color="#22c55e" sketch :stroke-width="2" :shadow="false"
            >
              <div class="mpr-sku-sheet__section-card-inner">
                <div class="mpr-sku-sheet__section-title-row">
                  <img class="mpr-sku-sheet__section-icon" :src="schemeAAssets.starBlue" alt="" />
                  <span class="mpr-sku-sheet__section-title">单品尺寸</span>
                </div>
                <div class="mpr-sku-sheet__dim-grid">
                  <div class="mpr-sku-sheet__dim-item">
                    <span class="mpr-sku-sheet__dim-label">长(L)</span>
                    <span class="mpr-sku-sheet__dim-val">{{ sku.productLengthCm ?? '-' }} cm</span>
                  </div>
                  <div class="mpr-sku-sheet__dim-item">
                    <span class="mpr-sku-sheet__dim-label">宽(W)</span>
                    <span class="mpr-sku-sheet__dim-val">{{ sku.productWidthCm ?? '-' }} cm</span>
                  </div>
                  <div class="mpr-sku-sheet__dim-item">
                    <span class="mpr-sku-sheet__dim-label">高(H)</span>
                    <span class="mpr-sku-sheet__dim-val">{{ sku.productHeightCm ?? '-' }} cm</span>
                  </div>
                </div>
                <div class="mpr-sku-sheet__single_weight-grid">
                  <div class="mpr-sku-sheet__weight-item">
                    <span class="mpr-sku-sheet__weight-label">单品重</span>
                    <span class="mpr-sku-sheet__weight-val" style="color: #9b0000; font-size: 16px">{{ unitWeightText }}</span>
                  </div>
                </div>
              </div>
            </SchemeADoodleFrame>

            <!-- 外箱信息 -->
            <SchemeADoodleFrame
              class="mpr-sku-sheet__section-card"
              color="#f59e0b" sketch :stroke-width="2" :shadow="false"
            >
              <div class="mpr-sku-sheet__section-card-inner">
                <div class="mpr-sku-sheet__section-title-row">
                  <img class="mpr-sku-sheet__section-icon" :src="schemeAAssets.squiggleBlue" alt="" />
                  <span class="mpr-sku-sheet__section-title">外箱信息</span>
                </div>

                <div class="mpr-sku-sheet__outer_dim-grid">
                  <div class="mpr-sku-sheet__dim-item">
                    <span class="mpr-sku-sheet__dim-label">外箱尺寸</span>
                    <span class="mpr-sku-sheet__dim-val">{{ cartonSizeStr }}</span>
                  </div>
                  <div class="mpr-sku-sheet__dim-item">
                    <span class="mpr-sku-sheet__dim-label">箱装数</span>
                    <span class="mpr-sku-sheet__dim-val">{{ sku.unitsPerCarton ?? '-' }} 件/箱</span>
                  </div>
                </div>

                <div class="mpr-sku-sheet__weight-grid">
                  <div class="mpr-sku-sheet__weight-item">
                    <span class="mpr-sku-sheet__weight-label">毛重</span>
                    <span class="mpr-sku-sheet__weight-val">{{ formatWeight(sku.cartonGrossWeightKg) }}</span>
                  </div>
                  <div class="mpr-sku-sheet__weight-item">
                    <span class="mpr-sku-sheet__weight-label">净重</span>
                    <span class="mpr-sku-sheet__weight-val">{{ formatWeight(sku.cartonNetWeightKg) }}</span>
                  </div>

                </div>

                <div v-if="sku.cartonName" class="mpr-sku-sheet__carton-name">
                  📦 {{ sku.cartonName }}
                </div>
              </div>
            </SchemeADoodleFrame>
          </div>
        </div>

        <!-- SKU 名片（全屏） -->
        <ProductsSkuCard
          :visible="cardVisible"
          :sku="sku"
          @close="cardVisible = false"
        />
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { EcSku } from '@/api/ecommerce/product'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import ProductsSkuCard from './ProductsSkuCard.vue'

const props = withDefaults(
  defineProps<{
    visible: boolean
    sku: EcSku | null
  }>(),
  { visible: false, sku: null },
)

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const cardVisible = ref(false)

const skuImageUrl = computed(() => getEcommerceImageUrl(props.sku?.imageName))

const statusColor = computed(() => {
  if (!props.sku) return '#94a3b8'
  if (props.sku.status === 'ON_SALE') return '#009B00'
  if (props.sku.status === 'OFF_SALE') return '#94a3b8'
  return '#f59e0b'
})

const cartonSizeStr = computed(() => {
  if (!props.sku) return '-'
  const { cartonLengthCm: l, cartonWidthCm: w, cartonHeightCm: h } = props.sku
  if (l == null && w == null && h == null) return '-'
  return `${l ?? '-'} × ${w ?? '-'} × ${h ?? '-'} cm`
})

const unitWeightText = computed(() => {
  if (!props.sku) return '-'
  const gross = props.sku.cartonGrossWeightKg
  const units = props.sku.unitsPerCarton
  if (gross == null || units == null || units < 1) return '-'
  return `${(Number(gross) / Number(units)).toFixed(3)} kg`
})

function formatWeight(v?: number | null): string {
  if (v == null) return '-'
  return `${Number(v).toFixed(3)} kg`
}

function openCard() {
  cardVisible.value = true
}

function close() {
  emit('update:visible', false)
}

// 关闭 SKU 弹窗时也关闭名片
watch(cardVisible, (val) => {
  if (!val) {
    // 名片已关闭，不做额外处理
  }
})
</script>

<style scoped lang="scss">
.mpr-sku-sheet-overlay {
  position: fixed;
  inset: 0;
  background: rgb(15 23 42 / 45%);
  z-index: 1100;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  font-family: 'ZCOOL KuaiLe', 'Microsoft YaHei', sans-serif;
}

.mpr-sku-sheet {
  width: 100%;
  max-width: 500px;
  max-height: 88dvh;
  background: #faf8f5;
  border-radius: 24px 24px 0 0;
  padding: 12px 0 max(20px, env(safe-area-inset-bottom));
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
}

/* ===== 装饰 ===== */
.mpr-sku-sheet__deco {
  position: absolute;
  pointer-events: none;
  z-index: 1;

  &--star {
    bottom: 40px;
    right: 16px;
    width: 24px;
    opacity: 0.5;
    animation: twinkle 2.5s ease-in-out infinite;
  }
}

@keyframes twinkle {
  0%, 100% { opacity: 0.4; transform: scale(1) rotate(0deg); }
  50% { opacity: 0.8; transform: scale(1.1) rotate(5deg); }
}

.mpr-sku-sheet__handle {
  width: 36px;
  height: 4px;
  background: #cbd5e1;
  border-radius: 999px;
  margin: 0 auto 10px;
  position: relative;
  z-index: 2;
  flex-shrink: 0;
}

/* ===== Header ===== */
.mpr-sku-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px 12px;
  position: relative;
  z-index: 2;
  flex-shrink: 0;
}

.mpr-sku-sheet__header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mpr-sku-sheet__header-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.mpr-sku-sheet__title {
  margin: 0;
  font-size: 18px;
  color: #1e293b;
  font-family: 'ZCOOL KuaiLe', 'Microsoft YaHei', sans-serif;
}

.mpr-sku-sheet__header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mpr-sku-sheet__card-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: #dbeafe;
  color: #2563eb;
  cursor: pointer;
  transition: transform 0.15s ease;

  &:active {
    transform: scale(0.9);
  }
}

.mpr-sku-sheet__close {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: #f1f5f9;
  color: #64748b;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  transition: transform 0.15s ease;

  &:active {
    transform: scale(0.9);
  }
}

/* ===== Body ===== */
.mpr-sku-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
  z-index: 2;
}

/* ===== Section Card ===== */
.mpr-sku-sheet__section-card {
  background: #faf8f5;

  :deep(.sa-doodle-frame__body) {
    padding: 8px;
  }
}

.mpr-sku-sheet__section-card-inner {
  padding: 24px;
}

.mpr-sku-sheet__section-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

.mpr-sku-sheet__section-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.mpr-sku-sheet__section-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  font-family: 'ZCOOL KuaiLe', 'Microsoft YaHei', sans-serif;
}

/* ===== SKU 基本信息 ===== */
.mpr-sku-sheet__sku-header {
  display: flex;
  gap: 14px;
  margin-bottom: 14px;
}

.mpr-sku-sheet__sku-img-wrap {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.mpr-sku-sheet__sku-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.mpr-sku-sheet__sku-img-placeholder {
  font-size: 28px;
}

.mpr-sku-sheet__sku-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  justify-content: center;
}

.mpr-sku-sheet__sku-code {
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
  font-family: monospace;
}

.mpr-sku-sheet__sku-spec {
  font-size: 12px;
  color: #64748b;
}

.mpr-sku-sheet__sku-price {
  font-size: 18px;
  font-weight: 800;
  color: #ef4444;
  margin-top: 2px;
}

.mpr-sku-sheet__sku-meta {
  display: flex;
  gap: 16px;
  align-items: center;
}

.mpr-sku-sheet__sku-meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.mpr-sku-sheet__meta-label {
  font-size: 12px;
  color: #94a3b8;
}

.mpr-sku-sheet__meta-chip {
  font-size: 10px;
  font-weight: 700;
  background: #faf8f5;

  :deep(.sa-doodle-frame__body) {
    padding: 8px;
  }
}

.mpr-sku-sheet__meta-val {
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;

  &--rebate {
    color: #f59e0b;
    font-size: 18px;
  }
}

/* ===== 尺寸网格 ===== */
.mpr-sku-sheet__outer_dim-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 10px;
}
.mpr-sku-sheet__dim-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 10px;
}

.mpr-sku-sheet__dim-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 8px;
  background: #fff;
  border-radius: 10px;
  border: 1.5px dashed #e2e8f0;
}

.mpr-sku-sheet__dim-label {
  font-size: 10px;
  color: #94a3b8;
}

.mpr-sku-sheet__dim-val {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
}

/* ===== 重量网格 ===== */
.mpr-sku-sheet__single_weight-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 8px;
  margin-top: 10px;
}
.mpr-sku-sheet__weight-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 10px;
}

.mpr-sku-sheet__weight-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 8px;
  background: #fff;
  border-radius: 10px;
  border: 1.5px dashed #e2e8f0;
}

.mpr-sku-sheet__weight-label {
  font-size: 10px;
  color: #94a3b8;
}

.mpr-sku-sheet__weight-val {
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;
}

.mpr-sku-sheet__carton-name {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1.5px dashed #e2e8f0;
  font-size: 12px;
  color: #64748b;
  font-family: 'ZCOOL KuaiLe', 'Microsoft YaHei', sans-serif;
}

/* ===== 内箱 ===== */
.mpr-sku-sheet__inner-box-note {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 12px;
  background: #fff;
  border-radius: 10px;
  border: 1.5px dashed #e2e8f0;
  font-size: 13px;
  color: #94a3b8;
  font-family: 'ZCOOL KuaiLe', 'Microsoft YaHei', sans-serif;
}

.mpr-sku-sheet__inner-box-icon {
  font-size: 18px;
}

/* ===== Transition ===== */
.mpr-sku-sheet-enter-active,
.mpr-sku-sheet-leave-active {
  transition: opacity 0.2s ease;

  .mpr-sku-sheet {
    transition: transform 0.25s ease;
  }
}

.mpr-sku-sheet-enter-from,
.mpr-sku-sheet-leave-to {
  opacity: 0;

  .mpr-sku-sheet {
    transform: translateY(100%);
  }
}
</style>
