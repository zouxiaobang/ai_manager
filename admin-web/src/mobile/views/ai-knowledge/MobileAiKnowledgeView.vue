<template>
  <div class="akm">
    <!-- 顶部三段切换：对话 / RAG 知识库 / 配置 -->
    <div class="akm__tabs">
      <button
        v-for="tab in AI_TABS"
        :key="tab.key"
        type="button"
        class="akm__tab"
        :class="{ 'is-active': activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >
        {{ t(tab.labelKey) }}
      </button>
    </div>

    <!-- ==================== 对话 ==================== -->
    <div v-show="activeTab === 'chat'" class="akm__chat">
      <!-- 会话条：当前会话入口 + 标记 / 更多 -->
      <div class="akm__convbar">
        <button
          type="button"
          class="akm__convbar-current"
          :disabled="loadingCategories"
          @click="convSheetOpen = true"
        >
          <span class="akm__convbar-title">{{ currentConvTitle }}</span>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </button>
        <div class="akm__convbar-actions">
          <button
            v-if="activeConvId"
            type="button"
            class="akm__icon-btn"
            :aria-label="t('aiKnowledge.chat.marker.marker')"
            @click="markerSheetOpen = true"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
            </svg>
          </button>
          <button
            v-if="activeConvId"
            type="button"
            class="akm__icon-btn"
            :aria-label="t('mobile.aiKnowledge.more')"
            @click="moreSheetOpen = true"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
              <circle cx="12" cy="5" r="2" /><circle cx="12" cy="12" r="2" /><circle cx="12" cy="19" r="2" />
            </svg>
          </button>
        </div>
      </div>

      <!-- 消息滚动容器：标记定位依赖 .ak-chat__msg-row[data-msg-id] 结构与 PC 端一致 -->
      <div
        ref="chatMessagesRef"
        class="akm__messages"
        @scroll="onChatScroll"
        @touchstart="onPressStart"
        @touchend="clearPress"
        @touchmove="clearPress"
        @contextmenu.prevent="onPressContext"
      >
        <div v-if="messages.length === 0" class="akm__empty">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            <path d="M8 10h.01M12 10h.01M16 10h.01" stroke-width="2" />
          </svg>
          <h3 class="akm__empty-title">{{ t('aiKnowledge.chat.emptyTitle') }}</h3>
          <p class="akm__empty-hint">{{ t('aiKnowledge.chat.emptyHint') }}</p>
        </div>

        <div
          v-for="msg in messages"
          :key="msg.id"
          class="ak-chat__msg-row"
          :class="`is-${msg.role}`"
          :data-msg-id="msg.id"
        >
          <div v-if="msg.role === 'assistant'" class="ak-chat__avatar ak-chat__avatar--ai">
            <img :src="messageAvatarUrl(msg)" class="ak-chat__avatar-img" alt="AI" @error="onAvatarError" />
          </div>
          <div v-else class="ak-chat__avatar ak-chat__avatar--user">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
          </div>
          <div class="ak-chat__bubble">
            <div class="ak-chat__content note-content-body" v-html="renderMessage(msg)" />
            <div v-if="msg.sources && msg.sources.length" class="ak-chat__sources">
              <span class="ak-chat__sources-label">{{ t('aiKnowledge.chat.ragSources') }}</span>
              <span v-for="(src, i) in msg.sources" :key="i" class="ak-chat__source-tag">{{ src.fileName }}</span>
            </div>
          </div>
        </div>

        <div v-if="sending" class="akm__typing">{{ t('mobile.aiKnowledge.sending') }}</div>
      </div>

      <!-- 输入区 -->
      <div class="akm__input">
        <div class="akm__input-tools">
          <select
            v-model="chatProvider"
            class="akm__model-select"
            :disabled="sending"
            @change="onChatModelChange(chatProvider)"
          >
            <option v-for="opt in modelOptions" :key="opt.key" :value="opt.key">{{ opt.displayModel }}</option>
          </select>
          <button
            type="button"
            class="akm__rag-pill"
            :class="{ 'is-on': useRag }"
            @click="useRag = !useRag"
          >
            {{ t('aiKnowledge.chat.useRag') }}
          </button>
        </div>
        <div class="akm__input-row">
          <textarea
            v-model="question"
            class="akm__input-area"
            rows="1"
            :placeholder="t('aiKnowledge.chat.placeholder')"
            :disabled="sending"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <button
            type="button"
            class="akm__send"
            :disabled="!canSend"
            @click="sendMessage"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- ==================== RAG 知识库 ==================== -->
    <div v-show="activeTab === 'rag'" class="akm__pane">
      <!-- 统计卡（2 列网格，最后一张占满整行） -->
      <div class="akm__stats">
        <div
          v-for="(s, i) in ragStatCards"
          :key="s.key"
          class="akm__stat"
          :class="{ 'akm__stat--wide': i === ragStatCards.length - 1 }"
        >
          <span class="akm__stat-value">{{ s.value }}</span>
          <span class="akm__stat-label">{{ t(s.labelKey) }}</span>
        </div>
      </div>

      <!-- 语义搜索 -->
      <MobileCard>
        <MobileDoodleSearch v-model="ragSearchQuery" :placeholder="t('aiKnowledge.rag.searchPlaceholder')" />
        <button
          type="button"
          class="akm__btn akm__btn--primary akm__btn--block"
          :disabled="!ragSearchQuery.trim()"
          @click="doRagSearch"
        >
          {{ t('aiKnowledge.rag.search') }}
        </button>
        <div v-if="ragSearchResults.length" class="akm__rag-results">
          <div v-for="(r, i) in ragSearchResults" :key="i" class="akm__rag-result">
            <div class="akm__rag-result-head">
              <span class="akm__rag-file">{{ r.fileName }}</span>
              <span class="akm__rag-score">{{ (r.score * 100).toFixed(1) }}%</span>
            </div>
            <div v-if="!expandedRagChunks[i]" class="akm__rag-snippet">{{ ragChunkSnippet(r.content) }}</div>
            <div v-else class="akm__rag-md note-content-body" v-html="renderRagChunkMarkdown(r.content)" />
            <button type="button" class="akm__rag-expand" @click="toggleRagChunk(i)">
              {{ t(expandedRagChunks[i] ? 'aiKnowledge.rag.collapseMarkdown' : 'aiKnowledge.rag.expandMarkdown') }}
            </button>
          </div>
        </div>
      </MobileCard>

      <!-- 文档列表 -->
      <MobileCard>
        <div class="akm__doc-head">
          <h3 class="mobile-section-title akm__doc-title">{{ t('aiKnowledge.rag.documentList') }}</h3>
          <div class="akm__doc-head-actions">
            <button type="button" class="akm__btn" :disabled="rebuilding" @click="rebuildIndex">
              {{ t('aiKnowledge.rag.rebuildIndex') }}
            </button>
            <button type="button" class="akm__btn akm__btn--primary" :disabled="uploading" @click="triggerUpload">
              {{ t('aiKnowledge.rag.upload') }}
            </button>
          </div>
        </div>
        <input
          ref="fileInputRef"
          type="file"
          class="akm__hidden-input"
          accept=".pdf,.txt,.md,.html,.docx"
          @change="handleFileSelected"
        />
        <div v-if="docsLoading" class="akm__loading"><div class="akm__spinner" /></div>
        <div v-else-if="ragDocuments.length === 0" class="mobile-empty-hint">{{ t('aiKnowledge.rag.empty') }}</div>
        <div v-else class="akm__doc-list">
          <div v-for="doc in ragDocuments" :key="doc.id" class="akm__doc">
            <div class="akm__doc-main">
              <button type="button" class="akm__doc-name" @click="openDocPreview(doc)">{{ doc.fileName }}</button>
              <div class="akm__doc-meta">
                <span>{{ doc.fileType }}</span>
                <span class="akm__dot">·</span>
                <span>{{ t('aiKnowledge.rag.chunkCount') }} {{ doc.chunkCount }}</span>
                <span class="akm__dot">·</span>
                <span class="akm__status-tag" :class="`is-${doc.status}`">{{ statusLabel(doc.status) }}</span>
              </div>
              <div v-if="doc.indexedAt" class="akm__doc-time">{{ doc.indexedAt }}</div>
            </div>
            <div class="akm__doc-ops">
              <button
                v-if="doc.status === 'failed'"
                type="button"
                class="akm__btn akm__btn--tiny"
                :disabled="retryingId === doc.id"
                @click="retryDoc(doc.id)"
              >
                {{ t('aiKnowledge.rag.retry') }}
              </button>
              <button type="button" class="akm__btn akm__btn--tiny akm__btn--danger" @click="removeDoc(doc.id)">
                {{ t('aiKnowledge.rag.remove') }}
              </button>
            </div>
          </div>
        </div>
      </MobileCard>
    </div>

    <!-- ==================== 设置 ==================== -->
    <div v-show="activeTab === 'settings'" class="akm__pane">
      <!-- LLM 配置 -->
      <MobileCard>
        <h3 class="mobile-section-title">{{ t('aiKnowledge.settings.title') }}</h3>
        <el-form
          ref="configFormRef"
          :model="configDraft"
          :rules="configRules"
          label-position="top"
        >
          <el-form-item :label="t('aiKnowledge.settings.provider')">
            <el-select v-model="configDraft.provider" style="width: 100%" @change="onProviderChange(configDraft.provider)">
              <el-option v-for="(info, key) in AI_PROVIDER_MAP" :key="key" :label="info.label" :value="key" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('aiKnowledge.settings.apiKey')" prop="apiKey">
            <el-input
              v-model="configDraft.apiKey"
              type="password"
              show-password
              :placeholder="t('aiKnowledge.settings.apiKeyPlaceholder')"
            />
          </el-form-item>
          <el-form-item :label="t('aiKnowledge.settings.apiBaseUrl')">
            <el-input v-model="configDraft.apiBaseUrl" :placeholder="t('aiKnowledge.settings.apiBaseUrlPlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('aiKnowledge.settings.model')" prop="model">
            <el-input v-model="configDraft.model" :placeholder="t('aiKnowledge.settings.modelPlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('aiKnowledge.settings.temperature')">
            <el-slider v-model="configDraft.temperature" :min="0" :max="2" :step="0.1" />
            <div class="akm__hint">{{ t('aiKnowledge.settings.temperatureHint') }}</div>
          </el-form-item>
          <el-form-item :label="t('aiKnowledge.settings.maxTokens')">
            <el-input-number v-model="configDraft.maxTokens" :min="256" :max="128000" :step="1024" style="width: 100%" />
            <div class="akm__hint">{{ t('aiKnowledge.settings.maxTokensHint') }}</div>
          </el-form-item>
          <el-form-item :label="t('aiKnowledge.settings.maxContextMessages')">
            <el-slider v-model="configDraft.maxContextMessages" :min="0" :max="50" :step="1" show-stops />
            <div class="akm__hint">{{ t('aiKnowledge.settings.maxContextMessagesHint') }}</div>
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="configDraft.defaultProvider">{{ t('aiKnowledge.settings.setAsDefault') }}</el-checkbox>
          </el-form-item>
          <div class="akm__form-actions">
            <button type="button" class="akm__btn akm__btn--primary" :disabled="savingConfig" @click="saveConfig">
              {{ t('aiKnowledge.settings.saveConfig') }}
            </button>
            <button type="button" class="akm__btn" :disabled="testingConnection" @click="testConnection">
              {{ testingConnection ? t('aiKnowledge.settings.testing') : t('aiKnowledge.settings.testConnection') }}
            </button>
          </div>
        </el-form>
      </MobileCard>

      <!-- Embedding 配置（可折叠） -->
      <MobileCard>
        <button type="button" class="akm__embed-head" @click="embedConfigExpanded = !embedConfigExpanded">
          <h3 class="mobile-section-title akm__embed-title">{{ t('aiKnowledge.settings.embeddingConfig') }}</h3>
          <span class="akm__embed-status">
            <span v-if="embeddingConfigured" class="akm__status-tag is-ready">{{ t('aiKnowledge.rag.statusReady') }}</span>
            <span v-else class="akm__status-tag is-pending">{{ t('aiKnowledge.rag.statusPending') }}</span>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline :points="embedConfigExpanded ? '18 15 12 9 6 15' : '6 9 12 15 18 9'" />
            </svg>
          </span>
        </button>
        <template v-if="embedConfigExpanded">
          <el-form :model="embedConfigDraft" label-position="top">
            <el-form-item :label="t('aiKnowledge.settings.provider')">
              <el-select
                v-model="embedConfigDraft.provider"
                style="width: 100%"
                @change="onEmbeddingProviderChange(embedConfigDraft.provider)"
              >
                <el-option v-for="(info, key) in AI_PROVIDER_MAP" :key="key" :label="info.label" :value="key" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('aiKnowledge.settings.apiKey')">
              <el-input
                v-model="embedConfigDraft.apiKey"
                type="password"
                show-password
                :placeholder="t('aiKnowledge.settings.apiKeyPlaceholder')"
              />
            </el-form-item>
            <el-form-item :label="t('aiKnowledge.settings.apiBaseUrl')">
              <el-input v-model="embedConfigDraft.apiBaseUrl" :placeholder="t('aiKnowledge.settings.apiBaseUrlPlaceholder')" />
            </el-form-item>
            <el-form-item label="Embedding Model">
              <el-input v-model="embedConfigDraft.embeddingModel" placeholder="text-embedding-3-small" />
            </el-form-item>
            <div class="akm__form-actions">
              <button type="button" class="akm__btn akm__btn--primary" :disabled="savingEmbedConfig" @click="saveEmbedConfig">
                {{ t('common.save') }}
              </button>
              <button type="button" class="akm__btn akm__btn--danger" :disabled="clearingEmbedConfig" @click="clearEmbedConfig">
                {{ t('aiKnowledge.chat.clear') }}
              </button>
            </div>
          </el-form>
        </template>
      </MobileCard>
    </div>

    <!-- ==================== 底部抽屉 ==================== -->
    <!-- 会话列表 -->
    <MobileBottomSheet v-model="convSheetOpen">
      <template #header>
        <span class="akm__sheet-title">{{ t('mobile.aiKnowledge.conversations') }}</span>
        <div class="akm__sheet-actions">
          <button type="button" class="akm__btn akm__btn--tiny" @click="addCategory">
            {{ t('mobile.aiKnowledge.addCategory') }}
          </button>
          <button type="button" class="akm__sheet-close" @click="convSheetOpen = false">✕</button>
        </div>
      </template>
      <div v-if="loadingCategories" class="akm__loading"><div class="akm__spinner" /></div>
      <div v-else-if="categories.length === 0" class="mobile-empty-hint">{{ t('mobile.aiKnowledge.noConversation') }}</div>
      <div v-else class="akm__cats">
        <div v-for="cat in categories" :key="cat.id" class="akm__cat">
          <div class="akm__cat-head">
            <button type="button" class="akm__cat-toggle" @click="toggleCategoryExpand(cat.id)">
              <svg
                width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                stroke-linecap="round" stroke-linejoin="round"
                :class="{ 'akm__chevron-rot': categoryExpanded[cat.id] }"
              >
                <polyline points="9 6 15 12 9 18" />
              </svg>
              <span class="akm__cat-name">{{ cat.name }}</span>
              <span class="akm__cat-count">{{ cat.conversations.length }}</span>
            </button>
            <div class="akm__cat-actions">
              <button type="button" class="akm__icon-btn" :aria-label="t('mobile.aiKnowledge.addConversation')" @click="addConversation(cat.id)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
                </svg>
              </button>
              <button type="button" class="akm__icon-btn" :aria-label="t('mobile.aiKnowledge.more')" @click="openCategoryActions(cat)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <circle cx="5" cy="12" r="2" /><circle cx="12" cy="12" r="2" /><circle cx="19" cy="12" r="2" />
                </svg>
              </button>
            </div>
          </div>
          <div v-if="categoryExpanded[cat.id]" class="akm__convs">
            <div
              v-for="conv in cat.conversations"
              :key="conv.id"
              class="akm__conv"
              :class="{ 'is-active': conv.id === activeConvId }"
            >
              <button type="button" class="akm__conv-main" @click="handleSwitchConversation(cat.id, conv.id)">
                <span class="akm__conv-title">{{ conv.title || t('mobile.aiKnowledge.newConversation') }}</span>
              </button>
              <button
                type="button"
                class="akm__icon-btn akm__conv-more"
                :aria-label="t('mobile.aiKnowledge.more')"
                @click="openConversationActions(cat, conv)"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <circle cx="5" cy="12" r="2" /><circle cx="12" cy="12" r="2" /><circle cx="19" cy="12" r="2" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </MobileBottomSheet>

    <!-- 标记列表 -->
    <MobileBottomSheet v-model="markerSheetOpen">
      <template #header>
        <span class="akm__sheet-title">{{ t('aiKnowledge.chat.marker.marker') }}</span>
        <button type="button" class="akm__sheet-close" @click="markerSheetOpen = false">✕</button>
      </template>
      <button type="button" class="akm__btn akm__btn--primary akm__btn--block" @click="handleAddMarker()">
        {{ t('aiKnowledge.chat.marker.addMarker') }}
      </button>
      <button
        type="button"
        class="akm__btn akm__btn--block akm__btn--margin"
        :disabled="!canJumpPrevious"
        @click="handleJumpPrevious"
      >
        {{ t('aiKnowledge.chat.marker.jumpPrevious') }}
      </button>
      <MobileDoodleSearch
        v-model="markerKeyword"
        class="akm__marker-search"
        :placeholder="t('aiKnowledge.chat.marker.searchPlaceholder')"
      />
      <div v-if="filteredMarkers.length === 0" class="mobile-empty-hint">
        {{ markers.length === 0 ? t('aiKnowledge.chat.marker.empty') : t('aiKnowledge.chat.marker.searchEmpty') }}
      </div>
      <div v-else class="akm__marker-list">
        <div v-for="m in filteredMarkers" :key="m.id" class="akm__marker">
          <button type="button" class="akm__marker-main" @click="handleJumpMarker(m.id)">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
            </svg>
            <span class="akm__marker-name">{{ m.name }}</span>
          </button>
          <button
            type="button"
            class="akm__icon-btn"
            :aria-label="t('mobile.aiKnowledge.more')"
            @click="openMarkerActions(m)"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
              <circle cx="5" cy="12" r="2" /><circle cx="12" cy="12" r="2" /><circle cx="19" cy="12" r="2" />
            </svg>
          </button>
        </div>
      </div>
      <button
        v-if="markers.length > 0"
        type="button"
        class="akm__btn akm__btn--danger akm__btn--block akm__btn--margin"
        @click="handleDeleteAllMarkers"
      >
        {{ t('aiKnowledge.chat.marker.deleteAll') }}
      </button>
    </MobileBottomSheet>

    <!-- 更多：清空 / 压缩 -->
    <MobileBottomSheet v-model="moreSheetOpen">
      <template #header>
        <span class="akm__sheet-title">{{ t('mobile.aiKnowledge.more') }}</span>
        <button type="button" class="akm__sheet-close" @click="moreSheetOpen = false">✕</button>
      </template>
      <button type="button" class="akm__btn akm__btn--block akm__btn--margin" :disabled="compressing" @click="handleCompress">
        {{ compressing ? t('mobile.aiKnowledge.compressing') : t('mobile.aiKnowledge.compress') }}
      </button>
      <button type="button" class="akm__btn akm__btn--danger akm__btn--block" @click="handleClear">
        {{ t('mobile.aiKnowledge.clear') }}
      </button>
    </MobileBottomSheet>

    <!-- 行操作动作单（分类/会话/标记/消息共用） -->
    <MobileBottomSheet v-model="actionSheetOpen" transition-name="v2-bottom-sheet">
      <div class="akm__action-list">
        <button
          v-for="item in actionItems"
          :key="item.key"
          type="button"
          class="akm__action-item"
          :class="{ 'is-danger': item.danger, 'is-disabled': item.disabled }"
          :disabled="item.disabled"
          @click="runAction(item)"
        >
          {{ t(item.labelKey) }}
        </button>
      </div>
    </MobileBottomSheet>

    <!-- 文档预览 -->
    <MobileBottomSheet v-model="ragPreviewOpen" :loading="previewLoading">
      <template #header>
        <span class="akm__sheet-title">{{ previewDoc?.fileName || t('mobile.aiKnowledge.ragPreview') }}</span>
        <button type="button" class="akm__sheet-close" @click="ragPreviewOpen = false">✕</button>
      </template>
      <div v-if="!previewLoading && !previewContent" class="mobile-empty-hint">{{ t('aiKnowledge.rag.previewEmpty') }}</div>
      <div
        v-if="previewIsMarkdown && previewContent"
        class="akm__preview-md note-content-body"
        v-html="renderDocPreviewContent()"
      />
      <pre v-if="!previewIsMarkdown && previewContent" class="akm__preview-text">{{ previewContent }}</pre>
    </MobileBottomSheet>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { marked } from 'marked'
