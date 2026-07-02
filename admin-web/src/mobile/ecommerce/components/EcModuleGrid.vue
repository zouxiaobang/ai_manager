<template>
  <section class="mec-section">
    <div class="mec-section__head">
      <img class="mec-section__icon" :src="schemeAAssets.starBlueOutline" alt="" />
      <h2 class="mec-section__title">{{ ec.t('mobile.home.allFunctions') }}</h2>
    </div>

    <div v-if="ec.filteredModules.value.length" class="mec-module-grid">
      <SchemeADoodleFrame
        v-for="item in ec.filteredModules.value"
        :key="item.key"
        tag="button"
        type="button"
        class="mec-module"
        :color="item.borderColor"
        :seed="doodleSeedFromKey(item.key)"
        sketch
        :stroke-width="3"
        :shadow="false"
        @click="ec.openModule(item)"
      >
        <div class="mec-module__inner">
          <img class="mec-module__icon" :src="item.icon" :alt="ec.t(item.labelKey)" />
          <span class="mec-module__text">
            <span class="mec-module__name">{{ ec.t(item.labelKey) }}</span>
            <span
              class="mec-module__squiggle"
              :style="{ '--mec-squiggle': item.squiggleColor }"
            />
          </span>
          <MobileDoodleChip
            v-if="item.key === 'order' && ec.pendingShipCount.value > 0"
            shape="pill"
            color="#f59e0b"
            :seed="7"
            class="mec-module__badge"
          >
            {{ ec.pendingShipCount.value > 99 ? '99+' : ec.pendingShipCount.value }}
          </MobileDoodleChip>
        </div>
      </SchemeADoodleFrame>
    </div>
    <div v-else class="mec-empty">{{ ec.t('mobile.home.searchEmpty') }}</div>
  </section>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_ECOMMERCE_KEY } from '../mobileEcommerceContext'
import { schemeAAssets } from '@/mobile/home/themes/scheme-a/assets'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import { doodleSeedFromKey } from '@/mobile/utils/doodleSeed'
import SchemeADoodleFrame from '@/mobile/home/themes/scheme-a/SchemeADoodleFrame.vue'

const ec = inject(MOBILE_ECOMMERCE_KEY)!
</script>

<style scoped lang="scss">
.mec-section__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.mec-section__icon {
  width: 22px;
  height: 22px;
}

.mec-section__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.mec-module-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.mec-module {
  padding: 0;
  text-align: left;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.mec-module__inner {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 64px;
  padding: 10px 12px;
}

.mec-module__icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  object-fit: contain;
}

.mec-module__text {
  flex: 1;
  min-width: 0;
}

.mec-module__name {
  display: block;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.mec-module__squiggle {
  display: block;
  width: 36px;
  height: 6px;
  margin-top: 4px;
  border-radius: 3px;
  background: var(--mec-squiggle, #2563eb);
  transform: rotate(-2deg);
  opacity: 0.85;
}

.mec-module__badge {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 22px;
  font-size: 11px;
  font-weight: 800;
  color: #78350f;
  text-align: center;

  :deep(.sa-doodle-frame) {
    background: #fbbf24;
  }

  :deep(.sa-doodle-frame__body) {
    padding: 2px 6px;
  }
}

.mec-empty {
  padding: 16px;
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
}
</style>
