import type { InjectionKey } from 'vue'
import type { useMobileProducts } from './useMobileProducts'

export type MobileProductsContext = ReturnType<typeof useMobileProducts>

export const MOBILE_PRODUCTS_KEY: InjectionKey<MobileProductsContext> = Symbol('mobileProducts')
