<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="720px"
    top="8vh"
    class="deploy-run-dialog"
    :close-on-click-modal="false"
    :close-on-press-escape="!running"
    :show-close="!running"
    destroy-on-close
    @closed="handleClosed"
  >
    <div class="deploy-run-dialog__meta">
      <span class="deploy-run-dialog__badge" :class="`is-${phase}`">{{ phaseLabel }}</span>
      <span v-if="projectRoot" class="deploy-run-dialog__root">{{ projectRoot }}</span>
    </div>

    <div ref="logViewportRef" class="deploy-run-dialog__log">
      <pre class="deploy-run-dialog__pre">{{ logText || placeholder }}</pre>
    </div>

    <template #footer>
      <el-button :disabled="running" @click="visible = false">
        {{ running ? t('deployCenter.deployRun.runningCloseHint') : t('deployCenter.deployRun.close') }}
      </el-button>
      <el-button
        v-if="!running && phase === 'failed'"
        type="primary"
        @click="restart"
      >
        {{ t('deployCenter.deployRun.retry') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  fetchDeployRunnerStatus,
  fetchDeployPreflight,
  streamDeploy,
  type DeployRunnerStatus,
} from '@/api/deployRunner'

const props = defineProps<{
  target: 'backend' | 'frontend'
}>()

const emit = defineEmits<{
  finished: [success: boolean]
}>()

const visible = defineModel<boolean>({ required: true })

const { t } = useI18n()

type Phase = 'idle' | 'checking' | 'running' | 'success' | 'failed' | 'unavailable'

const phase = ref<Phase>('idle')
const logs = ref<string[]>([])
const projectRoot = ref('')
const logViewportRef = ref<HTMLElement | null>(null)
let stopStream: (() => void) | null = null

const running = computed(() => phase.value === 'running' || phase.value === 'checking')

const dialogTitle = computed(() =>
  props.target === 'backend'
    ? t('deployCenter.deployRun.titleBackend')
    : t('deployCenter.deployRun.titleFrontend'),
)

const logText = computed(() => logs.value.join('\n'))

const placeholder = computed(() => {
  if (phase.value === 'checking') return t('deployCenter.deployRun.checking')
  if (phase.value === 'unavailable') return t('deployCenter.deployRun.unavailable')
  return t('deployCenter.deployRun.waiting')
})

const phaseLabel = computed(() => {
  switch (phase.value) {
    case 'checking':
      return t('deployCenter.deployRun.phaseChecking')
    case 'running':
      return t('deployCenter.deployRun.phaseRunning')
    case 'success':
      return t('deployCenter.deployRun.phaseSuccess')
    case 'failed':
      return t('deployCenter.deployRun.phaseFailed')
    case 'unavailable':
      return t('deployCenter.deployRun.phaseUnavailable')
    default:
      return t('deployCenter.deployRun.phaseIdle')
  }
})

function appendLog(line: string) {
  logs.value.push(line)
  void nextTick(() => {
    const el = logViewportRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function cleanupStream() {
  stopStream?.()
  stopStream = null
}

async function prepareAndStart() {
  cleanupStream()
  logs.value = []
  phase.value = 'checking'
  projectRoot.value = ''

  let status: DeployRunnerStatus
  try {
    status = await fetchDeployRunnerStatus()
  } catch (err) {
    phase.value = 'failed'
    appendLog(err instanceof Error ? err.message : t('deployCenter.deployRun.statusFailed'))
    return
  }

  if (!status.enabled || !status.available) {
    phase.value = 'unavailable'
    appendLog(status.message || t('deployCenter.deployRun.unavailableDetail'))
    return
  }

  if (status.running) {
    phase.value = 'failed'
    appendLog(t('deployCenter.deployRun.busy'))
    return
  }

  try {
    const preflight = await fetchDeployPreflight()
    const ready = preflight.ready ?? preflight.sshReady
    if (!ready) {
      phase.value = 'failed'
      if (preflight.deployMode === 'local') {
        appendLog(t('deployCenter.deployRun.localNotReady'))
        appendLog(preflight.message || t('deployCenter.deployRun.localSetupHint'))
      } else {
        appendLog(t('deployCenter.deployRun.sshNotReady', { target: preflight.sshTarget ?? '—' }))
        appendLog(
          preflight.message
            || t('deployCenter.deployRun.sshSetupHint', { target: preflight.sshTarget ?? '—' }),
        )
      }
      return
    }
  } catch (err) {
    phase.value = 'failed'
    appendLog(err instanceof Error ? err.message : t('deployCenter.deployRun.sshCheckFailed'))
    return
  }

  projectRoot.value = status.projectRoot || ''
  phase.value = 'running'
  appendLog(t('deployCenter.deployRun.started'))

  stopStream = streamDeploy(props.target, {
    onLog: appendLog,
    onDone: (success, exitCode) => {
      phase.value = success ? 'success' : 'failed'
      if (success) {
        appendLog(t('deployCenter.deployRun.doneSuccess'))
        ElMessage.success(t('deployCenter.deployRun.doneToast'))
      } else {
        appendLog(t('deployCenter.deployRun.doneFailed', { code: exitCode }))
        ElMessage.error(t('deployCenter.deployRun.failToast'))
      }
      emit('finished', success)
    },
    onError: (message) => {
      if (phase.value === 'running') {
        phase.value = 'failed'
        appendLog(message)
        ElMessage.error(message)
      }
    },
  })
}

function restart() {
  void prepareAndStart()
}

function handleClosed() {
  cleanupStream()
  phase.value = 'idle'
  logs.value = []
  projectRoot.value = ''
}

watch(
  () => visible.value,
  (open) => {
    if (open) void prepareAndStart()
    else handleClosed()
  },
)
</script>

<style scoped lang="scss">
.deploy-run-dialog {
  &__meta {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
    min-height: 28px;
  }

  &__badge {
    display: inline-flex;
    align-items: center;
    padding: 4px 10px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 600;
    background: #f3f4f6;
    color: #6b7280;

    &.is-running,
    &.is-checking {
      background: #eff6ff;
      color: #1d4ed8;
    }

    &.is-success {
      background: #f0fdf4;
      color: #15803d;
    }

    &.is-failed,
    &.is-unavailable {
      background: #fef2f2;
      color: #b91c1c;
    }
  }

  &__root {
    font-size: 12px;
    color: #9ca3af;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__log {
    height: 360px;
    overflow: auto;
    border-radius: 12px;
    border: 1px solid #1f2937;
    background: #0f172a;
  }

  &__pre {
    margin: 0;
    padding: 14px 16px;
    font-family: Consolas, 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.55;
    color: #e2e8f0;
    white-space: pre-wrap;
    word-break: break-word;
  }
}
</style>

<style lang="scss">
.deploy-run-dialog {
  .el-dialog__body {
    padding-top: 12px;
  }
}
</style>
