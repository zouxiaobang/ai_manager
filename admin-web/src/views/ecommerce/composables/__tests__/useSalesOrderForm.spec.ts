import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import type { EcShop } from '@/api/ecommerce/shop'
import { useSalesOrderForm } from '../useSalesOrderForm'

const mocks = vi.hoisted(() => {
  return {
    createSalesOrder: vi.fn(),
    updateSalesOrder: vi.fn(),
    fetchListingLinks: vi.fn(),
    fetchListingLink: vi.fn(),
    elMessageSuccess: vi.fn(),
    elMessageWarning: vi.fn(),
    parseProvinceFromAddress: vi.fn(),
    load: vi.fn(),
  }
})

vi.mock('@/api/ecommerce/salesOrder', () => ({
  createSalesOrder: mocks.createSalesOrder,
  updateSalesOrder: mocks.updateSalesOrder,
}))
vi.mock('@/api/ecommerce/listingLink', () => ({
  fetchListingLinks: mocks.fetchListingLinks,
  fetchListingLink: mocks.fetchListingLink,
}))
vi.mock('element-plus', () => ({
  ElMessage: { success: mocks.elMessageSuccess, warning: mocks.elMessageWarning },
}))
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key }),
}))
vi.mock('@/utils/addressProvince', () => ({
  parseProvinceFromAddress: mocks.parseProvinceFromAddress,
}))

function makeShop(id: number): EcShop {
  return { id, name: `店铺${id}`, status: 'ENABLED' } as EcShop
}

