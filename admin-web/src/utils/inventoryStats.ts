import type { EcInventory } from '@/api/ecommerce/inventory'

export type InventoryStatusKey = 'normal' | 'low' | 'zero' | 'slow'

export interface InventoryStatusStats {
  total: number
  normal: number
  low: number
  zero: number
  slow: number
}

export interface InventoryClassificationOptions {
  defaultAlertThreshold?: number
  slowMovingDays?: number
  slowMovingFallbackDays?: number
}

const DEFAULT_OPTIONS: Required<InventoryClassificationOptions> = {
  defaultAlertThreshold: 10,
  slowMovingDays: 45,
  slowMovingFallbackDays: 90,
}

export function isSlowMovingInventory(row: EcInventory, options?: InventoryClassificationOptions) {
  const qty = row.quantity ?? 0
  if (qty <= 0 || row.alertActive) return false
  const threshold = row.alertThreshold ?? options?.defaultAlertThreshold ?? DEFAULT_OPTIONS.defaultAlertThreshold
  const updated = row.updateTime ? new Date(row.updateTime).getTime() : 0
  const fallbackDays = options?.slowMovingFallbackDays ?? DEFAULT_OPTIONS.slowMovingFallbackDays
  const slowDays = options?.slowMovingDays ?? DEFAULT_OPTIONS.slowMovingDays
  const daysSince = updated ? (Date.now() - updated) / 86_400_000 : fallbackDays
  return daysSince >= slowDays && qty > threshold
}

export function classifyInventory(row: EcInventory, options?: InventoryClassificationOptions): InventoryStatusKey {
  const qty = row.quantity ?? 0
  if (qty <= 0) return 'zero'
  if (row.alertActive) return 'low'
  if (isSlowMovingInventory(row, options)) return 'slow'
  return 'normal'
}

export function computeInventoryStats(
  items: EcInventory[],
  options?: InventoryClassificationOptions,
): InventoryStatusStats {
  const stats: InventoryStatusStats = { total: items.length, normal: 0, low: 0, zero: 0, slow: 0 }
  for (const row of items) {
    stats[classifyInventory(row, options)] += 1
  }
  return stats
}

/** 正常=100、偏低=55、滞销=35、缺货=0，与首页监控口径一致 */
export function computeInventoryHealthScore(stats: InventoryStatusStats): number {
  if (stats.total <= 0) return 100
  const weighted = stats.normal * 100 + stats.low * 55 + stats.slow * 35
  return Math.round(weighted / stats.total)
}

export function inventoryStatusPercent(stats: InventoryStatusStats, key: InventoryStatusKey): number {
  if (stats.total <= 0) return 0
  return Math.round((stats[key] / stats.total) * 1000) / 10
}

export interface InventoryListRow extends EcInventory {
  listKey: string
  spuSkuCount?: number
}

export function inventorySpuGroupKey(row: EcInventory): string {
  return row.productId != null
    ? `product:${row.productId}`
    : `name:${(row.productName || row.skuCode).trim()}`
}

export function groupInventoriesBySpu(rows: EcInventory[]): InventoryListRow[] {
  const groups = new Map<string, EcInventory[]>()
  for (const row of rows) {
    const key = inventorySpuGroupKey(row)
    const bucket = groups.get(key) ?? []
    bucket.push(row)
    groups.set(key, bucket)
  }

  return Array.from(groups.entries()).map(([key, items]) => {
    const sorted = [...items].sort((a, b) => (b.quantity ?? 0) - (a.quantity ?? 0))
    const primary = sorted[0]
    const quantity = items.reduce((sum, item) => sum + (item.quantity ?? 0), 0)
    const inTransitQty = items.reduce((sum, item) => sum + (item.inTransitQty ?? 0), 0)
    const alertActive = items.some((item) => item.alertActive)
    const alertThreshold = items.reduce((sum, item) => sum + (item.alertThreshold ?? 0), 0)
    const stockValue = items.reduce((sum, item) => sum + (item.quantity ?? 0) * (item.salePrice ?? 0), 0)
    const salePrice = quantity > 0 ? stockValue / quantity : primary.salePrice

    return {
      ...primary,
      listKey: key,
      spuSkuCount: items.length,
      skuCode: primary.productName || primary.skuCode,
      productName: items.length > 1 ? undefined : (primary.specName || primary.skuCode),
      quantity,
      inTransitQty,
      alertActive,
      alertThreshold,
      salePrice,
    }
  }).sort((a, b) => (b.quantity ?? 0) - (a.quantity ?? 0))
}
