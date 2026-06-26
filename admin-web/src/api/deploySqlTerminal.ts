import { postData } from './request'

export type SqlTerminalTarget = 'local' | 'node118'

export interface DeploySqlBatchItem {
  index: number
  sql: string
  affectedRows: number
}

export interface DeploySqlExecuteResult {
  target: string
  targetLabel: string
  sql: string
  statementType: 'query' | 'update' | 'batch' | string
  durationMs: number
  affectedRows: number
  rowCount: number
  statementCount: number
  columns: string[]
  rows: unknown[][]
  batchItems: DeploySqlBatchItem[]
  message: string
}

export function executeDeploySql(target: SqlTerminalTarget, sql: string) {
  return postData<DeploySqlExecuteResult>(
    '/api/deploy/database/sql/execute',
    { target, sql },
    { timeout: 120000 },
  )
}
