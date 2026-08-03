import { describe, expect, it } from 'vitest'
import type { EcExpressPrice } from '@/api/ecommerce/express'
import {
  PRICE_FIELD_KEYS,
  collectPriceValues,
  priceHeatStyle,
  type PriceFieldKey,
} from '../expressPriceView'

function makePrice(partial?: Partial<EcExpressPrice>): EcExpressPrice {
  return { id: 1, stationId: 1, provinceName: '广东', ...partial }
}

describe('expressPriceView 快递价格热力域', () => {
  describe('PRICE_FIELD_KEYS', () => {
    it('覆盖 9 个重量档位字段且顺序与展示列一致', () => {
      expect(PRICE_FIELD_KEYS).toEqual([
        'priceW03Kg',
        'priceW05Kg',
        'priceW1Kg',
        'priceW15Kg',
        'priceW2Kg',
        'priceW25Kg',
        'priceW3Kg',
        'over3FirstPrice',
        'over3AdditionalPrice',
      ])
    })

    it('字面量类型可被 PriceFieldKey 接受', () => {
      const key: PriceFieldKey = 'priceW1Kg'
      expect(key).toBe('priceW1Kg')
    })
  })

  describe('collectPriceValues', () => {
    it('收集全部行所有档位数值', () => {
      const rows = [
        makePrice({ priceW03Kg: 10, priceW1Kg: 20 }),
        makePrice({ priceW05Kg: 15, over3FirstPrice: 30 }),
      ]
      expect(collectPriceValues(rows)).toEqual([10, 20, 15, 30])
    })

    it('忽略空值与 NaN', () => {
      const rows = [
        makePrice({ priceW03Kg: 100, priceW1Kg: null }),
        makePrice({ priceW05Kg: undefined, over3FirstPrice: Number('nan') }),
      ]
      expect(collectPriceValues(rows)).toEqual([100])
    })

    it('空行返回空数组', () => {
      expect(collectPriceValues([])).toEqual([])
    })

    it('自定义字段集合只收集指定档位', () => {
      const rows = [makePrice({ priceW03Kg: 10, priceW1Kg: 20, priceW2Kg: 30 })]
      expect(collectPriceValues(rows, ['priceW03Kg', 'priceW2Kg'])).toEqual([10, 30])
    })
  })

  describe('priceHeatStyle', () => {
    it('空值返回透明', () => {
      expect(priceHeatStyle(null, [makePrice()])).toEqual({ background: 'transparent' })
      expect(priceHeatStyle(undefined, [makePrice()])).toEqual({ background: 'transparent' })
    })

    it('无有效分布时回退淡绿', () => {
      expect(priceHeatStyle(100, [])).toEqual({ background: 'var(--wr-stat-green-bg, #f0fdf4)' })
    })

    it('分布单一（仅一个价位）时回退淡绿', () => {
      expect(priceHeatStyle(100, [makePrice({ priceW03Kg: 100 })])).toEqual({
        background: 'var(--wr-stat-green-bg, #f0fdf4)',
      })
    })

    it('最低价映射淡绿、最高价映射淡橙', () => {
      const rows = [makePrice({ priceW03Kg: 100, priceW1Kg: 200 })]
      expect(priceHeatStyle(100, rows)).toEqual({ background: 'rgb(240, 253, 244)' })
      expect(priceHeatStyle(200, rows)).toEqual({ background: 'rgb(255, 247, 237)' })
    })

    it('中间价按比例插值', () => {
      const rows = [makePrice({ priceW03Kg: 100, priceW1Kg: 200 })]
      expect(priceHeatStyle(150, rows)).toEqual({ background: 'rgb(248, 250, 241)' })
    })
  })
})
