<template>
  <V2Page>
    <div v-loading="carton.loading.value" class="v2-ec">
      <div class="v2-ec-search">
        <svg class="v2-ec-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input v-model="carton.searchQuery.value" class="v2-ec-search__input" placeholder="搜索纸箱..." type="search" />
      </div>

      <div class="v2-ec-tabs">
        <button
          v-for="cat in carton.categoryList.value"
          :key="cat.id"
          type="button"
          class="v2-ec-tab"
          :class="{ 'is-active': carton.activeCategory.value === cat.id }"
          @click="carton.activeCategory.value = cat.id"
        >
          {{ cat.icon }} {{ cat.name }} ({{ cat.count }})
        </button>
      </div>

      <div class="v2-ec-section-title">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--wr-text, #333)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
        </svg>
        <span>{{ currentCategoryName }}</span>
        <span style="font-size: 12px; color: var(--wr-muted, #999); font-weight: 400; margin-left: 4px;">共 {{ carton.filteredCartons.value.length }} 个</span>
      </div>

      <div v-if="carton.filteredCartons.value.length" class="v2-ec-carton-grid">
        <div
          v-for="item in carton.filteredCartons.value"
          :key="item.id"
          class="v2-ec-carton-card"
          :class="{ 'is-selected': selectedCartonId === item.id }"
          @click="handleSelectCarton(item)"
        >
          <div class="v2-ec-carton-card__bar" :style="{ background: getCardColor(item) }" />
          <div class="v2-ec-carton-card__inner">
            <div class="v2-ec-carton-card__image-wrap">
              <img :src="item.image" class="v2-ec-carton-card__image" :alt="item.name" />
            </div>
            <div class="v2-ec-carton-card__name">{{ item.name }}</div>
            <div class="v2-ec-carton-card__spec">{{ item.spec }}</div>
            <div class="v2-ec-carton-card__footer">
              <span class="v2-ec-carton-card__price">{{ item.unitPrice }}</span>
              <span class="v2-ec-carton-card__factory">{{ item.factoryName }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="v2-ec-carton-empty">暂无纸箱数据</div>

      <CartonCalculateSheet v-model="calcSheetOpen" />

      <button
        type="button"
        class="v2-ec-carton-fab"
        aria-label="纸箱计算"
        @click="calcSheetOpen = true"
      >
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="4" y="2" width="16" height="20" rx="2"/><line x1="8" y1="6" x2="16" y2="6"/><line x1="8" y1="10" x2="16" y2="10"/><line x1="8" y1="14" x2="12" y2="14"/><line x1="8" y1="18" x2="10" y2="18"/>
        </svg>
      </button>
    </div>
  </V2Page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import V2Page from '@/mobile-v2/components/V2Page.vue'
import { useMobileCarton } from '@/mobile/views/carton/useMobileCarton'
import CartonCalculateSheet from '@/mobile/views/carton/components/CartonCalculateSheet.vue'

import './styles/v2-ecommerce.scss'

const carton = useMobileCarton()
const calcSheetOpen = ref(false)
const selectedCartonId = ref<string | number | null>(null)

function getCardColor(item: { volume?: number }): string {
  if (!item.volume) return '#2563eb'
  if (item.volume < 5000) return '#22c55e'
  if (item.volume < 30000) return '#2563eb'
  return '#f59e0b'
}

const currentCategoryName = computed(() => {
  const cat = carton.categoryList.value.find((c) => c.id === carton.activeCategory.value)
  return cat ? cat.name : ''
})

onMounted(() => {
  void carton.loadCartons()
})

function handleSelectCarton(cartonItem: { id: string | number }) {
  selectedCartonId.value = cartonItem.id
  console.log('Selected carton:', cartonItem.id)
}
</script>

<style scoped lang="scss">
.v2-ec-carton-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 80px;
}

.v2-ec-carton-card {
  display: flex;
  flex-direction: column;
  background: var(--wr-card, #ffffff);
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  &:active {
    transform: scale(0.96);
  }

  &.is-selected {
    border-color: #2563eb;
    box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
  }
}

.v2-ec-carton-card__bar {
  height: 4px;
  flex-shrink: 0;
}

.v2-ec-carton-card__inner {
  padding: 12px;
}

.v2-ec-carton-card__image-wrap {
  width: 100%;
  height: 90px;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  margin-bottom: 8px;
}

.v2-ec-carton-card__image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.v2-ec-carton-card__name {
  font-size: 14px;
  font-weight: 700;
  color: var(--wr-text, #333);
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
}

.v2-ec-carton-card__spec {
  font-size: 13px;
  color: var(--wr-text-secondary, #666);
  margin-bottom: 6px;
  text-align: center;
}

.v2-ec-carton-card__footer {
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: center;
}

.v2-ec-carton-card__price {
  font-size: 16px;
  font-weight: 800;
  color: #c41e3a;
}

.v2-ec-carton-card__factory {
  font-size: 10px;
  color: var(--wr-muted, #999);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.v2-ec-carton-empty {
  padding: 40px 20px;
  text-align: center;
  font-size: 14px;
  color: var(--wr-muted, #999);
}

.v2-ec-carton-fab {
  position: fixed;
  bottom: 24px;
  right: 16px;
  z-index: 100;
  width: 56px;
  height: 56px;
  border: none;
  border-radius: 50%;
  background: #2563eb;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.4);
  transition: transform 0.2s ease, box-shadow 0.2s;
  padding: 0;

  &:active {
    transform: scale(1.05);
    box-shadow: 0 6px 20px rgba(37, 99, 235, 0.5);
  }
}
</style>
