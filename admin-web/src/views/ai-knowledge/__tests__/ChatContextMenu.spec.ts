import { mount, type VueWrapper } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import ElementPlus from 'element-plus'
import ChatContextMenu from '../ChatContextMenu.vue'
import type { ChatBookmark } from '@/api/aiKnowledge'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key }),
}))

let wrapper: VueWrapper | undefined

function marker(overrides: Partial<ChatBookmark> = {}): ChatBookmark {
  return {
    id: '1',
    conversationId: 'conv-1',
    name: '问题总结',
    msgId: 'm1',
    msgOffsetTop: 0,
    scrollTop: 0,
    createdAt: '2026-01-01T00:00:00',
    ...overrides,
  }
}

function mountMenu(props: { visible?: boolean; markers?: ChatBookmark[] } = {}) {
  wrapper = mount(ChatContextMenu, {
    props: {
      visible: true,
      x: 100,
      y: 100,
      markers: [marker()],
      ...props,
    },
    global: { plugins: [ElementPlus] },
    attachTo: document.body,
  })
  return wrapper
}

function item(selector: string) {
  return document.body.querySelector<HTMLElement>(selector)
}

afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  // 清空 Teleport 到 body 的残留 DOM，避免用例间互相污染
  document.body.innerHTML = ''
})

describe('ChatContextMenu 右键菜单', () => {
  it('visible 为 false 时不渲染', async () => {
    mountMenu({ visible: false })
    await nextTick()
    expect(item('.ak-ctx-menu')).toBeNull()
  })

  it('渲染「新增标记」「标记」与「删除全部标记」（有标记时）', async () => {
    mountMenu({ markers: [marker(), marker({ id: '2', name: '第二个' })] })
    await nextTick()
    expect(item('.ak-ctx-menu')).toBeTruthy()
    // 新增标记项
    expect(item('.ak-ctx-menu:not(.ak-ctx-menu--sub) .ak-ctx-menu__item:not(.ak-ctx-menu__item--expandable):not(.is-danger)')).toBeTruthy()
    // 删除全部标记（danger）可见
    expect(item('.ak-ctx-menu:not(.ak-ctx-menu--sub) .ak-ctx-menu__item.is-danger')).toBeTruthy()
  })

  it('无标记时不显示「删除全部标记」', async () => {
    mountMenu({ markers: [] })
    await nextTick()
    expect(item('.ak-ctx-menu:not(.ak-ctx-menu--sub) .ak-ctx-menu__item.is-danger')).toBeNull()
  })

  it('点击「新增标记」emit add-marker 并 close', async () => {
    const wrap = mountMenu()
    await nextTick()
    item('.ak-ctx-menu:not(.ak-ctx-menu--sub) .ak-ctx-menu__item')!.click()
    expect(wrap.emitted('add-marker')).toBeTruthy()
    expect(wrap.emitted('close')).toBeTruthy()
  })

  it('点击「标记」展开列表，显示搜索框与全部标记项', async () => {
    mountMenu({ markers: [marker({ id: '1', name: '甲' }), marker({ id: '2', name: '乙' })] })
    await nextTick()
    item('.ak-ctx-menu__item--expandable')!.click()
    await nextTick()
    expect(item('.ak-ctx-menu__search input')).toBeTruthy()
    expect(document.body.querySelectorAll('.ak-ctx-menu__marker')).toHaveLength(2)
  })

  it('无标记时展开列表显示空态', async () => {
    mountMenu({ markers: [] })
    await nextTick()
    item('.ak-ctx-menu__item--expandable')!.click()
    await nextTick()
    expect(item('.ak-ctx-menu__empty')).toBeTruthy()
  })

  it('搜索关键词过滤标记列表', async () => {
    mountMenu({ markers: [marker({ id: '1', name: '问题总结' }), marker({ id: '2', name: '待办事项' })] })
    await nextTick()
    item('.ak-ctx-menu__item--expandable')!.click()
    await nextTick()

    const input = item('.ak-ctx-menu__search input') as HTMLInputElement
    input.value = '待办'
    input.dispatchEvent(new Event('input'))
    await nextTick()

    const names = Array.from(document.body.querySelectorAll('.ak-ctx-menu__marker-name')).map(n => n.textContent)
    expect(names).toEqual(['待办事项'])
  })

  it('点击标记项 emit jump(id) 并 close', async () => {
    const wrap = mountMenu()
    await nextTick()
    item('.ak-ctx-menu__item--expandable')!.click()
    await nextTick()
    item('.ak-ctx-menu__marker')!.click()
    expect(wrap.emitted('jump')?.[0]).toEqual(['1'])
    expect(wrap.emitted('close')).toBeTruthy()
  })

  it('右键标记项弹出子菜单，点击「重命名」emit rename(id)', async () => {
    const wrap = mountMenu()
    await nextTick()
    item('.ak-ctx-menu__item--expandable')!.click()
    await nextTick()

    const row = item('.ak-ctx-menu__marker')!
    row.dispatchEvent(new MouseEvent('contextmenu', { bubbles: true, cancelable: true }))
    await nextTick()

    const renameBtn = item('.ak-ctx-menu--sub .ak-ctx-menu__item:not(.is-danger)')!
    renameBtn.click()
    expect(wrap.emitted('rename')?.[0]).toEqual(['1'])
    expect(wrap.emitted('close')).toBeTruthy()
  })

  it('右键标记项选择「删除标记」emit delete(id)', async () => {
    const wrap = mountMenu()
    await nextTick()
    item('.ak-ctx-menu__item--expandable')!.click()
    await nextTick()

    const row = item('.ak-ctx-menu__marker')!
    row.dispatchEvent(new MouseEvent('contextmenu', { bubbles: true, cancelable: true }))
    await nextTick()

    const deleteBtn = item('.ak-ctx-menu--sub .ak-ctx-menu__item.is-danger')!
    deleteBtn.click()
    expect(wrap.emitted('delete')?.[0]).toEqual(['1'])
    expect(wrap.emitted('close')).toBeTruthy()
  })

  it('点击「删除全部标记」emit delete-all 并 close', async () => {
    const wrap = mountMenu()
    await nextTick()
    item('.ak-ctx-menu:not(.ak-ctx-menu--sub) .ak-ctx-menu__item.is-danger')!.click()
    expect(wrap.emitted('delete-all')).toBeTruthy()
    expect(wrap.emitted('close')).toBeTruthy()
  })
})
