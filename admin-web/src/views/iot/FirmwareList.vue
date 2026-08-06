<template>
  <div class="firmware-list iot-panel">
    <div class="iot-panel__toolbar">
      <el-input
        v-model="keyword"
        :placeholder="t('iot.firmware.searchPlaceholder')"
        clearable
        class="iot-panel__search"
        @keyup.enter="onSearch"
        @clear="onSearch"
      />
      <div class="iot-panel__toolbar-spacer" />
      <el-button :loading="loading" @click="onRefresh">{{ t('iot.firmware.refresh') }}</el-button>
      <el-button type="primary" @click="openUpload">{{ t('iot.firmware.upload') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="records" border row-key="id" class="iot-table">
      <el-table-column prop="id" :label="t('iot.firmware.id')" width="70" />
      <el-table-column prop="version" :label="t('iot.firmware.version')" width="130" />
      <el-table-column :label="t('iot.firmware.size')" width="110">
        <template #default="{ row }">{{ formatBytes(row.size) }}</template>
      </el-table-column>
      <el-table-column :label="t('iot.firmware.force')" width="90">
        <template #default="{ row }">
          <el-tag :type="row.force ? 'danger' : 'info'" size="small">
            {{ row.force ? t('iot.firmware.forceYes') : t('iot.firmware.forceNo') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="releaseNote" :label="t('iot.firmware.releaseNote')" min-width="200" show-overflow-tooltip />
      <el-table-column :label="t('iot.firmware.createdAt')" width="170">
        <template #default="{ row }">{{ formatIotTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column
        :label="t('iot.firmware.actions')"
        width="200"
        fixed="right"
        align="center"
        :class-name="TABLE_ACTIONS_CELL_CLASS"
      >
        <template #default="{ row }">
          <div class="table-actions-cell-inner" @click.stop>
            <el-button link type="success" :title="t('iot.firmware.publish')" @click.stop="onPublish(row)">
              <el-icon><Promotion /></el-icon>
            </el-button>
            <el-button link type="warning" :title="t('iot.firmware.forceUpgrade')" @click.stop="onForceUpgrade(row)">
              <el-icon><Top /></el-icon>
            </el-button>
            <el-button link type="danger" :title="t('iot.firmware.delete')" @click.stop="onDelete(row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <TablePagination
      :page="page"
      :page-size="pageSize"
      :total="total"
      @update:page="onPageChange"
      @update:page-size="onSizeChange"
    />

    <div class="iot-section-title">{{ t('iot.ota.recordsTitle') }}</div>
    <el-table v-loading="otaLoading" :data="otaRecords" border row-key="id" class="iot-table iot-table--ota">
      <el-table-column prop="id" :label="t('iot.ota.id')" width="70" />
      <el-table-column :label="t('iot.ota.deviceName')" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ resolveDeviceName(row, (key) => t(key)) }}</template>
      </el-table-column>
      <el-table-column prop="firmwareVersion" :label="t('iot.ota.firmwareVersion')" width="130" />
      <el-table-column :label="t('iot.ota.state')" width="110">
        <template #default="{ row }">
          <el-tag :type="otaMeta(row.state).tagType" size="small">
            {{ t(otaMeta(row.state).label) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('iot.ota.progress')" width="150">
        <template #default="{ row }">
          <el-progress
            :percentage="normalizeProgress(row.progress)"
            :status="progressStatus(row.state)"
            :stroke-width="8"
          />
        </template>
      </el-table-column>
      <el-table-column :label="t('iot.ota.startedAt')" width="170">
        <template #default="{ row }">{{ formatIotTime(row.startedAt) }}</template>
      </el-table-column>
      <el-table-column :label="t('iot.ota.finishedAt')" width="170">
        <template #default="{ row }">{{ formatIotTime(row.finishedAt) }}</template>
      </el-table-column>
    </el-table>

    <TablePagination
      :page="otaPage"
      :page-size="otaPageSize"
      :total="otaTotal"
      @update:page="onOtaPageChange"
      @update:page-size="onOtaSizeChange"
    />

    <el-dialog
      v-model="uploadVisible"
      :title="t('iot.firmware.uploadTitle')"
      width="520px"
      destroy-on-close
      class="iot-upload-dialog"
    >
      <el-form :model="uploadForm" label-width="96px" @submit.prevent>
        <el-form-item :label="t('iot.firmware.version')" required>
          <el-input v-model="uploadForm.version" :placeholder="t('iot.firmware.versionPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('iot.firmware.file')" required>
          <el-upload
            drag
            :auto-upload="false"
            :limit="1"
            accept=".bin,.img,.zip"
            :on-change="onUploadFileChange"
            :on-remove="onUploadFileRemove"
            :file-list="uploadFileList"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">{{ t('iot.firmware.dropHint') }}</div>
          </el-upload>
        </el-form-item>
        <el-form-item :label="t('iot.firmware.releaseNote')">
          <el-input v-model="uploadForm.releaseNote" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item :label="t('iot.firmware.force')">
          <el-switch v-model="uploadForm.force" />
          <span class="iot-upload-dialog__force-hint">{{ t('iot.firmware.forceHint') }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">{{ t('iot.common.cancel') }}</el-button>
        <el-button type="primary" :loading="uploading" @click="onUploadSave">
          {{ t('iot.common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type UploadFile, type UploadUserFile } from 'element-plus'
import { Delete, Promotion, Top, UploadFilled } from '@element-plus/icons-vue'
import {
  deleteIotFirmware,
  fetchIotFirmwares,
  fetchIotOtaRecords,
  forceUpgradeIotFirmware,
  publishIotFirmware,
  uploadIotFirmware,
  type IotFirmware,
  type IotOtaRecord,
  type IotOtaState,
} from '@/api/iot'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { TABLE_ACTIONS_CELL_CLASS } from '@/constants/table'
import { formatBytes, formatIotTime, resolveDeviceName, resolveOtaStateMeta } from './iotFormat'

const { t } = useI18n()

const keyword = ref('')
const uploadVisible = ref(false)
const uploading = ref(false)
const uploadFileList = ref<UploadUserFile[]>([])
const uploadForm = reactive({
  version: '',
  releaseNote: '',
  force: false,
})

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination((p, ps) =>
  fetchIotFirmwares({
    page: p,
    pageSize: ps,
    ...(keyword.value.trim() ? { keyword: keyword.value.trim() } : {}),
  }),
)

const {
  page: otaPage,
  pageSize: otaPageSize,
  total: otaTotal,
  records: otaRecords,
  loading: otaLoading,
  load: loadOta,
  onPageChange: onOtaPageChange,
  onSizeChange: onOtaSizeChange,
} = usePagination<IotOtaRecord>((p, ps) =>
  fetchIotOtaRecords({
    page: p,
    pageSize: ps,
  }),
)

function otaMeta(state?: IotOtaState) {
  return resolveOtaStateMeta(state)
}

function normalizeProgress(progress?: number): number {
  const value = Math.round(progress ?? 0)
  if (!Number.isFinite(value)) return 0
  return Math.min(100, Math.max(0, value))
}

function progressStatus(state?: IotOtaState): 'success' | 'exception' | undefined {
  if (state === 'SUCCESS') return 'success'
  if (state === 'FAILED') return 'exception'
  return undefined
}

async function onSearch() {
  await load(true)
}

async function onRefresh() {
  await Promise.all([load(true), loadOta(true)])
}

function openUpload() {
  uploadForm.version = ''
  uploadForm.releaseNote = ''
  uploadForm.force = false
  uploadFileList.value = []
  uploadVisible.value = true
}

function onUploadFileChange(_file: UploadFile, files: UploadUserFile[]) {
  uploadFileList.value = files
}

function onUploadFileRemove() {
  uploadFileList.value = []
}

async function onUploadSave() {
  const version = uploadForm.version.trim()
  if (!version) {
    ElMessage.warning(t('iot.firmware.versionRequired'))
    return
  }
  const file = uploadFileList.value[0]?.raw
  if (!file) {
    ElMessage.warning(t('iot.firmware.fileRequired'))
    return
  }
  uploading.value = true
  try {
    await uploadIotFirmware({
      file,
      version,
      force: uploadForm.force,
      releaseNote: uploadForm.releaseNote.trim() || undefined,
    })
    ElMessage.success(t('iot.firmware.uploadSuccess'))
    uploadVisible.value = false
    await load(true)
  } finally {
    uploading.value = false
  }
}

async function onPublish(row: IotFirmware) {
  await ElMessageBox.confirm(t('iot.firmware.publishConfirm', { version: row.version }), t('iot.common.confirmTitle'), {
    type: 'warning',
  })
  await publishIotFirmware(row.id, false)
  ElMessage.success(t('iot.firmware.publishSuccess'))
}

async function onForceUpgrade(row: IotFirmware) {
  await ElMessageBox.confirm(
    t('iot.firmware.forceUpgradeConfirm', { version: row.version }),
    t('iot.common.confirmTitle'),
    { type: 'warning', confirmButtonClass: 'el-button--danger' },
  )
  await forceUpgradeIotFirmware(row.id)
  ElMessage.success(t('iot.firmware.forceUpgradeSuccess'))
  await loadOta(true)
}

async function onDelete(row: IotFirmware) {
  await ElMessageBox.confirm(t('iot.firmware.deleteConfirm', { version: row.version }), t('iot.common.confirmTitle'), {
    type: 'warning',
  })
  await deleteIotFirmware(row.id)
  ElMessage.success(t('iot.firmware.deleteSuccess'))
  await load(true)
}

onMounted(() => {
  void load()
  void loadOta()
})
</script>

<style scoped lang="scss">
.iot-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.iot-panel__toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.iot-panel__search {
  width: 260px;
}

.iot-panel__toolbar-spacer {
  flex: 1;
  min-width: 12px;
}

.iot-table {
  flex-shrink: 0;
}

.iot-table--ota {
  margin-top: 0;
}

.iot-section-title {
  margin: 20px 0 12px;
  font-size: 15px;
  font-weight: 700;
  color: var(--wr-text, #333);
}

.iot-upload-dialog__force-hint {
  margin-left: 10px;
  font-size: 12px;
  color: var(--wr-muted, #999);
}
</style>
