<template>
  <div class="mobile-todos">
    <!-- 问候语 -->
    <div class="ha-greeting">
      <div class="ha-greeting__left">
        <h2 class="ha-greeting__title">{{ greetingText }} {{ greetingEmoji }}</h2>
        <p class="ha-greeting__sub">
          今天还有 <strong>{{ todayCount }}</strong> 件事等着你
        </p>
      </div>
      <button class="ha-greeting__refresh" :class="{ loading: loading }" @click="loadTodos">
        ↻
      </button>
    </div>

    <!-- 进度环 + 统计 -->
    <div class="ha-stats-card">
      <div class="ha-ring">
        <svg viewBox="0 0 52 52" class="ha-ring__svg">
          <circle cx="26" cy="26" r="22" fill="none" stroke="#e2e8f0" stroke-width="4.5" />
          <circle
            cx="26" cy="26" r="22"
            fill="none" stroke="#2563eb" stroke-width="4.5"
            stroke-linecap="round"
            :stroke-dasharray="138.2"
            :stroke-dashoffset="ringOffset"
            transform="rotate(-90 26 26)"
          />
        </svg>
        <span class="ha-ring__text">{{ doneRatio }}%</span>
      </div>
      <div class="ha-stats">
        <div class="ha-stat">
          <span class="ha-stat__value ha-stat__value--blue">{{ pendingCount }}</span>
          <span class="ha-stat__label">待完成</span>
        </div>
        <div class="ha-stat-divider"></div>
        <div class="ha-stat">
          <span class="ha-stat__value ha-stat__value--orange">{{ todayCount }}</span>
          <span class="ha-stat__label">今日</span>
        </div>
        <div class="ha-stat-divider"></div>
        <div class="ha-stat">
          <span class="ha-stat__value ha-stat__value--red">{{ overdueCount }}</span>
          <span class="ha-stat__label">逾期</span>
        </div>
      </div>

      <!-- 置顶列表 -->
      <div v-if="pinnedTodos.length" class="ha-pinned">
        <div class="ha-pinned__head">📌 {{t('notebook.pin') }}</div>
        <div class="ha-pinned__list">
          <div
            v-for="item in pinnedTodos"
            :key="item.id"
            class="ha-pinned__item"
          >
            <div class="ha-pinned__content">{{ item.content }}</div>
            <div class="ha-pinned__meta">
              <span v-if="item.dueTime" class="ha-pinned__time">{{ formatTime(item.dueTime) }}</span>
              <span v-if="item.repeatType && item.repeatType !== 'NONE'" class="ha-pinned__tag">🔁</span>
              <span v-if="item.remindTime" class="ha-pinned__tag">🔔</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选标签 -->
    <MobileCategoryTabs
      :categories="filterOptions"
      :active-value="activeFilter"
      active-color="#2563eb"
      inactive-color="#94a3b8"
      fill-color="#2563eb"
      @update:active-value="setFilter"
    />

    <!-- 加载中 -->
    <div v-if="loading" class="ha-loading">
      <div class="ha-loading__spinner"></div>
      <span>加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!visibleGroups.length" class="ha-empty">
      <div class="ha-empty__icon">🎉</div>
      <div class="ha-empty__title">{{ t('notebook.todos.empty') }}</div>
      <p class="ha-empty__sub">在下方输入框添加第一条待办吧</p>
    </div>

    <!-- 分组列表 -->
    <div v-else class="ha-group-list">
      <div
        v-for="group in visibleGroups"
        :key="group.key"
        class="ha-group"
      >
        <button class="ha-group__head" @click="toggleGroup(group.key)">
          <span class="ha-group__icon">{{ groupIcon(group.key) }}</span>
          <span class="ha-group__title">{{ groupTitle(group.key) }}</span>
          <span class="ha-group__count">{{ group.items.length }}</span>
          <span class="ha-group__arrow" :class="{ expanded: !collapsedGroups.has(group.key) }">›</span>
        </button>
        <div v-if="!collapsedGroups.has(group.key)" class="ha-group__body">
          <SchemeADoodleFrame
            v-for="item in group.items"
            :key="item.id"
            class="ha-todo-card"
            :class="{ 'is-done': item.completed }"
            :color="cardFrameColor(group.key)"
            :seed="doodleSeedFromKey('todo-' + item.id)"
            sketch
            :stroke-width="2.5"
            :shadow="false"
            tag="div"
          >
            <div class="ha-todo-card__inner">
              <button
                class="ha-card-check"
                :class="{ checked: item.completed }"
                :disabled="togglingId === item.id"
                @click="toggleComplete(item)"
              >
                <span v-if="item.completed" class="ha-card-check__inner">✓</span>
                <span v-else-if="togglingId === item.id" class="ha-card-check__spinner"></span>
              </button>
              <div class="ha-card-body" @click="openMenu(item)">
                <span class="ha-card-body__text">{{ item.content }}</span>
                <div class="ha-card-body__meta">
                  <span v-if="item.dueTime" class="ha-card-time" :class="{ overdue: isItemOverdue(item) }">
                    ⏰ {{ formatTime(item.dueTime) }}
                  </span>
                  <span v-if="item.pinned" class="ha-tag ha-tag--pin">📌 置顶</span>
                  <span v-if="item.repeatType && item.repeatType !== 'NONE'" class="ha-tag ha-tag--repeat">🔁 {{ repeatLabel(item.repeatType) }}</span>
                  <span v-if="item.remindTime" class="ha-tag ha-tag--remind">🔔 {{ formatTime(item.remindTime) }}</span>
                </div>
              </div>
            </div>
          </SchemeADoodleFrame>
          <!-- 已完成分组加载更多 -->
          <button
            v-if="group.key === 'done' && hasMoreDone"
            class="ha-load-more"
            @click="loadMoreDone"
          >
            加载更多
          </button>
        </div>
      </div>
    </div>

    <!-- 底部快速添加 -->
    <div class="ha-bottom-bar">
      <input
        v-model="newContent"
        class="ha-bottom-bar__input"
        :placeholder="t('notebook.todos.create')"
        @keyup.enter="createTodo"
      />
      <button
        class="ha-bottom-bar__btn"
        :disabled="!newContent.trim() || creating"
        @click="createTodo"
      >
        {{ creating ? '...' : '添加' }}
      </button>
    </div>

    <!-- 操作菜单 -->
    <MobileBottomSheet v-model="menuVisible" :title="menuItem?.content ?? ''" show-close>
      <button v-if="!menuItem?.completed" class="ha-menu-sheet__action" @click="openEditDialog">
        修改
      </button>
      <button class="ha-menu-sheet__action" @click="togglePin">
        {{ menuItem?.pinned ? '取消置顶' : '置顶' }}
      </button>
      <button class="ha-menu-sheet__action ha-menu-sheet__action--danger" @click="openDeleteDialog">
        删除
      </button>
      <div class="ha-menu-sheet__divider"></div>
      <button class="ha-menu-sheet__action ha-menu-sheet__action--cancel" @click="closeMenu">
        取消
      </button>
    </MobileBottomSheet>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editVisible"
      title="修改待办"
      width="90%"
      :close-on-click-modal="false"
      top="20vh"
    >
      <div class="ha-edit-form">
        <label class="ha-edit-form__label">内容</label>
        <input
          v-model="editForm.content"
          class="ha-edit-form__input"
          placeholder="待办内容"
          type="text"
        />

        <label class="ha-edit-form__label">截止时间</label>
        <input
          v-model="editForm.dueTime"
          class="ha-edit-form__input"
          type="datetime-local"
        />

        <label class="ha-edit-form__label">提醒时间</label>
        <input
          v-model="editForm.remindTime"
          class="ha-edit-form__input"
          type="datetime-local"
        />

        <label class="ha-edit-form__label">重复</label>
        <div class="ha-edit-form__repeat-group">
          <button
            v-for="rt in repeatTypeOptions"
            :key="rt.value"
            class="ha-edit-form__repeat-btn"
            :class="{ active: editForm.repeatType === rt.value }"
            @click="editForm.repeatType = rt.value"
          >
            {{ rt.label }}
          </button>
        </div>
      </div>
      <template #footer>
        <button class="ha-edit-form__cancel" @click="editVisible = false">取消</button>
        <button class="ha-edit-form__confirm" @click="saveEdit">保存</button>
      </template>
    </el-dialog>

    <!-- 删除确认弹窗 -->
    <Teleport to="body">
      <Transition name="todo-delete-confirm">
        <div
          v-if="deleteTarget"
          class="todo-delete-confirm"
          @click.self="cancelDelete"
        >
          <SchemeADoodleFrame
            class="todo-delete-confirm__panel"
            color="#cbd5e1"
            :shadow="false"
            :seed="42"
            role="alertdialog"
            aria-modal="true"
          >
            <div class="todo-delete-confirm__inner">
              <div class="todo-delete-confirm__title-row">
                <img class="todo-delete-confirm__icon" :src="schemeAAssets.starBlue" alt="" />
                <h2 class="todo-delete-confirm__title">删除待办</h2>
              </div>

              <p class="todo-delete-confirm__message">
                确定删除「{{ deleteTarget.content }}」吗？
              </p>

              <div class="todo-delete-confirm__actions">
                <SchemeADoodleFrame
                  tag="button"
                  type="button"
                  shape="pill"
                  color="#cbd5e1"
                  :shadow="false"
                  class="todo-delete-confirm__btn todo-delete-confirm__btn--cancel"
                  @click="cancelDelete"
                >
                  取消
                </SchemeADoodleFrame>
                <SchemeADoodleFrame
                  tag="button"
                  type="button"
                  shape="pill"
                  color="#ef4444"
                  :shadow="false"
                  :sketch="true"
                  class="todo-delete-confirm__btn todo-delete-confirm__btn--confirm"
                  :class="{ 'is-disabled': deleting }"
                  :disabled="deleting"
                  @click="confirmDelete"
                >
                  确认
                </SchemeADoodleFrame>
              </div>
            </div>
          </SchemeADoodleFrame>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {useI18n} from 'vue-i18n'
