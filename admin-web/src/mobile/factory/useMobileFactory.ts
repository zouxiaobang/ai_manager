import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  createFactory,
  deleteFactory,
  fetchFactories,
  fetchFactoryStats,
  updateFactory,
  type EcFactory,
  type EcFactorySaveRequest,
  type EcFactoryType,
} from '@/api/ecommerce/factory'

export type FactoryTypeFilter = '' | EcFactoryType

const PAGE_SIZE = 20

function normalizeFactoryType(type?: string): EcFactoryType {
  if (type === 'CUSTOMER') return 'CUSTOMER'
  if (type === 'CARTON') return 'CARTON'
  return 'PRODUCTION'
}

export function useMobileFactory() {
  const { t } = useI18n()

  const loading = ref(false)
  const statsLoading = ref(false)
  const saving = ref(false)
  const searchQuery = ref('')
  const typeFilter = ref<FactoryTypeFilter>('')
  const records = ref<EcFactory[]>([])
  const page = ref(1)
  const total = ref(0)
  const formVisible = ref(false)
  const editingId = ref<number | null>(null)
  const deleteTarget = ref<EcFactory | null>(null)
  const deleting = ref(false)

  const stats = reactive({
    production: 0,
    customer: 0,
    carton: 0,
    enabled: 0,
    disabled: 0,
  })

  const form = reactive<EcFactorySaveRequest>({
    name: '',
    factoryType: 'PRODUCTION',
    contactName: '',
    contactPhone: '',
    address: '',
    remark: '',
    status: 'ENABLED',
  })

  const totalCount = computed(
    () => stats.production + stats.customer + stats.carton,
  )

  const summaryPillText = computed(() =>
    t('mobile.factory.summaryPill', {
      production: stats.production,
      customer: stats.customer,
      carton: stats.carton,
    }),
  )

  const totalBadgeText = computed(() =>
    t('mobile.factory.totalBadge', { count: totalCount.value }),
  )

  const filterOptions = computed(() => [
    { value: '' as FactoryTypeFilter, label: t('ecommerce.factory.filterAll') },
    { value: 'PRODUCTION' as FactoryTypeFilter, label: t('ecommerce.factory.factoryTypeProduction') },
    { value: 'CUSTOMER' as FactoryTypeFilter, label: t('ecommerce.factory.factoryTypeCustomer') },
    { value: 'CARTON' as FactoryTypeFilter, label: t('ecommerce.factory.factoryTypeCarton') },
  ])

  const hasMore = computed(() => records.value.length < total.value)

  function factoryTypeLabel(type?: string) {
    if (type === 'CUSTOMER') return t('ecommerce.factory.factoryTypeCustomer')
    if (type === 'CARTON') return t('ecommerce.factory.factoryTypeCarton')
    return t('ecommerce.factory.factoryTypeProduction')
  }

  function factoryTypeColor(type?: string) {
    if (type === 'CUSTOMER') return '#3b82f6'
    if (type === 'CARTON') return '#8b5cf6'
    return '#f97316'
  }

  function factoryContactLine(row: EcFactory) {
    const parts = [row.contactName, row.contactPhone].filter(Boolean)
    return parts.length ? parts.join(' · ') : t('mobile.factory.noContact')
  }

  function resetForm() {
    form.name = ''
    form.factoryType = 'PRODUCTION'
    form.contactName = ''
    form.contactPhone = ''
    form.address = ''
    form.remark = ''
    form.status = 'ENABLED'
  }

  async function loadStats() {
    statsLoading.value = true
    try {
      const result = await fetchFactoryStats()
      stats.production = result.productionCount
      stats.customer = result.customerCount
      stats.carton = result.cartonCount
      stats.enabled = result.enabledCount
      stats.disabled = result.disabledCount
    } finally {
      statsLoading.value = false
    }
  }

  async function loadFactories(reset = false) {
    if (reset) page.value = 1
    loading.value = true
    try {
      const result = await fetchFactories(searchQuery.value.trim() || undefined, {
        page: page.value,
        pageSize: PAGE_SIZE,
        ...(typeFilter.value ? { factoryType: typeFilter.value } : {}),
      })
      const rows = result.records ?? []
      records.value = reset || page.value === 1 ? rows : [...records.value, ...rows]
      total.value = result.total ?? rows.length
    } finally {
      loading.value = false
    }
  }

  async function init() {
    await Promise.all([loadFactories(true), loadStats()])
  }

  async function reload() {
    await Promise.all([loadFactories(true), loadStats()])
  }

  function setTypeFilter(value: FactoryTypeFilter) {
    typeFilter.value = value
    void loadFactories(true)
  }

  function onStatTypeClick(filterType: EcFactoryType) {
    typeFilter.value = typeFilter.value === filterType ? '' : filterType
    void loadFactories(true)
  }

  let searchTimer: ReturnType<typeof setTimeout> | null = null
  watch(searchQuery, () => {
    if (searchTimer) clearTimeout(searchTimer)
    searchTimer = setTimeout(() => {
      void loadFactories(true)
    }, 300)
  })

  function openCreate() {
    editingId.value = null
    resetForm()
    formVisible.value = true
  }

  function openEdit(row: EcFactory) {
    editingId.value = row.id
    form.name = row.name
    form.factoryType = normalizeFactoryType(row.factoryType)
    form.contactName = row.contactName || ''
    form.contactPhone = row.contactPhone || ''
    form.address = row.address || ''
    form.remark = row.remark || ''
    form.status = row.status
    formVisible.value = true
  }

  async function onSave() {
    if (!form.name.trim()) {
      ElMessage.warning(t('ecommerce.factory.nameRequired'))
      return
    }
    saving.value = true
    try {
      const payload: EcFactorySaveRequest = {
        name: form.name.trim(),
        factoryType: form.factoryType,
        contactName: form.contactName?.trim() || undefined,
        contactPhone: form.contactPhone?.trim() || undefined,
        address: form.address?.trim() || undefined,
        remark: form.remark?.trim() || undefined,
        status: form.status,
      }
      if (editingId.value) {
        await updateFactory(editingId.value, payload)
      } else {
        await createFactory(payload)
      }
      ElMessage.success(t('ecommerce.common.saved'))
      formVisible.value = false
      await reload()
    } finally {
      saving.value = false
    }
  }

  function onDelete(row: EcFactory) {
    deleteTarget.value = row
  }

  function cancelDelete() {
    deleteTarget.value = null
  }

  async function confirmDelete() {
    if (!deleteTarget.value || deleting.value) return
    deleting.value = true
    try {
      await deleteFactory(deleteTarget.value.id)
      ElMessage.success(t('ecommerce.common.deleted'))
      deleteTarget.value = null
      await reload()
    } finally {
      deleting.value = false
    }
  }

  function loadMore() {
    if (loading.value || !hasMore.value) return
    page.value += 1
    void loadFactories(false)
  }

  return {
    t,
    loading,
    statsLoading,
    saving,
    searchQuery,
    typeFilter,
    records,
    total,
    stats,
    form,
    formVisible,
    editingId,
    deleteTarget,
    deleting,
    totalCount,
    summaryPillText,
    totalBadgeText,
    filterOptions,
    hasMore,
    factoryTypeLabel,
    factoryTypeColor,
    factoryContactLine,
    init,
    reload,
    setTypeFilter,
    onStatTypeClick,
    openCreate,
    openEdit,
    onSave,
    onDelete,
    cancelDelete,
    confirmDelete,
    loadMore,
  }
}
