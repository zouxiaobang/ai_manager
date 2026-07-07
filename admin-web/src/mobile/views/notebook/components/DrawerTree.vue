<template>
  <template v-for="node in nodes" :key="node.nodeKey">
    <!-- FOLDER node -->
    <template v-if="node.nodeType === 'FOLDER'">
      <div
        class="drawer-tree-node drawer-tree-node--folder"
        :class="{ 'drawer-tree-node--expanded': expandedKeys.has(node.nodeKey) }"
        :style="{ paddingLeft: depth * 16 + 12 + 'px' }"
      >
        <span
          class="drawer-tree-node__arrow"
          @click.stop="emit('toggle', node.nodeKey)"
        >
          {{ expandedKeys.has(node.nodeKey) ? '▼' : '▶' }}
        </span>
        <svg width="16" height="16" class="drawer-tree-node__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
        <span
          class="drawer-tree-node__label"
          @click.stop="emit('closeDrawer'); emit('navigate', node)"
        >
          {{ node.name }}
        </span>
        <span class="drawer-tree-node__count">{{ countNotes(node) }}</span>
      </div>

      <!-- Children (recursive) -->
      <DrawerTree
        v-if="expandedKeys.has(node.nodeKey) && node.children?.length"
        :nodes="node.children"
        :expanded-keys="expandedKeys"
        :depth="depth + 1"
        @toggle="emit('toggle', $event)"
        @navigate="emit('navigate', $event)"
        @open-note="emit('openNote', $event)"
        @close-drawer="emit('closeDrawer')"
      />
    </template>

    <!-- NOTE node -->
    <div
      v-else-if="node.nodeType === 'NOTE' && node.noteId"
      class="drawer-tree-node drawer-tree-node--note"
      :style="{ paddingLeft: depth * 16 + 12 + 'px' }"
      @click.stop="emit('closeDrawer'); emit('openNote', node.noteId)"
    >
      <svg width="16" height="16" class="drawer-tree-node__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
      <span class="drawer-tree-node__label">{{ node.name }}</span>
    </div>
  </template>
</template>

<script setup lang="ts">
import type { NbTreeNode } from '@/api/notebook'

defineOptions({ name: 'DrawerTree' })

withDefaults(defineProps<{
  nodes: NbTreeNode[]
  expandedKeys: Set<string>
  depth?: number
}>(), {
  depth: 0,
})

const emit = defineEmits<{
  toggle: [nodeKey: string]
  navigate: [node: NbTreeNode]
  openNote: [noteId: number]
  closeDrawer: []
}>()

function countNotes(node: NbTreeNode): number {
  let count = 0
  if (node.nodeType === 'NOTE') count = 1
  if (node.children?.length) {
    count += node.children.reduce((acc, child) => acc + countNotes(child), 0)
  }
  return count
}
</script>

<style scoped lang="scss">
$blue: #2563eb;
$orange: #f97316;
$text-primary: #1e293b;
$text-muted: #94a3b8;
$border: #e2e8f0;

.drawer-tree-node {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
  user-select: none;
  transition: all 0.15s ease;

  &:hover {
    background: rgba($blue, 0.06);
  }

  &:active {
    background: rgba($blue, 0.1);
  }

  &--folder {
    font-weight: 500;
    color: $text-primary;

    &.drawer-tree-node--expanded {
      background: rgba($orange, 0.06);
    }
  }

  &--note {
    color: $text-primary;
  }

  &__arrow {
    font-size: 10px;
    color: $text-muted;
    flex-shrink: 0;
    width: 16px;
    text-align: center;
    transition: transform 0.2s;
    padding: 4px 0;
  }

  &__icon {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
    display: block;
  }

  &__label {
    flex: 1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 14px;
  }

  &__count {
    font-size: 11px;
    color: $text-muted;
    background: $border;
    padding: 1px 8px;
    border-radius: 8px;
    flex-shrink: 0;
  }
}
</style>
