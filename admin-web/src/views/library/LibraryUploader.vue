<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="t('library.uploadFile')"
    width="520px"
    :close-on-click-modal="false"
  >
    <el-upload
      drag
      multiple
      :http-request="customUpload"
      :show-file-list="true"
      :file-list="fileList"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">{{ t('library.dropHint') }}</div>
    </el-upload>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">{{ t('common.cancel') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadRawFile, UploadUserFile } from 'element-plus'
import { uploadFile } from '@/api/library/file'

const props = defineProps<{
  visible: boolean
  folderId: number | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  uploaded: []
}>()

const { t } = useI18n()

const fileList = ref<UploadUserFile[]>([])

async function customUpload(options: { file: File | Blob; filename: string; onSuccess: (response: unknown) => void }) {
  try {
    await uploadFile(props.folderId, options.file as UploadRawFile)
    ElMessage.success(t('library.uploadSuccess'))
    options.onSuccess?.(options.file)
    fileList.value = []
    emit('uploaded')
    emit('update:visible', false)
  } catch {
    const index = fileList.value.findIndex((f) => (f as unknown as { uid: number }).uid === (options.file as unknown as { uid: number }).uid)
    if (index !== -1) {
      fileList.value.splice(index, 1)
    }
  }
}
</script>

<style scoped lang="scss">
</style>
