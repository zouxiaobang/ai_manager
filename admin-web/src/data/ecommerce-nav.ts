import type { Component } from 'vue'
import {
  Box,
  Goods,
  HomeFilled,
  OfficeBuilding,
  PieChart,
  Setting,
  Shop,
  TakeawayBox,
  Tickets,
  Van,
  Wallet,
} from '@element-plus/icons-vue'

export type EcommerceWorkbenchModule =
  | 'monthlySettlement'
  | 'order'
  | 'express'
  | 'inventory'
  | 'product'
  | 'platformShop'
  | 'factory'
  | 'carton'

export interface EcommerceNavItem {
  key: string
  path: string
  labelKey: string
  icon: Component
  module?: EcommerceWorkbenchModule
  placeholder?: boolean
}

export const ECOMMERCE_NAV_ITEMS: EcommerceNavItem[] = [
  { key: 'home', path: '/ecommerce', labelKey: 'ecommerce.nav.home', icon: HomeFilled },
  {
    key: 'monthlySettlement',
    path: '/ecommerce/monthly-settlement',
    labelKey: 'ecommerce.nav.monthlySettlement',
    icon: Wallet,
    module: 'monthlySettlement',
  },
  {
    key: 'order',
    path: '/ecommerce/orders',
    labelKey: 'ecommerce.nav.order',
    icon: Tickets,
    module: 'order',
  },
  {
    key: 'inventory',
    path: '/ecommerce/inventory',
    labelKey: 'ecommerce.nav.inventory',
    icon: Box,
    module: 'inventory',
  },
  {
    key: 'product',
    path: '/ecommerce/products',
    labelKey: 'ecommerce.nav.product',
    icon: Goods,
    module: 'product',
  },
  {
    key: 'express',
    path: '/ecommerce/express',
    labelKey: 'ecommerce.nav.express',
    icon: Van,
    module: 'express',
  },
  {
    key: 'factory',
    path: '/ecommerce/factories',
    labelKey: 'ecommerce.nav.factory',
    icon: OfficeBuilding,
    module: 'factory',
  },
  {
    key: 'platformShop',
    path: '/ecommerce/shops',
    labelKey: 'ecommerce.nav.platformShop',
    icon: Shop,
    module: 'platformShop',
  },
  {
    key: 'carton',
    path: '/ecommerce/cartons',
    labelKey: 'ecommerce.nav.carton',
    icon: TakeawayBox,
    module: 'carton',
  },
  {
    key: 'reports',
    path: '/ecommerce/reports',
    labelKey: 'ecommerce.nav.reports',
    icon: PieChart,
    placeholder: true,
  },
  {
    key: 'settings',
    path: '/ecommerce/settings',
    labelKey: 'ecommerce.nav.settings',
    icon: Setting,
  },
]

const MODULE_PATH_MAP: Record<EcommerceWorkbenchModule, string> = {
  monthlySettlement: '/ecommerce/monthly-settlement',
  order: '/ecommerce/orders',
  express: '/ecommerce/express',
  inventory: '/ecommerce/inventory',
  product: '/ecommerce/products',
  platformShop: '/ecommerce/shops',
  factory: '/ecommerce/factories',
  carton: '/ecommerce/cartons',
}

const LEGACY_TAB_PATH_MAP: Record<string, string> = {
  monthlySettlement: MODULE_PATH_MAP.monthlySettlement,
  order: MODULE_PATH_MAP.order,
  express: MODULE_PATH_MAP.express,
  inventory: MODULE_PATH_MAP.inventory,
  product: MODULE_PATH_MAP.product,
  platformShop: MODULE_PATH_MAP.platformShop,
  factory: MODULE_PATH_MAP.factory,
  carton: MODULE_PATH_MAP.carton,
}

export function ecommercePathForModule(module: EcommerceWorkbenchModule | string): string {
  return MODULE_PATH_MAP[module as EcommerceWorkbenchModule] ?? '/ecommerce'
}

export function ecommercePathForLegacyTab(tab: unknown): string {
  if (typeof tab === 'string' && LEGACY_TAB_PATH_MAP[tab]) {
    return LEGACY_TAB_PATH_MAP[tab]
  }
  return '/ecommerce/products'
}

export const EC_NAV_COLLAPSED_KEY = 'ec-workbench-nav-collapsed'
