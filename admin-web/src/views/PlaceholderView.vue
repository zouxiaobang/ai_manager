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

const route = useRoute()
const { t } = useI18n()

const titleKeyMap: Record<string, string> = {
  '/user-center': 'portal.menu.userCenter',
  '/deploy-docs': 'portal.menu.deployCenter',
  '/storage': 'portal.menu.storage',
}

const pageTitle = computed(() => {
  const key = titleKeyMap[route.path]
  return key ? t(key) : t('portal.comingSoon')
})
</script>

<style scoped>
.war-room-panel--empty {
  padding: 48px 24px;
}
</style>
