<template>
  <div class="mfc-type-filter">
    <MobileDoodleChip
      v-for="opt in factory.filterOptions.value"
      :key="opt.value || 'all'"
      tag="button"
      type="button"
      shape="pill"
      :color="factory.typeFilter.value === opt.value ? '#8b5cf6' : '#cbd5e1'"
      :seed="opt.value ? opt.value.charCodeAt(0) : 0"
      :filled="factory.typeFilter.value === opt.value"
      fill-color="#8b5cf6"
      class="mfc-type-filter__chip"
      :class="{ 'is-active': factory.typeFilter.value === opt.value }"
      @click="factory.setTypeFilter(opt.value)"
    >
      {{ opt.label }}
    </MobileDoodleChip>
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import { MOBILE_FACTORY_KEY } from '../factoryContext'

const factory = inject(MOBILE_FACTORY_KEY)!
</script>

<style scoped lang="scss">
.mfc-type-filter {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.mfc-type-filter__chip {
  flex-shrink: 0;
  font-family: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.15s ease;
  color: #64748b;

  :deep(.sa-doodle-frame__body) {
    padding: 8px 16px;
  }

  &:active {
    transform: scale(0.96);
  }

  &.is-active {
    color: #8b0000;

    :deep(.sa-doodle-frame__body) {
      color: #8b0000;
    }
  }
}
</style>
