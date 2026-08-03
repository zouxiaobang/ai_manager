import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import { useCountingLoading } from '@/composables/useCountingLoading'
import type { EcExpressStation } from '@/api/ecommerce/express'
import type { EcSalesOrderMonthlyOverview } from '@/api/ecommerce/salesOrder'
import type { EcShop } from '@/api/ecommerce/shop'
import type { ExpressBillRecord, MonthlySettlementShopSummary } from '@/api/ecommerce/monthlySettlement'
import {
  useMonthlySettlementLoad,
  type MonthlySettlementLoadDeps,
  type SettlementResultData,
} from '../useMonthlySettlementLoad'

/** 订单月度总览 fixture */
function makeOverview(partial?: Partial<EcSalesOrderMonthlyOverview>): EcSalesOrderMonthlyOverview {
  return {
    orderMonth: '2026-08',
    totalOrderCount: 0,
    importedShopCount: 0,
    totalShopCount: 0,
    pendingReviewCount: 0,
    shops: [],
    ...partial,
  }
}

/** 店铺汇总 fixture */
function makeShopSummary(partial?: Partial<MonthlySettlementShopSummary>): MonthlySettlementShopSummary {
  return { shopId: 1, shopName: '店铺甲', ...partial }
}

/** 快递账单记录 fixture */
function makeBill(partial?: Partial<ExpressBillRecord>): ExpressBillRecord {
  return { id: 1, billMonth: '2026-08', ...partial }
}

function setup(partial?: Partial<MonthlySettlementLoadDeps>) {
  const settlementMonth = ref('2026-08')
  const calculated = ref(false)
  const result = ref<SettlementResultData | null>(null)
  const selectedShopId = ref<number | null>(null)
  const maxProfitShowAll = ref(false)
  const lastCalculatedAt = ref<string | null>(null)
  const orderOverview = ref<EcSalesOrderMonthlyOverview | null>(null)
  const expressBillRecords = ref<ExpressBillRecord[]>([])
  const expressBillImported = ref(false)
  const shopOptions = ref<EcShop[]>([])
  const expressStations = ref<EcExpressStation[]>([])

  const calculating = ref(false)
  const submitting = ref(false)
  const prepLoading = ref(false)
  const { begin: beginCalculating, end: endCalculating } = useCountingLoading(calculating)
  const { begin: beginSubmitting, end: endSubmitting } = useCountingLoading(submitting)
  const { begin: beginPrepLoading, end: endPrepLoading, reset: resetPrepLoading } = useCountingLoading(prepLoading)

  const actions = {
    clearSnapshot: vi.fn(),
    loadSnapshot: vi.fn().mockResolvedValue(undefined),
    syncPendingDecisions: vi.fn(),
    syncSelectedShop: vi.fn(),
  }

  const mocks = {
    fetchShopOptions: vi.fn().mockResolvedValue([]),
    fetchExpressStations: vi.fn().mockResolvedValue([]),
    fetchSalesOrderMonthlyOverview: vi.fn().mockResolvedValue(makeOverview()),
    fetchExpressBillImported: vi.fn().mockResolvedValue(false),
    fetchExpressBillRecords: vi.fn().mockResolvedValue([]),
    calculateMonthlySettlement: vi.fn().mockResolvedValue({ shops: [] }),
    fetchMonthlySettlementSnapshot: vi.fn().mockResolvedValue(null),
    notifyWarning: vi.fn(),
    notifySuccess: vi.fn(),
  }

  const api = useMonthlySettlementLoad({
    t: (key: string) => key,
    settlementMonth,
    calculated,
    result,
    selectedShopId,
    maxProfitShowAll,
    lastCalculatedAt,
    orderOverview,
    expressBillRecords,
    expressBillImported,
    shopOptions,
    expressStations,
    beginCalculating,
    endCalculating,
    beginSubmitting,
    endSubmitting,
    beginPrepLoading,
    endPrepLoading,
    resetPrepLoading,
    clearSnapshot: actions.clearSnapshot,
    loadSnapshot: actions.loadSnapshot,
    syncPendingDecisions: actions.syncPendingDecisions,
    syncSelectedShop: actions.syncSelectedShop,
    ...mocks,
    ...partial,
  })

  return {
    api,
    refs: {
      settlementMonth,
      calculated,
      result,
      selectedShopId,
      maxProfitShowAll,
      lastCalculatedAt,
      orderOverview,
      expressBillRecords,
      expressBillImported,
      shopOptions,
      expressStations,
      calculating,
      submitting,
      prepLoading,
    },
    actions,
    mocks,
  }
}

