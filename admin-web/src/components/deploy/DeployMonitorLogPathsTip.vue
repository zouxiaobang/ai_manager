<template>
  <div class="deploy-hover-tip">
    <div class="deploy-hover-tip__title">{{ t('deployCenter.monitorLogPathsTitle') }}</div>
    <div
      v-for="item in deployMonitorLogPaths"
      :key="item.path"
      class="deploy-hover-tip__row"
    >
      <div class="deploy-hover-tip__label">{{ item.label }}</div>
      <div class="deploy-hover-tip__value-row">
        <code class="deploy-hover-tip__value">{{ item.path }}</code>
        <button type="button" class="deploy-hover-tip__copy is-blue" @click="copy(item.path)">
          {{ t('deployCenter.copy') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { deployMonitorLogPaths } from '@/data/deploy-center'

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
.deploy-hover-tip {
  &__title {
    margin-bottom: 10px;
    font-size: 13px;
    font-weight: 700;
    color: #111827;
  }

  &__row {
    padding: 8px 0;
    border-bottom: 1px solid #f1f5f9;

    &:last-child {
      padding-bottom: 0;
      border-bottom: none;
    }

    &:first-of-type {
      padding-top: 0;
    }
  }

  &__label {
    margin-bottom: 4px;
    font-size: 12px;
    font-weight: 600;
    color: #374151;
  }

  &__value-row {
    display: flex;
    align-items: flex-start;
    gap: 8px;
  }

  &__value {
    flex: 1;
    min-width: 0;
    margin: 0;
    padding: 6px 8px;
    border-radius: 6px;
    background: #f8fafc;
    font-family: 'Cascadia Code', 'Consolas', monospace;
    font-size: 12px;
    color: #1f2937;
    word-break: break-all;
    line-height: 1.45;
  }

  &__copy {
    flex-shrink: 0;
    padding: 4px 10px;
    border-radius: 6px;
    font-size: 12px;
    cursor: pointer;
    white-space: nowrap;

    &.is-blue {
      border: 1px solid #dbeafe;
      background: #eff6ff;
      color: #2563eb;

      &:hover {
        background: #dbeafe;
      }
    }
  }
}
</style>
