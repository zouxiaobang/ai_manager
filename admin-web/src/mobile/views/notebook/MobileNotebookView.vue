<template>
  <div v-loading="loading" class="mobile-notebook-d">
    <!-- Drawer overlay -->
    <div
      v-show="drawerOpen"
      class="notebook-drawer-overlay"
      @click="closeDrawer"
    />

    <!-- Left drawer -->
    <div class="notebook-drawer" :class="{ 'notebook-drawer--open': drawerOpen }">
      <div class="notebook-drawer__header">
        <span class="notebook-drawer__title">
          <svg width="18" height="18" class="drawer-title-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
          {{ t('notebook.stats.folders') }}
        </span>
        <button class="notebook-drawer__close" @click="closeDrawer">✕</button>
      </div>

      <div class="notebook-drawer__tree">
        <!-- All Notes root -->
        <div class="drawer-node drawer-node--active" @click="closeDrawer; currentView = 'all'">
          <svg width="16" height="16" class="drawer-node__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="8" y="2" width="8" height="4" rx="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg>
          <span class="drawer-node__label">{{ t('notebook.tabs.all') }}</span>
          <span class="drawer-node__count">{{ totalNoteCount }}</span>
        </div>

        <!-- Recursive folder/note tree -->
        <DrawerTree
          :nodes="rootFolders"
          :expanded-keys="expandedDrawerFolders"
          @toggle="toggleDrawerFolder"
          @navigate="navigateToFolder"
          @open-note="openNote"
          @close-drawer="closeDrawer"
        />

        <!-- Trash link -->
        <div class="drawer-node drawer-node--trash" @click="closeDrawer">
          <svg width="16" height="16" class="drawer-node__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
          <span class="drawer-node__label">{{ t('notebook.tabs.trash') }}</span>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="notebook-main">
      <!-- Top Navigation Bar -->
      <div class="notebook-main__nav">
        <button class="notebook-main__hamburger" @click="openDrawer">
          <span class="hamburger-line" />
          <span class="hamburger-line" />
          <span class="hamburger-line" />
        </button>

        <div class="notebook-main__title-area">
          <h2 class="notebook-main__title">{{ t('portal.menu.notebook') }}</h2>
          <span v-if="totalNoteCount > 0" class="notebook-main__subtitle">{{ totalNoteCount }} {{ t('notebook.stats.notes') }}</span>
        </div>

        <button class="notebook-main__search-btn" @click="onSearchClick">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
        </button>
      </div>

      <!-- Current Path -->
      <div class="notebook-main__path">
        <svg width="12" height="12" class="path-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
        <span class="notebook-main__path-text">{{ t('notebook.tabs.all') }}</span>
      </div>

      <!-- Content Cards Area -->
      <div class="notebook-main__content">
        <!-- Pinned Section -->
        <div v-if="pinnedNotes.length" class="content-section">
          <div class="content-section__label content-section__label--pinned">
            <svg width="14" height="14" class="section-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="17" x2="12" y2="22"/><path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24Z"/></svg>
            <span>{{ t('mobile.notebook.pinned') }}</span>
            <span class="content-section__count">{{ pinnedNotes.length }}</span>
          </div>

          <div class="content-card-list">
            <SchemeADoodleFrame
              v-for="item in pinnedNotes"
              :key="item.id"
              :seed="item.id"
              color="#fbbf24"
              sketch
              :shadow="false"
              class="content-card content-card--pinned"
              @click="openNote(item.id)"
            >
              <div class="content-card__body">
                <div class="content-card__title-row">
                  <svg width="14" height="14" class="content-card__pin-icon" viewBox="0 0 24 24" fill="#9b0000" stroke="#9b0000" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="17" x2="12" y2="22"/><path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24Z"/></svg>
                  <span class="content-card__title">{{ item.title }}</span>
                </div>
                <div class="content-card__meta">
                  <svg width="12" height="12" class="meta-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
                  {{ item.folderPath || '未分类' }}
                </div>
              </div>
            </SchemeADoodleFrame>
          </div>
        </div>

        <!-- Folder Section (only FOLDER nodes) -->
        <div v-if="rootFolders.filter(n => n.nodeType === 'FOLDER').length" class="content-section">
          <div class="content-section__label content-section__label--folder">
            <svg width="16" height="16" class="section-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
            <span>{{ t('notebook.stats.folders') }}</span>
            <span class="content-section__count">{{ rootFolders.filter(n => n.nodeType === 'FOLDER').length }}</span>
          </div>

          <div class="content-card-list">
            <SchemeADoodleFrame
              v-for="folder in rootFolders.filter(n => n.nodeType === 'FOLDER')"
              :key="folder.nodeKey"
              :seed="getSeedFromKey(folder.nodeKey)"
              color="#f97316"
              sketch
              :shadow="false"
              class="content-card content-card--folder"
              @click="openFolder(folder)"
            >
              <div class="content-card__body">
                <div class="content-card__row">
                  <svg width="22" height="22" class="content-card__folder-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
                  <div class="content-card__folder-info">
                    <span class="content-card__title">{{ folder.name }}</span>
                    <span class="content-card__meta">{{ getFolderSummary(folder) }}</span>
                  </div>
                  <span class="content-card__arrow">›</span>
                </div>
              </div>
            </SchemeADoodleFrame>
          </div>
        </div>

        <!-- Empty State -->
        <div v-if="rootFolders.filter(n => n.nodeType === 'FOLDER').length === 0 && pinnedNotes.length === 0" class="content-empty">
          <svg width="48" height="48" class="content-empty__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          <div class="content-empty__text">{{ t('notebook.emptyTree') }}</div>
        </div>

        <div style="height: 80px;" />
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { fetchNotebookTree } from '@/api/notebook'
import type { NbTreeNode } from '@/api/notebook'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import DrawerTree from '@/mobile/views/notebook/components/DrawerTree.vue'

