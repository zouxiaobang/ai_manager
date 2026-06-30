<template>
  <WarRoomPage :title="pageTitle">
    <div class="war-room-panel war-room-panel--empty">
      <el-empty :description="t('portal.comingSoon')" />
    </div>
  </WarRoomPage>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
import { ECOMMERCE_NAV_ITEMS } from '@/data/ecommerce-nav'

const route = useRoute()
const { t } = useI18n()

const pageTitle = computed(() => {
  const item = ECOMMERCE_NAV_ITEMS.find((row) => row.path === route.path)
  return item ? t(item.labelKey) : t('portal.comingSoon')
})
</script>

<style scoped>
.war-room-panel--empty {
  padding: 48px 24px;
}
</style>
