<template>
  <div
    ref="spriteRef"
    class="pixel-dog-mobile-sprite"
    :class="[`pixel-dog-mobile-sprite--${status.toLowerCase()}`, `pixel-dog-mobile-sprite--face-${faceDirection}`, {
      'pixel-dog-mobile-sprite--active': activityLevel > 0.6,
      'pixel-dog-mobile-sprite--lazy': activityLevel < 0.3,
      'pixel-dog-mobile-sprite--focus': status === 'FOCUS'
    }]"
    :style="{
      transform: `translate(${position.x}px, ${position.y}px)`,
    }"
    @click="onClick"
    @touchstart.prevent="onTouchStart"
    @touchend.prevent="onTouchEnd"
  >
    <div class="pixel-dog-mobile-sprite__canvas">
      <canvas
        ref="canvasRef"
        class="pixel-dog-mobile-sprite__canvas-el"
        :width="viewBoxSize"
        :height="viewBoxSize"
        :style="{ width: displaySize + 'px', height: displaySize + 'px' }"
      ></canvas>
    </div>

    <div v-if="status === 'HAPPY'" class="pixel-dog-mobile-sprite__sparkles">
      <span class="pixel-dog-mobile-sprite__sparkle">✨</span>
      <span class="pixel-dog-mobile-sprite__sparkle">✨</span>
      <span class="pixel-dog-mobile-sprite__sparkle">✨</span>
    </div>

    <div v-if="status === 'PETTING'" class="pixel-dog-mobile-sprite__hearts">
      <span class="pixel-dog-mobile-sprite__heart">❤️</span>
      <span class="pixel-dog-mobile-sprite__heart">💕</span>
      <span class="pixel-dog-mobile-sprite__heart">❤️</span>
    </div>

    <div v-if="status === 'NUZZLE'" class="pixel-dog-mobile-sprite__paws">
      <span class="pixel-dog-mobile-sprite__paw">🐾</span>
      <span class="pixel-dog-mobile-sprite__paw">💗</span>
      <span class="pixel-dog-mobile-sprite__paw">🐾</span>
    </div>

    <div v-if="status === 'GREETING'" class="pixel-dog-mobile-sprite__waves">
      <span class="pixel-dog-mobile-sprite__wave">👋</span>
      <span class="pixel-dog-mobile-sprite__wave">✋</span>
    </div>

    <div v-if="status === 'SLEEPING'" class="pixel-dog-mobile-sprite__z">
      <span>Z</span>
      <span>z</span>
      <span>Z</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ITEM_SHAPES, SHAPE_OFFSETS } from '@/data/pixel-dog-items'
import type { PixelDogItemVO } from '@/api/pixelDog'

type DogStatus = 'IDLE' | 'HAPPY' | 'PETTING' | 'GREETING' | 'SLEEPING' | 'WALKING' | 'FOCUS' | 'NUZZLE'

const props = defineProps<{
  status: DogStatus
  emotion: number
  bond: number
  level?: number
  equippedItems?: number
  items?: PixelDogItemVO[]
}>()

const emit = defineEmits<{
  pet: []
}>()

const SIZE = 16
const BASE_SCALE = 4
const DISPLAY_SCALE = 2

const level = computed(() => props.level || 1)

const levelScale = computed(() => {
  return Math.min(1 + (level.value - 1) * 0.08, 2)
})

const SCALE = computed(() => {
  const s = Math.trunc(BASE_SCALE * levelScale.value)
  return Math.max(2, Math.min(12, s))
})

const viewBoxSize = computed(() => SIZE * SCALE.value)
const displaySize = computed(() => viewBoxSize.value * DISPLAY_SCALE)

const separation = computed(() => {
  return Math.min((level.value - 1) * 0.5, 4)
})

const canvasRef = ref<HTMLCanvasElement | null>(null)
const spriteRef = ref<HTMLElement | null>(null)

const position = ref({ x: 0, y: 0 })
const faceDirection = ref<'front' | 'left' | 'right' | 'back'>('front')

const containerSize = ref({ width: 300, height: 300 })

const moveBounds = computed(() => {
  const spriteSize = displaySize.value
  const w = containerSize.value.width
  const h = containerSize.value.height
  return {
    maxX: (w - spriteSize) / 2,
    maxY: (h - spriteSize) / 2,
  }
})

const bondFactor = computed(() => {
  return Math.min(1, 0.6 + props.bond / 100 * 0.4)
})

