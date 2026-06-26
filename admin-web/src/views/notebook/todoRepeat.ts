import type { NbTodoRepeatType } from '@/api/notebook/todo'

export const WEEKDAY_VALUES = [1, 2, 3, 4, 5, 6, 7] as const

export const MONTH_DAY_VALUES = Array.from({ length: 31 }, (_, index) => index + 1)

export function parseRepeatWeekdays(value?: string | null): number[] {
  if (!value?.trim()) return []
  return value
    .split(',')
    .map((part) => Number.parseInt(part.trim(), 10))
    .filter((day) => day >= 1 && day <= 7)
}

export function parseRepeatMonthDays(value?: string | null): number[] {
  if (!value?.trim()) return []
  return value
    .split(',')
    .map((part) => Number.parseInt(part.trim(), 10))
    .filter((day) => day >= 1 && day <= 31)
}

export function parseRepeatYearDays(value?: string | null): string[] {
  if (!value?.trim()) return []
  return value
    .split(',')
    .map((part) => part.trim())
    .filter((part) => /^\d{2}-\d{2}$/.test(part))
}

export function encodeRepeatDays(
  type: NbTodoRepeatType,
  weekdays: number[],
  monthDays: number[],
  yearDays: string[],
): string | undefined {
  if (type === 'WEEKLY') {
    const normalized = [...new Set(weekdays.filter((day) => day >= 1 && day <= 7))].sort((a, b) => a - b)
    return normalized.length ? normalized.join(',') : undefined
  }
  if (type === 'MONTHLY') {
    const normalized = [...new Set(monthDays.filter((day) => day >= 1 && day <= 31))].sort((a, b) => a - b)
    return normalized.length ? normalized.join(',') : undefined
  }
  if (type === 'YEARLY') {
    const normalized = [...new Set(yearDays.filter((day) => /^\d{2}-\d{2}$/.test(day)))].sort()
    return normalized.length ? normalized.join(',') : undefined
  }
  return undefined
}

export function decodeRepeatDays(
  type: NbTodoRepeatType,
  value?: string | null,
): { weekdays: number[]; monthDays: number[]; yearDays: string[] } {
  return {
    weekdays: type === 'WEEKLY' ? parseRepeatWeekdays(value) : [],
    monthDays: type === 'MONTHLY' ? parseRepeatMonthDays(value) : [],
    yearDays: type === 'YEARLY' ? parseRepeatYearDays(value) : [],
  }
}

export function extractTimePart(value?: string | null): string | null {
  if (!value?.trim()) return null
  const normalized = value.includes('T') ? value.replace('T', ' ') : value
  const time = normalized.trim().split(' ')[1]
  if (!time) return null
  return time.length === 5 ? `${time}:00` : time.slice(0, 8)
}

export function extractDatePart(value?: string | null): string | null {
  if (!value?.trim()) return null
  const normalized = value.includes('T') ? value.replace('T', ' ') : value
  return normalized.trim().split(' ')[0] ?? null
}

