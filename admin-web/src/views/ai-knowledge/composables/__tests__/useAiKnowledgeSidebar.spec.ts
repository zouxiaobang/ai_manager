import { describe, expect, it } from 'vitest'
import { useAiKnowledgeSidebar } from '../useAiKnowledgeSidebar'

describe('useAiKnowledgeSidebar', () => {
  it('初始为展开状态（对话列表可见）', () => {
    const { sidebarCollapsed } = useAiKnowledgeSidebar()
    expect(sidebarCollapsed.value).toBe(false)
  })

  it('collapseSidebar 收起对话列表', () => {
    const { sidebarCollapsed, collapseSidebar } = useAiKnowledgeSidebar()
    collapseSidebar()
    expect(sidebarCollapsed.value).toBe(true)
  })

  it('expandSidebar 展开对话列表', () => {
    const { sidebarCollapsed, collapseSidebar, expandSidebar } = useAiKnowledgeSidebar()
    collapseSidebar()
    expandSidebar()
    expect(sidebarCollapsed.value).toBe(false)
  })

  it('toggleSidebar 反复切换收起/展开', () => {
    const { sidebarCollapsed, toggleSidebar } = useAiKnowledgeSidebar()
    toggleSidebar()
    expect(sidebarCollapsed.value).toBe(true)
    toggleSidebar()
    expect(sidebarCollapsed.value).toBe(false)
  })
})
