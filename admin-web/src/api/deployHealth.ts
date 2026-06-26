import { fetchHealth } from './health'
import type { HealthData } from './types'
import { deployApiHealthUrl } from '@/data/deploy-center'

export interface DeployHealthCheckResult {
  ok: boolean
  status?: string
  redis?: string
  mysql?: string
  appNodeStatus?: string
  dataNodeStatus?: string
  lastDeployAt?: string
  startedAt?: string
  error?: string
}

function mapHealthData(data: HealthData): DeployHealthCheckResult {
  const ok = data.status === 'UP'
  return {
    ok,
    status: data.status,
    redis: data.redis,
    mysql: data.mysql,
    appNodeStatus: data.appNodeStatus,
    dataNodeStatus: data.dataNodeStatus,
    lastDeployAt: data.lastDeployAt ?? data.startedAt,
    startedAt: data.startedAt,
    error: ok ? undefined : data.status,
  }
}

/** 默认走同源 /api/health（开发代理 / 生产同域）；可传入远程地址检测生产节点 */
export async function checkDeployApiHealth(
  url?: string,
): Promise<DeployHealthCheckResult> {
  if (!url) {
    try {
      const data = await fetchHealth()
      return mapHealthData(data)
    } catch (err) {
      return {
        ok: false,
        error: err instanceof Error ? err.message : 'Network error',
      }
    }
  }

  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: { Accept: 'application/json' },
    })
    if (!response.ok) {
      return { ok: false, error: `HTTP ${response.status}` }
    }
    const body = (await response.json()) as { code?: number; message?: string; data?: HealthData }
    const data = body.data
    if (!data) {
      return { ok: false, error: body.message || 'Empty health data' }
    }
    const result = mapHealthData(data)
    if (body.code !== undefined && body.code !== 0 && body.message !== 'success') {
      return { ...result, ok: false, error: body.message }
    }
    return result
  } catch (err) {
    return {
      ok: false,
      error: err instanceof Error ? err.message : 'Network error',
    }
  }
}

/** 检测生产应用节点（部署中心远程巡检） */
export function checkProductionDeployHealth() {
  return checkDeployApiHealth(deployApiHealthUrl)
}
