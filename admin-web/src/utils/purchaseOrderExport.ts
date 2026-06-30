import html2canvas from 'html2canvas'
import ExcelJS from 'exceljs'
import { jsPDF } from 'jspdf'

export interface PurchaseOrderExportLine {
  skuCode: string
  productName: string
  outerPack: string
  totalCartons: string
  quantity: number
  unitPriceText: string
  totalAmountText: string
  discountText: string
  actualPaidText: string
}

export interface PurchaseOrderExportData {
  orderNo: string
  orderDateText: string
  shipDateText: string
  factoryContact: string
  factoryPhone: string
  lines: PurchaseOrderExportLine[]
  summaryQuantity: number
  summaryTotalCartonsText: string
  summaryTotalAmountText: string
  summaryActualPaidText: string
  requirementItems: string[]
  noteItems: string[]
}

export interface PurchaseOrderExportCompany {
  title: string
  address: string
  tel: string
  companyNo: string
  preparedBy: string
  preparedPhone: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
}

export interface PurchaseOrderExportLabels {
  orderDate: string
  shipDate: string
  factoryContact: string
  factoryPhone: string
  orderNo: string
  factorySku: string
  productName: string
  outerPack: string
  totalCartons: string
  totalQty: string
  unitPrice: string
  totalAmount: string
  discount: string
  actualPaid: string
  linesTitle: string
  colIndex: string
  summary: string
  requirements: string
  cartonMark: string
  innerMark: string
  markLater: string
  notesTitle: string
  preparedBy: string
  preparedPhone: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
}

const COL_COUNT = 10

const COLUMN_WIDTHS = [6, 14, 20, 11, 10, 10, 10, 12, 10, 13]

