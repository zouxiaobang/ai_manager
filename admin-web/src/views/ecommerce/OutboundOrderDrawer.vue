<template>
  <el-drawer
    v-model="visible"
    :title="t('ecommerce.outbound.orderWorkbenchTitle')"
    size="1120px"
    destroy-on-close
    class="outbound-order-drawer"
    @closed="onClosed"
  >
    <div class="outbound-workbench">
      <div class="outbound-workbench__toolbar">
        <el-input
          v-model="keyword"
          :placeholder="t('ecommerce.outbound.orderSearchPlaceholder')"
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
          <el-option v-for="f in productionFactoryOptions" :key="f.id" :label="f.name" :value="f.id" />
        </el-select>
        <el-button type="primary" size="large" @click="openCreate">
          {{ t('ecommerce.outbound.createOrder') }}
        </el-button>
      </div>

      <div v-loading="loading" class="outbound-kanban">
        <section v-for="column in kanbanColumns" :key="column.key" class="outbound-kanban__column">
          <header class="outbound-kanban__column-head" :class="`is-${column.tone}`">
            <span class="outbound-kanban__column-title">{{ column.title }}</span>
            <span class="outbound-kanban__column-count">{{ column.orders.length }}</span>
          </header>
          <div class="outbound-kanban__column-body">
            <article
              v-for="order in column.orders"
              :key="order.id"
              class="outbound-kanban-card"
              :class="`is-${column.tone}`"
              @click="openDetail(order)"
            >
              <div class="outbound-kanban-card__head">
                <span class="outbound-kanban-card__order-no">{{ order.orderNo }}</span>
                <el-tag :type="statusTagType(order.status)" size="small">{{ statusLabel(order.status) }}</el-tag>
              </div>
              <p class="outbound-kanban-card__factory">{{ order.factoryName || '—' }}</p>
              <div class="outbound-kanban-card__meta">
                <span>{{ t('ecommerce.outbound.orderTime') }} {{ formatDate(order.orderTime) }}</span>
                <span>{{ t('ecommerce.outbound.expectedShipTime') }} {{ formatDate(order.expectedShipTime) }}</span>
              </div>
              <div class="outbound-kanban-card__stats">
                <span>{{ t('ecommerce.outbound.skuLineCount', { count: orderLineStats(order).skuCount }) }}</span>
                <span>{{ t('ecommerce.outbound.totalOrderedQty', { count: orderLineStats(order).totalQty }) }}</span>
              </div>
              <p v-if="order.remark" class="outbound-kanban-card__remark">{{ order.remark }}</p>
              <div v-if="order.status === 'DRAFT'" class="outbound-kanban-card__actions">
                <el-button size="small" @click.stop="openEdit(order)">{{ t('ecommerce.inventory.edit') }}</el-button>
                <el-button size="small" type="success" @click.stop="openConfirm(order)">
                  {{ t('ecommerce.outbound.confirm') }}
                </el-button>
                <el-button size="small" type="warning" @click.stop="onCancel(order)">
                  {{ t('ecommerce.outbound.cancel') }}
                </el-button>
                <el-button size="small" type="danger" link @click.stop="onDelete(order)">
                  {{ t('ecommerce.inventory.delete') }}
                </el-button>
              </div>
              <div v-else-if="order.status === 'CONFIRMED'" class="outbound-kanban-card__meta">
                <span>{{ t('ecommerce.outbound.actualShipTime') }} {{ formatDate(order.actualShipTime) }}</span>
              </div>
            </article>
            <el-empty v-if="!column.orders.length" :description="t('ecommerce.outbound.kanbanEmpty')" :image-size="56" />
          </div>
        </section>
      </div>

      <section class="outbound-timeline">
        <h3 class="outbound-timeline__title">{{ t('ecommerce.outbound.recentShipActivity') }}</h3>
        <div v-if="recentConfirmed.length" class="outbound-timeline__list">
          <div v-for="order in recentConfirmed" :key="order.id" class="outbound-timeline__item" @click="openDetail(order)">
            <span class="outbound-timeline__dot" />
            <div class="outbound-timeline__body">
              <div class="outbound-timeline__line1">
                <strong>{{ order.orderNo }}</strong>
                <span>{{ order.factoryName || '—' }}</span>
              </div>
              <div class="outbound-timeline__line2">
                {{ t('ecommerce.outbound.actualShipTime') }} {{ formatDateTime(order.actualShipTime) }}
                · {{ t('ecommerce.outbound.totalOrderedQty', { count: orderLineStats(order).totalQty }) }}
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else :description="t('ecommerce.outbound.recentShipEmpty')" :image-size="48" />
      </section>
    </div>

    <el-dialog
      v-model="formVisible"
      :title="editingId ? t('ecommerce.outbound.editOrderTitle') : t('ecommerce.outbound.createOrderTitle')"
      width="820px"
      destroy-on-close
      append-to-body
      class="outbound-form-dialog"
      @closed="onFormDialogClosed"
    >
      <div class="outbound-form-wizard__center">
        <el-steps :active="formStep" align-center finish-status="success" class="outbound-form-wizard__steps">
          <el-step :title="t('ecommerce.outbound.formStepBasic')" />
          <el-step :title="t('ecommerce.outbound.formStepLines')" />
          <el-step :title="t('ecommerce.outbound.formStepConfirm')" />
        </el-steps>

        <div class="outbound-form-wizard__body">
        <section v-show="formStep === 0" class="outbound-form-step">
          <el-form label-width="100px" label-position="right" class="outbound-form-step__basic-form">
            <el-form-item :label="t('ecommerce.outbound.orderTime')" required class="outbound-form-step__field">
              <el-date-picker
                v-model="form.orderTime"
                type="date"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                :placeholder="t('ecommerce.outbound.orderTime')"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item :label="t('ecommerce.outbound.expectedShipTime')" required class="outbound-form-step__field">
              <el-date-picker
                v-model="form.expectedShipTime"
                type="date"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                :placeholder="t('ecommerce.outbound.expectedShipTime')"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item :label="t('ecommerce.inventory.factory')" class="outbound-form-step__field">
              <el-select
                v-model="form.factoryId"
                clearable
                filterable
                :placeholder="t('ecommerce.outbound.factoryNamePlaceholder')"
                style="width: 100%"
                @change="onFormFactoryChange"
              >
                <el-option v-for="f in productionFactoryOptions" :key="f.id" :label="f.name" :value="f.id" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('ecommerce.outbound.customer')" class="outbound-form-step__field">
              <el-select
                v-model="form.customerFactoryId"
                clearable
                filterable
                :placeholder="t('ecommerce.outbound.customerPlaceholder')"
                style="width: 100%"
              >
                <el-option v-for="f in customerFactoryOptions" :key="f.id" :label="f.name" :value="f.id" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('ecommerce.outbound.remark')" class="outbound-form-step__field">
              <el-input v-model="form.remark" type="textarea" :rows="3" :placeholder="t('ecommerce.outbound.remark')" />
            </el-form-item>
          </el-form>
        </section>

        <section v-show="formStep === 1" class="outbound-form-step">
          <div class="outbound-form-step__lines-head">
            <span class="outbound-form-step__lines-title">{{ t('ecommerce.outbound.lines') }}</span>
            <el-button type="success" size="large" class="outbound-form-step__add-line-btn" @click="addLine">
              <el-icon><Plus /></el-icon>
              {{ t('ecommerce.outbound.addLine') }}
            </el-button>
          </div>
          <div v-if="form.lines.length" class="outbound-line-cards">
            <article v-for="(line, index) in form.lines" :key="index" class="outbound-line-card">
              <div class="outbound-line-card__thumb">
                <el-image
                  v-if="lineSkuImageUrl(line.skuCode)"
                  :src="lineSkuImageUrl(line.skuCode)"
                  fit="cover"
                  class="outbound-line-card__image"
                >
                  <template #error>
                    <div class="outbound-line-card__image-fallback">{{ t('ecommerce.outbound.noSkuImage') }}</div>
                  </template>
                </el-image>
                <div v-else class="outbound-line-card__image-fallback">{{ t('ecommerce.outbound.noSkuImage') }}</div>
              </div>
              <div class="outbound-line-card__field is-sku">
                <div class="outbound-line-card__control">
                  <el-select
                    v-model="line.skuCode"
                    filterable
                    size="large"
                    :placeholder="t('ecommerce.inventory.skuCodePlaceholder')"
                    class="outbound-line-card__sku"
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
              <div class="outbound-line-card__field is-qty">
                <label class="outbound-line-card__label">{{ t('ecommerce.outbound.orderedQty') }}</label>
                <el-input-number
                  v-model="line.quantity"
                  :min="1"
                  :max="lineMaxQty(line.skuCode)"
                  :step="1"
                  size="large"
                  controls-position="right"
                />
              </div>
              <el-button
                link
                type="danger"
                size="large"
                class="outbound-line-card__remove"
                :disabled="form.lines.length <= 1"
                @click="removeLine(index)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </article>
          </div>
          <el-empty v-else :description="t('ecommerce.outbound.linesRequired')" :image-size="64" />
        </section>

        <section v-show="formStep === 2" class="outbound-form-step">
          <div class="outbound-form-confirm">
            <div class="outbound-form-confirm__section">
              <h4 class="outbound-form-confirm__heading">
                <span class="outbound-form-confirm__heading-leading">
                  <span class="outbound-form-confirm__heading-bar" aria-hidden="true" />
                  <span>{{ t('ecommerce.outbound.formStepBasic') }}</span>
                </span>
              </h4>
              <dl class="outbound-form-confirm__grid">
                <div class="outbound-form-confirm__item">
                  <dt>{{ t('ecommerce.outbound.orderTime') }}</dt>
                  <dd>{{ form.orderTime || '—' }}</dd>
                </div>
                <div class="outbound-form-confirm__item">
                  <dt>{{ t('ecommerce.outbound.expectedShipTime') }}</dt>
                  <dd>{{ form.expectedShipTime || '—' }}</dd>
                </div>
                <div class="outbound-form-confirm__item">
                  <dt>{{ t('ecommerce.inventory.factory') }}</dt>
                  <dd>{{ selectedFactoryName || '—' }}</dd>
                </div>
                <div class="outbound-form-confirm__item">
                  <dt>{{ t('ecommerce.outbound.customer') }}</dt>
                  <dd>{{ selectedCustomerName || '—' }}</dd>
                </div>
                <div class="outbound-form-confirm__item is-full">
                  <dt>{{ t('ecommerce.outbound.remark') }}</dt>
                  <dd>{{ form.remark?.trim() || '—' }}</dd>
                </div>
              </dl>
            </div>
            <div class="outbound-form-confirm__section">
              <h4 class="outbound-form-confirm__heading">
                <span class="outbound-form-confirm__heading-leading">
                  <span class="outbound-form-confirm__heading-bar" aria-hidden="true" />
                  <span>{{ t('ecommerce.outbound.formStepLines') }}</span>
                </span>
                <span class="outbound-form-confirm__badge">{{ formLinesSummary }}</span>
              </h4>
              <div class="outbound-form-confirm__lines">
                <article v-for="(line, index) in validFormLines" :key="index" class="outbound-confirm-line-card">
                  <div class="outbound-confirm-line-card__thumb">
                    <el-image
                      v-if="lineSkuImageUrl(line.skuCode)"
                      :src="lineSkuImageUrl(line.skuCode)"
                      fit="cover"
                      class="outbound-confirm-line-card__image"
                    >
                      <template #error>
                        <div class="outbound-confirm-line-card__image-fallback">{{ t('ecommerce.outbound.noSkuImage') }}</div>
                      </template>
                    </el-image>
                    <div v-else class="outbound-confirm-line-card__image-fallback">{{ t('ecommerce.outbound.noSkuImage') }}</div>
                  </div>
                  <div class="outbound-confirm-line-card__info">
                    <strong class="outbound-confirm-line-card__sku">{{ line.skuCode }}</strong>
                    <div
                      v-if="lineSkuSpecName(line.skuCode) || lineSkuProductName(line.skuCode)"
                      class="outbound-confirm-line-card__tags"
                    >
                      <el-tag
                        v-if="lineSkuSpecName(line.skuCode)"
                        size="small"
                        effect="light"
                        class="outbound-confirm-line-card__tag is-spec"
                      >
                        {{ lineSkuSpecName(line.skuCode) }}
                      </el-tag>
                      <el-tag
                        v-if="lineSkuProductName(line.skuCode)"
                        size="small"
                        effect="light"
                        type="success"
                        class="outbound-confirm-line-card__tag is-product"
                      >
                        {{ lineSkuProductName(line.skuCode) }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="outbound-confirm-line-card__qty">
                    <div class="outbound-confirm-line-card__qty-block">
                      <strong class="outbound-confirm-line-card__qty-value">{{ line.quantity }}</strong>
                      <span class="outbound-confirm-line-card__qty-unit">{{ t('ecommerce.inventory.unitPiece') }}</span>
                    </div>
                  </div>
                </article>
              </div>
            </div>
          </div>
        </section>
        </div>

        <div v-if="formStep === 1 && validFormLines.length" class="outbound-form-wizard__floating-summary">
          {{ formLinesSummary }}
        </div>
      </div>

      <template #footer>
        <div class="outbound-form-wizard__footer">
          <el-button @click="formVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
          <el-button v-if="formStep > 0" @click="onFormPrev">{{ t('ecommerce.outbound.prevStep') }}</el-button>
          <el-button v-if="formStep < 2" type="primary" @click="onFormNext">{{ t('ecommerce.outbound.nextStep') }}</el-button>
          <el-button v-else type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.outbound.confirmSave') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="confirmVisible"
      :title="t('ecommerce.outbound.confirmTitle')"
      width="720px"
      destroy-on-close
      append-to-body
    >
      <p class="confirm-hint">{{ t('ecommerce.outbound.confirmHint') }}</p>
      <el-table :data="confirmLines" border size="small" max-height="360">
        <el-table-column prop="skuCode" :label="t('ecommerce.inventory.skuCode')" width="120" />
        <el-table-column prop="specName" :label="t('ecommerce.inventory.specName')" min-width="100" show-overflow-tooltip />
        <el-table-column prop="productName" :label="t('ecommerce.inventory.productName')" min-width="120" show-overflow-tooltip />
        <el-table-column :label="t('ecommerce.outbound.orderedQty')" width="100" align="right">
          <template #default="{ row }">{{ row.quantity }}</template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.outbound.shippedQty')" width="160">
          <template #default="{ row }">
            <el-input-number
              v-model="row.shippedQuantity"
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
          {{ t('ecommerce.outbound.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <OrderDetailDialog
      v-model="detailVisible"
      :loading="detailLoading"
      variant="outbound"
      :order="outboundDetailData"
      :resolve-sku-image="detailSkuImageUrl"
      @edit="onDetailEdit"
      @confirm="onDetailConfirm"
      @cancel="onDetailCancel"
      @delete="onDetailDelete"
      @print="onDetailPrint"
    />

    <OutboundDeliveryNotePrintDialog
      v-model="printDeliveryVisible"
      :order="detailOrder"
      :resolve-sku-image="detailSkuImageUrl"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import {
  cancelOutboundOrder,
  confirmOutboundOrder,
  createOutboundOrder,
  deleteOutboundOrder,
  fetchOutboundOrder,
  fetchOutboundOrders,
  updateOutboundOrder,
  type EcOutboundOrder,
} from '@/api/ecommerce/outbound'
import { fetchInventorySkuOptions, type EcInventorySkuOption } from '@/api/ecommerce/inventory'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'
import { fetchFactoryOptions, type EcFactory } from '@/api/ecommerce/factory'
import { formatDate, formatDateTime, todayDateString, tomorrowDateString, toApiDateTime } from '@/utils/date'
import OrderDetailDialog, { type OrderDetailData } from './OrderDetailDialog.vue'
import OutboundDeliveryNotePrintDialog from './OutboundDeliveryNotePrintDialog.vue'

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
const orders = ref<EcOutboundOrder[]>([])
const factoryOptionList = ref<EcFactory[]>([])

const productionFactoryOptions = computed(() =>
  factoryOptionList.value.filter((f) => f.factoryType !== 'CUSTOMER'),
)

const customerFactoryOptions = computed(() =>
  factoryOptionList.value.filter((f) => f.factoryType === 'CUSTOMER'),
)

const skuOptions = ref<EcInventorySkuOption[]>([])

async function loadFactoryOptionLists() {
  const [production, customer] = await Promise.all([
    fetchFactoryOptions('PRODUCTION'),
    fetchFactoryOptions('CUSTOMER'),
  ])
  const productionIds = new Set(production.map((f) => f.id))
  factoryOptionList.value = [
    ...production.filter((f) => f.factoryType !== 'CUSTOMER'),
    ...customer.filter((f) => f.factoryType === 'CUSTOMER' && !productionIds.has(f.id)),
  ]
}

function sanitizeFormFactorySelections() {
  if (
    form.value.factoryId
    && !productionFactoryOptions.value.some((f) => f.id === form.value.factoryId)
  ) {
    form.value.factoryId = undefined
  }
  if (
    form.value.customerFactoryId
    && !customerFactoryOptions.value.some((f) => f.id === form.value.customerFactoryId)
  ) {
    form.value.customerFactoryId = undefined
  }
}

const formVisible = ref(false)
const formStep = ref(0)
const confirmVisible = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailOrder = ref<EcOutboundOrder | null>(null)
const printDeliveryVisible = ref(false)
const editingId = ref<number | null>(null)
const confirmingOrderId = ref<number | null>(null)
const confirmLines = ref<Array<{
  lineId: number
  skuCode: string
  specName?: string
  productName?: string
  quantity: number
  shippedQuantity: number
}>>([])

const form = ref({
  factoryId: undefined as number | undefined,
  customerFactoryId: undefined as number | undefined,
  remark: '',
  orderTime: '',
  expectedShipTime: '',
  lines: [{ skuCode: '', quantity: 1 }],
})

const filteredSkuOptions = computed(() => {
  const withStock = skuOptions.value.filter((opt) => opt.hasInventory && (opt.quantity ?? 0) > 0)
  if (!form.value.factoryId) return withStock
  return withStock.filter((opt) => opt.factoryId === form.value.factoryId)
})

const validFormLines = computed(() =>
  form.value.lines.filter((line) => line.skuCode && line.quantity > 0),
)

const formLinesSummary = computed(() =>
  t('ecommerce.outbound.formLinesSummary', {
    skuCount: validFormLines.value.length,
    totalQty: validFormLines.value.reduce((sum, line) => sum + line.quantity, 0),
  }),
)

const selectedFactoryName = computed(() => {
  if (!form.value.factoryId) return ''
  return productionFactoryOptions.value.find((f) => f.id === form.value.factoryId)?.name ?? ''
})

const selectedCustomerName = computed(() => {
  if (!form.value.customerFactoryId) return ''
  return customerFactoryOptions.value.find((f) => f.id === form.value.customerFactoryId)?.name ?? ''
})

const boardOrders = computed(() =>
  orders.value.filter((order) => order.status !== 'CANCELLED'),
)

const kanbanColumns = computed(() => {
  const draft: EcOutboundOrder[] = []
  const pending: EcOutboundOrder[] = []
  const confirmed: EcOutboundOrder[] = []
  const today = todayDateString()

  for (const order of boardOrders.value) {
    if (order.status === 'CONFIRMED') {
      confirmed.push(order)
      continue
    }
    if (order.status !== 'DRAFT') continue
    const expected = formatDate(order.expectedShipTime)
    if (expected !== '—' && expected <= today) {
      pending.push(order)
    } else {
      draft.push(order)
    }
  }

  return [
    { key: 'draft' as KanbanKey, tone: 'orange', title: t('ecommerce.outbound.kanbanDraft'), orders: draft },
    { key: 'pending' as KanbanKey, tone: 'blue', title: t('ecommerce.outbound.kanbanPending'), orders: pending },
    { key: 'confirmed' as KanbanKey, tone: 'green', title: t('ecommerce.outbound.kanbanConfirmed'), orders: confirmed },
  ]
})

const recentConfirmed = computed(() =>
  [...boardOrders.value]
    .filter((order) => order.status === 'CONFIRMED')
    .sort((a, b) => {
      const ta = a.actualShipTime ? new Date(a.actualShipTime).getTime() : 0
      const tb = b.actualShipTime ? new Date(b.actualShipTime).getTime() : 0
      return tb - ta
    })
    .slice(0, 8),
)

const outboundDetailData = computed<OrderDetailData | null>(() => {
  const order = detailOrder.value
  if (!order) return null
  return {
    orderNo: order.orderNo,
    factoryName: order.factoryName,
    status: order.status,
    remark: order.remark,
    orderTime: order.orderTime,
    expectedTime: order.expectedShipTime,
    actualTime: order.actualShipTime,
    customer: order.customerFactoryId || order.customerName
      ? {
          name: order.customerName,
          contactName: order.customerContactName,
          contactPhone: order.customerContactPhone,
          address: order.customerAddress,
        }
      : null,
    lines: order.lines,
  }
})

function detailSkuImageUrl(skuCode: string) {
  const opt = skuOptions.value.find((item) => item.skuCode === skuCode)
  return getEcommerceImageUrl(opt?.imageName)
}

function orderLineStats(order: EcOutboundOrder) {
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
  if (status === 'DRAFT') return t('ecommerce.outbound.statusDraft')
  if (status === 'CONFIRMED') return t('ecommerce.outbound.statusConfirmed')
  if (status === 'CANCELLED') return t('ecommerce.outbound.statusCancelled')
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
    const result = await fetchOutboundOrders(
      keyword.value.trim() || undefined,
      undefined,
      factoryId.value,
      { page: 1, pageSize: 500 },
    )
    orders.value = result.records
  } finally {
    loading.value = false
  }
}

async function loadSkuOptions() {
  const options = await fetchInventorySkuOptions(form.value.factoryId)
  skuOptions.value = options.filter((opt) => opt.hasInventory && (opt.quantity ?? 0) > 0)
}

function lineMaxQty(skuCode: string) {
  return resolveSkuOption(skuCode)?.quantity ?? undefined
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
    customerFactoryId: undefined,
    remark: '',
    orderTime: todayDateString(),
    expectedShipTime: tomorrowDateString(),
    lines: [{ skuCode: '', quantity: 1 }],
  }
  formStep.value = 0
}

function onFormDialogClosed() {
  formStep.value = 0
}

function validateBasicStep() {
  if (!form.value.orderTime) {
    ElMessage.warning(t('ecommerce.outbound.orderTimeRequired'))
    return false
  }
  if (!form.value.expectedShipTime) {
    ElMessage.warning(t('ecommerce.outbound.expectedShipTimeRequired'))
    return false
  }
  return true
}

function validateLinesStep() {
  if (!validFormLines.value.length) {
    ElMessage.warning(t('ecommerce.outbound.linesRequired'))
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
  await Promise.all([loadFactoryOptionLists(), loadSkuOptions()])
  formVisible.value = true
}

async function openEdit(row: EcOutboundOrder) {
  editingId.value = row.id
  formStep.value = 0
  form.value = {
    factoryId: row.factoryId ?? undefined,
    customerFactoryId: row.customerFactoryId ?? undefined,
    remark: row.remark || '',
    orderTime: row.orderTime ? formatDate(row.orderTime) : todayDateString(),
    expectedShipTime: row.expectedShipTime ? formatDate(row.expectedShipTime) : '',
    lines: (row.lines || []).map((line) => ({ skuCode: line.skuCode, quantity: line.quantity })),
  }
  if (!form.value.lines.length) {
    form.value.lines = [{ skuCode: '', quantity: 1 }]
  }
  await Promise.all([loadFactoryOptionLists(), loadSkuOptions()])
  sanitizeFormFactorySelections()
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
      customerFactoryId: form.value.customerFactoryId,
      remark: form.value.remark?.trim() || undefined,
      orderTime: toApiDateTime(form.value.orderTime),
      expectedShipTime: toApiDateTime(form.value.expectedShipTime),
      lines: lines.map((line) => ({ skuCode: line.skuCode, quantity: line.quantity })),
    }
    if (editingId.value) {
      await updateOutboundOrder(editingId.value, payload)
    } else {
      await createOutboundOrder(payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    formVisible.value = false
    await loadOrders()
  } finally {
    saving.value = false
  }
}

async function openConfirm(row: EcOutboundOrder) {
  const detail = await fetchOutboundOrder(row.id)
  confirmingOrderId.value = detail.id
  confirmLines.value = (detail.lines || []).map((line) => ({
    lineId: line.id!,
    skuCode: line.skuCode,
    specName: line.specName,
    productName: line.productName,
    quantity: line.quantity,
    shippedQuantity: line.quantity,
  }))
  if (!confirmLines.value.length) {
    ElMessage.warning(t('ecommerce.outbound.linesRequired'))
    return
  }
  confirmVisible.value = true
}

async function onConfirmSubmit() {
  if (!confirmingOrderId.value) return

  const hasPositive = confirmLines.value.some((line) => line.shippedQuantity > 0)
  if (!hasPositive) {
    ElMessage.warning(t('ecommerce.outbound.shippedQtyRequired'))
    return
  }

  confirmSaving.value = true
  try {
    await confirmOutboundOrder(confirmingOrderId.value, {
      lines: confirmLines.value.map((line) => ({
        lineId: line.lineId,
        shippedQuantity: line.shippedQuantity,
      })),
    })
    ElMessage.success(t('ecommerce.outbound.confirmSuccess'))
    confirmVisible.value = false
    await loadOrders()
    emit('refreshed')
  } finally {
    confirmSaving.value = false
  }
}

async function onCancel(row: EcOutboundOrder) {
  await ElMessageBox.confirm(t('ecommerce.outbound.cancelConfirm', { orderNo: row.orderNo }), { type: 'warning' })
  await cancelOutboundOrder(row.id)
  ElMessage.success(t('ecommerce.common.saved'))
  await loadOrders()
}

async function onDelete(row: EcOutboundOrder) {
  await ElMessageBox.confirm(t('ecommerce.outbound.deleteConfirm', { orderNo: row.orderNo }), { type: 'warning' })
  await deleteOutboundOrder(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  await loadOrders()
}

async function openDetail(row: EcOutboundOrder) {
  detailVisible.value = true
  detailLoading.value = true
  detailOrder.value = null
  try {
    const order = await fetchOutboundOrder(row.id)
    detailOrder.value = order
    const options = await fetchInventorySkuOptions(order.factoryId ?? undefined)
    skuOptions.value = options
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
  printDeliveryVisible.value = true
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
      await loadFactoryOptionLists()
      await loadOrders()
    }
  },
)

async function openOrderById(orderId: number) {
  const order = await fetchOutboundOrder(orderId)
  keyword.value = order.orderNo
  await loadOrders()
  await openDetail(order)
}

defineExpose({ openOrderById })
</script>

<style scoped lang="scss">
.outbound-workbench__toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
  align-items: center;
}

.outbound-kanban {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  min-height: 360px;
  align-items: start;
}

.outbound-kanban__column {
  display: flex;
  flex-direction: column;
  min-height: 320px;
  border: 1px solid #e8ecf2;
  border-radius: 14px;
  background: #f8fafc;
  overflow: hidden;
}

.outbound-kanban__column-head {
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

.outbound-kanban__column-count {
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

.outbound-kanban__column-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  max-height: 52vh;
  overflow: auto;
}

.outbound-kanban-card {
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

.outbound-kanban-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.outbound-kanban-card__order-no {
  font-size: 13px;
  font-weight: 700;
  color: #111827;
  word-break: break-all;
}

.outbound-kanban-card__factory {
  margin: 0 0 8px;
  font-size: 13px;
  color: #374151;
}

.outbound-kanban-card__meta,
.outbound-kanban-card__stats {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #6b7280;
}

.outbound-kanban-card__stats {
  margin-top: 8px;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 10px;
  font-weight: 600;
  color: #4b5563;
}

.outbound-kanban-card__remark {
  margin: 8px 0 0;
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.4;
}

.outbound-kanban-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}

.outbound-timeline {
  margin-top: 20px;
  padding: 16px 18px;
  border: 1px solid #e8ecf2;
  border-radius: 14px;
  background: #fff;
}

.outbound-timeline__title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
}

.outbound-timeline__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.outbound-timeline__item {
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

.outbound-timeline__dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 50%;
  background: #22c55e;
  flex-shrink: 0;
}

.outbound-timeline__line1 {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
  color: #374151;

  strong {
    color: #111827;
  }
}

.outbound-timeline__line2 {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.lines-toolbar {
  margin-bottom: 8px;
}

.outbound-form-wizard__center {
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
}

.outbound-form-wizard__steps {
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

.outbound-form-wizard__body {
  min-height: 320px;
}

.outbound-form-step__basic-form {
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

.outbound-form-step__field {
  margin-bottom: 0;
}

.outbound-form-step__lines-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.outbound-form-step__lines-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.outbound-form-step__add-line-btn {
  --el-button-bg-color: #15803d;
  --el-button-border-color: #15803d;
  --el-button-hover-bg-color: #166534;
  --el-button-hover-border-color: #166534;
  --el-button-active-bg-color: #14532d;
  --el-button-active-border-color: #14532d;
}

.outbound-line-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  overflow: auto;
  padding-right: 2px;
}

.outbound-line-card {
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

.outbound-line-card__thumb {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
}

.outbound-line-card__image {
  width: 64px;
  height: 64px;
}

.outbound-line-card__image-fallback {
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

.outbound-line-card__field {
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

.outbound-line-card__label {
  flex-shrink: 0;
  width: 72px;
  font-size: 14px;
  line-height: 1.4;
  color: var(--el-text-color-regular);
  text-align: right;
}

.outbound-line-card__control {
  flex: 1;
  min-width: 0;
}

.outbound-line-card__sku {
  width: 100%;
}

.outbound-line-card__sku-text {
  font-size: 14px;
  color: #111827;
}

.outbound-line-card__subtitle {
  margin: 6px 0 0;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.4;
}

.outbound-line-card__remove {
  flex-shrink: 0;
  align-self: center;
  margin-top: 0;
}

.outbound-line-card__qty-value {
  font-size: 16px;
  color: #111827;
}

.outbound-form-wizard__floating-summary {
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

.outbound-form-wizard__footer {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.outbound-form-confirm__section + .outbound-form-confirm__section {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid #f1f5f9;
}

.outbound-form-confirm__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.outbound-form-confirm__heading-leading {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.outbound-form-confirm__heading-bar {
  width: 4px;
  height: 16px;
  border-radius: 2px;
  background: #1d4ed8;
  flex-shrink: 0;
}

.outbound-form-confirm__badge {
  padding: 2px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 500;
}

.outbound-form-confirm__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 16px;
  margin: 0;
}

.outbound-form-confirm__item {
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

.outbound-form-confirm__lines {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 320px;
  overflow: auto;
  padding-right: 2px;
}

.outbound-confirm-line-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.outbound-confirm-line-card__thumb {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.outbound-confirm-line-card__image {
  width: 64px;
  height: 64px;
}

.outbound-confirm-line-card__image-fallback {
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

.outbound-confirm-line-card__info {
  flex: 1;
  min-width: 0;
}

.outbound-confirm-line-card__sku {
  display: block;
  font-size: 20px;
  font-weight: 800;
  color: #000;
  line-height: 1.25;
  letter-spacing: -0.01em;
}

.outbound-confirm-line-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.outbound-confirm-line-card__tag {
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

.outbound-confirm-line-card__qty {
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

.outbound-confirm-line-card__qty-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.outbound-confirm-line-card__qty-value {
  font-size: 28px;
  font-weight: 700;
  color: #ea580c;
  line-height: 1;
}

.outbound-confirm-line-card__qty-unit {
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
  .outbound-kanban {
    grid-template-columns: 1fr;
  }

  .outbound-form-confirm__grid {
    grid-template-columns: 1fr;
  }

  .outbound-line-card {
    flex-wrap: wrap;
  }

  .outbound-line-card__field.is-qty {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
