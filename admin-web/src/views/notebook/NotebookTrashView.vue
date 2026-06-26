<template>
  <div v-loading="loading" class="notebook-trash">
    <header class="notebook-trash__header">
      <div class="notebook-trash__heading">
        <h2 class="notebook-trash__title">{{ t('notebook.tabs.trash') }}</h2>
        <p class="notebook-trash__subtitle">
          {{ t('notebook.trash.subtitle', { count: items.length }) }}
        </p>
      </div>

      <div class="notebook-trash__toolbar">
        <el-input
          v-model="keyword"
          class="notebook-trash__search"
          clearable
          :prefix-icon="Search"
          :placeholder="t('notebook.trash.searchPlaceholder')"
        />
        <el-button
          class="notebook-trash__empty-btn"
          :disabled="!items.length"
          :icon="Delete"
          @click="onEmptyTrash"
        >
          {{ t('notebook.trash.emptyAll') }}
        </el-button>
      </div>
    </header>

    <div v-if="filteredItems.length" class="notebook-trash__grid">
      <article v-for="row in filteredItems" :key="row.id" class="notebook-trash-card">
        <div class="notebook-trash-card__head">
          <span class="notebook-trash-card__icon" aria-hidden="true">
            <el-icon><Document /></el-icon>
          </span>
          <div class="notebook-trash-card__title-wrap">
            <h3 class="notebook-trash-card__title">{{ displayTitle(row) }}</h3>
            <span v-if="row.notebookName" class="notebook-trash-card__tag">{{ row.notebookName }}</span>
          </div>
        </div>

        <p class="notebook-trash-card__time">{{ formatDeletedAt(row.updateTime) }}</p>
        <p class="notebook-trash-card__preview">{{ getPreview(row) }}</p>

        <div class="notebook-trash-card__actions">
          <button type="button" class="notebook-trash-card__action is-restore" @click="onRestore(row.id)">
            {{ t('notebook.restore') }}
          </button>
          <span class="notebook-trash-card__divider" aria-hidden="true" />
          <button type="button" class="notebook-trash-card__action is-purge" @click="onPurge(row.id)">
            {{ t('notebook.purge') }}
          </button>
        </div>
      </article>
    </div>

    <el-empty
      v-else-if="!loading"
      class="notebook-trash__empty"
      :description="emptyDescription"
      :image-size="88"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Delete, Document, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchTrashNotes,
  purgeAllTrashNotes,
  purgeNote,
  restoreNote,
  type NbNoteTrashItem,
} from '@/api/notebook'

const emit = defineEmits<{
  'count-change': [count: number]
  restored: []
}>()

const { t } = useI18n()

const loading = ref(false)
const items = ref<NbNoteTrashItem[]>([])
const keyword = ref('')

const filteredItems = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) return items.value
  return items.value.filter((item) => {
    const title = (item.title ?? '').toLowerCase()
    const notebook = (item.notebookName ?? '').toLowerCase()
    const preview = (item.contentExcerpt ?? '').toLowerCase()
    return title.includes(query) || notebook.includes(query) || preview.includes(query)
  })
})

const emptyDescription = computed(() =>
  keyword.value.trim() ? t('notebook.trash.searchEmpty') : t('notebook.trashEmpty'),
)

function displayTitle(row: NbNoteTrashItem) {
  const title = row.title?.trim()
  return title || t('notebook.untitled')
}

function getPreview(row: NbNoteTrashItem) {
  const excerpt = row.contentExcerpt?.trim()
  if (excerpt) return excerpt
  return t('notebook.trash.noPreview')
}

function formatDeletedAt(value?: string) {
  if (!value) return t('notebook.trash.deletedUnknown')
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return value

  const now = new Date()
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const time = `${hours}:${minutes}`

  const isToday =
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()

  if (isToday) {
    return t('notebook.trash.deletedToday', { time })
  }

  const weekdays = [
    t('notebook.todos.weekdays.sun'),
    t('notebook.todos.weekdays.mon'),
    t('notebook.todos.weekdays.tue'),
    t('notebook.todos.weekdays.wed'),
    t('notebook.todos.weekdays.thu'),
    t('notebook.todos.weekdays.fri'),
    t('notebook.todos.weekdays.sat'),
  ]

  return t('notebook.trash.deletedAt', {
    month: date.getMonth() + 1,
    day: date.getDate(),
    weekday: weekdays[date.getDay()] ?? '',
    time,
  })
}

