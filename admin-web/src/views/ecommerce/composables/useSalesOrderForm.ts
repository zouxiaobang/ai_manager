import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  createSalesOrder,
  updateSalesOrder,
  type EcSalesOrderLineSaveItem,
} from '@/api/ecommerce/salesOrder'
import { fetchListingLink, fetchListingLinks } from '@/api/ecommerce/listingLink'
import type { EcShop } from '@/api/ecommerce/shop'
import { parseProvinceFromAddress } from '@/utils/addressProvince'
import { todayDateString } from '@/utils/date'

/**
 * 销售订单新增/编辑对话框逻辑
 *
 * <p>从 {@code SalesOrderPanel.vue} 提取：表单状态、SKU 链接选项加载、行编辑、
 * 提交（新建/编辑）等与订单 form 对话框相关的逻辑。</p>
 *
 * 依赖注入（组件持有、多抽屉共用的部分）：
 * - {@code shopOptions} 店铺列表（resetForm 取默认店铺）
 * - {@code load} 列表刷新（保存成功后回到最新列表）
 *
 * {@code loadLinkSkuOptions} 同时被详情抽屉（useSalesOrderDetail deps）复用，
 * 由组件解构后透传。
 */
export interface SalesOrderFormDeps {
  shopOptions: { value: EcShop[] }
  load: () => Promise<void> | void
}

export type LineFormRow = EcSalesOrderLineSaveItem & { _pickerKey?: string }

