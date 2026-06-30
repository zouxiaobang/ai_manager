<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    size="760px"
    destroy-on-close
    class="inventory-detail-drawer"
    @open="onOpen"
  >
    <div v-loading="loading" class="inv-detail">
      <template v-if="showSpuOverview && spuSummary">
        <header class="inv-detail__hero">
          <div class="inv-detail__thumb">
            <el-image
              v-if="spuImageUrl"
              :src="spuImageUrl"
              fit="cover"
              class="inv-detail__image"
            >
              <template #error>
                <div class="inv-detail__image-fallback">{{ t('ecommerce.outbound.noSkuImage') }}</div>
              </template>
            </el-image>
            <div v-else class="inv-detail__image-fallback">{{ t('ecommerce.outbound.noSkuImage') }}</div>
          </div>
          <div class="inv-detail__hero-main">
            <div class="inv-detail__hero-info">
              <p class="inv-detail__line inv-detail__line--product">{{ spuSummary.productName }}</p>
              <p class="inv-detail__line inv-detail__line--sku">
                {{ t('ecommerce.inventory.spuSkuCount', { count: spuSummary.skuCount }) }}
              </p>
              <p class="inv-detail__line inv-detail__line--factory">{{ spuSummary.factoryName || '—' }}</p>
            </div>
            <div class="inv-detail__hero-status">
              <span
                class="inv-detail__status-badge"
                :class="spuSummary.alertActive ? 'is-alert' : 'is-normal'"
              >
                <span class="inv-detail__status-dot" aria-hidden="true" />
                {{ spuSummary.alertActive ? t('ecommerce.inventory.alerting') : t('ecommerce.inventory.normal') }}
              </span>
            </div>
          </div>
        </header>

        <div class="inv-detail__metrics">
          <article
            v-for="metric in spuDetailMetrics"
            :key="metric.key"
            class="inv-detail__metric"
            :class="`is-${metric.tone}`"
          >
            <div class="inv-detail__metric-icon" aria-hidden="true">
              <el-icon><component :is="metric.icon" /></el-icon>
            </div>
            <div class="inv-detail__metric-body">
              <span class="inv-detail__metric-label">{{ metric.label }}</span>
              <strong class="inv-detail__metric-value" :class="{ 'is-compact': metric.compact }">
                <CnyAmount v-if="metric.moneyValue != null" :value="metric.moneyValue" />
                <template v-else>{{ metric.value }}</template>
              </strong>
            </div>
          </article>
        </div>

        <el-tabs v-model="activeTab" class="inv-detail__tabs">
          <el-tab-pane :label="t('ecommerce.inventory.spuSkuBreakdown')" name="skus">
            <div class="inv-detail__panel">
              <div class="inv-detail__sku-cards">
                <article
                  v-for="row in spuSkuDetails"
                  :key="row.id"
                  class="inv-detail__sku-card"
                  :class="{ 'is-alert': row.alertActive }"
                  @click="openSpuSkuCard(row)"
                >
                  <div class="inv-detail__sku-card-head">
                    <span class="inv-detail__sku-card-spec">{{ row.specName || row.skuCode }}</span>
                    <el-tag :type="row.alertActive ? 'danger' : 'success'" size="small">
                      {{ row.alertActive ? t('ecommerce.inventory.alerting') : t('ecommerce.inventory.normal') }}
                    </el-tag>
                  </div>
                  <p class="inv-detail__sku-card-code">{{ row.skuCode }}</p>
                  <p class="inv-detail__sku-card-qty">{{ row.quantity ?? 0 }}</p>
                  <div class="inv-detail__sku-card-meta">
                    <span>{{ t('ecommerce.inventory.inTransit') }} {{ row.inTransitQty ?? 0 }}</span>
                    <span class="inv-detail__sku-card-meta-sep">·</span>
                    <span class="inv-detail__sku-card-value-wrap">
                      {{ t('ecommerce.inventory.stockValue') }}
                      <span class="inv-detail__sku-card-value">
                        <CnyAmount :value="skuStockValue(row)" />
                      </span>
                    </span>
                  </div>
                </article>
              </div>
              <el-empty v-if="!spuSkuDetails.length" :description="t('ecommerce.inventory.noData')" />
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('ecommerce.inventory.recentLogs')" name="logs">
            <div class="inv-detail__panel">
              <el-table :data="spuEnrichedLogs" stripe border size="small" max-height="360">
                <el-table-column :label="t('ecommerce.inventory.skuCode')" width="120" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.skuCode || '—' }}</template>
                </el-table-column>
                <el-table-column :label="t('ecommerce.inventory.changeType')" width="108">
                  <template #default="{ row }">
                    <el-tag :type="logChangeStyle(row.changeType).tagType" effect="light" size="small">
                      {{ changeTypeLabel(row.changeType) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column :label="t('ecommerce.inventory.changeQty')" width="96" align="right">
                  <template #default="{ row }">
                    <span class="inv-detail__log-qty" :style="{ color: logChangeStyle(row.changeType).color }">
                      {{ row.changeQty }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column :label="t('ecommerce.inventory.logOrderNo')" min-width="150" show-overflow-tooltip>
                  <template #default="{ row }">
                    <button
                      v-if="row.remarkParts && row.refId"
                      type="button"
                      class="inv-detail__order-link"
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
              <el-empty v-if="!spuEnrichedLogs.length" :description="t('ecommerce.inventory.noLogs')" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>

      <template v-else-if="showSkuDetail && detail">
        <button
          v-if="spuSkuDrillId"
          type="button"
          class="inv-detail__back"
          @click="backToSpuOverview"
        >
          ← {{ t('ecommerce.inventory.backToSpu') }}
        </button>

        <header class="inv-detail__hero">
          <div class="inv-detail__thumb">
            <el-image
              v-if="skuImageUrl"
              :src="skuImageUrl"
              fit="cover"
              class="inv-detail__image"
            >
              <template #error>
                <div class="inv-detail__image-fallback">{{ t('ecommerce.outbound.noSkuImage') }}</div>
              </template>
            </el-image>
            <div v-else class="inv-detail__image-fallback">{{ t('ecommerce.outbound.noSkuImage') }}</div>
          </div>
          <div class="inv-detail__hero-main">
            <div class="inv-detail__hero-info">
              <p class="inv-detail__line inv-detail__line--product">{{ detail.productName || '—' }}</p>
              <p class="inv-detail__line inv-detail__line--sku">{{ detail.skuCode }}</p>
              <button
                v-if="detail.productId && detail.specName"
                type="button"
                class="inv-detail__line inv-detail__line--spec is-link"
                @click="emitViewProduct"
              >
                {{ detail.specName }}
              </button>
              <p v-else class="inv-detail__line inv-detail__line--spec">{{ detail.specName || '—' }}</p>
              <p class="inv-detail__line inv-detail__line--factory">{{ detail.factoryName || '—' }}</p>
            </div>
            <div class="inv-detail__hero-status">
              <span
                class="inv-detail__status-badge"
                :class="detail.alertActive ? 'is-alert' : 'is-normal'"
              >
                <span class="inv-detail__status-dot" aria-hidden="true" />
                {{ detail.alertActive ? t('ecommerce.inventory.alerting') : t('ecommerce.inventory.normal') }}
              </span>
            </div>
          </div>
        </header>

        <div class="inv-detail__metrics">
          <article
            v-for="metric in detailMetrics"
            :key="metric.key"
            class="inv-detail__metric"
            :class="`is-${metric.tone}`"
          >
            <div class="inv-detail__metric-icon" aria-hidden="true">
              <el-icon><component :is="metric.icon" /></el-icon>
            </div>
            <div class="inv-detail__metric-body">
              <div class="inv-detail__metric-label-row">
                <span class="inv-detail__metric-label">{{ metric.label }}</span>
                <el-tag
                  v-if="metric.key === 'alertThreshold'"
                  :type="metric.ignoreAlert ? 'info' : 'success'"
                  size="small"
                  effect="plain"
                  class="inv-detail__metric-tag"
                >
                  {{
                    metric.ignoreAlert
                      ? t('ecommerce.inventory.ignoreAlertStatusOn')
                      : t('ecommerce.inventory.ignoreAlertStatusOff')
                  }}
                </el-tag>
              </div>
              <strong class="inv-detail__metric-value" :class="{ 'is-compact': metric.compact }">
                <CnyAmount v-if="metric.moneyValue != null" :value="metric.moneyValue" />
                <template v-else>{{ metric.value }}</template>
              </strong>
            </div>
          </article>
        </div>

        <section class="inv-detail__alert-card">
          <h4 class="inv-detail__alert-title">{{ t('ecommerce.inventory.alertSettings') }}</h4>
          <div class="inv-detail__alert-row">
            <span class="inv-detail__alert-label">{{ t('ecommerce.inventory.alertThreshold') }}</span>
            <div class="inv-detail__alert-stepper">
              <button
                type="button"
                class="inv-detail__alert-stepper-btn"
                :disabled="skuAlertDraft.alertThreshold <= 0 || skuAlertDraft.ignoreAlert || savingAlert"
                @click="decrementSkuAlertThreshold"
              >
                −
              </button>
              <el-input-number
                v-model="skuAlertDraft.alertThreshold"
                :min="0"
                :step="1"
                :controls="false"
                :disabled="skuAlertDraft.ignoreAlert || savingAlert"
                class="inv-detail__alert-stepper-input"
                @change="saveDetailAlert"
              />
              <button
                type="button"
                class="inv-detail__alert-stepper-btn"
                :disabled="skuAlertDraft.ignoreAlert || savingAlert"
                @click="incrementSkuAlertThreshold"
              >
                +
              </button>
              <span class="inv-detail__alert-stepper-unit">{{ t('ecommerce.inventory.unitPiece') }}</span>
            </div>
          </div>
          <div class="inv-detail__alert-switch-row">
            <div>
              <span class="inv-detail__alert-switch-label">{{ t('ecommerce.inventory.ignoreAlert') }}</span>
              <p class="inv-detail__alert-switch-hint">{{ t('ecommerce.inventory.ignoreAlertHint') }}</p>
            </div>
            <el-switch
              v-model="skuAlertDraft.ignoreAlert"
              :disabled="savingAlert"
              @change="saveDetailAlert"
            />
          </div>
        </section>

        <el-tabs v-model="activeTab" class="inv-detail__tabs">
          <el-tab-pane :label="t('ecommerce.inventory.packingEstimate')" name="packing">
            <div class="inv-detail__panel">
              <div v-if="packing" class="inv-detail__packing">
                <div class="inv-detail__packing-metrics">
                  <article
                    v-for="item in packingMetrics"
                    :key="item.key"
                    class="inv-detail__packing-metric"
                    :class="`is-${item.tone}`"
                  >
                    <div class="inv-detail__packing-metric-icon" aria-hidden="true">
                      <el-icon><component :is="item.icon" /></el-icon>
                    </div>
                    <div class="inv-detail__packing-metric-body">
                      <span class="inv-detail__packing-metric-label">{{ item.label }}</span>
                      <strong
                        class="inv-detail__packing-metric-value"
                        :class="{ 'is-text': item.text, 'is-compact': item.compact }"
                      >
                        {{ item.value }}
                      </strong>
                    </div>
                  </article>
                </div>
                <div class="inv-detail__packing-calc">
                  <span class="inv-detail__packing-calc-label">{{ t('ecommerce.inventory.outboundQty') }}</span>
                  <el-input-number
                    v-model="outboundQty"
                    :min="1"
                    :step="1"
                    controls-position="right"
                    class="inv-detail__packing-calc-input"
                    @change="refreshPacking"
                  />
                  <el-button type="primary" size="large" @click="refreshPacking">
                    {{ t('ecommerce.inventory.recalcPacking') }}
                  </el-button>
                </div>
              </div>
              <el-empty v-else :description="t('ecommerce.inventory.noPackingEstimate')" :image-size="64" />
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('ecommerce.inventory.recentLogs')" name="logs">
            <div class="inv-detail__panel">
              <el-table :data="enrichedRecentLogs" stripe border size="small" max-height="360">
                <el-table-column :label="t('ecommerce.inventory.changeType')" width="108">
                  <template #default="{ row }">
                    <el-tag :type="logChangeStyle(row.changeType).tagType" effect="light" size="small">
                      {{ changeTypeLabel(row.changeType) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column :label="t('ecommerce.inventory.changeQty')" width="96" align="right">
                  <template #default="{ row }">
                    <span class="inv-detail__log-qty" :style="{ color: logChangeStyle(row.changeType).color }">
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
                      class="inv-detail__order-link"
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
              <el-empty v-if="!enrichedRecentLogs.length" :description="t('ecommerce.inventory.noLogs')" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, markRaw, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Box, Van, Wallet, Warning, Grid, Files, DataBoard } from '@element-plus/icons-vue'
import {
  fetchInventory,
  fetchPackingEstimate,
  updateInventory,
  type EcInventory,
  type EcInventoryLog,
  type EcInventoryPackingEstimate,
} from '@/api/ecommerce/inventory'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'
import CnyAmount from '@/components/CnyAmount.vue'
import { formatDateTime } from '@/utils/date'
import {
  enrichInventoryLogs,
  getInventoryLogChangeStyle,
  parseInventoryLogRemark,
} from '@/utils/inventoryLogDisplay'

const props = defineProps<{
  modelValue: boolean
  inventoryId?: number | null
  spuItems?: EcInventory[] | null
}>()
const emit = defineEmits<{
  'update:modelValue': [boolean]
  refreshed: []
  viewProduct: [productId: number]
  viewInboundOrder: [orderId: number]
  viewOutboundOrder: [orderId: number]
}>()

const { t } = useI18n()
const loading = ref(false)
const detail = ref<EcInventory | null>(null)
const spuSkuDetails = ref<EcInventory[]>([])
const packing = ref<EcInventoryPackingEstimate | null>(null)
const outboundQty = ref(1)
const spuSkuDrillId = ref<number | null>(null)
const activeTab = ref('logs')
const savingAlert = ref(false)

const skuAlertDraft = reactive({ alertThreshold: 0, ignoreAlert: false })

const isSpuMode = computed(() => (props.spuItems?.length ?? 0) > 0)
const showSpuOverview = computed(() => isSpuMode.value && !spuSkuDrillId.value)
const showSkuDetail = computed(() => Boolean(detail.value && (!isSpuMode.value || spuSkuDrillId.value)))

const drawerTitle = computed(() => {
  if (showSpuOverview.value) return t('ecommerce.inventory.spuDetailTitle')
  return t('ecommerce.inventory.detailTitle')
})

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const stockValue = computed(() => (detail.value?.quantity ?? 0) * (detail.value?.salePrice ?? 0))

const spuSummary = computed(() => {
  if (!spuSkuDetails.value.length) return null
  const items = spuSkuDetails.value
  const primary = items[0]
  const quantity = items.reduce((sum, item) => sum + (item.quantity ?? 0), 0)
  const inTransitQty = items.reduce((sum, item) => sum + (item.inTransitQty ?? 0), 0)
  const alertThreshold = items.reduce((sum, item) => sum + (item.alertThreshold ?? 0), 0)
  const stockValueTotal = items.reduce((sum, item) => sum + skuStockValue(item), 0)
  return {
    productName: primary.productName || primary.skuCode,
    factoryName: primary.factoryName,
    skuCount: items.length,
    quantity,
    inTransitQty,
    alertThreshold,
    stockValueTotal,
    alertActive: items.some((item) => item.alertActive),
    productId: primary.productId,
  }
})

const spuImageUrl = computed(() => getEcommerceImageUrl(spuSkuDetails.value[0]?.imageName))

const spuDetailMetrics = computed(() => {
  if (!spuSummary.value) return []
  return [
    {
      key: 'quantity',
      tone: 'blue',
      label: t('ecommerce.inventory.quantity'),
      value: String(spuSummary.value.quantity),
      icon: markRaw(Box),
    },
    {
      key: 'inTransit',
      tone: 'orange',
      label: t('ecommerce.inventory.inTransit'),
      value: String(spuSummary.value.inTransitQty),
      icon: markRaw(Van),
    },
    {
      key: 'stockValue',
      tone: 'green',
      label: t('ecommerce.inventory.stockValue'),
      moneyValue: spuSummary.value.stockValueTotal,
      value: '',
      icon: markRaw(Wallet),
      compact: true,
    },
    {
      key: 'alertThreshold',
      tone: 'gray',
      label: t('ecommerce.inventory.spuTotalAlertThreshold'),
      value: String(spuSummary.value.alertThreshold),
      icon: markRaw(Warning),
    },
  ]
})

type SpuEnrichedLog = EcInventoryLog & {
  skuCode?: string
  remarkParts?: ReturnType<typeof parseInventoryLogRemark>
}

const spuEnrichedLogs = computed<SpuEnrichedLog[]>(() => {
  const merged: SpuEnrichedLog[] = []
  for (const item of spuSkuDetails.value) {
    const logs = enrichInventoryLogs(item.recentLogs ?? [], item.quantity ?? 0)
    for (const log of logs) {
      merged.push({
        ...log,
        skuCode: item.skuCode,
        remarkParts: parseInventoryLogRemark(log.remark, log.refType),
      })
    }
  }
  return merged.sort((a, b) => {
    const ta = a.createTime ? new Date(a.createTime).getTime() : 0
    const tb = b.createTime ? new Date(b.createTime).getTime() : 0
    return tb - ta
  })
})

function skuStockValue(row: EcInventory) {
  return (row.quantity ?? 0) * (row.salePrice ?? 0)
}

function syncSkuAlertDraft(item: EcInventory) {
  skuAlertDraft.alertThreshold = item.alertThreshold ?? 0
  skuAlertDraft.ignoreAlert = !!item.ignoreAlert
}

function decrementSkuAlertThreshold() {
  if (skuAlertDraft.alertThreshold > 0) {
    skuAlertDraft.alertThreshold -= 1
    void saveDetailAlert()
  }
}

function incrementSkuAlertThreshold() {
  skuAlertDraft.alertThreshold += 1
  void saveDetailAlert()
}

async function saveDetailAlert() {
  if (!detail.value) return
  const row = detail.value
  if (
    skuAlertDraft.alertThreshold === (row.alertThreshold ?? 0)
    && skuAlertDraft.ignoreAlert === !!row.ignoreAlert
  ) {
    return
  }
  savingAlert.value = true
  try {
    const updated = await updateInventory(row.id, {
      skuCode: row.skuCode,
      alertThreshold: skuAlertDraft.alertThreshold,
      ignoreAlert: skuAlertDraft.ignoreAlert,
    })
    detail.value = { ...detail.value, ...updated }
    syncSkuAlertDraft(detail.value)
    const index = spuSkuDetails.value.findIndex((item) => item.id === row.id)
    if (index >= 0) {
      spuSkuDetails.value[index] = { ...spuSkuDetails.value[index], ...updated }
    }
    ElMessage.success(t('ecommerce.inventory.alertThresholdSaved'))
    emit('refreshed')
  } finally {
    savingAlert.value = false
  }
}

async function openSpuSkuCard(row: EcInventory) {
  spuSkuDrillId.value = row.id
  loading.value = true
  try {
    detail.value = await fetchInventory(row.id)
    syncSkuAlertDraft(detail.value)
    packing.value = detail.value.packingEstimate ?? null
    outboundQty.value = detail.value.quantity && detail.value.quantity > 0 ? detail.value.quantity : 1
    activeTab.value = 'logs'
  } finally {
    loading.value = false
  }
}

function backToSpuOverview() {
  spuSkuDrillId.value = null
  detail.value = null
  activeTab.value = 'skus'
}

const detailMetrics = computed(() => {
  if (!detail.value) return []
  return [
    {
      key: 'quantity',
      tone: 'blue',
      label: t('ecommerce.inventory.quantity'),
      value: String(detail.value.quantity ?? 0),
      icon: markRaw(Box),
    },
    {
      key: 'inTransit',
      tone: 'orange',
      label: t('ecommerce.inventory.inTransit'),
      value: String(detail.value.inTransitQty ?? 0),
      icon: markRaw(Van),
    },
    {
      key: 'stockValue',
      tone: 'green',
      label: t('ecommerce.inventory.stockValue'),
      moneyValue: stockValue.value,
      value: '',
      icon: markRaw(Wallet),
      compact: true,
    },
    {
      key: 'alertThreshold',
      tone: 'gray',
      label: t('ecommerce.inventory.alertThreshold'),
      value: String(detail.value.alertThreshold ?? 0),
      ignoreAlert: skuAlertDraft.ignoreAlert,
      icon: markRaw(Warning),
    },
  ]
})

const packingMetrics = computed(() => {
  if (!packing.value) return []
  return [
    {
      key: 'carton',
      tone: 'blue',
      label: t('ecommerce.inventory.cartonName'),
      value: packing.value.cartonName || '—',
      icon: markRaw(Box),
      text: true,
    },
    {
      key: 'units',
      tone: 'green',
      label: t('ecommerce.product.unitsPerCarton'),
      value: String(packing.value.unitsPerCarton ?? '—'),
      icon: markRaw(Grid),
    },
    {
      key: 'cartons',
      tone: 'orange',
      label: t('ecommerce.inventory.cartonsNeeded'),
      value: String(packing.value.cartonsNeeded ?? 0),
      icon: markRaw(Files),
    },
    {
      key: 'volume',
      tone: 'gray',
      label: t('ecommerce.inventory.totalVolume'),
      value: formatVolume(packing.value.totalVolumeCm3),
      icon: markRaw(DataBoard),
      compact: true,
    },
  ]
})

const skuImageUrl = computed(() => getEcommerceImageUrl(detail.value?.imageName))

const enrichedRecentLogs = computed(() =>
  enrichInventoryLogs(detail.value?.recentLogs ?? [], detail.value?.quantity ?? 0).map((log) => ({
    ...log,
    remarkParts: parseInventoryLogRemark(log.remark, log.refType),
  })),
)

function logChangeStyle(changeType: string) {
  return getInventoryLogChangeStyle(changeType)
}

function openLogOrder(row: EcInventoryLog) {
  if (!row.refId) return
  if (row.refType === 'INBOUND_ORDER') {
    emit('viewInboundOrder', row.refId)
    return
  }
  if (row.refType === 'OUTBOUND_ORDER') {
    emit('viewOutboundOrder', row.refId)
  }
}

watch(
  () => [props.inventoryId, props.spuItems] as const,
  () => {
    if (visible.value) {
      void reloadDrawer()
    }
  },
)

async function reloadDrawer() {
  const drillId = spuSkuDrillId.value
  if (isSpuMode.value) {
    await loadSpuDetail()
    if (drillId != null) {
      const row = spuSkuDetails.value.find((item) => item.id === drillId)
      if (row) {
        await openSpuSkuCard(row)
      } else {
        spuSkuDrillId.value = null
      }
    }
    return
  }
  if (props.inventoryId) {
    await loadDetail()
  }
}

async function onOpen() {
  spuSkuDrillId.value = null
  activeTab.value = isSpuMode.value ? 'skus' : 'logs'
  await reloadDrawer()
}

async function loadSpuDetail() {
  const items = props.spuItems ?? []
  if (!items.length) {
    spuSkuDetails.value = []
    return
  }
  loading.value = true
  detail.value = null
  try {
    const details = await Promise.all(items.map((item) => fetchInventory(item.id)))
    spuSkuDetails.value = details.sort((a, b) => (b.quantity ?? 0) - (a.quantity ?? 0))
  } finally {
    loading.value = false
  }
}

async function loadDetail() {
  if (!props.inventoryId) return
  loading.value = true
  spuSkuDetails.value = []
  spuSkuDrillId.value = null
  try {
    detail.value = await fetchInventory(props.inventoryId)
    syncSkuAlertDraft(detail.value)
    packing.value = detail.value.packingEstimate ?? null
    outboundQty.value = detail.value.quantity && detail.value.quantity > 0 ? detail.value.quantity : 1
  } finally {
    loading.value = false
  }
}

async function refreshPacking() {
  if (!detail.value?.skuCode) return
  packing.value = await fetchPackingEstimate(detail.value.skuCode, outboundQty.value)
}

function emitViewProduct() {
  if (detail.value?.productId) {
    emit('viewProduct', detail.value.productId)
  }
}

function changeTypeLabel(type: string) {
  if (type === 'DEDUCT') return t('ecommerce.inventory.deduct')
  if (type === 'RECLAIM') return t('ecommerce.inventory.reclaim')
  if (type === 'INBOUND') return t('ecommerce.inbound.inbound')
  if (type === 'STOCKTAKE') return t('ecommerce.stocktake.stocktake')
  return type
}

function formatVolume(value?: number | null) {
  if (value == null) return '—'
  return `${Number(value).toLocaleString()} cm³`
}
</script>

<style scoped lang="scss">
.inv-detail {
  min-height: 200px;
}

.inv-detail__hero {
  display: flex;
  gap: 16px;
  padding: 18px;
  margin-bottom: 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%);
  border: 1px solid #dbeafe;
}

.inv-detail__thumb {
  flex-shrink: 0;
  width: 88px;
  height: 88px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.inv-detail__image {
  width: 88px;
  height: 88px;
}

.inv-detail__image-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 6px;
  font-size: 12px;
  color: #9ca3af;
  text-align: center;
  line-height: 1.3;
  background: #f3f4f6;
}

.inv-detail__hero-main {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 0;
}

.inv-detail__hero-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.inv-detail__line {
  margin: 0;
  line-height: 1.45;
  word-break: break-word;
}

.inv-detail__line--product {
  font-size: 14px;
  font-weight: 400;
  color: #374151;
}

.inv-detail__line--sku {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.inv-detail__line--spec {
  font-size: 17px;
  font-weight: 600;
  color: #166534;

  &.is-link {
    padding: 0;
    border: none;
    background: none;
    text-align: left;
    cursor: pointer;
    transition: color 0.15s ease;

    &:hover {
      color: #14532d;
      text-decoration: underline;
    }
  }
}

.inv-detail__line--factory {
  font-size: 13px;
  color: #6b7280;
}

.inv-detail__hero-status {
  flex-shrink: 0;
  align-self: center;
}

.inv-detail__status-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;

  &.is-normal {
    color: #16a34a;
    background: #ecfdf5;
    border: 1px solid #bbf7d0;
  }

  &.is-alert {
    color: #dc2626;
    background: #fef2f2;
    border: 1px solid #fecaca;
  }
}

.inv-detail__status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.inv-detail__status-badge.is-normal .inv-detail__status-dot {
  background: #22c55e;
}

.inv-detail__status-badge.is-alert .inv-detail__status-dot {
  background: #ef4444;
}

.inv-detail__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}

.inv-detail__metric {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 76px;
  padding: 14px 12px;
  border-radius: 12px;
  border: 1px solid transparent;

  &.is-blue {
    background: #eff6ff;
    border-color: #bfdbfe;

    .inv-detail__metric-icon {
      background: #3b82f6;
    }

    .inv-detail__metric-value {
      color: #2563eb;
    }
  }

  &.is-orange {
    background: #fff7ed;
    border-color: #fed7aa;

    .inv-detail__metric-icon {
      background: #f59e0b;
    }

    .inv-detail__metric-value {
      color: #ea580c;
    }
  }

  &.is-green {
    background: #f0fdf4;
    border-color: #bbf7d0;

    .inv-detail__metric-icon {
      background: #22c55e;
    }

    .inv-detail__metric-value {
      color: #16a34a;
    }
  }

  &.is-gray {
    background: #f8fafc;
    border-color: #e2e8f0;

    .inv-detail__metric-icon {
      background: #64748b;
    }

    .inv-detail__metric-value {
      color: #475569;
    }
  }
}

.inv-detail__metric-icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  color: #fff;
  font-size: 24px;
}

.inv-detail__metric-body {
  flex: 1;
  min-width: 0;
}

.inv-detail__metric-label-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 2px;
}

