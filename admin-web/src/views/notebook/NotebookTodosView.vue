<template>
  <div class="notebook-todos">
    <section class="notebook-todos__stats">
      <button
        v-for="card in statCards"
        :key="card.key"
        type="button"
        class="notebook-todos__stat-card"
        @click="card.onClick?.()"
      >
        <span class="notebook-todos__stat-icon" :class="`notebook-todos__stat-icon--${card.tone}`">
          <el-icon><component :is="card.icon" /></el-icon>
        </span>
        <div class="notebook-todos__stat-body">
          <span class="notebook-todos__stat-label">{{ card.label }}</span>
          <span class="notebook-todos__stat-value" :style="{ color: card.color }">{{ card.value }}</span>
        </div>
      </button>
    </section>

    <div class="notebook-todos__toolbar">
      <div class="notebook-todos__filters">
        <button
          v-for="item in filterOptions"
          :key="item.value"
          type="button"
          class="notebook-todos__filter"
          :class="{ 'is-active': filter === item.value }"
          @click="setFilter(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
      <div class="notebook-todos__toolbar-actions">
        <el-button
          type="primary"
          class="notebook-todos__create-btn"
          :icon="Plus"
          @click="openCreateDialog"
        >
          {{ t('notebook.todos.create') }}
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="notebook-todos__board">
      <section
        v-for="column in kanbanColumns"
        :key="column.key"
        class="notebook-todos__column"
        :class="`is-${column.key}`"
      >
        <header class="notebook-todos__column-header">
          <span class="notebook-todos__column-title">{{ columnTitle(column.key) }}</span>
          <span class="notebook-todos__column-count">{{ column.items.length }}</span>
        </header>

        <div
          :ref="(el) => setColumnBodyRef(column.key, el as HTMLElement | null)"
          class="notebook-todos__column-body"
        >
          <article
            v-for="row in column.items"
            :key="row.id"
            class="notebook-todos__card"
            :class="{ 'is-done': row.completed === 1 }"
            :data-id="row.id"
          >
            <div class="notebook-todos__card-lead">
              <button
                type="button"
                class="notebook-todos__card-drag"
                :title="t('notebook.todos.dragHandle')"
                @click.stop
              >
                <span class="notebook-todos__card-drag-dots" aria-hidden="true">
                  <span v-for="dot in 6" :key="dot" class="notebook-todos__card-drag-dot" />
                </span>
              </button>
              <el-checkbox
                v-if="column.key !== 'done'"
                class="notebook-todos__card-check"
                :model-value="row.completed === 1"
                @click.stop
                @change="(checked: boolean) => onToggle(row, checked)"
              />
            </div>
            <div class="notebook-todos__card-content">
              <h4 class="notebook-todos__card-title" @click="openEditDialog(row)">{{ row.content }}</h4>
              <div v-if="cardMeta(row).length" class="notebook-todos__card-meta" @click="openEditDialog(row)">
                <span
                  v-for="(meta, index) in cardMeta(row)"
                  :key="index"
                  class="notebook-todos__card-meta-line"
                  :class="meta.className"
                >
                  <el-icon class="notebook-todos__card-meta-icon">
                    <Calendar />
                  </el-icon>
                  <span>{{ meta.text }}</span>
                </span>
              </div>
              <el-tag
                v-if="repeatLabel(row)"
                size="small"
                type="info"
                class="notebook-todos__repeat-tag"
                @click="openEditDialog(row)"
              >
                {{ repeatLabel(row) }}
              </el-tag>
            </div>
            <div class="notebook-todos__card-actions">
              <button
                v-if="row.completed !== 1"
                type="button"
                class="notebook-todos__card-star"
                :class="{ 'is-active': row.pinned === 1 }"
                :title="row.pinned === 1 ? t('notebook.todos.unpin') : t('notebook.todos.pin')"
                @click.stop="onTogglePin(row)"
              >
                <el-icon>
                  <StarFilled v-if="row.pinned === 1" />
                  <Star v-else />
                </el-icon>
              </button>
              <el-dropdown trigger="click" @command="(cmd: string) => onCardMenu(cmd, row)">
                <button
                  type="button"
                  class="notebook-todos__card-more"
                  :title="t('notebook.todos.moreActions')"
                  @click.stop
                >
                  <el-icon><MoreFilled /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">
                      <span class="notebook-todos__menu-item">
                        <el-icon><Edit /></el-icon>
                        <span>{{ t('notebook.todos.edit') }}</span>
                      </span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="column.key === 'done'" command="revert">
                      <span class="notebook-todos__menu-item">
                        <el-icon><RefreshLeft /></el-icon>
                        <span>{{ t('notebook.todos.revertTask') }}</span>
                      </span>
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <span class="notebook-todos__menu-item is-danger">
                        <el-icon><Delete /></el-icon>
                        <span>{{ t('notebook.todos.delete') }}</span>
                      </span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </article>

          <div v-if="!column.items.length" class="notebook-todos__column-empty">
            {{ t('notebook.todos.kanban.columnEmpty') }}
          </div>
        </div>
      </section>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? t('notebook.todos.createTitle') : t('notebook.todos.editTitle')"
      width="560px"
      destroy-on-close
    >
      <el-form label-width="96px" @submit.prevent>
        <el-form-item :label="t('notebook.todos.content')" required>
          <el-input v-model="form.content" :placeholder="t('notebook.todos.addPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('notebook.todos.dueTime')">
          <el-time-picker
            v-if="isRecurringForm"
            v-model="form.dueTimeOnly"
            value-format="HH:mm:ss"
            :placeholder="t('notebook.todos.dueTimeOnlyPlaceholder')"
            clearable
            style="width: 100%"
          />
          <el-date-picker
            v-else
            v-model="form.dueTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            :placeholder="t('notebook.todos.dueTimePlaceholder')"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('notebook.todos.remindTime')">
          <el-time-picker
            v-if="isRecurringForm"
            v-model="form.remindTimeOnly"
            value-format="HH:mm:ss"
            :placeholder="t('notebook.todos.remindTimeOnlyPlaceholder')"
            clearable
            style="width: 100%"
          />
          <el-date-picker
            v-else
            v-model="form.remindTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            :placeholder="t('notebook.todos.remindTimePlaceholder')"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('notebook.todos.repeatLabel')">
          <div class="notebook-todos__repeat-form">
            <el-select v-model="form.repeatType" style="width: 160px">
              <el-option
                v-for="type in TODO_REPEAT_TYPES"
                :key="type"
                :label="t(`notebook.todos.repeatOptions.${type}`)"
                :value="type"
              />
            </el-select>
            <template v-if="form.repeatType !== 'NONE'">
              <span class="notebook-todos__repeat-gap">{{ t('notebook.todos.repeatEvery') }}</span>
              <el-input-number v-model="form.repeatInterval" :min="1" :max="365" />
              <span class="notebook-todos__repeat-gap">{{ repeatUnitLabel }}</span>
            </template>
          </div>
        </el-form-item>
        <el-form-item v-if="form.repeatType === 'WEEKLY'" :label="t('notebook.todos.repeatOnWeekdays')">
          <el-checkbox-group v-model="form.repeatWeekdays" class="notebook-todos__weekday-group">
            <el-checkbox v-for="day in WEEKDAY_VALUES" :key="day" :label="day">
              {{ t(`notebook.todos.weekdays.${weekdayI18nKey(day)}`) }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item v-if="form.repeatType === 'MONTHLY'" :label="t('notebook.todos.repeatOnMonthDays')">
          <el-select
            v-model="form.repeatMonthDays"
            multiple
            collapse-tags
            collapse-tags-tooltip
            :placeholder="t('notebook.todos.repeatDaysRequired')"
            style="width: 100%"
          >
            <el-option
              v-for="day in MONTH_DAY_VALUES"
              :key="day"
              :label="t('notebook.todos.repeatMonthDayLabel', { day })"
              :value="day"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.repeatType === 'YEARLY'" :label="t('notebook.todos.repeatOnYearDays')">
          <div class="notebook-todos__year-days">
            <el-tag
              v-for="day in form.repeatYearDays"
              :key="day"
              closable
              class="notebook-todos__year-day-tag"
              @close="removeYearDay(day)"
            >
              {{ formatYearDayLabel(day) }}
            </el-tag>
            <el-date-picker
              v-model="yearDayDraft"
              type="date"
              format="M月D日"
              value-format="MM-DD"
              :placeholder="t('notebook.todos.addYearDay')"
              :clearable="false"
              style="width: 168px"
              @change="onYearDayPicked"
            />
          </div>
        </el-form-item>
        <el-form-item v-if="form.repeatType !== 'NONE'" :label="t('notebook.todos.repeatUntil')">
          <el-date-picker
            v-model="form.repeatUntil"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            :placeholder="t('notebook.todos.repeatUntilPlaceholder')"
            clearable
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('pomodoro.common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDialog">
          {{ t('pomodoro.common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import Sortable from 'sortablejs'
import { Delete, Document, Edit, List, CollectionTag, MoreFilled, Plus, RefreshLeft, Star, StarFilled, Warning, Calendar } from '@element-plus/icons-vue'
import {
  TODO_REPEAT_TYPES,
  createTodo,
  fetchTodayTodos,
  fetchTodos,
  removeTodo,
  updateTodo,
  type NbTodoFilter,
  type NbTodoItem,
  type NbTodoRepeatType,
} from '@/api/notebook/todo'
import {
  buildColumnSortOrders,
  formatTodoScheduleLabel,
  isOverdue,
  isRemindSoon,
  isToday,
  splitTodosForKanban,
  type KanbanColumnKey,
  type TodoCardMetaItem,
} from './todoGroup'
import { useTodoReminders, dismissTodoNotification } from '@/composables/useTodoReminders'
import {
  MONTH_DAY_VALUES,
  WEEKDAY_VALUES,
  computeInitialDueDate,
  decodeRepeatDays,
  encodeRepeatDays,
  extractDatePart,
  extractTimePart,
  formatRepeatDaysLabel,
  mergeDateAndTime,
} from './todoRepeat'

import type { Component } from 'vue'

interface TodoStatCard {
  key: string
  label: string
  value: number
  color: string
  tone: 'blue' | 'green' | 'purple' | 'orange'
  icon: Component
  onClick?: () => void
}

const props = defineProps<{
  initialFilter?: NbTodoFilter
}>()

const { t, locale } = useI18n()
const { refreshTodayCount } = useTodoReminders()

const filter = ref<NbTodoFilter>(props.initialFilter ?? 'all')
const loading = ref(false)
const submitting = ref(false)
const items = ref<NbTodoItem[]>([])
const statToday = ref(0)
const statPending = ref(0)
const statDone = ref(0)
const statOverdue = ref(0)

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const dueDateAnchor = ref<string | null>(null)
const yearDayDraft = ref<string | null>(null)
const form = reactive({
  content: '',
  dueTime: null as string | null,
  remindTime: null as string | null,
  dueTimeOnly: null as string | null,
  remindTimeOnly: null as string | null,
  repeatType: 'NONE' as NbTodoRepeatType,
  repeatInterval: 1,
  repeatUntil: null as string | null,
  repeatWeekdays: [] as number[],
  repeatMonthDays: [] as number[],
  repeatYearDays: [] as string[],
})

const filterOptions = computed(() => [
  { value: 'all' as const, label: t('notebook.todos.filterAll') },
  { value: 'pending' as const, label: t('notebook.todos.filterPending') },
  { value: 'done' as const, label: t('notebook.todos.filterDone') },
])

const kanbanColumns = computed(() => splitTodosForKanban(items.value))

const columnBodyRefs = new Map<KanbanColumnKey, HTMLElement>()
const sortableInstances: Sortable[] = []
const reordering = ref(false)

function setColumnBodyRef(key: KanbanColumnKey, el: HTMLElement | null) {
  if (el) {
    columnBodyRefs.set(key, el)
    return
  }
  columnBodyRefs.delete(key)
}

function destroySortables() {
  for (const instance of sortableInstances) {
    instance.destroy()
  }
  sortableInstances.length = 0
}

function setupSortables() {
  destroySortables()
  void nextTick(() => {
    for (const column of kanbanColumns.value) {
      const el = columnBodyRefs.get(column.key)
      if (!el || !column.items.length) continue
      sortableInstances.push(
        Sortable.create(el, {
          animation: 160,
          handle: '.notebook-todos__card-drag',
          draggable: '.notebook-todos__card',
          ghostClass: 'notebook-todos__card--ghost',
          chosenClass: 'notebook-todos__card--chosen',
          disabled: reordering.value,
          onEnd: (evt) => {
            void onColumnReorder(column.key, evt.oldIndex, evt.newIndex)
          },
        }),
      )
    }
  })
}

async function onColumnReorder(columnKey: KanbanColumnKey, oldIndex?: number, newIndex?: number) {
  if (oldIndex === undefined || newIndex === undefined || oldIndex === newIndex) {
    return
  }

  const column = kanbanColumns.value.find((entry) => entry.key === columnKey)
  if (!column) return

  const orderedItems = [...column.items]
  const [moved] = orderedItems.splice(oldIndex, 1)
  if (!moved) return
  orderedItems.splice(newIndex, 0, moved)

  const sortOrders = buildColumnSortOrders(
    columnKey,
    orderedItems.map((item) => item.id),
  )
  const previous = new Map<number, number>()
  for (const item of items.value) {
    previous.set(item.id, item.sortOrder ?? 0)
  }

  const updates = orderedItems
    .map((item) => ({
      id: item.id,
      sortOrder: sortOrders.get(item.id) ?? item.sortOrder ?? 0,
    }))
    .filter((entry) => previous.get(entry.id) !== entry.sortOrder)

  if (!updates.length) {
    return
  }

  items.value = items.value.map((item) => {
    const sortOrder = sortOrders.get(item.id)
    return sortOrder === undefined ? item : { ...item, sortOrder }
  })

  reordering.value = true
  try {
    await Promise.all(updates.map((entry) => updateTodo(entry.id, { sortOrder: entry.sortOrder })))
  } catch {
    await loadTodos()
    ElMessage.error(t('notebook.todos.reorderFailed'))
  } finally {
    reordering.value = false
    setupSortables()
  }
}

watch(
  () => form.repeatType,
  (type) => {
    if (type !== 'WEEKLY') form.repeatWeekdays = []
    if (type !== 'MONTHLY') form.repeatMonthDays = []
    if (type !== 'YEARLY') {
      form.repeatYearDays = []
      yearDayDraft.value = null
    }
    if (type === 'NONE') {
      form.dueTimeOnly = null
      form.remindTimeOnly = null
      dueDateAnchor.value = null
    }
  },
)

watch(
  () => kanbanColumns.value.map((column) => `${column.key}:${column.items.map((item) => item.id).join(',')}`).join('|'),
  () => {
    if (!reordering.value) {
      setupSortables()
    }
  },
)

const statCards = computed<TodoStatCard[]>(() => [
  {
    key: 'today',
    label: t('notebook.todos.stats.today'),
    value: statToday.value,
    color: 'var(--wr-stat-blue)',
    tone: 'blue',
    icon: Document,
    onClick: () => setFilter('all'),
  },
  {
    key: 'pending',
    label: t('notebook.todos.stats.pending'),
    value: statPending.value,
    color: 'var(--wr-stat-green)',
    tone: 'green',
    icon: List,
    onClick: () => setFilter('pending'),
  },
  {
    key: 'done',
    label: t('notebook.todos.stats.done'),
    value: statDone.value,
    color: 'var(--wr-stat-purple)',
    tone: 'purple',
    icon: CollectionTag,
    onClick: () => setFilter('done'),
  },
  {
    key: 'overdue',
    label: t('notebook.todos.stats.overdue'),
    value: statOverdue.value,
    color: 'var(--wr-stat-orange)',
    tone: 'orange',
    icon: Warning,
    onClick: () => setFilter('all'),
  },
])

const repeatUnitLabel = computed(() => {
  if (form.repeatType === 'NONE') return ''
  return t(`notebook.todos.repeatUnit.${form.repeatType}`)
})

const isRecurringForm = computed(() => form.repeatType !== 'NONE')

const WEEKDAY_I18N_KEYS = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'] as const

function weekdayI18nKey(day: number) {
  return WEEKDAY_I18N_KEYS[day - 1] ?? 'mon'
}

function formatYearDayLabel(token: string) {
  const [monthText, dayText] = token.split('-')
  return t('notebook.todos.repeatYearDayLabel', {
    month: Number.parseInt(monthText, 10),
    day: Number.parseInt(dayText, 10),
  })
}

function removeYearDay(token: string) {
  form.repeatYearDays = form.repeatYearDays.filter((day) => day !== token)
}

function onYearDayPicked(value: string | null) {
  if (!value || form.repeatYearDays.includes(value)) {
    yearDayDraft.value = null
    return
  }
  form.repeatYearDays = [...form.repeatYearDays, value].sort()
  yearDayDraft.value = null
}

function columnTitle(key: KanbanColumnKey) {
  return t(`notebook.todos.kanban.${key}`)
}

function repeatLabel(row: NbTodoItem): string {
  const type = row.repeatType ?? 'NONE'
  if (type === 'NONE') return ''
  const interval = row.repeatInterval ?? 1
  const base = t(`notebook.todos.repeatTag.${type}`, { n: interval })
  const days = formatRepeatDaysLabel(type as NbTodoRepeatType, row.repeatDays ?? undefined, t)
  return days ? `${base} · ${days}` : base
}

const WEEKDAY_KEYS = ['sun', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat'] as const

function weekdayLabel(dayIndex: number) {
  return t(`notebook.todos.weekdays.${WEEKDAY_KEYS[dayIndex]}`)
}

function scheduleLabel(value: string, suffixKey: 'dueSuffix' | 'completedSuffix' | 'remindSuffix') {
  return formatTodoScheduleLabel(value, {
    todayLabel: t('notebook.todos.dateToday'),
    suffix: t(`notebook.todos.${suffixKey}`),
    locale: locale.value,
    weekday: weekdayLabel,
  })
}

function cardMeta(row: NbTodoItem): TodoCardMetaItem[] {
  const meta: TodoCardMetaItem[] = []

  if (row.completed === 1) {
    const completedAt = row.updateTime ?? row.createTime
    if (completedAt) {
      meta.push({
        kind: 'completed',
        text: scheduleLabel(completedAt, 'completedSuffix'),
        className: 'is-completed-time',
      })
    }
    return meta
  }

  if (row.dueTime) {
    let className = 'is-due-normal'
    if (isOverdue(row)) {
      className = 'is-overdue'
    } else if (isToday(row.dueTime)) {
      className = 'is-due-today'
    }
    meta.push({
      kind: 'due',
      text: scheduleLabel(row.dueTime, 'dueSuffix'),
      className,
    })
  }

  if (row.remindTime) {
    meta.push({
      kind: 'remind',
      text: scheduleLabel(row.remindTime, 'remindSuffix'),
      className: isRemindSoon(row) ? 'is-remind-today' : 'is-remind-normal',
    })
  }

  return meta
}

function buildSchedulePayload() {
  const recurring = form.repeatType !== 'NONE'
  const repeatDays = encodeRepeatDays(
    form.repeatType,
    form.repeatWeekdays,
    form.repeatMonthDays,
    form.repeatYearDays,
  )

  if (recurring) {
    const dateAnchor =
      dueDateAnchor.value ||
      computeInitialDueDate(form.repeatType, repeatDays, form.repeatInterval)
    return {
      dueTime: mergeDateAndTime(dateAnchor, form.dueTimeOnly) || undefined,
      clearDueTime: !form.dueTimeOnly,
      remindTime: mergeDateAndTime(dateAnchor, form.remindTimeOnly) || undefined,
      clearRemindTime: !form.remindTimeOnly,
      repeatType: form.repeatType,
      repeatInterval: form.repeatInterval,
      repeatUntil: form.repeatUntil || undefined,
      clearRepeatUntil: !form.repeatUntil,
      repeatDays,
      clearRepeatDays: !repeatDays,
    }
  }

  return {
    dueTime: form.dueTime || undefined,
    clearDueTime: !form.dueTime,
    remindTime: form.remindTime || undefined,
    clearRemindTime: !form.remindTime,
    repeatType: form.repeatType,
    repeatInterval: 1,
    repeatUntil: undefined,
    clearRepeatUntil: true,
    repeatDays: undefined,
    clearRepeatDays: true,
  }
}

function validateRepeatDays(): boolean {
  if (form.repeatType === 'WEEKLY' && !form.repeatWeekdays.length) {
    ElMessage.warning(t('notebook.todos.repeatDaysRequired'))
    return false
  }
  if (form.repeatType === 'MONTHLY' && !form.repeatMonthDays.length) {
    ElMessage.warning(t('notebook.todos.repeatDaysRequired'))
    return false
  }
  if (form.repeatType === 'YEARLY' && !form.repeatYearDays.length) {
    ElMessage.warning(t('notebook.todos.repeatDaysRequired'))
    return false
  }
  return true
}

function mergeItems(next: NbTodoItem[]) {
  const map = new Map<number, NbTodoItem>()
  for (const item of next) {
    map.set(item.id, item)
  }
  return [...map.values()]
}

function refreshStats() {
  void refreshTodayCount()
  void loadStats()
}

async function reload() {
  await Promise.all([loadTodos(), loadStats()])
}

async function loadStats() {
  try {
    const [pending, done, today] = await Promise.all([
      fetchTodos({ completed: false }),
      fetchTodos({ completed: true }),
      fetchTodayTodos(),
    ])
    statPending.value = pending.length
    statDone.value = done.length
    statToday.value = today.filter((row) => row.completed !== 1).length
    statOverdue.value = pending.filter((row) => isOverdue(row)).length
  } catch {
    statPending.value = 0
    statDone.value = 0
    statToday.value = 0
    statOverdue.value = 0
  }
}

async function loadTodos() {
  loading.value = true
  try {
    const completed = filter.value === 'all' ? undefined : filter.value === 'done'
    items.value = await fetchTodos({ completed })
  } catch {
    items.value = []
    ElMessage.error(t('notebook.todos.loadFailed'))
  } finally {
    loading.value = false
  }
}

function setFilter(next: NbTodoFilter) {
  if (filter.value === next) {
    void loadTodos()
    return
  }
  filter.value = next
  void loadTodos()
}

function resetForm() {
  form.content = ''
  form.dueTime = null
  form.remindTime = null
  form.dueTimeOnly = null
  form.remindTimeOnly = null
  form.repeatType = 'NONE'
  form.repeatInterval = 1
  form.repeatUntil = null
  form.repeatWeekdays = []
  form.repeatMonthDays = []
  form.repeatYearDays = []
  dueDateAnchor.value = null
  yearDayDraft.value = null
}

function openCreateDialog() {
  dialogMode.value = 'create'
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: NbTodoItem) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.content = row.content
  form.repeatType = (row.repeatType as NbTodoRepeatType) ?? 'NONE'
  form.repeatInterval = row.repeatInterval ?? 1
  form.repeatUntil = row.repeatUntil ?? null
  const decoded = decodeRepeatDays(form.repeatType, row.repeatDays)
  form.repeatWeekdays = decoded.weekdays
  form.repeatMonthDays = decoded.monthDays
  form.repeatYearDays = decoded.yearDays
  yearDayDraft.value = null

  if (form.repeatType !== 'NONE') {
    dueDateAnchor.value = extractDatePart(row.dueTime) || extractDatePart(row.remindTime)
    form.dueTimeOnly = extractTimePart(row.dueTime)
    form.remindTimeOnly = extractTimePart(row.remindTime)
    form.dueTime = null
    form.remindTime = null
  } else {
    dueDateAnchor.value = null
    form.dueTime = row.dueTime ?? null
    form.remindTime = row.remindTime ?? null
    form.dueTimeOnly = null
    form.remindTimeOnly = null
  }
  dialogVisible.value = true
}

async function submitDialog() {
  const content = form.content.trim()
  if (!content) {
    ElMessage.warning(t('notebook.todos.contentRequired'))
    return
  }
  if (form.repeatType !== 'NONE' && !validateRepeatDays()) {
    return
  }
  submitting.value = true
  try {
    const schedule = buildSchedulePayload()
    if (dialogMode.value === 'create') {
      const created = await createTodo({ content, ...schedule })
      if (filter.value !== 'done') {
        items.value = mergeItems([created, ...items.value])
      }
    } else if (editingId.value) {
      const result = await updateTodo(editingId.value, { content, ...schedule })
      items.value = mergeItems(
        items.value.map((item) => (item.id === result.item.id ? result.item : item)),
      )
    }
    dialogVisible.value = false
    ElMessage.success(t('pomodoro.common.saved'))
    refreshStats()
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  } finally {
    submitting.value = false
  }
}

async function onToggle(row: NbTodoItem, checked: boolean) {
  const prev = row.completed
  row.completed = checked ? 1 : 0
  try {
    const result = await updateTodo(row.id, { completed: checked })
    if (checked) {
      dismissTodoNotification(row.id)
    }
    const nextItems = items.value.filter((item) => item.id !== row.id)
    if (filter.value === 'all') {
      nextItems.push(result.item)
      if (result.nextOccurrence) {
        nextItems.push(result.nextOccurrence)
        ElMessage.success(t('notebook.todos.repeatSpawned'))
      }
      items.value = mergeItems(nextItems)
      refreshStats()
      return
    }
    if (filter.value === 'pending' && checked) {
      if (result.nextOccurrence) {
        items.value = mergeItems([...nextItems, result.nextOccurrence])
        ElMessage.success(t('notebook.todos.repeatSpawned'))
      } else {
        items.value = nextItems
      }
      refreshStats()
      return
    }
    if (filter.value === 'done' && !checked) {
      items.value = nextItems
      refreshStats()
      return
    }
    items.value = mergeItems(
      items.value.map((item) => (item.id === result.item.id ? result.item : item)),
    )
    refreshStats()
  } catch {
    row.completed = prev
    ElMessage.error(t('notebook.todos.saveFailed'))
  }
}

async function onTogglePin(row: NbTodoItem) {
  const prev = row.pinned ?? 0
  const next = prev === 1 ? 0 : 1
  row.pinned = next
  try {
    await updateTodo(row.id, { pinned: next === 1 })
    items.value = items.value.map((item) => (item.id === row.id ? { ...item, pinned: next } : item))
    refreshStats()
  } catch {
    row.pinned = prev
    ElMessage.error(t('notebook.todos.pinFailed'))
  }
}

function onCardMenu(command: string, row: NbTodoItem) {
  if (command === 'edit') {
    openEditDialog(row)
    return
  }
  if (command === 'delete') {
    void onRemove(row)
    return
  }
  if (command === 'revert') {
    void onToggle(row, false)
  }
}

async function onRemove(row: NbTodoItem) {
  try {
    await ElMessageBox.confirm(t('notebook.todos.deleteConfirm'), { type: 'warning' })
    await removeTodo(row.id)
    items.value = items.value.filter((item) => item.id !== row.id)
    ElMessage.success(t('pomodoro.common.deleted'))
    refreshStats()
  } catch {
    // 用户取消或请求失败
  }
}

onMounted(() => {
  void reload().finally(() => setupSortables())
})

onBeforeUnmount(() => {
  destroySortables()
})

defineExpose({ reload, setFilter })
</script>

<style scoped lang="scss">
.notebook-todos {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 480px;
}

.notebook-todos__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.notebook-todos__stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 80px;
  padding: 16px 18px;
  background: var(--wr-card);
  border: 1px solid var(--wr-border);
  border-radius: 12px;
  box-shadow: var(--wr-shadow);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    border-color: color-mix(in srgb, var(--wr-border) 70%, var(--wr-stat-blue));
    box-shadow: 0 4px 14px rgb(37 99 235 / 8%);
  }
}

.notebook-todos__stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  flex-shrink: 0;
  font-size: 22px;

  &--blue {
    background: var(--wr-stat-blue-bg);
    color: var(--wr-stat-blue);
  }

  &--green {
    background: var(--wr-stat-green-bg);
    color: var(--wr-stat-green);
  }

  &--purple {
    background: var(--wr-stat-purple-bg);
    color: var(--wr-stat-purple);
  }

  &--orange {
    background: var(--wr-stat-orange-bg);
    color: var(--wr-stat-orange);
  }
}

.notebook-todos__stat-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.notebook-todos__stat-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--wr-text-secondary);
  line-height: 1.3;
}

