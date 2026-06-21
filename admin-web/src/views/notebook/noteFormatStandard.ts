/**
 * 笔记正文一键优化所遵循的格式化标准（规则 ID 与 i18n key 对应）。
 */
export const NOTE_FORMAT_RULE_IDS = [
  'invisibleChars',
  'paragraphWhitespace',
  'emptyLines',
  'inlineBreaks',
  'pasteStyles',
  'emptyInline',
  'linkText',
  'structure',
  'codeBlock',
] as const

export type NoteFormatRuleId = (typeof NOTE_FORMAT_RULE_IDS)[number]

/** 编辑器默认正文字号 */
export const NOTE_DEFAULT_FONT_SIZE = '14px'

/** 粘贴时常见的冗余字号，优化时会移除对应 inline style */
export const NOTE_REDUNDANT_FONT_SIZES = new Set(['12px', '13px', '14px', '15px', '16px'])
