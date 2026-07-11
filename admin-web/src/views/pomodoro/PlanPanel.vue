<template>
  <!-- 计划管理面板主容器 -->
  <div class="plan-panel plan-panel--pixel">
    <!-- 锁定提示横幅：计时进行中时禁止编辑计划 -->
    <div v-if="planEditLocked" class="plan-panel__lock-banner pixel-panel-jagged" role="status">
      <div class="pixel-panel-jagged__inner plan-panel__lock-inner">
        {{ t('pomodoro.plan.pauseRequired') }}
      </div>
    </div>

    <!-- 顶部工具栏：新增和刷新按钮 -->
    <header class="plan-panel__toolbar">
      <!-- 新增计划按钮 -->
      <button
        type="button"
        class="plan-panel__btn plan-panel__btn--green"
        :disabled="planEditLocked"
        @click="openCreate"
      >
        <span class="plan-panel__btn__inner">+ {{ t('pomodoro.plan.add') }}</span>
      </button>
      <!-- 刷新按钮 -->
      <button
        type="button"
        class="plan-panel__btn plan-panel__btn--blue"
        :disabled="loading"
        @click="loadPlans"
      >
        <span class="plan-panel__btn__inner">{{ t('pomodoro.plan.refresh') }}</span>
      </button>
    </header>

    <!-- 主体内容：左侧计划列表 + 右侧详情 -->
    <div class="plan-panel__body">
      <!-- 左侧：计划列表 -->
      <aside class="plan-panel__master pixel-panel-jagged">
        <div class="pixel-panel-jagged__inner plan-panel__master-inner">
          <h3 class="plan-panel__section-title">
            <span class="pixel-spark">✦</span>
            {{ t('pomodoro.plan.listTitle') }}
            <span class="pixel-spark">✦</span>
          </h3>

          <!-- 加载状态 -->
          <div v-if="loading" class="plan-panel__state">{{ t('pomodoro.plan.loading') }}</div>
          <!-- 空状态 -->
          <div v-else-if="records.length === 0" class="plan-panel__state">
            {{ t('pomodoro.plan.emptyList') }}
          </div>
          <!-- 计划列表 -->
          <ul v-else class="plan-list">
            <li v-for="row in records" :key="row.id">
              <button
                type="button"
                class="plan-list__item"
                :class="{ 'is-selected': selectedId === row.id }"
                @click="selectPlan(row.id)"
              >
                <div class="plan-list__item-inner">
                  <p class="plan-list__title">{{ row.title }}</p>
                  <div class="plan-list__meta">
                    <span v-if="row.isDefault === 1" class="plan-tag">
                      {{ t('pomodoro.plan.defaultTag') }}
                    </span>
                    <p class="plan-list__durations">
                      <span class="plan-list__dur--work">{{ row.workDurationMin }}</span>
                      <span class="plan-list__dur-sep">+</span>
                      <span class="plan-list__dur--short">{{ row.shortBreakMin }}</span>
                      <span class="plan-list__dur-sep">+</span>
                      <span class="plan-list__dur--long">{{ row.longBreakMin }}</span>
                    </p>
                  </div>
                </div>
              </button>
            </li>
          </ul>

          <!-- 分页导航 -->
          <nav
            v-if="!loading && pageCount > 1"
            class="plan-panel__pagination"
            :aria-label="t('pomodoro.report.pagination')"
          >
            <button
              type="button"
              class="plan-panel__pagination-btn"
              :disabled="page <= 1"
              @click="onPageChange(page - 1)"
            >
              {{ t('pomodoro.report.prevPage') }}
            </button>
            <span class="plan-panel__pagination-info">
              {{ t('pomodoro.report.pageIndicator', { page, total: pageCount }) }}
            </span>
            <button
              type="button"
              class="plan-panel__pagination-btn"
              :disabled="page >= pageCount"
              @click="onPageChange(page + 1)"
            >
              {{ t('pomodoro.report.nextPage') }}
            </button>
          </nav>
        </div>
      </aside>

      <!-- 右侧：计划详情 -->
      <section class="plan-panel__detail pixel-panel-jagged">
        <div class="pixel-panel-jagged__inner plan-panel__detail-inner">
          <!-- 空状态：未选择计划 -->
          <div v-if="!selectedPlan" class="plan-panel__state">
            {{ t('pomodoro.plan.emptyDetail') }}
          </div>
          <!-- 计划详情内容 -->
          <template v-else>
            <!-- 详情头部：标题和时长配置 -->
            <header class="plan-detail__header">
              <div class="plan-detail__title-row">
                <h2 class="plan-detail__title">{{ selectedPlan.title }}</h2>
                <span v-if="selectedPlan.isDefault === 1" class="plan-tag">
                  {{ t('pomodoro.plan.defaultTag') }}
                </span>
              </div>
              <p class="plan-detail__durations">
                <span class="plan-detail__dur--work">{{ selectedPlan.workDurationMin }}</span>
                <span class="plan-detail__dur-sep">+</span>
                <span class="plan-detail__dur--short">{{ selectedPlan.shortBreakMin }}</span>
                <span class="plan-detail__dur-sep">+</span>
                <span class="plan-detail__dur--long">{{ selectedPlan.longBreakMin }}</span>
              </p>
            </header>

            <div class="plan-detail__fields">
              <div class="plan-detail__row">
                <span class="plan-detail__label">{{ t('pomodoro.plan.work') }}</span>
                <span class="plan-detail__value plan-detail__value--work">
                  {{ selectedPlan.workDurationMin }} {{ t('pomodoro.plan.minuteUnit') }}
                </span>
              </div>
              <div class="plan-detail__row">
                <span class="plan-detail__label">{{ t('pomodoro.plan.shortBreak') }}</span>
                <span class="plan-detail__value plan-detail__value--short">
                  {{ selectedPlan.shortBreakMin }} {{ t('pomodoro.plan.minuteUnit') }}
                </span>
              </div>
              <div class="plan-detail__row">
                <span class="plan-detail__label">{{ t('pomodoro.plan.longBreak') }}</span>
                <span class="plan-detail__value plan-detail__value--long">
                  {{ selectedPlan.longBreakMin }} {{ t('pomodoro.plan.minuteUnit') }}
                </span>
              </div>
              <div class="plan-detail__row">
                <span class="plan-detail__label">{{ t('pomodoro.plan.roundsBeforeLong') }}</span>
                <span class="plan-detail__value plan-detail__value--cyan">
                  {{
                    t('pomodoro.plan.roundsEveryLong', {
                      n: selectedPlan.roundsBeforeLongBreak,
                    })
                  }}
                </span>
              </div>
              <div class="plan-detail__row">
                <span class="plan-detail__label">{{ t('pomodoro.plan.dailyGoalRounds') }}</span>
                <span class="plan-detail__value plan-detail__value--cyan">
                  {{ selectedPlan.dailyGoalRounds }} {{ t('pomodoro.plan.rounds') }}
                </span>
              </div>
              <div class="plan-detail__row">
                <span class="plan-detail__label">{{ t('pomodoro.plan.dailyGoalMinutes') }}</span>
                <span class="plan-detail__value plan-detail__value--blue">
                  {{ selectedPlan.dailyGoalMinutes }} {{ t('pomodoro.plan.minuteUnit') }}
                </span>
              </div>
              <div class="plan-detail__row">
                <span class="plan-detail__label">{{ t('pomodoro.plan.dailyTotalMinutes') }}</span>
                <span class="plan-detail__value plan-detail__value--blue">
                  {{ selectedDailyTotal }} {{ t('pomodoro.plan.minuteUnit') }}
                </span>
              </div>
            </div>

            <footer class="plan-detail__actions">
              <button
                type="button"
                class="plan-action-btn plan-action-btn--green"
                :disabled="planEditLocked || selectedPlan.isDefault === 1 || saving"
                @click="onSetDefault"
              >
                <span class="plan-action-btn__inner">{{ t('pomodoro.plan.asDefault') }}</span>
              </button>
              <button
                type="button"
                class="plan-action-btn plan-action-btn--blue"
                :disabled="planEditLocked"
                @click="openEdit(selectedPlan)"
              >
                <span class="plan-action-btn__inner">{{ t('pomodoro.plan.edit') }}</span>
              </button>
              <button
                type="button"
                class="plan-action-btn plan-action-btn--danger"
                :disabled="planEditLocked || selectedPlan.isDefault === 1"
                @click="onDelete(selectedPlan)"
              >
                <span class="plan-action-btn__inner">{{ t('pomodoro.plan.delete') }}</span>
              </button>
            </footer>
          </template>
        </div>
      </section>
    </div>

    <el-dialog
      v-model="dialogVisible"
      class="plan-pixel-dialog"
      width="520px"
      destroy-on-close
      :show-close="false"
      align-center
    >
      <div class="plan-dialog-frame">
        <div class="plan-dialog-frame__inner">
          <h3 class="plan-dialog__title">
            {{ editingId ? t('pomodoro.plan.editTitle') : t('pomodoro.plan.createTitle') }}
          </h3>
          <el-form class="plan-dialog__form" :model="form" label-width="140px">
            <el-form-item :label="t('pomodoro.plan.name')" required>
              <el-input v-model="form.title" />
            </el-form-item>
            <el-form-item :label="t('pomodoro.plan.work')">
              <el-input-number v-model="form.workDurationMin" :min="1" :max="120" />
            </el-form-item>
            <el-form-item :label="t('pomodoro.plan.shortBreak')">
              <el-input-number v-model="form.shortBreakMin" :min="1" :max="60" />
            </el-form-item>
            <el-form-item :label="t('pomodoro.plan.longBreak')">
              <el-input-number v-model="form.longBreakMin" :min="1" :max="60" />
            </el-form-item>
            <el-form-item :label="t('pomodoro.plan.roundsBeforeLong')">
              <el-input-number v-model="form.roundsBeforeLongBreak" :min="1" :max="12" />
            </el-form-item>
            <el-form-item :label="t('pomodoro.plan.dailyGoalRounds')">
              <el-input-number v-model="form.dailyGoalRounds" :min="1" :max="50" />
            </el-form-item>
            <el-form-item :label="t('pomodoro.plan.dailyGoalMinutes')">
              <div class="plan-dialog__calc">
                <span class="plan-dialog__calc-value">
                  {{ dailyFocusMinutes }} {{ t('pomodoro.plan.minuteUnit') }}
                </span>
                <span class="plan-dialog__calc-hint">{{ t('pomodoro.plan.dailyGoalMinutesAuto') }}</span>
              </div>
            </el-form-item>
            <el-form-item :label="t('pomodoro.plan.dailyTotalMinutes')">
              <div class="plan-dialog__calc">
                <span class="plan-dialog__calc-value">
                  {{ dailyTotalMinutes }} {{ t('pomodoro.plan.minuteUnit') }}
                </span>
                <span class="plan-dialog__calc-hint">{{ t('pomodoro.plan.dailyTotalMinutesHint') }}</span>
              </div>
            </el-form-item>
            <el-form-item :label="t('pomodoro.plan.asDefault')">
              <el-switch v-model="form.asDefault" />
            </el-form-item>
          </el-form>
          <footer class="plan-dialog__footer">
            <button type="button" class="plan-action-btn plan-action-btn--blue" @click="dialogVisible = false">
              <span class="plan-action-btn__inner">{{ t('pomodoro.common.cancel') }}</span>
            </button>
            <button
              type="button"
              class="plan-action-btn plan-action-btn--green"
              :disabled="saving"
              @click="savePlan"
            >
              <span class="plan-action-btn__inner">{{ t('pomodoro.common.save') }}</span>
            </button>
          </footer>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 番茄钟计划管理面板组件
 * 管理番茄钟专注计划，支持计划的新增、编辑、删除和查看
 * 展示计划配置详情，包括工作时长、休息时长、每日目标等
 */
