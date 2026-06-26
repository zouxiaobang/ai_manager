<template>
  <WarRoomPage :title="t('portal.menu.todos')" fill>
    <div class="todos-page war-room-panel">
      <NotebookTodosView ref="todosRef" :initial-filter="initialFilter" />
    </div>
  </WarRoomPage>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import type { NbTodoFilter } from '@/api/notebook/todo'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
import NotebookTodosView from './notebook/NotebookTodosView.vue'

const { t } = useI18n()
const route = useRoute()
const todosRef = ref<InstanceType<typeof NotebookTodosView> | null>(null)

const initialFilter = computed((): NbTodoFilter | undefined => {
  const value = route.query.filter
  if (value === 'today') return 'all'
  if (value === 'pending' || value === 'done' || value === 'all') {
    return value
  }
  return undefined
})

function applyRouteFilter() {
  const filter = initialFilter.value
  if (filter) {
    void nextTick(() => todosRef.value?.setFilter(filter))
  }
}

watch(
  () => route.query.filter,
  () => {
    applyRouteFilter()
  },
)

onMounted(() => {
  applyRouteFilter()
})
</script>

<style scoped lang="scss">
.todos-page {
  min-height: 0;
}
</style>
