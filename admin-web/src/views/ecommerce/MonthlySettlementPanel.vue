<template>
  <div class="monthly-settlement-panel">
    <div class="panel-toolbar">
      <el-date-picker
        v-model="settlementMonth"
        type="month"
        value-format="YYYY-MM"
        :placeholder="t('ecommerce.monthlySettlement.monthPlaceholder')"
        style="width: 160px"
      />
      <el-select
        v-model="shopFilter"
        clearable
        filterable
        :placeholder="t('ecommerce.monthlySettlement.allShops')"
        style="width: 180px"
      >
        <el-option v-for="s in shopOptions" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="load">{{ t('ecommerce.monthlySettlement.calculate') }}</el-button>
      <el-button @click="openExpressBillDialog">{{ t('ecommerce.monthlySettlement.importExpressBill') }}</el-button>
      <el-button @click="buyerExcludeVisible = true">{{ t('ecommerce.monthlySettlement.buyerExcludeConfig') }}</el-button>
    </div>
    <p class="panel-hint" :class="{ 'panel-hint--ok': expressBillImported }">{{ panelHintText }}</p>

    <div v-loading="loading">
      <template v-if="shopSummaries.length">
        <el-card v-for="shop in shopSummaries" :key="shop.shopId" shadow="never" class="shop-summary-card">
          <template #header>
            <div class="shop-summary-header">
              <span class="shop-summary-title">{{ shop.shopName || `#${shop.shopId}` }}</span>
              <el-button
                size="small"
                :loading="recalculatingShopId === shop.shopId"
                @click="recalculateShop(shop)"
              >
                {{ t('ecommerce.monthlySettlement.recalculate') }}
              </el-button>
            </div>
          </template>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item :label="t('ecommerce.monthlySettlement.totalRevenue')">
              <span class="amount-primary">{{ formatMoney(shop.totalRevenue) }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.monthlySettlement.estimatedTotalCost')">
              <span class="amount-primary">{{ formatMoney(shop.estimatedTotalCost) }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.monthlySettlement.actualTotalCost')">
              <span class="amount-primary">{{ formatMoney(shop.actualTotalCost) }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.monthlySettlement.estimatedTotalProfit')">
              <span class="amount-profit">{{ formatMoney(shop.estimatedTotalProfit) }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.monthlySettlement.actualTotalProfit')">
              <span class="amount-profit">{{ formatMoney(shop.actualTotalProfit) }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('ecommerce.monthlySettlement.orderStats')">
              {{ t('ecommerce.monthlySettlement.includedCount', { count: shop.includedOrderCount ?? 0 }) }} /
              {{ t('ecommerce.monthlySettlement.excludedCount', { count: shop.excludedOrderCount ?? 0 }) }} /
              {{ t('ecommerce.monthlySettlement.pendingCount', { count: shop.pendingOrderCount ?? 0 }) }}
            </el-descriptions-item>
          </el-descriptions>

          <div v-if="shop.maxProfitOrder?.orderNo" class="max-profit-block">
            {{ t('ecommerce.monthlySettlement.maxProfitOrder') }}：
            <strong>{{ shop.maxProfitOrder.orderNo }}</strong>
           （{{ t('ecommerce.monthlySettlement.received') }} {{ formatMoney(shop.maxProfitOrder.receivedAmount) }}，
            {{ t('ecommerce.monthlySettlement.profit') }} {{ formatMoney(shop.maxProfitOrder.profitAmount) }}）
          </div>

          <div v-if="shop.pendingOrders?.length" class="pending-block">
            <h4>{{ t('ecommerce.monthlySettlement.pendingOrders') }}</h4>
            <el-table :data="shop.pendingOrders" border size="small">
              <el-table-column prop="orderNo" :label="t('ecommerce.salesOrder.orderNo')" width="140" />
              <el-table-column prop="platformOrderNo" :label="t('ecommerce.salesOrder.platformOrderNo')" min-width="150" show-overflow-tooltip />
              <el-table-column prop="status" :label="t('ecommerce.salesOrder.status')" width="110">
                <template #default="{ row }">{{ statusLabel(row.status) }}</template>
              </el-table-column>
              <el-table-column prop="buyerName" :label="t('ecommerce.monthlySettlement.buyerName')" width="120" show-overflow-tooltip />
              <el-table-column :label="t('ecommerce.salesOrder.receivedAmount')" width="100" align="right">
                <template #default="{ row }">{{ formatMoney(row.receivedAmount) }}</template>
              </el-table-column>
              <el-table-column :label="t('ecommerce.salesOrder.orderTime')" width="160">
                <template #default="{ row }">{{ formatDateTime(row.orderTime) }}</template>
              </el-table-column>
              <el-table-column :label="t('ecommerce.monthlySettlement.includeDecision')" width="200" fixed="right">
                <template #default="{ row }">
                  <el-radio-group
                    v-model="pendingDecisions[row.orderId!]"
                    size="small"
                    @change="(v: boolean) => onPendingDecisionChange(row.orderId!, v)"
                  >
                    <el-radio-button :value="true">{{ t('ecommerce.monthlySettlement.include') }}</el-radio-button>
                    <el-radio-button :value="false">{{ t('ecommerce.monthlySettlement.exclude') }}</el-radio-button>
                  </el-radio-group>
                </template>
              </el-table-column>
            </el-table>
            <div class="pending-actions">
              <el-button type="primary" size="small" :loading="savingDecisions" @click="savePendingDecisions(shop)">
                {{ t('ecommerce.monthlySettlement.saveDecisions') }}
              </el-button>
            </div>
          </div>
        </el-card>
      </template>
      <el-empty v-else-if="!loading && calculated" :description="t('ecommerce.monthlySettlement.noData')" />
    </div>

    <el-dialog v-model="buyerExcludeVisible" :title="t('ecommerce.monthlySettlement.buyerExcludeConfig')" width="640px" destroy-on-close>
      <div class="buyer-exclude-toolbar">
        <el-select v-model="excludeFormShopId" clearable filterable :placeholder="t('ecommerce.monthlySettlement.allShops')" style="width: 160px">
          <el-option v-for="s in shopOptions" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-input v-model="excludeFormBuyerName" :placeholder="t('ecommerce.monthlySettlement.buyerNamePlaceholder')" style="width: 160px" />
        <el-input v-model="excludeFormRemark" :placeholder="t('ecommerce.monthlySettlement.remarkPlaceholder')" style="width: 140px" />
        <el-button type="primary" @click="addBuyerExclude">{{ t('ecommerce.monthlySettlement.add') }}</el-button>
      </div>
      <el-table v-loading="loadingExcludes" :data="buyerExcludes" border size="small" max-height="360">
        <el-table-column prop="shopName" :label="t('ecommerce.salesOrder.shop')" width="140">
          <template #default="{ row }">{{ row.shopName || t('ecommerce.monthlySettlement.allShops') }}</template>
        </el-table-column>
        <el-table-column prop="buyerName" :label="t('ecommerce.monthlySettlement.buyerName')" min-width="120" />
        <el-table-column prop="remark" :label="t('ecommerce.monthlySettlement.remark')" min-width="100" show-overflow-tooltip />
        <el-table-column width="80" align="center">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click="removeBuyerExclude(row.id!)">{{ t('ecommerce.monthlySettlement.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <ExpressBillImportDialog
      v-model="expressBillVisible"
      :initial-month="settlementMonth"
      @imported="onExpressBillImported"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { fetchShopOptions, type EcShop } from '@/api/ecommerce/shop'
import ExpressBillImportDialog from '@/views/ecommerce/ExpressBillImportDialog.vue'
import {
  deleteSettlementBuyerExclude,
  fetchExpressBillImported,
  fetchMonthlySettlement,
  fetchSettlementBuyerExcludes,
  saveSettlementBuyerExclude,
  saveSettlementOrderDecisions,
  type MonthlySettlementShopSummary,
  type SettlementBuyerExclude,
} from '@/api/ecommerce/monthlySettlement'
import { formatDateTime } from '@/utils/date'

const { t } = useI18n()

const settlementMonth = ref(formatMonth(new Date()))
const shopFilter = ref<number | undefined>()
const shopOptions = ref<EcShop[]>([])
const loading = ref(false)
const calculated = ref(false)
const savingDecisions = ref(false)
const recalculatingShopId = ref<number | null>(null)
const expressBillImported = ref(false)
const result = ref<{ shops: MonthlySettlementShopSummary[]; expressBillImported?: boolean } | null>(null)
const pendingDecisions = reactive<Record<number, boolean>>({})

const buyerExcludeVisible = ref(false)
const expressBillVisible = ref(false)
const loadingExcludes = ref(false)
const buyerExcludes = ref<SettlementBuyerExclude[]>([])
const excludeFormShopId = ref<number | undefined>()
const excludeFormBuyerName = ref('')
const excludeFormRemark = ref('')

const shopSummaries = computed(() => {
  const shops = result.value?.shops ?? []
  if (!shopFilter.value) return shops
  return shops.filter((s) => s.shopId === shopFilter.value)
})

const panelHintText = computed(() =>
  expressBillImported.value
    ? t('ecommerce.monthlySettlement.expressBillImportedHint')
    : t('ecommerce.monthlySettlement.expressBillNotImportedHint'),
)

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

function formatMonth(d: Date) {
  const y = d.getFullYear()
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  return `${y}-${m}`
}

function formatMoney(v?: number | null) {
  if (v == null) return '—'
  return `¥${Number(v).toFixed(2)}`
}

function statusLabel(s?: string) {
  return statusOptions.value.find((o) => o.value === s)?.label ?? s ?? '—'
}

function syncPendingDecisions(shops: MonthlySettlementShopSummary[]) {
  Object.keys(pendingDecisions).forEach((k) => delete pendingDecisions[Number(k)])
  for (const shop of shops) {
    for (const row of shop.pendingOrders ?? []) {
      if (row.orderId == null) continue
      if (row.included != null) {
        pendingDecisions[row.orderId] = row.included
      }
    }
  }
}

async function refreshExpressBillStatus() {
  if (!settlementMonth.value) {
    expressBillImported.value = false
    return
  }
  expressBillImported.value = await fetchExpressBillImported(settlementMonth.value)
}

async function load() {
  if (!settlementMonth.value) {
    ElMessage.warning(t('ecommerce.monthlySettlement.monthRequired'))
    return
  }
  loading.value = true
  calculated.value = true
  try {
    result.value = await fetchMonthlySettlement(settlementMonth.value, shopFilter.value)
    if (result.value.expressBillImported != null) {
      expressBillImported.value = result.value.expressBillImported
    }
    syncPendingDecisions(result.value?.shops ?? [])
  } finally {
    loading.value = false
  }
}

/** 按店铺重新统计（可多次执行，每次实时重算） */
async function recalculateShop(shop: MonthlySettlementShopSummary) {
  if (!settlementMonth.value || shop.shopId == null) return
  recalculatingShopId.value = shop.shopId
  try {
    const data = await fetchMonthlySettlement(settlementMonth.value, shop.shopId)
    const updated = data.shops?.[0]
    if (!updated) return
    if (result.value?.shops) {
      const idx = result.value.shops.findIndex((s) => s.shopId === shop.shopId)
      if (idx >= 0) {
        result.value.shops[idx] = updated
      } else {
        result.value.shops.push(updated)
      }
    } else {
      result.value = { shops: [updated] }
    }
    syncPendingDecisions(result.value?.shops ?? [])
  } finally {
    recalculatingShopId.value = null
  }
}

function onPendingDecisionChange(orderId: number, included: boolean) {
  pendingDecisions[orderId] = included
}

async function savePendingDecisions(shop: MonthlySettlementShopSummary) {
  if (!settlementMonth.value) return
  const items = (shop.pendingOrders ?? [])
    .filter((row) => row.orderId != null && pendingDecisions[row.orderId!] != null)
    .map((row) => ({ orderId: row.orderId!, included: pendingDecisions[row.orderId!] }))
  if (!items.length) {
    ElMessage.warning(t('ecommerce.monthlySettlement.noDecisions'))
    return
  }
  savingDecisions.value = true
  try {
    result.value = await saveSettlementOrderDecisions({
      settlementMonth: settlementMonth.value,
      items,
    })
    syncPendingDecisions(result.value?.shops ?? [])
    ElMessage.success(t('ecommerce.monthlySettlement.decisionsSaved'))
  } finally {
    savingDecisions.value = false
  }
}

async function loadBuyerExcludes() {
  loadingExcludes.value = true
  try {
    buyerExcludes.value = await fetchSettlementBuyerExcludes(shopFilter.value)
  } finally {
    loadingExcludes.value = false
  }
}

async function addBuyerExclude() {
  const name = excludeFormBuyerName.value.trim()
  if (!name) {
    ElMessage.warning(t('ecommerce.monthlySettlement.buyerNameRequired'))
    return
  }
  await saveSettlementBuyerExclude({
    shopId: excludeFormShopId.value ?? null,
    buyerName: name,
    remark: excludeFormRemark.value.trim() || undefined,
    enabled: 1,
  })
  excludeFormBuyerName.value = ''
  excludeFormRemark.value = ''
  await loadBuyerExcludes()
  ElMessage.success(t('ecommerce.common.saved'))
}

async function removeBuyerExclude(id: number) {
  await deleteSettlementBuyerExclude(id)
  await loadBuyerExcludes()
}

function openExpressBillDialog() {
  expressBillVisible.value = true
}

async function onExpressBillImported() {
  expressBillImported.value = true
  await refreshExpressBillStatus()
  if (calculated.value) {
    await load()
  }
}

watch(settlementMonth, () => {
  refreshExpressBillStatus()
})

watch(buyerExcludeVisible, (visible) => {
  if (visible) loadBuyerExcludes()
})

onMounted(async () => {
  shopOptions.value = await fetchShopOptions()
  await refreshExpressBillStatus()
})

defineExpose({ load })
</script>

<style scoped lang="scss">
.panel-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.panel-hint {
  margin: 0 0 16px;
  font-size: 12px;
  color: var(--el-color-warning);
  line-height: 1.5;

  &--ok {
    color: var(--el-color-success);
  }
}

.shop-summary-card {
  margin-bottom: 16px;
}

.shop-summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.shop-summary-title {
  font-weight: 600;
}

.amount-primary {
  color: #fff;
  font-weight: 700;
}

.amount-profit {
  color: var(--el-color-success);
  font-weight: 700;
}

.max-profit-block {
  margin-top: 12px;
  font-size: 13px;
}

.pending-block {
  margin-top: 16px;

  h4 {
    margin: 0 0 8px;
    font-size: 14px;
  }
}

.pending-actions {
  margin-top: 8px;
  text-align: right;
}

.buyer-exclude-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
