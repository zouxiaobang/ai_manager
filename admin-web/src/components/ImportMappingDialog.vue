<template>
  <el-dialog
    v-model="visible"
    :title="t('sysImport.mappingTitle')"
    width="960px"
    class="import-mapping-dialog"
    destroy-on-close
    append-to-body
    align-center
    @open="onOpen"
  >
    <div class="mapping-dialog-inner">
    <div class="mapping-dialog-meta">
      <div class="mapping-meta-card mapping-meta-card--platform">
        <div class="mapping-platform-card">
          <img :src="platformIconUrl" alt="" class="mapping-platform-card__icon" />
          <span class="mapping-platform-card__name">{{ platformName || '—' }}</span>
        </div>
      </div>
      <div class="mapping-meta-card mapping-meta-card--grow">
        <span class="mapping-meta-card__label">{{ t('sysImport.profileName') }}</span>
        <el-input
          v-model="form.name"
          :placeholder="t('sysImport.profileNamePlaceholder')"
        />
      </div>
      <div v-if="showProfileSelect" class="mapping-meta-card mapping-meta-card--grow">
        <span class="mapping-meta-card__label">{{ t('sysImport.loadProfile') }}</span>
        <el-select
          v-model="selectedProfileId"
          clearable
          filterable
          :placeholder="t('sysImport.loadProfilePlaceholder')"
          @change="onProfileSelect"
        >
          <el-option v-for="p in profiles" :key="p.id" :label="p.name" :value="p.id!" />
        </el-select>
      </div>
      <el-tooltip :content="t('sysImport.mappingAutoMatchHint')" placement="top">
        <el-button type="primary" link class="mapping-auto-match" @click="autoMatchColumns">
          {{ t('sysImport.mappingAutoMatch') }}
        </el-button>
      </el-tooltip>
    </div>

    <div class="mapping-progress">
      <span class="mapping-progress__label">
        {{ t('sysImport.mappingRequiredProgress', { mapped: requiredMappedCount, total: requiredTotalCount }) }}
      </span>
      <el-progress
        :percentage="requiredProgressPercent"
        :stroke-width="8"
        :show-text="false"
        :status="requiredProgressPercent >= 100 ? 'success' : undefined"
        class="mapping-progress__bar"
      />
    </div>

    <div class="mapping-dialog-body">
      <aside class="mapping-doc-panel">
        <h5 class="mapping-panel-title">
          {{ t('sysImport.mappingDocColumns', { count: docColumns.length }) }}
        </h5>
        <el-input
          v-model="docColumnSearch"
          clearable
          class="mapping-doc-search"
          :placeholder="t('sysImport.mappingDocColumnSearch')"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <div class="mapping-doc-tags">
          <button
            v-for="col in filteredDocColumns"
            :key="col"
            type="button"
            class="mapping-doc-tag"
            :class="{
              'is-used': isDocColumnUsed(col),
              'is-clickable': isDocColumnUsed(col),
            }"
            :title="docColumnUsageTitle(col)"
            @click="onDocColumnClick(col)"
          >
            {{ col }}
          </button>
          <p v-if="!docColumns.length" class="mapping-doc-empty">{{ t('sysImport.mappingDocColumnsEmpty') }}</p>
          <p v-else-if="!filteredDocColumns.length" class="mapping-doc-empty">
            {{ t('sysImport.mappingDocColumnSearchEmpty') }}
          </p>
        </div>
      </aside>

      <main class="mapping-fields-panel">
        <h5 class="mapping-panel-title">{{ t('sysImport.mappingSystemFields') }}</h5>
        <div class="mapping-field-groups">
          <section
            v-for="group in fieldGroups"
            :key="group.id"
            class="mapping-field-group"
          >
            <button
              type="button"
              class="mapping-field-group__header"
              @click="toggleGroup(group.id)"
            >
              <span class="mapping-field-group__accent" aria-hidden="true" />
              <span class="mapping-field-group__title">{{ groupLabel(group) }}</span>
              <span class="mapping-field-group__count">
                {{ mappedCountInGroup(group) }}/{{ group.fields.length }}
              </span>
              <el-icon class="mapping-field-group__chevron" :class="{ 'is-expanded': isGroupExpanded(group.id) }">
                <ArrowDown />
              </el-icon>
            </button>
            <div v-show="isGroupExpanded(group.id)" class="mapping-field-group__body">
              <div
                v-for="field in group.fields"
                :key="field.key"
                :ref="(el) => setFieldRowRef(field.key, el)"
                class="mapping-field-row"
                :class="{
                  'is-mapped': isFieldMapped(field.key),
                  'is-flash-highlight': flashFieldKeys.has(field.key),
                }"
              >
                <div class="mapping-field-row__label">
                  <span>{{ fieldLabel(field) }}</span>
                  <el-tag v-if="field.required" type="danger" class="req-tag">
                    {{ t('sysImport.required') }}
                  </el-tag>
                </div>
                <el-select
                  v-model="form.columnMapping[field.key]"
                  clearable
                  filterable
                  allow-create
                  default-first-option
                  class="mapping-field-row__select"
                  :class="{ 'is-mapped': isFieldMapped(field.key) }"
                  :placeholder="t('sysImport.docColumnPlaceholder')"
                >
                  <el-option v-for="col in docColumns" :key="col" :label="col" :value="col" />
                </el-select>
                <span
                  class="mapping-field-row__status"
                  :class="isFieldMapped(field.key) ? 'is-mapped' : 'is-unmapped'"
                  :title="isFieldMapped(field.key) ? t('sysImport.mappingMatched') : t('sysImport.mappingUnmapped')"
                >
                  <el-icon v-if="isFieldMapped(field.key)"><CircleCheckFilled /></el-icon>
                  <el-icon v-else><RemoveFilled /></el-icon>
                </span>
              </div>
            </div>
          </section>
        </div>
      </main>
    </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">{{ t('ecommerce.common.cancel') }}</el-button>
      <el-button type="primary" :loading="saving" @click="onSave">{{ t('sysImport.saveMapping') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { ArrowDown, CircleCheckFilled, RemoveFilled, Search } from '@element-plus/icons-vue'
import {
  BIZ_SALES_ORDER,
  createImportProfile,
  defaultPlatformProfileName,
  fetchImportFields,
  fetchImportProfiles,
  updateImportProfile,
  type SysImportField,
  type SysImportProfile,
} from '@/api/sys/import'
import { filterImportFields, sanitizeImportColumnMapping } from '@/constants/importFieldKeys'
import { groupImportFields, type GroupedImportField } from '@/constants/importFieldGroups'
import { buildColumnMappingForUpload } from '@/utils/importColumnMapping'
import { resolvePlatformIcon } from '@/utils/platformVisual'
import { useEcSettingsStore } from '@/stores/ecSettings'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    bizType?: string
    platformId?: number
    platformName?: string
    platformCode?: number | null
    shopId?: number
    docColumns: string[]
    fileType?: string
    headerRow?: number
    dataStartRow?: number
    initialProfileId?: number | null
  }>(),
  {
    bizType: BIZ_SALES_ORDER,
    fileType: 'XLSX',
    headerRow: 1,
    dataStartRow: 2,
    initialProfileId: null,
  },
)

