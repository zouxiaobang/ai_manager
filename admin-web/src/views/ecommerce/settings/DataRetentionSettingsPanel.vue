<template>
  <el-form v-loading="loading" :model="form" label-width="160px" class="ec-settings-form">
    <el-form-item :label="t('ecommerce.settings.importHistoryRetentionDays')" required>
      <el-input-number v-model="form.importHistoryRetentionDays" :min="30" :max="3650" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.inventoryLogRetentionDays')" required>
      <el-input-number v-model="form.inventoryLogRetentionDays" :min="30" :max="3650" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.autoCleanupEnabled')">
      <el-switch v-model="form.autoCleanupEnabled" />
      <p class="ec-settings-form__hint">{{ t('ecommerce.settings.autoCleanupEnabledHint') }}</p>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.common.save') }}</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { fetchDataRetentionSettings, saveDataRetentionSettings } from '@/api/ecommerce/ecSettings'
import { useEcSettingsStore } from '@/stores/ecSettings'

const emit = defineEmits<{ saved: [updateTime?: string] }>()
const { t } = useI18n()
const ecSettings = useEcSettingsStore()
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  importHistoryRetentionDays: 365,
  inventoryLogRetentionDays: 180,
  autoCleanupEnabled: false,
})

async function load() {
  loading.value = true
  try {
    const data = await fetchDataRetentionSettings()
    form.importHistoryRetentionDays = data.importHistoryRetentionDays
    form.inventoryLogRetentionDays = data.inventoryLogRetentionDays
    form.autoCleanupEnabled = !!data.autoCleanupEnabled
    ecSettings.applyDataRetention(data)
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    const saved = await saveDataRetentionSettings({ ...form })
    ecSettings.applyDataRetention(saved)
    ElMessage.success(t('ecommerce.settings.saveSuccess'))
    emit('saved', saved.updateTime)
  } finally {
    saving.value = false
  }
}

onMounted(() => { void load() })
</script>

<style scoped lang="scss">
.ec-settings-form { max-width: 560px; }
.ec-settings-form__hint { margin: 6px 0 0; font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.5; }
</style>
