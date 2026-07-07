<template>
  <div class="mobile-todos">
    <!-- 问候语 -->
    <div class="ha-greeting">
      <div class="ha-greeting__left">
        <h2 class="ha-greeting__title">{{ greetingText }} {{ greetingEmoji }}</h2>
        <p class="ha-greeting__sub">
          今天还有 <strong>{{ todayCount }}</strong> 件事等着你
        </p>
      </div>
      <button class="ha-greeting__refresh" :class="{ loading: loading }" @click="loadTodos">
        ↻
      </button>
    </div>

    <!-- 进度环 + 统计 -->
    <div class="ha-stats-card">
      <div class="ha-ring">
        <svg viewBox="0 0 52 52" class="ha-ring__svg">
          <circle cx="26" cy="26" r="22" fill="none" stroke="#e2e8f0" stroke-width="4.5" />
          <circle
            cx="26" cy="26" r="22"
            fill="none" stroke="#2563eb" stroke-width="4.5"
            stroke-linecap="round"
            :stroke-dasharray="138.2"
            :stroke-dashoffset="ringOffset"
            transform="rotate(-90 26 26)"
          />
        </svg>
        <span class="ha-ring__text">{{ doneRatio }}%</span>
      </div>
      <div class="ha-stats">
        <div class="ha-stat">
          <span class="ha-stat__value ha-stat__value--blue">{{ pendingCount }}</span>
          <span class="ha-stat__label">待完成</span>
        </div>
        <div class="ha-stat-divider"></div>
        <div class="ha-stat">
          <span class="ha-stat__value ha-stat__value--orange">{{ todayCount }}</span>
          <span class="ha-stat__label">今日</span>
        </div>
        <div class="ha-stat-divider"></div>
        <div class="ha-stat">
          <span class="ha-stat__value ha-stat__value--red">{{ overdueCount }}</span>
          <span class="ha-stat__label">逾期</span>
        </div>
      </div>
    </div>

    <!-- 筛选标签 -->
    <div class="ha-filter-tabs">
      <button
        v-for="tab in filterOptions"
        :key="tab.key"
        class="ha-filter-tab"
        :class="{ active: activeFilter === tab.key }"
        @click="setFilter(tab.key)"
      >
        <span class="ha-filter-tab__icon">{{ tab.icon }}</span>
        <span class="ha-filter-tab__label">{{ tab.label }}</span>
      </button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="ha-loading">
      <div class="ha-loading__spinner"></div>
      <span>加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!visibleGroups.length" class="ha-empty">
      <div class="ha-empty__icon">🎉</div>
      <div class="ha-empty__title">{{ t('notebook.todos.empty') }}</div>
      <p class="ha-empty__sub">在下方输入框添加第一条待办吧</p>
    </div>

    <!-- 分组列表 -->
    <div v-else class="ha-group-list">
      <div
        v-for="group in visibleGroups"
        :key="group.key"
        class="ha-group"
      >
        <button class="ha-group__head" @click="toggleGroup(group.key)">
          <span class="ha-group__icon">{{ groupIcon(group.key) }}</span>
          <span class="ha-group__title">{{ groupTitle(group.key) }}</span>
          <span class="ha-group__count">{{ group.items.length }}</span>
          <span class="ha-group__arrow" :class="{ expanded: !collapsedGroups.has(group.key) }">›</span>
        </button>
        <div v-if="!collapsedGroups.has(group.key)" class="ha-group__body">
          <SchemeADoodleFrame
            v-for="item in group.items"
            :key="item.id"
            class="ha-todo-card"
            :class="{ 'is-done': item.completed }"
            :color="cardFrameColor(group.key)"
            :seed="doodleSeedFromKey('todo-' + item.id)"
            sketch
            :stroke-width="2.5"
            :shadow="false"
            tag="div"
          >
            <div class="ha-todo-card__inner">
              <button
                class="ha-card-check"
                :class="{ checked: item.completed }"
                :disabled="togglingId === item.id"
                @click="toggleComplete(item)"
              >
                <span v-if="item.completed" class="ha-card-check__inner">✓</span>
                <span v-else-if="togglingId === item.id" class="ha-card-check__spinner"></span>
              </button>
              <div class="ha-card-body" @click="openEdit(item)">
                <span class="ha-card-body__text">{{ item.content }}</span>
                <div class="ha-card-body__meta">
                  <span v-if="item.dueTime" class="ha-card-time" :class="{ overdue: isItemOverdue(item) }">
                    ⏰ {{ formatTime(item.dueTime) }}
                  </span>
                  <span v-if="item.pinned" class="ha-tag ha-tag--pin">📌 置顶</span>
                  <span v-if="item.repeatType && item.repeatType !== 'NONE'" class="ha-tag ha-tag--repeat">🔁 {{ repeatLabel(item.repeatType) }}</span>
                  <span v-if="item.remindTime" class="ha-tag ha-tag--remind">🔔 {{ formatTime(item.remindTime) }}</span>
                </div>
              </div>
              <button class="ha-card-more" @click="openMenu(item)">⋮</button>
            </div>
          </SchemeADoodleFrame>
        </div>
      </div>
    </div>

    <!-- 底部快速添加 -->
    <div class="ha-bottom-bar">
      <input
        v-model="newContent"
        class="ha-bottom-bar__input"
        :placeholder="t('notebook.todos.create')"
        @keyup.enter="createTodo"
      />
      <button
        class="ha-bottom-bar__btn"
        :disabled="!newContent.trim() || creating"
        @click="createTodo"
      >
        {{ creating ? '...' : '添加' }}
      </button>
    </div>

    <!-- 操作菜单 -->
    <Teleport to="body">
      <div v-if="menuVisible" class="ha-menu-overlay" @click="closeMenu"></div>
      <Transition name="ha-slide">
        <div v-if="menuVisible" class="ha-menu-sheet">
          <div class="ha-menu-sheet__handle"></div>
          <div class="ha-menu-sheet__title">{{ menuItem?.content }}</div>
          <div class="ha-menu-sheet__divider"></div>
          <button class="ha-menu-sheet__action" @click="togglePin">
            {{ menuItem?.pinned ? '取消置顶' : '置顶' }}
          </button>
          <button class="ha-menu-sheet__action ha-menu-sheet__action--danger" @click="deleteTodo">
            删除
          </button>
          <div class="ha-menu-sheet__divider"></div>
          <button class="ha-menu-sheet__action ha-menu-sheet__action--cancel" @click="closeMenu">
            取消
          </button>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  fetchTodos,
  createTodo as apiCreateTodo,
  updateTodo as apiUpdateTodo,
  removeTodo as apiRemoveTodo,
} from '@/api/notebook/todo'
import type { NbTodoItem, NbTodoSaveRequest } from '@/api/notebook/todo'
import { classifyTodo, isToday, isOverdue, formatDateTime } from '@/views/notebook/todoGroup'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import { doodleSeedFromKey } from '@/mobile/utils/doodleSeed'