import { ElMessage, ElMessageBox } from 'element-plus'
import { markdownToTextSnippet } from '@/utils/markdownToText'
import { useAiKnowledgeChat } from '@/views/ai-knowledge/composables/useAiKnowledgeChat'
import { useAiKnowledgeCategories } from '@/views/ai-knowledge/composables/useAiKnowledgeCategories'
import { useAiKnowledgeMarkers } from '@/views/ai-knowledge/composables/useAiKnowledgeMarkers'
import { useAiKnowledgeRag } from '@/views/ai-knowledge/composables/useAiKnowledgeRag'
import { useAiKnowledgeSettings } from '@/views/ai-knowledge/composables/useAiKnowledgeSettings'
import { AI_PROVIDER_MAP, renameChatCategory, updateChatConversation } from '@/api/aiKnowledge'
import type {
  AiProvider,
  ChatBookmark,
  ChatCategoryVO,
  ChatConversationVO,
  ChatMessage,
  ProviderInfo,
  RagSource,
} from '@/api/aiKnowledge'
import MobileCard from '@/mobile/components/MobileCard.vue'
import MobileDoodleSearch from '@/mobile/components/MobileDoodleSearch.vue'
import MobileBottomSheet from '@/mobile/components/MobileBottomSheet.vue'
import {
  activeConversationTitle,
  filterMarkersByName,
  persistChatProvider,
  readChatProviderFromStorage,
} from './mobileAiKnowledge'

