const BREAK_BEFORE = [
  'LEFT JOIN',
  'RIGHT JOIN',
  'INNER JOIN',
  'INSERT INTO',
  'DELETE FROM',
  'GROUP BY',
  'ORDER BY',
  'SELECT',
  'FROM',
  'WHERE',
  'HAVING',
  'LIMIT',
  'JOIN',
  'INTO',
  'VALUES',
  'UPDATE',
  'SET',
  'ON',
  'AND',
  'OR',
]

export function formatSql(sql: string): string {
  let text = sql.trim()
  if (!text) {
    return text
  }
  text = text.replace(/\s+/g, ' ')
  for (const keyword of BREAK_BEFORE) {
    const pattern = new RegExp(`\\s+${keyword.replace(/\s+/g, '\\s+')}\\s+`, 'gi')
    text = text.replace(pattern, `\n${keyword} `)
  }
  return text
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .join('\n')
}
