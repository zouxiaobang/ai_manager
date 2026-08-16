import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ChatBookmark, ChatCategoryVO } from '@/api/aiKnowledge'
import {
  CHAT_PROVIDER_STORAGE_KEY,
  activeConversationTitle,
  filterMarkersByName,
  persistChatProvider,
  readChatProviderFromStorage,
} from '../mobileAiKnowledge'

// 与真实 AI_PROVIDER_MAP 结构一致的精简版：仅用于校验 provider 合法性判定
vi.mock('@/api/aiKnowledge', () => ({
  AI_PROVIDER_MAP: {
    openai: { label: 'OpenAI', model: 'gpt-4o', apiBaseUrl: '', temperature: 1, maxTokens: 4096, embeddingModel: 'text-embedding-3-small' },
    deepseek: { label: 'DeepSeek', model: 'deepseek-chat', apiBaseUrl: '', temperature: 1, maxTokens: 8192, embeddingModel: '' },
  },
}))

describe('readChatProviderFromStorage', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('返回已存储的合法 provider', () => {
    localStorage.setItem(CHAT_PROVIDER_STORAGE_KEY, 'deepseek')
    expect(readChatProviderFromStorage()).toBe('deepseek')
  })

  it('非法值回落默认 openai', () => {
    localStorage.setItem(CHAT_PROVIDER_STORAGE_KEY, 'not-a-provider')
    expect(readChatProviderFromStorage()).toBe('openai')
  })

  it('无存储值时返回默认 openai', () => {
    expect(readChatProviderFromStorage()).toBe('openai')
  })

  it('localStorage 读取异常时不抛错并回落默认', () => {
    const spy = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked')
    })
    expect(readChatProviderFromStorage()).toBe('openai')
    spy.mockRestore()
  })
})

describe('persistChatProvider', () => {
  it('写入 localStorage 持久化模型选择', () => {
    const spy = vi.spyOn(Storage.prototype, 'setItem')
    persistChatProvider('deepseek')
    expect(spy).toHaveBeenCalledWith(CHAT_PROVIDER_STORAGE_KEY, 'deepseek')
    spy.mockRestore()
  })

  it('存储异常（如配额满）时不抛错', () => {
    const spy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('quota exceeded')
    })
    expect(() => persistChatProvider('openai')).not.toThrow()
    spy.mockRestore()
  })
})

describe('filterMarkersByName', () => {
  const markers: ChatBookmark[] = [
    { id: '1', conversationId: 'c', name: 'Alpha 标记', msgId: null, msgOffsetTop: 0, scrollTop: 0, createdAt: '' },
    { id: '2', conversationId: 'c', name: 'beta', msgId: null, msgOffsetTop: 0, scrollTop: 0, createdAt: '' },
  ]

  it('空关键词返回原数组（不做拷贝）', () => {
    expect(filterMarkersByName(markers, '')).toBe(markers)
    expect(filterMarkersByName(markers, '   ')).toBe(markers)
  })

  it('按名称大小写不敏感过滤并 trim 关键词', () => {
    const r = filterMarkersByName(markers, '  ALPHA ')
    expect(r).toHaveLength(1)
    expect(r[0].id).toBe('1')
  })

  it('无匹配返回空数组', () => {
    expect(filterMarkersByName(markers, 'zzz')).toEqual([])
  })
})

describe('activeConversationTitle', () => {
  const categories: ChatCategoryVO[] = [
    {
      id: 'c1',
      name: '工作',
      conversations: [
        { id: 'v1', categoryId: 'c1', title: '方案讨论', messages: '[]', createdAt: '', updatedAt: '' },
        { id: 'v2', categoryId: 'c1', title: '', messages: '[]', createdAt: '', updatedAt: '' },
      ],
    },
  ]

  it('命中返回会话标题', () => {
    expect(activeConversationTitle(categories, 'c1', 'v1')).toBe('方案讨论')
  })

  it('catId / convId 缺失返回空串', () => {
    expect(activeConversationTitle(categories, null, 'v1')).toBe('')
    expect(activeConversationTitle(categories, 'c1', null)).toBe('')
  })

  it('分类不存在返回空串', () => {
    expect(activeConversationTitle(categories, 'nope', 'v1')).toBe('')
  })

  it('会话不存在返回空串', () => {
    expect(activeConversationTitle(categories, 'c1', 'nope')).toBe('')
  })

  it('会话标题为空返回空串（由调用方回落「新对话」）', () => {
    expect(activeConversationTitle(categories, 'c1', 'v2')).toBe('')
  })
})
