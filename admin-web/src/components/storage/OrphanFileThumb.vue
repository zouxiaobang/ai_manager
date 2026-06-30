<template>
  <span
    class="orphan-file-thumb"
    :class="[
      `orphan-file-thumb--${size}`,
      { 'is-clickable': showImagePreview && previewable },
    ]"
  >
    <img
      v-if="showImagePreview && !previewable"
      :src="previewUrl"
      class="orphan-file-thumb__image"
      alt=""
      @error="onImageError"
    />
    <el-image
      v-else-if="showImagePreview"
      :src="previewUrl"
      :preview-src-list="[previewUrl]"
      :initial-index="0"
      fit="cover"
      preview-teleported
      hide-on-click-modal
      class="orphan-file-thumb__image"
      @error="onImageError"
    />
    <span
      v-else
      class="orphan-file-type-icon"
      :class="`orphan-file-type-icon--${fileTypeKind}`"
      aria-hidden="true"
    >
      <el-icon v-if="fileTypeKind === 'file'"><Document /></el-icon>
    </span>
  </span>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Document } from '@element-plus/icons-vue'
import {
  isOrphanImageFile,
  resolveOrphanFilePreviewUrl,
  resolveOrphanFileTypeKind,
} from '@/utils/orphanFileDisplay'

const props = withDefaults(
  defineProps<{
    zoneKey: string
    fileName: string
    relativePath: string
    size?: 'default' | 'large'
    previewable?: boolean
  }>(),
  {
    size: 'default',
    previewable: true,
  },
)

const imageBroken = ref(false)

const previewUrl = computed(() =>
  resolveOrphanFilePreviewUrl(props.zoneKey, props.fileName, props.relativePath),
)

const fileTypeKind = computed(() => resolveOrphanFileTypeKind(props.fileName))

const showImagePreview = computed(() =>
  Boolean(previewUrl.value) && isOrphanImageFile(props.fileName) && !imageBroken.value,
)

watch(
  () => [props.zoneKey, props.fileName, props.relativePath] as const,
  () => {
    imageBroken.value = false
  },
)

function onImageError() {
  imageBroken.value = true
}
</script>

<style scoped lang="scss">
.orphan-file-thumb {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;

  &.is-clickable {
    cursor: zoom-in;
  }

  &--large {
    width: 88px;
    height: 88px;
  }
}

.orphan-file-thumb__image {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  overflow: hidden;
  object-fit: cover;

  :deep(.el-image__inner) {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.orphan-file-thumb--large {
  .orphan-file-thumb__image,
  .orphan-file-type-icon {
    width: 88px;
    height: 88px;
    border-radius: 12px;
  }

  .orphan-file-type-icon {
    &--xlsx,
    &--xls {
      font-size: 36px;
    }

    &--csv,
    &--html,
    &--json {
      font-size: 16px;
    }

    &--file {
      font-size: 36px;
    }
  }
}

.orphan-file-type-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1;

  &--xlsx,
  &--xls {
    background: #217346;
    font-size: 18px;

    &::before {
      content: 'X';
    }
  }

  &--csv {
    background: #0284c7;
    font-size: 10px;

    &::before {
      content: 'CSV';
    }
  }

  &--html {
    background: #ea580c;
    font-size: 10px;

    &::before {
      content: 'HTML';
    }
  }

  &--json {
    background: #7c3aed;
    font-size: 10px;

    &::before {
      content: 'JSON';
    }
  }

  &--file {
    background: #6b7280;
    font-size: 18px;
  }
}
</style>
