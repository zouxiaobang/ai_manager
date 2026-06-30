<template>
  <div
    ref="rootRef"
    class="carton-box-3d"
    :class="{ 'is-dragging': dragging, 'is-empty': !hasSize, 'is-export': exportMode }"
    @pointerdown="onPointerDown"
    @pointermove="onPointerMove"
    @pointerup="onPointerUp"
    @pointercancel="onPointerUp"
    @pointerleave="onPointerUp"
  >
    <div class="carton-box-3d__scene" ref="sceneRef">
      <div class="carton-box-3d__box" :class="material.className" :style="boxStyle">
        <div
          v-for="face in faces"
          :key="face.key"
          class="carton-box-3d__face"
          :class="`is-${face.key}`"
          :style="face.style"
        >
          <div
            v-if="face.key === 'top' && material.tapeOnTop"
            class="carton-box-3d__tape"
            :class="material.className === 'material-express'
              ? 'carton-box-3d__tape--express'
              : 'carton-box-3d__tape--kraft'"
          />
          <span
            v-for="tag in face.labels"
            v-show="tag.text && !exportMode"
            :key="tag.kind"
            class="carton-box-3d__edge-label"
            :class="`carton-box-3d__edge-label--${tag.kind}`"
          >{{ tag.text }}</span>
        </div>
      </div>
    </div>

    <div v-if="hasSize && !exportMode" class="carton-box-3d__formula">{{ sizeFormula }}</div>
    <div v-else-if="!hasSize && !exportMode" class="carton-box-3d__placeholder">{{ t('ecommerce.carton.previewPlaceholder') }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { resolveCartonMaterial } from '@/constants/cartonMaterials'

const props = defineProps<{
  lengthCm?: number | null
  widthCm?: number | null
  heightCm?: number | null
  illustrationVariant?: number | null
  seed?: number | string
  /** 导出预览图：固定角度、白底、无尺寸标注 */
  exportMode?: boolean
}>()

const { t } = useI18n()

const rootRef = ref<HTMLElement | null>(null)
const sceneRef = ref<HTMLElement | null>(null)
const dragging = ref(false)
const rotX = ref(-16)
const rotY = ref(34)
const lastPointer = { x: 0, y: 0 }
let activePointerId: number | null = null

const length = computed(() => Number(props.lengthCm) || 0)
const width = computed(() => Number(props.widthCm) || 0)
const height = computed(() => Number(props.heightCm) || 0)

const hasSize = computed(() => length.value > 0 && width.value > 0 && height.value > 0)

const material = computed(() => resolveCartonMaterial(props.illustrationVariant, props.seed))

const displayDims = computed(() => {
  const l = hasSize.value ? length.value : 40
  const w = hasSize.value ? width.value : 30
  const h = hasSize.value ? height.value : 25
  const max = Math.max(l, w, h)
  const unit = 150 / max
  return {
    length: l * unit,
    width: w * unit,
    height: h * unit,
  }
})

const sizeFormula = computed(() => {
  if (!hasSize.value) return ''
  return `${length.value.toFixed(2)} × ${width.value.toFixed(2)} × ${height.value.toFixed(2)} cm`
})

const boxStyle = computed(() => ({
  '--box-l': `${displayDims.value.length}px`,
  '--box-w': `${displayDims.value.width}px`,
  '--box-h': `${displayDims.value.height}px`,
  '--carton-texture': `url(${material.value.textureUrl})`,
  transform: props.exportMode
    ? 'rotateX(-16deg) rotateY(34deg)'
    : `rotateX(${rotX.value}deg) rotateY(${rotY.value}deg)`,
}))

const faces = computed(() => {
  const lLabel = hasSize.value ? `${t('ecommerce.carton.length')} ${length.value.toFixed(0)}` : ''
  const wLabel = hasSize.value ? `${t('ecommerce.carton.width')} ${width.value.toFixed(0)}` : ''
  const hLabel = hasSize.value ? `${t('ecommerce.carton.height')} ${height.value.toFixed(0)}` : ''

  return [
    {
      key: 'front',
      labels: [
        { kind: 'length', text: lLabel },
        { kind: 'height', text: hLabel },
      ],
      style: {
        width: 'var(--box-l)',
        height: 'var(--box-h)',
        transform: 'translate(-50%, -50%) translateZ(calc(var(--box-w) / 2))',
      },
    },
    {
      key: 'back',
      labels: [],
      style: {
        width: 'var(--box-l)',
        height: 'var(--box-h)',
        transform: 'translate(-50%, -50%) rotateY(180deg) translateZ(calc(var(--box-w) / 2))',
      },
    },
    {
      key: 'left',
      labels: [{ kind: 'width', text: wLabel }],
      style: {
        width: 'var(--box-w)',
        height: 'var(--box-h)',
        transform: 'translate(-50%, -50%) rotateY(-90deg) translateZ(calc(var(--box-l) / 2))',
      },
    },
    {
      key: 'right',
      labels: [],
      style: {
        width: 'var(--box-w)',
        height: 'var(--box-h)',
        transform: 'translate(-50%, -50%) rotateY(90deg) translateZ(calc(var(--box-l) / 2))',
      },
    },
    {
      key: 'top',
      labels: [],
      style: {
        width: 'var(--box-l)',
        height: 'var(--box-w)',
        transform: 'translate(-50%, -50%) rotateX(90deg) translateZ(calc(var(--box-h) / 2))',
      },
    },
    {
      key: 'bottom',
      labels: [],
      style: {
        width: 'var(--box-l)',
        height: 'var(--box-w)',
        transform: 'translate(-50%, -50%) rotateX(-90deg) translateZ(calc(var(--box-h) / 2))',
      },
    },
  ]
})

function onPointerDown(event: PointerEvent) {
  if (props.exportMode) return
  if (event.button !== 0) return
  dragging.value = true
  activePointerId = event.pointerId
  lastPointer.x = event.clientX
  lastPointer.y = event.clientY
  rootRef.value?.setPointerCapture(event.pointerId)
}

function onPointerMove(event: PointerEvent) {
  if (!dragging.value || activePointerId !== event.pointerId) return
  const dx = event.clientX - lastPointer.x
  const dy = event.clientY - lastPointer.y
  rotY.value += dx * 0.55
  rotX.value -= dy * 0.55
  rotX.value = Math.max(-85, Math.min(85, rotX.value))
  lastPointer.x = event.clientX
  lastPointer.y = event.clientY
}

function onPointerUp(event: PointerEvent) {
  if (activePointerId !== event.pointerId) return
  dragging.value = false
  activePointerId = null
  if (rootRef.value?.hasPointerCapture(event.pointerId)) {
    rootRef.value.releasePointerCapture(event.pointerId)
  }
}

onBeforeUnmount(() => {
  dragging.value = false
  activePointerId = null
})
</script>

<style lang="scss">
@use '@/styles/carton-materials.scss';
</style>

<style scoped lang="scss">
.carton-box-3d {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 320px;
  padding: 12px 16px 8px;
  touch-action: none;
  cursor: grab;
  user-select: none;

  &.is-dragging {
    cursor: grabbing;
  }

  &.is-empty {
    opacity: 0.88;
  }

  &.is-export {
    min-height: 0;
    padding: 0;
    cursor: default;
    pointer-events: none;
  }
}

.carton-box-3d.is-export .carton-box-3d__scene {
  height: 320px;
  background: #fff;
  border-radius: 8px;
}

.carton-box-3d__scene {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 280px;
  perspective: 920px;
}

.carton-box-3d__box {
  position: relative;
  width: var(--box-l);
  height: var(--box-h);
  transform-style: preserve-3d;
  transition: transform 0.08s linear;
}

.carton-box-3d__face {
  position: absolute;
  top: 50%;
  left: 50%;
  box-sizing: border-box;
  border: 1px solid rgb(120 84 48 / 35%);
  backface-visibility: hidden;
  box-shadow: inset 0 0 0 1px rgb(255 255 255 / 10%);
}

.carton-box-3d__edge-label {
  position: absolute;
  z-index: 2;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgb(255 255 255 / 88%);
  border: 1px solid #e2e8f0;
  font-size: 10px;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
  pointer-events: none;

  &--length {
    left: 50%;
    bottom: 8px;
    transform: translateX(-50%);
  }

  &--width {
    left: 50%;
    bottom: 8px;
    transform: translateX(-50%);
  }

  &--height {
    left: 8px;
    top: 50%;
    transform: translateY(-50%);
  }
}

.carton-box-3d__formula {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--el-color-primary);
}

.carton-box-3d__placeholder {
  margin-top: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
