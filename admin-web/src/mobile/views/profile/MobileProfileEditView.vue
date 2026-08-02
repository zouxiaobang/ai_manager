<template>
  <MobilePage>
    <MobileCard>
      <div class="v2-profile-avatar">
        <div class="v2-profile-avatar__circle">
          <el-icon :size="40"><UserFilled /></el-icon>
        </div>
        <div class="v2-profile-avatar__name">{{ form.nickname || form.username }}</div>
      </div>

      <el-form label-width="80px" class="v2-profile-form">
        <el-form-item :label="t('mobile.v2.profileId')">
          <span class="v2-profile-form__value">{{ form.id }}</span>
        </el-form-item>

        <el-form-item :label="t('mobile.v2.profileUsername')">
          <span class="v2-profile-form__value">{{ form.username }}</span>
        </el-form-item>

        <el-form-item :label="t('mobile.v2.profileNickname')">
          <el-input v-model="form.nickname" :placeholder="form.username" maxlength="32" />
        </el-form-item>

        <el-form-item :label="t('mobile.v2.profileStatus')">
          <el-tag :type="form.status === 'ENABLED' ? 'success' : 'danger'" size="small">
            {{ form.status === 'ENABLED' ? t('mobile.v2.profileStatusEnabled') : t('mobile.v2.profileStatusDisabled') }}
          </el-tag>
        </el-form-item>

        <el-form-item :label="t('mobile.v2.profileCreateTime')">
          <span class="v2-profile-form__value v2-profile-form__value--small">{{ form.createTime }}</span>
        </el-form-item>

        <el-form-item :label="t('mobile.v2.profileUpdateTime')">
          <span class="v2-profile-form__value v2-profile-form__value--small">{{ form.updateTime }}</span>
        </el-form-item>
      </el-form>

      <div class="v2-profile-actions">
        <el-button type="primary" :loading="saving" @click="onSave">
          {{ t('mobile.v2.profileSave') }}
        </el-button>
        <el-button @click="onSwitchUser">
          {{ t('mobile.v2.profileSelectUser') }}
        </el-button>
      </div>
    </MobileCard>

    <el-button class="v2-profile-logout" @click="onLogout">
      {{ t('mobile.v2.profileLogout') }}
    </el-button>

    <el-dialog
      v-model="switchDialogVisible"
      :title="t('mobile.v2.profileSelectUser')"
      width="90%"
    >
      <div v-if="users.length === 0" class="v2-profile-empty">{{ t('mobile.v2.profileLoading') }}</div>
      <div v-else class="v2-profile-user-list">
        <div
          v-for="u in users"
          :key="u.id"
          class="v2-profile-user-item"
          :class="{ 'is-active': u.id === currentUserId }"
          @click="onSelectUser(u)"
        >
          <div class="v2-profile-user-item__avatar">
            <el-icon><UserFilled /></el-icon>
          </div>
          <div class="v2-profile-user-item__info">
            <div class="v2-profile-user-item__name">{{ u.nickname || u.username }}</div>
            <div class="v2-profile-user-item__meta">@{{ u.username }}</div>
          </div>
          <el-icon v-if="u.id === currentUserId" class="v2-profile-user-item__check" color="var(--wr-primary, #2563eb)"><Select /></el-icon>
        </div>
      </div>
    </el-dialog>
  </MobilePage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled, Select } from '@element-plus/icons-vue'
import MobilePage from '@/mobile/components/MobilePage.vue'
import MobileCard from '@/mobile/components/MobileCard.vue'
import { fetchUser, fetchUsers, updateUser } from '@/api/user'
import type { SysUser } from '@/api/types'

const router = useRouter()
const { t } = useI18n()

const currentUserId = ref(0)
const saving = ref(false)
const switchDialogVisible = ref(false)
const users = ref<SysUser[]>([])

const form = reactive({
  id: 0,
  username: '',
  nickname: '',
  status: '',
  createTime: '',
  updateTime: '',
})

let originalNickname = ''

async function loadProfile() {
  const raw = localStorage.getItem('user-id')
  const uid = raw ? Number(raw) : 0
  currentUserId.value = uid

  if (uid > 0) {
    try {
      const user = await fetchUser(uid)
      applyUser(user)
    } catch {
      await loadFirstUser()
    }
  } else {
    await loadFirstUser()
  }
}

