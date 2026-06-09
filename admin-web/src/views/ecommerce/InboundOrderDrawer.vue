<template>

  <el-drawer

    v-model="visible"

    :title="t('ecommerce.inbound.orderDrawerTitle')"

    size="960px"

    destroy-on-close

    @closed="onClosed"

  >

    <div class="inbound-drawer">

      <div class="drawer-toolbar">

        <el-input

          v-model="keyword"

          :placeholder="t('ecommerce.inbound.orderSearchPlaceholder')"

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

          <el-option :label="t('ecommerce.inbound.statusConfirmed')" value="CONFIRMED" />

          <el-option :label="t('ecommerce.inbound.statusCancelled')" value="CANCELLED" />

        </el-select>

        <el-button type="primary" @click="openCreate">{{ t('ecommerce.inbound.createOrder') }}</el-button>

      </div>



      <el-table v-loading="loading" :data="records" stripe border size="small">

        <el-table-column prop="orderNo" :label="t('ecommerce.inbound.orderNo')" width="168" fixed />

        <el-table-column prop="factoryName" :label="t('ecommerce.inventory.factory')" width="110" show-overflow-tooltip />

        <el-table-column :label="t('ecommerce.inbound.orderTime')" width="160">

          <template #default="{ row }">{{ formatDate(row.orderTime) }}</template>

        </el-table-column>

        <el-table-column :label="t('ecommerce.inbound.expectedDeliveryTime')" width="160">

          <template #default="{ row }">{{ formatDate(row.expectedDeliveryTime) }}</template>

        </el-table-column>

        <el-table-column :label="t('ecommerce.inbound.actualReceiptTime')" width="160">

          <template #default="{ row }">{{ formatDate(row.actualReceiptTime) }}</template>

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

              <el-button

                v-if="row.status === 'DRAFT'"

                link

                type="primary"

                :title="t('ecommerce.inventory.edit')"

                @click.stop="openEdit(row)"

              >

                <el-icon><Edit /></el-icon>

              </el-button>

              <el-button

                v-if="row.status === 'DRAFT'"

                link

                type="success"

                :title="t('ecommerce.inbound.confirm')"

                @click.stop="openConfirm(row)"

              >

                <el-icon><Check /></el-icon>

              </el-button>

              <el-button

                v-if="row.status === 'DRAFT'"

                link

                type="warning"

                :title="t('ecommerce.inbound.cancel')"

                @click.stop="onCancel(row)"

              >

                <el-icon><Close /></el-icon>

              </el-button>

              <el-button

                v-if="row.status === 'DRAFT'"

                link

                type="danger"

                :title="t('ecommerce.inventory.delete')"

                @click.stop="onDelete(row)"

              >

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

      :title="editingId ? t('ecommerce.inbound.editOrderTitle') : t('ecommerce.inbound.createOrderTitle')"

      width="760px"

      destroy-on-close

      append-to-body

    >

      <el-form label-width="108px">

        <el-form-item :label="t('ecommerce.inbound.orderTime')" required>

          <el-date-picker

            v-model="form.orderTime"

            type="date"

            value-format="YYYY-MM-DD"

            format="YYYY-MM-DD"

            :placeholder="t('ecommerce.inbound.orderTime')"

            style="width: 100%"

          />

        </el-form-item>

        <el-form-item :label="t('ecommerce.inbound.expectedDeliveryTime')" required>

          <el-date-picker

            v-model="form.expectedDeliveryTime"

            type="date"

            value-format="YYYY-MM-DD"

            format="YYYY-MM-DD"

            :placeholder="t('ecommerce.inbound.expectedDeliveryTime')"

            style="width: 100%"

          />

        </el-form-item>

        <el-form-item :label="t('ecommerce.inventory.factory')">

          <el-select

            v-model="form.factoryId"

            clearable

            filterable

            :placeholder="t('ecommerce.inbound.factoryOptional')"

            style="width: 100%"

            @change="onFormFactoryChange"

          >

            <el-option v-for="f in factoryOptions" :key="f.id" :label="f.name" :value="f.id" />

          </el-select>

        </el-form-item>

        <el-form-item :label="t('ecommerce.inbound.remark')">

          <el-input v-model="form.remark" type="textarea" :rows="2" />

        </el-form-item>

        <el-form-item :label="t('ecommerce.inbound.lines')">

          <div class="lines-toolbar">

            <el-button size="small" type="primary" @click="addLine">{{ t('ecommerce.inbound.addLine') }}</el-button>

          </div>

          <el-table :data="form.lines" border size="small" max-height="320">

            <el-table-column :label="t('ecommerce.inventory.skuCode')" min-width="200">

              <template #default="{ row }">

                <el-select

                  v-model="row.skuCode"

                  filterable

                  :placeholder="t('ecommerce.inventory.skuCodePlaceholder')"

                  style="width: 100%"

                >

                  <el-option

                    v-for="opt in filteredSkuOptions"

                    :key="opt.skuCode"

                    :label="skuOptionLabel(opt)"

                    :value="opt.skuCode"

                  />

                </el-select>

              </template>

            </el-table-column>

            <el-table-column :label="t('ecommerce.inbound.orderedQty')" width="140">

              <template #default="{ row }">

                <el-input-number v-model="row.quantity" :min="1" :step="1" controls-position="right" style="width: 100%" />

              </template>

            </el-table-column>

            <el-table-column width="56" align="center">

              <template #default="{ $index }">

                <el-button link type="danger" :disabled="form.lines.length <= 1" @click="removeLine($index)">

                  <el-icon><Delete /></el-icon>

                </el-button>

              </template>

            </el-table-column>

          </el-table>

        </el-form-item>

      </el-form>

      <template #footer>

        <el-button @click="formVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>

        <el-button type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.common.save') }}</el-button>

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

  </el-drawer>

