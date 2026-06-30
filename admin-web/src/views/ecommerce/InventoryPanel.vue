<template>
  <div class="inventory-center">
    <header class="inventory-center__header">
      <h2 class="inventory-center__title">{{ t('ecommerce.inventory.centerTitle') }}</h2>
      <p class="inventory-center__subtitle">{{ t('ecommerce.inventory.centerSubtitle') }}</p>
    </header>

    <div class="inventory-center__layout">
          <div class="inventory-center__left">
            <div class="inventory-center__filters">
              <el-select
                v-model="factoryId"
                clearable
                filterable
                :placeholder="t('ecommerce.inventory.factoryPlaceholder')"
                style="width: 160px"
              >
                <el-option v-for="f in factoryOptions" :key="f.id" :label="f.name" :value="f.id" />
              </el-select>
              <el-input
                v-model="keyword"
                :placeholder="t('ecommerce.inventory.searchPlaceholder')"
                clearable
                style="width: 280px"
              />
              <el-select
                v-model="statusFilter"
                clearable
                :placeholder="t('ecommerce.inventory.statusFilter')"
                style="width: 140px"
              >
                <el-option :label="t('ecommerce.home.inventoryNormal')" value="normal" />
                <el-option :label="t('ecommerce.home.inventoryLow')" value="low" />
                <el-option :label="t('ecommerce.home.inventoryZeroShort')" value="zero" />
                <el-option :label="t('ecommerce.home.inventorySlow')" value="slow" />
              </el-select>
              <el-select
                v-model="alertFilter"
                clearable
                :placeholder="t('ecommerce.inventory.alertFilter')"
                style="width: 140px"
              >
                <el-option :label="t('ecommerce.inventory.alerting')" value="alert" />
                <el-option :label="t('ecommerce.inventory.normal')" value="normal" />
              </el-select>
              <el-button @click="resetFilters">{{ t('ecommerce.inventory.resetFilter') }}</el-button>
            </div>

            <div class="inventory-center__stats">
              <div class="inventory-center__health">
                <InventoryHealthChart :items="statsItems" />
              </div>
              <div class="inventory-center__metrics">
                <div
                  v-for="metric in mainSummaryMetrics"
                  :key="metric.key"
                  class="inventory-metric-card"
                  :class="`is-${metric.tone}`"
                >
                  <div class="inventory-metric-card__icon" aria-hidden="true">
                    <el-icon><component :is="metric.icon" /></el-icon>
                  </div>
                  <div class="inventory-metric-card__body">
                    <div class="inventory-metric-card__label">{{ metric.label }}</div>
                    <AutoFitCnyAmount
                      v-if="metric.moneyValue != null"
                      :value="metric.moneyValue"
                      class="inventory-metric-card__value"
                    />
                    <AutoFitOneLineText
                      v-else
                      :text="metric.value"
                      :unit="metric.unit"
                      class="inventory-metric-card__value"
                    />
                    <div
                      v-if="metric.weekCompare"
                      class="inventory-metric-card__week"
                      :class="`is-${metric.weekCompare.tone}`"
                    >
                      {{ t('ecommerce.inventory.vsLastWeek') }}
                      <span class="inventory-metric-card__week-delta">
                        <span v-if="metric.weekCompare.direction === 'up'">↑</span>
                        <span v-else-if="metric.weekCompare.direction === 'down'">↓</span>
                        {{ metric.weekCompare.text }}
                      </span>
                    </div>
                    <div v-else class="inventory-metric-card__week is-muted">
                      {{ t('ecommerce.inventory.noWeekCompare') }}
                    </div>
                  </div>
                </div>
                <div class="inventory-metric-card inventory-metric-card--full is-purple">
                  <div class="inventory-metric-card__icon" aria-hidden="true">
                    <el-icon><Tickets /></el-icon>
                  </div>
                  <div class="inventory-metric-card__main">
                    <div class="inventory-metric-card__label-row">
                      <span class="inventory-metric-card__label">{{ inboundValueMetric.label }}</span>
                      <el-tooltip :content="t('ecommerce.inventory.metricInboundValueHint')" placement="top">
                        <el-icon class="inventory-metric-card__info"><InfoFilled /></el-icon>
                      </el-tooltip>
                    </div>
                    <AutoFitCnyAmount
                      :value="inboundValueMetric.moneyValue"
                      :max="28"
                      :min="18"
                      class="inventory-metric-card__value"
                    />
                  </div>
                  <div class="inventory-metric-card__week-side">
                    <span class="inventory-metric-card__week-side-label">{{ t('ecommerce.inventory.vsLastWeek') }}</span>
                    <div
                      v-if="inboundValueWeekCompare"
                      class="inventory-metric-card__week-side-value"
                      :class="`is-${inboundValueWeekCompare.tone}`"
                    >
                      <span v-if="inboundValueWeekCompare.direction === 'up'" class="inventory-metric-card__week-arrow">↑</span>
                      <span v-else-if="inboundValueWeekCompare.direction === 'down'" class="inventory-metric-card__week-arrow">↓</span>
                      <span>{{ inboundValueWeekCompare.text }}</span>
                    </div>
                    <div v-else class="inventory-metric-card__week-side-empty">
                      {{ t('ecommerce.inventory.noWeekCompare') }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="inventory-center__list-head">
              <div>
                <h3 class="inventory-center__list-title">{{ t('ecommerce.inventory.listTitle') }}</h3>
                <span class="inventory-center__list-count">
                  {{ t('ecommerce.inventory.listCount', { count: displayListCount }) }}
                </span>
              </div>
              <div class="inventory-center__list-actions">
                <el-button @click="toggleDisplayMode">{{ displayModeButtonLabel }}</el-button>
                <el-button @click="cycleListStockFilter">{{ listStockFilterButtonLabel }}</el-button>
                <el-button type="primary" @click="openCreate">{{ t('ecommerce.inventory.add') }}</el-button>
              </div>
            </div>

            <div v-loading="loading" class="inventory-card-grid">
              <div
                v-for="row in displayRecords"
                :key="row.listKey ?? row.id"
                class="inventory-card"
                :class="{ 'is-alert': row.alertActive }"
                @click="openDetail(row)"
              >
                <div class="inventory-card__head">
                  <div class="inventory-card__sku">
                    <span class="inventory-card__code">{{ row.skuCode }}</span>
                    <span class="inventory-card__name">
                      <template v-if="row.spuSkuCount && row.spuSkuCount > 1">
                        {{ t('ecommerce.inventory.spuSkuCount', { count: row.spuSkuCount }) }}
                      </template>
                      <template v-else>{{ row.productName || row.specName || '—' }}</template>
                    </span>
                  </div>
                  <el-tag v-if="row.alertActive" type="danger" size="small">{{ t('ecommerce.inventory.alerting') }}</el-tag>
                  <el-tag v-else type="success" size="small">{{ t('ecommerce.inventory.normal') }}</el-tag>
                </div>
                <div class="inventory-card__qty">{{ row.quantity ?? 0 }}</div>
                <div class="inventory-card__meta">
                  <span class="inventory-card__meta-item">{{ t('ecommerce.inventory.inTransit') }} {{ row.inTransitQty ?? 0 }}</span>
                  <span class="inventory-card__meta-sep">·</span>
                  <span class="inventory-card__value-wrap">
                    <span class="inventory-card__meta-item">{{ t('ecommerce.inventory.stockValue') }}</span>
                    <span class="inventory-card__value">
                      <CnyAmount :value="stockValue(row)" />
                    </span>
                  </span>
                </div>
                <div class="inventory-card__progress">
                  <div class="inventory-card__progress-bar">
                    <span
                      class="inventory-card__progress-fill"
                      :class="{ 'is-danger': row.alertActive }"
                      :style="{ width: `${stockLevelPct(row)}%` }"
                    />
                  </div>
                  <span class="inventory-card__progress-label">
                    {{ row.quantity ?? 0 }} / {{ row.alertThreshold ?? 0 }}
                  </span>
                </div>
                <div class="inventory-card__actions" @click.stop>
                  <el-button size="small" @click.stop="openDetail(row)">{{ t('ecommerce.inventory.detail') }}</el-button>
                  <el-button size="small" @click.stop="openAdjust(row)">{{ t('ecommerce.inventory.adjust') }}</el-button>
                  <el-button size="small" @click.stop="openLogs(row)">{{ t('ecommerce.inventory.logs') }}</el-button>
                </div>
              </div>
              <el-empty v-if="!loading && !displayRecords.length" :description="t('ecommerce.inventory.noData')" />
            </div>

            <TablePagination
              :page="page"
              :page-size="pageSize"
              :total="total"
              @update:page="onListPageChange"
              @update:page-size="onListSizeChange"
            />
          </div>

          <aside class="inventory-center__sidebar">
            <div class="inventory-sidebar-card is-green">
              <div class="inventory-sidebar-card__title-row">
                <el-icon class="inventory-sidebar-card__title-icon is-green"><Download /></el-icon>
                <h4>{{ t('ecommerce.inbound.quickInbound') }}</h4>
              </div>
              <el-form :model="quickInboundForm" label-position="top" size="large">
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
                <el-button type="success" size="large" :loading="quickInboundSaving" style="width: 100%" @click="onQuickInbound">
                  {{ t('ecommerce.inbound.confirmInbound') }}
                </el-button>
              </el-form>
            </div>

            <div class="inventory-sidebar-card is-blue">
              <div class="inventory-sidebar-card__title-row">
                <el-icon class="inventory-sidebar-card__title-icon is-blue"><Tickets /></el-icon>
                <h4>{{ t('ecommerce.inbound.orderEntry') }}</h4>
              </div>
              <p class="inventory-sidebar-card__hint">{{ t('ecommerce.inventory.inboundOrderHint') }}</p>
              <el-button type="primary" size="large" style="width: 100%" @click="inboundDrawerVisible = true">
                {{ t('ecommerce.inventory.manageInboundOrders') }}
              </el-button>
            </div>

            <div class="inventory-sidebar-card is-orange">
              <div class="inventory-sidebar-card__title-row">
                <el-icon class="inventory-sidebar-card__title-icon is-orange"><Upload /></el-icon>
                <h4>{{ t('ecommerce.inventory.outboundOrder') }}</h4>
              </div>
              <p class="inventory-sidebar-card__hint">{{ t('ecommerce.inventory.outboundOrderHint') }}</p>
              <el-button type="warning" size="large" style="width: 100%" @click="outboundDrawerVisible = true">
                {{ t('ecommerce.inventory.manageOutboundOrders') }}
              </el-button>
            </div>
          </aside>
        </div>

    <InventorySaveDialog
      v-model="saveDialogVisible"
      :inventory="saveTarget"
      @refreshed="loadInventories"
    />

    <InventoryAdjustDialog
      v-model="adjustVisible"
      :inventory="adjustTarget"
      @refreshed="loadInventories"
      @view-product="(id) => emit('viewProduct', id)"
    />

    <el-dialog
      v-model="logsVisible"
      :title="t('ecommerce.inventory.logsTitle')"
      width="720px"
      destroy-on-close
      class="inventory-logs-dialog"
    >
      <el-table v-loading="logsLoading" :data="logRecords" stripe border size="small" max-height="420">
        <el-table-column :label="t('ecommerce.inventory.changeType')" width="108">
          <template #default="{ row }">
            <el-tag :type="logChangeStyle(row.changeType).tagType" effect="light" size="small">
              {{ changeTypeLabel(row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.inventory.changeQty')" width="96" align="right">
          <template #default="{ row }">
            <span class="inventory-logs-dialog__qty" :style="{ color: logChangeStyle(row.changeType).color }">
              {{ row.changeQty }}
            </span>
          </template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.inventory.logBalance')" width="96" align="right">
          <template #default="{ row }">{{ row.balance }}</template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.inventory.logOrderNo')" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <button
              v-if="row.remarkParts && row.refId"
              type="button"
              class="inventory-logs-dialog__order-link"
              @click="openLogOrder(row)"
            >
              {{ row.remarkParts.orderNo }}
            </button>
            <span v-else-if="row.remarkParts">{{ row.remarkParts.orderNo }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.inventory.logTime')" width="170">
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
          <el-input-number v-model="quickInboundForm.quantity" :min="1" :step="1" controls-position="right" style="width: 100%" />
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

    <InboundOrderDrawer ref="inboundDrawerRef" v-model="inboundDrawerVisible" @refreshed="loadInventories" />
    <OutboundOrderDrawer ref="outboundDrawerRef" v-model="outboundDrawerVisible" @refreshed="loadInventories" />
    <StocktakeOrderDrawer v-model="stocktakeDrawerVisible" :factory-id="factoryId" @refreshed="loadInventories" />
    <InventoryDetailDrawer
      v-model="detailDrawerVisible"
      :inventory-id="detailInventoryId"
      :spu-items="detailSpuItems"
      @refreshed="onDetailRefreshed"
      @view-product="(id) => emit('viewProduct', id)"
      @view-inbound-order="onViewInboundOrder"
      @view-outbound-order="onViewOutboundOrder"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Box,
  Download,
  Goods,
  InfoFilled,
  Tickets,
  Upload,
  Wallet,
  Warning,
} from '@element-plus/icons-vue'
import {
  deleteInventory,
  fetchInventories,
  fetchInventoryInboundValueSummary,
  fetchInventoryLogs,
  fetchInventorySkuOptions,
  quickInbound,
  type EcInventory,
  type EcInventoryLog,
  type EcInventorySkuOption,
} from '@/api/ecommerce/inventory'
import { fetchFactoryOptions, type EcFactory } from '@/api/ecommerce/factory'
import InventoryHealthChart from '@/components/ecommerce/InventoryHealthChart.vue'
import AutoFitOneLineText from '@/components/AutoFitOneLineText.vue'
import AutoFitCnyAmount from '@/components/AutoFitCnyAmount.vue'
import CnyAmount from '@/components/CnyAmount.vue'
import TablePagination from '@/components/TablePagination.vue'
import { DEFAULT_PAGE_SIZE } from '@/api/pagination'
import {
  enrichInventoryLogs,
  getInventoryLogChangeStyle,
  parseInventoryLogRemark,
  type LogRemarkParts,
} from '@/utils/inventoryLogDisplay'
import { classifyInventory, groupInventoriesBySpu, inventorySpuGroupKey, type InventoryListRow } from '@/utils/inventoryStats'
import { useEcSettingsStore } from '@/stores/ecSettings'
import {
  buildInventoryWeekCompares,
  getPreviousWeekSnapshot,
  saveInventoryWeeklySnapshot,
} from '@/utils/inventoryWeeklySnapshot'
import { formatDateTime } from '@/utils/date'
import InboundOrderDrawer from './InboundOrderDrawer.vue'
import OutboundOrderDrawer from './OutboundOrderDrawer.vue'
import StocktakeOrderDrawer from './StocktakeOrderDrawer.vue'
import InventoryDetailDrawer from './InventoryDetailDrawer.vue'
import InventoryAdjustDialog from './InventoryAdjustDialog.vue'
import InventorySaveDialog from './InventorySaveDialog.vue'

const emit = defineEmits<{ viewProduct: [productId: number] }>()

const { t } = useI18n()
const ecSettings = useEcSettingsStore()

const inventoryClassificationOptions = computed(() => ({
  defaultAlertThreshold: ecSettings.inventory.defaultAlertThreshold,
  slowMovingDays: ecSettings.inventory.slowMovingDays,
  slowMovingFallbackDays: ecSettings.inventory.slowMovingFallbackDays,
}))

const statusFilter = ref<string | undefined>()
const alertFilter = ref<string | undefined>()
const displayMode = ref<'spu' | 'sku'>('spu')
const listStockFilter = ref<'inStock' | 'alert' | 'all'>('all')
const statsItems = ref<EcInventory[]>([])
const statsLoading = ref(false)
const totalInboundValue = ref<number | null>(null)
const weekCompares = ref<ReturnType<typeof buildInventoryWeekCompares>>({
  sku: null,
  qty: null,
  value: null,
  inbound: null,
  alert: null,
})

const saveDialogVisible = ref(false)
const saveTarget = ref<EcInventory | null>(null)
const logsLoading = ref(false)
const keyword = ref('')
const factoryId = ref<number | undefined>()
const factoryOptions = ref<EcFactory[]>([])

const page = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const total = ref(0)
const records = ref<EcInventory[]>([])
const extra = ref<Record<string, unknown> | undefined>()
const loading = ref(false)
const spuAllRows = ref<InventoryListRow[]>([])
const allSkuRowsCache = ref<EcInventory[]>([])

function inventoryFetchParams(pageNo: number, pageSizeNo: number) {
  return {
    keyword: keyword.value.trim() || undefined,
    alertOnly: listStockFilter.value === 'alert' || alertFilter.value === 'alert' ? true : undefined,
    factoryId: factoryId.value,
    pageQuery: { page: pageNo, pageSize: pageSizeNo },
    inStockOnly: listStockFilter.value === 'inStock',
  } as const
}

async function fetchAllMatchingInventories() {
  const all: EcInventory[] = []
  let p = 1
  const ps = 200
  while (true) {
    const params = inventoryFetchParams(p, ps)
    const result = await fetchInventories(
      params.keyword,
      params.alertOnly,
      params.factoryId,
      params.pageQuery,
      params.inStockOnly,
    )
    all.push(...result.records)
    if (result.records.length === 0 || all.length >= result.total) break
    p += 1
  }
  return all
}

function applyListClientFilters(rows: EcInventory[]) {
  let filtered = rows
  if (statusFilter.value) {
    filtered = filtered.filter(
      (row) => classifyInventory(row, inventoryClassificationOptions.value) === statusFilter.value,
    )
  }
  if (alertFilter.value === 'normal') {
    filtered = filtered.filter((row) => !row.alertActive)
  }
  return filtered
}

function applySpuGrouping() {
  spuAllRows.value = groupInventoriesBySpu(applyListClientFilters(allSkuRowsCache.value))
  total.value = spuAllRows.value.length
}

async function loadList(resetPage = false) {
  if (resetPage) {
    page.value = 1
  }
  loading.value = true
  try {
    if (displayMode.value === 'spu') {
      allSkuRowsCache.value = await fetchAllMatchingInventories()
      applySpuGrouping()
      return
    }

    const params = inventoryFetchParams(page.value, pageSize.value)
    const result = await fetchInventories(
      params.keyword,
      params.alertOnly,
      params.factoryId,
      params.pageQuery,
      params.inStockOnly,
    )
    records.value = result.records
    total.value = result.total
    page.value = result.page
    pageSize.value = result.pageSize
    extra.value = result.extra
    spuAllRows.value = []
    allSkuRowsCache.value = []
  } finally {
    loading.value = false
  }
}

function onListPageChange(value: number) {
  page.value = value
  if (displayMode.value === 'sku') {
    loadList()
  }
}

function onListSizeChange(value: number) {
  pageSize.value = value
  page.value = 1
  if (displayMode.value === 'sku') {
    loadList()
  }
}

const adjustVisible = ref(false)
const adjustTarget = ref<EcInventory | null>(null)
const logsVisible = ref(false)
const logsInventoryId = ref<number | null>(null)
const logsTarget = ref<EcInventory | null>(null)
const logPage = ref(1)
const logPageSize = ref(20)
const logTotal = ref(0)

interface InventoryLogRow extends EcInventoryLog {
  balance: number
  remarkParts: LogRemarkParts | null
}

const logRecords = ref<InventoryLogRow[]>([])
const quickInboundVisible = ref(false)
const quickInboundSaving = ref(false)
const skuOptionsLoading = ref(false)
const inboundDrawerVisible = ref(false)
const outboundDrawerVisible = ref(false)
const inboundDrawerRef = ref<InstanceType<typeof InboundOrderDrawer> | null>(null)
const outboundDrawerRef = ref<InstanceType<typeof OutboundOrderDrawer> | null>(null)
const stocktakeDrawerVisible = ref(false)
const detailDrawerVisible = ref(false)
const detailInventoryId = ref<number | null>(null)
const detailSpuItems = ref<EcInventory[] | null>(null)
const skuOptions = ref<EcInventorySkuOption[]>([])

const quickInboundForm = reactive({
  skuCode: '',
  quantity: 1,
  remark: '',
})

const selectedSkuOption = computed(() =>
  skuOptions.value.find((opt) => opt.skuCode === quickInboundForm.skuCode),
)

const displayRecords = computed((): InventoryListRow[] => {
  if (displayMode.value === 'spu') {
    const start = (page.value - 1) * pageSize.value
    return spuAllRows.value.slice(start, start + pageSize.value)
  }

  let rows = records.value
  if (statusFilter.value) {
    rows = rows.filter((row) => classifyInventory(row, inventoryClassificationOptions.value) === statusFilter.value)
  }
  if (alertFilter.value === 'normal') {
    rows = rows.filter((row) => !row.alertActive)
  }

  return rows.map((row) => ({ ...row, listKey: `sku:${row.id}` }))
})

const displayListCount = computed(() => total.value)

const displayModeButtonLabel = computed(() =>
  displayMode.value === 'spu'
    ? t('ecommerce.inventory.viewBySpu')
    : t('ecommerce.inventory.viewBySku'),
)

const listStockFilterButtonLabel = computed(() => {
  if (listStockFilter.value === 'inStock') return t('ecommerce.inventory.showInStockOnly')
  if (listStockFilter.value === 'alert') return t('ecommerce.inventory.showAlertOnly')
  return t('ecommerce.inventory.showAllStock')
})

function toggleDisplayMode() {
  displayMode.value = displayMode.value === 'spu' ? 'sku' : 'spu'
  loadList(true)
}

function cycleListStockFilter() {
  if (listStockFilter.value === 'inStock') {
    listStockFilter.value = 'alert'
  } else if (listStockFilter.value === 'alert') {
    listStockFilter.value = 'all'
  } else {
    listStockFilter.value = 'inStock'
  }
}

const mainSummaryMetrics = computed(() => {
  const totalQty = Number(extra.value?.totalQuantity ?? 0)
  const totalStockValue = Number(extra.value?.totalStockValue ?? 0)
  const alertCount = statsItems.value.filter((row) => row.alertActive).length
  const compares = weekCompares.value
  return [
    {
      key: 'sku',
      tone: 'blue',
      label: t('ecommerce.inventory.metricSkuCount'),
      value: String(total.value),
      unit: t('ecommerce.inventory.unitCount'),
      icon: markRaw(Goods),
      weekCompare: compares.sku,
    },
    {
      key: 'qty',
      tone: 'green',
      label: t('ecommerce.inventory.metricSalableQty'),
      value: String(totalQty),
      unit: t('ecommerce.inventory.unitPiece'),
      icon: markRaw(Box),
      weekCompare: compares.qty,
    },
    {
      key: 'value',
      tone: 'orange',
      label: t('ecommerce.inventory.metricStockValue'),
      moneyValue: totalStockValue,
      value: '',
      unit: '',
      icon: markRaw(Wallet),
      weekCompare: compares.value,
    },
    {
      key: 'alert',
      tone: 'red',
      label: t('ecommerce.inventory.metricAlertCount'),
      value: String(alertCount),
      unit: t('ecommerce.inventory.unitCount'),
      icon: markRaw(Warning),
      weekCompare: compares.alert,
    },
  ]
})

const inboundValueMetric = computed(() => ({
  label: factoryId.value
    ? t('ecommerce.inventory.metricInboundValueFactory')
    : t('ecommerce.inventory.metricInboundValue'),
  moneyValue: totalInboundValue.value ?? undefined,
}))

const inboundValueWeekCompare = computed(() => weekCompares.value.inbound ?? null)

function changeTypeLabel(type: string) {
  if (type === 'DEDUCT') return t('ecommerce.inventory.deduct')
  if (type === 'RECLAIM') return t('ecommerce.inventory.reclaim')
  if (type === 'INBOUND') return t('ecommerce.inbound.inbound')
  if (type === 'STOCKTAKE') return t('ecommerce.stocktake.stocktake')
  return type
}

function logChangeStyle(changeType: string) {
  return getInventoryLogChangeStyle(changeType)
}

function stockValue(row: EcInventory): number {
  return (row.quantity ?? 0) * (row.salePrice ?? 0)
}

function stockLevelPct(row: EcInventory) {
  const qty = row.quantity ?? 0
  const threshold = Math.max(row.alertThreshold ?? 0, 1)
  const max = Math.max(threshold * 2, qty, 1)
  return Math.min(100, Math.round((qty / max) * 100))
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

function openQuickInboundDialog() {
  resetQuickInboundForm()
  quickInboundVisible.value = true
  searchSkuOptions('')
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

async function loadInboundValueSummary() {
  try {
    const summary = await fetchInventoryInboundValueSummary(factoryId.value)
    const raw = summary.totalInboundValue
    totalInboundValue.value = raw == null ? null : Number(raw)
  } catch {
    totalInboundValue.value = null
  }
}

async function loadStatsItems() {
  statsLoading.value = true
  try {
    const result = await fetchInventories(undefined, false, factoryId.value, { page: 1, pageSize: 500 })
    await loadInboundValueSummary()
    statsItems.value = result.records
    const skuCount = result.total
    const totalQty = Number(result.extra?.totalQuantity ?? 0)
    const totalStockValue = Number(result.extra?.totalStockValue ?? 0)
    const alertCount = result.records.filter((row) => row.alertActive).length
    const inboundValue = totalInboundValue.value ?? 0

    const previous = getPreviousWeekSnapshot(factoryId.value)
    weekCompares.value = buildInventoryWeekCompares(
      { skuCount, totalQty, stockValue: totalStockValue, inboundValue, alertCount },
      previous,
    )

    saveInventoryWeeklySnapshot({
      factoryId: factoryId.value,
      skuCount,
      totalQty,
      stockValue: totalStockValue,
      inboundValue,
      alertCount,
    })
  } finally {
    statsLoading.value = false
  }
}

async function loadInventories() {
  await Promise.all([loadList(), loadStatsItems()])
}

function onFactoryFilterChange() {
  loadList(true)
  loadStatsItems()
  searchSkuOptions('')
}

function resetFilters() {
  keyword.value = ''
  factoryId.value = undefined
  statusFilter.value = undefined
  alertFilter.value = undefined
  onFactoryFilterChange()
}

let keywordTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (keywordTimer) clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => loadList(true), 300)
})

watch(factoryId, () => onFactoryFilterChange())

watch(alertFilter, () => loadList(true))

watch(listStockFilter, () => loadList(true))

watch(statusFilter, () => {
  if (displayMode.value === 'spu') {
    page.value = 1
    applySpuGrouping()
  }
})

async function openCreate() {
  saveTarget.value = null
  saveDialogVisible.value = true
}

function openEdit(row: EcInventory) {
  saveTarget.value = row
  saveDialogVisible.value = true
}

function openAdjust(row: EcInventory) {
  adjustTarget.value = row
  adjustVisible.value = true
}

function resolveSpuItems(row: InventoryListRow): EcInventory[] {
  const key = inventorySpuGroupKey(row)
  const source = allSkuRowsCache.value.length ? allSkuRowsCache.value : records.value
  const items = source.filter((item) => inventorySpuGroupKey(item) === key)
  return items.length ? items : [row]
}

function openDetail(row: EcInventory) {
  detailInventoryId.value = row.id
  detailSpuItems.value = displayMode.value === 'spu' ? resolveSpuItems(row as InventoryListRow) : null
  detailDrawerVisible.value = true
}

async function onDetailRefreshed() {
  if (displayMode.value === 'spu') {
    const key = detailSpuItems.value?.[0] ? inventorySpuGroupKey(detailSpuItems.value[0]) : null
    await loadList()
    if (key) {
      detailSpuItems.value = allSkuRowsCache.value.filter((item) => inventorySpuGroupKey(item) === key)
    }
  } else {
    await loadList()
  }
  await loadStatsItems()
}

async function onViewInboundOrder(orderId: number) {
  inboundDrawerVisible.value = true
  await nextTick()
  await inboundDrawerRef.value?.openOrderById(orderId)
}

async function onViewOutboundOrder(orderId: number) {
  outboundDrawerVisible.value = true
  await nextTick()
  await outboundDrawerRef.value?.openOrderById(orderId)
}

async function openLogs(row: EcInventory) {
  logsInventoryId.value = row.id
  logsTarget.value = row
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
    logTotal.value = result.total

    const currentQty = logsTarget.value?.quantity ?? 0
    const prefixSize = Math.min(logPage.value * logPageSize.value, result.total)
    const balanceMap = new Map<number, number>()

    if (prefixSize > 0) {
      const prefixResult = await fetchInventoryLogs(logsInventoryId.value, {
        page: 1,
        pageSize: prefixSize,
      })
      enrichInventoryLogs(prefixResult.records, currentQty).forEach((log) => {
        balanceMap.set(log.id, log.balance)
      })
    }

    logRecords.value = result.records.map((log) => ({
      ...log,
      balance: balanceMap.get(log.id) ?? 0,
      remarkParts: parseInventoryLogRemark(log.remark, log.refType),
    }))
  } finally {
    logsLoading.value = false
  }
}

function openLogOrder(row: EcInventoryLog) {
  if (!row.refId) return
  if (row.refType === 'INBOUND_ORDER') {
    void onViewInboundOrder(row.refId)
    return
  }
  if (row.refType === 'OUTBOUND_ORDER') {
    void onViewOutboundOrder(row.refId)
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
  await ElMessageBox.confirm(t('ecommerce.inventory.deleteConfirm', { sku: row.skuCode }), { type: 'warning' })
  await deleteInventory(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  await loadInventories()
}

onMounted(async () => {
  factoryOptions.value = await fetchFactoryOptions('PRODUCTION')
  await loadInventories()
  await searchSkuOptions('')
})

defineExpose({ loadInventories })
</script>

<style scoped lang="scss">
.inventory-center__header {
  margin-bottom: 16px;
}

.inventory-center__title {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: #111827;
}

.inventory-center__subtitle {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

.inventory-center__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.inventory-center__layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
  align-items: start;
}

.inventory-center__left {
  min-width: 0;
}

.inventory-center__stats {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(0, 1.2fr);
  gap: 14px;
  margin-bottom: 18px;
}

.inventory-center__metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.inventory-metric-card--full {
  grid-column: 1 / -1;
  align-items: stretch;
  min-height: 108px;
  padding-right: 22px;

  .inventory-metric-card__main {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  .inventory-metric-card__value {
    font-size: 32px;
  }
}

.inventory-metric-card__week-side {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 8px;
  min-width: 148px;
  padding-left: 24px;
  margin-left: 12px;
  border-left: 1px solid rgb(139 92 246 / 18%);
}

.inventory-metric-card__week-side-label {
  font-size: 13px;
  font-weight: 700;
  color: #6b7280;
  letter-spacing: 0.02em;
}

.inventory-metric-card__week-side-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  font-size: 30px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.02em;

  &.is-good {
    color: #16a34a;
  }

  &.is-bad {
    color: #dc2626;
  }

  &.is-muted {
    color: #6b7280;
  }
}

.inventory-metric-card__week-arrow {
  font-size: 24px;
  line-height: 1;
}

.inventory-metric-card__week-side-empty {
  font-size: 14px;
  font-weight: 600;
  color: #9ca3af;
  text-align: right;
  line-height: 1.4;
}

.inventory-metric-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 96px;
  padding: 18px 16px;
  border-radius: 14px;
  border: 1px solid transparent;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);

  &.is-blue {
    background: #eff6ff;
    border-color: #bfdbfe;

    .inventory-metric-card__icon {
      background: #3b82f6;
    }

    .inventory-metric-card__value {
      color: #2563eb;
    }
  }

  &.is-green {
    background: #f0fdf4;
    border-color: #bbf7d0;

    .inventory-metric-card__icon {
      background: #22c55e;
    }

    .inventory-metric-card__value {
      color: #16a34a;
    }
  }

  &.is-orange {
    background: #fffbeb;
    border-color: #fde68a;

    .inventory-metric-card__icon {
      background: #f59e0b;
    }

    .inventory-metric-card__value {
      color: #d97706;
    }
  }

  &.is-red {
    background: #fef2f2;
    border-color: #fecaca;

    .inventory-metric-card__icon {
      background: #ef4444;
    }

    .inventory-metric-card__value {
      color: #dc2626;
    }
  }

  &.is-purple {
    background: #f5f3ff;
    border-color: #ddd6fe;

    .inventory-metric-card__icon {
      background: #8b5cf6;
    }

    .inventory-metric-card__value {
      color: #7c3aed;
    }
  }
}

.inventory-metric-card__icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  color: #fff;
  font-size: 26px;
}

.inventory-metric-card__body {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.inventory-metric-card__label {
  font-size: 13px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 6px;
  line-height: 1.3;
}

.inventory-metric-card__label-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  min-width: 0;

  .inventory-metric-card__label {
    margin-bottom: 0;
  }
}

.inventory-metric-card__info {
  flex-shrink: 0;
  font-size: 14px;
  color: #9ca3af;
  cursor: help;
}

.inventory-metric-card__value {
  font-weight: 700;
  line-height: 1.15;
}

.inventory-metric-card__unit {
  margin-left: 4px;
  font-size: 15px;
  font-weight: 600;
}

.inventory-metric-card__week {
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;

  &.is-good {
    color: #16a34a;
  }

  &.is-bad {
    color: #dc2626;
  }

  &.is-muted {
    color: #9ca3af;
  }
}

.inventory-metric-card__week-delta {
  margin-left: 4px;
  font-weight: 600;
}

.inventory-center__sidebar {
  position: sticky;
  top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.inventory-sidebar-card {
  padding: 16px;
  border: 1px solid #e8ecf2;
  border-radius: 12px;
  background: #fff;

  &.is-green {
    background: #f0fdf4;
    border-color: #bbf7d0;
  }

  &.is-blue {
    background: #eff6ff;
    border-color: #bfdbfe;
  }

  &.is-orange {
    background: #fffbeb;
    border-color: #fde68a;
  }

  h4 {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
  }
}

.inventory-sidebar-card__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.inventory-sidebar-card__title-icon {
  font-size: 18px;

  &.is-green {
    color: #16a34a;
  }

  &.is-blue {
    color: #2563eb;
  }

  &.is-orange {
    color: #ea580c;
  }
}

.inventory-sidebar-card__hint {
  margin: 0 0 12px;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
}

.inventory-center__list-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.inventory-center__list-title {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 700;
  color: #111827;
}

.inventory-center__list-count {
  font-size: 13px;
  color: #6b7280;
}

.inventory-center__list-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.inventory-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.inventory-card {
  padding: 16px;
  border: 1px solid #e8ecf2;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    border-color: #bfdbfe;
    box-shadow: 0 4px 14px rgb(37 99 235 / 8%);
  }

  &.is-alert {
    border-color: #fecaca;
    background: #fffbfb;
  }
}

.inventory-card__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
}

