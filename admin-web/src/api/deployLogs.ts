import { getData, postData } from './request'

export interface DeployLogEntry {
  lineNumber: number
  timestamp: string
  level: string
  logger: string
  message: string
  raw: string
}

export interface DeployLogTail {
  logFile: string
  fileExists: boolean
  requestedLines: number
  returnedLines: number
  entries: DeployLogEntry[]
}

export interface DeployLogHourlyPoint {
  hour: string
  total: number
  errorCount: number
  warnCount: number
}

export interface DeployLogErrorSummary {
  message: string
  count: number
  lastSeen: string
}

export interface DeployLogStats {
  logFile: string
  todayTotal: number
  yesterdayTotal: number
  todayChangePercent: number | null
  errorCount: number
  yesterdayErrorCount: number
  errorChangePercent: number | null
  warnCount: number
  yesterdayWarnCount: number
  warnChangePercent: number | null
  levelCounts: Record<string, number>
  hourlyTrend: DeployLogHourlyPoint[]
  topErrors: DeployLogErrorSummary[]
}

export interface DeployLogAiInsightItem {
  severity: 'error' | 'warn' | 'info' | 'success'
  text: string
}

export interface DeployLogAiAnalyze {
  summary: string
  insights: string[]
  suggestions: string[]
  items: DeployLogAiInsightItem[]
  analyzedLines: number
  errorCount: number
  warnCount: number
}

export function fetchDeployLogTail(params?: {
  lines?: number
  level?: string
  keyword?: string
}) {
  return getData<DeployLogTail>('/api/deploy/logs/tail', params)
}

export function fetchDeployLogStats(hours = 24) {
  return getData<DeployLogStats>('/api/deploy/logs/stats', { hours })
}

export function analyzeDeployLogs(payload?: { lines?: number; question?: string }) {
  return postData<DeployLogAiAnalyze>('/api/deploy/logs/ai-analyze', payload ?? {}, { timeout: 60000 })
}

function apiBase(): string {
  const base = import.meta.env.VITE_API_BASE || ''
  return base.endsWith('/') ? base.slice(0, -1) : base
}

export interface DeployLogStreamHandlers {
  onReady?: () => void
  onLog: (entry: DeployLogEntry) => void
  onDisconnect: () => void
}

export function streamDeployLogs(
  params: { level?: string; keyword?: string },
  handlers: DeployLogStreamHandlers,
): () => void {
  const query = new URLSearchParams()
  if (params.level && params.level !== 'ALL') {
    query.set('level', params.level)
  }
  if (params.keyword?.trim()) {
    query.set('keyword', params.keyword.trim())
  }
  const suffix = query.toString() ? `?${query.toString()}` : ''
  const source = new EventSource(`${apiBase()}/api/deploy/logs/stream${suffix}`)
  let disconnectNotified = false

  source.addEventListener('ready', () => {
    handlers.onReady?.()
  })

  source.addEventListener('log', (event) => {
    try {
      const entry = JSON.parse((event as MessageEvent<string>).data) as DeployLogEntry
      handlers.onLog(entry)
    } catch {
      // ignore malformed event
    }
  })

  source.onerror = () => {
    if (disconnectNotified) return
    if (source.readyState === EventSource.CONNECTING) {
      return
    }
    disconnectNotified = true
    handlers.onDisconnect()
  }

  return () => source.close()
}
