import type { InjectionKey } from 'vue'
import type { useMobileFactory } from './useMobileFactory'

export type MobileFactoryContext = ReturnType<typeof useMobileFactory>

export const MOBILE_FACTORY_KEY: InjectionKey<MobileFactoryContext> = Symbol('mobileFactory')
