import { ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  useAiKnowledgeMarkers,
  detectTopHeading,
  topRelativeToContainer,
  previousMarkerFor,
} from '../useAiKnowledgeMarkers'
import {
  fetchChatBookmarks,
  createChatBookmark,
  renameChatBookmark,
  deleteChatBookmark,
  deleteAllChatBookmarks,
} from '@/api/aiKnowledge'
import type { ChatBookmark } from '@/api/aiKnowledge'

vi.mock('@/api/aiKnowledge', () => ({
  fetchChatBookmarks: vi.fn(),
  createChatBookmark: vi.fn(),
  renameChatBookmark: vi.fn(),
  deleteChatBookmark: vi.fn(),
  deleteAllChatBookmarks: vi.fn(),
}))

const fetchMock = vi.mocked(fetchChatBookmarks)
const createMock = vi.mocked(createChatBookmark)
const renameMock = vi.mocked(renameChatBookmark)
const deleteMock = vi.mocked(deleteChatBookmark)
const deleteAllMock = vi.mocked(deleteAllChatBookmarks)

/** 等待微任务 + 宏任务排空，覆盖 async/await 与 watch 的异步 flush */
function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function rect(top: number): DOMRect {
  return { top, bottom: top, left: 0, right: 0, width: 0, height: 0, x: 0, y: 0, toJSON: () => ({}) } as DOMRect
}

function marker(overrides: Partial<ChatBookmark> = {}): ChatBookmark {
  return {
    id: '1',
    conversationId: 'conv-1',
    name: '标记 1',
    msgId: 'm1',
    msgOffsetTop: 100,
    scrollTop: 200,
    createdAt: '2026-01-01T00:00:00',
    ...overrides,
  }
}

function setup(convId: string | null = 'conv-1') {
  const activeConvId = ref<string | null>(convId)
  const chatMessagesRef = ref<HTMLElement | null>(null)
  const api = useAiKnowledgeMarkers({ activeConvId, chatMessagesRef })
  return { api, activeConvId, chatMessagesRef }
}

/**
 * 构造一个带消息行的滚动容器：
 * topRelativeToContainer(row, container) = rowRectTop - containerRectTop + container.scrollTop
 */
function buildContainer(
  rows: { id: string; rectTop: number; height?: number }[],
  opts: { scrollTop?: number; clientHeight?: number; scrollHeight?: number; containerTop?: number } = {},
) {
  const container = document.createElement('div')
  container.scrollTop = opts.scrollTop ?? 0
  Object.defineProperty(container, 'clientHeight', { value: opts.clientHeight ?? 400, configurable: true })
  Object.defineProperty(container, 'scrollHeight', { value: opts.scrollHeight ?? 1200, configurable: true })
  vi.spyOn(container, 'getBoundingClientRect').mockReturnValue(rect(opts.containerTop ?? 50))
  for (const r of rows) {
    const row = document.createElement('div')
    // 锚点选择器为 .ak-chat__msg-row[data-msg-id]，行必须带该类才能被 addMarker 命中
    row.className = 'ak-chat__msg-row'
    row.setAttribute('data-msg-id', r.id)
    Object.defineProperty(row, 'offsetHeight', { value: r.height ?? 100, configurable: true })
    vi.spyOn(row, 'getBoundingClientRect').mockReturnValue(rect(r.rectTop))
    container.appendChild(row)
  }
  return container
}

/** 构造一个仅含标题的容器（用于 detectTopHeading） */
function headingContainer(headings: { tag: string; text: string; rectTop: number }[], clientHeight = 400) {
  const container = document.createElement('div')
  container.scrollTop = 0
  Object.defineProperty(container, 'clientHeight', { value: clientHeight, configurable: true })
  vi.spyOn(container, 'getBoundingClientRect').mockReturnValue(rect(0))
  for (const h of headings) {
    const el = document.createElement(h.tag)
    el.textContent = h.text
    vi.spyOn(el, 'getBoundingClientRect').mockReturnValue(rect(h.rectTop))
    container.appendChild(el)
  }
  return container
}

beforeEach(() => {
  vi.clearAllMocks()
  fetchMock.mockResolvedValue([])
  createMock.mockResolvedValue(marker({ id: '9' }))
  renameMock.mockResolvedValue(undefined)
  deleteMock.mockResolvedValue(undefined)
  deleteAllMock.mockResolvedValue(undefined)
})

