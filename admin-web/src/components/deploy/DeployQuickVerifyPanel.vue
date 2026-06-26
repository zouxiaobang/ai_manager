<template>
  <div class="deploy-quick-verify">
    <DeployCodeBlock
      :title="title"
      :description="description"
      :commands="commands"
      :hint="hint"
    />
    <div class="deploy-quick-verify__footer">
      <div
        class="deploy-quick-verify__status"
        :class="`is-${healthStatus}`"
      >
        <el-icon v-if="healthStatus === 'checking'" class="is-loading">
          <Loading />
        </el-icon>
        <el-icon v-else-if="healthStatus === 'up'">
          <CircleCheck />
        </el-icon>
        <el-icon v-else-if="healthStatus === 'down'">
          <CircleClose />
        </el-icon>
        <el-icon v-else>
          <InfoFilled />
        </el-icon>
        <span class="deploy-quick-verify__status-text">{{ statusLabel }}</span>
        <span v-if="statusDetail" class="deploy-quick-verify__status-detail">{{ statusDetail }}</span>
      </div>
      <button
        type="button"
        class="deploy-quick-verify__health-btn"
        :disabled="healthLoading"
        @click="runHealthCheck(true)"
      >
        <el-icon v-if="healthLoading" class="is-loading"><Loading /></el-icon>
        <el-icon v-else><Connection /></el-icon>
        {{ t('deployCenter.apiHealthButton') }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  CircleCheck,
  CircleClose,
  Connection,
  InfoFilled,
  Loading,
} from '@element-plus/icons-vue'
import { useSystemHealth } from '@/composables/useSystemHealth'
import DeployCodeBlock from '@/components/deploy/DeployCodeBlock.vue'

defineProps<{
  title?: string
  description?: string
  commands: string[]
  hint?: string
}>()

const { t } = useI18n()
const { healthData, healthLoading, refreshHealth } = useSystemHealth()

type HealthStatus = 'unknown' | 'checking' | 'up' | 'down'

const healthStatus = computed<HealthStatus>(() => {
  if (healthLoading.value) return 'checking'
  if (healthData.value?.status === 'UP') return 'up'
  if (healthData.value) return 'down'
  return 'unknown'
})

const statusLabel = computed(() => {
  if (healthStatus.value === 'checking') return t('deployCenter.apiHealthChecking')
  if (healthStatus.value === 'up') return t('deployCenter.apiHealthUp')
  if (healthStatus.value === 'down') return t('deployCenter.apiHealthDown')
  return t('deployCenter.apiHealthUnknown')
})

const statusDetail = computed(() => {
  if (healthStatus.value === 'down' && healthData.value?.mysql === 'DOWN') {
    return healthData.value.mysqlError || 'MySQL DOWN'
  }
  if (healthStatus.value === 'down' && healthData.value?.redis === 'DOWN') {
    return healthData.value.redisError || 'Redis DOWN'
  }
  return ''
})

async function runHealthCheck(showToast: boolean) {
  await refreshHealth(true)
  if (!showToast) return
  if (healthStatus.value === 'up') {
    ElMessage.success(t('deployCenter.apiHealthOkToast'))
  } else {
    ElMessage.error(t('deployCenter.apiHealthFailToast'))
  }
}

onMounted(() => {
  void refreshHealth()
})
</script>

<style scoped lang="scss">
.deploy-quick-verify {
  &__footer {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    margin-top: 12px;
  }

  &__status {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 6px;
    min-width: 0;
    flex: 1;
    padding: 10px 12px;
    border-radius: 10px;
    font-size: 13px;
    line-height: 1.5;
    border: 1px solid #e5e7eb;
    background: #f9fafb;
    color: #6b7280;

    .el-icon {
      flex-shrink: 0;
      font-size: 16px;
    }

    &.is-up {
      background: #f0fdf4;
      border-color: #bbf7d0;
      color: #15803d;

      .el-icon {
        color: #16a34a;
      }
    }

    &.is-down {
      background: #fef2f2;
      border-color: #fecaca;
      color: #b91c1c;

      .el-icon {
        color: #dc2626;
      }
    }

    &.is-checking {
      background: #eff6ff;
      border-color: #bfdbfe;
      color: #1d4ed8;

      .el-icon {
        color: #2563eb;
      }
    }
  }

  &__status-text {
    font-weight: 600;
  }

  &__status-detail {
    color: inherit;
    opacity: 0.85;
    word-break: break-all;
  }

  &__health-btn {
    flex-shrink: 0;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 9px 14px;
    border: 1px solid #bfdbfe;
    border-radius: 10px;
    background: linear-gradient(180deg, #eff6ff 0%, #dbeafe 100%);
    color: #1d4ed8;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 1px 3px rgb(37 99 235 / 12%);
    transition: background 0.15s, border-color 0.15s, box-shadow 0.15s;

    .el-icon {
      font-size: 15px;
    }

    &:hover:not(:disabled) {
      background: #dbeafe;
      border-color: #93c5fd;
      box-shadow: 0 2px 6px rgb(37 99 235 / 16%);
    }

    &:disabled {
      opacity: 0.75;
      cursor: wait;
    }
  }
}

@media (max-width: 700px) {
  .deploy-quick-verify__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .deploy-quick-verify__health-btn {
    justify-content: center;
  }
}
</style>
