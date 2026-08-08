<template>
  <!-- AI 天窗对话 / RAG 知识库 / 知识库设置 -->
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
                  <img :src="messageAvatarUrl(msg)" class="ak-chat__avatar-img" alt="AI" @error="onAvatarError">
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
                  <p v-if="!expandedRagChunks[i]" class="ak-rag__search-result-content">{{ ragChunkSnippet(src.content) }}</p>
                  <div
                    v-else
                    class="ak-rag__search-result-md ak-chat__content markdown-body"
                    v-html="renderRagChunkMarkdown(src.content)"
                  />
                  <button
                    v-if="hasMarkdownSyntax(src.content)"
                    type="button"
                    class="ak-rag__search-result-toggle"
                    @click="toggleRagChunk(i)"
                  >
                    {{ expandedRagChunks[i] ? t('aiKnowledge.rag.collapseMarkdown') : t('aiKnowledge.rag.expandMarkdown') }}
                  </button>
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
                <el-table-column :label="t('aiKnowledge.rag.fileName')" min-width="200">
                  <template #default="{ row }">
                    <a class="ak-rag__doc-name" title="点击预览" @click="openDocPreview(row)">
                      {{ row.fileName }}
                    </a>
                  </template>
                </el-table-column>
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

          <!-- Embedding 模型配置（独立于 LLM 模型配置，另起一张卡片） -->
          <div class="ak-settings war-room-panel" style="max-width: 600px; margin-top: 16px;">
            <div style="display: flex; align-items: center; justify-content: space-between; cursor: pointer;" @click="embedConfigExpanded = !embedConfigExpanded">
              <h3 class="ak-rag__section-title" style="margin: 0;">
                {{ t('aiKnowledge.settings.embeddingConfig') }}
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
                    <el-select v-model="embedConfigDraft.provider" style="width: 100%" @change="onEmbeddingProviderChange">
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
                      :placeholder="`${embedProviderInfo?.label || ''} API Key（用于生成嵌入向量）`"
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
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 文档全屏预览：点击 RAG 文档文件名打开，样式参考笔记本全屏阅读器 -->
    <RagDocumentPreview
      v-model:visible="previewVisible"
      :title="previewDoc?.fileName ?? ''"
      :file-type="previewDoc?.fileType ?? ''"
      :content="previewContent"
      :loading="previewLoading"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * AI 天窗视图
 * 包含 AI 对话、RAG 知识库和设置三大功能
 */
import { ref, watch } from 'vue'
import { marked } from 'marked'
import { hasMarkdownSyntax, markdownToTextSnippet } from '@/utils/markdownToText'
import { useAiKnowledgePrint } from './composables/useAiKnowledgePrint'
import { useAiKnowledgeChat } from './composables/useAiKnowledgeChat'
import { useAiKnowledgeCategories } from './composables/useAiKnowledgeCategories'
import { useAiKnowledgeRag } from './composables/useAiKnowledgeRag'
import { useAiKnowledgeSettings } from './composables/useAiKnowledgeSettings'
import RagDocumentPreview from './RagDocumentPreview.vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  AI_PROVIDER_MAP,
  type AiProvider,
  type ChatMessage,
  type ProviderInfo,
  type RagSource,
} from '@/api/aiKnowledge'
import {
  fetchNotebookTree,
  createNotebook,
  createNoteRequest,
  type NbTreeNode,
} from '@/api/notebook'
import { buildImportNoteMarkdown, deriveToolbarTitle, findTreeNode } from './composables/aiKnowledgeToolbar'

const { t } = useI18n()

// ========== 标签页状态 ==========
const route = useRoute()
const router = useRouter()

const AI_KNOWLEDGE_TABS = ['chat', 'rag', 'settings'] as const
type AiKnowledgeTab = (typeof AI_KNOWLEDGE_TABS)[number]

/** 校验并读取 URL query 中的标签，非法值回落为默认「智能问答」 */
function tabFromQuery(): AiKnowledgeTab {
  const tab = route.query.tab
  return typeof tab === 'string' && (AI_KNOWLEDGE_TABS as readonly string[]).includes(tab)
    ? (tab as AiKnowledgeTab)
    : 'chat'
}

// 标签页状态：初值取自 URL query（?tab=rag），刷新后仍停留在当前标签而非回到「智能问答」
const activeTab = ref<AiKnowledgeTab>(tabFromQuery())

