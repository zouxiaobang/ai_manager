<template>
  <el-form v-loading="loading" :model="form" label-width="120px" class="ec-settings-form">
    <el-form-item :label="t('ecommerce.monthlySettlement.headerRow')" required>
      <el-input-number v-model="form.headerRow" :min="1" :max="100" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.monthlySettlement.dataStartRow')" required>
      <el-input-number v-model="form.dataStartRow" :min="2" :max="200" />
      <p class="ec-settings-form__hint">{{ t('ecommerce.settings.orderImportRowsHint') }}</p>
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.orderImportDateFormat')" required>
      <el-input v-model="form.dateFormat" maxlength="64" />
      <p class="ec-settings-form__hint">{{ t('ecommerce.settings.orderImportDateFormatHint') }}</p>
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
import { fetchOrderImportSettings, saveOrderImportSettings } from '@/api/ecommerce/ecSettings'
import { useEcSettingsStore } from '@/stores/ecSettings'

const emit = defineEmits<{ saved: [updateTime?: string] }>()
const { t } = useI18n()
const ecSettings = useEcSettingsStore()

const loading = ref(false)
const saving = ref(false)
const form = reactive({ headerRow: 1, dataStartRow: 2, dateFormat: 'yyyy-MM-dd HH:mm:ss' })

async function load() {
  loading.value = true
  try {
    const data = await fetchOrderImportSettings()
    form.headerRow = data.headerRow
    form.dataStartRow = data.dataStartRow
    form.dateFormat = data.dateFormat || 'yyyy-MM-dd HH:mm:ss'
    ecSettings.applyOrderImport(data)
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (form.dataStartRow <= form.headerRow) {
    ElMessage.warning(t('ecommerce.settings.importRowsInvalid'))
    return
  }
  if (!form.dateFormat.trim()) {
    ElMessage.warning(t('ecommerce.settings.orderImportDateFormatRequired'))
    return
  }
  saving.value = true
  try {
    const saved = await saveOrderImportSettings({
      headerRow: form.headerRow,
      dataStartRow: form.dataStartRow,
      dateFormat: form.dateFormat.trim(),
    })
    ecSettings.applyOrderImport(saved)
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
  max-width: 520px;
}

.ec-settings-form__hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
