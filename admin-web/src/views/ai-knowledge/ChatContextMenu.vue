<template>
  <Teleport to="body">
    <!-- 聊天区右键菜单 -->
    <div
      v-if="visible"
      ref="menuRef"
      class="ak-ctx-menu"
      :style="{ left: `${x}px`, top: `${y}px` }"
      @click.stop
      @contextmenu.prevent
    >
      <button type="button" class="ak-ctx-menu__item" @click="emitAction('add-marker')">
        <svg class="ak-ctx-menu__icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
          <path d="M12 7v6M9 10h6" />
        </svg>
        <span>{{ t('aiKnowledge.chat.marker.addMarker') }}</span>
      </button>

      <button
        type="button"
        class="ak-ctx-menu__item ak-ctx-menu__item--expandable"
        :class="{ 'is-expanded': listExpanded }"
        @click="toggleList"
      >
        <svg class="ak-ctx-menu__icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
        </svg>
        <span>{{ t('aiKnowledge.chat.marker.marker') }}</span>
        <svg class="ak-ctx-menu__chevron" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline v-if="listExpanded" points="18 15 12 9 6 15" />
          <polyline v-else points="9 6 15 12 9 18" />
        </svg>
      </button>

      <!-- 标记列表子面板：紧贴「标记」选项下方展开（搜索 + 标记项，左键跳转 / 右键重命名、删除） -->
      <div v-if="listExpanded" class="ak-ctx-menu__panel">
        <el-input
          v-model="keyword"
          size="small"
          class="ak-ctx-menu__search"
          :placeholder="t('aiKnowledge.chat.marker.searchPlaceholder')"
          clearable
        />
        <div class="ak-ctx-menu__list">
          <div v-if="filteredMarkers.length === 0" class="ak-ctx-menu__empty">
            {{ markers.length === 0 ? t('aiKnowledge.chat.marker.empty') : t('aiKnowledge.chat.marker.searchEmpty') }}
          </div>
          <div
            v-for="m in filteredMarkers"
            :key="m.id"
            class="ak-ctx-menu__marker"
            :title="m.name"
            @click="emitAction('jump', m.id)"
            @contextmenu.prevent.stop="openSubMenu($event, m)"
          >
            <svg class="ak-ctx-menu__marker-icon" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
            </svg>
            <span class="ak-ctx-menu__marker-name">{{ m.name }}</span>
          </div>
        </div>
      </div>

      <!-- 回到上一个标签：当前视口上方最近的标记；无上一个时禁用 -->
      <button
        type="button"
        class="ak-ctx-menu__item ak-ctx-menu__item--jump-prev"
        :class="{ 'is-disabled': !canJumpPrevious }"
        :disabled="!canJumpPrevious"
        @click="emitAction('jump-prev')"
      >
        <svg class="ak-ctx-menu__icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
          <path d="M12 15V7M9 10l3-3 3 3" />
        </svg>
        <span>{{ t('aiKnowledge.chat.marker.jumpPrevious') }}</span>
      </button>

      <template v-if="markers.length > 0">
        <div class="ak-ctx-menu__divider" />
        <button type="button" class="ak-ctx-menu__item is-danger" @click="emitAction('delete-all')">
          <svg class="ak-ctx-menu__icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m3 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" />
          </svg>
          <span>{{ t('aiKnowledge.chat.marker.deleteAll') }}</span>
        </button>
      </template>
    </div>

    <!-- 标记子菜单：右键单个标记时的重命名/删除 -->
    <div
      v-if="subMenu.marker"
      class="ak-ctx-menu ak-ctx-menu--sub"
      :style="{ left: `${subMenu.x}px`, top: `${subMenu.y}px` }"
      @click.stop
      @contextmenu.prevent
    >
      <button type="button" class="ak-ctx-menu__item" @click="emitAction('rename', subMenu.marker!.id)">
        <svg class="ak-ctx-menu__icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
        </svg>
        <span>{{ t('aiKnowledge.chat.marker.rename') }}</span>
      </button>
      <button type="button" class="ak-ctx-menu__item is-danger" @click="emitAction('delete', subMenu.marker!.id)">
        <svg class="ak-ctx-menu__icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m3 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" />
        </svg>
        <span>{{ t('aiKnowledge.chat.marker.delete') }}</span>
      </button>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ChatBookmark } from '@/api/aiKnowledge'

const props = defineProps<{
  visible: boolean
  x: number
  y: number
  markers: ChatBookmark[]
  /** 当前滚动位置上方是否存在标记；控制「回到上一个标签」是否可点 */
  canJumpPrevious: boolean
}>()

