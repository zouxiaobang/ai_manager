import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import type { MonthlySettlementResult, MonthlySettlementShopSummary } from '@/api/ecommerce/monthlySettlement'
import {
  useMonthlySettlementPendingDecisions,
  type MonthlySettlementPendingDecisionsDeps,
} from '../useMonthlySettlementPendingDecisions'

function makeShop(partial?: Partial<MonthlySettlementShopSummary>): MonthlySettlementShopSummary {
  return { shopId: 1, shopName: '店铺甲', ...partial }
}

function makeResult(partial?: Partial<MonthlySettlementResult>): MonthlySettlementResult {
  return { settlementMonth: '2026-08', shops: [], ...partial }
}

function setup(partial?: Partial<MonthlySettlementPendingDecisionsDeps>) {
  const savingDecisions = ref(false)
  const lastPendingDecisionAt = ref<string | null>(null)
  const applySettlementResult = vi.fn()
  const saveDecisions = vi.fn(async () => makeResult())
  const notifyWarning = vi.fn()
  const notifySuccess = vi.fn()
  const api = useMonthlySettlementPendingDecisions({
    t: (key: string) => key,
    settlementMonth: ref('2026-08'),
    savingDecisions,
    lastPendingDecisionAt,
    applySettlementResult,
    saveDecisions,
    notifyWarning,
    notifySuccess,
    ...partial,
  })
  return { api, applySettlementResult, saveDecisions, notifyWarning, notifySuccess, savingDecisions, lastPendingDecisionAt }
}

describe('useMonthlySettlementPendingDecisions 待定订单决策域', () => {
  describe('resolvePendingDecision', () => {
    it('无订单 ID 时默认采纳', () => {
      const { api } = setup()
      expect(api.resolvePendingDecision({})).toBe(true)
    })

    it('首次访问按服务端 included 落表并返回', () => {
      const { api } = setup()
      expect(api.resolvePendingDecision({ orderId: 1, included: false })).toBe(false)
      expect(api.pendingDecisions[1]).toBe(false)
    })

    it('已决策后返回本地值', () => {
      const { api } = setup()
      api.onPendingDecisionChange(1, true)
      expect(api.resolvePendingDecision({ orderId: 1, included: false })).toBe(true)
    })
  })

  describe('onPendingDecisionChange / setAllPendingDecisions', () => {
    it('单行切换写入决策表', () => {
      const { api } = setup()
      api.onPendingDecisionChange(5, false)
      expect(api.pendingDecisions[5]).toBe(false)
    })

    it('批量设置写入店铺全部待定订单', () => {
      const { api } = setup()
      const shop = makeShop({ pendingOrders: [{ orderId: 1, included: true }, { orderId: 2, included: true }] })
      api.setAllPendingDecisions(shop, false)
      expect(api.pendingDecisions[1]).toBe(false)
      expect(api.pendingDecisions[2]).toBe(false)
    })

    it('空店铺批量设置不报错', () => {
      const { api } = setup()
      api.setAllPendingDecisions(null, false)
    })
  })

  describe('syncPendingDecisions', () => {
    it('先清空再回填，移除残留旧订单', () => {
      const { api } = setup()
      api.onPendingDecisionChange(9, true)
      api.syncPendingDecisions([makeShop({ pendingOrders: [{ orderId: 1, included: false }] })])
      expect(api.pendingDecisions[9]).toBeUndefined()
      expect(api.pendingDecisions[1]).toBe(false)
    })
  })

  describe('savePendingDecisions', () => {
    it('无待定订单时警告且不保存', async () => {
      const { api, saveDecisions, notifyWarning } = setup()
      await api.savePendingDecisions(makeShop())
      expect(notifyWarning).toHaveBeenCalledWith('ecommerce.monthlySettlement.noDecisions')
      expect(saveDecisions).not.toHaveBeenCalled()
    })

    it('保存成功后应用结算结果并记录操作时间与成功提示', async () => {
      const { api, applySettlementResult, saveDecisions, notifySuccess, lastPendingDecisionAt, savingDecisions } = setup()
      const shop = makeShop({ pendingOrders: [{ orderId: 1, included: true }, { orderId: 2, included: null }] })
      api.onPendingDecisionChange(1, false)
      await api.savePendingDecisions(shop)
      expect(saveDecisions).toHaveBeenCalledWith({
        settlementMonth: '2026-08',
        items: [
          { orderId: 1, included: false },
          { orderId: 2, included: true },
        ],
      })
      expect(applySettlementResult).toHaveBeenCalled()
      expect(lastPendingDecisionAt.value).not.toBeNull()
      expect(notifySuccess).toHaveBeenCalledWith('ecommerce.monthlySettlement.decisionsSaved')
      expect(savingDecisions.value).toBe(false)
    })

    it('保存失败时 saving 复位并向上抛出', async () => {
      const { api, savingDecisions } = setup({
        saveDecisions: vi.fn(async () => {
          throw new Error('boom')
        }),
      })
      const shop = makeShop({ pendingOrders: [{ orderId: 1, included: true }] })
      await expect(api.savePendingDecisions(shop)).rejects.toThrow('boom')
      expect(savingDecisions.value).toBe(false)
    })
  })
})
