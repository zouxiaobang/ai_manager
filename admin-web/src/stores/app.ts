import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import type { I18n } from 'vue-i18n'

export type ThemeMode = 'light' | 'dark'
export type LocaleCode = 'zh-CN' | 'en-US'

const THEME_KEY = 'admin-theme'
const LOCALE_KEY = 'admin-locale'

export const useAppStore = defineStore('app', () => {
  const theme = ref<ThemeMode>('light')
  const locale = ref<LocaleCode>('zh-CN')

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

  function setLocale(code: LocaleCode, i18n?: I18n) {
    locale.value = code
    localStorage.setItem(LOCALE_KEY, code)
    if (i18n) {
      i18n.global.locale.value = code
    }
  }

  function initLocale(i18n: I18n) {
    const saved = localStorage.getItem(LOCALE_KEY) as LocaleCode | null
    const code = saved === 'en-US' || saved === 'zh-CN' ? saved : 'zh-CN'
    setLocale(code, i18n)
  }

  watch(theme, (mode) => {
    document.documentElement.classList.toggle('dark', mode === 'dark')
  })

  return {
    theme,
    locale,
    applyTheme,
    toggleTheme,
    initTheme,
    setLocale,
    initLocale,
  }
})
