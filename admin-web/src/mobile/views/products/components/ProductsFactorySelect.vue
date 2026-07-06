<template>
  <div class="mpr-factory-select">
    <MobileDoodleChip
      v-for="opt in products.factoryOptions.value"
      :key="opt.value === 'all' ? 'all' : opt.value"
      tag="button"
      type="button"
      shape="pill"
      :color="isActive(opt.value) ? '#2563eb' : '#cbd5e1'"
      :filled="isActive(opt.value)"
      :seed="opt.value === 'all' ? 42 : (opt.value as number)"
      class="mpr-factory-select__chip"
      @click="products.setFactoryFilter(opt.value)"
    >
      <span class="mpr-factory-select__label">{{ opt.label }}</span>
      <span class="mpr-factory-select__count">{{ opt.productCount }}</span>
    </MobileDoodleChip>
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_PRODUCTS_KEY } from '../productsContext'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'

const products = inject(MOBILE_PRODUCTS_KEY)!

function isActive(value: number | 'all') {
  return products.selectedFactoryId.value === value
}
</script>

<style scoped lang="scss">
.mpr-factory-select {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 0 0 4px;
  scrollbar-width: none;
  margin-bottom: 8px;

  &::-webkit-scrollbar {
    display: none;
  }
}

.mpr-factory-select__chip {
  flex-shrink: 0;
  white-space: nowrap;
  font-family: 'ZCOOL KuaiLe', 'Microsoft YaHei', sans-serif;
  background: #faf8f5;

  :deep(.sa-doodle-frame__body) {
    padding: 10px 14px;
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

.mpr-factory-select__label {
  font-size: 13px;
}

.mpr-factory-select__count {
  font-size: 11px;
  font-weight: 700;
  opacity: 0.75;
}
</style>