export function useSalesOrderForm({ shopOptions, load }: SalesOrderFormDeps) {
  const { t } = useI18n()

  const dialogVisible = ref(false)
  const editingId = ref<number | null>(null)
  const saving = ref(false)

  const form = reactive<{
    shopId: number | undefined
    expressStationId: number | undefined
    orderTime: string
    payTime: string
    platformStatus: string
    platformOrderNo: string
    receivedAmount: number | undefined
    trackingNumber: string
    receiveAddress: string
    receiveProvince: string
    buyerRemark: string
    sellerRemark: string
    lines: LineFormRow[]
  }>({
    shopId: undefined,
    expressStationId: undefined,
    orderTime: '',
    payTime: '',
    platformStatus: '',
    platformOrderNo: '',
    receivedAmount: undefined,
    trackingNumber: '',
    receiveAddress: '',
    receiveProvince: '',
    buyerRemark: '',
    sellerRemark: '',
    lines: [],
  })

  const linkSkuOptions = ref<{ key: string; label: string; linkName: string; skuSpecName: string; listingLinkSkuId: number }[]>([])

  const statusOptions = computed(() => [
    { value: 'DRAFT', label: t('ecommerce.salesOrder.statusDraft') },
    { value: 'PAID', label: t('ecommerce.salesOrder.statusPaid') },
    { value: 'PARTIAL_SHIPPED', label: t('ecommerce.salesOrder.statusPartialShipped') },
    { value: 'SHIPPED', label: t('ecommerce.salesOrder.statusShipped') },
    { value: 'PARTIAL_REFUND', label: t('ecommerce.salesOrder.statusPartialRefund') },
    { value: 'COMPLETED', label: t('ecommerce.salesOrder.statusCompleted') },
    { value: 'REFUNDED', label: t('ecommerce.salesOrder.statusRefunded') },
    { value: 'CANCELLED', label: t('ecommerce.salesOrder.statusCancelled') },
  ])

  function emptyLine(): LineFormRow {
    return { skuQuantity: 1, linkName: '', skuSpecName: '', lineReceivedAmount: undefined }
  }

  function statusLabel(s?: string) {
    return statusOptions.value.find((o) => o.value === s)?.label ?? s ?? '—'
  }

  /** 输入收货地址后自动补齐省份（按省市区规则从地址文本解析） */
  function syncProvinceFromAddress() {
    form.receiveProvince = parseProvinceFromAddress(form.receiveAddress) ?? ''
  }

  function resetForm() {
    form.shopId = shopOptions.value[0]?.id
    form.expressStationId = undefined
    form.orderTime = `${todayDateString()} 00:00:00`
    form.payTime = form.orderTime
    form.platformStatus = '已完成'
    form.platformOrderNo = ''
    form.receivedAmount = undefined
    form.trackingNumber = ''
    form.receiveAddress = ''
    form.receiveProvince = ''
    form.buyerRemark = ''
    form.sellerRemark = ''
    form.lines = [emptyLine()]
    if (form.shopId) loadLinkSkuOptions(form.shopId)
  }

  /** 按店铺聚合可售链接及其 SKU，供下拉选择 */
  async function loadLinkSkuOptions(shopId: number) {
    linkSkuOptions.value = []
    const pageResult = await fetchListingLinks(undefined, shopId, undefined, { page: 1, pageSize: 100 })
    const opts: typeof linkSkuOptions.value = []
    for (const link of pageResult.records ?? []) {
      const d = await fetchListingLink(link.id)
      for (const sku of d.skus ?? []) {
        if (!sku.id) continue
        const key = `${d.name}|||${sku.skuName}`
        opts.push({
          key,
          label: `${d.name} · ${sku.skuName}`,
          linkName: d.name,
          skuSpecName: sku.skuName ?? '',
          listingLinkSkuId: sku.id,
        })
      }
    }
    linkSkuOptions.value = opts
  }

  function onShopChange(shopId: number) {
    if (shopId) void loadLinkSkuOptions(shopId)
  }

  // 打开对话框时确保店铺对应的 SKU 选项已加载
  watch(dialogVisible, (visible) => {
    if (visible && form.shopId) {
      void loadLinkSkuOptions(form.shopId)
    }
  })

  function addLine() {
    form.lines.push(emptyLine())
  }

  function removeLine(index: number) {
    form.lines.splice(index, 1)
  }

  function openCreate() {
    editingId.value = null
    resetForm()
    dialogVisible.value = true
  }

  async function onSave() {
    if (!form.shopId) {
      ElMessage.warning(t('ecommerce.salesOrder.shopRequired'))
      return
    }
    if (!form.orderTime) {
      ElMessage.warning(t('ecommerce.salesOrder.orderTimeRequired'))
      return
    }
    const lines = form.lines.filter((l) => l.linkName?.trim() && l.skuSpecName?.trim())
    if (!lines.length) {
      ElMessage.warning(t('ecommerce.salesOrder.linesRequired'))
      return
    }
    saving.value = true
    try {
      const payload = {
        shopId: form.shopId,
        expressStationId: form.expressStationId ?? null,
        orderTime: form.orderTime,
        payTime: form.payTime || form.orderTime,
        platformStatus: form.platformStatus || undefined,
        platformOrderNo: form.platformOrderNo || undefined,
        receivedAmount: form.receivedAmount ?? null,
        trackingNumber: form.trackingNumber || undefined,
        receiveAddress: form.receiveAddress || undefined,
        receiveProvince: form.receiveProvince || undefined,
        buyerRemark: form.buyerRemark || undefined,
        sellerRemark: form.sellerRemark || undefined,
        lines: lines.map((l, i) => ({
          listingLinkSkuId: l.listingLinkSkuId ?? null,
          linkName: l.linkName!.trim(),
          skuSpecName: l.skuSpecName!.trim(),
          skuQuantity: l.skuQuantity ?? 1,
          lineReceivedAmount: l.lineReceivedAmount ?? null,
          sortOrder: i,
        })),
      }
      if (editingId.value) {
        await updateSalesOrder(editingId.value, payload)
      } else {
        await createSalesOrder(payload)
      }
      ElMessage.success(t('ecommerce.common.saved'))
      dialogVisible.value = false
      editingId.value = null
      await load()
    } finally {
      saving.value = false
    }
  }

  return {
    dialogVisible,
    editingId,
    saving,
    form,
    linkSkuOptions,
    statusOptions,
    statusLabel,
    resetForm,
    loadLinkSkuOptions,
    onShopChange,
    addLine,
    removeLine,
    openCreate,
    onSave,
    syncProvinceFromAddress,
  }
}
