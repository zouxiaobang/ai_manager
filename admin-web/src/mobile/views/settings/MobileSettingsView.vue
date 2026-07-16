<template>
  <div class="mobile-page">
    <!-- 版本风格 -->
    <section class="mobile-card" style="margin-bottom: 12px">
      <div class="mobile-settings-version">
        <label class="mobile-settings-version__label">{{ t('mobile.settings.uiVersion') }}</label>
        <el-radio-group
          :model-value="currentVersion"
          @update:model-value="onVersionChange"
        >
          <el-radio value="v1">{{ t('mobile.settings.uiVersionV1') }}</el-radio>
          <el-radio value="v2">{{ t('mobile.settings.uiVersionV2') }}</el-radio>
        </el-radio-group>
      </div>
    </section>

    <section class="mobile-card">
      <SettingsView />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import SettingsView from '@/views/SettingsView.vue'
import { useAppStore } from '@/stores/app'
import { setAppShellPreference } from '@/utils/deviceShell'

const { t } = useI18n()
const appStore = useAppStore()

const currentVersion = ref(appStore.mobileUIVersion)

async function onVersionChange(version: 'v1' | 'v2') {
  if (version === currentVersion.value) return

  try {
    await ElMessageBox.confirm(
      t('app.confirmSwitchVersion'),
      t('app.confirmTitle'),
      { confirmButtonText: t('app.confirm'), cancelButtonText: t('app.cancel'), type: 'info' },
    )
    appStore.setMobileUIVersion(version)
    ElMessage.success(t('app.switchVersionSuccess'))
    setTimeout(() => {
      setAppShellPreference('mobile')
      window.location.reload()
    }, 800)
  } catch {
    currentVersion.value = appStore.mobileUIVersion
  }
}
</script>

<style scoped lang="scss">
.mobile-settings-version {
  padding: 4px 0;

  &__label {
    display: block;
    font-size: 14px;
    color: var(--el-text-color-secondary);
    margin-bottom: 8px;
  }
}

.mobile-card :deep(.dashboard-section-title) {
  margin-top: 0;
}

.mobile-card :deep(.el-form-item__label) {
  width: 88px !important;
}
</style>
