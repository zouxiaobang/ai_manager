<!--
 * 系统设置页面组件
 * 管理应用全局配置，包括语言切换、主题切换、移动端首页主题设置等
 -->
<template>
  <WarRoomPage :title="t('portal.menu.settings')">
    <div class="war-room-panel">
      <el-form label-width="120px" style="max-width: 480px">
        <el-form-item :label="t('app.language')">
          <el-select
            :model-value="appStore.locale"
            style="width: 200px"
            @change="onLocaleChange"
          >
            <el-option :label="t('app.languageChinese')" value="zh-CN" />
            <el-option :label="t('app.languageEnglish')" value="en-US" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('portal.settings.theme')">
          <el-radio-group
            :model-value="appStore.theme"
            @update:model-value="onThemeChange"
          >
            <el-radio-button value="light">{{ t('app.themeLight') }}</el-radio-button>
            <el-radio-button value="dark">{{ t('app.themeDark') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('mobile.settings.homeTheme')">
          <el-radio-group
            class="settings-home-theme"
            :model-value="appStore.mobileHomeTheme"
            @update:model-value="onHomeThemeChange"
          >
            <el-radio
              v-for="item in mobileHomeThemeOptions"
              :key="item.id"
              :value="item.id"
              class="settings-home-theme__option"
            >
              <span class="settings-home-theme__label">{{ t(item.labelKey) }}</span>
              <span class="settings-home-theme__desc">{{ t(item.descKey) }}</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </div>
  </WarRoomPage>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
import { mobileHomeThemeOptions } from '@/data/mobile-home-themes'
import type { MobileHomeThemeId } from '@/data/mobile-home-themes'
import { useAppStore, type LocaleCode, type ThemeMode } from '@/stores/app'
import i18n from '@/i18n'

const { t } = useI18n()
const appStore = useAppStore()

function onLocaleChange(code: LocaleCode) {
  appStore.setLocale(code, i18n)
}

function onThemeChange(mode: ThemeMode) {
  appStore.applyTheme(mode)
}

function onHomeThemeChange(id: MobileHomeThemeId) {
  appStore.setMobileHomeTheme(id)
}
</script>

<style scoped lang="scss">
.settings-home-theme {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
}

.settings-home-theme__option {
  display: flex;
  align-items: flex-start;
  height: auto;
  margin-right: 0;
  white-space: normal;
}

.settings-home-theme__label {
  display: block;
  font-weight: 600;
  line-height: 1.35;
}

.settings-home-theme__desc {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.35;
}
</style>