const BORDER_COLOR = 'FF000000'
const LABEL_FILL = 'FFF2F2F2'
const SECTION_FILL = 'FFF2F2F2'
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
    color?: string
  },
) {
  const cell = ws.getCell(row, col)
  cell.value = value
  cell.alignment = {
    vertical: 'middle',
    wrapText: true,
    ...options?.align,
  }
  if (options?.bold || options?.size || options?.color) {
    cell.font = {
      bold: options.bold,
      size: options.size,
      color: options.color ? { argb: options.color } : undefined,
    }
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

function estimateRowHeight(text: string, charsPerLine = 72, lineHeight = 15, minHeight = 24) {
  const lines = text.split('\n')
  let total = 0
  for (const line of lines) {
    total += Math.max(1, Math.ceil(line.length / charsPerLine))
  }
  return Math.max(minHeight, total * lineHeight)
}

function buildMarksText(labels: PurchaseOrderExportLabels) {
  return `${labels.cartonMark}：${labels.markLater}\n${labels.innerMark}：${labels.markLater}`
}

function buildSignText(company: PurchaseOrderExportCompany, labels: PurchaseOrderExportLabels) {
  return [
    `${labels.preparedBy}：${company.preparedBy || ''}`,
    `${labels.preparedPhone}：${company.preparedPhone || ''}`,
    `${labels.receiverName}：${company.receiverName || ''}`,
    `${labels.receiverPhone}：${company.receiverPhone || ''}`,
    `${labels.receiverAddress}：${company.receiverAddress || ''}`,
  ].join('\n')
}

function buildNumberedList(items: string[]) {
  return items.map((item, index) => `${index + 1}. ${item}`).join('\n')
}

async function buildPurchaseOrderWorkbook(
  company: PurchaseOrderExportCompany,
  labels: PurchaseOrderExportLabels,
  data: PurchaseOrderExportData,
) {
  const workbook = new ExcelJS.Workbook()
  const ws = workbook.addWorksheet('采购单', {
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

  mergeRange(ws, row, 1, row, 5)
  setCell(ws, row, 1, company.address || '', { align: { horizontal: 'left', vertical: 'top' } })
  mergeRange(ws, row, 6, row, COL_COUNT)
  const dateCell = setCell(
    ws,
    row,
    6,
    `${labels.orderDate}：${data.orderDateText}\n${labels.shipDate}：${data.shipDateText}`,
    { align: { horizontal: 'right', vertical: 'top' } },
  )
  dateCell.font = { size: 11 }
  dateCell.alignment = { horizontal: 'right', vertical: 'top', wrapText: true }
  ws.getRow(row).height = 34
  row += 1

  mergeRange(ws, row, 1, row, 5)
  setCell(ws, row, 1, company.tel || '')
  mergeRange(ws, row, 6, row, COL_COUNT)
  setCell(ws, row, 6, '')
  row += 1

  setLabelCell(ws, row, 1, labels.factoryContact)
  mergeRange(ws, row, 2, row, 5)
  setCell(ws, row, 2, data.factoryContact)
  setLabelCell(ws, row, 6, labels.factoryPhone)
  mergeRange(ws, row, 7, row, COL_COUNT)
  setCell(ws, row, 7, data.factoryPhone)
  row += 1

  setLabelCell(ws, row, 1, labels.orderNo)
  mergeRange(ws, row, 2, row, COL_COUNT)
  setCell(ws, row, 2, data.orderNo)
  row += 1

  writeSectionTitleRow(ws, row, labels.linesTitle)
  row += 1

  const lineHeaders = [
    labels.colIndex,
    labels.factorySku,
    labels.productName,
    labels.outerPack,
    labels.totalCartons,
    labels.totalQty,
    labels.unitPrice,
    labels.totalAmount,
    labels.discount,
    labels.actualPaid,
  ]
  lineHeaders.forEach((header, index) => {
    setLabelCell(ws, row, index + 1, header)
    ws.getCell(row, index + 1).alignment = { horizontal: 'center', vertical: 'middle', wrapText: true }
  })
  ws.getRow(row).height = 28
  row += 1

  for (const [index, line] of data.lines.entries()) {
    const values: ExcelJS.CellValue[] = [
      index + 1,
      line.skuCode,
      line.productName,
      line.outerPack,
      line.totalCartons,
      line.quantity,
      line.unitPriceText,
      line.totalAmountText,
      line.discountText,
      line.actualPaidText,
    ]
    values.forEach((value, colIndex) => {
      const cell = setCell(ws, row, colIndex + 1, value, colIndex === 1 ? { bold: true } : undefined)
      if (colIndex === 0) {
        cell.alignment = { horizontal: 'center', vertical: 'middle', wrapText: true }
      } else if (colIndex >= 5 && colIndex <= 9) {
        cell.alignment = { horizontal: 'right', vertical: 'middle', wrapText: true }
      }
    })
    row += 1
  }

  mergeRange(ws, row, 1, row, 3)
  setCell(ws, row, 1, labels.summary, { bold: true, align: { horizontal: 'right' } })
  setCell(ws, row, 4, '')
  setCell(ws, row, 5, data.summaryTotalCartonsText, { align: { horizontal: 'right' } })
  setCell(ws, row, 6, data.summaryQuantity, { bold: true, align: { horizontal: 'right' } })
  setCell(ws, row, 7, '')
  setCell(ws, row, 8, data.summaryTotalAmountText, { bold: true, align: { horizontal: 'right' } })
  setCell(ws, row, 9, '')
  setCell(ws, row, 10, data.summaryActualPaidText, { bold: true, align: { horizontal: 'right' } })
  for (let col = 1; col <= COL_COUNT; col += 1) {
    ws.getCell(row, col).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFFAFAFA' } }
  }
  row += 1

  writeSectionTitleRow(ws, row, labels.requirements)
  row += 1

  const requirementText = buildNumberedList(data.requirementItems)
  mergeRange(ws, row, 1, row, COL_COUNT)
  setCell(ws, row, 1, requirementText, { align: { horizontal: 'left', vertical: 'top' } })
  ws.getRow(row).height = estimateRowHeight(requirementText)
  row += 1

  const marksText = buildMarksText(labels)
  const notesText = `${labels.notesTitle}\n${buildNumberedList(data.noteItems)}`
  const signText = buildSignText(company, labels)
  const footerHeight = Math.max(
    estimateRowHeight(marksText, 24, 15, 72),
    estimateRowHeight(notesText, 34, 15, 72),
    estimateRowHeight(signText, 24, 15, 96),
  )

  mergeRange(ws, row, 1, row, 3)
  setCell(ws, row, 1, marksText, { align: { horizontal: 'left', vertical: 'top' } })
  mergeRange(ws, row, 4, row, 7)
  setCell(ws, row, 4, notesText, { align: { horizontal: 'left', vertical: 'top' } })
  mergeRange(ws, row, 8, row, COL_COUNT)
  setCell(ws, row, 8, signText, { align: { horizontal: 'left', vertical: 'top' } })
  ws.getRow(row).height = footerHeight

  const shipDateRich: ExcelJS.CellRichTextValue = {
    richText: [
      { text: `${labels.orderDate}：${data.orderDateText}\n` },
      { text: `${labels.shipDate}：`, font: { size: 11, color: { argb: 'FF111111' } } },
      { text: data.shipDateText, font: { size: 11, bold: true, color: { argb: SHIP_DATE_COLOR } } },
    ],
  }
  dateCell.value = shipDateRich

  return workbook
}

export async function exportPurchaseOrderExcel(
  filename: string,
  company: PurchaseOrderExportCompany,
  labels: PurchaseOrderExportLabels,
  data: PurchaseOrderExportData,
) {
  const workbook = await buildPurchaseOrderWorkbook(company, labels, data)
  const buffer = await workbook.xlsx.writeBuffer()
  downloadBlob(
    new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
    filename.endsWith('.xlsx') ? filename : `${filename}.xlsx`,
  )
}

export async function exportPurchaseOrderPdf(container: HTMLElement, filename: string) {
  const sections = Array.from(container.querySelectorAll<HTMLElement>('.po-sheet'))
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
