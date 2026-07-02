<template>
  <div v-loading="carton.loading.value" class="mobile-carton-view">
    <div class="mobile-carton-view__header">
      <div class="mobile-carton-view__header-left">
        <MobileDoodleChip
          tag="button"
          type="button"
          shape="pill"
          color="#2563eb"
          class="mobile-carton-view__back"
          @click="$router.back()"
        >
          <span>←</span>
        </MobileDoodleChip>
        <h1 class="mobile-carton-view__title">📦 纸箱管理</h1>
      </div>
    </div>

    <div class="mobile-carton-view__content">
      <CartonSearchBar v-model="carton.searchQuery.value" />
      <CartonCategoryTabs
        :categories="carton.categoryList.value"
        v-model:active-category="carton.activeCategory.value"
      />

      <div class="mobile-carton-view__section">
        <div class="mobile-carton-view__section-head">
          <img :src="schemeAAssets.starYellow" class="mobile-carton-view__section-icon" alt="" />
          <h2 class="mobile-carton-view__section-title">
            {{ currentCategoryName }}
          </h2>
          <span class="mobile-carton-view__section-count">{{ carton.filteredCartons.value.length }}个</span>
        </div>

        <CartonGrid
          :cartons="carton.filteredCartons.value"
          @select="handleSelectCarton"
        />
      </div>
    </div>

    <CartonCalculateSheet v-model="calcSheetOpen" />

    <div class="mobile-carton-view__fab">
      <MobileDoodleChip
        tag="button"
        type="button"
        shape="pill"
        color="#2563eb"
        :filled="true"
        fill-color="#2563eb"
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
import { useMobileCarton } from '@/mobile/carton/useMobileCarton'
import CartonSearchBar from '@/mobile/carton/components/CartonSearchBar.vue'
import CartonCategoryTabs from '@/mobile/carton/components/CartonCategoryTabs.vue'
import CartonGrid from '@/mobile/carton/components/CartonGrid.vue'
import CartonCalculateSheet from '@/mobile/carton/components/CartonCalculateSheet.vue'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import { schemeAAssets } from '@/mobile/home/themes/scheme-a/assets'

const calcIconUrl = `${import.meta.env.BASE_URL}mobile-home/scheme-a/icon-chart.svg`
const carton = useMobileCarton()
const calcSheetOpen = ref(false)

const currentCategoryName = computed(() => {
  const cat = carton.categoryList.value.find((c) => c.id === carton.activeCategory.value)
  return cat ? cat.icon + ' ' + cat.name : ''
})

onMounted(() => {
  void carton.loadCartons()
})

function handleSelectCarton(cartonItem: { id: number }) {
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

.mobile-carton-view__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: max(16px, env(safe-area-inset-top)) 16px 16px;
  background: #fff;
}

.mobile-carton-view__header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-carton-view__back {
  width: 36px;
  height: 36px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #2563eb;
  font-weight: 700;
  cursor: pointer;
  background: #fff;
  transition: transform 0.2s ease;

  :deep(.sa-doodle-frame__body) {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    padding: 0;
  }

  &:active {
    transform: scale(0.9);
  }
}

.mobile-carton-view__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 24px;
  color: #1e293b;
  margin: 0;
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

.mobile-carton-view__section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.mobile-carton-view__section-icon {
  width: 22px;
  height: 22px;
}

.mobile-carton-view__section-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 18px;
  color: #1e293b;
  margin: 0;
}

.mobile-carton-view__section-count {
  font-size: 12px;
  color: #94a3b8;
  margin-left: auto;
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
  filter: brightness(0) invert(1);
}
</style>