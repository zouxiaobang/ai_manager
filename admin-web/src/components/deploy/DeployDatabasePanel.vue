<template>
  <div class="deploy-database-panel">
    <div class="deploy-database-panel__stats-row">
      <article class="deploy-database-panel__stat-card is-purple">
        <div class="deploy-database-panel__stat-icon" aria-hidden="true">
          <el-icon><Grid /></el-icon>
        </div>
        <div class="deploy-database-panel__stat-body">
          <div class="deploy-database-panel__stat-label">{{ t('deployCenter.databaseStatTables') }}</div>
          <div class="deploy-database-panel__stat-value">
            {{ snapshot.tableCount }}
            <span class="deploy-database-panel__stat-unit">{{ t('deployCenter.databaseStatUnitTables') }}</span>
          </div>
        </div>
      </article>
      <article class="deploy-database-panel__stat-card is-blue">
        <div class="deploy-database-panel__stat-icon" aria-hidden="true">
          <el-icon><Tickets /></el-icon>
        </div>
        <div class="deploy-database-panel__stat-body">
          <div class="deploy-database-panel__stat-label">{{ t('deployCenter.databaseStatRows') }}</div>
          <div class="deploy-database-panel__stat-value">
            {{ formatCount(totalRows) }}
            <span class="deploy-database-panel__stat-unit">{{ t('deployCenter.databaseStatUnitRows') }}</span>
          </div>
        </div>
      </article>
      <article class="deploy-database-panel__stat-card is-green is-with-action">
        <div class="deploy-database-panel__stat-icon" aria-hidden="true">
          <el-icon><Box /></el-icon>
        </div>
        <div class="deploy-database-panel__stat-body">
          <div class="deploy-database-panel__stat-label">{{ t('deployCenter.databaseStatDatabase') }}</div>
          <div class="deploy-database-panel__stat-value is-text">{{ snapshot.databaseName || '—' }}</div>
        </div>
        <button type="button" class="deploy-database-panel__terminal" @click="openSqlTerminalFromCard">
          <el-icon><Monitor /></el-icon>
          {{ t('deployCenter.sqlTerminalButton') }}
        </button>
      </article>
      <article class="deploy-database-panel__stat-card is-orange is-with-sync">
        <div class="deploy-database-panel__stat-icon" aria-hidden="true">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="deploy-database-panel__stat-body">
          <div class="deploy-database-panel__stat-label">{{ t('deployCenter.databaseStatSynced') }}</div>
          <div class="deploy-database-panel__stat-value is-text">{{ snapshot.syncedAt || '—' }}</div>
        </div>
        <button
          type="button"
          class="deploy-database-panel__sync"
          :disabled="syncing"
          @click="sync"
        >
          <el-icon v-if="syncing" class="is-loading"><Loading /></el-icon>
          <el-icon v-else><Refresh /></el-icon>
          {{ syncing ? t('deployCenter.databaseSyncing') : t('deployCenter.databaseSync') }}
        </button>
      </article>
    </div>

    <section class="deploy-panel-card deploy-database-panel__groups">
      <div class="deploy-database-panel__toolbar">
        <el-input
          v-model="keyword"
          clearable
          :placeholder="t('deployCenter.databaseSearchPlaceholder')"
          class="deploy-database-panel__search"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <span class="deploy-database-panel__count">
          {{ t('deployCenter.databaseTableCount', { count: filteredTables.length }) }}
        </span>
      </div>

      <div v-loading="loading || syncing" class="deploy-database-panel__collapse-wrap">
        <el-collapse v-model="expandedGroups" class="deploy-database-panel__collapse">
          <el-collapse-item
            v-for="group in groupedTables"
            :key="group.key"
            :name="group.key"
          >
            <template #title>
              <span class="deploy-database-panel__group-title">{{ group.label }}</span>
              <span class="deploy-database-panel__group-badge">{{ group.tables.length }}</span>
            </template>
            <div class="deploy-database-panel__group-grid">
              <button
                v-for="table in group.tables"
                :key="table.tableName"
                type="button"
                class="deploy-database-panel__chip"
                @click="openTable(table)"
              >
                <div class="deploy-database-panel__chip-head">
                  <code class="deploy-database-panel__chip-name">{{ table.tableName }}</code>
                  <span class="deploy-database-panel__chip-rows">
                    {{ t('deployCenter.databaseRows', { count: formatCount(table.rowCount) }) }}
                  </span>
                </div>
                <p class="deploy-database-panel__chip-comment">
                  {{ table.tableComment || t('deployCenter.databaseNoComment') }}
                </p>
                <div class="deploy-database-panel__chip-meta">
                  <span>{{ t('deployCenter.databaseColumns', { count: table.columnCount }) }}</span>
                  <span v-if="table.engine">{{ table.engine }}</span>
                </div>
              </button>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <p v-if="!loading && !syncing && snapshot.tableCount === 0" class="deploy-database-panel__empty">
        {{ t('deployCenter.databaseEmpty') }}
      </p>
      <p v-else-if="!loading && filteredTables.length === 0" class="deploy-database-panel__empty">
        {{ t('deployCenter.databaseNoMatch') }}
      </p>
    </section>

    <el-drawer
      v-model="drawerVisible"
      direction="btt"
      size="56%"
      destroy-on-close
      class="deploy-database-panel__sheet"
      :show-close="true"
      :with-header="true"
    >
      <template #header>
        <div v-if="selectedTable" class="deploy-database-panel__sheet-header-wrap">
          <div class="deploy-database-panel__sheet-header">
            <h3 class="deploy-database-panel__sheet-title">{{ selectedTable.tableName }}</h3>
            <p class="deploy-database-panel__sheet-subtitle">
              {{ selectedTable.tableComment || t('deployCenter.databaseNoComment') }}
            </p>
          </div>
          <div class="deploy-database-panel__sheet-actions">
            <el-button size="small" type="success" @click="openSqlTerminalForTable">
              {{ t('deployCenter.sqlTerminalButton') }}
            </el-button>
          </div>
        </div>
      </template>
      <template v-if="selectedTable">
        <div class="deploy-database-panel__sheet-body">
          <div class="deploy-database-panel__sheet-meta">
            <span>{{ t('deployCenter.databaseRows', { count: formatCount(selectedTable.rowCount) }) }}</span>
            <span>{{ t('deployCenter.databaseColumns', { count: selectedTable.columnCount }) }}</span>
            <span v-if="selectedTable.engine">{{ selectedTable.engine }}</span>
          </div>

          <el-tabs v-model="sheetView" class="deploy-database-panel__sheet-tabs" @tab-change="onSheetTabChange">
            <el-tab-pane :label="t('deployCenter.databaseViewStructure')" name="structure">
              <div class="deploy-database-panel__pane-toolbar">
                <el-button size="small" :loading="ddlLoading" @click="showDdl">
                  {{ t('deployCenter.databaseShowDdl') }}
                </el-button>
              </div>
              <el-table
                :data="selectedTable.columns"
                stripe
                border
                size="small"
                class="deploy-database-panel__sheet-table"
              >
            <el-table-column :label="t('deployCenter.databaseColName')" min-width="160">
              <template #default="{ row }">
                <span class="deploy-database-panel__col-name">
                  {{ row.columnName }}
                  <el-icon
                    v-if="row.columnKey === 'PRI'"
                    class="deploy-database-panel__key-icon is-pri"
                    :title="t('deployCenter.databaseKeyPri')"
                  >
                    <Key />
                  </el-icon>
                  <span
                    v-else-if="row.columnKey === 'MUL'"
                    class="deploy-database-panel__key-icon is-mul"
                    :title="t('deployCenter.databaseKeyMul')"
                    aria-hidden="true"
                  >
                    <svg viewBox="0 0 16 16" width="14" height="14" fill="none">
                      <path
                        d="M8 3v3.5M8 10.5V13M8 10.5H5M8 10.5h3"
                        stroke="currentColor"
                        stroke-width="1.5"
                        stroke-linecap="round"
                      />
                      <circle cx="8" cy="3" r="1.2" fill="currentColor" />
                      <circle cx="5" cy="13" r="1.2" fill="currentColor" />
                      <circle cx="11" cy="13" r="1.2" fill="currentColor" />
                    </svg>
                  </span>
                  <el-icon
                    v-else-if="row.columnKey === 'UNI'"
                    class="deploy-database-panel__key-icon is-uni"
                    :title="t('deployCenter.databaseKeyUni')"
                  >
                    <Stamp />
                  </el-icon>
                </span>
              </template>
            </el-table-column>
            <el-table-column :label="t('deployCenter.databaseColType')" prop="columnType" min-width="120" />
            <el-table-column :label="t('deployCenter.databaseColNullable')" width="72">
              <template #default="{ row }">
                {{ row.nullable ? t('deployCenter.databaseYes') : t('deployCenter.databaseNo') }}
              </template>
            </el-table-column>
            <el-table-column :label="t('deployCenter.databaseColKey')" prop="columnKey" width="72" />
            <el-table-column :label="t('deployCenter.databaseColDefault')" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">
                <code class="deploy-database-panel__col-default">{{ formatDefault(row.columnDefault) }}</code>
              </template>
            </el-table-column>
            <el-table-column
              :label="t('deployCenter.databaseColComment')"
              prop="columnComment"
              min-width="180"
              show-overflow-tooltip
            />
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="t('deployCenter.databaseViewData')" name="data">
              <div class="deploy-database-panel__pane-toolbar">
                <el-button
                  size="small"
                  :disabled="!selectedTable"
                  @click="openExportDialog"
                >
                  {{ t('deployCenter.databaseExportData') }}
                </el-button>
              </div>
              <div v-loading="dataLoading" class="deploy-database-panel__data-view">
                <el-table
                  v-if="dataTableRows.length"
                  :data="dataTableRows"
                  stripe
                  border
                  size="small"
                  class="deploy-database-panel__sheet-table"
                >
                  <el-table-column
                    v-for="column in dataColumns"
                    :key="column"
                    :prop="column"
                    :label="column"
                    min-width="120"
                    show-overflow-tooltip
                  />
                </el-table>
                <p v-else-if="!dataLoading" class="deploy-database-panel__empty">
                  {{ t('deployCenter.databaseViewDataEmpty') }}
                </p>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </template>
    </el-drawer>

    <el-dialog
      v-model="ddlVisible"
      :title="t('deployCenter.databaseDdlTitle', { table: selectedTable?.tableName ?? '' })"
      width="720px"
      top="8vh"
      class="deploy-database-panel__ddl-dialog"
      destroy-on-close
    >
      <div v-loading="ddlLoading" class="deploy-database-panel__ddl-body">
        <div v-if="ddlText" class="deploy-database-panel__ddl-wrap">
          <button
            type="button"
            class="deploy-database-panel__ddl-copy"
            :title="t('deployCenter.copy')"
            @click="copyDdl"
          >
            <el-icon><DocumentCopy /></el-icon>
          </button>
          <pre class="deploy-database-panel__ddl-pre">{{ ddlText }}</pre>
        </div>
        <p v-else-if="!ddlLoading" class="deploy-database-panel__empty">{{ t('deployCenter.databaseDdlEmpty') }}</p>
      </div>
    </el-dialog>

    <el-dialog
      v-model="exportDialogVisible"
      :title="t('deployCenter.databaseExportDialogTitle')"
      width="440px"
      destroy-on-close
      class="deploy-database-panel__export-dialog"
    >
      <el-form label-width="108px" class="deploy-database-panel__export-form" @submit.prevent="confirmExportData">
        <el-form-item :label="t('deployCenter.databaseExportStartRow')">
          <el-input-number
            v-model="exportStartRow"
            :min="1"
            :step="1"
            controls-position="right"
            class="deploy-database-panel__export-input"
          />
        </el-form-item>
        <el-form-item :label="t('deployCenter.databaseExportRowCount')">
          <el-input-number
            v-model="exportRowCount"
            :min="1"
            :max="1000"
            :step="100"
            controls-position="right"
            class="deploy-database-panel__export-input"
          />
        </el-form-item>
        <p class="deploy-database-panel__export-hint">{{ t('deployCenter.databaseExportRowCountHint') }}</p>
      </el-form>
      <template #footer>
        <el-button @click="exportDialogVisible = false">{{ t('deployCenter.databaseExportCancel') }}</el-button>
        <el-button type="primary" :loading="exportSubmitting" @click="confirmExportData">
          {{ t('deployCenter.databaseExportConfirm') }}
        </el-button>
      </template>
    </el-dialog>

    <DeploySqlTerminalDialog v-model="sqlDialogVisible" :initial-sql="sqlInitialSql" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import DeploySqlTerminalDialog from '@/components/deploy/DeploySqlTerminalDialog.vue'