.inv-detail__metric-tag {
  flex-shrink: 0;
}

.inv-detail__metric-label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
}

.inv-detail__metric-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.1;

  &.is-compact {
    font-size: 18px;
  }
}

.inv-detail__tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 12px;
  }
}

.inv-detail__panel {
  min-height: 200px;

  :deep(.el-table th.el-table__cell) {
    font-weight: 700;
    color: #111827;
  }
}

.inv-detail__packing-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.inv-detail__packing-metric {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 84px;
  padding: 14px 12px;
  border-radius: 12px;
  border: 1px solid transparent;

  &.is-blue {
    background: #eff6ff;
    border-color: #bfdbfe;

    .inv-detail__packing-metric-icon {
      background: #3b82f6;
    }

    .inv-detail__packing-metric-value {
      color: #2563eb;
    }
  }

  &.is-green {
    background: #f0fdf4;
    border-color: #bbf7d0;

    .inv-detail__packing-metric-icon {
      background: #22c55e;
    }

    .inv-detail__packing-metric-value {
      color: #16a34a;
    }
  }

  &.is-orange {
    background: #fff7ed;
    border-color: #fed7aa;

    .inv-detail__packing-metric-icon {
      background: #f59e0b;
    }

    .inv-detail__packing-metric-value {
      color: #ea580c;
    }
  }

  &.is-gray {
    background: #f8fafc;
    border-color: #e2e8f0;

    .inv-detail__packing-metric-icon {
      background: #64748b;
    }

    .inv-detail__packing-metric-value {
      color: #475569;
    }
  }
}

