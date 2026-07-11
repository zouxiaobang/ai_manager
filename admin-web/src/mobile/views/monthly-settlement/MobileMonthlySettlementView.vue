<template>
  <!-- 移动端月结统计页主容器 -->
  <div class="mobile-monthly-settlement">
    <!-- 顶部导航栏：返回按钮 + 页面标题 -->
    <div class="ms-header">
      <div class="ms-header__left">
        <!-- 返回按钮：返回上一页 -->
        <MobileDoodleChip
          tag="button" type="button"
          shape="pill" color="#2563eb"
          class="ms-header__back"
          @click="goBack"
        >
          <span>←</span>
        </MobileDoodleChip>
        <h1 class="ms-header__title">月结统计</h1>
      </div>
    </div>

    <main class="ms-content">
      <!-- 加载中状态：数据加载时展示 -->
      <div v-if="loading" class="ms-loading">
        <div class="ms-loading__dots">
          <span class="ms-loading__dot"></span><span class="ms-loading__dot"></span><span class="ms-loading__dot"></span>
        </div>
        <p class="ms-loading__text">加载月结数据中...</p>
      </div>

      <!-- 错误状态：数据加载失败时展示 -->
      <div v-else-if="error" class="ms-empty">
        <span class="ms-empty__icon">😵</span>
        <p class="ms-empty__title">数据加载失败</p>
        <p class="ms-empty__desc">{{ error }}</p>
      </div>

      <!-- 无数据状态：暂无月结数据时展示 -->
      <div v-else-if="!shops.length" class="ms-empty">
        <span class="ms-empty__icon">📭</span>
        <p class="ms-empty__title">暂无月结数据</p>
        <p class="ms-empty__desc">当前月份还没有导入订单或尚未进行月结统计</p>
      </div>

      <!-- 数据内容区域：有数据时展示 -->
      <template v-else>
        <!-- 月份切换器：左右切换月份 -->
        <div class="ms-month-bar">
          <div class="month-picker">
            <button class="month-arrow" @click="shiftMonth(-1)" :disabled="loading">‹</button>
            <MobileMonthPicker v-model="orderMonth" class="month-label" />
            <button class="month-arrow" @click="shiftMonth(1)" :disabled="loading">›</button>
          </div>
        </div>

        <!-- 汇总数据卡片网格：总营业额/预估利润/实际利润/订单总数 -->
        <div class="ms-summary-grid">
          <!-- 总营业额卡片 -->
          <SchemeADoodleFrame :seed="1" color="#2563eb" class="ms-summary-card" sketch :shadow="false">
            <div class="ms-summary-card__inner">
              <div class="ms-summary-card__icon">💰</div>
              <div class="ms-summary-card__body">
                <div class="ms-summary-card__label">总营业额</div>
                <div class="ms-summary-card__value">{{ formatMoney(aggregated.totalRevenue) }}</div>
              </div>
            </div>
          </SchemeADoodleFrame>
          <!-- 预估利润卡片 -->
          <SchemeADoodleFrame :seed="2" color="#22c55e" class="ms-summary-card" sketch :shadow="false">
            <div class="ms-summary-card__inner">
              <div class="ms-summary-card__icon">📈</div>
              <div class="ms-summary-card__body">
                <div class="ms-summary-card__label">预估利润</div>
                <div class="ms-summary-card__value">{{ formatMoney(aggregated.estimatedTotalProfit) }}</div>
              </div>
            </div>
          </SchemeADoodleFrame>
          <!-- 实际利润卡片 -->
          <SchemeADoodleFrame :seed="3" color="#f59e0b" class="ms-summary-card" sketch :shadow="false">
            <div class="ms-summary-card__inner">
              <div class="ms-summary-card__icon">✅</div>
              <div class="ms-summary-card__body">
                <div class="ms-summary-card__label">实际利润</div>
                <div class="ms-summary-card__value">{{ formatMoney(aggregated.actualTotalProfit) }}</div>
              </div>
            </div>
          </SchemeADoodleFrame>
          <!-- 订单总数卡片：已统计/待处理/已排除 -->
          <SchemeADoodleFrame :seed="4" color="#ef4444" class="ms-summary-card" sketch :shadow="false">
            <div class="ms-summary-card__inner">
              <div class="ms-summary-card__icon">📋</div>
              <div class="ms-summary-card__body">
                <div class="ms-summary-card__label">订单总数</div>
                <div class="ms-summary-card__value">
                  <span class="ms-summary-card__count is-included">{{ aggregated.includedOrderCount }}</span>
                  <span class="ms-summary-card__sep">/</span>
                  <span class="ms-summary-card__count is-pending">{{ aggregated.pendingOrderCount }}</span>
                  <span class="ms-summary-card__sep">/</span>
                  <span class="ms-summary-card__count is-excluded">{{ aggregated.excludedOrderCount }}</span>
                </div>
              </div>
            </div>
          </SchemeADoodleFrame>
        </div>

        <!-- 利润对比卡片：预估与实际利润对比 + 差额 -->
        <SchemeADoodleFrame color="#6366f1" class="ms-profit-compare" sketch>
          <div class="ms-profit-compare__inner">
            <div class="ms-profit-compare__head">
              <span class="ms-profit-compare__title">📊 利润对比</span>
            </div>
            <!-- 利润对比进度条区域 -->
            <div class="ms-profit-compare__bars">
              <!-- 预估利润进度条 -->
              <div class="ms-profit-bar">
                <span class="ms-profit-bar__label">预估</span>
                <div class="ms-profit-bar__track">
                  <div class="ms-profit-bar__fill ms-profit-bar__fill--est" :style="{ width: estProfitPercent + '%' }" />
                </div>
                <span class="ms-profit-bar__value">{{ formatMoney(aggregated.estimatedTotalProfit) }}</span>
              </div>
              <!-- 实际利润进度条 -->
              <div class="ms-profit-bar">
                <span class="ms-profit-bar__label">实际</span>
                <div class="ms-profit-bar__track">
                  <div class="ms-profit-bar__fill ms-profit-bar__fill--actual" :style="{ width: actualProfitPercent + '%' }" />
                </div>
                <span class="ms-profit-bar__value">{{ formatMoney(aggregated.actualTotalProfit) }}</span>
              </div>
            </div>
            <!-- 预估与实际差额展示 -->
            <div class="ms-profit-compare__diff">
              <span class="ms-profit-compare__diff-label">预估 - 实际差额</span>
              <span class="ms-profit-compare__diff-value" :class="profitDiff >= 0 ? 'is-down' : 'is-up'">
                {{ profitDiff >= 0 ? '▼ 多估 ' : '▲ 少估 ' }}{{ formatMoney(Math.abs(profitDiff)) }}
              </span>
            </div>
          </div>
        </SchemeADoodleFrame>

        <!-- 各店铺汇总区域：按店铺展示月结数据 -->
        <div class="ms-shop-section">
          <div class="ms-shop-section__head">
            <span class="ms-shop-section__title">🏪 各店汇总</span>
            <span class="ms-shop-section__count">{{ shops.length }} 家店铺</span>
          </div>

          <!-- 店铺卡片列表 -->
          <div class="ms-shop-list">
            <!-- 店铺月结卡片：展示单个店铺的月结数据 -->
            <SchemeADoodleFrame
              v-for="shop in shops"
              :key="shop.shopId"
              class="ms-shop-card"
              :seed="shop.shopId"
              :color="getCardColor(shop)"
              sketch
            >
              <div class="ms-shop-card__inner">
                <!-- 店铺卡片头部：平台标签 + 店铺名称 + 利润率 -->
                <div class="ms-shop-card__head">
                  <span
                    class="ms-shop-card__platform"
                    :style="{ background: getPlatformColor(shop) }"
                  >
                    {{ getPlatformLabel(shop) }}
                  </span>
                  <span class="ms-shop-card__name">{{ shop.shopName || `#${shop.shopId}` }}</span>
                  <span
                    class="ms-shop-card__profit"
                    :class="(shop.estimatedTotalProfit ?? 0) >= 0 ? 'is-up' : 'is-down'"
                  >
                    {{ (shop.estimatedTotalProfit ?? 0) >= 0 ? '↑' : '↓' }}{{ profitRate(shop) }}%
                  </span>
                </div>

                <!-- 店铺核心指标：营收/利润/订单数 -->
                <div class="ms-shop-card__metrics">
                  <div class="ms-shop-card__metric">
                    <span class="ms-shop-card__metric-label">营收</span>
                    <span class="ms-shop-card__metric-value">{{ formatMoney(shop.totalRevenue) }}</span>
                  </div>
                  <div class="ms-shop-card__metric">
                    <span class="ms-shop-card__metric-label">利润</span>
                    <span class="ms-shop-card__metric-value is-profit">{{ formatMoney(shop.actualTotalProfit) }}</span>
                  </div>
                  <div class="ms-shop-card__metric">
                    <span class="ms-shop-card__metric-label">订单</span>
                    <span class="ms-shop-card__metric-value">
                      <span class="ms-shop-card__order-num is-included">{{ shop.includedOrderCount ?? 0 }}</span>
                      <span class="ms-shop-card__order-sep">/</span>
                      <span class="ms-shop-card__order-num is-pending">{{ shop.pendingOrderCount ?? 0 }}</span>
                      <span class="ms-shop-card__order-sep">/</span>
                      <span class="ms-shop-card__order-num is-excluded">{{ shop.excludedOrderCount ?? 0 }}</span>
                    </span>
                  </div>
                </div>

                <!-- 分隔线 -->
                <div class="ms-shop-card__divider"></div>

                <!-- 成本行：预估成本 + 实际成本 -->
                <div class="ms-shop-card__cost-row">
                  <div class="ms-shop-card__cost-item">
                    <span class="ms-shop-card__cost-label">预估成本</span>
                    <span class="ms-shop-card__cost-value">{{ formatMoney(shop.estimatedTotalCost) }}</span>
                  </div>
                  <div class="ms-shop-card__cost-sep">|</div>
                  <div class="ms-shop-card__cost-item">
                    <span class="ms-shop-card__cost-label">实际成本</span>
                    <span class="ms-shop-card__cost-value">{{ formatMoney(shop.actualTotalCost) }}</span>
                  </div>
                </div>
              </div>
            </SchemeADoodleFrame>
          </div>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
