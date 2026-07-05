<template>
  <div v-loading="loading" class="mobile-notebook-folder">
    <div class="notebook-folder-header">
      <button class="notebook-folder-back" @click="goBack">
        <span class="notebook-folder-back-icon">←</span>
      </button>
      <div class="notebook-folder-title-wrap">
        <h2 class="notebook-folder-title">{{ currentFolder?.name }}</h2>
        <p class="notebook-folder-meta">{{ childFolders.length + childNotes.length }} 项内容</p>
      </div>
    </div>

    <div v-if="childFolders.length" class="notebook-section">
      <div class="notebook-section-head">
        <img :src="assets.starBlue" class="notebook-section-icon" alt="" />
        <h3 class="notebook-section-title">{{ t('mobile.notebook.folders') }} ({{ childFolders.length }})</h3>
      </div>

      <div class="notebook-folder-grid">
        <SchemeADoodleFrame color="#f97316" class="notebook-folder-card" v-for="folder in childFolders" :key="folder.nodeKey" @click="openFolder(folder)">
          <div class="notebook-folder-card__body">
            <div class="notebook-folder-card__icon">📂</div>
            <div class="notebook-folder-card__info">
              <div class="notebook-folder-card__name">{{ folder.name }}</div>
              <div class="notebook-folder-card__meta">{{ getFolderSummary(folder) }}</div>
            </div>
          </div>
        </SchemeADoodleFrame>
      </div>
    </div>

    <div v-if="childNotes.length" class="notebook-section">
      <div class="notebook-section-head">
        <img :src="assets.starYellow" class="notebook-section-icon" alt="" />
        <h3 class="notebook-section-title">{{ t('mobile.notebook.notes') }} ({{ childNotes.length }})</h3>
      </div>

      <div class="notebook-notes-list">
        <SchemeADoodleFrame color="#2563eb" class="notebook-note-card" v-for="note in childNotes" :key="note.nodeKey" @click="openNote(note.noteId)">
          <div class="notebook-note-card__body">
            <div class="notebook-note-card__title">📄 {{ note.name }}</div>
            <div v-if="getNotePreview(note)" class="notebook-note-card__preview">{{ getNotePreview(note) }}</div>
          </div>
        </SchemeADoodleFrame>
      </div>
    </div>

    <div v-if="childFolders.length === 0 && childNotes.length === 0" class="notebook-empty">
      <div class="notebook-empty-icon">📝</div>
      <div class="notebook-empty-text">{{ t('notebook.folderEmpty') }}</div>
    </div>

    <SchemeADoodleFrame tag="button" shape="pill" color="#2563eb" class="notebook-create-btn" @click="onCreateNote">
      <span class="notebook-create-btn__text">{{ t('notebook.newNote') }}</span>
    </SchemeADoodleFrame>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { createNoteRequest, fetchNotebookTree } from '@/api/notebook'
import type { NbTreeNode } from '@/api/notebook'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const assets = schemeAAssets

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

function countChildFolders(node: NbTreeNode): number {
  return (node.children ?? []).filter((child) => child.nodeType === 'FOLDER').length
}

function countChildNotes(node: NbTreeNode): number {
  return (node.children ?? []).filter((child) => child.nodeType === 'NOTE').length
}

function getFolderSummary(node: NbTreeNode): string {
  const folders = countChildFolders(node)
  const notes = countChildNotes(node)
  return `${notes} 笔记 · ${folders} 子文件夹`
}

function getNotePreview(node: NbTreeNode): string {
  if (!node.contentExcerpt?.trim()) return ''
  return node.contentExcerpt
}

function goBack() {
  router.back()
}

function openFolder(folder: NbTreeNode) {
  router.push(`/notebook/folder/${folder.nodeKey}`)
}

function openNote(noteId?: number) {
  if (!noteId) return
  router.push(`/notebook/${noteId}`)
}

async function onCreateNote() {
  const created = await createNoteRequest({
    title: t('notebook.untitled'),
    content: '',
    tagIds: [],
  })
  router.push(`/notebook/${created.id}`)
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

onMounted(() => {
  void loadTree()
})
</script>

<style scoped lang="scss">
.mobile-notebook-folder {
  padding: 16px;
  background: #faf8f5;
  min-height: calc(100vh - 120px);
  min-height: calc(100dvh - 120px);
}

.notebook-folder-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.notebook-folder-back {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid #2563eb;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }
}

.notebook-folder-back-icon {
  font-size: 18px;
  color: #2563eb;
  font-weight: bold;
}

.notebook-folder-title-wrap {
  flex: 1;
}

.notebook-folder-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 24px;
  color: #1e293b;
  margin: 0 0 4px;
}

.notebook-folder-meta {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

.notebook-section {
  margin-bottom: 20px;
}

.notebook-section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.notebook-section-icon {
  width: 20px;
  height: 20px;
}

.notebook-section-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 18px;
  color: #1e293b;
  margin: 0;
}

.notebook-folder-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notebook-folder-card {
  background: #fff7ed;
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }
}

.notebook-folder-card__body {
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.notebook-folder-card__icon {
  font-size: 24px;
}

.notebook-folder-card__info {
  flex: 1;
}

.notebook-folder-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
  margin: 0 0 4px;
}

.notebook-folder-card__meta {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

.notebook-notes-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notebook-note-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }
}

.notebook-note-card__body {
  padding: 14px;
}

.notebook-note-card__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
  margin: 0 0 6px;
}

.notebook-note-card__preview {
  font-size: 12px;
  color: #64748b;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notebook-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
}

.notebook-empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.notebook-empty-text {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #94a3b8;
}

.notebook-create-btn {
  margin-top: 8px;
  margin-bottom: 40px;
  padding: 14px;
  cursor: pointer;
  background: #2563eb;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }
}

.notebook-create-btn__text {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 18px;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
</style>
