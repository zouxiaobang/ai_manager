<template>
  <el-form v-loading="loading" :model="form" label-width="120px" class="ec-settings-form">
    <el-form-item :label="t('ecommerce.settings.dnTitle')" required>
      <el-input v-model="form.title" maxlength="128" show-word-limit />
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.poAddress')">
      <el-input v-model="form.address" type="textarea" :rows="2" maxlength="512" show-word-limit />
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.poTel')">
      <el-input v-model="form.tel" maxlength="64" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.inbound.poPreparedBy')">
      <el-input v-model="form.preparedBy" maxlength="64" />
    </el-form-item>

    <el-divider content-position="left">{{ t('ecommerce.settings.dnShipFromSection') }}</el-divider>
    <el-form-item :label="t('ecommerce.inbound.poReceiverName')">
      <el-input v-model="form.shipFromName" maxlength="64" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.inbound.poReceiverPhone')">
      <el-input v-model="form.shipFromPhone" maxlength="64" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.inbound.poReceiverAddress')">
      <el-input v-model="form.shipFromAddress" type="textarea" :rows="2" maxlength="512" show-word-limit />
    </el-form-item>

    <el-form-item :label="t('ecommerce.settings.poRequirements')">
      <SettingsListEditor v-model="form.requirementItems" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.poNotes')">
      <SettingsListEditor v-model="form.noteItems" />
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
import { fetchDeliveryNoteConfig, saveDeliveryNoteConfig } from '@/api/ecommerce/ecSettings'
import SettingsListEditor from './SettingsListEditor.vue'

const emit = defineEmits<{ saved: [updateTime?: string] }>()
const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const form = reactive({
  title: '',
  address: '',
  tel: '',
  preparedBy: '',
  shipFromName: '',
  shipFromPhone: '',
  shipFromAddress: '',
  requirementItems: [''] as string[],
  noteItems: [''] as string[],
})

function applyData(data: Awaited<ReturnType<typeof fetchDeliveryNoteConfig>>) {
  form.title = data.title || ''
  form.address = data.address || ''
  form.tel = data.tel || ''
  form.preparedBy = data.preparedBy || ''
  form.shipFromName = data.shipFromName || ''
  form.shipFromPhone = data.shipFromPhone || ''
  form.shipFromAddress = data.shipFromAddress || ''
  form.requirementItems = data.requirementItems?.length ? [...data.requirementItems] : ['']
  form.noteItems = data.noteItems?.length ? [...data.noteItems] : ['']
}

async function load() {
  loading.value = true
  try {
    applyData(await fetchDeliveryNoteConfig())
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (!form.title.trim()) {
    ElMessage.warning(t('ecommerce.settings.dnTitleRequired'))
    return
  }
  saving.value = true
  try {
    const saved = await saveDeliveryNoteConfig({
      ...form,
      title: form.title.trim(),
      requirementItems: form.requirementItems.map((item) => item.trim()).filter(Boolean),
      noteItems: form.noteItems.map((item) => item.trim()).filter(Boolean),
    })
    applyData(saved)
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
  max-width: 760px;
}
</style>
