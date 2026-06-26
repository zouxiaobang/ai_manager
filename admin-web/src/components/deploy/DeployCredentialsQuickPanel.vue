<template>
  <div class="deploy-credentials-panel">
    <p class="deploy-credentials-panel__desc">{{ t('deployCenter.credentialsDesc') }}</p>
    <div class="deploy-cred-groups">
      <section
        v-for="group in deployCredentialGroups"
        :key="group.key"
        class="deploy-cred-group"
        :class="`is-${group.key}`"
      >
        <h3 class="deploy-cred-group__title">
          {{ t(`deployCenter.credentialGroups.${group.key}`) }}
        </h3>
        <div class="deploy-cred-group__rows">
          <div
            v-for="row in group.fields"
            :key="`${group.key}-${row.label}`"
            class="deploy-cred-group__row"
          >
            <span class="deploy-cred-group__label">{{ row.label }}</span>
            <code class="deploy-cred-group__value">{{ row.value }}</code>
            <button
              v-if="row.copyable !== false"
              type="button"
              class="deploy-cred-group__copy"
              @click="copy(row.value)"
            >
              {{ t('deployCenter.copy') }}
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { deployCredentialGroups } from '@/data/deploy-center'

const { t } = useI18n()

async function copy(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(t('deployCenter.copied'))
  } catch {
    ElMessage.error(t('deployCenter.copyFailed'))
  }
}
</script>

<style scoped lang="scss">
.deploy-credentials-panel {
  &__desc {
    margin: 0 0 16px;
    font-size: 13px;
    color: #6b7280;
    line-height: 1.6;
  }
}

.deploy-cred-groups {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 4px;
}

.deploy-cred-group {
  flex-shrink: 0;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;

  &__title {
    margin: 0;
    padding: 10px 14px;
    font-size: 13px;
    font-weight: 700;
    line-height: 1.4;
    border-bottom: 1px solid #e5e7eb;
  }

  &__rows {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 10px 12px 12px;
  }

  &__row {
    display: grid;
    grid-template-columns: 148px 1fr auto;
    gap: 10px;
    align-items: center;
    padding: 8px 10px;
    border-radius: 8px;
    background: #f9fafb;
  }

  &__label {
    font-size: 13px;
    color: #374151;
    font-weight: 500;
  }

  &__value {
    font-family: 'Cascadia Code', 'Consolas', monospace;
    font-size: 12px;
    color: #111827;
    word-break: break-all;
    line-height: 1.45;
  }

  &__copy {
    padding: 4px 10px;
    border-radius: 8px;
    font-size: 12px;
    cursor: pointer;
    white-space: nowrap;
    border: 1px solid transparent;
    background: #eff6ff;
    border-color: #dbeafe;
    color: #2563eb;

    &:hover {
      background: #dbeafe;
    }
  }

  &.is-nodes &__title {
    color: #1d4ed8;
    background: #eff6ff;
    border-bottom-color: #bfdbfe;
  }

  &.is-ssh &__title {
    color: #6d28d9;
    background: #f5f3ff;
    border-bottom-color: #ddd6fe;
  }

  &.is-mysql &__title {
    color: #15803d;
    background: #f0fdf4;
    border-bottom-color: #bbf7d0;
  }

  &.is-mysql &__copy {
    background: #f0fdf4;
    border-color: #bbf7d0;
    color: #15803d;

    &:hover {
      background: #dcfce7;
    }
  }

  &.is-app &__title {
    color: #b45309;
    background: #fffbeb;
    border-bottom-color: #fde68a;
  }

  &.is-app &__copy {
    background: #fffbeb;
    border-color: #fde68a;
    color: #b45309;

    &:hover {
      background: #fef3c7;
    }
  }
}

@media (max-width: 700px) {
  .deploy-cred-group__row {
    grid-template-columns: 1fr;
  }
}
</style>