import { executeDeploySql, type DeploySqlExecuteResult } from '@/api/deploySqlTerminal'
import { Box, Clock, DocumentCopy, Grid, Key, Loading, Monitor, Refresh, Search, Stamp, Tickets } from '@element-plus/icons-vue'
import {
  fetchDeployDatabaseSnapshot,
  syncDeployDatabaseSnapshot,
  type DeployDatabaseSnapshot,
  type DeployDatabaseTable,
} from '@/api/deployDatabase'

const GROUP_ORDER = ['ec', 'nb', 'sys', 'pomodoro', 'other'] as const
type GroupKey = (typeof GROUP_ORDER)[number]

const props = defineProps<{
  active?: boolean
}>()

const { t } = useI18n()

const loading = ref(false)
const syncing = ref(false)
const keyword = ref('')
const expandedGroups = ref<string[]>([])
const drawerVisible = ref(false)
const sqlDialogVisible = ref(false)
const sqlInitialSql = ref('')
const sheetView = ref<'structure' | 'data'>('structure')
const dataLoading = ref(false)
const dataResult = ref<DeploySqlExecuteResult | null>(null)
const ddlVisible = ref(false)
const ddlLoading = ref(false)
const ddlText = ref('')
const exportDialogVisible = ref(false)
const exportStartRow = ref(1)
const exportRowCount = ref(1000)
const exportSubmitting = ref(false)
const selectedTable = ref<DeployDatabaseTable | null>(null)
const snapshot = ref<DeployDatabaseSnapshot>({
  databaseName: '',
  syncedAt: '',
  syncedAtEpochMs: 0,
  tableCount: 0,
  tables: [],
})

