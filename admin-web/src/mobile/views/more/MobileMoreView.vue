<template>
  <!-- 移动端更多功能页主容器 -->
  <div class="mobile-page">
    <!-- 功能菜单列表：各项功能入口 -->
    <section
      v-for="item in menuItems"
      :key="item.path"
      class="mobile-card mobile-more__item"
      @click="go(item.path)"
    >
      <!-- 菜单项：图标 + 标题描述 + 箭头 -->
      <div class="mobile-list-item">
        <el-icon :size="22"><component :is="item.icon" /></el-icon>
        <div class="mobile-list-item__body">
          <div class="mobile-list-item__title">{{ t(item.labelKey) }}</div>
          <div v-if="item.descKey" class="mobile-list-item__meta">{{ t(item.descKey) }}</div>
        </div>
        <el-icon><ArrowRight /></el-icon>
      </div>
    </section>

    <!-- 打开桌面版按钮：切换到PC端界面 -->
    <el-button class="mobile-more__desktop" @click="openDesktop">
      {{ t('mobile.more.openDesktop') }}
    </el-button>
    <!-- 重置外壳按钮：清除设备外壳偏好设置 -->
    <el-button link type="primary" @click="resetShell">
      {{ t('mobile.more.resetShell') }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
/**
 * 移动端更多功能视图组件
 * 功能说明：
 * - 移动端更多功能入口页面
 * - 展示功能菜单列表（功能中心、电商、设置、用户管理）
 * - 提供切换到桌面版的入口
 * - 提供重置设备外壳偏好的功能
 * - 使用国际化多语言支持
 */
import type { Component } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowRight,
  Grid,
  Setting,
  ShoppingCart,
  User,
} from '@element-plus/icons-vue'
import { clearAppShellPreference, setAppShellPreference } from '@/utils/deviceShell'

const router = useRouter() // 路由实例
const { t } = useI18n() // 国际化翻译函数

// 更多菜单项接口定义
interface MoreMenuItem {
  path: string
  icon: Component
  labelKey: string
  descKey?: string
}

// 菜单项配置数据
const menuItems: MoreMenuItem[] = [
  {
    path: '/functions',
    icon: Grid,
    labelKey: 'mobile.more.functions',
    descKey: 'functions.subtitle',
  },
  {
    path: '/ecommerce',
    icon: ShoppingCart,
    labelKey: 'mobile.more.ecommerce',
    descKey: 'functions.items.ecommerce.desc',
  },
  {
    path: '/settings',
    icon: Setting,
    labelKey: 'mobile.more.settings',
  },
  {
    path: '/users',
    icon: User,
    labelKey: 'mobile.more.users',
  },
]

// 跳转到指定路由
function go(path: string) {
  router.push(path)
}

// 打开桌面版：设置外壳偏好为PC并跳转
function openDesktop() {
  setAppShellPreference('pc')
  const base = import.meta.env.BASE_URL || '/'
  window.location.href = `${base}index.html`
}

// 重置外壳：清除外壳偏好设置并刷新页面
function resetShell() {
  clearAppShellPreference()
  window.location.href = `${import.meta.env.BASE_URL || '/'}index.html`
}
</script>

<style scoped lang="scss">
.mobile-more__item {
  cursor: pointer;
  padding: 4px 12px;
}

.mobile-more__item .mobile-list-item {
  padding: 10px 0;
}

.mobile-more__desktop {
  width: 100%;
}
</style>