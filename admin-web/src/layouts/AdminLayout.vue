<template>
  <div class="portal portal--war-room">
    <div class="portal-war-shell">
      <aside class="portal-rail">
        <router-link
          to="/settings"
          class="portal-rail__gear"
          :class="{ 'portal-rail__gear--active': isSettingsActive }"
          :title="t('portal.menu.settings')"
        >
          <WarRoomGearIcon />
        </router-link>
        <nav class="portal-rail__nav">
          <router-link
            v-for="item in railItems"
            :key="item.path"
            :to="item.path"
            class="portal-rail__item"
            :class="{ 'portal-rail__item--active': isRailActive(item.path) }"
          >
            <span class="portal-rail__item-icon">
              <WarRoomNavIcon :name="item.iconKey" />
            </span>
            <span class="portal-rail__item-label">{{ item.label }}</span>
          </router-link>
        </nav>
      </aside>
      <main class="portal-war-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useTodoReminders } from '@/composables/useTodoReminders'
import WarRoomGearIcon from '@/components/war-room/WarRoomGearIcon.vue'
import WarRoomNavIcon, { type WarRoomNavIconName } from '@/components/war-room/WarRoomNavIcon.vue'

const route = useRoute()
const { t } = useI18n()
const { refreshTodayCount } = useTodoReminders()

watch(
  () => route.fullPath,
  () => {
    void refreshTodayCount()
  },
)

const railItems = computed(() => [
  { path: '/home', iconKey: 'home' as WarRoomNavIconName, label: t('portal.menu.home') },
  { path: '/pomodoro', iconKey: 'pomodoro' as WarRoomNavIconName, label: t('portal.menu.pomodoro') },
  { path: '/ecommerce', iconKey: 'ecommerce' as WarRoomNavIconName, label: t('portal.menu.ecommerce') },
  { path: '/pixel-dog', iconKey: 'pixel-dog' as WarRoomNavIconName, label: t('portal.menu.pixelDog') },
  { path: '/notebook', iconKey: 'notebook' as WarRoomNavIconName, label: t('portal.menu.notebook') },
  { path: '/todos', iconKey: 'todos' as WarRoomNavIconName, label: t('portal.menu.todos') },
  { path: '/functions', iconKey: 'functions' as WarRoomNavIconName, label: t('portal.menu.functions') },
  { path: '/deploy-docs', iconKey: 'deploy-docs' as WarRoomNavIconName, label: t('portal.menu.deployCenter') },
  { path: '/storage', iconKey: 'storage' as WarRoomNavIconName, label: t('portal.menu.storage') },
  { path: '/image-space', iconKey: 'image-space' as WarRoomNavIconName, label: t('portal.menu.imageSpace') },
])

const isSettingsActive = computed(() => route.path === '/settings')

function isRailActive(path: string) {
  const current = route.path
  if (path === '/functions') {
    return current === '/functions' || current === '/user-center' || current === '/users'
  }
  if (path === '/home') return current === '/home'
  if (path === '/pomodoro') return current.startsWith('/pomodoro')
  if (path === '/ecommerce') return current.startsWith('/ecommerce')
  if (path === '/pixel-dog') return current === '/pixel-dog'
  return current === path
}
</script>
