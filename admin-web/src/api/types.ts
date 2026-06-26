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
  /** 服务器部署产物最近修改时间或进程启动时间（ISO-8601） */
  lastDeployAt?: string
  startedAt?: string
}
