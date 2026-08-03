import { describe, expect, it } from 'vitest'
import type { EcListingLinkSku } from '@/api/ecommerce/listingLink'
import {
  calcNetRevenue,
  calcProfitRate,
  formatPercent,
  formSkuImageKey,
  parseSkuCodes,
  priceScale,
  primarySkuCode,
  profitAmountClass,
  profitRingColor,
  profitRingPercent,
  rowInventories,
  skuCardTone,
  skuStockAlert,
  skuStockTotal,
  summarizeSkuPricing,
} from '../listingLinkSkuView'

function makeSku(partial?: Partial<EcListingLinkSku>): EcListingLinkSku {
  return { skuName: 'SKU甲', skuCodes: 'A1,A2', ...partial }
}

describe('listingLinkSkuView SKU 行计算纯函数', () => {
  describe('formatPercent', () => {
    it('空值占位符', () => {
      expect(formatPercent(null)).toBe('—')
      expect(formatPercent(undefined)).toBe('—')
    })

    it('保留两位小数', () => {
      expect(formatPercent(12.345)).toBe('12.35%')
      expect(formatPercent(12)).toBe('12.00%')
    })
  })

  describe('calcNetRevenue', () => {
    it('按折扣与优惠券计算净额', () => {
      const sku = makeSku({ actualSetAmount: 100, couponAmount: 10, discountPct: 90 })
      expect(calcNetRevenue(sku)).toBe(81)
    })

    it('actualSetAmount 缺失返回 null', () => {
      expect(calcNetRevenue(makeSku({ actualSetAmount: null }))).toBeNull()
    })

    it('折扣越界或优惠券超售价返回 null', () => {
      expect(calcNetRevenue(makeSku({ actualSetAmount: 100, discountPct: 0 }))).toBeNull()
      expect(calcNetRevenue(makeSku({ actualSetAmount: 100, discountPct: 120 }))).toBeNull()
      expect(calcNetRevenue(makeSku({ actualSetAmount: 100, couponAmount: 150 }))).toBeNull()
    })
  })

  describe('calcProfitRate', () => {
    it('利润率 = 利润/净额×100', () => {
      expect(calcProfitRate(20, 100)).toBe(20)
    })

    it('利润或净额缺失、净额非正返回 null', () => {
      expect(calcProfitRate(null, 100)).toBeNull()
      expect(calcProfitRate(20, null)).toBeNull()
      expect(calcProfitRate(20, 0)).toBeNull()
    })
  })

  describe('parseSkuCodes', () => {
    it('逗号拆分并去空、trim', () => {
      expect(parseSkuCodes(makeSku({ skuCodes: ' A1 , B2 ,, C3 ' }))).toEqual(['A1', 'B2', 'C3'])
    })

    it('无编码时回退名称', () => {
      expect(parseSkuCodes(makeSku({ skuCodes: '', skuName: ' 名称甲 ' }))).toEqual(['名称甲'])
    })

    it('编码与名称都空返回空数组', () => {
      expect(parseSkuCodes(makeSku({ skuCodes: '', skuName: '  ' }))).toEqual([])
    })
  })

  describe('primarySkuCode', () => {
    it('取首个解析编码', () => {
      expect(primarySkuCode(makeSku())).toBe('A1')
    })

    it('解析为空但原始串为空格时回退原始串', () => {
      expect(primarySkuCode(makeSku({ skuCodes: '  ', skuName: '  ' }))).toBe('  ')
    })

    it('全空回退占位符', () => {
      expect(primarySkuCode(makeSku({ skuCodes: '', skuName: '  ' }))).toBe('—')
    })
  })

  describe('rowInventories / skuStockTotal / skuStockAlert', () => {
    it('未配置库存视为空列表', () => {
      expect(rowInventories(makeSku())).toEqual([])
    })

    it('总库存求和', () => {
      const sku = makeSku({
        inventories: [
          { skuCode: 'A1', quantity: 3, alertActive: false },
          { skuCode: 'A2', quantity: 5, alertActive: true },
        ],
      })
      expect(skuStockTotal(sku)).toBe(8)
    })

    it('存在告警行时告警为 true', () => {
      const sku = makeSku({
        inventories: [
          { skuCode: 'A1', quantity: 3, alertActive: false },
          { skuCode: 'A2', quantity: 0, alertActive: true },
        ],
      })
      expect(skuStockAlert(sku)).toBe(true)
      expect(skuStockAlert(makeSku())).toBe(false)
    })
  })

  describe('profitRingPercent', () => {
    it('无可算利润率归 0', () => {
      expect(profitRingPercent(makeSku({ profit: null }))).toBe(0)
    })

    it('绝对值封顶 100', () => {
      const sku = makeSku({ actualSetAmount: 100, couponAmount: 0, discountPct: 100, profit: -150 })
      expect(profitRingPercent(sku)).toBe(100)
    })
  })

  describe('profitRingColor / profitAmountClass', () => {
    it('负利润或亏损风险红', () => {
      const sku = makeSku({ profit: -1 })
      expect(profitRingColor(sku)).toBe('#ef4444')
      expect(profitAmountClass(sku)).toBe('is-danger')
      expect(profitRingColor(makeSku({ pricingRisk: 'NEGATIVE_PROFIT' }))).toBe('#ef4444')
    })

    it('低于最低价橙', () => {
      expect(profitRingColor(makeSku({ pricingRisk: 'BELOW_MIN' }))).toBe('#f59e0b')
      expect(profitAmountClass(makeSku({ pricingRisk: 'BELOW_MIN' }))).toBe('is-warning')
    })

    it('正常绿', () => {
      expect(profitRingColor(makeSku({ profit: 10 }))).toBe('#22c55e')
      expect(profitAmountClass(makeSku({ profit: 10 }))).toBe('is-success')
    })
  })

  describe('priceScale', () => {
    it('实际价高于最低价时右段为正', () => {
      const scale = priceScale(makeSku({ costPrice: 50, minSetAmount: 100, actualSetAmount: 150, profit: 10 }))
      expect(scale.costInLeft).toBe(50)
      expect(scale.rightSeg).toBe(50)
      expect(scale.belowMin).toBe(false)
      expect(scale.profitClass).toBe('is-profit')
    })

    it('实际价低于最低价时风险态', () => {
      const scale = priceScale(makeSku({ costPrice: 50, minSetAmount: 100, actualSetAmount: 80, profit: 10 }))
      expect(scale.belowMin).toBe(true)
      expect(scale.profitClass).toBe('is-risk')
    })

    it('BELOW_MIN 风险直接标记 belowMin', () => {
      const scale = priceScale(makeSku({ pricingRisk: 'BELOW_MIN', minSetAmount: 100, actualSetAmount: 150 }))
      expect(scale.belowMin).toBe(true)
    })

    it('min 为 0 时成本位置回退 35', () => {
      const scale = priceScale(makeSku({ costPrice: 10, minSetAmount: 0, actualSetAmount: 20 }))
      expect(scale.costInLeft).toBe(35)
    })
  })

  describe('skuCardTone', () => {
    it('风险优先', () => {
      expect(skuCardTone(makeSku({ pricingRisk: 'BELOW_MIN' }))).toBe('is-risk')
      expect(skuCardTone(makeSku({ pricingRisk: 'NEGATIVE_PROFIT' }))).toBe('is-risk')
    })

    it('按利润正负', () => {
      expect(skuCardTone(makeSku({ profit: -1 }))).toBe('is-risk')
      expect(skuCardTone(makeSku({ profit: 1 }))).toBe('is-profit')
      expect(skuCardTone(makeSku({ profit: 0 }))).toBe('')
    })
  })

  describe('summarizeSkuPricing', () => {
    it('汇总数量与平均利润/利润率', () => {
      const skus = [
        makeSku({ actualSetAmount: 100, couponAmount: 0, discountPct: 100, profit: 20 }),
        makeSku({ skuName: '乙', skuCodes: 'B1', actualSetAmount: 50, profit: 10 }),
        makeSku({ skuName: '丙', skuCodes: 'C1', profit: null }),
      ]
      const summary = summarizeSkuPricing(skus)
      expect(summary.count).toBe(3)
      expect(summary.avgProfit).toBe(15)
      // 净额 100 → 20%，净额 50 → 20%，无净额跳过 → 平均 20%
      expect(summary.avgProfitRate).toBe(20)
    })

    it('无有效数据时平均值为 null', () => {
      const summary = summarizeSkuPricing([makeSku({ profit: null, actualSetAmount: null })])
      expect(summary).toEqual({ count: 1, avgProfit: null, avgProfitRate: null })
    })
  })

  describe('formSkuImageKey', () => {
    it('按编码与行号生成唯一 key', () => {
      expect(formSkuImageKey(makeSku({ skuCodes: 'A1' }), 2)).toBe('edit-A1')
      expect(formSkuImageKey(makeSku({ skuCodes: '', skuName: '乙' }), 0)).toBe('edit-乙')
    })
  })
})