const { t } = useI18n()

// ========== 三段切换 ==========
const AI_TABS = [
  { key: 'chat', labelKey: 'aiKnowledge.tabs.chat' },
  { key: 'rag', labelKey: 'aiKnowledge.tabs.rag' },
  { key: 'settings', labelKey: 'aiKnowledge.tabs.settings' },
] as const
type AiTab = (typeof AI_TABS)[number]['key']

const activeTab = ref<AiTab>('chat')

/** 切换标签：懒加载对应标签所需数据（chat 数据已在初始化时加载） */
function switchTab(tab: AiTab) {
  activeTab.value = tab
  if (tab === 'chat') loadProviders()
  else if (tab === 'rag') loadRagData()
  else {
    loadConfig()
    loadEmbeddingConfig()
  }
}

// ========== 对话（共享 ref，与 PC 端 AiKnowledgeView 同构接线） ==========
const useRag = ref(false)
const providerList = ref<ProviderInfo[]>([])
const messages = ref<(ChatMessage & { sources?: RagSource[]; collapsed?: boolean })[]>([])
const activeConvId = ref<string | null>(null)

/** 当前对话使用的 AI provider：从 localStorage 恢复上次选择（与 PC 端同 key），变更时持久化 */
const chatProvider = ref<AiProvider>(readChatProviderFromStorage())
watch(chatProvider, (v) => persistChatProvider(v))

