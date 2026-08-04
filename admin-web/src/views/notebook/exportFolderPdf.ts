import { marked } from 'marked'
import type { NbNoteDetail, NbTreeNode } from '@/api/notebook'
import { fetchNote } from '@/api/notebook'
// Vite `?inline` 拿到 note-content.scss 编译后的 CSS 字符串，内联进打印窗口，
// 保证导出 PDF 与「全屏预览」共用同一份正文样式，视觉严格一致。
import noteContentCss from '@/styles/note-content.scss?inline'

/** 并发拉取正文的最大并发数：百度网盘冷读慢，过高的并发容易触发限流或拖垮连接 */
const FETCH_CONCURRENCY = 4

/** 导出结果：exported 成功篇数，failed 失败篇清单（失败篇不进 PDF，仅提示） */
export interface FolderPdfExportResult {
  exported: number
  failed: Array<{ noteId: number; title: string }>
}

/** 进度回调：done 已处理篇数，total 总篇数 */
export type FolderPdfProgress = (done: number, total: number) => void

/** 单篇正文拉取函数签名，便于单测注入替身 */
export type NoteFetcher = (id: number) => Promise<NbNoteDetail>

/** 收集某文件夹「直属」的笔记节点（不含子文件夹）。noteId 缺失的节点视为无效，跳过。 */
export function collectDirectNotes(folderNode: NbTreeNode): NbTreeNode[] {
  return (folderNode.children ?? []).filter((n) => n.nodeType === 'NOTE' && n.noteId != null)
}

/** 中文数字：个位与「两」 */
const CN_DIGIT: Record<string, number> = {
  零: 0,
  〇: 0,
  一: 1,
  二: 2,
  三: 3,
  四: 4,
  五: 5,
  六: 6,
  七: 7,
  八: 8,
  九: 9,
  两: 2,
}

/** 中文数字：数量单位 */
const CN_UNIT: Record<string, number> = {
  十: 10,
  百: 100,
  千: 1000,
  万: 10000,
  亿: 100000000,
}

/**
 * 解析标题开头的中文数字，如「一」「十三」「二十一」「一百零五」。
 * 采用「万/亿 分段 + 当前段 digit×unit 累加」的常规算法；「十/百/千」开头时隐式补 1（十三=13）。
 * 返回解析出的数值；开头不是中文数字则返回 null。
 */
export function parseLeadingChineseNumber(name: string): number | null {
  let total = 0 // 已累加的亿/万大段
  let section = 0 // 当前万以下段
  let digit = 0 // 当前个位
  let count = 0 // 已消费的中文数字字符数，用于判定「以中文数字开头」
  for (let pos = 0; pos < name.length; pos++) {
    const ch = name[pos]
    if (ch in CN_DIGIT) {
      digit = CN_DIGIT[ch]
      section += digit
      digit = 0
      count += 1
    } else if (ch in CN_UNIT) {
      // 「十/百/千」开头时隐式补 1（如「十三」= 13）
      if (section === 0 && digit === 0 && count === 0) {
        section = 1
      }
      const unit = CN_UNIT[ch]
      if (unit >= 10000) {
        total += (section + digit) * unit
        section = 0
      } else {
        section = (section + digit) * unit
        digit = 0
      }
      count += 1
    } else {
      break
    }
  }
  if (count === 0) return null
  return total + section + digit
}

/** 提取标题开头的数字前缀：阿拉伯数字或中文数字，返回数值；开头不是数字则返回 null */
export function parseLeadingNumber(name: string): number | null {
  const trimmed = name.trim()
  const arabic = /^(\d+)/.exec(trimmed)
  if (arabic) return Number(arabic[1])
  return parseLeadingChineseNumber(trimmed)
}

/**
 * 按名称排序，规则（与文件管理器直觉一致）：
 * 1. 以数字开头（阿拉伯或中文，如「一、概述」「第十章」「note2」）的标题排最前，内部按数值升序；
 * 2. 其余标题保持树接口原始顺序（isPinned→sortOrder→id，即界面摆放顺序），不按拼音。
 */
export function sortNotesByName(notes: NbTreeNode[]): NbTreeNode[] {
  const indexed = notes.map((node, index) => {
    const num = parseLeadingNumber(node.name)
    return { node, index, num }
  })
  // 数字标题：按数值升序，同值保持原始相对顺序（稳定）
  const numbered = indexed
    .filter((item) => item.num != null)
    .sort((a, b) => (a.num as number) - (b.num as number) || a.index - b.index)
  // 非数字标题：保持传入顺序（即树接口顺序）
  const plain = indexed.filter((item) => item.num == null)
  return [...numbered, ...plain].map((item) => item.node)
}

/**
 * 并发受限地逐篇拉取详情，返回顺序与入参一致；单篇失败以 detail=null 标记，不阻断整体。
 * 这是纯函数编排，fetcher 可注入，方便单测模拟成功/失败。
 */
