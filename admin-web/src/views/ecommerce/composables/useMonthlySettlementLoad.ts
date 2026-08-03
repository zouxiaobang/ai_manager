import { ref, type Ref } from 'vue'
import type { EcExpressStation } from '@/api/ecommerce/express'
import type { EcSalesOrderMonthlyOverview } from '@/api/ecommerce/salesOrder'
import type { EcShop } from '@/api/ecommerce/shop'
import type { ExpressBillRecord, MonthlySettlementShopSummary } from '@/api/ecommerce/monthlySettlement'

/** 结算结果快照入参（兼容 API MonthlySettlementResult 的字段子集） */
export interface SettlementResultData {
  shops: MonthlySettlementShopSummary[]
  expressBillImported?: boolean
  calculatedAt?: string
}

/**
 * 页面加载编排依赖：页面状态 refs 由组件持有、composable 写入，
 * loading 引用计数与跨域同步动作注入，保持编排逻辑可脱离组件单测。
 */
export interface MonthlySettlementLoadDeps {
  t: (key: string) => string
  // 页面状态 refs
  settlementMonth: Ref<string>
  calculated: Ref<boolean>
  result: Ref<SettlementResultData | null>
  selectedShopId: Ref<number | null>
  maxProfitShowAll: Ref<boolean>
  lastCalculatedAt: Ref<string | null>
  orderOverview: Ref<EcSalesOrderMonthlyOverview | null>
  expressBillRecords: Ref<ExpressBillRecord[]>
  expressBillImported: Ref<boolean>
  shopOptions: Ref<EcShop[]>
  expressStations: Ref<EcExpressStation[]>
  // loading 引用计数动作（组件由 useCountingLoading 提供）
  beginCalculating: () => void
  endCalculating: () => void
  beginSubmitting: () => void
  endSubmitting: () => void
  beginPrepLoading: (silent?: boolean) => void
  endPrepLoading: (silent?: boolean) => void
  resetPrepLoading: () => void
  // 跨域同步动作
  clearSnapshot: () => void
  loadSnapshot: () => Promise<unknown>
  syncPendingDecisions: (shops: MonthlySettlementShopSummary[]) => void
  syncSelectedShop: () => void
  // API（fetchExpressStations 封装为「拉全部站点」）
  fetchShopOptions: () => Promise<EcShop[]>
  fetchExpressStations: () => Promise<EcExpressStation[]>
  fetchSalesOrderMonthlyOverview: (month: string) => Promise<EcSalesOrderMonthlyOverview>
  fetchExpressBillImported: (month: string) => Promise<boolean>
  fetchExpressBillRecords: (month: string) => Promise<ExpressBillRecord[] | null>
  calculateMonthlySettlement: (month: string) => Promise<SettlementResultData>
  fetchMonthlySettlementSnapshot: (month: string) => Promise<SettlementResultData | null>
  // 用户提示
  notifyWarning: (message: string) => void
  notifySuccess: (message: string) => void
}

/**
 * 页面加载编排状态机：预备数据竞态保护（seq 只允许最近请求写入）、
 * 结算请求去重（pageLoadPromise 复用）、首屏一次性引导与月份监听解锁。
 */
