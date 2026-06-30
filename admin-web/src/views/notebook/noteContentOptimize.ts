import {
  NOTE_DEFAULT_FONT_SIZE,
  NOTE_REDUNDANT_FONT_SIZES,
} from './noteFormatStandard'
import { formatCodeBlocksInHtml } from './noteCodeFormat'

const LEADING_SPACE_RE = /^[\s\u00a0\u3000]+/
const TRAILING_SPACE_RE = /[\s\u00a0\u3000]+$/
const INVISIBLE_CHARS_RE = /[\u200B-\u200D\uFEFF]/g
const INLINE_SPACE_RE = /[ \t\u00a0\u3000]{2,}/g
const REMOVABLE_EMPTY_TAGS = new Set(['P', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'LI', 'BLOCKQUOTE'])
const PRESERVE_TAGS = new Set([
  'IMG',
  'VIDEO',
  'AUDIO',
  'IFRAME',
  'TABLE',
  'HR',
  'SVG',
  'CANVAS',
  'UL',
  'OL',
  'PRE',
  'CODE',
])
const TEXT_BLOCK_SELECTOR = 'p, h1, h2, h3, h4, h5, h6, li, blockquote'
const BLOCK_CHILD_SELECTOR = 'p, h1, h2, h3, h4, h5, h6, ul, ol, li, pre, blockquote, table, hr'
const INLINE_EMPTY_TAGS = new Set(['SPAN', 'B', 'STRONG', 'I', 'EM', 'U', 'S', 'STRIKE', 'FONT'])
const SKIP_TEXT_NORMALIZE_SELECTOR = 'pre, code, pre *, code *'

/**
 * 按笔记格式化标准优化富文本 HTML。
 */
export function optimizeNoteHtml(html: string): string {
  const raw = html?.trim() ?? ''
  if (!raw) return ''

  const beforeText = extractPlainText(raw)
  if (!beforeText) return raw

  const root = document.createElement('div')
  root.innerHTML = raw

  normalizeRootStructure(root)
  stripEditorArtifacts(root)
  convertTextDivsToParagraphs(root)
  flattenNestedParagraphs(root)

  root.querySelectorAll(TEXT_BLOCK_SELECTOR).forEach((block) => {
    trimLeadingWhitespace(block)
    trimTrailingWhitespace(block)
    collapseExtraBreaks(block)
  })

  cleanInlineNodes(root)
  normalizeInlineStyles(root)
  normalizeLinks(root)
  collapseInlineSpaces(root)
  formatCodeBlocksInHtml(root)

  removeAllEmptyBlocks(root)
  trimEdgeEmptyBlocks(root)

  const result = root.innerHTML.trim()
  const afterText = extractPlainText(result)

  if (beforeText && !afterText) {
    return raw
  }
  if (!result) {
    return raw
  }
  if (beforeText && afterText.length < beforeText.length) {
    const beforeChars = new Set(beforeText.replace(/\s+/g, ''))
    const afterChars = new Set(afterText.replace(/\s+/g, ''))
    for (const ch of beforeChars) {
      if (!afterChars.has(ch)) {
        return raw
      }
    }
  }
  return result
}

export function hasNoteVisibleText(html: string): boolean {
  return extractPlainText(html).length > 0
}

export function extractNotePlainText(html: string): string {
  return extractPlainText(html)
}

export function isSameNoteHtml(a: string, b: string): boolean {
  if (a === b) return true
  const left = document.createElement('div')
  const right = document.createElement('div')
  left.innerHTML = a
  right.innerHTML = b
  return left.innerHTML === right.innerHTML
}

function extractPlainText(html: string): string {
  const el = document.createElement('div')
  el.innerHTML = html
  return normalizeVisibleText(el.textContent ?? '')
}

function normalizeVisibleText(text: string): string {
  return text.replace(INVISIBLE_CHARS_RE, '').replace(/\u00a0/g, ' ').replace(/\s+/g, ' ').trim()
}

function normalizeRootStructure(root: HTMLElement) {
  Array.from(root.childNodes).forEach((node) => {
    if (node.nodeType === Node.TEXT_NODE && normalizeVisibleText(node.textContent ?? '')) {
      const p = document.createElement('p')
      p.textContent = node.textContent?.replace(INVISIBLE_CHARS_RE, '') ?? ''
      root.replaceChild(p, node)
    }
  })
}

