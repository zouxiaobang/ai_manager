import { describe, expect, it } from 'vitest'
import type { EcExpressPrice } from '@/api/ecommerce/express'
import { VOLUMETRIC_DIVISOR_CALC, computeCalcFreight, formatCalcPrice, formatCalcWeight } from '../expressCalc'

/** 构造全档位齐全的价格 fixture（各档 10 元起步，方便断言递增） */
function makePrice(partial?: Partial<EcExpressPrice>): EcExpressPrice {
  return {
    id: 1,
    stationId: 1,
    provinceName: '广东',
    priceW03Kg: 6,
    priceW05Kg: 7,
    priceW1Kg: 8,
    priceW15Kg: 9,
    priceW2Kg: 10,
    priceW25Kg: 11,
    priceW3Kg: 12,
    over3FirstPrice: 12,
    over3AdditionalPrice: 3,
    ...partial,
  }
}

describe('expressCalc 运费试算纯函数', () => {
  describe('computeCalcFreight', () => {
    it('≤0.3kg 命中最低档', () => {
      const r = computeCalcFreight(0, 0, 0, 0.2, makePrice(), 2)
      expect(r.tier).toBe('≤0.3kg')
      expect(r.freight).toBe(6)
      expect(r.warning).toBeUndefined()
    })

    it('≤0.5/1/1.5/2/2.5/3kg 各档逐级递增', () => {
      const cases: Array<[number, string, number]> = [
        [0.4, '≤0.5kg', 7],
        [0.9, '≤1kg', 8],
        [1.4, '≤1.5kg', 9],
        [1.9, '≤2kg', 10],
        [2.4, '≤2.5kg', 11],
        [2.9, '≤3kg', 12],
      ]
      for (const [weight, tier, freight] of cases) {
        const r = computeCalcFreight(0, 0, 0, weight, makePrice(), 0)
        expect(r.tier).toBe(tier)
        expect(r.freight).toBe(freight)
      }
    })

    it('>3kg 走首重+续重（超出部分向上取整）', () => {
      // 4.2kg：超 1.2kg → 续重 2kg
      const r = computeCalcFreight(0, 0, 0, 4.2, makePrice(), 0)
      expect(r.freight).toBe(12 + 2 * 3)
      expect(r.tier).toContain('续重2kg')
    })

    it('>3kg 未配置首重：运费 0 并给 warning', () => {
      const r = computeCalcFreight(0, 0, 0, 4, makePrice({ over3FirstPrice: null }), 0)
      expect(r.freight).toBe(0)
      expect(r.tier).toBe('>3kg')
      expect(r.warning).toContain('未配置续重价格')
    })

    it('未配置当前档位价格：运费按 0 计并给 warning', () => {
      const r = computeCalcFreight(0, 0, 0, 0.4, makePrice({ priceW05Kg: null }), 0)
      expect(r.freight).toBe(0)
      expect(r.tier).toBe('≤0.5kg')
      expect(r.warning).toContain('未配置 ≤0.5kg 价格')
    })

    it('体积重 = 长宽高积 ÷ 8000，仅三边均正数时计算', () => {
      const noVolume = computeCalcFreight(0, 0, 0, 1, makePrice(), 0)
      expect(noVolume.volumetricWeight).toBe(0)

      const vol = computeCalcFreight(40, 30, 20, 1, makePrice(), 0)
      expect(vol.volumetricWeight).toBeCloseTo((40 * 30 * 20) / VOLUMETRIC_DIVISOR_CALC)
      // 计费重仍取实重
      expect(vol.billingWeight).toBe(1)
    })

    it('labelPrice 与 total 原样透传', () => {
      const r = computeCalcFreight(0, 0, 0, 0.2, makePrice(), 5)
      expect(r.labelPrice).toBe(5)
      expect(r.total).toBe(r.freight)
    })
  })

  describe('formatCalcPrice', () => {
    it('空值显示 0.00', () => {
      expect(formatCalcPrice(null)).toBe('0.00')
      expect(formatCalcPrice(undefined)).toBe('0.00')
    })

    it('数字保留两位小数', () => {
      expect(formatCalcPrice(9)).toBe('9.00')
      expect(formatCalcPrice(9.5)).toBe('9.50')
      expect(formatCalcPrice(9.566)).toBe('9.57')
    })
  })

  describe('formatCalcWeight', () => {
    it('保留三位小数', () => {
      expect(formatCalcWeight(1)).toBe('1.000')
      expect(formatCalcWeight(1.23456)).toBe('1.235')
    })
  })
})