const router = useRouter()
const { t } = useI18n()

const loading = ref(false)
const drawerOpen = ref(false)
const rootFolders = ref<NbTreeNode[]>([])
const pinnedNotes = ref<Array<{ id: number; title: string; folderPath: string; isPinned: boolean }>>([])
const expandedDrawerFolders = ref(new Set<string>())
const currentView = ref<'all' | 'folder'>('all')

function countAllNotes(nodes: NbTreeNode[]): number {
  let count = 0
  for (const n of nodes) {
    if (n.nodeType === 'NOTE') count++
    if (n.children?.length) count += countAllNotes(n.children)
  }
  return count
}

const totalNoteCount = ref(0)

function getSeedFromKey(key: string): number {
  let hash = 0
  for (let i = 0; i < key.length; i++) {
    hash = ((hash << 5) - hash) + key.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash)
}

function countChildNotes(node: NbTreeNode): number {
  let count = 0
  if (node.nodeType === 'NOTE') count = 1
  if (node.children?.length) {
    count += node.children.reduce((acc, child) => acc + countChildNotes(child), 0)
  }
  return count
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

function getFolderSummary(node: NbTreeNode): string {
  const notes = countChildNotes(node)
  const folders = countChildFolders(node)
  const parts: string[] = []
  if (notes > 0) parts.push(`${notes} ${t('notebook.stats.notes')}`)
  if (folders > 0) parts.push(`${folders} ${t('notebook.stats.folders')}`)
  return parts.join(' · ') || t('notebook.folderEmpty')
}

function openDrawer() { drawerOpen.value = true }
function closeDrawer() { drawerOpen.value = false }

function toggleDrawerFolder(nodeKey: string) {
  const s = new Set(expandedDrawerFolders.value)
  if (s.has(nodeKey)) s.delete(nodeKey)
  else s.add(nodeKey)
  expandedDrawerFolders.value = s
}

function navigateToFolder(node: NbTreeNode) {
  router.push(`/notebook/folder/${node.nodeKey}`)
}

function onSearchClick() {
  router.push('/notebook/search')
}

function openFolder(folder: NbTreeNode) {
  router.push(`/notebook/folder/${folder.nodeKey}`)
}

function openNote(id: number) {
  router.push(`/notebook/${id}`)
}

async function loadNotes() {
  loading.value = true
  try {
    const tree = await fetchNotebookTree()
    rootFolders.value = tree
    totalNoteCount.value = countAllNotes(tree)

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
            ? folderPath ? `${folderPath} / ${node.name}` : node.name
            : folderPath
        for (const child of node.children) collectPinned(child, nextPath)
      }
    }
    for (const node of tree) collectPinned(node, '')
    pinnedNotes.value = pinned
  } finally {
    loading.value = false
  }
}

