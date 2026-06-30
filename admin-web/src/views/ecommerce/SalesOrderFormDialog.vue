<template>
  <el-dialog
    :model-value="modelValue"
    :title="editingId ? t('ecommerce.salesOrder.editTitle') : t('ecommerce.salesOrder.createTitle')"
    width="1100px"
    destroy-on-close
    top="4vh"
    class="so-form-dialog"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="so-form">
      <aside class="so-form__config">
        <section class="so-form__section">
          <h4 class="so-form__section-title">{{ t('ecommerce.salesOrder.formSectionShopLogistics') }}</h4>
          <el-form label-position="top" class="so-form__fields" @submit.prevent>
            <el-form-item :label="t('ecommerce.salesOrder.shop')" required>
              <div class="so-form__select-wrap">
                <img
                  v-if="selectedShopIcon"
                  :src="selectedShopIcon.src"
                  alt=""
                  class="so-form__select-prefix"
                  :class="{ 'is-avatar': selectedShopIcon.isCustomAvatar }"
                />
                <el-select
                  :model-value="form.shopId"
                  filterable
                  class="so-form__select"
                  @update:model-value="onShopIdChange"
                >
                  <el-option v-for="s in shopOptions" :key="s.id" :label="s.name" :value="s.id">
                    <div class="so-form__shop-option">
                      <img
                        :src="shopOptionIcon(s).src"
                        alt=""
                        class="so-form__shop-option-icon"
                        :class="{ 'is-avatar': shopOptionIcon(s).isCustomAvatar }"
                      />
                      <span>{{ s.name }}</span>
                    </div>
                  </el-option>
                </el-select>
              </div>
            </el-form-item>
            <el-form-item :label="t('ecommerce.salesOrder.expressStation')">
              <div class="so-form__select-wrap">
                <ExpressStationAvatar
                  v-if="selectedExpressStation"
                  :station="selectedExpressStation"
                  size="xs"
                  class="so-form__select-prefix so-form__select-prefix--express"
                />
                <el-select
                  :model-value="form.expressStationId"
                  clearable
                  filterable
                  class="so-form__select"
                  @update:model-value="updateFormField('expressStationId', $event)"
                >
                  <el-option v-for="s in expressOptions" :key="s.id" :label="s.name" :value="s.id">
                    <div class="so-form__express-option">
                      <ExpressStationAvatar :station="s" size="xs" />
                      <span>{{ s.name }}</span>
                    </div>
                  </el-option>
                </el-select>
              </div>
            </el-form-item>
            <el-form-item :label="t('ecommerce.salesOrder.orderTime')" required>
              <el-date-picker
                :model-value="form.orderTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
                @update:model-value="updateFormField('orderTime', $event ?? '')"
              />
            </el-form-item>
          </el-form>
        </section>

        <section class="so-form__section">
          <h4 class="so-form__section-title">{{ t('ecommerce.salesOrder.formSectionOrderAmount') }}</h4>
          <el-form label-position="top" class="so-form__fields">
            <el-form-item :label="t('ecommerce.salesOrder.platformOrderNo')">
              <el-input
                :model-value="form.platformOrderNo"
                @update:model-value="updateFormField('platformOrderNo', $event)"
              />
            </el-form-item>
            <el-form-item :label="t('ecommerce.salesOrder.receivedAmount')" class="so-form__amount-item">
              <el-input-number
                :model-value="form.receivedAmount"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
                @update:model-value="updateFormField('receivedAmount', $event ?? undefined)"
              />
            </el-form-item>
            <el-form-item :label="t('ecommerce.salesOrder.trackingNumber')">
              <el-input
                :model-value="form.trackingNumber"
                @update:model-value="updateFormField('trackingNumber', $event)"
              />
            </el-form-item>
          </el-form>
        </section>

        <section class="so-form__section">
          <h4 class="so-form__section-title">{{ t('ecommerce.salesOrder.formSectionAddress') }}</h4>
          <el-form label-position="top" class="so-form__fields">
            <el-form-item :label="t('ecommerce.salesOrder.receiveAddress')">
              <el-input
                :model-value="form.receiveAddress"
                type="textarea"
                :rows="3"
                @update:model-value="onAddressInput"
              />
            </el-form-item>
            <el-form-item :label="t('ecommerce.salesOrder.receiveProvince')">
              <el-input :model-value="form.receiveProvince || '—'" disabled />
              <p class="so-form__hint">{{ t('ecommerce.salesOrder.receiveProvinceHint') }}</p>
            </el-form-item>
          </el-form>
        </section>
      </aside>

      <section class="so-form__lines">
        <header class="so-form__lines-head">
          <h4 class="so-form__lines-title">{{ t('ecommerce.salesOrder.lines') }}</h4>
        </header>

        <div class="so-form__line-list">
          <article
            v-for="(row, index) in form.lines"
            :key="index"
            class="so-form__line-card"
          >
            <div class="so-form__line-main">
              <div class="so-form__line-link-grid">
                <div class="so-form__line-link-field">
                  <label class="so-form__line-label">{{ t('ecommerce.salesOrder.linkName') }}</label>
                  <el-autocomplete
                    :model-value="row.linkName ?? ''"
                    :fetch-suggestions="fetchLinkNameSuggestions"
                    clearable
                    fit-input-width
                    :trigger-on-focus="true"
                    :placeholder="t('ecommerce.salesOrder.linkNameInputPlaceholder')"
                    @update:model-value="(v: string) => onLineLinkNameChange(row, v)"
                    @select="() => syncLineMatch(row)"
                  />
                </div>
                <div class="so-form__line-link-field">
                  <label class="so-form__line-label">{{ t('ecommerce.salesOrder.skuSpecName') }}</label>
                  <el-autocomplete
                    :model-value="row.skuSpecName ?? ''"
                    :fetch-suggestions="bindSkuSpecSuggestions(row)"
                    clearable
                    fit-input-width
                    :trigger-on-focus="true"
                    :placeholder="t('ecommerce.salesOrder.skuSpecInputPlaceholder')"
                    @update:model-value="(v: string) => onLineSkuSpecChange(row, v)"
                    @select="() => syncLineMatch(row)"
                  />
                </div>
              </div>
              <p v-if="!linkSkuOptions.length" class="so-form__line-hint">
                {{ t('ecommerce.salesOrder.linkSkuManualHint') }}
              </p>
            </div>
            <div class="so-form__line-fields">
              <div class="so-form__line-field">
                <label class="so-form__line-label">{{ t('ecommerce.salesOrder.skuQuantity') }}</label>
                <el-input-number
                  :model-value="row.skuQuantity"
                  :min="1"
                  :precision="0"
                  controls-position="right"
                  @update:model-value="row.skuQuantity = $event ?? 1"
                />
              </div>
              <div class="so-form__line-field">
                <label class="so-form__line-label">{{ t('ecommerce.salesOrder.lineReceived') }}</label>
                <el-input-number
                  :model-value="row.lineReceivedAmount"
                  :min="0"
                  :precision="2"
                  controls-position="right"
                  @update:model-value="row.lineReceivedAmount = $event ?? undefined"
                />
              </div>
            </div>
            <el-button
              class="so-form__line-remove"
              link
              type="danger"
              :disabled="form.lines.length <= 1"
              @click="emit('remove-line', index)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </article>

          <button type="button" class="so-form__line-add" @click="emit('add-line')">
            <el-icon><Plus /></el-icon>
            <span>{{ t('ecommerce.salesOrder.formAddLineEmpty') }}</span>
          </button>
        </div>
      </section>
    </div>

    <template #footer>
      <div class="so-form__footer">
        <div class="so-form__summary">
          <span class="so-form__summary-label">{{ t('ecommerce.salesOrder.formTotalReceived') }}</span>
          <strong class="so-form__summary-value"><CnyAmount :value="displayReceivedTotal" /></strong>
          <span v-if="lineReceivedTotal > 0 && form.receivedAmount == null" class="so-form__summary-hint">
            {{ t('ecommerce.salesOrder.formLineTotalHint') }}
          </span>
        </div>
        <div class="so-form__actions">
          <el-button type="primary" size="large" class="so-form__save-btn" :loading="saving" @click="emit('save')">
            {{ t('ecommerce.salesOrder.formSaveOrder') }}
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Delete, Plus } from '@element-plus/icons-vue'
import type { EcSalesOrderLineSaveItem } from '@/api/ecommerce/salesOrder'
import type { EcShop } from '@/api/ecommerce/shop'
import CnyAmount from '@/components/CnyAmount.vue'
import type { EcExpressStation } from '@/api/ecommerce/express'
import ExpressStationAvatar from '@/components/ecommerce/ExpressStationAvatar.vue'
import { resolveShopIconMeta } from '@/utils/shopVisual'

