<template>
  <div class="carton-panel">
    <div class="panel-toolbar">
      <el-input
        v-model="keyword"
        :placeholder="t('ecommerce.carton.searchPlaceholder')"
        clearable
        style="width: 320px"
      />
      <el-button type="primary" @click="openCreate">{{ t('ecommerce.carton.add') }}</el-button>
    </div>

    <div class="panel-actions">
      <el-button @click="openCalculate">{{ t('ecommerce.carton.calculate') }}</el-button>
      <el-button :loading="backfillRunning" @click="onResetProductCartons">
        {{ t('ecommerce.carton.resetProductCartons') }}
      </el-button>
    </div>

    <el-table v-loading="loading" :data="records" stripe border>
      <el-table-column prop="name" :label="t('ecommerce.carton.name')" min-width="160" />
      <el-table-column prop="factoryName" :label="t('ecommerce.carton.factory')" min-width="140" show-overflow-tooltip />
      <el-table-column :label="t('ecommerce.carton.specSize')" min-width="180">
        <template #default="{ row }">{{ formatSize(row) }}</template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.carton.unitPrice')" width="100" align="right">
        <template #default="{ row }">{{ formatPrice(row.unitPrice) }}</template>
      </el-table-column>
      <el-table-column prop="remark" :label="t('ecommerce.carton.remark')" min-width="160" show-overflow-tooltip />
      <el-table-column :label="t('ecommerce.carton.updatedAt')" width="170">
        <template #default="{ row }">{{ formatDate(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column
        :label="t('ecommerce.carton.actions')"
        width="88"
        fixed="right"
        align="center"
        :class-name="TABLE_ACTIONS_CELL_CLASS"
      >
        <template #default="{ row }">
          <div class="table-actions-cell-inner" @click.stop>
            <el-button link type="primary" :title="t('ecommerce.carton.edit')" @click.stop="openEdit(row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button link type="danger" :title="t('ecommerce.carton.delete')" @click.stop="onDelete(row)">
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
      :title="editingId ? t('ecommerce.carton.editTitle') : t('ecommerce.carton.createTitle')"
      width="560px"
      destroy-on-close
    >
      <el-form :model="form" label-width="96px">
        <el-form-item :label="t('ecommerce.carton.name')" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('ecommerce.carton.factory')" required>
          <el-select
            v-model="form.factoryId"
            filterable
            :placeholder="t('ecommerce.carton.factoryPlaceholder')"
            style="width: 100%"
          >
            <el-option
              v-for="f in factoryOptions"
              :key="f.id"
              :label="f.name"
              :value="f.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('ecommerce.carton.specSize')">
          <div class="dim-row">
            <el-input-number v-model="form.lengthCm" :min="0" :precision="2" controls-position="right" :placeholder="t('ecommerce.carton.length')" />
            <span>×</span>
            <el-input-number v-model="form.widthCm" :min="0" :precision="2" controls-position="right" :placeholder="t('ecommerce.carton.width')" />
            <span>×</span>
            <el-input-number v-model="form.heightCm" :min="0" :precision="2" controls-position="right" :placeholder="t('ecommerce.carton.height')" />
            <span class="dim-unit">cm</span>
          </div>
        </el-form-item>
        <el-form-item :label="t('ecommerce.carton.unitPrice')">
          <el-input-number
            v-model="form.unitPrice"
            :min="0"
            :precision="2"
            :step="0.1"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('ecommerce.carton.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="calculateVisible"
      :title="t('ecommerce.carton.calculateTitle')"
      width="560px"
      destroy-on-close
    >
      <el-form label-width="108px">
        <el-form-item :label="t('ecommerce.carton.productSize')" required>
          <div class="dim-row">
            <el-input-number
              v-model="calcForm.lengthCm"
              :min="0"
              :precision="2"
              controls-position="right"
              :placeholder="t('ecommerce.carton.length')"
            />
            <span>×</span>
            <el-input-number
              v-model="calcForm.widthCm"
              :min="0"
              :precision="2"
              controls-position="right"
              :placeholder="t('ecommerce.carton.width')"
            />
            <span>×</span>
            <el-input-number
              v-model="calcForm.heightCm"
              :min="0"
              :precision="2"
              controls-position="right"
              :placeholder="t('ecommerce.carton.height')"
            />
            <span class="dim-unit">cm</span>
          </div>
        </el-form-item>
        <el-form-item :label="t('ecommerce.carton.factory')">
          <el-select
            v-model="calcForm.factoryId"
            clearable
            filterable
            :placeholder="t('ecommerce.carton.factoryOptional')"
            style="width: 100%"
          >
            <el-option v-for="f in factoryOptions" :key="f.id" :label="f.name" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="calcResult" :label="t('ecommerce.carton.matchedCarton')">
          <div class="calc-result">{{ formatCartonResult(calcResult.matchedCarton) }}</div>
        </el-form-item>
        <el-form-item v-if="calcResult" :label="t('ecommerce.carton.inventoryCarton')">
          <div class="calc-result">{{ formatCartonResult(calcResult.inventoryCarton) }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="calculateVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="calculating" @click="onCalculate">
          {{ t('ecommerce.carton.calculate') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="backfillProgressVisible"
      :title="t('ecommerce.carton.resetProgressTitle')"
      width="480px"
      :close-on-click-modal="false"
      :show-close="!backfillRunning"
    >
      <el-progress
        :percentage="backfillProgress"
        :status="backfillFailed ? 'exception' : backfillCompleted ? 'success' : undefined"
      />
      <p class="backfill-status">{{ backfillStatusText }}</p>
      <template #footer>
        <el-button :disabled="backfillRunning" @click="backfillProgressVisible = false">
          {{ t('ecommerce.common.close') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit } from '@element-plus/icons-vue'
import {
  calculateCarton,
  createCarton,
  deleteCarton,
  fetchBackfillTask,
  fetchCartons,
  startBackfillSkuCartonsAsync,
  updateCarton,
  type EcCarton,
  type EcCartonCalculateResult,
  type EcCartonSaveRequest,
} from '@/api/ecommerce/carton'
import { fetchFactoryOptions, type EcFactory } from '@/api/ecommerce/factory'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { TABLE_ACTIONS_CELL_CLASS } from '@/constants/table'
import { formatDate } from '@/utils/date'

const { t } = useI18n()

const saving = ref(false)
const keyword = ref('')
const factoryOptions = ref<EcFactory[]>([])

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination(
  (p, ps) => fetchCartons(keyword.value.trim() || undefined, { page: p, pageSize: ps }),
)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const calculateVisible = ref(false)
const calculating = ref(false)
const calcResult = ref<EcCartonCalculateResult | null>(null)
const backfillRunning = ref(false)
const backfillProgressVisible = ref(false)
const backfillProgress = ref(0)
const backfillStatusText = ref('')
const backfillCompleted = ref(false)
const backfillFailed = ref(false)

let backfillPollTimer: ReturnType<typeof setInterval> | null = null

const calcForm = reactive<{
  lengthCm: number | null
  widthCm: number | null
  heightCm: number | null
  factoryId?: number
}>({
  lengthCm: null,
  widthCm: null,
  heightCm: null,
  factoryId: undefined,
})

const form = reactive<{
  name: string
  factoryId?: number
  lengthCm: number | null
  widthCm: number | null
  heightCm: number | null
  unitPrice: number | null
  remark: string
}>({
  name: '',
  factoryId: undefined,
  lengthCm: null,
  widthCm: null,
  heightCm: null,
  unitPrice: null,
  remark: '',
})

function formatPrice(value: number | null | undefined) {
  if (value == null) return '—'
  return `¥${Number(value).toFixed(2)}`
}

function formatSize(row: EcCarton) {
  const { lengthCm, widthCm, heightCm } = row
  if (lengthCm == null && widthCm == null && heightCm == null) return '—'
  const parts = [lengthCm, widthCm, heightCm].map((v) => (v == null ? '—' : Number(v).toFixed(2)))
  return `${parts[0]} × ${parts[1]} × ${parts[2]} cm`
}

function resetForm() {
  form.name = ''
  form.factoryId = factoryOptions.value[0]?.id
  form.lengthCm = null
  form.widthCm = null
  form.heightCm = null
  form.unitPrice = null
  form.remark = ''
}

async function loadFactoryOptions() {
  factoryOptions.value = await fetchFactoryOptions()
}

async function loadCartons() {
  await load()
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => load(true), 300)
})

function formatCartonResult(carton: EcCarton | null | undefined) {
  if (!carton) return t('ecommerce.carton.noMatch')
  const size = formatSize(carton)
  const factory = carton.factoryName ? ` · ${carton.factoryName}` : ''
  return `${carton.name}（${size}）${factory}`
}

function resetCalcForm() {
  calcForm.lengthCm = null
  calcForm.widthCm = null
  calcForm.heightCm = null
  calcForm.factoryId = undefined
  calcResult.value = null
}

function openCalculate() {
  resetCalcForm()
  calculateVisible.value = true
}

async function onCalculate() {
  if (
    calcForm.lengthCm == null ||
    calcForm.widthCm == null ||
    calcForm.heightCm == null ||
    calcForm.lengthCm <= 0 ||
    calcForm.widthCm <= 0 ||
    calcForm.heightCm <= 0
  ) {
    ElMessage.warning(t('ecommerce.carton.productSizeRequired'))
    return
  }

  calculating.value = true
  try {
    calcResult.value = await calculateCarton(
      calcForm.lengthCm,
      calcForm.widthCm,
      calcForm.heightCm,
      calcForm.factoryId,
    )
  } finally {
    calculating.value = false
  }
}

function stopBackfillPoll() {
  if (backfillPollTimer) {
    clearInterval(backfillPollTimer)
    backfillPollTimer = null
  }
}

function updateBackfillProgress(task: {
  status: string
  total: number
  processed: number
  updated: number
  message?: string
}) {
  if (task.total > 0) {
    backfillProgress.value = Math.min(100, Math.round((task.processed / task.total) * 100))
  } else {
    backfillProgress.value = task.status === 'COMPLETED' ? 100 : 0
  }

  if (task.status === 'FAILED') {
    backfillFailed.value = true
    backfillStatusText.value = task.message || t('ecommerce.carton.resetFailed')
    return
  }

  if (task.status === 'COMPLETED') {
    backfillCompleted.value = true
    backfillStatusText.value = t('ecommerce.carton.resetDone', {
      total: task.total,
      updated: task.updated,
    })
    return
  }

  backfillStatusText.value = t('ecommerce.carton.resetRunning', {
    processed: task.processed,
    total: task.total,
    updated: task.updated,
  })
}

async function pollBackfillTask(taskId: string) {
  try {
    const task = await fetchBackfillTask(taskId)
    updateBackfillProgress(task)
    if (task.status === 'COMPLETED' || task.status === 'FAILED') {
      stopBackfillPoll()
      backfillRunning.value = false
      if (task.status === 'COMPLETED') {
        ElMessage.success(t('ecommerce.carton.resetDone', { total: task.total, updated: task.updated }))
      } else {
        ElMessage.error(task.message || t('ecommerce.carton.resetFailed'))
      }
    }
  } catch {
    stopBackfillPoll()
    backfillRunning.value = false
    backfillFailed.value = true
    backfillStatusText.value = t('ecommerce.carton.resetFailed')
  }
}

async function onResetProductCartons() {
  await ElMessageBox.confirm(t('ecommerce.carton.resetConfirm'), { type: 'warning' })

  backfillRunning.value = true
  backfillProgressVisible.value = true
  backfillProgress.value = 0
  backfillCompleted.value = false
  backfillFailed.value = false
  backfillStatusText.value = t('ecommerce.carton.resetStarting')

  try {
    const taskId = await startBackfillSkuCartonsAsync()
    stopBackfillPoll()
    backfillPollTimer = setInterval(() => pollBackfillTask(taskId), 1000)
    await pollBackfillTask(taskId)
  } catch {
    backfillRunning.value = false
    backfillFailed.value = true
    backfillStatusText.value = t('ecommerce.carton.resetFailed')
  }
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: EcCarton) {
  editingId.value = row.id
  form.name = row.name
  form.factoryId = row.factoryId ?? undefined
  form.lengthCm = row.lengthCm ?? null
  form.widthCm = row.widthCm ?? null
  form.heightCm = row.heightCm ?? null
  form.unitPrice = row.unitPrice ?? null
  form.remark = row.remark || ''
  dialogVisible.value = true
}

async function onSave() {
  if (!form.name.trim()) {
    ElMessage.warning(t('ecommerce.carton.nameRequired'))
    return
  }
  if (!form.factoryId) {
    ElMessage.warning(t('ecommerce.carton.factoryRequired'))
    return
  }

  saving.value = true
  try {
    const payload: EcCartonSaveRequest = {
      name: form.name.trim(),
      factoryId: form.factoryId,
      lengthCm: form.lengthCm,
      widthCm: form.widthCm,
      heightCm: form.heightCm,
      unitPrice: form.unitPrice,
      remark: form.remark?.trim() || undefined,
    }
    if (editingId.value) {
      await updateCarton(editingId.value, payload)
    } else {
      await createCarton(payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    dialogVisible.value = false
    await loadCartons()
  } finally {
    saving.value = false
  }
}

async function onDelete(row: EcCarton) {
  await ElMessageBox.confirm(
    t('ecommerce.carton.deleteConfirm', { name: row.name }),
    { type: 'warning' },
  )
  await deleteCarton(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  await loadCartons()
}

onMounted(async () => {
  await Promise.all([loadCartons(), loadFactoryOptions()])
})

onBeforeUnmount(() => {
  stopBackfillPoll()
})

defineExpose({ loadCartons })
</script>

<style scoped lang="scss">
.panel-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.panel-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.dim-row {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.dim-unit {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.calc-result {
  line-height: 1.6;
  word-break: break-word;
}

.backfill-status {
  margin: 12px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
