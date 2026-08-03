import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import type { EcExpressPrice } from '@/api/ecommerce/express'
import { useExpressPriceForm } from '../useExpressPriceForm'

const mocks = vi.hoisted(() => ({
  createExpressPrice: vi.fn(),
  updateExpressPrice: vi.fn(),
  deleteExpressPrice: vi.fn(),
  elMessageSuccess: vi.fn(),
  elMessageWarning: vi.fn(),
  elMessageBoxConfirm: vi.fn(),
  loadStations: vi.fn(),
  loadStationChildren: vi.fn(),
  invalidateExpandDetail: vi.fn(),
  loadExpandDetail: vi.fn(),
}))

vi.mock('@/api/ecommerce/express', () => ({
  createExpressPrice: mocks.createExpressPrice,
  updateExpressPrice: mocks.updateExpressPrice,
  deleteExpressPrice: mocks.deleteExpressPrice,
}))
vi.mock('element-plus', () => ({
  ElMessage: { success: mocks.elMessageSuccess, warning: mocks.elMessageWarning },
  ElMessageBox: { confirm: mocks.elMessageBoxConfirm },
}))
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key }),
}))

function makePrice(partial?: Partial<EcExpressPrice>): EcExpressPrice {
  return { id: 10, stationId: 1, provinceName: '广东', priceW1Kg: 8, ...partial }
}

function setup() {
  const editingId = ref<number | null>(1)
  const prices = ref<EcExpressPrice[]>([])
  const expandedRowKeys = ref<number[]>([])
  const api = useExpressPriceForm({
    editingId,
    prices,
    expandedRowKeys,
    loadStations: mocks.loadStations,
    loadStationChildren: mocks.loadStationChildren,
    invalidateExpandDetail: mocks.invalidateExpandDetail,
    loadExpandDetail: mocks.loadExpandDetail,
  })
  return { api, editingId, prices, expandedRowKeys }
}

