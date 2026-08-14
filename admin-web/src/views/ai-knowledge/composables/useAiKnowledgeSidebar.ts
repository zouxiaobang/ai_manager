import { ref } from 'vue'

/**
 * 智能问答页左侧对话列表的收起/展开状态
 * 纯 UI 状态：不持久化，切换标签页时组件保持挂载、状态保留
 */
export function useAiKnowledgeSidebar() {
  /** 对话列表是否收起（收起后仅保留左侧窄条展开入口） */
  const sidebarCollapsed = ref(false)

  function collapseSidebar() {
    sidebarCollapsed.value = true
  }

  function expandSidebar() {
    sidebarCollapsed.value = false
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return { sidebarCollapsed, collapseSidebar, expandSidebar, toggleSidebar }
}
