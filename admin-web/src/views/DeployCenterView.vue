<template>
  <div class="deploy-center war-room-page">
    <div class="deploy-center__shell">
      <section class="deploy-center__main">
        <header class="deploy-center__header">
          <h1 class="deploy-center__page-title">{{ t('deployCenter.title') }}</h1>
          <p class="deploy-center__page-sub">{{ t('deployCenter.guideTitle') }}</p>
        </header>

        <el-tabs v-model="activeTab" class="deploy-center__tabs">
          <el-tab-pane :label="t('deployCenter.tabs.overview')" name="overview">
            <div class="deploy-center__panel">
              <div class="deploy-overview-cards">
                <div
                  v-for="node in overviewNodes"
                  :key="node.key"
                  class="deploy-overview-cards__cell"
                >
                  <DeployOverviewCard :node="node" />
                </div>
              </div>

              <section class="deploy-panel-card">
                <h2 class="deploy-panel-card__title">{{ t('deployCenter.architectureTitle') }}</h2>
                <DeployArchitectureDiagram
                  :nodes="deployArchitectureFlow"
                  :monitor-label="t('deployCenter.monitorLabel')"
                  :credentials-button-label="t('deployCenter.tabs.credentials')"
                  :credentials-dialog-title="t('deployCenter.credentialsTitle')"
                />
              </section>

              <section class="deploy-panel-card">
                <DeployQuickVerifyPanel
                  :title="deployQuickVerify.title"
                  :description="deployQuickVerify.description"
                  :commands="quickVerifyCommands"
                  :hint="t('deployCenter.healthHint')"
                />
              </section>

              <section class="deploy-panel-card deploy-panel-card--notes">
                <h2 class="deploy-panel-card__title">{{ t('deployCenter.importantNotes') }}</h2>
                <ul class="deploy-notes">
                  <li v-for="(note, index) in deployImportantNotes" :key="index">{{ note }}</li>
                </ul>
              </section>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('deployCenter.tabs.steps')" name="steps">
            <div class="deploy-center__panel">
              <DeployStepsWorkbench @deploy-finished="onDeployFinished" />

              <section class="deploy-panel-card deploy-panel-card--docs">
                <h2 class="deploy-panel-card__title">{{ t('deployCenter.stepsWorkbench.docsTitle') }}</h2>
                <el-collapse v-model="expandedSteps" class="deploy-steps">
                  <el-collapse-item
                    v-for="(section, index) in deployStepSections"
                    :key="section.id"
                    :name="section.id"
                    :title="`${index + 1}. ${section.title}`"
                  >
                    <p class="deploy-steps__summary">{{ section.summary }}</p>
                    <DeployCodeBlock
                      v-for="(block, index) in section.blocks"
                      :key="`${section.id}-${index}`"
                      :title="block.title"
                      :commands="block.commands"
                      class="deploy-steps__block"
                    />
                  </el-collapse-item>
                </el-collapse>
              </section>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('deployCenter.tabs.database')" name="database">
            <div class="deploy-center__panel">
              <DeployDatabasePanel :active="activeTab === 'database'" />
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('deployCenter.tabs.versions')" name="versions">
            <div class="deploy-center__panel">
              <DeployVersionPanel ref="versionPanelRef" :active="activeTab === 'versions'" />
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('deployCenter.tabs.troubleshooting')" name="troubleshooting">
            <div class="deploy-center__panel">
              <section class="deploy-panel-card">
                <h2 class="deploy-panel-card__title">{{ t('deployCenter.troubleTitle') }}</h2>
                <div class="deploy-trouble-table">
                  <div class="deploy-trouble-table__head">
                    <span>{{ t('deployCenter.troubleSymptom') }}</span>
                    <span>{{ t('deployCenter.troubleAction') }}</span>
                  </div>
                  <div
                    v-for="(row, index) in deployTroubleshooting"
                    :key="index"
                    class="deploy-trouble-table__row"
                  >
                    <span class="deploy-trouble-table__symptom">{{ row.symptom }}</span>
                    <span class="deploy-trouble-table__action">{{ row.action }}</span>
                  </div>
                </div>
              </section>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DeployArchitectureDiagram from '@/components/deploy/DeployArchitectureDiagram.vue'
import DeployCodeBlock from '@/components/deploy/DeployCodeBlock.vue'
import DeployOverviewCard from '@/components/deploy/DeployOverviewCard.vue'
import DeployQuickVerifyPanel from '@/components/deploy/DeployQuickVerifyPanel.vue'
import DeployDatabasePanel from '@/components/deploy/DeployDatabasePanel.vue'
import DeployStepsWorkbench from '@/components/deploy/DeployStepsWorkbench.vue'
import DeployVersionPanel from '@/components/deploy/DeployVersionPanel.vue'
import { useSystemHealth } from '@/composables/useSystemHealth'
import {
  deployArchitectureFlow,
  deployImportantNotes,
  deployQuickVerify,
  deployStepSections,
  deployTroubleshooting,
  type DeployCenterTab,
} from '@/data/deploy-center'
import { buildDeployOverviewNodes } from '@/utils/deployOverviewStatus'

