<template>
  <section class="mec-section">
    <div class="mec-overview-grid">
      <SchemeADoodleFrame
        v-for="card in ec.overviewCards.value"
        :key="card.key"
        tag="button"
        type="button"
        class="mec-overview-card"
        :color="card.color"
        :seed="doodleSeedFromKey(card.key)"
        sketch
        :stroke-width="3"
        :shadow="false"
        @click="ec.openModule(card.module)"
      >
          <div class="mec-overview-card__inner">
            <div
              v-if="card.kind === 'ring'"
              class="mec-overview-card__ring"
              :style="{ '--progress': ringProgress }"
            >
              <span class="mec-overview-card__ring-text">{{ ec.pendingShipCount.value }}</span>
            </div>
            <img
              v-else
              class="mec-overview-card__icon"
              :src="card.icon"
              alt=""
            />
            <div class="mec-overview-card__body">
              <div
                class="mec-overview-card__value"
                :class="{
                  'is-green': card.key === 'ship',
                  'is-blue': card.key === 'revenue',
                  'is-orange': card.key === 'settlement',
                }"
              >
                {{ card.value }}
              </div>
              <div
                class="mec-overview-card__label"
                :class="{ 'is-link': card.labelLink }"
              >
                {{ card.label }}<span v-if="card.labelLink"> &gt;</span>
              </div>
            </div>
          </div>
        </SchemeADoodleFrame>
      </div>
  </section>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { MOBILE_ECOMMERCE_KEY } from '../mobileEcommerceContext'

import { doodleSeedFromKey } from '@/mobile/utils/doodleSeed'
import SchemeADoodleFrame from '@/mobile/home/themes/scheme-a/SchemeADoodleFrame.vue'

const ec = inject(MOBILE_ECOMMERCE_KEY)!

const ringProgress = computed(() => {
  const count = ec.pendingShipCount.value
  return Math.min(count / 10, 1)
})
</script>

<style scoped lang="scss">
.mec-overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.mec-overview-card {
  padding: 0;
  text-align: left;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.mec-overview-card__inner {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 76px;
  padding: 10px 12px;
}

.mec-overview-card__ring {
  --progress: 0;
  position: relative;
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: conic-gradient(#16a34a calc(var(--progress) * 360deg), #dcfce7 0);
  box-shadow: inset 0 0 0 2px #86efac;

  &::after {
    content: '';
    position: absolute;
    inset: 5px;
    border-radius: 50%;
    background: #f0fdf4;
  }
}

.mec-overview-card__ring-text {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 700;
  color: #15803d;
}

.mec-overview-card__icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  object-fit: contain;
}

.mec-overview-card__body {
  flex: 1;
  min-width: 0;
}

.mec-overview-card__value {
  font-size: 18px;
  line-height: 1.15;
  font-weight: 700;
  color: #1e293b;

  &.is-green {
    color: #16a34a;
  }

  &.is-blue {
    color: #2563eb;
  }

  &.is-orange {
    color: #ea580c;
  }
}

.mec-overview-card__label {
  margin-top: 4px;
  font-size: 12px;
  color: #475569;

  &.is-link {
    color: #ea580c;
  }
}
</style>
