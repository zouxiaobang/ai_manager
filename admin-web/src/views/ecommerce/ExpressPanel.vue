<template>
  <div class="express-panel">
    <div class="panel-toolbar">
      <el-input
        v-model="keyword"
        :placeholder="t('ecommerce.express.searchPlaceholder')"
        clearable
        style="width: 320px"
      />
      <el-button type="primary" @click="openCreate">{{ t('ecommerce.express.add') }}</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="records"
      stripe
      border
      class="express-station-table"
      :row-class-name="() => 'express-station-row'"
      @cell-click="onStationCellClick"
    >
      <el-table-column prop="name" :label="t('ecommerce.express.name')" min-width="140" />
      <el-table-column prop="contact" :label="t('ecommerce.express.contact')" width="140" />
      <el-table-column prop="address" :label="t('ecommerce.express.address')" min-width="220" show-overflow-tooltip />
      <el-table-column :label="t('ecommerce.express.isDefault')" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault" type="success" size="small">{{ t('ecommerce.express.defaultYes') }}</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.express.updatedAt')" width="170">
        <template #default="{ row }">{{ formatDate(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column
        :label="t('ecommerce.express.actions')"
        width="88"
        fixed="right"
        align="center"
        :class-name="TABLE_ACTIONS_CELL_CLASS"
      >
        <template #default="{ row }">
          <div class="table-actions-cell-inner" @click.stop>
            <el-button link type="primary" :title="t('ecommerce.express.edit')" @click.stop="openEdit(row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button link type="danger" :title="t('ecommerce.express.delete')" @click.stop="onDelete(row)">
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
      v-model="viewVisible"
      :title="viewDetail?.name || t('ecommerce.express.detailTitle')"
      width="960px"
      destroy-on-close
      top="5vh"
      class="express-detail-dialog"
    >
      <div v-loading="viewLoading" class="express-detail-body">
        <template v-if="viewDetail">
          <el-descriptions :column="2" border size="small" class="express-detail-info">
            <el-descriptions-item :label="t('ecommerce.express.name')">
              {{ viewDetail.name }}
              <el-tag v-if="viewDetail.isDefault" type="success" size="small" style="margin-left: 8px">
                {{ t('ecommerce.express.defaultYes') }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.express.contact')">
              {{ viewDetail.contact || '—' }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.express.address')" :span="2">
              {{ viewDetail.address || '—' }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.express.labelPrice')">
              {{ viewDetail.labelPrice != null ? formatPrice(viewDetail.labelPrice) : '—' }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.express.nameAliases')" :span="2">
              <template v-if="viewDetail.nameAliases?.length">
                <el-tag
                  v-for="alias in viewDetail.nameAliases"
                  :key="alias"
                  size="small"
                  style="margin-right: 6px; margin-bottom: 4px"
                >
                  {{ alias }}
                </el-tag>
              </template>
              <span v-else>—</span>
            </el-descriptions-item>
          </el-descriptions>

          <el-collapse v-model="noticeCollapseNames" class="express-detail-notices">
            <el-collapse-item name="notices">
              <template #title>
                <span>{{ t('ecommerce.express.noticesSection') }}</span>
                <el-tag v-if="viewNotices.length" size="small" type="info" style="margin-left: 8px">
                  {{ viewNotices.length }}
                </el-tag>
              </template>
              <ul v-if="viewNotices.length" class="notice-list">
                <li
                  v-for="item in viewNotices"
                  :key="item.id"
                  :class="{ 'text-red': item.highlightRed }"
                >
                  {{ item.content }}
                </li>
              </ul>
              <el-empty v-else :description="t('ecommerce.express.noNotices')" :image-size="64" />
            </el-collapse-item>
          </el-collapse>

          <div class="express-detail-prices">
            <div class="express-detail-prices__title">{{ t('ecommerce.express.priceList') }}</div>
            <el-table
              v-if="viewPrices.length"
              :data="viewPrices"
              stripe
              border
              size="small"
              max-height="420"
              class="price-table"
            >
              <el-table-column prop="provinceName" :label="t('ecommerce.express.province')" width="100" fixed />
              <el-table-column
                v-for="col in priceColumns"
                :key="col.key"
                :label="col.label"
                width="88"
                align="right"
              >
                <template #default="{ row }">{{ formatPrice(row[col.key]) }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-else :description="t('ecommerce.express.noPrices')" :image-size="64" />
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="viewVisible = false">{{ t('ecommerce.common.close') }}</el-button>
        <el-button type="primary" @click="openEditFromView">{{ t('ecommerce.express.edit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('ecommerce.express.editTitle') : t('ecommerce.express.createTitle')"
      width="920px"
      destroy-on-close
      top="5vh"
    >
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="t('ecommerce.express.tabBasic')" name="basic">
          <el-form :model="form" label-width="96px">
            <el-form-item :label="t('ecommerce.express.name')" required>
              <el-input v-model="form.name" />
            </el-form-item>
            <el-form-item :label="t('ecommerce.express.contact')">
              <el-input v-model="form.contact" />
            </el-form-item>
            <el-form-item :label="t('ecommerce.express.address')">
              <el-input v-model="form.address" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item :label="t('ecommerce.express.labelPrice')">
              <el-input-number
                v-model="form.labelPrice"
                :min="0"
                :precision="2"
                :step="0.1"
                controls-position="right"
                style="width: 200px"
              />
            </el-form-item>
            <el-form-item :label="t('ecommerce.express.isDefault')">
              <el-switch v-model="form.isDefault" />
            </el-form-item>
            <el-form-item :label="t('ecommerce.express.nameAliases')">
              <el-select
                v-model="form.nameAliases"
                multiple
                filterable
                allow-create
                default-first-option
                :reserve-keyword="false"
                :placeholder="t('ecommerce.express.nameAliasesPlaceholder')"
                style="width: 100%"
              />
              <div class="form-hint">{{ t('ecommerce.express.nameAliasesHint') }}</div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane :label="t('ecommerce.express.tabPrice')" name="price" :disabled="!editingId">
          <div v-if="!editingId" class="tab-hint">{{ t('ecommerce.express.saveStationFirst') }}</div>
          <template v-else>
            <div class="sub-toolbar">
              <el-button type="primary" size="small" @click="openPriceCreate">
                {{ t('ecommerce.express.addPrice') }}
              </el-button>
            </div>
            <el-table :data="prices" stripe border size="small" max-height="420" class="price-table">
              <el-table-column prop="provinceName" :label="t('ecommerce.express.province')" width="100" fixed />
              <el-table-column
                v-for="col in priceColumns"
                :key="col.key"
                :label="col.label"
                width="88"
                align="right"
              >
                <template #default="{ row }">{{ formatPrice(row[col.key]) }}</template>
              </el-table-column>
              <el-table-column
                :label="t('ecommerce.express.actions')"
                width="112"
                fixed="right"
                align="center"
                :class-name="TABLE_ACTIONS_CELL_CLASS"
              >
                <template #default="{ row }">
                  <div class="table-actions-cell-inner" @click.stop>
                    <el-button link type="primary" :title="t('ecommerce.express.edit')" @click.stop="openPriceEdit(row)">
                      <el-icon><Edit /></el-icon>
                    </el-button>
                    <el-button link type="primary" :title="t('ecommerce.express.copyPrice')" @click.stop="openPriceCopy(row)">
                      <el-icon><CopyDocument /></el-icon>
                    </el-button>
                    <el-button link type="danger" :title="t('ecommerce.express.delete')" @click.stop="onDeletePrice(row)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </el-tab-pane>

        <el-tab-pane :label="t('ecommerce.express.tabNotice')" name="notice" :disabled="!editingId">
          <div v-if="!editingId" class="tab-hint">{{ t('ecommerce.express.saveStationFirst') }}</div>
          <template v-else>
            <div class="sub-toolbar">
              <el-button type="primary" size="small" @click="openNoticeCreate">
                {{ t('ecommerce.express.addNotice') }}
              </el-button>
            </div>
            <el-table :data="notices" stripe border size="small" max-height="420">
              <el-table-column prop="sortOrder" :label="t('ecommerce.express.sortOrder')" width="70" align="center" />
              <el-table-column :label="t('ecommerce.express.noticeContent')" min-width="360">
                <template #default="{ row }">
                  <span :class="{ 'text-red': row.highlightRed }">{{ row.content }}</span>
                </template>
              </el-table-column>
              <el-table-column :label="t('ecommerce.express.highlightRed')" width="90" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.highlightRed" type="danger" size="small">{{ t('ecommerce.express.yes') }}</el-tag>
                  <span v-else>—</span>
                </template>
              </el-table-column>
              <el-table-column
                :label="t('ecommerce.express.actions')"
                width="88"
                align="center"
                :class-name="TABLE_ACTIONS_CELL_CLASS"
              >
                <template #default="{ row }">
                  <div class="table-actions-cell-inner" @click.stop>
                    <el-button link type="primary" :title="t('ecommerce.express.edit')" @click.stop="openNoticeEdit(row)">
                      <el-icon><Edit /></el-icon>
                    </el-button>
                    <el-button link type="danger" :title="t('ecommerce.express.delete')" @click.stop="onDeleteNotice(row)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button v-if="activeTab === 'basic'" type="primary" :loading="saving" @click="onSaveStation">
          {{ t('ecommerce.common.save') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="priceDialogVisible"
      :title="priceEditingId ? t('ecommerce.express.editPriceTitle') : t('ecommerce.express.addPriceTitle')"
      width="640px"
      destroy-on-close
      append-to-body
    >
      <el-form :model="priceForm" label-width="120px">
        <el-form-item :label="t('ecommerce.express.province')" required>
          <el-input v-model="priceForm.provinceName" />
        </el-form-item>
        <el-form-item v-for="col in priceColumns" :key="col.key" :label="col.label">
          <el-input-number
            v-model="priceForm[col.key]"
            :min="0"
            :precision="2"
            :step="0.5"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priceDialogVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="priceSaving" @click="onSavePrice">
          {{ t('ecommerce.common.save') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="noticeDialogVisible"
      :title="noticeEditingId ? t('ecommerce.express.editNoticeTitle') : t('ecommerce.express.addNoticeTitle')"
      width="560px"
      destroy-on-close
      append-to-body
    >
      <el-form :model="noticeForm" label-width="96px">
        <el-form-item :label="t('ecommerce.express.noticeContent')" required>
          <el-input v-model="noticeForm.content" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="t('ecommerce.express.highlightRed')">
          <el-switch v-model="noticeForm.highlightRed" />
        </el-form-item>
        <el-form-item :label="t('ecommerce.express.sortOrder')">
          <el-input-number v-model="noticeForm.sortOrder" :min="0" :step="1" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialogVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="noticeSaving" @click="onSaveNotice">
          {{ t('ecommerce.common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableColumnCtx } from 'element-plus'
import { CopyDocument, Delete, Edit } from '@element-plus/icons-vue'
import {
  createExpressNotice,
  createExpressPrice,
  createExpressStation,
  deleteExpressNotice,
  deleteExpressPrice,
  deleteExpressStation,
  fetchExpressNotices,
  fetchExpressPrices,
  fetchExpressStation,
  fetchExpressStations,
  updateExpressNotice,
  updateExpressPrice,
  updateExpressStation,
  type EcExpressNotice,
  type EcExpressPrice,
  type EcExpressStation,
  type EcExpressStationSaveRequest,
} from '@/api/ecommerce/express'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { TABLE_ACTIONS_CELL_CLASS } from '@/constants/table'
import { formatDate } from '@/utils/date'

const { t } = useI18n()

const saving = ref(false)
const keyword = ref('')

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination(
  (p, ps) => fetchExpressStations(keyword.value.trim() || undefined, { page: p, pageSize: ps }),
)

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const activeTab = ref('basic')

const prices = ref<EcExpressPrice[]>([])
const notices = ref<EcExpressNotice[]>([])

const priceDialogVisible = ref(false)
const priceEditingId = ref<number | null>(null)
const priceSaving = ref(false)

const noticeDialogVisible = ref(false)
const noticeEditingId = ref<number | null>(null)
const noticeSaving = ref(false)

const viewVisible = ref(false)
const viewLoading = ref(false)
const viewDetail = ref<EcExpressStation | null>(null)
const viewPrices = ref<EcExpressPrice[]>([])
const viewNotices = ref<EcExpressNotice[]>([])
const noticeCollapseNames = ref<string[]>([])

const form = reactive({
  name: '',
  contact: '',
  address: '',
  labelPrice: null as number | null,
  isDefault: false,
  nameAliases: [] as string[],
})

type PriceFieldKey =
  | 'priceW03Kg'
  | 'priceW05Kg'
  | 'priceW1Kg'
  | 'priceW15Kg'
  | 'priceW2Kg'
  | 'priceW25Kg'
  | 'priceW3Kg'
  | 'over3FirstPrice'
  | 'over3AdditionalPrice'

const priceColumns = computed(() => [
  { key: 'priceW03Kg' as PriceFieldKey, label: t('ecommerce.express.w03') },
  { key: 'priceW05Kg' as PriceFieldKey, label: t('ecommerce.express.w05') },
  { key: 'priceW1Kg' as PriceFieldKey, label: t('ecommerce.express.w1') },
  { key: 'priceW15Kg' as PriceFieldKey, label: t('ecommerce.express.w15') },
  { key: 'priceW2Kg' as PriceFieldKey, label: t('ecommerce.express.w2') },
  { key: 'priceW25Kg' as PriceFieldKey, label: t('ecommerce.express.w25') },
  { key: 'priceW3Kg' as PriceFieldKey, label: t('ecommerce.express.w3') },
  { key: 'over3FirstPrice' as PriceFieldKey, label: t('ecommerce.express.over3First') },
  { key: 'over3AdditionalPrice' as PriceFieldKey, label: t('ecommerce.express.over3Additional') },
])

const priceForm = reactive<Record<PriceFieldKey, number | null> & { provinceName: string }>({
  provinceName: '',
  priceW03Kg: null,
  priceW05Kg: null,
  priceW1Kg: null,
  priceW15Kg: null,
  priceW2Kg: null,
  priceW25Kg: null,
  priceW3Kg: null,
  over3FirstPrice: null,
  over3AdditionalPrice: null,
})

const noticeForm = reactive({
  content: '',
  highlightRed: false,
  sortOrder: 0,
})

function formatPrice(value: number | null | undefined) {
  if (value == null) return '—'
  return Number(value).toFixed(2)
}

function resetForm() {
  form.name = ''
  form.contact = ''
  form.address = ''
  form.labelPrice = null
  form.isDefault = false
  form.nameAliases = []
}

function resetPriceForm() {
  priceForm.provinceName = ''
  priceColumns.value.forEach((col) => {
    priceForm[col.key] = null
  })
}

function resetNoticeForm() {
  noticeForm.content = ''
  noticeForm.highlightRed = false
  noticeForm.sortOrder = 0
}

async function loadStations() {
  await load()
}

async function loadStationChildren(stationId: number) {
  const [priceList, noticeList] = await Promise.all([
    fetchExpressPrices(stationId),
    fetchExpressNotices(stationId),
  ])
  prices.value = priceList
  notices.value = noticeList
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => load(true), 300)
})

function onStationCellClick(
  row: EcExpressStation,
  column: TableColumnCtx<EcExpressStation>,
) {
  if (column.className === TABLE_ACTIONS_CELL_CLASS) return
  openView(row)
}

async function openView(row: EcExpressStation) {
  viewVisible.value = true
  viewLoading.value = true
  noticeCollapseNames.value = []
  viewDetail.value = null
  viewPrices.value = []
  viewNotices.value = []
  try {
    const detail = await fetchExpressStation(row.id)
    viewDetail.value = detail
    viewPrices.value = detail.prices || []
    viewNotices.value = detail.notices || []
  } finally {
    viewLoading.value = false
  }
}

function openEditFromView() {
  if (!viewDetail.value) return
  const row = viewDetail.value
  viewVisible.value = false
  openEdit(row)
}

function openCreate() {
  editingId.value = null
  activeTab.value = 'basic'
  resetForm()
  prices.value = []
  notices.value = []
  dialogVisible.value = true
}

async function openEdit(row: EcExpressStation) {
  editingId.value = row.id
  activeTab.value = 'basic'
  const detail = await fetchExpressStation(row.id)
  form.name = detail.name
  form.contact = detail.contact || ''
  form.address = detail.address || ''
  form.labelPrice = detail.labelPrice ?? null
  form.isDefault = !!detail.isDefault
  form.nameAliases = [...(detail.nameAliases || [])]
  prices.value = detail.prices || []
  notices.value = detail.notices || []
  dialogVisible.value = true
}

async function onSaveStation() {
  if (!form.name.trim()) {
    ElMessage.warning(t('ecommerce.express.nameRequired'))
    return
  }

  saving.value = true
  try {
    const payload: EcExpressStationSaveRequest = {
      name: form.name.trim(),
      contact: form.contact?.trim() || undefined,
      address: form.address?.trim() || undefined,
      labelPrice: form.labelPrice,
      isDefault: form.isDefault,
      nameAliases: form.nameAliases.map((item) => item.trim()).filter(Boolean),
    }
    if (editingId.value) {
      await updateExpressStation(editingId.value, payload)
    } else {
      const created = await createExpressStation(payload)
      editingId.value = created.id
    }
    ElMessage.success(t('ecommerce.common.saved'))
    await loadStations()
    if (editingId.value) {
      await loadStationChildren(editingId.value)
    }
  } finally {
    saving.value = false
  }
}

async function onDelete(row: EcExpressStation) {
  await ElMessageBox.confirm(
    t('ecommerce.express.deleteConfirm', { name: row.name }),
    { type: 'warning' },
  )
  await deleteExpressStation(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  await loadStations()
}

function openPriceCreate() {
  priceEditingId.value = null
  resetPriceForm()
  priceDialogVisible.value = true
}

function openPriceEdit(row: EcExpressPrice) {
  priceEditingId.value = row.id
  priceForm.provinceName = row.provinceName
  priceColumns.value.forEach((col) => {
    priceForm[col.key] = row[col.key] ?? null
  })
  priceDialogVisible.value = true
}

function openPriceCopy(row: EcExpressPrice) {
  priceEditingId.value = null
  resetPriceForm()
  priceColumns.value.forEach((col) => {
    priceForm[col.key] = row[col.key] ?? null
  })
  priceDialogVisible.value = true
}

async function onSavePrice() {
  if (!editingId.value) return
  if (!priceForm.provinceName.trim()) {
    ElMessage.warning(t('ecommerce.express.provinceRequired'))
    return
  }

  priceSaving.value = true
  try {
    const payload = {
      stationId: editingId.value,
      provinceName: priceForm.provinceName.trim(),
      priceW03Kg: priceForm.priceW03Kg,
      priceW05Kg: priceForm.priceW05Kg,
      priceW1Kg: priceForm.priceW1Kg,
      priceW15Kg: priceForm.priceW15Kg,
      priceW2Kg: priceForm.priceW2Kg,
      priceW25Kg: priceForm.priceW25Kg,
      priceW3Kg: priceForm.priceW3Kg,
      over3FirstPrice: priceForm.over3FirstPrice,
      over3AdditionalPrice: priceForm.over3AdditionalPrice,
    }
    if (priceEditingId.value) {
      await updateExpressPrice(priceEditingId.value, payload)
    } else {
      await createExpressPrice(payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    priceDialogVisible.value = false
    await loadStationChildren(editingId.value)
  } finally {
    priceSaving.value = false
  }
}

async function onDeletePrice(row: EcExpressPrice) {
  await ElMessageBox.confirm(
    t('ecommerce.express.deletePriceConfirm', { province: row.provinceName }),
    { type: 'warning' },
  )
  await deleteExpressPrice(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  if (editingId.value) {
    await loadStationChildren(editingId.value)
  }
}

function openNoticeCreate() {
  noticeEditingId.value = null
  resetNoticeForm()
  noticeDialogVisible.value = true
}

function openNoticeEdit(row: EcExpressNotice) {
  noticeEditingId.value = row.id
  noticeForm.content = row.content
  noticeForm.highlightRed = !!row.highlightRed
  noticeForm.sortOrder = row.sortOrder ?? 0
  noticeDialogVisible.value = true
}

async function onSaveNotice() {
  if (!editingId.value) return
  if (!noticeForm.content.trim()) {
    ElMessage.warning(t('ecommerce.express.noticeRequired'))
    return
  }

  noticeSaving.value = true
  try {
    const payload = {
      stationId: editingId.value,
      content: noticeForm.content.trim(),
      highlightRed: noticeForm.highlightRed,
      sortOrder: noticeForm.sortOrder,
    }
    if (noticeEditingId.value) {
      await updateExpressNotice(noticeEditingId.value, payload)
    } else {
      await createExpressNotice(payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    noticeDialogVisible.value = false
    await loadStationChildren(editingId.value)
  } finally {
    noticeSaving.value = false
  }
}

async function onDeleteNotice(row: EcExpressNotice) {
  await ElMessageBox.confirm(t('ecommerce.express.deleteNoticeConfirm'), { type: 'warning' })
  await deleteExpressNotice(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  if (editingId.value) {
    await loadStationChildren(editingId.value)
  }
}

watch(activeTab, async (tab) => {
  if ((tab === 'price' || tab === 'notice') && editingId.value) {
    await loadStationChildren(editingId.value)
  }
})

onMounted(loadStations)

defineExpose({ loadStations })
</script>

<style scoped lang="scss">
.panel-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.sub-toolbar {
  margin-bottom: 12px;
}

.tab-hint,
.form-hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  padding: 8px 0;
}

.form-hint {
  padding: 4px 0 0;
  line-height: 1.5;
}

.price-table {
  width: 100%;
}

.express-station-table :deep(.express-station-row) {
  cursor: pointer;
}

.express-station-table :deep(.table-actions-cell) {
  cursor: default;
}

.express-detail-body {
  min-height: 120px;
}

.express-detail-info {
  margin-bottom: 16px;
}

.express-detail-notices {
  margin-bottom: 16px;
  border: none;

  :deep(.el-collapse-item__header) {
    font-weight: 600;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }
}

.notice-list {
  margin: 0;
  padding: 0 0 0 18px;
  line-height: 1.7;
}

.express-detail-prices__title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.text-red {
  color: var(--el-color-danger);
  font-weight: 500;
}
</style>