describe('useExpressPriceForm 价格表单', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.createExpressPrice.mockResolvedValue(undefined)
    mocks.updateExpressPrice.mockResolvedValue(undefined)
    mocks.deleteExpressPrice.mockResolvedValue(undefined)
    mocks.loadStations.mockResolvedValue(undefined)
    mocks.loadStationChildren.mockResolvedValue(undefined)
    mocks.loadExpandDetail.mockResolvedValue(undefined)
    mocks.elMessageBoxConfirm.mockResolvedValue(undefined)
  })

  describe('openPriceCreate / openPriceEdit / openPriceCopy', () => {
    it('打开新建：重置表单、默认折叠首组', () => {
      const { api } = setup()
      api.priceForm.provinceName = '浙江'
      api.priceForm.priceW1Kg = 9

      api.openPriceCreate()

      expect(api.priceDialogVisible.value).toBe(true)
      expect(api.priceEditingId.value).toBeNull()
      expect(api.priceForm.provinceName).toBe('')
      expect(api.priceForm.priceW1Kg).toBeNull()
      expect(api.priceCollapseActive.value).toEqual(['le1'])
    })

    it('打开编辑：回填省份与档位、展开全部组', () => {
      const { api } = setup()
      api.openPriceEdit(makePrice({ id: 20, provinceName: '浙江', priceW1Kg: 9, priceW2Kg: 18 }))

      expect(api.priceEditingId.value).toBe(20)
      expect(api.priceForm.provinceName).toBe('浙江')
      expect(api.priceForm.priceW1Kg).toBe(9)
      expect(api.priceForm.priceW2Kg).toBe(18)
      expect(api.priceCollapseActive.value).toEqual(['le1', 'mid', 'over3'])
    })

    it('打开复制：复制档位、省份留空待选（无编辑 id）', () => {
      const { api } = setup()
      api.priceForm.provinceName = '广东' // 复制前遗留值应被重置
      api.openPriceCopy(makePrice({ id: 20, provinceName: '浙江', priceW1Kg: 9 }))

      expect(api.priceEditingId.value).toBeNull()
      expect(api.priceForm.provinceName).toBe('')
      expect(api.priceForm.priceW1Kg).toBe(9)
      expect(api.priceCollapseActive.value).toEqual(['le1', 'mid', 'over3'])
    })
  })

  describe('selectRecentRegion / copyPreviousRegionPrice', () => {
    it('selectRecentRegion 回填省份', () => {
      const { api } = setup()
      api.selectRecentRegion('广东')
      expect(api.priceForm.provinceName).toBe('广东')
    })

    it('无历史价格时复制提示并中止', () => {
      const { api } = setup()
      api.copyPreviousRegionPrice()
      expect(mocks.elMessageWarning).toHaveBeenCalledWith('ecommerce.express.noPreviousRegionPrice')
      expect(api.priceForm.priceW1Kg).toBeNull()
    })

    it('复制上一行档位值', () => {
      const { api, prices } = setup()
      prices.value = [
        makePrice({ id: 10, provinceName: '广东', priceW1Kg: 8 }),
        makePrice({ id: 11, provinceName: '浙江', priceW1Kg: 9, priceW2Kg: 20 }),
      ]
      api.copyPreviousRegionPrice()
      expect(api.priceForm.priceW1Kg).toBe(9)
      expect(api.priceForm.priceW2Kg).toBe(20)
      expect(mocks.elMessageSuccess).toHaveBeenCalledWith('ecommerce.express.copyPreviousRegionPriceSuccess')
    })
  })

  describe('onSavePrice 校验与提交', () => {
    it('无编辑站点时中止', async () => {
      const { api, editingId } = setup()
      editingId.value = null
      api.priceForm.provinceName = '广东'
      expect(await api.onSavePrice()).toBe(false)
      expect(mocks.createExpressPrice).not.toHaveBeenCalled()
    })

    it('无省份提示并中止', async () => {
      const { api } = setup()
      await api.onSavePrice()
      expect(mocks.elMessageWarning).toHaveBeenCalledWith('ecommerce.express.provinceRequired')
      expect(mocks.createExpressPrice).not.toHaveBeenCalled()
    })

    it('新建模式：create 后关闭弹窗并刷新子数据', async () => {
      const { api } = setup()
      api.priceForm.provinceName = '广东'
      api.priceForm.priceW1Kg = 8

      expect(await api.onSavePrice()).toBe(true)

      expect(mocks.createExpressPrice).toHaveBeenCalledWith(expect.objectContaining({ stationId: 1, provinceName: '广东', priceW1Kg: 8 }))
      expect(mocks.updateExpressPrice).not.toHaveBeenCalled()
      expect(mocks.elMessageSuccess).toHaveBeenCalledWith('ecommerce.common.saved')
      expect(api.priceDialogVisible.value).toBe(false)
      expect(mocks.loadStationChildren).toHaveBeenCalledWith(1)
      expect(mocks.invalidateExpandDetail).toHaveBeenCalledWith(1)
      expect(mocks.loadStations).toHaveBeenCalled()
    })

    it('编辑模式：update 编辑价格 id', async () => {
      const { api } = setup()
      api.priceEditingId.value = 20
      api.priceForm.provinceName = '广东'

      expect(await api.onSavePrice()).toBe(true)

      expect(mocks.updateExpressPrice).toHaveBeenCalledWith(20, expect.objectContaining({ stationId: 1, provinceName: '广东' }))
      expect(mocks.createExpressPrice).not.toHaveBeenCalled()
    })

    it('编辑行展开时联动刷新详情', async () => {
      const { api, expandedRowKeys } = setup()
      expandedRowKeys.value = [1]
      api.priceForm.provinceName = '广东'

      await api.onSavePrice()

      expect(mocks.loadExpandDetail).toHaveBeenCalledWith(1)
    })

    it('silent 模式不弹成功提示', async () => {
      const { api } = setup()
      api.priceForm.provinceName = '广东'

      await api.onSavePrice({ silent: true })

      expect(mocks.elMessageSuccess).not.toHaveBeenCalled()
      expect(mocks.createExpressPrice).toHaveBeenCalled()
    })
  })

  describe('onDeletePrice', () => {
    it('确认后删除并刷新子数据', async () => {
      const { api } = setup()
      await api.onDeletePrice(makePrice())

      expect(mocks.elMessageBoxConfirm).toHaveBeenCalled()
      expect(mocks.deleteExpressPrice).toHaveBeenCalledWith(10)
      expect(mocks.elMessageSuccess).toHaveBeenCalledWith('ecommerce.common.deleted')
      expect(mocks.loadStationChildren).toHaveBeenCalledWith(1)
      expect(mocks.invalidateExpandDetail).toHaveBeenCalledWith(1)
      expect(mocks.loadStations).toHaveBeenCalled()
    })

    it('无编辑站点时删除后不刷新子数据', async () => {
      const { api, editingId } = setup()
      editingId.value = null
      await api.onDeletePrice(makePrice())
      expect(mocks.loadStationChildren).not.toHaveBeenCalled()
    })
  })

  describe('价格行搜索 / 最近省份', () => {
    it('关键词过滤价格行', () => {
      const { api, prices } = setup()
      prices.value = [
        makePrice({ provinceName: '广东' }),
        makePrice({ id: 11, provinceName: '浙江' }),
      ]
      api.onPriceRegionSearchInput('浙')
      expect(api.filteredEditPrices.value).toHaveLength(1)
      expect(api.filteredEditPrices.value[0].provinceName).toBe('浙江')
    })

    it('空关键词返回全部', () => {
      const { api, prices } = setup()
      prices.value = [makePrice(), makePrice({ id: 11, provinceName: '浙江' })]
      api.priceRegionKeyword.value = ''
      expect(api.filteredEditPrices.value).toHaveLength(2)
    })

    it('最近省份去重取末倒序', () => {
      const { api, prices } = setup()
      prices.value = [
        makePrice({ provinceName: '广东' }),
        makePrice({ id: 11, provinceName: '浙江' }),
        makePrice({ id: 12, provinceName: '广东' }),
      ]
      expect(api.recentPriceRegions.value).toEqual(['浙江', '广东'])
    })

    it('resetPriceRegionKeyword 清空搜索词', () => {
      const { api } = setup()
      api.priceRegionKeyword.value = '浙'
      api.resetPriceRegionKeyword()
      expect(api.priceRegionKeyword.value).toBe('')
    })
  })
})
