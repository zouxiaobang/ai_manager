<template>
  <div
    class="pixel-dog-sprite"
    :class="[`pixel-dog-sprite--${status.toLowerCase()}`, { 'pixel-dog-sprite--active': activityLevel > 0.6, 'pixel-dog-sprite--lazy': activityLevel < 0.3, 'pixel-dog-sprite--focus': status === 'FOCUS' }]"
    @click="onClick"
    :style="{ 
      transform: `translate(${position.x}px, ${position.y}px) scaleX(${direction === 'LEFT' ? -1 : 1}) ${status === 'PETTING' ? 'rotate(-5deg)' : status === 'GREETING' ? 'rotate(5deg)' : ''}`,

      animation: isMoving ? `dog-idle ${1.5 - activityLevel * 0.5}s ease-in-out infinite` : 'none'
    }"
  >
    <div class="pixel-dog-sprite__canvas">
      <canvas
        ref="canvasRef"
        class="pixel-dog-sprite__canvas-el"
        :width="viewBoxSize"
        :height="viewBoxSize"
        :style="{ width: displaySize + 'px', height: displaySize + 'px' }"
      ></canvas>
    </div>

    <div v-if="status === 'HAPPY'" class="pixel-dog-sprite__sparkles">
    <span class="pixel-dog-sprite__sparkle">✨</span>
    <span class="pixel-dog-sprite__sparkle">✨</span>
    <span class="pixel-dog-sprite__sparkle">✨</span>
  </div>

  <div v-if="status === 'PETTING'" class="pixel-dog-sprite__hearts">
    <span class="pixel-dog-sprite__heart">❤️</span>
    <span class="pixel-dog-sprite__heart">💕</span>
    <span class="pixel-dog-sprite__heart">❤️</span>
  </div>

  <div v-if="status === 'GREETING'" class="pixel-dog-sprite__waves">
    <span class="pixel-dog-sprite__wave">👋</span>
    <span class="pixel-dog-sprite__wave">✋</span>
  </div>

  <div v-if="status === 'SLEEPING'" class="pixel-dog-sprite__z">
    <span>Z</span>
    <span>z</span>
    <span>Z</span>
  </div>

    <div class="pixel-dog-sprite__hint">点击互动</div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ITEM_SHAPES, SHAPE_OFFSETS } from '@/data/pixel-dog-items'
import type { PixelDogItemVO } from '@/api/pixelDog'

type DogStatus = 'IDLE' | 'HAPPY' | 'PETTING' | 'GREETING' | 'SLEEPING' | 'WALKING' | 'FOCUS'
type DogDirection = 'FRONT' | 'BACK' | 'LEFT' | 'RIGHT'

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
  greet: []
}>()

const SIZE = 16
const BASE_SCALE = 4  // 与 ESP 端一致（ESP: pixel_scale = (int)(4 * scale)）
const DISPLAY_SCALE = 2  // CSS 显示放大倍数（整数倍，保持像素清晰）

const level = computed(() => props.level || 1)

const levelScale = computed(() => {
  return Math.min(1 + (level.value - 1) * 0.08, 2)
})

// 与 ESP 端完全一致：Math.trunc 截断，限制 2-12
const SCALE = computed(() => {
  const s = Math.trunc(BASE_SCALE * levelScale.value)
  return Math.max(2, Math.min(12, s))
})

const viewBoxSize = computed(() => SIZE * SCALE.value)
const displaySize = computed(() => viewBoxSize.value * DISPLAY_SCALE)

const separation = computed(() => {
  return Math.min((level.value - 1) * 0.5, 4)
})

const position = ref({ x: 0, y: 0 })
const direction = ref<DogDirection>('FRONT')
const speed = ref(2)

const canvasRef = ref<HTMLCanvasElement | null>(null)

const bounds = computed(() => {
  const baseLeft = -200
  const baseRight = 200
  const baseTop = -120
  const baseBottom = 120
  
  const bondFactor = props.bond / 100
  
  const minBounds = 0.3
  const maxBounds = 1.2
  
  const scale = minBounds + (maxBounds - minBounds) * bondFactor
  
  return {
    left: Math.round(baseLeft * scale),
    right: Math.round(baseRight * scale),
    top: Math.round(baseTop * scale),
    bottom: Math.round(baseBottom * scale),
  }
})

let moveTimer: ReturnType<typeof setInterval> | null = null
let directionTimer: ReturnType<typeof setInterval> | null = null

