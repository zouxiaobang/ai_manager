import type { NbNoteTag } from '@/api/notebook'

/**
 * 笔记本展示类纯函数
 *
 * <p>从 {@code NotebookView.vue} 提取：笔记更新时间相对文案、标签色块样式。
 * 时间相对文案需要 i18n 翻译函数由调用方注入，保持函数无状态可测。</p>
 */

/** i18n 翻译函数签名（与 useI18n 的 t 兼容的子集） */
export type NoteI18nT = (key: string, params?: Record<string, unknown>) => string

/** 无自定义颜色的标签按索引轮换的主题色 */
export const TAG_PILL_THEMES = [
  { bg: '#eff6ff', color: '#2563eb' },
  { bg: '#f5f3ff', color: '#7c3aed' },
  { bg: '#f0fdf4', color: '#16a34a' },
  { bg: '#fff7ed', color: '#ea580c' },
] as const

/** 当天零点（用于计算相对天数） */
export function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

/**
 * 笔记更新时间 → 相对文案（今天/昨天/前天带时刻，更早显示完整日期时间）
 * 传入非法时间时原样返回并裁掉秒部分，避免展示 NaN
 */
export function formatNoteDisplayTime(updateTime: string | undefined, t: NoteI18nT): string {
  if (!updateTime) return ''
  const normalized = updateTime.includes('T') ? updateTime : updateTime.replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) {
    return updateTime.replace(/:\d{2}$/, '')
  }

  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  const timePart = `${hh}:${mm}`

  const diffDays = Math.round(
    (startOfDay(new Date()).getTime() - startOfDay(date).getTime()) / 86_400_000,
  )

  if (diffDays === 0) return t('notebook.todayAt', { time: timePart })
  if (diffDays === 1) return t('notebook.yesterdayAt', { time: timePart })
  if (diffDays === 2) return t('notebook.dayBeforeYesterdayAt', { time: timePart })

  const yyyy = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${yyyy}-${month}-${day} ${timePart}`
}

/** 标签色块样式：自定义色为浅底同色字，否则按索引轮换主题色 */
export function getTagPillStyle(tag: NbNoteTag, index = 0): Record<string, string> {
  if (tag.color) {
    return {
      backgroundColor: `${tag.color}1a`,
      color: tag.color,
    }
  }
  const theme = TAG_PILL_THEMES[index % TAG_PILL_THEMES.length]
  return {
    backgroundColor: theme.bg,
    color: theme.color,
  }
}
