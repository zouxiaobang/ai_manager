<template>
  <el-drawer
    v-model="visible"
    :title="t('ecommerce.inventory.detailTitle')"
    size="720px"
    destroy-on-close
    @open="onOpen"
  >
    <div v-loading="loading" class="inventory-detail">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small" class="detail-block">
          <el-descriptions-item :label="t('ecommerce.inventory.skuCode')">{{ detail.skuCode }}</el-descriptions-item>
          <el-descriptions-item :label="t('ecommerce.inventory.specName')">{{ detail.specName || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="t('ecommerce.inventory.productName')">
            <el-button v-if="detail.productId" link type="primary" @click="emitViewProduct">
              {{ detail.productName || '—' }}
            </el-button>
            <span v-else>{{ detail.productName || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('ecommerce.inventory.factory')">{{ detail.factoryName || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="t('ecommerce.product.skuStatus')">{{ skuStatusLabel(detail.skuStatus) }}</el-descriptions-item>
          <el-descriptions-item :label="t('ecommerce.inventory.quantity')">{{ detail.quantity ?? 0 }}</el-descriptions-item>
          <el-descriptions-item :label="t('ecommerce.inventory.inTransit')">{{ detail.inTransitQty ?? 0 }}</el-descriptions-item>
          <el-descriptions-item :label="t('ecommerce.inventory.stockValue')">{{ formatPrice(stockValue) }}</el-descriptions-item>
          <el-descriptions-item :label="t('ecommerce.inventory.alertStatus')">
            <el-tag v-if="detail.alertActive" type="danger" size="small">{{ t('ecommerce.inventory.alerting') }}</el-tag>
            <el-tag v-else type="success" size="small">{{ t('ecommerce.inventory.normal') }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <section class="detail-section">
          <h4>{{ t('ecommerce.inventory.packingEstimate') }}</h4>
          <div v-if="packing" class="packing-row">
            <span>{{ t('ecommerce.inventory.cartonName') }}：{{ packing.cartonName || '—' }}</span>
            <span>{{ t('ecommerce.product.unitsPerCarton') }}：{{ packing.unitsPerCarton ?? '—' }}</span>
            <span>{{ t('ecommerce.inventory.cartonsNeeded') }}：{{ packing.cartonsNeeded ?? 0 }}</span>
            <span>{{ t('ecommerce.inventory.totalVolume') }}：{{ formatVolume(packing.totalVolumeCm3) }}</span>
          </div>
          <div class="packing-calc">
            <span>{{ t('ecommerce.inventory.outboundQty') }}</span>
            <el-input-number v-model="outboundQty" :min="1" :step="1" controls-position="right" @change="refreshPacking" />
            <el-button size="small" @click="refreshPacking">{{ t('ecommerce.inventory.recalcPacking') }}</el-button>
          </div>
        </section>

        <section class="detail-section">
          <h4>{{ t('ecommerce.inventory.recentLogs') }}</h4>
          <el-table :data="detail.recentLogs ?? []" stripe border size="small" max-height="220">
            <el-table-column :label="t('ecommerce.inventory.changeType')" width="100">
              <template #default="{ row }">{{ changeTypeLabel(row.changeType) }}</template>
            </el-table-column>
            <el-table-column prop="changeQty" :label="t('ecommerce.inventory.changeQty')" width="90" align="right" />
            <el-table-column prop="remark" :label="t('ecommerce.inbound.remark')" min-width="120" show-overflow-tooltip />
            <el-table-column :label="t('ecommerce.inventory.logTime')" width="170">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!(detail.recentLogs?.length)" :description="t('ecommerce.inventory.noLogs')" />
        </section>

        <section class="detail-section">
          <h4>{{ t('ecommerce.inventory.relatedInbound') }}</h4>
          <el-table :data="detail.relatedInboundOrders ?? []" stripe border size="small" max-height="220">
            <el-table-column prop="orderNo" :label="t('ecommerce.inbound.orderNo')" width="160" />
            <el-table-column :label="t('ecommerce.inbound.status')" width="96">
              <template #default="{ row }">{{ inboundStatusLabel(row.status) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" :label="t('ecommerce.inbound.orderedQty')" width="90" align="right" />
            <el-table-column :label="t('ecommerce.inbound.orderTime')" width="120">
              <template #default="{ row }">{{ formatDate(row.orderTime) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!(detail.relatedInboundOrders?.length)" :description="t('ecommerce.inventory.noRelatedInbound')" />
        </section>
      </template>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  fetchInventory,
  fetchPackingEstimate,
  type EcInventory,
  type EcInventoryPackingEstimate,
} from '@/api/ecommerce/inventory'
import { formatDate, formatDateTime } from '@/utils/date'

const props = defineProps<{ modelValue: boolean; inventoryId?: number | null }>()
const emit = defineEmits<{
  'update:modelValue': [boolean]
  viewProduct: [productId: number]
}>()

const { t } = useI18n()
const loading = ref(false)
const detail = ref<EcInventory | null>(null)
const packing = ref<EcInventoryPackingEstimate | null>(null)
const outboundQty = ref(1)

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const stockValue = computed(() => (detail.value?.quantity ?? 0) * (detail.value?.salePrice ?? 0))

watch(
  () => props.inventoryId,
  () => {
    if (visible.value && props.inventoryId) {
      loadDetail()
    }
  },
)

async function onOpen() {
  if (props.inventoryId) {
    await loadDetail()
  }
}

async function loadDetail() {
  if (!props.inventoryId) return
  loading.value = true
  try {
    detail.value = await fetchInventory(props.inventoryId)
    packing.value = detail.value.packingEstimate ?? null
    outboundQty.value = detail.value.quantity && detail.value.quantity > 0 ? detail.value.quantity : 1
  } finally {
    loading.value = false
  }
}

async function refreshPacking() {
  if (!detail.value?.skuCode) return
  packing.value = await fetchPackingEstimate(detail.value.skuCode, outboundQty.value)
}

function emitViewProduct() {
  if (detail.value?.productId) {
    emit('viewProduct', detail.value.productId)
  }
}

function changeTypeLabel(type: string) {
  if (type === 'DEDUCT') return t('ecommerce.inventory.deduct')
  if (type === 'RECLAIM') return t('ecommerce.inventory.reclaim')
  if (type === 'INBOUND') return t('ecommerce.inbound.inbound')
  if (type === 'STOCKTAKE') return t('ecommerce.stocktake.stocktake')
  return type
}

function skuStatusLabel(status?: string) {
  if (status === 'ON_SALE') return t('ecommerce.product.onSale')
  if (status === 'OFF_SALE') return t('ecommerce.product.offSale')
  if (status === 'DRAFT') return t('ecommerce.product.draft')
  return status || '—'
}

function inboundStatusLabel(status: string) {
  if (status === 'DRAFT') return t('ecommerce.inbound.statusDraft')
  if (status === 'CONFIRMED') return t('ecommerce.inbound.statusConfirmed')
  if (status === 'CANCELLED') return t('ecommerce.inbound.statusCancelled')
  return status
}

function formatPrice(value: number) {
  if (Number.isNaN(value)) return '—'
  return `¥${Number(value).toFixed(2)}`
}

function formatVolume(value?: number | null) {
  if (value == null) return '—'
  return `${Number(value).toLocaleString()} cm³`
}
</script>

<style scoped lang="scss">
.inventory-detail {
  min-height: 200px;
}

.detail-block {
  margin-bottom: 20px;
}

.detail-section {
  margin-bottom: 20px;

  h4 {
    margin: 0 0 10px;
    font-size: 14px;
    font-weight: 600;
  }
}

.packing-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 10px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.packing-calc {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