const { t } = useI18n()

// 数据状态
const todos = ref<NbTodoItem[]>([])
const loading = ref(false)
const creating = ref(false)
const togglingId = ref<number | null>(null)

// 筛选
const activeFilter = ref<string>('all')
const collapsedGroups = reactive(new Set<string>(['done']))

// 新建内容
const newContent = ref('')

// 底部菜单
const menuVisible = ref(false)
const menuItem = ref<NbTodoItem | null>(null)

const filterOptions = [
  { key: 'all', icon: '📋', label: '全部' },
  { key: 'today', icon: '📅', label: '今日' },
  { key: 'overdue', icon: '⚠️', label: '逾期' },
  { key: 'done', icon: '✅', label: '已完成' },
]

// ---- 问候语 ----
const hour = new Date().getHours()
const greetingText = hour < 6 ? '夜深了' : hour < 9 ? '早上好' : hour < 12 ? '上午好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好'
const greetingEmoji = hour < 6 ? '🌙' : hour < 9 ? '☀️' : hour < 12 ? '☀️' : hour < 14 ? '🌤' : hour < 18 ? '🌇' : '🌆'

// ---- 统计计算 ----
const pendingCount = computed(() => todos.value.filter(t => !t.completed).length)
const todayCount = computed(() => todos.value.filter(t => !t.completed && (isToday(t.dueTime) || isToday(t.remindTime))).length)
const overdueCount = computed(() => todos.value.filter(t => !t.completed && isOverdue(t)).length)
const doneCount = computed(() => todos.value.filter(t => t.completed).length)
const doneRatio = computed(() => todos.value.length ? Math.round((doneCount.value / todos.value.length) * 100) : 0)
const ringOffset = computed(() => 138.2 - (138.2 * doneRatio.value) / 100)

