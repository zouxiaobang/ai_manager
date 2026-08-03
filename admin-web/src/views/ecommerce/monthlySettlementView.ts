import { formatDateTime } from '@/utils/date'
import { formatMoney } from '@/utils/formatMoney'
import { resolveExpressIconMetaFromStation } from '@/utils/expressVisual'
import { resolveShopIconMeta } from '@/utils/shopVisual'
import type { ExpressBillRecord, MonthlySettlementShopSummary } from '@/api/ecommerce/monthlySettlement'
import type { EcExpressStation } from '@/api/ecommerce/express'
import type { EcSalesOrderMonthlyOverview, ShopImportStatus } from '@/api/ecommerce/salesOrder'
import type { EcShop } from '@/api/ecommerce/shop'

/** 预备清单里的快递账单匹配卡片 */
export interface PrepExpressBillCard {
  id: number
  name: string
  iconSrc: string
  isCustomAvatar: boolean
  matched: number
  total: number
  gapCount?: number
}

export type PrepTone = 'success' | 'warning' | 'danger' | 'muted'
export type PrepStatusTagType = 'success' | 'warning' | 'danger' | 'info'

/** 结算预备清单条目（语义/展示结构，由组件注入 i18n 文案与跳转动作） */
export interface PrepTask {
  key: string
  title: string
  desc: string
  descHighlight?: string
  tone: PrepTone
  statusTag?: { label: string; type: PrepStatusTagType }
  lastOperationTime?: string
  lastTimeLabelKey?: string
  expressBillCards?: PrepExpressBillCard[]
  subItems?: string[]
  action?: () => void
}

type MaxProfitOrder = NonNullable<MonthlySettlementShopSummary['maxProfitOrder']>

/** 最大利润订单 + 归属店铺展示信息 */
export type MaxProfitDisplay = Omit<MaxProfitOrder, 'shopId' | 'shopName'> & {
  shopId: number
  shopName: string
}

/** 状态选项结构（statusLabel 第二参） */
export interface StatusOption {
  value: string
  label: string
}

/** 快递账单去重 key：其他快递合并为 other，否则优先站台 id，兜底名称/记录 id */
export function stationRecordKey(record: ExpressBillRecord): string {
  if (record.otherExpress) return 'other'
  return String(record.expressStationId ?? record.expressStationName ?? record.id)
}

/** 按账单月份过滤快递导入记录（月份按 trim 后精确匹配） */
export function filterExpressBillRecordsByMonth(records: ExpressBillRecord[], month: string) {
  const monthKey = month.trim()
  return records.filter((record) => (record.billMonth ?? '').trim() === monthKey)
}

/**
 * 由某月快递账单记录构建匹配卡片列表，按站台分组。
 *
 * <p>getStation 由调用方注入（组件持有站台缓存 Map 的查询函数），
 * 保持本函数可脱离组件状态单测。</p>
 */
export function buildExpressBillCards(
  records: ExpressBillRecord[],
  month: string,
  getStation: (stationId: number) => EcExpressStation | undefined,
): PrepExpressBillCard[] {
  const scopedRecords = filterExpressBillRecordsByMonth(records, month)
  const grouped = new Map<string, ExpressBillRecord[]>()
  for (const record of scopedRecords) {
    const key = stationRecordKey(record)
    const list = grouped.get(key) ?? []
    list.push(record)
    grouped.set(key, list)
  }

  const cards: PrepExpressBillCard[] = []
  for (const stationRecords of grouped.values()) {
    // API 已按导入时间倒序；优先展示最近一次有账单行数的文件导入，避免手动补录批次 (0/0) 占位
    const primary =
      stationRecords.find((r) => (r.totalRows ?? 0) > 0) ?? stationRecords[0]
    const station = primary.expressStationId
      ? getStation(primary.expressStationId)
      : undefined
    const name = primary.expressStationName || station?.name || '—'
    const iconMeta = resolveExpressIconMetaFromStation(
      station ?? { name: primary.expressStationName ?? undefined },
    )
    const gapCount = stationRecords.reduce((max, r) => Math.max(max, r.gapOrderRows ?? 0), 0)
    cards.push({
      id: primary.id,
      name,
      iconSrc: iconMeta.src,
      isCustomAvatar: iconMeta.isCustomAvatar,
      matched: primary.matchedRows ?? 0,
      total: primary.totalRows ?? 0,
      gapCount: gapCount > 0 ? gapCount : undefined,
    })
  }
  return cards
}

