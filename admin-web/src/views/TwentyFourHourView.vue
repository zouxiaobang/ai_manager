<template>
  <WarRoomPage :title="t('portal.menu.24hour')" fill>
    <div class="tfh">
      <!-- 顶栏 -->
      <div class="tfh-bar">
        <div class="tfh-tabs">
          <button
            class="tfh-tab"
            :class="{ 'tfh-tab--active': activeTab === 'checklist' }"
            @click="activeTab = 'checklist'"
          >
            <svg viewBox="0 0 24 24" width="15" height="15">
              <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2M9 5a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2M9 5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2m-6 9l2 2 4-4" stroke="currentColor" stroke-width="1.8" fill="none" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            检查清单
          </button>
          <button
            class="tfh-tab"
            :class="{ 'tfh-tab--active': activeTab === 'stats' }"
            @click="activeTab = 'stats'"
          >
            <svg viewBox="0 0 24 24" width="15" height="15">
              <path d="M18 20V10M12 20V4M6 20v-6" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            统计
          </button>
        </div>
        <template v-if="activeTab === 'checklist'">
          <div class="tfh-bar__date">
            <span class="tfh-bar__date-label">{{ dateLabel }}</span>
            <input v-model="dateStr" type="date" class="tfh-bar__picker" @change="loadData" />
          </div>
          <div class="tfh-bar__progress">
            <span class="tfh-bar__progress-text">{{ completedCount }}/{{ totalCount }}</span>
            <div class="tfh-bar__track">
              <div class="tfh-bar__fill" :style="{ width: progressPercent + '%' }" />
            </div>
          </div>
        </template>
      </div>

      <!-- ────── 检查清单 ────── -->
      <template v-if="activeTab === 'checklist'">
        <!-- 前夜行为暗示（顶部） -->
        <div v-if="prevPrepHint" class="tfh-top-hint">
          <svg viewBox="0 0 24 24" width="14" height="14">
            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" fill="currentColor" />
          </svg>
          <span class="tfh-top-hint__label">昨夜暗示：</span>
          <span class="tfh-top-hint__text">{{ prevPrepHint }}</span>
        </div>

        <div class="tfh-grid">
          <div
            v-for="phase in phases"
            :key="phase.key"
            class="tfh-phase"
            :class="{ 'tfh-phase--all-done': isPhaseAllDone(phase) }"
            :style="{ '--accent': phase.accent }"
          >
            <div class="tfh-phase__top">
              <span class="tfh-phase__badge">{{ phase.badge }}</span>
              <div class="tfh-phase__ring">
                <svg viewBox="0 0 36 36" width="36" height="36">
                  <circle cx="18" cy="18" r="15.5" fill="none" stroke="#e5e7eb" stroke-width="3" />
                  <circle cx="18" cy="18" r="15.5" fill="none" :stroke="phase.accent" stroke-width="3" stroke-linecap="round" :stroke-dasharray="circumference" :stroke-dashoffset="phaseOffset(phase)" transform="rotate(-90 18 18)" />
                </svg>
                <span class="tfh-phase__ring-text">{{ phaseCompletedCount(phase) }}</span>
              </div>
            </div>
            <h3 class="tfh-phase__title">{{ phase.title }}</h3>
            <p v-if="phase.desc" class="tfh-phase__desc">{{ phase.desc }}</p>
            <div class="tfh-phase__items">
              <div v-for="item in phase.items" :key="item.key" class="tfh-item" :class="{ 'tfh-item--done': isChecked(item.key) }">
                <button
                  class="tfh-item__cb"
                  :class="{ 'tfh-item__cb--checked': isChecked(item.key) }"
                  :style="isChecked(item.key) ? { '--cb-accent': phase.accent } : {}"
                  @click="onToggle(item)"
                >
                  <svg v-if="isChecked(item.key)" viewBox="0 0 24 24" width="13" height="13">
                    <path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="2.8" fill="none" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                </button>
                <span class="tfh-item__label">{{ item.label }}</span>
                <span v-if="item.hasContent && getContent(item.key)" class="tfh-item__preview" @click.stop="openWrite(item)">{{ getContent(item.key) }}</span>
                <button
                  v-if="item.hasContent"
                  class="tfh-item__write"
                  :class="{ 'tfh-item__write--has': !!getContent(item.key) }"
                  :style="getContent(item.key) ? { '--write-accent': phase.accent } : {}"
                  @click.stop="openWrite(item)"
                >
                  <svg viewBox="0 0 24 24" width="12" height="12">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" stroke="currentColor" stroke-width="1.8" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" stroke="currentColor" stroke-width="1.8" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 前夜交付任务（底部） -->
        <div v-if="prevPrepTask" class="tfh-bottom-mission">
          <svg viewBox="0 0 24 24" width="14" height="14">
            <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span class="tfh-bottom-mission__label">昨夜定下的今日交付：</span>
          <span class="tfh-bottom-mission__text">{{ prevPrepTask }}</span>
        </div>

        <div class="tfh-foot">
          <div class="tfh-foot__item tfh-foot__item--trap">
            <span class="tfh-foot__tag">⚠ 伪努力陷阱</span>
            <span class="tfh-foot__detail">强度透支 · 工具沉迷</span>
          </div>
          <div class="tfh-foot__item tfh-foot__item--path">
            <span class="tfh-foot__tag">✓ 成长路径</span>
            <span class="tfh-foot__detail">低摩擦 · 持续 · 复盘 · 深耕 · 不断线</span>
          </div>
        </div>
      </template>

      <!-- ────── 统计 ────── -->
      <template v-if="activeTab === 'stats'">
        <div class="tfh-stats">
          <!-- 日期范围 -->
          <div class="tfh-stats__range">
            <label class="tfh-stats__range-label">统计范围</label>
            <div class="tfh-stats__range-inputs">
              <input v-model="statsStart" type="date" class="tfh-stats__input" @change="loadStats" />
              <span class="tfh-stats__range-sep">至</span>
              <input v-model="statsEnd" type="date" class="tfh-stats__input" @change="loadStats" />
            </div>
          </div>

          <!-- 总览卡片 -->
          <div class="tfh-stats__overview">
            <div class="tfh-stats__card">
              <span class="tfh-stats__card-num">{{ statsData?.summary.totalDays ?? 0 }}</span>
              <span class="tfh-stats__card-label">记录天数</span>
            </div>
            <div class="tfh-stats__card">
              <span class="tfh-stats__card-num">{{ overallRateText }}</span>
              <span class="tfh-stats__card-label">整体完成率</span>
            </div>
            <div class="tfh-stats__card">
              <span class="tfh-stats__card-num">{{ statsData?.summary.currentStreak ?? 0 }}</span>
              <span class="tfh-stats__card-label">当前连胜</span>
            </div>
            <div class="tfh-stats__card">
              <span class="tfh-stats__card-num">{{ statsData?.summary.bestStreak ?? 0 }}</span>
              <span class="tfh-stats__card-label">最长连胜</span>
            </div>
            <div class="tfh-stats__card">
              <span class="tfh-stats__card-num">{{ statsData?.summary.perfectDays ?? 0 }}</span>
              <span class="tfh-stats__card-label">完美天数</span>
            </div>
          </div>

          <!-- 各时段完成率 -->
          <div class="tfh-stats__section">
            <h3 class="tfh-stats__section-title">各时段完成率</h3>
            <div class="tfh-stats__phase-grid">
              <div v-for="p in phases" :key="p.key" class="tfh-stats__phase-bar" :style="{ '--accent': p.accent }">
                <div class="tfh-stats__phase-bar-hd">
                  <span class="tfh-stats__phase-badge">{{ p.badge }}</span>
                  <span class="tfh-stats__phase-rate">{{ phaseRateText(p.key) }}</span>
                </div>
                <div class="tfh-stats__phase-track">
                  <div class="tfh-stats__phase-fill" :style="{ width: phaseRatePercent(p.key) }" />
                </div>
              </div>
            </div>
          </div>

          <!-- 每日明细 -->
          <div class="tfh-stats__section">
            <h3 class="tfh-stats__section-title">每日明细</h3>
            <div class="tfh-stats__table-wrap">
              <table class="tfh-stats__table">
                <thead>
                  <tr>
                    <th>日期</th>
                    <th>完成</th>
                    <th>完成率</th>
                    <th v-for="p in phases" :key="p.key" class="tfh-stats__th-phase">
                      <span :style="{ color: p.accent }">{{ p.badge }}</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in statsData?.items ?? []" :key="row.date">
                    <td class="tfh-stats__td-date">{{ formatShortDate(row.date) }}</td>
                    <td>{{ row.completedCount }}/{{ row.totalCount }}</td>
                    <td>
                      <span class="tfh-stats__rate-badge" :class="rateBadgeClass(row.completionRate)">{{ (row.completionRate * 100).toFixed(0) }}%</span>
                    </td>
                    <td v-for="p in phases" :key="p.key" class="tfh-stats__td-phase-dot">
                      <span
                        v-if="getPhaseStat(row, p.key) !== undefined"
                        class="tfh-stats__dot"
                        :style="{ background: getPhaseStat(row, p.key) === 1 ? p.accent : '#e5e7eb' }"
                      />
                      <span v-else class="tfh-stats__dot-empty">—</span>
                    </td>
                  </tr>
                  <tr v-if="!(statsData?.items?.length)">
                    <td colspan="100%" class="tfh-stats__empty">暂无数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 写作弹窗 -->
    <Teleport to="body">
      <div v-if="writeItem" class="tfh-modal-mask" @click.self="closeWrite">
        <div class="tfh-modal">
          <div class="tfh-modal__hd">
            <span class="tfh-modal__badge">{{ writePhaseBadge }}</span>
            <span class="tfh-modal__title">{{ writeItem?.label }}</span>
            <button class="tfh-modal__close" @click="closeWrite">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
              </svg>
            </button>
          </div>
          <div class="tfh-modal__bd">
            <textarea ref="writeTextarea" v-model="writeContent" class="tfh-modal__textarea" rows="6" placeholder="记录你的想法…" />
            <div class="tfh-modal__actions">
              <button class="tfh-modal__btn tfh-modal__btn--cancel" @click="closeWrite">取消</button>
              <button class="tfh-modal__btn tfh-modal__btn--save" @click="saveWrite">
                <svg viewBox="0 0 24 24" width="14" height="14">
                  <path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                提交
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 撤销确认弹窗 -->
    <Teleport to="body">
      <div v-if="uncheckItem" class="tfh-modal-mask" @click.self="cancelUncheck">
        <div class="tfh-modal tfh-modal--sm">
          <div class="tfh-modal__hd">
            <span class="tfh-modal__title">撤销确认</span>
          </div>
          <div class="tfh-modal__bd">
            <p class="tfh-modal__confirm-text">确定要撤销 "{{ uncheckItem }}" 吗？</p>
            <div class="tfh-modal__actions">
              <button class="tfh-modal__btn tfh-modal__btn--cancel" @click="cancelUncheck">取消</button>
              <button class="tfh-modal__btn tfh-modal__btn--danger" @click="confirmUncheck">
                <svg viewBox="0 0 24 24" width="14" height="14">
                  <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                确认撤销
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </WarRoomPage>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
import { fetchDailyChecklist, saveDailyChecklist, fetchDailyChecklistStats } from '@/api/dailyChecklist'
import type { DailyChecklistItem, DailyChecklistStats, DailyStatsItem, PhaseStatsItem } from '@/api/dailyChecklist'
import { phases, getCurrentPhase, type ItemDef, type PhaseDef } from '@/data/24hour-phases'
import { dismiss24HourNotification, markPhaseNotified } from '@/composables/use24HourNotification'

