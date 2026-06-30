<template>
  <el-form v-loading="loading" :model="form" label-width="120px" class="ec-settings-form">
    <el-form-item :label="t('ecommerce.product.rebatePct')" required>
      <el-input-number v-model="form.defaultRebatePct" :min="0" :max="100" :precision="2" :step="0.5" />
      <p class="ec-settings-form__hint">{{ t('ecommerce.settings.rebateDefaultHint') }}</p>
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
import { fetchRebateSettings, saveRebateSettings } from '@/api/ecommerce/ecSettings'
import { useEcSettingsStore } from '@/stores/ecSettings'

const emit = defineEmits<{ saved: [updateTime?: string] }>()
const { t } = useI18n()
const ecSettings = useEcSettingsStore()
const loading = ref(false)
const saving = ref(false)
const form = reactive({ defaultRebatePct: 0 })

async function load() {
  loading.value = true
  try {
    const data = await fetchRebateSettings()
    form.defaultRebatePct = Number(data.defaultRebatePct ?? 0)
    ecSettings.applyRebate(data)
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    const saved = await saveRebateSettings({ defaultRebatePct: form.defaultRebatePct })
    ecSettings.applyRebate(saved)
    ElMessage.success(t('ecommerce.settings.saveSuccess'))
    emit('saved', saved.updateTime)
  } finally {
    saving.value = false
  }
}

onMounted(() => { void load() })
</script>

<style scoped lang="scss">
.ec-settings-form { max-width: 420px; }
.ec-settings-form__hint { margin: 6px 0 0; font-size: 12px; color: var(--el-text-color-secondary); }
</style>
