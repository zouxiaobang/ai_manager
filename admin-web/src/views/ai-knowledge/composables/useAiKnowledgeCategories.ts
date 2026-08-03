import { nextTick, ref } from 'vue'
import type { Ref } from 'vue'
import { ElMessageBox } from 'element-plus'
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
import type {
  ChatCategoryVO,
  ChatConversationVO,
  ChatMessage,
  ChatSearchResult,
  RagSource,
} from '@/api/aiKnowledge'

/** useAiKnowledgeCategories 依赖注入：分类/会话与聊天状态解耦，由调用方注入 */
export interface AiKnowledgeCategoriesDeps {
  /** 消息列表（调用方持有，与聊天状态机共享） */
  messages: Ref<(ChatMessage & { sources?: RagSource[]; collapsed?: boolean })[]>
  /** 当前激活的会话 id（调用方持有，与聊天状态机共享，清空对话时由 chat 读取） */
  activeConvId: Ref<string | null>
  /** 是否正在发送（切换/删除对话时的保护） */
  sending: Ref<boolean>
  /** 当前会话累计 Token（切换对话时重置，来自聊天状态机） */
  currentContextTokens: Ref<number>
  /** 重新计算 Token（来自聊天状态机） */
  recalcContextTokens: () => void
  /** 滚动到指定消息（来自聊天状态机，搜索结果定位用） */
  scrollToMsg: (msgId: string) => void
}

/**
 * AI 知识页分类与会话状态机
 *
 * <p>从 {@code AiKnowledgeView.vue} 提取：分类/会话 CRUD、全局搜索、会话持久化与恢复。
 * 消息、激活会话与 token 由调用方持有并注入（与聊天状态机共享），其余分类专属状态内聚于此，
 * 可独立于组件单元测试。</p>
 */
