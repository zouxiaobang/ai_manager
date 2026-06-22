<template>
  <div class="mobile-app">
    <header class="mobile-app__header">
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
        <span
          v-if="tab.key === 'todos' && todayTodoCount > 0"
          class="mobile-app__tab-badge"
          :data-count="todayTodoCount > 99 ? '99+' : todayTodoCount"
        >
          <el-icon><component :is="tab.icon" /></el-icon>
        </span>
        <el-icon v-else><component :is="tab.icon" /></el-icon>
        <span>{{ t(tab.labelKey) }}</span>
      </button>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowLeft,
  HomeFilled,
  List,
  MoreFilled,
  Notebook,
  Timer,
} from '@element-plus/icons-vue'
import MobileTodoHeaderIcon from '@/mobile/components/MobileTodoHeaderIcon.vue'
import { formatMobileHeaderDate } from '@/mobile/utils/headerDate'
import { useTodoReminders } from '@/composables/useTodoReminders'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { todayTodoCount, refreshTodayCount } = useTodoReminders()

const headerDate = computed(() => formatMobileHeaderDate())

const tabs = [
  { key: 'home', path: '/home', icon: HomeFilled, labelKey: 'mobile.nav.home' },
  { key: 'notebook', path: '/notebook', icon: Notebook, labelKey: 'mobile.nav.notebook' },
  { key: 'todos', path: '/todos', icon: List, labelKey: 'mobile.nav.todos' },
  { key: 'pomodoro', path: '/pomodoro', icon: Timer, labelKey: 'mobile.nav.pomodoro' },
  { key: 'more', path: '/more', icon: MoreFilled, labelKey: 'mobile.nav.more' },
] as const

const showTabBar = computed(() => !route.meta.hideTabBar)
const showBack = computed(() => Boolean(route.meta.hideTabBar))
const activeTab = computed(() => (route.meta.tab as string) ?? '')

const pageTitle = computed(() => {
  const key = route.meta.titleKey as string | undefined
  return key ? t(key) : t('portal.title')
})

function goTab(path: string) {
  if (route.path !== path) {
    router.push(path)
  }
}

function goBack() {
  router.back()
}

function goTodos() {
  router.push('/todos')
}

watch(
  () => route.fullPath,
  () => {
    void refreshTodayCount()
  },
)
</script>