/**
 * 移动端月结统计视图组件
 * 功能说明：
 * - 月结统计的移动端入口页面
 * - 支持按月份切换查看月结数据
 * - 顶部展示汇总统计（总营业额、预估利润、实际利润、订单总数）
 * - 利润对比区域：预估与实际利润对比及差额
 * - 各店铺汇总列表：按店铺展示营收、利润、订单、成本等数据
 * - 支持加载中、错误、无数据三种状态展示
 * - 根据店铺名称自动识别平台并显示对应颜色
 * - 使用手绘风格UI设计
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import MobileMonthPicker from '@/mobile/components/MobileMonthPicker.vue'
import {
  fetchMonthlySettlement,
  type MonthlySettlementShopSummary,
  type MonthlySettlementResult,
} from '@/api/ecommerce/monthlySettlement.ts'

const router = useRouter()

const loading = ref(true) // 加载状态
const error = ref('') // 错误信息
const result = ref<MonthlySettlementResult | null>(null) // 月结数据结果

// 格式化月份为 YYYY-MM 格式
function formatMonth(d: Date) {
  const y = d.getFullYear()
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  return `${y}-${m}`
}

const orderMonth = ref(formatMonth(new Date())) // 当前选中的月份

// 店铺列表计算属性
const shops = computed(() => result.value?.shops ?? [])

// 汇总数据计算属性：聚合所有店铺的数据
const aggregated = computed(() => {
  const list = shops.value
  return {
    totalRevenue: list.reduce((s, shop) => s + (shop.totalRevenue ?? 0), 0), // 总营业额
    estimatedTotalCost: list.reduce((s, shop) => s + (shop.estimatedTotalCost ?? 0), 0), // 预估总成本
    actualTotalCost: list.reduce((s, shop) => s + (shop.actualTotalCost ?? 0), 0), // 实际总成本
    estimatedTotalProfit: list.reduce((s, shop) => s + (shop.estimatedTotalProfit ?? 0), 0), // 预估总利润
    actualTotalProfit: list.reduce((s, shop) => s + (shop.actualTotalProfit ?? 0), 0), // 实际总利润
    includedOrderCount: list.reduce((s, shop) => s + (shop.includedOrderCount ?? 0), 0), // 已统计订单数
    excludedOrderCount: list.reduce((s, shop) => s + (shop.excludedOrderCount ?? 0), 0), // 已排除订单数
    pendingOrderCount: list.reduce((s, shop) => s + (shop.pendingOrderCount ?? 0), 0), // 待处理订单数
  }
})

// 利润对比最大参考值：取预估和实际利润中的较大值
const maxProfitRef = computed(() => {
  const max = Math.max(
    aggregated.value.estimatedTotalProfit,
    aggregated.value.actualTotalProfit,
    1,
  )
  return max
})

// 预估利润占比百分比
const estProfitPercent = computed(() =>
  Math.max(4, (aggregated.value.estimatedTotalProfit / maxProfitRef.value) * 100),
)

// 实际利润占比百分比
const actualProfitPercent = computed(() =>
  Math.max(4, (aggregated.value.actualTotalProfit / maxProfitRef.value) * 100),
)

// 利润差额：预估 - 实际
const profitDiff = computed(() =>
  aggregated.value.estimatedTotalProfit - aggregated.value.actualTotalProfit,
)

// 格式化金额：大于1万显示万单位
function formatMoney(value?: number | null): string {
  const v = value ?? 0
  if (Math.abs(v) >= 10000) {
    return '¥' + (v / 10000).toFixed(1) + '万'
  }
  return '¥' + Math.round(v).toLocaleString('zh-CN')
}

// 计算店铺利润率
function profitRate(shop: MonthlySettlementShopSummary): string {
  const revenue = shop.totalRevenue ?? 0
  if (revenue === 0) return '0.0'
  const rate = ((shop.actualTotalProfit ?? 0) / revenue) * 100
  return rate.toFixed(1)
}

// 根据店铺名称获取平台颜色
function getPlatformColor(shop: MonthlySettlementShopSummary): string {
  const name = shop.shopName ?? ''
  if (name.includes('淘宝') || name.includes('淘')) return '#ff6a00'
  if (name.includes('京东')) return '#e1251b'
  if (name.includes('抖音') || name.includes('抖')) return '#00b96b'
  if (name.includes('拼多多') || name.includes('拼') || name.includes('多多')) return '#ff2d1b'
  const colors = ['#6366f1', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316']
  return colors[(shop.shopId ?? 0) % colors.length]
}

// 根据店铺名称获取卡片边框颜色
function getCardColor(shop: MonthlySettlementShopSummary): string {
  const name = shop.shopName ?? ''
  if (name.includes('淘宝') || name.includes('淘')) return '#2563eb'
  if (name.includes('京东')) return '#e1251b'
  if (name.includes('抖音') || name.includes('抖')) return '#00b96b'
  if (name.includes('拼多多') || name.includes('拼') || name.includes('多多')) return '#f97316'
  const colors = ['#6366f1', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316']
  return colors[(shop.shopId ?? 0) % colors.length]
}

// 根据店铺名称获取平台标签文字
function getPlatformLabel(shop: MonthlySettlementShopSummary): string {
  const name = shop.shopName ?? ''
  if (name.includes('淘宝') || name.includes('淘')) return '淘宝'
  if (name.includes('京东')) return '京东'
  if (name.includes('抖音') || name.includes('抖')) return '抖音'
  if (name.includes('拼多多') || name.includes('拼') || name.includes('多多')) return '拼多多'
  return '店铺'
}

// 加载月结数据
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

// 切换月份：向前/向后切换
function shiftMonth(delta: number) {
  const [y, m] = orderMonth.value.split('-').map(Number)
  const d = new Date(y, m - 1 + delta, 1)
  orderMonth.value = formatMonth(d)
}

// 返回上一页
function goBack() {
  router.back()
}

// 监听月份变化：切换月份时重新加载数据
watch(orderMonth, () => {
  result.value = null
  void loadData(orderMonth.value)
})

// 组件挂载：加载当前月份月结数据
onMounted(() => {
  void loadData(orderMonth.value)
})
</script>

<style scoped lang="scss">
.mobile-monthly-settlement {
  min-height: 100vh;
  background: #fff;
  display: flex;
  flex-direction: column;
  font-family: 'ZCOOL KuaiLe', sans-serif;
}

/* ── Header ── */
.ms-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: max(16px, env(safe-area-inset-top)) 16px 12px;
  background: #fff;

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__back {
    width: 36px;
    height: 36px;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: #2563eb;
    font-weight: 700;
    cursor: pointer;
    background: #faf8f5;
    transition: transform 0.2s ease;

    :deep(.sa-doodle-frame__body) {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      padding: 0;
    }

    &:active {
      transform: scale(0.9);
    }
  }

  &__title {
    font-size: 24px;
    margin: 0;
    color: #1e293b;
  }
}

