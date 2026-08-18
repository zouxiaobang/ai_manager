import { computed, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  fetchAiModelConfig,
  saveAiModelConfig,
  sendChatMessage,
  AI_PROVIDER_MAP,
  type AiProvider,
  type AiModelConfig,
  type ProviderInfo,
} from '@/api/aiKnowledge'

/** useAiKnowledgeSettings 依赖注入：共享 ref 与来自聊天状态机的回调 */
export interface AiKnowledgeSettingsDeps {
  /** 各 AI provider 列表（chat 状态机共享，保存后由 loadProviders 刷新） */
  providerList: Ref<ProviderInfo[]>
  /** 当前对话使用的 AI provider（chat 状态机读取，切换 provider 时同步） */
  chatProvider: Ref<AiProvider>
  /** 重新拉取 provider 列表（chat 状态机提供，保存配置后调用刷新默认值） */
  loadProviders: () => Promise<void>
}

/**
 * AI 知识页「设置」状态机
 *
 * <p>从 {@code AiKnowledgeView.vue} 提取：Chat 配置（provider/apiKey/model/上下文参数）的
 * 加载、保存、连接测试与 provider 切换草稿缓存。依赖通过参数注入，可独立于组件单元测试。</p>
 */
export function useAiKnowledgeSettings({ providerList, chatProvider, loadProviders }: AiKnowledgeSettingsDeps) {
  const { t } = useI18n()

  const configFormRef = ref<FormInstance | null>(null)
  const configLoading = ref(false)
  const savingConfig = ref(false)
  const testingConnection = ref(false)

  function buildDefaultConfig(provider: AiProvider = 'openai'): AiModelConfig {
    const info = AI_PROVIDER_MAP[provider]
    return {
      provider,
      apiKey: '',
      apiBaseUrl: info.apiBaseUrl,
      model: info.model,
      temperature: info.temperature,
      maxTokens: info.maxTokens,
      embeddingModel: info.embeddingModel,
      defaultProvider: false,
      maxContextMessages: 10,
    }
  }

  const defaultConfig = buildDefaultConfig()

  /** 加号按钮点击处理 */
  function onPlusClick() {
    ElMessage.info(t('aiKnowledge.chat.attachComingSoon'))
  }

  /** 头像加载失败时的 fallback 处理 */
  function onAvatarError(e: Event) {
    const img = e.target as HTMLImageElement
    if (img && !img.src.endsWith('custom.svg')) {
      img.src = '/icons/providers/custom.svg'
    }
  }

  const configDraft = ref<AiModelConfig>({ ...defaultConfig })

  /** 上一次的提供商（用于切换时缓存 API Key） */
  const lastProvider = ref(configDraft.value.provider)

  /** 各提供商的草稿缓存（切换时保留未保存的修改） */
  const configDraftCache = ref<Record<string, AiModelConfig>>({})

  // 在 Settings 中切换提供商时同步 chat 状态机使用的 provider
  watch(() => configDraft.value.provider, (val) => {
    chatProvider.value = val
  })

  /** API Key 缓存（切换 provider 时保留） */
  const apiKeyCache = ref<Record<string, string>>({})

  /** 当前 provider 信息 */
  const providerInfo = computed(() => AI_PROVIDER_MAP[configDraft.value.provider])

  /** 模型选项列表（保留用户当前选择的 model） */
  const modelOptions = computed(() => {
    const currentProvider = configDraft.value.provider
    const currentModel = configDraft.value.model

    // 构建可用的 provider 选项列表，保留当前配置的 model
    if (providerList.value.length > 0) {
      return providerList.value.map(p => {
        // 若 provider 匹配 configDraft 中的当前配置则保留该 model，否则使用 Settings 默认值
        const model = p.provider === currentProvider && currentModel
          ? currentModel
          : p.model
        return {
          key: p.provider,
          displayModel: model || AI_PROVIDER_MAP[p.provider]?.model || p.provider,
          configured: p.configured,
          defaultProvider: p.defaultProvider,
        }
      })
    }

    // fallback: AI_PROVIDER_MAP 中未在 providerList 中出现的条目
    return Object.entries(AI_PROVIDER_MAP).map(([key, info]) => ({
      key,
      displayModel: key === currentProvider && currentModel ? currentModel : info.model || info.label,
      defaultProvider: false,
    }))
  })

  /** 获取当前 AI provider 的图标 URL */
  const providerAvatarUrl = computed(() => {
    return `/icons/providers/${chatProvider.value}.svg`
  })

  const configRules: FormRules = {
    apiKey: [{ required: true, message: t('aiKnowledge.settings.apiKeyPlaceholder'), trigger: 'blur' }],
    model: [{ required: true, message: t('aiKnowledge.settings.modelPlaceholder'), trigger: 'blur' }],
  }

  async function loadConfig() {
    if (configLoading.value) return
    configLoading.value = true
    try {
      const data = await fetchAiModelConfig()
      configDraft.value = { ...buildDefaultConfig(data.provider), ...data }
      chatProvider.value = data.provider
      lastProvider.value = data.provider
    } catch {
      // 加载配置失败，使用默认配置
      configDraft.value = { ...defaultConfig }
      chatProvider.value = defaultConfig.provider
      lastProvider.value = defaultConfig.provider
    } finally {
      configLoading.value = false
    }
  }

  async function saveConfig() {
    const valid = await configFormRef.value?.validate().catch(() => false)
    if (!valid) return

    savingConfig.value = true
    try {
      await saveAiModelConfig(configDraft.value)
      ElMessage.success(t('aiKnowledge.settings.saveSuccess'))
      // 更新缓存中的已保存值，下次切换不会丢失
      configDraftCache.value[configDraft.value.provider] = { ...configDraft.value }
      // 保存后刷新 provider 列表以更新默认配置
      loadProviders()
    } catch {
      ElMessage.error(t('aiKnowledge.settings.saveFailed'))
    } finally {
      savingConfig.value = false
    }
  }

  async function testConnection() {
    testingConnection.value = true
    try {
      // 测试连接：发送一条简单消息
      await sendChatMessage({ question: 'Hi', provider: configDraft.value.provider, useRag: false })
      ElMessage.success(t('aiKnowledge.settings.testSuccess'))
    } catch {
      ElMessage.error(t('aiKnowledge.settings.testFailed'))
    } finally {
      testingConnection.value = false
    }
  }

  /**
   * 切换 AI provider 时的处理函数
   * - 更新当前选择的 model 为该 provider 的默认 model
   * - API Key 若不同则清空并提示用户重新填写
   */
  function onProviderChange(provider: AiProvider) {
    const info = AI_PROVIDER_MAP[provider]
    if (!info) return

    // 缓存当前提供商的草稿（切换后保留未保存的修改）
    const old = lastProvider.value
    if (old && old !== provider) {
      configDraftCache.value[old] = { ...configDraft.value }
    }

    // 检查是否有缓存的草稿
    const cached = configDraftCache.value[provider]
    if (cached) {
      configDraft.value = { ...cached }
    } else {
      // 从 providerList 恢复已保存的配置
      const saved = providerList.value.find(p => p.provider === provider)
      configDraft.value.apiBaseUrl = info.apiBaseUrl
      configDraft.value.model = saved?.model || info.model
      configDraft.value.embeddingModel = info.embeddingModel
      configDraft.value.temperature = info.temperature
      configDraft.value.maxTokens = info.maxTokens
      configDraft.value.apiKey = apiKeyCache.value[provider] ?? ''
      configDraft.value.defaultProvider = saved?.defaultProvider ?? false
      configDraft.value.maxContextMessages = saved?.maxContextMessages ?? 10
    }

    // 恢复完草稿后回填 provider：聊天区顶部切换（onChatModelChange）时 el-select 只更新
    // chatProvider 而不写 configDraft.provider；不回填会让 modelOptions 用旧 provider 匹配
    // currentModel，导致下拉里多项显示同一模型（如 openai 项也变成 deepseek-chat）
    configDraft.value.provider = provider

    lastProvider.value = provider
  }

  /** 聊天框 provider 选择器变更时的转发（模板 @change 绑定） */
  function onChatModelChange(provider: AiProvider) {
    onProviderChange(provider)
  }

  return {
    configFormRef,
    configLoading,
    savingConfig,
    testingConnection,
    configDraft,
    providerInfo,
    modelOptions,
    providerAvatarUrl,
    configRules,
    buildDefaultConfig,
    onChatModelChange,
    onPlusClick,
    onAvatarError,
    loadConfig,
    saveConfig,
    testConnection,
    onProviderChange,
  }
}
