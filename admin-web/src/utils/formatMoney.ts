export type CnyAmountPart =
  | { kind: 'symbol'; text: string }
  | { kind: 'digits'; text: string }
  | { kind: 'unit'; text: string }

export interface FormatCnyOptions {
  /** 是否显示 ¥ 符号，默认 true */
  symbol?: boolean
  /** 小数位数，默认 2 */
  fractionDigits?: number
  /** 紧凑模式：大额使用单一万/亿单位，如 ¥39.34万 */
  compact?: boolean
}

const QUANTIFIER_UNITS = ['', '万', '亿', '万亿'] as const

function formatIntegerGroups(integerPart: number, fractionText: string): Array<{ digits: string; unit?: string }> {
  if (integerPart < 10000) {
    return [{ digits: `${integerPart}${fractionText}` }]
  }

  const groups: string[] = []
  let remaining = integerPart
  while (remaining > 0) {
    groups.push(String(remaining % 10000))
    remaining = Math.floor(remaining / 10000)
  }

  const segments: Array<{ digits: string; unit?: string }> = []
  for (let i = groups.length - 1; i >= 0; i -= 1) {
    const isLast = i === 0
    segments.push({
      digits: isLast ? `${groups[i]}${fractionText}` : groups[i],
      unit: i > 0 ? QUANTIFIER_UNITS[i] : undefined,
    })
  }
  return segments
}

function trimFractionZeros(text: string): string {
  if (!text.includes('.')) return text
  return text.replace(/\.?0+$/, '')
}

function formatCompactSegments(abs: number, fractionDigits: number): Array<{ digits: string; unit?: string }> {
  if (abs < 10000) {
    const text = fractionDigits > 0 ? abs.toFixed(fractionDigits) : String(Math.round(abs))
    return [{ digits: trimFractionZeros(text) }]
  }
  if (abs < 100000000) {
    const wan = abs / 10000
    const decimals = wan >= 1000 ? 0 : wan >= 100 ? 1 : Math.min(fractionDigits, 2)
    return [{ digits: trimFractionZeros(wan.toFixed(decimals)), unit: '万' }]
  }
  if (abs < 1000000000000) {
    const yi = abs / 100000000
    const decimals = yi >= 1000 ? 0 : yi >= 100 ? 1 : Math.min(fractionDigits, 2)
    return [{ digits: trimFractionZeros(yi.toFixed(decimals)), unit: '亿' }]
  }
  const wanYi = abs / 1000000000000
  return [{ digits: trimFractionZeros(wanYi.toFixed(Math.min(fractionDigits, 2))), unit: '万亿' }]
}

export function buildCnyAmountParts(
  value: number | null | undefined,
  options: FormatCnyOptions = {},
): CnyAmountPart[] | null {
  if (value == null || Number.isNaN(value)) return null

  const { symbol = true, fractionDigits = 2, compact = false } = options
  const negative = value < 0
  const abs = Math.abs(value)

  const parts: CnyAmountPart[] = []
  if (symbol) {
    parts.push({ kind: 'symbol', text: negative ? '-¥' : '¥' })
  } else if (negative) {
    parts.push({ kind: 'symbol', text: '-' })
  }

  if (compact) {
    const segments = formatCompactSegments(abs, fractionDigits)
    for (const segment of segments) {
      parts.push({ kind: 'digits', text: segment.digits })
      if (segment.unit) {
        parts.push({ kind: 'unit', text: segment.unit })
      }
    }
    return parts
  }

  const fixed = abs.toFixed(fractionDigits)
  const [intStr, decStr] = fixed.split('.')
  const intNum = Number.parseInt(intStr, 10)
  const fractionText = decStr != null ? `.${decStr}` : ''

  const segments = formatIntegerGroups(intNum, fractionText)
  for (const segment of segments) {
    parts.push({ kind: 'digits', text: segment.digits })
    if (segment.unit) {
      parts.push({ kind: 'unit', text: segment.unit })
    }
  }
  return parts
}

export function formatCnyPlain(value: number | null | undefined, options?: FormatCnyOptions): string {
  const parts = buildCnyAmountParts(value, options)
  if (!parts) return '—'
  return parts.map((part) => part.text).join('')
}

export function formatSignedCnyPlain(value: number | null | undefined, options?: FormatCnyOptions): string {
  if (value == null || Number.isNaN(value)) return '—'
  const num = Number(value)
  const plain = formatCnyPlain(num, options)
  if (plain === '—') return plain
  return num > 0 ? `+${plain}` : plain
}

export const formatMoney = formatCnyPlain
export const formatPrice = formatCnyPlain
