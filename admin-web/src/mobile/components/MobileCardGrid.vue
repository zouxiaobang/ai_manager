<template>
  <div class="mobile-card-grid" :class="classList">
    <template v-for="item in items" :key="item.id">
      <slot
        name="card"
        :item="item"
        :selected="isSelected(item.id)"
        :pressed="pressedId === item.id"
        :select="() => handleSelect(item)"
      />
    </template>

    <div v-if="items.length === 0" class="mobile-card-grid__empty">
      <slot name="empty">
        <img :src="schemeAAssets.starBlueOutline" class="mobile-card-grid__empty-icon" alt="" />
        <p class="mobile-card-grid__empty-text">{{ emptyText }}</p>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'

interface GridItem {
  id: string | number
}

const pressedId = ref<string | number | null>(null)

const props = withDefaults(
  defineProps<{
    items: GridItem[]
    emptyText?: string
    class?: string
    columns?: number
    selectable?: boolean
    modelValue?: string | number | null
  }>(),
  {
    emptyText: '暂无数据',
    columns: 2,
    selectable: false,
    modelValue: null,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string | number | null]
  select: [item: GridItem]
}>()

const internalSelectedId = ref<string | number | null>(null)

watch(
  () => props.modelValue,
  (val) => {
    internalSelectedId.value = val
  },
  { immediate: true },
)

const classList = computed(() => ({
  [props.class as string]: !!props.class,
  [`mobile-card-grid--${props.columns}-cols`]: props.columns !== 2,
  'mobile-card-grid--selectable': props.selectable,
}))

function isSelected(id: string | number): boolean {
  return props.selectable && internalSelectedId.value === id
}

function handleSelect(item: GridItem) {
  if (!props.selectable) return
  if (internalSelectedId.value === item.id) {
    internalSelectedId.value = null
    emit('update:modelValue', null)
  } else {
    internalSelectedId.value = item.id
    emit('update:modelValue', item.id)
  }
  emit('select', item)
}
</script>

<style scoped lang="scss">
.mobile-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.mobile-card-grid--3-cols {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.mobile-card-grid--4-cols {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.mobile-card-grid :deep(button),
.mobile-card-grid :deep(a) {
  -webkit-tap-highlight-color: transparent;
  -webkit-user-select: none;
  user-select: none;
}

.mobile-card-grid--selectable :deep(.mobile-card-grid__card) {
  position: relative;

  &.is-selected {
    :deep(.sa-doodle-frame__body) {
      background-color: rgba(37, 99, 235, 0.1);
    }

    &::after {
      content: '';
      position: absolute;
      top: 4px;
      right: 4px;
      width: 16px;
      height: 16px;
      background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%232563eb' stroke='%232563eb' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='20 6 9 17 4 12'%3E%3C/polyline%3E%3C/svg%3E")
        no-repeat center/contain;
      z-index: 10;
    }
  }
}

.mobile-card-grid__empty {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  gap: 12px;
}

.mobile-card-grid__empty-icon {
  width: 48px;
  height: 48px;
  opacity: 0.5;
}

.mobile-card-grid__empty-text {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
}
</style>
