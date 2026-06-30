import {
  BIZ_SALES_ORDER,
  BIZ_SETTLEMENT_EXPRESS_BILL,
  type SysImportField,
} from '@/api/sys/import'

export interface ImportFieldGroupDef {
  id: string
  labelZh: string
  labelEn: string
  keys: string[]
}

export const SALES_ORDER_FIELD_GROUPS: ImportFieldGroupDef[] = [
  {
    id: 'order_info',
    labelZh: '订单信息',
    labelEn: 'Order info',
    keys: ['platform_order_no', 'order_time', 'pay_time', 'ship_time', 'complete_time'],
  },
  {
    id: 'amount_logistics',
    labelZh: '金额与物流',
    labelEn: 'Amount & logistics',
    keys: ['express_station_name', 'received_amount', 'tracking_number'],
  },
  {
    id: 'product',
    labelZh: '商品信息',
    labelEn: 'Product info',
    keys: ['link_name', 'sku_spec_name', 'sku_quantity'],
  },
  {
    id: 'status_remark',
    labelZh: '状态与备注',
    labelEn: 'Status & remarks',
    keys: [
      'buyer_name',
      'buyer_phone',
      'receive_address',
      'buyer_remark',
      'seller_remark',
      'platform_status',
      'platform_line_status',
    ],
  },
]

export const SETTLEMENT_EXPRESS_BILL_FIELD_GROUPS: ImportFieldGroupDef[] = [
  {
    id: 'bill_info',
    labelZh: '账单信息',
    labelEn: 'Bill info',
    keys: [
      'tracking_number',
      'freight_amount',
      'settlement_destination',
      'weight',
      'ship_time',
    ],
  },
]

export interface GroupedImportField {
  id: string
  labelZh: string
  labelEn: string
  fields: SysImportField[]
}

function getGroupDefs(bizType: string): ImportFieldGroupDef[] {
  if (bizType === BIZ_SETTLEMENT_EXPRESS_BILL) {
    return SETTLEMENT_EXPRESS_BILL_FIELD_GROUPS
  }
  if (bizType === BIZ_SALES_ORDER) {
    return SALES_ORDER_FIELD_GROUPS
  }
  return []
}

export function groupImportFields(fields: SysImportField[], bizType: string): GroupedImportField[] {
  const defs = getGroupDefs(bizType)
  const fieldMap = new Map(fields.map((f) => [f.key, f]))
  const assigned = new Set<string>()
  const grouped: GroupedImportField[] = []

  for (const def of defs) {
    const groupFields = def.keys
      .map((key) => fieldMap.get(key))
      .filter((f): f is SysImportField => !!f)
    groupFields.forEach((f) => assigned.add(f.key))
    if (groupFields.length) {
      grouped.push({ ...def, fields: groupFields })
    }
  }

  const rest = fields.filter((f) => !assigned.has(f.key))
  if (rest.length) {
    grouped.push({
      id: 'other',
      labelZh: '其他',
      labelEn: 'Other',
      fields: rest,
    })
  }

  if (!grouped.length && fields.length) {
    grouped.push({
      id: 'all',
      labelZh: '全部字段',
      labelEn: 'All fields',
      fields: [...fields],
    })
  }

  return grouped
}
