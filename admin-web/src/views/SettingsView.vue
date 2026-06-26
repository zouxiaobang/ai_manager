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
            <el-option label="中文" value="zh-CN" />
            <el-option label="English" value="en-US" />
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
      </el-form>
    </div>
  </WarRoomPage>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
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
</script>