/* ── Loading ── */
.ms-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;

  &__dots {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;
  }

  &__dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #2563eb;
    animation: dotPulse 1.2s ease-in-out infinite;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }

  &__text {
    font-size: 15px;
    color: #94a3b8;
    margin: 0;
  }
}

@keyframes dotPulse {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* ── Empty / Error ── */
.ms-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;

  &__icon { font-size: 48px; margin-bottom: 12px; }

  &__title {
    font-size: 18px;
    color: #1e293b;
    margin: 0 0 8px;
  }

  &__desc {
    font-size: 14px;
    color: #94a3b8;
    margin: 0;
    max-width: 220px;
    line-height: 1.5;
  }
}

/* ── Content ── */
.ms-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

/* ── Month Bar ── */
.ms-month-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.month-picker {
  display: flex;
  align-items: center;
  gap: 6px;
  background: white;
  border: 2px solid #e2e8f0;
  border-radius: 999px;
  padding: 4px 2px;
  flex: 1;
}

.month-arrow {
  width: 30px;
  height: 30px;
  border: none;
  background: transparent;
  font-size: 18px;
  font-weight: 700;
  color: #2563eb;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;

  &:hover {
    background: #eff6ff;
  }

  &:disabled {
    color: #cbd5e1;
    cursor: not-allowed;
  }
}

