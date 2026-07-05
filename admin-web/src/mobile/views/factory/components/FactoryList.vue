<template>
  <section class="mfc-section">
    <MobileSectionHeader
        :icon="schemeAAssets.starBlue"
        :title="factoryTitle"
        :count="factory.total.value"
    />

    <div v-if="factory.records.value.length" class="mfc-factory-list">
      <SchemeADoodleFrame
        v-for="row in factory.records.value"
        :key="row.id"
        class="mfc-factory-card"
        color="#cbd5e1"
        :seed="row.id"
        sketch
        :stroke-width="3"
        :shadow="false"
      >
        <div class="mfc-factory-card__inner">
          <div class="mfc-factory-card__head">
            <h3 class="mfc-factory-card__name">{{ row.name }}</h3>
            <span
              class="mfc-factory-card__status"
              :class="row.status === 'ENABLED' ? 'is-enabled' : 'is-disabled'"
            >
              {{
                row.status === 'ENABLED'
                  ? factory.t('ecommerce.product.enabled')
                  : factory.t('ecommerce.product.disabled')
              }}
            </span>
          </div>

          <MobileDoodleChip
            shape="pill"
            :color="factory.factoryTypeColor(row.factoryType)"
            :seed="row.id"
            class="mfc-factory-card__type"
            :style="{
              color: factory.factoryTypeColor(row.factoryType),
              '--mfc-type-bg': `${factory.factoryTypeColor(row.factoryType)}18`,
            }"
          >
            {{ factory.factoryTypeLabel(row.factoryType) }}
          </MobileDoodleChip>

          <p class="mfc-factory-card__contact">{{ factory.factoryContactLine(row) }}</p>
          <p class="mfc-factory-card__address">{{ row.address || '\u00a0' }}</p>

          <div class="mfc-factory-card__actions">
            <MobileDoodleChip
              tag="button"
              type="button"
              shape="pill"
              color="#8b5cf6"
              :seed="row.id + 1"
              class="mfc-factory-card__edit"
              @click="factory.openEdit(row)"
            >
              {{ factory.t('ecommerce.factory.edit') }}
            </MobileDoodleChip>
            <MobileDoodleChip
              tag="button"
              type="button"
              shape="pill"
              color="#ef4444"
              :seed="row.id + 2"
              class="mfc-factory-card__delete"
              @click="factory.onDelete(row)"
            >
              {{ factory.t('ecommerce.factory.delete') }}
            </MobileDoodleChip>
          </div>
        </div>
      </SchemeADoodleFrame>
    </div>

    <SchemeADoodleFrame v-else-if="!factory.loading.value" class="mfc-empty-card" color="#cbd5e1" :shadow="false">
      <p class="mfc-empty">{{ factory.t('mobile.factory.emptyList') }}</p>
    </SchemeADoodleFrame>

    <SchemeADoodleFrame
      v-if="factory.hasMore.value"
      tag="button"
      type="button"
      class="mfc-load-more"
      color="#cbd5e1"
      sketch
      :stroke-width="2.5"
      :shadow="false"
      :disabled="factory.loading.value"
      @click="factory.loadMore()"
    >
      {{ factory.loading.value ? factory.t('mobile.factory.loadingMore') : factory.t('mobile.factory.loadMore') }}
    </SchemeADoodleFrame>
  </section>
</template>

<script setup lang="ts">
import {computed, inject} from 'vue'
import { MOBILE_FACTORY_KEY } from '../factoryContext'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import MobileSectionHeader from "@/mobile/components/MobileSectionHeader.vue";

const factory = inject(MOBILE_FACTORY_KEY)!
const factoryTitle = computed(() => factory.t('mobile.factory.factoryList'))

</script>

<style scoped lang="scss">
.mfc-section__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.mfc-section__icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.mfc-section__title {
  margin: 0;
  flex: 1;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.mfc-section__count {
  font-size: 24px;
  font-weight: 800;
  color: #991b1b;
}

.mfc-factory-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.mfc-factory-card {
  min-width: 0;
  height: 100%;

  :deep(.sa-doodle-frame__body) {
    box-sizing: border-box;
    padding: 4px 5px 8px;
    height: 100%;
  }
}

.mfc-factory-card__inner {
  box-sizing: border-box;
  height: 100%;
  padding: 8px 6px 6px;
  overflow: visible;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.mfc-factory-card__head {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 100%;
  margin-bottom: 6px;
}

.mfc-factory-card__name {
  margin: 0;
  width: 100%;
  font-size: 14px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.25;
  text-align: center;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.mfc-factory-card__status {
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

.mfc-factory-card__type {
  display: inline-flex;
  max-width: 100%;
  margin-bottom: 6px;
  font-size: 10px;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  :deep(.sa-doodle-frame) {
    background: var(--mfc-type-bg, #fff);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 2px 7px;
  }
}

.mfc-factory-card__contact {
  margin: 0 0 3px;
  width: 100%;
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mfc-factory-card__address {
  margin: 0 0 8px;
  width: 100%;
  min-height: 14px;
  font-size: 10px;
  line-height: 1.4;
  color: #94a3b8;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mfc-factory-card__actions {
  position: relative;
  z-index: 3;
  flex-shrink: 0;
  display: flex;
  gap: 5px;
  width: 100%;
  justify-content: center;
}

.mfc-factory-card__edit,
.mfc-factory-card__delete {
  flex: 1;
  min-width: 0;
  font-family: inherit;
  font-size: 11px;
  font-weight: 800;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  background: #fff;

  :deep(.sa-doodle-frame__body) {
    padding: 5px 4px;
    text-align: center;
  }
}

.mfc-factory-card__edit {
  color: #8b5cf6;
}

.mfc-factory-card__delete {
  color: #ef4444;
}

.mfc-empty-card {
  grid-column: 1 / -1;

  :deep(.sa-doodle-frame__body) {
    padding: 24px 16px;
    text-align: center;
  }
}

.mfc-empty {
  margin: 0;
  color: #94a3b8;
  font-size: 14px;
  font-weight: 600;
}

.mfc-load-more {
  grid-column: 1 / -1;
  display: block;
  width: 100%;
  margin-top: 2px;
  font-family: inherit;
  font-size: 14px;
  font-weight: 700;
  color: #8b5cf6;
  cursor: pointer;
  background: #fff;

  :deep(.sa-doodle-frame__body) {
    padding: 10px;
    text-align: center;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}
</style>
