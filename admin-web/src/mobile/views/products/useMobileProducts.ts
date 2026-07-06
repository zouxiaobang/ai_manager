import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  fetchProducts,
  fetchProduct,
  deleteProduct,
  type EcProductListItem,
  type EcProductDetail,
} from '@/api/ecommerce/product'
import { fetchFactories, type EcFactory } from '@/api/ecommerce/factory'

const PAGE_SIZE = 50

export interface ProductsGroupedByFactory {
  factoryId: number | null | undefined
  factoryName: string
  factoryStatus: string
  products: EcProductListItem[]
}

export function useMobileProducts() {
  const { t } = useI18n()

  // ---- State ----
  const loading = ref(false)
  const searchQuery = ref('')
  const records = ref<EcProductListItem[]>([])
  const total = ref(0)
  const page = ref(1)

  const factories = ref<EcFactory[]>([])
  const selectedFactoryId = ref<number | 'all'>('all')

  const detailProduct = ref<EcProductDetail | null>(null)
  const detailLoading = ref(false)
  const detailVisible = ref(false)

  const stats = reactive({
    totalFactories: 0,
    totalProducts: 0,
    totalSkus: 0,
    enabledCount: 0,
    disabledCount: 0,
  })

  // ---- Computed ----
  const hasMore = computed(() => records.value.length < total.value)

  const groupedByFactory = computed<ProductsGroupedByFactory[]>(() => {
    const map = new Map<number | null, ProductsGroupedByFactory>()

    for (const prod of records.value) {
      const key = prod.factoryId ?? -1 // treat null as -1
      if (!map.has(key)) {
        const fac = factories.value.find(f => f.id === prod.factoryId)
        map.set(key, {
          factoryId: prod.factoryId,
          factoryName: prod.factoryName || fac?.name || '未知工厂',
          factoryStatus: fac?.status || 'ENABLED',
          products: [],
        })
      }
      const group = map.get(key)!
      // Only add each SPU once per factory
      if (!group.products.some(p => p.id === prod.id)) {
        group.products.push(prod)
      }
    }

    // Filter by selected factory
    const entries = Array.from(map.values())
    if (selectedFactoryId.value === 'all') return entries
    return entries.filter(g => g.factoryId === selectedFactoryId.value)
  })

  const factoryOptions = computed(() => [
    { value: 'all' as const, label: t('ecommerce.factory.filterAll'), productCount: records.value.length },
    ...factories.value
      .filter(f => f.status === 'ENABLED')
      .map(f => {
        const count = records.value.filter(p => p.factoryId === f.id).length
        return { value: f.id, label: f.name, productCount: count }
      }),
  ])

  // ---- Data Loading ----
  async function loadProducts(reset = false) {
    if (reset) page.value = 1
    loading.value = true
    try {
      const result = await fetchProducts(searchQuery.value.trim() || undefined, {
        page: page.value,
        pageSize: PAGE_SIZE,
      })
      const rows = result.records ?? []
      records.value = reset || page.value === 1 ? rows : [...records.value, ...rows]
      total.value = result.total ?? rows.length
      computeStats()
    } finally {
      loading.value = false
    }
  }

  async function loadFactoriesList() {
    try {
      const result = await fetchFactories(undefined, { page: 1, pageSize: 200 })
      factories.value = result.records ?? []
    } catch {
      factories.value = []
    }
  }

  function computeStats() {
    const uniqueFactoryIds = new Set(records.value.map(p => p.factoryId).filter(Boolean))
    stats.totalFactories = uniqueFactoryIds.size
    stats.totalProducts = records.value.length
    stats.totalSkus = records.value.reduce((sum, p) => sum + (p.skuCount || 0), 0)
    stats.enabledCount = records.value.filter(p => p.status === 'ENABLED').length
    stats.disabledCount = records.value.filter(p => p.status !== 'ENABLED').length
  }

  async function loadProductDetail(id: number) {
    detailLoading.value = true
    detailVisible.value = true
    try {
      detailProduct.value = await fetchProduct(id)
    } finally {
      detailLoading.value = false
    }
  }

  function closeDetail() {
    detailVisible.value = false
    detailProduct.value = null
  }

  // ---- Search debounce ----
  let searchTimer: ReturnType<typeof setTimeout> | null = null
  watch(searchQuery, () => {
    if (searchTimer) clearTimeout(searchTimer)
    searchTimer = setTimeout(() => {
      void loadProducts(true)
    }, 300)
  })

  // ---- Init ----
  async function init() {
    await Promise.all([loadProducts(true), loadFactoriesList()])
  }

  async function reload() {
    await Promise.all([loadProducts(true), loadFactoriesList()])
  }

  function setFactoryFilter(factoryId: number | 'all') {
    selectedFactoryId.value = factoryId
  }

  function loadMore() {
    if (loading.value || !hasMore.value) return
    page.value += 1
    void loadProducts(false)
  }

  // ---- Delete ----
  async function onDeleteProduct(id: number) {
    try {
      await deleteProduct(id)
      await reload()
    } catch {
      // handled by HTTP interceptor
    }
  }

  return {
    t,
    // State
    loading,
    searchQuery,
    records,
    total,
    factories,
    selectedFactoryId,
    stats,
    detailProduct,
    detailLoading,
    detailVisible,
    // Computed
    hasMore,
    groupedByFactory,
    factoryOptions,
    // Methods
    init,
    reload,
    loadProducts,
    setFactoryFilter,
    loadProductDetail,
    closeDetail,
    loadMore,
    onDeleteProduct,
  }
}
