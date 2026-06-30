<template>
  <el-dialog
    :model-value="visible"
    :title="t('storageCenter.orphanDialogTitle')"
    width="920px"
    append-to-body
    destroy-on-close
    class="storage-orphan-dialog"
    @update:model-value="emit('update:visible', $event)"
    @opened="loadPreview"
  >
    <div v-loading="loading" class="storage-orphan-dialog__body">
      <div v-if="preview" class="storage-orphan-dialog__summary-wrap">
        <div class="storage-orphan-dialog__summary">
          <div class="storage-orphan-dialog__summary-item is-orange">
            <div class="storage-orphan-dialog__summary-icon" aria-hidden="true">
              <el-icon><Delete /></el-icon>
            </div>
            <div class="storage-orphan-dialog__summary-content">
              <span class="storage-orphan-dialog__summary-label">{{ t('storageCenter.orphanTotalCount') }}</span>
              <div class="storage-orphan-dialog__summary-value">
                <span class="storage-orphan-dialog__summary-number">{{ preview.totalOrphanCount }}</span>
                <span class="storage-orphan-dialog__summary-unit">{{ t('storageCenter.unitCount') }}</span>
              </div>
            </div>
          </div>
          <div class="storage-orphan-dialog__summary-item is-green">
            <div class="storage-orphan-dialog__summary-icon" aria-hidden="true">
              <el-icon><Coin /></el-icon>
            </div>
            <div class="storage-orphan-dialog__summary-content">
              <span class="storage-orphan-dialog__summary-label">{{ t('storageCenter.orphanTotalFreed') }}</span>
              <div class="storage-orphan-dialog__summary-value">
                <span class="storage-orphan-dialog__summary-number">{{ freedSizeParts.value }}</span>
                <span class="storage-orphan-dialog__summary-unit">{{ freedSizeParts.unit }}</span>
              </div>
            </div>
          </div>
          <div class="storage-orphan-dialog__summary-item is-blue">
            <div class="storage-orphan-dialog__summary-icon" aria-hidden="true">
              <el-icon><Search /></el-icon>
            </div>
            <div class="storage-orphan-dialog__summary-content">
              <span class="storage-orphan-dialog__summary-label">{{ t('storageCenter.orphanTotalScanned') }}</span>
              <div class="storage-orphan-dialog__summary-value">
                <span class="storage-orphan-dialog__summary-number">{{ preview.totalScanned }}</span>
                <span class="storage-orphan-dialog__summary-unit">{{ t('storageCenter.unitCount') }}</span>
              </div>
            </div>
          </div>
        </div>
        <p class="storage-orphan-dialog__last-cleanup">
          {{ t('storageCenter.orphanLastCleanup') }}：
          <span>{{ formatLastOrphanCleanup(preview.lastOrphanCleanupAt) }}</span>
        </p>
      </div>

      <p class="storage-orphan-dialog__hint">{{ t('storageCenter.orphanDialogHint') }}</p>

      <el-empty
        v-if="preview && preview.totalOrphanCount === 0 && !loading"
        :description="t('storageCenter.orphanEmpty')"
      />

      <div v-else-if="preview" class="storage-orphan-dialog__grid">
        <article
          v-for="zone in preview.zones"
          :key="zone.zoneKey"
          class="storage-orphan-zone-card"
          :class="zoneToneClass(zone.zoneKey)"
        >
          <div class="storage-orphan-zone-card__head">
            <h4 class="storage-orphan-zone-card__title">{{ zone.zoneLabel }}</h4>
            <el-tag
              size="small"
              :type="zone.orphanCount > 0 ? 'warning' : 'success'"
              effect="plain"
              round
            >
              {{ t('storageCenter.orphanZoneCount', { count: zone.orphanCount }) }}
            </el-tag>
          </div>

          <p class="storage-orphan-zone-card__purpose">{{ zone.zonePurpose }}</p>

          <div class="storage-orphan-zone-card__metrics">
            <span>{{ t('storageCenter.orphanZoneFreed') }} {{ formatStorageBytes(zone.freedBytes) }}</span>
            <span>{{ t('storageCenter.orphanZoneScanned') }} {{ zone.scannedCount }}</span>
          </div>

          <ul v-if="previewFiles(zone).length" class="storage-orphan-zone-card__files">
            <li v-for="file in previewFiles(zone)" :key="file.relativePath">
              <span class="storage-orphan-zone-card__file-name" :title="file.fileName">
                {{ file.fileName }}
              </span>
              <span class="storage-orphan-zone-card__file-size">
                {{ formatStorageBytes(file.sizeBytes) }}
              </span>
            </li>
          </ul>
          <p v-else class="storage-orphan-zone-card__empty">{{ t('storageCenter.orphanZoneEmpty') }}</p>

          <button
            v-if="zone.orphanCount > 0"
            type="button"
            class="storage-orphan-zone-card__more"
            @click="openZoneDetail(zone)"
          >
            {{ t('storageCenter.orphanViewAll') }}
            <span v-if="zone.orphanCount > previewFiles(zone).length">
              ({{ zone.orphanCount }})
            </span>
          </button>
        </article>
      </div>
    </div>

    <template #footer>
      <el-button :loading="loading" @click="loadPreview">{{ t('storageCenter.orphanRefreshPreview') }}</el-button>
      <el-button
        type="danger"
        :loading="executing"
        :disabled="!preview || preview.totalOrphanCount === 0"
        @click="confirmExecute"
      >
        {{ t('storageCenter.orphanExecuteAll') }}
      </el-button>
    </template>

    <el-drawer
      v-model="detailVisible"
      :show-close="false"
      size="680px"
      append-to-body
      destroy-on-close
      class="storage-orphan-detail-drawer"
      @close="detailZone = null"
    >
      <template #header>
        <div v-if="detailZone" class="storage-orphan-detail-drawer__header">
          <div class="storage-orphan-detail-drawer__title-row">
            <h3 class="storage-orphan-detail-drawer__title">{{ detailTitle }}</h3>
            <span class="storage-orphan-detail-drawer__summary-tag">
              <el-icon class="storage-orphan-detail-drawer__summary-icon"><WarningFilled /></el-icon>
              <span>{{ t('storageCenter.orphanZoneSummary', {
                count: detailZone.orphanCount,
                size: formatStorageBytes(detailZone.freedBytes),
              }) }}</span>
            </span>
          </div>
          <el-button
            type="danger"
            class="storage-orphan-detail-drawer__cleanup-btn"
            :loading="zoneExecuting"
            :disabled="detailZone.orphanCount === 0"
            @click="confirmZoneCleanup"
          >
            <el-icon v-if="!zoneExecuting"><Delete /></el-icon>
            {{ t('storageCenter.orphanCleanZone') }}
          </el-button>
        </div>
      </template>

      <div v-loading="detailLoading" class="storage-orphan-detail-drawer__content">
        <template v-if="detailZone">
          <div class="storage-orphan-detail-drawer__stats">
            <article class="storage-orphan-detail-drawer__stat-card is-blue">
              <div class="storage-orphan-detail-drawer__stat-icon" aria-hidden="true">
                <el-icon><Picture /></el-icon>
              </div>
              <div class="storage-orphan-detail-drawer__stat-main">
                <span class="storage-orphan-detail-drawer__stat-number">{{ detailZone.orphanCount }}</span>
                <span class="storage-orphan-detail-drawer__stat-label">{{ t('storageCenter.orphanDetailOrphanFiles') }}</span>
              </div>
              <div class="storage-orphan-detail-drawer__stat-side">
                <span class="storage-orphan-detail-drawer__stat-side-label">{{ t('storageCenter.orphanDetailOccupiedSpace') }}</span>
                <span class="storage-orphan-detail-drawer__stat-side-value">
                  <span class="storage-orphan-detail-drawer__stat-side-used">{{ detailOrphanOccupiedUsedText }}</span>
                  <span
                    v-if="detailOrphanOccupiedQuotaText"
                    class="storage-orphan-detail-drawer__stat-side-total"
                  >/{{ detailOrphanOccupiedQuotaText }}</span>
                </span>
              </div>
            </article>
            <article class="storage-orphan-detail-drawer__stat-card is-green">
              <div class="storage-orphan-detail-drawer__stat-icon" aria-hidden="true">
                <el-icon><Document /></el-icon>
              </div>
              <div class="storage-orphan-detail-drawer__stat-main">
                <span class="storage-orphan-detail-drawer__stat-number">{{ detailZone.scannedCount }}</span>
                <span class="storage-orphan-detail-drawer__stat-label">{{ t('storageCenter.orphanDetailTotalFiles') }}</span>
              </div>
              <div class="storage-orphan-detail-drawer__stat-side">
                <span class="storage-orphan-detail-drawer__stat-side-label">{{ t('storageCenter.orphanDetailAvgSize') }}</span>
                <span class="storage-orphan-detail-drawer__stat-side-value">{{ detailAvgFileSize }}</span>
              </div>
            </article>
          </div>

          <p v-if="detailZone.orphanCount > detailZone.files.length" class="storage-orphan-detail-drawer__truncated">
            {{ t('storageCenter.orphanListTruncated', {
              shown: detailZone.files.length,
              total: detailZone.orphanCount,
            }) }}
          </p>

          <el-empty
            v-if="detailZone.orphanCount === 0"
            :description="t('storageCenter.orphanZoneEmpty')"
          />

          <el-table
            v-else
            :data="detailZone.files"
            size="small"
            stripe
            class="storage-orphan-detail-drawer__table"
            :default-sort="{ prop: 'orphanedAt', order: 'descending' }"
          >
            <el-table-column prop="fileName" min-width="200">
              <template #header>
                <span class="storage-orphan-detail-drawer__col-header">
                  {{ t('storageCenter.orphanFileName') }}
                  <el-tooltip
                    :content="detailZone.zonePurpose"
                    placement="top"
                    :show-after="200"
                  >
                    <el-icon class="storage-orphan-detail-drawer__col-tip"><InfoFilled /></el-icon>
                  </el-tooltip>
                </span>
              </template>
              <template #default="{ row }">
                <div class="storage-orphan-detail-drawer__file-cell">
                  <OrphanFileThumb
                    :zone-key="detailZone.zoneKey"
                    :file-name="row.fileName"
                    :relative-path="row.relativePath"
                  />
                  <el-tooltip
                    :content="resolveOrphanAbsolutePath(row.relativePath)"
                    placement="top"
                    :show-after="200"
                  >
                    <span class="storage-orphan-detail-drawer__file-name">{{ row.fileName }}</span>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('storageCenter.orphanFileSize')" width="90">
              <template #default="{ row }">{{ formatStorageBytes(row.sizeBytes) }}</template>
            </el-table-column>
            <el-table-column
              prop="orphanedAt"
              :label="t('storageCenter.orphanIsolatedAt')"
              width="168"
              sortable
            >
              <template #default="{ row }">{{ formatDateTime(row.orphanedAt) }}</template>
            </el-table-column>
            <el-table-column
              :label="t('storageCenter.orphanActions')"
              width="72"
              align="center"
              fixed="right"
              class-name="storage-orphan-detail-drawer__actions-cell"
            >
              <template #default="{ row }">
                <el-button
                  type="danger"
                  link
                  :loading="deletingRelativePath === row.relativePath"
                  :disabled="Boolean(deletingRelativePath && deletingRelativePath !== row.relativePath)"
                  @click="confirmDeleteOrphanFile(row)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-drawer>

    <el-dialog
      v-model="deleteConfirmVisible"
      :title="t('storageCenter.orphanDeleteFile')"
      width="420px"
      append-to-body
      destroy-on-close
      class="storage-orphan-delete-dialog"
      @closed="pendingDeleteFile = null"
    >
      <div v-if="pendingDeleteFile && detailZone" class="storage-orphan-delete-dialog__body">
        <OrphanFileThumb
          :zone-key="detailZone.zoneKey"
          :file-name="pendingDeleteFile.fileName"
          :relative-path="pendingDeleteFile.relativePath"
          size="large"
          :previewable="false"
        />
        <p class="storage-orphan-delete-dialog__name" :title="pendingDeleteFile.fileName">
          {{ pendingDeleteFile.fileName }}
        </p>
        <p class="storage-orphan-delete-dialog__hint">
          {{ t('storageCenter.orphanDeleteFileConfirm') }}
        </p>
      </div>
      <template #footer>
        <el-button @click="deleteConfirmVisible = false">{{ t('storageCenter.cancel') }}</el-button>
        <el-button
          type="danger"
          :loading="Boolean(deletingRelativePath)"
          @click="executeDeleteOrphanFile"
        >
          {{ t('storageCenter.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Coin, Delete, Document, InfoFilled, Picture, Search, WarningFilled } from '@element-plus/icons-vue'
import {
  cleanupOrphanFiles,
  deleteOrphanFile,
  executeAllOrphanCleanup,
  formatStorageBytes,
  formatStorageBytesCompact,
  previewAllOrphanFiles,
  previewOrphanZone,
  type StorageOrphanFileItem,
  type StorageOrphanPreview,
  type StorageOrphanZonePreview,
} from '@/api/storageCenter'
import OrphanFileThumb from '@/components/storage/OrphanFileThumb.vue'
import { formatDateTime } from '@/utils/date'

const CARD_FILE_PREVIEW_LIMIT = 3

const zoneToneMap: Record<string, string> = {
  ECOMMERCE_IMAGES: 'is-blue',
  NOTEBOOK_IMAGES: 'is-purple',
  NOTEBOOK_CONTENT: 'is-green',
  IMPORT_FILES: 'is-orange',
}

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  completed: []
}>()

const { t } = useI18n()

const loading = ref(false)
const executing = ref(false)
const zoneExecuting = ref(false)
const deletingRelativePath = ref('')
const deleteConfirmVisible = ref(false)
const pendingDeleteFile = ref<StorageOrphanFileItem | null>(null)
const detailLoading = ref(false)
const preview = ref<StorageOrphanPreview | null>(null)
const detailVisible = ref(false)
const detailZone = ref<StorageOrphanZonePreview | null>(null)

const detailTitle = computed(() =>
  detailZone.value
    ? t('storageCenter.orphanZoneDetailTitle', { name: detailZone.value.zoneLabel })
    : '',
)

const detailAvgFileSize = computed(() => {
  const zone = detailZone.value
  if (!zone || zone.orphanCount <= 0) {
    return formatStorageBytes(0)
  }
  return formatStorageBytes(Math.round(zone.freedBytes / zone.orphanCount))
})

const detailOrphanOccupiedUsedText = computed(() => {
  const zone = detailZone.value
  if (!zone) return ''
  return formatStorageBytesCompact(zone.freedBytes)
})

const detailOrphanOccupiedQuotaText = computed(() => {
  const zone = detailZone.value
  if (!zone?.zoneQuotaBytes || zone.zoneQuotaBytes <= 0) return ''
  return formatStorageBytesCompact(zone.zoneQuotaBytes)
})

const freedSizeParts = computed(() => splitStorageBytes(preview.value?.totalFreedBytes || 0))

watch(
  () => props.visible,
  (open) => {
    if (!open) {
      preview.value = null
      detailVisible.value = false
      detailZone.value = null
    }
  },
)

function resolveOrphanAbsolutePath(relativePath: string) {
  const localPath = detailZone.value?.localPath
  if (!localPath) {
    return relativePath
  }
  const base = localPath.replace(/[/\\]+$/, '')
  const separator = base.includes('\\') ? '\\' : '/'
  const normalizedRelativePath = relativePath
    .replace(/^[/\\]+/, '')
    .split(/[/\\]+/)
    .join(separator)
  return `${base}${separator}${normalizedRelativePath}`
}

function zoneToneClass(zoneKey: string) {
  return zoneToneMap[zoneKey] || 'is-blue'
}

function previewFiles(zone: StorageOrphanZonePreview) {
  return zone.files.slice(0, CARD_FILE_PREVIEW_LIMIT)
}

function openZoneDetail(zone: StorageOrphanZonePreview) {
  detailZone.value = zone
  detailVisible.value = true
  void loadZoneDetail(zone.zoneKey)
}

async function loadZoneDetail(zoneKey: string) {
  detailLoading.value = true
  try {
    detailZone.value = await previewOrphanZone(zoneKey)
  } catch {
    ElMessage.error(t('storageCenter.orphanPreviewFailed'))
  } finally {
    detailLoading.value = false
  }
}

async function confirmDeleteOrphanFile(file: StorageOrphanFileItem) {
  if (!detailZone.value) return
  pendingDeleteFile.value = file
  deleteConfirmVisible.value = true
}

async function executeDeleteOrphanFile() {
  if (!detailZone.value || !pendingDeleteFile.value) return
  const file = pendingDeleteFile.value

  deletingRelativePath.value = file.relativePath
  try {
    await deleteOrphanFile(detailZone.value.zoneKey, file.relativePath)
    deleteConfirmVisible.value = false
    pendingDeleteFile.value = null
    await refreshDialogContent()
    ElMessage.success(
      t('storageCenter.orphanDeleteFileDone', {
        name: file.fileName,
        size: formatStorageBytes(file.sizeBytes),
      }),
    )
    emit('completed')
  } catch {
    ElMessage.error(t('storageCenter.cleanupFailed'))
  } finally {
    deletingRelativePath.value = ''
  }
}

async function confirmZoneCleanup() {
  if (!detailZone.value || detailZone.value.orphanCount === 0) return
  const zone = detailZone.value
  try {
    await ElMessageBox.confirm(
      t('storageCenter.orphanZoneExecuteConfirm', {
        name: zone.zoneLabel,
        count: zone.orphanCount,
        size: formatStorageBytes(zone.freedBytes),
      }),
      t('storageCenter.orphanCleanZone'),
      { type: 'warning' },
    )
  } catch {
    return
  }
  zoneExecuting.value = true
  try {
    const removedCount = zone.orphanCount
    const freedBytes = zone.freedBytes
    await cleanupOrphanFiles(zone.zoneKey, false)
    await refreshDialogContent()
    ElMessage.success(
      t('storageCenter.orphanExecuteDone', {
        count: removedCount,
        size: formatStorageBytes(freedBytes),
      }),
    )
    emit('completed')
  } catch {
    ElMessage.error(t('storageCenter.cleanupFailed'))
  } finally {
    zoneExecuting.value = false
  }
}

function formatLastOrphanCleanup(value?: string | null) {
  if (!value) {
    return t('storageCenter.orphanNeverCleaned')
  }
  return formatDateTime(value)
}

function splitStorageBytes(bytes: number) {
  const formatted = formatStorageBytes(bytes)
  const spaceIndex = formatted.indexOf(' ')
  if (spaceIndex === -1) {
    return { value: formatted, unit: '' }
  }
  return {
    value: formatted.slice(0, spaceIndex),
    unit: formatted.slice(spaceIndex + 1),
  }
}

async function loadPreview() {
  loading.value = true
  try {
    preview.value = await previewAllOrphanFiles()
  } catch {
    ElMessage.error(t('storageCenter.orphanPreviewFailed'))
  } finally {
    loading.value = false
  }
}

async function refreshDialogContent() {
  await loadPreview()
  if (detailVisible.value && detailZone.value) {
    await loadZoneDetail(detailZone.value.zoneKey)
  }
}

async function confirmExecute() {
  if (!preview.value || preview.value.totalOrphanCount === 0) return
  try {
    await ElMessageBox.confirm(
      t('storageCenter.orphanExecuteConfirm', {
        count: preview.value.totalOrphanCount,
        size: formatStorageBytes(preview.value.totalFreedBytes),
      }),
      t('storageCenter.orphanExecuteAll'),
      { type: 'warning' },
    )
  } catch {
    return
  }
  executing.value = true
  try {
    const removedCount = preview.value.totalOrphanCount
    const freedBytes = preview.value.totalFreedBytes
    await executeAllOrphanCleanup()
    detailVisible.value = false
    detailZone.value = null
    await refreshDialogContent()
    ElMessage.success(
      t('storageCenter.orphanExecuteDone', {
        count: removedCount,
        size: formatStorageBytes(freedBytes),
      }),
    )
    emit('completed')
  } catch {
    ElMessage.error(t('storageCenter.cleanupFailed'))
  } finally {
    executing.value = false
  }
}
</script>

<style scoped lang="scss">
.storage-orphan-dialog {
  &__body {
    min-height: 240px;
  }

  &__summary-wrap {
    margin-bottom: 14px;
  }

  &__summary {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
  }

  &__summary-item {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 14px;
    padding: 16px 14px;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    background: #fff;
    text-align: center;
  }

  &__summary-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 48px;
    border-radius: 14px;
    flex-shrink: 0;
    font-size: 24px;
    color: #fff;
  }

  &__summary-item.is-orange .storage-orphan-dialog__summary-icon {
    background: var(--wr-stat-orange, #ea580c);
  }

  &__summary-item.is-green .storage-orphan-dialog__summary-icon {
    background: var(--wr-stat-green, #16a34a);
  }

  &__summary-item.is-blue .storage-orphan-dialog__summary-icon {
    background: var(--wr-stat-blue, #2563eb);
  }

  &__summary-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-width: 0;
  }

  &__summary-value {
    display: flex;
    align-items: baseline;
    justify-content: center;
    gap: 3px;
  }

  &__summary-number {
    font-size: 22px;
    font-weight: 700;
    line-height: 1.2;
  }

  &__summary-unit {
    font-size: 13px;
    font-weight: 400;
    line-height: 1.2;
  }

  &__summary-item.is-orange &__summary-value {
    color: var(--wr-stat-orange, #ea580c);
  }

  &__summary-item.is-green &__summary-value {
    color: var(--wr-stat-green, #16a34a);
  }

  &__summary-item.is-blue &__summary-value {
    color: var(--wr-stat-blue, #2563eb);
  }

  &__summary-label {
    margin-bottom: 4px;
    font-size: 12px;
    color: #6b7280;
  }

  &__last-cleanup {
    margin: 10px 0 0;
    padding: 10px 14px;
    border-radius: 8px;
    background: #f9fafb;
    font-size: 13px;
    color: #6b7280;
    text-align: center;

    span {
      color: #374151;
      font-weight: 600;
    }
  }

  &__hint {
    margin: 0 0 14px;
    font-size: 13px;
    line-height: 1.5;
    color: #6b7280;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
  }
}

.storage-orphan-zone-card {
  display: flex;
  flex-direction: column;
  min-height: 220px;
  padding: 14px 16px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);

  &.is-blue {
    border-top: 3px solid var(--wr-stat-blue, #2563eb);
  }

  &.is-purple {
    border-top: 3px solid var(--wr-stat-purple, #7c3aed);
  }

  &.is-green {
    border-top: 3px solid var(--wr-stat-green, #16a34a);
  }

  &.is-orange {
    border-top: 3px solid var(--wr-stat-orange, #ea580c);
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 8px;
  }

  &__title {
    margin: 0;
    font-size: 15px;
    font-weight: 700;
    color: #1f2937;
  }

  &__purpose {
    margin: 0 0 10px;
    font-size: 12px;
    line-height: 1.55;
    color: #6b7280;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__metrics {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 14px;
    margin-bottom: 10px;
    font-size: 12px;
    color: #374151;
    font-weight: 600;
  }

  &__files {
    margin: 0;
    padding: 0;
    list-style: none;
    flex: 1;
  }

  &__files li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 6px 0;
    border-bottom: 1px dashed #eef2f7;
    font-size: 12px;

    &:last-child {
      border-bottom: none;
    }
  }

  &__file-name {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #1f2937;
  }

  &__file-size {
    flex-shrink: 0;
    color: #9ca3af;
  }

  &__empty {
    margin: 0;
    flex: 1;
    font-size: 12px;
    color: #9ca3af;
  }

  &__more {
    align-self: flex-start;
    margin-top: 10px;
    padding: 0;
    border: none;
    background: transparent;
    font-size: 13px;
    font-weight: 600;
    color: #2563eb;
    cursor: pointer;

    &:hover {
      color: #1d4ed8;
      text-decoration: underline;
    }
  }
}

.storage-orphan-detail-drawer {
  :deep(.el-drawer__header) {
    position: sticky;
    top: 0;
    z-index: 3;
    margin-bottom: 0;
    padding: 16px 20px 12px;
    border-bottom: 1px solid #e5e7eb;
    background: #fff;
  }

  :deep(.el-drawer__body) {
    padding: 0;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    width: 100%;
  }

  &__title-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    min-width: 0;
  }

  &__title {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: #1f2937;
    line-height: 1.4;
  }

  &__summary-tag {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
    height: 28px;
    padding: 0 12px;
    border: 1px solid var(--el-color-warning-light-5, #f5dab1);
    border-radius: 999px;
    background: var(--el-color-warning-light-9, #fdf6ec);
    color: var(--el-color-warning, #e6a23c);
    font-size: 12px;
    line-height: 1;
    vertical-align: middle;
    box-sizing: border-box;
  }

  &__summary-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    line-height: 0;
    flex-shrink: 0;

    :deep(svg) {
      display: block;
    }
  }

  &__cleanup-btn :deep(.el-icon) {
    margin-right: 6px;
  }

  &__content {
    min-height: 200px;
    padding: 14px 20px 20px;
  }

  &__stats {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 14px;
  }

  &__stat-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    background: #fff;
    box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
  }

  &__stat-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border-radius: 50%;
    flex-shrink: 0;
    font-size: 22px;
  }

  &__stat-card.is-blue &__stat-icon {
    color: var(--wr-stat-blue, #2563eb);
    background: rgb(37 99 235 / 10%);
  }

  &__stat-card.is-green &__stat-icon {
    color: var(--wr-stat-green, #16a34a);
    background: rgb(22 163 74 / 10%);
  }

  &__stat-main {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }

  &__stat-number {
    font-size: 24px;
    font-weight: 700;
    line-height: 1.15;
    color: #1f2937;
  }

  &__stat-label {
    margin-top: 2px;
    font-size: 12px;
    color: #6b7280;
  }

  &__stat-side {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    margin-left: auto;
    text-align: right;
    flex-shrink: 0;
  }

  &__stat-side-label {
    font-size: 12px;
    color: #9ca3af;
    white-space: nowrap;
  }

  &__stat-side-value {
    display: inline-flex;
    align-items: baseline;
    margin-top: 2px;
    white-space: nowrap;
  }

  &__stat-card.is-blue &__stat-side-used {
    font-size: 18px;
    font-weight: 700;
    line-height: 1.2;
    color: var(--wr-stat-blue, #2563eb);
  }

  &__stat-card.is-blue &__stat-side-total {
    font-size: 13px;
    font-weight: 600;
    line-height: 1.2;
    color: #9ca3af;
  }

  &__stat-card.is-green &__stat-side-value {
    margin-top: 2px;
    font-size: 14px;
    font-weight: 600;
    color: #374151;
  }

  &__truncated {
    margin: 0 0 10px;
    font-size: 12px;
    color: #f59e0b;
  }

  &__col-header {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: #111827;
  }

  &__col-tip {
    font-size: 14px;
    color: #9ca3af;
    cursor: help;
    vertical-align: middle;

    &:hover {
      color: #6b7280;
    }
  }

  &__file-cell {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
    width: 100%;
    height: 40px;
  }

  &__file-name {
    display: inline-block;
    min-width: 0;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    vertical-align: middle;
    cursor: default;
  }

  &__table {
    width: 100%;

    :deep(.el-table__header .el-table__cell) {
      font-weight: 700;
      color: #111827;
    }

    :deep(.el-table__header .el-table__cell .cell) {
      color: #111827;
    }

    :deep(.el-table__body tr) {
      height: 56px;
    }

    :deep(.el-table__body .el-table__cell) {
      padding: 0;
      height: 56px;
    }

    :deep(.el-table__body .el-table__cell .cell) {
      display: flex;
      align-items: center;
      height: 56px;
      line-height: 1.4;
    }

    :deep(.el-table__body .storage-orphan-detail-drawer__actions-cell .cell) {
      justify-content: center;
    }
  }
}

@media (max-width: 760px) {
  .storage-orphan-dialog__grid {
    grid-template-columns: 1fr;
  }

  .storage-orphan-dialog__summary {
    grid-template-columns: 1fr;
  }

  .storage-orphan-detail-drawer__stats {
    grid-template-columns: 1fr;
  }
}
</style>

<style lang="scss">
.storage-orphan-delete-dialog {
  &__body {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    padding: 4px 8px 0;
    text-align: center;
  }

  &__name {
    margin: 0;
    max-width: 100%;
    font-size: 14px;
    font-weight: 600;
    color: #1f2937;
    word-break: break-all;
    line-height: 1.45;
  }

  &__hint {
    margin: 0;
    font-size: 13px;
    line-height: 1.55;
    color: #6b7280;
  }
}
</style>
