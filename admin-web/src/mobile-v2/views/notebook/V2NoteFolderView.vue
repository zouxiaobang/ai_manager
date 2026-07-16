<template>
  <V2Page>
    <div v-if="loading" class="v2-note-folder__status">加载中…</div>

    <template v-else>
      <V2Card v-if="subFolders.length > 0">
        <div class="section-header">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#f97316" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
          <span>{{ t('notebook.stats.folders') }}</span>
          <span class="section-header__count">{{ subFolders.length }}</span>
        </div>
        <div class="folder-grid">
          <div
            v-for="folder in subFolders"
            :key="folder.nodeKey"
            class="folder-card"
            @click="openFolder(folder)"
          >
            <div class="folder-card__icon" :style="{ background: getFolderColor(folder.nodeKey) }">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
            </div>
            <div class="folder-card__name">{{ folder.name }}</div>
            <div class="folder-card__count">{{ countFolderNotes(folder) }} {{ t('notebook.stats.notes') }}</div>
          </div>
        </div>
      </V2Card>

      <V2Card v-if="directNotes.length > 0">
        <div class="section-header">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--wr-stat-blue, #2563eb)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          <span>{{ folderName }}</span>
          <span class="section-header__count">{{ directNotes.length }}</span>
        </div>
        <div class="note-list">
          <div
            v-for="note in directNotes"
            :key="note.id"
            class="note-item"
            @click="openNote(note.id)"
          >
            <div class="note-item__icon" :style="{ background: getNoteColor(note.id) }">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="8" y="2" width="8" height="4" rx="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg>
            </div>
            <div class="note-item__body">
              <div class="note-item__title">{{ note.title }}</div>
              <div class="note-item__excerpt">{{ note.contentExcerpt || t('notebook.emptyContent') }}</div>
            </div>
          </div>
        </div>
      </V2Card>

      <div v-if="subFolders.length === 0 && directNotes.length === 0" class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--wr-muted, #999999)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
        <div class="empty-state__text">{{ t('notebook.emptyTree') }}</div>
      </div>
    </template>
  </V2Page>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { fetchNotebookTree, type NbTreeNode } from '@/api/notebook'
import V2Page from '@/mobile-v2/components/V2Page.vue'
import V2Card from '@/mobile-v2/components/V2Card.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const loading = ref(true)
const folderName = ref('')
const subFolders = ref<NbTreeNode[]>([])
const directNotes = ref<Array<{ id: number; title: string; contentExcerpt?: string }>>([])

const FOLDER_COLORS = ['#2563eb', '#7c3aed', '#059669', '#ea580c', '#0891b2', '#dc2626']
const NOTE_COLORS = ['#2563eb', '#7c3aed', '#059669', '#ea580c', '#0891b2', '#f97316', '#d97706', '#6b7280']

function getFolderColor(key: string): string {
  let hash = 0
  for (let i = 0; i < key.length; i++) {
    hash = ((hash << 5) - hash) + key.charCodeAt(i)
    hash |= 0
  }
  return FOLDER_COLORS[Math.abs(hash) % FOLDER_COLORS.length]
}

function getNoteColor(id: number): string {
  return NOTE_COLORS[id % NOTE_COLORS.length]
}

function countFolderNotes(node: NbTreeNode): number {
  let count = 0
  if (node.nodeType === 'NOTE') count = 1
  if (node.children?.length) {
    count += node.children.reduce((acc, child) => acc + countFolderNotes(child), 0)
  }
  return count
}

async function loadFolder(folderKey: string) {
  loading.value = true
  folderName.value = ''
  subFolders.value = []
  directNotes.value = []
  try {
    const tree = await fetchNotebookTree()
    function findFolder(nodes: NbTreeNode[]): NbTreeNode | null {
      for (const n of nodes) {
        if (n.nodeType === 'FOLDER' && n.nodeKey === folderKey) return n
        if (n.children?.length) {
          const found = findFolder(n.children)
          if (found) return found
        }
      }
      return null
    }
    const folder = findFolder(tree)
    if (folder) {
      folderName.value = folder.name
      subFolders.value = (folder.children || []).filter(c => c.nodeType === 'FOLDER')
      directNotes.value = (folder.children || [])
        .filter(c => c.nodeType === 'NOTE' && c.noteId)
        .map(c => ({ id: c.noteId!, title: c.name, contentExcerpt: c.contentExcerpt }))
    }
  } finally {
    loading.value = false
  }
}

watch(() => route.params.key, (key) => {
  if (key) void loadFolder(key as string)
}, { immediate: true })

function openFolder(folder: NbTreeNode) {
  router.push(`/notebook/folder/${folder.nodeKey}`)
}

function openNote(id: number) {
  router.push(`/notebook/${id}`)
}
</script>

<style scoped lang="scss">
.v2-note-folder__status {
  text-align: center;
  color: var(--wr-text-secondary, #666666);
  padding: 32px;
  font-size: 14px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 700;
  color: var(--wr-text, #333333);
  margin-bottom: 12px;

  &__count {
    font-size: 11px;
    font-weight: 600;
    background: rgba(0,0,0,0.06);
    padding: 0 8px;
    border-radius: 8px;
    line-height: 18px;
    color: var(--wr-text-secondary, #666666);
  }
}

.folder-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.folder-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 8px;
  background: var(--wr-index-bg, #eff6ff);
  border-radius: 10px;
  cursor: pointer;
  transition: transform 0.15s;

  &:hover {
    transform: translateY(-2px);
  }

  &__icon {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__name {
    font-size: 13px;
    font-weight: 600;
    color: var(--wr-text, #333333);
    text-align: center;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 100%;
  }

  &__count {
    font-size: 11px;
    color: var(--wr-text-secondary, #666666);
  }
}

.note-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.note-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--wr-border, #e8ecef);
  cursor: pointer;
  transition: background 0.15s;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--wr-index-bg, #eff6ff);
  }

  &__icon {
    width: 36px;
    height: 36px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    margin-top: 2px;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: 14px;
    font-weight: 600;
    color: var(--wr-text, #333333);
    line-height: 1.4;
    margin-bottom: 4px;
  }

  &__excerpt {
    font-size: 13px;
    color: var(--wr-text-secondary, #666666);
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;

  &__text {
    font-size: 14px;
    color: var(--wr-muted, #999999);
    margin-top: 12px;
  }
}
</style>
