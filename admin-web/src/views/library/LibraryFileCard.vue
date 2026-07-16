<template>
  <div
    class="library-file-card"
    :class="{ 'is-selected': isSelected }"
    @click="emit('select')"
    @dblclick="emit('preview')"
    @contextmenu.prevent="emit('contextmenu', $event)"
  >
    <div class="library-file-card__thumb">
      <img
        v-if="isImage"
        :src="file.thumbnailPath"
        :alt="file.name"
        class="library-file-card__img"
        loading="lazy"
      />
      <div v-else class="library-file-card__icon-wrap">
        <el-icon class="library-file-card__type-icon"><component :is="fileIcon" /></el-icon>
        <span class="library-file-card__ext">{{ file.extension }}</span>
      </div>
    </div>

    <div class="library-file-card__info">
      <div class="library-file-card__name">{{ file.name }}</div>
      <div class="library-file-card__meta">
        <span>{{ formatSize(file.fileSize) }}</span>
        <span class="library-file-card__dot">·</span>
        <span>{{ formatTime(file.updateTime) }}</span>
      </div>
    </div>

    <div class="library-file-card__footer">
      <button
        type="button"
        class="library-file-card__fav"
        :class="{ 'is-active': false }"
        @click.stop="emit('toggle-favorite')"
      >
        <el-icon><Star /></el-icon>
      </button>
      <el-tag
        v-if="file.kbStatus"
        :type="kbTagType"
        size="small"
        class="library-file-card__tag"
      >
        {{ kbStatusLabel }}
      </el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Document, Picture, VideoCamera, Files, Star } from '@element-plus/icons-vue'
import type { DocFile } from '@/api/library/types'

const props = defineProps<{
  file: DocFile
  isSelected: boolean
}>()

const emit = defineEmits<{
  select: []
  preview: []
  'toggle-favorite': []
  contextmenu: [event: MouseEvent]
}>()

const { t } = useI18n()

const isImage = computed(() => props.file.mimeType?.startsWith('image/'))
const isVideo = computed(() => props.file.mimeType?.startsWith('video/'))
const isDocument = computed(() => props.file.mimeType?.startsWith('application/'))

const fileIcon = computed(() => {
  if (isImage.value) return Picture
  if (isVideo.value) return VideoCamera
  if (isDocument.value) return Document
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
.library-file-card {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.15s, box-shadow 0.15s;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    box-shadow: 0 4px 12px rgb(0 0 0 / 8%);
  }

  &.is-selected {
    border-color: var(--el-color-primary);
    box-shadow: 0 0 0 1px var(--el-color-primary);
  }
}

.library-file-card__thumb {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 10;
  background: var(--el-fill-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.library-file-card__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.library-file-card__icon-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--el-text-color-secondary);
}

.library-file-card__type-icon {
  font-size: 32px;
}

.library-file-card__ext {
  font-size: 11px;
  text-transform: uppercase;
  color: var(--el-text-color-placeholder);
}

.library-file-card__info {
  padding: 10px 12px 6px;
  min-width: 0;
}

.library-file-card__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.library-file-card__meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.library-file-card__dot {
  color: var(--el-text-color-disabled);
}

.library-file-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px 8px;
}

.library-file-card__fav {
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

.library-file-card__tag {
  flex-shrink: 0;
}
</style>