describe('detectTopHeading 纯函数', () => {
  it('取可见标题中层级最小者（H1 优先于 H2）', () => {
    const el = headingContainer([
      { tag: 'h2', text: '二级', rectTop: 100 },
      { tag: 'h1', text: '一级', rectTop: 200 },
    ])
    expect(detectTopHeading(el)).toBe('一级')
  })

  it('同层级取文档序靠前者', () => {
    const el = headingContainer([
      { tag: 'h2', text: '第一个', rectTop: 50 },
      { tag: 'h2', text: '第二个', rectTop: 150 },
    ])
    expect(detectTopHeading(el)).toBe('第一个')
  })

  it('视口外的标题不计入，全部不可见返回 null', () => {
    const el = headingContainer([
      { tag: 'h1', text: '在上面', rectTop: -10 },
      { tag: 'h2', text: '在下面', rectTop: 500 },
    ])
    expect(detectTopHeading(el)).toBeNull()
  })

  it('无标题返回 null', () => {
    const el = document.createElement('div')
    vi.spyOn(el, 'getBoundingClientRect').mockReturnValue(rect(0))
    expect(detectTopHeading(el)).toBeNull()
  })

  it('标题文本超过 30 字时截断', () => {
    const el = headingContainer([{ tag: 'h1', text: '很'.repeat(40), rectTop: 10 }])
    expect(detectTopHeading(el)).toBe('很'.repeat(30))
  })
})

