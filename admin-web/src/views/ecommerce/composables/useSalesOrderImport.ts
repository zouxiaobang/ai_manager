import { computed, ref, type ComputedRef, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadInstance } from 'element-plus'
import {
  commitSalesOrderImport,
  fetchSalesOrderImportPreview,
  reparseSalesOrderImport,
  replaceSalesOrderImportFile,
  uploadSalesOrderImport,
  type EcSalesOrderImportPreview,
  type EcSalesOrderImportRow,
  type ImportRowPatchItem,
  type ShopImportStatus,
} from '@/api/ecommerce/salesOrder'
import type { EcShop } from '@/api/ecommerce/shop'
import {
  BIZ_SALES_ORDER,
  createImportProfile,
  defaultPlatformProfileName,
  fetchImportFields,
  fetchImportProfiles,
  type SysImportProfile,
} from '@/api/sys/import'
import { filterImportFields } from '@/constants/importFieldKeys'
import { normalizeLineStatus, type ImportLineStatus } from '@/constants/importStatusMapping'
import { detectSpreadsheetColumns, type ParsedSpreadsheet } from '@/utils/spreadsheetParse'
import { buildColumnMappingForUpload } from '@/utils/importColumnMapping'
import { formatImportFileSize, parseManualCostNumber, sanitizeManualCostInput } from '@/utils/salesOrderView'
import { resolveShopIconMeta } from '@/utils/shopVisual'
import { resolvePlatformIconMeta } from '@/utils/platformVisual'
import { useEcSettingsStore } from '@/stores/ecSettings'

/** 店铺导入卡片视图（与模板 shopImportCards 渲染契约） */
export interface ShopImportCardView {
  shopId: number
  shopName: string
  platformName?: string
  platformCode?: number | null
  shopAvatarUrl?: string | null
  platformAvatarUrl?: string | null
  status: ShopImportStatus
  orderCount: number
  statusText: string
  dateLabel?: string
  tone: 'green' | 'gray' | 'orange'
  actionLabel?: string
  actionType?: 'primary' | 'warning' | 'default'
  pendingBatchId?: number | null
}

/** useSalesOrderImport 依赖注入：跨块共享的组件 ref / 回调 */
export interface SalesOrderImportDeps {
  /** 店铺下拉选项 */
  shopOptions: Ref<EcShop[]>
  /** 当前店铺筛选（openImport 缺省取第一个或当前筛选） */
  shopFilter: Ref<number | undefined>
  /** 店铺 id → 店铺 映射（店铺卡片图标解析用，与组件详情图标共用） */
  shopOptionMap: ComputedRef<Map<number, EcShop>>
  /** 当前结算月份（上传导入文件时随订单写入） */
  orderMonth: Ref<string>
  /** 提交成功后刷新月度概览与订单列表（组件提供） */
  refreshAll: () => Promise<void>
}

/**
 * 销售订单导入状态机
 *
 * <p>从 {@code SalesOrderPanel.vue} 提取：导入文件上传/替换/重新解析/复核/提交、店铺卡片图标、
 * 导入字段配置（profile）管理。依赖通过参数注入，i18n 与 ecSettings store 内部获取。</p>
 */
