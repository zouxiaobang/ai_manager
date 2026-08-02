<template>
  <div v-loading="loading" class="mobile-page">
    <div class="mobile-users__toolbar">
      <el-button size="small" :loading="loading" @click="loadUsers">
        {{ t('user.refresh') }}
      </el-button>
    </div>

    <section v-if="records.length" class="mobile-card">
      <div v-for="user in records" :key="user.id" class="mobile-list-item">
        <div class="mobile-list-item__body">
          <div class="mobile-list-item__title">{{ user.nickname || user.username }}</div>
          <div class="mobile-list-item__meta">
            {{ t('user.username') }}：{{ user.username }} · ID {{ user.id }}
          </div>
        </div>
        <el-tag size="small">{{ user.status }}</el-tag>
      </div>
    </section>
    <div v-else class="mobile-empty-hint">{{ t('user.empty') }}</div>

    <TablePagination
      :page="page"
      :page-size="pageSize"
      :total="total"
      @update:page="onPageChange"
      @update:page-size="onSizeChange"
    />
  </div>
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

<style scoped lang="scss">
.mobile-users__toolbar {
  display: flex;
  justify-content: flex-end;
}
</style>
