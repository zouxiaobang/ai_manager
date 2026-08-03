import { computed, nextTick, reactive, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type TableInstance } from 'element-plus'
import Sortable from 'sortablejs'
import {
  createExpressNotice,
  deleteExpressNotice,
  updateExpressNotice,
  type EcExpressNotice,
} from '@/api/ecommerce/express'
import { buildNoticeSavePayload, computeNoticeReorders } from '../expressPanelView'

/**
 * 公告表单依赖：站点主弹窗可见性与共享编辑态由组件持有并注入。
 * 拖拽排序基于通知表格 DOM，仅在站点弹窗内挂载。
 */
export interface ExpressNoticeFormDeps {
  /** 站点主弹窗可见性（决定拖拽排序是否挂载） */
  dialogVisible: Ref<boolean>
  editingId: Ref<number | null>
  notices: Ref<EcExpressNotice[]>
  expandedRowKeys: Ref<number[]>
  loadStations: () => Promise<void>
  loadStationChildren: (stationId: number) => Promise<void>
  invalidateExpandDetail: (stationId: number) => void
  loadExpandDetail: (stationId: number) => Promise<void>
}

/**
 * 公告表单状态机：公告弹窗（新建/编辑）+ 预览 + 拖拽排序。
 * 保存/删除后刷新子数据；排序变更批量落库并联动展开详情。
 */
export function useExpressNoticeForm(deps: ExpressNoticeFormDeps) {
  const { t } = useI18n()

  const noticeDialogVisible = ref(false)
  const noticeEditingId = ref<number | null>(null)
  const noticeSaving = ref(false)
  const noticeReordering = ref(false)
  const noticeTableRef = ref<TableInstance>()
  let noticeSortable: Sortable | null = null

  const noticeForm = reactive({
    content: '',
    highlightRed: false,
    sortOrder: 0,
  })

  /** 公告预览文本：内容为空时展示占位 */
  const noticePreviewText = computed(() => {
    const text = noticeForm.content.trim()
    return text || t('ecommerce.express.noticePreviewPlaceholder')
  })

  /** 销毁拖拽实例（弹窗关闭/数据变化/卸载时清理） */
  function destroyNoticeSortable() {
    noticeSortable?.destroy()
    noticeSortable = null
  }

  /** 挂载拖拽排序：仅站点弹窗可见、有编辑态且有公告时生效 */
  function setupNoticeSortable() {
    destroyNoticeSortable()
    if (!deps.dialogVisible.value || !deps.editingId.value || !deps.notices.value.length) {
      return
    }
    void nextTick(() => {
      const tbody = noticeTableRef.value?.$el?.querySelector('.el-table__body-wrapper tbody')
      if (!tbody) return
      noticeSortable = Sortable.create(tbody as HTMLElement, {
        animation: 150,
        handle: '.express-notice-drag-handle',
        ghostClass: 'express-notice-row--ghost',
        disabled: noticeReordering.value,
        onEnd: (evt) => {
          void onNoticeReorder(evt.oldIndex, evt.newIndex)
        },
      })
    })
  }

  /** 拖拽结束落库：diff 出顺序变更条目批量更新，失败回滚重拉 */
  async function onNoticeReorder(oldIndex?: number, newIndex?: number) {
    if (
      oldIndex === undefined
      || newIndex === undefined
      || oldIndex === newIndex
      || !deps.editingId.value
    ) {
      return
    }

    const reorder = computeNoticeReorders(deps.notices.value, oldIndex, newIndex)
    if (!reorder) {
      return
    }
    const { ordered, updates } = reorder
    deps.notices.value = ordered

    noticeReordering.value = true
    try {
      await Promise.all(
        updates.map((item) =>
          updateExpressNotice(item.id, {
            stationId: deps.editingId.value!,
            content: item.content,
            highlightRed: item.highlightRed,
            sortOrder: item.sortOrder,
          }),
        ),
      )
      deps.invalidateExpandDetail(deps.editingId.value!)
      if (deps.expandedRowKeys.value.includes(deps.editingId.value)) {
        await deps.loadExpandDetail(deps.editingId.value)
      }
    } catch {
      // 落库失败回滚为服务器数据，避免本地重排与库不一致
      if (deps.editingId.value) {
        await deps.loadStationChildren(deps.editingId.value)
      }
      ElMessage.error(t('ecommerce.express.noticeReorderFailed'))
    } finally {
      noticeReordering.value = false
      setupNoticeSortable()
    }
  }

  /** 重置公告表单：清空内容，排序默认追加到末尾 */
  function resetNoticeForm() {
    noticeForm.content = ''
    noticeForm.highlightRed = false
    noticeForm.sortOrder = deps.notices.value.length + 1
  }

  function openNoticeCreate() {
    noticeEditingId.value = null
    resetNoticeForm()
    noticeDialogVisible.value = true
  }

  function openNoticeEdit(row: EcExpressNotice) {
    noticeEditingId.value = row.id
    noticeForm.content = row.content
    noticeForm.highlightRed = !!row.highlightRed
    noticeForm.sortOrder = row.sortOrder ?? 0
    noticeDialogVisible.value = true
  }

  /** 保存/删除后刷新子数据与展开详情 */
  async function refreshChildren() {
    if (!deps.editingId.value) return
    await deps.loadStationChildren(deps.editingId.value)
    deps.invalidateExpandDetail(deps.editingId.value)
    if (deps.expandedRowKeys.value.includes(deps.editingId.value)) {
      await deps.loadExpandDetail(deps.editingId.value)
    }
    await deps.loadStations()
  }

  /** 保存公告：新建/编辑落库后关闭弹窗并刷新 */
  async function onSaveNotice(options?: { silent?: boolean; loadingRef?: Ref<boolean> }) {
    if (!deps.editingId.value) return false
    if (!noticeForm.content.trim()) {
      ElMessage.warning(t('ecommerce.express.noticeRequired'))
      return false
    }

    const loader = options?.loadingRef ?? noticeSaving
    loader.value = true
    try {
      const payload = buildNoticeSavePayload(noticeForm, deps.editingId.value)
      if (noticeEditingId.value) {
        await updateExpressNotice(noticeEditingId.value, payload)
      } else {
        await createExpressNotice(payload)
      }
      if (!options?.silent) {
        ElMessage.success(t('ecommerce.common.saved'))
      }
      noticeDialogVisible.value = false
      await refreshChildren()
      return true
    } finally {
      loader.value = false
    }
  }

  /** 删除公告：确认后删除并刷新子数据 */
  async function onDeleteNotice(row: EcExpressNotice) {
    await ElMessageBox.confirm(t('ecommerce.express.deleteNoticeConfirm'), { type: 'warning' })
    await deleteExpressNotice(row.id)
    ElMessage.success(t('ecommerce.common.deleted'))
    if (deps.editingId.value) {
      await refreshChildren()
    }
  }

  // 站点弹窗可见且有数据时挂载拖拽；关闭或编辑态切换时销毁重建
  watch([deps.dialogVisible, () => deps.notices.value.length, deps.editingId], ([visible]) => {
    if (visible && deps.editingId.value) {
      setupNoticeSortable()
      return
    }
    destroyNoticeSortable()
  })

  return {
    noticeForm,
    noticeDialogVisible,
    noticeEditingId,
    noticeSaving,
    noticeReordering,
    noticeTableRef,
    noticePreviewText,
    resetNoticeForm,
    openNoticeCreate,
    openNoticeEdit,
    onSaveNotice,
    onDeleteNotice,
    setupNoticeSortable,
    destroyNoticeSortable,
    onNoticeReorder,
  }
}