let moveTimer: ReturnType<typeof setInterval> | null = null
let targetPoint = { x: 0, y: 0 }
let isIdle = false
let idleTimer: ReturnType<typeof setTimeout> | null = null

function pickRandomTarget() {
  const factor = bondFactor.value
  const maxX = moveBounds.value.maxX * factor
  const maxY = moveBounds.value.maxY * factor
  targetPoint = {
    x: (Math.random() * 2 - 1) * maxX,
    y: (Math.random() * 2 - 1) * maxY,
  }
}

function updateFaceDirection(dx: number, dy: number) {
  if (Math.abs(dx) > Math.abs(dy)) {
    faceDirection.value = dx > 0 ? 'right' : 'left'
  } else {
    faceDirection.value = dy > 0 ? 'front' : 'back'
  }
}

function startIdle() {
  isIdle = true
  const idleDuration = 1000 + Math.random() * 2500
  idleTimer = setTimeout(() => {
    isIdle = false
    pickRandomTarget()
  }, idleDuration)
}

function startMoving() {
  if (moveTimer) clearInterval(moveTimer)
  if (idleTimer) clearTimeout(idleTimer)

  pickRandomTarget()

  moveTimer = setInterval(() => {
    if (props.status === 'FOCUS' || props.status === 'SLEEPING') {
      position.value = { x: 0, y: 0 }
      faceDirection.value = 'front'
      return
    }

    if (isIdle) return

    let { x, y } = position.value
    const dx = targetPoint.x - x
    const dy = targetPoint.y - y
    const dist = Math.sqrt(dx * dx + dy * dy)

    if (dist < 3) {
      startIdle()
      return
    }

    const baseSpeed = 1.5 + Math.random() * 1.5
    const vx = (dx / dist) * baseSpeed
    const vy = (dy / dist) * baseSpeed

    x += vx
    y += vy

    updateFaceDirection(dx, dy)

    const factor = bondFactor.value
    const maxX = moveBounds.value.maxX * factor
    const maxY = moveBounds.value.maxY * factor

    if (x > maxX) x = maxX
    if (x < -maxX) x = -maxX
    if (y > maxY) y = maxY
    if (y < -maxY) y = -maxY

    position.value = { x, y }
  }, 50)
}

function stopMoving() {
  if (moveTimer) {
    clearInterval(moveTimer)
    moveTimer = null
  }
  if (idleTimer) {
    clearTimeout(idleTimer)
    idleTimer = null
  }
}

interface ItemPixelGrid {
  grid: number[][]
  fill: string
  offsetCol: number
  offsetRow: number
}

const itemGrids = computed<ItemPixelGrid[]>(() => {
  const apiItems = props.items || []
  return apiItems.map((item) => {
    const shapeIdx = item.shape % ITEM_SHAPES.length
    const offset = SHAPE_OFFSETS[shapeIdx]
    return {
      grid: ITEM_SHAPES[shapeIdx],
      fill: item.color,
      offsetCol: offset.col,
      offsetRow: offset.row,
    }
  })
})

const visibleItemPixels = computed(() => {
  const result: Array<{ x: number; y: number; w: number; h: number; fill: string }> = []
  const s = SCALE.value
  const equipped = props.equippedItems || 0
  const apiItems = props.items || []
  for (let idx = 0; idx < itemGrids.value.length; idx++) {
    const itemId = apiItems[idx]?.id
    if (itemId === undefined) continue
    if ((BigInt(equipped) & (1n << BigInt(itemId - 1))) === 0n) continue
    const item = itemGrids.value[idx]
    for (let ri = 0; ri < item.grid.length; ri++) {
      for (let ci = 0; ci < item.grid[ri].length; ci++) {
        if (item.grid[ri][ci] === 0) continue
        const col = item.offsetCol + ci
        const row = item.offsetRow + ri
        result.push({
          x: col * s,
          y: row * s,
          w: s,
          h: s,
          fill: item.fill,
        })
      }
    }
  }
  return result
})

const idleFrame1 = [
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0],
  [0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0],
  [0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0],
  [0,0,0,1,1,2,2,1,1,2,2,1,1,0,0,0],
  [0,0,0,1,2,2,2,1,1,2,2,2,1,0,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0],
  [0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0],
  [0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
]

const idleFrame2 = [
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0],
  [0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0],
  [0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0],
  [0,0,0,1,1,2,2,3,3,2,2,1,1,0,0,0],
  [0,0,0,1,2,2,2,3,3,2,2,2,1,0,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0],
  [0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0],
  [0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
]

