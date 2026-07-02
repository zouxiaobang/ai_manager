<template>
  <div class="timer-panel timer-panel--pixel">
    <div class="timer-panel__sync-stage">
      <PomoSyncCard
        :device-time="devicePreviewTime"
        :health-text="syncHealthText"
        :online="deviceOnline"
        :controller="controlOwner"
      />
    </div>

    <div class="timer-main">
      <aside class="timer-side timer-side--left">
        <div class="pixel-panel-jagged">
          <div class="pixel-panel-jagged__inner timer-side--left-inner">
        <h3 class="pixel-panel__subtitle">{{ t('pomodoro.timer.todayProgress') }}</h3>
        <p class="timer-side__caption">{{ t('pomodoro.timer.completedRounds') }}</p>
        <div class="daily-dots">
          <div v-for="(row, ri) in dailyDotRows" :key="ri" class="daily-dots__row">
            <span
              v-for="(filled, di) in row"
              :key="`${ri}-${di}`"
              class="daily-dot"
              :class="{ 'is-filled': filled }"
            />
          </div>
        </div>
        <p class="timer-side__today">
          <span>{{ t('pomodoro.timer.todayRoundPrefix') }}</span>
          <span class="timer-side__big-num">{{ today.workRounds }}</span>
          <span>{{ t('pomodoro.timer.todayRoundSuffix') }}</span>
        </p>
          </div>
        </div>
      </aside>

      <section class="timer-center">
        <div class="timer-center__ring-stage">
          <PomoPixelRing
            :clock="displayClockText"
            :status="phaseStatusText"
            :percentage="ringProgress"
            :color="phaseColor"
          />
        </div>
        <div class="phase-pills">
          <button type="button" class="phase-pill phase-pill--work" :class="{ 'is-active': pillActive.work }" disabled>
            <img src="/icons/modules/pomodoro.svg" alt="" width="20" height="20">
            {{ phasePillText.work }}
          </button>
          <button type="button" class="phase-pill phase-pill--short" :class="{ 'is-active': pillActive.shortBreak }" disabled>
            <span aria-hidden="true">🍃</span>
            {{ phasePillText.short }}
          </button>
          <button type="button" class="phase-pill phase-pill--long" :class="{ 'is-active': pillActive.longBreak }" disabled>
            <span aria-hidden="true">☕</span>
            {{ phasePillText.long }}
          </button>
        </div>
        <div v-if="showSecondaryActions" class="timer-center__secondary">
          <button type="button" class="pixel-link" :title="t('pomodoro.timer.skipHint')" @click="skipPhase">{{ t('pomodoro.timer.skip') }}</button>
          <button type="button" class="pixel-link" :title="t('pomodoro.timer.resetHint')" @click="resetTimer">{{ t('pomodoro.timer.reset') }}</button>
          <button
            v-if="serverReachable && controlOwner === 'DEVICE'"
            type="button"
            class="pixel-link pixel-link--warn"
            @click="takeControl"
          >
            {{ t('pomodoro.timer.takeControl') }}
          </button>
        </div>
      </section>

      <aside class="timer-side timer-side--right">
        <div class="plan-card pixel-panel-jagged pixel-panel-jagged--plan">
          <div class="pixel-panel-jagged__inner">
          <h3 class="pixel-panel__subtitle">
            <span class="plan-card__clip" aria-hidden="true">📋</span>
            {{ t('pomodoro.timer.currentPlan') }}
          </h3>
          <p class="plan-card__name">{{ activePlan?.title ?? '—' }}</p>
          <p v-if="activePlan" class="plan-card__durations">
            <span class="plan-card__dur plan-card__dur--work">{{ activePlan.workDurationMin }}</span>
            <span class="plan-card__dur-sep" aria-hidden="true">+</span>
            <span class="plan-card__dur plan-card__dur--short">{{ activePlan.shortBreakMin }}</span>
            <span class="plan-card__dur-sep" aria-hidden="true">+</span>
            <span class="plan-card__dur plan-card__dur--long">{{ activePlan.longBreakMin }}</span>
          </p>
          <p v-else class="plan-card__durations">—</p>
          </div>
        </div>
        <button
          type="button"
          class="pixel-btn-start"
          :class="{ 'is-goal-blocked': primaryStartWorkBlocked }"
          @click="onPrimaryClick"
        >
          <span class="pixel-btn-start__icon" aria-hidden="true">▶</span>
          {{ primaryButtonLabel }}
        </button>
        <el-alert
          v-if="todayRoundsGoalReached && !todayPlanComplete"
          type="info"
          :closable="false"
          show-icon
          class="plan-card__done"
        >
          {{ t('pomodoro.timer.todayRoundsDone') }}
        </el-alert>
        <el-alert
          v-if="todayPlanComplete"
          type="success"
          :closable="false"
          show-icon
          class="plan-card__done"
        >
          {{ t('pomodoro.timer.todayPlanDone') }}
        </el-alert>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import PomoPixelRing from './PomoPixelRing.vue'
