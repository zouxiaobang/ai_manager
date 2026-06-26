<template>
  <WarRoomPage :title="t('portal.menu.permission')">
    <div class="war-room-panel">
      <div class="users-toolbar">
        <span>{{ t('portal.menu.permission') }}</span>
        <el-button size="small" :loading="loading" @click="loadUsers">
          {{ t('user.refresh') }}
        </el-button>
      </div>

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
    </div>
  </WarRoomPage>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
import { fetchUsers } from '@/api/user'
import type { SysUser } from '@/api/types'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'

const { t } = useI18n()

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } =
  usePagination<SysUser>((p, ps) => fetchUsers({ page: p, pageSize: ps }))

function loadUsers() {
  void load()
}

onMounted(() => {
  void loadUsers()
})
</script>

<style scoped lang="scss">
.users-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}
</style>