function applyUser(user: SysUser) {
  form.id = user.id
  form.username = user.username
  form.nickname = user.nickname || ''
  form.status = user.status
  form.createTime = user.createTime || ''
  form.updateTime = user.updateTime || ''
  originalNickname = form.nickname

  currentUserId.value = user.id
  localStorage.setItem('user-id', String(user.id))
  if (user.nickname) {
    localStorage.setItem('user-nickname', user.nickname)
  }
}

async function loadFirstUser() {
  try {
    const result = await fetchUsers({ page: 1, pageSize: 1 })
    if (result.records.length > 0) {
      applyUser(result.records[0])
    }
  } catch {
    form.id = 0
    form.username = '—'
    form.nickname = ''
  }
}

async function onSave() {
  if (form.id === 0) {
    ElMessage.warning(t('mobile.v2.profileSelectUser'))
    return
  }
  const nickname = form.nickname.trim()
  if (nickname === originalNickname) {
    ElMessage.info(t('mobile.v2.profileNoChanges'))
    return
  }
  saving.value = true
  try {
    await updateUser(form.id, { nickname })
    originalNickname = nickname
    localStorage.setItem('user-nickname', nickname)
    ElMessage.success(t('mobile.v2.profileSaveSuccess'))
  } catch {
    ElMessage.error(t('mobile.v2.profileSaveFailed'))
  } finally {
    saving.value = false
  }
}

async function onSwitchUser() {
  try {
    const result = await fetchUsers({ page: 1, pageSize: 100 })
    users.value = result.records
  } catch {
    users.value = []
  }
  switchDialogVisible.value = true
}

function onSelectUser(user: SysUser) {
  applyUser(user)
  switchDialogVisible.value = false
  ElMessage.success(t('mobile.v2.profileSaveSuccess'))
}

async function onLogout() {
  try {
    await ElMessageBox.confirm(
      t('mobile.v2.profileLogoutConfirm'),
      t('app.confirmTitle'),
      { confirmButtonText: t('app.confirm'), cancelButtonText: t('app.cancel'), type: 'warning' },
    )
    localStorage.removeItem('user-id')
    localStorage.removeItem('user-nickname')
    router.push('/more')
    ElMessage.success(t('mobile.v2.profileSaveSuccess'))
  } catch {
  }
}

onMounted(() => {
  void loadProfile()
})
</script>

<style scoped lang="scss">
.v2-profile-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 16px 0 24px;

  &__circle {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 72px;
    height: 72px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--wr-stat-blue, #2563eb), #60a5fa);
    color: #fff;
  }

  &__name {
    font-size: 18px;
    font-weight: 700;
    color: var(--wr-text, #333333);
  }
}

.v2-profile-form {
  :deep(.el-form-item) {
    margin-bottom: 14px;
  }

  :deep(.el-form-item__label) {
    font-weight: 600;
    color: var(--wr-text-secondary, #666666);
  }
}

.v2-profile-form__value {
  font-size: 14px;
  color: var(--wr-text, #333333);

  &--small {
    font-size: 12px;
    color: var(--wr-muted, #999999);
  }
}

.v2-profile-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;

  .el-button {
    flex: 1;
  }
}

.v2-profile-logout {
  width: 100%;
  color: var(--el-color-danger);
  border-color: var(--el-color-danger);

  &:hover {
    background: var(--el-color-danger-light-9);
  }
}

.v2-profile-empty {
  padding: 24px;
  text-align: center;
  color: var(--wr-muted, #999999);
}

.v2-profile-user-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.v2-profile-user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.15s;

  &:hover {
    border-color: var(--wr-stat-blue, #2563eb);
  }

  &.is-active {
    border-color: var(--wr-stat-blue, #2563eb);
    background: var(--wr-index-bg, #eff6ff);
  }

  &__avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: #f0f0f0;
    color: #666;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 15px;
    font-weight: 600;
    color: var(--wr-text, #333333);
  }

  &__meta {
    margin-top: 2px;
    font-size: 12px;
    color: var(--wr-muted, #999999);
  }

  &__check {
    flex-shrink: 0;
  }
}
</style>
