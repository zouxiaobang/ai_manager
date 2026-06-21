<template>
  <div class="portal">
    <header class="portal-header">
      <div class="portal-header__brand">
        <div class="portal-header__logo">AI</div>
        <span class="portal-header__title">{{ t('portal.title') }}</span>
      </div>
      <el-input
        v-model="searchKeyword"
        class="portal-header__search"
        :placeholder="t('portal.searchPlaceholder')"
        clearable
        :prefix-icon="Search"
      />
      <div class="portal-header__actions">
        <el-badge
          :value="todayTodoCount"
          :max="99"
          :hidden="todayTodoCount === 0"
          class="portal-badge"
        >
          <el-button
            :icon="Bell"
            circle
            :title="t('portal.dashboard.todayTodos')"
            @click="goTodayTodos"
          />
        </el-badge>
        <el-select
          :model-value="appStore.locale"
          size="small"
          style="width: 100px"
          @change="onLocaleChange"
        >
          <el-option label="中文" value="zh-CN" />
          <el-option label="EN" value="en-US" />
        </el-select>
        <el-button size="small" @click="appStore.toggleTheme()">
          {{ appStore.theme === 'dark' ? t('app.themeLight') : t('app.themeDark') }}
        </el-button>
        <el-avatar :size="32">{{ t('portal.adminShort') }}</el-avatar>
      </div>
    </header>

    <div class="portal-quick-nav">
      <span class="portal-quick-nav__label">{{ t('portal.quickNav') }}</span>
      <el-button
        v-for="item in quickNavItems"
        :key="item.key"
        size="small"
        text
        @click="onQuickNav(item.key)"
      >
        {{ item.icon }} {{ t(`portal.quick.${item.key}`) }}
      </el-button>
      <el-button size="small" text type="primary" :icon="Plus">
        {{ t('portal.quick.custom') }}
      </el-button>
    </div>

    <div class="portal-body">
      <aside class="portal-aside">
        <el-menu :default-active="activeMenu" router>
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <span>{{ t('portal.menu.home') }}</span>
          </el-menu-item>
          <el-menu-item index="/functions">
            <el-icon><Grid /></el-icon>
            <span>{{ t('portal.menu.functions') }}</span>
          </el-menu-item>
          <el-menu-item index="/notebook">
            <el-icon><Notebook /></el-icon>
            <span>{{ t('portal.menu.notebook') }}</span>
          </el-menu-item>
          <el-menu-item index="/user-center">
            <el-icon><Avatar /></el-icon>
            <span>{{ t('portal.menu.userCenter') }}</span>
          </el-menu-item>
          <el-menu-item index="/users">
            <el-icon><Lock /></el-icon>
            <span>{{ t('portal.menu.permission') }}</span>
          </el-menu-item>
          <el-menu-item index="/deploy-docs">
            <el-icon><Document /></el-icon>
            <span>{{ t('portal.menu.deployDocs') }}</span>
          </el-menu-item>
          <el-menu-item index="/storage">
            <el-icon><FolderOpened /></el-icon>
            <span>{{ t('portal.menu.storage') }}</span>
          </el-menu-item>
          <el-menu-item index="/settings">
            <el-icon><Setting /></el-icon>
            <span>{{ t('portal.menu.settings') }}</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <div class="portal-content">
        <main class="portal-main">
          <router-view />
        </main>
        <footer class="portal-footer">
          <span>{{ t('portal.footer.support') }}</span>
          <span>{{ t('portal.footer.email') }}</span>
          <span>{{ t('portal.footer.version') }}</span>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  Avatar,
  Bell,
  Document,
  FolderOpened,
  Grid,
  HomeFilled,
  Lock,
  Notebook,
  Plus,
  Search,
  Setting,
} from '@element-plus/icons-vue'
import { useAppStore, type LocaleCode } from '@/stores/app'
import { useTodoReminders } from '@/composables/useTodoReminders'
import i18n from '@/i18n'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const appStore = useAppStore()
const { todayTodoCount, refreshTodayCount } = useTodoReminders()
const searchKeyword = ref('')

const activeMenu = computed(() => route.path)

watch(
  () => route.fullPath,
  () => {
    void refreshTodayCount()
  },
)

function goTodayTodos() {
  router.push({ path: '/notebook', query: { tab: 'todos', filter: 'today' } })
}

const quickNavItems = [
  { key: 'finance', icon: '⭐' },
  { key: 'crm', icon: '📈' },
  { key: 'oa', icon: '🛠️' },
  { key: 'assets', icon: '📁' },
] as const

function onLocaleChange(code: LocaleCode) {
  appStore.setLocale(code, i18n)
}

function onQuickNav(key: string) {
  ElMessage.info(t('portal.quickNavHint', { name: t(`portal.quick.${key}`) }))
}
</script>

<style scoped lang="scss">
.portal-badge :deep(.el-badge__content) {
  top: 0;
  right: 0;
  left: auto;
  transform: translate(50%, -50%);
}
</style>
