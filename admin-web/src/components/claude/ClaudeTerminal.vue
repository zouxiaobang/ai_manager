<template>
  <div
    ref="containerRef"
    class="claude-terminal"
    :class="{ 'claude-terminal--inactive': !terminalActive }"
  >
    <!-- Connection overlay -->
    <Transition name="claude-terminal-overlay">
      <div v-if="!terminalActive" class="claude-terminal__overlay">
        <div class="claude-terminal__overlay-content">
          <div class="claude-terminal__overlay-icon">
            <span v-if="terminalError" class="claude-terminal__status-dot claude-terminal__status-dot--error" />
            <span v-else-if="terminalConnected" class="claude-terminal__status-dot claude-terminal__status-dot--ok" />
            <span v-else class="claude-terminal__status-dot claude-terminal__status-dot--idle" />
          </div>
          <p class="claude-terminal__overlay-text">
            {{ overlayText }}
          </p>
          <button
            v-if="terminalError || terminalExited"
            class="claude-terminal__reconnect-btn"
            @click="handleReconnect"
          >
            {{ t('claudeTerminal.reconnect') }}
          </button>
        </div>
      </div>
    </Transition>

    <!-- xterm.js mount point -->
    <div ref="xtermRef" class="claude-terminal__xterm" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { Terminal } from '@xterm/xterm'
import { FitAddon } from '@xterm/addon-fit'
import { WebLinksAddon } from '@xterm/addon-web-links'
import { useClaudeTerminal } from '@/composables/useClaudeTerminal'

import '@xterm/xterm/css/xterm.css'

// ─── Props ───────────────────────────────────────────────────────────────────
const props = withDefaults(
  defineProps<{
    fontSize?: number
    fontFamily?: string
    /** Minimum contrast ratio for cursor (3.0 meets WCAG AA) */
    cursorContrast?: number
    /** Scrollback buffer size (lower = less memory, better mobile perf) */
    scrollback?: number
    /** Enable cursor blinking (disable on mobile to reduce repaints) */
    cursorBlink?: boolean
    /** Enable proposed API (disable on mobile to reduce overhead) */
    allowProposedApi?: boolean
  }>(),
  {
    fontSize: 14,
    fontFamily: "'Cascadia Code', 'Fira Code', Consolas, 'Courier New', monospace",
    cursorContrast: 3.0,
    scrollback: 5000,
    cursorBlink: true,
    allowProposedApi: true,
  },
)

const { t } = useI18n()
const {
  connected: terminalConnected,
  error: terminalError,
  sessionActive,
  exited: terminalExited,
  exitCode: terminalExitCode,
  registerCallbacks,
  connect,
  disconnect,
  sendData,
  resize,
  sync,
} = useClaudeTerminal()

// ─── Refs ────────────────────────────────────────────────────────────────────
const containerRef = ref<HTMLDivElement>()
const xtermRef = ref<HTMLDivElement>()

let terminal: Terminal | null = null
let fitAddon: FitAddon | null = null

const terminalActive = computed(() => sessionActive.value && !terminalExited.value)

const overlayText = computed(() => {
  if (terminalError.value) {
    return terminalError.value
  }
  if (terminalExited.value) {
    const code = terminalExitCode.value
    return code != null
      ? t('claudeTerminal.exitedWithCode', { code })
      : t('claudeTerminal.exited')
  }
  if (terminalConnected.value) {
    return t('claudeTerminal.connecting')
  }
  return t('claudeTerminal.connecting')
})

// ─── Terminal initialization ─────────────────────────────────────────────────

function createTerminal() {
  if (!xtermRef.value || terminal) return

  terminal = new Terminal({
    fontSize: props.fontSize,
    fontFamily: props.fontFamily,
    theme: {
      background: '#1a1b1e',
      foreground: '#c9d1d9',
      cursor: '#c9d1d9',
      selectionBackground: '#264f78',
      black: '#484f58',
      red: '#ff7b72',
      green: '#3fb950',
      yellow: '#d29922',
      blue: '#58a6ff',
      magenta: '#bc8cff',
      cyan: '#39c5d6',
      white: '#b1bac4',
      brightBlack: '#6e7681',
      brightRed: '#ffa198',
      brightGreen: '#56d364',
      brightYellow: '#e3b341',
      brightBlue: '#79c0ff',
      brightMagenta: '#d2a8ff',
      brightCyan: '#56d4dd',
      brightWhite: '#f0f6fc',
    },
    cursorBlink: props.cursorBlink,
    cursorStyle: 'bar',
    allowProposedApi: props.allowProposedApi,
    scrollback: props.scrollback,
    tabStopWidth: 4,
  })

  fitAddon = new FitAddon()
  const webLinksAddon = new WebLinksAddon()

  terminal.loadAddon(fitAddon)
  terminal.loadAddon(webLinksAddon)

  // Try WebGL addon for better performance
  import('@xterm/addon-webgl').then(({ WebglAddon }) => {
    try {
      const webglAddon = new WebglAddon()
      terminal!.loadAddon(webglAddon)
    } catch {
      // WebGL not available, fallback to canvas renderer silently
    }
  }).catch(() => {
    // WebGL addon failed to load
  })

  terminal.open(xtermRef.value)
  fitAddon.fit()

  // Sync PTY dimensions with actual terminal size
  resize(terminal.cols, terminal.rows)

  // Forward user input
  terminal.onData((data) => {
    sendData(data)
  })
}

