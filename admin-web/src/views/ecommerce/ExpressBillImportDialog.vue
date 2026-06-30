<template>
  <el-drawer
    v-model="visible"
    :title="t('ecommerce.monthlySettlement.expressBillWorkbenchTitle')"
    size="88%"
    destroy-on-close
    class="express-bill-drawer"
    @open="onOpen"
    @closed="onClosed"
  >
    <p class="express-bill-workbench__subtitle">{{ t('ecommerce.monthlySettlement.expressBillWorkbenchSubtitle') }}</p>

    <div class="express-bill-workbench">
      <aside class="express-bill-workbench__sidebar">
        <div class="sidebar-block">
          <div class="sidebar-block__label">{{ t('ecommerce.monthlySettlement.monthPlaceholder') }}</div>
          <el-date-picker
            v-if="!lockMonth"
            v-model="billMonth"
            type="month"
            value-format="YYYY-MM"
            style="width: 100%"
          />
          <div v-else class="sidebar-block__month-readonly">{{ formatBillMonth(billMonth) }}</div>
        </div>

        <div class="sidebar-block sidebar-block--stations">
          <div class="sidebar-block__label">{{ t('ecommerce.monthlySettlement.expressBillStationList') }}</div>
          <div class="station-list">
            <button
              v-for="station in expressStations"
              :key="station.id"
              type="button"
              class="station-item"
              :class="{ 'is-active': expressStationId === station.id }"
              @click="selectStation(station.id)"
            >
              <ExpressStationAvatar :station="station" size="xs" />
              <span class="station-item__name">{{ station.name }}</span>
              <span class="station-item__stat">{{ stationMatchLabel(station.id) }}</span>
            </button>
            <button
              type="button"
              class="station-item"
              :class="{ 'is-active': expressStationId === EXPRESS_STATION_OTHER }"
              @click="selectStation(EXPRESS_STATION_OTHER)"
            >
              <span class="station-item__avatar-placeholder">⋯</span>
              <span class="station-item__name">{{ t('ecommerce.monthlySettlement.otherExpressStation') }}</span>
              <span class="station-item__stat">{{ stationMatchLabel(EXPRESS_STATION_OTHER) }}</span>
            </button>
          </div>
          <button type="button" class="sidebar-records-link" @click="centerView = 'records'">
            {{ t('ecommerce.monthlySettlement.expressBillViewRecords') }}
          </button>
        </div>
      </aside>

      <main class="express-bill-workbench__main">
        <div class="main-toolbar">
          <el-radio-group v-model="centerView" size="large" class="main-toolbar__view-switch">
            <el-radio-button value="import">{{ t('ecommerce.monthlySettlement.expressBillCenterImport') }}</el-radio-button>
            <el-radio-button value="manual">{{ t('ecommerce.monthlySettlement.expressBillCenterManual') }}</el-radio-button>
            <el-radio-button value="records">{{ t('ecommerce.monthlySettlement.expressBillCenterRecords') }}</el-radio-button>
          </el-radio-group>

          <div v-if="centerView === 'import'" class="main-toolbar__options">
            <el-checkbox v-model="includeLabelPrice" :disabled="!canIncludeLabelPrice">
              {{ t('ecommerce.monthlySettlement.includeLabelPriceLabel') }}
            </el-checkbox>
            <span v-if="selectedStationLabelPrice != null" class="label-price-hint">
              {{ t('ecommerce.monthlySettlement.stationLabelPrice', { price: formatMoney(selectedStationLabelPrice) }) }}
            </span>
          </div>
          <div v-else-if="centerView === 'manual'" class="main-toolbar__options">
            <el-checkbox v-model="includeLabelPrice" :disabled="!canIncludeLabelPrice">
              {{ t('ecommerce.monthlySettlement.includeLabelPriceLabel') }}
            </el-checkbox>
            <span v-if="selectedStationLabelPrice != null" class="label-price-hint">
              {{ t('ecommerce.monthlySettlement.stationLabelPrice', { price: formatMoney(selectedStationLabelPrice) }) }}
            </span>
          </div>
        </div>

        <template v-if="centerView === 'import'">
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

          <div class="mapping-block">
            <div class="mapping-block__header">
              <span>{{ t('ecommerce.monthlySettlement.columnMapping') }}</span>
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
          </div>
        </template>

        <template v-else-if="centerView === 'manual'">
          <section class="manual-panel">
            <header class="manual-panel__head">
              <div class="manual-panel__intro">
                <h4 class="manual-panel__title">{{ t('ecommerce.monthlySettlement.expressBillCenterManual') }}</h4>
                <p class="manual-panel__hint">{{ t('ecommerce.monthlySettlement.manualPanelHint') }}</p>
              </div>
              <div class="manual-panel__stats">
                <div class="manual-stat">
                  <span class="manual-stat__label">{{ t('ecommerce.monthlySettlement.manualStatTotal') }}</span>
                  <strong class="manual-stat__value">{{ manualLines.length }}</strong>
                </div>
                <div class="manual-stat manual-stat--success">
                  <span class="manual-stat__label">{{ t('ecommerce.monthlySettlement.manualStatFilled') }}</span>
                  <strong class="manual-stat__value">{{ manualFilledCount }}</strong>
                </div>
                <div class="manual-stat manual-stat--warning">
                  <span class="manual-stat__label">{{ t('ecommerce.monthlySettlement.manualStatEmpty') }}</span>
                  <strong class="manual-stat__value">{{ manualEmptyFreightCount }}</strong>
                </div>
              </div>
            </header>

            <div class="manual-panel__toolbar">
              <div class="manual-panel__actions">
                <el-button
                  type="primary"
                  :disabled="!canPrepareManual"
                  :loading="preparingManual"
                  @click="prepareManualList"
                >
                  {{ t('ecommerce.monthlySettlement.prepareManualList') }}
                </el-button>
                <el-button plain @click="addManualRow">{{ t('ecommerce.monthlySettlement.addManualRow') }}</el-button>
              </div>
              <span v-if="currentBillId" class="manual-panel__batch">
                {{ t('ecommerce.monthlySettlement.currentBillId', { id: currentBillId }) }}
              </span>
            </div>

            <el-empty
              v-if="!manualLines.length"
              class="manual-panel__empty"
              :description="t('ecommerce.monthlySettlement.manualEmptyHint')"
              :image-size="80"
            >
              <template #description>
                <p class="manual-panel__empty-title">{{ t('ecommerce.monthlySettlement.manualEmptyTitle') }}</p>
                <p class="manual-panel__empty-desc">{{ t('ecommerce.monthlySettlement.manualEmptyHint') }}</p>
              </template>
            </el-empty>

            <div v-else class="manual-line-list">
              <article
                v-for="(row, index) in manualLines"
                :key="row.id ?? row.orderId ?? `manual-${index}`"
                class="manual-line-card"
                :class="{ 'is-filled': isManualLineFilled(row) }"
              >
                <div class="manual-line-card__main">
                  <div class="manual-line-card__order">
                    <div class="manual-line-card__shop">{{ row.shopName || '—' }}</div>
                    <button
                      v-if="row.orderId && row.platformOrderNo"
                      type="button"
                      class="manual-order-link manual-line-card__order-no"
                      @click="openManualLineOrderDetail(row)"
                    >
                      {{ row.platformOrderNo }}
                    </button>
                    <div v-else class="manual-line-card__order-no is-text">
                      {{ row.platformOrderNo || '—' }}
                    </div>
                    <div class="manual-line-card__meta">
                      <span v-if="row.trackingNumber" class="manual-line-card__tag">
                        {{ t('ecommerce.salesOrder.trackingNumber') }}：{{ row.trackingNumber }}
                      </span>
                      <span v-if="row.settlementDestination" class="manual-line-card__tag">
                        {{ row.settlementDestination }}
                      </span>
                      <span v-if="row.shipTime" class="manual-line-card__tag">
                        {{ formatManualShipTime(row.shipTime) }}
                      </span>
                    </div>
                  </div>

                  <div class="manual-line-card__inputs">
                    <label class="manual-input-field">
                      <span class="manual-input-field__label">{{ t('ecommerce.monthlySettlement.manualWeightHighlight') }}</span>
                      <el-input-number
                        v-model="row.weight"
                        class="manual-num-input"
                        :min="0"
                        :precision="3"
                        size="default"
                        controls-position="right"
                      />
                    </label>
                    <label class="manual-input-field manual-input-field--freight">
                      <span class="manual-input-field__label">{{ t('ecommerce.monthlySettlement.manualFreightHighlight') }}</span>
                      <el-input-number
                        v-model="row.freightAmount"
                        class="manual-num-input manual-num-input--freight"
                        :min="0"
                        :precision="2"
                        size="default"
                        controls-position="right"
                      />
                    </label>
                  </div>
                </div>

                <el-button
                  link
                  type="danger"
                  class="manual-line-card__remove"
                  :title="t('ecommerce.monthlySettlement.delete')"
                  @click="manualLines.splice(index, 1)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </article>
            </div>
          </section>
        </template>

        <template v-else>
          <section class="records-panel">
            <header class="records-panel__head">
              <div class="records-panel__intro">
                <h4 class="records-panel__title">{{ t('ecommerce.monthlySettlement.expressBillCenterRecords') }}</h4>
                <p class="records-panel__hint">{{ t('ecommerce.monthlySettlement.expressBillRecordsHint') }}</p>
              </div>
              <div class="records-panel__stats">
                <div class="records-stat">
                  <span class="records-stat__label">{{ t('ecommerce.monthlySettlement.expressBillRecordsBatchCount') }}</span>
                  <strong class="records-stat__value">{{ recordsPanelSummary.batchCount }}</strong>
                </div>
                <div class="records-stat records-stat--primary">
                  <span class="records-stat__label">{{ t('ecommerce.monthlySettlement.expressBillRecordsFileCount') }}</span>
                  <strong class="records-stat__value">{{ recordsPanelSummary.fileCount }}</strong>
                </div>
                <div class="records-stat records-stat--warning">
                  <span class="records-stat__label">{{ t('ecommerce.monthlySettlement.expressBillRecordsManualCount') }}</span>
                  <strong class="records-stat__value">{{ recordsPanelSummary.manualCount }}</strong>
                </div>
              </div>
            </header>

            <div class="records-panel__toolbar">
              <el-radio-group v-model="recordsModeFilter" size="small" class="records-panel__filter">
                <el-radio-button value="ALL">{{ t('ecommerce.monthlySettlement.expressBillRecordsFilterAll') }}</el-radio-button>
                <el-radio-button value="FILE">{{ t('ecommerce.monthlySettlement.modeFile') }}</el-radio-button>
                <el-radio-button value="MANUAL">{{ t('ecommerce.monthlySettlement.modeManual') }}</el-radio-button>
                <el-radio-button value="MIXED">{{ t('ecommerce.monthlySettlement.modeMixed') }}</el-radio-button>
              </el-radio-group>
              <span v-if="recordsPanelSummary.latestTime" class="records-panel__latest">
                {{ t('ecommerce.monthlySettlement.expressBillRecordsLatest', { time: recordsPanelSummary.latestTime }) }}
              </span>
            </div>

            <el-empty
              v-if="!loadingRecords && !displayedRecords.length"
              class="records-panel__empty"
              :image-size="80"
            >
              <template #description>
                <p class="records-panel__empty-title">{{ t('ecommerce.monthlySettlement.expressBillRecordsEmptyTitle') }}</p>
                <p class="records-panel__empty-desc">{{ t('ecommerce.monthlySettlement.expressBillRecordsEmpty') }}</p>
              </template>
            </el-empty>

            <div v-else v-loading="loadingRecords" class="records-list">
              <article
                v-for="record in displayedRecords"
                :key="record.id"
                class="records-card"
                :class="{ 'is-active': selectedRecordId === record.id }"
                @click="selectRecord(record)"
              >
                <div class="records-card__header">
                  <div class="records-card__identity">
                    <ExpressStationAvatar
                      v-if="stationForRecord(record)"
                      :station="stationForRecord(record)!"
                      size="xs"
                    />
                    <span v-else class="records-card__avatar-placeholder">⋯</span>
                    <div class="records-card__title-wrap">
                      <div class="records-card__title-row">
                        <span class="records-card__station">{{ recordStationName(record) }}</span>
                        <el-tag size="small" :type="importModeTagType(record.importMode)" effect="plain">
                          {{ importModeLabel(record.importMode) }}
                        </el-tag>
                      </div>
                      <div class="records-card__file" :title="recordFileLabel(record)">
                        {{ recordFileLabel(record) }}
                      </div>
                    </div>
                  </div>
                  <div class="records-card__meta">
                    <span class="records-card__time">{{ formatRecordTime(record.createTime) }}</span>
                    <span class="records-card__batch-id">#{{ record.id }}</span>
                  </div>
                </div>

                <div class="records-card__stats">
                  <div class="records-card__stat">
                    <span class="records-card__stat-label">{{ t('ecommerce.monthlySettlement.totalRows') }}</span>
                    <strong class="records-card__stat-value">{{ record.totalRows ?? 0 }}</strong>
                  </div>
                  <div class="records-card__stat records-card__stat--success">
                    <span class="records-card__stat-label">{{ t('ecommerce.monthlySettlement.matchedRows') }}</span>
                    <strong class="records-card__stat-value">{{ record.matchedRows ?? 0 }}</strong>
                  </div>
                  <div class="records-card__stat records-card__stat--warning">
                    <span class="records-card__stat-label">{{ t('ecommerce.monthlySettlement.unmatchedRows') }}</span>
                    <strong class="records-card__stat-value">{{ record.unmatchedRows ?? 0 }}</strong>
                  </div>
                  <div class="records-card__stat records-card__stat--danger">
                    <span class="records-card__stat-label">{{ t('ecommerce.monthlySettlement.gapOrderRows') }}</span>
                    <strong class="records-card__stat-value">{{ record.gapOrderRows ?? 0 }}</strong>
                  </div>
                </div>

                <div v-if="recordMatchRate(record) != null" class="records-card__progress">
                  <el-progress
                    :percentage="recordMatchRate(record)!"
                    :stroke-width="8"
                    :color="recordMatchRate(record)! >= 80 ? '#16a34a' : undefined"
                  />
                  <span class="records-card__progress-text">
                    {{ t('ecommerce.monthlySettlement.expressBillMatchRate', { rate: recordMatchRate(record) }) }}
                  </span>
                </div>

                <div class="records-card__actions" @click.stop>
                  <el-button
                    v-if="(record.totalRows ?? 0) > 0 && (record.unmatchedRows ?? 0) > 0"
                    size="small"
                    plain
                    @click="selectRecord(record)"
                  >
                    {{ t('ecommerce.monthlySettlement.expressBillRecordsViewUnmatched') }}
                  </el-button>
                  <el-button
                    v-if="canOpenRecordManual(record)"
                    size="small"
                    type="warning"
                    plain
                    @click="openBillManual(record)"
                  >
                    {{ t('ecommerce.monthlySettlement.viewManual') }}
                  </el-button>
                </div>
              </article>
            </div>
          </section>
        </template>
      </main>

      <aside class="express-bill-workbench__preview">
        <h4 class="preview-title">
          {{
            centerView === 'records'
              ? t('ecommerce.monthlySettlement.expressBillRecordDetailTitle')
              : t('ecommerce.monthlySettlement.expressBillPreviewTitle')
          }}
        </h4>

        <template v-if="effectivePreviewStats">
          <div class="preview-body">
          <div class="preview-stats">
            <div class="preview-stat">
              <span class="preview-stat__label">{{ t('ecommerce.monthlySettlement.totalRows') }}</span>
              <strong class="preview-stat__value">{{ effectivePreviewStats.totalRows ?? 0 }}</strong>
            </div>
            <div class="preview-stat preview-stat--success">
              <span class="preview-stat__label">{{ t('ecommerce.monthlySettlement.matchedRows') }}</span>
              <strong class="preview-stat__value">{{ effectivePreviewStats.matchedRows ?? 0 }}</strong>
            </div>
            <div class="preview-stat preview-stat--warning">
              <span class="preview-stat__label">{{ t('ecommerce.monthlySettlement.unmatchedRows') }}</span>
              <strong class="preview-stat__value">{{ effectivePreviewStats.unmatchedRows ?? 0 }}</strong>
            </div>
            <div class="preview-stat preview-stat--danger">
              <span class="preview-stat__label">{{ t('ecommerce.monthlySettlement.gapOrderRows') }}</span>
              <strong class="preview-stat__value">{{ effectivePreviewStats.gapOrderRows ?? effectivePreviewStats.manualPendingRows ?? 0 }}</strong>
            </div>
          </div>

          <div v-if="effectiveMatchRate != null" class="preview-match-rate">
            {{ t('ecommerce.monthlySettlement.expressBillMatchRate', { rate: effectiveMatchRate }) }}
          </div>

          <div v-if="(effectivePreviewStats.gapOrderRows ?? effectivePreviewStats.manualPendingRows ?? 0) > 0" class="preview-actions">
            <el-button size="small" type="warning" plain @click="goManualFromPreview">
              {{ t('ecommerce.monthlySettlement.expressBillGoManual') }}
            </el-button>
          </div>

          <div class="preview-unmatched">
            <div class="preview-unmatched__title">{{ t('ecommerce.monthlySettlement.expressBillUnmatchedList') }}</div>
            <div v-loading="loadingUnmatched" class="preview-unmatched__list">
              <div v-for="line in unmatchedLines" :key="line.id ?? line.trackingNumber" class="preview-unmatched__item">
                <el-tag size="small" type="warning" effect="plain">{{ t('ecommerce.monthlySettlement.unmatchedRows') }}</el-tag>
                <span class="preview-unmatched__tracking">{{ line.trackingNumber || '—' }}</span>
              </div>
              <el-empty
                v-if="!loadingUnmatched && !unmatchedLines.length"
                :description="t('ecommerce.monthlySettlement.expressBillUnmatchedEmpty')"
                :image-size="56"
              />
            </div>
          </div>
          </div>
        </template>

        <el-empty
          v-else
          class="preview-empty"
          :description="t('ecommerce.monthlySettlement.expressBillPreviewEmpty')"
          :image-size="72"
        />
      </aside>
    </div>

    <div class="express-bill-workbench__footer">
      <el-button @click="visible = false">{{ t('ecommerce.common.cancel') }}</el-button>
      <el-button
        v-if="centerView === 'import'"
        type="primary"
        :loading="uploading"
        :disabled="!canUpload"
        @click="submitUpload"
      >
        {{ t('ecommerce.monthlySettlement.expressBillSubmit') }}
      </el-button>
      <el-button
        v-else-if="centerView === 'manual'"
        type="primary"
        :loading="savingManual"
        :disabled="!currentBillId || !manualLines.length"
        @click="saveManual"
      >
        {{ t('ecommerce.monthlySettlement.saveManualLines') }}
      </el-button>
    </div>
  </el-drawer>

  <SalesOrderDetailDrawer
    v-model="orderDetailVisible"
    :loading="orderDetailLoading"
    :order="orderDetail"
    :shop-icon-meta="orderDetailShopIconMeta"
    :shop-options="shopOptions"
    :express-options="expressStations"
    :link-sku-options="[]"
    :show-delete="false"
  />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type UploadFile, type UploadInstance } from 'element-plus'
