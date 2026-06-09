export interface PageQuery {
  page?: number
  pageSize?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
  extra?: Record<string, unknown>
}

export const DEFAULT_PAGE_SIZE = 20
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100]

export function pageParams(page: number, pageSize: number): PageQuery {
  return { page, pageSize }
}