// ---- 加载数据 ----
async function loadTodos() {
  loading.value = true
  try {
    const data = await fetchTodos()
    todos.value = data.sort((a, b) => {
      // 置顶优先，逾期其次，然后按 sortOrder
      const pa = a.pinned ? 1 : 0
      const pb = b.pinned ? 1 : 0
      if (pa !== pb) return pb - pa
      const oa = isOverdue(a) ? 1 : 0
      const ob = isOverdue(b) ? 1 : 0
      if (oa !== ob) return ob - oa
      return (a.sortOrder ?? 0) - (b.sortOrder ?? 0)
    })
  } catch {
    ElMessage.error(t('notebook.todos.loadFailed'))
  } finally {
    loading.value = false
  }
}

// ---- 分组 ----
function groupTodos(items: NbTodoItem[]): { key: string; items: NbTodoItem[] }[] {
  const pending = items.filter(t => !t.completed)
  const groups: { key: string; items: NbTodoItem[] }[] = [
    { key: 'overdue', items: pending.filter(t => isOverdue(t)) },
    { key: 'today', items: pending.filter(t => !isOverdue(t) && (isToday(t.dueTime) || isToday(t.remindTime))) },
    { key: 'upcoming', items: pending.filter(t => !isOverdue(t) && !isToday(t.dueTime) && !isToday(t.remindTime) && (t.dueTime || t.remindTime)) },
    { key: 'unscheduled', items: pending.filter(t => !isOverdue(t) && !isToday(t.dueTime) && !isToday(t.remindTime) && !t.dueTime && !t.remindTime) },
    { key: 'done', items: items.filter(t => t.completed) },
  ]
  return groups.filter(g => g.items.length > 0)
}

const visibleGroups = computed(() => {
  let result = groupTodos(todos.value)
  if (activeFilter.value === 'today') {
    result = result.filter(g => g.key === 'today' || g.key === 'overdue')
  } else if (activeFilter.value === 'overdue') {
    result = result.filter(g => g.key === 'overdue')
  } else if (activeFilter.value === 'done') {
    result = result.filter(g => g.key === 'done')
  }
  return result
})

// ---- 过滤 ----
function setFilter(key: string) {
  activeFilter.value = key
  // 切换 filter 时自动展开逾期组
  if (key === 'overdue' || key === 'today') {
    collapsedGroups.delete('overdue')
  }
  if (key === 'done') {
    collapsedGroups.delete('done')
  }
}

// ---- 切换折叠 ----
function toggleGroup(key: string) {
  if (collapsedGroups.has(key)) {
    collapsedGroups.delete(key)
  } else {
    collapsedGroups.add(key)
  }
}

// ---- 分组展示 ----
function groupIcon(key: string) {
  const map: Record<string, string> = { overdue: '🔴', today: '🟡', upcoming: '🟢', unscheduled: '⚪', done: '✅' }
  return map[key] || '📋'
}

function groupTitle(key: string) {
  const map: Record<string, string> = { overdue: '已逾期', today: '今日待办', upcoming: '未来安排', unscheduled: '未安排', done: '已完成' }
  return map[key] || key
}

// ---- 工具函数 ----
function isItemOverdue(item: NbTodoItem): boolean {
  return !item.completed && isOverdue(item)
}

function formatTime(value?: string | null): string {
  if (!value) return ''
  return formatDateTime(value)
}

function repeatLabel(type: string): string {
  const map: Record<string, string> = { DAILY: '每天', WEEKLY: '每周', MONTHLY: '每月', YEARLY: '每年' }
  return map[type] || type
}

function cardFrameColor(groupKey: string): string {
  const map: Record<string, string> = {
    overdue: '#ef4444',
    today: '#f59e0b',
    upcoming: '#22c55e',
    unscheduled: '#94a3b8',
    done: '#94a3b8',
  }
  return map[groupKey] || '#2563eb'
}

// ---- 操作 ----
async function toggleComplete(item: NbTodoItem) {
  togglingId.value = item.id
  try {
    const body: NbTodoSaveRequest = { completed: !item.completed }
    const result = await apiUpdateTodo(item.id, body)
    // 更新本地数据
    item.completed = result.item.completed
    item.updateTime = result.item.updateTime
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  } finally {
    togglingId.value = null
  }
}

