<template>

  <WarRoomSvgIcon group="nav" :name="name" :size="26" class="wr-nav-icon" />

</template>



<script setup lang="ts">

import WarRoomSvgIcon from '@/components/war-room/WarRoomSvgIcon.vue'



export type WarRoomNavIconName =
  | 'home'
  | 'pomodoro'
  | 'ecommerce'
  | 'pixel-dog'
  | 'functions'
  | 'notebook'
  | 'todos'
  | 'deploy-docs'
  | 'storage'
  | 'image-space'



defineProps<{

  name: WarRoomNavIconName

}>()

</script>



<style scoped>

.wr-nav-icon {

  display: block;

}

</style>


