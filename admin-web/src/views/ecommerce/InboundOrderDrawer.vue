<template>
  <el-drawer
    v-model="visible"
    :title="t('ecommerce.inbound.orderWorkbenchTitle')"
    size="1120px"
    destroy-on-close
    class="inbound-order-drawer"
    @closed="onClosed"
  >
    <div class="inbound-workbench">
      <div class="inbound-workbench__toolbar">
        <el-input
          v-model="keyword"
          :placeholder="t('ecommerce.inbound.orderSearchPlaceholder')"
          clearable
          size="large"
          style="width: 240px"
        />
        <el-select
          v-model="factoryId"
          clearable
          filterable
          :placeholder="t('ecommerce.inventory.factoryPlaceholder')"
          size="large"
          style="width: 180px"
        >
          <el-option v-for="f in factoryOptions" :key="f.id" :label="f.name" :value="f.id" />
        </el-select>
        <el-button type="primary" size="large" @click="openCreate">
          {{ t('ecommerce.inbound.createOrder') }}
        </el-button>
      </div>

      <div v-loading="loading" class="inbound-kanban">
        <section v-for="column in kanbanColumns" :key="column.key" class="inbound-kanban__column">
          <header class="inbound-kanban__column-head" :class="`is-${column.tone}`">
            <span class="inbound-kanban__column-title">{{ column.title }}</span>
            <span class="inbound-kanban__column-count">{{ column.orders.length }}</span>
          </header>
          <div class="inbound-kanban__column-body">
            <article
              v-for="order in column.orders"
              :key="order.id"
              class="inbound-kanban-card"
              :class="`is-${column.tone}`"
              @click="openDetail(order)"
            >
              <div class="inbound-kanban-card__head">
                <span class="inbound-kanban-card__order-no">{{ order.orderNo }}</span>
                <el-tag :type="statusTagType(order.status)" size="small">{{ statusLabel(order.status) }}</el-tag>
              </div>
              <p class="inbound-kanban-card__factory">{{ order.factoryName || '—' }}</p>
              <div class="inbound-kanban-card__meta">
                <span>{{ t('ecommerce.inbound.orderTime') }} {{ formatDate(order.orderTime) }}</span>
                <span>{{ t('ecommerce.inbound.expectedDeliveryTime') }} {{ formatDate(order.expectedDeliveryTime) }}</span>
              </div>
              <div class="inbound-kanban-card__stats">
                <span>{{ t('ecommerce.inbound.skuLineCount', { count: orderLineStats(order).skuCount }) }}</span>
                <span>{{ t('ecommerce.inbound.totalOrderedQty', { count: orderLineStats(order).totalQty }) }}</span>
              </div>
              <p v-if="order.remark" class="inbound-kanban-card__remark">{{ order.remark }}</p>
              <div v-if="order.status === 'DRAFT'" class="inbound-kanban-card__actions">
                <el-button size="small" @click.stop="openEdit(order)">{{ t('ecommerce.inventory.edit') }}</el-button>
                <el-button size="small" type="success" @click.stop="openConfirm(order)">
                  {{ t('ecommerce.inbound.confirm') }}
                </el-button>
                <el-button size="small" type="warning" @click.stop="onCancel(order)">
                  {{ t('ecommerce.inbound.cancel') }}
                </el-button>
                <el-button size="small" type="danger" link @click.stop="onDelete(order)">
                  {{ t('ecommerce.inventory.delete') }}
                </el-button>
              </div>
              <div v-else-if="order.status === 'CONFIRMED'" class="inbound-kanban-card__meta">
                <span>{{ t('ecommerce.inbound.actualReceiptTime') }} {{ formatDate(order.actualReceiptTime) }}</span>
              </div>
            </article>
            <el-empty v-if="!column.orders.length" :description="t('ecommerce.inbound.kanbanEmpty')" :image-size="56" />
          </div>
        </section>
      </div>

      <section class="inbound-timeline">
        <h3 class="inbound-timeline__title">{{ t('ecommerce.inbound.recentReceiptActivity') }}</h3>
        <div v-if="recentConfirmed.length" class="inbound-timeline__list">
          <div v-for="order in recentConfirmed" :key="order.id" class="inbound-timeline__item" @click="openDetail(order)">
            <span class="inbound-timeline__dot" />
            <div class="inbound-timeline__body">
              <div class="inbound-timeline__line1">
                <strong>{{ order.orderNo }}</strong>
                <span>{{ order.factoryName || '—' }}</span>
              </div>
              <div class="inbound-timeline__line2">
                {{ t('ecommerce.inbound.actualReceiptTime') }} {{ formatDateTime(order.actualReceiptTime) }}
                · {{ t('ecommerce.inbound.totalOrderedQty', { count: orderLineStats(order).totalQty }) }}
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else :description="t('ecommerce.inbound.recentReceiptEmpty')" :image-size="48" />
      </section>
    </div>

    <el-dialog
      v-model="formVisible"
      :title="editingId ? t('ecommerce.inbound.editOrderTitle') : t('ecommerce.inbound.createOrderTitle')"
      width="820px"
      destroy-on-close
      append-to-body
      class="inbound-form-dialog"
      @closed="onFormDialogClosed"
    >
      <div class="inbound-form-wizard__center">
        <el-steps :active="formStep" align-center finish-status="success" class="inbound-form-wizard__steps">
          <el-step :title="t('ecommerce.inbound.formStepBasic')" />
          <el-step :title="t('ecommerce.inbound.formStepLines')" />
          <el-step :title="t('ecommerce.inbound.formStepConfirm')" />
        </el-steps>

        <div class="inbound-form-wizard__body">
        <section v-show="formStep === 0" class="inbound-form-step">
          <el-form label-width="100px" label-position="right" class="inbound-form-step__basic-form">
            <el-form-item :label="t('ecommerce.inbound.orderTime')" required class="inbound-form-step__field">
              <el-date-picker
                v-model="form.orderTime"
                type="date"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                :placeholder="t('ecommerce.inbound.orderTime')"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item :label="t('ecommerce.inbound.expectedDeliveryTime')" required class="inbound-form-step__field">
              <el-date-picker
                v-model="form.expectedDeliveryTime"
                type="date"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                :placeholder="t('ecommerce.inbound.expectedDeliveryTime')"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item :label="t('ecommerce.inventory.factory')" class="inbound-form-step__field">
              <el-select
                v-model="form.factoryId"
                clearable
                filterable
                :placeholder="t('ecommerce.inbound.factoryNamePlaceholder')"
                style="width: 100%"
                @change="onFormFactoryChange"
              >
                <el-option v-for="f in factoryOptions" :key="f.id" :label="f.name" :value="f.id" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('ecommerce.inbound.remark')" class="inbound-form-step__field">
              <el-input v-model="form.remark" type="textarea" :rows="3" :placeholder="t('ecommerce.inbound.remark')" />
            </el-form-item>
          </el-form>
        </section>

        <section v-show="formStep === 1" class="inbound-form-step">
          <div class="inbound-form-step__lines-head">
            <span class="inbound-form-step__lines-title">{{ t('ecommerce.inbound.lines') }}</span>
            <el-button type="success" size="large" class="inbound-form-step__add-line-btn" @click="addLine">
              <el-icon><Plus /></el-icon>
              {{ t('ecommerce.inbound.addLine') }}
            </el-button>
          </div>
          <div v-if="form.lines.length" class="inbound-line-cards">
            <article v-for="(line, index) in form.lines" :key="index" class="inbound-line-card">
              <div class="inbound-line-card__thumb">
                <el-image
                  v-if="lineSkuImageUrl(line.skuCode)"
                  :src="lineSkuImageUrl(line.skuCode)"
                  fit="cover"
                  class="inbound-line-card__image"
                >
                  <template #error>
                    <div class="inbound-line-card__image-fallback">{{ t('ecommerce.inbound.noSkuImage') }}</div>
                  </template>
                </el-image>
                <div v-else class="inbound-line-card__image-fallback">{{ t('ecommerce.inbound.noSkuImage') }}</div>
              </div>
              <div class="inbound-line-card__field is-sku">
                <div class="inbound-line-card__control">
                  <el-select
                    v-model="line.skuCode"
                    filterable
                    size="large"
                    :placeholder="t('ecommerce.inventory.skuCodePlaceholder')"
                    class="inbound-line-card__sku"
                  >
                    <el-option
                      v-for="opt in filteredSkuOptions"
                      :key="opt.skuCode"
                      :label="skuOptionLabel(opt)"
                      :value="opt.skuCode"
                    />
                  </el-select>
                </div>
              </div>
              <div class="inbound-line-card__field is-qty">
                <label class="inbound-line-card__label">{{ t('ecommerce.inbound.orderedQty') }}</label>
                <el-input-number
                  v-model="line.quantity"
                  :min="1"
                  :step="1"
                  size="large"
                  controls-position="right"
                />
              </div>
              <el-button
                link
                type="danger"
                size="large"
                class="inbound-line-card__remove"
                :disabled="form.lines.length <= 1"
                @click="removeLine(index)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </article>
          </div>
          <el-empty v-else :description="t('ecommerce.inbound.linesRequired')" :image-size="64" />
        </section>

        <section v-show="formStep === 2" class="inbound-form-step">
          <div class="inbound-form-confirm">
            <div class="inbound-form-confirm__section">
              <h4 class="inbound-form-confirm__heading">
                <span class="inbound-form-confirm__heading-leading">
                  <span class="inbound-form-confirm__heading-bar" aria-hidden="true" />
                  <span>{{ t('ecommerce.inbound.formStepBasic') }}</span>
                </span>
              </h4>
              <dl class="inbound-form-confirm__grid">
                <div class="inbound-form-confirm__item">
                  <dt>{{ t('ecommerce.inbound.orderTime') }}</dt>
                  <dd>{{ form.orderTime || '—' }}</dd>
                </div>
                <div class="inbound-form-confirm__item">
                  <dt>{{ t('ecommerce.inbound.expectedDeliveryTime') }}</dt>
                  <dd>{{ form.expectedDeliveryTime || '—' }}</dd>
                </div>
                <div class="inbound-form-confirm__item">
                  <dt>{{ t('ecommerce.inventory.factory') }}</dt>
                  <dd>{{ selectedFactoryName || '—' }}</dd>
                </div>
                <div class="inbound-form-confirm__item is-full">
                  <dt>{{ t('ecommerce.inbound.remark') }}</dt>
                  <dd>{{ form.remark?.trim() || '—' }}</dd>
                </div>
              </dl>
            </div>
            <div class="inbound-form-confirm__section">
              <h4 class="inbound-form-confirm__heading">
                <span class="inbound-form-confirm__heading-leading">
                  <span class="inbound-form-confirm__heading-bar" aria-hidden="true" />
                  <span>{{ t('ecommerce.inbound.formStepLines') }}</span>
                </span>
                <span class="inbound-form-confirm__badge">{{ formLinesSummary }}</span>
              </h4>
              <div class="inbound-form-confirm__lines">
                <article v-for="(line, index) in validFormLines" :key="index" class="inbound-confirm-line-card">
                  <div class="inbound-confirm-line-card__thumb">
                    <el-image
                      v-if="lineSkuImageUrl(line.skuCode)"
                      :src="lineSkuImageUrl(line.skuCode)"
                      fit="cover"
                      class="inbound-confirm-line-card__image"
                    >
                      <template #error>
                        <div class="inbound-confirm-line-card__image-fallback">{{ t('ecommerce.inbound.noSkuImage') }}</div>
                      </template>
                    </el-image>
                    <div v-else class="inbound-confirm-line-card__image-fallback">{{ t('ecommerce.inbound.noSkuImage') }}</div>
                  </div>
                  <div class="inbound-confirm-line-card__info">
                    <strong class="inbound-confirm-line-card__sku">{{ line.skuCode }}</strong>
                    <div
                      v-if="lineSkuSpecName(line.skuCode) || lineSkuProductName(line.skuCode)"
                      class="inbound-confirm-line-card__tags"
                    >
                      <el-tag
                        v-if="lineSkuSpecName(line.skuCode)"
                        size="small"
                        effect="light"
                        class="inbound-confirm-line-card__tag is-spec"
                      >
                        {{ lineSkuSpecName(line.skuCode) }}
                      </el-tag>
                      <el-tag
                        v-if="lineSkuProductName(line.skuCode)"
                        size="small"
                        effect="light"
                        type="success"
                        class="inbound-confirm-line-card__tag is-product"
                      >
                        {{ lineSkuProductName(line.skuCode) }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="inbound-confirm-line-card__qty">
                    <div class="inbound-confirm-line-card__qty-block">
                      <strong class="inbound-confirm-line-card__qty-value">{{ line.quantity }}</strong>
                      <span class="inbound-confirm-line-card__qty-unit">{{ t('ecommerce.inventory.unitPiece') }}</span>
                    </div>
                  </div>
                </article>
              </div>
            </div>
          </div>
        </section>
        </div>

        <div v-if="formStep === 1 && validFormLines.length" class="inbound-form-wizard__floating-summary">
          {{ formLinesSummary }}
        </div>
      </div>

      <template #footer>
        <div class="inbound-form-wizard__footer">
          <el-button @click="formVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
          <el-button v-if="formStep > 0" @click="onFormPrev">{{ t('ecommerce.inbound.prevStep') }}</el-button>
          <el-button v-if="formStep < 2" type="primary" @click="onFormNext">{{ t('ecommerce.inbound.nextStep') }}</el-button>
          <el-button v-else type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.inbound.confirmSave') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="confirmVisible"
      :title="t('ecommerce.inbound.confirmTitle')"
      width="720px"
      destroy-on-close
      append-to-body
    >
      <p class="confirm-hint">{{ t('ecommerce.inbound.confirmHint') }}</p>
      <el-table :data="confirmLines" border size="small" max-height="360">
        <el-table-column prop="skuCode" :label="t('ecommerce.inventory.skuCode')" width="120" />
        <el-table-column prop="specName" :label="t('ecommerce.inventory.specName')" min-width="100" show-overflow-tooltip />
        <el-table-column prop="productName" :label="t('ecommerce.inventory.productName')" min-width="120" show-overflow-tooltip />
        <el-table-column :label="t('ecommerce.inbound.orderedQty')" width="100" align="right">
          <template #default="{ row }">{{ row.quantity }}</template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.inbound.receivedQty')" width="160">
          <template #default="{ row }">
            <el-input-number
              v-model="row.receivedQuantity"
              :min="0"
              :step="1"
              controls-position="right"
              style="width: 100%"
            />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="confirmVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="confirmSaving" @click="onConfirmSubmit">
          {{ t('ecommerce.inbound.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <OrderDetailDialog
      v-model="detailVisible"
      :loading="detailLoading"
      variant="inbound"
      :order="inboundDetailData"
      :resolve-sku-image="detailSkuImageUrl"
      @edit="onDetailEdit"
      @confirm="onDetailConfirm"
      @cancel="onDetailCancel"
      @delete="onDetailDelete"
      @print="onDetailPrint"
    />

    <InboundPurchaseOrderPrintDialog
      v-model="printPurchaseVisible"
      :order="detailOrder"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import {
  cancelInboundOrder,
  confirmInboundOrder,
  createInboundOrder,
  deleteInboundOrder,
  fetchInboundOrder,
  fetchInboundOrders,
  updateInboundOrder,
  type EcInboundOrder,
} from '@/api/ecommerce/inbound'
import { fetchInventorySkuOptions, type EcInventorySkuOption } from '@/api/ecommerce/inventory'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'
import { fetchFactoryOptions, type EcFactory } from '@/api/ecommerce/factory'
import { formatDate, formatDateTime, todayDateString, tomorrowDateString, toApiDateTime } from '@/utils/date'
import OrderDetailDialog, { type OrderDetailData } from './OrderDetailDialog.vue'
import InboundPurchaseOrderPrintDialog from './InboundPurchaseOrderPrintDialog.vue'

type KanbanKey = 'draft' | 'pending' | 'confirmed'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; refreshed: [] }>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const saving = ref(false)
const confirmSaving = ref(false)
const loading = ref(false)
const keyword = ref('')
const factoryId = ref<number | undefined>()
const orders = ref<EcInboundOrder[]>([])
const factoryOptions = ref<EcFactory[]>([])
const skuOptions = ref<EcInventorySkuOption[]>([])

const formVisible = ref(false)
const formStep = ref(0)
const confirmVisible = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailOrder = ref<EcInboundOrder | null>(null)
const printPurchaseVisible = ref(false)
const editingId = ref<number | null>(null)
const confirmingOrderId = ref<number | null>(null)
const confirmLines = ref<Array<{
  lineId: number
  skuCode: string
  specName?: string
  productName?: string
  quantity: number
  receivedQuantity: number
}>>([])

const form = ref({
  factoryId: undefined as number | undefined,
  remark: '',
  orderTime: '',
  expectedDeliveryTime: '',
  lines: [{ skuCode: '', quantity: 1 }],
})

const filteredSkuOptions = computed(() => {
  if (!form.value.factoryId) return skuOptions.value
  return skuOptions.value.filter((opt) => opt.factoryId === form.value.factoryId)
})

const validFormLines = computed(() =>
  form.value.lines.filter((line) => line.skuCode && line.quantity > 0),
)

const formLinesSummary = computed(() =>
  t('ecommerce.inbound.formLinesSummary', {
    skuCount: validFormLines.value.length,
    totalQty: validFormLines.value.reduce((sum, line) => sum + line.quantity, 0),
  }),
)

const selectedFactoryName = computed(() => {
  if (!form.value.factoryId) return ''
  return factoryOptions.value.find((f) => f.id === form.value.factoryId)?.name ?? ''
})

const boardOrders = computed(() =>
  orders.value.filter((order) => order.status !== 'CANCELLED'),
)

const kanbanColumns = computed(() => {
  const draft: EcInboundOrder[] = []
  const pending: EcInboundOrder[] = []
  const confirmed: EcInboundOrder[] = []
  const today = todayDateString()

  for (const order of boardOrders.value) {
    if (order.status === 'CONFIRMED') {
      confirmed.push(order)
      continue
    }
    if (order.status !== 'DRAFT') continue
    const expected = formatDate(order.expectedDeliveryTime)
    if (expected !== '—' && expected <= today) {
      pending.push(order)
    } else {
      draft.push(order)
    }
  }

  return [
    { key: 'draft' as KanbanKey, tone: 'orange', title: t('ecommerce.inbound.kanbanDraft'), orders: draft },
    { key: 'pending' as KanbanKey, tone: 'blue', title: t('ecommerce.inbound.kanbanPending'), orders: pending },
    { key: 'confirmed' as KanbanKey, tone: 'green', title: t('ecommerce.inbound.kanbanConfirmed'), orders: confirmed },
  ]
})

const recentConfirmed = computed(() =>
  [...boardOrders.value]
    .filter((order) => order.status === 'CONFIRMED')
    .sort((a, b) => {
      const ta = a.actualReceiptTime ? new Date(a.actualReceiptTime).getTime() : 0
      const tb = b.actualReceiptTime ? new Date(b.actualReceiptTime).getTime() : 0
      return tb - ta
    })
    .slice(0, 8),
)

const inboundDetailData = computed<OrderDetailData | null>(() => {
  const order = detailOrder.value
  if (!order) return null
  return {
    orderNo: order.orderNo,
    factoryName: order.factoryName,
    status: order.status,
    remark: order.remark,
    orderTime: order.orderTime,
    expectedTime: order.expectedDeliveryTime,
    actualTime: order.actualReceiptTime,
    lines: order.lines,
  }
})

function detailSkuImageUrl(skuCode: string) {
  const opt = skuOptions.value.find((item) => item.skuCode === skuCode)
  return getEcommerceImageUrl(opt?.imageName)
}

function orderLineStats(order: EcInboundOrder) {
  const lines = order.lines ?? []
  return {
    skuCount: lines.length,
    totalQty: lines.reduce((sum, line) => sum + (line.quantity ?? 0), 0),
  }
}

function skuOptionLabel(opt: EcInventorySkuOption) {
  const parts = [opt.skuCode]
  if (opt.specName) parts.push(opt.specName)
  if (opt.productName) parts.push(opt.productName)
  return parts.join(' · ')
}

function statusLabel(status: string) {
  if (status === 'DRAFT') return t('ecommerce.inbound.statusDraft')
  if (status === 'CONFIRMED') return t('ecommerce.inbound.statusConfirmed')
  if (status === 'CANCELLED') return t('ecommerce.inbound.statusCancelled')
  return status
}

function statusTagType(status: string) {
  if (status === 'CONFIRMED') return 'success'
  if (status === 'CANCELLED') return 'info'
  return 'warning'
}

async function loadOrders() {
  loading.value = true
  try {
    const result = await fetchInboundOrders(
      keyword.value.trim() || undefined,
      undefined,
      factoryId.value,
      undefined,
      { page: 1, pageSize: 500 },
    )
    orders.value = result.records
  } finally {
    loading.value = false
  }
}

async function loadSkuOptions() {
  skuOptions.value = await fetchInventorySkuOptions(form.value.factoryId)
}

function resolveSkuOption(skuCode: string) {
  if (!skuCode) return undefined
  return filteredSkuOptions.value.find((item) => item.skuCode === skuCode)
    ?? skuOptions.value.find((item) => item.skuCode === skuCode)
}

function lineSkuSpecName(skuCode: string) {
  return resolveSkuOption(skuCode)?.specName ?? ''
}

function lineSkuProductName(skuCode: string) {
  return resolveSkuOption(skuCode)?.productName ?? ''
}

function lineSkuImageUrl(skuCode: string) {
  return getEcommerceImageUrl(resolveSkuOption(skuCode)?.imageName)
}

function resetForm() {
  form.value = {
    factoryId: undefined,
    remark: '',
    orderTime: todayDateString(),
    expectedDeliveryTime: tomorrowDateString(),
    lines: [{ skuCode: '', quantity: 1 }],
  }
  formStep.value = 0
}

function onFormDialogClosed() {
  formStep.value = 0
}

function validateBasicStep() {
  if (!form.value.orderTime) {
    ElMessage.warning(t('ecommerce.inbound.orderTimeRequired'))
    return false
  }
  if (!form.value.expectedDeliveryTime) {
    ElMessage.warning(t('ecommerce.inbound.expectedDeliveryTimeRequired'))
    return false
  }
  return true
}

function validateLinesStep() {
  if (!validFormLines.value.length) {
    ElMessage.warning(t('ecommerce.inbound.linesRequired'))
    return false
  }
  return true
}

function onFormNext() {
  if (formStep.value === 0 && !validateBasicStep()) return
  if (formStep.value === 1 && !validateLinesStep()) return
  formStep.value += 1
}

function onFormPrev() {
  if (formStep.value > 0) formStep.value -= 1
}

async function openCreate() {
  editingId.value = null
  resetForm()
  await loadSkuOptions()
  formVisible.value = true
}

async function openEdit(row: EcInboundOrder) {
  editingId.value = row.id
  formStep.value = 0
  form.value = {
    factoryId: row.factoryId ?? undefined,
    remark: row.remark || '',
    orderTime: row.orderTime ? formatDate(row.orderTime) : todayDateString(),
    expectedDeliveryTime: row.expectedDeliveryTime ? formatDate(row.expectedDeliveryTime) : '',
    lines: (row.lines || []).map((line) => ({ skuCode: line.skuCode, quantity: line.quantity })),
  }
  if (!form.value.lines.length) {
    form.value.lines = [{ skuCode: '', quantity: 1 }]
  }
  await loadSkuOptions()
  formVisible.value = true
}

function addLine() {
  form.value.lines.push({ skuCode: '', quantity: 1 })
}

function removeLine(index: number) {
  form.value.lines.splice(index, 1)
}

async function onFormFactoryChange() {
  await loadSkuOptions()
  form.value.lines.forEach((line) => {
    if (line.skuCode && !filteredSkuOptions.value.some((opt) => opt.skuCode === line.skuCode)) {
      line.skuCode = ''
    }
  })
}

async function onSave() {
  if (!validateBasicStep() || !validateLinesStep()) {
    if (!validateBasicStep()) formStep.value = 0
    else if (!validateLinesStep()) formStep.value = 1
    return
  }

  const lines = validFormLines.value
  saving.value = true
  try {
    const payload = {
      factoryId: form.value.factoryId,
      remark: form.value.remark?.trim() || undefined,
      orderTime: toApiDateTime(form.value.orderTime),
      expectedDeliveryTime: toApiDateTime(form.value.expectedDeliveryTime),
      lines: lines.map((line) => ({ skuCode: line.skuCode, quantity: line.quantity })),
    }
    if (editingId.value) {
      await updateInboundOrder(editingId.value, payload)
    } else {
      await createInboundOrder(payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    formVisible.value = false
    await loadOrders()
  } finally {
    saving.value = false
  }
}

async function openConfirm(row: EcInboundOrder) {
  const detail = await fetchInboundOrder(row.id)
  confirmingOrderId.value = detail.id
  confirmLines.value = (detail.lines || []).map((line) => ({
    lineId: line.id!,
    skuCode: line.skuCode,
    specName: line.specName,
    productName: line.productName,
    quantity: line.quantity,
    receivedQuantity: line.quantity,
  }))
  if (!confirmLines.value.length) {
    ElMessage.warning(t('ecommerce.inbound.linesRequired'))
    return
  }
  confirmVisible.value = true
}

async function onConfirmSubmit() {
  if (!confirmingOrderId.value) return

  const hasPositive = confirmLines.value.some((line) => line.receivedQuantity > 0)
  if (!hasPositive) {
    ElMessage.warning(t('ecommerce.inbound.receivedQtyRequired'))
    return
  }

  confirmSaving.value = true
  try {
    await confirmInboundOrder(confirmingOrderId.value, {
      lines: confirmLines.value.map((line) => ({
        lineId: line.lineId,
        receivedQuantity: line.receivedQuantity,
      })),
    })
    ElMessage.success(t('ecommerce.inbound.confirmSuccess'))
    confirmVisible.value = false
    await loadOrders()
    emit('refreshed')
  } finally {
    confirmSaving.value = false
  }
}

async function onCancel(row: EcInboundOrder) {
  await ElMessageBox.confirm(t('ecommerce.inbound.cancelConfirm', { orderNo: row.orderNo }), { type: 'warning' })
  await cancelInboundOrder(row.id)
  ElMessage.success(t('ecommerce.common.saved'))
  await loadOrders()
}

async function onDelete(row: EcInboundOrder) {
  await ElMessageBox.confirm(t('ecommerce.inbound.deleteConfirm', { orderNo: row.orderNo }), { type: 'warning' })
  await deleteInboundOrder(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  await loadOrders()
}

async function openDetail(row: EcInboundOrder) {
  detailVisible.value = true
  detailLoading.value = true
  detailOrder.value = null
  try {
    const order = await fetchInboundOrder(row.id)
    detailOrder.value = order
    skuOptions.value = await fetchInventorySkuOptions(order.factoryId ?? undefined)
  } finally {
    detailLoading.value = false
  }
}

function onDetailEdit() {
  if (!detailOrder.value) return
  const row = detailOrder.value
  detailVisible.value = false
  openEdit(row)
}

async function onDetailConfirm() {
  if (!detailOrder.value) return
  const row = detailOrder.value
  detailVisible.value = false
  await openConfirm(row)
}

function onDetailPrint() {
  printPurchaseVisible.value = true
}

async function onDetailCancel() {
  if (!detailOrder.value) return
  await onCancel(detailOrder.value)
  detailVisible.value = false
}

async function onDetailDelete() {
  if (!detailOrder.value) return
  await onDelete(detailOrder.value)
  detailVisible.value = false
}

function onClosed() {
  keyword.value = ''
  factoryId.value = undefined
}

let keywordTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (keywordTimer) clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => loadOrders(), 300)
})

watch(factoryId, () => loadOrders())

watch(
  () => props.modelValue,
  async (open) => {
    if (open) {
      factoryOptions.value = await fetchFactoryOptions('PRODUCTION')
      await loadOrders()
    }
  },
)

async function openOrderById(orderId: number) {
  const order = await fetchInboundOrder(orderId)
  keyword.value = order.orderNo
  await loadOrders()
  await openDetail(order)
}

defineExpose({ openOrderById })
</script>

<style scoped lang="scss">
.inbound-workbench__toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
  align-items: center;
}

.inbound-kanban {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  min-height: 360px;
  align-items: start;
}

.inbound-kanban__column {
  display: flex;
  flex-direction: column;
  min-height: 320px;
  border: 1px solid #e8ecf2;
  border-radius: 14px;
  background: #f8fafc;
  overflow: hidden;
}

.inbound-kanban__column-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  font-weight: 600;

  &.is-orange {
    background: #fff7ed;
    color: #c2410c;
    border-bottom: 1px solid #fed7aa;
  }

  &.is-blue {
    background: #eff6ff;
    color: #1d4ed8;
    border-bottom: 1px solid #bfdbfe;
  }

  &.is-green {
    background: #f0fdf4;
    color: #15803d;
    border-bottom: 1px solid #bbf7d0;
  }
}

.inbound-kanban__column-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgb(255 255 255 / 72%);
  font-size: 13px;
}

