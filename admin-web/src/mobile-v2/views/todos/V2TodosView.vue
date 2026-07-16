<template>
  <V2Page>
    <V2Card>
      <div class="v2-todos-greeting">
        <div class="v2-todos-greeting__left">
          <div class="v2-todos-greeting__title">{{ greetingText }} {{ greetingEmoji }}</div>
          <div class="v2-todos-greeting__sub">
            今天还有 <strong>{{ todayCount }}</strong> 件事等着你
          </div>
        </div>
        <button class="v2-todos-greeting__refresh" :class="{ loading }" @click="loadTodos">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 4 23 10 17 10" />
            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
          </svg>
        </button>
      </div>

      <div class="v2-todos-stats">
        <div class="v2-todos-ring">
          <svg viewBox="0 0 52 52" class="v2-todos-ring__svg">
            <circle cx="26" cy="26" r="22" fill="none" stroke="var(--wr-border, #e8ecef)" stroke-width="4.5" />
            <circle
              cx="26" cy="26" r="22"
              fill="none"
              stroke="var(--wr-stat-blue, #2563eb)"
              stroke-width="4.5"
              stroke-linecap="round"
              :stroke-dasharray="ringCircumference"
              :stroke-dashoffset="ringOffset"
              transform="rotate(-90 26 26)"
            />
          </svg>
          <span class="v2-todos-ring__text">{{ doneRatio }}%</span>
        </div>
        <div class="v2-todos-stat-group">
          <div class="v2-todos-stat">
            <span class="v2-todos-stat__value v2-todos-stat__value--blue">{{ pendingCount }}</span>
            <span class="v2-todos-stat__label">{{ t('notebook.todos.stats.pending') }}</span>
          </div>
          <div class="v2-todos-stat-divider" />
          <div class="v2-todos-stat">
            <span class="v2-todos-stat__value v2-todos-stat__value--orange">{{ todayCount }}</span>
            <span class="v2-todos-stat__label">{{ t('notebook.todos.stats.today') }}</span>
          </div>
          <div class="v2-todos-stat-divider" />
          <div class="v2-todos-stat">
            <span class="v2-todos-stat__value v2-todos-stat__value--red">{{ overdueCount }}</span>
            <span class="v2-todos-stat__label">{{ t('notebook.todos.stats.overdue') }}</span>
          </div>
        </div>
      </div>

      <div v-if="pinnedTodos.length" class="v2-todos-pinned">
        <div class="v2-todos-pinned__header">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#d97706" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="17" x2="12" y2="22" />
            <path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24Z" />
          </svg>
          <span>{{ t('notebook.pin') }}</span>
          <span class="v2-todos-pinned__count">{{ pinnedTodos.length }}条</span>
        </div>
        <div class="v2-todos-pinned__list">
          <div
            v-for="item in pinnedTodos"
            :key="item.id"
            class="v2-todos-pinned__item"
          >
            <div class="v2-todos-pinned__content">{{ item.content }}</div>
            <div class="v2-todos-pinned__meta">
              <span v-if="item.dueTime" class="v2-todos-pinned__time">{{ formatTime(item.dueTime) }}</span>
            </div>
            <button
              class="v2-todos-pinned__unpin"
              :disabled="unpinningId === item.id"
              @click="confirmUnpin(item)"
            >
              <svg v-if="unpinningId !== item.id" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              <span v-else class="v2-todos-pinned__unpin-spinner" />
            </button>
          </div>
        </div>
      </div>
    </V2Card>

    <div class="v2-todos-filters">
      <button
        v-for="opt in filterOptions"
        :key="opt.id"
        class="v2-todos-filter-btn"
        :class="{ 'is-active': activeFilter === opt.id }"
        @click="setFilter(opt.id)"
      >
        {{ opt.label }}
      </button>
    </div>

    <div v-if="loading" class="v2-todos-status">
      <div class="v2-todos-status__spinner" />
      <span>{{ t('notebook.todos.loading') }}</span>
    </div>

    <div v-else-if="!visibleGroups.length" class="v2-todos-empty">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--wr-muted, #999999)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="M9 11l3 3L22 4" />
        <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
      </svg>
      <div class="v2-todos-empty__title">{{ t('notebook.todos.empty') }}</div>
      <p class="v2-todos-empty__sub">在下方输入框添加第一条待办吧</p>
    </div>

    <div v-else class="v2-todos-groups">
      <div
        v-for="group in visibleGroups"
        :key="group.key"
        class="v2-todos-group"
      >
        <button class="v2-todos-group__head" @click="toggleGroup(group.key)">
          <span class="v2-todos-group__icon" :style="{ color: groupIconColor(group.key) }" v-html="groupSvg(group.key)" />
          <span class="v2-todos-group__title">{{ groupTitle(group.key) }}</span>
          <span class="v2-todos-group__count">{{ group.items.length }}</span>
          <svg
            class="v2-todos-group__arrow"
            :class="{ expanded: !collapsedGroups.has(group.key) }"
            width="18" height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </button>
        <div v-if="!collapsedGroups.has(group.key)" class="v2-todos-group__body">
          <div
            v-for="item in group.items"
            :key="item.id"
            class="v2-todos-item"
            :class="{ 'v2-todos-item--done': item.completed }"
          >
            <button
              class="v2-todos-item__check"
              :class="{ 'v2-todos-item__check--checked': item.completed }"
              :disabled="togglingId === item.id"
              @click="toggleComplete(item)"
            >
              <svg v-if="item.completed" viewBox="0 0 24 24" width="12" height="12">
                <path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              <span v-else-if="togglingId === item.id" class="v2-todos-item__check-spinner" />
            </button>
            <div class="v2-todos-item__body" @click="openMenu(item)">
              <span class="v2-todos-item__text">{{ item.content }}</span>
              <div class="v2-todos-item__meta">
                <span v-if="item.dueTime" class="v2-todos-item__time" :class="{ 'v2-todos-item__time--overdue': isItemOverdue(item) }">
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
                  </svg>
                  {{ formatTime(item.dueTime) }}
                </span>
                <span v-if="item.remindTime" class="v2-tag v2-tag--remind">
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                    <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                  </svg>
                  {{ formatTime(item.remindTime) }}
                </span>
                <span v-if="item.repeatType && item.repeatType !== 'NONE'" class="v2-tag v2-tag--repeat">
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="23 4 23 10 17 10" />
                    <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
                  </svg>
                  {{ repeatLabel(item.repeatType) }}
                </span>
              </div>
            </div>
          </div>
          <button
            v-if="group.key === 'done' && hasMoreDone"
            class="v2-todos-load-more"
            @click="loadMoreDone"
          >
            {{ t('notebook.todos.loadMore') || '加载更多' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 底部快速添加 -->
    <div class="v2-todos-bottom-bar">
      <input
        v-model="newContent"
        class="v2-todos-bottom-bar__input"
        :placeholder="t('notebook.todos.addPlaceholder')"
        @keyup.enter="createTodo"
      />
      <button
        class="v2-todos-bottom-bar__btn"
        :disabled="!newContent.trim() || creating"
        @click="createTodo"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        <span>{{ creating ? '...' : t('notebook.todos.add') }}</span>
      </button>
    </div>

    <!-- 操作菜单 -->
    <Teleport to="body">
      <Transition name="v2-todos-sheet">
        <div v-if="menuVisible" class="v2-todos-sheet-overlay" @click.self="closeMenu">
          <div class="v2-todos-sheet">
            <div class="v2-todos-sheet__title">{{ menuItem?.content }}</div>
            <button class="v2-todos-sheet__action" @click="openEditDialog">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
              </svg>
              {{ t('notebook.todos.edit') }}
            </button>
            <button class="v2-todos-sheet__action" @click="togglePin">
              <svg v-if="menuItem?.pinned" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="17" x2="12" y2="22" />
                <path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24Z" />
              </svg>
              <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="17" x2="12" y2="22" />
                <path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24Z" />
              </svg>
              {{ menuItem?.pinned ? t('notebook.todos.unpin') : t('notebook.todos.pin') }}
            </button>
            <button class="v2-todos-sheet__action v2-todos-sheet__action--danger" @click="openDeleteDialog">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6" />
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              </svg>
              {{ t('notebook.todos.delete') }}
            </button>
            <div class="v2-todos-sheet__divider" />
            <button class="v2-todos-sheet__action v2-todos-sheet__action--cancel" @click="closeMenu">
              {{ t('app.cancel') }}
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editVisible"
      :title="t('notebook.todos.editTitle')"
      width="90%"
      :close-on-click-modal="false"
      top="20vh"
      class="v2-todos-edit-dialog"
    >
      <div class="v2-todos-edit-form">
        <label class="v2-todos-edit-form__label">{{ t('notebook.todos.content') }}</label>
        <input
          v-model="editForm.content"
          class="v2-todos-edit-form__input"
          :placeholder="t('notebook.todos.contentRequired')"
          type="text"
        />

        <label class="v2-todos-edit-form__label">{{ t('notebook.todos.dueTime') }}</label>
        <input
          v-model="editForm.dueTime"
          class="v2-todos-edit-form__input"
          type="datetime-local"
        />

        <label class="v2-todos-edit-form__label">{{ t('notebook.todos.remindTime') }}</label>
        <input
          v-model="editForm.remindTime"
          class="v2-todos-edit-form__input"
          type="datetime-local"
        />

        <label class="v2-todos-edit-form__label">{{ t('notebook.todos.repeatLabel') }}</label>
        <div class="v2-todos-edit-form__repeat-group">
          <button
            v-for="rt in repeatTypeOptions"
            :key="rt.value"
            class="v2-todos-edit-form__repeat-btn"
            :class="{ active: editForm.repeatType === rt.value }"
            @click="editForm.repeatType = rt.value"
          >
            {{ rt.label }}
          </button>
        </div>
      </div>
      <template #footer>
        <div class="v2-todos-edit-form__footer">
          <button class="v2-todos-edit-form__cancel" @click="editVisible = false">{{ t('app.cancel') }}</button>
          <button class="v2-todos-edit-form__confirm" @click="saveEdit">{{ t('notebook.todos.edit') }}</button>
        </div>
      </template>
    </el-dialog>

    <!-- 取消置顶确认 -->
    <Teleport to="body">
      <Transition name="v2-todos-delete">
        <div
          v-if="unpinTarget"
          class="v2-todos-delete-overlay"
          @click.self="cancelUnpin"
        >
          <div class="v2-todos-delete-panel">
            <div class="v2-todos-delete-panel__icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#d97706" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="17" x2="12" y2="22" />
                <path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24Z" />
              </svg>
            </div>
            <h3 class="v2-todos-delete-panel__title">{{ t('notebook.todos.unpin') }}</h3>
            <p class="v2-todos-delete-panel__content">确定取消「{{ unpinTarget.content }}」的置顶吗？</p>
            <div class="v2-todos-delete-panel__actions">
              <button class="v2-todos-delete-panel__btn v2-todos-delete-panel__btn--cancel" @click="cancelUnpin">
                {{ t('app.cancel') }}
              </button>
              <button
                class="v2-todos-delete-panel__btn v2-todos-delete-panel__btn--confirm"
                style="background: #d97706"
                :disabled="unpinningId !== null && unpinningId > 0"
                @click="doUnpin"
              >
                {{ unpinningId !== null && unpinningId > 0 ? '...' : t('app.confirm') }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 删除确认 -->
    <Teleport to="body">
      <Transition name="v2-todos-delete">
        <div
          v-if="deleteTarget"
          class="v2-todos-delete-overlay"
          @click.self="cancelDelete"
        >
          <div class="v2-todos-delete-panel">
            <div class="v2-todos-delete-panel__icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6" />
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              </svg>
            </div>
            <h3 class="v2-todos-delete-panel__title">{{ t('notebook.todos.delete') }}</h3>
            <p class="v2-todos-delete-panel__message">{{ t('notebook.todos.deleteConfirm') }}</p>
            <p class="v2-todos-delete-panel__content">「{{ deleteTarget.content }}」</p>
            <div class="v2-todos-delete-panel__actions">
              <button class="v2-todos-delete-panel__btn v2-todos-delete-panel__btn--cancel" @click="cancelDelete">
                {{ t('app.cancel') }}
              </button>
              <button
                class="v2-todos-delete-panel__btn v2-todos-delete-panel__btn--confirm"
                :disabled="deleting"
                @click="confirmDelete"
              >
                {{ deleting ? '...' : t('app.confirm') }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <div class="v2-todos-spacer" />
  </V2Page>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'

import V2Page from '@/mobile-v2/components/V2Page.vue'
import V2Card from '@/mobile-v2/components/V2Card.vue'
import type { NbTodoItem, NbTodoSaveRequest } from '@/api/notebook/todo'
import {
  createTodo as apiCreateTodo,
  fetchTodos,
  removeTodo as apiRemoveTodo,
  updateTodo as apiUpdateTodo,
} from '@/api/notebook/todo'
import { formatDateTime, isOverdue } from '@/views/notebook/todoGroup'

const { t } = useI18n()

const todos = ref<NbTodoItem[]>([])
const loading = ref(false)
const creating = ref(false)
const togglingId = ref<number | null>(null)

const activeFilter = ref<string>('all')
const collapsedGroups = reactive(new Set<string>(['done']))

const newContent = ref('')

const menuVisible = ref(false)
const menuItem = ref<NbTodoItem | null>(null)

const deleteTarget = ref<NbTodoItem | null>(null)
const deleting = ref(false)

const unpinTarget = ref<NbTodoItem | null>(null)
const unpinningId = ref<number | null>(null)

const RING_CIRCUMFERENCE = 2 * Math.PI * 22

const filterOptions = [
  { id: 'all', label: '全部' },
  { id: 'today', label: t('notebook.todos.filterToday') },
  { id: 'overdue', label: '逾期' },
  { id: 'upcoming', label: '未来' },
  { id: 'unscheduled', label: '未安排' },
  { id: 'done', label: t('notebook.todos.filterDone') },
]

const donePageSize = 20
const donePage = ref(1)

const hasMoreDone = computed(() => {
  const allDone = todos.value.filter(t => t.completed)
  return allDone.length > donePage.value * donePageSize
})

function loadMoreDone() {
  donePage.value++
}

// ---- 问候语 ----
const hour = new Date().getHours()
const greetingText = hour < 6 ? '夜深了' : hour < 9 ? '早上好' : hour < 12 ? '上午好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好'
const greetingEmoji = hour < 6 ? '🌙' : hour < 9 ? '☀️' : hour < 12 ? '☀️' : hour < 14 ? '🌤' : hour < 18 ? '🌇' : '🌆'

// ---- 统计 ----
const pinnedTodos = computed(() => todos.value.filter(t => t.pinned))
const pendingCount = computed(() => todos.value.filter(t => !t.completed).length)
const todayCount = computed(() => todos.value.filter(t => !t.completed && isTodayCheck(t)).length)
const overdueCount = computed(() => todos.value.filter(t => !t.completed && isOverdue(t)).length)
const doneCount = computed(() => todos.value.filter(t => t.completed).length)
const doneRatio = computed(() => todos.value.length ? Math.round((doneCount.value / todos.value.length) * 100) : 0)
const ringOffset = computed(() => RING_CIRCUMFERENCE - (RING_CIRCUMFERENCE * doneRatio.value) / 100)
const ringCircumference = computed(() => RING_CIRCUMFERENCE)

function isTodayCheck(item: NbTodoItem): boolean {
  return isToday(item.dueTime) || isToday(item.remindTime)
}

function isToday(value?: string | null): boolean {
  if (!value) return false
  const date = new Date(value.includes('T') ? value : value.replace(' ', 'T'))
  if (isNaN(date.getTime())) return false
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const end = new Date(start.getFullYear(), start.getMonth(), start.getDate(), 23, 59, 59, 999)
  return date >= start && date <= end
}

// ---- 排序 ----
function sortTodos(list: NbTodoItem[]): NbTodoItem[] {
  return list.sort((a, b) => {
    const pa = a.pinned ? 1 : 0
    const pb = b.pinned ? 1 : 0
    if (pa !== pb) return pb - pa
    const oa = isOverdue(a) ? 1 : 0
    const ob = isOverdue(b) ? 1 : 0
    if (oa !== ob) return ob - oa
    return (a.sortOrder ?? 0) - (b.sortOrder ?? 0)
  })
}

// ---- 加载 ----
async function loadTodos() {
  loading.value = true
  try {
    const data = await fetchTodos()
    todos.value = sortTodos(data)
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
    result = result.filter(g => g.key === 'today')
  } else if (activeFilter.value === 'overdue') {
    result = result.filter(g => g.key === 'overdue')
  } else if (activeFilter.value === 'upcoming') {
    result = result.filter(g => g.key === 'upcoming')
  } else if (activeFilter.value === 'unscheduled') {
    result = result.filter(g => g.key === 'unscheduled')
  } else if (activeFilter.value === 'done') {
    result = result.filter(g => g.key === 'done')
  }
  return result.map(g => {
    if (g.key === 'done') {
      return { ...g, items: g.items.slice(0, donePage.value * donePageSize) }
    }
    return g
  })
})

// ---- 过滤 ----
function setFilter(key: string) {
  activeFilter.value = key
  if (key === 'overdue' || key === 'today') {
    collapsedGroups.delete('overdue')
    collapsedGroups.delete('today')
  }
  if (key === 'done') {
    collapsedGroups.delete('done')
    donePage.value = 1
  }
  if (key === 'upcoming') {
    collapsedGroups.delete('upcoming')
  }
  if (key === 'unscheduled') {
    collapsedGroups.delete('unscheduled')
  }
}

// ---- 折叠 ----
function toggleGroup(key: string) {
  if (collapsedGroups.has(key)) {
    collapsedGroups.delete(key)
  } else {
    collapsedGroups.add(key)
  }
}

// ---- 分组展示 ----
function groupIconColor(key: string): string {
  const map: Record<string, string> = {
    overdue: '#ef4444',
    today: '#f59e0b',
    upcoming: '#22c55e',
    unscheduled: '#94a3b8',
    done: '#16a34a',
  }
  return map[key] || '#2563eb'
}

function groupSvg(key: string): string {
  const icons: Record<string, string> = {
    overdue: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
    today: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/><circle cx="12" cy="15" r="1"/></svg>',
    upcoming: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
    unscheduled: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/></svg>',
    done: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
  }
  return icons[key] || ''
}

function groupTitle(key: string): string {
  const map: Record<string, string> = {
    overdue: '已逾期',
    today: '今日待办',
    upcoming: '未来安排',
    unscheduled: '未安排',
    done: '已完成',
  }
  return map[key] || key
}

// ---- 工具 ----
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

// ---- 操作 ----
async function toggleComplete(item: NbTodoItem) {
  togglingId.value = item.id
  try {
    const body: NbTodoSaveRequest = { completed: !item.completed }
    const result = await apiUpdateTodo(item.id, body)
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

// ---- 编辑 ----
const editVisible = ref(false)
const editForm = reactive({
  content: '',
  dueTime: '',
  remindTime: '',
  repeatType: 'NONE',
})

const repeatTypeOptions = [
  { value: 'NONE', label: '不重复' },
  { value: 'DAILY', label: '每天' },
  { value: 'WEEKLY', label: '每周' },
  { value: 'MONTHLY', label: '每月' },
  { value: 'YEARLY', label: '每年' },
]

function toDatetimeLocal(value?: string | null): string {
  if (!value) return ''
  const d = new Date(value)
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function openEditDialog() {
  if (!menuItem.value) return
  if (menuItem.value.completed) return
  menuVisible.value = false
  editForm.content = menuItem.value.content
  editForm.dueTime = toDatetimeLocal(menuItem.value.dueTime)
  editForm.remindTime = toDatetimeLocal(menuItem.value.remindTime)
  editForm.repeatType = menuItem.value.repeatType ?? 'NONE'
  editVisible.value = true
}

async function saveEdit() {
  if (!menuItem.value) return
  if (menuItem.value.completed) return
  if (!editForm.content.trim()) {
    ElMessage.warning(t('notebook.todos.contentRequired'))
    return
  }
  const body: NbTodoSaveRequest = {
    content: editForm.content.trim(),
    dueTime: editForm.dueTime ? editForm.dueTime + ':00' : null,
    clearDueTime: !editForm.dueTime,
    remindTime: editForm.remindTime ? editForm.remindTime + ':00' : null,
    clearRemindTime: !editForm.remindTime,
    repeatType: editForm.repeatType === 'NONE' ? 'NONE' : editForm.repeatType,
  }
  try {
    const result = await apiUpdateTodo(menuItem.value.id, body)
    Object.assign(menuItem.value, result.item)
    ElMessage.success('已修改')
    editVisible.value = false
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  }
}

// ---- 菜单 ----
function openMenu(item: NbTodoItem) {
  menuItem.value = item
  menuVisible.value = true
}

function closeMenu() {
  menuVisible.value = false
  setTimeout(() => { menuItem.value = null }, 200)
}

async function togglePin() {
  if (!menuItem.value) return
  const item = menuItem.value
  menuVisible.value = false
  try {
    const result = await apiUpdateTodo(item.id, { pinned: !item.pinned })
    item.pinned = result.item.pinned
    sortTodos(todos.value)
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  }
}

function confirmUnpin(item: NbTodoItem) {
  unpinTarget.value = item
}

function cancelUnpin() {
  unpinTarget.value = null
  unpinningId.value = null
}

async function doUnpin() {
  if (!unpinTarget.value) return
  const item = unpinTarget.value
  unpinningId.value = item.id
  try {
    await apiUpdateTodo(item.id, { pinned: false })
    item.pinned = 0
    todos.value = todos.value.filter(t => t.id !== item.id)
    unpinTarget.value = null
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  } finally {
    unpinningId.value = null
  }
}

function openDeleteDialog() {
  if (!menuItem.value) return
  deleteTarget.value = menuItem.value
  menuVisible.value = false
}

function cancelDelete() {
  deleteTarget.value = null
}

async function confirmDelete() {
  if (!deleteTarget.value || deleting.value) return
  deleting.value = true
  try {
    await apiRemoveTodo(deleteTarget.value.id)
    todos.value = todos.value.filter(t => t.id !== deleteTarget.value!.id)
    ElMessage.success('已删除')
    deleteTarget.value = null
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  void loadTodos()
})
</script>

<style scoped lang="scss">
// ---- 问候语 ----
.v2-todos-greeting {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;

  &__left {
    flex: 1;
  }

  &__title {
    font-size: 20px;
    font-weight: 700;
    color: var(--wr-text, #333333);
    line-height: 1.3;
    margin-bottom: 2px;
  }

  &__sub {
    font-size: 13px;
    color: var(--wr-text-secondary, #666666);
    margin: 0;

    strong {
      color: var(--wr-stat-blue, #2563eb);
      font-weight: 700;
    }
  }

  &__refresh {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    border: 1.5px solid var(--wr-border, #e8ecef);
    background: var(--wr-card, #ffffff);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    flex-shrink: 0;
    color: var(--wr-text-secondary, #666666);
    transition: all 0.15s;

    &:hover {
      border-color: var(--wr-stat-blue, #2563eb);
      color: var(--wr-stat-blue, #2563eb);
    }

    &:active {
      opacity: 0.75;
    }

    &.loading svg {
      animation: v2-todos-spin 0.8s linear infinite;
    }
  }
}

@keyframes v2-todos-spin {
  100% { transform: rotate(360deg); }
}

// ---- 统计卡片 ----
.v2-todos-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 4px 0;
}

.v2-todos-ring {
  position: relative;
  width: 56px;
  height: 56px;
  flex-shrink: 0;

  &__svg {
    width: 100%;
    height: 100%;
  }

  &__text {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 800;
    color: var(--wr-text, #333333);
    font-variant-numeric: tabular-nums;
  }
}

.v2-todos-stat-group {
  display: flex;
  align-items: center;
  gap: 0;
  flex: 1;
}

.v2-todos-stat {
  text-align: center;
  flex: 1;

  &__value {
    font-size: 20px;
    font-weight: 800;
    display: block;
    line-height: 1.3;
    font-variant-numeric: tabular-nums;

    &--blue { color: var(--wr-stat-blue, #2563eb); }
    &--orange { color: #f59e0b; }
    &--red { color: #ef4444; }
  }

  &__label {
    font-size: 11px;
    color: var(--wr-text-secondary, #666666);
    margin-top: 1px;
    display: block;
  }
}

.v2-todos-stat-divider {
  width: 1px;
  height: 28px;
  background: var(--wr-border, #e8ecef);
  flex-shrink: 0;
}

// ---- 置顶 ----
.v2-todos-pinned {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed var(--wr-border, #e8ecef);

  &__header {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    margin-bottom: 12px;
  }

  &__count {
    font-size: 12px;
    font-weight: 400;
    color: var(--wr-text-secondary, #666666);
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  &__item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 8px 12px;
    background: #fffbeb;
    border: 1px solid #fde68a;
    border-radius: 8px;
    cursor: default;

    &:active {
      opacity: 0.85;
    }
  }

  &__content {
    font-size: 13px;
    font-weight: 600;
    color: #1e293b;
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 4px;
    flex-shrink: 0;
  }

  &__time {
    font-size: 11px;
    color: #d97706;
    font-weight: 600;
  }

  &__unpin {
    flex-shrink: 0;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    border: 1.5px solid #fde68a;
    background: #fffbeb;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    padding: 0;
    color: #d97706;
    transition: all 0.15s;

    &:hover {
      border-color: #d97706;
      background: #fef3c7;
    }

    &:active {
      transform: scale(0.9);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  &__unpin-spinner {
    width: 12px;
    height: 12px;
    border: 2px solid #fde68a;
    border-top-color: #d97706;
    border-radius: 50%;
    animation: v2-todos-spin 0.6s linear infinite;
    display: block;
  }
}

// ---- 筛选标签 ----
.v2-todos-filters {
  display: flex;
  gap: 6px;
  padding: 0 4px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.v2-todos-filter-btn {
  flex-shrink: 0;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1.5px solid var(--wr-border, #e8ecef);
  background: var(--wr-card, #ffffff);
  font-size: 13px;
  font-weight: 500;
  color: var(--wr-text-secondary, #666666);
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  font-family: inherit;

  &:hover {
    border-color: var(--wr-stat-blue, #2563eb);
    color: var(--wr-stat-blue, #2563eb);
  }

  &.is-active {
    border-color: var(--wr-stat-blue, #2563eb);
    background: #eff6ff;
    color: var(--wr-stat-blue, #2563eb);
    font-weight: 700;
  }
}

// ---- 状态 ----
.v2-todos-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 48px 0;
  font-size: 14px;
  color: var(--wr-text-secondary, #666666);

  &__spinner {
    width: 18px;
    height: 18px;
    border: 2px solid var(--wr-border, #e8ecef);
    border-top-color: var(--wr-stat-blue, #2563eb);
    border-radius: 50%;
    animation: v2-todos-spin 0.6s linear infinite;
  }
}

// ---- 空状态 ----
.v2-todos-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  text-align: center;

  svg {
    margin-bottom: 12px;
  }

  &__title {
    font-size: 15px;
    font-weight: 600;
    color: var(--wr-text, #333333);
    margin-bottom: 6px;
  }

  &__sub {
    font-size: 13px;
    color: var(--wr-text-secondary, #666666);
    margin: 0;
  }
}

// ---- 分组列表 ----
.v2-todos-groups {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.v2-todos-group {
  background: var(--wr-card, #ffffff);
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 12px;
  overflow: hidden;

  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    padding: 12px 14px;
    border: none;
    background: transparent;
    cursor: pointer;
    font: inherit;
    text-align: left;
    transition: background 0.15s;

    &:hover {
      background: var(--wr-index-bg, #f8fafc);
    }

    &:active {
      background: #f1f5f9;
    }
  }

  &__icon {
    display: flex;
    align-items: center;
    flex-shrink: 0;
  }

  &__title {
    font-size: 14px;
    font-weight: 700;
    color: var(--wr-text, #333333);
  }

  &__count {
    margin-left: auto;
    font-size: 11px;
    font-weight: 600;
    color: var(--wr-text-secondary, #666666);
    background: rgba(0, 0, 0, 0.05);
    padding: 1px 8px;
    border-radius: 999px;
    line-height: 18px;
  }

  &__arrow {
    flex-shrink: 0;
    color: var(--wr-text-secondary, #666666);
    transition: transform 0.2s ease;
    margin-left: 2px;

    &.expanded {
      transform: rotate(90deg);
    }
  }

  &__body {
    padding: 0 14px 8px;
  }
}

// ---- 待办项 ----
.v2-todos-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--wr-border, #e8ecef);

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }

  &--done {
    .v2-todos-item__text {
      text-decoration: line-through;
      color: var(--wr-muted, #999999);
    }
  }

  &__check {
    width: 22px;
    height: 22px;
    flex-shrink: 0;
    border-radius: 50%;
    border: 2px solid var(--wr-border, #d1d5db);
    background: var(--wr-card, #ffffff);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    padding: 0;
    margin-top: 1px;
    transition: all 0.2s ease;
    color: #fff;

    &:hover {
      border-color: var(--wr-stat-blue, #2563eb);
      box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    &--checked {
      background: #22c55e;
      border-color: #22c55e;

      &:hover {
        border-color: #16a34a;
        box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.15);
      }
    }
  }

  &__check-spinner {
    width: 12px;
    height: 12px;
    border: 2px solid var(--wr-border, #e8ecef);
    border-top-color: var(--wr-stat-blue, #2563eb);
    border-radius: 50%;
    animation: v2-todos-spin 0.6s linear infinite;
    display: block;
  }

  &__body {
    flex: 1;
    min-width: 0;
    cursor: pointer;
  }

  &__text {
    font-size: 14px;
    font-weight: 500;
    color: var(--wr-text, #333333);
    line-height: 1.4;
    display: block;
  }

  &__meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
    margin-top: 4px;
    min-height: 17px;
  }

  &__time {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    font-size: 11px;
    font-weight: 500;
    color: var(--wr-text-secondary, #666666);

    svg {
      flex-shrink: 0;
    }

    &--overdue {
      color: #ef4444;
      font-weight: 600;
    }
  }
}

.v2-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  line-height: 1.3;

  &--remind {
    background: #fce7f3;
    color: #db2777;
  }

  &--repeat {
    background: #dbeafe;
    color: #2563eb;
  }
}

.v2-todos-load-more {
  display: block;
  width: 100%;
  padding: 10px;
  margin-top: 4px;
  border: 1.5px dashed var(--wr-border, #e8ecef);
  border-radius: 8px;
  background: transparent;
  color: var(--wr-text-secondary, #666666);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  text-align: center;
  transition: all 0.15s;
  font-family: inherit;

  &:hover {
    background: var(--wr-index-bg, #f8fafc);
    color: var(--wr-text, #333333);
    border-color: var(--wr-text-secondary, #666666);
  }

  &:active {
    background: #f1f5f9;
  }
}

// ---- 底部添加栏 ----
.v2-todos-bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 8px;
  padding: 8px 16px;
  padding-bottom: calc(8px + env(safe-area-inset-bottom, 0px));
  background: var(--wr-card, #ffffff);
  border-top: 1.5px solid var(--wr-border, #e8ecef);
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
  z-index: 10;
  box-sizing: border-box;
}

.v2-todos-bottom-bar__input {
  flex: 1;
  height: 40px;
  padding: 0 14px;
  border: 1.5px solid var(--wr-border, #e8ecef);
  border-radius: 10px;
  outline: none;
  font-size: 14px;
  color: var(--wr-text, #333333);
  transition: border-color 0.15s;
  box-sizing: border-box;
  font-family: inherit;

  &:focus {
    border-color: var(--wr-stat-blue, #2563eb);
  }

  &::placeholder {
    color: var(--wr-muted, #999999);
  }
}

.v2-todos-bottom-bar__btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 40px;
  padding: 0 18px;
  border: none;
  background: var(--wr-stat-blue, #2563eb);
  color: #fff;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
  flex-shrink: 0;
  font-family: inherit;

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  &:not(:disabled):hover {
    opacity: 0.9;
  }

  &:not(:disabled):active {
    opacity: 0.8;
  }
}

// ---- 底部操作菜单 ----
.v2-todos-sheet-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(15, 23, 42, 0.35);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.v2-todos-sheet {
  width: 100%;
  max-width: 400px;
  background: var(--wr-card, #ffffff);
  border-radius: 16px 16px 0 0;
  padding: 20px 16px calc(16px + env(safe-area-inset-bottom, 0px));
  animation: v2-todos-sheet-up 0.25s ease;

  &__title {
    font-size: 15px;
    font-weight: 700;
    color: var(--wr-text, #333333);
    margin-bottom: 16px;
    text-align: center;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__divider {
    height: 1px;
    background: var(--wr-border, #e8ecef);
    margin: 6px 0;
  }

  &__action {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 14px 8px;
    border: none;
    background: transparent;
    font-size: 15px;
    font-weight: 500;
    color: var(--wr-text, #333333);
    cursor: pointer;
    text-align: left;
    border-radius: 10px;
    transition: background 0.15s;
    font-family: inherit;

    &:hover {
      background: var(--wr-index-bg, #f8fafc);
    }

    &:active {
      background: #f1f5f9;
    }

    &--danger {
      color: #ef4444;
    }

    &--cancel {
      justify-content: center;
      font-weight: 700;
      color: var(--wr-text-secondary, #666666);
    }
  }
}

@keyframes v2-todos-sheet-up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.v2-todos-sheet-enter-active,
.v2-todos-sheet-leave-active {
  transition: opacity 0.2s ease;

  .v2-todos-sheet {
    transition: transform 0.25s ease;
  }
}

.v2-todos-sheet-enter-from,
.v2-todos-sheet-leave-to {
  opacity: 0;

  .v2-todos-sheet {
    transform: translateY(100%);
  }
}

// ---- 编辑对话框 ----
:deep(.v2-todos-edit-dialog) {
  .el-dialog {
    border-radius: 16px;
    overflow: hidden;
  }

  .el-dialog__header {
    padding: 18px 20px 0;
    margin-right: 0;

    .el-dialog__title {
      font-size: 17px;
      font-weight: 700;
      color: var(--wr-text, #333333);
    }
  }

  .el-dialog__body {
    padding: 16px 20px;
    overflow: hidden;
  }

  .el-dialog__footer {
    padding: 0 20px 18px;
  }
}

.v2-todos-edit-form {
  display: flex;
  flex-direction: column;
  gap: 14px;

  &__label {
    font-size: 13px;
    font-weight: 700;
    color: var(--wr-text, #333333);
  }

  &__input {
    width: 100%;
    padding: 10px 12px;
    border: 1.5px solid var(--wr-border, #e8ecef);
    border-radius: 10px;
    font-size: 14px;
    color: var(--wr-text, #333333);
    outline: none;
    box-sizing: border-box;
    font-family: inherit;
    transition: border-color 0.15s;

    &:focus {
      border-color: var(--wr-stat-blue, #2563eb);
    }

    &::placeholder {
      color: var(--wr-muted, #999999);
    }
  }

  &__repeat-group {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__repeat-btn {
    padding: 6px 14px;
    border: 1.5px solid var(--wr-border, #e8ecef);
    border-radius: 999px;
    background: var(--wr-card, #ffffff);
    font-size: 13px;
    font-weight: 500;
    color: var(--wr-text-secondary, #666666);
    cursor: pointer;
    transition: all 0.15s;
    font-family: inherit;

    &:hover {
      border-color: var(--wr-stat-blue, #2563eb);
      color: var(--wr-stat-blue, #2563eb);
    }

    &.active {
      border-color: var(--wr-stat-blue, #2563eb);
      background: #eff6ff;
      color: var(--wr-stat-blue, #2563eb);
      font-weight: 700;
    }
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }

  &__cancel {
    padding: 8px 20px;
    border: 1.5px solid var(--wr-border, #e8ecef);
    border-radius: 10px;
    background: var(--wr-card, #ffffff);
    color: var(--wr-text-secondary, #666666);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.15s;
    font-family: inherit;

    &:hover {
      border-color: var(--wr-text-secondary, #666666);
    }
  }

  &__confirm {
    padding: 8px 24px;
    border: none;
    border-radius: 10px;
    background: var(--wr-stat-blue, #2563eb);
    color: #fff;
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
    transition: opacity 0.15s;
    font-family: inherit;

    &:hover {
      opacity: 0.9;
    }

    &:active {
      opacity: 0.8;
    }
  }
}

// ---- 删除确认 ----
.v2-todos-delete-overlay {
  position: fixed;
  inset: 0;
  z-index: 210;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 20px;
  background: rgba(15, 23, 42, 0.45);
}

.v2-todos-delete-panel {
  width: min(100%, 320px);
  background: var(--wr-card, #ffffff);
  border-radius: 16px;
  padding: 28px 24px 20px;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);

  &__icon {
    margin-bottom: 12px;
  }

  &__title {
    margin: 0 0 8px;
    font-size: 18px;
    font-weight: 700;
    color: var(--wr-text, #333333);
  }

  &__message {
    margin: 0 0 4px;
    font-size: 14px;
    color: var(--wr-text-secondary, #666666);
    line-height: 1.5;
  }

  &__content {
    margin: 0 0 20px;
    font-size: 15px;
    font-weight: 600;
    color: var(--wr-text, #333333);
    word-break: break-all;
    line-height: 1.4;
  }

  &__actions {
    display: flex;
    gap: 10px;
  }

  &__btn {
    flex: 1;
    padding: 11px 0;
    border: none;
    border-radius: 10px;
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
    transition: opacity 0.15s;
    font-family: inherit;

    &:active {
      opacity: 0.8;
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    &--cancel {
      background: var(--wr-index-bg, #f1f5f9);
      color: var(--wr-text-secondary, #666666);
    }

    &--confirm {
      background: #ef4444;
      color: #fff;
    }
  }
}

.v2-todos-delete-enter-active,
.v2-todos-delete-leave-active {
  transition: opacity 0.2s ease;

  .v2-todos-delete-panel {
    transition: transform 0.22s ease, opacity 0.22s ease;
  }
}

.v2-todos-delete-enter-from,
.v2-todos-delete-leave-to {
  opacity: 0;

  .v2-todos-delete-panel {
    transform: scale(0.92);
    opacity: 0;
  }
}

.v2-todos-spacer {
  height: 72px;
  flex-shrink: 0;
}
</style>
