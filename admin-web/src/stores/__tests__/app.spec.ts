import { beforeEach, describe, expect, it } from 'vitest'
import { nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { useAppStore, PRIMARY_COLORS } from '@/stores/app'
import { MOBILE_HOME_THEME_DEFAULT } from '@/data/mobile-home-themes'

describe('app store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('番茄钟提示次数从 localStorage 恢复并回写', async () => {
    localStorage.setItem('pomodoro-beeps', '12')
    const store = useAppStore()
    expect(store.pomodoroBeeps).toBe(12)

    store.setPomodoroBeeps(7)
    await nextTick()
    expect(localStorage.getItem('pomodoro-beeps')).toBe('7')
  })

  it('非法持久化的音量回退默认 0.3', () => {
    localStorage.setItem('todo-remind-volume', '99')
    const store = useAppStore()
    expect(store.todoRemindVolume).toBe(0.3)
  })

  it('applyTheme 写入 localStorage 并同步 DOM class', () => {
    const store = useAppStore()
    store.applyTheme('dark')
    expect(localStorage.getItem('admin-theme')).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('toggleTheme 在浅色与深色间切换', () => {
    const store = useAppStore()
    const before = store.theme
    store.toggleTheme()
    expect(store.theme).not.toBe(before)
  })

  it('setLocale 更新语言并持久化', () => {
    const store = useAppStore()
    store.setLocale('en-US')
    expect(store.locale).toBe('en-US')
    expect(localStorage.getItem('admin-locale')).toBe('en-US')
  })

  it('initLocale 读取非法值时回退中文', () => {
    localStorage.setItem('admin-locale', 'fr-FR')
    const store = useAppStore()
    store.initLocale({ global: { locale: { value: 'zh-CN' } } })
    expect(store.locale).toBe('zh-CN')
  })

  it('setMobileHomeTheme 持久化主题 ID', () => {
    const store = useAppStore()
    store.setMobileHomeTheme(MOBILE_HOME_THEME_DEFAULT)
    expect(localStorage.getItem('mobile-home-theme')).toBe(MOBILE_HOME_THEME_DEFAULT)
  })

  it('applyPrimaryColor 写入 localStorage 与 CSS 变量', () => {
    const store = useAppStore()
    store.applyPrimaryColor(PRIMARY_COLORS[0].value)
    expect(localStorage.getItem('admin-primary-color')).toBe(PRIMARY_COLORS[0].value)
    expect(document.documentElement.style.getPropertyValue('--wr-primary')).toBe(PRIMARY_COLORS[0].value)
  })

  it('initPrimaryColor 读取非法颜色时回退默认', () => {
    localStorage.setItem('admin-primary-color', 'not-a-color')
    const store = useAppStore()
    store.initPrimaryColor()
    expect(store.primaryColor).toBe('#2563eb')
  })
})
