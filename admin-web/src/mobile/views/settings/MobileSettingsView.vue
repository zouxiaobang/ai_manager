<template>
  <!-- 移动端设置页主容器 -->
  <div class="mobile-page">
    <!-- 设置卡片区域：复用PC端设置组件 -->
    <section class="mobile-card">
      <SettingsView />
    </section>
  </div>
</template>

<script setup lang="ts">
/**
 * 移动端设置视图组件
 * 功能说明：
 * - 系统设置的移动端入口页面
 * - 复用PC端 SettingsView 组件，通过样式适配移动端显示
 * - 包含系统各项配置设置功能
 */
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