import type {NbTodoItem, NbTodoSaveRequest} from '@/api/notebook/todo'
import {
  createTodo as apiCreateTodo,
  fetchTodos,
  removeTodo as apiRemoveTodo,
  updateTodo as apiUpdateTodo,
} from '@/api/notebook/todo'
import {formatDateTime, isOverdue, isToday} from '@/views/notebook/todoGroup'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import {doodleSeedFromKey} from '@/mobile/utils/doodleSeed'
import {schemeAAssets} from '@/mobile/views/home/themes/scheme-a/assets'
import MobileCategoryTabs from '@/mobile/components/MobileCategoryTabs.vue'
import type {CategoryItem} from '@/mobile/components/MobileCategoryTabs.vue'
import MobileBottomSheet from '@/mobile/components/MobileBottomSheet.vue'

const { t } = useI18n()

// 数据状态
const todos = ref<NbTodoItem[]>([])
const loading = ref(false)
const creating = ref(false)
const togglingId = ref<number | null>(null)

// 筛选
const activeFilter = ref<string>('all')
const collapsedGroups = reactive(new Set<string>(['done']))

// 新建内容
const newContent = ref('')

// 底部菜单
const menuVisible = ref(false)
const menuItem = ref<NbTodoItem | null>(null)

// 删除确认
const deleteTarget = ref<NbTodoItem | null>(null)
const deleting = ref(false)

