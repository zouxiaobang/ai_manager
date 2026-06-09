<template>
  <div class="timer-panel">
    <el-row :gutter="16">
      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <el-form inline>
            <el-form-item :label="t('pomodoro.timer.plan')">
              <el-select
                v-model="selectedPlanId"
                style="width: 220px"
                @change="onPlanChange"
              >
                <el-option
                  v-for="p in plans"
                  :key="p.id"
                  :label="p.title"
                  :value="p.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-tag :type="syncTagType" size="small">{{ syncStatusText }}</el-tag>
            </el-form-item>
          </el-form>

          <div class="timer-display">
            <el-tag :type="phaseTagType" size="large">{{ phaseLabel }}</el-tag>
            <div class="timer-display__clock">{{ clockText }}</div>
            <div class="timer-display__round">
              {{ t('pomodoro.timer.sessionRound', { n: sessionWorkRounds }) }}
            </div>
          </div>

          <div class="timer-actions">
            <el-button
              v-if="pendingPhase"
              type="primary"
              size="large"
              :disabled="todayRoundsGoalReached"
              @click="startPendingPhase"
            >
              {{ pendingPhase === 'work' ? t('pomodoro.timer.startWork') : t('pomodoro.timer.startBreak') }}
            </el-button>
            <el-button
              v-else-if="phase === 'idle'"
              type="primary"
              size="large"
              :disabled="todayRoundsGoalReached"
              @click="startWork"
            >
              {{ t('pomodoro.timer.startWork') }}
            </el-button>
            <el-button
              v-else-if="phaseReadyToStart"
              type="primary"
              size="large"
              :disabled="todayRoundsGoalReached"
              @click="startCurrentPhase"
            >
              {{ phase === 'work' ? t('pomodoro.timer.startWork') : t('pomodoro.timer.startBreak') }}
            </el-button>
            <template v-else>
              <el-button type="primary" size="large" @click="togglePause">
                {{ paused ? t('pomodoro.timer.resume') : t('pomodoro.timer.pause') }}
              </el-button>
              <el-button size="large" @click="skipPhase">{{ t('pomodoro.timer.skip') }}</el-button>
              <el-button size="large" @click="resetTimer">{{ t('pomodoro.timer.reset') }}</el-button>
            </template>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card shadow="never">
          <h4 class="side-title">{{ t('pomodoro.timer.todayTitle') }}</h4>
          <div class="today-stat">
            <div class="today-stat__item">
              <span class="today-stat__label">{{ t('pomodoro.timer.todayRounds') }}</span>
              <span class="today-stat__value">{{ today.workRounds }}</span>
            </div>
            <div class="today-stat__item">
              <span class="today-stat__label">{{ t('pomodoro.timer.todayMinutes') }}</span>
              <span class="today-stat__value">{{ today.workMinutes }} min</span>
            </div>
          </div>
          <el-progress
            v-if="activePlan"
            :percentage="roundProgress"
            :stroke-width="10"
            style="margin-top: 16px"
          />
          <p v-if="activePlan" class="goal-hint">
            {{ t('pomodoro.timer.goalHint', {
              rounds: today.workRounds,
              goalRounds: activePlan.dailyGoalRounds,
            }) }}
          </p>
          <el-progress
            v-if="activePlan"
            :percentage="minuteProgress"
            status="success"
            :stroke-width="10"
            style="margin-top: 12px"
          />
          <p v-if="activePlan" class="goal-hint">
            {{ t('pomodoro.timer.minuteGoalHint', {
              minutes: today.workMinutes,
              goal: activePlan.dailyGoalMinutes,
            }) }}
          </p>
          <el-alert
            v-if="todayPlanComplete"
            type="success"
            :closable="false"
            show-icon
            style="margin-top: 16px"
          >
            {{ t('pomodoro.timer.todayPlanDone') }}
          </el-alert>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
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
const lastAppliedSyncedMs = ref(0)
const lastSeenWorkRounds = ref(0)
const applyingRemote = ref(false)

let tickTimer: ReturnType<typeof setInterval> | null = null
let remoteSyncTimer: ReturnType<typeof setInterval> | null = null
let lastPublishedFingerprint = ''

const REMOTE_SYNC_INTERVAL_MS = 1000

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

const syncStatusText = computed(() => {
  if (controlOwner.value === 'DEVICE') {
    return t('pomodoro.timer.syncDevice')
  }
  if (controlOwner.value === 'ADMIN') {
    return t('pomodoro.timer.syncAdmin')
  }
  return t('pomodoro.timer.syncIdle')
})

