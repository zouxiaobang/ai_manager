<template>
  <span
    ref="rootRef"
    class="auto-fit-cny"
    :class="toneClass"
    :style="{ fontSize: `${fontSize}px` }"
  >
    <template v-for="(part, index) in parts" :key="index">
      <span v-if="part.kind === 'symbol'" class="auto-fit-cny__symbol">{{ part.text }}</span>
      <span v-else-if="part.kind === 'unit'" class="auto-fit-cny__quantifier">{{ part.text }}</span>
      <span v-else class="auto-fit-cny__digits">{{ part.text }}</span>
    </template>
  </span>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { buildCnyAmountParts } from '@/utils/formatMoney'

const props = withDefaults(
  defineProps<{
    value?: number | null
    max?: number
    min?: number
    toneClass?: string
    symbol?: boolean
    fractionDigits?: number
  }>(),
  {
    max: 28,
    min: 14,
    toneClass: '',
    symbol: true,
    fractionDigits: 2,
  },
)

const rootRef = ref<HTMLElement | null>(null)
const fontSize = ref(props.max)

const parts = computed(() => {
  if (props.value == null || Number.isNaN(props.value)) {
    return [{ kind: 'digits' as const, text: '—' }]
  }
  return buildCnyAmountParts(Number(props.value), {
    symbol: props.symbol,
    fractionDigits: props.fractionDigits,
  }) ?? [{ kind: 'digits' as const, text: '—' }]
})

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
  () => [props.value, props.symbol, props.max, props.min, props.fractionDigits] as const,
  () => {
    void fit()
  },
)
</script>

<style scoped lang="scss">
.auto-fit-cny {
  display: inline-flex;
  align-items: flex-end;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  font-weight: 700;
  line-height: 1.15;
}

.auto-fit-cny__quantifier {
  font-size: max(0.62em, 11px);
  font-weight: 500;
  color: var(--el-text-color-placeholder);
  align-self: flex-end;
  line-height: 1;
  margin: 0 0.04em;
}
</style>
