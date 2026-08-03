import { reactive, type Ref } from 'vue'
import type { MonthlySettlementResult, MonthlySettlementShopSummary } from '@/api/ecommerce/monthlySettlement'
import type { PrepTranslateFn } from './useMonthlySettlementPrepTasks'

/** 待定订单决策保存载荷 */
export interface PendingDecisionItem {
  orderId: number
  included: boolean
}

/**
 * 待定订单决策依赖：结算月份与两个共享 ref 由组件持有，
 * 保存回调、结果应用与用户提示注入，composable 管理决策表状态。
 */
export interface MonthlySettlementPendingDecisionsDeps {
  t: PrepTranslateFn
  settlementMonth: Readonly<Ref<string>>
  savingDecisions: Ref<boolean>
  lastPendingDecisionAt: Ref<string | null>
  applySettlementResult: (data: MonthlySettlementResult | null) => void
  saveDecisions: (payload: { settlementMonth: string; items: PendingDecisionItem[] }) => Promise<MonthlySettlementResult>
  notifyWarning: (message: string) => void
  notifySuccess: (message: string) => void
}

/**
 * 待定订单决策状态机：订单级 included 决策表 + 单行/全选变更 + 批量保存。
 * 决策表以订单 ID 为键，行未决策时默认取服务端 included。
 */
export function useMonthlySettlementPendingDecisions(deps: MonthlySettlementPendingDecisionsDeps) {
  const pendingDecisions = reactive<Record<number, boolean>>({})

  /** 按店铺汇总重建决策表：先清空再回填，避免残留旧订单 */
  function syncPendingDecisions(shops: MonthlySettlementShopSummary[]) {
    Object.keys(pendingDecisions).forEach((k) => delete pendingDecisions[Number(k)])
    for (const shop of shops) {
      for (const row of shop.pendingOrders ?? []) {
        if (row.orderId == null) continue
        pendingDecisions[row.orderId] = row.included ?? true
      }
    }
  }

  /** 单行切换：记录订单决策 */
  function onPendingDecisionChange(orderId: number, included: boolean) {
    pendingDecisions[orderId] = included
  }

  /** 行展示值：未决策时按服务端 included 兜底并落表 */
  function resolvePendingDecision(row: { orderId?: number; included?: boolean | null }) {
    const orderId = row.orderId
    if (orderId == null) return true
    if (pendingDecisions[orderId] === undefined) {
      pendingDecisions[orderId] = row.included ?? true
    }
    return pendingDecisions[orderId]
  }

  /** 全选/全不选：批量写店铺待定订单 */
  function setAllPendingDecisions(shop: MonthlySettlementShopSummary | null, included: boolean) {
    if (!shop) return
    for (const row of shop.pendingOrders ?? []) {
      if (row.orderId == null) continue
      pendingDecisions[row.orderId] = included
    }
  }

  /** 批量保存：无待定订单提示；成功后应用结算结果并记录操作时间 */
  async function savePendingDecisions(shop: MonthlySettlementShopSummary) {
    if (!deps.settlementMonth.value) return
    const items: PendingDecisionItem[] = (shop.pendingOrders ?? [])
      .filter((row) => row.orderId != null)
      .map((row) => ({ orderId: row.orderId!, included: pendingDecisions[row.orderId!] ?? true }))
    if (!items.length) {
      deps.notifyWarning(deps.t('ecommerce.monthlySettlement.noDecisions'))
      return
    }
    deps.savingDecisions.value = true
    try {
      const data = await deps.saveDecisions({
        settlementMonth: deps.settlementMonth.value,
        items,
      })
      deps.applySettlementResult(data)
      deps.lastPendingDecisionAt.value = new Date().toISOString()
      deps.notifySuccess(deps.t('ecommerce.monthlySettlement.decisionsSaved'))
    } finally {
      deps.savingDecisions.value = false
    }
  }

  return {
    pendingDecisions,
    syncPendingDecisions,
    onPendingDecisionChange,
    resolvePendingDecision,
    setAllPendingDecisions,
    savePendingDecisions,
  }
}
