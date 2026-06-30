<template>
  <el-drawer
    :model-value="modelValue"
    :title="editing ? t('ecommerce.salesOrder.editTitle') : t('ecommerce.salesOrder.detailTitle')"
    size="720px"
    destroy-on-close
    append-to-body
    :z-index="zIndex"
    class="so-detail-drawer"
    @update:model-value="onDrawerVisibleChange"
    @open="emit('open')"
  >
    <div v-loading="loading" class="so-detail">
      <template v-if="order">
        <header class="so-detail__header">
          <div class="so-detail__shop">
            <template v-if="editing">
              <div class="so-detail__shop-edit">
                <img
                  v-if="editShopIcon"
                  :src="editShopIcon.src"
                  alt=""
                  class="so-detail__shop-avatar"
                  :class="{ 'is-custom': editShopIcon.isCustomAvatar }"
                />
                <el-select
                  v-model="draft.shopId"
                  filterable
                  class="so-detail__shop-select"
                  @change="onDraftShopChange"
                >
                  <el-option v-for="s in shopOptions" :key="s.id" :label="s.name" :value="s.id">
                    <div class="so-detail__shop-option">
                      <img
                        :src="shopOptionIcon(s).src"
                        alt=""
                        class="so-detail__shop-option-icon"
                        :class="{ 'is-avatar': shopOptionIcon(s).isCustomAvatar }"
                      />
                      <span>{{ s.name }}</span>
                    </div>
                  </el-option>
                </el-select>
              </div>
            </template>
            <template v-else>
              <img
                :src="shopIconMeta.src"
                alt=""
                class="so-detail__shop-avatar"
                :class="{ 'is-custom': shopIconMeta.isCustomAvatar }"
              />
              <div class="so-detail__shop-meta">
                <span class="so-detail__shop-name">{{ order.shopName || '—' }}</span>
                <el-tag
                  v-if="order.platformName"
                  size="small"
                  effect="plain"
                  class="so-detail__platform-tag"
                  :style="platformTagStyle"
                >
                  {{ order.platformName }}
                </el-tag>
              </div>
            </template>
          </div>
          <div class="so-detail__order-head">
            <div class="so-detail__order-main">
              <h2 v-if="!editing" class="so-detail__platform-order">
                <button
                  v-if="order.platformOrderNo"
                  type="button"
                  class="so-detail__copyable"
                  :title="t('ecommerce.salesOrder.copyPlatformOrderNo')"
                  @click="copyText(order.platformOrderNo)"
                >
                  {{ order.platformOrderNo }}
                </button>
                <span v-else>—</span>
              </h2>
              <el-input
                v-else
                v-model="draft.platformOrderNo"
                class="so-detail__platform-order-input"
                :placeholder="t('ecommerce.salesOrder.platformOrderNo')"
              />
              <p class="so-detail__order-no">
                {{ t('ecommerce.salesOrder.orderNo') }}：{{ order.orderNo }}
              </p>
            </div>
            <span class="so-detail__status" :class="statusBadgeClass">
              <span class="so-detail__status-dot" aria-hidden="true" />
              {{ statusLabel(order.status) }}
            </span>
          </div>
        </header>

        <div class="so-detail__timeline">
          <article class="so-detail__node">
            <div class="so-detail__node-rail" aria-hidden="true">
              <span class="so-detail__node-index">1</span>
              <span class="so-detail__node-line" />
            </div>
            <div class="so-detail__node-body">
              <h3 class="so-detail__node-title">{{ t('ecommerce.salesOrder.detailTimelineOrder') }}</h3>
              <dl class="so-detail__kv-grid">
                <div class="so-detail__kv">
                  <dt>{{ t('ecommerce.salesOrder.orderTime') }}</dt>
                  <dd v-if="!editing">{{ formatDateTime(order.orderTime) }}</dd>
                  <dd v-else>
                    <el-date-picker
                      v-model="draft.orderTime"
                      type="datetime"
                      value-format="YYYY-MM-DD HH:mm:ss"
                      style="width: 100%"
                      @change="onDraftOrderTimeChange"
                    />
                  </dd>
                </div>
                <div class="so-detail__kv">
                  <dt>{{ t('ecommerce.salesOrder.payTime') }}</dt>
                  <dd v-if="!editing">{{ formatDateTime(order.payTime) }}</dd>
                  <dd v-else>
                    <el-date-picker
                      v-model="draft.payTime"
                      type="datetime"
                      value-format="YYYY-MM-DD HH:mm:ss"
                      style="width: 100%"
                      @change="onDraftPayTimeChange"
                    />
                  </dd>
                </div>
                <div class="so-detail__kv">
                  <dt>{{ t('ecommerce.salesOrder.platformStatus') }}</dt>
                  <dd v-if="!editing">{{ order.platformStatus || '—' }}</dd>
                  <dd v-else>
                    <el-input v-model="draft.platformStatus" />
                  </dd>
                </div>
                <div class="so-detail__kv">
                  <dt>{{ t('ecommerce.salesOrder.receivedAmount') }}</dt>
                  <dd v-if="!editing" class="is-amount"><CnyAmount :value="order.receivedAmount" /></dd>
                  <dd v-else>
                    <el-input-number
                      v-model="draft.receivedAmount"
                      :min="0"
                      :precision="2"
                      controls-position="right"
                      style="width: 100%"
                    />
                  </dd>
                </div>
              </dl>
            </div>
          </article>

          <article class="so-detail__node">
            <div class="so-detail__node-rail" aria-hidden="true">
              <span class="so-detail__node-index">2</span>
              <span class="so-detail__node-line" />
            </div>
            <div class="so-detail__node-body">
              <h3 class="so-detail__node-title">
                {{ t('ecommerce.salesOrder.detailTimelineProducts') }}
                <span v-if="editing ? draft.lines.length : lineCount" class="so-detail__node-badge">
                  {{ t('ecommerce.salesOrder.lineCountTag', { count: editing ? draft.lines.length : lineCount }) }}
                </span>
              </h3>
              <template v-if="editing">
                <div class="so-detail__line-edit-list">
                  <article
                    v-for="(row, index) in draft.lines"
                    :key="index"
                    class="so-detail__line-edit-card"
                  >
                    <div class="so-detail__line-edit-fields">
                      <div class="so-detail__line-edit-field">
                        <label class="so-detail__line-edit-label">{{ t('ecommerce.salesOrder.linkName') }}</label>
                        <el-autocomplete
                          :model-value="row.linkName ?? ''"
                          :fetch-suggestions="fetchLinkNameSuggestions"
                          clearable
                          fit-input-width
                          :trigger-on-focus="true"
                          :placeholder="t('ecommerce.salesOrder.linkNameInputPlaceholder')"
                          @update:model-value="(v: string) => onLineLinkNameChange(row, v)"
                          @select="() => syncLineMatch(row)"
                        />
                      </div>
                      <div class="so-detail__line-edit-field">
                        <label class="so-detail__line-edit-label">{{ t('ecommerce.salesOrder.skuSpecName') }}</label>
                        <el-autocomplete
                          :model-value="row.skuSpecName ?? ''"
                          :fetch-suggestions="bindSkuSpecSuggestions(row)"
                          clearable
                          fit-input-width
                          :trigger-on-focus="true"
                          :placeholder="t('ecommerce.salesOrder.skuSpecInputPlaceholder')"
                          @update:model-value="(v: string) => onLineSkuSpecChange(row, v)"
                          @select="() => syncLineMatch(row)"
                        />
                      </div>
                      <div class="so-detail__line-edit-field so-detail__line-edit-field--qty">
                        <label class="so-detail__line-edit-label">{{ t('ecommerce.salesOrder.skuQuantity') }}</label>
                        <el-input-number
                          v-model="row.skuQuantity"
                          :min="1"
                          :precision="0"
                          controls-position="right"
                        />
                      </div>
                      <div class="so-detail__line-edit-field so-detail__line-edit-field--amount">
                        <label class="so-detail__line-edit-label">{{ t('ecommerce.salesOrder.lineReceived') }}</label>
                        <el-input-number
                          v-model="row.lineReceivedAmount"
                          :min="0"
                          :precision="2"
                          controls-position="right"
                        />
                      </div>
                    </div>
                    <el-button
                      link
                      type="danger"
                      class="so-detail__line-edit-remove"
                      :disabled="draft.lines.length <= 1"
                      @click="removeDraftLine(index)"
                    >
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </article>
                  <button type="button" class="so-detail__line-add" @click="addDraftLine">
                    <el-icon><Plus /></el-icon>
                    <span>{{ t('ecommerce.salesOrder.formAddLineEmpty') }}</span>
                  </button>
                  <p v-if="!linkSkuOptions.length" class="so-detail__line-hint">
                    {{ t('ecommerce.salesOrder.linkSkuManualHint') }}
                  </p>
                </div>
              </template>
              <template v-else>
                <div v-if="order.lines?.length" class="so-detail__line-list">
                  <article v-for="(line, index) in order.lines" :key="line.id ?? index" class="so-detail__line-card">
                    <div class="so-detail__line-main">
                      <strong class="so-detail__line-name">{{ line.linkName || '—' }}</strong>
                      <div class="so-detail__line-meta">
                        <el-tag v-if="line.skuSpecName" size="small" effect="light" class="so-detail__line-tag">
                          {{ line.skuSpecName }}
                        </el-tag>
                        <span class="so-detail__line-qty">
                          ×{{ line.skuQuantity ?? '—' }}
                        </span>
                      </div>
                    </div>
                    <div class="so-detail__line-side">
                      <el-tag size="small" :type="lineStatusTagType(line.status)" effect="plain">
                        {{ lineStatusLabel(line.status) }}
                      </el-tag>
                      <div class="so-detail__line-finance">
                        <span v-if="showLineFinance" class="so-detail__line-profit">
                          <CnyAmount :value="line.profit" />
                        </span>
                        <span
                          v-if="hasPositiveLoss(line.lossAmount)"
                          class="so-detail__line-loss"
                        >
                          {{ t('ecommerce.salesOrder.loss') }} <CnyAmount :value="line.lossAmount" />
                        </span>
                      </div>
                    </div>
                  </article>
                </div>
                <p v-else class="so-detail__empty">{{ t('ecommerce.salesOrder.linesRequired') }}</p>
              </template>
            </div>
          </article>

          <article class="so-detail__node">
            <div class="so-detail__node-rail" aria-hidden="true">
              <span class="so-detail__node-index">3</span>
              <span class="so-detail__node-line" />
            </div>
            <div class="so-detail__node-body">
              <h3 class="so-detail__node-title">{{ t('ecommerce.salesOrder.detailTimelineLogistics') }}</h3>
              <div class="so-detail__logistics-card">
                <div class="so-detail__logistics-main">
                  <div class="so-detail__logistics-summary">
                    <template v-if="!editing">
                      <p class="so-detail__logistics-line">
                        <span class="so-detail__inline-label">{{ t('ecommerce.salesOrder.trackingNumber') }}</span>
                        <button
                          v-if="order.trackingNumber"
                          type="button"
                          class="so-detail__inline-value so-detail__copyable"
                          @click="copyText(order.trackingNumber, 'copyTrackingNumberSuccess')"
                        >
                          {{ order.trackingNumber }}
                        </button>
                        <span v-else class="so-detail__inline-value">—</span>
                      </p>
                      <p class="so-detail__logistics-line">
                        <span class="so-detail__inline-label">{{ t('ecommerce.salesOrder.expressStation') }}</span>
                        <span class="so-detail__inline-value so-detail__express-value">
                          <ExpressStationAvatar
                            v-if="orderExpressStation"
                            :station="orderExpressStation"
                            size="xs"
                          />
                          <span>{{ order.expressStationName || '—' }}</span>
                        </span>
                      </p>
                    </template>
                    <template v-else>
                      <div class="so-detail__logistics-line so-detail__logistics-line--edit">
                        <span class="so-detail__inline-label">{{ t('ecommerce.salesOrder.trackingNumber') }}</span>
                        <div class="so-detail__inline-control">
                          <el-input v-model="draft.trackingNumber" />
                        </div>
                      </div>
                      <div class="so-detail__logistics-line so-detail__logistics-line--edit">
                        <span class="so-detail__inline-label">{{ t('ecommerce.salesOrder.expressStation') }}</span>
                        <div class="so-detail__inline-control">
                          <div class="so-detail__express-select">
                            <ExpressStationAvatar
                              v-if="selectedExpressStation"
                              :station="selectedExpressStation"
                              size="xs"
                            />
                            <el-select
                              v-model="draft.expressStationId"
                              clearable
                              filterable
                              style="width: 100%"
                            >
                              <el-option v-for="s in expressOptions" :key="s.id" :label="s.name" :value="s.id">
                                <div class="so-detail__express-option">
                                  <ExpressStationAvatar :station="s" size="xs" />
                                  <span>{{ s.name }}</span>
                                </div>
                              </el-option>
                            </el-select>
                          </div>
                        </div>
                      </div>
                    </template>
                  </div>
                  <button
                    v-if="!editing"
                    type="button"
                    class="so-detail__logistics-toggle"
                    :aria-label="
                      logisticsAddressExpanded
                        ? t('ecommerce.salesOrder.detailLogisticsCollapseAddress')
                        : t('ecommerce.salesOrder.detailLogisticsExpandAddress')
                    "
                    @click="logisticsAddressExpanded = !logisticsAddressExpanded"
                  >
                    <el-icon>
                      <ArrowUp v-if="logisticsAddressExpanded" />
                      <ArrowDown v-else />
                    </el-icon>
                  </button>
                </div>
                <div
                  v-if="logisticsAddressExpanded || editing"
                  class="so-detail__logistics-expanded"
                >
                  <dl class="so-detail__kv-grid">
                    <div v-if="!editing" class="so-detail__kv is-full">
                      <dt>{{ t('ecommerce.salesOrder.detailLogisticsTime') }}</dt>
                      <dd>
                        <span
                          v-if="logisticsTimeFrom(order) || logisticsTimeTo(order)"
                          class="so-detail__time-range"
                        >
                          <span v-if="logisticsTimeFrom(order)">{{ logisticsTimeFrom(order) }}</span>
                          <el-icon
                            v-if="logisticsTimeFrom(order) && logisticsTimeTo(order)"
                            class="so-detail__time-range-arrow"
                          >
                            <Right />
                          </el-icon>
                          <span v-if="logisticsTimeTo(order)">{{ logisticsTimeTo(order) }}</span>
                        </span>
                        <span v-else>—</span>
                      </dd>
                    </div>
                    <div class="so-detail__kv is-full">
                      <dt>{{ t('ecommerce.salesOrder.receiveProvince') }}</dt>
                      <dd v-if="!editing">{{ order.receiveProvince || '—' }}</dd>
                      <dd v-else>
                        <el-input v-model="draft.receiveProvince" />
                        <p class="so-detail__field-hint">{{ t('ecommerce.salesOrder.receiveProvinceHint') }}</p>
                      </dd>
                    </div>
                    <div class="so-detail__kv is-full">
                      <dt>{{ t('ecommerce.salesOrder.receiveAddress') }}</dt>
                      <dd v-if="!editing">{{ order.receiveAddress || '—' }}</dd>
                      <dd v-else>
                        <el-input v-model="draft.receiveAddress" type="textarea" :rows="3" @blur="onAddressBlur" />
                      </dd>
                    </div>
                  </dl>
                </div>
              </div>
            </div>
          </article>

          <article class="so-detail__node">
            <div class="so-detail__node-rail" aria-hidden="true">
              <span class="so-detail__node-index">4</span>
              <span class="so-detail__node-line" />
            </div>
            <div class="so-detail__node-body">
              <h3 class="so-detail__node-title">{{ t('ecommerce.salesOrder.detailTimelineSettlement') }}</h3>
              <div class="so-detail__settlement">
                <div class="so-detail__settlement-main">
                  <span class="so-detail__settlement-label">{{ t('ecommerce.salesOrder.profit') }}</span>
                  <strong
                    class="so-detail__settlement-profit"
                    :class="{ 'is-negative': isNegativeProfit(order.profitAmount) }"
                  >
                    <CnyAmount :value="order.profitAmount" signed />
                  </strong>
                </div>
                <div class="so-detail__settlement-metrics">
                  <div class="so-detail__settlement-col">
                    <p class="so-detail__settlement-line">
                      <span class="so-detail__inline-label">{{ t('ecommerce.salesOrder.totalCost') }}</span>
                      <span class="so-detail__settlement-value is-highlight"><CnyAmount :value="order.totalCostAmount" /></span>
                    </p>
                    <p class="so-detail__settlement-line">
                      <span class="so-detail__inline-label">{{ t('ecommerce.salesOrder.totalLoss') }}</span>
                      <span
                        class="so-detail__settlement-value"
                        :class="{ 'is-loss': hasPositiveLoss(order.totalLossAmount) }"
                      >
                        <CnyAmount :value="order.totalLossAmount" />
                      </span>
                    </p>
                  </div>
                  <div class="so-detail__settlement-divider" aria-hidden="true" />
                  <div class="so-detail__settlement-col">
                    <p class="so-detail__settlement-line">
                      <span class="so-detail__inline-label">{{ t('ecommerce.salesOrder.estimatedFreight') }}</span>
                      <span class="so-detail__settlement-value"><CnyAmount :value="order.estimatedFreightAmount" /></span>
                    </p>
                    <p class="so-detail__settlement-line">
                      <span class="so-detail__inline-label">{{ t('ecommerce.salesOrder.actualFreight') }}</span>
                      <span class="so-detail__settlement-value is-highlight"><CnyAmount :value="order.actualFreightAmount" /></span>
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </article>

          <article class="so-detail__node is-last">
            <div class="so-detail__node-rail" aria-hidden="true">
              <span class="so-detail__node-index">5</span>
            </div>
            <div class="so-detail__node-body">
              <h3 class="so-detail__node-title">{{ t('ecommerce.salesOrder.detailTimelineRemark') }}</h3>
              <template v-if="editing">
                <div class="so-detail__remark-edit">
                  <div class="so-detail__remark-field">
                    <label class="so-detail__remark-label so-detail__remark-label--seller">{{ t('ecommerce.salesOrder.sellerRemark') }}</label>
                    <el-input v-model="draft.sellerRemark" type="textarea" :rows="2" />
                  </div>
                  <div class="so-detail__remark-field">
                    <label class="so-detail__remark-label so-detail__remark-label--buyer">{{ t('ecommerce.salesOrder.buyerRemark') }}</label>
                    <el-input v-model="draft.buyerRemark" type="textarea" :rows="2" />
                  </div>
                </div>
              </template>
              <template v-else>
                <div v-if="hasRemark" class="so-detail__remark-box">
                  <p v-if="order.sellerRemark?.trim()">
                    <span class="so-detail__remark-label so-detail__remark-label--seller">{{ t('ecommerce.salesOrder.sellerRemark') }}</span>
                    {{ order.sellerRemark }}
                  </p>
                  <p v-if="order.buyerRemark?.trim()">
                    <span class="so-detail__remark-label so-detail__remark-label--buyer">{{ t('ecommerce.salesOrder.buyerRemark') }}</span>
                    {{ order.buyerRemark }}
                  </p>
                </div>
                <p v-else class="so-detail__empty">{{ t('ecommerce.salesOrder.detailTimelineNoRemark') }}</p>
              </template>
            </div>
          </article>
        </div>
      </template>
    </div>

    <template v-if="order" #footer>
      <div class="so-detail__footer">
        <el-button @click="onCloseClick">
          {{ editing ? t('ecommerce.common.cancel') : t('ecommerce.common.close') }}
        </el-button>
        <div class="so-detail__footer-actions">
          <template v-if="editing">
            <el-button type="primary" :loading="saving" @click="onSaveClick">
              {{ t('ecommerce.common.save') }}
            </el-button>
          </template>
          <template v-else>
            <el-button v-if="canEdit" type="primary" @click="enterEdit">
              {{ t('ecommerce.salesOrder.edit') }}
            </el-button>
            <el-button
              v-if="canDelete"
              type="danger"
              :loading="deleting"
              @click="emit('delete')"
            >
              {{ t('ecommerce.salesOrder.delete') }}
            </el-button>
          </template>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Delete, Plus, ArrowDown, ArrowUp, Right } from '@element-plus/icons-vue'
