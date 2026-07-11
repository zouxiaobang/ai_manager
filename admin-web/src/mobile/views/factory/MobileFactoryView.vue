<template>
  <!-- 移动端工厂管理页主容器 -->
  <div v-loading="factory.loading.value" class="mfc-shell mh-scheme-a">
    <!-- 顶部海报头部区域：工厂概览统计 -->
    <FactoryPosterHeader />

    <!-- 主内容区域：搜索 + 类型筛选 + 工厂列表 -->
    <main class="mfc-content">
      <!-- 搜索框：搜索工厂名称 -->
      <MobileDoodleSearch
          v-model="factory.searchQuery.value"
          :placeholder="factory.t('ecommerce.factory.searchPlaceholder')"
      />
      <!-- 工厂类型筛选器：按类型筛选工厂 -->
      <FactoryTypeFilter />
      <!-- 工厂列表区域 -->
      <FactoryList />
    </main>

    <!-- 悬浮添加按钮：快速创建新工厂 -->
    <div class="mfc-fab">
      <button
        type="button"
        class="mfc-fab__btn"
        :aria-label="factory.t('ecommerce.factory.add')"
        @click="factory.openCreate()"
      >
        <span class="mfc-fab__icon">+</span>
        <span class="mfc-fab__label">{{ factory.t('mobile.factory.createShort') }}</span>
      </button>
    </div>

    <!-- 工厂表单底部弹窗：创建/编辑工厂 -->
    <FactoryFormSheet />
    <!-- 工厂删除确认弹窗 -->
    <FactoryDeleteConfirm />
  </div>
</template>

<script setup lang="ts">
/**
 * 移动端工厂管理视图组件
 * 功能说明：
 * - 工厂管理的移动端入口页面
 * - 顶部海报头部展示工厂概览统计
 * - 提供工厂搜索和类型筛选功能
 * - 工厂列表展示，支持悬浮按钮快速创建
 * - 支持工厂的创建、编辑、删除操作
 * - 使用上下文模式向下提供工厂数据
 * - 手绘风格UI设计
 */
import { onMounted, provide } from 'vue'
import { MOBILE_FACTORY_KEY } from '@/mobile/views/factory/factoryContext'
import { useMobileFactory } from '@/mobile/views/factory/useMobileFactory'
import FactoryPosterHeader from '@/mobile/views/factory/components/FactoryPosterHeader.vue'
import FactoryTypeFilter from '@/mobile/views/factory/components/FactoryTypeFilter.vue'
import FactoryList from '@/mobile/views/factory/components/FactoryList.vue'
import FactoryFormSheet from '@/mobile/views/factory/components/FactoryFormSheet.vue'
import FactoryDeleteConfirm from '@/mobile/views/factory/components/FactoryDeleteConfirm.vue'
import MobileDoodleSearch from "@/mobile/components/MobileDoodleSearch.vue";

const factory = useMobileFactory() // 工厂业务逻辑组合函数
provide(MOBILE_FACTORY_KEY, factory) // 向下提供工厂上下文

onMounted(() => {
  void factory.init() // 组件挂载时初始化工厂数据
})
</script>

<style lang="scss">
@use '@/mobile/views/home/themes/scheme-a/scheme-a.scss';
@use '@/mobile/views/factory/styles/mobile-factory.scss';
</style>
