<template>
  <el-dialog
    v-model="visible"
    :title="platformId ? t('ecommerce.platform.editTitle') : t('ecommerce.platform.createTitle')"
    width="760px"
    destroy-on-close
    top="4vh"
    append-to-body
    class="platform-form-dialog"
    @open="onOpen"
  >
    <div class="platform-form-dialog__layout">
      <aside class="platform-form-dialog__left">
        <section class="platform-form-dialog__preview-card">
          <EcImageField
            v-model="form.avatarUrl"
            size="large"
            class="platform-form-dialog__avatar"
            :dialog-title="t('ecommerce.platform.avatar')"
            :fallback-src="platformId ? presetPlatformIcon : ''"
          />
          <div class="platform-form-dialog__preview-name">
            {{ form.name.trim() || t('ecommerce.platform.namePlaceholder') }}
          </div>
          <div class="platform-form-dialog__preview-tags">
            <el-tag
              size="small"
              :type="form.channelType === 'ONLINE' ? 'danger' : 'success'"
              class="platform-form-dialog__preview-tag"
            >
              {{ form.channelType === 'ONLINE' ? t('ecommerce.platform.online') : t('ecommerce.platform.offline') }}
            </el-tag>
            <el-tag
              size="small"
              :type="form.status === 'ENABLED' ? 'success' : 'info'"
              class="platform-form-dialog__preview-tag"
            >
              {{ form.status === 'ENABLED' ? t('ecommerce.product.enabled') : t('ecommerce.product.disabled') }}
            </el-tag>
          </div>
          <p class="platform-form-dialog__preview-hint">{{ t('ecommerce.platform.avatarHint') }}</p>
        </section>
      </aside>

      <div class="platform-form-dialog__right">
        <el-form :model="form" label-position="top" class="platform-form-dialog__form">
          <el-form-item :label="t('ecommerce.platform.name')" required>
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item :label="t('ecommerce.platform.nameEn')">
            <el-input v-model="form.nameEn" />
          </el-form-item>
          <el-form-item :label="t('ecommerce.platform.channelType')" required>
            <div class="platform-form-dialog__channel-cards">
              <button
                type="button"
                class="platform-form-dialog__channel-card is-online"
                :class="{ 'is-active': form.channelType === 'ONLINE' }"
                @click="form.channelType = 'ONLINE'"
              >
                <span class="platform-form-dialog__channel-check" aria-hidden="true">
                  <el-icon v-if="form.channelType === 'ONLINE'"><Check /></el-icon>
                </span>
                <span class="platform-form-dialog__channel-icon-wrap">
                  <el-icon class="platform-form-dialog__channel-icon"><Monitor /></el-icon>
                </span>
                <span class="platform-form-dialog__channel-text">
                  <span class="platform-form-dialog__channel-title">{{ t('ecommerce.platform.online') }}</span>
                  <span class="platform-form-dialog__channel-desc">{{ t('ecommerce.platform.onlineDesc') }}</span>
                </span>
              </button>
              <button
                type="button"
                class="platform-form-dialog__channel-card is-offline"
                :class="{ 'is-active': form.channelType === 'OFFLINE' }"
                @click="form.channelType = 'OFFLINE'"
              >
                <span class="platform-form-dialog__channel-check" aria-hidden="true">
                  <el-icon v-if="form.channelType === 'OFFLINE'"><Check /></el-icon>
                </span>
                <span class="platform-form-dialog__channel-icon-wrap">
                  <el-icon class="platform-form-dialog__channel-icon"><OfficeBuilding /></el-icon>
                </span>
                <span class="platform-form-dialog__channel-text">
                  <span class="platform-form-dialog__channel-title">{{ t('ecommerce.platform.offline') }}</span>
                  <span class="platform-form-dialog__channel-desc">{{ t('ecommerce.platform.offlineDesc') }}</span>
                </span>
              </button>
            </div>
          </el-form-item>
          <el-form-item :label="t('ecommerce.platform.remark')">
            <el-input v-model="form.remark" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item :label="t('ecommerce.platform.status')">
            <div class="platform-form-dialog__status-toggle">
              <button
                type="button"
                class="platform-form-dialog__status-option"
                :class="{ 'is-active': form.status === 'ENABLED' }"
                @click="form.status = 'ENABLED'"
              >
                {{ t('ecommerce.product.enabled') }}
              </button>
              <button
                type="button"
                class="platform-form-dialog__status-option"
                :class="{ 'is-active': form.status === 'DISABLED' }"
                @click="form.status = 'DISABLED'"
              >
                {{ t('ecommerce.product.disabled') }}
              </button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <template #footer>
      <el-button
        type="primary"
        size="large"
        class="platform-form-dialog__save"
        :loading="saving"
        @click="onSave"
      >
        {{ t('ecommerce.common.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Check, Monitor, OfficeBuilding } from '@element-plus/icons-vue'
import EcImageField from '@/components/ecommerce/EcImageField.vue'
import {
  createPlatform,
  updatePlatform,
  type EcPlatform,
  type EcPlatformSaveRequest,
} from '@/api/ecommerce/platform'
import { resolvePlatformIcon } from '@/utils/platformVisual'

const props = defineProps<{
  platform?: EcPlatform | null
}>()

const visible = defineModel<boolean>({ default: false })
const emit = defineEmits<{ saved: [] }>()

const { t } = useI18n()

const saving = ref(false)
const platformId = ref<number | null>(null)

const form = reactive<EcPlatformSaveRequest>({
  name: '',
  nameEn: '',
  avatarUrl: '',
  platformCode: 99,
  channelType: 'ONLINE',
  remark: '',
  status: 'ENABLED',
})

const presetPlatformIcon = computed(() => resolvePlatformIcon(undefined, form.platformCode))

watch(
  () => form.avatarUrl,
  (url) => {
    if (url?.trim()) {
      form.platformCode = 99
    }
  },
)

function resetForm() {
  Object.assign(form, {
    name: '',
    nameEn: '',
    avatarUrl: '',
    platformCode: 99,
    channelType: 'ONLINE',
    remark: '',
    status: 'ENABLED',
  })
}

function onOpen() {
  platformId.value = props.platform?.id ?? null
  if (props.platform) {
    Object.assign(form, {
      name: props.platform.name,
      nameEn: props.platform.nameEn || '',
      avatarUrl: props.platform.avatarUrl ?? '',
      platformCode: props.platform.platformCode,
      channelType: props.platform.channelType,
      remark: props.platform.remark || '',
      status: props.platform.status,
    })
  } else {
    resetForm()
  }
}

async function onSave() {
  if (!form.name.trim()) {
    ElMessage.warning(t('ecommerce.platform.nameRequired'))
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      name: form.name.trim(),
      nameEn: form.nameEn?.trim() || undefined,
      avatarUrl: form.avatarUrl?.trim() || undefined,
    }
    if (platformId.value) {
      await updatePlatform(platformId.value, payload)
    } else {
      await createPlatform(payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.platform-form-dialog {
  :deep(.el-dialog) {
    display: flex;
    flex-direction: column;
    max-height: 90vh;
    margin-bottom: 0;
  }

  :deep(.el-dialog__header) {
    flex-shrink: 0;
    margin-right: 0;
  }

  :deep(.el-dialog__body) {
    flex: 1;
    min-height: 0;
    overflow: hidden;
    padding-top: 8px;
  }

  :deep(.el-dialog__footer) {
    flex-shrink: 0;
    padding-top: 12px;
    border-top: 1px solid var(--wr-border, #e8ecef);
    display: flex;
    justify-content: center;
  }
}

.platform-form-dialog__save {
  min-width: 200px;
  height: 44px;
  padding: 0 32px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
}

.platform-form-dialog__layout {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
  height: 100%;
  max-height: min(68vh, 520px);
  min-height: 300px;
}

.platform-form-dialog__left {
  min-width: 0;
  align-self: start;
}

.platform-form-dialog__preview-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 16px;
  border-radius: 12px;
  background: var(--wr-bg, #f9f9fa);
  border: 1px solid var(--wr-border, #e8ecef);
  text-align: center;
}

.platform-form-dialog__avatar {
  :deep(.ec-image-field) {
    align-items: center;
  }

  :deep(.ec-image-field__preview) {
    width: 112px;
    height: 112px;
    border-radius: 16px;
  }
}

.platform-form-dialog__preview-name {
  margin-top: 14px;
  font-size: 15px;
  font-weight: 600;
  color: var(--wr-text, #333);
  line-height: 1.4;
  word-break: break-all;
}

.platform-form-dialog__preview-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
  margin-top: 10px;
}

.platform-form-dialog__preview-tag {
  border: none;
}

.platform-form-dialog__preview-hint {
  margin: 12px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--wr-muted, #999);
}

.platform-form-dialog__right {
  min-width: 0;
  height: 100%;
  max-height: min(68vh, 560px);
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 6px;
  padding-bottom: 8px;
  scrollbar-width: thin;
}

.platform-form-dialog__form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
  }
}

.platform-form-dialog__channel-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

.platform-form-dialog__channel-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 88px;
  padding: 16px 40px 16px 16px;
  border: 2px solid var(--wr-border, #e8ecef);
  border-radius: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;

  &:hover {
    border-color: #cbd5e1;
  }

  &.is-online.is-active {
    border-color: #dc2626;
    box-shadow: 0 0 0 1px rgb(220 38 38 / 8%);
  }

  &.is-offline.is-active {
    border-color: #16a34a;
    box-shadow: 0 0 0 1px rgb(22 163 74 / 8%);
  }
}

.platform-form-dialog__channel-check {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: 2px solid #d1d5db;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  background: transparent;

  .platform-form-dialog__channel-card.is-active.is-online & {
    border-color: #dc2626;
    background: #dc2626;
  }

  .platform-form-dialog__channel-card.is-active.is-offline & {
    border-color: #16a34a;
    background: #16a34a;
  }
}

.platform-form-dialog__channel-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  flex-shrink: 0;
  background: #d1d5db;
  color: #fff;

  .platform-form-dialog__channel-card.is-online.is-active & {
    background: #dc2626;
  }

  .platform-form-dialog__channel-card.is-offline.is-active & {
    background: #16a34a;
  }
}

.platform-form-dialog__channel-icon {
  font-size: 22px;
}

.platform-form-dialog__channel-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.platform-form-dialog__channel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--wr-text, #333);
  line-height: 1.3;
}

.platform-form-dialog__channel-desc {
  font-size: 12px;
  line-height: 1.45;
  color: var(--wr-muted, #999);
}

.platform-form-dialog__status-toggle {
  display: inline-flex;
  align-items: center;
  padding: 3px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #fff;
}

.platform-form-dialog__status-option {
  min-width: 76px;
  padding: 6px 22px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--wr-text, #333);
  font-size: 14px;
  line-height: 1.4;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;

  &.is-active {
    background: var(--wr-stat-green, #16a34a);
    color: #fff;
  }

  &:not(.is-active):hover {
    color: var(--wr-text-secondary, #666);
  }
}

@media (max-width: 768px) {
  .platform-form-dialog__layout {
    grid-template-columns: 1fr;
    max-height: min(75vh, 640px);
  }

  .platform-form-dialog__right {
    max-height: min(52vh, 480px);
  }

  .platform-form-dialog__channel-cards {
    grid-template-columns: 1fr;
  }
}
</style>