import CnyAmount from '@/components/CnyAmount.vue'
import type {
  EcSalesOrder,
  EcSalesOrderLineStatus,
  EcSalesOrderSaveRequest,
  EcSalesOrderStatus,
} from '@/api/ecommerce/salesOrder'
import type { EcShop } from '@/api/ecommerce/shop'
import type { EcExpressStation } from '@/api/ecommerce/express'
import type { ShopIconMeta } from '@/utils/shopVisual'
import { resolvePlatformOptionTone } from '@/utils/platformVisual'
import { resolveShopIconMeta } from '@/utils/shopVisual'
import { formatDateTime, todayDateString } from '@/utils/date'
import { parseProvinceFromAddress } from '@/utils/addressProvince'
import ExpressStationAvatar from '@/components/ecommerce/ExpressStationAvatar.vue'
import type { SalesOrderFormModel, SalesOrderLineFormRow } from './SalesOrderFormDialog.vue'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    loading?: boolean
    saving?: boolean
    deleting?: boolean
    order: EcSalesOrder | null
    shopIconMeta: ShopIconMeta
    shopOptions?: EcShop[]
    expressOptions?: EcExpressStation[]
    linkSkuOptions?: {
      key: string
      label: string
      linkName: string
      skuSpecName: string
      listingLinkSkuId: number
    }[]
    showDelete?: boolean
    saveCommitKey?: number
    zIndex?: number
  }>(),
  {
    shopOptions: () => [],
    expressOptions: () => [],
    linkSkuOptions: () => [],
    showDelete: true,
    zIndex: 3100,
  },
)

