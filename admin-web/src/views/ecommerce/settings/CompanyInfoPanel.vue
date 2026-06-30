<template>
  <el-form v-loading="loading" :model="form" label-width="120px" class="ec-settings-form">
    <el-form-item :label="t('ecommerce.settings.companyName')">
      <el-input v-model="form.companyName" maxlength="128" show-word-limit />
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.poAddress')">
      <el-input v-model="form.address" type="textarea" :rows="2" maxlength="512" show-word-limit />
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.poTel')">
      <el-input v-model="form.tel" maxlength="64" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.contactName')">
      <el-input v-model="form.contactName" maxlength="64" />
    </el-form-item>
    <el-form-item :label="t('ecommerce.settings.contactPhone')">
      <el-input v-model="form.contactPhone" maxlength="64" />
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
import { fetchCompanyInfo, saveCompanyInfo } from '@/api/ecommerce/ecSettings'

const emit = defineEmits<{ saved: [updateTime?: string] }>()
const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const form = reactive({
  companyName: '',
  address: '',
  tel: '',
  contactName: '',
  contactPhone: '',
})

async function load() {
  loading.value = true
  try {
    const data = await fetchCompanyInfo()
    form.companyName = data.companyName || ''
    form.address = data.address || ''
    form.tel = data.tel || ''
    form.contactName = data.contactName || ''
    form.contactPhone = data.contactPhone || ''
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    const saved = await saveCompanyInfo({ ...form })
    form.companyName = saved.companyName || ''
    form.address = saved.address || ''
    form.tel = saved.tel || ''
    form.contactName = saved.contactName || ''
    form.contactPhone = saved.contactPhone || ''
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
</style>