const emit = defineEmits<{
  'update:modelValue': [boolean]
  saved: [SysImportProfile]
}>()

const { t, locale } = useI18n()
const ecSettings = useEcSettingsStore()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const saving = ref(false)
const fields = ref<SysImportField[]>([])
const profiles = ref<SysImportProfile[]>([])
const selectedProfileId = ref<number | undefined>()
const expandedGroups = ref<Record<string, boolean>>({})
const docColumnSearch = ref('')
const flashFieldKeys = ref<Set<string>>(new Set())
const fieldRowRefs = ref<Record<string, HTMLElement>>({})
let flashTimer: ReturnType<typeof setTimeout> | undefined
/** 已载入保存的配置时，不再自动覆盖用户留空的映射 */
const mappingLoadedFromProfile = ref(false)
const form = reactive({
  id: undefined as number | undefined,
  name: '',
  columnMapping: {} as Record<string, string>,
  valueMapping: {} as Record<string, string>,
})

const showProfileSelect = computed(() => profiles.value.length > 0)
const fieldGroups = computed(() => groupImportFields(fields.value, props.bizType))
const platformIconUrl = computed(() => resolvePlatformIcon(props.platformName, props.platformCode))

const filteredDocColumns = computed(() => {
  const query = docColumnSearch.value.trim().toLowerCase()
  if (!query) return props.docColumns
  return props.docColumns.filter((col) => col.toLowerCase().includes(query))
})

