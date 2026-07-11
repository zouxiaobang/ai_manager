<template>
  <div class="mobile-24h">
    <!-- 顶部导航栏 -->
    <MobilePageHeader title="24小时重启系统" @back="goBack">
      <template #right>
        <button class="mobile-24h__header-btn" @click="toggleView">
          <span v-if="activeTab === 'checklist'">📊</span>
          <span v-else>📋</span>
        </button>
      </template>
    </MobilePageHeader>

    <!-- 清单视图 -->
    <template v-if="activeTab === 'checklist'">
      <!-- 今日进度条 -->
      <div class="mobile-24h__progress-bar">
        <div class="mobile-24h__progress-info">
          <span class="mobile-24h__progress-label">今日进度</span>
          <span class="mobile-24h__progress-count">{{ completedCount }}/{{ totalCount }}</span>
        </div>
        <div class="mobile-24h__progress-track">
          <div class="mobile-24h__progress-fill" :style="{ width: progressPercent + '%' }" />
        </div>
        <span class="mobile-24h__progress-percent">{{ progressPercent }}%</span>
      </div>

      <!-- 前夜暗示 -->
      <div v-if="prevPrepHint" class="mobile-24h__hint mobile-24h__hint--yellow">
        <span class="mobile-24h__hint-icon">⭐</span>
        <span class="mobile-24h__hint-label">昨夜暗示：</span>
        <span class="mobile-24h__hint-text">{{ prevPrepHint }}</span>
      </div>

      <!-- 下午提醒 -->
      <div v-if="afternoonReminder" class="mobile-24h__hint mobile-24h__hint--blue">
        <span class="mobile-24h__hint-icon">⏰</span>
        <span class="mobile-24h__hint-label">下午提示：</span>
        <span class="mobile-24h__hint-text">{{ afternoonReminder }}</span>
      </div>

      <!-- 当前时段提示 -->
      <div v-if="currentPhase" class="mobile-24h__current-banner" :style="{ '--accent': currentPhase.accent }">
        <div class="mobile-24h__current-left">
          <span class="mobile-24h__current-badge">{{ currentPhase.badge }}</span>
          <div class="mobile-24h__current-info">
            <span class="mobile-24h__current-title">{{ currentPhase.title }}</span>
            <span class="mobile-24h__current-sub">还剩 {{ phaseRemainingCount(currentPhase) }} 项待完成</span>
          </div>
        </div>
        <div class="mobile-24h__current-ring">
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
          <span class="mobile-24h__current-ring-text">{{ phaseCompletedCount(currentPhase) }}</span>
        </div>
      </div>

      <!-- 昨夜交付任务 -->
      <div v-if="prevPrepTask" class="mobile-24h__hint mobile-24h__hint--green">
        <span class="mobile-24h__hint-icon">🎯</span>
        <span class="mobile-24h__hint-label">今日交付：</span>
        <span class="mobile-24h__hint-text">{{ prevPrepTask }}</span>
      </div>

      <!-- 时段列表 -->
      <div class="mobile-24h__phase-list">
        <PhaseCard
          v-for="phase in phases"
          :key="phase.key"
          :phase="phase"
          :is-current="currentPhase?.key === phase.key"
          :all-done="isPhaseAllDone(phase)"
          :completed-count="phaseCompletedCount(phase)"
          :collapsed="collapsedPhases.has(phase.key)"
          @toggle-collapse="togglePhaseCollapse(phase.key)"
          @toggle-item="onToggleItem"
          @open-write="openWrite"
        >
          <template #item="{ item }">
            <ChecklistItem
              :item="item"
              :checked="isChecked(item.key)"
              :content="getContent(item.key)"
              :accent="phase.accent"
              @toggle="onToggleItem(item)"
              @open-write="openWrite(item)"
            />
          </template>
        </PhaseCard>
      </div>

      <!-- 底部提示 -->
      <div class="mobile-24h__footer">
        <div class="mobile-24h__footer-tag mobile-24h__footer-tag--danger">
          <span>⚠</span> 伪努力陷阱
        </div>
        <div class="mobile-24h__footer-tag mobile-24h__footer-tag--success">
          <span>✓</span> 成长路径
        </div>
      </div>
    </template>

    <!-- 统计视图 -->
    <template v-else>
      <StatsView :phases="phases" />
    </template>

    <!-- 写作底部弹窗 -->
    <MobileBottomSheet
      v-model="writeSheetVisible"
      :title="writeItem?.label || ''"
      show-close
    >
      <template #header-left>
        <span v-if="writePhaseBadge" class="mobile-24h__write-badge">{{ writePhaseBadge }}</span>
      </template>
      <div class="mobile-24h__write-body">
        <textarea
          v-model="writeContent"
          class="mobile-24h__write-textarea"
          rows="6"
          placeholder="记录你的想法…"
          ref="writeTextareaRef"
        />
        <div class="mobile-24h__write-actions">
          <button class="mobile-24h__btn mobile-24h__btn--cancel" @click="closeWrite">取消</button>
          <button class="mobile-24h__btn mobile-24h__btn--save" @click="saveWrite">
            <span>✓</span> 提交
          </button>
        </div>
      </div>
    </MobileBottomSheet>

    <!-- 撤销确认底部弹窗 -->
    <MobileBottomSheet
      v-model="uncheckSheetVisible"
      title="撤销确认"
      show-close
    >
      <div class="mobile-24h__uncheck-body">
        <p class="mobile-24h__uncheck-text">确定要撤销 "{{ uncheckItemLabel }}" 吗？</p>
        <div class="mobile-24h__uncheck-actions">
          <button class="mobile-24h__btn mobile-24h__btn--cancel" @click="cancelUncheck">取消</button>
          <button class="mobile-24h__btn mobile-24h__btn--danger" @click="confirmUncheck">
            <span>✕</span> 确认撤销
          </button>
        </div>
      </div>
    </MobileBottomSheet>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import MobilePageHeader from '@/mobile/components/MobilePageHeader.vue'