const canDelete = computed(
  () => props.showDelete !== false && (props.order?.source === 'MANUAL' || props.order?.status === 'DRAFT'),
)

const canEdit = computed(() => props.order?.source === 'MANUAL')

const emit = defineEmits<{
  'update:modelValue': [boolean]
  open: []
  save: [EcSalesOrderSaveRequest]
  delete: []
  'shop-change': [number]
}>()

const { t } = useI18n()

const editing = ref(false)
const logisticsAddressExpanded = ref(false)
const payTimeLinkedOrderTime = ref(true)

const draft = reactive<SalesOrderFormModel>({
  shopId: undefined,
  expressStationId: undefined,
  orderTime: '',
  payTime: '',
  platformStatus: '',
  platformOrderNo: '',
  receivedAmount: undefined,
  trackingNumber: '',
  receiveAddress: '',
  receiveProvince: '',
  buyerRemark: '',
  sellerRemark: '',
  lines: [],
})

const platformTagStyle = computed(() => {
  const tone = resolvePlatformOptionTone(undefined, props.order?.platformName)
  return {
    color: tone.color,
    borderColor: tone.color,
    backgroundColor: 'transparent',
  }
})

const editShopIcon = computed(() => {
  const shop = props.shopOptions.find((s) => s.id === draft.shopId)
  if (!shop) return null
  return resolveShopIconMeta(shop.name, shop.platformName, shop.platformCode, shop.avatarUrl)
})

