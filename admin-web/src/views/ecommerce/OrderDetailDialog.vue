<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="820px"
    append-to-body
    destroy-on-close
    class="order-detail-dialog"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="order-detail">
      <template v-if="order">
        <header class="order-detail__hero">
          <div class="order-detail__hero-main">
            <div class="order-detail__hero-info">
              <p class="order-detail__line order-detail__line--order-no">{{ order.orderNo }}</p>
              <p class="order-detail__line order-detail__line--factory">{{ order.factoryName || '—' }}</p>
            </div>
            <div class="order-detail__hero-status">
              <span class="order-detail__status-badge" :class="statusBadgeClass">
                <span class="order-detail__status-dot" aria-hidden="true" />
                {{ statusLabel }}
              </span>
            </div>
          </div>
        </header>

        <div v-if="isConfirmed && actualTimeText" class="order-detail__actual-strip">
          <span class="order-detail__actual-strip-label">{{ actualTimeLabel }}</span>
          <span>{{ actualTimeText }}</span>
        </div>

        <div class="order-detail__metrics">
          <article
            v-for="metric in metrics"
            :key="metric.key"
            class="order-detail__metric"
            :class="`is-${metric.tone}`"
          >
            <div class="order-detail__metric-icon" aria-hidden="true">
              <el-icon><component :is="metric.icon" /></el-icon>
            </div>
            <div class="order-detail__metric-body">
              <span class="order-detail__metric-label">{{ metric.label }}</span>
              <AutoFitOneLineText
                v-if="metric.isDate"
                :text="metric.value"
                :max="16"
                :min="11"
                class="order-detail__metric-value"
              />
              <strong v-else class="order-detail__metric-value" :class="{ 'is-compact': metric.compact }">
                {{ metric.value }}
              </strong>
            </div>
          </article>
        </div>

        <el-tabs v-model="activeTab" class="order-detail__tabs">
          <el-tab-pane
            v-if="variant === 'outbound'"
            :label="customerTabLabel"
            name="customer"
          >
            <div class="order-detail__panel">
              <dl v-if="hasCustomerInfo" class="order-detail__customer-grid">
                <div class="order-detail__customer-item">
                  <dt>{{ customerUnitLabel }}</dt>
                  <dd>{{ order.customer?.name || '—' }}</dd>
                </div>
                <div class="order-detail__customer-item">
                  <dt>{{ customerContactLabel }}</dt>
                  <dd>{{ order.customer?.contactName || '—' }}</dd>
                </div>
                <div class="order-detail__customer-item">
                  <dt>{{ customerPhoneLabel }}</dt>
                  <dd>{{ order.customer?.contactPhone || '—' }}</dd>
                </div>
                <div class="order-detail__customer-item is-full">
                  <dt>{{ customerAddressLabel }}</dt>
                  <dd>{{ order.customer?.address || '—' }}</dd>
                </div>
              </dl>
              <el-empty v-else :description="emptyCustomerText" :image-size="64" />
            </div>
          </el-tab-pane>
          <el-tab-pane :label="linesTabLabel" name="lines">
            <div class="order-detail__panel">
              <div v-if="order.lines?.length" class="order-detail__line-cards">
                <article v-for="(line, index) in order.lines" :key="index" class="order-detail__line-card">
                  <div class="order-detail__line-thumb">
                    <el-image
                      v-if="lineImageUrl(line.skuCode)"
                      :src="lineImageUrl(line.skuCode)"
                      fit="cover"
                      class="order-detail__line-image"
                    >
                      <template #error>
                        <div class="order-detail__line-image-fallback">{{ noImageText }}</div>
                      </template>
                    </el-image>
                    <div v-else class="order-detail__line-image-fallback">{{ noImageText }}</div>
                  </div>
                  <div class="order-detail__line-info">
                    <strong class="order-detail__line-sku">{{ line.skuCode }}</strong>
                    <div v-if="line.specName || line.productName" class="order-detail__line-tags">
                      <el-tag v-if="line.specName" size="small" effect="light" class="order-detail__line-tag is-spec">
                        {{ line.specName }}
                      </el-tag>
                      <el-tag v-if="line.productName" size="small" effect="light" type="success" class="order-detail__line-tag is-product">
                        {{ line.productName }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="order-detail__line-qty">
                    <div class="order-detail__line-qty-block">
                      <strong class="order-detail__line-qty-value">{{ lineDisplayQty(line) }}</strong>
                      <span class="order-detail__line-qty-unit">{{ pieceUnit }}</span>
                    </div>
                  </div>
                </article>
              </div>
              <el-empty v-else :description="emptyLinesText" :image-size="64" />
            </div>
          </el-tab-pane>
          <el-tab-pane :label="remarkTabLabel" name="remark">
            <div class="order-detail__panel">
              <div v-if="order.remark?.trim()" class="order-detail__remark-box">
                {{ order.remark }}
              </div>
              <el-empty v-else :description="emptyRemarkText" :image-size="64" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>
    <template #footer>
      <template v-if="order?.status === 'DRAFT'">
        <el-button type="primary" @click="emit('confirm')">{{ confirmText }}</el-button>
        <el-button
          v-if="variant === 'inbound'"
          type="success"
          plain
          @click="emit('print')"
        >
          {{ printPurchaseText }}
        </el-button>
        <el-button
          v-if="variant === 'outbound'"
          type="success"
          plain
          @click="emit('print')"
        >
          {{ printDeliveryText }}
        </el-button>
        <el-button @click="emit('edit')">{{ editText }}</el-button>
        <el-button @click="emit('cancel')">{{ cancelText }}</el-button>
        <el-button type="danger" plain @click="emit('delete')">{{ deleteText }}</el-button>
      </template>
      <el-button
        v-else-if="variant === 'inbound' && order"
        type="success"
        plain
        @click="emit('print')"
      >
        {{ printPurchaseText }}
      </el-button>
      <el-button
        v-else-if="variant === 'outbound' && order"
        type="success"
        plain
        @click="emit('print')"
      >
        {{ printDeliveryText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, markRaw, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Box, Calendar, Clock, Grid } from '@element-plus/icons-vue'
import { formatDate, formatDateTime } from '@/utils/date'
import AutoFitOneLineText from '@/components/AutoFitOneLineText.vue'

export interface OrderDetailLine {
  skuCode: string
  specName?: string
  productName?: string
  quantity: number
  receivedQuantity?: number | null
  shippedQuantity?: number | null
}

export interface OrderDetailCustomer {
  name?: string
  contactName?: string
  contactPhone?: string
  address?: string
}

export interface OrderDetailData {
  orderNo: string
  factoryName?: string
  status: string
  remark?: string
  orderTime?: string
  expectedTime?: string
  actualTime?: string | null
  customer?: OrderDetailCustomer | null
  lines?: OrderDetailLine[]
}

const props = defineProps<{
  modelValue: boolean
  loading?: boolean
  variant: 'inbound' | 'outbound'
  order: OrderDetailData | null
  resolveSkuImage?: (skuCode: string) => string | undefined
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  edit: []
  confirm: []
  cancel: []
  delete: []
  print: []
}>()

const { t } = useI18n()
const activeTab = ref('lines')

const prefix = computed(() => (props.variant === 'inbound' ? 'ecommerce.inbound' : 'ecommerce.outbound'))

const title = computed(() => t(`${prefix.value}.orderDetailTitle`))
const linesTabLabel = computed(() => t(`${prefix.value}.lines`))
const customerTabLabel = computed(() => t('ecommerce.outbound.customerInfo'))
const remarkTabLabel = computed(() => t(`${prefix.value}.remark`))
const customerUnitLabel = computed(() => t('ecommerce.outbound.dnShipTo'))
const customerContactLabel = computed(() => t('ecommerce.outbound.dnReceiverName'))
const customerPhoneLabel = computed(() => t('ecommerce.outbound.dnReceiverPhone'))
const customerAddressLabel = computed(() => t('ecommerce.outbound.dnReceiverAddress'))
const emptyCustomerText = computed(() => t('ecommerce.outbound.customerEmpty'))
const editText = computed(() => t('ecommerce.inventory.edit'))
const deleteText = computed(() => t('ecommerce.inventory.delete'))
const confirmText = computed(() => t(`${prefix.value}.confirm`))
const printPurchaseText = computed(() => t('ecommerce.inbound.printPurchaseOrder'))
const printDeliveryText = computed(() => t('ecommerce.outbound.printDeliveryOrder'))
const cancelText = computed(() => t(`${prefix.value}.cancel`))
const noImageText = computed(() => t(`${prefix.value}.noSkuImage`))
const emptyLinesText = computed(() => t(`${prefix.value}.linesRequired`))
const emptyRemarkText = computed(() => t(`${prefix.value}.remarkEmpty`))
const pieceUnit = computed(() => t('ecommerce.inventory.unitPiece'))
const actualTimeLabel = computed(() =>
  props.variant === 'inbound' ? t('ecommerce.inbound.actualReceiptTime') : t('ecommerce.outbound.actualShipTime'),
)

const isConfirmed = computed(() => props.order?.status === 'CONFIRMED')

const hasCustomerInfo = computed(() => {
  const customer = props.order?.customer
  if (!customer) return false
  return Boolean(
    customer.name?.trim()
      || customer.contactName?.trim()
      || customer.contactPhone?.trim()
      || customer.address?.trim(),
  )
})

const defaultTab = computed(() => (props.variant === 'outbound' ? 'customer' : 'lines'))

const statusLabel = computed(() => {
  const status = props.order?.status
  if (!status) return ''
  if (status === 'DRAFT') return t(`${prefix.value}.statusDraft`)
  if (status === 'CONFIRMED') return t(`${prefix.value}.statusConfirmed`)
  if (status === 'CANCELLED') return t(`${prefix.value}.statusCancelled`)
  return status
})

const statusBadgeClass = computed(() => {
  const status = props.order?.status
  if (status === 'CONFIRMED') return 'is-confirmed'
  if (status === 'CANCELLED') return 'is-cancelled'
  return 'is-draft'
})

const lineStats = computed(() => {
  const lines = props.order?.lines ?? []
  return {
    skuCount: lines.length,
    totalQty: lines.reduce((sum, line) => sum + (line.quantity ?? 0), 0),
  }
})

const metrics = computed(() => {
  if (!props.order) return []
  const expectedLabel =
    props.variant === 'inbound'
      ? t('ecommerce.inbound.expectedDeliveryTime')
      : t('ecommerce.outbound.expectedShipTime')
  const orderTimeLabel =
    props.variant === 'inbound' ? t('ecommerce.inbound.orderTime') : t('ecommerce.outbound.orderTime')

  return [
    {
      key: 'orderTime',
      tone: 'blue',
      label: orderTimeLabel,
      value: formatDate(props.order.orderTime),
      icon: markRaw(Calendar),
      isDate: true,
    },
    {
      key: 'expectedTime',
      tone: 'purple',
      label: expectedLabel,
      value: formatDate(props.order.expectedTime),
      icon: markRaw(Clock),
      isDate: true,
    },
    {
      key: 'skuCount',
      tone: 'green',
      label: t(`${prefix.value}.skuLineCount`, { count: lineStats.value.skuCount }),
      value: String(lineStats.value.skuCount),
      icon: markRaw(Grid),
      compact: false,
    },
    {
      key: 'totalQty',
      tone: 'orange',
      label: t(`${prefix.value}.totalOrderedQty`, { count: lineStats.value.totalQty }),
      value: String(lineStats.value.totalQty),
      icon: markRaw(Box),
      compact: false,
    },
  ]
})

const actualTimeText = computed(() => {
  if (!props.order?.actualTime) return ''
  return formatDateTime(props.order.actualTime)
})

function lineActualQty(line: OrderDetailLine) {
  if (props.variant === 'inbound') return line.receivedQuantity ?? null
  return line.shippedQuantity ?? null
}

function lineDisplayQty(line: OrderDetailLine) {
  if (isConfirmed.value) {
    const actual = lineActualQty(line)
    if (actual != null) return actual
  }
  return line.quantity
}

function lineImageUrl(skuCode: string) {
  return props.resolveSkuImage?.(skuCode)
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) activeTab.value = defaultTab.value
  },
)
</script>

