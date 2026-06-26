import axios from 'axios'
import { getData } from './request'

export interface DeployLogsCheckResult {
  ok: boolean
  skipped?: boolean
  message: string
}

/** 检查清单专用：失败时不弹全局错误提示 */
export async function fetchDeployLogsCheck(): Promise<DeployLogsCheckResult> {
  try {
    return await getData<DeployLogsCheckResult>('/api/deploy/checklist/logs', undefined, {
      silent: true,
    })
  } catch (err) {
    if (axios.isAxiosError(err) && err.response?.status === 404) {
      throw new Error('LOGS_CHECK_NOT_AVAILABLE')
    }
    throw err
  }
}