// saveMessages 桥接：chat 状态机先于 categories 构造，通过闭包转发（同 PC 端）
let saveMessagesBridge: () => Promise<void> = async () => {}

const {
  question,
  sending,
  chatMessagesRef,
  currentContextTokens,
  compressing,
  loadProviders,
  loadChatUsage,
  recalcContextTokens,
  scrollToMsg,
  scrollChatToBottom,
  renderMessage,
  sendMessage,
  clearConversation,
  compressConversation,
  onChatScroll,
} = useAiKnowledgeChat({
  messages,
  providerList,
  chatProvider,
  useRag,
  activeConvId,
  saveMessages: () => saveMessagesBridge(),
})

// ========== 对话标记 ==========
const {
  markers,
  addMarker,
  renameMarker,
  deleteMarker,
  deleteAllMarkers,
  jumpToMarker,
  jumpToPreviousMarker,
  hasPreviousAt,
} = useAiKnowledgeMarkers({ activeConvId, chatMessagesRef })

// ========== RAG 知识库与 Embedding 配置 ==========
const {
  ragStats,
  ragDocuments,
  docsLoading,
  retryingId,
  rebuilding,
  ragSearchQuery,
  ragSearchResults,
  previewVisible,
  previewLoading,
  previewDoc,
  previewContent,
  openDocPreview,
  embedConfigExpanded,
  embedConfigDraft,
  savingEmbedConfig,
  clearingEmbedConfig,
  embeddingConfigured,
  onEmbeddingProviderChange,
  uploading,
  fileInputRef,
  statusLabel,
  loadRagData,
  loadEmbeddingConfig,
  saveEmbedConfig,
  clearEmbedConfig,
  retryDoc,
  removeDoc,
  rebuildIndex,
  doRagSearch,
  triggerUpload,
  handleFileSelected,
} = useAiKnowledgeRag()

