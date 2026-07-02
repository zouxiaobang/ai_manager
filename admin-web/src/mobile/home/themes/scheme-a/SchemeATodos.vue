<template>
  <section v-if="home.showTodosSection.value" class="mh-scheme-a__section">
    <div class="mh-scheme-a__section-head">
      <img class="mh-scheme-a__section-icon" :src="schemeAAssets.starBlue" alt="" />
      <h2 class="mh-scheme-a__section-title">{{ home.t('portal.dashboard.todayTodos') }}</h2>
    </div>

    <SchemeADoodleFrame
      class="mh-scheme-a__todos-card"
      color="#2563eb"
      :seed="doodleSeedFromKey('home-todos')"
      sketch
      :stroke-width="3"
      :shadow="false"
    >
      <img class="mh-scheme-a__paperclip" :src="schemeAAssets.paperclip" alt="" />

      <div v-if="home.displayTodos.value.length" class="mh-scheme-a__todo-list">
        <div v-for="item in home.displayTodos.value" :key="item.id" class="mh-scheme-a__todo-row">
          <button
            type="button"
            class="mh-scheme-a__todo-check"
            :class="{ 'is-done': item.completed === 1 }"
            :aria-label="home.t('portal.dashboard.todayTodos')"
            @click="home.onToggle(item, item.completed !== 1)"
          />
          <button type="button" class="mh-scheme-a__todo-text" @click="home.router.push('/todos')">
            {{ item.content }}
          </button>
        </div>
      </div>
      <div v-else class="mh-scheme-a__empty">{{ home.t('portal.dashboard.todayTodosEmpty') }}</div>

      <img class="mh-scheme-a__todo-squiggle" :src="schemeAAssets.squiggleRed" alt="" />
    </SchemeADoodleFrame>
  </section>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_HOME_KEY } from '../../mobileHomeContext'
import { schemeAAssets } from './assets'
import { doodleSeedFromKey } from '@/mobile/utils/doodleSeed'
import SchemeADoodleFrame from './SchemeADoodleFrame.vue'

const home = inject(MOBILE_HOME_KEY)!
</script>
