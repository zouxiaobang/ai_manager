<template>
  <div
    ref="containerRef"
    class="swipeable-card"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <div class="swipeable-card__wrapper" :style="wrapperStyle">
      <div class="swipeable-card__content" @click="closeSelf">
        <slot />
      </div>
      <div class="swipeable-card__actions">
        <button class="swipeable-card__delete-btn" @click.stop="onDelete">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6" />
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
          </svg>
          <span>{{ deleteText }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

defineProps<{
  deleteText?: string
}>()

const emit = defineEmits<{
  delete: []
}>()

const ACTION_WIDTH = 70
const THRESHOLD = 25

const containerRef = ref<HTMLElement>()

const offsetX = ref(0)
const startX = ref(0)
const isDragging = ref(false)
const isOpen = ref(false)
let lastTouchEndTime = 0

const wrapperStyle = computed(() => {
  const x = isOpen.value ? -ACTION_WIDTH : Math.min(offsetX.value, 0)
  return {
    transform: `translateX(${x}px)`,
    transition: isDragging.value ? 'none' : 'transform 0.25s cubic-bezier(0.22, 1, 0.36, 1)'
  }
})

function onTouchStart(e: TouchEvent) {
  startX.value = e.touches[0].clientX
  isDragging.value = true
}

function onTouchMove(e: TouchEvent) {
  if (!isDragging.value) return
  const currentX = e.touches[0].clientX
  const delta = currentX - startX.value
  if (delta > 0 && offsetX.value >= 0) return
  offsetX.value = isOpen.value ? delta - ACTION_WIDTH : delta
}

function onTouchEnd() {
  isDragging.value = false
  lastTouchEndTime = Date.now()
  if (isOpen.value) {
    if (offsetX.value > -THRESHOLD) {
      isOpen.value = false
    }
    offsetX.value = 0
  } else {
    if (offsetX.value < -THRESHOLD) {
      isOpen.value = true
      broadcastOpen()
    }
    offsetX.value = 0
  }
}

function closeSelf() {
  // Ignore clicks that fire shortly after a touch/swipe (< 500ms)
  // to prevent a spurious click event from closing the card right after swipe.
  if (Date.now() - lastTouchEndTime < 500) return
  if (isOpen.value) {
    isOpen.value = false
  }
}

function onDelete() {
  emit('delete')
  isOpen.value = false
}

function broadcastOpen() {
  document.dispatchEvent(new CustomEvent('swipeable-card-open'))
}

function onGlobalOpen() {
  if (isOpen.value) {
    isOpen.value = false
    offsetX.value = 0
  }
}

onMounted(() => {
  document.addEventListener('swipeable-card-open', onGlobalOpen)
})

onUnmounted(() => {
  document.removeEventListener('swipeable-card-open', onGlobalOpen)
})
</script>

<style lang="scss" scoped>
.swipeable-card {
  width: 100%;
  overflow: hidden;
  position: relative;
  touch-action: pan-y;

  &__wrapper {
    display: flex;
    width: 100%;
    will-change: transform;
  }

  &__content {
    flex-shrink: 0;
    width: 100%;
  }

  &__actions {
    flex-shrink: 0;
    width: 70px;
    display: flex;
    align-items: stretch;
  }

  &__delete-btn {
    width: 100%;
    border: none;
    background: #ef4444;
    color: white;
    font-family: 'ZCOOL KuaiLe', cursive;
    font-size: 12px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    cursor: pointer;
    transition: background 0.15s;
    padding: 0;

    &:active {
      background: #dc2626;
    }
  }
}
</style>