const filterOptions: CategoryItem[] = [
  { id: 'all', icon: '📋', name: '全部' },
  { id: 'today', icon: '📅', name: '今日' },
  { id: 'overdue', icon: '⚠️', name: '逾期' },
  { id: 'upcoming', icon: '🟢', name: '未来' },
  { id: 'unscheduled', icon: '⚪', name: '未安排' },
  { id: 'done', icon: '✅', name: '已完成' },
]

const donePageSize = 20
const donePage = ref(1)

const hasMoreDone = computed(() => {
  const allDone = todos.value.filter(t => t.completed)
  return allDone.length > donePage.value * donePageSize
})

function loadMoreDone() {
  donePage.value++
}

// ---- 问候语 ----
const hour = new Date().getHours()
const greetingText = hour < 6 ? '夜深了' : hour < 9 ? '早上好' : hour < 12 ? '上午好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好'
const greetingEmoji = hour < 6 ? '🌙' : hour < 9 ? '☀️' : hour < 12 ? '☀️' : hour < 14 ? '🌤' : hour < 18 ? '🌇' : '🌆'

// ---- 统计计算 ----
const pinnedTodos = computed(() => todos.value.filter(t => t.pinned))
const pendingCount = computed(() => todos.value.filter(t => !t.completed).length)
const todayCount = computed(() => todos.value.filter(t => !t.completed && (isToday(t.dueTime) || isToday(t.remindTime))).length)
const overdueCount = computed(() => todos.value.filter(t => !t.completed && isOverdue(t)).length)
const doneCount = computed(() => todos.value.filter(t => t.completed).length)
const doneRatio = computed(() => todos.value.length ? Math.round((doneCount.value / todos.value.length) * 100) : 0)
const ringOffset = computed(() => 138.2 - (138.2 * doneRatio.value) / 100)

