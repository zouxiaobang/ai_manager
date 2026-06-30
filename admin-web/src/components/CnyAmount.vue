<template>
  <span v-if="parts" class="cny-amount" :class="variantClass">
    <template v-for="(part, index) in parts" :key="index">
      <span v-if="part.kind === 'symbol'" class="cny-amount__symbol">{{ part.text }}</span>
      <span v-else-if="part.kind === 'unit'" class="cny-amount__quantifier">{{ part.text }}</span>
      <span v-else class="cny-amount__digits">{{ part.text }}</span>
    </template>
  </span>
  <span v-else class="cny-amount is-empty">{{ emptyText }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { buildCnyAmountParts } from '@/utils/formatMoney'

const props = withDefaults(
  defineProps<{
    value?: number | null
    variant?: 'inline' | 'display'
    compact?: boolean
    symbol?: boolean
    signed?: boolean
    fractionDigits?: number
    emptyText?: string
  }>(),
  {
    variant: 'inline',
    symbol: true,
    signed: false,
    fractionDigits: 2,
    emptyText: '—',
  },
)

const variantClass = computed(() => `is-${props.variant}`)

const parts = computed(() => {
  if (props.value == null || Number.isNaN(props.value)) return null
  const num = Number(props.value)
  const baseParts = buildCnyAmountParts(num, {
    symbol: props.symbol,
    fractionDigits: props.fractionDigits,
    compact: props.compact,
  })
  if (!baseParts) return null
  if (!props.signed || num <= 0) return baseParts
  return [{ kind: 'symbol', text: '+' }, ...baseParts.filter((part) => part.kind !== 'symbol')]
})
</script>

<style scoped lang="scss">
.cny-amount {
  display: inline-flex;
  align-items: flex-end;
  max-width: 100%;
  white-space: nowrap;
  line-height: 1.2;
}

.cny-amount__quantifier {
  font-size: max(0.62em, 11px);
  font-weight: 500;
  color: var(--el-text-color-placeholder);
  align-self: flex-end;
  line-height: 1;
  margin: 0 0.04em;
}

.cny-amount.is-empty {
  color: inherit;
}
</style>
