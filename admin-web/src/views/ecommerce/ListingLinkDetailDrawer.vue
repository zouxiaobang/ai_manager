<template>
  <el-drawer
    v-model="visible"
    :title="t('ecommerce.listingLink.detailTitle')"
    size="960px"
    destroy-on-close
    class="link-detail-drawer"
    @open="loadDetail"
  >
    <div v-loading="loading" class="link-detail">
      <template v-if="detail">
        <header class="link-detail__hero">
          <div class="link-detail__hero-main">
            <img
              v-if="platformIconSrc"
              :src="platformIconSrc"
              :alt="detail.platformName || ''"
              class="link-detail__platform-icon"
            />
            <h2 class="link-detail__title">{{ detail.name }}</h2>
            <p class="link-detail__subtitle">
              <span>{{ detail.shopName || '—' }}</span>
              <span class="link-detail__subtitle-sep">·</span>
              <span>{{ formatDateTime(detail.listingTime) }}</span>
            </p>
          </div>
        </header>

        <div class="link-detail__metrics">
          <div class="link-metric-card is-blue">
            <div class="link-metric-card__icon"><el-icon><Goods /></el-icon></div>
            <div class="link-metric-card__body">
              <div class="link-metric-card__label">{{ t('ecommerce.listingLink.skuCount') }}</div>
              <div class="link-metric-card__value">{{ summary.skuCount }}</div>
              <div class="link-metric-card__hint">{{ t('ecommerce.listingLink.skuCountUnit') }}</div>
            </div>
          </div>
          <div class="link-metric-card" :class="summary.totalProfit >= 0 ? 'is-green' : 'is-red'">
            <div class="link-metric-card__icon"><el-icon><Money /></el-icon></div>
            <div class="link-metric-card__body">
              <div class="link-metric-card__label">{{ t('ecommerce.listingLink.totalProfit') }}</div>
              <div class="link-metric-card__value"><CnyAmount variant="display" :value="summary.totalProfit" /></div>
              <div
                v-if="summary.totalProfitRate != null"
                class="link-metric-card__hint"
                :class="summary.totalProfitRate >= 0 ? 'is-success' : 'is-danger'"
              >
                {{ t('ecommerce.listingLink.totalProfitRate', { rate: formatPercent(summary.totalProfitRate) }) }}
                <span v-if="summary.totalProfitRate > 0" class="link-metric-card__arrow">↑</span>
              </div>
            </div>
          </div>
          <div class="link-metric-card" :class="minProfitCardTone">
            <div class="link-metric-card__icon"><el-icon><Warning /></el-icon></div>
            <div class="link-metric-card__body">
              <div class="link-metric-card__label">{{ t('ecommerce.listingLink.minProfitSku') }}</div>
              <div class="link-metric-card__value">{{ formatLossMoney(summary.minProfit) }}</div>
              <div
                v-if="summary.minProfitSkuName && summary.minProfitRate != null"
                class="link-metric-card__hint is-danger"
              >
                {{ t('ecommerce.listingLink.skuProfitRate', {
                  name: summary.minProfitSkuName,
                  rate: formatPercent(summary.minProfitRate),
                }) }}
              </div>
            </div>
          </div>
          <div class="link-metric-card is-orange">
            <div class="link-metric-card__icon"><el-icon><Calendar /></el-icon></div>
            <div class="link-metric-card__body">
              <div class="link-metric-card__label">{{ t('ecommerce.listingLink.listingDays') }}</div>
              <div class="link-metric-card__value">{{ summary.listingDays ?? '—' }}</div>
              <div v-if="summary.listingDate" class="link-metric-card__hint">
                {{ t('ecommerce.listingLink.listedSince', { date: summary.listingDate }) }}
              </div>
            </div>
          </div>
        </div>

        <section class="link-detail__info-card">
          <h3 class="link-detail__section-title">{{ t('ecommerce.listingLink.linkInfoSection') }}</h3>
          <dl class="link-detail__info-grid">
            <div class="link-detail__info-row">
              <dt>{{ t('ecommerce.listingLink.product') }}</dt>
              <dd>
                <div v-if="linkedProducts.length" class="link-detail__product-tags">
                  <el-tag
                    v-for="item in linkedProducts"
                    :key="item.key"
                    size="small"
                    effect="light"
                    type="success"
                    round
                    class="link-detail__product-tag"
                  >
                    {{ item.name }}
                  </el-tag>
                </div>
                <span v-else>—</span>
              </dd>
            </div>
            <div class="link-detail__info-row">
              <dt>{{ t('ecommerce.listingLink.platformUrl') }}</dt>
              <dd>
                <a
                  v-if="detail.platformUrl"
                  :href="detail.platformUrl"
                  target="_blank"
                  rel="noopener"
                  class="link-detail__url"
                >
                  {{ detail.platformUrl }}
                </a>
                <span v-else>—</span>
              </dd>
            </div>
            <div v-if="detail.remark?.trim()" class="link-detail__info-row">
              <dt>{{ t('ecommerce.listingLink.remark') }}</dt>
              <dd>{{ detail.remark }}</dd>
            </div>
          </dl>
        </section>

        <el-collapse v-model="formulaExpanded" class="link-detail__formula-collapse">
          <el-collapse-item :title="t('ecommerce.listingLink.costFormula')" name="formula">
            <p v-if="detail.costFormula" class="link-detail__formula-text">{{ detail.costFormula }}</p>
            <p v-else class="link-detail__formula-text">{{ t('ecommerce.listingLink.calcHint') }}</p>
          </el-collapse-item>
        </el-collapse>

        <section class="link-detail__sku-section">
          <h3 class="link-detail__section-title">{{ t('ecommerce.listingLink.skuPricingSection') }}</h3>
          <div v-if="detail.skus?.length" class="link-detail__sku-grid">
            <article
              v-for="(sku, index) in detail.skus"
              :key="sku.id ?? sku.skuCodes ?? index"
              class="link-sku-card"
              :class="skuCardTone(sku)"
            >
              <span
                class="link-sku-card__stock"
                :class="{ 'link-sku-card__stock--alert': skuStockAlert(sku) }"
              >
                {{ t('ecommerce.listingLink.stockBadge', { count: skuStockTotal(sku) }) }}
              </span>

              <div class="link-sku-card__summary">
                <div class="link-sku-card__thumb">
                  <img
                    v-if="skuImageUrl(sku) && !brokenImageKeys.has(skuImageKey(sku, index))"
                    :src="skuImageUrl(sku)"
                    :alt="sku.skuName || sku.skuCodes"
                    class="link-sku-card__thumb-img"
                    @error="markImageBroken(sku, index)"
                  />
                  <div v-else class="link-sku-card__thumb-placeholder">
                    <el-icon><Picture /></el-icon>
                  </div>
                </div>

                <div class="link-sku-card__meta">
                  <div class="link-sku-card__code">{{ primarySkuCode(sku) }}</div>
                  <div class="link-sku-card__name">{{ sku.skuName || '—' }}</div>
                  <div class="link-sku-card__sub">{{ skuCodesSubline(sku) }}</div>
                </div>

                <div class="link-sku-card__gauge">
                  <el-progress
                    type="circle"
                    :percentage="profitRingPercent(sku)"
                    :width="84"
                    :stroke-width="5"
                    :color="profitRingColor(sku)"
                    :show-text="false"
                    class="link-sku-card__gauge-ring"
                  />
                  <div class="profit-ring__inner">
                    <div class="profit-ring__rate">{{ formatPercent(calcProfitRate(sku.profit ?? null, calcNetRevenue(sku))) }}</div>
                    <div class="profit-ring__amount" :class="profitAmountClass(sku)">
                      <CnyAmount :value="sku.profit" />
                    </div>
                    <div class="profit-ring__label">{{ t('ecommerce.listingLink.profitRate') }}</div>
                  </div>
                </div>
              </div>

              <div class="link-sku-card__pricing">
                <div class="link-sku-card__amounts">
                  <div class="amount-cell is-cost">
                    <span class="amount-cell__label">{{ t('ecommerce.listingLink.costPrice') }}</span>
                    <span class="amount-cell__value"><CnyAmount :value="sku.costPrice" /></span>
                  </div>
                  <div class="amount-cell">
                    <span class="amount-cell__label">{{ t('ecommerce.listingLink.minSetAmountLine') }}</span>
                    <span class="amount-cell__value"><CnyAmount :value="sku.minSetAmount" /></span>
                  </div>
                  <div class="amount-cell is-actual">
                    <span class="amount-cell__label">{{ t('ecommerce.listingLink.actualSetAmountShort') }}</span>
                    <span class="amount-cell__value"><CnyAmount :value="sku.actualSetAmount" /></span>
                  </div>
                </div>
                <div class="price-scale">
                  <div class="price-scale__track">
                    <div class="price-scale__half is-left">
                      <div class="price-scale__seg is-cost" :style="{ width: priceScale(sku).costInLeft + '%' }" />
                      <div class="price-scale__seg is-gap" :style="{ width: priceScale(sku).gapInLeft + '%' }" />
                    </div>
                    <div
                      class="price-scale__half is-right"
                      :class="{ 'is-risk-mode': priceScale(sku).belowMin }"
                    >
                      <div
                        v-if="priceScale(sku).rightSeg > 0"
                        class="price-scale__seg"
                        :class="priceScale(sku).profitClass"
                        :style="{ width: priceScale(sku).rightSeg + '%' }"
                      />
                    </div>
                    <div
                      class="price-scale__marker"
                      :title="t('ecommerce.listingLink.minSetAmountLine')"
                    />
                  </div>
                </div>
              </div>
            </article>
          </div>
          <el-empty v-else :description="t('ecommerce.listingLink.noSkus')" :image-size="72" />
        </section>
      </template>
    </div>

    <template v-if="detail" #footer>
      <div class="link-detail__footer">
        <el-button type="primary" size="large" class="link-detail__edit-btn" @click="emitEdit">
          <el-icon><Edit /></el-icon>
          {{ t('ecommerce.listingLink.editLink') }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Calendar, Edit, Goods, Money, Picture, Warning } from '@element-plus/icons-vue'
