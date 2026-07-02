<template>
  <div class="carton-category-tabs">
    <MobileDoodleChip
      v-for="cat in categories"
      :key="cat.id"
      tag="button"
      type="button"
      shape="pill"
      :color="activeCategory === cat.id ? '#2563eb' : '#2563eb'"
      :seed="cat.id.charCodeAt(0)"
      :filled="activeCategory === cat.id"
      fill-color="#2563eb"
      class="carton-category-tab"
      :class="{ active: activeCategory === cat.id }"
      @click="$emit('update:activeCategory', cat.id)"
    >
      <span class="carton-category-tab__icon">{{ cat.icon }}</span>
      <span class="carton-category-tab__name">{{ cat.name }}</span>
      <span v-if="cat.count > 0" class="carton-category-tab__badge">{{ cat.count }}</span>
    </MobileDoodleChip>
  </div>
</template>

<script setup lang="ts">
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import type { CartonCategory } from '../useMobileCarton'

defineProps<{
  categories: Array<{
    id: CartonCategory
    name: string
    icon: string
    count: number
  }>
  activeCategory: CartonCategory
}>()

defineEmits<{
  'update:activeCategory': [value: CartonCategory]
}>()
</script>

<style scoped lang="scss">
.carton-category-tabs {
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

.carton-category-tab {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: transform 0.2s ease;
  background: #fff;
  color: #2563eb;

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
    color: #fff;

    .carton-category-tab__name {
      color: #991b1b;
    }
  }
}

.carton-category-tab__icon {
  font-size: 16px;
}

.carton-category-tab__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #2563eb;
}

.carton-category-tab__badge {
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
