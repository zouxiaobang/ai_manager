<template>
  <div v-loading="factory.loading.value" class="mfc-shell mh-scheme-a">
    <FactoryPosterHeader />

    <main class="mfc-content">
<!--      <FactorySearch />-->
      <MobileDoodleSearch
          v-model="factory.searchQuery.value"
          :placeholder="factory.t('ecommerce.factory.searchPlaceholder')"
      />
      <FactoryTypeFilter />
      <FactoryList />
    </main>

    <div class="mfc-fab">
      <button
        type="button"
        class="mfc-fab__btn"
        :aria-label="factory.t('ecommerce.factory.add')"
        @click="factory.openCreate()"
      >
        <span class="mfc-fab__icon">+</span>
        <span class="mfc-fab__label">{{ factory.t('mobile.factory.createShort') }}</span>
      </button>
    </div>

    <FactoryFormSheet />
    <FactoryDeleteConfirm />
  </div>
</template>

<script setup lang="ts">
import { onMounted, provide } from 'vue'
import { MOBILE_FACTORY_KEY } from '@/mobile/views/factory/factoryContext'
import { useMobileFactory } from '@/mobile/views/factory/useMobileFactory'
import FactoryPosterHeader from '@/mobile/views/factory/components/FactoryPosterHeader.vue'
import FactorySearch from '@/mobile/views/factory/components/FactorySearch.vue'
import FactoryTypeFilter from '@/mobile/views/factory/components/FactoryTypeFilter.vue'
import FactoryList from '@/mobile/views/factory/components/FactoryList.vue'
import FactoryFormSheet from '@/mobile/views/factory/components/FactoryFormSheet.vue'
import FactoryDeleteConfirm from '@/mobile/views/factory/components/FactoryDeleteConfirm.vue'
import MobileDoodleSearch from "@/mobile/components/MobileDoodleSearch.vue";

const factory = useMobileFactory()
provide(MOBILE_FACTORY_KEY, factory)

onMounted(() => {
  void factory.init()
})
</script>

<style lang="scss">
@use '@/mobile/views/home/themes/scheme-a/scheme-a.scss';
@use '@/mobile/views/factory/styles/mobile-factory.scss';
</style>
