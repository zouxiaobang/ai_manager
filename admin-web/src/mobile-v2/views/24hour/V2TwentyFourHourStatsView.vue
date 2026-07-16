<template>
  <div class="v2-24h-stats">
    <div class="v2-24h-stats__header">
      <button class="v2-24h-stats__back" @click="goBack">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <h1 class="v2-24h-stats__title">{{ t('portal.dashboard.navStats') }}</h1>
    </div>
    <div class="v2-24h-stats__body">
      <V2StatsView :phases="phases" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import V2StatsView from './components/V2StatsView.vue'
import { phases } from '@/data/24hour-phases'

const router = useRouter()
const { t } = useI18n()

function goBack() {
  router.back()
}
</script>

<style scoped lang="scss">
.v2-24h-stats {
  min-height: 100vh;
  background: #f6f8fa;
  display: flex;
  flex-direction: column;
}

.v2-24h-stats__header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  position: sticky;
  top: 0;
  z-index: 10;
  background: #f6f8fa;
}

.v2-24h-stats__back {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1.5px solid #e2e8f0;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  color: #475569;
  transition: all 0.15s;

  &:active {
    opacity: 0.75;
  }
}

.v2-24h-stats__title {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.v2-24h-stats__body {
  flex: 1;
  padding: 0 16px 24px;
}
</style>
