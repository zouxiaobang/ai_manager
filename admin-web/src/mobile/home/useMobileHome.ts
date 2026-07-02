import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { fetchHealth } from '@/api/health'
import { fetchNotebookTree, type NbTreeNode } from '@/api/notebook'
import { fetchTodayTodos, updateTodo, type NbTodoItem } from '@/api/notebook/todo'
import type { HealthData } from '@/api/types'
import { mobileHomeModules, type MobileHomeModule } from '@/data/mobile-home-modules'
import { warRoomIconUrl } from '@/data/war-room-icons'
import { dismissTodoNotification, useTodoReminders } from '@/composables/useTodoReminders'
import { setAppShellPreference } from '@/utils/deviceShell'

export type HealthState = 'up' | 'down' | 'unknown'

export function useMobileHome() {
  const router = useRouter()
  const { t } = useI18n()
  const { refreshTodayCount } = useTodoReminders()

  const loading = ref(false)
  const searchQuery = ref('')
  const healthStatus = ref<HealthState>('unknown')
  const healthData = ref<HealthData | null>(null)
  const todayTodos = ref<NbTodoItem[]>([])
  const noteCount = ref(0)

  const normalizedSearch = computed(() => searchQuery.value.trim().toLowerCase())
  const todoDoneCount = computed(() => todayTodos.value.filter((row) => row.completed === 1).length)
  const todoTotalCount = computed(() => todayTodos.value.length)
  const todoPendingCount = computed(() =>
    todayTodos.value.filter((row) => row.completed !== 1).length,
  )
  const todoProgressRatio = computed(() => {
    if (todoTotalCount.value === 0) return 0
    return todoDoneCount.value / todoTotalCount.value
  })
  const todoProgressText = computed(() => `${todoDoneCount.value}/${todoTotalCount.value || 0}`)

  const systemOverviewText = computed(() => {
    if (healthStatus.value === 'up') return t('mobile.home.systemOk')
    if (healthStatus.value === 'down') return t('mobile.home.systemFail')
    return t('mobile.home.systemUnknown')
  })

  const systemBadgeClass = computed(() => {
    if (healthStatus.value === 'up') return 'is-up'
    if (healthStatus.value === 'down') return 'is-down'
    return 'is-unknown'
  })

  const serviceNodes = computed(() => {
    const backendUp = healthStatus.value === 'up'
    const redisUp = healthData.value?.redis === 'UP'
    const redisDown = healthData.value?.redis === 'DOWN'
    const redisState: HealthState = redisUp ? 'up' : redisDown ? 'down' : 'unknown'
    const mysqlUp = healthData.value?.mysql === 'UP'
    const mysqlDown = healthData.value?.mysql === 'DOWN'
    const mysqlState: HealthState = mysqlUp ? 'up' : mysqlDown ? 'down' : 'unknown'
    const gatewayState: HealthState = backendUp ? 'up' : healthStatus.value === 'down' ? 'down' : 'unknown'

    return [
      { key: 'api', label: t('mobile.home.serviceApi'), state: healthStatus.value },
      { key: 'db', label: t('mobile.home.serviceDb'), state: mysqlState },
      { key: 'cache', label: t('mobile.home.serviceCache'), state: redisState },
      { key: 'gateway', label: t('mobile.home.serviceGateway'), state: gatewayState },
    ]
  })

  function matchesSearch(text: string) {
    if (!normalizedSearch.value) return true
    return text.toLowerCase().includes(normalizedSearch.value)
  }

  const filteredModules = computed(() => {
    if (!normalizedSearch.value) return mobileHomeModules
    return mobileHomeModules.filter((item) => {
      const name = moduleName(item)
      const desc = moduleDesc(item)
      const keys = item.searchKeys?.join(' ') ?? ''
      return matchesSearch(name) || matchesSearch(desc) || matchesSearch(keys)
    })
  })

  const displayTodos = computed(() => {
    const rows = todayTodos.value
    if (!normalizedSearch.value) return rows.slice(0, 3)
    return rows.filter((item) => matchesSearch(item.content)).slice(0, 5)
  })

  const showOverview = computed(() => !normalizedSearch.value)
  const showFunctionsSection = computed(() => !normalizedSearch.value || filteredModules.value.length > 0)
  const showTodosSection = computed(() => !normalizedSearch.value || displayTodos.value.length > 0)

  const summaryPillText = computed(
    () =>
      `${t('portal.menu.todos')} ${todoPendingCount.value} · ${t('portal.menu.notebook')} ${noteCount.value}`,
  )

  function iconUrl(name: string) {
    return warRoomIconUrl('modules', name)
  }

  function moduleName(item: MobileHomeModule) {
    return t(item.nameKey)
  }

  function moduleDesc(item: MobileHomeModule) {
    return t(item.descKey)
  }

  function moduleCardStyle(item: MobileHomeModule) {
    return {
      borderColor: `${item.barColor}55`,
      background: `${item.barColor}12`,
      boxShadow: `0 3px 12px ${item.barColor}30`,
      '--module-accent': item.barColor,
    }
  }

  function statusText(state: HealthState) {
    if (state === 'up') return t('mobile.home.statusNormal')
    if (state === 'down') return t('mobile.home.statusAbnormal')
    return t('mobile.home.statusUnknown')
  }

  function countNotes(nodes: NbTreeNode[]): number {
    let total = 0
    for (const node of nodes) {
      if (node.nodeType === 'NOTE') total += 1
      if (node.children?.length) total += countNotes(node.children)
    }
    return total
  }

  function todoTimeText(item: NbTodoItem) {
    const raw = item.dueTime ?? item.remindTime
    if (!raw) return ''
    const normalized = raw.replace('T', ' ')
    const timePart = normalized.slice(11, 16)
    if (timePart && timePart !== '00:00') return timePart
    return normalized.slice(0, 10)
  }

  function openDesktop(path: string) {
    setAppShellPreference('pc')
    const base = import.meta.env.BASE_URL || '/'
    window.location.href = `${base}index.html#${path}`
  }

  function openModule(item: MobileHomeModule) {
    if (item.route) {
      router.push(item.route)
      return
    }
    if (item.desktopPath) {
      ElMessage.info(t('mobile.home.openDesktopHint', { name: moduleName(item) }))
      openDesktop(item.desktopPath)
      return
    }
    ElMessage.info(t('functions.openSoon', { name: moduleName(item) }))
  }

  async function loadData() {
    loading.value = true
    try {
      const [health, todos, tree] = await Promise.all([
        fetchHealth(),
        fetchTodayTodos(),
        fetchNotebookTree().catch(() => [] as NbTreeNode[]),
      ])
      healthData.value = health
      healthStatus.value = health.status === 'UP' ? 'up' : 'down'
      todayTodos.value = todos
      noteCount.value = countNotes(tree)
    } catch {
      healthStatus.value = 'down'
      healthData.value = null
      todayTodos.value = []
      noteCount.value = 0
    } finally {
      loading.value = false
    }
  }

  async function onToggle(item: NbTodoItem, checked: boolean) {
    try {
      await updateTodo(item.id, { completed: checked })
      if (checked) {
        dismissTodoNotification(item.id)
        todayTodos.value = todayTodos.value.filter((row) => row.id !== item.id)
      } else {
        const row = todayTodos.value.find((entry) => entry.id === item.id)
        if (row) row.completed = 0
      }
      await refreshTodayCount()
    } catch {
      ElMessage.error(t('notebook.todos.saveFailed'))
    }
  }

  onMounted(() => {
    void loadData()
  })

  return {
    router,
    t,
    loading,
    searchQuery,
    healthStatus,
    noteCount,
    todoProgressRatio,
    todoProgressText,
    todoPendingCount,
    systemOverviewText,
    systemBadgeClass,
    serviceNodes,
    filteredModules,
    displayTodos,
    showOverview,
    showFunctionsSection,
    showTodosSection,
    summaryPillText,
    iconUrl,
    moduleName,
    moduleDesc,
    moduleCardStyle,
    statusText,
    todoTimeText,
    openModule,
    onToggle,
  }
}
