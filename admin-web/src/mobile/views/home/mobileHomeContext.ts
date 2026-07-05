import type { InjectionKey } from 'vue'
import type { useMobileHome } from './useMobileHome'

export type MobileHomeContext = ReturnType<typeof useMobileHome>

export const MOBILE_HOME_KEY: InjectionKey<MobileHomeContext> = Symbol('mobileHome')