import PomoSyncCard from './PomoSyncCard.vue'
import {
  createRecord,
  fetchActiveSession,
  fetchDefaultPlan,
  fetchEnabledPlans,
  fetchTodayStat,
  syncActiveSession,
  type PomodoroActiveSession,
  type PomodoroPlan,
  type PomodoroSessionSyncRequest,
} from '@/api/pomodoro'
import { isPlanMutationBlocked } from '@/utils/pomodoroSession'

type Phase = 'idle' | 'work' | 'shortBreak' | 'longBreak'

const { t } = useI18n()

const plans = ref<PomodoroPlan[]>([])
const selectedPlanId = ref<number | null>(null)
const phase = ref<Phase>('idle')
const remainingSec = ref(0)
const plannedSec = ref(0)
const paused = ref(false)
const sessionWorkRounds = ref(0)
/** 阶段结束后待用户确认的下一阶段（不自动开始） */
const pendingPhase = ref<Phase | null>(null)
/** 当前控制方：副屏 DEVICE / 本页 ADMIN */
const controlOwner = ref<'ADMIN' | 'DEVICE' | null>(null)
const serverReachable = ref(false)
const lastRemoteSession = ref<PomodoroActiveSession | null>(null)
const lastAppliedSyncedMs = ref(0)
const lastSeenWorkRounds = ref(0)
const applyingRemote = ref(false)
/** 本地 interval 是否在跑（须为 ref，供 phaseReadyToStart 等 computed 追踪） */
const ticking = ref(false)

let tickTimer: ReturnType<typeof setInterval> | null = null
let remoteSyncTimer: ReturnType<typeof setInterval> | null = null
let lastPublishedFingerprint = ''

const REMOTE_SYNC_INTERVAL_MS = 1000
const DEVICE_ONLINE_TTL_MS = 15000

const today = ref({ workRounds: 0, workMinutes: 0, breakMinutes: 0 })
let todayPlanKnownComplete = false
let skipNextPlanDoneNotify = true

function isTodayPlanComplete(): boolean {
  const plan = activePlan.value
  if (!plan || plan.dailyGoalRounds <= 0 || plan.dailyGoalMinutes <= 0) {
    return false
  }
  return (
    today.value.workRounds >= plan.dailyGoalRounds &&
    today.value.workMinutes >= plan.dailyGoalMinutes
  )
}

const todayPlanComplete = computed(() => isTodayPlanComplete())

function isTodayRoundsGoalReached(): boolean {
  const plan = activePlan.value
  if (!plan || plan.dailyGoalRounds <= 0) {
    return false
  }
  return today.value.workRounds >= plan.dailyGoalRounds
}

const todayRoundsGoalReached = computed(() => isTodayRoundsGoalReached())

function notifyTodayRoundsGoalReached() {
  const plan = activePlan.value
  if (!plan || plan.dailyGoalRounds <= 0) return
  ElMessage.warning(
    t('pomodoro.timer.todayRoundsGoalReached', {
      goal: plan.dailyGoalRounds,
      rounds: today.value.workRounds,
    }),
  )
}

function todayPlanNotifyKey(): string {
  return `pomodoro-plan-done-${new Date().toISOString().slice(0, 10)}`
}

function notifyTodayPlanDone() {
  if (sessionStorage.getItem(todayPlanNotifyKey())) {
    return
  }
  sessionStorage.setItem(todayPlanNotifyKey(), '1')
  ElMessage.success(t('pomodoro.timer.todayPlanDone'))
}

function maybeNotifyTodayPlanDone() {
  const nowComplete = isTodayPlanComplete()
  if (nowComplete && !todayPlanKnownComplete && !skipNextPlanDoneNotify) {
    notifyTodayPlanDone()
  }
  todayPlanKnownComplete = nowComplete
}

const activePlan = computed(() =>
  plans.value.find((p) => p.id === selectedPlanId.value) ?? null,
)

const syncHealthText = computed(() => {
  if (!serverReachable.value) {
    return t('pomodoro.timer.syncOffline')
  }
  if (deviceOnline.value) {
    return t('pomodoro.timer.syncNormal')
  }
  return t('pomodoro.timer.syncWaitingDevice')
})

const deviceOnline = computed(() => {
  if (!serverReachable.value) {
    return false
  }
  const seen = lastRemoteSession.value?.deviceLastSeenMs
  if (!seen) {
    return false
  }
  return Date.now() - seen <= DEVICE_ONLINE_TTL_MS
})

