<template>
  <div class="mh-doodle mh-theme mh-theme-d">
    <header class="mh-theme-d__poster mh-doodle__card">
      <h2 class="mh-doodle__hero-title">{{ home.t('mobile.home.pageTitle') }}</h2>
      <p class="mh-theme-d__subtitle">{{ home.t('mobile.home.heroSubtitle') }}</p>
      <span class="mh-theme-d__tag">{{ home.t('mobile.home.versionTag') }}</span>
    </header>

    <div v-if="home.showOverview.value" class="mh-doodle__quick-row">
      <button type="button" class="mh-doodle__quick-item mh-doodle__tap" @click="home.router.push('/notebook')">
        <img class="mh-doodle__quick-icon" :src="home.iconUrl('notebook')" alt="" />
        <span class="mh-doodle__quick-label">{{ home.t('portal.menu.notebook') }}</span>
      </button>
      <button type="button" class="mh-doodle__quick-item mh-doodle__tap" @click="home.router.push('/todos')">
        <img class="mh-doodle__quick-icon" :src="home.iconUrl('todos')" alt="" />
        <span class="mh-doodle__quick-label">{{ home.t('portal.menu.todos') }}</span>
      </button>
      <button type="button" class="mh-doodle__quick-item mh-doodle__tap" @click="home.router.push('/ecommerce')">
        <img class="mh-doodle__quick-icon" :src="home.iconUrl('ecommerce')" alt="" />
        <span class="mh-doodle__quick-label">{{ home.t('portal.menu.ecommerce') }}</span>
      </button>
      <button type="button" class="mh-doodle__quick-item mh-doodle__tap" @click="home.router.push('/more')">
        <img class="mh-doodle__quick-icon" :src="home.iconUrl('settings')" alt="" />
        <span class="mh-doodle__quick-label">{{ home.t('mobile.nav.more') }}</span>
      </button>
    </div>

    <section v-if="home.showOverview.value" class="mh-doodle__card mh-theme-d__stamp-card">
      <div class="mh-doodle__section-head">
        <h2 class="mh-doodle__title">{{ home.t('mobile.home.todoProgress') }}</h2>
        <span class="mh-theme-d__ratio">{{ home.todoProgressText.value }}</span>
      </div>
      <div class="mh-doodle__stamp-row">
        <span
          v-for="i in 7"
          :key="i"
          class="mh-doodle__stamp"
          :class="{ 'is-done': i <= todoDoneCount }"
        >{{ i <= todoDoneCount ? '★' : '' }}</span>
      </div>
    </section>

    <HomeSearch />

    <section v-if="home.showOverview.value" class="mh-doodle__card">
      <h2 class="mh-doodle__title">{{ home.t('mobile.home.overviewTitle') }}</h2>
      <HomeOverview />
    </section>

    <HomeTodos />
    <HomeModulesGrid />
    <HomeStatus />
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { MOBILE_HOME_KEY } from '../mobileHomeContext'
import HomeSearch from '../components/HomeSearch.vue'
import HomeOverview from '../components/HomeOverview.vue'
import HomeTodos from '../components/HomeTodos.vue'
import HomeModulesGrid from '../components/HomeModulesGrid.vue'
import HomeStatus from '../components/HomeStatus.vue'

const home = inject(MOBILE_HOME_KEY)!

const todoDoneCount = computed(() => {
  const text = home.todoProgressText.value
  const done = Number(text.split('/')[0])
  return Number.isFinite(done) ? done : 0
})
</script>

<style scoped lang="scss">
.mh-theme-d__poster {
  --mh-card-border: var(--mh-blue);
  text-align: center;
}

.mh-theme-d__subtitle {
  margin: 6px 0 10px;
  font-size: 13px;
  color: #64748b;
  font-weight: 600;
}

.mh-theme-d__tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 8px;
  background: var(--mh-yellow);
  border: 2px solid #f59e0b;
  font-size: 12px;
  font-weight: 800;
  color: #78350f;
}

.mh-theme-d__stamp-card {
  --mh-card-border: #f59e0b;
}

.mh-theme-d__ratio {
  font-size: 14px;
  font-weight: 800;
  color: var(--mh-red);
}
</style>
