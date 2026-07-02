<template>
  <div v-loading="factory.loading.value" class="mfc-shell mh-scheme-a">
    <FactoryPosterHeader />

    <main class="mfc-content">
      <FactorySearch />
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
import { MOBILE_FACTORY_KEY } from '@/mobile/factory/factoryContext'
import { useMobileFactory } from '@/mobile/factory/useMobileFactory'
import FactoryPosterHeader from '@/mobile/factory/components/FactoryPosterHeader.vue'
import FactorySearch from '@/mobile/factory/components/FactorySearch.vue'
import FactoryTypeFilter from '@/mobile/factory/components/FactoryTypeFilter.vue'
import FactoryList from '@/mobile/factory/components/FactoryList.vue'
import FactoryFormSheet from '@/mobile/factory/components/FactoryFormSheet.vue'
import FactoryDeleteConfirm from '@/mobile/factory/components/FactoryDeleteConfirm.vue'

const factory = useMobileFactory()
provide(MOBILE_FACTORY_KEY, factory)

onMounted(() => {
  void factory.init()
})
</script>

<style lang="scss">
@use '@/mobile/home/themes/scheme-a/scheme-a.scss';
@use '@/mobile/factory/styles/mobile-factory.scss';
</style>