const { t } = useI18n()

// ─── Tab ────────────────────────────
const activeTab = ref<'checklist' | 'stats'>('checklist')

// ─── 日期 ──────────────────────────
const today = new Date().toISOString().split('T')[0]
const dateStr = ref(today)
const dateLabel = computed(() => {
  if (dateStr.value === today) return '今天'
  const parts = dateStr.value.split('-')
  const d = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]))
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  return `${d.getMonth() + 1}月${d.getDate()}日 ${week}`
})

// ─── 统计日期范围 ──────────────────
function defaultStatsRange() {
  const end = new Date()
  const start = new Date(end)
  start.setDate(start.getDate() - 29)
  return { start: start.toISOString().split('T')[0], end: end.toISOString().split('T')[0] }
}
const defaultRange = defaultStatsRange()
const statsStart = ref(defaultRange.start)
const statsEnd = ref(defaultRange.end)

// ─── 数据 ──────────────────────────
const checklistMap = reactive<Record<string, DailyChecklistItem>>({})
const prevChecklistMap = reactive<Record<string, DailyChecklistItem>>({})
const statsData = ref<DailyChecklistStats | null>(null)
const circumference = 2 * Math.PI * 15.5

// ─── 前夜数据 ──────────────────────
const previousDate = computed(() => {
  const parts = dateStr.value.split('-')
  const d = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]))
  d.setDate(d.getDate() - 1)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
})

