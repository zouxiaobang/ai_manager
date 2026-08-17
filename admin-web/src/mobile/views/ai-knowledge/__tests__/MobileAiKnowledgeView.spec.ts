import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'
import ElementPlus from 'element-plus'
import MobileAiKnowledgeView from '../MobileAiKnowledgeView.vue'

// i18n：视图模板用 t() 渲染 key 本身，断言以 key 文本为准
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key }),
}))

// 底层请求层 mock：拦截 request.ts 的四个封装，让 aiKnowledge.ts 内真实高层函数安全返回空数据，
// 再在高层按需覆写 fixture（仅用 `@/api/request` 路径，与 `./request` 相对导入解析到同一模块）
const request = vi.hoisted(() => ({
  getData: vi.fn().mockResolvedValue([]),
  postData: vi.fn().mockResolvedValue({}),
  putData: vi.fn().mockResolvedValue({}),
  deleteData: vi.fn().mockResolvedValue({}),
}))

vi.mock('@/api/request', async (importActual) => {
  const actual = await importActual<typeof import('@/api/request')>()
  return {
    ...actual,
    getData: request.getData,
    postData: request.postData,
    putData: request.putData,
    deleteData: request.deleteData,
  }
})

// 高层 API 覆写：仅接管需要固定数据的接口，其余走 mocked 请求层返回空值
const api = vi.hoisted(() => ({
  fetchChatCategories: vi.fn(),
  fetchRagStats: vi.fn(),
  fetchRagDocuments: vi.fn(),
  fetchAiModelConfig: vi.fn(),
}))

const CATEGORIES_FIXTURE = [
  {
    id: 'c1',
    name: '工作',
    conversations: [
      {
        id: 'v1',
        categoryId: 'c1',
        title: '方案讨论',
        messages: JSON.stringify([
          { id: 'm1', role: 'user', content: '你好', createdAt: '' },
          { id: 'm2', role: 'assistant', content: '你好！', createdAt: '' },
        ]),
        createdAt: '',
        updatedAt: '',
      },
      { id: 'v2', categoryId: 'c1', title: '待办整理', messages: '[]', createdAt: '', updatedAt: '' },
    ],
  },
]

vi.mock('@/api/aiKnowledge', async (importActual) => {
  const actual = await importActual<typeof import('@/api/aiKnowledge')>()
  return {
    ...actual,
    fetchChatCategories: api.fetchChatCategories,
    fetchRagStats: api.fetchRagStats,
    fetchRagDocuments: api.fetchRagDocuments,
    fetchAiModelConfig: api.fetchAiModelConfig,
  }
})

// 子组件打桩：隔离 Teleport / 涂鸦动画细节，仅关心抽屉的显隐与插槽内容
const stubs = {
  MobileCard: { template: '<div class="mb-card"><slot /></div>' },
  MobileDoodleSearch: {
    props: ['modelValue', 'placeholder'],
    emits: ['update:modelValue'],
    template: '<div class="mb-search" />',
  },
  MobileBottomSheet: {
    props: ['modelValue', 'loading'],
    emits: ['update:modelValue'],
    template: '<div class="mb-sheet" v-show="modelValue"><slot name="header" /><slot /></div>',
  },
}

let wrapper: VueWrapper | undefined

function mountView() {
  wrapper = mount(MobileAiKnowledgeView, {
    global: { plugins: [ElementPlus], stubs },
    attachTo: document.body,
  })
  return wrapper
}

function findTab(labelKey: string) {
  return wrapper!.findAll('.akm__tab').find((b) => b.text() === labelKey)
}

// 会话激活状态保存在 localStorage（loadActiveSession），清空避免用例间互相污染
beforeEach(() => {
  localStorage.clear()
})

afterEach(() => {
  vi.useRealTimers()
  wrapper?.unmount()
  wrapper = undefined
  // 清空 Teleport / EP 弹层残留 DOM，避免用例间互相污染
  document.body.innerHTML = ''
})