describe('useSalesOrderForm 订单表单', () => {
  function setup(shops: EcShop[] = [makeShop(1)]) {
    return useSalesOrderForm({
      shopOptions: { value: shops },
      load: mocks.load,
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mocks.createSalesOrder.mockResolvedValue(undefined)
    mocks.updateSalesOrder.mockResolvedValue(undefined)
    mocks.fetchListingLinks.mockResolvedValue({ records: [] })
    mocks.parseProvinceFromAddress.mockReturnValue('广东省')
  })

  describe('openCreate / resetForm', () => {
    it('打开新建：清空编辑态、填充默认值', () => {
      const api = setup()
      api.openCreate()
      expect(api.dialogVisible.value).toBe(true)
      expect(api.editingId.value).toBeNull()
      expect(api.form.shopId).toBe(1)
      expect(api.form.platformStatus).toBe('已完成')
      expect(api.form.orderTime).toMatch(/^\d{4}-\d{2}-\d{2} 00:00:00$/)
      expect(api.form.lines).toHaveLength(1)
    })

    it('无店铺时 shopId 默认 undefined', () => {
      const api = setup([])
      api.openCreate()
      expect(api.form.shopId).toBeUndefined()
    })
  })

  describe('onSave 校验与提交', () => {
    function fillValid(api: ReturnType<typeof setup>) {
      api.form.shopId = 1
      api.form.orderTime = '2026-08-01 10:00:00'
      api.form.lines = [{ linkName: '链接A', skuSpecName: '红色' } as never]
    }

    it('无店铺提示并中止', async () => {
      const api = setup()
      api.form.orderTime = '2026-08-01 10:00:00'
      api.form.lines = [{ linkName: '链接A', skuSpecName: '红色' } as never]
      await api.onSave()
      expect(mocks.elMessageWarning).toHaveBeenCalledWith('ecommerce.salesOrder.shopRequired')
      expect(mocks.createSalesOrder).not.toHaveBeenCalled()
    })

    it('无订单时间提示并中止', async () => {
      const api = setup()
      api.form.shopId = 1
      api.form.lines = [{ linkName: '链接A', skuSpecName: '红色' } as never]
      await api.onSave()
      expect(mocks.elMessageWarning).toHaveBeenCalledWith('ecommerce.salesOrder.orderTimeRequired')
    })

    it('无有效行提示并中止', async () => {
      const api = setup()
      api.form.shopId = 1
      api.form.orderTime = '2026-08-01 10:00:00'
      await api.onSave()
      expect(mocks.elMessageWarning).toHaveBeenCalledWith('ecommerce.salesOrder.linesRequired')
    })

    it('新建模式调 createSalesOrder 并刷新列表', async () => {
      const api = setup()
      fillValid(api)
      await api.onSave()
      expect(mocks.createSalesOrder).toHaveBeenCalledWith(expect.objectContaining({ shopId: 1 }))
      expect(mocks.updateSalesOrder).not.toHaveBeenCalled()
      expect(mocks.elMessageSuccess).toHaveBeenCalledWith('ecommerce.common.saved')
      expect(api.dialogVisible.value).toBe(false)
      expect(api.editingId.value).toBeNull()
      expect(mocks.load).toHaveBeenCalled()
    })

    it('编辑模式调 updateSalesOrder(编辑ID)', async () => {
      const api = setup()
      fillValid(api)
      api.editingId.value = 5
      await api.onSave()
      expect(mocks.updateSalesOrder).toHaveBeenCalledWith(5, expect.objectContaining({ shopId: 1 }))
      expect(mocks.createSalesOrder).not.toHaveBeenCalled()
      expect(mocks.load).toHaveBeenCalled()
    })

    it('保存失败复位 saving 且不关闭对话框', async () => {
      mocks.createSalesOrder.mockRejectedValue(new Error('boom'))
      const api = setup()
      fillValid(api)
      api.dialogVisible.value = true // 模拟对话框已打开
      await expect(api.onSave()).rejects.toThrow('boom')
      expect(api.saving.value).toBe(false)
      expect(api.dialogVisible.value).toBe(true) // 失败不关闭
    })
  })

  describe('行编辑', () => {
    it('addLine 追加空行', () => {
      const api = setup()
      api.addLine()
      expect(api.form.lines).toHaveLength(1)
      api.addLine()
      expect(api.form.lines).toHaveLength(2)
    })

    it('removeLine 移除指定行', () => {
      const api = setup()
      api.addLine()
      api.addLine()
      api.removeLine(0)
      expect(api.form.lines).toHaveLength(1)
    })
  })

  describe('onShopChange / watch(dialogVisible)', () => {
    it('切换店铺后加载链接 SKU 选项', () => {
      const api = setup()
      api.onShopChange(2)
      expect(mocks.fetchListingLinks).toHaveBeenCalledWith(undefined, 2, undefined, { page: 1, pageSize: 100 })
    })

    it('shopId 为 0 不加载', () => {
      const api = setup()
      api.onShopChange(0)
      expect(mocks.fetchListingLinks).not.toHaveBeenCalled()
    })

    it('打开对话框且有店铺时自动加载 SKU 选项', async () => {
      const api = setup()
      api.form.shopId = 3
      api.dialogVisible.value = true
      await nextTick()
      expect(mocks.fetchListingLinks).toHaveBeenCalled()
    })
  })

  describe('loadLinkSkuOptions 聚合链接与 SKU', () => {
    beforeEach(() => {
      mocks.fetchListingLinks.mockResolvedValue({
        records: [{ id: 10 }, { id: 11 }],
      })
      mocks.fetchListingLink.mockImplementation(async (id: number) => {
        if (id === 10) {
          return { name: '链接A', skus: [{ id: 100, skuName: '红色' }, { id: 101, skuName: '蓝色' }, { id: 0, skuName: '无ID' }] }
        }
        return { name: '链接B', skus: [{ id: 200, skuName: 'M' }] }
      })
    })

    it('聚合每个链接的 SKU 生成下拉选项，跳过无 id 的 SKU', async () => {
      const api = setup()
      await api.loadLinkSkuOptions(1)
      expect(api.linkSkuOptions.value).toEqual([
        { key: '链接A|||红色', label: '链接A · 红色', linkName: '链接A', skuSpecName: '红色', listingLinkSkuId: 100 },
        { key: '链接A|||蓝色', label: '链接A · 蓝色', linkName: '链接A', skuSpecName: '蓝色', listingLinkSkuId: 101 },
        { key: '链接B|||M', label: '链接B · M', linkName: '链接B', skuSpecName: 'M', listingLinkSkuId: 200 },
      ])
    })
  })

  describe('syncProvinceFromAddress', () => {
    it('按地址解析省份并回填', () => {
      const api = setup()
      api.form.receiveAddress = '广东省深圳市南山区'
      api.syncProvinceFromAddress()
      expect(mocks.parseProvinceFromAddress).toHaveBeenCalledWith('广东省深圳市南山区')
      expect(api.form.receiveProvince).toBe('广东省')
    })
  })

  describe('statusLabel', () => {
    it('已知状态映射为 i18n 文案', () => {
      const api = setup()
      expect(api.statusLabel('PAID')).toBe('ecommerce.salesOrder.statusPaid')
    })

    it('未知状态返回原值，空值返回占位符', () => {
      const api = setup()
      expect(api.statusLabel('UNKNOWN')).toBe('UNKNOWN')
      expect(api.statusLabel()).toBe('—')
    })
  })
})