const prevPrepTask = computed(() => prevChecklistMap['prep_task']?.content || '')
const prevPrepHint = computed(() => prevChecklistMap['prep_hint']?.content || '')

// ─── 进度 ──────────────────────────
const allItemKeys = phases.flatMap(p => p.items.map(i => i.key))
const totalCount = computed(() => allItemKeys.length)
const completedCount = computed(() => allItemKeys.filter(k => isChecked(k)).length)
const progressPercent = computed(() => totalCount.value ? Math.round((completedCount.value / totalCount.value) * 100) : 0)

function phaseCompletedCount(phase: PhaseDef) {
  return phase.items.filter(i => isChecked(i.key)).length
}

function isPhaseAllDone(phase: PhaseDef) {
  return phase.items.every(i => isChecked(i.key))
}

function phaseOffset(phase: PhaseDef) {
  const c = circumference
  const total = phase.items.length
  const done = phaseCompletedCount(phase)
  if (total === 0) return c
  return c - (done / total) * c
}

function isChecked(key: string) {
  return checklistMap[key]?.completed === 1
}

function getContent(key: string) {
  return checklistMap[key]?.content || ''
}

// ─── 写作弹窗 ──────────────────────
const writeItem = ref<ItemDef | null>(null)
const writePhaseBadge = ref('')
const writeContent = ref('')
const writeTextarea = ref<HTMLTextAreaElement | null>(null)

