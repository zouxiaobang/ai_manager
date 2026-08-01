<template>
  <div class="v2-pomodoro-page">
    <div class="v2-pomodoro-page__bg"></div>
    <div class="v2-pomodoro-page__content">
      <div v-show="activeTab === 'timer'" class="v2-pomodoro-body">
        <div class="v2-pomodoro-sync">
          <div class="v2-pomodoro-sync__inner">
            <div class="v2-pomodoro-sync__left">
              <span class="v2-pomodoro-sync__label">{{ t('pomodoro.timer.syncControllerPrefix') }}</span>
              <span class="v2-pomodoro-sync__value v2-pomodoro-sync__value--control">{{ controlWho }}</span>
            </div>
            <div class="v2-pomodoro-sync__center">
              <span class="v2-pomodoro-sync__health">{{ syncHealthText }}</span>
              <span class="v2-pomodoro-sync__wifi" :class="{ 'is-on': deviceOnline }">
                <svg viewBox="0 0 14 10" width="16" height="12" fill="currentColor">
                  <rect x="2" y="0" width="2" height="2" />
                  <rect x="10" y="0" width="2" height="2" />
                  <rect x="1" y="2" width="12" height="2" />
                  <rect x="2" y="4" width="10" height="2" />
                  <rect x="3" y="6" width="8" height="2" />
                  <rect x="6" y="8" width="2" height="2" />
                </svg>
              </span>
            </div>
            <div class="v2-pomodoro-sync__right">
              <span class="v2-pomodoro-sync__label">{{ t('pomodoro.timer.syncPanelDevice') }}</span>
              <span class="v2-pomodoro-sync__badge" :class="{ 'is-on': deviceOnline }">
                <span class="v2-pomodoro-sync__dot" />
                {{ deviceOnline ? t('pomodoro.timer.online') : t('pomodoro.timer.offline') }}
              </span>
            </div>
            <nav v-if="!reportLoading && reportDailySorted.length > TABLE_PAGE_SIZE" class="v2-report__pagination">
              <button type="button" class="v2-report__page-btn" :disabled="reportTablePage <= 1" @click="reportTablePage -= 1">
                {{ t('pomodoro.report.prevPage') }}
              </button>
              <span class="v2-report__page-info">
                {{ t('pomodoro.report.pageIndicator', { page: reportTablePage, total: reportTablePageCount }) }}
              </span>
              <button type="button" class="v2-report__page-btn" :disabled="reportTablePage >= reportTablePageCount" @click="reportTablePage += 1">
                {{ t('pomodoro.report.nextPage') }}
              </button>
            </nav>
          </div>
        </div>

        <div class="v2-pomodoro-main">
          <div class="v2-pomodoro-ring-wrap">
            <V2PomodoroRing
              :clock="displayClockText"
              :status="phaseStatusText"
              :percentage="ringProgress"
              :color="phaseColor"
            />
          </div>

          <div class="v2-pomodoro-phase-pills">
            <button
              type="button"
              class="v2-pomodoro-phase-pill v2-pomodoro-phase-pill--work"
              :class="{ 'is-active': pillActive.work }"
              disabled
            >
              <span>🍅</span>
              {{ phasePillText.work }}
            </button>
            <button
              type="button"
              class="v2-pomodoro-phase-pill v2-pomodoro-phase-pill--short"
              :class="{ 'is-active': pillActive.shortBreak }"
              disabled
            >
              <span>🍃</span>
              {{ phasePillText.short }}
            </button>
            <button
              type="button"
              class="v2-pomodoro-phase-pill v2-pomodoro-phase-pill--long"
              :class="{ 'is-active': pillActive.longBreak }"
              disabled
            >
              <span>☕</span>
              {{ phasePillText.long }}
            </button>
          </div>

          <div v-if="showSecondaryActions" class="v2-pomodoro-actions">
            <button type="button" class="v2-pomodoro-action" @click="skipPhase">{{ t('pomodoro.timer.skip') }}</button>
            <button type="button" class="v2-pomodoro-action" @click="resetTimer">{{ t('pomodoro.timer.reset') }}</button>
            <button
              v-if="serverReachable && controlOwner === 'DEVICE'"
              type="button"
              class="v2-pomodoro-action v2-pomodoro-action--warn"
              @click="takeControl"
            >
              {{ t('pomodoro.timer.takeControl') }}
            </button>
          </div>
        </div>

        <div class="v2-pomodoro-side">
          <div class="v2-pomodoro-progress">
            <h3 class="v2-pomodoro-progress__title">{{ t('pomodoro.timer.todayProgress') }}</h3>
            <p class="v2-pomodoro-progress__caption">{{ t('pomodoro.timer.completedRounds') }}</p>
            <div class="v2-pomodoro-progress__dots">
              <div v-for="(row, ri) in dailyDotRows" :key="ri" class="v2-pomodoro-progress__dot-row">
                <span
                  v-for="(filled, di) in row"
                  :key="`${ri}-${di}`"
                  class="v2-pomodoro-progress__dot"
                  :class="{ 'is-filled': filled }"
                />
              </div>
            </div>
            <p class="v2-pomodoro-progress__today">
              <span>{{ t('pomodoro.timer.todayRoundPrefix') }}</span>
              <span class="v2-pomodoro-progress__big-num">{{ today.workRounds }}</span>
              <span>{{ t('pomodoro.timer.todayRoundSuffix') }}</span>
            </p>
          </div>

          <el-alert
            v-if="todayRoundsGoalReached && !todayPlanComplete"
            type="info"
            :closable="false"
            show-icon
            class="v2-pomodoro-alert"
          >
            {{ t('pomodoro.timer.todayRoundsDone') }}
          </el-alert>
          <el-alert
            v-if="todayPlanComplete"
            type="success"
            :closable="false"
            show-icon
            class="v2-pomodoro-alert"
          >
            {{ t('pomodoro.timer.todayPlanDone') }}
          </el-alert>
        </div>
      </div>

      <div v-show="activeTab === 'timer'" class="v2-pomodoro-start-bar">
        <button
          type="button"
          class="v2-pomodoro-start"
          :class="{ 'is-goal-blocked': primaryStartWorkBlocked }"
          @click="onPrimaryClick"
        >
          <span class="v2-pomodoro-start__icon">{{ primaryButtonIcon }}</span>
          {{ primaryButtonLabel }}
        </button>
      </div>

      <div v-show="activeTab === 'stats'" class="v2-report">
        <div class="v2-report__toolbar">
          <el-date-picker
            v-model="reportRange"
            class="v2-report__date-picker"
            type="daterange"
            value-format="YYYY-MM-DD"
            :start-placeholder="t('pomodoro.report.start')"
            :end-placeholder="t('pomodoro.report.end')"
            :teleported="true"
          />
          <button type="button" class="v2-report__query" :disabled="reportLoading" @click="loadReport">
            {{ t('pomodoro.report.query') }}
          </button>
        </div>

        <div class="v2-report__body">
          <div class="v2-report__kpis">
            <div v-for="kpi in reportKpis" :key="kpi.key" class="v2-report__kpi-card">
              <p class="v2-report__kpi-label">{{ kpi.label }}</p>
              <p class="v2-report__kpi-value" :class="`v2-report__kpi-value--${kpi.tone}`">{{ kpi.value }}</p>
            </div>
          </div>

          <div class="v2-report__chart">
            <h3 class="v2-report__section-title">
              <span class="pixel-spark">✦</span>
              {{ t('pomodoro.report.chartRounds') }}
              <span class="pixel-spark">✦</span>
            </h3>
            <div v-if="reportLoading" class="v2-report__state">{{ t('pomodoro.report.loading') }}</div>
            <div v-else-if="reportDaily.length === 0" class="v2-report__state">{{ t('pomodoro.report.noData') }}</div>
            <div v-else class="v2-report__bar-chart">
              <div
                v-for="row in reportDailySorted"
                :key="row.statDate"
                class="v2-report__bar-col"
                :title="reportBarLabel(row.workRounds)"
              >
                <div class="v2-report__bar-stack">
                  <div class="v2-report__bar">
                    <span
                      v-for="bi in reportBarBlocks(row.workRounds)"
                      :key="bi"
                      class="v2-report__bar-block"
                    />
                  </div>
                </div>
                <span class="v2-report__bar-label">{{ formatChartDate(row.statDate) }}</span>
              </div>
            </div>
          </div>

          <div class="v2-report__table-wrap">
            <h3 class="v2-report__section-title">
              <span class="pixel-spark">✦</span>
              {{ t('pomodoro.report.dailyTable') }}
              <span class="pixel-spark">✦</span>
            </h3>
            <div class="v2-report__table-scroll">
              <table class="v2-report__table">
                <thead>
                  <tr>
                    <th>{{ t('pomodoro.report.date') }}</th>
                    <th>{{ t('pomodoro.report.rounds') }}</th>
                    <th>{{ t('pomodoro.report.workMin') }}</th>
                    <th>{{ t('pomodoro.report.breakMin') }}</th>
                    <th class="v2-report__th-progress">{{ t('pomodoro.report.workBar') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="reportLoading">
                    <td colspan="5" class="v2-report__td-state">{{ t('pomodoro.report.loading') }}</td>
                  </tr>
                  <tr v-else-if="reportDaily.length === 0">
                    <td colspan="5" class="v2-report__td-state">{{ t('pomodoro.report.noData') }}</td>
                  </tr>
                  <tr v-for="row in reportDailyPaged" v-else :key="row.statDate">
                    <td>{{ formatTableDate(row.statDate) }}</td>
                    <td class="v2-report__td-num v2-report__td-num--red">{{ row.workRounds }}</td>
                    <td>{{ row.workMinutes }}m</td>
                    <td>{{ row.breakMinutes }}m</td>
                    <td>
                      <div class="pixel-progress-bar">
                        <div class="pixel-progress-bar__fill" :style="{ width: `${reportBarPct(row.workMinutes)}%` }" />
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <div v-show="activeTab === 'plan'" class="v2-plan">
        <div v-if="planEditLocked" class="v2-plan__lock">{{ t('pomodoro.plan.pauseRequired') }}</div>

        <div class="v2-plan__header" @click="planListExpanded = !planListExpanded">
          <span class="v2-plan__header-arrow">{{ planListExpanded ? '▼' : '▶' }}</span>
          <span class="v2-plan__header-label">{{ t('pomodoro.plan.listTitle') }}</span>
          <span class="v2-plan__header-count">{{ planRecords.length }}</span>
        </div>

        <div v-show="planListExpanded" class="v2-plan__list">
          <div v-if="planLoading" class="v2-plan__state">{{ t('pomodoro.plan.loading') }}</div>
          <div v-else-if="planRecords.length === 0" class="v2-plan__state">{{ t('pomodoro.plan.emptyList') }}</div>
          <button
            v-for="row in planRecords"
            v-else
            :key="row.id"
            type="button"
            class="v2-plan__list-item"
            :class="{ 'is-selected': planSelectedId === row.id }"
            @click="planSelectedId = row.id"
          >
            <p class="v2-plan__list-title">{{ row.title }}</p>
            <div class="v2-plan__list-meta">
              <span v-if="row.isDefault === 1" class="v2-plan__tag">{{ t('pomodoro.plan.defaultTag') }}</span>
              <span class="v2-plan__list-durs">
                <span class="v2-plan__dur--work">{{ row.workDurationMin }}</span>
                <span class="v2-plan__dur-sep">+</span>
                <span class="v2-plan__dur--short">{{ row.shortBreakMin }}</span>
                <span class="v2-plan__dur-sep">+</span>
                <span class="v2-plan__dur--long">{{ row.longBreakMin }}</span>
              </span>
            </div>
          </button>
        </div>

        <div v-if="planSelected" class="v2-plan__detail">
          <div class="v2-plan__detail-header">
            <h2 class="v2-plan__detail-title">{{ planSelected.title }}</h2>
            <span v-if="planSelected.isDefault === 1" class="v2-plan__tag">{{ t('pomodoro.plan.defaultTag') }}</span>
          </div>
          <p class="v2-plan__detail-durs">
            <span class="v2-plan__dur--work">{{ planSelected.workDurationMin }}</span>
            <span class="v2-plan__dur-sep">+</span>
            <span class="v2-plan__dur--short">{{ planSelected.shortBreakMin }}</span>
            <span class="v2-plan__dur-sep">+</span>
            <span class="v2-plan__dur--long">{{ planSelected.longBreakMin }}</span>
          </p>
          <div class="v2-plan__detail-fields">
            <div class="v2-plan__detail-row">
              <span class="v2-plan__detail-label">{{ t('pomodoro.plan.work') }}</span>
              <span class="v2-plan__detail-val v2-plan__detail-val--work">{{ planSelected.workDurationMin }} {{ t('pomodoro.plan.minuteUnit') }}</span>
            </div>
            <div class="v2-plan__detail-row">
              <span class="v2-plan__detail-label">{{ t('pomodoro.plan.shortBreak') }}</span>
              <span class="v2-plan__detail-val v2-plan__detail-val--short">{{ planSelected.shortBreakMin }} {{ t('pomodoro.plan.minuteUnit') }}</span>
            </div>
            <div class="v2-plan__detail-row">
              <span class="v2-plan__detail-label">{{ t('pomodoro.plan.longBreak') }}</span>
              <span class="v2-plan__detail-val v2-plan__detail-val--long">{{ planSelected.longBreakMin }} {{ t('pomodoro.plan.minuteUnit') }}</span>
            </div>
            <div class="v2-plan__detail-row">
              <span class="v2-plan__detail-label">{{ t('pomodoro.plan.roundsBeforeLong') }}</span>
              <span class="v2-plan__detail-val v2-plan__detail-val--cyan">{{ t('pomodoro.plan.roundsEveryLong', { n: planSelected.roundsBeforeLongBreak }) }}</span>
            </div>
            <div class="v2-plan__detail-row">
              <span class="v2-plan__detail-label">{{ t('pomodoro.plan.dailyGoalRounds') }}</span>
              <span class="v2-plan__detail-val v2-plan__detail-val--cyan">{{ planSelected.dailyGoalRounds }} {{ t('pomodoro.plan.rounds') }}</span>
            </div>
            <div class="v2-plan__detail-row">
              <span class="v2-plan__detail-label">{{ t('pomodoro.plan.dailyGoalMinutes') }}</span>
              <span class="v2-plan__detail-val v2-plan__detail-val--blue">{{ planSelected.dailyGoalMinutes }} {{ t('pomodoro.plan.minuteUnit') }}</span>
            </div>
            <div class="v2-plan__detail-row">
              <span class="v2-plan__detail-label">{{ t('pomodoro.plan.dailyTotalMinutes') }}</span>
              <span class="v2-plan__detail-val v2-plan__detail-val--blue">{{ planSelectedDailyTotal }} {{ t('pomodoro.plan.minuteUnit') }}</span>
            </div>
          </div>
          <div class="v2-plan__detail-actions">
            <button type="button" class="v2-plan__act v2-plan__act--green" :disabled="planEditLocked || planSelected.isDefault === 1 || planSaving" @click="onSetDefault">
              <span>{{ t('pomodoro.plan.asDefault') }}</span>
            </button>
            <button type="button" class="v2-plan__act v2-plan__act--blue" :disabled="planEditLocked" @click="openEdit(planSelected)">
              <span>{{ t('pomodoro.plan.edit') }}</span>
            </button>
            <button type="button" class="v2-plan__act v2-plan__act--danger" :disabled="planEditLocked || planSelected.isDefault === 1" @click="onDelete(planSelected)">
              <span>{{ t('pomodoro.plan.delete') }}</span>
            </button>
          </div>
        </div>
        <div v-else-if="!planLoading && planRecords.length > 0" class="v2-plan__state">
          {{ t('pomodoro.plan.emptyDetail') }}
        </div>
      </div>

      <el-dialog
        v-model="planDialogVisible"
        class="v2-plan-dialog"
        width="90%"
        destroy-on-close
        :show-close="false"
        align-center
      >
        <div class="v2-plan-dialog__frame">
          <h3 class="v2-plan-dialog__title">
            {{ planEditingId ? t('pomodoro.plan.editTitle') : t('pomodoro.plan.createTitle') }}
          </h3>
          <el-form class="v2-plan-dialog__form" :model="planForm" label-width="120px">
            <el-form-item :label="t('pomodoro.plan.name')" required>
              <el-input v-model="planForm.title" />
            </el-form-item>
            <el-form-item :label="t('pomodoro.plan.work')">
              <el-input-number v-model="planForm.workDurationMin" :min="1" :max="120" />
            </el-form-item>
            <el-form-item :label="t('pomodoro.plan.shortBreak')">
              <el-input-number v-model="planForm.shortBreakMin" :min="1" :max="60" />
            </el-form-item>
            <el-form-item :label="t('pomodoro.plan.longBreak')">
              <el-input-number v-model="planForm.longBreakMin" :min="1" :max="60" />
            </el-form-item>
            <el-form-item :label="t('pomodoro.plan.roundsBeforeLong')">
              <el-input-number v-model="planForm.roundsBeforeLongBreak" :min="1" :max="12" />
            </el-form-item>
            <el-form-item :label="t('pomodoro.plan.dailyGoalRounds')">
              <el-input-number v-model="planForm.dailyGoalRounds" :min="1" :max="50" />
            </el-form-item>
            <el-form-item :label="t('pomodoro.plan.dailyGoalMinutes')">
              <span class="v2-plan-dialog__calc">{{ planDailyFocusMinutes }} {{ t('pomodoro.plan.minuteUnit') }}</span>
            </el-form-item>
            <el-form-item :label="t('pomodoro.plan.asDefault')">
              <el-switch v-model="planForm.asDefault" />
            </el-form-item>
          </el-form>
          <div class="v2-plan-dialog__footer">
            <button type="button" class="v2-plan__act v2-plan__act--blue" @click="planDialogVisible = false">
              <span>{{ t('pomodoro.common.cancel') }}</span>
            </button>
            <button type="button" class="v2-plan__act v2-plan__act--green" :disabled="planSaving" @click="savePlanData">
              <span>{{ t('pomodoro.common.save') }}</span>
            </button>
          </div>
        </div>
      </el-dialog>

      <div v-show="activeTab === 'plan'" class="v2-plan__toolbar-bar">
        <button type="button" class="v2-plan__btn v2-plan__btn--green" :disabled="planEditLocked" @click="openCreate">
          <span>+ {{ t('pomodoro.plan.add') }}</span>
        </button>
        <button type="button" class="v2-plan__btn v2-plan__btn--blue" :disabled="planLoading" @click="loadPlanData">
          <span>{{ t('pomodoro.plan.refresh') }}</span>
        </button>
      </div>

      <nav class="v2-pomodoro-nav">
        <div class="v2-pomodoro-nav__items">
          <button
            v-for="tab in navTabs"
            :key="tab.id"
            type="button"
            class="v2-pomodoro-nav__item"
            :class="{ 'is-active': activeTab === tab.id }"
            @click="activeTab = tab.id"
          >
            <span class="v2-pomodoro-nav__icon">{{ tab.icon }}</span>
            <span class="v2-pomodoro-nav__label">{{ tab.label }}</span>
          </button>
        </div>
      </nav>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import V2PomodoroRing from './components/V2PomodoroRing.vue'
import {
  createPlan,
  createRecord,
  fetchActiveSession,
  fetchDailyStats,
  fetchDefaultPlan,
  fetchEnabledPlans,
  fetchSummary,
  fetchTodayStat,
  removePlan,
  syncActiveSession,
  updatePlan,
  type PomodoroActiveSession,
  type PomodoroDailyStat,
  type PomodoroPlan,
  type PomodoroPlanSaveRequest,
  type PomodoroSessionSyncRequest,
  type PomodoroSummary,
} from '@/api/pomodoro'

import { usePomodoroSound } from '@/composables/usePomodoroSound'
import { useAppStore } from '@/stores/app'

type Phase = 'idle' | 'work' | 'shortBreak' | 'longBreak'

const { t } = useI18n()

const activeTab = ref<'timer' | 'stats' | 'plan'>('timer')

const navTabs = computed(() => [
  { id: 'timer' as const, icon: '⏱', label: t('pomodoro.tabs.timer') },
  { id: 'stats' as const, icon: '📊', label: t('pomodoro.tabs.report') },
  { id: 'plan' as const, icon: '📋', label: t('pomodoro.tabs.plan') },
])

const plans = ref<PomodoroPlan[]>([])
const selectedPlanId = ref<number | null>(null)
const phase = ref<Phase>('idle')
const remainingSec = ref(0)
const plannedSec = ref(0)
const paused = ref(false)
const sessionWorkRounds = ref(0)
const pendingPhase = ref<Phase | null>(null)
const controlOwner = ref<'ADMIN' | 'DEVICE' | null>(null)
const serverReachable = ref(false)
const lastRemoteSession = ref<PomodoroActiveSession | null>(null)
const lastAppliedSyncedMs = ref(0)
const lastSeenWorkRounds = ref(0)
const applyingRemote = ref(false)
const ticking = ref(false)

let tickTimer: ReturnType<typeof setInterval> | null = null
let remoteSyncTimer: ReturnType<typeof setInterval> | null = null
let lastPublishedFingerprint = ''

const pomodoroSound = usePomodoroSound()
const appStore = useAppStore()

const REMOTE_SYNC_INTERVAL_MS = 1000
const DEVICE_ONLINE_TTL_MS = 15000

const today = ref({ workRounds: 0, workMinutes: 0, breakMinutes: 0 })
let todayPlanKnownComplete = false
let skipNextPlanDoneNotify = true

const reportLoading = ref(false)
const reportRange = ref<[string, string] | null>(null)
const reportDaily = ref<PomodoroDailyStat[]>([])
const reportSummary = ref<PomodoroSummary | null>(null)
const reportGoalMin = ref(200)
const TABLE_PAGE_SIZE = 6
const reportTablePage = ref(1)

const reportDailySorted = computed(() =>
  [...reportDaily.value].sort((a, b) => a.statDate.localeCompare(b.statDate)),
)

const reportTablePageCount = computed(() =>
  Math.max(1, Math.ceil(reportDailySorted.value.length / TABLE_PAGE_SIZE)),
)

const reportDailyPaged = computed(() => {
  const start = (reportTablePage.value - 1) * TABLE_PAGE_SIZE
  return reportDailySorted.value.slice(start, start + TABLE_PAGE_SIZE)
})

const reportMaxRounds = computed(() =>
  Math.max(1, ...reportDailySorted.value.map((r) => r.workRounds), 1),
)

const reportBarH = 100
const reportBarBlockH = 8
const reportBarBlockGap = 2

const reportKpis = computed(() => [
  {
    key: 'rounds',
    label: t('pomodoro.report.totalRoundsShort'),
    value: String(reportSummary.value?.totalWorkRounds ?? 0),
    tone: 'red',
  },
  {
    key: 'minutes',
    label: t('pomodoro.report.totalMinutesShort'),
    value: String(reportSummary.value?.totalWorkMinutes ?? 0),
    tone: 'green',
  },
  {
    key: 'days',
    label: t('pomodoro.report.activeDays'),
    value: String(reportSummary.value?.activeDays ?? 0),
    tone: 'blue',
  },
  {
    key: 'avg',
    label: t('pomodoro.report.avgMinutesShort'),
    value: String(Math.round(reportSummary.value?.avgWorkMinutesPerDay ?? 0)),
    tone: 'cyan',
  },
])

function initReportRange() {
  const end = new Date()
  const start = new Date(end)
  start.setDate(start.getDate() - 6)
  reportRange.value = [
    `${start.getFullYear()}-${String(start.getMonth() + 1).padStart(2, '0')}-${String(start.getDate()).padStart(2, '0')}`,
    `${end.getFullYear()}-${String(end.getMonth() + 1).padStart(2, '0')}-${String(end.getDate()).padStart(2, '0')}`,
  ]
}

function formatChartDate(statDate: string): string {
  const p = statDate.split('-')
  return p.length >= 3 ? `${p[1]}-${p[2]}` : statDate
}

function formatTableDate(statDate: string): string {
  return formatChartDate(statDate)
}

function reportBarBlocks(rounds: number): number {
  if (rounds <= 0) return 0
  const h = Math.max(reportBarBlockH, Math.round((rounds / reportMaxRounds.value) * reportBarH))
  return Math.ceil(h / (reportBarBlockH + reportBarBlockGap))
}

function reportBarLabel(rounds: number): string {
  return t('pomodoro.report.chartBarRounds', { n: rounds })
}

function reportBarPct(workMinutes: number): number {
  if (reportGoalMin.value <= 0) return 0
  return Math.min(100, Math.round((workMinutes / reportGoalMin.value) * 100))
}

async function loadReport() {
  if (!reportRange.value || reportRange.value.length !== 2) return
  const [startDate, endDate] = reportRange.value
  reportLoading.value = true
  try {
    const [dailyData, summaryData] = await Promise.all([
      fetchDailyStats(startDate, endDate),
      fetchSummary(startDate, endDate),
    ])
    reportDaily.value = dailyData
    reportSummary.value = summaryData
    reportTablePage.value = 1
  } finally {
    reportLoading.value = false
  }
}

const planLoading = ref(false)
const planRecords = ref<PomodoroPlan[]>([])
const planSelectedId = ref<number | null>(null)
const planDialogVisible = ref(false)
const planEditingId = ref<number | null>(null)
const planSaving = ref(false)
const planEditLocked = ref(false)
const planListExpanded = ref(false)

const planSelected = computed(() =>
  planRecords.value.find((r) => r.id === planSelectedId.value) ?? null,
)

const planDefaultForm = (): PomodoroPlanSaveRequest & { asDefault: boolean } => ({
  title: '',
  workDurationMin: 25,
  shortBreakMin: 5,
  longBreakMin: 15,
  roundsBeforeLongBreak: 4,
  dailyGoalRounds: 8,
  dailyGoalMinutes: 200,
  asDefault: false,
  status: 'ENABLED',
})

const planForm = reactive(planDefaultForm())

const planDailyFocusMinutes = computed(() =>
  Math.max(0, planForm.dailyGoalRounds) * Math.max(0, planForm.workDurationMin),
)

const planSelectedDailyTotal = computed(() => {
  const row = planSelected.value
  if (!row) return 0
  const focus = Math.max(0, row.dailyGoalRounds) * Math.max(0, row.workDurationMin)
  let breakSum = 0
  if (row.dailyGoalRounds > 1 && row.roundsBeforeLongBreak >= 1) {
    for (let k = 1; k < row.dailyGoalRounds; k++) {
      breakSum += k % row.roundsBeforeLongBreak === 0 ? row.longBreakMin : row.shortBreakMin
    }
  }
  return focus + breakSum
})

async function refreshPlanEditLock() {
  try {
    const session = await fetchActiveSession()
    planEditLocked.value =
      session !== null && session.runState === 'RUNNING'
  } catch {
    planEditLocked.value = false
  }
}

async function loadPlanData() {
  planLoading.value = true
  try {
    planRecords.value = await fetchEnabledPlans()
    if (planRecords.value.length > 0) {
      const current = planRecords.value.find((r) => r.id === planSelectedId.value)
      if (!current) {
        const def = planRecords.value.find((r) => r.isDefault === 1)
        planSelectedId.value = def?.id ?? planRecords.value[0].id
      }
    } else {
      planSelectedId.value = null
    }
  } finally {
    planLoading.value = false
  }
}

function openCreate() {
  if (planEditLocked.value) return
  planEditingId.value = null
  Object.assign(planForm, planDefaultForm())
  planDialogVisible.value = true
}

function openEdit(row: PomodoroPlan) {
  if (planEditLocked.value) return
  planEditingId.value = row.id
  Object.assign(planForm, {
    title: row.title,
    workDurationMin: row.workDurationMin,
    shortBreakMin: row.shortBreakMin,
    longBreakMin: row.longBreakMin,
    roundsBeforeLongBreak: row.roundsBeforeLongBreak,
    dailyGoalRounds: row.dailyGoalRounds,
    dailyGoalMinutes: row.dailyGoalMinutes,
    asDefault: row.isDefault === 1,
    status: row.status,
  })
  planDialogVisible.value = true
}

async function savePlanData() {
  if (planEditLocked.value) return
  if (!planForm.title.trim()) {
    ElMessage.warning(t('pomodoro.plan.nameRequired'))
    return
  }
  planSaving.value = true
  try {
    const payload: PomodoroPlanSaveRequest & { asDefault?: boolean } = {
      ...planForm,
      dailyGoalMinutes: planDailyFocusMinutes.value,
    }
    if (planEditingId.value) {
      await updatePlan(planEditingId.value, payload)
      planSelectedId.value = planEditingId.value
    } else {
      const created = await createPlan(payload)
      planSelectedId.value = created.id
    }
    planDialogVisible.value = false
    ElMessage.success(t('pomodoro.common.saved'))
    await loadPlanData()
  } finally {
    planSaving.value = false
  }
}

async function onSetDefault() {
  if (planEditLocked.value) return
  const row = planSelected.value
  if (!row || row.isDefault === 1) return
  planSaving.value = true
  try {
    const payload: PomodoroPlanSaveRequest = {
      title: row.title,
      workDurationMin: row.workDurationMin,
      shortBreakMin: row.shortBreakMin,
      longBreakMin: row.longBreakMin,
      roundsBeforeLongBreak: row.roundsBeforeLongBreak,
      dailyGoalRounds: row.dailyGoalRounds,
      dailyGoalMinutes: row.dailyGoalMinutes,
      asDefault: true,
      status: row.status,
    }
    await updatePlan(row.id, payload)
    ElMessage.success(t('pomodoro.common.saved'))
    await loadPlanData()
  } finally {
    planSaving.value = false
  }
}

async function onDelete(row: PomodoroPlan) {
  if (planEditLocked.value) return
  await ElMessageBox.confirm(t('pomodoro.plan.deleteConfirm', { name: row.title }), { type: 'warning' })
  await removePlan(row.id)
  ElMessage.success(t('pomodoro.common.deleted'))
  await loadPlanData()
}

function isTodayPlanComplete(): boolean {
  const plan = activePlan.value
  if (!plan || plan.dailyGoalRounds <= 0 || plan.dailyGoalMinutes <= 0) return false
  return today.value.workRounds >= plan.dailyGoalRounds && today.value.workMinutes >= plan.dailyGoalMinutes
}

const todayPlanComplete = computed(() => isTodayPlanComplete())

function isTodayRoundsGoalReached(): boolean {
  const plan = activePlan.value
  if (!plan || plan.dailyGoalRounds <= 0) return false
  return today.value.workRounds >= plan.dailyGoalRounds
}

const todayRoundsGoalReached = computed(() => isTodayRoundsGoalReached())

function notifyTodayRoundsGoalReached() {
  const plan = activePlan.value
  if (!plan || plan.dailyGoalRounds <= 0) return
  ElMessage.warning(t('pomodoro.timer.todayRoundsGoalReached', { goal: plan.dailyGoalRounds, rounds: today.value.workRounds }))
}

function todayPlanNotifyKey(): string {
  return `pomodoro-plan-done-${new Date().toISOString().slice(0, 10)}`
}

function notifyTodayPlanDone() {
  if (sessionStorage.getItem(todayPlanNotifyKey())) return
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

const activePlan = computed(() => plans.value.find((p) => p.id === selectedPlanId.value) ?? null)

const syncHealthText = computed(() => {
  if (!serverReachable.value) return t('pomodoro.timer.syncOffline')
  if (deviceOnline.value) return t('pomodoro.timer.syncNormal')
  return t('pomodoro.timer.syncWaitingDevice')
})

const deviceOnline = computed(() => {
  if (!serverReachable.value) return false
  const seen = lastRemoteSession.value?.deviceLastSeenMs
  if (!seen) return false
  return Date.now() - seen <= DEVICE_ONLINE_TTL_MS
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
  if (!plan) return { work: '', short: '', long: '' }
  return {
    work: t('pomodoro.timer.phaseWorkDuration', { time: formatMinTime(plan.workDurationMin) }),
    short: t('pomodoro.timer.phaseShortDuration', { time: formatMinTime(plan.shortBreakMin) }),
    long: t('pomodoro.timer.phaseLongDuration', { time: formatMinTime(plan.longBreakMin) }),
  }
})

const uiPhase = computed((): Phase => {
  if (phase.value === 'idle' && pendingPhase.value) return pendingPhase.value
  return phase.value
})

const ringProgress = computed(() => {
  if (phase.value === 'idle' && !pendingPhase.value) return 0
  if (phase.value === 'idle' && pendingPhase.value) return 100
  if (!plannedSec.value) return 0
  return Math.min(100, Math.round(((plannedSec.value - remainingSec.value) / plannedSec.value) * 100))
})

const phaseColor = computed(() => {
  const p = uiPhase.value
  if (p === 'work') return '#ef5350'
  if (p === 'shortBreak') return '#8bc34a'
  if (p === 'longBreak') return '#7e57c2'
  return '#8bc34a'
})

const displayClockText = computed(() => {
  if (todayRoundsGoalReached.value && phase.value === 'idle' && !pendingPhase.value) return '--:--'
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
  if (phase.value === 'idle') return t('pomodoro.timer.phaseIdle')
  if (phaseReadyToStart.value) return t('pomodoro.timer.phaseIdle')
  if (paused.value) return `${phaseLabel.value} · ${t('pomodoro.timer.pausedSuffix')}`
  return phaseLabel.value
})

const pillActive = computed(() => ({
  work: uiPhase.value === 'work',
  shortBreak: uiPhase.value === 'shortBreak',
  longBreak: uiPhase.value === 'longBreak',
}))

const controlWho = computed(() => {
  if (controlOwner.value === 'ADMIN') return t('pomodoro.timer.syncControllerAdmin')
  if (controlOwner.value === 'DEVICE') return t('pomodoro.timer.syncControllerDevice')
  return '—'
})

const primaryButtonLabel = computed(() => {
  if (primaryStartWorkBlocked.value) return t('pomodoro.timer.todayRoundsDone')
  if (pendingPhase.value) {
    return pendingPhase.value === 'work' ? t('pomodoro.timer.startWork') : t('pomodoro.timer.startBreak')
  }
  if (phase.value === 'idle') return t('pomodoro.timer.startWork')
  if (phaseReadyToStart.value) {
    return phase.value === 'work' ? t('pomodoro.timer.startWork') : t('pomodoro.timer.startBreak')
  }
  return paused.value ? t('pomodoro.timer.resume') : t('pomodoro.timer.pause')
})

const primaryButtonIcon = computed(() => {
  if (primaryStartWorkBlocked.value) return '✓'
  if (pendingPhase.value) return '▶'
  if (phase.value === 'idle') return '▶'
  if (phaseReadyToStart.value) return '▶'
  return paused.value ? '▶' : '⏸'
})

const showSecondaryActions = computed(() => phase.value !== 'idle' && !pendingPhase.value && !phaseReadyToStart.value)

const phaseLabel = computed(() => {
  if (pendingPhase.value === 'shortBreak' || pendingPhase.value === 'longBreak') {
    return t('pomodoro.timer.workDonePending')
  }
  if (pendingPhase.value === 'work') return t('pomodoro.timer.breakDonePending')
  const map: Record<Phase, string> = {
    idle: t('pomodoro.timer.phaseIdle'),
    work: t('pomodoro.timer.phaseWork'),
    shortBreak: t('pomodoro.timer.phaseShortBreak'),
    longBreak: t('pomodoro.timer.phaseLongBreak'),
  }
  return map[phase.value]
})

const clockText = computed(() => {
  const m = Math.floor(remainingSec.value / 60)
  const s = remainingSec.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

function isPhaseAtStart(): boolean {
  return phase.value !== 'idle' && !pendingPhase.value && !ticking.value && !paused.value && plannedSec.value > 0 && remainingSec.value >= plannedSec.value
}

const phaseReadyToStart = computed(() => isPhaseAtStart())

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
  if (v === 'RUNNING' || v === 'PAUSED' || v === 'IDLE') return v
  return 'IDLE'
}

function normalizeServerPhase(serverPhase: string | undefined): PomodoroActiveSession['phase'] {
  const v = (serverPhase ?? '').trim().toUpperCase()
  if (v === 'WORK' || v === 'SHORT_BREAK' || v === 'LONG_BREAK' || v === 'IDLE') return v
  return 'IDLE'
}

function mapServerPhase(serverPhase: PomodoroActiveSession['phase']): Phase {
  if (serverPhase === 'WORK') return 'work'
  if (serverPhase === 'SHORT_BREAK') return 'shortBreak'
  if (serverPhase === 'LONG_BREAK') return 'longBreak'
  return 'idle'
}

function mapServerPendingPhase(pending: PomodoroActiveSession['pendingPhase']): Phase | null {
  if (!pending) return null
  return mapServerPhase(pending)
}

function sessionOwner(session: PomodoroActiveSession): 'ADMIN' | 'DEVICE' {
  const raw = session.controller ?? session.source
  return raw === 'DEVICE' ? 'DEVICE' : 'ADMIN'
}

function shouldApplyRemoteSession(session: PomodoroActiveSession): boolean {
  const owner = sessionOwner(session)
  const runState = normalizeRunState(session.runState)
  const synced = Number(session.syncedAtMs) || 0
  const remoteActive = runState === 'RUNNING' || runState === 'PAUSED'
  const serverPhase = normalizeServerPhase(session.phase)

  if (owner === 'DEVICE' && runState === 'IDLE' && serverPhase === 'IDLE') {
    const localNotReset = phase.value !== 'idle' || ticking.value || paused.value || pendingPhase.value !== null || remainingSec.value !== 0
    if (localNotReset) return true
    if (controlOwner.value !== 'DEVICE') return true
  }

  if (owner === 'DEVICE' && runState === 'IDLE' && serverPhase !== 'IDLE') {
    if (ticking.value || paused.value) return true
    if (controlOwner.value !== 'DEVICE') return true
  }

  if (owner === 'DEVICE' && remoteActive) {
    if (phase.value === 'idle') return true
    if (controlOwner.value === 'ADMIN' && isPhaseAtStart()) return false
    if (controlOwner.value === 'ADMIN' && runState === 'RUNNING' && paused.value) return true
    if (controlOwner.value !== 'DEVICE' && !isPhaseAtStart()) return true
    if (paused.value !== (runState === 'PAUSED')) return true
    if (runState === 'RUNNING' && !paused.value && !ticking.value) return true
  }

  /* ── ADMIN-ADMIN 同步：仅在有意义的变更时才应用 ── */
  if (owner === 'ADMIN') {
    // 本地完全空闲（无 pending、无 ticking、无 paused）→ 始终应用远程状态
    const localCompletelyIdle = phase.value === 'idle' && !pendingPhase.value && !ticking.value && !paused.value
    if (localCompletelyIdle) return true

    // 本地活跃 → 比较状态是否真正变更
    const remotePhase = mapServerPhase(serverPhase)
    const remotePending = mapServerPendingPhase(session.pendingPhase)

    // phase 变更（包括 idle + pendingPhase → RUNNING 另一阶段：remotePhase 从 idle 变为 work/shortBreak/longBreak）
    if (remotePhase !== phase.value) return true
    // runState 变更（IDLE ↔ RUNNING ↔ PAUSED）
    const localRunState: string = paused.value ? 'PAUSED' : (ticking.value ? 'RUNNING' : 'IDLE')
    if (runState !== localRunState) return true
    // pendingPhase 变更
    if (remotePending !== pendingPhase.value) return true
    // 完成轮次变更
    if ((session.sessionWorkRounds ?? 0) !== sessionWorkRounds.value) return true
    // 计划变更
    if (session.planId && session.planId !== selectedPlanId.value) return true

    // 无有意义变更 → 跳过，避免每秒钟因 syncedAtMs 更新而重置本地 tick
    return false
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
    if (fp === lastPublishedFingerprint) return
    lastPublishedFingerprint = fp
  }

  try {
    const session = await syncActiveSession(payload)
    lastRemoteSession.value = session
    const owner = sessionOwner(session)
    const synced = Number(session.syncedAtMs) || Date.now()

    if (owner === 'DEVICE' && !takeControl) {
      if (shouldApplyRemoteSession(session)) applyRemoteSession(session)
      return
    }

    controlOwner.value = owner
    lastAppliedSyncedMs.value = synced
    serverReachable.value = true
    if (takeControl && !isPhaseAtStart()) {
      ensureLocalTickRunning()
    }
  } catch {
    serverReachable.value = false
  }
}

function applyRemoteSession(session: PomodoroActiveSession) {
  if (!shouldApplyRemoteSession(session)) return

  const owner = sessionOwner(session)
  const runState = normalizeRunState(session.runState)
  const serverPhase = normalizeServerPhase(session.phase)

  applyingRemote.value = true

  if (runState === 'IDLE') {
    clearTick()
    paused.value = false
    controlOwner.value = owner
    sessionWorkRounds.value = session.sessionWorkRounds ?? 0
    if (session.planId) selectedPlanId.value = session.planId

    const total = Math.max(1, Number(session.phaseTotalSec) || 1)
    const remaining = Math.max(0, Number(session.remainingSec) || 0)

    if (serverPhase === 'IDLE') {
      pendingPhase.value = null
      phase.value = 'idle'
      remainingSec.value = 0
    } else if (remaining <= 0) {
      // runState=IDLE 且剩余时间为0 → 阶段已完成，设为 idle + pendingPhase
      phase.value = 'idle'
      plannedSec.value = total
      remainingSec.value = 0
      const remotePending = mapServerPendingPhase(session.pendingPhase)
      if (remotePending) {
        pendingPhase.value = remotePending
      } else if (serverPhase === 'WORK' && activePlan.value) {
        const plan = activePlan.value
        pendingPhase.value = sessionWorkRounds.value % plan.roundsBeforeLongBreak === 0 ? 'longBreak' : 'shortBreak'
      } else if (serverPhase === 'SHORT_BREAK' || serverPhase === 'LONG_BREAK') {
        pendingPhase.value = 'work'
      } else {
        pendingPhase.value = null
      }
    } else {
      // runState=IDLE 但剩余时间>0 → 阶段被中断，保留 phase 以便恢复
      phase.value = mapServerPhase(serverPhase)
      plannedSec.value = total
      remainingSec.value = remaining
      pendingPhase.value = null
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

  if (session.planId) selectedPlanId.value = session.planId

  const total = Math.max(1, Number(session.phaseTotalSec) || 1)
  let remaining = Math.max(0, Number(session.remainingSec) || 0)
  if (runState === 'RUNNING' && remaining <= 0) remaining = total

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
  if (runState === 'RUNNING') startTick(false)
  applyingRemote.value = false
}

async function pullRemoteSession() {
  try {
    const session = await fetchActiveSession()
    serverReachable.value = true
    lastRemoteSession.value = session
    if (session) applyRemoteSession(session)
  } catch {
    serverReachable.value = false
  }
}

function startRemoteSync() {
  clearRemoteSync()
  void pullRemoteSession()
  remoteSyncTimer = setInterval(() => { void pullRemoteSession() }, REMOTE_SYNC_INTERVAL_MS)
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
  if (publish) void publishSession(false)
}

function ensureLocalTickRunning() {
  if (controlOwner.value === 'ADMIN' && !applyingRemote.value && !paused.value && !pendingPhase.value && phase.value !== 'idle' && !isPhaseAtStart() && !ticking.value && remainingSec.value > 0) {
    startTick(false)
  }
}

function enterPhase(next: Phase) {
  phase.value = next
  const plan = activePlan.value
  if (!plan) return
  if (next === 'work') plannedSec.value = plan.workDurationMin * 60
  else if (next === 'shortBreak') plannedSec.value = plan.shortBreakMin * 60
  else if (next === 'longBreak') plannedSec.value = plan.longBreakMin * 60
  else {
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

async function onPrimaryClick() {
  pomodoroSound.init()
  if (primaryStartWorkBlocked.value) {
    notifyTodayRoundsGoalReached()
    return
  }
  if (pendingPhase.value) {
    await startPendingPhase()
    return
  }
  if (phase.value === 'idle') {
    if (!activePlan.value) {
      ElMessage.warning(t('pomodoro.timer.noPlan'))
      return
    }
    controlOwner.value = 'ADMIN'
    enterPhase('work')
    await publishSession(true)
    return
  }
  if (phaseReadyToStart.value) {
    controlOwner.value = 'ADMIN'
    paused.value = false
    startTick(false)
    await publishSession(true)
    return
  }
  const willPause = !paused.value
  paused.value = willPause
  controlOwner.value = 'ADMIN'
  if (willPause) clearTick()
  else startTick(false)
  await publishSession(true)
  if (willPause) void pullRemoteSession()
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
  await createRecord({ planId: activePlan.value.id, recordType: type, durationSec: elapsedSec })
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
    pendingPhase.value = sessionWorkRounds.value % plan.roundsBeforeLongBreak === 0 ? 'longBreak' : 'shortBreak'
    await publishSession(true)
    if (appStore.pomodoroSoundEnabled) {
      pomodoroSound.playWorkComplete(appStore.pomodoroBeeps, appStore.pomodoroVolume)
    }
    ElMessage.success(t('pomodoro.timer.workDonePending'))
    return
  }

  if (phase.value === 'shortBreak' || phase.value === 'longBreak') {
    const type = phase.value === 'longBreak' ? 'LONG_BREAK' : 'SHORT_BREAK'
    await savePhaseRecord(type, elapsed)
    pendingPhase.value = 'work'
    await publishSession(true)
    if (appStore.pomodoroSoundEnabled) {
      pomodoroSound.playBreakComplete(appStore.pomodoroBeeps, appStore.pomodoroVolume)
    }
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
    pendingPhase.value = sessionWorkRounds.value % plan.roundsBeforeLongBreak === 0 ? 'longBreak' : 'shortBreak'
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

async function takeControl() {
  controlOwner.value = 'ADMIN'
  await publishSession(true)
}

onMounted(async () => {
  await loadPlans()
  await refreshToday()
  todayPlanKnownComplete = isTodayPlanComplete()
  lastSeenWorkRounds.value = today.value.workRounds
  skipNextPlanDoneNotify = false
  startRemoteSync()
  initReportRange()
  try {
    const plan = await fetchDefaultPlan()
    reportGoalMin.value = plan.dailyGoalMinutes
  } catch { /* use default */ }
  await loadReport()
  await loadPlanData()
  await refreshPlanEditLock()
})

onUnmounted(() => {
  clearTick()
  clearRemoteSync()
})
</script>

<style scoped lang="scss">
$pomo-bg: #08081a;
$pomo-card-bg: rgb(16 16 40 / 52%);
$pomo-border: #3d5a80;
$pomo-green: #8bc34a;
$pomo-red: #ef5350;
$pomo-blue: #5c9fd4;
$pomo-purple: #7e57c2;
$pomo-text: #e0e8f0;
$pomo-dim: #607080;
$pomo-pixel-step: 4px;

@mixin pixel-chamfer($step: $pomo-pixel-step) {
  clip-path: polygon(
    0 $step,
    $step $step,
    $step 0,
    calc(100% - #{$step}) 0,
    calc(100% - #{$step}) $step,
    100% $step,
    100% calc(100% - #{$step}),
    calc(100% - #{$step}) calc(100% - #{$step}),
    calc(100% - #{$step}) 100%,
    $step 100%,
    $step calc(100% - #{$step}),
    0 calc(100% - #{$step})
  );
}

@mixin pixel-panel-jagged-frame($frame-color) {
  --s: 4px;
  padding: var(--s);
  background: $frame-color;
  clip-path: polygon(
    0 calc(var(--s) * 4),
    var(--s) calc(var(--s) * 4),
    var(--s) calc(var(--s) * 3),
    calc(var(--s) * 2) calc(var(--s) * 3),
    calc(var(--s) * 2) calc(var(--s) * 2),
    calc(var(--s) * 3) calc(var(--s) * 2),
    calc(var(--s) * 3) var(--s),
    calc(var(--s) * 4) var(--s),
    calc(var(--s) * 4) 0,
    calc(100% - var(--s) * 4) 0,
    calc(100% - var(--s) * 4) var(--s),
    calc(100% - var(--s) * 3) var(--s),
    calc(100% - var(--s) * 3) calc(var(--s) * 2),
    calc(100% - var(--s) * 2) calc(var(--s) * 2),
    calc(100% - var(--s) * 2) calc(var(--s) * 3),
    calc(100% - var(--s)) calc(var(--s) * 3),
    calc(100% - var(--s)) calc(var(--s) * 4),
    100% calc(var(--s) * 4),
    100% calc(100% - var(--s) * 4),
    calc(100% - var(--s)) calc(100% - var(--s) * 4),
    calc(100% - var(--s)) calc(100% - var(--s) * 3),
    calc(100% - var(--s) * 2) calc(100% - var(--s) * 3),
    calc(100% - var(--s) * 2) calc(100% - var(--s) * 2),
    calc(100% - var(--s) * 3) calc(100% - var(--s) * 2),
    calc(100% - var(--s) * 3) calc(100% - var(--s)),
    calc(100% - var(--s) * 4) calc(100% - var(--s)),
    calc(100% - var(--s) * 4) 100%,
    calc(var(--s) * 4) 100%,
    calc(var(--s) * 4) calc(100% - var(--s)),
    calc(var(--s) * 3) calc(100% - var(--s)),
    calc(var(--s) * 3) calc(100% - var(--s) * 2),
    calc(var(--s) * 2) calc(100% - var(--s) * 2),
    calc(var(--s) * 2) calc(100% - var(--s) * 3),
    var(--s) calc(100% - var(--s) * 3),
    var(--s) calc(100% - var(--s) * 4),
    0 calc(100% - var(--s) * 4)
  );
  box-shadow: 0 6px 0 rgb(0 0 0 / 35%);
}

@mixin pixel-panel-jagged-inner {
  background: rgb(16 16 40 / 26%);
  clip-path: polygon(
    0 calc(var(--s) * 4),
    var(--s) calc(var(--s) * 4),
    var(--s) calc(var(--s) * 3),
    calc(var(--s) * 2) calc(var(--s) * 3),
    calc(var(--s) * 2) calc(var(--s) * 2),
    calc(var(--s) * 3) calc(var(--s) * 2),
    calc(var(--s) * 3) var(--s),
    calc(var(--s) * 4) var(--s),
    calc(var(--s) * 4) 0,
    calc(100% - var(--s) * 4) 0,
    calc(100% - var(--s) * 4) var(--s),
    calc(100% - var(--s) * 3) var(--s),
    calc(100% - var(--s) * 3) calc(var(--s) * 2),
    calc(100% - var(--s) * 2) calc(var(--s) * 2),
    calc(100% - var(--s) * 2) calc(var(--s) * 3),
    calc(100% - var(--s)) calc(var(--s) * 3),
    calc(100% - var(--s)) calc(var(--s) * 4),
    100% calc(var(--s) * 4),
    100% calc(100% - var(--s) * 4),
    calc(100% - var(--s)) calc(100% - var(--s) * 4),
    calc(100% - var(--s)) calc(100% - var(--s) * 3),
    calc(100% - var(--s) * 2) calc(100% - var(--s) * 3),
    calc(100% - var(--s) * 2) calc(100% - var(--s) * 2),
    calc(100% - var(--s) * 3) calc(100% - var(--s) * 2),
    calc(100% - var(--s) * 3) calc(100% - var(--s)),
    calc(100% - var(--s) * 4) calc(100% - var(--s)),
    calc(100% - var(--s) * 4) 100%,
    calc(var(--s) * 4) 100%,
    calc(var(--s) * 4) calc(100% - var(--s)),
    calc(var(--s) * 3) calc(100% - var(--s)),
    calc(var(--s) * 3) calc(100% - var(--s) * 2),
    calc(var(--s) * 2) calc(100% - var(--s) * 2),
    calc(var(--s) * 2) calc(100% - var(--s) * 3),
    var(--s) calc(100% - var(--s) * 3),
    var(--s) calc(100% - var(--s) * 4),
    0 calc(100% - var(--s) * 4)
  );
}

@mixin pomo-pixel-digits {
  font-family: 'Press Start 2P', 'Courier New', Consolas, monospace;
  font-weight: 400;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.08em;
  line-height: 1.4;
  -webkit-font-smoothing: none;
  -moz-osx-font-smoothing: unset;
}

.v2-pomodoro-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  image-rendering: pixelated;
}

.v2-pomodoro-page__bg {
  position: fixed;
  inset: 0;
  background: $pomo-bg;
  z-index: 0;
}

.v2-pomodoro-page__content {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-bottom: env(safe-area-inset-bottom);
}


.v2-pomodoro-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  gap: 12px;
  overflow-y: auto;
}

.v2-pomodoro-sync {
  @include pixel-panel-jagged-frame(rgb(61 90 128 / 72%));

  &__inner {
    @include pixel-panel-jagged-inner;
    padding: 10px 12px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  &__left,
  &__right {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    flex: 1;
  }

  &__center {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    flex: 1.5;
  }

  &__label {
    font-size: 11px;
    color: $pomo-dim;
  }

  &__value {
    font-size: 13px;
    font-weight: 700;

    &--control {
      color: $pomo-red;
    }
  }

  &__health {
    font-size: 12px;
    font-weight: 800;
    color: $pomo-green;
    white-space: nowrap;
  }

  &__wifi {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 11px;
    color: $pomo-dim;

    &.is-on {
      color: $pomo-blue;
    }
  }

  &__badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px 2px 5px;
    font-size: 12px;
    font-weight: 700;
    color: $pomo-dim;
    background: rgb(20 28 48 / 42%);
    border: 2px solid #2a3a50;
    @include pixel-chamfer(4px);

    &.is-on {
      color: $pomo-green;
      border-color: rgb(139 195 74 / 45%);
    }
  }

  &__dot {
    width: 6px;
    height: 6px;
    background: #506070;

    .v2-pomodoro-sync__badge.is-on & {
      background: $pomo-green;
      box-shadow: 0 0 4px $pomo-green;
    }
  }
}

.v2-pomodoro-main {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.v2-pomodoro-ring-wrap {
  width: 100%;
  max-width: 280px;
  aspect-ratio: 1;
}

.v2-pomodoro-phase-pills {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: center;
}

.v2-pomodoro-phase-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  @include pixel-chamfer(4px);
  font-size: 11px;
  font-weight: 700;
  border: 2px solid transparent;
  opacity: 0.5;
  cursor: default;
  color: inherit;
  background: transparent;
  box-shadow: 2px 2px 0 rgb(0 0 0 / 25%);

  &.is-active {
    opacity: 1;
  }

  &--work {
    color: #ffcdd2;
    background: rgb(239 83 80 / 15%);
    border-color: rgb(239 83 80 / 45%);

    &.is-active {
      background: rgb(239 83 80 / 28%);
    }
  }

  &--short {
    color: #dcedc8;
    background: rgb(139 195 74 / 12%);
    border-color: rgb(139 195 74 / 40%);

    &.is-active {
      background: rgb(139 195 74 / 25%);
    }
  }

  &--long {
    color: #d1c4e9;
    background: rgb(126 87 194 / 12%);
    border-color: rgb(126 87 194 / 40%);

    &.is-active {
      background: rgb(126 87 194 / 25%);
    }
  }
}

.v2-pomodoro-actions {
  display: flex;
  gap: 12px;
}

.v2-pomodoro-action {
  font-size: 12px;
  font-weight: 600;
  color: $pomo-blue;
  background: none;
  border: none;
  cursor: pointer;
  border-bottom: 2px solid currentColor;
  line-height: 1.4;

  &--warn {
    color: #ffb74d;
  }

  &:active {
    opacity: 0.7;
  }
}

.v2-pomodoro-side {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.v2-pomodoro-progress {
  @include pixel-panel-jagged-frame(rgb(61 90 128 / 72%));

  &__inner {
    @include pixel-panel-jagged-inner;
  }

  &__title {
    margin: 0 0 6px;
    text-align: center;
    font-size: 13px;
    font-weight: 700;
    color: $pomo-green;
    letter-spacing: 1px;
  }

  &__caption {
    margin: 0 0 6px;
    font-size: 11px;
    color: $pomo-dim;
    text-align: center;
  }

  &__dots {
    display: flex;
    flex-direction: column;
    gap: 6px;
    margin: 6px 0;
  }

  &__dot-row {
    display: flex;
    justify-content: center;
    gap: 6px;
  }

  &__dot {
    width: 12px;
    height: 12px;
    background: #2a2a45;
    border: 2px solid #404060;
    transition: background 0.15s;

    &.is-filled {
      background: $pomo-green;
      border-color: #a5d66a;
      box-shadow: 2px 2px 0 rgb(0 0 0 / 35%);
    }
  }

  &__today {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    margin: 6px 0 0;
    font-size: 12px;
    color: $pomo-dim;
  }

  &__big-num {
    @include pomo-pixel-digits;
    font-size: 24px;
    line-height: 1;
    color: $pomo-green;
    text-shadow: 2px 2px 0 #1a3a10;
  }
}

.v2-pomodoro-start {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  min-height: 48px;
  padding: 10px 14px;
  border: 3px solid #6a9e3a;
  @include pixel-chamfer(5px);
  font-size: 15px;
  font-weight: 800;
  color: #1a2a10;
  background: $pomo-green;
  box-shadow: 0 6px 0 #4a7a28, 6px 6px 0 rgb(0 0 0 / 35%);
  cursor: pointer;
  transition: transform 0.1s, box-shadow 0.1s;
  image-rendering: pixelated;

  &:active:not(:disabled) {
    transform: translate(2px, 2px);
    box-shadow: 0 2px 0 #4a7a28, 2px 2px 0 rgb(0 0 0 / 30%);
  }

  &:disabled,
  &.is-goal-blocked {
    opacity: 0.45;
    cursor: not-allowed;
  }

  &__icon {
    font-size: 13px;
  }
}

.v2-pomodoro-alert {
  :deep(.el-alert) {
    background: rgb(139 195 74 / 8%);
    border: 1px solid rgb(139 195 74 / 35%);
    border-radius: 0;
  }

  :deep(.el-alert__content) {
    font-size: 12px;
    color: $pomo-text;
  }
}

.v2-pomodoro-subpage {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
  overflow-y: auto;

  &__empty {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
  }

  &__empty-icon {
    font-size: 48px;
    opacity: 0.6;
  }

  &__empty-text {
    margin: 0;
    font-size: 14px;
    color: $pomo-dim;
  }
}

.v2-pomodoro-records__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.v2-pomodoro-records__stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 8px;
  @include pixel-panel-jagged-frame(rgb(61 90 128 / 72%));

  &__inner {
    @include pixel-panel-jagged-inner;
  }
}

.v2-pomodoro-records__stat-value {
  @include pomo-pixel-digits;
  font-size: 28px;
  color: $pomo-green;
  text-shadow: 2px 2px 0 #1a3a10;
}

.v2-pomodoro-records__stat-label {
  font-size: 11px;
  color: $pomo-dim;
  text-align: center;
}

.v2-pomodoro-items-page {
  padding: 16px;
}

.v2-pomodoro-plan-card {
  @include pixel-panel-jagged-frame(rgb(92 159 212 / 68%));

  &__title {
    margin: 0 0 6px;
    font-size: 13px;
    font-weight: 700;
    color: $pomo-text;
  }

  &__name {
    margin: 0 0 6px;
    font-size: 15px;
    font-weight: 700;
    color: $pomo-text;
  }

  &__durations {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    margin: 0;
    @include pomo-pixel-digits;
    font-size: 16px;
    line-height: 1.45;
  }

  &__sep {
    color: $pomo-dim;
    font-size: 14px;
  }

  &__dur {
    &--work { color: $pomo-red; }
    &--short { color: $pomo-green; }
    &--long { color: #29b6f6; }
  }
}

.v2-pomodoro-start-bar {
  flex-shrink: 0;
  padding: 8px 16px;
  padding-bottom: 4px;
}

.v2-pomodoro-nav {
  position: relative;
  z-index: 2;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  padding-bottom: calc(8px + env(safe-area-inset-bottom));
  border-top: 2px solid #2a2a50;
  background: rgb(8 8 26 / 72%);

  &__items {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
  }

  &__item {
    --s: 3px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 3px;
    min-width: 72px;
    padding: 8px 12px;
    border: 2px solid transparent;
    background: transparent;
    color: #8090a8;
    cursor: pointer;
    transition: color 0.15s, border-color 0.15s, background 0.15s;
    clip-path: polygon(
      0 calc(var(--s) * 3), var(--s) calc(var(--s) * 3), var(--s) calc(var(--s) * 2),
      calc(var(--s) * 2) calc(var(--s) * 2), calc(var(--s) * 2) var(--s),
      calc(var(--s) * 3) var(--s), calc(var(--s) * 3) 0,
      calc(100% - var(--s) * 3) 0, calc(100% - var(--s) * 3) var(--s),
      calc(100% - var(--s) * 2) var(--s), calc(100% - var(--s) * 2) calc(var(--s) * 2),
      calc(100% - var(--s)) calc(var(--s) * 2), calc(100% - var(--s)) calc(var(--s) * 3),
      100% calc(var(--s) * 3), 100% calc(100% - var(--s) * 3),
      calc(100% - var(--s)) calc(100% - var(--s) * 3),
      calc(100% - var(--s)) calc(100% - var(--s) * 2),
      calc(100% - var(--s) * 2) calc(100% - var(--s) * 2),
      calc(100% - var(--s) * 2) calc(100% - var(--s)),
      calc(100% - var(--s) * 3) calc(100% - var(--s)),
      calc(100% - var(--s) * 3) 100%, calc(var(--s) * 3) 100%,
      calc(var(--s) * 3) calc(100% - var(--s)),
      calc(var(--s) * 2) calc(100% - var(--s)),
      calc(var(--s) * 2) calc(100% - var(--s) * 2),
      var(--s) calc(100% - var(--s) * 2), var(--s) calc(100% - var(--s) * 3),
      0 calc(100% - var(--s) * 3)
    );

    &.is-active {
      color: $pomo-green;
      border-color: rgb(139 195 74 / 72%);
      background: rgb(139 195 74 / 10%);
    }

    &:active {
      color: #c0d0e8;
    }
  }

  &__icon {
    font-size: 20px;
    line-height: 1;
  }

  &__label {
    font-size: 12px;
    font-weight: 700;
  }
}

.pixel-spark {
  color: $pomo-green;
  opacity: 0.85;
}

.v2-report {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &__toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
    padding: 10px 12px;
    background: rgb(8 8 26 / 60%);
    border-bottom: 2px solid $pomo-border;
  }

  &__date-picker {
    flex: 1;
    min-width: 0;

    :deep(.el-input__wrapper) {
      background: transparent;
      box-shadow: none;
      padding: 0 4px;
    }

    :deep(.el-input__inner) {
      color: $pomo-text;
      font-size: 12px;
      font-weight: 600;
    }

    :deep(.el-range-separator) {
      color: $pomo-dim;
      font-size: 11px;
    }

    :deep(.el-range-input) {
      color: $pomo-text;
      font-size: 12px;
      font-weight: 600;
    }

    :deep(.el-input__icon) {
      color: $pomo-dim;
    }
  }

  &__query {
    --s: 2px;
    flex-shrink: 0;
    padding: var(--s);
    border: none;
    background: #6a9e3a;
    @include pixel-chamfer(3px);
    cursor: pointer;
    color: $pomo-green;
    font-size: 12px;
    font-weight: 700;
    transition: filter 0.15s;

    &:hover:not(:disabled) {
      filter: brightness(1.08);
    }

    &:disabled {
      opacity: 0.5;
      cursor: wait;
    }
  }

  &__body {
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 10px 12px;
  }

  &__kpis {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
    flex-shrink: 0;
  }

  &__kpi-card {
    text-align: center;
    padding: 10px 4px;
    @include pixel-panel-jagged-frame(rgb(61 90 128 / 52%));
  }

  &__kpi-label {
    margin: 0 0 4px;
    font-size: 10px;
    color: $pomo-dim;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__kpi-value {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    letter-spacing: 1px;

    &--red { color: $pomo-red; text-shadow: 1px 1px 0 rgb(80 20 20 / 50%); }
    &--green { color: $pomo-green; text-shadow: 1px 1px 0 rgb(20 50 10 / 50%); }
    &--blue { color: $pomo-blue; text-shadow: 1px 1px 0 rgb(20 30 60 / 50%); }
    &--cyan { color: #29b6f6; text-shadow: 1px 1px 0 rgb(10 40 60 / 50%); }
  }

  &__section-title {
    margin: 0 0 10px;
    text-align: center;
    font-size: 13px;
    font-weight: 700;
    color: $pomo-green;
    letter-spacing: 0.5px;
  }

  &__state {
    text-align: center;
    padding: 16px 8px;
    font-size: 12px;
    color: $pomo-dim;
  }

  &__chart {
    flex-shrink: 0;
    @include pixel-panel-jagged-frame(rgb(61 90 128 / 42%));
    padding: 10px 12px;
  }

  &__bar-chart {
    display: flex;
    align-items: flex-end;
    justify-content: center;
    gap: clamp(4px, 1.2vw, 10px);
    min-height: 120px;
    padding: 4px 2px;
    overflow-x: auto;
  }

  &__bar-col {
    display: flex;
    flex-direction: column;
    align-items: center;
    flex: 1;
    min-width: 22px;
    max-width: 36px;
    cursor: default;
  }

  &__bar-stack {
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    align-items: center;
    width: 100%;
    height: 100px;
  }

  &__bar {
    display: flex;
    flex-direction: column-reverse;
    align-items: stretch;
    width: 100%;
    max-width: 28px;
    gap: 2px;
  }

  &__bar-block {
    --s: 2px;
    display: block;
    width: 100%;
    height: 8px;
    background: $pomo-blue;
    @include pixel-chamfer(2px);
    box-shadow: inset 0 -2px 0 rgb(255 255 255 / 14%), 1px 1px 0 rgb(0 0 0 / 28%);

    &:last-child {
      --s: 2px;
      height: 10px;
      background: #6eb0e8;
      box-shadow: inset 0 -2px 0 rgb(255 255 255 / 18%), 1px 1px 0 rgb(0 0 0 / 32%);
    }
  }

  &__bar-label {
    margin-top: 4px;
    font-size: 10px;
    color: $pomo-dim;
    white-space: nowrap;
  }

  &__table-wrap {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    @include pixel-panel-jagged-frame(rgb(61 90 128 / 42%));
    padding: 10px 12px;
  }

  &__table-scroll {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
  }

  &__table {
    width: 100%;
    border-collapse: collapse;
    font-size: 12px;

    th, td {
      padding: 10px 6px;
      text-align: left;
      vertical-align: middle;
      border-bottom: 1px solid rgb(61 90 128 / 40%);
    }

    th {
      color: $pomo-dim;
      font-weight: 700;
      font-size: 11px;
      white-space: nowrap;
      padding-top: 6px;
      padding-bottom: 6px;
    }

    tbody tr:last-child td {
      border-bottom: none;
    }
  }

  &__th-progress {
    min-width: 80px;
  }

  &__td-num {
    font-weight: 700;
    &--red {
      font-size: 18px;
      line-height: 1;
      color: $pomo-red;
      text-shadow: 1px 1px 0 rgb(80 20 20 / 45%);
    }
  }

  &__td-state {
    text-align: center;
    color: $pomo-dim;
    padding: 16px !important;
  }

  &__pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    flex-shrink: 0;
    margin-top: 8px;
    padding-top: 4px;
  }

  &__page-info {
    font-size: 11px;
    font-weight: 700;
    color: $pomo-dim;
    min-width: 72px;
    text-align: center;
  }

  &__page-btn {
    --s: 2px;
    min-width: 60px;
    padding: 4px 10px;
    border: none;
    font-size: 11px;
    font-weight: 700;
    color: $pomo-text;
    background: rgb(61 90 128 / 55%);
    cursor: pointer;
    @include pixel-chamfer(2px);

    &:hover:not(:disabled) {
      color: $pomo-green;
      background: rgb(61 90 128 / 72%);
    }

    &:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  }
}

.pixel-progress-bar {
  --s: 2px;
  height: 14px;
  padding: 2px;
  background: rgb(8 8 26 / 60%);
  @include pixel-chamfer(2px);

  &__fill {
    --s: 2px;
    display: block;
    height: 100%;
    min-width: 0;
    background: $pomo-red;
    @include pixel-chamfer(2px);
    box-shadow: inset 0 -2px 0 rgb(255 255 255 / 18%);
    transition: width 0.25s ease-out;
  }
}

.v2-plan {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &__lock {
    flex-shrink: 0;
    padding: 8px 12px;
    text-align: center;
    font-size: 12px;
    font-weight: 700;
    color: #ffb74d;
    @include pixel-panel-jagged-frame(rgb(61 90 128 / 52%));
  }

  &__toolbar {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
    padding: 8px 12px;
    background: rgb(8 8 26 / 50%);
    border-bottom: 2px solid $pomo-border;
  }

  &__header {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
    padding: 10px 12px;
    background: rgb(8 8 26 / 50%);
    border-bottom: 2px solid $pomo-border;
    cursor: pointer;
    user-select: none;
    transition: background 0.12s;

    &:active {
      background: rgb(8 8 26 / 70%);
    }
  }

  &__header-arrow {
    font-size: 10px;
    color: $pomo-green;
    width: 14px;
    text-align: center;
  }

  &__header-label {
    flex: 1;
    font-size: 13px;
    font-weight: 700;
    color: $pomo-text;
  }

  &__header-count {
    font-size: 11px;
    font-weight: 700;
    color: $pomo-dim;
    padding: 2px 8px;
    @include pixel-chamfer(2px);
    background: rgb(61 90 128 / 42%);
  }

  &__toolbar-bar {
    flex-shrink: 0;
    display: flex;
    gap: 8px;
    padding: 8px 12px;
    padding-bottom: 4px;
  }

  &__btn {
    --s: 2px;
    flex: 1;
    padding: var(--s);
    border: none;
    cursor: pointer;
    font-size: 12px;
    font-weight: 700;
    transition: filter 0.15s;

    &:hover:not(:disabled) { filter: brightness(1.08); }
    &:disabled { opacity: 0.5; cursor: wait; }

    &--green {
      background: #6a9e3a;
      @include pixel-chamfer(2px);
      span { display: block; padding: 6px 10px; background: rgb(20 48 20 / 92%); color: $pomo-green; @include pixel-chamfer(2px); }
    }
    &--blue {
      background: rgb(61 90 128 / 88%);
      @include pixel-chamfer(2px);
      span { display: block; padding: 6px 10px; background: rgb(16 16 40 / 55%); color: $pomo-blue; @include pixel-chamfer(2px); }
    }
  }

  &__list {
    flex: 0 0 auto;
    max-height: 240px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 10px 12px;
  }

  &__state {
    padding: 20px 8px;
    text-align: center;
    font-size: 12px;
    color: $pomo-dim;
  }

  &__list-item {
    --s: 2px;
    flex-shrink: 0;
    padding: var(--s);
    border: none;
    text-align: left;
    width: 100%;
    background: rgb(61 90 128 / 42%);
    @include pixel-chamfer(2px);
    cursor: pointer;
    transition: filter 0.12s;

    &:hover { filter: brightness(1.06); }

    &.is-selected {
      background: rgb(139 195 74 / 78%);
      box-shadow: 0 3px 0 rgb(0 0 0 / 32%);
    }
  }

  &__list-title {
    margin: 0 0 2px;
    font-size: 13px;
    font-weight: 700;
    color: $pomo-text;
    padding: 6px 8px;
    @include pixel-chamfer(2px);
    background: rgb(16 16 40 / 38%);
  }

  &__list-meta {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 8px 6px;
  }

  &__list-durs {
    @include pomo-pixel-digits;
    font-size: 13px;
    line-height: 1.3;
    color: $pomo-dim;
  }

  &__tag {
    --s: 2px;
    display: inline-block;
    padding: 1px 5px;
    font-size: 10px;
    font-weight: 800;
    color: $pomo-green;
    background: rgb(20 48 20 / 88%);
    @include pixel-chamfer(2px);
  }

  &__detail {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    margin: 8px 12px 8px;
    @include pixel-panel-jagged-frame(rgb(92 159 212 / 68%));
    padding: 12px;
  }

  &__detail-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
  }

  &__detail-title {
    margin: 0;
    font-size: 16px;
    font-weight: 800;
    color: $pomo-text;
  }

  &__detail-durs {
    margin: 0 0 10px;
    @include pomo-pixel-digits;
    font-size: 22px;
    line-height: 1.2;
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
  }

  &__dur--work { color: $pomo-red; text-shadow: 1px 1px 0 rgb(80 20 20 / 45%); }
  &__dur--short { color: $pomo-green; text-shadow: 1px 1px 0 rgb(20 50 10 / 45%); }
  &__dur--long { color: #29b6f6; text-shadow: 1px 1px 0 rgb(10 40 60 / 45%); }
  &__dur-sep { color: $pomo-text; font-size: 16px; }

  &__detail-fields {
    margin-bottom: 10px;
  }

  &__detail-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    padding: 8px 2px;
    border-bottom: 1px solid rgb(61 90 128 / 40%);
    font-size: 12px;

    &:last-child { border-bottom: none; }
  }

  &__detail-label {
    color: $pomo-text;
    font-weight: 600;
    flex-shrink: 0;
  }

  &__detail-val {
    font-weight: 700;
    text-align: right;

    &--work { @include pomo-pixel-digits; color: $pomo-red; font-size: 15px; }
    &--short { @include pomo-pixel-digits; color: $pomo-green; font-size: 15px; }
    &--long { @include pomo-pixel-digits; color: #29b6f6; font-size: 15px; }
    &--cyan { @include pomo-pixel-digits; color: #29b6f6; font-size: 14px; }
    &--blue { @include pomo-pixel-digits; color: $pomo-blue; font-size: 14px; }
  }

  &__detail-actions {
    display: flex;
    gap: 8px;
  }

  &__act {
    --s: 2px;
    flex: 1;
    padding: var(--s);
    border: none;
    cursor: pointer;
    font-size: 11px;
    font-weight: 700;
    transition: filter 0.12s;

    &:hover:not(:disabled) { filter: brightness(1.08); }
    &:disabled { opacity: 0.42; cursor: not-allowed; }

    &--green {
      background: #6a9e3a;
      @include pixel-chamfer(2px);
      span { display: block; padding: 4px 8px; background: rgb(20 48 20 / 92%); color: $pomo-green; @include pixel-chamfer(2px); }
    }
    &--blue {
      background: rgb(61 90 128 / 88%);
      @include pixel-chamfer(2px);
      span { display: block; padding: 4px 8px; background: rgb(16 16 40 / 55%); color: $pomo-blue; @include pixel-chamfer(2px); }
    }
    &--danger {
      background: rgb(120 50 50 / 75%);
      @include pixel-chamfer(2px);
      span { display: block; padding: 4px 8px; background: rgb(40 16 16 / 88%); color: #ef9a9a; @include pixel-chamfer(2px); }
    }
  }
}

.v2-plan-dialog {
  --el-dialog-bg-color: transparent;
  --el-dialog-box-shadow: none;
  --el-dialog-padding-primary: 0;
  border-radius: 0 !important;
  background: transparent !important;
  box-shadow: none !important;

  .el-dialog__header { display: none; }
  .el-dialog__body { padding: 0; }
  .el-dialog__footer { padding: 0; }

  &__frame {
    --s: 3px;
    padding: var(--s);
    background: rgb(92 159 212 / 72%);
    @include pixel-chamfer(3px);
    box-shadow: 0 6px 0 rgb(0 0 0 / 40%);
    padding: 14px 16px;
    background: rgb(12 12 32 / 96%);
  }

  &__title {
    margin: 0 0 12px;
    text-align: center;
    font-size: 14px;
    font-weight: 800;
    color: $pomo-green;
  }

  &__form {
    :deep(.el-form-item__label) {
      color: $pomo-dim;
      font-weight: 600;
      font-size: 12px;
    }

    :deep(.el-input__wrapper),
    :deep(.el-input-number) {
      --el-input-bg-color: rgb(16 16 40 / 55%);
      --el-input-border-color: rgb(61 90 128 / 55%);
      --el-input-text-color: #{$pomo-text};
      --el-input-hover-border-color: rgb(92 159 212 / 65%);
      --el-input-focus-border-color: #{$pomo-blue};
      border-radius: 0;
      box-shadow: none;
    }

    :deep(.el-switch.is-checked .el-switch__core) {
      background-color: $pomo-green;
      border-color: #6a9e3a;
    }
  }

  &__calc {
    font-size: 14px;
    font-weight: 700;
    color: $pomo-text;
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 14px;
    padding-top: 10px;
    border-top: 2px solid rgb(61 90 128 / 40%);

    .v2-plan__act {
      flex: 0 1 auto;
      min-width: 72px;
    }
  }
}
</style>