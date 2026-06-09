<script setup lang="ts">
import { PAGE_SIZE_OPTIONS } from '@/api/pagination'

defineProps<{
  total: number
  page: number
  pageSize: number
}>()

const emit = defineEmits<{
  'update:page': [number]
  'update:pageSize': [number]
}>()

function onPageChange(value: number) {
  emit('update:page', value)
}

function onSizeChange(value: number) {
  emit('update:pageSize', value)
}
</script>

<template>
  <div class="table-pagination">
    <el-pagination
      :current-page="page"
      :page-size="pageSize"
      :page-sizes="PAGE_SIZE_OPTIONS"
      :total="total"
      background
      layout="total, sizes, prev, pager, next, jumper"
      @current-change="onPageChange"
      @size-change="onSizeChange"
    />
  </div>
</template>

<style scoped lang="scss">
.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