import { computed, inject, onMounted, onUnmounted, reactive, ref, watch, watchEffect } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePagination } from '@/composables/usePagination'
import {
  createPlan,
  fetchActiveSession,
  fetchPlans,
  removePlan,
  updatePlan,
  type PomodoroPlan,
  type PomodoroPlanSaveRequest,
} from '@/api/pomodoro'
import { isPlanMutationBlocked } from '@/utils/pomodoroSession'
import { POMODORO_PLAN_CONTEXT_KEY } from './pomodoroPlanContext'

const { t } = useI18n() // 国际化函数
const planContext = inject(POMODORO_PLAN_CONTEXT_KEY, null) // 计划上下文
const saving = ref(false) // 保存状态
const selectedId = ref<number | null>(null) // 当前选中的计划ID
const dialogVisible = ref(false) // 编辑对话框是否可见
const editingId = ref<number | null>(null) // 正在编辑的计划ID
const planEditLocked = ref(false) // 计划编辑是否锁定

const LOCK_POLL_MS = 2000 // 锁定状态轮询间隔
let lockPollTimer: ReturnType<typeof setInterval> | null = null // 锁定轮询定时器

const { page, pageSize, total, records, loading, load, onPageChange } = usePagination(
  // 分页获取计划列表
  (p, ps) => fetchPlans({ page: p, pageSize: ps }),
)

