<template>
  <div class="sales-order-panel">
    <div class="panel-toolbar">
      <el-input
        v-model="keyword"
        class="panel-search-input"
        :placeholder="t('ecommerce.salesOrder.searchPlaceholder')"
        clearable
        @keyup.enter="searchOrders"
        @clear="searchOrders"
      />
      <el-select v-model="statusFilter" clearable :placeholder="t('ecommerce.salesOrder.statusFilter')" style="width: 140px" @change="() => load(true)">
        <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-select v-model="shopFilter" clearable filterable :placeholder="t('ecommerce.salesOrder.shop')" style="width: 160px" @change="() => load(true)">
        <el-option v-for="s in shopOptions" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-button type="primary" @click="openCreate">{{ t('ecommerce.salesOrder.add') }}</el-button>
      <el-button @click="importVisible = true">{{ t('ecommerce.salesOrder.import') }}</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="records"
      stripe
      border
      size="small"
      row-key="id"
      class="sales-order-table"
      @row-click="(row: EcSalesOrder) => openDetail(row.id)"
    >
      <el-table-column prop="platformOrderNo" :label="t('ecommerce.salesOrder.platformOrderNo')" min-width="150" show-overflow-tooltip fixed>
        <template #default="{ row }">
          <span>{{ row.platformOrderNo || '—' }}</span>
          <el-tag v-if="(row.lineCount ?? 0) > 1" size="small" type="info" class="line-count-tag">
            {{ t('ecommerce.salesOrder.lineCountTag', { count: row.lineCount }) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="shopName" :label="t('ecommerce.salesOrder.shop')" width="120" show-overflow-tooltip />
      <el-table-column :label="t('ecommerce.salesOrder.status')" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.salesOrder.orderTime')" width="160">
        <template #default="{ row }">{{ formatDateTime(row.orderTime) }}</template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.salesOrder.receivedAmount')" width="100" align="right">
        <template #default="{ row }">{{ formatMoney(row.receivedAmount) }}</template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.salesOrder.profit')" width="90" align="right">
        <template #default="{ row }">{{ formatMoney(row.profitAmount) }}</template>
      </el-table-column>
      <el-table-column prop="expressStationName" :label="t('ecommerce.salesOrder.expressStation')" width="110" show-overflow-tooltip />
      <el-table-column prop="trackingNumber" :label="t('ecommerce.salesOrder.trackingNumber')" width="120" show-overflow-tooltip />
    </el-table>

    <TablePagination :page="page" :page-size="pageSize" :total="total" @update:page="onPageChange" @update:page-size="onSizeChange" />

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? t('ecommerce.salesOrder.editTitle') : t('ecommerce.salesOrder.createTitle')" width="1100px" destroy-on-close top="4vh">
      <el-form :model="form" label-width="108px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item :label="t('ecommerce.salesOrder.shop')" required>
              <el-select v-model="form.shopId" filterable style="width: 100%" @change="onShopChange">
                <el-option v-for="s in shopOptions" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('ecommerce.salesOrder.expressStation')">
              <el-select v-model="form.expressStationId" clearable filterable style="width: 100%">
                <el-option v-for="s in expressOptions" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('ecommerce.salesOrder.orderTime')" required>
              <el-date-picker v-model="form.orderTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('ecommerce.salesOrder.platformOrderNo')">
              <el-input v-model="form.platformOrderNo" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('ecommerce.salesOrder.receivedAmount')">
              <el-input-number v-model="form.receivedAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('ecommerce.salesOrder.trackingNumber')">
              <el-input v-model="form.trackingNumber" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="t('ecommerce.salesOrder.receiveAddress')">
              <el-input v-model="form.receiveAddress" type="textarea" :rows="2" @input="syncProvinceFromAddress" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('ecommerce.salesOrder.receiveProvince')">
              <el-input :model-value="form.receiveProvince || '—'" disabled />
              <p class="field-hint">{{ t('ecommerce.salesOrder.receiveProvinceHint') }}</p>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="line-header">
        <span>{{ t('ecommerce.salesOrder.lines') }}</span>
        <el-button size="small" @click="addLine">{{ t('ecommerce.salesOrder.addLine') }}</el-button>
      </div>
      <el-table :data="form.lines" border size="small" max-height="320">
        <el-table-column :label="t('ecommerce.salesOrder.linkSku')" min-width="220">
          <template #default="{ row }">
            <el-select
              v-model="row._pickerKey"
              filterable
              clearable
              :placeholder="t('ecommerce.salesOrder.linkSkuPlaceholder')"
              style="width: 100%"
              @change="(v: string | undefined) => onLinePickerChange(row, v)"
            >
              <el-option v-for="opt in linkSkuOptions" :key="opt.key" :label="opt.label" :value="opt.key" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.salesOrder.skuQuantity')" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.skuQuantity" :min="1" :precision="0" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.salesOrder.lineReceived')" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.lineReceivedAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.salesOrder.actions')" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" :disabled="form.lines.length <= 1" @click="form.lines.splice($index, 1)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" :title="t('ecommerce.salesOrder.detailTitle')" size="920px" destroy-on-close @open="loadDetail">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border size="small" class="detail-block">
            <el-descriptions-item :label="t('ecommerce.salesOrder.orderNo')">{{ detail.orderNo }}</el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.platformOrderNo')">
              <span
                v-if="detail.platformOrderNo"
                class="copyable-text"
                :title="t('ecommerce.salesOrder.copyPlatformOrderNo')"
                @click="copyPlatformOrderNo(detail.platformOrderNo)"
              >{{ detail.platformOrderNo }}</span>
              <span v-else>—</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.status')">{{ statusLabel(detail.status) }}</el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.platformStatus')">{{ detail.platformStatus || '—' }}</el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.orderTime')">{{ formatDateTime(detail.orderTime) }}</el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.payTime')">{{ formatDateTime(detail.payTime) }}</el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.shipTime')">{{ formatDateTime(detail.shipTime) }}</el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.completeTime')">{{ formatDateTime(detail.completeTime) }}</el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.shop')">{{ detail.shopName }}</el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.expressStation')">{{ detail.expressStationName || '—' }}</el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.receivedAmount')">
              <span class="detail-amount-primary">{{ formatMoney(detail.receivedAmount) }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.totalCost')">
              <span class="detail-amount-primary">{{ formatMoney(detail.totalCostAmount) }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.profit')">
              <span class="detail-amount-profit">{{ formatMoney(detail.profitAmount) }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.totalLoss')">
              <span :class="{ 'detail-amount-loss': hasPositiveLoss(detail.totalLossAmount) }">
                {{ formatMoney(detail.totalLossAmount) }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.trackingNumber')">{{ detail.trackingNumber || '—' }}</el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.receiveAddress')">{{ detail.receiveAddress || '—' }}</el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.receiveProvince')">{{ detail.receiveProvince || '—' }}</el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.sellerRemark')" :span="2">
              {{ detail.sellerRemark || '—' }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.estimatedFreight')">{{ formatMoney(detail.estimatedFreightAmount) }}</el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.salesOrder.actualFreight')">{{ formatMoney(detail.actualFreightAmount) }}</el-descriptions-item>
          </el-descriptions>

          <el-table :data="detail.lines ?? []" border size="small" class="line-table">
            <el-table-column prop="linkName" :label="t('ecommerce.salesOrder.linkName')" min-width="120" />
            <el-table-column prop="skuSpecName" :label="t('ecommerce.salesOrder.skuSpecName')" min-width="100" />
            <el-table-column prop="skuQuantity" :label="t('ecommerce.salesOrder.skuQuantity')" width="70" align="center" />
            <el-table-column :label="t('ecommerce.salesOrder.lineStatus')" width="90">
              <template #default="{ row }">{{ lineStatusLabel(row.status) }}</template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.salesOrder.lineReceived')" width="90" align="right">
              <template #default="{ row }">
                {{ isSingleSkuOrder(detail) ? formatMoney(row.lineReceivedAmount) : '—' }}
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.salesOrder.profit')" width="80" align="right">
              <template #default="{ row }">
                {{ isSingleSkuOrder(detail) ? formatMoney(row.profit) : '—' }}
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.salesOrder.loss')" width="80" align="right">
              <template #default="{ row }">{{ formatMoney(row.lossAmount) }}</template>
            </el-table-column>
          </el-table>
        </template>
      </div>
      <template v-if="detail" #footer>
        <el-button @click="detailVisible = false">{{ t('ecommerce.common.close') }}</el-button>
        <el-button type="danger" :loading="deletingDetail" @click="onDeleteDetailOrder">
          {{ t('ecommerce.salesOrder.delete') }}
        </el-button>
      </template>
    </el-drawer>

    <!-- 导入 -->
    <el-dialog
      v-model="importVisible"
      class="import-dialog"
      :title="t('ecommerce.salesOrder.importTitle')"
      width="900px"
      destroy-on-close
      @closed="resetImport"
      @open="onImportOpen"
    >
      <el-form class="import-form" label-width="76px" label-position="left">
        <el-form-item :label="t('ecommerce.salesOrder.shop')" required>
          <el-select v-model="importShopId" filterable style="width: 100%" @change="onImportShopChange">
            <el-option v-for="s in shopOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="importPlatformName" :label="t('ecommerce.salesOrder.platform')">
          <span>{{ importPlatformName }}</span>
          <span class="import-hint inline-hint">{{ t('ecommerce.salesOrder.platformMappingHint') }}</span>
        </el-form-item>
        <el-collapse v-if="importPlatformId" v-model="statusMappingCollapse" class="import-collapse">
          <el-collapse-item name="statusMapping">
            <template #title>
              <span>{{ t('sysImport.statusMapping') }}</span>
            </template>
            <ImportStatusMappingEditor
              :profile-id="importProfileId"
              @saved="onStatusMappingSaved"
            />
          </el-collapse-item>
        </el-collapse>
        <el-form-item :label="t('ecommerce.salesOrder.importFile')" class="import-file-item">
          <div class="import-file-block">
            <el-upload
              ref="importUploadRef"
              class="import-upload"
              drag
              :auto-upload="false"
              :show-file-list="false"
              accept=".csv,.txt,.xlsx,.xls"
              :disabled="!importShopId"
              @change="onImportUploadChange"
            >
              <el-icon class="import-upload__icon"><UploadFilled /></el-icon>
              <p v-if="importFile" class="import-upload__name">{{ importFile.name }}</p>
              <p v-else class="import-upload__trigger">{{ t('ecommerce.salesOrder.importUpload') }}</p>
            </el-upload>
            <p class="import-hint import-hint--block">{{ t('ecommerce.salesOrder.importHint') }}</p>
            <el-collapse v-if="parsedSpreadsheet" v-model="detectedColumnsCollapse" class="import-collapse import-collapse--nested">
              <el-collapse-item name="detectedColumns">
                <template #title>
                  <span>{{ t('ecommerce.salesOrder.detectedColumns', { count: parsedSpreadsheet.columns.length }) }}</span>
                </template>
                <p v-if="parsedSpreadsheet.columns.length" class="import-meta import-columns-list">
                  {{ parsedSpreadsheet.columns.join('、') }}
                </p>
                <p v-else class="import-meta">{{ t('ecommerce.salesOrder.importEmpty') }}</p>
              </el-collapse-item>
            </el-collapse>
          </div>
        </el-form-item>
        <el-form-item v-if="parsedSpreadsheet">
          <el-button type="primary" plain :disabled="!importPlatformId" @click="openMapping">{{ t('ecommerce.salesOrder.configMapping') }}</el-button>
          <span v-if="importProfileId" class="profile-tag">{{ importProfileLabel }}</span>
        </el-form-item>
      </el-form>
      <p v-if="importPreview" class="import-meta import-stats">
        {{ t('ecommerce.salesOrder.importStatsMatched', { count: importPreview.matchedRows }) }}
        <strong v-if="importPreview.unmatchedRows > 0" class="import-stats-unmatched">
          {{ t('ecommerce.salesOrder.importStatsUnmatched', { count: importPreview.unmatchedRows }) }}
        </strong>
        <span v-else>{{ t('ecommerce.salesOrder.importStatsUnmatched', { count: importPreview.unmatchedRows }) }}</span>
        <strong v-if="(importPreview.statusUnmatchedRows ?? 0) > 0" class="import-stats-unmatched">
          {{ t('ecommerce.salesOrder.importStatsStatusUnmatched', { count: importPreview.statusUnmatchedRows ?? 0 }) }}
        </strong>
        <span v-else>{{ t('ecommerce.salesOrder.importStatsStatusUnmatched', { count: importPreview.statusUnmatchedRows ?? 0 }) }}</span>
        {{ t('ecommerce.salesOrder.importStatsErrors', { count: importPreview.errorRows }) }}
      </p>
      <p v-if="importPreview?.unmatchedRows === 0 && (importPreview?.statusUnmatchedRows ?? 0) === 0 && importPreview?.batchId" class="import-meta import-all-matched">
        {{ t('ecommerce.salesOrder.importAllMatched') }}
      </p>
      <p v-if="importReviewRows.some((row) => row.sellerRemark)" class="import-meta import-hint--block">
        {{ t('ecommerce.salesOrder.sellerRemarkManualHint') }}
      </p>
      <el-table v-if="importReviewRows.length" :data="importReviewRows" border size="small" max-height="280">
        <el-table-column prop="rowNo" :label="'#'" width="50" />
        <el-table-column prop="platformOrderNo" :label="t('ecommerce.salesOrder.platformOrderNo')" min-width="120" show-overflow-tooltip />
        <el-table-column prop="linkName" :label="t('ecommerce.salesOrder.linkName')" min-width="120" />
        <el-table-column prop="skuSpecName" :label="t('ecommerce.salesOrder.skuSpecName')" min-width="100" />
        <el-table-column prop="sellerRemark" :label="t('ecommerce.salesOrder.sellerRemark')" min-width="100" show-overflow-tooltip />
        <el-table-column prop="platformLineStatus" :label="t('ecommerce.salesOrder.importPlatformStatus')" min-width="100" show-overflow-tooltip />
        <el-table-column :label="t('ecommerce.salesOrder.lineStatus')" width="130">
          <template #default="{ row }">
            <el-select
              v-if="row.statusMatchStatus === 'UNMATCHED'"
              v-model="row.lineStatus"
              size="small"
              :placeholder="t('ecommerce.salesOrder.importSelectLineStatus')"
              class="import-line-status-select"
            >
              <el-option
                v-for="opt in importLineStatusOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <span v-else>{{ importLineStatusLabel(row.lineStatus) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.salesOrder.manualCost')" width="130">
          <template #default="{ row }">
            <el-input-number
              v-if="row.matchStatus === 'UNMATCHED'"
              v-model="row.manualCostPrice"
              :min="0"
              :precision="2"
              :controls="false"
              size="small"
              class="manual-cost-input"
            />
            <span v-else class="import-meta">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" :label="t('ecommerce.salesOrder.error')" min-width="120" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button @click="importVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button
          type="primary"
          :disabled="!canFinishUpload"
          :loading="uploading"
          @click="onFinishUpload"
        >{{ t('ecommerce.salesOrder.finishUpload') }}</el-button>
        <el-button type="success" :disabled="!canCommitImport" :loading="importing" @click="onCommitImport">{{ t('ecommerce.salesOrder.commitImport') }}</el-button>
      </template>
    </el-dialog>

    <ImportMappingDialog
      v-model="mappingVisible"
      :platform-id="importPlatformId"
      :platform-name="importPlatformName"
      :shop-id="importShopId"
      :doc-columns="parsedSpreadsheet?.columns ?? []"
      :file-type="parsedSpreadsheet?.fileType"
      :header-row="parsedSpreadsheet?.headerRow"
      :data-start-row="parsedSpreadsheet?.dataStartRow"
      :initial-profile-id="importProfileId"
      @saved="onMappingSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type UploadFile, type UploadInstance } from 'element-plus'
import { Delete, UploadFilled } from '@element-plus/icons-vue'
import { fetchShopOptions, type EcShop } from '@/api/ecommerce/shop'
import { fetchExpressStations, type EcExpressStation } from '@/api/ecommerce/express'
import { fetchListingLink, fetchListingLinks } from '@/api/ecommerce/listingLink'
import {
  commitSalesOrderImport,
  createSalesOrder,
  deleteSalesOrder,
  fetchSalesOrder,
  fetchSalesOrders,
  uploadSalesOrderImport,
  updateSalesOrder,
  type EcSalesOrder,
  type EcSalesOrderImportPreview,
  type EcSalesOrderLineSaveItem,
} from '@/api/ecommerce/salesOrder'
import TablePagination from '@/components/TablePagination.vue'
import ImportMappingDialog from '@/components/ImportMappingDialog.vue'
import ImportStatusMappingEditor from '@/components/ImportStatusMappingEditor.vue'
import { detectSpreadsheetColumns, type ParsedSpreadsheet } from '@/utils/spreadsheetParse'
import { BIZ_SALES_ORDER, createImportProfile, defaultPlatformProfileName, fetchImportFields, fetchImportProfiles, type SysImportProfile } from '@/api/sys/import'
import { DEFAULT_STATUS_MAPPING } from '@/constants/importStatusMapping'
import { filterImportFields } from '@/constants/importFieldKeys'
import { buildColumnMappingForUpload } from '@/utils/importColumnMapping'
import { usePagination } from '@/composables/usePagination'
import { formatDateTime, todayDateString } from '@/utils/date'
import { normalizeLineStatus, type ImportLineStatus } from '@/constants/importStatusMapping'
import { parseProvinceFromAddress } from '@/utils/addressProvince'

const { t } = useI18n()

const keyword = ref('')
const statusFilter = ref<string | undefined>()
const shopFilter = ref<number | undefined>()
const shopOptions = ref<EcShop[]>([])
const expressOptions = ref<EcExpressStation[]>([])

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)

const detailVisible = ref(false)
const detailLoading = ref(false)
const deletingDetail = ref(false)
const detailId = ref<number | null>(null)
const detail = ref<EcSalesOrder | null>(null)

const importVisible = ref(false)
const importShopId = ref<number | undefined>()
const importPreview = ref<EcSalesOrderImportPreview | null>(null)
const importing = ref(false)
const uploading = ref(false)
const mappingVisible = ref(false)
const importFile = ref<File | null>(null)
const parsedSpreadsheet = ref<ParsedSpreadsheet | null>(null)
const importProfileId = ref<number | null>(null)
const importProfileName = ref<string | null>(null)
const importUploadRef = ref<UploadInstance>()
const statusMappingCollapse = ref<string[]>([])
const detectedColumnsCollapse = ref<string[]>([])

const importShop = computed(() => shopOptions.value.find((s) => s.id === importShopId.value))
const importPlatformId = computed(() => importShop.value?.platformId)
const importPlatformName = computed(() => importShop.value?.platformName ?? '')
const importProfileLabel = computed(() => {
  if (!importProfileName.value) return t('ecommerce.salesOrder.profileReady')
  return t('ecommerce.salesOrder.profileReadyNamed', { name: importProfileName.value })
})

const canFinishUpload = computed(
  () => !!importFile.value && !!importShopId.value && !!parsedSpreadsheet.value && !uploading.value,
)

const canCommitImport = computed(() => {
  const preview = importPreview.value
  if (!preview?.batchId) return false
  return preview.rows.some((row) => isImportRowCommittable(row))
})

function isImportRowCommittable(row: EcSalesOrderImportPreview['rows'][number]) {
  if (row.parseStatus !== 'OK') return false
  if (row.statusMatchStatus === 'UNMATCHED' && !row.lineStatus) return false
  if (row.matchStatus === 'UNMATCHED') {
    return (row.manualCostPrice ?? 0) > 0
  }
  return true
}

const importReviewRows = computed(() => {
  const preview = importPreview.value
  if (!preview?.rows) return []
  return preview.rows.filter(
    (row) =>
      row.parseStatus === 'OK'
      && (row.matchStatus === 'UNMATCHED' || row.statusMatchStatus === 'UNMATCHED'),
  )
})

const importLineStatusOptions = computed(() => {
  const values: ImportLineStatus[] = [
    'PAID', 'SHIPPED', 'COMPLETED', 'CANCELLED', 'PARTIAL_REFUND', 'REFUNDED', 'RETURNED',
  ]
  return values.map((value) => ({ value, label: importLineStatusLabel(value) }))
})

function importLineStatusLabel(status?: string | null) {
  const key = normalizeLineStatus(status)
  const map: Record<ImportLineStatus, string> = {
    PAID: t('ecommerce.salesOrder.importLineStatusPaid'),
    SHIPPED: t('ecommerce.salesOrder.importLineStatusShipped'),
    COMPLETED: t('ecommerce.salesOrder.importLineStatusCompleted'),
    CANCELLED: t('ecommerce.salesOrder.importLineStatusCancelled'),
    PARTIAL_REFUND: t('ecommerce.salesOrder.importLineStatusPartialRefund'),
    REFUNDED: t('ecommerce.salesOrder.importLineStatusRefunded'),
    RETURNED: t('ecommerce.salesOrder.importLineStatusReturned'),
  }
  return map[key]
}

type LineFormRow = EcSalesOrderLineSaveItem & { _pickerKey?: string }

const form = reactive<{
  shopId: number | undefined
  expressStationId: number | undefined
  orderTime: string
  platformOrderNo: string
  receivedAmount: number | undefined
  trackingNumber: string
  receiveAddress: string
  receiveProvince: string
  lines: LineFormRow[]
}>({
  shopId: undefined,
  expressStationId: undefined,
  orderTime: '',
  platformOrderNo: '',
  receivedAmount: undefined,
  trackingNumber: '',
  receiveAddress: '',
  receiveProvince: '',
  lines: [],
})

const linkSkuOptions = ref<{ key: string; label: string; linkName: string; skuSpecName: string; listingLinkSkuId: number }[]>([])

const statusOptions = computed(() => [
  { value: 'DRAFT', label: t('ecommerce.salesOrder.statusDraft') },
  { value: 'PAID', label: t('ecommerce.salesOrder.statusPaid') },
  { value: 'PARTIAL_SHIPPED', label: t('ecommerce.salesOrder.statusPartialShipped') },
  { value: 'SHIPPED', label: t('ecommerce.salesOrder.statusShipped') },
  { value: 'PARTIAL_REFUND', label: t('ecommerce.salesOrder.statusPartialRefund') },
  { value: 'COMPLETED', label: t('ecommerce.salesOrder.statusCompleted') },
  { value: 'REFUNDED', label: t('ecommerce.salesOrder.statusRefunded') },
  { value: 'CANCELLED', label: t('ecommerce.salesOrder.statusCancelled') },
])

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination(
  (p, ps) => fetchSalesOrders(keyword.value.trim() || undefined, statusFilter.value, shopFilter.value, { page: p, pageSize: ps }),
)

function searchOrders() {
  load(true)
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => load(true), 300)
})

