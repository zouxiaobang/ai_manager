import type { PomodoroActiveSession } from '@/api/pomodoro'

/** 会话是否处于专注/休息倒计时中（须先暂停才能改计划） */
export function isPlanMutationBlocked(
  session: PomodoroActiveSession | null | undefined,
): boolean {
  if (!session) return false
  const runState = (session.runState ?? '').trim().toUpperCase()
  const phase = (session.phase ?? '').trim().toUpperCase()
  if (runState !== 'RUNNING') return false
  return phase === 'WORK' || phase === 'SHORT_BREAK' || phase === 'LONG_BREAK'
}
