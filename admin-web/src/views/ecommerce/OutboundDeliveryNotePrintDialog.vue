<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('ecommerce.outbound.printDeliveryTitle')"
    width="1040px"
    append-to-body
    destroy-on-close
    align-center
    :show-close="false"
    class="outbound-dn-print-dialog"
    @update:model-value="emit('update:modelValue', $event)"
    @opened="onOpened"
  >
    <div v-loading="loading" class="outbound-dn-print">
      <section v-if="order && lineRows.length" ref="printRootRef" class="dn-sheet">
        <header class="dn-sheet__title">{{ sheetMeta.title }}</header>
        <div class="dn-sheet__head-row">
          <div class="dn-sheet__company">
            <p>{{ sheetMeta.address }}</p>
            <p>{{ sheetMeta.tel }}</p>
          </div>
          <div class="dn-sheet__meta">
            <p><strong>{{ t('ecommerce.outbound.dnOrderNo') }}：</strong>{{ order.orderNo }}</p>
            <p>{{ t('ecommerce.outbound.dnOrderDate') }}：{{ orderDateText }}</p>
            <p class="dn-sheet__ship-date">{{ t('ecommerce.outbound.dnShipDate') }}：{{ shipDateText }}</p>
            <p v-if="isConfirmed">{{ t('ecommerce.outbound.dnActualShipDate') }}：{{ actualShipText }}</p>
          </div>
        </div>

        <table class="dn-table">
          <tbody>
            <tr>
              <td class="dn-table__label">{{ t('ecommerce.inbound.poReceiverName') }}</td>
              <td colspan="2">{{ shipFrom.name || '—' }}</td>
              <td class="dn-table__label">{{ t('ecommerce.inbound.poReceiverPhone') }}</td>
              <td colspan="4">{{ shipFrom.phone || '—' }}</td>
            </tr>
            <tr>
              <td class="dn-table__label">{{ t('ecommerce.outbound.dnShipFromAddress') }}</td>
              <td colspan="7">{{ shipFrom.address || '—' }}</td>
            </tr>
            <tr>
              <td class="dn-table__label">{{ t('ecommerce.outbound.dnShipTo') }}</td>
              <td colspan="2" class="dn-table__editable">
                <input
                  v-if="!pdfExporting"
                  v-model="receiverForm.unit"
                  type="text"
                  class="dn-inline-input"
                />
                <span v-else>{{ receiverForm.unit || '—' }}</span>
              </td>
              <td class="dn-table__label">{{ t('ecommerce.outbound.dnReceiverName') }}</td>
              <td class="dn-table__editable">
                <input
                  v-if="!pdfExporting"
                  v-model="receiverForm.name"
                  type="text"
                  class="dn-inline-input"
                />
                <span v-else>{{ receiverForm.name || '—' }}</span>
              </td>
              <td class="dn-table__label">{{ t('ecommerce.outbound.dnReceiverPhone') }}</td>
              <td colspan="2" class="dn-table__editable">
                <input
                  v-if="!pdfExporting"
                  v-model="receiverForm.phone"
                  type="text"
                  class="dn-inline-input"
                />
                <span v-else>{{ receiverForm.phone || '—' }}</span>
              </td>
            </tr>
            <tr>
              <td class="dn-table__label">{{ t('ecommerce.outbound.dnReceiverAddress') }}</td>
              <td colspan="7" class="dn-table__editable">
                <input
                  v-if="!pdfExporting"
                  v-model="receiverForm.address"
                  type="text"
                  class="dn-inline-input dn-inline-input--wide"
                />
                <span v-else>{{ receiverForm.address || '—' }}</span>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="dn-sheet__section-title">{{ t('ecommerce.outbound.dnLinesTitle') }}</div>
        <table class="dn-table dn-table--lines">
          <thead>
            <tr>
              <th class="dn-col-index">{{ t('ecommerce.outbound.dnColIndex') }}</th>
              <th class="dn-col-image">{{ t('ecommerce.outbound.dnColImage') }}</th>
              <th class="dn-col-sku">{{ t('ecommerce.inventory.skuCode') }}</th>
              <th class="dn-col-spec">{{ t('ecommerce.inventory.specName') }}</th>
              <th class="dn-col-product">{{ t('ecommerce.inventory.productName') }}</th>
              <th class="dn-col-qty">{{ t('ecommerce.outbound.orderedQty') }}</th>
              <th class="dn-col-qty">{{ t('ecommerce.outbound.shippedQty') }}</th>
              <th class="dn-col-carton">{{ t('ecommerce.outbound.dnColCartons') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in lineRows" :key="row.line.skuCode">
              <td class="dn-col-index">{{ index + 1 }}</td>
              <td class="dn-col-image">
                <img v-if="row.imageUrl" :src="row.imageUrl" class="dn-line-image" alt="" />
                <span v-else class="dn-line-image-fallback">{{ t('ecommerce.outbound.noSkuImage') }}</span>
              </td>
              <td class="dn-col-sku"><strong>{{ row.line.skuCode }}</strong></td>
              <td class="dn-col-spec">{{ row.line.specName || '—' }}</td>
              <td class="dn-col-product">{{ row.line.productName || '—' }}</td>
              <td class="dn-col-qty is-qty">{{ row.line.quantity }}</td>
              <td class="dn-col-qty is-qty">{{ row.shippedQtyText }}</td>
              <td class="dn-col-carton">{{ row.cartonsText }}</td>
            </tr>
            <tr class="dn-table__summary">
              <td colspan="5" class="dn-table__summary-label">{{ t('ecommerce.outbound.dnSummary') }}</td>
              <td class="dn-col-qty is-qty">{{ summary.plannedQty }}</td>
              <td class="dn-col-qty is-qty">{{ summary.shippedQty }}</td>
              <td class="dn-col-carton">{{ summary.cartons }}</td>
            </tr>
          </tbody>
        </table>

        <div v-if="order.remark?.trim()" class="dn-sheet__remark">
          <span class="dn-sheet__remark-label">{{ t('ecommerce.outbound.remark') }}：</span>
          {{ order.remark }}
        </div>

        <div class="dn-sheet__footer-grid">
          <div class="dn-sheet__block">
            <div class="dn-sheet__block-title">{{ t('ecommerce.outbound.dnRequirements') }}</div>
            <ol>
              <li v-for="(item, index) in requirementItems" :key="index">{{ item }}</li>
            </ol>
          </div>
          <div class="dn-sheet__block">
            <div class="dn-sheet__block-title">{{ t('ecommerce.outbound.dnNotesTitle') }}</div>
            <ol>
              <li v-for="(item, index) in noteItems" :key="index">{{ item }}</li>
            </ol>
          </div>
          <div class="dn-sheet__block dn-sheet__sign">
            <p>{{ t('ecommerce.outbound.dnPreparedBy') }}：{{ sheetMeta.preparedBy }}</p>
            <p>{{ t('ecommerce.outbound.dnShipperSign') }}：________________</p>
            <p>{{ t('ecommerce.outbound.dnReceiverSign') }}：________________</p>
            <p>{{ t('ecommerce.outbound.dnSignDate') }}：________________</p>
          </div>
        </div>
      </section>
      <el-empty v-else-if="!loading" :description="t('ecommerce.outbound.linesRequired')" :image-size="64" />
    </div>
    <template #footer>
      <el-dropdown trigger="click" :disabled="!lineRows.length" @command="onExport">
        <el-button type="primary" :disabled="!lineRows.length" :loading="exporting">
          {{ t('ecommerce.outbound.exportAction') }}
          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="excel">{{ t('ecommerce.outbound.exportExcel') }}</el-dropdown-item>
            <el-dropdown-item command="pdf">{{ t('ecommerce.outbound.exportPdf') }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import type { EcOutboundOrder } from '@/api/ecommerce/outbound'
import { fetchInventorySkuOptions } from '@/api/ecommerce/inventory'
import { fetchProduct, type EcSku } from '@/api/ecommerce/product'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'
import type { EcDeliveryNoteConfig } from '@/api/ecommerce/ecSettings'
import { fetchDeliveryNoteConfig } from '@/api/ecommerce/ecSettings'
import { formatDate, formatDateTime } from '@/utils/date'
import {
  exportDeliveryNoteExcel,
  exportDeliveryNotePdf,
  type DeliveryNoteExportData,
  type DeliveryNoteExportLabels,
} from '@/utils/deliveryNoteExport'

interface DeliveryLineRow {
  line: NonNullable<EcOutboundOrder['lines']>[number]
  imageUrl?: string
  shippedQtyText: string
  cartonsText: string
}

const props = defineProps<{
  modelValue: boolean
  order: EcOutboundOrder | null
  resolveSkuImage?: (skuCode: string) => string | undefined
}>()

const emit = defineEmits<{ 'update:modelValue': [boolean] }>()

const { t } = useI18n()

const loading = ref(false)
const exporting = ref(false)
const pdfExporting = ref(false)
const printRootRef = ref<HTMLElement | null>(null)
const dnConfig = ref<EcDeliveryNoteConfig | null>(null)
const lineRows = ref<DeliveryLineRow[]>([])
const skuMetaMap = ref<Map<string, EcSku>>(new Map())

const receiverForm = reactive({
  unit: '',
  name: '',
  phone: '',
  address: '',
})

const sheetMeta = computed(() => ({
  title: dnConfig.value?.title || '唯十嘉送货单',
  address: dnConfig.value?.address || '',
  tel: dnConfig.value?.tel || '',
  preparedBy: dnConfig.value?.preparedBy || '',
}))

const shipFrom = computed(() => ({
  name: dnConfig.value?.shipFromName || '',
  phone: dnConfig.value?.shipFromPhone || '',
  address: dnConfig.value?.shipFromAddress || '',
}))

const exportCompany = computed(() => ({
  title: sheetMeta.value.title,
  address: sheetMeta.value.address,
  tel: sheetMeta.value.tel,
  preparedBy: sheetMeta.value.preparedBy,
}))

const requirementItems = computed(() => dnConfig.value?.requirementItems?.filter(Boolean) ?? [])

const noteItems = computed(() => dnConfig.value?.noteItems?.filter(Boolean) ?? [])

const isConfirmed = computed(() => props.order?.status === 'CONFIRMED')
const orderDateText = computed(() => formatDate(props.order?.orderTime))
const shipDateText = computed(() => formatDate(props.order?.expectedShipTime))
const actualShipText = computed(() => formatDateTime(props.order?.actualShipTime))

const exportLabels = computed<DeliveryNoteExportLabels>(() => ({
  orderNo: t('ecommerce.outbound.dnOrderNo'),
  orderDate: t('ecommerce.outbound.dnOrderDate'),
  shipDate: t('ecommerce.outbound.dnShipDate'),
  actualShipDate: t('ecommerce.outbound.dnActualShipDate'),
  shipFromName: t('ecommerce.inbound.poReceiverName'),
  shipFromPhone: t('ecommerce.inbound.poReceiverPhone'),
  shipFromAddress: t('ecommerce.outbound.dnShipFromAddress'),
  shipTo: t('ecommerce.outbound.dnShipTo'),
  receiverName: t('ecommerce.outbound.dnReceiverName'),
  receiverPhone: t('ecommerce.outbound.dnReceiverPhone'),
  receiverAddress: t('ecommerce.outbound.dnReceiverAddress'),
  linesTitle: t('ecommerce.outbound.dnLinesTitle'),
  colIndex: t('ecommerce.outbound.dnColIndex'),
  skuCode: t('ecommerce.inventory.skuCode'),
  specName: t('ecommerce.inventory.specName'),
  productName: t('ecommerce.inventory.productName'),
  orderedQty: t('ecommerce.outbound.orderedQty'),
  shippedQty: t('ecommerce.outbound.shippedQty'),
  cartons: t('ecommerce.outbound.dnColCartons'),
  summary: t('ecommerce.outbound.dnSummary'),
  remark: t('ecommerce.outbound.remark'),
  requirements: t('ecommerce.outbound.dnRequirements'),
  notesTitle: t('ecommerce.outbound.dnNotesTitle'),
  preparedBy: t('ecommerce.outbound.dnPreparedBy'),
  shipperSign: t('ecommerce.outbound.dnShipperSign'),
  receiverSign: t('ecommerce.outbound.dnReceiverSign'),
  signDate: t('ecommerce.outbound.dnSignDate'),
}))

const summary = computed(() => {
  const rows = lineRows.value
  let plannedQty = 0
  let shippedQty = 0
  let cartons = 0
  for (const row of rows) {
    plannedQty += row.line.quantity ?? 0
    if (row.line.shippedQuantity != null) shippedQty += row.line.shippedQuantity
    const cartonText = row.cartonsText
    const match = cartonText.match(/^(\d+)/)
    if (match) cartons += Number(match[1])
  }
  const cartonUnit = t('ecommerce.inbound.poCartonUnit')
  return {
    plannedQty,
    shippedQty: isConfirmed.value ? shippedQty : '—',
    cartons: cartons > 0 ? `${cartons}${cartonUnit}` : '—',
    shippedQtyText: isConfirmed.value ? String(shippedQty) : '—',
  }
})

function buildExportData(): DeliveryNoteExportData {
  const order = props.order
  return {
    orderNo: order?.orderNo || '—',
    orderDateText: orderDateText.value,
    shipDateText: shipDateText.value,
    actualShipText: actualShipText.value,
    showActualShip: isConfirmed.value,
    shipFromName: shipFrom.value.name || '—',
    shipFromPhone: shipFrom.value.phone || '—',
    shipFromAddress: shipFrom.value.address || '—',
    shipToUnit: receiverForm.unit.trim() || '—',
    shipToName: receiverForm.name.trim() || '—',
    shipToPhone: receiverForm.phone.trim() || '—',
    shipToAddress: receiverForm.address.trim() || '—',
    remark: order?.remark?.trim() || '',
    lines: lineRows.value.map((row) => ({
      skuCode: row.line.skuCode,
      specName: row.line.specName || '—',
      productName: row.line.productName || '—',
      orderedQty: row.line.quantity,
      shippedQtyText: row.shippedQtyText,
      cartonsText: row.cartonsText,
    })),
    summaryPlannedQty: summary.value.plannedQty,
    summaryShippedQtyText: summary.value.shippedQtyText,
    summaryCartonsText: typeof summary.value.cartons === 'string' ? summary.value.cartons : '—',
    requirementItems: requirementItems.value,
    noteItems: noteItems.value,
  }
}

function exportFilename() {
  const orderNo = props.order?.orderNo || 'delivery-note'
  return `${orderNo}-送货单`
}

function buildCartonsText(quantity: number, sku?: EcSku) {
  const units = sku?.unitsPerCarton
  if (!units || units <= 0) return '—'
  const count = Math.ceil(quantity / units)
  return `${count}${t('ecommerce.inbound.poCartonUnit')}`
}

function buildLineRow(
  line: NonNullable<EcOutboundOrder['lines']>[number],
  imageUrl?: string,
): DeliveryLineRow {
  const sku = skuMetaMap.value.get(line.skuCode)
  const qtyForCarton = isConfirmed.value && line.shippedQuantity != null
    ? line.shippedQuantity
    : line.quantity
  return {
    line,
    imageUrl,
    shippedQtyText: isConfirmed.value
      ? String(line.shippedQuantity ?? 0)
      : '—',
    cartonsText: buildCartonsText(qtyForCarton, sku),
  }
}

async function loadSheetData() {
  const order = props.order
  if (!order?.lines?.length) {
    lineRows.value = []
    dnConfig.value = null
    skuMetaMap.value = new Map()
    receiverForm.unit = ''
    receiverForm.name = ''
    receiverForm.phone = ''
    receiverForm.address = ''
    return
  }

  loading.value = true
  try {
    dnConfig.value = await fetchDeliveryNoteConfig()
    receiverForm.unit = order.customerName?.trim() || ''
    receiverForm.name = order.customerContactName?.trim() || ''
    receiverForm.phone = order.customerContactPhone?.trim() || ''
    receiverForm.address = order.customerAddress?.trim() || ''
    const skuOptions = await fetchInventorySkuOptions(order.factoryId ?? undefined)
    const productCache = new Map<number, Awaited<ReturnType<typeof fetchProduct>>>()
    const meta = new Map<string, EcSku>()

    lineRows.value = await Promise.all(
      order.lines.map(async (line) => {
        const opt = skuOptions.find((item) => item.skuCode === line.skuCode)
        if (opt?.productId) {
          let product = productCache.get(opt.productId)
          if (!product) {
            product = await fetchProduct(opt.productId)
            productCache.set(opt.productId, product)
          }
          const sku = product.skus.find((item) => item.skuCode === line.skuCode)
          if (sku) meta.set(line.skuCode, sku)
        }
        const imageUrl = props.resolveSkuImage?.(line.skuCode) ?? getEcommerceImageUrl(opt?.imageName)
        return { line, imageUrl }
      }),
    )
    skuMetaMap.value = meta
    lineRows.value = lineRows.value.map((row) => buildLineRow(row.line, row.imageUrl))
  } finally {
    loading.value = false
  }
}

async function onOpened() {
  await loadSheetData()
}

async function onExport(command: string | number | object) {
  if (!printRootRef.value || !lineRows.value.length) return
  exporting.value = true
  try {
    const filename = exportFilename()
    const data = buildExportData()
    if (command === 'excel') {
      await exportDeliveryNoteExcel(filename, exportCompany.value, exportLabels.value, data)
    } else if (command === 'pdf') {
      pdfExporting.value = true
      await nextTick()
      try {
        await exportDeliveryNotePdf(printRootRef.value, filename)
      } finally {
        pdfExporting.value = false
      }
    }
    ElMessage.success(t('ecommerce.outbound.exportSuccess'))
  } catch {
    ElMessage.error(t('ecommerce.outbound.exportFailed'))
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped lang="scss">
.outbound-dn-print {
  min-height: 200px;
}

.dn-sheet {
  border: 1px solid #111;
  padding: 12px 14px 16px;
  background: #fff;
  color: #111;
  font-size: 12px;
  line-height: 1.45;
}

.dn-sheet__title {
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 10px;
  letter-spacing: 2px;
}

.dn-sheet__head-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 8px;
}

.dn-sheet__company p {
  margin: 0 0 4px;
}

.dn-sheet__meta {
  text-align: right;
  white-space: nowrap;

  p {
    margin: 0 0 4px;
  }
}

.dn-sheet__ship-date {
  color: #dc2626;
  font-weight: 700;
}

.dn-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;

  th,
  td {
    border: 1px solid #111;
    padding: 5px 6px;
    vertical-align: middle;
    word-break: break-word;
  }

  th {
    background: #f1f5f9;
    font-weight: 700;
    text-align: center;
  }
}

.dn-table__label {
  background: #f8fafc;
  font-weight: 600;
  white-space: nowrap;
  width: 72px;
}

.dn-table__editable {
  padding: 2px 4px;
}

.dn-inline-input {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 2px;
  padding: 3px 6px;
  font-size: 12px;
  background: #fff;
  box-sizing: border-box;

  &--wide {
    min-width: 100%;
  }
}

.dn-sheet__section-title {
  margin-top: 10px;
  margin-bottom: 0;
  padding: 6px 8px;
  border: 1px solid #111;
  border-bottom: none;
  background: #eff6ff;
  font-weight: 700;
  text-align: center;
}

.dn-table--lines {
  margin-top: 0;
}

.dn-col-index {
  width: 36px;
  text-align: center;
}

.dn-col-image {
  width: 56px;
  text-align: center;
}

.dn-col-sku {
  width: 100px;
}

.dn-col-spec {
  width: 88px;
}

.dn-col-qty {
  width: 72px;
  text-align: center;

  &.is-qty {
    font-size: 15px;
    font-weight: 700;
    color: #ea580c;
  }
}

.dn-col-carton {
  width: 64px;
  text-align: center;
}

.dn-line-image {
  width: 44px;
  height: 44px;
  object-fit: contain;
  display: block;
  margin: 0 auto;
}

.dn-line-image-fallback {
  display: block;
  font-size: 10px;
  color: #9ca3af;
  text-align: center;
  line-height: 1.2;
}

.dn-table__summary {
  background: #fff7ed;

  .dn-table__summary-label {
    text-align: right;
    font-weight: 700;
    padding-right: 10px;
  }
}

.dn-sheet__remark {
  margin-top: 8px;
  padding: 8px 10px;
  border: 1px solid #111;
  background: #fefce8;
}

.dn-sheet__remark-label {
  font-weight: 700;
}

.dn-sheet__footer-grid {
  display: grid;
  grid-template-columns: 1.2fr 1.5fr 1fr;
  gap: 0;
  margin-top: 10px;
  border: 1px solid #111;
}

.dn-sheet__block {
  padding: 8px 10px;
  border-right: 1px solid #111;
  min-height: 140px;

  &:last-child {
    border-right: none;
  }

  ol {
    margin: 0;
    padding-left: 18px;
  }

  li + li {
    margin-top: 4px;
  }
}

.dn-sheet__block-title {
  font-weight: 700;
  margin-bottom: 6px;
}

.dn-sheet__sign p {
  margin: 0 0 12px;
}
</style>

<style lang="scss">
.outbound-dn-print-dialog {
  .el-dialog__footer {
    text-align: center;
  }
}
</style>
