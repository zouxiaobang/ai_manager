<template>
  <Teleport to="body">
    <Transition name="express-delete-confirm">
      <div
        v-if="deleteTarget"
        class="express-delete-confirm"
        @click.self="$emit('cancel')"
      >
        <SchemeADoodleFrame
          class="express-delete-confirm__panel"
          color="#cbd5e1"
          :shadow="false"
          :seed="42"
          role="alertdialog"
          aria-modal="true"
        >
          <div class="express-delete-confirm__inner">
            <div class="express-delete-confirm__title-row">
              <img class="express-delete-confirm__icon" :src="schemeAAssets.starBlue" alt="" />
              <h2 class="express-delete-confirm__title">删除快递</h2>
            </div>

            <p class="express-delete-confirm__message">
              确认删除 "{{ deleteTarget.name }}"？
            </p>

            <div class="express-delete-confirm__actions">
              <SchemeADoodleFrame
                tag="button"
                type="button"
                shape="pill"
                color="#cbd5e1"
                :shadow="false"
                class="express-delete-confirm__btn express-delete-confirm__btn--cancel"
                @click="$emit('cancel')"
              >
                取消
              </SchemeADoodleFrame>
              <SchemeADoodleFrame
                tag="button"
                type="button"
                shape="pill"
                color="#ef4444"
                :shadow="false"
                :sketch="true"
                class="express-delete-confirm__btn express-delete-confirm__btn--confirm"
                :class="{ 'is-disabled': deleting }"
                :disabled="deleting"
                @click="$emit('confirm')"
              >
                {{ deleting ? '删除中...' : '确认删除' }}
              </SchemeADoodleFrame>
            </div>
          </div>
        </SchemeADoodleFrame>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import type { EcExpressStation } from '@/api/ecommerce/express'

defineProps<{
  deleteTarget: EcExpressStation | null
  deleting: boolean
}>()

defineEmits<{
  cancel: []
  confirm: []
}>()
</script>

<style scoped lang="scss">
.express-delete-confirm {
  position: fixed;
  inset: 0;
  z-index: 210;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 20px;
  background: rgb(15 23 42 / 45%);
}

.express-delete-confirm__panel {
  width: min(100%, 320px);

  :deep(.sa-doodle-frame__body) {
    padding: 6px 6px 8px;
  }
}

.express-delete-confirm__inner {
  padding: 16px 14px 14px;
  font-family: 'ZCOOL KuaiLe', 'Alibaba PuHuiTi', 'PingFang SC', sans-serif;
  text-align: center;
}

.express-delete-confirm__title-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
}

.express-delete-confirm__icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.express-delete-confirm__title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
}

.express-delete-confirm__message {
  margin: 0 0 18px;
  font-size: 15px;
  line-height: 1.5;
  color: #64748b;
}

.express-delete-confirm__actions {
  display: flex;
  gap: 10px;
}

.express-delete-confirm__btn {
  flex: 1;
  padding: 0;
  border: none;
  background: transparent;
  font-family: inherit;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.14s ease;

  &:active:not(.is-disabled) {
    transform: scale(0.97);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 11px 10px;
    text-align: center;
  }

  &--cancel {
    color: #64748b;
  }

  &--confirm {
    background: #ef4444;
    color: #fff;

    :deep(.sa-doodle-frame__body) {
      position: relative;
      z-index: 3;
      color: #fff;
    }
  }

  &.is-disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.express-delete-confirm-enter-active,
.express-delete-confirm-leave-active {
  transition: opacity 0.2s ease;

  .express-delete-confirm__panel {
    transition: transform 0.22s ease, opacity 0.22s ease;
  }
}

.express-delete-confirm-enter-from,
.express-delete-confirm-leave-to {
  opacity: 0;

  .express-delete-confirm__panel {
    transform: scale(0.92);
    opacity: 0;
  }
}
</style>
