import { deleteData, getData, postData, putData } from '../request'

export type NbTodoRepeatType = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'

export type NbTodoFilter = 'pending' | 'done' | 'all' | 'today'

export interface NbTodoItem {
  id: number
  content: string
  completed: number
  dueTime?: string | null
  remindTime?: string | null
  repeatType?: NbTodoRepeatType | string
  repeatInterval?: number
  repeatUntil?: string | null
  repeatDays?: string | null
  seriesId?: number | null
  sortOrder?: number
  pinned?: number
  createTime?: string
  updateTime?: string
}

export interface NbTodoSaveRequest {
  content?: string
  completed?: boolean
  dueTime?: string | null
  clearDueTime?: boolean
  remindTime?: string | null
  clearRemindTime?: boolean
  repeatType?: NbTodoRepeatType | string
  repeatInterval?: number
  repeatUntil?: string | null
  clearRepeatUntil?: boolean
  repeatDays?: string | null
  clearRepeatDays?: boolean
  sortOrder?: number
  pinned?: boolean
}

export interface NbTodoMutation {
  item: NbTodoItem
  nextOccurrence?: NbTodoItem
}

export const TODO_REPEAT_TYPES: NbTodoRepeatType[] = [
  'NONE',
  'DAILY',
  'WEEKLY',
  'MONTHLY',
  'YEARLY',
]

export function fetchTodos(options?: { completed?: boolean; today?: boolean; pinned?: boolean }) {
  const params: Record<string, boolean> = {}
  if (options?.completed !== undefined) {
    params.completed = options.completed
  }
  if (options?.today) {
    params.today = true
  }
  if (options?.pinned) {
    params.pinned = true
  }
  return getData<NbTodoItem[]>('/api/todos', params)
}

export function fetchPinnedTodos() {
  return fetchTodos({ pinned: true })
}

export function fetchTodayTodos() {
  return getData<NbTodoItem[]>('/api/todos/today')
}

export function fetchDueReminders() {
  return getData<NbTodoItem[]>('/api/todos/reminders/due')
}

export function ackTodoRemind(id: number) {
  return postData<void>(`/api/todos/${id}/remind-ack`)
}

export function createTodo(body: NbTodoSaveRequest) {
  return postData<NbTodoItem>('/api/todos', body)
}

export function updateTodo(id: number, body: NbTodoSaveRequest) {
  return putData<NbTodoMutation>(`/api/todos/${id}`, body)
}

export function removeTodo(id: number) {
  return deleteData(`/api/todos/${id}`)
}