import { fetchListingLink, type EcListingLink, type EcListingLinkSku } from '@/api/ecommerce/listingLink'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'
import CnyAmount from '@/components/CnyAmount.vue'
import { formatMoney } from '@/utils/formatMoney'
import { fetchProduct } from '@/api/ecommerce/product'
import { formatDate, formatDateTime } from '@/utils/date'
import { resolvePlatformIcon } from '@/utils/platformVisual'
import { resolveListingLinkSkuImageName } from '@/utils/listingLinkSkuImage'

const props = defineProps<{ modelValue: boolean; linkId?: number | null }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; edit: [id: number] }>()

const { t } = useI18n()
const loading = ref(false)
const detail = ref<EcListingLink | null>(null)
const productImageMap = ref<Record<number, string | undefined>>({})
const skuImageMap = ref<Record<string, string | undefined>>({})
const brokenImageKeys = ref<Set<string>>(new Set())
const formulaExpanded = ref<string[]>([])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const linkedProducts = computed(() => {
  const d = detail.value
  if (!d) return []
  if (d.products?.length) {
    return d.products.map((p) => ({
      key: `product-${p.productId}`,
      name: p.productName?.trim() || String(p.productId),
    }))
  }
  const names = d.productNames?.trim()
  if (!names) return []
  return names
    .split(/[、,，]/)
    .map((name) => name.trim())
    .filter(Boolean)
    .map((name, index) => ({ key: `name-${index}-${name}`, name }))
})

