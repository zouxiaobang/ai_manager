<template>
  <div class="carton-grid">
    <SchemeADoodleFrame
      v-for="carton in cartons"
      :key="carton.id"
      tag="button"
      type="button"
      class="carton-grid-card"
      :color="getCardColor(carton)"
      :seed="carton.id"
      sketch
      :stroke-width="3.5"
      :shadow="false"
      @click="$emit('select', carton)"
    >
      <div class="carton-grid-card__inner">
        <div class="carton-grid-card__image-wrap">
          <img :src="carton.image" class="carton-grid-card__image" :alt="carton.name" />
        </div>
        <div class="carton-grid-card__info">
          <div class="carton-grid-card__name">{{ carton.name }}</div>
          <div class="carton-grid-card__spec">{{ carton.spec }}</div>
          <div class="carton-grid-card__footer">
            <span class="carton-grid-card__price">{{ carton.unitPrice }}</span>
            <span class="carton-grid-card__factory">{{ carton.factoryName }}</span>
          </div>
        </div>
      </div>
    </SchemeADoodleFrame>

    <div v-if="cartons.length === 0" class="carton-grid__empty">
      <img :src="schemeAAssets.starBlueOutline" class="carton-grid__empty-icon" alt="" />
      <p class="carton-grid__empty-text">暂无纸箱数据</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import SchemeADoodleFrame from '@/mobile/home/themes/scheme-a/SchemeADoodleFrame.vue'
import { schemeAAssets } from '@/mobile/home/themes/scheme-a/assets'
import type { MobileCartonItem } from '../useMobileCarton'

defineProps<{
  cartons: MobileCartonItem[]
}>()

defineEmits<{
  select: [carton: MobileCartonItem]
}>()

function getCardColor(carton: MobileCartonItem): string {
  if (carton.volume < 5000) return '#22c55e'
  if (carton.volume < 30000) return '#2563eb'
  return '#f59e0b'
}
</script>

<style scoped lang="scss">
.carton-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.carton-grid-card {
  cursor: pointer;
  appearance: none;
  outline: none;
  transition: transform 0.2s ease;

  &:active {
    transform: scale(0.95);
  }

  :deep(.sa-doodle-frame--rect) {
    border-radius: 0;
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.carton-grid-card__inner {
  padding: 12px;
}

.carton-grid-card__image-wrap {
  width: 100%;
  height: 90px;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  margin-bottom: 8px;
}

.carton-grid-card__image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.carton-grid-card__info {
  text-align: center;
}

.carton-grid-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.carton-grid-card__spec {
  font-size: 15px;
  color: #64748b;
  margin-bottom: 6px;
}

.carton-grid-card__footer {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.carton-grid-card__price {
  font-size: 17px;
  font-weight: 900;
  color: #c41e3a;
}

.carton-grid-card__factory {
  font-size: 10px;
  color: #94a3b8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.carton-grid__empty {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  gap: 12px;
}

.carton-grid__empty-icon {
  width: 48px;
  height: 48px;
  opacity: 0.5;
}

.carton-grid__empty-text {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
}
</style>