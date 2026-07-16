<template>
  <div class="mobile-v2-app">
    <header v-if="showAppHeader" class="mobile-v2-app__header">
      <el-button
        v-if="showBack"
        class="mobile-v2-app__back"
        link
        :icon="ArrowLeft"
        @click="goBack"
      />
      <div class="mobile-v2-app__heading">
        <h1 class="mobile-v2-app__title">{{ pageTitle }}</h1>
      </div>
      <div class="mobile-v2-app__header-actions">
        <button
          v-if="headerActionMeta"
          type="button"
          class="mobile-v2-app__todo-btn"
          :aria-label="t(headerActionMeta.ariaLabelKey)"
          @click="router.push(headerActionMeta.to)"
        >
          <el-icon :size="20"><component :is="headerActionIcon" /></el-icon>
        </button>
      </div>
    </header>

    <main class="mobile-v2-app__main">
      <router-view v-slot="{ Component }">
        <keep-alive :include="cachedComponents">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </main>

    <nav v-show="showTabBar" class="mobile-v2-app__tabbar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        class="mobile-v2-app__tab"
        :class="{ 'is-active': activeTab === tab.key }"
        @click="goTab(tab.path)"
      >
        <WarRoomSvgIcon group="nav" :name="tab.iconName" :size="22" class="mobile-v2-app__tab-svg" />
        <span>{{ t(tab.labelKey) }}</span>
      </button>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowLeft,
  DataAnalysis,
  Search,
} from '@element-plus/icons-vue'
import type { Component } from 'vue'
import WarRoomSvgIcon from '@/components/war-room/WarRoomSvgIcon.vue'

const headerActionIconMap: Record<string, Component> = { Search, DataAnalysis }

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const cachedComponents = ref<string[]>([])

const tabs = [
  { key: 'home', path: '/home', iconName: 'home' as const, labelKey: 'mobile.nav.home' },
  { key: 'notebook', path: '/notebook', iconName: 'notebook' as const, labelKey: 'mobile.nav.notebook' },
  { key: '24hour', path: '/24hour', iconName: '24hour' as const, labelKey: 'portal.menu.24hour' },
  { key: 'more', path: '/more', iconName: 'user-center' as const, labelKey: 'mobile.v2.mine' },
] as const

const showTabBar = computed(() => !route.meta.hideTabBar)
const showBack = computed(() => Boolean(route.meta.hideTabBar))
const showAppHeader = computed(() => route.meta.hideAppHeader !== true)
const activeTab = computed(() => (route.meta.tab as string) ?? '')

interface HeaderActionMeta {
  icon: string
  to: string
  ariaLabelKey: string
}

const headerActionMeta = computed(() => route.meta.headerAction as HeaderActionMeta | undefined)
const headerActionIcon = computed(() => {
  const meta = headerActionMeta.value
  return meta ? headerActionIconMap[meta.icon] : null
})

const pageTitle = computed(() => {
  const key = route.meta.titleKey as string | undefined
  return key ? t(key) : t('portal.title')
})

function goTab(path: string) {
  if (route.path === path) return
  if (path === '/home') {
    router.replace(path)
  } else {
    router.push(path)
  }
}

function goBack() {
  router.back()
}
</script>

<style scoped lang="scss">
.mobile-v2-app__todo-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--wr-stat-blue, #2563eb);
  cursor: pointer;

  &:active {
    opacity: 0.75;
  }
}

.mobile-v2-app__tab-svg {
  color: inherit;
}
</style>
