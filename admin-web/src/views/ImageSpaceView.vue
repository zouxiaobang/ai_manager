<template>
  <div class="image-space war-room-page">
    <header class="image-space__header">
      <div>
        <h1 class="image-space__title">{{ t('imageSpace.title') }}</h1>
        <p class="image-space__subtitle">{{ t('imageSpace.subtitle') }}</p>
      </div>
      <el-button :loading="loading" @click="reloadAll">{{ t('imageSpace.refresh') }}</el-button>
    </header>

    <div class="image-space__layout war-room-panel">
      <aside class="image-space__tree">
        <h2 class="image-space__tree-title">{{ t('imageSpace.categoryTitle') }}</h2>
        <div v-loading="categoriesLoading" class="image-space__tree-body">
          <section v-for="zone in categories" :key="zone.id" class="image-space-zone">
            <button
              type="button"
              class="image-space-zone__head"
              :class="{ 'is-active': selectedZone === zone.id }"
              @click="selectZone(zone)"
            >
              <span>{{ zone.label }}</span>
              <el-icon :class="{ 'is-expanded': expandedZones.has(zone.id) }">
                <ArrowDown />
              </el-icon>
            </button>
            <div v-if="expandedZones.has(zone.id)" class="image-space-zone__children">
              <button
                v-for="child in zone.children || []"
                :key="child.id"
                type="button"
                class="image-space-zone__item"
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

      <main class="image-space__main">
        <div class="image-space__breadcrumb">{{ breadcrumb }}</div>
        <div class="image-space__toolbar">
          <el-input
            v-model="keyword"
            clearable
            :placeholder="t('imageSpace.searchPlaceholder')"
            class="image-space__search"
            @clear="reloadImages"
            @keyup.enter="reloadImages"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button @click="reloadImages">{{ t('imageSpace.search') }}</el-button>
        </div>

        <div v-loading="loading" class="image-space__grid-wrap">
          <div v-if="!loading && images.length === 0" class="image-space__empty">
            {{ t('imageSpace.empty') }}
          </div>
          <div v-else class="image-space__grid">
            <button
              v-for="item in images"
              :key="`${item.zone}:${item.relativePath}`"
              type="button"
              class="image-space-card"
              :class="{
                'is-selected':
                  selectedImage?.relativePath === item.relativePath &&
                  selectedImage?.zone === item.zone,
              }"
              @click="selectImage(item)"
            >
              <el-image
                :src="getStorageImageUrl(item.zone, item.relativePath)"
                :preview-src-list="[getStorageImageUrl(item.zone, item.relativePath)]"
                :alt="item.fileName"
                preview-teleported
                hide-on-click-modal
                fit="cover"
                class="image-space-card__thumb"
              />
              <span class="image-space-card__name" :title="item.fileName">{{ item.fileName }}</span>
            </button>
          </div>
        </div>

        <div v-if="total > pageSize" class="image-space__pager">
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            layout="prev, pager, next"
            :total="total"
            @current-change="loadImages"
          />
        </div>
      </main>

      <aside class="image-space__detail">
        <h2 class="image-space__detail-title">{{ t('imageSpace.detailTitle') }}</h2>
        <div v-if="!detail" class="image-space__detail-empty">
          {{ t('imageSpace.detailEmpty') }}
        </div>
        <template v-else>
          <div class="image-space-detail">
            <div class="image-space-detail__preview">
            <el-image
              :src="getStorageImageUrl(detail.zone, detail.relativePath)"
              :preview-src-list="[getStorageImageUrl(detail.zone, detail.relativePath)]"
              :alt="detail.fileName"
              preview-teleported
              hide-on-click-modal
              fit="contain"
              class="image-space-detail__preview-image"
            />
          </div>

            <div class="image-space-detail__field">
            <label>{{ t('imageSpace.fileNameLabel') }}</label>
            <div class="image-space-detail__name-row">
              <div class="image-space-detail__name-input-row">
                <el-input
                  v-model="renameBase"
                  :placeholder="t('imageSpace.fileNamePlaceholder')"
                  @input="onRenameBaseInput"
                />
                <span v-if="renameExtension" class="image-space-detail__ext">{{ renameExtension }}</span>
              </div>
              <el-button
                type="primary"
                class="image-space-detail__save"
                :loading="renaming"
                :disabled="!canSaveRename"
                @click="saveRename"
              >
                {{ t('imageSpace.saveRename') }}
              </el-button>
            </div>
            <p
              v-if="nameCheckMessage"
              class="image-space-detail__name-hint"
              :class="{ 'is-error': nameCheckAvailable === false }"
            >
              {{ nameCheckMessage }}
            </p>
          </div>

          <dl class="image-space-detail__meta">
            <div>
              <dt>{{ t('imageSpace.sizeLabel') }}</dt>
              <dd>{{ formatStorageBytes(detail.sizeBytes) }}</dd>
            </div>
            <div>
              <dt>{{ t('imageSpace.updatedAtLabel') }}</dt>
              <dd>{{ detail.modifiedAt || '—' }}</dd>
            </div>
            <div>
              <dt>{{ t('imageSpace.referenceCountLabel') }}</dt>
              <dd>{{ detail.referenceCount }}</dd>
            </div>
          </dl>

          <div class="image-space-detail__delete-section">
            <el-button
              type="danger"
              plain
              class="image-space-detail__delete"
              :loading="deleting"
              :disabled="detail.referenceCount > 0"
              @click="confirmDelete"
            >
              <el-icon><Delete /></el-icon>
              {{ t('imageSpace.deleteImage') }}
            </el-button>
            <p v-if="detail.referenceCount > 0" class="image-space-detail__delete-hint">
              {{ t('imageSpace.deleteBlockedHint') }}
            </p>
          </div>

          <div v-if="detail.linkedSpuNames.length" class="image-space-detail__tags">
            <span class="image-space-detail__tags-label">{{ t('imageSpace.linkedSpu') }}</span>
            <el-tag
              v-for="name in detail.linkedSpuNames"
              :key="name"
              size="small"
              effect="plain"
              round
            >
              {{ name }}
            </el-tag>
          </div>

          <ul v-if="detail.referenceHints.length" class="image-space-detail__refs">
            <li v-for="hint in detail.referenceHints" :key="hint">{{ hint }}</li>
          </ul>
          </div>
        </template>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Delete, Search } from '@element-plus/icons-vue'
