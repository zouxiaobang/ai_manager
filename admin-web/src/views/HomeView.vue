<template>
  <div class="war-room-page">
    <header class="war-room-page__header">
      <h1 class="war-room-page__title">{{ t('portal.dashboard.warRoom.title') }}</h1>
      <div class="war-room-page__meta">
        <span v-if="lastRefreshText">
          {{ t('portal.dashboard.warRoom.lastRefresh') }} {{ lastRefreshText }}
        </span>
        <button
          type="button"
          class="war-room-meta-btn"
          :disabled="healthLoading"
          @click="refreshAll"
        >
          {{ t('portal.dashboard.warRoom.manualArchive') }} {{ manualArchiveText }}
        </button>
      </div>
    </header>

    <section class="home-stats">
      <div
        v-for="card in statCards"
        :key="card.key"
        class="home-stat-card"
        :class="{ 'is-clickable': !!card.onClick }"
        @click="card.onClick?.()"
      >
        <div class="home-stat-card__head">
          <WarRoomStatIcon :name="card.icon" :tone="card.tone" />
          <span class="home-stat-card__label">{{ card.label }}</span>
        </div>
        <p v-if="card.hint" class="home-stat-card__hint">{{ card.hint }}</p>
        <span v-else class="home-stat-card__value" :style="{ color: card.color }">{{ card.value }}</span>
      </div>
    </section>

    <section v-loading="pinnedTodosLoading" class="home-pinned war-room-panel">
      <div class="home-pinned__header">
        <h2 class="home-section-title">{{ t('portal.dashboard.pinnedTodos.title') }}</h2>
        <button type="button" class="home-pinned__link" @click="goTodosPage('all')">
          {{ t('portal.dashboard.pinnedTodos.viewAll') }}
        </button>
      </div>
      <div v-if="pinnedTodos.length" class="home-pinned__list">
        <article
          v-for="item in pinnedTodos"
          :key="item.id"
          class="home-pinned-card"
          @click="goTodosPage('all')"
        >
          <el-icon class="home-pinned-card__star"><StarFilled /></el-icon>
          <div class="home-pinned-card__body">
            <h3 class="home-pinned-card__title">{{ item.content }}</h3>
            <p v-if="pinnedTodoMeta(item)" class="home-pinned-card__desc">{{ pinnedTodoMeta(item) }}</p>
          </div>
          <el-checkbox
            class="home-pinned-card__check"
            :model-value="item.completed === 1"
            @click.stop
            @change="(checked: boolean) => onTogglePinnedTodo(item, checked)"
          />
        </article>
      </div>
      <p v-else class="home-pinned__empty">{{ t('portal.dashboard.pinnedTodos.empty') }}</p>
    </section>

    <section class="home-modules">
      <div class="home-modules__box">
        <div ref="moduleScrollRef" class="home-modules__inner">
          <WarRoomModuleCard
            v-for="item in quickModules"
            :key="item.key"
            :icon="item.icon"
            :name="moduleLabel(item)"
            :bar-color="item.barColor"
            @click="openQuickModule(item)"
          />
        </div>
        <button
          type="button"
          class="home-modules__arrow"
          :title="t('portal.dashboard.warRoom.scrollModules')"
          @click="scrollModulesNext"
        >
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <path d="M7 4.5 11.5 9 7 13.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
      </div>
    </section>

    <section class="war-room-panel">
      <h2 class="home-section-title">{{ t('portal.dashboard.warRoom.systemStatus') }}</h2>
      <div class="home-status">
        <div class="home-status__diagram">
          <div ref="statusTreeRef" class="home-status__tree">
          <svg
            v-if="connectorSize.width > 0"
            class="home-status__connector-overlay"
            :width="connectorSize.width"
            :height="connectorSize.height"
            aria-hidden="true"
          >
            <path
              v-for="(path, index) in connectorPaths"
              :key="`path-${index}`"
              :d="path"
              class="home-status__connector-line"
            />
          </svg>
          <span
            v-for="(dot, index) in connectorDots"
            :key="`dot-${index}`"
            class="home-status__connector-dot"
            :style="{ left: `${dot.cx}px`, top: `${dot.cy}px` }"
            aria-hidden="true"
          />
          <div ref="statusRootRef" class="home-status-node" :class="statusNodeClass(healthStatus)">
            <span class="home-status-node__name">{{ t('portal.dashboard.warRoom.backendNode') }}</span>
            <span class="home-status-badge" :class="statusBadgeClass(healthStatus)">{{ statusLabel(healthStatus) }}</span>
          </div>
          <div class="home-status__children">
            <div
              v-for="(node, index) in infraNodes"
              :key="node.key"
              class="home-status__child"
            >
              <div
                :ref="(el) => setStatusChildRef(el as HTMLElement | null, index)"
                class="home-status-node"
                :class="statusNodeClass(node.state)"
              >
                <span class="home-status-node__name">{{ node.label }}</span>
                <span class="home-status-badge" :class="statusBadgeClass(node.state)">{{ statusLabel(node.state) }}</span>
              </div>
            </div>
          </div>
        </div>
        </div>
        <div class="home-status__divider" aria-hidden="true" />
        <div class="home-status__legend-wrap">
          <div class="home-status__legend">
            <h3 class="home-status__legend-title">{{ t('portal.dashboard.warRoom.legendTitle') }}</h3>
            <ul class="home-status__legend-list">
              <li><span class="dot dot--up" />{{ t('portal.dashboard.warRoom.legendUp') }}</li>
              <li><span class="dot dot--warn" />{{ t('portal.dashboard.warRoom.legendWarn') }}</li>
              <li><span class="dot dot--down" />{{ t('portal.dashboard.warRoom.legendDown') }}</li>
              <li><span class="dot dot--unknown" />{{ t('portal.dashboard.warRoom.legendUnknown') }}</li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <section v-loading="todayTodosLoading" class="home-tasks">
      <article
        v-for="(item, index) in displayCards"
        :key="item.key"
        class="home-task-card"
        @click="item.onClick?.()"
      >
        <span class="home-task-card__index">{{ index + 1 }}</span>
        <div class="home-task-card__body">
          <h3 class="home-task-card__title">{{ item.title }}</h3>
          <p class="home-task-card__desc">{{ item.desc }}</p>
        </div>
        <el-checkbox
          v-if="item.todo"
          class="home-task-card__check"
          :model-value="item.todo.completed === 1"
          @click.stop
          @change="(checked: boolean) => onToggleTodayTodo(item.todo!, checked)"
        />
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { StarFilled } from '@element-plus/icons-vue'
import WarRoomStatIcon from '@/components/war-room/WarRoomStatIcon.vue'
import WarRoomModuleCard from '@/components/war-room/WarRoomModuleCard.vue'
import { fetchHealth } from '@/api/health'
import type { HealthData } from '@/api/types'
import { fetchPinnedTodos, fetchTodayTodos, fetchTodos, updateTodo, type NbTodoFilter, type NbTodoItem } from '@/api/notebook/todo'
import { useTodoReminders, dismissTodoNotification } from '@/composables/useTodoReminders'
import { formatTodoScheduleLabel } from '@/views/notebook/todoGroup'
import { functionItems } from '@/data/function-items'
import { type HomeModuleKey } from '@/data/module-visuals'
import { quickModules, type QuickModuleDef } from '@/data/module-visuals'

