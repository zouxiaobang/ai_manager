<template>
  <div class="note-tree">
    <div
      v-for="node in tree"
      :key="node.nodeKey"
      class="note-tree__item"
    >
      <template v-if="node.nodeType === 'FOLDER'">
        <SchemeADoodleFrame color="#f97316" class="note-tree__folder-card" @click="toggleFolder(node.nodeKey)">
          <div class="note-tree__folder-body">
            <div class="note-tree__folder-icon">📂</div>
            <div class="note-tree__folder-info">
              <div class="note-tree__folder-name">{{ node.name }}</div>
              <div class="note-tree__folder-meta">
                {{ countNotesInFolder(node) }} 笔记 · {{ countSubfolders(node) }} 子文件夹
              </div>
            </div>
            <div class="note-tree__folder-arrow">{{ expandedFolders.includes(node.nodeKey) ? '▼' : '▶' }}</div>
          </div>
        </SchemeADoodleFrame>

        <div
          v-if="expandedFolders.includes(node.nodeKey) && node.children?.length"
          class="note-tree__children"
        >
          <NoteTree
            :tree="node.children"
            :expanded-folders="expandedFolders"
            @toggle="handleToggle"
            @open-note="handleOpenNote"
          />
        </div>
      </template>

      <template v-else-if="node.nodeType === 'NOTE' && node.noteId">
        <SchemeADoodleFrame color="#2563eb" class="note-tree__note-card" @click="openNote(node.noteId)">
          <div class="note-tree__note-body">
            <div class="note-tree__note-title">📄 {{ node.name }}</div>
          </div>
        </SchemeADoodleFrame>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import SchemeADoodleFrame from '@/mobile/home/themes/scheme-a/SchemeADoodleFrame.vue'
import { countNotesInFolder, countSubfolders, type TreeNode } from '@/mobile/notebook/utils/noteFolderTree'

defineProps<{
  tree: TreeNode[]
  expandedFolders: string[]
}>()

const emit = defineEmits<{
  toggle: [nodeKey: string]
  openNote: [noteId: number]
}>()

function toggleFolder(nodeKey: string) {
  emit('toggle', nodeKey)
}

function openNote(noteId: number) {
  emit('openNote', noteId)
}

function handleToggle(nodeKey: string) {
  emit('toggle', nodeKey)
}

function handleOpenNote(noteId: number) {
  emit('openNote', noteId)
}
</script>

<style scoped lang="scss">
.note-tree {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.note-tree__item {
  display: flex;
  flex-direction: column;
}

.note-tree__folder-card {
  background: #fff7ed;
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }
}

.note-tree__folder-body {
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.note-tree__folder-icon {
  font-size: 24px;
}

.note-tree__folder-info {
  flex: 1;
}

.note-tree__folder-name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
  margin: 0 0 4px;
}

.note-tree__folder-meta {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

.note-tree__folder-arrow {
  font-size: 12px;
  color: #f97316;
  font-weight: bold;
}

.note-tree__children {
  padding-left: 16px;
  margin-top: 4px;
}

.note-tree__note-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }
}

.note-tree__note-body {
  padding: 14px;
}

.note-tree__note-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
  margin: 0;
}
</style>
