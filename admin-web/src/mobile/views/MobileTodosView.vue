<template>
  <div class="mobile-todos">
    <NotebookTodosView />
  </div>
</template>

<script setup lang="ts">
import NotebookTodosView from '@/views/notebook/NotebookTodosView.vue'
</script>

<style scoped lang="scss">
.mobile-todos {
  margin: -12px;
  padding: 0 4px;
}
</style>
