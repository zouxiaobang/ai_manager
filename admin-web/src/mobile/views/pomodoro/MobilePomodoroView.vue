<template>
  <!-- 移动端番茄钟页面容器 -->
  <div class="mobile-pomodoro">
    <!-- 顶部标签切换栏：计时器/计划/报表三个功能模块 -->
    <el-tabs v-model="activeTab" class="mobile-pomodoro__tabs">
      <!-- 计时器标签页：番茄钟计时主功能 -->
      <el-tab-pane :label="t('pomodoro.tabs.timer')" name="timer">
        <TimerPanel ref="timerRef" />
      </el-tab-pane>
      <!-- 计划标签页：番茄钟计划管理 -->
      <el-tab-pane :label="t('pomodoro.tabs.plan')" name="plan">
        <PlanPanel ref="planRef" />
      </el-tab-pane>
      <!-- 报表标签页：番茄钟数据统计报表 -->
      <el-tab-pane :label="t('pomodoro.tabs.report')" name="report">
        <ReportPanel />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
/**
 * 移动端番茄钟视图组件
 * 功能说明：
 * - 提供番茄钟的移动端适配页面
 * - 包含计时器、计划管理、数据报表三个核心功能模块
 * - 通过Tab切换实现不同功能模块的展示
 * - 提供番茄钟计划上下文，协调各子组件间的状态同步
 */
import { provide, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import PlanPanel from '@/views/pomodoro/PlanPanel.vue'
import TimerPanel from '@/views/pomodoro/TimerPanel.vue'
import ReportPanel from '@/views/pomodoro/ReportPanel.vue'
import { POMODORO_PLAN_CONTEXT_KEY } from '@/views/pomodoro/pomodoroPlanContext'
import { fetchActiveSession } from '@/api/pomodoro'
import { isPlanMutationBlocked } from '@/utils/pomodoroSession'

const { t } = useI18n() // 国际化翻译函数
const activeTab = ref('timer') // 当前激活的标签页
const timerRef = ref<InstanceType<typeof TimerPanel> | null>(null) // 计时器组件引用
const planRef = ref<InstanceType<typeof PlanPanel> | null>(null) // 计划面板组件引用

// 向下提供番茄钟计划上下文：检查是否可编辑 + 通知计划变更
provide(POMODORO_PLAN_CONTEXT_KEY, {
  // 检查计划是否可编辑：当前有进行中的番茄钟会话时不可编辑
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
  // 通知计划变更：计时器面板刷新计划数据
  notifyPlansChanged: async (planId?: number) => {
    await timerRef.value?.onPlansChanged?.(planId)
  },
})

// 监听标签切换：切换时刷新对应面板的数据
watch(activeTab, (tab) => {
  if (tab === 'timer') {
    timerRef.value?.loadPlans() // 加载计划列表
    timerRef.value?.refreshToday() // 刷新今日数据
    timerRef.value?.pullRemoteSession() // 拉取远程会话
    timerRef.value?.startRemoteSync() // 启动远程同步
  }
  if (tab === 'plan') {
    planRef.value?.loadPlans() // 加载计划列表
    timerRef.value?.pullRemoteSession() // 拉取远程会话
    timerRef.value?.startRemoteSync() // 启动远程同步
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