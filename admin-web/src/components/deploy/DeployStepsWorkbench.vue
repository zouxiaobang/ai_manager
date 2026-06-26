<template>
  <div class="deploy-steps-workbench">
    <div class="deploy-steps-workbench__status-row">
      <article
        v-for="card in statusCards"
        :key="card.key"
        class="deploy-steps-workbench__status-card"
        :class="`is-${card.tone}`"
      >
        <div class="deploy-steps-workbench__status-label">{{ card.label }}</div>
        <div class="deploy-steps-workbench__status-value" :class="card.valueClass">
          {{ card.value }}
        </div>
        <div class="deploy-steps-workbench__status-foot">
          <span class="deploy-steps-workbench__status-dot" :class="card.dotClass" />
          <span>{{ card.foot }}</span>
        </div>
      </article>
    </div>

    <div class="deploy-steps-workbench__actions">
      <button
        type="button"
        class="deploy-steps-workbench__action is-backend"
        @click="startDeploy('backend')"
        :disabled="deployRunning"
      >
        <el-icon><Promotion /></el-icon>
        {{ t('deployCenter.stepsWorkbench.deployBackend') }}
      </button>
      <button
        type="button"
        class="deploy-steps-workbench__action is-frontend"
        @click="startDeploy('frontend')"
        :disabled="deployRunning"
      >
        <el-icon><Monitor /></el-icon>
        {{ t('deployCenter.stepsWorkbench.deployFrontend') }}
      </button>
      <button
        type="button"
        class="deploy-steps-workbench__action is-health"
        :disabled="healthLoading"
        @click="runHealthCheck(true)"
      >
        <el-icon v-if="healthLoading" class="is-loading"><Loading /></el-icon>
        <el-icon v-else><FirstAidKit /></el-icon>
        {{ t('deployCenter.stepsWorkbench.healthCheck') }}
      </button>
      <button
        type="button"
        class="deploy-steps-workbench__action is-visit"
        @click="openAdminUrl"
      >
        <el-icon><Link /></el-icon>
        {{ t('deployCenter.stepsWorkbench.visitUrl') }}
      </button>
    </div>

    <DeployRunDialog
      v-model="deployDialogVisible"
      :target="deployTarget"
      @finished="onDeployFinished"
    />

    <div class="deploy-steps-workbench__split">
      <section class="deploy-steps-workbench__checklist">
        <h3 class="deploy-steps-workbench__checklist-title">
          {{ t('deployCenter.stepsWorkbench.checklistTitle') }}
        </h3>
        <div class="deploy-steps-workbench__checklist-list">
          <button
            v-for="(item, index) in checklist"
            :key="item.id"
            type="button"
            class="deploy-steps-workbench__checklist-row"
            :class="{ 'is-active': selectedId === item.id, 'is-done': isItemDone(item) }"
            @click="selectedId = item.id"
          >
            <span class="deploy-steps-workbench__check-box" :class="{ 'is-done': isItemDone(item) }">
              <el-icon v-if="isItemDone(item)"><Check /></el-icon>
            </span>
            <span class="deploy-steps-workbench__check-order">{{ index + 1 }}.</span>
            <span class="deploy-steps-workbench__check-label">{{ item.title }}</span>
            <span
              class="deploy-steps-workbench__check-badge"
              :class="isItemDone(item) ? 'is-done' : 'is-pending'"
            >
              {{ isItemDone(item) ? t('deployCenter.stepsWorkbench.done') : t('deployCenter.stepsWorkbench.pending') }}
            </span>
            <el-icon class="deploy-steps-workbench__check-chevron"><ArrowRight /></el-icon>
          </button>
        </div>
      </section>

      <section v-if="selectedItem" class="deploy-steps-workbench__detail">
        <h3 class="deploy-steps-workbench__detail-title">{{ selectedItem.detailTitle }}</h3>
        <p class="deploy-steps-workbench__detail-desc">{{ selectedItem.detailDesc }}</p>

        <h4 class="deploy-steps-workbench__detail-sub">{{ t('deployCenter.stepsWorkbench.inspectSteps') }}</h4>
        <ol class="deploy-steps-workbench__steps">
          <li v-for="(step, stepIndex) in selectedItem.inspectSteps" :key="stepIndex">
            {{ step }}
          </li>
        </ol>

        <h4 class="deploy-steps-workbench__detail-sub">{{ t('deployCenter.stepsWorkbench.exampleCommands') }}</h4>
        <DeployCodeBlock :commands="selectedItem.commands" />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowRight,
  Check,
  FirstAidKit,
  Link,
  Loading,
  Monitor,
  Promotion,
} from '@element-plus/icons-vue'
import { useSystemHealth } from '@/composables/useSystemHealth'
import DeployCodeBlock from '@/components/deploy/DeployCodeBlock.vue'
import DeployRunDialog from '@/components/deploy/DeployRunDialog.vue'
import { fetchDeployRunnerStatus } from '@/api/deployRunner'
import {
  deployAdminUrl,
  deployAppNodeId,
  deployDataNodeId,
  deployStepsChecklist,
  type DeployStepsChecklistItem,
} from '@/data/deploy-center'
import { formatNodePairStatus } from '@/utils/deployOverviewStatus'
import { formatDeployTime } from '@/utils/deployTimeFormat'