// ---- 排序 ----
function sortTodos(list: NbTodoItem[]): NbTodoItem[] {
  return list.sort((a, b) => {
    // 置顶优先，逾期其次，然后按 sortOrder
    const pa = a.pinned ? 1 : 0
    const pb = b.pinned ? 1 : 0
    if (pa !== pb) return pb - pa
    const oa = isOverdue(a) ? 1 : 0
    const ob = isOverdue(b) ? 1 : 0
    if (oa !== ob) return ob - oa
    return (a.sortOrder ?? 0) - (b.sortOrder ?? 0)
  })
}

// ---- 加载数据 ----
async function loadTodos() {
  loading.value = true
  try {
    const data = await fetchTodos()
    todos.value = sortTodos(data)
  } catch {
    ElMessage.error(t('notebook.todos.loadFailed'))
  } finally {
    loading.value = false
  }
}

// ---- 分组 ----
function groupTodos(items: NbTodoItem[]): { key: string; items: NbTodoItem[] }[] {
  const pending = items.filter(t => !t.completed)
  const groups: { key: string; items: NbTodoItem[] }[] = [
    { key: 'overdue', items: pending.filter(t => isOverdue(t)) },
    { key: 'today', items: pending.filter(t => !isOverdue(t) && (isToday(t.dueTime) || isToday(t.remindTime))) },
    { key: 'upcoming', items: pending.filter(t => !isOverdue(t) && !isToday(t.dueTime) && !isToday(t.remindTime) && (t.dueTime || t.remindTime)) },
    { key: 'unscheduled', items: pending.filter(t => !isOverdue(t) && !isToday(t.dueTime) && !isToday(t.remindTime) && !t.dueTime && !t.remindTime) },
    { key: 'done', items: items.filter(t => t.completed) },
  ]
  return groups.filter(g => g.items.length > 0)
}

const visibleGroups = computed(() => {
  let result = groupTodos(todos.value)
  if (activeFilter.value === 'today') {
    result = result.filter(g => g.key === 'today')
  } else if (activeFilter.value === 'overdue') {
    result = result.filter(g => g.key === 'overdue')
  } else if (activeFilter.value === 'upcoming') {
    result = result.filter(g => g.key === 'upcoming')
  } else if (activeFilter.value === 'unscheduled') {
    result = result.filter(g => g.key === 'unscheduled')
  } else if (activeFilter.value === 'done') {
    result = result.filter(g => g.key === 'done')
  }
  // 已完成分组按页数截取
  return result.map(g => {
    if (g.key === 'done') {
      return { ...g, items: g.items.slice(0, donePage.value * donePageSize) }
    }
    return g
  })
})