function formatClockSec(totalSec: number): string {
  const sec = Math.max(0, totalSec)
  const mm = Math.floor(sec / 60)
  const ss = sec % 60
  return `${String(mm).padStart(2, '0')}:${String(ss).padStart(2, '0')}`
}

const devicePreviewTime = computed(() => {
  const session = lastRemoteSession.value
  if (controlOwner.value === 'DEVICE' && session) {
    return formatClockSec(session.remainingSec)
  }
  return displayClockText.value
})

function formatMinTime(min: number): string {
  return `${String(min).padStart(2, '0')}:00`
}

const dailyDots = computed(() => {
  const goal = activePlan.value?.dailyGoalRounds ?? 10
  const done = today.value.workRounds
  return Array.from({ length: Math.max(goal, 1) }, (_, i) => i < done)
})

const dailyDotRows = computed(() => {
  const dots = dailyDots.value
  const half = Math.ceil(dots.length / 2)
  return [dots.slice(0, half), dots.slice(half)]
})

const phasePillText = computed(() => {
  const plan = activePlan.value
  if (!plan) {
    return { work: '', short: '', long: '' }
  }
  return {
    work: t('pomodoro.timer.phaseWorkDuration', { time: formatMinTime(plan.workDurationMin) }),
    short: t('pomodoro.timer.phaseShortDuration', { time: formatMinTime(plan.shortBreakMin) }),
    long: t('pomodoro.timer.phaseLongDuration', { time: formatMinTime(plan.longBreakMin) }),
  }
})

const uiPhase = computed((): Phase => {
  if (phase.value === 'idle' && pendingPhase.value) {
    return pendingPhase.value
  }
  return phase.value
})

const ringProgress = computed(() => {
  if (phase.value === 'idle' && !pendingPhase.value) return 0
  if (phase.value === 'idle' && pendingPhase.value) return 100
  if (!plannedSec.value) return 0
  return Math.min(
    100,
    Math.round(((plannedSec.value - remainingSec.value) / plannedSec.value) * 100),
  )
})

const phaseColor = computed(() => {
  const p = uiPhase.value
  if (p === 'work') return '#ef5350'
  if (p === 'shortBreak') return '#8bc34a'
  if (p === 'longBreak') return '#7e57c2'
  return '#8bc34a'
})

const displayClockText = computed(() => {
  if (todayRoundsGoalReached.value && phase.value === 'idle' && !pendingPhase.value) {
    return '--:--'
  }
  if (phase.value === 'idle' && !pendingPhase.value && activePlan.value) {
    const m = activePlan.value.workDurationMin
    return `${String(m).padStart(2, '0')}:00`
  }
  if (phase.value === 'idle' && pendingPhase.value && activePlan.value) {
    const plan = activePlan.value
    let min = plan.workDurationMin
    if (pendingPhase.value === 'shortBreak') min = plan.shortBreakMin
    if (pendingPhase.value === 'longBreak') min = plan.longBreakMin
    return `${String(min).padStart(2, '0')}:00`
  }
  return clockText.value
})

const phaseStatusText = computed(() => {
  if (todayRoundsGoalReached.value && phase.value === 'idle' && !pendingPhase.value) {
    return t('pomodoro.timer.todayRoundsDone')
  }
  if (pendingPhase.value && phase.value === 'idle') {
    return t('pomodoro.timer.pendingNextPhase')
  }
  if (phase.value === 'idle') {
    return t('pomodoro.timer.phaseIdle')
  }
  if (phaseReadyToStart.value) {
    return t('pomodoro.timer.phaseIdle')
  }
  if (paused.value) {
    return `${phaseLabel.value} · ${t('pomodoro.timer.pausedSuffix')}`
  }
  return phaseLabel.value
})

const pillActive = computed(() => {
  const p = uiPhase.value
  return {
    work: p === 'work',
    shortBreak: p === 'shortBreak',
    longBreak: p === 'longBreak',
  }
})

const primaryButtonLabel = computed(() => {
  if (primaryStartWorkBlocked.value) {
    return t('pomodoro.timer.todayRoundsDone')
  }
  if (pendingPhase.value) {
    return pendingPhase.value === 'work'
      ? t('pomodoro.timer.startWork')
      : t('pomodoro.timer.startBreak')
  }
  if (phase.value === 'idle') {
    return t('pomodoro.timer.startWork')
  }
  if (phaseReadyToStart.value) {
    return phase.value === 'work'
      ? t('pomodoro.timer.startWork')
      : t('pomodoro.timer.startBreak')
  }
  return paused.value ? t('pomodoro.timer.resume') : t('pomodoro.timer.pause')
})

const showSecondaryActions = computed(
  () => phase.value !== 'idle' && !pendingPhase.value && !phaseReadyToStart.value,
)