export type SalesOrderLineFormRow = EcSalesOrderLineSaveItem & { _pickerKey?: string }

export interface SalesOrderFormModel {
  shopId: number | undefined
  expressStationId: number | undefined
  orderTime: string
  payTime: string
  platformStatus: string
  platformOrderNo: string
  receivedAmount: number | undefined
  trackingNumber: string
  receiveAddress: string
  receiveProvince: string
  buyerRemark: string
  sellerRemark: string
  lines: SalesOrderLineFormRow[]
}

const props = defineProps<{
  modelValue: boolean
  editingId: number | null
  saving?: boolean
  form: SalesOrderFormModel
  shopOptions: EcShop[]
  expressOptions: EcExpressStation[]
  linkSkuOptions: { key: string; label: string; linkName: string; skuSpecName: string; listingLinkSkuId: number }[]
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  save: []
  'shop-change': [number]
  'sync-province': []
  'add-line': []
  'remove-line': [number]
}>()

const { t } = useI18n()

const lineReceivedTotal = computed(() =>
  props.form.lines.reduce((sum, row) => sum + (row.lineReceivedAmount ?? 0), 0),
)

const displayReceivedTotal = computed(() => {
  if (props.form.receivedAmount != null) return props.form.receivedAmount
  if (lineReceivedTotal.value > 0) return lineReceivedTotal.value
  return undefined
})

