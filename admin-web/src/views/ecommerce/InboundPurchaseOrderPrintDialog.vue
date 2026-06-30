<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('ecommerce.inbound.printPurchaseTitle')"
    width="1120px"
    append-to-body
    destroy-on-close
    align-center
    :show-close="false"
    class="inbound-po-print-dialog"
    @update:model-value="emit('update:modelValue', $event)"
    @opened="onOpened"
  >
    <div v-loading="loading" class="inbound-po-print">
      <div v-if="order && sheetLines.length" ref="printRootRef" class="inbound-po-print__pages">
        <section class="po-sheet">
          <table class="po-grid" cellspacing="0" cellpadding="0">
            <colgroup>
              <col v-for="n in 10" :key="n" class="po-grid__col" />
            </colgroup>
            <tbody>
              <tr>
                <td colspan="10" class="po-grid__title">{{ company.title }}</td>
              </tr>
              <tr>
                <td colspan="5" class="po-grid__meta-left">{{ company.address }}</td>
                <td colspan="5" class="po-grid__meta-right">
                  <div>{{ t('ecommerce.inbound.poOrderDate') }}：{{ orderDateText }}</div>
                  <div class="po-grid__ship-date">{{ t('ecommerce.inbound.poShipDate') }}：{{ shipDateText }}</div>
                </td>
              </tr>
              <tr>
                <td colspan="5" class="po-grid__meta-left">{{ company.tel }}</td>
                <td colspan="5" class="po-grid__meta-right" />
              </tr>
              <tr>
                <td class="po-grid__label">{{ t('ecommerce.inbound.poFactoryContact') }}</td>
                <td colspan="4">{{ factoryContact }}</td>
                <td class="po-grid__label">{{ t('ecommerce.inbound.poFactoryPhone') }}</td>
                <td colspan="4">{{ factoryPhone }}</td>
              </tr>
              <tr>
                <td class="po-grid__label">{{ t('ecommerce.inbound.poOrderNo') }}</td>
                <td colspan="9">{{ orderNoText }}</td>
              </tr>
              <tr>
                <td colspan="10" class="po-grid__section-title">{{ t('ecommerce.inbound.poLinesTitle') }}</td>
              </tr>
              <tr>
                <td colspan="10" class="po-grid__lines-cell">
                  <table class="po-lines-table" cellspacing="0" cellpadding="0">
                    <thead>
                      <tr>
                        <th class="po-lines-table__index">{{ t('ecommerce.inbound.poColIndex') }}</th>
                        <th>{{ t('ecommerce.inbound.poFactorySku') }}</th>
                        <th>{{ t('ecommerce.inbound.poProductName') }}</th>
                        <th>{{ t('ecommerce.inbound.poOuterPack') }}</th>
                        <th>{{ t('ecommerce.inbound.poTotalCartons') }}</th>
                        <th>{{ t('ecommerce.inbound.poTotalQty') }}</th>
                        <th>{{ t('ecommerce.inbound.poUnitPrice') }}</th>
                        <th>{{ t('ecommerce.inbound.poTotalAmount') }}</th>
                        <th>{{ t('ecommerce.inbound.poDiscount') }}</th>
                        <th>{{ t('ecommerce.inbound.poActualPaid') }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(sheet, index) in sheetLines" :key="`${sheet.line.skuCode}-${index}`">
                        <td class="po-lines-table__index">{{ index + 1 }}</td>
                        <td><strong>{{ sheet.line.skuCode }}</strong></td>
                        <td>{{ sheet.line.productName || '—' }}</td>
                        <td class="po-grid__editable">
                          <template v-if="pdfExporting">
                            {{ sheet.outerPackText }}
                          </template>
                          <template v-else>
                            <input
                              v-model.number="sheet.unitsPerCarton"
                              type="number"
                              step="1"
                              min="1"
                              class="po-grid__number-input"
                              @input="syncSheetCartons(sheet)"
                            />
                            <span class="po-grid__input-unit">{{ t('ecommerce.inventory.unitPiece') }}</span>
                          </template>
                        </td>
                        <td>{{ sheet.totalCartonsText }}</td>
                        <td class="po-lines-table__qty">{{ sheet.line.quantity }}</td>
                        <td class="po-lines-table__money">{{ sheet.unitPriceText }}</td>
                        <td class="po-lines-table__money">{{ sheet.totalAmountText }}</td>
                        <td class="po-grid__editable">
                          <template v-if="pdfExporting">
                            <template v-if="sheet.discountZhe != null">{{ sheet.discountText }}{{ t('ecommerce.inbound.poDiscountZhe') }}</template>
                            <span v-else>—</span>
                          </template>
                          <template v-else-if="sheet.totalAmount != null && sheet.discountZhe != null">
                            <input
                              v-model.number="sheet.discountZhe"
                              type="number"
                              step="0.1"
                              min="0"
                              max="10"
                              class="po-grid__discount-input"
                              @input="syncSheetAmounts(sheet)"
                            />
                            <span class="po-grid__discount-unit">{{ t('ecommerce.inbound.poDiscountZhe') }}</span>
                          </template>
                          <span v-else>—</span>
                        </td>
                        <td class="po-lines-table__money">{{ sheet.actualPaidText }}</td>
                      </tr>
                    </tbody>
                    <tfoot>
                      <tr class="po-lines-table__summary">
                        <td colspan="3" class="po-lines-table__summary-label">{{ t('ecommerce.inbound.poSummary') }}</td>
                        <td />
                        <td>{{ summary.totalCartonsText }}</td>
                        <td class="po-lines-table__qty">{{ summary.quantity }}</td>
                        <td />
                        <td class="po-lines-table__money">{{ summary.totalAmountText }}</td>
                        <td />
                        <td class="po-lines-table__money">{{ summary.actualPaidText }}</td>
                      </tr>
                    </tfoot>
                  </table>
                </td>
              </tr>
              <tr>
                <td colspan="10" class="po-grid__section-title">{{ t('ecommerce.inbound.poRequirements') }}</td>
              </tr>
              <tr>
                <td colspan="10" class="po-grid__requirements">
                  <ol>
                    <li v-for="(item, reqIndex) in requirementItems" :key="reqIndex">{{ item }}</li>
                  </ol>
                </td>
              </tr>
              <tr>
                <td colspan="3" class="po-grid__footer-cell">
                  <div class="po-sheet__mark-row">
                    <span>{{ t('ecommerce.inbound.poCartonMark') }}</span>
                    <span>{{ t('ecommerce.inbound.poMarkLater') }}</span>
                  </div>
                  <div class="po-sheet__mark-row">
                    <span>{{ t('ecommerce.inbound.poInnerMark') }}</span>
                    <span>{{ t('ecommerce.inbound.poMarkLater') }}</span>
                  </div>
                </td>
                <td colspan="4" class="po-grid__footer-cell">
                  <div class="po-grid__section-title po-grid__section-title--inline">
                    {{ t('ecommerce.inbound.poNotesTitle') }}
                  </div>
                  <ol class="po-grid__notes-list">
                    <li v-for="(item, noteIndex) in noteItems" :key="noteIndex">{{ item }}</li>
                  </ol>
                </td>
                <td colspan="3" class="po-grid__footer-cell po-grid__sign">
                  <p>{{ t('ecommerce.inbound.poPreparedBy') }}：{{ company.preparedBy }}</p>
                  <p>{{ t('ecommerce.inbound.poPreparedPhone') }}：{{ company.preparedPhone }}</p>
                  <p>{{ t('ecommerce.inbound.poReceiverName') }}：{{ company.receiverName }}</p>
                  <p>{{ t('ecommerce.inbound.poReceiverPhone') }}：{{ company.receiverPhone }}</p>
                  <p>{{ t('ecommerce.inbound.poReceiverAddress') }}：{{ company.receiverAddress }}</p>
                </td>
              </tr>
            </tbody>
          </table>
        </section>
      </div>
      <el-empty v-else-if="!loading" :description="t('ecommerce.inbound.linesRequired')" :image-size="64" />
    </div>
    <template #footer>
      <el-dropdown trigger="click" :disabled="!sheetLines.length" @command="onExport">
        <el-button type="primary" :disabled="!sheetLines.length" :loading="exporting">
          {{ t('ecommerce.inbound.exportAction') }}
          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="excel">{{ t('ecommerce.inbound.exportExcel') }}</el-dropdown-item>
            <el-dropdown-item command="pdf">{{ t('ecommerce.inbound.exportPdf') }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import type { EcInboundOrder } from '@/api/ecommerce/inbound'
import { formatMoney as formatCnyPlain } from '@/utils/formatMoney'

function formatPoMoney(value: number) {
  if (!Number.isFinite(value)) return '—'
  return formatCnyPlain(value, { symbol: false })
}
import { fetchFactory, type EcFactory } from '@/api/ecommerce/factory'
import { fetchInventorySkuOptions } from '@/api/ecommerce/inventory'
import { fetchProduct, type EcSku } from '@/api/ecommerce/product'
import { fetchPurchaseOrderConfig, type EcPurchaseOrderConfig } from '@/api/ecommerce/purchaseOrderConfig'
import { formatDate } from '@/utils/date'
import {
  exportPurchaseOrderExcel,
  exportPurchaseOrderPdf,
  type PurchaseOrderExportCompany,
  type PurchaseOrderExportData,
  type PurchaseOrderExportLabels,
} from '@/utils/purchaseOrderExport'

interface PurchaseSheetLine {
  line: NonNullable<EcInboundOrder['lines']>[number]
  unitsPerCarton: number | null
  totalCartonsText: string
  outerPackText: string
  unitPriceText: string
  totalAmount: number | null
  totalAmountText: string
  discountZhe: number | null
  discountText: string
  actualPaidText: string
}

const props = defineProps<{
  modelValue: boolean
  order: EcInboundOrder | null
}>()

const emit = defineEmits<{ 'update:modelValue': [boolean] }>()

const { t } = useI18n()

const loading = ref(false)
const exporting = ref(false)
const pdfExporting = ref(false)
const printRootRef = ref<HTMLElement | null>(null)
const factory = ref<EcFactory | null>(null)
const sheetLines = ref<PurchaseSheetLine[]>([])
const poConfig = ref<EcPurchaseOrderConfig | null>(null)

const company = computed<PurchaseOrderExportCompany>(() => ({
  title: poConfig.value?.title || '唯十嘉采购单',
  address: poConfig.value?.address || '',
  tel: poConfig.value?.tel || '',
  companyNo: poConfig.value?.companyNo || '',
  preparedBy: poConfig.value?.preparedBy || '',
  preparedPhone: poConfig.value?.preparedPhone || '',
  receiverName: poConfig.value?.receiverName || '',
  receiverPhone: poConfig.value?.receiverPhone || '',
  receiverAddress: poConfig.value?.receiverAddress || '',
}))

const requirementItems = computed(() => poConfig.value?.requirementItems?.filter(Boolean) ?? [])
const noteItems = computed(() => poConfig.value?.noteItems?.filter(Boolean) ?? [])

const orderDateText = computed(() => formatDate(props.order?.orderTime))
const shipDateText = computed(() => formatDate(props.order?.expectedDeliveryTime))
const orderNoText = computed(() => props.order?.orderNo || '—')

const factoryContact = computed(() => {
  const name = factory.value?.contactName
  const factoryName = props.order?.factoryName
  if (name && factoryName) return `${factoryName} ${name}`
  return factoryName || name || '—'
})

const factoryPhone = computed(() => factory.value?.contactPhone || '—')

const summary = computed(() => {
  let quantity = 0
  let totalCartons = 0
  let totalAmount = 0
  let actualPaid = 0
  for (const sheet of sheetLines.value) {
    quantity += sheet.line.quantity
    const cartons = parseCartonCount(sheet.totalCartonsText)
    if (cartons != null) totalCartons += cartons
    if (sheet.totalAmount != null) {
      totalAmount += sheet.totalAmount
      if (sheet.discountZhe != null) {
        actualPaid += sheet.totalAmount * (Math.min(10, Math.max(0, sheet.discountZhe)) / 10)
      }
    }
  }
  return {
    quantity,
    totalCartonsText: totalCartons > 0 ? `${totalCartons}${t('ecommerce.inbound.poCartonUnit')}` : '—',
    totalAmountText: formatPoMoney(totalAmount),
    actualPaidText: formatPoMoney(actualPaid),
  }
})

const exportLabels = computed<PurchaseOrderExportLabels>(() => ({
  orderDate: t('ecommerce.inbound.poOrderDate'),
  shipDate: t('ecommerce.inbound.poShipDate'),
  factoryContact: t('ecommerce.inbound.poFactoryContact'),
  factoryPhone: t('ecommerce.inbound.poFactoryPhone'),
  orderNo: t('ecommerce.inbound.poOrderNo'),
  factorySku: t('ecommerce.inbound.poFactorySku'),
  productName: t('ecommerce.inbound.poProductName'),
  outerPack: t('ecommerce.inbound.poOuterPack'),
  totalCartons: t('ecommerce.inbound.poTotalCartons'),
  totalQty: t('ecommerce.inbound.poTotalQty'),
  unitPrice: t('ecommerce.inbound.poUnitPrice'),
  totalAmount: t('ecommerce.inbound.poTotalAmount'),
  discount: t('ecommerce.inbound.poDiscount'),
  actualPaid: t('ecommerce.inbound.poActualPaid'),
  linesTitle: t('ecommerce.inbound.poLinesTitle'),
  colIndex: t('ecommerce.inbound.poColIndex'),
  summary: t('ecommerce.inbound.poSummary'),
  requirements: t('ecommerce.inbound.poRequirements'),
  cartonMark: t('ecommerce.inbound.poCartonMark'),
  innerMark: t('ecommerce.inbound.poInnerMark'),
  markLater: t('ecommerce.inbound.poMarkLater'),
  notesTitle: t('ecommerce.inbound.poNotesTitle'),
  preparedBy: t('ecommerce.inbound.poPreparedBy'),
  preparedPhone: t('ecommerce.inbound.poPreparedPhone'),
  receiverName: t('ecommerce.inbound.poReceiverName'),
  receiverPhone: t('ecommerce.inbound.poReceiverPhone'),
  receiverAddress: t('ecommerce.inbound.poReceiverAddress'),
}))

function parseCartonCount(text: string) {
  const matched = text.match(/^(\d+)/)
  if (!matched) return null
  const value = Number(matched[1])
  return Number.isFinite(value) ? value : null
}

function rebateToDiscountZhe(rebatePct?: number | null) {
  if (rebatePct == null) return 10
  return Math.round((100 - rebatePct) * 10) / 100
}

function syncSheetAmounts(sheet: PurchaseSheetLine) {
  if (sheet.totalAmount == null || sheet.discountZhe == null) {
    sheet.discountText = '—'
    sheet.actualPaidText = '—'
    return
  }
  const discountZhe = Math.min(10, Math.max(0, sheet.discountZhe))
  sheet.discountZhe = discountZhe
  sheet.discountText = discountZhe.toFixed(1)
  sheet.actualPaidText = formatPoMoney(sheet.totalAmount * (discountZhe / 10))
}

function syncSheetCartons(sheet: PurchaseSheetLine) {
  const units = sheet.unitsPerCarton
  if (units != null && units > 0) {
    sheet.unitsPerCarton = Math.floor(units)
    const cartons = Math.ceil(sheet.line.quantity / sheet.unitsPerCarton)
    sheet.totalCartonsText = `${cartons}${t('ecommerce.inbound.poCartonUnit')}`
    sheet.outerPackText = `${sheet.unitsPerCarton}${t('ecommerce.inventory.unitPiece')}`
    return
  }
  sheet.unitsPerCarton = units != null && units <= 0 ? null : units
  sheet.totalCartonsText = '—'
  sheet.outerPackText = '—'
}

function buildSheetLine(
  line: NonNullable<EcInboundOrder['lines']>[number],
  sku?: EcSku | null,
  rebatePct?: number | null,
): PurchaseSheetLine {
  const unitsPerCarton = sku?.unitsPerCarton && sku.unitsPerCarton > 0 ? sku.unitsPerCarton : null
  const unitPrice = sku?.salePrice ?? null
  const totalAmount = unitPrice != null ? unitPrice * line.quantity : null
  const discountZhe = totalAmount != null ? rebateToDiscountZhe(rebatePct ?? sku?.rebatePct) : null

  const sheet: PurchaseSheetLine = {
    line,
    unitsPerCarton,
    totalCartonsText: '—',
    outerPackText: '—',
    unitPriceText: unitPrice != null ? formatPoMoney(unitPrice) : '—',
    totalAmount,
    totalAmountText: totalAmount != null ? formatPoMoney(totalAmount) : '—',
    discountZhe,
    discountText: '—',
    actualPaidText: '—',
  }
  syncSheetCartons(sheet)
  syncSheetAmounts(sheet)
  return sheet
}

function buildExportData(): PurchaseOrderExportData {
  return {
    orderNo: orderNoText.value,
    orderDateText: orderDateText.value,
    shipDateText: shipDateText.value,
    factoryContact: factoryContact.value,
    factoryPhone: factoryPhone.value,
    lines: sheetLines.value.map((sheet) => ({
      skuCode: sheet.line.skuCode,
      productName: sheet.line.productName || '—',
      outerPack: sheet.outerPackText,
      totalCartons: sheet.totalCartonsText,
      quantity: sheet.line.quantity,
      unitPriceText: sheet.unitPriceText,
      totalAmountText: sheet.totalAmountText,
      discountText: sheet.discountText,
      actualPaidText: sheet.actualPaidText,
    })),
    summaryQuantity: summary.value.quantity,
    summaryTotalCartonsText: summary.value.totalCartonsText,
    summaryTotalAmountText: summary.value.totalAmountText,
    summaryActualPaidText: summary.value.actualPaidText,
    requirementItems: requirementItems.value,
    noteItems: noteItems.value,
  }
}

function exportFilename() {
  const orderNo = props.order?.orderNo || 'purchase-order'
  return `${orderNo}-采购单`
}

async function loadSheetData() {
  const order = props.order
  if (!order?.lines?.length) {
    sheetLines.value = []
    factory.value = null
    return
  }

  loading.value = true
  try {
    poConfig.value = await fetchPurchaseOrderConfig()
    factory.value = order.factoryId ? await fetchFactory(order.factoryId) : null
    const skuOptions = await fetchInventorySkuOptions(order.factoryId ?? undefined)
    const productCache = new Map<number, Awaited<ReturnType<typeof fetchProduct>>>()

    sheetLines.value = await Promise.all(
      order.lines.map(async (line) => {
        const opt = skuOptions.find((item) => item.skuCode === line.skuCode)
        let sku: EcSku | undefined
        let rebatePct: number | null | undefined
        if (opt?.productId) {
          let product = productCache.get(opt.productId)
          if (!product) {
            product = await fetchProduct(opt.productId)
            productCache.set(opt.productId, product)
          }
          sku = product.skus.find((item) => item.skuCode === line.skuCode)
          rebatePct = product.rebatePct
        }
        return buildSheetLine(line, sku, rebatePct)
      }),
    )
  } finally {
    loading.value = false
  }
}

async function onOpened() {
  await loadSheetData()
}

async function onExport(command: string | number | object) {
  if (!printRootRef.value || !sheetLines.value.length) return
  exporting.value = true
  try {
    const filename = exportFilename()
    const data = buildExportData()
    if (command === 'excel') {
      await exportPurchaseOrderExcel(filename, company.value, exportLabels.value, data)
    } else if (command === 'pdf') {
      pdfExporting.value = true
      await nextTick()
      try {
        await exportPurchaseOrderPdf(printRootRef.value, filename)
      } finally {
        pdfExporting.value = false
      }
    }
    ElMessage.success(t('ecommerce.inbound.exportSuccess'))
  } catch {
    ElMessage.error(t('ecommerce.inbound.exportFailed'))
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped lang="scss">
.inbound-po-print {
  min-height: 200px;
  overflow-x: auto;
}

.po-sheet {
  background: #fff;
  color: #111;
  font-size: 12px;
  line-height: 1.45;
}

.po-grid {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  border: 1px solid #000;
  background: #fff;

  td {
    border: 1px solid #000;
    padding: 4px 6px;
    vertical-align: middle;
    word-break: break-word;
  }
}

.po-grid__title {
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  padding: 10px 6px;
  border-bottom: 1px solid #000;
}

.po-grid__meta-left {
  vertical-align: top;
}

.po-grid__meta-right {
  text-align: right;
  vertical-align: top;
  white-space: nowrap;
}

.po-grid__ship-date {
  color: #dc2626;
  font-weight: 700;
}

.po-grid__label {
  background: #f2f2f2;
  font-weight: 600;
  white-space: nowrap;
}

.po-grid__lines-cell {
  padding: 0;
  vertical-align: top;
}

.po-lines-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: auto;
  font-size: 11px;

  th,
  td {
    border: 1px solid #000;
    padding: 4px 5px;
    vertical-align: middle;
    word-break: break-word;
  }

  th {
    background: #f2f2f2;
    font-weight: 600;
    white-space: nowrap;
    text-align: center;
  }
}

.po-lines-table__index {
  width: 36px;
  text-align: center;
}

.po-lines-table__qty,
.po-lines-table__money {
  text-align: right;
  white-space: nowrap;
}

.po-lines-table__summary {
  font-weight: 700;
  background: #fafafa;
}

.po-lines-table__summary-label {
  text-align: right;
  padding-right: 8px;
}

.po-grid__section-title {
  font-weight: 700;
  text-align: center;
  background: #f2f2f2;

  &--inline {
    text-align: left;
    background: transparent;
    padding: 0 0 6px;
    border: none;
  }
}

.po-grid__requirements {
  vertical-align: top;

  ol {
    margin: 0;
    padding-left: 18px;
  }

  li + li {
    margin-top: 4px;
  }
}

.po-grid__footer-cell {
  vertical-align: top;
  min-height: 150px;
}

.po-sheet__mark-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.po-grid__notes-list {
  margin: 0;
  padding-left: 18px;
}

.po-grid__sign p {
  margin: 0 0 6px;
}

.po-grid__editable {
  padding: 2px 4px;
  white-space: nowrap;
}

.po-grid__discount-input {
  width: 48px;
  border: 1px solid #d1d5db;
  border-radius: 2px;
  padding: 2px 4px;
  font-size: 11px;
  text-align: center;
  background: #fff;
}

.po-grid__number-input {
  width: 52px;
  border: 1px solid #d1d5db;
  border-radius: 2px;
  padding: 2px 4px;
  font-size: 11px;
  text-align: center;
  background: #fff;
}

.po-grid__input-unit,
.po-grid__discount-unit {
  margin-left: 2px;
  font-size: 10px;
}
</style>

<style lang="scss">
.inbound-po-print-dialog {
  .el-dialog__footer {
    text-align: center;
  }
}
</style>
