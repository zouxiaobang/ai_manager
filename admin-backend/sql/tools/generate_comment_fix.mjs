import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sqlRoot = path.resolve(__dirname, '..')
const deployAllPath = path.join(sqlRoot, 'deploy-all.sql')
const deployAll = fs.readFileSync(deployAllPath, 'utf8')

const extraSqlFiles = fs
  .readdirSync(sqlRoot)
  .filter((name) => name.endsWith('.sql'))
  .filter((name) => name !== 'deploy-all.sql' && name !== 'all_table_comment_fix.sql' && name !== 'ec_express_comment_fix.sql')
  .sort()
  .map((name) => path.join(sqlRoot, name))

/** @type {Map<string, { tableComment: string, columns: Map<string, { def: string, comment: string }> }>} */
const tables = new Map()

function unescapeSqlString(value) {
  return value.replace(/''/g, "'")
}

function parseCreateTable(block, tableName) {
  const tableCommentMatch = block.match(/COMMENT\s*=\s*'((?:[^']|'')*)'/i)
  const tableComment = tableCommentMatch ? unescapeSqlString(tableCommentMatch[1]) : ''

  let entry = tables.get(tableName)
  if (!entry) {
    entry = { tableComment: '', columns: new Map() }
    tables.set(tableName, entry)
  }
  if (tableComment) {
    entry.tableComment = tableComment
  }

  const bodyMatch = block.match(/\(\s*([\s\S]*)\)\s*ENGINE/i)
  if (!bodyMatch) {
    return
  }
  const body = bodyMatch[1]
  const lines = body.split('\n')
  for (const rawLine of lines) {
    const line = rawLine.trim()
    if (!line || line.startsWith('PRIMARY KEY') || line.startsWith('UNIQUE KEY') || line.startsWith('KEY ')) {
      continue
    }
    const colMatch = line.match(/^`?([a-zA-Z0-9_]+)`?\s+(.+?)(?:,\s*)?$/i)
    if (!colMatch) {
      continue
    }
    const colName = colMatch[1]
    let colDef = colMatch[2].replace(/,\s*$/, '').trim()
    const commentMatch = colDef.match(/\s+COMMENT\s+'((?:[^']|'')*)'\s*$/i)
    if (!commentMatch) {
      continue
    }
    const comment = unescapeSqlString(commentMatch[1])
    colDef = colDef.replace(/\s+COMMENT\s+'(?:[^']|'')*'\s*$/i, '').trim()
    entry.columns.set(colName, { def: colDef, comment })
  }
}

function parseAlterAddOrModify(sql) {
  const tableMatch = sql.match(/ALTER\s+TABLE\s+`?([a-zA-Z0-9_]+)`?/i)
  if (!tableMatch) {
    return
  }
  const tableName = tableMatch[1]
  let entry = tables.get(tableName)
  if (!entry) {
    entry = { tableComment: '', columns: new Map() }
    tables.set(tableName, entry)
  }

  const tableCommentMatch = sql.match(/COMMENT\s*=\s*'((?:[^']|'')*)'/i)
  if (tableCommentMatch && !/MODIFY\s+COLUMN/i.test(sql)) {
    entry.tableComment = unescapeSqlString(tableCommentMatch[1])
  }

  const modifyRegex =
    /MODIFY\s+COLUMN\s+`?([a-zA-Z0-9_]+)`?\s+([\s\S]*?)(?=,\s*MODIFY\s+COLUMN|;\s*$)/gi
  let match
  while ((match = modifyRegex.exec(sql)) !== null) {
    const colName = match[1]
    let colDef = match[2].trim().replace(/,\s*$/, '')
    const commentMatch = colDef.match(/\s+COMMENT\s+'((?:[^']|'')*)'\s*$/i)
    if (!commentMatch) {
      continue
    }
    const comment = unescapeSqlString(commentMatch[1])
    colDef = colDef.replace(/\s+COMMENT\s+'(?:[^']|'')*'\s*$/i, '').trim()
    entry.columns.set(colName, { def: colDef, comment })
  }

  const addRegex = /ADD\s+COLUMN\s+(?:IF\s+NOT\s+EXISTS\s+)?`?([a-zA-Z0-9_]+)`?\s+([\s\S]*?)(?=,\s*ADD\s+COLUMN|;\s*$)/gi
  while ((match = addRegex.exec(sql)) !== null) {
    const colName = match[1]
    let colDef = match[2].trim().replace(/,\s*$/, '')
    colDef = colDef.replace(/\s+AFTER\s+`?[a-zA-Z0-9_]+`?\s*$/i, '').trim()
    const commentMatch = colDef.match(/\s+COMMENT\s+'((?:[^']|'')*)'\s*$/i)
    if (!commentMatch) {
      continue
    }
    const comment = unescapeSqlString(commentMatch[1])
    colDef = colDef.replace(/\s+COMMENT\s+'(?:[^']|'')*'\s*$/i, '').trim()
    entry.columns.set(colName, { def: colDef, comment })
  }
}

