<template>
  <Teleport to="body">
    <Transition name="mfc-form-sheet">
      <div v-if="factory.formVisible.value" class="mfc-form-sheet" @click.self="close">
        <div class="mfc-form-sheet__panel" role="dialog" aria-modal="true">
          <header class="mfc-form-sheet__header">
            <div class="mfc-form-sheet__header-main">
              <div class="mfc-form-sheet__title-row">
                <span class="mfc-form-sheet__title-icon">⭐</span>
                <h2 class="mfc-form-sheet__title">
                  {{
                    factory.editingId.value
                      ? factory.t('ecommerce.factory.editTitle')
                      : factory.t('ecommerce.factory.createTitle')
                  }}
                </h2>
              </div>
              <p class="mfc-form-sheet__subtitle">{{ factory.t('ecommerce.factory.formSubtitle') }}</p>
            </div>
            <div
              class="mfc-form-sheet__close"
              aria-label="关闭"
              @click="close"
            >
              <span class="mfc-form-sheet__close-icon">×</span>
            </div>
          </header>

          <div class="mfc-form-sheet__body">
            <div class="mfc-form-sheet__type-cards">
              <div
                v-for="typeOpt in typeOptions"
                :key="typeOpt.value"
                class="mfc-form-sheet__type-card"
                :class="[
                  `is-${typeOpt.value.toLowerCase()}`,
                  { 'is-active': factory.form.factoryType === typeOpt.value },
                ]"
                @click="factory.form.factoryType = typeOpt.value"
              >
                <div class="mfc-form-sheet__type-card-inner">
                  <img :src="typeOpt.icon" alt="" />
                  <span>{{ typeOpt.label }}</span>
                </div>
              </div>
            </div>

            <label class="mfc-form-sheet__field mfc-form-sheet__field--full">
              <span class="mfc-form-sheet__label">{{ factory.t('ecommerce.factory.name') }} *</span>
              <div class="mfc-form-sheet__input-frame">
                <input v-model="factory.form.name" type="text" class="mfc-form-sheet__input" />
              </div>
            </label>

            <div class="mfc-form-sheet__field-row">
              <label class="mfc-form-sheet__field">
                <span class="mfc-form-sheet__label">{{ factory.t('ecommerce.factory.contactName') }}</span>
                <div class="mfc-form-sheet__input-frame">
                  <input v-model="factory.form.contactName" type="text" class="mfc-form-sheet__input" />
                </div>
              </label>
              <label class="mfc-form-sheet__field">
                <span class="mfc-form-sheet__label">{{ factory.t('ecommerce.factory.contactPhone') }}</span>
                <div class="mfc-form-sheet__input-frame">
                  <input v-model="factory.form.contactPhone" type="tel" class="mfc-form-sheet__input" />
                </div>
              </label>
            </div>

            <label class="mfc-form-sheet__field mfc-form-sheet__field--full">
              <span class="mfc-form-sheet__label">{{ factory.t('ecommerce.factory.address') }}</span>
              <div class="mfc-form-sheet__input-frame">
                <textarea v-model="factory.form.address" rows="2" class="mfc-form-sheet__textarea" />
              </div>
            </label>

            <label class="mfc-form-sheet__field mfc-form-sheet__field--full">
              <span class="mfc-form-sheet__label">{{ factory.t('ecommerce.factory.remark') }}</span>
              <div class="mfc-form-sheet__input-frame">
                <textarea v-model="factory.form.remark" rows="2" class="mfc-form-sheet__textarea" />
              </div>
            </label>

            <div class="mfc-form-sheet__status">
              <span class="mfc-form-sheet__label">{{ factory.t('ecommerce.factory.status') }}</span>
              <div class="mfc-form-sheet__status-toggle">
                <div
                  class="mfc-form-sheet__status-btn"
                  :class="{ 'is-active': factory.form.status === 'ENABLED' }"
                  @click="factory.form.status = 'ENABLED'"
                >
                  {{ factory.t('ecommerce.product.enabled') }}
                </div>
                <div
                  class="mfc-form-sheet__status-btn"
                  :class="{ 'is-active': factory.form.status === 'DISABLED' }"
                  @click="factory.form.status = 'DISABLED'"
                >
                  {{ factory.t('ecommerce.product.disabled') }}
                </div>
              </div>
            </div>

            <div
              class="mfc-form-sheet__submit"
              :class="{ 'is-disabled': factory.saving.value }"
              @click="factory.onSave()"
            >
              {{ factory.t('ecommerce.common.save') }}
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { MOBILE_FACTORY_KEY } from '@/mobile/views/factory/factoryContext'
import type { EcFactoryType } from '@/api/ecommerce/factory'

const factory = inject(MOBILE_FACTORY_KEY)!

const ecommerceSvg = `data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='28' height='28' viewBox='0 0 24 24' fill='none' stroke='%233b82f6' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='9' cy='21' r='1'/%3E%3Ccircle cx='20' cy='21' r='1'/%3E%3Cpath d='M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6'/%3E%3C/svg%3E`

