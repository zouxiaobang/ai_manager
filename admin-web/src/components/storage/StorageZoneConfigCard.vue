<template>
  <section v-if="config" class="storage-config-card">
    <div class="storage-config-card__head">
      <h3 class="storage-config-card__title">{{ t('storageCenter.configCardTitle') }}</h3>
      <div v-if="editing" class="storage-config-card__head-actions">
        <el-button @click="emit('cancel')">{{ t('storageCenter.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="emit('save')">
          {{ t('storageCenter.saveConfig') }}
        </el-button>
      </div>
      <el-button v-else type="primary" link @click="emit('edit')">
        {{ t('storageCenter.editConfig') }}
      </el-button>
    </div>

    <div class="storage-config-card__info">
      <template v-if="isRedisZone">
        <div class="storage-config-card__columns">
          <div class="storage-config-card__column">
            <div class="storage-config-card__row">
              <dt>{{ t('storageCenter.cacheMaxMb') }}</dt>
              <dd>
                <template v-if="editing && draft">
                  <StorageQuotaInput v-model="draft.cacheMaxMb" :mb-step="64" :gb-step="0.5" />
                </template>
                <StorageMbQuotaText v-else-if="config.cacheMaxMb > 0" :mb="config.cacheMaxMb" />
                <span v-else>{{ t('storageCenter.noQuota') }}</span>
              </dd>
            </div>
            <div class="storage-config-card__row">
              <dt>{{ t('storageCenter.cacheTtlSeconds') }}</dt>
              <dd>
                <el-input-number
                  v-if="editing && draft"
                  v-model="draft.cacheTtlSeconds"
                  :min="60"
                  :step="60"
                  controls-position="right"
                />
                <span v-else>{{ `${config.cacheTtlSeconds} s` }}</span>
              </dd>
            </div>
            <div v-if="config.updateTime" class="storage-config-card__row">
              <dt>{{ t('storageCenter.configUpdatedAt') }}</dt>
              <dd>{{ config.updateTime }}</dd>
            </div>
          </div>
          <div class="storage-config-card__column">
            <div class="storage-config-card__row">
              <dt>{{ t('storageCenter.overLimitStrategy') }}</dt>
              <dd>
                <el-radio-group
                  v-if="editing && draft"
                  v-model="draft.cacheOverLimitStrategy"
                  class="storage-config-card__strategy-group"
                >
                  <el-radio v-for="item in strategyOptions" :key="item" :value="item">
                    {{ formatStrategyLabel(item) }}
                  </el-radio>
                </el-radio-group>
                <el-tag
                  v-else
                  :type="strategyTagType(config.cacheOverLimitStrategy)"
                  effect="plain"
                  round
                  size="small"
                >
                  {{ formatStrategyLabel(config.cacheOverLimitStrategy) }}
                </el-tag>
              </dd>
            </div>
          </div>
        </div>
      </template>

      <template v-else-if="zone">
        <div class="storage-config-card__columns">
          <div class="storage-config-card__column">
            <div class="storage-config-card__row">
              <dt>{{ t('storageCenter.zoneQuotaLabel') }}</dt>
              <dd>
                <template v-if="editing && draft">
                  <StorageQuotaInput v-model="zoneQuotaMb" :mb-step="256" :gb-step="0.5" />
                </template>
                <StorageMbQuotaText
                  v-else-if="zoneQuotaField(zone.key, config) > 0"
                  :mb="zoneQuotaField(zone.key, config)"
                />
                <span v-else>{{ t('storageCenter.noQuota') }}</span>
              </dd>
            </div>
            <div class="storage-config-card__row">
              <dt>{{ t('storageCenter.localQuotaMb') }}</dt>
              <dd>
                <template v-if="editing && draft">
                  <StorageQuotaInput v-model="draft.localQuotaMb" :mb-step="512" :gb-step="0.5" />
                </template>
                <StorageMbQuotaText v-else-if="config.localQuotaMb > 0" :mb="config.localQuotaMb" />
                <span v-else>{{ t('storageCenter.noQuota') }}</span>
              </dd>
            </div>
            <div class="storage-config-card__row">
              <dt>{{ t('storageCenter.fileCountLabel') }}</dt>
              <dd>{{ t('storageCenter.fileCount', { count: zone.fileCount }) }}</dd>
            </div>
            <div v-if="config.updateTime" class="storage-config-card__row">
              <dt>{{ t('storageCenter.configUpdatedAt') }}</dt>
              <dd>{{ config.updateTime }}</dd>
            </div>
          </div>
          <div class="storage-config-card__column storage-config-card__column--tags">
            <div class="storage-config-card__row">
              <dt>{{ t('storageCenter.overLimitStrategy') }}</dt>
              <dd>
                <el-radio-group
                  v-if="editing && draft"
                  v-model="zoneStrategy"
                  class="storage-config-card__strategy-group"
                >
                  <el-radio v-for="item in strategyOptions" :key="item" :value="item">
                    {{ formatStrategyLabel(item) }}
                  </el-radio>
                </el-radio-group>
                <el-tag v-else :type="strategyTagType(zoneStrategyValue)" effect="plain" round size="small">
                  {{ formatStrategyLabel(zoneStrategyValue) }}
                </el-tag>
              </dd>
            </div>
            <div class="storage-config-card__row">
              <dt>{{ t('storageCenter.localQuotaOverLimitStrategy') }}</dt>
              <dd>
                <el-radio-group
                  v-if="editing && draft"
                  v-model="draft.localQuotaOverLimitStrategy"
                  class="storage-config-card__strategy-group"
                >
                  <el-radio v-for="item in strategyOptions" :key="item" :value="item">
                    {{ formatStrategyLabel(item) }}
                  </el-radio>
                </el-radio-group>
                <el-tag
                  v-else
                  :type="strategyTagType(config.localQuotaOverLimitStrategy)"
                  effect="plain"
                  round
                  size="small"
                >
                  {{ formatStrategyLabel(config.localQuotaOverLimitStrategy) }}
                </el-tag>
              </dd>
            </div>
            <div class="storage-config-card__row">
              <dt>{{ t('storageCenter.dualStorageEnabled') }}</dt>
              <dd>
                <el-switch
                  v-if="editing && draft"
                  v-model="draft.dualStorageEnabled"
                  class="storage-config-card__green-switch"
                />
                <el-tag
                  v-else
                  :type="config.dualStorageEnabled ? 'success' : 'info'"
                  effect="plain"
                  round
                  size="small"
                >
                  {{ config.dualStorageEnabled ? t('storageCenter.enabled') : t('storageCenter.disabled') }}
                </el-tag>
              </dd>
            </div>
            <div class="storage-config-card__row">
              <dt class="storage-config-card__label">
                <span>{{ t('storageCenter.defaultOverLimitStrategy') }}</span>
                <el-tooltip
                  :content="t('storageCenter.defaultOverLimitStrategyTip')"
                  placement="top"
                  :show-after="200"
                >
                  <el-icon class="storage-config-card__field-tip"><InfoFilled /></el-icon>
                </el-tooltip>
              </dt>
              <dd>
                <el-radio-group
                  v-if="editing && draft"
                  v-model="draft.overLimitStrategy"
                  class="storage-config-card__strategy-group"
                >
                  <el-radio v-for="item in strategyOptions" :key="item" :value="item">
                    {{ formatStrategyLabel(item) }}
                  </el-radio>
                </el-radio-group>
                <el-tag v-else :type="strategyTagType(config.overLimitStrategy)" effect="plain" round size="small">
                  {{ formatStrategyLabel(config.overLimitStrategy) }}
                </el-tag>
              </dd>
            </div>
          </div>
        </div>
      </template>
    </div>

    <p v-if="editing" class="storage-config-card__edit-hint">{{ t('storageCenter.configDialogHint') }}</p>

    <div v-if="$slots.actions" class="storage-config-card__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import {
  type StorageCenterConfig,
  type StorageOverLimitStrategy,
  type StorageZone,
} from '@/api/storageCenter'
import StorageMbQuotaText from '@/components/storage/StorageMbQuotaText.vue'
import StorageQuotaInput from '@/components/storage/StorageQuotaInput.vue'

const strategyOptions: StorageOverLimitStrategy[] = ['REJECT', 'CLEANUP_OLDEST', 'CLEANUP_LARGEST']

const props = defineProps<{
  zoneKey: string
  zone: StorageZone | null
  config: StorageCenterConfig | null
  editing: boolean
  draft: StorageCenterConfig | null
  saving: boolean
}>()

const emit = defineEmits<{
  edit: []
  cancel: []
  save: []
}>()

const { t } = useI18n()

const isRedisZone = computed(() => props.zoneKey === 'REDIS_CACHE')

const zoneQuotaMb = computed({
  get() {
    if (!props.draft || !props.zone) return 0
    return zoneQuotaField(props.zone.key, props.draft)
  },
  set(value: number) {
    if (!props.draft || !props.zone) return
    setZoneQuotaField(props.zone.key, props.draft, value)
  },
})

const zoneStrategy = computed({
  get() {
    if (!props.draft || !props.zone) return 'REJECT' as StorageOverLimitStrategy
    return zoneStrategyField(props.zone.key, props.draft)
  },
  set(value: StorageOverLimitStrategy) {
    if (!props.draft || !props.zone) return
    setZoneStrategyField(props.zone.key, props.draft, value)
  },
})

const zoneStrategyValue = computed(() => {
  if (!props.config || !props.zone) return 'REJECT' as StorageOverLimitStrategy
  return zoneStrategyField(props.zone.key, props.config)
})

function formatStrategyLabel(strategy: StorageOverLimitStrategy | string) {
  switch (strategy) {
    case 'CLEANUP_OLDEST':
      return t('storageCenter.strategyCleanupOldest')
    case 'CLEANUP_LARGEST':
      return t('storageCenter.strategyCleanupLargest')
    default:
      return t('storageCenter.strategyReject')
  }
}

function strategyTagType(
  strategy: StorageOverLimitStrategy | string,
): 'success' | 'warning' | 'info' | 'danger' {
  switch (strategy) {
    case 'CLEANUP_OLDEST':
      return 'warning'
    case 'CLEANUP_LARGEST':
      return 'danger'
    default:
      return 'info'
  }
}

function zoneQuotaField(key: string, config: StorageCenterConfig): number {
  switch (key) {
    case 'ECOMMERCE_IMAGES':
      return config.ecommerceImagesQuotaMb
    case 'NOTEBOOK_IMAGES':
      return config.notebookImagesQuotaMb
    case 'NOTEBOOK_CONTENT':
      return config.notebookContentQuotaMb
    case 'IMPORT_FILES':
      return config.importFilesQuotaMb
    default:
      return 0
  }
}

function setZoneQuotaField(key: string, config: StorageCenterConfig, value: number) {
  switch (key) {
    case 'ECOMMERCE_IMAGES':
      config.ecommerceImagesQuotaMb = value
      break
    case 'NOTEBOOK_IMAGES':
      config.notebookImagesQuotaMb = value
      break
    case 'NOTEBOOK_CONTENT':
      config.notebookContentQuotaMb = value
      break
    case 'IMPORT_FILES':
      config.importFilesQuotaMb = value
      break
    default:
      break
  }
}

function zoneStrategyField(key: string, config: StorageCenterConfig): StorageOverLimitStrategy {
  switch (key) {
    case 'ECOMMERCE_IMAGES':
      return config.ecommerceImagesOverLimitStrategy
    case 'NOTEBOOK_IMAGES':
      return config.notebookImagesOverLimitStrategy
    case 'NOTEBOOK_CONTENT':
      return config.notebookContentOverLimitStrategy
    case 'IMPORT_FILES':
      return config.importFilesOverLimitStrategy
    default:
      return config.overLimitStrategy
  }
}

function setZoneStrategyField(
  key: string,
  config: StorageCenterConfig,
  value: StorageOverLimitStrategy,
) {
  switch (key) {
    case 'ECOMMERCE_IMAGES':
      config.ecommerceImagesOverLimitStrategy = value
      break
    case 'NOTEBOOK_IMAGES':
      config.notebookImagesOverLimitStrategy = value
      break
    case 'NOTEBOOK_CONTENT':
      config.notebookContentOverLimitStrategy = value
      break
    case 'IMPORT_FILES':
      config.importFilesOverLimitStrategy = value
      break
    default:
      break
  }
}
</script>

<style scoped lang="scss">
.storage-config-card {
  --storage-config-green: #15803d;

  margin-top: 8px;
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: linear-gradient(180deg, #fafbfc 0%, #fff 100%);

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 14px;
  }

  &__head-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__title {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    color: #1f2937;
  }

  &__info {
    margin: 0;
  }

  &__columns {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px 20px;
  }

  &__column {
    display: flex;
    flex-direction: column;
    gap: 12px;
    min-width: 0;
  }

  &__row {
    min-width: 0;

    dt {
      margin: 0 0 6px;
      font-size: 12px;
      color: var(--wr-muted);
    }

    .storage-config-card__label {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }

    .storage-config-card__field-tip {
      font-size: 14px;
      color: #9ca3af;
      cursor: help;
      vertical-align: middle;

      &:hover {
        color: #6b7280;
      }
    }

    dd {
      margin: 0;
      font-size: 13px;
      font-weight: 600;
      color: #1f2937;
      word-break: break-word;
    }
  }

  &__strategy-group {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px 16px;
    font-weight: 400;

    :deep(.el-radio) {
      margin-right: 0;
      height: auto;
    }

    :deep(.el-radio__label) {
      font-size: 13px;
      font-weight: 500;
      color: #374151;
    }

    :deep(.el-radio.is-checked .el-radio__inner) {
      border-color: var(--storage-config-green);
      background: var(--storage-config-green);
    }

    :deep(.el-radio.is-checked .el-radio__label) {
      color: var(--storage-config-green);
      font-weight: 600;
    }
  }

  &__green-switch {
    :deep(.el-switch.is-checked .el-switch__core) {
      border-color: var(--storage-config-green);
      background-color: var(--storage-config-green);
    }
  }

  &__hint {
    display: block;
    margin-top: 4px;
    font-size: 12px;
    font-weight: 500;
    color: #6b7280;
  }

  &__edit-hint {
    margin: 14px 0 0;
    font-size: 12px;
    line-height: 1.5;
    color: #6b7280;
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-top: 16px;
    padding-top: 14px;
    border-top: 1px solid #eef2f7;
  }
}

@media (max-width: 1100px) {
  .storage-config-card__columns {
    grid-template-columns: 1fr;
  }
}
</style>