function formatMoney(v?: number | null) {
  if (v == null) return '—'
  return `¥${Number(v).toFixed(2)}`
}

/** 多 SKU/多链接订单的行实收、利润为均摊值，详情中不展示 */
function isSingleSkuOrder(order?: EcSalesOrder | null) {
  const count = order?.lines?.length ?? order?.lineCount ?? 0
  return count === 1
}

function hasPositiveLoss(v?: number | null) {
  return v != null && Number(v) !== 0
}

function statusLabel(s?: string) {
  return statusOptions.value.find((o) => o.value === s)?.label ?? s ?? '—'
}

function lineStatusLabel(s?: string) {
  const map: Record<string, string> = {
    PAID: t('ecommerce.salesOrder.linePaid'),
    SHIPPED: t('ecommerce.salesOrder.lineShipped'),
    COMPLETED: t('ecommerce.salesOrder.lineCompleted'),
    CANCELLED: t('ecommerce.salesOrder.lineCancelled'),
    PARTIAL_REFUND: t('ecommerce.salesOrder.linePartialRefund'),
    REFUNDED: t('ecommerce.salesOrder.lineRefunded'),
    RETURNED: t('ecommerce.salesOrder.lineReturned'),
  }
  return map[s ?? ''] ?? s ?? '—'
}

