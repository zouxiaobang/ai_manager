import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { useAiKnowledgeSidebar } from '../useAiKnowledgeSidebar'

/** 构造可控的 matchMedia 假对象（matches 可变，监听函数为 spy） */
function createMql(initialMatches: boolean) {
  return {
    matches: initialMatches,
    media: '',
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }
}

type SidebarApi = {
  sidebarCollapsed: boolean
  collapseTipVisible: boolean
  collapseSidebar: () => void
  expandSidebar: () => void
  toggleSidebar: () => void
  handleCollapseSidebar: () => void
}

let mql: ReturnType<typeof createMql>

function mountSidebar() {
  const wrapper = mount(
    defineComponent({
      setup() {
        return useAiKnowledgeSidebar()
      },
      template: '<div/>',
    }),
  )
  return wrapper
}

function vmOf(wrapper: ReturnType<typeof mountSidebar>) {
  return wrapper.vm as unknown as SidebarApi
}

/** 取挂载时注册的 media query change 监听，模拟档位切换 */
function changeListenerOf() {
  return mql.addEventListener.mock.calls[0]?.[1] as (e: MediaQueryListEvent) => void
}

describe('useAiKnowledgeSidebar 收起/展开状态', () => {
  beforeEach(() => {
    mql = createMql(false) // 默认宽视口
    vi.stubGlobal('matchMedia', vi.fn(() => mql))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('宽视口初始展开（对话列表可见）', () => {
    const api = vmOf(mountSidebar())
    expect(api.sidebarCollapsed).toBe(false)
  })

  it('平板视口初始收起', () => {
    mql.matches = true
    const api = vmOf(mountSidebar())
    expect(api.sidebarCollapsed).toBe(true)
  })

  it('视口进入平板档时自动收起', () => {
    const wrapper = mountSidebar()
    const api = vmOf(wrapper)
    expect(api.sidebarCollapsed).toBe(false)

    changeListenerOf()({ matches: true } as MediaQueryListEvent)
    expect(api.sidebarCollapsed).toBe(true)
  })

  it('离开平板档不强制展开（保留用户手动选择）', () => {
    mql.matches = true
    const wrapper = mountSidebar()
    const api = vmOf(wrapper)
    api.expandSidebar() // 用户手动展开
    expect(api.sidebarCollapsed).toBe(false)

    changeListenerOf()({ matches: false } as MediaQueryListEvent)
    expect(api.sidebarCollapsed).toBe(false)
  })

  it('点击收起时同时隐藏收起按钮 tooltip', () => {
    const api = vmOf(mountSidebar())
    api.collapseTipVisible = true // 模拟 hover 已显示 tip
    api.handleCollapseSidebar()
    expect(api.sidebarCollapsed).toBe(true)
    expect(api.collapseTipVisible).toBe(false)
  })

  it('collapseSidebar 收起对话列表', () => {
    const api = vmOf(mountSidebar())
    api.collapseSidebar()
    expect(api.sidebarCollapsed).toBe(true)
  })

  it('expandSidebar 展开对话列表', () => {
    mql.matches = true
    const api = vmOf(mountSidebar())
    api.expandSidebar()
    expect(api.sidebarCollapsed).toBe(false)
  })

  it('toggleSidebar 反复切换收起/展开', () => {
    const api = vmOf(mountSidebar())
    api.toggleSidebar()
    expect(api.sidebarCollapsed).toBe(true)
    api.toggleSidebar()
    expect(api.sidebarCollapsed).toBe(false)
  })

  it('挂载时注册 media query 监听、卸载时移除', () => {
    const wrapper = mountSidebar()
    expect(mql.addEventListener).toHaveBeenCalledTimes(1)
    wrapper.unmount()
    expect(mql.removeEventListener).toHaveBeenCalledTimes(1)
  })
})
