<template>
  <el-drawer
    v-model="visible"
    :title="t('ecommerce.stocktake.orderDrawerTitle')"
    size="960px"
    destroy-on-close
    @open="() => load(true)"
    @closed="emit('refreshed')"
  >
    <div class="stocktake-drawer">
      <div class="drawer-toolbar">
        <el-input
          v-model="keyword"
          :placeholder="t('ecommerce.stocktake.orderSearchPlaceholder')"
          clearable
          style="width: 240px"
        />
        <el-select
          v-model="statusFilter"
          clearable
          :placeholder="t('ecommerce.inbound.statusFilter')"
          style="width: 140px"
          @change="() => load(true)"
        >
          <el-option :label="t('ecommerce.inbound.statusDraft')" value="DRAFT" />
          <el-option :label="t('ecommerce.stocktake.statusConfirmed')" value="CONFIRMED" />
          <el-option :label="t('ecommerce.inbound.statusCancelled')" value="CANCELLED" />
        </el-select>
        <el-button type="primary" @click="openCreate">{{ t('ecommerce.stocktake.createOrder') }}</el-button>
      </div>

      <el-table v-loading="loading" :data="records" stripe border size="small">
        <el-table-column prop="orderNo" :label="t('ecommerce.stocktake.orderNo')" width="168" fixed />
        <el-table-column prop="factoryName" :label="t('ecommerce.inventory.factory')" width="110" show-overflow-tooltip />
        <el-table-column :label="t('ecommerce.stocktake.stocktakeTime')" width="160">
          <template #default="{ row }">{{ formatDate(row.stocktakeTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.inbound.status')" width="96" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="t('ecommerce.inbound.remark')" min-width="100" show-overflow-tooltip />
        <el-table-column
          :label="t('ecommerce.inventory.actions')"
          width="148"
          fixed="right"
          align="center"
          :class-name="TABLE_ACTIONS_CELL_CLASS"
        >
          <template #default="{ row }">
            <div class="table-actions-cell-inner" @click.stop>
              <el-button v-if="row.status === 'DRAFT'" link type="primary" @click.stop="openEdit(row.id)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button v-if="row.status === 'DRAFT'" link type="success" @click.stop="onConfirm(row)">
                <el-icon><Check /></el-icon>
              </el-button>
              <el-button v-if="row.status === 'DRAFT'" link type="warning" @click.stop="onCancel(row)">
                <el-icon><Close /></el-icon>
              </el-button>
              <el-button v-if="row.status === 'DRAFT'" link type="danger" @click.stop="onDelete(row)">
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
    </div>

    <el-dialog
      v-model="formVisible"
      :title="editingId ? t('ecommerce.stocktake.editOrderTitle') : t('ecommerce.stocktake.createOrderTitle')"
      width="720px"
      destroy-on-close
    >
      <el-form label-width="108px">
        <el-form-item :label="t('ecommerce.inventory.factory')">
          <el-select v-model="form.factoryId" clearable filterable style="width: 100%">
            <el-option v-for="f in factoryOptions" :key="f.id" :label="f.name" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('ecommerce.stocktake.stocktakeTime')" required>
          <el-date-picker
            v-model="form.stocktakeTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('ecommerce.inbound.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="t('ecommerce.inbound.lines')">
          <el-button size="small" @click="addLine">{{ t('ecommerce.inbound.addLine') }}</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="form.lines" stripe border size="small" max-height="280">
        <el-table-column :label="t('ecommerce.inventory.skuCode')" min-width="180">
          <template #default="{ row }">
            <el-select
              v-model="row.skuCode"
              filterable
              remote
              :remote-method="searchSkuOptions"
              style="width: 100%"
            >
              <el-option
                v-for="opt in inventorySkuOptions"
                :key="opt.skuCode"
                :label="skuOptionLabel(opt)"
                :value="opt.skuCode"
                :disabled="!opt.hasInventory"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.stocktake.bookQty')" width="100" align="right">
          <template #default="{ row }">{{ rowBookQty(row.skuCode) }}</template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.stocktake.actualQty')" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.actualQuantity" :min="0" :step="1" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column width="60" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" :disabled="form.lines.length <= 1" @click="form.lines.splice($index, 1)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="formVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.common.save') }}</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, Delete, Edit } from '@element-plus/icons-vue'
import {
  cancelStocktakeOrder,
  confirmStocktakeOrder,
  createStocktakeOrder,
  deleteStocktakeOrder,
  fetchStocktakeOrder,
  fetchStocktakeOrders,
  updateStocktakeOrder,
  type EcStocktakeOrder,
} from '@/api/ecommerce/stocktake'
import { fetchInventorySkuOptions, type EcInventorySkuOption } from '@/api/ecommerce/inventory'
import { fetchFactoryOptions, type EcFactory } from '@/api/ecommerce/factory'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { TABLE_ACTIONS_CELL_CLASS } from '@/constants/table'
import { formatDate, formatDateTime } from '@/utils/date'