function openWrite(item: ItemDef) {
  const phase = phases.find(p => p.items.some(i => i.key === item.key))
  writePhaseBadge.value = phase?.badge || ''
  writeItem.value = item
  writeContent.value = getContent(item.key)
  nextTick(() => writeTextarea.value?.focus())
}

function closeWrite() {
  writeItem.value = null
  writeContent.value = ''
}

function saveWrite() {
  if (!writeItem.value) return
  const key = writeItem.value.key
  const content = writeContent.value
  checklistMap[key] = { itemKey: key, completed: content.trim() ? 1 : 0, content }
  closeWrite()
  debouncedSave()
}

// ─── 勾选 / 撤销 ──────────────────
const uncheckItem = ref<string | null>(null)
let pendingUncheckKey = ''

function onToggle(item: ItemDef) {
  if (isChecked(item.key)) {
    pendingUncheckKey = item.key
    uncheckItem.value = item.label
    return
  }
  if (item.hasContent) {
    openWrite(item)
    return
  }
  checklistMap[item.key] = { itemKey: item.key, completed: 1, content: null }
  debouncedSave()
}

function cancelUncheck() {
  uncheckItem.value = null
  pendingUncheckKey = ''
}

function confirmUncheck() {
  if (pendingUncheckKey) {
    checklistMap[pendingUncheckKey] = { itemKey: pendingUncheckKey, completed: 0, content: getContent(pendingUncheckKey) }
    debouncedSave()
  }
  cancelUncheck()
}

// ─── 自动保存 ──────────────────────
let saveTimer: ReturnType<typeof setTimeout> | null = null
function debouncedSave() {
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(doSave, 600)
}

async function doSave() {
  const items = Object.values(checklistMap).map(item => ({ itemKey: item.itemKey, completed: item.completed, content: item.content }))
  try { await saveDailyChecklist({ date: dateStr.value, items }) } catch { /* 静默 */ }
}

// ─── 加载清单数据 ──────────────────
async function loadData() {
  try {
    const [data, prevData] = await Promise.all([
      fetchDailyChecklist(dateStr.value),
      fetchDailyChecklist(previousDate.value),
    ])
    Object.keys(checklistMap).forEach(k => delete checklistMap[k])
    data.forEach(item => { checklistMap[item.itemKey] = item })
    Object.keys(prevChecklistMap).forEach(k => delete prevChecklistMap[k])
    prevData.forEach(item => { prevChecklistMap[item.itemKey] = item })
  } catch { /* 静默 */ }
}

