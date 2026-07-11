<template>
  <!-- 移动端笔记详情页容器 -->
  <div class="mobile-note-detail">
    <!-- 页面头部：返回按钮 + 笔记标题 -->
    <div class="mobile-note-detail__header">
      <!-- 返回按钮：返回上一页 -->
      <button class="mobile-note-detail__back" @click="goBack">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <!-- 笔记标题区域 -->
      <div class="mobile-note-detail__title"><h2>{{ title }}</h2></div>
    </div>

    <!-- 笔记内容编辑/展示区域 -->
    <div class="mobile-note-detail__editor">
      <!-- 内容加载中状态 -->
      <div v-if="contentLoading" class="mobile-note-detail__loading">
        {{ t('notebook.contentLoading') }}
      </div>
      <!-- 笔记内容展示区：富文本HTML渲染 -->
      <div
        v-show="!contentLoading"
        class="mobile-note-detail__content"
        v-html="content"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 移动端笔记详情视图组件
 * 功能说明：
 * - 展示单条笔记的详细内容
 * - 支持富文本HTML内容渲染
 * - 提供返回按钮进行页面导航
 * - 处理内容加载状态和加载失败状态
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { APP_FONT_FAMILY } from '@/constants/font-family'
import { fetchNote } from '@/api/notebook'

const route = useRoute() // 当前路由信息
const router = useRouter() // 路由实例
const { t } = useI18n() // 国际化翻译函数

const noteId = Number(route.params.id) // 笔记ID（从路由参数获取）
const title = ref('') // 笔记标题
const content = ref('') // 笔记内容（HTML格式）
const contentLoading = ref(true) // 内容加载状态

// 返回上一页
function goBack() {
  router.back()
}

// 加载笔记详情数据
async function loadNote() {
  if (!Number.isFinite(noteId) || noteId <= 0) return
  contentLoading.value = true
  try {
    const detail = await fetchNote(noteId) // 请求笔记详情API
    title.value = detail.title
    content.value = detail.content ?? ''
  } catch {
    content.value = `<p class="mobile-note-detail__error">${t('notebook.contentLoadFailed')}</p>`
  } finally {
    contentLoading.value = false
  }
}

onMounted(() => {
  void loadNote() // 组件挂载时加载笔记数据
})
</script>

<style scoped lang="scss">
.mobile-note-detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: calc(100vh - 120px);
  min-height: calc(100dvh - 120px);
}

.mobile-note-detail__header {
  display: flex;
  align-items: center;
  padding: 8px 16px;
}

.mobile-note-detail__back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 2px solid #333;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  color: #333;
  font-size: 16px;
  transition: background 0.15s;

  &:active {
    background: #f0ede8;
  }
}

.mobile-note-detail__toolbar {
  display: none;
}

.mobile-note-detail__title {
  flex: 1;
  margin-left: 16px;
  color: #272727;

  h2 {
    margin: 0;
    font-size: 17px;
    font-weight: 600;
    font-family: 'ZCOOL KuaiLe', cursive;
  }
}

.mobile-note-detail__editor {
  flex: 1;
  min-height: 360px;
}

.mobile-note-detail__loading {
  padding: 16px;
  color: #888;
  font-size: 14px;
  font-family: 'ZCOOL KuaiLe', cursive;
}

.mobile-note-detail__content {
  padding: 12px 20px 72px;
  font-family: v-bind(APP_FONT_FAMILY);
  font-size: 14px;
  line-height: 1.75;
  color: var(--wr-text, #333);
  word-wrap: break-word;
  overflow-wrap: break-word;

  :deep(img) {
    max-width: 100%;
    height: auto;
    border-radius: 6px;
  }

  :deep(p),
  :deep(li) {
    margin: 0 0 12px;
    font-size: 14px;
    line-height: 1.75;
  }

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4),
  :deep(h5) {
    margin: 1.2em 0 0.6em;
    font-weight: 600;
    line-height: 1.4;
  }

  :deep(h1) { font-size: 22px; }
  :deep(h2) { font-size: 20px; }
  :deep(h3) { font-size: 18px; }
  :deep(h4) { font-size: 16px; }

  :deep(blockquote) {
    margin: 8px 0;
    padding: 4px 16px;
    border-left: 3px solid #d0d5dd;
    color: #666;
    background: #f8f9fa;
    border-radius: 0 4px 4px 0;
  }

  :deep(pre) {
    margin: 8px 0;
    padding: 12px 16px;
    background: #f5f6f7;
    border-radius: 8px;
    overflow-x: auto;
    font-size: 13px;
    line-height: 1.6;
    font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  }

  :deep(code) {
    padding: 2px 6px;
    background: #f0f0f0;
    border-radius: 4px;
    font-size: 13px;
    font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  }

  :deep(pre code) {
    padding: 0;
    background: none;
    border-radius: 0;
  }

  :deep(ul),
  :deep(ol) {
    padding-left: 24px;
    margin: 8px 0;
  }

  :deep(li) {
    margin-bottom: 4px;
  }

  :deep(a) {
    color: #1677ff;
    text-decoration: underline;
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 12px 0;
    font-size: 14px;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid #e0e0e0;
    padding: 8px 12px;
    text-align: left;
  }

  :deep(th) {
    background: #f8f9fa;
    font-weight: 600;
  }

  :deep(hr) {
    margin: 16px 0;
    border: none;
    border-top: 1px solid #e0e0e0;
  }

  :deep(.w-e-text-container) {
    all: unset;
  }
}

.mobile-note-detail__error {
  color: #ef4444;
  padding: 16px;
}
</style>
