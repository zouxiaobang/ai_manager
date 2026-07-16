<template>
  <div class="v2-24h">
      <div class="v2-24h__progress-card">
        <div class="v2-24h__progress-hd">
          <span class="v2-24h__today">{{ today }}</span>
          <span class="v2-24h__progress-count">{{ completedCount }}/{{ totalCount }}</span>
        </div>
        <div class="v2-24h__progress-track">
          <div class="v2-24h__progress-fill" :style="{ width: progressPercent + '%' }" />
        </div>
        <div class="v2-24h__progress-bt">
          <span class="v2-24h__progress-label">今日完成</span>
          <span class="v2-24h__progress-percent">{{ progressPercent }}%</span>
        </div>
      </div>

      <!-- 前夜暗示 -->
      <div v-if="prevPrepHint" class="v2-24h__hint v2-24h__hint--yellow">
        <div class="v2-24h__hint-icon">
          <svg viewBox="0 0 24 24" width="16" height="16">
            <path d="M12 2l3 6.09 6.73.97-4.87 4.74 1.15 6.7L12 17.77 5.99 20.5l1.15-6.7L2.27 9.06 9 8.09 12 2z" fill="currentColor" />
          </svg>
        </div>
        <div class="v2-24h__hint-body">
          <span class="v2-24h__hint-label">昨夜暗示</span>
          <span class="v2-24h__hint-text">{{ prevPrepHint }}</span>
        </div>
      </div>

      <!-- 下午提醒 -->
      <div v-if="afternoonReminder" class="v2-24h__hint v2-24h__hint--blue">
        <div class="v2-24h__hint-icon">
          <svg viewBox="0 0 24 24" width="16" height="16">
            <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.8" fill="none" />
            <path d="M12 7v5l3 3" stroke="currentColor" stroke-width="1.8" fill="none" stroke-linecap="round" />
          </svg>
        </div>
        <div class="v2-24h__hint-body">
          <span class="v2-24h__hint-label">下午提示</span>
          <span class="v2-24h__hint-text">{{ afternoonReminder }}</span>
        </div>
      </div>

      <!-- 昨夜交付任务 -->
      <div v-if="prevPrepTask" class="v2-24h__hint v2-24h__hint--green">
        <div class="v2-24h__hint-icon">
          <svg viewBox="0 0 24 24" width="16" height="16">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" stroke="currentColor" stroke-width="1.8" fill="none" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M22 4L12 14.01l-3-3" stroke="currentColor" stroke-width="1.8" fill="none" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
        <div class="v2-24h__hint-body">
          <span class="v2-24h__hint-label">今日交付</span>
          <span class="v2-24h__hint-text">{{ prevPrepTask }}</span>
        </div>
      </div>

      <!-- 当前时段横幅 -->
      <div v-if="currentPhase" class="v2-24h__current-banner" :style="{ '--accent': currentPhase.accent }">
        <div class="v2-24h__current-banner-left">
          <span class="v2-24h__current-badge">{{ currentPhase.badge }}</span>
          <div class="v2-24h__current-info">
            <span class="v2-24h__current-title">{{ currentPhase.title }}</span>
            <span class="v2-24h__current-sub">还有 {{ phaseRemainingCount(currentPhase) }} 项待完成</span>
          </div>
        </div>
        <div class="v2-24h__current-ring">
          <svg viewBox="0 0 40 40" width="40" height="40">
            <circle cx="20" cy="20" r="16" fill="none" stroke="rgba(255,255,255,0.3)" stroke-width="3" />
            <circle
              cx="20" cy="20" r="16"
              fill="none"
              stroke="#fff"
              stroke-width="3"
              stroke-linecap="round"
              :stroke-dasharray="circumference"
              :stroke-dashoffset="phaseRingOffset(currentPhase)"
              transform="rotate(-90 20 20)"
            />
          </svg>
          <span class="v2-24h__current-ring-text">{{ phaseCompletedCount(currentPhase) }}</span>
        </div>
      </div>

      <!-- 时段列表 -->
      <div class="v2-24h__phase-list">
        <V2PhaseCard
          v-for="phase in phases"
          :key="phase.key"
          :phase="phase"
          :is-current="currentPhase?.key === phase.key"
          :all-done="isPhaseAllDone(phase)"
          :completed-count="phaseCompletedCount(phase)"
          :collapsed="collapsedPhases.has(phase.key)"
          @toggle-collapse="togglePhaseCollapse(phase.key)"
        >
          <template #item="{ item }">
            <V2ChecklistItem
              :item="item"
              :checked="isChecked(item.key)"
              :content="getContent(item.key)"
              :accent="phase.accent"
              @toggle="onToggleItem(item)"
              @open-write="openWrite(item)"
            />
          </template>
        </V2PhaseCard>
      </div>

      <!-- 底部标签 -->
      <div class="v2-24h__footer">
        <span class="v2-24h__footer-tag v2-24h__footer-tag--warn">伪努力陷阱</span>
        <span class="v2-24h__footer-tag v2-24h__footer-tag--ok">成长路径</span>
      </div>

      <!-- 写作弹窗 -->
      <Transition name="v2-24h-overlay">
        <div v-if="writeSheetVisible" class="v2-24h__overlay" @click.self="closeWrite">
          <Transition name="v2-24h-sheet" appear>
            <div v-if="writeSheetVisible" class="v2-24h__sheet">
              <div class="v2-24h__sheet-hd">
                <div class="v2-24h__sheet-hd-left">
                  <span v-if="writePhaseBadge" class="v2-24h__sheet-badge">{{ writePhaseBadge }}</span>
                  <span class="v2-24h__sheet-title">{{ writeItem?.label || '' }}</span>
                </div>
                <button type="button" class="v2-24h__sheet-close" @click="closeWrite">
                  <svg viewBox="0 0 24 24" width="20" height="20">
                    <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                  </svg>
                </button>
              </div>
              <div class="v2-24h__sheet-body">
                <textarea
                  v-model="writeContent"
                  class="v2-24h__write-textarea"
                  rows="6"
                  placeholder="记录你的想法…"
                  ref="writeTextareaRef"
                />
                <div class="v2-24h__sheet-actions">
                  <button class="v2-24h__btn v2-24h__btn--cancel" @click="closeWrite">取消</button>
                  <button class="v2-24h__btn v2-24h__btn--save" @click="saveWrite">提交</button>
                </div>
              </div>
            </div>
          </Transition>
        </div>
      </Transition>

      <!-- 撤销确认弹窗 -->
      <Transition name="v2-24h-overlay">
        <div v-if="uncheckSheetVisible" class="v2-24h__overlay" @click.self="cancelUncheck">
          <Transition name="v2-24h-sheet" appear>
            <div v-if="uncheckSheetVisible" class="v2-24h__sheet v2-24h__sheet--sm">
              <div class="v2-24h__sheet-hd">
                <span class="v2-24h__sheet-title">撤销确认</span>
                <button type="button" class="v2-24h__sheet-close" @click="cancelUncheck">
                  <svg viewBox="0 0 24 24" width="20" height="20">
                    <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                  </svg>
                </button>
              </div>
              <div class="v2-24h__sheet-body">
                <p class="v2-24h__uncheck-text">确定要撤销「{{ uncheckItemLabel }}」吗？</p>
                <div class="v2-24h__sheet-actions">
                  <button class="v2-24h__btn v2-24h__btn--cancel" @click="cancelUncheck">取消</button>
                  <button class="v2-24h__btn v2-24h__btn--danger" @click="confirmUncheck">确认撤销</button>
                </div>
              </div>
            </div>
          </Transition>
        </div>
      </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import V2PhaseCard from './components/V2PhaseCard.vue'
