import { computed, type Ref } from 'vue'
import type { EcExpressStation } from '@/api/ecommerce/express'
import type { ExpressBillRecord, MonthlySettlementShopSummary, SettlementBuyerExclude } from '@/api/ecommerce/monthlySettlement'
import type { EcSalesOrderMonthlyOverview } from '@/api/ecommerce/salesOrder'
import {
  buildExpressBillCards,
  filterExpressBillRecordsByMonth,
  pickLatestPrepTime,
  type PrepExpressBillCard,
  type PrepTask,
  type PrepTone,
} from '../monthlySettlementView'

/** 翻译函数签名（vue-i18n t 的宽松子集，便于测试注入） */
export type PrepTranslateFn = (key: string, params?: Record<string, unknown>) => string

/**
 * 结算前准备清单依赖：全部输入以 Ref/函数注入，
 * 组件持有状态与动作，composable 只负责把状态翻译成准备任务列表。
 */
export interface MonthlySettlementPrepTasksDeps {
  t: PrepTranslateFn
  orderOverview: Readonly<Ref<EcSalesOrderMonthlyOverview | null>>
  expressBillRecords: Readonly<Ref<ExpressBillRecord[]>>
  expressStationMap: Readonly<Ref<Map<number, EcExpressStation>>>
  settlementMonth: Readonly<Ref<string>>
  expressBillImported: Readonly<Ref<boolean>>
  buyerExcludeCount: Readonly<Ref<number>>
  buyerExcludesSnapshot: Readonly<Ref<SettlementBuyerExclude[]>>
  lastBuyerExcludeOpAt: Readonly<Ref<string | null>>
  totalPendingOrders: Readonly<Ref<number>>
  calculated: Readonly<Ref<boolean>>
  shopSummaries: Readonly<Ref<MonthlySettlementShopSummary[]>>
  lastPendingDecisionAt: Readonly<Ref<string | null>>
  lastCalculatedAt: Readonly<Ref<string | null>>
  openExpressBillDialog: () => void
  openBuyerExcludeDialog: () => void
  goReviewPending: () => void
  goImportOrders: () => void
}

/**
 * 结算前准备清单状态机：把订单导入/快递账单/买家排除/待定订单四域
 * 的实时状态翻译成统一的准备任务卡片列表（音调、状态标签、文案、动作）。
 */
