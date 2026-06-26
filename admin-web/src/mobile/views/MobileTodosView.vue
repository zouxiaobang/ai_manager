<template>
  <div class="mobile-todos">
    <NotebookTodosView ref="todosRef" :initial-filter="initialFilter" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { NbTodoFilter } from '@/api/notebook/todo'
import NotebookTodosView from '@/views/notebook/NotebookTodosView.vue'

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
.mobile-todos {
  margin: -12px;
  padding: 0 4px;
}
</style>
