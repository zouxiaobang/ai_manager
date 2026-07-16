<template>
  <V2Page>
    <V2Card>

      
      <div v-if="prevPrepHint" class="v2-home-first-thing" @click="goTodos">
        <div class="v2-home-first-thing__head">
          <WarRoomStatIcon name="target" tone="blue" />
          <div class="v2-home-first-thing__title">{{ t('mobile.v2.firstThing') }}</div>
        </div>
        <div class="v2-home-first-thing__content">{{ prevPrepHint }}</div>
      </div>
      <div class="v2-home-stats">
        <div class="v2-home-stat-card" @click="goTodos">
          <div class="v2-home-stat-card__value" style="color: #16a34a">{{ pendingCount }}</div>
          <div class="v2-home-stat-card__label">{{ t('portal.dashboard.todayTodos') }}</div>
        </div>
        <div class="v2-home-stat-card" @click="goMore">
          <div class="v2-home-stat-card__value" style="color: #7c3aed">{{ moduleCount }}</div>
          <div class="v2-home-stat-card__label">{{ t('portal.dashboard.warRoom.modules') }}</div>
        </div>
        <div class="v2-home-stat-card" @click="goNotebook">
          <div class="v2-home-stat-card__value" style="color: #6b7280">{{ doneCount }}</div>
          <div class="v2-home-stat-card__label">{{ t('portal.dashboard.warRoom.archived') }}</div>
        </div>
      </div>
    </V2Card>

    <div v-if="currentPhase" class="v2-home-phase" :style="{ '--accent': currentPhase.accent }" @click="go24Hour">
      <div class="v2-home-phase__top">
        <div class="v2-home-phase__title">{{ currentPhase.title }}</div>
        <div class="v2-home-phase__ring">
          <svg viewBox="0 0 36 36" width="32" height="32">
            <circle cx="18" cy="18" r="15.5" fill="none" stroke="#e5e7eb" stroke-width="3" />
            <circle
              cx="18" cy="18" r="15.5"
              fill="none"
              :stroke="currentPhase.accent"
              stroke-width="3"
              stroke-linecap="round"
              :stroke-dasharray="homePhaseCircumference"
              :stroke-dashoffset="homePhaseOffset"
              transform="rotate(-90 18 18)"
            />
          </svg>
          <span class="v2-home-phase__ring-text">{{ homePhaseDone }}/{{ currentPhase.items.length }}</span>
        </div>
      </div>
      <div class="v2-home-phase__items">
        <div
          v-for="item in currentPhase.items"
          :key="item.key"
          class="v2-home-phase__item"
          :class="{ 'v2-home-phase__item--done': homeChecklistMap[item.key]?.completed === 1 }"
        >
          <span class="v2-home-phase__dot" :class="{ 'v2-home-phase__dot--done': homeChecklistMap[item.key]?.completed === 1 }" />
          <span class="v2-home-phase__label">{{ item.label }}</span>
        </div>
      </div>
      <div class="v2-home-phase__action">{{ t('mobile.v2.viewAllPhases') }}</div>
    </div>

    <V2Card v-if="pinnedTodos.length">
      <h2 class="mobile-v2-section-title">{{ t('portal.dashboard.pinnedTodos.title') }}</h2>
      <div class="v2-home-pinned-list">
        <div
          v-for="item in pinnedTodos"
          :key="item.id"
          class="v2-home-pinned-item"
          @click="goTodos"
        >
          <div class="v2-home-pinned-item__body">
            <div class="v2-home-pinned-item__title">{{ item.content }}</div>
          </div>
          <el-checkbox
            :model-value="item.completed === 1"
            @click.stop
            @change="(checked: boolean) => onTogglePinned(item, checked)"
          />
        </div>
      </div>
    </V2Card>

    <V2Card>
      <h2 class="mobile-v2-section-title">{{ t('portal.dashboard.warRoom.systemStatus') }}</h2>
      <div class="v2-home-status">
        <div ref="statusTreeRef" class="v2-home-status__tree">
          <svg class="v2-home-status__svg" aria-hidden="true">
            <path
              v-for="(path, index) in connectorPaths"
              :key="`path-${index}`"
              :d="path"
              class="v2-home-status__connector-line"
            />
          </svg>
          <div ref="statusRootRef" class="v2-home-status-node v2-home-status-node--root" :class="statusNodeClass(healthStatus)">
            <span class="v2-home-status-node__name">{{ t('portal.dashboard.warRoom.backendNode') }}</span>
            <span class="v2-home-status-badge" :class="statusBadgeClass(healthStatus)">{{ statusLabel(healthStatus) }}</span>
          </div>
          <div class="v2-home-status__children">
            <div
              v-for="(node, index) in infraNodes"
              :key="node.key"
              class="v2-home-status__child"
            >
              <div
                :ref="(el) => setStatusChildRef(el as HTMLElement | null, index)"
                class="v2-home-status-node"
                :class="statusNodeClass(node.state)"
              >
                <span class="v2-home-status-node__name">{{ node.label }}</span>
                <span class="v2-home-status-badge" :class="statusBadgeClass(node.state)">{{ statusLabel(node.state) }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="v2-home-status__legend">
          <span class="v2-home-status__legend-label">{{ t('portal.dashboard.warRoom.legendTitle') }}</span>
          <div class="v2-home-status__legend-items">
            <span class="v2-home-status__legend-item"><span class="dot dot--up" /><span class="v2-home-status__legend-text">{{ t('portal.dashboard.warRoom.legendUp') }}</span></span>
            <span class="v2-home-status__legend-item"><span class="dot dot--warn" /><span class="v2-home-status__legend-text">{{ t('portal.dashboard.warRoom.legendWarn') }}</span></span>
            <span class="v2-home-status__legend-item"><span class="dot dot--down" /><span class="v2-home-status__legend-text">{{ t('portal.dashboard.warRoom.legendDown') }}</span></span>
            <span class="v2-home-status__legend-item"><span class="dot dot--unknown" /><span class="v2-home-status__legend-text">{{ t('portal.dashboard.warRoom.legendUnknown') }}</span></span>
          </div>
        </div>
      </div>
    </V2Card>
  </V2Page>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'

import V2Page from '@/mobile-v2/components/V2Page.vue'
import V2Card from '@/mobile-v2/components/V2Card.vue'
import WarRoomStatIcon from '@/components/war-room/WarRoomStatIcon.vue'
import { fetchHealth } from '@/api/health'
import type { HealthData } from '@/api/types'
import { fetchPinnedTodos, fetchTodayTodos, updateTodo } from '@/api/notebook/todo'
import { fetchDailyChecklist, type DailyChecklistItem } from '@/api/dailyChecklist'
import { functionItems } from '@/data/function-items'
import { useTodoReminders } from '@/composables/useTodoReminders'
import { getCurrentPhase, type PhaseDef } from '@/data/24hour-phases'

const router = useRouter()
const { t } = useI18n()
const { refreshTodayCount } = useTodoReminders()

const pendingCount = ref(0)
const doneCount = ref(0)
const pinnedTodos = ref<any[]>([])
const moduleCount = computed(() => functionItems.length)
const prevPrepHint = ref('')

const statusTreeRef = ref<HTMLElement | null>(null)
const statusRootRef = ref<HTMLElement | null>(null)
const statusChildRefs = ref<(HTMLElement | null)[]>([])
const connectorPaths = ref<string[]>([])

const CONNECTOR_CORNER_RADIUS = 6
const CONNECTOR_STEM_GAP = 12

const currentPhase = ref<PhaseDef | null>(null)
const homeChecklistMap = reactive<Record<string, DailyChecklistItem>>({})
let home24Timer: ReturnType<typeof setInterval> | null = null

const homePhaseCircumference = 2 * Math.PI * 15.5

const homePhaseDone = computed(() => {
  if (!currentPhase.value) return 0
  return currentPhase.value.items.filter(i => homeChecklistMap[i.key]?.completed === 1).length
})

const homePhaseOffset = computed(() => {
  const phase = currentPhase.value
  if (!phase || phase.items.length === 0) return homePhaseCircumference
  const total = phase.items.length
  const done = homePhaseDone.value
  return homePhaseCircumference - (done / total) * homePhaseCircumference
})

type HealthState = 'up' | 'down' | 'unknown'
const healthStatus = ref<HealthState>('unknown')
const healthData = ref<HealthData | null>(null)

function statusNodeClass(state: HealthState) {
  return {
    'v2-home-status-node--up': state === 'up',
    'v2-home-status-node--down': state === 'down',
    'v2-home-status-node--unknown': state === 'unknown',
  }
}

function statusBadgeClass(state: HealthState) {
  return {
    'v2-home-status-badge--up': state === 'up',
    'v2-home-status-badge--down': state === 'down',
    'v2-home-status-badge--unknown': state === 'unknown',
  }
}

function statusLabel(state: HealthState) {
  if (state === 'up') return t('portal.dashboard.warRoom.statusUp')
  if (state === 'down') return t('portal.dashboard.warRoom.statusDown')
  return t('portal.dashboard.warRoom.statusUnknown')
}

const infraNodes = computed(() => {
  const backendUp = healthStatus.value === 'up'
  const redisUp = healthData.value?.redis === 'UP'
  const redisDown = healthData.value?.redis === 'DOWN'
  const redisState: HealthState = redisUp ? 'up' : redisDown ? 'down' : 'unknown'

  const mysqlUp = healthData.value?.mysql === 'UP'
  const mysqlDown = healthData.value?.mysql === 'DOWN'
  const mysqlState: HealthState = mysqlUp ? 'up' : mysqlDown ? 'down' : 'unknown'

  const nginxState: HealthState = backendUp ? 'up' : healthStatus.value === 'down' ? 'down' : 'unknown'

  return [
    { key: 'mysql', label: t('portal.dashboard.warRoom.mysqlNode'), state: mysqlState },
    { key: 'redis', label: t('portal.dashboard.warRoom.redisNode'), state: redisState },
    { key: 'nginx', label: t('portal.dashboard.warRoom.nginxNode'), state: nginxState },
  ]
})

function setStatusChildRef(el: HTMLElement | null, index: number) {
  if (el) {
    statusChildRefs.value[index] = el
  }
}

function buildChildDrop(x: number, railY: number, nodeTop: number, side: 'left' | 'center' | 'right') {
  const r = CONNECTOR_CORNER_RADIUS
  const endY = nodeTop

  if (side === 'center') {
    return `M ${x} ${railY} V ${endY}`
  }
  if (side === 'left') {
    return `M ${x + r} ${railY} A ${r} ${r} 0 0 0 ${x} ${railY + r} V ${endY}`
  }
  return `M ${x - r} ${railY} A ${r} ${r} 0 0 1 ${x} ${railY + r} V ${endY}`
}

function updateStatusConnector() {
  const tree = statusTreeRef.value
  const root = statusRootRef.value
  const children = statusChildRefs.value.filter((el): el is HTMLElement => !!el)
  if (!tree || !root || children.length === 0) return

  const treeRect = tree.getBoundingClientRect()
  const rootRect = root.getBoundingClientRect()
  const childRects = children.map((el) => el.getBoundingClientRect())

  const rootCx = rootRect.left + rootRect.width / 2 - treeRect.left
  const rootBottom = rootRect.bottom - treeRect.top
  const railY = rootBottom + CONNECTOR_STEM_GAP
  const cornerR = CONNECTOR_CORNER_RADIUS

  const childPoints = childRects.map((rect, index) => ({
    x: rect.left + rect.width / 2 - treeRect.left,
    top: rect.top - treeRect.top,
    side: (index === 0 ? 'left' : index === children.length - 1 ? 'right' : 'center') as 'left' | 'center' | 'right',
  }))

  const left = childPoints[0]
  const right = childPoints[childPoints.length - 1]
  const paths = [
    `M ${rootCx} ${rootBottom} V ${railY}`,
    `M ${left.x + cornerR} ${railY} H ${right.x - cornerR}`,
    ...childPoints.map((child) => buildChildDrop(child.x, railY, child.top, child.side)),
  ]

  connectorPaths.value = paths
}

async function loadHealth() {
  try {
    const data = await fetchHealth()
    healthData.value = data
    healthStatus.value = data.status === 'UP' ? 'up' : 'down'
  } catch {
    healthData.value = null
    healthStatus.value = 'down'
  }
}

async function loadTodos() {
  try {
    const today = await fetchTodayTodos()
    pendingCount.value = today.filter((row: any) => row.completed !== 1).length
    doneCount.value = today.filter((row: any) => row.completed === 1).length
    const pinned = await fetchPinnedTodos()
    pinnedTodos.value = pinned
  } catch {
    pendingCount.value = 0
    doneCount.value = 0
    pinnedTodos.value = []
  }
}

function yesterdayDateString(): string {
  const d = new Date()
  d.setDate(d.getDate() - 1)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

async function loadPrepHint() {
  try {
    const data = await fetchDailyChecklist(yesterdayDateString())
    const hint = data.find((item: any) => item.itemKey === 'prep_hint')
    prevPrepHint.value = hint?.content || ''
  } catch {
    prevPrepHint.value = ''
  }
}

async function onTogglePinned(item: any, checked: boolean) {
  try {
    await updateTodo(item.id, { completed: checked })
    if (checked) {
      pinnedTodos.value = pinnedTodos.value.filter((r: any) => r.id !== item.id)
      doneCount.value = doneCount.value + 1
      pendingCount.value = Math.max(0, pendingCount.value - 1)
    }
    await refreshTodayCount()
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  }
}

function goTodos() {
  router.push('/todos')
}

function goMore() {
  router.push('/more')
}

function goNotebook() {
  router.push('/notebook')
}

function go24Hour() {
  router.push('/24hour')
}

async function loadHomePhase() {
  const today = new Date().toISOString().split('T')[0]
  try {
    const data = await fetchDailyChecklist(today)
    Object.keys(homeChecklistMap).forEach(k => delete homeChecklistMap[k])
    data.forEach(item => { homeChecklistMap[item.itemKey] = item })
  } catch {
    Object.keys(homeChecklistMap).forEach(k => delete homeChecklistMap[k])
  }
  currentPhase.value = getCurrentPhase()
}

onMounted(() => {
  void loadHealth()
  void loadTodos()
  void loadPrepHint()
  void loadHomePhase()
  home24Timer = setInterval(loadHomePhase, 60_000)

  void nextTick(updateStatusConnector)
})

onUnmounted(() => {
  if (home24Timer) {
    clearInterval(home24Timer)
    home24Timer = null
  }
})

watch(infraNodes, () => {
  void nextTick(updateStatusConnector)
})
</script>

<style scoped lang="scss">
.v2-home-first-thing {
  margin-bottom: 14px;
  padding: 16px;
  background: linear-gradient(135deg, #eef2ff, #e0e7ff);
  border-radius: 12px;
  cursor: pointer;
  transition: opacity 0.15s;
  text-align: center;

  &:hover {
    opacity: 0.85;
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    margin-bottom: 6px;
  }

  &__title {
    font-size: 13px;
    font-weight: 700;
    color: #4338ca;
  }

  &__content {
    font-size: 14px;
    font-weight: 500;
    line-height: 1.6;
    color: #1e1b4b;
  }
}

.v2-home-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.v2-home-stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 8px;
  background: var(--wr-card, #ffffff);
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.15s;

  &:hover {
    border-color: var(--wr-stat-blue, #2563eb);
  }

  &__value {
    font-size: 28px;
    font-weight: 700;
    line-height: 1;
    font-variant-numeric: tabular-nums;
  }

  &__label {
    font-size: 12px;
    font-weight: 500;
    color: var(--wr-text-secondary, #666666);
    text-align: center;
  }
}

.v2-home-pinned-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.v2-home-phase {
  padding: 16px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--accent) 7%, #fff);
  border: 1px solid color-mix(in srgb, var(--accent) 20%, transparent);
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;

  &:hover {
    box-shadow: 0 2px 10px rgb(0 0 0 / 8%);
    border-color: color-mix(in srgb, var(--accent) 40%, transparent);
  }

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }

  &__title {
    font-size: 15px;
    font-weight: 700;
    color: #111827;
  }

  &__ring {
    position: relative;
    width: 32px;
    height: 32px;

    svg { display: block; }
  }

  &__ring-text {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: 700;
    color: var(--accent);
  }

  &__items {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  &__item {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 500;
    color: #374151;
    background: #fff;
    border: 1px solid #f0f0f0;

    &--done {
      opacity: 0.5;
      text-decoration: line-through;
    }
  }

  &__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #d1d5db;
    flex-shrink: 0;

    &--done {
      background: var(--accent);
    }
  }

  &__label {
    line-height: 1.3;
  }

  &__action {
    margin-top: 10px;
    font-size: 12px;
    font-weight: 600;
    color: var(--accent);
    text-align: right;
    opacity: 0.7;
    transition: opacity 0.15s;

    .v2-home-phase:hover & {
      opacity: 1;
    }
  }
}

.v2-home-pinned-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: var(--wr-index-bg, #eff6ff);
  border-radius: 8px;
  cursor: pointer;

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: 13px;
    font-weight: 500;
    line-height: 1.4;
    color: var(--wr-text, #333333);
  }
}

.v2-home-status {
  &__tree {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 4px 0;
  }

  &__svg {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 0;
    pointer-events: none;
    overflow: visible;
  }

  &__connector-line {
    fill: none;
    stroke: #5a9a68;
    stroke-width: 2;
    stroke-linecap: round;
    stroke-linejoin: round;
    vector-effect: non-scaling-stroke;
  }

  &__children {
    display: flex;
    gap: 10px;
    width: 100%;
    margin-top: 40px;
  }

  &__child {
    flex: 1;
    display: flex;
    justify-content: center;
  }

  &__legend {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid var(--wr-border, #e8ecef);
  }

  &__legend-label {
    font-size: 12px;
    font-weight: 600;
    color: var(--wr-text-secondary, #666);
    margin-bottom: 6px;
  }

  &__legend-items {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 4px 0;
  }

  &__legend-item {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }

  &__legend-text {
    font-size: 11px;
    color: var(--wr-text-secondary, #666);
  }
}

.v2-home-status-node {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  background: #fff;
  position: relative;
  z-index: 1;

  &--up {
    border-color: #16a34a;
    background: #f0fdf4;
  }

  &--down {
    border-color: #dc2626;
    background: #fef2f2;
  }

  &--unknown {
    border-color: #9ca3af;
    background: #f9fafb;
  }

  &__name {
    font-size: 12px;
    font-weight: 600;
    color: #111827;
    white-space: nowrap;
  }
}

.v2-home-status-badge {
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
  white-space: nowrap;

  &--up {
    background: #16a34a;
    color: #fff;
  }

  &--down {
    background: #dc2626;
    color: #fff;
  }

  &--unknown {
    background: #9ca3af;
    color: #fff;
  }
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  vertical-align: middle;
  margin-right: 2px;

  &--up { background: #389845; }
  &--warn { background: #f59e0b; }
  &--down { background: #ef4444; }
  &--unknown { background: #9ca3af; }
}
</style>
