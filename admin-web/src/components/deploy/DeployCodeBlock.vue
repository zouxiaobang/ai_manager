<template>
  <div class="deploy-code-block">
    <div v-if="title || description" class="deploy-code-block__head">
      <h3 v-if="title" class="deploy-code-block__title">{{ title }}</h3>
      <p v-if="description" class="deploy-code-block__desc">{{ description }}</p>
    </div>
    <div class="deploy-code-block__body">
      <pre class="deploy-code-block__pre"><code><span
        v-for="(line, lineIndex) in highlightedLines"
        :key="lineIndex"
        class="deploy-code-block__line"
      ><span
        v-for="(token, tokenIndex) in line"
        :key="tokenIndex"
        :class="tokenClass(token.kind)"
      >{{ token.text }}</span></span></code></pre>
      <button type="button" class="deploy-code-block__copy" :title="copyLabel" @click="copy">
        <el-icon><DocumentCopy /></el-icon>
      </button>
    </div>
    <p v-if="hint" class="deploy-code-block__hint">
      <el-icon><InfoFilled /></el-icon>
      {{ hint }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { DocumentCopy, InfoFilled } from '@element-plus/icons-vue'
import {
  highlightDeployCode,
  type DeployCodeTokenKind,
} from '@/utils/deployCodeHighlight'

const props = defineProps<{
  title?: string
  description?: string
  commands: string[]
  hint?: string
}>()

const { t } = useI18n()

const commandText = computed(() => props.commands.join('\n'))
const highlightedLines = computed(() => highlightDeployCode(commandText.value))
const copyLabel = computed(() => t('deployCenter.copy'))

function tokenClass(kind: DeployCodeTokenKind): string {
  return `deploy-code-block__token deploy-code-block__token--${kind}`
}

async function copy() {
  try {
    await navigator.clipboard.writeText(commandText.value)
    ElMessage.success(t('deployCenter.copied'))
  } catch {
    ElMessage.error(t('deployCenter.copyFailed'))
  }
}
</script>

<style scoped lang="scss">
.deploy-code-block {
  &__head {
    margin-bottom: 12px;
  }

  &__title {
    margin: 0 0 6px;
    font-size: 15px;
    font-weight: 600;
    color: #1f2937;
  }

  &__desc {
    margin: 0;
    font-size: 13px;
    color: #6b7280;
    line-height: 1.6;
  }

  &__body {
    position: relative;
    border-radius: 12px;
    background: #1e293b;
    overflow: auto;
    max-height: 320px;
    box-shadow: inset 0 1px 0 rgb(255 255 255 / 4%);
  }

  &__pre {
    margin: 0;
    padding: 16px 48px 16px 18px;
    overflow: visible;
    font-family: 'Cascadia Code', 'Consolas', 'Monaco', monospace;
    font-size: 13px;
    line-height: 1.55;
    color: #e2e8f0;
    white-space: normal;
    tab-size: 2;
  }

  &__line {
    display: block;
    min-height: 1.55em;
    white-space: pre-wrap;
    word-break: break-all;
  }

  &__token {
    &--plain {
      color: #e2e8f0;
    }

    &--comment {
      color: #64748b;
      font-style: italic;
    }

    &--command {
      color: #67e8f9;
      font-weight: 600;
    }

    &--flag {
      color: #93c5fd;
    }

    &--url {
      color: #e2e8f0;
      text-decoration: underline;
      text-decoration-color: rgb(148 163 184 / 55%);
      text-underline-offset: 2px;
    }

    &--pipe {
      color: #cbd5e1;
    }

    &--json-string {
      color: #86efac;
    }

    &--json-number {
      color: #86efac;
    }
  }

  &__copy {
    position: absolute;
    top: 10px;
    right: 10px;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border: none;
    border-radius: 8px;
    background: rgb(255 255 255 / 8%);
    color: #94a3b8;
    cursor: pointer;
    transition: background 0.15s, color 0.15s;

    &:hover {
      background: rgb(255 255 255 / 14%);
      color: #f8fafc;
    }
  }

  &__hint {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    margin: 10px 0 0;
    font-size: 12px;
    color: #6b7280;
    line-height: 1.5;

    .el-icon {
      margin-top: 2px;
      color: #3b82f6;
    }
  }
}
</style>
