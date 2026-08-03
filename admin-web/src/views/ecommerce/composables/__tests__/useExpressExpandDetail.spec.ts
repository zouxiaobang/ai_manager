import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import type { EcExpressStation } from '@/api/ecommerce/express'
import { useExpressExpandDetail } from '../useExpressExpandDetail'

/** 站点 fixture */
function makeStation(partial?: Partial<EcExpressStation>): EcExpressStation {
  return { id: 1, name: '站点甲', ...partial }
}

function setup() {
  const records = ref<EcExpressStation[]>([])
  const fetchExpressStation = vi.fn()
  const api = useExpressExpandDetail({ records, fetchExpressStation })
  return { api, records, fetchExpressStation }
}

describe('useExpressExpandDetail 展开行详情', () => {
  describe('loadExpandDetail', () => {
    it('拉取详情入缓存并回写行级计数', async () => {
      const { api, records, fetchExpressStation } = setup()
      records.value = [makeStation()]
      const detail = makeStation({ prices: [{ id: 10, stationId: 1, provinceName: '广东' }], notices: [{ id: 1, stationId: 1, content: 'x' }] })
      fetchExpressStation.mockResolvedValue(detail)

      await api.loadExpandDetail(1)

      expect(fetchExpressStation).toHaveBeenCalledWith(1)
      expect(api.getExpandDetail(1)).toEqual(detail)
      expect(records.value[0].priceCount).toBe(1)
      expect(records.value[0].noticeCount).toBe(1)
      expect(api.isExpandLoading(1)).toBe(false)
    })

    it('已缓存或加载中不重复请求', async () => {
      const { api, fetchExpressStation } = setup()
      fetchExpressStation.mockResolvedValue(makeStation())

      await api.loadExpandDetail(1)
      expect(fetchExpressStation).toHaveBeenCalledTimes(1)
      await api.loadExpandDetail(1)
      expect(fetchExpressStation).toHaveBeenCalledTimes(1)
    })

    it('加载失败清除 loading 状态并向调用方抛出', async () => {
      const { api, fetchExpressStation } = setup()
      fetchExpressStation.mockRejectedValue(new Error('boom'))

      await expect(api.loadExpandDetail(1)).rejects.toThrow('boom')

      expect(api.isExpandLoading(1)).toBe(false)
    })
  })

  describe('invalidateExpandDetail', () => {
    it('删除缓存并重建 Map 触发响应式', async () => {
      const { api, fetchExpressStation } = setup()
      fetchExpressStation.mockResolvedValue(makeStation())
      await api.loadExpandDetail(1)
      expect(api.getExpandDetail(1)).toBeDefined()

      api.invalidateExpandDetail(1)
      expect(api.getExpandDetail(1)).toBeUndefined()
    })
  })

  describe('onExpandChange', () => {
    it('展开时记录 key 并加载详情', async () => {
      const { api, records, fetchExpressStation } = setup()
      records.value = [makeStation()]
      fetchExpressStation.mockResolvedValue(makeStation())

      const row = makeStation()
      await api.onExpandChange(row, [row])

      expect(api.expandedRowKeys.value).toEqual([1])
      expect(fetchExpressStation).toHaveBeenCalledWith(1)
    })

    it('收起时仅更新 key 不加载', async () => {
      const { api, fetchExpressStation } = setup()
      api.expandedRowKeys.value = [1]
      const row = makeStation()

      await api.onExpandChange(row, [])

      expect(api.expandedRowKeys.value).toEqual([])
      expect(fetchExpressStation).not.toHaveBeenCalled()
    })
  })

  describe('rowClassName', () => {
    it('展开行返回高亮样式', () => {
      const { api } = setup()
      api.expandedRowKeys.value = [1]
      expect(api.rowClassName({ row: makeStation() })).toBe('express-station-row is-expanded')
      api.expandedRowKeys.value = []
      expect(api.rowClassName({ row: makeStation() })).toBe('express-station-row')
    })
  })
})
