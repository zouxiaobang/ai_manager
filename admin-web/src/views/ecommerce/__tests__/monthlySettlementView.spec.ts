import { describe, expect, it } from 'vitest'
import type { ExpressBillRecord, MonthlySettlementShopSummary } from '@/api/ecommerce/monthlySettlement'
import {
  buildExpressBillCards,
  buildMaxProfitDisplay,
  computeShopSpan,
  filterExpressBillRecordsByMonth,
  findBestMaxProfitOrder,
  formatExcludeTime,
  formatPrepLastTime,
  formatSettlementPeriodLabel,
  pickLatestPrepTime,
  resolveMaxProfitActualDisplay,
  stationRecordKey,
  statusLabel,
  sumShopMetric,
} from '../monthlySettlementView'

/** 构造快递账单记录 fixture */
function makeRecord(partial: Partial<ExpressBillRecord> & { id: number }): ExpressBillRecord {
  return { billMonth: '2026-08', ...partial }
}

/** 构造店铺汇总 fixture（返回函数以便按需覆盖字段） */
function makeShop(partial?: Partial<MonthlySettlementShopSummary>): MonthlySettlementShopSummary {
  return {
    shopId: 1,
    shopName: '店铺甲',
    totalRevenue: 100,
    maxProfitOrder: { orderId: 10, orderNo: 'SO-1', profitAmount: 50 },
    ...partial,
  }
}

