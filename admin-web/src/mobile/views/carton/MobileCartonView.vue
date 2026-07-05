<template>
  <div v-loading="carton.loading.value" class="mobile-carton-view">
    <MobilePageHeader title="📦 纸箱管理" @back="$router.back()" />

    <div class="mobile-carton-view__content">
      <MobileDoodleSearch
        v-model="carton.searchQuery.value"
        placeholder="搜索纸箱..."
      />
      <MobileCategoryTabs
        :categories="carton.categoryList.value"
        v-model:active-value="carton.activeCategory.value"
      />

      <div class="mobile-carton-view__section">
        <MobileSectionHeader
          :icon="schemeAAssets.starYellow"
          :title="currentCategoryName"
          :count="carton.filteredCartons.value.length"
        />

        <MobileCardGrid
          :items="carton.filteredCartons.value"
          empty-text="暂无纸箱数据"
          :selectable="true"
          v-model="selectedCartonId"
          @select="handleSelectCarton"
        >
          <template #card="{ item, selected, select }">
            <SchemeADoodleFrame
              tag="button"
              type="button"
              class="carton-grid-card"
              :color="getCardColor(item as any)"
              :seed="item.id"
              :selected="selected"
              sketch
              :stroke-width="3.5"
              :shadow="false"
              @click="select"
            >
              <div class="carton-grid-card__inner">
                <div class="carton-grid-card__image-wrap">
                  <img :src="(item as any).image" class="carton-grid-card__image" :alt="(item as any).name" />
                </div>
                <div class="carton-grid-card__info">
                  <div class="carton-grid-card__name">{{ (item as any).name }}</div>
                  <div class="carton-grid-card__spec">{{ (item as any).spec }}</div>
                  <div class="carton-grid-card__footer">
                    <span class="carton-grid-card__price">{{ (item as any).unitPrice }}</span>
                    <span class="carton-grid-card__factory">{{ (item as any).factoryName }}</span>
                  </div>
                </div>
              </div>
            </SchemeADoodleFrame>
          </template>
        </MobileCardGrid>
      </div>
    </div>

    <CartonCalculateSheet v-model="calcSheetOpen" />

    <div class="mobile-carton-view__fab">
      <MobileDoodleChip
        tag="button"
        type="button"
        shape="rect"
        color="#2563eb"
        class="mobile-carton-view__fab-btn"
        aria-label="纸箱计算"
        @click="calcSheetOpen = true"
      >
        <img class="mobile-carton-view__fab-icon" :src="calcIconUrl" alt="" />
      </MobileDoodleChip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useMobileCarton } from '@/mobile/views/carton/useMobileCarton'
import MobilePageHeader from '@/mobile/components/MobilePageHeader.vue'
import MobileDoodleSearch from '@/mobile/components/MobileDoodleSearch.vue'
import MobileCategoryTabs from '@/mobile/components/MobileCategoryTabs.vue'
import MobileSectionHeader from '@/mobile/components/MobileSectionHeader.vue'
import MobileCardGrid from '@/mobile/components/MobileCardGrid.vue'
import CartonCalculateSheet from '@/mobile/views/carton/components/CartonCalculateSheet.vue'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'

const calcIconUrl = `${import.meta.env.BASE_URL}mobile-home/scheme-a/icon-chart.svg`
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
  console.log('Selected carton:', cartonItem.id)
}
</script>

<style scoped lang="scss">
.mobile-carton-view {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.mobile-carton-view__content {
  flex: 1;
  padding: 0 16px;
  padding-bottom: 80px;
  overflow-y: auto;
  background: #fff;
}

.mobile-carton-view__section {
  margin-top: 8px;
}

.mobile-carton-view__fab {
  position: fixed;
  bottom: 24px;
  right: 16px;
  z-index: 100;
}

.mobile-carton-view__fab-btn {
  width: 56px;
  height: 56px;
  padding: 0;
  background-color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.4);
  transition: transform 0.2s ease;

  :deep(.sa-doodle-frame__body) {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    padding: 0;
  }

  &:active {
    transform: scale(1.05);
  }
}

.mobile-carton-view__fab-icon {
  display: block;
  width: 24px;
  height: 24px;
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
</style>