// ---- 过滤 ----
function setFilter(key: string | number) {
  activeFilter.value = String(key)
  // 切换 filter 时自动展开对应组
  if (key === 'overdue' || key === 'today') {
    collapsedGroups.delete('overdue')
  }
  if (key === 'done') {
    collapsedGroups.delete('done')
    donePage.value = 1
  }
  if (key === 'upcoming') {
    collapsedGroups.delete('upcoming')
  }
  if (key === 'unscheduled') {
    collapsedGroups.delete('unscheduled')
  }
}

// ---- 切换折叠 ----
function toggleGroup(key: string) {
  if (collapsedGroups.has(key)) {
    collapsedGroups.delete(key)
  } else {
    collapsedGroups.add(key)
  }
}

// ---- 分组展示 ----
function groupIcon(key: string) {
  const map: Record<string, string> = { overdue: '🔴', today: '🟡', upcoming: '🟢', unscheduled: '⚪', done: '✅' }
  return map[key] || '📋'
}

function groupTitle(key: string) {
  const map: Record<string, string> = { overdue: '已逾期', today: '今日待办', upcoming: '未来安排', unscheduled: '未安排', done: '已完成' }
  return map[key] || key
}

// ---- 工具函数 ----
function isItemOverdue(item: NbTodoItem): boolean {
  return !item.completed && isOverdue(item)
}

function formatTime(value?: string | null): string {
  if (!value) return ''
  return formatDateTime(value)
}

function repeatLabel(type: string): string {
  const map: Record<string, string> = { DAILY: '每天', WEEKLY: '每周', MONTHLY: '每月', YEARLY: '每年' }
  return map[type] || type
}

function cardFrameColor(groupKey: string): string {
  const map: Record<string, string> = {
    overdue: '#ef4444',
    today: '#f59e0b',
    upcoming: '#22c55e',
    unscheduled: '#94a3b8',
    done: '#94a3b8',
  }
  return map[groupKey] || '#2563eb'
}

// ---- 操作 ----
async function toggleComplete(item: NbTodoItem) {
  togglingId.value = item.id
  try {
    const body: NbTodoSaveRequest = { completed: !item.completed }
    const result = await apiUpdateTodo(item.id, body)
    // 更新本地数据
    item.completed = result.item.completed
    item.updateTime = result.item.updateTime
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  } finally {
    togglingId.value = null
  }
}

async function createTodo() {
  const content = newContent.value.trim()
  if (!content || creating.value) return
  creating.value = true
  try {
    const newItem = await apiCreateTodo({ content })
    todos.value.unshift(newItem)
    newContent.value = ''
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  } finally {
    creating.value = false
  }
}

// ---- 编辑对话框 ----
const editVisible = ref(false)
const editForm = reactive({
  content: '',
  dueTime: '',
  remindTime: '',
  repeatType: 'NONE',
})

const repeatTypeOptions = [
  { value: 'NONE', label: '不重复' },
  { value: 'DAILY', label: '每天' },
  { value: 'WEEKLY', label: '每周' },
  { value: 'MONTHLY', label: '每月' },
  { value: 'YEARLY', label: '每年' },
]

