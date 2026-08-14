import { nextTick, ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAiKnowledgeChat } from '../useAiKnowledgeChat'
import {
  sendChatMessageStream,
  sendChatMessage,
  fetchChatUsage,
  fetchAiProviders,
} from '@/api/aiKnowledge'
import { ElMessageBox } from 'element-plus'
import type { ChatMessage, ProviderInfo, RagSource } from '@/api/aiKnowledge'

vi.mock('@/api/aiKnowledge', () => ({
  sendChatMessageStream: vi.fn(() => ({ abort: vi.fn() })),
  sendChatMessage: vi.fn(),
  fetchChatUsage: vi.fn(),
  fetchAiProviders: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessageBox: { confirm: vi.fn() },
}))

const streamMock = vi.mocked(sendChatMessageStream)
const sendMock = vi.mocked(sendChatMessage)
const usageMock = vi.mocked(fetchChatUsage)
const providersMock = vi.mocked(fetchAiProviders)
const confirmMock = vi.mocked(ElMessageBox.confirm)

type ChatMsg = ChatMessage & { sources?: RagSource[]; collapsed?: boolean }

/** 构造 composable 及依赖，返回可断言的句柄 */
function setup() {
  const messages = ref<ChatMsg[]>([])
  const providerList = ref<ProviderInfo[]>([])
  const chatProvider = ref<'openai' | 'claude' | 'deepseek' | 'qwen' | 'custom'>('openai')
  const useRag = ref(false)
  const activeConvId = ref<string | null>('conv-1')
  const saveMessages = vi.fn().mockResolvedValue(undefined)
  const api = useAiKnowledgeChat({
    messages, providerList, chatProvider, useRag, activeConvId, saveMessages,
  })
  return { api, messages, providerList, chatProvider, useRag, activeConvId, saveMessages }
}

