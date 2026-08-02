import { describe, it, expect } from 'vitest'
import {
  EMPTY_DATE,
  formatDateParam,
  addDays,
  formatDate,
  formatDateTime,
  todayDateString,
  tomorrowDateString,
  toApiDateTime,
  formatMonth,
  shiftMonth,
  formatMonthDay,
  monthDateRange,
  defaultOrderMonth,
} from '@/utils/date'

describe('formatDateParam', () => {
  it('输出 yyyy-MM-dd', () => {
    expect(formatDateParam(new Date(2026, 7, 2))).toBe('2026-08-02')
  })

  it('月日不足两位补零', () => {
    expect(formatDateParam(new Date(2026, 0, 5))).toBe('2026-01-05')
  })
})

describe('addDays', () => {
  it('返回新实例不修改原日期', () => {
    const base = new Date(2026, 7, 2)
    const next = addDays(base, 3)
    expect(next.getDate()).toBe(5)
    expect(base.getDate()).toBe(2)
  })

  it('支持负数（往前移）', () => {
    expect(addDays(new Date(2026, 7, 2), -1).getDate()).toBe(1)
  })
})

describe('formatDate', () => {
  it('null/空串返回占位符', () => {
    expect(formatDate(null)).toBe(EMPTY_DATE)
    expect(formatDate('')).toBe(EMPTY_DATE)
  })

  it('Date 实例取日期部分', () => {
    expect(formatDate(new Date(2026, 7, 2, 13, 30))).toBe('2026-08-02')
  })

  it('带时间戳字符串归一化为日期', () => {
    expect(formatDate('2026-08-02T15:00:00')).toBe('2026-08-02')
  })

  it('非法字符串原样返回', () => {
    expect(formatDate('not-a-date')).toBe('not-a-date')
  })
})

describe('formatDateTime', () => {
  it('null 返回占位符', () => {
    expect(formatDateTime(null)).toBe(EMPTY_DATE)
  })

  it('Date 实例输出完整时间', () => {
    expect(formatDateTime(new Date(2026, 7, 2, 9, 5, 6))).toBe('2026-08-02 09:05:06')
  })

  it('仅日期补 00:00:00', () => {
    expect(formatDateTime('2026-08-02')).toBe('2026-08-02 00:00:00')
  })

  it('带 T 与毫秒的时间串归一化', () => {
    expect(formatDateTime('2026-08-02T15:30:00.123')).toBe('2026-08-02 15:30:00')
  })
})

describe('日期工具', () => {
  it('todayDateString 为今天', () => {
    expect(todayDateString()).toBe(formatDateParam(new Date()))
  })

  it('tomorrowDateString 为明天', () => {
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    expect(tomorrowDateString()).toBe(formatDateParam(tomorrow))
  })

  it('toApiDateTime 仅日期补 T 前缀', () => {
    expect(toApiDateTime('2026-08-02')).toBe('2026-08-02T00:00:00')
    expect(toApiDateTime('2026-08-02T10:00:00')).toBe('2026-08-02T10:00:00')
    expect(toApiDateTime('')).toBe('')
  })

  it('formatMonth 输出 yyyy-MM', () => {
    expect(formatMonth(new Date(2026, 11, 15))).toBe('2026-12')
  })

  it('shiftMonth 跨年偏移', () => {
    expect(shiftMonth('2026-01', -1)).toBe('2025-12')
    expect(shiftMonth('2026-12', 1)).toBe('2027-01')
  })

  it('formatMonthDay 输出 M月D日', () => {
    expect(formatMonthDay(new Date(2026, 0, 9))).toBe('1月9日')
    expect(formatMonthDay('2026-02-03')).toBe('2月3日')
    expect(formatMonthDay(null)).toBe(EMPTY_DATE)
    expect(formatMonthDay('invalid')).toBe(EMPTY_DATE)
  })

  it('defaultOrderMonth 为上个月', () => {
    const now = new Date()
    const expected = shiftMonth(formatMonth(now), -1)
    expect(defaultOrderMonth()).toBe(expected)
  })

  it('monthDateRange 返回当月首末日期', () => {
    expect(monthDateRange('2026-02')).toEqual(['2026-02-01', '2026-02-28'])
    expect(monthDateRange('2024-02')).toEqual(['2024-02-01', '2024-02-29'])
  })
})