// ========== 设置（LLM 配置） ==========
const {
  configFormRef,
  configDraft,
  modelOptions,
  configRules,
  savingConfig,
  testingConnection,
  onChatModelChange,
  onProviderChange,
  onAvatarError,
  loadConfig,
  saveConfig,
  testConnection,
} = useAiKnowledgeSettings({ providerList, chatProvider, loadProviders })

// ========== 分类与会话 ==========
const {
  categories,
  activeCatId,
  categoryExpanded,
  loadingCategories,
  loadCategoriesFromServer,
  switchConversation,
  saveMessages,
  addCategory,
  addConversation,
  deleteCategory,
  deleteConversation,
  toggleCategoryExpand,
} = useAiKnowledgeCategories({
  messages,
  activeConvId,
  sending,
  currentContextTokens,
  recalcContextTokens,
  scrollToMsg,
})

saveMessagesBridge = saveMessages

// 初始化：加载 provider 列表与会话（同 PC 端进入即恢复数据）
loadProviders()
loadCategoriesFromServer()
void loadChatUsage()

// ========== Chat 展示辅助 ==========
function messageAvatarUrl(msg: ChatMessage): string {
  return `/icons/providers/${msg.provider ?? chatProvider.value}.svg`
}

/** 当前会话标题：无标题时回落「新对话」 */
const currentConvTitle = computed(() => {
  const title = activeConversationTitle(categories.value, activeCatId.value, activeConvId.value)
  return title || t('mobile.aiKnowledge.newConversation')
})

const canSend = computed(() => question.value.trim().length > 0 && !sending.value && !!activeConvId.value)

// ========== 会话抽屉 ==========
const convSheetOpen = ref(false)

async function handleSwitchConversation(catId: string, convId: string) {
  await switchConversation(catId, convId)
  convSheetOpen.value = false
  scrollChatToBottom(true)
}

/** 分类重命名（移动端弹窗输入，改后同步本地树） */
async function renameCategory(cat: ChatCategoryVO) {
  try {
    const { value } = await ElMessageBox.prompt(t('mobile.aiKnowledge.renameCategoryPrompt'), t('mobile.aiKnowledge.rename'), {
      inputValue: cat.name,
      inputPattern: /\S/,
      inputErrorMessage: t('mobile.aiKnowledge.rename'),
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    })
    const name = value?.trim()
    if (!name) return
    await renameChatCategory(cat.id, name)
    cat.name = name
  } catch {
    // 用户取消或接口失败均不打断
  }
}

/** 会话重命名（同分类，直接改本地对象） */
async function renameConversation(conv: ChatConversationVO) {
  try {
    const { value } = await ElMessageBox.prompt(t('mobile.aiKnowledge.renameConversationPrompt'), t('mobile.aiKnowledge.rename'), {
      inputValue: conv.title,
      inputPattern: /\S/,
      inputErrorMessage: t('mobile.aiKnowledge.rename'),
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    })
    const title = value?.trim()
    if (!title) return
    await updateChatConversation(conv.id, { title })
    conv.title = title
  } catch {
    // 用户取消或接口失败均不打断
  }
}

// ========== 通用行动作单（分类/会话/标记/消息共用） ==========
interface ActionItem {
  key: string
  labelKey: string
  danger?: boolean
  /** 条件禁用：无前置标记的「回到上一个」、无标记时的「删除全部」 */
  disabled?: boolean
  handler: () => void
}

const actionSheetOpen = ref(false)
const actionItems = ref<ActionItem[]>([])

function openActionSheet(items: ActionItem[]) {
  actionItems.value = items
  actionSheetOpen.value = true
}

function runAction(item: ActionItem) {
  actionSheetOpen.value = false
  item.handler()
}

function openCategoryActions(cat: ChatCategoryVO) {
  openActionSheet([
    { key: 'rename', labelKey: 'mobile.aiKnowledge.rename', handler: () => renameCategory(cat) },
    { key: 'delete', labelKey: 'mobile.aiKnowledge.delete', danger: true, handler: () => deleteCategory(cat.id) },
  ])
}

function openConversationActions(cat: ChatCategoryVO, conv: ChatConversationVO) {
  openActionSheet([
    { key: 'rename', labelKey: 'mobile.aiKnowledge.rename', handler: () => renameConversation(conv) },
    { key: 'delete', labelKey: 'mobile.aiKnowledge.delete', danger: true, handler: () => deleteConversation(cat.id, conv.id) },
  ])
}

// ========== 长按消息：标记四件套（新增 / 标记 / 回到上一个 / 删除全部） ==========
let pressTimer: ReturnType<typeof setTimeout> | null = null
let pressMsgId: string | null = null

function clearPress() {
  if (pressTimer) {
    clearTimeout(pressTimer)
    pressTimer = null
  }
  pressMsgId = null
}

function onPressStart(e: Event) {
  const row = (e.target as HTMLElement).closest<HTMLElement>('.ak-chat__msg-row')
  if (!row) return
  // 先清掉上一个计时器（避免快速连按残留），再记录目标消息，最后挂 500ms 长按
  clearPress()
  pressMsgId = row.getAttribute('data-msg-id')
  // 500ms 长按触发动作单；touchmove 会取消，避免滚动时误触发
  pressTimer = setTimeout(openMsgActions, 500)
}

/** 桌面/Android 长按会触发 contextmenu，走同一入口（iOS 依赖上面的触摸计时器） */
function onPressContext(e: Event) {
  e.preventDefault()
  clearPress()
  const row = (e.target as HTMLElement).closest<HTMLElement>('.ak-chat__msg-row')
  if (!row) return
  pressMsgId = row.getAttribute('data-msg-id')
  openMsgActions()
}

