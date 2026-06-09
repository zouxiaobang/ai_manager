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
}
