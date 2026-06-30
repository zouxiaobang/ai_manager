<template>
  <span
    ref="rootRef"
    class="auto-fit-one-line"
    :class="props.toneClass"
    :style="{ fontSize: `${fontSize}px` }"
  >
    {{ text }}<span v-if="unit" class="auto-fit-one-line__unit">{{ unit }}</span>
  </span>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    text: string
    unit?: string
    max?: number
    min?: number
    toneClass?: string
  }>(),
  {
    unit: '',
    max: 28,
    min: 14,
    toneClass: '',
  },
)

const rootRef = ref<HTMLElement | null>(null)
const fontSize = ref(props.max)

async function fit() {
  const el = rootRef.value
  if (!el) return

  let size = props.max
  fontSize.value = size
  await nextTick()

  while (size > props.min && el.scrollWidth > el.clientWidth) {
    size -= 1
    fontSize.value = size
    await nextTick()
  }
}

let resizeObserver: ResizeObserver | null = null

onMounted(async () => {
  await fit()
  resizeObserver = new ResizeObserver(() => {
    void fit()
  })
  if (rootRef.value?.parentElement) {
    resizeObserver.observe(rootRef.value.parentElement)
  }
})

onUnmounted(() => {
  resizeObserver?.disconnect()
})

watch(
  () => [props.text, props.unit, props.max, props.min] as const,
  () => {
    void fit()
  },
)
</script>

<style scoped lang="scss">
.auto-fit-one-line {
  display: block;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  font-weight: 700;
  line-height: 1.15;
}

.auto-fit-one-line__unit {
  margin-left: 4px;
  font-size: 0.55em;
  font-weight: 600;
}
</style>
