import { beforeEach, describe, expect, it, vi } from 'vitest'
import { computed, ref } from 'vue'
import { useSalesOrderImport } from '../useSalesOrderImport'
import {
  commitSalesOrderImport,
  fetchSalesOrderImportPreview,
  reparseSalesOrderImport,
  replaceSalesOrderImportFile,
  uploadSalesOrderImport,
  type EcSalesOrderImportPreview,
  type EcSalesOrderImportRow,
} from '@/api/ecommerce/salesOrder'
import type { EcShop } from '@/api/ecommerce/shop'
import { createImportProfile, defaultPlatformProfileName, fetchImportFields, fetchImportProfiles } from '@/api/sys/import'
import { detectSpreadsheetColumns } from '@/utils/spreadsheetParse'
import { ElMessage } from 'element-plus'

const storeMock = {
  ensureLoaded: vi.fn().mockResolvedValue(undefined),
  orderImport: { headerRow: 1, dataStartRow: 2 },
  statusMappingForImport: {},
}

vi.mock('@/stores/ecSettings', () => ({
  useEcSettingsStore: () => storeMock,
}))

vi.mock('@/api/ecommerce/salesOrder', () => ({
  commitSalesOrderImport: vi.fn(),
  fetchSalesOrderImportPreview: vi.fn(),
  reparseSalesOrderImport: vi.fn(),
  replaceSalesOrderImportFile: vi.fn(),
  uploadSalesOrderImport: vi.fn(),
}))

vi.mock('@/api/sys/import', () => ({
  BIZ_SALES_ORDER: 'SALES_ORDER',
  createImportProfile: vi.fn(),
  defaultPlatformProfileName: vi.fn((p: string) => p || '默认'),
  fetchImportFields: vi.fn(),
  fetchImportProfiles: vi.fn(),
}))

vi.mock('@/utils/spreadsheetParse', () => ({
  detectSpreadsheetColumns: vi.fn(),
}))

vi.mock('@/constants/importFieldKeys', () => ({
  filterImportFields: vi.fn((f: unknown) => f),
}))

vi.mock('@/utils/importColumnMapping', () => ({
  buildColumnMappingForUpload: vi.fn(() => ({ link_name: '链接' })),
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() },
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (k: string) => k }),
}))

const commitMock = vi.mocked(commitSalesOrderImport)
const previewMock = vi.mocked(fetchSalesOrderImportPreview)
const reparseMock = vi.mocked(reparseSalesOrderImport)
const replaceMock = vi.mocked(replaceSalesOrderImportFile)
const uploadMock = vi.mocked(uploadSalesOrderImport)
const profilesMock = vi.mocked(fetchImportProfiles)
const fieldsMock = vi.mocked(fetchImportFields)
const createProfileMock = vi.mocked(createImportProfile)
const defaultProfileNameMock = vi.mocked(defaultPlatformProfileName)
const detectMock = vi.mocked(detectSpreadsheetColumns)
const successMock = vi.mocked(ElMessage.success)
const errorMock = vi.mocked(ElMessage.error)
const warningMock = vi.mocked(ElMessage.warning)

function makeShop(id: number, overrides: Partial<EcShop> = {}): EcShop {
  return { id, name: `店铺${id}`, platformId: 10, platformName: '淘宝', platformCode: 1, status: 'ENABLED', ...overrides }
}

function makeRow(id: number, overrides: Partial<EcSalesOrderImportRow> = {}): EcSalesOrderImportRow {
  return {
    id,
    rowNo: id,
    parseStatus: 'OK',
    matchStatus: 'UNMATCHED',
    statusMatchStatus: 'UNMATCHED',
    lineStatus: null,
    linkName: `链接${id}`,
    skuSpecName: `规格${id}`,
    manualCostPrice: undefined,
    ...overrides,
  }
}

function makePreview(overrides: Partial<EcSalesOrderImportPreview> = {}): EcSalesOrderImportPreview {
  return {
    batchId: 7,
    batchNo: 'B-001',
    fileName: 'orders.xlsx',
    fileSize: 2048,
    detectedColumnCount: 3,
    detectedColumns: ['链接', '规格'],
    totalRows: 1,
    matchedRows: 0,
    unmatchedRows: 1,
    statusUnmatchedRows: 0,
    errorRows: 0,
    rows: [makeRow(1)],
    ...overrides,
  }
}

