import { nextTick, ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAiKnowledgeCategories } from '../useAiKnowledgeCategories'
import {
  fetchChatCategories,
  createChatCategory,
  renameChatCategory,
  deleteChatCategory,
  createChatConversation,
  updateChatConversation,
  deleteChatConversation,
  searchChatConversations,
} from '@/api/aiKnowledge'
import { ElMessageBox } from 'element-plus'
import type {
  ChatCategoryVO,
  ChatConversationVO,
  ChatMessage,
  ChatSearchResult,
  RagSource,
} from '@/api/aiKnowledge'

vi.mock('@/api/aiKnowledge', () => ({
  fetchChatCategories: vi.fn(),
  createChatCategory: vi.fn(),
  renameChatCategory: vi.fn(),
  deleteChatCategory: vi.fn(),
  createChatConversation: vi.fn(),
  updateChatConversation: vi.fn(),
  deleteChatConversation: vi.fn(),
  searchChatConversations: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessageBox: { prompt: vi.fn(), confirm: vi.fn() },
}))

const fetchCategoriesMock = vi.mocked(fetchChatCategories)
const createCategoryMock = vi.mocked(createChatCategory)
const renameCategoryMock = vi.mocked(renameChatCategory)
const deleteCategoryMock = vi.mocked(deleteChatCategory)
const createConvMock = vi.mocked(createChatConversation)
const updateConvMock = vi.mocked(updateChatConversation)
const deleteConvMock = vi.mocked(deleteChatConversation)
const searchMock = vi.mocked(searchChatConversations)
const promptMock = vi.mocked(ElMessageBox.prompt)
const confirmMock = vi.mocked(ElMessageBox.confirm)

type ChatMsg = ChatMessage & { sources?: RagSource[]; collapsed?: boolean }

function makeConv(id: string, title = '', msgs: unknown[] = []): ChatConversationVO {
  return { id, categoryId: '', title, messages: JSON.stringify(msgs), createdAt: '', updatedAt: '' }
}
function makeCat(id: string, name = '', convs: ChatConversationVO[] = []): ChatCategoryVO {
  return { id, name, conversations: convs }
}
function makeSearchResult(categoryId: string, conversationId: string): ChatSearchResult {
  return { categoryId, categoryName: '', conversationId, conversationTitle: '', matchField: '', matchSummary: '' }
}

/** 构造 composable 及依赖，返回可断言的句柄 */
function setup() {
  const messages = ref<ChatMsg[]>([])
  const sending = ref(false)
  const currentContextTokens = ref(0)
  const recalcContextTokens = vi.fn()
  const scrollToMsg = vi.fn()
  const activeConvId = ref<string | null>(null)
  const api = useAiKnowledgeCategories({
    messages, sending, currentContextTokens, recalcContextTokens, scrollToMsg, activeConvId,
  })
  return { api, messages, sending, currentContextTokens, recalcContextTokens, scrollToMsg, activeConvId }
}

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
  createCategoryMock.mockResolvedValue(makeCat('cat-default', '默认分类') as never)
  createConvMock.mockResolvedValue(makeConv('conv-default') as never)
  deleteCategoryMock.mockResolvedValue(undefined as never)
  deleteConvMock.mockResolvedValue(undefined as never)
  updateConvMock.mockResolvedValue(undefined as never)
  renameCategoryMock.mockResolvedValue(undefined as never)
  searchMock.mockResolvedValue([] as never)
})