function statusTagType(s?: string) {
  if (s === 'DRAFT') return 'info'
  if (s === 'PAID') return 'primary'
  if (s === 'PARTIAL_SHIPPED') return 'warning'
  if (s === 'SHIPPED') return 'warning'
  if (s === 'PARTIAL_REFUND') return 'danger'
  if (s === 'REFUNDED' || s === 'CANCELLED') return 'danger'
  if (s === 'COMPLETED') return 'success'
  return undefined
}

function emptyLine(): LineFormRow {
  return { skuQuantity: 1, linkName: '', skuSpecName: '', lineReceivedAmount: undefined }
}

function syncProvinceFromAddress() {
  form.receiveProvince = parseProvinceFromAddress(form.receiveAddress) ?? ''
}

function resetForm() {
  form.shopId = shopOptions.value[0]?.id
  form.expressStationId = undefined
  form.orderTime = `${todayDateString()} 00:00:00`
  form.platformOrderNo = ''
  form.receivedAmount = undefined
  form.trackingNumber = ''
  form.receiveAddress = ''
  form.receiveProvince = ''
  form.lines = [emptyLine()]
  if (form.shopId) loadLinkSkuOptions(form.shopId)
}

async function loadLinkSkuOptions(shopId: number) {
  linkSkuOptions.value = []
  const pageResult = await fetchListingLinks(undefined, shopId, undefined, { page: 1, pageSize: 100 })
  const opts: typeof linkSkuOptions.value = []
  for (const link of pageResult.records ?? []) {
    const d = await fetchListingLink(link.id)
    for (const sku of d.skus ?? []) {
      if (!sku.id) continue
      const key = `${d.name}|||${sku.skuName}`
      opts.push({
        key,
        label: `${d.name} · ${sku.skuName}`,
        linkName: d.name,
        skuSpecName: sku.skuName ?? '',
        listingLinkSkuId: sku.id,
      })
    }
  }
  linkSkuOptions.value = opts
}