<style scoped lang="scss">
.order-detail__hero {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.order-detail__hero-main {
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 0;
}

.order-detail__hero-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.order-detail__line {
  margin: 0;
  line-height: 1.45;
  word-break: break-word;
}

.order-detail__line--order-no {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.order-detail__line--factory {
  font-size: 14px;
  color: #6b7280;
}

.order-detail__hero-status {
  flex-shrink: 0;
}

.order-detail__status-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;

  &.is-draft {
    color: #c2410c;
    background: #fff7ed;
    border: 1px solid #fed7aa;

    .order-detail__status-dot {
      background: #f59e0b;
    }
  }

  &.is-confirmed {
    color: #16a34a;
    background: #ecfdf5;
    border: 1px solid #bbf7d0;

    .order-detail__status-dot {
      background: #22c55e;
    }
  }

  &.is-cancelled {
    color: #64748b;
    background: #f8fafc;
    border: 1px solid #e2e8f0;

    .order-detail__status-dot {
      background: #94a3b8;
    }
  }
}

.order-detail__status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.order-detail__actual-strip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  padding: 10px 14px;
  border-radius: 10px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  font-size: 13px;
  color: #166534;
}

.order-detail__actual-strip-label {
  font-weight: 600;
}

.order-detail__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}

.order-detail__metric {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 76px;
  padding: 14px 12px;
  border-radius: 12px;
  border: 1px solid transparent;

  &.is-blue {
    background: #eff6ff;
    border-color: #bfdbfe;

    .order-detail__metric-icon {
      background: #3b82f6;
    }

    .order-detail__metric-value {
      color: #2563eb;
    }
  }

  &.is-purple {
    background: #f5f3ff;
    border-color: #ddd6fe;

    .order-detail__metric-icon {
      background: #8b5cf6;
    }

    .order-detail__metric-value {
      color: #7c3aed;
    }
  }

  &.is-orange {
    background: #fff7ed;
    border-color: #fed7aa;

    .order-detail__metric-icon {
      background: #f59e0b;
    }

    .order-detail__metric-value {
      color: #ea580c;
    }
  }

  &.is-green {
    background: #f0fdf4;
    border-color: #bbf7d0;

    .order-detail__metric-icon {
      background: #22c55e;
    }

    .order-detail__metric-value {
      color: #16a34a;
    }
  }
}