.month-label {
  flex: 1;

  :deep(.mobile-month-picker__trigger) {
    border: none;
    background: transparent;
    padding: 2px 4px;
    font-size: 15px;
    cursor: pointer;
    justify-content: center;
    width: 100%;
  }

  :deep(.mobile-month-picker__trigger-text) {
    min-width: 80px;
    text-align: center;
  }

  :deep(.mobile-month-picker__arrow) {
    display: none;
  }
}

/* ── Summary Grid ── */
.ms-summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 16px;
}

.ms-summary-card {
  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.ms-summary-card__inner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 12px;
}

.ms-summary-card__icon {
  font-size: 22px;
  flex-shrink: 0;
}

.ms-summary-card__body {
  flex: 1;
  min-width: 0;
}

.ms-summary-card__label {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 2px;
}

.ms-summary-card__value {
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.2;
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.ms-summary-card__count {
  font-size: 14px;
  font-weight: 700;

  &.is-included { color: #22c55e; }
  &.is-pending { color: #f59e0b; }
  &.is-excluded { color: #94a3b8; }
}

.ms-summary-card__sep {
  color: #cbd5e1;
  font-weight: 400;
  font-size: 12px;
}

/* ── Profit Compare ── */
.ms-profit-compare {
  margin-bottom: 16px;
  padding: 24px;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.ms-profit-compare__inner {
  padding: 14px;
}

.ms-profit-compare__head {
  margin-bottom: 12px;
}

.ms-profit-compare__title {
  font-size: 15px;
  color: #1e293b;
}

.ms-profit-compare__diff {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 2px dashed #e2e8f0;
}

.ms-profit-compare__diff-label {
  font-size: 12px;
  color: #94a3b8;
}

.ms-profit-compare__diff-value {
  font-size: 13px;
  font-weight: 700;

  &.is-down { color: #ef4444; }
  &.is-up { color: #22c55e; }
}

.ms-profit-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  &:last-child { margin-bottom: 0; }

  &__label {
    font-size: 12px;
    color: #64748b;
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
    color: #1e293b;
    width: 56px;
    text-align: right;
    white-space: nowrap;
  }
}

/* ── Shop Section ── */
.ms-shop-section {
  margin-bottom: 24px;

  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    padding: 0 4px;
  }

  &__title {
    font-size: 16px;
    color: #1e293b;
    margin: 0;
  }

  &__count {
    font-size: 12px;
    color: #94a3b8;
    margin-left: auto;
  }
}

.ms-shop-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ms-shop-card {
  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.ms-shop-card__inner {
  padding: 24px;
}

.ms-shop-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.ms-shop-card__platform {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 10px;
  color: white;
  font-weight: 600;
  flex-shrink: 0;
}

.ms-shop-card__name {
  font-size: 15px;
  color: #1e293b;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ms-shop-card__profit {
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;

  &.is-up { color: #22c55e; }
  &.is-down { color: #ef4444; }
}

.ms-shop-card__metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.ms-shop-card__metric {
  text-align: center;
}

.ms-shop-card__metric-label {
  display: block;
  font-size: 10px;
  color: #94a3b8;
  margin-bottom: 2px;
}

.ms-shop-card__metric-value {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  margin-top: 8px;

  &.is-profit { color: #22c55e; }
}

.ms-shop-card__order-num {
  font-weight: 700;
  &.is-included { color: #22c55e; }
  &.is-pending { color: #f59e0b; }
  &.is-excluded { color: #94a3b8; }
}

.ms-shop-card__order-sep {
  color: #d1d5db;
  font-size: 11px;
  margin: 0 1px;
}

.ms-shop-card__divider {
  height: 0;
  border-top: 2px dashed #e2e8f0;
  margin-bottom: 10px;
}

.ms-shop-card__cost-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ms-shop-card__cost-item {
  flex: 1;
  text-align: center;
}

.ms-shop-card__cost-label {
  display: block;
  font-size: 10px;
  color: #94a3b8;
  margin-bottom: 2px;
}

.ms-shop-card__cost-value {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.ms-shop-card__cost-sep {
  color: #d1d5db;
  font-size: 12px;
  flex-shrink: 0;
}
</style>
