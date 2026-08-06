<template>
  <div class="online-sessions iot-panel">
    <div class="iot-panel__toolbar">
      <el-select
        v-model="onlineFilter"
        class="iot-panel__status-filter"
        :placeholder="t('iot.session.stateAll')"
        @change="onFilterChange"
      >
        <el-option :label="t('iot.session.stateAll')" value="" />
        <el-option :label="t('iot.session.stateOnline')" value="true" />
        <el-option :label="t('iot.session.stateEnded')" value="false" />
      </el-select>
      <div class="iot-panel__toolbar-spacer" />
      <el-button :loading="loading" @click="onRefresh">{{ t('iot.session.refresh') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="records" border row-key="id" class="iot-table">
      <el-table-column prop="id" :label="t('iot.session.id')" width="80" />
      <el-table-column :label="t('iot.session.deviceName')" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">{{ resolveDeviceName(row, (key) => t(key)) }}</template>
      </el-table-column>
      <el-table-column prop="sessionId" :label="t('iot.session.sessionId')" min-width="200" show-overflow-tooltip />
      <el-table-column :label="t('iot.session.startedAt')" width="170">
        <template #default="{ row }">{{ formatIotTime(row.startedAt) }}</template>
      </el-table-column>
      <el-table-column :label="t('iot.session.endedAt')" width="170">
        <template #default="{ row }">{{ formatIotTime(row.endedAt) }}</template>
      </el-table-column>
      <el-table-column prop="turnCount" :label="t('iot.session.turnCount')" width="100" align="center" />
      <el-table-column :label="t('iot.session.state')" width="100">
        <template #default="{ row }">
          <el-tag :type="isOnline(row) ? 'success' : 'info'" size="small">
            {{ isOnline(row) ? t('iot.session.stateOnline') : t('iot.session.stateEnded') }}
          </el-tag>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchIotOnlineSessions, type IotOnlineSession } from '@/api/iot'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { formatIotTime, resolveDeviceName, resolveSessionOnline } from './iotFormat'

const { t } = useI18n()

const onlineFilter = ref<'' | 'true' | 'false'>('')

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination<IotOnlineSession>(
  (p, ps) =>
    fetchIotOnlineSessions({
      page: p,
      pageSize: ps,
      ...(onlineFilter.value === 'true' ? { online: true } : {}),
      ...(onlineFilter.value === 'false' ? { online: false } : {}),
    }),
)

function isOnline(row: IotOnlineSession) {
  return resolveSessionOnline(row)
}

function onFilterChange() {
  void load(true)
}

async function onRefresh() {
  await load(true)
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
</style>