function onTabChange() {
  // 将当前标签同步到 URL query，保证刷新/分享/前进后退时标签不丢失
  if (route.query.tab !== activeTab.value) {
    router.replace({ query: { ...route.query, tab: activeTab.value } })
  }
  if (activeTab.value === 'chat') {
    loadProviders()
  } else if (activeTab.value === 'rag') {
    loadRagData()
  } else if (activeTab.value === 'settings') {
    loadConfig()
    loadEmbeddingConfig()
  }
}

// 浏览器前进/后退导致 query.tab 变化时同步标签页
watch(
  () => route.query.tab,
  (tab) => {
    if (typeof tab === 'string' && (AI_KNOWLEDGE_TABS as readonly string[]).includes(tab) && activeTab.value !== tab) {
      activeTab.value = tab as AiKnowledgeTab
      onTabChange()
    }
  },
)

// ========== 对话 ==========
const useRag = ref(false)

/** AI provider 列表 */
const providerList = ref<ProviderInfo[]>([])

const messages = ref<(ChatMessage & { sources?: RagSource[]; collapsed?: boolean })[]>([])

/** 当前激活会话 id（分类/聊天状态机共享，切换会话时由分类状态机更新） */
const activeConvId = ref<string | null>(null)

/** 聊天模型选择的 localStorage 持久化 key（刷新后保留上次使用的大模型） */
const CHAT_PROVIDER_STORAGE_KEY = 'ai-knowledge.chatProvider'

/** 从 localStorage 恢复上次使用的大模型，非法值回落默认 openai */
function readChatProviderFromStorage(): AiProvider {
  try {
    const raw = localStorage.getItem(CHAT_PROVIDER_STORAGE_KEY)
    if (raw && Object.keys(AI_PROVIDER_MAP).includes(raw)) {
      return raw as AiProvider
    }
  } catch {
    // localStorage 不可用（如隐私模式）时忽略，回落默认
  }
  return 'openai'
}

/**
 * 当前对话使用的 AI provider（settings 状态机维护，chat 状态机读取）
 * 初值从 localStorage 恢复上次选择的大模型，刷新后不重置回默认 gpt-4o；
 * 配置加载后由 loadConfig 同步更新
 */
const chatProvider = ref<AiProvider>(readChatProviderFromStorage())

// 变更时持久化，供下次进入/刷新恢复
watch(chatProvider, (val) => {
  try {
    localStorage.setItem(CHAT_PROVIDER_STORAGE_KEY, val)
  } catch {
    // 存储不可用时忽略，仅影响刷新后的模型记忆
  }
})

/** 回复消息对应的大模型图标：每条 assistant 消息记录其 provider，旧消息无 provider 时回落当前选中 */
function messageAvatarUrl(msg: ChatMessage): string {
  return `/icons/providers/${msg.provider ?? chatProvider.value}.svg`
}

// 打印能力（markdown → 可打印 HTML），逻辑已拆至 composables/useAiKnowledgePrint
const { buildPrintableHtml } = useAiKnowledgePrint()

// RAG 知识库与 Embedding 配置（逻辑已拆至 composables/useAiKnowledgeRag）
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
  embedConfigFormRef,
  embedProviderInfo,
  savingEmbedConfig,
  clearingEmbedConfig,
  embeddingConfigured,
  onEmbeddingProviderChange,
  uploading,
  fileInputRef,
  statusTagType,
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

// ========== RAG 检索结果片段展示 ==========
/** 纯文本片段字符预算（约 3 行），过长时按行截断 + 省略号 */
const RAG_SNIPPET_MAX_CHARS = 240
/** 各检索结果是否已展开 Markdown 渲染（按下标记录，避免切换结果时错位） */
const expandedRagChunks = ref<Record<number, boolean>>({})

/** 检索结果片段：Markdown → 可读纯文本 → 截断 */
function ragChunkSnippet(content: string): string {
  return markdownToTextSnippet(content, RAG_SNIPPET_MAX_CHARS)
}

/** 检索结果展开后：复用聊天消息的 marked 渲染 */
function renderRagChunkMarkdown(content: string): string {
  return marked.parse(content) as string
}

function toggleRagChunk(index: number) {
  expandedRagChunks.value[index] = !expandedRagChunks.value[index]
}

// 新一次检索结果替换时清空展开态，避免旧结果的展开状态串到新结果上
watch(ragSearchResults, () => {
  expandedRagChunks.value = {}
})

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
      // 查找当前消息的索引位置，取上一条用户消息内容作为笔记标题
      const idx = messages.value.findIndex(m => m.id === msg.id)
      const title = deriveToolbarTitle(messages.value, idx)

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
        const mdContent = buildImportNoteMarkdown(title, msg.content, date)

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
      // 查找当前消息的索引位置，取上一条用户消息内容作为导出标题
      const idx = messages.value.findIndex(m => m.id === msg.id)
      const title = deriveToolbarTitle(messages.value, idx)

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

