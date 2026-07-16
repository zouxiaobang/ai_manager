<template>
  <div
    class="library-file-row"
    :class="{ 'is-selected': selected }"
    @click="emit('select')"
    @contextmenu.prevent="emit('contextmenu', $event)"
  >
    <el-checkbox :model-value="selected" @click.stop @change="emit('select')" />
    <div class="library-file-row__icon">
      <el-icon><component :is="fileIcon" /></el-icon>
    </div>
    <div class="library-file-row__name" @dblclick.stop="emit('preview')">
      <span class="library-file-row__name-text">{{ file.name }}</span>
      <span v-if="file.extension" class="library-file-row__ext">.{{ file.extension }}</span>
    </div>
    <div class="library-file-row__size">{{ formatSize(file.fileSize) }}</div>
    <div class="library-file-row__time">{{ formatTime(file.updateTime) }}</div>
    <div class="library-file-row__actions">
      <button
        type="button"
        class="library-file-row__fav"
        :class="{ 'is-active': false }"
        @click.stop="emit('toggle-favorite')"
      >
        <el-icon><Star /></el-icon>
      </button>
      <el-tag
        v-if="file.kbStatus"
        :type="kbTagType"
        size="small"
      >
        {{ kbStatusLabel }}
      </el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Document, Files, Picture, Star, VideoCamera } from '@element-plus/icons-vue'
import type { DocFile } from '@/api/library/types'

const props = defineProps<{
  file: DocFile
  selected: boolean
}>()

const emit = defineEmits<{
  select: []
  preview: []
  'toggle-favorite': []
  contextmenu: [event: MouseEvent]
}>()

const { t } = useI18n()

const fileIcon = computed(() => {
  const type = props.file.mimeType ?? ''
  if (type.startsWith('image/')) return Picture
  if (type.startsWith('video/')) return VideoCamera
  if (type.startsWith('application/')) return Document
  return Files
})

const kbTagType = computed(() => {
  if (props.file.kbStatus === 'indexed') return 'success'
  if (props.file.kbStatus === 'processing') return 'info'
  return 'default'
})

const kbStatusLabel = computed(() => {
  switch (props.file.kbStatus) {
    case 'indexed': return t('library.kbIndexed')
    case 'processing': return t('library.kbProcessing')
    default: return ''
  }
})

function formatSize(bytes: number): string {
  if (!bytes) return '—'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(i > 0 ? 1 : 0)} ${units[i]}`
}

function formatTime(time?: string): string {
  if (!time) return '—'
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return t('library.justNow')
  if (diff < 3600000) return t('library.minutesAgo', { count: Math.floor(diff / 60000) })
  if (diff < 86400000) return t('library.hoursAgo', { count: Math.floor(diff / 3600000) })
  return d.toLocaleDateString()
}
</script>

<style scoped lang="scss">
.library-file-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &.is-selected {
    background: var(--el-color-primary-light-9);
  }
}

.library-file-row__icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
  font-size: 18px;
}

.library-file-row__name {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
}

.library-file-row__name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.library-file-row__ext {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  text-transform: lowercase;
}

.library-file-row__size {
  width: 80px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.library-file-row__time {
  width: 120px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.library-file-row__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.library-file-row__fav {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--el-text-color-placeholder);
  cursor: pointer;
  font-size: 16px;
  transition: color 0.15s;

  &:hover {
    color: var(--el-color-warning);
  }

  &.is-active {
    color: var(--el-color-warning);
  }
}
</style>
