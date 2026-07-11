import { getData, postData } from './request'

export interface DailyChecklistItem {
  itemKey: string
  completed: number
  content?: string | null
}

export interface DailyChecklistSaveRequest {
  date: string
  items: {
    itemKey: string
    completed: number
    content?: string | null
  }[]
}

// ─── 统计 ──────────────────────────
export interface PhaseStatsItem {
  phaseKey: string
  totalCount: number
  completedCount: number
  completionRate: number
}

export interface DailyStatsItem {
  date: string
  totalCount: number
  completedCount: number
  completionRate: number
  phaseStats: PhaseStatsItem[]
}

export interface DailyStatsSummary {
  totalDays: number
  overallRate: number
  currentStreak: number
  bestStreak: number
  perfectDays: number
}

export interface DailyChecklistStats {
  items: DailyStatsItem[]
  summary: DailyStatsSummary
}

export function fetchDailyChecklist(date: string) {
  return getData<DailyChecklistItem[]>('/api/24hour', { date })
}

export function saveDailyChecklist(body: DailyChecklistSaveRequest) {
  return postData<void>('/api/24hour', body)
}

export function fetchDailyChecklistStats(startDate: string, endDate: string) {
  return getData<DailyChecklistStats>('/api/24hour/stats', { startDate, endDate })
}