import V2ChecklistItem from './components/V2ChecklistItem.vue'
import { fetchDailyChecklist, saveDailyChecklist } from '@/api/dailyChecklist'
import type { DailyChecklistItem } from '@/api/dailyChecklist'
import { phases, getCurrentPhase, type ItemDef, type PhaseDef } from '@/data/24hour-phases'

const today = new Date().toISOString().split('T')[0]
const checklistMap = reactive<Record<string, DailyChecklistItem>>({})
const prevChecklistMap = reactive<Record<string, DailyChecklistItem>>({})

const collapsedPhases = reactive(new Set<string>())
const circumference = 2 * Math.PI * 16

const currentPhase = computed(() => getCurrentPhase())

const previousDate = computed(() => {
  const parts = today.split('-')
  const d = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]))
  d.setDate(d.getDate() - 1)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
})

const prevPrepTask = computed(() => prevChecklistMap['prep_task']?.content || '')
const prevPrepHint = computed(() => prevChecklistMap['prep_hint']?.content || '')
const afternoonReminder = computed(() => checklistMap['focus_tomorrow_reminder']?.content || '')

const allItemKeys = phases.flatMap(p => p.items.map(i => i.key))
const totalCount = computed(() => allItemKeys.length)
const completedCount = computed(() => allItemKeys.filter(k => isChecked(k)).length)
const progressPercent = computed(() => totalCount.value ? Math.round((completedCount.value / totalCount.value) * 100) : 0)