const usedDocColumns = computed(() => {
  const used = new Set<string>()
  for (const value of Object.values(form.columnMapping)) {
    const col = value?.trim()
    if (col) used.add(col)
  }
  return used
})

const requiredFields = computed(() => fields.value.filter((f) => f.required))
const requiredTotalCount = computed(() => requiredFields.value.length)
const requiredMappedCount = computed(() =>
  requiredFields.value.filter((f) => isFieldMapped(f.key)).length,
)
const requiredProgressPercent = computed(() => {
  if (!requiredTotalCount.value) return 100
  return Math.round((requiredMappedCount.value / requiredTotalCount.value) * 100)
})

function fieldLabel(row: SysImportField) {
  return locale.value.startsWith('zh') ? row.labelZh : row.labelEn
}

function groupLabel(group: GroupedImportField) {
  return locale.value.startsWith('zh') ? group.labelZh : group.labelEn
}

function isFieldMapped(fieldKey: string) {
  return !!form.columnMapping[fieldKey]?.trim()
}

function isDocColumnUsed(col: string) {
  return usedDocColumns.value.has(col)
}

function docColumnUsageTitle(col: string) {
  if (!isDocColumnUsed(col)) return `${col}（${t('sysImport.mappingDocColumnUnmappedHint')}）`
  const mappedFields = fields.value
    .filter((f) => form.columnMapping[f.key]?.trim() === col)
    .map((f) => fieldLabel(f))
  return mappedFields.length ? `${col} → ${mappedFields.join('、')}` : col
}

function fieldKeysForDocColumn(col: string): string[] {
  return fields.value
    .filter((f) => form.columnMapping[f.key]?.trim() === col)
    .map((f) => f.key)
}

function setFieldRowRef(fieldKey: string, el: unknown) {
  if (el instanceof HTMLElement) {
    fieldRowRefs.value[fieldKey] = el
  } else if (!el) {
    delete fieldRowRefs.value[fieldKey]
  }
}

function flashFieldRows(fieldKeys: string[]) {
  if (flashTimer) clearTimeout(flashTimer)
  flashFieldKeys.value = new Set(fieldKeys)
  flashTimer = setTimeout(() => {
    flashFieldKeys.value = new Set()
    flashTimer = undefined
  }, 1800)
}

async function onDocColumnClick(col: string) {
  const keys = fieldKeysForDocColumn(col)
  if (!keys.length) {
    ElMessage.info(t('sysImport.mappingDocColumnUnmapped'))
    return
  }
  for (const group of fieldGroups.value) {
    if (group.fields.some((f) => keys.includes(f.key))) {
      expandedGroups.value[group.id] = true
    }
  }
  await nextTick()
  const firstEl = fieldRowRefs.value[keys[0]]
  firstEl?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  flashFieldRows(keys)
}

function mappedCountInGroup(group: GroupedImportField) {
  return group.fields.filter((f) => isFieldMapped(f.key)).length
}

function isGroupExpanded(groupId: string) {
  return expandedGroups.value[groupId] !== false
}

function toggleGroup(groupId: string) {
  expandedGroups.value[groupId] = !isGroupExpanded(groupId)
}

function initExpandedGroups() {
  const next: Record<string, boolean> = {}
  for (const group of fieldGroups.value) {
    next[group.id] = true
  }
  expandedGroups.value = next
}

function defaultProfileName() {
  if (props.platformName?.trim()) {
    return defaultPlatformProfileName(props.platformName.trim())
  }
  return t('sysImport.defaultProfileName')
}

function resetMapping() {
  const map: Record<string, string> = {}
  for (const f of fields.value) {
    map[f.key] = form.columnMapping[f.key] ?? ''
  }
  form.columnMapping = map
}

