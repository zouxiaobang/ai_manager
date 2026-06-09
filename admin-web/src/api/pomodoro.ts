import { deleteData, getData, postData, putData } from './request'
import type { PageQuery, PageResult } from './pagination'



export interface PomodoroPlan {

  id: number

  title: string

  workDurationMin: number

  shortBreakMin: number

  longBreakMin: number

  roundsBeforeLongBreak: number

  dailyGoalRounds: number

  dailyGoalMinutes: number

  isDefault: number

  status: string

  createTime?: string

  updateTime?: string

}



export interface PomodoroPlanSaveRequest {

  title: string

  workDurationMin: number

  shortBreakMin: number

  longBreakMin: number

  roundsBeforeLongBreak: number

  dailyGoalRounds: number

  dailyGoalMinutes: number

  asDefault?: boolean

  status?: string

}



export interface PomodoroRecord {

  id: number

  planId?: number

  recordType: 'WORK' | 'SHORT_BREAK' | 'LONG_BREAK'

  durationSec: number

  roundIndex: number

  statDate: string

  source: string

  remark?: string

  createTime?: string

}



export interface PomodoroRecordCreateRequest {

  planId?: number

  recordType: string

  durationSec: number

  roundIndex?: number

  source?: 'ADMIN' | 'DEVICE'

  remark?: string

}



export interface PomodoroDailyStat {

  statDate: string

  workRounds: number

  workMinutes: number

  breakMinutes: number

  totalMinutes: number

}



export interface PomodoroSummary {

  totalWorkRounds: number

  totalWorkMinutes: number

  totalBreakMinutes: number

  activeDays: number

  avgWorkMinutesPerDay: number

}



export interface PomodoroTodayStat {

  workRounds: number

  workMinutes: number

  breakMinutes: number

}



export interface PomodoroActiveSession {

  phase: 'IDLE' | 'WORK' | 'SHORT_BREAK' | 'LONG_BREAK'

  runState: 'IDLE' | 'RUNNING' | 'PAUSED'

  remainingSec: number

  phaseTotalSec: number

  sessionWorkRounds: number

  planId?: number

  source: string

  controller: 'ADMIN' | 'DEVICE'

  pendingPhase?: 'WORK' | 'SHORT_BREAK' | 'LONG_BREAK' | null

  syncedAtMs: number

}



export interface PomodoroSessionSyncRequest {

  phase: PomodoroActiveSession['phase']

  runState: PomodoroActiveSession['runState']

  remainingSec: number

  phaseTotalSec: number

  sessionWorkRounds: number

  planId?: number

  source: 'ADMIN' | 'DEVICE'

  takeControl?: boolean

  pendingPhase?: PomodoroActiveSession['pendingPhase']

}



export function fetchPlans(pageQuery?: PageQuery) {
  return getData<PageResult<PomodoroPlan>>('/api/pomodoro/plans', pageQuery ?? {})
}

export function fetchEnabledPlans() {
  return getData<PomodoroPlan[]>('/api/pomodoro/plans/enabled')
}



export function fetchDefaultPlan() {

  return getData<PomodoroPlan>('/api/pomodoro/plans/default')

}



export function createPlan(body: PomodoroPlanSaveRequest) {

  return postData<PomodoroPlan>('/api/pomodoro/plans', body)

}



export function updatePlan(id: number, body: PomodoroPlanSaveRequest) {

  return putData<PomodoroPlan>(`/api/pomodoro/plans/${id}`, body)

}



export function removePlan(id: number) {

  return deleteData(`/api/pomodoro/plans/${id}`)

}



export function createRecord(body: PomodoroRecordCreateRequest) {

  return postData<PomodoroRecord>('/api/pomodoro/records', body)

}



export function fetchDailyStats(startDate: string, endDate: string) {

  return getData<PomodoroDailyStat[]>('/api/pomodoro/stats/daily', { startDate, endDate })

}



export function fetchSummary(startDate: string, endDate: string) {

  return getData<PomodoroSummary>('/api/pomodoro/stats/summary', { startDate, endDate })

}



export function fetchTodayStat() {

  return getData<PomodoroTodayStat>('/api/pomodoro/stats/today')

}



export function fetchActiveSession() {

  return getData<PomodoroActiveSession | null>('/api/pomodoro/session')

}



/** 浏览器 / ESP 上报当前会话，供另一端拉取同步 */

export function syncActiveSession(body: PomodoroSessionSyncRequest) {

  return putData<PomodoroActiveSession>('/api/pomodoro/session', body)

}


