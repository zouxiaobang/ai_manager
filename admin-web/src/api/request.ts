import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from './types'

/**
 * Axios 实例
 * 全局统一配置 baseURL 和超时时间
 */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 15000,
})

/**
 * 响应拦截器
 * 统一处理 ApiResult 格式的响应：
 * - code=0：成功，正常返回
 * - code≠0：失败，弹出错误提示并 reject
 * 
 * 支持静默错误模式（X-Silent-Error 请求头），
 * 静默模式下失败时不弹出全局提示，由调用方自行处理。
 */
request.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body && typeof body.code === 'number' && body.code !== 0) {
      const silent = Boolean(response.config?.headers?.['X-Silent-Error'])
      if (!silent) {
        ElMessage.error(body.message || '请求失败')
      }
      return Promise.reject(new Error(body.message || '请求失败'))
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

/**
 * GET 请求封装
 * 自动解析 ApiResult<T>，直接返回 data 字段
 * 
 * @param url - 请求地址
 * @param params - URL 查询参数
 * @param options - 配置选项（silent: 静默错误, timeout: 超时时间）
 * @returns 响应数据（ApiResult.data）
 */
export async function getData<T>(
  url: string,
  params?: Record<string, unknown>,
  options?: { silent?: boolean; timeout?: number },
): Promise<T> {
  const response = await request.get<ApiResult<T>>(url, {
    params,
    timeout: options?.timeout,
    headers: options?.silent ? { 'X-Silent-Error': '1' } : undefined,
  })
  return response.data.data
}

/**
 * POST 请求封装
 * 自动解析 ApiResult<T>，直接返回 data 字段
 * 
 * @param url - 请求地址
 * @param data - 请求体数据
 * @param options - 配置选项（silent: 静默错误, timeout: 超时时间）
 * @returns 响应数据（ApiResult.data）
 */
export async function postData<T>(
  url: string,
  data?: unknown,
  options?: { timeout?: number; silent?: boolean },
): Promise<T> {
  const response = await request.post<ApiResult<T>>(url, data, {
    timeout: options?.timeout,
    headers: options?.silent ? { 'X-Silent-Error': '1' } : undefined,
  })
  return response.data.data
}

/**
 * PUT 请求封装
 * 自动解析 ApiResult<T>，直接返回 data 字段
 * 
 * @param url - 请求地址
 * @param data - 请求体数据
 * @returns 响应数据（ApiResult.data）
 */
export async function putData<T>(url: string, data?: unknown): Promise<T> {
  const response = await request.put<ApiResult<T>>(url, data)
  return response.data.data
}

/**
 * DELETE 请求封装
 * 
 * @param url - 请求地址
 */
export async function deleteData(url: string): Promise<void> {
  await request.delete(url)
}

export default request
