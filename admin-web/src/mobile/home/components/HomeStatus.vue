<template>
  <section v-if="home.showOverview.value" class="mh-doodle__card" :class="cardClass">
    <h2 v-if="showTitle" class="mh-doodle__title">{{ home.t('mobile.home.serviceStatus') }}</h2>
    <div class="mh-doodle__status-row">
      <div
        v-for="node in home.serviceNodes.value"
        :key="node.key"
        class="mh-doodle__status-pill"
        :class="`is-${node.state}`"
      >
        <span class="mh-doodle__status-dot" aria-hidden="true" />
        <span class="mh-doodle__status-name">{{ node.label }}</span>
        <span class="mh-doodle__status-text">{{ home.statusText(node.state) }}</span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_HOME_KEY } from '../mobileHomeContext'

withDefaults(
  defineProps<{
    cardClass?: string
    showTitle?: boolean
  }>(),
  { showTitle: true },
)

const home = inject(MOBILE_HOME_KEY)!
</script>
