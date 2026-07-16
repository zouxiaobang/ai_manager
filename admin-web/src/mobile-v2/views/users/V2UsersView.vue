<template>
  <V2Page>
    <V2Card>
      <MobileUsersView />
    </V2Card>
  </V2Page>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import V2Page from '@/mobile-v2/components/V2Page.vue'
import V2Card from '@/mobile-v2/components/V2Card.vue'
import MobileUsersView from '@/mobile/views/users/MobileUsersView.vue'

const { t: _t } = useI18n()
</script>

<style scoped lang="scss">
</style>