const props = defineProps<{ modelValue: boolean; factoryId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; refreshed: [] }>()

const { t } = useI18n()
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const saving = ref(false)
const keyword = ref('')
const statusFilter = ref<string | undefined>()
const factoryOptions = ref<EcFactory[]>([])
const inventorySkuOptions = ref<EcInventorySkuOption[]>([])

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination(
  (p, ps) =>
    fetchStocktakeOrders(keyword.value.trim() || undefined, statusFilter.value, props.factoryId, {
      page: p,
      pageSize: ps,
    }),
)

const formVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref({
  factoryId: undefined as number | undefined,
  remark: '',
  stocktakeTime: '',
  lines: [{ skuCode: '', actualQuantity: 0 as number | undefined }],
})

function statusLabel(status: string) {
  if (status === 'DRAFT') return t('ecommerce.inbound.statusDraft')
  if (status === 'CONFIRMED') return t('ecommerce.stocktake.statusConfirmed')
  if (status === 'CANCELLED') return t('ecommerce.inbound.statusCancelled')
  return status
}

function statusTagType(status: string) {
  if (status === 'CONFIRMED') return 'success'
  if (status === 'CANCELLED') return 'info'
  return 'warning'
}

function skuOptionLabel(opt: EcInventorySkuOption) {
  const parts = [opt.skuCode]
  if (opt.specName) parts.push(opt.specName)
  if (opt.hasInventory) parts.push(`${t('ecommerce.inventory.quantity')}:${opt.quantity}`)
  else parts.push(t('ecommerce.stocktake.noInventory'))
  return parts.join(' · ')
}

function rowBookQty(skuCode: string) {
  const opt = inventorySkuOptions.value.find((o) => o.skuCode === skuCode)
  return opt?.quantity ?? '—'
}

async function searchSkuOptions(query: string) {
  inventorySkuOptions.value = await fetchInventorySkuOptions(form.value.factoryId ?? props.factoryId, query || undefined)
}

function addLine() {
  form.value.lines.push({ skuCode: '', actualQuantity: 0 })
}

function resetForm() {
  form.value = {
    factoryId: props.factoryId,
    remark: '',
    stocktakeTime: formatDateTime(new Date()),
    lines: [{ skuCode: '', actualQuantity: 0 }],
  }
}

async function openCreate() {
  editingId.value = null
  resetForm()
  await searchSkuOptions('')
  formVisible.value = true
}

async function openEdit(id: number) {
  editingId.value = id
  const order = await fetchStocktakeOrder(id)
  form.value = {
    factoryId: order.factoryId ?? undefined,
    remark: order.remark || '',
    stocktakeTime: order.stocktakeTime || formatDateTime(new Date()),
    lines: (order.lines ?? []).map((line) => ({
      skuCode: line.skuCode,
      actualQuantity: line.actualQuantity ?? 0,
    })),
  }
  await searchSkuOptions('')
  formVisible.value = true
}

async function onSave() {
  const lines = form.value.lines.filter((l) => l.skuCode?.trim())
  if (!lines.length) {
    ElMessage.warning(t('ecommerce.inbound.linesRequired'))
    return
  }
  if (!form.value.stocktakeTime) {
    ElMessage.warning(t('ecommerce.stocktake.stocktakeTimeRequired'))
    return
  }
  saving.value = true
  try {
    const payload = {
      factoryId: form.value.factoryId ?? null,
      remark: form.value.remark?.trim() || undefined,
      stocktakeTime: form.value.stocktakeTime,
      lines: lines.map((l) => ({
        skuCode: l.skuCode.trim(),
        actualQuantity: l.actualQuantity ?? 0,
      })),
    }
    if (editingId.value) {
      await updateStocktakeOrder(editingId.value, payload)
    } else {
      await createStocktakeOrder(payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    formVisible.value = false
    await load()
    emit('refreshed')
  } finally {
    saving.value = false
  }
}

async function onConfirm(row: EcStocktakeOrder) {
  await ElMessageBox.confirm(t('ecommerce.stocktake.confirmHint'), { type: 'warning' })
  await confirmStocktakeOrder(row.id)
  ElMessage.success(t('ecommerce.stocktake.confirmSuccess'))
  await load()
  emit('refreshed')
}

async function onCancel(row: EcStocktakeOrder) {
  await ElMessageBox.confirm(t('ecommerce.stocktake.cancelConfirm', { orderNo: row.orderNo }), { type: 'warning' })
  await cancelStocktakeOrder(row.id)
  ElMessage.success(t('ecommerce.common.saved'))
  await load()
}

async function onDelete(row: EcStocktakeOrder) {
  await ElMessageBox.confirm(t('ecommerce.stocktake.deleteConfirm', { orderNo: row.orderNo }), { type: 'warning' })
  await deleteStocktakeOrder(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  await load()
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => load(true), 300)
})

onMounted(async () => {
  factoryOptions.value = await fetchFactoryOptions()
})
</script>

<style scoped lang="scss">
.drawer-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}
</style>
