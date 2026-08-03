import type {
  EcExpressNotice,
  EcExpressNoticeSaveRequest,
  EcExpressPrice,
  EcExpressPriceSaveRequest,
  EcExpressStation,
  EcExpressStationSaveRequest,
} from '@/api/ecommerce/express'
import type { PriceFieldKey } from './expressPriceView'

/** 按区域名过滤价格：选中集 trim 后精确匹配；无筛选时返回全部 */
export function filterPricesByRegions(
  prices: EcExpressPrice[] | undefined,
  regionNames: string[],
): EcExpressPrice[] {
  if (!prices?.length) return []
  if (!regionNames.length) return prices
  const selected = new Set(regionNames.map((name) => name.trim()))
  return prices.filter((price) => selected.has(price.provinceName.trim()))
}

/** 按关键词过滤价格：省份名不区分大小写包含匹配；空关键词返回全部 */
export function filterPricesByKeyword(prices: EcExpressPrice[], keyword: string): EcExpressPrice[] {
  const kw = keyword.trim().toLowerCase()
  if (!kw) return prices
  return prices.filter((price) => price.provinceName.toLowerCase().includes(kw))
}

/** 最近使用省份：去重后取末 count 条并倒序（供试算/价格弹窗快捷选择） */
export function buildRecentPriceRegions(prices: EcExpressPrice[], count = 6): string[] {
  const names = prices.map((item) => item.provinceName.trim()).filter(Boolean)
  return [...new Set(names)].slice(-count).reverse()
}

/** 通知拖拽排序结果：重排后的列表 + 需要落库的顺序变更条目 */
export interface NoticeReorderResult {
  ordered: EcExpressNotice[]
  updates: EcExpressNotice[]
}

/**
 * 通知拖拽排序 diff：移动 oldIndex → newIndex，返回重排列表与 sortOrder 变化条目。
 * 仅当有实际移动且存在被移动项时返回结果，否则返回 null。
 */
export function computeNoticeReorders(
  notices: EcExpressNotice[],
  oldIndex: number,
  newIndex: number,
): NoticeReorderResult | null {
  if (oldIndex === newIndex || oldIndex < 0 || newIndex < 0 || !notices.length) return null
  const ordered = notices.map((n) => ({ ...n }))
  const [moved] = ordered.splice(oldIndex, 1)
  if (!moved) return null
  ordered.splice(newIndex, 0, moved)
  const previous = new Map(notices.map((item) => [item.id, item.sortOrder ?? 0]))
  ordered.forEach((item, index) => {
    item.sortOrder = index
  })
  const updates = ordered.filter((item, index) => previous.get(item.id) !== index)
  if (!updates.length) return null
  return { ordered, updates }
}

/** 站点编辑表单 → 保存请求体：trim 空串归 undefined、别名过滤空串 */
export function buildStationSavePayload(form: {
  name: string
  avatarUrl?: string | null
  contact?: string
  address?: string
  labelPrice?: number | null
  isDefault: boolean
  nameAliases: string[]
}): EcExpressStationSaveRequest {
  return {
    name: form.name.trim(),
    avatarUrl: form.avatarUrl?.trim() || undefined,
    contact: form.contact?.trim() || undefined,
    address: form.address?.trim() || undefined,
    labelPrice: form.labelPrice,
    isDefault: form.isDefault,
    nameAliases: form.nameAliases.map((item) => item.trim()).filter(Boolean),
  }
}

/** 价格矩阵表单值：省份名 + 各重量档位价 */
export interface PriceFormValue {
  provinceName: string
  priceW03Kg: number | null
  priceW05Kg: number | null
  priceW1Kg: number | null
  priceW15Kg: number | null
  priceW2Kg: number | null
  priceW25Kg: number | null
  priceW3Kg: number | null
  over3FirstPrice: number | null
  over3AdditionalPrice: number | null
}

/** 价格表单 → 保存请求体：省份名 trim，档位值原样透传 */
export function buildPriceSavePayload(
  form: PriceFormValue,
  stationId: number,
): EcExpressPriceSaveRequest {
  return {
    stationId,
    provinceName: form.provinceName.trim(),
    priceW03Kg: form.priceW03Kg,
    priceW05Kg: form.priceW05Kg,
    priceW1Kg: form.priceW1Kg,
    priceW15Kg: form.priceW15Kg,
    priceW2Kg: form.priceW2Kg,
    priceW25Kg: form.priceW25Kg,
    priceW3Kg: form.priceW3Kg,
    over3FirstPrice: form.over3FirstPrice,
    over3AdditionalPrice: form.over3AdditionalPrice,
  }
}

/** 公告表单 → 保存请求体：内容 trim，标记与排序原样透传 */
export function buildNoticeSavePayload(
  form: { content: string; highlightRed: boolean; sortOrder: number },
  stationId: number,
): EcExpressNoticeSaveRequest {
  return {
    stationId,
    content: form.content.trim(),
    highlightRed: form.highlightRed,
    sortOrder: form.sortOrder,
  }
}

/** 行级地区数：列表已带计数优先，否则回退展开详情的价格行数 */
export function resolveRegionCount(row: EcExpressStation, detail?: EcExpressStation | null): number {
  if (row.priceCount != null) return row.priceCount
  return detail?.prices?.length ?? 0
}

/** 行级须知数：列表已带计数优先，否则回退展开详情的公告条数 */
export function resolveNoticeCount(row: EcExpressStation, detail?: EcExpressStation | null): number {
  if (row.noticeCount != null) return row.noticeCount
  return detail?.notices?.length ?? 0
}

/** 展开详情同步行级计数，供列表角标展示 */
export function syncRowCounts(row: EcExpressStation, detail: EcExpressStation): void {
  row.priceCount = detail.prices?.length ?? 0
  row.noticeCount = detail.notices?.length ?? 0
}

/** 从价格行取指定档位键的数值映射（复制上一行/编辑回填复用） */
export function pickPriceValues(
  source: EcExpressPrice,
  keys: readonly PriceFieldKey[],
): Record<PriceFieldKey, number | null> {
  const result = {} as Record<PriceFieldKey, number | null>
  for (const key of keys) {
    result[key] = source[key] ?? null
  }
  return result
}
