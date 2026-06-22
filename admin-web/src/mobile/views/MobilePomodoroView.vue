<template>
  <div class="mobile-pomodoro">
    <el-tabs v-model="activeTab" class="mobile-pomodoro__tabs">
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
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import PlanPanel from '@/views/pomodoro/PlanPanel.vue'
import TimerPanel from '@/views/pomodoro/TimerPanel.vue'
import ReportPanel from '@/views/pomodoro/ReportPanel.vue'

const { t } = useI18n()
const activeTab = ref('timer')
const timerRef = ref<InstanceType<typeof TimerPanel> | null>(null)
const planRef = ref<InstanceType<typeof PlanPanel> | null>(null)

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
.mobile-pomodoro {
  margin: -4px -8px 0;
}

.mobile-pomodoro__tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}

.mobile-pomodoro__tabs :deep(.el-tabs__nav-wrap) {
  overflow-x: auto;
}
</style>
