<template>
  <!-- AI 知识库对话 / RAG 知识库 / 知识库设置 -->
  <div class="ai-knowledge-page war-room-page war-room-page--fill">
    <!-- 页面头部标题 -->
    <header class="ai-knowledge__header">
      <h1 class="ai-knowledge__title">{{ t('aiKnowledge.title') }}</h1>
    </header>

    <!-- 标签页切换 -->
    <el-tabs
      v-model="activeTab"
      class="ai-knowledge__tabs"
      @tab-change="onTabChange"
    >
      <!-- ========== 对话 ========== -->
      <el-tab-pane :label="t('aiKnowledge.tabs.chat')" name="chat">
        <div class="ai-knowledge__tab-content">
          <div class="ak-layout">
            <!-- 左侧对话列表 -->
            <aside class="ak-sidebar">
              <div class="ak-sidebar__header">
                <span class="ak-sidebar__header-title">💬 对话列表</span>
                <el-tooltip content="新增分类" placement="top">
                  <el-button text circle size="small" class="ak-sidebar__add-cat-btn" @click.stop="addCategory">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                  </el-button>
                </el-tooltip>
              </div>
              <!-- 搜索框 -->
              <div class="ak-sidebar__search">
                <el-input
                  v-model="searchQuery"
                  placeholder="搜索对话..."
                  clearable
                  @input="handleSearchInput"
                  @clear="clearSearch"
                >
                  <template #prefix>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                  </template>
                </el-input>
              </div>
              <!-- 搜索结果 -->
              <div v-if="searchQuery.trim()" class="ak-sidebar__list">
                <div v-if="searching" class="ak-sidebar__search-status">搜索中...</div>
                <div v-else-if="searchResults.length === 0" class="ak-sidebar__search-status">无匹配结果</div>
                <div v-else class="ak-sidebar__search-results">
                  <div
                    v-for="r in searchResults"
                    :key="r.conversationId"
                    class="ak-sidebar__search-item"
                    :class="{ 'is-active': activeConvId === r.conversationId }"
                    @click="switchToSearchResult(r)"
                  >
                    <div class="ak-sidebar__search-cat">{{ r.categoryName }}</div>
                    <div class="ak-sidebar__search-title">{{ r.conversationTitle || '新对话' }}</div>
                    <div class="ak-sidebar__search-snippet">{{ r.matchSummary }}</div>
                  </div>
                </div>
              </div>
              <div class="ak-sidebar__list" v-else-if="categories.length > 0">
                <div v-for="cat in categories" :key="cat.id" class="ak-sidebar__category">
                  <div class="ak-sidebar__cat-header" :class="{ 'is-empty': cat.conversations.length === 0 }">
                    <span class="ak-sidebar__cat-toggle" @click="toggleCategoryExpand(cat.id)">
                      <svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor" :class="{ 'is-expanded': categoryExpanded[cat.id] }"><polygon points="8,4 8,20 20,12"/></svg>
                    </span>
                    <template v-if="renamingCatId === cat.id">
                      <el-input v-model="catRenameInput" size="small" ref="catRenameRef" class="ak-sidebar__rename-input" @keydown.enter="confirmRenameCategory(cat.id)" @blur="confirmRenameCategory(cat.id)" @keydown.escape="cancelRenameCategory" />
                    </template>
                    <span v-else class="ak-sidebar__cat-name" @click="toggleCategoryExpand(cat.id)">{{ cat.name }}</span>
                    <span class="ak-sidebar__cat-count">{{ cat.conversations.length }}</span>
                    <div class="ak-sidebar__cat-actions">
                      <el-tooltip content="新建对话" placement="top">
                        <el-button text circle size="small" class="ak-sidebar__action-btn" @click.stop="addConversation(cat.id)">
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                        </el-button>
                      </el-tooltip>
                      <el-dropdown trigger="click" @command="(cmd: string) => handleCategoryCommand(cmd, cat)" popper-class="ak-sidebar__dropdown-popper">
                        <el-button text circle size="small" class="ak-sidebar__action-btn" @click.stop>
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="19" r="1.5"/></svg>
                        </el-button>
                        <template #dropdown>
                          <el-dropdown-menu>
                            <el-dropdown-item command="rename"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg> 重命名</el-dropdown-item>
                            <el-dropdown-item command="delete" divided><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg> 删除分类</el-dropdown-item>
                          </el-dropdown-menu>
                        </template>
                      </el-dropdown>
                    </div>
                  </div>
                  <div v-show="categoryExpanded[cat.id]" class="ak-sidebar__conv-list">
                    <div
                      v-for="conv in cat.conversations"
                      :key="conv.id"
                      class="ak-sidebar__conv-item"
                      :class="{ 'is-active': activeConvId === conv.id && activeCatId === cat.id }"
                      @click="switchConversation(cat.id, conv.id)"
                    >
                      <svg class="ak-sidebar__conv-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                      <template v-if="renamingConvId === conv.id">
                        <el-input v-model="convRenameInput" size="small" ref="convRenameRef" class="ak-sidebar__rename-input" @keydown.enter="confirmRenameConversation(cat.id, conv.id)" @blur="confirmRenameConversation(cat.id, conv.id)" @keydown.escape="cancelRenameConversation" />
                      </template>
                      <span v-else class="ak-sidebar__conv-title">{{ conv.title || '新对话' }}</span>
                      <el-dropdown trigger="click" @command="(cmd: string) => handleConversationCommand(cmd, cat.id, conv)" popper-class="ak-sidebar__dropdown-popper" class="ak-sidebar__conv-actions">
                        <el-button text circle size="small" class="ak-sidebar__action-btn" @click.stop>
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="19" r="1.5"/></svg>
                        </el-button>
                        <template #dropdown>
                          <el-dropdown-menu>
                            <el-dropdown-item command="rename"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg> 重命名</el-dropdown-item>
                            <el-dropdown-item command="delete" divided><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg> 删除对话</el-dropdown-item>
                          </el-dropdown-menu>
                        </template>
                      </el-dropdown>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="ak-sidebar__empty">
                <p>暂无对话，请新增分类</p>
              </div>
            </aside>

            <!-- 右侧聊天区域 -->
            <div class="ak-chat">
            <div class="ak-chat__body">
            <div ref="chatMessagesRef" class="ak-chat__messages">
              <div v-if="messages.length === 0" class="ak-chat__empty">
                <div class="ak-chat__empty-icon">
                  <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                    <path d="M8 10h.01M12 10h.01M16 10h.01" stroke-width="2"/>
                  </svg>
                </div>
                <h3 class="ak-chat__empty-title">{{ t('aiKnowledge.chat.emptyTitle') }}</h3>
                <p class="ak-chat__empty-hint">{{ t('aiKnowledge.chat.emptyHint') }}</p>
              </div>

              <div v-for="msg in messages" :key="msg.id" class="ak-chat__msg-row" :class="`is-${msg.role}`" :data-msg-id="msg.id">
                <div v-if="msg.role === 'assistant'" class="ak-chat__avatar ak-chat__avatar--ai">
                  <img :src="providerAvatarUrl" class="ak-chat__avatar-img" alt="AI" @error="onAvatarError">
                </div>
                <div v-else class="ak-chat__avatar ak-chat__avatar--user">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                  </svg>
                </div>
                <div class="ak-chat__bubble">
                  <div v-show="!msg.collapsed" class="ak-chat__content markdown-body" v-html="renderMessage(msg)"/>
                  <div v-if="msg.sources && msg.sources.length" class="ak-chat__sources">
                    <span class="ak-chat__sources-label">{{ t('aiKnowledge.chat.ragSources') }}</span>
                    <span v-for="(src, i) in msg.sources" :key="i" class="ak-chat__source-tag">
                      {{ src.fileName }}
                    </span>
                  </div>
                  <!-- 助手消息工具栏 -->
                  <div v-if="msg.role === 'assistant'" class="ak-chat__toolbar">
                    <el-dropdown
                      trigger="click"
                      popper-class="ak-chat__toolbar-popper"
                      @command="(cmd: string) => handleToolbarCommand(cmd, msg)"
                    >
                      <button class="ak-chat__toolbar-btn" @click.stop>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="5" r="2"/><circle cx="12" cy="12" r="2"/><circle cx="12" cy="19" r="2"/></svg>
                      </button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="copy">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                            <span>{{ t('aiKnowledge.chat.toolbar.copy') }}</span>
                          </el-dropdown-item>
                          <el-dropdown-item command="import-note">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="12" y1="18" x2="12" y2="12"/><line x1="9" y1="15" x2="15" y2="15"/></svg>
                            <span>{{ t('aiKnowledge.chat.toolbar.importNote') }}</span>
                          </el-dropdown-item>
                          <el-dropdown-item command="export-pdf">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><path d="M16 13H8M16 17H8M10 9H8"/></svg>
                            <span>{{ t('aiKnowledge.chat.toolbar.exportPdf') }}</span>
                          </el-dropdown-item>
                          <el-dropdown-item :command="msg.collapsed ? 'expand' : 'collapse'">
                            <svg v-if="msg.collapsed" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15"/></svg>
                            <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
                            <span>{{ t(msg.collapsed ? 'aiKnowledge.chat.toolbar.expand' : 'aiKnowledge.chat.toolbar.collapse') }}</span>
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </div>
              </div>

            </div>

            <!-- 右侧锚点导航 -->
            <div v-if="userAnchorMessages.length > 1" class="ak-chat__anchors" @click.stop>
              <div
                v-for="msg in userAnchorMessages"
                :key="msg.id"
                class="ak-chat__anchor-item"
                @click="scrollToMsg(msg.id)"
              >
                <div class="ak-chat__anchor-dot"></div>
                <div class="ak-chat__anchor-preview">{{ msg.content }}</div>
              </div>
            </div>
          </div>

          <!-- 输入区域 -->
          <div class="ak-chat__input-area">
              <div class="ak-chat__input-container" :class="{ 'is-sending': sending }">
                <div class="ak-chat__input-bar">
                  <!-- 附加功能按钮（上传文件等） -->
                  <el-button class="ak-chat__btn-plus" text circle @click="onPlusClick" :disabled="sending">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <line x1="12" y1="5" x2="12" y2="19"/>
                      <line x1="5" y1="12" x2="19" y2="12"/>
                    </svg>
                  </el-button>

                  <!-- 消息输入框，Enter 发送，Shift+Enter 换行 -->
                  <el-input
                    v-model="question"
                    type="textarea"
                    :rows="1"
                    autosize
                    :placeholder="t('aiKnowledge.chat.placeholder')"
                    :disabled="sending"
                    class="ak-chat__input-field"
                    @keydown.enter.exact.prevent="sendMessage"
                  />

                  <!-- 模型选择下拉 -->
                  <div class="ak-chat__model-select">
                    <el-select
                      v-model="chatProvider"
                      size="small"
                      popper-class="ak-chat__model-popper"
                      @change="onChatModelChange"
                    >
                      <el-option
                        v-for="opt in modelOptions"
                        :key="opt.key"
                        :label="opt.displayModel"
                        :value="opt.key"
                      >
                        <template #default>
                          <div class="ak-chat__model-option">
                            <span>{{ opt.displayModel }}</span>
                            <el-tag
                              v-if="opt.defaultProvider"
                              size="small"
                              type="warning"
                              effect="plain"
                              class="ak-chat__model-default-tag"
                            >
                              {{ t('aiKnowledge.settings.defaultModel') }}
                            </el-tag>
                          </div>
                        </template>
                      </el-option>
                    </el-select>
                  </div>
                </div>
                <!-- 输入区域底部 -->
                <div class="ak-chat__input-footer">
                  <el-checkbox v-model="useRag" size="small">
                    {{ t('aiKnowledge.chat.useRag') }}
                  </el-checkbox>
                  <div class="ak-chat__footer-right">
                    <span v-if="messages.length > memoryLimit" class="ak-chat__memory-warning">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="flex-shrink:0"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                      仅记忆最近 {{ memoryLimit }} 条对话，超出部分不会被 AI 记忆
                    </span>
                  </div>
                </div>
              </div>
            </div>
            <!-- Debug floating button -->
            <button
              class="ak-chat__debug-btn"
              :class="{ 'is-active': showDebugPanel }"
              @click="showDebugPanel = !showDebugPanel"
              title="调试信息"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>
            </button>
          </div>
          </div>
        </div>

        <!-- Debug Panel -->
        <Teleport to="body">
          <Transition name="debug-fade">
            <div v-if="showDebugPanel" class="ak-debug-overlay" @click.self="showDebugPanel = false">
              <div class="ak-debug-panel" @click.stop>
                <div class="ak-debug-panel__header">
                  <span class="ak-debug-panel__title">🛠 调试信息</span>
                  <button class="ak-debug-panel__close" @click="showDebugPanel = false">✕</button>
                </div>
                <div class="ak-debug-panel__body">
                  <div class="ak-debug-section">
                    <div class="ak-debug-section__header" @click="debugReqExpanded = !debugReqExpanded">
                      <span>📋 本次请求参数</span>
                      <span class="ak-debug-section__toggle">{{ debugReqExpanded ? '▾' : '▸' }}</span>
                    </div>
                    <div v-if="debugReqExpanded" class="ak-debug-section__body">
                      <div class="ak-debug-field">
                        <label>System</label>
                        <pre>你是一个有用的AI助手，请用中文回答用户的问题。</pre>
                      </div>
                      <div class="ak-debug-field" v-if="requestPayload?.history.length">
                        <label>History（{{ requestPayload.history.length }}条）</label>
                        <div v-for="(h, i) in requestPayload.history" :key="i" class="ak-debug-history-item">
                          <span class="ak-debug-role-tag" :class="h.role">{{ h.role === 'user' ? '用户' : 'AI' }}</span>
                          <pre>{{ h.content }}</pre>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="ak-debug-section">
                    <div class="ak-debug-section__header"><span>📊 上下文</span></div>
                    <div class="ak-debug-section__body">
                      <div class="ak-context-bar-wrap">
                        <div class="ak-context-bar">
                          <div class="ak-context-bar__fill" :style="{ width: contextPercentage + '%' }"
                               :class="{ 'ak-context-bar__fill--warn': contextPercentage >= 70, 'ak-context-bar__fill--danger': contextPercentage >= 90 }">
                          </div>
                          <span class="ak-context-bar__label">{{ contextPercentage }}%</span>
                        </div>
                        <div class="ak-context-bar__detail">
                          <span>{{ (currentContextTokens || 0).toLocaleString() }}</span>
                          <span class="ak-context-bar__separator">/</span>
                          <span>{{ (maxContextWindow || 0).toLocaleString() }}</span>
                          <span class="ak-context-bar__used-label">已使用</span>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="ak-debug-section">
                    <div class="ak-debug-section__header"><span>💬 消息</span></div>
                    <div class="ak-debug-section__body ak-debug-stats">
                      <div class="ak-debug-stat"><span>记忆消息数</span><strong>{{ memoryLimit }}</strong></div>
                      <div class="ak-debug-stat"><span>已存在消息</span><strong>{{ messages.length }}</strong></div>
                    </div>
                  </div>
                  <div class="ak-debug-section">
                    <div class="ak-debug-section__header"><span>💰 费用统计</span></div>
                    <div class="ak-debug-section__body">
                      <div class="ak-balance-row">
                        <span class="ak-balance-row__label">剩余金额</span>
                        <span class="ak-balance-row__value">{{ chatUsage?.remainingBalance != null ? '¥' + chatUsage.remainingBalance.toFixed(2) : 'N/A' }}</span>
                      </div>
                      <div class="ak-debug-stats">
                        <div class="ak-debug-stat"><span>总请求</span><strong>{{ chatUsage?.totalRequests ?? 0 }}</strong></div>
                        <div class="ak-debug-stat"><span>总Tokens</span><strong>{{ (chatUsage?.totalTokens ?? 0).toLocaleString() }}</strong></div>
                        <div class="ak-debug-stat"><span>今日请求</span><strong>{{ chatUsage?.todayRequests ?? 0 }}</strong></div>
                        <div class="ak-debug-stat"><span>今日Tokens</span><strong>{{ (chatUsage?.todayTokens ?? 0).toLocaleString() }}</strong></div>
                      </div>
                    </div>
                    <div style="margin-top: 8px; text-align: right; font-size: 12px;">
                      <a href="https://platform.deepseek.com/usage" target="_blank" rel="noopener"
                         style="color: #409eff; text-decoration: none;">
                        DeepSeek 官网用量详情 ↗
                      </a>
                    </div>
                  </div>
                  <div class="ak-debug-actions">
                    <button class="ak-debug-btn ak-debug-btn--danger" @click="clearConversation">清空对话</button>
                    <button class="ak-debug-btn ak-debug-btn--primary" :disabled="compressing" @click="compressConversation">
                      {{ compressing ? '⏳ 压缩中...' : '压缩对话' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </Transition>
        </Teleport>
      </el-tab-pane>

      <!-- ========== RAG 閻儴鐦戞惔?========== -->
      <el-tab-pane :label="t('aiKnowledge.tabs.rag')" name="rag">
        <div class="ai-knowledge__tab-content">
          <div class="ak-rag">
            <!-- RAG 知识库统计 -->
            <section v-if="ragStats" class="ak-rag__stats">
              <div class="ak-stat-card is-green">
                <div class="ak-stat-card__icon">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                    <line x1="16" y1="13" x2="8" y2="13"/>
                    <line x1="16" y1="17" x2="8" y2="17"/>
                    <polyline points="10 9 9 9 8 9"/>
                  </svg>
                </div>
                <div class="ak-stat-card__body">
                  <div class="ak-stat-card__label">{{ t('aiKnowledge.rag.totalDocs') }}</div>
                  <div class="ak-stat-card__value">{{ ragStats.totalDocs }}</div>
                </div>
              </div>
              <div class="ak-stat-card is-blue">
                <div class="ak-stat-card__icon">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="9 11 12 14 22 4"/>
                    <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
                  </svg>
                </div>
                <div class="ak-stat-card__body">
                  <div class="ak-stat-card__label">{{ t('aiKnowledge.rag.readyCount') }}</div>
                  <div class="ak-stat-card__value">{{ ragStats.readyCount }}</div>
                </div>
              </div>
              <div class="ak-stat-card is-orange">
                <div class="ak-stat-card__icon">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/>
                    <polyline points="12 6 12 12 16 14"/>
                  </svg>
                </div>
                <div class="ak-stat-card__body">
                  <div class="ak-stat-card__label">{{ t('aiKnowledge.rag.processingCount') }}</div>
                  <div class="ak-stat-card__value">{{ ragStats.processingCount }}</div>
                </div>
              </div>
              <div class="ak-stat-card is-red">
                <div class="ak-stat-card__icon">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="15" y1="9" x2="9" y2="15"/>
                    <line x1="9" y1="9" x2="15" y2="15"/>
                  </svg>
                </div>
                <div class="ak-stat-card__body">
                  <div class="ak-stat-card__label">{{ t('aiKnowledge.rag.failedCount') }}</div>
                  <div class="ak-stat-card__value">{{ ragStats.failedCount }}</div>
                </div>
              </div>
              <div class="ak-stat-card is-purple">
                <div class="ak-stat-card__icon">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
                  </svg>
                </div>
                <div class="ak-stat-card__body">
                  <div class="ak-stat-card__label">{{ t('aiKnowledge.rag.totalChunks') }}</div>
                  <div class="ak-stat-card__value">{{ ragStats.totalChunks }}</div>
                </div>
              </div>
            </section>

            <!-- 濞村鐦Λ鈧槐?-->
            <section class="ak-rag__search war-room-panel">
              <div class="ak-rag__search-row">
                <el-input
                  v-model="ragSearchQuery"
                  :placeholder="t('aiKnowledge.rag.searchPlaceholder')"
                  style="flex: 1"
                  @keydown.enter="doRagSearch"
                />
                <el-button type="primary" @click="doRagSearch">
                  {{ t('aiKnowledge.rag.search') }}
                </el-button>
              </div>
              <!-- 濡偓缁便垻绮ㄩ弸?-->
              <div v-if="ragSearchResults.length" class="ak-rag__search-results">
                <div
                  v-for="(src, i) in ragSearchResults"
                  :key="i"
                  class="ak-rag__search-result"
                >
                  <div class="ak-rag__search-result-head">
                    <span class="ak-rag__search-result-file">{{ src.fileName }}</span>
                    <el-tag size="small" effect="plain">
                      {{ (src.score * 100).toFixed(1) }}%
                    </el-tag>
                  </div>
                  <p class="ak-rag__search-result-content">{{ src.content }}</p>
                </div>
              </div>
            </section>

            <!-- 閺傚洦銆傞崚妤勩€� -->
            <section class="ak-rag__docs war-room-panel">
              <div class="ak-rag__docs-head">
                <h3 class="ak-rag__section-title">{{ t('aiKnowledge.rag.documentList') }}</h3>
                <div style="display: flex; gap: 8px;">
                  <el-button
                    size="small"
                    type="primary"
                    :loading="uploading"
                    @click="triggerUpload"
                  >
                    {{ t('aiKnowledge.rag.upload') }}
                  </el-button>
                  <input
                    ref="fileInputRef"
                    type="file"
                    accept=".pdf,.txt,.md,.html,.htm,.docx"
                    style="display: none"
                    @change="handleFileSelected"
                  />
                  <el-button
                    size="small"
                    :loading="rebuilding"
                    @click="rebuildIndex"
                  >
                    {{ t('aiKnowledge.rag.rebuildIndex') }}
                  </el-button>
                </div>
              </div>

              <el-table
                v-loading="docsLoading"
                :data="ragDocuments"
                border
                stripe
                style="width: 100%"
                empty-text=""
              >
                <el-table-column :label="t('aiKnowledge.rag.fileName')" prop="fileName" min-width="200" show-overflow-tooltip />
                <el-table-column :label="t('aiKnowledge.rag.fileType')" prop="fileType" width="100" />
                <el-table-column :label="t('aiKnowledge.rag.chunkCount')" prop="chunkCount" width="100" align="center" />
                <el-table-column :label="t('aiKnowledge.rag.status')" width="130" align="center">
                  <template #default="{ row }">
                    <el-tag :type="statusTagType(row.status)" effect="plain" size="small">
                      {{ statusLabel(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column :label="t('aiKnowledge.rag.indexedAt')" prop="indexedAt" width="170" align="center">
                  <template #default="{ row }">
                    <span class="ak-rag__time">{{ row.indexedAt || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column :label="t('aiKnowledge.rag.actions')" width="140" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button
                      v-if="row.status === 'failed'"
                      size="small"
                      text
                      type="primary"
                      :loading="retryingId === row.id"
                      @click="retryDoc(row.id)"
                    >
                      {{ t('aiKnowledge.rag.retry') }}
                    </el-button>
                    <el-button
                      size="small"
                      text
                      type="danger"
                      @click="removeDoc(row.id)"
                    >
                      {{ t('aiKnowledge.rag.remove') }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <!-- 缁岃櫣濮搁幀?-->
              <div v-if="ragDocuments.length === 0 && !docsLoading" class="ak-rag__empty">
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="color: #9ca3af;">
                  <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/>
                  <polyline points="13 2 13 9 20 9"/>
                </svg>
                <p>{{ t('aiKnowledge.rag.empty') }}</p>
              </div>
            </section>

            <!-- Embedding 配置（独立于 Chat 配置） -->
            <section class="ak-rag__embed-config war-room-panel" style="margin-top: 16px;">
              <div style="display: flex; align-items: center; justify-content: space-between; cursor: pointer;" @click="embedConfigExpanded = !embedConfigExpanded">
                <h3 class="ak-rag__section-title" style="margin: 0;">
                  {{ t('aiKnowledge.settings.embeddingConfig') || 'Embedding 配置' }}
                </h3>
                <el-tag v-if="embeddingConfigured" type="success" size="small" effect="plain">已配置</el-tag>
                <el-tag v-else type="warning" size="small" effect="plain">未配置</el-tag>
              </div>
              <el-collapse-transition>
                <div v-show="embedConfigExpanded" style="padding-top: 12px;">
                  <el-form
                    ref="embedConfigFormRef"
                    :model="embedConfigDraft"
                    label-width="120px"
                    size="small"
                  >
                    <el-form-item label="提供商" prop="provider">
                      <el-select v-model="embedConfigDraft.provider" style="width: 100%">
                        <el-option
                          v-for="(info, key) in AI_PROVIDER_MAP"
                          :key="key"
                          :label="info.label"
                          :value="key"
                        />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="API Key" prop="apiKey">
                      <el-input
                        v-model="embedConfigDraft.apiKey"
                        type="password"
                        show-password
                        :placeholder="`${providerInfo?.label || ''} API Key（用于生成嵌入向量）`"
                      />
                    </el-form-item>
                    <el-form-item label="API 地址" prop="apiBaseUrl">
                      <el-input
                        v-model="embedConfigDraft.apiBaseUrl"
                        :placeholder="AI_PROVIDER_MAP[embedConfigDraft.provider]?.apiBaseUrl || 'https://api.openai.com/v1'"
                      />
                    </el-form-item>
                    <el-form-item label="Embedding 模型" prop="embeddingModel">
                      <el-input
                        v-model="embedConfigDraft.embeddingModel"
                        :placeholder="AI_PROVIDER_MAP[embedConfigDraft.provider]?.embeddingModel || 'text-embedding-3-small'"
                      />
                    </el-form-item>
                    <el-form-item>
                      <el-button
                        type="primary"
                        :loading="savingEmbedConfig"
                        @click="saveEmbedConfig"
                      >
                        保存 Embedding 配置
                      </el-button>
                      <el-button
                        v-if="embeddingConfigured"
                        type="danger"
                        plain
                        :loading="clearingEmbedConfig"
                        @click="clearEmbedConfig"
                      >
                        清除配置
                      </el-button>
                    </el-form-item>
                  </el-form>
                </div>
              </el-collapse-transition>
            </section>
          </div>
        </div>
      </el-tab-pane>

      <!-- ========== 设置 ========== -->
      <el-tab-pane :label="t('aiKnowledge.tabs.settings')" name="settings">
        <div class="ai-knowledge__tab-content">
          <div class="ak-settings war-room-panel" style="max-width: 600px;">
            <h3 class="ak-rag__section-title">{{ t('aiKnowledge.settings.title') }}</h3>

            <el-form
              ref="configFormRef"
              :model="configDraft"
              :rules="configRules"
              label-width="140px"
              class="ak-settings__form"
            >
              <!-- 閹绘劒绶甸崯?-->
              <el-form-item :label="t('aiKnowledge.settings.provider')" prop="provider">
                <el-select v-model="configDraft.provider" style="width: 100%" @change="onProviderChange">
                  <el-option
                    v-for="(info, key) in AI_PROVIDER_MAP"
                    :key="key"
                    :label="info.label"
                    :value="key"
                  />
                </el-select>
              </el-form-item>

              <!-- API Key -->
              <el-form-item :label="t('aiKnowledge.settings.apiKey')" prop="apiKey">
                <el-input
                  v-model="configDraft.apiKey"
                  type="password"
                  show-password
                  :placeholder="`${providerInfo?.label} API Key`"
                />
                <template #help>
                  <div v-if="providerInfo?.apiKeyHint" class="ak-settings__hint">
                    获取地址：{{ providerInfo.apiKeyHint }}
                  </div>
                </template>
              </el-form-item>

              <!-- API 地址 -->
              <el-form-item :label="t('aiKnowledge.settings.apiBaseUrl')" prop="apiBaseUrl">
                <el-input
                  v-model="configDraft.apiBaseUrl"
                  :placeholder="providerInfo?.apiBaseUrl || t('aiKnowledge.settings.apiBaseUrlPlaceholder')"
                />
              </el-form-item>

              <!-- 模型选择 -->
              <el-form-item :label="t('aiKnowledge.settings.model')" prop="model">
                <el-input
                  v-model="configDraft.model"
                  :placeholder="providerInfo?.model || t('aiKnowledge.settings.modelPlaceholder')"
                />
              </el-form-item>

              <!-- Temperature -->
              <el-form-item :label="t('aiKnowledge.settings.temperature')" prop="temperature">
                <div class="ak-settings__slider-row">
                  <el-slider
                    v-model="configDraft.temperature"
                    :min="0"
                    :max="2"
                    :step="0.1"
                    style="flex: 1; margin-right: 12px"
                  />
                  <span class="ak-settings__slider-value">{{ configDraft.temperature.toFixed(1) }}</span>
                </div>
                <div class="ak-settings__hint">{{ t('aiKnowledge.settings.temperatureHint') }}（推荐：{{ providerInfo?.temperature || '0.7' }}）</div>
              </el-form-item>

              <!-- Max Tokens -->
              <el-form-item :label="t('aiKnowledge.settings.maxTokens')" prop="maxTokens">
                <el-input-number
                  v-model="configDraft.maxTokens"
                  :min="256"
                  :max="128000"
                  :step="1024"
                  style="width: 100%"
                />
                <div class="ak-settings__hint">{{ t('aiKnowledge.settings.maxTokensHint') }}（推荐：{{ providerInfo?.maxTokens || '4096' }}）</div>
              </el-form-item>

              <!-- Embedding 模型 -->
              <el-form-item :label="t('aiKnowledge.settings.embeddingModel')" prop="embeddingModel">
                <el-input
                  v-model="configDraft.embeddingModel"
                  :placeholder="providerInfo?.embeddingModel || t('aiKnowledge.settings.embeddingModelPlaceholder')"
                />
              </el-form-item>

              <!-- 上下文消息数量设置 -->
              <el-form-item :label="t('aiKnowledge.settings.maxContextMessages')" prop="maxContextMessages">
                <div class="ak-settings__slider-row">
                  <el-slider
                    v-model="configDraft.maxContextMessages"
                    :min="0"
                    :max="50"
                    :step="1"
                    style="flex: 1; margin-right: 12px"
                    show-stops
                  />
                  <span class="ak-settings__slider-value">{{ configDraft.maxContextMessages ?? 10 }}条</span>
                </div>
                <div class="ak-settings__hint">{{ t('aiKnowledge.settings.maxContextMessagesHint') }}</div>
              </el-form-item>

              <!-- 设为默认配置 -->
              <el-form-item>
                <el-checkbox v-model="configDraft.defaultProvider" size="small">
                  {{ t('aiKnowledge.settings.setAsDefault') }}
                </el-checkbox>
              </el-form-item>

              <!-- 閹垮秳缍旈幐澶愭尦 -->
              <el-form-item>
                <div class="ak-settings__actions">
                  <el-button
                    type="primary"
                    :loading="savingConfig"
                    @click="saveConfig"
                  >
                    {{ t('aiKnowledge.settings.saveConfig') }}
                  </el-button>
                  <el-button
                    :loading="testingConnection"
                    @click="testConnection"
                  >
                    {{ t('aiKnowledge.settings.testConnection') }}
                  </el-button>
                </div>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
/**
 * AI 閻儴鐦戞惔鎾汇€夐棃?
 * 包含 AI 对话、RAG 知识库和设置三大功能
 */
import { computed, nextTick, ref, watch } from 'vue'
import { marked } from 'marked'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  fetchAiModelConfig,
  saveAiModelConfig,
  fetchRagStats,
  fetchRagDocuments,
  retryRagDocument,
  removeRagDocument,
  searchRag,
  rebuildRagIndex,
  uploadRagDocument,
  sendChatMessage,
  sendChatMessageStream,
  fetchAiProviders,
  fetchEmbeddingConfig,
  saveEmbeddingConfig,
  AI_PROVIDER_MAP,
  type AiProvider,
  type AiModelConfig,
  type ChatMessage,
  type ProviderInfo,
  type RagStats,
  type RagDocument,
  type RagSource,
  fetchChatCategories,
  createChatCategory,
  renameChatCategory,
  deleteChatCategory,
  createChatConversation,
  updateChatConversation,
  deleteChatConversation,
  searchChatConversations,
  fetchChatUsage,
  type ChatCategoryVO,
  type ChatConversationVO,
  type ChatSearchResult,
  type ChatUsageVO,
} from '@/api/aiKnowledge'
import {
  fetchNotebookTree,
  createNotebook,
  createNoteRequest,
  type NbTreeNode,
} from '@/api/notebook'

const { t } = useI18n()

// ========== 标签页状态 ==========
const activeTab = ref('chat')

function onTabChange() {
  if (activeTab.value === 'chat') {
    loadProviders()
  } else if (activeTab.value === 'rag') {
    loadRagData()
    loadEmbeddingConfig()
  } else if (activeTab.value === 'settings') {
    loadConfig()
  }
}

// ========== 对话 ==========
const question = ref('')
const useRag = ref(false)

/** AI provider 列表 */
const providerList = ref<ProviderInfo[]>([])
const providersLoading = ref(false)

async function loadProviders() {
  if (providersLoading.value) return
  providersLoading.value = true
  try {
    const data = await fetchAiProviders()
    providerList.value = data
    // 获取 providerList 后，若当前 chatProvider 不在列表中则切换到第一个
    if (data.length > 0 && !data.some(p => p.provider === chatProvider.value)) {
      chatProvider.value = data[0].provider
    }
  } catch {
    // 加载 providerList 失败，使用 fallback 数据
  } finally {
    providersLoading.value = false
  }
}

const sending = ref(false)
const messages = ref<(ChatMessage & { sources?: RagSource[]; collapsed?: boolean })[]>([])
const chatMessagesRef = ref<HTMLElement | null>(null)

/** 用户消息列表（用于右侧锚点导航） */
const userAnchorMessages = computed(() => messages.value.filter(m => m.role === 'user'))
const memoryLimit = computed(() => {
  const info = providerList.value.find(p => p.provider === chatProvider.value)
  return info?.maxContextMessages ?? 10
})

// ========== 调试面板 ==========
const showDebugPanel = ref(false)
const debugReqExpanded = ref(false)
const chatUsage = ref<ChatUsageVO | null>(null)

/** 当前会话累计消耗的 Token 数（从 API 返回的真实数据），用于计算上下文占用百分比 */
const currentContextTokens = ref(0)


/** 从已加载的消息中重新计算当前会话的 Token 消耗 */
function recalcContextTokens() {
  let total = 0
  for (const m of messages.value) {
    if (m.tokens) total += m.tokens
  }
  currentContextTokens.value = total
}

/** 当前提供商的大模型上下文窗口大小 */
const maxContextWindow = computed(() => {
  const info = providerList.value.find(p => p.provider === chatProvider.value)
  return info?.maxContextTokens ?? 0
})

/** 上下文占用百分比 */
const contextPercentage = computed(() => {
  if (!maxContextWindow.value || !currentContextTokens.value) return 0
  return Math.min(100, Math.round((currentContextTokens.value / maxContextWindow.value) * 100))
})


const requestPayload = computed(() => {
  if (messages.value.length === 0) return null
  const memLimit = memoryLimit.value
  const history = messages.value.slice(-memLimit).map(m => ({
    role: m.role,
    content: m.content.length > 300 ? m.content.slice(0, 300) + '...' : m.content,
  }))
  return { history }
})

// const windowTokens = computed(() => {
//   const payload = requestPayload.value
//   if (!payload) return 0
//   let total = estimateTokens('你是一个有用的AI助手，请用中文回答用户的问题。')
//   for (const h of payload.history) {
//     total += estimateTokens(h.content)
//   }
//   return total
// })

async function loadChatUsage() {
  try {
    chatUsage.value = await fetchChatUsage()
  } catch { /* ignore */ }
}

/** 滚动到指定消息 */
function scrollToMsg(msgId: string) {
  const el = chatMessagesRef.value?.querySelector(`[data-msg-id="${msgId}"]`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}



let msgCounter = 0

// ========== 对话分类 & 列表 ==========

const categories = ref<ChatCategoryVO[]>([])
const activeCatId = ref<string | null>(null)
const activeConvId = ref<string | null>(null)
const categoryExpanded = ref<Record<string, boolean>>({})
const renamingCatId = ref<string | null>(null)
const renamingConvId = ref<string | null>(null)
const catRenameInput = ref('')
const convRenameInput = ref('')
const loadingCategories = ref(false)

// ========== 全局搜索 ==========
const searchQuery = ref('')
const searchResults = ref<import('@/api/aiKnowledge').ChatSearchResult[]>([])
const searching = ref(false)
let searchTimeout: ReturnType<typeof setTimeout> | null = null

async function doSearch(keyword: string) {
  if (!keyword.trim()) {
    searchResults.value = []
    return
  }
  searching.value = true
  try {
    searchResults.value = await searchChatConversations(keyword.trim())
  } catch {
    searchResults.value = []
  } finally {
    searching.value = false
  }
}

function handleSearchInput(val: string) {
  if (searchTimeout) clearTimeout(searchTimeout)
  if (!val.trim()) {
    searchResults.value = []
    return
  }
  searchTimeout = setTimeout(() => doSearch(val), 300)
}

function clearSearch() {
  searchQuery.value = ''
  searchResults.value = []
  if (searchTimeout) clearTimeout(searchTimeout)
}

async function switchToSearchResult(r: ChatSearchResult) {
  const keyword = searchQuery.value // 在 clearSearch 之前保存搜索关键词
  clearSearch()
  // 确保分类已展开
  categoryExpanded.value[r.categoryId] = true
  // 如果该分类不在 categories 中，先加载
  let cat = categories.value.find(c => c.id === r.categoryId)
  if (!cat) {
    await loadCategoriesFromServer()
    cat = categories.value.find(c => c.id === r.categoryId)
  }
  if (cat) {
    const conv = cat.conversations.find(c => c.id === r.conversationId)
    if (conv) {
      await switchConversation(r.categoryId, r.conversationId)
      scrollToFirstMatchingMessage(keyword)
    } else {
      // 对话不在当前列表，直接通过 API 加载
      activeCatId.value = r.categoryId
      activeConvId.value = r.conversationId
      try {
        const data = await fetchChatCategories()
        categories.value = data || []
        const foundCat = data?.find(c => c.id === r.categoryId)
        const foundConv = foundCat?.conversations.find(c => c.id === r.conversationId)
        if (foundCat && foundConv) {
          messages.value = JSON.parse(foundConv.messages || '[]')
          recalcContextTokens()
        } else {
          messages.value = []
          currentContextTokens.value = 0
        }
      } catch {
        messages.value = []
        currentContextTokens.value = 0
      }
      saveActiveSession()
      scrollToFirstMatchingMessage(keyword)
    }
  }
}

/** 在当前 messages 中查找包含关键词的消息，滚动到该位置 */
function scrollToFirstMatchingMessage(keyword: string) {
  if (!keyword) return
  const lowerKw = keyword.toLowerCase()
  const match = messages.value.find(m => m.content.toLowerCase().includes(lowerKw))
  if (match) {
    nextTick(() => {
      scrollToMsg(match.id)
    })
  }
}

/** 持久化当前激活的对话到 localStorage（仅前端记住上次打开的对话） */
function saveActiveSession() {
  try {
    localStorage.setItem('ak-active-session', JSON.stringify({ catId: activeCatId.value, convId: activeConvId.value }))
  } catch { /* ignore */ }
}

/** 获取上次激活的对话 */
function loadActiveSession(): { catId: string; convId: string } | null {
  try {
    const raw = localStorage.getItem('ak-active-session')
    if (!raw) return null
    const parsed = JSON.parse(raw)
    return {
      catId: String(parsed.catId),
      convId: String(parsed.convId),
    }
  } catch { return null }
}

/** 将当前 messages 同步到激活对话 */
async function syncMessagesToConversation() {
  if (!activeCatId.value || !activeConvId.value) return
  const cat = categories.value.find(c => c.id === activeCatId.value)
  if (!cat) return
  const conv = cat.conversations.find(c => c.id === activeConvId.value)
  if (!conv) return
  // 更新标题（从第一条用户消息）
  if (!conv.title) {
    const firstUser = messages.value.find(m => m.role === 'user')
    if (firstUser) {
      conv.title = firstUser.content.slice(0, 30).replace(/\n/g, ' ').trim()
    }
  }
  // 保存到后端
  try {
    await updateChatConversation(conv.id, {
      title: conv.title,
      messages: JSON.stringify(messages.value.filter(m => !(m.role === 'assistant' && !m.content))),
    })
  } catch { /* ignore */ }
}

/** 保存对话 */
async function saveMessages() {
  await syncMessagesToConversation()
}

/** 从后端加载分类和对话 */
async function loadCategoriesFromServer() {
  if (loadingCategories.value) return
  loadingCategories.value = true
  try {
    const data = await fetchChatCategories()
    categories.value = data || []
    data?.forEach(c => { categoryExpanded.value[c.id] = true })

    if (data && data.length > 0) {
      // 尝试恢复上次激活的对话
      const active = loadActiveSession()
      if (active) {
        const cat = data.find(c => c.id === active.catId)
        const conv = cat?.conversations.find(c => c.id === active.convId)
        if (cat && conv) {
          activeCatId.value = cat.id
          activeConvId.value = conv.id
          try {
            messages.value = JSON.parse(conv.messages || '[]')
            recalcContextTokens()
          } catch { messages.value = [] }
          return
        }
      }
      // 默认选中第一个对话
      const firstCat = data[0]
      if (firstCat.conversations.length > 0) {
        const firstConv = firstCat.conversations[0]
        activeCatId.value = firstCat.id
        activeConvId.value = firstConv.id
        try {
          messages.value = JSON.parse(firstConv.messages || '[]')
          recalcContextTokens()
        } catch { messages.value = [] }
      } else {
        // 分类下无对话，自动创建
        await addConversation(firstCat.id)
      }
    } else {
      // 无数据，创建默认
      await createDefaultData()
    }
  } catch {
    await createDefaultData()
  } finally {
    loadingCategories.value = false
  }
}

/** 切换到指定对话 */
async function switchConversation(catId: string, convId: string) {
  if (sending.value) return
  await syncMessagesToConversation()
  const cat = categories.value.find(c => c.id === catId)
  if (!cat) return
  const conv = cat.conversations.find(c => c.id === convId)
  if (!conv) return
  activeCatId.value = catId
  activeConvId.value = convId
  try {
    messages.value = JSON.parse(conv.messages || '[]')
    recalcContextTokens()
  } catch { messages.value = [] }
  saveActiveSession()
}

/** 新增分类 */
async function addCategory() {
  try {
    const { value } = await ElMessageBox.prompt('请输入分类名称', '新增分类', {
      confirmButtonText: '确定', cancelButtonText: '取消',
      inputPattern: /\S/, inputErrorMessage: '分类名称不能为空',
    })
    const cat = await createChatCategory(value.trim())
    categories.value.push(cat)
    categoryExpanded.value[cat.id] = true
  } catch { /* cancelled */ }
}

/** 重命名分类 */
function startRenameCategory(catId: string) {
  const cat = categories.value.find(c => c.id === catId)
  if (!cat) return
  renamingCatId.value = catId
  catRenameInput.value = cat.name
  nextTick(() => {
    const el = document.querySelector('.ak-sidebar__rename-input') as HTMLElement
    el?.focus()
  })
}
async function confirmRenameCategory(catId: string) {
  const name = catRenameInput.value.trim()
  if (!name) { renamingCatId.value = null; return }
  try {
    await renameChatCategory(catId, name)
    const cat = categories.value.find(c => c.id === catId)
    if (cat) cat.name = name
  } catch { /* ignore */ }
  renamingCatId.value = null
}
function cancelRenameCategory() { renamingCatId.value = null }

/** 删除分类 */
async function deleteCategory(catId: string) {
  if (sending.value) return
  const idx = categories.value.findIndex(c => c.id === catId)
  if (idx === -1) return
  try {
    await ElMessageBox.confirm('确定要删除此分类及其所有对话吗？', '删除分类', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
    })
    await deleteChatCategory(catId)
    categories.value.splice(idx, 1)
    if (activeCatId.value === catId) {
      if (categories.value.length > 0) {
        const fc = categories.value[0]
        if (fc.conversations.length > 0) {
          await switchConversation(fc.id, fc.conversations[0].id)
        } else {
          await addConversation(fc.id)
        }
      } else { await createDefaultData() }
    }
  } catch { /* cancelled or error */ }
}

function handleCategoryCommand(cmd: string, cat: ChatCategoryVO) {
  if (cmd === 'rename') startRenameCategory(cat.id)
  else if (cmd === 'delete') deleteCategory(cat.id)
}

function handleConversationCommand(cmd: string, catId: string, conv: ChatConversationVO) {
  if (cmd === 'rename') startRenameConversation(conv.id)
  else if (cmd === 'delete') deleteConversation(catId, conv.id)
}

function toggleCategoryExpand(catId: string) {
  categoryExpanded.value[catId] = !categoryExpanded.value[catId]
}

/** 新增对话 */
async function addConversation(catId: string) {
  if (sending.value) return
  const cat = categories.value.find(c => c.id === catId)
  if (!cat) return
  try {
    const conv = await createChatConversation(catId)
    cat.conversations.push(conv)
    categoryExpanded.value[catId] = true
    activeCatId.value = catId
    activeConvId.value = conv.id
    messages.value = []
    currentContextTokens.value = 0
    saveActiveSession()
  } catch { /* ignore */ }
}

/** 删除对话 */
async function deleteConversation(catId: string, convId: string) {
  if (sending.value) return
  const cat = categories.value.find(c => c.id === catId)
  if (!cat) return
  const idx = cat.conversations.findIndex(c => c.id === convId)
  if (idx === -1) return
  try {
    await ElMessageBox.confirm('确定要删除此对话吗？', '删除对话', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
    })
    await deleteChatConversation(convId)
    cat.conversations.splice(idx, 1)
    if (activeCatId.value === catId && activeConvId.value === convId) {
      if (cat.conversations.length > 0) {
        await switchConversation(catId, cat.conversations[0].id)
      } else {
        await addConversation(catId)
      }
    }
  } catch { /* cancelled or error */ }
}

/** 重命名对话 */
function startRenameConversation(convId: string) {
  const all = categories.value.flatMap(c => c.conversations)
  const conv = all.find(c => c.id === convId)
  if (!conv) return
  renamingConvId.value = convId
  convRenameInput.value = conv.title
  nextTick(() => {
    const el = document.querySelector('.ak-sidebar__rename-input') as HTMLElement
    el?.focus()
  })
}
async function confirmRenameConversation(catId: string, convId: string) {
  const title = convRenameInput.value.trim()
  if (!title) { renamingConvId.value = null; return }
  try {
    await updateChatConversation(convId, { title })
    const cat = categories.value.find(c => c.id === catId)
    if (!cat) return
    const conv = cat.conversations.find(c => c.id === convId)
    if (conv) conv.title = title
  } catch { /* ignore */ }
  renamingConvId.value = null
}
function cancelRenameConversation() { renamingConvId.value = null }

/** 创建默认分类和对话 */
async function createDefaultData() {
  try {
    const cat = await createChatCategory('默认分类')
    const conv = await createChatConversation(cat.id)
    categories.value = [cat]
    categoryExpanded.value[cat.id] = true
    activeCatId.value = cat.id
    activeConvId.value = conv.id
    messages.value = []
    currentContextTokens.value = 0
    saveActiveSession()
  } catch { /* ignore */ }
}

// 页面加载时从后端恢复数据
loadCategoriesFromServer()
loadChatUsage()

function genMsgId() {
  return `msg_${Date.now()}_${++msgCounter}`
}

function renderMessage(msg: ChatMessage): string {
  let html = marked.parse(msg.content) as string
  // 正则匹配纯 emoji 开头的段落，添加特殊类名
  html = html.replace(
    /<p>([\u{1F300}-\u{1FAD6}\u{2600}-\u{27BF}\u{2700}-\u{27BF}]\s)/gu,
    '<p class="ak-emoji-heading">$1',
  )
  return html
}

function scrollChatToBottom() {
  void nextTick(() => {
    const el = chatMessagesRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function sendMessage() {
  const text = question.value.trim()
  if (!text || sending.value) return

  const userMsg: ChatMessage = {
    id: genMsgId(),
    role: 'user',
    content: text,
    timestamp: Date.now(),
  }
  messages.value.push(userMsg)
  saveMessages()
  question.value = ''
  sending.value = true
  scrollChatToBottom()

  // 预创建助手消息占位
  const assistantMsg: ChatMessage & { sources?: RagSource[] } = {
    id: genMsgId(),
    role: 'assistant',
    content: '',
    timestamp: Date.now(),
  }
  messages.value.push(assistantMsg)
  scrollChatToBottom()

  sendChatMessageStream(
    {
      question: text,
      provider: chatProvider.value,
      useRag: useRag.value,
      history: messages.value.slice(-memoryLimit.value).map(m => ({ role: m.role, content: m.content })),
    },
    (fullText) => {
      assistantMsg.content = fullText
      messages.value = [...messages.value]
      scrollChatToBottom()
    },
    () => {
      sending.value = false
      saveMessages()
      scrollChatToBottom()
      loadChatUsage()
    },
    (err) => {
      if (!assistantMsg.content) {
        assistantMsg.content = '\u9519\u8bef\uff1a' + err
      }
      sending.value = false
      saveMessages()
      scrollChatToBottom()
      loadChatUsage()
    },
    (tokens) => {
      assistantMsg.tokens = tokens
      currentContextTokens.value += tokens
      messages.value = [...messages.value]
    },
  )}

/** 清空当前对话 */
async function clearConversation() {
  if (!activeConvId.value) return
  try {
    await ElMessageBox.confirm('确定要清空当前对话的所有消息吗？此操作不可撤销。', '清空对话', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning',
    })
    messages.value = []
    currentContextTokens.value = 0
    await saveMessages()
  } catch {
    // 用户取消不做任何事
  }
}

/** 压缩对话 — 调用 LLM 对历史做摘要，保留最近一轮问答完整 */
const compressing = ref(false)

async function compressConversation() {
  if (messages.value.length <= 2 || compressing.value) return
  compressing.value = true
  try {
    const KEEP = 2
    const toCompress = messages.value.slice(0, -KEEP)
    const recent = messages.value.slice(-KEEP)

    // 构造摘要提示
    const historyText = toCompress
      .map(m => (m.role === 'user' ? '用户' : m.role === 'assistant' ? 'AI' : '系统') + '：' + m.content)
      .join('\n\n---\n\n')

    const res = await sendChatMessage({
      question: `请用中文简洁总结以下对话历史的核心内容、已解决的问题和待办事项，保留所有关键事实和决策信息，以便后续继续对话时不需要重复询问。\n\n${historyText}`,
      provider: chatProvider.value,
      useRag: false,
    })

    // 替换为一条 system 摘要 + 最近一轮问答
    messages.value = [
      { id: genMsgId(), role: 'system', content: '📋 对话摘要：\n' + res.answer, timestamp: Date.now() } as ChatMessage,
      ...recent,
    ]
    // 用已有消息的 token 数重新计算（摘要消息无 tokens，仅计算保留的最近消息）
    recalcContextTokens()
    await saveMessages()
  } catch (e) {
    console.warn('对话压缩失败', e)
  } finally {
    compressing.value = false
  }
}

/** 将 PDF Markdown 转换为 HTML */
function formatPdfBody(md: string, options?: { title?: string; date?: string }): string {
  let html = marked.parse(md) as string

  // 为不同标题级别添加图标前缀
  const icons: Record<number, string> = { 1: '🗂️ ', 2: '📌 ', 3: '🔸 ', 4: '💡 ' }
  for (let i = 1; i <= 4; i++) {
    if (icons[i]) {
      html = html.replace(new RegExp(`<h${i}(\\s[^>]*)?>`, 'g'), `<h${i}$1>${icons[i]}`)
    }
  }

  // 为打印 PDF 添加额外样式
  html = html
    .replace(/<h1\b/g, '<h1 style="color:#991b1b;font-size:21px;font-weight:700;border-bottom:2px solid #fca5a5;padding-bottom:8px"')
    .replace(/<h2\b/g, '<h2 style="color:#c2410c;font-size:18px;font-weight:700;border-bottom:2px solid #fdba74;padding-bottom:6px"')
    .replace(/<h3\b/g, '<h3 style="color:#a16207;font-size:16px;font-weight:600"')
    .replace(/<h4\b/g, '<h4 style="color:#7c3aed;font-size:15px;font-weight:600"')
    .replace(/<strong\b(?!\sstyle)/g, '<strong style="color:#b91c1c"')
    .replace(/<em\b(?!\sstyle)/g, '<em style="color:#d97706;font-style:italic"')
    .replace(/<blockquote\b/g, '<blockquote style="margin:12px 0;padding:10px 18px;border-left:4px solid #f59e0b;background:#fffbeb;color:#92400e;border-radius:0 8px 8px 0"')
    .replace(/<pre\b(?!\sstyle)/g, '<pre style="background:#18181b;color:#e4e4e7;padding:14px 18px;border-radius:8px;overflow-x:auto;margin:12px 0;font-size:13px;line-height:1.6;border:1px solid #27272a"')
    .replace(/<table\b/g, '<table style="border-collapse:collapse;width:100%;margin:14px 0;font-size:13px;border:1px solid #e5e7eb"')
    .replace(/<th\b/g, '<th style="background:#f59e0b;color:#fff;font-weight:600;padding:10px 14px;text-align:left;border:1px solid #d97706"')
    .replace(/<td\b/g, '<td style="padding:9px 14px;border:1px solid #e5e7eb"')
    .replace(/<hr\b/g, '<hr style="border:none;height:2px;background:linear-gradient(to right,transparent,#f59e0b,transparent);margin:18px 0"')

  const metaParts: string[] = []
  if (options?.title) {
    metaParts.push(`<div style="font-size:22px;font-weight:700;color:#991b1b;padding-bottom:10px;border-bottom:3px solid #fca5a5;margin-bottom:6px">${options.title}</div>`)
  }
  if (options?.date) {
    metaParts.push(`<div style="font-size:12px;color:#9ca3af;margin-bottom:20px">📅 ${options.date}</div>`)
  }

  return `<div>${metaParts.join('\n')}</div>${html}`
}

/** 构建可打印的 HTML（用于 PDF 导出）*/
function buildPrintableHtml(md: string, title: string): string {
  const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' })
  const bodyHtml = formatPdfBody(md, { title, date: today })

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <title>${title}</title>
  <style>
    @page { margin: 22mm 18mm; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;
      font-size: 14px;
      line-height: 1.75;
      color: #1f2937;
      background: #fff;
      max-width: 800px;
      margin: 0 auto;
    }

    /* ===== 文档标题 ===== */
    .doc-title {
      font-size: 22px;
      font-weight: 700;
      color: #991b1b;
      padding-bottom: 10px;
      border-bottom: 3px solid #fca5a5;
      margin-bottom: 6px;
    }
    .doc-date {
      font-size: 12px;
      color: #9ca3af;
      margin-bottom: 20px;
    }

    /* ===== 段落 ===== */
    p { margin: 0 0 10px; line-height: 1.75; }

    /* 标题 + 图标 */
    h1, h2, h3, h4 { margin: 22px 0 10px; font-weight: 700; }
    h1 { font-size: 21px; color: #991b1b; padding-bottom: 8px; border-bottom: 2px solid #fca5a5; }
    h2 { font-size: 18px; color: #c2410c; padding-bottom: 6px; border-bottom: 2px solid #fdba74; }
    h3 { font-size: 16px; color: #a16207; }
    h4 { font-size: 15px; color: #7c3aed; }
    .hi { margin-right: 6px; font-size: 1.1em; }

    /* 文字样式 */
    strong { font-weight: 700; color: #b91c1c; }
    em { color: #d97706; font-style: italic; }
    del { color: #9ca3af; text-decoration: line-through; }
    code {
      background: #fef2f2; color: #b91c1c; padding: 2px 8px;
      border-radius: 4px; font-size: 0.85em; border: 1px solid #fecaca;
      font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
    }
    kbd {
      background: linear-gradient(180deg, #f9fafb, #f3f4f6);
      border: 1px solid #d1d5db; border-radius: 5px;
      padding: 1px 6px; font-size: 12px; color: #374151;
      box-shadow: 0 1px 2px rgba(0,0,0,0.06);
    }
    mark { background: #fef3c7; color: #92400e; padding: 1px 4px; border-radius: 3px; }

    /* 娴狅絿鐖滈崸?*/
    .code-header {
      font-size: 11px;
      font-family: "SFMono-Regular", Consolas, monospace;
      color: #a1a1aa;
      background: #27272a;
      padding: 4px 14px;
      border-radius: 8px 8px 0 0;
      margin-top: 12px;
    }
    pre {
      background: #18181b; color: #e4e4e7; padding: 14px 18px;
      border-radius: 0 0 8px 8px; overflow-x: auto; margin: 0 0 12px;
      font-size: 13px; line-height: 1.6; border: 1px solid #27272a; border-top: none;
    }
    pre code { background: none; padding: 0; border: none; color: inherit; font-size: inherit; }
    pre:not(.code-header + pre) { border-radius: 8px; border-top: 1px solid #27272a; }

    /* 瀵洜鏁� */
    blockquote {
      margin: 12px 0; padding: 10px 18px;
      border-left: 4px solid #f59e0b;
      background: linear-gradient(135deg, #fffbeb, #fef3c7);
      color: #92400e; border-radius: 0 8px 8px 0;
      line-height: 1.6; box-shadow: 0 1px 3px rgba(245,158,11,0.1);
    }
    blockquote p { margin: 0; }
    blockquote blockquote { margin: 8px 0; border-left-color: #f97316; background: #fff7ed; color: #9a3412; }

    /* 列表 */
    ul, ol { margin: 6px 0; padding-left: 24px; }
    li { margin: 4px 0; line-height: 1.6; }
    ul li::marker { color: #f59e0b; }
    ol li::marker { color: #f59e0b; font-weight: 600; }
    .task-list-item { list-style: none; margin-left: -24px; }
    .task-list-item input[type="checkbox"] { margin-right: 6px; }

    /* 鐞涖劍鐗� */
    table {
      border-collapse: separate; border-spacing: 0;
      width: 100%; margin: 14px 0; font-size: 13px;
      border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden;
    }
    thead { background: linear-gradient(135deg, #f59e0b, #d97706); color: #fff; }
    th { font-weight: 600; padding: 10px 14px; text-align: left; letter-spacing: 0.02em; }
    td { padding: 9px 14px; border-top: 1px solid #f3f4f6; }
    tbody tr:nth-child(even) td { background: #fafaf9; }

    /* 图片 */
    img { max-width: 100%; border-radius: 8px; margin: 10px 0; border: 1px solid #f3f4f6; }
    a { color: #2563eb; text-decoration: none; border-bottom: 1px solid #bfdbfe; }

    /* 分隔线 */
    hr { border: none; height: 2px; background: linear-gradient(to right, transparent, #f59e0b, #d97706, #f59e0b, transparent); margin: 18px 0; opacity: 0.6; }
  </style>
</head>
<body>
  ${bodyHtml}
  <script>window.onload = function () { window.print(); setTimeout(function () { window.close(); }, 200); }<\/script>
</body>
</html>`
}

/** 工具栏命令处理函数 */
async function handleToolbarCommand(cmd: string, msg: ChatMessage & { collapsed?: boolean }) {
  switch (cmd) {
    case 'copy':
      try {
        await navigator.clipboard.writeText(msg.content)
        ElMessage.success(t('aiKnowledge.chat.toolbar.copySuccess'))
      } catch {
        ElMessage.error(t('aiKnowledge.status.error'))
      }
      break

    case 'import-note': {
      // 查找当前消息的索引位置
      const idx = messages.value.findIndex(m => m.id === msg.id)
      const userMsg = idx > 0 ? messages.value[idx - 1] : null
      const title = userMsg?.content
        ? userMsg.content.slice(0, 50).replace(/\n/g, ' ').trim() || 'AI 问答'
        : 'AI 问答'

      try {
        // 1. 查找或创建 /AI问答/ 目录
        const tree = await fetchNotebookTree()
        let aiFolder = findTreeNode(tree, n => n.nodeType === 'FOLDER' && n.name === 'AI问答')

        // 2. 如果没有则自动创建
        if (!aiFolder) {
          const created = await createNotebook({ name: 'AI问答', parentId: null })
          aiFolder = { nodeKey: 'folder-' + created.id, notebookId: created.id, name: 'AI问答', nodeType: 'FOLDER' } as NbTreeNode
        }

        // 3. 构建 Markdown 内容
        const date = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' })
        const mdContent = `> **${title}**  ·  📅 ${date}

---

${msg.content}`

        await createNoteRequest({
          notebookId: aiFolder.notebookId ?? null,
          title: title + '.md',
          content: mdContent,
          noteType: 'NOTE',
        })

        ElMessage.success(`笔记已创建到 /AI问答/${title}.md`)
      } catch (e: unknown) {
        ElMessage.error((e as Error)?.message || t('aiKnowledge.status.error'))
      }
      break
    }

    case 'export-pdf': {
      // 查找当前消息的索引位置
      const idx = messages.value.findIndex(m => m.id === msg.id)
      const userMsg = idx > 0 ? messages.value[idx - 1] : null
      const title = userMsg?.content
        ? userMsg.content.slice(0, 50).replace(/\n/g, ' ').trim() || 'AI 问答'
        : 'AI 问答'

      const printHtml = buildPrintableHtml(msg.content, title)
      const blob = new Blob([printHtml], { type: 'text/html' })
      const url = URL.createObjectURL(blob)
      window.open(url, '_blank')
      setTimeout(() => URL.revokeObjectURL(url), 60000)
      break
    }

    case 'collapse':
      msg.collapsed = true
      saveMessages()
      break

    case 'expand':
      msg.collapsed = false
      saveMessages()
      break
  }
}

/** 递归查找树节点 */
function findTreeNode(nodes: NbTreeNode[], predicate: (n: NbTreeNode) => boolean): NbTreeNode | null {
  for (const node of nodes) {
    if (predicate(node)) return node
    if (node.children) {
      const found = findTreeNode(node.children, predicate)
      if (found) return found
    }
  }
  return null
}

// ========== RAG 知识库 ==========
const ragStats = ref<RagStats | null>(null)
const ragDocuments = ref<RagDocument[]>([])
const docsLoading = ref(false)
const retryingId = ref<number | null>(null)
const rebuilding = ref(false)
const ragSearchQuery = ref('')
const ragSearchResults = ref<RagSource[]>([])

// ========== Embedding 配置（独立于 Chat 配置） ==========
const embedConfigExpanded = ref(false)
const embedConfigDraft = ref<AiModelConfig>({
  provider: 'openai',
  apiKey: '',
  apiBaseUrl: 'https://api.openai.com/v1',
  model: 'gpt-4o',
  embeddingModel: 'text-embedding-3-small',
  temperature: 0.7,
  maxTokens: 4096,
  defaultProvider: false,
  maxContextMessages: 10,
})
const embedConfigFormRef = ref<FormInstance | null>(null)
const savingEmbedConfig = ref(false)
const clearingEmbedConfig = ref(false)
const embeddingConfigured = ref(false)

function statusTagType(status: string): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'ready': return 'success'
    case 'processing': return 'warning'
    case 'failed': return 'danger'
    default: return 'info'
  }
}

function statusLabel(status: string): string {
  switch (status) {
    case 'pending': return t('aiKnowledge.rag.statusPending')
    case 'processing': return t('aiKnowledge.rag.statusProcessing')
    case 'ready': return t('aiKnowledge.rag.statusReady')
    case 'failed': return t('aiKnowledge.rag.statusFailed')
    default: return status
  }
}

async function loadRagData() {
  docsLoading.value = true
  try {
    const [stats, docs] = await Promise.all([
      fetchRagStats(),
      fetchRagDocuments(),
    ])
    ragStats.value = stats
    ragDocuments.value = docs
  } catch {
    ElMessage.error(t('aiKnowledge.status.error'))
  } finally {
    docsLoading.value = false
  }
}

// ========== Embedding 配置 ==========

async function loadEmbeddingConfig() {
  try {
    const config = await fetchEmbeddingConfig()
    embedConfigDraft.value = {
      provider: config?.provider || 'openai',
      apiKey: config?.apiKey || '',
      apiBaseUrl: config?.apiBaseUrl || AI_PROVIDER_MAP[config?.provider || 'openai']?.apiBaseUrl || 'https://api.openai.com/v1',
      model: config?.model || 'gpt-4o',
      embeddingModel: config?.embeddingModel || AI_PROVIDER_MAP[config?.provider || 'openai']?.embeddingModel || 'text-embedding-3-small',
      temperature: config?.temperature ?? 0.7,
      maxTokens: config?.maxTokens ?? 4096,
      defaultProvider: config?.defaultProvider ?? false,
      maxContextMessages: config?.maxContextMessages ?? 10,
    }
    embeddingConfigured.value = !!config?.apiKey && !config.apiKey.includes('****')
  } catch {
    // 加载失败使用默认值
    embeddingConfigured.value = false
  }
}

async function saveEmbedConfig() {
  savingEmbedConfig.value = true
  try {
    await saveEmbeddingConfig(embedConfigDraft.value)
    ElMessage.success('Embedding 配置已保存')
    embeddingConfigured.value = true
    // 刷新 embedding 配置状态
    await loadEmbeddingConfig()
  } catch {
    ElMessage.error('保存 Embedding 配置失败')
  } finally {
    savingEmbedConfig.value = false
  }
}

async function clearEmbedConfig() {
  clearingEmbedConfig.value = true
  try {
    await saveEmbeddingConfig({
      provider: embedConfigDraft.value.provider,
      apiKey: '',
      apiBaseUrl: '',
      model: '',
      embeddingModel: '',
      temperature: 0.7,
      maxTokens: 4096,
      defaultProvider: false,
      maxContextMessages: 10,
    })
    ElMessage.success('Embedding 配置已清除，将使用 Chat 配置')
    embeddingConfigured.value = false
    await loadEmbeddingConfig()
  } catch {
    ElMessage.error('清除 Embedding 配置失败')
  } finally {
    clearingEmbedConfig.value = false
  }
}

async function retryDoc(id: number) {
  retryingId.value = id
  try {
    await retryRagDocument(id)
    ElMessage.success(t('aiKnowledge.status.loading'))
    await loadRagData()
  } catch {
    // error handled globally
  } finally {
    retryingId.value = null
  }
}

async function removeDoc(id: number) {
  try {
    await removeRagDocument(id)
    ElMessage.success(t('common.save'))
    await loadRagData()
  } catch {
    // error handled globally
  }
}

async function rebuildIndex() {
  try {
    await ElMessageBox.confirm(
      t('aiKnowledge.rag.rebuildConfirm'),
      t('common.confirmTitle'),
      { type: 'warning' },
    )
  } catch {
    return
  }
  rebuilding.value = true
  try {
    await rebuildRagIndex()
    ElMessage.success(t('aiKnowledge.status.loading'))
    await loadRagData()
  } catch {
    // error handled globally
  } finally {
    rebuilding.value = false
  }
}

async function doRagSearch() {
  const query = ragSearchQuery.value.trim()
  if (!query) return
  try {
    const result = await searchRag({ query, topK: 5 })
    ragSearchResults.value = result.sources
  } catch {
    // error handled globally
  }
}

const uploading = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)

function triggerUpload() {
  fileInputRef.value?.click()
}

async function handleFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  uploading.value = true
  try {
    const result = await uploadRagDocument(file)
    ElMessage.success(`${t('common.save')}：${result.fileName}`)
    await loadRagData()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : t('aiKnowledge.status.error'))
  } finally {
    uploading.value = false
    // 重置 input 以允许重复上传同名文件
    input.value = ''
  }
}

// ========== 设置 ==========
const configFormRef = ref<FormInstance | null>(null)
const configLoading = ref(false)
const savingConfig = ref(false)
const testingConnection = ref(false)

function buildDefaultConfig(provider: AiProvider = 'openai'): AiModelConfig {
  const info = AI_PROVIDER_MAP[provider]
  return {
    provider,
    apiKey: '',
    apiBaseUrl: info.apiBaseUrl,
    model: info.model,
    temperature: info.temperature,
    maxTokens: info.maxTokens,
    embeddingModel: info.embeddingModel,
    defaultProvider: false,
    maxContextMessages: 10,
  }
}

const defaultConfig = buildDefaultConfig()

/** 当前对话使用的 AI provider */
const chatProvider = ref<AiProvider>(defaultConfig.provider)

function onChatModelChange(provider: AiProvider) {
  onProviderChange(provider)
}

/** 加号按钮点击处理 */
function onPlusClick() {
  ElMessage.info(t('aiKnowledge.chat.attachComingSoon'))
}

/** 头像加载失败时的 fallback 处理 */
function onAvatarError(e: Event) {
  const img = e.target as HTMLImageElement
  if (img && !img.src.endsWith('custom.svg')) {
    img.src = '/icons/providers/custom.svg'
  }
}

const configDraft = ref<AiModelConfig>({ ...defaultConfig })

/** 上一次的提供商（用于切换时缓存 API Key） */
const lastProvider = ref(configDraft.value.provider)

/** 各提供商的草稿缓存（切换时保留未保存的修改） */
const configDraftCache = ref<Record<string, AiModelConfig>>({})

// 在 Settings 中切换提供商时
watch(() => configDraft.value.provider, (val) => {
  chatProvider.value = val
})

// 调试面板打开时刷新用量统计
watch(showDebugPanel, (val) => {
  if (val) {
    loadChatUsage()
    recalcContextTokens()
  }
})

/** API Key 缓存（切换 provider 时保留） */
const apiKeyCache = ref<Record<string, string>>({})

/** 当前 provider 信息 */
const providerInfo = computed(() => AI_PROVIDER_MAP[configDraft.value.provider])

/** 模型选项列表（保留用户当前选择的 model） */
const modelOptions = computed(() => {
  const currentProvider = configDraft.value.provider
  const currentModel = configDraft.value.model

  // 构建可用的 provider 选项列表，保留当前配置的 model
  if (providerList.value.length > 0) {
    return providerList.value.map(p => {
      // 若 provider 匹配 configDraft 中的当前配置则保留该 model，否则使用 Settings 默认值
      const model = p.provider === currentProvider && currentModel
        ? currentModel
        : p.model
      return {
        key: p.provider,
        displayModel: model || AI_PROVIDER_MAP[p.provider]?.model || p.provider,
        configured: p.configured,
        defaultProvider: p.defaultProvider,
      }
    })
  }

  // fallback: AI_PROVIDER_MAP 中未在 providerList 中出现的条目
  return Object.entries(AI_PROVIDER_MAP).map(([key, info]) => ({
    key,
    displayModel: key === currentProvider && currentModel ? currentModel : info.model || info.label,
    defaultProvider: false,
  }))
})

/** 获取当前 AI provider 的图标 URL */
const providerAvatarUrl = computed(() => {
  return `/icons/providers/${chatProvider.value}.svg`
})

const configRules: FormRules = {
  apiKey: [{ required: true, message: t('aiKnowledge.settings.apiKeyPlaceholder'), trigger: 'blur' }],
  model: [{ required: true, message: t('aiKnowledge.settings.modelPlaceholder'), trigger: 'blur' }],
}

async function loadConfig() {
  if (configLoading.value) return
  configLoading.value = true
  try {
    const data = await fetchAiModelConfig()
    configDraft.value = { ...buildDefaultConfig(data.provider), ...data }
    chatProvider.value = data.provider
    lastProvider.value = data.provider
  } catch {
    // 加载配置失败，使用默认配置
    configDraft.value = { ...defaultConfig }
    chatProvider.value = defaultConfig.provider
    lastProvider.value = defaultConfig.provider
  } finally {
    configLoading.value = false
  }
}

async function saveConfig() {
  const valid = await configFormRef.value?.validate().catch(() => false)
  if (!valid) return

  savingConfig.value = true
  try {
    await saveAiModelConfig(configDraft.value)
    ElMessage.success(t('aiKnowledge.settings.saveSuccess'))
    // 更新缓存中的已保存值，下次切换不会丢失
    configDraftCache.value[configDraft.value.provider] = { ...configDraft.value }
    // 保存后刷新 provider 列表以更新默认配置
    loadProviders()
  } catch {
    ElMessage.error(t('aiKnowledge.settings.saveFailed'))
  } finally {
    savingConfig.value = false
  }
}

async function testConnection() {
  testingConnection.value = true
  try {
    // 测试连接：发送一条简单消息
    await sendChatMessage({ question: 'Hi', provider: configDraft.value.provider, useRag: false })
    ElMessage.success(t('aiKnowledge.settings.testSuccess'))
  } catch {
    ElMessage.error(t('aiKnowledge.settings.testFailed'))
  } finally {
    testingConnection.value = false
  }
}

/**
 * 切换 AI provider 时的处理函数
 * - 更新当前选择的 model 为该 provider 的默认 model
 * - API Key 若不同则清空并提示用户重新填写
 */
function onProviderChange(provider: AiProvider) {
  const info = AI_PROVIDER_MAP[provider]
  if (!info) return

  // 缓存当前提供商的草稿（切换后保留未保存的修改）
  const old = lastProvider.value
  if (old && old !== provider) {
    configDraftCache.value[old] = { ...configDraft.value }
  }

  // 检查是否有缓存的草稿
  const cached = configDraftCache.value[provider]
  if (cached) {
    configDraft.value = { ...cached }
  } else {
    // 从 providerList 恢复已保存的配置
    const saved = providerList.value.find(p => p.provider === provider)
    configDraft.value.apiBaseUrl = info.apiBaseUrl
    configDraft.value.model = saved?.model || info.model
    configDraft.value.embeddingModel = info.embeddingModel
    configDraft.value.temperature = info.temperature
    configDraft.value.maxTokens = info.maxTokens
    configDraft.value.apiKey = apiKeyCache.value[provider] ?? ''
    configDraft.value.defaultProvider = saved?.defaultProvider ?? false
    configDraft.value.maxContextMessages = saved?.maxContextMessages ?? 10
  }

  lastProvider.value = provider
}

// 初始化时加载 provider 列表
loadProviders()
</script>

<style scoped lang="scss">
/* ==================== 页面布局 ==================== */
.ai-knowledge-page {
  display: flex;
  flex-direction: column;
  padding: 20px 24px 0;
  height: 100%;
}

.ai-knowledge__header {
  margin-bottom: 8px;
}

.ai-knowledge__title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
}

.ai-knowledge__tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;

  :deep(.el-tabs__content) {
    flex: 1;
    min-height: 0;
  }

  :deep(.el-tab-pane) {
    height: 100%;
  }
}

.ai-knowledge__tab-content {
  height: 100%;
  padding-bottom: 20px;
}

/* ===== 对话列表侧边栏 ===== */
.ak-layout {
  display: flex;
  height: 100%;
  gap: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  overflow: hidden;
  background: var(--el-bg-color);
}

.ak-sidebar {
  width: 260px;
  min-width: 260px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--el-border-color-lighter);
  background: #fafafa;
  overflow: hidden;
}

.ak-sidebar__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 12px 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
}

.ak-sidebar__header-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.ak-sidebar__add-cat-btn {
  color: #6b7280;
  &:hover { color: #374151; background: #e5e7eb; }
}

.ak-sidebar__list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 8px;
}

.ak-sidebar__empty {
  padding: 32px 16px;
  text-align: center;
  font-size: 13px;
  color: #9ca3af;
}

.ak-sidebar__search {
  padding: 8px 10px 6px;
  flex-shrink: 0;
  :deep(.el-input__wrapper) {
    border-radius: 8px;
    padding-left: 10px;
    height: 36px;
  }
  :deep(.el-input__prefix) {
    display: flex;
    align-items: center;
    color: #9ca3af;
  }
}

.ak-sidebar__search-status {
  padding: 24px 16px;
  text-align: center;
  font-size: 13px;
  color: #9ca3af;
}

.ak-sidebar__search-results {
  padding: 4px 0;
}

.ak-sidebar__search-item {
  padding: 10px 14px;
  cursor: pointer;
  border-radius: 6px;
  margin: 2px 8px;
  transition: background 0.12s;
  &:hover { background: #f3f4f6; }
  &.is-active { background: #e0f2fe; }
}

.ak-sidebar__search-cat {
  font-size: 11px;
  color: #9ca3af;
  margin-bottom: 2px;
}

.ak-sidebar__search-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ak-sidebar__search-snippet {
  font-size: 12px;
  color: #6b7280;
  margin-top: 3px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ak-sidebar__category {
  margin-bottom: 2px;
}

.ak-sidebar__cat-header {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 5px 6px;
  border-radius: 8px;
  cursor: pointer;
  user-select: none;
  transition: background 0.12s;
  &:hover { background: #f3f4f6; }
  &.is-empty { opacity: 0.6; }
}

.ak-sidebar__cat-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  color: #9ca3af;
  svg {
    transition: transform 0.2s;
    transform: rotate(-90deg);
    &.is-expanded { transform: rotate(0deg); }
  }
  &:hover { color: #6b7280; }
}

.ak-sidebar__cat-name {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  padding: 1px 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ak-sidebar__cat-count {
  font-size: 11px;
  color: #9ca3af;
  margin-right: 2px;
  flex-shrink: 0;
}

.ak-sidebar__cat-actions {
  display: none;
  align-items: center;
  gap: 1px;
  flex-shrink: 0;
  .ak-sidebar__cat-header:hover & { display: flex; }
}

.ak-sidebar__action-btn {
  width: 22px;
  height: 22px;
  color: #9ca3af;
  &:hover { color: #374151; background: #e5e7eb; }
}

.ak-sidebar__rename-input {
  flex: 1;
  :deep(.el-input__wrapper) {
    padding: 0 6px;
    box-shadow: 0 0 0 1px #3b82f6 !important;
    border-radius: 4px;
  }
  :deep(.el-input__inner) {
    font-size: 13px;
    height: 28px;
  }
}

.ak-sidebar__conv-list {
  padding-left: 21px;
  margin: 1px 0;
}

.ak-sidebar__conv-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 8px;
  margin: 1px 0;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.12s;
  &:hover { background: #f3f4f6; }
  &.is-active {
    background: #e0f2fe;
    .ak-sidebar__conv-title { color: #0369a1; font-weight: 600; }
    .ak-sidebar__conv-icon { color: #0369a1; }
  }
}

.ak-sidebar__conv-icon {
  flex-shrink: 0;
  color: #9ca3af;
}

.ak-sidebar__conv-title {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #4b5563;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ak-sidebar__conv-actions {
  flex-shrink: 0;
  display: none;
  .ak-sidebar__conv-item:hover & { display: flex; }
}

/* 侧边栏下拉菜单 */
.ak-sidebar__dropdown-popper {
  min-width: 120px;
}

/* ==================== 对话区域 ==================== */
.ak-chat {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: hidden;
  position: relative;
}

.ak-chat__body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.ak-chat__anchors {
  position: relative;
  width: 20px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  justify-content: center;
  margin-right: 4px;
  align-items: center;
  pointer-events: none;
}

.ak-chat__anchor-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  pointer-events: auto;
}

.ak-chat__anchor-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d1d5db;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.ak-chat__anchor-item:hover .ak-chat__anchor-dot {
  width: 16px;
  height: 16px;
  background: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.25);
}

.ak-chat__anchor-preview {
  display: none;
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  background: #1f2937;
  color: #f9fafb;
  font-size: 12px;
  line-height: 1.4;
  padding: 6px 10px;
  border-radius: 6px;
  white-space: nowrap;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  pointer-events: none;
  z-index: 10;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.ak-chat__anchor-preview::after {
  content: '';
  position: absolute;
  right: -5px;
  top: 50%;
  transform: translateY(-50%);
  border: 5px solid transparent;
  border-left-color: #1f2937;
  border-right: none;
}

.ak-chat__anchor-item:hover .ak-chat__anchor-preview {
  display: block;
}

.ak-chat__messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  min-height: 0;
}

.ak-chat__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #9ca3af;
  text-align: center;
  padding: 40px 20px;
}

.ak-chat__empty-icon {
  margin-bottom: 16px;
  opacity: 0.4;
}

.ak-chat__empty-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: #4b5563;
}

.ak-chat__empty-hint {
  margin: 0;
  font-size: 14px;
  color: #9ca3af;
  max-width: 320px;
  line-height: 1.5;
}

.ak-chat__msg-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  max-width: 85%;

  &.is-user {
    flex-direction: row-reverse;
    margin-left: auto;
  }
}

.ak-chat__avatar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  margin-top: 2px;
  overflow: hidden;

  &--ai {
    background: #fff;
    border: 1px solid #e5e7eb;
  }

  &--user {
    background: #e5e7eb;
    color: #4b5563;
  }
}

.ak-chat__avatar-img {
  width: 22px;
  height: 22px;
  object-fit: contain;
  display: block;
}

.ak-chat__bubble {
  padding: 12px 16px;
  border-radius: 12px;
  background: #f3f4f6;
  line-height: 1.55;
  font-size: 14px;
  color: #1f2937;
  word-break: break-word;

  .is-user & {
    background: #dcfce7;
    color: #166534;
  }
}

.ak-chat__content {
  /* marked 渲染的 Markdown 内容样式 */
}

/* ==================== Markdown 聊天消息样式 ==================== */
.ak-chat__content :deep(p) {
  margin: 0 0 8px;
  &:last-child { margin-bottom: 0; }
  line-height: 1.7;
}

.ak-chat__content :deep(strong) {
  font-weight: 700;
  color: #b91c1c;
}

.ak-chat__content :deep(em) {
  color: #d97706;
  font-style: italic;
}

.ak-chat__content :deep(code) {
  background: #fef2f2;
  color: #b91c1c;
  padding: 2px 7px;
  border-radius: 4px;
  font-size: 0.85em;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  border: 1px solid #fecaca;
}

.ak-chat__content :deep(pre) {
  background: #18181b;
  color: #e4e4e7;
  padding: 14px 18px;
  border-radius: 10px;
  overflow-x: auto;
  margin: 12px 0;
  font-size: 13px;
  line-height: 1.6;
  border: 1px solid #27272a;

  code {
    background: none;
    padding: 0;
    border: none;
    border-radius: 0;
    color: inherit;
    font-size: inherit;
  }
}

.ak-chat__content :deep(ul),
.ak-chat__content :deep(ol) {
  margin: 6px 0;
  padding-left: 22px;

  li {
    margin: 4px 0;
    line-height: 1.6;
  }
}

.ak-chat__content :deep(ul) {
  list-style-type: disc;

  ul { list-style-type: circle; }
}

.ak-chat__content :deep(ol) {
  list-style-type: decimal;

  ol { list-style-type: lower-alpha; }
}

.ak-chat__content :deep(h1) {
  margin: 24px 0 12px;
  font-size: 22px;
  font-weight: 700;
  color: #991b1b;
  padding-bottom: 8px;
  border-bottom: 3px solid #fca5a5;
}

.ak-chat__content :deep(h2) {
  margin: 20px 0 10px;
  font-size: 18px;
  font-weight: 700;
  color: #c2410c;
  padding-bottom: 6px;
  border-bottom: 2px solid #fdba74;
}

.ak-chat__content :deep(h3) {
  margin: 16px 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #a16207;
}

.ak-chat__content :deep(h4) {
  margin: 12px 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: #7c3aed;
}

/* ===== Emoji 标题样式 ===== */
.ak-chat__content :deep(.ak-emoji-heading) {
  margin: 18px 0 8px;
  font-size: 16px;
  font-weight: 700;
  color: #374151;
  line-height: 1.5;

  &:first-child { margin-top: 0; }
}

/* ===== 任务列表 checkbox ===== */
.ak-chat__content :deep(.task-list-item) {
  list-style: none;
  margin-left: -22px;

  input[type='checkbox'] {
    appearance: none;
    -webkit-appearance: none;
    width: 15px;
    height: 15px;
    border: 2px solid #d1d5db;
    border-radius: 3px;
    margin-right: 7px;
    vertical-align: middle;
    cursor: default;
    position: relative;
    top: -1px;
    transition: all 0.15s;
  }

  input[type='checkbox']:checked {
    background: #16a34a;
    border-color: #16a34a;

    &::after {
      content: '';
      position: absolute;
      left: 3px;
      top: 0;
      width: 5px;
      height: 9px;
      border: solid #fff;
      border-width: 0 2px 2px 0;
      transform: rotate(45deg);
    }
  }
}

/* ===== 键盘按键 ===== */
.ak-chat__content :deep(kbd) {
  background: linear-gradient(180deg, #f9fafb 0%, #f3f4f6 100%);
  border: 1px solid #d1d5db;
  border-radius: 5px;
  padding: 1px 6px;
  font-size: 12px;
  font-family: inherit;
  color: #374151;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
}

/* ===== 高亮标记 ===== */
.ak-chat__content :deep(mark) {
  background: #fef3c7;
  color: #92400e;
  padding: 1px 4px;
  border-radius: 3px;
}

.ak-chat__content :deep(blockquote) {
  margin: 12px 0;
  padding: 10px 16px;
  border-left: 4px solid #f59e0b;
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  color: #92400e;
  border-radius: 0 8px 8px 0;
  line-height: 1.6;
  box-shadow: 0 1px 3px rgba(245, 158, 11, 0.1);

  p { margin: 0; }

  /* 瀹撳苯顨滃鏇犳暏 */
  blockquote {
    margin: 8px 0;
    border-left-color: #f97316;
    background: #fff7ed;
    color: #9a3412;
  }
}

.ak-chat__content :deep(a) {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
  border-bottom: 1px solid #bfdbfe;
  transition: color 0.15s, border-bottom-color 0.15s;

  &:hover {
    color: #1d4ed8;
    border-bottom-color: #2563eb;
  }

  /* 给外部链接添加小图标指示 */
  &[href^="http"]::after {
    content: '↗';
    display: inline-block;
    font-size: 0.75em;
    margin-left: 2px;
    color: #93c5fd;
    transition: transform 0.15s;
  }

  &:hover[href^="http"]::after {
    transform: translate(1px, -1px);
  }
}

.ak-chat__content :deep(table) {
  border-collapse: separate;
  border-spacing: 0;
  width: 100%;
  margin: 14px 0;
  font-size: 13px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;

  thead {
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    color: #fff;
  }

  th {
    font-weight: 600;
    padding: 10px 14px;
    text-align: left;
    border-bottom: 1px solid #d97706;
    letter-spacing: 0.02em;
  }

  td {
    padding: 9px 14px;
    border-bottom: 1px solid #f3f4f6;
  }

  tbody tr {
    transition: background 0.15s;
  }

  tbody tr:last-child td { border-bottom: none; }

  tbody tr:nth-child(even) td {
    background: #fafaf9;
  }

  tbody tr:hover td {
    background: #fffbeb;
  }
}

.ak-chat__content :deep(hr) {
  border: none;
  height: 2px;
  background: linear-gradient(to right, transparent, #f59e0b, #d97706, #f59e0b, transparent);
  margin: 18px 0;
  border-radius: 2px;
  opacity: 0.6;
}

.ak-chat__content :deep(del) {
  color: #9ca3af;
  text-decoration: line-through;
  text-decoration-color: #d1d5db;
}

.ak-chat__content :deep(img) {
  max-width: 100%;
  border-radius: 10px;
  margin: 10px 0;
  border: 1px solid #f3f4f6;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  }
}

.ak-chat__input-container.is-sending {
  border-color: #22c55e;
  animation: ak-breathing 1.5s ease-in-out infinite;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.15), 0 4px 24px rgba(34, 197, 94, 0.12);
}

@keyframes ak-breathing {
  0%, 100% {
    border-color: #22c55e;
    box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.15), 0 4px 24px rgba(34, 197, 94, 0.12);
  }
  50% {
    border-color: #86efac;
    box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.08), 0 4px 28px rgba(34, 197, 94, 0.2);
  }
}

.ak-chat__sources {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.ak-chat__sources-label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.ak-chat__source-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #e0f2fe;
  color: #0369a1;
}

/* ==================== 工具栏 ==================== */
.ak-chat__toolbar {
  opacity: 0;
  transition: opacity 0.15s;
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid transparent;
}

.ak-chat__bubble:hover .ak-chat__toolbar {
  opacity: 1;
  border-top-color: #f3f4f6;
}

.ak-chat__toolbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.15s;
  padding: 0;

  &:hover {
    background: #f9fafb;
    color: #6b7280;
    border-color: #d1d5db;
  }
}

/* ==================== 输入区域 ==================== */
.ak-chat__input-area {
  padding: 0 16px 16px;
  background: transparent;
}

.ak-chat__input-container {
  max-width: 760px;
  margin: 0 auto;
  border: 1px solid #e0e0e0;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: box-shadow 0.2s, border-color 0.2s;

  &:focus-within {
    border-color: #c4c4c4;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
  }
}

.ak-chat__input-bar {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  padding: 8px 8px 4px 12px;
}

.ak-chat__btn-plus {
  flex-shrink: 0;
  margin-bottom: 4px;
  color: #6b7280;

  &:hover {
    color: #374151;
    background: #f3f4f6;
  }
}

.ak-chat__input-field {
  flex: 1;

  :deep(.el-textarea__inner) {
    border: none !important;
    box-shadow: none !important;
    padding: 4px 0;
    min-height: 24px;
    font-size: 14px;
    line-height: 1.5;
    resize: none;

    &:focus {
      box-shadow: none !important;
    }
  }
}

.ak-chat__input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 16px 8px;

  :deep(.el-checkbox__label) {
    font-size: 13px;
  }
}

.ak-chat__footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ak-chat__memory-warning {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #f59e0b;
  line-height: 1.3;
}

/* ===== Debug Panel ===== */
.ak-chat__debug-btn {
  position: absolute;
  right: 4px;
  bottom: 4px;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #9ca3af;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  z-index: 5;
  &:hover { color: #6366f1; border-color: #6366f1; background: #eef2ff; }
  &.is-active { color: #6366f1; border-color: #6366f1; background: #e0e7ff; }
}

.ak-debug-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  justify-content: flex-end;
  z-index: 2000;
}

.ak-debug-panel {
  width: 360px;
  max-width: 90vw;
  height: 100%;
  background: #1a1a2e;
  color: #e4e4e7;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: -4px 0 20px rgba(0,0,0,0.3);
}

.ak-debug-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  flex-shrink: 0;
}
.ak-debug-panel__title { font-size: 14px; font-weight: 600; }
.ak-debug-panel__close {
  background: none; border: none; color: #9ca3af; cursor: pointer;
  font-size: 16px; padding: 2px 6px; border-radius: 4px;
  &:hover { color: #fff; background: rgba(255,255,255,0.1); }
}

.ak-debug-panel__body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
}

.ak-debug-section {
  border-bottom: 1px solid rgba(255,255,255,0.06);
  &:last-child { border-bottom: none; }
}
.ak-debug-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  font-size: 13px;
  cursor: default;
  user-select: none;
}
.ak-debug-section__toggle { font-size: 10px; color: #9ca3af; }

.ak-debug-section__body {
  padding: 0 16px 12px;
}

.ak-debug-field {
  margin-bottom: 10px;
  label {
    display: block;
    font-size: 11px;
    color: #9ca3af;
    margin-bottom: 4px;
  }
  pre {
    margin: 0;
    font-size: 11px;
    line-height: 1.5;
    color: #d4d4d8;
    background: rgba(255,255,255,0.05);
    padding: 8px 10px;
    border-radius: 6px;
    max-height: 120px;
    overflow-y: auto;
    white-space: pre-wrap;
    word-break: break-all;
    font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
  }
}

.ak-debug-history-item {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
  pre { flex: 1; max-height: 80px; }
}
.ak-debug-role-tag {
  flex-shrink: 0;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  margin-top: 8px;
  height: fit-content;
  &.user { background: rgba(59,130,246,0.2); color: #93c5fd; }
  &.assistant { background: rgba(139,92,246,0.2); color: #c4b5fd; }
}

.ak-debug-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 12px;
}
.ak-debug-stat {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  padding: 4px 0;
  span { color: #a1a1aa; }
  strong { color: #e4e4e7; font-weight: 600; font-variant-numeric: tabular-nums; }
}

/* 剩余金额独占一行 */
.ak-balance-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 8px 0 12px;
  border-bottom: 1px solid #27272a;
  margin-bottom: 8px;
}
.ak-balance-row__label {
  font-size: 12px;
  color: #a1a1aa;
}
.ak-balance-row__value {
  font-size: 22px;
  font-weight: 800;
  color: #22d3ee;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 0 20px rgba(34, 211, 238, 0.3);
}

/* 调试面板底部操作按钮 */
.ak-debug-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 0 4px;
  border-top: 1px solid #27272a;
  margin-top: 8px;
}
.ak-debug-btn {
  display: block;
  width: 100%;
  padding: 8px 0;
  border: 1px solid transparent;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  text-align: center;
  transition: all 0.15s ease;
}
.ak-debug-btn--danger {
  color: #fca5a5;
  background: rgba(239,68,68,0.1);
  border-color: rgba(239,68,68,0.25);
}
.ak-debug-btn--danger:hover {
  background: rgba(239,68,68,0.2);
  border-color: rgba(239,68,68,0.4);
}
.ak-debug-btn--primary {
  color: #93c5fd;
  background: rgba(59,130,246,0.1);
  border-color: rgba(59,130,246,0.25);
}
.ak-debug-btn--primary:hover {
  background: rgba(59,130,246,0.2);
  border-color: rgba(59,130,246,0.4);
}
.ak-debug-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 上下文进度条 — 类似 Claude Code CLI 风格 */
.ak-context-bar-wrap {
  padding: 8px 0 4px;
}
.ak-context-bar {
  position: relative;
  height: 20px;
  background: #27272a;
  border-radius: 10px;
  overflow: hidden;
}
.ak-context-bar__fill {
  height: 100%;
  background: linear-gradient(90deg, #22c55e, #22d3ee);
  border-radius: 10px;
  transition: width 0.3s ease;
  min-width: 0;
}
.ak-context-bar__fill--warn {
  background: linear-gradient(90deg, #eab308, #f97316);
}
.ak-context-bar__fill--danger {
  background: linear-gradient(90deg, #ef4444, #dc2626);
}
.ak-context-bar__label {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
}
.ak-context-bar__detail {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 11px;
  color: #a1a1aa;
  font-variant-numeric: tabular-nums;
}
.ak-context-bar__separator {
  color: #52525b;
}
.ak-context-bar__used-label {
  color: #71717a;
  margin-left: 2px;
}

.debug-fade-enter-active,
.debug-fade-leave-active {
  transition: opacity 0.2s ease;
}
.debug-fade-enter-from,
.debug-fade-leave-to {
  opacity: 0;
}
.debug-fade-enter-active .ak-debug-panel,
.debug-fade-leave-active .ak-debug-panel {
  transition: transform 0.2s ease;
}
.debug-fade-enter-from .ak-debug-panel,
.debug-fade-leave-to .ak-debug-panel {
  transform: translateX(100%);
}

/* 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓 RAG 閻儴鐦戞惔?閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓 */
.ak-rag {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ak-rag__stats {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.ak-stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--wr-border, #e5e7eb);
  background: var(--wr-card, #fff);
  box-shadow: var(--wr-shadow, 0 1px 3px rgba(0,0,0,0.06));
  border-top: 3px solid #9ca3af;

  &.is-green { border-top-color: #22c55e; .ak-stat-card__icon { background: #22c55e; } }
  &.is-blue { border-top-color: #3b82f6; .ak-stat-card__icon { background: #3b82f6; } }
  &.is-orange { border-top-color: #f59e0b; .ak-stat-card__icon { background: #f59e0b; } }
  &.is-red { border-top-color: #ef4444; .ak-stat-card__icon { background: #ef4444; } }
  &.is-purple { border-top-color: #8b5cf6; .ak-stat-card__icon { background: #8b5cf6; } }
}

.ak-stat-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  flex-shrink: 0;
  color: #fff;
}

.ak-stat-card__body {
  min-width: 0;
  flex: 1;
}

.ak-stat-card__label {
  font-size: 12px;
  color: var(--wr-muted, #6b7280);
  margin-bottom: 4px;
}

.ak-stat-card__value {
  font-size: 22px;
  font-weight: 700;
  color: #111827;
  line-height: 1.2;
}

.ak-rag__search {
  padding: 16px 18px;
}

.ak-rag__search-row {
  display: flex;
  gap: 10px;
}

.ak-rag__search-results {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ak-rag__search-result {
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.ak-rag__search-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.ak-rag__search-result-file {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.ak-rag__search-result-content {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ak-rag__docs {
  padding: 16px 18px;
}

.ak-rag__docs-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.ak-rag__section-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
}

.ak-rag__time {
  font-size: 13px;
  color: #6b7280;
}

.ak-rag__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 20px;
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
  line-height: 1.5;
}

/* ==================== 设置 ==================== */
.ak-settings {
  padding: 20px 24px 28px;
}

.ak-settings__form {
  margin-top: 16px;
}

.ak-settings__slider-row {
  display: flex;
  align-items: center;
  width: 100%;
}

.ak-settings__slider-value {
  font-size: 14px;
  font-weight: 600;
  min-width: 32px;
  color: var(--el-text-color-primary);
}

.ak-settings__hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
  line-height: 1.4;
}

.ak-settings__actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

/* ==================== 响应式 ==================== */
@media (max-width: 1100px) {
  .ak-rag__stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .ak-rag__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

<!-- 非 scoped 样式：覆盖 el-select 内部样式 / 下拉菜单样式，必须放在全局且不能 scoped -->
<style lang="scss">
.ak-chat__model-select {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
  width: 150px;

  .el-select {
    flex: 1;
  }

  .el-select__wrapper {
    box-shadow: none !important;
    background: transparent !important;

    &:hover,
    &.is-hovered,
    &.is-active {
      box-shadow: none !important;
      background: rgba(0, 0, 0, 0.04) !important;
    }
  }

  .el-select__placeholder {
    font-size: 14px;
    font-weight: 500;
  }
}

.ak-chat__model-default-indicator {
  flex-shrink: 0;
  font-size: 11px;
  padding: 0 6px;
  height: 20px;
  line-height: 20px;
}

.ak-chat__model-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.ak-chat__model-default-tag {
  flex-shrink: 0;
  margin-left: 8px;
  font-size: 11px;
}

.ak-chat__model-popper {
  border: none !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12) !important;
}

/* 工具栏下拉菜单样式 / body 覆盖 */
.ak-chat__toolbar-popper {
  min-width: 140px;

  .el-dropdown-menu__item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    padding: 7px 14px;

    svg {
      flex-shrink: 0;
      color: #9ca3af;
    }

    &:hover svg {
      color: inherit;
    }
  }
}
</style>