describe('useAiKnowledgeCategories', () => {
  describe('loadCategoriesFromServer', () => {
    it('无数据时创建默认分类和对话', async () => {
      const { api } = setup()
      fetchCategoriesMock.mockResolvedValue([] as never)

      await api.loadCategoriesFromServer()

      expect(createCategoryMock).toHaveBeenCalledWith('默认分类')
      expect(createConvMock).toHaveBeenCalledWith('cat-default')
      expect(api.categories.value).toHaveLength(1)
      expect(api.activeCatId.value).toBe('cat-default')
      expect(api.activeConvId.value).toBe('conv-default')
    })

    it('有上次激活会话时恢复该会话消息', async () => {
      const { api, messages, recalcContextTokens } = setup()
      const conv = makeConv('conv-1', '', [{ id: 'm1', role: 'user', content: 'hi' }])
      fetchCategoriesMock.mockResolvedValue([makeCat('cat-1', '分类1', [conv])] as never)
      localStorage.setItem('ak-active-session', JSON.stringify({ catId: 'cat-1', convId: 'conv-1' }))

      await api.loadCategoriesFromServer()

      expect(api.activeCatId.value).toBe('cat-1')
      expect(api.activeConvId.value).toBe('conv-1')
      expect(messages.value).toHaveLength(1)
      expect(recalcContextTokens).toHaveBeenCalled()
    })

    it('无上次会话时默认选中第一个对话', async () => {
      const { api, activeConvId } = setup()
      const conv = makeConv('conv-1')
      fetchCategoriesMock.mockResolvedValue([makeCat('cat-1', '分类1', [conv])] as never)

      await api.loadCategoriesFromServer()

      expect(api.activeCatId.value).toBe('cat-1')
      expect(activeConvId.value).toBe('conv-1')
    })

    it('分类下无对话时自动创建', async () => {
      const { api } = setup()
      fetchCategoriesMock.mockResolvedValue([makeCat('cat-1', '分类1')] as never)
      createConvMock.mockResolvedValue(makeConv('conv-auto') as never)

      await api.loadCategoriesFromServer()

      expect(createConvMock).toHaveBeenCalledWith('cat-1')
      expect(api.activeConvId.value).toBe('conv-auto')
    })

    it('加载中重复调用被跳过', async () => {
      const { api } = setup()
      let resolveFetch!: (v: unknown) => void
      fetchCategoriesMock.mockReturnValue(new Promise(r => { resolveFetch = r }) as never)

      const p1 = api.loadCategoriesFromServer()
      const p2 = api.loadCategoriesFromServer()
      resolveFetch([])
      await Promise.all([p1, p2])

      expect(fetchCategoriesMock).toHaveBeenCalledTimes(1)
    })
  })

  describe('switchConversation', () => {
    it('发送中跳过切换', async () => {
      const { api, sending, activeConvId } = setup()
      api.categories.value = [makeCat('cat-1', '分类1', [makeConv('conv-1')])]
      sending.value = true

      await api.switchConversation('cat-1', 'conv-1')

      expect(activeConvId.value).toBeNull()
      expect(updateConvMock).not.toHaveBeenCalled()
    })

    it('正常切换加载目标会话消息并记住激活', async () => {
      const { api, messages, activeConvId } = setup()
      const conv = makeConv('conv-1', '', [{ id: 'm1', role: 'user', content: 'hi' }])
      api.categories.value = [makeCat('cat-1', '分类1', [conv])]

      await api.switchConversation('cat-1', 'conv-1')

      expect(api.activeCatId.value).toBe('cat-1')
      expect(activeConvId.value).toBe('conv-1')
      expect(messages.value).toHaveLength(1)
      expect(JSON.parse(localStorage.getItem('ak-active-session')!)).toEqual({ catId: 'cat-1', convId: 'conv-1' })
    })

    it('消息 JSON 解析失败时置空消息', async () => {
      const { api, messages } = setup()
      const conv: ChatConversationVO = { id: 'conv-1', categoryId: '', title: '', messages: 'not-json', createdAt: '', updatedAt: '' }
      api.categories.value = [makeCat('cat-1', '分类1', [conv])]

      await api.switchConversation('cat-1', 'conv-1')

      expect(messages.value).toHaveLength(0)
    })
  })

  describe('addConversation', () => {
    it('发送中跳过创建', async () => {
      const { api, sending } = setup()
      api.categories.value = [makeCat('cat-1', '分类1')]
      sending.value = true

      await api.addConversation('cat-1')

      expect(createConvMock).not.toHaveBeenCalled()
    })

    it('创建对话并激活，清空消息与 token', async () => {
      const { api, messages, currentContextTokens, activeConvId } = setup()
      api.categories.value = [makeCat('cat-1', '分类1')]
      messages.value = [{ id: 'm1', role: 'user', content: 'hi', timestamp: 1 }]
      currentContextTokens.value = 123
      createConvMock.mockResolvedValue(makeConv('conv-2') as never)

      await api.addConversation('cat-1')

      expect(createConvMock).toHaveBeenCalledWith('cat-1')
      expect(api.activeCatId.value).toBe('cat-1')
      expect(activeConvId.value).toBe('conv-2')
      expect(messages.value).toHaveLength(0)
      expect(currentContextTokens.value).toBe(0)
      expect(api.categoryExpanded.value['cat-1']).toBe(true)
      expect(JSON.parse(localStorage.getItem('ak-active-session')!)).toEqual({ catId: 'cat-1', convId: 'conv-2' })
    })
  })

  describe('deleteConversation', () => {
    it('发送中跳过删除', async () => {
      const { api, sending } = setup()
      api.categories.value = [makeCat('cat-1', '分类1', [makeConv('conv-1')])]
      sending.value = true

      await api.deleteConversation('cat-1', 'conv-1')

      expect(deleteConvMock).not.toHaveBeenCalled()
    })

    it('确认后删除并切换到分类下第一个对话', async () => {
      const { api, activeConvId } = setup()
      const conv1 = makeConv('conv-1', '', [{ id: 'm1', role: 'user', content: 'hi' }])
      const conv2 = makeConv('conv-2')
      api.categories.value = [makeCat('cat-1', '分类1', [conv1, conv2])]
      api.activeCatId.value = 'cat-1'
      activeConvId.value = 'conv-1'
      confirmMock.mockResolvedValue('confirm' as never)

      await api.deleteConversation('cat-1', 'conv-1')

      expect(deleteConvMock).toHaveBeenCalledWith('conv-1')
      expect(api.categories.value[0].conversations).toHaveLength(1)
      expect(activeConvId.value).toBe('conv-2')
    })

    it('取消时不删除', async () => {
      const { api, activeConvId } = setup()
      api.categories.value = [makeCat('cat-1', '分类1', [makeConv('conv-1')])]
      api.activeCatId.value = 'cat-1'
      activeConvId.value = 'conv-1'
      confirmMock.mockRejectedValue(new Error('cancel'))

      await api.deleteConversation('cat-1', 'conv-1')

      expect(deleteConvMock).not.toHaveBeenCalled()
      expect(api.categories.value[0].conversations).toHaveLength(1)
    })
  })

  describe('deleteCategory', () => {
    it('确认后删除分类', async () => {
      const { api, activeConvId } = setup()
      api.categories.value = [
        makeCat('cat-1', '分类1', [makeConv('conv-1')]),
        makeCat('cat-2', '分类2', [makeConv('conv-2')]),
      ]
      api.activeCatId.value = 'cat-2'
      activeConvId.value = 'conv-2'
      confirmMock.mockResolvedValue('confirm' as never)

      await api.deleteCategory('cat-2')

      expect(deleteCategoryMock).toHaveBeenCalledWith('cat-2')
      expect(api.categories.value).toHaveLength(1)
      expect(api.activeCatId.value).toBe('cat-1')
      expect(activeConvId.value).toBe('conv-1')
    })

    it('分类全删后创建默认数据', async () => {
      const { api, activeConvId } = setup()
      api.categories.value = [makeCat('cat-1', '分类1', [makeConv('conv-1')])]
      api.activeCatId.value = 'cat-1'
      activeConvId.value = 'conv-1'
      confirmMock.mockResolvedValue('confirm' as never)

      await api.deleteCategory('cat-1')

      expect(createCategoryMock).toHaveBeenCalledWith('默认分类')
      expect(api.activeConvId.value).toBe('conv-default')
    })
  })

  describe('分类增改', () => {
    it('addCategory 弹窗输入后创建分类', async () => {
      const { api } = setup()
      promptMock.mockResolvedValue({ value: '新分类' } as never)
      createCategoryMock.mockResolvedValue(makeCat('cat-new', '新分类') as never)

      await api.addCategory()

      expect(createCategoryMock).toHaveBeenCalledWith('新分类')
      expect(api.categories.value).toHaveLength(1)
      expect(api.categories.value[0].name).toBe('新分类')
      expect(api.categoryExpanded.value['cat-new']).toBe(true)
    })

    it('confirmRenameCategory 更新本地名称并退出编辑态', async () => {
      const { api } = setup()
      api.categories.value = [makeCat('cat-1', '旧名')]
      api.catRenameInput.value = '新名'

      await api.confirmRenameCategory('cat-1')

      expect(renameCategoryMock).toHaveBeenCalledWith('cat-1', '新名')
      expect(api.categories.value[0].name).toBe('新名')
      expect(api.renamingCatId.value).toBeNull()
    })

    it('confirmRenameConversation 更新本地标题并退出编辑态', async () => {
      const { api } = setup()
      const conv = makeConv('conv-1', '旧标题')
      api.categories.value = [makeCat('cat-1', '分类1', [conv])]
      api.convRenameInput.value = '新标题'

      await api.confirmRenameConversation('cat-1', 'conv-1')

      expect(updateConvMock).toHaveBeenCalledWith('conv-1', { title: '新标题' })
      expect(api.categories.value[0].conversations[0].title).toBe('新标题')
      expect(api.renamingConvId.value).toBeNull()
    })
  })

  describe('saveMessages', () => {
    it('以第一条用户消息生成标题并过滤空助手消息', async () => {
      const { api, messages, activeConvId } = setup()
      const conv = makeConv('conv-1')
      api.categories.value = [makeCat('cat-1', '分类1', [conv])]
      api.activeCatId.value = 'cat-1'
      activeConvId.value = 'conv-1'
      messages.value = [
        { id: 'm1', role: 'user', content: '第一条用户消息\n换行', timestamp: 1 },
        { id: 'm2', role: 'assistant', content: '', timestamp: 1 },
        { id: 'm3', role: 'assistant', content: '回答', timestamp: 1 },
      ]

      await api.saveMessages()

      expect(conv.title).toBe('第一条用户消息 换行')
      expect(updateConvMock).toHaveBeenCalledWith('conv-1', {
        title: conv.title,
        messages: JSON.stringify([
          { id: 'm1', role: 'user', content: '第一条用户消息\n换行', timestamp: 1 },
          { id: 'm3', role: 'assistant', content: '回答', timestamp: 1 },
        ]),
      })
    })

    it('无激活会话时跳过同步', async () => {
      const { api } = setup()
      await api.saveMessages()
      expect(updateConvMock).not.toHaveBeenCalled()
    })
  })

  describe('搜索', () => {
    it('doSearch 空关键词清空结果', async () => {
      const { api } = setup()
      api.searchResults.value = [makeSearchResult('cat-1', 'conv-1')]

      await api.doSearch('')

      expect(api.searchResults.value).toHaveLength(0)
      expect(searchMock).not.toHaveBeenCalled()
    })

    it('doSearch 拉取匹配结果', async () => {
      const { api } = setup()
      searchMock.mockResolvedValue([makeSearchResult('cat-1', 'conv-1')] as never)

      await api.doSearch('关键词')

      expect(searchMock).toHaveBeenCalledWith('关键词')
      expect(api.searchResults.value).toHaveLength(1)
      expect(api.searching.value).toBe(false)
    })

    it('handleSearchInput 空值清空，非空防抖后搜索', () => {
      const { api } = setup()
      vi.useFakeTimers()

      api.handleSearchInput('')
      expect(api.searchResults.value).toHaveLength(0)
      expect(searchMock).not.toHaveBeenCalled()

      api.handleSearchInput('abc')
      vi.advanceTimersByTime(300)
      expect(searchMock).toHaveBeenCalledWith('abc')

      vi.useRealTimers()
    })

    it('clearSearch 重置查询与结果', () => {
      const { api } = setup()
      api.searchQuery.value = 'x'
      api.searchResults.value = [makeSearchResult('cat-1', 'conv-1')]

      api.clearSearch()

      expect(api.searchQuery.value).toBe('')
      expect(api.searchResults.value).toHaveLength(0)
    })

    it('switchToSearchResult 分类已存在时展开并切换会话', async () => {
      const { api, messages, scrollToMsg } = setup()
      const conv = makeConv('conv-1', '', [{ id: 'm1', role: 'user', content: '你好关键词' }])
      api.categories.value = [makeCat('cat-1', '分类1', [conv])]
      api.searchQuery.value = '关键词'

      await api.switchToSearchResult(makeSearchResult('cat-1', 'conv-1'))
      await nextTick()

      expect(api.categoryExpanded.value['cat-1']).toBe(true)
      expect(api.activeCatId.value).toBe('cat-1')
      expect(api.activeConvId.value).toBe('conv-1')
      expect(messages.value).toHaveLength(1)
      expect(scrollToMsg).toHaveBeenCalledWith('m1')
    })

    it('switchToSearchResult 分类不在列表时先加载分类', async () => {
      const { api } = setup()
      const conv = makeConv('conv-1', '', [{ id: 'm1', role: 'user', content: '你好关键词' }])
      fetchCategoriesMock.mockResolvedValue([makeCat('cat-1', '分类1', [conv])] as never)

      await api.switchToSearchResult(makeSearchResult('cat-1', 'conv-1'))

      expect(fetchCategoriesMock).toHaveBeenCalled()
      expect(api.activeCatId.value).toBe('cat-1')
    })

    it('switchToSearchResult 对话不在当前列表时直接加载并激活', async () => {
      const { api, messages } = setup()
      const conv = makeConv('conv-1', '', [{ id: 'm1', role: 'user', content: 'abc关键词' }])
      api.categories.value = [makeCat('cat-1', '分类1', [])]
      fetchCategoriesMock.mockResolvedValue([makeCat('cat-1', '分类1', [conv])] as never)

      await api.switchToSearchResult(makeSearchResult('cat-1', 'conv-1'))

      expect(api.activeCatId.value).toBe('cat-1')
      expect(api.activeConvId.value).toBe('conv-1')
      expect(messages.value).toHaveLength(1)
    })
  })

  describe('createDefaultData', () => {
    it('创建默认分类与对话并激活', async () => {
      const { api, activeConvId } = setup()

      await api.createDefaultData()

      expect(createCategoryMock).toHaveBeenCalledWith('默认分类')
      expect(api.categories.value[0].id).toBe('cat-default')
      expect(api.activeCatId.value).toBe('cat-default')
      expect(activeConvId.value).toBe('conv-default')
      expect(api.categoryExpanded.value['cat-default']).toBe(true)
    })
  })

  describe('展开与命令分发', () => {
    it('toggleCategoryExpand 切换展开态', () => {
      const { api } = setup()
      api.toggleCategoryExpand('cat-1')
      expect(api.categoryExpanded.value['cat-1']).toBe(true)
      api.toggleCategoryExpand('cat-1')
      expect(api.categoryExpanded.value['cat-1']).toBe(false)
    })

    it('handleCategoryCommand 分发重命名与删除', () => {
      const { api } = setup()
      confirmMock.mockRejectedValue(new Error('cancel'))
      const cat = makeCat('cat-1', '分类1')
      api.categories.value = [cat]

      api.handleCategoryCommand('rename', cat)
      expect(api.renamingCatId.value).toBe('cat-1')

      api.handleCategoryCommand('delete', cat)
      expect(confirmMock).toHaveBeenCalled()
    })

    it('handleConversationCommand 分发重命名与删除', () => {
      const { api } = setup()
      confirmMock.mockRejectedValue(new Error('cancel'))
      const conv = makeConv('conv-1', '旧标题')
      api.categories.value = [makeCat('cat-1', '分类1', [conv])]

      api.handleConversationCommand('rename', 'cat-1', conv)
      expect(api.renamingConvId.value).toBe('conv-1')

      api.handleConversationCommand('delete', 'cat-1', conv)
      expect(confirmMock).toHaveBeenCalled()
    })
  })
})