const phaseLabel = computed(() => {
  if (pendingPhase.value === 'shortBreak' || pendingPhase.value === 'longBreak') {
    return t('pomodoro.timer.workDonePending')
  }
  if (pendingPhase.value === 'work') {
    return t('pomodoro.timer.breakDonePending')
  }
  const map: Record<Phase, string> = {
    idle: t('pomodoro.timer.phaseIdle'),
    work: t('pomodoro.timer.phaseWork'),
    shortBreak: t('pomodoro.timer.phaseShortBreak'),
    longBreak: t('pomodoro.timer.phaseLongBreak'),
  }
  return map[phase.value]
})

async function onPrimaryClick() {
  if (primaryStartWorkBlocked.value) {
    notifyTodayRoundsGoalReached()
    return
  }
  if (pendingPhase.value) {
    await startPendingPhase()
    return
  }
  if (phase.value === 'idle') {
    await startWork()
    return
  }
  if (phaseReadyToStart.value) {
    await startCurrentPhase()
    return
  }
  await togglePause()
}

async function takeControl() {
  controlOwner.value = 'ADMIN'
  await publishSession(true)
}

const clockText = computed(() => {
  const m = Math.floor(remainingSec.value / 60)
  const s = remainingSec.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

function isPhaseAtStart(): boolean {
  return (
    phase.value !== 'idle' &&
    !pendingPhase.value &&
    !ticking.value &&
    !paused.value &&
    plannedSec.value > 0 &&
    remainingSec.value >= plannedSec.value
  )
}

const phaseReadyToStart = computed(() => isPhaseAtStart())

/** 主按钮若点击将尝试开始专注，但今日轮次已满 */
const primaryStartWorkBlocked = computed(() => {
  if (!todayRoundsGoalReached.value) return false
  if (pendingPhase.value) return pendingPhase.value === 'work'
  if (phase.value === 'idle') return true
  if (phaseReadyToStart.value) return phase.value === 'work'
  return false
})

function clearTick() {
  ticking.value = false
  if (tickTimer) {
    clearInterval(tickTimer)
    tickTimer = null
  }
}

function clearRemoteSync() {
  if (remoteSyncTimer) {
    clearInterval(remoteSyncTimer)
    remoteSyncTimer = null
  }
}

function normalizeRunState(runState: string | undefined): PomodoroActiveSession['runState'] {
  const v = (runState ?? '').trim().toUpperCase()
  if (v === 'RUNNING' || v === 'PAUSED' || v === 'IDLE') {
    return v
  }
  return 'IDLE'
}

function normalizeServerPhase(phase: string | undefined): PomodoroActiveSession['phase'] {
  const v = (phase ?? '').trim().toUpperCase()
  if (v === 'WORK' || v === 'SHORT_BREAK' || v === 'LONG_BREAK' || v === 'IDLE') {
    return v
  }
  return 'IDLE'
}

function mapServerPhase(serverPhase: PomodoroActiveSession['phase']): Phase {
  if (serverPhase === 'WORK') return 'work'
  if (serverPhase === 'SHORT_BREAK') return 'shortBreak'
  if (serverPhase === 'LONG_BREAK') return 'longBreak'
  return 'idle'
}

function mapServerPendingPhase(
  pending: PomodoroActiveSession['pendingPhase'],
): Phase | null {
  if (!pending) return null
  return mapServerPhase(pending)
}

function sessionOwner(session: PomodoroActiveSession): 'ADMIN' | 'DEVICE' {
  const raw = session.controller ?? session.source
  return raw === 'DEVICE' ? 'DEVICE' : 'ADMIN'
}

/** 副屏已开始而本页仍空闲时，必须跟随（避免陈旧 ADMIN 会话挡住 syncedAtMs 去重） */
function shouldApplyRemoteSession(session: PomodoroActiveSession): boolean {
  const owner = sessionOwner(session)
  const runState = normalizeRunState(session.runState)
  const synced = Number(session.syncedAtMs) || 0
  const remoteActive = runState === 'RUNNING' || runState === 'PAUSED'
  const serverPhase = normalizeServerPhase(session.phase)

  // 副屏长按全量重置（IDLE/IDLE）或阶段内重置（如 SHORT_BREAK+IDLE）
  if (owner === 'DEVICE' && runState === 'IDLE' && serverPhase === 'IDLE') {
    const localNotReset =
      phase.value !== 'idle' ||
      ticking.value ||
      paused.value ||
      pendingPhase.value !== null ||
      remainingSec.value !== 0
    if (localNotReset) return true
    if (controlOwner.value !== 'DEVICE') return true
  }

  // 副屏阶段内重置为待开始（WORK+IDLE 等历史协议）
  if (owner === 'DEVICE' && runState === 'IDLE' && serverPhase !== 'IDLE') {
    if (ticking.value || paused.value) return true
    if (controlOwner.value !== 'DEVICE') return true
  }

  if (owner === 'DEVICE' && remoteActive) {
    if (phase.value === 'idle') return true
    // ADMIN 阶段起点（重置/待开始）不被副屏陈旧 RUNNING/PAUSED 覆盖
    if (controlOwner.value === 'ADMIN' && isPhaseAtStart()) return false
    // 本页曾暂停，副屏点继续后跟随 RUNNING，并切换为「已与副屏同步」
    if (controlOwner.value === 'ADMIN' && runState === 'RUNNING' && paused.value) return true
    if (controlOwner.value !== 'DEVICE' && !isPhaseAtStart()) return true
    if (paused.value !== (runState === 'PAUSED')) return true
    // 上次 apply 半途失败：副屏在跑但本地未启动 tick
    if (runState === 'RUNNING' && !paused.value && !ticking.value) return true
  }

  return synced > lastAppliedSyncedMs.value
}

function buildSessionPayload(): PomodoroSessionSyncRequest | null {
  const plan = activePlan.value
  if (!plan) return null

  if (phase.value === 'idle') {
    const total = plan.workDurationMin * 60
    return {
      phase: 'IDLE',
      runState: 'IDLE',
      remainingSec: total,
      phaseTotalSec: total,
      sessionWorkRounds: sessionWorkRounds.value,
      planId: plan.id,
      source: 'ADMIN',
    }
  }

  // 阶段结束待确认：上报 IDLE，避免副屏把 remaining=0 当成 RUNNING 反复触发阶段完成
  if (pendingPhase.value) {
    let apiPhase: PomodoroActiveSession['phase'] = 'WORK'
    if (phase.value === 'shortBreak') apiPhase = 'SHORT_BREAK'
    if (phase.value === 'longBreak') apiPhase = 'LONG_BREAK'
    let apiPending: PomodoroActiveSession['pendingPhase'] = 'SHORT_BREAK'
    if (pendingPhase.value === 'work') apiPending = 'WORK'
    if (pendingPhase.value === 'longBreak') apiPending = 'LONG_BREAK'
    return {
      phase: apiPhase,
      runState: 'IDLE',
      remainingSec: 0,
      phaseTotalSec: plannedSec.value || plan.workDurationMin * 60,
      sessionWorkRounds: sessionWorkRounds.value,
      planId: plan.id,
      source: 'ADMIN',
      pendingPhase: apiPending,
    }
  }

  let apiPhase: PomodoroActiveSession['phase'] = 'WORK'
  if (phase.value === 'shortBreak') apiPhase = 'SHORT_BREAK'
  if (phase.value === 'longBreak') apiPhase = 'LONG_BREAK'

  const atPhaseStart = isPhaseAtStart()

  return {
    phase: apiPhase,
    runState: atPhaseStart ? 'IDLE' : paused.value ? 'PAUSED' : 'RUNNING',
    remainingSec: remainingSec.value,
    phaseTotalSec: plannedSec.value || 1,
    sessionWorkRounds: sessionWorkRounds.value,
    planId: plan.id,
    source: 'ADMIN',
  }
}

function sessionFingerprint(payload: PomodoroSessionSyncRequest): string {
  return [
    payload.phase,
    payload.runState,
    payload.remainingSec,
    payload.phaseTotalSec,
    payload.sessionWorkRounds,
    payload.planId ?? '',
    payload.pendingPhase ?? '',
    payload.takeControl ? '1' : '0',
  ].join('|')
}

async function publishSession(takeControl = false) {
  if (applyingRemote.value) return
  if (!takeControl && controlOwner.value !== 'ADMIN') return

  const payload = buildSessionPayload()
  if (!payload) return
  payload.takeControl = takeControl

  if (takeControl) {
    lastPublishedFingerprint = ''
  } else {
    const fp = sessionFingerprint(payload)
    if (fp === lastPublishedFingerprint) {
      return
    }
    lastPublishedFingerprint = fp
  }

  try {
    const session = await syncActiveSession(payload)
    lastRemoteSession.value = session
    const owner = sessionOwner(session)
    const synced = Number(session.syncedAtMs) || Date.now()

    if (owner === 'DEVICE' && !takeControl) {
      if (shouldApplyRemoteSession(session)) {
        applyRemoteSession(session)
      }
      return
    }

    controlOwner.value = owner
    lastAppliedSyncedMs.value = synced
    serverReachable.value = true
    if (takeControl) {
      if (!isPhaseAtStart()) {
        ensureLocalTickRunning()
      }
    }
  } catch {
    serverReachable.value = false
    // 后端不可用时仅本地计时
  }
}

function applyRemoteSession(session: PomodoroActiveSession) {
  if (!shouldApplyRemoteSession(session)) {
    return
  }

  const owner = sessionOwner(session)
  const runState = normalizeRunState(session.runState)
  const serverPhase = normalizeServerPhase(session.phase)

  applyingRemote.value = true

  if (runState === 'IDLE') {
    clearTick()
    paused.value = false
    controlOwner.value = owner
    sessionWorkRounds.value = session.sessionWorkRounds ?? 0
    if (session.planId) {
      selectedPlanId.value = session.planId
    }

    const total = Math.max(1, Number(session.phaseTotalSec) || 1)
    const remaining = Math.max(0, Number(session.remainingSec) || 0)

    if (serverPhase === 'IDLE') {
      pendingPhase.value = null
      phase.value = 'idle'
      remainingSec.value = 0
    } else {
      phase.value = mapServerPhase(serverPhase)
      plannedSec.value = total
      remainingSec.value = remaining
      const remotePending = mapServerPendingPhase(session.pendingPhase)
      if (remotePending) {
        pendingPhase.value = remotePending
      } else if (remaining <= 0 && phase.value === 'work' && activePlan.value) {
        const plan = activePlan.value
        pendingPhase.value =
          sessionWorkRounds.value % plan.roundsBeforeLongBreak === 0
            ? 'longBreak'
            : 'shortBreak'
      } else if (remaining <= 0 && (phase.value === 'shortBreak' || phase.value === 'longBreak')) {
        pendingPhase.value = 'work'
      } else {
        pendingPhase.value = null
      }
    }

    const rounds = session.sessionWorkRounds ?? 0
    if (rounds > lastSeenWorkRounds.value) {
      lastSeenWorkRounds.value = rounds
      void refreshToday()
    }

    lastAppliedSyncedMs.value = Number(session.syncedAtMs) || Date.now()
    applyingRemote.value = false
    return
  }

  const nextPhase = mapServerPhase(serverPhase)
  if (nextPhase === 'idle') {
    applyingRemote.value = false
    return
  }

  if (session.planId) {
    selectedPlanId.value = session.planId
  }

  const total = Math.max(1, Number(session.phaseTotalSec) || 1)
  let remaining = Math.max(0, Number(session.remainingSec) || 0)
  if (runState === 'RUNNING' && remaining <= 0) {
    remaining = total
  }

  phase.value = nextPhase
  plannedSec.value = total
  remainingSec.value = remaining
  paused.value = runState === 'PAUSED'
  pendingPhase.value = null
  sessionWorkRounds.value = session.sessionWorkRounds ?? 0
  controlOwner.value = owner

  const rounds = session.sessionWorkRounds ?? 0
  if (rounds > lastSeenWorkRounds.value) {
    lastSeenWorkRounds.value = rounds
    void refreshToday()
  }

  lastAppliedSyncedMs.value = Number(session.syncedAtMs) || Date.now()

  clearTick()
  if (runState === 'RUNNING') {
    startTick(false)
  }
  applyingRemote.value = false
}

async function pullRemoteSession() {
  try {
    const session = await fetchActiveSession()
    serverReachable.value = true
    lastRemoteSession.value = session
    if (!session) {
      return
    }
    applyRemoteSession(session)
  } catch {
    serverReachable.value = false
  }
}

function startRemoteSync() {
  clearRemoteSync()
  void pullRemoteSession()
  remoteSyncTimer = setInterval(() => {
    void pullRemoteSession()
  }, REMOTE_SYNC_INTERVAL_MS)
}

function startTick(publish = true) {
  clearTick()
  ticking.value = true
  tickTimer = setInterval(() => {
    if (paused.value) return
    if (remainingSec.value <= 0) {
      clearTick()
      void onPhaseComplete()
      return
    }
    remainingSec.value -= 1
  }, 1000)
  if (publish) {
    void publishSession(false)
  }
}

/** ADMIN 控制且应运行中但 tick 未启动时补启（如副屏暂停后本页继续） */
function ensureLocalTickRunning() {
  if (
    controlOwner.value === 'ADMIN' &&
    !applyingRemote.value &&
    !paused.value &&
    !pendingPhase.value &&
    phase.value !== 'idle' &&
    !isPhaseAtStart() &&
    !ticking.value &&
    remainingSec.value > 0
  ) {
    startTick(false)
  }
}

function enterPhase(next: Phase) {
  phase.value = next
  const plan = activePlan.value
  if (!plan) return
  if (next === 'work') {
    plannedSec.value = plan.workDurationMin * 60
  } else if (next === 'shortBreak') {
    plannedSec.value = plan.shortBreakMin * 60
  } else if (next === 'longBreak') {
    plannedSec.value = plan.longBreakMin * 60
  } else {
    remainingSec.value = 0
    return
  }
  remainingSec.value = plannedSec.value
  paused.value = false
  startTick(false)
}

async function startPendingPhase() {
  if (pendingPhase.value === 'work' && todayRoundsGoalReached.value) {
    notifyTodayRoundsGoalReached()
    return
  }
  const next = pendingPhase.value
  if (!next || !activePlan.value) return
  pendingPhase.value = null
  controlOwner.value = 'ADMIN'
  enterPhase(next)
  await publishSession(true)
}

async function startWork() {
  if (todayRoundsGoalReached.value) {
    notifyTodayRoundsGoalReached()
    return
  }
  if (!activePlan.value) {
    ElMessage.warning(t('pomodoro.timer.noPlan'))
    return
  }
  if (pendingPhase.value) {
    await startPendingPhase()
    return
  }
  controlOwner.value = 'ADMIN'
  enterPhase('work')
  await publishSession(true)
}

async function togglePause() {
  if (todayRoundsGoalReached.value && phase.value === 'idle') {
    return
  }
  const willPause = !paused.value
  paused.value = willPause
  controlOwner.value = 'ADMIN'
  if (willPause) {
    clearTick()
  } else {
    // 副屏暂停后 tick 已被 clear；本页继续须重启本地倒计时
    startTick(false)
  }
  await publishSession(true)
  // 暂停后不再由本页心跳占位，便于副屏 takeControl 继续并回传 DEVICE 会话
  if (willPause) {
    void pullRemoteSession()
  }
}

async function startCurrentPhase() {
  if (phase.value === 'work' && todayRoundsGoalReached.value) {
    notifyTodayRoundsGoalReached()
    return
  }
  if (!activePlan.value || phase.value === 'idle' || pendingPhase.value) return
  controlOwner.value = 'ADMIN'
  paused.value = false
  startTick(false)
  await publishSession(true)
}

async function resetTimer() {
  clearTick()
  pendingPhase.value = null
  paused.value = false
  controlOwner.value = 'ADMIN'

  const plan = activePlan.value
  if (!plan) return

  if (phase.value === 'shortBreak') {
    plannedSec.value = plan.shortBreakMin * 60
    remainingSec.value = plannedSec.value
  } else if (phase.value === 'longBreak') {
    plannedSec.value = plan.longBreakMin * 60
    remainingSec.value = plannedSec.value
  } else if (phase.value === 'work') {
    plannedSec.value = plan.workDurationMin * 60
    remainingSec.value = plannedSec.value
  } else {
    phase.value = 'idle'
    remainingSec.value = 0
  }
  await publishSession(true)
}

async function savePhaseRecord(type: 'WORK' | 'SHORT_BREAK' | 'LONG_BREAK', elapsedSec: number) {
  if (!activePlan.value || elapsedSec < 1) return
  await createRecord({
    planId: activePlan.value.id,
    recordType: type,
    durationSec: elapsedSec,
  })
  await refreshToday()
}

async function onPhaseComplete() {
  const plan = activePlan.value
  if (!plan) return
  const elapsed = plannedSec.value

  clearTick()
  paused.value = false
  remainingSec.value = 0
  controlOwner.value = 'ADMIN'

  if (phase.value === 'work') {
    await savePhaseRecord('WORK', elapsed)
    sessionWorkRounds.value += 1
    await refreshToday()
    if (isTodayRoundsGoalReached()) {
      pendingPhase.value = null
      phase.value = 'idle'
      remainingSec.value = 0
      await publishSession(true)
      ElMessage.success(t('pomodoro.timer.todayRoundsDone'))
      return
    }
    pendingPhase.value =
      sessionWorkRounds.value % plan.roundsBeforeLongBreak === 0
        ? 'longBreak'
        : 'shortBreak'
    await publishSession(true)
    ElMessage.success(t('pomodoro.timer.workDonePending'))
    return
  }

  if (phase.value === 'shortBreak' || phase.value === 'longBreak') {
    const type = phase.value === 'longBreak' ? 'LONG_BREAK' : 'SHORT_BREAK'
    await savePhaseRecord(type, elapsed)
    pendingPhase.value = 'work'
    await publishSession(true)
    ElMessage.success(t('pomodoro.timer.breakDonePending'))
  }
}

async function skipPhase() {
  const plan = activePlan.value
  if (!plan || phase.value === 'idle' || pendingPhase.value) return

  const elapsed = Math.max(0, plannedSec.value - remainingSec.value)
  clearTick()
  paused.value = false
  remainingSec.value = 0
  controlOwner.value = 'ADMIN'

  if (phase.value === 'work') {
    if (elapsed >= 1) {
      await savePhaseRecord('WORK', elapsed)
      sessionWorkRounds.value += 1
    }
    pendingPhase.value =
      sessionWorkRounds.value % plan.roundsBeforeLongBreak === 0
        ? 'longBreak'
        : 'shortBreak'
    await publishSession(true)
    ElMessage.success(t('pomodoro.timer.workDonePending'))
    return
  }

  if (phase.value === 'shortBreak' || phase.value === 'longBreak') {
    if (elapsed >= 1) {
      const type = phase.value === 'longBreak' ? 'LONG_BREAK' : 'SHORT_BREAK'
      await savePhaseRecord(type, elapsed)
    }
    pendingPhase.value = 'work'
    await publishSession(true)
    ElMessage.success(t('pomodoro.timer.breakDonePending'))
  }
}

async function refreshToday() {
  today.value = await fetchTodayStat()
  maybeNotifyTodayPlanDone()
}

async function loadPlans() {
  plans.value = await fetchEnabledPlans()
  if (plans.value.length === 0) return
  if (!selectedPlanId.value) {
    const def = await fetchDefaultPlan()
    selectedPlanId.value = def.id
  }
}

function isLocallyPlanEditBlocked(): boolean {
  if (phase.value === 'idle' || pendingPhase.value) return false
  if (isPhaseAtStart()) return false
  if (paused.value) return false
  return ticking.value || remainingSec.value > 0
}

function isPlanEditBlocked(): boolean {
  if (isLocallyPlanEditBlocked()) return true
  return isPlanMutationBlocked(lastRemoteSession.value)
}

async function checkPlanEditable(): Promise<boolean> {
  try {
    const session = await fetchActiveSession()
    serverReachable.value = true
    if (session) {
      lastRemoteSession.value = session
      if (isPlanMutationBlocked(session)) {
        return false
      }
    }
  } catch {
    serverReachable.value = false
  }
  return !isLocallyPlanEditBlocked()
}

function phaseDurationSec(plan: PomodoroPlan, currentPhase: Phase): number {
  if (currentPhase === 'work') return plan.workDurationMin * 60
  if (currentPhase === 'shortBreak') return plan.shortBreakMin * 60
  if (currentPhase === 'longBreak') return plan.longBreakMin * 60
  return plan.workDurationMin * 60
}

function refreshPhaseDurationsFromPlan(plan: PomodoroPlan) {
  if (phase.value === 'idle' || pendingPhase.value) return

  const newTotal = phaseDurationSec(plan, phase.value)
  const oldTotal = plannedSec.value > 0 ? plannedSec.value : newTotal
  plannedSec.value = newTotal

  if (isPhaseAtStart()) {
    remainingSec.value = newTotal
    return
  }

  if (paused.value && oldTotal > 0) {
    remainingSec.value = Math.min(
      newTotal,
      Math.max(0, Math.round((remainingSec.value / oldTotal) * newTotal)),
    )
    return
  }

  remainingSec.value = Math.min(remainingSec.value, newTotal)
}

async function onPlansChanged(affectedPlanId?: number) {
  const previousPlanId = selectedPlanId.value
  await loadPlans()

  if (affectedPlanId != null) {
    const exists = plans.value.some((p) => p.id === affectedPlanId)
    if (exists) {
      selectedPlanId.value = affectedPlanId
    }
  } else if (previousPlanId != null && plans.value.some((p) => p.id === previousPlanId)) {
    selectedPlanId.value = previousPlanId
  }

  const plan =
    (affectedPlanId != null ? plans.value.find((p) => p.id === affectedPlanId) : null) ??
    activePlan.value
  if (plan && selectedPlanId.value === plan.id) {
    refreshPhaseDurationsFromPlan(plan)
  }

  await publishSession(true)
  void pullRemoteSession()
}

function onPageVisible() {
  if (document.visibilityState === 'visible') {
    void pullRemoteSession()
  }
}

onMounted(async () => {
  await loadPlans()
  await refreshToday()
  todayPlanKnownComplete = isTodayPlanComplete()
  lastSeenWorkRounds.value = today.value.workRounds
  skipNextPlanDoneNotify = false
  startRemoteSync()
  document.addEventListener('visibilitychange', onPageVisible)
})

onUnmounted(() => {
  clearTick()
  clearRemoteSync()
  document.removeEventListener('visibilitychange', onPageVisible)
})

defineExpose({
  loadPlans,
  refreshToday,
  pullRemoteSession,
  startRemoteSync,
  serverReachable,
  isPlanEditBlocked,
  checkPlanEditable,
  onPlansChanged,
})
</script>

<style scoped lang="scss">
@use './pomodoro-pixel.scss';
</style>
