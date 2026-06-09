import { ref, type Ref } from 'vue'
import { DEFAULT_PAGE_SIZE, type PageResult } from '@/api/pagination'

export function usePagination<T>(
  loader: (page: number, pageSize: number) => Promise<PageResult<T>>,
) {
  const page = ref(1)
  const pageSize = ref(DEFAULT_PAGE_SIZE)
  const total = ref(0)
  const records = ref([]) as Ref<T[]>
  const extra = ref<Record<string, unknown> | undefined>()
  const loading = ref(false)

  async function load(resetPage = false) {
    if (resetPage) {
      page.value = 1
    }
    loading.value = true
    try {
      const result = await loader(page.value, pageSize.value)
      records.value = result.records
      total.value = result.total
      page.value = result.page
      pageSize.value = result.pageSize
      extra.value = result.extra
    } finally {
      loading.value = false
    }
  }

  function onPageChange(value: number) {
    page.value = value
    load()
  }

  function onSizeChange(value: number) {
    pageSize.value = value
    load(true)
  }

  return {
    page,
    pageSize,
    total,
    records,
    extra,
    loading,
    load,
    onPageChange,
    onSizeChange,
  }
}