const filteredTables = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) {
    return snapshot.value.tables
  }
  return snapshot.value.tables.filter((table) => {
    const name = table.tableName.toLowerCase()
    const comment = (table.tableComment || '').toLowerCase()
    return name.includes(q) || comment.includes(q)
  })
})

const totalRows = computed(() =>
  snapshot.value.tables.reduce((sum, table) => sum + (table.rowCount >= 0 ? table.rowCount : 0), 0),
)

const dataColumns = computed(() => dataResult.value?.columns ?? [])

const dataTableRows = computed(() => {
  if (!dataResult.value || dataResult.value.statementType !== 'query') {
    return []
  }
  return dataResult.value.rows.map((row) => {
    const record: Record<string, unknown> = {}
    dataResult.value!.columns.forEach((column, index) => {
      const value = row[index]
      record[column] = value === null || value === undefined ? 'NULL' : value
    })
    return record
  })
})

const groupedTables = computed(() => {
  const buckets = new Map<GroupKey, DeployDatabaseTable[]>()
  for (const table of filteredTables.value) {
    const key = resolveGroupKey(table.tableName)
    const list = buckets.get(key) ?? []
    list.push(table)
    buckets.set(key, list)
  }
  return GROUP_ORDER.filter((key) => (buckets.get(key)?.length ?? 0) > 0).map((key) => ({
    key,
    label: t(`deployCenter.databaseGroup.${key}`),
    tables: buckets.get(key) ?? [],
  }))
})

