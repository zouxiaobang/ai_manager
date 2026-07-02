import type { InjectionKey } from 'vue'
import type { useMobileEcommerce } from './useMobileEcommerce'

export type MobileEcommerceContext = ReturnType<typeof useMobileEcommerce>

export const MOBILE_ECOMMERCE_KEY: InjectionKey<MobileEcommerceContext> = Symbol('mobileEcommerce')
