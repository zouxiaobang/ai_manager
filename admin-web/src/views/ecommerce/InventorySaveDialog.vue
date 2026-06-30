<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? t('ecommerce.inventory.editTitle') : t('ecommerce.inventory.createTitle')"
    width="520px"
    destroy-on-close
    class="inventory-save-dialog"
    @open="onOpen"
    @closed="onClosed"
  >
    <div v-loading="loading" class="inv-save">
      <section v-if="!isEdit" class="inv-save__field">
        <span class="inv-save__field-label">{{ t('ecommerce.inventory.skuCode') }}</span>
        <el-select
          v-model="form.skuCode"
          filterable
          remote
          :remote-method="searchSkuOptions"
          :loading="skuOptionsLoading"
          :placeholder="t('ecommerce.inventory.skuCodePlaceholder')"
          class="inv-save__select"
        >
          <el-option
            v-for="opt in skuOptions"
            :key="opt.skuCode"
            :label="skuOptionLabel(opt)"
            :value="opt.skuCode"
          />
        </el-select>
      </section>

      <template v-if="form.skuCode || isEdit">
        <header class="inv-save__hero">
        <div class="inv-save__thumb">
          <el-image v-if="skuImageUrl" :src="skuImageUrl" fit="cover" class="inv-save__image">
            <template #error>
              <div class="inv-save__image-fallback">{{ t('ecommerce.outbound.noSkuImage') }}</div>
            </template>
          </el-image>
          <div v-else class="inv-save__image-fallback">{{ t('ecommerce.outbound.noSkuImage') }}</div>
        </div>
        <div class="inv-save__hero-body">
          <h3 class="inv-save__sku">{{ displaySkuCode }}</h3>
          <p class="inv-save__product">{{ displayProductName }}</p>
          <p class="inv-save__meta">
            <span v-if="displaySpecName">{{ displaySpecName }}</span>
            <span v-if="displaySpecName && displayFactoryName"> · </span>
            <span v-if="displayFactoryName">{{ displayFactoryName }}</span>
          </p>
        </div>
        </header>

        <section class="inv-save__preview">
        <div class="inv-save__preview-side is-highlight">
          <span class="inv-save__preview-label">
            {{ isEdit ? t('ecommerce.inventory.quantity') : t('ecommerce.inventory.initialStock') }}
          </span>
          <strong class="inv-save__preview-value">{{ displayQuantity }}</strong>
          <span class="inv-save__preview-unit">{{ t('ecommerce.inventory.unitPiece') }}</span>
        </div>
        </section>

        <section v-if="!isEdit" class="inv-save__controls">
        <div class="inv-save__field">
          <span class="inv-save__field-label">{{ t('ecommerce.inventory.initialStock') }}</span>
          <div class="inv-save__stepper">
            <button
              type="button"
              class="inv-save__stepper-btn"
              :disabled="form.quantity <= 0"
              @click="decrementQty"
            >
              −
            </button>
            <el-input-number
              v-model="form.quantity"
              :min="0"
              :step="1"
              :controls="false"
              class="inv-save__stepper-input"
            />
            <button type="button" class="inv-save__stepper-btn" @click="incrementQty">+</button>
            <span class="inv-save__stepper-unit">{{ t('ecommerce.inventory.unitPiece') }}</span>
          </div>
        </div>
        </section>

        <section class="inv-save__alert-card">
        <h4 class="inv-save__alert-title">{{ t('ecommerce.inventory.alertSettings') }}</h4>
        <div class="inv-save__field">
          <span class="inv-save__field-label">{{ t('ecommerce.inventory.alertThreshold') }}</span>
          <div class="inv-save__stepper is-compact">
            <button
              type="button"
              class="inv-save__stepper-btn"
              :disabled="form.alertThreshold <= 0"
              @click="decrementThreshold"
            >
              −
            </button>
            <el-input-number
              v-model="form.alertThreshold"
              :min="0"
              :step="1"
              :controls="false"
              class="inv-save__stepper-input is-compact"
            />
            <button type="button" class="inv-save__stepper-btn" @click="incrementThreshold">+</button>
            <span class="inv-save__stepper-unit">{{ t('ecommerce.inventory.unitPiece') }}</span>
          </div>
        </div>
        <div class="inv-save__switch-row">
          <div>
            <span class="inv-save__switch-label">{{ t('ecommerce.inventory.ignoreAlert') }}</span>
            <p class="inv-save__switch-hint">{{ t('ecommerce.inventory.ignoreAlertHint') }}</p>
          </div>
          <el-switch v-model="form.ignoreAlert" />
        </div>
      </section>
      </template>
    </div>

    <template #footer>
      <el-button @click="visible = false">{{ t('ecommerce.common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="onSubmit">
        {{ isEdit ? t('ecommerce.common.save') : t('ecommerce.inventory.confirmCreate') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useEcSettingsStore } from '@/stores/ecSettings'
import {
  createInventory,
  fetchInventory,
  fetchInventorySkuOptions,
  updateInventory,
  type EcInventory,
  type EcInventorySaveRequest,
  type EcInventorySkuOption,
} from '@/api/ecommerce/inventory'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'

const props = defineProps<{
  modelValue: boolean
  inventory: EcInventory | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  refreshed: []
}>()

const { t } = useI18n()
const ecSettings = useEcSettingsStore()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const isEdit = computed(() => Boolean(props.inventory?.id))

const loading = ref(false)
const submitting = ref(false)
const skuOptionsLoading = ref(false)
const skuOptions = ref<EcInventorySkuOption[]>([])
const editDetail = ref<EcInventory | null>(null)

const form = reactive({
  skuCode: '',
  quantity: 0,
  ignoreAlert: false,
  alertThreshold: 0,
})

const selectedSku = computed(() => skuOptions.value.find((opt) => opt.skuCode === form.skuCode))

const displaySkuCode = computed(() => {
  if (isEdit.value) return editDetail.value?.skuCode || props.inventory?.skuCode || '—'
  return form.skuCode || '—'
})

const displayProductName = computed(() => {
  if (isEdit.value) return editDetail.value?.productName || props.inventory?.productName || '—'
  return selectedSku.value?.productName || '—'
})

const displaySpecName = computed(() => {
  if (isEdit.value) return editDetail.value?.specName || props.inventory?.specName
  return selectedSku.value?.specName
})

const displayFactoryName = computed(() => {
  if (isEdit.value) return editDetail.value?.factoryName || props.inventory?.factoryName
  return selectedSku.value?.factoryName
})

const skuImageUrl = computed(() => {
  if (isEdit.value) {
    return getEcommerceImageUrl(editDetail.value?.imageName)
  }
  return getEcommerceImageUrl(selectedSku.value?.imageName)
})

const displayQuantity = computed(() => {
  if (isEdit.value) return editDetail.value?.quantity ?? props.inventory?.quantity ?? 0
  return form.quantity
})

const canSubmit = computed(() => {
  if (isEdit.value) return Boolean(props.inventory?.id)
  return Boolean(form.skuCode.trim())
})

async function onOpen() {
  await ecSettings.ensureLoaded()
  resetForm()
  if (isEdit.value && props.inventory) {
    await loadEditDetail()
    return
  }
  form.alertThreshold = ecSettings.inventory.defaultAlertThreshold
  await searchSkuOptions('')
}

function onClosed() {
  editDetail.value = null
  skuOptions.value = []
  resetForm()
}

function resetForm() {
  form.skuCode = ''
  form.quantity = 0
  form.ignoreAlert = false
  form.alertThreshold = 0
}

async function loadEditDetail() {
  if (!props.inventory) return
  loading.value = true
  try {
    editDetail.value = await fetchInventory(props.inventory.id)
    form.skuCode = editDetail.value.skuCode
    form.quantity = editDetail.value.quantity ?? 0
    form.ignoreAlert = !!editDetail.value.ignoreAlert
    form.alertThreshold = editDetail.value.alertThreshold ?? 0
  } finally {
    loading.value = false
  }
}

async function searchSkuOptions(keyword: string) {
  skuOptionsLoading.value = true
  try {
    const options = await fetchInventorySkuOptions(undefined, keyword.trim() || undefined)
    skuOptions.value = options.filter((opt) => !opt.hasInventory)
  } finally {
    skuOptionsLoading.value = false
  }
}

function skuOptionLabel(opt: EcInventorySkuOption) {
  const parts = [opt.skuCode]
  if (opt.specName) parts.push(opt.specName)
  if (opt.productName) parts.push(opt.productName)
  return parts.join(' · ')
}

function decrementQty() {
  if (form.quantity > 0) form.quantity -= 1
}

function incrementQty() {
  form.quantity += 1
}

function decrementThreshold() {
  if (form.alertThreshold > 0) form.alertThreshold -= 1
}

function incrementThreshold() {
  form.alertThreshold += 1
}

async function onSubmit() {
  if (!canSubmit.value) {
    ElMessage.warning(t('ecommerce.inventory.skuCodeRequired'))
    return
  }

  submitting.value = true
  try {
    const payload: EcInventorySaveRequest = {
      skuCode: form.skuCode.trim(),
      ignoreAlert: form.ignoreAlert,
      alertThreshold: form.alertThreshold,
    }
    if (!isEdit.value) {
      payload.quantity = form.quantity
      await createInventory(payload)
    } else if (props.inventory) {
      await updateInventory(props.inventory.id, payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    visible.value = false
    emit('refreshed')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.inv-save {
  min-height: 120px;
}

.inv-save__field {
  margin-bottom: 18px;
}

.inv-save__field-label {
  display: block;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.inv-save__select {
  width: 100%;
}

.inv-save__hero {
  display: flex;
  gap: 14px;
  margin-bottom: 20px;
}

.inv-save__thumb {
  flex-shrink: 0;
  width: 64px;
  height: 64px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e8ecf2;
  background: #f8fafc;
}

.inv-save__image {
  width: 100%;
  height: 100%;
}

.inv-save__image-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 11px;
  color: #9ca3af;
  text-align: center;
  padding: 4px;
}

.inv-save__hero-body {
  min-width: 0;
}

.inv-save__sku {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.inv-save__product {
  margin: 0 0 4px;
  font-size: 13px;
  color: #374151;
  line-height: 1.4;
}

.inv-save__meta {
  margin: 0;
  font-size: 12px;
  color: #9ca3af;
}

.inv-save__preview {
  display: flex;
  justify-content: center;
  padding: 20px 16px;
  margin-bottom: 20px;
  border: 1px solid #e8ecf2;
  border-radius: 12px;
  background: #fafbfc;
}

.inv-save__preview-side {
  text-align: center;
}

.inv-save__preview-label {
  display: block;
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 6px;
}

.inv-save__preview-value {
  display: block;
  font-size: 36px;
  font-weight: 700;
  line-height: 1.1;
  color: #111827;
}

.inv-save__preview-unit {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.inv-save__preview-side.is-highlight {
  padding: 12px 24px;
  border-radius: 10px;
  background: #ecfdf5;

  .inv-save__preview-value {
    color: #059669;
  }
}

.inv-save__controls {
  margin-bottom: 20px;
}

.inv-save__stepper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;

  &.is-compact {
    justify-content: flex-start;
  }
}

.inv-save__stepper-btn {
  width: 40px;
  height: 40px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background: #fff;
  font-size: 20px;
  line-height: 1;
  color: #374151;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;

  &:hover:not(:disabled) {
    border-color: #2563eb;
    color: #2563eb;
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.inv-save__stepper-input {
  width: 100px;

  &.is-compact :deep(.el-input__inner) {
    font-size: 18px;
    height: 40px;
  }

  :deep(.el-input__inner) {
    text-align: center;
    font-size: 22px;
    font-weight: 600;
    height: 44px;
  }
}

.inv-save__stepper-unit {
  font-size: 14px;
  color: #6b7280;
}

.inv-save__alert-card {
  padding: 16px;
  border: 1px solid #e8ecf2;
  border-radius: 12px;
  background: #fff;
}

.inv-save__alert-title {
  margin: 0 0 14px;
  padding-left: 10px;
  border-left: 3px solid #2563eb;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.inv-save__switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.inv-save__switch-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.inv-save__switch-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.4;
}
</style>