function resolveGroupKey(tableName: string): GroupKey {
  const lower = tableName.toLowerCase()
  if (lower.startsWith('ec_')) return 'ec'
  if (lower.startsWith('nb_')) return 'nb'
  if (lower.startsWith('sys_')) return 'sys'
  if (lower.startsWith('pomodoro_')) return 'pomodoro'
  return 'other'
}

function formatCount(value: number) {
  if (value < 0) {
    return '—'
  }
  return value.toLocaleString()
}

function formatDefault(value?: string) {
  if (value === null || value === undefined || value === '') {
    return '—'
  }
  return value
}

function buildTableSelectSql(tableName: string, limit = 100, offset = 0) {
  return `SELECT *\nFROM \`${tableName}\`\nLIMIT ${limit} OFFSET ${offset}`
}

function openSqlTerminalFromCard() {
  sqlInitialSql.value = ''
  sqlDialogVisible.value = true
}

function openSqlTerminalForTable() {
  if (!selectedTable.value) {
    return
  }
  sqlInitialSql.value = buildTableSelectSql(selectedTable.value.tableName)
  sqlDialogVisible.value = true
}

function onSheetTabChange(name: string | number) {
  if (name === 'data') {
    void loadTableData()
  }
}

async function loadTableData() {
  if (!selectedTable.value || dataLoading.value) {
    return
  }
  dataLoading.value = true
  dataResult.value = null
  try {
    dataResult.value = await executeDeploySql('local', buildTableSelectSql(selectedTable.value.tableName, 100, 0))
  } catch {
    dataResult.value = null
  } finally {
    dataLoading.value = false
  }
}