export function mergeDateAndTime(datePart: string, timePart?: string | null): string | null {
  if (!timePart?.trim()) return null
  const time = timePart.length === 5 ? `${timePart}:00` : timePart
  return `${datePart} ${time}`
}

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function formatDateYmd(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function addDays(date: Date, days: number) {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

function isoWeekday(date: Date) {
  const day = date.getDay()
  return day === 0 ? 7 : day
}

function monthStart(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

function monthsBetween(start: Date, end: Date) {
  return (end.getFullYear() - start.getFullYear()) * 12 + (end.getMonth() - start.getMonth())
}

function weekMonday(date: Date) {
  const next = new Date(date)
  const weekday = isoWeekday(next)
  next.setDate(next.getDate() - (weekday - 1))
  next.setHours(0, 0, 0, 0)
  return next
}

function weeksBetween(startMonday: Date, endMonday: Date) {
  return Math.round((endMonday.getTime() - startMonday.getTime()) / (7 * 24 * 60 * 60 * 1000))
}

function dayExistsInMonth(year: number, monthIndex: number, day: number) {
  return day <= new Date(year, monthIndex + 1, 0).getDate()
}

export function computeInitialDueDate(
  type: NbTodoRepeatType,
  repeatDays: string | undefined,
  interval: number,
  from = new Date(),
): string {
  const base = new Date(from)
  base.setHours(0, 0, 0, 0)
  const step = Math.max(1, interval)

  if (type === 'DAILY') {
    return formatDateYmd(base)
  }

  if (type === 'WEEKLY') {
    const weekdays = parseRepeatWeekdays(repeatDays)
    if (!weekdays.length) {
      return formatDateYmd(addDays(base, step * 7))
    }
    const anchorWeek = weekMonday(base)
    for (let offset = 0; offset < 370; offset += 1) {
      const candidate = addDays(base, offset)
      const weekday = isoWeekday(candidate)
      if (!weekdays.includes(weekday)) continue
      const weeks = weeksBetween(anchorWeek, weekMonday(candidate))
      if (weeks % step === 0) {
        return formatDateYmd(candidate)
      }
    }
    return formatDateYmd(base)
  }

  if (type === 'MONTHLY') {
    const monthDays = parseRepeatMonthDays(repeatDays)
    if (!monthDays.length) {
      const next = new Date(base)
      next.setMonth(next.getMonth() + step)
      return formatDateYmd(next)
    }
    const anchorMonth = monthStart(base)
    for (let offset = 0; offset < 370; offset += 1) {
      const candidate = addDays(base, offset)
      const dom = candidate.getDate()
      if (!monthDays.includes(dom)) continue
      const months = monthsBetween(anchorMonth, monthStart(candidate))
      if (months % step === 0) {
        return formatDateYmd(candidate)
      }
    }
    return formatDateYmd(base)
  }

  if (type === 'YEARLY') {
    const yearDays = parseRepeatYearDays(repeatDays)
    if (!yearDays.length) {
      const next = new Date(base)
      next.setFullYear(next.getFullYear() + step)
      return formatDateYmd(next)
    }
    const year = base.getFullYear()
    const candidates: Date[] = []
    for (let y = year; y <= year + step; y += 1) {
      for (const token of yearDays) {
        const [monthText, dayText] = token.split('-')
        const monthIndex = Number.parseInt(monthText, 10) - 1
        const day = Number.parseInt(dayText, 10)
        if (!dayExistsInMonth(y, monthIndex, day)) continue
        candidates.push(new Date(y, monthIndex, day))
      }
    }
    candidates.sort((a, b) => a.getTime() - b.getTime())
    const hit = candidates.find((candidate) => candidate.getTime() >= base.getTime())
    if (hit) return formatDateYmd(hit)
    const [monthText, dayText] = yearDays[0].split('-')
    return formatDateYmd(new Date(year + step, Number.parseInt(monthText, 10) - 1, Number.parseInt(dayText, 10)))
  }

  return formatDateYmd(base)
}

export function formatRepeatDaysLabel(
  type: NbTodoRepeatType,
  repeatDays: string | undefined,
  t: (key: string, params?: Record<string, unknown>) => string,
): string {
  if (!repeatDays?.trim()) return ''
  if (type === 'WEEKLY') {
    const labels = parseRepeatWeekdays(repeatDays).map((day) =>
      t(`notebook.todos.weekdays.${['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'][day - 1]}`),
    )
    return labels.join('、')
  }
  if (type === 'MONTHLY') {
    return parseRepeatMonthDays(repeatDays)
      .map((day) => t('notebook.todos.repeatMonthDayLabel', { day }))
      .join('、')
  }
  if (type === 'YEARLY') {
    return parseRepeatYearDays(repeatDays)
      .map((token) => {
        const [monthText, dayText] = token.split('-')
        return t('notebook.todos.repeatYearDayLabel', {
          month: Number.parseInt(monthText, 10),
          day: Number.parseInt(dayText, 10),
        })
      })
      .join('、')
  }
  return ''
}
