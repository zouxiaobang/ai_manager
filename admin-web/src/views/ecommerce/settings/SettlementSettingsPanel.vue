<template>
  <el-form v-loading="loading" :model="form" label-width="150px" class="ec-settings-form">
    <el-form-item :label="t('ecommerce.settings.profitDisplayMode')">
      <el-radio-group v-model="form.profitDisplayMode">
        <el-radio value="ACTUAL_PREFERRED">{{ t('ecommerce.settings.profitDisplayActual') }}</el-radio>
        <el-radio value="ESTIMATED">{{ t('ecommerce.settings.profitDisplayEstimated') }}</el-radio>
      </el-radio-group>
      <p class="ec-settings-form__hint">{{ t('ecommerce.settings.profitDisplayModeHint') }}</p>
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.costIncludesFreight')">
      <el-switch v-model="form.costIncludesFreight" />
      <p class="ec-settings-form__hint">{{ t('ecommerce.settings.costIncludesFreightHint') }}</p>
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
import { fetchSettlementSettings, saveSettlementSettings } from '@/api/ecommerce/ecSettings'
import { useEcSettingsStore } from '@/stores/ecSettings'

const emit = defineEmits<{ saved: [updateTime?: string] }>()
const { t } = useI18n()
const ecSettings = useEcSettingsStore()
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  profitDisplayMode: 'ACTUAL_PREFERRED' as 'ESTIMATED' | 'ACTUAL_PREFERRED',
  costIncludesFreight: true,
})

async function load() {
  loading.value = true
  try {
    const data = await fetchSettlementSettings()
    form.profitDisplayMode = data.profitDisplayMode
    form.costIncludesFreight = !!data.costIncludesFreight
    ecSettings.applySettlement(data)
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    const saved = await saveSettlementSettings({ ...form })
    ecSettings.applySettlement(saved)
    ElMessage.success(t('ecommerce.settings.saveSuccess'))
    emit('saved', saved.updateTime)
  } finally {
    saving.value = false
  }
}

onMounted(() => { void load() })
</script>

<style scoped lang="scss">
.ec-settings-form { max-width: 640px; }
.ec-settings-form__hint { margin: 6px 0 0; font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.5; }
</style>
