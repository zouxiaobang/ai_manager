<template>
  <div class="report-panel report-panel--pixel">
    <header class="report-panel__toolbar">
      <div class="report-panel__range-wrap pixel-panel-jagged">
        <div class="pixel-panel-jagged__inner report-panel__range-inner">
          <el-date-picker
            v-model="dateRange"
            class="report-panel__date-picker"
            type="daterange"
            value-format="YYYY-MM-DD"
            :start-placeholder="t('pomodoro.report.start')"
            :end-placeholder="t('pomodoro.report.end')"
            :teleported="true"
          />
        </div>
      </div>
      <button
        type="button"
        class="report-panel__query"
        :disabled="loading"
        @click="loadReport"
      >
        <span class="report-panel__query__inner">{{ t('pomodoro.report.query') }}</span>
      </button>
    </header>

    <div class="report-panel__kpis">
      <div
        v-for="kpi in kpiItems"
        :key="kpi.key"
        class="report-kpi pixel-panel-jagged"
      >
        <div class="pixel-panel-jagged__inner report-kpi__inner">
          <p class="report-kpi__label">{{ kpi.label }}</p>
          <p class="report-kpi__value" :class="`report-kpi__value--${kpi.tone}`">{{ kpi.value }}</p>
        </div>
      </div>
    </div>

    <div class="report-panel__chart pixel-panel-jagged">
      <div class="pixel-panel-jagged__inner report-panel__chart-inner">
        <h3 class="report-panel__section-title">
          <span class="pixel-spark">✦</span>
          {{ t('pomodoro.report.chartRounds') }}
          <span class="pixel-spark">✦</span>
        </h3>
        <div v-if="loading" class="report-panel__loading">{{ t('pomodoro.report.loading') }}</div>
        <div v-else-if="dailyChronological.length === 0" class="report-panel__empty">
          {{ t('pomodoro.report.noData') }}
        </div>
        <div v-else class="pixel-bar-chart">
          <div
            v-for="row in dailyChronological"
            :key="row.statDate"
            class="pixel-bar-chart__col"
            :title="barHoverLabel(row.workRounds)"
          >
            <div class="pixel-bar-chart__tooltip" aria-hidden="true">
              {{ barHoverLabel(row.workRounds) }}
            </div>
            <div class="pixel-bar-chart__bar-stack">
              <div class="pixel-bar-chart__bar">
                <span
                  v-for="bi in barBlockCount(row.workRounds)"
                  :key="bi"
                  class="pixel-bar-chart__block"
                />
              </div>
            </div>
            <span class="pixel-bar-chart__label">{{ formatChartDate(row.statDate) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="report-panel__table-wrap pixel-panel-jagged">
      <div class="pixel-panel-jagged__inner report-panel__table-inner">
        <h3 class="report-panel__section-title">
          <span class="pixel-spark">✦</span>
          {{ t('pomodoro.report.dailyTable') }}
          <span class="pixel-spark">✦</span>
        </h3>
        <div class="report-table-scroll">
          <table class="report-table">
            <thead>
              <tr>
                <th>{{ t('pomodoro.report.date') }}</th>
                <th>{{ t('pomodoro.report.rounds') }}</th>
                <th>{{ t('pomodoro.report.workMin') }}</th>
                <th>{{ t('pomodoro.report.breakMin') }}</th>
                <th class="report-table__progress-col">{{ t('pomodoro.report.workBar') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="5" class="report-table__state">{{ t('pomodoro.report.loading') }}</td>
              </tr>
              <tr v-else-if="dailyChronological.length === 0">
                <td colspan="5" class="report-table__state">{{ t('pomodoro.report.noData') }}</td>
              </tr>
              <tr v-for="row in dailyPaged" v-else :key="row.statDate">
                <td>{{ formatTableDate(row.statDate) }}</td>
                <td class="report-table__num report-table__num--red">{{ row.workRounds }}</td>
                <td>{{ row.workMinutes }}m</td>
                <td>{{ row.breakMinutes }}m</td>
                <td>
                  <div class="pixel-progress">
                    <div
                      class="pixel-progress__fill"
                      :style="{ width: `${barPercent(row.workMinutes)}%` }"
                    />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <nav
          v-if="!loading && dailyChronological.length > TABLE_PAGE_SIZE"
          class="report-pagination"
          :aria-label="t('pomodoro.report.pagination')"
        >
          <button
            type="button"
            class="report-pagination__btn"
            :disabled="tablePage <= 1"
            @click="tablePage -= 1"
          >
            {{ t('pomodoro.report.prevPage') }}
          </button>
          <span class="report-pagination__info">
            {{ t('pomodoro.report.pageIndicator', { page: tablePage, total: tablePageCount }) }}
          </span>
          <button
            type="button"
            class="report-pagination__btn"
            :disabled="tablePage >= tablePageCount"
            @click="tablePage += 1"
          >
            {{ t('pomodoro.report.nextPage') }}
          </button>
        </nav>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  fetchDailyStats,
  fetchDefaultPlan,
  fetchSummary,
  type PomodoroDailyStat,
  type PomodoroSummary,
} from '@/api/pomodoro'
import { addDays, formatDateParam } from '@/utils/date'

const { t } = useI18n()
const loading = ref(false)
const dateRange = ref<[string, string] | null>(null)
const daily = ref<PomodoroDailyStat[]>([])
const summary = ref<PomodoroSummary | null>(null)
const goalMinutes = ref(200)

const TABLE_PAGE_SIZE = 10
const tablePage = ref(1)

const dailyChronological = computed(() =>
  [...daily.value].sort((a, b) => a.statDate.localeCompare(b.statDate)),
)

const tablePageCount = computed(() =>
  Math.max(1, Math.ceil(dailyChronological.value.length / TABLE_PAGE_SIZE)),
)

const dailyPaged = computed(() => {
  const start = (tablePage.value - 1) * TABLE_PAGE_SIZE
  return dailyChronological.value.slice(start, start + TABLE_PAGE_SIZE)
})

const maxRounds = computed(() => {
  const vals = dailyChronological.value.map((r) => r.workRounds)
  return Math.max(1, ...vals, 1)
})

const BAR_STACK_H = 120
const BAR_BLOCK_H = 8
const BAR_BLOCK_GAP = 2

const kpiItems = computed(() => [
  {
    key: 'rounds',
    label: t('pomodoro.report.totalRoundsShort'),
    value: String(summary.value?.totalWorkRounds ?? 0),
    tone: 'red',
  },
  {
    key: 'minutes',
    label: t('pomodoro.report.totalMinutesShort'),
    value: String(summary.value?.totalWorkMinutes ?? 0),
    tone: 'green',
  },
  {
    key: 'days',
    label: t('pomodoro.report.activeDays'),
    value: String(summary.value?.activeDays ?? 0),
    tone: 'blue',
  },
  {
    key: 'avg',
    label: t('pomodoro.report.avgMinutesShort'),
    value: String(Math.round(summary.value?.avgWorkMinutesPerDay ?? 0)),
    tone: 'cyan',
  },
])

function initRange() {
  const end = new Date()
  const start = addDays(end, -6)
  dateRange.value = [formatDateParam(start), formatDateParam(end)]
}

function formatChartDate(statDate: string): string {
  const parts = statDate.split('-')
  if (parts.length >= 3) return `${parts[1]}-${parts[2]}`
  return statDate
}

function formatTableDate(statDate: string): string {
  return formatChartDate(statDate)
}

function barBlockCount(rounds: number): number {
  if (rounds <= 0) return 0
  const heightPx = Math.max(
    BAR_BLOCK_H,
    Math.round((rounds / maxRounds.value) * BAR_STACK_H),
  )
  return Math.ceil(heightPx / (BAR_BLOCK_H + BAR_BLOCK_GAP))
}

function barHoverLabel(rounds: number): string {
  return t('pomodoro.report.chartBarRounds', { n: rounds })
}

function barPercent(workMinutes: number) {
  if (goalMinutes.value <= 0) return 0
  return Math.min(100, Math.round((workMinutes / goalMinutes.value) * 100))
}

async function loadReport() {
  if (!dateRange.value || dateRange.value.length !== 2) return
  const [startDate, endDate] = dateRange.value
  loading.value = true
  try {
    const [dailyData, summaryData] = await Promise.all([
      fetchDailyStats(startDate, endDate),
      fetchSummary(startDate, endDate),
    ])
    daily.value = dailyData
    summary.value = summaryData
    tablePage.value = 1
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  initRange()
  try {
    const plan = await fetchDefaultPlan()
    goalMinutes.value = plan.dailyGoalMinutes
  } catch {
    /* use default */
  }
  await loadReport()
})

defineExpose({ loadReport })
</script>

<style scoped lang="scss">
@use './pomodoro-pixel-report.scss';
</style>
