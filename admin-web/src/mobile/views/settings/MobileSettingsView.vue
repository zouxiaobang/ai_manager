<template>
  <MobilePage>
    <MobileCard>
      <el-form label-width="100px">
        <el-form-item :label="t('app.language')">
          <el-select
            :model-value="appStore.locale"
            style="width: 100%"
            @change="onLocaleChange"
          >
            <el-option :label="t('app.languageChinese')" value="zh-CN" />
            <el-option :label="t('app.languageEnglish')" value="en-US" />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('mobile.settings.primaryColor')">
          <div class="v2-settings-colors">
            <button
              v-for="c in PRIMARY_COLORS"
              :key="c.value"
              type="button"
              class="v2-settings-color-btn"
              :class="{ 'is-active': appStore.primaryColor === c.value }"
              :style="{ background: c.value }"
              :title="isZh ? c.name : c.nameEn"
              @click="onColorChange(c.value)"
            />
          </div>
        </el-form-item>

        <el-form-item :label="t('mobile.settings.mode')">
          <el-radio-group
            :model-value="appStore.theme"
            @update:model-value="onThemeChange"
          >
            <el-radio value="light">{{ t('app.themeLight') }}</el-radio>
            <el-radio value="dark">{{ t('app.themeDark') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-divider style="margin: 12px 0" />

        <div class="v2-settings-subtitle">{{ t('portal.settings.pomodoroSound') }}</div>

        <el-form-item :label="t('portal.settings.pomodoroSoundEnabled')">
          <el-switch
            :model-value="appStore.pomodoroSoundEnabled"
            @update:model-value="appStore.setPomodoroSoundEnabled"
          />
        </el-form-item>

        <el-form-item :label="t('portal.settings.pomodoroBeeps')">
          <div class="v2-slider-row">
            <el-slider
              :model-value="appStore.pomodoroBeeps"
              :min="1"
              :max="20"
              :step="1"
              style="flex: 1; margin-right: 10px"
              @update:model-value="appStore.setPomodoroBeeps"
            />
            <span class="v2-slider-row__value">{{ appStore.pomodoroBeeps }}</span>
          </div>
        </el-form-item>

        <el-form-item :label="t('portal.settings.pomodoroVolume')">
          <div class="v2-slider-row">
            <el-slider
              :model-value="appStore.pomodoroVolume * 100"
              :min="0"
              :max="100"
              :step="1"
              style="flex: 1; margin-right: 10px"
              @update:model-value="onVolumeChange"
            />
            <span class="v2-slider-row__value">{{ Math.round(appStore.pomodoroVolume * 100) }}%</span>
          </div>
        </el-form-item>

        <el-divider style="margin: 12px 0" />

        <div class="v2-settings-subtitle">{{ t('portal.settings.todoRemindSound') }}</div>

        <el-form-item :label="t('portal.settings.todoRemindEnabled')">
          <el-switch
            :model-value="appStore.todoRemindEnabled"
            @update:model-value="appStore.setTodoRemindEnabled"
          />
        </el-form-item>

        <el-form-item :label="t('portal.settings.todoRemindVolume')">
          <div class="v2-slider-row">
            <el-slider
              :model-value="appStore.todoRemindVolume * 100"
              :min="0"
              :max="100"
              :step="1"
              :disabled="!appStore.todoRemindEnabled"
              style="flex: 1; margin-right: 10px"
              @update:model-value="onTodoVolumeChange"
            />
            <span class="v2-slider-row__value">{{ Math.round(appStore.todoRemindVolume * 100) }}%</span>
          </div>
        </el-form-item>

        <el-form-item :label="t('portal.settings.todoRemindBeeps')">
          <div class="v2-slider-row">
            <el-slider
              :model-value="appStore.todoRemindBeeps"
              :min="1"
              :max="20"
              :step="1"
              :disabled="!appStore.todoRemindEnabled"
              style="flex: 1; margin-right: 10px"
              @update:model-value="appStore.setTodoRemindBeeps"
            />
            <span class="v2-slider-row__value">{{ appStore.todoRemindBeeps }}次</span>
          </div>
        </el-form-item>
      </el-form>
    </MobileCard>

    <el-button class="v2-settings-desktop" @click="onOpenDesktop">
      {{ t('mobile.settings.openDesktop') }}
    </el-button>
  </MobilePage>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElDivider, ElMessage } from 'element-plus'
import MobilePage from '@/mobile/components/MobilePage.vue'
import MobileCard from '@/mobile/components/MobileCard.vue'
import { useAppStore, PRIMARY_COLORS, type LocaleCode, type ThemeMode } from '@/stores/app'
import { setAppShellPreference, resolveAppShell } from '@/utils/deviceShell'
import i18n from '@/i18n'

const { t, locale } = useI18n()
const appStore = useAppStore()

const isZh = computed(() => locale.value === 'zh-CN')

function onLocaleChange(code: LocaleCode) {
  appStore.setLocale(code, i18n)
}

function onColorChange(color: string) {
  appStore.applyPrimaryColor(color)
  ElMessage.success(t('mobile.settings.primaryColor') + ' ' + (isZh.value ? '已更新' : 'updated'))
}

function onThemeChange(mode: ThemeMode) {
  appStore.applyTheme(mode)
}

function onVolumeChange(val: number) {
  appStore.setPomodoroVolume(val / 100)
}

function onTodoVolumeChange(val: number) {
  appStore.setTodoRemindVolume(val / 100)
}

function onOpenDesktop() {
  const target = resolveAppShell() === 'pc' ? window.location.href : window.location.href.replace('mobile.html', 'index.html')
  if (resolveAppShell() !== 'pc') {
    setAppShellPreference('pc')
  }
  window.location.href = target
}
</script>

<style scoped lang="scss">
.v2-settings-colors {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.v2-settings-color-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: border-color 0.15s, transform 0.15s;
  outline: none;

  &:hover {
    transform: scale(1.15);
  }

  &.is-active {
    border-color: var(--wr-text, #333);
    box-shadow: 0 0 0 2px white, 0 0 0 4px currentColor;
  }
}

.v2-settings-subtitle {
  font-size: 15px;
  font-weight: 600;
  padding: 0 0 8px;
  color: var(--wr-text, #333);
}

.v2-slider-row {
  display: flex;
  align-items: center;
  width: 100%;

  &__value {
    font-size: 14px;
    min-width: 28px;
    text-align: center;
    color: var(--wr-text-secondary, #666);
  }
}

.v2-settings-desktop {
  width: 100%;
  margin-top: 8px;
}
</style>
