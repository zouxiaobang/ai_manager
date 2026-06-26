<template>
  <div class="deploy-system-logs">
    <div class="deploy-system-logs__stats-row">
      <article class="deploy-system-logs__stat-card is-purple">
        <div class="deploy-system-logs__stat-icon"><el-icon><Document /></el-icon></div>
        <div class="deploy-system-logs__stat-body">
          <div class="deploy-system-logs__stat-label">{{ t('deployCenter.logsStatToday') }}</div>
          <div class="deploy-system-logs__stat-value">{{ formatCount(stats?.todayTotal ?? 0) }}</div>
          <div class="deploy-system-logs__stat-delta" :class="deltaTone(stats?.todayChangePercent, false)">
            {{ formatDelta(stats?.todayChangePercent) }}
          </div>
        </div>
      </article>
      <article class="deploy-system-logs__stat-card is-red">
        <div class="deploy-system-logs__stat-icon"><el-icon><WarningFilled /></el-icon></div>
        <div class="deploy-system-logs__stat-body">
          <div class="deploy-system-logs__stat-label">ERROR</div>
          <div class="deploy-system-logs__stat-value">{{ formatCount(stats?.errorCount ?? 0) }}</div>
          <div class="deploy-system-logs__stat-delta" :class="deltaTone(stats?.errorChangePercent, true)">
            {{ formatDelta(stats?.errorChangePercent) }}
          </div>
        </div>
      </article>
      <article class="deploy-system-logs__stat-card is-orange">
        <div class="deploy-system-logs__stat-icon"><el-icon><Bell /></el-icon></div>
        <div class="deploy-system-logs__stat-body">
          <div class="deploy-system-logs__stat-label">WARN</div>
          <div class="deploy-system-logs__stat-value">{{ formatCount(stats?.warnCount ?? 0) }}</div>
          <div class="deploy-system-logs__stat-delta" :class="deltaTone(stats?.warnChangePercent, false)">
            {{ formatDelta(stats?.warnChangePercent) }}
          </div>
        </div>
      </article>
      <article class="deploy-system-logs__stat-card" :class="realtimeConnected ? 'is-green' : 'is-gray'">
        <div class="deploy-system-logs__stat-icon"><el-icon><Connection /></el-icon></div>
        <div class="deploy-system-logs__stat-body">
          <div class="deploy-system-logs__stat-label">{{ t('deployCenter.logsStatRealtime') }}</div>
          <div class="deploy-system-logs__stat-value is-text">
            {{ realtimeConnected ? t('deployCenter.logsRealtimeOn') : t('deployCenter.logsRealtimeOff') }}
          </div>
          <div v-if="realtimeConnected" class="deploy-system-logs__stat-hint">
            {{ t('deployCenter.logsRealtimeHealthy') }}
          </div>
        </div>
      </article>
    </div>

    <div class="deploy-system-logs__insight-row">
      <section class="deploy-panel-card deploy-system-logs__charts-card">
        <div class="deploy-system-logs__charts-grid">
          <div class="deploy-system-logs__chart-block">
            <h3 class="deploy-system-logs__chart-title">{{ t('deployCenter.logsLevelChart') }}</h3>
            <div ref="pieChartRef" class="deploy-system-logs__chart-box" />
            <p v-if="!hasChartData" class="deploy-system-logs__chart-empty">{{ t('deployCenter.logsChartEmpty') }}</p>
          </div>
          <div class="deploy-system-logs__chart-block">
            <h3 class="deploy-system-logs__chart-title">{{ t('deployCenter.logsTrendChart') }}</h3>
            <div ref="lineChartRef" class="deploy-system-logs__chart-box" />
            <p v-if="!hasChartData" class="deploy-system-logs__chart-empty">{{ t('deployCenter.logsChartEmpty') }}</p>
          </div>
        </div>
        <div v-if="stats?.topErrors?.length" class="deploy-system-logs__top-errors">
          <h3 class="deploy-system-logs__chart-title">{{ t('deployCenter.logsTopErrors') }}</h3>
          <el-table :data="stats.topErrors" size="small" stripe border>
            <el-table-column prop="message" :label="t('deployCenter.logsErrorMessage')" min-width="280" show-overflow-tooltip />
            <el-table-column prop="count" :label="t('deployCenter.logsErrorCount')" width="80" align="center" />
            <el-table-column prop="lastSeen" :label="t('deployCenter.logsErrorLastSeen')" width="160" />
          </el-table>
        </div>
      </section>

      <section class="deploy-system-logs__ai-card">
        <header class="deploy-system-logs__ai-banner">
          <el-icon class="deploy-system-logs__ai-banner-icon"><MagicStick /></el-icon>
          <h3 class="deploy-system-logs__ai-banner-title">{{ t('deployCenter.logsAiTitle') }}</h3>
        </header>
        <div class="deploy-system-logs__ai-body">
          <p class="deploy-system-logs__ai-intro">{{ t('deployCenter.logsAiIntro') }}</p>
          <p v-if="aiResult?.summary" class="deploy-system-logs__ai-summary">{{ aiResult.summary }}</p>
          <p v-else-if="!aiLoading" class="deploy-system-logs__ai-placeholder">{{ t('deployCenter.logsAiPlaceholder') }}</p>
          <ul v-if="aiDisplayItems.length" class="deploy-system-logs__ai-items">
            <li
              v-for="(item, index) in aiDisplayItems"
              :key="`ai-item-${index}`"
              class="deploy-system-logs__ai-item"
            >
              <span class="deploy-system-logs__ai-dot" :class="`is-${item.severity}`" />
              <span class="deploy-system-logs__ai-item-text">{{ item.text }}</span>
            </li>
          </ul>
          <el-input
            v-model="aiQuestion"
            type="textarea"
            :rows="2"
            :placeholder="t('deployCenter.logsAiQuestionPlaceholder')"
            class="deploy-system-logs__ai-question"
          />
          <el-button
            type="success"
            size="large"
            class="deploy-system-logs__ai-action"
            :loading="aiLoading"
            @click="runAiAnalyze"
          >
            <el-icon><Refresh /></el-icon>
            {{ t('deployCenter.logsAiAnalyze') }}
          </el-button>
        </div>
      </section>
    </div>

    <section class="deploy-panel-card deploy-system-logs__console-card">
      <div class="deploy-system-logs__console-toolbar">
        <div class="deploy-system-logs__mode-switch" role="tablist">
          <button
            type="button"
            class="deploy-system-logs__mode-btn"
            :class="{ 'is-active': logMode === 'history' }"
            role="tab"
            :aria-selected="logMode === 'history'"
            @click="switchLogMode('history')"
          >
            {{ t('deployCenter.logsModeHistory') }}
          </button>
          <button
            type="button"
            class="deploy-system-logs__mode-btn"
            :class="{ 'is-active': logMode === 'realtime' }"
            role="tab"
            :aria-selected="logMode === 'realtime'"
            @click="switchLogMode('realtime')"
          >
            {{ t('deployCenter.logsModeRealtime') }}
          </button>
        </div>
        <el-select v-model="lineCount" size="large" class="deploy-system-logs__toolbar-item" :disabled="logMode === 'realtime'">
          <el-option :value="100" label="100" />
          <el-option :value="200" label="200" />
          <el-option :value="500" label="500" />
          <el-option :value="1000" label="1000" />
        </el-select>
        <el-select v-model="levelFilter" size="large" class="deploy-system-logs__toolbar-item" clearable :placeholder="t('deployCenter.logsLevelAll')">
          <el-option value="ALL" :label="t('deployCenter.logsLevelAll')" />
          <el-option value="ERROR" label="ERROR" />
          <el-option value="WARN" label="WARN" />
          <el-option value="INFO" label="INFO" />
          <el-option value="DEBUG" label="DEBUG" />
        </el-select>
        <el-input
          v-model="keyword"
          size="large"
          clearable
          class="deploy-system-logs__search"
          :placeholder="t('deployCenter.logsSearchPlaceholder')"
          @keyup.enter="refreshLogs"
        />
        <el-button size="large" :loading="logsLoading" @click="refreshLogs">{{ t('deployCenter.logsRefresh') }}</el-button>
        <el-button size="large" :disabled="!displayEntries.length" @click="exportLogs">{{ t('deployCenter.logsExport') }}</el-button>
      </div>

      <p class="deploy-system-logs__console-meta">
        <span>{{ logFileLabel }}</span>
        <span>{{ t('deployCenter.logsLoadedCount', { count: displayEntries.length }) }}</span>
      </p>

      <div ref="consoleRef" v-loading="logsLoading" class="deploy-system-logs__console">
        <p v-if="!displayEntries.length && !logsLoading" class="deploy-system-logs__console-empty">
          {{ tail?.fileExists ? t('deployCenter.logsEmpty') : t('deployCenter.logsFileMissing') }}
        </p>
        <div
          v-for="entry in displayEntries"
          :key="`${entry.lineNumber}-${entry.raw}`"
          class="deploy-system-logs__line"
          :class="`is-${(entry.level || 'info').toLowerCase()}`"
        >
          <span class="deploy-system-logs__line-prefix">&gt;</span>
          <span v-if="entry.timestamp" class="deploy-system-logs__line-time">{{ entry.timestamp }}</span>
          <span
            v-if="logMode === 'history'"
            class="deploy-system-logs__level-tag"
            :class="`is-${(entry.level || 'INFO').toLowerCase()}`"
          >
            {{ entry.level || 'INFO' }}
          </span>
          <span v-else class="deploy-system-logs__line-level">{{ entry.level || 'INFO' }}</span>
          <span v-if="entry.logger" class="deploy-system-logs__line-logger">{{ shortLogger(entry.logger) }}</span>
          <span v-if="entry.logger" class="deploy-system-logs__line-sep">-</span>
          <span class="deploy-system-logs__line-message">{{ entry.message || entry.raw }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { ECharts } from 'echarts/core'
import { Bell, Connection, Document, MagicStick, Refresh, WarningFilled } from '@element-plus/icons-vue'
import {
  analyzeDeployLogs,
  fetchDeployLogStats,
  fetchDeployLogTail,
  streamDeployLogs,
  type DeployLogAiAnalyze,
  type DeployLogAiInsightItem,
  type DeployLogEntry,
  type DeployLogStats,
  type DeployLogTail,
} from '@/api/deployLogs'
import { echarts } from '@/utils/echarts'

const props = defineProps<{
  active?: boolean
}>()

const { t } = useI18n()

const stats = ref<DeployLogStats | null>(null)
const tail = ref<DeployLogTail | null>(null)
const aiResult = ref<DeployLogAiAnalyze | null>(null)
const realtimeEntries = ref<DeployLogEntry[]>([])

const logsLoading = ref(false)
const aiLoading = ref(false)
const logMode = ref<'history' | 'realtime'>('history')
const lineCount = ref(100)
const levelFilter = ref('ALL')
const keyword = ref('')
const aiQuestion = ref('')
const realtimeConnected = ref(false)

const pieChartRef = ref<HTMLElement | null>(null)
const lineChartRef = ref<HTMLElement | null>(null)
const consoleRef = ref<HTMLElement | null>(null)

let pieChart: ECharts | null = null
let lineChart: ECharts | null = null
let stopStream: (() => void) | null = null
let chartResizeObserver: ResizeObserver | null = null

const hasChartData = computed(() => {
  const counts = stats.value?.levelCounts ?? {}
  return Object.values(counts).some((value) => value > 0)
})

const displayEntries = computed(() => {
  if (logMode.value === 'realtime') {
    return realtimeEntries.value
  }
  return tail.value?.entries ?? []
})

const logFileLabel = computed(() => {
  const path = tail.value?.logFile || stats.value?.logFile
  if (!path) {
    return t('deployCenter.logsFileUnknown')
  }
  return t('deployCenter.logsFilePath', { path })
})

const aiDisplayItems = computed((): DeployLogAiInsightItem[] => {
  const items = aiResult.value?.items ?? []
  if (items.length) {
    return [...items].sort((a, b) => severityRank(a.severity) - severityRank(b.severity))
  }
  const fallback: DeployLogAiInsightItem[] = []
  for (const text of aiResult.value?.insights ?? []) {
    fallback.push({ severity: 'error', text })
  }
  for (const text of aiResult.value?.suggestions ?? []) {
    fallback.push({ severity: 'info', text })
  }
  return fallback.sort((a, b) => severityRank(a.severity) - severityRank(b.severity))
})

function severityRank(severity: DeployLogAiInsightItem['severity']) {
  const order: Record<DeployLogAiInsightItem['severity'], number> = {
    error: 0,
    warn: 1,
    info: 2,
    success: 3,
  }
  return order[severity] ?? 9
}

function formatCount(value: number) {
  return value.toLocaleString()
}

function formatDelta(percent?: number | null) {
  if (percent == null || Number.isNaN(percent)) {
    return t('deployCenter.logsCompareFlat')
  }
  const arrow = percent >= 0 ? '↑' : '↓'
  return t('deployCenter.logsCompareYesterday', {
    arrow,
    percent: Math.abs(percent).toFixed(1),
  })
}

function deltaTone(percent?: number | null, invert = false) {
  if (percent == null || Number.isNaN(percent) || percent === 0) {
    return 'is-flat'
  }
  const positive = percent > 0
  const good = invert ? !positive : positive
  return good ? 'is-up' : 'is-down'
}

function shortLogger(logger: string) {
  const parts = logger.split('.')
  return parts[parts.length - 1] || logger
}

async function loadStats() {
  try {
    stats.value = await fetchDeployLogStats(24)
    scheduleChartRender()
  } catch {
    stats.value = null
  }
}

async function loadTail() {
  logsLoading.value = true
  try {
    tail.value = await fetchDeployLogTail({
      lines: lineCount.value,
      level: levelFilter.value === 'ALL' || !levelFilter.value ? undefined : levelFilter.value,
      keyword: keyword.value.trim() || undefined,
    })
    await nextTick()
    scrollConsoleToBottom()
  } catch {
    tail.value = null
  } finally {
    logsLoading.value = false
  }
}

async function refreshLogs() {
  await Promise.all([loadStats(), loadTail()])
}

async function runAiAnalyze() {
  aiLoading.value = true
  try {
    aiResult.value = await analyzeDeployLogs({
      lines: lineCount.value,
      question: aiQuestion.value.trim() || undefined,
    })
  } finally {
    aiLoading.value = false
  }
}

function onModeChange(mode: string | number | boolean | undefined) {
  if (mode === 'realtime') {
    startRealtime()
    return
  }
  stopRealtime()
  void loadTail()
}

function switchLogMode(mode: 'history' | 'realtime') {
  if (logMode.value === mode) {
    return
  }
  logMode.value = mode
  onModeChange(mode)
}

function startRealtime() {
  stopRealtime()
  realtimeEntries.value = []
  realtimeConnected.value = false
  stopStream = streamDeployLogs(
    {
      level: levelFilter.value,
      keyword: keyword.value,
    },
    {
      onReady: () => {
        realtimeConnected.value = true
      },
      onLog: (entry) => {
        realtimeEntries.value.push(entry)
        if (realtimeEntries.value.length > 2000) {
          realtimeEntries.value.splice(0, realtimeEntries.value.length - 2000)
        }
        void nextTick().then(scrollConsoleToBottom)
      },
      onDisconnect: () => {
        realtimeConnected.value = false
      },
    },
  )
}

function stopRealtime() {
  if (stopStream) {
    stopStream()
    stopStream = null
  }
  realtimeConnected.value = false
}

function scrollConsoleToBottom() {
  const el = consoleRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

function exportLogs() {
  const lines = displayEntries.value.map((entry) => entry.raw || entry.message)
  if (!lines.length) {
    ElMessage.warning(t('deployCenter.logsExportEmpty'))
    return
  }
  const blob = new Blob([`\uFEFF${lines.join('\n')}`], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `backend-logs-${Date.now()}.log`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success(t('deployCenter.logsExportOk'))
}

function scheduleChartRender() {
  if (!props.active) {
    return
  }
  void nextTick(() => {
    requestAnimationFrame(() => {
      renderCharts()
      pieChart?.resize()
      lineChart?.resize()
    })
  })
}

function renderCharts() {
  renderPieChart()
  renderLineChart()
}

function renderPieChart() {
  const el = pieChartRef.value
  if (!el || !props.active) return
  if (!pieChart || pieChart.isDisposed()) {
    pieChart = echarts.init(el)
  }
  const counts = stats.value?.levelCounts ?? {}
  const data = Object.entries(counts)
    .filter(([, value]) => value > 0)
    .map(([name, value]) => ({ name, value }))
  pieChart.setOption({
    color: ['#ef4444', '#f59e0b', '#3b82f6', '#22c55e', '#94a3b8', '#cbd5e1'],
    tooltip: {
      trigger: 'item',
      formatter: (params: { name: string; value: number; percent: number }) =>
        `${params.name}: ${params.value} (${params.percent}%)`,
    },
    legend: { bottom: 0, icon: 'circle' },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '44%'],
        data: data.length ? data : [{ name: 'INFO', value: 0 }],
        label: {
          formatter: (params: { name: string; value: number; percent: number }) =>
            `${params.name}\n${params.value} (${params.percent}%)`,
        },
      },
    ],
  })
  pieChart.resize()
}

