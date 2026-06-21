import { onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElNotification } from 'element-plus'
import { ackTodoRemind, fetchDueReminders, fetchTodayTodos } from '@/api/notebook/todo'

const POLL_INTERVAL_MS = 30_000

const todayTodoCount = ref(0)
const activeTodoNotifications = new Map<number, () => void>()
let timer: ReturnType<typeof setInterval> | null = null
let polling = false
let subscriberCount = 0
let notifyTitle = '待办提醒'

async function refreshTodayCount() {
  try {
    const items = await fetchTodayTodos()
    todayTodoCount.value = items.length
  } catch {
    todayTodoCount.value = 0
  }
}

function registerTodoNotification(todoId: number, close: () => void) {
  activeTodoNotifications.get(todoId)?.()
  activeTodoNotifications.set(todoId, close)
}

function unregisterTodoNotification(todoId: number) {
  activeTodoNotifications.delete(todoId)
}

export function dismissTodoNotification(todoId: number) {
  const close = activeTodoNotifications.get(todoId)
  if (!close) return
  close()
  unregisterTodoNotification(todoId)
}

function showTodoNotification(todoId: number, body: string) {
  if ('Notification' in window && Notification.permission === 'granted') {
    const notification = new Notification(notifyTitle, {
      body,
      tag: `todo-remind-${todoId}`,
      requireInteraction: true,
    })
    registerTodoNotification(todoId, () => notification.close())
    notification.onclose = () => unregisterTodoNotification(todoId)
    notification.onclick = () => {
      window.focus()
      dismissTodoNotification(todoId)
    }
    return
  }

  const handle = ElNotification({
    title: notifyTitle,
    message: body,
    type: 'warning',
    duration: 0,
    showClose: true,
    onClose: () => unregisterTodoNotification(todoId),
  })
  registerTodoNotification(todoId, () => handle.close())
}

async function pollDueReminders() {
  if (polling) return
  polling = true
  try {
    const dueItems = await fetchDueReminders()
    for (const item of dueItems) {
      showTodoNotification(item.id, item.content)
      await ackTodoRemind(item.id)
    }
    if (dueItems.length > 0) {
      await refreshTodayCount()
    }
  } catch {
    // 轮询失败时静默，下一轮重试
  } finally {
    polling = false
  }
}

function startPolling() {
  if (timer) return
  void refreshTodayCount()
  void pollDueReminders()
  timer = setInterval(() => {
    void refreshTodayCount()
    void pollDueReminders()
  }, POLL_INTERVAL_MS)
}

function stopPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

export function useTodoReminders() {
  const { t } = useI18n()
  notifyTitle = t('notebook.todos.remindTitle')

  async function requestPermission() {
    if (!('Notification' in window)) {
      return false
    }
    if (Notification.permission === 'granted') {
      return true
    }
    if (Notification.permission === 'denied') {
      return false
    }
    const result = await Notification.requestPermission()
    return result === 'granted'
  }

  onMounted(() => {
    subscriberCount += 1
    if (subscriberCount === 1) {
      void requestPermission()
      startPolling()
    }
  })

  onUnmounted(() => {
    subscriberCount -= 1
    if (subscriberCount <= 0) {
      subscriberCount = 0
      stopPolling()
    }
  })

  return {
    todayTodoCount,
    refreshTodayCount,
    dismissTodoNotification,
  }
}
