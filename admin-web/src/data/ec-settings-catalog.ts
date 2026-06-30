import type { Component } from 'vue'
import {
  Bell,
  Box,
  Document,
  Download,
  Money,
  Setting,
  Van,
} from '@element-plus/icons-vue'

export type EcSettingsCategoryKey =
  | 'documents'
  | 'inventory'
  | 'settlement'
  | 'orderImport'
  | 'express'
  | 'notification'
  | 'general'

export type EcSettingsItemKey =
  | 'purchase-order'
  | 'outbound-order'
  | 'delivery-note'
  | 'inventory-defaults'
  | 'slow-moving'
  | 'profit-rules'
  | 'rebate-default'
  | 'import-template'
  | 'date-format'
  | 'order-import-status'
  | 'express-bill-mapping'
  | 'label-price'
  | 'inventory-alert-notify'
  | 'settlement-remind'
  | 'company-info'
  | 'data-retention'

export interface EcSettingsCategoryDef {
  key: EcSettingsCategoryKey
  labelKey: string
  descKey: string
  icon: Component
  tone: 'blue' | 'green' | 'purple' | 'orange' | 'cyan' | 'gray'
  itemKeys: EcSettingsItemKey[]
}

export type EcSettingsPanelKey =
  | 'purchase-order'
  | 'outbound-order'
  | 'inventory-defaults'
  | 'import-template'
  | 'order-import-status'
  | 'express-bill-mapping'
  | 'delivery-note'
  | 'company-info'
  | 'profit-rules'
  | 'rebate-default'
  | 'notification'
  | 'data-retention'

export interface EcSettingsItemDef {
  key: EcSettingsItemKey
  categoryKey: EcSettingsCategoryKey
  labelKey: string
  descKey: string
  tagKeys: string[]
  searchKeywords: string[]
  implemented: boolean
  panelKey?: EcSettingsPanelKey
}

export const EC_SETTINGS_CATEGORIES: EcSettingsCategoryDef[] = [
  {
    key: 'documents',
    labelKey: 'ecommerce.settings.categories.documents',
    descKey: 'ecommerce.settings.categories.documentsDesc',
    icon: Document,
    tone: 'blue',
    itemKeys: ['purchase-order', 'outbound-order', 'delivery-note'],
  },
  {
    key: 'inventory',
    labelKey: 'ecommerce.settings.categories.inventory',
    descKey: 'ecommerce.settings.categories.inventoryDesc',
    icon: Box,
    tone: 'green',
    itemKeys: ['inventory-defaults', 'slow-moving'],
  },
  {
    key: 'settlement',
    labelKey: 'ecommerce.settings.categories.settlement',
    descKey: 'ecommerce.settings.categories.settlementDesc',
    icon: Money,
    tone: 'purple',
    itemKeys: ['profit-rules', 'rebate-default'],
  },
  {
    key: 'orderImport',
    labelKey: 'ecommerce.settings.categories.orderImport',
    descKey: 'ecommerce.settings.categories.orderImportDesc',
    icon: Download,
    tone: 'orange',
    itemKeys: ['import-template', 'date-format', 'order-import-status'],
  },
  {
    key: 'express',
    labelKey: 'ecommerce.settings.categories.express',
    descKey: 'ecommerce.settings.categories.expressDesc',
    icon: Van,
    tone: 'cyan',
    itemKeys: ['express-bill-mapping', 'label-price'],
  },
  {
    key: 'notification',
    labelKey: 'ecommerce.settings.categories.notification',
    descKey: 'ecommerce.settings.categories.notificationDesc',
    icon: Bell,
    tone: 'orange',
    itemKeys: ['inventory-alert-notify', 'settlement-remind'],
  },
  {
    key: 'general',
    labelKey: 'ecommerce.settings.categories.general',
    descKey: 'ecommerce.settings.categories.generalDesc',
    icon: Setting,
    tone: 'gray',
    itemKeys: ['company-info', 'data-retention'],
  },
]

