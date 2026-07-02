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
import { provide, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import PlanPanel from '@/views/pomodoro/PlanPanel.vue'
import TimerPanel from '@/views/pomodoro/TimerPanel.vue'
import ReportPanel from '@/views/pomodoro/ReportPanel.vue'
import { POMODORO_PLAN_CONTEXT_KEY } from '@/views/pomodoro/pomodoroPlanContext'
import { fetchActiveSession } from '@/api/pomodoro'
import { isPlanMutationBlocked } from '@/utils/pomodoroSession'

const { t } = useI18n()
const activeTab = ref('timer')
const timerRef = ref<InstanceType<typeof TimerPanel> | null>(null)
const planRef = ref<InstanceType<typeof PlanPanel> | null>(null)

provide(POMODORO_PLAN_CONTEXT_KEY, {
  checkEditable: async () => {
    if (timerRef.value?.checkPlanEditable) {
      return timerRef.value.checkPlanEditable()
    }
    try {
      const session = await fetchActiveSession()
      return !isPlanMutationBlocked(session)
    } catch {
      return true
    }
  },
  notifyPlansChanged: async (planId?: number) => {
    await timerRef.value?.onPlansChanged?.(planId)
  },
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
    timerRef.value?.pullRemoteSession()
    timerRef.value?.startRemoteSync()
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
