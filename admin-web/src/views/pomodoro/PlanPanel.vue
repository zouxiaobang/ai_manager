<template>

  <div>

    <div class="panel-toolbar">

      <el-button type="primary" @click="openCreate">{{ t('pomodoro.plan.add') }}</el-button>

      <el-button @click="loadPlans">{{ t('pomodoro.plan.refresh') }}</el-button>

    </div>



    <el-table v-loading="loading" :data="records" stripe border>

      <el-table-column prop="title" :label="t('pomodoro.plan.name')" min-width="140" />

      <el-table-column :label="t('pomodoro.plan.work')" width="90">

        <template #default="{ row }">{{ row.workDurationMin }} min</template>

      </el-table-column>

      <el-table-column :label="t('pomodoro.plan.shortBreak')" width="90">

        <template #default="{ row }">{{ row.shortBreakMin }} min</template>

      </el-table-column>

      <el-table-column :label="t('pomodoro.plan.longBreak')" width="90">

        <template #default="{ row }">{{ row.longBreakMin }} min</template>

      </el-table-column>

      <el-table-column :label="t('pomodoro.plan.dailyGoal')" width="120">

        <template #default="{ row }">

          {{ row.dailyGoalRounds }} {{ t('pomodoro.plan.rounds') }} /

          {{ row.dailyGoalMinutes }} min

        </template>

      </el-table-column>

      <el-table-column :label="t('pomodoro.plan.default')" width="80">

        <template #default="{ row }">

          <el-tag v-if="row.isDefault === 1" type="success" size="small">

            {{ t('pomodoro.plan.defaultTag') }}

          </el-tag>

        </template>

      </el-table-column>

      <el-table-column
        :label="t('pomodoro.plan.actions')"
        width="88"
        fixed="right"
        align="center"
        :class-name="TABLE_ACTIONS_CELL_CLASS"
      >
        <template #default="{ row }">
          <div class="table-actions-cell-inner" @click.stop>
            <el-button link type="primary" :title="t('pomodoro.plan.edit')" @click.stop="openEdit(row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button
              link
              type="danger"
              :title="t('pomodoro.plan.delete')"
              :disabled="row.isDefault === 1"
              @click.stop="onDelete(row)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
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

    <el-dialog

      v-model="dialogVisible"

      :title="editingId ? t('pomodoro.plan.editTitle') : t('pomodoro.plan.createTitle')"

      width="520px"

      destroy-on-close

    >

      <el-form :model="form" label-width="140px">

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

          <div class="calc-field">

            <span class="calc-field__value">{{ dailyFocusMinutes }} min</span>

            <span class="calc-field__hint">{{ t('pomodoro.plan.dailyGoalMinutesAuto') }}</span>

          </div>

        </el-form-item>

        <el-form-item :label="t('pomodoro.plan.dailyTotalMinutes')">

          <div class="calc-field">

            <span class="calc-field__value">{{ dailyTotalMinutes }} min</span>

            <span class="calc-field__hint">{{ t('pomodoro.plan.dailyTotalMinutesHint') }}</span>

          </div>

        </el-form-item>

        <el-form-item :label="t('pomodoro.plan.asDefault')">

          <el-switch v-model="form.asDefault" />

        </el-form-item>

      </el-form>

      <template #footer>

        <el-button @click="dialogVisible = false">{{ t('pomodoro.common.cancel') }}</el-button>

        <el-button type="primary" :loading="saving" @click="savePlan">

          {{ t('pomodoro.common.save') }}

        </el-button>

      </template>

    </el-dialog>

  </div>

</template>



<script setup lang="ts">

import { computed, onMounted, reactive, ref, watchEffect } from 'vue'

import { useI18n } from 'vue-i18n'

import { ElMessage, ElMessageBox } from 'element-plus'

import { Delete, Edit } from '@element-plus/icons-vue'

import { TABLE_ACTIONS_CELL_CLASS } from '@/constants/table'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'

import {

  createPlan,

  fetchPlans,

  removePlan,

  updatePlan,

  type PomodoroPlan,

  type PomodoroPlanSaveRequest,

} from '@/api/pomodoro'



const { t } = useI18n()

const saving = ref(false)

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination(
  (p, ps) => fetchPlans({ page: p, pageSize: ps }),
)

const dialogVisible = ref(false)

const editingId = ref<number | null>(null)



function calcDailyBreakMinutes(

  dailyGoalRounds: number,

  roundsBeforeLongBreak: number,

  shortBreakMin: number,

  longBreakMin: number,

): number {

  if (dailyGoalRounds <= 1 || roundsBeforeLongBreak < 1) {

    return 0

  }

  let total = 0

  for (let k = 1; k < dailyGoalRounds; k++) {

    total += k % roundsBeforeLongBreak === 0 ? longBreakMin : shortBreakMin

  }

  return total

}



function calcDailyFocusMinutes(dailyGoalRounds: number, workDurationMin: number): number {

  return Math.max(0, dailyGoalRounds) * Math.max(0, workDurationMin)

}



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



async function loadPlans() {

  await load()

}



function openCreate() {

  editingId.value = null

  Object.assign(form, defaultForm())

  dialogVisible.value = true

}



function openEdit(row: PomodoroPlan) {

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

}



async function savePlan() {

  if (!form.title.trim()) {

    ElMessage.warning(t('pomodoro.plan.nameRequired'))

    return

  }

  saving.value = true

  try {

    const payload = { ...form, dailyGoalMinutes: dailyFocusMinutes.value }

    if (editingId.value) {

      await updatePlan(editingId.value, payload)

    } else {

      await createPlan(payload)

    }

    dialogVisible.value = false

    await loadPlans()

    ElMessage.success(t('pomodoro.common.saved'))

  } finally {

    saving.value = false

  }

}



async function onDelete(row: PomodoroPlan) {

  await ElMessageBox.confirm(

    t('pomodoro.plan.deleteConfirm', { name: row.title }),

    { type: 'warning' },

  )

  await removePlan(row.id)

  ElMessage.success(t('pomodoro.common.deleted'))

  await loadPlans()

}



onMounted(loadPlans)



defineExpose({ loadPlans })

</script>



<style scoped>

.panel-toolbar {

  margin-bottom: 12px;

  display: flex;

  gap: 8px;

}



.calc-field {

  display: flex;

  flex-direction: column;

  gap: 4px;

  padding-top: 4px;

}



.calc-field__value {

  font-size: 16px;

  font-weight: 600;

  color: var(--el-text-color-primary);

}



.calc-field__hint {

  font-size: 12px;

  color: var(--el-text-color-secondary);

  line-height: 1.4;

}

</style>