export function useMonthlySettlementLoad(deps: MonthlySettlementLoadDeps) {
  // 预备数据竞态序号：月份快速切换时丢弃过期请求结果
  let prepRequestSeq = 0
  let enterPromise: Promise<void> | null = null
  let pageLoadPromise: Promise<void> | null = null
  let bootstrapped = false
  /** 首屏挂载期间忽略月份监听，避免初始 enter 与 watch 双重触发 */
  const ignoreMonthWatch = ref(true)

  /** 应用结算结果：清空或写入页面状态，并同步决策表/选中店铺 */
  function applySettlementResult(data: SettlementResultData | null) {
    if (!data) {
      deps.result.value = null
      deps.calculated.value = false
      deps.selectedShopId.value = null
      deps.maxProfitShowAll.value = false
      deps.lastCalculatedAt.value = null
      return
    }
    deps.result.value = data
    deps.calculated.value = true
    deps.syncPendingDecisions(data.shops ?? [])
    deps.syncSelectedShop()
    deps.lastCalculatedAt.value = data.calculatedAt ?? new Date().toISOString()
  }

  /** 加载预备数据（订单总览/快递导入态/账单记录/买家排除快照），带请求竞态保护 */
  async function loadPrepData(options?: { silent?: boolean }) {
    if (!deps.settlementMonth.value) {
      deps.orderOverview.value = null
      deps.expressBillRecords.value = []
      deps.clearSnapshot()
      deps.expressBillImported.value = false
      deps.resetPrepLoading()
      return
    }
    const seq = ++prepRequestSeq
    deps.beginPrepLoading(options?.silent)
    try {
      const [overview, imported, records] = await Promise.all([
        deps.fetchSalesOrderMonthlyOverview(deps.settlementMonth.value),
        deps.fetchExpressBillImported(deps.settlementMonth.value),
        deps.fetchExpressBillRecords(deps.settlementMonth.value),
        deps.loadSnapshot(),
      ])
      // 仅最近一次请求的结果可写入，防止切换月份后旧数据回填
      if (seq !== prepRequestSeq) return
      deps.orderOverview.value = overview
      deps.expressBillImported.value = !!imported
      deps.expressBillRecords.value = records ?? []
    } catch {
      if (seq !== prepRequestSeq) return
      deps.orderOverview.value = null
      deps.expressBillRecords.value = []
      deps.clearSnapshot()
      deps.expressBillImported.value = false
    } finally {
      deps.endPrepLoading(options?.silent)
    }
  }

  /**
   * 页面加载编排：manual（计算并保存）/ auto（读取快照）。
   * 并发调用复用同一加载；加载中月份变化时丢弃过期结果。
   */
  async function runPageLoad(source: 'auto' | 'manual') {
    const month = deps.settlementMonth.value
    if (!month) {
      if (source === 'manual') {
        deps.notifyWarning(deps.t('ecommerce.monthlySettlement.monthRequired'))
      }
      return
    }
    if (pageLoadPromise) {
      await pageLoadPromise
      if (source === 'manual') {
        return runPageLoad('manual')
      }
      if (deps.calculated.value && deps.settlementMonth.value === month) {
        return
      }
    }

    pageLoadPromise = (async () => {
      deps.beginCalculating()
      if (source === 'manual') deps.beginSubmitting()
      try {
        await loadPrepData({ silent: true })
        if (deps.settlementMonth.value !== month) return
        if (source === 'manual') {
          const data = await deps.calculateMonthlySettlement(month)
          if (deps.settlementMonth.value !== month) return
          applySettlementResult(data ?? { shops: [] })
          deps.notifySuccess(deps.t('ecommerce.monthlySettlement.calculateSaved'))
        } else {
          const data = await deps.fetchMonthlySettlementSnapshot(month)
          if (deps.settlementMonth.value !== month) return
          applySettlementResult(data)
        }
      } catch {
        // auto 模式加载失败清空结果；manual 失败由用户重试（保留原状）
        if (deps.settlementMonth.value !== month) return
        if (source === 'auto') {
          applySettlementResult(null)
        }
      } finally {
        deps.endCalculating()
        if (source === 'manual') deps.endSubmitting()
      }
    })().finally(() => {
      pageLoadPromise = null
    })
    return pageLoadPromise
  }

  /** 首屏一次性引导：加载店铺/快递站选项后自动读取当月快照 */
  async function enter() {
    if (enterPromise) return enterPromise
    enterPromise = (async () => {
      if (!deps.shopOptions.value.length) {
        try {
          deps.shopOptions.value = await deps.fetchShopOptions()
        } catch {
          // 选项加载失败不阻塞首屏，保持空列表，后续操作会显式报错
          deps.shopOptions.value = []
        }
      }
      if (!deps.expressStations.value.length) {
        try {
          deps.expressStations.value = await deps.fetchExpressStations()
        } catch {
          deps.expressStations.value = []
        }
      }
      await runPageLoad('auto')
      bootstrapped = true
    })().finally(() => {
      enterPromise = null
    })
    return enterPromise
  }

  /** 挂载任务：已引导/引导中则复用，结束后解锁月份监听 */
  async function mount() {
    const task = bootstrapped || enterPromise ? (enterPromise ?? Promise.resolve()) : enter()
    return Promise.resolve(task).finally(() => {
      ignoreMonthWatch.value = false
    })
  }

  return {
    applySettlementResult,
    runPageLoad,
    enter,
    loadPrepData,
    mount,
    ignoreMonthWatch,
  }
}
