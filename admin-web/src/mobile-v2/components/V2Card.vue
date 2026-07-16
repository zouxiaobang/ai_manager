<template>
  <section class="mobile-v2-card" :class="{ 'mobile-v2-card--flush': flush }">
    <slot />
  </section>
</template>

<script setup lang="ts">
defineProps<{
  flush?: boolean
}>()
</script>
