export type ImportLineStatus =
  | 'PAID'
  | 'SHIPPED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'PARTIAL_REFUND'
  | 'REFUNDED'
  | 'RETURNED'

export interface StatusMappingRow {
  key: string
  platformStatus: string
  systemStatus: ImportLineStatus
}

/** 与后端 EcImportStatusSupport 内置映射保持一致 */
export const DEFAULT_STATUS_MAPPING: Record<string, ImportLineStatus> = {
  交易成功: 'COMPLETED',
  交易关闭: 'CANCELLED',
  确认收货: 'COMPLETED',
  '卖家已发货，等待买家确认': 'SHIPPED',
  等待买家确认收货: 'SHIPPED',
  卖家已发货: 'SHIPPED',
  等待买家确认: 'SHIPPED',
  '买家已付款，等待卖家发货': 'PAID',
  买家已付款: 'PAID',
  等待卖家发货: 'PAID',
  待发货: 'PAID',
  已关闭: 'CANCELLED',
  已发货: 'SHIPPED',
  已完成: 'COMPLETED',
  已退款: 'REFUNDED',
  退款成功: 'REFUNDED',
  部分退款: 'PARTIAL_REFUND',
  退款中: 'REFUNDED',
  退货退款: 'RETURNED',
  已取消: 'CANCELLED',
}

export function mappingToRows(map?: Record<string, string> | null): StatusMappingRow[] {
  const source = map && Object.keys(map).length > 0 ? map : DEFAULT_STATUS_MAPPING
  return Object.entries(source).map(([platformStatus, systemStatus], index) => ({
    key: `status-${index}-${platformStatus}`,
    platformStatus,
    systemStatus: normalizeLineStatus(systemStatus),
  }))
}

export function rowsToMapping(rows: StatusMappingRow[]): Record<string, string> {
  const result: Record<string, string> = {}
  for (const row of rows) {
    const key = row.platformStatus.trim()
    if (key) {
      result[key] = row.systemStatus
    }
  }
  return result
}

export function normalizeLineStatus(value?: string | null): ImportLineStatus {
  const upper = (value ?? 'PAID').trim().toUpperCase()
  if (upper === 'SHIPPED') return 'SHIPPED'
  if (upper === 'COMPLETED') return 'COMPLETED'
  if (upper === 'CANCELLED') return 'CANCELLED'
  if (upper === 'PARTIAL_REFUND') return 'PARTIAL_REFUND'
  if (upper === 'REFUNDED') return 'REFUNDED'
  if (upper === 'RETURNED') return 'RETURNED'
  return 'PAID'
}