export async function fetchNotesConcurrently(
  notes: NbTreeNode[],
  fetcher: NoteFetcher = fetchNote,
  onProgress?: FolderPdfProgress,
): Promise<Array<{ node: NbTreeNode; detail: NbNoteDetail | null }>> {
  const results: Array<{ node: NbTreeNode; detail: NbNoteDetail | null }> = []
  let cursor = 0
  let done = 0

  async function worker() {
    while (cursor < notes.length) {
      const index = cursor++
      const node = notes[index]
      try {
        const detail = await fetcher(node.noteId as number)
        results[index] = { node, detail }
      } catch {
        // 不吞异常：记录失败项，让整体导出继续（失败清单由上层提示）
        results[index] = { node, detail: null }
      }
      done += 1
      onProgress?.(done, notes.length)
    }
  }

  const workerCount = Math.min(FETCH_CONCURRENCY, notes.length)
  await Promise.all(Array.from({ length: workerCount }, () => worker()))
  return results
}

/** 把正文 HTML 中的相对图片地址绝对化：打印窗口（srcdoc）需要完整 URL 才能加载图片 */
export function absolutizeImageSrc(html: string, base: string): string {
  if (!html || !base) return html
  return html.replace(/(<img[^>]*?src=["'])(\/[^"']*?)(["'])/g, (_match, prefix: string, src: string, suffix: string) => {
    // 完整 URL 与 data URI 保持原样
    if (/^https?:\/\//i.test(src) || src.startsWith('data:')) return _match
    return `${prefix}${base}${src}${suffix}`
  })
}

/** HTML 转义，防止笔记标题/正文内容注入破坏打印文档结构 */
export function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/**
 * 单篇正文转渲染后的 HTML：标题以 .md 结尾走 marked 渲染，其余（富文本）保持原文。
 * 与全屏预览/编辑预览的判定与渲染管线一致，保证导出与界面所见一致。
 */
export function buildNoteBodyHtml(detail: NbNoteDetail): string {
  const isMd = detail.title?.toLowerCase().endsWith('.md')
  if (isMd) return marked.parse(detail.content ?? '') as string
  return detail.content ?? ''
}

/**
 * 构建可打印的聚合 HTML：每篇一个分页 section（标题 + 正文），注入
 * 与全屏预览同源的 note-content.scss 样式与 @page 打印规则。
 * css 可注入，便于单测断言打印文档结构。
 */
export function buildFolderPdfHtml(
  items: Array<{ node: NbTreeNode; detail: NbNoteDetail }>,
  options: { folderName?: string; css?: string } = {},
): string {
  const base = import.meta.env.VITE_API_BASE || ''
  const css = options.css ?? noteContentCss

  const sections = items
    .map(({ detail }) => {
      const body = absolutizeImageSrc(buildNoteBodyHtml(detail), base)
      const safeTitle = escapeHtml(detail.title || '无标题')
      return `<section class="note-export__note">
  <h1 class="note-export__note-title">${safeTitle}</h1>
  <div class="note-content-body">${body}</div>
</section>`
    })
    .join('\n')

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<title>${escapeHtml(options.folderName ?? '')} 导出</title>
<style>
@page { margin: 18mm 16mm; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif; font-size: 14px; line-height: 1.75; color: #1f2937; background: #fff; }
.note-export__note { page-break-before: always; }
.note-export__note:first-child { page-break-before: auto; }
.note-export__note-title { margin: 0 0 18px; font-size: 22px; font-weight: 700; color: #991b1b; padding-bottom: 10px; border-bottom: 3px solid #fca5a5; }
${css}
</style>
</head>
<body>
${sections}
</body>
</html>`
}

/** 复用隐藏 iframe 打开打印窗口：srcdoc 内联文档，避免 window.open 被浏览器弹窗拦截 */
function openPrintWindow(html: string): void {
  let iframe = document.getElementById('note-export-print-frame') as HTMLIFrameElement | null
  if (!iframe) {
    iframe = document.createElement('iframe')
    iframe.id = 'note-export-print-frame'
    // 隐藏定位，不影响页面布局
    iframe.style.position = 'fixed'
    iframe.style.right = '0'
    iframe.style.bottom = '0'
    iframe.style.width = '0'
    iframe.style.height = '0'
    iframe.style.border = '0'
    iframe.setAttribute('aria-hidden', 'true')
    document.body.appendChild(iframe)
  }
  iframe.srcdoc = html
  // srcdoc 内容（含图片）加载完成后触发打印，确保 PDF 中图片就绪
  iframe.onload = () => {
    iframe?.contentWindow?.focus()
    iframe?.contentWindow?.print()
  }
}

/** 导出编排：收集直属 → 排序 → 并发拉正文 → 构建聚合 HTML → 打开打印窗口 */
export async function exportFolderToPdf(
  folderNode: NbTreeNode,
  options: { fetcher?: NoteFetcher; onProgress?: FolderPdfProgress } = {},
): Promise<FolderPdfExportResult> {
  const directNotes = sortNotesByName(collectDirectNotes(folderNode))
  if (!directNotes.length) {
    return { exported: 0, failed: [] }
  }

  const fetched = await fetchNotesConcurrently(directNotes, options.fetcher, options.onProgress)
  const succeeded = fetched.filter(
    (r): r is { node: NbTreeNode; detail: NbNoteDetail } => r.detail != null,
  )
  const failed = fetched
    .filter((r) => r.detail == null)
    .map((r) => ({ noteId: r.node.noteId as number, title: r.node.name }))

  const html = buildFolderPdfHtml(succeeded, { folderName: folderNode.name })
  // 仅在真实浏览器环境打开打印窗口（单测不会走到这里，导出结果已可断言）
  if (typeof document !== 'undefined') {
    openPrintWindow(html)
  }
  return { exported: succeeded.length, failed }
}
