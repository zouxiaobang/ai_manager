<template>
  <div v-loading="loading" class="mobile-page">
    <section class="mobile-card">
      <h2 class="mobile-section-title">{{ t('portal.dashboard.systems') }}</h2>
      <div class="mobile-list-item">
        <div class="mobile-list-item__body">
          <div class="mobile-list-item__title">{{ t('portal.dashboard.aiManager') }}</div>
        </div>
        <el-tag :type="healthTagType" size="small">{{ healthLabel }}</el-tag>
      </div>
    </section>

    <section class="mobile-card">
      <div class="mobile-section-title">{{ t('portal.dashboard.todayTodos') }}</div>
      <div v-if="todayTodos.length">
        <div v-for="item in todayTodos" :key="item.id" class="mobile-list-item">
          <el-checkbox
            :model-value="item.completed === 1"
            @change="(checked: boolean) => onToggle(item, checked)"
          />
          <div class="mobile-list-item__body" @click="router.push(`/todos`)">
            <div class="mobile-list-item__title">{{ item.content }}</div>
            <div v-if="item.dueTime || item.remindTime" class="mobile-list-item__meta">
              <span v-if="item.dueTime">{{ formatDateTime(item.dueTime) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="mobile-empty-hint">{{ t('portal.dashboard.todayTodosEmpty') }}</div>
      <el-button link type="primary" @click="router.push('/todos')">
        {{ t('portal.dashboard.viewAllTodayTodos') }}
      </el-button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { fetchHealth } from '@/api/health'
import { fetchTodayTodos, updateTodo, type NbTodoItem } from '@/api/notebook/todo'
import { dismissTodoNotification, useTodoReminders } from '@/composables/useTodoReminders'
import { formatDateTime } from '@/views/notebook/todoGroup'

const router = useRouter()
const { t } = useI18n()
const { refreshTodayCount } = useTodoReminders()

const loading = ref(false)
const healthStatus = ref<'unknown' | 'up' | 'down'>('unknown')
const todayTodos = ref<NbTodoItem[]>([])

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

async function loadData() {
  loading.value = true
  try {
    const [health, todos] = await Promise.all([fetchHealth(), fetchTodayTodos()])
    healthStatus.value = health.status === 'UP' ? 'up' : 'down'
    todayTodos.value = todos
  } catch {
    healthStatus.value = 'down'
    todayTodos.value = []
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
    }
    await refreshTodayCount()
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  }
}

onMounted(() => {
  void loadData()
})
</script>