import { Delete, UploadFilled } from '@element-plus/icons-vue'
import { fetchExpressStations, type EcExpressStation } from '@/api/ecommerce/express'
import { formatMoney } from '@/utils/formatMoney'
import { fetchShopOptions, type EcShop } from '@/api/ecommerce/shop'
import { fetchSalesOrder, type EcSalesOrder } from '@/api/ecommerce/salesOrder'
import ExpressStationAvatar from '@/components/ecommerce/ExpressStationAvatar.vue'
import SalesOrderDetailDrawer from './SalesOrderDetailDrawer.vue'
import { resolveShopIconMeta } from '@/utils/shopVisual'
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
  fetchUnmatchedExpressBillLines,
  EXPRESS_STATION_OTHER,
  prepareManualExpressBill,
  previewExpressBillColumns,
  saveManualExpressBillLines,
  uploadSettlementExpressBill,
  type ExpressBillImportResult,
  type ExpressBillLine,
  type ExpressBillRecord,
} from '@/api/ecommerce/monthlySettlement'
import { autoMatchColumnMapping } from '@/utils/importColumnMapping'
import { useEcSettingsStore } from '@/stores/ecSettings'

const props = defineProps<{
  modelValue: boolean
  initialMonth?: string
  lockMonth?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  imported: []
}>()