async function onOpen() {
  if (!props.platformId) {
    ElMessage.warning(t('sysImport.platformRequired'))
    visible.value = false
    return
  }
  await ecSettings.ensureLoaded()
  fields.value = filterImportFields(await fetchImportFields(props.bizType))
  profiles.value = await fetchImportProfiles(props.bizType, props.platformId, props.shopId)
  form.id = props.initialProfileId ?? undefined
  form.name = defaultProfileName()
  form.columnMapping = {}
  form.valueMapping = { ...ecSettings.statusMappingForImport }
  mappingLoadedFromProfile.value = false
  docColumnSearch.value = ''
  flashFieldKeys.value = new Set()
  resetMapping()
  initExpandedGroups()
  selectedProfileId.value = props.initialProfileId ?? undefined
  if (selectedProfileId.value) {
    await onProfileSelect(selectedProfileId.value)
  } else if (profiles.value.length) {
    const preferredName = defaultProfileName()
    const preferred = profiles.value.find((p) => p.name === preferredName) ?? profiles.value[0]
    if (preferred?.id) {
      selectedProfileId.value = preferred.id
      await onProfileSelect(preferred.id)
    }
  }
  if (!mappingLoadedFromProfile.value) {
    applyAutoMatch(false)
  }
  initExpandedGroups()
}

function applyAutoMatch(showMessage: boolean) {
  form.columnMapping = buildColumnMappingForUpload(
    fields.value,
    props.docColumns,
    props.platformName,
  )
  if (showMessage) {
    ElMessage.success(t('sysImport.mappingAutoMatchDone'))
  }
}

function autoMatchColumns() {
  applyAutoMatch(true)
}

async function onProfileSelect(id?: number) {
  if (!id) {
    mappingLoadedFromProfile.value = false
    return
  }
  const p = profiles.value.find((x) => x.id === id)
  if (!p) return
  form.id = p.id
  form.name = p.name
  form.columnMapping = sanitizeImportColumnMapping({ ...p.columnMapping })
  form.valueMapping = p.valueMapping && Object.keys(p.valueMapping).length
    ? { ...p.valueMapping }
    : { ...ecSettings.statusMappingForImport }
  resetMapping()
  mappingLoadedFromProfile.value = true
}

async function onSave() {
  if (!props.platformId) {
    ElMessage.warning(t('sysImport.platformRequired'))
    return
  }
  if (!form.name.trim()) {
    ElMessage.warning(t('sysImport.profileNameRequired'))
    return
  }
  for (const f of fields.value) {
    if (f.required && !form.columnMapping[f.key]?.trim()) {
      ElMessage.warning(t('sysImport.requiredFieldMissing', { field: fieldLabel(f) }))
      return
    }
  }
  resetMapping()
  const payload = {
    id: form.id,
    name: form.name.trim(),
    bizType: props.bizType,
    platformId: props.platformId,
    fileType: props.fileType,
    headerRow: props.headerRow,
    dataStartRow: props.dataStartRow,
    columnMapping: { ...form.columnMapping },
    valueMapping: { ...form.valueMapping },
  }
  saving.value = true
  try {
    const saved = form.id
      ? await updateImportProfile(form.id, payload)
      : await createImportProfile(payload)
    const idx = profiles.value.findIndex((x) => x.id === saved.id)
    if (idx >= 0) {
      profiles.value[idx] = saved
    } else {
      profiles.value.push(saved)
    }
    mappingLoadedFromProfile.value = true
    form.columnMapping = { ...(saved.columnMapping ?? {}) }
    resetMapping()
    ElMessage.success(t('sysImport.saveSuccess'))
    emit('saved', saved)
    visible.value = false
  } finally {
    saving.value = false
  }
}

watch(
  () => props.docColumns,
  () => {
    if (visible.value && !mappingLoadedFromProfile.value) applyAutoMatch(false)
  },
)

watch(fieldGroups, () => {
  if (visible.value) initExpandedGroups()
})
</script>

<style scoped lang="scss">
.import-mapping-dialog {
  :deep(.el-dialog__body) {
    display: flex;
    flex-direction: column;
    overflow: hidden;
    padding: 16px 20px;
  }

  :deep(.el-dialog__footer) {
    flex-shrink: 0;
    padding: 12px 20px 16px;
    border-top: 1px solid var(--el-border-color-lighter);
  }
}

.mapping-dialog-inner {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
}

.mapping-dialog-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 10px 12px;
  margin-bottom: 14px;
  flex-shrink: 0;
}

.mapping-meta-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 120px;

  &--grow {
    flex: 1;
    min-width: 160px;
  }

  &--platform {
    flex-shrink: 0;
  }
}

