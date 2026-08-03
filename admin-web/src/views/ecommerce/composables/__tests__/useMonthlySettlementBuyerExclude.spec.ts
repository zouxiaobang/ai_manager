import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { SettlementBuyerExclude } from '@/api/ecommerce/monthlySettlement'
import { useMonthlySettlementBuyerExclude, type MonthlySettlementBuyerExcludeDeps } from '../useMonthlySettlementBuyerExclude'

const mocks = vi.hoisted(() => {
  return {
    fetchSettlementBuyerExcludes: vi.fn(),
    saveSettlementBuyerExclude: vi.fn(),
    deleteSettlementBuyerExclude: vi.fn(),
    elMessageSuccess: vi.fn(),
    elMessageWarning: vi.fn(),
  }
})

vi.mock('@/api/ecommerce/monthlySettlement', () => ({
  fetchSettlementBuyerExcludes: mocks.fetchSettlementBuyerExcludes,
  saveSettlementBuyerExclude: mocks.saveSettlementBuyerExclude,
  deleteSettlementBuyerExclude: mocks.deleteSettlementBuyerExclude,
}))
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key }),
}))
vi.mock('element-plus', () => ({
  ElMessage: { success: mocks.elMessageSuccess, warning: mocks.elMessageWarning },
}))

function makeExclude(id: number, partial?: Partial<SettlementBuyerExclude>): SettlementBuyerExclude {
  return {
    id,
    buyerName: `买家${id}`,
    remark: '',
    ...partial,
  }
}

function setup(getShop: MonthlySettlementBuyerExcludeDeps['getShop'] = () => undefined) {
  return useMonthlySettlementBuyerExclude({ getShop })
}

