<template>
  <div class="pomodoro-page">
    <h2 class="pomodoro-page__title">{{ t('pomodoro.title') }}</h2>
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
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import PlanPanel from './pomodoro/PlanPanel.vue'
import TimerPanel from './pomodoro/TimerPanel.vue'
import ReportPanel from './pomodoro/ReportPanel.vue'

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

<style scoped>
.pomodoro-page__title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
</style>
