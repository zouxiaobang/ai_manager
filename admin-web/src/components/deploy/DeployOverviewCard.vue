<template>
  <article class="deploy-overview-card" :class="`is-${node.tone}`">
    <div class="deploy-overview-card__icon" aria-hidden="true">
      <component :is="overviewIconMap[node.icon]" />
    </div>
    <div class="deploy-overview-card__body">
      <div class="deploy-overview-card__label">{{ node.title }}</div>
      <a
        v-if="node.key === 'url'"
        class="deploy-overview-card__value deploy-overview-card__value--link"
        :href="node.value"
        target="_blank"
        rel="noopener noreferrer"
      >
        {{ node.value }}
      </a>
      <div v-else class="deploy-overview-card__value">{{ node.value }}</div>
      <div class="deploy-overview-card__sub">{{ node.subtitle }}</div>
      <div v-if="node.tags.length" class="deploy-overview-card__tags">
        <span
          class="deploy-overview-card__tag"
          :class="{ 'is-success': node.tagTone === 'success' }"
        >
          <el-icon v-if="node.tagTone === 'success'" class="deploy-overview-card__tag-icon">
            <CircleCheck />
          </el-icon>
          {{ node.tags.join(' · ') }}
        </span>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { CircleCheck } from '@element-plus/icons-vue'
import DeployOverviewIconDatabase from '@/components/deploy/icons/DeployOverviewIconDatabase.vue'
import DeployOverviewIconGlobe from '@/components/deploy/icons/DeployOverviewIconGlobe.vue'
import DeployOverviewIconLaptop from '@/components/deploy/icons/DeployOverviewIconLaptop.vue'
import DeployOverviewIconServer from '@/components/deploy/icons/DeployOverviewIconServer.vue'
import type { DeployNodeCard } from '@/data/deploy-center'

defineProps<{
  node: DeployNodeCard
}>()

const overviewIconMap: Record<string, Component> = {
  server: DeployOverviewIconServer,
  database: DeployOverviewIconDatabase,
  laptop: DeployOverviewIconLaptop,
  globe: DeployOverviewIconGlobe,
}
</script>

<style scoped lang="scss">
.deploy-overview-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  width: 100%;
  height: 100%;
  padding: 18px 16px 16px;
  border-radius: 14px;
  border: 1px solid transparent;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);

  &__icon {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 52px;
    height: 52px;
    border-radius: 14px;
    color: #fff;

    svg {
      width: 28px;
      height: 28px;
    }
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__label {
    font-size: 15px;
    font-weight: 700;
    color: #111827;
    margin-bottom: 6px;
    line-height: 1.3;
  }

  &__value {
    font-size: 30px;
    font-weight: 700;
    line-height: 1.1;
    color: #111827;
    word-break: break-all;

    &--link {
      display: inline-block;
      font-size: 14px;
      font-weight: 600;
      line-height: 1.45;
      color: #2563eb;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  &__sub {
    margin-top: 6px;
    font-size: 12px;
    color: #6b7280;
  }

  &__tags {
    margin-top: 12px;
  }

  &__tag {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 600;
    line-height: 1.4;

    &.is-success {
      background: #ecfdf5;
      color: #16a34a;
    }
  }

  &__tag-icon {
    font-size: 14px;
  }

  &.is-blue {
    background: #eff6ff;
    border-color: #bfdbfe;

    .deploy-overview-card__icon {
      background: #3b82f6;
    }

    .deploy-overview-card__value {
      color: #2563eb;
    }

    .deploy-overview-card__tag:not(.is-success) {
      background: #dbeafe;
      color: #1d4ed8;
    }
  }

  &.is-green {
    background: #f0fdf4;
    border-color: #bbf7d0;

    .deploy-overview-card__icon {
      background: #22c55e;
    }

    .deploy-overview-card__value {
      color: #16a34a;
    }

    .deploy-overview-card__tag:not(.is-success) {
      background: #dcfce7;
      color: #15803d;
    }
  }

  &.is-purple {
    background: #f5f3ff;
    border-color: #ddd6fe;

    .deploy-overview-card__icon {
      background: #8b5cf6;
    }

    .deploy-overview-card__value {
      color: #7c3aed;
    }

    .deploy-overview-card__tag:not(.is-success) {
      background: #ede9fe;
      color: #6d28d9;
    }
  }

  &.is-orange {
    background: #fffbeb;
    border-color: #fde68a;

    .deploy-overview-card__icon {
      background: #f59e0b;
    }

    .deploy-overview-card__value--link {
      color: #2563eb;
    }
  }
}
</style>
