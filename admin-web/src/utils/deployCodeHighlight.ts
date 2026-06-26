export type DeployCodeTokenKind =
  | 'plain'
  | 'comment'
  | 'command'
  | 'flag'
  | 'url'
  | 'pipe'
  | 'json-string'
  | 'json-number'

export interface DeployCodeToken {
  kind: DeployCodeTokenKind
  text: string
}

const SHELL_COMMANDS =
  /\b(curl(?:\.\w+)?|redis-cli|mysql|bash|sudo|docker|jq|nano|cd|scp|ssh|systemctl|grep|nano)\b/g

const SHELL_FLAGS = /(?<=\s|^)(-{1,2}[\w-]+)/g

const URL_PATTERN = /https?:\/\/[^\s|]+/g

const PIPE_PATTERN = /(\||&&|;)/g

function pushPlain(tokens: DeployCodeToken[], text: string) {
  if (!text) return
  const last = tokens[tokens.length - 1]
  if (last?.kind === 'plain') {
    last.text += text
    return
  }
  tokens.push({ kind: 'plain', text })
}

function mergeMatches(
  line: string,
  matches: Array<{ index: number; length: number; kind: DeployCodeTokenKind }>,
): DeployCodeToken[] {
  const sorted = [...matches].sort((a, b) => a.index - b.index)
  const tokens: DeployCodeToken[] = []
  let cursor = 0

  for (const match of sorted) {
    if (match.index < cursor) continue
    pushPlain(tokens, line.slice(cursor, match.index))
    tokens.push({ kind: match.kind, text: line.slice(match.index, match.index + match.length) })
    cursor = match.index + match.length
  }

  pushPlain(tokens, line.slice(cursor))
  return tokens
}

function collectRegexMatches(
  line: string,
  regex: RegExp,
  kind: DeployCodeTokenKind,
): Array<{ index: number; length: number; kind: DeployCodeTokenKind }> {
  const flags = regex.flags.includes('g') ? regex.flags : `${regex.flags}g`
  const re = new RegExp(regex.source, flags)
  const matches: Array<{ index: number; length: number; kind: DeployCodeTokenKind }> = []
  let m: RegExpExecArray | null
  while ((m = re.exec(line))) {
    matches.push({ index: m.index, length: m[0].length, kind })
  }
  return matches
}

function isJsonLine(line: string): boolean {
  const trimmed = line.trim()
  if (!trimmed) return false
  if (/^[{}\[\],]$/.test(trimmed)) return true
  if (trimmed.startsWith('"') && trimmed.includes(':')) return true
  if (/^"[^"]+"\s*:/.test(trimmed)) return true
  if (/:\s*("|\d|\{)/.test(trimmed) && trimmed.includes('"')) return true
  return false
}

function isShellLine(line: string): boolean {
  const trimmed = line.trim()
  if (!trimmed) return false
  return /^(curl|redis-cli|mysql|bash|sudo|docker|cd|scp|ssh|systemctl|grep|nano|jq)\b/.test(trimmed)
}

function highlightShellLine(line: string): DeployCodeToken[] {
  const matches = [
    ...collectRegexMatches(line, SHELL_COMMANDS, 'command'),
    ...collectRegexMatches(line, URL_PATTERN, 'url'),
    ...collectRegexMatches(line, SHELL_FLAGS, 'flag'),
    ...collectRegexMatches(line, PIPE_PATTERN, 'pipe'),
  ]
  return mergeMatches(line, matches)
}

function highlightJsonLine(line: string): DeployCodeToken[] {
  const tokens: DeployCodeToken[] = []
  const regex = /"(?:[^"\\]|\\.)*"|\d+|[{}[\],:]|[^\s"{}[\],:\d]+|\s+/g
  let match: RegExpExecArray | null
  while ((match = regex.exec(line))) {
    const text = match[0]
    if (text.startsWith('"')) {
      tokens.push({ kind: 'json-string', text })
    } else if (/^\d+$/.test(text)) {
      tokens.push({ kind: 'json-number', text })
    } else {
      pushPlain(tokens, text)
    }
  }
  return tokens
}

function highlightLine(line: string): DeployCodeToken[] {
  const trimmed = line.trimStart()
  if (!line) return [{ kind: 'plain', text: '' }]
  if (trimmed.startsWith('#')) return [{ kind: 'comment', text: line }]
  if (isJsonLine(line)) return highlightJsonLine(line)
  if (isShellLine(line)) return highlightShellLine(line)
  return [{ kind: 'plain', text: line }]
}

export function highlightDeployCode(text: string): DeployCodeToken[][] {
  return text.split('\n').map((line) => highlightLine(line))
}
