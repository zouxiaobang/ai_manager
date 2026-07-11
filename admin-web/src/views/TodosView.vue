<!--
 * 待办页面组件
 * 展示和管理个人待办任务，支持任务创建、完成、筛选和排序
 * 内嵌 NotebookTodosView 组件实现核心待办功能
 -->
<template>
  <!-- 页面主容器：待办页面整体布局 -->
  <WarRoomPage :title="t('portal.menu.todos')" fill>
    <!-- 待办内容区域：嵌入 NotebookTodosView 组件实现核心待办功能 -->
    <div class="todos-page war-room-panel">
      <!-- 待办列表组件：展示和管理待办任务 -->
      <NotebookTodosView ref="todosRef" :initial-filter="initialFilter" />
    </div>
  </WarRoomPage>
</template>

<script setup lang="ts">
/**
 * 待办页面组件
 * 展示和管理个人待办任务，支持任务创建、完成、筛选和排序
 * 内嵌 NotebookTodosView 组件实现核心待办功能
 */
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import type { NbTodoFilter } from '@/api/notebook/todo'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
import NotebookTodosView from './notebook/NotebookTodosView.vue'

const { t } = useI18n() // 国际化函数
const route = useRoute() // 路由实例
const todosRef = ref<InstanceType<typeof NotebookTodosView> | null>(null) // 待办列表组件引用

const initialFilter = computed((): NbTodoFilter | undefined => {
  const value = route.query.filter
  if (value === 'today') return 'all'
  if (value === 'pending' || value === 'done' || value === 'all') {
    return value
  }
  return undefined
})

function applyRouteFilter() {
  // 应用路由中的筛选参数
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