const { t } = useI18n()
const ecSettings = useEcSettingsStore()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const billMonth = ref('')
const expressStationId = ref<number | undefined>()
const includeLabelPrice = ref(true)
const expressStations = ref<EcExpressStation[]>([])
const shopOptions = ref<EcShop[]>([])
const centerView = ref<'import' | 'manual' | 'records'>('import')
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
const loadingUnmatched = ref(false)
const currentBillId = ref<number | null>(null)
const lastImportResult = ref<ExpressBillImportResult | null>(null)
const manualLines = ref<ExpressBillLine[]>([])
const unmatchedLines = ref<ExpressBillLine[]>([])
const records = ref<ExpressBillRecord[]>([])
const recordsModeFilter = ref<'ALL' | 'FILE' | 'MANUAL' | 'MIXED'>('ALL')
const selectedRecordId = ref<number | null>(null)

const orderDetailVisible = ref(false)
const orderDetailLoading = ref(false)
const orderDetailId = ref<number | null>(null)
const orderDetail = ref<EcSalesOrder | null>(null)

const shopOptionMap = computed(() => {
  const map = new Map<number, EcShop>()
  for (const shop of shopOptions.value) {
    map.set(shop.id, shop)
  }
  return map
})

const orderDetailShopIconMeta = computed(() => {
  const order = orderDetail.value
  if (!order) return resolveShopIconMeta()
  const shop = shopOptionMap.value.get(order.shopId)
  return resolveShopIconMeta(
    order.shopName ?? shop?.name,
    shop?.platformName ?? order.platformName,
    shop?.platformCode,
    shop?.avatarUrl,
  )
})

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

