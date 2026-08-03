import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteSalesOrder,
  fetchSalesOrder,
  updateSalesOrder,
  type EcSalesOrder,
  type EcSalesOrderSaveRequest,
} from '@/api/ecommerce/salesOrder'
import type { ShopIconMeta } from '@/utils/shopVisual'

/**
 * 销售订单详情抽屉逻辑
 *
 * <p>从 {@code SalesOrderPanel.vue} 提取：详情加载 / 保存 / 删除 / 路由直达打开 /
 * 行选中高亮等与详情抽屉相关的状态与函数。</p>
 *
 * 依赖注入（组件持有、两个抽屉共用的部分）：
 * - {@code getOrderShopIconMeta} 店铺图标元数据解析（模板行内也用，留在组件）
 * - {@code loadLinkSkuOptions} 按店铺加载可售链接/SKU（新增/编辑对话框共用）
 * - {@code load} 列表刷新（保存/删除后需要回到最新列表）
 */
export interface SalesOrderDetailDeps {
  getOrderShopIconMeta: (order?: EcSalesOrder | null) => ShopIconMeta
  loadLinkSkuOptions: (shopId: number) => Promise<void> | void
  load: () => Promise<void> | void
}

export function useSalesOrderDetail({ getOrderShopIconMeta, loadLinkSkuOptions, load }: SalesOrderDetailDeps) {
  const { t } = useI18n()
  const route = useRoute()
  const router = useRouter()

  const detailVisible = ref(false)
  const detailLoading = ref(false)
  const detailSaving = ref(false)
  /** 详情保存完成信号，递增触发子组件重新提交本地表单态 */
  const detailSaveCommitKey = ref(0)
  const deletingDetail = ref(false)
  const detailId = ref<number | null>(null)
  const detail = ref<EcSalesOrder | null>(null)

  const detailShopIconMeta = computed(() => getOrderShopIconMeta(detail.value))

  function openDetail(id: number) {
    detailId.value = id
    detailVisible.value = true
  }

  /** 详情抽屉打开的行加高亮类 */
  function orderRowClassName({ row }: { row: EcSalesOrder }) {
    return row.id === detailId.value && detailVisible.value ? 'is-selected' : ''
  }

  /** 从路由 query 的 orderId 直达打开详情，并清理 query 避免刷新重复触发 */
  function openDetailFromRouteQuery() {
    const raw = route.query.orderId
    const id = typeof raw === 'string' ? Number(raw) : Array.isArray(raw) ? Number(raw[0]) : NaN
    if (!Number.isFinite(id) || id <= 0) return
    openDetail(id)
    const nextQuery = { ...route.query }
    delete nextQuery.orderId
    void router.replace({ path: route.path, query: nextQuery })
  }

  async function loadDetail() {
    if (!detailId.value) return
    detailLoading.value = true
    try {
      detail.value = await fetchSalesOrder(detailId.value)
    } finally {
      detailLoading.value = false
    }
  }

  async function onSaveDetail(payload: EcSalesOrderSaveRequest) {
    if (!detailId.value) return
    detailSaving.value = true
    try {
      await updateSalesOrder(detailId.value, payload)
      ElMessage.success(t('ecommerce.common.saved'))
      detailSaveCommitKey.value += 1
      await load()
      await loadDetail()
    } finally {
      detailSaving.value = false
    }
  }

  async function onDeleteDetailOrder() {
    if (!detail.value?.id) return
    if (detail.value.source !== 'MANUAL' && detail.value.status !== 'DRAFT') {
      ElMessage.warning(t('ecommerce.salesOrder.deleteNotAllowed'))
      return
    }
    const label = detail.value.platformOrderNo || detail.value.orderNo
    await ElMessageBox.confirm(t('ecommerce.salesOrder.deleteConfirm', { orderNo: label }), { type: 'warning' })
    deletingDetail.value = true
    try {
      await deleteSalesOrder(detail.value.id)
      ElMessage.success(t('ecommerce.common.deleted'))
      detailVisible.value = false
      detail.value = null
      detailId.value = null
      await load()
    } finally {
      deletingDetail.value = false
    }
  }

  /** 详情内切换店铺后重载可售链接/SKU */
  function onDetailShopChange(shopId: number) {
    if (shopId) void loadLinkSkuOptions(shopId)
  }

  return {
    detailVisible,
    detailLoading,
    detailSaving,
    detailSaveCommitKey,
    deletingDetail,
    detailId,
    detail,
    detailShopIconMeta,
    openDetail,
    orderRowClassName,
    openDetailFromRouteQuery,
    loadDetail,
    onSaveDetail,
    onDeleteDetailOrder,
    onDetailShopChange,
  }
}
