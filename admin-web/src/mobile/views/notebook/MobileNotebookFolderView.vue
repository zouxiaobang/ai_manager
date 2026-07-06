<template>
  <div v-loading="loading" class="mobile-notebook-folder-d">
    <!-- Header with back button -->
    <div class="folder-d__nav">
      <button class="folder-d__back" @click="goBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <div class="folder-d__title-area">
        <h2 class="folder-d__title">{{ currentFolder?.name }}</h2>
        <span class="folder-d__meta">{{ childFolders.length + childNotes.length }} {{ t('notebook.folderItemCount', { count: childFolders.length + childNotes.length }) }}</span>
      </div>
    </div>

    <!-- Breadcrumb -->
    <div class="folder-d__breadcrumb">
      <span class="folder-d__crumb" @click="goHome">
        <svg width="12" height="12" class="crumb-folder-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
        {{ t('notebook.treeTitle') }}
      </span>
      <span class="folder-d__sep">/</span>
      <span class="folder-d__crumb folder-d__crumb--current">{{ currentFolder?.name }}</span>
    </div>

    <!-- Sub Folders -->
    <div v-if="childFolders.length" class="folder-d__section">
      <div class="folder-d__section-label folder-d__section-label--folder">
        <svg width="16" height="16" class="section-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
        <span>{{ t('notebook.folderSection') }}</span>
        <span class="folder-d__section-count">{{ childFolders.length }}</span>
      </div>

      <div class="folder-d__card-list">
        <SchemeADoodleFrame
          v-for="folder in childFolders"
          :key="folder.nodeKey"
          :seed="getSeedFromKey(folder.nodeKey)"
          color="#f97316"
          sketch
          :shadow="false"
          class="folder-d__card folder-d__card--folder"
          @click="openFolder(folder)"
        >
          <div class="folder-d__card-body">
            <div class="folder-d__card-row">
              <svg width="20" height="20" class="folder-d__card-folder-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
              <div class="folder-d__card-info">
                <span class="folder-d__card-title">{{ folder.name }}</span>
                <span class="folder-d__card-meta">{{ getFolderSummary(folder) }}</span>
              </div>
              <span class="folder-d__card-arrow">›</span>
            </div>
          </div>
        </SchemeADoodleFrame>
      </div>
    </div>

    <!-- Notes -->
    <div v-if="childNotes.length" class="folder-d__section">
      <div class="folder-d__section-label folder-d__section-label--note">
        <svg width="16" height="16" class="section-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
        <span>{{ t('notebook.noteSection') }}</span>
        <span class="folder-d__section-count">{{ childNotes.length }}</span>
      </div>

      <div class="folder-d__card-list">
        <SchemeADoodleFrame
          v-for="note in childNotes"
          :key="note.nodeKey"
          :seed="getSeedFromKey(note.nodeKey)"
          color="#2563eb"
          sketch
          :shadow="false"
          class="folder-d__card folder-d__card--note"
          @click="openNote(note.noteId)"
        >
          <div class="folder-d__card-body">
            <div class="folder-d__card-title-row">
              <svg width="16" height="16" class="folder-d__card-note-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
              <span class="folder-d__card-title">{{ note.name }}</span>
            </div>
            <div v-if="getNotePreview(note)" class="folder-d__card-preview">{{ getNotePreview(note) }}</div>
          </div>
        </SchemeADoodleFrame>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="childFolders.length === 0 && childNotes.length === 0" class="folder-d__empty">
      <svg width="48" height="48" class="folder-d__empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
      <div class="folder-d__empty-text">{{ t('notebook.folderEmpty') }}</div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { fetchNotebookTree } from '@/api/notebook'
import type { NbTreeNode } from '@/api/notebook'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()

const loading = ref(false)
const treeData = ref<NbTreeNode[]>([])
const currentFolderKey = ref<string>('')

const currentFolder = computed(() => {
  function findNode(nodes: NbTreeNode[], nodeKey: string): NbTreeNode | undefined {
    for (const node of nodes) {
      if (node.nodeKey === nodeKey) return node
      if (node.children) {
        const found = findNode(node.children, nodeKey)
        if (found) return found
      }
    }
    return undefined
  }
  return findNode(treeData.value, currentFolderKey.value)
})

const childFolders = computed(() => {
  return (currentFolder.value?.children ?? []).filter((node) => node.nodeType === 'FOLDER')
})

const childNotes = computed(() => {
  return (currentFolder.value?.children ?? []).filter((node) => node.nodeType === 'NOTE')
})

function getSeedFromKey(key: string): number {
  let hash = 0
  for (let i = 0; i < key.length; i++) {
    hash = ((hash << 5) - hash) + key.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash)
}

function countChildFolders(node: NbTreeNode): number {
  let count = 0
  if (node.children?.length) {
    for (const child of node.children) {
      if (child.nodeType === 'FOLDER') count += 1 + countChildFolders(child)
    }
  }
  return count
}

function countChildNotes(node: NbTreeNode): number {
  let count = 0
  if (node.nodeType === 'NOTE') count = 1
  if (node.children?.length) {
    count += node.children.reduce((acc, child) => acc + countChildNotes(child), 0)
  }
  return count
}

function getFolderSummary(node: NbTreeNode): string {
  const notes = countChildNotes(node)
  const folders = countChildFolders(node)
  const parts: string[] = []
  if (notes > 0) parts.push(`${notes} ${t('notebook.stats.notes')}`)
  if (folders > 0) parts.push(`${folders} ${t('notebook.stats.folders')}`)
  return parts.join(' · ') || t('notebook.folderEmpty')
}