function destroyTerminal() {
  if (terminal) {
    terminal.dispose()
    terminal = null
    fitAddon = null
  }
}

// ─── Resize handling ─────────────────────────────────────────────────────────

let resizeDebounce: ReturnType<typeof setTimeout> | null = null

function handleResize() {
  if (!fitAddon || !terminal) return

  // Debounce resize to avoid flooding on mobile orientation changes
  if (resizeDebounce) clearTimeout(resizeDebounce)
  resizeDebounce = setTimeout(() => {
    try {
      fitAddon!.fit()
      if (terminal) {
        resize(terminal.cols, terminal.rows)
      }
    } catch {
      // Container might not be visible
    }
  }, 100)
}

// ─── Reconnect ───────────────────────────────────────────────────────────────

function handleReconnect() {
  destroyTerminal()
  disconnect()
  nextTick(() => {
    createTerminal()
    connect()
  })
}

// ─── Lifecycle ───────────────────────────────────────────────────────────────

registerCallbacks({
  onData(data: string) {
    // Write PTY output to xterm.js. The terminal might not exist yet if data
    // arrives before the component is mounted; that's fine — it's just the
    // initial conpty clear-screen sequence.
    if (terminal) {
      terminal.write(data)
    }
  },
  onReady(_cols: number, _rows: number) {
    nextTick(() => {
      fitAddon?.fit()
      if (terminal) {
        resize(terminal.cols, terminal.rows)
        // Request buffered content replay now that terminal is fitted
        sync()
      }
    })
  },
  onExit(_code: number, _signal?: number) {
    // Overlay will show
  },
})

onMounted(() => {
  nextTick(() => {
    createTerminal()
    connect()
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (resizeDebounce) clearTimeout(resizeDebounce)
  destroyTerminal()
  disconnect()
})

// Expose for parent
defineExpose({
  fit: () => fitAddon?.fit(),
  focus: () => terminal?.focus(),
  /** Scroll viewport by delta lines: positive = scroll down (older content), negative = scroll up */
  scrollLines: (delta: number) => terminal?.scrollLines(delta),
})
</script>

<style scoped>
.claude-terminal {
  position: relative;
  width: 100%;
  height: 100%;
  background: #1a1b1e;
  border-radius: 6px;
  overflow: hidden;
}

.claude-terminal--inactive {
  opacity: 0.7;
}

.claude-terminal__xterm {
  width: 100%;
  height: 100%;
  padding: 8px 4px 4px 8px;
}

.claude-terminal__overlay {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(13, 17, 23, 0.85);
  backdrop-filter: blur(4px);
}

.claude-terminal__overlay-content {
  text-align: center;
  color: #c9d1d9;
}

.claude-terminal__overlay-icon {
  margin-bottom: 12px;
}

.claude-terminal__status-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.claude-terminal__status-dot--ok {
  background: #3fb950;
  box-shadow: 0 0 8px rgba(63, 185, 80, 0.5);
}

.claude-terminal__status-dot--error {
  background: #f85149;
  box-shadow: 0 0 8px rgba(248, 81, 73, 0.5);
}

.claude-terminal__status-dot--idle {
  background: #d29922;
  animation: claude-terminal-pulse 1.5s ease-in-out infinite;
}

@keyframes claude-terminal-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.claude-terminal__overlay-text {
  margin: 0 0 16px;
  font-size: 14px;
  line-height: 1.5;
  max-width: 320px;
}

.claude-terminal__reconnect-btn {
  padding: 8px 20px;
  border: 1px solid #30363d;
  border-radius: 6px;
  background: #21262d;
  color: #c9d1d9;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.claude-terminal__reconnect-btn:hover {
  background: #30363d;
  border-color: #58a6ff;
}

/* Overlay transition */
.claude-terminal-overlay-enter-active,
.claude-terminal-overlay-leave-active {
  transition: opacity 0.3s ease;
}

.claude-terminal-overlay-enter-from,
.claude-terminal-overlay-leave-to {
  opacity: 0;
}
</style>
