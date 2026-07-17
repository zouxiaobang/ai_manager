<template>
  <V2Page>
    <V2Card>
      <V2UsersListView />
    </V2Card>
  </V2Page>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import V2Page from '@/mobile-v2/components/V2Page.vue'
import V2Card from '@/mobile-v2/components/V2Card.vue'
import V2UsersListView from '@/mobile-v2/views/users/V2UsersListView.vue'

const { t: _t } = useI18n()
</script>

<style scoped lang="scss">
</style>