.mapping-platform-card {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  min-width: 80px;
  padding: 10px 14px;
  border: 1px solid #fdba74;
  border-radius: 10px;
  background: #fff7ed;
  box-shadow: 0 1px 2px rgb(234 88 12 / 8%);
}

.mapping-platform-card__icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  flex-shrink: 0;
  object-fit: cover;
}

.mapping-platform-card__name {
  font-size: 14px;
  font-weight: 600;
  color: #ea580c;
  line-height: 1.2;
  text-align: left;
  white-space: nowrap;
}

.mapping-meta-card__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.mapping-auto-match {
  margin-left: auto;
  padding-bottom: 6px;
  font-size: 14px;
}

.mapping-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  flex-shrink: 0;
}

.mapping-progress__label {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.mapping-progress__bar {
  flex: 1;
  min-width: 0;
}

.mapping-dialog-body {
  display: grid;
  grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
  gap: 16px;
  flex: 1;
  min-height: 0;
  height: 400px;
  overflow: hidden;
}

.mapping-panel-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  flex-shrink: 0;
}

.mapping-doc-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
}

.mapping-doc-search {
  margin-bottom: 10px;
  flex-shrink: 0;
}

.mapping-doc-tags {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: 6px;
  padding-right: 2px;
}

.mapping-doc-tag {
  max-width: 100%;
  padding: 5px 11px;
  border-radius: 999px;
  border: 1px solid var(--el-border-color);
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.4;
  word-break: break-all;
  font-family: inherit;
  text-align: left;

  &.is-used {
    border-color: var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
  }

  &.is-clickable {
    cursor: pointer;

    &:hover {
      border-color: var(--el-color-primary);
      background: var(--el-color-primary-light-8);
    }
  }
}

.mapping-doc-empty {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-placeholder);
}

.mapping-fields-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.mapping-field-groups {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 2px;
}

.mapping-field-group {
  margin-bottom: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-bg-color);

  &:last-child {
    margin-bottom: 0;
  }
}

.mapping-field-group__header {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 12px 14px;
  border: none;
  background: var(--el-fill-color-light);
  cursor: pointer;
  text-align: left;

  &:hover {
    background: var(--el-fill-color);
  }
}

.mapping-field-group__accent {
  flex-shrink: 0;
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: var(--el-color-primary);
}

.mapping-field-group__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.mapping-field-group__count {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.mapping-field-group__chevron {
  margin-left: auto;
  font-size: 16px;
  color: var(--el-text-color-secondary);
  transition: transform 0.2s ease;

  &.is-expanded {
    transform: rotate(180deg);
  }
}

.mapping-field-group__body {
  padding: 8px 12px 12px;
}

.mapping-field-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(140px, 1.4fr) 28px;
  align-items: center;
  gap: 10px;
  padding: 8px 6px;
  margin: 0 -6px;
  border-radius: 6px;
  transition: background-color 0.15s ease;

  & + & {
    border-top: 1px dashed var(--el-border-color-extra-light);
  }

  &.is-mapped .mapping-field-row__label {
    color: var(--el-text-color-primary);
  }

  &.is-flash-highlight {
    animation: mapping-row-flash 0.6s ease-in-out 3;
  }
}

@keyframes mapping-row-flash {
  0%,
  100% {
    background-color: transparent;
  }

  50% {
    background-color: #dbeafe;
  }
}

.mapping-field-row__label {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.mapping-field-row__select {
  width: 100%;
  min-width: 0;

  &.is-mapped :deep(.el-select__selected-item) {
    color: #2563eb;
    font-weight: 500;
  }

  &.is-mapped :deep(.el-select__wrapper) {
    box-shadow: 0 0 0 1px #93c5fd inset;
  }
}

.mapping-field-row__status {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;

  &.is-mapped {
    color: var(--el-color-success);
  }

  &.is-unmapped {
    color: var(--el-text-color-placeholder);
  }
}

.req-tag {
  vertical-align: middle;
}

@media (max-width: 768px) {
  .mapping-dialog-body {
    grid-template-columns: 1fr;
    grid-template-rows: 160px minmax(0, 1fr);
    height: auto;
    max-height: 520px;
  }
}
</style>