const selectedExpressStation = computed(() =>
  props.expressOptions.find((s) => s.id === draft.expressStationId),
)

const orderExpressStation = computed(() => {
  const order = props.order
  if (!order) return null
  const matched = props.expressOptions.find((s) => s.id === order.expressStationId)
  if (matched) return matched
  if (order.expressStationName) {
    return { id: order.expressStationId ?? 0, name: order.expressStationName }
  }
  return null
})

const lineCount = computed(() => props.order?.lines?.length ?? props.order?.lineCount ?? 0)

const showLineFinance = computed(() => isSingleSkuOrder(props.order))

const hasRemark = computed(() => {
  const order = props.order
  if (!order) return false
  return Boolean(order.sellerRemark?.trim() || order.buyerRemark?.trim())
})

const statusBadgeClass = computed(() => {
  const status = props.order?.status
  if (!status) return ''
  if (status === 'COMPLETED') return 'is-completed'
  if (status === 'CANCELLED' || status === 'REFUNDED') return 'is-cancelled'
  if (status === 'SHIPPED' || status === 'PARTIAL_SHIPPED') return 'is-shipped'
  if (status === 'PAID' || status === 'DRAFT') return 'is-paid'
  if (status === 'PARTIAL_REFUND') return 'is-refund'
  return 'is-default'
})

