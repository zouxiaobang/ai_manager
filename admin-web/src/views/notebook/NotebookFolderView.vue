<template>
  <div class="notebook-folder-view">
    <header class="notebook-folder-view__header">
      <div class="notebook-folder-view__title-wrap">
        <h3 class="notebook-folder-view__title">{{ folderNode.name }}</h3>
        <p class="notebook-folder-view__meta">
          {{ t('notebook.folderItemCount', { count: displayCount }) }}
        </p>
      </div>

      <el-dropdown trigger="click" @command="onViewModeChange">
        <el-button circle :title="t('notebook.switchView')">
          <el-icon><Grid /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu class="notebook-view-dropdown">
            <el-dropdown-item disabled class="notebook-view-dropdown__group-item">
              {{ t('notebook.viewGroup.folder') }}
            </el-dropdown-item>
            <el-dropdown-item
              v-for="option in folderViewOptions"
              :key="option.mode"
              :command="option.mode"
              :class="{ 'is-active': viewMode === option.mode }"
            >
              <span class="notebook-view-dropdown__item">
                <el-icon><component :is="option.icon" /></el-icon>
                <span>{{ t(option.labelKey) }}</span>
                <el-icon v-if="viewMode === option.mode" class="notebook-view-dropdown__check"><Check /></el-icon>
              </span>
            </el-dropdown-item>
            <el-dropdown-item divided disabled class="notebook-view-dropdown__group-item">
              {{ t('notebook.viewGroup.note') }}
            </el-dropdown-item>
            <el-dropdown-item
              v-for="option in noteViewOptions"
              :key="option.mode"
              :command="option.mode"
              :class="{ 'is-active': viewMode === option.mode }"
            >
              <span class="notebook-view-dropdown__item">
                <el-icon><component :is="option.icon" /></el-icon>
                <span>{{ t(option.labelKey) }}</span>
                <el-icon v-if="viewMode === option.mode" class="notebook-view-dropdown__check"><Check /></el-icon>
              </span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>

    <div v-if="isContentEmpty" class="notebook-folder-view__empty">
      <el-empty :description="emptyDescription" :image-size="72" />
    </div>

    <template v-else>
      <section v-if="showFolderSection" class="notebook-folder-view__section">
        <h4 class="notebook-folder-view__section-title">
          {{ t('notebook.folderSectionWithCount', { count: childFolders.length }) }}
        </h4>

        <div class="notebook-folder-view__folder-grid">
          <button
            v-for="item in childFolders"
            :key="item.nodeKey"
            type="button"
            class="notebook-folder-card"
            @click="emit('open-folder', item)"
          >
            <el-icon class="notebook-folder-card__icon"><Folder /></el-icon>
            <span class="notebook-folder-card__body">
              <span class="notebook-folder-card__name">{{ item.name }}</span>
              <span class="notebook-folder-card__meta">{{ getFolderChildSummary(item) }}</span>
            </span>
          </button>
        </div>
      </section>

      <section v-if="showNoteSection" class="notebook-folder-view__section">
        <h4 class="notebook-folder-view__section-title">
          {{ t('notebook.noteSectionWithCount', { count: childNotes.length }) }}
        </h4>

        <div v-if="noteDisplay === 'card'" class="notebook-folder-view__note-grid">
          <button
            v-for="item in childNotes"
            :key="item.nodeKey"
            type="button"
            class="notebook-note-card"
            @click="emit('open-note', item)"
          >
            <div class="notebook-note-card__title">{{ item.name }}</div>
            <div class="notebook-note-card__preview">{{ getNotePreview(item) }}</div>
          </button>
        </div>

        <div v-else class="notebook-folder-view__note-list">
          <button
            v-for="item in childNotes"
            :key="item.nodeKey"
            type="button"
            class="notebook-note-row"
            :class="{ 'is-title-only': noteDisplay === 'title-list' }"
            @click="emit('open-note', item)"
          >
            <el-icon v-if="noteDisplay === 'detail-list'" class="notebook-note-row__icon">
              <Document />
            </el-icon>
            <div class="notebook-note-row__main">
              <span class="notebook-note-row__title">{{ item.name }}</span>
              <span v-if="noteDisplay === 'detail-list'" class="notebook-note-row__preview">
                {{ getNotePreview(item) }}
              </span>
            </div>
            <span v-if="noteDisplay === 'detail-list'" class="notebook-detail-row__tail">
              <span>{{ getNoteCreateTime(item) }}</span>
              <span class="notebook-detail-row__dot">·</span>
              <span>{{ getNoteFileSize(item) }}</span>
            </span>
          </button>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, Document, Folder, Grid, List, Menu, Operation } from '@element-plus/icons-vue'
