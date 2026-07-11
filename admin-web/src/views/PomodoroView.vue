<!--
 * 番茄钟页面组件
 * 提供专注计时功能，支持工作/休息阶段切换、副屏同步、统计报表
 * 采用像素风格设计，包含计时器、计划管理和数据报告三个标签页
 -->
<template>
  <!-- 页面主容器：番茄钟页面整体布局，像素风格设计 -->
  <div class="pomo-pixel-page war-room-page--fill">
    <!-- 星空背景装饰层 -->
    <div class="pomo-pixel-page__stars" aria-hidden="true" />

    <!-- 计时器页面顶部留白区域（仅计时器标签页显示） -->
    <div v-if="activeTab === 'timer'" class="pomo-pixel-page__before" aria-hidden="true" />

    <!-- 内容区域：根据当前激活标签页显示对应面板 -->
    <div
      class="pomo-pixel-page__content"
      :class="{ 'pomo-pixel-page__content--fill': activeTab !== 'timer' }"
    >
      <!-- 计时器面板：专注计时核心功能 -->
      <TimerPanel v-show="activeTab === 'timer'" ref="timerRef" />
      <!-- 报告面板：番茄钟数据统计报表 -->
      <div v-show="activeTab === 'report'" class="pomo-pixel-subpanel">
        <ReportPanel ref="reportRef" />
      </div>
      <!-- 计划面板：番茄钟计划管理 -->
      <div v-show="activeTab === 'plan'" class="pomo-pixel-subpanel">
        <PlanPanel ref="planRef" />
      </div>
    </div>

    <!-- 计时器页面底部留白区域（仅计时器标签页显示） -->
    <div v-if="activeTab === 'timer'" class="pomo-pixel-page__after" aria-hidden="true" />

    <!-- 底部导航栏：计时器、报告、计划三个标签页切换 -->
    <PomoPixelNav v-model="activeTab" :items="navItems" />
  </div>
</template>

<script setup lang="ts">
/**
 * 番茄钟页面组件
 * 提供专注计时功能，支持工作/休息阶段切换、副屏同步、统计报表
 * 采用像素风格设计，包含计时器、计划管理和数据报告三个标签页
 */
import { computed, defineAsyncComponent, onMounted, provide, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import TimerPanel from './pomodoro/TimerPanel.vue'
import PomoPixelNav, { type PomoNavTabId } from './pomodoro/PomoPixelNav.vue'
import { POMODORO_PLAN_CONTEXT_KEY } from './pomodoro/pomodoroPlanContext'
import { fetchActiveSession } from '@/api/pomodoro'
import { isPlanMutationBlocked } from '@/utils/pomodoroSession'

const ReportPanel = defineAsyncComponent(() => import('./pomodoro/ReportPanel.vue'))
const PlanPanel = defineAsyncComponent(() => import('./pomodoro/PlanPanel.vue'))

interface PomodoroPanelExpose {
  loadPlans?: () => void
  refreshToday?: () => void
  pullRemoteSession?: () => void
  startRemoteSync?: () => void
  loadReport?: () => Promise<void>
  checkPlanEditable?: () => Promise<boolean>
  onPlansChanged?: (planId?: number) => Promise<void>
}

const { t } = useI18n() // 国际化函数
const route = useRoute() // 路由实例
const activeTab = ref<PomoNavTabId>('timer') // 当前激活的标签页
const timerRef = ref<PomodoroPanelExpose | null>(null) // 计时器面板引用
const reportRef = ref<PomodoroPanelExpose | null>(null) // 报告面板引用
const planRef = ref<PomodoroPanelExpose | null>(null) // 计划面板引用

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

const navItems = computed(() => [
  { id: 'timer' as const, icon: '🏠', label: t('pomodoro.timer.navTimer') },
  { id: 'report' as const, icon: '📊', label: t('pomodoro.timer.navStats') },
  { id: 'plan' as const, icon: '📋', label: t('pomodoro.timer.navPlan') },
])

function onTabActivated(tab: PomoNavTabId) {
  // 标签页激活时的处理逻辑，刷新对应面板数据
  if (tab === 'timer') {
    timerRef.value?.loadPlans?.()
    timerRef.value?.refreshToday?.()
    timerRef.value?.pullRemoteSession?.()
    timerRef.value?.startRemoteSync?.()
  }
  if (tab === 'report') {
    void reportRef.value?.loadReport?.()
  }
  if (tab === 'plan') {
    planRef.value?.loadPlans?.()
    timerRef.value?.pullRemoteSession?.()
    timerRef.value?.startRemoteSync?.()
  }
}

onMounted(() => {
  const tab = route.query.tab
  if (tab === 'timer' || tab === 'report' || tab === 'plan') {
    activeTab.value = tab
  }
  onTabActivated(activeTab.value)
})

watch(activeTab, (tab) => {
  onTabActivated(tab)
})
</script>

<style scoped lang="scss">
$pomo-bg: #08081a;

.pomo-pixel-page {
  --pomo-card-bg: #{rgb(16 16 40 / 52%)};
  --pomo-pixel-step: 6px;
  --pomo-content-height: 66.666%;
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  height: 100%;
  padding: clamp(8px, 1.2vh, 12px) clamp(8px, 1.2vw, 12px) 0;
  border-radius: 0;
  background-color: transparent;
  overflow: hidden;
  image-rendering: pixelated;

  :deep(img),
  :deep(svg),
  :deep(canvas) {
    image-rendering: pixelated;
    shape-rendering: crispEdges;
  }
}

.pomo-pixel-page__stars {
  pointer-events: none;
  position: absolute;
  inset: 0;
  z-index: 0;
  background-color: $pomo-bg;
  background-image: url('/patterns/pomo-pixel-stars.svg');
  background-repeat: repeat;
  background-size: 280px 280px;
  image-rendering: pixelated;
  image-rendering: crisp-edges;
}

.pomo-pixel-page__before,
.pomo-pixel-page__after {
  flex: 1 1 0;
  min-height: 0;
}

.pomo-pixel-page__content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  flex: 0 0 var(--pomo-content-height);
  height: var(--pomo-content-height);
  max-height: var(--pomo-content-height);
  min-height: 0;
  overflow: visible;
  padding-bottom: clamp(4px, 0.8vh, 8px);

  &--fill {
    flex: 1 1 auto;
    height: auto;
    max-height: none;
  }

  :deep(.timer-panel--pixel) {
    flex: 1;
    min-height: 0;
  }
}

.pomo-pixel-subpanel {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 4px 2px;
}
</style>

<style lang="scss">
@use './pomodoro/pomodoro-pixel.scss';
</style>