watch(
  () => props.saveCommitKey,
  () => {
    if (editing.value) {
      editing.value = false
    }
  },
)

function emptyLine(): SalesOrderLineFormRow {
  return {
    listingLinkSkuId: null,
    linkName: '',
    skuSpecName: '',
    skuQuantity: 1,
    lineReceivedAmount: undefined,
  }
}

function populateDraftFromOrder(order: EcSalesOrder) {
  draft.shopId = order.shopId
  draft.expressStationId = order.expressStationId ?? undefined
  draft.orderTime = order.orderTime ?? `${todayDateString()} 00:00:00`
  draft.payTime = order.payTime ?? draft.orderTime
  payTimeLinkedOrderTime.value = !order.payTime || order.payTime === order.orderTime
  draft.platformStatus = order.platformStatus ?? '已完成'
  draft.platformOrderNo = order.platformOrderNo ?? ''
  draft.receivedAmount = order.receivedAmount ?? undefined
  draft.trackingNumber = order.trackingNumber ?? ''
  draft.receiveAddress = order.receiveAddress ?? ''
  draft.receiveProvince = order.receiveProvince ?? parseProvinceFromAddress(draft.receiveAddress) ?? ''
  draft.buyerRemark = order.buyerRemark ?? ''
  draft.sellerRemark = order.sellerRemark ?? ''
  const lines = order.lines?.length ? order.lines : [emptyLine()]
  draft.lines = lines.map((line) => {
    const linkName = line.linkName ?? ''
    const skuSpecName = line.skuSpecName ?? ''
    return {
      listingLinkSkuId: line.listingLinkSkuId ?? null,
      linkName,
      skuSpecName,
      skuQuantity: line.skuQuantity ?? 1,
      lineReceivedAmount: line.lineReceivedAmount ?? undefined,
      _pickerKey: linkName && skuSpecName ? `${linkName}|||${skuSpecName}` : undefined,
    }
  })
}

function enterEdit() {
  if (!props.order || !canEdit.value) return
  populateDraftFromOrder(props.order)
  editing.value = true
  logisticsAddressExpanded.value = true
  if (draft.shopId) {
    emit('shop-change', draft.shopId)
  }
}

function cancelEdit() {
  editing.value = false
}

function onDrawerVisibleChange(visible: boolean) {
  if (!visible) {
    editing.value = false
    logisticsAddressExpanded.value = false
  }
  emit('update:modelValue', visible)
}

function onCloseClick() {
  if (editing.value) {
    cancelEdit()
    return
  }
  emit('update:modelValue', false)
}

function onDraftOrderTimeChange(value: string) {
  if (payTimeLinkedOrderTime.value) {
    draft.payTime = value
  }
}

function onDraftPayTimeChange(value: string) {
  payTimeLinkedOrderTime.value = value === draft.orderTime
}

