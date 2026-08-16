import { marked } from 'marked'

/** TOC 条目：目录锚点跳转用 */
export interface TocItem {
  id: string
  level: number
  text: string
}

/**
 * 给 v-html 渲染出的 h1-h6 补自增 id，供 TOC 锚点跳转。
 * marked v18 不再输出标题 id，富文本 HTML 一般也无 id；已带 id 的标题跳过避免覆盖用户自定义锚点。
 */
export function addHeadingIds(html: string): string {
  let counter = 0
  return html.replace(/<h([1-6])(\b[^>]*)?>/gi, (full, level, attrs) => {
    if (attrs && /\bid\s*=/i.test(attrs)) return full
    return `<h${level}${attrs || ''} id="nb-heading-${counter++}">`
  })
}

/**
 * 渲染笔记正文 HTML：与 PC 端全屏预览同源——
 * isMd 走 marked（同 NotebookView / RagDocumentPreview），富文本 HTML 原样输出，
 * 排版统一由全局 note-content.scss（.note-content-body）负责，保证两端视觉严格一致。
 * 空内容返回空串，空态文案由调用方决定。
 * 注意：md 判定由调用方按标题后缀 .md 得出（PC 端同款），后端 noteType 对 md 笔记是 "NOTE"，不能作依据。
 */
export function renderNoteBody(isMd: boolean, content?: string | null): string {
  if (!content) return ''
  const html = isMd ? (marked.parse(content) as string) : content
  return addHeadingIds(html)
}

/** 从渲染后 HTML 提取 TOC 标题列表（h1-h6，文本去标签），id 与 addHeadingIds 的自增编号对齐 */
export function parseTocItems(html: string): TocItem[] {
  if (!html?.trim()) return []
  const items: TocItem[] = []
  const regex = /<h([1-6])(?:\s[^>]*)?>([\s\S]*?)<\/h\1>/gi
  let match: RegExpExecArray | null
  let index = 0
  while ((match = regex.exec(html)) !== null) {
    const level = Number.parseInt(match[1], 10)
    const text = match[2].replace(/<[^>]+>/g, '').trim()
    if (text) {
      items.push({ id: `nb-heading-${index}`, level, text })
      index++
    }
  }
  return items
}
