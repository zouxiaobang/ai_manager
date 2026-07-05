<template>
  <div class="mobile-app" @touchstart="handleTouchStart" @touchmove="handleTouchMove">
    <header v-if="showAppHeader" class="mobile-app__header">
      <el-button
        v-if="showBack"
        class="mobile-app__back"
        link
        :icon="ArrowLeft"
        @click="goBack"
      />
      <div class="mobile-app__heading">
        <h1 class="mobile-app__title">{{ pageTitle }}</h1>
        <p class="mobile-app__date">{{ headerDate }}</p>
      </div>
      <div class="mobile-app__header-actions">
        <el-badge
          :value="todayTodoCount"
          :max="99"
          :hidden="todayTodoCount === 0"
        >
          <button
            type="button"
            class="mobile-app__todo-btn"
            :aria-label="t('mobile.header.todoEntry')"
            @click="goTodos"
          >
            <MobileTodoHeaderIcon />
          </button>
        </el-badge>
      </div>
    </header>

    <main
      class="mobile-app__main"
      :class="{ 'mobile-app__main--with-tabbar': showTabBar }"
    >
      <router-view />
    </main>

    <nav v-if="showTabBar" class="mobile-app__tabbar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        class="mobile-app__tab"
        :class="{ 'is-active': activeTab === tab.key }"
        @click="goTab(tab.path)"
      >
        <template v-if="useSchemeATabbar">
          <span class="mobile-app__tab-icon">
            <img :src="schemeATabIcon(tab.key)" alt="" />
          </span>
        </template>
        <template v-else>
          <span
            v-if="tab.key === 'todos' && todayTodoCount > 0"
            class="mobile-app__tab-badge"
            :data-count="todayTodoCount > 99 ? '99+' : todayTodoCount"
          >
            <el-icon><component :is="tab.icon" /></el-icon>
          </span>
          <el-icon v-else><component :is="tab.icon" /></el-icon>
        </template>
        <span>{{ t(tab.labelKey) }}</span>
      </button>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowLeft,
  HomeFilled,
  List,
  MoreFilled,
  Notebook,
} from '@element-plus/icons-vue'
import MobileTodoHeaderIcon from '@/mobile/components/MobileTodoHeaderIcon.vue'
import { formatMobileHeaderDate } from '@/mobile/utils/headerDate'
import { isMobileHomeRoute } from '@/mobile/utils/homeBackGuard'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'
import { useAppStore } from '@/stores/app'
import { useTodoReminders } from '@/composables/useTodoReminders'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const appStore = useAppStore()
const { todayTodoCount, refreshTodayCount } = useTodoReminders()

const useSchemeATabbar = computed(() => appStore.mobileHomeTheme === 'scheme-a')

const schemeATabIcons: Record<string, string> = {
  home: schemeAAssets.tabHome,
  notebook: schemeAAssets.tabNotebook,
  todos: schemeAAssets.tabTodos,
  more: schemeAAssets.tabMore,
}

function schemeATabIcon(key: string) {
  return schemeATabIcons[key] ?? schemeAAssets.tabMore
}

const headerDate = computed(() => formatMobileHeaderDate())

const tabs = [
  { key: 'home', path: '/home', icon: HomeFilled, labelKey: 'mobile.nav.home' },
  { key: 'notebook', path: '/notebook', icon: Notebook, labelKey: 'mobile.nav.notebook' },
  { key: 'todos', path: '/todos', icon: List, labelKey: 'mobile.nav.todos' },
  { key: 'more', path: '/more', icon: MoreFilled, labelKey: 'mobile.nav.more' },
] as const

const showTabBar = computed(() => !route.meta.hideTabBar)
const showBack = computed(
  () => Boolean(route.meta.hideTabBar) && !isMobileHomeRoute(route.name),
)
const showAppHeader = computed(
  () =>
  !(isMobileHomeRoute(route.name) && appStore.mobileHomeTheme === 'scheme-a')
    && route.meta.hideAppHeader !== true,
)
const activeTab = computed(() => (route.meta.tab as string) ?? '')

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

function goTodos() {
  router.push('/todos')
}

let touchStartX = 0
let touchStartY = 0

function handleTouchStart(e: TouchEvent) {
  touchStartX = e.touches[0].clientX
  touchStartY = e.touches[0].clientY
}

function handleTouchMove(e: TouchEvent) {
  if (!isMobileHomeRoute(route.name)) return
  const touchCurrentX = e.touches[0].clientX
  const touchCurrentY = e.touches[0].clientY
  const deltaX = touchCurrentX - touchStartX
  const deltaY = touchCurrentY - touchStartY
  if (Math.abs(deltaX) > Math.abs(deltaY) && deltaX > 50 && touchStartX < 100) {
    e.preventDefault()
  }
}

watch(
  () => route.fullPath,
  () => {
    void refreshTodayCount()
  },
)

function syncHomeRouteClass() {
  if (typeof document === 'undefined') return
  const onHome = isMobileHomeRoute(route.name)
  document.documentElement.classList.toggle('mobile-home-route', onHome)
}

onMounted(syncHomeRouteClass)
onUnmounted(() => {
  document.documentElement.classList.remove('mobile-home-route')
})
watch(() => route.name, syncHomeRouteClass)
</script>

