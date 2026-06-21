<template>
  <Teleport to="body">
    <div
      v-if="visible && node"
      ref="menuRef"
      class="note-tree-context-menu"
      :style="{ left: `${x}px`, top: `${y}px` }"
      @click.stop
      @contextmenu.prevent
    >
      <template v-if="node.nodeType === 'NOTE'">
        <button type="button" class="note-tree-context-menu__item" @click="emitAction('pin')">
          <span>{{ node.isPinned === 1 ? t('notebook.unpin') : t('notebook.pin') }}</span>
          <kbd>Ctrl+Shift+T</kbd>
        </button>
        <button type="button" class="note-tree-context-menu__item" @click="emitAction('favorite')">
          <span>{{ node.isFavorite === 1 ? t('notebook.unfavorite') : t('notebook.favorite') }}</span>
          <kbd>Ctrl+Shift+C</kbd>
        </button>
        <button type="button" class="note-tree-context-menu__item" @click="emitAction('rename')">
          <span>{{ t('notebook.renameNote') }}</span>
        </button>
        <button type="button" class="note-tree-context-menu__item" @click="emitAction('move')">
          <span>{{ t('notebook.moveTo') }}</span>
          <kbd>Ctrl+Alt+M</kbd>
        </button>
        <button type="button" class="note-tree-context-menu__item" @click="emitAction('copy')">
          <span>{{ t('notebook.copy') }}</span>
          <kbd>Ctrl+C</kbd>
        </button>
        <button
          type="button"
          class="note-tree-context-menu__item"
          :class="{ 'is-disabled': !canPaste }"
          :disabled="!canPaste"
          @click="emitAction('paste')"
        >
          <span>{{ t('notebook.paste') }}</span>
          <kbd>Ctrl+V</kbd>
        </button>
        <div class="note-tree-context-menu__divider" />
        <button type="button" class="note-tree-context-menu__item is-danger" @click="emitAction('delete')">
          <span>{{ t('notebook.deleteNote') }}</span>
        </button>
        <div class="note-tree-context-menu__divider" />
        <button type="button" class="note-tree-context-menu__item" @click="emitAction('exportWord')">
          <span>{{ t('notebook.exportWord') }}</span>
        </button>
      </template>

      <template v-else-if="node.nodeType === 'FOLDER'">
        <button type="button" class="note-tree-context-menu__item" @click="emitAction('pin')">
          <span>{{ t('notebook.pin') }}</span>
          <kbd>Ctrl+Shift+T</kbd>
        </button>
        <button type="button" class="note-tree-context-menu__item" @click="emitAction('rename')">
          <span>{{ t('notebook.renameFolder') }}</span>
        </button>
        <button type="button" class="note-tree-context-menu__item" @click="emitAction('move')">
          <span>{{ t('notebook.moveTo') }}</span>
          <kbd>Ctrl+Alt+M</kbd>
        </button>
        <div class="note-tree-context-menu__divider" />
        <button
          type="button"
          class="note-tree-context-menu__item is-disabled"
          disabled
          :title="t('notebook.folderFavoriteUnsupported')"
        >
          <span>{{ t('notebook.favorite') }}</span>
          <kbd>Ctrl+Shift+C</kbd>
        </button>
        <button type="button" class="note-tree-context-menu__item" @click="emitAction('copy')">
          <span>{{ t('notebook.copy') }}</span>
          <kbd>Ctrl+C</kbd>
        </button>
        <button
          type="button"
          class="note-tree-context-menu__item"
          :class="{ 'is-disabled': !canPaste }"
          :disabled="!canPaste"
          @click="emitAction('paste')"
        >
          <span>{{ t('notebook.paste') }}</span>
          <kbd>Ctrl+V</kbd>
        </button>
        <div class="note-tree-context-menu__divider" />
        <button type="button" class="note-tree-context-menu__item is-danger" @click="emitAction('delete')">
          <span>{{ t('notebook.deleteFolder') }}</span>
        </button>
      </template>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { NbTreeNode } from '@/api/notebook'

export type TreeContextMenuAction =
  | 'pin'
  | 'favorite'
  | 'rename'
  | 'move'
  | 'copy'
  | 'paste'
  | 'delete'
  | 'exportWord'

const props = defineProps<{
  visible: boolean
  x: number
  y: number
  node: NbTreeNode | null
  canPaste: boolean
}>()

const emit = defineEmits<{
  action: [action: TreeContextMenuAction]
  close: []
}>()

const { t } = useI18n()
const menuRef = ref<HTMLElement | null>(null)
const x = ref(props.x)
const y = ref(props.y)

watch(
  () => [props.visible, props.x, props.y] as const,
  async ([visible, posX, posY]) => {
    x.value = posX
    y.value = posY
    if (!visible) return
    await nextTick()
    const menu = menuRef.value
    if (!menu) return
    const rect = menu.getBoundingClientRect()
    const maxX = window.innerWidth - rect.width - 8
    const maxY = window.innerHeight - rect.height - 8
    x.value = Math.max(8, Math.min(posX, maxX))
    y.value = Math.max(8, Math.min(posY, maxY))
  },
  { immediate: true },
)

function emitAction(action: TreeContextMenuAction) {
  if (action === 'paste' && !props.canPaste) return
  emit('action', action)
  emit('close')
}
</script>

<style scoped lang="scss">
.note-tree-context-menu {
  position: fixed;
  z-index: 4000;
  min-width: 220px;
  padding: 6px 0;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  box-shadow: var(--el-box-shadow-light);
}

.note-tree-context-menu__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  width: 100%;
  padding: 8px 16px;
  border: none;
  background: transparent;
  font-size: 14px;
  color: var(--el-text-color-primary);
  cursor: pointer;
  text-align: left;

  &:hover:not(:disabled) {
    background: var(--el-fill-color-light);
  }

  &.is-danger {
    color: var(--el-color-danger);
  }

  &.is-disabled,
  &:disabled {
    color: var(--el-text-color-disabled);
    cursor: not-allowed;
  }

  kbd {
    font-family: inherit;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }
}

.note-tree-context-menu__divider {
  height: 1px;
  margin: 4px 0;
  background: var(--el-border-color-lighter);
}
</style>