.order-detail__metric-icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  color: #fff;
  font-size: 24px;
}

.order-detail__metric-body {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.order-detail__metric-label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
  line-height: 1.3;
}

.order-detail__metric-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.1;
  color: inherit;

  &.is-compact {
    font-size: 16px;
  }
}

.order-detail__tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 12px;
  }
}

.order-detail__panel {
  min-height: 160px;
}

.order-detail__line-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 320px;
  overflow: auto;
  padding-right: 2px;
}

.order-detail__line-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.order-detail__line-thumb {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
}

.order-detail__line-image {
  width: 64px;
  height: 64px;
}

.order-detail__line-image-fallback {
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

.order-detail__line-info {
  flex: 1;
  min-width: 0;
}

.order-detail__line-sku {
  display: block;
  font-size: 20px;
  font-weight: 800;
  color: #000;
  line-height: 1.25;
  letter-spacing: -0.01em;
}

.order-detail__line-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.order-detail__line-tag {
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

.order-detail__line-qty {
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

.order-detail__line-qty-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.order-detail__line-qty-value {
  font-size: 28px;
  font-weight: 700;
  color: #ea580c;
  line-height: 1;
}

.order-detail__line-qty-unit {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1;
}

.order-detail__remark-box {
  padding: 14px 16px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  font-size: 14px;
  line-height: 1.6;
  color: #374151;
  white-space: pre-wrap;
  word-break: break-word;
}

.order-detail__customer-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
  margin: 0;
  padding: 16px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.order-detail__customer-item {
  min-width: 0;

  &.is-full {
    grid-column: 1 / -1;
  }

  dt {
    margin: 0 0 4px;
    font-size: 12px;
    font-weight: 600;
    color: #6b7280;
  }

  dd {
    margin: 0;
    font-size: 14px;
    line-height: 1.5;
    color: #111827;
    word-break: break-word;
  }
}

@media (max-width: 720px) {
  .order-detail__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .order-detail__hero-main {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
