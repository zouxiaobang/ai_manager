<template>
  <V2Page>
    <div v-loading="factory.loading.value" class="v2-factory">
      <div class="v2-factory-poster">
        <div class="v2-factory-poster__gradient">
          <div class="v2-factory-poster__title-row">
            <svg class="v2-factory-poster__icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 8v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8"/><polyline points="4 8 12 3 20 8"/><line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
            <h2 class="v2-factory-poster__title">{{ factory.t('ecommerce.factory.pageTitle') }}</h2>
          </div>
        </div>
        <div class="v2-factory-poster__pill">
          <span class="v2-factory-poster__pill-item">
            <span class="v2-factory-poster__pill-dot" style="background: #f97316" />
            {{ factory.t('ecommerce.factory.factoryTypeProduction') }}
            <strong>{{ factory.stats.production }}</strong>
          </span>
          <span class="v2-factory-poster__pill-divider" />
          <span class="v2-factory-poster__pill-item">
            <span class="v2-factory-poster__pill-dot" style="background: #3b82f6" />
            {{ factory.t('ecommerce.factory.factoryTypeCustomer') }}
            <strong>{{ factory.stats.customer }}</strong>
          </span>
          <span class="v2-factory-poster__pill-divider" />
          <span class="v2-factory-poster__pill-item">
            <span class="v2-factory-poster__pill-dot" style="background: #8b5cf6" />
            {{ factory.t('mobile.factory.posterCartonLabel') }}
            <strong>{{ factory.stats.carton }}</strong>
          </span>
        </div>
      </div>

      <div class="v2-factory-body">
        <div class="v2-factory-search">
          <svg class="v2-factory-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input
            v-model="factory.searchQuery.value"
            class="v2-factory-search__input"
            :placeholder="factory.t('ecommerce.factory.searchPlaceholder')"
            type="search"
          />
        </div>

        <div class="v2-factory-tabs">
          <button
            v-for="opt in factory.filterOptions.value"
            :key="opt.value"
            type="button"
            class="v2-factory-tab"
            :class="{ 'is-active': factory.typeFilter.value === opt.value }"
            @click="factory.setTypeFilter(opt.value)"
          >
            {{ opt.label }}
          </button>
        </div>

        <div v-if="factory.records.value.length" class="v2-factory-grid">
          <div
            v-for="item in factory.records.value"
            :key="item.id"
            class="v2-factory-card"
            :class="{ 'v2-factory-card--disabled': item.status !== 'ENABLED' }"
          >
            <div class="v2-factory-card__head">
              <h3 class="v2-factory-card__name">{{ item.name }}</h3>
              <span
                class="v2-factory-card__status"
                :class="item.status === 'ENABLED' ? 'is-enabled' : 'is-disabled'"
              >
                {{
                  item.status === 'ENABLED'
                    ? factory.t('ecommerce.product.enabled')
                    : factory.t('ecommerce.product.disabled')
                }}
              </span>
            </div>

            <span
              class="v2-factory-card__type"
              :style="{
                color: factory.factoryTypeColor(item.factoryType),
                background: `${factory.factoryTypeColor(item.factoryType)}18`,
              }"
            >
              {{ factory.factoryTypeLabel(item.factoryType) }}
            </span>

            <p class="v2-factory-card__contact">{{ factory.factoryContactLine(item) }}</p>
            <p class="v2-factory-card__address">{{ item.address || '\u00a0' }}</p>

            <div class="v2-factory-card__actions">
              <button
                type="button"
                class="v2-factory-card__btn v2-factory-card__btn--edit"
                @click="factory.openEdit(item)"
              >
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                </svg>
                {{ factory.t('ecommerce.factory.edit') }}
              </button>
              <button
                type="button"
                class="v2-factory-card__btn v2-factory-card__btn--delete"
                @click="factory.onDelete(item)"
              >
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                </svg>
                {{ factory.t('ecommerce.factory.delete') }}
              </button>
            </div>
          </div>
        </div>

        <div v-else-if="!factory.loading.value" class="v2-factory-empty">
          {{ factory.t('mobile.factory.emptyList') }}
        </div>

        <button
          v-if="factory.hasMore.value"
          type="button"
          class="v2-factory-load-more"
          :disabled="factory.loading.value"
          @click="factory.loadMore()"
        >
          {{ factory.loading.value ? factory.t('mobile.factory.loadingMore') : factory.t('mobile.factory.loadMore') }}
        </button>
      </div>

      <button
        type="button"
        class="v2-factory-fab"
        :aria-label="factory.t('ecommerce.factory.add')"
        @click="factory.openCreate()"
      >
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
      </button>

      <V2FactoryFormSheet />
      <V2FactoryDeleteConfirm />
    </div>
  </V2Page>
