<template>
  <div class="mh-doodle mh-theme mh-theme-b">
    <header class="mh-theme-b__poster mh-doodle__card">
      <span class="mh-theme-b__online">{{ home.t('mobile.home.onlineBadge') }}</span>
      <h2 class="mh-doodle__hero-title">{{ home.t('mobile.home.pageTitle') }}</h2>
      <p class="mh-theme-b__subtitle">{{ home.t('mobile.home.heroSubtitle') }}</p>
      <img class="mh-theme-b__illus" :src="home.iconUrl('pixel-dog')" alt="" />
      <div class="mh-doodle__summary-pill">{{ home.summaryPillText.value }}</div>
    </header>

    <HomeSearch />

    <div v-if="home.showOverview.value" class="mh-doodle__banner">{{ home.t('mobile.home.overviewTitle') }}</div>

    <section v-if="home.showOverview.value" class="mh-doodle__card mh-theme-b__card">
      <HomeOverview />
    </section>

    <HomeTodos card-class="mh-theme-b__card" />
    <HomeModulesGrid card-class="mh-theme-b__card" />
    <HomeStatus card-class="mh-theme-b__card" />
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_HOME_KEY } from '../mobileHomeContext'
import HomeSearch from '../components/HomeSearch.vue'
import HomeOverview from '../components/HomeOverview.vue'
import HomeTodos from '../components/HomeTodos.vue'
import HomeModulesGrid from '../components/HomeModulesGrid.vue'
import HomeStatus from '../components/HomeStatus.vue'

const home = inject(MOBILE_HOME_KEY)!
</script>

<style scoped lang="scss">
.mh-theme-b__poster {
  position: relative;
  --mh-card-border: var(--mh-blue);
  padding-bottom: 16px;
}

.mh-theme-b__online {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 10px;
  border-radius: 8px;
  background: var(--mh-red);
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}

.mh-theme-b__subtitle {
  margin: 4px 0 12px;
  font-size: 13px;
  color: #64748b;
  font-weight: 600;
}

.mh-theme-b__illus {
  display: block;
  width: 100%;
  max-width: 200px;
  margin: 0 auto 12px;
  border-radius: 14px;
  border: 2.5px dashed var(--mh-blue);
  padding: 12px;
  background: #fff;
}

.mh-theme-b__card {
  --mh-card-border: #93c5fd;
}
</style>
