<template>
  <div class="ec-settings-list">
    <div v-for="(_, index) in model" :key="`item-${index}`" class="ec-settings-list__row">
      <span class="ec-settings-list__index">{{ index + 1 }}.</span>
      <el-input v-model="model[index]" type="textarea" :rows="2" />
      <el-button link type="danger" :disabled="model.length <= 1" @click="remove(index)">
        <el-icon><Delete /></el-icon>
      </el-button>
    </div>
    <el-button type="primary" link @click="add">
      <el-icon><Plus /></el-icon>
      {{ t('ecommerce.settings.addItem') }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Delete, Plus } from '@element-plus/icons-vue'

const model = defineModel<string[]>({ required: true })
const { t } = useI18n()

function add() {
  model.value = [...model.value, '']
}

function remove(index: number) {
  const next = [...model.value]
  next.splice(index, 1)
  model.value = next.length ? next : ['']
}
</script>

<style scoped lang="scss">
.ec-settings-list {
  width: 100%;
}

.ec-settings-list__row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
}

.ec-settings-list__index {
  flex: 0 0 20px;
  line-height: 32px;
  color: var(--el-text-color-secondary);
}
</style>