.notebook-todos__stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.notebook-todos__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.notebook-todos__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.notebook-todos__filter {
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid var(--wr-border);
  border-radius: 999px;
  background: var(--wr-card);
  color: var(--wr-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease, background 0.15s ease;

  &.is-active {
    border-color: var(--wr-rail-active-color);
    background: var(--wr-rail-active-bg);
    color: var(--wr-rail-active-color);
    font-weight: 600;
  }
}

.notebook-todos__toolbar-actions {
  display: flex;
  gap: 8px;
}

.notebook-todos__create-btn {
  min-height: 42px;
  padding: 0 22px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;

  :deep(.el-icon) {
    font-size: 16px;
    margin-right: 6px;
  }
}

.notebook-todos__board {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  align-items: start;
  min-height: 360px;
}

.notebook-todos__column {
  display: flex;
  flex-direction: column;
  min-height: 320px;
  background: var(--wr-bg);
  border: 1px solid var(--wr-border);
  border-radius: 12px;
  overflow: hidden;
}

.notebook-todos__column-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 14px;
  border-bottom: 2px solid var(--wr-border);
  background: var(--wr-card);
}

.notebook-todos__column.is-pending .notebook-todos__column-header {
  border-bottom-color: var(--wr-stat-gray);
}

.notebook-todos__column.is-remindToday .notebook-todos__column-header {
  border-bottom-color: var(--wr-stat-orange);
}

