<template>
  <div class="carton-style-picker">
    <div class="carton-style-picker__label">{{ t('ecommerce.carton.illustrationMaterial') }}</div>
    <div class="carton-style-picker__list" role="listbox" :aria-label="t('ecommerce.carton.illustrationMaterial')">
      <button
        v-for="item in CARTON_MATERIALS"
        :key="item.id"
        type="button"
        class="carton-style-picker__item"
        :class="{ 'is-active': modelValue === item.id }"
        role="option"
        :aria-selected="modelValue === item.id"
        @click="select(item.id)"
      >
        <div class="carton-style-picker__preview">
          <img
            class="carton-material-thumb"
            :src="item.textureUrl"
            :alt="t(item.nameKey)"
            draggable="false"
          />
          <span v-if="modelValue === item.id" class="carton-style-picker__check" aria-hidden="true">
            <el-icon><CircleCheck /></el-icon>
          </span>
        </div>
        <div class="carton-style-picker__text">
          <span class="carton-style-picker__name">{{ t(item.nameKey) }}</span>
          <span class="carton-style-picker__sub">{{ t(item.subKey) }}</span>
        </div>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { CircleCheck } from '@element-plus/icons-vue'
import { CARTON_MATERIALS, DEFAULT_CARTON_MATERIAL_VARIANT } from '@/constants/cartonMaterials'

const modelValue = defineModel<number>({ default: DEFAULT_CARTON_MATERIAL_VARIANT })

const { t } = useI18n()

function select(index: number) {
  modelValue.value = index
}
</script>

<style lang="scss">
@use '@/styles/carton-materials.scss';
</style>

<style scoped lang="scss">
.carton-style-picker {
  width: 100%;
  max-width: 420px;
}

.carton-style-picker__label {
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.carton-style-picker__list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.carton-style-picker__item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 8px 10px;
  border: 2px solid #e8edf3;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  text-align: center;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    transform: translateY(-1px);
  }

  &.is-active {
    border-color: var(--el-color-primary);
    box-shadow: 0 0 0 2px rgb(64 158 255 / 16%);
  }
}

.carton-style-picker__preview {
  position: relative;
  aspect-ratio: 1;
  padding: 6px;
  border-radius: 8px;
  background: #f3f4f6;
  overflow: hidden;
}

.carton-style-picker__check {
  position: absolute;
  top: 4px;
  right: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--el-color-primary);
  color: #fff;
  box-shadow: 0 2px 6px rgb(64 158 255 / 35%);

  .el-icon {
    font-size: 14px;
  }
}

.carton-style-picker__text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.carton-style-picker__name {
  font-size: 12px;
  font-weight: 600;
  color: var(--wr-text, #333);
  line-height: 1.3;
}

.carton-style-picker__sub {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  line-height: 1.2;
}

@media (max-width: 900px) {
  .carton-style-picker__list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