/** 捕获 sendChatMessageStream 的第 N 次调用回调 */
function lastStreamCall() {
  const call = streamMock.mock.calls.at(-1)!
  return { onChunk: call[1], onDone: call[2], onError: call[3], onTokens: call[4] }
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('useAiKnowledgeChat', () => {
  describe('sendMessage', () => {
    it('追加用户消息与助手占位并触发流式请求', () => {
      const { api, messages, saveMessages } = setup()
      api.question.value = '你好'

      api.sendMessage()

      expect(messages.value).toHaveLength(2)
      expect(messages.value[0].role).toBe('user')
      expect(messages.value[0].content).toBe('你好')
      expect(messages.value[1].role).toBe('assistant')
      expect(messages.value[1].content).toBe('')
      expect(api.question.value).toBe('')
      expect(api.sending.value).toBe(true)
      expect(streamMock).toHaveBeenCalledTimes(1)
      expect(saveMessages).toHaveBeenCalled()
    })

    it('助手消息记录发送时的 provider，供每条回答按回复它的模型展示图标', () => {
      const { api, messages, chatProvider } = setup()
      chatProvider.value = 'deepseek'
      api.question.value = '你好'

      api.sendMessage()

      expect(messages.value[1].role).toBe('assistant')
      expect(messages.value[1].provider).toBe('deepseek')
    })

    it('空文本或发送中不重复发送', () => {
      const { api, messages } = setup()
      api.sendMessage()
      api.sendMessage()
      expect(messages.value).toHaveLength(0)
      expect(streamMock).not.toHaveBeenCalled()

      api.question.value = 'abc'
      api.sending.value = true
      api.sendMessage()
      expect(streamMock).not.toHaveBeenCalled()
    })

    it('流式回调更新内容、token 并结束发送态', () => {
      const { api, messages, saveMessages } = setup()
      api.question.value = '问题'

      api.sendMessage()
      const { onChunk, onDone, onTokens } = lastStreamCall()

      onChunk!('第一段')
      onChunk!('第一段第二段')
      expect(messages.value[1].content).toBe('第一段第二段')

      onTokens!(50)
      expect(api.currentContextTokens.value).toBe(50)

      onDone!()
      expect(api.sending.value).toBe(false)
      expect(saveMessages).toHaveBeenCalled()
    })

    it('流式错误时写入占位错误消息', () => {
      const { api, messages } = setup()
      api.question.value = '问题'

      api.sendMessage()
      const { onError } = lastStreamCall()

      onError!('boom')
      expect(messages.value[1].content).toBe('错误：boom')
      expect(api.sending.value).toBe(false)
    })
  })

  describe('token 与上下文', () => {
    it('recalcContextTokens 累加消息 token', () => {
      const { api, messages } = setup()
      messages.value = [
        { id: '1', role: 'user', content: 'a', timestamp: 1 },
        { id: '2', role: 'assistant', content: 'b', timestamp: 1, tokens: 30 },
        { id: '3', role: 'assistant', content: 'c', timestamp: 1, tokens: 20 },
      ]
      api.recalcContextTokens()
      expect(api.currentContextTokens.value).toBe(50)
    })

    it('memoryLimit 取 provider 配置，缺省 10', () => {
      const { api, providerList } = setup()
      expect(api.memoryLimit.value).toBe(10)
      providerList.value = [{ provider: 'openai', model: 'gpt', configured: true, defaultProvider: true, maxContextMessages: 20 } as ProviderInfo]
      expect(api.memoryLimit.value).toBe(20)
    })

    it('contextPercentage 封顶 100', () => {
      const { api, providerList } = setup()
      providerList.value = [{ provider: 'openai', model: 'gpt', configured: true, defaultProvider: true, maxContextTokens: 100 } as ProviderInfo]
      api.currentContextTokens.value = 150
      expect(api.contextPercentage.value).toBe(100)
      api.currentContextTokens.value = 50
      expect(api.contextPercentage.value).toBe(50)
    })

    it('requestPayload 截断长消息并仅保留最近 N 条', () => {
      const { api, messages, providerList } = setup()
      providerList.value = [{ provider: 'openai', model: 'gpt', configured: true, defaultProvider: true, maxContextMessages: 1 } as ProviderInfo]
      messages.value = [
        { id: '1', role: 'user', content: '短', timestamp: 1 },
        { id: '2', role: 'user', content: '长'.repeat(500), timestamp: 1 },
      ]
      const payload = api.requestPayload.value
      expect(payload).not.toBeNull()
      expect(payload!.history).toHaveLength(1)
      expect(payload!.history[0].content.endsWith('...')).toBe(true)
    })
  })

  describe('清空与压缩', () => {
    it('确认后清空消息并同步', async () => {
      const { api, messages, saveMessages } = setup()
      messages.value = [{ id: '1', role: 'user', content: 'a', timestamp: 1 }]
      confirmMock.mockResolvedValue('confirm' as never)

      await api.clearConversation()

      expect(messages.value).toHaveLength(0)
      expect(saveMessages).toHaveBeenCalled()
    })

    it('取消清空不清空消息', async () => {
      const { api, messages, saveMessages } = setup()
      messages.value = [{ id: '1', role: 'user', content: 'a', timestamp: 1 }]
      confirmMock.mockRejectedValue(new Error('cancel'))

      await api.clearConversation()

      expect(messages.value).toHaveLength(1)
      expect(saveMessages).not.toHaveBeenCalled()
    })

    it('无激活会话时不清空', async () => {
      const { api, activeConvId } = setup()
      activeConvId.value = null
      await api.clearConversation()
      expect(confirmMock).not.toHaveBeenCalled()
    })

    it('compressConversation 以摘要替换历史并保留最近一轮', async () => {
      const { api, messages, saveMessages } = setup()
      messages.value = [
        { id: '1', role: 'user', content: 'Q1', timestamp: 1 },
        { id: '2', role: 'assistant', content: 'A1', timestamp: 1 },
        { id: '3', role: 'user', content: 'Q2', timestamp: 1 },
        { id: '4', role: 'assistant', content: 'A2', timestamp: 1 },
      ]
      sendMock.mockResolvedValue({ answer: '摘要内容' } as never)

      await api.compressConversation()

      expect(sendMock).toHaveBeenCalled()
      expect(messages.value[0].role).toBe('system')
      expect(messages.value[0].content).toContain('摘要内容')
      expect(messages.value.slice(1)).toHaveLength(2)
      expect(messages.value.slice(1)[0].content).toBe('Q2')
      expect(saveMessages).toHaveBeenCalled()
    })
  })

  describe('provider 与工具', () => {
    it('loadProviders 刷新列表并切换不在列表中的当前 provider', async () => {
      const { api, chatProvider, providerList } = setup()
      chatProvider.value = 'claude'
      providersMock.mockResolvedValue([
        { provider: 'openai', model: 'gpt', configured: true, defaultProvider: true } as ProviderInfo,
      ] as never)

      await api.loadProviders()

      expect(providerList.value).toHaveLength(1)
      expect(chatProvider.value).toBe('openai')
    })

    it('genMsgId 每次返回不同 id', () => {
      const { api } = setup()
      expect(api.genMsgId()).not.toBe(api.genMsgId())
    })

    it('loadChatUsage 拉取用量', async () => {
      const { api } = setup()
      usageMock.mockResolvedValue({ totalTokens: 1, totalCalls: 2, totalDays: 3 } as never)
      await api.loadChatUsage()
      expect(api.chatUsage.value).toEqual({ totalTokens: 1, totalCalls: 2, totalDays: 3 })
    })
  })

  describe('流式输出滚动跟随', () => {
    /** 构造一个可模拟滚动位置的消息容器元素（保持可变类型以便测试内赋值 scrollHeight） */
    function mockContainer(overrides: Partial<{ scrollTop: number; clientHeight: number; scrollHeight: number }> = {}) {
      return {
        scrollTop: 0,
        clientHeight: 400,
        scrollHeight: 1200,
        ...overrides,
      }
    }

    it('在底部时 scrollChatToBottom 滚动到容器底部', async () => {
      const { api } = setup()
      const el = mockContainer()
      api.chatMessagesRef.value = el as unknown as HTMLElement

      api.scrollChatToBottom()
      await nextTick()
      expect(el.scrollTop).toBe(1200)
    })

    it('用户上滑离开底部后暂停跟随，滚回底部自动恢复', async () => {
      const { api } = setup()
      const el = mockContainer({ scrollTop: 200 })
      api.chatMessagesRef.value = el as unknown as HTMLElement

      // 用户上滑到中部（scrollTop=200，未贴底）→ 暂停跟随
      api.onChatScroll()
      el.scrollHeight = 1400
      api.scrollChatToBottom()
      await nextTick()
      expect(el.scrollTop).toBe(200) // 未被动拉到底

      // 用户滚回底部 → 恢复跟随，新内容继续自动滚动
      el.scrollTop = el.scrollHeight - el.clientHeight
      api.onChatScroll()
      el.scrollHeight = 1600
      api.scrollChatToBottom()
      await nextTick()
      expect(el.scrollTop).toBe(1600)
    })

    it('force=true 无视暂停状态强制滚动并恢复跟随', async () => {
      const { api } = setup()
      const el = mockContainer({ scrollTop: 200 })
      api.chatMessagesRef.value = el as unknown as HTMLElement
      api.onChatScroll() // 用户上滑 → 暂停

      el.scrollHeight = 1600
      api.scrollChatToBottom(true)
      await nextTick()
      expect(el.scrollTop).toBe(1600)

      // 之后不再依赖用户滚动也能跟随
      el.scrollTop = 100
      el.scrollHeight = 2000
      api.scrollChatToBottom()
      await nextTick()
      expect(el.scrollTop).toBe(2000)
    })

    it('发送新问题时上滑状态不强制回底，流式输出同样保持当前位置', async () => {
      const { api } = setup()
      const el = mockContainer({ scrollTop: 200 })
      api.chatMessagesRef.value = el as unknown as HTMLElement
      api.onChatScroll() // 用户上滑 → 暂停跟随
      api.question.value = '新问题'

      api.sendMessage()
      await nextTick()
      expect(el.scrollTop).toBe(200) // 发送后未被拉到底

      // 跟随仍为暂停：后续 chunk 也不滚动
      el.scrollHeight = 1600
      const { onChunk } = lastStreamCall()
      onChunk!('流式内容')
      await nextTick()
      expect(el.scrollTop).toBe(200)
    })

    it('流式输出中用户上滑后，后续 chunk 不再强制拉到底部', async () => {
      const { api } = setup()
      const el = mockContainer({ scrollTop: 200 })
      api.chatMessagesRef.value = el as unknown as HTMLElement
      api.question.value = '问题'
      api.sendMessage()
      const { onChunk } = lastStreamCall()

      // 流式过程中用户上滑到中部 → 暂停跟随，chunk 不强制滚动
      api.onChatScroll()
      el.scrollHeight = 1400
      onChunk!('第一段')
      await nextTick()
      expect(el.scrollTop).toBe(200)

      // 滚回底部后，后续 chunk 恢复跟随
      el.scrollTop = el.scrollHeight - el.clientHeight
      api.onChatScroll()
      el.scrollHeight = 1600
      onChunk!('第二段')
      await nextTick()
      expect(el.scrollTop).toBe(1600)
    })
  })
})
