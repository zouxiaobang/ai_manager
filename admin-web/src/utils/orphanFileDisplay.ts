const IMAGE_EXTENSIONS = new Set([
  '.png',
  '.jpg',
  '.jpeg',
  '.gif',
  '.webp',
  '.bmp',
  '.svg',
  '.ico',
])

export type OrphanFileTypeKind = 'xlsx' | 'xls' | 'csv' | 'html' | 'json' | 'file'

export function isOrphanImageFile(fileName: string): boolean {
  const lower = fileName.trim().toLowerCase()
  const dot = lower.lastIndexOf('.')
  if (dot < 0) return false
  return IMAGE_EXTENSIONS.has(lower.slice(dot))
}

export function resolveOrphanFileTypeKind(fileName: string): OrphanFileTypeKind {
  const lower = fileName.trim().toLowerCase()
  if (lower.endsWith('.xlsx')) return 'xlsx'
  if (lower.endsWith('.xls')) return 'xls'
  if (lower.endsWith('.csv')) return 'csv'
  if (lower.endsWith('.html')) return 'html'
  if (lower.endsWith('.json')) return 'json'
  return 'file'
}

export function resolveOrphanFilePreviewUrl(
  zoneKey: string,
  fileName: string,
  relativePath: string,
): string {
  if (!isOrphanImageFile(fileName)) {
    return ''
  }

  const base = import.meta.env.VITE_API_BASE || ''
  const encodePath = (value: string) => value.split('/').map(encodeURIComponent).join('/')
  const path = relativePath?.trim() || fileName

  switch (zoneKey) {
    case 'ECOMMERCE_IMAGES':
    case 'IMPORT_FILES':
      return `${base}/uploads/ecommerce/${encodePath(path)}`
    case 'NOTEBOOK_IMAGES':
      return `${base}/uploads/notebook/images/${encodePath(fileName)}`
    default:
      return ''
  }
}
