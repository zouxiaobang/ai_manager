<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>{{ t('portal.menu.permission') }}</span>
        <el-button size="small" :loading="loading" @click="loadUsers">
          {{ t('user.refresh') }}
        </el-button>
      </div>
    </template>

    <el-table :data="records" stripe border style="width: 100%" v-loading="loading">
      <el-table-column prop="id" :label="t('user.id')" width="80" />
      <el-table-column prop="username" :label="t('user.username')" />
      <el-table-column prop="nickname" :label="t('user.nickname')" />
      <el-table-column prop="status" :label="t('user.status')" width="120" />
    </el-table>

    <TablePagination
      :page="page"
      :page-size="pageSize"
      :total="total"
      @update:page="onPageChange"
      @update:page-size="onSizeChange"
    />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchUsers } from '@/api/user'
import type { SysUser } from '@/api/types'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'

const { t } = useI18n()

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } =
  usePagination<SysUser>((p, ps) => fetchUsers({ page: p, pageSize: ps }))

async function loadUsers() {
  await load()
}

onMounted(loadUsers)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
