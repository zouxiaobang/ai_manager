export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface SysUser {
  id: number
  username: string
  nickname: string
  status: string
  createTime?: string
  updateTime?: string
}

export interface HealthData {
  status: string
  service: string
  redis?: string
  redisError?: string
  mysql?: string
  mysqlError?: string
  appNodeStatus?: string
  dataNodeStatus?: string
  /** 服务器东八区时间，如 2026/06/26 13:53 */
  lastDeployAt?: string
  startedAt?: string
  serverTime?: string
  serverTimeZone?: string
}
