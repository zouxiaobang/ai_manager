/**
 * Markdown → 可读纯文本工具
 *
 * 用于 RAG 检索结果片段的预览：把文档里残留的 markdown 格式符号（表格管道符/分隔行、
 * 代码围栏、标题符、加粗/斜体、链接/图片语法等）去掉，只保留正文与换行结构，
 * 便于快速扫读重点内容。纯函数、无依赖，便于单元测试。
 */

/** 截断省略号 */
const ELLIPSIS = '…'

/**
 * 常见的 markdown 结构语法片段：命中任一即认为原文含 markdown，
 * 用于决定是否提供「展开 Markdown」入口。
 */
const MARKDOWN_SYNTAX_PATTERN = new RegExp(
  [
    /(?:^|\n)\s{0,3}#{1,6}[ \t]/,          // ATX 标题
    /\*\*[^*\n]+\*\*/,                       // 加粗
    /(?:^|\n)\s{0,3}>[ \t]/,                 // 引用
    /(?:^|\n)\s{0,3}(?:[-+*]|\d+\.)[ \t]/,   // 列表
    /`{3}/,                                  // 代码围栏
    /!\[[^\]]*\]\([^)]*\)/,                  // 图片
    /\[[^\]]*\]\([^)]*\)/,                   // 链接
    /^\s*\|.*\|[ \t]*$/,                     // 表格行（起止均带管道符）
    /(?:^|\n)\s*[-*_]{3,}\s*$/,              // 分隔线 / 表格分隔行
  ]
    .map((re) => re.source)
    .join('|'),
  'm',
)

/** 判断原文是否含 markdown 结构语法（表格、标题、代码、链接等） */
export function hasMarkdownSyntax(md: string): boolean {
  if (!md) return false
  return MARKDOWN_SYNTAX_PATTERN.test(md)
}

/**
 * 将 Markdown 文本转换为可读纯文本。
 *
 * 处理顺序：YAML frontmatter → 逐行块级语法（代码围栏/表格/标题/引用/列表）→ 行内语法
 * （HTML 标签/图片/链接/行内代码/强调/转义）→ 空白归一化。代码围栏内容原样保留。
 */
export function markdownToText(md: string): string {
  if (!md) return ''

  // 1. 去掉 YAML frontmatter（笔记标签等元信息，对阅读正文无意义）
  const body = md.replace(/^\uFEFF?---\r?\n[\s\S]*?\r?\n---\r?\n?/, '')

  const lines = body.split(/\r?\n/)
  const out: string[] = []
  let inFence = false

  for (const raw of lines) {
    const line = raw.replace(/\r$/, '')
    // 代码围栏：开/关切换，围栏内内容原样保留（不做内联处理）
    if (/^\s*(?:`{3,}|~{3,})/.test(line)) {
      inFence = !inFence
      continue
    }
    if (inFence) {
      out.push(line)
      continue
    }
    // 返回 null 表示该行是纯语法（表格分隔行/分隔线等），直接丢弃不占空行
    const stripped = stripBlockSyntax(line)
    if (stripped !== null) out.push(stripped)
  }
  return normalizeText(out.join('\n'))
}

/**
 * 去掉一行中的块级语法（表格/分隔线/引用/标题/列表/缩进代码）。
 * 返回 null 表示整行是纯语法结构（表格分隔行、分隔线、孤立的列表符号），应丢弃且不产生空行。
 */