async function createTodo() {
  const content = newContent.value.trim()
  if (!content || creating.value) return
  creating.value = true
  try {
    const newItem = await apiCreateTodo({ content })
    todos.value.unshift(newItem)
    newContent.value = ''
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  } finally {
    creating.value = false
  }
}

// ---- 点击卡片 ----
function openEdit(item: NbTodoItem) {
  openMenu(item)
}

// ---- 菜单 ----
function openMenu(item: NbTodoItem) {
  menuItem.value = item
  menuVisible.value = true
}

function closeMenu() {
  menuVisible.value = false
  menuItem.value = null
}

async function togglePin() {
  if (!menuItem.value) return
  const item = menuItem.value
  menuVisible.value = false
  try {
    const result = await apiUpdateTodo(item.id, { pinned: !item.pinned })
    item.pinned = result.item.pinned
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  }
}

async function deleteTodo() {
  if (!menuItem.value) return
  const item = menuItem.value
  menuVisible.value = false
  try {
    await ElMessageBox.confirm(t('notebook.todos.deleteConfirm'), t('common.confirm'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
    await apiRemoveTodo(item.id)
    todos.value = todos.value.filter(t => t.id !== item.id)
    ElMessage.success(t('notebook.todos.deleted'))
  } catch {
    // canceled or error
  }
}

// ---- 生命周期 ----
onMounted(() => {
  loadTodos()
})
</script>

<style scoped lang="scss">
.mobile-todos {
  min-height: calc(100vh - 56px);
  background: #faf8f5;
  padding: 16px;
  padding-bottom: 80px;
  overflow-y: auto;
}

// ---- 问候语 ----
.ha-greeting {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}

.ha-greeting__left {
  flex: 1;
}

.ha-greeting__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 22px;
  color: #1e293b;
  margin: 0 0 2px;
}

.ha-greeting__sub {
  font-size: 13px;
  color: #64748b;
  margin: 0;

  strong {
    color: #2563eb;
    font-weight: 700;
  }
}

.ha-greeting__refresh {
  background: none;
  border: none;
  font-size: 22px;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px 8px;
  transition: transform 0.3s ease;

  &.loading {
    animation: ha-spin 0.8s linear infinite;
  }
}

@keyframes ha-spin {
  100% { transform: rotate(360deg); }
}

// ---- 统计卡片 ----
.ha-stats-card {
  display: flex;
  align-items: center;
  gap: 18px;
  background: white;
  border-radius: 16px;
  padding: 14px 18px;
  margin-bottom: 14px;
  border: 2px solid #e2e8f0;
}

.ha-ring {
  position: relative;
  width: 60px;
  height: 60px;
  flex-shrink: 0;
}

.ha-ring__svg {
  width: 100%;
  height: 100%;
}

.ha-ring__text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  color: #1e293b;
}