function onAddressBlur() {
  if (!draft.receiveProvince?.trim()) {
    draft.receiveProvince = parseProvinceFromAddress(draft.receiveAddress) ?? ''
  }
}

function onDraftShopChange(shopId: number) {
  if (shopId) {
    emit('shop-change', shopId)
  }
}

function addDraftLine() {
  draft.lines.push(emptyLine())
}

function removeDraftLine(index: number) {
  draft.lines.splice(index, 1)
}

function shopOptionIcon(shop: EcShop) {
  return resolveShopIconMeta(shop.name, shop.platformName, shop.platformCode, shop.avatarUrl)
}

function fetchLinkNameSuggestions(query: string, cb: (results: { value: string }[]) => void) {
  const q = query.trim().toLowerCase()
  const names = new Set<string>()
  for (const opt of props.linkSkuOptions) {
    if (!q || opt.linkName.toLowerCase().includes(q)) {
      names.add(opt.linkName)
    }
  }
  const results = [...names].map((value) => ({ value }))
  if (q && !names.has(query.trim())) {
    results.unshift({ value: query.trim() })
  }
  cb(results)
}

function fetchSkuSpecSuggestions(
  query: string,
  cb: (results: { value: string }[]) => void,
  row: SalesOrderLineFormRow,
) {
  const q = query.trim().toLowerCase()
  const link = row.linkName?.trim()
  const specs = new Set<string>()
  for (const opt of props.linkSkuOptions) {
    if (link && opt.linkName !== link) continue
    if (!q || opt.skuSpecName.toLowerCase().includes(q)) {
      specs.add(opt.skuSpecName)
    }
  }
  const results = [...specs].map((value) => ({ value }))
  if (q && !specs.has(query.trim())) {
    results.unshift({ value: query.trim() })
  }
  cb(results)
}

function bindSkuSpecSuggestions(row: SalesOrderLineFormRow) {
  return (query: string, cb: (results: { value: string }[]) => void) => {
    fetchSkuSpecSuggestions(query, cb, row)
  }
}

function onLineLinkNameChange(row: SalesOrderLineFormRow, value: string) {
  row.linkName = value
  row.listingLinkSkuId = undefined
  syncLineMatch(row)
}

function onLineSkuSpecChange(row: SalesOrderLineFormRow, value: string) {
  row.skuSpecName = value
  row.listingLinkSkuId = undefined
  syncLineMatch(row)
}

function syncLineMatch(row: SalesOrderLineFormRow) {
  const linkName = row.linkName?.trim()
  const skuSpecName = row.skuSpecName?.trim()
  if (!linkName || !skuSpecName) {
    row.listingLinkSkuId = undefined
    return
  }
  const opt = props.linkSkuOptions.find(
    (item) => item.linkName === linkName && item.skuSpecName === skuSpecName,
  )
  row.listingLinkSkuId = opt?.listingLinkSkuId
}

function onSaveClick() {
  if (!draft.shopId) {
    ElMessage.warning(t('ecommerce.salesOrder.shopRequired'))
    return
  }
  if (!draft.orderTime) {
    ElMessage.warning(t('ecommerce.salesOrder.orderTimeRequired'))
    return
  }
  const lines = draft.lines.filter((l) => l.linkName?.trim() && l.skuSpecName?.trim())
  if (!lines.length) {
    ElMessage.warning(t('ecommerce.salesOrder.linesRequired'))
    return
  }
  emit('save', {
    shopId: draft.shopId,
    expressStationId: draft.expressStationId ?? null,
    orderTime: draft.orderTime,
    payTime: draft.payTime || draft.orderTime,
    platformStatus: draft.platformStatus || undefined,
    platformOrderNo: draft.platformOrderNo || undefined,
    receivedAmount: draft.receivedAmount ?? null,
    trackingNumber: draft.trackingNumber || undefined,
    receiveAddress: draft.receiveAddress || undefined,
    receiveProvince: draft.receiveProvince || undefined,
    buyerRemark: draft.buyerRemark || undefined,
    sellerRemark: draft.sellerRemark || undefined,
    lines: lines.map((l, i) => ({
      listingLinkSkuId: l.listingLinkSkuId ?? null,
      linkName: l.linkName!.trim(),
      skuSpecName: l.skuSpecName!.trim(),
      skuQuantity: l.skuQuantity ?? 1,
      lineReceivedAmount: l.lineReceivedAmount ?? null,
      sortOrder: i,
    })),
  })
}

function logisticsTimeFrom(order: EcSalesOrder) {
  const value = formatDateTime(order.shipTime)
  return value === '—' ? '' : value
}

function logisticsTimeTo(order: EcSalesOrder) {
  const value = formatDateTime(order.completeTime)
  return value === '—' ? '' : value
}

function isSingleSkuOrder(order?: EcSalesOrder | null) {
  const count = order?.lines?.length ?? order?.lineCount ?? 0
  return count === 1
}

function statusLabel(status?: EcSalesOrderStatus) {
  const map: Record<string, string> = {
    DRAFT: t('ecommerce.salesOrder.statusDraft'),
    PAID: t('ecommerce.salesOrder.statusPaid'),
    PARTIAL_SHIPPED: t('ecommerce.salesOrder.statusPartialShipped'),
    SHIPPED: t('ecommerce.salesOrder.statusShipped'),
    PARTIAL_REFUND: t('ecommerce.salesOrder.statusPartialRefund'),
    COMPLETED: t('ecommerce.salesOrder.statusCompleted'),
    REFUNDED: t('ecommerce.salesOrder.statusRefunded'),
    CANCELLED: t('ecommerce.salesOrder.statusCancelled'),
  }
  return map[status ?? ''] ?? status ?? '—'
}

