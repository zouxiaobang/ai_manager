<template>
  <div class="library-trash-view">
    <div class="library-trash-view__toolbar">
      <h3 class="library-trash-view__title">{{ t('library.trash') }}</h3>
      <el-button type="danger" :disabled="trashFiles.length === 0" @click="handleEmptyTrash">
        <el-icon><Delete /></el-icon>
        {{ t('library.emptyTrash') }}
      </el-button>
    </div>

    <el-table
      :data="trashFiles"
      v-loading="loading"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="name" :label="t('library.fileName')" min-width="200" />
      <el-table-column prop="extension" :label="t('library.fileType')" width="120" />
      <el-table-column prop="folderName" :label="t('library.originalLocation')" width="180" />
      <el-table-column prop="deletedAt" :label="t('library.deleteTime')" width="180" />
      <el-table-column prop="fileSize" :label="t('library.fileSize')" width="100">
        <template #default="{ row }">
          {{ formatSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('library.actions')" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="handleRestore(row.id)">
            {{ t('library.restore') }}
          </el-button>
          <el-button size="small" type="danger" @click="handlePurge(row.id)">
            {{ t('library.purge') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && trashFiles.length === 0" :description="t('library.trashEmpty')" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import type { DocTrashItem } from '@/api/library/types'
import { fetchTrashFiles, restoreFile, purgeFile, purgeAllTrash } from '@/api/library/file'

const { t } = useI18n()

const trashFiles = ref<DocTrashItem[]>([])
const loading = ref(false)

async function loadTrash() {
  loading.value = true
  try {
    trashFiles.value = await fetchTrashFiles()
  } catch {
    trashFiles.value = []
  } finally {
    loading.value = false
  }
}

async function handleRestore(id: number) {
  try {
    await restoreFile(id)
    ElMessage.success(t('library.restoreSuccess'))
    await loadTrash()
  } catch {
    // silent
  }
}

async function handlePurge(id: number) {
  try {
    await ElMessageBox.confirm(t('library.purgeConfirm'), t('common.confirmTitle'), {
      type: 'warning',
    })
    await purgeFile(id)
    ElMessage.success(t('library.purgeSuccess'))
    await loadTrash()
  } catch {
    // silent
  }
}

async function handleEmptyTrash() {
  try {
    await ElMessageBox.confirm(t('library.emptyTrashConfirm'), t('common.confirmTitle'), {
      type: 'warning',
    })
    await purgeAllTrash()
    ElMessage.success(t('library.emptyTrashSuccess'))
    await loadTrash()
  } catch {
    // silent
  }
}

function formatSize(bytes?: number) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(1)} ${units[i]}`
}

onMounted(loadTrash)
</script>

<style scoped lang="scss">
.library-trash-view {
  padding: 20px;
}

.library-trash-view__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.library-trash-view__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
</style>
