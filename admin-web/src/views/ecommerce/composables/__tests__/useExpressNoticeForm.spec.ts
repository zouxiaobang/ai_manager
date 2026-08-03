import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import type { EcExpressNotice } from '@/api/ecommerce/express'
import { useExpressNoticeForm } from '../useExpressNoticeForm'

const mocks = vi.hoisted(() => ({
  createExpressNotice: vi.fn(),
  updateExpressNotice: vi.fn(),
  deleteExpressNotice: vi.fn(),
  elMessageSuccess: vi.fn(),
  elMessageWarning: vi.fn(),
  elMessageError: vi.fn(),
  elMessageBoxConfirm: vi.fn(),
  loadStations: vi.fn(),
  loadStationChildren: vi.fn(),
  invalidateExpandDetail: vi.fn(),
  loadExpandDetail: vi.fn(),
  sortableCreate: vi.fn(),
  sortableDestroy: vi.fn(),
}))

vi.mock('@/api/ecommerce/express', () => ({
  createExpressNotice: mocks.createExpressNotice,
  updateExpressNotice: mocks.updateExpressNotice,
  deleteExpressNotice: mocks.deleteExpressNotice,
}))
vi.mock('element-plus', () => ({
  ElMessage: {
    success: mocks.elMessageSuccess,
    warning: mocks.elMessageWarning,
    error: mocks.elMessageError,
  },
  ElMessageBox: { confirm: mocks.elMessageBoxConfirm },
}))
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key }),
}))
vi.mock('sortablejs', () => ({
  default: { create: mocks.sortableCreate },
}))

function makeNotice(partial?: Partial<EcExpressNotice>): EcExpressNotice {
  return { id: 1, stationId: 1, content: '注意', sortOrder: 0, ...partial }
}

function setup() {
  const dialogVisible = ref(true)
  const editingId = ref<number | null>(1)
  const notices = ref<EcExpressNotice[]>([])
  const expandedRowKeys = ref<number[]>([])
  const api = useExpressNoticeForm({
    dialogVisible,
    editingId,
    notices,
    expandedRowKeys,
    loadStations: mocks.loadStations,
    loadStationChildren: mocks.loadStationChildren,
    invalidateExpandDetail: mocks.invalidateExpandDetail,
    loadExpandDetail: mocks.loadExpandDetail,
  })
  return { api, dialogVisible, editingId, notices, expandedRowKeys }
}

