<!--
 * 用户列表页面组件
 * 展示系统用户列表，支持用户信息查看和权限管理
 * 提供用户数据刷新功能
 -->
<template>
  <!-- 页面主容器：用户列表页面整体布局 -->
  <WarRoomPage :title="t('portal.menu.permission')">
    <!-- 用户列表内容区域 -->
    <div class="war-room-panel">
      <!-- 工具栏：标题和刷新按钮 -->
      <div class="users-toolbar">
        <span>{{ t('portal.menu.permission') }}</span>
        <el-button size="small" :loading="loading" @click="loadUsers">
          {{ t('user.refresh') }}
        </el-button>
      </div>

      <!-- 用户列表表格：展示用户信息 -->
      <el-table :data="records" stripe border style="width: 100%" v-loading="loading">
        <el-table-column prop="id" :label="t('user.id')" width="80" />
        <el-table-column prop="username" :label="t('user.username')" />
        <el-table-column prop="nickname" :label="t('user.nickname')" />
        <el-table-column prop="status" :label="t('user.status')" width="120" />
      </el-table>

      <!-- 分页组件：用户列表分页 -->
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
/**
 * 用户列表页面组件
 * 展示系统用户列表，支持用户信息查看
 * 提供用户数据刷新和分页功能
 */
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
import { fetchUsers } from '@/api/user'
import type { SysUser } from '@/api/types'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'

const { t } = useI18n() // 国际化函数

// 分页组合式函数：管理用户列表分页数据
const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } =
  usePagination<SysUser>((p, ps) => fetchUsers({ page: p, pageSize: ps }))

function loadUsers() {
  // 加载用户列表数据
  void load()
}

onMounted(() => {
  // 页面挂载时加载用户数据
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
