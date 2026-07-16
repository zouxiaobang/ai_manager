<template>
  <V2Page>
    <div v-loading="factory.loading.value" class="v2-ec">
      <div class="v2-ec-stats">
        <div class="v2-ec-stat-card" style="background: #fff7ed;" @click="factory.onStatTypeClick('PRODUCTION')">
          <div class="v2-ec-stat-card__icon" style="background: #ea580c; color: #fff;">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 8v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8"/><polyline points="4 8 12 3 20 8"/><line x1="12" y1="3" x2="12" y2="15"/><line x1="8" y1="11" x2="16" y2="11"/>
            </svg>
          </div>
          <div class="v2-ec-stat-card__info">
            <div class="v2-ec-stat-card__value" style="color: #ea580c;">{{ factory.stats.production }}</div>
            <div class="v2-ec-stat-card__label">{{ factory.t('ecommerce.factory.factoryTypeProduction') }}</div>
          </div>
        </div>
        <div class="v2-ec-stat-card" style="background: #eff6ff;" @click="factory.onStatTypeClick('CUSTOMER')">
          <div class="v2-ec-stat-card__icon" style="background: #2563eb; color: #fff;">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
          </div>
          <div class="v2-ec-stat-card__info">
            <div class="v2-ec-stat-card__value" style="color: #2563eb;">{{ factory.stats.customer }}</div>
            <div class="v2-ec-stat-card__label">{{ factory.t('ecommerce.factory.factoryTypeCustomer') }}</div>
          </div>
        </div>
        <div class="v2-ec-stat-card" style="background: #f5f3ff;" @click="factory.onStatTypeClick('CARTON')">
          <div class="v2-ec-stat-card__icon" style="background: #7c3aed; color: #fff;">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/>
            </svg>
          </div>
          <div class="v2-ec-stat-card__info">
            <div class="v2-ec-stat-card__value" style="color: #7c3aed;">{{ factory.stats.carton }}</div>
            <div class="v2-ec-stat-card__label">{{ factory.t('ecommerce.factory.factoryTypeCarton') }}</div>
          </div>
        </div>
        <div class="v2-ec-stat-card" style="background: #f0fdf4;">
          <div class="v2-ec-stat-card__icon" style="background: #16a34a; color: #fff;">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
          <div class="v2-ec-stat-card__info">
            <div class="v2-ec-stat-card__value" style="color: #16a34a;">{{ factory.stats.enabled }}</div>
            <div class="v2-ec-stat-card__label">{{ factory.t('ecommerce.common.enabled') }}</div>
          </div>
        </div>
      </div>

      <div class="v2-ec-search">
        <svg class="v2-ec-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input
          v-model="factory.searchQuery.value"
          class="v2-ec-search__input"
          :placeholder="factory.t('ecommerce.factory.searchPlaceholder')"
          type="search"
        />
      </div>

      <div class="v2-ec-tabs">
        <button
          v-for="opt in factory.filterOptions.value"
          :key="opt.value"
          type="button"
          class="v2-ec-tab"
          :class="{ 'is-active': factory.typeFilter.value === opt.value }"
          @click="factory.setTypeFilter(opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>

      <div v-if="factory.records.value.length" class="v2-ec-factory-grid">
        <div
          v-for="item in factory.records.value"
          :key="item.id"
          class="v2-ec-factory-card"
          :class="{ 'v2-ec-factory-card--disabled': item.status !== 'ENABLED' }"
        >
          <div class="v2-ec-factory-card__inner">
            <div class="v2-ec-factory-card__header">
              <div class="v2-ec-factory-card__name">{{ item.name }}</div>
              <div class="v2-ec-factory-card__actions">
                <button
                  type="button"
                  class="v2-ec-factory-card__action-btn"
                  :aria-label="factory.t('ecommerce.common.edit')"
                  @click.stop="factory.openEdit(item)"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                </button>
                <button
                  type="button"
                  class="v2-ec-factory-card__action-btn v2-ec-factory-card__action-btn--danger"
                  :aria-label="factory.t('ecommerce.common.delete')"
                  @click.stop="factory.onDelete(item)"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                  </svg>
                </button>
              </div>
            </div>
            <div class="v2-ec-factory-card__type">
              <span
                class="v2-ec-factory-card__type-dot"
                :style="{ background: factory.factoryTypeColor(item.factoryType) }"
              />
              {{ factory.factoryTypeLabel(item.factoryType) }}
            </div>
            <div class="v2-ec-factory-card__contact">{{ factory.factoryContactLine(item) }}</div>
            <div class="v2-ec-factory-card__address">{{ item.address || factory.t('mobile.factory.noAddress') }}</div>
          </div>
        </div>
      </div>
      <div v-else class="v2-ec-empty">{{ factory.t('mobile.factory.emptyText') }}</div>

      <button
        type="button"
        class="v2-ec-factory-fab"
        :aria-label="factory.t('ecommerce.factory.add')"
        @click="factory.openCreate()"
      >
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
      </button>

      <FactoryFormSheet />
      <FactoryDeleteConfirm />
    </div>
  </V2Page>
