import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { EcShop } from '@/api/ecommerce/shop'
import { resolveShopIconMeta } from '@/utils/shopVisual'
import {
  deleteSettlementBuyerExclude,
  fetchSettlementBuyerExcludes,
  saveSettlementBuyerExclude,
  type SettlementBuyerExclude,
} from '@/api/ecommerce/monthlySettlement'

export interface MonthlySettlementBuyerExcludeDeps {
  /** 按店铺 id 查询店铺信息（排除项图标解析用），由组件持有店铺缓存注入 */
  getShop: (shopId: number) => EcShop | undefined
}

/**
 * 结算买家排除管理域
 *
 * <p>从 {@code MonthlySettlementPanel.vue} 提取：排除项对话框的状态机
 * （列表/表单/统计/过滤/增删）与跨域共享的排除快照状态。
 * 快照 ref（buyerExcludeCount/buyerExcludesSnapshot/lastBuyerExcludeOpAt）由组件解构后
 * 供预备清单 prepTasks 与 loadPrepData 使用，保持单一数据源。</p>
 */
export function useMonthlySettlementBuyerExclude(deps: MonthlySettlementBuyerExcludeDeps) {
  const { t } = useI18n()

  const buyerExcludeVisible = ref(false)
  const loadingExcludes = ref(false)
  const savingBuyerExclude = ref(false)
  const buyerExcludes = ref<SettlementBuyerExclude[]>([])
  const excludeFormShopId = ref<number | undefined>()
  const excludeFormBuyerName = ref('')
  const excludeFormRemark = ref('')
  const excludeSearchKeyword = ref('')
  // 跨域共享：prepTasks / loadPrepData 经组件解构后引用
  const buyerExcludeCount = ref(0)
  const buyerExcludesSnapshot = ref<SettlementBuyerExclude[]>([])
  const lastBuyerExcludeOpAt = ref<string | null>(null)

  /** 对话框统计：总数 / 全店铺排除数 / 指定店铺排除数 */
  const buyerExcludeStats = computed(() => {
    const list = buyerExcludes.value
    return {
      total: list.length,
      globalCount: list.filter((item) => !item.shopId).length,
      shopCount: list.filter((item) => item.shopId).length,
    }
  })

  /** 对话框列表：按店铺名/买家名/备注关键字过滤（忽略大小写） */
  const filteredBuyerExcludes = computed(() => {
    const keyword = excludeSearchKeyword.value.trim().toLowerCase()
    if (!keyword) return buyerExcludes.value
    return buyerExcludes.value.filter((item) => {
      const shop = (item.shopName || t('ecommerce.monthlySettlement.allShops')).toLowerCase()
      const buyer = item.buyerName.toLowerCase()
      const remark = (item.remark || '').toLowerCase()
      return shop.includes(keyword) || buyer.includes(keyword) || remark.includes(keyword)
    })
  })

  /** 排除项图标：店铺缺失时按名称回退解析 */
  function getExcludeShopIconMeta(item: SettlementBuyerExclude) {
    if (!item.shopId) {
      return resolveShopIconMeta(item.shopName ?? undefined)
    }
    const shop = deps.getShop(item.shopId)
    return resolveShopIconMeta(
      item.shopName ?? shop?.name,
      shop?.platformName,
      shop?.platformCode,
      shop?.avatarUrl,
    )
  }

  /** 关闭对话框时清空表单与搜索条件 */
  function onBuyerExcludeDialogClosed() {
    excludeFormShopId.value = undefined
    excludeFormBuyerName.value = ''
    excludeFormRemark.value = ''
    excludeSearchKeyword.value = ''
  }

  /** 排除操作时间戳：供预备清单"最近操作"展示 */
  function touchBuyerExcludeOpTime() {
    lastBuyerExcludeOpAt.value = new Date().toISOString()
  }

  /** 打开对话框时全量加载排除列表并同步快照 */
  async function loadBuyerExcludes() {
    loadingExcludes.value = true
    try {
      buyerExcludes.value = await fetchSettlementBuyerExcludes()
      buyerExcludesSnapshot.value = buyerExcludes.value
      buyerExcludeCount.value = buyerExcludes.value.length
    } finally {
      loadingExcludes.value = false
    }
  }

  /**
   * 供结算预备数据并行加载：仅刷新快照计数，不触发表单 loading
   * （表单 loading 由组件预备加载状态机控制），返回加载结果供 Promise.all 使用。
   */
  async function loadSnapshot() {
    const excludes = await fetchSettlementBuyerExcludes()
    buyerExcludesSnapshot.value = excludes ?? []
    buyerExcludeCount.value = buyerExcludesSnapshot.value.length
    return excludes ?? []
  }

  /** 清空快照（结算月为空 / 预备加载失败时由组件调用） */
  function clearSnapshot() {
    buyerExcludesSnapshot.value = []
    buyerExcludeCount.value = 0
  }

  /** 新增排除项：买家名必填，保存后刷新列表并记录操作时间 */
  async function addBuyerExclude() {
    const name = excludeFormBuyerName.value.trim()
    if (!name) {
      ElMessage.warning(t('ecommerce.monthlySettlement.buyerNameRequired'))
      return
    }
    savingBuyerExclude.value = true
    try {
      await saveSettlementBuyerExclude({
        shopId: excludeFormShopId.value ?? null,
        buyerName: name,
        remark: excludeFormRemark.value.trim() || undefined,
        enabled: 1,
      })
      excludeFormBuyerName.value = ''
      excludeFormRemark.value = ''
      await loadBuyerExcludes()
      touchBuyerExcludeOpTime()
      ElMessage.success(t('ecommerce.common.saved'))
    } finally {
      savingBuyerExclude.value = false
    }
  }

  /** 删除排除项：删除后刷新列表并记录操作时间 */
  async function removeBuyerExclude(id: number) {
    await deleteSettlementBuyerExclude(id)
    await loadBuyerExcludes()
    touchBuyerExcludeOpTime()
  }

  return {
    buyerExcludeVisible,
    loadingExcludes,
    savingBuyerExclude,
    buyerExcludes,
    excludeFormShopId,
    excludeFormBuyerName,
    excludeFormRemark,
    excludeSearchKeyword,
    buyerExcludeCount,
    buyerExcludesSnapshot,
    lastBuyerExcludeOpAt,
    buyerExcludeStats,
    filteredBuyerExcludes,
    getExcludeShopIconMeta,
    onBuyerExcludeDialogClosed,
    touchBuyerExcludeOpTime,
    loadBuyerExcludes,
    loadSnapshot,
    clearSnapshot,
    addBuyerExclude,
    removeBuyerExclude,
  }
}