</template>

<script setup lang="ts">
import { onMounted, provide } from 'vue'
import { MOBILE_FACTORY_KEY } from '@/mobile/views/factory/factoryContext'
import { useMobileFactory } from '@/mobile/views/factory/useMobileFactory'
import V2Page from '@/mobile-v2/components/V2Page.vue'
import V2FactoryFormSheet from '@/mobile-v2/views/factory/components/V2FactoryFormSheet.vue'
import V2FactoryDeleteConfirm from '@/mobile-v2/views/factory/components/V2FactoryDeleteConfirm.vue'

const factory = useMobileFactory()
provide(MOBILE_FACTORY_KEY, factory)

onMounted(() => {
  void factory.init()
})
</script>

<style scoped lang="scss">
.v2-factory {
  --v2-fac-orange: #f97316;
  --v2-fac-blue: #3b82f6;
  --v2-fac-purple: #8b5cf6;

  .v2-factory-poster {
    margin: calc(-1 * var(--wr-page-pad, 16px)) calc(-1 * var(--wr-page-pad, 16px)) 16px;
    position: relative;

    &__gradient {
      background: linear-gradient(135deg, #1e1b4b 0%, #312e81 40%, #3730a3 70%, #4f46e5 100%);
      padding: 48px 20px 64px;
      position: relative;
      overflow: hidden;

      &::before {
        content: '';
        position: absolute;
        inset: 0;
        background:
          radial-gradient(ellipse 300px 200px at 20% 30%, rgba(139, 92, 246, 0.25), transparent),
          radial-gradient(ellipse 250px 180px at 80% 20%, rgba(59, 130, 246, 0.2), transparent),
          radial-gradient(ellipse 200px 150px at 50% 80%, rgba(249, 115, 22, 0.15), transparent);
        pointer-events: none;
      }
    }

    &__back {
      position: absolute;
      z-index: 2;
      left: 12px;
      top: 12px;
      width: 34px;
      height: 34px;
      padding: 0;
      border: none;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.15);
      backdrop-filter: blur(4px);
      color: #fff;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: background 0.2s;

      &:active {
        background: rgba(255, 255, 255, 0.25);
      }
    }

    &__title-row {
      display: flex;
      align-items: center;
      gap: 10px;
      position: relative;
      z-index: 1;
    }

    &__icon {
      color: #a78bfa;
      flex-shrink: 0;
    }

    &__title {
      margin: 0;
      font-size: 22px;
      font-weight: 800;
      color: #fff;
      letter-spacing: 0.5px;
    }

    &__pill {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      margin: -40px 16px 0;
      padding: 10px 16px;
      background: #fff;
      border-radius: 999px;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
      position: relative;
      z-index: 2;
    }

    &__pill-item {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      font-weight: 600;
      color: #64748b;
      white-space: nowrap;

      strong {
        font-size: 14px;
        font-weight: 800;
        color: #1e293b;
        margin-left: 2px;
      }
    }

    &__pill-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      flex-shrink: 0;
    }

    &__pill-divider {
      width: 1px;
      height: 16px;
      background: #e2e8f0;
      flex-shrink: 0;
    }
  }

  .v2-factory-body {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .v2-factory-search {
    position: relative;

    &__input {
      width: 100%;
      padding: 10px 12px 10px 36px;
      border: 1px solid var(--wr-border, #e8ecef);
      border-radius: 8px;
      font-size: 14px;
      font-family: inherit;
      color: var(--wr-text, #333);
      background: var(--wr-card, #fff);
      outline: none;
      box-sizing: border-box;
      transition: border-color 0.2s;

      &::placeholder {
        color: var(--wr-muted, #999);
      }

      &:focus {
        border-color: var(--v2-fac-purple);
      }
    }

    &__icon {
      position: absolute;
      left: 10px;
      top: 50%;
      transform: translateY(-50%);
      width: 16px;
      height: 16px;
      color: var(--wr-muted, #999);
    }
  }

  .v2-factory-tabs {
    display: flex;
    gap: 6px;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }

  .v2-factory-tab {
    flex-shrink: 0;
    padding: 6px 14px;
    border-radius: 999px;
    font-size: 13px;
    font-weight: 500;
    border: 1px solid var(--wr-border, #e8ecef);
    background: var(--wr-card, #fff);
    color: var(--wr-text-secondary, #666);
    cursor: pointer;
    transition: all 0.2s;
    white-space: nowrap;
    font-family: inherit;

    &.is-active {
      border-color: var(--v2-fac-purple);
      color: var(--v2-fac-purple);
      font-weight: 600;
      background: #f5f3ff;
    }
  }

  .v2-factory-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .v2-factory-card {
    display: flex;
    flex-direction: column;
    padding: 12px;
    background: var(--wr-card, #fff);
    border: 1px solid var(--wr-border, #e8ecef);
    border-radius: 12px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
    transition: transform 0.15s, box-shadow 0.2s;
    min-width: 0;

    &:active {
      transform: scale(0.97);
    }

    &--disabled {
      opacity: 0.65;
    }

    &__head {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 3px;
      width: 100%;
      margin-bottom: 6px;
    }

    &__name {
      margin: 0;
      width: 100%;
      font-size: 14px;
      font-weight: 800;
      color: var(--wr-text, #333);
      line-height: 1.25;
      text-align: center;
      display: -webkit-box;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 2;
      overflow: hidden;
    }

    &__status {
      flex-shrink: 0;
      font-size: 10px;
      font-weight: 800;
      line-height: 1.2;

      &.is-enabled {
        color: #16a34a;
      }

      &.is-disabled {
        color: #94a3b8;
      }
    }

    &__type {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      max-width: 100%;
      margin: 0 auto 6px;
      padding: 2px 8px;
      border-radius: 999px;
      font-size: 10px;
      font-weight: 800;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      line-height: 1.5;
    }

    &__contact {
      margin: 0 0 3px;
      width: 100%;
      font-size: 11px;
      font-weight: 600;
      color: var(--wr-text-secondary, #666);
      text-align: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &__address {
      margin: 0 0 8px;
      width: 100%;
      min-height: 13px;
      font-size: 10px;
      line-height: 1.4;
      color: var(--wr-muted, #999);
      text-align: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &__actions {
      display: flex;
      gap: 5px;
      width: 100%;
      justify-content: center;
      margin-top: auto;
    }

    &__btn {
      flex: 1;
      min-width: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 3px;
      padding: 5px 4px;
      border: 1px solid var(--wr-border, #e8ecef);
      border-radius: 6px;
      background: var(--wr-card, #fff);
      font-family: inherit;
      font-size: 11px;
      font-weight: 800;
      cursor: pointer;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      transition: background 0.15s, color 0.15s;

      svg {
        flex-shrink: 0;
      }

      &--edit {
        color: #8b5cf6;

        &:active {
          background: #f5f3ff;
        }
      }

      &--delete {
        color: #ef4444;

        &:active {
          background: #fef2f2;
        }
      }
    }
  }

  .v2-factory-empty {
    padding: 40px 20px;
    text-align: center;
    font-size: 14px;
    color: var(--wr-muted, #999);
  }

  .v2-factory-load-more {
    display: block;
    width: 100%;
    padding: 12px;
    font-size: 14px;
    font-weight: 700;
    color: var(--v2-fac-purple);
    background: var(--wr-card, #fff);
    border: 1px solid var(--wr-border, #e8ecef);
    border-radius: 10px;
    cursor: pointer;
    transition: background 0.2s;
    font-family: inherit;

    &:active:not(:disabled) {
      background: #f5f3ff;
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }

  .v2-factory-fab {
    position: fixed;
    bottom: 24px;
    right: 16px;
    z-index: 100;
    width: 56px;
    height: 56px;
    border: none;
    border-radius: 50%;
    background: #7c3aed;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    box-shadow: 0 4px 16px rgba(124, 58, 237, 0.4);
    transition: transform 0.2s ease, box-shadow 0.2s;
    padding: 0;

    &:active {
      transform: scale(1.05);
      box-shadow: 0 6px 20px rgba(124, 58, 237, 0.5);
    }
  }
}
</style>
