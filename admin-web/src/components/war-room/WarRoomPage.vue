<template>
  <div class="war-room-page" :class="{ 'war-room-page--fill': fill }">
    <header v-if="title || $slots.meta" class="war-room-page__header">
      <div>
        <h1 v-if="title" class="war-room-page__title">{{ title }}</h1>
        <p v-if="subtitle" class="war-room-page__subtitle">{{ subtitle }}</p>
      </div>
      <div v-if="$slots.meta" class="war-room-page__meta">
        <slot name="meta" />
      </div>
    </header>
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  subtitle?: string
  fill?: boolean
}>()
</script>

<style scoped lang="scss">
.war-room-page--fill {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding-bottom: 24px;
}
</style>
