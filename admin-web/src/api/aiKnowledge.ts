import { getData, postData, putData, deleteData } from './request'
import type { ApiResult } from './types'

// ==================== AI 模型配置 ====================

export type AiProvider = 'openai' | 'claude' | 'deepseek' | 'qwen' | 'custom'

export interface AiProviderInfo {
  label: string
  apiBaseUrl: string
  model: string
  embeddingModel: string
  /** 推荐温度 */
  temperature: number
  /** 推荐最大 Token */
  maxTokens: number
  /** API Key 获取地址提示 */
  apiKeyHint: string
}

export const AI_PROVIDER_MAP: Record<AiProvider, AiProviderInfo> = {
  openai: {
    label: 'OpenAI',
    apiBaseUrl: 'https://api.openai.com/v1',
    model: 'gpt-4o',
    embeddingModel: 'text-embedding-3-small',
    temperature: 0.7,
    maxTokens: 4096,
    apiKeyHint: 'platform.openai.com/api-keys',
  },
  claude: {
    label: 'Claude',
    apiBaseUrl: 'https://api.anthropic.com',
    model: 'claude-sonnet-4-20250514',
    embeddingModel: 'text-embedding-3-small',
    temperature: 0.7,
    maxTokens: 8192,
    apiKeyHint: 'console.anthropic.com',
  },
  deepseek: {
    label: 'DeepSeek',
    apiBaseUrl: 'https://api.deepseek.com',
    model: 'deepseek-chat',
    embeddingModel: 'deepseek-embedding',
    temperature: 0.7,
    maxTokens: 8192,
    apiKeyHint: 'platform.deepseek.com/api_keys',
  },
  qwen: {
    label: '通义千问',
    apiBaseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    model: 'qwen-plus',
    embeddingModel: 'text-embedding-v3',
    temperature: 0.7,
    maxTokens: 8192,
    apiKeyHint: 'bailian.console.aliyun.com',
  },
  custom: {
    label: 'Custom',
    apiBaseUrl: '',
    model: '',
    embeddingModel: '',
    temperature: 0.7,
    maxTokens: 4096,
    apiKeyHint: '',
  },
}

export interface AiModelConfig {
  provider: AiProvider
  apiKey: string
  apiBaseUrl: string
  model: string
  temperature: number
  maxTokens: number
  embeddingModel: string
  defaultProvider?: boolean
  maxContextMessages?: number
}

export function fetchAiModelConfig() {
  return getData<AiModelConfig>('/api/ai-knowledge/config')
}

export function saveAiModelConfig(config: AiModelConfig) {
  return putData<AiModelConfig>('/api/ai-knowledge/config', config)
}

// ==================== 智能问答 ====================

export type MessageRole = 'user' | 'assistant' | 'system'

export interface ChatMessage {
  id: string
  role: MessageRole
  content: string
  timestamp: number
  /** 本次请求消耗的 Token 总数（仅 assistant 消息有） */
  tokens?: number
  /** 回复该条消息的大模型 provider（仅 assistant 消息有，用于展示对应图标） */
  provider?: AiProvider
}

export interface ChatRequest {
  question: string
  provider?: AiProvider
  useRag: boolean
  history?: { role: MessageRole; content: string }[]
}

export interface ChatResponse {
  answer: string
  sources?: RagSource[]
  totalTokens?: number
}

export function sendChatMessage(req: ChatRequest) {
  return postData<ChatResponse>('/api/ai-knowledge/chat', req, { timeout: 120000 })
}

/**
 * 流式聊天 - 通过 SSE 逐块获取 AI 回复
 * @param onChunk 每收到一段文本的回调（参数为已累积的完整文本）
 * @returns AbortController 用于取消请求
 */
export function sendChatMessageStream(
  req: ChatRequest,
  onChunk: (fullText: string) => void,
  onDone?: () => void,
  onError?: (err: string) => void,
  onTokens?: (tokens: number) => void,
): AbortController {
  const controller = new AbortController()

  fetch('/api/ai-knowledge/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
    signal: controller.signal,
  }).then(async (response) => {
    if (!response.ok) {
      onError?.('HTTP ' + response.status)
      return
    }

    const reader = response.body!.getReader()
    const decoder = new TextDecoder()
    let fullText = ''
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      // SSE 事件由 \n\n 分隔（兼容 \r\n\r\n）
      const parts = buffer.split(/\r?\n\r?\n/)
      // 最后一个部分可能是不完整的事件，留到下次处理
      buffer = parts.pop() || ''

      for (const part of parts) {
        const trimmedPart = part.trim()
        if (!trimmedPart) continue

        // 纯文本格式后备（兼容非标准 SSE）
        if (trimmedPart === '[DONE]') continue
        if (trimmedPart.startsWith('[ERROR]')) {
          onError?.(trimmedPart.slice(7).trim())
          continue
        }
        if (trimmedPart.startsWith('[TOKENS]')) {
          const tokens = parseInt(trimmedPart.slice(8).trim(), 10)
          if (!isNaN(tokens)) onTokens?.(tokens)
          continue
        }

        // 收集事件内所有 data: 行
        const lines = part.split(/\r?\n/)
        let eventData = ''
        let dataLineCount = 0

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const d = line.slice(5)
            // SSE 规范：同一事件的多个 data: 行用 \n 连接
            // 不能用 if (eventData) 判断，因为首行 data: 可能是空字符串（代表 \n）
            if (dataLineCount > 0) eventData += '\n'
            eventData += d
            dataLineCount++
          }
        }

        if (!eventData) continue
        if (eventData === '[DONE]') continue
        if (eventData.startsWith('[ERROR]')) {
          onError?.(eventData.slice(7).trim())
          continue
        }
        if (eventData.startsWith('[TOKENS]')) {
          const tokens = parseInt(eventData.slice(8).trim(), 10)
          if (!isNaN(tokens)) onTokens?.(tokens)
          continue
        }

        fullText += eventData
        onChunk(fullText)
      }
    }

    onDone?.()
  }).catch((err) => {
    if (err.name !== 'AbortError') {
      onError?.(err.message)
    }
  })

  return controller
}