import MobileBottomSheet from '@/mobile/components/MobileBottomSheet.vue'
import PhaseCard from './components/PhaseCard.vue'
import ChecklistItem from './components/ChecklistItem.vue'
import StatsView from './components/StatsView.vue'
import { fetchDailyChecklist, saveDailyChecklist } from '@/api/dailyChecklist'
import type { DailyChecklistItem } from '@/api/dailyChecklist'
import { phases, getCurrentPhase, type ItemDef, type PhaseDef } from '@/data/24hour-phases'

const router = useRouter()

const activeTab = ref<'checklist' | 'stats'>('checklist')

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
      if (isPhaseAllDone(phase)) {
        collapsedPhases.add(phase.key)
      }
    })
  } catch {
    /* 静默 */
  }
}

function goBack() {
  router.back()
}

function toggleView() {
  activeTab.value = activeTab.value === 'checklist' ? 'stats' : 'checklist'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.mobile-24h {
  min-height: 100vh;
  background: #f8fafc;
  padding-bottom: max(20px, env(safe-area-inset-bottom));
}

.mobile-24h__header-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  border: none;
  border-radius: 50%;
  font-size: 18px;
  cursor: pointer;
  transition: transform 0.15s ease;

  &:active {
    transform: scale(0.92);
  }
}

// 进度条
.mobile-24h__progress-bar {
  margin: 0 16px 12px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 14px;
  border: 2px solid #e2e8f0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-24h__progress-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex-shrink: 0;
}

.mobile-24h__progress-label {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 13px;
  color: #64748b;
}

.mobile-24h__progress-count {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.mobile-24h__progress-track {
  flex: 1;
  height: 10px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
}

.mobile-24h__progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #6366f1);
  border-radius: 999px;
  transition: width 0.3s ease;
}

.mobile-24h__progress-percent {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #2563eb;
  flex-shrink: 0;
  min-width: 36px;
  text-align: right;
}

// 提示条
.mobile-24h__hint {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: 0 16px 10px;
  padding: 10px 14px;
  border-radius: 12px;
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

.mobile-24h__hint-icon {
  flex-shrink: 0;
  font-size: 14px;
}

.mobile-24h__hint-label {
  font-weight: 700;
  flex-shrink: 0;
  font-family: 'ZCOOL KuaiLe', sans-serif;
}

.mobile-24h__hint-text {
  flex: 1;
  min-width: 0;
}

// 当前时段横幅
.mobile-24h__current-banner {
  margin: 0 16px 12px;
  padding: 14px 16px;
  border-radius: 16px;
  background: var(--accent);
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  box-shadow: 0 4px 14px color-mix(in srgb, var(--accent) 40%, transparent);
}

.mobile-24h__current-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.mobile-24h__current-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.25);
  font-size: 12px;
  font-weight: 700;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  flex-shrink: 0;
}

.mobile-24h__current-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.mobile-24h__current-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  font-weight: 700;
}

.mobile-24h__current-sub {
  font-size: 12px;
  opacity: 0.9;
}

.mobile-24h__current-ring {
  position: relative;
  width: 40px;
  height: 40px;
  flex-shrink: 0;

  svg {
    display: block;
  }
}

.mobile-24h__current-ring-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
}

// 时段列表
.mobile-24h__phase-list {
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

// 底部
.mobile-24h__footer {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 16px;
  padding: 0 16px;
  flex-wrap: wrap;
}

.mobile-24h__footer-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  font-family: 'ZCOOL KuaiLe', sans-serif;

  &--danger {
    background: #fef2f2;
    color: #991b1b;
  }

  &--success {
    background: #f0fdf4;
    color: #166534;
  }
}

// 写作弹窗
.mobile-24h__write-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 8px;
  border-radius: 6px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
  margin-right: 8px;
}

.mobile-24h__write-body {
  padding: 4px 0;
}

.mobile-24h__write-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 2px solid #e2e8f0;
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

.mobile-24h__write-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

// 撤销弹窗
.mobile-24h__uncheck-body {
  padding: 4px 0;
}

.mobile-24h__uncheck-text {
  margin: 0 0 18px;
  font-size: 15px;
  color: #374151;
  line-height: 1.5;
}

.mobile-24h__uncheck-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

// 通用按钮
.mobile-24h__btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 9px 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  border: none;
  cursor: pointer;
  transition: all 0.12s ease;

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
</style>
