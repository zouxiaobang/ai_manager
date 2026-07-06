<template>
  <div class="mobile-category-tabs" :class="classList">
    <MobileDoodleChip
      v-for="cat in categories"
      :key="cat.id ?? ''"
      tag="button"
      type="button"
      shape="pill"
      :color="activeValue === cat.id ? activeColor : inactiveColor"
      :seed="resolveSeed(cat.id)"
      :filled="activeValue === cat.id"
      :fill-color="fillColor"
      class="mobile-category-tab"
      :class="{ active: activeValue === cat.id }"
      @click="handleCatClick(cat)"
    >
      <span v-if="cat.icon" class="mobile-category-tab__icon">{{ cat.icon }}</span>
      <span class="mobile-category-tab__name">{{ cat.name }}</span>
      <span v-if="cat.count != null && cat.count > 0" class="mobile-category-tab__badge">
        {{ cat.count }}
      </span>
    </MobileDoodleChip>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MobileDoodleChip from './MobileDoodleChip.vue'

export interface CategoryItem {
  id: string | number | null
  name: string
  icon?: string
  count?: number
}

const props = withDefaults(
  defineProps<{
    categories: CategoryItem[]
    activeValue: string | number | null
    activeColor?: string
    inactiveColor?: string
    fillColor?: string
    class?: string
  }>(),
  {
    activeColor: '#2563eb',
    inactiveColor: '#94a3b8',
    fillColor: '#2563eb',
  },
)

const emit = defineEmits<{
  'update:activeValue': [value: string | number]
}>()

function handleCatClick(cat: CategoryItem) {
  if (cat.id != null) {
    emit('update:activeValue', cat.id)
  }
}

function resolveSeed(id: string | number | null): number | undefined {
  if (id == null) return undefined
  return typeof id === 'string' ? id.charCodeAt(0) : id
}

const classList = computed(() => ({
  [props.class as string]: !!props.class,
}))
</script>

<style scoped lang="scss">
.mobile-category-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 12px;
  margin-bottom: 16px;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.mobile-category-tab {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: transform 0.2s ease;
  background: #fff;
  color: #94a3b8;

  :deep(.sa-doodle-frame__body) {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 16px;
  }

  &:active {
    transform: scale(0.95);
  }

  &.active {
    color: #991b1b;

    .mobile-category-tab__name {
      color: #991b1b;
    }
  }
}

.mobile-category-tab__icon {
  font-size: 16px;
}

.mobile-category-tab__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #94a3b8;
}

.mobile-category-tab__badge {
  font-size: 11px;
  font-weight: 800;
  color: #fff;
  background: #3b82f6;
  padding: 2px 6px;
  border-radius: 999px;
  min-width: 18px;
  text-align: center;

  .active & {
    background: #fbbf24;
    color: #78350f;
  }
}
</style>