// ========== 聊天状态机（消息发送/流式/token 统计/清空与压缩） ==========
// saveMessages 桥接：useAiKnowledgeChat 先于 useAiKnowledgeCategories 初始化，
// 通过闭包转发 saveMessages，避免两个状态机相互构造依赖
let saveMessagesBridge: () => Promise<void> = async () => {}

const {
  question,
  sending,
  chatMessagesRef,
  showDebugPanel,
  debugReqExpanded,
  chatUsage,
  currentContextTokens,
  compressing,
  userAnchorMessages,
  memoryLimit,
  maxContextWindow,
  contextPercentage,
  requestPayload,
  loadProviders,
  loadChatUsage,
  recalcContextTokens,
  scrollToMsg,
  renderMessage,
  sendMessage,
  clearConversation,
  compressConversation,
} = useAiKnowledgeChat({ messages, providerList, chatProvider, useRag, activeConvId, saveMessages: () => saveMessagesBridge() })

// ========== 设置状态机（Chat 配置加载/保存/连接测试/切换草稿缓存） ==========
// chat 状态机先构造以提供 loadProviders，settings 后构造读取共享 ref，无循环依赖
const {
  configFormRef,
  savingConfig,
  testingConnection,
  configDraft,
  providerInfo,
  modelOptions,
  configRules,
  onChatModelChange,
  onPlusClick,
  onAvatarError,
  loadConfig,
  saveConfig,
  testConnection,
  onProviderChange,
} = useAiKnowledgeSettings({ providerList, chatProvider, loadProviders })

// ========== 分类与会话状态机（分类/会话 CRUD、全局搜索、会话持久化与恢复） ==========
const {
  categories,
  activeCatId,
  categoryExpanded,
  renamingCatId,
  renamingConvId,
  catRenameInput,
  convRenameInput,
  searchQuery,
  searchResults,
  searching,
  handleSearchInput,
  clearSearch,
  switchToSearchResult,
  loadCategoriesFromServer,
  switchConversation,
  saveMessages,
  addCategory,
  confirmRenameCategory,
  cancelRenameCategory,
  handleCategoryCommand,
  handleConversationCommand,
  toggleCategoryExpand,
  addConversation,
  confirmRenameConversation,
  cancelRenameConversation,
} = useAiKnowledgeCategories({ messages, activeConvId, sending, currentContextTokens, recalcContextTokens, scrollToMsg })

saveMessagesBridge = saveMessages

// 初始化时加载 provider 列表
loadProviders()

// 调试面板打开时刷新用量统计（showDebugPanel 由聊天能力提供，需在其初始化后注册）
watch(showDebugPanel, (val) => {
  if (val) {
    loadChatUsage()
    recalcContextTokens()
  }
})

// 页面加载时从后端恢复数据（聊天能力初始化完成后触发）
loadCategoriesFromServer()
void loadChatUsage()

// 初始进入：若从 URL 恢复的标签不是 chat，加载对应标签数据（chat 数据已在 setup 顶部加载）
if (activeTab.value !== 'chat') {
  onTabChange()
}
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

/* RAG 文档名：可点击打开全屏预览；超长文件名省略号截断（替代原 show-overflow-tooltip） */
.ak-rag__doc-name {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #2563eb;
  text-decoration: none;
  cursor: pointer;
  font-weight: 500;
  border-bottom: 1px solid transparent;
  transition: color 0.15s, border-bottom-color 0.15s;

  &:hover {
    color: #1d4ed8;
    border-bottom-color: #2563eb;
  }
}

.ak-rag__search-result-content {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
  /* 保留纯文本片段中的换行（表格逐行、段落换行），同时用 line-clamp 兜底限高 */
  white-space: pre-wrap;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ak-rag__search-result-md {
  margin-top: 8px;
  padding: 10px 14px;
  border: 1px solid #f3f4f6;
  border-radius: 8px;
  background: #fafaf9;
  font-size: 13px;
  line-height: 1.7;
  color: #374151;
  /* 展开后内容可能较长，限制高度滚动，避免结果列表被撑爆 */
  max-height: 320px;
  overflow-y: auto;
}

.ak-rag__search-result-toggle {
  margin-top: 6px;
  padding: 0;
  border: none;
  background: none;
  font-size: 12px;
  color: #2563eb;
  cursor: pointer;
  user-select: none;

  &:hover {
    color: #1d4ed8;
    text-decoration: underline;
  }
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










