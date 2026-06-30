<template>
  <el-dialog
    v-model="visible"
    :title="shopId ? t('ecommerce.shop.editTitle') : t('ecommerce.shop.createTitle')"
    width="880px"
    destroy-on-close
    top="4vh"
    class="shop-form-dialog"
    @open="onOpen"
  >
    <div class="shop-form-dialog__layout">
      <aside class="shop-form-dialog__left">
        <section class="shop-form-dialog__profile-card">
          <EcImageField
            v-model="form.avatarUrl"
            size="large"
            class="shop-form-dialog__avatar"
            :dialog-title="t('ecommerce.shop.avatar')"
            :fallback-src="shopId ? platformFallbackIcon : ''"
          />
          <div class="shop-form-dialog__preview-name">
            {{ form.name.trim() || t('ecommerce.shop.namePlaceholder') }}
          </div>
          <el-tag v-if="selectedPlatform" size="small" type="info" class="shop-form-dialog__platform-tag">
            {{ selectedPlatform.name }}
          </el-tag>
          <p class="shop-form-dialog__avatar-hint">{{ t('ecommerce.shop.avatarHint') }}</p>
        </section>
      </aside>

      <div class="shop-form-dialog__right">
        <el-form :model="form" label-position="top" class="shop-form-dialog__form">
          <el-form-item :label="t('ecommerce.shop.name')" required>
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item :label="t('ecommerce.shop.nameEn')">
            <el-input v-model="form.nameEn" />
          </el-form-item>
          <el-form-item :label="t('ecommerce.shop.platform')" required>
            <el-select
              v-model="form.platformId"
              filterable
              style="width: 100%"
              class="shop-form-dialog__platform-select"
            >
              <template v-if="selectedPlatform" #prefix>
                <img
                  :src="resolvePlatformIcon(selectedPlatform.name, selectedPlatform.platformCode, selectedPlatform.avatarUrl)"
                  alt=""
                  class="shop-form-dialog__platform-select-icon"
                />
              </template>
              <el-option v-for="p in platformOptions" :key="p.id" :label="p.name" :value="p.id">
                <div class="shop-form-dialog__platform-option">
                  <img
                    :src="resolvePlatformIcon(p.name, p.platformCode, p.avatarUrl)"
                    alt=""
                    class="shop-form-dialog__platform-option-icon"
                  />
                  <span>{{ p.name }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item :label="t('ecommerce.shop.remark')">
            <el-input v-model="form.remark" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item :label="t('ecommerce.shop.status')">
            <div class="shop-form-dialog__status-toggle">
              <button
                type="button"
                class="shop-form-dialog__status-option"
                :class="{ 'is-active': form.status === 'ENABLED' }"
                @click="form.status = 'ENABLED'"
              >
                {{ t('ecommerce.product.enabled') }}
              </button>
              <button
                type="button"
                class="shop-form-dialog__status-option"
                :class="{ 'is-active': form.status === 'DISABLED' }"
                @click="form.status = 'DISABLED'"
              >
                {{ t('ecommerce.product.disabled') }}
              </button>
            </div>
          </el-form-item>

          <el-collapse class="shop-form-dialog__fees">
            <el-collapse-item :title="t('ecommerce.shop.feeSection')" name="fees">
              <div class="shop-form-dialog__fee-grid">
                <el-form-item :label="t('ecommerce.shop.categoryCommission')" class="shop-form-dialog__fee-item">
                  <el-input-number v-model="form.categoryCommissionPct" :min="0" :max="100" :precision="2" controls-position="right" class="shop-form-dialog__fee-input" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.shop.techServiceFee')" class="shop-form-dialog__fee-item">
                  <el-input-number v-model="form.techServiceFeePct" :min="0" :max="100" :precision="2" controls-position="right" class="shop-form-dialog__fee-input" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.shop.paymentFee')" class="shop-form-dialog__fee-item">
                  <el-input-number v-model="form.paymentFeePct" :min="0" :max="100" :precision="2" controls-position="right" class="shop-form-dialog__fee-input" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.shop.promotionFee')" class="shop-form-dialog__fee-item">
                  <el-input-number v-model="form.promotionFeePct" :min="0" :max="100" :precision="2" controls-position="right" class="shop-form-dialog__fee-input" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.shop.fulfillmentFee')" class="shop-form-dialog__fee-item">
                  <el-input-number v-model="form.fulfillmentFeePct" :min="0" :max="100" :precision="2" controls-position="right" class="shop-form-dialog__fee-input" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.shop.returnServiceFee')" class="shop-form-dialog__fee-item">
                  <el-input-number v-model="form.returnServiceFeePct" :min="0" :max="100" :precision="2" controls-position="right" class="shop-form-dialog__fee-input" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.shop.installmentFee')" class="shop-form-dialog__fee-item">
                  <el-input-number v-model="form.installmentFeePct" :min="0" :max="100" :precision="2" controls-position="right" class="shop-form-dialog__fee-input" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.shop.activityServiceFee')" class="shop-form-dialog__fee-item">
                  <el-input-number v-model="form.activityServiceFeePct" :min="0" :max="100" :precision="2" controls-position="right" class="shop-form-dialog__fee-input" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.shop.annualPlatformFee')" class="shop-form-dialog__fee-item">
                  <el-input-number v-model="form.annualPlatformFee" :min="0" :precision="2" controls-position="right" class="shop-form-dialog__fee-input" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.shop.depositAmount')" class="shop-form-dialog__fee-item">
                  <el-input-number v-model="form.depositAmount" :min="0" :precision="2" controls-position="right" class="shop-form-dialog__fee-input" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.shop.shippingInsuranceFee')" class="shop-form-dialog__fee-item">
                  <el-input-number v-model="form.shippingInsuranceFee" :min="0" :precision="2" controls-position="right" class="shop-form-dialog__fee-input" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.shop.otherFee')" class="shop-form-dialog__fee-item">
                  <el-input-number v-model="form.otherFeePct" :min="0" :max="100" :precision="2" controls-position="right" class="shop-form-dialog__fee-input" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.shop.defaultReceiveProvince')" class="shop-form-dialog__fee-item is-full">
                  <el-input v-model="form.defaultReceiveProvince" :placeholder="t('ecommerce.shop.defaultReceiveProvinceHint')" />
                </el-form-item>
                <el-form-item :label="t('ecommerce.shop.otherFeeRemark')" class="shop-form-dialog__fee-item is-full">
                  <el-input v-model="form.otherFeeRemark" />
                </el-form-item>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-form>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">{{ t('ecommerce.common.cancel') }}</el-button>
      <el-button type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.common.save') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { EcPlatform } from '@/api/ecommerce/platform'
import { createShop, updateShop, type EcShop, type EcShopSaveRequest } from '@/api/ecommerce/shop'
import EcImageField from '@/components/ecommerce/EcImageField.vue'
import { resolvePlatformIcon } from '@/utils/platformVisual'

const props = defineProps<{
  platformOptions: EcPlatform[]
  shop?: EcShop | null
}>()

const visible = defineModel<boolean>({ default: false })
const emit = defineEmits<{ saved: [] }>()

const { t } = useI18n()

const saving = ref(false)
const shopId = ref<number | null>(null)

const form = reactive<EcShopSaveRequest>({
  name: '',
  nameEn: '',
  avatarUrl: '',
  platformId: 0,
  remark: '',
  status: 'ENABLED',
})

const selectedPlatform = computed(() =>
  props.platformOptions.find((p) => p.id === form.platformId),
)

const platformFallbackIcon = computed(() => {
  const platform = selectedPlatform.value
  if (!platform) return ''
  return resolvePlatformIcon(platform.name, platform.platformCode, platform.avatarUrl)
})

function resetForm() {
  Object.assign(form, {
    name: '',
    nameEn: '',
    avatarUrl: '',
    platformId: props.platformOptions[0]?.id ?? 0,
    remark: '',
    categoryCommissionPct: undefined,
    techServiceFeePct: undefined,
    paymentFeePct: undefined,
    promotionFeePct: undefined,
    fulfillmentFeePct: undefined,
    returnServiceFeePct: undefined,
    installmentFeePct: undefined,
    activityServiceFeePct: undefined,
    annualPlatformFee: undefined,
    depositAmount: undefined,
    shippingInsuranceFee: undefined,
    otherFeePct: undefined,
    otherFeeRemark: '',
    defaultReceiveProvince: '',
    status: 'ENABLED',
  })
}

function onOpen() {
  shopId.value = props.shop?.id ?? null
  if (props.shop) {
    Object.assign(form, { ...props.shop, avatarUrl: props.shop.avatarUrl ?? '' })
  } else {
    resetForm()
  }
}

async function onSave() {
  if (!form.name.trim()) {
    ElMessage.warning(t('ecommerce.shop.nameRequired'))
    return
  }
  if (!form.platformId) {
    ElMessage.warning(t('ecommerce.shop.platformRequired'))
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      name: form.name.trim(),
      avatarUrl: form.avatarUrl?.trim() || undefined,
    }
    if (shopId.value) {
      await updateShop(shopId.value, payload)
    } else {
      await createShop(payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.shop-form-dialog {
  :deep(.el-dialog) {
    display: flex;
    flex-direction: column;
    max-height: 90vh;
    margin-bottom: 0;
  }

  :deep(.el-dialog__header) {
    flex-shrink: 0;
    margin-right: 0;
  }

  :deep(.el-dialog__body) {
    flex: 1;
    min-height: 0;
    overflow: hidden;
    padding-top: 8px;
  }

  :deep(.el-dialog__footer) {
    flex-shrink: 0;
    padding-top: 12px;
    border-top: 1px solid var(--wr-border, #e8ecef);
  }
}

.shop-form-dialog__layout {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
  height: 100%;
  max-height: min(68vh, 600px);
  min-height: 280px;
}

.shop-form-dialog__left {
  min-width: 0;
  align-self: start;
}

.shop-form-dialog__profile-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 16px;
  border-radius: 12px;
  background: var(--wr-bg, #f9f9fa);
  border: 1px solid var(--wr-border, #e8ecef);
  text-align: center;
}

.shop-form-dialog__avatar {
  :deep(.ec-image-field) {
    align-items: center;
  }

  :deep(.ec-image-field__preview) {
    width: 112px;
    height: 112px;
    border-radius: 50%;
  }
}

.shop-form-dialog__preview-name {
  margin-top: 14px;
  font-size: 16px;
  font-weight: 600;
  color: var(--wr-text, #333);
  line-height: 1.4;
  word-break: break-all;
}

.shop-form-dialog__platform-tag {
  margin-top: 8px;
}

.shop-form-dialog__platform-select {
  :deep(.el-select__prefix) {
    display: flex;
    align-items: center;
  }
}

.shop-form-dialog__platform-select-icon {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  object-fit: contain;
  flex-shrink: 0;
}

.shop-form-dialog__platform-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.shop-form-dialog__platform-option-icon {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  object-fit: contain;
  flex-shrink: 0;
}

.shop-form-dialog__avatar-hint {
  margin: 12px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--wr-muted, #999);
}

.shop-form-dialog__right {
  min-width: 0;
  height: 100%;
  max-height: min(68vh, 600px);
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 6px;
  padding-bottom: 8px;
  scrollbar-width: thin;
}

.shop-form-dialog__form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
  }
}

.shop-form-dialog__status-toggle {
  display: inline-flex;
  align-items: center;
  padding: 3px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #fff;
}

.shop-form-dialog__status-option {
  min-width: 76px;
  padding: 6px 22px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--wr-text, #333);
  font-size: 14px;
  line-height: 1.4;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;

  &.is-active {
    background: var(--wr-stat-green, #16a34a);
    color: #fff;
  }

  &:not(.is-active):hover {
    color: var(--wr-text-secondary, #666);
  }
}

.shop-form-dialog__fees {
  margin-top: 4px;
  border: none;

  :deep(.el-collapse-item__header) {
    height: 44px;
    font-weight: 600;
    color: var(--wr-text, #333);
    border-bottom: 1px solid var(--wr-border, #e8ecef);
    background: transparent;
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }

  :deep(.el-collapse-item__content) {
    padding: 12px 0 4px;
  }
}

.shop-form-dialog__fee-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.shop-form-dialog__fee-item {
  margin-bottom: 12px;

  &.is-full {
    grid-column: 1 / -1;
  }

  :deep(.el-form-item__label) {
    padding-bottom: 4px;
    line-height: 1.4;
    color: var(--wr-text-secondary, #666);
  }

  :deep(.el-form-item__content) {
    line-height: 1;
  }
}

.shop-form-dialog__fee-input {
  width: 100%;

  :deep(.el-input) {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .shop-form-dialog__layout {
    grid-template-columns: 1fr;
    max-height: min(75vh, 640px);
  }

  .shop-form-dialog__right {
    max-height: min(52vh, 480px);
  }

  .shop-form-dialog__fee-grid {
    grid-template-columns: 1fr;
  }
}
</style>