const platformIconSrc = computed(() => {
  const name = detail.value?.platformName?.trim()
  if (!name) return ''
  return resolvePlatformIcon(name)
})

const summary = computed(() => {
  const skus = detail.value?.skus ?? []
  const profits = skus.map((s) => s.profit).filter((v): v is number => v != null)
  const totalProfit = profits.reduce((sum, v) => sum + Number(v), 0)

  let totalNetRevenue = 0
  let hasNetRevenue = false
  for (const sku of skus) {
    const netRevenue = calcNetRevenue(sku)
    if (netRevenue != null && netRevenue > 0) {
      totalNetRevenue += netRevenue
      hasNetRevenue = true
    }
  }
  const totalProfitRate =
    hasNetRevenue && profits.length ? Number(((totalProfit / totalNetRevenue) * 100).toFixed(2)) : null

  let minProfit: number | null = null
  let minProfitSkuName = ''
  let minProfitRate: number | null = null
  for (const sku of skus) {
    if (sku.profit == null) continue
    const profit = Number(sku.profit)
    if (minProfit == null || profit < minProfit) {
      minProfit = profit
      minProfitSkuName = sku.skuName?.trim() || sku.skuCodes?.trim() || ''
      minProfitRate = calcProfitRate(profit, calcNetRevenue(sku))
    }
  }

  const listingTime = detail.value?.listingTime
  return {
    skuCount: skus.length,
    totalProfit: profits.length ? totalProfit : 0,
    totalProfitRate,
    minProfit,
    minProfitSkuName,
    minProfitRate,
    listingDays: calcListingDays(listingTime),
    listingDate: listingTime ? formatDate(listingTime) : '',
  }
})

