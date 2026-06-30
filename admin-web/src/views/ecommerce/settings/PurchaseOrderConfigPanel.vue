<template>
  <el-form v-loading="loading" :model="form" label-width="120px" class="ec-settings-form">
    <el-divider content-position="left">{{ t('ecommerce.settings.purchaseOrderSection') }}</el-divider>

    <el-form-item :label="t('ecommerce.settings.poTitle')" required>
      <el-input v-model="form.title" maxlength="128" show-word-limit />
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.poAddress')">
      <el-input v-model="form.address" type="textarea" :rows="2" maxlength="512" show-word-limit />
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.poTel')">
      <el-input v-model="form.tel" maxlength="64" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.poCompanyNo')">
      <el-input v-model="form.companyNo" maxlength="64" />
    </el-form-item>

    <el-form-item :label="t('ecommerce.settings.poRequirements')">
      <div class="ec-settings-list">
        <div v-for="(_, index) in form.requirementItems" :key="`req-${index}`" class="ec-settings-list__row">
          <span class="ec-settings-list__index">{{ index + 1 }}.</span>
          <el-input v-model="form.requirementItems[index]" type="textarea" :rows="2" />
          <el-button link type="danger" :disabled="form.requirementItems.length <= 1" @click="removeRequirement(index)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <el-button type="primary" link @click="addRequirement">
          <el-icon><Plus /></el-icon>
          {{ t('ecommerce.settings.addItem') }}
        </el-button>
      </div>
    </el-form-item>

    <el-form-item :label="t('ecommerce.settings.poNotes')">
      <div class="ec-settings-list">
        <div v-for="(_, index) in form.noteItems" :key="`note-${index}`" class="ec-settings-list__row">
          <span class="ec-settings-list__index">{{ index + 1 }}.</span>
          <el-input v-model="form.noteItems[index]" type="textarea" :rows="2" />
          <el-button link type="danger" :disabled="form.noteItems.length <= 1" @click="removeNote(index)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <el-button type="primary" link @click="addNote">
          <el-icon><Plus /></el-icon>
          {{ t('ecommerce.settings.addItem') }}
        </el-button>
      </div>
    </el-form-item>

    <el-divider content-position="left">{{ t('ecommerce.settings.poSignSection') }}</el-divider>

    <el-form-item :label="t('ecommerce.inbound.poPreparedBy')">
      <el-input v-model="form.preparedBy" maxlength="64" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.inbound.poPreparedPhone')">
      <el-input v-model="form.preparedPhone" maxlength="64" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.inbound.poReceiverName')">
      <el-input v-model="form.receiverName" maxlength="64" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.inbound.poReceiverPhone')">
      <el-input v-model="form.receiverPhone" maxlength="64" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.inbound.poReceiverAddress')">
      <el-input v-model="form.receiverAddress" type="textarea" :rows="2" maxlength="512" show-word-limit />
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
import { Delete, Plus } from '@element-plus/icons-vue'
import {
  fetchPurchaseOrderConfig,
  savePurchaseOrderConfig,
  type EcPurchaseOrderConfigSaveRequest,
} from '@/api/ecommerce/purchaseOrderConfig'

const emit = defineEmits<{
  saved: [updateTime?: string]
}>()

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)

const form = reactive<EcPurchaseOrderConfigSaveRequest>({
  title: '',
  address: '',
  tel: '',
  requirementItems: [''],
  noteItems: [''],
  preparedBy: '',
  preparedPhone: '',
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  companyNo: '',
})

function addRequirement() {
  form.requirementItems.push('')
}

function removeRequirement(index: number) {
  form.requirementItems.splice(index, 1)
}

function addNote() {
  form.noteItems.push('')
}

function removeNote(index: number) {
  form.noteItems.splice(index, 1)
}

function applyConfig(data: Awaited<ReturnType<typeof fetchPurchaseOrderConfig>>) {
  form.title = data.title || ''
  form.address = data.address || ''
  form.tel = data.tel || ''
  form.requirementItems = data.requirementItems?.length ? [...data.requirementItems] : ['']
  form.noteItems = data.noteItems?.length ? [...data.noteItems] : ['']
  form.preparedBy = data.preparedBy || ''
  form.preparedPhone = data.preparedPhone || ''
  form.receiverName = data.receiverName || ''
  form.receiverPhone = data.receiverPhone || ''
  form.receiverAddress = data.receiverAddress || ''
  form.companyNo = data.companyNo || ''
}

async function load() {
  loading.value = true
  try {
    const data = await fetchPurchaseOrderConfig()
    applyConfig(data)
    return data
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (!form.title.trim()) {
    ElMessage.warning(t('ecommerce.settings.poTitleRequired'))
    return
  }
  saving.value = true
  try {
    const saved = await savePurchaseOrderConfig({
      ...form,
      title: form.title.trim(),
      requirementItems: form.requirementItems.map((item) => item.trim()).filter(Boolean),
      noteItems: form.noteItems.map((item) => item.trim()).filter(Boolean),
    })
    applyConfig(saved)
    ElMessage.success(t('ecommerce.settings.saveSuccess'))
    emit('saved', saved.updateTime)
  } finally {
    saving.value = false
  }
}

function isConfigured() {
  return !!form.title.trim()
}

onMounted(() => {
  void load()
})

defineExpose({ load, isConfigured })
</script>

<style scoped lang="scss">
.ec-settings-form {
  max-width: 760px;
}

.ec-settings-list {
  width: 100%;
}

.ec-settings-list__row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
}

.ec-settings-list__index {
  flex: 0 0 20px;
  line-height: 32px;
  color: var(--el-text-color-secondary);
}
</style>
