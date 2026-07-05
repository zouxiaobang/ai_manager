import type { EcommerceWorkbenchModule } from '@/data/ecommerce-nav'

export interface MobileEcommerceModule {
  key: EcommerceWorkbenchModule
  icon: string
  borderColor: string
  squiggleColor: string
  labelKey: string
  segment: string
}

export const MOBILE_EC_MODULE_SEGMENTS: Record<string, EcommerceWorkbenchModule> = {
  'monthly-settlement': 'monthlySettlement',
  orders: 'order',
  inventory: 'inventory',
  products: 'product',
  express: 'express',
  factories: 'factory',
  shops: 'platformShop',
  cartons: 'carton',
}

export const mobileEcommerceModules: MobileEcommerceModule[] = [
  {
    key: 'monthlySettlement',
    icon: '/mobile-home/scheme-a/icon-chart.svg',
    borderColor: '#f59e0b',
    squiggleColor: '#f59e0b',
    labelKey: 'ecommerce.nav.monthlySettlement',
    segment: 'monthly-settlement',
  },
  {
    key: 'order',
    icon: '/mobile-home/scheme-a/icon-order.svg',
    borderColor: '#3b82f6',
    squiggleColor: '#2563eb',
    labelKey: 'ecommerce.nav.order',
    segment: 'orders',
  },
  {
    key: 'inventory',
    icon: '/mobile-home/scheme-a/icon-warehouse.svg',
    borderColor: '#22c55e',
    squiggleColor: '#16a34a',
    labelKey: 'ecommerce.nav.inventory',
    segment: 'inventory',
  },
  {
    key: 'product',
    icon: '/mobile-home/scheme-a/icon-product.svg',
    borderColor: '#6366f1',
    squiggleColor: '#6366f1',
    labelKey: 'ecommerce.nav.product',
    segment: 'products',
  },
  {
    key: 'express',
    icon: '/mobile-home/scheme-a/icon-truck.svg',
    borderColor: '#f97316',
    squiggleColor: '#ea580c',
    labelKey: 'ecommerce.nav.express',
    segment: 'express',
  },
  {
    key: 'factory',
    icon: '/mobile-home/scheme-a/icon-factory.svg',
    borderColor: '#8b5cf6',
    squiggleColor: '#7c3aed',
    labelKey: 'ecommerce.nav.factory',
    segment: 'factories',
  },
  {
    key: 'platformShop',
    icon: '/mobile-home/scheme-a/icon-shop.svg',
    borderColor: '#10b981',
    squiggleColor: '#059669',
    labelKey: 'ecommerce.nav.platformShop',
    segment: 'shops',
  },
  {
    key: 'carton',
    icon: '/mobile-home/scheme-a/icon-box.svg',
    borderColor: '#94a3b8',
    squiggleColor: '#64748b',
    labelKey: 'ecommerce.nav.carton',
    segment: 'cartons',
  },
]

export function mobileEcommercePathForModule(key: EcommerceWorkbenchModule): string {
  if (key === 'factory') {
    return '/ecommerce/factory'
  }
  const item = mobileEcommerceModules.find((m) => m.key === key)
  return item ? `/ecommerce/${item.segment}` : '/ecommerce'
}

export function mobileEcommerceModuleFromSegment(segment: string): EcommerceWorkbenchModule | null {
  return MOBILE_EC_MODULE_SEGMENTS[segment] ?? null
}
