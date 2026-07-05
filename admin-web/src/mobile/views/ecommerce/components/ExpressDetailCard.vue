<template>
  <SchemeADoodleFrame
    tag="button"
    type="button"
    shape="rect"
    :color="station.isDefault ? '#fbbf24' : '#2563eb'"
    class="express-detail-card"
    @click="$emit('click', station)"
  >
    <div class="express-detail-card__header">
      <span class="express-detail-card__icon">{{ getExpressIcon(station.name) }}</span>
      <div class="express-detail-card__info">
        <div class="express-detail-card__name">
          {{ station.name }}
          <span v-if="station.isDefault" class="default-badge">⭐</span>
        </div>
        <div class="express-detail-card__contact">
          📞 {{ station.contact || '-' }} · {{ station.address || '-' }}
        </div>
      </div>
    </div>
    <div class="express-detail-card__divider"></div>
    <div class="express-detail-card__details">
      <span class="express-detail-card__tag price-tag">
        🏷️ 面单费 ¥{{ formatPrice(station.labelPrice) }}
      </span>
      <span class="express-detail-card__tag province-tag">
        📍 覆盖{{ station.priceCount || 0 }}省份
      </span>
      <span v-if="station.isDefault" class="express-detail-card__tag default-tag">
        ⭐ 默认
      </span>
      <span v-if="station.noticeCount && station.noticeCount > 0" class="express-detail-card__tag notice-tag">
        ⚠️ {{ station.noticeCount }}条须知
      </span>
    </div>
    <div class="express-detail-card__actions">
      <button class="action-btn action-btn--delete" @click.stop="$emit('delete', station)">
        删除
      </button>
    </div>
  </SchemeADoodleFrame>
</template>

<script setup lang="ts">
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import type { EcExpressStation } from '@/api/ecommerce/express'

defineProps<{
  station: EcExpressStation
}>()

defineEmits<{
  click: [station: EcExpressStation]
  delete: [station: EcExpressStation]
}>()

function getExpressIcon(name: string): string {
  const nameLower = name.toLowerCase()
  if (nameLower.includes('顺丰')) return '💎'
  if (nameLower.includes('中通')) return '🟢'
  if (nameLower.includes('圆通')) return '🔴'
  if (nameLower.includes('申通')) return '🟡'
  if (nameLower.includes('韵达')) return '🔵'
  if (nameLower.includes('京东')) return '🔷'
  if (nameLower.includes('极兔')) return '🐰'
  if (nameLower.includes('德邦')) return '📦'
  if (nameLower.includes('ems')) return '✉️'
  if (nameLower.includes('百世')) return '🌐'
  return '📦'
}

function formatPrice(price?: number | null): string {
  if (price == null) return '0.00'
  return price.toFixed(2)
}
</script>

<style scoped lang="scss">
.express-detail-card {
  overflow: hidden;
  position: relative;

  :deep(.sa-doodle-frame) {
    overflow: hidden;
    position: relative;
  }

  :deep(.sa-doodle-frame__stroke) {
    inset: 0 !important;
    width: 100% !important;
    height: 100% !important;
    overflow: hidden;
  }

  :deep(.sa-doodle-frame__body) {
    padding: 24px;
    background: #fff;
  }

  &__header {
    display: flex;
    gap: 12px;
    margin-bottom: 12px;
  }

  &__icon {
    font-size: 28px;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 16px;
    font-weight: 800;
    color: #1e293b;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .default-badge {
    font-size: 14px;
  }

  &__contact {
    font-size: 12px;
    color: #64748b;
    margin-top: 3px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__divider {
    height: 2px;
    background: #e2e8f0;
    border-radius: 1px;
    margin-bottom: 12px;
  }

  &__details {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;
  }

  &__tag {
    font-size: 11px;
    font-weight: 600;
    color: #64748b;
    background: #f1f5f9;
    padding: 3px 8px;
    border-radius: 6px;

    &.default-tag {
      color: #fbbf24;
      background: #fffbeb;
    }

    &.notice-tag {
      color: #ef4444;
      background: #fef2f2;
    }

    &.price-tag {
      color: #f97316;
      background: #fff7ed;
    }

    &.province-tag {
      color: #16a34a;
      background: #dcfce7;
    }
  }

  &__actions {
    display: flex;
    justify-content: flex-end;
  }

  &:active {
    transform: scale(0.98);
  }
}

.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.15s;

  &:active {
    transform: scale(0.95);
  }

  &--delete {
    background: #fee2e2;
    color: #ef4444;
  }
}
</style>
