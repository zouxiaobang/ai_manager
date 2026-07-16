<template>
  <div class="library-toolbar">
    <div class="library-toolbar__top">
      <div class="library-toolbar__breadcrumb">
        <el-breadcrumb>
          <el-breadcrumb-item>{{ t('library.library') }}</el-breadcrumb-item>
          <el-breadcrumb-item v-if="folderName">{{ folderName }}</el-breadcrumb-item>
        </el-breadcrumb>
        <span class="library-toolbar__count">{{ t('library.fileCount', { count: fileCount }) }}</span>
      </div>

      <div class="library-toolbar__actions">
        <el-input
          v-model="searchKeyword"
          :placeholder="t('library.searchPlaceholder')"
          :prefix-icon="Search"
          clearable
          class="library-toolbar__search"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />

        <el-button-group class="library-toolbar__view-switch">
          <el-tooltip :content="t('library.viewGrid')" placement="top">
            <el-button
              :type="viewMode === 'grid' ? 'primary' : 'default'"
              @click="emit('view-mode-change', 'grid')"
            >
              <el-icon><Grid /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip :content="t('library.viewList')" placement="top">
            <el-button
              :type="viewMode === 'list' ? 'primary' : 'default'"
              @click="emit('view-mode-change', 'list')"
            >
              <el-icon><List /></el-icon>
            </el-button>
          </el-tooltip>
        </el-button-group>

        <el-dropdown trigger="click" @command="onSortCommand">
          <el-button class="library-toolbar__sort-btn">
            {{ t('library.sortBy') }}
            <el-icon><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="opt in sortOptions"
                :key="opt.field"
                :command="opt.field"
                :class="{ 'is-active': sortField === opt.field }"
              >
                <span :class="{ 'is-selected': sortField === opt.field }">{{ t(opt.labelKey) }}</span>
                <el-icon v-if="sortField === opt.field" class="library-toolbar__sort-check">
                  <Check />
                </el-icon>
              </el-dropdown-item>
              <el-dropdown-item divided>
            <div class="library-toolbar__sort-order">
              <el-switch
                :model-value="sortOrder === 'desc'"
                :active-text="t('library.desc')"
                :inactive-text="t('library.asc')"
                inline-prompt
                :active-icon="ArrowDown"
                :inactive-icon="ArrowUp"
                style="--el-switch-on-color: #2e7d32; --el-switch-off-color: #b71c1c"
                @change="onOrderChange"
              />
            </div>
          </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-button type="primary" @click="emit('upload-click')">
          <el-icon><Upload /></el-icon>
          <span>{{ t('library.upload') }}</span>
        </el-button>
      </div>
    </div>

    <Transition name="batch-bar">
      <div v-if="selectedCount > 0" class="library-toolbar__batch">
        <el-checkbox
          :model-value="allSelected"
          :indeterminate="indeterminate"
          @change="onSelectAll"
        >
          {{ t('library.selectedCount', { count: selectedCount }) }}
        </el-checkbox>
        <div class="library-toolbar__batch-actions">
          <el-button size="small" @click="emit('delete-selected')">
            <el-icon><Delete /></el-icon>
            {{ t('library.batchDelete') }}
          </el-button>
          <el-button size="small" @click="emit('move-selected')">
            <el-icon><FolderOpened /></el-icon>
            {{ t('library.batchMove') }}
          </el-button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowDown, ArrowUp, Check, Delete, FolderOpened, Grid, List, Search, Upload } from '@element-plus/icons-vue'
import { useLibraryStore } from '@/stores/library'
import { storeToRefs } from 'pinia'
import type { SortField, SortOrder } from '@/api/library/types'

defineProps<{
  folderName: string
  fileCount: number
}>()

const emit = defineEmits<{
  search: [keyword: string]
  'upload-click': []
  'delete-selected': []
  'move-selected': []
  'view-mode-change': [mode: 'grid' | 'list']
  'sort-change': [field: SortField, order: SortOrder]
}>()

const { t } = useI18n()
const store = useLibraryStore()
const { viewMode, sortField, sortOrder, selectedFiles } = storeToRefs(store)

const searchKeyword = ref('')

const selectedCount = computed(() => selectedFiles.value.size)
const allSelected = computed(() => false)
const indeterminate = computed(() => false)

const sortOptions = [
  { field: 'name' as SortField, labelKey: 'library.sortName' },
  { field: 'size' as SortField, labelKey: 'library.sortSize' },
  { field: 'updateTime' as SortField, labelKey: 'library.sortUpdateTime' },
]

function onSearch() {
  emit('search', searchKeyword.value)
}

function onSortCommand(field: SortField) {
  const order = sortField.value === field && sortOrder.value === 'asc' ? 'desc' : 'asc'
  emit('sort-change', field, order)
}

function onOrderChange(val: boolean) {
  emit('sort-change', sortField.value, val ? 'desc' : 'asc')
}

function onSelectAll(_val: boolean) {
}
</script>

<style scoped lang="scss">
.library-toolbar {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}

.library-toolbar__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  flex-wrap: wrap;
}

.library-toolbar__breadcrumb {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.library-toolbar__count {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.library-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.library-toolbar__search {
  width: 220px;
}

.library-toolbar__view-switch {
  flex-shrink: 0;
}

.library-toolbar__sort-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.library-toolbar__sort-check {
  margin-left: 8px;
  font-size: 14px;
  color: var(--el-color-success);
  font-weight: bold;
}

.library-toolbar__sort-order {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 0;

  .el-radio {
    display: flex;
    align-items: center;
    margin-right: 0;
    height: 32px;
  }
}

:deep(.el-dropdown-menu__item.is-active) {
  font-weight: bold;
  color: var(--el-color-success);
}

:deep(.el-dropdown-menu__item .is-selected) {
  font-weight: bold;
}

.library-toolbar__batch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 8px 16px;
  background: var(--el-color-primary-light-9);
  border-top: 1px solid var(--el-border-color-lighter);
}

.library-toolbar__batch-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-bar-enter-active,
.batch-bar-leave-active {
  transition: all 0.2s ease;
}

.batch-bar-enter-from,
.batch-bar-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
  overflow: hidden;
}
</style>
