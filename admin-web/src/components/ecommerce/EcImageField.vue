<template>
  <div :class="['ec-image-field', `ec-image-field--${size}`]">
    <div
      class="ec-image-field__preview"
      @click="openPicker"
    >
      <img
        v-if="showImage && imageUrl"
        :src="imageUrl"
        :alt="modelValue || t('ecommerce.product.imageName')"
        class="ec-image-field__img"
        @error="onImageError"
      />
      <img
        v-else-if="fallbackSrc && !fallbackBroken"
        :src="fallbackSrc"
        :alt="t('ecommerce.product.noImage')"
        class="ec-image-field__img ec-image-field__img--fallback"
        @error="onFallbackError"
      />
      <div v-else class="ec-image-field__placeholder">
        <el-icon :size="iconSize"><Picture /></el-icon>
        <span v-if="size !== 'compact'" class="ec-image-field__placeholder-text">
          {{ t('ecommerce.product.noImage') }}
        </span>
      </div>
    </div>

    <StorageImagePickerDialog
      v-model="pickerVisible"
      :scope="scope"
      :readonly="readonly"
      :width="previewWidth"
      :title="dialogTitle"
      :upload-file="uploadLocalImage"
      @confirm="onImagePicked"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Picture } from '@element-plus/icons-vue'
import { getEcommerceImageUrl, uploadEcommerceImage } from '@/api/ecommerce/image'
import { getNotebookImageUrl, uploadNotebookImage } from '@/api/notebook/image'
import StorageImagePickerDialog from '@/components/storage/StorageImagePickerDialog.vue'
import type { ImagePickerScope } from '@/components/storage/StorageImagePickerPanel.vue'

const props = withDefaults(defineProps<{
  modelValue?: string
  size?: 'compact' | 'medium' | 'large'
  dialogTitle?: string
  readonly?: boolean
  fallbackSrc?: string
  scope?: ImagePickerScope
}>(), {
  modelValue: '',
  size: 'medium',
  readonly: false,
  fallbackSrc: '',
  scope: 'ecommerce',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  persist: []
}>()

const { t } = useI18n()

const pickerVisible = ref(false)
const imageBroken = ref(false)
const fallbackBroken = ref(false)

const imageUrl = computed(() => {
  if (props.scope === 'notebook') {
    return getNotebookImageUrl(props.modelValue)
  }
  return getEcommerceImageUrl(props.modelValue)
})

const showImage = computed(() => Boolean(props.modelValue?.trim()) && !imageBroken.value)

const iconSize = computed(() => {
  if (props.size === 'compact') return 18
  if (props.size === 'large') return 32
  return 24
})

const previewWidth = computed(() => {
  if (props.size === 'compact') return '800px'
  return '920px'
})

watch(() => props.modelValue, () => {
  imageBroken.value = false
})

watch(() => props.fallbackSrc, () => {
  fallbackBroken.value = false
})

function emitPersist() {
  emit('persist')
}

function onImageError() {
  imageBroken.value = true
}

function onFallbackError() {
  fallbackBroken.value = true
}

function openPicker() {
  pickerVisible.value = true
}

function onImagePicked(fileName: string) {
  emit('update:modelValue', fileName)
  imageBroken.value = false
  emitPersist()
}

async function uploadLocalImage(file: File): Promise<string> {
  if (props.scope === 'notebook') {
    return uploadNotebookImage(file)
  }
  return uploadEcommerceImage(file)
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

  &--fallback {
    object-fit: contain;
    padding: 14%;
    background: #f3f4f6;
  }
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
</style>
