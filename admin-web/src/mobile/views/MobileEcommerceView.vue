<template>
  <div class="mobile-ecommerce">
    <EcommerceTabsView />
  </div>
</template>

<script setup lang="ts">
import EcommerceTabsView from '@/views/ecommerce/EcommerceTabsView.vue'
</script>

<style scoped lang="scss">
.mobile-ecommerce {
  margin: -8px -12px 0;
  overflow-x: auto;
}

.mobile-ecommerce :deep(.ecommerce-page__title) {
  display: none;
}

.mobile-ecommerce :deep(.el-tabs__nav-wrap) {
  overflow-x: auto;
}
</style>