const minProfitCardTone = computed(() => {
  const profit = summary.value.minProfit
  if (profit == null) return 'is-orange'
  if (profit < 0) return 'is-red'
  return 'is-orange'
})

watch(
  () => props.linkId,
  () => {
    if (visible.value && props.linkId) loadDetail()
  },
)

async function loadDetail() {
  if (!props.linkId) return
  loading.value = true
  try {
    detail.value = await fetchListingLink(props.linkId)
    productImageMap.value = {}
    skuImageMap.value = {}
    brokenImageKeys.value = new Set()
    formulaExpanded.value = []
    if (detail.value) await enrichProductImages(detail.value)
  } finally {
    loading.value = false
  }
}

function linkProductIds(link: EcListingLink) {
  return (link.products ?? []).map((p) => p.productId).filter((id) => id != null)
}

function parseSkuCodes(sku: EcListingLinkSku) {
  const codes = sku.skuCodes?.split(',').map((c) => c.trim()).filter(Boolean) ?? []
  if (codes.length) return codes
  return sku.skuName?.trim() ? [sku.skuName.trim()] : []
}

async function enrichProductImages(link: EcListingLink) {
  const ids = linkProductIds(link)
  if (!ids.length) return

  const entries = await Promise.all(
    ids.map(async (id) => {
      try {
        const product = await fetchProduct(id)
        return { id, imageName: product.imageName?.trim() || undefined, skus: product.skus ?? [] }
      } catch {
        return { id, imageName: undefined, skus: [] }
      }
    }),
  )

  const nextProduct = { ...productImageMap.value }
  const nextSku = { ...skuImageMap.value }
  for (const entry of entries) {
    nextProduct[entry.id] = entry.imageName
    for (const productSku of entry.skus) {
      const code = productSku.skuCode?.trim()
      if (code) nextSku[code] = productSku.imageName?.trim() || undefined
    }
  }
  productImageMap.value = nextProduct
  skuImageMap.value = nextSku
}

function resolveSkuImageName(sku: EcListingLinkSku): string | undefined {
  const link = detail.value
  if (!link) return undefined
  return resolveListingLinkSkuImageName(
    parseSkuCodes(sku),
    linkProductIds(link),
    skuImageMap.value,
    productImageMap.value,
  )
}

function skuImageUrl(sku: EcListingLinkSku) {
  return getEcommerceImageUrl(resolveSkuImageName(sku))
}

function skuImageKey(sku: EcListingLinkSku, index: number) {
  return `${sku.skuCodes || sku.skuName || index}`
}

function markImageBroken(sku: EcListingLinkSku, index: number) {
  const key = skuImageKey(sku, index)
  if (brokenImageKeys.value.has(key)) return
  brokenImageKeys.value = new Set([...brokenImageKeys.value, key])
}

function primarySkuCode(sku: EcListingLinkSku) {
  return parseSkuCodes(sku)[0] || sku.skuCodes || '—'
}

