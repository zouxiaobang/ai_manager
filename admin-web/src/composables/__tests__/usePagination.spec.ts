import { describe, it, expect, vi } from 'vitest'
import type { PageResult } from '@/api/pagination'
import { usePagination } from '@/composables/usePagination'

const makeResult = (page: number, pageSize: number, total: number): PageResult<string> => ({
  records: Array.from({ length: pageSize }, (_, i) => `item-${i}`),
  total,
  page,
  pageSize,
})

describe('usePagination', () => {
  it('初始状态为默认分页参数', () => {
    const { page, pageSize, total, records, loading } = usePagination(vi.fn())
    expect(page.value).toBe(1)
    expect(pageSize.value).toBe(20)
    expect(total.value).toBe(0)
    expect(records.value).toEqual([])
    expect(loading.value).toBe(false)
  })

  it('load 用当前页参数请求并写入结果', async () => {
    const loader = vi.fn().mockResolvedValue(makeResult(1, 10, 42))
    const { page, pageSize, total, records, load } = usePagination(loader)

    await load()

    expect(loader).toHaveBeenCalledWith(1, 20)
    expect(page.value).toBe(1)
    expect(pageSize.value).toBe(10)
    expect(total.value).toBe(42)
    expect(records.value).toHaveLength(10)
  })

  it('load(resetPage=true) 重置到第一页', async () => {
    const loader = vi.fn().mockResolvedValue(makeResult(1, 20, 5))
    const { page, load } = usePagination(loader)
    page.value = 3

    await load(true)

    expect(page.value).toBe(1)
  })

  it('onPageChange 更新页码并重新加载', async () => {
    const loader = vi.fn().mockResolvedValue(makeResult(2, 20, 30))
    const { page, onPageChange } = usePagination(loader)

    onPageChange(2)

    expect(page.value).toBe(2)
    // 异步加载完成后应请求第 2 页
    await vi.waitFor(() => expect(loader).toHaveBeenCalledWith(2, 20))
  })

  it('onSizeChange 更新每页条数并重置页码', async () => {
    const loader = vi.fn().mockResolvedValue(makeResult(1, 50, 100))
    const { page, pageSize, onSizeChange } = usePagination(loader)

    page.value = 4
    onSizeChange(50)

    expect(pageSize.value).toBe(50)
    expect(page.value).toBe(1)
    await vi.waitFor(() => expect(loader).toHaveBeenCalledWith(1, 50))
  })

  it('load 期间 loading 状态切换', async () => {
    const loader = vi.fn().mockImplementation(() => new Promise((resolve) => setTimeout(() => resolve(makeResult(1, 20, 1)), 10)))
    const { loading, load } = usePagination(loader)

    const promise = load()
    expect(loading.value).toBe(true)
    await promise
    expect(loading.value).toBe(false)
  })

  it('loader 抛错时 loading 复位且不扩散异常', async () => {
    const loader = vi.fn().mockRejectedValue(new Error('boom'))
    const { loading, load } = usePagination(loader)

    await expect(load()).rejects.toThrow('boom')
    expect(loading.value).toBe(false)
  })
})
