<template>
  <div class="inventory-panel">
    <div class="panel-toolbar">
      <el-input
        v-model="keyword"
        :placeholder="t('ecommerce.inventory.searchPlaceholder')"
        clearable
        style="width: 320px"
      />
      <el-select
        v-model="factoryId"
        clearable
        filterable
        :placeholder="t('ecommerce.inventory.factoryPlaceholder')"
        style="width: 200px"
        @change="() => load(true)"
      >
        <el-option v-for="f in factoryOptions" :key="f.id" :label="f.name" :value="f.id" />
      </el-select>
      <el-checkbox v-model="alertOnly" @change="() => load(true)">
        {{ t('ecommerce.inventory.alertOnly') }}
      </el-checkbox>
      <el-button type="success" @click="openQuickInbound">{{ t('ecommerce.inbound.quickInbound') }}</el-button>
      <el-button @click="inboundDrawerVisible = true">{{ t('ecommerce.inbound.orderEntry') }}</el-button>
      <el-button @click="stocktakeDrawerVisible = true">{{ t('ecommerce.stocktake.orderEntry') }}</el-button>
      <el-button @click="globalLogDrawerVisible = true">{{ t('ecommerce.inventory.globalLogs') }}</el-button>
      <el-button @click="toggleFactorySummary">{{ factorySummaryVisible ? t('ecommerce.inventory.hideFactorySummary') : t('ecommerce.inventory.showFactorySummary') }}</el-button>
      <el-button type="primary" @click="openCreate">{{ t('ecommerce.inventory.add') }}</el-button>
    </div>

    <el-table
      v-if="factorySummaryVisible"
      v-loading="factorySummaryLoading"
      :data="factorySummary"
      stripe
      border
      size="small"
      class="factory-summary-table"
    >
      <el-table-column prop="factoryName" :label="t('ecommerce.inventory.factory')" min-width="120" />
      <el-table-column prop="skuCount" :label="t('ecommerce.inventory.skuCount')" width="100" align="right" />
      <el-table-column prop="totalQuantity" :label="t('ecommerce.inventory.quantity')" width="110" align="right" />
      <el-table-column :label="t('ecommerce.inventory.stockValue')" width="120" align="right">
        <template #default="{ row }">{{ formatPrice(Number(row.totalStockValue ?? 0)) }}</template>
      </el-table-column>
      <el-table-column prop="alertSkuCount" :label="t('ecommerce.inventory.alertSkuCount')" width="110" align="right" />
    </el-table>

    <el-table v-loading="loading" :data="records" stripe border show-summary :summary-method="inventorySummary">
      <el-table-column prop="skuCode" :label="t('ecommerce.inventory.skuCode')" width="120" fixed>
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="openDetail(row)">{{ row.skuCode }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="specName" :label="t('ecommerce.inventory.specName')" width="120" show-overflow-tooltip />
      <el-table-column prop="productName" :label="t('ecommerce.inventory.productName')" min-width="140" show-overflow-tooltip />
      <el-table-column prop="quantity" :label="t('ecommerce.inventory.quantity')" width="100" align="right" />
      <el-table-column :label="t('ecommerce.inventory.inTransit')" width="90" align="right">
        <template #default="{ row }">{{ row.inTransitQty ?? '—' }}</template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.inventory.stockValue')" width="120" align="right">
        <template #default="{ row }">{{ formatPrice(stockValue(row)) }}</template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.inventory.alertThreshold')" width="100" align="right">
        <template #default="{ row }">{{ row.alertThreshold ?? 0 }}</template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.inventory.ignoreAlert')" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.ignoreAlert" type="info" size="small">{{ t('ecommerce.inventory.yes') }}</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.inventory.alertStatus')" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.alertActive" type="danger" size="small">{{ t('ecommerce.inventory.alerting') }}</el-tag>
          <el-tag v-else type="success" size="small">{{ t('ecommerce.inventory.normal') }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.inventory.updatedAt')" width="170">
        <template #default="{ row }">{{ formatDate(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column
        :label="t('ecommerce.inventory.actions')"
        width="180"
        fixed="right"
        align="center"
        :class-name="TABLE_ACTIONS_CELL_CLASS"
      >
        <template #default="{ row }">
          <div class="table-actions-cell-inner" @click.stop>
            <el-button link type="primary" :title="t('ecommerce.inventory.detail')" @click.stop="openDetail(row)">
              <el-icon><View /></el-icon>
            </el-button>
            <el-button link type="primary" :title="t('ecommerce.inventory.edit')" @click.stop="openEdit(row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button link type="primary" :title="t('ecommerce.inventory.adjust')" @click.stop="openAdjust(row)">
              <el-icon><Sort /></el-icon>
            </el-button>
            <el-button link type="primary" :title="t('ecommerce.inventory.logs')" @click.stop="openLogs(row)">
              <el-icon><Document /></el-icon>
            </el-button>
            <el-button link type="danger" :title="t('ecommerce.inventory.delete')" @click.stop="onDelete(row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <TablePagination
      :page="page"
      :page-size="pageSize"
      :total="total"
      @update:page="onPageChange"
      @update:page-size="onSizeChange"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('ecommerce.inventory.editTitle') : t('ecommerce.inventory.createTitle')"
      width="520px"
      destroy-on-close
    >
      <el-form :model="form" label-width="108px">
        <el-form-item :label="t('ecommerce.inventory.skuCode')" required>
          <el-select
            v-if="!editingId"
            v-model="form.skuCode"
            filterable
            :placeholder="t('ecommerce.inventory.skuCodePlaceholder')"
            style="width: 100%"
          >
            <el-option v-for="code in availableSkuCodes" :key="code" :label="code" :value="code" />
          </el-select>
          <el-input v-else v-model="form.skuCode" disabled />
        </el-form-item>
        <el-form-item v-if="!editingId" :label="t('ecommerce.inventory.quantity')">
          <el-input-number v-model="form.quantity" :min="0" :step="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('ecommerce.inventory.alertThreshold')">
          <el-input-number v-model="form.alertThreshold" :min="0" :step="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('ecommerce.inventory.ignoreAlert')">
          <el-switch v-model="form.ignoreAlert" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="adjustVisible"
      :title="t('ecommerce.inventory.adjustTitle')"
      width="440px"
      destroy-on-close
    >
      <p v-if="adjustTarget" class="adjust-hint">
        {{ t('ecommerce.inventory.adjustHint', { sku: adjustTarget.skuCode, qty: adjustTarget.quantity }) }}
      </p>
      <el-form :model="adjustForm" label-width="96px">
        <el-form-item :label="t('ecommerce.inventory.changeType')" required>
          <el-radio-group v-model="adjustForm.changeType">
            <el-radio value="DEDUCT">{{ t('ecommerce.inventory.deduct') }}</el-radio>
            <el-radio value="RECLAIM">{{ t('ecommerce.inventory.reclaim') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('ecommerce.inventory.changeQty')" required>
          <el-input-number
            v-model="adjustForm.changeQty"
            :min="1"
            :step="1"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="adjusting" @click="onAdjust">{{ t('ecommerce.common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="logsVisible"
      :title="t('ecommerce.inventory.logsTitle')"
      width="640px"
      destroy-on-close
    >
      <el-table v-loading="logsLoading" :data="logRecords" stripe border size="small" max-height="420">
        <el-table-column :label="t('ecommerce.inventory.changeType')" width="100">
          <template #default="{ row }">{{ changeTypeLabel(row.changeType) }}</template>
        </el-table-column>
        <el-table-column prop="changeQty" :label="t('ecommerce.inventory.changeQty')" width="100" align="right" />
        <el-table-column prop="remark" :label="t('ecommerce.inbound.remark')" min-width="140" show-overflow-tooltip />
        <el-table-column :label="t('ecommerce.inventory.logTime')" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <TablePagination
        v-if="logsInventoryId"
        :page="logPage"
        :page-size="logPageSize"
        :total="logTotal"
        @update:page="onLogPageChange"
        @update:page-size="onLogSizeChange"
      />
      <el-empty v-if="!logsLoading && !logRecords.length" :description="t('ecommerce.inventory.noLogs')" />
    </el-dialog>

    <el-dialog
      v-model="quickInboundVisible"
      :title="t('ecommerce.inbound.quickInboundTitle')"
      width="520px"
      destroy-on-close
    >
      <el-form :model="quickInboundForm" label-width="108px">
        <el-form-item :label="t('ecommerce.inventory.skuCode')" required>
          <el-select
            v-model="quickInboundForm.skuCode"
            filterable
            remote
            :remote-method="searchSkuOptions"
            :loading="skuOptionsLoading"
            :placeholder="t('ecommerce.inventory.skuCodePlaceholder')"
            style="width: 100%"
          >
            <el-option
              v-for="opt in skuOptions"
              :key="opt.skuCode"
              :label="skuOptionLabel(opt)"
              :value="opt.skuCode"
              :disabled="opt.inboundAllowed === false"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedSkuOption && !selectedSkuOption.hasInventory" :label="t('ecommerce.inbound.newInventoryHint')">
          <el-tag type="warning" size="small">{{ t('ecommerce.inbound.autoCreateHint') }}</el-tag>
        </el-form-item>
        <el-form-item :label="t('ecommerce.inbound.inboundQty')" required>
          <el-input-number
            v-model="quickInboundForm.quantity"
            :min="1"
            :step="1"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('ecommerce.inbound.remark')">
          <el-input v-model="quickInboundForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickInboundVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="quickInboundSaving" @click="onQuickInbound">
          {{ t('ecommerce.inbound.confirmInbound') }}
        </el-button>
      </template>
    </el-dialog>

    <InboundOrderDrawer v-model="inboundDrawerVisible" @refreshed="loadInventories" />
    <StocktakeOrderDrawer v-model="stocktakeDrawerVisible" :factory-id="factoryId" @refreshed="loadInventories" />
    <GlobalLogDrawer v-model="globalLogDrawerVisible" />
    <InventoryDetailDrawer
      v-model="detailDrawerVisible"
      :inventory-id="detailInventoryId"
      @view-product="(id) => emit('viewProduct', id)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableColumnCtx } from 'element-plus'
import { Delete, Document, Edit, Sort, View } from '@element-plus/icons-vue'
import {
  adjustInventory,
  createInventory,
  deleteInventory,
  fetchAvailableSkuCodes,
  fetchInventoryFactorySummary,
  fetchInventoryLogs,
  fetchInventorySkuOptions,
  fetchInventories,
  quickInbound,
  updateInventory,
  type EcInventory,
  type EcInventoryFactorySummary,
  type EcInventoryLog,
  type EcInventorySaveRequest,
  type EcInventorySkuOption,
} from '@/api/ecommerce/inventory'
import { fetchFactoryOptions, type EcFactory } from '@/api/ecommerce/factory'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { TABLE_ACTIONS_CELL_CLASS } from '@/constants/table'
import { formatDate, formatDateTime } from '@/utils/date'
import InboundOrderDrawer from './InboundOrderDrawer.vue'
import StocktakeOrderDrawer from './StocktakeOrderDrawer.vue'
import GlobalLogDrawer from './GlobalLogDrawer.vue'
import InventoryDetailDrawer from './InventoryDetailDrawer.vue'

const emit = defineEmits<{ viewProduct: [productId: number] }>()

const { t } = useI18n()

const saving = ref(false)
const adjusting = ref(false)
const logsLoading = ref(false)
const keyword = ref('')
const factoryId = ref<number | undefined>()
const alertOnly = ref(false)
const factoryOptions = ref<EcFactory[]>([])
const availableSkuCodes = ref<string[]>([])

const { page, pageSize, total, records, extra, loading, load, onPageChange, onSizeChange } = usePagination(
  (p, ps) =>
    fetchInventories(keyword.value.trim() || undefined, alertOnly.value, factoryId.value, {
      page: p,
      pageSize: ps,
    }),
)

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const adjustVisible = ref(false)
const adjustTarget = ref<EcInventory | null>(null)
const logsVisible = ref(false)
const logsInventoryId = ref<number | null>(null)
const logPage = ref(1)
const logPageSize = ref(20)
const logTotal = ref(0)
const logRecords = ref<EcInventoryLog[]>([])
const quickInboundVisible = ref(false)
const quickInboundSaving = ref(false)
const skuOptionsLoading = ref(false)
const inboundDrawerVisible = ref(false)
const stocktakeDrawerVisible = ref(false)
const globalLogDrawerVisible = ref(false)
const detailDrawerVisible = ref(false)
const detailInventoryId = ref<number | null>(null)
const factorySummaryVisible = ref(false)
const factorySummaryLoading = ref(false)
const factorySummary = ref<EcInventoryFactorySummary[]>([])
const skuOptions = ref<EcInventorySkuOption[]>([])

const quickInboundForm = reactive({
  skuCode: '',
  quantity: 1,
  remark: '',
})

const selectedSkuOption = computed(() =>
  skuOptions.value.find((opt) => opt.skuCode === quickInboundForm.skuCode),
)

const form = reactive({
  skuCode: '',
  quantity: 0,
  ignoreAlert: false,
  alertThreshold: 0,
})

const adjustForm = reactive({
  changeType: 'DEDUCT' as 'DEDUCT' | 'RECLAIM',
  changeQty: 1,
})

function changeTypeLabel(type: string) {
  if (type === 'DEDUCT') return t('ecommerce.inventory.deduct')
  if (type === 'RECLAIM') return t('ecommerce.inventory.reclaim')
  if (type === 'INBOUND') return t('ecommerce.inbound.inbound')
  if (type === 'STOCKTAKE') return t('ecommerce.stocktake.stocktake')
  return type
}

function stockValue(row: EcInventory): number {
  return (row.quantity ?? 0) * (row.salePrice ?? 0)
}

function formatPrice(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return '—'
  return `¥${Number(value).toFixed(2)}`
}

function inventorySummary({ columns }: { columns: TableColumnCtx<EcInventory>[] }) {
  const totalQuantity = Number(extra.value?.totalQuantity ?? 0)
  const totalStockValue = Number(extra.value?.totalStockValue ?? 0)
  const sums: string[] = []
  columns.forEach((column: TableColumnCtx<EcInventory>, index: number) => {
    if (index === 0) {
      sums[index] = t('ecommerce.inventory.total')
      return
    }
    if (column.property === 'quantity') {
      sums[index] = String(totalQuantity)
      return
    }
    if (column.label === t('ecommerce.inventory.stockValue')) {
      sums[index] = formatPrice(totalStockValue)
      return
    }
    sums[index] = ''
  })
  return sums
}

function skuOptionLabel(opt: EcInventorySkuOption) {
  const parts = [opt.skuCode]
  if (opt.specName) parts.push(opt.specName)
  if (opt.inboundAllowed === false) parts.push(t('ecommerce.inventory.inboundBlocked'))
  else if (opt.hasInventory) parts.push(`${t('ecommerce.inventory.quantity')}:${opt.quantity}`)
  else parts.push(t('ecommerce.inbound.noInventoryYet'))
  return parts.join(' · ')
}

function resetQuickInboundForm() {
  quickInboundForm.skuCode = ''
  quickInboundForm.quantity = 1
  quickInboundForm.remark = ''
}

async function searchSkuOptions(query: string) {
  skuOptionsLoading.value = true
  try {
    skuOptions.value = await fetchInventorySkuOptions(factoryId.value, query || undefined)
  } finally {
    skuOptionsLoading.value = false
  }
}

async function openQuickInbound() {
  resetQuickInboundForm()
  quickInboundVisible.value = true
  await searchSkuOptions('')
}

async function onQuickInbound() {
  if (!quickInboundForm.skuCode) {
    ElMessage.warning(t('ecommerce.inventory.skuCodeRequired'))
    return
  }
  if (selectedSkuOption.value?.inboundAllowed === false) {
    ElMessage.warning(t('ecommerce.inventory.inboundBlockedMsg'))
    return
  }
  if (!quickInboundForm.quantity || quickInboundForm.quantity <= 0) {
    ElMessage.warning(t('ecommerce.inbound.inboundQtyRequired'))
    return
  }

  quickInboundSaving.value = true
  try {
    await quickInbound({
      skuCode: quickInboundForm.skuCode,
      quantity: quickInboundForm.quantity,
      remark: quickInboundForm.remark?.trim() || undefined,
    })
    ElMessage.success(t('ecommerce.inbound.inboundSuccess'))
    quickInboundVisible.value = false
    await loadInventories()
  } finally {
    quickInboundSaving.value = false
  }
}

function resetForm() {
  form.skuCode = ''
  form.quantity = 0
  form.ignoreAlert = false
  form.alertThreshold = 0
}

async function loadInventories() {
  await load()
}

async function loadAvailableSkuCodes() {
  availableSkuCodes.value = await fetchAvailableSkuCodes()
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => load(true), 300)
})

async function openCreate() {
  editingId.value = null
  resetForm()
  await loadAvailableSkuCodes()
  dialogVisible.value = true
}

function openEdit(row: EcInventory) {
  editingId.value = row.id
  form.skuCode = row.skuCode
  form.quantity = row.quantity
  form.ignoreAlert = !!row.ignoreAlert
  form.alertThreshold = row.alertThreshold ?? 0
  dialogVisible.value = true
}

async function onSave() {
  if (!form.skuCode.trim()) {
    ElMessage.warning(t('ecommerce.inventory.skuCodeRequired'))
    return
  }

  saving.value = true
  try {
    const payload: EcInventorySaveRequest = {
      skuCode: form.skuCode.trim(),
      ignoreAlert: form.ignoreAlert,
      alertThreshold: form.alertThreshold,
    }
    if (!editingId.value) {
      payload.quantity = form.quantity
      await createInventory(payload)
    } else {
      await updateInventory(editingId.value, payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    dialogVisible.value = false
    await loadInventories()
  } finally {
    saving.value = false
  }
}

function openAdjust(row: EcInventory) {
  adjustTarget.value = row
  adjustForm.changeType = 'DEDUCT'
  adjustForm.changeQty = 1
  adjustVisible.value = true
}

async function onAdjust() {
  if (!adjustTarget.value) return
  if (!adjustForm.changeQty || adjustForm.changeQty <= 0) {
    ElMessage.warning(t('ecommerce.inventory.changeQtyRequired'))
    return
  }

  adjusting.value = true
  try {
    await adjustInventory(adjustTarget.value.id, {
      changeType: adjustForm.changeType,
      changeQty: adjustForm.changeQty,
    })
    ElMessage.success(t('ecommerce.common.saved'))
    adjustVisible.value = false
    await loadInventories()
  } finally {
    adjusting.value = false
  }
}

function openDetail(row: EcInventory) {
  detailInventoryId.value = row.id
  detailDrawerVisible.value = true
}

async function toggleFactorySummary() {
  factorySummaryVisible.value = !factorySummaryVisible.value
  if (factorySummaryVisible.value) {
    factorySummaryLoading.value = true
    try {
      factorySummary.value = await fetchInventoryFactorySummary(factoryId.value)
    } finally {
      factorySummaryLoading.value = false
    }
  }
}

async function openLogs(row: EcInventory) {
  logsInventoryId.value = row.id
  logsVisible.value = true
  logPage.value = 1
  await loadLogPage(true)
}

async function loadLogPage(reset = false) {
  if (!logsInventoryId.value) return
  if (reset) logPage.value = 1
  logsLoading.value = true
  try {
    const result = await fetchInventoryLogs(logsInventoryId.value, {
      page: logPage.value,
      pageSize: logPageSize.value,
    })
    logRecords.value = result.records
    logTotal.value = result.total
  } finally {
    logsLoading.value = false
  }
}

function onLogPageChange(p: number) {
  logPage.value = p
  loadLogPage()
}

function onLogSizeChange(ps: number) {
  logPageSize.value = ps
  loadLogPage(true)
}

async function onDelete(row: EcInventory) {
  await ElMessageBox.confirm(
    t('ecommerce.inventory.deleteConfirm', { sku: row.skuCode }),
    { type: 'warning' },
  )
  await deleteInventory(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  await loadInventories()
}

onMounted(async () => {
  factoryOptions.value = await fetchFactoryOptions()
  await loadInventories()
})

defineExpose({ loadInventories })
</script>

<style scoped lang="scss">
.panel-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.adjust-hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.factory-summary-table {
  margin-bottom: 16px;
}
</style>
