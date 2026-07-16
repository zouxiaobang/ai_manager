<template>
  <div class="mobile-v2-page" :class="{ 'mobile-v2-page--flush': flush }">
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  flush?: boolean
}>()
</script>