import type { Component } from 'vue'
import type { NbTreeNode } from '@/api/notebook'
import {
  formatFileSize,
  formatNoteCreateTime,
  type NoteFolderListMeta,
} from './notePreview'
import {
  loadFolderContentViewMode,
  saveFolderContentViewMode,
  type FolderContentViewMode,
} from './folderViewMode'

const props = defineProps<{
  folderNode: NbTreeNode
  noteMeta: Record<number, NoteFolderListMeta>
}>()

const emit = defineEmits<{
  'open-folder': [node: NbTreeNode]
  'open-note': [node: NbTreeNode]
}>()

const { t } = useI18n()

const viewMode = ref<FolderContentViewMode>(loadFolderContentViewMode())

const folderViewOptions: Array<{
  mode: FolderContentViewMode
  labelKey: string
  icon: Component
}> = [
  { mode: 'folder-card', labelKey: 'notebook.view.folderCard', icon: Grid },
  { mode: 'folder-detail-list', labelKey: 'notebook.view.folderDetailList', icon: Operation },
  { mode: 'folder-title-list', labelKey: 'notebook.view.folderTitleList', icon: Menu },
]

const noteViewOptions: Array<{
  mode: FolderContentViewMode
  labelKey: string
  icon: Component
}> = [
  { mode: 'note-card', labelKey: 'notebook.view.noteCard', icon: Grid },
  { mode: 'note-detail-list', labelKey: 'notebook.view.noteDetailList', icon: List },
  { mode: 'note-title-list', labelKey: 'notebook.view.noteTitleList', icon: Menu },
]

const childFolders = computed(() =>
  (props.folderNode.children ?? []).filter((node) => node.nodeType === 'FOLDER'),
)

const childNotes = computed(() =>
  (props.folderNode.children ?? []).filter((node) => node.nodeType === 'NOTE'),
)

const showFolderSection = computed(() => childFolders.value.length > 0)
const showNoteSection = computed(() => childNotes.value.length > 0)

const noteDisplay = computed<'card' | 'detail-list' | 'title-list'>(() => {
  switch (viewMode.value) {
    case 'folder-detail-list':
    case 'note-detail-list':
      return 'detail-list'
    case 'folder-title-list':
    case 'note-title-list':
      return 'title-list'
    default:
      return 'card'
  }
})

const displayCount = computed(() => childFolders.value.length + childNotes.value.length)

const isContentEmpty = computed(
  () => childFolders.value.length === 0 && childNotes.value.length === 0,
)

const emptyDescription = computed(() => t('notebook.folderEmpty'))

function onViewModeChange(mode: FolderContentViewMode) {
  viewMode.value = mode
  saveFolderContentViewMode(mode)
}

function countChildFolders(node: NbTreeNode): number {
  return (node.children ?? []).filter((child) => child.nodeType === 'FOLDER').length
}

function countChildNotes(node: NbTreeNode): number {
  return (node.children ?? []).filter((child) => child.nodeType === 'NOTE').length
}