function stripEditorArtifacts(root: Element) {
  root.querySelectorAll('*').forEach((el) => {
    if (el.id?.startsWith('w-e-')) {
      el.removeAttribute('id')
    }
    Array.from(el.attributes).forEach((attr) => {
      if (attr.name.startsWith('data-')) {
        el.removeAttribute(attr.name)
      }
    })
  })
}

function convertTextDivsToParagraphs(root: Element) {
  Array.from(root.querySelectorAll('div')).reverse().forEach((div) => {
    if (div.closest('table, pre, blockquote')) return
    if (div.querySelector('div, table, ul, ol, pre, blockquote')) return
    if (div.querySelector(BLOCK_CHILD_SELECTOR)) return
    const p = document.createElement('p')
    p.innerHTML = div.innerHTML
    div.replaceWith(p)
  })
}

function flattenNestedParagraphs(root: Element) {
  let changed = true
  while (changed) {
    changed = false
    Array.from(root.querySelectorAll('p')).forEach((paragraph) => {
      const nested = Array.from(paragraph.children).filter((child) => child.tagName === 'P')
      if (!nested.length) return
      const parent = paragraph.parentElement
      if (!parent) return
      nested.forEach((child) => {
        parent.insertBefore(child, paragraph.nextSibling)
      })
      changed = true
      if (isEmptyBlock(paragraph)) {
        paragraph.remove()
      }
    })
  }
}

function isEmptyBlock(el: Element): boolean {
  if (PRESERVE_TAGS.has(el.tagName)) {
    return false
  }
  const clone = el.cloneNode(true) as Element
  clone.querySelectorAll('br').forEach((br) => br.remove())
  if (normalizeVisibleText(clone.textContent ?? '')) {
    return false
  }
  return clone.querySelectorAll('img, video, audio, iframe, table, svg, canvas').length === 0
}

function removeAllEmptyBlocks(root: Element) {
  Array.from(root.children).forEach((child) => {
    if (child.children.length > 0) {
      removeAllEmptyBlocks(child)
    }
  })
  Array.from(root.children).forEach((child) => {
    if (REMOVABLE_EMPTY_TAGS.has(child.tagName) && isEmptyBlock(child)) {
      child.remove()
    }
  })
}

function trimLeadingWhitespace(el: Element) {
  while (el.firstChild) {
    const node = el.firstChild
    if (node.nodeType === Node.TEXT_NODE) {
      const text = node.textContent ?? ''
      const trimmed = text.replace(INVISIBLE_CHARS_RE, '').replace(LEADING_SPACE_RE, '')
      if (!normalizeVisibleText(trimmed)) {
        node.remove()
        continue
      }
      node.textContent = trimmed
      break
    }
    if (node.nodeName === 'BR') {
      node.remove()
      continue
    }
    if (node.nodeType === Node.ELEMENT_NODE) {
      const child = node as Element
      if (PRESERVE_TAGS.has(child.tagName)) {
        break
      }
      trimLeadingWhitespace(child)
      if (isEffectivelyEmpty(child)) {
        node.remove()
        continue
      }
      break
    }
    break
  }
}

function trimTrailingWhitespace(el: Element) {
  while (el.lastChild) {
    const node = el.lastChild
    if (node.nodeType === Node.TEXT_NODE) {
      const text = node.textContent ?? ''
      const trimmed = text.replace(INVISIBLE_CHARS_RE, '').replace(TRAILING_SPACE_RE, '')
      if (!normalizeVisibleText(trimmed)) {
        node.remove()
        continue
      }
      node.textContent = trimmed
      break
    }
    if (node.nodeName === 'BR') {
      node.remove()
      continue
    }
    if (node.nodeType === Node.ELEMENT_NODE) {
      const child = node as Element
      if (PRESERVE_TAGS.has(child.tagName)) {
        break
      }
      trimTrailingWhitespace(child)
      if (isEffectivelyEmpty(child)) {
        node.remove()
        continue
      }
      break
    }
    break
  }
}

