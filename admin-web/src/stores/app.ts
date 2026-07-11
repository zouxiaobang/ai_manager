import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import {
  isMobileHomeThemeId,
  MOBILE_HOME_THEME_DEFAULT,
  type MobileHomeThemeId,
} from '@/data/mobile-home-themes'

/** 主题模式类型：浅色/深色 */
export type ThemeMode = 'light' | 'dark'
/** 语言代码类型：中文/英文 */
export type LocaleCode = 'zh-CN' | 'en-US'

/** i18n 实例类型定义（用于类型推断） */
type AppI18n = {
  global: {
    locale: { value: LocaleCode }
  }
}

/** 本地存储的主题 key */
const THEME_KEY = 'admin-theme'
/** 本地存储的语言 key */
const LOCALE_KEY = 'admin-locale'
/** 本地存储的移动端首页主题 key */
const MOBILE_HOME_THEME_KEY = 'mobile-home-theme'

/**
 * 将移动端首页主题应用到 document 根元素
 * 通过 data 属性控制 CSS 变量和样式
 */
function applyMobileHomeThemeToDocument(theme: MobileHomeThemeId) {
  if (typeof document === 'undefined') return
  document.documentElement.dataset.mobileHomeTheme = theme
}

/**
 * 应用全局状态管理 Store
 * <p>
 * 管理主题模式、语言设置、移动端首页主题等全局状态，
 * 支持 localStorage 持久化存储。
 * </p>
 */
export const useAppStore = defineStore('app', () => {
  /** 当前主题模式（浅色/深色） */
  const theme = ref<ThemeMode>('light')
  /** 当前语言代码 */
  const locale = ref<LocaleCode>('zh-CN')
  /** 移动端首页主题ID */
  const mobileHomeTheme = ref<MobileHomeThemeId>(MOBILE_HOME_THEME_DEFAULT)

  /**
   * 应用指定的主题模式
   * @param mode 主题模式 light | dark
   */
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

  /**
   * 切换主题（浅色/深色互切）
   */
  function toggleTheme() {
    applyTheme(theme.value === 'light' ? 'dark' : 'light')
  }

  /**
   * 初始化主题：从 localStorage 读取，或跟随系统偏好
   */
  function initTheme() {
    const saved = localStorage.getItem(THEME_KEY) as ThemeMode | null
    if (saved === 'light' || saved === 'dark') {
      applyTheme(saved)
      return
    }
    // 未设置时跟随系统偏好
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    applyTheme(prefersDark ? 'dark' : 'light')
  }

  /**
   * 设置语言
   * @param code 语言代码
   * @param i18n i18n 实例（可选）
   */
  function setLocale(code: LocaleCode, i18n?: AppI18n) {
    locale.value = code
    localStorage.setItem(LOCALE_KEY, code)
    if (i18n) {
      i18n.global.locale.value = code
    }
  }

  /**
   * 初始化语言设置：从 localStorage 读取，默认中文
   * @param i18n i18n 实例
   */
  function initLocale(i18n: AppI18n) {
    const saved = localStorage.getItem(LOCALE_KEY) as LocaleCode | null
    const code = saved === 'en-US' || saved === 'zh-CN' ? saved : 'zh-CN'
    setLocale(code, i18n)
  }

  /**
   * 设置移动端首页主题
   * @param id 主题ID
   */
  function setMobileHomeTheme(id: MobileHomeThemeId) {
    mobileHomeTheme.value = id
    localStorage.setItem(MOBILE_HOME_THEME_KEY, id)
    applyMobileHomeThemeToDocument(id)
  }

  /**
   * 初始化移动端首页主题：从 localStorage 读取，使用默认主题
   */
  function initMobileHomeTheme() {
    const saved = localStorage.getItem(MOBILE_HOME_THEME_KEY)
    const id = isMobileHomeThemeId(saved) ? saved : MOBILE_HOME_THEME_DEFAULT
    setMobileHomeTheme(id)
  }

  // 监听主题变化，同步到 DOM class
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
