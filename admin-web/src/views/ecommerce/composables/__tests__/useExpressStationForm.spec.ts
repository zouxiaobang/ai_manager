import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import type { EcExpressNotice, EcExpressPrice, EcExpressStation } from '@/api/ecommerce/express'
import { useExpressStationForm } from '../useExpressStationForm'

const mocks = vi.hoisted(() => ({
  fetchExpressStation: vi.fn(),
  createExpressStation: vi.fn(),
  updateExpressStation: vi.fn(),
  deleteExpressStation: vi.fn(),
  copyExpressStation: vi.fn(),
  elMessageSuccess: vi.fn(),
  elMessageWarning: vi.fn(),
  elMessageBoxConfirm: vi.fn(),
  loadStations: vi.fn(),
  loadStationChildren: vi.fn(),
  invalidateExpandDetail: vi.fn(),
  loadExpandDetail: vi.fn(),
  resetPriceRegionKeyword: vi.fn(),
}))

vi.mock('@/api/ecommerce/express', () => ({
  fetchExpressStation: mocks.fetchExpressStation,
  createExpressStation: mocks.createExpressStation,
  updateExpressStation: mocks.updateExpressStation,
  deleteExpressStation: mocks.deleteExpressStation,
  copyExpressStation: mocks.copyExpressStation,
}))
vi.mock('element-plus', () => ({
  ElMessage: { success: mocks.elMessageSuccess, warning: mocks.elMessageWarning },
  ElMessageBox: { confirm: mocks.elMessageBoxConfirm },
}))
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key }),
}))

function makeStation(partial?: Partial<EcExpressStation>): EcExpressStation {
  return { id: 1, name: '站点甲', ...partial }
}

function setup() {
  const editingId = ref<number | null>(null)
  const prices = ref<EcExpressPrice[]>([])
  const notices = ref<EcExpressNotice[]>([])
  const expandedRowKeys = ref<number[]>([])
  const api = useExpressStationForm({
    editingId,
    prices,
    notices,
    expandedRowKeys,
    loadStations: mocks.loadStations,
    loadStationChildren: mocks.loadStationChildren,
    invalidateExpandDetail: mocks.invalidateExpandDetail,
    loadExpandDetail: mocks.loadExpandDetail,
    resetPriceRegionKeyword: mocks.resetPriceRegionKeyword,
  })
  return { api, editingId, prices, notices, expandedRowKeys }
}

