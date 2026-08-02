<template>
  <MobilePage>
    <span v-if="totalNoteCount > 0" class="notebook-header__subtitle">{{ totalNoteCount }} {{ t('notebook.stats.notes') }}</span>

    <MobileCard v-if="folders.length > 0">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#f97316" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
        <span>{{ t('notebook.stats.folders') }}</span>
        <span class="section-header__count">{{ folders.length }}</span>
      </div>
      <div class="folder-grid">
        <div
          v-for="folder in folders"
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
    </MobileCard>

    <MobileCard v-if="pinnedNotes.length > 0">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#d97706" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="17" x2="12" y2="22"/><path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24Z"/></svg>
        <span>{{ t('notebook.pinned') }}</span>
        <span class="section-header__count">{{ pinnedNotes.length }}</span>
      </div>
      <div class="note-cards">
        <div
          v-for="note in pinnedNotes"
          :key="note.id"
          class="note-card note-card--pinned"
          @click="openNote(note.id)"
        >
          <div class="note-card__pin">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="#9b0000" stroke="#9b0000" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="17" x2="12" y2="22"/><path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24Z"/></svg>
          </div>
          <div class="note-card__title">{{ note.title }}</div>
          <div class="note-card__excerpt">{{ note.contentExcerpt || t('notebook.emptyContent') }}</div>
          <div class="note-card__footer">{{ note.folderName || t('notebook.uncategorized') }} · {{ formatTime(note.updateTime) }}</div>
        </div>
      </div>
    </MobileCard>

    <MobileCard v-if="recentNotes.length > 0">
      <div class="section-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--wr-stat-blue, #2563eb)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/></svg>
        <span>{{ t('notebook.recent') }}</span>
      </div>
      <div class="note-list">
        <div
          v-for="note in recentNotes"
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
          <div class="note-item__meta">{{ formatTime(note.updateTime) }}</div>
        </div>
      </div>
    </MobileCard>

    <div v-if="folders.length === 0 && pinnedNotes.length === 0 && recentNotes.length === 0" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--wr-muted, #999999)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
      <div class="empty-state__text">{{ t('notebook.emptyTree') }}</div>
    </div>

  </MobilePage>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import MobilePage from '@/mobile/components/MobilePage.vue'
import MobileCard from '@/mobile/components/MobileCard.vue'
import {
  fetchNotebookTree,
  fetchRecentNotes,
  type NbNoteDetail,
  type NbTreeNode
} from '@/api/notebook'

const router = useRouter()
const { t } = useI18n()

const loading = ref(false)
const rootFolders = ref<NbTreeNode[]>([])
const folders = ref<NbTreeNode[]>([])
const pinnedNotes = ref<Array<{ id: number; title: string; contentExcerpt?: string; folderName?: string; updateTime?: string }>>([])
const recentNotes = ref<NbNoteDetail[]>([])
const totalNoteCount = ref(0)

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

function countAllNotes(nodes: NbTreeNode[]): number {
  let count = 0
  for (const n of nodes) {
    if (n.nodeType === 'NOTE') count++
    if (n.children?.length) count += countAllNotes(n.children)
  }
  return count
}

function countFolderNotes(node: NbTreeNode): number {
  let count = 0
  if (node.nodeType === 'NOTE') count = 1
  if (node.children?.length) {
    count += node.children.reduce((acc, child) => acc + countFolderNotes(child), 0)
  }
  return count
}

function formatTime(time?: string): string {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) {
    return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  } else if (days === 1) {
    return t('notebook.yesterday')
  } else if (days < 7) {
    return `${days} ${t('notebook.daysAgo')}`
  } else {
    return `${date.getMonth() + 1}/${date.getDate()}`
  }
}

async function loadData() {
  loading.value = true
  try {
    const [tree, recent] = await Promise.all([
      fetchNotebookTree(),
      fetchRecentNotes(10),
    ])
    
    rootFolders.value = tree
    totalNoteCount.value = countAllNotes(tree)
    
    folders.value = tree.filter(n => n.nodeType === 'FOLDER')
    
    const pinned: typeof pinnedNotes.value = []
    function collectPinned(node: NbTreeNode, folderPath: string) {
      if (node.nodeType === 'NOTE' && node.noteId && node.isPinned === 1) {
        pinned.push({
          id: node.noteId,
          title: node.name,
          contentExcerpt: node.contentExcerpt,
          folderName: folderPath || undefined,
          updateTime: node.noteId?.toString(),
        })
      }
      if (node.children?.length) {
        const nextPath = node.nodeType === 'FOLDER' 
          ? (folderPath ? `${folderPath} / ${node.name}` : node.name)
          : folderPath
        for (const child of node.children) collectPinned(child, nextPath)
      }
    }
    for (const node of tree) collectPinned(node, '')
    pinnedNotes.value = pinned
    
    const pinnedIds = new Set(pinned.map(n => n.id))
    recentNotes.value = recent.filter(n => !pinnedIds.has(n.id))
  } finally {
    loading.value = false
  }
}

function openFolder(folder: NbTreeNode) {
  router.push(`/notebook/folder/${folder.nodeKey}`)
}

function openNote(id: number) {
  router.push(`/notebook/${id}`)
}

onMounted(() => {
  void loadData()
})
</script>

<style scoped lang="scss">
.notebook-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px;
  margin-bottom: 4px;

  &__title-area {
    flex: 1;
    min-width: 0;
  }

  &__title {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    color: var(--wr-text, #333333);
    line-height: 1.3;
  }

  &__subtitle {
    font-size: 12px;
    color: var(--wr-text-secondary, #666666);
  }

  &__search-btn {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    border: 1.5px solid var(--wr-border, #e8ecef);
    background: var(--wr-card, #ffffff);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: var(--wr-text-secondary, #666666);
    transition: all 0.15s;
    flex-shrink: 0;

    &:hover {
      border-color: var(--wr-stat-blue, #2563eb);
      color: var(--wr-stat-blue, #2563eb);
    }
  }
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
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
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

.note-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.note-card {
  padding: 14px;
  background: var(--wr-card, #ffffff);
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 10px;
  cursor: pointer;
  transition: box-shadow 0.15s;

  &:hover {
    box-shadow: var(--wr-shadow, 0 4px 12px rgb(0 0 0 / 5%));
  }

  &--pinned {
    background: #fffbeb;
    border-color: #fbbf24;
  }

  &__pin {
    margin-bottom: 6px;
  }

  &__title {
    font-size: 14px;
    font-weight: 600;
    color: var(--wr-text, #333333);
    margin-bottom: 4px;
  }

  &__excerpt {
    font-size: 13px;
    color: var(--wr-text-secondary, #666666);
    line-height: 1.5;
    margin-bottom: 8px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__footer {
    font-size: 11px;
    color: var(--wr-muted, #999999);
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

  &__meta {
    font-size: 11px;
    color: var(--wr-muted, #999999);
    flex-shrink: 0;
    margin-top: 2px;
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