import { AI_PROVIDER_MAP } from '@/api/aiKnowledge'
import type { AiProvider, ChatBookmark, ChatCategoryVO } from '@/api/aiKnowledge'

/** 聊天模型选择的 localStorage 持久化 key（与 PC 端 AiKnowledgeView 同 key，两端模型记忆互通） */
export const CHAT_PROVIDER_STORAGE_KEY = 'ai-knowledge.chatProvider'

/**
 * 从 localStorage 恢复上次使用的大模型，非法值回落默认 openai。
 * 与 PC 端同 key 同判定逻辑，保证手机端与桌面端看到的是同一个模型选择。
 */
export function readChatProviderFromStorage(): AiProvider {
  try {
    const raw = localStorage.getItem(CHAT_PROVIDER_STORAGE_KEY)
    if (raw && raw in AI_PROVIDER_MAP) {
      return raw as AiProvider
    }
  } catch {
    // localStorage 不可用（如隐私模式）时忽略，回落默认
  }
  return 'openai'
}

/** 持久化当前模型选择；存储不可用时静默忽略，仅影响刷新后的模型记忆 */
export function persistChatProvider(provider: AiProvider): void {
  try {
    localStorage.setItem(CHAT_PROVIDER_STORAGE_KEY, provider)
  } catch {
    // 存储不可用时忽略
  }
}

/** 按名称过滤标记（大小写不敏感），空关键词返回全量 */
export function filterMarkersByName(markers: ChatBookmark[], keyword: string): ChatBookmark[] {
  const kw = keyword.trim().toLowerCase()
  if (!kw) return markers
  return markers.filter((m) => m.name.toLowerCase().includes(kw))
}

/**
 * 取当前激活会话标题：从分类/会话树中查找；未找到（如删除中的间隙）返回空串，由调用方回落。
 */
export function activeConversationTitle(
  categories: ChatCategoryVO[],
  catId: string | null,
  convId: string | null,
): string {
  if (!catId || !convId) return ''
  const cat = categories.find((c) => c.id === catId)
  const conv = cat?.conversations.find((c) => c.id === convId)
  return conv?.title ?? ''
}