function makeDeps(shopList: EcShop[] = [makeShop(1)]) {
  const shopOptions = ref<EcShop[]>(shopList)
  const shopFilter = ref<number | undefined>()
  const shopOptionMap = computed(() => {
    const map = new Map<number, EcShop>()
    for (const s of shopOptions.value) map.set(s.id, s)
    return map
  })
  const orderMonth = ref('2026-08')
  const refreshAll = vi.fn().mockResolvedValue(undefined)
  const api = useSalesOrderImport({ shopOptions, shopFilter, shopOptionMap, orderMonth, refreshAll })
  return { api, shopOptions, shopFilter, orderMonth, refreshAll }
}

beforeEach(() => {
  vi.clearAllMocks()
  defaultProfileNameMock.mockImplementation((p: string) => p || '默认')
  profilesMock.mockResolvedValue([{ id: 5, name: '淘宝' }] as never)
  fieldsMock.mockResolvedValue([{ key: 'link_name', label: '链接' }] as never)
  createProfileMock.mockResolvedValue({ id: 9, name: '淘宝' } as never)
  detectMock.mockResolvedValue({
    fileType: 'xlsx',
    headerRow: 1,
    dataStartRow: 2,
    columns: [{ index: 0, title: '链接' }],
  } as never)
  uploadMock.mockResolvedValue(makePreview() as never)
  previewMock.mockResolvedValue(makePreview() as never)
  replaceMock.mockResolvedValue(makePreview() as never)
  reparseMock.mockResolvedValue(makePreview() as never)
  commitMock.mockResolvedValue(undefined as never)
})

