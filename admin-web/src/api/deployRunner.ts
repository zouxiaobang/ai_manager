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
  onError: (message: string) => void
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

  source.addEventListener('log', (event) => {
    handlers.onLog((event as MessageEvent<string>).data)
  })

  source.addEventListener('done', (event) => {
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
    if (source.readyState === EventSource.CLOSED) return
    handlers.onError('部署日志连接中断')
    source.close()
  }

  return () => source.close()
}
