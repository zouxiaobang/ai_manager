import type { EcListingLinkSku, EcListingLinkSkuInventory } from '@/api/ecommerce/listingLink'

/** 上架链接 SKU 行（组件表单行与接口结构一致） */
export type SkuRow = EcListingLinkSku

/** 价格刻度条展示结构 */
export interface PriceScaleDisplay {
  costInLeft: number
  gapInLeft: number
  rightSeg: number
  belowMin: boolean
  profitClass: 'is-risk' | 'is-profit'
}

/** 百分比展示：空值占位符，否则保留两位小数 */
export function formatPercent(rate?: number | null) {
  if (rate == null) return '—'
  return `${Number(rate).toFixed(2)}%`
}

/**
 * 实收净额 = (实际设置价 - 优惠券) × 折扣比例。
 * 折扣越界或优惠券大于售价视为无效输入。
 */
export function calcNetRevenue(row: SkuRow): number | null {
  if (row.actualSetAmount == null) return null
  const setAmount = Number(row.actualSetAmount)
  const coupon = Number(row.couponAmount ?? 0)
  const discount = Number(row.discountPct ?? 100)
  if (discount <= 0 || discount > 100 || setAmount < coupon) return null
  return Number(((setAmount - coupon) * (discount / 100)).toFixed(2))
}

/** 利润率 = 利润 / 净额 × 100%，净额缺失或非正时无法计算 */
export function calcProfitRate(profit: number | null, netRevenue: number | null): number | null {
  if (profit == null || netRevenue == null || netRevenue <= 0) return null
  return Number(((profit / netRevenue) * 100).toFixed(2))
}

/**
 * 解析 SKU 编码：逗号分隔去空；无编码时回退到名称。
 * 名称也空时返回空数组。
 */
export function parseSkuCodes(sku: SkuRow): string[] {
  const codes = sku.skuCodes?.split(',').map((c) => c.trim()).filter(Boolean) ?? []
  if (codes.length) return codes
  return sku.skuName?.trim() ? [sku.skuName.trim()] : []
}

/** 主 SKU 编码：首个解析编码，缺失时回退原始串，再缺失回退占位符 */
export function primarySkuCode(row: SkuRow) {
  return parseSkuCodes(row)[0] || row.skuCodes || '—'
}

/** SKU 库存行：未配置时视为空列表 */
export function rowInventories(row: SkuRow): EcListingLinkSkuInventory[] {
  return row.inventories ?? []
}

/** 总库存 = 各库存行数量之和 */
export function skuStockTotal(row: SkuRow) {
  return rowInventories(row).reduce((sum, inv) => sum + (inv.quantity ?? 0), 0)
}

/** 是否存在触线告警的库存行 */
export function skuStockAlert(row: SkuRow) {
  return rowInventories(row).some((inv) => inv.alertActive)
}

/** 利润环百分比：无可算利润率归 0，绝对值封顶 100 */
export function profitRingPercent(row: SkuRow) {
  const rate = calcProfitRate(row.profit ?? null, calcNetRevenue(row))
  if (rate == null) return 0
  return Math.min(100, Math.abs(rate))
}

/** 利润环颜色：负利润/亏损红，低于最低价橙，其余绿 */
export function profitRingColor(row: SkuRow) {
  const profit = row.profit ?? 0
  if (profit < 0 || row.pricingRisk === 'NEGATIVE_PROFIT') return '#ef4444'
  if (row.pricingRisk === 'BELOW_MIN') return '#f59e0b'
  return '#22c55e'
}

/** 利润金额展示态：亏损红、低于最低价黄、其余绿 */
export function profitAmountClass(row: SkuRow) {
  const profit = row.profit ?? 0
  if (profit < 0 || row.pricingRisk === 'NEGATIVE_PROFIT') return 'is-danger'
  if (row.pricingRisk === 'BELOW_MIN') return 'is-warning'
  return 'is-success'
}

/**
 * 价格刻度条：成本相对最低价的位置、最低价与实际价的偏差段、
 * 是否低于最低价及风险态。
 */
export function priceScale(row: SkuRow): PriceScaleDisplay {
  const cost = Number(row.costPrice ?? 0)
  const min = Number(row.minSetAmount ?? 0)
  const actual = Number(row.actualSetAmount ?? 0)
  const profit = row.profit ?? 0
  const belowMin = actual < min || row.pricingRisk === 'BELOW_MIN'

  const costInLeft = min > 0 ? Math.min(100, Math.max(0, (cost / min) * 100)) : 35
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

/** SKU 卡片色调：风险优先，其次按利润正负 */
export function skuCardTone(row: SkuRow) {
  if (row.pricingRisk === 'BELOW_MIN' || row.pricingRisk === 'NEGATIVE_PROFIT') return 'is-risk'
  if ((row.profit ?? 0) < 0) return 'is-risk'
  if ((row.profit ?? 0) > 0) return 'is-profit'
  return ''
}

/** SKU 行定价汇总：数量、平均利润、平均利润率 */
export function summarizeSkuPricing(skus: SkuRow[]) {
  const profits = skus.map((s) => s.profit).filter((v): v is number => v != null)
  const rates: number[] = []
  for (const sku of skus) {
    const rate = calcProfitRate(sku.profit ?? null, calcNetRevenue(sku))
    if (rate != null) rates.push(rate)
  }
  return {
    count: skus.length,
    avgProfit: profits.length ? Number((profits.reduce((a, b) => a + b, 0) / profits.length).toFixed(2)) : null,
    avgProfitRate: rates.length ? Number((rates.reduce((a, b) => a + b, 0) / rates.length).toFixed(2)) : null,
  }
}

/** 编辑态 SKU 图片缓存 key：按编码/名称与行号唯一化 */
export function formSkuImageKey(row: SkuRow, index: number) {
  return `edit-${row.skuCodes || row.skuName || index}`
}
