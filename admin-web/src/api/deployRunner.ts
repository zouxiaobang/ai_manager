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
  }
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

export function fetchDeployRunnerStatus() {
  return getData<DeployRunnerStatus>('/api/deploy/runner/status')
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
  onTick: (elapsedSeconds: number, running: boolean) => void,
  deployStartedAt: number,
  intervalMs = 5000,
  maxWaitMs = 60 * 60 * 1000,
): Promise<{ success: boolean; exitCode: number; timedOut: boolean; unknown?: boolean }> {
  const startedAt = Date.now()
  while (Date.now() - startedAt < maxWaitMs) {
    await new Promise((resolve) => setTimeout(resolve, intervalMs))
    const status = await fetchDeployRunnerStatus()
    const elapsedSeconds = Math.floor((Date.now() - startedAt) / 1000)
    onTick(elapsedSeconds, Boolean(status.running))

    if (status.running) {
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