const selectedShop = computed(() => props.shopOptions.find((s) => s.id === props.form.shopId))

const selectedShopIcon = computed(() => {
  const shop = selectedShop.value
  if (!shop) return null
  return resolveShopIconMeta(shop.name, shop.platformName, shop.platformCode, shop.avatarUrl)
})

const selectedExpressStation = computed(() =>
  props.expressOptions.find((s) => s.id === props.form.expressStationId),
)

function shopOptionIcon(shop: EcShop) {
  return resolveShopIconMeta(shop.name, shop.platformName, shop.platformCode, shop.avatarUrl)
}

function fetchLinkNameSuggestions(query: string, cb: (results: { value: string }[]) => void) {
  const q = query.trim().toLowerCase()
  const names = new Set<string>()
  for (const opt of props.linkSkuOptions) {
    if (!q || opt.linkName.toLowerCase().includes(q)) {
      names.add(opt.linkName)
    }
  }
  const results = [...names].map((value) => ({ value }))
  if (q && !names.has(query.trim())) {
    results.unshift({ value: query.trim() })
  }
  cb(results)
}

function fetchSkuSpecSuggestions(
  query: string,
  cb: (results: { value: string }[]) => void,
  row: SalesOrderLineFormRow,
) {
  const q = query.trim().toLowerCase()
  const link = row.linkName?.trim()
  const specs = new Set<string>()
  for (const opt of props.linkSkuOptions) {
    if (link && opt.linkName !== link) continue
    if (!q || opt.skuSpecName.toLowerCase().includes(q)) {
      specs.add(opt.skuSpecName)
    }
  }
  const results = [...specs].map((value) => ({ value }))
  if (q && !specs.has(query.trim())) {
    results.unshift({ value: query.trim() })
  }
  cb(results)
}

function bindSkuSpecSuggestions(row: SalesOrderLineFormRow) {
  return (query: string, cb: (results: { value: string }[]) => void) => {
    fetchSkuSpecSuggestions(query, cb, row)
  }
}

function onLineLinkNameChange(row: SalesOrderLineFormRow, value: string) {
  row.linkName = value
  row.listingLinkSkuId = undefined
  syncLineMatch(row)
}

function onLineSkuSpecChange(row: SalesOrderLineFormRow, value: string) {
  row.skuSpecName = value
  row.listingLinkSkuId = undefined
  syncLineMatch(row)
}

function syncLineMatch(row: SalesOrderLineFormRow) {
  const linkName = row.linkName?.trim()
  const skuSpecName = row.skuSpecName?.trim()
  if (!linkName || !skuSpecName) {
    row.listingLinkSkuId = undefined
    return
  }
  const opt = props.linkSkuOptions.find(
    (item) => item.linkName === linkName && item.skuSpecName === skuSpecName,
  )
  row.listingLinkSkuId = opt?.listingLinkSkuId
}

function updateFormField<K extends keyof SalesOrderFormModel>(key: K, value: SalesOrderFormModel[K]) {
  props.form[key] = value
}

function onShopIdChange(shopId: number) {
  updateFormField('shopId', shopId)
  emit('shop-change', shopId)
}

function onAddressInput(value: string) {
  updateFormField('receiveAddress', value)
  emit('sync-province')
}
</script>

<style scoped lang="scss">
.so-form-dialog {
  :deep(.el-dialog__body) {
    padding: 0 20px 12px;
  }

  :deep(.el-dialog__footer) {
    padding: 0;
    border-top: 1px solid var(--el-border-color-lighter);
  }
}

.so-form {
  display: flex;
  gap: 0;
  min-height: 480px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  overflow: hidden;
}

