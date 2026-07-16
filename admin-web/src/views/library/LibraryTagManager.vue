<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="t('library.manageTags')"
    width="480px"
    :close-on-click-modal="false"
  >
    <div class="library-tag-manager">
      <div class="library-tag-manager__section">
        <label class="library-tag-manager__section-label">{{ t('library.selectedTags') }}</label>
        <div class="library-tag-manager__tag-list">
          <el-tag
            v-for="tag in selectedTags"
            :key="tag.id"
            closable
            @close="removeTag(tag)"
          >
            {{ tag.name }}
          </el-tag>
          <span v-if="selectedTags.length === 0" class="library-tag-manager__empty">
            {{ t('library.noTags') }}
          </span>
        </div>
      </div>

      <div class="library-tag-manager__section">
        <label class="library-tag-manager__section-label">{{ t('library.allTags') }}</label>
        <div class="library-tag-manager__tag-list">
          <el-tag
            v-for="tag in availableTags"
            :key="tag.id"
            style="cursor: pointer"
            :type="isSelected(tag) ? 'success' : 'info'"
            :effect="isSelected(tag) ? 'dark' : 'plain'"
            @click="toggleTag(tag)"
          >
            {{ tag.name }}
          </el-tag>
          <span v-if="availableTags.length === 0" class="library-tag-manager__empty">
            {{ t('library.noTags') }}
          </span>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleManageTags">{{ t('library.tagManage') }}</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">
        {{ t('common.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { fetchAllTags } from '@/api/library/tag'
import { syncFileTags } from '@/api/library/tag'
import type { DocTag } from '@/api/library/types'

const props = defineProps<{
  visible: boolean
  fileId: number | null
  tagIds: number[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: [tagIds: number[]]
}>()

const { t } = useI18n()

const selectedTags = ref<DocTag[]>([])
const availableTags = ref<DocTag[]>([])
const saving = ref(false)

function isSelected(tag: DocTag) {
  return selectedTags.value.some((t) => t.id === tag.id)
}

function toggleTag(tag: DocTag) {
  if (isSelected(tag)) {
    selectedTags.value = selectedTags.value.filter((t) => t.id !== tag.id)
  } else {
    selectedTags.value.push(tag)
  }
}

function removeTag(tag: DocTag) {
  selectedTags.value = selectedTags.value.filter((t) => t.id !== tag.id)
}

async function loadAllTags() {
  try {
    availableTags.value = await fetchAllTags()
  } catch {
    availableTags.value = []
  }
}

async function loadFileTags() {
  if (!props.fileId) return
  try {
    const allTags = await fetchAllTags()
    selectedTags.value = allTags.filter((t) => props.tagIds.includes(t.id))
  } catch {
    selectedTags.value = []
  }
}

async function handleSave() {
  if (!props.fileId) return
  saving.value = true
  try {
    const tagIds = selectedTags.value.map((t) => t.id)
    await syncFileTags(props.fileId, tagIds)
    ElMessage.success(t('common.saved'))
    emit('saved', tagIds)
    emit('update:visible', false)
  } finally {
    saving.value = false
  }
}

function handleManageTags() {
  emit('update:visible', false)
}

watch(
  () => props.visible,
  (v) => {
    if (v) {
      loadAllTags()
      loadFileTags()
    }
  },
)
</script>

<style scoped lang="scss">
.library-tag-manager {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.library-tag-manager__section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.library-tag-manager__section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.library-tag-manager__tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 32px;
}

.library-tag-manager__empty {
  font-size: 13px;
  color: var(--el-text-color-placeholder);
}
</style>
