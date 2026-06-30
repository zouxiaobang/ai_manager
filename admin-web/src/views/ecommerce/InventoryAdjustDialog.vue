<template>
  <el-dialog
    v-model="visible"
    :title="t('ecommerce.inventory.adjustTitle')"
    width="520px"
    destroy-on-close
    class="inventory-adjust-dialog"
    @open="onOpen"
    @closed="onClosed"
  >
    <div v-loading="loading" class="inv-adjust">
      <template v-if="target">
        <header class="inv-adjust__hero">
          <div class="inv-adjust__thumb">
            <el-image v-if="skuImageUrl" :src="skuImageUrl" fit="cover" class="inv-adjust__image">
              <template #error>
                <div class="inv-adjust__image-fallback">{{ t('ecommerce.outbound.noSkuImage') }}</div>
              </template>
            </el-image>
            <div v-else class="inv-adjust__image-fallback">{{ t('ecommerce.outbound.noSkuImage') }}</div>
          </div>
          <div class="inv-adjust__hero-main">
            <div class="inv-adjust__hero-info">
              <p class="inv-adjust__line inv-adjust__line--product">{{ target.productName || '—' }}</p>
              <p class="inv-adjust__line inv-adjust__line--sku">{{ target.skuCode }}</p>
              <button
                v-if="target.productId && target.specName"
                type="button"
                class="inv-adjust__line inv-adjust__line--spec is-link"
                @click="emitViewProduct"
              >
                {{ target.specName }}
              </button>
              <p v-else class="inv-adjust__line inv-adjust__line--spec">{{ target.specName || '—' }}</p>
              <p class="inv-adjust__line inv-adjust__line--factory">{{ target.factoryName || '—' }}</p>
            </div>
            <div class="inv-adjust__hero-status">
              <span
                class="inv-adjust__status-badge"
                :class="target.alertActive ? 'is-alert' : 'is-normal'"
              >
                <span class="inv-adjust__status-dot" aria-hidden="true" />
                {{ target.alertActive ? t('ecommerce.inventory.alerting') : t('ecommerce.inventory.normal') }}
              </span>
            </div>
          </div>
        </header>

        <section class="inv-adjust__preview">
          <div class="inv-adjust__preview-side">
            <span class="inv-adjust__preview-label">{{ t('ecommerce.inventory.adjustBefore') }}</span>
            <strong class="inv-adjust__preview-value">{{ currentQty }}</strong>
            <span class="inv-adjust__preview-unit">{{ t('ecommerce.inventory.unitPiece') }}</span>
          </div>
          <div class="inv-adjust__preview-arrow" aria-hidden="true">→</div>
          <div class="inv-adjust__preview-side is-after" :class="{ 'is-warning': deductExceeds }">
            <span class="inv-adjust__preview-label">{{ t('ecommerce.inventory.adjustAfter') }}</span>
            <strong class="inv-adjust__preview-value">{{ afterQty }}</strong>
            <span class="inv-adjust__preview-unit">{{ t('ecommerce.inventory.unitPiece') }}</span>
          </div>
        </section>

        <section class="inv-adjust__controls">
          <div class="inv-adjust__field">
            <span class="inv-adjust__field-label">{{ t('ecommerce.inventory.changeType') }}</span>
            <el-radio-group v-model="form.changeType" class="inv-adjust__type-group">
              <el-radio value="DEDUCT">{{ t('ecommerce.inventory.deductFull') }}</el-radio>
              <el-radio value="RECLAIM">{{ t('ecommerce.inventory.reclaimFull') }}</el-radio>
            </el-radio-group>
          </div>

          <div class="inv-adjust__field">
            <span class="inv-adjust__field-label">{{ t('ecommerce.inventory.adjustQtyLabel') }}</span>
            <div class="inv-adjust__stepper">
              <button
                type="button"
                class="inv-adjust__stepper-btn"
                :disabled="form.changeQty <= 1"
                @click="decrementQty"
              >
                −
              </button>
              <el-input-number
                v-model="form.changeQty"
                :min="1"
                :step="1"
                :controls="false"
                class="inv-adjust__stepper-input"
              />
              <button type="button" class="inv-adjust__stepper-btn" @click="incrementQty">+</button>
              <span class="inv-adjust__stepper-unit">{{ t('ecommerce.inventory.unitPiece') }}</span>
            </div>
            <p v-if="form.changeType === 'DEDUCT'" class="inv-adjust__hint">
              {{ t('ecommerce.inventory.availableDeduct', { qty: currentQty }) }}
            </p>
          </div>

          <el-alert
            v-if="deductExceeds"
            type="warning"
            :closable="false"
            show-icon
            class="inv-adjust__alert"
            :title="t('ecommerce.inventory.deductExceedsStock', { qty: currentQty })"
          />
        </section>
      </template>
    </div>

    <template #footer>
      <el-button @click="visible = false">{{ t('ecommerce.common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="onSubmit">
        {{ t('ecommerce.inventory.confirmAdjust') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { adjustInventory, fetchInventory, type EcInventory } from '@/api/ecommerce/inventory'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'

const props = defineProps<{
  modelValue: boolean
  inventory: EcInventory | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  refreshed: []
  viewProduct: [productId: number]
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const loading = ref(false)
const submitting = ref(false)
const target = ref<EcInventory | null>(null)

const form = reactive({
  changeType: 'DEDUCT' as 'DEDUCT' | 'RECLAIM',
  changeQty: 1,
})

const skuImageUrl = computed(() => getEcommerceImageUrl(target.value?.imageName))

const currentQty = computed(() => target.value?.quantity ?? 0)

const afterQty = computed(() => {
  if (form.changeType === 'DEDUCT') {
    return currentQty.value - form.changeQty
  }
  return currentQty.value + form.changeQty
})

const deductExceeds = computed(
  () => form.changeType === 'DEDUCT' && form.changeQty > currentQty.value,
)

const canSubmit = computed(
  () => Boolean(target.value) && form.changeQty > 0 && !deductExceeds.value,
)

watch(
  () => props.inventory?.id,
  () => {
    if (visible.value && props.inventory) {
      loadTarget()
    }
  },
)

async function onOpen() {
  if (props.inventory) {
    await loadTarget()
  }
}

function onClosed() {
  target.value = null
  form.changeType = 'DEDUCT'
  form.changeQty = 1
}

async function loadTarget() {
  if (!props.inventory) return
  loading.value = true
  try {
    target.value = await fetchInventory(props.inventory.id)
    form.changeType = 'DEDUCT'
    form.changeQty = 1
  } finally {
    loading.value = false
  }
}

function emitViewProduct() {
  if (target.value?.productId) {
    emit('viewProduct', target.value.productId)
  }
}

function decrementQty() {
  if (form.changeQty > 1) {
    form.changeQty -= 1
  }
}

function incrementQty() {
  form.changeQty += 1
}

async function onSubmit() {
  if (!target.value || !canSubmit.value) return
  if (!form.changeQty || form.changeQty <= 0) {
    ElMessage.warning(t('ecommerce.inventory.changeQtyRequired'))
    return
  }

  submitting.value = true
  try {
    await adjustInventory(target.value.id, {
      changeType: form.changeType,
      changeQty: form.changeQty,
    })
    ElMessage.success(t('ecommerce.common.saved'))
    visible.value = false
    emit('refreshed')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.inv-adjust {
  min-height: 120px;
}

.inv-adjust__hero {
  display: flex;
  gap: 16px;
  padding: 18px;
  margin-bottom: 20px;
  border-radius: 14px;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%);
  border: 1px solid #dbeafe;
}

.inv-adjust__thumb {
  flex-shrink: 0;
  width: 88px;
  height: 88px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.inv-adjust__image {
  width: 88px;
  height: 88px;
}

.inv-adjust__image-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 6px;
  font-size: 12px;
  color: #9ca3af;
  text-align: center;
  line-height: 1.3;
  background: #f3f4f6;
}

.inv-adjust__hero-main {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 0;
}

.inv-adjust__hero-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.inv-adjust__line {
  margin: 0;
  line-height: 1.45;
  word-break: break-word;
}

.inv-adjust__line--product {
  font-size: 14px;
  font-weight: 400;
  color: #374151;
}

.inv-adjust__line--sku {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.inv-adjust__line--spec {
  font-size: 17px;
  font-weight: 600;
  color: #166534;

  &.is-link {
    padding: 0;
    border: none;
    background: none;
    text-align: left;
    cursor: pointer;
    transition: color 0.15s ease;

    &:hover {
      color: #14532d;
      text-decoration: underline;
    }
  }
}

.inv-adjust__line--factory {
  font-size: 13px;
  color: #6b7280;
}

.inv-adjust__hero-status {
  flex-shrink: 0;
  align-self: center;
}

.inv-adjust__status-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;

  &.is-normal {
    color: #16a34a;
    background: #ecfdf5;
    border: 1px solid #bbf7d0;
  }

  &.is-alert {
    color: #dc2626;
    background: #fef2f2;
    border: 1px solid #fecaca;
  }
}

.inv-adjust__status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.inv-adjust__status-badge.is-normal .inv-adjust__status-dot {
  background: #22c55e;
}

.inv-adjust__status-badge.is-alert .inv-adjust__status-dot {
  background: #ef4444;
}

.inv-adjust__preview {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 20px 16px;
  margin-bottom: 20px;
  border: 1px solid #e8ecf2;
  border-radius: 12px;
  background: #fafbfc;
}

.inv-adjust__preview-side {
  flex: 1;
  text-align: center;
}

.inv-adjust__preview-label {
  display: block;
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 6px;
}

.inv-adjust__preview-value {
  display: block;
  font-size: 36px;
  font-weight: 700;
  line-height: 1.1;
  color: #111827;
}

.inv-adjust__preview-unit {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.inv-adjust__preview-side.is-after {
  padding: 12px 8px;
  border-radius: 10px;
  background: #fff7ed;

  .inv-adjust__preview-value {
    color: #ea580c;
  }
}

.inv-adjust__preview-side.is-after.is-warning {
  background: #fef2f2;

  .inv-adjust__preview-value {
    color: #dc2626;
  }
}

.inv-adjust__preview-arrow {
  flex-shrink: 0;
  font-size: 22px;
  color: #d1d5db;
  font-weight: 300;
}

.inv-adjust__controls {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.inv-adjust__field-label {
  display: block;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.inv-adjust__type-group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.inv-adjust__stepper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.inv-adjust__stepper-btn {
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

.inv-adjust__stepper-input {
  width: 100px;

  :deep(.el-input__inner) {
    text-align: center;
    font-size: 22px;
    font-weight: 600;
    height: 44px;
  }
}

.inv-adjust__stepper-unit {
  font-size: 14px;
  color: #6b7280;
}

.inv-adjust__hint {
  margin: 10px 0 0;
  text-align: center;
  font-size: 12px;
  color: #9ca3af;
}

.inv-adjust__alert {
  margin-top: 4px;
}
</style>