const { t, locale } = useI18n()
const { healthData, healthLoading, refreshHealth } = useSystemHealth()

const checklist = deployStepsChecklist
const selectedId = ref(checklist[0]?.id ?? '')
const deployDialogVisible = ref(false)
const deployTarget = ref<'backend' | 'frontend'>('backend')
const deployRunning = ref(false)

const selectedItem = computed(() => checklist.find((item) => item.id === selectedId.value))

const healthUp = computed(() => healthData.value?.status === 'UP')
const redisStatus = computed(() => healthData.value?.redis ?? '')
const mysqlStatus = computed(() => healthData.value?.mysql ?? '')
const lastDeployAt = computed(
  () => healthData.value?.lastDeployAt ?? healthData.value?.startedAt ?? '',
)

const nodePairStatus = computed(() => formatNodePairStatus(healthData.value, healthLoading.value))

const deployTimeDisplay = computed(() => {
  if (!lastDeployAt.value) {
    return {
      relative: t('deployCenter.stepsWorkbench.recentDeployUnknown'),
      absolute: t('deployCenter.stepsWorkbench.recentDeployHint'),
    }
  }
  return formatDeployTime(lastDeployAt.value, locale.value)
})

const statusCards = computed(() => {
  const checking = healthLoading.value
  const redisUp = redisStatus.value === 'UP'
  const mysqlUp = mysqlStatus.value === 'UP'
  const nodeFootKey = nodePairStatus.value.footKey

  return [
    {
      key: 'nodes',
      label: t('deployCenter.stepsWorkbench.nodeStatus'),
      value: `${deployAppNodeId}/${deployDataNodeId}`,
      foot: t(`deployCenter.stepsWorkbench.${nodeFootKey}`),
      tone: nodePairStatus.value.tone,
      valueClass: nodePairStatus.value.tone === 'green' ? 'is-up' : '',
      dotClass:
        nodePairStatus.value.tone === 'green'
          ? 'is-up'
          : nodePairStatus.value.tone === 'orange'
            ? 'is-warn'
            : checking
              ? 'is-checking'
              : 'is-down',
    },
    {
      key: 'mysql',
      label: t('deployCenter.stepsWorkbench.mysqlNode'),
      value: deployDataNodeId,
      foot: checking ? '…' : mysqlUp ? 'UP' : mysqlStatus.value || '—',
      tone: mysqlUp ? 'green' : checking ? 'blue' : 'gray',
      valueClass: mysqlUp ? 'is-up' : '',
      dotClass: mysqlUp ? 'is-up' : checking ? 'is-checking' : 'is-down',
    },
    {
      key: 'redis',
      label: 'Redis',
      value: redisUp ? 'UP' : redisStatus.value || '—',
      foot: checking ? '…' : redisUp ? 'UP' : redisStatus.value || '—',
      tone: redisUp ? 'green' : checking ? 'blue' : 'gray',
      valueClass: redisUp ? 'is-up' : '',
      dotClass: redisUp ? 'is-up' : checking ? 'is-checking' : 'is-down',
    },
    {
      key: 'deploy',
      label: t('deployCenter.stepsWorkbench.recentDeploy'),
      value: deployTimeDisplay.value.relative,
      foot: deployTimeDisplay.value.absolute,
      tone: lastDeployAt.value ? 'blue' : 'gray',
      valueClass: lastDeployAt.value ? 'is-info' : '',
      dotClass: lastDeployAt.value ? 'is-info' : 'is-down',
    },
  ]
})