const happyFrames = [
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0],
  [0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0],
  [0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0],
  [0,0,0,1,1,2,4,4,4,2,2,1,1,0,0,0],
  [0,0,0,1,2,4,4,4,4,4,2,1,0,0,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0],
  [0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
]

const sleepFrames = [
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0],
  [0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0],
  [0,0,0,0,3,1,2,3,3,2,1,3,0,0,0,0],
  [0,0,0,1,1,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,0,1,2,2,2,2,2,2,2,2,1,0,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0],
  [0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0],
  [0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
]

const walkingFrame1 = [
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0],
  [0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0],
  [0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0],
  [0,0,0,1,1,2,2,1,1,2,2,1,1,0,0,0],
  [0,0,0,1,2,2,2,1,1,2,2,2,1,0,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,1,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [1,2,2,2,2,2,2,2,2,2,2,2,2,1,1,0],
  [0,2,2,2,2,2,2,2,2,2,2,2,2,2,1,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,0,0,1,1,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,0],
  [0,0,0,0,0,0,1,3,3,1,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
]

const walkingFrame2 = [
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0],
  [0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0],
  [0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0],
  [0,0,0,1,1,2,2,1,1,2,2,1,1,0,0,0],
  [0,0,0,1,2,2,2,1,1,2,2,2,1,0,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0],
  [0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
]

const pettingFrames = [
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,3,1,1,1,3,0,0,0,0,0,0],
  [0,0,0,0,3,1,2,2,2,1,3,0,0,0,0,0],
  [0,0,0,3,1,2,2,4,4,2,1,3,0,0,0,0],
  [0,0,1,1,2,2,4,4,4,4,2,1,1,0,0,0],
  [0,0,1,2,2,4,4,4,4,4,2,2,1,0,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0],
  [0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0],
  [0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
]

const pettingFrames2 = [
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0],
  [0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0],
  [0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0],
  [0,0,0,1,1,2,4,4,4,2,2,1,1,0,0,0],
  [0,0,0,1,2,4,4,4,4,4,2,1,0,0,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0],
  [0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0],
  [0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
]

const greetingFrames = [
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0],
  [0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0],
  [0,0,0,0,3,1,2,4,4,2,1,3,0,0,0,0],
  [0,0,0,1,1,2,4,4,4,4,2,1,1,0,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0],
  [0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0],
  [0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
]

const greetingFrames2 = [
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,3,1,1,1,3,0,0,0,0,0,0],
  [0,0,0,0,3,1,2,2,2,1,3,0,0,0,0,0],
  [0,0,0,3,1,2,4,4,4,2,1,3,0,0,0,0],
  [0,0,1,1,2,4,4,4,4,4,2,1,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0],
  [0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0],
  [0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
]

const greetingFrames3 = [
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0],
  [0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0],
  [0,0,0,0,3,1,2,4,4,2,1,3,0,0,0,0],
  [0,0,0,1,1,2,4,4,4,4,2,1,1,0,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0],
  [0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0],
  [0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0],
  [0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0],
  [0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0],
  [0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
]

const allFrames: Record<DogStatus, number[][][]> = {
  IDLE: [idleFrame1, idleFrame2],
  HAPPY: [happyFrames, happyFrames, happyFrames],
  PETTING: [pettingFrames, pettingFrames2, pettingFrames],
  NUZZLE: [pettingFrames, pettingFrames2, pettingFrames],
  GREETING: [greetingFrames, greetingFrames2, greetingFrames3, greetingFrames2],
  SLEEPING: [sleepFrames, sleepFrames],
  WALKING: [walkingFrame1, walkingFrame2, walkingFrame1, walkingFrame2],
  FOCUS: [idleFrame1, idleFrame2],
}

const currentFrameIndex = ref(0)
let animationTimer: ReturnType<typeof setInterval> | null = null
let touchTimer: ReturnType<typeof setTimeout> | null = null

const activityLevel = computed(() => {
  const emotionFactor = (props.emotion + 100) / 200
  const bondFactor = props.bond / 100
  return emotionFactor * 0.5 + bondFactor * 0.5
})

const currentFrame = computed(() => {
  if (props.status === 'FOCUS') {
    return [idleFrame1, idleFrame2][currentFrameIndex.value % 2]
  }

  const frames = allFrames[props.status]
  return frames[currentFrameIndex.value % frames.length]
})

function getX(col: number, row: number): number {
  let offset = 0
  const sep = separation.value

  if (row === 1 && (col === 6 || col === 9)) {
    offset = -sep * (col === 6 ? 0.8 : -0.8)
  } else if (row === 2 && (col === 5 || col === 10)) {
    offset = -sep * (col === 5 ? 0.6 : -0.6)
  } else if ((row === 3 || row === 4) && (col === 6 || col === 9)) {
    offset = -sep * (col === 6 ? 0.4 : -0.4)
  } else if (row === 4 && (col === 7 || col === 8)) {
    offset = sep * (col === 7 ? -0.3 : 0.3)
  } else if (row === 5 && (col === 7 || col === 8)) {
    offset = sep * (col === 7 ? -0.2 : 0.2)
  }

  return Math.trunc((col + offset) * SCALE.value)
}

function getY(row: number, col: number): number {
  let offset = 0
  const sep = separation.value

  if (row === 1 && (col === 6 || col === 9)) {
    offset = -sep * 1.2
  } else if (row === 2 && (col === 5 || col === 10)) {
    offset = -sep * 0.8
  } else if (row === 4 && col === 7) {
    offset = sep * 0.3
  } else if (row === 13 && col <= 3) {
    offset = sep * 0.5
  } else if (row === 14 && (col === 5 || col === 6 || col === 7 || col === 8 || col === 9)) {
    offset = sep * (col === 5 ? 1.2 : col === 6 ? 0.8 : col === 7 ? 0.3 : col === 8 ? 0.3 : 0.8)
  }

  return Math.trunc((row + offset) * SCALE.value)
}

function getColor(pixel: number): string {
  const colorMap: Record<number, string> = {
    1: '#8d6e63',
    2: '#ffffff',
    3: '#4e342e',
    4: '#ff5252',
  }
  return colorMap[pixel] || '#000000'
}

function drawCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const w = viewBoxSize.value
  ctx.clearRect(0, 0, w, w)
  ctx.imageSmoothingEnabled = false

  const s = SCALE.value
  const frame = currentFrame.value
  if (!frame) return

  for (let ri = 0; ri < frame.length; ri++) {
    for (let ci = 0; ci < frame[ri].length; ci++) {
      const pixel = frame[ri][ci]
      if (pixel === 0) continue
      ctx.fillStyle = getColor(pixel)
      ctx.fillRect(getX(ci, ri), getY(ri, ci), s, s)
    }
  }

  for (const px of visibleItemPixels.value) {
    ctx.fillStyle = px.fill
    ctx.fillRect(px.x, px.y, px.w, px.h)
  }
}

