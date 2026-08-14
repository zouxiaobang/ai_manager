import { ref, watch } from 'vue'
import type { Ref } from 'vue'
import {
  fetchChatBookmarks,
  createChatBookmark,
  renameChatBookmark,
  deleteChatBookmark,
  deleteAllChatBookmarks,
} from '@/api/aiKnowledge'
import type { ChatBookmark } from '@/api/aiKnowledge'

/** useAiKnowledgeMarkers 依赖注入：标记状态机与聊天/会话状态机解耦，由调用方注入 */
export interface AiKnowledgeMarkersDeps {
  /** 当前激活会话 id；切换会话时据此重新加载/清空标记 */
  activeConvId: Ref<string | null>
  /** 聊天消息滚动容器（.ak-chat__messages），标记定位依赖其滚动位置与渲染结构 */
  chatMessagesRef: Ref<HTMLElement | null>
}

/** 标题型标记名称的最大长度（超长标题截断） */
const MARKER_NAME_MAX = 30

/**
 * 元素相对容器内容顶部的偏移（像素）。
 * 用 getBoundingClientRect 差值抵消嵌套布局差异（jsdom 下由测试 stub 该 API）；
 * 加上 container.scrollTop 后得到的是相对容器「内容」的偏移，不随滚动变化。
 */
export function topRelativeToContainer(el: HTMLElement, container: HTMLElement): number {
  const elTop = el.getBoundingClientRect().top
  const cTop = container.getBoundingClientRect().top
  return elTop - cTop + container.scrollTop
}

/**
 * 检测当前视口内「最大级别」的标题（H1 > H2 > …）。
 * 遍历容器内 h1..h6，取可见（viewTop ∈ [0, clientHeight]）、文本非空、
 * 层级最小者（同层按文档序优先）；无可见标题返回 null。
 */
export function detectTopHeading(container: HTMLElement): string | null {
  const heads = Array.from(container.querySelectorAll<HTMLElement>('h1,h2,h3,h4,h5,h6'))
  let best: string | null = null
  let bestLevel = Infinity
  for (const h of heads) {
    const text = (h.textContent ?? '').trim()
    if (!text) continue
    const level = Number(h.tagName.charAt(1))
    const viewTop = topRelativeToContainer(h, container) - container.scrollTop
    if (viewTop >= 0 && viewTop <= container.clientHeight && level < bestLevel) {
      bestLevel = level
      best = text.slice(0, MARKER_NAME_MAX)
    }
  }
  return best
}

/**
 * 判定「当前停靠在标记上」的容差（px）。
 * 跳到标记后的实际 scrollTop 因重锚定/布局微偏移会略偏离记录值，若用严格小于比较，
 * 会把自己所在的标记误判为「上一个」导致卡住回不去；容差覆盖该偏移。
 */
const DOCK_TOLERANCE = 16

/**
 * 计算某滚动位置处「上一个」标记：
 * 1) 若 scrollTop 与某标记记录位置在容差内，视为「停靠」在该标记上 → 以它记录位置为界取更上方的标记；
 * 2) 否则取严格位于该滚动位置上方的最近标记。
 * 返回 null 表示没有上一个标记。
 */
export function previousMarkerFor(markers: ChatBookmark[], scrollTop: number): ChatBookmark | null {
  let docked = -1
  for (let i = 0; i < markers.length; i++) {
    if (Math.abs(markers[i].scrollTop - scrollTop) <= DOCK_TOLERANCE) {
      docked = i
      break
    }
  }
  const boundary = docked >= 0 ? markers[docked].scrollTop : scrollTop
  let prev: ChatBookmark | null = null
  for (const m of markers) {
    if (m.scrollTop < boundary && (prev === null || m.scrollTop > prev.scrollTop)) {
      prev = m
    }
  }
  return prev
}

/**
 * AI 知识页对话标记状态机。
 *
 * <p>为聊天区提供「标记」能力：每个标记记录当前滚动位置（另存锚点消息用于跳转时重锚定，
 * 抵消会话内折叠/展开造成的偏移），名称默认取可见最大级别标题。数据存后端，
 * 随会话持久化，刷新/跨设备可见。</p>
 */
