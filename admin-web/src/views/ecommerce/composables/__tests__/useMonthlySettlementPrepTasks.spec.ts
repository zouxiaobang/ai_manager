import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import type { EcExpressStation } from '@/api/ecommerce/express'
import type { ExpressBillRecord, MonthlySettlementShopSummary } from '@/api/ecommerce/monthlySettlement'
import type { EcSalesOrderMonthlyOverview } from '@/api/ecommerce/salesOrder'
import {
  useMonthlySettlementPrepTasks,
  type MonthlySettlementPrepTasksDeps,
} from '../useMonthlySettlementPrepTasks'

function makeBill(partial?: Partial<ExpressBillRecord>): ExpressBillRecord {
  return { id: 1, billMonth: '2026-08', createTime: '2026-08-01 10:00:00', ...partial }
}

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

function makeShopSummary(partial?: Partial<MonthlySettlementShopSummary>): MonthlySettlementShopSummary {
  return { shopId: 1, shopName: '店铺甲', ...partial }
}

type Actions = {
  openExpressBillDialog: ReturnType<typeof vi.fn>
  openBuyerExcludeDialog: ReturnType<typeof vi.fn>
  goReviewPending: ReturnType<typeof vi.fn>
  goImportOrders: ReturnType<typeof vi.fn>
}

function setup(partial?: Partial<MonthlySettlementPrepTasksDeps>) {
  const actions: Actions = {
    openExpressBillDialog: vi.fn(),
    openBuyerExcludeDialog: vi.fn(),
    goReviewPending: vi.fn(),
    goImportOrders: vi.fn(),
  }
  const deps: MonthlySettlementPrepTasksDeps = {
    t: (key: string) => key,
    orderOverview: ref<EcSalesOrderMonthlyOverview | null>(null),
    expressBillRecords: ref<ExpressBillRecord[]>([]),
    expressStationMap: ref<Map<number, EcExpressStation>>(new Map()),
    settlementMonth: ref('2026-08'),
    expressBillImported: ref(false),
    buyerExcludeCount: ref(0),
    buyerExcludesSnapshot: ref([]),
    lastBuyerExcludeOpAt: ref<string | null>(null),
    totalPendingOrders: ref(0),
    calculated: ref(false),
    shopSummaries: ref<MonthlySettlementShopSummary[]>([]),
    lastPendingDecisionAt: ref<string | null>(null),
    lastCalculatedAt: ref<string | null>(null),
    ...actions,
    ...partial,
  }
  const api = useMonthlySettlementPrepTasks(deps)
  return { api, deps, actions }
}

/** 订单 / 快递 / 买家排除 / 待定订单 四张卡片的快捷读取 */
function readTasks(api: ReturnType<typeof useMonthlySettlementPrepTasks>) {
  const list = api.prepTasks.value
  return { orders: list[0], express: list[1], exclude: list[2], pending: list[3] }
}