function onClick() {
  if (props.status === 'SLEEPING') return
  emit('pet')
}

function onTouchStart() {
  if (props.status === 'SLEEPING') return
  touchTimer = setTimeout(() => {
    emit('pet')
  }, 150)
}

function onTouchEnd() {
  if (touchTimer) {
    clearTimeout(touchTimer)
    touchTimer = null
  }
}

const animationSpeed = computed(() => {
  const baseSpeed = 800
  const activity = activityLevel.value
  return baseSpeed * (1 - activity * 0.6)
})

watch([currentFrame, visibleItemPixels, viewBoxSize], () => {
  drawCanvas()
})

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  animationTimer = setInterval(() => {
    currentFrameIndex.value++
  }, animationSpeed.value)

  nextTick(() => drawCanvas())

  const parent = spriteRef.value?.parentElement
  if (parent) {
    containerSize.value = { width: parent.clientWidth, height: parent.clientHeight }
    resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        containerSize.value = {
          width: entry.contentBoxSize?.[0]?.inlineSize ?? entry.contentRect.width,
          height: entry.contentBoxSize?.[0]?.blockSize ?? entry.contentRect.height,
        }
      }
    })
    resizeObserver.observe(parent)
  }

  startMoving()
})

onUnmounted(() => {
  if (animationTimer) {
    clearInterval(animationTimer)
  }
  if (resizeObserver) {
    resizeObserver.disconnect()
  }
  stopMoving()
})
</script>

