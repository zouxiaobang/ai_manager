<template>
  <div class="mobile-ecommerce">
    <EcommerceView />
  </div>
</template>

<script setup lang="ts">
import EcommerceView from '@/views/EcommerceView.vue'
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
