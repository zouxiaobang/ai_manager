<template>
  <div :class="['ec-image-field', `ec-image-field--${size}`]">
    <div
      class="ec-image-field__preview"
      :title="t('ecommerce.product.imageClickHint')"
      @click="openPreview"
    >
      <img
        v-if="showImage && imageUrl"
        :src="imageUrl"
        :alt="modelValue || t('ecommerce.product.imageName')"
        class="ec-image-field__img"
        @error="onImageError"
      />
      <div v-else class="ec-image-field__placeholder">
        <el-icon :size="iconSize"><Picture /></el-icon>
        <span v-if="size !== 'compact'" class="ec-image-field__placeholder-text">
          {{ t('ecommerce.product.noImage') }}
        </span>
      </div>
    </div>
    <p v-if="showName && modelValue" class="ec-image-field__name">{{ modelValue }}</p>

    <el-dialog
      v-model="previewVisible"
      :title="dialogTitle"
      :width="previewWidth"
      append-to-body
      destroy-on-close
      class="ec-image-preview-dialog"
      @closed="previewBroken = false"
    >
      <div class="ec-image-preview">
        <div class="ec-image-preview__body">
          <img
            v-if="showImage && imageUrl && !previewBroken"
            :src="imageUrl"
            :alt="modelValue || t('ecommerce.product.imageName')"
            class="ec-image-preview__img"
            @error="previewBroken = true"
          />
          <div v-else class="ec-image-preview__empty">
            <el-icon :size="48"><Picture /></el-icon>
            <p>{{ t('ecommerce.product.noImage') }}</p>
          </div>
        </div>
        <div v-if="!readonly" class="ec-image-preview__footer">
          <input
            ref="fileInputRef"
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            hidden
            @change="onFileSelected"
          />
          <el-button type="primary" :loading="uploading" @click="triggerUpload">
            {{ t('ecommerce.product.uploadImage') }}
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { getEcommerceImageUrl, uploadEcommerceImage } from '@/api/ecommerce/image'

const props = withDefaults(defineProps<{
  modelValue?: string
  size?: 'compact' | 'medium' | 'large'
  showName?: boolean
  dialogTitle?: string
  readonly?: boolean
}>(), {
  modelValue: '',
  size: 'medium',
  showName: false,
  readonly: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const { t } = useI18n()

const previewVisible = ref(false)
const uploading = ref(false)
const imageBroken = ref(false)
const previewBroken = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)

const imageUrl = computed(() => getEcommerceImageUrl(props.modelValue))

const showImage = computed(() => Boolean(props.modelValue?.trim()) && !imageBroken.value)

const iconSize = computed(() => {
  if (props.size === 'compact') return 18
  if (props.size === 'large') return 32
  return 24
})

const previewWidth = computed(() => {
  if (props.size === 'compact') return '520px'
  return '560px'
})

const dialogTitle = computed(() => props.dialogTitle || t('ecommerce.product.imagePreview'))

watch(() => props.modelValue, () => {
  imageBroken.value = false
  previewBroken.value = false
})

function onImageError() {
  imageBroken.value = true
}

function openPreview() {
  previewBroken.value = false
  previewVisible.value = true
}

function triggerUpload() {
  fileInputRef.value?.click()
}

async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  uploading.value = true
  try {
    const fileName = await uploadEcommerceImage(file)
    emit('update:modelValue', fileName)
    imageBroken.value = false
    previewBroken.value = false
    ElMessage.success(t('ecommerce.product.uploadSuccess'))
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped lang="scss">
.ec-image-field {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.ec-image-field__preview {
  border: 1px dashed var(--el-border-color);
  border-radius: 10px;
  overflow: hidden;
  background: var(--el-fill-color-light);
  cursor: zoom-in;
  transition: border-color 0.2s, box-shadow 0.2s;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    box-shadow: 0 2px 10px rgb(64 158 255 / 12%);
  }
}

.ec-image-field--compact .ec-image-field__preview {
  width: 48px;
  height: 48px;
}

.ec-image-field--medium .ec-image-field__preview {
  width: 96px;
  height: 96px;
}

.ec-image-field--large .ec-image-field__preview {
  width: 160px;
  height: 160px;
}

.ec-image-field__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.ec-image-field__placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--el-text-color-secondary);
}

.ec-image-field__placeholder-text {
  font-size: 12px;
}

.ec-image-field__name {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  word-break: break-all;
  max-width: 160px;
}

.ec-image-preview {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ec-image-preview__body {
  min-height: 280px;
  max-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  overflow: hidden;
}

.ec-image-preview__img {
  max-width: 100%;
  max-height: 60vh;
  object-fit: contain;
  display: block;
}

.ec-image-preview__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
  padding: 40px;
}

.ec-image-preview__footer {
  display: flex;
  justify-content: center;
  padding-top: 4px;
}
</style>