<style scoped lang="scss">
.pixel-dog-mobile-sprite {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.3s ease-out;
  -webkit-tap-highlight-color: transparent;

  &--sleeping {
    cursor: not-allowed;
    opacity: 0.8;
  }

  &--face-left &__canvas {
    transform: scaleX(-1);
  }

  &--face-right &__canvas {
    transform: scaleX(1);
  }

  &--face-back &__canvas {
    transform: scaleX(1);
  }

  &--face-front &__canvas {
    transform: scaleX(1);
  }

  &__canvas {
    padding: 12px;
    background: transparent;
    border: none;
    box-shadow: none;

    .pixel-dog-mobile-sprite--active & {
      animation: dog-wag 0.3s ease-in-out infinite;
    }

    .pixel-dog-mobile-sprite--lazy & {
      animation: dog-sway 2s ease-in-out infinite;
    }

    .pixel-dog-mobile-sprite--focus & {
      animation: dog-focus-wag 0.5s ease-in-out infinite;
    }
  }

  &__canvas-el {
    image-rendering: pixelated;
    display: block;
  }

  &__sparkles {
    position: absolute;
    top: -6px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    gap: 6px;
    animation: sparkle 0.5s ease-in-out infinite alternate;
  }

  &__sparkle {
    font-size: 14px;
    animation: float 0.6s ease-in-out infinite;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }

  &__hearts {
    position: absolute;
    top: -10px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    gap: 4px;
    animation: heart-bounce 0.5s ease-in-out infinite;
  }

  &__heart {
    font-size: 16px;
    animation: float-up 0.8s ease-out infinite;

    &:nth-child(1) { animation-delay: 0s; transform: rotate(-15deg); }
    &:nth-child(2) { animation-delay: 0.2s; font-size: 18px; }
    &:nth-child(3) { animation-delay: 0.4s; transform: rotate(15deg); }
  }

  &__paws {
    position: absolute;
    top: -12px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    gap: 4px;
    animation: paw-bounce 0.4s ease-in-out infinite;
  }

  &__paw {
    font-size: 16px;
    animation: paw-float 0.7s ease-out infinite;

    &:nth-child(1) { animation-delay: 0s; transform: rotate(-20deg); }
    &:nth-child(2) { animation-delay: 0.15s; font-size: 18px; }
    &:nth-child(3) { animation-delay: 0.3s; transform: rotate(20deg); }
  }

  &__waves {
    position: absolute;
    top: -15px;
    right: -5px;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__wave {
    font-size: 20px;
    animation: wave-hand 0.6s ease-in-out infinite;

    &:nth-child(1) { animation-delay: 0s; }
    &:nth-child(2) { animation-delay: 0.3s; opacity: 0.7; }
  }

  &__z {
    position: absolute;
    top: -10px;
    right: 15px;
    display: flex;
    flex-direction: column;
    gap: 2px;
    animation: float-down 2s ease-in-out infinite;

    span {
      font-size: 16px;
      font-weight: bold;
      color: #ffffff;
      opacity: 0.7;

      &:nth-child(2) {
        font-size: 12px;
        transform: translateX(8px);
      }

      &:nth-child(3) {
        font-size: 14px;
        transform: translateX(4px);
      }
    }
  }
}

@keyframes dog-wag {
  0%, 100% { transform: rotate(-2deg); }
  50% { transform: rotate(2deg); }
}

@keyframes dog-sway {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-3px); }
  75% { transform: translateX(3px); }
}

@keyframes dog-focus-wag {
  0%, 100% { transform: rotate(-1deg); }
  50% { transform: rotate(1deg); }
}

@keyframes sparkle {
  0% { transform: translateX(-50%) scale(1); }
  100% { transform: translateX(-50%) scale(1.2); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

@keyframes float-down {
  0%, 100% { transform: translateY(0); opacity: 0.7; }
  50% { transform: translateY(8px); opacity: 0.4; }
}

@keyframes heart-bounce {
  0%, 100% { transform: translateX(-50%) scale(1); }
  50% { transform: translateX(-50%) scale(1.1); }
}

@keyframes paw-bounce {
  0%, 100% { transform: translateX(-50%) scale(1) rotate(-3deg); }
  50% { transform: translateX(-50%) scale(1.15) rotate(3deg); }
}

@keyframes float-up {
  0% { transform: translateY(0) rotate(var(--rotation, 0deg)); opacity: 1; }
  100% { transform: translateY(-24px) rotate(var(--rotation, 0deg)); opacity: 0; }
}

@keyframes paw-float {
  0% { transform: translateY(0) scale(1); opacity: 1; }
  50% { transform: translateY(-10px) scale(1.1); }
  100% { transform: translateY(-20px) scale(0.9); opacity: 0; }
}

@keyframes wave-hand {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(30deg); }
  75% { transform: rotate(-30deg); }
}
</style>