const colors: Record<number, string> = {
  1: '#8d6e63',
  2: '#ffffff',
  3: '#4e342e',
  4: '#ff5252',
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

const allFrames: Record<DogStatus, typeof idleFrame1[]> = {
  IDLE: [idleFrame1, idleFrame2],
  HAPPY: [happyFrames, happyFrames, happyFrames],
  PETTING: [pettingFrames, pettingFrames2, pettingFrames],
  GREETING: [greetingFrames, greetingFrames2, greetingFrames3, greetingFrames2],
  SLEEPING: [sleepFrames, sleepFrames],
  WALKING: [walkingFrame1, walkingFrame2, walkingFrame1, walkingFrame2],
  FOCUS: [idleFrame1, idleFrame2],
}

const currentFrameIndex = ref(0)
let animationTimer: ReturnType<typeof setInterval> | null = null

const isMoving = computed(() => {
  return props.status === 'IDLE'
})

const isFocus = computed(() => {
  return props.status === 'FOCUS'
})

const activityLevel = computed(() => {
  const emotionFactor = (props.emotion + 100) / 200
  const bondFactor = props.bond / 100
  return emotionFactor * 0.5 + bondFactor * 0.5
})

const currentFrame = computed(() => {
  if (isFocus.value) {
    return [idleFrame1, idleFrame2][currentFrameIndex.value % 2]
  }
  
  if (!isMoving.value) {
    if (activityLevel.value < 0.3) {
      return idleFrame1
    }
    return [idleFrame1, idleFrame2][currentFrameIndex.value % 2]
  }
  
  let frames = allFrames[props.status]
  
  if (props.status === 'IDLE' && isMoving.value) {
    frames = allFrames.WALKING
    
    if (activityLevel.value < 0.3) {
      return idleFrame1
    } else if (activityLevel.value < 0.6) {
      return [idleFrame1, walkingFrame1][currentFrameIndex.value % 2]
    }
  }
  
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
  return colors[pixel] || '#000000'
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

  // 绘制狗的像素（与 ESP 端 draw_dog_frame 一致）
  for (let ri = 0; ri < frame.length; ri++) {
    for (let ci = 0; ci < frame[ri].length; ci++) {
      const pixel = frame[ri][ci]
      if (pixel === 0) continue
      ctx.fillStyle = getColor(pixel)
      ctx.fillRect(getX(ci, ri), getY(ri, ci), s, s)
    }
  }

  // 绘制物品像素（与 ESP 端 draw_dog_items 一致）
  for (const px of visibleItemPixels.value) {
    ctx.fillStyle = px.fill
    ctx.fillRect(px.x, px.y, px.w, px.h)
  }
}

function onClick() {
  if (props.status === 'SLEEPING') return
  emit('pet')
}

function move() {
  if (props.status !== 'IDLE' || isFocus.value) return

  switch (direction.value) {
    case 'LEFT':
      position.value.x = Math.max(bounds.value.left, position.value.x - speed.value)
      break
    case 'RIGHT':
      position.value.x = Math.min(bounds.value.right, position.value.x + speed.value)
      break
    case 'FRONT':
      position.value.y = Math.max(bounds.value.top, position.value.y - speed.value)
      break
    case 'BACK':
      position.value.y = Math.min(bounds.value.bottom, position.value.y + speed.value)
      break
  }
}

function changeDirection() {
  if (props.status !== 'IDLE') return

  const directions: DogDirection[] = ['FRONT', 'BACK', 'LEFT', 'RIGHT']
  const randomDir = directions[Math.floor(Math.random() * directions.length)]
  
  if (randomDir === 'LEFT' && position.value.x <= bounds.value.left) {
    direction.value = 'RIGHT'
  } else if (randomDir === 'RIGHT' && position.value.x >= bounds.value.right) {
    direction.value = 'LEFT'
  } else if (randomDir === 'FRONT' && position.value.y <= bounds.value.top) {
    direction.value = 'BACK'
  } else if (randomDir === 'BACK' && position.value.y >= bounds.value.bottom) {
    direction.value = 'FRONT'
  } else {
    direction.value = randomDir
  }
}

const animationSpeed = computed(() => {
  const baseSpeed = 800
  const activity = activityLevel.value
  return baseSpeed * (1 - activity * 0.6)
})

watch(() => props.status, (newStatus) => {
  if (newStatus === 'FOCUS') {
    position.value.x = 0
    position.value.y = 0
  }
})

watch([currentFrame, visibleItemPixels, viewBoxSize], () => {
  drawCanvas()
})

onMounted(() => {
  animationTimer = setInterval(() => {
    currentFrameIndex.value++
  }, animationSpeed.value)

  moveTimer = setInterval(() => {
    move()
  }, 50)

  directionTimer = setInterval(() => {
    changeDirection()
  }, 2000)

  nextTick(() => drawCanvas())
})

onUnmounted(() => {
  if (animationTimer) {
    clearInterval(animationTimer)
  }
  if (moveTimer) {
    clearInterval(moveTimer)
  }
  if (directionTimer) {
    clearInterval(directionTimer)
  }
})
</script>

<style scoped lang="scss">
@use './pixel-dog.scss';
</style>