import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from './types'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 15000,
})

request.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body && typeof body.code === 'number' && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return response
  },
  (error) => {
    const silent = Boolean(error.config?.headers?.['X-Silent-Error'])
    if (!silent) {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  },
)

export async function getData<T>(
  url: string,
  params?: Record<string, unknown>,
  options?: { silent?: boolean },
): Promise<T> {
  const response = await request.get<ApiResult<T>>(url, {
    params,
    headers: options?.silent ? { 'X-Silent-Error': '1' } : undefined,
  })
  return response.data.data
}

export async function postData<T>(
  url: string,
  data?: unknown,
  options?: { timeout?: number },
): Promise<T> {
  const response = await request.post<ApiResult<T>>(url, data, { timeout: options?.timeout })
  return response.data.data
}

export async function putData<T>(url: string, data?: unknown): Promise<T> {
  const response = await request.put<ApiResult<T>>(url, data)
  return response.data.data
}

export async function deleteData(url: string): Promise<void> {
  await request.delete(url)
}

export default request