type HealthState = 'up' | 'down' | 'unknown'

interface ConnectorDot {
  cx: number
  cy: number
}

const CONNECTOR_CORNER_RADIUS = 6
const CONNECTOR_STEM_GAP = 18

interface HomeStatCard {
  key: string
  label: string
  hint?: string
  value?: number
  color?: string
  tone: 'blue' | 'orange' | 'purple' | 'green' | 'gray'
  icon: 'target' | 'pulse' | 'alert' | 'cube' | 'checklist' | 'folder'
  onClick?: () => void
}

interface DisplayCard {
  key: string
  title: string
  desc: string
  todo?: NbTodoItem
  onClick?: () => void
}

const spotlightOrder: HomeModuleKey[] = [
  'notebook',
  'todos',
  'pomodoro',
  'ecommerce',
  'aiKnowledge',
  'library',
  'pixelDog',
]

const spotlightRoutes: Partial<Record<HomeModuleKey, string>> = {
  pomodoro: '/pomodoro',
  ecommerce: '/ecommerce',
  pixelDog: '/pixel-dog',
}

const { t, locale } = useI18n()
const router = useRouter()
const { refreshTodayCount } = useTodoReminders()

const moduleScrollRef = ref<HTMLElement | null>(null)
const statusTreeRef = ref<HTMLElement | null>(null)
const statusRootRef = ref<HTMLElement | null>(null)
const statusChildRefs = ref<(HTMLElement | null)[]>([])
const connectorSize = ref({ width: 0, height: 0 })
const connectorPaths = ref<string[]>([])
const connectorDots = ref<ConnectorDot[]>([])
let statusConnectorObserver: ResizeObserver | null = null
const healthLoading = ref(false)
const healthData = ref<HealthData | null>(null)
const healthStatus = ref<HealthState>('unknown')
const lastRefreshAt = ref<Date | null>(null)
const manualArchiveAt = ref<Date | null>(null)