const manualFilledCount = computed(
  () => manualLines.value.filter((line) => isManualLineFilled(line)).length,
)

const manualEmptyFreightCount = computed(
  () => manualLines.value.length - manualFilledCount.value,
)

function isManualLineFilled(line: ExpressBillLine) {
  return line.freightAmount != null && Number(line.freightAmount) > 0
}

function stationRecordKey(record: ExpressBillRecord): number {
  return record.otherExpress ? EXPRESS_STATION_OTHER : (record.expressStationId ?? -1)
}

/** 与月结准备清单 buildExpressBillCards 一致：按快递公司聚合，优先取有账单行数的文件导入批次 */
const stationContextByStation = computed(() => {
  const grouped = new Map<number, ExpressBillRecord[]>()
  for (const record of records.value) {
    const key = stationRecordKey(record)
    const list = grouped.get(key) ?? []
    list.push(record)
    grouped.set(key, list)
  }
  const map = new Map<
    number,
    {
      primary: ExpressBillRecord | null
      latest: ExpressBillRecord | null
      gapOrderRows: number
      manualBill: ExpressBillRecord | null
    }
  >()
  for (const [key, stationRecords] of grouped) {
    const primary =
      stationRecords.find((r) => (r.totalRows ?? 0) > 0) ?? stationRecords[0] ?? null
    const latest = stationRecords[0] ?? null
    const gapOrderRows = stationRecords.reduce((max, r) => Math.max(max, r.gapOrderRows ?? 0), 0)
    const manualBill = stationRecords.find((r) => r.importMode === 'MANUAL') ?? null
    map.set(key, { primary, latest, gapOrderRows, manualBill })
  }
  return map
})

