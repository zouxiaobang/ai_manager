<template>
  <div class="mobile-page">
    <section class="mobile-card">
      <SettingsView />
    </section>
  </div>
</template>

<script setup lang="ts">
import SettingsView from '@/views/SettingsView.vue'
</script>

<style scoped lang="scss">
.mobile-card :deep(.dashboard-section-title) {
  margin-top: 0;
}

.mobile-card :deep(.el-form-item__label) {
  width: 88px !important;
}
</style>
