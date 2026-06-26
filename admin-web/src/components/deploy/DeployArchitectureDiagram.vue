<template>
  <div class="deploy-arch-diagram">
    <div class="deploy-arch-diagram__flow">
      <template v-for="(step, index) in nodes" :key="step.key">
        <div class="deploy-arch-diagram__cell">
          <DeployHoverPopover v-if="hasDeployNodeHoverTip(step.key)" :width="hoverTipWidth(step.key)">
            <DeployArchCard :step="step" />
            <template #content>
              <DeployAppNodePathsTip v-if="step.key === 'app'" />
              <DeployDataNodeCredentialsTip v-else-if="step.key === 'data'" />
              <DeployStorageNodePathsTip v-else-if="step.key === 'storage'" />
            </template>
          </DeployHoverPopover>
          <DeployArchCard v-else :step="step" />
        </div>
        <span v-if="index < nodes.length - 1" class="deploy-arch-diagram__arrow" aria-hidden="true">
          →
        </span>
      </template>
    </div>

    <div class="deploy-arch-diagram__footer">
      <div class="deploy-arch-diagram__feedback">
        <svg
          class="deploy-arch-diagram__feedback-svg"
          viewBox="0 0 1000 72"
          preserveAspectRatio="none"
          aria-hidden="true"
        >
          <defs>
            <marker
              id="deploy-arch-feedback-arrow"
              markerWidth="10"
              markerHeight="10"
              refX="8"
              refY="5"
              orient="auto"
            >
              <path d="M0,0 L10,5 L0,10 Z" fill="#1d4ed8" />
            </marker>
          </defs>
          <path
            class="deploy-arch-diagram__feedback-path"
            d="M 900 4 L 900 38 Q 900 44 894 44 L 306 44 Q 300 44 300 38 L 300 4"
            fill="none"
            stroke="#2563eb"
            stroke-width="2.5"
            stroke-dasharray="9 6"
            stroke-linecap="round"
            stroke-linejoin="round"
            marker-end="url(#deploy-arch-feedback-arrow)"
          />
        </svg>
        <div class="deploy-arch-diagram__feedback-label-anchor">
          <DeployHoverPopover :width="420" inline placement="top">
            <span class="deploy-arch-diagram__feedback-label">{{ monitorLabel }}</span>
            <template #content>
              <DeployMonitorLogPathsTip />
            </template>
          </DeployHoverPopover>
        </div>
      </div>
      <button
        type="button"
        class="deploy-arch-diagram__cred-btn"
        @click="credentialsVisible = true"
      >
        <el-icon><Key /></el-icon>
        {{ credentialsButtonLabel }}
      </button>
    </div>

    <el-dialog
      v-model="credentialsVisible"
      :title="credentialsDialogTitle"
      width="640px"
      top="8vh"
      class="deploy-credentials-dialog"
      destroy-on-close
    >
      <DeployCredentialsQuickPanel />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Key } from '@element-plus/icons-vue'
import DeployAppNodePathsTip from '@/components/deploy/DeployAppNodePathsTip.vue'
import DeployArchCard from '@/components/deploy/DeployArchCard.vue'
import DeployCredentialsQuickPanel from '@/components/deploy/DeployCredentialsQuickPanel.vue'
import DeployDataNodeCredentialsTip from '@/components/deploy/DeployDataNodeCredentialsTip.vue'
import DeployHoverPopover from '@/components/deploy/DeployHoverPopover.vue'
import DeployMonitorLogPathsTip from '@/components/deploy/DeployMonitorLogPathsTip.vue'
import DeployStorageNodePathsTip from '@/components/deploy/DeployStorageNodePathsTip.vue'
import type { DeployArchitectureNode } from '@/data/deploy-center'
import { hasDeployNodeHoverTip, hoverTipWidth } from '@/data/deploy-center'

defineProps<{
  nodes: DeployArchitectureNode[]
  monitorLabel: string
  credentialsButtonLabel: string
  credentialsDialogTitle: string
}>()

const credentialsVisible = ref(false)
</script>