function openMsgActions() {
  if (!pressMsgId) return
  const msg = messages.value.find((m) => m.id === pressMsgId)
  if (!msg) return
  // 长按直达标记四件套；「回到上一个」无前置标记、「删除全部」无标记时禁用
  openActionSheet([
    { key: 'add-marker', labelKey: 'aiKnowledge.chat.marker.addMarker', handler: () => addMarkerFromPress(msg) },
    { key: 'marker', labelKey: 'aiKnowledge.chat.marker.marker', handler: () => { markerSheetOpen.value = true } },
    { key: 'jump-previous', labelKey: 'aiKnowledge.chat.marker.jumpPrevious', disabled: !canJumpPrevious.value, handler: handleJumpPrevious },
    { key: 'delete-all-markers', labelKey: 'aiKnowledge.chat.marker.deleteAll', danger: true, disabled: markers.value.length === 0, handler: handleDeleteAllMarkers },
  ])
}

async function addMarkerFromPress(msg: ChatMessage) {
  // 直接锚定被长按的消息打标，不滚动视口（避免跳转到该回答顶部）
  await handleAddMarker(msg.id)
}

// ========== 标记操作 ==========
const markerSheetOpen = ref(false)
const markerKeyword = ref('')
const filteredMarkers = computed(() => filterMarkersByName(markers.value, markerKeyword.value))

/** 当前滚动位置上方是否存在「上一个」标记（与 PC 端菜单可用性判定同源） */
const canJumpPrevious = computed(() => hasPreviousAt(chatMessagesRef.value?.scrollTop ?? 0))

async function handleAddMarker(anchorMsgId?: string | null) {
  const fallback = t('aiKnowledge.chat.marker.defaultName', { n: markers.value.length + 1 })
  try {
    // 长按场景传锚点消息直接锚定；标记抽屉按钮不带锚点走视口顶线推断
    await addMarker(fallback, anchorMsgId)
    ElMessage.success(t('aiKnowledge.chat.marker.addSuccess'))
  } catch {
    ElMessage.error(t('aiKnowledge.status.error'))
  }
}

function handleJumpMarker(id: string) {
  jumpToMarker(id)
  markerSheetOpen.value = false
}

function handleJumpPrevious() {
  jumpToPreviousMarker()
}

function openMarkerActions(m: ChatBookmark) {
  openActionSheet([
    { key: 'rename', labelKey: 'aiKnowledge.chat.marker.rename', handler: () => renameMarkerAction(m) },
    { key: 'delete', labelKey: 'aiKnowledge.chat.marker.delete', danger: true, handler: () => deleteMarkerAction(m) },
  ])
}

async function renameMarkerAction(m: ChatBookmark) {
  try {
    const { value } = await ElMessageBox.prompt(
      t('aiKnowledge.chat.marker.renamePromptTitle'),
      t('aiKnowledge.chat.marker.rename'),
      {
        inputValue: m.name,
        inputPlaceholder: t('aiKnowledge.chat.marker.renamePromptPlaceholder'),
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
      },
    )
    const name = value?.trim()
    if (!name) return
    await renameMarker(m.id, name)
    ElMessage.success(t('aiKnowledge.chat.marker.renameSuccess'))
  } catch {
    // 用户取消或接口失败均不打断
  }
}

async function deleteMarkerAction(m: ChatBookmark) {
  try {
    await ElMessageBox.confirm(t('aiKnowledge.chat.marker.deleteConfirm', { name: m.name }), t('aiKnowledge.chat.marker.delete'), {
      type: 'warning',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    })
  } catch {
    return
  }
  try {
    await deleteMarker(m.id)
    ElMessage.success(t('aiKnowledge.chat.marker.deleteSuccess'))
  } catch {
    ElMessage.error(t('aiKnowledge.status.error'))
  }
}

async function handleDeleteAllMarkers() {
  try {
    await ElMessageBox.confirm(t('aiKnowledge.chat.marker.deleteAllConfirm', { n: markers.value.length }), t('aiKnowledge.chat.marker.deleteAll'), {
      type: 'warning',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    })
    await ElMessageBox.confirm(t('aiKnowledge.chat.marker.deleteAllConfirmAgain'), t('aiKnowledge.chat.marker.deleteAll'), {
      type: 'warning',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    })
  } catch {
    return
  }
  try {
    await deleteAllMarkers()
    ElMessage.success(t('aiKnowledge.chat.marker.deleteAllSuccess'))
  } catch {
    ElMessage.error(t('aiKnowledge.status.error'))
  }
}

// ========== 更多（清空 / 压缩） ==========
const moreSheetOpen = ref(false)

function handleClear() {
  moreSheetOpen.value = false
  clearConversation()
}

function handleCompress() {
  moreSheetOpen.value = false
  compressConversation()
}

// ========== RAG 视图辅助 ==========
const ragStatCards = computed(() => {
  const s = ragStats.value
  return [
    { key: 'totalDocs', value: s?.totalDocs ?? 0, labelKey: 'aiKnowledge.rag.totalDocs' },
    { key: 'readyCount', value: s?.readyCount ?? 0, labelKey: 'aiKnowledge.rag.readyCount' },
    { key: 'processingCount', value: s?.processingCount ?? 0, labelKey: 'aiKnowledge.rag.processingCount' },
    { key: 'failedCount', value: s?.failedCount ?? 0, labelKey: 'aiKnowledge.rag.failedCount' },
    { key: 'totalChunks', value: s?.totalChunks ?? 0, labelKey: 'aiKnowledge.rag.totalChunks' },
  ]
})

const RAG_SNIPPET_MAX_CHARS = 240
/** 各检索结果是否已展开 Markdown 渲染（按下标记录，避免切换结果时错位） */
const expandedRagChunks = ref<Record<number, boolean>>({})

/** 检索结果片段：Markdown → 可读纯文本 → 截断 */
function ragChunkSnippet(content: string): string {
  return markdownToTextSnippet(content, RAG_SNIPPET_MAX_CHARS)
}

/** 检索结果展开后：复用 marked 渲染 */
function renderRagChunkMarkdown(content: string): string {
  return marked.parse(content) as string
}

function toggleRagChunk(index: number) {
  expandedRagChunks.value[index] = !expandedRagChunks.value[index]
}