describe('useSalesOrderImport', () => {
  it('openImport 重置并设置 shop/batch 后打开对话框', () => {
    const { api } = makeDeps([makeShop(1), makeShop(2)])
    api.importPreview.value = makePreview()

    api.openImport(2, 99)

    expect(api.importVisible.value).toBe(true)
    expect(api.importShopId.value).toBe(2)
    expect(api.pendingResumeBatchId.value).toBe(99)
    // resetImport 已清空旧预览
    expect(api.importPreview.value).toBeNull()
  })

  it('openImport 缺省取当前筛选或第一个店铺', () => {
    const { api, shopFilter } = makeDeps([makeShop(1), makeShop(2)])
    shopFilter.value = 2
    api.openImport()
    expect(api.importShopId.value).toBe(2)
  })

  it('importShop 依据 shopOptions 解析当前店铺', () => {
    const { api } = makeDeps([makeShop(1, { platformId: 11, platformName: '拼多多' })])
    api.importShopId.value = 1
    expect(api.importShop.value?.platformId).toBe(11)
    expect(api.importPlatformName.value).toBe('拼多多')
  })

  it('getShopCardShopIcon 解析店铺图标元信息', () => {
    const { api } = makeDeps([makeShop(1, { avatarUrl: '/a.png' })])
    const icon = api.getShopCardShopIcon({ shopId: 1, shopName: '店铺1', status: 'NOT_IMPORTED', orderCount: 0, statusText: 'x', tone: 'green' })
    expect(icon.src).toContain('a.png')
  })

  it('canFinishUpload 需要文件/店铺/解析结果', () => {
    const { api } = makeDeps()
    expect(api.canFinishUpload.value).toBe(false)
    api.importFile.value = new File(['x'], 'a.xlsx')
    api.importShopId.value = 1
    api.parsedSpreadsheet.value = { fileName: 'a.xlsx', fileType: 'XLSX', headerRow: 1, dataStartRow: 2, columns: ['链接'] }
    expect(api.canFinishUpload.value).toBe(true)
  })

  it('isImportRowExcludedByStatus 匹配排除状态', () => {
    const { api } = makeDeps()
    api.importExcludeStatuses.value = ['REFUNDED']
    expect(api.isImportRowExcludedByStatus(makeRow(1, { lineStatus: 'REFUNDED' }))).toBe(true)
    expect(api.isImportRowExcludedByStatus(makeRow(2, { lineStatus: 'PAID' }))).toBe(false)
  })

  it('isImportRowImportable 各分支判定', () => {
    const { api } = makeDeps()
    expect(api.isImportRowImportable(makeRow(1, { parseStatus: 'ERROR' }))).toBe(false)
    expect(api.isImportRowImportable(makeRow(2, { matchStatus: 'MATCHED', statusMatchStatus: 'UNMATCHED', lineStatus: null }))).toBe(false)
    expect(api.isImportRowImportable(makeRow(3, { matchStatus: 'MATCHED', statusMatchStatus: 'MATCHED' }))).toBe(true)
    // UNMATCHED 无成本不可导入（状态已匹配，走到成本分支）
    expect(api.isImportRowImportable(makeRow(4, { matchStatus: 'UNMATCHED', statusMatchStatus: 'MATCHED', manualCostPrice: undefined }))).toBe(false)
    expect(api.isImportRowImportable(makeRow(5, { matchStatus: 'UNMATCHED', statusMatchStatus: 'MATCHED', manualCostPrice: 10 }))).toBe(true)
  })

  it('importReviewRows 过滤需要复核的行', () => {
    const { api } = makeDeps()
    api.importPreview.value = makePreview({
      rows: [
        makeRow(1, { matchStatus: 'MATCHED', statusMatchStatus: 'MATCHED' }),
        makeRow(2, { matchStatus: 'UNMATCHED', statusMatchStatus: 'MATCHED' }),
        makeRow(3, { matchStatus: 'MATCHED', statusMatchStatus: 'UNMATCHED' }),
      ],
    })
    expect(api.importReviewRows.value.map(r => r.id)).toEqual([2, 3])
  })

  it('importReviewDisplayRows 支持按链接/规格搜索', () => {
    const { api } = makeDeps()
    api.importPreview.value = makePreview({
      rows: [makeRow(1, { linkName: '苹果' }), makeRow(2, { linkName: '香蕉' })],
    })
    api.importReviewSearch.value = '香蕉'
    expect(api.importReviewDisplayRows.value.map(r => r.id)).toEqual([2])
    api.importReviewSearch.value = ''
    expect(api.importReviewDisplayRows.value).toHaveLength(2)
  })

  it('importPendingCostCount 统计缺成本的 UNMATCHED 行', () => {
    const { api } = makeDeps()
    api.importPreview.value = makePreview({
      rows: [
        makeRow(1, { matchStatus: 'UNMATCHED', manualCostPrice: undefined }),
        makeRow(2, { matchStatus: 'UNMATCHED', manualCostPrice: 10 }),
      ],
    })
    expect(api.importPendingCostCount.value).toBe(1)
  })

  it('resolveRowManualCost 草稿优先于行值', () => {
    const { api } = makeDeps()
    api.manualCostDrafts.value = { 1: '12.5' }
    expect(api.resolveRowManualCost(makeRow(1, { manualCostPrice: 8 }))).toBe(12.5)
    expect(api.resolveRowManualCost(makeRow(2, { manualCostPrice: 8 }))).toBe(8)
  })

  it('onManualCostInput 清洗并写回行成本', () => {
    const { api } = makeDeps()
    const row = makeRow(1)
    api.onManualCostInput(row, '12.345')
    expect(row.manualCostPrice).toBe(12.34)
    expect(api.manualCostDrafts.value[1]).toBe('12.34')
  })

  it('onManualCostBlur 有效值保留两位小数，无效值清空草稿', () => {
    const { api } = makeDeps()
    const row = makeRow(1, { manualCostPrice: 12.345 })
    api.onManualCostBlur(row)
    expect(row.manualCostPrice).toBe(12.35)
    expect(api.manualCostDrafts.value[1]).toBe('12.35')

    const row2 = makeRow(2, { manualCostPrice: undefined })
    api.manualCostDrafts.value[2] = ''
    api.onManualCostBlur(row2)
    expect(row2.manualCostPrice).toBeUndefined()
    expect(api.manualCostDrafts.value[2]).toBeUndefined()
  })

  it('applyImportReviewBatchCost 无效成本提示且不填充', () => {
    const { api } = makeDeps()
    api.importReviewBatchCost.value = '0'
    api.applyImportReviewBatchCost()
    expect(warningMock).toHaveBeenCalledWith('ecommerce.salesOrder.importReviewBatchCostInvalid')
  })

  it('applyImportReviewBatchCost 批量填充未匹配行成本', () => {
    const { api } = makeDeps()
    api.importPreview.value = makePreview({ rows: [makeRow(1, { matchStatus: 'UNMATCHED' })] })
    api.importReviewBatchCost.value = '9.9'
    api.applyImportReviewBatchCost()
    expect(api.importReviewRows.value[0].manualCostPrice).toBe(9.9)
    expect(successMock).toHaveBeenCalled()
  })

  it('importLineStatusLabel 与 Options 映射', () => {
    const { api } = makeDeps()
    expect(api.importLineStatusLabel('PAID')).toBe('ecommerce.salesOrder.importLineStatusPaid')
    expect(api.importLineStatusLabel()).toBe('—')
    expect(api.importLineStatusOptions.value).toHaveLength(7)
    expect(api.importLineStatusOptions.value[0]).toEqual({ value: 'PAID', label: 'ecommerce.salesOrder.importLineStatusPaid' })
  })

  it('importFileCardType 依据解析类型/文件名推断', () => {
    const { api } = makeDeps()
    api.importResumedFileName.value = 'a.csv'
    expect(api.importFileCardType.value).toBe('CSV')
    api.importResumedFileName.value = 'a.xls'
    expect(api.importFileCardType.value).toBe('XLS')
    api.importResumedFileName.value = 'a.xlsx'
    expect(api.importFileCardType.value).toBe('XLSX')
  })

  it('importFileCardMeta 拼接行数/大小/批次', () => {
    const { api } = makeDeps()
    api.importPreview.value = makePreview({ totalRows: 100, fileSize: 1024 })
    expect(api.importFileCardMeta.value).toContain('ecommerce.salesOrder.importFileMetaRows')
    expect(api.importFileCardMeta.value).toContain('B-001')
  })

  it('resetImport 清空全部导入状态', () => {
    const { api } = makeDeps()
    api.importPreview.value = makePreview()
    api.importFile.value = new File(['x'], 'a.xlsx')
    api.importResumed.value = true
    api.resetImport()
    expect(api.importPreview.value).toBeNull()
    expect(api.importFile.value).toBeNull()
    expect(api.importResumed.value).toBe(false)
  })

  it('resumePendingBatch 成功恢复批次', async () => {
    const { api } = makeDeps()
    previewMock.mockResolvedValue(makePreview({ shopId: 3, profileId: 8 }) as never)

    await api.resumePendingBatch(7)

    expect(previewMock).toHaveBeenCalledWith(7)
    expect(api.importShopId.value).toBe(3)
    expect(api.importProfileId.value).toBe(8)
    expect(api.importResumed.value).toBe(true)
    expect(api.importResuming.value).toBe(false)
  })

  it('resumePendingBatch 失败提示并复位加载态', async () => {
    const { api } = makeDeps()
    previewMock.mockRejectedValue(new Error('boom') as never)

    await api.resumePendingBatch(7)

    expect(errorMock).toHaveBeenCalledWith('ecommerce.salesOrder.resumeBatchFailed')
    expect(api.importResuming.value).toBe(false)
  })

  it('loadImportProfileForPlatform 找到首选 profile', async () => {
    const { api } = makeDeps()
    api.importShopId.value = 1
    await api.loadImportProfileForPlatform()
    expect(api.importProfileId.value).toBe(5)
  })

  it('onImportShopChange 清空 profile 并重新加载', async () => {
    const { api } = makeDeps()
    api.importProfileId.value = 5
    api.importPreview.value = makePreview()
    api.onImportShopChange()
    await Promise.resolve()
    expect(api.importProfileId.value).toBeNull()
    expect(api.importPreview.value).toBeNull()
  })

  it('handleImportFileSelected 无店铺时拦截', async () => {
    const { api } = makeDeps()
    await api.handleImportFileSelected(new File(['x'], 'a.xlsx'))
    expect(warningMock).toHaveBeenCalledWith('ecommerce.salesOrder.importShopRequired')
    expect(detectMock).not.toHaveBeenCalled()
  })

  it('handleImportFileSelected 解析空列时提示并清空', async () => {
    const { api } = makeDeps()
    api.importShopId.value = 1
    detectMock.mockResolvedValue({ fileType: 'xlsx', headerRow: 1, dataStartRow: 2, columns: [] } as never)
    await api.handleImportFileSelected(new File(['x'], 'a.xlsx'))
    expect(warningMock).toHaveBeenCalledWith('ecommerce.salesOrder.importEmpty')
    expect(api.importFile.value).toBeNull()
    expect(api.importFileParsing.value).toBe(false)
  })

  it('onFinishUpload 上传成功后填充预览', async () => {
    const { api } = makeDeps()
    // 无已存 profile → ensureImportProfile 走创建分支（createImportProfile 返回 id 9）
    profilesMock.mockResolvedValue([] as never)
    api.importShopId.value = 1
    api.importFile.value = new File(['x'], 'a.xlsx')
    api.parsedSpreadsheet.value = { fileName: 'a.xlsx', fileType: 'XLSX', headerRow: 1, dataStartRow: 2, columns: ['链接'] }

    await api.onFinishUpload()

    expect(createProfileMock).toHaveBeenCalled()
    expect(uploadMock).toHaveBeenCalledWith(expect.any(File), 1, 9, '2026-08')
    expect(api.importPreview.value?.batchId).toBe(7)
    expect(api.uploading.value).toBe(false)
  })

  it('onReparseImport 无 batch 直接返回', async () => {
    const { api } = makeDeps()
    await api.onReparseImport()
    expect(reparseMock).not.toHaveBeenCalled()
  })

  it('onReparseImport 成功重新解析', async () => {
    const { api } = makeDeps()
    api.importPreview.value = makePreview()
    await api.onReparseImport()
    expect(reparseMock).toHaveBeenCalledWith(7)
    expect(api.reparsing.value).toBe(false)
  })

  it('onCommitImport 缺成本时拦截', async () => {
    const { api } = makeDeps()
    api.importPreview.value = makePreview({ rows: [makeRow(1, { matchStatus: 'UNMATCHED', manualCostPrice: undefined })] })
    await api.onCommitImport()
    expect(warningMock).toHaveBeenCalledWith('ecommerce.salesOrder.importUnmatchedCostRequired')
    expect(commitMock).not.toHaveBeenCalled()
  })

  it('onCommitImport 缺状态时拦截', async () => {
    const { api } = makeDeps()
    api.importPreview.value = makePreview({
      rows: [makeRow(1, { matchStatus: 'MATCHED', statusMatchStatus: 'UNMATCHED', lineStatus: null, manualCostPrice: 10 })],
    })
    await api.onCommitImport()
    expect(warningMock).toHaveBeenCalledWith('ecommerce.salesOrder.importUnmatchedStatusRequired')
    expect(commitMock).not.toHaveBeenCalled()
  })

  it('onCommitImport 成功提交并关闭/刷新', async () => {
    const { api, refreshAll } = makeDeps()
    api.importPreview.value = makePreview({
      rows: [makeRow(1, { matchStatus: 'MATCHED', statusMatchStatus: 'MATCHED' })],
    })

    await api.onCommitImport()

    expect(commitMock).toHaveBeenCalledWith(7, {})
    expect(successMock).toHaveBeenCalledWith('ecommerce.salesOrder.importSuccess')
    expect(api.importVisible.value).toBe(false)
    expect(api.importPreview.value).toBeNull()
    expect(refreshAll).toHaveBeenCalled()
    expect(api.importing.value).toBe(false)
  })

  it('onCommitImport 失败提示错误', async () => {
    const { api } = makeDeps()
    api.importPreview.value = makePreview({ rows: [makeRow(1, { matchStatus: 'MATCHED', statusMatchStatus: 'MATCHED' })] })
    commitMock.mockRejectedValue(new Error('boom') as never)

    await api.onCommitImport()

    expect(errorMock).toHaveBeenCalledWith('ecommerce.salesOrder.importCommitFailed')
  })

  it('onMappingSaved 保存 profile 并触发重新解析', async () => {
    const { api } = makeDeps()
    api.importPreview.value = makePreview()
    api.onMappingSaved({ id: 8, name: '新映射' } as never)
    await Promise.resolve()
    expect(api.importProfileId.value).toBe(8)
    expect(api.importProfileName.value).toBe('新映射')
    expect(reparseMock).toHaveBeenCalledWith(7)
  })

  it('onStatusMappingSaved 用 profile.name，name 为空时保留原名', () => {
    const { api } = makeDeps()
    api.importProfileName.value = '旧名'
    api.onStatusMappingSaved({ id: 8, name: '新名' } as never)
    expect(api.importProfileName.value).toBe('新名')
    api.onStatusMappingSaved({ id: 8, name: null } as never)
    expect(api.importProfileName.value).toBe('新名')
  })

  it('triggerImportFileReplace 触发隐藏文件输入点击', () => {
    const { api } = makeDeps()
    api.importShopId.value = 1
    const click = vi.fn()
    api.importReplaceInputRef.value = { click } as unknown as HTMLInputElement
    api.triggerImportFileReplace()
    expect(click).toHaveBeenCalled()
  })

  it('onImportReplaceFileChange 有 batch 时替换并重新解析', async () => {
    const { api } = makeDeps()
    api.importPreview.value = makePreview()
    const input = { files: [new File(['x'], 'new.xlsx')], value: 'x' } as unknown as HTMLInputElement
    await api.onImportReplaceFileChange({ target: input } as unknown as Event)
    expect(replaceMock).toHaveBeenCalledWith(7, expect.any(File))
    expect(api.reparsing.value).toBe(false)
    expect(input.value).toBe('')
  })

  it('onImportReplaceFileChange 无 batch 时走普通文件选择', async () => {
    const { api } = makeDeps()
    api.importShopId.value = 1
    const input = { files: [new File(['x'], 'new.xlsx')], value: 'x' } as unknown as HTMLInputElement
    await api.onImportReplaceFileChange({ target: input } as unknown as Event)
    expect(detectMock).toHaveBeenCalled()
  })
})
