<template>
  <div class="mobile-page">
    <section
      v-for="item in menuItems"
      :key="item.path"
      class="mobile-card mobile-more__item"
      @click="go(item.path)"
    >
      <div class="mobile-list-item">
        <el-icon :size="22"><component :is="item.icon" /></el-icon>
        <div class="mobile-list-item__body">
          <div class="mobile-list-item__title">{{ t(item.labelKey) }}</div>
          <div v-if="item.descKey" class="mobile-list-item__meta">{{ t(item.descKey) }}</div>
        </div>
        <el-icon><ArrowRight /></el-icon>
      </div>
    </section>

    <el-button class="mobile-more__desktop" @click="openDesktop">
      {{ t('mobile.more.openDesktop') }}
    </el-button>
    <el-button link type="primary" @click="resetShell">
      {{ t('mobile.more.resetShell') }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
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

const router = useRouter()
const { t } = useI18n()

interface MoreMenuItem {
  path: string
  icon: Component
  labelKey: string
  descKey?: string
}

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

function go(path: string) {
  router.push(path)
}

function openDesktop() {
  setAppShellPreference('pc')
  const base = import.meta.env.BASE_URL || '/'
  window.location.href = `${base}index.html`
}

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