function skuCodesSubline(sku: EcListingLinkSku) {
  const codes = sku.skuCodes?.trim() || parseSkuCodes(sku).join(', ')
  if (!codes) return ''
  return t('ecommerce.listingLink.skuCodesLabel', { codes })
}

function skuStockTotal(sku: EcListingLinkSku) {
  return (sku.inventories ?? []).reduce((sum, inv) => sum + (inv.quantity ?? 0), 0)
}

function skuStockAlert(sku: EcListingLinkSku) {
  return (sku.inventories ?? []).some((inv) => inv.alertActive)
}

function profitRingPercent(sku: EcListingLinkSku) {
  const rate = calcProfitRate(sku.profit ?? null, calcNetRevenue(sku))
  if (rate == null) return 0
  return Math.min(100, Math.abs(rate))
}

function profitRingColor(sku: EcListingLinkSku) {
  const profit = sku.profit ?? 0
  if (profit < 0 || sku.pricingRisk === 'NEGATIVE_PROFIT') return '#ef4444'
  if (sku.pricingRisk === 'BELOW_MIN') return '#f59e0b'
  return '#22c55e'
}

function profitAmountClass(sku: EcListingLinkSku) {
  const profit = sku.profit ?? 0
  if (profit < 0 || sku.pricingRisk === 'NEGATIVE_PROFIT') return 'is-danger'
  if (sku.pricingRisk === 'BELOW_MIN') return 'is-warning'
  return 'is-success'
}

function priceScale(sku: EcListingLinkSku) {
  const cost = Number(sku.costPrice ?? 0)
  const min = Number(sku.minSetAmount ?? 0)
  const actual = Number(sku.actualSetAmount ?? 0)
  const profit = sku.profit ?? 0
  const belowMin = actual < min || sku.pricingRisk === 'BELOW_MIN'

  const costInLeft = min > 0
    ? Math.min(100, Math.max(0, (cost / min) * 100))
    : 35
  const gapInLeft = Math.max(0, 100 - costInLeft)

  let rightSeg = 0
  if (min > 0 && actual > min) {
    rightSeg = Math.min(100, ((actual - min) / min) * 100)
  } else if (min > 0 && actual < min) {
    rightSeg = Math.min(100, ((min - actual) / min) * 100)
  }

  return {
    costInLeft,
    gapInLeft,
    rightSeg,
    belowMin,
    profitClass: belowMin || profit < 0 ? 'is-risk' : 'is-profit',
  }
}

function calcListingDays(listingTime?: string | null) {
  if (!listingTime?.trim()) return null
  const start = new Date(listingTime.trim().slice(0, 10))
  if (Number.isNaN(start.getTime())) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  start.setHours(0, 0, 0, 0)
  const diff = Math.floor((today.getTime() - start.getTime()) / 86400000)
  return Math.max(0, diff)
}

function calcNetRevenue(sku: EcListingLinkSku): number | null {
  if (sku.actualSetAmount == null) return null
  const setAmount = Number(sku.actualSetAmount)
  const coupon = Number(sku.couponAmount ?? 0)
  const discount = Number(sku.discountPct ?? 100)
  if (discount <= 0 || discount > 100 || setAmount < coupon) return null
  return Number(((setAmount - coupon) * (discount / 100)).toFixed(2))
}

function calcProfitRate(profit: number | null, netRevenue: number | null): number | null {
  if (profit == null || netRevenue == null || netRevenue <= 0) return null
  return Number(((profit / netRevenue) * 100).toFixed(2))
}

function formatPercent(rate?: number | null) {
  if (rate == null) return '—'
  return `${Number(rate).toFixed(2)}%`
}

function formatLossMoney(v?: number | null) {
  if (v == null) return '—'
  return formatMoney(Number(v))
}

function skuCardTone(sku: EcListingLinkSku) {
  if (sku.pricingRisk === 'BELOW_MIN' || sku.pricingRisk === 'NEGATIVE_PROFIT') return 'is-risk'
  if ((sku.profit ?? 0) < 0) return 'is-risk'
  if ((sku.profit ?? 0) > 0) return 'is-profit'
  return ''
}

function emitEdit() {
  if (detail.value?.id) {
    emit('edit', detail.value.id)
  }
}
</script>