.inbound-kanban__column-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  max-height: 52vh;
  overflow: auto;
}

.inbound-kanban-card {
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #fff;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
  cursor: pointer;
  transition: box-shadow 0.15s ease, border-color 0.15s ease;

  &:hover {
    border-color: #cbd5e1;
    box-shadow: 0 4px 12px rgb(15 23 42 / 8%);
  }

  &.is-orange {
    border-left: 3px solid #f59e0b;
  }

  &.is-blue {
    border-left: 3px solid #3b82f6;
  }

  &.is-green {
    border-left: 3px solid #22c55e;
  }
}

.inbound-kanban-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.inbound-kanban-card__order-no {
  font-size: 13px;
  font-weight: 700;
  color: #111827;
  word-break: break-all;
}

.inbound-kanban-card__factory {
  margin: 0 0 8px;
  font-size: 13px;
  color: #374151;
}

.inbound-kanban-card__meta,
.inbound-kanban-card__stats {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #6b7280;
}

.inbound-kanban-card__stats {
  margin-top: 8px;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 10px;
  font-weight: 600;
  color: #4b5563;
}

.inbound-kanban-card__remark {
  margin: 8px 0 0;
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.4;
}

.inbound-kanban-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}

.inbound-timeline {
  margin-top: 20px;
  padding: 16px 18px;
  border: 1px solid #e8ecf2;
  border-radius: 14px;
  background: #fff;
}

