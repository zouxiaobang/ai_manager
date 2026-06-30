<template>
  <span v-if="parts" class="storage-mb-quota-text">
    <span>{{ parts.primary }}</span>
    <span class="storage-mb-quota-text__higher">{{ parts.secondary }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { splitStorageMbWithHigherUnit } from '@/api/storageCenter'

const props = defineProps<{
  mb: number
}>()

const parts = computed(() => splitStorageMbWithHigherUnit(props.mb))
</script>

<style scoped lang="scss">
.storage-mb-quota-text {
  &__higher {
    color: #ef4444;
    font-weight: 700;
  }
}
</style>
