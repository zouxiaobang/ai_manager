<template>
  <button type="button" class="wr-module-card" @click="$emit('click')">
    <img class="wr-module-card__icon" :src="iconUrl" :alt="name" />
    <span class="wr-module-card__name">{{ name }}</span>
    <span class="wr-module-card__bar" :style="{ background: barColor }" />
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { warRoomIconUrl } from '@/data/war-room-icons'

const props = defineProps<{
  icon: string
  name: string
  barColor: string
}>()

defineEmits<{ click: [] }>()

const iconUrl = computed(() => warRoomIconUrl('modules', props.icon))
</script>