function isItemDone(item: DeployStepsChecklistItem): boolean {
  if (item.id === 'node-status') return healthUp.value && mysqlStatus.value === 'UP'
  if (item.autoComplete === 'health') return healthUp.value
  if (item.autoComplete === 'redis') return redisStatus.value === 'UP'
  if (item.autoComplete === 'mysql') return mysqlStatus.value === 'UP'
  return false
}

async function runHealthCheck(showToast: boolean) {
  await refreshHealth(showToast)
  if (showToast) {
    if (healthUp.value) {
      ElMessage.success(t('deployCenter.apiHealthOkToast'))
    } else {
      ElMessage.error(t('deployCenter.apiHealthFailToast'))
    }
  }
}

async function startDeploy(target: 'backend' | 'frontend') {
  let deployMode: 'local' | 'remote' = 'remote'
  try {
    const status = await fetchDeployRunnerStatus()
    deployMode = status.deployMode === 'local' ? 'local' : 'remote'
  } catch {
    // 默认按远程部署文案提示
  }

  const title =
    target === 'backend'
      ? t('deployCenter.deployRun.confirmTitleBackend')
      : t('deployCenter.deployRun.confirmTitleFrontend')
  const message =
    target === 'backend'
      ? deployMode === 'local'
        ? t('deployCenter.deployRun.confirmBackendLocal')
        : t('deployCenter.deployRun.confirmBackend')
      : deployMode === 'local'
        ? t('deployCenter.deployRun.confirmFrontendLocal')
        : t('deployCenter.deployRun.confirmFrontend')

  try {
    await ElMessageBox.confirm(message, title, {
      type: 'warning',
      confirmButtonText: t('deployCenter.deployRun.confirmContinue'),
      cancelButtonText: t('deployCenter.deployRun.confirmCancel'),
    })
  } catch {
    return
  }

  deployTarget.value = target
  deployRunning.value = true
  deployDialogVisible.value = true
}

function onDeployFinished(success: boolean) {
  deployRunning.value = false
  if (success) void refreshHealth(true)
}

function openAdminUrl() {
  window.open(deployAdminUrl, '_blank', 'noopener,noreferrer')
}

onMounted(() => {
  void refreshHealth()
})

watch(deployDialogVisible, (open) => {
  if (!open) deployRunning.value = false
})
</script>