onMounted(() => { void loadNotes() })
</script>

<style scoped lang="scss">
// =========================================
// Colors
// =========================================
$blue: #2563eb;
$orange: #f97316;
$yellow: #fbbf24;
$bg: #f0ede8;
$surface: #faf8f5;
$text-primary: #1e293b;
$text-secondary: #64748b;
$text-muted: #94a3b8;
$border: #e2e8f0;

// =========================================
// Main Container
// =========================================
.mobile-notebook-d {
  position: relative;
  min-height: calc(100vh - 120px);
  min-height: calc(100dvh - 120px);
  background: $bg;
  display: flex;
  overflow: hidden;
}

// =========================================
// Drawer Overlay
// =========================================
.notebook-drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  z-index: 50;
  animation: fadeIn 0.2s ease;
}

// =========================================
// Drawer
// =========================================
.notebook-drawer {
  position: fixed;
  top: 0;
  left: 0;
  width: 280px;
  height: 100%;
  height: 100dvh;
  background: #f8f6f3;
  z-index: 55;
  transform: translateX(-100%);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.1);

  &--open {
    transform: translateX(0);
  }

  &__header {
    padding: 16px;
    border-bottom: 1px solid $border;
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-shrink: 0;
  }

  &__title {
    font-family: 'ZCOOL KuaiLe', cursive;
    font-size: 18px;
    color: $text-primary;
  }

  &__close {
    width: 28px;
    height: 28px;
    border-radius: 8px;
    border: none;
    background: transparent;
    cursor: pointer;
    font-size: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: $text-muted;

    &:hover {
      background: $border;
    }
  }

  &__tree {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
  }
}

// Drawer Nodes
.drawer-node {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
  font-size: 14px;
  color: $text-primary;
  user-select: none;

  &:hover {
    background: rgba($blue, 0.06);
  }

  &:active {
    background: rgba($blue, 0.1);
  }

  &--active {
    background: #dbeafe;
    color: $blue;
    font-weight: 600;
  }

  &--folder {
    font-weight: 500;

    &.drawer-node--expanded {
      background: rgba($orange, 0.06);
    }
  }

  &--subfolder {
    padding-left: 36px;
  }

  &--note {
    padding-left: 36px;
  }

  &--trash {
    margin-top: 8px;
    border-top: 1px solid $border;
    padding-top: 12px;
    border-radius: 0;
    color: $text-muted;
  }

  &__icon {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
    display: block;
  }

  &__arrow {
    font-size: 10px;
    color: $text-muted;
    flex-shrink: 0;
    width: 16px;
    text-align: center;
    transition: transform 0.2s;
  }

  &__label {
    flex: 1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__count {
    font-size: 11px;
    color: $text-muted;
    background: $border;
    padding: 1px 8px;
    border-radius: 8px;
    flex-shrink: 0;
  }
}

// =========================================
// Main Content
// =========================================
.notebook-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;

  &__nav {
    padding: 8px 16px 4px;
    display: flex;
    align-items: center;
    gap: 12px;
    flex-shrink: 0;
    background: white;
  }

  &__hamburger {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    border: 2px solid $border;
    background: white;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    cursor: pointer;
    transition: all 0.15s;
    flex-shrink: 0;

    &:hover {
      border-color: $blue;
      background: #dbeafe;
    }

    &:active {
      transform: scale(0.95);
    }
  }

  &__title-area {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-family: 'ZCOOL KuaiLe', cursive;
    font-size: 20px;
    color: $text-primary;
    margin: 0;
    line-height: 1.2;
  }

  &__subtitle {
    font-size: 11px;
    color: $text-muted;
    display: block;
  }

  &__search-btn {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    border: 2px solid $border;
    background: white;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: $text-muted;
    transition: all 0.15s;
    flex-shrink: 0;

    &:hover {
      border-color: $blue;
      color: $blue;
    }

    &:active {
      transform: scale(0.95);
    }
  }

  &__path {
    padding: 4px 16px 8px;
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: $text-muted;
    flex-shrink: 0;
    background: white;
  }

  &__path-text {
    color: $text-secondary;
    font-weight: 500;
  }

  &__content {
    flex: 1;
    overflow-y: auto;
    padding: 8px 12px 0;
    scroll-behavior: smooth;
    -webkit-overflow-scrolling: touch;
    background: white;
  }
}