watch(ragSearchResults, () => {
  expandedRagChunks.value = {}
})

// ========== 文档预览（BottomSheet 版） ==========
const ragPreviewOpen = computed({
  get: () => previewVisible.value,
  set: (v: boolean) => {
    previewVisible.value = v
  },
})

const previewIsMarkdown = computed(() => {
  const type = previewDoc.value?.fileType?.toLowerCase() ?? ''
  return type === 'md' || type === 'markdown'
})

function renderDocPreviewContent(): string {
  if (!previewContent.value) return ''
  return previewIsMarkdown.value ? (marked.parse(previewContent.value) as string) : previewContent.value
}
</script>

<style scoped lang="scss">
.akm {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--wr-bg, #f9f9fa);
}

/* ========== 三段切换 ========== */
.akm__tabs {
  flex-shrink: 0;
  display: flex;
  gap: 6px;
  padding: 10px 12px;
  background: var(--wr-card, #ffffff);
  border-bottom: 1px solid var(--wr-border, #e8ecef);
}

.akm__tab {
  flex: 1;
  padding: 7px 0;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--wr-text-secondary, #666666);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;

  &.is-active {
    background: var(--wr-stat-blue, #2563eb);
    color: #ffffff;
    font-weight: 600;
  }
}

/* ========== 对话 ========== */
.akm__chat {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.akm__convbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--wr-card, #ffffff);
  border-bottom: 1px solid var(--wr-border, #e8ecef);
}

.akm__convbar-current {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
  border: none;
  background: transparent;
  color: var(--wr-text, #333333);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;

  &:disabled {
    opacity: 0.5;
  }
}

.akm__convbar-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.akm__convbar-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.akm__icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--wr-text-secondary, #666666);
  cursor: pointer;

  &:active {
    opacity: 0.7;
  }
}

/* 消息区 */
.akm__messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior-y: contain;
  padding: 14px 12px 8px;
}

.akm__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 72px 24px;
  color: var(--wr-muted, #999999);
  text-align: center;

  .akm__empty-title {
    margin: 14px 0 6px;
    font-size: 16px;
    font-weight: 600;
    color: var(--wr-text, #333333);
  }

  .akm__empty-hint {
    margin: 0;
    font-size: 13px;
    color: var(--wr-muted, #999999);
  }
}

/* 消息行结构与 PC 端一致（标记锚点依赖 .ak-chat__msg-row[data-msg-id]），样式按移动端重写 */
.ak-chat__msg-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 16px;

  &.is-user {
    flex-direction: row-reverse;

    .ak-chat__bubble {
      background: var(--wr-stat-blue, #2563eb);
      color: #ffffff;

      :deep(.note-content-body) {
        color: #ffffff;

        a { color: #dbeafe; }
        code { background: rgb(255 255 255 / 18%); color: #ffffff; }
      }
    }
  }

  &.is-system {
    .ak-chat__bubble {
      background: var(--wr-index-bg, #eff6ff);
      color: var(--wr-text-secondary, #666666);
    }
  }
}

.ak-chat__avatar {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  &--ai {
    background: #ffffff;
    border: 1px solid var(--wr-border, #e8ecef);
  }

  &--user {
    background: var(--wr-stat-blue, #2563eb);
    color: #ffffff;
  }
}

.ak-chat__avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ak-chat__bubble {
  max-width: 82%;
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--wr-card, #ffffff);
  border: 1px solid var(--wr-border, #e8ecef);
  box-shadow: var(--wr-shadow, 0 2px 8px rgb(0 0 0 / 5%));
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  overflow-wrap: break-word;
}

.ak-chat__sources {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
}

.ak-chat__sources-label {
  font-size: 11px;
  color: var(--wr-muted, #999999);
}

.ak-chat__source-tag {
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--wr-index-bg, #eff6ff);
  color: var(--wr-stat-blue, #2563eb);
  font-size: 11px;
}

.akm__typing {
  padding: 4px 4px 10px;
  font-size: 12px;
  color: var(--wr-muted, #999999);
}

/* 输入区 */
.akm__input {
  flex-shrink: 0;
  padding: 8px 12px max(10px, env(safe-area-inset-bottom));
  background: var(--wr-card, #ffffff);
  border-top: 1px solid var(--wr-border, #e8ecef);
}

.akm__input-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.akm__model-select {
  max-width: 60%;
  padding: 4px 24px 4px 8px;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 8px;
  background: var(--wr-bg, #f9f9fa);
  color: var(--wr-text, #333333);
  font-size: 12px;
  font-family: inherit;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='10' viewBox='0 0 24 24' fill='none' stroke='%23999' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 6px center;
}

.akm__rag-pill {
  padding: 5px 10px;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 999px;
  background: var(--wr-bg, #f9f9fa);
  color: var(--wr-muted, #999999);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;

  &.is-on {
    background: var(--wr-index-bg, #eff6ff);
    border-color: var(--wr-stat-blue, #2563eb);
    color: var(--wr-stat-blue, #2563eb);
    font-weight: 600;
  }
}

.akm__input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.akm__input-area {
  flex: 1;
  min-height: 40px;
  max-height: 96px;
  padding: 10px 12px;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 10px;
  background: var(--wr-bg, #f9f9fa);
  color: var(--wr-text, #333333);
  font-family: inherit;
  font-size: 14px;
  line-height: 1.4;
  resize: none;
  outline: none;

  &:focus {
    border-color: var(--wr-stat-blue, #2563eb);
  }

  &:disabled {
    opacity: 0.6;
  }
}

.akm__send {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  padding: 0;
  border: none;
  border-radius: 10px;
  background: var(--wr-stat-blue, #2563eb);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  &:active:not(:disabled) {
    opacity: 0.8;
  }
}

/* ========== RAG / 设置 滚动面板 ========== */
.akm__pane {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.akm__stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.akm__stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 12px 8px;
  background: var(--wr-card, #ffffff);
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 12px;
  box-shadow: var(--wr-shadow, 0 2px 8px rgb(0 0 0 / 5%));

  &--wide {
    grid-column: 1 / -1;
  }
}

.akm__stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--wr-stat-blue, #2563eb);
  line-height: 1.2;
}

.akm__stat-label {
  font-size: 12px;
  color: var(--wr-text-secondary, #666666);
}

.akm__hidden-input {
  display: none;
}

.akm__btn {
  padding: 7px 14px;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 10px;
  background: var(--wr-card, #ffffff);
  color: var(--wr-text, #333333);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  &:active:not(:disabled) {
    opacity: 0.8;
  }

  &--primary {
    background: var(--wr-stat-blue, #2563eb);
    border-color: var(--wr-stat-blue, #2563eb);
    color: #ffffff;
  }

  &--danger {
    color: #ef4444;
    border-color: #fecaca;
  }

  &--block {
    display: block;
    width: 100%;
  }

  &--margin {
    margin-bottom: 10px;
  }

  &--tiny {
    padding: 3px 8px;
    font-size: 12px;
    border-radius: 8px;
  }
}

/* RAG 语义搜索 */
.akm__rag-results {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.akm__rag-result {
  padding: 10px 12px;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 10px;
  background: var(--wr-bg, #f9f9fa);
}

.akm__rag-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.akm__rag-file {
  font-size: 13px;
  font-weight: 600;
  color: var(--wr-stat-blue, #2563eb);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.akm__rag-score {
  flex-shrink: 0;
  font-size: 12px;
  color: #059669;
  font-weight: 600;
}

.akm__rag-snippet {
  font-size: 13px;
  color: var(--wr-text-secondary, #666666);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.akm__rag-md {
  font-size: 13px;
}

.akm__rag-expand {
  margin-top: 6px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--wr-stat-blue, #2563eb);
  font-size: 12px;
  cursor: pointer;
}

/* 文档列表 */
.akm__doc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}

.akm__doc-title {
  margin: 0;
}

.akm__doc-head-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.akm__doc-list {
  display: flex;
  flex-direction: column;
}

.akm__doc {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px solid var(--wr-border, #e8ecef);

  &:last-child {
    border-bottom: none;
  }
}

.akm__doc-main {
  flex: 1;
  min-width: 0;
}

.akm__doc-name {
  display: block;
  max-width: 100%;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--wr-stat-blue, #2563eb);
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.akm__doc-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 3px;
  font-size: 12px;
  color: var(--wr-muted, #999999);
}

.akm__dot {
  color: var(--wr-border, #e8ecef);
}

.akm__status-tag {
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;

  &.is-ready { background: #f0fdf4; color: #059669; }
  &.is-processing, &.is-pending { background: #fffbeb; color: #b45309; }
  &.is-failed { background: #fef2f2; color: #b91c1c; }
  &.is-info { background: #f1f5f9; color: #64748b; }
}

.akm__doc-time {
  margin-top: 2px;
  font-size: 11px;
  color: var(--wr-muted, #999999);
}

.akm__doc-ops {
  flex-shrink: 0;
  display: flex;
  gap: 6px;
}

.akm__loading {
  display: flex;
  justify-content: center;
  padding: 32px 0;
}

.akm__spinner {
  width: 26px;
  height: 26px;
  border: 3px solid var(--wr-border, #e8ecef);
  border-top-color: var(--wr-stat-blue, #2563eb);
  border-radius: 50%;
  animation: akm-spin 0.7s linear infinite;
}

@keyframes akm-spin {
  to { transform: rotate(360deg); }
}

/* ========== 设置 ========== */
.akm__hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--wr-muted, #999999);
  line-height: 1.4;
}

.akm__form-actions {
  display: flex;
  gap: 8px;
}

.akm__embed-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
}

.akm__embed-title {
  margin: 0;
}

.akm__embed-status {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--wr-muted, #999999);
}

/* ========== 底部抽屉 ========== */
.akm__sheet-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--wr-text, #333333);
}

.akm__sheet-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.akm__sheet-close {
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: #f1f5f9;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
}

/* 会话列表 */
.akm__cat-head {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 0 6px;
}

.akm__cat-toggle {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--wr-text, #333333);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.akm__chevron-rot {
  transform: rotate(90deg);
}

.akm__cat-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.akm__cat-count {
  padding: 0 6px;
  border-radius: 8px;
  background: var(--wr-index-bg, #eff6ff);
  color: var(--wr-stat-blue, #2563eb);
  font-size: 11px;
  font-weight: 600;
}

.akm__cat-actions {
  display: flex;
  flex-shrink: 0;
}

.akm__convs {
  padding-left: 12px;
}

.akm__conv {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
  padding: 2px 4px 2px 6px;
  border-radius: 8px;
  color: var(--wr-text-secondary, #666666);

  &.is-active {
    background: var(--wr-index-bg, #eff6ff);
    color: var(--wr-stat-blue, #2563eb);
    font-weight: 600;
  }
}

.akm__conv-main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  padding: 8px 6px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: inherit;
  font-size: 13px;
  font-weight: inherit;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}

.akm__conv-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.akm__conv-more {
  flex-shrink: 0;
}

/* 标记列表 */
.akm__marker-search {
  margin-top: 10px;
}

.akm__marker-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.akm__marker {
  display: flex;
  align-items: center;
  gap: 4px;

  .akm__marker-main {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 9px 6px;
    border: none;
    border-radius: 8px;
    background: transparent;
    color: var(--wr-text, #333333);
    font-size: 13px;
    text-align: left;
    cursor: pointer;

    svg {
      flex-shrink: 0;
      color: var(--wr-stat-blue, #2563eb);
    }
  }

  .akm__marker-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &:hover .akm__marker-main {
    background: var(--wr-index-bg, #eff6ff);
  }
}

/* 动作单 */
.akm__action-list {
  display: flex;
  flex-direction: column;
}

.akm__action-item {
  padding: 14px 4px;
  border: none;
  border-bottom: 1px solid var(--wr-border, #e8ecef);
  background: transparent;
  color: var(--wr-text, #333333);
  font-size: 15px;
  text-align: center;
  cursor: pointer;

  &:last-child {
    border-bottom: none;
  }

  &.is-danger {
    color: #ef4444;
  }

  &.is-disabled {
    color: #c0c4cc;
    cursor: not-allowed;

    &:active {
      background: transparent;
    }
  }

  &:active {
    background: var(--wr-index-bg, #eff6ff);
  }
}

/* 文档预览 */
.akm__preview-md {
  font-size: 14px;
}

.akm__preview-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
  color: var(--wr-text, #333333);
  font-family: inherit;
}
</style>
