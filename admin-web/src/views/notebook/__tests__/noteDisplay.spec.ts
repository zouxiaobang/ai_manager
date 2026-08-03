import { describe, expect, it, vi } from 'vitest'
import type { NbNoteTag } from '@/api/notebook'
import { formatNoteDisplayTime, getTagPillStyle, startOfDay, type NoteI18nT } from '../noteDisplay'

function makeT(): NoteI18nT & ReturnType<typeof vi.fn> {
  return vi.fn((key: string, params?: Record<string, unknown>) => `${key}::${JSON.stringify(params)}`)
}

/** 构造某天 HH:mm 的 ISO 时间字符串（本地时区） */
function isoOf(daysAgo: number, hour: number, minute: number): string {
  const d = new Date()
  d.setDate(d.getDate() - daysAgo)
  d.setHours(hour, minute, 0, 0)
  return d.toISOString()
}

describe('noteDisplay 展示纯函数', () => {
  describe('startOfDay', () => {
    it('清零时分秒', () => {
      const d = new Date(2026, 7, 3, 15, 30, 45)
      const start = startOfDay(d)
      expect(start.getFullYear()).toBe(2026)
      expect(start.getMonth()).toBe(7)
      expect(start.getDate()).toBe(3)
      expect(start.getHours()).toBe(0)
      expect(start.getMinutes()).toBe(0)
      expect(start.getSeconds()).toBe(0)
    })
  })

  describe('formatNoteDisplayTime', () => {
    it('空值返回空串', () => {
      const t = makeT()
      expect(formatNoteDisplayTime(undefined, t)).toBe('')
      expect(formatNoteDisplayTime('', t)).toBe('')
    })

    it('非法时间返回原样并裁掉秒', () => {
      const t = makeT()
      expect(formatNoteDisplayTime('not-a-date', t)).toBe('not-a-date')
      expect(formatNoteDisplayTime('2026-13-40 10:20:30', t)).toBe('2026-13-40 10:20')
    })

    it('今天显示 todayAt', () => {
      const t = makeT()
      const result = formatNoteDisplayTime(isoOf(0, 9, 30), t)
      expect(t).toHaveBeenCalledWith('notebook.todayAt', { time: '09:30' })
      expect(result).toContain('notebook.todayAt')
    })

    it('昨天显示 yesterdayAt', () => {
      const t = makeT()
      formatNoteDisplayTime(isoOf(1, 18, 5), t)
      expect(t).toHaveBeenCalledWith('notebook.yesterdayAt', { time: '18:05' })
    })

    it('前天显示 dayBeforeYesterdayAt', () => {
      const t = makeT()
      formatNoteDisplayTime(isoOf(2, 8, 0), t)
      expect(t).toHaveBeenCalledWith('notebook.dayBeforeYesterdayAt', { time: '08:00' })
    })

    it('三天前显示完整日期', () => {
      const t = makeT()
      const daysAgo = isoOf(3, 12, 45)
      const result = formatNoteDisplayTime(daysAgo, t)
      expect(result).toMatch(/^\d{4}-\d{2}-\d{2} 12:45$/)
      expect(t).not.toHaveBeenCalled()
    })

    it('兼容无 T 的时间串', () => {
      const t = makeT()
      formatNoteDisplayTime(isoOf(0, 9, 0).replace('T', ' '), t)
      expect(t).toHaveBeenCalled()
    })
  })

  describe('getTagPillStyle', () => {
    it('自定义色生成浅底同色字', () => {
      const style = getTagPillStyle({ id: 1, name: '红', color: '#ef4444' })
      expect(style).toEqual({ backgroundColor: '#ef44441a', color: '#ef4444' })
    })

    it('无自定义色按索引轮换主题', () => {
      const tag: NbNoteTag = { id: 1, name: '无' }
      const s0 = getTagPillStyle(tag, 0)
      const s1 = getTagPillStyle(tag, 1)
      const s4 = getTagPillStyle(tag, 4)
      expect(s0).not.toEqual(s1)
      // 4 与 0 同余轮回到第一个主题
      expect(s4).toEqual(s0)
    })
  })
})
