import { normalizeLineStatus } from '@/constants/importStatusMapping'
import type { ImportLineStatus } from '@/constants/importStatusMapping'

/**
 * 销售订单面板的纯展示工具函数
 *
 * <p>从 {@code SalesOrderPanel.vue} 提取：颜色映射、月份解析、导入文件大小/手工成本
 * 输入格式化、状态 tag 映射等无状态依赖的函数，便于独立单元测试。</p>
 */

/** 月度统计卡片 tone → 颜色（不含激活态） */
export function orderStatColor(tone: string): string {
  if (tone === 'green') return '#22c55e'
  if (tone === 'orange') return '#f59e0b'
  if (tone === 'purple') return '#8b5cf6'
  if (tone === 'blue') return '#2563eb'
  return '#cbd5e1'
}

/** 店铺卡片 tone → 颜色（激活态优先蓝色） */
export function orderShopColor(tone: string, active: boolean): string {
  if (active) return '#2563eb'
  if (tone === 'green') return '#22c55e'
  if (tone === 'orange') return '#f59e0b'
  if (tone === 'gray') return '#94a3b8'
  return '#cbd5e1'
}

/** 从路由 query 解析 `YYYY-MM` 月份，非法值返回 null */
export function parseOrderMonthFromQuery(raw: unknown): string | null {
  const value = typeof raw === 'string' ? raw.trim() : Array.isArray(raw) ? raw[0]?.trim() : ''
  if (!value || !/^\d{4}-\d{2}$/.test(value)) return null
  return value
}

/** 导入文件大小字节数 → 人类可读文本（B / KB / MB） */
export function formatImportFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) {
    const kb = bytes / 1024
    return kb < 10 ? `${kb.toFixed(1)} KB` : `${Math.round(kb)} KB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/**
 * 清洗手工成本输入：去非法字符、保留单个小数点、限两位小数、允许负号
 * 输入过程逐步清理，保证控件内始终为合法成本文本
 */
export function sanitizeManualCostInput(raw: string): string {
  if (!raw) return ''
  let value = raw.trim()
  let sign = ''
  if (value.startsWith('-')) {
    sign = '-'
    value = value.slice(1)
  }
  value = value.replace(/[^\d.]/g, '')
  const dotIndex = value.indexOf('.')
  if (dotIndex >= 0) {
    value = value.slice(0, dotIndex + 1) + value.slice(dotIndex + 1).replace(/\./g, '')
  }
  const [intPart = '', fracPart = ''] = value.split('.')
  const normalized = fracPart.length > 0
    ? `${intPart}.${fracPart.slice(0, 2)}`
    : intPart
  if (sign && !normalized && raw.includes('-')) return '-'
  return `${sign}${normalized}`
}

/** 成本文本 → 数字；空/符号/非法返回 undefined */
export function parseManualCostNumber(raw: string): number | undefined {
  if (!raw || raw === '-' || raw === '.' || raw === '-.') return undefined
  const num = Number(raw)
  return Number.isFinite(num) ? num : undefined
}

/** 导入行 lineStatus → el-tag type */
export function importLineStatusTagType(status?: string | null): 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  if (!status) return 'info'
  const key = normalizeLineStatus(status)
  const map: Record<ImportLineStatus, 'success' | 'warning' | 'info' | 'danger' | 'primary'> = {
    PAID: 'warning',
    SHIPPED: 'primary',
    COMPLETED: 'success',
    CANCELLED: 'info',
    PARTIAL_REFUND: 'danger',
    REFUNDED: 'danger',
    RETURNED: 'info',
  }
  return map[key]
}

/** 订单主状态 → el-tag type（未知状态返回 undefined 用默认样式） */
export function statusTagType(s?: string): 'info' | 'primary' | 'warning' | 'danger' | 'success' | undefined {
  if (s === 'DRAFT') return 'info'
  if (s === 'PAID') return 'primary'
  if (s === 'PARTIAL_SHIPPED') return 'warning'
  if (s === 'SHIPPED') return 'warning'
  if (s === 'PARTIAL_REFUND') return 'danger'
  if (s === 'REFUNDED' || s === 'CANCELLED') return 'danger'
  if (s === 'COMPLETED') return 'success'
  return undefined
}
