<template>
  <div class="note-toc-panel">
    <el-tree
      v-if="treeData.length"
      :data="treeData"
      node-key="id"
      :props="treeProps"
      :current-node-key="activeNodeId"
      default-expand-all
      highlight-current
      :expand-on-click-node="false"
      class="note-toc-panel__tree"
      @node-click="onNodeClick"
    >
      <template #default="{ data }">
        <span
          class="note-toc-tree-node"
          :class="[
            `is-level-${data.level}`,
            { 'is-active': data.index === activeIndex },
          ]"
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
  activeIndex?: number
}>()

const emit = defineEmits<{
  select: [index: number]
}>()

const treeProps = { label: 'label', children: 'children' }

const treeData = computed(() => buildNoteTocTree(props.items))

const activeNodeId = computed(() => {
  if (props.activeIndex == null || props.activeIndex < 0) return ''
  return props.items[props.activeIndex]?.id ?? ''
})

function onNodeClick(data: NoteTocTreeNode) {
  emit('select', data.index)
}
</script>

<style scoped lang="scss">
.note-toc-panel {
  flex: 0 1 auto;
  min-height: 0;
  overflow: auto;
  padding: 4px 8px 12px;
}

.note-toc-panel__tree {
  padding: 0;
  background: transparent;
}

.note-toc-tree-node {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  line-height: 1.5;
  color: var(--wr-text-secondary, #666);

  &.is-level-1 {
    font-weight: 600;
    color: var(--wr-text, #333);
  }

  &.is-level-2 {
    font-size: 13px;
  }

  &.is-level-3,
  &.is-level-4,
  &.is-level-5,
  &.is-level-6 {
    font-size: 12px;
    color: var(--wr-muted, #999);
  }

  &.is-active {
    color: var(--wr-rail-active-color, #0b21c7);
    font-weight: 600;
  }
}

:deep(.el-tree-node__content) {
  position: relative;
  height: 34px;
  border-radius: 8px;
  padding-left: 10px !important;
}

:deep(.el-tree-node__content:hover) {
  background: var(--wr-stat-blue-bg, #eff6ff);
}

:deep(.el-tree-node__content:hover .note-toc-tree-node) {
  color: var(--wr-rail-active-color, #0b21c7);
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: var(--wr-stat-blue-bg, #eff6ff);
}

:deep(.el-tree-node.is-current > .el-tree-node__content::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 3px;
  border-radius: 2px;
  background: var(--wr-rail-active-color, #0b21c7);
}

:deep(.el-tree-node__expand-icon) {
  color: var(--wr-muted, #999);
  font-size: 12px;
}
</style>
