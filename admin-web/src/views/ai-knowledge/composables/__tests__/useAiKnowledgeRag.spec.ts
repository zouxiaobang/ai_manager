import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAiKnowledgeRag } from '../useAiKnowledgeRag'
import {
  fetchRagStats,
  fetchRagDocuments,
  retryRagDocument,
  removeRagDocument,
  searchRag,
  rebuildRagIndex,
  uploadRagDocument,
  fetchEmbeddingConfig,
  saveEmbeddingConfig,
} from '@/api/aiKnowledge'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { RagDocument, RagSource, RagStats } from '@/api/aiKnowledge'

vi.mock('@/api/aiKnowledge', () => ({
  fetchRagStats: vi.fn(),
  fetchRagDocuments: vi.fn(),
  retryRagDocument: vi.fn(),
  removeRagDocument: vi.fn(),
  searchRag: vi.fn(),
  rebuildRagIndex: vi.fn(),
  uploadRagDocument: vi.fn(),
  fetchEmbeddingConfig: vi.fn(),
  saveEmbeddingConfig: vi.fn(),
  // loadEmbeddingConfig 在配置缺失部分字段时回退读取 AI_PROVIDER_MAP；切换提供商测试需要 openai/deepseek 默认值
  AI_PROVIDER_MAP: {
    openai: { label: 'OpenAI', apiBaseUrl: 'https://api.openai.com/v1', model: 'gpt-4o', embeddingModel: 'text-embedding-3-small', temperature: 0.7, maxTokens: 4096, apiKeyHint: '' },
    deepseek: { label: 'DeepSeek', apiBaseUrl: 'https://api.deepseek.com', model: 'deepseek-chat', embeddingModel: 'deepseek-embedding', temperature: 0.7, maxTokens: 8192, apiKeyHint: '' },
  },
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn() },
  ElMessageBox: { confirm: vi.fn() },
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (k: string) => k }),
}))

const statsMock = vi.mocked(fetchRagStats)
const docsMock = vi.mocked(fetchRagDocuments)
const retryMock = vi.mocked(retryRagDocument)
const removeMock = vi.mocked(removeRagDocument)
const searchMock = vi.mocked(searchRag)
const rebuildMock = vi.mocked(rebuildRagIndex)
const uploadMock = vi.mocked(uploadRagDocument)
const fetchEmbedMock = vi.mocked(fetchEmbeddingConfig)
const saveEmbedMock = vi.mocked(saveEmbeddingConfig)
const successMock = vi.mocked(ElMessage.success)
const errorMock = vi.mocked(ElMessage.error)
const confirmMock = vi.mocked(ElMessageBox.confirm)

function makeDoc(id: number, status: RagDocument['status'] = 'ready'): RagDocument {
  return { id, fileName: `f${id}.pdf`, fileType: 'pdf', fileSize: 100, chunkCount: 1, status, indexedAt: null, errorMessage: null }
}
function makeStats(overrides: Partial<RagStats> = {}): RagStats {
  return { totalDocs: 1, readyCount: 1, processingCount: 0, failedCount: 0, totalChunks: 2, ...overrides }
}
function makeSource(id: number): RagSource {
  return { documentId: id, fileName: `f${id}.pdf`, chunkIndex: 0, content: '内容', score: 0.9 }
}

beforeEach(() => {
  vi.clearAllMocks()
  statsMock.mockResolvedValue(makeStats() as never)
  docsMock.mockResolvedValue([makeDoc(1)] as never)
  retryMock.mockResolvedValue(undefined as never)
  removeMock.mockResolvedValue(undefined as never)
  searchMock.mockResolvedValue({ sources: [makeSource(1)] } as never)
  rebuildMock.mockResolvedValue(undefined as never)
  saveEmbedMock.mockResolvedValue(undefined as never)
})

