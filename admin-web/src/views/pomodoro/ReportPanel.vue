<template>
  <div>
    <div class="panel-toolbar">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        :start-placeholder="t('pomodoro.report.start')"
        :end-placeholder="t('pomodoro.report.end')"
      />
      <el-button type="primary" :loading="loading" @click="loadReport">
        {{ t('pomodoro.report.query') }}
      </el-button>
    </div>

    <el-row :gutter="16" class="summary-row">
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card__label">{{ t('pomodoro.report.totalRounds') }}</div>
          <div class="stat-card__value">{{ summary?.totalWorkRounds ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card__label">{{ t('pomodoro.report.totalMinutes') }}</div>
          <div class="stat-card__value">{{ summary?.totalWorkMinutes ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card__label">{{ t('pomodoro.report.activeDays') }}</div>
          <div class="stat-card__value">{{ summary?.activeDays ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card__label">{{ t('pomodoro.report.avgMinutes') }}</div>
          <div class="stat-card__value">
            {{ (summary?.avgWorkMinutesPerDay ?? 0).toFixed(1) }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="charts-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" v-loading="loading">
          <template #header>{{ t('pomodoro.report.chartTrend') }}</template>
          <div ref="lineChartRef" class="chart-box" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" v-loading="loading">
          <template #header>{{ t('pomodoro.report.chartGauges') }}</template>
          <div class="gauge-grid">
            <div ref="avgGaugeRef" class="gauge-box" />
            <div ref="periodGaugeRef" class="gauge-box" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="charts-row">
      <el-col :span="24">
        <el-card shadow="never" v-loading="loading">
          <template #header>{{ t('pomodoro.report.chartRounds') }}</template>
          <div ref="barChartRef" class="chart-box chart-box--bar" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>{{ t('pomodoro.report.dailyTable') }}</template>
      <el-table v-loading="loading" :data="daily" stripe border>
        <el-table-column prop="statDate" :label="t('pomodoro.report.date')" width="120" />
        <el-table-column prop="workRounds" :label="t('pomodoro.report.rounds')" width="100" />
        <el-table-column prop="workMinutes" :label="t('pomodoro.report.workMin')" width="120" />
        <el-table-column prop="breakMinutes" :label="t('pomodoro.report.breakMin')" width="120" />
        <el-table-column prop="totalMinutes" :label="t('pomodoro.report.totalMin')" width="120" />
        <el-table-column :label="t('pomodoro.report.workBar')" min-width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="barPercent(row.workMinutes)"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ECharts } from 'echarts/core'
import {
  fetchDailyStats,
  fetchDefaultPlan,
  fetchSummary,
  type PomodoroDailyStat,
  type PomodoroSummary,
} from '@/api/pomodoro'
import { addDays, formatDateParam } from '@/utils/date'
import { echarts } from '@/utils/echarts'

const { t, locale } = useI18n()
const loading = ref(false)
const dateRange = ref<[string, string] | null>(null)
const daily = ref<PomodoroDailyStat[]>([])
const summary = ref<PomodoroSummary | null>(null)
const goalMinutes = ref(200)

const lineChartRef = ref<HTMLElement | null>(null)
const barChartRef = ref<HTMLElement | null>(null)
const avgGaugeRef = ref<HTMLElement | null>(null)
const periodGaugeRef = ref<HTMLElement | null>(null)

let lineChart: ECharts | null = null
let barChart: ECharts | null = null
let avgGauge: ECharts | null = null
let periodGauge: ECharts | null = null

/** 折线/柱状图按时间正序 */
const dailyChronological = computed(() =>
  [...daily.value].sort((a, b) => a.statDate.localeCompare(b.statDate)),
)

const rangeDayCount = computed(() => {
  if (!dateRange.value || dateRange.value.length !== 2) return 1
  const [start, end] = dateRange.value
  const s = new Date(`${start}T00:00:00`)
  const e = new Date(`${end}T00:00:00`)
  const diff = Math.round((e.getTime() - s.getTime()) / 86400000) + 1
  return diff > 0 ? diff : 1
})

const avgGoalPercent = computed(() => {
  if (goalMinutes.value <= 0 || !summary.value) return 0
  return Math.min(
    100,
    Math.round((summary.value.avgWorkMinutesPerDay / goalMinutes.value) * 100),
  )
})

const periodGoalPercent = computed(() => {
  if (goalMinutes.value <= 0 || !summary.value) return 0
  const target = goalMinutes.value * rangeDayCount.value
  if (target <= 0) return 0
  return Math.min(100, Math.round((summary.value.totalWorkMinutes / target) * 100))
})

function initRange() {
  const end = new Date()
  const start = addDays(end, -6)
  dateRange.value = [formatDateParam(start), formatDateParam(end)]
}

function barPercent(workMinutes: number) {
  if (goalMinutes.value <= 0) return 0
  return Math.min(100, Math.round((workMinutes / goalMinutes.value) * 100))
}

function buildGaugeOption(value: number, title: string) {
  return {
    series: [
      {
        type: 'gauge',
        min: 0,
        max: 100,
        radius: '88%',
        center: ['50%', '58%'],
        progress: { show: true, width: 10 },
        axisLine: {
          lineStyle: {
            width: 10,
            color: [
              [0.35, '#e6a23c'],
              [0.7, '#409eff'],
              [1, '#67c23a'],
            ],
          },
        },
        axisTick: { show: false },
        splitLine: { length: 8, lineStyle: { width: 1, color: '#999' } },
        axisLabel: { distance: 14, fontSize: 10 },
        pointer: { width: 5 },
        title: {
          show: true,
          offsetCenter: [0, '78%'],
          fontSize: 12,
          color: '#606266',
        },
        detail: {
          valueAnimation: true,
          fontSize: 22,
          fontWeight: 700,
          offsetCenter: [0, '18%'],
          formatter: '{value}%',
        },
        data: [{ value, name: title }],
      },
    ],
  }
}

function buildLineOption() {
  const rows = dailyChronological.value
  const dates = rows.map((r) => r.statDate)
  return {
    tooltip: { trigger: 'axis' },
    legend: {
      data: [t('pomodoro.report.workMin'), t('pomodoro.report.breakMin')],
      bottom: 0,
    },
    grid: { left: 48, right: 24, top: 24, bottom: 48 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLabel: { rotate: dates.length > 10 ? 35 : 0 },
    },
    yAxis: {
      type: 'value',
      name: t('pomodoro.report.minutesUnit'),
      minInterval: 1,
    },
    series: [
      {
        name: t('pomodoro.report.workMin'),
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 2 },
        itemStyle: { color: '#f56c6c' },
        areaStyle: { color: 'rgba(245, 108, 108, 0.12)' },
        data: rows.map((r) => r.workMinutes),
      },
      {
        name: t('pomodoro.report.breakMin'),
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 2 },
        itemStyle: { color: '#67c23a' },
        areaStyle: { color: 'rgba(103, 194, 58, 0.12)' },
        data: rows.map((r) => r.breakMinutes),
      },
    ],
  }
}

function buildBarOption() {
  const rows = dailyChronological.value
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 24, top: 24, bottom: 40 },
    xAxis: {
      type: 'category',
      data: rows.map((r) => r.statDate),
      axisLabel: { rotate: rows.length > 12 ? 35 : 0 },
    },
    yAxis: {
      type: 'value',
      name: t('pomodoro.report.rounds'),
      minInterval: 1,
    },
    series: [
      {
        name: t('pomodoro.report.rounds'),
        type: 'bar',
        barMaxWidth: 36,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409eff' },
            { offset: 1, color: '#79bbff' },
          ]),
          borderRadius: [4, 4, 0, 0],
        },
        data: rows.map((r) => r.workRounds),
      },
    ],
  }
}

