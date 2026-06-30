<template>
  <el-dialog
    :model-value="modelValue"
    width="680px"
    append-to-body
    destroy-on-close
    :show-close="false"
    class="sku-detail-card-dialog"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div
      v-if="sku"
      ref="cardRef"
      class="sku-detail-card"
      :class="{ 'sku-detail-card--copying': copying }"
      :title="t('ecommerce.product.skuCardClickToCopy')"
      @click="copyCardImage"
    >
      <div class="sku-detail-card__layout">
        <div class="sku-detail-card__left">
          <div class="sku-detail-card__image-wrap">
            <img
              v-if="imageUrl && !imageBroken"
              :src="imageUrl"
              alt=""
              class="sku-detail-card__image"
              crossorigin="anonymous"
              @error="imageBroken = true"
            />
            <div v-else class="sku-detail-card__image-placeholder">
              <el-icon :size="40"><Picture /></el-icon>
              <span>{{ t('ecommerce.product.noImage') }}</span>
            </div>
          </div>

          <div class="sku-detail-card__identity">
            <p class="sku-detail-card__code">{{ sku.skuCode?.trim() || '—' }}</p>
            <p v-if="sku.specName?.trim()" class="sku-detail-card__spec">{{ sku.specName.trim() }}</p>
          </div>
        </div>

        <div class="sku-detail-card__right">
          <div class="sku-detail-card__section sku-detail-card__section--first">
            <h4 class="sku-detail-card__section-title">{{ t('ecommerce.product.skuSectionDimensions') }}</h4>
            <dl class="sku-detail-card__fields">
              <div class="sku-detail-card__row">
                <dt>{{ t('ecommerce.product.productSize') }}</dt>
                <dd>{{ formatSize(sku.productLengthCm, sku.productWidthCm, sku.productHeightCm) }}</dd>
              </div>
              <div class="sku-detail-card__row">
                <dt>{{ t('ecommerce.product.unitWeight') }}</dt>
                <dd>{{ unitWeightText }}</dd>
              </div>
            </dl>
          </div>

          <div class="sku-detail-card__section">
            <h4 class="sku-detail-card__section-title">{{ t('ecommerce.product.skuSectionPackaging') }}</h4>
            <dl class="sku-detail-card__fields">
              <div class="sku-detail-card__row">
                <dt>{{ t('ecommerce.product.cartonSize') }}</dt>
                <dd>{{ formatSize(sku.cartonLengthCm, sku.cartonWidthCm, sku.cartonHeightCm) }}</dd>
              </div>
              <div class="sku-detail-card__row">
                <dt>{{ t('ecommerce.product.gross') }}</dt>
                <dd>{{ formatWeight(sku.cartonGrossWeightKg) }}</dd>
              </div>
              <div class="sku-detail-card__row">
                <dt>{{ t('ecommerce.product.net') }}</dt>
                <dd>{{ formatWeight(sku.cartonNetWeightKg) }}</dd>
              </div>
              <div class="sku-detail-card__row">
                <dt>{{ t('ecommerce.product.unitsPerCarton') }}</dt>
                <dd>{{ sku.unitsPerCarton ?? '—' }}</dd>
              </div>
            </dl>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import html2canvas from 'html2canvas'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'
import type { EcSku } from '@/api/ecommerce/product'