function toDatetimeLocal(value?: string | null): string {
  if (!value) return ''
  const d = new Date(value)
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function openEditDialog() {
  if (!menuItem.value) return
  if (menuItem.value.completed) return
  menuVisible.value = false
  editForm.content = menuItem.value.content
  editForm.dueTime = toDatetimeLocal(menuItem.value.dueTime)
  editForm.remindTime = toDatetimeLocal(menuItem.value.remindTime)
  editForm.repeatType = menuItem.value.repeatType ?? 'NONE'
  editVisible.value = true
}

async function saveEdit() {
  if (!menuItem.value) return
  if (menuItem.value.completed) return
  if (!editForm.content.trim()) {
    ElMessage.warning('内容不能为空')
    return
  }
  const body: NbTodoSaveRequest = {
    content: editForm.content.trim(),
    dueTime: editForm.dueTime ? editForm.dueTime + ':00' : null,
    clearDueTime: !editForm.dueTime,
    remindTime: editForm.remindTime ? editForm.remindTime + ':00' : null,
    clearRemindTime: !editForm.remindTime,
    repeatType: editForm.repeatType === 'NONE' ? 'NONE' : editForm.repeatType,
  }
  try {
    const result = await apiUpdateTodo(menuItem.value.id, body)
    Object.assign(menuItem.value, result.item)
    ElMessage.success('已修改')
    editVisible.value = false
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  }
}

// ---- 点击卡片 ----
function openMenu(item: NbTodoItem) {
  menuItem.value = item
  menuVisible.value = true
}

function closeMenu() {
  menuVisible.value = false
  menuItem.value = null
}

async function togglePin() {
  if (!menuItem.value) return
  const item = menuItem.value
  menuVisible.value = false
  try {
    const result = await apiUpdateTodo(item.id, { pinned: !item.pinned })
    item.pinned = result.item.pinned
    sortTodos(todos.value)
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  }
}

function openDeleteDialog() {
  if (!menuItem.value) return
  deleteTarget.value = menuItem.value
  menuVisible.value = false
}

function cancelDelete() {
  deleteTarget.value = null
}

async function confirmDelete() {
  if (!deleteTarget.value || deleting.value) return
  deleting.value = true
  try {
    await apiRemoveTodo(deleteTarget.value.id)
    todos.value = todos.value.filter(t => t.id !== deleteTarget.value!.id)
    ElMessage.success(t('notebook.todos.deleted'))
    deleteTarget.value = null
  } catch {
    ElMessage.error(t('notebook.todos.saveFailed'))
  } finally {
    deleting.value = false
  }
}

// ---- 生命周期 ----
onMounted(() => {
  loadTodos()
})
</script>

<style scoped lang="scss">
.mobile-todos {
  min-height: calc(100vh - 56px);
  background: #fff;
  padding: 16px;
  padding-bottom: 80px;
  overflow-y: auto;
}

// ---- 问候语 ----
.ha-greeting {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}

.ha-greeting__left {
  flex: 1;
}

.ha-greeting__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 22px;
  color: #1e293b;
  margin: 0 0 2px;
}

.ha-greeting__sub {
  font-size: 13px;
  color: #64748b;
  margin: 0;

  strong {
    color: #2563eb;
    font-weight: 700;
  }
}

.ha-greeting__refresh {
  background: none;
  border: none;
  font-size: 22px;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px 8px;
  transition: transform 0.3s ease;

  &.loading {
    animation: ha-spin 0.8s linear infinite;
  }
}

@keyframes ha-spin {
  100% { transform: rotate(360deg); }
}

// ---- 统计卡片 ----
.ha-stats-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 18px;
  background: white;
  border-radius: 16px;
  padding: 14px 18px;
  margin-bottom: 14px;
  border: 2px solid #e2e8f0;
}

.ha-ring {
  position: relative;
  width: 60px;
  height: 60px;
  flex-shrink: 0;
}

.ha-ring__svg {
  width: 100%;
  height: 100%;
}

.ha-ring__text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  color: #1e293b;
}

