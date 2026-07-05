<template>
  <div class="mh-doodle mh-theme mh-theme-c">
    <HomeSearch />

    <section v-if="home.showOverview.value" class="mh-doodle__card mh-theme-c__frame">
      <h2 class="mh-doodle__title">{{ home.t('mobile.home.overviewTitle') }}</h2>
      <HomeOverview />
    </section>

    <div v-if="home.showOverview.value" class="mh-doodle__wave" aria-hidden="true" />

    <HomeTodos card-class="mh-theme-c__card" />
    <HomeModulesScroll card-class="mh-theme-c__card" />
    <HomeStatus card-class="mh-theme-c__card" />
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_HOME_KEY } from '../mobileHomeContext'
import HomeSearch from '../components/HomeSearch.vue'
import HomeOverview from '../components/HomeOverview.vue'
import HomeTodos from '../components/HomeTodos.vue'
import HomeModulesScroll from '../components/HomeModulesScroll.vue'
import HomeStatus from '../components/HomeStatus.vue'

const home = inject(MOBILE_HOME_KEY)!
</script>

<style scoped lang="scss">
.mh-theme-c__frame {
  --mh-card-border: #2563eb;
  border-width: 3px;
  box-shadow: 0 6px 18px rgb(37 99 235 / 14%);
}

.mh-theme-c__card {
  --mh-card-border: #cbd5e1;
}
</style>