describe('useAiKnowledgeRag', () => {
  it('loadRagData 拉取统计与文档列表', async () => {
    const api = useAiKnowledgeRag()
    docsMock.mockResolvedValue([makeDoc(1), makeDoc(2)] as never)

    await api.loadRagData()

    expect(statsMock).toHaveBeenCalled()
    expect(docsMock).toHaveBeenCalled()
    expect(api.ragStats.value).toEqual(makeStats())
    expect(api.ragDocuments.value).toHaveLength(2)
    expect(api.docsLoading.value).toBe(false)
  })

  it('loadRagData 统计失败时文档列表仍刷新且不报错', async () => {
    const api = useAiKnowledgeRag()
    statsMock.mockRejectedValue(new Error('boom') as never)
    docsMock.mockResolvedValue([makeDoc(2)] as never)

    await api.loadRagData()

    // 文档列表必须刷新（删除/重试后不残留旧列表），统计失败仅静默跳过
    expect(api.ragDocuments.value).toEqual([makeDoc(2)])
    expect(api.ragStats.value).toBeNull()
    expect(errorMock).not.toHaveBeenCalled()
    expect(api.docsLoading.value).toBe(false)
  })

  it('loadRagData 文档列表失败时提示错误且不更新列表', async () => {
    const api = useAiKnowledgeRag()
    docsMock.mockRejectedValue(new Error('boom') as never)

    await api.loadRagData()

    expect(errorMock).toHaveBeenCalled()
    expect(api.docsLoading.value).toBe(false)
  })

  it('loadEmbeddingConfig 填充草稿并标记已配置', async () => {
    const api = useAiKnowledgeRag()
    fetchEmbedMock.mockResolvedValue({
      provider: 'claude', apiKey: 'sk-123', apiBaseUrl: 'https://x', model: 'claude-5',
      embeddingModel: 'embed', temperature: 0.3, maxTokens: 2048, defaultProvider: true, maxContextMessages: 5,
    } as never)

    await api.loadEmbeddingConfig()

    expect(api.embedConfigDraft.value.provider).toBe('claude')
    expect(api.embedConfigDraft.value.apiKey).toBe('sk-123')
    expect(api.embedConfigDraft.value.embeddingModel).toBe('embed')
    expect(api.embeddingConfigured.value).toBe(true)
  })

  it('loadEmbeddingConfig 中空 key 占位 **** 视为未配置', async () => {
    const api = useAiKnowledgeRag()
    fetchEmbedMock.mockResolvedValue({ provider: 'openai', apiKey: '****' } as never)

    await api.loadEmbeddingConfig()

    expect(api.embeddingConfigured.value).toBe(false)
  })

  it('loadEmbeddingConfig 中脱敏真实 key 视为已配置（不再把 **** 当未配置）', async () => {
    const api = useAiKnowledgeRag()
    fetchEmbedMock.mockResolvedValue({ provider: 'openai', apiKey: 'sk-****abcd' } as never)

    await api.loadEmbeddingConfig()

    expect(api.embeddingConfigured.value).toBe(true)
  })

  it('loadEmbeddingConfig 失败时使用默认值并标记未配置', async () => {
    const api = useAiKnowledgeRag()
    fetchEmbedMock.mockRejectedValue(new Error('boom') as never)

    await api.loadEmbeddingConfig()

    expect(api.embeddingConfigured.value).toBe(false)
    expect(api.embedConfigDraft.value.provider).toBe('openai')
  })

  it('onEmbeddingProviderChange 地址与模型跟随提供商且 key 不跨提供商误用', async () => {
    const api = useAiKnowledgeRag()
    fetchEmbedMock.mockResolvedValue(undefined as never)
    api.embedConfigDraft.value.apiKey = 'sk-openai'

    await api.onEmbeddingProviderChange('deepseek')

    // 地址/Embedding 模型/chat 模型跟随 deepseek 默认值
    expect(api.embedConfigDraft.value.provider).toBe('deepseek')
    expect(api.embedConfigDraft.value.apiBaseUrl).toBe('https://api.deepseek.com')
    expect(api.embedConfigDraft.value.embeddingModel).toBe('deepseek-embedding')
    expect(api.embedConfigDraft.value.model).toBe('deepseek-chat')
    // 无 deepseek 已存配置 → key 置空，避免把 openai 的 key 误存给 deepseek
    expect(api.embedConfigDraft.value.apiKey).toBe('')
    expect(fetchEmbedMock).toHaveBeenCalledWith('deepseek')

    // 切回 openai：key 从缓存恢复，地址/模型同步还原
    await api.onEmbeddingProviderChange('openai')
    expect(api.embedConfigDraft.value.apiKey).toBe('sk-openai')
    expect(api.embedConfigDraft.value.apiBaseUrl).toBe('https://api.openai.com/v1')
    expect(api.embedConfigDraft.value.embeddingModel).toBe('text-embedding-3-small')
  })

  it('onEmbeddingProviderChange 从后端恢复该提供商已保存配置', async () => {
    const api = useAiKnowledgeRag()
    fetchEmbedMock.mockResolvedValue({
      provider: 'deepseek', apiKey: 'sk-****wxyz', apiBaseUrl: 'https://api.deepseek.com',
      model: 'deepseek-chat', embeddingModel: 'deepseek-embedding',
    } as never)

    await api.onEmbeddingProviderChange('deepseek')

    expect(fetchEmbedMock).toHaveBeenCalledWith('deepseek')
    expect(api.embedConfigDraft.value.provider).toBe('deepseek')
    expect(api.embedConfigDraft.value.apiKey).toBe('sk-****wxyz')
    expect(api.embeddingConfigured.value).toBe(true)
  })

  it('onEmbeddingProviderChange 已保存提供商 key 为空占位 **** 标记未配置', async () => {
    const api = useAiKnowledgeRag()
    fetchEmbedMock.mockResolvedValue({ provider: 'deepseek', apiKey: '****' } as never)

    await api.onEmbeddingProviderChange('deepseek')

    expect(api.embeddingConfigured.value).toBe(false)
  })

  it('loadEmbeddingConfig 后切换提供商可恢复已加载的 key', async () => {
    const api = useAiKnowledgeRag()
    fetchEmbedMock.mockResolvedValue({ provider: 'openai', apiKey: 'sk-real' } as never)

    await api.loadEmbeddingConfig()

    fetchEmbedMock.mockImplementation((provider?: string) => {
      if (provider === 'deepseek') return Promise.resolve(undefined as never)
      return Promise.resolve({ provider: 'openai', apiKey: 'sk-real' } as never)
    })
    await api.onEmbeddingProviderChange('deepseek')
    expect(api.embedConfigDraft.value.apiKey).toBe('')

    await api.onEmbeddingProviderChange('openai')
    expect(api.embedConfigDraft.value.apiKey).toBe('sk-real')
    expect(api.embedProviderInfo.value?.label).toBe('OpenAI')
  })

  it('saveEmbedConfig 保存成功并刷新', async () => {
    const api = useAiKnowledgeRag()
    api.embedConfigDraft.value.apiKey = 'sk-new'
    fetchEmbedMock.mockResolvedValue({ provider: 'openai', apiKey: 'sk-****abcd' } as never)

    await api.saveEmbedConfig()

    expect(saveEmbedMock).toHaveBeenCalled()
    expect(successMock).toHaveBeenCalledWith('Embedding 配置已保存')
    expect(api.embeddingConfigured.value).toBe(true)
    expect(api.savingEmbedConfig.value).toBe(false)
  })

  it('saveEmbedConfig 失败提示错误', async () => {
    const api = useAiKnowledgeRag()
    saveEmbedMock.mockRejectedValue(new Error('boom') as never)

    await api.saveEmbedConfig()

    expect(errorMock).toHaveBeenCalledWith('保存 Embedding 配置失败')
  })

  it('clearEmbedConfig 清空 apiKey 并本地重置为未配置', async () => {
    const api = useAiKnowledgeRag()
    api.embedConfigDraft.value.apiKey = 'sk-****abcd'

    await api.clearEmbedConfig()

    expect(saveEmbedMock).toHaveBeenCalledWith(expect.objectContaining({ apiKey: '', embeddingModel: '' }))
    expect(successMock).toHaveBeenCalled()
    expect(api.embeddingConfigured.value).toBe(false)
    expect(api.embedConfigDraft.value.apiKey).toBe('')
  })

  it('retryDoc 重试指定文档并刷新列表', async () => {
    const api = useAiKnowledgeRag()

    await api.retryDoc(7)

    expect(retryMock).toHaveBeenCalledWith(7)
    expect(successMock).toHaveBeenCalledWith('aiKnowledge.rag.retrySubmitted')
    expect(docsMock).toHaveBeenCalled()
    expect(api.retryingId.value).toBeNull()
  })

  it('removeDoc 删除文档并刷新列表', async () => {
    const api = useAiKnowledgeRag()

    await api.removeDoc(7)

    expect(removeMock).toHaveBeenCalledWith(7)
    expect(successMock).toHaveBeenCalledWith('aiKnowledge.rag.removeSuccess')
    expect(docsMock).toHaveBeenCalled()
  })

  it('rebuildIndex 确认后重建索引并刷新', async () => {
    const api = useAiKnowledgeRag()
    confirmMock.mockResolvedValue('confirm' as never)

    await api.rebuildIndex()

    expect(rebuildMock).toHaveBeenCalled()
    expect(docsMock).toHaveBeenCalled()
    expect(api.rebuilding.value).toBe(false)
  })

  it('rebuildIndex 取消时不重建', async () => {
    const api = useAiKnowledgeRag()
    confirmMock.mockRejectedValue(new Error('cancel'))

    await api.rebuildIndex()

    expect(rebuildMock).not.toHaveBeenCalled()
  })

  it('doRagSearch 空查询直接跳过', async () => {
    const api = useAiKnowledgeRag()

    await api.doRagSearch()

    expect(searchMock).not.toHaveBeenCalled()
  })

  it('doRagSearch 填充搜索结果', async () => {
    const api = useAiKnowledgeRag()
    api.ragSearchQuery.value = '关键词'

    await api.doRagSearch()

    expect(searchMock).toHaveBeenCalledWith({ query: '关键词', topK: 5 })
    expect(api.ragSearchResults.value).toHaveLength(1)
  })

  it('statusTagType 映射状态', () => {
    const api = useAiKnowledgeRag()
    expect(api.statusTagType('ready')).toBe('success')
    expect(api.statusTagType('processing')).toBe('warning')
    expect(api.statusTagType('failed')).toBe('danger')
    expect(api.statusTagType('pending')).toBe('info')
    expect(api.statusTagType('unknown')).toBe('info')
  })

  it('statusLabel 映射状态文案', () => {
    const api = useAiKnowledgeRag()
    expect(api.statusLabel('pending')).toBe('aiKnowledge.rag.statusPending')
    expect(api.statusLabel('unknown')).toBe('unknown')
  })

  it('handleFileSelected 无文件时直接返回', async () => {
    const api = useAiKnowledgeRag()
    const event = { target: { files: [] } } as unknown as Event

    await api.handleFileSelected(event)

    expect(uploadMock).not.toHaveBeenCalled()
  })

  it('handleFileSelected 上传成功并刷新列表', async () => {
    const api = useAiKnowledgeRag()
    uploadMock.mockResolvedValue({ documentId: 1, fileName: 'a.pdf' } as never)
    const input = { files: [{ name: 'a.pdf' }], value: 'x' } as unknown as HTMLInputElement
    const event = { target: input } as unknown as Event

    await api.handleFileSelected(event)

    expect(uploadMock).toHaveBeenCalledWith(input.files![0])
    expect(successMock).toHaveBeenCalled()
    expect(docsMock).toHaveBeenCalled()
    expect(input.value).toBe('')
    expect(api.uploading.value).toBe(false)
  })

  it('handleFileSelected 上传失败提示错误并重置 input', async () => {
    const api = useAiKnowledgeRag()
    uploadMock.mockRejectedValue(new Error('bad file') as never)
    const input = { files: [{ name: 'a.pdf' }], value: 'x' } as unknown as HTMLInputElement
    const event = { target: input } as unknown as Event

    await api.handleFileSelected(event)

    expect(errorMock).toHaveBeenCalledWith('bad file')
    expect(input.value).toBe('')
  })

  it('triggerUpload 触发隐藏文件输入框点击', () => {
    const api = useAiKnowledgeRag()
    const click = vi.fn()
    api.fileInputRef.value = { click } as unknown as HTMLInputElement

    api.triggerUpload()

    expect(click).toHaveBeenCalled()
  })
})
