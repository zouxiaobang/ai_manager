import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import {
  isMobileHomeThemeId,
  MOBILE_HOME_THEME_DEFAULT,
  type MobileHomeThemeId,
} from '@/data/mobile-home-themes'

/** 将 hex 颜色解析为 RGB 分量 */
function hexToRgb(hex: string) {
  const c = hex.replace('#', '')
  return {
    r: parseInt(c.substring(0, 2), 16),
    g: parseInt(c.substring(2, 4), 16),
    b: parseInt(c.substring(4, 6), 16),
  }
}

/** 混合两个 RGB 颜色 */
function mixRgb(r1: number, g1: number, b1: number, r2: number, g2: number, b2: number, weight: number) {
  const r = Math.round(r1 * (1 - weight) + r2 * weight)
  const g = Math.round(g1 * (1 - weight) + g2 * weight)
  const b = Math.round(b1 * (1 - weight) + b2 * weight)
  return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`
}

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
/** 本地存储的主色 key */
const PRIMARY_COLOR_KEY = 'admin-primary-color'
/** 本地存储的番茄钟提示次数 key */
const POMODORO_BEEPS_KEY = 'pomodoro-beeps'
/** 本地存储的番茄钟音量 key */
const POMODORO_VOLUME_KEY = 'pomodoro-volume'
/** 本地存储的番茄钟音效开关 key */
const POMODORO_SOUND_ENABLED_KEY = 'pomodoro-sound-enabled'
/** 本地存储的待办提醒音量 key */
const TODO_REMIND_VOLUME_KEY = 'todo-remind-volume'
/** 本地存储的待办提醒声音开关 key */
const TODO_REMIND_ENABLED_KEY = 'todo-remind-enabled'
/** 本地存储的待办提醒提示次数 key */
const TODO_REMIND_BEEPS_KEY = 'todo-remind-beeps'

/** 预设主色方案 */
export const PRIMARY_COLORS = [
  { name: '经典蓝', nameEn: 'Blue', value: '#2563eb' },
  { name: '紫色', nameEn: 'Purple', value: '#7c3aed' },
  { name: '绿色', nameEn: 'Green', value: '#059669' },
  { name: '红色', nameEn: 'Red', value: '#dc2626' },
  { name: '橙色', nameEn: 'Orange', value: '#ea580c' },
  { name: '青色', nameEn: 'Cyan', value: '#0891b2' },
] as const

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
  /** 主色 */
  const primaryColor = ref('#2563eb')
  /** 番茄钟完成时提示音重复次数 */
  const pomodoroBeeps = ref(5)
  /** 番茄钟完成时提示音音量 (0-1) */
  const pomodoroVolume = ref(0.3)
  /** 番茄钟完成时提示音开关 */
  const pomodoroSoundEnabled = ref(true)
  /** 待办提醒声音开关 */
  const todoRemindEnabled = ref(true)
  /** 待办提醒音量 (0-1) */
  const todoRemindVolume = ref(0.3)
  /** 待办提醒提示次数 */
  const todoRemindBeeps = ref(3)

  /**
   * 应用主色到 CSS 变量（含 Element Plus 主题色）
   */
  function applyPrimaryColor(color: string) {
    primaryColor.value = color
    if (typeof document !== 'undefined') {
      const root = document.documentElement
      root.style.setProperty('--wr-primary', color)
      const { r, g, b } = hexToRgb(color)
      root.style.setProperty('--el-color-primary', color)
      root.style.setProperty('--el-color-primary-light-3', mixRgb(r, g, b, 255, 255, 255, 0.3))
      root.style.setProperty('--el-color-primary-light-5', mixRgb(r, g, b, 255, 255, 255, 0.5))
      root.style.setProperty('--el-color-primary-light-7', mixRgb(r, g, b, 255, 255, 255, 0.7))
      root.style.setProperty('--el-color-primary-light-8', mixRgb(r, g, b, 255, 255, 255, 0.8))
      root.style.setProperty('--el-color-primary-light-9', mixRgb(r, g, b, 255, 255, 255, 0.9))
      root.style.setProperty('--el-color-primary-dark-2', mixRgb(r, g, b, 0, 0, 0, 0.2))
    }
    localStorage.setItem(PRIMARY_COLOR_KEY, color)
  }

  /**
   * 初始化主色
   */
  function initPrimaryColor() {
    const saved = localStorage.getItem(PRIMARY_COLOR_KEY)
    if (saved && /^#[0-9a-fA-F]{6}$/.test(saved)) {
      applyPrimaryColor(saved)
    } else {
      applyPrimaryColor('#2563eb')
    }
  }

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

  /**
   * 设置番茄钟提示音重复次数
   */
  function setPomodoroBeeps(beeps: number) {
    pomodoroBeeps.value = beeps
    localStorage.setItem(POMODORO_BEEPS_KEY, String(beeps))
  }

  /**
   * 初始化番茄钟提示音重复次数
   */
  function initPomodoroBeeps() {
    const saved = localStorage.getItem(POMODORO_BEEPS_KEY)
    const n = saved ? parseInt(saved, 10) : 5
    pomodoroBeeps.value = n >= 1 && n <= 20 ? n : 5
  }

  /**
   * 设置番茄钟提示音音量
   */
  function setPomodoroVolume(volume: number) {
    pomodoroVolume.value = volume
    localStorage.setItem(POMODORO_VOLUME_KEY, String(volume))
  }

  /**
   * 初始化番茄钟提示音音量
   */
  function initPomodoroVolume() {
    const saved = localStorage.getItem(POMODORO_VOLUME_KEY)
    const v = saved ? parseFloat(saved) : 0.3
    pomodoroVolume.value = v >= 0 && v <= 1 ? v : 0.3
  }

  /**
   * 设置番茄钟提示音开关
   */
  function setPomodoroSoundEnabled(enabled: boolean) {
    pomodoroSoundEnabled.value = enabled
    localStorage.setItem(POMODORO_SOUND_ENABLED_KEY, String(enabled))
  }

  /**
   * 初始化番茄钟提示音开关
   */
  function initPomodoroSoundEnabled() {
    const saved = localStorage.getItem(POMODORO_SOUND_ENABLED_KEY)
    if (saved !== null) {
      pomodoroSoundEnabled.value = saved === 'true'
    }
  }

  /**
   * 设置待办提醒声音开关
   */
  function setTodoRemindEnabled(enabled: boolean) {
    todoRemindEnabled.value = enabled
    localStorage.setItem(TODO_REMIND_ENABLED_KEY, String(enabled))
  }

  /**
   * 初始化待办提醒声音开关
   */
  function initTodoRemindEnabled() {
    const saved = localStorage.getItem(TODO_REMIND_ENABLED_KEY)
    if (saved !== null) {
      todoRemindEnabled.value = saved === 'true'
    }
  }

  /**
   * 设置待办提醒音量
   */
  function setTodoRemindVolume(volume: number) {
    todoRemindVolume.value = volume
    localStorage.setItem(TODO_REMIND_VOLUME_KEY, String(volume))
  }

  /**
   * 初始化待办提醒音量
   */
  function initTodoRemindVolume() {
    const saved = localStorage.getItem(TODO_REMIND_VOLUME_KEY)
    const v = saved ? parseFloat(saved) : 0.3
    todoRemindVolume.value = v >= 0 && v <= 1 ? v : 0.3
  }

  /**
   * 设置待办提醒提示次数
   */
  function setTodoRemindBeeps(beeps: number) {
    todoRemindBeeps.value = beeps
    localStorage.setItem(TODO_REMIND_BEEPS_KEY, String(beeps))
  }

  /**
   * 初始化待办提醒提示次数
   */
  function initTodoRemindBeeps() {
    const saved = localStorage.getItem(TODO_REMIND_BEEPS_KEY)
    const n = saved ? parseInt(saved, 10) : 3
    todoRemindBeeps.value = n >= 1 && n <= 20 ? n : 3
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
    primaryColor,
    applyPrimaryColor,
    initPrimaryColor,
    pomodoroBeeps,
    pomodoroVolume,
    setPomodoroBeeps,
    setPomodoroVolume,
    initPomodoroBeeps,
    initPomodoroVolume,
    pomodoroSoundEnabled,
    setPomodoroSoundEnabled,
    initPomodoroSoundEnabled,
    todoRemindEnabled,
    todoRemindVolume,
    setTodoRemindEnabled,
    setTodoRemindVolume,
    initTodoRemindEnabled,
    initTodoRemindVolume,
    todoRemindBeeps,
    setTodoRemindBeeps,
    initTodoRemindBeeps,
  }
})
