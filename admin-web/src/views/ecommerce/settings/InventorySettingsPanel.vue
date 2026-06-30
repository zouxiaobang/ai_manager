<template>
  <el-form v-loading="loading" :model="form" label-width="140px" class="ec-settings-form">
    <el-divider content-position="left">{{ t('ecommerce.settings.inventoryDefaultsSection') }}</el-divider>
    <el-form-item :label="t('ecommerce.settings.defaultAlertThreshold')" required>
      <el-input-number v-model="form.defaultAlertThreshold" :min="0" :max="99999" />
      <p class="ec-settings-form__hint">{{ t('ecommerce.settings.defaultAlertThresholdHint') }}</p>
    </el-form-item>

    <el-divider content-position="left">{{ t('ecommerce.settings.slowMovingSection') }}</el-divider>
    <el-form-item :label="t('ecommerce.settings.slowMovingDays')" required>
      <el-input-number v-model="form.slowMovingDays" :min="1" :max="3650" />
      <p class="ec-settings-form__hint">{{ t('ecommerce.settings.slowMovingDaysHint') }}</p>
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.slowMovingFallbackDays')" required>
      <el-input-number v-model="form.slowMovingFallbackDays" :min="1" :max="3650" />
      <p class="ec-settings-form__hint">{{ t('ecommerce.settings.slowMovingFallbackDaysHint') }}</p>
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
import { fetchInventorySettings, saveInventorySettings } from '@/api/ecommerce/ecSettings'
import { useEcSettingsStore } from '@/stores/ecSettings'

const emit = defineEmits<{ saved: [updateTime?: string] }>()
const { t } = useI18n()
const ecSettings = useEcSettingsStore()

const loading = ref(false)
const saving = ref(false)
const form = reactive({
  defaultAlertThreshold: 10,
  slowMovingDays: 45,
  slowMovingFallbackDays: 90,
})

async function load() {
  loading.value = true
  try {
    const data = await fetchInventorySettings()
    form.defaultAlertThreshold = data.defaultAlertThreshold
    form.slowMovingDays = data.slowMovingDays
    form.slowMovingFallbackDays = data.slowMovingFallbackDays
    ecSettings.applyInventory(data)
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    const saved = await saveInventorySettings({ ...form })
    ecSettings.applyInventory(saved)
    ElMessage.success(t('ecommerce.settings.saveSuccess'))
    emit('saved', saved.updateTime)
  } finally {
    saving.value = false
  }
}

onMounted(() => { void load() })
</script>

<style scoped lang="scss">
.ec-settings-form {
  max-width: 640px;
}

.ec-settings-form__hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
</style>