async function showDdl() {
  if (!selectedTable.value) {
    return
  }
  ddlVisible.value = true
  ddlLoading.value = true
  ddlText.value = ''
  try {
    const result = await executeDeploySql(
      'local',
      `SHOW CREATE TABLE \`${selectedTable.value.tableName}\``,
    )
    const createIdx = result.columns.findIndex((column) => /create\s+table/i.test(column))
    if (createIdx >= 0 && result.rows[0]) {
      ddlText.value = String(result.rows[0][createIdx] ?? '')
    } else if (result.rows[0]?.length) {
      ddlText.value = result.rows[0].map((cell) => String(cell ?? '')).join('\n')
    }
  } catch {
    ddlText.value = ''
  } finally {
    ddlLoading.value = false
  }
}

async function copyDdl() {
  if (!ddlText.value) {
    return
  }
  try {
    await navigator.clipboard.writeText(ddlText.value)
    ElMessage.success(t('deployCenter.copied'))
  } catch {
    ElMessage.error(t('deployCenter.copyFailed'))
  }
}

function escapeCsvCell(value: unknown) {
  if (value === null || value === undefined) {
    return ''
  }
  const text = String(value)
  if (/[",\r\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

function downloadResultAsCsv(result: DeploySqlExecuteResult, fileName: string) {
  const lines = [
    result.columns.map(escapeCsvCell).join(','),
    ...result.rows.map((row) => row.map(escapeCsvCell).join(',')),
  ]
  const blob = new Blob([`\uFEFF${lines.join('\n')}`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}

function openExportDialog() {
  if (!selectedTable.value) {
    return
  }
  exportStartRow.value = 1
  exportRowCount.value = 1000
  exportDialogVisible.value = true
}

async function confirmExportData() {
  if (!selectedTable.value || exportSubmitting.value) {
    return
  }
  const startRow = Math.max(1, Math.floor(exportStartRow.value || 1))
  const rowCount = Math.max(1, Math.min(1000, Math.floor(exportRowCount.value || 1000)))
  exportStartRow.value = startRow
  exportRowCount.value = rowCount

  exportSubmitting.value = true
  try {
    const result = await executeDeploySql(
      'local',
      buildTableSelectSql(selectedTable.value.tableName, rowCount, startRow - 1),
    )
    if (!result.rows.length) {
      ElMessage.warning(t('deployCenter.databaseExportDataEmpty'))
      return
    }
    const suffix = startRow === 1 && rowCount === 1000 ? '' : `_${startRow}-${startRow + result.rows.length - 1}`
    downloadResultAsCsv(result, `${selectedTable.value.tableName}${suffix}.csv`)
    exportDialogVisible.value = false
    ElMessage.success(t('deployCenter.databaseExportDataOk', { count: result.rows.length }))
  } catch {
    // request interceptor already shows error
  } finally {
    exportSubmitting.value = false
  }
}

function openTable(table: DeployDatabaseTable) {
  selectedTable.value = table
  sheetView.value = 'structure'
  dataResult.value = null
  ddlText.value = ''
  drawerVisible.value = true
}

async function load() {
  loading.value = true
  try {
    snapshot.value = await fetchDeployDatabaseSnapshot()
  } catch {
    snapshot.value = {
      databaseName: '',
      syncedAt: '',
      syncedAtEpochMs: 0,
      tableCount: 0,
      tables: [],
    }
  } finally {
    loading.value = false
  }
}

async function sync() {
  syncing.value = true
  try {
    snapshot.value = await syncDeployDatabaseSnapshot()
    ElMessage.success(t('deployCenter.databaseSyncOk'))
  } catch {
    // request interceptor already shows error
  } finally {
    syncing.value = false
  }
}

watch(keyword, (value) => {
  const q = value.trim()
  if (!q) {
    expandedGroups.value = []
    return
  }
  expandedGroups.value = groupedTables.value.map((group) => group.key)
})

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
.deploy-database-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;

  &__stats-row {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
    align-items: stretch;
  }

  &__sync,
  &__terminal {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 12px;
    border-radius: 10px;
    border: 1px solid #d1d5db;
    background: #fff;
    color: #374151;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    flex-shrink: 0;
    white-space: nowrap;

    &:disabled {
      opacity: 0.7;
      cursor: wait;
    }
  }

  &__terminal {
    border-color: #86efac;
  }

  &__sync {
    border-color: #fcd34d;
  }

  &__stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 18px 16px;
    border-radius: 14px;
    border: 1px solid transparent;
    box-shadow: 0 1px 2px rgb(15 23 42 / 4%);

    &.is-with-sync,
    &.is-with-action {
      padding-right: 12px;

      .deploy-database-panel__stat-body {
        flex: 1;
        min-width: 0;
      }
    }

    &.is-purple {
      background: #f5f3ff;
      border-color: #ddd6fe;

      .deploy-database-panel__stat-icon {
        background: #8b5cf6;
      }

      .deploy-database-panel__stat-value {
        color: #7c3aed;
      }
    }

    &.is-blue {
      background: #eff6ff;
      border-color: #bfdbfe;

      .deploy-database-panel__stat-icon {
        background: #3b82f6;
      }

      .deploy-database-panel__stat-value {
        color: #2563eb;
      }
    }

    &.is-green {
      background: #f0fdf4;
      border-color: #bbf7d0;

      .deploy-database-panel__stat-icon {
        background: #22c55e;
      }

      .deploy-database-panel__stat-value {
        color: #16a34a;
      }
    }

    &.is-orange {
      background: #fffbeb;
      border-color: #fde68a;

      .deploy-database-panel__stat-icon {
        background: #f59e0b;
      }

      .deploy-database-panel__stat-value {
        color: #d97706;
      }
    }
  }

  &__stat-icon {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 52px;
    height: 52px;
    border-radius: 14px;
    color: #fff;
    font-size: 26px;
  }

  &__stat-body {
    flex: 1;
    min-width: 0;
  }

  &__stat-label {
    font-size: 13px;
    color: #6b7280;
    margin-bottom: 6px;
    line-height: 1.3;
  }

  &__stat-value {
    font-size: 28px;
    font-weight: 700;
    line-height: 1.15;
    word-break: break-all;

    &.is-text {
      font-size: 17px;
      line-height: 1.35;
    }
  }

  &__stat-unit {
    margin-left: 4px;
    font-size: 15px;
    font-weight: 600;
  }

  &__groups {
    padding: 20px 22px;
    border-radius: 14px;
    background: #fff;
    border: 1px solid #e8ecf2;
  }

  &__toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
  }

  &__search {
    max-width: 360px;
  }

  &__count {
    font-size: 13px;
    color: #6b7280;
    white-space: nowrap;
  }

  &__collapse-wrap {
    min-height: 80px;
  }

  &__collapse {
    border: none;

    :deep(.el-collapse-item) {
      margin-bottom: 12px;
      border: 1px solid #e8ecf2;
      border-radius: 12px;
      overflow: hidden;
      background: #fff;

      &:last-child {
        margin-bottom: 0;
      }
    }

    :deep(.el-collapse-item__header) {
      height: auto;
      min-height: 48px;
      padding: 12px 16px;
      font-size: 14px;
      font-weight: 600;
      color: #111827;
      border: none;
      background: #f8fafc;
    }

    :deep(.el-collapse-item__wrap) {
      border: none;
    }

    :deep(.el-collapse-item__content) {
      padding: 14px 16px 16px;
    }
  }

  &__group-title {
    margin-right: 8px;
  }

  &__group-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 22px;
    height: 22px;
    padding: 0 7px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 700;
    color: #2563eb;
    background: #eff6ff;
  }

  &__group-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 12px;
  }

  &__chip {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 12px 14px;
    border-radius: 10px;
    border: 1px solid #e5e7eb;
    background: #fff;
    text-align: left;
    cursor: pointer;
    transition: border-color 0.15s ease, box-shadow 0.15s ease;

    &:hover {
      border-color: #93c5fd;
      box-shadow: 0 2px 10px rgba(37, 99, 235, 0.08);
    }
  }

  &__chip-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  &__chip-name {
    font-family: Consolas, 'Courier New', monospace;
    font-size: 12px;
    font-weight: 700;
    color: #1d4ed8;
    background: #eff6ff;
    padding: 2px 6px;
    border-radius: 6px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
    min-width: 0;
  }

  &__chip-rows {
    font-size: 11px;
    font-weight: 700;
    color: #15803d;
    white-space: nowrap;
    flex-shrink: 0;
  }

  &__chip-comment {
    margin: 0;
    font-size: 12px;
    color: #4b5563;
    line-height: 1.45;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    min-height: 34px;
  }

  &__chip-meta {
    display: flex;
    gap: 10px;
    font-size: 11px;
    color: #9ca3af;
    font-weight: 600;
  }

  &__empty {
    margin: 16px 0 0;
    text-align: center;
    color: #6b7280;
    font-size: 13px;
  }

  &__sheet-body {
    display: flex;
    flex-direction: column;
    gap: 12px;
    height: 100%;
    min-height: 0;
  }

  &__sheet-header-wrap {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    width: 100%;
    padding-right: 28px;
  }

  &__sheet-header {
    min-width: 0;
    flex: 1;
  }

  &__sheet-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  &__sheet-tabs {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;

    :deep(.el-tabs__content) {
      flex: 1;
      min-height: 0;
    }

    :deep(.el-tab-pane) {
      height: 100%;
    }
  }

  &__pane-toolbar {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 10px;
  }

  &__ddl-body {
    min-height: 120px;
    max-height: 60vh;
    overflow: auto;
  }

  &__ddl-wrap {
    position: relative;
    border-radius: 10px;
    border: 1px solid rgb(34 197 94 / 28%);
    background: rgb(34 197 94 / 10%);
    overflow: auto;
  }

  &__ddl-copy {
    position: absolute;
    top: 10px;
    right: 10px;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border: 1px solid rgb(34 197 94 / 35%);
    border-radius: 8px;
    background: rgb(255 255 255 / 72%);
    color: #166534;
    cursor: pointer;
    transition: background 0.15s, color 0.15s, border-color 0.15s;

    &:hover {
      background: #fff;
      color: #14532d;
      border-color: rgb(34 197 94 / 55%);
    }
  }

  &__ddl-pre {
    margin: 0;
    padding: 14px 48px 14px 16px;
    border-radius: 10px;
    background: transparent;
    color: #111827;
    font-family: Consolas, 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.55;
    white-space: pre-wrap;
    word-break: break-word;
  }

  &__export-form {
    margin-top: 4px;
  }

  &__export-input {
    width: 100%;
  }

  &__export-hint {
    margin: 0 0 4px;
    padding-left: 108px;
    font-size: 12px;
    color: #9ca3af;
    line-height: 1.5;
  }

  &__data-view {
    flex: 1;
    min-height: 200px;
  }

  &__sheet-title {
    margin: 0;
    font-family: Consolas, 'Courier New', monospace;
    font-size: 16px;
    font-weight: 800;
    color: #111827;
    line-height: 1.35;
  }

  &__sheet-subtitle {
    margin: 6px 0 0;
    font-size: 14px;
    font-weight: 700;
    color: #111827;
    line-height: 1.5;
  }

  &__sheet-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    font-size: 12px;
    color: #6b7280;
    font-weight: 600;
  }

  &__sheet-table {
    flex: 1;
    min-height: 0;

    :deep(th) {
      font-weight: 700;
      color: #111827;
    }
  }

  &__col-name {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-family: Consolas, 'Courier New', monospace;
    font-size: 13px;
    font-weight: 800;
    color: #111827;
  }

  &__key-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    line-height: 1;

    &.is-pri {
      font-size: 14px;
      color: #d97706;
    }

    &.is-mul {
      color: #2563eb;
    }

    &.is-uni {
      font-size: 14px;
      color: #7c3aed;
    }
  }

  &__col-default {
    font-family: Consolas, 'Courier New', monospace;
    font-size: 12px;
    color: #374151;
    background: #f8fafc;
    padding: 1px 6px;
    border-radius: 4px;
  }
}

:deep(.deploy-database-panel__sheet.el-drawer) {
  border-radius: 16px 16px 0 0;
  max-height: 88vh;
}

:deep(.deploy-database-panel__sheet .el-drawer__header) {
  margin-bottom: 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

:deep(.deploy-database-panel__sheet .el-drawer__body) {
  display: flex;
  flex-direction: column;
  padding-top: 12px;
  overflow: auto;
}

@media (max-width: 1200px) {
  .deploy-database-panel__stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .deploy-database-panel__stats-row {
    grid-template-columns: 1fr;
  }

  .deploy-database-panel__toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .deploy-database-panel__search {
    max-width: none;
  }

  :deep(.deploy-database-panel__sheet.el-drawer) {
    height: 72% !important;
  }
}
</style>
