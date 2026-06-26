<template>
  <el-dialog
    v-model="visible"
    :title="t('deployCenter.sqlTerminalTitle')"
    width="920px"
    top="6vh"
    class="deploy-sql-terminal-dialog"
    destroy-on-close
    @closed="handleClosed"
  >
    <div class="deploy-sql-terminal-dialog__toolbar">
      <el-radio-group v-model="target" size="small">
        <el-radio-button value="local">{{ t('deployCenter.sqlTerminalLocal') }}</el-radio-button>
        <el-radio-button value="node118">{{ t('deployCenter.sqlTerminalNode118') }}</el-radio-button>
      </el-radio-group>
      <div class="deploy-sql-terminal-dialog__actions">
        <el-button
          type="success"
          class="deploy-sql-terminal-dialog__run-btn"
          :loading="running"
          @click="runSql"
        >
          <el-icon v-if="!running"><VideoPlay /></el-icon>
          {{ t('deployCenter.sqlTerminalRun') }}
        </el-button>
        <el-button class="deploy-sql-terminal-dialog__action-btn" @click="beautify">
          {{ t('deployCenter.sqlTerminalBeautify') }}
        </el-button>
        <el-button class="deploy-sql-terminal-dialog__action-btn" @click="clearSql">
          {{ t('deployCenter.sqlTerminalClear') }}
        </el-button>
      </div>
    </div>

    <el-input
      v-model="sql"
      type="textarea"
      :rows="10"
      resize="vertical"
      class="deploy-sql-terminal-dialog__editor"
      :placeholder="t('deployCenter.sqlTerminalPlaceholder')"
    />

    <div v-if="result" class="deploy-sql-terminal-dialog__result">
      <div class="deploy-sql-terminal-dialog__result-meta">
        <span>{{ result.targetLabel }}</span>
        <span>{{ result.message }}</span>
        <span>{{ t('deployCenter.sqlTerminalDuration', { ms: result.durationMs }) }}</span>
      </div>

      <el-table
        v-if="result.statementType === 'query' && tableRows.length"
        :data="tableRows"
        stripe
        border
        size="small"
        max-height="360"
        class="deploy-sql-terminal-dialog__table"
      >
        <el-table-column
          v-for="column in result.columns"
          :key="column"
          :prop="column"
          :label="column"
          min-width="120"
          show-overflow-tooltip
        />
      </el-table>
      <p v-else-if="result.statementType === 'query'" class="deploy-sql-terminal-dialog__empty">
        {{ t('deployCenter.sqlTerminalNoRows') }}
      </p>

      <el-table
        v-else-if="result.statementType === 'batch' && result.batchItems?.length"
        :data="result.batchItems"
        stripe
        border
        size="small"
        max-height="360"
        class="deploy-sql-terminal-dialog__table"
      >
        <el-table-column prop="index" :label="t('deployCenter.sqlTerminalBatchIndex')" width="64" align="center" />
        <el-table-column prop="sql" :label="t('deployCenter.sqlTerminalBatchSql')" min-width="280" show-overflow-tooltip />
        <el-table-column
          prop="affectedRows"
          :label="t('deployCenter.sqlTerminalBatchAffected')"
          width="100"
          align="center"
        />
      </el-table>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { VideoPlay } from '@element-plus/icons-vue'
import { executeDeploySql, type DeploySqlExecuteResult, type SqlTerminalTarget } from '@/api/deploySqlTerminal'
import { formatSql } from '@/utils/formatSql'

const props = defineProps<{
  modelValue: boolean
  initialSql?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const target = ref<SqlTerminalTarget>('local')
const sql = ref('')
const running = ref(false)
const result = ref<DeploySqlExecuteResult | null>(null)

const tableRows = computed(() => {
  if (!result.value || result.value.statementType !== 'query') {
    return []
  }
  return result.value.rows.map((row) => {
    const record: Record<string, unknown> = {}
    result.value!.columns.forEach((column, index) => {
      const value = row[index]
      record[column] = value === null || value === undefined ? 'NULL' : value
    })
    return record
  })
})

function beautify() {
  if (!sql.value.trim()) {
    return
  }
  sql.value = formatSql(sql.value)
}

function clearSql() {
  sql.value = ''
  result.value = null
}

async function runSql() {
  if (!sql.value.trim()) {
    return
  }
  running.value = true
  try {
    result.value = await executeDeploySql(target.value, sql.value)
  } catch {
    result.value = null
  } finally {
    running.value = false
  }
}

function handleClosed() {
  result.value = null
}

watch(visible, (open) => {
  if (open) {
    target.value = 'local'
    sql.value = props.initialSql?.trim() ? props.initialSql : ''
    result.value = null
  }
})
</script>

<style scoped lang="scss">
.deploy-sql-terminal-dialog {
  &__toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
    flex-wrap: wrap;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }

  &__run-btn {
    padding: 10px 22px;
    font-size: 14px;
    font-weight: 600;

    .el-icon {
      margin-right: 6px;
      font-size: 16px;
    }
  }

  &__action-btn {
    padding: 10px 18px;
    font-size: 14px;
  }

  &__editor {
    :deep(textarea) {
      font-family: Consolas, 'Courier New', monospace;
      font-size: 13px;
      line-height: 1.55;
    }
  }

  &__result {
    margin-top: 16px;
    padding-top: 14px;
    border-top: 1px solid #e8ecf2;
  }

  &__result-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 10px;
    font-size: 12px;
    color: #6b7280;
    font-weight: 600;
  }

  &__table {
    width: 100%;
  }

  &__empty {
    margin: 0;
    font-size: 13px;
    color: #6b7280;
  }
}
</style>
