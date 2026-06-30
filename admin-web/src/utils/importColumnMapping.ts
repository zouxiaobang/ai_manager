import type { SysImportField } from '@/api/sys/import'

/** 与 sql/sys_import.sql 内置模板一致 */
const TAOBAO_COLUMN_MAPPING: Record<string, string> = {
  platform_order_no: '订单编号',
  order_time: '买家下单时间',
  pay_time: '买家付款时间',
  ship_time: '发货时间',
  complete_time: '确认收货时间',
  express_station_name: '物流公司',
  received_amount: '买家实付金额',
  tracking_number: '运单号',
  buyer_name: '买家会员名',
  buyer_phone: '联系手机',
  receive_address: '收货地址',
  link_name: '宝贝标题',
  sku_spec_name: '宝贝规格',
  sku_quantity: '宝贝总数量',
  platform_line_status: '订单状态',
  platform_status: '订单状态',
}

const ALIBABA_1688_COLUMN_MAPPING: Record<string, string> = {
  platform_order_no: '订单号',
  order_time: '下单时间',
  express_station_name: '物流公司',
  received_amount: '实付款',
  tracking_number: '运单号',
  buyer_name: '买家',
  receive_address: '收货地址',
  link_name: '货品标题',
  sku_spec_name: '规格',
  sku_quantity: '数量',
  platform_line_status: '退款状态',
  platform_status: '订单状态',
}

function resolvePlatformTemplate(platformName?: string): Record<string, string> {
  const name = (platformName ?? '').trim()
  if (!name) return {}
  if (/淘宝|taobao/i.test(name)) return { ...TAOBAO_COLUMN_MAPPING }
  if (/1688|阿里巴巴|alibaba/i.test(name)) return { ...ALIBABA_1688_COLUMN_MAPPING }
  return {}
}

function matchStatusColumn(docColumns: string[], fieldKey: string): string {
  const findCol = (...names: string[]) => {
    for (const name of names) {
      const hit = docColumns.find((c) => c.trim() === name)
      if (hit) return hit
    }
    for (const name of names) {
      const hit = docColumns.find((c) => c.trim().includes(name))
      if (hit) return hit
    }
    return ''
  }
  if (fieldKey === 'platform_line_status') {
    return findCol('退款状态') || findCol('订单状态', '交易状态', '当前订单状态', '子订单状态')
  }
  if (fieldKey === 'platform_status') {
    return findCol('订单状态', '交易状态', '当前订单状态', '平台订单状态')
  }
  return ''
}

export function autoMatchColumnMapping(
  fields: SysImportField[],
  docColumns: string[],
  existing: Record<string, string> = {},
): Record<string, string> {
  const mapping: Record<string, string> = {}
  for (const f of fields) {
    if (Object.prototype.hasOwnProperty.call(existing, f.key)) {
      mapping[f.key] = existing[f.key] ?? ''
      continue
    }
    if (f.key === 'platform_line_status' || f.key === 'platform_status') {
      mapping[f.key] = matchStatusColumn(docColumns, f.key)
      continue
    }
    const zh = f.labelZh.trim()
    const en = f.labelEn.trim()
    const hit = docColumns.find((c) => {
      const col = c.trim()
      if (col === zh || col === en) return true
      // 短标签不做 contains 模糊匹配，避免「买家」误匹配「买家应付货款」
      const minLen = Math.min(zh.length, en.length)
      if (minLen >= 3 && (col.includes(zh) || col.includes(en))) return true
      return false
    })
    mapping[f.key] = hit ?? ''
  }
  return mapping
}

/** 上传前构建列映射：优先平台内置模板，再按表头自动匹配 */
export function buildColumnMappingForUpload(
  fields: SysImportField[],
  docColumns: string[],
  platformName?: string,
): Record<string, string> {
  const template = resolvePlatformTemplate(platformName)
  const preset: Record<string, string> = {}
  for (const f of fields) {
    const col = template[f.key]?.trim()
    if (col && docColumns.includes(col)) {
      preset[f.key] = col
    }
  }
  return autoMatchColumnMapping(fields, docColumns, preset)
}
