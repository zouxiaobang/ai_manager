import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import { useAiKnowledgeSettings } from '../useAiKnowledgeSettings'
import {
  fetchAiModelConfig,
  saveAiModelConfig,
  sendChatMessage,
  AI_PROVIDER_MAP,
} from '@/api/aiKnowledge'
import { ElMessage } from 'element-plus'
import type { AiProvider, ProviderInfo } from '@/api/aiKnowledge'

vi.mock('@/api/aiKnowledge', () => ({
  fetchAiModelConfig: vi.fn(),
  saveAiModelConfig: vi.fn(),
  sendChatMessage: vi.fn(),
  // buildDefaultConfig / modelOptions fallback 会遍历 AI_PROVIDER_MAP，mock 需提供全部 provider
  AI_PROVIDER_MAP: {
    openai: { label: 'OpenAI', apiBaseUrl: 'https://api.openai.com/v1', model: 'gpt-4o', embeddingModel: 'text-embedding-3-small', temperature: 0.7, maxTokens: 4096, apiKeyHint: 'platform.openai.com/api-keys' },
    claude: { label: 'Claude', apiBaseUrl: 'https://api.anthropic.com', model: 'claude-sonnet-4-20250514', embeddingModel: 'text-embedding-3-small', temperature: 0.7, maxTokens: 8192, apiKeyHint: 'console.anthropic.com' },
    deepseek: { label: 'DeepSeek', apiBaseUrl: 'https://api.deepseek.com', model: 'deepseek-chat', embeddingModel: 'deepseek-embedding', temperature: 0.7, maxTokens: 8192, apiKeyHint: 'platform.deepseek.com/api_keys' },
    qwen: { label: '通义千问', apiBaseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', model: 'qwen-plus', embeddingModel: 'text-embedding-v3', temperature: 0.7, maxTokens: 8192, apiKeyHint: 'bailian.console.aliyun.com' },
    custom: { label: 'Custom', apiBaseUrl: '', model: '', embeddingModel: '', temperature: 0.7, maxTokens: 4096, apiKeyHint: '' },
  },
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (k: string) => k }),
}))

const fetchMock = vi.mocked(fetchAiModelConfig)
const saveMock = vi.mocked(saveAiModelConfig)
const sendMock = vi.mocked(sendChatMessage)
const successMock = vi.mocked(ElMessage.success)
const errorMock = vi.mocked(ElMessage.error)
const infoMock = vi.mocked(ElMessage.info)

const PROVIDERS: ProviderInfo[] = [
  { provider: 'openai', model: 'gpt-4o', configured: true, defaultProvider: true },
  { provider: 'claude', model: 'claude-sonnet-4-20250514', configured: false, defaultProvider: false },
]

/** 组装被测对象：注入共享 ref 与 loadProviders 桩 */
function makeApi() {
  const providerList = ref<ProviderInfo[]>([...PROVIDERS])
  const chatProvider = ref<AiProvider>('openai')
  const loadProviders = vi.fn().mockResolvedValue(undefined)
  const api = useAiKnowledgeSettings({ providerList, chatProvider, loadProviders })
  return { api, providerList, chatProvider, loadProviders }
}

beforeEach(() => {
  vi.clearAllMocks()
  fetchMock.mockResolvedValue({
    provider: 'claude', apiKey: 'sk-123', apiBaseUrl: 'https://api.anthropic.com', model: 'claude-sonnet-4-20250514',
    temperature: 0.7, maxTokens: 8192, embeddingModel: 'text-embedding-3-small', defaultProvider: true, maxContextMessages: 5,
  } as never)
  saveMock.mockResolvedValue(undefined as never)
  sendMock.mockResolvedValue(undefined as never)
})