/** 汇总店铺指标：空值按 0 计入 */
export function sumShopMetric(
  shops: MonthlySettlementShopSummary[],
  pick: (shop: MonthlySettlementShopSummary) => number | null | undefined,
) {
  return shops.reduce((sum, shop) => sum + (pick(shop) ?? 0), 0)
}

/** 展示买家排除时间：ISO 去 T 截断到秒 */
export function formatExcludeTime(value?: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 19)
}

/** 预备清单时间展示：非法/空返回 undefined（组件据此隐藏该行） */
export function formatPrepLastTime(value?: string | Date | null) {
  if (value == null || value === '') return undefined
  const formatted = formatDateTime(value)
  return formatted === '—' ? undefined : formatted
}

/** 从若干时间候选中取最晚的有效时间，用于各预备项"最近操作时间"展示 */
export function pickLatestPrepTime(...candidates: (string | Date | null | undefined)[]) {
  let latestTs: number | null = null
  let latestRaw: string | Date | undefined
  for (const candidate of candidates) {
    if (candidate == null || candidate === '') continue
    const date = candidate instanceof Date ? candidate : new Date(String(candidate).replace('T', ' '))
    const ts = date.getTime()
    if (Number.isNaN(ts)) continue
    if (latestTs == null || ts > latestTs) {
      latestTs = ts
      latestRaw = candidate
    }
  }
  return latestRaw != null ? formatPrepLastTime(latestRaw) : undefined
}

/** 结算月份展示：2026-08 → 2026年8月；非法格式原样返回 */
export function formatSettlementPeriodLabel(month: string): string {
  if (!month) return ''
  const [y, m] = month.split('-')
  if (!y || !m) return month
  return `${y}年${m}月`
}

/** 在全部店铺里找利润最高的有效订单；无有效订单返回 null */
export function findBestMaxProfitOrder(shops: MonthlySettlementShopSummary[]): MaxProfitDisplay | null {
  let bestShop: MonthlySettlementShopSummary | null = null
  let bestOrder: MaxProfitOrder | null = null
  for (const shop of shops) {
    const order = shop.maxProfitOrder
    if (!order?.orderNo && !order?.platformOrderNo) continue
    const profit = order.profitAmount ?? Number.NEGATIVE_INFINITY
    if (!bestOrder || profit > (bestOrder.profitAmount ?? Number.NEGATIVE_INFINITY)) {
      bestShop = shop
      bestOrder = order
    }
  }
  if (!bestShop || !bestOrder) return null
  return {
    ...bestOrder,
    shopId: bestShop.shopId,
    shopName: bestShop.shopName || `#${bestShop.shopId}`,
  }
}

/**
 * 展示用最大利润订单：showAll 时全店铺最优；否则取选中店铺的订单（无选中/无有效订单返回 null）
 */
export function buildMaxProfitDisplay(
  shops: MonthlySettlementShopSummary[],
  selectedShopId: number | null,
  showAll: boolean,
): MaxProfitDisplay | null {
  if (showAll) return findBestMaxProfitOrder(shops)
  const shop = selectedShopId == null ? null : shops.find((s) => s.shopId === selectedShopId) ?? null
  const order = shop?.maxProfitOrder
  if (!shop || (!order?.orderNo && !order?.platformOrderNo)) return null
  return {
    ...order,
    shopId: shop.shopId,
    shopName: shop.shopName || `#${shop.shopId}`,
  }
}

/**
 * 店铺汇总表的 span-method：未导入订单的店铺整行合并（首列占 6 列，其余隐藏）。
 * isImported 由调用方基于店铺导入状态判定，保持纯函数可测。
 */
