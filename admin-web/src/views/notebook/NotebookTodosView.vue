<template>
  <div class="notebook-todos">
    <div class="notebook-todos__toolbar">
      <el-radio-group v-model="filter" size="small" @change="loadTodos">
        <el-radio-button value="today">{{ t('notebook.todos.filterToday') }}</el-radio-button>
        <el-radio-button value="pending">{{ t('notebook.todos.filterPending') }}</el-radio-button>
        <el-radio-button value="done">{{ t('notebook.todos.filterDone') }}</el-radio-button>
        <el-radio-button value="all">{{ t('notebook.todos.filterAll') }}</el-radio-button>
      </el-radio-group>
      <div class="notebook-todos__toolbar-actions">
        <el-button type="primary" size="small" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          {{ t('notebook.todos.create') }}
        </el-button>
        <el-button size="small" :loading="loading" @click="loadTodos">
          <el-icon><Refresh /></el-icon>
          {{ t('notebook.refresh') }}
        </el-button>
      </div>
    </div>

    <div class="notebook-todos__composer">
      <el-input
        v-model="quickContent"
        :placeholder="t('notebook.todos.addPlaceholder')"
        @keyup.enter="onQuickAdd"
      />
      <el-date-picker
        v-model="quickDueTime"
        type="datetime"
        value-format="YYYY-MM-DD HH:mm:ss"
        :placeholder="t('notebook.todos.dueTimePlaceholder')"
        clearable
      />
      <el-button
        type="primary"
        :loading="quickAdding"
        :disabled="!quickContent.trim()"
        @click="onQuickAdd"
      >
        {{ t('notebook.todos.add') }}
      </el-button>
    </div>

    <div v-loading="loading" class="notebook-todos__groups">
      <section
        v-for="group in groupedItems"
        :key="group.key"
        class="notebook-todos__group"
      >
        <div class="notebook-todos__group-title">
          <span>{{ t(`notebook.todos.groups.${group.key}`) }}</span>
          <span class="notebook-todos__group-count">{{ group.items.length }}</span>
        </div>
        <el-table :data="group.items" stripe border>
          <el-table-column width="52" align="center">
            <template #default="{ row }">
              <el-checkbox
                :model-value="row.completed === 1"
                @change="(checked: boolean) => onToggle(row, checked)"
              />
            </template>
          </el-table-column>
          <el-table-column :label="t('notebook.todos.content')" min-width="220">
            <template #default="{ row }">
              <div class="notebook-todos__content-cell">
                <span :class="{ 'is-done': row.completed === 1 }">{{ row.content }}</span>
                <el-tag
                  v-if="repeatLabel(row)"
                  size="small"
                  type="info"
                  class="notebook-todos__repeat-tag"
                >
                  {{ repeatLabel(row) }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="t('notebook.todos.dueTime')" width="168">
            <template #default="{ row }">
              <span v-if="row.dueTime" :class="{ 'is-overdue': isOverdue(row) }">
                {{ formatDateTime(row.dueTime) }}
              </span>
              <span v-else class="notebook-todos__muted">{{ t('notebook.todos.noDueTime') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('notebook.todos.remindTime')" width="168">
            <template #default="{ row }">
              <span
                v-if="row.remindTime"
                :class="{ 'is-remind-today': isRemindSoon(row) }"
              >
                {{ formatDateTime(row.remindTime) }}
              </span>
              <span v-else class="notebook-todos__muted">{{ t('notebook.todos.noRemindTime') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('notebook.actions')" width="88" align="center" fixed="right">
            <template #default="{ row }">
              <div class="notebook-todos__actions">
                <el-button
                  link
                  type="primary"
                  :title="t('notebook.todos.edit')"
                  @click="openEditDialog(row)"
                >
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button
                  link
                  type="danger"
                  :title="t('notebook.todos.delete')"
                  @click="onRemove(row)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <el-empty
        v-if="!loading && groupedItems.length === 0"
        :description="t('notebook.todos.globalEmpty')"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? t('notebook.todos.createTitle') : t('notebook.todos.editTitle')"
      width="560px"
      destroy-on-close
    >
      <el-form label-width="96px" @submit.prevent>
        <el-form-item :label="t('notebook.todos.content')" required>
          <el-input v-model="form.content" :placeholder="t('notebook.todos.addPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('notebook.todos.dueTime')">
          <el-date-picker
            v-model="form.dueTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            :placeholder="t('notebook.todos.dueTimePlaceholder')"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('notebook.todos.remindTime')">
          <el-date-picker
            v-model="form.remindTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            :placeholder="t('notebook.todos.remindTimePlaceholder')"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('notebook.todos.repeatLabel')">
          <div class="notebook-todos__repeat-form">
            <el-select v-model="form.repeatType" style="width: 160px">
              <el-option
                v-for="type in TODO_REPEAT_TYPES"
                :key="type"
                :label="t(`notebook.todos.repeatOptions.${type}`)"
                :value="type"
              />
            </el-select>
            <template v-if="form.repeatType !== 'NONE'">
              <span class="notebook-todos__repeat-gap">{{ t('notebook.todos.repeatEvery') }}</span>
              <el-input-number v-model="form.repeatInterval" :min="1" :max="365" />
              <span class="notebook-todos__repeat-gap">{{ repeatUnitLabel }}</span>
            </template>
          </div>
        </el-form-item>
        <el-form-item v-if="form.repeatType !== 'NONE'" :label="t('notebook.todos.repeatUntil')">
          <el-date-picker
            v-model="form.repeatUntil"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            :placeholder="t('notebook.todos.repeatUntilPlaceholder')"
            clearable
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('pomodoro.common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDialog">
          {{ t('pomodoro.common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import {
  TODO_REPEAT_TYPES,
  createTodo,
  fetchTodayTodos,
  fetchTodos,
  removeTodo,
  updateTodo,
  type NbTodoFilter,
  type NbTodoItem,
  type NbTodoRepeatType,
} from '@/api/notebook/todo'
import {
  formatDateTime,
  groupTodos,
  isOverdue,
  isRemindSoon,
} from './todoGroup'
import { useTodoReminders, dismissTodoNotification } from '@/composables/useTodoReminders'

const props = defineProps<{
  initialFilter?: NbTodoFilter
}>()

const { t } = useI18n()
const { refreshTodayCount } = useTodoReminders()

const filter = ref<NbTodoFilter>(props.initialFilter ?? 'today')
const loading = ref(false)
const quickAdding = ref(false)
const submitting = ref(false)
const items = ref<NbTodoItem[]>([])
const quickContent = ref('')
const quickDueTime = ref<string | null>(null)

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const form = reactive({
  content: '',
  dueTime: null as string | null,
  remindTime: null as string | null,
  repeatType: 'NONE' as NbTodoRepeatType,
  repeatInterval: 1,
  repeatUntil: null as string | null,
})

const groupedItems = computed(() => groupTodos(items.value, filter.value))

const repeatUnitLabel = computed(() => {
  if (form.repeatType === 'NONE') return ''
  return t(`notebook.todos.repeatUnit.${form.repeatType}`)
})

function repeatLabel(row: NbTodoItem): string {
  const type = row.repeatType ?? 'NONE'
  if (type === 'NONE') return ''
  const interval = row.repeatInterval ?? 1
  return t(`notebook.todos.repeatTag.${type}`, { n: interval })
}

function buildSchedulePayload() {
  return {
    dueTime: form.dueTime || undefined,
    clearDueTime: !form.dueTime,
    remindTime: form.remindTime || undefined,
    clearRemindTime: !form.remindTime,
    repeatType: form.repeatType,
    repeatInterval: form.repeatType === 'NONE' ? 1 : form.repeatInterval,
    repeatUntil: form.repeatType === 'NONE' ? undefined : form.repeatUntil || undefined,
    clearRepeatUntil: form.repeatType !== 'NONE' && !form.repeatUntil,
  }
}

function mergeItems(next: NbTodoItem[]) {
  const map = new Map<number, NbTodoItem>()
  for (const item of next) {
    map.set(item.id, item)
  }
  return [...map.values()]
}

async function loadTodos() {
  loading.value = true
  try {
    if (filter.value === 'today') {
      items.value = await fetchTodayTodos()
    } else {
      const completed = filter.value === 'all' ? undefined : filter.value === 'done'
      items.value = await fetchTodos({ completed })
    }
  } catch {
    items.value = []
    ElMessage.error(t('notebook.todos.loadFailed'))
  } finally {
    loading.value = false
  }
}

function setFilter(next: NbTodoFilter) {
  if (filter.value === next) {
    void loadTodos()
    return
  }
  filter.value = next
  void loadTodos()
}

async function onQuickAdd() {
  const content = quickContent.value.trim()
  if (!content) return
  quickAdding.value = true
  try {
    const created = await createTodo({
      content,
      dueTime: quickDueTime.value || undefined,
    })
    quickContent.value = ''
    quickDueTime.value = null
    if (filter.value !== 'done') {
      items.value = mergeItems([created, ...items.value])
    }
    ElMessage.success(t('pomodoro.common.saved'))
    void refreshTodayCount()
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  } finally {
    quickAdding.value = false
  }
}

function resetForm() {
  form.content = ''
  form.dueTime = null
  form.remindTime = null
  form.repeatType = 'NONE'
  form.repeatInterval = 1
  form.repeatUntil = null
}

function openCreateDialog() {
  dialogMode.value = 'create'
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: NbTodoItem) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.content = row.content
  form.dueTime = row.dueTime ?? null
  form.remindTime = row.remindTime ?? null
  form.repeatType = (row.repeatType as NbTodoRepeatType) ?? 'NONE'
  form.repeatInterval = row.repeatInterval ?? 1
  form.repeatUntil = row.repeatUntil ?? null
  dialogVisible.value = true
}

async function submitDialog() {
  const content = form.content.trim()
  if (!content) {
    ElMessage.warning(t('notebook.todos.contentRequired'))
    return
  }
  submitting.value = true
  try {
    const schedule = buildSchedulePayload()
    if (dialogMode.value === 'create') {
      const created = await createTodo({ content, ...schedule })
      if (filter.value !== 'done') {
        items.value = mergeItems([created, ...items.value])
      }
    } else if (editingId.value) {
      const result = await updateTodo(editingId.value, { content, ...schedule })
      items.value = mergeItems(
        items.value.map((item) => (item.id === result.item.id ? result.item : item)),
      )
    }
    dialogVisible.value = false
    ElMessage.success(t('pomodoro.common.saved'))
    void refreshTodayCount()
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  } finally {
    submitting.value = false
  }
}

async function onToggle(row: NbTodoItem, checked: boolean) {
  const prev = row.completed
  row.completed = checked ? 1 : 0
  try {
    const result = await updateTodo(row.id, { completed: checked })
    if (checked) {
      dismissTodoNotification(row.id)
    }
    const nextItems = items.value.filter((item) => item.id !== row.id)
    if (filter.value === 'all') {
      nextItems.push(result.item)
      if (result.nextOccurrence) {
        nextItems.push(result.nextOccurrence)
        ElMessage.success(t('notebook.todos.repeatSpawned'))
      }
      items.value = mergeItems(nextItems)
      void refreshTodayCount()
      return
    }
    if ((filter.value === 'pending' || filter.value === 'today') && checked) {
      if (result.nextOccurrence) {
        items.value = mergeItems([...nextItems, result.nextOccurrence])
        ElMessage.success(t('notebook.todos.repeatSpawned'))
      } else {
        items.value = nextItems
      }
      void refreshTodayCount()
      return
    }
    if (filter.value === 'done' && !checked) {
      items.value = nextItems
      void refreshTodayCount()
      return
    }
    items.value = mergeItems(
      items.value.map((item) => (item.id === result.item.id ? result.item : item)),
    )
    void refreshTodayCount()
  } catch {
    row.completed = prev
    ElMessage.error(t('notebook.todos.saveFailed'))
  }
}

async function onRemove(row: NbTodoItem) {
  try {
    await ElMessageBox.confirm(t('notebook.todos.deleteConfirm'), { type: 'warning' })
    await removeTodo(row.id)
    items.value = items.value.filter((item) => item.id !== row.id)
    ElMessage.success(t('pomodoro.common.deleted'))
    void refreshTodayCount()
  } catch {
    // 用户取消或请求失败
  }
}

onMounted(() => {
  void loadTodos()
})

defineExpose({ reload: loadTodos, setFilter })
</script>

<style scoped lang="scss">
.notebook-todos {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 360px;
}

.notebook-todos__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.notebook-todos__toolbar-actions {
  display: flex;
  gap: 8px;
}

.notebook-todos__composer {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;

  .el-input {
    flex: 1 1 240px;
    min-width: 200px;
  }
}

.notebook-todos__groups {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 200px;
}

.notebook-todos__group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 15px;
  font-weight: 600;
}

.notebook-todos__group-count {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.notebook-todos__content-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.notebook-todos__repeat-tag {
  margin: 0;
}

.notebook-todos__repeat-form {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.notebook-todos__repeat-gap {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.is-done {
  color: var(--el-text-color-secondary);
  text-decoration: line-through;
}

.is-overdue {
  color: var(--el-color-danger);
  font-weight: 600;
}

.is-remind-today {
  color: var(--el-color-warning);
  font-weight: 600;
}

.notebook-todos__muted {
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

.notebook-todos__actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}
</style>
