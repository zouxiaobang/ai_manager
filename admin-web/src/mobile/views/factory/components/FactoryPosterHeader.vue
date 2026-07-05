<template>
  <div class="mfc-poster-banner-wrap">
    <div class="mfc-poster-banner" role="img" :aria-label="factory.t('ecommerce.factory.pageTitle')">
      <img
        class="mfc-poster-banner__img"
        :src="factoryAssets.posterHeader"
        alt=""
        decoding="async"
      />

      <!-- 遮住原图左上角「N家」绿框（v15 坐标） -->
      <div class="mfc-poster-banner__mask mfc-poster-banner__mask--badge" aria-hidden="true" />

      <div class="mfc-poster-banner__mask mfc-poster-banner__mask--pill" aria-hidden="true" />

      <MobileDoodleChip
        tag="button"
        type="button"
        shape="pill"
        color="#8b5cf6"
        class="mfc-poster-banner__back"
        @click="$router.back()"
      >
        <span>←</span>
      </MobileDoodleChip>

      <div class="mfc-poster-banner__pill-wrap">
        <MobileDoodleChip shape="pill" color="#d97706" :seed="42" :inline="false" class="mfc-poster-banner__pill" aria-live="polite">
          <span class="mfc-poster-banner__pill-label">{{ factory.t('ecommerce.factory.factoryTypeProduction') }}</span>
          <span class="mfc-poster-banner__pill-value">{{ factory.stats.production }}</span>
          <span class="mfc-poster-banner__pill-dot">·</span>
          <span class="mfc-poster-banner__pill-label">{{ factory.t('ecommerce.factory.factoryTypeCustomer') }}</span>
          <span class="mfc-poster-banner__pill-value">{{ factory.stats.customer }}</span>
          <span class="mfc-poster-banner__pill-dot">·</span>
          <span class="mfc-poster-banner__pill-label">{{ factory.t('mobile.factory.posterCartonLabel') }}</span>
          <span class="mfc-poster-banner__pill-value">{{ factory.stats.carton }}</span>
        </MobileDoodleChip>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_FACTORY_KEY } from '../factoryContext'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import { factoryAssets } from '../assets'

const factory = inject(MOBILE_FACTORY_KEY)!
</script>

<style scoped lang="scss">
.mfc-poster-banner-wrap {
  padding: env(safe-area-inset-top) 0 0;
}

.mfc-poster-banner {
  position: relative;
  width: 100%;
  aspect-ratio: 608 / 387;
  overflow: hidden;
  background: #fff;
}

.mfc-poster-banner__img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mfc-poster-banner__back {
  position: absolute;
  z-index: 3;
  left: 4.2%;
  top: 7.2%;
  width: 34px;
  height: 34px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #8b5cf6;
  font-weight: 700;
  cursor: pointer;
  background: #fff;

  :deep(.sa-doodle-frame__body) {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    padding: 0;
  }

  &:active {
    transform: scale(0.92);
  }
}

.mfc-poster-banner__mask {
  position: absolute;
  background: #fff;
  pointer-events: none;
  z-index: 1;

  &--badge {
    left: 3.6%;
    top: 6.6%;
    width: 25%;
    height: 18%;
    border-radius: 3px;
  }

  &--pill {
    left: 8.5%;
    bottom: 6.2%;
    width: 83%;
    height: 13%;
    border-radius: 999px;
  }
}

.mfc-poster-banner__pill-wrap {
  position: absolute;
  left: 8.5%;
  bottom: 6.2%;
  width: 83%;
  z-index: 2;
}

.mfc-poster-banner__pill {
  width: 100%;
  font-family: 'ZCOOL KuaiLe', 'Microsoft YaHei', sans-serif;
  font-size: clamp(11px, 3.1vw, 15px);
  font-weight: 700;
  color: #78350f;
  white-space: nowrap;

  :deep(.sa-doodle-frame) {
    background: #fde68a;
    box-shadow: 1px 2px 0 rgb(217 119 6 / 15%);
  }

  :deep(.sa-doodle-frame__body) {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.2em;
    padding: 6px 12px;
  }
}

.mfc-poster-banner__pill-label {
  font-weight: 700;
}

.mfc-poster-banner__pill-value {
  font-weight: 900;
  font-size: 1.08em;
  min-width: 1.2em;
  text-align: center;
}

.mfc-poster-banner__pill-dot {
  opacity: 0.65;
  margin: 0 0.1em;
}
</style>