async function reload() {
  loading.value = true
  try {
    items.value = await fetchTrashNotes()
    emit('count-change', items.value.length)
  } finally {
    loading.value = false
  }
}

async function onRestore(id: number) {
  await restoreNote(id)
  ElMessage.success(t('notebook.restore'))
  await reload()
  emit('restored')
}

async function onPurge(id: number) {
  await ElMessageBox.confirm(t('notebook.purgeConfirm'), { type: 'warning' })
  await purgeNote(id)
  ElMessage.success(t('pomodoro.common.deleted'))
  await reload()
}

async function onEmptyTrash() {
  if (!items.value.length) return
  await ElMessageBox.confirm(t('notebook.trash.emptyAllConfirm'), { type: 'warning' })
  await purgeAllTrashNotes()
  ElMessage.success(t('notebook.trash.emptyAllDone'))
  await reload()
}

onMounted(() => {
  void reload()
})

defineExpose({ reload })
</script>

<style scoped lang="scss">
.notebook-trash {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 360px;
}

.notebook-trash__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.notebook-trash__heading {
  min-width: 0;
}

.notebook-trash__title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
  color: var(--wr-text, #333);
}

.notebook-trash__subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--wr-muted, #999);
}

.notebook-trash__toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-left: auto;
}

.notebook-trash__search {
  width: 260px;
}

.notebook-trash__empty-btn {
  --el-button-text-color: #dc2626;
  --el-button-border-color: #fca5a5;
  --el-button-bg-color: #fff;
  --el-button-hover-text-color: #b91c1c;
  --el-button-hover-border-color: #f87171;
  --el-button-hover-bg-color: #fef2f2;
}

.notebook-trash__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.notebook-trash-card {
  display: flex;
  flex-direction: column;
  min-height: 220px;
  padding: 16px;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 12px;
  background: var(--wr-card, #fff);
  box-shadow: var(--wr-shadow, 0 4px 12px rgb(0 0 0 / 5%));
}

.notebook-trash-card__head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.notebook-trash-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 10px;
  background: var(--wr-stat-blue-bg, #eff6ff);
  color: var(--wr-stat-blue, #2563eb);
  font-size: 20px;
}

.notebook-trash-card__title-wrap {
  min-width: 0;
  flex: 1;
}

.notebook-trash-card__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.45;
  color: var(--wr-text, #333);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notebook-trash-card__tag {
  display: inline-flex;
  margin-top: 8px;
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--wr-stat-gray-bg, #f3f4f6);
  color: var(--wr-text-secondary, #666);
  font-size: 12px;
  line-height: 1.5;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notebook-trash-card__time {
  margin: 14px 0 0;
  font-size: 12px;
  color: var(--wr-muted, #999);
}

.notebook-trash-card__preview {
  flex: 1;
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.65;
  color: var(--wr-text-secondary, #666);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notebook-trash-card__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--wr-border, #e8ecef);
}

.notebook-trash-card__action {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;

  &.is-restore {
    color: var(--wr-stat-blue, #2563eb);

    &:hover {
      color: #1d4ed8;
    }
  }

  &.is-purge {
    color: #dc2626;

    &:hover {
      color: #b91c1c;
    }
  }
}

.notebook-trash-card__divider {
  width: 1px;
  height: 14px;
  background: var(--wr-border, #e8ecef);
}

.notebook-trash__empty {
  padding: 48px 0;
}

@media (max-width: 1200px) {
  .notebook-trash__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .notebook-trash__toolbar {
    width: 100%;
    margin-left: 0;
  }

  .notebook-trash__search {
    flex: 1;
    min-width: 0;
    width: auto;
  }

  .notebook-trash__grid {
    grid-template-columns: 1fr;
  }
}
</style>
