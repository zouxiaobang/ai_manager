import html2canvas from 'html2canvas'
import ExcelJS from 'exceljs'
import { jsPDF } from 'jspdf'

export interface DeliveryNoteExportLine {
  skuCode: string
  specName: string
  productName: string
  orderedQty: number
  shippedQtyText: string
  cartonsText: string
}

export interface DeliveryNoteExportData {
  orderNo: string
  orderDateText: string
  shipDateText: string
  actualShipText: string
  showActualShip: boolean
  shipFromName: string
  shipFromPhone: string
  shipFromAddress: string
  shipToUnit: string
  shipToName: string
  shipToPhone: string
  shipToAddress: string
  remark: string
  lines: DeliveryNoteExportLine[]
  summaryPlannedQty: number
  summaryShippedQtyText: string
  summaryCartonsText: string
  requirementItems: string[]
  noteItems: string[]
}

export interface DeliveryNoteExportCompany {
  title: string
  address: string
  tel: string
  preparedBy: string
}

export interface DeliveryNoteExportLabels {
  orderNo: string
  orderDate: string
  shipDate: string
  actualShipDate: string
  shipFromName: string
  shipFromPhone: string
  shipFromAddress: string
  shipTo: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  linesTitle: string
  colIndex: string
  skuCode: string
  specName: string
  productName: string
  orderedQty: string
  shippedQty: string
  cartons: string
  summary: string
  remark: string
  requirements: string
  notesTitle: string
  preparedBy: string
  shipperSign: string
  receiverSign: string
  signDate: string
}

const COL_COUNT = 7

const COLUMN_WIDTHS = [6, 14, 12, 22, 10, 10, 10]

const BORDER_COLOR = 'FF000000'
const LABEL_FILL = 'FFF2F2F2'
const SECTION_FILL = 'FFEFF6FF'
const SUMMARY_FILL = 'FFFFF7ED'
const SHIP_DATE_COLOR = 'FFDC2626'

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function borderSides(): Partial<ExcelJS.Borders> {
  const side: ExcelJS.Border = { style: 'thin', color: { argb: BORDER_COLOR } }
  return { top: side, left: side, bottom: side, right: side }
}

function applyBorder(cell: ExcelJS.Cell) {
  cell.border = borderSides() as ExcelJS.Borders
}

function applyBordersInRange(ws: ExcelJS.Worksheet, r1: number, c1: number, r2: number, c2: number) {
  for (let row = r1; row <= r2; row += 1) {
    for (let col = c1; col <= c2; col += 1) {
      applyBorder(ws.getCell(row, col))
    }
  }
}

function mergeRange(ws: ExcelJS.Worksheet, r1: number, c1: number, r2: number, c2: number) {
  if (r1 !== r2 || c1 !== c2) {
    ws.mergeCells(r1, c1, r2, c2)
  }
  applyBordersInRange(ws, r1, c1, r2, c2)
}

function setCell(
  ws: ExcelJS.Worksheet,
  row: number,
  col: number,
  value: ExcelJS.CellValue,
  options?: {
    bold?: boolean
    size?: number
    fill?: string
    align?: Partial<ExcelJS.Alignment>
  },
) {
  const cell = ws.getCell(row, col)
  cell.value = value
  cell.alignment = {
    vertical: 'middle',
    wrapText: true,
    ...options?.align,
  }
  if (options?.bold || options?.size) {
    cell.font = { bold: options.bold, size: options.size }
  }
  if (options?.fill) {
    cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: options.fill } }
  }
  applyBorder(cell)
  return cell
}

function setLabelCell(ws: ExcelJS.Worksheet, row: number, col: number, value: string) {
  return setCell(ws, row, col, value, { bold: true, fill: LABEL_FILL })
}

function writeSectionTitleRow(ws: ExcelJS.Worksheet, row: number, text: string) {
  mergeRange(ws, row, 1, row, COL_COUNT)
  const cell = ws.getCell(row, 1)
  cell.value = text
  cell.alignment = { horizontal: 'center', vertical: 'middle', wrapText: true }
  cell.font = { bold: true }
  cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: SECTION_FILL } }
}

function estimateRowHeight(text: string, charsPerLine = 56, lineHeight = 15, minHeight = 24) {
  const lines = text.split('\n')
  let total = 0
  for (const line of lines) {
    total += Math.max(1, Math.ceil(line.length / charsPerLine))
  }
  return Math.max(minHeight, total * lineHeight)
}

