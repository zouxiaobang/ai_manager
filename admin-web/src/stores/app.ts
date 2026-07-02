import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import {
  isMobileHomeThemeId,
  MOBILE_HOME_THEME_DEFAULT,
  type MobileHomeThemeId,
} from '@/data/mobile-home-themes'

export type ThemeMode = 'light' | 'dark'
export type LocaleCode = 'zh-CN' | 'en-US'

type AppI18n = {
  global: {
    locale: { value: LocaleCode }
  }
}

const THEME_KEY = 'admin-theme'
const LOCALE_KEY = 'admin-locale'
const MOBILE_HOME_THEME_KEY = 'mobile-home-theme'

function applyMobileHomeThemeToDocument(theme: MobileHomeThemeId) {
  if (typeof document === 'undefined') return
  document.documentElement.dataset.mobileHomeTheme = theme
}

export const useAppStore = defineStore('app', () => {
  const theme = ref<ThemeMode>('light')
  const locale = ref<LocaleCode>('zh-CN')
  const mobileHomeTheme = ref<MobileHomeThemeId>(MOBILE_HOME_THEME_DEFAULT)

  function applyTheme(mode: ThemeMode) {
    theme.value = mode
    const root = document.documentElement
    if (mode === 'dark') {
      root.classList.add('dark')
    } else {
      root.classList.remove('dark')
    }
    localStorage.setItem(THEME_KEY, mode)
  }

  function toggleTheme() {
    applyTheme(theme.value === 'light' ? 'dark' : 'light')
  }

  function initTheme() {
    const saved = localStorage.getItem(THEME_KEY) as ThemeMode | null
    if (saved === 'light' || saved === 'dark') {
      applyTheme(saved)
      return
    }
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    applyTheme(prefersDark ? 'dark' : 'light')
  }

  function setLocale(code: LocaleCode, i18n?: AppI18n) {
    locale.value = code
    localStorage.setItem(LOCALE_KEY, code)
    if (i18n) {
      i18n.global.locale.value = code
    }
  }

  function initLocale(i18n: AppI18n) {
    const saved = localStorage.getItem(LOCALE_KEY) as LocaleCode | null
    const code = saved === 'en-US' || saved === 'zh-CN' ? saved : 'zh-CN'
    setLocale(code, i18n)
  }

  function setMobileHomeTheme(id: MobileHomeThemeId) {
    mobileHomeTheme.value = id
    localStorage.setItem(MOBILE_HOME_THEME_KEY, id)
    applyMobileHomeThemeToDocument(id)
  }

  function initMobileHomeTheme() {
    const saved = localStorage.getItem(MOBILE_HOME_THEME_KEY)
    const id = isMobileHomeThemeId(saved) ? saved : MOBILE_HOME_THEME_DEFAULT
    setMobileHomeTheme(id)
  }

  watch(theme, (mode) => {
    document.documentElement.classList.toggle('dark', mode === 'dark')
  })

  return {
    theme,
    locale,
    mobileHomeTheme,
    applyTheme,
    toggleTheme,
    initTheme,
    setLocale,
    initLocale,
    setMobileHomeTheme,
    initMobileHomeTheme,
  }
})
