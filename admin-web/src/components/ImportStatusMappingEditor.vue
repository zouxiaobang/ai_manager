<template>
  <div class="status-mapping-card">
    <button type="button" class="status-mapping-card__header" @click="toggleExpanded">
      <span class="status-mapping-card__title">{{ t('sysImport.statusMapping') }}</span>
      <el-tag size="small" class="status-mapping-required-tag">{{ t('sysImport.statusMappingRequired') }}</el-tag>
      <el-icon class="status-mapping-card__chevron" :class="{ 'is-expanded': expandedModel }">
        <ArrowUp />
      </el-icon>
    </button>

    <div v-show="expandedModel" class="status-mapping-card__body">
      <div class="status-mapping-alert">
        <el-icon class="status-mapping-alert__icon"><InfoFilled /></el-icon>
        <span>{{ t('sysImport.statusMappingInfo') }}</span>
      </div>

      <div class="status-mapping-rows">
        <div v-for="(row, index) in rows" :key="row.key" class="status-mapping-row">
          <el-input
            v-model="row.platformStatus"
            size="small"
            :placeholder="t('sysImport.platformStatusPlaceholder')"
            class="status-mapping-row__platform"
            @change="emitMapping"
          />
          <span class="status-mapping-row__arrow" aria-hidden="true">→</span>
          <el-select
            v-model="row.systemStatus"
            size="small"
            class="status-mapping-row__system"
            :class="systemStatusSelectClass(row.systemStatus)"
            @change="emitMapping"
          >
            <el-option
              v-for="opt in systemStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            >
              <span class="status-option" :class="`status-option--${opt.value.toLowerCase()}`">
                {{ opt.label }}
              </span>
            </el-option>
          </el-select>
          <button
            type="button"
            class="status-mapping-row__delete"
            :title="t('sysImport.removeRow')"
            @click="removeRow(index)"
          >
            <el-icon><Delete /></el-icon>
          </button>
        </div>
      </div>

      <button type="button" class="status-mapping-add" @click="addRow">
        + {{ t('sysImport.addStatusRow') }}
      </button>

      <div v-if="showSaveButton" class="status-mapping-footer">
        <el-button
          type="primary"
          class="status-mapping-footer__save"
          :loading="saving"
          :disabled="!profileId"
          @click="onSave"
        >
          {{ t('sysImport.saveStatusMapping') }}
        </el-button>
      </div>
      <p v-if="showSaveButton && !profileId" class="status-mapping-footer__hint">
        {{ t('sysImport.statusMappingNeedProfile') }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { ArrowUp, Delete, InfoFilled } from '@element-plus/icons-vue'
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
    expanded?: boolean
  }>(),
  {
    modelValue: undefined,
    profileId: null,
    showSaveButton: true,
    expanded: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [Record<string, string>]
  'update:expanded': [boolean]
  saved: [SysImportProfile]
}>()

const { t } = useI18n()

const saving = ref(false)
const rows = ref<StatusMappingRow[]>(mappingToRows())

const expandedModel = computed({
  get: () => props.expanded,
  set: (value: boolean) => emit('update:expanded', value),
})

const systemStatusOptions = computed(() => [
  { value: 'PAID' as ImportLineStatus, label: t('ecommerce.salesOrder.importLineStatusPaid') },
  { value: 'SHIPPED' as ImportLineStatus, label: t('ecommerce.salesOrder.importLineStatusShipped') },
  { value: 'COMPLETED' as ImportLineStatus, label: t('ecommerce.salesOrder.importLineStatusCompleted') },
  { value: 'CANCELLED' as ImportLineStatus, label: t('ecommerce.salesOrder.importLineStatusCancelled') },
  { value: 'PARTIAL_REFUND' as ImportLineStatus, label: t('ecommerce.salesOrder.importLineStatusPartialRefund') },
  { value: 'REFUNDED' as ImportLineStatus, label: t('ecommerce.salesOrder.importLineStatusRefunded') },
  { value: 'RETURNED' as ImportLineStatus, label: t('ecommerce.salesOrder.importLineStatusReturned') },
])

function systemStatusSelectClass(status: ImportLineStatus) {
  return `is-${status.toLowerCase().replace(/_/g, '-')}`
}

function syncRowsFromMapping(map?: Record<string, string> | null) {
  rows.value = mappingToRows(map)
}