const syncTagType = computed(() => {
  if (controlOwner.value === 'DEVICE') return 'warning'
  if (controlOwner.value === 'ADMIN') return 'success'
  return 'info'
})

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

const phaseTagType = computed(() => {
  if (phase.value === 'work') return 'danger'
  if (phase.value === 'idle') return 'info'
  return 'success'
})

const clockText = computed(() => {
  const m = Math.floor(remainingSec.value / 60)
  const s = remainingSec.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

function isPhaseAtStart(): boolean {
  return (
    phase.value !== 'idle' &&
    !pendingPhase.value &&
    tickTimer === null &&
    !paused.value &&
    plannedSec.value > 0 &&
    remainingSec.value >= plannedSec.value
  )
}

const phaseReadyToStart = computed(() => isPhaseAtStart())

const roundProgress = computed(() => {
  if (!activePlan.value || activePlan.value.dailyGoalRounds <= 0) return 0
  return Math.min(100, Math.round((today.value.workRounds / activePlan.value.dailyGoalRounds) * 100))
})

const minuteProgress = computed(() => {
  if (!activePlan.value || activePlan.value.dailyGoalMinutes <= 0) return 0
  return Math.min(100, Math.round((today.value.workMinutes / activePlan.value.dailyGoalMinutes) * 100))
})

function clearTick() {
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
      tickTimer !== null ||
      paused.value ||
      pendingPhase.value !== null ||
      remainingSec.value !== 0
    if (localNotReset) return true
    if (controlOwner.value !== 'DEVICE') return true
  }

  // 副屏阶段内重置为待开始（WORK+IDLE 等历史协议）
  if (owner === 'DEVICE' && runState === 'IDLE' && serverPhase !== 'IDLE') {
    if (tickTimer !== null || paused.value) return true
    if (controlOwner.value !== 'DEVICE') return true
  }

  if (owner === 'DEVICE' && remoteActive) {
    if (phase.value === 'idle') return true
    // 本页曾暂停，副屏点继续后跟随 RUNNING，并切换为「已与副屏同步」
    if (controlOwner.value === 'ADMIN' && runState === 'RUNNING') return true
    if (controlOwner.value !== 'DEVICE') return true
    if (paused.value !== (runState === 'PAUSED')) return true
    // 上次 apply 半途失败：副屏在跑但本地未启动 tick
    if (runState === 'RUNNING' && !paused.value && tickTimer === null) return true
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
  } catch {
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
    if (!session) {
      return
    }
    applyRemoteSession(session)
  } catch {
    // ignore
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
  if (todayRoundsGoalReached.value) return
  const next = pendingPhase.value
  if (!next || !activePlan.value) return
  pendingPhase.value = null
  controlOwner.value = 'ADMIN'
  enterPhase(next)
  await publishSession(true)
}

async function startWork() {
  if (todayRoundsGoalReached.value) return
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
  const willPause = !paused.value
  paused.value = willPause
  controlOwner.value = 'ADMIN'
  await publishSession(true)
  // 暂停后不再由本页心跳占位，便于副屏 takeControl 继续并回传 DEVICE 会话
  if (willPause) {
    void pullRemoteSession()
  }
}

async function startCurrentPhase() {
  if (todayRoundsGoalReached.value) return
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
  const elapsed = Math.max(0, plannedSec.value - remainingSec.value)
  clearTick()
  pendingPhase.value = null
  if (phase.value === 'work' && elapsed >= 30) {
    await savePhaseRecord('WORK', elapsed)
    sessionWorkRounds.value += 1
  } else if (
    (phase.value === 'shortBreak' || phase.value === 'longBreak') &&
    elapsed >= 10
  ) {
    const type = phase.value === 'longBreak' ? 'LONG_BREAK' : 'SHORT_BREAK'
    await savePhaseRecord(type, elapsed)
  }
  phase.value = 'idle'
  remainingSec.value = 0
  controlOwner.value = 'ADMIN'
  await publishSession(true)
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

async function onPlanChange() {
  if (phase.value !== 'idle') {
    await resetTimer()
  }
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

defineExpose({ loadPlans, refreshToday, pullRemoteSession, startRemoteSync })
</script>

<style scoped lang="scss">
.timer-display {
  text-align: center;
  padding: 32px 0 24px;
}

.timer-display__clock {
  font-size: 56px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  margin: 16px 0 8px;
  letter-spacing: 2px;
}

.timer-display__round {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.timer-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.side-title {
  margin: 0 0 12px;
  font-size: 15px;
}

.today-stat {
  display: flex;
  gap: 24px;
}

.today-stat__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.today-stat__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.today-stat__value {
  font-size: 22px;
  font-weight: 600;
}

.goal-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