function renderLineChart() {
  const el = lineChartRef.value
  if (!el || !props.active) return
  if (!lineChart || lineChart.isDisposed()) {
    lineChart = echarts.init(el)
  }
  const trend = stats.value?.hourlyTrend ?? []
  lineChart.setOption({
    color: ['#3b82f6', '#ef4444', '#f59e0b'],
    tooltip: { trigger: 'axis' },
    legend: { data: [t('deployCenter.logsTrendTotal'), 'ERROR', 'WARN'], bottom: 0 },
    grid: { left: 36, right: 16, top: 24, bottom: 48 },
    xAxis: { type: 'category', data: trend.map((item) => item.hour) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: t('deployCenter.logsTrendTotal'), type: 'line', smooth: true, data: trend.map((item) => item.total) },
      { name: 'ERROR', type: 'line', smooth: true, data: trend.map((item) => item.errorCount) },
      { name: 'WARN', type: 'line', smooth: true, data: trend.map((item) => item.warnCount) },
    ],
  })
  lineChart.resize()
}

function resizeCharts() {
  pieChart?.resize()
  lineChart?.resize()
}

function setupChartObserver() {
  chartResizeObserver?.disconnect()
  chartResizeObserver = new ResizeObserver(() => {
    if (!props.active) return
    pieChart?.resize()
    lineChart?.resize()
  })
  if (pieChartRef.value) chartResizeObserver.observe(pieChartRef.value)
  if (lineChartRef.value) chartResizeObserver.observe(lineChartRef.value)
}

