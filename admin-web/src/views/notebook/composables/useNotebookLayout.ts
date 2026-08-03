import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { TABLET_COMPACT_MAX_WIDTH, TABLET_MAX_WIDTH } from '@/utils/deviceShell'

/**
 * 笔记本页面响应式布局状态机
 *
 * <p>从 {@code NotebookView.vue} 提取：平板/紧凑/矮视口三类 media query 驱动的
 * 布局状态（目录、侧栏、标签页折叠、标题行偏移）与配套监听注册。</p>
 *
 * 无共享外部依赖，所有 ref 在本 composable 内创建，由组件解构后绑定模板。
 */
export function useNotebookLayout() {
  // 平板中宽档（PC 壳内响应式，769–1200px）：TOC 默认折叠，可手动展开
  const tabletMql = typeof window !== 'undefined' ? window.matchMedia(`(max-width: ${TABLET_MAX_WIDTH}px)`) : null
  const isTabletRange = ref(tabletMql?.matches ?? false)
  // 平板窄档（≤1300px）：左侧树改为原位抽屉（1201–1300 时 TOC 仍显示，≤1200 才折叠）
  const compactMql = typeof window !== 'undefined' ? window.matchMedia(`(max-width: ${TABLET_COMPACT_MAX_WIDTH}px)`) : null
  const isCompactRange = ref(compactMql?.matches ?? false)
  const tocVisible = ref(!isTabletRange.value) // 目录是否可见（平板档默认折叠）
  const sidebarVisible = ref(!isCompactRange.value) // 侧栏是否可见（窄档默认收起为抽屉）
  // 抽屉 aria-expanded：桌面档不渲染该属性，窄档按抽屉开合输出 true/false
  const sidebarAriaExpanded = computed(() => (isCompactRange.value ? (sidebarVisible.value ? 'true' : 'false') : undefined))
  function onTabletRangeChange(e: MediaQueryListEvent) {
    isTabletRange.value = e.matches
    if (e.matches) tocVisible.value = false
  }
  function onCompactRangeChange(e: MediaQueryListEvent) {
    isCompactRange.value = e.matches
    if (e.matches) sidebarVisible.value = false
  }
  function onEscapeKeyForSidebar(e: KeyboardEvent) {
    if (e.key === 'Escape' && isCompactRange.value && sidebarVisible.value) {
      sidebarVisible.value = false
    }
  }

  // 矮视口（高度 ≤ 900px）：标签页整行收起为小图标按钮
  const shortViewportMql = typeof window !== 'undefined' ? window.matchMedia('(max-height: 900px)') : null
  const tabsCollapsed = ref(shortViewportMql?.matches ?? false)
  // 编辑器标题行是否右移：左上角存在悬浮按钮（紧凑档抽屉开关 / 矮视口标签页开关）时，
  // 标题、时间字数、置顶等按钮会被遮挡，右移避开。
  const editorTitleShifted = computed(
    () => (isCompactRange.value && !sidebarVisible.value) || tabsCollapsed.value,
  )

  function applyShortViewport() {
    tabsCollapsed.value = shortViewportMql?.matches ?? false
  }

  applyShortViewport()
  onMounted(() => {
    tabletMql?.addEventListener('change', onTabletRangeChange)
    compactMql?.addEventListener('change', onCompactRangeChange)
    shortViewportMql?.addEventListener('change', applyShortViewport)
    if (typeof window !== 'undefined') {
      // 兜底：部分内嵌 WebView / 旧浏览器 matchMedia change 事件不可靠，窗口 resize 时重新应用
      window.addEventListener('resize', applyShortViewport)
      window.addEventListener('keydown', onEscapeKeyForSidebar)
    }
  })
  onBeforeUnmount(() => {
    tabletMql?.removeEventListener('change', onTabletRangeChange)
    compactMql?.removeEventListener('change', onCompactRangeChange)
    shortViewportMql?.removeEventListener('change', applyShortViewport)
    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', applyShortViewport)
      window.removeEventListener('keydown', onEscapeKeyForSidebar)
    }
  })

  return {
    isTabletRange,
    isCompactRange,
    tocVisible,
    sidebarVisible,
    sidebarAriaExpanded,
    tabsCollapsed,
    editorTitleShifted,
    onTabletRangeChange,
    onCompactRangeChange,
    onEscapeKeyForSidebar,
    applyShortViewport,
  }
}
