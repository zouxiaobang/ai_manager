import { describe, expect, it } from 'vitest'
import type {
  EcExpressNotice,
  EcExpressPrice,
  EcExpressStation,
} from '@/api/ecommerce/express'
import {
  buildNoticeSavePayload,
  buildPriceSavePayload,
  buildRecentPriceRegions,
  buildStationSavePayload,
  computeNoticeReorders,
  filterPricesByKeyword,
  filterPricesByRegions,
  pickPriceValues,
  resolveNoticeCount,
  resolveRegionCount,
  syncRowCounts,
  type PriceFormValue,
} from '../expressPanelView'

/** 价格行 fixture：档位全空，便于按需覆盖 */
function makePrice(partial?: Partial<EcExpressPrice>): EcExpressPrice {
  return { id: 1, stationId: 1, provinceName: '广东', ...partial }
}

/** 通知 fixture */
function makeNotice(partial?: Partial<EcExpressNotice>): EcExpressNotice {
  return { id: 1, stationId: 1, content: '通知', ...partial }
}

/** 站点 fixture */
function makeStation(partial?: Partial<EcExpressStation>): EcExpressStation {
  return { id: 1, name: '站点甲', ...partial }
}

describe('expressPanelView 快递面板纯函数域', () => {
  describe('filterPricesByRegions', () => {
    it('无区域筛选返回全部', () => {
      const rows = [makePrice(), makePrice({ id: 2, provinceName: '浙江' })]
      expect(filterPricesByRegions(rows, [])).toEqual(rows)
    })

    it('空列表返回空数组', () => {
      expect(filterPricesByRegions(undefined, ['广东'])).toEqual([])
      expect(filterPricesByRegions([], ['广东'])).toEqual([])
    })

    it('按选中区域精确过滤（trim 后匹配）', () => {
      const rows = [
        makePrice(),
        makePrice({ id: 2, provinceName: ' 广东 ' }),
        makePrice({ id: 3, provinceName: '浙江' }),
      ]
      expect(filterPricesByRegions(rows, ['广东'])).toEqual([rows[0], rows[1]])
      expect(filterPricesByRegions(rows, [' 广东 '])).toEqual([rows[0], rows[1]])
    })
  })

  describe('filterPricesByKeyword', () => {
    it('空关键词返回全部', () => {
      const rows = [makePrice(), makePrice({ id: 2, provinceName: '浙江' })]
      expect(filterPricesByKeyword(rows, '')).toEqual(rows)
      expect(filterPricesByKeyword(rows, '   ')).toEqual(rows)
    })

    it('省份名不区分大小写包含匹配', () => {
      const rows = [makePrice(), makePrice({ id: 2, provinceName: '内蒙古' })]
      expect(filterPricesByKeyword(rows, '广')).toEqual([rows[0]])
      expect(filterPricesByKeyword(rows, ' 广 ')).toEqual([rows[0]])
      expect(filterPricesByKeyword(rows, '古')).toEqual([rows[1]])
    })

    it('无匹配返回空数组', () => {
      expect(filterPricesByKeyword([makePrice()], '不存在省')).toEqual([])
    })
  })

  describe('buildRecentPriceRegions', () => {
    it('去重后取末 count 条并倒序', () => {
      const rows = ['广东', '浙江', '广东', '江苏', '山东', '四川', '湖北'].map((p, i) =>
        makePrice({ id: i + 1, provinceName: p }),
      )
      // 去重：广东、浙江、江苏、山东、四川、湖北 → 取末 6：浙江? 不，保持首次顺序去重后是 广东,浙江,江苏,山东,四川,湖北
      expect(buildRecentPriceRegions(rows)).toEqual(['湖北', '四川', '山东', '江苏', '浙江', '广东'])
    })

    it('count 参数控制返回条数', () => {
      const rows = ['广东', '浙江', '江苏'].map((p, i) => makePrice({ id: i + 1, provinceName: p }))
      expect(buildRecentPriceRegions(rows, 2)).toEqual(['江苏', '浙江'])
    })

    it('忽略空省份名', () => {
      const rows = [makePrice(), makePrice({ id: 2, provinceName: '  ' })]
      expect(buildRecentPriceRegions(rows)).toEqual(['广东'])
    })
  })

  describe('computeNoticeReorders', () => {
    it('同序或越界返回 null', () => {
      const rows = [makeNotice(), makeNotice({ id: 2 })]
      expect(computeNoticeReorders(rows, 0, 0)).toBeNull()
      expect(computeNoticeReorders([], 0, 1)).toBeNull()
      expect(computeNoticeReorders(rows, -1, 1)).toBeNull()
    })

    it('移动后重排列表并按新位置写 sortOrder，返回顺序变化条目', () => {
      const rows = [
        makeNotice({ id: 1, sortOrder: 0 }),
        makeNotice({ id: 2, sortOrder: 1 }),
        makeNotice({ id: 3, sortOrder: 2 }),
      ]
      // 0 → 2：id1 移到末尾，三个条目 sortOrder 全部位移
      const result = computeNoticeReorders(rows, 0, 2)
      expect(result).not.toBeNull()
      expect(result!.ordered.map((n) => n.id)).toEqual([2, 3, 1])
      expect(result!.ordered.map((n) => n.sortOrder)).toEqual([0, 1, 2])
      expect(result!.updates.map((n) => n.id)).toEqual([2, 3, 1])
      // 原列表不被篡改
      expect(rows.map((n) => n.sortOrder)).toEqual([0, 1, 2])
    })

    it('相邻移动 1 → 2 仅两条顺移', () => {
      const rows = [
        makeNotice({ id: 1, sortOrder: 0 }),
        makeNotice({ id: 2, sortOrder: 1 }),
        makeNotice({ id: 3, sortOrder: 2 }),
      ]
      const result = computeNoticeReorders(rows, 1, 2)
      expect(result!.ordered.map((n) => n.id)).toEqual([1, 3, 2])
      expect(result!.updates.map((n) => n.id)).toEqual([3, 2])
    })
  })

  describe('表单 payload 构造', () => {
    it('buildStationSavePayload 去空串并 trim', () => {
      const payload = buildStationSavePayload({
        name: ' 站点甲 ',
        avatarUrl: ' http://x ',
        contact: ' ',
        address: undefined,
        labelPrice: 5,
        isDefault: true,
        nameAliases: [' 别名A ', '', '别名B'],
      })
      expect(payload).toEqual({
        name: '站点甲',
        avatarUrl: 'http://x',
        contact: undefined,
        address: undefined,
        labelPrice: 5,
        isDefault: true,
        nameAliases: ['别名A', '别名B'],
      })
    })

    it('buildPriceSavePayload 省份 trim 其余原样透传', () => {
      const form: PriceFormValue = {
        provinceName: ' 广东 ',
        priceW03Kg: 6,
        priceW05Kg: null,
        priceW1Kg: 8,
        priceW15Kg: null,
        priceW2Kg: null,
        priceW25Kg: null,
        priceW3Kg: null,
        over3FirstPrice: null,
        over3AdditionalPrice: null,
      }
      const payload = buildPriceSavePayload(form, 7)
      expect(payload.stationId).toBe(7)
      expect(payload.provinceName).toBe('广东')
      expect(payload.priceW03Kg).toBe(6)
      expect(payload.priceW1Kg).toBe(8)
      expect(payload.priceW05Kg).toBeNull()
    })

    it('buildNoticeSavePayload 内容 trim 其余原样透传', () => {
      const payload = buildNoticeSavePayload({ content: ' 通知内容 ', highlightRed: true, sortOrder: 3 }, 7)
      expect(payload).toEqual({ stationId: 7, content: '通知内容', highlightRed: true, sortOrder: 3 })
    })
  })

  describe('行计数与复制', () => {
    it('resolveRegionCount 行级计数优先，否则回退详情', () => {
      expect(resolveRegionCount(makeStation({ priceCount: 3 }), null)).toBe(3)
      expect(resolveRegionCount(makeStation(), makeStation({ prices: [makePrice(), makePrice({ id: 2 })] }))).toBe(2)
      expect(resolveRegionCount(makeStation(), null)).toBe(0)
    })

    it('resolveNoticeCount 行级计数优先，否则回退详情', () => {
      expect(resolveNoticeCount(makeStation({ noticeCount: 3 }), null)).toBe(3)
      expect(resolveNoticeCount(makeStation(), makeStation({ notices: [makeNotice(), makeNotice({ id: 2 })] }))).toBe(2)
      expect(resolveNoticeCount(makeStation(), null)).toBe(0)
    })

    it('syncRowCounts 用详情覆盖行级计数', () => {
      const row = makeStation({ priceCount: 1, noticeCount: 1 })
      syncRowCounts(row, makeStation({ prices: [makePrice()], notices: [] }))
      expect(row.priceCount).toBe(1)
      expect(row.noticeCount).toBe(0)
    })

    it('pickPriceValues 只取指定档位键，空值落 null', () => {
      const source = makePrice({ priceW03Kg: 6, priceW1Kg: 8, priceW2Kg: null })
      const result = pickPriceValues(source, ['priceW03Kg', 'priceW1Kg', 'priceW2Kg'])
      expect(result).toEqual({ priceW03Kg: 6, priceW1Kg: 8, priceW2Kg: null })
    })
  })
})
