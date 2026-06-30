<template>
  <div class="storage-center war-room-page">
    <header class="storage-center__header">
      <div>
        <h1 class="storage-center__title">{{ t('storageCenter.title') }}</h1>
        <p class="storage-center__subtitle">{{ t('storageCenter.subtitle') }}</p>
      </div>
      <div class="storage-center__header-actions">
        <el-tag
          :type="overview?.baiduPanAuthorized ? 'success' : 'warning'"
          effect="plain"
          round
        >
          {{
            overview?.baiduPanAuthorized
              ? t('storageCenter.baiduConnected')
              : t('storageCenter.baiduDisconnected')
          }}
        </el-tag>
        <el-button :loading="loading" @click="reloadAll">
          {{ t('storageCenter.refresh') }}
        </el-button>
        <el-button
          v-if="!overview?.baiduPanAuthorized && overview?.baiduPanAuthorizeUrl"
          @click="connectBaiduPan"
        >
          {{ t('storageCenter.connectBaidu') }}
        </el-button>
      </div>
    </header>

    <section v-if="overview" class="storage-center__stats">
      <div class="storage-stat-card is-blue">
        <div class="storage-stat-card__icon" aria-hidden="true">
          <el-icon><FolderOpened /></el-icon>
        </div>
        <div class="storage-stat-card__body">
          <div class="storage-stat-card__label">{{ t('storageCenter.stats.localUsed') }}</div>
          <div class="storage-stat-card__value">
            {{ formatStorageBytes(overview.totalLocalUsedBytes) }}
            <span v-if="overview.totalLocalQuotaBytes > 0" class="storage-stat-card__quota">
              / {{ formatStorageBytes(overview.totalLocalQuotaBytes) }}
            </span>
          </div>
          <el-progress
            :percentage="overview.totalLocalUsagePercent"
            :stroke-width="8"
            :show-text="false"
          />
        </div>
      </div>

      <div class="storage-stat-card is-purple">
        <div class="storage-stat-card__icon" aria-hidden="true">
          <el-icon><Cpu /></el-icon>
        </div>
        <div class="storage-stat-card__body">
          <div class="storage-stat-card__label">{{ t('storageCenter.stats.cache') }}</div>
          <div class="storage-stat-card__value">
            {{ formatStorageBytes(overview.cacheUsedBytes) }}
            <span v-if="overview.cacheMaxBytes > 0" class="storage-stat-card__quota">
              / {{ formatStorageBytes(overview.cacheMaxBytes) }}
            </span>
          </div>
          <div class="storage-stat-card__meta">
            TTL {{ overview.cacheTtlSeconds }}s
          </div>
        </div>
      </div>

      <div class="storage-stat-card is-green">
        <div class="storage-stat-card__icon" aria-hidden="true">
          <el-icon><Connection /></el-icon>
        </div>
        <div class="storage-stat-card__body">
          <div class="storage-stat-card__label">{{ t('storageCenter.stats.dualStorage') }}</div>
          <div class="storage-stat-card__value">
            {{ overview.dualStorageEnabled ? t('storageCenter.enabled') : t('storageCenter.disabled') }}
          </div>
          <div class="storage-stat-card__meta">{{ t('storageCenter.stats.dualStorageHint') }}</div>
        </div>
      </div>

      <div class="storage-stat-card is-orange">
        <div class="storage-stat-card__icon" aria-hidden="true">
          <el-icon><Grid /></el-icon>
        </div>
        <div class="storage-stat-card__body">
          <div class="storage-stat-card__label">{{ t('storageCenter.stats.zones') }}</div>
          <div class="storage-stat-card__value">{{ overview.zones.length }}</div>
          <div class="storage-stat-card__meta">{{ t('storageCenter.stats.zonesHint') }}</div>
        </div>
      </div>
    </section>

    <section v-if="overview" class="storage-center__breakdown war-room-panel">
      <h2 class="storage-center__section-title">{{ t('storageCenter.breakdownTitle') }}</h2>
      <div class="storage-breakdown">
        <div
          v-for="zone in overview.zones"
          :key="zone.key"
          class="storage-breakdown__row"
        >
          <div class="storage-breakdown__head">
            <span class="storage-breakdown__name">{{ zone.label }}</span>
            <span class="storage-breakdown__size">
              {{ formatStorageBytes(zone.usedBytes) }}
              <template v-if="zone.quotaBytes > 0">
                / {{ formatStorageBytes(zone.quotaBytes) }}
              </template>
            </span>
          </div>
          <el-progress
            :percentage="zone.usagePercent"
            :stroke-width="10"
            :color="zoneColor(zone.usagePercent)"
          />
          <div class="storage-breakdown__meta">
            <span>{{ t('storageCenter.fileCount', { count: zone.fileCount }) }}</span>
            <span>
              {{
                zone.dualStorageEnabled && zone.cloudAvailable
                  ? t('storageCenter.localAndCloud')
                  : t('storageCenter.localOnly')
              }}
            </span>
          </div>
        </div>
      </div>
    </section>

    <section class="storage-center__main">
      <aside class="storage-center__zones war-room-panel">
        <div class="storage-center__zones-head">
          <h2 class="storage-center__section-title">{{ t('storageCenter.zoneListTitle') }}</h2>
          <el-button size="small" plain @click="orphanDialogVisible = true">
            {{ t('storageCenter.orphanCleanupEntry') }}
          </el-button>
        </div>
        <button
          v-for="zone in overview?.zones || []"
          :key="zone.key"
          type="button"
          class="storage-zone-item"
          :class="{ 'is-active': selectedZoneKey === zone.key }"
          @click="selectedZoneKey = zone.key"
        >
          <div class="storage-zone-item__head">
            <span class="storage-zone-item__name">{{ zone.label }}</span>
            <el-tag
              v-if="zone.dualStorageEnabled && zone.cloudAvailable"
              size="small"
              type="success"
              effect="plain"
              round
            >
              {{ t('storageCenter.dualTag') }}
            </el-tag>
          </div>
          <div class="storage-zone-item__usage">
            {{ formatStorageBytes(zone.usedBytes) }}
            <span v-if="zone.quotaBytes > 0">· {{ zone.usagePercent }}%</span>
          </div>
        </button>

        <button
          type="button"
          class="storage-zone-item"
          :class="{ 'is-active': selectedZoneKey === 'REDIS_CACHE' }"
          @click="selectedZoneKey = 'REDIS_CACHE'"
        >
          <div class="storage-zone-item__head">
            <span class="storage-zone-item__name">{{ t('storageCenter.redisCache') }}</span>
          </div>
          <div class="storage-zone-item__usage">
            {{ formatStorageBytes(overview?.cacheUsedBytes || 0) }}
          </div>
        </button>
      </aside>

      <div class="storage-center__detail war-room-panel">
        <template v-if="selectedZone">
          <div class="storage-detail-header" :class="detailHeaderTone">
            <div class="storage-detail-header__icon" aria-hidden="true">
              <el-icon><component :is="detailZoneIcon" /></el-icon>
            </div>
            <h2 class="storage-detail-header__title">{{ selectedZone.label }}</h2>
          </div>

          <div class="storage-detail-paths">
            <div class="storage-path-field">
              <div class="storage-path-field__label">{{ t('storageCenter.localPath') }}</div>
              <div class="storage-path-box">
                <code class="storage-path-box__text">{{ selectedZone.localPath }}</code>
                <button
                  type="button"
                  class="storage-path-box__copy"
                  :title="t('storageCenter.copyPath')"
                  @click="copyPath(selectedZone.localPath)"
                >
                  <el-icon><CopyDocument /></el-icon>
                </button>
              </div>
            </div>
            <div class="storage-path-field">
              <div class="storage-path-field__label">{{ t('storageCenter.cloudPath') }}</div>
              <div class="storage-path-box">
                <code class="storage-path-box__text">{{ selectedZone.cloudPath }}</code>
                <button
                  type="button"
                  class="storage-path-box__copy"
                  :title="t('storageCenter.copyPath')"
                  @click="copyPath(selectedZone.cloudPath)"
                >
                  <el-icon><CopyDocument /></el-icon>
                </button>
              </div>
            </div>
          </div>

          <div class="storage-detail-status">
            <div class="storage-usage-panel">
              <div class="storage-usage-panel__head">
                <span class="storage-usage-panel__label">{{ t('storageCenter.usage') }}</span>
                <div class="storage-usage-panel__stats">
                  <span class="storage-usage-panel__size">
                    {{ formatStorageBytes(selectedZone.usedBytes) }}
                    <template v-if="selectedZone.quotaBytes > 0">
                      / {{ formatStorageBytes(selectedZone.quotaBytes) }}
                    </template>
                  </span>
                  <span
                    v-if="selectedZone.quotaBytes > 0"
                    class="storage-usage-panel__percent"
                    :style="{ color: zoneColor(selectedZone.usagePercent) }"
                  >
                    {{ selectedZone.usagePercent }}%
                  </span>
                </div>
              </div>
              <div v-if="selectedZone.quotaBytes > 0" class="storage-usage-bar">
                <div
                  class="storage-usage-bar__fill"
                  :style="{
                    width: `${zoneUsagePercent(selectedZone)}%`,
                    background: zoneColor(selectedZone.usagePercent),
                  }"
                />
              </div>
            </div>

            <div class="storage-sync-mode">
              <div class="storage-detail-field__label">{{ t('storageCenter.syncMode') }}</div>
              <el-tag
                :type="syncModeTagType(selectedZone)"
                effect="plain"
                round
              >
                {{
                  selectedZone.dualStorageEnabled
                    ? t('storageCenter.syncDual')
                    : t('storageCenter.syncLocal')
                }}
              </el-tag>
            </div>
          </div>

          <StorageZoneConfigCard
            v-if="configForm"
            :zone-key="selectedZoneKey"
            :zone="selectedZone"
            :config="configForm"
            :editing="configEditing"
            :draft="configDraft"
            :saving="saving"
            @edit="startConfigEdit"
            @cancel="cancelConfigEdit"
            @save="confirmSaveConfig"
          >
            <template #actions>
              <el-button
                v-if="selectedZone.key === 'NOTEBOOK_CONTENT'"
                :loading="noteSyncLoading"
                @click="runNoteContentSync"
              >
                {{ t('storageCenter.syncNoteContent') }}
              </el-button>
            </template>
          </StorageZoneConfigCard>
        </template>

        <template v-else-if="selectedZoneKey === 'REDIS_CACHE'">
          <div class="storage-detail-header" :class="detailHeaderTone">
            <div class="storage-detail-header__icon" aria-hidden="true">
              <el-icon><component :is="detailZoneIcon" /></el-icon>
            </div>
            <h2 class="storage-detail-header__title">{{ t('storageCenter.redisCache') }}</h2>
          </div>
          <p class="storage-center__hint">{{ t('storageCenter.redisHint') }}</p>

          <div class="storage-detail-status">
            <div class="storage-usage-panel">
              <div class="storage-usage-panel__head">
                <span class="storage-usage-panel__label">{{ t('storageCenter.usage') }}</span>
                <div class="storage-usage-panel__stats">
                  <span class="storage-usage-panel__size">
                    {{ formatStorageBytes(overview?.cacheUsedBytes || 0) }}
                    <template v-if="overview && overview.cacheMaxBytes > 0">
                      / {{ formatStorageBytes(overview.cacheMaxBytes) }}
                    </template>
                  </span>
                  <span
                    v-if="overview && overview.cacheMaxBytes > 0"
                    class="storage-usage-panel__percent"
                    :style="{ color: zoneColor(cacheUsagePercent) }"
                  >
                    {{ cacheUsagePercent }}%
                  </span>
                </div>
              </div>
              <div v-if="overview && overview.cacheMaxBytes > 0" class="storage-usage-bar">
                <div
                  class="storage-usage-bar__fill"
                  :style="{
                    width: `${cacheUsagePercent}%`,
                    background: zoneColor(cacheUsagePercent),
                  }"
                />
              </div>
            </div>
          </div>

          <StorageZoneConfigCard
            v-if="configForm"
            zone-key="REDIS_CACHE"
            :zone="null"
            :config="configForm"
            :editing="configEditing"
            :draft="configDraft"
            :saving="saving"
            @edit="startConfigEdit"
            @cancel="cancelConfigEdit"
            @save="confirmSaveConfig"
          >
            <template #actions>
              <el-button :loading="cleanupLoading" @click="previewCacheCleanup">
                {{ t('storageCenter.previewCacheCleanup') }}
              </el-button>
              <el-button
                type="danger"
                plain
                :loading="cleanupLoading"
                @click="executeCacheCleanup"
              >
                {{ t('storageCenter.executeCacheCleanup') }}
              </el-button>
            </template>
          </StorageZoneConfigCard>
        </template>

        <template v-else>
          <el-empty :description="t('storageCenter.selectZoneHint')" />
        </template>
      </div>
    </section>

    <StorageOrphanCleanupDialog
      v-model:visible="orphanDialogVisible"
      @completed="reloadAll"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch, type Component } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Connection,
  CopyDocument,
  Cpu,
  Document,
  FolderOpened,
  GoodsFilled,
  Grid,
  Picture,
  UploadFilled,
} from '@element-plus/icons-vue'
import {
  cleanupStorageCache,
  fetchStorageConfig,
  fetchStorageOverview,
  formatStorageBytes,
  saveStorageConfig,
  syncNoteContentReconcile,
  type StorageCenterConfig,
  type StorageCenterOverview,
  type StorageZone,
} from '@/api/storageCenter'
import StorageOrphanCleanupDialog from '@/views/storage/StorageOrphanCleanupDialog.vue'
import StorageZoneConfigCard from '@/components/storage/StorageZoneConfigCard.vue'