function isEffectivelyEmpty(el: Element): boolean {
  if (PRESERVE_TAGS.has(el.tagName)) {
    return false
  }
  const text = el.textContent ?? ''
  return !normalizeVisibleText(text) && el.querySelectorAll('img, video, audio, iframe, table, svg, canvas').length === 0
}

function collapseExtraBreaks(el: Element) {
  if (PRESERVE_TAGS.has(el.tagName)) return
  const html = el.innerHTML
  const normalized = html
    .replace(/(<br\s*\/?>\s*){2,}/gi, '<br>')
    .replace(/(\s|&nbsp;|\u00a0)+(<br\s*\/?>)/gi, '$2')
    .replace(/(<br\s*\/?>)(\s|&nbsp;|\u00a0)+/gi, '$1')
  if (normalized !== html) {
    el.innerHTML = normalized
  }
}

function cleanInlineNodes(root: Element) {
  Array.from(root.querySelectorAll('span, b, strong, i, em, u, s, strike, font')).reverse().forEach((node) => {
    if (node.closest('pre, code')) return
    if (!INLINE_EMPTY_TAGS.has(node.tagName)) return

    if (node.tagName === 'FONT') {
      unwrapElement(node)
      return
    }

    if (isEffectivelyEmpty(node)) {
      node.remove()
      return
    }

    if (node.tagName === 'SPAN' && shouldUnwrapStyleOnlySpan(node)) {
      unwrapElement(node)
    }
  })
}

function shouldUnwrapStyleOnlySpan(span: Element): boolean {
  if (span.attributes.length === 0) {
    return true
  }
  if (span.attributes.length > 1) {
    return false
  }
  if (!span.hasAttribute('style')) {
    return false
  }
  const el = span as HTMLElement
  const hasMeaningfulStyle =
    !!el.style.color ||
    !!el.style.backgroundColor ||
    !!el.style.textDecoration ||
    !!el.style.fontWeight ||
    !!el.style.fontStyle
  return !hasMeaningfulStyle
}

function unwrapElement(el: Element) {
  const parent = el.parentElement
  if (!parent) return
  while (el.firstChild) {
    parent.insertBefore(el.firstChild, el)
  }
  el.remove()
}

function normalizeInlineStyles(root: Element) {
  root.querySelectorAll<HTMLElement>('[style]').forEach((el) => {
    if (el.closest('pre, code')) return

    el.style.removeProperty('font-family')
    el.style.removeProperty('line-height')
    el.style.removeProperty('margin')
    el.style.removeProperty('padding')

    const fontSize = el.style.fontSize
    if (fontSize && (NOTE_REDUNDANT_FONT_SIZES.has(fontSize) || fontSize === NOTE_DEFAULT_FONT_SIZE)) {
      el.style.removeProperty('font-size')
    }

    if (!el.getAttribute('style')?.trim()) {
      el.removeAttribute('style')
    }
  })
}

function normalizeLinks(root: Element) {
  root.querySelectorAll('a').forEach((anchor) => {
    const href = anchor.getAttribute('href')?.trim() ?? ''
    if (!href) {
      unwrapElement(anchor)
      return
    }
    trimLeadingWhitespace(anchor)
    trimTrailingWhitespace(anchor)
  })
}

function collapseInlineSpaces(root: Element) {
  const skipNodes = new Set<Node>()
  root.querySelectorAll(SKIP_TEXT_NORMALIZE_SELECTOR).forEach((el) => {
    const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT)
    let current = walker.nextNode()
    while (current) {
      skipNodes.add(current)
      current = walker.nextNode()
    }
  })

  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT)
  let node = walker.nextNode()
  while (node) {
    if (!skipNodes.has(node) && node.textContent) {
      const normalized = node.textContent
        .replace(INVISIBLE_CHARS_RE, '')
        .replace(/\u00a0/g, ' ')
        .replace(INLINE_SPACE_RE, ' ')
      if (normalized !== node.textContent) {
        node.textContent = normalized
      }
    }
    node = walker.nextNode()
  }
}

function trimEdgeEmptyBlocks(root: Element) {
  while (root.firstElementChild && isEmptyBlock(root.firstElementChild)) {
    root.firstElementChild.remove()
  }
  while (root.lastElementChild && isEmptyBlock(root.lastElementChild)) {
    root.lastElementChild.remove()
  }
}
