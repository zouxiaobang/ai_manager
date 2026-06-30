<template>
  <div class="storage-image-picker">
    <div class="storage-image-picker__layout">
      <aside class="storage-image-picker__tree">
        <h3 class="storage-image-picker__tree-title">{{ t('imageSpace.categoryTitle') }}</h3>
        <div v-loading="categoriesLoading" class="storage-image-picker__tree-body">
          <section v-for="zone in visibleCategories" :key="zone.id" class="storage-image-picker-zone">
            <button
              type="button"
              class="storage-image-picker-zone__head"
              :class="{ 'is-active': selectedZone === zone.id }"
              @click="selectZone(zone)"
            >
              <span>{{ zone.label }}</span>
              <el-icon :class="{ 'is-expanded': expandedZones.has(zone.id) }">
                <ArrowDown />
              </el-icon>
            </button>
            <div v-if="expandedZones.has(zone.id)" class="storage-image-picker-zone__children">
              <button
                v-for="child in zone.children || []"
                :key="child.id"
                type="button"
                class="storage-image-picker-zone__item"
                :class="{
                  'is-active': selectedZone === zone.id && selectedCategoryId === child.id,
                }"
                @click="selectCategory(zone, child)"
              >
                {{ child.label }}
              </button>
            </div>
          </section>
        </div>
      </aside>

      <main class="storage-image-picker__main">
        <div v-if="breadcrumb" class="storage-image-picker__breadcrumb">{{ breadcrumb }}</div>
        <div class="storage-image-picker__toolbar">
          <el-input
            v-model="keyword"
            clearable
            :placeholder="t('imagePicker.searchPlaceholder')"
            class="storage-image-picker__search"
            @clear="reloadFromFirst"
            @keyup.enter="reloadFromFirst"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div v-loading="loading" class="storage-image-picker__body">
          <div v-if="!loading && items.length === 0" class="storage-image-picker__empty">
            {{ t('imagePicker.empty') }}
          </div>
          <div v-else class="storage-image-picker__grid">
            <button
              v-for="item in items"
              :key="`${item.zone}:${item.relativePath}`"
              type="button"
              class="storage-image-picker__card"
              :class="{ 'is-selected': isSelected(item) }"
              @click="selectItem(item)"
            >
              <img
                :src="getStorageImageUrl(item.zone, resolveStorageImageValue(item))"
                :alt="item.fileName"
                class="storage-image-picker__thumb"
                loading="lazy"
              />
              <span class="storage-image-picker__name" :title="item.fileName">{{ item.fileName }}</span>
            </button>
          </div>
        </div>

        <div v-if="total > pageSize" class="storage-image-picker__pager">
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            layout="prev, pager, next"
            :total="total"
            @current-change="loadItems"
          />
        </div>

        <div class="storage-image-picker__footer-link">
          <router-link
            :to="{ name: 'image-space' }"
            class="storage-image-picker__space-link"
            @click="onOpenImageSpace"
          >
            {{ t('imagePicker.openImageSpace') }}
            <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowDown, ArrowRight, Search } from '@element-plus/icons-vue'
import {
  fetchImageSpaceCategories,
  fetchImageSpaceImages,
  type ImageSpaceCategoryNode,
} from '@/api/imageSpace'
import {
  getStorageImageUrl,
  resolveStorageImageValue,
  type StorageImageItem,
  type StorageImageZone,
} from '@/api/storageImage'

export type ImagePickerScope = 'ecommerce' | 'notebook'

const props = withDefaults(
  defineProps<{
    scope?: ImagePickerScope
    modelValue?: StorageImageItem | null
  }>(),
  {
    scope: 'ecommerce',
    modelValue: null,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: StorageImageItem | null]
  openImageSpace: []
}>()

const { t } = useI18n()

const loading = ref(false)
const categoriesLoading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = 10
const total = ref(0)
const items = ref<StorageImageItem[]>([])
const categories = ref<ImageSpaceCategoryNode[]>([])
const expandedZones = ref(new Set<string>())

const selectedZone = ref<StorageImageZone>(
  props.scope === 'notebook' ? 'NOTEBOOK_IMAGES' : 'ECOMMERCE_IMAGES',
)
const selectedCategoryId = ref('all')
const selectedZoneLabel = ref('')
const selectedCategoryLabel = ref('')

const scopeZone = computed<StorageImageZone>(() =>
  props.scope === 'notebook' ? 'NOTEBOOK_IMAGES' : 'ECOMMERCE_IMAGES',
)

const visibleCategories = computed(() =>
  categories.value.filter((zone) => zone.id === scopeZone.value),
)

const breadcrumb = computed(() => {
  const parts = [selectedZoneLabel.value, selectedCategoryLabel.value].filter(Boolean)
  return parts.join(' / ')
})

function isSelected(item: StorageImageItem) {
  if (!props.modelValue) return false
  return (
    props.modelValue.zone === item.zone &&
    resolveStorageImageValue(props.modelValue) === resolveStorageImageValue(item)
  )
}

function selectItem(item: StorageImageItem) {
  emit('update:modelValue', item)
}

function onOpenImageSpace() {
  emit('openImageSpace')
}

