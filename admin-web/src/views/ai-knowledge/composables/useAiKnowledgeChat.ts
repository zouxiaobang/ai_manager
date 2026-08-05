import { computed, nextTick, ref } from 'vue'
import type { Ref } from 'vue'
import { marked } from 'marked'
import { ElMessageBox } from 'element-plus'
import {
  sendChatMessageStream,
  sendChatMessage,
  fetchChatUsage,
  fetchAiProviders,
} from '@/api/aiKnowledge'
import type {
  AiProvider,
  ChatMessage,
  ChatUsageVO,
  ProviderInfo,
  RagSource,
} from '@/api/aiKnowledge'

/** useAiKnowledgeChat 依赖注入：聊天状态机与分类/会话/持久化解耦，由调用方注入 */
export interface AiKnowledgeChatDeps {
  /** 消息列表（调用方持有，便于与分类/会话切换联动） */
  messages: Ref<(ChatMessage & { sources?: RagSource[]; collapsed?: boolean })[]>
  /** AI provider 列表 */
  providerList: Ref<ProviderInfo[]>
  /** 当前选中的 provider */
  chatProvider: Ref<AiProvider>
  /** 是否启用 RAG */
  useRag: Ref<boolean>
  /** 当前会话 id（清空对话时校验） */
  activeConvId: Ref<string | null>
  /** 消息变更后持久化到当前会话（调用方结合分类状态实现） */
  saveMessages: () => Promise<void>
}

/**
 * AI 知识页聊天状态机
 *
 * <p>从 {@code AiKnowledgeView.vue} 提取的对话核心：提问、流式发送、消息渲染、token 上下文统计、
 * 滚动定位、清空与压缩对话。消息列表由调用方持有（与分类/会话联动），其余聊天专属状态内聚于此，
 * 可独立于组件单元测试。</p>
 */