function getFolderChildSummary(node: NbTreeNode): string {
  return t('notebook.folderCardSummary', {
    folderCount: countChildFolders(node),
    noteCount: countChildNotes(node),
  })
}

function getNotePreview(item: NbTreeNode): string {
  if (!item.noteId) return t('notebook.contentPlaceholder')
  const excerpt = props.noteMeta[item.noteId]?.contentExcerpt
  if (excerpt?.trim()) return excerpt
  return t('notebook.contentPlaceholder')
}

function getNoteCreateTime(item: NbTreeNode): string {
  if (!item.noteId) return '—'
  return formatNoteCreateTime(props.noteMeta[item.noteId]?.createTime)
}

function getNoteFileSize(item: NbTreeNode): string {
  if (!item.noteId) return '0 B'
  return formatFileSize(props.noteMeta[item.noteId]?.size ?? 0)
}
</script>

<style scoped lang="scss">
.notebook-folder-view {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: auto;
  padding: 4px 0;
}

.notebook-folder-view__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.notebook-folder-view__title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.3;
}

.notebook-folder-view__meta {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.notebook-folder-view__section {
  margin-bottom: 24px;
}

.notebook-folder-view__section-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.notebook-folder-view__folder-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.notebook-folder-card {
  display: inline-flex;
  align-items: flex-start;
  gap: 8px;
  flex: 0 0 172px;
  width: 172px;
  max-width: 100%;
  min-height: 52px;
  padding: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
  box-shadow: 0 1px 2px rgb(15 23 42 / 5%);
  cursor: pointer;
  text-align: left;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    border-color: var(--el-color-primary-light-7);
    box-shadow: 0 2px 8px rgb(64 158 255 / 12%);
  }
}

.notebook-folder-card__icon {
  flex-shrink: 0;
  margin-top: 2px;
  font-size: 16px;
  color: var(--el-color-primary-light-3);
}

.notebook-folder-card__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.notebook-folder-card__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.4;
  color: var(--el-text-color-primary);
}

.notebook-folder-card__meta {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  line-height: 1.3;
  color: var(--el-text-color-placeholder);
}

.notebook-folder-view__note-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notebook-note-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s ease, background 0.15s ease;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    background: var(--el-fill-color-light);
  }
}

.notebook-detail-row__tail {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  white-space: nowrap;
}

.notebook-detail-row__dot {
  color: var(--el-text-color-disabled);
}

.notebook-note-row__main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.notebook-folder-view__note-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
}

.notebook-note-card {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  min-height: 148px;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    box-shadow: 0 4px 12px rgb(0 0 0 / 8%);
  }
}

.notebook-note-card__title {
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notebook-note-card__preview {
  flex: 1;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  white-space: pre-wrap;
}

.notebook-note-row__icon {
  flex-shrink: 0;
  font-size: 18px;
  color: var(--el-text-color-secondary);
}

.notebook-note-row__title {
  flex-shrink: 0;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.notebook-note-row.is-title-only .notebook-note-row__title {
  max-width: none;
  font-size: 15px;
  font-weight: 500;
}

.notebook-note-row__preview {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.notebook-note-row.is-title-only {
  padding: 10px 14px;
}

.notebook-folder-view__empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>

<style lang="scss">
.notebook-view-dropdown {
  min-width: 220px;
  padding: 6px 0;
}

.notebook-view-dropdown__group-item {
  font-size: 12px !important;
  color: var(--el-text-color-secondary) !important;
  cursor: default !important;
  opacity: 1 !important;
}

.notebook-view-dropdown__group {
  padding: 6px 16px 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  list-style: none;
}

.notebook-view-dropdown__divider {
  height: 1px;
  margin: 6px 0;
  background: var(--el-border-color-lighter);
  list-style: none;
}

.notebook-view-dropdown__item {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.notebook-view-dropdown__check {
  margin-left: auto;
  font-size: 14px;
  color: var(--el-color-primary);
}

.el-dropdown-menu__item.is-active {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
</style>