function getNotePreview(node: NbTreeNode): string {
  console.log(node.contentExcerpt)
  if (!node.contentExcerpt?.trim()) return ''
  return node.contentExcerpt
}

function goBack() {
  router.back()
}

function goHome() {
  router.push('/notebook')
}

function openFolder(folder: NbTreeNode) {
  router.push(`/notebook/folder/${folder.nodeKey}`)
}

function openNote(noteId?: number) {
  if (!noteId) return
  router.push(`/notebook/${noteId}`)
}

async function loadTree() {
  loading.value = true
  try {
    treeData.value = await fetchNotebookTree()
    currentFolderKey.value = route.params.folderKey as string
  } finally {
    loading.value = false
  }
}

onMounted(() => { void loadTree() })

// Watch for route param changes (navigating to a different folder in the same route)
watch(
  () => route.params.folderKey,
  (newKey) => {
    if (newKey && newKey !== currentFolderKey.value) {
      void loadTree()
    }
  }
)
</script>

<style scoped lang="scss">
$blue: #2563eb;
$orange: #f97316;
$text-primary: #1e293b;
$text-secondary: #64748b;
$text-muted: #94a3b8;
$border: #e2e8f0;
$bg: #f0ede8;
$surface: #faf8f5;

.mobile-notebook-folder-d {
  padding: 0;
  background: white;
  min-height: calc(100vh - 120px);
  min-height: calc(100dvh - 120px);
}

// Navigation
.folder-d__nav {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: $surface;
}

.folder-d__back {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 2px solid $blue;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: $blue;
  transition: all 0.15s;
  flex-shrink: 0;

  &:hover { background: #dbeafe; }
  &:active { transform: scale(0.95); }
}

.folder-d__title-area {
  flex: 1;
  min-width: 0;
}

.folder-d__title {
  font-family: 'ZCOOL KuaiLe', cursive;
  font-size: 20px;
  color: $text-primary;
  margin: 0;
  line-height: 1.2;
}

.folder-d__meta {
  font-size: 11px;
  color: $text-muted;
}

.folder-d__more {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 2px solid $border;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  color: $text-muted;
  flex-shrink: 0;

  &:hover { border-color: $text-muted; }
}

// Breadcrumb
.folder-d__breadcrumb {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 16px 10px;
  font-size: 12px;
  color: $text-muted;
  background: $surface;
}

.folder-d__crumb {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  &:hover { color: $blue; }
  &--current {
    color: $text-secondary;
    font-weight: 500;
  }

  .crumb-folder-icon {
    width: 12px;
    height: 12px;
    display: block;
    flex-shrink: 0;
  }
}

.folder-d__sep {
  color: $border;
}

// Sections
.folder-d__section {
  padding: 12px 12px 0;

  &-label {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 700;
    padding: 0 4px 8px;

    &--folder { color: $orange; }
    &--note { color: $blue; }

    .section-icon {
      width: 16px;
      height: 16px;
      display: block;
      flex-shrink: 0;
    }
  }

  &-count {
    font-size: 11px;
    font-weight: 600;
    background: rgba(0,0,0,0.06);
    padding: 0 8px;
    border-radius: 8px;
    line-height: 18px;
  }
}

.folder-d__card-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

// Cards
.folder-d__card {
  cursor: pointer;
  transition: all 0.2s ease;
  overflow: hidden;

  &:hover { transform: scale(0.99); }
  &:active { transform: scale(0.98); }

  &--folder { background: #fff; }
  &--note { background: white; }

  &-stripe {
    position: absolute;
    left: 0;
    top: 4px;
    bottom: 4px;
    width: 4px;
    border-radius: 2px;
    z-index: 3;
    opacity: 0.8;
  }

  &-body {
    padding: 12px 20px 12px 24px;
    position: relative;
    z-index: 1;
  }

  &-row {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  &-title-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 4px;
  }

  &-folder-icon { width: 20px; height: 20px; flex-shrink: 0; display: block; }
  &-note-icon { width: 16px; height: 16px; flex-shrink: 0; display: block; }

  &-title {
    font-family: 'ZCOOL KuaiLe', cursive;
    font-size: 15px;
    color: $text-primary;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &-info { flex: 1; min-width: 0; }

  &-meta {
    font-size: 11px;
    color: $text-muted;
    margin-top: 2px;
    display: block;
  }

  &-preview {
    font-size: 12px;
    color: $text-secondary;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    line-height: 1.5;
  }

  &-arrow {
    font-size: 18px;
    color: $orange;
    font-weight: bold;
    flex-shrink: 0;
  }
}

// Empty
.folder-d__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;

  &-icon { width: 48px; height: 48px; margin-bottom: 12px; color: $text-muted; }
  &-text {
    font-family: 'ZCOOL KuaiLe', cursive;
    font-size: 16px;
    color: $text-muted;
  }
}

// Create button
.folder-d__create-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 16px 12px 40px;
  padding: 14px;
  border-radius: 14px;
  border: none;
  background: $blue;
  color: white;
  font-family: 'ZCOOL KuaiLe', cursive;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
  width: calc(100% - 24px);

  &:hover { transform: scale(0.99); }
  &:active { transform: scale(0.98); }
}
</style>
