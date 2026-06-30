<template>
  <div class="carton-box-illustration">
    <div class="carton-box-illustration__media">
      <button
        v-if="!previewImageUrl"
        type="button"
        class="carton-box-illustration__shuffle"
        :title="t('ecommerce.carton.shuffleIllustration')"
        @click="shuffleVariant"
      >
        <el-icon><Refresh /></el-icon>
      </button>
      <img
        :key="displayImage"
        class="carton-box-illustration__image"
        :class="{
          'is-muted': !hasSize && !previewImageUrl,
          'is-preview-3d': !!previewImageUrl,
        }"
        :src="displayImage"
        :alt="t('ecommerce.carton.illustrationAlt')"
        draggable="false"
      />
      <span v-if="!hasSize && !previewImageUrl" class="carton-box-illustration__empty-text">—</span>
    </div>

    <div v-if="hasSize" class="carton-box-illustration__spec" :aria-label="dimensionAriaLabel">
      <div class="carton-box-illustration__spec-rows">
        <div v-for="item in dimensionItems" :key="item.key" class="carton-box-illustration__spec-row">
          <span class="carton-box-illustration__spec-label">{{ item.label }}</span>
          <span class="carton-box-illustration__spec-value">{{ item.value }}</span>
          <span class="carton-box-illustration__spec-unit">cm</span>
        </div>
      </div>

      <div class="carton-box-illustration__formula">{{ sizeFormula }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Refresh } from '@element-plus/icons-vue'
import {
  CARTON_ILLUSTRATIONS,
  CARTON_ILLUSTRATION_COUNT,
  resolveCartonIllustrationVariant,
} from '@/constants/cartonIllustrations'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'

const props = defineProps<{
  lengthCm?: number
  widthCm?: number
  heightCm?: number
  seed?: number | string
  /** 持久化样式 0~3，优先于 seed 哈希 */
  variant?: number | null
  /** 保存的 3D 预览图文件名 */
  previewImage?: string | null
  /** 用于刷新预览图缓存 */
  previewVersion?: string | null
}>()

const { t } = useI18n()

const manualVariant = ref<number | null>(null)

const length = computed(() => Number(props.lengthCm) || 0)
const width = computed(() => Number(props.widthCm) || 0)
const height = computed(() => Number(props.heightCm) || 0)

const hasSize = computed(() => length.value > 0 && width.value > 0 && height.value > 0)

const variantIndex = computed(() => {
  if (manualVariant.value !== null) return manualVariant.value
  return resolveCartonIllustrationVariant(props.variant, props.seed)
})

const currentImage = computed(() => CARTON_ILLUSTRATIONS[variantIndex.value])

const previewImageUrl = computed(() => {
  const base = getEcommerceImageUrl(props.previewImage)
  if (!base) return ''
  const version = props.previewVersion ?? props.previewImage
  if (!version) return base
  return `${base}?v=${encodeURIComponent(String(version))}`
})

const displayImage = computed(() => previewImageUrl.value || currentImage.value)

const sizeFormula = computed(() => {
  if (!hasSize.value) return '—'
  return `${length.value.toFixed(2)} × ${width.value.toFixed(2)} × ${height.value.toFixed(2)} cm`
})

const dimensionItems = computed(() => [
  { key: 'length', label: t('ecommerce.carton.length'), value: length.value.toFixed(2) },
  { key: 'width', label: t('ecommerce.carton.width'), value: width.value.toFixed(2) },
  { key: 'height', label: t('ecommerce.carton.height'), value: height.value.toFixed(2) },
])

const dimensionAriaLabel = computed(() => {
  if (!hasSize.value) return ''
  return dimensionItems.value.map((item) => `${item.label} ${item.value}cm`).join(', ')
})

watch(
  () => [props.seed, props.variant] as const,
  () => {
    manualVariant.value = null
  },
)

function shuffleVariant() {
  let next = Math.floor(Math.random() * CARTON_ILLUSTRATION_COUNT)
  if (CARTON_ILLUSTRATION_COUNT > 1 && next === variantIndex.value) {
    next = (next + 1) % CARTON_ILLUSTRATION_COUNT
  }
  manualVariant.value = next
}
</script>

<style scoped lang="scss">
.carton-box-illustration {
  flex-shrink: 0;
  width: 280px;
}

.carton-box-illustration__media {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  padding: 8px;
  border-radius: 10px;
  background: #fff;
  overflow: visible;

  &:has(.is-preview-3d) {
    min-height: 0;
    padding: 12px 8px;
  }
}

.carton-box-illustration__shuffle {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: rgb(255 255 255 / 92%);
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition: color 0.15s ease, border-color 0.15s ease, background 0.15s ease;

  &:hover {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary-light-5);
    background: #fff;
  }
}

.carton-box-illustration__image {
  display: block;
  width: 100%;
  max-height: 200px;
  object-fit: contain;
  user-select: none;

  &.is-muted {
    opacity: 0.45;
  }

  &.is-preview-3d {
    width: 100%;
    height: auto;
    max-height: none;
    object-fit: contain;
    object-position: center center;
  }
}

.carton-box-illustration__empty-text {
  position: absolute;
  font-size: 18px;
  color: var(--el-text-color-placeholder);
}

.carton-box-illustration__spec {
  margin-top: 10px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: #fff;
}

.carton-box-illustration__spec-rows {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.carton-box-illustration__spec-row {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 4px;
  border-radius: 8px;
  background: #f8fafc;
}

.carton-box-illustration__spec-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.carton-box-illustration__spec-value {
  font-size: 15px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--wr-text, #333);
  line-height: 1.2;
}

.carton-box-illustration__spec-unit {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.carton-box-illustration__formula {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color-lighter);
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--el-color-primary);
}
</style>