function emitMapping() {
  if (props.modelValue !== undefined) {
    emit('update:modelValue', rowsToMapping(rows.value))
  }
}

function toggleExpanded() {
  expandedModel.value = !expandedModel.value
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
    expandedModel.value = false
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

<style scoped lang="scss">
.status-mapping-card {
  width: 100%;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
  overflow: hidden;
}

.status-mapping-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 12px;
  border: none;
  background: var(--el-bg-color);
  cursor: pointer;
  text-align: left;

  &:hover {
    background: var(--el-fill-color-light);
  }
}

.status-mapping-card__title {
  font-size: 14px;
  font-weight: 600;
  color: #111;
}

.status-mapping-required-tag {
  --el-tag-bg-color: #fff7ed;
  --el-tag-border-color: #fdba74;
  --el-tag-text-color: #ea580c;
}

.status-mapping-card__chevron {
  margin-left: auto;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  transition: transform 0.2s ease;

  &:not(.is-expanded) {
    transform: rotate(180deg);
  }
}

.status-mapping-card__body {
  padding: 0 12px 12px;
}

.status-mapping-alert {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  font-size: 12px;
  line-height: 1.55;
  color: #1e3a8a;
}

.status-mapping-alert__icon {
  flex-shrink: 0;
  margin-top: 1px;
  font-size: 15px;
  color: #2563eb;
}

.status-mapping-rows {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 240px;
  overflow-y: auto;
  margin-bottom: 10px;
  padding-right: 2px;
}

.status-mapping-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-blank);
}

.status-mapping-row__platform {
  flex: 1;
  min-width: 0;
}

.status-mapping-row__arrow {
  flex-shrink: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.status-mapping-row__system {
  flex: 1;
  min-width: 0;

  &.is-paid :deep(.el-select__wrapper) {
    background: #eff6ff;
    box-shadow: 0 0 0 1px #93c5fd inset;
  }

  &.is-paid :deep(.el-select__selected-item) {
    color: #2563eb;
  }

  &.is-shipped :deep(.el-select__wrapper) {
    background: #f0fdf4;
    box-shadow: 0 0 0 1px #86efac inset;
  }

  &.is-shipped :deep(.el-select__selected-item) {
    color: #16a34a;
  }

  &.is-completed :deep(.el-select__wrapper) {
    background: #f5f3ff;
    box-shadow: 0 0 0 1px #c4b5fd inset;
  }

  &.is-completed :deep(.el-select__selected-item) {
    color: #7c3aed;
  }

  &.is-cancelled :deep(.el-select__wrapper) {
    background: #f9fafb;
    box-shadow: 0 0 0 1px #d1d5db inset;
  }

  &.is-cancelled :deep(.el-select__selected-item) {
    color: #6b7280;
  }

  &.is-partial-refund,
  &.is-refunded,
  &.is-returned {
    :deep(.el-select__wrapper) {
      background: #fef2f2;
      box-shadow: 0 0 0 1px #fecaca inset;
    }

    :deep(.el-select__selected-item) {
      color: #dc2626;
    }
  }
}

.status-option {
  font-size: 13px;

  &--paid { color: #2563eb; }
  &--shipped { color: #16a34a; }
  &--completed { color: #7c3aed; }
  &--cancelled { color: #6b7280; }
  &--partial_refund,
  &--refunded,
  &--returned { color: #dc2626; }
}

.status-mapping-row__delete {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--el-text-color-secondary);
  cursor: pointer;

  &:hover {
    color: var(--el-color-danger);
    background: var(--el-fill-color-light);
  }
}

.status-mapping-add {
  width: 100%;
  margin-bottom: 12px;
  padding: 8px 12px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  background: var(--el-bg-color);
  color: var(--el-color-primary);
  font-size: 13px;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease;

  &:hover {
    background: var(--el-fill-color-light);
    border-color: var(--el-color-primary-light-5);
  }
}

.status-mapping-footer {
  width: 100%;
}

.status-mapping-footer__save {
  --el-button-bg-color: #ea580c;
  --el-button-border-color: #ea580c;
  --el-button-hover-bg-color: #c2410c;
  --el-button-hover-border-color: #c2410c;
  --el-button-active-bg-color: #9a3412;
  --el-button-active-border-color: #9a3412;
  width: 100%;
  min-height: 36px;
  padding: 8px 18px;
}

.status-mapping-footer__hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: center;
}
</style>
