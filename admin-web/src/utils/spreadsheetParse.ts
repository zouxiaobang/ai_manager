import * as XLSX from 'xlsx'

export interface ParsedSpreadsheet {
  fileName: string
  fileType: 'CSV' | 'XLSX' | 'XLS'
  columns: string[]
  headerRow: number
  dataStartRow: number
}

function detectFileType(fileName: string): ParsedSpreadsheet['fileType'] {
  const lower = fileName.toLowerCase()
  if (lower.endsWith('.csv') || lower.endsWith('.txt')) return 'CSV'
  if (lower.endsWith('.xls')) return 'XLS'
  return 'XLSX'
}

function splitCsvLine(line: string): string[] {
  const cols: string[] = []
  let current = ''
  let inQuotes = false
  for (let i = 0; i < line.length; i++) {
    const c = line.charAt(i)
    if (c === '"') {
      inQuotes = !inQuotes
    } else if (c === ',' && !inQuotes) {
      cols.push(trimCsvCell(current))
      current = ''
    } else {
      current += c
    }
  }
  cols.push(trimCsvCell(current))
  return cols
}

function trimCsvCell(cell: string): string {
  let v = (cell ?? '').trim()
  if (v.length >= 2 && v.startsWith('"') && v.endsWith('"')) {
    v = v.slice(1, -1).trim()
  }
  return v
}

function parseCsvColumns(text: string, headerRow = 1): string[] {
  const lines = text.split(/\r?\n/).filter((l) => l.trim())
  if (lines.length < headerRow) return []
  return splitCsvLine(lines[headerRow - 1]).map((h) => h.trim()).filter(Boolean)
}

function parseExcelColumns(buffer: ArrayBuffer, headerRow = 1): string[] {
  const workbook = XLSX.read(buffer, { type: 'array' })
  const sheetName = workbook.SheetNames[0]
  if (!sheetName) return []
  const sheet = workbook.Sheets[sheetName]
  const rows = XLSX.utils.sheet_to_json<(string | number | null)[]>(sheet, {
    header: 1,
    defval: '',
    raw: false,
  })
  if (rows.length < headerRow) return []
  const header = rows[headerRow - 1] ?? []
  return header.map((c) => String(c ?? '').trim()).filter(Boolean)
}

export async function detectSpreadsheetColumns(file: File, headerRow = 1): Promise<ParsedSpreadsheet> {
  const fileType = detectFileType(file.name)
  let columns: string[] = []
  if (fileType === 'CSV') {
    const text = await file.text()
    columns = parseCsvColumns(text, headerRow)
  } else {
    const buffer = await file.arrayBuffer()
    columns = parseExcelColumns(buffer, headerRow)
  }
  return {
    fileName: file.name,
    fileType,
    columns,
    headerRow,
    dataStartRow: headerRow + 1,
  }
}