const zoneIconMap: Record<string, Component> = {
  ECOMMERCE_IMAGES: GoodsFilled,
  NOTEBOOK_IMAGES: Picture,
  NOTEBOOK_CONTENT: Document,
  IMPORT_FILES: UploadFilled,
  REDIS_CACHE: Cpu,
}

const zoneToneMap: Record<string, string> = {
  ECOMMERCE_IMAGES: 'is-blue',
  NOTEBOOK_IMAGES: 'is-purple',
  NOTEBOOK_CONTENT: 'is-green',
  IMPORT_FILES: 'is-orange',
  REDIS_CACHE: 'is-purple',
}

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const cleanupLoading = ref(false)
const noteSyncLoading = ref(false)
const overview = ref<StorageCenterOverview | null>(null)
const configForm = ref<StorageCenterConfig | null>(null)
const configDraft = ref<StorageCenterConfig | null>(null)
const configEditing = ref(false)
const orphanDialogVisible = ref(false)
const selectedZoneKey = ref('ECOMMERCE_IMAGES')

const selectedZone = computed(() =>
  overview.value?.zones.find((zone) => zone.key === selectedZoneKey.value) || null,
)

const detailZoneIcon = computed(
  () => zoneIconMap[selectedZoneKey.value] || FolderOpened,
)

