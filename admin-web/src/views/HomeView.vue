<template>
  <div class="dashboard">
    <h2 class="dashboard-page-title">{{ t('portal.dashboard.title') }}</h2>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="dashboard-card">
          <h3 class="dashboard-section-title">🛡️ {{ t('portal.dashboard.systems') }}</h3>
          <div class="system-status-item">
            <span>{{ t('portal.dashboard.aiManager') }}</span>
            <el-space>
              <el-tag :type="healthTagType" size="small">{{ healthLabel }}</el-tag>
              <el-button size="small" link :loading="healthLoading" @click="checkHealth">
                {{ t('home.healthCheck') }}
              </el-button>
            </el-space>
          </div>
          <div v-for="sys in systemBoard" :key="sys.key" class="system-status-item">
            <span>{{ t(`portal.dashboard.systemsList.${sys.key}`) }}</span>
            <el-tag :type="sys.tagType" size="small">
              {{ t(`portal.dashboard.systemsStatus.${sys.statusKey}`, { count: sys.count }) }}
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="dashboard-card">
          <h3 class="dashboard-section-title">🔔 {{ t('portal.dashboard.notices') }}</h3>
          <div v-for="(notice, idx) in noticeKeys" :key="idx" class="notice-item">
            {{ t(`portal.dashboard.noticeItems.${notice}.title`) }}
            <span class="notice-item__date">
              [{{ t(`portal.dashboard.noticeItems.${notice}.date`) }}]
            </span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="dashboard-card">
      <h3 class="dashboard-section-title">⚙️ {{ t('portal.dashboard.todos') }}</h3>
      <div v-for="todo in todoKeys" :key="todo" class="todo-item">
        <div>
          <el-tag
            size="small"
            :type="todo === 'travel' || todo === 'server' ? 'warning' : 'info'"
          >
            {{ t(`portal.dashboard.todoItems.${todo}.badge`) }}
          </el-tag>
          {{ t(`portal.dashboard.todoItems.${todo}.title`) }}
        </div>
        <div class="todo-item__meta">
          {{ t('portal.dashboard.todoFrom') }}
          {{ t(`portal.dashboard.todoItems.${todo}.from`) }}
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="dashboard-card">
      <h3 class="dashboard-section-title">🚀 {{ t('portal.dashboard.allSystems') }}</h3>
      <el-row :gutter="16">
        <el-col
          v-for="group in systemGroupKeys"
          :key="group"
          :xs="24"
          :md="8"
        >
          <div class="system-group">
            <div class="system-group__title">
              {{ t(`portal.dashboard.systemGroups.${group}.title`) }}
            </div>
            <router-link
              v-for="entry in getGroupEntries(group)"
              :key="entry"
              :to="entry === 'permission' ? '/users' : '/home'"
              class="system-entry"
            >
              {{ t(`portal.dashboard.systemGroups.${group}.items.${entry}`) }}
            </router-link>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchHealth } from '@/api/health'

const { t } = useI18n()

const healthLoading = ref(false)
const healthStatus = ref<'unknown' | 'up' | 'down'>('unknown')

const systemBoard = [
  { key: 'hr', statusKey: 'todo', count: 3, tagType: 'warning' as const },
  { key: 'supply', statusKey: 'alert', count: 1, tagType: 'danger' as const },
]

const noticeKeys = ['security', 'maintenance'] as const
const todoKeys = ['travel', 'inventory', 'server'] as const
const systemGroupKeys = ['ops', 'admin', 'devops'] as const

const groupEntries: Record<(typeof systemGroupKeys)[number], string[]> = {
  ops: ['bi', 'crm'],
  admin: ['oa', 'hr'],
  devops: ['permission', 'devops'],
}

function getGroupEntries(group: (typeof systemGroupKeys)[number]) {
  return groupEntries[group]
}

const healthTagType = computed(() => {
  if (healthStatus.value === 'up') return 'success'
  if (healthStatus.value === 'down') return 'danger'
  return 'info'
})

const healthLabel = computed(() => {
  if (healthStatus.value === 'up') return t('home.healthOk')
  if (healthStatus.value === 'down') return t('home.healthFail')
  return t('portal.dashboard.healthUnknown')
})

async function checkHealth() {
  healthLoading.value = true
  try {
    const data = await fetchHealth()
    healthStatus.value = data.status === 'UP' ? 'up' : 'down'
  } catch {
    healthStatus.value = 'down'
  } finally {
    healthLoading.value = false
  }
}

onMounted(checkHealth)
</script>

<style scoped lang="scss">
.dashboard-page-title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
</style>