</template>



<script setup lang="ts">

import { computed, ref, watch } from 'vue'

import { useI18n } from 'vue-i18n'

import { ElMessage, ElMessageBox } from 'element-plus'

import { Check, Close, Delete, Edit } from '@element-plus/icons-vue'

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

import { fetchFactoryOptions, type EcFactory } from '@/api/ecommerce/factory'

import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { TABLE_ACTIONS_CELL_CLASS } from '@/constants/table'
import { formatDate, todayDateString, toApiDateTime } from '@/utils/date'



const props = defineProps<{ modelValue: boolean }>()

const emit = defineEmits<{ 'update:modelValue': [boolean]; refreshed: [] }>()



const { t } = useI18n()



const visible = computed({

  get: () => props.modelValue,

  set: (v) => emit('update:modelValue', v),

})



const saving = ref(false)

const confirmSaving = ref(false)

const keyword = ref('')

const statusFilter = ref<string | undefined>()

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination(
  (p, ps) =>
    fetchInboundOrders(keyword.value.trim() || undefined, statusFilter.value, undefined, {
      page: p,
      pageSize: ps,
    }),
)

const factoryOptions = ref<EcFactory[]>([])

const skuOptions = ref<EcInventorySkuOption[]>([])



const formVisible = ref(false)

const confirmVisible = ref(false)

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

  await load()

}



async function loadSkuOptions() {

  skuOptions.value = await fetchInventorySkuOptions(form.value.factoryId)

}



function resetForm() {

  form.value = {

    factoryId: undefined,

    remark: '',

    orderTime: todayDateString(),

    expectedDeliveryTime: '',

    lines: [{ skuCode: '', quantity: 1 }],

  }

}



async function openCreate() {

  editingId.value = null

  resetForm()

  await loadSkuOptions()

  formVisible.value = true

}



async function openEdit(row: EcInboundOrder) {

  editingId.value = row.id

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

  if (!form.value.orderTime) {

    ElMessage.warning(t('ecommerce.inbound.orderTimeRequired'))

    return

  }

  if (!form.value.expectedDeliveryTime) {

    ElMessage.warning(t('ecommerce.inbound.expectedDeliveryTimeRequired'))

    return

  }



  const lines = form.value.lines.filter((line) => line.skuCode && line.quantity > 0)

  if (!lines.length) {

    ElMessage.warning(t('ecommerce.inbound.linesRequired'))

    return

  }



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



function onClosed() {

  keyword.value = ''

  statusFilter.value = undefined

}



let searchTimer: ReturnType<typeof setTimeout> | null = null

watch(keyword, () => {

  if (searchTimer) clearTimeout(searchTimer)

  searchTimer = setTimeout(() => load(true), 300)

})



watch(

  () => props.modelValue,

  async (open) => {

    if (open) {

      factoryOptions.value = await fetchFactoryOptions()

      await loadOrders()

    }

  },

)

</script>



<style scoped lang="scss">

.drawer-toolbar {

  display: flex;

  gap: 12px;

  margin-bottom: 16px;

  flex-wrap: wrap;

  align-items: center;

}



.lines-toolbar {

  margin-bottom: 8px;

}



.confirm-hint {

  margin: 0 0 12px;

  color: var(--el-text-color-secondary);

  font-size: 13px;

}

</style>