const detailHeaderTone = computed(
  () => zoneToneMap[selectedZoneKey.value] || 'is-blue',
)

const cacheUsagePercent = computed(() => {
  const used = overview.value?.cacheUsedBytes || 0
  const max = overview.value?.cacheMaxBytes || 0
  if (max <= 0) return 0
  return Math.min(100, Math.round((used / max) * 100))
})

function syncModeTagType(zone: StorageZone): 'success' | 'warning' | 'info' {
  if (!zone.dualStorageEnabled) {
    return 'info'
  }
  return zone.cloudAvailable ? 'success' : 'warning'
}

async function copyPath(text: string) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(t('storageCenter.copied'))
  } catch {
    ElMessage.error(t('storageCenter.copyFailed'))
  }
}

function zoneColor(percent: number) {
  if (percent >= 90) return '#ef4444'
  if (percent >= 70) return '#f59e0b'
  return '#2563eb'
}

function zoneUsagePercent(zone: StorageZone) {
  if (!zone.quotaBytes || zone.quotaBytes <= 0) {
    return 0
  }
  return Math.min(100, Math.max(0, zone.usagePercent))
}

async function reloadAll() {
  loading.value = true
  try {
    const [nextOverview, nextConfig] = await Promise.all([
      fetchStorageOverview(),
      fetchStorageConfig(),
    ])
    overview.value = nextOverview
    configForm.value = { ...nextConfig }
    if (!nextOverview.zones.some((zone) => zone.key === selectedZoneKey.value)) {
      selectedZoneKey.value = nextOverview.zones[0]?.key || 'REDIS_CACHE'
    }
  } catch {
    ElMessage.error(t('storageCenter.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function startConfigEdit() {
  if (!configForm.value) {
    try {
      configForm.value = await fetchStorageConfig()
    } catch {
      ElMessage.error(t('storageCenter.loadFailed'))
      return
    }
  }
  configDraft.value = JSON.parse(JSON.stringify(configForm.value)) as StorageCenterConfig
  configEditing.value = true
}

function cancelConfigEdit() {
  configEditing.value = false
  configDraft.value = null
}

async function confirmSaveConfig() {
  if (!configDraft.value) return
  try {
    await ElMessageBox.confirm(
      t('storageCenter.saveConfigConfirm'),
      t('storageCenter.saveConfig'),
      { type: 'warning' },
    )
  } catch {
    return
  }
  saving.value = true
  try {
    configForm.value = await saveStorageConfig({ ...configDraft.value })
    configEditing.value = false
    configDraft.value = null
    await reloadAll()
    ElMessage.success(t('storageCenter.saveSuccess'))
  } catch {
    ElMessage.error(t('storageCenter.saveFailed'))
  } finally {
    saving.value = false
  }
}

function connectBaiduPan() {
  const url = overview.value?.baiduPanAuthorizeUrl
  if (!url) return
  const returnPath = encodeURIComponent('/storage')
  const joiner = url.includes('?') ? '&' : '?'
  window.location.href = `${url}${joiner}state=${returnPath}`
}

async function runNoteContentSync() {
  noteSyncLoading.value = true
  try {
    await syncNoteContentReconcile()
    ElMessage.success(t('storageCenter.syncNoteContentStarted'))
  } catch {
    ElMessage.error(t('storageCenter.cleanupFailed'))
  } finally {
    noteSyncLoading.value = false
  }
}

async function previewCacheCleanup() {
  await runCacheCleanup(true)
}

async function executeCacheCleanup() {
  await ElMessageBox.confirm(
    t('storageCenter.cacheCleanupConfirm'),
    t('storageCenter.executeCacheCleanup'),
    { type: 'warning' },
  )
  await runCacheCleanup(false)
}

async function runCacheCleanup(dryRun: boolean) {
  cleanupLoading.value = true
  try {
    const result = await cleanupStorageCache(dryRun)
    const message = dryRun
      ? t('storageCenter.cacheCleanupPreviewDone', {
          count: result.removedCount,
          size: formatStorageBytes(result.freedBytes),
        })
      : t('storageCenter.cacheCleanupDone', {
          count: result.removedCount,
          size: formatStorageBytes(result.freedBytes),
        })
    if (dryRun) {
      ElMessage.info(message)
    } else {
      ElMessage.success(message)
    }
    if (!dryRun) await reloadAll()
  } catch {
    ElMessage.error(t('storageCenter.cleanupFailed'))
  } finally {
    cleanupLoading.value = false
  }
}

onMounted(() => {
  void reloadAll()
})

watch(selectedZoneKey, () => {
  if (configEditing.value) {
    cancelConfigEdit()
  }
})
</script>

<style scoped lang="scss">
.storage-center {
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

  &__header-actions {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  &__stats {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
    margin-bottom: 16px;
  }

  &__breakdown {
    margin-bottom: 16px;
  }

  &__section-title {
    margin: 0 0 14px;
    font-size: 15px;
    font-weight: 700;
    color: #1f2937;
  }

  &__main {
    display: grid;
    grid-template-columns: 280px minmax(0, 1fr);
    gap: 16px;
  }

  &__zones {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__zones-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 4px;

    .storage-center__section-title {
      margin-bottom: 0;
    }
  }

  &__form-title {
    margin: 0 0 12px;
    font-size: 14px;
    font-weight: 700;
  }

  &__form {
    max-width: 560px;
  }

  &__hint {
    margin: 0 0 12px;
    font-size: 13px;
    color: var(--wr-text-secondary);
  }
}

.storage-stat-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 12px;
  border: 1px solid var(--wr-border);
  background: var(--wr-card);
  box-shadow: var(--wr-shadow);

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border-radius: 12px;
    flex-shrink: 0;
    font-size: 22px;
    color: #fff;
  }

  &__body {
    min-width: 0;
    flex: 1;
  }

  &__label {
    font-size: 12px;
    color: var(--wr-muted);
    margin-bottom: 8px;
  }

  &__value {
    font-size: 22px;
    font-weight: 700;
    color: #111827;
    margin-bottom: 10px;
  }

  &__quota {
    font-size: 14px;
    font-weight: 500;
    color: var(--wr-muted);
  }

  &__meta {
    margin-top: 8px;
    font-size: 12px;
    color: var(--wr-text-secondary);
  }

  &.is-blue {
    border-top: 3px solid var(--wr-stat-blue);

    .storage-stat-card__icon {
      background: var(--wr-stat-blue, #2563eb);
    }
  }

  &.is-purple {
    border-top: 3px solid var(--wr-stat-purple);

    .storage-stat-card__icon {
      background: var(--wr-stat-purple, #7c3aed);
    }
  }

  &.is-green {
    border-top: 3px solid var(--wr-stat-green);

    .storage-stat-card__icon {
      background: var(--wr-stat-green, #16a34a);
    }
  }

  &.is-orange {
    border-top: 3px solid var(--wr-stat-orange);

    .storage-stat-card__icon {
      background: var(--wr-stat-orange, #ea580c);
    }
  }
}

.storage-breakdown {
  display: grid;
  gap: 14px;

  &__row {
    display: grid;
    gap: 6px;
  }

  &__head {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    font-size: 13px;
  }

  &__name {
    font-weight: 600;
    color: #1f2937;
  }

  &__size {
    color: var(--wr-muted);
  }

  &__meta {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: var(--wr-text-secondary);
  }
}

.storage-zone-item {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--wr-border);
  border-radius: 10px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;

  &:hover {
    border-color: #c7d2fe;
    background: #f8faff;
  }

  &.is-active {
    border-color: #6366f1;
    background: #eef2ff;
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 6px;
  }

  &__name {
    font-size: 13px;
    font-weight: 600;
    color: #1f2937;
  }

  &__usage {
    font-size: 12px;
    color: var(--wr-muted);
  }
}

.storage-detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    border-radius: 16px;
    flex-shrink: 0;
    font-size: 28px;
    color: #fff;
  }

  &__title {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
    color: #1f2937;
  }

  &.is-blue .storage-detail-header__icon {
    background: var(--wr-stat-blue, #2563eb);
  }

  &.is-purple .storage-detail-header__icon {
    background: var(--wr-stat-purple, #7c3aed);
  }

  &.is-green .storage-detail-header__icon {
    background: var(--wr-stat-green, #16a34a);
  }

  &.is-orange .storage-detail-header__icon {
    background: var(--wr-stat-orange, #ea580c);
  }
}

.storage-detail-paths {
  display: grid;
  gap: 14px;
  margin-bottom: 16px;
}

.storage-path-field {
  &__label {
    margin-bottom: 6px;
    font-size: 13px;
    font-weight: 600;
    color: #374151;
  }
}

.storage-path-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;

  &__text {
    flex: 1;
    min-width: 0;
    margin: 0;
    font-family: Consolas, 'Cascadia Code', 'Courier New', monospace;
    font-size: 13px;
    color: #1f2937;
    word-break: break-all;
    line-height: 1.45;
    background: transparent;
  }

  &__copy {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 32px;
    height: 32px;
    border: none;
    border-radius: 6px;
    background: transparent;
    color: #6b7280;
    cursor: pointer;
    font-size: 16px;

    &:hover {
      background: #e5e7eb;
      color: #374151;
    }
  }
}

.storage-detail-field {
  margin-bottom: 16px;

  &__label {
    font-size: 12px;
    color: var(--wr-muted);
    margin-bottom: 4px;
  }

  &__value {
    font-size: 13px;
    color: #1f2937;
    word-break: break-all;
  }
}

.storage-detail-status {
  display: grid;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.storage-sync-mode {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.storage-usage-panel {
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 10px;
  }

  &__label {
    font-size: 13px;
    font-weight: 600;
    color: #374151;
  }

  &__stats {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-left: auto;
  }

  &__size {
    font-size: 13px;
    color: #6b7280;
  }

  &__percent {
    font-size: 14px;
    font-weight: 600;
    color: #2563eb;
  }
}

.storage-usage-bar {
  height: 10px;
  border-radius: 999px;
  background: #f3f4f6;
  overflow: hidden;

  &__fill {
    height: 100%;
    border-radius: 999px;
    background: #2563eb;
    transition: width 0.3s ease;
    min-width: 0;
  }
}

@media (max-width: 1100px) {
  .storage-center__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .storage-center__main {
    grid-template-columns: 1fr;
  }
}
</style>