describe('useMonthlySettlementLoad 加载编排状态机', () => {
  describe('applySettlementResult', () => {
    it('null 清空页面状态且不同步决策', () => {
      const { api, refs, actions } = setup()
      refs.result.value = { shops: [makeShopSummary()] }
      refs.calculated.value = true
      refs.selectedShopId.value = 1
      refs.lastCalculatedAt.value = 'x'
      api.applySettlementResult(null)
      expect(refs.result.value).toBeNull()
      expect(refs.calculated.value).toBe(false)
      expect(refs.selectedShopId.value).toBeNull()
      expect(refs.maxProfitShowAll.value).toBe(false)
      expect(refs.lastCalculatedAt.value).toBeNull()
      expect(actions.syncPendingDecisions).not.toHaveBeenCalled()
    })

    it('正常结果写入并同步决策表/选中店铺/计算时间', () => {
      const { api, refs, actions } = setup()
      const shops = [makeShopSummary()]
      api.applySettlementResult({ shops, calculatedAt: '2026-08-01T00:00:00.000Z' })
      expect(refs.result.value).toEqual({ shops, calculatedAt: '2026-08-01T00:00:00.000Z' })
      expect(refs.calculated.value).toBe(true)
      expect(refs.lastCalculatedAt.value).toBe('2026-08-01T00:00:00.000Z')
      expect(actions.syncPendingDecisions).toHaveBeenCalledWith(shops)
      expect(actions.syncSelectedShop).toHaveBeenCalled()
    })

    it('缺 calculatedAt 时用当前时间', () => {
      const { api, refs } = setup()
      api.applySettlementResult({ shops: [] })
      expect(refs.lastCalculatedAt.value).toMatch(/^\d{4}-\d{2}-\d{2}T/)
    })
  })

  describe('loadPrepData', () => {
    it('空月份清空预备状态并重置 loading', () => {
      const { api, refs, actions } = setup({ settlementMonth: ref('') })
      refs.orderOverview.value = makeOverview()
      refs.expressBillRecords.value = [makeBill()]
      refs.expressBillImported.value = true
      api.loadPrepData()
      expect(refs.orderOverview.value).toBeNull()
      expect(refs.expressBillRecords.value).toEqual([])
      expect(refs.expressBillImported.value).toBe(false)
      expect(actions.clearSnapshot).toHaveBeenCalled()
      // reset 而非计数归零，不残留旧计数
      expect(refs.prepLoading.value).toBe(false)
    })

    it('正常加载写入预备数据', async () => {
      const { api, refs, mocks, actions } = setup()
      const overview = makeOverview({ totalOrderCount: 5 })
      mocks.fetchSalesOrderMonthlyOverview.mockResolvedValue(overview)
      mocks.fetchExpressBillImported.mockResolvedValue(true)
      mocks.fetchExpressBillRecords.mockResolvedValue([makeBill()])
      await api.loadPrepData()
      expect(refs.orderOverview.value).toEqual(overview)
      expect(refs.expressBillImported.value).toBe(true)
      expect(refs.expressBillRecords.value).toEqual([makeBill()])
      expect(actions.loadSnapshot).toHaveBeenCalled()
      expect(refs.prepLoading.value).toBe(false)
    })

    it('竞态：旧请求迟到结果被丢弃', async () => {
      const { api, refs, mocks } = setup()
      let releaseOld!: (v: EcSalesOrderMonthlyOverview) => void
      mocks.fetchSalesOrderMonthlyOverview
        .mockImplementationOnce(() => new Promise((res) => { releaseOld = res }))
        .mockResolvedValueOnce(makeOverview({ totalOrderCount: 9 }))
      const first = api.loadPrepData() // seq=1 挂起
      await api.loadPrepData() // seq=2 立即完成
      releaseOld(makeOverview({ totalOrderCount: 1 })) // seq=1 迟到，应丢弃
      await first
      expect(refs.orderOverview.value?.totalOrderCount).toBe(9)
    })

    it('加载失败清空预备数据', async () => {
      const { api, refs, mocks, actions } = setup()
      mocks.fetchSalesOrderMonthlyOverview.mockRejectedValue(new Error('boom'))
      refs.orderOverview.value = makeOverview()
      await api.loadPrepData()
      expect(refs.orderOverview.value).toBeNull()
      expect(refs.expressBillImported.value).toBe(false)
      expect(refs.expressBillRecords.value).toEqual([])
      expect(actions.clearSnapshot).toHaveBeenCalled()
      expect(refs.prepLoading.value).toBe(false)
    })
  })

  describe('runPageLoad', () => {
    it('manual 空月份提示月份必选', async () => {
      const { api, mocks } = setup({ settlementMonth: ref('') })
      await api.runPageLoad('manual')
      expect(mocks.notifyWarning).toHaveBeenCalledWith('ecommerce.monthlySettlement.monthRequired')
      expect(mocks.calculateMonthlySettlement).not.toHaveBeenCalled()
    })

    it('auto 空月份静默返回', async () => {
      const { api, mocks } = setup({ settlementMonth: ref('') })
      await api.runPageLoad('auto')
      expect(mocks.notifyWarning).not.toHaveBeenCalled()
      expect(mocks.fetchMonthlySettlementSnapshot).not.toHaveBeenCalled()
    })

    it('auto 读取快照并应用结果', async () => {
      const { api, refs, mocks } = setup()
      const data = { shops: [makeShopSummary()] }
      mocks.fetchMonthlySettlementSnapshot.mockResolvedValue(data)
      await api.runPageLoad('auto')
      expect(mocks.fetchMonthlySettlementSnapshot).toHaveBeenCalledWith('2026-08')
      expect(refs.result.value).toEqual(data)
      expect(refs.calculated.value).toBe(true)
      expect(refs.calculating.value).toBe(false)
      expect(mocks.notifySuccess).not.toHaveBeenCalled()
    })

    it('manual 计算并保存，提示成功', async () => {
      const { api, refs, mocks } = setup()
      mocks.calculateMonthlySettlement.mockResolvedValue({ shops: [] })
      await api.runPageLoad('manual')
      expect(mocks.calculateMonthlySettlement).toHaveBeenCalledWith('2026-08')
      expect(refs.calculated.value).toBe(true)
      expect(mocks.notifySuccess).toHaveBeenCalledWith('ecommerce.monthlySettlement.calculateSaved')
      expect(refs.calculating.value).toBe(false)
      expect(refs.submitting.value).toBe(false)
    })

    it('auto 加载失败清空结果', async () => {
      const { api, refs, mocks } = setup()
      mocks.fetchMonthlySettlementSnapshot.mockRejectedValue(new Error('boom'))
      refs.result.value = { shops: [makeShopSummary()] }
      await api.runPageLoad('auto')
      expect(refs.result.value).toBeNull()
    })

    it('并发调用复用同一加载', async () => {
      const { api, mocks } = setup()
      let release!: (v: SettlementResultData | null) => void
      mocks.fetchMonthlySettlementSnapshot.mockImplementationOnce(
        () => new Promise((res) => { release = res }),
      )
      const p1 = api.runPageLoad('auto')
      const p2 = api.runPageLoad('auto')
      // 等 p1 走完预备数据（Promise.all 已 resolve）挂到 fetchSnapshot，此时 release 才就绪
      await vi.waitFor(() => {
        expect(release).toBeTypeOf('function')
      })
      release({ shops: [] })
      await Promise.all([p1, p2])
      expect(mocks.fetchMonthlySettlementSnapshot).toHaveBeenCalledTimes(1)
    })
  })

  describe('enter / mount', () => {
    it('首屏引导填充选项并读取快照', async () => {
      const { api, refs, mocks } = setup()
      mocks.fetchShopOptions.mockResolvedValue([{ id: 1, name: '店铺甲', platformId: 9, status: 'ENABLED' }])
      mocks.fetchExpressStations.mockResolvedValue([{ id: 3, name: '站' } as EcExpressStation])
      await api.enter()
      expect(refs.shopOptions.value).toHaveLength(1)
      expect(refs.expressStations.value).toHaveLength(1)
      expect(mocks.fetchMonthlySettlementSnapshot).toHaveBeenCalled()
    })

    it('选项已加载不重复请求', async () => {
      const { api, refs, mocks } = setup()
      refs.shopOptions.value = [{ id: 1, name: '店铺甲', platformId: 9, status: 'ENABLED' }]
      refs.expressStations.value = [{ id: 3, name: '站' } as EcExpressStation]
      await api.enter()
      expect(mocks.fetchShopOptions).not.toHaveBeenCalled()
      expect(mocks.fetchExpressStations).not.toHaveBeenCalled()
      expect(mocks.fetchMonthlySettlementSnapshot).toHaveBeenCalled()
    })

    it('mount 复用进行中的引导并解锁月份监听', async () => {
      const { api } = setup()
      expect(api.ignoreMonthWatch.value).toBe(true)
      await api.mount()
      expect(api.ignoreMonthWatch.value).toBe(false)
    })
  })
})
