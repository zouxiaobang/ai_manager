import { describe, expect, it } from 'vitest'
import type { EcListingLink, EcListingLinkProduct, EcListingLinkSku } from '@/api/ecommerce/listingLink'
import {
  CARD_SKU_PREVIEW_LIMIT,
  cardImageStack,
  cardSkuOverflow,
  cardSkuPreviews,
  formatProfitChip,
  linkProductIds,
  markStackImageBroken,
  platformStripStyle,
  profitChipClass,
} from '../listingLinkCardView'

function makeLink(partial?: Partial<EcListingLink>): EcListingLink {
  return { id: 1, shopId: 1, name: '链接甲', status: 'ENABLED', ...partial }
}

function makeSku(partial?: Partial<EcListingLinkSku>): EcListingLinkSku {
  return { skuName: 'SKU甲', skuCodes: 'A1', ...partial }
}

function makeProduct(id: number): EcListingLinkProduct {
  return { productId: id }
}

describe('listingLinkCardView 卡片展示域', () => {
  describe('platformStripStyle', () => {
    it('已知平台取专属渐变', () => {
      expect(platformStripStyle(makeLink({ platformName: '淘宝' }))).toEqual({
        background: 'linear-gradient(90deg, #ff6a00, #ff9500)',
      })
    })

    it('未知平台回退蓝色渐变', () => {
      expect(platformStripStyle(makeLink({ platformName: '未知平台' }))).toEqual({
        background: 'linear-gradient(90deg, #2563eb, #3b82f6)',
      })
      expect(platformStripStyle(makeLink({ platformName: '  ' }))).toEqual({
        background: 'linear-gradient(90deg, #2563eb, #3b82f6)',
      })
    })
  })

  describe('linkProductIds', () => {
    it('提取产品 ID；无产品时为空', () => {
      expect(linkProductIds(makeLink({ products: [makeProduct(1), makeProduct(2)] }))).toEqual([1, 2])
      expect(linkProductIds(makeLink())).toEqual([])
    })
  })

  describe('formatProfitChip', () => {
    it('空值占位符', () => {
      expect(formatProfitChip(null)).toBe('—')
      expect(formatProfitChip(undefined)).toBe('—')
    })

    it('正利润带加号、负利润带负号', () => {
      expect(formatProfitChip(20)).toBe('+¥20.00')
      expect(formatProfitChip(-5)).toBe('-¥5.00')
    })
  })

  describe('profitChipClass', () => {
    it('风险与负利润红', () => {
      expect(profitChipClass(makeSku({ pricingRisk: 'BELOW_MIN' }))).toBe('is-danger')
      expect(profitChipClass(makeSku({ pricingRisk: 'NEGATIVE_PROFIT' }))).toBe('is-danger')
      expect(profitChipClass(makeSku({ profit: -1 }))).toBe('is-danger')
    })

    it('正利润绿、零利润灰', () => {
      expect(profitChipClass(makeSku({ profit: 1 }))).toBe('is-success')
      expect(profitChipClass(makeSku({ profit: 0 }))).toBe('is-muted')
    })
  })

  describe('cardSkuPreviews', () => {
    it('previews 优先，取前 limit 条', () => {
      const previews = [makeSku({ skuCodes: 'A1' }), makeSku({ skuCodes: 'B2' }), makeSku({ skuCodes: 'C3' })]
      const row = makeLink({ skus: [makeSku({ skuCodes: 'D4' })] })
      expect(cardSkuPreviews(row, previews)).toEqual([previews[0], previews[1]])
    })

    it('previews 缺省回退行内 skus', () => {
      const row = makeLink({ skus: [makeSku({ skuCodes: 'A1' }), makeSku({ skuCodes: 'B2' })] })
      expect(cardSkuPreviews(row, undefined)).toHaveLength(2)
    })

    it('自定义 limit', () => {
      const previews = [makeSku(), makeSku(), makeSku()]
      expect(cardSkuPreviews(makeLink(), previews, 1)).toHaveLength(1)
    })
  })

  describe('cardSkuOverflow', () => {
    it('超出上限返回折叠数', () => {
      const previews = [makeSku(), makeSku(), makeSku()]
      expect(cardSkuOverflow(makeLink(), previews)).toBe(1)
    })

    it('未超出返回 0', () => {
      expect(cardSkuOverflow(makeLink(), [makeSku()])).toBe(0)
    })

    it('skuCount 优先于行内数量', () => {
      const row = makeLink({ skuCount: 5, skus: [makeSku()] })
      expect(cardSkuOverflow(row, undefined)).toBe(5 - CARD_SKU_PREVIEW_LIMIT)
    })
  })

  describe('cardImageStack', () => {
    it('单产品多 SKU 时按编码铺 SKU 图并去重', () => {
      const row = makeLink({ products: [makeProduct(7)], skus: [makeSku({ skuCodes: 'A1,A1' }), makeSku({ skuCodes: 'B2' })] })
      const previews = row.skus
      const items = cardImageStack(row, previews, {}, { A1: 'sku-a.png', B2: 'sku-b.png' })
      expect(items).toEqual([
        { key: 'sku-A1', imageName: 'sku-a.png' },
        { key: 'sku-B2', imageName: 'sku-b.png' },
      ])
    })

    it('多产品时铺产品图', () => {
      const row = makeLink({ products: [makeProduct(1), makeProduct(2)] })
      const items = cardImageStack(row, undefined, { 1: 'p1.png' }, {})
      expect(items).toEqual([
        { key: 'product-1', imageName: 'p1.png' },
        { key: 'product-2', imageName: undefined },
      ])
    })

    it('单产品单 SKU 时铺产品图', () => {
      expect(cardImageStack(makeLink({ products: [makeProduct(1)] }), undefined, {}, {})).toEqual([
        { key: 'product-1', imageName: undefined },
      ])
    })

    it('无产品时为空', () => {
      expect(cardImageStack(makeLink(), undefined, {}, {})).toEqual([])
    })
  })

  describe('markStackImageBroken', () => {
    it('新 key 追加为新 Set', () => {
      const current = new Set(['a'])
      const next = markStackImageBroken('b', current)
      expect(next).toEqual(new Set(['a', 'b']))
      expect(next).not.toBe(current)
    })

    it('已存在返回原 Set', () => {
      const current = new Set(['a'])
      expect(markStackImageBroken('a', current)).toBe(current)
    })
  })
})