const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const selectedPlan = computed(() => records.value.find((r) => r.id === selectedId.value) ?? null)

function calcDailyBreakMinutes(
  dailyGoalRounds: number,
  roundsBeforeLongBreak: number,
  shortBreakMin: number,
  longBreakMin: number,
): number {
  if (dailyGoalRounds <= 1 || roundsBeforeLongBreak < 1) {
    return 0
  }
  let sum = 0
  for (let k = 1; k < dailyGoalRounds; k++) {
    sum += k % roundsBeforeLongBreak === 0 ? longBreakMin : shortBreakMin
  }
  return sum
}

function calcDailyFocusMinutes(dailyGoalRounds: number, workDurationMin: number): number {
  return Math.max(0, dailyGoalRounds) * Math.max(0, workDurationMin)
}

const selectedDailyTotal = computed(() => {
  const row = selectedPlan.value
  if (!row) return 0
  return (
    calcDailyFocusMinutes(row.dailyGoalRounds, row.workDurationMin) +
    calcDailyBreakMinutes(
      row.dailyGoalRounds,
      row.roundsBeforeLongBreak,
      row.shortBreakMin,
      row.longBreakMin,
    )
  )
})

const defaultForm = (): PomodoroPlanSaveRequest & { asDefault: boolean } => ({
  title: '',
  workDurationMin: 25,
  shortBreakMin: 5,
  longBreakMin: 15,
  roundsBeforeLongBreak: 4,
  dailyGoalRounds: 8,
  dailyGoalMinutes: 200,
  asDefault: false,
  status: 'ENABLED',
})