.notebook-todos__column.is-done .notebook-todos__column-header {
  border-bottom-color: var(--wr-stat-green);
}

.notebook-todos__column-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--wr-text);
}

.notebook-todos__column-count {
  min-width: 22px;
  height: 22px;
  padding: 0 7px;
  border-radius: 11px;
  font-size: 12px;
  line-height: 22px;
  text-align: center;
  font-variant-numeric: tabular-nums;
  color: var(--wr-text-secondary);
  background: var(--wr-stat-gray-bg);
}

.notebook-todos__column.is-remindToday .notebook-todos__column-count {
  color: var(--wr-stat-orange);
  background: var(--wr-stat-orange-bg);
}

.notebook-todos__column.is-done .notebook-todos__column-count {
  color: var(--wr-stat-green);
  background: var(--wr-stat-green-bg);
}

.notebook-todos__column-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  min-height: 0;
  padding: 12px;
  overflow-y: auto;
}

.notebook-todos__card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  column-gap: 8px;
  align-items: center;
  padding: 12px;
  background: var(--wr-card);
  border: 1px solid var(--wr-border);
  border-radius: 10px;
  box-shadow: var(--wr-shadow);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    border-color: color-mix(in srgb, var(--wr-border) 70%, var(--wr-stat-blue));
    box-shadow: 0 6px 16px rgb(37 99 235 / 8%);
  }

  &.is-done {
    opacity: 0.88;
  }

  &--ghost {
    opacity: 0.45;
    box-shadow: none;
  }

  &--chosen {
    border-color: var(--wr-rail-active-color);
    box-shadow: 0 8px 20px rgb(37 99 235 / 12%);
  }
}

