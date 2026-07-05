<template>
  <section v-if="home.showTodosSection.value" class="mh-doodle__card" :class="cardClass">
    <div class="mh-doodle__section-head">
      <h2 class="mh-doodle__title">{{ home.t('portal.dashboard.todayTodos') }}</h2>
      <button type="button" class="mh-doodle__link" @click="home.router.push('/todos')">
        {{ home.t('portal.dashboard.viewAllTodayTodos') }}
        <el-icon><ArrowRight /></el-icon>
      </button>
    </div>
    <div v-if="home.displayTodos.value.length" class="mh-doodle__todo-list">
      <div v-for="item in home.displayTodos.value" :key="item.id" class="mh-doodle__todo-row">
        <el-checkbox
          :model-value="item.completed === 1"
          @change="(checked: boolean) => home.onToggle(item, checked)"
        />
        <button
          type="button"
          class="mh-doodle__todo-body mh-doodle__tap"
          @click="home.router.push('/todos')"
        >
          <span class="mh-doodle__todo-text">{{ item.content }}</span>
        </button>
        <span v-if="home.todoTimeText(item)" class="mh-doodle__todo-time">{{ home.todoTimeText(item) }}</span>
      </div>
    </div>
    <div v-else class="mobile-empty-hint">{{ home.t('portal.dashboard.todayTodosEmpty') }}</div>
  </section>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'
import { MOBILE_HOME_KEY } from '../mobileHomeContext'

defineProps<{
  cardClass?: string
}>()

const home = inject(MOBILE_HOME_KEY)!
</script>
