<template>
  <el-form v-loading="loading" :model="form" label-width="160px" class="ec-settings-form">
    <el-form-item :label="t('ecommerce.settings.inventoryAlertEnabled')">
      <el-switch v-model="form.inventoryAlertEnabled" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.zeroStockAlertEnabled')">
      <el-switch v-model="form.zeroStockAlertEnabled" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.settlementRemindEnabled')">
      <el-switch v-model="form.settlementRemindEnabled" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.settlementRemindDay')">
      <el-input-number v-model="form.settlementRemindDayOfMonth" :min="1" :max="28" />
      <p class="ec-settings-form__hint">{{ t('ecommerce.settings.settlementRemindDayHint') }}</p>
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
import { fetchNotificationSettings, saveNotificationSettings } from '@/api/ecommerce/ecSettings'
import { useEcSettingsStore } from '@/stores/ecSettings'

const emit = defineEmits<{ saved: [updateTime?: string] }>()
const { t } = useI18n()
const ecSettings = useEcSettingsStore()
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  inventoryAlertEnabled: true,
  zeroStockAlertEnabled: true,
  settlementRemindEnabled: true,
  settlementRemindDayOfMonth: 25,
})

async function load() {
  loading.value = true
  try {
    const data = await fetchNotificationSettings()
    form.inventoryAlertEnabled = !!data.inventoryAlertEnabled
    form.zeroStockAlertEnabled = !!data.zeroStockAlertEnabled
    form.settlementRemindEnabled = !!data.settlementRemindEnabled
    form.settlementRemindDayOfMonth = data.settlementRemindDayOfMonth
    ecSettings.applyNotification(data)
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    const saved = await saveNotificationSettings({ ...form })
    ecSettings.applyNotification(saved)
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
.ec-settings-form__hint { margin: 6px 0 0; font-size: 12px; color: var(--el-text-color-secondary); }
</style>