function onShopChange(shopId: number) {
  if (shopId) loadLinkSkuOptions(shopId)
}

function onLinePickerChange(row: LineFormRow, key: string | undefined) {
  if (!key) return
  const opt = linkSkuOptions.value.find((o) => o.key === key)
  if (!opt) return
  row.listingLinkSkuId = opt.listingLinkSkuId
  row.linkName = opt.linkName
  row.skuSpecName = opt.skuSpecName
}

function addLine() {
  form.lines.push(emptyLine())
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

async function copyPlatformOrderNo(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(t('ecommerce.salesOrder.copyPlatformOrderNoSuccess'))
  } catch {
    ElMessage.error(t('ecommerce.salesOrder.copyFailed'))
  }
}

function openDetail(id: number) {
  detailId.value = id
  detailVisible.value = true
}

async function loadDetail() {
  if (!detailId.value) return
  detailLoading.value = true
  try {
    detail.value = await fetchSalesOrder(detailId.value)
  } finally {
    detailLoading.value = false
  }
}

async function onDeleteDetailOrder() {
  if (!detail.value?.id) return
  const label = detail.value.platformOrderNo || detail.value.orderNo
  await ElMessageBox.confirm(t('ecommerce.salesOrder.deleteConfirm', { orderNo: label }), { type: 'warning' })
  deletingDetail.value = true
  try {
    await deleteSalesOrder(detail.value.id)
    ElMessage.success(t('ecommerce.common.deleted'))
    detailVisible.value = false
    detail.value = null
    detailId.value = null
    await load()
  } finally {
    deletingDetail.value = false
  }
}

