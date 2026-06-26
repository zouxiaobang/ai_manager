<template>
  <WarRoomPage :title="t('pomodoro.title')" fill>
    <div class="war-room-panel war-room-panel--tabs">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="t('pomodoro.tabs.timer')" name="timer">
          <TimerPanel ref="timerRef" />
        </el-tab-pane>
        <el-tab-pane :label="t('pomodoro.tabs.plan')" name="plan">
          <PlanPanel ref="planRef" />
        </el-tab-pane>
        <el-tab-pane :label="t('pomodoro.tabs.report')" name="report">
          <ReportPanel />
        </el-tab-pane>
      </el-tabs>
    </div>
  </WarRoomPage>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
import PlanPanel from './pomodoro/PlanPanel.vue'
import TimerPanel from './pomodoro/TimerPanel.vue'
import ReportPanel from './pomodoro/ReportPanel.vue'

const { t } = useI18n()
const route = useRoute()
const activeTab = ref('timer')
const timerRef = ref<InstanceType<typeof TimerPanel> | null>(null)
const planRef = ref<InstanceType<typeof PlanPanel> | null>(null)

onMounted(() => {
  const tab = route.query.tab
  if (tab === 'timer' || tab === 'plan' || tab === 'report') {
    activeTab.value = tab
  }
})

watch(activeTab, (tab) => {
  if (tab === 'timer') {
    timerRef.value?.loadPlans()
    timerRef.value?.refreshToday()
    timerRef.value?.pullRemoteSession()
    timerRef.value?.startRemoteSync()
  }
  if (tab === 'plan') {
    planRef.value?.loadPlans()
  }
})
</script>

<style scoped lang="scss">
.war-room-panel--tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  :deep(.el-tabs) {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
  }

  :deep(.el-tabs__content) {
    flex: 1;
    min-height: 0;
    overflow: auto;
  }
}
</style>