.ha-stats {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.ha-stat {
  text-align: center;
  flex: 1;
}

.ha-stat__value {
  font-size: 20px;
  font-weight: 800;
  display: block;

  &--blue { color: #2563eb; }
  &--orange { color: #f59e0b; }
  &--red { color: #ef4444; }
}

.ha-stat__label {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 1px;
}

.ha-stat-divider {
  width: 1px;
  height: 28px;
  background: #e2e8f0;
}

// ---- 筛选标签 ----
.ha-filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  overflow-x: auto;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar { display: none; }
}

.ha-filter-tab {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 7px 16px;
  border-radius: 999px;
  border: 2px solid #e2e8f0;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
  font: inherit;

  &.active {
    border-color: #2563eb;
    background: #eff6ff;
  }
}

.ha-filter-tab__icon { font-size: 13px; }

.ha-filter-tab__label {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 13px;
  color: #1e293b;
}

// ---- 加载 & 空状态 ----
.ha-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 60px 0;
  font-size: 14px;
  color: #94a3b8;
}

.ha-loading__spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e2e8f0;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: ha-spin 0.6s linear infinite;
}

.ha-empty {
  text-align: center;
  padding: 60px 20px;
}

.ha-empty__icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.ha-empty__title {
  font-size: 16px;
  color: #1e293b;
  font-weight: 600;
  margin-bottom: 6px;
}

.ha-empty__sub {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}

// ---- 分组列表 ----
.ha-group-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ha-group {
  background: white;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.ha-group__head {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 11px 14px;
  border: none;
  background: transparent;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.ha-group__icon { font-size: 15px; }

.ha-group__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
}

.ha-group__count {
  margin-left: auto;
  font-size: 11px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 999px;
}

.ha-group__arrow {
  font-size: 18px;
  color: #94a3b8;
  margin-left: 4px;
  transition: transform 0.2s ease;

  &.expanded {
    transform: rotate(90deg);
  }
}

.ha-group__body {
  padding: 6px 14px 14px;
}

// ---- 待办卡片（手绘边框） ----
.ha-todo-card {
  margin-bottom: 8px;
  border-radius: 16px;

  &:last-child { margin-bottom: 0; }

  &.is-done {
    opacity: 0.55;

    .ha-card-body__text {
      text-decoration: line-through;
      color: #94a3b8;
    }
  }
}

.ha-todo-card__inner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
}

.ha-card-check {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #cbd5e1;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  margin-top: 2px;
  padding: 0;
  transition: all 0.2s ease;

  &.checked {
    background: #22c55e;
    border-color: #22c55e;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.ha-card-check__inner {
  color: white;
  font-size: 11px;
  font-weight: 700;
}

.ha-card-check__spinner {
  width: 12px;
  height: 12px;
  border: 2px solid #94a3b8;
  border-top-color: white;
  border-radius: 50%;
  animation: ha-spin 0.6s linear infinite;
}

.ha-card-body {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.ha-card-body__text {
  font-size: 14px;
  color: #1e293b;
  display: block;
  line-height: 1.4;
}

.ha-card-body__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
}

.ha-card-time {
  font-size: 11px;
  color: #64748b;

  &.overdue {
    color: #ef4444;
    font-weight: 600;
  }
}

.ha-tag {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #64748b;
  line-height: 1.3;

  &--pin { background: #fef3c7; color: #d97706; }
  &--repeat { background: #dbeafe; color: #2563eb; }
  &--remind { background: #fce7f3; color: #db2777; }
}

.ha-card-more {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 18px;
  cursor: pointer;
  padding: 4px 2px;
  line-height: 1;
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 1px;
}

// ---- 底部添加栏 ----
.ha-bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 8px;
  background: white;
  border-top: 2px solid #2563eb;
  padding: 8px 16px;
  padding-bottom: calc(8px + env(safe-area-inset-bottom, 0px));
  box-shadow: 0 -4px 16px rgba(37, 99, 235, 0.1);
  z-index: 10;
}

.ha-bottom-bar__input {
  flex: 1;
  border: 2px solid #e2e8f0;
  border-radius: 999px;
  outline: none;
  padding: 10px 16px;
  font-size: 14px;
  color: #1e293b;
  transition: border-color 0.2s ease;

  &:focus {
    border-color: #2563eb;
  }

  &::placeholder {
    color: #94a3b8;
  }
}

.ha-bottom-bar__btn {
  padding: 10px 24px;
  border: none;
  background: #2563eb;
  color: white;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s ease;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &:not(:disabled):hover {
    opacity: 0.9;
  }
}

// ---- 底部操作菜单 ----
.ha-menu-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 100;
}

.ha-menu-sheet {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-radius: 20px 20px 0 0;
  z-index: 101;
  padding: 12px 20px calc(12px + env(safe-area-inset-bottom, 0px));
}

.ha-menu-sheet__handle {
  width: 36px;
  height: 4px;
  border-radius: 999px;
  background: #e2e8f0;
  margin: 0 auto 16px;
}

.ha-menu-sheet__title {
  font-size: 15px;
  color: #1e293b;
  font-weight: 600;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ha-menu-sheet__divider {
  height: 1px;
  background: #f1f5f9;
  margin: 8px 0;
}

.ha-menu-sheet__action {
  width: 100%;
  padding: 14px 0;
  border: none;
  background: transparent;
  font-size: 16px;
  color: #1e293b;
  cursor: pointer;
  text-align: center;

  &--danger {
    color: #ef4444;
  }

  &--cancel {
    font-weight: 600;
  }
}

// ---- Transition ----
.ha-slide-enter-active,
.ha-slide-leave-active {
  transition: transform 0.3s ease;
}

.ha-slide-enter-from,
.ha-slide-leave-to {
  transform: translateY(100%);
}
</style>