describe('useExpressStationForm 站点表单', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.fetchExpressStation.mockResolvedValue(makeStation())
    mocks.createExpressStation.mockResolvedValue({ id: 9 })
    mocks.updateExpressStation.mockResolvedValue(undefined)
    mocks.deleteExpressStation.mockResolvedValue(undefined)
    mocks.copyExpressStation.mockResolvedValue(undefined)
    mocks.loadStations.mockResolvedValue(undefined)
    mocks.loadStationChildren.mockResolvedValue(undefined)
    mocks.loadExpandDetail.mockResolvedValue(undefined)
    mocks.elMessageBoxConfirm.mockResolvedValue(undefined)
  })

  describe('openCreate / openEdit', () => {
    it('打开新建：清空编辑态、重置表单与子数据', () => {
      const { api, editingId, prices, notices } = setup()
      editingId.value = 5
      prices.value = [{} as EcExpressPrice]
      notices.value = [{} as EcExpressNotice]

      api.openCreate()

      expect(editingId.value).toBeNull()
      expect(api.dialogVisible.value).toBe(true)
      expect(api.form.name).toBe('')
      expect(api.form.isDefault).toBe(false)
      expect(api.form.nameAliases).toEqual([])
      expect(prices.value).toEqual([])
      expect(notices.value).toEqual([])
      expect(mocks.resetPriceRegionKeyword).toHaveBeenCalled()
    })

    it('打开编辑：拉详情回填基本信息与子数据', async () => {
      const { api, editingId, prices, notices } = setup()
      mocks.fetchExpressStation.mockResolvedValue(
        makeStation({
          name: '站点乙',
          labelPrice: 3,
          isDefault: true,
          nameAliases: ['别名A'],
          prices: [{ id: 10, stationId: 1, provinceName: '广东' }],
          notices: [{ id: 1, stationId: 1, content: '注意' }],
        }),
      )

      await api.openEdit(makeStation({ id: 2, name: '站点乙' }))

      expect(editingId.value).toBe(2)
      expect(api.form.name).toBe('站点乙')
      expect(api.form.labelPrice).toBe(3)
      expect(api.form.isDefault).toBe(true)
      expect(api.form.nameAliases).toEqual(['别名A'])
      expect(prices.value).toHaveLength(1)
      expect(notices.value).toHaveLength(1)
      expect(api.dialogVisible.value).toBe(true)
    })
  })

  describe('别名管理', () => {
    it('空输入不追加', () => {
      const { api } = setup()
      api.nameAliasInput.value = '   '
      expect(api.addNameAlias()).toBe(false)
      expect(api.form.nameAliases).toEqual([])
    })

    it('新增别名并清空输入', () => {
      const { api } = setup()
      api.nameAliasInput.value = '别名A'
      expect(api.addNameAlias()).toBe(true)
      expect(api.form.nameAliases).toEqual(['别名A'])
      expect(api.nameAliasInput.value).toBe('')
    })

    it('重复别名不追加', () => {
      const { api } = setup()
      api.form.nameAliases = ['别名A']
      api.nameAliasInput.value = '别名A'
      expect(api.addNameAlias()).toBe(false)
      expect(api.form.nameAliases).toEqual(['别名A'])
    })

    it('popover 确认追加成功后收起弹窗', () => {
      const { api } = setup()
      api.aliasInputVisible.value = true
      api.nameAliasInput.value = '别名A'
      api.onAddNameAliasFromPopover()
      expect(api.form.nameAliases).toEqual(['别名A'])
      expect(api.aliasInputVisible.value).toBe(false)
    })

    it('removeNameAlias 移除指定索引', () => {
      const { api } = setup()
      api.form.nameAliases = ['别名A', '别名B']
      api.removeNameAlias(0)
      expect(api.form.nameAliases).toEqual(['别名B'])
    })
  })

  describe('onSaveStation 校验与提交', () => {
    it('无名称提示并中止', async () => {
      const { api } = setup()
      await api.onSaveStation()
      expect(mocks.elMessageWarning).toHaveBeenCalledWith('ecommerce.express.nameRequired')
      expect(mocks.createExpressStation).not.toHaveBeenCalled()
    })

    it('新建模式：create 后回写编辑 id 并刷新列表与子数据', async () => {
      const { api, editingId } = setup()
      api.form.name = ' 站点甲 '
      api.form.isDefault = true

      const result = await api.onSaveStation()

      expect(result).toBe(true)
      expect(mocks.createExpressStation).toHaveBeenCalledWith(expect.objectContaining({ name: '站点甲', isDefault: true }))
      expect(editingId.value).toBe(9)
      expect(mocks.elMessageSuccess).toHaveBeenCalledWith('ecommerce.common.saved')
      expect(mocks.loadStations).toHaveBeenCalled()
      expect(mocks.loadStationChildren).toHaveBeenCalledWith(9)
    })

    it('编辑模式：update 编辑 id 并使展开详情失效', async () => {
      const { api, editingId, expandedRowKeys } = setup()
      editingId.value = 5
      expandedRowKeys.value = [5]
      api.form.name = '站点甲'

      const result = await api.onSaveStation()

      expect(result).toBe(true)
      expect(mocks.updateExpressStation).toHaveBeenCalledWith(5, expect.objectContaining({ name: '站点甲' }))
      expect(mocks.createExpressStation).not.toHaveBeenCalled()
      expect(mocks.invalidateExpandDetail).toHaveBeenCalledWith(5)
      expect(mocks.loadStations).toHaveBeenCalled()
      expect(mocks.loadStationChildren).toHaveBeenCalledWith(5)
      expect(mocks.loadExpandDetail).toHaveBeenCalledWith(5)
    })

    it('展开行未展开时不联动刷新详情', async () => {
      const { api, editingId } = setup()
      editingId.value = 5
      api.form.name = '站点甲'

      await api.onSaveStation()

      expect(mocks.loadExpandDetail).not.toHaveBeenCalled()
    })

    it('silent 模式不弹成功提示', async () => {
      const { api } = setup()
      api.form.name = '站点甲'

      await api.onSaveStation({ silent: true })

      expect(mocks.elMessageSuccess).not.toHaveBeenCalled()
      expect(mocks.createExpressStation).toHaveBeenCalled()
    })
  })

  describe('onSaveBasicSection / ensureStationSavedForSection', () => {
    it('基本信息段保存复用 savingBasic loading', async () => {
      const { api } = setup()
      api.form.name = '站点甲'
      await api.onSaveBasicSection()
      expect(mocks.createExpressStation).toHaveBeenCalled()
      expect(api.savingBasic.value).toBe(false)
    })

    it('已有编辑 id 时段保存前置直接通过', async () => {
      const { api, editingId } = setup()
      editingId.value = 5
      expect(await api.ensureStationSavedForSection()).toBe(true)
      expect(mocks.createExpressStation).not.toHaveBeenCalled()
    })

    it('无编辑 id 时静默保存以拿到 id', async () => {
      const { api } = setup()
      api.form.name = '站点甲'
      expect(await api.ensureStationSavedForSection()).toBe(true)
      expect(mocks.createExpressStation).toHaveBeenCalled()
      expect(mocks.elMessageSuccess).not.toHaveBeenCalled()
    })
  })

  describe('onDelete / onCopy', () => {
    it('删除：确认后删除、清理展开态并刷新', async () => {
      const { api, expandedRowKeys } = setup()
      expandedRowKeys.value = [1, 2]

      await api.onDelete(makeStation({ id: 1 }))

      expect(mocks.elMessageBoxConfirm).toHaveBeenCalled()
      expect(mocks.deleteExpressStation).toHaveBeenCalledWith(1)
      expect(mocks.elMessageSuccess).toHaveBeenCalledWith('ecommerce.common.deleted')
      expect(mocks.invalidateExpandDetail).toHaveBeenCalledWith(1)
      expect(expandedRowKeys.value).toEqual([2])
      expect(mocks.loadStations).toHaveBeenCalled()
    })

    it('复制：整站复制后刷新', async () => {
      const { api } = setup()
      await api.onCopy(makeStation())
      expect(mocks.copyExpressStation).toHaveBeenCalledWith(1)
      expect(mocks.elMessageSuccess).toHaveBeenCalledWith('ecommerce.express.copyStationSuccess')
      expect(mocks.loadStations).toHaveBeenCalled()
    })
  })
})