.ha-stats {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.ha-stat {
  text-align: center;
  flex: 1;
}

.ha-stat__value {
  font-size: 20px;
  font-weight: 800;
  display: block;

  &--blue { color: #2563eb; }
  &--orange { color: #f59e0b; }
  &--red { color: #ef4444; }
}

.ha-stat__label {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 1px;
}

.ha-stat-divider {
  width: 1px;
  height: 28px;
  background: #e2e8f0;
}

// ---- 置顶列表 ----
.ha-pinned {
  width: 100%;
  padding-top: 8px;
  border-top: 2px dashed #e2e8f0;
}

.ha-pinned__head {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 15px;
  margin-bottom: 8px;
  font-weight: 700;
  color: #9b0000;
}

.ha-pinned__list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ha-pinned__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  background: #fef9e7;
  border-radius: 12px;
  padding: 4px 24px;
  cursor: pointer;
  transition: transform 0.15s ease;

  &:active {
    transform: scale(0.98);
  }
}

.ha-pinned__content {
  font-size: 14px;
  color: #1e293b;
  font-weight: 600;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ha-pinned__meta {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.ha-pinned__time {
  font-size: 11px;
  color: #d97706;
  font-weight: 600;
}

.ha-pinned__tag {
  font-size: 13px;
}

// ---- 加载 & 空状态 ----
.ha-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 60px 0;
  font-size: 14px;
  color: #94a3b8;
}

.ha-loading__spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e2e8f0;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: ha-spin 0.6s linear infinite;
}

.ha-empty {
  text-align: center;
  padding: 60px 20px;
}

.ha-empty__icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.ha-empty__title {
  font-size: 16px;
  color: #1e293b;
  font-weight: 600;
  margin-bottom: 6px;
}

.ha-empty__sub {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}

// ---- 分组列表 ----
.ha-group-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ha-group {
  background: white;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.ha-group__head {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 11px 14px;
  border: none;
  background: transparent;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.ha-group__icon { font-size: 15px; }

.ha-group__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
}

.ha-group__count {
  margin-left: auto;
  font-size: 11px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 999px;
}

.ha-group__arrow {
  font-size: 18px;
  color: #94a3b8;
  margin-left: 4px;
  transition: transform 0.2s ease;

  &.expanded {
    transform: rotate(90deg);
  }
}

.ha-group__body {
  padding: 6px 14px 14px;
}

// ---- 待办卡片（手绘边框） ----
.ha-todo-card {
  margin-bottom: 8px;
  border-radius: 16px;

  &:last-child { margin-bottom: 0; }

  &.is-done {
    opacity: 0.55;

    .ha-card-body__text {
      text-decoration: line-through;
      color: #94a3b8;
    }
  }
}

.ha-todo-card__inner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 24px;
}

.ha-card-check {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #cbd5e1;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  margin-top: 2px;
  padding: 0;
  transition: all 0.2s ease;

  &.checked {
    background: #22c55e;
    border-color: #22c55e;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.ha-card-check__inner {
  color: white;
  font-size: 11px;
  font-weight: 700;
}

.ha-card-check__spinner {
  width: 12px;
  height: 12px;
  border: 2px solid #94a3b8;
  border-top-color: white;
  border-radius: 50%;
  animation: ha-spin 0.6s linear infinite;
}

.ha-card-body {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.ha-card-body__text {
  font-size: 14px;
  color: #1e293b;
  display: block;
  line-height: 1.4;
}

.ha-card-body__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
}

.ha-card-time {
  font-size: 11px;
  color: #64748b;

  &.overdue {
    color: #ef4444;
    font-weight: 600;
  }
}

.ha-load-more {
  display: block;
  width: 100%;
  padding: 10px;
  margin-top: 8px;
  border: 2px dashed #cbd5e1;
  border-radius: 10px;
  background: transparent;
  color: #64748b;
  font-size: 14px;
  font-weight: 700;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  cursor: pointer;
  text-align: center;
  transition: background 0.15s, color 0.15s;

  &:hover {
    background: #f1f5f9;
    color: #475569;
  }

  &:active {
    background: #e2e8f0;
  }
}

.ha-tag {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #64748b;
  line-height: 1.3;

  &--pin { background: #fef3c7; color: #d97706; }
  &--repeat { background: #dbeafe; color: #2563eb; }
  &--remind { background: #fce7f3; color: #db2777; }
}

.ha-card-more {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 18px;
  cursor: pointer;
  padding: 4px 2px;
  line-height: 1;
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 1px;
}

// ---- 底部添加栏 ----
.ha-bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 8px;
  background: white;
  border-top: 2px solid #2563eb;
  padding: 8px 16px;
  padding-bottom: calc(8px + env(safe-area-inset-bottom, 0px));
  box-shadow: 0 -4px 16px rgba(37, 99, 235, 0.1);
  z-index: 10;
}

.ha-bottom-bar__input {
  flex: 1;
  border: 2px solid #e2e8f0;
  border-radius: 999px;
  outline: none;
  padding: 10px 16px;
  font-size: 14px;
  color: #1e293b;
  transition: border-color 0.2s ease;

  &:focus {
    border-color: #2563eb;
  }

  &::placeholder {
    color: #94a3b8;
  }
}

.ha-bottom-bar__btn {
  padding: 10px 24px;
  border: none;
  background: #2563eb;
  color: white;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s ease;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &:not(:disabled):hover {
    opacity: 0.9;
  }
}

// ---- 底部操作菜单 ----
.ha-menu-sheet__divider {
  height: 1px;
  background: #f1f5f9;
  margin: 8px 0;
}

.ha-menu-sheet__action {
  width: 100%;
  padding: 14px 0;
  border: none;
  background: transparent;
  font-size: 16px;
  color: #1e293b;
  cursor: pointer;
  text-align: center;

  &--danger {
    color: #ef4444;
  }

  &--cancel {
    font-weight: 600;
  }
}

// ---- 编辑对话框 ----
.ha-edit-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.ha-edit-form__label {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
}

.ha-edit-form__input {
  width: 100%;
  min-width: 0;
  max-width: 100%;
  padding: 10px 12px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  color: #1e293b;
  outline: none;
  box-sizing: border-box;
  font-family: inherit;

  &:focus {
    border-color: #2563eb;
  }
}

.ha-edit-form__repeat-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ha-edit-form__repeat-btn {
  padding: 6px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 999px;
  background: #fff;
  font-size: 13px;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;

  &.active {
    border-color: #2563eb;
    background: #eff6ff;
    color: #2563eb;
    font-weight: 700;
  }
}

.ha-edit-form__cancel {
  padding: 8px 20px;
  border: 2px solid #e2e8f0;
  border-radius: 999px;
  background: #fff;
  color: #64748b;
  font-size: 14px;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  cursor: pointer;
}

.ha-edit-form__confirm {
  padding: 8px 24px;
  border: none;
  border-radius: 999px;
  background: #2563eb;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  cursor: pointer;
  margin-left: 8px;
}

// 编辑对话框 body 禁止内容溢出
:deep(.el-dialog__body) {
  overflow: hidden;
}

// ---- 删除确认弹窗 ----
.todo-delete-confirm {
  position: fixed;
  inset: 0;
  z-index: 210;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 20px;
  background: rgb(15 23 42 / 45%);
}

.todo-delete-confirm__panel {
  width: min(100%, 320px);

  :deep(.sa-doodle-frame__body) {
    padding: 6px 6px 8px;
  }
}

.todo-delete-confirm__inner {
  padding: 16px 14px 14px;
  font-family: 'ZCOOL KuaiLe', 'Alibaba PuHuiTi', 'PingFang SC', sans-serif;
  text-align: center;
}

.todo-delete-confirm__title-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
}

.todo-delete-confirm__icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.todo-delete-confirm__title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
}

.todo-delete-confirm__message {
  margin: 0 0 18px;
  font-size: 15px;
  line-height: 1.5;
  color: #64748b;
  word-break: break-all;
}

.todo-delete-confirm__actions {
  display: flex;
  gap: 10px;
}

.todo-delete-confirm__btn {
  flex: 1;
  padding: 0;
  border: none;
  background: transparent;
  font-family: inherit;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.14s ease;

  &:active:not(.is-disabled) {
    transform: scale(0.97);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 11px 10px;
    text-align: center;
  }

  &--cancel {
    color: #64748b;
  }

  &--confirm {
    background: #ef4444;
    color: #fff;

    :deep(.sa-doodle-frame__body) {
      position: relative;
      z-index: 3;
      color: #fff;
    }
  }

  &.is-disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.todo-delete-confirm-enter-active,
.todo-delete-confirm-leave-active {
  transition: opacity 0.2s ease;

  .todo-delete-confirm__panel {
    transition: transform 0.22s ease, opacity 0.22s ease;
  }
}

.todo-delete-confirm-enter-from,
.todo-delete-confirm-leave-to {
  opacity: 0;

  .todo-delete-confirm__panel {
    transform: scale(0.92);
    opacity: 0;
  }
}
</style>
