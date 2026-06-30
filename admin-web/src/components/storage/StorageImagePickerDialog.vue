<template>
  <el-dialog
    v-model="visible"
    :title="title || t('imagePicker.title')"
    :width="width"
    append-to-body
    destroy-on-close
    class="storage-image-picker-dialog"
    @closed="onClosed"
  >
    <el-tabs v-model="activeTab" class="storage-image-picker-dialog__tabs">
      <el-tab-pane :label="t('imagePicker.tabProject')" name="project">
        <StorageImagePickerPanel
          ref="panelRef"
          v-model="selectedItem"
          :scope="scope"
          @open-image-space="visible = false"
        />
      </el-tab-pane>
      <el-tab-pane :label="t('imagePicker.tabLocal')" name="local">
        <div
          class="storage-image-picker-dialog__dropzone"
          :class="{
            'is-dragover': dragOver,
            'is-readonly': readonly,
          }"
          @click="triggerLocalPick"
          @dragenter.prevent="onDragEnter"
          @dragover.prevent="onDragEnter"
          @dragleave.prevent="onDragLeave"
          @drop.prevent="onDrop"
        >
          <div class="storage-image-picker-dialog__dropzone-inner">
            <el-icon :size="36"><UploadFilled /></el-icon>
            <p class="storage-image-picker-dialog__dropzone-title">
              {{ t('ecommerce.product.uploadDropTitle') }}
            </p>
            <p class="storage-image-picker-dialog__dropzone-hint">
              {{ t('ecommerce.product.uploadDropHint') }}
            </p>
            <p class="storage-image-picker-dialog__dropzone-formats">
              {{ t('ecommerce.product.uploadFormats') }}
            </p>
          </div>
          <input
            ref="fileInputRef"
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            hidden
            @change="onFileSelected"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <div class="storage-image-picker-dialog__footer">
        <el-button @click="visible = false">{{ t('storageCenter.cancel') }}</el-button>
        <el-button
          v-if="activeTab === 'project'"
          type="primary"
          :disabled="!selectedItem"
          @click="confirmProject"
        >
          {{ t('imagePicker.useSelected') }}
        </el-button>
        <el-button
          v-else-if="!readonly"
          type="primary"
          :loading="uploading"
          @click="triggerLocalPick"
        >
          {{ t('ecommerce.product.uploadImage') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import StorageImagePickerPanel, {
  type ImagePickerScope,
} from '@/components/storage/StorageImagePickerPanel.vue'
import {
  resolveStorageImageValue,
  type StorageImageItem,
} from '@/api/storageImage'

const props = withDefaults(
  defineProps<{
    scope?: ImagePickerScope
    readonly?: boolean
    width?: string
    title?: string
    uploadFile?: (file: File) => Promise<string>
  }>(),
  {
    scope: 'ecommerce',
    readonly: false,
    width: '920px',
  },
)

const visible = defineModel<boolean>({ default: false })

const emit = defineEmits<{
  confirm: [value: string]
}>()

const { t } = useI18n()

const activeTab = ref<'project' | 'local'>('project')
const selectedItem = ref<StorageImageItem | null>(null)
const uploading = ref(false)
const dragOver = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const panelRef = ref<InstanceType<typeof StorageImagePickerPanel> | null>(null)

watch(visible, (open) => {
  if (open) {
    activeTab.value = 'project'
    selectedItem.value = null
  }
})

function onClosed() {
  selectedItem.value = null
  dragOver.value = false
  uploading.value = false
}

function confirmProject() {
  if (!selectedItem.value) {
    ElMessage.warning(t('imagePicker.selectFirst'))
    return
  }
  emit('confirm', resolveStorageImageValue(selectedItem.value))
  visible.value = false
}

function triggerLocalPick() {
  if (props.readonly || uploading.value || !props.uploadFile) return
  fileInputRef.value?.click()
}

function onDragEnter() {
  if (props.readonly || uploading.value || !props.uploadFile) return
  dragOver.value = true
}

function onDragLeave() {
  dragOver.value = false
}

function onDrop(event: DragEvent) {
  dragOver.value = false
  if (props.readonly || uploading.value || !props.uploadFile) return
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    void uploadLocal(file)
  }
}

async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !props.uploadFile) return
  await uploadLocal(file)
}

async function uploadLocal(file: File) {
  if (!props.uploadFile) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning(t('ecommerce.product.uploadInvalidType'))
    return
  }
  uploading.value = true
  try {
    const fileName = await props.uploadFile(file)
    emit('confirm', fileName)
    visible.value = false
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped lang="scss">
.storage-image-picker-dialog {
  &__tabs {
    :deep(.el-tabs__header) {
      margin-bottom: 14px;
    }
  }

  &__dropzone {
    min-height: 280px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    border-radius: 14px;
    border: 2px dashed #bfdbfe;
    background: #eff6ff;
    cursor: pointer;
    transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;

    &:hover:not(.is-readonly) {
      border-color: #60a5fa;
      background: #dbeafe;
    }

    &.is-dragover {
      border-color: #3b82f6;
      background: #dbeafe;
      box-shadow: 0 0 0 4px rgb(59 130 246 / 12%);
    }

    &.is-readonly {
      cursor: default;
    }
  }

  &__dropzone-inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 8px;
    color: #64748b;
    pointer-events: none;
  }

  &__dropzone-title {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #1e293b;
  }

  &__dropzone-hint,
  &__dropzone-formats {
    margin: 0;
    font-size: 13px;
    color: #64748b;
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
}
</style>