.inventory-card__sku {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.inventory-card__code {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.inventory-card__name {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inventory-card__qty {
  font-size: 32px;
  font-weight: 700;
  color: #111827;
  line-height: 1.1;
  margin-bottom: 8px;
}

.inventory-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px;
  font-size: 12px;
  margin-bottom: 10px;
}

.inventory-card__meta-item,
.inventory-card__meta-sep {
  color: #6b7280;
}

.inventory-card__value-wrap {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
}

.inventory-card__value {
  font-size: 16px;
  font-weight: 700;
  color: #991b1b;
  line-height: 1.2;
}

.inventory-card__progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.inventory-card__progress-bar {
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: #eef2f7;
  overflow: hidden;
}

.inventory-card__progress-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: #2563eb;

  &.is-danger {
    background: #dc2626;
  }
}

.inventory-card__progress-label {
  font-size: 11px;
  color: #9ca3af;
  white-space: nowrap;
}

.inventory-card__actions {
  display: flex;
  gap: 6px;
}

.inventory-logs-dialog {
  :deep(.el-table th.el-table__cell) {
    font-weight: 700;
    color: #111827;
  }
}

.inventory-logs-dialog__qty {
  font-weight: 700;
}

.inventory-logs-dialog__order-link {
  padding: 0;
  border: none;
  background: none;
  color: #1e3a8a;
  font-weight: 600;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

@media (max-width: 1200px) {
  .inventory-center__layout {
    grid-template-columns: 1fr;
  }

  .inventory-center__stats {
    grid-template-columns: 1fr;
  }

  .inventory-center__sidebar {
    position: static;
  }
}
</style>
