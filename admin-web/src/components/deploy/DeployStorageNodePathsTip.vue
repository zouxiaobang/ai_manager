<template>
  <div class="deploy-hover-tip">
    <div class="deploy-hover-tip__title">{{ t('deployCenter.storageNodePathsTitle') }}</div>

    <section
      v-for="section in deployStorageNodePaths"
      :key="section.key"
      class="deploy-hover-tip__section"
      :class="`is-${section.key}`"
    >
      <h4 class="deploy-hover-tip__section-title">{{ section.title }}</h4>
      <div
        v-for="field in section.fields"
        :key="`${section.key}-${field.label}`"
        class="deploy-hover-tip__row"
      >
        <div class="deploy-hover-tip__label">{{ field.label }}</div>
        <div class="deploy-hover-tip__value-row">
          <code class="deploy-hover-tip__value">{{ field.value }}</code>
          <button
            v-if="field.copyable !== false"
            type="button"
            class="deploy-hover-tip__copy is-purple"
            @click="copy(field.value)"
          >
            {{ t('deployCenter.copy') }}
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { deployStorageNodePaths } from '@/data/deploy-center'

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

  &__section {
    padding-top: 10px;
    margin-top: 10px;
    border-top: 1px solid #e5e7eb;

    &:first-of-type {
      padding-top: 0;
      margin-top: 0;
      border-top: none;
    }
  }

  &__section-title {
    margin: 0 0 8px;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.02em;
  }

  &__section.is-local-server &__section-title {
    color: #2563eb;
  }

  &__section.is-local-dev &__section-title {
    color: #7c3aed;
  }

  &__section.is-baidu-pan &__section-title {
    color: #ea580c;
  }

  &__row {
    padding: 6px 0;

    &:last-child {
      padding-bottom: 0;
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

    &.is-purple {
      border: 1px solid #ddd6fe;
      background: #f5f3ff;
      color: #6d28d9;

      &:hover {
        background: #ede9fe;
      }
    }
  }
}
</style>
