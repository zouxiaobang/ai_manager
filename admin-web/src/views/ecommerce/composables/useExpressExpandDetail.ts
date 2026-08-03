import { ref, type Ref } from 'vue'
import type { EcExpressStation } from '@/api/ecommerce/express'
import { syncRowCounts } from '../expressPanelView'

/**
 * 展开行详情依赖：当前行记录（用于详情加载后回写行级计数）+ 站点详情 API。
 */
export interface ExpressExpandDetailDeps {
  /** 当前分页行列表，详情加载后同步行级计数 */
  records: Ref<EcExpressStation[]>
  fetchExpressStation: (id: number) => Promise<EcExpressStation>
}

/**
 * 展开行详情状态机：展开行 key、详情缓存、加载中集合 + 加载/失效/行样式。
 * 详情拉取后回写行级价格/公告计数；缓存命中或加载中时跳过重复请求。
 */
export function useExpressExpandDetail(deps: ExpressExpandDetailDeps) {
  const expandedRowKeys = ref<number[]>([])
  const expandDetails = ref(new Map<number, EcExpressStation>())
  const expandLoadingIds = ref(new Set<number>())

  /** 展开行详情缓存读取 */
  function getExpandDetail(id: number) {
    return expandDetails.value.get(id)
  }

  /** 行是否处于详情加载中 */
  function isExpandLoading(id: number) {
    return expandLoadingIds.value.has(id)
  }

  /** 详情失效：删除缓存并重建 Map 触发响应式 */
  function invalidateExpandDetail(id: number) {
    expandDetails.value.delete(id)
    expandDetails.value = new Map(expandDetails.value)
  }

  /** 展开行行样式：展开行高亮 */
  function rowClassName({ row }: { row: EcExpressStation }) {
    return expandedRowKeys.value.includes(row.id) ? 'express-station-row is-expanded' : 'express-station-row'
  }

  /** 拉取并缓存站点详情，成功回写行级计数 */
  async function loadExpandDetail(id: number) {
    if (expandDetails.value.has(id) || expandLoadingIds.value.has(id)) {
      return
    }
    const nextLoading = new Set(expandLoadingIds.value)
    nextLoading.add(id)
    expandLoadingIds.value = nextLoading
    try {
      const detail = await deps.fetchExpressStation(id)
      const nextDetails = new Map(expandDetails.value)
      nextDetails.set(id, detail)
      expandDetails.value = nextDetails
      const row = deps.records.value.find((item) => item.id === id)
      if (row) {
        syncRowCounts(row, detail)
      }
    } finally {
      const doneLoading = new Set(expandLoadingIds.value)
      doneLoading.delete(id)
      expandLoadingIds.value = doneLoading
    }
  }

  /** 展开状态变更：记录展开行 key，新增展开时加载详情 */
  async function onExpandChange(row: EcExpressStation, expandedRows: EcExpressStation[]) {
    expandedRowKeys.value = expandedRows.map((item) => item.id)
    if (expandedRows.some((item) => item.id === row.id)) {
      await loadExpandDetail(row.id)
    }
  }

  return {
    expandedRowKeys,
    expandDetails,
    expandLoadingIds,
    getExpandDetail,
    isExpandLoading,
    invalidateExpandDetail,
    rowClassName,
    loadExpandDetail,
    onExpandChange,
  }
}