const todayTodosLoading = ref(false)
const pinnedTodosLoading = ref(false)
const todayTodos = ref<NbTodoItem[]>([])
const pinnedTodos = ref<NbTodoItem[]>([])
const pendingCount = ref(0)
const doneCount = ref(0)
const reminderCount = ref(0)

function formatTime(d: Date | null) {
  if (!d) return '—'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const lastRefreshText = computed(() => formatTime(lastRefreshAt.value))
const manualArchiveText = computed(() => {
  if (!manualArchiveAt.value) return '—'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(manualArchiveAt.value.getHours())}:${pad(manualArchiveAt.value.getMinutes())}`
})

const firstPriorityText = computed(() => {
  const pending = todayTodos.value.find((row) => row.completed !== 1)
  return pending?.content ?? t('portal.dashboard.warRoom.firstPriorityHint')
})

const statCards = computed<HomeStatCard[]>(() => [
  {
    key: 'firstPriority',
    label: t('portal.dashboard.warRoom.firstPriority'),
    hint: firstPriorityText.value,
    tone: 'blue',
    icon: 'target',
    onClick: () => goTodosPage('all'),
  },
  {
    key: 'today',
    label: t('portal.dashboard.todayTodos'),
    value: todayTodos.value.filter((row) => row.completed !== 1).length,
    color: '#16a34a',
    tone: 'green',
    icon: 'checklist',
    onClick: () => goTodosPage('all'),
  },
  {
    key: 'inProgress',
    label: t('portal.dashboard.warRoom.inProgress'),
    value: pendingCount.value,
    color: '#2563eb',
    tone: 'blue',
    icon: 'pulse',
    onClick: () => goTodosPage('pending'),
  },
  {
    key: 'reminders',
    label: t('portal.dashboard.warRoom.awaitingConfirm'),
    value: reminderCount.value,
    color: '#ea580c',
    tone: 'orange',
    icon: 'alert',
  },
  {
    key: 'modules',
    label: t('portal.dashboard.warRoom.modules'),
    value: functionItems.length,
    color: '#7c3aed',
    tone: 'purple',
    icon: 'cube',
    onClick: goFunctions,
  },
  {
    key: 'archived',
    label: t('portal.dashboard.warRoom.archived'),
    value: doneCount.value,
    color: '#6b7280',
    tone: 'gray',
    icon: 'folder',
  },
])

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

const WEEKDAY_KEYS = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'] as const

function weekdayLabel(dayIndex: number) {
  return t(`notebook.todos.weekdays.${WEEKDAY_KEYS[dayIndex]}`)
}

function pinnedTodoMeta(item: NbTodoItem): string {
  const value = item.dueTime ?? item.remindTime
  if (!value) return ''
  const suffixKey = item.dueTime ? 'dueSuffix' : 'remindSuffix'
  return formatTodoScheduleLabel(value, {
    todayLabel: t('notebook.todos.dateToday'),
    suffix: t(`notebook.todos.${suffixKey}`),
    locale: locale.value,
    weekday: weekdayLabel,
  })
}

const displayCards = computed<DisplayCard[]>(() => {
  const todoCards: DisplayCard[] = todayTodos.value.slice(0, 6).map((item) => ({
    key: `todo-${item.id}`,
    title: item.content,
    desc: pinnedTodoMeta(item) || t('portal.dashboard.todayTodos'),
    todo: item,
    onClick: () => goTodosPage('all'),
  }))

  if (todoCards.length >= 6) return todoCards

  const fillers: DisplayCard[] = []
  for (const key of spotlightOrder) {
    if (todoCards.length + fillers.length >= 6) break
    fillers.push({
      key: `spotlight-${key}`,
      title: t(`functions.items.${key}.name`),
      desc: t(`functions.items.${key}.desc`),
      onClick: () => openFunctionByKey(key),
    })
  }
  return [...todoCards, ...fillers].slice(0, 6)
})

function setStatusChildRef(el: HTMLElement | null, index: number) {
  statusChildRefs.value[index] = el
}

function buildChildDrop(
  x: number,
  railY: number,
  nodeTop: number,
  side: 'left' | 'center' | 'right',
) {
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
  if (!tree || !root || children.length !== 3) return

  const treeRect = tree.getBoundingClientRect()
  const rootRect = root.getBoundingClientRect()
  const childRects = children.map((el) => el.getBoundingClientRect())

  connectorSize.value = {
    width: Math.ceil(treeRect.width),
    height: Math.ceil(treeRect.height),
  }

  const rootCx = rootRect.left + rootRect.width / 2 - treeRect.left
  const rootBottom = rootRect.bottom - treeRect.top
  const railY = rootBottom + CONNECTOR_STEM_GAP
  const cornerR = CONNECTOR_CORNER_RADIUS

  const childPoints = childRects.map((rect, index) => ({
    x: rect.left + rect.width / 2 - treeRect.left,
    top: rect.top - treeRect.top,
    side: (index === 0 ? 'left' : index === 1 ? 'center' : 'right') as 'left' | 'center' | 'right',
  }))

  const [left, , right] = childPoints
  const paths = [
    `M ${rootCx} ${rootBottom} V ${railY}`,
    `M ${left.x + cornerR} ${railY} H ${right.x - cornerR}`,
    ...childPoints.map((child) => buildChildDrop(child.x, railY, child.top, child.side)),
  ]

  connectorPaths.value = paths
  connectorDots.value = [
    { cx: rootCx, cy: railY },
    ...childPoints.map((child) => ({
      cx: child.x,
      cy: child.top,
    })),
  ]
}

function scheduleStatusConnectorUpdate() {
  void nextTick(() => updateStatusConnector())
}

function moduleLabel(item: QuickModuleDef) {
  if (item.i18nKey) return t(`functions.items.${item.i18nKey}.name`)
  if (item.labelKey) return t(item.labelKey)
  return item.key
}

function statusBadgeClass(state: HealthState) {
  return { 'home-status-badge--up': state === 'up', 'home-status-badge--down': state === 'down', 'home-status-badge--unknown': state === 'unknown' }
}

function statusNodeClass(state: HealthState) {
  return { 'home-status-node--up': state === 'up', 'home-status-node--down': state === 'down', 'home-status-node--unknown': state === 'unknown' }
}

function statusLabel(state: HealthState) {
  if (state === 'up') return t('portal.dashboard.warRoom.statusUp')
  if (state === 'down') return t('portal.dashboard.warRoom.statusDown')
  return t('portal.dashboard.warRoom.statusUnknown')
}

async function checkHealth() {
  healthLoading.value = true
  try {
    const data = await fetchHealth()
    healthData.value = data
    healthStatus.value = data.status === 'UP' ? 'up' : 'down'
    lastRefreshAt.value = new Date()
  } catch {
    healthData.value = null
    healthStatus.value = 'down'
  } finally {
    healthLoading.value = false
  }
}

async function loadStats() {
  try {
    const [pending, done, today, pinned] = await Promise.all([
      fetchTodos({ completed: false }),
      fetchTodos({ completed: true }),
      fetchTodayTodos(),
      fetchPinnedTodos(),
    ])
    pendingCount.value = pending.length
    doneCount.value = done.length
    todayTodos.value = today
    pinnedTodos.value = pinned
    reminderCount.value = today.filter((row) => row.remindTime && row.completed !== 1).length
  } catch {
    pendingCount.value = 0
    doneCount.value = 0
    todayTodos.value = []
    pinnedTodos.value = []
    reminderCount.value = 0
  }
}

async function loadTodayTodos() {
  todayTodosLoading.value = true
  pinnedTodosLoading.value = true
  try {
    await loadStats()
  } finally {
    todayTodosLoading.value = false
    pinnedTodosLoading.value = false
  }
}

async function refreshAll() {
  manualArchiveAt.value = new Date()
  await Promise.all([checkHealth(), loadTodayTodos()])
  scheduleStatusConnectorUpdate()
}

function scrollModulesNext() {
  moduleScrollRef.value?.scrollBy({ left: 300, behavior: 'smooth' })
}

function openQuickModule(item: QuickModuleDef) {
  if (!item.route) return
  if (item.key === 'report') {
    router.push({ path: '/pomodoro', query: { tab: 'report' } })
    return
  }
  router.push(item.route)
}

function openFunctionByKey(key: HomeModuleKey) {
  const item = functionItems.find((row) => row.key === key)
  if (item?.route) {
    router.push(item.route)
    return
  }
  const railRoute = spotlightRoutes[key]
  if (railRoute) {
    router.push(railRoute)
    return
  }
  ElMessage.info(t('functions.openSoon', { name: t(`functions.items.${key}.name`) }))
}

function goFunctions() {
  router.push({ path: '/functions' })
}

function goTodosPage(filter: NbTodoFilter = 'all') {
  router.push({ path: '/todos', query: { filter } })
}

async function onToggleTodayTodo(item: NbTodoItem, checked: boolean) {
  const prev = item.completed
  item.completed = checked ? 1 : 0
  try {
    const result = await updateTodo(item.id, { completed: checked })
    if (checked) dismissTodoNotification(item.id)
    if (checked) {
      todayTodos.value = todayTodos.value.filter((row) => row.id !== item.id)
      doneCount.value += 1
      pendingCount.value = Math.max(0, pendingCount.value - 1)
      if (result.nextOccurrence) {
        todayTodos.value = [result.nextOccurrence, ...todayTodos.value]
        pendingCount.value += 1
      }
    } else {
      todayTodos.value = todayTodos.value.map((row) => (row.id === item.id ? result.item : row))
    }
    await refreshTodayCount()
  } catch {
    item.completed = prev
    ElMessage.error(t('notebook.todos.saveFailed'))
  }
}

async function onTogglePinnedTodo(item: NbTodoItem, checked: boolean) {
  const prev = item.completed
  item.completed = checked ? 1 : 0
  try {
    const result = await updateTodo(item.id, { completed: checked })
    if (checked) dismissTodoNotification(item.id)
    if (checked) {
      pinnedTodos.value = pinnedTodos.value.filter((row) => row.id !== item.id)
      doneCount.value += 1
      pendingCount.value = Math.max(0, pendingCount.value - 1)
      if (result.nextOccurrence?.pinned === 1) {
        pinnedTodos.value = [result.nextOccurrence, ...pinnedTodos.value]
        pendingCount.value += 1
      }
    } else {
      pinnedTodos.value = pinnedTodos.value.map((row) => (row.id === item.id ? result.item : row))
    }
    await refreshTodayCount()
  } catch {
    item.completed = prev
    ElMessage.error(t('notebook.todos.saveFailed'))
  }
}

onMounted(() => {
  void refreshAll()
  scheduleStatusConnectorUpdate()
  if (statusTreeRef.value) {
    statusConnectorObserver = new ResizeObserver(() => scheduleStatusConnectorUpdate())
    statusConnectorObserver.observe(statusTreeRef.value)
  }
  window.addEventListener('resize', scheduleStatusConnectorUpdate)
})

onUnmounted(() => {
  statusConnectorObserver?.disconnect()
  window.removeEventListener('resize', scheduleStatusConnectorUpdate)
})

watch(healthStatus, () => scheduleStatusConnectorUpdate())
</script>

<style scoped lang="scss">
.war-room-meta-btn {
  padding: 0;
  border: none;
  background: none;
  font-size: 12px;
  color: var(--wr-muted);
  cursor: pointer;

  &:disabled {
    opacity: 0.6;
    cursor: wait;
  }
}

.home-stats {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.home-stat-card {
  display: grid;
  grid-template-rows: 40px 1fr;
  align-items: start;
  justify-items: center;
  gap: 12px;
  min-height: 118px;
  padding: 18px 16px;
  background: var(--wr-card);
  border: 1px solid var(--wr-border);
  border-radius: 12px;
  box-shadow: var(--wr-shadow);
  text-align: center;

  &.is-clickable {
    cursor: pointer;
    transition: border-color 0.15s ease, box-shadow 0.15s ease;

    &:hover {
      border-color: color-mix(in srgb, var(--wr-border) 70%, var(--wr-stat-blue));
      box-shadow: 0 4px 14px rgb(37 99 235 / 8%);
    }
  }
}

.home-stat-card__head {
  grid-row: 1;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  width: 100%;
  height: 40px;

  :deep(.wr-stat-icon) {
    grid-column: 1;
    justify-self: end;
    margin-right: 10px;
    width: 40px;
    height: 40px;

    svg {
      width: 20px;
      height: 20px;
    }
  }
}

.home-stat-card__label {
  grid-column: 2;
  font-size: 14px;
  font-weight: 600;
  color: #2d2d2d;
  line-height: 1.3;
  text-align: center;
  white-space: nowrap;
}

.home-stat-card__value {
  grid-row: 2;
  align-self: center;
  width: 100%;
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  text-align: center;
}

.home-stat-card__hint {
  grid-row: 2;
  align-self: center;
  margin: 0;
  width: 100%;
  font-size: 13px;
  line-height: 1.45;
  color: #555;
  text-align: center;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.home-modules {
  margin-bottom: 16px;
}

.home-pinned {
  margin-bottom: 16px;
}

.home-pinned__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;

  .home-section-title {
    margin: 0;
  }
}

.home-pinned__link {
  padding: 0;
  border: none;
  background: transparent;
  color: var(--wr-stat-blue);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

.home-pinned__list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.home-pinned-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  background: var(--wr-card);
  border: 1px solid var(--wr-border);
  border-radius: 12px;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    border-color: color-mix(in srgb, var(--wr-border) 70%, #f59e0b);
    box-shadow: 0 4px 14px rgb(245 158 11 / 8%);
  }
}

.home-pinned-card__star {
  font-size: 18px;
  color: #f59e0b;
}

.home-pinned-card__body {
  min-width: 0;
}

.home-pinned-card__title {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.45;
  color: var(--wr-text);
}

.home-pinned-card__desc {
  margin: 0;
  font-size: 12px;
  line-height: 1.4;
  color: var(--wr-text-secondary);
}

.home-pinned__empty {
  margin: 0;
  padding: 20px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--wr-muted);
  border: 1px dashed var(--wr-border);
  border-radius: 10px;
  background: rgb(255 255 255 / 60%);
}

.home-modules__box {
  display: flex;
  align-items: stretch;
  gap: 12px;
  padding: 12px;
  background: var(--wr-card);
  border: 1px solid var(--wr-border);
  border-radius: 12px;
  box-shadow: var(--wr-shadow);
}

.home-modules__inner {
  display: flex;
  flex: 1;
  gap: 12px;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }

  .wr-module-card {
    flex: 1 1 0;
    min-width: 0;
  }
}

.home-modules__arrow {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  align-self: stretch;
  background: #f9fafb;
  border: 1px solid var(--wr-border);
  border-radius: 8px;
  color: #666;
  cursor: pointer;

  &:hover {
    background: #f3f4f6;
    color: #333;
  }
}

.home-section-title {
  margin: 0 0 18px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.home-status {
  display: flex;
  align-items: stretch;
  min-height: 220px;
}

.home-status__diagram {
  flex: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  padding: 12px 20px;
}

.home-status__divider {
  flex-shrink: 0;
  align-self: stretch;
  width: 1px;
  margin: 8px 0;
  background: var(--wr-border);
}

.home-status__legend-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  padding: 12px 20px;
}

.home-status__tree {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 520px;
  overflow: visible;
}

.home-status__connector-overlay {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
  pointer-events: none;
  overflow: visible;
}

.home-status__connector-line {
  fill: none;
  stroke: #5a9a68;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
  vector-effect: non-scaling-stroke;
}

.home-status__connector-dot {
  position: absolute;
  z-index: 3;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #389845;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.home-status-node {
  position: relative;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;

  &--up {
    border-color: #bad2c0;
    background: var(--wr-up-bg);
  }

  &--down {
    border-color: #fca5a5;
    background: #fef2f2;
  }

  &--unknown {
    border-color: #e5e7eb;
    background: #fafafa;
  }
}

.home-status-node__name {
  font-size: 14px;
  font-weight: 700;
  color: #374151;
}

.home-status__children {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  width: 100%;
  margin-top: 44px;
}

.home-status__child {
  display: flex;
  justify-content: center;
}

.home-status-badge {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;

  &--up {
    background: #389845;
    color: #fff;
    margin-left: 8px;
    font-weight: 100;
  }

  &--down {
    background: #fee2e2;
    color: #b91c1c;
  }

  &--unknown {
    background: #f3f4f6;
    color: #6b7280;
  }
}

.home-status__legend-title {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 700;
  color: #666;
}

.home-status__legend-list {
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 12px;
  color: #888;
  line-height: 2.2;

  li {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;

  &--up {
    background: #389845;
  }

  &--warn {
    background: #f59e0b;
  }

  &--down {
    background: #ef4444;
  }

  &--unknown {
    background: #9ca3af;
  }
}

.home-tasks {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  min-height: 60px;
  margin-top: 16px;
}

.home-task-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid var(--wr-border);
  border-radius: 12px;
  box-shadow: var(--wr-shadow);
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;

  &:hover {
    border-color: #d1d5db;
    box-shadow: 0 6px 16px rgb(0 0 0 / 6%);
  }
}

.home-task-card__index {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--wr-index-bg);
  color: var(--wr-index-text);
  font-size: 15px;
  font-weight: 700;
}

.home-task-card__title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
}

.home-task-card__desc {
  margin: 0;
  font-size: 12px;
  color: #888;
  line-height: 1.5;
}

.home-task-card__body {
  flex: 1;
  min-width: 0;
}

@media (max-width: 1280px) {
  .home-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .home-tasks {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .home-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .home-status {
    flex-direction: column;
    min-height: 0;
  }

  .home-status__divider {
    width: auto;
    height: 1px;
    margin: 8px 0;
  }

  .home-status__diagram,
  .home-status__legend-wrap {
    padding: 12px 0;
  }

  .home-tasks {
    grid-template-columns: 1fr;
  }
}
</style>
