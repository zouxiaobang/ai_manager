<template>
  <Teleport to="body">
    <Transition name="mpr-sku-card">
      <div v-if="visible" class="mpr-sku-card-overlay">
        <!-- 工具栏 -->
        <div class="mpr-sku-card__toolbar">
          <button type="button" class="mpr-sku-card__toolbar-btn mpr-sku-card__toolbar-btn--close" @click="close">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            <span>关闭</span>
          </button>
        </div>

        <!-- 可截屏区域 -->
        <div v-if="sku" class="mpr-sku-card__scroll-area">
          <div ref="cardRef" class="mpr-sku-card">
            <!-- 顶部装饰条 -->
            <div class="mpr-sku-card__top-bar">
              <div class="mpr-sku-card__top-bar-inner">
                <div class="mpr-sku-card__top-bar-dot" />
                <span class="mpr-sku-card__top-bar-text">SKU BUSINESS CARD</span>
                <div class="mpr-sku-card__top-bar-dot" />
              </div>
            </div>

            <!-- 卡片主体 -->
            <div class="mpr-sku-card__body">
              <!-- 顶部：图片 + SKU 标识（左右排布） -->
              <div class="mpr-sku-card__top">
                <div class="mpr-sku-card__image-wrap">
                  <img
                    v-if="sku.imageName && !imageBroken"
                    :src="skuImageUrl"
                    alt=""
                    class="mpr-sku-card__image"
                    crossorigin="anonymous"
                    @error="imageBroken = true"
                  />
                  <div v-else class="mpr-sku-card__image-placeholder">
                    <span class="mpr-sku-card__image-placeholder-icon">📷</span>
                    <span>暂无图片</span>
                  </div>
                </div>
                <div class="mpr-sku-card__identity">
                  <div class="mpr-sku-card__code">{{ sku.skuCode }}</div>
                  <div v-if="sku.specName" class="mpr-sku-card__spec">{{ sku.specName }}</div>
                </div>
              </div>

              <!-- 底部：详细信息 -->
              <div class="mpr-sku-card__bottom">
                <!-- 单品尺寸 -->
                <div class="mpr-sku-card__section">
                  <h4 class="mpr-sku-card__section-title">📏 单品尺寸</h4>
                  <dl class="mpr-sku-card__fields">
                    <div class="mpr-sku-card__row">
                      <dt>产品尺寸(L×W×H)</dt>
                      <dd>{{ formatSize(sku.productLengthCm, sku.productWidthCm, sku.productHeightCm) }}</dd>
                    </div>
                    <div class="mpr-sku-card__row">
                      <dt>单品重量</dt>
                      <dd>{{ unitWeightText }}</dd>
                    </div>
                  </dl>
                </div>

                <!-- 包装信息 -->
                <div class="mpr-sku-card__section">
                  <h4 class="mpr-sku-card__section-title">📦 包装信息</h4>
                  <dl class="mpr-sku-card__fields">
                    <div class="mpr-sku-card__row">
                      <dt>外箱尺寸(L×W×H)</dt>
                      <dd>{{ formatSize(sku.cartonLengthCm, sku.cartonWidthCm, sku.cartonHeightCm) }}</dd>
                    </div>
                    <div class="mpr-sku-card__row">
                      <dt>毛重</dt>
                      <dd>{{ formatWeight(sku.cartonGrossWeightKg) }}</dd>
                    </div>
                    <div class="mpr-sku-card__row">
                      <dt>净重</dt>
                      <dd>{{ formatWeight(sku.cartonNetWeightKg) }}</dd>
                    </div>
                    <div class="mpr-sku-card__row">
                      <dt>箱装数</dt>
                      <dd>{{ sku.unitsPerCarton ?? '—' }} 个</dd>
                    </div>
                  </dl>
                </div>

              </div>
            </div>

            <!-- 底部水印 -->
            <div class="mpr-sku-card__footer">
              <span class="mpr-sku-card__footer-text">AI Manager · SKU 信息名片</span>
              <span class="mpr-sku-card__footer-date">{{ currentDate }}</span>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import type {EcSku} from '@/api/ecommerce/product'
import {getEcommerceImageUrl} from '@/api/ecommerce/image'

const props = withDefaults(
  defineProps<{
    visible: boolean
    sku: EcSku | null
  }>(),
  { visible: false, sku: null },
)

const emit = defineEmits<{
  close: []
}>()

const cardRef = ref<HTMLElement | null>(null)
const imageBroken = ref(false)

const skuImageUrl = computed(() => getEcommerceImageUrl(props.sku?.imageName))

const currentDate = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

const unitWeightText = computed(() => {
  if (!props.sku) return '—'
  const gross = props.sku.cartonGrossWeightKg
  const units = props.sku.unitsPerCarton
  if (gross == null || units == null || units < 1) return '—'
  return `${(Number(gross) / Number(units)).toFixed(3)} kg`
})

