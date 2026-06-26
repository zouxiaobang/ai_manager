export interface DeployTimeDisplay {
  relative: string
  absolute: string
}

const SERVER_TIME_ZONE = 'Asia/Shanghai'

/** 解析后端返回的部署/健康检查时间（支持 ISO 与 yyyy-MM-dd HH:mm:ss） */
export function parseServerTime(value: string): Date {
  if (!value) {
    return new Date(Number.NaN)
  }
  const trimmed = value.trim()
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}/.test(trimmed)) {
    return new Date(trimmed.replace(' ', 'T') + '+08:00')
  }
  if (/^\d{4}\/\d{2}\/\d{2} \d{2}:\d{2}/.test(trimmed)) {
    const normalized = trimmed.replace(/\//g, '-').replace(' ', 'T') + '+08:00'
    return new Date(normalized)
  }
  return new Date(trimmed)
}

function formatRelativeMinutes(diffMinutes: number, locale: string): string {
  if (diffMinutes < 1) {
    return locale.startsWith('zh') ? '刚刚' : 'Just now'
  }
  if (diffMinutes < 60) {
    return locale.startsWith('zh') ? `${diffMinutes} 分钟前` : `${diffMinutes} min ago`
  }
  const hours = Math.floor(diffMinutes / 60)
  if (hours < 24) {
    return locale.startsWith('zh') ? `${hours} 小时前` : `${hours} h ago`
  }
  const days = Math.floor(hours / 24)
  if (days < 30) {
    return locale.startsWith('zh') ? `${days} 天前` : `${days} d ago`
  }
  const months = Math.floor(days / 30)
  return locale.startsWith('zh') ? `${months} 个月前` : `${months} mo ago`
}

/** 格式化为东八区时间，如 2026/06/26 13:53 */
export function formatServerDateTime(date: Date, locale: string): string {
  if (Number.isNaN(date.getTime())) {
    return locale.startsWith('zh') ? '时间无效' : 'Invalid time'
  }
  const parts = new Intl.DateTimeFormat(locale.startsWith('zh') ? 'zh-CN' : 'en-GB', {
    timeZone: SERVER_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).formatToParts(date)

  const pick = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((part) => part.type === type)?.value ?? ''

  const year = pick('year')
  const month = pick('month')
  const day = pick('day')
  const hour = pick('hour')
  const minute = pick('minute')
  return `${year}/${month}/${day} ${hour}:${minute}`
}

export function formatDeployTime(iso: string, locale: string): DeployTimeDisplay {
  const date = parseServerTime(iso)
  if (Number.isNaN(date.getTime())) {
    return {
      relative: '—',
      absolute: locale.startsWith('zh') ? '时间无效' : 'Invalid time',
    }
  }
  const diffMinutes = Math.max(0, Math.floor((Date.now() - date.getTime()) / 60_000))
  return {
    relative: formatRelativeMinutes(diffMinutes, locale),
    absolute: formatServerDateTime(date, locale),
  }
}