<style scoped lang="scss">
.deploy-steps-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__status-row {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
  }

  &__status-card {
    padding: 18px 16px 14px;
    border-radius: 14px;
    background: #fff;
    border: 1px solid #e8ecf2;
    box-shadow: 0 1px 2px rgb(15 23 42 / 4%);

    &.is-green {
      border-color: #d1fae5;
    }

    &.is-blue {
      border-color: #dbeafe;
    }

    &.is-orange {
      border-color: #fed7aa;
    }
  }

  &__status-label {
    font-size: 13px;
    color: #6b7280;
    margin-bottom: 8px;
  }

  &__status-value {
    font-size: 26px;
    font-weight: 700;
    line-height: 1.2;
    color: #111827;
    margin-bottom: 10px;
    word-break: break-all;

    &.is-up {
      color: #16a34a;
    }

    &.is-info {
      color: #2563eb;
      font-size: 22px;
    }
  }

  &__status-foot {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #6b7280;
  }

  &__status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #9ca3af;

    &.is-up {
      background: #22c55e;
    }

    &.is-down {
      background: #ef4444;
    }

    &.is-checking {
      background: #3b82f6;
    }

    &.is-warn {
      background: #f59e0b;
    }

    &.is-info {
      background: #3b82f6;
    }
  }

  &__actions {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
  }

  &__action {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: 56px;
    padding: 14px 16px;
    border-radius: 12px;
    border: none;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: filter 0.15s, box-shadow 0.15s;

    .el-icon {
      font-size: 18px;
    }

    &:disabled {
      opacity: 0.75;
      cursor: wait;
    }

    &.is-backend {
      color: #fff;
      background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%);
      box-shadow: 0 2px 8px rgb(37 99 235 / 28%);
    }

    &.is-frontend {
      color: #fff;
      background: linear-gradient(180deg, #22c55e 0%, #16a34a 100%);
      box-shadow: 0 2px 8px rgb(22 163 74 / 24%);
    }

    &.is-health {
      color: #fff;
      background: linear-gradient(180deg, #a855f7 0%, #7c3aed 100%);
      box-shadow: 0 2px 8px rgb(124 58 237 / 24%);
    }

    &.is-visit {
      color: #374151;
      background: #fff;
      border: 1px solid #d1d5db;
      box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
    }

    &:hover:not(:disabled) {
      filter: brightness(1.03);
    }
  }

  &__split {
    display: grid;
    grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
    gap: 16px;
    align-items: start;
  }

  &__checklist,
  &__detail {
    padding: 18px 16px;
    border-radius: 14px;
    background: #fff;
    border: 1px solid #e8ecf2;
  }

  &__checklist-title,
  &__detail-title {
    margin: 0 0 14px;
    font-size: 15px;
    font-weight: 700;
    color: #111827;
  }

  &__checklist-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__checklist-row {
    display: grid;
    grid-template-columns: auto auto 1fr auto auto;
    align-items: center;
    gap: 8px;
    width: 100%;
    padding: 10px 12px;
    border: 1px solid #eef2f7;
    border-radius: 10px;
    background: #fafbfc;
    text-align: left;
    cursor: pointer;
    transition: border-color 0.15s, background 0.15s;

    &:hover,
    &.is-active {
      border-color: #bfdbfe;
      background: #eff6ff;
    }

    &.is-done .deploy-steps-workbench__check-label {
      color: #1d4ed8;
    }
  }

  &__check-box {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    border: 2px solid #d1d5db;
    border-radius: 4px;
    color: #fff;
    font-size: 12px;

    &.is-done {
      border-color: #2563eb;
      background: #2563eb;
    }
  }

  &__check-order {
    font-size: 13px;
    font-weight: 600;
    color: #6b7280;
  }

  &__check-label {
    font-size: 13px;
    font-weight: 600;
    color: #374151;
    min-width: 0;
  }

  &__check-badge {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 999px;
    white-space: nowrap;

    &.is-done {
      color: #1d4ed8;
      background: #dbeafe;
    }

    &.is-pending {
      color: #6b7280;
      background: #f3f4f6;
    }
  }

  &__check-chevron {
    color: #9ca3af;
    font-size: 14px;
  }

  &__detail-desc {
    margin: 0 0 16px;
    font-size: 13px;
    color: #6b7280;
    line-height: 1.7;
  }

  &__detail-sub {
    margin: 0 0 10px;
    font-size: 13px;
    font-weight: 700;
    color: #374151;
  }

  &__steps {
    margin: 0 0 18px;
    padding-left: 20px;
    font-size: 13px;
    color: #4b5563;
    line-height: 1.7;
  }
}

@media (max-width: 1100px) {
  .deploy-steps-workbench {
    &__status-row,
    &__actions {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    &__split {
      grid-template-columns: 1fr;
    }
  }
}

@media (max-width: 600px) {
  .deploy-steps-workbench {
    &__status-row,
    &__actions {
      grid-template-columns: 1fr;
    }

    &__checklist-row {
      grid-template-columns: auto auto 1fr;
      grid-template-rows: auto auto;

      .deploy-steps-workbench__check-badge,
      .deploy-steps-workbench__check-chevron {
        grid-column: 3;
      }
    }
  }
}
</style>