<style scoped lang="scss">
.link-detail {
  min-height: 160px;
  padding-bottom: 8px;
}

.link-detail-drawer {
  :deep(.el-drawer__footer) {
    display: flex;
    justify-content: center;
    padding: 16px 24px 20px;
  }
}

.link-detail__footer {
  display: flex;
  justify-content: center;
  width: 100%;
}

.link-detail__edit-btn {
  min-width: 220px;
  height: 44px;
  font-size: 15px;
  font-weight: 600;

  .el-icon {
    margin-right: 6px;
    font-size: 16px;
  }
}

.link-detail__hero {
  margin-bottom: 16px;
}

.link-detail__platform-icon {
  display: block;
  width: 32px;
  height: 32px;
  margin-bottom: 10px;
  border-radius: 8px;
  object-fit: cover;
}

.link-detail__title {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.35;
  color: var(--el-text-color-primary);
}

.link-detail__subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.link-detail__subtitle-sep {
  margin: 0 6px;
}

.link-detail__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.link-metric-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 88px;
  padding: 14px 12px;
  border-radius: 12px;
  border: 1px solid transparent;

  &.is-blue {
    background: #eff6ff;
    border-color: #bfdbfe;

    .link-metric-card__icon {
      background: #3b82f6;
    }

    .link-metric-card__value {
      color: #2563eb;
    }
  }

  &.is-green {
    background: #f0fdf4;
    border-color: #bbf7d0;

    .link-metric-card__icon {
      background: #22c55e;
    }

    .link-metric-card__value {
      color: #16a34a;
    }
  }

  &.is-red {
    background: #fef2f2;
    border-color: #fecaca;

    .link-metric-card__icon {
      background: #ef4444;
    }

    .link-metric-card__value {
      color: #dc2626;
    }
  }

  &.is-orange {
    background: #fffbeb;
    border-color: #fde68a;

    .link-metric-card__icon {
      background: #f59e0b;
    }

    .link-metric-card__value {
      color: var(--el-text-color-primary);
    }
  }
}

.link-metric-card__icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  color: #fff;
  font-size: 22px;
}

.link-metric-card__label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.link-metric-card__value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.link-metric-card__unit {
  margin-left: 2px;
  font-size: 13px;
  font-weight: 600;
}

.link-metric-card__hint {
  margin-top: 4px;
  font-size: 11px;
  color: #6b7280;
  line-height: 1.4;

  &.is-success {
    color: #16a34a;
  }

  &.is-danger {
    color: #dc2626;
  }
}

.link-metric-card__arrow {
  margin-left: 2px;
  font-weight: 700;
}

.link-detail__info-card {
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-fill-color-blank);
}

.link-detail__section-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.link-detail__info-grid {
  margin: 0;
}

.link-detail__info-row {
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.5;

  &:last-child {
    margin-bottom: 0;
  }

  dt {
    margin: 0;
    color: var(--el-text-color-secondary);
  }

  dd {
    margin: 0;
    color: var(--el-text-color-primary);
    word-break: break-all;
  }
}

.link-detail__product-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.link-detail__product-tag {
  max-width: 100%;
  height: auto;
  padding: 4px 10px;
  line-height: 1.4;
  white-space: normal;
  word-break: break-word;
}

.link-detail__url {
  color: var(--el-color-primary);
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.link-detail__formula-collapse {
  margin-bottom: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  overflow: hidden;
  background: var(--el-fill-color-blank);

  :deep(.el-collapse) {
    border: none;
  }

  :deep(.el-collapse-item__header) {
    height: 44px;
    padding: 0 14px;
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    background: var(--el-fill-color-blank);
    border-bottom: none;
  }

  :deep(.el-collapse-item__wrap) {
    border-top: 1px solid var(--el-border-color-extra-light);
  }

  :deep(.el-collapse-item__content) {
    padding: 10px 14px 12px;
  }
}

.link-detail__formula-text {
  margin: 0;
  font-size: 12px;
  line-height: 1.65;
  color: var(--el-text-color-secondary);
  word-break: break-word;
}

.link-detail__sku-section {
  margin-bottom: 8px;
}

.link-detail__sku-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-items: stretch;
}