function phaseCompletedCount(phase: PhaseDef) {
  return phase.items.filter(i => isChecked(i.key)).length
}

function phaseRemainingCount(phase: PhaseDef) {
  return phase.items.length - phaseCompletedCount(phase)
}

function isPhaseAllDone(phase: PhaseDef) {
  return phase.items.every(i => isChecked(i.key))
}

function phaseRingOffset(phase: PhaseDef) {
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

function togglePhaseCollapse(key: string) {
  if (collapsedPhases.has(key)) {
    collapsedPhases.delete(key)
  } else {
    collapsedPhases.add(key)
  }
}

const writeSheetVisible = ref(false)
const writeItem = ref<ItemDef | null>(null)
const writePhaseBadge = ref('')
const writeContent = ref('')
const writeTextareaRef = ref<HTMLTextAreaElement | null>(null)

function openWrite(item: ItemDef) {
  const phase = phases.find(p => p.items.some(i => i.key === item.key))
  writePhaseBadge.value = phase?.badge || ''
  writeItem.value = item
  writeContent.value = getContent(item.key)
  writeSheetVisible.value = true
  nextTick(() => writeTextareaRef.value?.focus())
}

function closeWrite() {
  writeSheetVisible.value = false
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

const uncheckSheetVisible = ref(false)
const uncheckItemLabel = ref('')
let pendingUncheckKey = ''

function onToggleItem(item: ItemDef) {
  if (isChecked(item.key)) {
    pendingUncheckKey = item.key
    uncheckItemLabel.value = item.label
    uncheckSheetVisible.value = true
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
  uncheckSheetVisible.value = false
  pendingUncheckKey = ''
}

function confirmUncheck() {
  if (pendingUncheckKey) {
    checklistMap[pendingUncheckKey] = {
      itemKey: pendingUncheckKey,
      completed: 0,
      content: getContent(pendingUncheckKey),
    }
    debouncedSave()
  }
  cancelUncheck()
}

let saveTimer: ReturnType<typeof setTimeout> | null = null
function debouncedSave() {
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(doSave, 600)
}

async function doSave() {
  const items = Object.values(checklistMap).map(item => ({
    itemKey: item.itemKey,
    completed: item.completed,
    content: item.content,
  }))
  try {
    await saveDailyChecklist({ date: today, items })
  } catch {
    /* 静默 */
  }
}

async function loadData() {
  try {
    const [data, prevData] = await Promise.all([
      fetchDailyChecklist(today),
      fetchDailyChecklist(previousDate.value),
    ])
    Object.keys(checklistMap).forEach(k => delete checklistMap[k])
    data.forEach(item => { checklistMap[item.itemKey] = item })
    Object.keys(prevChecklistMap).forEach(k => delete prevChecklistMap[k])
    prevData.forEach(item => { prevChecklistMap[item.itemKey] = item })

    phases.forEach(phase => {
      if (currentPhase.value?.key !== phase.key) {
        collapsedPhases.add(phase.key)
      }
    })
  } catch {
    /* 静默 */
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.v2-24h {
  min-height: 100vh;
  background: #f6f8fa;
  padding: 0 16px 24px;
}

// ─── Progress ─────────────────────────
.v2-24h__progress-card {
  background: #fff;
  border-radius: 14px;
  border: 1.5px solid #e8ecef;
  padding: 16px;
  margin: 10px 0;
}

.v2-24h__progress-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.v2-24h__today {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.v2-24h__progress-count {
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
}

.v2-24h__progress-track {
  height: 10px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
  margin-bottom: 6px;
}

.v2-24h__progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #6366f1);
  border-radius: 999px;
  transition: width 0.4s ease;
}

.v2-24h__progress-bt {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.v2-24h__progress-label {
  font-size: 12px;
  color: #94a3b8;
}

.v2-24h__progress-percent {
  font-size: 13px;
  font-weight: 700;
  color: #2563eb;
}

// ─── Hints ────────────────────────────
.v2-24h__hint {
  display: flex;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 12px;
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.5;

  &--yellow {
    background: #fefce8;
    border: 1px solid #fde68a;
    color: #92400e;
  }

  &--blue {
    background: #e0f2fe;
    border: 1px solid #7dd3fc;
    color: #1e3a5f;
  }

  &--green {
    background: #f0fdf4;
    border: 1px solid #bbf7d0;
    color: #166534;
  }
}

.v2-24h__hint-icon {
  flex-shrink: 0;
  margin-top: 1px;
  opacity: 0.7;
}

.v2-24h__hint-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.v2-24h__hint-label {
  font-weight: 700;
  font-size: 12px;
  opacity: 0.7;
}

.v2-24h__hint-text {
  word-break: break-all;
}

// ─── Current phase banner ────────────
.v2-24h__current-banner {
  margin-bottom: 12px;
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--accent);
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  box-shadow: 0 4px 16px color-mix(in srgb, var(--accent) 35%, transparent);
}

.v2-24h__current-banner-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.v2-24h__current-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.25);
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.v2-24h__current-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.v2-24h__current-title {
  font-size: 15px;
  font-weight: 700;
}

.v2-24h__current-sub {
  font-size: 12px;
  opacity: 0.9;
}

.v2-24h__current-ring {
  position: relative;
  width: 40px;
  height: 40px;
  flex-shrink: 0;

  svg {
    display: block;
  }
}

.v2-24h__current-ring-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
}

// ─── Phase list ──────────────────────
.v2-24h__phase-list {
  display: flex;
  flex-direction: column;
}

// ─── Footer ──────────────────────────
.v2-24h__footer {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.v2-24h__footer-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;

  &--warn {
    background: #fef2f2;
    color: #991b1b;
  }

  &--ok {
    background: #f0fdf4;
    color: #166534;
  }
}

// ─── Overlay ─────────────────────────
.v2-24h__overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

// ─── Sheet ───────────────────────────
.v2-24h__sheet {
  width: 100%;
  max-width: 500px;
  background: #fff;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 -4px 30px rgba(0, 0, 0, 0.12);

  &--sm {
    max-width: 380px;
  }
}

.v2-24h__sheet-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px 0;

  &-left {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
    flex: 1;
  }
}

.v2-24h__sheet-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.v2-24h__sheet-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 8px;
  border-radius: 6px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.v2-24h__sheet-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  flex-shrink: 0;

  &:active {
    background: #e2e8f0;
  }
}

.v2-24h__sheet-body {
  padding: 14px 18px 20px;
}

.v2-24h__sheet-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

// ─── Write textarea ──────────────────
.v2-24h__write-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  font-size: 14px;
  color: #1e293b;
  line-height: 1.6;
  resize: vertical;
  font-family: inherit;
  box-sizing: border-box;
  background: #f8fafc;
  transition: border-color 0.2s ease;

  &:focus {
    outline: none;
    border-color: #2563eb;
    background: #fff;
  }

  &::placeholder {
    color: #94a3b8;
  }
}

