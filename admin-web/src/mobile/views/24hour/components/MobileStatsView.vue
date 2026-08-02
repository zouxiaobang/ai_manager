<template>
  <div class="v2-stats-view">
    <div class="v2-stats-view__range">
      <span class="v2-stats-view__range-label">{{ t('24hour.stats.statsRange') }}</span>
      <div class="v2-stats-view__range-inputs">
        <input
          v-model="statsStart"
          type="date"
          class="v2-stats-view__input"
          @change="loadStats"
        />
        <span class="v2-stats-view__range-sep">{{ t('24hour.stats.rangeSep') }}</span>
        <input
          v-model="statsEnd"
          type="date"
          class="v2-stats-view__input"
          @change="loadStats"
        />
      </div>
    </div>

    <div class="v2-stats-view__overview">
      <div class="v2-stats-view__row">
        <div class="v2-stats-view__card v2-stats-view__card--days">
          <span class="v2-stats-view__card-num">{{ globalTotalDays }}</span>
          <span class="v2-stats-view__card-label">天</span>
          <span class="v2-stats-view__card-desc">总天数（封顶30天）</span>
        </div>
        <div class="v2-stats-view__card v2-stats-view__card--streak">
          <span class="v2-stats-view__card-num">{{ statsData?.summary.currentStreak ?? 0 }}</span>
          <span class="v2-stats-view__card-label">天</span>
          <span class="v2-stats-view__card-desc">连续坚持 ≥50%</span>
        </div>
      </div>
      <div class="v2-stats-view__row">
        <div class="v2-stats-view__card v2-stats-view__card--best">
          <span class="v2-stats-view__card-num">{{ statsData?.summary.bestStreak ?? 0 }}</span>
          <span class="v2-stats-view__card-label">天</span>
          <span class="v2-stats-view__card-desc">最长连续坚持</span>
        </div>
        <div class="v2-stats-view__card v2-stats-view__card--perfect">
          <span class="v2-stats-view__card-num">{{ statsData?.summary.perfectDays ?? 0 }}</span>
          <span class="v2-stats-view__card-label">天</span>
          <span class="v2-stats-view__card-desc">全部完成</span>
        </div>
      </div>
      <div class="v2-stats-view__row">
        <div class="v2-stats-view__card v2-stats-view__card--combined">
          <div class="v2-stats-view__combined-left">
            <div class="v2-stats-view__card-top">
              <span class="v2-stats-view__card-label">{{ t('24hour.stats.vsLastWeek') }}</span>
              <span
                class="v2-stats-view__week-trend"
                :class="weekTrendClass"
              >
                <svg
                  v-if="weekTrendDir === 'up'"
                  class="v2-stats-view__week-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                >
                  <path d="M12 19V5M5 12l7-7 7 7" />
                </svg>
                <svg
                  v-else-if="weekTrendDir === 'down'"
                  class="v2-stats-view__week-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                >
                  <path d="M12 5v14M19 12l-7 7-7-7" />
                </svg>
                {{ weekTrendText }}
              </span>
            </div>
            <div class="v2-stats-view__week-body">
              <div class="v2-stats-view__week-metric">
                <span class="v2-stats-view__week-num">{{ thisWeekRateText }}</span>
                <span class="v2-stats-view__week-sub">本周</span>
              </div>
              <div class="v2-stats-view__week-arrow">→</div>
              <div class="v2-stats-view__week-metric v2-stats-view__week-metric--last">
                <span class="v2-stats-view__week-num">{{ lastWeekRateText }}</span>
                <span class="v2-stats-view__week-sub">上周</span>
              </div>
            </div>
          </div>
          <div class="v2-stats-view__combined-divider" />
          <div class="v2-stats-view__combined-right">
            <span class="v2-stats-view__card-num">{{ overallRateText }}</span>
            <span class="v2-stats-view__card-label">{{ t('24hour.stats.overallRate') }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="v2-stats-view__section">
      <h3 class="v2-stats-view__section-title">{{ t('24hour.stats.phaseRateTitle') }}</h3>
      <div class="v2-stats-view__phase-list">
        <div
          v-for="p in phases"
          :key="p.key"
          class="v2-stats-view__phase-bar"
          :style="{ '--accent': p.accent }"
        >
          <div class="v2-stats-view__phase-bar-hd">
            <div class="v2-stats-view__phase-bar-hd-left">
              <span class="v2-stats-view__phase-badge">{{ p.badge }}</span>
              <span class="v2-stats-view__phase-name">{{ p.title }}</span>
            </div>
            <span class="v2-stats-view__phase-rate">{{ phaseRateText(p.key) }}</span>
          </div>
          <div class="v2-stats-view__phase-track">
            <div
              class="v2-stats-view__phase-fill"
              :style="{ width: phaseRatePercent(p.key) }"
            />
          </div>
          <div class="v2-stats-view__phase-bt">
            <span class="v2-stats-view__phase-count">{{ phaseDoneText(p.key) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="v2-stats-view__section">
      <h3 class="v2-stats-view__section-title">{{ t('24hour.stats.dailyDetailTitle') }}</h3>
      <div class="v2-stats-view__table-wrap">
        <table class="v2-stats-view__table">
          <thead>
            <tr>
              <th>{{ t('24hour.stats.date') }}</th>
              <th>{{ t('24hour.stats.completed') }}</th>
              <th>完成率</th>
              <th
                v-for="p in phases"
                :key="p.key"
                class="v2-stats-view__th-phase"
              >
                <span :style="{ color: p.accent }">{{ p.badge }}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in statsData?.items ?? []" :key="row.date">
              <td class="v2-stats-view__td-date">{{ formatShortDate(row.date) }}</td>
              <td>{{ row.completedCount }}/{{ row.totalCount }}</td>
              <td>
                <span
                  class="v2-stats-view__rate-badge"
                  :class="rateBadgeClass(row.completionRate)"
                >
                  {{ (row.completionRate * 100).toFixed(0) }}%
                </span>
              </td>
              <td
                v-for="p in phases"
                :key="p.key"
                class="v2-stats-view__td-phase"
              >
                <span
                  class="v2-stats-view__phase-cell"
                  :class="getPhaseCellClass(row, p.key)"
                  :style="{ '--accent': p.accent }"
                >
                  {{ getPhaseCellText(row, p.key) }}
                </span>
              </td>
            </tr>
            <tr v-if="!(statsData?.items?.length)">
              <td colspan="100%" class="v2-stats-view__empty">{{ t('24hour.stats.noData') }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchDailyChecklistStats } from '@/api/dailyChecklist'
import type { DailyChecklistStats, DailyStatsItem } from '@/api/dailyChecklist'
import type { PhaseDef } from '@/data/24hour-phases'

const props = defineProps<{
  phases: PhaseDef[]
}>()

const { t } = useI18n()

function defaultStatsRange() {
  const end = new Date()
  const start = new Date(end)
  start.setDate(start.getDate() - 29)
  return {
    start: start.toISOString().split('T')[0],
    end: end.toISOString().split('T')[0],
  }
}

const defaultRange = defaultStatsRange()
const statsStart = ref(defaultRange.start)
const statsEnd = ref(defaultRange.end)
const statsData = ref<DailyChecklistStats | null>(null)

const overallRateText = computed(() => {
  if (!statsData.value?.items.length) return '—'
  const totalDays = globalTotalDays.value
  if (totalDays === 0) return '—'
  let totalItems = 0
  let totalDone = 0
  for (const p of props.phases) {
    const { total, done } = phaseStatsForKey(p.key)
    totalItems += total
    totalDone += done
  }
  if (totalItems === 0) return '—'
  return (totalDone / totalItems * 100).toFixed(1) + '%'
})

function rateForDateRange(startDate: string, endDate: string) {
  if (!statsData.value?.items.length) return { rate: 0, hasData: false }
  const start = new Date(startDate)
  const end = new Date(endDate)
  const totalDays = Math.floor((end.getTime() - start.getTime()) / 86400000) + 1
  if (totalDays <= 0) return { rate: 0, hasData: false }
  let totalItems = 0
  let totalDone = 0
  for (const p of props.phases) {
    const itemsPerDay = p.items.length
    const phaseTotal = totalDays * itemsPerDay
    let phaseDone = 0
    for (const d of statsData.value.items) {
      const dDate = new Date(d.date)
      if (dDate >= start && dDate <= end) {
        const ps = d.phaseStats.find(sp => sp.phaseKey === p.key)
        if (ps && ps.totalCount > 0) {
          phaseDone += ps.completedCount
        }
      }
    }
    totalItems += phaseTotal
    totalDone += phaseDone
  }
  if (totalItems === 0) return { rate: 0, hasData: false }
  return { rate: totalDone / totalItems, hasData: true }
}

function thisWeekRange() {
  const end = new Date(statsEnd.value)
  const start = new Date(end)
  start.setDate(start.getDate() - 6)
  return {
    start: start.toISOString().split('T')[0],
    end: end.toISOString().split('T')[0],
  }
}

function lastWeekRange() {
  const end = new Date(statsEnd.value)
  end.setDate(end.getDate() - 7)
  const start = new Date(end)
  start.setDate(start.getDate() - 6)
  return {
    start: start.toISOString().split('T')[0],
    end: end.toISOString().split('T')[0],
  }
}

const thisWeekRateText = computed(() => {
  const r = thisWeekRange()
  const { rate, hasData } = rateForDateRange(r.start, r.end)
  if (!hasData) return '—'
  return (rate * 100).toFixed(0) + '%'
})

const lastWeekRateText = computed(() => {
  const r = lastWeekRange()
  const { rate, hasData } = rateForDateRange(r.start, r.end)
  if (!hasData) return '—'
  return (rate * 100).toFixed(0) + '%'
})

const weekTrendDir = computed<'up' | 'down' | 'flat'>(() => {
  const tw = thisWeekRange()
  const lw = lastWeekRange()
  const twRes = rateForDateRange(tw.start, tw.end)
  const lwRes = rateForDateRange(lw.start, lw.end)
  if (!twRes.hasData || !lwRes.hasData) return 'flat'
  const diff = twRes.rate - lwRes.rate
  if (Math.abs(diff) < 0.001) return 'flat'
  return diff > 0 ? 'up' : 'down'
})

const weekTrendClass = computed(() => {
  return `v2-stats-view__week-trend--${weekTrendDir.value}`
})

const weekTrendText = computed(() => {
  const tw = thisWeekRange()
  const lw = lastWeekRange()
  const twRes = rateForDateRange(tw.start, tw.end)
  const lwRes = rateForDateRange(lw.start, lw.end)
  if (!twRes.hasData || !lwRes.hasData) return t('24hour.stats.weekFlat')
  const diff = Math.abs(twRes.rate - lwRes.rate) * 100
  if (diff < 0.1) return t('24hour.stats.weekFlat')
  const label = weekTrendDir.value === 'up' ? t('24hour.stats.weekUp') : t('24hour.stats.weekDown')
  return `${label} ${diff.toFixed(1)}%`
})

const globalTotalDays = computed(() => {
  if (!statsData.value?.items.length) return 0
  const firstDate = statsData.value.items[0]?.date
  if (!firstDate) return 0
  const start = new Date(firstDate)
  const end = new Date(statsEnd.value)
  const days = Math.floor((end.getTime() - start.getTime()) / 86400000) + 1
  return Math.min(days, 30)
})

function phaseFirstDate(phaseKey: string): string | null {
  if (!statsData.value?.items.length) return null
  for (const d of statsData.value.items) {
    const ps = d.phaseStats.find(p => p.phaseKey === phaseKey)
    if (ps && ps.totalCount > 0) return d.date
  }
  return null
}

function phaseDaysSinceFirstRecord(phaseKey: string) {
  const firstDate = phaseFirstDate(phaseKey)
  if (!firstDate) return 0
  const start = new Date(firstDate)
  const end = new Date(statsEnd.value)
  const days = Math.floor((end.getTime() - start.getTime()) / 86400000) + 1
  return Math.min(days, 30)
}

function phaseStatsForKey(phaseKey: string) {
  if (!statsData.value?.items.length) return { total: 0, done: 0, days: 0 }
  const itemsPerDay = getPhaseTotalCount(phaseKey)
  if (itemsPerDay === 0) return { total: 0, done: 0, days: 0 }
  let days = 0
  let done = 0
  for (const d of statsData.value.items) {
    const ps = d.phaseStats.find(p => p.phaseKey === phaseKey)
    if (ps && ps.totalCount > 0) {
      days++
      done += ps.completedCount
    }
  }
  const totalDays = phaseDaysSinceFirstRecord(phaseKey)
  return { total: totalDays * itemsPerDay, done, days }
}

function phaseDoneText(phaseKey: string) {
  const { total, done } = phaseStatsForKey(phaseKey)
  if (total === 0) return ''
  return `已完成 ${done}/${total} 项`
}

function phaseRateText(phaseKey: string) {
  const { total, done, days } = phaseStatsForKey(phaseKey)
  if (days === 0) return '—'
  return (done / total * 100).toFixed(0) + '%'
}

function phaseRatePercent(phaseKey: string) {
  const { total, done, days } = phaseStatsForKey(phaseKey)
  if (days === 0) return '0%'
  return (done / total * 100).toFixed(0) + '%'
}

function getPhaseTotalCount(phaseKey: string) {
  return props.phases.find(p => p.key === phaseKey)?.items.length ?? 0
}

function getPhaseCompletedCount(row: DailyStatsItem, phaseKey: string) {
  return row.phaseStats.find(p => p.phaseKey === phaseKey)?.completedCount ?? 0
}

function getPhaseStat(row: DailyStatsItem, phaseKey: string): number {
  const total = getPhaseTotalCount(phaseKey)
  if (total === 0) return 2
  const done = getPhaseCompletedCount(row, phaseKey)
  if (done >= total) return 1
  if (done > 0) return 0
  return 2
}

function getPhaseCellText(row: DailyStatsItem, phaseKey: string) {
  const total = getPhaseTotalCount(phaseKey)
  if (total === 0) return '—'
  const done = getPhaseCompletedCount(row, phaseKey)
  return `${done}/${total}`
}

function getPhaseCellClass(row: DailyStatsItem, phaseKey: string) {
  const stat = getPhaseStat(row, phaseKey)
  if (stat === 2) return 'v2-stats-view__phase-cell--skip'
  if (stat === 1) return 'v2-stats-view__phase-cell--done'
  return 'v2-stats-view__phase-cell--partial'
}

function formatShortDate(dateStr: string) {
  const [, m, d] = dateStr.split('-')
  return `${parseInt(m)}/${parseInt(d)}`
}

function rateBadgeClass(rate: number) {
  if (rate >= 0.8) return 'v2-stats-view__rate-badge--high'
  if (rate >= 0.5) return 'v2-stats-view__rate-badge--mid'
  return 'v2-stats-view__rate-badge--low'
}

async function loadStats() {
  try {
    statsData.value = await fetchDailyChecklistStats(statsStart.value, statsEnd.value)
  } catch {
    /* 静默 */
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped lang="scss">
.v2-stats-view {
  display: flex;
  flex-direction: column;

  &__range {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
  }

  &__range-label {
    font-size: 13px;
    font-weight: 600;
    color: #374151;
    flex-shrink: 0;
  }

  &__range-inputs {
    display: flex;
    align-items: center;
    gap: 6px;
    flex: 1;
    min-width: 0;
  }

  &__input {
    flex: 1;
    min-width: 0;
    padding: 7px 10px;
    border: 1.5px solid #e2e8f0;
    border-radius: 8px;
    font-size: 13px;
    color: #374151;
    background: #fff;
    font-family: inherit;
    transition: border-color 0.2s ease;

    &:focus {
      outline: none;
      border-color: #2563eb;
    }
  }

  &__range-sep {
    font-size: 13px;
    color: #94a3b8;
    flex-shrink: 0;
  }

  &__overview {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-bottom: 20px;
  }

  &__row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;

    &:last-child {
      grid-template-columns: 1fr;
    }
  }

  &__card {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    padding: 16px 8px 14px;
    border: none;
    border-radius: 14px;

    &--days {
      background: linear-gradient(135deg, #eff6ff, #dbeafe);
      .v2-stats-view__card-num { color: #1d4ed8; }
      .v2-stats-view__card-label { color: #3b82f6; }
      .v2-stats-view__card-desc { color: #60a5fa; }
    }

    &--streak {
      background: linear-gradient(135deg, #fefce8, #fef08a);
      .v2-stats-view__card-num { color: #a16207; }
      .v2-stats-view__card-label { color: #ca8a04; }
      .v2-stats-view__card-desc { color: #eab308; }
    }

    &--best {
      background: linear-gradient(135deg, #f0fdf4, #bbf7d0);
      .v2-stats-view__card-num { color: #15803d; }
      .v2-stats-view__card-label { color: #22c55e; }
      .v2-stats-view__card-desc { color: #4ade80; }
    }

    &--perfect {
      background: linear-gradient(135deg, #faf5ff, #e9d5ff);
      .v2-stats-view__card-num { color: #7e22ce; }
      .v2-stats-view__card-label { color: #a855f7; }
      .v2-stats-view__card-desc { color: #c084fc; }
    }

    &--combined {
      background: linear-gradient(135deg, #f0f9ff, #bae6fd);
      flex-direction: row;
      align-items: stretch;
      justify-content: space-between;
      gap: 0;
      padding: 14px 16px;

      .v2-stats-view__card-num { color: #be123c; }
      .v2-stats-view__card-label {
        color: #0369a1;
        font-size: 11px;
      }
    }
  }

  &__combined-left {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 8px;
    min-width: 0;
  }

  &__combined-divider {
    width: 1px;
    background: rgba(148, 163, 184, 0.35);
    margin: 4px 14px;
    flex-shrink: 0;
  }

  &__combined-right {
    flex: 0 0 auto;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    min-width: 72px;
  }

  &__card-metric {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
  }

  &__card-num {
    font-size: 26px;
    font-weight: 800;
    line-height: 1.1;
  }

  &__card-label {
    font-size: 11px;
    text-align: center;
    font-weight: 600;
  }

  &__card-desc {
    font-size: 10px;
    font-weight: 500;
  }

  &__card-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__week-body {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 8px;
  }

  &__week-metric {
    display: flex;
    flex-direction: column;
    gap: 2px;
    flex: 1;

    &:last-child {
      text-align: right;
    }

    &--last {
      .v2-stats-view__week-num { color: #94a3b8; }
      .v2-stats-view__week-sub { color: #94a3b8; }
    }
  }

  &__week-num {
    font-size: 24px;
    font-weight: 800;
    color: #0369a1;
    line-height: 1.1;
  }

  &__week-sub {
    font-size: 11px;
    color: #0284c7;
    font-weight: 600;
  }

  &__week-arrow {
    font-size: 16px;
    color: #38bdf8;
    font-weight: 700;
    flex-shrink: 0;
    padding-bottom: 4px;
  }

  &__week-trend {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    font-size: 12px;
    font-weight: 700;
    padding: 2px 8px;
    border-radius: 999px;

    &--up {
      background: rgba(22, 163, 74, 0.15);
      color: #15803d;
    }

    &--down {
      background: rgba(220, 38, 38, 0.15);
      color: #be123c;
    }

    &--flat {
      background: rgba(100, 116, 139, 0.15);
      color: #475569;
    }
  }

  &__week-icon {
    width: 12px;
    height: 12px;
  }

  &__section {
    margin-bottom: 20px;
  }

  &__section-title {
    font-size: 15px;
    font-weight: 700;
    color: #1e293b;
    margin: 0 0 12px;
  }

  &__phase-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__phase-bar {
    display: flex;
    flex-direction: column;
    gap: 5px;
    padding: 12px 14px;
    background: #fff;
    border: 1.5px solid #e8ecef;
    border-radius: 12px;

    &-hd {
      display: flex;
      align-items: center;
      justify-content: space-between;

      &-left {
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;
        flex: 1;
      }
    }
  }

  &__phase-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 1px 8px;
    border-radius: 6px;
    font-size: 11px;
    font-weight: 600;
    background: color-mix(in srgb, var(--accent) 12%, transparent);
    color: var(--accent);
    flex-shrink: 0;
  }

  &__phase-name {
    font-size: 13px;
    color: #475569;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__phase-rate {
    font-size: 14px;
    font-weight: 700;
    color: var(--accent);
    flex-shrink: 0;
  }

  &__phase-track {
    height: 8px;
    background: #e5e7eb;
    border-radius: 999px;
    overflow: hidden;
  }

  &__phase-fill {
    height: 100%;
    border-radius: 999px;
    background: var(--accent);
    transition: width 0.4s ease;
  }

  &__phase-bt {
    display: flex;
    justify-content: flex-end;
  }

  &__phase-count {
    font-size: 11px;
    color: #94a3b8;
    font-weight: 500;
  }

  &__table-wrap {
    overflow-x: auto;
    background: #fff;
    border: 1.5px solid #e8ecef;
    border-radius: 12px;
  }

  &__table {
    width: 100%;
    border-collapse: collapse;
    font-size: 12px;

    th, td {
      padding: 8px 10px;
      text-align: center;
      white-space: nowrap;
    }

    thead th {
      background: #f8fafc;
      color: #64748b;
      font-weight: 600;
      font-size: 11px;
      border-bottom: 2px solid #e8ecef;
    }

    tbody tr {
      border-bottom: 1px solid #f1f5f9;

      &:last-child {
        border-bottom: none;
      }
    }
  }

  &__th-phase {
    font-size: 11px;
  }

  &__td-date {
    color: #374151;
    font-weight: 500;
  }

  &__rate-badge {
    display: inline-block;
    padding: 1px 8px;
    border-radius: 6px;
    font-weight: 600;
    font-size: 11px;

    &--high {
      background: #f0fdf4;
      color: #166534;
    }

    &--mid {
      background: #fefce8;
      color: #92400e;
    }

    &--low {
      background: #fef2f2;
      color: #991b1b;
    }
  }

  &__td-phase {
    text-align: center;
  }

  &__phase-cell {
    display: inline-block;
    padding: 1px 6px;
    border-radius: 5px;
    font-size: 11px;
    font-weight: 600;
    min-width: 28px;

    &--done {
      background: color-mix(in srgb, var(--accent) 14%, transparent);
      color: var(--accent);
    }

    &--partial {
      background: #f1f5f9;
      color: #64748b;
    }

    &--skip {
      color: #cbd5e1;
      font-weight: 400;
      background: transparent;
      padding: 1px 0;
    }
  }

  &__empty {
    text-align: center;
    color: #94a3b8;
    padding: 30px 0;
  }
}
</style>
