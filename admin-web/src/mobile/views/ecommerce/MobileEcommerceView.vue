<template>
  <!-- 移动端电商概览页容器：加载状态 + 顶部头部 + 功能模块网格 -->
  <div v-loading="ec.loading.value" class="mec-shell mh-scheme-a">
    <!-- 顶部英雄头部区域：展示电商概览统计数据 -->
    <EcHeroHeader />
    <!-- 内容区域：电商各功能模块入口网格 -->
    <div class="mec-content">
      <EcModuleGrid />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 移动端电商概览视图组件
 * 功能说明：
 * - 电商模块的移动端入口页面
 * - 顶部展示电商核心数据统计（英雄头部）
 * - 下方展示各功能模块入口网格（订单、商品、库存、快递等）
 * - 提供电商模块上下文给子组件使用
 */
import {onMounted, provide} from 'vue'
import {MOBILE_ECOMMERCE_KEY} from '@/mobile/views/ecommerce/mobileEcommerceContext'
import {useMobileEcommerce} from '@/mobile/views/ecommerce/useMobileEcommerce'
import EcHeroHeader from '@/mobile/views/ecommerce/components/EcHeroHeader.vue'
import EcModuleGrid from '@/mobile/views/ecommerce/components/EcModuleGrid.vue'

defineOptions({ name: 'MobileEcommerceView' })

const ec = useMobileEcommerce() // 电商业务逻辑组合函数
provide(MOBILE_ECOMMERCE_KEY, ec) // 向下提供电商上下文

onMounted(() => {
  void ec.init() // 组件挂载时初始化电商数据
})
</script>

<style lang="scss">
@use '@/mobile/views/home/themes/scheme-a/scheme-a.scss';
@use '@/mobile/views/ecommerce/styles/mobile-ecommerce.scss';
</style>
