<template>
  <div class="settings-status-map">
    <el-form-item :label="t('ecommerce.settings.defaultLineStatus')">
      <el-select v-model="defaultStatus" class="settings-status-map__default">
        <el-option
          v-for="opt in systemStatusOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
    </el-form-item>

    <div class="settings-status-map__rows">
      <div v-for="(row, index) in rows" :key="row.key" class="settings-status-map__row">
        <el-input v-model="row.platformStatus" :placeholder="t('sysImport.platformStatusPlaceholder')" />
        <span class="settings-status-map__arrow">→</span>
        <el-select v-model="row.systemStatus">
          <el-option
            v-for="opt in systemStatusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-button link type="danger" :disabled="rows.length <= 1" @click="removeRow(index)">
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
    </div>
    <el-button type="primary" link @click="addRow">
      <el-icon><Plus /></el-icon>
      {{ t('sysImport.addStatusRow') }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Delete, Plus } from '@element-plus/icons-vue'
import {
  DEFAULT_STATUS_MAPPING,
  mappingToRows,
  rowsToMapping,
  type ImportLineStatus,
  type StatusMappingRow,
} from '@/constants/importStatusMapping'

const props = defineProps<{
  defaultLineStatus: ImportLineStatus
  statusMapping: Record<string, string>
}>()

const emit = defineEmits<{
  'update:defaultLineStatus': [ImportLineStatus]
  'update:statusMapping': [Record<string, string>]
}>()

const { t } = useI18n()
const rows = ref<StatusMappingRow[]>([])

const defaultStatus = computed({
  get: () => props.defaultLineStatus,
  set: (value: ImportLineStatus) => emit('update:defaultLineStatus', value),
})

const systemStatusOptions = computed(() => [
  { value: 'PAID' as const, label: t('ecommerce.salesOrder.importLineStatusPaid') },
  { value: 'SHIPPED' as const, label: t('ecommerce.salesOrder.importLineStatusShipped') },
  { value: 'COMPLETED' as const, label: t('ecommerce.salesOrder.importLineStatusCompleted') },
  { value: 'CANCELLED' as const, label: t('ecommerce.salesOrder.importLineStatusCancelled') },
  { value: 'PARTIAL_REFUND' as const, label: t('ecommerce.salesOrder.importLineStatusPartialRefund') },
  { value: 'REFUNDED' as const, label: t('ecommerce.salesOrder.importLineStatusRefunded') },
  { value: 'RETURNED' as const, label: t('ecommerce.salesOrder.importLineStatusReturned') },
])

function syncRowsFromMapping(map?: Record<string, string>) {
  const source = map && Object.keys(map).length ? map : DEFAULT_STATUS_MAPPING
  rows.value = mappingToRows(source)
}

function emitMapping() {
  emit('update:statusMapping', rowsToMapping(rows.value))
}

function addRow() {
  rows.value.push({
    key: `status-${Date.now()}`,
    platformStatus: '',
    systemStatus: 'PAID',
  })
  emitMapping()
}

function removeRow(index: number) {
  rows.value.splice(index, 1)
  if (!rows.value.length) addRow()
  else emitMapping()
}

watch(
  () => props.statusMapping,
  (map) => syncRowsFromMapping(map),
  { immediate: true, deep: true },
)

watch(rows, () => emitMapping(), { deep: true })
</script>

<style scoped lang="scss">
.settings-status-map__default {
  width: 220px;
}

.settings-status-map__rows {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 8px;
}

.settings-status-map__row {
  display: grid;
  grid-template-columns: 1fr 24px 180px 32px;
  gap: 8px;
  align-items: center;
}

.settings-status-map__arrow {
  text-align: center;
  color: var(--el-text-color-secondary);
}
</style>