export function computeShopSpan(columnIndex: number, isImported: boolean) {
  if (!isImported) {
    if (columnIndex === 1) {
      return { rowspan: 1, colspan: 6 }
    }
    if (columnIndex > 1) {
      return { rowspan: 0, colspan: 0 }
    }
  }
  return { rowspan: 1, colspan: 1 }
}

/** 按状态选项取展示文案；未命中回退原值，空值回退占位符 */
export function statusLabel(s: string | undefined, options: StatusOption[]): string {
  return options.find((o) => o.value === s)?.label ?? s ?? '—'
}

/**
 * 最大利润订单实际利润展示：
 * 未知原因优先翻译；其次金额格式化；金额缺失时按快递账单导入态给未知提示。
 */
export function resolveMaxProfitActualDisplay(
  item: MaxProfitDisplay | null,
  expressBillImported: boolean,
  t: (key: string) => string,
): { text: string; unknown: boolean } {
  if (!item) return { text: '—', unknown: false }

  const reasonCode = item.actualProfitUnknownReason
  if (reasonCode) {
    return {
      text: t(`ecommerce.monthlySettlement.maxProfitUnknownReason.${reasonCode}`),
      unknown: true,
    }
  }

  if (item.actualProfitAmount != null) {
    return { text: formatMoney(item.actualProfitAmount), unknown: false }
  }

  if (!expressBillImported) {
    return {
      text: t('ecommerce.monthlySettlement.maxProfitUnknownReason.EXPRESS_BILL_NOT_IMPORTED'),
      unknown: true,
    }
  }
  return {
    text: t('ecommerce.monthlySettlement.maxProfitUnknownReason.ACTUAL_FREIGHT_MISSING'),
    unknown: true,
  }
}

export type CalculateButtonMode = 'disabled' | 'recalculate' | 'precalculate' | 'calculate'

export interface CalculateButtonState {
  salesOrdersImported: boolean
  calculated: boolean
  expressBillImported: boolean
}

/**
 * 结算按钮状态机：由「订单导入 / 已结算 / 快递账单导入」三态推导 mode、禁用、文案与提示。
 * label 判定与原组件一致：已结算优先于销售导入判定（极端态下 mode 为 disabled 但 label 仍可走重算）。
 */
export function resolveCalculateButton(
  state: CalculateButtonState,
  t: (key: string) => string,
): { mode: CalculateButtonMode; disabled: boolean; label: string; tooltip: string } {
  const { salesOrdersImported, calculated, expressBillImported } = state
  const disabled = !salesOrdersImported

  let mode: CalculateButtonMode
  if (disabled) mode = 'disabled'
  else if (calculated) mode = 'recalculate'
  else if (!expressBillImported) mode = 'precalculate'
  else mode = 'calculate'

  let label: string
  if (calculated) label = t('ecommerce.monthlySettlement.recalculate')
  else if (salesOrdersImported && !expressBillImported) label = t('ecommerce.monthlySettlement.preCalculate')
  else label = t('ecommerce.monthlySettlement.calculate')

  const tooltip = mode === 'precalculate' ? t('ecommerce.monthlySettlement.preCalculateTip') : ''
  return { mode, disabled, label, tooltip }
}

/** 店铺汇总行图标解析：行内店铺名优先，缺失时回退选项店铺补全平台信息 */
export function resolveShopDisplayIcon(row: MonthlySettlementShopSummary, shopOptionMap: Map<number, EcShop>) {
  const shop = shopOptionMap.get(row.shopId)
  return resolveShopIconMeta(
    row.shopName ?? shop?.name,
    shop?.platformName,
    shop?.platformCode,
    shop?.avatarUrl,
  )
}

