/** 代码块缩进空格数（按语言） */
const INDENT_SIZE: Record<string, number> = {
  java: 4,
  csharp: 4,
  c: 4,
  cpp: 4,
  go: 4,
  kotlin: 4,
  swift: 4,
  javascript: 2,
  js: 2,
  typescript: 2,
  ts: 2,
  json: 2,
  yaml: 2,
  yml: 2,
  html: 2,
  xml: 2,
  css: 2,
  scss: 2,
  python: 4,
  py: 4,
}

const CPP_LIKE_LANGS = new Set([
  'java',
  'javascript',
  'js',
  'typescript',
  'ts',
  'csharp',
  'c',
  'cpp',
  'go',
  'kotlin',
  'swift',
  'json',
])

const INVISIBLE_CHARS_RE = /[\u200B-\u200D\uFEFF]/g

/**
 * 格式化单个代码块纯文本。
 */
export function formatCodeBlockContent(text: string, language: string): string {
  const normalized = normalizeCodeLines(text)
  if (!normalized) return normalized

  const lang = normalizeLanguage(language)
  const indentSize = INDENT_SIZE[lang] ?? 4

  if (lang === 'python' || lang === 'py') {
    return formatPythonBlock(normalized, indentSize)
  }

  if (CPP_LIKE_LANGS.has(lang) || shouldUseBraceIndent(normalized)) {
    return formatBraceIndent(normalized, indentSize)
  }

  return normalized
}

/**
 * 从 wangEditor 代码块节点检测语言。
 */
export function detectCodeLanguage(codeEl: Element): string {
  const className = codeEl.getAttribute('class') ?? ''
  const fromClass = className.match(/language-([\w-]+)/i)?.[1] ?? ''
  if (fromClass && !isPlainLanguage(fromClass)) {
    return fromClass
  }

  const text = codeEl.textContent ?? ''
  return detectLanguageFromContent(text)
}

export function formatCodeBlocksInHtml(root: Element) {
  root.querySelectorAll('pre').forEach((pre) => {
    const codes = pre.querySelectorAll(':scope > code')
    if (codes.length > 0) {
      codes.forEach((code) => applyCodeFormat(code))
      return
    }
    const text = pre.textContent ?? ''
    if (!text.trim()) return
    const code = document.createElement('code')
    code.textContent = formatCodeBlockContent(text, detectLanguageFromContent(text))
    pre.textContent = ''
    pre.appendChild(code)
  })
}

function applyCodeFormat(code: Element) {
  const language = detectCodeLanguage(code)
  const raw = code.textContent ?? ''
  const formatted = formatCodeBlockContent(raw, language)
  if (formatted !== raw) {
    code.textContent = formatted
  }
}

function normalizeLanguage(language: string): string {
  return language.trim().toLowerCase().replace(/^plaintext$/, '').replace(/^text$/, '')
}

function isPlainLanguage(language: string): boolean {
  const lang = normalizeLanguage(language)
  return !lang || lang === 'plain' || lang === 'plaintext' || lang === 'text'
}

function detectLanguageFromContent(text: string): string {
  const sample = text.slice(0, 800)
  if (/\bpublic\s+class\b|\bpublic\s+static\b|\bvolatile\b|\bsynchronized\s*\(/.test(sample)) {
    return 'java'
  }
  if (/\bfunction\b|\bconst\b|\blet\b|=>/.test(sample)) {
    return 'javascript'
  }
  if (/\binterface\b|\btype\b.*=/.test(sample) && /:\s*\w+/.test(sample)) {
    return 'typescript'
  }
  if (/^\s*def\s+\w+/m.test(sample) || /^\s*class\s+\w+.*:/m.test(sample)) {
    return 'python'
  }
  if (/^\s*#include\b/m.test(sample)) {
    return 'cpp'
  }
  if (/^\s*using\s+System/m.test(sample)) {
    return 'csharp'
  }
  return ''
}

function normalizeCodeLines(text: string): string {
  const lines = text
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(INVISIBLE_CHARS_RE, '')
    .split('\n')
    .map((line) => line.replace(/\t/g, '  ').replace(/[ \t]+$/, ''))

  while (lines.length > 0 && !lines[0].trim()) {
    lines.shift()
  }
  while (lines.length > 0 && !lines[lines.length - 1].trim()) {
    lines.pop()
  }

  return lines.join('\n')
}

function shouldUseBraceIndent(text: string): boolean {
  return /[{}]/.test(text)
}

function formatBraceIndent(code: string, indentSize: number): string {
  const lines = code.split('\n')
  let level = 0
  const out: string[] = []

  for (const rawLine of lines) {
    const trimmed = rawLine.trim()
    if (!trimmed) {
      out.push('')
      continue
    }

    if (/^[}\])]/.test(trimmed)) {
      level = Math.max(0, level - 1)
    }

    out.push(`${' '.repeat(level * indentSize)}${trimmed}`)

    for (const ch of trimmed) {
      if (ch === '{') {
        level += 1
      }
    }
  }

  return out.join('\n')
}

function formatPythonBlock(code: string, indentSize: number): string {
  const lines = code.split('\n')
  const out: string[] = []
  let level = 0

  for (const rawLine of lines) {
    const trimmed = rawLine.trim()
    if (!trimmed) {
      out.push('')
      continue
    }

    if (/^(elif\b|else\b|except\b|finally\b)/.test(trimmed)) {
      level = Math.max(0, level - 1)
    }

    out.push(`${' '.repeat(level * indentSize)}${trimmed}`)

    if (/:\s*$/.test(trimmed) && !/^#/.test(trimmed)) {
      level += 1
    }
  }

  return out.join('\n')
}