describe('useAiKnowledgeMarkers', () => {
  describe('加载与会话联动', () => {
    it('loadMarkers 拉取当前会话标记', async () => {
      const { api } = setup()
      fetchMock.mockResolvedValue([marker()])
      await api.loadMarkers()
      expect(fetchMock).toHaveBeenCalledWith('conv-1')
      expect(api.markers.value).toHaveLength(1)
    })

    it('会话切换后自动重新加载', async () => {
      const { api, activeConvId } = setup()
      fetchMock.mockResolvedValue([marker({ id: '2', name: '新会话标记' })])
      activeConvId.value = 'conv-2'
      await flushPromises()
      expect(fetchMock).toHaveBeenCalledWith('conv-2')
      expect(api.markers.value).toHaveLength(1)
    })

    it('无会话时清空标记且不请求后端', async () => {
      const { api } = setup(null)
      await flushPromises()
      expect(api.markers.value).toHaveLength(0)
      expect(fetchMock).not.toHaveBeenCalled()
    })
  })

  describe('addMarker', () => {
    it('无会话或无容器时不新增', async () => {
      const { api } = setup(null)
      expect(await api.addMarker('标记 1')).toBeNull()
      const { api: api2, chatMessagesRef } = setup('conv-1')
      chatMessagesRef.value = null
      expect(await api2.addMarker('标记 1')).toBeNull()
      expect(createMock).not.toHaveBeenCalled()
    })

    it('记录滚动位置与锚点消息并回填本地', async () => {
      const { api, chatMessagesRef } = setup()
      // 行 rectTop=150，容器 rectTop=50，scrollTop=200 → 锚点偏移 = 150-50+200 = 300
      chatMessagesRef.value = buildContainer([{ id: 'm1', rectTop: 150 }], { scrollTop: 200 })
      const created = marker({ id: '9' })
      createMock.mockResolvedValue(created)

      const result = await api.addMarker('标记 1')

      expect(createMock).toHaveBeenCalledWith('conv-1', { name: '标记 1', msgId: 'm1', msgOffsetTop: 300, scrollTop: 200 })
      expect(result).toEqual(created)
      expect(api.markers.value).toEqual([created])
    })

    it('有可见标题时名称取最大级别标题', async () => {
      const { api, chatMessagesRef } = setup()
      const container = buildContainer([{ id: 'm1', rectTop: 150 }], { scrollTop: 200 })
      const h1 = document.createElement('h1')
      h1.textContent = '问题总结'
      vi.spyOn(h1, 'getBoundingClientRect').mockReturnValue(rect(160))
      container.appendChild(h1)
      chatMessagesRef.value = container

      await api.addMarker('标记 1')

      expect(createMock).toHaveBeenCalledWith(
        'conv-1',
        expect.objectContaining({ name: '问题总结' }),
      )
    })

    it('无可见标题时名称回落默认名', async () => {
      const { api, chatMessagesRef } = setup()
      chatMessagesRef.value = buildContainer([{ id: 'm1', rectTop: 150 }], { scrollTop: 200 })

      await api.addMarker('默认标记')

      expect(createMock).toHaveBeenCalledWith('conv-1', expect.objectContaining({ name: '默认标记' }))
    })

    it('指定锚点消息时直接锚定该消息，不依赖视口顶线', async () => {
      const { api, chatMessagesRef } = setup()
      // m1 已滚过视口顶线（不在可视位置），m2 为长按目标：指定 m2 应精确锚定它
      chatMessagesRef.value = buildContainer(
        [{ id: 'm1', rectTop: 50 }, { id: 'm2', rectTop: 300, height: 60 }],
        { scrollTop: 500 },
      )
      const created = marker({ id: '9' })
      createMock.mockResolvedValue(created)

      const result = await api.addMarker('标记 1', 'm2')

      // 偏移 = 300 - 50(containerTop) + 500(scrollTop) = 750；scrollTop 保持原值不跳动
      expect(createMock).toHaveBeenCalledWith('conv-1', { name: '标记 1', msgId: 'm2', msgOffsetTop: 750, scrollTop: 500 })
      expect(result).toEqual(created)
      expect(api.markers.value).toEqual([created])
    })

    it('指定锚点消息不存在时回退视口顶线推断', async () => {
      const { api, chatMessagesRef } = setup()
      chatMessagesRef.value = buildContainer([{ id: 'm1', rectTop: 150 }], { scrollTop: 200 })

      await api.addMarker('标记 1', 'ghost')

      // 回退与原行为一致：m1 偏移 150-50+200=300 盖住视口顶线 200
      expect(createMock).toHaveBeenCalledWith('conv-1', { name: '标记 1', msgId: 'm1', msgOffsetTop: 300, scrollTop: 200 })
    })
  })

  describe('jumpToMarker', () => {
    it('锚点消息仍在时按重锚定计算并滚动', () => {
      const { api, chatMessagesRef } = setup()
      const container = buildContainer([{ id: 'm1', rectTop: 150 }], { scrollTop: 200 })
      chatMessagesRef.value = container
      // 当前锚点偏移 300；标记记录 (msgOffsetTop=100, scrollTop=200)
      // 目标 = 300 + (200-100) = 400
      api.markers.value = [marker()]
      api.jumpToMarker('1')
      expect(container.scrollTop).toBe(400)
    })

    it('锚点消息丢失时回退绝对滚动位置', () => {
      const { api, chatMessagesRef } = setup()
      const container = buildContainer([{ id: 'other', rectTop: 150 }], { scrollTop: 0 })
      chatMessagesRef.value = container
      api.markers.value = [marker({ msgId: 'gone' })]
      api.jumpToMarker('1')
      expect(container.scrollTop).toBe(200)
    })

    it('目标越界时钳制到合法区间', () => {
      const { api, chatMessagesRef } = setup()
      const container = buildContainer([{ id: 'm1', rectTop: 150 }], { scrollTop: 200, scrollHeight: 250 })
      chatMessagesRef.value = container
      api.markers.value = [marker()]
      // 重锚定目标 400，但 max = 250-400 = -150 → 钳到 0
      api.jumpToMarker('1')
      expect(container.scrollTop).toBe(0)
    })
  })

  describe('jumpToPreviousMarker', () => {
    it('跳到当前视口上方最近的标记（取记录 scrollTop 最大者，按重锚定计算）', () => {
      const { api, chatMessagesRef } = setup()
      const container = buildContainer(
        [{ id: 'm1', rectTop: 150 }, { id: 'm2', rectTop: 400 }],
        { scrollTop: 300 },
      )
      chatMessagesRef.value = container
      api.markers.value = [
        marker({ id: 'm1', msgId: 'm1', msgOffsetTop: 100, scrollTop: 100 }),
        marker({ id: 'm2', msgId: 'm2', msgOffsetTop: 250, scrollTop: 250 }),
      ]
      // 当前 300 → 上一个为 scrollTop=250 的 m2
      expect(api.jumpToPreviousMarker()).toBe(true)
      // 重锚定：m2 当前内容偏移 = 400-50+300 = 650；目标 = 650 + (250-250) = 650
      expect(container.scrollTop).toBe(650)
    })

    it('当前位于最顶部、无上一个标记时返回 false 且不滚动', () => {
      const { api, chatMessagesRef } = setup()
      const container = buildContainer([{ id: 'm1', rectTop: 150 }], { scrollTop: 0 })
      chatMessagesRef.value = container
      api.markers.value = [marker({ scrollTop: 100 })]
      expect(api.jumpToPreviousMarker()).toBe(false)
      expect(container.scrollTop).toBe(0)
    })

    it('无容器时返回 false', () => {
      const { api } = setup()
      api.markers.value = [marker({ scrollTop: 100 })]
      expect(api.jumpToPreviousMarker()).toBe(false)
    })

    it('停靠于某标记（落点与记录值有偏差）时回它的上一个，而不是再次跳回自己（回归）', () => {
      const { api, chatMessagesRef } = setup()
      // 当前滚动 505，与标记 m2 的记录值 500 偏差 5px（重锚定/布局偏移场景）
      const container = buildContainer(
        [{ id: 'm1', rectTop: 150 }, { id: 'm2', rectTop: 400 }],
        { scrollTop: 505 },
      )
      chatMessagesRef.value = container
      api.markers.value = [
        marker({ id: 'm1', msgId: 'm1', msgOffsetTop: 100, scrollTop: 100 }),
        marker({ id: 'm2', msgId: 'm2', msgOffsetTop: 250, scrollTop: 500 }),
      ]
      // 应回 m1 而非再次选中 m2：m1 重锚定目标 = 150-50+505 + (100-100) = 605
      expect(api.jumpToPreviousMarker()).toBe(true)
      expect(container.scrollTop).toBe(605)
    })

    it('停靠于第一个标记时无上一个可回', () => {
      const { api, chatMessagesRef } = setup()
      const container = buildContainer([{ id: 'm1', rectTop: 150 }], { scrollTop: 100 })
      chatMessagesRef.value = container
      api.markers.value = [marker({ msgId: 'm1', msgOffsetTop: 100, scrollTop: 100 })]
      expect(api.jumpToPreviousMarker()).toBe(false)
      expect(container.scrollTop).toBe(100)
    })
  })

  describe('previousMarkerFor / hasPreviousAt', () => {
    const list = () => [
      marker({ id: 'm1', scrollTop: 100 }),
      marker({ id: 'm2', scrollTop: 500 }),
    ]

    it('停靠在标记记录位置（含容差内偏差）时以该标记为界返回更上方的标记', () => {
      expect(previousMarkerFor(list(), 500)?.id).toBe('m1')
      expect(previousMarkerFor(list(), 505)?.id).toBe('m1')
    })

    it('不在任何标记上时返回上方最近标记', () => {
      expect(previousMarkerFor(list(), 600)?.id).toBe('m2')
      expect(previousMarkerFor(list(), 300)?.id).toBe('m1')
    })

    it('第一个标记之前 / 最顶部无上一个时返回 null', () => {
      expect(previousMarkerFor(list(), 100)).toBeNull()
      expect(previousMarkerFor(list(), 0)).toBeNull()
    })

    it('hasPreviousAt 与 previousMarkerFor 判定一致', () => {
      const { api } = setup()
      api.markers.value = list()
      expect(api.hasPreviousAt(500)).toBe(true)
      expect(api.hasPreviousAt(600)).toBe(true)
      expect(api.hasPreviousAt(100)).toBe(false)
      expect(api.hasPreviousAt(0)).toBe(false)
    })
  })

  describe('重命名 / 删除', () => {
    it('renameMarker 调用接口并同步本地', async () => {
      const { api } = setup()
      api.markers.value = [marker()]
      await api.renameMarker('1', '新名字')
      expect(renameMock).toHaveBeenCalledWith('1', '新名字')
      expect(api.markers.value[0].name).toBe('新名字')
    })

    it('deleteMarker 调用接口并从本地移除', async () => {
      const { api } = setup()
      api.markers.value = [marker(), marker({ id: '2' })]
      await api.deleteMarker('1')
      expect(deleteMock).toHaveBeenCalledWith('1')
      expect(api.markers.value.map(m => m.id)).toEqual(['2'])
    })

    it('deleteAllMarkers 调用接口并清空；无会话时不调用', async () => {
      const { api } = setup()
      api.markers.value = [marker()]
      await api.deleteAllMarkers()
      expect(deleteAllMock).toHaveBeenCalledWith('conv-1')
      expect(api.markers.value).toHaveLength(0)

      const { api: api2 } = setup(null)
      await api2.deleteAllMarkers()
      expect(deleteAllMock).toHaveBeenCalledTimes(1)
    })
  })

  describe('topRelativeToContainer', () => {
    it('返回元素相对容器内容顶部的偏移（含 scrollTop）', () => {
      const el = document.createElement('div')
      const container = document.createElement('div')
      container.scrollTop = 80
      vi.spyOn(el, 'getBoundingClientRect').mockReturnValue(rect(300))
      vi.spyOn(container, 'getBoundingClientRect').mockReturnValue(rect(100))
      expect(topRelativeToContainer(el, container)).toBe(300 - 100 + 80)
    })
  })
})
