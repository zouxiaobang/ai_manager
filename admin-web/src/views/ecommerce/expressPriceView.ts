import type { EcExpressPrice } from '@/api/ecommerce/express'

/** 快递价格矩阵重量档位字段（热力图、编辑表单、分组共用的一组键，顺序即展示顺序） */
export const PRICE_FIELD_KEYS = [
  'priceW03Kg',
  'priceW05Kg',
  'priceW1Kg',
  'priceW15Kg',
  'priceW2Kg',
  'priceW25Kg',
  'priceW3Kg',
  'over3FirstPrice',
  'over3AdditionalPrice',
] as const

/** 价格矩阵字段键字面量 */
export type PriceFieldKey = (typeof PRICE_FIELD_KEYS)[number]

/**
 * 收集价格矩阵全部数值：空值与 NaN 视为无价，不参与热力分布计算。
 * 支持传入自定义字段集合，便于按展示列复用。
 */
export function collectPriceValues(
  priceRows: EcExpressPrice[],
  fieldKeys: readonly PriceFieldKey[] = PRICE_FIELD_KEYS,
): number[] {
  const values: number[] = []
  for (const row of priceRows) {
    for (const key of fieldKeys) {
      const value = row[key]
      if (value != null && !Number.isNaN(Number(value))) {
        values.push(Number(value))
      }
    }
  }
  return values
}

/** 价格热力背景：无值透明；无有效分布或分布单一时回退淡绿；否则按最低→最高从淡绿渐变到淡橙 */
export function priceHeatStyle(
  value: number | null | undefined,
  priceRows: EcExpressPrice[],
): { background: string } {
  if (value == null) {
    return { background: 'transparent' }
  }
  const allValues = collectPriceValues(priceRows)
  if (!allValues.length) {
    return { background: 'var(--wr-stat-green-bg, #f0fdf4)' }
  }
  const min = Math.min(...allValues)
  const max = Math.max(...allValues)
  if (min === max) {
    return { background: 'var(--wr-stat-green-bg, #f0fdf4)' }
  }
  const ratio = (Number(value) - min) / (max - min)
  const low = [240, 253, 244]
  const high = [255, 247, 237]
  const r = Math.round(low[0] + (high[0] - low[0]) * ratio)
  const g = Math.round(low[1] + (high[1] - low[1]) * ratio)
  const b = Math.round(low[2] + (high[2] - low[2]) * ratio)
  return { background: `rgb(${r}, ${g}, ${b})` }
}
