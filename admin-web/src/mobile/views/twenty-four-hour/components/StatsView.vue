<template>
  <div class="stats-view">
    <!-- 日期范围选择 -->
    <div class="stats-view__range">
      <span class="stats-view__range-label">统计范围</span>
      <div class="stats-view__range-inputs">
        <input
          v-model="statsStart"
          type="date"
          class="stats-view__input"
          @change="loadStats"
        />
        <span class="stats-view__range-sep">至</span>
        <input
          v-model="statsEnd"
          type="date"
          class="stats-view__input"
          @change="loadStats"
        />
      </div>
    </div>

    <!-- 总览卡片 -->
    <div class="stats-view__overview">
      <div class="stats-view__card">
        <span class="stats-view__card-num">{{ statsData?.summary.totalDays ?? 0 }}</span>
        <span class="stats-view__card-label">记录天数</span>
      </div>
      <div class="stats-view__card">
        <span class="stats-view__card-num">{{ overallRateText }}</span>
        <span class="stats-view__card-label">整体完成率</span>
      </div>
      <div class="stats-view__card">
        <span class="stats-view__card-num">{{ statsData?.summary.currentStreak ?? 0 }}</span>
        <span class="stats-view__card-label">当前连胜</span>
      </div>
      <div class="stats-view__card">
        <span class="stats-view__card-num">{{ statsData?.summary.bestStreak ?? 0 }}</span>
        <span class="stats-view__card-label">最长连胜</span>
      </div>
      <div class="stats-view__card">
        <span class="stats-view__card-num">{{ statsData?.summary.perfectDays ?? 0 }}</span>
        <span class="stats-view__card-label">完美天数</span>
      </div>
    </div>

    <!-- 各时段完成率 -->
    <div class="stats-view__section">
      <h3 class="stats-view__section-title">各时段完成率</h3>
      <div class="stats-view__phase-list">
        <div
          v-for="p in phases"
          :key="p.key"
          class="stats-view__phase-bar"
          :style="{ '--accent': p.accent }"
        >
          <div class="stats-view__phase-bar-hd">
            <span class="stats-view__phase-badge">{{ p.badge }}</span>
            <span class="stats-view__phase-rate">{{ phaseRateText(p.key) }}</span>
          </div>
          <div class="stats-view__phase-track">
            <div
              class="stats-view__phase-fill"
              :style="{ width: phaseRatePercent(p.key) }"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 每日明细 -->
    <div class="stats-view__section">
      <h3 class="stats-view__section-title">每日明细</h3>
      <div class="stats-view__table-wrap">
        <table class="stats-view__table">
          <thead>
            <tr>
              <th>日期</th>
              <th>完成</th>
              <th>完成率</th>
              <th
                v-for="p in phases"
                :key="p.key"
                class="stats-view__th-phase"
              >
                <span :style="{ color: p.accent }">{{ p.badge }}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in statsData?.items ?? []" :key="row.date">
              <td class="stats-view__td-date">{{ formatShortDate(row.date) }}</td>
              <td>{{ row.completedCount }}/{{ row.totalCount }}</td>
              <td>
                <span
                  class="stats-view__rate-badge"
                  :class="rateBadgeClass(row.completionRate)"
                >
                  {{ (row.completionRate * 100).toFixed(0) }}%
                </span>
              </td>
              <td
                v-for="p in phases"
                :key="p.key"
                class="stats-view__td-phase-dot"
              >
                <span
                  v-if="getPhaseStat(row, p.key) !== undefined"
                  class="stats-view__dot"
                  :style="{ background: getPhaseStat(row, p.key) === 1 ? p.accent : '#e5e7eb' }"
                />
                <span v-else class="stats-view__dot-empty">—</span>
              </td>
            </tr>
            <tr v-if="!(statsData?.items?.length)">
              <td colspan="100%" class="stats-view__empty">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchDailyChecklistStats } from '@/api/dailyChecklist'
import type { DailyChecklistStats, DailyStatsItem } from '@/api/dailyChecklist'
import type { PhaseDef } from '@/data/24hour-phases'