function ensureChart(
  el: HTMLElement | null,
  instance: ECharts | null,
): ECharts | null {
  if (!el) return null
  if (instance) return instance
  return echarts.init(el)
}

function renderCharts() {
  const rows = dailyChronological.value

  if (lineChartRef.value) {
    lineChart = ensureChart(lineChartRef.value, lineChart)
    if (lineChart) {
      lineChart.setOption(buildLineOption(), true)
      if (rows.length === 0) {
        lineChart.showLoading({
          text: t('pomodoro.report.noData'),
          maskColor: 'rgba(255,255,255,0.6)',
          textColor: '#909399',
        })
      } else {
        lineChart.hideLoading()
      }
    }
  }

  if (barChartRef.value) {
    barChart = ensureChart(barChartRef.value, barChart)
    if (barChart) {
      barChart.setOption(buildBarOption(), true)
      if (rows.length === 0) {
        barChart.showLoading({
          text: t('pomodoro.report.noData'),
          maskColor: 'rgba(255,255,255,0.6)',
          textColor: '#909399',
        })
      } else {
        barChart.hideLoading()
      }
    }
  }

  if (avgGaugeRef.value) {
    avgGauge = ensureChart(avgGaugeRef.value, avgGauge)
    avgGauge?.setOption(
      buildGaugeOption(avgGoalPercent.value, t('pomodoro.report.gaugeAvgGoal')),
      true,
    )
  }

  if (periodGaugeRef.value) {
    periodGauge = ensureChart(periodGaugeRef.value, periodGauge)
    periodGauge?.setOption(
      buildGaugeOption(periodGoalPercent.value, t('pomodoro.report.gaugePeriodGoal')),
      true,
    )
  }
}

function resizeCharts() {
  lineChart?.resize()
  barChart?.resize()
  avgGauge?.resize()
  periodGauge?.resize()
}

function disposeCharts() {
  lineChart?.dispose()
  barChart?.dispose()
  avgGauge?.dispose()
  periodGauge?.dispose()
  lineChart = null
  barChart = null
  avgGauge = null
  periodGauge = null
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
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

watch(locale, () => {
  nextTick(() => renderCharts())
})

onMounted(async () => {
  initRange()
  try {
    const plan = await fetchDefaultPlan()
    goalMinutes.value = plan.dailyGoalMinutes
  } catch {
    /* use default */
  }
  await loadReport()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})
</script>

<style scoped>
.panel-toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.summary-row {
  margin-bottom: 8px;
}

.charts-row {
  margin-top: 8px;
}

.stat-card {
  text-align: center;
  margin-bottom: 12px;
}

.stat-card__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.stat-card__value {
  font-size: 28px;
  font-weight: 700;
  margin-top: 8px;
}

.chart-box {
  width: 100%;
  height: 320px;
}

.chart-box--bar {
  height: 260px;
}

.gauge-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  min-height: 320px;
}

.gauge-box {
  width: 100%;
  height: 300px;
}

@media (max-width: 992px) {
  .gauge-grid {
    grid-template-columns: 1fr;
  }
}
</style>
