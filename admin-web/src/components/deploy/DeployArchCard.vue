<template>
  <article class="deploy-arch-card" :class="[`is-${step.tone}`, `is-${step.key}`]">
    <div
      class="deploy-arch-card__icons"
      :class="{ 'deploy-arch-card__icons--stacked': step.icons.length > 2 }"
      aria-hidden="true"
    >
      <component
        :is="archIconMap[icon]"
        v-for="icon in step.icons"
        :key="icon"
        class="deploy-arch-card__icon"
      />
    </div>
    <div class="deploy-arch-card__body">
      <div class="deploy-arch-card__label">{{ step.label }}</div>
      <p v-for="line in step.lines" :key="line" class="deploy-arch-card__line">{{ line }}</p>
      <a
        v-if="step.url"
        class="deploy-arch-card__link"
        :href="step.url"
        target="_blank"
        rel="noopener noreferrer"
      >
        {{ step.url }}
      </a>
    </div>
  </article>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import DeployArchIconAdmin from '@/components/deploy/icons/DeployArchIconAdmin.vue'
import DeployArchIconClient from '@/components/deploy/icons/DeployArchIconClient.vue'
import DeployArchIconDocker from '@/components/deploy/icons/DeployArchIconDocker.vue'
import DeployArchIconFolder from '@/components/deploy/icons/DeployArchIconFolder.vue'
import DeployArchIconMysql from '@/components/deploy/icons/DeployArchIconMysql.vue'
import DeployArchIconNginx from '@/components/deploy/icons/DeployArchIconNginx.vue'
import DeployArchIconRedis from '@/components/deploy/icons/DeployArchIconRedis.vue'
import type { DeployArchitectureNode } from '@/data/deploy-center'

defineProps<{
  step: DeployArchitectureNode
}>()

const archIconMap: Record<string, Component> = {
  client: DeployArchIconClient,
  nginx: DeployArchIconNginx,
  mysql: DeployArchIconMysql,
  redis: DeployArchIconRedis,
  docker: DeployArchIconDocker,
  folder: DeployArchIconFolder,
  admin: DeployArchIconAdmin,
}
</script>

<style scoped lang="scss">
.deploy-arch-card {
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 108px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 12px;
  border-radius: 12px;
  border: 1px solid transparent;

  &__icons {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    align-self: center;
    gap: 4px;

    &--stacked {
      display: grid;
      grid-template-columns: repeat(2, 32px);
      gap: 4px;
      justify-items: center;
      align-content: center;

      .deploy-arch-card__icon:nth-child(3) {
        grid-column: 1 / -1;
        justify-self: center;
      }
    }
  }

  &__icon {
    width: 40px;
    height: 40px;
    flex-shrink: 0;

    .deploy-arch-card__icons--stacked & {
      width: 32px;
      height: 32px;
    }

    :deep(svg) {
      width: 100%;
      height: 100%;
      display: block;
    }
  }

  &__body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  &__label {
    font-size: 15px;
    font-weight: 700;
    color: #111827;
    line-height: 1.3;
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__line {
    margin: 0;
    font-size: 12px;
    color: #6b7280;
    line-height: 1.45;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__link {
    display: -webkit-box;
    margin-top: 2px;
    font-size: 11px;
    font-weight: 600;
    color: #2563eb;
    text-decoration: none;
    word-break: break-all;
    line-height: 1.4;
    overflow: hidden;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    line-clamp: 2;

    &:hover {
      text-decoration: underline;
    }
  }

  &.is-gray {
    background: #f3f4f6;
    border-color: #d1d5db;
  }

  &.is-blue {
    background: #eff6ff;
    border-color: #bfdbfe;
  }

  &.is-green {
    background: #f0fdf4;
    border-color: #bbf7d0;
  }

  &.is-purple {
    background: #f5f3ff;
    border-color: #ddd6fe;
  }

  &.is-orange {
    background: #fffbeb;
    border-color: #fde68a;
  }
}
</style>
