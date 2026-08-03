import type { EcListingLink, EcListingLinkSku } from '@/api/ecommerce/listingLink'
import { formatSignedCnyPlain } from '@/utils/formatMoney'
import { parseSkuCodes } from './listingLinkSkuView'

/** 卡片 SKU 预览条数上限（超出折叠为 +N） */
export const CARD_SKU_PREVIEW_LIMIT = 2

/** 平台条渐变背景：未知平台回退蓝色渐变 */
export const PLATFORM_STRIP_COLORS: Record<string, string> = {
  淘宝: 'linear-gradient(90deg, #ff6a00, #ff9500)',
  拼多多: 'linear-gradient(90deg, #e02e24, #f43530)',
  抖音: 'linear-gradient(90deg, #111827, #4b5563)',
  京东: 'linear-gradient(90deg, #c81623, #e1251b)',
}

/** 卡片图片栈项（key 供损坏态标记去重） */
export type CardImageStackItem = { key: string; imageName?: string }

/** 平台条样式：按平台名取渐变背景 */
export function platformStripStyle(row: EcListingLink) {
  const name = row.platformName?.trim() ?? ''
  const gradient = PLATFORM_STRIP_COLORS[name] ?? 'linear-gradient(90deg, #2563eb, #3b82f6)'
  return { background: gradient }
}

/** 卡片产品 ID 列表（去掉未关联的空 ID） */
export function linkProductIds(row: EcListingLink) {
  return (row.products ?? []).map((p) => p.productId).filter((id) => id != null)
}

/** 利润徽章文本：空值占位符，正利润带加号 */
export function formatProfitChip(profit?: number | null) {
  if (profit == null) return '—'
  return formatSignedCnyPlain(Number(profit))
}

/** 利润徽章态：亏损/风险红，正利润绿，零利润灰 */
export function profitChipClass(sku: EcListingLinkSku) {
  if (sku.pricingRisk === 'BELOW_MIN' || sku.pricingRisk === 'NEGATIVE_PROFIT') return 'is-danger'
  const profit = sku.profit ?? 0
  if (profit < 0) return 'is-danger'
  if (profit > 0) return 'is-success'
  return 'is-muted'
}

/** 卡片 SKU 预览条：取前 limit 条；previews 为已加载的 SKU 预览，缺省回退行内 skus */
export function cardSkuPreviews(
  row: EcListingLink,
  previews: EcListingLinkSku[] | undefined,
  limit = CARD_SKU_PREVIEW_LIMIT,
) {
  const skus = previews ?? row.skus ?? []
  return skus.slice(0, limit)
}

/** 卡片 SKU 折叠数：总数减去预览上限，未超出时为 0 */
export function cardSkuOverflow(
  row: EcListingLink,
  previews: EcListingLinkSku[] | undefined,
  limit = CARD_SKU_PREVIEW_LIMIT,
) {
  const totalCount = row.skuCount ?? (previews ?? row.skus ?? []).length
  return Math.max(0, totalCount - limit)
}

/**
 * 卡片图片栈：单产品多 SKU 时铺 SKU 图（按编码去重），否则铺产品图。
 * maps 注入避免依赖组件响应式状态，便于纯函数测试。
 */
export function cardImageStack(
  row: EcListingLink,
  previews: EcListingLinkSku[] | undefined,
  productImageMap: Record<number, string | undefined>,
  skuImageMap: Record<string, string | undefined>,
): CardImageStackItem[] {
  const productIds = linkProductIds(row)
  const linkSkus = previews ?? row.skus ?? []

  if (productIds.length === 1 && linkSkus.length > 1) {
    const items: CardImageStackItem[] = []
    const seen = new Set<string>()
    for (const sku of linkSkus) {
      for (const code of parseSkuCodes(sku)) {
        if (seen.has(code)) continue
        seen.add(code)
        items.push({ key: `sku-${code}`, imageName: skuImageMap[code] })
      }
    }
    if (items.length) return items
  }

  if (productIds.length > 0) {
    return productIds.map((id) => ({
      key: `product-${id}`,
      imageName: productImageMap[id],
    }))
  }

  return []
}

/** 标记卡片图片已损坏：返回含新 key 的新 Set（不可变更新，避免原地改 ref） */
export function markStackImageBroken(key: string, current: Set<string>): Set<string> {
  if (current.has(key)) return current
  return new Set([...current, key])
}
