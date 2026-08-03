import { describe, expect, it } from 'vitest'
import type { EcSalesOrderMonthlyOverview, EcSalesOrderShopImportStatus } from '@/api/ecommerce/salesOrder'
import { buildStatCards, toShopImportCardView, type TranslateFn } from '../salesOrderPanelView'

/** i18n stub：返回 key（带参数序列化），便于断言翻译键与参数 */
const t: TranslateFn = (key, params) => `${key}${params ? `:${JSON.stringify(params)}` : ''}`

function shop(overrides: Partial<EcSalesOrderShopImportStatus> = {}): EcSalesOrderShopImportStatus {
  return {
    shopId: 1,
    shopName: '店铺甲',
    platformName: '淘宝',
    platformCode: 1,
    orderCount: 5,
    status: 'NOT_IMPORTED',
    lastImportTime: null,
    pendingReviewRows: 2,
    ...overrides,
  }
}

function overview(overrides: Partial<EcSalesOrderMonthlyOverview> = {}): EcSalesOrderMonthlyOverview {
  return {
    orderMonth: '2026-08',
    totalOrderCount: 100,
    importedShopCount: 8,
    totalShopCount: 10,
    pendingReviewCount: 3,
    lastImportTime: '2026-08-04T10:00:00',
    shops: [],
    ...overrides,
  }
}

describe('buildStatCards', () => {
  it('按概览数据映射四张统计卡片', () => {
    const cards = buildStatCards(overview(), t)
    expect(cards).toHaveLength(4)

    const orders = cards.find((c) => c.key === 'orders')!
    expect(orders.label).toBe('ecommerce.salesOrder.statImportedOrders')
    expect(orders.value).toBe('100')
    expect(orders.tone).toBe('blue')

    const shops = cards.find((c) => c.key === 'shops')!
    expect(shops.value).toBe('8/10')
    expect(shops.tone).toBe('green')

    const pending = cards.find((c) => c.key === 'pending')!
    expect(pending.value).toBe('3')
    expect(pending.tone).toBe('orange')
  })

  it('最近导入卡：有时间时展示日期且无 hint', () => {
    const last = buildStatCards(overview(), t).find((c) => c.key === 'lastImport')!
    expect(last.value).not.toBe('—')
    expect(last.hint).toBeUndefined()
    expect(last.tone).toBe('gray')
  })

  it('最近导入卡：无时间时占位「—」且 hint 为提示文案', () => {
    const last = buildStatCards(overview({ lastImportTime: null }), t).find((c) => c.key === 'lastImport')!
    expect(last.value).toBe('—')
    expect(last.hint).toBe('ecommerce.salesOrder.statLastImportEmpty')
  })

  it('概览为空时全部回退默认值', () => {
    const cards = buildStatCards(null, t)
    expect(cards.find((c) => c.key === 'orders')!.value).toBe('0')
    expect(cards.find((c) => c.key === 'shops')!.value).toBe('0/0')
    expect(cards.find((c) => c.key === 'pending')!.value).toBe('0')
    expect(cards.find((c) => c.key === 'lastImport')!.value).toBe('—')
  })
})

describe('toShopImportCardView', () => {
  it('待审核且已有订单：橙色提示「已导入含待审核」', () => {
    const card = toShopImportCardView(shop({ status: 'PENDING_REVIEW' }), t)
    expect(card.tone).toBe('orange')
    expect(card.statusText).toBe('ecommerce.salesOrder.shopStatusImportedWithPending:{"imported":5,"pending":2}')
    expect(card.actionType).toBe('warning')
    expect(card.actionLabel).toBe('ecommerce.salesOrder.shopContinueReview')
  })

  it('待审核但无订单：仅显示待审核数', () => {
    const card = toShopImportCardView(shop({ status: 'PENDING_REVIEW', orderCount: 0 }), t)
    expect(card.statusText).toBe('ecommerce.salesOrder.shopStatusPendingReview:{"count":2}')
  })

  it('已导入：绿色卡片，操作跳转订单', () => {
    const card = toShopImportCardView(shop({ status: 'IMPORTED' }), t)
    expect(card.tone).toBe('green')
    expect(card.statusText).toBe('ecommerce.salesOrder.shopStatusImported:{"count":5}')
    expect(card.actionType).toBe('default')
    expect(card.actionLabel).toBe('ecommerce.salesOrder.shopViewOrders')
  })

  it('未导入：灰色卡片，操作去导入', () => {
    const card = toShopImportCardView(shop(), t)
    expect(card.tone).toBe('gray')
    expect(card.statusText).toBe('ecommerce.salesOrder.shopStatusNotImported')
    expect(card.actionType).toBe('primary')
    expect(card.actionLabel).toBe('ecommerce.salesOrder.shopGoImport')
  })

  it('店铺名为空时回退 #id', () => {
    const card = toShopImportCardView(shop({ shopName: undefined }), t)
    expect(card.shopName).toBe('#1')
  })

  it('最近导入时间存在时填充 dateLabel', () => {
    const card = toShopImportCardView(shop({ status: 'IMPORTED', lastImportTime: '2026-08-04T10:00:00' }), t)
    expect(card.dateLabel).toBeDefined()
  })

  it('待审核单保留 pendingBatchId 供继续审核', () => {
    const card = toShopImportCardView(shop({ status: 'PENDING_REVIEW', pendingBatchId: 42 }), t)
    expect(card.pendingBatchId).toBe(42)
  })
})
