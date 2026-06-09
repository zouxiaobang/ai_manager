<template>
  <div class="status-mapping">
    <div class="status-mapping-header">
      <span class="hint">{{ t('sysImport.statusMappingHint') }}</span>
    </div>
    <el-table :data="rows" border size="small" max-height="220" class="status-table">
      <el-table-column :label="t('sysImport.platformStatus')" min-width="180">
        <template #default="{ row }">
          <el-input
            v-model="row.platformStatus"
            size="small"
            :placeholder="t('sysImport.platformStatusPlaceholder')"
            @change="emitMapping"
          />
        </template>
      </el-table-column>
      <el-table-column :label="t('sysImport.systemStatus')" min-width="140">
        <template #default="{ row }">
          <el-select v-model="row.systemStatus" size="small" style="width: 100%" @change="emitMapping">
            <el-option
              v-for="opt in systemStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column width="56" align="center">
        <template #default="{ $index }">
          <el-button link type="danger" size="small" @click="removeRow($index)">{{ t('sysImport.removeRow') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="status-actions">
      <el-button size="small" @click="addRow">{{ t('sysImport.addStatusRow') }}</el-button>
      <el-button
        v-if="showSaveButton"
        size="small"
        type="primary"
        :loading="saving"
        :disabled="!profileId"
        @click="onSave"
      >
        {{ t('sysImport.saveStatusMapping') }}
      </el-button>
      <span v-if="showSaveButton && !profileId" class="hint">{{ t('sysImport.statusMappingNeedProfile') }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  BIZ_SALES_ORDER,
  fetchImportProfile,
  updateImportProfile,
  type SysImportProfile,
} from '@/api/sys/import'
import {
  DEFAULT_STATUS_MAPPING,
  mappingToRows,
  rowsToMapping,
  type ImportLineStatus,
  type StatusMappingRow,
} from '@/constants/importStatusMapping'

const props = withDefaults(
  defineProps<{
    modelValue?: Record<string, string>
    profileId?: number | null
    showSaveButton?: boolean
  }>(),
  {
    modelValue: undefined,
    profileId: null,
    showSaveButton: true,
  },
)

const emit = defineEmits<{
  'update:modelValue': [Record<string, string>]
  saved: [SysImportProfile]
}>()

const { t } = useI18n()

const saving = ref(false)
const rows = ref<StatusMappingRow[]>(mappingToRows())

const systemStatusOptions = computed(() => [
  { value: 'PAID' as ImportLineStatus, label: t('ecommerce.salesOrder.linePaid') },
  { value: 'SHIPPED' as ImportLineStatus, label: t('ecommerce.salesOrder.lineShipped') },
  { value: 'COMPLETED' as ImportLineStatus, label: t('ecommerce.salesOrder.lineCompleted') },
  { value: 'CANCELLED' as ImportLineStatus, label: t('ecommerce.salesOrder.lineCancelled') },
  { value: 'PARTIAL_REFUND' as ImportLineStatus, label: t('ecommerce.salesOrder.linePartialRefund') },
  { value: 'REFUNDED' as ImportLineStatus, label: t('ecommerce.salesOrder.lineRefunded') },
  { value: 'RETURNED' as ImportLineStatus, label: t('ecommerce.salesOrder.lineReturned') },
])

function syncRowsFromMapping(map?: Record<string, string> | null) {
  rows.value = mappingToRows(map)
}

function emitMapping() {
  if (props.modelValue !== undefined) {
    emit('update:modelValue', rowsToMapping(rows.value))
  }
}

function addRow() {
  rows.value.push({
    key: `status-new-${Date.now()}`,
    platformStatus: '',
    systemStatus: 'PAID',
  })
  emitMapping()
}

function removeRow(index: number) {
  rows.value.splice(index, 1)
  emitMapping()
}

async function loadFromProfile(id: number) {
  const profile = await fetchImportProfile(id)
  syncRowsFromMapping(profile.valueMapping)
  emitMapping()
}

async function onSave() {
  if (!props.profileId) {
    ElMessage.warning(t('sysImport.statusMappingNeedProfile'))
    return
  }
  const mapping = rowsToMapping(rows.value)
  if (!Object.keys(mapping).length) {
    ElMessage.warning(t('sysImport.statusMappingEmpty'))
    return
  }
  saving.value = true
  try {
    const profile = await fetchImportProfile(props.profileId)
    const saved = await updateImportProfile(props.profileId, {
      id: profile.id,
      name: profile.name,
      bizType: profile.bizType ?? BIZ_SALES_ORDER,
      platformId: profile.platformId,
      shopId: profile.shopId,
      fileType: profile.fileType,
      headerRow: profile.headerRow,
      dataStartRow: profile.dataStartRow,
      sheetName: profile.sheetName,
      columnMapping: profile.columnMapping ?? {},
      valueMapping: mapping,
      extraConfig: profile.extraConfig,
      remark: profile.remark,
    })
    ElMessage.success(t('sysImport.saveStatusSuccess'))
    emit('saved', saved)
    emit('update:modelValue', mapping)
  } finally {
    saving.value = false
  }
}

watch(
  () => props.modelValue,
  (map) => {
    if (map !== undefined) {
      syncRowsFromMapping(map)
    }
  },
  { immediate: true, deep: true },
)

watch(
  () => props.profileId,
  async (id) => {
    if (props.modelValue !== undefined || !id) {
      if (!id && props.modelValue === undefined) {
        syncRowsFromMapping(DEFAULT_STATUS_MAPPING)
      }
      return
    }
    await loadFromProfile(id)
  },
  { immediate: true },
)

defineExpose({
  getMapping: () => rowsToMapping(rows.value),
  setMapping: (map: Record<string, string>) => syncRowsFromMapping(map),
})
</script>

<style scoped>
.status-mapping {
  width: 100%;
}

.status-mapping-header {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.status-table {
  width: 100%;
}

.status-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}
</style>