.link-sku-card {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 248px;
  padding: 16px 16px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
  background: var(--el-fill-color-blank);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.06);
  }

  &.is-profit {
    border-color: #d1fae5;
  }

  &.is-risk {
    border-color: #fecaca;
  }
}

.link-sku-card__stock {
  position: absolute;
  top: 14px;
  right: 14px;
  z-index: 2;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.2;
  background: #ecfdf5;
  color: #059669;

  &--alert {
    background: #fef2f2;
    color: #dc2626;
  }
}

.link-sku-card__summary {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 96px;
  padding-right: 68px;
  margin-bottom: 14px;
}

.link-sku-card__thumb {
  flex-shrink: 0;
  width: 72px;
  height: 72px;
  border-radius: 10px;
  overflow: hidden;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
}

.link-sku-card__thumb-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.link-sku-card__thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 24px;
  color: var(--el-text-color-placeholder);
}

.link-sku-card__meta {
  flex: 1;
  min-width: 0;
  min-height: 66px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.link-sku-card__code {
  font-size: 15px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.link-sku-card__name {
  font-size: 13px;
  color: var(--el-text-color-primary);
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 38px;
}

.link-sku-card__sub {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-height: 15px;
}

.link-sku-card__gauge {
  position: relative;
  flex-shrink: 0;
  width: 84px;
  height: 84px;
}

.link-sku-card__gauge-ring {
  :deep(.el-progress-circle__track) {
    stroke: #e5e7eb;
  }
}

.profit-ring__inner {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  z-index: 1;
}

.profit-ring__rate {
  font-size: 13px;
  font-weight: 700;
  line-height: 1.15;
  color: var(--el-text-color-primary);
  font-variant-numeric: tabular-nums;
}

.profit-ring__amount {
  margin-top: 2px;
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;

  &.is-success {
    color: #16a34a;
  }

  &.is-danger {
    color: #dc2626;
  }

  &.is-warning {
    color: #d97706;
  }
}

.profit-ring__label {
  margin-top: 2px;
  font-size: 10px;
  color: var(--el-text-color-secondary);
}

.link-sku-card__pricing {
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid var(--el-border-color-extra-light);
}

.link-sku-card__amounts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.amount-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 0;
  text-align: center;
}

.amount-cell__label {
  min-height: 30px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  line-height: 1.35;
}

.amount-cell__value {
  font-size: 15px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--el-text-color-primary);
}

.amount-cell.is-cost {
  .amount-cell__label,
  .amount-cell__value {
    color: #3b82f6;
  }
}

.amount-cell.is-actual {
  .amount-cell__label,
  .amount-cell__value {
    color: #16a34a;
  }
}

.is-risk .amount-cell.is-actual {
  .amount-cell__label,
  .amount-cell__value {
    color: #dc2626;
  }
}

.price-scale {
  padding: 0 1px;
}

.price-scale__track {
  position: relative;
  display: flex;
  height: 8px;
  border-radius: 999px;
  overflow: visible;
  background: #e2e8f0;
}

.price-scale__half {
  display: flex;
  width: 50%;
  height: 100%;
  min-width: 0;

  &.is-right {
    &.is-risk-mode {
      justify-content: flex-end;
    }
  }
}

.price-scale__seg {
  height: 100%;
  min-width: 0;
  transition: width 0.2s ease;

  &.is-cost {
    background: #3b82f6;
    border-radius: 999px 0 0 999px;
  }

  &.is-gap {
    background: #cbd5e1;
  }

  &.is-profit {
    background: #22c55e;
    border-radius: 0 999px 999px 0;
  }

  &.is-risk {
    background: #f87171;
    border-radius: 0 999px 999px 0;
  }
}

.price-scale__marker {
  position: absolute;
  top: -4px;
  bottom: -4px;
  left: 50%;
  width: 0;
  border-left: 2px solid #111827;
  transform: translateX(-1px);
  pointer-events: none;
  z-index: 2;
}

@media (max-width: 900px) {
  .link-detail__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .link-detail__sku-grid {
    grid-template-columns: 1fr;
  }
}
</style>
