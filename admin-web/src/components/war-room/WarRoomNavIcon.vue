<template>

  <WarRoomSvgIcon group="nav" :name="name" :size="26" class="wr-nav-icon" />

</template>



<script setup lang="ts">

import WarRoomSvgIcon from '@/components/war-room/WarRoomSvgIcon.vue'



export type WarRoomNavIconName =

  | 'home'

  | 'functions'

  | 'notebook'

  | 'todos'

  | 'user-center'

  | 'permission'

  | 'deploy-docs'

  | 'storage'



defineProps<{

  name: WarRoomNavIconName

}>()

</script>



<style scoped>

.wr-nav-icon {

  display: block;

}

</style>


