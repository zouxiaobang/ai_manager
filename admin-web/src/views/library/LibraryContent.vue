<template>
  <div class="library-content">
    <div v-if="!files.length && !displayFolders.length" class="library-content__empty">
      <el-empty :description="t('library.empty')" :image-size="80" />
    </div>

    <template v-else>
      <div v-if="displayFolders.length" class="library-content__folder-section">
        <h4 class="library-content__section-title">{{ t('library.folders') }}</h4>
        <div class="library-content__folder-grid">
          <button
            v-for="folder in displayFolders"
            :key="folder.id"
            type="button"
            class="library-folder-card"
            @click="emit('folder-select', folder)"
          >
            <el-icon class="library-folder-card__icon"><FolderOpened /></el-icon>
            <span class="library-folder-card__name">{{ folder.name }}</span>
          </button>
        </div>
      </div>

      <div v-if="files.length" class="library-content__file-section">
        <h4 v-if="displayFolders.length" class="library-content__section-title">
          {{ t('library.files') }}
        </h4>
        <div
          v-if="viewMode === 'grid'"
          class="library-content__grid"
        >
          <LibraryFileCard
            v-for="file in files"
            :key="file.id"
            :file="file"
            :is-selected="isFileSelected(file.id)"
            @select="onFileSelect(file)"
            @preview="emit('file-preview', file)"
            @toggle-favorite="onToggleFavorite(file)"
            @contextmenu="onContextMenu($event, file)"
          />
        </div>
        <div
          v-else
          class="library-content__list"
        >
          <LibraryFileRow
            v-for="file in files"
            :key="file.id"
            :file="file"
            :selected="isFileSelected(file.id)"
            @select="onFileSelect(file)"
            @preview="emit('file-preview', file)"
            @toggle-favorite="onToggleFavorite(file)"
            @contextmenu="onContextMenu($event, file)"
          />
        </div>
      </div>
    </template>

    <div v-if="totalPages > 1" class="library-content__pagination">
      <el-pagination
        v-model:current-page="curPageModel"
        :page-size="pageSize"
        :total="totalFiles"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { FolderOpened } from '@element-plus/icons-vue'
import { useLibraryStore } from '@/stores/library'
import { storeToRefs } from 'pinia'
import LibraryFileCard from './LibraryFileCard.vue'
import LibraryFileRow from './LibraryFileRow.vue'
import type { DocFile, DocFolder } from '@/api/library/types'

const emit = defineEmits<{
  'file-select': [file: DocFile]
  'file-preview': [file: DocFile]
  'folder-select': [folder: DocFolder]
  'contextmenu': [event: MouseEvent, file: DocFile]
}>()

const { t } = useI18n()
const store = useLibraryStore()
const { files, tree, viewMode, pageSize, totalFiles, selectedFiles, currentPage } = storeToRefs(store)

const displayFolders = computed(() => tree.value.filter(f => !f.parentId))

const curPageModel = computed({
  get: () => currentPage.value,
  set: (val: number) => { store.currentPage = val },
})

const totalPages = computed(() => Math.ceil(totalFiles.value / pageSize.value) || 1)

function isFileSelected(id: number): boolean {
  return selectedFiles.value.has(id)
}

function onFileSelect(file: DocFile) {
  emit('file-select', file)
}

function onContextMenu(event: MouseEvent, file: DocFile) {
  emit('contextmenu', event, file)
}

async function onToggleFavorite(file: DocFile) {
  await store.togglePin(file.id)
}

function onPageChange(pageNum: number) {
  store.loadFiles({ page: pageNum, pageSize: pageSize.value })
}

function onSizeChange(size: number) {
  store.loadFiles({ page: 1, pageSize: size })
}
</script>

<style scoped lang="scss">
.library-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.library-content__empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.library-content__folder-section {
  margin-bottom: 24px;
}

.library-content__file-section {
  margin-bottom: 24px;
}

.library-content__section-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.library-content__folder-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.library-folder-card {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 160px;
  max-width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
  cursor: pointer;
  text-align: left;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    box-shadow: 0 2px 8px rgb(64 158 255 / 12%);
  }
}

.library-folder-card__icon {
  flex-shrink: 0;
  font-size: 18px;
  color: var(--el-color-primary-light-3);
}

.library-folder-card__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.library-content__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
}

.library-content__list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.library-content__pagination {
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  padding: 16px 0 0;
}
</style>
