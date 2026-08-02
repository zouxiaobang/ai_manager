<template>
  <section class="mobile-card" :class="{ 'mobile-card--flush': flush }">
    <slot />
  </section>
</template>

<script setup lang="ts">
defineProps<{
  flush?: boolean
}>()
</script>