// ─── Uncheck ─────────────────────────
.v2-24h__uncheck-text {
  margin: 0 0 16px;
  font-size: 15px;
  color: #374151;
  line-height: 1.5;
}

// ─── Buttons ─────────────────────────
.v2-24h__btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 9px 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.12s ease;
  font-family: inherit;

  &:active {
    transform: scale(0.97);
  }

  &--cancel {
    background: #f1f5f9;
    color: #475569;

    &:hover {
      background: #e2e8f0;
    }
  }

  &--save {
    background: #2563eb;
    color: #fff;

    &:hover {
      background: #1d4ed8;
    }
  }

  &--danger {
    background: #dc2626;
    color: #fff;

    &:hover {
      background: #b91c1c;
    }
  }
}

// ─── Animations ──────────────────────
.v2-24h-overlay-enter-active,
.v2-24h-overlay-leave-active {
  transition: opacity 0.2s ease;
}

.v2-24h-overlay-enter-from,
.v2-24h-overlay-leave-to {
  opacity: 0;
}

.v2-24h-sheet-enter-active {
  transition: transform 0.25s ease, opacity 0.2s ease;
}

.v2-24h-sheet-leave-active {
  transition: transform 0.2s ease, opacity 0.15s ease;
}

.v2-24h-sheet-enter-from {
  transform: translateY(30px);
  opacity: 0;
}

.v2-24h-sheet-leave-to {
  transform: translateY(20px);
  opacity: 0;
}
</style>