watch(
  () => stats.value,
  () => {
    scheduleChartRender()
  },
  { deep: true },
)

watch(
  () => props.active,
  (active) => {
    if (active) {
      void refreshLogs().then(() => runAiAnalyze())
      scheduleChartRender()
    } else {
      stopRealtime()
      logMode.value = 'history'
    }
  },
  { immediate: true },
)

onMounted(() => {
  setupChartObserver()
  scheduleChartRender()
})

watch(lineCount, () => {
  if (props.active && logMode.value === 'history') {
    void loadTail()
  }
})

watch([levelFilter, keyword], () => {
  if (props.active && logMode.value === 'realtime') {
    startRealtime()
  }
})

onUnmounted(() => {
  stopRealtime()
  chartResizeObserver?.disconnect()
  pieChart?.dispose()
  lineChart?.dispose()
  pieChart = null
  lineChart = null
  window.removeEventListener('resize', resizeCharts)
})

window.addEventListener('resize', resizeCharts)

defineExpose({ reload: refreshLogs })
</script>

<style scoped lang="scss">
.deploy-system-logs {
  display: flex;
  flex-direction: column;
  gap: 18px;

  &__stats-row {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
  }

  &__stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 18px 16px;
    border-radius: 14px;
    border: 1px solid transparent;
    box-shadow: 0 1px 2px rgb(15 23 42 / 4%);

    &.is-purple {
      background: #f5f3ff;
      border-color: #ddd6fe;
      .deploy-system-logs__stat-icon { background: #8b5cf6; }
      .deploy-system-logs__stat-value { color: #7c3aed; }
    }
    &.is-red {
      background: #fef2f2;
      border-color: #fecaca;
      .deploy-system-logs__stat-icon { background: #ef4444; }
      .deploy-system-logs__stat-value { color: #dc2626; }
    }
    &.is-orange {
      background: #fffbeb;
      border-color: #fde68a;
      .deploy-system-logs__stat-icon { background: #f59e0b; }
      .deploy-system-logs__stat-value { color: #d97706; }
    }
    &.is-green {
      background: #f0fdf4;
      border-color: #bbf7d0;
      .deploy-system-logs__stat-icon { background: #22c55e; }
      .deploy-system-logs__stat-value { color: #16a34a; }
    }
    &.is-gray {
      background: #f8fafc;
      border-color: #e2e8f0;
      .deploy-system-logs__stat-icon { background: #94a3b8; }
      .deploy-system-logs__stat-value { color: #64748b; }
    }
  }

  &__stat-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 52px;
    height: 52px;
    border-radius: 14px;
    color: #fff;
    font-size: 26px;
  }

  &__stat-label {
    font-size: 13px;
    color: #6b7280;
    margin-bottom: 6px;
  }

  &__stat-value {
    font-size: 28px;
    font-weight: 700;
    line-height: 1.15;

    &.is-text {
      font-size: 17px;
    }
  }

  &__stat-delta {
    margin-top: 6px;
    font-size: 12px;
    font-weight: 600;

    &.is-up { color: #16a34a; }
    &.is-down { color: #dc2626; }
    &.is-flat { color: #6b7280; }
  }

  &__stat-hint {
    margin-top: 6px;
    font-size: 12px;
    color: #16a34a;
    font-weight: 600;
  }

  &__insight-row {
    display: grid;
    grid-template-columns: minmax(0, 1.45fr) minmax(280px, 0.95fr);
    gap: 14px;
    align-items: stretch;
  }

  &__charts-card,
  &__console-card {
    padding: 18px 20px;
    border-radius: 14px;
    background: #fff;
    border: 1px solid #e8ecf2;
  }

  &__ai-card {
    overflow: hidden;
    border-radius: 14px;
    border: 1px solid #e8ecf2;
    background: #fff;
    box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
    display: flex;
    flex-direction: column;
  }

  &__charts-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
  }

  &__chart-title {
    margin: 0 0 8px;
    font-size: 14px;
    font-weight: 700;
    color: #111827;
  }

  &__chart-box {
    height: 220px;
  }

  &__chart-empty {
    margin: -200px 0 0;
    height: 200px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: #9ca3af;
    pointer-events: none;
  }

  &__top-errors {
    margin-top: 14px;
  }

  &__ai-banner {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 16px 18px;
    background: linear-gradient(135deg, #7c3aed 0%, #6366f1 55%, #3b82f6 100%);
    color: #fff;
  }

  &__ai-banner-icon {
    font-size: 22px;
  }

  &__ai-banner-title {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
  }

  &__ai-body {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 16px 18px 18px;
    flex: 1;
  }

  &__ai-intro {
    margin: 0;
    font-size: 13px;
    color: #6b7280;
  }

  &__ai-summary {
    margin: 0;
    font-size: 13px;
    line-height: 1.6;
    color: #374151;
    font-weight: 600;
  }

  &__ai-placeholder {
    margin: 0;
    font-size: 13px;
    color: #9ca3af;
  }

  &__ai-items {
    margin: 0;
    padding: 0;
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  &__ai-item {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    font-size: 13px;
    line-height: 1.55;
    color: #374151;
  }

  &__ai-dot {
    flex-shrink: 0;
    width: 8px;
    height: 8px;
    margin-top: 6px;
    border-radius: 50%;

    &.is-error { background: #ef4444; }
    &.is-warn { background: #f59e0b; }
    &.is-info { background: #3b82f6; }
    &.is-success { background: #22c55e; }
  }

  &__ai-item-text {
    flex: 1;
    min-width: 0;
    word-break: break-word;
  }

  &__ai-question {
    margin-top: 4px;
  }

  &__ai-action {
    width: 100%;
    margin-top: auto;
  }

  &__console-toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
  }

  &__mode-switch {
    display: inline-flex;
    align-items: stretch;
    padding: 3px;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    background: #fff;
    box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
  }

  &__mode-btn {
    min-width: 96px;
    padding: 9px 20px;
    border: 2px solid transparent;
    border-radius: 8px;
    background: transparent;
    color: #1f2937;
    font-size: 14px;
    font-weight: 500;
    line-height: 1.2;
    cursor: pointer;
    transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;

    &:hover:not(.is-active) {
      background: #f8fafc;
    }

    &.is-active {
      background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 52%, #f5f3ff 100%);
      border-color: #8b5cf6;
      color: #6d28d9;
      font-weight: 600;
    }
  }

  &__toolbar-item {
    width: 130px;
  }

  &__search {
    width: 260px;
  }

  &__console-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin: 0 0 10px;
    font-size: 12px;
    color: #6b7280;
    font-weight: 600;
  }

  &__console {
    min-height: 300px;
    max-height: 460px;
    overflow: auto;
    border-radius: 12px;
    background: #111827;
    padding: 14px 16px;
    font-family: Consolas, 'Courier New', monospace;
    font-size: 13px;
    line-height: 1.65;
  }

  &__console-empty {
    margin: 0;
    color: #9ca3af;
  }

  &__line {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: 8px;
    margin-bottom: 6px;
    color: #e5e7eb;

    &.is-error { color: #fca5a5; }
    &.is-warn { color: #fcd34d; }
    &.is-info { color: #bfdbfe; }
    &.is-debug,
    &.is-trace { color: #cbd5e1; }
  }

  &__line-prefix {
    color: #64748b;
    user-select: none;
  }

  &__line-time {
    color: #94a3b8;
    white-space: nowrap;
  }

  &__level-tag {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 52px;
    padding: 2px 8px;
    border-radius: 999px;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.02em;
    color: #fff;

    &.is-error { background: #ef4444; }
    &.is-warn { background: #f59e0b; color: #111827; }
    &.is-info { background: #3b82f6; }
    &.is-debug { background: #22c55e; }
    &.is-trace { background: #94a3b8; }
  }

  &__line-level {
    font-weight: 700;
    min-width: 44px;
  }

  &__line-logger {
    color: #a5b4fc;
  }

  &__line-sep {
    color: #64748b;
  }

  &__line-message {
    flex: 1;
    min-width: 200px;
    word-break: break-word;
  }
}

@media (max-width: 1200px) {
  .deploy-system-logs__stats-row,
  .deploy-system-logs__insight-row,
  .deploy-system-logs__charts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
