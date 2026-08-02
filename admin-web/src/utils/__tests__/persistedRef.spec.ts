import { describe, it, expect, beforeEach } from 'vitest'
import { nextTick } from 'vue'
import { createPersistedRef } from '@/utils/persistedRef'

describe('createPersistedRef', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('无持久化值时使用默认值', () => {
    const r = createPersistedRef('k', 5, (raw) => (raw ? Number.parseInt(raw, 10) : 5))
    expect(r.value).toBe(5)
  })

  it('读取合法持久化值', () => {
    localStorage.setItem('k', '8')
    const r = createPersistedRef('k', 5, (raw) => (raw ? Number.parseInt(raw, 10) : 5))
    expect(r.value).toBe(8)
  })

  it('写入 value 时自动持久化到 localStorage', async () => {
    const r = createPersistedRef('k', 5, (raw) => (raw ? Number.parseInt(raw, 10) : 5))
    r.value = 10
    await nextTick()
    expect(localStorage.getItem('k')).toBe('10')
  })

  it('非法持久化值由 parse 回退默认', () => {
    localStorage.setItem('k', '999')
    const r = createPersistedRef('k', 5, (raw) => {
      const n = raw ? Number.parseInt(raw, 10) : 5
      return n >= 1 && n <= 20 ? n : 5
    })
    expect(r.value).toBe(5)
  })

  it('布尔 parse 处理 null 与字符串', () => {
    const r = createPersistedRef('flag', true, (raw) => (raw === null ? true : raw === 'true'))
    expect(r.value).toBe(true)

    localStorage.setItem('flag', 'false')
    const r2 = createPersistedRef('flag', true, (raw) => (raw === null ? true : raw === 'true'))
    expect(r2.value).toBe(false)
  })
})
