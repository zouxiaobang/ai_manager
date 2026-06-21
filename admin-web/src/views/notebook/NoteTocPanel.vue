<template>
  <div class="note-toc-panel">
    <el-tree
      v-if="treeData.length"
      :data="treeData"
      node-key="id"
      :props="treeProps"
      default-expand-all
      :expand-on-click-node="false"
      class="note-toc-panel__tree"
      @node-click="onNodeClick"
    >
      <template #default="{ data }">
        <span
          class="note-toc-tree-node"
          :class="`is-level-${data.level}`"
          :title="data.label"
        >
          {{ data.label }}
        </span>
      </template>
    </el-tree>
    <el-empty v-else :description="emptyText" :image-size="56" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { buildNoteTocTree, type NoteTocItem, type NoteTocTreeNode } from './noteToc'

const props = defineProps<{
  items: NoteTocItem[]
  emptyText: string
}>()

const emit = defineEmits<{
  select: [index: number]
}>()

const treeProps = { label: 'label', children: 'children' }

const treeData = computed(() => buildNoteTocTree(props.items))

function onNodeClick(data: NoteTocTreeNode) {
  emit('select', data.index)
}
</script>

<style scoped lang="scss">
.note-toc-panel {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 4px 0 12px;
}

.note-toc-panel__tree {
  padding: 0 4px;
  background: transparent;
}

.note-toc-tree-node {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  line-height: 1.4;
  color: var(--el-text-color-regular);

  &.is-level-1 {
    font-weight: 600;
  }

  &.is-level-4,
  &.is-level-5,
  &.is-level-6 {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

:deep(.el-tree-node__content) {
  height: 32px;
  border-radius: 4px;
}

:deep(.el-tree-node__content:hover) {
  background: var(--el-color-primary-light-9);
}

:deep(.el-tree-node__content:hover .note-toc-tree-node) {
  color: var(--el-color-primary);
}

:deep(.el-tree-node:focus > .el-tree-node__content) {
  background: var(--el-color-primary-light-9);
}

:deep(.el-tree-node__expand-icon) {
  color: var(--el-text-color-secondary);
}
</style>
