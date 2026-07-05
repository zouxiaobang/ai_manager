<template>
  <section class="mec-section">
    <div class="mec-section__head">
      <img class="mec-section__icon" :src="schemeAAssets.starBlue" alt="" />
      <h2 class="mec-section__title">{{ ec.t('mobile.ecommerce.pendingOrders') }}</h2>
      <button type="button" class="mec-section__link" @click="ec.openModule('order')">
        {{ ec.t('mobile.ecommerce.viewAll') }} ›
      </button>
    </div>

    <SchemeADoodleFrame class="mec-orders-card" color="#2563eb" sketch :stroke-width="3" :shadow="false">
      <img class="mec-orders-card__clip" :src="schemeAAssets.paperclip" alt="" />

      <div v-if="ec.displayPendingOrders.value.length" class="mec-orders-list">
        <div
          v-for="order in ec.displayPendingOrders.value"
          :key="order.id"
          class="mec-orders-row"
        >
          <MobileDoodleChip shape="pill" color="#f97316" :seed="order.id" class="mec-orders-row__dot" />
          <button
            type="button"
            class="mec-orders-row__body"
            @click="ec.openModule(order._isWarning ? 'inventory' : 'order')"
          >
            <span class="mec-orders-row__title">{{ ec.orderTitle(order) }}</span>
            <span class="mec-orders-row__meta">{{ ec.orderMeta(order) }}</span>
          </button>
          <MobileDoodleChip shape="pill" color="#e63946" :seed="order.id + 9" class="mec-orders-row__tag">
            {{ ec.orderTag(order) }}
          </MobileDoodleChip>
        </div>
      </div>
      <div v-else class="mec-empty">{{ ec.t('mobile.ecommerce.pendingEmpty') }}</div>

      <img class="mec-orders-card__squiggle" :src="schemeAAssets.squiggleRed" alt="" />
    </SchemeADoodleFrame>
  </section>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_ECOMMERCE_KEY } from '../mobileEcommerceContext'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'

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
  flex-shrink: 0;
}

.mec-section__title {
  margin: 0;
  flex: 1;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.mec-section__link {
  padding: 0;
  border: none;
  background: transparent;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.mec-orders-card {
  position: relative;

  :deep(.sa-doodle-frame__body) {
    padding: 14px 26px;
  }
}

.mec-orders-card__clip {
  position: absolute;
  top: -6px;
  right: 14px;
  width: 22px;
  height: 32px;
  pointer-events: none;
}

.mec-orders-card__squiggle {
  position: absolute;
  right: 12px;
  bottom: 10px;
  width: 32px;
  height: 20px;
  pointer-events: none;
}

.mec-orders-list {
  display: flex;
  flex-direction: column;
}

.mec-orders-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 2px dashed #e2e8f0;

  &:last-child {
    border-bottom: none;
    padding-bottom: 4px;
  }
}

.mec-orders-row__dot {
  flex-shrink: 0;
  width: 18px;
  height: 18px;

  :deep(.sa-doodle-frame) {
    background: #fff7ed;
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
    width: 18px;
    height: 18px;
  }
}

.mec-orders-row__body {
  flex: 1;
  min-width: 0;
  padding: 0;
  border: none;
  background: transparent;
  text-align: left;
  font-family: inherit;
}

.mec-orders-row__title {
  display: block;
  font-size: 15px;
  line-height: 1.4;
  font-weight: 700;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mec-orders-row__meta {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  font-weight: 700;
  color: #94a3b8;
}

.mec-orders-row__tag {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 800;
  color: #e63946;

  :deep(.sa-doodle-frame__body) {
    padding: 2px 8px;
  }
}

.mec-empty {
  padding: 12px 0;
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
}
</style>