function lineStatusLabel(status?: EcSalesOrderLineStatus) {
  const map: Record<string, string> = {
    PAID: t('ecommerce.salesOrder.linePaid'),
    SHIPPED: t('ecommerce.salesOrder.lineShipped'),
    COMPLETED: t('ecommerce.salesOrder.lineCompleted'),
    CANCELLED: t('ecommerce.salesOrder.lineCancelled'),
    PARTIAL_REFUND: t('ecommerce.salesOrder.linePartialRefund'),
    REFUNDED: t('ecommerce.salesOrder.lineRefunded'),
    RETURNED: t('ecommerce.salesOrder.lineReturned'),
  }
  return map[status ?? ''] ?? status ?? '—'
}

function lineStatusTagType(status?: EcSalesOrderLineStatus) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'SHIPPED') return 'primary'
  if (status === 'PAID') return 'warning'
  if (status === 'CANCELLED') return 'info'
  if (status === 'REFUNDED' || status === 'RETURNED' || status === 'PARTIAL_REFUND') return 'danger'
  return undefined
}

function isNegativeProfit(v?: number | null) {
  return v != null && Number(v) < 0
}

function hasPositiveLoss(v?: number | null) {
  return v != null && Number(v) !== 0
}

async function copyText(
  text: string,
  successMessageKey: 'copyPlatformOrderNoSuccess' | 'copyTrackingNumberSuccess' = 'copyPlatformOrderNoSuccess',
) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(t(`ecommerce.salesOrder.${successMessageKey}`))
  } catch {
    ElMessage.error(t('ecommerce.salesOrder.copyFailed'))
  }
}
</script>

<style scoped lang="scss">
.so-detail-drawer {
  :deep(.el-drawer__body) {
    padding: 0 20px 12px;
    overflow: auto;
  }

  :deep(.el-drawer__footer) {
    padding: 12px 20px 16px;
    border-top: 1px solid var(--el-border-color-lighter);
  }
}

.so-detail {
  min-height: 120px;
}

.so-detail__header {
  padding: 8px 4px 20px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  margin-bottom: 8px;
}