describe('monthlySettlementView 结算展示纯函数', () => {
  describe('stationRecordKey', () => {
    it('其他快递合并为 other', () => {
      expect(stationRecordKey(makeRecord({ id: 1, otherExpress: true }))).toBe('other')
    })

    it('优先站台 id', () => {
      expect(stationRecordKey(makeRecord({ id: 1, expressStationId: 5, expressStationName: '顺丰' }))).toBe('5')
    })

    it('无站台 id 时兜底名称', () => {
      expect(stationRecordKey(makeRecord({ id: 1, expressStationName: '顺丰' }))).toBe('顺丰')
    })

    it('都缺失时兜底记录 id', () => {
      expect(stationRecordKey(makeRecord({ id: 7 }))).toBe('7')
    })
  })

  describe('filterExpressBillRecordsByMonth', () => {
    it('按月份精确匹配（trim 两侧空格）', () => {
      const records = [
        makeRecord({ id: 1, billMonth: '2026-08' }),
        makeRecord({ id: 2, billMonth: ' 2026-08 ' }),
        makeRecord({ id: 3, billMonth: '2026-07' }),
      ]
      expect(filterExpressBillRecordsByMonth(records, '2026-08').map((r) => r.id)).toEqual([1, 2])
    })

    it('空月份列表返回空数组', () => {
      expect(filterExpressBillRecordsByMonth([], '2026-08')).toEqual([])
    })
  })

  describe('buildExpressBillCards', () => {
    const stationMap = new Map<number, { id: number; name: string }>()
    const getStation = (id: number) => stationMap.get(id)

    it('按站台分组生成卡片', () => {
      const records = [
        makeRecord({ id: 1, expressStationId: 5, billMonth: '2026-08', totalRows: 10, matchedRows: 8 }),
        makeRecord({ id: 2, expressStationId: 5, billMonth: '2026-08', totalRows: 20, matchedRows: 15 }),
        makeRecord({ id: 3, expressStationId: 9, billMonth: '2026-08', totalRows: 5, matchedRows: 5 }),
      ]
      const cards = buildExpressBillCards(records, '2026-08', getStation)
      expect(cards).toHaveLength(2)
    })

    it('优先展示有账单行数的批次，避免手动补录 0/0 占位', () => {
      const records = [
        makeRecord({ id: 1, expressStationId: 5, billMonth: '2026-08', totalRows: 0, matchedRows: 0 }),
        makeRecord({ id: 2, expressStationId: 5, billMonth: '2026-08', totalRows: 20, matchedRows: 15 }),
      ]
      const cards = buildExpressBillCards(records, '2026-08', getStation)
      expect(cards[0]).toMatchObject({ id: 2, matched: 15, total: 20 })
    })

    it('gapCount 取该站台全部批次的最大值，为 0 时不输出', () => {
      const records = [
        makeRecord({ id: 1, expressStationId: 5, billMonth: '2026-08', totalRows: 10, gapOrderRows: 2 }),
        makeRecord({ id: 2, expressStationId: 5, billMonth: '2026-08', totalRows: 10, gapOrderRows: 5 }),
        makeRecord({ id: 3, expressStationId: 5, billMonth: '2026-08', totalRows: 10, gapOrderRows: 0 }),
      ]
      const cards = buildExpressBillCards(records, '2026-08', getStation)
      expect(cards[0]?.gapCount).toBe(5)
    })

    it('站台名优先记录名，均缺失时兜底占位符', () => {
      stationMap.set(5, { id: 5, name: '顺丰站台' })
      const named = buildExpressBillCards(
        [makeRecord({ id: 1, expressStationId: 5, billMonth: '2026-08', expressStationName: '记录名' })],
        '2026-08',
        getStation,
      )
      expect(named[0]?.name).toBe('记录名')
      const noName = buildExpressBillCards(
        [makeRecord({ id: 2, expressStationId: 5, billMonth: '2026-08' })],
        '2026-08',
        getStation,
      )
      expect(noName[0]?.name).toBe('顺丰站台')
      const fallback = buildExpressBillCards(
        [makeRecord({ id: 3, billMonth: '2026-08' })],
        '2026-08',
        getStation,
      )
      expect(fallback[0]?.name).toBe('—')
    })

    it('不同月份记录不进入卡片', () => {
      const records = [makeRecord({ id: 1, expressStationId: 5, billMonth: '2026-07' })]
      expect(buildExpressBillCards(records, '2026-08', getStation)).toEqual([])
    })
  })

  describe('sumShopMetric', () => {
    it('求和并忽略空值', () => {
      const shops = [
        makeShop({ totalRevenue: 100 }),
        makeShop({ shopId: 2, totalRevenue: undefined }),
        makeShop({ shopId: 3, totalRevenue: 50 }),
      ]
      expect(sumShopMetric(shops, (s) => s.totalRevenue)).toBe(150)
    })

    it('空数组返回 0', () => {
      expect(sumShopMetric([], (s) => s.totalRevenue)).toBe(0)
    })
  })

  describe('formatExcludeTime', () => {
    it('ISO 去 T 截断到秒', () => {
      expect(formatExcludeTime('2026-08-03T10:20:30.123Z')).toBe('2026-08-03 10:20:30')
    })

    it('空值返回空串', () => {
      expect(formatExcludeTime('')).toBe('')
      expect(formatExcludeTime(undefined)).toBe('')
    })
  })

  describe('formatPrepLastTime', () => {
    it('空值返回 undefined', () => {
      expect(formatPrepLastTime('')).toBeUndefined()
      expect(formatPrepLastTime(null)).toBeUndefined()
      expect(formatPrepLastTime(undefined)).toBeUndefined()
    })

    it('合法时间返回格式串', () => {
      const result = formatPrepLastTime('2026-08-03 10:20:30')
      expect(result).not.toBeUndefined()
      expect(result).toMatch(/^\d{4}-\d{2}-\d{2}/)
    })
  })

  describe('pickLatestPrepTime', () => {
    it('从多候选取最晚时间', () => {
      const result = pickLatestPrepTime('2026-08-01 10:00:00', '2026-08-03 09:00:00', '2026-08-02 12:00:00')
      expect(result).toMatch(/2026-08-03 09:00/)
    })

    it('忽略非法与空候选', () => {
      const result = pickLatestPrepTime(null, undefined, '', 'garbage', '2026-08-01 08:00:00')
      expect(result).toMatch(/2026-08-01/)
    })

    it('全部候选非法返回 undefined', () => {
      expect(pickLatestPrepTime(undefined, '', null)).toBeUndefined()
    })
  })

  describe('formatSettlementPeriodLabel', () => {
    it('2026-08 → 2026年08月（月份保持两位，与结算月份一致）', () => {
      expect(formatSettlementPeriodLabel('2026-08')).toBe('2026年08月')
    })

    it('非法格式原样返回', () => {
      expect(formatSettlementPeriodLabel('abc')).toBe('abc')
      expect(formatSettlementPeriodLabel('2026')).toBe('2026')
    })

    it('空值返回空串', () => {
      expect(formatSettlementPeriodLabel('')).toBe('')
    })
  })

  describe('findBestMaxProfitOrder', () => {
    it('返回利润最高的有效订单并附店铺信息', () => {
      const shops = [
        makeShop({ shopId: 1, maxProfitOrder: { orderId: 1, orderNo: 'A', profitAmount: 10 } }),
        makeShop({ shopId: 2, maxProfitOrder: { orderId: 2, orderNo: 'B', profitAmount: 99 } }),
        makeShop({ shopId: 3, maxProfitOrder: { orderId: 3, orderNo: 'C', profitAmount: 50 } }),
      ]
      const best = findBestMaxProfitOrder(shops)
      expect(best).toMatchObject({ orderId: 2, shopId: 2, shopName: '店铺甲' })
    })

    it('跳过无订单号（平台号）的订单', () => {
      const shops = [
        makeShop({ shopId: 1, maxProfitOrder: { orderId: 1, profitAmount: 999 } }),
        makeShop({ shopId: 2, maxProfitOrder: { orderId: 2, orderNo: 'B', profitAmount: 5 } }),
      ]
      const best = findBestMaxProfitOrder(shops)
      expect(best).toMatchObject({ orderId: 2 })
    })

    it('无有效订单返回 null', () => {
      expect(findBestMaxProfitOrder([])).toBeNull()
      expect(findBestMaxProfitOrder([makeShop({ maxProfitOrder: { orderId: 1 } })])).toBeNull()
    })

    it('店铺无名称时用 #id 兜底', () => {
      const best = findBestMaxProfitOrder([
        makeShop({ shopId: 7, shopName: undefined, maxProfitOrder: { orderId: 1, orderNo: 'A', profitAmount: 1 } }),
      ])
      expect(best?.shopName).toBe('#7')
    })
  })

  describe('buildMaxProfitDisplay', () => {
    const shops = [
      makeShop({ shopId: 1, maxProfitOrder: { orderId: 1, orderNo: 'A', profitAmount: 10 } }),
      makeShop({ shopId: 2, maxProfitOrder: { orderId: 2, orderNo: 'B', profitAmount: 99 } }),
    ]

    it('showAll 返回全局最优', () => {
      const result = buildMaxProfitDisplay(shops, 1, true)
      expect(result).toMatchObject({ orderId: 2 })
    })

    it('非 showAll 返回选中店铺订单', () => {
      const result = buildMaxProfitDisplay(shops, 1, false)
      expect(result).toMatchObject({ orderId: 1, shopId: 1 })
    })

    it('无选中店铺返回 null', () => {
      expect(buildMaxProfitDisplay(shops, null, false)).toBeNull()
    })

    it('选中店铺无有效订单返回 null', () => {
      expect(buildMaxProfitDisplay([makeShop({ maxProfitOrder: { orderId: 1 } })], 1, false)).toBeNull()
    })
  })

  describe('computeShopSpan', () => {
    it('已导入店铺各列占 1', () => {
      expect(computeShopSpan(0, true)).toEqual({ rowspan: 1, colspan: 1 })
      expect(computeShopSpan(3, true)).toEqual({ rowspan: 1, colspan: 1 })
    })

    it('未导入首列横向占 6 列，后续列隐藏', () => {
      expect(computeShopSpan(1, false)).toEqual({ rowspan: 1, colspan: 6 })
      expect(computeShopSpan(2, false)).toEqual({ rowspan: 0, colspan: 0 })
      expect(computeShopSpan(5, false)).toEqual({ rowspan: 0, colspan: 0 })
    })

    it('未导入第 0 列占 1', () => {
      expect(computeShopSpan(0, false)).toEqual({ rowspan: 1, colspan: 1 })
    })
  })

  describe('statusLabel', () => {
    const options = [
      { value: 'PAID', label: '已付款' },
      { value: 'SHIPPED', label: '已发货' },
    ]

    it('命中返回选项文案', () => {
      expect(statusLabel('PAID', options)).toBe('已付款')
    })

    it('未命中回退原值', () => {
      expect(statusLabel('DRAFT', options)).toBe('DRAFT')
    })

    it('空值回退占位符', () => {
      expect(statusLabel(undefined, options)).toBe('—')
    })
  })

  describe('resolveMaxProfitActualDisplay', () => {
    const t = (key: string) => key

    it('空 item 显示占位符', () => {
      expect(resolveMaxProfitActualDisplay(null, true, t)).toEqual({ text: '—', unknown: false })
    })

    it('未知原因优先翻译', () => {
      const item = { shopId: 1, shopName: '店铺甲', actualProfitUnknownReason: 'ACTUAL_FREIGHT_MISSING' as const }
      expect(resolveMaxProfitActualDisplay(item, true, t)).toEqual({
        text: 'ecommerce.monthlySettlement.maxProfitUnknownReason.ACTUAL_FREIGHT_MISSING',
        unknown: true,
      })
    })

    it('有实际利润时格式化金额', () => {
      const item = { shopId: 1, shopName: '店铺甲', actualProfitAmount: 20 }
      expect(resolveMaxProfitActualDisplay(item, false, t)).toEqual({ text: '¥20.00', unknown: false })
    })

    it('金额缺失且快递未导入提示先导入', () => {
      const item = { shopId: 1, shopName: '店铺甲' }
      expect(resolveMaxProfitActualDisplay(item, false, t)).toEqual({
        text: 'ecommerce.monthlySettlement.maxProfitUnknownReason.EXPRESS_BILL_NOT_IMPORTED',
        unknown: true,
      })
    })

    it('金额缺失且快递已导入提示运费缺失', () => {
      const item = { shopId: 1, shopName: '店铺甲' }
      expect(resolveMaxProfitActualDisplay(item, true, t)).toEqual({
        text: 'ecommerce.monthlySettlement.maxProfitUnknownReason.ACTUAL_FREIGHT_MISSING',
        unknown: true,
      })
    })
  })
})
