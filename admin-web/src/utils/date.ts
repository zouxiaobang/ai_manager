export const EMPTY_DATE = '—'



export function formatDateParam(date: Date): string {

  const y = date.getFullYear()

  const m = String(date.getMonth() + 1).padStart(2, '0')

  const d = String(date.getDate()).padStart(2, '0')

  return `${y}-${m}-${d}`

}



export function addDays(date: Date, days: number): Date {

  const next = new Date(date)

  next.setDate(next.getDate() + days)

  return next

}



/** 业务日期展示：yyyy-MM-dd */

export function formatDate(value?: string | null | Date): string {

  if (value == null || value === '') return EMPTY_DATE

  if (value instanceof Date) return formatDateParam(value)

  const normalized = value.trim().replace('T', ' ')

  const datePart = normalized.slice(0, 10)

  if (!/^\d{4}-\d{2}-\d{2}$/.test(datePart)) return value

  return datePart

}



/** 日志等精确时间展示：yyyy-MM-dd HH:mm:ss */

export function formatDateTime(value?: string | null | Date): string {

  if (value == null || value === '') return EMPTY_DATE

  if (value instanceof Date) {

    const y = value.getFullYear()

    const m = String(value.getMonth() + 1).padStart(2, '0')

    const d = String(value.getDate()).padStart(2, '0')

    const h = String(value.getHours()).padStart(2, '0')

    const min = String(value.getMinutes()).padStart(2, '0')

    const s = String(value.getSeconds()).padStart(2, '0')

    return `${y}-${m}-${d} ${h}:${min}:${s}`

  }

  const normalized = value.trim().replace('T', ' ').replace(/\.\d+/, '')

  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(normalized.slice(0, 19))) {
    return normalized.slice(0, 19)
  }

  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/.test(normalized)) {
    return `${normalized}:00`
  }

  if (normalized.length >= 19) return normalized.slice(0, 19)

  if (/^\d{4}-\d{2}-\d{2}$/.test(normalized.slice(0, 10))) {

    return `${normalized.slice(0, 10)} 00:00:00`

  }

  return normalized

}



export function todayDateString(): string {

  return formatDateParam(new Date())

}



/** 仅日期提交 API 时转为 LocalDateTime 字符串 */

export function toApiDateTime(date: string): string {

  if (!date) return date

  if (date.includes('T')) return date

  return `${date}T00:00:00`

}