defineProps<{
  phases: PhaseDef[]
}>()

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
  if (!statsData.value) return '—'
  return (statsData.value.summary.overallRate * 100).toFixed(1) + '%'
})

function phaseRateText(phaseKey: string) {
  if (!statsData.value?.items.length) return '—'
  const total = statsData.value.items.reduce(
    (s, d) => s + (d.phaseStats.find(p => p.phaseKey === phaseKey)?.totalCount ?? 0),
    0,
  )
  const done = statsData.value.items.reduce(
    (s, d) => s + (d.phaseStats.find(p => p.phaseKey === phaseKey)?.completedCount ?? 0),
    0,
  )
  if (total === 0) return '—'
  return (done / total * 100).toFixed(0) + '%'
}

function phaseRatePercent(phaseKey: string) {
  if (!statsData.value?.items.length) return '0%'
  const total = statsData.value.items.reduce(
    (s, d) => s + (d.phaseStats.find(p => p.phaseKey === phaseKey)?.totalCount ?? 0),
    0,
  )
  const done = statsData.value.items.reduce(
    (s, d) => s + (d.phaseStats.find(p => p.phaseKey === phaseKey)?.completedCount ?? 0),
    0,
  )
  if (total === 0) return '0%'
  return (done / total * 100).toFixed(0) + '%'
}

function getPhaseStat(row: DailyStatsItem, phaseKey: string): number | undefined {
  const ps = row.phaseStats.find(p => p.phaseKey === phaseKey)
  if (!ps) return undefined
  if (ps.totalCount === 0) return undefined
  return ps.completionRate >= 1 ? 1 : 0
}

function formatShortDate(dateStr: string) {
  const [, m, d] = dateStr.split('-')
  return `${parseInt(m)}/${parseInt(d)}`
}

function rateBadgeClass(rate: number) {
  if (rate >= 0.8) return 'stats-view__rate-badge--high'
  if (rate >= 0.5) return 'stats-view__rate-badge--mid'
  return 'stats-view__rate-badge--low'
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
.stats-view {
  padding: 0 16px 20px;

  &__range {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 14px;
  }

  &__range-label {
    font-size: 13px;
    font-weight: 600;
    color: #374151;
    font-family: 'ZCOOL KuaiLe', sans-serif;
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
    padding: 6px 10px;
    border: 2px solid #e2e8f0;
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
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    margin-bottom: 18px;
  }

  &__card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 14px 8px;
    background: #fff;
    border: 2px solid #e2e8f0;
    border-radius: 12px;
  }

  &__card-num {
    font-size: 20px;
    font-weight: 800;
    color: #1e293b;
    font-family: 'ZCOOL KuaiLe', sans-serif;
  }

  &__card-label {
    font-size: 11px;
    color: #64748b;
    text-align: center;
  }

  &__section {
    margin-bottom: 18px;
  }

  &__section-title {
    font-size: 15px;
    font-weight: 700;
    color: #1e293b;
    margin: 0 0 10px;
    font-family: 'ZCOOL KuaiLe', sans-serif;
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
    padding: 10px 12px;
    background: #fff;
    border: 2px solid #e2e8f0;
    border-radius: 10px;

    &-hd {
      display: flex;
      align-items: center;
      justify-content: space-between;
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
    font-family: 'ZCOOL KuaiLe', sans-serif;
  }

  &__phase-rate {
    font-size: 13px;
    font-weight: 700;
    color: var(--accent);
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
    transition: width 0.3s ease;
  }

  &__table-wrap {
    overflow-x: auto;
    background: #fff;
    border: 2px solid #e2e8f0;
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
      font-weight: 500;
      border-bottom: 2px solid #e2e8f0;
      font-family: 'ZCOOL KuaiLe', sans-serif;
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

  &__td-phase-dot {
    text-align: center;
  }

  &__dot {
    display: inline-block;
    width: 10px;
    height: 10px;
    border-radius: 50%;
  }

  &__dot-empty {
    color: #d1d5db;
    font-size: 12px;
  }

  &__empty {
    text-align: center;
    color: #94a3b8;
    padding: 30px 0;
  }
}
</style>