<style scoped lang="scss">
.deploy-arch-diagram {
  &__flow {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr) auto minmax(0, 1fr) auto minmax(0, 1fr) auto minmax(0, 1fr);
    align-items: stretch;
    column-gap: 6px;
  }

  &__cell {
    min-width: 0;
    min-height: 108px;
    display: flex;

    > .deploy-arch-card,
    > .deploy-hover-popover {
      flex: 1;
      min-width: 0;
      width: 100%;
    }
  }

  &__arrow {
    flex-shrink: 0;
    align-self: center;
    color: #9ca3af;
    font-size: 20px;
    font-weight: 300;
    line-height: 1;
    padding: 0 2px;
  }

  &__footer {
    position: relative;
    margin-top: 4px;
    min-height: 76px;
    padding: 0 4px;
  }

  &__feedback {
    position: relative;
    height: 76px;
  }

  &__feedback-svg {
    display: block;
    width: 100%;
    height: 100%;
  }

  &__feedback-path {
    vector-effect: non-scaling-stroke;
  }

  &__feedback-label-anchor {
    position: absolute;
    left: 60%;
    top: calc(44 / 72 * 100%);
    transform: translate(-50%, -50%);
    z-index: 1;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: max-content;
    height: max-content;
    line-height: 1;
    pointer-events: auto;

    :deep(.deploy-hover-popover.is-inline) {
      display: inline-flex;
      flex: none;
      width: auto;
      height: auto;
      line-height: 1;
    }

    :deep(.deploy-hover-popover.is-inline .deploy-hover-popover__ref) {
      display: inline-flex;
      flex: none;
      align-items: center;
      width: auto;
      height: auto;
      line-height: 1;
    }

    :deep(.el-tooltip__trigger) {
      display: inline-flex;
      align-items: center;
      width: auto;
      height: auto;
      line-height: 1;
    }
  }

  &__feedback-label {
    display: inline-flex;
    align-items: center;
    flex: none;
    box-sizing: border-box;
    width: auto;
    height: auto;
    line-height: 1.35;
    padding: 5px 14px;
    border-radius: 999px;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
    font-size: 12px;
    font-weight: 600;
    color: #2563eb;
    white-space: nowrap;
    box-shadow: 0 1px 2px rgba(37, 99, 235, 0.08);
    cursor: help;
    transition: background 0.15s, border-color 0.15s, box-shadow 0.15s;

    &:hover {
      background: #dbeafe;
      border-color: #93c5fd;
      box-shadow: 0 2px 6px rgba(37, 99, 235, 0.12);
    }
  }

  &__cred-btn {
    position: absolute;
    right: 4px;
    bottom: 4px;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 14px;
    border: 1px solid #fde68a;
    border-radius: 10px;
    background: linear-gradient(180deg, #fffbeb 0%, #fef3c7 100%);
    color: #b45309;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 1px 3px rgba(180, 83, 9, 0.12);
    transition: background 0.15s, border-color 0.15s, box-shadow 0.15s;

    .el-icon {
      font-size: 15px;
    }

    &:hover {
      background: #fef3c7;
      border-color: #fbbf24;
      box-shadow: 0 2px 6px rgba(180, 83, 9, 0.16);
    }
  }
}

@media (max-width: 1100px) {
  .deploy-arch-diagram__flow {
    display: flex;
    flex-wrap: wrap;
    row-gap: 10px;
    column-gap: 10px;
  }

  .deploy-arch-diagram__cell {
    flex: 1 1 calc(50% - 10px);
    min-width: 160px;
  }

  .deploy-arch-diagram__arrow {
    display: none;
  }

  .deploy-arch-diagram__feedback {
    display: none;
  }

  .deploy-arch-diagram__footer {
    min-height: 0;
    display: flex;
    justify-content: flex-end;
    padding-top: 8px;
  }

  .deploy-arch-diagram__cred-btn {
    position: static;
  }
}
</style>

<style lang="scss">
.deploy-credentials-dialog {
  .el-dialog__body {
    max-height: calc(100vh - 140px);
    overflow-x: hidden;
    overflow-y: auto;
    padding-bottom: 20px;
  }
}
</style>