.inbound-timeline__title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
}

.inbound-timeline__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.inbound-timeline__item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  cursor: pointer;
  padding: 6px 8px;
  margin: -6px -8px;
  border-radius: 8px;
  transition: background 0.15s ease;

  &:hover {
    background: #f8fafc;
  }
}

.inbound-timeline__dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 50%;
  background: #22c55e;
  flex-shrink: 0;
}

.inbound-timeline__line1 {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
  color: #374151;

  strong {
    color: #111827;
  }
}

.inbound-timeline__line2 {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.lines-toolbar {
  margin-bottom: 8px;
}

.inbound-form-wizard__center {
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
}

.inbound-form-wizard__steps {
  margin-bottom: 24px;
  --el-color-success: #15803d;
  --el-color-primary: #1d4ed8;

  :deep(.el-step__head.is-finish) {
    color: #15803d;
    border-color: #15803d;
  }

  :deep(.el-step__head.is-finish .el-step__icon) {
    background: #15803d;
    border-color: #15803d;
  }

  :deep(.el-step__head.is-finish .el-step__line-inner) {
    background-color: #15803d;
    border-color: #15803d;
  }

  :deep(.el-step__title.is-finish) {
    color: #15803d;
  }

  :deep(.el-step__head.is-process) {
    color: #1d4ed8;
    border-color: #1d4ed8;
  }

  :deep(.el-step__head.is-process .el-step__icon) {
    background: #1d4ed8;
    border-color: #1d4ed8;
    color: #fff;
  }

  :deep(.el-step__title.is-process) {
    color: #1d4ed8;
    font-weight: 600;
  }
}

.inbound-form-wizard__body {
  min-height: 320px;
}

.inbound-form-step__basic-form {
  :deep(.el-form-item) {
    display: flex;
    align-items: center;
    margin-bottom: 14px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(.el-form-item__label) {
    display: inline-flex;
    align-items: center;
    justify-content: flex-end;
    height: auto;
    line-height: 1.4;
    padding-bottom: 0;
  }

  :deep(.el-form-item__content) {
    flex: 1;
    min-width: 0;
    line-height: normal;
  }
}

.inbound-form-step__field {
  margin-bottom: 0;
}

.inbound-form-step__lines-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.inbound-form-step__lines-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.inbound-form-step__add-line-btn {
  --el-button-bg-color: #15803d;
  --el-button-border-color: #15803d;
  --el-button-hover-bg-color: #166534;
  --el-button-hover-border-color: #166534;
  --el-button-active-bg-color: #14532d;
  --el-button-active-border-color: #14532d;
}

.inbound-line-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  overflow: auto;
  padding-right: 2px;
}

.inbound-line-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);

  &.is-readonly {
    background: #f8fafc;
  }
}

