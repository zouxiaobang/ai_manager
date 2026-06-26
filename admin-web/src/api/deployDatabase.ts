import { getData, postData } from './request'

export interface DeployDatabaseColumn {
  columnName: string
  columnType: string
  nullable: boolean
  columnKey: string
  columnComment: string
  columnDefault?: string
  ordinalPosition: number
}

export interface DeployDatabaseTable {
  tableName: string
  tableComment: string
  engine: string
  rowCount: number
  columnCount: number
  columns: DeployDatabaseColumn[]
}

export interface DeployDatabaseSnapshot {
  databaseName: string
  syncedAt: string
  syncedAtEpochMs: number
  tableCount: number
  tables: DeployDatabaseTable[]
}

export function fetchDeployDatabaseSnapshot() {
  return getData<DeployDatabaseSnapshot>('/api/deploy/database')
}

export function syncDeployDatabaseSnapshot() {
  return postData<DeployDatabaseSnapshot>('/api/deploy/database/sync', undefined, { timeout: 120000 })
}