const { t } = useI18n()
const { healthData, healthLoading, refreshHealth } = useSystemHealth()

const activeTab = ref<DeployCenterTab>('overview')
const expandedSteps = ref<string[]>([])
const versionPanelRef = ref<InstanceType<typeof DeployVersionPanel> | null>(null)

function onDeployFinished() {
  versionPanelRef.value?.reload()
}

const overviewNodes = computed(() =>
  buildDeployOverviewNodes(healthData.value, healthLoading.value, t),
)

const quickVerifyCommands = [
  'curl.exe -s http://192.168.0.114/api/health',
  '# 或 Linux / 树莓派上：',
  'curl -fsS http://192.168.0.114/api/health',
  '',
  '# 示例响应',
  '{',
  '  "code": 0,',
  '  "message": "success",',
  '  "data": { "status": "UP", "mysql": "UP", "redis": "UP", "lastDeployAt": "..." }',
  '}',
]

onMounted(() => {
  void refreshHealth()
})
</script>

<style scoped lang="scss">
.deploy-center {
  padding: 0;
  min-height: 100%;

  &__shell {
    min-height: 100%;
    background: #f3f5f9;
  }

  &__main {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  &__header {
    flex-shrink: 0;
    padding: 22px 28px 0;
    background: #f3f5f9;
  }

  &__page-title {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: #111827;
  }

  &__page-sub {
    margin: 6px 0 0;
    font-size: 14px;
    color: #6b7280;
  }

  &__tabs {
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: 0 28px 32px;

    :deep(.el-tabs__header) {
      margin: 12px 0 0;
      background: #f3f5f9;
    }

    :deep(.el-tabs__nav-wrap::after) {
      height: 1px;
      background: #e5e7eb;
    }

    :deep(.el-tabs__item) {
      height: 42px;
      font-size: 14px;
      color: #6b7280;

      &.is-active {
        color: #2563eb;
        font-weight: 600;
      }
    }

    :deep(.el-tabs__active-bar) {
      background: #2563eb;
    }

    :deep(.el-tabs__content) {
      flex: 1;
      padding-top: 20px;
    }
  }

  &__panel {
    display: flex;
    flex-direction: column;
    gap: 18px;
    padding-bottom: 24px;
  }
}

.deploy-overview-cards {
  display: flex;
  align-items: stretch;
  gap: 14px;
  width: 100%;

  &__cell {
    flex: 1;
    min-width: 0;
    display: flex;
  }
}

.deploy-panel-card {
  padding: 20px 22px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #e8ecf2;

  &--notes {
    background: #fffbeb;
    border-color: #fde68a;
  }

  &--docs {
    :deep(.deploy-steps) {
      margin-top: 4px;
    }
  }

  &__title {
    margin: 0 0 12px;
    font-size: 16px;
    font-weight: 700;
    color: #111827;
  }

  &__desc {
    margin: -4px 0 16px;
    font-size: 13px;
    color: #6b7280;
    line-height: 1.6;
  }
}

.deploy-notes {
  margin: 0;
  padding-left: 18px;
  color: #92400e;
  font-size: 13px;
  line-height: 1.7;
}

.deploy-steps {
  border: none;

  :deep(.el-collapse-item) {
    margin-bottom: 12px;
    border: 1px solid #e8ecf2;
    border-radius: 12px;
    overflow: hidden;
    background: #fff;
  }

  :deep(.el-collapse-item__header) {
    height: auto;
    min-height: 48px;
    padding: 12px 16px;
    font-size: 14px;
    font-weight: 600;
    color: #111827;
    border: none;
    background: #fff;
  }

  :deep(.el-collapse-item__wrap) {
    border: none;
  }

  :deep(.el-collapse-item__content) {
    padding: 0 16px 16px;
  }

  &__summary {
    margin: 0 0 14px;
    font-size: 13px;
    color: #6b7280;
    line-height: 1.6;
  }

  &__block {
    margin-bottom: 14px;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.deploy-trouble-table {
  border: 1px solid #e8ecf2;
  border-radius: 12px;
  overflow: hidden;

  &__head,
  &__row {
    display: grid;
    grid-template-columns: 220px 1fr;
    gap: 16px;
    padding: 12px 16px;
    font-size: 13px;
    line-height: 1.6;
  }

  &__head {
    background: #f8fafc;
    font-weight: 600;
    color: #374151;
    border-bottom: 1px solid #e8ecf2;
  }

  &__row {
    border-bottom: 1px solid #f1f5f9;

    &:last-child {
      border-bottom: none;
    }
  }

  &__symptom {
    color: #b45309;
    font-weight: 600;
  }

  &__action {
    color: #374151;
  }
}

@media (max-width: 1200px) {
  .deploy-overview-cards {
    flex-wrap: wrap;
  }

  .deploy-overview-cards__cell {
    flex: 1 1 calc(50% - 7px);
  }
}

@media (max-width: 900px) {
  .deploy-overview-cards {
    flex-direction: column;
  }

  .deploy-overview-cards__cell {
    flex: 1 1 auto;
  }

  .deploy-trouble-table__head,
  .deploy-trouble-table__row {
    grid-template-columns: 1fr;
  }
}
</style>
