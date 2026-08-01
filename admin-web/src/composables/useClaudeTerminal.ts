import { ref, readonly, onUnmounted } from 'vue'
import { connectClaudeTerminal, type ClaudeTerminalConnection } from '@/api/claudeTerminal'

// ─── Module-level shared state (shared across ClaudeTerminal instances) ──
const connected = ref(false)
const error = ref<string | null>(null)
const sessionActive = ref(false)
const exited = ref(false)
const exitCode = ref<number | null>(null)

let connection: ClaudeTerminalConnection | null = null

type TerminalCallbacks = {
  onData: (data: string) => void
  onReady: (cols: number, rows: number) => void
  onExit: (code: number, signal?: number) => void
}

let callbacks: TerminalCallbacks | null = null

// Track how many components are using this composable (on the same page/tab)
let instanceCount = 0

// ─── Composable ──────────────────────────────────────────────────────────────

export function useClaudeTerminal() {
  instanceCount++

  function registerCallbacks(cb: TerminalCallbacks) {
    callbacks = cb
  }

  function doConnect() {
    if (connection) return  // Already connected in this tab

    error.value = null
    exited.value = false
    exitCode.value = null

    connection = connectClaudeTerminal({
      onData(data) {
        sessionActive.value = true
        callbacks?.onData(data)
      },
      onReady(_cols, _rows) {
        connected.value = true
        sessionActive.value = true
        error.value = null
        callbacks?.onReady(_cols, _rows)
      },
      onExit(code, signal) {
        exited.value = true
        exitCode.value = code
        sessionActive.value = false
        callbacks?.onExit(code, signal)
      },
      onError(msg) {
        error.value = msg
        connected.value = false
        sessionActive.value = false
      },
      onClose() {
        connected.value = false
        sessionActive.value = false
        connection = null
      },
    })
  }

  function disconnect() {
    if (connection) {
      connection.close()
      connection = null
    }
    connected.value = false
    sessionActive.value = false
  }

  function sendData(data: string) {
    connection?.sendData(data)
  }

  function resize(cols: number, rows: number) {
    connection?.resize(cols, rows)
  }

  function sync() {
    connection?.sync()
  }

  onUnmounted(() => {
    instanceCount--
    if (instanceCount <= 0) {
      instanceCount = 0
      disconnect()
    }
  })

  return {
    connected: readonly(connected),
    error: readonly(error),
    sessionActive: readonly(sessionActive),
    exited: readonly(exited),
    exitCode: readonly(exitCode),
    registerCallbacks,
    connect: doConnect,
    disconnect,
    sendData,
    resize,
    sync,
  }
}
