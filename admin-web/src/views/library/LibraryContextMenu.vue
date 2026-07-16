<template>
  <Teleport to="body">
    <div
      v-if="visible"
      ref="menuRef"
      class="library-context-menu"
      :style="{ left: `${menuX}px`, top: `${menuY}px` }"
      @click.stop
      @contextmenu.prevent
    >
      <div class="library-context-menu__item" @click="emitAction('download')">
        <el-icon><Download /></el-icon>
        <span>{{ t('library.download') }}</span>
      </div>
      <div class="library-context-menu__divider" />
      <div class="library-context-menu__item" @click="emitAction('rename')">
        <el-icon><Edit /></el-icon>
        <span>{{ t('library.rename') }}</span>
      </div>
      <div class="library-context-menu__item" @click="emitAction('move')">
        <el-icon><FolderOpened /></el-icon>
        <span>{{ t('library.move') }}</span>
      </div>
      <div class="library-context-menu__divider" />
      <div class="library-context-menu__item" @click="emitAction('pin')">
        <el-icon><Star /></el-icon>
        <span>{{ file?.isPinned ? t('library.unpin') : t('library.pin') }}</span>
      </div>
      <div class="library-context-menu__item" @click="emitAction('tag')">
        <el-icon><CollectionTag /></el-icon>
        <span>{{ t('library.tag') }}</span>
      </div>
      <div class="library-context-menu__item" @click="emitAction('kb')">
        <el-icon><Reading /></el-icon>
        <span>{{ t('library.kbMark') }}</span>
      </div>
      <div class="library-context-menu__divider" />
      <div class="library-context-menu__item is-danger" @click="emitAction('delete')">
        <el-icon><Delete /></el-icon>
        <span>{{ t('library.delete') }}</span>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Download, Edit, FolderOpened, Star, CollectionTag, Reading, Delete } from '@element-plus/icons-vue'
import type { DocFile } from '@/api/library/types'

const props = defineProps<{
  x: number
  y: number
  file: DocFile | null
}>()

const emit = defineEmits<{
  close: []
  action: [action: string]
}>()

const { t } = useI18n()
const menuRef = ref<HTMLElement | null>(null)
const menuX = ref(props.x)
const menuY = ref(props.y)

const visible = computed(() => !!props.file)

watch(
  () => [props.x, props.y, props.file] as const,
  async ([posX, posY]) => {
    menuX.value = posX
    menuY.value = posY
    await nextTick()
    const menu = menuRef.value
    if (!menu) return
    const rect = menu.getBoundingClientRect()
    const maxX = window.innerWidth - rect.width - 8
    const maxY = window.innerHeight - rect.height - 8
    menuX.value = Math.max(8, Math.min(posX, maxX))
    menuY.value = Math.max(8, Math.min(posY, maxY))
  },
  { immediate: true },
)

function emitAction(action: string) {
  emit('action', action)
  emit('close')
}
</script>

<style scoped lang="scss">
.library-context-menu {
  position: fixed;
  z-index: 4000;
  min-width: 180px;
  padding: 6px 0;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  box-shadow: var(--el-box-shadow-light);
}

.library-context-menu__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  font-size: 14px;
  color: var(--el-text-color-primary);
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &.is-danger {
    color: var(--el-color-danger);

    &:hover {
      background: var(--el-color-danger-light-9);
    }
  }
}

.library-context-menu__divider {
  height: 1px;
  margin: 4px 0;
  background: var(--el-border-color-lighter);
}
</style>