function activeStationKey(): number | null {
  if (expressStationId.value == null) return null
  return expressStationId.value === EXPRESS_STATION_OTHER ? EXPRESS_STATION_OTHER : expressStationId.value
}

function pickWorkingBillId(stationId: number): number | null {
  const ctx = stationContextByStation.value.get(stationId)
  if (!ctx) return null
  return ctx.manualBill?.id ?? ctx.primary?.id ?? ctx.latest?.id ?? null
}

function pickFileBillId(stationId: number): number | null {
  return stationContextByStation.value.get(stationId)?.primary?.id ?? null
}

const previewStats = computed(() => {
  const key = activeStationKey()
  if (key == null) return null
  const ctx = stationContextByStation.value.get(key)
  if (!ctx) return null

  if (
    lastImportResult.value &&
    currentBillId.value === lastImportResult.value.billId &&
    (lastImportResult.value.totalRows ?? 0) > 0
  ) {
    return {
      ...lastImportResult.value,
      gapOrderRows: ctx.gapOrderRows,
      manualPendingRows: ctx.gapOrderRows,
    }
  }

  const record = ctx.primary
  if (!record && ctx.gapOrderRows === 0) return null
  return {
    billId: record?.id ?? ctx.latest?.id ?? null,
    totalRows: record?.totalRows ?? 0,
    matchedRows: record?.matchedRows ?? 0,
    unmatchedRows: record?.unmatchedRows ?? 0,
    gapOrderRows: ctx.gapOrderRows,
    manualPendingRows: ctx.gapOrderRows,
  }
})

const selectedRecord = computed(() => {
  if (!selectedRecordId.value) return null
  return records.value.find((r) => r.id === selectedRecordId.value) ?? null
})

const recordPreviewStats = computed(() => {
  if (centerView.value !== 'records' || !selectedRecord.value) return null
  const record = selectedRecord.value
  return {
    billId: record.id,
    totalRows: record.totalRows ?? 0,
    matchedRows: record.matchedRows ?? 0,
    unmatchedRows: record.unmatchedRows ?? 0,
    gapOrderRows: record.gapOrderRows ?? 0,
    manualPendingRows: record.gapOrderRows ?? 0,
  }
})

const effectivePreviewStats = computed(() => recordPreviewStats.value ?? previewStats.value)

const stationFilteredRecords = computed(() => {
  const key = activeStationKey()
  if (key == null) return records.value
  return records.value.filter((r) => stationRecordKey(r) === key)
})

const displayedRecords = computed(() => {
  let list = stationFilteredRecords.value
  if (recordsModeFilter.value !== 'ALL') {
    list = list.filter((r) => r.importMode === recordsModeFilter.value)
  }
  return list
})

const recordsPanelSummary = computed(() => {
  const list = stationFilteredRecords.value
  return {
    batchCount: list.length,
    fileCount: list.filter((r) => r.importMode === 'FILE' || r.importMode === 'MIXED').length,
    manualCount: list.filter((r) => r.importMode === 'MANUAL').length,
    latestTime: list[0]?.createTime,
  }
})

const effectiveMatchRate = computed(() => {
  const stats = effectivePreviewStats.value
  if (!stats?.totalRows) return null
  const matched = stats.matchedRows ?? 0
  return Math.round((matched / stats.totalRows) * 100)
})

function syncIncludeLabelPriceDefault() {
  if (canIncludeLabelPrice.value) {
    includeLabelPrice.value = ecSettings.express.includeLabelPriceDefault
    return
  }
  includeLabelPrice.value = false
}

function stationMatchLabel(stationId: number) {
  const record = stationContextByStation.value.get(stationId)?.primary
  if (!record?.totalRows) {
    return t('ecommerce.monthlySettlement.expressBillStationNotImported')
  }
  return t('ecommerce.monthlySettlement.expressBillStationMatchRatio', {
    matched: record.matchedRows ?? 0,
    total: record.totalRows ?? 0,
  })
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
  if (expressStationId.value == null && expressStations.value[0]?.id != null) {
    expressStationId.value = expressStations.value[0].id
  }
}

async function loadShopOptions() {
  shopOptions.value = await fetchShopOptions()
}