describe('useExpressNoticeForm 公告表单', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.createExpressNotice.mockResolvedValue(undefined)
    mocks.updateExpressNotice.mockResolvedValue(undefined)
    mocks.deleteExpressNotice.mockResolvedValue(undefined)
    mocks.loadStations.mockResolvedValue(undefined)
    mocks.loadStationChildren.mockResolvedValue(undefined)
    mocks.loadExpandDetail.mockResolvedValue(undefined)
    mocks.elMessageBoxConfirm.mockResolvedValue(undefined)
    mocks.sortableCreate.mockReturnValue({ destroy: mocks.sortableDestroy })
  })

  describe('resetNoticeForm / 预览', () => {
    it('重置表单：清空内容，排序默认追加到末尾', () => {
      const { api, notices } = setup()
      notices.value = [makeNotice(), makeNotice({ id: 2 })]
      api.noticeForm.content = '旧内容'
      api.noticeForm.highlightRed = true

      api.resetNoticeForm()

      expect(api.noticeForm.content).toBe('')
      expect(api.noticeForm.highlightRed).toBe(false)
      expect(api.noticeForm.sortOrder).toBe(3)
    })

    it('预览空内容时展示占位文案', () => {
      const { api } = setup()
      expect(api.noticePreviewText.value).toBe('ecommerce.express.noticePreviewPlaceholder')
    })

    it('预览展示内容 trim 结果', () => {
      const { api } = setup()
      api.noticeForm.content = ' 新公告 '
      expect(api.noticePreviewText.value).toBe('新公告')
    })
  })

  describe('openNoticeCreate / openNoticeEdit', () => {
    it('打开新建：无编辑 id、重置表单', () => {
      const { api } = setup()
      api.openNoticeCreate()
      expect(api.noticeDialogVisible.value).toBe(true)
      expect(api.noticeEditingId.value).toBeNull()
      expect(api.noticeForm.content).toBe('')
    })

    it('打开编辑：回填公告内容与标记', () => {
      const { api } = setup()
      api.openNoticeEdit(makeNotice({ id: 5, content: '公告A', highlightRed: true, sortOrder: 2 }))
      expect(api.noticeEditingId.value).toBe(5)
      expect(api.noticeForm.content).toBe('公告A')
      expect(api.noticeForm.highlightRed).toBe(true)
      expect(api.noticeForm.sortOrder).toBe(2)
    })
  })

  describe('onSaveNotice 校验与提交', () => {
    it('无编辑站点时中止', async () => {
      const { api, editingId } = setup()
      editingId.value = null
      api.noticeForm.content = '公告A'
      expect(await api.onSaveNotice()).toBe(false)
      expect(mocks.createExpressNotice).not.toHaveBeenCalled()
    })

    it('内容为空提示并中止', async () => {
      const { api } = setup()
      await api.onSaveNotice()
      expect(mocks.elMessageWarning).toHaveBeenCalledWith('ecommerce.express.noticeRequired')
      expect(mocks.createExpressNotice).not.toHaveBeenCalled()
    })

    it('新建模式：create 后关闭弹窗并刷新子数据', async () => {
      const { api } = setup()
      api.noticeForm.content = ' 新公告 '
      api.noticeForm.highlightRed = true
      api.noticeForm.sortOrder = 3

      expect(await api.onSaveNotice()).toBe(true)

      expect(mocks.createExpressNotice).toHaveBeenCalledWith(
        expect.objectContaining({ stationId: 1, content: '新公告', highlightRed: true, sortOrder: 3 }),
      )
      expect(mocks.updateExpressNotice).not.toHaveBeenCalled()
      expect(mocks.elMessageSuccess).toHaveBeenCalledWith('ecommerce.common.saved')
      expect(api.noticeDialogVisible.value).toBe(false)
      expect(mocks.loadStationChildren).toHaveBeenCalledWith(1)
      expect(mocks.invalidateExpandDetail).toHaveBeenCalledWith(1)
      expect(mocks.loadStations).toHaveBeenCalled()
    })

    it('编辑模式：update 编辑公告 id', async () => {
      const { api } = setup()
      api.noticeEditingId.value = 5
      api.noticeForm.content = '公告A'

      expect(await api.onSaveNotice()).toBe(true)

      expect(mocks.updateExpressNotice).toHaveBeenCalledWith(5, expect.objectContaining({ stationId: 1, content: '公告A' }))
      expect(mocks.createExpressNotice).not.toHaveBeenCalled()
    })

    it('silent 模式不弹成功提示', async () => {
      const { api } = setup()
      api.noticeForm.content = '公告A'

      await api.onSaveNotice({ silent: true })

      expect(mocks.elMessageSuccess).not.toHaveBeenCalled()
    })
  })

  describe('onDeleteNotice', () => {
    it('确认后删除并刷新子数据', async () => {
      const { api } = setup()
      await api.onDeleteNotice(makeNotice())

      expect(mocks.elMessageBoxConfirm).toHaveBeenCalled()
      expect(mocks.deleteExpressNotice).toHaveBeenCalledWith(1)
      expect(mocks.elMessageSuccess).toHaveBeenCalledWith('ecommerce.common.deleted')
      expect(mocks.loadStationChildren).toHaveBeenCalledWith(1)
    })
  })

  describe('onNoticeReorder 拖拽排序', () => {
    it('同位置移动不落库', async () => {
      const { api, notices } = setup()
      notices.value = [makeNotice(), makeNotice({ id: 2 }), makeNotice({ id: 3 })]
      await api.onNoticeReorder(1, 1)
      expect(mocks.updateExpressNotice).not.toHaveBeenCalled()
    })

    it('无编辑站点时中止', async () => {
      const { api, editingId, notices } = setup()
      editingId.value = null
      notices.value = [makeNotice()]
      await api.onNoticeReorder(0, 2)
      expect(mocks.updateExpressNotice).not.toHaveBeenCalled()
    })

    it('移动后重排列表并批量落库、联动展开详情', async () => {
      const { api, notices, expandedRowKeys } = setup()
      expandedRowKeys.value = [1]
      notices.value = [
        makeNotice({ id: 1, sortOrder: 0 }),
        makeNotice({ id: 2, sortOrder: 1 }),
        makeNotice({ id: 3, sortOrder: 2 }),
      ]

      await api.onNoticeReorder(0, 2)

      expect(notices.value.map((n) => n.id)).toEqual([2, 3, 1])
      expect(mocks.updateExpressNotice).toHaveBeenCalledTimes(3)
      expect(mocks.invalidateExpandDetail).toHaveBeenCalledWith(1)
      expect(mocks.loadExpandDetail).toHaveBeenCalledWith(1)
      expect(api.noticeReordering.value).toBe(false)
    })

    it('落库失败回滚重拉并提示', async () => {
      const { api, notices } = setup()
      notices.value = [
        makeNotice({ id: 1, sortOrder: 0 }),
        makeNotice({ id: 2, sortOrder: 1 }),
        makeNotice({ id: 3, sortOrder: 2 }),
      ]
      mocks.updateExpressNotice.mockRejectedValue(new Error('boom'))

      await api.onNoticeReorder(0, 2)

      expect(mocks.loadStationChildren).toHaveBeenCalledWith(1)
      expect(mocks.elMessageError).toHaveBeenCalledWith('ecommerce.express.noticeReorderFailed')
      expect(api.noticeReordering.value).toBe(false)
    })
  })

  describe('setupNoticeSortable / destroyNoticeSortable', () => {
    it('站点弹窗不可见时不挂载拖拽', () => {
      const { api, dialogVisible, notices } = setup()
      dialogVisible.value = false
      notices.value = [makeNotice()]
      api.setupNoticeSortable()
      expect(mocks.sortableCreate).not.toHaveBeenCalled()
    })

    it('无编辑态时不挂载拖拽', () => {
      const { api, editingId, notices } = setup()
      editingId.value = null
      notices.value = [makeNotice()]
      api.setupNoticeSortable()
      expect(mocks.sortableCreate).not.toHaveBeenCalled()
    })

    it('无公告时不挂载拖拽', () => {
      const { api } = setup()
      api.setupNoticeSortable()
      expect(mocks.sortableCreate).not.toHaveBeenCalled()
    })

    it('条件满足时挂载拖拽实例', async () => {
      const { api, notices } = setup()
      notices.value = [makeNotice()]
      api.noticeTableRef.value = {
        $el: { querySelector: () => document.createElement('tbody') },
      } as never

      await api.setupNoticeSortable()
      await nextTick()

      expect(mocks.sortableCreate).toHaveBeenCalled()
    })

    it('destroyNoticeSortable 销毁实例', async () => {
      const { api, notices } = setup()
      notices.value = [makeNotice()]
      api.noticeTableRef.value = {
        $el: { querySelector: () => document.createElement('tbody') },
      } as never
      await api.setupNoticeSortable()
      await nextTick()

      api.destroyNoticeSortable()

      expect(mocks.sortableDestroy).toHaveBeenCalled()
    })

    it('未挂载时销毁安全跳过', () => {
      const { api } = setup()
      expect(() => api.destroyNoticeSortable()).not.toThrow()
    })
  })
})
