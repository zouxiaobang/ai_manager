/**
 * WebSocket connection manager for Claude Code terminal relay.
 *
 * In development the Vite dev server proxies /claude-relay to the relay server.
 * In production Nginx proxies /claude-relay to the relay server.
 * Multiple clients can connect simultaneously and share the same PTY session.
 */
export interface ClaudeTerminalOptions {
  onData: (data: string) => void
  onReady: (cols: number, rows: number) => void
  onExit: (exitCode: number, signal?: number) => void
  onError: (message: string) => void
  onClose: () => void
}

export interface ClaudeTerminalConnection {
  sendData: (data: string) => void
  resize: (cols: number, rows: number) => void
  /** Ask relay to replay buffered content (called after terminal is fitted) */
  sync: () => void
  close: () => void
}

function buildUrl(): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  return `${protocol}//${host}/claude-relay`
}

export function connectClaudeTerminal(
  options: ClaudeTerminalOptions,
): ClaudeTerminalConnection {
  const url = buildUrl()
  const ws = new WebSocket(url)
  ws.binaryType = 'blob'

  let transportError = false

  ws.addEventListener('open', () => {
    // Connection established — waiting for 'ready' message from relay
  })

  ws.addEventListener('message', (event) => {
    if (typeof event.data === 'string') {
      try {
        const msg = JSON.parse(event.data)
        if (msg.type === 'ready') {
          options.onReady(msg.cols ?? 80, msg.rows ?? 24)
          return
        }
        if (msg.type === 'exit') {
          options.onExit(msg.exitCode ?? 0, msg.signal)
          return
        }
        if (msg.type === 'error') {
          options.onError(msg.message ?? 'Unknown error')
          return
        }
        // 'clients' message: ignore for now (could show connected client count)
        if (msg.type === 'clients') {
          return
        }
      } catch {
        // Not JSON — it's terminal output
      }
    }

    // Terminal output data
    if (typeof event.data === 'string') {
      options.onData(event.data)
    } else if (event.data instanceof Blob) {
      const reader = new FileReader()
      reader.onload = () => {
        if (typeof reader.result === 'string') {
          options.onData(reader.result)
        }
      }
      reader.readAsText(event.data)
    }
  })

  ws.addEventListener('close', (event) => {
    if (transportError && !event.wasClean) {
      options.onError('Cannot reach Claude terminal service. Please check that the relay server is running.')
    }
    options.onClose()
  })

  ws.addEventListener('error', () => {
    transportError = true
  })

  return {
    sendData(data: string) {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(data)
      }
    },
    resize(cols: number, rows: number) {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'resize', cols, rows }))
      }
    },
    sync() {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'sync' }))
      }
    },
    close() {
      ws.close()
    },
  }
}
