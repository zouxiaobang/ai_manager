export function isBaiduApiErrorBody(content: string): boolean {
  const trimmed = content?.trim() ?? ''
  if (!trimmed.startsWith('{') || !trimmed.endsWith('}')) return false
  if (trimmed.includes('<')) return false
  return trimmed.includes('"error_code"') || trimmed.includes('"errno"')
}

export function isContentLoadFailure(
  detail: { content?: string; contentExcerpt?: string; contentSize?: number },
  rawContent: string,
): boolean {
  if (isBaiduApiErrorBody(rawContent)) return true
  if ((rawContent?.trim() ?? '').length > 0) return false
  if ((detail.contentSize ?? 0) > 0) return true
  return (detail.contentExcerpt?.trim() ?? '').length > 0
}

export function htmlToPlainPreview(html: string, maxLen = 140): string {
  if (!html?.trim()) return ''
  const doc = new DOMParser().parseFromString(html, 'text/html')
  const text = doc.body.textContent?.replace(/\s+/g, ' ').trim() ?? ''
  if (!text) return ''
  return text.length > maxLen ? `${text.slice(0, maxLen)}…` : text
}

export interface NoteFolderListMeta {
  contentExcerpt?: string
  createTime?: string
  size: number
}

export function calcNoteContentSize(content: string): number {
  return new Blob([content ?? '']).size
}

export function formatFileSize(bytes: number): string {
  if (bytes <= 0) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) {
    const kb = bytes / 1024
    return kb < 10 ? `${kb.toFixed(1)} KB` : `${Math.round(kb)} KB`
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export function formatNoteCreateTime(value?: string): string {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}