export const EC_SETTINGS_ITEMS: EcSettingsItemDef[] = [
  {
    key: 'purchase-order',
    categoryKey: 'documents',
    labelKey: 'ecommerce.settings.items.purchaseOrder',
    descKey: 'ecommerce.settings.items.purchaseOrderDesc',
    tagKeys: [
      'ecommerce.settings.tags.poTitle',
      'ecommerce.settings.tags.poSign',
    ],
    searchKeywords: ['采购单', '采购订单', '打印', '抬头', '签字', '收货', 'purchase order', 'PO'],
    implemented: true,
    panelKey: 'purchase-order',
  },
  {
    key: 'outbound-order',
    categoryKey: 'documents',
    labelKey: 'ecommerce.settings.items.outboundOrder',
    descKey: 'ecommerce.settings.items.outboundOrderDesc',
    tagKeys: ['ecommerce.settings.tags.outboundHeader', 'ecommerce.settings.tags.outboundSign'],
    searchKeywords: ['出库单', '出库', 'outbound'],
    implemented: true,
    panelKey: 'outbound-order',
  },
  {
    key: 'delivery-note',
    categoryKey: 'documents',
    labelKey: 'ecommerce.settings.items.deliveryNote',
    descKey: 'ecommerce.settings.items.deliveryNoteDesc',
    tagKeys: ['ecommerce.settings.tags.deliveryHeader', 'ecommerce.settings.tags.deliverySign'],
    searchKeywords: ['送货单', '发货单', 'delivery note'],
    implemented: true,
    panelKey: 'delivery-note',
  },
  {
    key: 'inventory-defaults',
    categoryKey: 'inventory',
    labelKey: 'ecommerce.settings.items.inventoryDefaults',
    descKey: 'ecommerce.settings.items.inventoryDefaultsDesc',
    tagKeys: ['ecommerce.settings.tags.alertThreshold', 'ecommerce.settings.tags.defaultAlert'],
    searchKeywords: ['库存', '预警', '阈值', '默认', 'inventory', 'alert'],
    implemented: true,
    panelKey: 'inventory-defaults',
  },
  {
    key: 'slow-moving',
    categoryKey: 'inventory',
    labelKey: 'ecommerce.settings.items.slowMoving',
    descKey: 'ecommerce.settings.items.slowMovingDesc',
    tagKeys: ['ecommerce.settings.tags.slowDays', 'ecommerce.settings.tags.turnover'],
    searchKeywords: ['滞销', '周转', 'slow moving'],
    implemented: true,
    panelKey: 'inventory-defaults',
  },
  {
    key: 'profit-rules',
    categoryKey: 'settlement',
    labelKey: 'ecommerce.settings.items.profitRules',
    descKey: 'ecommerce.settings.items.profitRulesDesc',
    tagKeys: ['ecommerce.settings.tags.grossMargin', 'ecommerce.settings.tags.netProfit'],
    searchKeywords: ['利润', '毛利', '净利', '月结', 'profit', 'margin'],
    implemented: true,
    panelKey: 'profit-rules',
  },
  {
    key: 'rebate-default',
    categoryKey: 'settlement',
    labelKey: 'ecommerce.settings.items.rebateDefault',
    descKey: 'ecommerce.settings.items.rebateDefaultDesc',
    tagKeys: ['ecommerce.settings.tags.rebatePct', 'ecommerce.settings.tags.platformRebate'],
    searchKeywords: ['退点', '返利', 'rebate'],
    implemented: true,
    panelKey: 'rebate-default',
  },
  {
    key: 'order-import-status',
    categoryKey: 'orderImport',
    labelKey: 'ecommerce.settings.items.orderImportStatus',
    descKey: 'ecommerce.settings.items.orderImportStatusDesc',
    tagKeys: ['ecommerce.settings.tags.statusMapping', 'ecommerce.settings.tags.defaultLineStatus'],
    searchKeywords: ['订单状态', '状态映射', '行状态', 'status mapping'],
    implemented: true,
    panelKey: 'order-import-status',
  },
  {
    key: 'import-template',
    categoryKey: 'orderImport',
    labelKey: 'ecommerce.settings.items.importTemplate',
    descKey: 'ecommerce.settings.items.importTemplateDesc',
    tagKeys: ['ecommerce.settings.tags.headerRow', 'ecommerce.settings.tags.columnMapping'],
    searchKeywords: ['订单导入', '导入模板', '列映射', 'import', 'mapping'],
    implemented: true,
    panelKey: 'import-template',
  },
  {
    key: 'date-format',
    categoryKey: 'orderImport',
    labelKey: 'ecommerce.settings.items.dateFormat',
    descKey: 'ecommerce.settings.items.dateFormatDesc',
    tagKeys: ['ecommerce.settings.tags.datePattern', 'ecommerce.settings.tags.timezone'],
    searchKeywords: ['日期格式', '时间格式', 'date format'],
    implemented: true,
    panelKey: 'import-template',
  },
  {
    key: 'express-bill-mapping',
    categoryKey: 'express',
    labelKey: 'ecommerce.settings.items.expressBillMapping',
    descKey: 'ecommerce.settings.items.expressBillMappingDesc',
    tagKeys: ['ecommerce.settings.tags.billColumns', 'ecommerce.settings.tags.stationMapping'],
    searchKeywords: ['快递账单', '列映射', 'express bill', 'mapping'],
    implemented: true,
    panelKey: 'express-bill-mapping',
  },
  {
    key: 'label-price',
    categoryKey: 'express',
    labelKey: 'ecommerce.settings.items.labelPrice',
    descKey: 'ecommerce.settings.items.labelPriceDesc',
    tagKeys: ['ecommerce.settings.tags.includeLabel', 'ecommerce.settings.tags.freightCost'],
    searchKeywords: ['面单', '面单价', '快递费', 'label price'],
    implemented: true,
    panelKey: 'express-bill-mapping',
  },
  {
    key: 'inventory-alert-notify',
    categoryKey: 'notification',
    labelKey: 'ecommerce.settings.items.inventoryAlertNotify',
    descKey: 'ecommerce.settings.items.inventoryAlertNotifyDesc',
    tagKeys: ['ecommerce.settings.tags.lowStock', 'ecommerce.settings.tags.zeroStock'],
    searchKeywords: ['库存预警', '通知', '提醒', 'alert notify'],
    implemented: true,
    panelKey: 'notification',
  },
  {
    key: 'settlement-remind',
    categoryKey: 'notification',
    labelKey: 'ecommerce.settings.items.settlementRemind',
    descKey: 'ecommerce.settings.items.settlementRemindDesc',
    tagKeys: ['ecommerce.settings.tags.monthlyRemind', 'ecommerce.settings.tags.pendingTask'],
    searchKeywords: ['月结', '提醒', '待办', 'settlement remind'],
    implemented: true,
    panelKey: 'notification',
  },
  {
    key: 'company-info',
    categoryKey: 'general',
    labelKey: 'ecommerce.settings.items.companyInfo',
    descKey: 'ecommerce.settings.items.companyInfoDesc',
    tagKeys: ['ecommerce.settings.tags.companyName', 'ecommerce.settings.tags.contact'],
    searchKeywords: ['公司', '企业信息', 'company'],
    implemented: true,
    panelKey: 'company-info',
  },
  {
    key: 'data-retention',
    categoryKey: 'general',
    labelKey: 'ecommerce.settings.items.dataRetention',
    descKey: 'ecommerce.settings.items.dataRetentionDesc',
    tagKeys: ['ecommerce.settings.tags.logRetention', 'ecommerce.settings.tags.importHistory'],
    searchKeywords: ['数据保留', '日志', '清理', 'retention'],
    implemented: true,
    panelKey: 'data-retention',
  },
]

