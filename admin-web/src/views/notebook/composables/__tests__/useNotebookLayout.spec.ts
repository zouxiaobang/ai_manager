import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { useNotebookLayout } from '../useNotebookLayout'

/** 构造可控的 matchMedia 假对象（matches 可变，监听函数为 spy） */
function createMql(initialMatches: boolean) {
  return {
    matches: initialMatches,
    media: '',
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }
}

type LayoutApi = {
  isTabletRange: boolean
  isCompactRange: boolean
  tocVisible: boolean
  sidebarVisible: boolean
  sidebarAriaExpanded: string | undefined
  tabsCollapsed: boolean
  editorTitleShifted: boolean
  onTabletRangeChange: (e: MediaQueryListEvent) => void
  onCompactRangeChange: (e: MediaQueryListEvent) => void
  onEscapeKeyForSidebar: (e: KeyboardEvent) => void
  applyShortViewport: () => void
}

let mql: ReturnType<typeof createMql>

function mountLayout() {
  const wrapper = mount(defineComponent({
    setup() {
      return useNotebookLayout()
    },
    template: '<div/>',
  }))
  return wrapper
}

function vmOf(wrapper: ReturnType<typeof mountLayout>) {
  return wrapper.vm as unknown as LayoutApi
}

describe('useNotebookLayout 响应式布局', () => {
  beforeEach(() => {
    mql = createMql(false)
    vi.stubGlobal('matchMedia', vi.fn(() => mql))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('宽视口初始状态：目录与侧栏展开、不折叠、标题不偏移', () => {
    const api = vmOf(mountLayout())
    expect(api.isTabletRange).toBe(false)
    expect(api.isCompactRange).toBe(false)
    expect(api.tocVisible).toBe(true)
    expect(api.sidebarVisible).toBe(true)
    expect(api.tabsCollapsed).toBe(false)
    expect(api.editorTitleShifted).toBe(false)
    expect(api.sidebarAriaExpanded).toBeUndefined()
  })

  it('窄视口初始状态：目录与侧栏收起、标签折叠、标题偏移', () => {
    mql.matches = true
    const api = vmOf(mountLayout())
    expect(api.isTabletRange).toBe(true)
    expect(api.isCompactRange).toBe(true)
    expect(api.tocVisible).toBe(false)
    expect(api.sidebarVisible).toBe(false)
    expect(api.tabsCollapsed).toBe(true)
    expect(api.editorTitleShifted).toBe(true)
    expect(api.sidebarAriaExpanded).toBe('false')
  })

  describe('onTabletRangeChange', () => {
    it('进入平板档折叠目录', () => {
      const api = vmOf(mountLayout())
      api.onTabletRangeChange({ matches: true } as MediaQueryListEvent)
      expect(api.isTabletRange).toBe(true)
      expect(api.tocVisible).toBe(false)
    })

    it('退出平板档仅更新范围，不自动展开目录', () => {
      mql.matches = true
      const api = vmOf(mountLayout())
      api.tocVisible = false
      api.onTabletRangeChange({ matches: false } as MediaQueryListEvent)
      expect(api.isTabletRange).toBe(false)
      expect(api.tocVisible).toBe(false)
    })
  })

  describe('onCompactRangeChange', () => {
    it('进入紧凑档收起侧栏', () => {
      const api = vmOf(mountLayout())
      api.onCompactRangeChange({ matches: true } as MediaQueryListEvent)
      expect(api.isCompactRange).toBe(true)
      expect(api.sidebarVisible).toBe(false)
    })

    it('退出紧凑档仅更新范围', () => {
      mql.matches = true
      const api = vmOf(mountLayout())
      api.onCompactRangeChange({ matches: false } as MediaQueryListEvent)
      expect(api.isCompactRange).toBe(false)
      expect(api.sidebarVisible).toBe(false)
    })
  })

  describe('onEscapeKeyForSidebar', () => {
    it('窄档抽屉打开时 Escape 关闭', () => {
      mql.matches = true
      const api = vmOf(mountLayout())
      api.sidebarVisible = true
      api.onEscapeKeyForSidebar({ key: 'Escape' } as KeyboardEvent)
      expect(api.sidebarVisible).toBe(false)
    })

    it('桌面档 Escape 不影响侧栏', () => {
      const api = vmOf(mountLayout())
      api.sidebarVisible = true
      api.onEscapeKeyForSidebar({ key: 'Escape' } as KeyboardEvent)
      expect(api.sidebarVisible).toBe(true)
    })

    it('非 Escape 键不影响', () => {
      mql.matches = true
      const api = vmOf(mountLayout())
      api.sidebarVisible = true
      api.onEscapeKeyForSidebar({ key: 'a' } as KeyboardEvent)
      expect(api.sidebarVisible).toBe(true)
    })
  })

  describe('applyShortViewport', () => {
    it('按矮视口 matches 更新折叠态', () => {
      const api = vmOf(mountLayout())
      expect(api.tabsCollapsed).toBe(false)

      mql.matches = true
      api.applyShortViewport()
      expect(api.tabsCollapsed).toBe(true)

      mql.matches = false
      api.applyShortViewport()
      expect(api.tabsCollapsed).toBe(false)
    })
  })

  describe('editorTitleShifted', () => {
    it('紧凑档抽屉收起时标题右移', () => {
      const api = vmOf(mountLayout())
      expect(api.editorTitleShifted).toBe(false)
      api.onCompactRangeChange({ matches: true } as MediaQueryListEvent)
      expect(api.editorTitleShifted).toBe(true)
      api.sidebarVisible = true
      api.onCompactRangeChange({ matches: false } as MediaQueryListEvent)
      expect(api.editorTitleShifted).toBe(false)
    })
  })

  describe('监听生命周期', () => {
    it('挂载时注册三个 matchMedia 监听', () => {
      mountLayout()
      expect(mql.addEventListener).toHaveBeenCalledTimes(3)
    })

    it('卸载时移除三个 matchMedia 监听', () => {
      const wrapper = mountLayout()
      wrapper.unmount()
      expect(mql.removeEventListener).toHaveBeenCalledTimes(3)
    })
  })
})
