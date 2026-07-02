<template>
  <SchemeADoodleFrame class="mec-poster" color="#2563eb">
    <span class="mec-poster__online">{{ ec.t('mobile.home.onlineBadge') }}</span>
    <h2 class="mec-poster__title">{{ ec.t('ecommerce.workbenchTitle') }}</h2>
    <p class="mec-poster__subtitle">{{ ec.t('functions.items.ecommerce.desc') }}</p>
    <div class="mec-poster__illus-wrap">
      <img class="mec-poster__illus" :src="schemeAAssets.pixelDog" alt="" />
    </div>
    <div class="mec-poster__pill">{{ ec.summaryPillText.value }}</div>
  </SchemeADoodleFrame>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_ECOMMERCE_KEY } from '../mobileEcommerceContext'
import { schemeAAssets } from '@/mobile/home/themes/scheme-a/assets'
import SchemeADoodleFrame from '@/mobile/home/themes/scheme-a/SchemeADoodleFrame.vue'

const ec = inject(MOBILE_ECOMMERCE_KEY)!
</script>

<style scoped lang="scss">
.mec-poster {
  position: relative;
  padding-bottom: 4px;

  :deep(.sa-doodle-frame__body) {
    padding: 14px 14px 16px;
    text-align: center;
  }
}

.mec-poster__online {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  padding: 4px 10px;
  border-radius: 8px;
  background: #e63946;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}

.mec-poster__title {
  margin: 0;
  font-size: 28px;
  font-weight: 400;
  color: #2563eb;
  letter-spacing: 0.02em;
}

.mec-poster__subtitle {
  margin: 6px 0 12px;
  font-size: 13px;
  color: #64748b;
  font-weight: 600;
}

.mec-poster__illus-wrap {
  display: inline-flex;
  margin: 0 auto 12px;
  padding: 12px;
  border-radius: 14px;
  border: 2.5px dashed #2563eb;
  background: #fff;
}

.mec-poster__illus {
  display: block;
  width: 100%;
  max-width: 120px;
  height: auto;
}

.mec-poster__pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: #fbbf24;
  border: 2px solid #f59e0b;
  font-size: 13px;
  font-weight: 800;
  color: #78350f;
  box-shadow: 0 3px 10px rgb(251 191 36 / 35%);
}
</style>
