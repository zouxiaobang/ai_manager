<template>
  <span class="wr-stat-icon" :class="`wr-stat-icon--${tone}`">
    <WarRoomSvgIcon group="stats" :name="name" :size="18" />
  </span>
</template>

<script setup lang="ts">
import WarRoomSvgIcon from '@/components/war-room/WarRoomSvgIcon.vue'

defineProps<{
  name: 'target' | 'pulse' | 'alert' | 'cube' | 'checklist' | 'folder'
  tone: 'blue' | 'orange' | 'purple' | 'green' | 'gray'
}>()
</script>
