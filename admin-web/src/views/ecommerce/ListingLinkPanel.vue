<template>
  <div class="listing-link-panel">
    <div class="panel-toolbar">
      <el-input
        v-model="keyword"
        :placeholder="t('ecommerce.listingLink.searchPlaceholder')"
        clearable
        style="width: 240px"
      />
      <el-select
        v-model="platformId"
        clearable
        filterable
        :placeholder="t('ecommerce.listingLink.platform')"
        style="width: 160px"
        @change="onPlatformFilterChange"
      >
        <el-option v-for="p in platformOptions" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <el-select
        v-model="shopId"
        clearable
        filterable
        :placeholder="t('ecommerce.listingLink.shop')"
        style="width: 180px"
        @change="() => load(true)"
      >
        <el-option v-for="s in filteredShopOptions" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-button type="primary" @click="openCreate">{{ t('ecommerce.listingLink.add') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="records" stripe border size="small">
      <el-table-column prop="name" :label="t('ecommerce.listingLink.name')" min-width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="openDetail(row.id)">{{ row.name }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="shopName" :label="t('ecommerce.listingLink.shop')" width="140" show-overflow-tooltip />
      <el-table-column prop="platformName" :label="t('ecommerce.listingLink.platform')" width="110" show-overflow-tooltip />
      <el-table-column prop="productNames" :label="t('ecommerce.listingLink.product')" min-width="140" show-overflow-tooltip />
      <el-table-column :label="t('ecommerce.listingLink.listingTime')" width="170">
        <template #default="{ row }">{{ formatDateTime(row.listingTime) }}</template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.listingLink.skuCount')" width="90" align="center">
        <template #default="{ row }">{{ row.skuCount ?? row.skus?.length ?? 0 }}</template>
      </el-table-column>
      <el-table-column prop="remark" :label="t('ecommerce.listingLink.remark')" min-width="120" show-overflow-tooltip />
      <el-table-column
        :label="t('ecommerce.listingLink.actions')"
        width="120"
        fixed="right"
        align="center"
        :class-name="TABLE_ACTIONS_CELL_CLASS"
      >
        <template #default="{ row }">
          <div class="table-actions-cell-inner" @click.stop>
            <el-button link type="primary" @click.stop="openEdit(row.id)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button
              link
              type="primary"
              :title="t('ecommerce.listingLink.copyLink')"
              @click.stop="onCopyLink(row)"
            >
              <el-icon><DocumentCopy /></el-icon>
            </el-button>
            <el-button link type="danger" @click.stop="onDelete(row)">
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
      :title="editingId ? t('ecommerce.listingLink.editTitle') : t('ecommerce.listingLink.createTitle')"
      width="1180px"
      destroy-on-close
      top="5vh"
    >
      <el-form :model="form" label-width="108px">
        <el-form-item :label="t('ecommerce.listingLink.name')" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('ecommerce.listingLink.platformUrl')">
          <el-input v-model="form.platformUrl" :placeholder="t('ecommerce.listingLink.platformUrl')" />
        </el-form-item>
        <el-form-item :label="t('ecommerce.listingLink.product')">
          <el-select
            v-model="form.productIds"
            multiple
            clearable
            filterable
            remote
            reserve-keyword
            collapse-tags
            collapse-tags-tooltip
            :remote-method="searchProducts"
            :loading="productLoading"
            :placeholder="t('ecommerce.listingLink.productsPlaceholder')"
            style="width: 100%"
          >
            <el-option v-for="p in productOptions" :key="p.id" :label="productOptionLabel(p)" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('ecommerce.listingLink.shop')" required>
          <el-select v-model="form.shopId" filterable style="width: 100%" @change="onFormShopChange">
            <el-option v-for="s in shopOptions" :key="s.id" :label="shopOptionLabel(s)" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.shopId" :label="t('ecommerce.listingLink.costFormula')">
          <el-alert :title="shopCostFormulaHint" type="info" :closable="false" show-icon />
        </el-form-item>
        <el-form-item :label="t('ecommerce.listingLink.listingTime')" required>
          <el-date-picker
            v-model="form.listingTime"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('ecommerce.listingLink.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <div class="sku-section-header">
        <span>{{ t('ecommerce.listingLink.skuSection') }}</span>
        <el-button size="small" @click="addSkuRow">{{ t('ecommerce.listingLink.addSku') }}</el-button>
      </div>

      <el-table :data="form.skus" stripe border size="small" max-height="360">
        <el-table-column :label="t('ecommerce.listingLink.skuName')" min-width="100">
          <template #default="{ row }"><el-input v-model="row.skuName" /></template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.listingLink.skuCodes')" min-width="160">
          <template #default="{ row }">
            <ListingLinkSkuSelect
              v-model="row.skuCodes"
              :product-ids="form.productIds"
              :placeholder="t('ecommerce.listingLink.skuCodesHint')"
              @change="(picked) => onSkuCodesChange(row, picked)"
            />
          </template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.listingLink.inventory')" min-width="120">
          <template #default="{ row }">
            <template v-if="rowInventories(row).length">
              <div v-for="inv in rowInventories(row)" :key="inv.skuCode" class="inventory-line">
                <span>{{ inv.skuCode }}: {{ inv.quantity ?? 0 }}</span>
                <el-tag v-if="inv.alertActive" type="danger" size="small" class="inv-alert-tag">
                  {{ t('ecommerce.inventory.alerting') }}
                </el-tag>
              </div>
              <div v-if="rowInventoryMin(row) != null" class="inventory-min">
                {{ t('ecommerce.listingLink.inventoryMin') }}: {{ rowInventoryMin(row) }}
              </div>
            </template>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.listingLink.skuAmount')" width="88" align="right">
          <template #default="{ row }">{{ formatMoney(row.skuAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.listingLink.cartonAmount')" width="88" align="right">
          <template #default="{ row }">{{ formatMoney(row.cartonAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.listingLink.expressAmount')" width="88" align="right">
          <template #default="{ row }">{{ formatMoney(row.expressAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.listingLink.costPrice')" width="96" align="right">
          <template #default="{ row }">{{ formatMoney(row.costPrice) }}</template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.listingLink.discountPct')" width="108">
          <template #default="{ row }">
            <el-input-number
              v-model="row.discountPct"
              :min="1"
              :max="100"
              :precision="0"
              controls-position="right"
              style="width: 100%"
              @change="() => recalcRow(row)"
            />
          </template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.listingLink.couponAmount')" width="108">
          <template #default="{ row }">
            <el-input-number
              v-model="row.couponAmount"
              :min="0"
              :precision="2"
              controls-position="right"
              style="width: 100%"
              @change="() => recalcRow(row)"
            />
          </template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.listingLink.minSetAmount')" width="112" align="right">
          <template #default="{ row }">{{ formatMoney(row.minSetAmount) }}</template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.listingLink.actualSetAmount')" width="120">
          <template #header>
            <span>{{ t('ecommerce.listingLink.actualSetAmount') }}<span class="required-mark">*</span></span>
          </template>
          <template #default="{ row }">
            <el-input-number
              v-model="row.actualSetAmount"
              :min="0"
              :precision="2"
              controls-position="right"
              style="width: 100%"
              @change="() => recalcRow(row)"
            />
          </template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.listingLink.profit')" width="120" align="right">
          <template #default="{ row }">
            <div class="profit-cell">
              <span>{{ formatMoney(row.profit) }}</span>
              <el-tag v-if="row.pricingRisk === 'BELOW_MIN'" type="danger" size="small">
                {{ t('ecommerce.listingLink.pricingRiskBelowMin') }}
              </el-tag>
              <el-tag v-else-if="row.pricingRisk === 'NEGATIVE_PROFIT'" type="warning" size="small">
                {{ t('ecommerce.listingLink.pricingRiskNegativeProfit') }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('ecommerce.listingLink.actions')" width="80" align="center" fixed="right">
          <template #default="{ row, $index }">
            <div class="sku-row-actions" @click.stop>
              <el-button
                link
                type="primary"
                :title="t('ecommerce.listingLink.copySkuPricing')"
                @click="copySkuPricing(row)"
              >
                <el-icon><DocumentCopy /></el-icon>
              </el-button>
              <el-button link type="danger" :disabled="form.skus.length <= 1" @click="form.skus.splice($index, 1)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-alert
        v-if="form.costFormula"
        class="cost-formula-alert"
        :title="form.costFormula"
        type="info"
        :closable="false"
        show-icon
      />

      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.common.save') }}</el-button>
      </template>
    </el-dialog>

    <ListingLinkDetailDrawer v-model="detailVisible" :link-id="detailLinkId" @edit="openEdit" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, DocumentCopy, Edit } from '@element-plus/icons-vue'
import { fetchPlatformOptions, type EcPlatform } from '@/api/ecommerce/platform'
import { fetchShopOptions, type EcShop } from '@/api/ecommerce/shop'
import { fetchProduct, fetchProducts, type EcProductListItem } from '@/api/ecommerce/product'
import type { EcInventorySkuOption } from '@/api/ecommerce/inventory'
import {
  calculateListingPricing,
  copyListingLink,
  createListingLink,
  deleteListingLink,
  fetchListingLink,
  fetchListingLinks,
  updateListingLink,
  type EcListingLink,
  type EcListingLinkSku,
  type EcListingLinkSkuInventory,
} from '@/api/ecommerce/listingLink'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { TABLE_ACTIONS_CELL_CLASS } from '@/constants/table'
import { formatDateTime, todayDateString } from '@/utils/date'
import ListingLinkDetailDrawer from './ListingLinkDetailDrawer.vue'
import ListingLinkSkuSelect from './ListingLinkSkuSelect.vue'

const detailVisible = ref(false)
const detailLinkId = ref<number | null>(null)

const { t } = useI18n()

const keyword = ref('')
const platformId = ref<number | undefined>()
const shopId = ref<number | undefined>()
const platformOptions = ref<EcPlatform[]>([])
const shopOptions = ref<EcShop[]>([])

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const productLoading = ref(false)
const productOptions = ref<EcProductListItem[]>([])
const lastRecalcCostFormula = ref<string | undefined>()

const filteredShopOptions = computed(() => {
  if (!platformId.value) return shopOptions.value
  return shopOptions.value.filter((s) => s.platformId === platformId.value)
})

const shopCostFormulaHint = computed(() => {
  return form.costFormula || lastRecalcCostFormula.value || t('ecommerce.listingLink.calcHint')
})

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination(
  (p, ps) =>
    fetchListingLinks(keyword.value.trim() || undefined, shopId.value, platformId.value, {
      page: p,
      pageSize: ps,
    }),
)

type SkuRow = EcListingLinkSku

const form = reactive<{
  name: string
  platformUrl: string
  productIds: number[]
  shopId: number | undefined
  listingTime: string
  remark: string
  status: string
  costFormula: string | undefined
  skus: SkuRow[]
}>({
  name: '',
  platformUrl: '',
  productIds: [],
  shopId: undefined,
  listingTime: '',
  remark: '',
  status: 'ENABLED',
  costFormula: undefined,
  skus: [],
})

function emptySkuRow(): SkuRow {
  return {
    skuName: '',
    skuCodes: '',
    discountPct: 100,
    couponAmount: 0,
    minSetAmount: undefined,
    costPrice: undefined,
    actualSetAmount: undefined,
    profit: undefined,
    skuAmount: undefined,
    cartonAmount: undefined,
    expressAmount: undefined,
    pricingRisk: undefined,
    inventories: undefined,
  }
}

function shopOptionLabel(s: EcShop) {
  return s.platformName ? `${s.name} · ${s.platformName}` : s.name
}

function productOptionLabel(p: EcProductListItem) {
  return p.factoryName ? `${p.name} · ${p.factoryName}` : p.name
}

function formatMoney(v?: number | null) {
  if (v == null) return '—'
  return `¥${Number(v).toFixed(2)}`
}

function defaultListingDate() {
  return todayDateString()
}

function toListingDate(value?: string | null) {
  if (!value) return defaultListingDate()
  return value.trim().slice(0, 10)
}

function toListingDateTime(date: string) {
  if (!date) return date
  if (date.length <= 10) return `${date} 00:00:00`
  return date
}

function resetForm() {
  form.name = ''
  form.platformUrl = ''
  form.productIds = []
  form.shopId = shopOptions.value[0]?.id
  form.listingTime = defaultListingDate()
  form.remark = ''
  form.status = 'ENABLED'
  form.costFormula = undefined
  lastRecalcCostFormula.value = undefined
  form.skus = [emptySkuRow()]
}

function addSkuRow() {
  form.skus.push(emptySkuRow())
}

function rowInventories(row: SkuRow): EcListingLinkSkuInventory[] {
  return row.inventories ?? []
}

function rowInventoryMin(row: SkuRow): number | null {
  const invs = rowInventories(row)
  if (!invs.length) return null
  return Math.min(...invs.map((i) => i.quantity ?? 0))
}

function skuOptionAlertActive(p: EcInventorySkuOption) {
  if (p.ignoreAlert || p.alertThreshold == null) return false
  return (p.quantity ?? 0) <= p.alertThreshold
}

function onSkuCodesChange(row: SkuRow, picked: EcInventorySkuOption[]) {
  row.inventories = picked.map((p) => ({
    skuCode: p.skuCode,
    specName: p.specName,
    skuStatus: p.skuStatus,
    quantity: p.quantity,
    alertThreshold: p.alertThreshold,
    alertActive: skuOptionAlertActive(p),
    inboundAllowed: p.inboundAllowed,
  }))
  recalcRow(row)
}

async function recalcRow(row: SkuRow) {
  if (!form.shopId) {
    ElMessage.warning(t('ecommerce.listingLink.shopRequired'))
    return
  }
  if (!row.skuCodes?.trim()) {
    row.skuAmount = undefined
    row.cartonAmount = undefined
    row.expressAmount = undefined
    row.costPrice = undefined
    row.minSetAmount = undefined
    row.profit = undefined
    row.pricingRisk = undefined
    return
  }
  try {
    const result = await calculateListingPricing({
      shopId: form.shopId,
      skuCodes: row.skuCodes.trim(),
      discountPct: row.discountPct ?? 100,
      couponAmount: row.couponAmount ?? 0,
      actualSetAmount: row.actualSetAmount ?? undefined,
    })
    row.skuAmount = result.skuAmount
    row.cartonAmount = result.cartonAmount
    row.expressAmount = result.expressAmount
    row.costPrice = result.costPrice
    row.minSetAmount = result.minSetAmount
    row.profit = result.profit ?? undefined
    row.pricingRisk = result.pricingRisk
    row.skuCodes = result.skuCodes
    if (result.costFormula) {
      form.costFormula = result.costFormula
      lastRecalcCostFormula.value = result.costFormula
    }
  } catch {
    row.skuAmount = undefined
    row.cartonAmount = undefined
    row.expressAmount = undefined
    row.costPrice = undefined
    row.minSetAmount = undefined
    row.profit = undefined
    row.pricingRisk = undefined
  }
}

function onFormShopChange() {
  lastRecalcCostFormula.value = undefined
  for (const row of form.skus) {
    if (row.skuCodes?.trim()) recalcRow(row)
  }
}

function copySkuPricing(row: SkuRow) {
  const skuCodes = row.skuCodes?.trim()
  if (!skuCodes) {
    ElMessage.warning(t('ecommerce.listingLink.skuCodesHint'))
    return
  }
  if (row.actualSetAmount == null) {
    ElMessage.warning(t('ecommerce.listingLink.actualSetAmountRequired'))
    return
  }
  const newRow: SkuRow = {
    ...emptySkuRow(),
    skuCodes,
    discountPct: row.discountPct ?? 100,
    couponAmount: row.couponAmount ?? 0,
    actualSetAmount: row.actualSetAmount,
  }
  form.skus.push(newRow)
  recalcRow(newRow)
  ElMessage.success(t('ecommerce.listingLink.copySkuPricingSuccess'))
}

async function searchProducts(query: string) {
  productLoading.value = true
  try {
    const result = await fetchProducts(query.trim() || undefined, { page: 1, pageSize: 20 })
    productOptions.value = result.records ?? []
  } finally {
    productLoading.value = false
  }
}

async function ensureProductOption(productId: number, productName?: string) {
  if (productOptions.value.some((p) => p.id === productId)) return
  if (productName) {
    productOptions.value = [
      { id: productId, name: productName, rebatePct: 0, status: 'ENABLED', skuCount: 0 },
      ...productOptions.value,
    ]
    return
  }
  try {
    const detail = await fetchProduct(productId)
    productOptions.value = [
      {
        id: detail.id,
        name: detail.name,
        factoryName: detail.factoryName ?? undefined,
        rebatePct: detail.rebatePct,
        status: detail.status,
        skuCount: detail.skus?.length ?? 0,
      },
      ...productOptions.value,
    ]
  } catch {
    /* ignore */
  }
}

async function ensureProductOptions(
  productIds: number[],
  products?: { productId: number; productName?: string }[],
) {
  for (const p of products ?? []) {
    await ensureProductOption(p.productId, p.productName)
  }
  for (const id of productIds) {
    if (!productOptions.value.some((p) => p.id === id)) {
      await ensureProductOption(id)
    }
  }
}

function openDetail(id: number) {
  detailLinkId.value = id
  detailVisible.value = true
}

function openCreate() {
  editingId.value = null
  resetForm()
  searchProducts('')
  dialogVisible.value = true
}

function openCreateFromProduct(productId: number, productName: string, skuCodes?: string[]) {
  editingId.value = null
  resetForm()
  form.productIds = [productId]
  form.name = `${productName} 链接`
  if (skuCodes?.length) {
    form.skus = [{ ...emptySkuRow(), skuCodes: skuCodes.join(',') }]
  }
  ensureProductOption(productId, productName)
  dialogVisible.value = true
}

async function openEdit(id: number) {
  editingId.value = id
  const detail = await fetchListingLink(id)
  form.name = detail.name
  form.platformUrl = detail.platformUrl || ''
  form.productIds = (detail.products ?? []).map((p) => p.productId)
  form.shopId = detail.shopId
  form.listingTime = toListingDate(detail.listingTime)
  form.remark = detail.remark || ''
  form.status = detail.status
  form.costFormula = detail.costFormula
  lastRecalcCostFormula.value = detail.costFormula
  form.skus = (detail.skus?.length ? detail.skus : [emptySkuRow()]).map((s) => ({ ...s }))
  if (form.productIds.length) {
    await ensureProductOptions(form.productIds, detail.products)
  } else {
    searchProducts('')
  }
  dialogVisible.value = true
}

async function onSave() {
  if (!form.name.trim()) {
    ElMessage.warning(t('ecommerce.listingLink.nameRequired'))
    return
  }
  if (!form.shopId) {
    ElMessage.warning(t('ecommerce.listingLink.shopRequired'))
    return
  }
  if (!form.listingTime) {
    ElMessage.warning(t('ecommerce.listingLink.listingTimeRequired'))
    return
  }
  const skus = form.skus.filter((s) => s.skuName?.trim() && s.skuCodes?.trim())
  if (!skus.length) {
    ElMessage.warning(t('ecommerce.listingLink.skusRequired'))
    return
  }
  const missingActual = skus.find((s) => s.actualSetAmount == null)
  if (missingActual) {
    ElMessage.warning(t('ecommerce.listingLink.actualSetAmountRequired'))
    return
  }

  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      shopId: form.shopId,
      platformUrl: form.platformUrl?.trim() || undefined,
      productIds: form.productIds.length ? form.productIds : undefined,
      listingTime: toListingDateTime(form.listingTime),
      remark: form.remark?.trim() || undefined,
      status: form.status,
      skus: skus.map((s, i) => ({
        skuName: s.skuName.trim(),
        skuCodes: s.skuCodes.trim(),
        discountPct: s.discountPct ?? 100,
        couponAmount: s.couponAmount ?? 0,
        actualSetAmount: s.actualSetAmount ?? undefined,
        sortOrder: i,
      })),
    }
    if (editingId.value) {
      await updateListingLink(editingId.value, payload)
    } else {
      await createListingLink(payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onCopyLink(row: EcListingLink) {
  await copyListingLink(row.id)
  ElMessage.success(t('ecommerce.listingLink.copyLinkSuccess'))
  await load()
}

async function onDelete(row: EcListingLink) {
  await ElMessageBox.confirm(t('ecommerce.listingLink.deleteConfirm', { name: row.name }), { type: 'warning' })
  await deleteListingLink(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  await load()
}

async function refreshOptions() {
  platformOptions.value = await fetchPlatformOptions()
  shopOptions.value = await fetchShopOptions()
}

function onPlatformFilterChange() {
  if (shopId.value && !filteredShopOptions.value.some((s) => s.id === shopId.value)) {
    shopId.value = undefined
  }
  load(true)
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => load(true), 300)
})

async function loadListingLinks() {
  await refreshOptions()
  await load()
}

onMounted(loadListingLinks)

defineExpose({ loadListingLinks, openCreateFromProduct })
</script>

<style scoped lang="scss">
.panel-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.sku-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16px 0 8px;
  font-weight: 600;
  font-size: 14px;
}

.cost-formula-alert {
  margin-top: 12px;
}

.required-mark {
  margin-left: 2px;
  color: var(--el-color-danger);
}

.sku-row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.inventory-line {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  line-height: 1.5;
}

.inventory-min {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.inv-alert-tag {
  flex-shrink: 0;
}

.profit-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}
</style>
