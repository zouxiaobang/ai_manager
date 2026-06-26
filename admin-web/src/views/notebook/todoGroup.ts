import type { NbTodoFilter, NbTodoItem } from '@/api/notebook/todo'

export type TodoGroupKey = 'today' | 'overdue' | 'upcoming' | 'noDate' | 'done'

export interface TodoGroup {
  key: TodoGroupKey
  items: NbTodoItem[]
}

const GROUP_ORDER: TodoGroupKey[] = ['today', 'overdue', 'upcoming', 'noDate', 'done']

function parseDateTime(value?: string | null): Date | null {
  if (!value) return null
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

function startOfToday(): Date {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), now.getDate())
}

function endOfToday(): Date {
  const start = startOfToday()
  return new Date(start.getFullYear(), start.getMonth(), start.getDate(), 23, 59, 59, 999)
}

export function isToday(value?: string | null): boolean {
  const date = parseDateTime(value)
  if (!date) return false
  const start = startOfToday()
  const end = endOfToday()
  return date >= start && date <= end
}

export function classifyTodo(item: NbTodoItem): TodoGroupKey {
  if (item.completed === 1) {
    return 'done'
  }
  if (isToday(item.dueTime) || isToday(item.remindTime)) {
    return 'today'
  }
  const due = parseDateTime(item.dueTime)
  const remind = parseDateTime(item.remindTime)
  if (!due && !remind) {
    return 'noDate'
  }
  const todayStart = startOfToday()
  if (due && due < todayStart) {
    return 'overdue'
  }
  if (!due && remind && remind < todayStart) {
    return 'overdue'
  }
  return 'upcoming'
}

export type KanbanColumnKey = 'pending' | 'remindToday' | 'done'

export interface KanbanColumn {
  key: KanbanColumnKey
  items: NbTodoItem[]
}

export const KANBAN_COLUMN_SORT_BASE: Record<KanbanColumnKey, number> = {
  remindToday: 0,
  pending: 100_000,
  done: 200_000,
}

const KANBAN_ORDER: KanbanColumnKey[] = ['remindToday', 'pending', 'done']

function getScheduleTimestamp(item: NbTodoItem): number | null {
  const due = parseDateTime(item.dueTime)
  if (due) return due.getTime()
  const remind = parseDateTime(item.remindTime)
  if (remind) return remind.getTime()
  return null
}

export function compareTodosBySchedule(a: NbTodoItem, b: NbTodoItem): number {
  const orderA = a.sortOrder ?? 0
  const orderB = b.sortOrder ?? 0
  if (orderA !== orderB) {
    return orderA - orderB
  }

  const timeA = getScheduleTimestamp(a)
  const timeB = getScheduleTimestamp(b)
  if (timeA === null && timeB === null) {
    return (b.id ?? 0) - (a.id ?? 0)
  }
  if (timeA === null) return 1
  if (timeB === null) return -1
  if (timeA !== timeB) {
    return timeA - timeB
  }
  return (b.id ?? 0) - (a.id ?? 0)
}

export function sortTodoItems(items: NbTodoItem[]): NbTodoItem[] {
  return [...items].sort(compareTodosBySchedule)
}

export function resolveKanbanColumnKey(item: NbTodoItem): KanbanColumnKey {
  if (item.completed === 1) {
    return 'done'
  }
  if (isToday(item.remindTime) || isToday(item.dueTime)) {
    return 'remindToday'
  }
  return 'pending'
}

export function buildColumnSortOrders(columnKey: KanbanColumnKey, orderedIds: number[]): Map<number, number> {
  const base = KANBAN_COLUMN_SORT_BASE[columnKey]
  const map = new Map<number, number>()
  orderedIds.forEach((id, index) => {
    map.set(id, base + index)
  })
  return map
}

export function splitTodosForKanban(items: NbTodoItem[]): KanbanColumn[] {
  const buckets: Record<KanbanColumnKey, NbTodoItem[]> = {
    pending: [],
    remindToday: [],
    done: [],
  }

  for (const item of items) {
    buckets[resolveKanbanColumnKey(item)].push(item)
  }

  return KANBAN_ORDER.map((key) => ({
    key,
    items: sortTodoItems(buckets[key]),
  }))
}

export function groupTodos(items: NbTodoItem[], filter: NbTodoFilter): TodoGroup[] {
  if (filter === 'today') {
    return items.length ? [{ key: 'today', items }] : []
  }
  const buckets = new Map<TodoGroupKey, NbTodoItem[]>()
  for (const key of GROUP_ORDER) {
    buckets.set(key, [])
  }

  for (const item of items) {
    const key = classifyTodo(item)
    if (filter === 'pending' && key === 'done') {
      continue
    }
    if (filter === 'done' && key !== 'done') {
      continue
    }
    buckets.get(key)?.push(item)
  }

  const groups: TodoGroup[] = []
  for (const key of GROUP_ORDER) {
    const groupItems = buckets.get(key) ?? []
    if (!groupItems.length) {
      continue
    }
    if (filter === 'pending' && key === 'done') {
      continue
    }
    if (filter === 'done' && key !== 'done') {
      continue
    }
    groups.push({ key, items: groupItems })
  }
  return groups
}

export function formatDateTime(value?: string | null): string {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

export interface TodoScheduleLabelOptions {
  todayLabel: string
  suffix: string
  locale: string
  weekday: (dayIndex: number) => string
}

function formatTimeHm(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function formatMonthDay(date: Date, locale: string): string {
  if (locale.startsWith('zh')) {
    return `${date.getMonth() + 1}月${date.getDate()}日`
  }
  return date.toLocaleDateString(locale, { month: 'short', day: 'numeric' })
}

function formatWeekdayParen(date: Date, locale: string, weekday: (dayIndex: number) => string): string {
  const label = weekday(date.getDay())
  return locale.startsWith('zh') ? `（${label}）` : ` (${label})`
}

export function formatTodoScheduleLabel(value: string, options: TodoScheduleLabelOptions): string {
  const date = parseDateTime(value)
  if (!date) return ''

  const { todayLabel, suffix, locale, weekday } = options
  if (isToday(value)) {
    return `${todayLabel} ${formatTimeHm(date)} ${suffix}`
  }
  return `${formatMonthDay(date, locale)}${formatWeekdayParen(date, locale, weekday)} ${suffix}`
}

export type TodoCardMetaKind = 'due' | 'completed' | 'remind'

export interface TodoCardMetaItem {
  kind: TodoCardMetaKind
  text: string
  className?: string
}

export function isOverdue(item: NbTodoItem): boolean {
  if (item.completed === 1 || !item.dueTime) return false
  const due = parseDateTime(item.dueTime)
  if (!due) return false
  return due.getTime() < Date.now()
}

export function isRemindSoon(item: NbTodoItem): boolean {
  if (item.completed === 1 || !item.remindTime) return false
  return isToday(item.remindTime)
}