/** 最大利润订单的归属店铺图标：无 shopId 时仅用订单店铺名，否则按选项补全 */
export function resolveMaxProfitShopIcon(item: MaxProfitDisplay | null, shopOptionMap: Map<number, EcShop>) {
  if (!item?.shopId) {
    return resolveShopIconMeta(item?.shopName)
  }
  const shop = shopOptionMap.get(item.shopId)
  return resolveShopIconMeta(
    item.shopName ?? shop?.name,
    shop?.platformName,
    shop?.platformCode,
    shop?.avatarUrl,
  )
}

/** 订单月度总览 → 店铺导入状态 Map */
export function buildShopImportStatusMap(overview: EcSalesOrderMonthlyOverview | null): Map<number, ShopImportStatus> {
  const map = new Map<number, ShopImportStatus>()
  for (const shop of overview?.shops ?? []) {
    map.set(shop.shopId, shop.status)
  }
  return map
}

/** 订单导入判定：有订单数或待审数即视为已导入 */
export function hasSalesOrdersImported(overview: EcSalesOrderMonthlyOverview | null): boolean {
  const orderCount = overview?.totalOrderCount ?? 0
  const reviewCount = overview?.pendingReviewCount ?? 0
  return orderCount > 0 || reviewCount > 0
}

export interface OverallSummary {
  totalRevenue: number
  estimatedTotalCost: number
  actualTotalCost: number
  estimatedTotalProfit: number
  actualTotalProfit: number
  includedOrderCount: number
  excludedOrderCount: number
  pendingOrderCount: number
}

/** 已导入店铺的结算汇总：isImported 由调用方注入（依赖店铺导入状态表），过滤后逐项求和 */
export function computeOverallSummary(
  shops: MonthlySettlementShopSummary[],
  isImported: (shopId: number) => boolean,
): OverallSummary {
  const included = shops.filter((shop) => isImported(shop.shopId))
  return {
    totalRevenue: sumShopMetric(included, (s) => s.totalRevenue),
    estimatedTotalCost: sumShopMetric(included, (s) => s.estimatedTotalCost),
    actualTotalCost: sumShopMetric(included, (s) => s.actualTotalCost),
    estimatedTotalProfit: sumShopMetric(included, (s) => s.estimatedTotalProfit),
    actualTotalProfit: sumShopMetric(included, (s) => s.actualTotalProfit),
    includedOrderCount: sumShopMetric(included, (s) => s.includedOrderCount),
    excludedOrderCount: sumShopMetric(included, (s) => s.excludedOrderCount),
    pendingOrderCount: sumShopMetric(included, (s) => s.pendingOrderCount),
  }
}

/** 店铺选项列表 → id 索引 Map，供图标/名称快速反查 */
export function buildShopOptionMap(shopOptions: EcShop[]): Map<number, EcShop> {
  const map = new Map<number, EcShop>()
  for (const shop of shopOptions) {
    map.set(shop.id, shop)
  }
  return map
}

/** 快递站列表 → id 索引 Map */
export function buildExpressStationMap(stations: EcExpressStation[]): Map<number, EcExpressStation> {
  const map = new Map<number, EcExpressStation>()
  for (const station of stations) {
    map.set(station.id, station)
  }
  return map
}

/** 订单状态选项字典（salesOrder 状态枚举 → i18n 文案），供状态标签展示 */
export function buildStatusOptions(t: (key: string) => string): StatusOption[] {
  return [
    { value: 'DRAFT', label: t('ecommerce.salesOrder.statusDraft') },
    { value: 'PAID', label: t('ecommerce.salesOrder.statusPaid') },
    { value: 'PARTIAL_SHIPPED', label: t('ecommerce.salesOrder.statusPartialShipped') },
    { value: 'SHIPPED', label: t('ecommerce.salesOrder.statusShipped') },
    { value: 'PARTIAL_REFUND', label: t('ecommerce.salesOrder.statusPartialRefund') },
    { value: 'COMPLETED', label: t('ecommerce.salesOrder.statusCompleted') },
    { value: 'REFUNDED', label: t('ecommerce.salesOrder.statusRefunded') },
    { value: 'CANCELLED', label: t('ecommerce.salesOrder.statusCancelled') },
  ]
}