const form = reactive(defaultForm())

const dailyFocusMinutes = computed(() =>
  calcDailyFocusMinutes(form.dailyGoalRounds, form.workDurationMin),
)

const dailyBreakMinutes = computed(() =>
  calcDailyBreakMinutes(
    form.dailyGoalRounds,
    form.roundsBeforeLongBreak,
    form.shortBreakMin,
    form.longBreakMin,
  ),
)

const dailyTotalMinutes = computed(() => dailyFocusMinutes.value + dailyBreakMinutes.value)

watchEffect(() => {
  form.dailyGoalMinutes = dailyFocusMinutes.value
})

function syncSelection() {
  if (records.value.length === 0) {
    selectedId.value = null
    return
  }
  const current = records.value.find((r) => r.id === selectedId.value)
  if (!current) {
    const def = records.value.find((r) => r.isDefault === 1)
    selectedId.value = def?.id ?? records.value[0].id
  }
}

watch(records, syncSelection)

function selectPlan(id: number) {
  selectedId.value = id
}

async function refreshPlanEditLock() {
  if (planContext) {
    planEditLocked.value = !(await planContext.checkEditable())
    return
  }
  try {
    const session = await fetchActiveSession()
    planEditLocked.value = isPlanMutationBlocked(session)
  } catch {
    planEditLocked.value = false
  }
}

