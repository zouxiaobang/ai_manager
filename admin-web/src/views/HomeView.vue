<template>
  <div class="dashboard">
    <h2 class="dashboard-page-title">{{ t('portal.dashboard.title') }}</h2>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="dashboard-card">
          <h3 class="dashboard-section-title">🛡️ {{ t('portal.dashboard.systems') }}</h3>
          <div class="system-status-item">
            <span>{{ t('portal.dashboard.aiManager') }}</span>
            <el-space>
              <el-tag :type="healthTagType" size="small">{{ healthLabel }}</el-tag>
              <el-button size="small" link :loading="healthLoading" @click="checkHealth">
                {{ t('home.healthCheck') }}
              </el-button>
            </el-space>
          </div>
          <div v-for="sys in systemBoard" :key="sys.key" class="system-status-item">
            <span>{{ t(`portal.dashboard.systemsList.${sys.key}`) }}</span>
            <el-tag :type="sys.tagType" size="small">
              {{ t(`portal.dashboard.systemsStatus.${sys.statusKey}`, { count: sys.count }) }}
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="dashboard-card">
          <h3 class="dashboard-section-title">🔔 {{ t('portal.dashboard.notices') }}</h3>
          <div v-for="(notice, idx) in noticeKeys" :key="idx" class="notice-item">
            {{ t(`portal.dashboard.noticeItems.${notice}.title`) }}
            <span class="notice-item__date">
              [{{ t(`portal.dashboard.noticeItems.${notice}.date`) }}]
            </span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-loading="todayTodosLoading" shadow="never" class="dashboard-card">
      <div class="dashboard-card__header">
        <h3 class="dashboard-section-title">📋 {{ t('portal.dashboard.todayTodos') }}</h3>
        <router-link class="dashboard-card__link" to="/notebook?tab=todos&filter=today">
          {{ t('portal.dashboard.viewAllTodayTodos') }}
        </router-link>
      </div>
      <div v-if="todayTodos.length" class="today-todo-list">
        <div v-for="item in todayTodos" :key="item.id" class="today-todo-item">
          <el-checkbox
            :model-value="item.completed === 1"
            @change="(checked: boolean) => onToggleTodayTodo(item, checked)"
          />
          <div class="today-todo-item__body">
            <div class="today-todo-item__title">{{ item.content }}</div>
            <div class="today-todo-item__meta">
              <span v-if="item.dueTime">
                {{ t('notebook.todos.dueTime') }}：{{ formatDateTime(item.dueTime) }}
              </span>
              <span v-if="item.remindTime">
                {{ t('notebook.todos.remindTime') }}：{{ formatDateTime(item.remindTime) }}
              </span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else :description="t('portal.dashboard.todayTodosEmpty')" :image-size="72" />
    </el-card>

    <el-card shadow="never" class="dashboard-card">
      <h3 class="dashboard-section-title">🚀 {{ t('portal.dashboard.allSystems') }}</h3>
      <el-row :gutter="16">
        <el-col
          v-for="group in systemGroupKeys"
          :key="group"
          :xs="24"
          :md="8"
        >
          <div class="system-group">
            <div class="system-group__title">
              {{ t(`portal.dashboard.systemGroups.${group}.title`) }}
            </div>
            <router-link
              v-for="entry in getGroupEntries(group)"
              :key="entry"
              :to="entry === 'permission' ? '/users' : '/home'"
              class="system-entry"
            >
              {{ t(`portal.dashboard.systemGroups.${group}.items.${entry}`) }}
            </router-link>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { fetchHealth } from '@/api/health'
import { fetchTodayTodos, updateTodo, type NbTodoItem } from '@/api/notebook/todo'
import { useTodoReminders, dismissTodoNotification } from '@/composables/useTodoReminders'
import { formatDateTime } from '@/views/notebook/todoGroup'

const { t } = useI18n()
const { refreshTodayCount } = useTodoReminders()

const healthLoading = ref(false)
const healthStatus = ref<'unknown' | 'up' | 'down'>('unknown')
const todayTodosLoading = ref(false)
const todayTodos = ref<NbTodoItem[]>([])

const systemBoard = [
  { key: 'hr', statusKey: 'todo', count: 3, tagType: 'warning' as const },
  { key: 'supply', statusKey: 'alert', count: 1, tagType: 'danger' as const },
]

const noticeKeys = ['security', 'maintenance'] as const
const systemGroupKeys = ['ops', 'admin', 'devops'] as const

const groupEntries: Record<(typeof systemGroupKeys)[number], string[]> = {
  ops: ['bi', 'crm'],
  admin: ['oa', 'hr'],
  devops: ['permission', 'devops'],
}

function getGroupEntries(group: (typeof systemGroupKeys)[number]) {
  return groupEntries[group]
}

const healthTagType = computed(() => {
  if (healthStatus.value === 'up') return 'success'
  if (healthStatus.value === 'down') return 'danger'
  return 'info'
})

const healthLabel = computed(() => {
  if (healthStatus.value === 'up') return t('home.healthOk')
  if (healthStatus.value === 'down') return t('home.healthFail')
  return t('portal.dashboard.healthUnknown')
})

async function checkHealth() {
  healthLoading.value = true
  try {
    const data = await fetchHealth()
    healthStatus.value = data.status === 'UP' ? 'up' : 'down'
  } catch {
    healthStatus.value = 'down'
  } finally {
    healthLoading.value = false
  }
}

async function loadTodayTodos() {
  todayTodosLoading.value = true
  try {
    todayTodos.value = await fetchTodayTodos()
  } catch {
    todayTodos.value = []
  } finally {
    todayTodosLoading.value = false
  }
}

async function onToggleTodayTodo(item: NbTodoItem, checked: boolean) {
  const prev = item.completed
  item.completed = checked ? 1 : 0
  try {
    const result = await updateTodo(item.id, { completed: checked })
    if (checked) {
      dismissTodoNotification(item.id)
    }
    if (checked) {
      todayTodos.value = todayTodos.value.filter((row) => row.id !== item.id)
      if (result.nextOccurrence) {
        todayTodos.value = [result.nextOccurrence, ...todayTodos.value]
      }
    } else {
      todayTodos.value = todayTodos.value.map((row) => (row.id === item.id ? result.item : row))
    }
    await refreshTodayCount()
  } catch {
    item.completed = prev
    ElMessage.error(t('notebook.todos.saveFailed'))
  }
}

onMounted(() => {
  void checkHealth()
  void loadTodayTodos()
})
</script>

<style scoped lang="scss">
.dashboard-page-title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}

.dashboard-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.dashboard-card__link {
  font-size: 13px;
  color: var(--el-color-primary);
  text-decoration: none;
}

.today-todo-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.today-todo-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.today-todo-item__body {
  flex: 1;
  min-width: 0;
}

.today-todo-item__title {
  font-size: 14px;
  line-height: 1.5;
}

.today-todo-item__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
