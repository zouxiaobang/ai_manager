<template>
  <section class="deploy-version-panel deploy-panel-card">
    <div class="deploy-version-panel__head">
      <div>
        <h2 class="deploy-panel-card__title">{{ t('deployCenter.versionsTitle') }}</h2>
        <p class="deploy-panel-card__desc">{{ t('deployCenter.versionsDesc') }}</p>
      </div>
      <button type="button" class="deploy-version-panel__refresh" :disabled="loading" @click="load">
        <el-icon v-if="loading" class="is-loading"><Loading /></el-icon>
        <el-icon v-else><Refresh /></el-icon>
        {{ t('deployCenter.versionsRefresh') }}
      </button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe border class="deploy-version-panel__table">
      <el-table-column :label="t('deployCenter.versionsColTime')" min-width="150">
        <template #default="{ row }">
          {{ formatTime(row) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('deployCenter.versionsColTarget')" width="100">
        <template #default="{ row }">
          <span class="deploy-version-panel__target" :class="`is-${row.target}`">
            {{ targetLabel(row.target) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="t('deployCenter.versionsColStatus')" width="96">
        <template #default="{ row }">
          <span class="deploy-version-panel__status" :class="row.success ? 'is-ok' : 'is-fail'">
            {{ row.success ? t('deployCenter.versionsStatusOk') : t('deployCenter.versionsStatusFail') }}
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="t('deployCenter.versionsColCommit')" width="100">
        <template #default="{ row }">
          <code v-if="row.gitCommit" class="deploy-version-panel__commit">{{ row.gitCommit }}</code>
          <span v-else class="deploy-version-panel__muted">—</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('deployCenter.versionsColMessage')" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.gitMessage || '—' }}
        </template>
      </el-table-column>
      <el-table-column :label="t('deployCenter.versionsColBranch')" width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.gitBranch || '—' }}
        </template>
      </el-table-column>
      <el-table-column :label="t('deployCenter.versionsColAuthor')" width="100" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.gitAuthor || '—' }}
        </template>
      </el-table-column>
      <el-table-column :label="t('deployCenter.versionsColMode')" width="88">
        <template #default="{ row }">
          {{ modeLabel(row.deployMode) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('deployCenter.versionsColDuration')" width="100">
        <template #default="{ row }">
          {{ formatDuration(row.durationMs) }}
        </template>
      </el-table-column>
    </el-table>

    <p v-if="!loading && rows.length === 0" class="deploy-version-panel__empty">
      {{ t('deployCenter.versionsEmpty') }}
    </p>
  </section>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Loading, Refresh } from '@element-plus/icons-vue'
import { fetchDeployVersions, type DeployVersionRecord } from '@/api/deployVersion'
import { formatServerDateTime } from '@/utils/deployTimeFormat'

const props = defineProps<{
  active?: boolean
}>()

const { t, locale } = useI18n()
const loading = ref(false)
const rows = ref<DeployVersionRecord[]>([])

function formatTime(row: DeployVersionRecord) {
  if (row.deployedAt) {
    return row.deployedAt
  }
  if (row.finishedAt) {
    return formatServerDateTime(new Date(row.finishedAt), locale.value)
  }
  return '—'
}

function formatDuration(ms?: number) {
  if (!ms || ms < 1000) {
    return ms ? `${ms} ms` : '—'
  }
  const totalSec = Math.round(ms / 1000)
  if (totalSec < 60) {
    return `${totalSec}s`
  }
  const min = Math.floor(totalSec / 60)
  const sec = totalSec % 60
  return sec > 0 ? `${min}m ${sec}s` : `${min}m`
}

function targetLabel(target: string) {
  return target === 'backend'
    ? t('deployCenter.versionsTargetBackend')
    : target === 'frontend'
      ? t('deployCenter.versionsTargetFrontend')
      : target
}

function modeLabel(mode?: string) {
  if (mode === 'local') return t('deployCenter.versionsModeLocal')
  if (mode === 'remote') return t('deployCenter.versionsModeRemote')
  return mode || '—'
}

async function load() {
  loading.value = true
  try {
    rows.value = await fetchDeployVersions(50)
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}

watch(
  () => props.active,
  (active) => {
    if (active) void load()
  },
  { immediate: true },
)

defineExpose({ reload: load })
</script>

<style scoped lang="scss">
.deploy-version-panel {
  &__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 14px;
  }

  &__refresh {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 14px;
    border-radius: 10px;
    border: 1px solid #d1d5db;
    background: #fff;
    color: #374151;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    flex-shrink: 0;

    &:disabled {
      opacity: 0.7;
      cursor: wait;
    }
  }

  &__table {
    width: 100%;
  }

  &__target {
    display: inline-flex;
    padding: 2px 8px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 600;

    &.is-backend {
      color: #1d4ed8;
      background: #eff6ff;
    }

    &.is-frontend {
      color: #15803d;
      background: #f0fdf4;
    }
  }

  &__status {
    font-size: 12px;
    font-weight: 600;

    &.is-ok {
      color: #15803d;
    }

    &.is-fail {
      color: #b91c1c;
    }
  }

  &__commit {
    font-family: Consolas, 'Courier New', monospace;
    font-size: 12px;
    color: #1f2937;
    background: #f3f4f6;
    padding: 2px 6px;
    border-radius: 6px;
  }

  &__muted {
    color: #9ca3af;
  }

  &__empty {
    margin: 16px 0 0;
    text-align: center;
    color: #6b7280;
    font-size: 13px;
  }
}
</style>
