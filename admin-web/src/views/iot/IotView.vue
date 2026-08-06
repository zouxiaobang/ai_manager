<template>
  <WarRoomPage :title="t('iot.title')" :subtitle="t('iot.subtitle')" fill>
    <div class="iot-view">
      <el-tabs v-model="activeTab" class="iot-view__tabs">
        <el-tab-pane :label="t('iot.tabDevices')" name="devices" lazy>
          <DeviceList />
        </el-tab-pane>
        <el-tab-pane :label="t('iot.tabFirmwares')" name="firmwares" lazy>
          <FirmwareList />
        </el-tab-pane>
        <el-tab-pane :label="t('iot.tabSessions')" name="sessions" lazy>
          <OnlineSessions />
        </el-tab-pane>
      </el-tabs>
    </div>
  </WarRoomPage>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
import DeviceList from './DeviceList.vue'
import FirmwareList from './FirmwareList.vue'
import OnlineSessions from './OnlineSessions.vue'

const { t } = useI18n()

const activeTab = ref<'devices' | 'firmwares' | 'sessions'>('devices')
</script>

<style scoped lang="scss">
.iot-view {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.iot-view__tabs {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;

  :deep(.el-tabs__content) {
    flex: 1;
    min-height: 0;
  }

  :deep(.el-tab-pane) {
    display: flex;
    flex-direction: column;
    flex: 1;
    min-height: 0;
  }
}
</style>