import {
  checkImageSpaceFileName,
  deleteImageSpaceFile,
  fetchImageSpaceCategories,
  fetchImageSpaceDetail,
  fetchImageSpaceImages,
  renameImageSpaceFile,
  type ImageSpaceCategoryNode,
  type ImageSpaceImageDetail,
  type ImageSpaceImageItem,
} from '@/api/imageSpace'
import { formatStorageBytes } from '@/api/storageCenter'
import { getStorageImageUrl, type StorageImageZone } from '@/api/storageImage'

const { t } = useI18n()

const categoriesLoading = ref(false)
const loading = ref(false)
const renaming = ref(false)
const deleting = ref(false)
const categories = ref<ImageSpaceCategoryNode[]>([])
const images = ref<ImageSpaceImageItem[]>([])
const detail = ref<ImageSpaceImageDetail | null>(null)
const selectedImage = ref<ImageSpaceImageItem | null>(null)

const selectedZone = ref<StorageImageZone>('ECOMMERCE_IMAGES')
const selectedCategoryId = ref('all')
const selectedCategoryLabel = ref('')
const selectedZoneLabel = ref('')
const expandedZones = ref(new Set<string>(['ECOMMERCE_IMAGES']))

const keyword = ref('')
const page = ref(1)
const pageSize = 10
const total = ref(0)

const renameBase = ref('')
const renameExtension = ref('')
const nameCheckMessage = ref('')
const nameCheckAvailable = ref<boolean | null>(null)
let nameCheckTimer: ReturnType<typeof setTimeout> | null = null

const renameFullName = computed(() => `${renameBase.value.trim()}${renameExtension.value}`)

const breadcrumb = computed(() => {
  const parts = [t('imageSpace.title'), selectedZoneLabel.value, selectedCategoryLabel.value].filter(
    Boolean,
  )
  return parts.join(' / ')
})

const canSaveRename = computed(() => {
  if (!detail.value) return false
  if (renameFullName.value === detail.value.fileName) return false
  return nameCheckAvailable.value === true
})

function splitImageFileName(fileName: string): { base: string; ext: string } {
  const idx = fileName.lastIndexOf('.')
  if (idx <= 0) {
    return { base: fileName, ext: '' }
  }
  return { base: fileName.slice(0, idx), ext: fileName.slice(idx) }
}

function applyRenameParts(fileName: string) {
  const parts = splitImageFileName(fileName)
  renameBase.value = parts.base
  renameExtension.value = parts.ext
}

function onRenameBaseInput() {
  renameBase.value = renameBase.value.replace(/[\\/]/g, '').replace(/\./g, '')
  scheduleNameCheck()
}

