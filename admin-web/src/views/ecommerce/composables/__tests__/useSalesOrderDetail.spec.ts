import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { EcSalesOrder } from '@/api/ecommerce/salesOrder'
import { useSalesOrderDetail } from '../useSalesOrderDetail'

const mocks = vi.hoisted(() => {
  const routeQuery: Record<string, unknown> = {}
  return {
    routeQuery,
    replaceMock: vi.fn(),
    fetchSalesOrder: vi.fn(),
    updateSalesOrder: vi.fn(),
    deleteSalesOrder: vi.fn(),
    elMessageSuccess: vi.fn(),
    elMessageWarning: vi.fn(),
    confirmMock: vi.fn(),
    getOrderShopIconMeta: vi.fn(() => ({ src: 'icon.png', isCustomAvatar: false })),
    loadLinkSkuOptions: vi.fn(),
    load: vi.fn(),
  }
})

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: mocks.routeQuery, path: '/orders' }),
  useRouter: () => ({ replace: mocks.replaceMock }),
}))
vi.mock('@/api/ecommerce/salesOrder', () => ({
  fetchSalesOrder: mocks.fetchSalesOrder,
  updateSalesOrder: mocks.updateSalesOrder,
  deleteSalesOrder: mocks.deleteSalesOrder,
}))
vi.mock('element-plus', () => ({
  ElMessage: { success: mocks.elMessageSuccess, warning: mocks.elMessageWarning },
  ElMessageBox: { confirm: mocks.confirmMock },
}))
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key }),
}))

/** 构造一个最小订单对象 */
function makeOrder(overrides: Partial<EcSalesOrder> = {}): EcSalesOrder {
  return {
    id: 1,
    shopId: 1,
    shopName: '旗舰店',
    platformName: '淘宝',
    orderNo: 'SO-1',
    orderTime: '2026-08-01 10:00:00',
    receivedAmount: 100,
    profitAmount: 20,
    status: 'DRAFT',
    source: 'MANUAL',
    lines: [],
    ...overrides,
  } as EcSalesOrder
}