async function loadCategories() {
  categoriesLoading.value = true
  try {
    categories.value = await fetchImageSpaceCategories()
    const zone = visibleCategories.value[0]
    if (!zone) return
    expandedZones.value.add(zone.id)
    selectedZone.value = zone.id as StorageImageZone
    selectedZoneLabel.value = zone.label
    const child =
      zone.children?.find((item) => item.id === selectedCategoryId.value) || zone.children?.[0]
    if (child) {
      selectedCategoryId.value = child.id
      selectedCategoryLabel.value = child.label
    }
  } finally {
    categoriesLoading.value = false
  }
}

function selectZone(zone: ImageSpaceCategoryNode) {
  if (expandedZones.value.has(zone.id)) {
    expandedZones.value.delete(zone.id)
    return
  }
  expandedZones.value.add(zone.id)
  selectedZone.value = zone.id as StorageImageZone
  selectedZoneLabel.value = zone.label
  const first = zone.children?.[0]
  if (first) {
    selectCategory(zone, first)
  }
}

function selectCategory(zone: ImageSpaceCategoryNode, child: ImageSpaceCategoryNode) {
  selectedZone.value = zone.id as StorageImageZone
  selectedZoneLabel.value = zone.label
  selectedCategoryId.value = child.id
  selectedCategoryLabel.value = child.label
  expandedZones.value.add(zone.id)
  emit('update:modelValue', null)
  reloadFromFirst()
}

async function loadItems() {
  loading.value = true
  try {
    const result = await fetchImageSpaceImages({
      zone: selectedZone.value,
      categoryId: selectedCategoryId.value,
      keyword: keyword.value,
      page: page.value,
      pageSize,
    })
    items.value = (result.records || []).map((item) => ({
      zone: item.zone,
      fileName: item.fileName,
      relativePath: item.relativePath,
      sizeBytes: item.sizeBytes,
      modifiedAt: item.modifiedAt,
    }))
    total.value = result.total || 0
  } finally {
    loading.value = false
  }
}

async function reloadFromFirst() {
  page.value = 1
  await loadItems()
}

async function initPanel() {
  selectedZone.value = scopeZone.value
  selectedCategoryId.value = 'all'
  selectedZoneLabel.value = ''
  selectedCategoryLabel.value = ''
  expandedZones.value = new Set([scopeZone.value])
  emit('update:modelValue', null)
  await loadCategories()
  await reloadFromFirst()
}

watch(
  () => props.scope,
  () => {
    void initPanel()
  },
  { immediate: true },
)

defineExpose({ reload: reloadFromFirst })
</script>

<style scoped lang="scss">
.storage-image-picker {
  min-height: 380px;
}

.storage-image-picker__layout {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 14px;
  min-height: 380px;
}

.storage-image-picker__tree {
  min-height: 0;
  padding: 10px;
  border: 1px solid #eef2f7;
  border-radius: 10px;
  background: #fafbfc;
}

.storage-image-picker__tree-title {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 700;
  color: #374151;
}

.storage-image-picker__tree-body {
  min-height: 120px;
}

.storage-image-picker-zone {
  & + & {
    margin-top: 8px;
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding: 7px 8px;
    border: none;
    border-radius: 8px;
    background: transparent;
    font-size: 12px;
    font-weight: 600;
    color: #374151;
    cursor: pointer;

    &.is-active,
    &:hover {
      background: #f0fdf4;
      color: #15803d;
    }

    .el-icon {
      transition: transform 0.15s ease;

      &.is-expanded {
        transform: rotate(180deg);
      }
    }
  }

  &__children {
    display: flex;
    flex-direction: column;
    gap: 3px;
    margin-top: 3px;
    padding-left: 6px;
  }

  &__item {
    width: 100%;
    padding: 6px 8px;
    border: none;
    border-radius: 8px;
    background: transparent;
    text-align: left;
    font-size: 12px;
    color: #4b5563;
    cursor: pointer;

    &.is-active,
    &:hover {
      background: #ecfdf5;
      color: #15803d;
      font-weight: 600;
    }
  }
}

.storage-image-picker__main {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  min-height: 0;
}

.storage-image-picker__breadcrumb {
  font-size: 12px;
  color: #6b7280;
}

.storage-image-picker__toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.storage-image-picker__search {
  flex: 1;
  min-width: 0;
}

.storage-image-picker__body {
  flex: 1;
  min-height: 220px;
}

.storage-image-picker__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  color: #9ca3af;
  font-size: 13px;
}

.storage-image-picker__grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.storage-image-picker__card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    border-color: #86efac;
    box-shadow: 0 2px 8px rgb(21 128 61 / 8%);
  }

  &.is-selected {
    border-color: #15803d;
    box-shadow: 0 0 0 3px rgb(21 128 61 / 12%);
  }
}

.storage-image-picker__thumb {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 6px;
  background: #f3f4f6;
}

.storage-image-picker__name {
  font-size: 11px;
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.storage-image-picker__pager {
  display: flex;
  justify-content: center;
}

.storage-image-picker__footer-link {
  display: flex;
  justify-content: center;
  padding-top: 2px;
}

.storage-image-picker__space-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
  color: #15803d;
  text-decoration: none;

  &:hover {
    color: #166534;
    text-decoration: underline;
  }
}

@media (max-width: 900px) {
  .storage-image-picker__layout {
    grid-template-columns: 1fr;
  }

  .storage-image-picker__grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