const createRegex = /CREATE\s+TABLE\s+IF\s+NOT\s+EXISTS\s+`?([a-zA-Z0-9_]+)`?\s*\(/gi
let createMatch
const createStarts = []
while ((createMatch = createRegex.exec(deployAll)) !== null) {
  createStarts.push({ tableName: createMatch[1], start: createMatch.index })
}

for (let i = 0; i < createStarts.length; i++) {
  const { tableName, start } = createStarts[i]
  const end = i + 1 < createStarts.length ? createStarts[i + 1].start : deployAll.length
  const block = deployAll.slice(start, end)
  if (block.includes('ENGINE=InnoDB')) {
    parseCreateTable(block, tableName)
  }
}

const alterRegex = /ALTER\s+TABLE[\s\S]*?;/gi
for (const alter of deployAll.match(alterRegex) || []) {
  parseAlterAddOrModify(alter)
}

for (const filePath of extraSqlFiles) {
  const sql = fs.readFileSync(filePath, 'utf8')
  for (const alter of sql.match(alterRegex) || []) {
    parseAlterAddOrModify(alter)
  }
}

const sortedTables = [...tables.keys()].sort()

const lines = []
lines.push('-- 全库表/字段 COMMENT 修复（由 deploy-all.sql 自动生成）')
lines.push('-- 生成：node admin-backend/sql/tools/generate_comment_fix.mjs')
lines.push('-- 执行：mysql -h 192.168.0.118 -u ai_manager -p123456 --default-character-set=utf8mb4 ai_manager_admin < all_table_comment_fix.sql')
lines.push('')
lines.push('SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;')
lines.push('USE ai_manager_admin;')
lines.push('')

for (const tableName of sortedTables) {
  const entry = tables.get(tableName)
  if (!entry) {
    continue
  }
  lines.push(`-- ========== ${tableName} ==========`)
  if (entry.tableComment) {
    lines.push(`ALTER TABLE \`${tableName}\` COMMENT = '${entry.tableComment.replace(/'/g, "''")}';`)
    lines.push('')
  }
  const cols = [...entry.columns.entries()]
  if (cols.length === 0) {
    lines.push('')
    continue
  }
  const chunks = []
  for (const [colName, { def, comment }] of cols) {
    const escapedComment = comment.replace(/'/g, "''")
    chunks.push(
      `    MODIFY COLUMN \`${colName}\` ${def} COMMENT '${escapedComment}'`,
    )
  }
  lines.push(`ALTER TABLE \`${tableName}\``)
  lines.push(chunks.join(',\n') + ';')
  lines.push('')
}

const outPath = path.join(sqlRoot, 'all_table_comment_fix.sql')
fs.writeFileSync(outPath, lines.join('\n'), 'utf8')
console.log(`Generated ${outPath} with ${sortedTables.length} tables`)
