import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
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
  AI_PROVIDER_MAP,
} from '@/api/aiKnowledge'
import type { AiModelConfig, RagDocument, RagSource, RagStats } from '@/api/aiKnowledge'

/**
 * AI 知识页 RAG 知识库与 Embedding 配置状态机
 *
 * <p>从 {@code AiKnowledgeView.vue} 提取：RAG 统计/文档管理/搜索/重建、文件上传、
 * Embedding 配置的加载/保存/清除。完全自包含（i18n 在内部获取），可独立于组件单元测试。</p>
 */
export function useAiKnowledgeRag() {
  const { t } = useI18n()

  // ========== RAG 知识库 ==========
  const ragStats = ref<RagStats | null>(null)
  const ragDocuments = ref<RagDocument[]>([])
  const docsLoading = ref(false)
  const retryingId = ref<number | null>(null)
  const rebuilding = ref(false)
  const ragSearchQuery = ref('')
  const ragSearchResults = ref<RagSource[]>([])

  // ========== Embedding 配置（独立于 Chat 配置） ==========
  const embedConfigExpanded = ref(false)
  const embedConfigDraft = ref<AiModelConfig>({
    provider: 'openai',
    apiKey: '',
    apiBaseUrl: 'https://api.openai.com/v1',
    model: 'gpt-4o',
    embeddingModel: 'text-embedding-3-small',
    temperature: 0.7,
    maxTokens: 4096,
    defaultProvider: false,
    maxContextMessages: 10,
  })
  const embedConfigFormRef = ref<FormInstance | null>(null)
  const savingEmbedConfig = ref(false)
  const clearingEmbedConfig = ref(false)
  const embeddingConfigured = ref(false)

  function statusTagType(status: string): 'success' | 'warning' | 'danger' | 'info' {
    switch (status) {
      case 'ready': return 'success'
      case 'processing': return 'warning'
      case 'failed': return 'danger'
      default: return 'info'
    }
  }

  function statusLabel(status: string): string {
    switch (status) {
      case 'pending': return t('aiKnowledge.rag.statusPending')
      case 'processing': return t('aiKnowledge.rag.statusProcessing')
      case 'ready': return t('aiKnowledge.rag.statusReady')
      case 'failed': return t('aiKnowledge.rag.statusFailed')
      default: return status
    }
  }

  async function loadRagData() {
    docsLoading.value = true
    try {
      const [stats, docs] = await Promise.all([
        fetchRagStats(),
        fetchRagDocuments(),
      ])
      ragStats.value = stats
      ragDocuments.value = docs
    } catch {
      ElMessage.error(t('aiKnowledge.status.error'))
    } finally {
      docsLoading.value = false
    }
  }

  // ========== Embedding 配置 ==========

  async function loadEmbeddingConfig() {
    try {
      const config = await fetchEmbeddingConfig()
      embedConfigDraft.value = {
        provider: config?.provider || 'openai',
        apiKey: config?.apiKey || '',
        apiBaseUrl: config?.apiBaseUrl || AI_PROVIDER_MAP[config?.provider || 'openai']?.apiBaseUrl || 'https://api.openai.com/v1',
        model: config?.model || 'gpt-4o',
        embeddingModel: config?.embeddingModel || AI_PROVIDER_MAP[config?.provider || 'openai']?.embeddingModel || 'text-embedding-3-small',
        temperature: config?.temperature ?? 0.7,
        maxTokens: config?.maxTokens ?? 4096,
        defaultProvider: config?.defaultProvider ?? false,
        maxContextMessages: config?.maxContextMessages ?? 10,
      }
      embeddingConfigured.value = !!config?.apiKey && !config.apiKey.includes('****')
    } catch {
      // 加载失败使用默认值
      embeddingConfigured.value = false
    }
  }

  async function saveEmbedConfig() {
    savingEmbedConfig.value = true
    try {
      await saveEmbeddingConfig(embedConfigDraft.value)
      ElMessage.success('Embedding 配置已保存')
      embeddingConfigured.value = true
      // 刷新 embedding 配置状态
      await loadEmbeddingConfig()
    } catch {
      ElMessage.error('保存 Embedding 配置失败')
    } finally {
      savingEmbedConfig.value = false
    }
  }

  async function clearEmbedConfig() {
    clearingEmbedConfig.value = true
    try {
      await saveEmbeddingConfig({
        provider: embedConfigDraft.value.provider,
        apiKey: '',
        apiBaseUrl: '',
        model: '',
        embeddingModel: '',
        temperature: 0.7,
        maxTokens: 4096,
        defaultProvider: false,
        maxContextMessages: 10,
      })
      ElMessage.success('Embedding 配置已清除，将使用 Chat 配置')
      embeddingConfigured.value = false
      await loadEmbeddingConfig()
    } catch {
      ElMessage.error('清除 Embedding 配置失败')
    } finally {
      clearingEmbedConfig.value = false
    }
  }

  async function retryDoc(id: number) {
    retryingId.value = id
    try {
      await retryRagDocument(id)
      ElMessage.success(t('aiKnowledge.status.loading'))
      await loadRagData()
    } catch {
      // error handled globally
    } finally {
      retryingId.value = null
    }
  }

  async function removeDoc(id: number) {
    try {
      await removeRagDocument(id)
      ElMessage.success(t('common.save'))
      await loadRagData()
    } catch {
      // error handled globally
    }
  }

  async function rebuildIndex() {
    try {
      await ElMessageBox.confirm(
        t('aiKnowledge.rag.rebuildConfirm'),
        t('common.confirmTitle'),
        { type: 'warning' },
      )
    } catch {
      return
    }
    rebuilding.value = true
    try {
      await rebuildRagIndex()
      ElMessage.success(t('aiKnowledge.status.loading'))
      await loadRagData()
    } catch {
      // error handled globally
    } finally {
      rebuilding.value = false
    }
  }

  async function doRagSearch() {
    const query = ragSearchQuery.value.trim()
    if (!query) return
    try {
      const result = await searchRag({ query, topK: 5 })
      ragSearchResults.value = result.sources
    } catch {
      // error handled globally
    }
  }

  const uploading = ref(false)
  const fileInputRef = ref<HTMLInputElement | null>(null)

  function triggerUpload() {
    fileInputRef.value?.click()
  }

  async function handleFileSelected(event: Event) {
    const input = event.target as HTMLInputElement
    const file = input.files?.[0]
    if (!file) return

    uploading.value = true
    try {
      const result = await uploadRagDocument(file)
      ElMessage.success(`${t('common.save')}：${result.fileName}`)
      await loadRagData()
    } catch (e: unknown) {
      ElMessage.error(e instanceof Error ? e.message : t('aiKnowledge.status.error'))
    } finally {
      uploading.value = false
      // 重置 input 以允许重复上传同名文件
      input.value = ''
    }
  }

  return {
    ragStats,
    ragDocuments,
    docsLoading,
    retryingId,
    rebuilding,
    ragSearchQuery,
    ragSearchResults,
    embedConfigExpanded,
    embedConfigDraft,
    embedConfigFormRef,
    savingEmbedConfig,
    clearingEmbedConfig,
    embeddingConfigured,
    uploading,
    fileInputRef,
    statusTagType,
    statusLabel,
    loadRagData,
    loadEmbeddingConfig,
    saveEmbedConfig,
    clearEmbedConfig,
    retryDoc,
    removeDoc,
    rebuildIndex,
    doRagSearch,
    triggerUpload,
    handleFileSelected,
  }
}
