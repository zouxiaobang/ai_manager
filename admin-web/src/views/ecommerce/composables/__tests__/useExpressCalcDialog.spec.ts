import { describe, expect, it, vi } from 'vitest'
import type { EcExpressStation } from '@/api/ecommerce/express'
import { useExpressCalcDialog } from '../useExpressCalcDialog'

/** 站点 fixture：默认快递 + 价格矩阵 */
function makeStation(partial?: Partial<EcExpressStation>): EcExpressStation {
  return {
    id: 1,
    name: '站点甲',
    isDefault: true,
    labelPrice: 2,
    prices: [{ id: 10, stationId: 1, provinceName: '广东', priceW1Kg: 8 }],
    ...partial,
  }
}

function setup() {
  const fetchExpressStations = vi.fn()
  const fetchExpressStation = vi.fn()
  const api = useExpressCalcDialog({ fetchExpressStations, fetchExpressStation })
  return { api, fetchExpressStations, fetchExpressStation }
}

describe('useExpressCalcDialog 运费试算弹窗', () => {
  describe('openCalcDialog', () => {
    it('默认选中默认快递并预加载详情，预置广东省份', async () => {
      const { api, fetchExpressStations, fetchExpressStation } = setup()
      const station = makeStation()
      fetchExpressStations.mockResolvedValue({ records: [station] })
      fetchExpressStation.mockResolvedValue(station)

      await api.openCalcDialog()

      expect(fetchExpressStations).toHaveBeenCalled()
      expect(fetchExpressStation).toHaveBeenCalledWith(1)
      expect(api.calcStations.value).toEqual([station])
      expect(api.calcStationId.value).toBe(1)
      expect(api.calcProvince.value).toBe('广东')
      expect(api.calcDialogVisible.value).toBe(true)
    })

    it('无默认快递时选中第一个站点', async () => {
      const { api, fetchExpressStations, fetchExpressStation } = setup()
      const station = makeStation({ id: 2, isDefault: false, name: '站点乙' })
      fetchExpressStations.mockResolvedValue({ records: [station] })
      fetchExpressStation.mockResolvedValue(station)

      await api.openCalcDialog()

      expect(api.calcStationId.value).toBe(2)
      expect(api.calcProvince.value).toBe('广东')
    })

    it('拉取失败清空站点但弹窗仍打开', async () => {
      const { api, fetchExpressStations } = setup()
      fetchExpressStations.mockRejectedValue(new Error('boom'))

      await api.openCalcDialog()

      expect(api.calcStations.value).toEqual([])
      expect(api.calcDialogVisible.value).toBe(true)
    })

    it('打开时重置上一次的计算状态', async () => {
      const { api, fetchExpressStations } = setup()
      fetchExpressStations.mockResolvedValue({ records: [] })
      api.calcProvince.value = '浙江'
      api.calcResult.value = { volumetricWeight: 0, billingWeight: 1, tier: '≤1kg', freight: 8, labelPrice: 0, total: 8 }

      await api.openCalcDialog()

      expect(api.calcStationId.value).toBeNull()
      expect(api.calcProvince.value).toBe('')
      expect(api.calcResult.value).toBeNull()
      expect(api.calcLength.value).toBeNull()
    })
  })

  describe('calcStationDetail / availableProvinces', () => {
    it('未选站点返回 null 与空省份', () => {
      const { api } = setup()
      expect(api.calcStationDetail.value).toBeNull()
      expect(api.availableProvinces.value).toEqual([])
    })

    it('选中站点按缓存详情暴露省份（排序）', async () => {
      const { api, fetchExpressStations, fetchExpressStation } = setup()
      const station = makeStation({
        prices: [
          { id: 10, stationId: 1, provinceName: '浙江', priceW1Kg: 8 },
          { id: 11, stationId: 1, provinceName: '广东', priceW1Kg: 7 },
        ],
      })
      fetchExpressStations.mockResolvedValue({ records: [station] })
      fetchExpressStation.mockResolvedValue(station)

      await api.openCalcDialog()

      expect(api.calcStationDetail.value).toEqual(station)
      expect(api.availableProvinces.value).toEqual(['广东', '浙江'])
    })
  })

  describe('站点切换加载详情', () => {
    it('切换新站点时拉取详情入缓存', async () => {
      const { api, fetchExpressStation } = setup()
      const station = makeStation({ id: 5, isDefault: false, name: '站点戊' })
      fetchExpressStation.mockResolvedValue(station)

      api.calcStationId.value = 5
      await vi.waitFor(() => {
        expect(fetchExpressStation).toHaveBeenCalledWith(5)
      })
    })

    it('空 id 不触发请求', async () => {
      const { api, fetchExpressStation } = setup()
      api.calcStationId.value = null
      await new Promise((r) => setTimeout(r, 0))
      expect(fetchExpressStation).not.toHaveBeenCalled()
    })
  })

  describe('handleCalculate', () => {
    it('缺站点或省份不计算', async () => {
      const { api, fetchExpressStation } = setup()
      await api.handleCalculate()
      expect(fetchExpressStation).not.toHaveBeenCalled()
    })

    it('只填体积时换算体积重再计费', async () => {
      const { api, fetchExpressStations, fetchExpressStation } = setup()
      const station = makeStation()
      fetchExpressStations.mockResolvedValue({ records: [station] })
      fetchExpressStation.mockResolvedValue(station)
      await api.openCalcDialog()

      api.calcLength.value = 40
      api.calcWidth.value = 30
      api.calcHeight.value = 20
      await api.handleCalculate()

      // 40*30*20 / 8000 = 3kg
      expect(api.calcWeight.value).toBeCloseTo(3, 5)
      expect(api.calcResult.value).not.toBeNull()
      expect(api.calcLoading.value).toBe(false)
    })

    it('详情未缓存时计算前兜底拉取', async () => {
      const { api, fetchExpressStations, fetchExpressStation } = setup()
      fetchExpressStations.mockResolvedValue({ records: [] })
      const station = makeStation()
      fetchExpressStation.mockResolvedValue(station)
      await api.openCalcDialog()

      api.calcStationId.value = 1
      await vi.waitFor(() => {
        expect(fetchExpressStation).toHaveBeenCalledWith(1)
      })
      api.calcProvince.value = '广东'
      api.calcWeight.value = 1
      await api.handleCalculate()

      expect(api.calcResult.value?.freight).toBe(8)
    })

    it('省份无对应价格时清空结果', async () => {
      const { api, fetchExpressStations, fetchExpressStation } = setup()
      const station = makeStation()
      fetchExpressStations.mockResolvedValue({ records: [station] })
      fetchExpressStation.mockResolvedValue(station)
      await api.openCalcDialog()

      api.calcProvince.value = '不存在省'
      api.calcWeight.value = 1
      await api.handleCalculate()

      expect(api.calcResult.value).toBeNull()
    })
  })
})
