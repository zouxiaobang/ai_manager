import { computed, ref, watch, onUnmounted, getCurrentInstance } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import {
  fetchRagStats,
  fetchRagDocuments,
  fetchRagDocumentContent,
  retryRagDocument,
  removeRagDocument,
  searchRag,
  rebuildRagIndex,
  uploadRagDocument,
  fetchEmbeddingConfig,
  saveEmbeddingConfig,
  AI_PROVIDER_MAP,
} from '@/api/aiKnowledge'
import type { AiModelConfig, AiProvider, RagDocument, RagSource, RagStats } from '@/api/aiKnowledge'

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
  const retryingId = ref<string | null>(null)
  const rebuilding = ref(false)
  const ragSearchQuery = ref('')
  const ragSearchResults = ref<RagSource[]>([])
  // ========== 文档预览（点击文件名打开全屏预览） ==========
  const previewVisible = ref(false)
  const previewLoading = ref(false)
  const previewDoc = ref<RagDocument | null>(null)
  const previewContent = ref('')

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
  /** 各提供商 API Key 会话级缓存（切换提供商时记住/恢复真实 key，避免把 A 的 key 误存给 B） */
  const embedApiKeyCache = ref<Record<string, string>>({})
  /** 上一次（切换前）的 Embedding 提供商：@change 触发时 v-model 已更新 provider，需单独跟踪旧值 */
  const lastEmbedProvider = ref(embedConfigDraft.value.provider)
  /** 切换加载序号：丢弃被更快切换覆盖的异步响应 */
  let embedLoadSeq = 0
  /** 当前 Embedding 提供商元信息（API Key 占位符等） */
  const embedProviderInfo = computed(() => AI_PROVIDER_MAP[embedConfigDraft.value.provider])

  /** 把表单切到某提供商的默认值（key 单独指定），用于切换/清除/未保存场景 */
  function applyEmbeddingProviderDefaults(provider: AiProvider, apiKey: string) {
    const info = AI_PROVIDER_MAP[provider]
    if (!info) return
    embedConfigDraft.value = {
      provider,
      apiKey,
      apiBaseUrl: info.apiBaseUrl,
      model: info.model,
      embeddingModel: info.embeddingModel,
      temperature: info.temperature,
      maxTokens: info.maxTokens,
      defaultProvider: false,
      maxContextMessages: 10,
    }
  }

  /** 切换 Embedding 提供商：地址/Embedding 模型跟随新提供商默认值；key 优先本会话缓存，其次后端已存配置 */
  async function onEmbeddingProviderChange(provider: AiProvider) {
    const info = AI_PROVIDER_MAP[provider]
    if (!info) return
    // 先把上一个提供商当前输入的 key 记入缓存（@change 前 v-model 已改 provider，旧值需用 lastEmbedProvider 取）
    const old = lastEmbedProvider.value
    if (old && old !== provider) {
      embedApiKeyCache.value[old] = embedConfigDraft.value.apiKey
    }
    lastEmbedProvider.value = provider
    const seq = ++embedLoadSeq

    // 本会话缓存到该提供商的真实 key → 直接恢复（不再向后端请求，避免被脱敏值覆盖）
    const cachedKey = embedApiKeyCache.value[provider]
    if (cachedKey && cachedKey !== '****') {
      applyEmbeddingProviderDefaults(provider, cachedKey)
      embeddingConfigured.value = true
      return
    }

    // 先按默认值填充（清空旧提供商字段），再从后端读取该提供商已保存配置
    applyEmbeddingProviderDefaults(provider, '')
    embeddingConfigured.value = false
    try {
      const saved = await fetchEmbeddingConfig(provider)
      if (seq !== embedLoadSeq) return // 已被更快的切换覆盖
      if (saved?.apiKey) {
        embedConfigDraft.value = {
          provider,
          apiKey: saved.apiKey,
          apiBaseUrl: saved.apiBaseUrl || info.apiBaseUrl,
          model: saved.model || info.model,
          embeddingModel: saved.embeddingModel || info.embeddingModel,
          temperature: saved.temperature ?? info.temperature,
          maxTokens: saved.maxTokens ?? info.maxTokens,
          defaultProvider: saved.defaultProvider ?? false,
          maxContextMessages: saved.maxContextMessages ?? 10,
        }
        embeddingConfigured.value = saved.apiKey !== '****'
      }
    } catch {
      // 读取失败保持默认值（未配置）
    }
  }

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

  async function loadRagData(silent = false) {
    // silent=true 用于处理中自动轮询：不置 loading（避免列表每轮闪烁加载态）、失败不弹错（下轮重试即可）
    if (!silent) docsLoading.value = true
    try {
      // 分开拉取统计与文档列表：删除/重试后文档列表必须刷新，
      // 不能因统计接口瞬时失败被 Promise.all 带崩，导致界面残留已删文档（移除失效假象）
      const [statsResult, docsResult] = await Promise.allSettled([fetchRagStats(), fetchRagDocuments()])
      if (statsResult.status === 'fulfilled') {
        ragStats.value = statsResult.value
      }
      if (docsResult.status === 'fulfilled') {
        ragDocuments.value = docsResult.value
      } else if (!silent) {
        ElMessage.error(t('aiKnowledge.status.error'))
      }
    } finally {
      if (!silent) docsLoading.value = false
    }
  }

  // ========== 处理中文档自动轮询 ==========
  /** 轮询间隔 ms：异步处理（解析→分块→嵌入→存向量）通常数十秒，5s 轮询兼顾及时性与请求量 */
  const AUTO_REFRESH_INTERVAL = 5000
  let pollTimer: ReturnType<typeof setInterval> | null = null

  /** 是否有文档仍在异步处理中（processing/pending），驱动列表自动轮询 */
  const hasInFlightDocs = computed(() =>
    ragDocuments.value.some((d) => d.status === 'processing' || d.status === 'pending'),
  )

  function stopAutoRefresh() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  function startAutoRefresh() {
    if (pollTimer) return
    // 静默刷新：处理完成后列表自动置 ready，无需用户手动刷新
    pollTimer = setInterval(() => {
      void loadRagData(true)
    }, AUTO_REFRESH_INTERVAL)
  }

  // 有处理中文档则轮询，全部进入终态（ready/failed）即停；immediate 覆盖页面加载时已在处理的文档
  watch(
    hasInFlightDocs,
    (inFlight) => {
      if (inFlight) startAutoRefresh()
      else stopAutoRefresh()
    },
    { immediate: true },
  )
  // 无组件实例（单测环境）时 onUnmounted 无对应实例会告警，故仅在实际挂载时注册清理，避免定时器泄漏
  if (getCurrentInstance()) {
    onUnmounted(stopAutoRefresh)
  }

  // ========== Embedding 配置 ==========

  async function loadEmbeddingConfig() {
    try {
      const config = await fetchEmbeddingConfig()
      if (config?.provider) {
        const info = AI_PROVIDER_MAP[config.provider]
        embedConfigDraft.value = {
          provider: config.provider,
          apiKey: config.apiKey || '',
          apiBaseUrl: config.apiBaseUrl || info?.apiBaseUrl || 'https://api.openai.com/v1',
          model: config.model || info?.model || 'gpt-4o',
          embeddingModel: config.embeddingModel || info?.embeddingModel || 'text-embedding-3-small',
          temperature: config.temperature ?? info?.temperature ?? 0.7,
          maxTokens: config.maxTokens ?? info?.maxTokens ?? 4096,
          defaultProvider: config.defaultProvider ?? false,
          maxContextMessages: config.maxContextMessages ?? 10,
        }
        lastEmbedProvider.value = config.provider
        // 后端 apiKey 已脱敏（真实 key 形如 sk-****abcd，空值占位 ****），据此判断是否已配置
        embeddingConfigured.value = !!config.apiKey && config.apiKey !== '****'
      } else {
        // 无任何已保存配置 → 默认提供商未配置
        applyEmbeddingProviderDefaults('openai', '')
        lastEmbedProvider.value = 'openai'
        embeddingConfigured.value = false
      }
    } catch {
      applyEmbeddingProviderDefaults('openai', '')
      lastEmbedProvider.value = 'openai'
      embeddingConfigured.value = false
    }
  }

  async function saveEmbedConfig() {
    savingEmbedConfig.value = true
    try {
      // 先缓存当前真实 key，防止 loadEmbeddingConfig 用脱敏值覆盖后切换丢失
      embedApiKeyCache.value[embedConfigDraft.value.provider] = embedConfigDraft.value.apiKey
      await saveEmbeddingConfig(embedConfigDraft.value)
      ElMessage.success('Embedding 配置已保存')
      embeddingConfigured.value = !!embedConfigDraft.value.apiKey && embedConfigDraft.value.apiKey !== '****'
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
      // 清除该提供商缓存，本地重置为未配置（保留 provider 选择，避免误跳其他提供商）
      embedApiKeyCache.value[embedConfigDraft.value.provider] = ''
      applyEmbeddingProviderDefaults(embedConfigDraft.value.provider, '')
      embeddingConfigured.value = false
    } catch {
      ElMessage.error('清除 Embedding 配置失败')
    } finally {
      clearingEmbedConfig.value = false
    }
  }

  async function retryDoc(id: string) {
    retryingId.value = id
    try {
      await retryRagDocument(id)
      ElMessage.success(t('aiKnowledge.rag.retrySubmitted'))
      await loadRagData()
    } catch {
      // error handled globally
    } finally {
      retryingId.value = null
    }
  }

  async function removeDoc(id: string) {
    try {
      await removeRagDocument(id)
      ElMessage.success(t('aiKnowledge.rag.removeSuccess'))
      await loadRagData()
    } catch {
      // error handled globally
    }
  }

  /** 打开文档全屏预览：先渲染空态与 loading，再异步拉取内容（md 走 marked，其余走纯文本） */
  async function openDocPreview(doc: RagDocument) {
    previewDoc.value = doc
    previewContent.value = ''
    previewVisible.value = true
    previewLoading.value = true
    try {
      const data = await fetchRagDocumentContent(doc.id)
      previewContent.value = data.content
    } catch {
      ElMessage.error(t('aiKnowledge.rag.previewLoadError'))
    } finally {
      previewLoading.value = false
    }
  }

  // 关闭预览（v-model 置 false）时清理状态，避免下次打开残留旧文档内容
  watch(previewVisible, (open) => {
    if (!open) {
      previewDoc.value = null
      previewContent.value = ''
    }
  })

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
    previewVisible,
    previewLoading,
    previewDoc,
    previewContent,
    openDocPreview,
    embedConfigExpanded,
    embedConfigDraft,
    embedConfigFormRef,
    embedProviderInfo,
    savingEmbedConfig,
    clearingEmbedConfig,
    embeddingConfigured,
    onEmbeddingProviderChange,
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
