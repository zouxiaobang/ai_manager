<template>
  <SchemeADoodleFrame color="#f97316" class="folder-card" @click="handleClick">
    <div class="folder-card__body">
      <div class="folder-card__icon">📂</div>
      <div class="folder-card__info">
        <div class="folder-card__name">{{ name }}</div>
        <div class="folder-card__meta">{{ noteCount }} 笔记 · {{ subfolderCount }} 子文件夹</div>
      </div>
      <div class="folder-card__arrow">{{ expanded ? '▼' : '▶' }}</div>
    </div>
  </SchemeADoodleFrame>
</template>

<script setup lang="ts">
import SchemeADoodleFrame from '@/mobile/home/themes/scheme-a/SchemeADoodleFrame.vue'

defineProps<{
  name: string
  noteCount: number
  subfolderCount: number
  expanded: boolean
}>()

const emit = defineEmits<{
  click: []
}>()

function handleClick() {
  emit('click')
}
</script>

<style scoped lang="scss">
.folder-card {
  background: #fff7ed;
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }
}

.folder-card__body {
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.folder-card__icon {
  font-size: 24px;
}

.folder-card__info {
  flex: 1;
}

.folder-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
  margin: 0 0 4px;
}

.folder-card__meta {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

.folder-card__arrow {
  font-size: 12px;
  color: #f97316;
  font-weight: bold;
}
</style>
