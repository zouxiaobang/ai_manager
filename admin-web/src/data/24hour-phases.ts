export interface ItemDef {
  key: string
  label: string
  hasContent?: boolean
}

export interface PhaseDef {
  key: string
  badge: string
  title: string
  desc?: string
  accent: string
  items: ItemDef[]
  hourStart: number
  hourEnd: number
}

export const phases: PhaseDef[] = [
  {
    key: 'MORNING',
    badge: '晨起',
    title: '晨起黄金60分钟',
    accent: '#f59e0b',
    hourStart: 6,
    hourEnd: 8,
    desc: '起身后五分钟启动并隔离手机诱惑',
    items: [
      { key: 'morning_rest', label: '卧床静息五分钟' },
      { key: 'morning_light', label: '见自然光 + 喝温水' },
      { key: 'morning_stretch', label: '低强度拉伸' },
      { key: 'morning_read', label: '看书半个钟', hasContent: true },
      { key: 'morning_mit', label: '定一个核心任务', hasContent: true },
    ],
  },
  {
    key: 'MORNING_FOCUS',
    badge: '上午',
    title: '深度专注90-120分钟',
    accent: '#3b82f6',
    hourStart: 8,
    hourEnd: 12,
    desc: '先完成再追求完美，卡点降级不放弃',
    items: [
      { key: 'focus_pomodoro', label: '番茄钟专注法' },
      { key: 'focus_summary', label: '休息时间总结', hasContent: true },
      { key: 'focus_tomorrow_reminder', label: '下午继续工作的提醒', hasContent: true },
    ],
  },
  {
    key: 'MIDDAY_RESET',
    badge: '中段',
    title: '中段30-60分钟重置',
    accent: '#10b981',
    hourStart: 12,
    hourEnd: 14,
    items: [
      { key: 'midday_lunch', label: '午饭并休息' },
      { key: 'midday_review', label: '三行复位复盘', hasContent: true },
      { key: 'midday_desk', label: '桌面整理进入状态' },
    ],
  },
  {
    key: 'AFTERNOON',
    badge: '下午',
    title: '下午二次推进60-90分钟',
    accent: '#8b5cf6',
    hourStart: 14,
    hourEnd: 18,
    desc: '不随意切换方向',
    items: [
      { key: 'afternoon_deep_done', label: '需有可交付的完结内容', hasContent: true },
      { key: 'afternoon_tomorrow_reminder', label: '明日继续工作的提醒', hasContent: true },
    ],
  },
  {
    key: 'EVENING_REVIEW',
    badge: '晚间',
    title: '晚间15分钟复盘',
    accent: '#ef4444',
    hourStart: 18,
    hourEnd: 20,
    desc: '不做自责大会，保留好习惯、删除坏行为',
    items: [
      { key: 'review_content_effective', label: '总结工作内容', hasContent: true },
      { key: 'review_content_obstacle', label: '可改进的地方', hasContent: true },
      { key: 'review_keep', label: '保留好习惯删除坏行为' },
    ],
  },
  {
    key: 'EVENING_PREP',
    badge: '前夜',
    title: '前夜前置铺垫',
    accent: '#6366f1',
    hourStart: 20,
    hourEnd: 24,
    items: [
      { key: 'prep_task', label: '写明日交付任务', hasContent: true },
      { key: 'prep_env', label: '整理办公环境' },
      { key: 'prep_hint', label: '写明日行为暗示', hasContent: true },
    ],
  },
]

export function getCurrentPhase(): PhaseDef | null {
  const hour = new Date().getHours()
  for (const p of phases) {
    if (hour >= p.hourStart && hour < p.hourEnd) {
      return p
    }
  }
  return null
}

export function getPhaseByKey(key: string): PhaseDef | undefined {
  return phases.find(p => p.key === key)
}