// ─── 统计 ──────────────────────────
const overallRateText = computed(() => {
  if (!statsData.value) return '—'
  return (statsData.value.summary.overallRate * 100).toFixed(1) + '%'
})

function phaseRateText(phaseKey: string) {
  if (!statsData.value?.items.length) return '—'
  const total = statsData.value.items.reduce((s, d) => s + (d.phaseStats.find(p => p.phaseKey === phaseKey)?.totalCount ?? 0), 0)
  const done = statsData.value.items.reduce((s, d) => s + (d.phaseStats.find(p => p.phaseKey === phaseKey)?.completedCount ?? 0), 0)
  if (total === 0) return '—'
  return (done / total * 100).toFixed(0) + '%'
}

function phaseRatePercent(phaseKey: string) {
  if (!statsData.value?.items.length) return '0%'
  const total = statsData.value.items.reduce((s, d) => s + (d.phaseStats.find(p => p.phaseKey === phaseKey)?.totalCount ?? 0), 0)
  const done = statsData.value.items.reduce((s, d) => s + (d.phaseStats.find(p => p.phaseKey === phaseKey)?.completedCount ?? 0), 0)
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
  if (rate >= 0.8) return 'tfh-stats__rate-badge--high'
  if (rate >= 0.5) return 'tfh-stats__rate-badge--mid'
  return 'tfh-stats__rate-badge--low'
}

async function loadStats() {
  try {
    statsData.value = await fetchDailyChecklistStats(statsStart.value, statsEnd.value)
  } catch { /* 静默 */ }
}

// ─── 初始化 ──────────────────────────
onMounted(() => {
  loadData()
  loadStats()
  dismiss24HourNotification()
  const current = getCurrentPhase()
  if (current) markPhaseNotified(current.key)
})
</script>