async function openManualLineOrderDetail(row: ExpressBillLine) {
  if (!row.orderId) return
  const orderId = Number(row.orderId)
  if (!Number.isFinite(orderId) || orderId <= 0) return

  orderDetailId.value = orderId
  orderDetail.value = null
  orderDetailVisible.value = true
  orderDetailLoading.value = true
  try {
    orderDetail.value = await fetchSalesOrder(orderId)
  } catch {
    orderDetail.value = null
    ElMessage.error(t('ecommerce.salesOrder.loadDetailFailed'))
  } finally {
    orderDetailLoading.value = false
  }
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
    syncCurrentBillFromStation()
  } finally {
    loadingRecords.value = false
  }
}

async function loadUnmatchedLines() {
  let billId: number | null = null
  if (centerView.value === 'records' && selectedRecord.value && (selectedRecord.value.totalRows ?? 0) > 0) {
    billId = selectedRecord.value.id
  } else {
    const key = activeStationKey()
    billId = (key != null ? pickFileBillId(key) : null) ?? previewStats.value?.billId ?? null
  }
  if (!billId) {
    unmatchedLines.value = []
    return
  }
  loadingUnmatched.value = true
  try {
    unmatchedLines.value = await fetchUnmatchedExpressBillLines(billId)
  } finally {
    loadingUnmatched.value = false
  }
}

function syncCurrentBillFromStation() {
  if (currentBillId.value || expressStationId.value == null) return
  currentBillId.value = pickWorkingBillId(expressStationId.value)
}

async function selectStation(stationId: number) {
  expressStationId.value = stationId
  syncIncludeLabelPriceDefault()
  currentBillId.value = pickWorkingBillId(stationId)
  lastImportResult.value = null
  await loadColumnProfile()
  if (billFile.value) {
    await refreshDocColumns()
  }
  await loadUnmatchedLines()
}

function formatBillMonth(month?: string) {
  if (!month) return '—'
  const [year, mon] = month.split('-')
  if (!year || !mon) return month
  return `${year}年${mon}月`
}

async function onOpen() {
  await ecSettings.ensureLoaded()
  headerRow.value = ecSettings.express.headerRow
  dataStartRow.value = ecSettings.express.dataStartRow
  billMonth.value = props.initialMonth || billMonth.value
  centerView.value = 'import'
  billFile.value = null
  currentBillId.value = null
  lastImportResult.value = null
  selectedRecordId.value = null
  manualLines.value = []
  unmatchedLines.value = []
  docColumns.value = []
  profileId.value = undefined
  uploadRef.value?.clearFiles()
  await loadImportFields()
  await Promise.all([loadStations(), loadShopOptions()])
  if (expressStationId.value != null && expressStationId.value !== EXPRESS_STATION_OTHER) {
    await loadColumnProfile()
  } else {
    columnMapping.value = defaultMapping()
  }
  syncIncludeLabelPriceDefault()
  await loadRecords()
  await loadUnmatchedLines()
}

function onClosed() {
  billFile.value = null
  uploadRef.value?.clearFiles()
  orderDetailVisible.value = false
  orderDetailId.value = null
  orderDetail.value = null
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
  if (expressStationId.value == null || expressStationId.value === EXPRESS_STATION_OTHER) {
    ElMessage.warning(t('ecommerce.monthlySettlement.expressStationRequired'))
    return
  }
  if (!billFile.value) {
    ElMessage.warning(t('ecommerce.monthlySettlement.expressBillFileRequired'))
    return
  }
  uploading.value = true
  try {
    await refreshDocColumns()
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
    lastImportResult.value = res
    ElMessage.success(
      t('ecommerce.monthlySettlement.expressBillImported', {
        matched: res.matchedRows,
        unmatched: res.unmatchedRows,
        skipped: res.overwrittenRows ?? 0,
      }),
    )
    await loadRecords()
    await loadUnmatchedLines()
    if ((res.gapOrderRows ?? res.manualPendingRows ?? 0) > 0) {
      centerView.value = 'manual'
      await loadManualLines(res.billId)
    }
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
    lastImportResult.value = res
    await loadManualLines(res.billId)
    await loadRecords()
    await loadUnmatchedLines()
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
    lastImportResult.value = res
    ElMessage.success(
      t('ecommerce.monthlySettlement.manualSaved', { applied: res.manualAppliedRows ?? 0 }),
    )
    await loadManualLines(currentBillId.value)
    await loadRecords()
    await loadUnmatchedLines()
    emit('imported')
  } finally {
    savingManual.value = false
  }
}

async function openBillManual(record: ExpressBillRecord) {
  currentBillId.value = record.id
  selectedRecordId.value = record.id
  expressStationId.value = record.otherExpress ? EXPRESS_STATION_OTHER : record.expressStationId
  centerView.value = 'manual'
  await loadManualLines(record.id)
  await loadUnmatchedLines()
}

function syncRecordsSelection() {
  const list = displayedRecords.value
  if (!list.length) {
    selectedRecordId.value = null
    return
  }
  if (!selectedRecordId.value || !list.some((r) => r.id === selectedRecordId.value)) {
    selectedRecordId.value = list[0].id
  }
}

function selectRecord(record: ExpressBillRecord) {
  selectedRecordId.value = record.id
  void loadUnmatchedLines()
}

function stationForRecord(record: ExpressBillRecord) {
  if (record.otherExpress) return null
  return expressStations.value.find((s) => s.id === record.expressStationId) ?? null
}

function recordStationName(record: ExpressBillRecord) {
  if (record.otherExpress) {
    return t('ecommerce.monthlySettlement.otherExpressStation')
  }
  return record.expressStationName || stationForRecord(record)?.name || '—'
}

