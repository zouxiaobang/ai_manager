const STORAGE_KEY = 'ec-inventory-weekly-snapshots'

export interface InventoryWeeklySnapshot {
  weekKey: string
  factoryKey: string
  skuCount: number
  totalQty: number
  stockValue: number
  inboundValue: number
  alertCount: number
  updatedAt: string
}

export interface InventoryWeekCompare {
  direction: 'up' | 'down' | 'flat'
  text: string
  tone: 'good' | 'bad' | 'muted'
}

function factoryKey(factoryId?: number | null) {
  return factoryId == null ? 'all' : String(factoryId)
}

export function getIsoWeekKey(date = new Date()): string {
  const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
  const dayNum = d.getUTCDay() || 7
  d.setUTCDate(d.getUTCDate() + 4 - dayNum)
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1))
  const weekNo = Math.ceil(((d.getTime() - yearStart.getTime()) / 86_400_000 + 1) / 7)
  return `${d.getUTCFullYear()}-W${String(weekNo).padStart(2, '0')}`
}

export function getPreviousIsoWeekKey(date = new Date()): string {
  const prev = new Date(date)
  prev.setDate(prev.getDate() - 7)
  return getIsoWeekKey(prev)
}

function readSnapshots(): InventoryWeeklySnapshot[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw) as InventoryWeeklySnapshot[]
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function writeSnapshots(items: InventoryWeeklySnapshot[]) {
  const trimmed = items.slice(-104)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(trimmed))
}

export function saveInventoryWeeklySnapshot(input: {
  factoryId?: number | null
  skuCount: number
  totalQty: number
  stockValue: number
  inboundValue: number
  alertCount: number
}) {
  const weekKey = getIsoWeekKey()
  const key = factoryKey(input.factoryId)
  const items = readSnapshots()
  const next: InventoryWeeklySnapshot = {
    weekKey,
    factoryKey: key,
    skuCount: input.skuCount,
    totalQty: input.totalQty,
    stockValue: input.stockValue,
    inboundValue: input.inboundValue,
    alertCount: input.alertCount,
    updatedAt: new Date().toISOString(),
  }
  const index = items.findIndex((item) => item.weekKey === weekKey && item.factoryKey === key)
  if (index >= 0) {
    items[index] = next
  } else {
    items.push(next)
  }
  writeSnapshots(items)
}

export function getPreviousWeekSnapshot(factoryId?: number | null): InventoryWeeklySnapshot | null {
  const weekKey = getPreviousIsoWeekKey()
  const key = factoryKey(factoryId)
  return readSnapshots().find((item) => item.weekKey === weekKey && item.factoryKey === key) ?? null
}

function formatDelta(delta: number, mode: 'absolute' | 'percent', unit = '') {
  if (mode === 'percent') {
    const sign = delta > 0 ? '+' : ''
    return `${sign}${delta.toFixed(1)}%`
  }
  const rounded = Math.round(delta)
  if (unit) return `${Math.abs(rounded)}${unit}`
  return String(Math.abs(rounded))
}

function buildCompare(
  current: number,
  previous: number,
  mode: 'absolute' | 'percent',
  unit: string,
  invertTone: boolean,
): InventoryWeekCompare | null {
  if (previous <= 0 && current <= 0) {
    return { direction: 'flat', text: '0', tone: 'muted' }
  }
  if (previous <= 0) {
    return { direction: 'up', text: mode === 'percent' ? '+100%' : formatDelta(current, 'absolute', unit), tone: invertTone ? 'bad' : 'good' }
  }

  const delta = current - previous
  if (Math.abs(delta) < 0.0001) {
    return { direction: 'flat', text: mode === 'percent' ? '0%' : `0${unit}`, tone: 'muted' }
  }

  const direction = delta > 0 ? 'up' : 'down'
  const text =
    mode === 'percent'
      ? formatDelta((delta / previous) * 100, 'percent')
      : formatDelta(delta, 'absolute', unit)

  let tone: InventoryWeekCompare['tone'] = 'muted'
  if (direction === 'up') tone = invertTone ? 'bad' : 'good'
  if (direction === 'down') tone = invertTone ? 'good' : 'bad'

  return { direction, text, tone }
}

export function buildInventoryWeekCompares(
  current: {
    skuCount: number
    totalQty: number
    stockValue: number
    inboundValue: number
    alertCount: number
  },
  previous: InventoryWeeklySnapshot | null,
): Record<string, InventoryWeekCompare | null> {
  if (!previous) {
    return { sku: null, qty: null, value: null, inbound: null, alert: null }
  }
  return {
    sku: buildCompare(current.skuCount, previous.skuCount, 'absolute', '', false),
    qty: buildCompare(current.totalQty, previous.totalQty, 'percent', '', false),
    value: buildCompare(current.stockValue, previous.stockValue, 'percent', '', false),
    inbound: buildCompare(current.inboundValue, previous.inboundValue ?? 0, 'percent', '', false),
    alert: buildCompare(current.alertCount, previous.alertCount, 'absolute', '', true),
  }
}