const itemMap = new Map(EC_SETTINGS_ITEMS.map((item) => [item.key, item]))
const categoryMap = new Map(EC_SETTINGS_CATEGORIES.map((category) => [category.key, category]))

export function getSettingsItem(key: EcSettingsItemKey): EcSettingsItemDef | undefined {
  return itemMap.get(key)
}

export function getSettingsCategory(key: EcSettingsCategoryKey): EcSettingsCategoryDef | undefined {
  return categoryMap.get(key)
}

export function getItemsForCategory(categoryKey: EcSettingsCategoryKey): EcSettingsItemDef[] {
  const category = categoryMap.get(categoryKey)
  if (!category) return []
  return category.itemKeys
    .map((key) => itemMap.get(key))
    .filter((item): item is EcSettingsItemDef => !!item)
}

export function isSettingsItemKey(value: unknown): value is EcSettingsItemKey {
  return typeof value === 'string' && itemMap.has(value as EcSettingsItemKey)
}

export function resolveSettingsPanelKey(itemKey: EcSettingsItemKey): EcSettingsPanelKey | undefined {
  const item = itemMap.get(itemKey)
  return item?.panelKey
}

export function isSettingsPanelImplemented(panelKey: EcSettingsPanelKey): boolean {
  return EC_SETTINGS_ITEMS.some((item) => item.panelKey === panelKey && item.implemented)
}