function buildNumberedList(items: string[]) {
  return items.map((item, index) => `${index + 1}. ${item}`).join('\n')
}

function buildSignText(labels: DeliveryNoteExportLabels) {
  return [
    `${labels.shipperSign}：________________`,
    `${labels.receiverSign}：________________`,
    `${labels.signDate}：________________`,
  ].join('\n')
}

async function buildDeliveryNoteWorkbook(
  company: DeliveryNoteExportCompany,
  labels: DeliveryNoteExportLabels,
  data: DeliveryNoteExportData,
) {
  const workbook = new ExcelJS.Workbook()
  const ws = workbook.addWorksheet('送货单', {
    views: [{ showGridLines: false }],
  })

  COLUMN_WIDTHS.forEach((width, index) => {
    ws.getColumn(index + 1).width = width
  })

  let row = 1

  mergeRange(ws, row, 1, row, COL_COUNT)
  setCell(ws, row, 1, company.title, {
    bold: true,
    size: 16,
    align: { horizontal: 'center' },
  })
  ws.getRow(row).height = 30
  row += 1

  mergeRange(ws, row, 1, row, 3)
  setCell(ws, row, 1, company.address || '', { align: { horizontal: 'left', vertical: 'top' } })
  mergeRange(ws, row, 4, row, COL_COUNT)
  const metaLines = [
    `${labels.orderNo}：${data.orderNo}`,
    `${labels.orderDate}：${data.orderDateText}`,
    `${labels.shipDate}：${data.shipDateText}`,
  ]
  if (data.showActualShip) {
    metaLines.push(`${labels.actualShipDate}：${data.actualShipText}`)
  }
  const metaCell = setCell(ws, row, 4, metaLines.join('\n'), { align: { horizontal: 'right', vertical: 'top' } })
  ws.getRow(row).height = data.showActualShip ? 52 : 40
  row += 1

  mergeRange(ws, row, 1, row, 3)
  setCell(ws, row, 1, company.tel || '')
  mergeRange(ws, row, 4, row, COL_COUNT)
  setCell(ws, row, 4, '')
  row += 1

  setLabelCell(ws, row, 1, labels.shipFromName)
  setCell(ws, row, 2, data.shipFromName)
  setLabelCell(ws, row, 3, labels.shipFromPhone)
  mergeRange(ws, row, 4, row, COL_COUNT)
  setCell(ws, row, 4, data.shipFromPhone)
  row += 1

  setLabelCell(ws, row, 1, labels.shipFromAddress)
  mergeRange(ws, row, 2, row, COL_COUNT)
  setCell(ws, row, 2, data.shipFromAddress)
  row += 1

  setLabelCell(ws, row, 1, labels.shipTo)
  setCell(ws, row, 2, data.shipToUnit)
  setLabelCell(ws, row, 3, labels.receiverName)
  setCell(ws, row, 4, data.shipToName)
  setLabelCell(ws, row, 5, labels.receiverPhone)
  mergeRange(ws, row, 6, row, COL_COUNT)
  setCell(ws, row, 6, data.shipToPhone)
  row += 1

  setLabelCell(ws, row, 1, labels.receiverAddress)
  mergeRange(ws, row, 2, row, COL_COUNT)
  setCell(ws, row, 2, data.shipToAddress)
  row += 1

  writeSectionTitleRow(ws, row, labels.linesTitle)
  row += 1

  const lineHeaders = [
    labels.colIndex,
    labels.skuCode,
    labels.specName,
    labels.productName,
    labels.orderedQty,
    labels.shippedQty,
    labels.cartons,
  ]
  lineHeaders.forEach((header, index) => {
    setLabelCell(ws, row, index + 1, header)
    ws.getCell(row, index + 1).alignment = { horizontal: 'center', vertical: 'middle', wrapText: true }
  })
  ws.getRow(row).height = 26
  row += 1

  for (const [index, line] of data.lines.entries()) {
    const values: ExcelJS.CellValue[] = [
      index + 1,
      line.skuCode,
      line.specName,
      line.productName,
      line.orderedQty,
      line.shippedQtyText,
      line.cartonsText,
    ]
    values.forEach((value, colIndex) => {
      const cell = setCell(ws, row, colIndex + 1, value, colIndex === 1 ? { bold: true } : undefined)
      if (colIndex === 0) {
        cell.alignment = { horizontal: 'center', vertical: 'middle', wrapText: true }
      } else if (colIndex >= 4 && colIndex <= 6) {
        cell.alignment = { horizontal: 'center', vertical: 'middle', wrapText: true }
        cell.font = { bold: true, color: { argb: 'FFEA580C' } }
      }
    })
    row += 1
  }

  mergeRange(ws, row, 1, row, 4)
  setCell(ws, row, 1, labels.summary, { bold: true, align: { horizontal: 'right' } })
  setCell(ws, row, 5, data.summaryPlannedQty, { bold: true, align: { horizontal: 'center' } })
  setCell(ws, row, 6, data.summaryShippedQtyText, { bold: true, align: { horizontal: 'center' } })
  setCell(ws, row, 7, data.summaryCartonsText, { bold: true, align: { horizontal: 'center' } })
  for (let col = 1; col <= COL_COUNT; col += 1) {
    ws.getCell(row, col).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: SUMMARY_FILL } }
  }
  row += 1

  if (data.remark.trim()) {
    setLabelCell(ws, row, 1, labels.remark)
    mergeRange(ws, row, 2, row, COL_COUNT)
    setCell(ws, row, 2, data.remark, { align: { horizontal: 'left', vertical: 'top' } })
    row += 1
  }

  const requirementText = buildNumberedList(data.requirementItems)
  const notesText = buildNumberedList(data.noteItems)
  const signText = `${labels.preparedBy}：${company.preparedBy}\n${buildSignText(labels)}`
  const footerHeight = Math.max(
    estimateRowHeight(requirementText, 28, 15, 96),
    estimateRowHeight(notesText, 34, 15, 96),
    estimateRowHeight(signText, 24, 15, 96),
  )

  mergeRange(ws, row, 1, row, 2)
  setCell(ws, row, 1, `${labels.requirements}\n${requirementText}`, { align: { horizontal: 'left', vertical: 'top' } })
  mergeRange(ws, row, 3, row, 4)
  setCell(ws, row, 3, `${labels.notesTitle}\n${notesText}`, { align: { horizontal: 'left', vertical: 'top' } })
  mergeRange(ws, row, 5, row, COL_COUNT)
  setCell(ws, row, 5, signText, { align: { horizontal: 'left', vertical: 'top' } })
  ws.getRow(row).height = footerHeight

  const shipDateRich: ExcelJS.CellRichTextValue = {
    richText: [
      { text: `${labels.orderNo}：${data.orderNo}\n` },
      { text: `${labels.orderDate}：${data.orderDateText}\n` },
      { text: `${labels.shipDate}：`, font: { size: 11, color: { argb: 'FF111111' } } },
      { text: `${data.shipDateText}\n`, font: { size: 11, bold: true, color: { argb: SHIP_DATE_COLOR } } },
      ...(data.showActualShip
        ? [{ text: `${labels.actualShipDate}：${data.actualShipText}`, font: { size: 11 } }]
        : []),
    ],
  }
  metaCell.value = shipDateRich

  return workbook
}

