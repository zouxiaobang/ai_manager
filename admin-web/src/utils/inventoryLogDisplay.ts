import type { EcInventoryLog } from '@/api/ecommerce/inventory'

export type InventoryLogChangeTone = 'danger' | 'success' | 'primary' | 'warning' | 'info'

export interface InventoryLogChangeStyle {
  tagType: InventoryLogChangeTone
  color: string
}

export interface EnrichedInventoryLog extends EcInventoryLog {
  balance: number
}

export interface LogRemarkParts {
  prefix: string
  orderNo: string
  suffix?: string
  orderKind: 'inbound' | 'outbound'
}

export function getInventoryLogChangeStyle(changeType: string): InventoryLogChangeStyle {
  switch (changeType) {
    case 'DEDUCT':
      return { tagType: 'danger', color: '#dc2626' }
    case 'RECLAIM':
      return { tagType: 'success', color: '#16a34a' }
    case 'INBOUND':
      return { tagType: 'primary', color: '#2563eb' }
    case 'STOCKTAKE':
      return { tagType: 'warning', color: '#d97706' }
    default:
      return { tagType: 'info', color: '#6b7280' }
  }
}

function parseStocktakeBookQty(remark?: string): number | null {
  const match = remark?.match(/账面(\d+)/)
  if (!match) return null
  const value = Number(match[1])
  return Number.isFinite(value) ? value : null
}

function reverseLogQuantity(after: number, log: EcInventoryLog): number {
  const qty = log.changeQty ?? 0
  if (log.changeType === 'DEDUCT') return after + qty
  if (log.changeType === 'RECLAIM' || log.changeType === 'INBOUND') return after - qty
  if (log.changeType === 'STOCKTAKE') {
    const bookQty = parseStocktakeBookQty(log.remark)
    if (bookQty != null) return bookQty
    const increasedBefore = after - qty
    if (increasedBefore >= 0) return increasedBefore
    return after + qty
  }
  return after
}

export function enrichInventoryLogs(logs: EcInventoryLog[], currentQuantity: number): EnrichedInventoryLog[] {
  if (!logs.length) return []
  const sorted = [...logs].sort((a, b) => (b.id ?? 0) - (a.id ?? 0))
  let running = currentQuantity
  return sorted.map((log) => {
    const balance = running
    running = reverseLogQuantity(running, log)
    return { ...log, balance }
  })
}

export function parseInventoryLogRemark(remark?: string, refType?: string): LogRemarkParts | null {
  if (!remark?.trim()) return null

  const inboundMatch = /^进货单\s+(\S+)(.*)$/.exec(remark.trim())
  if (inboundMatch || refType === 'INBOUND_ORDER') {
    const orderNo = inboundMatch?.[1] ?? extractOrderNo(remark)
    if (!orderNo) return null
    return {
      prefix: '进货单 ',
      orderNo,
      suffix: inboundMatch?.[2]?.trim() || undefined,
      orderKind: 'inbound',
    }
  }

  const outboundMatch = /^出货单\s+(\S+)(.*)$/.exec(remark.trim())
  if (outboundMatch || refType === 'OUTBOUND_ORDER') {
    const orderNo = outboundMatch?.[1] ?? extractOrderNo(remark)
    if (!orderNo) return null
    return {
      prefix: '出货单 ',
      orderNo,
      suffix: outboundMatch?.[2]?.trim() || undefined,
      orderKind: 'outbound',
    }
  }

  return null
}

function extractOrderNo(remark: string): string | undefined {
  const match = /\b(IN|OUT)\d+\b/.exec(remark)
  return match?.[0]
}