async function onSave() {
  if (!form.shopId) {
    ElMessage.warning(t('ecommerce.salesOrder.shopRequired'))
    return
  }
  if (!form.orderTime) {
    ElMessage.warning(t('ecommerce.salesOrder.orderTimeRequired'))
    return
  }
  const lines = form.lines.filter((l) => l.linkName?.trim() && l.skuSpecName?.trim())
  if (!lines.length) {
    ElMessage.warning(t('ecommerce.salesOrder.linesRequired'))
    return
  }
  saving.value = true
  try {
    const payload = {
      shopId: form.shopId,
      expressStationId: form.expressStationId ?? null,
      orderTime: form.orderTime,
      platformOrderNo: form.platformOrderNo || undefined,
      receivedAmount: form.receivedAmount ?? null,
      trackingNumber: form.trackingNumber || undefined,
      receiveAddress: form.receiveAddress || undefined,
      lines: lines.map((l, i) => ({
        listingLinkSkuId: l.listingLinkSkuId ?? null,
        linkName: l.linkName!.trim(),
        skuSpecName: l.skuSpecName!.trim(),
        skuQuantity: l.skuQuantity ?? 1,
        lineReceivedAmount: l.lineReceivedAmount ?? null,
        sortOrder: i,
      })),
    }
    if (editingId.value) {
      await updateSalesOrder(editingId.value, payload)
    } else {
      await createSalesOrder(payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function resetImport() {
  importPreview.value = null
  importFile.value = null
  parsedSpreadsheet.value = null
  importProfileId.value = null
  importProfileName.value = null
  statusMappingCollapse.value = []
  detectedColumnsCollapse.value = []
  importUploadRef.value?.clearFiles()
}

function onImportShopChange() {
  importProfileId.value = null
  importProfileName.value = null
  importPreview.value = null
  void loadImportProfileForPlatform()
}

async function onImportOpen() {
  if (importShopId.value) {
    await loadImportProfileForPlatform()
  }
}

async function loadImportProfileForPlatform() {
  if (!importPlatformId.value) {
    importProfileId.value = null
    importProfileName.value = null
    return
  }
  const profiles = await fetchImportProfiles(BIZ_SALES_ORDER, importPlatformId.value)
  const preferredName = defaultPlatformProfileName(importPlatformName.value || '')
  const preferred = profiles.find((p) => p.name === preferredName) ?? profiles[0]
  if (preferred?.id) {
    importProfileId.value = preferred.id
    importProfileName.value = preferred.name ?? null
  } else {
    importProfileId.value = null
    importProfileName.value = null
  }
}

function openMapping() {
  if (!importPlatformId.value) {
    ElMessage.warning(t('ecommerce.salesOrder.importShopRequired'))
    return
  }
  mappingVisible.value = true
}

async function onImportUploadChange(uploadFile: UploadFile) {
  const file = uploadFile.raw
  if (!file) return
  await handleImportFileSelected(file)
}

async function handleImportFileSelected(file: File) {
  if (!importShopId.value) {
    ElMessage.warning(t('ecommerce.salesOrder.importShopRequired'))
    importUploadRef.value?.clearFiles()
    return
  }
  try {
    parsedSpreadsheet.value = await detectSpreadsheetColumns(file)
    if (!parsedSpreadsheet.value.columns.length) {
      ElMessage.warning(t('ecommerce.salesOrder.importEmpty'))
      importUploadRef.value?.clearFiles()
      return
    }
    importFile.value = file
    importPreview.value = null
    detectedColumnsCollapse.value = []
  } catch {
    ElMessage.error(t('ecommerce.salesOrder.parseFailed'))
    importUploadRef.value?.clearFiles()
  }
}

function onMappingSaved(profile: SysImportProfile) {
  importProfileId.value = profile.id ?? null
  importProfileName.value = profile.name ?? defaultPlatformProfileName(importPlatformName.value || '')
}

function onStatusMappingSaved(profile: SysImportProfile) {
  importProfileId.value = profile.id ?? null
  importProfileName.value = profile.name ?? importProfileName.value
}

async function ensureImportProfile(): Promise<number> {
  if (importProfileId.value) {
    return importProfileId.value
  }
  await loadImportProfileForPlatform()
  if (importProfileId.value) {
    return importProfileId.value
  }
  if (!importPlatformId.value || !parsedSpreadsheet.value) {
    throw new Error('missing import context')
  }
  const fields = filterImportFields(await fetchImportFields(BIZ_SALES_ORDER))
  const columnMapping = buildColumnMappingForUpload(
    fields,
    parsedSpreadsheet.value.columns,
    importPlatformName.value,
  )
  if (!columnMapping.link_name?.trim()) {
    ElMessage.warning(t('ecommerce.salesOrder.importMappingRequired'))
    throw new Error('link_name not mapped')
  }
  const profile = await createImportProfile({
    name: defaultPlatformProfileName(importPlatformName.value || ''),
    bizType: BIZ_SALES_ORDER,
    platformId: importPlatformId.value,
    fileType: parsedSpreadsheet.value.fileType,
    headerRow: parsedSpreadsheet.value.headerRow,
    dataStartRow: parsedSpreadsheet.value.dataStartRow,
    columnMapping,
    valueMapping: { ...DEFAULT_STATUS_MAPPING },
  })
  importProfileId.value = profile.id ?? null
  importProfileName.value = profile.name ?? null
  return profile.id!
}

async function onFinishUpload() {
  if (!importFile.value || !importShopId.value || !parsedSpreadsheet.value) return
  uploading.value = true
  try {
    const profileId = await ensureImportProfile()
    importPreview.value = await uploadSalesOrderImport(
      importFile.value,
      importShopId.value,
      profileId,
    )
    ElMessage.success(t('ecommerce.salesOrder.uploadSuccess'))
  } finally {
    uploading.value = false
  }
}

async function onCommitImport() {
  if (!importPreview.value?.batchId) return
  const reviewRows = importReviewRows.value
  const missingCost = reviewRows.filter(
    (row) => row.matchStatus === 'UNMATCHED' && (!row.manualCostPrice || row.manualCostPrice <= 0),
  )
  if (missingCost.length) {
    ElMessage.warning(t('ecommerce.salesOrder.importUnmatchedCostRequired'))
    return
  }
  const missingStatus = reviewRows.filter(
    (row) => row.statusMatchStatus === 'UNMATCHED' && !row.lineStatus,
  )
  if (missingStatus.length) {
    ElMessage.warning(t('ecommerce.salesOrder.importUnmatchedStatusRequired'))
    return
  }
  importing.value = true
  try {
    const patches = reviewRows
      .filter((row) => row.id != null)
      .map((row) => ({
        rowId: row.id!,
        ...(row.matchStatus === 'UNMATCHED'
          ? { manualCostPrice: row.manualCostPrice ?? null }
          : {}),
        ...(row.statusMatchStatus === 'UNMATCHED'
          ? { lineStatus: row.lineStatus ?? null }
          : {}),
      }))
    await commitSalesOrderImport(
      importPreview.value.batchId,
      patches.length ? { items: patches } : undefined,
    )
    ElMessage.success(t('ecommerce.salesOrder.importSuccess'))
    importVisible.value = false
    resetImport()
    await load()
  } finally {
    importing.value = false
  }
}

onMounted(async () => {
  shopOptions.value = await fetchShopOptions()
  expressOptions.value = (await fetchExpressStations(undefined, { page: 1, pageSize: 100 })).records ?? []
  await load()
})

defineExpose({ load })
</script>

<style scoped lang="scss">
.panel-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.panel-search-input {
  width: 320px;
  flex-shrink: 0;
}

.line-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 12px 0 8px;
  font-weight: 600;
}

.detail-block {
  margin-bottom: 12px;
}

.detail-amount-primary {
  color: #fff;
  font-weight: 700;
}

.detail-amount-profit {
  color: var(--el-color-success);
  font-weight: 700;
}

.detail-amount-loss {
  color: var(--el-color-danger);
  font-weight: 700;
}

.line-table {
  margin-top: 8px;
}

.import-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);

  &--block {
    display: block;
    margin-top: 8px;
    line-height: 1.5;
  }
}

.import-dialog {
  :deep(.el-dialog__body) {
    padding-top: 8px;
    padding-left: 16px;
  }
}

.import-form {
  margin-left: 2px;

  :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  :deep(.el-form-item__label) {
    padding-right: 8px;
  }
}

.import-file-block {
  width: 100%;
}

.import-upload {
  width: 100%;

  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
    padding: 18px 16px;
  }
}