const emit = defineEmits<{
  'add-marker': []
  jump: [id: string]
  rename: [id: string]
  delete: [id: string]
  'delete-all': []
  'jump-prev': []
  close: []
}>()

const { t } = useI18n()
const menuRef = ref<HTMLElement | null>(null)
const x = ref(props.x)
const y = ref(props.y)

/** 标记列表面板展开态与搜索词（每次打开重置） */
const listExpanded = ref(false)
const keyword = ref('')
/** 标记子菜单（右键单个标记弹出）位置与目标 */
const subMenu = reactive<{ marker: ChatBookmark | null; x: number; y: number }>({ marker: null, x: 0, y: 0 })

/** 按名称过滤标记（大小写不敏感），空关键词返回全量 */
const filteredMarkers = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return props.markers
  return props.markers.filter(m => m.name.toLowerCase().includes(kw))
})

// 打开时重置子状态并做视口边缘钳制（同 NoteTreeContextMenu）
watch(
  () => [props.visible, props.x, props.y] as const,
  async ([visible, posX, posY]) => {
    x.value = posX
    y.value = posY
    if (!visible) return
    listExpanded.value = false
    keyword.value = ''
    subMenu.marker = null
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

function toggleList() {
  listExpanded.value = !listExpanded.value
}

/** 在光标处弹出标记子菜单（重命名/删除），做视口钳制 */
function openSubMenu(event: MouseEvent, marker: ChatBookmark) {
  subMenu.marker = marker
  subMenu.x = event.clientX
  subMenu.y = event.clientY
  // 待子菜单渲染后钳制到视口内
  void nextTick(() => {
    // 子菜单尺寸未知，用固定估算值粗略钳制，避免被切出屏幕
    const W = 140
    const H = 88
    subMenu.x = Math.max(8, Math.min(subMenu.x, window.innerWidth - W - 8))
    subMenu.y = Math.max(8, Math.min(subMenu.y, window.innerHeight - H - 8))
  })
}

/**
 * 触发动作并关闭菜单（jump/rename/delete 携带标记 id）。
 * emit 是重载函数，switch 收敛出的联合类型无法赋值给单个重载签名，
 * 因此每个分支用字面量显式 emit，保证类型安全。
 */
function emitAction(action: 'add-marker' | 'jump' | 'rename' | 'delete' | 'delete-all' | 'jump-prev', id?: string) {
  switch (action) {
    case 'jump':
      emit('jump', id as string)
      break
    case 'rename':
      emit('rename', id as string)
      break
    case 'delete':
      emit('delete', id as string)
      break
    case 'add-marker':
      emit('add-marker')
      break
    case 'jump-prev':
      emit('jump-prev')
      break
    default:
      emit('delete-all')
  }
  emit('close')
}
</script>

<style scoped lang="scss">
.ak-ctx-menu {
  position: fixed;
  z-index: 4000;
  min-width: 200px;
  padding: 6px 0;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  box-shadow: var(--el-box-shadow-light);

  &--sub {
    min-width: 150px;
  }
}

.ak-ctx-menu__item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 14px;
  border: none;
  background: transparent;
  font-size: 14px;
  color: var(--el-text-color-primary);
  cursor: pointer;
  text-align: left;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &.is-danger {
    color: var(--el-color-danger);
  }

  &.is-disabled,
  &:disabled {
    color: var(--el-text-color-placeholder);
    cursor: not-allowed;

    &:hover {
      background: transparent;
    }
  }

  &--expandable {
    justify-content: flex-start;

    .ak-ctx-menu__chevron {
      margin-left: auto;
      color: var(--el-text-color-secondary);
      transition: transform 0.15s;
    }

    &.is-expanded {
      color: var(--el-color-primary);
    }
  }
}

.ak-ctx-menu__icon {
  flex-shrink: 0;
  color: currentColor;
}

.ak-ctx-menu__panel {
  padding: 6px 8px 8px;
}

.ak-ctx-menu__search {
  margin-bottom: 6px;
}

.ak-ctx-menu__list {
  max-height: 240px;
  overflow-y: auto;
}

.ak-ctx-menu__marker {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  font-size: 13px;
  color: var(--el-text-color-primary);
  cursor: pointer;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &-icon {
    flex-shrink: 0;
    color: var(--el-color-primary);
  }

  &-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.ak-ctx-menu__empty {
  padding: 10px 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.ak-ctx-menu__divider {
  height: 1px;
  margin: 4px 0;
  background: var(--el-border-color-lighter);
}
</style>
