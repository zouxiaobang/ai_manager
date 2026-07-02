<template>
  <div v-loading="home.loading.value" class="mobile-home-shell">
    <component :is="themeComponent" />
  </div>
</template>

<script setup lang="ts">
import { computed, provide } from 'vue'
import { useAppStore } from '@/stores/app'
import type { MobileHomeThemeId } from '@/data/mobile-home-themes'
import { MOBILE_HOME_KEY } from '@/mobile/home/mobileHomeContext'
import { useMobileHome } from '@/mobile/home/useMobileHome'
import ThemeA from '@/mobile/home/themes/ThemeA.vue'
import ThemeB from '@/mobile/home/themes/ThemeB.vue'
import ThemeC from '@/mobile/home/themes/ThemeC.vue'
import ThemeD from '@/mobile/home/themes/ThemeD.vue'

const themeComponents: Record<MobileHomeThemeId, object> = {
  'scheme-a': ThemeA,
  'scheme-b': ThemeB,
  'scheme-c': ThemeC,
  'scheme-d': ThemeD,
}

const appStore = useAppStore()
const home = useMobileHome()

provide(MOBILE_HOME_KEY, home)

const themeComponent = computed(() => themeComponents[appStore.mobileHomeTheme])
</script>

<style lang="scss">
@use '@/mobile/home/styles/home-shared.scss';

.mobile-home-shell {
  min-height: 100%;
}

html.is-mobile-shell[data-mobile-home-theme]:not([data-mobile-home-theme='scheme-a']) {
  .mobile-app__main {
    background: #faf8f5;
  }
}

html.is-mobile-shell[data-mobile-home-theme]:not([data-mobile-home-theme='scheme-a']) .mobile-app__tabbar {
  background: #2563eb;
  border-top: none;
  border-radius: 20px 20px 0 0;
  box-shadow: 0 -4px 16px rgb(37 99 235 / 22%);
}

html.is-mobile-shell[data-mobile-home-theme]:not([data-mobile-home-theme='scheme-a']) .mobile-app__tab {
  color: rgb(255 255 255 / 72%);

  &.is-active {
    color: #fbbf24;
    font-weight: 700;
  }
}
</style>
