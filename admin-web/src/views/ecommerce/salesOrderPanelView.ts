import { formatMonthDay } from '@/utils/date'
import type {
  EcSalesOrderMonthlyOverview,
  EcSalesOrderShopImportStatus,
  ShopImportStatus,
} from '@/api/ecommerce/salesOrder'

/** i18n 翻译函数类型：宽松签名，组件侧注入 vue-i18n t，测试侧注入 stub */
export type TranslateFn = (key: string, params?: Record<string, unknown>) => string

/** 店铺导入卡片视图（与模板 shopImportCards 渲染契约） */
export interface ShopImportCardView {
  shopId: number
  shopName: string
  platformName?: string
  platformCode?: number | null
  shopAvatarUrl?: string | null
  platformAvatarUrl?: string | null
  status: ShopImportStatus
  orderCount: number
  statusText: string
  dateLabel?: string
  tone: 'green' | 'gray' | 'orange'
  actionLabel?: string
  actionType?: 'primary' | 'warning' | 'default'
  pendingBatchId?: number | null
}

/** 月度统计卡片（顶部 statCards 渲染契约） */
export interface StatCard {
  key: string
  label: string
  value: string
  hint?: string
  tone: string
}

/**
 * 月度概览 → 顶部统计卡片列表（导入订单数 / 完成店铺 / 待审核 / 最近导入）。
 * 纯映射，i18n 文案通过 t 注入以便单元测试。
 */
export function buildStatCards(overview: EcSalesOrderMonthlyOverview | null, t: TranslateFn): StatCard[] {
  return [
    {
      key: 'orders',
      label: t('ecommerce.salesOrder.statImportedOrders'),
      value: `${overview?.totalOrderCount ?? 0}`,
      hint: t('ecommerce.salesOrder.statImportedOrdersUnit'),
      tone: 'blue',
    },
    {
      key: 'shops',
      label: t('ecommerce.salesOrder.statShopsDone'),
      value: `${overview?.importedShopCount ?? 0}/${overview?.totalShopCount ?? 0}`,
      hint: undefined,
      tone: 'green',
    },
    {
      key: 'pending',
      label: t('ecommerce.salesOrder.statPendingReview'),
      value: `${overview?.pendingReviewCount ?? 0}`,
      hint: t('ecommerce.salesOrder.statPendingReviewUnit'),
      tone: 'orange',
    },
    {
      key: 'lastImport',
      label: t('ecommerce.salesOrder.statLastImport'),
      value: overview?.lastImportTime ? formatMonthDay(overview.lastImportTime) : '—',
      hint: overview?.lastImportTime ? undefined : t('ecommerce.salesOrder.statLastImportEmpty'),
      tone: 'gray',
    },
  ]
}

/**
 * 店铺导入状态 → 导入卡片视图（待审核 / 已导入 / 未导入三种状态展示）。
 * 纯映射，i18n 文案通过 t 注入以便单元测试。
 */
export function toShopImportCardView(shop: EcSalesOrderShopImportStatus, t: TranslateFn): ShopImportCardView {
  const base = {
    shopId: shop.shopId,
    shopName: shop.shopName || `#${shop.shopId}`,
    platformName: shop.platformName,
    platformCode: shop.platformCode,
    shopAvatarUrl: shop.shopAvatarUrl,
    platformAvatarUrl: shop.platformAvatarUrl,
    status: shop.status,
    orderCount: shop.orderCount,
    pendingBatchId: shop.pendingBatchId,
  }
  if (shop.status === 'PENDING_REVIEW') {
    return {
      ...base,
      tone: 'orange',
      statusText: shop.orderCount > 0
        ? t('ecommerce.salesOrder.shopStatusImportedWithPending', { imported: shop.orderCount, pending: shop.pendingReviewRows ?? 0 })
        : t('ecommerce.salesOrder.shopStatusPendingReview', { count: shop.pendingReviewRows ?? 0 }),
      dateLabel: shop.lastImportTime ? formatMonthDay(shop.lastImportTime) : undefined,
      actionLabel: t('ecommerce.salesOrder.shopContinueReview'),
      actionType: 'warning',
    }
  }
  if (shop.status === 'IMPORTED') {
    return {
      ...base,
      tone: 'green',
      statusText: t('ecommerce.salesOrder.shopStatusImported', { count: shop.orderCount }),
      dateLabel: shop.lastImportTime ? formatMonthDay(shop.lastImportTime) : undefined,
      actionLabel: t('ecommerce.salesOrder.shopViewOrders'),
      actionType: 'default',
    }
  }
  return {
    ...base,
    tone: 'gray',
    statusText: t('ecommerce.salesOrder.shopStatusNotImported'),
    actionLabel: t('ecommerce.salesOrder.shopGoImport'),
    actionType: 'primary',
  }
}
