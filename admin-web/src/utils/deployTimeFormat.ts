export interface DeployTimeDisplay {
  relative: string
  absolute: string
}

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

function formatAbsolute(date: Date, locale: string): string {
  const y = date.getFullYear()
  const m = pad(date.getMonth() + 1)
  const d = pad(date.getDate())
  const h = pad(date.getHours())
  const min = pad(date.getMinutes())
  if (locale.startsWith('zh')) {
    return `${y}-${m}-${d} ${h}:${min}`
  }
  return `${y}-${m}-${d} ${h}:${min}`
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

export function formatDeployTime(iso: string, locale: string): DeployTimeDisplay {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) {
    return {
      relative: '—',
      absolute: locale.startsWith('zh') ? '时间无效' : 'Invalid time',
    }
  }
  const diffMinutes = Math.max(0, Math.floor((Date.now() - date.getTime()) / 60_000))
  return {
    relative: formatRelativeMinutes(diffMinutes, locale),
    absolute: formatAbsolute(date, locale),
  }
}
