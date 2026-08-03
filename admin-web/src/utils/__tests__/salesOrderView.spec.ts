import { describe, expect, it } from 'vitest'
import {
  formatImportFileSize,
  importLineStatusTagType,
  orderShopColor,
  orderStatColor,
  parseManualCostNumber,
  parseOrderMonthFromQuery,
  sanitizeManualCostInput,
  statusTagType,
} from '../salesOrderView'

describe('salesOrderView 纯函数', () => {
  describe('orderStatColor', () => {
    it('映射各 tone 颜色', () => {
      expect(orderStatColor('green')).toBe('#22c55e')
      expect(orderStatColor('orange')).toBe('#f59e0b')
      expect(orderStatColor('purple')).toBe('#8b5cf6')
      expect(orderStatColor('blue')).toBe('#2563eb')
    })
    it('未知 tone 返回默认灰色', () => {
      expect(orderStatColor('unknown')).toBe('#cbd5e1')
    })
  })

  describe('orderShopColor', () => {
    it('激活态优先蓝色', () => {
      expect(orderShopColor('green', true)).toBe('#2563eb')
    })
    it('映射非激活态 tone', () => {
      expect(orderShopColor('green', false)).toBe('#22c55e')
      expect(orderShopColor('orange', false)).toBe('#f59e0b')
      expect(orderShopColor('gray', false)).toBe('#94a3b8')
    })
    it('未知 tone 返回默认灰色', () => {
      expect(orderShopColor('pink', false)).toBe('#cbd5e1')
    })
  })

  describe('parseOrderMonthFromQuery', () => {
    it('解析合法月份', () => {
      expect(parseOrderMonthFromQuery('2024-03')).toBe('2024-03')
      expect(parseOrderMonthFromQuery(' 2024-12 ')).toBe('2024-12')
    })
    it('解析数组形式路由参数', () => {
      expect(parseOrderMonthFromQuery(['2024-05', '2024-06'])).toBe('2024-05')
    })
    it('非法或空值返回 null', () => {
      expect(parseOrderMonthFromQuery('2024/03')).toBeNull()
      expect(parseOrderMonthFromQuery('')).toBeNull()
      expect(parseOrderMonthFromQuery(null)).toBeNull()
      expect(parseOrderMonthFromQuery(123)).toBeNull()
    })
  })

  describe('formatImportFileSize', () => {
    it('B 单位', () => {
      expect(formatImportFileSize(512)).toBe('512 B')
    })
    it('KB 带小数', () => {
      expect(formatImportFileSize(1024 * 5)).toBe('5.0 KB')
    })
    it('KB 取整', () => {
      expect(formatImportFileSize(1024 * 20)).toBe('20 KB')
    })
    it('MB 保留一位小数', () => {
      expect(formatImportFileSize(1024 * 1024 * 3)).toBe('3.0 MB')
    })
  })

  describe('sanitizeManualCostInput', () => {
    it('空输入返回空串', () => {
      expect(sanitizeManualCostInput('')).toBe('')
    })
    it('去除非法字符', () => {
      expect(sanitizeManualCostInput('12a34')).toBe('1234')
    })
    it('只保留第一个小数点', () => {
      expect(sanitizeManualCostInput('12.5.5')).toBe('12.55')
    })
    it('限制两位小数', () => {
      expect(sanitizeManualCostInput('12.345')).toBe('12.34')
    })
    it('保留负号', () => {
      expect(sanitizeManualCostInput('-12.5')).toBe('-12.5')
    })
    it('仅负号保留', () => {
      expect(sanitizeManualCostInput('-')).toBe('-')
    })
    it('清理首尾空格', () => {
      expect(sanitizeManualCostInput('  9.9 ')).toBe('9.9')
    })
  })

  describe('parseManualCostNumber', () => {
    it('解析合法数字', () => {
      expect(parseManualCostNumber('12.5')).toBe(12.5)
      expect(parseManualCostNumber('-3')).toBe(-3)
    })
    it('符号/空/非法返回 undefined', () => {
      expect(parseManualCostNumber('')).toBeUndefined()
      expect(parseManualCostNumber('-')).toBeUndefined()
      expect(parseManualCostNumber('.')).toBeUndefined()
      expect(parseManualCostNumber('-.')).toBeUndefined()
      expect(parseManualCostNumber('abc')).toBeUndefined()
    })
  })

  describe('importLineStatusTagType', () => {
    it('映射各导入状态', () => {
      expect(importLineStatusTagType('PAID')).toBe('warning')
      expect(importLineStatusTagType('SHIPPED')).toBe('primary')
      expect(importLineStatusTagType('COMPLETED')).toBe('success')
      expect(importLineStatusTagType('CANCELLED')).toBe('info')
      expect(importLineStatusTagType('PARTIAL_REFUND')).toBe('danger')
      expect(importLineStatusTagType('REFUNDED')).toBe('danger')
      expect(importLineStatusTagType('RETURNED')).toBe('info')
    })
    it('空状态返回 info', () => {
      expect(importLineStatusTagType()).toBe('info')
      expect(importLineStatusTagType(null)).toBe('info')
    })
  })

  describe('statusTagType', () => {
    it('映射订单主状态', () => {
      expect(statusTagType('DRAFT')).toBe('info')
      expect(statusTagType('PAID')).toBe('primary')
      expect(statusTagType('PARTIAL_SHIPPED')).toBe('warning')
      expect(statusTagType('SHIPPED')).toBe('warning')
      expect(statusTagType('PARTIAL_REFUND')).toBe('danger')
      expect(statusTagType('REFUNDED')).toBe('danger')
      expect(statusTagType('CANCELLED')).toBe('danger')
      expect(statusTagType('COMPLETED')).toBe('success')
    })
    it('未知状态返回 undefined', () => {
      expect(statusTagType('UNKNOWN')).toBeUndefined()
      expect(statusTagType()).toBeUndefined()
    })
  })
})
