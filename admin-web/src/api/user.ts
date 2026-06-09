import { getData } from './request'

import type { PageQuery, PageResult } from './pagination'

import type { SysUser } from './types'



export function fetchUsers(pageQuery?: PageQuery) {

  return getData<PageResult<SysUser>>('/api/system/users', pageQuery ?? {})

}

