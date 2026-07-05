<template>
  <Teleport to="body">
    <Transition name="mfc-delete-confirm">
      <div
        v-if="factory.deleteTarget.value"
        class="mfc-delete-confirm"
        @click.self="factory.cancelDelete()"
      >
        <SchemeADoodleFrame
          class="mfc-delete-confirm__panel"
          color="#cbd5e1"
          :shadow="false"
          :seed="42"
          role="alertdialog"
          aria-modal="true"
        >
          <div class="mfc-delete-confirm__inner">
            <div class="mfc-delete-confirm__title-row">
              <img class="mfc-delete-confirm__icon" :src="schemeAAssets.starBlue" alt="" />
              <h2 class="mfc-delete-confirm__title">{{ factory.t('ecommerce.factory.delete') }}</h2>
            </div>

            <p class="mfc-delete-confirm__message">
              {{
                factory.t('ecommerce.factory.deleteConfirm', {
                  name: factory.deleteTarget.value?.name ?? '',
                })
              }}
            </p>

            <div class="mfc-delete-confirm__actions">
              <SchemeADoodleFrame
                tag="button"
                type="button"
                shape="pill"
                color="#cbd5e1"
                :shadow="false"
                class="mfc-delete-confirm__btn mfc-delete-confirm__btn--cancel"
                @click="factory.cancelDelete()"
              >
                {{ factory.t('ecommerce.common.cancel') }}
              </SchemeADoodleFrame>
              <SchemeADoodleFrame
                tag="button"
                type="button"
                shape="pill"
                color="#ef4444"
                :shadow="false"
                :sketch="true"
                class="mfc-delete-confirm__btn mfc-delete-confirm__btn--confirm"
                :class="{ 'is-disabled': factory.deleting.value }"
                :disabled="factory.deleting.value"
                @click="factory.confirmDelete()"
              >
                {{ factory.t('ecommerce.common.confirm') }}
              </SchemeADoodleFrame>
            </div>
          </div>
        </SchemeADoodleFrame>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_FACTORY_KEY } from '../factoryContext'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'

const factory = inject(MOBILE_FACTORY_KEY)!
</script>

<style scoped lang="scss">
.mfc-delete-confirm {
  position: fixed;
  inset: 0;
  z-index: 210;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 20px;
  background: rgb(15 23 42 / 45%);
}

.mfc-delete-confirm__panel {
  width: min(100%, 320px);

  :deep(.sa-doodle-frame__body) {
    padding: 6px 6px 8px;
  }
}

.mfc-delete-confirm__inner {
  padding: 16px 14px 14px;
  font-family: 'ZCOOL KuaiLe', 'Alibaba PuHuiTi', 'PingFang SC', sans-serif;
  text-align: center;
}

.mfc-delete-confirm__title-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
}

.mfc-delete-confirm__icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.mfc-delete-confirm__title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
}

.mfc-delete-confirm__message {
  margin: 0 0 18px;
  font-size: 15px;
  line-height: 1.5;
  color: #64748b;
}

.mfc-delete-confirm__actions {
  display: flex;
  gap: 10px;
}

.mfc-delete-confirm__btn {
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

.mfc-delete-confirm-enter-active,
.mfc-delete-confirm-leave-active {
  transition: opacity 0.2s ease;

  .mfc-delete-confirm__panel {
    transition: transform 0.22s ease, opacity 0.22s ease;
  }
}

.mfc-delete-confirm-enter-from,
.mfc-delete-confirm-leave-to {
  opacity: 0;

  .mfc-delete-confirm__panel {
    transform: scale(0.92);
    opacity: 0;
  }
}
</style>