.notebook-todos__card-lead {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  align-self: center;
}

.notebook-todos__card-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
}

.notebook-todos__card-title {
  margin: 0;
  width: 100%;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.45;
  color: var(--wr-text);
  word-break: break-word;
  cursor: pointer;
}

.notebook-todos__card-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  cursor: pointer;
}

.notebook-todos__repeat-tag {
  margin: 0;
  cursor: pointer;
}

.notebook-todos__card-drag {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--wr-text-secondary);
  cursor: grab;
  touch-action: none;

  &:hover {
    background: var(--wr-bg);
    color: var(--wr-text);
  }

  &:active {
    cursor: grabbing;
  }
}

.notebook-todos__card-drag-dots {
  display: grid;
  grid-template-columns: repeat(2, 3px);
  grid-template-rows: repeat(3, 3px);
  gap: 3px;
}

.notebook-todos__card-drag-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: currentColor;
}

.notebook-todos__card-check {
  margin: 0;
  height: auto;
  flex-shrink: 0;

  :deep(.el-checkbox__inner) {
    width: 18px;
    height: 18px;
  }

  :deep(.el-checkbox__inner::after) {
    top: 2px;
    left: 5px;
    width: 4px;
    height: 8px;
    border-width: 2px;
  }
}

.notebook-todos__card.is-done .notebook-todos__card-title {
  color: var(--wr-text-secondary);
  text-decoration: line-through;
}