describe('useMonthlySettlementPrepTasks 结算前准备清单', () => {
  describe('订单域', () => {
    it('未导入：红色未导入态 + 提示文案', () => {
      const { api } = setup()
      const { orders } = readTasks(api)
      expect(orders.tone).toBe('danger')
      expect(orders.statusTag).toEqual({
        label: 'ecommerce.monthlySettlement.prepStatusNotImported',
        type: 'danger',
      })
      expect(orders.desc).toBe('ecommerce.monthlySettlement.salesOrdersNotImportedHint')
    })

    it('待审核：黄色待审核态 + 导入摘要', () => {
      const { api } = setup({
        orderOverview: ref(makeOverview({ totalOrderCount: 5, importedShopCount: 2, pendingReviewCount: 3 })),
      })
      const { orders } = readTasks(api)
      expect(orders.tone).toBe('warning')
      expect(orders.statusTag).toEqual({
        label: 'ecommerce.monthlySettlement.prepStatusPendingReview',
        type: 'warning',
      })
      expect(orders.descHighlight).toContain('salesOrdersImportedCount')
    })

    it('已导入无待审：绿色已完成态', () => {
      const { api } = setup({
        orderOverview: ref(makeOverview({ totalOrderCount: 5, importedShopCount: 2 })),
      })
      const { orders } = readTasks(api)
      expect(orders.tone).toBe('success')
      expect(orders.statusTag).toEqual({
        label: 'ecommerce.monthlySettlement.prepStatusCompleted',
        type: 'success',
      })
    })

    it('动作绑定跳转订单导入', () => {
      const { api, actions } = setup()
      const { orders } = readTasks(api)
      orders.action?.()
      expect(actions.goImportOrders).toHaveBeenCalled()
    })
  })

  describe('快递账单域', () => {
    it('未导入无记录：红色未导入态 + 导入提示', () => {
      const { api } = setup()
      const { express } = readTasks(api)
      expect(express.tone).toBe('danger')
      expect(express.statusTag).toEqual({
        label: 'ecommerce.monthlySettlement.prepStatusNotImported',
        type: 'danger',
      })
      expect(express.desc).toBe('ecommerce.monthlySettlement.expressBillImportPrompt')
    })

    it('已导入有缺口：黄色待补充态', () => {
      const { api } = setup({
        expressBillImported: ref(true),
        expressBillRecords: ref([makeBill({ gapOrderRows: 2, unmatchedRows: 1, matchedRows: 3 })]),
      })
      const { express } = readTasks(api)
      expect(express.tone).toBe('warning')
      expect(express.statusTag).toEqual({
        label: 'ecommerce.monthlySettlement.prepStatusPendingFill',
        type: 'warning',
      })
      expect(express.expressBillCards).toBeDefined()
    })

    it('已导入无缺口：绿色已完成态 + 匹配摘要', () => {
      const { api } = setup({
        expressBillImported: ref(true),
        expressBillRecords: ref([makeBill({ matchedRows: 4 })]),
      })
      const { express } = readTasks(api)
      expect(express.tone).toBe('success')
      expect(express.desc).toContain('ImportedSummary')
    })

    it('动作绑定打开快递账单对话框', () => {
      const { api, actions } = setup()
      const { express } = readTasks(api)
      express.action?.()
      expect(actions.openExpressBillDialog).toHaveBeenCalled()
    })
  })

  describe('买家排除域', () => {
    it('未配置：灰色未配置态', () => {
      const { api } = setup()
      const { exclude } = readTasks(api)
      expect(exclude.tone).toBe('muted')
      expect(exclude.statusTag).toEqual({
        label: 'ecommerce.monthlySettlement.prepStatusNotConfigured',
        type: 'info',
      })
    })

    it('已配置：绿色已配置态 + 数量高亮', () => {
      const { api } = setup({ buyerExcludeCount: ref(3) })
      const { exclude } = readTasks(api)
      expect(exclude.tone).toBe('success')
      expect(exclude.statusTag).toEqual({
        label: 'ecommerce.monthlySettlement.prepStatusConfigured',
        type: 'success',
      })
      expect(exclude.descHighlight).toContain('CountHighlight')
    })

    it('动作绑定打开买家排除对话框', () => {
      const { api, actions } = setup()
      const { exclude } = readTasks(api)
      exclude.action?.()
      expect(actions.openBuyerExcludeDialog).toHaveBeenCalled()
    })
  })

  describe('待定订单域', () => {
    it('订单未导入：提示先导入，动作指向导入', () => {
      const { api, actions } = setup()
      const { pending } = readTasks(api)
      expect(pending.statusTag).toEqual({
        label: 'ecommerce.monthlySettlement.prepStatusPendingImport',
        type: 'info',
      })
      pending.action?.()
      expect(actions.goImportOrders).toHaveBeenCalled()
    })

    it('已导入未计算：提示先计算', () => {
      const { api } = setup({
        orderOverview: ref(makeOverview({ totalOrderCount: 1 })),
        calculated: ref(false),
      })
      const { pending } = readTasks(api)
      expect(pending.statusTag).toEqual({
        label: 'ecommerce.monthlySettlement.prepStatusPendingCalculate',
        type: 'info',
      })
    })

    it('已计算有待定订单：黄色待处理态', () => {
      const { api, actions } = setup({
        orderOverview: ref(makeOverview({ totalOrderCount: 1 })),
        calculated: ref(true),
        totalPendingOrders: ref(2),
      })
      const { pending } = readTasks(api)
      expect(pending.tone).toBe('warning')
      expect(pending.statusTag).toEqual({
        label: 'ecommerce.monthlySettlement.prepStatusPendingDecision',
        type: 'warning',
      })
      pending.action?.()
      expect(actions.goReviewPending).toHaveBeenCalled()
    })

    it('已计算无待定且有店铺汇总：绿色已完成', () => {
      const { api } = setup({
        orderOverview: ref(makeOverview({ totalOrderCount: 1 })),
        calculated: ref(true),
        shopSummaries: ref([makeShopSummary()]),
      })
      const { pending } = readTasks(api)
      expect(pending.tone).toBe('success')
      expect(pending.statusTag).toEqual({
        label: 'ecommerce.monthlySettlement.prepStatusCompleted',
        type: 'success',
      })
    })
  })

  describe('lastOperationTime', () => {
    it('订单最后导入时间取 overview 与店铺导入时间的最大值', () => {
      const { api } = setup({
        orderOverview: ref(
          makeOverview({
            totalOrderCount: 1,
            lastImportTime: '2026-08-01 08:00:00',
            shops: [{ shopId: 1, orderCount: 0, status: 'IMPORTED', lastImportTime: '2026-08-02 09:00:00' }],
          }),
        ),
      })
      const { orders } = readTasks(api)
      expect(orders.lastOperationTime).toBe('2026-08-02 09:00:00')
    })

    it('买家排除最后操作时间含手动操作时间', () => {
      const { api } = setup({
        buyerExcludeCount: ref(1),
        buyerExcludesSnapshot: ref([{ id: 1, buyerName: '张三', createTime: '2026-08-01 10:00:00' }]),
        lastBuyerExcludeOpAt: ref('2026-08-03 11:00:00'),
      })
      const { exclude } = readTasks(api)
      expect(exclude.lastOperationTime).toBe('2026-08-03 11:00:00')
    })

    it('待定订单最后操作时间含计算时间', () => {
      const { api } = setup({
        orderOverview: ref(makeOverview({ totalOrderCount: 1 })),
        calculated: ref(true),
        lastPendingDecisionAt: ref('2026-08-02 09:00:00'),
        lastCalculatedAt: ref('2026-08-01 08:00:00'),
      })
      const { pending } = readTasks(api)
      expect(pending.lastOperationTime).toBe('2026-08-02 09:00:00')
    })
  })
})