const props = defineProps<{
  modelValue: boolean
  sku: EcSku | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const { t } = useI18n()
const cardRef = ref<HTMLElement | null>(null)
const imageBroken = ref(false)
const copying = ref(false)

const imageUrl = computed(() => getEcommerceImageUrl(props.sku?.imageName))

const unitWeightText = computed(() => {
  const sku = props.sku
  if (!sku) return '—'
  const gross = sku.cartonGrossWeightKg
  const units = sku.unitsPerCarton
  if (gross == null || units == null || units < 1) return '—'
  return `${(Number(gross) / Number(units)).toFixed(3)} kg`
})

watch(
  () => [props.modelValue, props.sku?.imageName] as const,
  () => {
    imageBroken.value = false
  },
)

function formatSize(l?: number | null, w?: number | null, h?: number | null) {
  if (l == null && w == null && h == null) return '—'
  const fmt = (v?: number | null) => (v != null ? Number(v).toFixed(2) : '—')
  return `${fmt(l)} × ${fmt(w)} × ${fmt(h)} cm`
}

function formatWeight(v?: number | null) {
  if (v == null) return '—'
  return `${Number(v).toFixed(3)} kg`
}

async function renderCardCanvas() {
  const el = cardRef.value
  if (!el) throw new Error('card not ready')
  return html2canvas(el, {
    backgroundColor: '#ffffff',
    scale: 2,
    useCORS: true,
    logging: false,
  })
}

async function copyCardImage() {
  if (copying.value || !props.sku) return

  copying.value = true
  try {
    const canvas = await renderCardCanvas()
    const blob = await new Promise<Blob | null>((resolve) => {
      canvas.toBlob((value) => resolve(value), 'image/png')
    })
    if (!blob) throw new Error('blob failed')

    if (navigator.clipboard?.write && typeof ClipboardItem !== 'undefined') {
      await navigator.clipboard.write([new ClipboardItem({ 'image/png': blob })])
      ElMessage.success(t('ecommerce.product.skuCardCopied'))
      return
    }

    const link = document.createElement('a')
    const code = props.sku.skuCode?.trim() || 'sku'
    link.download = `${code}-detail.png`
    link.href = canvas.toDataURL('image/png')
    link.click()
    ElMessage.success(t('ecommerce.product.skuCardDownloaded'))
  } catch {
    ElMessage.error(t('ecommerce.product.skuCardCopyFailed'))
  } finally {
    copying.value = false
  }
}
</script>

<style lang="scss">
.sku-detail-card-dialog {
  .el-dialog__header {
    display: none;
  }

  .el-dialog__body {
    padding: 12px;
  }

  .el-dialog__footer {
    display: none;
  }
}
</style>

<style scoped lang="scss">
.sku-detail-card {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  color: #111827;
  cursor: pointer;
  transition: box-shadow 0.15s ease;

  &:hover {
    box-shadow: 0 4px 14px rgb(15 23 42 / 8%);
  }

  &--copying {
    pointer-events: none;
    opacity: 0.85;
  }
}

.sku-detail-card__layout {
  display: flex;
  align-items: stretch;
  gap: 16px;
  min-width: 0;
}

.sku-detail-card__left {
  flex: 0 0 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.sku-detail-card__right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.sku-detail-card__image-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.sku-detail-card__image {
  width: 140px;
  height: 140px;
  object-fit: contain;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
}

.sku-detail-card__image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 140px;
  height: 140px;
  border-radius: 8px;
  border: 1px dashed #d1d5db;
  background: #f9fafb;
  color: #9ca3af;
  font-size: 12px;
}

.sku-detail-card__identity {
  width: 100%;
  text-align: center;
}

.sku-detail-card__code {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  line-height: 1.35;
  color: #030712;
  letter-spacing: 0.02em;
  word-break: break-word;
}

.sku-detail-card__spec {
  margin: 8px 0 0;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.4;
  color: #111827;
  word-break: break-word;
}

.sku-detail-card__section {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f3f4f6;

  &--first {
    margin-top: 0;
    padding-top: 0;
    border-top: none;
  }
}

.sku-detail-card__section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  line-height: 1.4;

  &::before {
    content: '';
    width: 3px;
    height: 14px;
    border-radius: 2px;
    flex-shrink: 0;
    background: #2563eb;
  }
}

.sku-detail-card__fields {
  margin: 0;
}

.sku-detail-card__row {
  display: grid;
  grid-template-columns: 118px 1fr;
  gap: 8px;
  align-items: baseline;
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.5;

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
    color: #111827;
    word-break: break-word;
  }
}
</style>
