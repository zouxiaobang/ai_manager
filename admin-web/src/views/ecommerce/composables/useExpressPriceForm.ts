import { computed, reactive, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createExpressPrice,
  deleteExpressPrice,
  updateExpressPrice,
  type EcExpressPrice,
} from '@/api/ecommerce/express'
import {
  buildPriceSavePayload,
  buildRecentPriceRegions,
  filterPricesByKeyword,
  pickPriceValues,
} from '../expressPanelView'
import type { PriceFieldKey } from '../expressPriceView'

/** 价格表单档位字段（与价格列一致，重置/复制上一行/编辑回填复用） */
const PRICE_FIELDS: PriceFieldKey[] = [
  'priceW03Kg',
  'priceW05Kg',
  'priceW1Kg',
  'priceW15Kg',
  'priceW2Kg',
  'priceW25Kg',
  'priceW3Kg',
  'over3FirstPrice',
  'over3AdditionalPrice',
]

/**
 * 价格表单依赖：共享编辑态与跨表单联动动作由组件持有并注入。
 * 价格行列表 prices 为只读引用（刷新由 loadStationChildren 完成）。
 */
export interface ExpressPriceFormDeps {
  editingId: Ref<number | null>
  prices: Ref<EcExpressPrice[]>
  expandedRowKeys: Ref<number[]>
  loadStations: () => Promise<void>
  loadStationChildren: (stationId: number) => Promise<void>
  invalidateExpandDetail: (stationId: number) => void
  loadExpandDetail: (stationId: number) => Promise<void>
}

/**
 * 价格矩阵表单状态机：价格弹窗（新建/编辑/复制）+ 行搜索 + 最近省份快捷选择。
 * 保存/删除后刷新子数据与展开详情。
 */
export function useExpressPriceForm(deps: ExpressPriceFormDeps) {
  const { t } = useI18n()

  const priceDialogVisible = ref(false)
  const priceEditingId = ref<number | null>(null)
  const priceSaving = ref(false)
  const priceCollapseActive = ref(['le1'])
  const priceRegionKeyword = ref('')

  const priceForm = reactive<Record<PriceFieldKey, number | null> & { provinceName: string }>({
    provinceName: '',
    priceW03Kg: null,
    priceW05Kg: null,
    priceW1Kg: null,
    priceW15Kg: null,
    priceW2Kg: null,
    priceW25Kg: null,
    priceW3Kg: null,
    over3FirstPrice: null,
    over3AdditionalPrice: null,
  })

  /** 最近使用省份（供弹窗快捷选择） */
  const recentPriceRegions = computed(() => buildRecentPriceRegions(deps.prices.value))

  /** 编辑区内价格行搜索过滤结果 */
  const filteredEditPrices = computed(() =>
    filterPricesByKeyword(deps.prices.value, priceRegionKeyword.value),
  )

  function resetPriceRegionKeyword() {
    priceRegionKeyword.value = ''
  }

  /** 重置价格表单：清空省份与全部档位 */
  function resetPriceForm() {
    priceForm.provinceName = ''
    PRICE_FIELDS.forEach((key) => {
      priceForm[key] = null
    })
  }

  /** 最近省份快捷回填 */
  function selectRecentRegion(name: string) {
    priceForm.provinceName = name
  }

  /** 复制上一行价格档位：无历史提示并中止 */
  function copyPreviousRegionPrice() {
    if (!deps.prices.value.length) {
      ElMessage.warning(t('ecommerce.express.noPreviousRegionPrice'))
      return
    }
    const previous = deps.prices.value[deps.prices.value.length - 1]
    const values = pickPriceValues(previous, PRICE_FIELDS)
    Object.assign(priceForm, values)
    ElMessage.success(t('ecommerce.express.copyPreviousRegionPriceSuccess'))
  }

  /** 打开新建价格：重置表单、默认折叠到首组 */
  function openPriceCreate() {
    priceEditingId.value = null
    resetPriceForm()
    priceCollapseActive.value = ['le1']
    priceDialogVisible.value = true
  }

  /** 打开编辑价格：回填省份与档位、展开全部组 */
  function openPriceEdit(row: EcExpressPrice) {
    priceEditingId.value = row.id
    priceForm.provinceName = row.provinceName
    PRICE_FIELDS.forEach((key) => {
      priceForm[key] = row[key] ?? null
    })
    priceCollapseActive.value = ['le1', 'mid', 'over3']
    priceDialogVisible.value = true
  }

  /** 打开复制价格：以选中行为模板新建（无编辑 id） */
  function openPriceCopy(row: EcExpressPrice) {
    priceEditingId.value = null
    resetPriceForm()
    PRICE_FIELDS.forEach((key) => {
      priceForm[key] = row[key] ?? null
    })
    priceCollapseActive.value = ['le1', 'mid', 'over3']
    priceDialogVisible.value = true
  }

  /** 价格行搜索输入 */
  function onPriceRegionSearchInput(value: string) {
    priceRegionKeyword.value = value
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

  /** 保存价格：新建/编辑落库后关闭弹窗并刷新 */
  async function onSavePrice(options?: { silent?: boolean; loadingRef?: Ref<boolean> }) {
    if (!deps.editingId.value) return false
    if (!priceForm.provinceName.trim()) {
      ElMessage.warning(t('ecommerce.express.provinceRequired'))
      return false
    }

    const loader = options?.loadingRef ?? priceSaving
    loader.value = true
    try {
      const payload = buildPriceSavePayload(priceForm, deps.editingId.value)
      if (priceEditingId.value) {
        await updateExpressPrice(priceEditingId.value, payload)
      } else {
        await createExpressPrice(payload)
      }
      if (!options?.silent) {
        ElMessage.success(t('ecommerce.common.saved'))
      }
      priceDialogVisible.value = false
      await refreshChildren()
      return true
    } finally {
      loader.value = false
    }
  }

  /** 删除价格：确认后删除并刷新子数据 */
  async function onDeletePrice(row: EcExpressPrice) {
    await ElMessageBox.confirm(
      t('ecommerce.express.deletePriceConfirm', { province: row.provinceName }),
      { type: 'warning' },
    )
    await deleteExpressPrice(row.id)
    ElMessage.success(t('ecommerce.common.deleted'))
    if (deps.editingId.value) {
      await refreshChildren()
    }
  }

  return {
    priceForm,
    priceDialogVisible,
    priceEditingId,
    priceSaving,
    priceCollapseActive,
    priceRegionKeyword,
    recentPriceRegions,
    filteredEditPrices,
    resetPriceForm,
    resetPriceRegionKeyword,
    selectRecentRegion,
    copyPreviousRegionPrice,
    openPriceCreate,
    openPriceEdit,
    openPriceCopy,
    onPriceRegionSearchInput,
    onSavePrice,
    onDeletePrice,
  }
}
