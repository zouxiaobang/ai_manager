<template>
  <div v-loading="loading" class="mobile-notebook">
    <div class="notebook-header">
      <div class="notebook-title-wrap">
        <img :src="assets.starYellow" class="notebook-title-star" alt="" />
        <h2 class="notebook-title">{{ t('portal.menu.notebook') }}</h2>
        <img :src="assets.starBlue" class="notebook-title-star" alt="" />
      </div>
    </div>

    <SchemeADoodleFrame shape="pill" color="#2563eb" class="notebook-search">
      <div class="notebook-search-inner">
        <img :src="assets.search" class="notebook-search-icon" alt="" />
        <input
          v-model="keyword"
          type="search"
          enterkeyhint="search"
          :placeholder="t('notebook.searchPlaceholder')"
        />
      </div>
    </SchemeADoodleFrame>

    <div v-if="pinnedNotes.length" class="notebook-section">
      <div class="notebook-section-head">
        <img :src="assets.starYellow" class="notebook-section-icon" alt="" />
        <h3 class="notebook-section-title">{{ t('mobile.notebook.pinned') }}</h3>
      </div>

      <div class="notebook-notes-list">
        <SchemeADoodleFrame color="#fbbf24" class="notebook-note-card notebook-note-card--pinned" v-for="item in pinnedNotes" :key="item.id" @click="openNote(item.id)">
          <img :src="assets.paperclip" class="notebook-paperclip" alt="" />
          <div class="notebook-note-card__body">
            <div class="notebook-note-card__title">📌 {{ item.title }}</div>
            <div v-if="item.folderPath" class="notebook-note-card__meta">📁 {{ item.folderPath }}</div>
          </div>
          <img :src="assets.squiggleRed" class="notebook-squiggle" alt="" />
        </SchemeADoodleFrame>
      </div>
    </div>

    <div class="notebook-section">
      <div class="notebook-section-head">
        <img :src="assets.starBlue" class="notebook-section-icon" alt="" />
        <h3 class="notebook-section-title">{{ t('mobile.notebook.folders') }}</h3>
      </div>

      <div class="notebook-folder-grid">
        <SchemeADoodleFrame color="#f97316" class="notebook-folder-card" v-for="folder in rootFolders" :key="folder.nodeKey" @click="openFolder(folder)">
          <div class="notebook-folder-card__body">
            <div class="notebook-folder-card__icon">📂</div>
            <div class="notebook-folder-card__info">
              <div class="notebook-folder-card__name">{{ folder.name }}</div>
              <div class="notebook-folder-card__meta">{{ getFolderSummary(folder) }}</div>
            </div>
            <div class="notebook-folder-card__arrow">→</div>
          </div>
        </SchemeADoodleFrame>
      </div>
    </div>

    <div v-if="rootFolders.length === 0 && pinnedNotes.length === 0" class="notebook-empty">
      <div class="notebook-empty-icon">📝</div>
      <div class="notebook-empty-text">{{ t('notebook.emptyTree') }}</div>
    </div>

    <SchemeADoodleFrame tag="button" shape="pill" color="#2563eb" class="notebook-create-btn" @click="onCreateNote">
      <span class="notebook-create-btn__text">{{ t('notebook.newNote') }}</span>
    </SchemeADoodleFrame>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { createNoteRequest, fetchNotebookTree } from '@/api/notebook'
import type { NbTreeNode } from '@/api/notebook'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'

const router = useRouter()
const { t } = useI18n()
const assets = schemeAAssets

const loading = ref(false)
const keyword = ref('')
const rootFolders = ref<NbTreeNode[]>([])
const pinnedNotes = ref<Array<{ id: number; title: string; folderPath: string; isPinned: boolean }>>([])

function countChildFolders(node: NbTreeNode): number {
  let count = 0
  if (node.children?.length) {
    for (const child of node.children) {
      if (child.nodeType === 'FOLDER') {
        count += 1 + countChildFolders(child)
      }
    }
  }
  return count
}

function countChildNotes(node: NbTreeNode): number {
  let count = 0
  if (node.nodeType === 'NOTE') {
    count = 1
  }
  if (node.children?.length) {
    count += node.children.reduce((acc, child) => acc + countChildNotes(child), 0)
  }
  return count
}

function getFolderSummary(node: NbTreeNode): string {
  const folders = countChildFolders(node)
  const notes = countChildNotes(node)
  return `${notes} 笔记 · ${folders} 子文件夹`
}

function openFolder(folder: NbTreeNode) {
  router.push(`/notebook/folder/${folder.nodeKey}`)
}

function openNote(id: number) {
  router.push(`/notebook/${id}`)
}

async function onCreateNote() {
  const created = await createNoteRequest({
    title: t('notebook.untitled'),
    content: '',
    tagIds: [],
  })
  router.push(`/notebook/${created.id}`)
}

async function loadNotes() {
  loading.value = true
  try {
    const tree = await fetchNotebookTree()
    rootFolders.value = tree

    const pinned: typeof pinnedNotes.value = []
    function collectPinned(node: NbTreeNode, folderPath: string) {
      if (node.nodeType === 'NOTE' && node.noteId && node.isPinned === 1) {
        pinned.push({
          id: node.noteId,
          title: node.name,
          folderPath,
          isPinned: true,
        })
      }
      if (node.children?.length) {
        const nextPath =
          node.nodeType === 'FOLDER'
            ? folderPath
              ? `${folderPath} / ${node.name}`
              : node.name
            : folderPath
        for (const child of node.children) {
          collectPinned(child, nextPath)
        }
      }
    }
    for (const node of tree) {
      collectPinned(node, '')
    }
    pinnedNotes.value = pinned
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadNotes()
})
</script>

<style scoped lang="scss">
.mobile-notebook {
  padding: 16px;
  background: #faf8f5;
  min-height: calc(100vh - 120px);
  min-height: calc(100dvh - 120px);
}

.notebook-header {
  margin-bottom: 16px;
}

.notebook-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notebook-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 24px;
  color: #1e293b;
  margin: 0;
}

.notebook-title-star {
  width: 24px;
  height: 24px;
}

.notebook-search {
  margin-bottom: 16px;
}

.notebook-search-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
}

.notebook-search-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.notebook-search-inner input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
}

.notebook-search-inner input::placeholder {
  color: #94a3b8;
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
  gap: 12px;
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

.notebook-folder-card__arrow {
  font-size: 16px;
  color: #f97316;
  font-weight: bold;
}

.notebook-notes-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notebook-note-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  &--pinned {
    background: #fffbeb;
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

.notebook-note-card__meta {
  font-size: 11px;
  color: #94a3b8;
  margin: 0;
}

.notebook-paperclip {
  position: absolute;
  top: -8px;
  right: 16px;
  width: 32px;
  height: 32px;
  z-index: 3;
}

.notebook-squiggle {
  position: absolute;
  bottom: -6px;
  left: 0;
  right: 0;
  height: 12px;
  width: 100%;
  z-index: 3;
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
