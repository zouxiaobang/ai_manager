import { getData } from './request'

export interface DeployVersionRecord {
  id: string
  target: 'backend' | 'frontend' | string
  success: boolean
  exitCode: number
  deployMode?: string
  startedAt: number
  finishedAt: number
  durationMs: number
  deployedAt?: string
  gitCommit?: string
  gitMessage?: string
  gitBranch?: string
  gitAuthor?: string
  projectRoot?: string
}

export function fetchDeployVersions(limit = 50) {
  return getData<DeployVersionRecord[]>('/api/deploy/versions', { limit })
}