</template>

<script setup lang="ts">
import { onMounted, provide } from 'vue'
import { MOBILE_FACTORY_KEY } from '@/mobile/views/factory/factoryContext'
import { useMobileFactory } from '@/mobile/views/factory/useMobileFactory'
import V2Page from '@/mobile-v2/components/V2Page.vue'
import FactoryFormSheet from '@/mobile/views/factory/components/FactoryFormSheet.vue'
import FactoryDeleteConfirm from '@/mobile/views/factory/components/FactoryDeleteConfirm.vue'

import './styles/v2-ecommerce.scss'

const factory = useMobileFactory()
provide(MOBILE_FACTORY_KEY, factory)

onMounted(() => {
  void factory.init()
})
</script>

<style scoped lang="scss">
.v2-ec {
  .v2-ec-header {
    margin-bottom: 16px;

    &__top {
      margin-bottom: 0;
    }

    &__title-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    &__back-row {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    &__title {
      margin: 0;
      font-size: 20px;
      font-weight: 700;
      color: var(--wr-text, #333);
    }
  }

  .v2-ec-factory-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
    margin-bottom: 80px;
  }

  .v2-ec-factory-card {
    display: flex;
    flex-direction: column;
    background: var(--wr-card, #ffffff);
    border: 1px solid var(--wr-border, #e8ecef);
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    overflow: hidden;
    transition: transform 0.15s, box-shadow 0.2s;

    &:active {
      transform: scale(0.97);
    }

    &--disabled {
      opacity: 0.65;
    }

    &__inner {
      padding: 12px;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    &__header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 4px;
    }

    &__name {
      flex: 1;
      font-size: 14px;
      font-weight: 700;
      color: var(--wr-text, #333);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      min-width: 0;
    }

    &__actions {
      display: flex;
      gap: 4px;
      flex-shrink: 0;
    }

    &__action-btn {
      width: 26px;
      height: 26px;
      padding: 0;
      border: 1px solid var(--wr-border, #e8ecef);
      border-radius: 6px;
      background: var(--wr-card, #fff);
      color: var(--wr-text-secondary, #666);
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: background 0.15s, color 0.15s;

      &:active {
        background: #f3f4f6;
      }

      &--danger:active {
        color: #dc2626;
        background: #fef2f2;
      }
    }

    &__type {
      display: flex;
      align-items: center;
      gap: 5px;
      font-size: 12px;
      font-weight: 500;
      color: var(--wr-text-secondary, #666);
    }

    &__type-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      flex-shrink: 0;
    }

    &__contact {
      font-size: 12px;
      color: var(--wr-text-secondary, #666);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &__address {
      font-size: 11px;
      color: var(--wr-muted, #999);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .v2-ec-empty {
    padding: 40px 20px;
    text-align: center;
    font-size: 14px;
    color: var(--wr-muted, #999);
  }

  .v2-ec-factory-fab {
    position: fixed;
    bottom: 24px;
    right: 16px;
    z-index: 100;
    width: 56px;
    height: 56px;
    border: none;
    border-radius: 50%;
    background: #2563eb;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    box-shadow: 0 4px 16px rgba(37, 99, 235, 0.4);
    transition: transform 0.2s ease, box-shadow 0.2s;
    padding: 0;

    &:active {
      transform: scale(1.05);
      box-shadow: 0 6px 20px rgba(37, 99, 235, 0.5);
    }
  }
}
</style>
