<template>
  <el-dialog
    :model-value="file !== null"
    @update:model-value="$emit('close')"
    fullscreen
    :close-on-click-modal="false"
    class="library-preview"
  >
    <template #header>
      <div class="library-preview__header">
        <span class="library-preview__filename">{{ file?.name }}</span>
      </div>
    </template>

    <div v-if="file" class="library-preview__body">
      <template v-if="isImage">
        <el-image
          :src="previewUrl"
          :preview-src-list="previewList"
          fit="contain"
          class="library-preview__image"
        />
      </template>
      <template v-else-if="isPdf">
        <iframe
          :src="previewUrl"
          class="library-preview__iframe"
          frameborder="0"
        />
      </template>
      <template v-else-if="isText">
        <div class="library-preview__text">{{ textContent }}</div>
      </template>
      <template v-else>
        <div class="library-preview__unsupported">
          <el-icon class="library-preview__unsupported-icon"><Document /></el-icon>
          <p>{{ t('library.previewUnsupported') }}</p>
          <el-button type="primary" @click="handleDownload">
            {{ t('library.download') }}
          </el-button>
        </div>
      </template>
    </div>

    <template #footer>
      <div class="library-preview__footer">
        <span>{{ t('library.fileSize') }}: {{ formatSize(file?.fileSize) }}</span>
        <span>{{ t('library.fileType') }}: {{ file?.mimeType }}</span>
        <span>{{ t('library.uploadTime') }}: {{ file?.createTime }}</span>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Document } from '@element-plus/icons-vue'
import type { DocFile } from '@/api/library/types'
import { getData } from '@/api/request'

const props = defineProps<{
  file: DocFile | null
  folderFiles?: DocFile[]
}>()

defineEmits<{
  close: []
}>()

const { t } = useI18n()
const textContent = ref('')

const isImage = computed(() => props.file?.mimeType?.startsWith('image/'))
const isPdf = computed(() => props.file?.mimeType === 'application/pdf')
const isText = computed(() => props.file?.mimeType?.startsWith('text/'))

const previewUrl = computed(() => {
  if (!props.file) return ''
  const base = import.meta.env.VITE_API_BASE || ''
  return `${base}/api/library/files/${props.file.id}/preview`
})

const imageFiles = computed(() => {
  if (!props.folderFiles) return props.file ? [props.file] : []
  return props.folderFiles.filter((f) => f.mimeType?.startsWith('image/'))
})

const previewList = computed(() => {
  const base = import.meta.env.VITE_API_BASE || ''
  return imageFiles.value.map((f) => `${base}/api/library/files/${f.id}/preview`)
})

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

function handleDownload() {
  if (props.file) {
    const base = import.meta.env.VITE_API_BASE || ''
    window.open(`${base}/api/library/files/${props.file.id}/download`, '_blank')
  }
}

watch(
  () => props.file,
  async (newFile) => {
    if (newFile && isText.value) {
      try {
        textContent.value = await getData<string>(`/api/library/files/${newFile.id}/content`)
      } catch {
        textContent.value = ''
      }
    } else {
      textContent.value = ''
    }
  },
)
</script>

<style scoped lang="scss">
.library-preview__header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.library-preview__filename {
  font-size: 16px;
  font-weight: 600;
}

.library-preview__body {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  height: calc(100vh - 180px);
}

.library-preview__image {
  max-width: 100%;
  max-height: 100%;
}

.library-preview__iframe {
  width: 100%;
  height: 100%;
}

.library-preview__text {
  width: 100%;
  height: 100%;
  overflow: auto;
  white-space: pre-wrap;
  padding: 16px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-family: monospace;
  font-size: 14px;
  line-height: 1.6;
}

.library-preview__unsupported {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: var(--el-text-color-secondary);
}

.library-preview__unsupported-icon {
  font-size: 64px;
}

.library-preview__footer {
  display: flex;
  justify-content: center;
  gap: 24px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
