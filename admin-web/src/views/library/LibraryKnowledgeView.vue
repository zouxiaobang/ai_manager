<template>
  <div class="library-knowledge-view">
    <div class="library-knowledge-view__header">
      <h3 class="library-knowledge-view__title">{{ t('library.knowledgeBase') }}</h3>
    </div>

    <div class="library-knowledge-view__stats">
      <el-card class="library-knowledge-view__stat-card">
        <div class="library-knowledge-view__stat-value">{{ stats.totalFiles }}</div>
        <div class="library-knowledge-view__stat-label">{{ t('library.kbTotalDocs') }}</div>
      </el-card>
      <el-card class="library-knowledge-view__stat-card is-success">
        <div class="library-knowledge-view__stat-value">{{ stats.kbReadyCount }}</div>
        <div class="library-knowledge-view__stat-label">{{ t('library.kbReadyCount') }}</div>
      </el-card>
      <el-card class="library-knowledge-view__stat-card is-warning">
        <div class="library-knowledge-view__stat-value">{{ stats.kbProcessingCount }}</div>
        <div class="library-knowledge-view__stat-label">{{ t('library.kbProcessingCount') }}</div>
      </el-card>
      <el-card class="library-knowledge-view__stat-card is-danger">
        <div class="library-knowledge-view__stat-value">{{ failedCount }}</div>
        <div class="library-knowledge-view__stat-label">{{ t('library.kbFailedCount') }}</div>
      </el-card>
    </div>

    <el-table
      :data="kbFiles"
      v-loading="loading"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="name" :label="t('library.fileName')" min-width="200" />
      <el-table-column prop="extension" :label="t('library.fileType')" width="100" />
      <el-table-column prop="fileSize" :label="t('library.fileSize')" width="100">
        <template #default="{ row }">
          {{ formatSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('library.chunkCount')" width="100">
        <template #default="{ row }">
          {{ row.kbStatus === 'READY' ? '-' : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="kbProcessedAt" :label="t('library.processTime')" width="180" />
      <el-table-column :label="t('library.kbStatus')" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.kbStatus)" size="small">
            {{ statusLabel(row.kbStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('library.actions')" width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.kbStatus === 'FAILED'"
            size="small"
            type="warning"
            @click="handleReprocess(row.id)"
          >
            {{ t('library.reprocess') }}
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleRemoveFromKb(row.id)"
          >
            {{ t('library.removeFromKb') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && kbFiles.length === 0" :description="t('library.kbEmpty')" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchKbFiles, fetchKbStats } from '@/api/library/file'
import { toggleKbStatus } from '@/api/library/file'
import type { DocFile, DocStats } from '@/api/library/types'

const { t } = useI18n()

const kbFiles = ref<DocFile[]>([])
const stats = ref<DocStats>({
  totalFiles: 0,
  totalSize: 0,
  imageCount: 0,
  documentCount: 0,
  archiveCount: 0,
  videoCount: 0,
  otherCount: 0,
  kbReadyCount: 0,
  kbProcessingCount: 0,
})
const loading = ref(false)

const failedCount = computed(() => {
  return kbFiles.value.filter((f) => f.kbStatus === 'FAILED').length
})

async function loadKbFiles() {
  loading.value = true
  try {
    const result = await fetchKbFiles({})
    kbFiles.value = result.records
  } catch {
    kbFiles.value = []
  } finally {
    loading.value = false
  }
}

async function loadKbStats() {
  try {
    stats.value = await fetchKbStats()
  } catch {
    stats.value = {
      totalFiles: 0,
      totalSize: 0,
      imageCount: 0,
      documentCount: 0,
      archiveCount: 0,
      videoCount: 0,
      otherCount: 0,
      kbReadyCount: 0,
      kbProcessingCount: 0,
    }
  }
}

async function handleReprocess(id: number) {
  try {
    await toggleKbStatus(id)
    ElMessage.success(t('library.reprocessStarted'))
    await loadKbFiles()
    await loadKbStats()
  } catch {
    // silent
  }
}

async function handleRemoveFromKb(id: number) {
  try {
    await ElMessageBox.confirm(t('library.removeFromKbConfirm'), t('common.confirmTitle'), {
      type: 'warning',
    })
    await toggleKbStatus(id)
    ElMessage.success(t('library.removeFromKbSuccess'))
    await loadKbFiles()
    await loadKbStats()
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

function statusTagType(status: string) {
  switch (status) {
    case 'READY': return 'success'
    case 'PROCESSING': return 'warning'
    case 'FAILED': return 'danger'
    default: return 'info'
  }
}

function statusLabel(status: string) {
  switch (status) {
    case 'READY': return t('library.kbStatusReady')
    case 'PROCESSING': return t('library.kbStatusProcessing')
    case 'FAILED': return t('library.kbStatusFailed')
    default: return status
  }
}

onMounted(() => {
  loadKbStats()
  loadKbFiles()
})
</script>

<style scoped lang="scss">
.library-knowledge-view {
  padding: 20px;
}

.library-knowledge-view__header {
  margin-bottom: 20px;
}

.library-knowledge-view__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.library-knowledge-view__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.library-knowledge-view__stat-card {
  text-align: center;

  :deep(.el-card__body) {
    padding: 20px;
  }

  &.is-success .library-knowledge-view__stat-value {
    color: var(--el-color-success);
  }

  &.is-warning .library-knowledge-view__stat-value {
    color: var(--el-color-warning);
  }

  &.is-danger .library-knowledge-view__stat-value {
    color: var(--el-color-danger);
  }
}

.library-knowledge-view__stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.library-knowledge-view__stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
</style>
