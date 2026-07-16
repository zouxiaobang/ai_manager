import { getData, putData } from './request'

import type { PageQuery, PageResult } from './pagination'

import type { SysUser } from './types'

export function fetchUsers(pageQuery?: PageQuery) {
  return getData<PageResult<SysUser>>('/api/system/users', pageQuery ?? {})
}

export function fetchUser(id: number) {
  return getData<SysUser>(`/api/system/users/${id}`)
}

export function updateUser(id: number, data: Partial<Pick<SysUser, 'nickname'>>) {
  return putData<void>(`/api/system/users/${id}`, data)
}

