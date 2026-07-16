<template>
  <V2Page>
    <div class="v2-ec">
      <div v-if="loading" class="v2-ec-settlement-loading">
        <span>加载月结数据中...</span>
      </div>

      <div v-else-if="error" class="v2-ec-settlement-empty">
        <span class="v2-ec-settlement-empty__icon">!</span>
        <span class="v2-ec-settlement-empty__title">数据加载失败</span>
        <span class="v2-ec-settlement-empty__desc">{{ error }}</span>
      </div>

      <div v-else-if="!shops.length" class="v2-ec-settlement-empty">
        <span class="v2-ec-settlement-empty__icon">-</span>
        <span class="v2-ec-settlement-empty__title">暂无月结数据</span>
        <span class="v2-ec-settlement-empty__desc">当前月份还没有导入订单或尚未进行月结统计</span>
      </div>

      <template v-else>
        <div style="margin-bottom: 16px;">
          <V2MonthPicker v-model="orderMonth" :disabled="loading" />
        </div>

        <div class="v2-ec-stats">
          <div class="v2-ec-stat-card" style="background: #eff6ff;">
            <div class="v2-ec-stat-card__info">
              <div class="v2-ec-stat-card__value" style="color: #2563eb;">{{ formatMoney(aggregated.totalRevenue) }}</div>
              <div class="v2-ec-stat-card__label">总营业额</div>
            </div>
          </div>
          <div class="v2-ec-stat-card" style="background: #f0fdf4;">
            <div class="v2-ec-stat-card__info">
              <div class="v2-ec-stat-card__value" style="color: #22c55e;">{{ formatMoney(aggregated.estimatedTotalProfit) }}</div>
              <div class="v2-ec-stat-card__label">预估利润</div>
            </div>
          </div>
          <div class="v2-ec-stat-card" style="background: #fffbeb;">
            <div class="v2-ec-stat-card__info">
              <div class="v2-ec-stat-card__value" style="color: #f59e0b;">{{ formatMoney(aggregated.actualTotalProfit) }}</div>
              <div class="v2-ec-stat-card__label">实际利润</div>
            </div>
          </div>
          <div class="v2-ec-stat-card" style="background: #f8fafc;">
            <div class="v2-ec-stat-card__info">
              <div class="v2-ec-stat-card__value" style="color: #ef4444;">
                <span style="color: #22c55e;">{{ aggregated.includedOrderCount }}</span>
                <span style="color: #cbd5e1; font-weight: 400; font-size: 12px;"> / </span>
                <span style="color: #f59e0b;">{{ aggregated.pendingOrderCount }}</span>
                <span style="color: #cbd5e1; font-weight: 400; font-size: 12px;"> / </span>
                <span style="color: #94a3b8;">{{ aggregated.excludedOrderCount }}</span>
              </div>
              <div class="v2-ec-stat-card__label">订单总数（已统计/待处理/已排除）</div>
            </div>
          </div>
        </div>

        <div class="v2-ec-settlement-profit-card">
          <div class="v2-ec-settlement-profit-card__head">利润对比</div>
          <div class="v2-ec-settlement-profit-card__bars">
            <div class="v2-ec-settlement-profit-bar">
              <span class="v2-ec-settlement-profit-bar__label">预估</span>
              <div class="v2-ec-settlement-profit-bar__track">
                <div class="v2-ec-settlement-profit-bar__fill v2-ec-settlement-profit-bar__fill--est" :style="{ width: estProfitPercent + '%' }" />
              </div>
              <span class="v2-ec-settlement-profit-bar__value">{{ formatMoney(aggregated.estimatedTotalProfit) }}</span>
            </div>
            <div class="v2-ec-settlement-profit-bar">
              <span class="v2-ec-settlement-profit-bar__label">实际</span>
              <div class="v2-ec-settlement-profit-bar__track">
                <div class="v2-ec-settlement-profit-bar__fill v2-ec-settlement-profit-bar__fill--actual" :style="{ width: actualProfitPercent + '%' }" />
              </div>
              <span class="v2-ec-settlement-profit-bar__value">{{ formatMoney(aggregated.actualTotalProfit) }}</span>
            </div>
          </div>
          <div class="v2-ec-settlement-profit-card__diff">
            <span class="v2-ec-settlement-profit-card__diff-label">预估 - 实际差额</span>
            <span class="v2-ec-settlement-profit-card__diff-value" :class="profitDiff >= 0 ? 'is-down' : 'is-up'">
              {{ profitDiff >= 0 ? '▼ 多估 ' : '▲ 少估 ' }}{{ formatMoney(Math.abs(profitDiff)) }}
            </span>
          </div>
        </div>

        <div class="v2-ec-settlement-shop-section">
          <div class="v2-ec-section-title">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--wr-text, #333)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
            </svg>
            <span>各店汇总</span>
            <span style="font-size: 12px; color: var(--wr-muted, #999); font-weight: 400; margin-left: auto;">{{ shops.length }} 家店铺</span>
          </div>

          <div class="v2-ec-settlement-shop-list">
            <div v-for="shop in shops" :key="shop.shopId" class="v2-ec-settlement-shop-card">
              <div class="v2-ec-settlement-shop-card__head">
                <span class="v2-ec-settlement-shop-card__platform" :style="{ background: getPlatformColor(shop) }">
                  {{ getPlatformLabel(shop) }}
                </span>
                <span class="v2-ec-settlement-shop-card__name">{{ shop.shopName || `#${shop.shopId}` }}</span>
                <span class="v2-ec-settlement-shop-card__profit" :class="(shop.estimatedTotalProfit ?? 0) >= 0 ? 'is-up' : 'is-down'">
                  {{ (shop.estimatedTotalProfit ?? 0) >= 0 ? '↑' : '↓' }}{{ profitRate(shop) }}%
                </span>
              </div>

              <div class="v2-ec-settlement-shop-card__metrics">
                <div class="v2-ec-settlement-shop-card__metric">
                  <span class="v2-ec-settlement-shop-card__metric-label">营收</span>
                  <span class="v2-ec-settlement-shop-card__metric-value">{{ formatMoney(shop.totalRevenue) }}</span>
                </div>
                <div class="v2-ec-settlement-shop-card__metric">
                  <span class="v2-ec-settlement-shop-card__metric-label">利润</span>
                  <span class="v2-ec-settlement-shop-card__metric-value is-profit">{{ formatMoney(shop.actualTotalProfit) }}</span>
                </div>
                <div class="v2-ec-settlement-shop-card__metric">
                  <span class="v2-ec-settlement-shop-card__metric-label">订单</span>
                  <span class="v2-ec-settlement-shop-card__metric-value">
                    <span class="v2-ec-settlement-shop-card__order-num is-included">{{ shop.includedOrderCount ?? 0 }}</span>
                    <span class="v2-ec-settlement-shop-card__order-sep">/</span>
                    <span class="v2-ec-settlement-shop-card__order-num is-pending">{{ shop.pendingOrderCount ?? 0 }}</span>
                    <span class="v2-ec-settlement-shop-card__order-sep">/</span>
                    <span class="v2-ec-settlement-shop-card__order-num is-excluded">{{ shop.excludedOrderCount ?? 0 }}</span>
                  </span>
                </div>
              </div>

              <div class="v2-ec-settlement-shop-card__divider"></div>

              <div class="v2-ec-settlement-shop-card__cost-row">
                <div class="v2-ec-settlement-shop-card__cost-item">
                  <span class="v2-ec-settlement-shop-card__cost-label">预估成本</span>
                  <span class="v2-ec-settlement-shop-card__cost-value">{{ formatMoney(shop.estimatedTotalCost) }}</span>
                </div>
                <span class="v2-ec-settlement-shop-card__cost-sep">|</span>
                <div class="v2-ec-settlement-shop-card__cost-item">
                  <span class="v2-ec-settlement-shop-card__cost-label">实际成本</span>
                  <span class="v2-ec-settlement-shop-card__cost-value">{{ formatMoney(shop.actualTotalCost) }}</span>
                </div>
              </div>

              <div v-if="shop.settlementStatus" class="v2-ec-settlement-shop-card__status-row">
                <span class="v2-ec-settlement-shop-card__status-label">结算状态</span>
                <span class="v2-ec-settlement-shop-card__status-tag" :class="'status--' + (shop.settlementStatus || '').toLowerCase()">
                  {{ shop.settlementStatus }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </V2Page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import V2Page from '@/mobile-v2/components/V2Page.vue'
import V2MonthPicker from '@/mobile-v2/components/V2MonthPicker.vue'
import {
  fetchMonthlySettlement,
  type MonthlySettlementShopSummary,
  type MonthlySettlementResult,
} from '@/api/ecommerce/monthlySettlement.ts'

import './styles/v2-ecommerce.scss'

const loading = ref(true)
const error = ref('')
const result = ref<MonthlySettlementResult | null>(null)

function formatMonth(d: Date) {
  const y = d.getFullYear()
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  return `${y}-${m}`
}

const orderMonth = ref(formatMonth(new Date()))

const shops = computed(() => result.value?.shops ?? [])

const aggregated = computed(() => {
  const list = shops.value
  return {
    totalRevenue: list.reduce((s, shop) => s + (shop.totalRevenue ?? 0), 0),
    estimatedTotalCost: list.reduce((s, shop) => s + (shop.estimatedTotalCost ?? 0), 0),
    actualTotalCost: list.reduce((s, shop) => s + (shop.actualTotalCost ?? 0), 0),
    estimatedTotalProfit: list.reduce((s, shop) => s + (shop.estimatedTotalProfit ?? 0), 0),
    actualTotalProfit: list.reduce((s, shop) => s + (shop.actualTotalProfit ?? 0), 0),
    includedOrderCount: list.reduce((s, shop) => s + (shop.includedOrderCount ?? 0), 0),
    excludedOrderCount: list.reduce((s, shop) => s + (shop.excludedOrderCount ?? 0), 0),
    pendingOrderCount: list.reduce((s, shop) => s + (shop.pendingOrderCount ?? 0), 0),
  }
})

const maxProfitRef = computed(() => {
  const max = Math.max(
    aggregated.value.estimatedTotalProfit,
    aggregated.value.actualTotalProfit,
    1,
  )
  return max
})

const estProfitPercent = computed(() =>
  Math.max(4, (aggregated.value.estimatedTotalProfit / maxProfitRef.value) * 100),
)

const actualProfitPercent = computed(() =>
  Math.max(4, (aggregated.value.actualTotalProfit / maxProfitRef.value) * 100),
)

const profitDiff = computed(() =>
  aggregated.value.estimatedTotalProfit - aggregated.value.actualTotalProfit,
)

function formatMoney(value?: number | null): string {
  const v = value ?? 0
  if (Math.abs(v) >= 10000) {
    return '¥' + (v / 10000).toFixed(1) + '万'
  }
  return '¥' + Math.round(v).toLocaleString('zh-CN')
}

function profitRate(shop: MonthlySettlementShopSummary): string {
  const revenue = shop.totalRevenue ?? 0
  if (revenue === 0) return '0.0'
  const rate = ((shop.actualTotalProfit ?? 0) / revenue) * 100
  return rate.toFixed(1)
}

function getPlatformColor(shop: MonthlySettlementShopSummary): string {
  const name = shop.shopName ?? ''
  if (name.includes('淘宝') || name.includes('淘')) return '#ff6a00'
  if (name.includes('京东')) return '#e1251b'
  if (name.includes('抖音') || name.includes('抖')) return '#00b96b'
  if (name.includes('拼多多') || name.includes('拼') || name.includes('多多')) return '#ff2d1b'
  const colors = ['#6366f1', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316']
  return colors[(shop.shopId ?? 0) % colors.length]
}

function getPlatformLabel(shop: MonthlySettlementShopSummary): string {
  const name = shop.shopName ?? ''
  if (name.includes('淘宝') || name.includes('淘')) return '淘宝'
  if (name.includes('京东')) return '京东'
  if (name.includes('抖音') || name.includes('抖')) return '抖音'
  if (name.includes('拼多多') || name.includes('拼') || name.includes('多多')) return '拼多多'
  return '店铺'
}

async function loadData(month: string) {
  loading.value = true
  error.value = ''
  try {
    const data = await fetchMonthlySettlement(month)
    result.value = data
  } catch (e: any) {
    error.value = e?.message || '请求失败，请检查网络连接'
    result.value = null
  } finally {
    loading.value = false
  }
}

watch(orderMonth, () => {
  result.value = null
  void loadData(orderMonth.value)
})

onMounted(() => {
  void loadData(orderMonth.value)
})
</script>

<style scoped lang="scss">
.v2-ec-settlement-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  font-size: 15px;
  color: var(--wr-muted, #999);
}

.v2-ec-settlement-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;

  &__icon {
    font-size: 48px;
    margin-bottom: 12px;
    opacity: 0.6;
  }

  &__title {
    font-size: 18px;
    color: var(--wr-text, #333);
    margin-bottom: 8px;
  }

  &__desc {
    font-size: 14px;
    color: var(--wr-muted, #999);
    max-width: 220px;
    line-height: 1.5;
  }
}

.v2-ec-settlement-profit-card {
  background: var(--wr-card, #ffffff);
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  &__head {
    font-size: 15px;
    font-weight: 600;
    color: var(--wr-text, #333);
    margin-bottom: 12px;
  }

  &__bars {
    margin-bottom: 10px;
  }

  &__diff {
    display: flex;
    align-items: center;
    gap: 8px;
    padding-top: 10px;
    border-top: 2px dashed var(--wr-border, #e8ecef);
  }

  &__diff-label {
    font-size: 12px;
    color: var(--wr-muted, #999);
  }

  &__diff-value {
    font-size: 13px;
    font-weight: 700;

    &.is-down { color: #ef4444; }
    &.is-up { color: #22c55e; }
  }
}

.v2-ec-settlement-profit-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;

  &:last-child { margin-bottom: 0; }

  &__label {
    font-size: 12px;
    color: var(--wr-text-secondary, #666);
    width: 36px;
    flex-shrink: 0;
  }

  &__track {
    flex: 1;
    height: 10px;
    background: #f1f5f9;
    border-radius: 999px;
    overflow: hidden;
  }

  &__fill {
    height: 100%;
    border-radius: 999px;
    transition: width 0.4s ease;

    &--est { background: linear-gradient(90deg, #2563eb, #60a5fa); }
    &--actual { background: linear-gradient(90deg, #22c55e, #4ade80); }
  }

  &__value {
    font-size: 12px;
    font-weight: 700;
    color: var(--wr-text, #333);
    width: 56px;
    text-align: right;
    white-space: nowrap;
  }
}

.v2-ec-settlement-shop-section {
  margin-bottom: 24px;
}

.v2-ec-settlement-shop-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.v2-ec-settlement-shop-card {
  background: var(--wr-card, #ffffff);
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
  }

  &__platform {
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 10px;
    color: white;
    font-weight: 600;
    flex-shrink: 0;
  }

  &__name {
    font-size: 15px;
    color: var(--wr-text, #333);
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__profit {
    font-size: 12px;
    font-weight: 700;
    flex-shrink: 0;

    &.is-up { color: #22c55e; }
    &.is-down { color: #ef4444; }
  }

  &__metrics {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    margin-bottom: 12px;
  }

  &__metric {
    text-align: center;
  }

  &__metric-label {
    display: block;
    font-size: 10px;
    color: var(--wr-muted, #999);
    margin-bottom: 4px;
  }

  &__metric-value {
    display: block;
    font-size: 13px;
    font-weight: 700;
    color: var(--wr-text, #333);

    &.is-profit { color: #22c55e; }
  }

  &__order-num {
    font-weight: 700;
    &.is-included { color: #22c55e; }
    &.is-pending { color: #f59e0b; }
    &.is-excluded { color: var(--wr-muted, #999); }
  }

  &__order-sep {
    color: #d1d5db;
    font-size: 11px;
    margin: 0 1px;
  }

  &__divider {
    height: 0;
    border-top: 2px dashed var(--wr-border, #e8ecef);
    margin-bottom: 10px;
  }

  &__cost-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__cost-item {
    flex: 1;
    text-align: center;
  }

  &__cost-label {
    display: block;
    font-size: 10px;
    color: var(--wr-muted, #999);
    margin-bottom: 2px;
  }

  &__cost-value {
    display: block;
    font-size: 12px;
    font-weight: 600;
    color: var(--wr-text-secondary, #666);
  }

  &__cost-sep {
    color: #d1d5db;
    font-size: 12px;
    flex-shrink: 0;
  }

  &__status-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 10px;
    padding-top: 10px;
    border-top: 1px solid var(--wr-border, #e8ecef);
  }

  &__status-label {
    font-size: 12px;
    color: var(--wr-muted, #999);
  }

  &__status-tag {
    font-size: 11px;
    font-weight: 600;
    padding: 2px 10px;
    border-radius: 999px;

    &.status--settled {
      color: #22c55e;
      background: #f0fdf4;
    }

    &.status--pending {
      color: #f59e0b;
      background: #fffbeb;
    }

    &.status--failed {
      color: #ef4444;
      background: #fef2f2;
    }
  }
}
</style>