export async function exportDeliveryNoteExcel(
  filename: string,
  company: DeliveryNoteExportCompany,
  labels: DeliveryNoteExportLabels,
  data: DeliveryNoteExportData,
) {
  const workbook = await buildDeliveryNoteWorkbook(company, labels, data)
  const buffer = await workbook.xlsx.writeBuffer()
  downloadBlob(
    new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
    filename.endsWith('.xlsx') ? filename : `${filename}.xlsx`,
  )
}

export async function exportDeliveryNotePdf(container: HTMLElement, filename: string) {
  const sections = Array.from(container.querySelectorAll<HTMLElement>('.dn-sheet'))
  const targets = sections.length ? sections : [container]
  const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' })
  const pageWidth = pdf.internal.pageSize.getWidth()
  const pageHeight = pdf.internal.pageSize.getHeight()
  const margin = 8

  for (let i = 0; i < targets.length; i += 1) {
    const canvas = await html2canvas(targets[i], {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
    })
    const imgData = canvas.toDataURL('image/png')
    const imgWidth = pageWidth - margin * 2
    const imgHeight = (canvas.height * imgWidth) / canvas.width
    const renderHeight = Math.min(imgHeight, pageHeight - margin * 2)
    if (i > 0) pdf.addPage()
    pdf.addImage(imgData, 'PNG', margin, margin, imgWidth, renderHeight)
  }

  pdf.save(filename.endsWith('.pdf') ? filename : `${filename}.pdf`)
}
