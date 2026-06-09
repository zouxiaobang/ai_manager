/** 已从列对应关系配置中移除的导入字段（后端未重启时前端兜底过滤） */
export const DEPRECATED_IMPORT_FIELD_KEYS = new Set([
  'line_received_amount',
  'buyer_goods_amount',
  'pay_detail',
  'manual_cost_price',
])

export function filterImportFields<T extends { key: string }>(fields: T[]): T[] {
  return fields.filter((f) => !DEPRECATED_IMPORT_FIELD_KEYS.has(f.key))
}

export function sanitizeImportColumnMapping(mapping: Record<string, string>): Record<string, string> {
  const result: Record<string, string> = {}
  for (const [key, value] of Object.entries(mapping)) {
    if (!DEPRECATED_IMPORT_FIELD_KEYS.has(key)) {
      result[key] = value
    }
  }
  return result
}
