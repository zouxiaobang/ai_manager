<template>
  <el-dialog
    v-model="visible"
    :title="t('sysImport.mappingTitle')"
    width="760px"
    destroy-on-close
    @open="onOpen"
  >
    <el-form label-width="100px" label-position="left">
      <el-form-item :label="t('sysImport.platform')" required>
        <el-input :model-value="platformName || '—'" disabled />
      </el-form-item>
      <el-form-item :label="t('sysImport.profileName')" required>
        <el-input v-model="form.name" :placeholder="t('sysImport.profileNamePlaceholder')" />
      </el-form-item>
      <el-form-item v-if="showProfileSelect" :label="t('sysImport.loadProfile')">
        <el-select
          v-model="selectedProfileId"
          clearable
          filterable
          style="width: 100%"
          :placeholder="t('sysImport.loadProfilePlaceholder')"
          @change="onProfileSelect"
        >
          <el-option v-for="p in profiles" :key="p.id" :label="p.name" :value="p.id!" />
        </el-select>
      </el-form-item>
    </el-form>

    <el-table :data="fieldRows" border size="small" max-height="360">
      <el-table-column :label="t('sysImport.backendField')" min-width="160">
        <template #default="{ row }">
          <span>{{ fieldLabel(row) }}</span>
          <el-tag v-if="row.required" size="small" type="danger" class="req-tag">{{ t('sysImport.required') }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('sysImport.docColumn')" min-width="200">
        <template #default="{ row }">
          <el-select
            v-model="form.columnMapping[row.key]"
            clearable
            filterable
            allow-create
            default-first-option
            style="width: 100%"
            :placeholder="t('sysImport.docColumnPlaceholder')"
          >
            <el-option v-for="col in docColumns" :key="col" :label="col" :value="col" />
          </el-select>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="visible = false">{{ t('ecommerce.common.cancel') }}</el-button>
      <el-button type="primary" :loading="saving" @click="onSave">{{ t('sysImport.saveMapping') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
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
import { DEFAULT_STATUS_MAPPING } from '@/constants/importStatusMapping'
import { filterImportFields, sanitizeImportColumnMapping } from '@/constants/importFieldKeys'
import { autoMatchColumnMapping } from '@/utils/importColumnMapping'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    bizType?: string
    platformId?: number
    platformName?: string
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

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const saving = ref(false)
const fields = ref<SysImportField[]>([])
const profiles = ref<SysImportProfile[]>([])
const selectedProfileId = ref<number | undefined>()
/** 已载入保存的配置时，不再自动覆盖用户留空的映射 */
const mappingLoadedFromProfile = ref(false)
const form = reactive({
  id: undefined as number | undefined,
  name: '',
  columnMapping: {} as Record<string, string>,
  valueMapping: { ...DEFAULT_STATUS_MAPPING } as Record<string, string>,
})

const showProfileSelect = computed(() => profiles.value.length > 0)
const fieldRows = computed(() => fields.value)

function fieldLabel(row: SysImportField) {
  return locale.value.startsWith('zh') ? row.labelZh : row.labelEn
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
  fields.value = filterImportFields(await fetchImportFields(props.bizType))
  profiles.value = await fetchImportProfiles(props.bizType, props.platformId, props.shopId)
  form.id = props.initialProfileId ?? undefined
  form.name = defaultProfileName()
  form.columnMapping = {}
  form.valueMapping = { ...DEFAULT_STATUS_MAPPING }
  mappingLoadedFromProfile.value = false
  resetMapping()
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
    autoMatchColumns()
  }
}

function autoMatchColumns() {
  form.columnMapping = autoMatchColumnMapping(fields.value, props.docColumns, form.columnMapping)
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
    : { ...DEFAULT_STATUS_MAPPING }
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
    if (visible.value && !mappingLoadedFromProfile.value) autoMatchColumns()
  },
)
</script>

<style scoped>
.req-tag {
  margin-left: 6px;
  vertical-align: middle;
}
</style>