describe('useSalesOrderDetail 详情抽屉', () => {
  function setup() {
    return useSalesOrderDetail({
      getOrderShopIconMeta: mocks.getOrderShopIconMeta,
      loadLinkSkuOptions: mocks.loadLinkSkuOptions,
      load: mocks.load,
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mocks.routeQuery.orderId = undefined
    mocks.fetchSalesOrder.mockResolvedValue(makeOrder())
    mocks.updateSalesOrder.mockResolvedValue(undefined)
    mocks.deleteSalesOrder.mockResolvedValue(undefined)
    mocks.confirmMock.mockResolvedValue('confirm')
  })

  describe('openDetail / 行高亮', () => {
    it('openDetail 设置 detailId 并打开抽屉', () => {
      const api = setup()
      api.openDetail(5)
      expect(api.detailId.value).toBe(5)
      expect(api.detailVisible.value).toBe(true)
    })

    it('当前打开行返回 is-selected', () => {
      const api = setup()
      api.openDetail(3)
      expect(api.orderRowClassName({ row: makeOrder({ id: 3 }) })).toBe('is-selected')
    })

    it('其他行与未打开时不加高亮', () => {
      const api = setup()
      expect(api.orderRowClassName({ row: makeOrder({ id: 1 }) })).toBe('')
      api.openDetail(3)
      expect(api.orderRowClassName({ row: makeOrder({ id: 2 }) })).toBe('')
    })
  })

  describe('openDetailFromRouteQuery', () => {
    it('合法 orderId 直达打开并清理 query', () => {
      mocks.routeQuery.orderId = '7'
      const api = setup()
      api.openDetailFromRouteQuery()
      expect(api.detailId.value).toBe(7)
      expect(api.detailVisible.value).toBe(true)
      expect(mocks.replaceMock).toHaveBeenCalledWith({ path: '/orders', query: {} })
    })

    it('数组形式 orderId 取首项', () => {
      mocks.routeQuery.orderId = ['8', '9']
      const api = setup()
      api.openDetailFromRouteQuery()
      expect(api.detailId.value).toBe(8)
    })

    it('缺失或非法 orderId 不打开', () => {
      const api = setup()
      api.openDetailFromRouteQuery()
      expect(api.detailVisible.value).toBe(false)

      mocks.routeQuery.orderId = 'abc'
      api.openDetailFromRouteQuery()
      expect(api.detailVisible.value).toBe(false)

      mocks.routeQuery.orderId = '-1'
      api.openDetailFromRouteQuery()
      expect(api.detailVisible.value).toBe(false)
    })
  })

  describe('detailShopIconMeta', () => {
    it('委托 getOrderShopIconMeta 解析', () => {
      const api = setup()
      api.detail.value = makeOrder()
      expect(api.detailShopIconMeta.value).toEqual({ src: 'icon.png', isCustomAvatar: false })
      expect(mocks.getOrderShopIconMeta).toHaveBeenCalledWith(api.detail.value)
    })
  })

  describe('loadDetail', () => {
    it('成功加载填充详情', async () => {
      mocks.fetchSalesOrder.mockResolvedValue(makeOrder({ id: 9 }))
      const api = setup()
      api.detailId.value = 9
      await api.loadDetail()
      expect(mocks.fetchSalesOrder).toHaveBeenCalledWith(9)
      expect(api.detail.value?.id).toBe(9)
      expect(api.detailLoading.value).toBe(false)
    })

    it('无 detailId 直接返回', async () => {
      const api = setup()
      await api.loadDetail()
      expect(mocks.fetchSalesOrder).not.toHaveBeenCalled()
    })

    it('请求失败仍复位 loading', async () => {
      mocks.fetchSalesOrder.mockRejectedValue(new Error('boom'))
      const api = setup()
      api.detailId.value = 1
      await expect(api.loadDetail()).rejects.toThrow('boom')
      expect(api.detailLoading.value).toBe(false)
    })
  })

  describe('onSaveDetail', () => {
    it('保存成功递增 commitKey 并刷新列表与详情', async () => {
      const api = setup()
      api.detailId.value = 2
      const payload = { shopId: 1 } as Parameters<typeof api.onSaveDetail>[0]
      await api.onSaveDetail(payload)
      expect(mocks.updateSalesOrder).toHaveBeenCalledWith(2, payload)
      expect(mocks.elMessageSuccess).toHaveBeenCalled()
      expect(api.detailSaveCommitKey.value).toBe(1)
      expect(mocks.load).toHaveBeenCalled()
      expect(mocks.fetchSalesOrder).toHaveBeenCalledWith(2)
    })

    it('无 detailId 不提交', async () => {
      const api = setup()
      await api.onSaveDetail({ shopId: 1 } as Parameters<typeof api.onSaveDetail>[0])
      expect(mocks.updateSalesOrder).not.toHaveBeenCalled()
    })

    it('保存失败复位 saving', async () => {
      mocks.updateSalesOrder.mockRejectedValue(new Error('boom'))
      const api = setup()
      api.detailId.value = 2
      await expect(api.onSaveDetail({ shopId: 1 } as Parameters<typeof api.onSaveDetail>[0])).rejects.toThrow('boom')
      expect(api.detailSaving.value).toBe(false)
    })
  })

  describe('onDeleteDetailOrder', () => {
    it('非手工且非草稿订单禁止删除', async () => {
      const api = setup()
      api.detail.value = makeOrder({ source: 'IMPORT', status: 'PAID' })
      await api.onDeleteDetailOrder()
      expect(mocks.elMessageWarning).toHaveBeenCalledWith('ecommerce.salesOrder.deleteNotAllowed')
      expect(mocks.confirmMock).not.toHaveBeenCalled()
      expect(mocks.deleteSalesOrder).not.toHaveBeenCalled()
    })

    it('确认后删除并关闭抽屉、刷新列表', async () => {
      const api = setup()
      api.detail.value = makeOrder({ id: 4 })
      await api.onDeleteDetailOrder()
      expect(mocks.confirmMock).toHaveBeenCalled()
      expect(mocks.deleteSalesOrder).toHaveBeenCalledWith(4)
      expect(mocks.elMessageSuccess).toHaveBeenCalledWith('ecommerce.common.deleted')
      expect(api.detailVisible.value).toBe(false)
      expect(api.detail.value).toBeNull()
      expect(api.detailId.value).toBeNull()
      expect(mocks.load).toHaveBeenCalled()
    })

    it('取消确认不删除', async () => {
      mocks.confirmMock.mockRejectedValue(new Error('cancel'))
      const api = setup()
      api.detail.value = makeOrder({ id: 4 })
      await expect(api.onDeleteDetailOrder()).rejects.toThrow('cancel')
      expect(mocks.deleteSalesOrder).not.toHaveBeenCalled()
    })
  })

  describe('onDetailShopChange', () => {
    it('切换店铺后重载链接 SKU 选项', () => {
      const api = setup()
      api.onDetailShopChange(3)
      expect(mocks.loadLinkSkuOptions).toHaveBeenCalledWith(3)
    })

    it('shopId 为 0 不加载', () => {
      const api = setup()
      api.onDetailShopChange(0)
      expect(mocks.loadLinkSkuOptions).not.toHaveBeenCalled()
    })
  })
})