function stripBlockSyntax(line: string): string | null {
  let text = line

  // 表格分隔行（| --- | --- | 或 |:--|--:|）：整行丢弃
  if (text.includes('|') && /^\s*\|?[\s|:\-]+\|?\s*$/.test(text)) return null

  // 表格数据行：拆掉管道符，单元格用空格分隔，保留表头（数据仍是正文）
  if (text.includes('|')) {
    const cells = text
      .replace(/^\s*\|/, '')
      .replace(/\|\s*$/, '')
      .split('|')
      .map((c) => c.trim())
      .filter((c) => c !== '')
    if (cells.length > 1) {
      return stripInlineSyntax(cells.join('  ')).trim()
    }
  }

  // 分隔线 / 水平线（---、- - -、*** 等）
  if (/^\s*[-*_]\s*[-*_]\s*[-*_]/.test(text)) return null
  // Setext 标题下划线（===）
  if (/^\s*={3,}\s*$/.test(text)) return null
  // 单独成行的列表符号（分块切分残留）
  if (/^\s*[-+*]\s*$/.test(text)) return null

  // 引用：去掉「> 」前缀（支持嵌套引用）
  text = text.replace(/^(\s*>+\s?)+/, '')

  // ATX 标题：# 标题  →  标题（同时去掉结尾的闭合格）
  text = text.replace(/^\s{0,3}(#{1,6})\s+/, '').replace(/\s+#+\s*$/, '')

  // 任务列表：- [x] 完成  →  ✓ 完成；- [ ] 待办  →  ☐ 待办
  text = text.replace(/^\s*[-+*]\s+\[([ xX])\]\s+/, (_m, mark) => (mark.toLowerCase() === 'x' ? '✓ ' : '☐ '))

  // 无序列表：去掉「- / * / + 」前缀
  text = text.replace(/^\s*[-+*]\s+/, '')
  // 有序列表：保留编号，只规整空格（1. 内容）
  text = text.replace(/^\s*(\d+\.)\s+/, '$1 ')
  // 缩进代码块（4 空格缩进）
  text = text.replace(/^ {4,}/, '')

  return stripInlineSyntax(text).trim()
}

/** 去掉行内语法：HTML 标签 / 图片 / 链接 / 行内代码 / 强调 / 反斜杠转义 */
function stripInlineSyntax(text: string): string {
  return text
    .replace(/<!--[\s\S]*?-->/g, '') // HTML 注释
    .replace(/<\/?[a-zA-Z][^>]*>/g, '') // HTML 标签（解析文档残留的 <div>、<br> 等）
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1') // 图片 → 保留 alt 文字
    .replace(/\[([^\]\[]*)\]\([^)]*\)/g, '$1') // 链接 → 保留链接文字
    .replace(/\[\[([^\]|]+)(?:\|([^\]]*))?\]\]/g, (_m, target, alias) => alias || target) // 双链笔记
    .replace(/`([^`]+)`/g, '$1') // 行内代码 → 保留内容
    .replace(/~~([^~]+)~~/g, '$1') // 删除线
    .replace(/\*\*([^*]+)\*\*/g, '$1') // 加粗
    .replace(/__([^_]+)__/g, '$1') // 加粗（下划线写法）
    .replace(/\*([^*\n]+)\*/g, '$1') // 斜体
    .replace(/_([^_\n]*\s[^_\n]*)_/g, '$1') // 斜体（下划线写法，要求内容含空格以免误伤 snake_case）
    .replace(/\\([\\`*_#+\-!~|>])/g, '$1') // 去掉反斜杠转义符
}

/** 空白归一化：去首尾空行、合并连续空行（段落之间最多留一空行） */
function normalizeText(text: string): string {
  return text
    .split('\n')
    .map((l) => l.trimEnd())
    .join('\n')
    .replace(/^\n+/, '')
    .replace(/\n+$/, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

/**
 * 按字符预算截断文本，优先保留完整行，末尾追加省略号。
 * 用于把纯文本片段压缩到几行以内。
 */
export function truncateText(text: string, maxChars: number, ellipsis: string = ELLIPSIS): string {
  if (!text || text.length <= maxChars) return text
  const lines = text.split('\n')
  const kept: string[] = []
  let used = 0
  for (const line of lines) {
    if (used + line.length > maxChars) break
    kept.push(line)
    used += line.length + 1
  }
  if (kept.length === 0) {
    // 首行本身就超预算：直接按字符截断
    return text.slice(0, maxChars) + ellipsis
  }
  return kept.join('\n') + ellipsis
}

/**
 * Markdown → 可直接展示的纯文本片段（先转纯文本再截断）。
 *
 * @param md markdown 原文
 * @param maxChars 片段字符预算，默认 240（约 3 行）
 */
export function markdownToTextSnippet(md: string, maxChars: number = 240): string {
  return truncateText(markdownToText(md), maxChars)
}
