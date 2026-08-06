<template>
  <div class="device-list iot-panel">
    <div class="iot-panel__toolbar">
      <el-input
        v-model="keyword"
        :placeholder="t('iot.device.searchPlaceholder')"
        clearable
        class="iot-panel__search"
        @keyup.enter="onSearch"
        @clear="onSearch"
      />
      <el-select
        v-model="statusFilter"
        class="iot-panel__status-filter"
        :placeholder="t('iot.device.statusAll')"
        @change="onFilterChange"
      >
        <el-option :label="t('iot.device.statusAll')" value="" />
        <el-option :label="t('iot.device.statusOnline')" value="ONLINE" />
        <el-option :label="t('iot.device.statusOffline')" value="OFFLINE" />
        <el-option :label="t('iot.device.statusInactive')" value="INACTIVE" />
      </el-select>
      <div class="iot-panel__toolbar-spacer" />
      <el-button :loading="loading" @click="onRefresh">{{ t('iot.device.refresh') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="records" border row-key="id" class="iot-table">
      <el-table-column prop="id" :label="t('iot.device.id')" width="70" />
      <el-table-column prop="uuid" :label="t('iot.device.uuid')" min-width="170" show-overflow-tooltip />
      <el-table-column prop="mac" :label="t('iot.device.mac')" width="140" show-overflow-tooltip />
      <el-table-column prop="model" :label="t('iot.device.model')" width="130" show-overflow-tooltip />
      <el-table-column prop="chip" :label="t('iot.device.chip')" width="100" />
      <el-table-column prop="firmwareVersion" :label="t('iot.device.firmwareVersion')" width="120" />
      <el-table-column :label="t('iot.device.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="statusMeta(row.status).tagType" size="small">
            {{ t(statusMeta(row.status).label) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('iot.device.lastSeenAt')" width="170">
        <template #default="{ row }">
          {{ formatIotTime(row.lastSeenAt) }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('iot.device.actions')"
        width="150"
        fixed="right"
        align="center"
        :class-name="TABLE_ACTIONS_CELL_CLASS"
      >
        <template #default="{ row }">
          <div class="table-actions-cell-inner" @click.stop>
            <el-button link type="primary" :title="t('iot.device.detail')" @click.stop="openDetail(row)">
              <el-icon><View /></el-icon>
            </el-button>
            <el-button link type="warning" :title="t('iot.device.reboot')" @click.stop="onReboot(row)">
              <el-icon><RefreshRight /></el-icon>
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

    <el-dialog
      v-model="detailVisible"
      :title="t('iot.device.detailTitle')"
      width="560px"
      destroy-on-close
      class="iot-detail-dialog"
    >
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item :label="t('iot.device.id')">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item :label="t('iot.device.uuid')">{{ detail.uuid || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="t('iot.device.clientId')">{{ detail.clientId || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="t('iot.device.mac')">{{ detail.mac || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="t('iot.device.model')">{{ detail.model || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="t('iot.device.chip')">{{ detail.chip || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="t('iot.device.firmwareVersion')">{{ detail.firmwareVersion || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="t('iot.device.otaState')">{{ detail.otaState || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="t('iot.device.sessionId')">{{ detail.sessionId || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="t('iot.device.lastSeenAt')">{{ formatIotTime(detail.lastSeenAt) }}</el-descriptions-item>
        </el-descriptions>
        <div class="iot-detail-dialog__online">
          <el-button :loading="onlineChecking" @click="probeOnline">
            {{ t('iot.device.probeOnline') }}
          </el-button>
          <el-tag v-if="onlineResult !== null" :type="onlineResult ? 'success' : 'info'" size="small">
            {{ onlineResult ? t('iot.device.statusOnline') : t('iot.device.statusOffline') }}
          </el-tag>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">{{ t('iot.common.close') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, View } from '@element-plus/icons-vue'
import {
  fetchIotDevice,
  fetchIotDeviceOnlineStatus,
  fetchIotDevices,
  rebootIotDevice,
  type IotDevice,
  type IotDeviceStatus,
} from '@/api/iot'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { TABLE_ACTIONS_CELL_CLASS } from '@/constants/table'
import { formatIotTime, resolveDeviceStatusMeta } from './iotFormat'

const { t } = useI18n()

const keyword = ref('')
const statusFilter = ref<'' | IotDeviceStatus>('')
const detailVisible = ref(false)
const detail = ref<IotDevice | null>(null)
const onlineChecking = ref(false)
const onlineResult = ref<boolean | null>(null)

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination((p, ps) =>
  fetchIotDevices({
    page: p,
    pageSize: ps,
    ...(keyword.value.trim() ? { keyword: keyword.value.trim() } : {}),
    ...(statusFilter.value ? { status: statusFilter.value } : {}),
  }),
)

function statusMeta(status?: IotDeviceStatus) {
  return resolveDeviceStatusMeta(status)
}

async function onSearch() {
  await load(true)
}

function onFilterChange() {
  void load(true)
}

async function onRefresh() {
  await load(true)
}

async function openDetail(row: IotDevice) {
  detail.value = null
  onlineResult.value = null
  detailVisible.value = true
  try {
    detail.value = await fetchIotDevice(row.id)
  } catch {
    detail.value = row
  }
}

async function probeOnline() {
  if (!detail.value) return
  onlineChecking.value = true
  try {
    const result = await fetchIotDeviceOnlineStatus(detail.value.id)
    onlineResult.value = result.online
    if (result.lastSeenAt) {
      detail.value.lastSeenAt = result.lastSeenAt
    }
  } finally {
    onlineChecking.value = false
  }
}

async function onReboot(row: IotDevice) {
  await ElMessageBox.confirm(t('iot.device.rebootConfirm', { name: `#${row.id}` }), t('iot.common.confirmTitle'), {
    type: 'warning',
  })
  await rebootIotDevice(row.id)
  ElMessage.success(t('iot.device.rebootSuccess'))
}

onMounted(() => {
  void load()
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

.iot-panel__status-filter {
  width: 140px;
}

.iot-panel__toolbar-spacer {
  flex: 1;
  min-width: 12px;
}

.iot-table {
  flex: 1;
}

.iot-detail-dialog__online {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}
</style>