.so-detail__shop {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.so-detail__shop-edit {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.so-detail__shop-select {
  flex: 1;
  min-width: 0;
}

.so-detail__shop-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.so-detail__shop-option-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
  flex-shrink: 0;

  &.is-avatar {
    border-radius: 50%;
    object-fit: cover;
  }
}

.so-detail__shop-avatar {
  width: 36px;
  height: 36px;
  margin-right: 10px;
  object-fit: contain;
  flex-shrink: 0;

  &.is-custom {
    border-radius: 50%;
    object-fit: cover;
  }
}

.so-detail__shop-edit .so-detail__shop-avatar {
  margin-right: 0;
}

.so-detail__shop-meta {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.so-detail__shop-name {
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
  color: var(--el-text-color-primary);
}

.so-detail__platform-tag {
  flex-shrink: 0;
  height: 22px;
  padding: 0 8px;
  font-weight: 600;
  border-width: 1px;

  :deep(.el-tag__content) {
    line-height: 20px;
  }
}

.so-detail__order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.so-detail__order-main {
  min-width: 0;
  flex: 1;
}

.so-detail__platform-order {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.25;
  color: var(--el-text-color-primary);
  letter-spacing: 0.02em;
}

.so-detail__platform-order-input {
  margin-bottom: 6px;

  :deep(.el-input__wrapper) {
    padding: 8px 12px;
  }

  :deep(.el-input__inner) {
    font-size: 22px;
    font-weight: 700;
    letter-spacing: 0.02em;
  }
}

.so-detail__order-no {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.so-detail__status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;

  &.is-completed {
    color: #16a34a;
    background: #ecfdf5;
    border: 1px solid #bbf7d0;

    .so-detail__status-dot { background: #22c55e; }
  }

  &.is-shipped {
    color: #2563eb;
    background: #eff6ff;
    border: 1px solid #bfdbfe;

    .so-detail__status-dot { background: #3b82f6; }
  }

  &.is-paid {
    color: #c2410c;
    background: #fff7ed;
    border: 1px solid #fed7aa;

    .so-detail__status-dot { background: #f59e0b; }
  }

  &.is-cancelled {
    color: #64748b;
    background: #f8fafc;
    border: 1px solid #e2e8f0;

    .so-detail__status-dot { background: #94a3b8; }
  }

  &.is-refund {
    color: #dc2626;
    background: #fef2f2;
    border: 1px solid #fecaca;

    .so-detail__status-dot { background: #ef4444; }
  }

  &.is-default {
    color: var(--el-text-color-regular);
    background: var(--el-fill-color-light);
    border: 1px solid var(--el-border-color-lighter);

    .so-detail__status-dot { background: var(--el-text-color-secondary); }
  }
}

.so-detail__status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.so-detail__copyable {
  padding: 0;
  border: none;
  background: none;
  font: inherit;
  color: inherit;
  cursor: pointer;
  text-align: left;

  &:hover {
    color: var(--el-color-primary);
  }
}

.so-detail__timeline {
  padding: 4px 0 8px;
}

.so-detail__node {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding-bottom: 4px;

  &.is-last .so-detail__node-line {
    display: none;
  }
}

.so-detail__node-rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 28px;
  flex-shrink: 0;
}

.so-detail__node-index {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1px solid #e5e7eb;
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
  flex-shrink: 0;
  z-index: 1;
}

.so-detail__node:nth-child(1) .so-detail__node-index {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #2563eb;
}

.so-detail__node:nth-child(2) .so-detail__node-index {
  background: #ecfdf5;
  border-color: #a7f3d0;
  color: #059669;
}

.so-detail__node:nth-child(3) .so-detail__node-index {
  background: #f0f9ff;
  border-color: #bae6fd;
  color: #0284c7;
}

.so-detail__node:nth-child(4) .so-detail__node-index {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #ea580c;
}

.so-detail__node:nth-child(5) .so-detail__node-index {
  background: #f5f3ff;
  border-color: #ddd6fe;
  color: #7c3aed;
}

.so-detail__node-line {
  flex: 1;
  width: 1px;
  min-height: 28px;
  margin-top: 0;
  background: #e5e7eb;
}

.so-detail__node-body {
  flex: 1;
  min-width: 0;
  padding-top: 3px;
  padding-bottom: 24px;
}

.so-detail__node-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.so-detail__node-badge {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
}

.so-detail__kv-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
  margin: 0;

  &--compact {
    margin-top: 14px;
    padding-top: 14px;
    border-top: 1px dashed var(--el-border-color-lighter);
  }
}

.so-detail__logistics-card {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}

.so-detail__logistics-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.so-detail__logistics-summary {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.so-detail__logistics-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  min-width: 0;
  white-space: nowrap;

  &--edit {
    white-space: normal;
  }
}

.so-detail__inline-label {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.so-detail__inline-value {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.so-detail__express-value {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;

  > span:last-child {
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.so-detail__inline-control {
  flex: 1;
  min-width: 0;
}

.so-detail__logistics-expanded {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--el-border-color-lighter);
}

.so-detail__time-range {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.so-detail__time-range-arrow {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.so-detail__logistics-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  align-self: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  background: none;
  font-size: 16px;
  color: var(--el-text-color-secondary);
  cursor: pointer;

  &:hover {
    color: var(--el-color-primary);
  }
}

.so-detail__kv {
  margin: 0;
  min-width: 0;

  &.is-full {
    grid-column: 1 / -1;
  }

  dt {
    margin: 0 0 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  dd {
    margin: 0;
    font-size: 14px;
    color: var(--el-text-color-primary);
    word-break: break-word;

    &.is-amount {
      font-size: 20px;
      font-weight: 700;
      color: #c2410c;
    }

    &.is-loss {
      color: var(--el-color-danger);
      font-weight: 600;
    }
  }
}

.so-detail__express-select {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.so-detail__express-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.so-detail__line-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.so-detail__line-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}

.so-detail__line-edit-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.so-detail__line-edit-card {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}

.so-detail__line-edit-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 12px;
  flex: 1;
  min-width: 0;
}

.so-detail__line-edit-field {
  min-width: 0;

  &--qty,
  &--amount {
    grid-column: span 1;
  }
}

.so-detail__line-edit-label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.so-detail__line-edit-remove {
  flex-shrink: 0;
  margin-top: 20px;
}

.so-detail__line-add {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 10px;
  border: 1px dashed var(--el-border-color);
  border-radius: 10px;
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-color-primary);
  cursor: pointer;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
  }
}

.so-detail__line-hint {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.so-detail__line-main {
  flex: 1;
  min-width: 0;
}

.so-detail__line-name {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  line-height: 1.4;
  color: var(--el-text-color-primary);
}

.so-detail__line-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.so-detail__line-tag {
  border: none;
}

.so-detail__line-qty {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.so-detail__line-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
}

.so-detail__line-finance {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  font-size: 12px;
}

.so-detail__line-profit {
  font-weight: 700;
  color: #16a34a;
}

.so-detail__line-loss {
  color: var(--el-color-danger);
}

.so-detail__settlement {
  padding: 14px 16px;
  border-radius: 12px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
}

.so-detail__settlement-main {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 8px;
}

.so-detail__settlement-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.so-detail__settlement-profit {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  color: #16a34a;

  &.is-negative {
    color: var(--el-color-danger);
  }
}

.so-detail__settlement-metrics {
  display: flex;
  align-items: stretch;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed var(--el-border-color-lighter);
}

.so-detail__settlement-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 14px;

  &:first-child {
    padding-left: 0;
  }

  &:last-child {
    padding-right: 0;
  }
}

.so-detail__settlement-divider {
  flex-shrink: 0;
  width: 1px;
  background: var(--el-border-color-lighter);
}

.so-detail__settlement-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 0;
  min-width: 0;

  .so-detail__inline-label {
    flex: 1;
    min-width: 0;
  }
}

.so-detail__settlement-value {
  flex-shrink: 0;
  width: 72px;
  text-align: left;
  font-size: 14px;
  color: var(--el-text-color-primary);

  &.is-loss {
    color: var(--el-color-danger);
    font-weight: 600;
  }

  &.is-highlight {
    font-weight: 700;
    color: #c2410c;
  }
}

.so-detail__remark-box {
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  font-size: 14px;
  line-height: 1.6;
  color: var(--el-text-color-regular);

  p {
    margin: 0;

    & + p {
      margin-top: 10px;
      padding-top: 10px;
      border-top: 1px dashed var(--el-border-color-lighter);
    }
  }
}

.so-detail__remark-label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);

  &--buyer {
    color: #4ade80;
  }

  &--seller {
    color: #fca5a5;
  }
}

.so-detail__remark-edit {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.so-detail__remark-field {
  min-width: 0;
}

.so-detail__field-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  line-height: 1.4;
}

.so-detail__empty {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-placeholder);
}

.so-detail__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.so-detail__footer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