watch(
  () => [props.visible, props.sku?.imageName] as const,
  () => {
    imageBroken.value = false
  },
)

function formatSize(l?: number | null, w?: number | null, h?: number | null): string {
  if (l == null && w == null && h == null) return '—'
  const fmt = (v?: number | null) => (v != null ? Number(v).toFixed(2) : '—')
  return `${fmt(l)} × ${fmt(w)} × ${fmt(h)} cm`
}

function formatWeight(v?: number | null): string {
  if (v == null) return '—'
  return `${Number(v).toFixed(3)} kg`
}

function close() {
  emit('close')
}
</script>

<style scoped lang="scss">
.mpr-sku-card-overlay {
  position: fixed;
  inset: 0;
  background: #f0f2f5;
  z-index: 1200;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ===== 工具栏 ===== */
.mpr-sku-card__toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 8px;
  flex-shrink: 0;
  z-index: 2;
}

.mpr-sku-card__toolbar-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1.5px solid #2563eb;
  border-radius: 8px;
  background: #fff;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;

  &:active {
    transform: scale(0.95);
    background: #eff6ff;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &--close {
    border-color: #e2e8f0;
    color: #64748b;

    &:active {
      background: #f8fafc;
    }
  }
}

/* ===== 可滚动区域 ===== */
.mpr-sku-card__scroll-area {
  flex: 1;
  overflow-y: auto;
  display: flex;
  justify-content: center;
  padding: 20px 16px max(40px, env(safe-area-inset-bottom));
}

/* ===== 名片主体 ===== */
.mpr-sku-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgb(15 23 42 / 10%);
  overflow: hidden;
  align-self: flex-start;
}

/* ===== 顶部装饰条 ===== */
.mpr-sku-card__top-bar {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  padding: 10px 16px;
}

.mpr-sku-card__top-bar-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.mpr-sku-card__top-bar-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
}

.mpr-sku-card__top-bar-text {
  font-size: 12px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.9);
  letter-spacing: 0.1em;
}

/* ===== 卡片主体 ===== */
.mpr-sku-card__body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ===== 顶部：图片 + 标识 ===== */
.mpr-sku-card__top {
  display: flex;
  gap: 16px;
  align-items: center;
}

.mpr-sku-card__image-wrap {
  width: 100px;
  height: 100px;
  flex-shrink: 0;
  border-radius: 12px;
  border: 1.5px solid #e5e7eb;
  overflow: hidden;
  background: #f9fafb;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mpr-sku-card__image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.mpr-sku-card__image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #9ca3af;
  font-size: 12px;
}

.mpr-sku-card__image-placeholder-icon {
  font-size: 32px;
}

.mpr-sku-card__identity {
  flex: 1;
  min-width: 0;
  text-align: left;
}

.mpr-sku-card__code {
  font-size: 22px;
  font-weight: bold;
  color: #000;
}

.mpr-sku-card__spec {
  margin-top: 4px;
  font-size: 16px;
  color: #374151;
  word-break: break-word;
  font-family: 'ZCOOL KuaiLe', 'Microsoft YaHei', sans-serif;
}

.mpr-sku-card__price {
  margin-top: 6px;
  font-size: 20px;
  font-weight: 900;
  color: #c41e3a;
}

/* ===== 底部：详细信息 ===== */
.mpr-sku-card__bottom {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mpr-sku-card__section {
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
}

.mpr-sku-card__section-title {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  color: #00009B;
  text-align: center;
  padding-bottom: 8px;
  border-bottom: 1.5px dashed #cbd5e1;
}

.mpr-sku-card__fields {
  margin: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.mpr-sku-card__row {
  display: flex;
  grid-template-columns: 100px 1fr;
  gap: 6px;
  align-items: baseline;
  margin-bottom: 6px;
  font-size: 12px;

  &:last-child {
    margin-bottom: 0;
  }

  dt {
    margin: 0;
    text-align: right;
    color: #6b7280;
    font-weight: 500;
  }

  dd {
    margin: 0;
    color: #030712;
    font-weight: 600;
    word-break: break-word;
  }
}

/* ===== 底部水印 ===== */
.mpr-sku-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  border-top: 1px solid #e5e7eb;
  background: #f8fafc;
}

.mpr-sku-card__footer-text {
  font-size: 11px;
  color: #9ca3af;
  font-weight: 600;
}

.mpr-sku-card__footer-date {
  font-size: 11px;
  color: #9ca3af;
}

/* ===== Transition ===== */
.mpr-sku-card-enter-active,
.mpr-sku-card-leave-active {
  transition: opacity 0.2s ease;
}

.mpr-sku-card-enter-from,
.mpr-sku-card-leave-to {
  opacity: 0;
}
</style>
