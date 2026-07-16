<template>
  <div class="library-view">
    <LibrarySidebar
      @folder-select="onFolderSelect"
      @navigate-to="onNavigateTo"
    />
    <div class="library-view__main">
      <LibraryToolbar
        :folder-name="currentFolderName"
        :file-count="store.totalFiles"
        @search="onSearch"
        @upload-click="showUploader = true"
        @delete-selected="onBatchDelete"
        @move-selected="onBatchMove"
        @view-mode-change="onViewModeChange"
        @sort-change="onSortChange"
      />
      <LibraryContent
        @file-select="onFileSelect"
        @file-preview="onFilePreview"
        @folder-select="onFolderSelect"
        @contextmenu="onFileContextMenu"
      />
    </div>

    <LibraryDetailPanel
      v-if="selectedFile"
      :file="selectedFile"
      @close="selectedFile = null"
      @pin="onTogglePin"
      @tag="onTagManage"
      @kb="onKbToggle"
    />

    <LibraryUploader
      v-model:visible="showUploader"
      :folder-id="store.currentFolder?.id ?? null"
      @uploaded="onUploaded"
    />

    <LibraryPreview
      v-if="previewFile"
      :file="previewFile"
      @close="previewFile = null"
    />

    <LibraryContextMenu
      v-if="contextMenu"
      :x="contextMenu.x"
      :y="contextMenu.y"
      :file="contextMenu.file"
      @close="contextMenu = null"
      @action="onContextAction"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useLibraryStore } from '@/stores/library'
import LibrarySidebar from './LibrarySidebar.vue'
import LibraryToolbar from './LibraryToolbar.vue'
import LibraryContent from './LibraryContent.vue'
import LibraryDetailPanel from './LibraryDetailPanel.vue'
import LibraryUploader from './LibraryUploader.vue'
import LibraryPreview from './LibraryPreview.vue'
import LibraryContextMenu from './LibraryContextMenu.vue'
import type { DocFile, DocFolder, SortField, SortOrder } from '@/api/library/types'

const { t } = useI18n()
const store = useLibraryStore()

const selectedFile = ref<DocFile | null>(null)
const previewFile = ref<DocFile | null>(null)
const showUploader = ref(false)
const contextMenu = ref<{ x: number; y: number; file: DocFile } | null>(null)

const currentFolderName = computed(() => store.currentFolder?.name ?? t('library.allFiles'))

onMounted(async () => {
  await Promise.all([
    store.loadTree(),
    store.loadFiles({ page: 1, pageSize: 20 }),
    store.loadTags(),
  ])
})

async function onFolderSelect(folder: DocFolder) {
  contextMenu.value = null
  store.setCurrentFolder(folder)
  await store.loadFiles({ page: 1, pageSize: store.pageSize, folderId: folder.id })
}

async function onNavigateTo(key: string) {
  contextMenu.value = null
  store.setCurrentFolder(null)
  if (key === 'trash') {
    await store.loadTrashFiles()
  } else if (key === 'favorites') {
    await store.loadFiles({ page: 1, pageSize: store.pageSize, favorite: true })
  } else if (key === 'recent') {
    await store.loadFiles({ page: 1, pageSize: store.pageSize, sortField: 'updateTime', sortOrder: 'desc' })
  } else {
    await store.loadFiles({ page: 1, pageSize: store.pageSize })
  }
}

async function onSearch(keyword: string) {
  contextMenu.value = null
  if (keyword.trim()) {
    await store.searchFiles({ keyword: keyword.trim(), page: 1, size: store.pageSize })
  } else {
    await store.loadFiles({ page: 1, pageSize: store.pageSize })
  }
}

function onFileSelect(file: DocFile) {
  selectedFile.value = file
}

function onFilePreview(file: DocFile) {
  previewFile.value = file
}

function onFileContextMenu(event: MouseEvent, file: DocFile) {
  contextMenu.value = { x: event.clientX, y: event.clientY, file }
}

async function onBatchDelete() {
  await store.batchDeleteFiles()
  ElMessage.success(t('library.batchDeleteSuccess'))
  await store.loadFiles({ page: 1, pageSize: store.pageSize })
}

async function onBatchMove() {
  await store.batchMoveFiles()
  ElMessage.success(t('library.batchMoveSuccess'))
}

function onViewModeChange(mode: 'grid' | 'list') {
  store.viewMode = mode
}

function onSortChange(field: SortField, order: SortOrder) {
  store.sortField = field
  store.sortOrder = order
  store.loadFiles({ page: 1, pageSize: store.pageSize, sortField: field, sortOrder: order })
}

async function onTogglePin() {
  if (!selectedFile.value) return
  await store.togglePin(selectedFile.value.id)
}

async function onTagManage() {
  if (!selectedFile.value) return
  await store.manageTags(selectedFile.value.id)
}

async function onKbToggle() {
  if (!selectedFile.value) return
  await store.toggleKbStatus(selectedFile.value.id)
}

async function onUploaded() {
  showUploader.value = false
  await store.loadFiles({ page: 1, pageSize: store.pageSize })
}

async function onContextAction(action: string) {
  const file = contextMenu.value?.file
  contextMenu.value = null
  if (!file) return
  switch (action) {
    case 'preview':
      onFilePreview(file)
      break
    case 'download':
      await store.downloadFile(file.id)
      break
    case 'rename':
      await store.renameFile(file.id)
      break
    case 'move':
      await store.moveFile(file.id)
      break
    case 'delete':
      await store.deleteFile(file.id)
      ElMessage.success(t('library.deleteSuccess'))
      await store.loadFiles({ page: 1, pageSize: store.pageSize })
      break
    case 'copy':
      await store.copyFileLink(file.id)
      break
  }
}
</script>

<style scoped lang="scss">
.library-view {
  display: flex;
  height: 100%;
  min-height: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  overflow: hidden;
  background: var(--el-bg-color);
}

.library-view__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
</style>