.inbound-line-card__thumb {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
}

.inbound-line-card__image {
  width: 64px;
  height: 64px;
}

.inbound-line-card__image-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 11px;
  color: #9ca3af;
  background: #f3f4f6;
  text-align: center;
  line-height: 1.3;
  padding: 4px;
}

.inbound-line-card__field {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;

  &.is-sku {
    flex: 1;
    align-items: center;
  }

  &.is-qty {
    flex-shrink: 0;
  }
}

.inbound-line-card__label {
  flex-shrink: 0;
  width: 72px;
  font-size: 14px;
  line-height: 1.4;
  color: var(--el-text-color-regular);
  text-align: right;
}

.inbound-line-card__control {
  flex: 1;
  min-width: 0;
}

.inbound-line-card__sku {
  width: 100%;
}

.inbound-line-card__sku-text {
  font-size: 14px;
  color: #111827;
}

.inbound-line-card__subtitle {
  margin: 6px 0 0;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.4;
}

.inbound-line-card__remove {
  flex-shrink: 0;
  align-self: center;
  margin-top: 0;
}

.inbound-line-card__qty-value {
  font-size: 16px;
  color: #111827;
}

.inbound-form-wizard__floating-summary {
  margin-top: 14px;
  padding: 10px 16px;
  border-radius: 999px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
}

