import { onMounted, onUnmounted } from 'vue'
import { ElNotification } from 'element-plus'
import { getCurrentPhase, type PhaseDef } from '@/data/24hour-phases'

const NOTIFIED_PHASE_KEY = 'tfh_notified_phase'

let timer: ReturnType<typeof setInterval> | null = null
let currentNotification: { close: () => void } | null = null
let subscriberCount = 0

function getNotifiedPhase(): string {
  return localStorage.getItem(NOTIFIED_PHASE_KEY) || ''
}

function setNotifiedPhase(key: string) {
  localStorage.setItem(NOTIFIED_PHASE_KEY, key)
}

function showPhaseNotification(phase: PhaseDef) {
  closeCurrentNotification()

  const total = phase.items.length
  const bodyText = `当前时段：${phase.badge} - ${phase.title}，共 ${total} 项任务待完成`

  if ('Notification' in window && Notification.permission === 'granted') {
    const n = new Notification('24小时重启系统', {
      body: bodyText,
      tag: 'tfh-phase-notification',
      requireInteraction: true,
    })
    n.onclick = () => {
      window.focus()
      n.close()
    }
    currentNotification = { close: () => n.close() }
    return
  }

  const handle = ElNotification({
    title: '24小时重启系统',
    message: bodyText,
    type: 'warning',
    duration: 0,
    showClose: true,
  })
  currentNotification = { close: () => handle.close() }
}

function closeCurrentNotification() {
  if (currentNotification) {
    currentNotification.close()
    currentNotification = null
  }
}

function checkPhaseChange() {
  const phase = getCurrentPhase()
  if (!phase) return

  const notified = getNotifiedPhase()
  if (notified !== phase.key) {
    setNotifiedPhase(phase.key)
    showPhaseNotification(phase)
  }
}

function startPolling() {
  if (timer) return
  checkPhaseChange()
  timer = setInterval(checkPhaseChange, 60_000)
}

function stopPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

export function use24HourNotification() {
  onMounted(() => {
    subscriberCount += 1
    if (subscriberCount === 1) {
      if ('Notification' in window && Notification.permission !== 'denied') {
        Notification.requestPermission()
      }
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
    dismissNotification: closeCurrentNotification,
  }
}

export function dismiss24HourNotification() {
  closeCurrentNotification()
}

export function markPhaseNotified(key: string) {
  setNotifiedPhase(key)
}