describe('MobileAiKnowledgeView 三段式布局', () => {
  it('渲染三个切换 tab，默认显示对话面板', async () => {
    api.fetchChatCategories.mockResolvedValue(CATEGORIES_FIXTURE)
    mountView()
    await flushPromises()

    expect(wrapper!.findAll('.akm__tab')).toHaveLength(3)
    expect(findTab('aiKnowledge.tabs.chat')!.exists()).toBe(true)
    expect(findTab('aiKnowledge.tabs.rag')!.exists()).toBe(true)
    expect(findTab('aiKnowledge.tabs.settings')!.exists()).toBe(true)

    expect(wrapper!.find('.akm__chat').isVisible()).toBe(true)
    // rag / settings 面板默认隐藏（v-show）
    expect(wrapper!.findAll('.akm__pane').every((p) => !p.isVisible())).toBe(true)
  })

  it('加载会话后标题联动：默认选中第一个对话，无会话时回落「新对话」', async () => {
    api.fetchChatCategories.mockResolvedValue(CATEGORIES_FIXTURE)
    mountView()
    await flushPromises()

    expect(api.fetchChatCategories).toHaveBeenCalled()
    expect(wrapper!.find('.akm__convbar-title').text()).toBe('方案讨论')
  })

  it('无激活会话时发送按钮禁用，输入问题后可用', async () => {
    api.fetchChatCategories.mockResolvedValue(CATEGORIES_FIXTURE)
    mountView()
    await flushPromises()

    const send = wrapper!.find('.akm__send')
    expect(send.attributes('disabled')).toBeDefined()

    await wrapper!.find('.akm__input-area').setValue('测试问题')
    expect(wrapper!.find('.akm__send').attributes('disabled')).toBeUndefined()
  })

  it('切换到 RAG 面板：加载统计卡与文档列表', async () => {
    api.fetchChatCategories.mockResolvedValue(CATEGORIES_FIXTURE)
    api.fetchRagStats.mockResolvedValue({ totalDocs: 3, readyCount: 2, processingCount: 1, failedCount: 0, totalChunks: 12 })
    api.fetchRagDocuments.mockResolvedValue([
      { id: 'd1', fileName: 'a.md', fileType: 'md', fileSize: 1024, chunkCount: 2, status: 'ready', indexedAt: '2026-08-01 10:00', errorMessage: null },
    ])
    mountView()
    await flushPromises()

    await findTab('aiKnowledge.tabs.rag')!.trigger('click')
    await flushPromises()

    expect(api.fetchRagStats).toHaveBeenCalled()
    const stats = wrapper!.findAll('.akm__stat')
    expect(stats).toHaveLength(5)
    expect(stats[0].find('.akm__stat-value').text()).toBe('3')
    expect(wrapper!.find('.akm__doc-name').text()).toBe('a.md')
  })

  it('切换到配置面板：渲染 LLM 表单并加载配置', async () => {
    api.fetchChatCategories.mockResolvedValue(CATEGORIES_FIXTURE)
    api.fetchAiModelConfig.mockResolvedValue({
      provider: 'openai',
      apiKey: '',
      apiBaseUrl: 'https://api.openai.com/v1',
      model: 'gpt-4o',
      temperature: 1,
      maxTokens: 4096,
      embeddingModel: 'text-embedding-3-small',
      maxContextMessages: 10,
    })
    mountView()
    await flushPromises()

    await findTab('aiKnowledge.tabs.settings')!.trigger('click')
    await flushPromises()

    expect(api.fetchAiModelConfig).toHaveBeenCalled()
    expect(wrapper!.find('.el-form').exists()).toBe(true)
    expect(wrapper!.find('.el-select').exists()).toBe(true)
  })
})

describe('MobileAiKnowledgeView 对话与标记交互', () => {
  it('打开会话抽屉切换对话，标题随激活会话联动', async () => {
    api.fetchChatCategories.mockResolvedValue(CATEGORIES_FIXTURE)
    mountView()
    await flushPromises()

    // 打开会话抽屉
    await wrapper!.find('.akm__convbar-current').trigger('click')
    await nextTick()
    expect(wrapper!.find('.mb-sheet').isVisible()).toBe(true)

    // 切到第二个会话（点击会话行内主按钮，会话行本身是 div 容器）
    const convs = wrapper!.findAll('.akm__conv-main')
    expect(convs.map((c) => c.text().trim())).toContain('待办整理')
    await convs.find((c) => c.text().includes('待办整理'))!.trigger('click')
    await flushPromises()

    expect(wrapper!.find('.akm__convbar-title').text()).toBe('待办整理')
    // 切换后抽屉关闭
    expect(wrapper!.find('.mb-sheet').isVisible()).toBe(false)
  })

  it('打开标记抽屉：无标记时显示空态', async () => {
    api.fetchChatCategories.mockResolvedValue(CATEGORIES_FIXTURE)
    mountView()
    await flushPromises()

    await wrapper!.find('[aria-label="aiKnowledge.chat.marker.marker"]').trigger('click')
    await nextTick()

    const markerSheet = wrapper!.findAll('.mb-sheet').find((s) => s.text().includes('aiKnowledge.chat.marker.addMarker'))
    expect(markerSheet?.text()).toContain('aiKnowledge.chat.marker.empty')
  })

  it('长按消息弹出标记四件套动作单，无标记时「回到上一个 / 删除全部」禁用', async () => {
    api.fetchChatCategories.mockResolvedValue(CATEGORIES_FIXTURE)
    mountView()
    await flushPromises()

    // 挂载后自动选中 v1，其 messages 含两条消息
    const rows = wrapper!.findAll('.ak-chat__msg-row')
    expect(rows).toHaveLength(2)

    vi.useFakeTimers()
    await rows[0].trigger('touchstart')
    await vi.advanceTimersByTimeAsync(600)
    await nextTick()

    const items = wrapper!.findAll('.akm__action-item')
    const labels = items.map((i) => i.text())
    expect(labels).toEqual([
      'aiKnowledge.chat.marker.addMarker',
      'aiKnowledge.chat.marker.marker',
      'aiKnowledge.chat.marker.jumpPrevious',
      'aiKnowledge.chat.marker.deleteAll',
    ])
    // 当前会话无标记、滚动在顶部无「上一个」，两项应禁用
    expect(items[2].attributes('disabled')).toBeDefined()
    expect(items[3].attributes('disabled')).toBeDefined()
  })

  it('长按点「新增标记」：锚定被长按消息且不滚动视口', async () => {
    api.fetchChatCategories.mockResolvedValue(CATEGORIES_FIXTURE)
    mountView()
    await flushPromises()

    const rows = wrapper!.findAll('.ak-chat__msg-row')
    expect(rows).toHaveLength(2)
    request.postData.mockClear()

    vi.useFakeTimers()
    await rows[0].trigger('touchstart')
    await vi.advanceTimersByTimeAsync(600)
    await nextTick()

    // 点第一项「新增标记」
    await wrapper!.findAll('.akm__action-item')[0].trigger('click')
    // 推进宏任务与微任务，让 createChatBookmark 的异步链落定
    await vi.advanceTimersByTimeAsync(0)
    await nextTick()

    const bookmarkCall = request.postData.mock.calls.find(([url]) => String(url).includes('/bookmarks'))
    expect(bookmarkCall).toBeDefined()
    // 锚点 = 被长按的消息 m1（而非视口顶线推断），scrollTop 保持 0 未跳动
    expect((bookmarkCall![1] as { msgId: string | null }).msgId).toBe('m1')
  })
})