<style scoped lang="scss">
.tfh {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

/* ─────── 顶栏 ─────── */
.tfh-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 20px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;

  &__date {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-left: auto;
  }

  &__date-label {
    font-size: 14px;
    font-weight: 600;
    color: #111827;
  }

  &__picker {
    padding: 4px 8px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 13px;
    color: #374151;
    background: #fff;
    cursor: pointer;
    &:focus { outline: none; border-color: #2563eb; }
  }

  &__progress {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__progress-text { font-size: 13px; color: #6b7280; min-width: 40px; text-align: right; }

  &__track { width: 100px; height: 6px; background: #e5e7eb; border-radius: 3px; overflow: hidden; }

  &__fill { height: 100%; background: #2563eb; border-radius: 3px; transition: width 0.3s ease; }
}

.tfh-tabs {
  display: flex;
  gap: 4px;
  background: #f3f4f6;
  border-radius: 8px;
  padding: 3px;
}

.tfh-tab {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 14px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  background: transparent;
  cursor: pointer;
  transition: all 0.12s;

  &:hover { color: #374151; }

  &--active {
    background: #fff;
    color: #111827;
    box-shadow: 0 1px 3px rgba(0,0,0,.08);
  }
}

/* ─────── 卡片网格 ─────── */
.tfh-grid {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px 20px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  align-content: start;

  @media (max-width: 1024px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 640px) { grid-template-columns: 1fr; }
}

.tfh-phase {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  transition: box-shadow 0.2s;
  display: flex;
  flex-direction: column;

  &:hover { box-shadow: 0 2px 12px rgba(0,0,0,.07); }

  &--all-done { background: color-mix(in srgb, var(--accent) 10%, #fff); border-color: color-mix(in srgb, var(--accent) 25%, transparent); }

  &__top { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 10px; }

  &__badge {
    display: inline-flex; align-items: center; justify-content: center;
    padding: 2px 8px; border-radius: 5px; font-size: 11px; font-weight: 600;
    background: color-mix(in srgb, var(--accent) 12%, transparent); color: var(--accent);
  }

  &__ring { position: relative; width: 36px; height: 36px; flex-shrink: 0;
    svg { display: block; }
    &-text { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; color: var(--accent); }
  }

  &__title { font-size: 14px; font-weight: 600; color: #111827; margin: 0 0 4px; line-height: 1.3; }

  &__desc { font-size: 12px; color: #6b7280; margin: 0 0 10px; line-height: 1.5; }

  &__items { display: flex; flex-direction: column; gap: 1px; }
}

/* ─────── 检查项 ─────── */
.tfh-item {
  display: flex; align-items: center; gap: 7px; padding: 6px 6px; border-radius: 6px; transition: background 0.1s;
  &:hover { background: #f9fafb; }
  &--done { opacity: .5;
    .tfh-item__label { text-decoration: line-through; }
  }
  &__cb {
    width: 18px; height: 18px; flex-shrink: 0; border-radius: 5px; border: 1.5px solid #d1d5db;
    background: #fff; display: inline-flex; align-items: center; justify-content: center;
    cursor: pointer; padding: 0; transition: all 0.15s; color: #fff;
    &:hover { border-color: var(--accent, #2563eb); }
    &--checked { background: var(--cb-accent, #2563eb); border-color: var(--cb-accent, #2563eb); }
  }
  &__label { flex: 1; min-width: 0; font-size: 13px; color: #374151; line-height: 1.4; }
  &__preview { font-size: 11px; color: #6b7280; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 90px; cursor: pointer;
    &::before { content: '「'; } &::after { content: '」'; }
    &:hover { color: #2563eb; }
  }
  &__write {
    flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center;
    width: 22px; height: 22px; border: none; border-radius: 4px; background: transparent; color: #9ca3af; cursor: pointer; padding: 0; transition: all 0.12s;
    &:hover { background: color-mix(in srgb, var(--write-accent, #2563eb) 10%, transparent); color: var(--write-accent, #2563eb); }
    &--has { color: var(--write-accent, #2563eb); }
  }
}

/* ─────── 底部提示 ─────── */
.tfh-foot {
  display: flex; gap: 8px; padding: 8px 16px 12px; border-top: 1px solid #f3f4f6; flex-shrink: 0; flex-wrap: wrap;
}

.tfh-foot__item { display: flex; align-items: center; gap: 6px; padding: 5px 10px; border-radius: 6px; font-size: 11px; line-height: 1.4;
  &--trap { background: #fef2f2; color: #991b1b; }
  &--path { background: #f0fdf4; color: #166534; }
}

.tfh-foot__tag { font-weight: 600; white-space: nowrap; }
.tfh-foot__detail { opacity: 0.8; }

/* ─────── 统计页面 ─────── */
.tfh-stats {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px 30px;

  &__range {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 18px;
  }

  &__range-label {
    font-size: 13px;
    font-weight: 600;
    color: #374151;
  }

  &__range-inputs {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &__input {
    padding: 5px 8px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 13px;
    color: #374151;
    background: #fff;
    &:focus { outline: none; border-color: #2563eb; }
  }

  &__range-sep {
    font-size: 13px;
    color: #9ca3af;
  }

  &__overview {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 10px;
    margin-bottom: 20px;

    @media (max-width: 768px) { grid-template-columns: repeat(3, 1fr); }
    @media (max-width: 480px) { grid-template-columns: repeat(2, 1fr); }
  }

  &__card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 16px 10px;
    background: #fff;
    border: 1px solid #f0f0f0;
    border-radius: 10px;
  }

  &__card-num {
    font-size: 22px;
    font-weight: 700;
    color: #111827;
  }

  &__card-label {
    font-size: 12px;
    color: #6b7280;
  }

  &__section {
    margin-bottom: 20px;
  }

  &__section-title {
    font-size: 14px;
    font-weight: 600;
    color: #111827;
    margin: 0 0 12px;
  }

  &__phase-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;

    @media (max-width: 768px) { grid-template-columns: repeat(2, 1fr); }
    @media (max-width: 480px) { grid-template-columns: 1fr; }
  }

  &__phase-bar {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 12px;
    background: #fff;
    border: 1px solid #f0f0f0;
    border-radius: 10px;

    &-hd {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
  }

  &__phase-badge {
    display: inline-flex; align-items: center; justify-content: center;
    padding: 1px 6px; border-radius: 4px; font-size: 11px; font-weight: 600;
    background: color-mix(in srgb, var(--accent) 12%, transparent); color: var(--accent);
  }

  &__phase-rate {
    font-size: 13px;
    font-weight: 700;
    color: var(--accent);
  }

  &__phase-track {
    height: 8px;
    background: #e5e7eb;
    border-radius: 4px;
    overflow: hidden;
  }

  &__phase-fill {
    height: 100%;
    border-radius: 4px;
    background: var(--accent);
    transition: width 0.3s;
  }

  &__table-wrap {
    overflow-x: auto;
    background: #fff;
    border: 1px solid #f0f0f0;
    border-radius: 10px;
  }

  &__table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;

    th, td {
      padding: 8px 12px;
      text-align: center;
      white-space: nowrap;
    }

    thead th {
      background: #f9fafb;
      color: #6b7280;
      font-weight: 500;
      border-bottom: 1px solid #e5e7eb;
    }

    tbody tr {
      border-bottom: 1px solid #f3f4f6;
      &:hover { background: #f9fafb; }
    }
  }

  &__th-phase { font-size: 12px; }

  &__td-date { color: #374151; font-weight: 500; }

  &__rate-badge {
    display: inline-block;
    padding: 1px 8px;
    border-radius: 4px;
    font-weight: 600;
    font-size: 12px;

    &--high { background: #f0fdf4; color: #166534; }
    &--mid { background: #fefce8; color: #92400e; }
    &--low { background: #fef2f2; color: #991b1b; }
  }

  &__td-phase-dot { text-align: center; }

  &__dot {
    display: inline-block;
    width: 10px;
    height: 10px;
    border-radius: 50%;
  }

  &__dot-empty { color: #d1d5db; font-size: 12px; }

  &__empty {
    text-align: center;
    color: #9ca3af;
    padding: 30px 0;
  }
}

/* ─────── 前夜提示 ─────── */
.tfh-top-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  margin: 8px 20px 0;
  background: #fefce8;
  border: 1px solid #fde68a;
  border-radius: 8px;
  font-size: 13px;
  color: #92400e;
  flex-shrink: 0;

  svg { flex-shrink: 0; color: #f59e0b; }

  &__label { font-weight: 600; white-space: nowrap; }

  &__text { line-height: 1.5; }
}

.tfh-bottom-mission {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  margin: 0 20px 8px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  font-size: 13px;
  color: #1e40af;
  flex-shrink: 0;

  svg { flex-shrink: 0; color: #3b82f6; }

  &__label { font-weight: 600; white-space: nowrap; }

  &__text { line-height: 1.5; }
}

/* ─────── 弹窗 ─────── */
.tfh-modal-mask {
  position: fixed; inset: 0; z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,.32); animation: tfh-fade-in 0.15s ease;
}

.tfh-modal {
  width: 420px; max-width: 90vw; background: #fff; border-radius: 14px;
  box-shadow: 0 8px 30px rgba(0,0,0,.15); overflow: hidden;
  &--sm { width: 340px; }
  &__hd { display: flex; align-items: center; gap: 8px; padding: 16px 18px 0; }
  &__badge { display: inline-flex; align-items: center; justify-content: center; padding: 1px 6px; border-radius: 4px; font-size: 11px; font-weight: 600; background: #eff6ff; color: #2563eb; }
  &__title { flex: 1; font-size: 15px; font-weight: 600; color: #111827; }
  &__close { flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; width: 28px; height: 28px; border: none; border-radius: 6px; background: transparent; color: #9ca3af; cursor: pointer; padding: 0;
    &:hover { background: #f3f4f6; color: #374151; }
  }
  &__bd { padding: 14px 18px 18px; }
  &__textarea { width: 100%; padding: 10px 12px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px; color: #374151; line-height: 1.6; resize: vertical; font-family: inherit; box-sizing: border-box;
    &:focus { outline: none; border-color: #2563eb; box-shadow: 0 0 0 2px rgba(37,99,235,.12); }
    &::placeholder { color: #9ca3af; }
  }
  &__confirm-text { margin: 0 0 16px; font-size: 14px; color: #374151; line-height: 1.5; }
  &__actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 14px; }
  &__btn { display: inline-flex; align-items: center; gap: 5px; padding: 7px 16px; border-radius: 7px; font-size: 13px; font-weight: 500; border: none; cursor: pointer; transition: all 0.12s;
    &--cancel { background: #f3f4f6; color: #374151; &:hover { background: #e5e7eb; } }
    &--save { background: #2563eb; color: #fff; &:hover { background: #1d4ed8; } }
    &--danger { background: #dc2626; color: #fff; &:hover { background: #b91c1c; } }
  }
}

@keyframes tfh-fade-in { from { opacity: 0; } to { opacity: 1; } }
</style>
