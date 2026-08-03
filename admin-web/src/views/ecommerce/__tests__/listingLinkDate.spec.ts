import { describe, expect, it, vi } from 'vitest'
import { defaultListingDate, toListingDate, toListingDateTime } from '../listingLinkDate'

vi.mock('@/utils/date', () => ({
  todayDateString: () => '2026-08-03',
}))

describe('listingLinkDate 上架时间域', () => {
  describe('defaultListingDate', () => {
    it('返回今天日期', () => {
      expect(defaultListingDate()).toBe('2026-08-03')
    })
  })

  describe('toListingDate', () => {
    it('空值回退今天', () => {
      expect(toListingDate()).toBe('2026-08-03')
      expect(toListingDate(null)).toBe('2026-08-03')
      expect(toListingDate('')).toBe('2026-08-03')
      // 纯空格 trim 后为空串，原逻辑不触发空值回退
      expect(toListingDate('   ')).toBe('')
    })

    it('trim 后取日期部分', () => {
      expect(toListingDate(' 2026-08-01 12:30:00 ')).toBe('2026-08-01')
      expect(toListingDate('2026-08-15T10:00:00')).toBe('2026-08-15')
    })

    it('本身是纯日期时原样返回', () => {
      expect(toListingDate('2026-08-20')).toBe('2026-08-20')
    })
  })

  describe('toListingDateTime', () => {
    it('空串原样返回', () => {
      expect(toListingDateTime('')).toBe('')
    })

    it('仅日期补齐零时分秒', () => {
      expect(toListingDateTime('2026-08-01')).toBe('2026-08-01 00:00:00')
    })

    it('完整时间戳原样返回', () => {
      expect(toListingDateTime('2026-08-01 12:30:00')).toBe('2026-08-01 12:30:00')
    })
  })
})
