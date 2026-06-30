import { getData } from './request'

export interface DeployRunnerStatus {
  enabled: boolean
  available: boolean
  running: boolean
  runningTarget?: string | null
  platform?: string
  projectRoot?: string
  message?: string
  deployMode?: 'local' | 'remote'
  runAsUser?: string
  lastDeploy?: {
    target?: string
    running?: boolean
    success?: boolean
    exitCode?: number
    startedAt?: number
    finishedAt?: number
  } | null
}

export interface DeployPreflightStatus {
  sshTarget?: string
  sshReady: boolean
  ready?: boolean
  deployMode?: 'local' | 'remote'
  message?: string
}

export interface DeployStreamHandlers {
  onLog: (line: string) => void
  onDone: (success: boolean, exitCode: number) => void
  onDisconnect: () => void
}

export function fetchDeployRunnerStatus(options?: { silent?: boolean; timeout?: number }) {
  return getData<DeployRunnerStatus>('/api/deploy/runner/status', undefined, {
    silent: options?.silent,
    timeout: options?.timeout,
  })
}

function isTransientDeployStatusError(err: unknown): boolean {
  if (!err || typeof err !== 'object') return false
  const axiosErr = err as { response?: { status?: number }; code?: string; message?: string }
  const status = axiosErr.response?.status
  if (status === 502 || status === 503 || status === 504) return true
  if (axiosErr.code === 'ECONNABORTED' || axiosErr.code === 'ERR_NETWORK') return true
  const message = axiosErr.message ?? ''
  return message.includes('502') || message.includes('503') || message.includes('Network Error')
}

export function fetchDeployPreflight() {
  return getData<DeployPreflightStatus>('/api/deploy/runner/preflight')
}

function apiBase(): string {
  const base = import.meta.env.VITE_API_BASE || ''
  return base.endsWith('/') ? base.slice(0, -1) : base
}

export function streamDeploy(
  target: 'backend' | 'frontend',
  handlers: DeployStreamHandlers,
): () => void {
  const url = `${apiBase()}/api/deploy/stream?target=${target}`
  const source = new EventSource(url)
  let doneReceived = false
  let disconnectNotified = false

  source.addEventListener('log', (event) => {
    handlers.onLog((event as MessageEvent<string>).data)
  })

  source.addEventListener('done', (event) => {
    doneReceived = true
    try {
      const payload = JSON.parse((event as MessageEvent<string>).data) as {
        success?: boolean
        exitCode?: number
      }
      handlers.onDone(Boolean(payload.success), payload.exitCode ?? -1)
    } catch {
      handlers.onDone(false, -1)
    }
    source.close()
  })

  source.onerror = () => {
    if (doneReceived || disconnectNotified) return
    if (source.readyState === EventSource.CONNECTING) {
      return
    }
    disconnectNotified = true
    handlers.onDisconnect()
  }

  return () => source.close()
}

export async function pollDeployUntilFinished(
  target: 'backend' | 'frontend',
  onTick: (elapsedSeconds: number, running: boolean, transientError?: boolean) => void,
  deployStartedAt: number,
  intervalMs = 5000,
  maxWaitMs = 60 * 60 * 1000,
): Promise<{ success: boolean; exitCode: number; timedOut: boolean; unknown?: boolean }> {
  const startedAt = Date.now()
  let consecutiveErrors = 0
  const maxConsecutiveErrors = 24

  while (Date.now() - startedAt < maxWaitMs) {
    await new Promise((resolve) => setTimeout(resolve, intervalMs))
    const elapsedSeconds = Math.floor((Date.now() - startedAt) / 1000)

    let status: DeployRunnerStatus
    try {
      status = await fetchDeployRunnerStatus({ silent: true, timeout: 30000 })
      consecutiveErrors = 0
    } catch (err) {
      if (!isTransientDeployStatusError(err)) {
        throw err
      }
      consecutiveErrors += 1
      onTick(elapsedSeconds, true, true)
      if (consecutiveErrors >= maxConsecutiveErrors) {
        throw err
      }
      continue
    }

    const stillRunning = Boolean(status.running || status.lastDeploy?.running)
    onTick(elapsedSeconds, stillRunning)

    if (stillRunning) {
      continue
    }

    const last = status.lastDeploy
    if (
      last?.finishedAt &&
      last.finishedAt >= deployStartedAt - 2000 &&
      (!last.target || last.target === target)
    ) {
      return {
        success: Boolean(last.success),
        exitCode: last.exitCode ?? -1,
        timedOut: false,
      }
    }
    return { success: false, exitCode: -1, timedOut: false, unknown: true }
  }
  return { success: false, exitCode: -1, timedOut: true }
}