.so-form__config {
  width: 320px;
  flex-shrink: 0;
  padding: 16px;
  background: #f8fafc;
  border-right: 1px solid var(--el-border-color-lighter);
  overflow-y: auto;
  max-height: 62vh;
}

.so-form__section {
  & + & {
    margin-top: 18px;
    padding-top: 18px;
    border-top: 1px dashed var(--el-border-color-lighter);
  }
}

.so-form__section-title,
.so-form__lines-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 700;
  color: var(--el-text-color-primary);

  &::before {
    content: '';
    width: 3px;
    height: 14px;
    border-radius: 2px;
    background: var(--el-color-primary);
    flex-shrink: 0;
  }
}

.so-form__lines-title {
  margin: 0;
  font-size: 15px;
}

.so-form__fields {
  :deep(.el-form-item) {
    margin-bottom: 14px;
  }

  :deep(.el-form-item__label) {
    padding-bottom: 4px;
    font-size: 12px;
    font-weight: 600;
    color: var(--el-text-color-secondary);
    line-height: 1.3;
  }
}

.so-form__amount-item {
  :deep(.el-input-number .el-input__wrapper) {
    background: #fff7ed;
    box-shadow: 0 0 0 1px #fed7aa inset;
  }

  :deep(.el-input__inner) {
    font-weight: 700;
    color: #ea580c;
  }
}

.so-form__hint {
  margin: 4px 0 0;
  font-size: 11px;
  line-height: 1.4;
  color: var(--el-text-color-placeholder);
}

.so-form__express-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.so-form__shop-option {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.so-form__shop-option-icon {
  width: 18px;
  height: 18px;
  object-fit: contain;
  flex-shrink: 0;

  &.is-avatar {
    border-radius: 50%;
    object-fit: cover;
  }
}

.so-form__select-wrap {
  position: relative;
  width: 100%;
}

.so-form__select {
  width: 100%;
}

.so-form__select-prefix {
  position: absolute;
  left: 10px;
  top: 50%;
  z-index: 2;
  width: 18px;
  height: 18px;
  object-fit: contain;
  transform: translateY(-50%);
  pointer-events: none;

  &.is-avatar {
    border-radius: 50%;
    object-fit: cover;
  }

  &--express {
    position: absolute;
  }
}

.so-form__select-wrap:has(.so-form__select-prefix) {
  :deep(.el-select__wrapper) {
    padding-left: 34px;
  }
}

.so-form__lines {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  max-height: 62vh;
}

.so-form__lines-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 16px 12px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
}

.so-form__line-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.so-form__line-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 12px;
  align-items: start;
  padding: 14px;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  background: #fafafa;
}

.so-form__line-main {
  min-width: 0;
}

.so-form__line-label {
  display: block;
  margin-bottom: 6px;
  font-size: 11px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.so-form__line-link-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.so-form__line-link-field {
  min-width: 0;

  :deep(.el-autocomplete) {
    width: 100%;
  }
}

.so-form__line-hint {
  margin: 8px 0 0;
  font-size: 11px;
  line-height: 1.4;
  color: var(--el-text-color-placeholder);
}

.so-form__line-fields {
  display: flex;
  gap: 10px;
}

.so-form__line-field {
  width: 108px;

  :deep(.el-input-number) {
    width: 100%;
  }
}

.so-form__line-remove {
  margin-top: 22px;
}

.so-form__line-add {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  min-height: 72px;
  padding: 12px;
  border: 1px dashed var(--el-border-color);
  border-radius: 12px;
  background: transparent;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s, background 0.15s;

  &:hover {
    border-color: var(--el-color-primary);
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
}

.so-form__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 20px 16px;
}

.so-form__summary {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.so-form__summary-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.so-form__summary-value {
  font-size: 22px;
  font-weight: 700;
  color: #ea580c;
}

.so-form__summary-hint {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.so-form__actions {
  display: flex;
  flex-shrink: 0;
}

.so-form__save-btn {
  min-width: 148px;
  height: 44px;
  padding: 0 28px;
  font-size: 15px;
  font-weight: 600;
}

@media (max-width: 900px) {
  .so-form {
    flex-direction: column;
    min-height: auto;
  }

  .so-form__config {
    width: 100%;
    max-height: none;
    border-right: none;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  .so-form__lines {
    max-height: none;
  }

  .so-form__line-card {
    grid-template-columns: 1fr;
  }

  .so-form__line-link-grid {
    grid-template-columns: 1fr;
  }

  .so-form__line-remove {
    margin-top: 0;
    justify-self: end;
  }

  .so-form__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .so-form__actions {
    justify-content: flex-end;
  }
}
</style>
