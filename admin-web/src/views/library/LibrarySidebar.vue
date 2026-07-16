<template>
  <aside class="library-sidebar">
    <div class="library-sidebar__header">
      <el-button type="primary" class="library-sidebar__create-btn" @click="showNewFolderDialog">
        <el-icon><FolderAdd /></el-icon>
        <span>{{ t('library.newFolder') }}</span>
      </el-button>
    </div>

    <div class="library-sidebar__nav">
      <button
        v-for="item in quickNavItems"
        :key="item.key"
        type="button"
        class="library-sidebar__nav-item"
        :class="{ 'is-active': activeNav === item.key }"
        @click="onNavClick(item.key)"
      >
        <el-icon class="library-sidebar__nav-icon"><component :is="item.icon" /></el-icon>
        <span>{{ t(item.labelKey) }}</span>
      </button>
    </div>

    <div class="library-sidebar__divider" />

    <div class="library-sidebar__tree-wrap">
      <el-tree
        ref="treeRef"
        :data="tree"
        node-key="id"
        :props="{ children: 'children', label: 'name' }"
        :highlight-current="true"
        :expand-on-click-node="false"
        default-expand-all
        @node-click="onNodeClick"
      >
        <template #default="{ data }">
          <div class="library-tree-node" :class="{ 'is-active': store.currentFolder?.id === data.id }">
            <el-icon class="library-tree-node__icon"><FolderOpened /></el-icon>
            <span class="library-tree-node__label">{{ data.name }}</span>
          </div>
        </template>
      </el-tree>
    </div>

    <el-dialog
      v-model="newFolderDialogVisible"
      :title="t('library.newFolder')"
      width="360px"
      :close-on-click-modal="false"
    >
      <el-input
        v-model="newFolderName"
        :placeholder="t('library.folderNamePlaceholder')"
        @keyup.enter="onConfirmNewFolder"
      />
      <template #footer>
        <el-button @click="newFolderDialogVisible = false">{{ t('library.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="onConfirmNewFolder">
          {{ t('library.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { ElTree } from 'element-plus'
import { FolderAdd, FolderOpened, Collection, Clock, Star, Delete } from '@element-plus/icons-vue'
import { useLibraryStore } from '@/stores/library'
import { storeToRefs } from 'pinia'
import type { DocFolder } from '@/api/library/types'

const emit = defineEmits<{
  'folder-select': [folder: DocFolder]
  'navigate-to': [key: string]
}>()

const { t } = useI18n()
const store = useLibraryStore()
const { tree } = storeToRefs(store)

const treeRef = ref<InstanceType<typeof ElTree> | null>(null)
const activeNav = ref('all')
const newFolderDialogVisible = ref(false)
const newFolderName = ref('')
const submitting = ref(false)

const quickNavItems = [
  { key: 'all', icon: Collection, labelKey: 'library.allFiles' },
  { key: 'recent', icon: Clock, labelKey: 'library.recentDocuments' },
  { key: 'favorites', icon: Star, labelKey: 'library.favorites' },
  { key: 'trash', icon: Delete, labelKey: 'library.trash' },
]

function onNodeClick(data: DocFolder) {
  activeNav.value = ''
  emit('folder-select', data)
}

function onNavClick(key: string) {
  activeNav.value = key
  emit('navigate-to', key)
}

function showNewFolderDialog() {
  newFolderName.value = ''
  newFolderDialogVisible.value = true
}

async function onConfirmNewFolder() {
  const name = newFolderName.value.trim()
  if (!name) return
  submitting.value = true
  try {
    await store.createFolder(name, store.currentFolder?.id ?? null)
    newFolderDialogVisible.value = false
    ElMessage.success(t('library.createFolderSuccess'))
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.library-sidebar {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
  overflow: hidden;
}

.library-sidebar__header {
  flex-shrink: 0;
  padding: 16px 12px 12px;
}

.library-sidebar__create-btn {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.library-sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 8px;
  flex-shrink: 0;
}

.library-sidebar__nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  font-size: 14px;
  color: var(--el-text-color-primary);
  cursor: pointer;
  text-align: left;
  transition: background 0.15s;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &.is-active {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    font-weight: 600;
  }
}

.library-sidebar__nav-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.library-sidebar__divider {
  height: 1px;
  margin: 8px 12px;
  background: var(--el-border-color-lighter);
  flex-shrink: 0;
}

.library-sidebar__tree-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 0 8px 12px;

  :deep(.el-tree) {
    background: transparent;
  }

  :deep(.el-tree-node__content) {
    height: auto;
    padding: 2px 0;
    border-radius: 8px;
  }
}

.library-tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 6px 8px;
  border-radius: 8px;
  font-size: 13px;

  &.is-active {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
}

.library-tree-node__icon {
  flex-shrink: 0;
  font-size: 16px;
  color: var(--el-text-color-secondary);
}

.library-tree-node__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
