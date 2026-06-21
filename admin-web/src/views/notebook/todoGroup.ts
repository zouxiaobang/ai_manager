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

export function isOverdue(item: NbTodoItem): boolean {
  if (item.completed === 1 || !item.dueTime) return false
  const due = parseDateTime(item.dueTime)
  if (!due) return false
  return due < startOfToday()
}

export function isRemindSoon(item: NbTodoItem): boolean {
  if (item.completed === 1 || !item.remindTime) return false
  return isToday(item.remindTime)
}