// ==================== 提供商列表（来自数据库） ====================

export interface ProviderInfo {
  provider: AiProvider
  model: string
  configured: boolean
  defaultProvider: boolean
  maxContextMessages?: number
  /** 大模型上下文窗口大小（Token），如 65536 表示 64K */
  maxContextTokens?: number
}

export function fetchAiProviders() {
  return getData<ProviderInfo[]>('/api/ai-knowledge/providers')
}

// ==================== RAG 知识库 ====================

export interface RagDocument {
  id: number
  fileName: string
  fileType: string
  fileSize: number
  chunkCount: number
  status: 'pending' | 'processing' | 'ready' | 'failed'
  indexedAt: string | null
  errorMessage: string | null
}

export interface RagStats {
  totalDocs: number
  readyCount: number
  processingCount: number
  failedCount: number
  totalChunks: number
}

export interface RagSource {
  documentId: number
  fileName: string
  chunkIndex: number
  content: string
  score: number
}

export interface RagSearchRequest {
  query: string
  topK: number
}

export interface RagSearchResult {
  sources: RagSource[]
}

export function fetchRagStats() {
  return getData<RagStats>('/api/ai-knowledge/rag/stats')
}

export function fetchRagDocuments() {
  return getData<RagDocument[]>('/api/ai-knowledge/rag/documents')
}

export function retryRagDocument(id: number) {
  return postData<void>(`/api/ai-knowledge/rag/documents/${id}/retry`)
}

export function removeRagDocument(id: number) {
  return deleteData(`/api/ai-knowledge/rag/documents/${id}`)
}

export interface RagUploadResult {
  documentId: number
  fileName: string
  status: string
  message: string
}

// ==================== Embedding 配置（独立于 Chat 配置） ====================

export function fetchEmbeddingConfig() {
  return getData<AiModelConfig>('/api/ai-knowledge/rag/embedding-config')
}

export function saveEmbeddingConfig(config: AiModelConfig) {
  return putData<AiModelConfig>('/api/ai-knowledge/rag/embedding-config', config)
}

export function searchRag(req: RagSearchRequest) {
  return postData<RagSearchResult>('/api/ai-knowledge/rag/search', req)
}

export function rebuildRagIndex() {
  return postData<void>('/api/ai-knowledge/rag/rebuild')
}

/** RAG 文档上传结果（对应后端 AiKnowledgeRagUploadResultVO） */
export interface RagUploadResult {
  documentId: number
  fileName: string
  status: string
  message: string
}

export async function uploadRagDocument(file: File): Promise<RagUploadResult> {
  const formData = new FormData()
  formData.append('file', file)
  const base = import.meta.env.VITE_API_BASE || ''
  const resp = await fetch(`${base}/api/ai-knowledge/rag/upload`, {
    method: 'POST',
    body: formData,
  })
  // 后端统一返回 ApiResult 包装，解包 data 部分并透出业务错误，与 postData 约定一致
  const json = (await resp.json()) as ApiResult<RagUploadResult>
  if (json && typeof json.code === 'number' && json.code !== 0) {
    throw new Error(json.message || '文档上传失败')
  }
  return json.data
}

// ==================== 对话分类 & 对话列表 ==================

export interface ChatCategoryVO {
  id: string
  name: string
  conversations: ChatConversationVO[]
}

export interface ChatConversationVO {
  id: string
  categoryId: string
  title: string
  messages: string
  createdAt: string
  updatedAt: string
}

export function fetchChatCategories() {
  return getData<ChatCategoryVO[]>('/api/ai-knowledge/chat/categories')
}

export function createChatCategory(name: string) {
  return postData<ChatCategoryVO>('/api/ai-knowledge/chat/categories', { name })
}

export function renameChatCategory(id: string, name: string) {
  return putData<void>(`/api/ai-knowledge/chat/categories/${id}`, { name })
}

export function deleteChatCategory(id: string) {
  return deleteData(`/api/ai-knowledge/chat/categories/${id}`)
}

export function createChatConversation(categoryId: string) {
  return postData<ChatConversationVO>(`/api/ai-knowledge/chat/categories/${categoryId}/conversations`)
}

export function updateChatConversation(id: string, data: { title?: string; messages?: string }) {
  return putData<void>(`/api/ai-knowledge/chat/conversations/${id}`, data)
}

export function deleteChatConversation(id: string) {
  return deleteData(`/api/ai-knowledge/chat/conversations/${id}`)
}

export interface ChatSearchResult {
  categoryId: string
  categoryName: string
  conversationId: string
  conversationTitle: string
  matchField: string
  matchSummary: string
}

export function searchChatConversations(keyword: string) {
  return getData<ChatSearchResult[]>('/api/ai-knowledge/chat/search', { keyword })
}

export interface ChatUsageVO {
  remainingBalance: number | null
  totalCost: number
  totalRequests: number
  totalTokens: number
  todayRequests: number
  todayTokens: number
  lastDate: string
}

export function fetchChatUsage() {
  return getData<ChatUsageVO>('/api/ai-knowledge/chat/usage')
}

export function recordChatUsage(data: { tokens: number; cost: number }) {
  return postData<void>('/api/ai-knowledge/chat/usage', data)
}