.notebook-todos__card-meta-line {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--wr-text-secondary);
}

.notebook-todos__card-meta-icon {
  font-size: 13px;
  flex-shrink: 0;
}

.is-due-normal,
.is-completed-time,
.is-remind-normal {
  color: var(--wr-text-secondary);
}

.is-due-today,
.is-remind-today {
  color: var(--wr-stat-orange);
}

.is-overdue {
  color: #dc2626;
  font-weight: 700;

  .notebook-todos__card-meta-icon {
    color: #dc2626;
  }
}

.notebook-todos__card-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
  align-self: flex-start;
}

.notebook-todos__card-star,
.notebook-todos__card-more {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--wr-text-secondary);
  cursor: pointer;

  &:hover {
    background: var(--wr-bg);
    color: var(--wr-text);
  }

  .el-icon {
    font-size: 16px;
  }
}

.notebook-todos__card-star.is-active {
  color: #f59e0b;

  &:hover {
    color: #d97706;
    background: #fffbeb;
  }
}

.notebook-todos__menu-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 96px;

  &.is-danger {
    color: var(--el-color-danger);
  }
}

.notebook-todos__column-empty {
  padding: 28px 12px;
  text-align: center;
  font-size: 13px;
  color: var(--wr-muted);
  border: 1px dashed var(--wr-border);
  border-radius: 10px;
  background: rgb(255 255 255 / 60%);
}

.notebook-todos__global-empty {
  margin-top: -8px;
}

.notebook-todos__repeat-form {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.notebook-todos__weekday-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}

.notebook-todos__year-days {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.notebook-todos__year-day-tag {
  margin: 0;
}

.notebook-todos__repeat-gap {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

@media (max-width: 1100px) {
  .notebook-todos__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .notebook-todos__board {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .notebook-todos__stats {
    grid-template-columns: 1fr;
  }
}
</style>
