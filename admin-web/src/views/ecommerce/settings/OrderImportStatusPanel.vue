<template>
  <el-form v-loading="loading" label-width="120px" class="ec-settings-form">
    <SettingsStatusMappingEditor
      v-model:default-line-status="form.defaultLineStatus"
      v-model:status-mapping="form.statusMapping"
    />
    <el-form-item>
      <el-button type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.common.save') }}</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { fetchOrderImportStatusSettings, saveOrderImportStatusSettings } from '@/api/ecommerce/ecSettings'
import { DEFAULT_STATUS_MAPPING, normalizeLineStatus, type ImportLineStatus } from '@/constants/importStatusMapping'
import { useEcSettingsStore } from '@/stores/ecSettings'
import SettingsStatusMappingEditor from './SettingsStatusMappingEditor.vue'

const emit = defineEmits<{ saved: [updateTime?: string] }>()
const { t } = useI18n()
const ecSettings = useEcSettingsStore()
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  defaultLineStatus: 'PAID' as ImportLineStatus,
  statusMapping: { ...DEFAULT_STATUS_MAPPING } as Record<string, string>,
})

async function load() {
  loading.value = true
  try {
    const data = await fetchOrderImportStatusSettings()
    form.defaultLineStatus = normalizeLineStatus(data.defaultLineStatus)
    form.statusMapping = { ...data.statusMapping }
    ecSettings.applyOrderImportStatus(data)
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    const saved = await saveOrderImportStatusSettings({ ...form })
    form.defaultLineStatus = normalizeLineStatus(saved.defaultLineStatus)
    form.statusMapping = { ...saved.statusMapping }
    ecSettings.applyOrderImportStatus(saved)
    ElMessage.success(t('ecommerce.settings.saveSuccess'))
    emit('saved', saved.updateTime)
  } finally {
    saving.value = false
  }
}

onMounted(() => { void load() })
</script>

<style scoped lang="scss">
.ec-settings-form { max-width: 760px; }
</style>
