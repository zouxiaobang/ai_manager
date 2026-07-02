<template>
  <div class="pomo-ring">
    <svg
      class="pomo-ring__svg"
      :viewBox="`0 0 ${DESIGN_SIZE} ${DESIGN_SIZE}`"
      shape-rendering="crispEdges"
    >
      <g
        v-for="seg in segments"
        :key="seg.i"
        :transform="`rotate(${seg.angle} ${center} ${center})`"
      >
        <rect
          v-for="(px, pi) in seg.pixels"
          :key="pi"
          :x="px.x"
          :y="px.y"
          :width="PIXEL"
          :height="PIXEL"
          :fill="px.fill"
        />
      </g>
    </svg>
    <div class="pomo-ring__content">
      <img
        class="pomo-ring__tomato"
        src="/icons/pomodoro/tomato.png"
        alt=""
      >
      <span class="pomo-ring__clock">{{ clock }}</span>
      <span class="pomo-ring__status">{{ status }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    clock: string
    status: string
    percentage: number
    color?: string
  }>(),
  {
    color: '#ef5350',
  },
)

const DESIGN_SIZE = 240
const SEG_COUNT = 20
const PIXEL = 2
const OUTLINE = '#101028'
const TRACK = '#252545'
const TRACK_HI = '#32325a'

/** 单段像素造型：O 描边 / F 填充 / H 高光（阶梯锯齿边） */
const SEGMENT_ART = [
  '...OOO...',
  '..OFFF...',
  '.OFFFFO..',
  'OFFFFFFO.',
  'OFFHFHFFO',
  'OFFFFFFO.',
  'OFFFFFFO.',
  'OFFFFFFO.',
  'OFFFFFFO.',
  'OFFHFHFFO',
  'OFFFFFFO.',
  '.OFFFFO..',
  '..OFFF...',
  '...OOO...',
]

const center = DESIGN_SIZE / 2
const outerR = DESIGN_SIZE / 2 - 14

function brighten(hex: string, factor: number): string {
  const raw = hex.replace('#', '')
  const full = raw.length === 3 ? raw.split('').map((c) => c + c).join('') : raw
  const n = Number.parseInt(full, 16)
  const r = Math.min(255, Math.round(((n >> 16) & 255) * factor))
  const g = Math.min(255, Math.round(((n >> 8) & 255) * factor))
  const b = Math.min(255, Math.round((n & 255) * factor))
  return `rgb(${r} ${g} ${b})`
}

function buildSegmentPixels(isFilled: boolean) {
  const body = isFilled ? props.color : TRACK
  const hi = isFilled ? brighten(props.color, 1.18) : TRACK_HI
  const anchorX = center
  const anchorY = center - outerR
  const rowW = SEGMENT_ART[0].length
  const midCol = Math.floor(rowW / 2)

  const pixels: { x: number; y: number; fill: string }[] = []

  SEGMENT_ART.forEach((row, rowIdx) => {
    [...row].forEach((ch, colIdx) => {
      if (ch === '.') return
      let fill = body
      if (ch === 'O') fill = OUTLINE
      if (ch === 'H') fill = hi
      pixels.push({
        x: anchorX + (colIdx - midCol) * PIXEL,
        y: anchorY + rowIdx * PIXEL,
        fill,
      })
    })
  })

  return pixels
}

const segments = computed(() => {
  const pct = Math.min(100, Math.max(0, props.percentage))
  const filled = Math.round((pct / 100) * SEG_COUNT)
  const step = 360 / SEG_COUNT

  return Array.from({ length: SEG_COUNT }, (_, i) => ({
    i,
    angle: step * i,
    pixels: buildSegmentPixels(i < filled),
  }))
})
</script>

<style scoped lang="scss">
@use './pomo-pixel-mixins.scss' as jag;

.pomo-ring {
  position: relative;
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  aspect-ratio: 1;
  container-type: inline-size;
  image-rendering: pixelated;
}

.pomo-ring__svg {
  width: 100%;
  height: 100%;
  image-rendering: pixelated;
  shape-rendering: crispEdges;
}

.pomo-ring__content {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.2em;
  padding-top: 0.4em;
}

.pomo-ring__tomato {
  width: 15cqi;
  height: 15cqi;
  object-fit: contain;
  image-rendering: pixelated;
  image-rendering: crisp-edges;
  margin-bottom: 0.1em;
}

.pomo-ring__clock {
  @include jag.pomo-pixel-digits;
  font-size: 11cqi;
  color: #f0f4ff;
  text-shadow:
    2px 2px 0 #1a1a3a,
    0 0 0 #1a1a3a;
}

.pomo-ring__status {
  font-size: 4.5cqi;
  font-weight: 600;
  color: #90a4ae;
}
</style>
