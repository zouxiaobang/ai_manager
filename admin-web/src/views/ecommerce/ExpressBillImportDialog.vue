<template>
  <el-dialog
    v-model="visible"
    :title="t('ecommerce.monthlySettlement.importExpressBillTitle')"
    width="1000px"
    destroy-on-close
    @open="onOpen"
    @closed="onClosed"
  >
    <el-form label-width="96px" class="express-bill-form">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="t('ecommerce.monthlySettlement.monthPlaceholder')" required>
            <el-date-picker
              v-model="billMonth"
              type="month"
              value-format="YYYY-MM"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('ecommerce.monthlySettlement.expressStation')" required>
            <el-select
              v-model="expressStationId"
              filterable
              style="width: 100%"
              :placeholder="t('ecommerce.monthlySettlement.expressStationPlaceholder')"
              @change="onStationChange"
            >
              <el-option v-for="s in expressStations" :key="s.id" :label="s.name" :value="s.id" />
              <el-option :label="t('ecommerce.monthlySettlement.otherExpressStation')" :value="EXPRESS_STATION_OTHER" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="t('ecommerce.monthlySettlement.includeLabelPrice')">
        <div class="label-price-option">
          <el-checkbox v-model="includeLabelPrice" :disabled="!canIncludeLabelPrice">
            {{ t('ecommerce.monthlySettlement.includeLabelPriceLabel') }}
          </el-checkbox>
          <span v-if="selectedStationLabelPrice != null" class="label-price-hint">
            {{ t('ecommerce.monthlySettlement.stationLabelPrice', { price: formatMoney(selectedStationLabelPrice) }) }}
          </span>
          <span v-else-if="expressStationId" class="label-price-hint label-price-hint--muted">
            {{ t('ecommerce.monthlySettlement.stationLabelPriceUnset') }}
          </span>
        </div>
      </el-form-item>
    </el-form>

    <el-tabs v-model="activeTab">
      <el-tab-pane :label="t('ecommerce.monthlySettlement.expressBillTabUpload')" name="upload">
        <div class="express-bill-upload-block">
          <el-upload
            ref="uploadRef"
            class="express-bill-upload"
            drag
            :auto-upload="false"
            :show-file-list="false"
            accept=".csv,.txt,.xlsx,.xls"
            @change="onFileChange"
          >
            <el-icon class="express-bill-upload__icon"><UploadFilled /></el-icon>
            <p v-if="billFile" class="express-bill-upload__name">{{ billFile.name }}</p>
            <p v-else class="express-bill-upload__trigger">{{ t('ecommerce.monthlySettlement.expressBillUpload') }}</p>
          </el-upload>
          <p class="express-bill-hint">{{ t('ecommerce.monthlySettlement.expressBillHint') }}</p>
        </div>

        <el-collapse v-model="mappingCollapse" class="mapping-collapse">
          <el-collapse-item :title="t('ecommerce.monthlySettlement.columnMapping')" name="mapping">
            <div class="mapping-block__actions">
              <el-button link type="primary" :loading="savingProfile" @click="saveColumnProfile">
                {{ t('ecommerce.monthlySettlement.saveColumnMapping') }}
              </el-button>
            </div>
            <el-row :gutter="12" class="mapping-row-settings">
              <el-col :span="8">
                <span class="mapping-label">{{ t('ecommerce.monthlySettlement.headerRow') }}</span>
                <el-input-number v-model="headerRow" :min="1" :max="20" size="small" />
              </el-col>
              <el-col :span="8">
                <span class="mapping-label">{{ t('ecommerce.monthlySettlement.dataStartRow') }}</span>
                <el-input-number v-model="dataStartRow" :min="1" :max="100" size="small" />
              </el-col>
            </el-row>
            <el-table :data="mappingRows" border size="small" class="mapping-table">
              <el-table-column :label="t('ecommerce.monthlySettlement.systemField')" min-width="140">
                <template #default="{ row }">
                  {{ row.label }}
                  <el-tag v-if="row.required" size="small" type="danger">{{ t('sysImport.required') }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="t('ecommerce.monthlySettlement.fileColumn')" min-width="200">
                <template #default="{ row }">
                  <el-select
                    v-model="columnMapping[row.key]"
                    clearable
                    filterable
                    allow-create
                    default-first-option
                    style="width: 100%"
                  >
                    <el-option v-for="col in docColumns" :key="col" :label="col" :value="col" />
                  </el-select>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>

        <div class="tab-actions">
          <el-button type="primary" :loading="uploading" :disabled="!canUpload" @click="submitUpload">
            {{ t('ecommerce.monthlySettlement.expressBillSubmit') }}
          </el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('ecommerce.monthlySettlement.expressBillTabManual')" name="manual">
        <div class="manual-toolbar">
          <el-button :disabled="!canPrepareManual" :loading="preparingManual" @click="prepareManualList">
            {{ t('ecommerce.monthlySettlement.prepareManualList') }}
          </el-button>
          <el-button @click="addManualRow">{{ t('ecommerce.monthlySettlement.addManualRow') }}</el-button>
          <span v-if="currentBillId" class="manual-bill-id">
            {{ t('ecommerce.monthlySettlement.currentBillId', { id: currentBillId }) }}
          </span>
        </div>
        <el-table :data="manualLines" border size="small" max-height="360" class="manual-lines-table">
          <el-table-column :label="t('ecommerce.salesOrder.shop')" width="100" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="readonly-cell">{{ row.shopName || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('ecommerce.salesOrder.payTime')" width="160">
            <template #default="{ row }">
              <span class="readonly-cell">{{ formatManualShipTime(row.shipTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('ecommerce.monthlySettlement.settlementDestination')" min-width="100" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="readonly-cell">{{ row.settlementDestination || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('ecommerce.salesOrder.platformOrderNo')" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="readonly-cell">{{ row.platformOrderNo || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('ecommerce.salesOrder.trackingNumber')" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="readonly-cell">{{ row.trackingNumber || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('ecommerce.monthlySettlement.weight')" min-width="120">
            <template #default="{ row }">
              <el-input-number
                v-model="row.weight"
                class="manual-num-input"
                :min="0"
                :precision="3"
                size="small"
                controls-position="right"
              />
            </template>
          </el-table-column>
          <el-table-column :label="t('ecommerce.monthlySettlement.freightAmount')" min-width="120">
            <template #default="{ row }">
              <el-input-number
                v-model="row.freightAmount"
                class="manual-num-input"
                :min="0"
                :precision="2"
                size="small"
                controls-position="right"
              />
            </template>
          </el-table-column>
          <el-table-column :label="t('ecommerce.product.actions')" width="48" fixed="right" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" :title="t('ecommerce.monthlySettlement.delete')" @click="manualLines.splice($index, 1)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="tab-actions">
          <el-button type="primary" :loading="savingManual" :disabled="!currentBillId || !manualLines.length" @click="saveManual">
            {{ t('ecommerce.monthlySettlement.saveManualLines') }}
          </el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('ecommerce.monthlySettlement.expressBillTabRecords')" name="records">
        <el-table v-loading="loadingRecords" :data="records" border size="small" max-height="400">
          <el-table-column :label="t('ecommerce.monthlySettlement.importTime')" width="160">
            <template #default="{ row }">{{ row.createTime }}</template>
          </el-table-column>
          <el-table-column prop="expressStationName" :label="t('ecommerce.monthlySettlement.expressStation')" width="120" />
          <el-table-column prop="importMode" :label="t('ecommerce.monthlySettlement.importMode')" width="90">
            <template #default="{ row }">{{ importModeLabel(row.importMode) }}</template>
          </el-table-column>
          <el-table-column prop="fileName" :label="t('ecommerce.monthlySettlement.expressBillFile')" min-width="140" show-overflow-tooltip />
          <el-table-column prop="totalRows" :label="t('ecommerce.monthlySettlement.totalRows')" width="80" />
          <el-table-column prop="matchedRows" :label="t('ecommerce.monthlySettlement.matchedRows')" width="80" />
          <el-table-column prop="unmatchedRows" :label="t('ecommerce.monthlySettlement.unmatchedRows')" width="90" />
          <el-table-column prop="gapOrderRows" :label="t('ecommerce.monthlySettlement.gapOrderRows')" width="100" />
          <el-table-column :label="t('ecommerce.monthlySettlement.includeLabelPrice')" width="100">
            <template #default="{ row }">
              {{ row.includeLabelPrice ? t('ecommerce.monthlySettlement.yes') : t('ecommerce.monthlySettlement.no') }}
            </template>
          </el-table-column>
          <el-table-column prop="manualAppliedRows" :label="t('ecommerce.monthlySettlement.manualAppliedRows')" width="100" />
          <el-table-column :label="t('ecommerce.product.actions')" width="90" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openBillManual(row)">
                {{ t('ecommerce.monthlySettlement.viewManual') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="visible = false">{{ t('ecommerce.common.cancel') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type UploadFile, type UploadInstance } from 'element-plus'
import { Delete, UploadFilled } from '@element-plus/icons-vue'
import { fetchExpressStations, type EcExpressStation } from '@/api/ecommerce/express'
import {
  BIZ_SETTLEMENT_EXPRESS_BILL,
  createImportProfile,
  defaultExpressBillProfileName,
  expressStationScopeKey,
  fetchImportFields,
  fetchImportProfileByScope,
  updateImportProfile,
  type SysImportField,
} from '@/api/sys/import'
import {
  fetchExpressBillRecords,
  fetchManualExpressBillLines,
  EXPRESS_STATION_OTHER,
  prepareManualExpressBill,
  previewExpressBillColumns,
  saveManualExpressBillLines,
  uploadSettlementExpressBill,
  type ExpressBillLine,
  type ExpressBillRecord,
} from '@/api/ecommerce/monthlySettlement'
import { autoMatchColumnMapping } from '@/utils/importColumnMapping'

const props = defineProps<{
  modelValue: boolean
  initialMonth?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  imported: []
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const billMonth = ref('')
const expressStationId = ref<number | undefined>()
const includeLabelPrice = ref(true)
const expressStations = ref<EcExpressStation[]>([])
const activeTab = ref('upload')
const mappingCollapse = ref<string[]>([])
const billFile = ref<File | null>(null)
const uploadRef = ref<UploadInstance | null>(null)
const docColumns = ref<string[]>([])
const columnMapping = ref<Record<string, string>>({})
const importFields = ref<SysImportField[]>([])
const profileId = ref<number | undefined>()
const headerRow = ref(1)
const dataStartRow = ref(2)
const uploading = ref(false)
const savingProfile = ref(false)
const preparingManual = ref(false)
const savingManual = ref(false)
const loadingRecords = ref(false)
const currentBillId = ref<number | null>(null)
const manualLines = ref<ExpressBillLine[]>([])
const records = ref<ExpressBillRecord[]>([])

const mappingRows = computed(() =>
  importFields.value.map((f) => ({
    key: f.key,
    label: f.labelZh,
    required: f.required,
  })),
)

const canUpload = computed(
  () =>
    !!billMonth.value &&
    expressStationId.value != null &&
    expressStationId.value !== EXPRESS_STATION_OTHER &&
    !!billFile.value &&
    importFields.value.every((f) => !f.required || !!columnMapping.value[f.key]?.trim()) &&
    !uploading.value,
)

const canPrepareManual = computed(
  () => billMonth.value && expressStationId.value != null && !preparingManual.value,
)

const selectedStationLabelPrice = computed(() => {
  const station = expressStations.value.find((s) => s.id === expressStationId.value)
  const price = station?.labelPrice
  return price != null && price > 0 ? price : null
})

const canIncludeLabelPrice = computed(() => selectedStationLabelPrice.value != null)

function syncIncludeLabelPriceDefault() {
  includeLabelPrice.value = canIncludeLabelPrice.value
}

function formatMoney(v?: number | null) {
  if (v == null) return '—'
  return `¥${Number(v).toFixed(2)}`
}

function defaultMapping(): Record<string, string> {
  const mapping: Record<string, string> = {}
  for (const f of importFields.value) {
    mapping[f.key] = ''
  }
  return mapping
}

async function loadImportFields() {
  importFields.value = await fetchImportFields(BIZ_SETTLEMENT_EXPRESS_BILL)
}

async function loadStations() {
  const page = await fetchExpressStations(undefined, { page: 1, pageSize: 200 })
  expressStations.value = page.records ?? []
}

async function loadColumnProfile() {
  if (expressStationId.value == null || expressStationId.value === EXPRESS_STATION_OTHER) return
  const profile = await fetchImportProfileByScope(
    BIZ_SETTLEMENT_EXPRESS_BILL,
    expressStationScopeKey(expressStationId.value),
  )
  profileId.value = profile.id
  columnMapping.value = { ...defaultMapping(), ...profile.columnMapping }
  headerRow.value = profile.headerRow ?? 1
  dataStartRow.value = profile.dataStartRow ?? 2
}

async function loadRecords() {
  if (!billMonth.value) return
  loadingRecords.value = true
  try {
    records.value = await fetchExpressBillRecords(billMonth.value)
  } finally {
    loadingRecords.value = false
  }
}

async function onOpen() {
  billMonth.value = props.initialMonth || billMonth.value
  activeTab.value = 'upload'
  mappingCollapse.value = []
  billFile.value = null
  currentBillId.value = null
  manualLines.value = []
  docColumns.value = []
  profileId.value = undefined
  uploadRef.value?.clearFiles()
  await loadImportFields()
  await loadStations()
  if (expressStationId.value != null && expressStationId.value !== EXPRESS_STATION_OTHER) {
    await loadColumnProfile()
  } else {
    columnMapping.value = defaultMapping()
  }
  syncIncludeLabelPriceDefault()
  await loadRecords()
}

function onClosed() {
  billFile.value = null
  uploadRef.value?.clearFiles()
}

async function onStationChange() {
  syncIncludeLabelPriceDefault()
  await loadColumnProfile()
  if (billFile.value) {
    await refreshDocColumns()
  }
}

async function onFileChange(uploadFile: UploadFile) {
  billFile.value = uploadFile.raw ?? null
  if (billFile.value) {
    await refreshDocColumns()
  }
}

async function refreshDocColumns() {
  if (!billFile.value) return
  docColumns.value = await previewExpressBillColumns(billFile.value, headerRow.value)
  columnMapping.value = autoMatchColumnMapping(importFields.value, docColumns.value, columnMapping.value)
  applyExpressBillColumnAliases()
}

const EXPRESS_BILL_COLUMN_ALIASES: Record<string, string[]> = {
  ship_time: ['账单时间', '结算时间', '出账时间', '账单日期'],
  tracking_number: ['运单号码', '快件单号', '面单号'],
  freight_amount: ['应付运费', '账单金额', '费用合计'],
  settlement_destination: ['目的省', '目的市', '收件省', '收件市'],
  weight: ['结算重量', '结算重', '称重重量'],
}

function applyExpressBillColumnAliases() {
  for (const field of importFields.value) {
    if (columnMapping.value[field.key]?.trim()) continue
    const aliases = EXPRESS_BILL_COLUMN_ALIASES[field.key] ?? []
    const hit = docColumns.value.find((col) => {
      const text = col.trim()
      return aliases.some((alias) => text === alias || text.includes(alias))
    })
    if (hit) columnMapping.value[field.key] = hit
  }
}

watch(headerRow, () => {
  if (billFile.value) refreshDocColumns()
})

async function saveColumnProfile() {
  if (expressStationId.value == null || expressStationId.value === EXPRESS_STATION_OTHER) {
    ElMessage.warning(t('ecommerce.monthlySettlement.expressStationRequired'))
    return
  }
  const station = expressStations.value.find((s) => s.id === expressStationId.value)
  const scopeKey = expressStationScopeKey(expressStationId.value)
  const payload = {
    name: defaultExpressBillProfileName(station?.name ?? `#${expressStationId.value}`),
    bizType: BIZ_SETTLEMENT_EXPRESS_BILL,
    scopeKey,
    columnMapping: columnMapping.value,
    headerRow: headerRow.value,
    dataStartRow: dataStartRow.value,
  }
  savingProfile.value = true
  try {
    const saved = profileId.value
      ? await updateImportProfile(profileId.value, payload)
      : await createImportProfile(payload)
    profileId.value = saved.id
    ElMessage.success(t('ecommerce.common.saved'))
  } finally {
    savingProfile.value = false
  }
}

async function submitUpload() {
  if (!billMonth.value) {
    ElMessage.warning(t('ecommerce.monthlySettlement.monthRequired'))
    return
  }
  if (expressStationId.value == null) {
    ElMessage.warning(t('ecommerce.monthlySettlement.expressStationRequired'))
    return
  }
  if (expressStationId.value === EXPRESS_STATION_OTHER) {
    ElMessage.warning(t('ecommerce.monthlySettlement.expressStationRequired'))
    return
  }
  if (!billFile.value) {
    ElMessage.warning(t('ecommerce.monthlySettlement.expressBillFileRequired'))
    return
  }
  uploading.value = true
  try {
    if (billFile.value) {
      await refreshDocColumns()
    }
    const res = await uploadSettlementExpressBill({
      month: billMonth.value,
      expressStationId: expressStationId.value,
      file: billFile.value,
      columnMapping: columnMapping.value,
      headerRow: headerRow.value,
      dataStartRow: dataStartRow.value,
      includeLabelPrice: includeLabelPrice.value,
    })
    currentBillId.value = res.billId
    ElMessage.success(
      t('ecommerce.monthlySettlement.expressBillImported', {
        matched: res.matchedRows,
        unmatched: res.unmatchedRows,
        skipped: res.overwrittenRows ?? 0,
      }),
    )
    await loadRecords()
    emit('imported')
  } finally {
    uploading.value = false
  }
}

async function prepareManualList() {
  if (!billMonth.value || expressStationId.value == null) return
  preparingManual.value = true
  try {
    const res = await prepareManualExpressBill(
      billMonth.value,
      expressStationId.value,
      includeLabelPrice.value,
    )
    currentBillId.value = res.billId
    await loadManualLines(res.billId)
    ElMessage.success(t('ecommerce.monthlySettlement.manualListPrepared', { count: res.manualPendingRows ?? 0 }))
    emit('imported')
  } finally {
    preparingManual.value = false
  }
}

async function loadManualLines(billId: number) {
  manualLines.value = (await fetchManualExpressBillLines(billId)).map((line) => ({
    ...line,
    freightAmount: line.freightAmount ?? null,
    shipTime: formatShipTimeForPicker(line.shipTime),
  }))
}

function formatShipTimeForPicker(value?: string | null): string | undefined {
  if (!value) return undefined
  return value.replace('T', ' ').slice(0, 19)
}

function formatManualShipTime(value?: string | null): string {
  if (!value) return '—'
  return formatShipTimeForPicker(value) ?? '—'
}

function addManualRow() {
  manualLines.value.push({
    platformOrderNo: '',
    orderNo: '',
    trackingNumber: '',
    freightAmount: null,
    settlementDestination: '',
    weight: null,
    shipTime: undefined,
    source: 'MANUAL',
  })
}

async function saveManual() {
  if (!currentBillId.value) return
  savingManual.value = true
  try {
    const res = await saveManualExpressBillLines({
      billId: currentBillId.value,
      expressStationId: expressStationId.value ?? undefined,
      lines: manualLines.value.map((line) => ({
        lineId: line.id,
        orderId: line.orderId,
        trackingNumber: line.trackingNumber?.trim() || undefined,
        freightAmount: line.freightAmount,
        settlementDestination: line.settlementDestination?.trim() || undefined,
        weight: line.weight,
        shipTime: line.shipTime || undefined,
        remark: line.remark,
      })),
    })
    ElMessage.success(
      t('ecommerce.monthlySettlement.manualSaved', { applied: res.manualAppliedRows ?? 0 }),
    )
    await loadManualLines(currentBillId.value)
    await loadRecords()
    emit('imported')
  } finally {
    savingManual.value = false
  }
}

async function openBillManual(record: ExpressBillRecord) {
  currentBillId.value = record.id
  expressStationId.value = record.otherExpress ? EXPRESS_STATION_OTHER : record.expressStationId
  activeTab.value = 'manual'
  await loadManualLines(record.id)
}

function importModeLabel(mode?: string) {
  const map: Record<string, string> = {
    FILE: t('ecommerce.monthlySettlement.modeFile'),
    MANUAL: t('ecommerce.monthlySettlement.modeManual'),
    MIXED: t('ecommerce.monthlySettlement.modeMixed'),
  }
  return map[mode ?? ''] ?? mode ?? '—'
}

watch(billMonth, () => {
  if (visible.value) loadRecords()
})
</script>

<style scoped lang="scss">
.express-bill-form {
  margin-bottom: 8px;
}

.express-bill-upload-block {
  width: 100%;
}

.express-bill-upload {
  width: 100%;

  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
    padding: 20px 16px;
  }
}

.express-bill-upload__icon {
  font-size: 36px;
  color: var(--el-color-primary);
}

.express-bill-upload__name,
.express-bill-upload__trigger {
  margin: 8px 0 0;
  font-size: 13px;
  word-break: break-all;
}

.express-bill-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.mapping-collapse {
  margin-top: 16px;

  :deep(.el-collapse-item__header) {
    font-weight: 600;
    font-size: 13px;
  }
}

.mapping-block__actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.mapping-block {
  margin-top: 16px;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
    font-weight: 600;
    font-size: 13px;
  }
}

.mapping-row-settings {
  margin-bottom: 8px;
}

.mapping-label {
  display: inline-block;
  margin-right: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.mapping-table {
  margin-top: 4px;
}

.tab-actions {
  margin-top: 12px;
  text-align: right;
}

.manual-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.manual-bill-id {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.readonly-cell {
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.manual-lines-table {
  :deep(.manual-num-input) {
    width: 100%;
  }

  :deep(.manual-num-input .el-input__wrapper) {
    padding-left: 8px;
    padding-right: 30px;
  }
}

.label-price-option {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.label-price-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);

  &--muted {
    color: var(--el-text-color-placeholder);
  }
}
</style>