export function useSalesOrderImport({ shopOptions, shopFilter, shopOptionMap, orderMonth, refreshAll }: SalesOrderImportDeps) {
  const { t } = useI18n()
  const ecSettings = useEcSettingsStore()

  // ========== 导入对话框状态 ==========
  const importVisible = ref(false)
  const importShopId = ref<number | undefined>()
  const importPreview = ref<EcSalesOrderImportPreview | null>(null)
  const importing = ref(false)
  const uploading = ref(false)
  const mappingVisible = ref(false)
  const importFile = ref<File | null>(null)
  const parsedSpreadsheet = ref<ParsedSpreadsheet | null>(null)
  const importProfileId = ref<number | null>(null)
  const importProfileName = ref<string | null>(null)
  const importUploadRef = ref<UploadInstance>()
  const importReplaceInputRef = ref<HTMLInputElement>()
  const statusMappingExpanded = ref(false)
  const detectedColumnsExpanded = ref(false)
  const reparsing = ref(false)
  const importFileParsing = ref(false)
  const manualCostDrafts = ref<Record<number, string>>({})
  const pendingResumeBatchId = ref<number | null>(null)
  const importResumed = ref(false)
  const importResumedFileName = ref<string | null>(null)
  const importResuming = ref(false)

  const importShop = computed(() => shopOptions.value.find((s) => s.id === importShopId.value))

  function getShopCardShopIcon(shop: ShopImportCardView) {
    const opt = shopOptionMap.value.get(shop.shopId)
    return resolveShopIconMeta(
      shop.shopName,
      shop.platformName ?? opt?.platformName,
      shop.platformCode ?? opt?.platformCode,
      shop.shopAvatarUrl ?? opt?.avatarUrl,
    )
  }

  function getShopCardPlatformIcon(shop: ShopImportCardView) {
    const opt = shopOptionMap.value.get(shop.shopId)
    return resolvePlatformIconMeta(
      shop.platformName ?? opt?.platformName,
      shop.platformCode ?? opt?.platformCode,
      shop.platformAvatarUrl,
    )
  }

  const importPlatformId = computed(() => importShop.value?.platformId)
  const importPlatformName = computed(() => importShop.value?.platformName ?? '')

  const canFinishUpload = computed(
    () => !!importFile.value && !!importShopId.value && !!parsedSpreadsheet.value && !uploading.value,
  )

  const importExcludeStatuses = ref<ImportLineStatus[]>([])
  const importReviewSearch = ref('')
  const importReviewBatchCost = ref('')

  function isImportRowExcludedByStatus(row: EcSalesOrderImportRow): boolean {
    if (!row.lineStatus) return false
    return importExcludeStatuses.value.includes(normalizeLineStatus(row.lineStatus))
  }

  const canCommitImport = computed(() => {
    const preview = importPreview.value
    if (!preview?.batchId) return false
    return preview.rows.some((row) => isImportRowImportable(row))
  })

  function isImportRowImportable(row: EcSalesOrderImportPreview['rows'][number]) {
    if (row.parseStatus !== 'OK') return false
    if (row.statusMatchStatus === 'UNMATCHED' && !row.lineStatus) return false
    if (isImportRowExcludedByStatus(row) && row.matchStatus === 'UNMATCHED') return false
    if (row.matchStatus === 'UNMATCHED') {
      const cost = resolveRowManualCost(row)
      return cost != null && cost !== 0
    }
    return true
  }

  const importReviewRows = computed(() => {
    const preview = importPreview.value
    if (!preview?.rows) return []
    return preview.rows.filter(
      (row) =>
        row.parseStatus === 'OK'
        && (row.matchStatus === 'UNMATCHED' || row.statusMatchStatus === 'UNMATCHED')
        && !isImportRowExcludedByStatus(row),
    )
  })

  const importReviewDisplayRows = computed(() => {
    const query = importReviewSearch.value.trim().toLowerCase()
    if (!query) return importReviewRows.value
    return importReviewRows.value.filter((row) => {
      const linkName = (row.linkName ?? '').toLowerCase()
      const skuSpecName = (row.skuSpecName ?? '').toLowerCase()
      return linkName.includes(query) || skuSpecName.includes(query)
    })
  })

  const importReviewDisplayUnmatchedCount = computed(() =>
    importReviewDisplayRows.value.filter((row) => row.matchStatus === 'UNMATCHED').length,
  )

  const importPendingCostCount = computed(() =>
    importReviewRows.value.filter((row) => {
      if (row.matchStatus !== 'UNMATCHED') return false
      const cost = resolveRowManualCost(row)
      return cost == null || cost === 0
    }).length,
  )

  type ImportFileCardType = 'CSV' | 'XLSX' | 'XLS'

  const showImportFileCard = computed(
    () => !!(importPreview.value?.batchId || importFile.value || importResumed.value),
  )

  const showImportMappingEntry = computed(
    () => showImportFileCard.value && !importFileParsing.value && !reparsing.value,
  )

  const importReviewBannerVisible = computed(
    () => importResumed.value && !!importPreview.value?.batchNo,
  )

  const importFileCardName = computed(
    () => importResumedFileName.value ?? importFile.value?.name ?? importPreview.value?.fileName ?? '—',
  )

  const importFileCardUploaded = computed(() => !!importPreview.value?.batchId)

  const canReparseImport = computed(() => {
    const preview = importPreview.value
    if (!preview?.batchId) return false
    return preview.importFileReadable !== false
  })

  const importFileCardType = computed((): ImportFileCardType => {
    if (parsedSpreadsheet.value?.fileType) return parsedSpreadsheet.value.fileType
    const name = importFileCardName.value.toLowerCase()
    if (name.endsWith('.csv') || name.endsWith('.txt')) return 'CSV'
    if (name.endsWith('.xls') && !name.endsWith('.xlsx')) return 'XLS'
    return 'XLSX'
  })

  const importFileCardMeta = computed(() => {
    const parts: string[] = []
    const totalRows = importPreview.value?.totalRows
    const colCount = importDetectedColumnCount.value
    if (totalRows != null) {
      parts.push(t('ecommerce.salesOrder.importFileMetaRows', { count: totalRows }))
    } else if (colCount != null) {
      parts.push(t('ecommerce.salesOrder.importFileMetaCols', { count: colCount }))
    }
    const size = importPreview.value?.fileSize ?? importFile.value?.size
    if (size != null && size > 0) {
      parts.push(formatImportFileSize(size))
    }
    const batch = importPreview.value?.batchNo
    if (batch) {
      parts.push(batch)
    }
    return parts.join(' · ')
  })

  const importDetectedColumnCount = computed(() => {
    if (parsedSpreadsheet.value?.columns.length) {
      return parsedSpreadsheet.value.columns.length
    }
    return importPreview.value?.detectedColumnCount ?? null
  })

  const importDetectedColumns = computed(() => {
    if (parsedSpreadsheet.value?.columns.length) {
      return parsedSpreadsheet.value.columns
    }
    return importPreview.value?.detectedColumns ?? []
  })

  function resolveRowManualCost(row: EcSalesOrderImportPreview['rows'][number]): number | undefined {
    if (row.id != null && manualCostDrafts.value[row.id] !== undefined) {
      const fromDraft = parseManualCostNumber(manualCostDrafts.value[row.id])
      if (fromDraft != null) return fromDraft
    }
    return row.manualCostPrice ?? undefined
  }

  function manualCostInputValue(row: EcSalesOrderImportPreview['rows'][number]) {
    if (row.id != null && manualCostDrafts.value[row.id] !== undefined) {
      return manualCostDrafts.value[row.id]
    }
    if (row.manualCostPrice == null) return ''
    return String(row.manualCostPrice)
  }

  function onManualCostInput(row: EcSalesOrderImportPreview['rows'][number], raw: string) {
    const sanitized = sanitizeManualCostInput(raw)
    if (row.id != null) {
      manualCostDrafts.value[row.id] = sanitized
    }
    row.manualCostPrice = parseManualCostNumber(sanitized) ?? undefined
  }

  function onManualCostBlur(row: EcSalesOrderImportPreview['rows'][number]) {
    if (row.id == null) return
    const parsed = row.manualCostPrice
    if (parsed != null && Number.isFinite(parsed)) {
      const rounded = Math.round(parsed * 100) / 100
      row.manualCostPrice = rounded
      manualCostDrafts.value[row.id] = String(rounded)
      return
    }
    delete manualCostDrafts.value[row.id]
    row.manualCostPrice = undefined
  }

  function applyImportReviewBatchCost() {
    const sanitized = sanitizeManualCostInput(importReviewBatchCost.value)
    importReviewBatchCost.value = sanitized
    const cost = parseManualCostNumber(sanitized)
    if (cost == null || cost === 0) {
      ElMessage.warning(t('ecommerce.salesOrder.importReviewBatchCostInvalid'))
      return
    }
    const rounded = Math.round(cost * 100) / 100
    let filled = 0
    for (const row of importReviewRows.value) {
      if (row.matchStatus !== 'UNMATCHED' || row.id == null) continue
      row.manualCostPrice = rounded
      manualCostDrafts.value[row.id] = String(rounded)
      filled += 1
    }
    if (!filled) {
      ElMessage.info(t('ecommerce.salesOrder.importReviewBatchFillNone'))
      return
    }
    ElMessage.success(t('ecommerce.salesOrder.importReviewBatchFillSuccess', { count: filled }))
  }

  const importLineStatusOptions = computed(() => {
    const values: ImportLineStatus[] = [
      'PAID', 'SHIPPED', 'COMPLETED', 'CANCELLED', 'PARTIAL_REFUND', 'REFUNDED', 'RETURNED',
    ]
    return values.map((value) => ({ value, label: importLineStatusLabel(value) }))
  })

  function importLineStatusLabel(status?: string | null) {
    if (!status) return '—'
    const key = normalizeLineStatus(status)
    const map: Record<ImportLineStatus, string> = {
      PAID: t('ecommerce.salesOrder.importLineStatusPaid'),
      SHIPPED: t('ecommerce.salesOrder.importLineStatusShipped'),
      COMPLETED: t('ecommerce.salesOrder.importLineStatusCompleted'),
      CANCELLED: t('ecommerce.salesOrder.importLineStatusCancelled'),
      PARTIAL_REFUND: t('ecommerce.salesOrder.importLineStatusPartialRefund'),
      REFUNDED: t('ecommerce.salesOrder.importLineStatusRefunded'),
      RETURNED: t('ecommerce.salesOrder.importLineStatusReturned'),
    }
    return map[key]
  }

  // ========== 打开 / 恢复批次 ==========

  function openImport(shopId?: number, batchId?: number) {
    resetImport()
    importShopId.value = shopId ?? shopFilter.value ?? shopOptions.value[0]?.id
    pendingResumeBatchId.value = batchId ?? null
    importVisible.value = true
  }

  async function resumePendingBatch(batchId: number) {
    importResuming.value = true
    try {
      const preview = await fetchSalesOrderImportPreview(batchId)
      importPreview.value = preview
      if (preview.shopId) {
        importShopId.value = preview.shopId
      }
      if (preview.profileId) {
        importProfileId.value = preview.profileId
        importProfileName.value = null
      } else {
        await loadImportProfileForPlatform()
      }
      importResumed.value = true
      importResumedFileName.value = preview.fileName ?? preview.batchNo
    } catch {
      ElMessage.error(t('ecommerce.salesOrder.resumeBatchFailed'))
    } finally {
      importResuming.value = false
    }
  }

  // ========== 导入生命周期 ==========

  function resetImport() {
    importPreview.value = null
    importFile.value = null
    parsedSpreadsheet.value = null
    importProfileId.value = null
    importProfileName.value = null
    statusMappingExpanded.value = false
    detectedColumnsExpanded.value = false
    reparsing.value = false
    importFileParsing.value = false
    manualCostDrafts.value = {}
    importExcludeStatuses.value = []
    importReviewSearch.value = ''
    importReviewBatchCost.value = ''
    pendingResumeBatchId.value = null
    importResumed.value = false
    importResumedFileName.value = null
    importUploadRef.value?.clearFiles()
  }

  function onImportShopChange() {
    if (importResumed.value) return
    importProfileId.value = null
    importProfileName.value = null
    importPreview.value = null
    void loadImportProfileForPlatform()
  }

  async function onImportOpen() {
    if (pendingResumeBatchId.value) {
      await resumePendingBatch(pendingResumeBatchId.value)
      pendingResumeBatchId.value = null
      return
    }
    if (importShopId.value) {
      await loadImportProfileForPlatform()
    }
  }

  async function loadImportProfileForPlatform() {
    if (!importPlatformId.value) {
      importProfileId.value = null
      importProfileName.value = null
      return
    }
    const profiles = await fetchImportProfiles(BIZ_SALES_ORDER, importPlatformId.value)
    const preferredName = defaultPlatformProfileName(importPlatformName.value || '')
    const preferred = profiles.find((p) => p.name === preferredName) ?? profiles[0]
    if (preferred?.id) {
      importProfileId.value = preferred.id
      importProfileName.value = preferred.name ?? null
    } else {
      importProfileId.value = null
      importProfileName.value = null
    }
  }

  function openMapping() {
    if (!importPlatformId.value) {
      ElMessage.warning(t('ecommerce.salesOrder.importShopRequired'))
      return
    }
    mappingVisible.value = true
  }

  async function onImportUploadChange(uploadFile: UploadFile) {
    const file = uploadFile.raw
    if (!file) return
    await handleImportFileSelected(file)
  }

  async function handleImportFileSelected(file: File) {
    if (!importShopId.value) {
      ElMessage.warning(t('ecommerce.salesOrder.importShopRequired'))
      importUploadRef.value?.clearFiles()
      return
    }
    importFile.value = file
    importResumedFileName.value = file.name
    importPreview.value = null
    importFileParsing.value = true
    detectedColumnsExpanded.value = false
    try {
      await ecSettings.ensureLoaded()
      parsedSpreadsheet.value = await detectSpreadsheetColumns(file, ecSettings.orderImport.headerRow)
      if (parsedSpreadsheet.value) {
        parsedSpreadsheet.value.dataStartRow = ecSettings.orderImport.dataStartRow
      }
      if (!parsedSpreadsheet.value.columns.length) {
        ElMessage.warning(t('ecommerce.salesOrder.importEmpty'))
        importFile.value = null
        importResumedFileName.value = null
        parsedSpreadsheet.value = null
        importUploadRef.value?.clearFiles()
        return
      }
    } catch {
      ElMessage.error(t('ecommerce.salesOrder.parseFailed'))
      importFile.value = null
      importResumedFileName.value = null
      parsedSpreadsheet.value = null
      importUploadRef.value?.clearFiles()
    } finally {
      importFileParsing.value = false
    }
  }

  function onMappingSaved(profile: SysImportProfile) {
    importProfileId.value = profile.id ?? null
    importProfileName.value = profile.name ?? defaultPlatformProfileName(importPlatformName.value || '')
    if (importPreview.value?.batchId) {
      void onReparseImport()
    }
  }

  function onStatusMappingSaved(profile: SysImportProfile) {
    importProfileId.value = profile.id ?? null
    importProfileName.value = profile.name ?? importProfileName.value
    if (importPreview.value?.batchId) {
      void onReparseImport()
    }
  }

  async function ensureImportProfile(): Promise<number> {
    if (importProfileId.value) {
      return importProfileId.value
    }
    await loadImportProfileForPlatform()
    if (importProfileId.value) {
      return importProfileId.value
    }
    if (!importPlatformId.value || !parsedSpreadsheet.value) {
      throw new Error('missing import context')
    }
    await ecSettings.ensureLoaded()
    const fields = filterImportFields(await fetchImportFields(BIZ_SALES_ORDER))
    const columnMapping = buildColumnMappingForUpload(
      fields,
      parsedSpreadsheet.value.columns,
      importPlatformName.value,
    )
    if (!columnMapping.link_name?.trim()) {
      ElMessage.warning(t('ecommerce.salesOrder.importMappingRequired'))
      throw new Error('link_name not mapped')
    }
    const profile = await createImportProfile({
      name: defaultPlatformProfileName(importPlatformName.value || ''),
      bizType: BIZ_SALES_ORDER,
      platformId: importPlatformId.value,
      fileType: parsedSpreadsheet.value.fileType,
      headerRow: parsedSpreadsheet.value.headerRow,
      dataStartRow: parsedSpreadsheet.value.dataStartRow,
      columnMapping,
      valueMapping: { ...ecSettings.statusMappingForImport },
    })
    importProfileId.value = profile.id ?? null
    importProfileName.value = profile.name ?? null
    return profile.id!
  }

  async function onFinishUpload() {
    if (!importFile.value || !importShopId.value || !parsedSpreadsheet.value) return
    uploading.value = true
    try {
      const profileId = await ensureImportProfile()
      importPreview.value = await uploadSalesOrderImport(
        importFile.value,
        importShopId.value,
        profileId,
        orderMonth.value,
      )
      importResumedFileName.value = importPreview.value.fileName ?? importFile.value.name
      ElMessage.success(t('ecommerce.salesOrder.uploadSuccess'))
    } finally {
      uploading.value = false
    }
  }

  function triggerImportFileReplace() {
    if (!importShopId.value) {
      ElMessage.warning(t('ecommerce.salesOrder.importShopRequired'))
      return
    }
    importReplaceInputRef.value?.click()
  }

  async function onImportReplaceFileChange(event: Event) {
    const input = event.target as HTMLInputElement
    const file = input.files?.[0]
    input.value = ''
    if (!file) return
    if (importPreview.value?.batchId) {
      reparsing.value = true
      importFile.value = file
      importResumedFileName.value = file.name
      try {
        importPreview.value = await replaceSalesOrderImportFile(importPreview.value.batchId, file)
        importResumedFileName.value = importPreview.value.fileName ?? file.name
        await ecSettings.ensureLoaded()
        parsedSpreadsheet.value = await detectSpreadsheetColumns(file, ecSettings.orderImport.headerRow)
        if (parsedSpreadsheet.value) {
          parsedSpreadsheet.value.dataStartRow = ecSettings.orderImport.dataStartRow
        }
        ElMessage.success(t('ecommerce.salesOrder.replaceFileSuccess'))
      } catch {
        ElMessage.error(t('ecommerce.salesOrder.replaceFileFailed'))
      } finally {
        reparsing.value = false
      }
      return
    }
    await handleImportFileSelected(file)
  }

  async function onReparseImport() {
    if (!importPreview.value?.batchId) return
    if (!canReparseImport.value) {
      ElMessage.warning(t('ecommerce.salesOrder.importFileMissingHint'))
      return
    }
    reparsing.value = true
    try {
      importPreview.value = await reparseSalesOrderImport(importPreview.value.batchId)
      ElMessage.success(t('ecommerce.salesOrder.reparseImportSuccess'))
    } catch {
      ElMessage.error(t('ecommerce.salesOrder.reparseImportFailed'))
    } finally {
      reparsing.value = false
    }
  }

  async function onCommitImport() {
    if (!importPreview.value?.batchId) return
    const reviewRows = importReviewRows.value
    const missingCost = reviewRows.filter((row) => {
      if (row.matchStatus !== 'UNMATCHED') return false
      const cost = resolveRowManualCost(row)
      return cost == null || cost === 0
    })
    if (missingCost.length) {
      ElMessage.warning(t('ecommerce.salesOrder.importUnmatchedCostRequired'))
      return
    }
    const missingStatus = reviewRows.filter(
      (row) => row.statusMatchStatus === 'UNMATCHED' && !row.lineStatus,
    )
    if (missingStatus.length) {
      ElMessage.warning(t('ecommerce.salesOrder.importUnmatchedStatusRequired'))
      return
    }
    importing.value = true
    try {
      const patches: ImportRowPatchItem[] = []
      for (const row of importPreview.value.rows) {
        if (row.id == null || row.parseStatus !== 'OK') continue
        if (isImportRowExcludedByStatus(row)) {
          const patch: ImportRowPatchItem = { rowId: row.id }
          if (row.matchStatus === 'UNMATCHED') {
            patch.manualCostPrice = 0
          }
          if (row.statusMatchStatus === 'UNMATCHED' && row.lineStatus) {
            patch.lineStatus = row.lineStatus
          }
          if (patch.manualCostPrice !== undefined || patch.lineStatus) {
            patches.push(patch)
          }
          continue
        }
        const needsReview = row.matchStatus === 'UNMATCHED' || row.statusMatchStatus === 'UNMATCHED'
        if (!needsReview) continue
        patches.push({
          rowId: row.id,
          ...(row.matchStatus === 'UNMATCHED'
            ? { manualCostPrice: resolveRowManualCost(row) ?? null }
            : {}),
          ...(row.statusMatchStatus === 'UNMATCHED'
            ? { lineStatus: row.lineStatus ?? null }
            : {}),
        })
      }
      await commitSalesOrderImport(importPreview.value.batchId, {
        ...(patches.length ? { items: patches } : {}),
        ...(importExcludeStatuses.value.length
          ? { excludedLineStatuses: [...importExcludeStatuses.value] }
          : {}),
      })
      ElMessage.success(t('ecommerce.salesOrder.importSuccess'))
      importVisible.value = false
      resetImport()
      await refreshAll()
    } catch {
      ElMessage.error(t('ecommerce.salesOrder.importCommitFailed'))
    } finally {
      importing.value = false
    }
  }

  return {
    importVisible,
    importShopId,
    importPreview,
    importing,
    uploading,
    mappingVisible,
    importFile,
    parsedSpreadsheet,
    importProfileId,
    importProfileName,
    importUploadRef,
    importReplaceInputRef,
    statusMappingExpanded,
    detectedColumnsExpanded,
    reparsing,
    importFileParsing,
    manualCostDrafts,
    pendingResumeBatchId,
    importResumed,
    importResumedFileName,
    importResuming,
    importShop,
    importPlatformId,
    importPlatformName,
    getShopCardShopIcon,
    getShopCardPlatformIcon,
    canFinishUpload,
    importExcludeStatuses,
    importReviewSearch,
    importReviewBatchCost,
    isImportRowExcludedByStatus,
    canCommitImport,
    isImportRowImportable,
    importReviewRows,
    importReviewDisplayRows,
    importReviewDisplayUnmatchedCount,
    importPendingCostCount,
    showImportFileCard,
    showImportMappingEntry,
    importReviewBannerVisible,
    importFileCardName,
    importFileCardUploaded,
    canReparseImport,
    importFileCardType,
    importFileCardMeta,
    importDetectedColumnCount,
    importDetectedColumns,
    resolveRowManualCost,
    manualCostInputValue,
    onManualCostInput,
    onManualCostBlur,
    applyImportReviewBatchCost,
    importLineStatusOptions,
    importLineStatusLabel,
    openImport,
    resumePendingBatch,
    resetImport,
    onImportShopChange,
    onImportOpen,
    loadImportProfileForPlatform,
    openMapping,
    onImportUploadChange,
    handleImportFileSelected,
    onMappingSaved,
    onStatusMappingSaved,
    ensureImportProfile,
    onFinishUpload,
    triggerImportFileReplace,
    onImportReplaceFileChange,
    onReparseImport,
    onCommitImport,
  }
}