.import-upload__icon {
  font-size: 52px;
  color: var(--el-color-primary);
}

.import-upload__trigger,
.import-upload__name {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.4;
}

.import-upload__name {
  color: var(--el-color-primary);
  word-break: break-all;
}

.import-meta {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
}

.import-stats-unmatched {
  color: var(--el-color-danger);
  font-weight: 700;
}

.inline-hint {
  margin-left: 8px;
}

.profile-tag {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-color-success);
}

.manual-cost-input {
  width: 100%;
}

.field-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.import-collapse {
  margin-bottom: 12px;

  :deep(.el-collapse-item__header) {
    height: 36px;
    line-height: 36px;
    font-size: 13px;
    color: var(--el-text-color-regular);
    background: transparent;
    border-bottom: none;
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }

  :deep(.el-collapse-item__content) {
    padding-bottom: 4px;
  }
}

.import-collapse--nested {
  margin-top: 10px;
  margin-bottom: 0;
  width: 100%;

  :deep(.el-collapse) {
    width: 100%;
    border-top: none;
    border-bottom: none;
  }

  :deep(.el-collapse-item__header) {
    width: 100%;
    padding-left: 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
}

.import-columns-list {
  margin: 0;
  word-break: break-all;
}

.sales-order-table {
  :deep(.el-table__body tr) {
    cursor: pointer;
  }
}

.line-count-tag {
  margin-left: 6px;
  vertical-align: middle;
}

.copyable-text {
  color: var(--el-color-primary);
  cursor: pointer;
  word-break: break-all;

  &:hover {
    text-decoration: underline;
  }
}
</style>
