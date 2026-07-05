<template>
  <div class="mobile-ec-module" :class="{ 'mobile-ec-doodle-shell': doodle.enabled.value }">
    <EcommerceModuleView />
  </div>
</template>

<script setup lang="ts">
import EcommerceModuleView from '@/views/ecommerce/EcommerceModuleView.vue'
import { useMobileEcDoodle } from '@/composables/useMobileEcDoodle'

const doodle = useMobileEcDoodle()
</script>

<style scoped lang="scss">
@use '@/mobile/views/ecommerce/styles/ec-doodle-panel.scss';

.mobile-ec-module {
  margin: -8px -12px 0;
  min-height: 100%;
}

.mobile-ec-module.mobile-ec-doodle-shell {
  background: #fff;
}

.mobile-ec-module :deep(.ec-module-page__panel) {
  border-radius: 0;
  box-shadow: none;
}
</style>
