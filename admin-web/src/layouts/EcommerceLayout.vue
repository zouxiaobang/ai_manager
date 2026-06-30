<template>
  <div class="ec-layout">
    <aside class="ec-nav" :class="{ 'ec-nav--collapsed': collapsed }">
      <nav class="ec-nav__list">
        <router-link
          v-for="item in navItems"
          :key="item.key"
          :to="item.path"
          class="ec-nav__item"
          :class="{ 'ec-nav__item--active': isNavActive(item) }"
          :title="collapsed ? t(item.labelKey) : undefined"
        >
          <el-icon class="ec-nav__icon"><component :is="item.icon" /></el-icon>
          <span v-show="!collapsed" class="ec-nav__label">{{ t(item.labelKey) }}</span>
        </router-link>
      </nav>
      <button type="button" class="ec-nav__collapse" @click="toggleCollapsed">
        <el-icon><component :is="collapsed ? Expand : Fold" /></el-icon>
        <span v-show="!collapsed">{{ t('ecommerce.nav.collapse') }}</span>
      </button>
    </aside>
    <main class="ec-layout__main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Expand, Fold } from '@element-plus/icons-vue'
import { EC_NAV_COLLAPSED_KEY, ECOMMERCE_NAV_ITEMS } from '@/data/ecommerce-nav'
import { useEcSettingsStore } from '@/stores/ecSettings'

const route = useRoute()
const { t } = useI18n()
const ecSettings = useEcSettingsStore()

onMounted(() => {
  void ecSettings.ensureLoaded()
})

const navItems = ECOMMERCE_NAV_ITEMS

const collapsed = ref(readNavCollapsed())

function readNavCollapsed() {
  const stored = localStorage.getItem(EC_NAV_COLLAPSED_KEY)
  if (stored === null) return true
  return stored === '1'
}

watch(collapsed, (value) => {
  localStorage.setItem(EC_NAV_COLLAPSED_KEY, value ? '1' : '0')
})

function toggleCollapsed() {
  collapsed.value = !collapsed.value
}

function isNavActive(item: (typeof navItems)[number]) {
  if (item.key === 'home') {
    return route.path === '/ecommerce'
  }
  return route.path === item.path || route.path.startsWith(`${item.path}/`)
}
</script>

<style scoped lang="scss">
.ec-layout {
  display: flex;
  align-items: flex-start;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: var(--wr-bg);
  gap: 12px;
  padding: 12px 12px 12px 0;
}

.ec-nav {
  width: 188px;
  flex-shrink: 0;
  align-self: flex-start;
  display: flex;
  flex-direction: column;
  height: auto;
  max-height: calc(100vh - 80px);
  margin-left: 4px;
  padding: 14px 10px;
  background: #fff;
  border: 1px solid var(--wr-border);
  border-radius: 14px;
  box-shadow: 0 8px 24px rgb(15 23 42 / 6%), 0 2px 8px rgb(15 23 42 / 4%);

  &--collapsed {
    width: 68px;
  }
}

.ec-nav__list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 0 1 auto;
  overflow-y: auto;
  scrollbar-width: thin;
}

.ec-nav__item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 10px 12px;
  border-radius: 10px;
  text-decoration: none;
  color: #4b5563;
  transition: background 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    background: #f8fafc;
    color: #1f2937;
  }

  &--active {
    background: #fff7ed;
    color: #c2410c;
    box-shadow: inset 0 0 0 1px #fed7aa;

    .ec-nav__icon {
      color: #f59e0b;
    }

    .ec-nav__label {
      font-weight: 600;
    }
  }
}

.ec-nav--collapsed .ec-nav__item {
  justify-content: center;
  padding-inline: 8px;
}

.ec-nav__icon {
  font-size: 18px;
  flex-shrink: 0;
  color: #6b7280;
}

.ec-nav__label {
  font-size: 14px;
  line-height: 1.2;
  white-space: nowrap;
}

.ec-nav__collapse {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  margin-top: 12px;
  padding: 10px 12px;
  border: none;
  border-top: 1px solid var(--wr-border);
  background: transparent;
  color: #6b7280;
  font-size: 13px;
  cursor: pointer;
  border-radius: 8px;

  &:hover {
    background: #f8fafc;
    color: #374151;
  }
}

.ec-nav--collapsed .ec-nav__collapse {
  padding-inline: 8px;
}

.ec-layout__main {
  flex: 1;
  align-self: stretch;
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: auto;
  border-radius: 14px;

  &:has(.war-room-page--fill),
  &:has(.ec-module-page) {
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }
}
</style>
