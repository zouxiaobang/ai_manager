import type { EcExpressPrice } from '@/api/ecommerce/express'

/** 体积重除数：长宽高(cm) 乘积 ÷ 8000 → 体积重(kg) */
export const VOLUMETRIC_DIVISOR_CALC = 8000

/** 运费试算结果（含计费重量、命中档位、运费与面单费合计） */
export interface CalcResult {
  volumetricWeight: number
  billingWeight: number
  tier: string
  freight: number
  labelPrice: number
  total: number
  warning?: string
}

/**
 * 按实重命中最重价格档位计算运费。
 *
 * <p>档位按 ≤0.3/0.5/1/1.5/2/2.5/3kg 递增命中，>3kg 走首重+续重。
 * 未配置某档价格时该档运费按 0 计并在 warning 说明。</p>
 */
export function computeCalcFreight(
  length: number,
  width: number,
  height: number,
  actualWeight: number,
  price: EcExpressPrice,
  labelPrice: number,
): CalcResult {
  const hasVolume = length > 0 && width > 0 && height > 0
  const volumetricWeight = hasVolume ? (length * width * height) / VOLUMETRIC_DIVISOR_CALC : 0
  const billingWeight = actualWeight

  let freight = 0
  let tier = ''
  const warnings: string[] = []

  if (billingWeight <= 0.3) {
    freight = price.priceW03Kg ?? 0
    tier = '≤0.3kg'
    if (price.priceW03Kg == null) warnings.push('该地区未配置 ≤0.3kg 价格')
  } else if (billingWeight <= 0.5) {
    freight = price.priceW05Kg ?? 0
    tier = '≤0.5kg'
    if (price.priceW05Kg == null) warnings.push('该地区未配置 ≤0.5kg 价格')
  } else if (billingWeight <= 1) {
    freight = price.priceW1Kg ?? 0
    tier = '≤1kg'
    if (price.priceW1Kg == null) warnings.push('该地区未配置 ≤1kg 价格')
  } else if (billingWeight <= 1.5) {
    freight = price.priceW15Kg ?? 0
    tier = '≤1.5kg'
    if (price.priceW15Kg == null) warnings.push('该地区未配置 ≤1.5kg 价格')
  } else if (billingWeight <= 2) {
    freight = price.priceW2Kg ?? 0
    tier = '≤2kg'
    if (price.priceW2Kg == null) warnings.push('该地区未配置 ≤2kg 价格')
  } else if (billingWeight <= 2.5) {
    freight = price.priceW25Kg ?? 0
    tier = '≤2.5kg'
    if (price.priceW25Kg == null) warnings.push('该地区未配置 ≤2.5kg 价格')
  } else if (billingWeight <= 3) {
    freight = price.priceW3Kg ?? 0
    tier = '≤3kg'
    if (price.priceW3Kg == null) warnings.push('该地区未配置 ≤3kg 价格')
  } else {
    if (price.over3FirstPrice == null) {
      warnings.push('该地区未配置续重价格，无法计算 >3kg 运费')
      tier = '>3kg'
    } else {
      const over = billingWeight - 3
      const additionalKg = Math.ceil(over)
      const additionalPrice = price.over3AdditionalPrice ?? 0
      freight = price.over3FirstPrice + additionalKg * additionalPrice
      tier = `>3kg（首重¥${price.over3FirstPrice.toFixed(2)} + 续重${additionalKg}kg×¥${additionalPrice.toFixed(2)}）`
    }
  }

  const total = freight
  return {
    volumetricWeight,
    billingWeight,
    tier,
    freight,
    labelPrice,
    total,
    warning: warnings.length ? warnings.join('；') : undefined,
  }
}

/**
 * 体积重换算：长宽高(cm) 积 ÷ 8000 → 体积重(kg)，保留三位有效小数。
 * 与 computeCalcFreight 的体积重口径一致（仅三边均正数时有意义，由调用方保证）。
 */
export function computeVolumeWeight(length: number, width: number, height: number): number {
  return Math.round((length * width * height) / VOLUMETRIC_DIVISOR_CALC * 1000) / 1000
}

/** 试算金额展示：保留两位小数，空值显示 0.00 */
export function formatCalcPrice(price?: number | null): string {
  if (price == null) return '0.00'
  return Number(price).toFixed(2)
}

/** 试算重量展示：保留三位小数 */
export function formatCalcWeight(weight: number): string {
  return Number(weight).toFixed(3)
}