function recordFileLabel(record: ExpressBillRecord) {
  if (record.fileName) return record.fileName
  if (record.importMode === 'MANUAL') {
    return t('ecommerce.monthlySettlement.expressBillRecordsManualBatch')
  }
  return t('ecommerce.monthlySettlement.expressBillRecordsNoFile')
}

function formatRecordTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 19)
}

function recordMatchRate(record: ExpressBillRecord): number | null {
  if (!record.totalRows) return null
  return Math.round(((record.matchedRows ?? 0) / record.totalRows) * 100)
}

function canOpenRecordManual(record: ExpressBillRecord) {
  return record.importMode === 'MANUAL' || record.importMode === 'MIXED' || (record.gapOrderRows ?? 0) > 0
}

function importModeTagType(mode?: string): 'primary' | 'warning' | 'success' | 'info' {
  if (mode === 'FILE') return 'primary'
  if (mode === 'MANUAL') return 'warning'
  if (mode === 'MIXED') return 'success'
  return 'info'
}

function goManualFromPreview() {
  const stationId = expressStationId.value
  if (stationId == null) return
  const billId =
    (centerView.value === 'records' &&
    selectedRecord.value &&
    canOpenRecordManual(selectedRecord.value)
      ? selectedRecord.value.id
      : null) ?? pickWorkingBillId(stationId)
  centerView.value = 'manual'
  if (billId) {
    currentBillId.value = billId
    void loadManualLines(billId)
  }
}

function importModeLabel(mode?: string) {
  const map: Record<string, string> = {
    FILE: t('ecommerce.monthlySettlement.modeFile'),
    MANUAL: t('ecommerce.monthlySettlement.modeManual'),
    MIXED: t('ecommerce.monthlySettlement.modeMixed'),
  }
  return map[mode ?? ''] ?? mode ?? '—'
}

watch(
  () => props.initialMonth,
  (month) => {
    if (props.lockMonth && month) {
      billMonth.value = month
    }
  },
)

watch(billMonth, () => {
  if (visible.value) {
    currentBillId.value = null
    lastImportResult.value = null
    selectedRecordId.value = null
    void loadRecords().then(() => {
      syncRecordsSelection()
      return loadUnmatchedLines()
    })
  }
})

watch(centerView, (view) => {
  if (view === 'records') {
    syncRecordsSelection()
    void loadUnmatchedLines()
  }
})

watch([expressStationId, recordsModeFilter, records], () => {
  if (centerView.value === 'records') {
    syncRecordsSelection()
    void loadUnmatchedLines()
  }
})

watch(selectedRecordId, () => {
  if (visible.value && centerView.value === 'records') void loadUnmatchedLines()
})

watch(currentBillId, () => {
  if (visible.value && centerView.value !== 'records') void loadUnmatchedLines()
})
</script>

<style scoped lang="scss">
.express-bill-drawer {
  :deep(.el-drawer__body) {
    display: flex;
    flex-direction: column;
    padding-top: 8px;
    overflow: hidden;
  }
}

.express-bill-workbench__subtitle {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.express-bill-workbench {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 280px;
  grid-template-rows: minmax(0, 1fr);
  gap: 12px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.express-bill-workbench__sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow: hidden;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-light);
}

.sidebar-block__label {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.sidebar-block__month-readonly {
  padding: 8px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  background: #fff;
}

.sidebar-block--stations {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.station-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: min(420px, calc(100vh - 320px));
  overflow: auto;
}

.station-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: left;

  &.is-active {
    border-color: var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
  }

  &:hover {
    border-color: var(--el-border-color);
  }
}

.station-item__name {
  font-size: 13px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.station-item__stat {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.station-item__avatar-placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.sidebar-records-link {
  margin-top: 4px;
  padding: 0;
  border: none;
  background: none;
  color: var(--el-color-primary);
  font-size: 12px;
  cursor: pointer;
  text-align: left;

  &:hover {
    text-decoration: underline;
  }
}

.express-bill-workbench__main {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: #fff;
  overflow: auto;
}

.main-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.main-toolbar__options {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.main-toolbar__view-switch {
  :deep(.el-radio-button__inner) {
    min-width: 120px;
    height: 42px;
    padding: 0 22px;
    font-size: 15px;
    font-weight: 500;
    line-height: 40px;
  }
}

.express-bill-upload {
  width: 100%;

  :deep(.el-upload),
  :deep(.el-upload-dragger) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    padding: 18px 16px;
  }
}

.express-bill-upload__icon {
  font-size: 32px;
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

.mapping-block__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-weight: 600;
  font-size: 13px;
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

.manual-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.manual-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
}

.manual-panel__head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-light);
}

.manual-panel__title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.manual-panel__hint {
  margin: 0;
  max-width: 520px;
  font-size: 12px;
  line-height: 1.55;
  color: var(--el-text-color-secondary);
}

.manual-panel__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(88px, 1fr));
  gap: 8px;
  min-width: 280px;
}

.manual-stat {
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);

  &__label {
    display: block;
    font-size: 11px;
    color: var(--el-text-color-secondary);
  }

  &__value {
    display: block;
    margin-top: 4px;
    font-size: 20px;
    line-height: 1.2;
    font-weight: 700;
  }

  &--success .manual-stat__value {
    color: var(--el-color-success);
  }

  &--warning .manual-stat__value {
    color: var(--el-color-warning);
  }
}

.manual-panel__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 0 2px;
}

.manual-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.manual-panel__batch {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.manual-panel__empty {
  padding: 32px 16px;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
}

.manual-panel__empty-title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.manual-panel__empty-desc {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.records-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
}

.records-panel__head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-light);
}

