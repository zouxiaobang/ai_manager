<template>
  <SchemeADoodleFrame color="#2563eb" class="note-card" @click="handleClick">
    <div class="note-card__body">
      <div class="note-card__title">{{ title }}</div>
      <div v-if="preview" class="note-card__preview">{{ preview }}</div>
      <div v-if="folderPath" class="note-card__meta">📁 {{ folderPath }}</div>
    </div>
  </SchemeADoodleFrame>
</template>

<script setup lang="ts">
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'

defineProps<{
  title: string
  preview?: string
  folderPath?: string
}>()

const emit = defineEmits<{
  click: []
}>()

function handleClick() {
  emit('click')
}
</script>

<style scoped lang="scss">
.note-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }
}

.note-card__body {
  padding: 14px;
}

.note-card__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
  margin: 0 0 6px;
}

.note-card__preview {
  font-size: 12px;
  color: #64748b;
  margin: 0 0 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-card__meta {
  font-size: 11px;
  color: #94a3b8;
  margin: 0;
}
</style>
