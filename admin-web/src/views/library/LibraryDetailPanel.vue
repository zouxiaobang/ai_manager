<template>
  <el-drawer
    :model-value="file !== null"
    @update:model-value="handleClose"
    :title="file?.name || ''"
    direction="rtl"
    size="320px"
    :close-on-click-modal="true"
    class="library-detail-panel"
  >
    <template v-if="file">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item :label="t('library.fileName')">
          {{ file.name }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('library.fileType')">
          {{ file.mimeType }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('library.fileSize')">
          {{ formatSize(file.fileSize) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('library.createTime')">
          {{ file.createTime }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('library.updateTime')">
          {{ file.updateTime }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('library.sha256')">
          <span class="library-detail-panel__hash">{{ file.contentHash || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('library.storagePath')">
          <span class="library-detail-panel__path">{{ file.storagePath || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="library-detail-panel__section">
        <label class="library-detail-panel__label">{{ t('library.tags') }}</label>
        <div class="library-detail-panel__tags">
          <el-tag
            v-for="tag in (file.tags || [])"
            :key="tag.id"
            closable
            size="small"
            @close="$emit('tag', { fileId: file.id, tagId: tag.id, action: 'remove' })"
          >
            {{ tag.name }}
          </el-tag>
          <el-button size="small" @click="$emit('tag', { fileId: file.id, action: 'manage' })">
            {{ t('library.manageTags') }}
          </el-button>
        </div>
      </div>

      <div class="library-detail-panel__section">
        <label class="library-detail-panel__label">{{ t('library.description') }}</label>
        <el-input
          :model-value="file.description"
          type="textarea"
          :rows="3"
          @blur="handleDescriptionChange"
        />
      </div>

      <div class="library-detail-panel__section">
        <label class="library-detail-panel__label">{{ t('library.stats') }}</label>
        <div class="library-detail-panel__stats">
          <span>{{ t('library.viewCount') }}: {{ file.viewCount }}</span>
          <span>{{ t('library.downloadCount') }}: {{ file.downloadCount }}</span>
        </div>
      </div>

      <div class="library-detail-panel__actions">
        <el-button :type="file.isPinned ? 'warning' : 'default'" @click="handleTogglePin">
          <el-icon><Star /></el-icon>
          {{ file.isPinned ? t('library.unpin') : t('library.pin') }}
        </el-button>
        <el-button @click="$emit('kb', file.id)">
          <el-icon><Reading /></el-icon>
          {{ t('library.kbMark') }}
        </el-button>
        <el-button @click="handleDownload">
          <el-icon><Download /></el-icon>
          {{ t('library.download') }}
        </el-button>
        <el-button type="danger" @click="handleDelete">
          <el-icon><Delete /></el-icon>
          {{ t('library.delete') }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Star, Reading, Download, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { DocFile } from '@/api/library/types'
import { togglePin, removeFile, updateFileDescription } from '@/api/library/file'

// contentHash/storagePath 仅详情接口返回，作为可选字段并入 props 类型
const props = defineProps<{
  file: (DocFile & { contentHash?: string; storagePath?: string }) | null
}>()

const emit = defineEmits<{
  close: []
  pin: [fileId: number]
  tag: [payload: { fileId: number; tagId?: number; action: string }]
  kb: [fileId: number]
  download: [fileId: number]
  delete: [fileId: number]
}>()

const { t } = useI18n()

function handleClose() {
  emit('close')
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

async function handleTogglePin() {
  if (!props.file) return
  try {
    await togglePin(props.file.id)
    ElMessage.success(t('common.saved'))
    emit('pin', props.file.id)
  } catch {
    // silent
  }
}

async function handleDownload() {
  if (!props.file) return
  const base = import.meta.env.VITE_API_BASE || ''
  window.open(`${base}/api/library/files/${props.file.id}/download`, '_blank')
  emit('download', props.file.id)
}

async function handleDelete() {
  if (!props.file) return
  try {
    await removeFile(props.file.id)
    ElMessage.success(t('library.deleteSuccess'))
    emit('delete', props.file.id)
    emit('close')
  } catch {
    // silent
  }
}

async function handleDescriptionChange(e: FocusEvent) {
  const target = e.target as HTMLInputElement
  const value = target.value?.trim()
  if (props.file && value !== undefined) {
    try {
      await updateFileDescription(props.file.id, value)
      ElMessage.success(t('common.saved'))
    } catch {
      // silent
    }
  }
}

watch(
  () => props.file,
  (newFile) => {
    if (!newFile) {
      emit('close')
    }
  },
)
</script>

<style scoped lang="scss">
.library-detail-panel__section {
  margin-top: 16px;
}

.library-detail-panel__label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.library-detail-panel__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.library-detail-panel__hash,
.library-detail-panel__path {
  font-family: monospace;
  font-size: 12px;
  word-break: break-all;
}

.library-detail-panel__stats {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.library-detail-panel__actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  margin-top: 24px;

  :deep(.el-button) {
    min-width: 180px;
  }
}
</style>
