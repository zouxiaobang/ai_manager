import { onBeforeUnmount, onMounted, ref } from 'vue'
import { TABLET_MAX_WIDTH } from '@/utils/deviceShell'

/**
 * 智能问答页左侧对话列表的收起/展开状态
 *
 * 平板档（视口宽度 ≤ TABLET_MAX_WIDTH）默认收起，宽屏默认展开；
 * 视口宽度进入平板档时自动收起，离开平板档不强制展开（尊重用户手动选择）。
 * 纯 UI 状态：不持久化，切换标签页时组件保持挂载、状态保留。
 */
export function useAiKnowledgeSidebar() {
  /** 对话列表是否收起（收起后仅保留左侧窄条展开入口） */
  const sidebarCollapsed = ref(false)

  // 平板档 media query：初始收起/展开依赖视口宽度
  // typeof 防御：SSR / 无 matchMedia 的测试环境回落为宽屏（展开）
  const tabletMql =
    typeof window !== 'undefined' && typeof window.matchMedia === 'function'
      ? window.matchMedia(`(max-width: ${TABLET_MAX_WIDTH}px)`)
      : null
  sidebarCollapsed.value = tabletMql?.matches ?? false

  // 仅处理"进入平板档"：自动收起；离开平板档不强制展开
  function onTabletRangeChange(e: MediaQueryListEvent) {
    if (e.matches) sidebarCollapsed.value = true
  }

  function collapseSidebar() {
    sidebarCollapsed.value = true
  }

  function expandSidebar() {
    sidebarCollapsed.value = false
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  /** 收起按钮的 tooltip 是否显示（受控：点击收起后侧边栏消失、hover 无法自然结束，需立即隐藏） */
  const collapseTipVisible = ref(false)

  /** 点击收起按钮：先隐藏按钮 tooltip，再收起侧边栏 */
  function handleCollapseSidebar() {
    collapseTipVisible.value = false
    collapseSidebar()
  }

  onMounted(() => {
    tabletMql?.addEventListener('change', onTabletRangeChange)
  })
  onBeforeUnmount(() => {
    tabletMql?.removeEventListener('change', onTabletRangeChange)
  })

  return { sidebarCollapsed, collapseTipVisible, collapseSidebar, expandSidebar, toggleSidebar, handleCollapseSidebar }
}
