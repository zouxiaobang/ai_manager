import { nextTick, reactive, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type InputInstance } from 'element-plus'
import {
  copyExpressStation,
  createExpressStation,
  deleteExpressStation,
  fetchExpressStation,
  updateExpressStation,
  type EcExpressNotice,
  type EcExpressPrice,
  type EcExpressStation,
} from '@/api/ecommerce/express'
import { buildStationSavePayload } from '../expressPanelView'

/**
 * 站点基本信息表单依赖：共享编辑态与跨表单联动动作均由组件持有并注入，
 * 保持表单 CRUD 编排可脱离组件单测。
 */
export interface ExpressStationFormDeps {
  /** 当前编辑站点 id（与价格/公告表单共享） */
  editingId: Ref<number | null>
  /** 当前站点价格矩阵（新建清空/编辑回填） */
  prices: Ref<EcExpressPrice[]>
  /** 当前站点须知（同上） */
  notices: Ref<EcExpressNotice[]>
  /** 展开行 id 集合（保存后联动刷新展开详情） */
  expandedRowKeys: Ref<number[]>
  /** 重新加载列表（保存/删除/复制后回最新列表） */
  loadStations: () => Promise<void>
  /** 重新加载当前站点价格/须知子数据 */
  loadStationChildren: (stationId: number) => Promise<void>
  /** 使展开详情缓存失效（保存/删除后强制重拉） */
  invalidateExpandDetail: (stationId: number) => void
  /** 拉取并缓存展开详情（展开行保存后联动刷新） */
  loadExpandDetail: (stationId: number) => Promise<void>
  /** 打开/编辑站点时清空价格行搜索关键词 */
  resetPriceRegionKeyword: () => void
}

/**
 * 站点基本信息表单状态机：基本信息 + 别名标签 + 新建/编辑/删除/复制。
 * 保存后回写编辑态并刷新列表与展开详情；无编辑 id 时先建后编（段保存的前提）。
 */
export function useExpressStationForm(deps: ExpressStationFormDeps) {
  const { t } = useI18n()

  const dialogVisible = ref(false)
  const saving = ref(false)
  const savingBasic = ref(false)

  const form = reactive({
    name: '',
    avatarUrl: '',
    contact: '',
    address: '',
    labelPrice: null as number | null,
    isDefault: false,
    nameAliases: [] as string[],
  })

  const nameAliasInput = ref('')
  const aliasInputVisible = ref(false)
  const aliasInputRef = ref<InputInstance>()

  /** 重置表单与别名编辑态（打开新建/编辑时统一清理） */
  function resetForm() {
    form.name = ''
    form.avatarUrl = ''
    form.contact = ''
    form.address = ''
    form.labelPrice = null
    form.isDefault = false
    form.nameAliases = []
    nameAliasInput.value = ''
    aliasInputVisible.value = false
    deps.resetPriceRegionKeyword()
  }

  /** 追加别名标签：空值或重复返回 false（重复时清空输入便于重输） */
  function addNameAlias(): boolean {
    const value = nameAliasInput.value.trim()
    if (!value) return false
    if (form.nameAliases.includes(value)) {
      nameAliasInput.value = ''
      return false
    }
    form.nameAliases.push(value)
    nameAliasInput.value = ''
    return true
  }

  /** 别名弹窗内确认：追加成功后收起弹窗 */
  function onAddNameAliasFromPopover() {
    if (addNameAlias()) {
      aliasInputVisible.value = false
    }
  }

  /** 弹窗打开后聚焦别名输入框 */
  function focusAliasInput() {
    void nextTick(() => aliasInputRef.value?.focus())
  }

  function removeNameAlias(index: number) {
    form.nameAliases.splice(index, 1)
  }

  /** 打开新建：清空编辑态、重置表单与子数据 */
  function openCreate() {
    deps.editingId.value = null
    resetForm()
    deps.prices.value = []
    deps.notices.value = []
    dialogVisible.value = true
  }

  /** 打开编辑：拉详情回填基本信息与子数据 */
  async function openEdit(row: EcExpressStation) {
    deps.editingId.value = row.id
    const detail = await fetchExpressStation(row.id)
    form.name = detail.name
    form.avatarUrl = detail.avatarUrl || ''
    form.contact = detail.contact || ''
    form.address = detail.address || ''
    form.labelPrice = detail.labelPrice ?? null
    form.isDefault = !!detail.isDefault
    form.nameAliases = [...(detail.nameAliases || [])]
    nameAliasInput.value = ''
    aliasInputVisible.value = false
    deps.resetPriceRegionKeyword()
    deps.prices.value = detail.prices || []
    deps.notices.value = detail.notices || []
    dialogVisible.value = true
  }

  /** 保存站点：新建回写 created id、编辑使展开详情失效；保存后刷新列表与展开行 */
  async function onSaveStation(options?: { silent?: boolean; loadingRef?: Ref<boolean> }) {
    if (!form.name.trim()) {
      ElMessage.warning(t('ecommerce.express.nameRequired'))
      return false
    }

    const loader = options?.loadingRef ?? saving
    loader.value = true
    try {
      const payload = buildStationSavePayload(form)
      if (deps.editingId.value) {
        await updateExpressStation(deps.editingId.value, payload)
        deps.invalidateExpandDetail(deps.editingId.value)
      } else {
        const created = await createExpressStation(payload)
        deps.editingId.value = created.id
      }
      if (!options?.silent) {
        ElMessage.success(t('ecommerce.common.saved'))
      }
      await deps.loadStations()
      if (deps.editingId.value) {
        await deps.loadStationChildren(deps.editingId.value)
        if (deps.expandedRowKeys.value.includes(deps.editingId.value)) {
          await deps.loadExpandDetail(deps.editingId.value)
        }
      }
      return true
    } finally {
      loader.value = false
    }
  }

  /** 基本信息段保存：走 onSaveStation 但复用「基本信息」loading 指示 */
  async function onSaveBasicSection() {
    await onSaveStation({ loadingRef: savingBasic })
  }

  /** 段保存前置：无编辑 id 时先静默保存站点以拿到 id */
  async function ensureStationSavedForSection(): Promise<boolean> {
    if (deps.editingId.value) {
      return true
    }
    return onSaveStation({ silent: true, loadingRef: savingBasic })
  }

  /** 删除站点：确认后删除，清理展开态并刷新列表 */
  async function onDelete(row: EcExpressStation) {
    await ElMessageBox.confirm(
      t('ecommerce.express.deleteConfirm', { name: row.name }),
      { type: 'warning' },
    )
    await deleteExpressStation(row.id)
    ElMessage.success(t('ecommerce.common.deleted'))
    deps.invalidateExpandDetail(row.id)
    deps.expandedRowKeys.value = deps.expandedRowKeys.value.filter((id) => id !== row.id)
    await deps.loadStations()
  }

  /** 复制站点：后端整站复制后刷新列表 */
  async function onCopy(row: EcExpressStation) {
    await copyExpressStation(row.id)
    ElMessage.success(t('ecommerce.express.copyStationSuccess'))
    await deps.loadStations()
  }

  return {
    dialogVisible,
    saving,
    savingBasic,
    form,
    nameAliasInput,
    aliasInputVisible,
    aliasInputRef,
    resetForm,
    addNameAlias,
    onAddNameAliasFromPopover,
    focusAliasInput,
    removeNameAlias,
    openCreate,
    openEdit,
    onSaveStation,
    onSaveBasicSection,
    ensureStationSavedForSection,
    onDelete,
    onCopy,
  }
}
