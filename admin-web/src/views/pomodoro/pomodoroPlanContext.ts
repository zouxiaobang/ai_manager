import type { InjectionKey } from 'vue'

export interface PomodoroPlanContext {
  checkEditable: () => Promise<boolean>
  notifyPlansChanged: (planId?: number) => Promise<void>
}

export const POMODORO_PLAN_CONTEXT_KEY: InjectionKey<PomodoroPlanContext> =
  Symbol('pomodoroPlanContext')