// =========================================
// Content Sections
// =========================================
.content-section {
  margin-bottom: 16px;

  &__label {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 700;
    padding: 0 4px 8px;

    &--pinned { color: #d97706; }
    &--folder { color: $orange; }
    &--note { color: $blue; }
  }

  &__count {
    font-size: 11px;
    font-weight: 600;
    background: rgba(0,0,0,0.06);
    padding: 0 8px;
    border-radius: 8px;
    line-height: 18px;
  }
}

.content-card-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

// =========================================
// Content Cards (with left stripe + doodle frame)
// =========================================
.content-card {
  cursor: pointer;
  transition: all 0.2s ease;
  overflow: hidden;

  &:hover {
    transform: scale(0.99);
  }

  &:active {
    transform: scale(0.98);
  }

  &--pinned {
    background: #fffbeb;
  }

  &--folder {
    background: #fff;
  }

  &--note {
    background: white;
  }

  &__left-stripe {
    position: absolute;
    left: 0;
    top: 4px;
    bottom: 4px;
    width: 4px;
    border-radius: 2px;
    z-index: 3;
    opacity: 0.8;
  }

  &__body {
    padding: 12px 20px 12px 24px;
    position: relative;
    z-index: 1;
  }

  &__row {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  &__title-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 4px;
  }

  &__pin-icon {
    width: 14px;
    height: 14px;
    flex-shrink: 0;
    display: block;
  }

  &__note-icon {
    width: 14px;
    height: 14px;
    flex-shrink: 0;
    display: block;
  }

  &__folder-icon {
    width: 22px;
    height: 22px;
    flex-shrink: 0;
    display: block;
  }

  &__title {
    font-family: 'ZCOOL KuaiLe', cursive;
    font-size: 15px;
    color: $text-primary;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__folder-info {
    flex: 1;
    min-width: 0;
  }

  &__meta {
    font-size: 11px;
    color: $text-muted;
    margin-top: 2px;
    display: flex;
    align-items: center;
    gap: 2px;
  }

  &__arrow {
    font-size: 18px;
    color: $orange;
    font-weight: bold;
    flex-shrink: 0;
  }
}

// =========================================
// Empty State
// =========================================
.content-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;

  &__icon {
    width: 48px;
    height: 48px;
    margin-bottom: 12px;
    color: $text-muted;
  }

  &__text {
    font-family: 'ZCOOL KuaiLe', cursive;
    font-size: 16px;
    color: $text-muted;
  }
}

// =========================================
// Inline icon helpers
// =========================================
.section-icon {
  width: 16px;
  height: 16px;
  display: block;
  flex-shrink: 0;
}

.meta-icon {
  width: 12px;
  height: 12px;
  display: inline-block;
  vertical-align: middle;
  flex-shrink: 0;
  margin-right: 2px;
}

.path-icon {
  width: 12px;
  height: 12px;
  display: block;
  flex-shrink: 0;
}

.drawer-title-icon {
  display: inline-block;
  vertical-align: middle;
  margin-right: 4px;
}

// =========================================
// FAB
// =========================================
.notebook-fab {
  position: fixed;
  bottom: 80px;
  right: 20px;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: $blue;
  color: white;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba($blue, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  z-index: 40;

  &:hover {
    transform: scale(1.05);
    box-shadow: 0 6px 20px rgba($blue, 0.45);
  }

  &:active {
    transform: scale(0.95);
  }
}

// =========================================
// Hamburger lines
// =========================================
.hamburger-line {
  display: block;
  width: 16px;
  height: 2px;
  background: $text-primary;
  border-radius: 1px;
  transition: all 0.2s;
}

// =========================================
// Animations
// =========================================
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

// =========================================
// Loading overlay
// =========================================
:deep(.el-loading-mask) {
  background: rgba($bg, 0.6);
}
</style>