const typeOptions = computed(() => [
  {
    value: 'PRODUCTION' as EcFactoryType,
    label: factory.t('ecommerce.factory.statProduction'),
    icon: `${import.meta.env.BASE_URL}mobile-home/scheme-a/icon-factory.svg`,
    color: '#f97316',
    seed: 11,
  },
  {
    value: 'CUSTOMER' as EcFactoryType,
    label: factory.t('ecommerce.factory.statCustomer'),
    icon: ecommerceSvg,
    color: '#3b82f6',
    seed: 22,
  },
  {
    value: 'CARTON' as EcFactoryType,
    label: factory.t('ecommerce.factory.statCarton'),
    icon: `${import.meta.env.BASE_URL}mobile-home/scheme-a/icon-box.svg`,
    color: '#8b5cf6',
    seed: 33,
  },
])

function close() {
  factory.formVisible.value = false
}
</script>

<style scoped lang="scss">
.mfc-form-sheet {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: flex-end;
  background: rgb(15 23 42 / 45%);
}

.mfc-form-sheet__panel {
  width: 100%;
  max-height: 92dvh;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 20px 20px 0 0;
  overflow: hidden;
  font-family: 'ZCOOL KuaiLe', 'Alibaba PuHuiTi', 'PingFang SC', sans-serif;
  color: #1e293b;
}

.mfc-form-sheet__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 16px 12px;
  border-bottom: 2px dashed #e2e8f0;
  flex-shrink: 0;
}

.mfc-form-sheet__header-main {
  flex: 1;
  min-width: 0;
}

.mfc-form-sheet__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.mfc-form-sheet__title-icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  font-size: 22px;
  line-height: 1;
}

.mfc-form-sheet__title {
  font-size: 20px;
  color: #1e293b;
  margin: 0;
  font-weight: 800;
}

.mfc-form-sheet__subtitle {
  font-size: 12px;
  color: #64748b;
  margin: 0;
  line-height: 1.4;
}

.mfc-form-sheet__close {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
}

.mfc-form-sheet__close-icon {
  font-size: 22px;
  line-height: 1;
  color: #64748b;
}

.mfc-form-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px max(20px, env(safe-area-inset-bottom));
}

.mfc-form-sheet__type-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 16px;
}

.mfc-form-sheet__type-card {
  padding: 4px 3px 6px;
  border: none;
  background: transparent;
  font-family: inherit;
  cursor: pointer;
  text-align: center;
  transition: transform 0.14s ease;
  height: 100%;
  box-sizing: border-box;

  &:active {
    transform: scale(0.97);
  }

  &.is-active {
    .mfc-form-sheet__type-card-inner {
      background: rgb(139 92 246 / 6%);
    }
  }

  &.is-active.is-production .mfc-form-sheet__type-card-inner {
    background: #fff7ed;
    color: #c2410c;
  }

  &.is-active.is-customer .mfc-form-sheet__type-card-inner {
    background: #eff6ff;
    color: #1d4ed8;
  }

  &.is-active.is-carton .mfc-form-sheet__type-card-inner {
    background: #f5f3ff;
    color: #6d28d9;
  }
}

.mfc-form-sheet__type-card-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 6px 8px;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;

  img {
    width: 28px;
    height: 28px;
  }
}

.mfc-form-sheet__field-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.mfc-form-sheet__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;

  &--full {
    width: 100%;
  }
}

.mfc-form-sheet__label {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}

.mfc-form-sheet__input-frame {
  padding: 4px 6px 6px;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  transition: border-color 0.15s;

  &:focus-within {
    border-color: #8b5cf6;
  }
}

.mfc-form-sheet__input,
.mfc-form-sheet__textarea {
  width: 100%;
  border: none;
  outline: none;
  padding: 8px 6px 8px 18px;
  font-family: inherit;
  font-size: 16px;
  color: #1e293b;
  background: transparent;
  resize: vertical;
}

.mfc-form-sheet__textarea {
  min-height: 52px;
  line-height: 1.4;
}

.mfc-form-sheet__status {
  margin-bottom: 16px;
}

.mfc-form-sheet__status-toggle {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}

.mfc-form-sheet__status-btn {
  flex: 1;
  padding: 10px 8px;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  font-family: inherit;
  font-size: 14px;
  font-weight: 700;
  color: #64748b;
  cursor: pointer;
  text-align: center;
  transition: transform 0.14s ease, border-color 0.15s, background 0.15s, color 0.15s;

  &:active {
    transform: scale(0.97);
  }

  &.is-active {
    border-color: #8b5cf6;
    background: #f5f3ff;
    color: #6d28d9;
  }

  &:not(.is-active) {
    opacity: 0.85;
  }
}

.mfc-form-sheet__submit {
  width: 100%;
  padding: 14px 16px;
  border: none;
  background: #8b5cf6;
  font-family: inherit;
  font-size: 16px;
  font-weight: 800;
  color: #fff;
  text-align: center;
  cursor: pointer;
  transition: transform 0.14s ease;
  position: relative;
  z-index: 3;

  &:active:not(.is-disabled) {
    transform: scale(0.98);
  }

  &.is-disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.mfc-form-sheet-enter-active,
.mfc-form-sheet-leave-active {
  transition: opacity 0.2s ease;

  .mfc-form-sheet__panel {
    transition: transform 0.25s ease;
  }
}

.mfc-form-sheet-enter-from,
.mfc-form-sheet-leave-to {
  opacity: 0;

  .mfc-form-sheet__panel {
    transform: translateY(100%);
  }
}
</style>
