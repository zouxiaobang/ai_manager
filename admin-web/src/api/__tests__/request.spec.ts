import { describe, expect, it, vi, beforeEach } from 'vitest'

// mock axios：通过模块命名空间暴露实例与响应拦截器处理器，供断言使用
vi.mock('axios', () => {
  const handlers: { fulfilled: ((r: unknown) => unknown) | undefined; rejected: ((e: unknown) => unknown) | undefined } = {
    fulfilled: undefined,
    rejected: undefined,
  }
  const instance = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: {
      response: {
        use: vi.fn((fulfilled: (r: unknown) => unknown, rejected: (e: unknown) => unknown) => {
          handlers.fulfilled = fulfilled
          handlers.rejected = rejected
        }),
      },
    },
  }
  return {
    __mockState: { instance, handlers },
    default: { create: vi.fn(() => instance) },
  }
})

// element-plus 仅用于错误提示，mock 掉避免 jsdom 渲染
vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn() },
}))

import * as axiosNs from 'axios'
import { ElMessage } from 'element-plus'
import { deleteData, getData, postData, putData } from '@/api/request'

type MockInstance = { get: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn>; put: ReturnType<typeof vi.fn>; delete: ReturnType<typeof vi.fn> }
type MockHandlers = { fulfilled: (r: unknown) => unknown; rejected: (e: unknown) => unknown }

function mockState() {
  return (axiosNs as unknown as { __mockState: { instance: MockInstance; handlers: MockHandlers } }).__mockState
}

function makeResponse(data: unknown, configHeaders: Record<string, unknown> = {}) {
  return { data, config: { headers: configHeaders } }
}

describe('request 响应拦截器', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('code=0 时放行响应', async () => {
    const { handlers } = mockState()
    const response = makeResponse({ code: 0, data: 'ok' })
    const result = handlers.fulfilled(response)
    expect(result).toBe(response)
    expect(ElMessage.error).not.toHaveBeenCalled()
  })

  it('code!=0 时弹出错误提示并 reject', async () => {
    const { handlers } = mockState()
    const response = makeResponse({ code: 400, message: '参数错误' })
    await expect(handlers.fulfilled(response)).rejects.toThrow('参数错误')
    expect(ElMessage.error).toHaveBeenCalledWith('参数错误')
  })

  it('X-Silent-Error 请求头下失败不弹提示', async () => {
    const { handlers } = mockState()
    const response = makeResponse({ code: 500, message: '内部错误' }, { 'X-Silent-Error': '1' })
    await expect(handlers.fulfilled(response)).rejects.toThrow('内部错误')
    expect(ElMessage.error).not.toHaveBeenCalled()
  })

  it('网络错误时弹提示并 reject', async () => {
    const { handlers } = mockState()
    const err = new Error('network down')
    await expect(handlers.rejected(err)).rejects.toThrow('network down')
    expect(ElMessage.error).toHaveBeenCalledWith('network down')
  })
})

describe('request 封装方法', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getData 解包 ApiResult.data', async () => {
    const { instance } = mockState()
    instance.get.mockResolvedValue({ data: { code: 0, data: { id: 1 } } })
    const result = await getData<{ id: number }>('/api/x')
    expect(result).toEqual({ id: 1 })
  })

  it('postData 传 JSON body 并解包', async () => {
    const { instance } = mockState()
    instance.post.mockResolvedValue({ data: { code: 0, data: 'created' } })
    const result = await postData<string>('/api/x', { name: 'a' })
    expect(instance.post).toHaveBeenCalledWith('/api/x', { name: 'a' }, expect.anything())
    expect(result).toBe('created')
  })

  it('putData 支持 silent 头', async () => {
    const { instance } = mockState()
    instance.put.mockResolvedValue({ data: { code: 0, data: null } })
    await putData<void>('/api/x', {}, { silent: true })
    expect(instance.put).toHaveBeenCalledWith(
      '/api/x',
      {},
      expect.objectContaining({ headers: { 'X-Silent-Error': '1' } }),
    )
  })

  it('deleteData 支持 silent 头', async () => {
    const { instance } = mockState()
    instance.delete.mockResolvedValue({ data: { code: 0 } })
    await deleteData('/api/x', { silent: true })
    expect(instance.delete).toHaveBeenCalledWith(
      '/api/x',
      expect.objectContaining({ headers: { 'X-Silent-Error': '1' } }),
    )
  })
})