describe('useAiKnowledgeSettings', () => {
  it('buildDefaultConfig 返回指定 provider 的默认配置', () => {
    const { api } = makeApi()
    const cfg = api.buildDefaultConfig('claude')
    expect(cfg.provider).toBe('claude')
    expect(cfg.apiBaseUrl).toBe(AI_PROVIDER_MAP.claude.apiBaseUrl)
    expect(cfg.model).toBe(AI_PROVIDER_MAP.claude.model)
    expect(cfg.embeddingModel).toBe(AI_PROVIDER_MAP.claude.embeddingModel)
    expect(cfg.apiKey).toBe('')
    expect(cfg.defaultProvider).toBe(false)
  })

  it('buildDefaultConfig 缺省为 openai', () => {
    const { api } = makeApi()
    expect(api.buildDefaultConfig().provider).toBe('openai')
  })

  it('loadConfig 成功填充草稿并同步 chatProvider', async () => {
    const { api, chatProvider } = makeApi()

    await api.loadConfig()

    expect(fetchMock).toHaveBeenCalled()
    expect(api.configDraft.value.provider).toBe('claude')
    expect(api.configDraft.value.apiKey).toBe('sk-123')
    expect(api.configDraft.value.maxContextMessages).toBe(5)
    expect(chatProvider.value).toBe('claude')
    expect(api.configLoading.value).toBe(false)
  })

  it('loadConfig 失败时回退默认配置', async () => {
    const { api, chatProvider } = makeApi()
    fetchMock.mockRejectedValue(new Error('boom') as never)

    await api.loadConfig()

    expect(api.configDraft.value.provider).toBe('openai')
    expect(chatProvider.value).toBe('openai')
    expect(api.configLoading.value).toBe(false)
  })

  it('saveConfig 校验不通过时不提交', async () => {
    const { api } = makeApi()
    api.configFormRef.value = { validate: vi.fn().mockRejectedValue(new Error('invalid')) } as never

    await api.saveConfig()

    expect(saveMock).not.toHaveBeenCalled()
    expect(api.savingConfig.value).toBe(false)
  })

  it('saveConfig 成功提交、缓存草稿并刷新 provider 列表', async () => {
    const { api, loadProviders } = makeApi()
    api.configFormRef.value = { validate: vi.fn().mockResolvedValue(true) } as never

    await api.saveConfig()

    expect(saveMock).toHaveBeenCalledWith(api.configDraft.value)
    expect(successMock).toHaveBeenCalledWith('aiKnowledge.settings.saveSuccess')
    expect(loadProviders).toHaveBeenCalled()
    expect(api.savingConfig.value).toBe(false)
  })

  it('saveConfig 失败提示错误', async () => {
    const { api } = makeApi()
    api.configFormRef.value = { validate: vi.fn().mockResolvedValue(true) } as never
    saveMock.mockRejectedValue(new Error('boom') as never)

    await api.saveConfig()

    expect(errorMock).toHaveBeenCalledWith('aiKnowledge.settings.saveFailed')
  })

  it('testConnection 发送探测消息', async () => {
    const { api } = makeApi()

    await api.testConnection()

    expect(sendMock).toHaveBeenCalledWith({ question: 'Hi', provider: 'openai', useRag: false })
    expect(successMock).toHaveBeenCalledWith('aiKnowledge.settings.testSuccess')
    expect(api.testingConnection.value).toBe(false)
  })

  it('testConnection 失败提示错误', async () => {
    const { api } = makeApi()
    sendMock.mockRejectedValue(new Error('boom') as never)

    await api.testConnection()

    expect(errorMock).toHaveBeenCalledWith('aiKnowledge.settings.testFailed')
  })

  it('onProviderChange 无缓存时恢复已保存配置并保留 apiKey 缓存', () => {
    const { api } = makeApi()
    // 模拟 el-select v-model 先更新 provider 再触发 @change 的调用顺序
    api.configDraft.value.provider = 'claude'
    api.onProviderChange('claude')

    expect(api.configDraft.value.provider).toBe('claude')
    expect(api.configDraft.value.model).toBe('claude-sonnet-4-20250514')
    expect(api.configDraft.value.apiBaseUrl).toBe(AI_PROVIDER_MAP.claude.apiBaseUrl)
    expect(api.configDraft.value.temperature).toBe(0.7)
  })

  it('onProviderChange 有缓存草稿时整体恢复', () => {
    const { api } = makeApi()
    // 先切到 claude 产生 openai 缓存，再修改 claude 草稿并切回
    api.onProviderChange('claude')
    api.configDraft.value.apiKey = 'sk-modified'
    api.onProviderChange('openai')
    // 第二次切回 claude 应使用缓存的修改后草稿
    api.onProviderChange('claude')

    expect(api.configDraft.value.apiKey).toBe('sk-modified')
  })

  it('watch configDraft.provider 同步 chatProvider', async () => {
    const { api, chatProvider } = makeApi()

    api.configDraft.value.provider = 'qwen'
    await nextTick()

    expect(chatProvider.value).toBe('qwen')
  })

  it('modelOptions 有 providerList 时按列表映射', () => {
    const { api } = makeApi()
    const opts = api.modelOptions.value
    expect(opts).toHaveLength(2)
    expect(opts[0]).toMatchObject({ key: 'openai', displayModel: 'gpt-4o' })
    expect(opts[1]).toMatchObject({ key: 'claude', displayModel: 'claude-sonnet-4-20250514', configured: false })
  })

  it('modelOptions 无 providerList 时回退 AI_PROVIDER_MAP', () => {
    const { api, providerList } = makeApi()
    providerList.value = []
    const opts = api.modelOptions.value
    expect(opts.length).toBe(Object.keys(AI_PROVIDER_MAP).length)
    expect(opts.some(o => o.key === 'openai')).toBe(true)
  })

  it('providerInfo 计算当前 provider 信息', () => {
    const { api } = makeApi()
    expect(api.providerInfo.value.label).toBe('OpenAI')
  })

  it('providerAvatarUrl 跟随 chatProvider', () => {
    const { api, chatProvider } = makeApi()
    expect(api.providerAvatarUrl.value).toBe('/icons/providers/openai.svg')
    chatProvider.value = 'deepseek'
    expect(api.providerAvatarUrl.value).toBe('/icons/providers/deepseek.svg')
  })

  it('onPlusClick 提示功能待上线', () => {
    const { api } = makeApi()
    api.onPlusClick()
    expect(infoMock).toHaveBeenCalledWith('aiKnowledge.chat.attachComingSoon')
  })

  it('onAvatarError 将非 custom 头像替换为 custom', () => {
    const { api } = makeApi()
    const img = { src: '/icons/providers/openai.svg' } as HTMLImageElement
    api.onAvatarError({ target: img } as unknown as Event)
    expect(img.src).toBe('/icons/providers/custom.svg')
  })

  it('onAvatarError 对已回退的 custom 头像不再处理', () => {
    const { api } = makeApi()
    const img = { src: '/icons/providers/custom.svg' } as HTMLImageElement
    api.onAvatarError({ target: img } as unknown as Event)
    expect(img.src).toBe('/icons/providers/custom.svg')
  })
})