.records-panel__title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.records-panel__hint {
  margin: 0;
  max-width: 520px;
  font-size: 12px;
  line-height: 1.55;
  color: var(--el-text-color-secondary);
}

.records-panel__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(88px, 1fr));
  gap: 8px;
  min-width: 280px;
}

.records-stat {
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);

  &__label {
    display: block;
    font-size: 11px;
    color: var(--el-text-color-secondary);
  }

  &__value {
    display: block;
    margin-top: 4px;
    font-size: 20px;
    line-height: 1.2;
    font-weight: 700;
  }

  &--primary .records-stat__value {
    color: var(--el-color-primary);
  }

  &--warning .records-stat__value {
    color: var(--el-color-warning);
  }
}

.records-panel__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.records-panel__latest {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.records-panel__empty {
  padding: 32px 16px;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
}

.records-panel__empty-title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.records-panel__empty-desc {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: calc(100vh - 380px);
  overflow: auto;
  padding-right: 2px;
}

.records-card {
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &.is-active {
    border-color: var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
    box-shadow: 0 0 0 1px var(--el-color-primary-light-7);
  }

  &:hover {
    border-color: var(--el-border-color);
    box-shadow: 0 1px 4px rgba(15, 23, 42, 0.06);
  }
}

.records-card__header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.records-card__identity {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.records-card__avatar-placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
  font-size: 14px;
  flex-shrink: 0;
}

.records-card__title-wrap {
  min-width: 0;
  flex: 1;
}

.records-card__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.records-card__station {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.records-card__file {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.records-card__meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}

.records-card__time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.records-card__batch-id {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  font-family: ui-monospace, monospace;
}

.records-card__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.records-card__stat {
  padding: 8px 10px;
  border-radius: 6px;
  background: var(--el-fill-color-light);

  &-label {
    display: block;
    font-size: 10px;
    color: var(--el-text-color-secondary);
  }

  &-value {
    display: block;
    margin-top: 2px;
    font-size: 16px;
    font-weight: 700;
    line-height: 1.2;
  }

  &--success .records-card__stat-value {
    color: var(--el-color-success);
  }

  &--warning .records-card__stat-value {
    color: var(--el-color-warning);
  }

  &--danger .records-card__stat-value {
    color: var(--el-color-danger);
  }
}

.records-card__progress {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;

  :deep(.el-progress) {
    flex: 1;
  }
}

.records-card__progress-text {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.records-card__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.manual-line-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: calc(100vh - 380px);
  overflow: auto;
  padding-right: 2px;
}

.manual-line-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: start;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: #fff;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &.is-filled {
    border-color: var(--el-color-success-light-5);
    background: var(--el-color-success-light-9);
  }

  &:hover {
    border-color: var(--el-border-color);
    box-shadow: 0 1px 4px rgba(15, 23, 42, 0.06);
  }
}

.manual-line-card__main {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px 20px;
  min-width: 0;
}

.manual-line-card__order {
  flex: 1;
  min-width: 220px;
}

.manual-line-card__shop {
  margin-bottom: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.manual-line-card__order-no {
  font-size: 15px;
  font-weight: 600;

  &.is-text {
    color: var(--el-text-color-primary);
    word-break: break-all;
  }
}

.manual-line-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  margin-top: 8px;
}

.manual-line-card__tag {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.manual-line-card__inputs {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}

.manual-input-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 140px;

  &__label {
    font-size: 12px;
    font-weight: 500;
    color: var(--el-text-color-secondary);
  }

  &--freight .manual-input-field__label {
    color: var(--el-color-primary);
    font-weight: 600;
  }
}

.manual-line-card__remove {
  margin-top: 2px;
}

.manual-num-input {
  width: 140px;

  &--freight {
    :deep(.el-input__wrapper) {
      box-shadow: 0 0 0 1px var(--el-color-primary-light-5) inset;
    }
  }
}

.manual-bill-id {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.readonly-cell {
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.manual-order-link {
  padding: 0;
  border: none;
  background: none;
  color: var(--el-color-primary);
  font-size: 13px;
  cursor: pointer;
  text-align: left;
  word-break: break-all;

  &:hover {
    text-decoration: underline;
  }
}

.express-bill-workbench__preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  height: 100%;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
  overflow: hidden;
}

.preview-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow: hidden;
}

.preview-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.preview-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.preview-stat {
  padding: 10px;
  border-radius: 8px;
  background: var(--el-fill-color-light);

  &__label {
    display: block;
    font-size: 11px;
    color: var(--el-text-color-secondary);
  }

  &__value {
    display: block;
    margin-top: 4px;
    font-size: 18px;
    line-height: 1.2;
  }

  &--success .preview-stat__value {
    color: var(--el-color-success);
  }

  &--warning .preview-stat__value {
    color: var(--el-color-warning);
  }

  &--danger .preview-stat__value {
    color: var(--el-color-danger);
  }
}

.preview-match-rate {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-success);
}

.preview-unmatched {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.preview-unmatched__title {
  flex-shrink: 0;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.preview-unmatched__list {
  flex: 1;
  min-height: 0;
  max-height: calc(100vh - 380px);
  overflow-y: auto;
  padding-right: 2px;
}

.preview-unmatched__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.preview-unmatched__tracking {
  font-size: 12px;
  color: var(--el-text-color-primary);
  word-break: break-all;
}

.preview-empty {
  flex: 1;
}

.label-price-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.express-bill-workbench__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

@media (max-width: 1100px) {
  .express-bill-workbench {
    grid-template-columns: 200px minmax(0, 1fr);
    grid-template-rows: auto auto;
  }

  .express-bill-workbench__preview {
    grid-column: 1 / -1;
    max-height: 280px;
  }
}
</style>