export function useMonthlySettlementPrepTasks(deps: MonthlySettlementPrepTasksDeps) {
  const { t } = deps

  const prepTasks = computed<PrepTask[]>(() => {
    const overview = deps.orderOverview.value
    const orderCount = overview?.totalOrderCount ?? 0
    const shopCount = overview?.importedShopCount ?? 0
    const reviewCount = overview?.pendingReviewCount ?? 0

    let orderTone: PrepTone = 'danger'
    let orderStatusTag: PrepTask['statusTag'] = {
      label: t('ecommerce.monthlySettlement.prepStatusNotImported'),
      type: 'danger',
    }
    let orderDesc = ''
    let orderDescHighlight = ''
    if (reviewCount > 0) {
      orderTone = 'warning'
      orderStatusTag = {
        label: t('ecommerce.monthlySettlement.prepStatusPendingReview'),
        type: 'warning',
      }
      orderDesc = t('ecommerce.monthlySettlement.salesOrdersImportedPrefix')
      orderDescHighlight = t('ecommerce.monthlySettlement.salesOrdersImportedCount', { shopCount, orderCount })
    } else if (orderCount > 0) {
      orderTone = 'success'
      orderStatusTag = {
        label: t('ecommerce.monthlySettlement.prepStatusCompleted'),
        type: 'success',
      }
      orderDesc = t('ecommerce.monthlySettlement.salesOrdersImportedPrefix')
      orderDescHighlight = t('ecommerce.monthlySettlement.salesOrdersImportedCount', { shopCount, orderCount })
    } else {
      orderDesc = t('ecommerce.monthlySettlement.salesOrdersNotImportedHint')
    }

    const expressBillCards: PrepExpressBillCard[] = []
    let expressTone: PrepTone = deps.expressBillImported.value ? 'success' : 'danger'
    let expressStatusTag: PrepTask['statusTag'] = deps.expressBillImported.value
      ? { label: t('ecommerce.monthlySettlement.prepStatusCompleted'), type: 'success' }
      : { label: t('ecommerce.monthlySettlement.prepStatusNotImported'), type: 'danger' }
    let expressDesc = ''
    let expressHasGap = false
    let expressHasUnmatched = false

    const monthRecords = filterExpressBillRecordsByMonth(deps.expressBillRecords.value, deps.settlementMonth.value)
    if (monthRecords.length) {
      for (const record of monthRecords) {
        if ((record.gapOrderRows ?? 0) > 0) {
          expressHasGap = true
        }
        if ((record.unmatchedRows ?? 0) > 0) {
          expressHasUnmatched = true
        }
      }
      expressBillCards.push(
        ...buildExpressBillCards(monthRecords, deps.settlementMonth.value, (stationId) =>
          deps.expressStationMap.value.get(stationId),
        ),
      )
      if (expressHasGap || expressHasUnmatched) {
        expressTone = 'warning'
        expressStatusTag = {
          label: t('ecommerce.monthlySettlement.prepStatusPendingFill'),
          type: 'warning',
        }
      } else if (deps.expressBillImported.value) {
        expressTone = 'success'
        expressStatusTag = {
          label: t('ecommerce.monthlySettlement.prepStatusCompleted'),
          type: 'success',
        }
      }
      const totalMatched = expressBillCards.reduce((sum, card) => sum + card.matched, 0)
      expressDesc = t('ecommerce.monthlySettlement.expressBillImportedSummary', { count: totalMatched })
    } else if (!deps.expressBillImported.value) {
      expressDesc = t('ecommerce.monthlySettlement.expressBillImportPrompt')
    }

    const excludeTone: PrepTone = deps.buyerExcludeCount.value > 0 ? 'success' : 'muted'
    const excludeStatusTag: PrepTask['statusTag'] = deps.buyerExcludeCount.value > 0
      ? { label: t('ecommerce.monthlySettlement.prepStatusConfigured'), type: 'success' }
      : { label: t('ecommerce.monthlySettlement.prepStatusNotConfigured'), type: 'info' }
    const excludeDesc =
      deps.buyerExcludeCount.value > 0
        ? t('ecommerce.monthlySettlement.buyerExcludePrefix')
        : t('ecommerce.monthlySettlement.buyerExcludePurposeHint')
    const excludeDescHighlight =
      deps.buyerExcludeCount.value > 0
        ? t('ecommerce.monthlySettlement.buyerExcludeCountHighlight', { count: deps.buyerExcludeCount.value })
        : ''

    const pendingCount = deps.totalPendingOrders.value
    const salesOrdersImported = orderCount > 0 || reviewCount > 0
    let pendingTone: PrepTone = 'muted'
    let pendingStatusTag: PrepTask['statusTag'] | undefined
    let pendingDesc = ''
    let pendingDescHighlight = ''
    let pendingAction: (() => void) | undefined = deps.goReviewPending
    if (!salesOrdersImported) {
      pendingTone = 'muted'
      pendingStatusTag = {
        label: t('ecommerce.monthlySettlement.prepStatusPendingImport'),
        type: 'info',
      }
      pendingDesc = t('ecommerce.monthlySettlement.pendingOrdersImportFirstHint')
      pendingAction = deps.goImportOrders
    } else if (!deps.calculated.value) {
      pendingTone = 'muted'
      pendingStatusTag = {
        label: t('ecommerce.monthlySettlement.prepStatusPendingCalculate'),
        type: 'info',
      }
    } else if (pendingCount > 0) {
      pendingTone = 'warning'
      pendingStatusTag = {
        label: t('ecommerce.monthlySettlement.prepStatusPendingDecision'),
        type: 'warning',
      }
      pendingDesc = t('ecommerce.monthlySettlement.pendingOrdersPrefix')
      pendingDescHighlight = t('ecommerce.monthlySettlement.pendingOrdersCountHighlight', { count: pendingCount })
    } else if (deps.shopSummaries.value.length > 0) {
      pendingTone = 'success'
      pendingStatusTag = {
        label: t('ecommerce.monthlySettlement.prepStatusCompleted'),
        type: 'success',
      }
    } else {
      pendingStatusTag = {
        label: t('ecommerce.monthlySettlement.prepStatusPendingCalculate'),
        type: 'info',
      }
    }

    const orderLastTime =
      orderCount > 0 || reviewCount > 0
        ? pickLatestPrepTime(
            overview?.lastImportTime,
            ...(overview?.shops?.map((s) => s.lastImportTime) ?? []),
          )
        : undefined
    const expressLastTime = pickLatestPrepTime(...deps.expressBillRecords.value.map((r) => r.createTime))
    const excludeLastTime = pickLatestPrepTime(
      ...deps.buyerExcludesSnapshot.value.map((item) => item.createTime),
      deps.lastBuyerExcludeOpAt.value,
    )
    const pendingLastTime = pickLatestPrepTime(
      deps.lastPendingDecisionAt.value,
      deps.calculated.value ? deps.lastCalculatedAt.value : null,
    )

    return [
      {
        key: 'orders',
        title: t('ecommerce.monthlySettlement.prepSalesOrders'),
        desc: orderDesc,
        descHighlight: orderDescHighlight || undefined,
        tone: orderTone,
        statusTag: orderStatusTag,
        lastOperationTime: orderLastTime,
        lastTimeLabelKey: 'ecommerce.monthlySettlement.prepLastImport',
        action: deps.goImportOrders,
      },
      {
        key: 'express',
        title: t('ecommerce.monthlySettlement.prepExpressBill'),
        desc: expressDesc,
        tone: expressTone,
        statusTag: expressStatusTag,
        lastOperationTime: expressLastTime,
        expressBillCards: expressBillCards.length ? expressBillCards : undefined,
        action: deps.openExpressBillDialog,
      },
      {
        key: 'exclude',
        title: t('ecommerce.monthlySettlement.prepBuyerExclude'),
        desc: excludeDesc,
        descHighlight: excludeDescHighlight || undefined,
        tone: excludeTone,
        statusTag: excludeStatusTag,
        lastOperationTime: excludeLastTime,
        action: deps.openBuyerExcludeDialog,
      },
      {
        key: 'pending',
        title: t('ecommerce.monthlySettlement.prepPendingOrders'),
        desc: pendingDesc,
        descHighlight: pendingDescHighlight || undefined,
        tone: pendingTone,
        statusTag: pendingStatusTag,
        lastOperationTime: pendingLastTime,
        action: pendingAction,
      },
    ]
  })

  return { prepTasks }
}