.inbound-form-wizard__footer {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.inbound-form-confirm__section + .inbound-form-confirm__section {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid #f1f5f9;
}

.inbound-form-confirm__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.inbound-form-confirm__heading-leading {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.inbound-form-confirm__heading-bar {
  width: 4px;
  height: 16px;
  border-radius: 2px;
  background: #1d4ed8;
  flex-shrink: 0;
}

.inbound-form-confirm__badge {
  padding: 2px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 500;
}

.inbound-form-confirm__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 16px;
  margin: 0;
}

.inbound-form-confirm__item {
  margin: 0;

  dt {
    margin: 0 0 4px;
    font-size: 12px;
    color: #6b7280;
  }

  dd {
    margin: 0;
    font-size: 14px;
    color: #111827;
    word-break: break-word;
  }

  &.is-full {
    grid-column: 1 / -1;
  }
}

.inbound-form-confirm__lines {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 320px;
  overflow: auto;
  padding-right: 2px;
}

.inbound-confirm-line-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.inbound-confirm-line-card__thumb {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.inbound-confirm-line-card__image {
  width: 64px;
  height: 64px;
}

.inbound-confirm-line-card__image-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 12px;
  color: #9ca3af;
  background: #f3f4f6;
  text-align: center;
  line-height: 1.3;
  padding: 4px;
}

.inbound-confirm-line-card__info {
  flex: 1;
  min-width: 0;
}

.inbound-confirm-line-card__sku {
  display: block;
  font-size: 20px;
  font-weight: 800;
  color: #000;
  line-height: 1.25;
  letter-spacing: -0.01em;
}

.inbound-confirm-line-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.inbound-confirm-line-card__tag {
  max-width: 100%;

  &.is-spec {
    --el-tag-bg-color: #f1f5f9;
    --el-tag-border-color: #e2e8f0;
    --el-tag-text-color: #475569;
  }

  &.is-product {
    font-weight: 600;
  }
}

.inbound-confirm-line-card__qty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  align-self: stretch;
  flex-shrink: 0;
  min-width: 52px;
  padding: 0 4px 0 16px;
  margin-left: 4px;
  border-left: 1px solid #e5e7eb;
}

.inbound-confirm-line-card__qty-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.inbound-confirm-line-card__qty-value {
  font-size: 28px;
  font-weight: 700;
  color: #ea580c;
  line-height: 1;
}

.inbound-confirm-line-card__qty-unit {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1;
}

.confirm-hint {
  margin: 0 0 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

@media (max-width: 1100px) {
  .inbound-kanban {
    grid-template-columns: 1fr;
  }

  .inbound-form-confirm__grid {
    grid-template-columns: 1fr;
  }

  .inbound-line-card {
    flex-wrap: wrap;
  }

  .inbound-line-card__field.is-qty {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
