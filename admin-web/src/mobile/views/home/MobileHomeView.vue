<template>
  <!-- 移动端首页容器：加载状态指示 + 主题组件动态渲染 -->
  <div v-loading="home.loading.value" class="mobile-home-shell">
    <!-- 动态主题组件：根据用户设置渲染不同的首页主题 -->
    <component :is="themeComponent" />
  </div>
</template>

<script setup lang="ts">
/**
 * 移动端首页视图组件
 * 功能说明：
 * - 作为移动端首页的入口组件
 * - 根据用户设置的主题动态渲染不同的首页样式（A/B/C/D四种方案）
 * - 提供首页上下文数据给子组件使用
 * - 管理首页加载状态
 */
import { computed, provide } from 'vue'
import { useAppStore } from '@/stores/app'
import type { MobileHomeThemeId } from '@/data/mobile-home-themes'
import { MOBILE_HOME_KEY } from '@/mobile/views/home/mobileHomeContext'
import { useMobileHome } from '@/mobile/views/home/useMobileHome'
import ThemeA from '@/mobile/views/home/themes/ThemeA.vue'
import ThemeB from '@/mobile/views/home/themes/ThemeB.vue'
import ThemeC from '@/mobile/views/home/themes/ThemeC.vue'
import ThemeD from '@/mobile/views/home/themes/ThemeD.vue'

// 主题组件映射表：主题ID对应组件
const themeComponents: Record<MobileHomeThemeId, object> = {
  'scheme-a': ThemeA,
  'scheme-b': ThemeB,
  'scheme-c': ThemeC,
  'scheme-d': ThemeD,
}

const appStore = useAppStore() // 应用全局状态
const home = useMobileHome() // 首页业务逻辑组合函数

provide(MOBILE_HOME_KEY, home) // 向下提供首页上下文

// 当前主题组件计算属性：根据全局设置返回对应主题组件
const themeComponent = computed(() => themeComponents[appStore.mobileHomeTheme])
</script>

<style lang="scss">
@use '@/mobile/views/home/styles/home-shared.scss';

.mobile-home-shell {
  min-height: 100%;
}

html.is-mobile-shell[data-mobile-home-theme]:not([data-mobile-home-theme='scheme-a']) {
  .mobile-app__main {
    background: #faf8f5;
  }
}
</style>