.inv-detail__packing-metric-icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  color: #fff;
  font-size: 24px;
}

.inv-detail__packing-metric-body {
  flex: 1;
  min-width: 0;
}

.inv-detail__packing-metric-label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
}

.inv-detail__packing-metric-value {
  display: block;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  word-break: break-word;

  &.is-text {
    font-size: 18px;
    line-height: 1.35;
  }

  &.is-compact {
    font-size: 22px;
  }
}

.inv-detail__packing-calc {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  padding: 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%);
  border: 1px solid #dbeafe;
}

.inv-detail__packing-calc-label {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.inv-detail__packing-calc-input {
  width: 140px;

  :deep(.el-input__inner) {
    font-size: 18px;
    font-weight: 600;
  }
}

.inv-detail__log-qty {
  font-weight: 700;
}

.inv-detail__order-link {
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

.inv-detail__alert-input {
  width: 108px;
}

.inv-detail__back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 14px;
  padding: 0;
  border: none;
  background: none;
  color: #2563eb;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

.inv-detail__sku-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.inv-detail__sku-card {
  padding: 14px;
  border: 1px solid #e5e7eb;
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

.inv-detail__sku-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.inv-detail__sku-card-spec {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  line-height: 1.3;
}

.inv-detail__sku-card-code {
  margin: 0 0 8px;
  font-size: 12px;
  color: #6b7280;
}

.inv-detail__sku-card-qty {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  line-height: 1.1;
}

.inv-detail__sku-card-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
}

.inv-detail__sku-card-value {
  font-size: 14px;
  font-weight: 700;
  color: #991b1b;
}

.inv-detail__alert-card {
  margin-bottom: 16px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
}

.inv-detail__alert-title {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.inv-detail__alert-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.inv-detail__alert-label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.inv-detail__alert-stepper {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.inv-detail__alert-stepper-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  color: #374151;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
}

.inv-detail__alert-stepper-input {
  width: 72px;

  :deep(.el-input__inner) {
    text-align: center;
    font-weight: 700;
  }
}

.inv-detail__alert-stepper-unit {
  font-size: 12px;
  color: #6b7280;
}

.inv-detail__alert-switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.inv-detail__alert-switch-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.inv-detail__alert-switch-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.4;
}

@media (max-width: 720px) {
  .inv-detail__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .inv-detail__packing-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
