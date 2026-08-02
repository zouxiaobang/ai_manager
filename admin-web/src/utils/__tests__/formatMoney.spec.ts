import { describe, it, expect } from 'vitest'
import {
  buildCnyAmountParts,
  formatCnyPlain,
  formatSignedCnyPlain,
  formatMoney,
  formatPrice,
} from '@/utils/formatMoney'

describe('buildCnyAmountParts', () => {
  it('null/undefined/NaN 返回 null', () => {
    expect(buildCnyAmountParts(null)).toBeNull()
    expect(buildCnyAmountParts(undefined)).toBeNull()
    expect(buildCnyAmountParts(Number.NaN)).toBeNull()
  })

  it('小数默认保留 2 位并带符号', () => {
    expect(buildCnyAmountParts(1234.5)?.map((p) => p.text).join('')).toBe('¥1234.50')
  })

  it('负数符号在 ¥ 前', () => {
    expect(buildCnyAmountParts(-500)?.map((p) => p.text).join('')).toBe('-¥500.00')
  })

  it('symbol=false 时负数仅保留负号前缀', () => {
    const parts = buildCnyAmountParts(-500, { symbol: false })
    expect(parts?.map((p) => p.text).join('')).toBe('-500.00')
  })

  it('大额按四位分组并带 万 单位', () => {
    expect(buildCnyAmountParts(1234567.89)?.map((p) => p.text).join('')).toBe('¥123万4567.89')
  })

  it('紧凑模式 万 档', () => {
    expect(formatCnyPlain(393400, { compact: true })).toBe('¥39.34万')
  })

  it('紧凑模式 亿 档', () => {
    expect(formatCnyPlain(123456789, { compact: true })).toBe('¥1.23亿')
  })

  it('fractionDigits=0 时四舍五入为整数', () => {
    expect(formatCnyPlain(100.5, { fractionDigits: 0 })).toBe('¥101')
  })
})

describe('formatCnyPlain / formatMoney / formatPrice', () => {
  it('非法值返回占位符 —', () => {
    expect(formatCnyPlain(null)).toBe('—')
    expect(formatCnyPlain(Number.NaN)).toBe('—')
  })

  it('formatMoney 与 formatPrice 均为 formatCnyPlain 别名', () => {
    expect(formatMoney(10)).toBe('¥10.00')
    expect(formatPrice(10)).toBe('¥10.00')
  })
})

describe('formatSignedCnyPlain', () => {
  it('正数加 + 前缀', () => {
    expect(formatSignedCnyPlain(100)).toBe('+¥100.00')
  })

  it('负数保持原有负号', () => {
    expect(formatSignedCnyPlain(-50)).toBe('-¥50.00')
  })

  it('非法值返回占位符', () => {
    expect(formatSignedCnyPlain(null)).toBe('—')
    expect(formatSignedCnyPlain(undefined)).toBe('—')
  })
})