export function useAiKnowledgeChat(deps: AiKnowledgeChatDeps) {
  const {
    messages,
    providerList,
    chatProvider,
    useRag,
    activeConvId,
    saveMessages,
  } = deps

  const question = ref('')
  const sending = ref(false)
  const chatMessagesRef = ref<HTMLElement | null>(null)
  const providersLoading = ref(false)

  // ========== 调试面板 ==========
  const showDebugPanel = ref(false)
  const debugReqExpanded = ref(false)
  const chatUsage = ref<ChatUsageVO | null>(null)

  /** 当前会话累计消耗的 Token 数（从 API 返回的真实数据），用于计算上下文占用百分比 */
  const currentContextTokens = ref(0)

  let msgCounter = 0

  /** 从已加载的消息中重新计算当前会话的 Token 消耗 */
  function recalcContextTokens() {
    let total = 0
    for (const m of messages.value) {
      if (m.tokens) total += m.tokens
    }
    currentContextTokens.value = total
  }

  /** 用户消息列表（用于右侧锚点导航） */
  const userAnchorMessages = computed(() => messages.value.filter(m => m.role === 'user'))

  const memoryLimit = computed(() => {
    const info = providerList.value.find(p => p.provider === chatProvider.value)
    return info?.maxContextMessages ?? 10
  })

  /** 当前提供商的大模型上下文窗口大小 */
  const maxContextWindow = computed(() => {
    const info = providerList.value.find(p => p.provider === chatProvider.value)
    return info?.maxContextTokens ?? 0
  })

  /** 上下文占用百分比 */
  const contextPercentage = computed(() => {
    if (!maxContextWindow.value || !currentContextTokens.value) return 0
    return Math.min(100, Math.round((currentContextTokens.value / maxContextWindow.value) * 100))
  })

  const requestPayload = computed(() => {
    if (messages.value.length === 0) return null
    const memLimit = memoryLimit.value
    const history = messages.value.slice(-memLimit).map(m => ({
      role: m.role,
      content: m.content.length > 300 ? m.content.slice(0, 300) + '...' : m.content,
    }))
    return { history }
  })

  /** AI provider 列表加载：刷新后若当前 provider 不在列表则切到第一个 */
  async function loadProviders() {
    if (providersLoading.value) return
    providersLoading.value = true
    try {
      const data = await fetchAiProviders()
      providerList.value = data
      if (data.length > 0 && !data.some(p => p.provider === chatProvider.value)) {
        chatProvider.value = data[0].provider
      }
    } catch {
      // 加载 providerList 失败，使用 fallback 数据
    } finally {
      providersLoading.value = false
    }
  }

  async function loadChatUsage() {
    try {
      chatUsage.value = await fetchChatUsage()
    } catch { /* ignore */ }
  }

  /** 滚动到指定消息 */
  function scrollToMsg(msgId: string) {
    const el = chatMessagesRef.value?.querySelector(`[data-msg-id="${msgId}"]`)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }

  function scrollChatToBottom() {
    void nextTick(() => {
      const el = chatMessagesRef.value
      if (el) el.scrollTop = el.scrollHeight
    })
  }

  function genMsgId() {
    return `msg_${Date.now()}_${++msgCounter}`
  }

  /** 将消息 Markdown 渲染为 HTML（聊天气泡内联渲染） */
  function renderMessage(msg: ChatMessage): string {
    let html = marked.parse(msg.content) as string
    // 正则匹配纯 emoji 开头的段落，添加特殊类名
    html = html.replace(
      /<p>([\u{1F300}-\u{1FAD6}\u{2600}-\u{27BF}\u{2700}-\u{27BF}]\s)/gu,
      '<p class="ak-emoji-heading">$1',
    )
    return html
  }

  /** 发送用户问题：追加消息 → 预创建助手占位 → SSE 流式接收 */
  function sendMessage() {
    const text = question.value.trim()
    if (!text || sending.value) return

    const userMsg: ChatMessage = {
      id: genMsgId(),
      role: 'user',
      content: text,
      timestamp: Date.now(),
    }
    messages.value.push(userMsg)
    void saveMessages()
    question.value = ''
    sending.value = true
    scrollChatToBottom()

    // 预创建助手消息占位（记录回复它的 provider，用于展示该条消息的大模型图标）
    const assistantMsg: ChatMessage & { sources?: RagSource[] } = {
      id: genMsgId(),
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
      provider: chatProvider.value,
    }
    messages.value.push(assistantMsg)
    scrollChatToBottom()

    sendChatMessageStream(
      {
        question: text,
        provider: chatProvider.value,
        useRag: useRag.value,
        history: messages.value.slice(-memoryLimit.value).map(m => ({ role: m.role, content: m.content })),
      },
      (fullText) => {
        assistantMsg.content = fullText
        messages.value = [...messages.value]
        scrollChatToBottom()
      },
      () => {
        sending.value = false
        void saveMessages()
        scrollChatToBottom()
        void loadChatUsage()
      },
      (err) => {
        if (!assistantMsg.content) {
          assistantMsg.content = '错误：' + err
        }
        sending.value = false
        void saveMessages()
        scrollChatToBottom()
        void loadChatUsage()
      },
      (tokens) => {
        assistantMsg.tokens = tokens
        currentContextTokens.value += tokens
        messages.value = [...messages.value]
      },
    )
  }

  /** 清空当前对话（确认后清空消息并同步后端） */
  async function clearConversation() {
    if (!activeConvId.value) return
    try {
      await ElMessageBox.confirm('确定要清空当前对话的所有消息吗？此操作不可撤销。', '清空对话', {
        confirmButtonText: '清空',
        cancelButtonText: '取消',
        type: 'warning',
      })
      messages.value = []
      currentContextTokens.value = 0
      await saveMessages()
    } catch {
      // 用户取消不做任何事
    }
  }

  const compressing = ref(false)

  /** 压缩对话 — 调用 LLM 对历史做摘要，保留最近一轮问答完整 */
  async function compressConversation() {
    if (messages.value.length <= 2 || compressing.value) return
    compressing.value = true
    try {
      const KEEP = 2
      const toCompress = messages.value.slice(0, -KEEP)
      const recent = messages.value.slice(-KEEP)

      // 构造摘要提示
      const historyText = toCompress
        .map(m => (m.role === 'user' ? '用户' : m.role === 'assistant' ? 'AI' : '系统') + '：' + m.content)
        .join('\n\n---\n\n')

      const res = await sendChatMessage({
        question: `请用中文简洁总结以下对话历史的核心内容、已解决的问题和待办事项，保留所有关键事实和决策信息，以便后续继续对话时不需要重复询问。\n\n${historyText}`,
        provider: chatProvider.value,
        useRag: false,
      })

      // 替换为一条 system 摘要 + 最近一轮问答
      messages.value = [
        { id: genMsgId(), role: 'system', content: '📋 对话摘要：\n' + res.answer, timestamp: Date.now() } as ChatMessage,
        ...recent,
      ]
      recalcContextTokens()
      await saveMessages()
    } catch (e) {
      console.warn('对话压缩失败', e)
    } finally {
      compressing.value = false
    }
  }

  return {
    question,
    sending,
    chatMessagesRef,
    providersLoading,
    showDebugPanel,
    debugReqExpanded,
    chatUsage,
    currentContextTokens,
    compressing,
    userAnchorMessages,
    memoryLimit,
    maxContextWindow,
    contextPercentage,
    requestPayload,
    loadProviders,
    loadChatUsage,
    recalcContextTokens,
    scrollToMsg,
    scrollChatToBottom,
    genMsgId,
    renderMessage,
    sendMessage,
    clearConversation,
    compressConversation,
  }
}
