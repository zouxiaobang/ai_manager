<template>
  <MobilePage>
    <MobileCard>
      <MobileUsersListView />
    </MobileCard>
  </MobilePage>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import MobilePage from '@/mobile/components/MobilePage.vue'
import MobileCard from '@/mobile/components/MobileCard.vue'
import MobileUsersListView from '@/mobile/views/users/MobileUsersListView.vue'

const { t: _t } = useI18n()
</script>

<style scoped lang="scss">
</style>