async function loadCategories() {
  categoriesLoading.value = true
  try {
    categories.value = await fetchImageSpaceCategories()
    const zone = categories.value.find((item) => item.id === selectedZone.value)
    if (zone) {
      selectedZoneLabel.value = zone.label
      const child =
        zone.children?.find((item) => item.id === selectedCategoryId.value) || zone.children?.[0]
      if (child) {
        selectedCategoryId.value = child.id
        selectedCategoryLabel.value = child.label
      }
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
  page.value = 1
  selectedImage.value = null
  detail.value = null
  void loadImages()
}

async function loadImages() {
  loading.value = true
  try {
    const result = await fetchImageSpaceImages({
      zone: selectedZone.value,
      categoryId: selectedCategoryId.value,
      keyword: keyword.value,
      page: page.value,
      pageSize,
    })
    images.value = result.records || []
    total.value = result.total || 0
  } finally {
    loading.value = false
  }
}

async function selectImage(item: ImageSpaceImageItem) {
  selectedImage.value = item
  detail.value = await fetchImageSpaceDetail(item.zone, item.relativePath)
  applyRenameParts(detail.value.fileName)
  nameCheckMessage.value = ''
  nameCheckAvailable.value = null
}

function scheduleNameCheck() {
  if (nameCheckTimer) clearTimeout(nameCheckTimer)
  nameCheckTimer = setTimeout(() => {
    void runNameCheck()
  }, 300)
}

async function runNameCheck() {
  if (!detail.value) return
  const next = renameFullName.value
  if (!next) {
    nameCheckMessage.value = t('imageSpace.nameRequired')
    nameCheckAvailable.value = false
    return
  }
  if (next === detail.value.fileName) {
    nameCheckMessage.value = ''
    nameCheckAvailable.value = null
    return
  }
  const result = await checkImageSpaceFileName(detail.value.zone, detail.value.relativePath, next)
  nameCheckMessage.value = result.message
  nameCheckAvailable.value = result.available
}

async function saveRename() {
  if (!detail.value || !canSaveRename.value) return
  renaming.value = true
  try {
    detail.value = await renameImageSpaceFile({
      zone: detail.value.zone,
      relativePath: detail.value.relativePath,
      newFileName: renameFullName.value,
    })
    applyRenameParts(detail.value.fileName)
    ElMessage.success(t('imageSpace.renameSuccess'))
    await loadImages()
    const refreshed = images.value.find(
      (item) => item.relativePath === detail.value?.relativePath,
    )
    if (refreshed) {
      selectedImage.value = refreshed
    }
  } catch {
    ElMessage.error(t('imageSpace.renameFailed'))
  } finally {
    renaming.value = false
  }
}

async function confirmDelete() {
  if (!detail.value || detail.value.referenceCount > 0) return
  try {
    await ElMessageBox.confirm(
      t('imageSpace.deleteConfirm', { name: detail.value.fileName }),
      { type: 'warning' },
    )
  } catch {
    return
  }
  deleting.value = true
  try {
    await deleteImageSpaceFile(detail.value.zone, detail.value.relativePath)
    ElMessage.success(t('imageSpace.deleteSuccess'))
    selectedImage.value = null
    detail.value = null
    await loadImages()
  } catch {
    ElMessage.error(t('imageSpace.deleteFailed'))
  } finally {
    deleting.value = false
  }
}

function reloadImages() {
  page.value = 1
  void loadImages()
}

async function reloadAll() {
  await loadCategories()
  await loadImages()
  if (selectedImage.value && detail.value) {
    await selectImage(selectedImage.value)
  }
}

onMounted(() => {
  void reloadAll()
})

onBeforeUnmount(() => {
  if (nameCheckTimer) clearTimeout(nameCheckTimer)
})
</script>

<style scoped lang="scss">
.image-space {
  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 20px;
  }

  &__title {
    margin: 0;
    font-size: 24px;
    font-weight: 700;
    color: #1a1a1a;
  }

  &__subtitle {
    margin: 6px 0 0;
    font-size: 13px;
    color: var(--wr-muted);
  }

  &__layout {
    display: grid;
    grid-template-columns: 220px minmax(0, 1fr) 280px;
    gap: 0;
    min-height: 640px;
    padding: 0;
    overflow: hidden;
  }

  &__tree,
  &__main,
  &__detail {
    min-height: 0;
    padding: 16px;
  }

  &__tree {
    border-right: 1px solid #eef2f7;
    background: #fafbfc;
  }

  &__tree-title,
  &__detail-title {
    margin: 0 0 12px;
    font-size: 13px;
    font-weight: 700;
    color: #374151;
  }

  &__main {
    display: flex;
    flex-direction: column;
    gap: 12px;
    min-width: 0;
  }

  &__detail {
    border-left: 1px solid #eef2f7;
    background: #fafbfc;
    display: flex;
    flex-direction: column;
  }

  &__breadcrumb {
    font-size: 12px;
    color: #6b7280;
  }

  &__toolbar {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  &__search {
    flex: 1;
    min-width: 0;
  }

  &__grid-wrap {
    flex: 1;
    min-height: 280px;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 12px;
  }

  &__empty,
  &__detail-empty {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 240px;
    color: #9ca3af;
    font-size: 13px;
  }

  &__pager {
    display: flex;
    justify-content: center;
  }
}

.image-space-zone {
  & + & {
    margin-top: 10px;
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding: 8px 10px;
    border: none;
    border-radius: 8px;
    background: transparent;
    font-size: 13px;
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
    gap: 4px;
    margin-top: 4px;
    padding-left: 8px;
  }

  &__item {
    width: 100%;
    padding: 7px 10px;
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

.image-space-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  text-align: left;

  &:hover {
    border-color: #86efac;
  }

  &.is-selected {
    border-color: #15803d;
    box-shadow: 0 0 0 3px rgb(21 128 61 / 12%);
  }

  &__thumb {
    width: 100%;
    aspect-ratio: 1;
    border-radius: 6px;
    overflow: hidden;
    background: #f3f4f6;
    cursor: zoom-in;

    :deep(.el-image__inner) {
      width: 100%;
      height: 100%;
    }
  }

  &__name {
    font-size: 11px;
    color: #6b7280;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.image-space-detail {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;

  &__preview {
    margin-bottom: 14px;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    overflow: hidden;
    background: #fff;
  }

  &__preview-image {
    display: block;
    width: 100%;
    max-height: 180px;
    cursor: zoom-in;
    background: #f8fafc;

    :deep(.el-image__inner) {
      width: 100%;
      max-height: 180px;
      object-fit: contain;
    }
  }

  &__name-row {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__name-input-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__name-input-row .el-input {
    flex: 1;
    min-width: 0;
  }

  &__ext {
    flex-shrink: 0;
    padding: 0 10px;
    height: 32px;
    line-height: 32px;
    border: 1px solid var(--el-border-color);
    border-radius: 6px;
    background: #f3f4f6;
    font-size: 13px;
    font-weight: 600;
    color: #6b7280;
  }

  &__name-input-row :deep(.el-input__wrapper) {
    border-radius: 6px;
  }

  &__save {
    align-self: flex-end;
  }

  &__field {
    margin-bottom: 14px;

    label {
      display: block;
      margin-bottom: 6px;
      font-size: 12px;
      color: #6b7280;
    }
  }

  &__name-hint {
    margin: 6px 0 0;
    font-size: 12px;
    color: #15803d;

    &.is-error {
      color: #dc2626;
      font-weight: 600;
    }
  }

  &__meta {
    display: grid;
    gap: 14px;
    margin: 0 0 12px;

    div {
      display: grid;
      grid-template-columns: 72px 1fr;
      gap: 8px;
      font-size: 12px;
      line-height: 1.75;
    }

    dt {
      margin: 0;
      color: #9ca3af;
    }

    dd {
      margin: 0;
      color: #374151;
      font-weight: 600;
      word-break: break-word;
    }
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
    margin-bottom: 16px;
    line-height: 1.75;
  }

  &__tags-label {
    font-size: 12px;
    color: #6b7280;
  }

  &__refs {
    margin: 0 0 18px;
    padding-left: 18px;
    font-size: 12px;
    line-height: 2;
    color: #6b7280;

    li + li {
      margin-top: 8px;
    }
  }

  &__delete-section {
    margin-bottom: 16px;
  }

  &__delete {
    width: 100%;

    .el-icon {
      margin-right: 4px;
    }
  }

  &__delete-hint {
    margin: 8px 0 0;
    font-size: 12px;
    line-height: 1.5;
    color: #9ca3af;
    text-align: center;
  }
}

@media (max-width: 1200px) {
  .image-space__layout {
    grid-template-columns: 1fr;
  }

  .image-space__tree,
  .image-space__detail {
    border: none;
    border-bottom: 1px solid #eef2f7;
  }

  .image-space__grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