async function guardPlanEdit(): Promise<boolean> {
  await refreshPlanEditLock()
  if (planEditLocked.value) {
    ElMessage.warning(t('pomodoro.plan.pauseRequired'))
    return false
  }
  return true
}

async function notifyPlansChanged(planId?: number) {
  if (planContext) {
    await planContext.notifyPlansChanged(planId)
  }
}

async function loadPlans() {
  await load()
  syncSelection()
  await refreshPlanEditLock()
}

function openCreate() {
  void (async () => {
    if (!(await guardPlanEdit())) return
    editingId.value = null
    Object.assign(form, defaultForm())
    dialogVisible.value = true
  })()
}

function openEdit(row: PomodoroPlan) {
  void (async () => {
    if (!(await guardPlanEdit())) return
    editingId.value = row.id
    Object.assign(form, {
      title: row.title,
      workDurationMin: row.workDurationMin,
      shortBreakMin: row.shortBreakMin,
      longBreakMin: row.longBreakMin,
      roundsBeforeLongBreak: row.roundsBeforeLongBreak,
      dailyGoalRounds: row.dailyGoalRounds,
      dailyGoalMinutes: row.dailyGoalMinutes,
      asDefault: row.isDefault === 1,
      status: row.status,
    })
    dialogVisible.value = true
  })()
}

function planPayload(row: PomodoroPlan, asDefault?: boolean): PomodoroPlanSaveRequest {
  return {
    title: row.title,
    workDurationMin: row.workDurationMin,
    shortBreakMin: row.shortBreakMin,
    longBreakMin: row.longBreakMin,
    roundsBeforeLongBreak: row.roundsBeforeLongBreak,
    dailyGoalRounds: row.dailyGoalRounds,
    dailyGoalMinutes: row.dailyGoalMinutes,
    asDefault: asDefault ?? row.isDefault === 1,
    status: row.status,
  }
}

async function onSetDefault() {
  if (!(await guardPlanEdit())) return
  const row = selectedPlan.value
  if (!row || row.isDefault === 1) return
  saving.value = true
  try {
    await updatePlan(row.id, planPayload(row, true))
    await loadPlans()
    await notifyPlansChanged(row.id)
    ElMessage.success(t('pomodoro.common.saved'))
  } finally {
    saving.value = false
  }
}

async function savePlan() {
  if (!(await guardPlanEdit())) return
  if (!form.title.trim()) {
    ElMessage.warning(t('pomodoro.plan.nameRequired'))
    return
  }
  saving.value = true
  try {
    const payload = { ...form, dailyGoalMinutes: dailyFocusMinutes.value }
    if (editingId.value) {
      await updatePlan(editingId.value, payload)
      selectedId.value = editingId.value
    } else {
      const created = await createPlan(payload)
      selectedId.value = created.id
    }
    dialogVisible.value = false
    const changedId = editingId.value ?? selectedId.value ?? undefined
    await loadPlans()
    await notifyPlansChanged(changedId)
    ElMessage.success(t('pomodoro.common.saved'))
  } finally {
    saving.value = false
  }
}

async function onDelete(row: PomodoroPlan) {
  if (!(await guardPlanEdit())) return
  await ElMessageBox.confirm(t('pomodoro.plan.deleteConfirm', { name: row.title }), {
    type: 'warning',
  })
  await removePlan(row.id)
  ElMessage.success(t('pomodoro.common.deleted'))
  await loadPlans()
  await notifyPlansChanged()
}

onMounted(() => {
  void loadPlans()
  void refreshPlanEditLock()
  lockPollTimer = setInterval(() => {
    void refreshPlanEditLock()
  }, LOCK_POLL_MS)
})

onUnmounted(() => {
  if (lockPollTimer) {
    clearInterval(lockPollTimer)
    lockPollTimer = null
  }
})

defineExpose({ loadPlans })
</script>

<style scoped lang="scss">
@use './pomodoro-pixel-plan.scss';
</style>

<style lang="scss">
@use './pomodoro-pixel-plan-dialog.scss';
</style>
