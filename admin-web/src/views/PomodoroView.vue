<template>
  <div class="pomo-pixel-page war-room-page--fill">
    <div class="pomo-pixel-page__stars" aria-hidden="true" />

    <div v-if="activeTab === 'timer'" class="pomo-pixel-page__before" aria-hidden="true" />

    <div
      class="pomo-pixel-page__content"
      :class="{ 'pomo-pixel-page__content--fill': activeTab !== 'timer' }"
    >
      <TimerPanel v-show="activeTab === 'timer'" ref="timerRef" />
      <div v-show="activeTab === 'report'" class="pomo-pixel-subpanel">
        <ReportPanel ref="reportRef" />
      </div>
      <div v-show="activeTab === 'plan'" class="pomo-pixel-subpanel">
        <PlanPanel ref="planRef" />
      </div>
    </div>

    <div v-if="activeTab === 'timer'" class="pomo-pixel-page__after" aria-hidden="true" />

    <PomoPixelNav v-model="activeTab" :items="navItems" />
  </div>
</template>

<script setup lang="ts">
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

const { t } = useI18n()
const route = useRoute()
const activeTab = ref<PomoNavTabId>('timer')
const timerRef = ref<PomodoroPanelExpose | null>(null)
const reportRef = ref<PomodoroPanelExpose | null>(null)
const planRef = ref<PomodoroPanelExpose | null>(null)

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
