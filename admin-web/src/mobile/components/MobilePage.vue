<template>
  <div class="mobile-page" :class="{ 'mobile-page--flush': flush }">
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  flush?: boolean
}>()
</script>
