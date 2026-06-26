import { fetchHealth } from '@/api/health'
import { fetchDeployLogsCheck } from '@/api/deployChecklist'
import type { HealthData } from '@/api/types'
import { deployDataNodeId, deployStepsChecklist } from '@/data/deploy-center'
import { formatServerDateTime, parseServerTime } from '@/utils/deployTimeFormat'

export type DeployCheckStatus = 'pending' | 'running' | 'passed' | 'failed' | 'skipped'

export interface DeployCheckResult {
  id: string
  status: DeployCheckStatus
  message: string
}

function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function checkFrontendAccess(): Promise<Pick<DeployCheckResult, 'status' | 'message'>> {
  try {
    const response = await fetch('/index.html', { cache: 'no-store' })
    const html = await response.text()
    const ok =
      response.ok &&
      (html.includes('id="app"') || html.includes("id='app'") || html.includes('<div id="app">'))
    return {
      status: ok ? 'passed' : 'failed',
      message: ok
        ? 'index.html 可访问且包含 Vue 挂载点'
        : `前端入口异常（HTTP ${response.status}）`,
    }
  } catch (err) {
    return {
      status: 'failed',
      message: err instanceof Error ? err.message : '无法访问 index.html',
    }
  }
}

async function runSingleCheck(
  id: string,
  health: HealthData | null,
): Promise<Pick<DeployCheckResult, 'status' | 'message'>> {
  if (!health) {
    return { status: 'failed', message: '无法获取 /api/health，请确认后端已启动' }
  }

  switch (id) {
    case 'node-status': {
      const ok =
        health.dataNodeStatus === 'UP' && health.mysql === 'UP' && health.redis === 'UP'
      return {
        status: ok ? 'passed' : 'failed',
        message: ok
          ? `数据节点 ${deployDataNodeId} 在线，MySQL / Redis 均为 UP`
          : `数据节点异常（dataNode=${health.dataNodeStatus ?? '—'}, mysql=${health.mysql ?? '—'}, redis=${health.redis ?? '—'}）`,
      }
    }
    case 'service-health': {
      const ok = health.status === 'UP'
      const startedLabel = health.startedAt
        ? formatServerDateTime(parseServerTime(health.startedAt), 'zh-CN')
        : ''
      return {
        status: ok ? 'passed' : 'failed',
        message: ok
          ? `Spring Boot 服务 UP${startedLabel ? `，启动于 ${startedLabel}` : ''}`
          : `服务状态异常：${health.status ?? '—'}`,
      }
    }
    case 'api-check': {
      const ok = health.status === 'UP' && health.service === 'ai-manager-admin'
      return {
        status: ok ? 'passed' : 'failed',
        message: ok
          ? '对外 /api/health 响应正常'
          : 'API 健康检查未通过',
      }
    }
    case 'frontend-access':
      return checkFrontendAccess()
    case 'database': {
      const ok = health.mysql === 'UP'
      return {
        status: ok ? 'passed' : 'failed',
        message: ok
          ? 'MySQL 连接正常'
          : `MySQL 不可用${health.mysqlError ? `：${health.mysqlError}` : ''}`,
      }
    }
    case 'redis-cache': {
      const ok = health.redis === 'UP'
      return {
        status: ok ? 'passed' : 'failed',
        message: ok
          ? 'Redis 连接正常'
          : `Redis 不可用${health.redisError ? `：${health.redisError}` : ''}`,
      }
    }
    case 'logs-alerts': {
      if (import.meta.env.DEV) {
        return { status: 'skipped', message: '本地开发环境，跳过 systemd 日志检查' }
      }
      try {
        const logs = await fetchDeployLogsCheck()
        if (logs.skipped) {
          return { status: 'skipped', message: logs.message }
        }
        return {
          status: logs.ok ? 'passed' : 'failed',
          message: logs.message,
        }
      } catch (err) {
        if (err instanceof Error && err.message === 'LOGS_CHECK_NOT_AVAILABLE') {
          return {
            status: 'skipped',
            message: '日志检查接口不可用，请重新编译并重启后端后重试',
          }
        }
        return {
          status: 'failed',
          message: err instanceof Error ? err.message : '日志检查请求失败',
        }
      }
    }
    default:
      return { status: 'skipped', message: '未配置自动检查' }
  }
}

export async function runDeployChecklistSequential(
  onProgress: (result: DeployCheckResult) => void,
): Promise<DeployCheckResult[]> {
  const results: DeployCheckResult[] = []

  let health: HealthData | null = null
  try {
    health = await fetchHealth()
  } catch {
    health = null
  }

  for (const item of deployStepsChecklist) {
    const running: DeployCheckResult = {
      id: item.id,
      status: 'running',
      message: '检查中…',
    }
    onProgress(running)
    await delay(350)

    const outcome = await runSingleCheck(item.id, health)
    const result: DeployCheckResult = {
      id: item.id,
      status: outcome.status,
      message: outcome.message,
    }
    results.push(result)
    onProgress(result)

    if (item.id === 'api-check' && health === null) {
      try {
        health = await fetchHealth()
      } catch {
        health = null
      }
    }
  }

  return results
}

export function summarizeChecklistResults(results: DeployCheckResult[]) {
  const passed = results.filter((item) => item.status === 'passed').length
  const failed = results.filter((item) => item.status === 'failed').length
  const skipped = results.filter((item) => item.status === 'skipped').length
  const total = results.length
  return { passed, failed, skipped, total }
}