export function useAiKnowledgeMarkers(deps: AiKnowledgeMarkersDeps) {
  const { activeConvId, chatMessagesRef } = deps

  const markers = ref<ChatBookmark[]>([])
  const markersLoading = ref(false)

  /** 加载序号：会话快速切换时丢弃过期响应，避免旧会话标记串到新会话 */
  let loadSeq = 0

  /** 加载当前会话标记；无会话则清空 */
  async function loadMarkers() {
    const convId = activeConvId.value
    const seq = ++loadSeq
    if (!convId) {
      markers.value = []
      return
    }
    markersLoading.value = true
    try {
      const list = await fetchChatBookmarks(convId)
      if (seq === loadSeq) markers.value = list
    } finally {
      if (seq === loadSeq) markersLoading.value = false
    }
  }

  // 会话切换 → 重新加载（初始化时无会话先清空）
  watch(activeConvId, () => {
    void loadMarkers()
  }, { immediate: true })

  /**
   * 在当前滚动位置新增标记。
   * 名称优先取当前视口内最大级别标题；无标题时回落调用方传入的默认名。
   * 返回新建标记（含后端回填的 id），失败返回 null（调用方负责错误提示）。
   */
  async function addMarker(fallbackName: string): Promise<ChatBookmark | null> {
    const convId = activeConvId.value
    const el = chatMessagesRef.value
    if (!convId || !el) return null
    // 本地增删使进行中的加载失效，避免慢响应覆盖新状态
    loadSeq++

    const scrollTop = el.scrollTop
    // 锚点消息：内容偏移首次盖住视口顶线的消息行（其内容正处于当前可视位置）
    let msgId: string | null = null
    let msgOffsetTop = 0
    const rows = Array.from(el.querySelectorAll<HTMLElement>('.ak-chat__msg-row[data-msg-id]'))
    for (const row of rows) {
      const anchor = topRelativeToContainer(row, el)
      if (anchor + row.offsetHeight >= scrollTop) {
        msgId = row.getAttribute('data-msg-id')
        msgOffsetTop = anchor
        break
      }
    }

    const created = await createChatBookmark(convId, {
      name: detectTopHeading(el) ?? fallbackName,
      msgId,
      msgOffsetTop,
      scrollTop,
    })
    markers.value = [...markers.value, created]
    return created
  }

  /** 重命名标记并同步本地状态 */
  async function renameMarker(id: string, name: string) {
    loadSeq++
    await renameChatBookmark(id, name)
    const marker = markers.value.find(m => m.id === id)
    if (marker) marker.name = name
  }

  /** 删除单个标记并同步本地状态 */
  async function deleteMarker(id: string) {
    loadSeq++
    await deleteChatBookmark(id)
    markers.value = markers.value.filter(m => m.id !== id)
  }

  /** 删除当前会话全部标记 */
  async function deleteAllMarkers() {
    const convId = activeConvId.value
    if (!convId) return
    loadSeq++
    await deleteAllChatBookmarks(convId)
    markers.value = []
  }

  /**
   * 跳转到标记位置：锚点消息仍在则按「当前锚点偏移 + (记录滚动 - 记录锚点偏移)」重锚定，
   * 抵消标记后折叠/展开造成的整体偏移；锚点丢失回退绝对滚动位置，最后钳制到合法区间。
   */
  function jumpToMarker(id: string) {
    const el = chatMessagesRef.value
    const marker = markers.value.find(m => m.id === id)
    if (!el || !marker) return

    let target = marker.scrollTop
    if (marker.msgId) {
      const row = el.querySelector<HTMLElement>(`[data-msg-id="${marker.msgId}"]`)
      if (row) {
        target = topRelativeToContainer(row, el) + (marker.scrollTop - marker.msgOffsetTop)
      }
    }
    el.scrollTop = Math.max(0, Math.min(target, el.scrollHeight - el.clientHeight))
  }

  /**
   * 跳到当前视口处的「上一个」标记（「回到上一个标签」）。
   * 停靠于某标记时回它的上一个，否则回上方最近标记；无上一个或无容器返回 false。
   */
  function jumpToPreviousMarker(): boolean {
    const el = chatMessagesRef.value
    if (!el) return false
    const prev = previousMarkerFor(markers.value, el.scrollTop)
    if (!prev) return false
    jumpToMarker(prev.id)
    return true
  }

  /** 指定滚动位置上方是否存在「上一个」标记（菜单可用性判定，与 jumpToPreviousMarker 同源逻辑） */
  function hasPreviousAt(scrollTop: number): boolean {
    return previousMarkerFor(markers.value, scrollTop) !== null
  }

  return {
    markers,
    markersLoading,
    loadMarkers,
    addMarker,
    renameMarker,
    deleteMarker,
    deleteAllMarkers,
    jumpToMarker,
    jumpToPreviousMarker,
    hasPreviousAt,
  }
}