export function useAiKnowledgeCategories(deps: AiKnowledgeCategoriesDeps) {
  const {
    messages,
    activeConvId,
    sending,
    currentContextTokens,
    recalcContextTokens,
    scrollToMsg,
  } = deps

  const categories = ref<ChatCategoryVO[]>([])
  const activeCatId = ref<string | null>(null)
  const categoryExpanded = ref<Record<string, boolean>>({})
  const renamingCatId = ref<string | null>(null)
  const renamingConvId = ref<string | null>(null)
  const catRenameInput = ref('')
  const convRenameInput = ref('')
  const loadingCategories = ref(false)

  // ========== 全局搜索 ==========
  const searchQuery = ref('')
  const searchResults = ref<ChatSearchResult[]>([])
  const searching = ref(false)
  let searchTimeout: ReturnType<typeof setTimeout> | null = null

  async function doSearch(keyword: string) {
    if (!keyword.trim()) {
      searchResults.value = []
      return
    }
    searching.value = true
    try {
      searchResults.value = await searchChatConversations(keyword.trim())
    } catch {
      searchResults.value = []
    } finally {
      searching.value = false
    }
  }

  function handleSearchInput(val: string) {
    if (searchTimeout) clearTimeout(searchTimeout)
    if (!val.trim()) {
      searchResults.value = []
      return
    }
    searchTimeout = setTimeout(() => doSearch(val), 300)
  }

  function clearSearch() {
    searchQuery.value = ''
    searchResults.value = []
    if (searchTimeout) clearTimeout(searchTimeout)
  }

  async function switchToSearchResult(r: ChatSearchResult) {
    const keyword = searchQuery.value // 在 clearSearch 之前保存搜索关键词
    clearSearch()
    // 确保分类已展开
    categoryExpanded.value[r.categoryId] = true
    // 如果该分类不在 categories 中，先加载
    let cat = categories.value.find(c => c.id === r.categoryId)
    if (!cat) {
      await loadCategoriesFromServer()
      cat = categories.value.find(c => c.id === r.categoryId)
    }
    if (cat) {
      const conv = cat.conversations.find(c => c.id === r.conversationId)
      if (conv) {
        await switchConversation(r.categoryId, r.conversationId)
        scrollToFirstMatchingMessage(keyword)
      } else {
        // 对话不在当前列表，直接通过 API 加载
        activeCatId.value = r.categoryId
        activeConvId.value = r.conversationId
        try {
          const data = await fetchChatCategories()
          categories.value = data || []
          const foundCat = data?.find(c => c.id === r.categoryId)
          const foundConv = foundCat?.conversations.find(c => c.id === r.conversationId)
          if (foundCat && foundConv) {
            messages.value = JSON.parse(foundConv.messages || '[]')
            recalcContextTokens()
          } else {
            messages.value = []
            currentContextTokens.value = 0
          }
        } catch {
          messages.value = []
          currentContextTokens.value = 0
        }
        saveActiveSession()
        scrollToFirstMatchingMessage(keyword)
      }
    }
  }

  /** 在当前 messages 中查找包含关键词的消息，滚动到该位置 */
  function scrollToFirstMatchingMessage(keyword: string) {
    if (!keyword) return
    const lowerKw = keyword.toLowerCase()
    const match = messages.value.find(m => m.content.toLowerCase().includes(lowerKw))
    if (match) {
      nextTick(() => {
        scrollToMsg(match.id)
      })
    }
  }

  /** 持久化当前激活的对话到 localStorage（仅前端记住上次打开的对话） */
  function saveActiveSession() {
    try {
      localStorage.setItem('ak-active-session', JSON.stringify({ catId: activeCatId.value, convId: activeConvId.value }))
    } catch { /* ignore */ }
  }

  /** 获取上次激活的对话 */
  function loadActiveSession(): { catId: string; convId: string } | null {
    try {
      const raw = localStorage.getItem('ak-active-session')
      if (!raw) return null
      const parsed = JSON.parse(raw)
      return {
        catId: String(parsed.catId),
        convId: String(parsed.convId),
      }
    } catch { return null }
  }

  /** 将当前 messages 同步到激活对话 */
  async function syncMessagesToConversation() {
    if (!activeCatId.value || !activeConvId.value) return
    const cat = categories.value.find(c => c.id === activeCatId.value)
    if (!cat) return
    const conv = cat.conversations.find(c => c.id === activeConvId.value)
    if (!conv) return
    // 更新标题（从第一条用户消息）
    if (!conv.title) {
      const firstUser = messages.value.find(m => m.role === 'user')
      if (firstUser) {
        conv.title = firstUser.content.slice(0, 30).replace(/\n/g, ' ').trim()
      }
    }
    // 保存到后端
    try {
      await updateChatConversation(conv.id, {
        title: conv.title,
        messages: JSON.stringify(messages.value.filter(m => !(m.role === 'assistant' && !m.content))),
      })
    } catch { /* ignore */ }
  }

  /** 保存对话 */
  async function saveMessages() {
    await syncMessagesToConversation()
  }

  /** 从后端加载分类和对话 */
  async function loadCategoriesFromServer() {
    if (loadingCategories.value) return
    loadingCategories.value = true
    try {
      const data = await fetchChatCategories()
      categories.value = data || []
      data?.forEach(c => { categoryExpanded.value[c.id] = true })

      if (data && data.length > 0) {
        // 尝试恢复上次激活的对话
        const active = loadActiveSession()
        if (active) {
          const cat = data.find(c => c.id === active.catId)
          const conv = cat?.conversations.find(c => c.id === active.convId)
          if (cat && conv) {
            activeCatId.value = cat.id
            activeConvId.value = conv.id
            try {
              messages.value = JSON.parse(conv.messages || '[]')
              recalcContextTokens()
            } catch { messages.value = [] }
            return
          }
        }
        // 默认选中第一个对话
        const firstCat = data[0]
        if (firstCat.conversations.length > 0) {
          const firstConv = firstCat.conversations[0]
          activeCatId.value = firstCat.id
          activeConvId.value = firstConv.id
          try {
            messages.value = JSON.parse(firstConv.messages || '[]')
            recalcContextTokens()
          } catch { messages.value = [] }
        } else {
          // 分类下无对话，自动创建
          await addConversation(firstCat.id)
        }
      } else {
        // 无数据，创建默认
        await createDefaultData()
      }
    } catch {
      await createDefaultData()
    } finally {
      loadingCategories.value = false
    }
  }

  /** 切换到指定对话 */
  async function switchConversation(catId: string, convId: string) {
    if (sending.value) return
    await syncMessagesToConversation()
    const cat = categories.value.find(c => c.id === catId)
    if (!cat) return
    const conv = cat.conversations.find(c => c.id === convId)
    if (!conv) return
    activeCatId.value = catId
    activeConvId.value = convId
    try {
      messages.value = JSON.parse(conv.messages || '[]')
      recalcContextTokens()
    } catch { messages.value = [] }
    saveActiveSession()
  }

  /** 新增分类 */
  async function addCategory() {
    try {
      const { value } = await ElMessageBox.prompt('请输入分类名称', '新增分类', {
        confirmButtonText: '确定', cancelButtonText: '取消',
        inputPattern: /\S/, inputErrorMessage: '分类名称不能为空',
      })
      const cat = await createChatCategory(value.trim())
      categories.value.push(cat)
      categoryExpanded.value[cat.id] = true
    } catch { /* cancelled */ }
  }

  /** 重命名分类 */
  function startRenameCategory(catId: string) {
    const cat = categories.value.find(c => c.id === catId)
    if (!cat) return
    renamingCatId.value = catId
    catRenameInput.value = cat.name
    nextTick(() => {
      const el = document.querySelector('.ak-sidebar__rename-input') as HTMLElement
      el?.focus()
    })
  }
  async function confirmRenameCategory(catId: string) {
    const name = catRenameInput.value.trim()
    if (!name) { renamingCatId.value = null; return }
    try {
      await renameChatCategory(catId, name)
      const cat = categories.value.find(c => c.id === catId)
      if (cat) cat.name = name
    } catch { /* ignore */ }
    renamingCatId.value = null
  }
  function cancelRenameCategory() { renamingCatId.value = null }

  /** 删除分类 */
  async function deleteCategory(catId: string) {
    if (sending.value) return
    const idx = categories.value.findIndex(c => c.id === catId)
    if (idx === -1) return
    try {
      await ElMessageBox.confirm('确定要删除此分类及其所有对话吗？', '删除分类', {
        type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
      })
      await deleteChatCategory(catId)
      categories.value.splice(idx, 1)
      if (activeCatId.value === catId) {
        if (categories.value.length > 0) {
          const fc = categories.value[0]
          if (fc.conversations.length > 0) {
            await switchConversation(fc.id, fc.conversations[0].id)
          } else {
            await addConversation(fc.id)
          }
        } else { await createDefaultData() }
      }
    } catch { /* cancelled or error */ }
  }

  function handleCategoryCommand(cmd: string, cat: ChatCategoryVO) {
    if (cmd === 'rename') startRenameCategory(cat.id)
    else if (cmd === 'delete') deleteCategory(cat.id)
  }

  function handleConversationCommand(cmd: string, catId: string, conv: ChatConversationVO) {
    if (cmd === 'rename') startRenameConversation(conv.id)
    else if (cmd === 'delete') deleteConversation(catId, conv.id)
  }

  function toggleCategoryExpand(catId: string) {
    categoryExpanded.value[catId] = !categoryExpanded.value[catId]
  }

  /** 新增对话 */
  async function addConversation(catId: string) {
    if (sending.value) return
    const cat = categories.value.find(c => c.id === catId)
    if (!cat) return
    try {
      const conv = await createChatConversation(catId)
      cat.conversations.push(conv)
      categoryExpanded.value[catId] = true
      activeCatId.value = catId
      activeConvId.value = conv.id
      messages.value = []
      currentContextTokens.value = 0
      saveActiveSession()
    } catch { /* ignore */ }
  }

  /** 删除对话 */
  async function deleteConversation(catId: string, convId: string) {
    if (sending.value) return
    const cat = categories.value.find(c => c.id === catId)
    if (!cat) return
    const idx = cat.conversations.findIndex(c => c.id === convId)
    if (idx === -1) return
    try {
      await ElMessageBox.confirm('确定要删除此对话吗？', '删除对话', {
        type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
      })
      await deleteChatConversation(convId)
      cat.conversations.splice(idx, 1)
      if (activeCatId.value === catId && activeConvId.value === convId) {
        if (cat.conversations.length > 0) {
          await switchConversation(catId, cat.conversations[0].id)
        } else {
          await addConversation(catId)
        }
      }
    } catch { /* cancelled or error */ }
  }

  /** 重命名对话 */
  function startRenameConversation(convId: string) {
    const all = categories.value.flatMap(c => c.conversations)
    const conv = all.find(c => c.id === convId)
    if (!conv) return
    renamingConvId.value = convId
    convRenameInput.value = conv.title
    nextTick(() => {
      const el = document.querySelector('.ak-sidebar__rename-input') as HTMLElement
      el?.focus()
    })
  }
  async function confirmRenameConversation(catId: string, convId: string) {
    const title = convRenameInput.value.trim()
    if (!title) { renamingConvId.value = null; return }
    try {
      await updateChatConversation(convId, { title })
      const cat = categories.value.find(c => c.id === catId)
      if (!cat) return
      const conv = cat.conversations.find(c => c.id === convId)
      if (conv) conv.title = title
    } catch { /* ignore */ }
    renamingConvId.value = null
  }
  function cancelRenameConversation() { renamingConvId.value = null }

  /** 创建默认分类和对话 */
  async function createDefaultData() {
    try {
      const cat = await createChatCategory('默认分类')
      const conv = await createChatConversation(cat.id)
      categories.value = [cat]
      categoryExpanded.value[cat.id] = true
      activeCatId.value = cat.id
      activeConvId.value = conv.id
      messages.value = []
      currentContextTokens.value = 0
      saveActiveSession()
    } catch { /* ignore */ }
  }

  return {
    categories,
    activeCatId,
    activeConvId,
    categoryExpanded,
    renamingCatId,
    renamingConvId,
    catRenameInput,
    convRenameInput,
    loadingCategories,
    searchQuery,
    searchResults,
    searching,
    doSearch,
    handleSearchInput,
    clearSearch,
    switchToSearchResult,
    loadCategoriesFromServer,
    switchConversation,
    saveMessages,
    addCategory,
    startRenameCategory,
    confirmRenameCategory,
    cancelRenameCategory,
    deleteCategory,
    handleCategoryCommand,
    handleConversationCommand,
    toggleCategoryExpand,
    addConversation,
    deleteConversation,
    startRenameConversation,
    confirmRenameConversation,
    cancelRenameConversation,
    createDefaultData,
  }
}