describe('useMonthlySettlementBuyerExclude 买家排除域', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.fetchSettlementBuyerExcludes.mockResolvedValue([])
    mocks.saveSettlementBuyerExclude.mockResolvedValue(undefined)
    mocks.deleteSettlementBuyerExclude.mockResolvedValue(undefined)
  })

  describe('初始状态', () => {
    it('对话框默认关闭、快照计数为 0', () => {
      const api = setup()
      expect(api.buyerExcludeVisible.value).toBe(false)
      expect(api.buyerExcludeCount.value).toBe(0)
      expect(api.buyerExcludesSnapshot.value).toEqual([])
      expect(api.lastBuyerExcludeOpAt.value).toBeNull()
      expect(api.buyerExcludes.value).toEqual([])
    })
  })

  describe('buyerExcludeStats', () => {
    it('区分全店铺与指定店铺排除数', () => {
      const api = setup()
      api.buyerExcludes.value = [
        makeExclude(1),
        makeExclude(2, { shopId: 3 }),
        makeExclude(3, { shopId: 4 }),
      ]
      expect(api.buyerExcludeStats.value).toEqual({ total: 3, globalCount: 1, shopCount: 2 })
    })
  })

  describe('filteredBuyerExcludes', () => {
    it('空关键字返回全量', () => {
      const api = setup()
      api.buyerExcludes.value = [makeExclude(1)]
      api.excludeSearchKeyword.value = '  '
      expect(api.filteredBuyerExcludes.value).toHaveLength(1)
    })

    it('按买家名/店铺名/备注忽略大小写过滤', () => {
      const api = setup()
      api.buyerExcludes.value = [
        makeExclude(1, { buyerName: 'ZhangSan' }),
        makeExclude(2, { shopName: '淘宝店' }),
        makeExclude(3, { remark: '重点客户' }),
      ]
      api.excludeSearchKeyword.value = '淘宝'
      expect(api.filteredBuyerExcludes.value.map((x) => x.id)).toEqual([2])

      api.excludeSearchKeyword.value = 'zhangsan'
      expect(api.filteredBuyerExcludes.value.map((x) => x.id)).toEqual([1])

      api.excludeSearchKeyword.value = '重点'
      expect(api.filteredBuyerExcludes.value.map((x) => x.id)).toEqual([3])
    })
  })

  describe('getExcludeShopIconMeta', () => {
    it('无 shopId 时按名称回退解析', () => {
      const api = setup()
      const meta = api.getExcludeShopIconMeta(makeExclude(1, { shopName: '散客' }))
      expect(meta.src).toBeDefined()
    })

    it('有 shopId 时经注入的 getShop 解析店铺', () => {
      const getShop = vi.fn(() => ({ id: 3, name: '店铺C', platformName: '淘宝', platformCode: 'taobao' }) as never)
      const api = setup(getShop)
      const meta = api.getExcludeShopIconMeta(makeExclude(1, { shopId: 3 }))
      expect(getShop).toHaveBeenCalledWith(3)
      expect(meta.src).toBeDefined()
    })
  })

  describe('onBuyerExcludeDialogClosed', () => {
    it('清空表单与搜索条件', () => {
      const api = setup()
      api.excludeFormShopId.value = 1
      api.excludeFormBuyerName.value = '张三'
      api.excludeFormRemark.value = '备注'
      api.excludeSearchKeyword.value = '搜'
      api.onBuyerExcludeDialogClosed()
      expect(api.excludeFormShopId.value).toBeUndefined()
      expect(api.excludeFormBuyerName.value).toBe('')
      expect(api.excludeFormRemark.value).toBe('')
      expect(api.excludeSearchKeyword.value).toBe('')
    })
  })

  describe('touchBuyerExcludeOpTime', () => {
    it('写入 ISO 时间戳', () => {
      const api = setup()
      api.touchBuyerExcludeOpTime()
      expect(api.lastBuyerExcludeOpAt.value).not.toBeNull()
      expect(api.lastBuyerExcludeOpAt.value).toContain('T')
    })
  })

  describe('loadBuyerExcludes', () => {
    it('加载列表并同步快照计数，loading 复位', async () => {
      const api = setup()
      mocks.fetchSettlementBuyerExcludes.mockResolvedValue([makeExclude(1)])
      const promise = api.loadBuyerExcludes()
      expect(api.loadingExcludes.value).toBe(true)
      await promise
      expect(api.loadingExcludes.value).toBe(false)
      expect(api.buyerExcludes.value).toHaveLength(1)
      expect(api.buyerExcludesSnapshot.value).toHaveLength(1)
      expect(api.buyerExcludeCount.value).toBe(1)
    })
  })

  describe('loadSnapshot / clearSnapshot', () => {
    it('loadSnapshot 更新快照但不触发表单 loading', async () => {
      const api = setup()
      mocks.fetchSettlementBuyerExcludes.mockResolvedValue([makeExclude(1)])
      const result = await api.loadSnapshot()
      expect(result).toHaveLength(1)
      expect(api.buyerExcludeCount.value).toBe(1)
      expect(api.loadingExcludes.value).toBe(false)
    })

    it('clearSnapshot 清空快照', () => {
      const api = setup()
      api.buyerExcludesSnapshot.value = [makeExclude(1)]
      api.buyerExcludeCount.value = 1
      api.clearSnapshot()
      expect(api.buyerExcludesSnapshot.value).toEqual([])
      expect(api.buyerExcludeCount.value).toBe(0)
    })
  })

  describe('addBuyerExclude', () => {
    it('买家名必填：空名 warning 且不调用保存', async () => {
      const api = setup()
      await api.addBuyerExclude()
      expect(mocks.elMessageWarning).toHaveBeenCalled()
      expect(mocks.saveSettlementBuyerExclude).not.toHaveBeenCalled()
    })

    it('保存成功后清空表单、刷新列表并记录操作时间', async () => {
      const api = setup()
      mocks.fetchSettlementBuyerExcludes.mockResolvedValue([makeExclude(1)])
      api.excludeFormShopId.value = 3
      api.excludeFormBuyerName.value = ' 张三 '
      api.excludeFormRemark.value = ' 备注 '
      await api.addBuyerExclude()
      expect(mocks.saveSettlementBuyerExclude).toHaveBeenCalledWith({
        shopId: 3,
        buyerName: '张三',
        remark: '备注',
        enabled: 1,
      })
      expect(api.excludeFormBuyerName.value).toBe('')
      expect(api.excludeFormRemark.value).toBe('')
      expect(mocks.fetchSettlementBuyerExcludes).toHaveBeenCalled()
      expect(api.lastBuyerExcludeOpAt.value).not.toBeNull()
      expect(mocks.elMessageSuccess).toHaveBeenCalled()
    })

    it('失败时 saving 复位', async () => {
      const api = setup()
      mocks.saveSettlementBuyerExclude.mockRejectedValue(new Error('boom'))
      api.excludeFormBuyerName.value = '张三'
      await expect(api.addBuyerExclude()).rejects.toThrow('boom')
      expect(api.savingBuyerExclude.value).toBe(false)
    })
  })

  describe('removeBuyerExclude', () => {
    it('删除后刷新列表并记录操作时间', async () => {
      const api = setup()
      mocks.fetchSettlementBuyerExcludes.mockResolvedValue([makeExclude(2)])
      await api.removeBuyerExclude(1)
      expect(mocks.deleteSettlementBuyerExclude).toHaveBeenCalledWith(1)
      expect(api.lastBuyerExcludeOpAt.value).not.toBeNull()
      expect(api.buyerExcludes.value.map((x) => x.id)).toEqual([2])
    })
  })
})
