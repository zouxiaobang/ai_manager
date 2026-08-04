<!--
 * 笔记本页面组件
 * 提供笔记管理功能，包括笔记列表、富文本编辑、文件夹管理、标签管理
 * 支持百度网盘同步、正文格式优化等功能
 -->
<template>
  <!-- 页面主容器：笔记本页面整体布局 -->
  <WarRoomPage :title="t('notebook.title')" fill>
    <!-- 页面头部右侧：百度网盘绑定按钮（link 样式、不悬浮；已绑定时显示为绿色） -->
    <template #meta>
      <el-button
        type="link"
        :icon="Link"
        class="notebook-bind-pan-btn"
        :class="{ 'is-bound': baiduPanStatus?.authorized }"
        @click="openBaiduPanAuthorize"
      >
        {{ t('notebook.baiduPanConnect') }}
      </el-button>
    </template>
    <div class="notebook-page war-room-panel war-room-panel--notebook">
    <!-- 百度网盘授权提示：未授权时显示警告提示和连接按钮 -->
    <el-alert
      v-if="baiduPanStatus && !baiduPanStatus.authorized && !isBaiduAuthPending"
      class="notebook-page__baidu-alert"
      type="warning"
      :closable="false"
      show-icon
      :title="t('notebook.baiduPanRequiredTitle')"
    >
      <template #default>
        <span>{{ t('notebook.baiduPanRequiredDesc') }}</span>
        <el-button
          v-if="baiduPanStatus.authorizeUrl"
          type="primary"
          link
          @click="openBaiduPanAuthorize"
        >
          {{ t('notebook.baiduPanConnect') }}
        </el-button>
      </template>
    </el-alert>

    <!-- 标签页导航：全部笔记、回收站 -->
    <div class="notebook-tabs-wrap">
    <el-tabs v-model="activeTab" class="notebook-tabs" @tab-change="onTabChange">
      <!-- 全部笔记标签页 -->
      <el-tab-pane :label="t('notebook.tabs.all')" name="all">
        <div class="notebook-tab-all">
          <!-- 统计卡片区域：展示笔记、文件夹、标签、回收站数量统计 -->
          <section class="notebook-stats">
            <div class="notebook-stat-card notebook-stat-card--blue">
              <span class="notebook-stat-card__icon notebook-stat-card__icon--blue">
                <el-icon><Document /></el-icon>
              </span>
              <div class="notebook-stat-card__body">
                <span class="notebook-stat-card__label">{{ t('notebook.stats.notes') }}</span>
                <span class="notebook-stat-card__value">{{ notebookStats.noteCount }}</span>
              </div>
            </div>
            <div class="notebook-stat-card notebook-stat-card--green">
              <span class="notebook-stat-card__icon notebook-stat-card__icon--green">
                <el-icon><Folder /></el-icon>
              </span>
              <div class="notebook-stat-card__body">
                <span class="notebook-stat-card__label">{{ t('notebook.stats.folders') }}</span>
                <span class="notebook-stat-card__value">{{ notebookStats.folderCount }}</span>
              </div>
            </div>
            <div class="notebook-stat-card notebook-stat-card--purple">
              <span class="notebook-stat-card__icon notebook-stat-card__icon--purple">
                <el-icon><CollectionTag /></el-icon>
              </span>
              <div class="notebook-stat-card__body">
                <span class="notebook-stat-card__label">{{ t('notebook.stats.tags') }}</span>
                <span class="notebook-stat-card__value">{{ notebookStats.tagCount }}</span>
              </div>
            </div>
            <div class="notebook-stat-card notebook-stat-card--orange">
              <span class="notebook-stat-card__icon notebook-stat-card__icon--orange">
                <el-icon><Delete /></el-icon>
              </span>
              <div class="notebook-stat-card__body">
                <span class="notebook-stat-card__label">{{ t('notebook.stats.trash') }}</span>
                <span class="notebook-stat-card__value">{{ notebookStats.trashCount }}</span>
              </div>
            </div>
          </section>

          <!-- 主布局区域：左侧侧边栏 + 右侧主内容区 -->
          <div class="notebook-layout">
            <!-- 紧凑档侧栏开关按钮（抽屉收起时显示，唤出抽屉） -->
            <button
              v-if="isCompactRange && !sidebarVisible"
              type="button"
              class="notebook-sidebar-toggle"
              :aria-label="t('notebook.showSidebar')"
              :aria-expanded="false"
              aria-controls="notebook-sidebar"
              @click="sidebarVisible = true"
            >
              <el-icon :size="18"><FolderOpened /></el-icon>
            </button>
            <!-- 左侧侧边栏：搜索栏、新建按钮、笔记文件夹树 -->
          <aside
            id="notebook-sidebar"
            class="notebook-sidebar"
            :class="{ 'is-offcanvas': isCompactRange, 'is-collapsed': isCompactRange && !sidebarVisible }"
            :aria-expanded="sidebarAriaExpanded"
          >
            <!-- 紧凑档抽屉头部：标题 + 关闭按钮（仅抽屉模式显示） -->
            <div v-if="isCompactRange" class="notebook-sidebar__drawer-head">
              <span class="notebook-sidebar__drawer-title">{{ t('notebook.treeTitle') }}</span>
              <button
                type="button"
                class="notebook-sidebar__drawer-close"
                :aria-label="t('notebook.hideSidebar')"
                @click="sidebarVisible = false"
              >
                <el-icon><Close /></el-icon>
              </button>
            </div>
            <!-- 侧边栏工具栏：搜索框和新建按钮 -->
            <div class="notebook-sidebar__toolbar">
              <!-- 搜索框区域 -->
              <div class="notebook-sidebar__search-wrap">
                <el-input
                  ref="searchInputRef"
                  v-model="filterText"
                  clearable
                  :placeholder="t('notebook.searchPlaceholder')"
                  :prefix-icon="Search"
                  class="notebook-sidebar__search"
                >
                  <template #suffix>
                    <kbd v-if="!filterText" class="notebook-sidebar__search-kbd">{{ searchShortcutLabel }}</kbd>
                  </template>
                </el-input>
              </div>

              <!-- 新建按钮区域：新建笔记和新建文件夹 -->
              <div class="notebook-sidebar__create">
                <button
                  type="button"
                  class="notebook-sidebar__create-main"
                  @click="openCreateNote"
                >
                  <el-icon><Plus /></el-icon>
                  <span>{{ t('notebook.newNote') }}</span>
                </button>
                <span class="notebook-sidebar__create-divider" aria-hidden="true" />
                <el-dropdown
                  v-model:visible="createDropdownVisible"
                  trigger="click"
                  popper-class="notebook-sidebar__create-dropdown-popper"
                  @command="onSidebarCreateCommand"
                >
                  <button
                    type="button"
                    class="notebook-sidebar__create-more"
                    :title="t('notebook.newFolder')"
                  >
                    <el-icon><ArrowDown /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="folder">
                        {{ t('notebook.newFolder') }}
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>

            <!-- 侧边栏主体：笔记文件夹树 -->
            <div class="notebook-sidebar__body">
              <!-- 树形结构容器：笔记和文件夹的层级树 -->
              <div
                v-loading="treeLoading"
                class="notebook-sidebar__tree-wrap"
                @click.self="onTreeBlankClick"
              >
                <!-- 笔记文件夹树组件 -->
                <el-tree
                  v-if="treeData.length"
                  ref="treeRef"
                  :data="treeData"
                  node-key="nodeKey"
                  :props="treeProps"
                  :current-node-key="activeNodeKey"
                  :expand-on-click-node="false"
                  :filter-node-method="filterNode"
                  highlight-current
                  @node-click="onTreeNodeClick"
                  @node-expand="onTreeNodeExpand"
                  @node-collapse="onTreeNodeCollapse"
                >
                  <template #default="{ data }">
                    <span
                      class="notebook-tree-node"
                      :class="{
                        'is-note': data.nodeType === 'NOTE',
                        'is-folder': data.nodeType === 'FOLDER',
                        'is-active': data.nodeKey === activeNodeKey,
                      }"
                      @contextmenu="onTreeNodeContextMenu($event, data)"
                    >
                      <el-icon
                        v-if="data.nodeType === 'FOLDER'"
                        class="notebook-tree-node__icon is-folder"
                      >
                        <svg viewBox="0 0 16 16" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                          <path
                            fill="currentColor"
                            d="M1.75 2.5A1.25 1.25 0 0 1 3 1.25h3.086a1 1 0 0 1 .708.293L7.543 2.75H13a1.25 1.25 0 0 1 1.25 1.25v9A1.25 1.25 0 0 1 13 14.25H3A1.25 1.25 0 0 1 1.75 13V2.5Z"
                          />
                        </svg>
                      </el-icon>
                      <el-icon v-else class="notebook-tree-node__icon is-note">
                        <Document />
                      </el-icon>
                      <span class="notebook-tree-node__label">{{ data.name }}</span>
                      <el-icon v-if="data.isPinned === 1" class="notebook-tree-node__pin">
                        <Top />
                      </el-icon>
                    </span>
                  </template>
                </el-tree>
                <el-empty v-else :description="t('notebook.emptyTree')" :image-size="64" />
              </div>
            </div>
          </aside>

            <!-- 紧凑档侧栏遮罩（抽屉展开时点击收起） -->
            <div
              v-if="isCompactRange && sidebarVisible"
              class="notebook-sidebar-overlay"
              @click="sidebarVisible = false"
            />

          <!-- 右侧主内容区：笔记编辑器或文件夹视图 -->
          <main class="notebook-main" :class="{ 'is-editing': !!currentNote }">
            <!-- 笔记工作区：编辑器 + 目录 -->
            <div v-if="currentNote" class="notebook-workspace">
              <!-- 笔记编辑器区域 -->
              <div class="notebook-editor" :class="{ 'editor-title-shifted': editorTitleShifted }">
              <!-- 编辑器头部：标题、元信息、操作按钮 -->
              <div class="notebook-editor__header">
              <!-- 标题行：标题输入框 + 操作按钮列 -->
              <div class="notebook-editor__title-row">
                <!-- 标题列：标题输入和元信息 -->
                <div class="notebook-editor__title-col">
                  <el-input
                    v-model="editForm.title"
                    class="notebook-editor__title"
                    :placeholder="t('notebook.untitled')"
                    :disabled="contentLoading"
                    @input="onContentChanged"
                  />
                  <div class="notebook-editor__meta-line">
                    <span v-if="contentLoading" class="notebook-editor__meta-item">
                      {{ t('notebook.contentLoading') }}
                    </span>
                    <span v-else-if="saveState === 'saving'" class="notebook-editor__meta-item">
                      {{ t('notebook.saving') }}
                    </span>
                    <span v-else-if="saveState === 'localSavedSyncFailed'" class="notebook-editor__meta-item is-warn">
                      <el-icon class="notebook-editor__meta-check"><CircleCheck /></el-icon>
                      {{ t('notebook.localSavedSyncFailed') }}
                    </span>
                    <span v-else-if="contentLoadBlocked" class="notebook-editor__meta-item is-error">
                      {{ t('notebook.contentLoadSaveBlocked') }}
                    </span>
                    <span v-else-if="saveState === 'idle' && isDirty()" class="notebook-editor__meta-item is-warn">
                      {{ t('notebook.pleaseSave') }}
                    </span>
                    <span v-else-if="saveState === 'saved'" class="notebook-editor__meta-item is-ok">
                      <el-icon class="notebook-editor__meta-check"><CircleCheck /></el-icon>
                      {{ t('notebook.saved') }}
                    </span>
                    <span
                      v-else-if="!contentLoadBlocked && currentNote.syncStatus === 'CLOUD_PENDING'"
                      class="notebook-editor__meta-item is-warn"
                    >
                      {{ t('notebook.cloudPending') }}
                    </span>
                    <span
                      v-else-if="!contentLoadBlocked && currentNote.syncStatus === 'SYNCING'"
                      class="notebook-editor__meta-item"
                    >
                      {{ t('notebook.syncing') }}
                    </span>
                    <span
                      v-else-if="!contentLoadBlocked && currentNote.syncStatus === 'FAILED'"
                      class="notebook-editor__meta-item is-error"
                    >
                      {{ t('notebook.syncFailed') }}
                    </span>
                    <template v-if="formattedUpdateTime">
                      <span v-if="showMetaStatus" class="notebook-editor__meta-dot">·</span>
                      <span class="notebook-editor__meta-item">{{ formattedUpdateTime }}</span>
                    </template>
                    <span
                      v-if="showMetaStatus || formattedUpdateTime"
                      class="notebook-editor__meta-dot"
                    >·</span>
                    <span class="notebook-editor__meta-item">
                      {{ t('notebook.wordCount', { count: noteWordCount }) }}
                    </span>
                  </div>
                </div>
                <div v-if="!isTabletRange" class="notebook-editor__actions-col">
                  <div class="notebook-editor__pin-actions">
                    <button
                      type="button"
                      class="notebook-editor__meta-action"
                      :class="{ 'is-active': currentNote.isPinned === 1 }"
                      @click="togglePin"
                    >
                      <el-icon><Top /></el-icon>
                      {{ currentNote.isPinned === 1 ? t('notebook.unpin') : t('notebook.pin') }}
                    </button>
                    <button
                      type="button"
                      class="notebook-editor__meta-action"
                      :class="{ 'is-active': currentNote.isFavorite === 1 }"
                      @click="toggleFavorite"
                    >
                      <el-icon><Star /></el-icon>
                      {{ currentNote.isFavorite === 1 ? t('notebook.unfavorite') : t('notebook.favorite') }}
                    </button>
                    <div v-if="isMdNote" class="notebook-editor__md-mode-group">
                      <button
                        type="button"
                        class="notebook-editor__meta-action"
                        :class="{ 'is-active': mdViewMode === 'edit' }"
                        @click="mdViewMode = 'edit'"
                      >
                        <el-icon><Edit /></el-icon>
                        {{ t('notebook.mdEdit') }}
                      </button>
                      <button
                        type="button"
                        class="notebook-editor__meta-action"
                        :class="{ 'is-active': mdViewMode === 'split' }"
                        @click="mdViewMode = 'split'"
                      >
                        <span class="notebook-editor__md-split-icon">‖</span>
                        {{ t('notebook.mdSplit') }}
                      </button>
                      <button
                        type="button"
                        class="notebook-editor__meta-action"
                        :class="{ 'is-active': mdViewMode === 'preview' }"
                        @click="mdViewMode = 'preview'"
                      >
                        <el-icon><View /></el-icon>
                        {{ t('notebook.mdPreview') }}
                      </button>
                    </div>
                    <button
                      type="button"
                      class="notebook-editor__meta-action"
                      :title="t('notebook.fullscreenOpen')"
                      @click="openFullscreen"
                    >
                      <el-icon><FullScreen /></el-icon>
                    </button>
                  </div>
                </div>
                <!-- 平板档：置顶/收藏/MD模式/全屏/标签 收进一个配置按钮（和标题同行，释放内容区高度） -->
                <el-popover
                  v-else
                  v-model:visible="configMenuVisible"
                  placement="bottom-end"
                  :width="240"
                  trigger="click"
                  popper-class="notebook-editor-config-popper"
                >
                  <template #reference>
                    <button
                      type="button"
                      class="notebook-editor__config-btn"
                      :class="{ 'is-toc-collapsed': !tocVisible }"
                      :title="t('notebook.config')"
                      :aria-label="t('notebook.config')"
                    >
                      <el-icon :size="16"><MoreFilled /></el-icon>
                    </button>
                  </template>
                  <div class="notebook-editor-config">
                    <button
                      type="button"
                      class="notebook-editor-config__item"
                      :class="{ 'is-active': currentNote.isPinned === 1 }"
                      @click="configMenuVisible = false; togglePin()"
                    >
                      <el-icon><Top /></el-icon>
                      <span>{{ currentNote.isPinned === 1 ? t('notebook.unpin') : t('notebook.pin') }}</span>
                    </button>
                    <button
                      type="button"
                      class="notebook-editor-config__item"
                      :class="{ 'is-active': currentNote.isFavorite === 1 }"
                      @click="configMenuVisible = false; toggleFavorite()"
                    >
                      <el-icon><Star /></el-icon>
                      <span>{{ currentNote.isFavorite === 1 ? t('notebook.unfavorite') : t('notebook.favorite') }}</span>
                    </button>
                    <template v-if="isMdNote">
                      <div class="notebook-editor-config__sep" />
                      <div class="notebook-editor-config__md">
                        <button
                          type="button"
                          class="notebook-editor-config__md-item"
                          :class="{ 'is-active': mdViewMode === 'edit' }"
                          @click="configMenuVisible = false; mdViewMode = 'edit'"
                        >
                          <el-icon><Edit /></el-icon>
                          {{ t('notebook.mdEdit') }}
                        </button>
                        <button
                          type="button"
                          class="notebook-editor-config__md-item"
                          :class="{ 'is-active': mdViewMode === 'split' }"
                          @click="configMenuVisible = false; mdViewMode = 'split'"
                        >
                          <span class="notebook-editor__md-split-icon">‖</span>
                          {{ t('notebook.mdSplit') }}
                        </button>
                        <button
                          type="button"
                          class="notebook-editor-config__md-item"
                          :class="{ 'is-active': mdViewMode === 'preview' }"
                          @click="configMenuVisible = false; mdViewMode = 'preview'"
                        >
                          <el-icon><View /></el-icon>
                          {{ t('notebook.mdPreview') }}
                        </button>
                      </div>
                    </template>
                    <div class="notebook-editor-config__sep" />
                    <button
                      type="button"
                      class="notebook-editor-config__item"
                      @click="configMenuVisible = false; openFullscreen()"
                    >
                      <el-icon><FullScreen /></el-icon>
                      <span>{{ t('notebook.fullscreenOpen') }}</span>
                    </button>
                    <div class="notebook-editor-config__sep" />
                    <div class="notebook-editor-config__label">{{ t('notebook.tags') }}</div>
                    <button
                      v-for="(tag, tagIndex) in allTags"
                      :key="tag.id"
                      type="button"
                      class="notebook-editor-config__item"
                      :class="{ 'is-selected': editForm.tagIds.includes(tag.id) }"
                      @click="toggleNoteTag(tag.id)"
                    >
                      <span
                        class="notebook-tag-picker__pill"
                        :style="getTagPillStyle(tag, tagIndex)"
                      >
                        # {{ tag.name }}
                      </span>
                      <el-icon v-if="editForm.tagIds.includes(tag.id)" class="notebook-editor-config__check"><Check /></el-icon>
                    </button>
                    <el-empty
                      v-if="!allTags.length"
                      :description="t('notebook.noTags')"
                      :image-size="48"
                    />
                    <button
                      type="button"
                      class="notebook-editor-config__manage"
                      @click="configMenuVisible = false; openTagManage()"
                    >
                      {{ t('notebook.tagManage') }}
                    </button>
                  </div>
                </el-popover>
              </div>

              <!-- 标签区域：笔记标签管理和添加 -->
              <div v-if="!isTabletRange" class="notebook-editor__tags">
                <span
                  v-for="(tag, tagIndex) in selectedNoteTags"
                  :key="tag.id"
                  class="notebook-tag-pill"
                  :style="getTagPillStyle(tag, tagIndex)"
                  :title="t('notebook.removeTag')"
                  @click="detachNoteTag(tag.id)"
                >
                  # {{ tag.name }}
                </span>
                <el-popover
                  v-model:visible="tagPickerVisible"
                  placement="bottom-start"
                  :width="240"
                  trigger="click"
                  popper-class="notebook-tag-popover"
                >
                  <template #reference>
                    <button type="button" class="notebook-tag-pill is-add">
                      <el-icon><Plus /></el-icon>
                      {{ t('notebook.addTag') }}
                    </button>
                  </template>
                  <div class="notebook-tag-picker">
                    <button
                      v-for="(tag, tagIndex) in allTags"
                      :key="tag.id"
                      type="button"
                      class="notebook-tag-picker__item"
                      :class="{ 'is-selected': editForm.tagIds.includes(tag.id) }"
                      @click="toggleNoteTag(tag.id)"
                    >
                      <span
                        class="notebook-tag-picker__pill"
                        :style="getTagPillStyle(tag, tagIndex)"
                      >
                        # {{ tag.name }}
                      </span>
                      <el-icon v-if="editForm.tagIds.includes(tag.id)"><Check /></el-icon>
                    </button>
                    <el-empty
                      v-if="!allTags.length"
                      :description="t('notebook.noTags')"
                      :image-size="48"
                    />
                    <button
                      type="button"
                      class="notebook-tag-picker__manage"
                      @click="openTagManage"
                    >
                      {{ t('notebook.tagManage') }}
                    </button>
                  </div>
                </el-popover>
              </div>
              </div>

              <!-- 编辑器内容区域：MD 编辑/分屏/预览 + 富文本编辑器 -->
              <div class="notebook-editor__content">
                <!-- MD 分屏模式 -->
                <template v-if="isMdNote && mdViewMode === 'split'">
                  <div class="notebook-editor__md-split">
                    <textarea
                      ref="mdSplitEditorRef"
                      v-model="editForm.content"
                      class="notebook-editor__md-textarea is-split"
                      :placeholder="t('notebook.contentPlaceholder')"
                      @scroll="onMdSplitScroll('editor')"
                    />
                    <div
                      ref="mdSplitPreviewRef"
                      class="notebook-editor__md-preview is-split"
                      v-html="mdPreviewHtml"
                      @scroll="onMdSplitScroll('preview')"
                    />
                  </div>
                </template>
                <!-- MD 预览模式 -->
                <div
                  v-else-if="isMdNote && mdViewMode === 'preview'"
                  class="notebook-editor__md-preview"
                  v-html="mdPreviewHtml"
                />
                <!-- MD 编辑模式 -->
                <textarea
                  v-else-if="isMdNote && mdViewMode === 'edit'"
                  v-model="editForm.content"
                  class="notebook-editor__md-textarea"
                  :placeholder="t('notebook.contentPlaceholder')"
                />
                <!-- HTML 富文本编辑器 -->
                <NoteRichEditor
                  v-else-if="currentNote"
                  ref="editorRef"
                  :key="currentNote.id"
                  v-model="editForm.content"
                  class="notebook-editor__content-editor"
                  :class="{ 'is-content-loading': contentLoading }"
                  :placeholder="t('notebook.contentPlaceholder')"
                  @change="onContentChanged"
                  @heading-active="onHeadingActive"
                />
                <div
                  v-if="contentLoading"
                  class="notebook-editor__content-loading notebook-editor__content-loading--overlay"
                >
                  <el-icon class="notebook-editor__loading-icon is-loading"><Loading /></el-icon>
                  <span>{{ t('notebook.contentLoading') }}</span>
                </div>
                <button
                  v-if="currentNote && !contentLoading"
                  type="button"
                  class="notebook-editor__save-btn"
                  :title="`${t('notebook.save')} (Ctrl+S)`"
                  :disabled="saveState === 'saving'"
                  @click="manualSave"
                >
                  <el-icon v-if="saveState === 'saving'"><Loading /></el-icon>
                  <el-icon v-else><Check /></el-icon>
                </button>
              </div>
              </div>

              <!-- 目录侧边栏：笔记章节目录导航 -->
              <aside class="notebook-toc" :class="{ 'is-collapsed': !tocVisible }">
                <!-- 目录头部：标题和折叠按钮 -->
                <div class="notebook-toc__header">
                  <h3 class="notebook-toc__title">{{ t('notebook.tabs.toc') }}</h3>
                  <button
                    type="button"
                    class="notebook-toc__toggle"
                    :title="t('notebook.hideToc')"
                    @click="tocVisible = false"
                  >
                    <el-icon><Close /></el-icon>
                  </button>
                </div>
                <NoteTocPanel
                  :items="tocItems"
                  :active-index="activeTocIndex"
                  :empty-text="t('notebook.tocEmpty')"
                  @select="onTocItemClick"
                />
              </aside>

              <button
                v-if="!tocVisible"
                type="button"
                class="notebook-toc-expand"
                :title="t('notebook.showToc')"
                @click="tocVisible = true"
              >
                {{ t('notebook.tabs.toc') }}
              </button>
            </div>

            <NotebookFolderView
              v-if="!currentNote && selectedFolderNode"
              :folder-node="selectedFolderNode"
              :note-meta="folderNoteMeta"
              @open-folder="onFolderViewOpenFolder"
              @open-note="onFolderViewOpenNote"
            />

            <div v-show="!currentNote && !selectedFolderNode" class="notebook-main__empty">
              <el-empty :description="t('notebook.selectNoteHint')" />
            </div>
          </main>
        </div>
        </div>
      </el-tab-pane>

      <!-- 回收站标签页：已删除的笔记管理 -->
      <el-tab-pane :label="t('notebook.tabs.trash')" name="trash">
        <NotebookTrashView
          ref="trashViewRef"
          @count-change="trashCount = $event"
          @restored="loadTree()"
        />
      </el-tab-pane>
    </el-tabs>
    <!-- 矮视口标签页收起：小图标按钮切换 全部/回收站 -->
    <div v-if="tabsCollapsed" class="notebook-tabs-switch">
      <el-popover
        v-model:visible="tabSwitchVisible"
        placement="bottom-start"
        :width="132"
        trigger="click"
        popper-class="notebook-tabs-switch-popper"
      >
        <template #reference>
          <button
            type="button"
            class="notebook-tabs-switch__btn"
            :aria-label="t('notebook.tabs.switch')"
          >
            <el-icon :size="16"><Menu /></el-icon>
          </button>
        </template>
        <div class="notebook-tabs-switch__menu">
          <button
            v-for="tab in tabSwitchOptions"
            :key="tab.name"
            type="button"
            class="notebook-tabs-switch__item"
            :class="{ 'is-active': activeTab === tab.name }"
            @click="onTabSwitchSelect(tab.name)"
          >
            <el-icon v-if="activeTab === tab.name" class="notebook-tabs-switch__check"><Check /></el-icon>
            <span>{{ tab.label }}</span>
          </button>
        </div>
      </el-popover>
    </div>
    </div>

    <!-- 树节点右键菜单：笔记/文件夹的右键操作菜单 -->
    <NoteTreeContextMenu
      :visible="contextMenu.visible"
      :x="contextMenu.x"
      :y="contextMenu.y"
      :node="contextMenu.node"
      :can-paste="canPasteClipboard"
      @action="onContextMenuAction"
      @close="closeContextMenu"
    />

    <!-- 移动笔记/文件夹对话框：选择目标文件夹 -->
    <el-dialog
      v-model="moveNoteDialogVisible"
      :title="t('notebook.moveNoteTitle')"
      width="420px"
      destroy-on-close
    >
      <p class="notebook-move-hint">{{ t('notebook.moveNoteHint') }}</p>
      <el-tree
        ref="moveTreeRef"
        class="notebook-move-tree"
        :data="moveFolderTree"
        node-key="nodeKey"
        :props="treeProps"
        highlight-current
        default-expand-all
        :expand-on-click-node="false"
        @node-click="onMoveFolderPick"
      >
        <template #default="{ data }">
          <span class="notebook-move-folder-node">
            <el-icon v-if="data.nodeKey === 'folder-root'"><Folder /></el-icon>
            <el-icon v-else><Folder /></el-icon>
            <span>{{ data.name }}</span>
          </span>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="moveNoteDialogVisible = false">{{ t('pomodoro.common.cancel') }}</el-button>
        <el-button type="primary" :loading="moveNoteSubmitting" @click="submitMoveTarget">
          {{ t('pomodoro.common.save') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 文件夹对话框：新建或重命名文件夹 -->
    <el-dialog
      v-model="folderDialogVisible"
      :title="folderDialogMode === 'create' ? t('notebook.createFolderTitle') : t('notebook.renameFolderTitle')"
      width="420px"
      destroy-on-close
    >
      <el-form @submit.prevent>
        <el-form-item :label="t('notebook.folderName')" required>
          <el-input v-model="folderForm.name" autofocus @keyup.enter="submitFolder" />
        </el-form-item>
        <el-form-item
          v-if="folderDialogMode === 'create'"
          :label="t('notebook.createFolderLocation')"
        >
          <el-radio-group v-model="folderForm.location" class="notebook-folder-location">
            <el-radio v-if="createFolderContext" value="inside">
              {{ t('notebook.createFolderInside', { name: createFolderContext.name }) }}
            </el-radio>
            <el-radio v-if="createFolderContext" value="sibling">
              {{ t('notebook.createFolderSibling', { name: createFolderContext.name }) }}
            </el-radio>
            <el-radio value="root">
              {{ t('notebook.createFolderRoot') }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="folderDialogVisible = false">{{ t('pomodoro.common.cancel') }}</el-button>
        <el-button type="primary" :loading="folderSubmitting" @click="submitFolder">
          {{ t('pomodoro.common.save') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 标签管理对话框：新建和删除标签 -->
    <el-dialog v-model="tagDialogVisible" :title="t('notebook.tagManage')" width="480px" destroy-on-close>
      <div class="tag-manage">
        <div class="tag-manage__new">
          <el-input v-model="newTagName" :placeholder="t('notebook.tagName')" />
          <el-button type="primary" :loading="tagSubmitting" @click="onCreateTag">
            {{ t('notebook.newTag') }}
          </el-button>
        </div>
        <el-table :data="allTags" size="small" max-height="280">
          <el-table-column prop="name" :label="t('notebook.tagName')" />
          <el-table-column :label="t('notebook.actions')" width="80">
            <template #default="{ row }">
              <el-button link type="danger" @click="onDeleteTag(row.id)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
    </div>

    <!-- 全屏阅读视图 -->
    <NoteFullscreenViewer
      v-model:visible="fullscreenViewerVisible"
      :title="editForm.title"
      :html="fullscreenNoteHtml"
      :md-html="fullscreenNoteMdHtml"
      :is-md="isMdNote"
    />
  </WarRoomPage>
</template>

<script setup lang="ts">
/**
 * 笔记本页面组件
 * 提供笔记管理功能，包括笔记列表、富文本编辑、文件夹管理、标签管理
 * 支持百度网盘同步、正文格式优化、目录导航等功能
 */
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch, watchEffect } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
import { ElMessage, ElMessageBox, type ElInput, type ElTree, type MessageHandler } from 'element-plus'
import {
  ArrowDown,
  Check,
  CircleCheck,
  Close,
  CollectionTag,
  Delete,
  Document,
  Edit,
  Folder,
  FolderOpened,
  FullScreen,
  Link,
  Loading,
  Menu,
  MoreFilled,
  Plus,
  Search,
  Star,
  Top,
  View,
} from '@element-plus/icons-vue'
import NotebookFolderView from './notebook/NotebookFolderView.vue'
import NotebookTrashView from './notebook/NotebookTrashView.vue'
import NoteRichEditor from './notebook/NoteRichEditor.vue'
import NoteTocPanel from './notebook/NoteTocPanel.vue'
import NoteFullscreenViewer from './notebook/NoteFullscreenViewer.vue'
import NoteTreeContextMenu, { type TreeContextMenuAction } from './notebook/NoteTreeContextMenu.vue'
import { exportNoteAsWord } from './notebook/exportNoteWord'
import { exportFolderToPdf } from './notebook/exportFolderPdf'
import { parseNoteToc } from './notebook/noteToc'
import { formatNoteDisplayTime, getTagPillStyle } from './notebook/noteDisplay'
import {
  buildFolderOnlyTree,
  buildNoteStubFromTree,
  collectAncestorFolderKeys,
  collectFolderSubtreeIds,
  collectSearchExpandKeys,
  countTreeNodes,
  filterNode,
  findNodeByKey,
  findNodeByNoteId,
} from './notebook/noteTreeUtils'
import { marked } from 'marked'

import type { NoteFolderListMeta } from './notebook/notePreview'
import { isContentLoadFailure } from './notebook/notePreview'
import { useBaiduPanAutoAuth } from '@/composables/useBaiduPanAutoAuth'
import { useNotebookLayout } from './notebook/composables/useNotebookLayout'
import {
  createNoteRequest,
  createNotebook,
  createNoteTag,
  fetchNote,
  fetchNotesMeta,
  fetchNoteTags,
  fetchNotebookTree,
  removeNote,
  removeNoteTag,
  removeNotebook,
  updateNote,
  updateNoteKeepalive,
  updateNotebook,
  type NbNoteSaveRequest,
  type NbNoteDetail,
  type NbNoteTag,
  type NbTreeNode,
} from '@/api/notebook'

const { t } = useI18n() // 国际化函数
const route = useRoute() // 路由实例
const router = useRouter() // 路由操作实例

const { baiduPanStatus, isBaiduAuthPending, redirectToBaiduAuthorize } = useBaiduPanAutoAuth() // 百度网盘授权状态

const activeTab = ref('all') // 当前激活的标签页
// ========== 响应式布局（逻辑已拆至 composables/useNotebookLayout） ==========
const {
  isTabletRange,
  isCompactRange,
  tocVisible,
  sidebarVisible,
  sidebarAriaExpanded,
  tabsCollapsed,
  editorTitleShifted,
} = useNotebookLayout()
const tabSwitchVisible = ref(false) // 平板档标签页切换浮层显隐
// 平板档：标题行右侧「配置」按钮的浮层菜单显隐
const configMenuVisible = ref(false)

const tabSwitchOptions = computed<{ name: 'all' | 'trash'; label: string }[]>(() => [
  { name: 'all', label: t('notebook.tabs.all') },
  { name: 'trash', label: t('notebook.tabs.trash') },
])

function onTabSwitchSelect(name: 'all' | 'trash') {
  tabSwitchVisible.value = false
  if (activeTab.value === name) return
  activeTab.value = name
  void onTabChange(name)
}
const treeLoading = ref(false) // 树结构加载状态
const treeData = ref<NbTreeNode[]>([]) // 笔记文件夹树数据
const trashCount = ref(0) // 回收站数量
const allTags = ref<NbNoteTag[]>([]) // 所有标签列表
const filterText = ref('') // 搜索过滤文本
const searchInputRef = ref<InstanceType<typeof ElInput> | null>(null) // 搜索输入框引用
const isMacPlatform = typeof navigator !== 'undefined' && /Mac|iPhone|iPod|iPad/i.test(navigator.platform) // 是否Mac平台
const searchShortcutLabel = computed(() => (isMacPlatform ? '⌘K' : 'Ctrl+K')) // 搜索快捷键文本
const treeRef = ref<InstanceType<typeof ElTree> | null>(null) // 树组件引用
const moveTreeRef = ref<InstanceType<typeof ElTree> | null>(null) // 移动对话框树引用
const editorRef = ref<InstanceType<typeof NoteRichEditor> | null>(null) // 富文本编辑器引用
const trashViewRef = ref<InstanceType<typeof NotebookTrashView> | null>(null) // 回收站视图引用
const createDropdownVisible = ref(false) // 新建下拉菜单是否可见

const editorRevision = ref(0) // 编辑器版本号（用于强制刷新）
const contentLoadBlocked = ref(false) // 内容加载是否被阻止
const contentLoading = ref(false) // 内容加载状态
let noteLoadSeq = 0 // 笔记加载序列号（防止竞态）
let treeClickDedupeAt = 0 // 树点击去重时间戳
let treeClickDedupeKey = '' // 树点击去重key
let treeSelectionHandling = false // 是否正在处理树选择
let suppressContentChange = false // 内容变更抑制（笔记加载期间避免误触发）
let titleAtOpen = '' // 打开笔记时的原始标题（用于防止 loadNoteDetail 覆盖用户输入）
const userExpandedKeys = ref<Set<string>>(new Set()) // 用户展开的节点key集合
let searchExpandedSnapshot: Set<string> | null = null // 搜索时展开状态快照

const activeNodeKey = ref('') // 当前激活的节点key
const selectedFolderId = ref<number | null>(null) // 当前选中的文件夹ID
const currentNote = ref<NbNoteDetail | null>(null) // 当前打开的笔记

const editForm = reactive({
  // 编辑表单响应式数据
  title: '', // 笔记标题
  content: '', // 笔记内容
  tagIds: [] as number[], // 标签ID列表
})

const saveState = ref<'idle' | 'saving' | 'saved' | 'localSavedSyncFailed'>('idle') // 保存状态

type NoteSnapshot = { title: string; content: string; tagIds: string }

const savedSnapshot = ref<NoteSnapshot | null>(null)

function snapshotFromForm(): NoteSnapshot {
  return {
    title: editForm.title,
    content: editForm.content,
    tagIds: JSON.stringify([...editForm.tagIds].sort((a, b) => a - b)),
  }
}

function syncSavedSnapshot() {
  if (!currentNote.value) {
    savedSnapshot.value = null
    return
  }
  savedSnapshot.value = snapshotFromForm()
}

function isDirty(): boolean {
  if (!currentNote.value || !savedSnapshot.value) return false
  const cur = snapshotFromForm()
  const saved = savedSnapshot.value
  return (
    cur.title !== saved.title ||
    cur.content !== saved.content ||
    cur.tagIds !== saved.tagIds
  )
}

function buildSavePayload(): NbNoteSaveRequest {
  return {
    title: editForm.title,
    content: editForm.content,
    tagIds: editForm.tagIds,
    notebookId: currentNote.value!.notebookId,
  }
}

const folderDialogVisible = ref(false)
const folderDialogMode = ref<'create' | 'rename'>('create')
const folderSubmitting = ref(false)
const editingFolderId = ref<number | null>(null)
const folderForm = reactive({
  name: '',
  location: 'root' as 'inside' | 'sibling' | 'root',
})

const tagDialogVisible = ref(false)
const tagPickerVisible = ref(false)
const newTagName = ref('')
const tagSubmitting = ref(false)
const activeTocIndex = ref(-1)

const fullscreenViewerVisible = ref(false)
const fullscreenNoteHtml = computed(() => isMdNote.value ? '' : (editForm.content || ''))
const fullscreenNoteMdHtml = computed(() => isMdNote.value ? (mdPreviewHtml.value || '') : '')

function openFullscreen() {
  fullscreenViewerVisible.value = true
}

const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  node: null as NbTreeNode | null,
})

const copiedNoteClip = ref<{
  title: string
  content?: string
  tagIds?: number[]
} | null>(null)

const copiedFolderClip = ref<{
  name: string
} | null>(null)

const moveNoteDialogVisible = ref(false)
const moveNoteSubmitting = ref(false)
const moveDialogTarget = ref<{ kind: 'note' | 'folder'; id: number } | null>(null)
const moveTargetNotebookId = ref<number | null>(null)

const canPasteClipboard = computed(() => !!(copiedNoteClip.value || copiedFolderClip.value))

const treeProps = { label: 'name', children: 'children' }

const tocItems = computed(() => {
  // MD 笔记使用渲染后的 HTML 提取标题，否则使用原始内容
  const source = (isMdNote.value && mdPreviewHtml.value)
    ? mdPreviewHtml.value
    : editForm.content
  return parseNoteToc(source)
})

const selectedNoteTags = computed(() =>
  allTags.value.filter((tag) => editForm.tagIds.includes(tag.id)),
)

const noteWordCount = computed(() => {
  const text = editForm.content
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/\u200B/g, '')
    .trim()
  return text.length
})

const formattedUpdateTime = computed(() => formatNoteDisplayTime(currentNote.value?.updateTime, t))

/** 检测当前笔记标题（实时输入）是否以 .md 结尾 */
const isMdNote = computed(() => {
  const title = editForm.title || currentNote.value?.title || ''
  return title.toLowerCase().endsWith('.md')
})

/** MD 笔记视图模式：编辑 / 分屏 / 预览 */
const mdViewMode = ref<'edit' | 'split' | 'preview'>('edit')

/** MD 笔记预览时渲染的 HTML */
const mdPreviewHtml = ref('')

/** 分屏模式左右滚动同步 */
const mdSplitEditorRef = ref<HTMLTextAreaElement | null>(null)
const mdSplitPreviewRef = ref<HTMLDivElement | null>(null)
let mdScrollSyncing = false

function onMdSplitScroll(source: 'editor' | 'preview') {
  if (mdScrollSyncing) return
  mdScrollSyncing = true
  const ta = mdSplitEditorRef.value
  const pv = mdSplitPreviewRef.value
  if (ta && pv) {
    if (source === 'editor') {
      const ratio = ta.scrollTop / (ta.scrollHeight - ta.clientHeight)
      pv.scrollTop = ratio * (pv.scrollHeight - pv.clientHeight)
    } else {
      const ratio = pv.scrollTop / (pv.scrollHeight - pv.clientHeight)
      ta.scrollTop = ratio * (ta.scrollHeight - ta.clientHeight)
    }
  }
  requestAnimationFrame(() => { mdScrollSyncing = false })
}

/** 打开 .md 笔记时默认进入预览模式 */
watch(
  () => currentNote.value,
  (note) => {
    const isMd = note?.title?.toLowerCase().endsWith('.md')
    mdViewMode.value = isMd ? 'preview' : 'edit'
  },
  { immediate: true }
)

/** 监听 MD 笔记内容或视图模式变化，更新预览 HTML */
watchEffect(async () => {
  if (isMdNote.value && mdViewMode.value !== 'edit' && editForm.content) {
    mdPreviewHtml.value = await marked.parse(editForm.content)
  } else if (isMdNote.value && mdViewMode.value !== 'edit') {
    mdPreviewHtml.value = ''
  }
})

const showMetaStatus = computed(() => {
  if (!currentNote.value) return false
  if (contentLoading.value) return true
  if (saveState.value === 'saving' || saveState.value === 'saved' || saveState.value === 'localSavedSyncFailed') return true
  if (contentLoadBlocked.value) return true
  if (!contentLoadBlocked.value && currentNote.value.syncStatus === 'CLOUD_PENDING') return true
  if (!contentLoadBlocked.value && currentNote.value.syncStatus === 'SYNCING') return true
  if (!contentLoadBlocked.value && currentNote.value.syncStatus === 'FAILED') return true
  return false
})

function toggleNoteTag(tagId: number) {
  const index = editForm.tagIds.indexOf(tagId)
  if (index >= 0) {
    editForm.tagIds.splice(index, 1)
  } else {
    editForm.tagIds.push(tagId)
  }
  onContentChanged()
}

function detachNoteTag(tagId: number) {
  editForm.tagIds = editForm.tagIds.filter((id) => id !== tagId)
  onContentChanged()
}

function openTagManage() {
  tagPickerVisible.value = false
  tagDialogVisible.value = true
}

function onHeadingActive(index: number) {
  activeTocIndex.value = index
}

const notebookStats = computed(() => ({
  noteCount: countTreeNodes(treeData.value, 'NOTE'),
  folderCount: countTreeNodes(treeData.value, 'FOLDER'),
  tagCount: allTags.value.length,
  trashCount: trashCount.value,
}))

const moveFolderTree = computed(() => {
  const rootOption: NbTreeNode = {
    nodeKey: 'folder-root',
    nodeType: 'FOLDER',
    name: t('notebook.moveNoteRoot'),
    children: buildFolderOnlyTree(treeData.value),
  }
  return [rootOption]
})

function onTocItemClick(index: number) {
  activeTocIndex.value = index
  if (isMdNote.value && mdViewMode.value !== 'edit') {
    scrollMdToHeading(index)
  } else {
    editorRef.value?.scrollToHeading(index)
  }
}

/**
 * MD 预览/分屏模式下滚动到指定标题
 */
function scrollMdToHeading(index: number) {
  const container = mdViewMode.value === 'split'
    ? mdSplitPreviewRef.value
    : document.querySelector<HTMLElement>('.notebook-editor__md-preview:not(.is-split)')
  if (!container) return
  const headings = container.querySelectorAll<HTMLElement>('h1,h2,h3,h4,h5,h6')
  const target = headings[index]
  if (!target) return
  target.scrollIntoView({ behavior: 'instant', block: 'start' })
}

async function restoreTreeSelectionView() {
  if (!activeNodeKey.value) return
  await nextTick()
  const ancestorKeys = collectAncestorFolderKeys(treeData.value, activeNodeKey.value) ?? []
  ancestorKeys.forEach((key) => {
    treeRef.value?.getNode(key)?.expand(null, true)
  })
  treeRef.value?.setCurrentKey(activeNodeKey.value)
  syncOpenNoteFromActiveKey()
  await nextTick()
  const treeEl = treeRef.value?.$el as HTMLElement | undefined
  treeEl?.querySelector('.el-tree-node.is-current')?.scrollIntoView({ block: 'nearest' })
}

function syncOpenNoteFromActiveKey() {
  if (!activeNodeKey.value) return
  const node = findNodeByKey(treeData.value, activeNodeKey.value)
  if (!node) {
    activeNodeKey.value = ''
    currentNote.value = null
    return
  }
  if (node.nodeType === 'FOLDER') {
    selectedFolderId.value = node.notebookId ?? null
    currentNote.value = null
    contentLoading.value = false
    return
  }
  if (node.nodeType === 'NOTE' && node.noteId) {
    if (currentNote.value?.id === node.noteId) return
    beginOpenNote(node.noteId, buildNoteStubFromTree(node), node.nodeKey)
  }
}

watch(filterText, async (val) => {
  treeRef.value?.filter(val)
  await nextTick()
  const keyword = val.trim()
  if (keyword) {
    if (!searchExpandedSnapshot) {
      searchExpandedSnapshot = new Set(userExpandedKeys.value)
    }
    expandFoldersForSearch(keyword)
  } else {
    if (searchExpandedSnapshot) {
      userExpandedKeys.value = new Set(searchExpandedSnapshot)
      searchExpandedSnapshot = null
    }
    restoreUserTreeExpansion()
  }
})

function onTreeNodeExpand(data: NbTreeNode) {
  if (data.nodeType !== 'FOLDER' || filterText.value.trim()) return
  userExpandedKeys.value.add(data.nodeKey)
}

function onTreeNodeCollapse(data: NbTreeNode) {
  if (data.nodeType !== 'FOLDER' || filterText.value.trim()) return
  userExpandedKeys.value.delete(data.nodeKey)
}

function expandTreeNodes(keys: string[]) {
  const tree = treeRef.value
  if (!tree) return
  keys.forEach((key) => {
    tree.getNode(key)?.expand(null, true)
  })
}

function expandFoldersForSearch(keyword: string) {
  const keys = collectSearchExpandKeys(treeData.value, keyword)
  expandTreeNodes(keys)
}

function restoreUserTreeExpansion() {
  const tree = treeRef.value
  if (!tree) return

  tree.store._getAllNodes().forEach((node) => {
    if (node.childNodes?.length) node.collapse()
  })
  expandTreeNodes([...userExpandedKeys.value])
}

function ensureAncestorsExpanded(nodeKey: string) {
  const ancestorKeys = collectAncestorFolderKeys(treeData.value, nodeKey) ?? []
  ancestorKeys.forEach((key) => userExpandedKeys.value.add(key))
  expandTreeNodes(ancestorKeys)
}

async function loadTree(keepSelection = true) {
  treeLoading.value = true
  const selectionAtStart = activeNodeKey.value
  try {
    treeData.value = await fetchNotebookTree()
    if (keepSelection && selectionAtStart) {
      if (activeNodeKey.value === selectionAtStart) {
        const stillExists = findNodeByKey(treeData.value, selectionAtStart)
        if (stillExists) {
          activeNodeKey.value = selectionAtStart
        } else {
          activeNodeKey.value = ''
          currentNote.value = null
        }
      } else if (activeNodeKey.value && !findNodeByKey(treeData.value, activeNodeKey.value)) {
        activeNodeKey.value = ''
        currentNote.value = null
      }
    }
    await nextTick()
    if (!filterText.value.trim()) {
      restoreUserTreeExpansion()
    }
    if (keepSelection && activeNodeKey.value) {
      await restoreTreeSelectionView()
    }
  } finally {
    treeLoading.value = false
  }
}
async function loadTags() {
  allTags.value = await fetchNoteTags()
}

async function onTabChange(tab: string | number) {
  createDropdownVisible.value = false
  if (tab === 'trash') {
    void flushSaveInBackground()
    await trashViewRef.value?.reload()
  }
}

function capturePendingSave(): { noteId: number; payload: NbNoteSaveRequest } | null {
  if (contentLoadBlocked.value || contentLoading.value) {
    return null
  }
  if (!currentNote.value?.id || !isDirty()) {
    return null
  }
  const noteId = currentNote.value.id
  const payload = buildSavePayload()
  syncSavedSnapshot()
  return { noteId, payload }
}

async function saveNoteInBackground(noteId: number, payload: NbNoteSaveRequest) {
  try {
    await updateNote(noteId, payload)
  } catch {
    ElMessage.error(t('notebook.saveFailed'))
  }
}

function flushSaveInBackground() {
  const pending = capturePendingSave()
  if (!pending) return
  void saveNoteInBackground(pending.noteId, pending.payload)
}



async function onTreeNodeClick(data: NbTreeNode) {
  if (treeSelectionHandling) return
  const dedupeKey = `${data.nodeKey}:${data.nodeType}`
  const now = Date.now()
  if (treeClickDedupeKey === dedupeKey && now - treeClickDedupeAt < 80) {
    return
  }
  treeClickDedupeKey = dedupeKey
  treeClickDedupeAt = now

  closeContextMenu()
  if (data.nodeKey === activeNodeKey.value && data.nodeType === 'NOTE' && currentNote.value?.id === data.noteId) {
    return
  }
  if (data.nodeKey === activeNodeKey.value && data.nodeType === 'FOLDER' && !currentNote.value) {
    return
  }

  const pending = capturePendingSave()

  treeSelectionHandling = true
  try {
    if (data.nodeType === 'FOLDER') {
      selectedFolderId.value = data.notebookId ?? null
      activeNodeKey.value = data.nodeKey
      currentNote.value = null
      savedSnapshot.value = null
      contentLoadBlocked.value = false
      contentLoading.value = false
    } else if (data.nodeType === 'NOTE' && data.noteId) {
      beginOpenNote(data.noteId, buildNoteStubFromTree(data), data.nodeKey)
      ensureAncestorsExpanded(activeNodeKey.value)
    }

    await nextTick()
    treeRef.value?.setCurrentKey(activeNodeKey.value || undefined)
  } finally {
    treeSelectionHandling = false
  }

  if (data.nodeType === 'NOTE' && data.noteId) {
    updateUrlNoteId(data.noteId)
  } else if (data.nodeType === 'FOLDER') {
    updateUrlNoteId(null)
  }

  if (pending) {
    void saveNoteInBackground(pending.noteId, pending.payload)
  }
}

function onTreeNodeContextMenu(event: MouseEvent, data: NbTreeNode) {
  if (data.nodeType === 'NOTE' && !data.noteId) return
  if (data.nodeType === 'FOLDER' && !data.notebookId) return
  if (data.nodeType !== 'NOTE' && data.nodeType !== 'FOLDER') return
  event.preventDefault()
  event.stopPropagation()
  contextMenu.visible = true
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.node = data
}

function closeContextMenu() {
  contextMenu.visible = false
  contextMenu.node = null
}

async function onContextMenuAction(action: TreeContextMenuAction) {
  const node = contextMenu.node
  if (!node) return

  if (node.nodeType === 'FOLDER') {
    await onFolderContextMenuAction(action, node)
    return
  }

  if (!node.noteId) return

  switch (action) {
    case 'pin':
      await toggleNotePin(node)
      break
    case 'favorite':
      await toggleNoteFavorite(node)
      break
    case 'rename':
      await renameNoteFromMenu(node)
      break
    case 'move':
      openMoveNoteDialog(node.noteId)
      break
    case 'copy':
      await copyNoteToClipboard(node.noteId)
      break
    case 'paste':
      await pasteNoteFromClipboard(node)
      break
    case 'delete':
      await deleteNoteFromMenu(node)
      break
    case 'exportWord':
      await exportNoteFromMenu(node.noteId)
      break
  }
}

async function onFolderContextMenuAction(action: TreeContextMenuAction, node: NbTreeNode) {
  if (!node.notebookId) return

  switch (action) {
    case 'pin':
      await pinFolderFromMenu(node)
      break
    case 'rename':
      await renameFolderFromMenu(node)
      break
    case 'move':
      openMoveFolderDialog(node.notebookId)
      break
    case 'copy':
      copyFolderToClipboard(node)
      break
    case 'paste':
      await pasteIntoFolderFromMenu(node)
      break
    case 'delete':
      await deleteFolderFromMenu(node)
      break
    case 'exportPdf':
      await exportFolderToPdfFromMenu(node)
      break
    default:
      break
  }
}

async function toggleNotePin(node: NbTreeNode) {
  if (!node.noteId) return
  const pinned = node.isPinned !== 1
  await updateNote(node.noteId, { pinned })
  if (currentNote.value?.id === node.noteId) {
    currentNote.value = { ...currentNote.value, isPinned: pinned ? 1 : 0 }
  }
  await loadTree()
}

async function toggleNoteFavorite(node: NbTreeNode) {
  if (!node.noteId) return
  const favorite = node.isFavorite !== 1
  await updateNote(node.noteId, { favorite })
  if (currentNote.value?.id === node.noteId) {
    currentNote.value = { ...currentNote.value, isFavorite: favorite ? 1 : 0 }
  }
  await loadTree()
}

async function renameNoteFromMenu(node: NbTreeNode) {
  if (!node.noteId) return
  const { value } = await ElMessageBox.prompt(
    t('notebook.renameNoteTitle'),
    t('notebook.renameNote'),
    {
      inputValue: node.name,
      confirmButtonText: t('pomodoro.common.save'),
      cancelButtonText: t('pomodoro.common.cancel'),
    },
  )
  const title = value.trim()
  if (!title) return
  const updated = await updateNote(node.noteId, { title })
  if (currentNote.value?.id === node.noteId) {
    currentNote.value = updated
    editForm.title = updated.title
    syncSavedSnapshot()
  }
  await loadTree()
  treeRef.value?.setCurrentKey(node.nodeKey)
}

function openMoveNoteDialog(noteId: number) {
  moveDialogTarget.value = { kind: 'note', id: noteId }
  moveTargetNotebookId.value = null
  moveNoteDialogVisible.value = true
  nextTick(() => {
    moveTreeRef.value?.setCurrentKey('folder-root')
  })
}

function openMoveFolderDialog(folderId: number) {
  moveDialogTarget.value = { kind: 'folder', id: folderId }
  moveTargetNotebookId.value = null
  moveNoteDialogVisible.value = true
  nextTick(() => {
    moveTreeRef.value?.setCurrentKey('folder-root')
  })
}

function onMoveFolderPick(data: NbTreeNode) {
  moveTreeRef.value?.setCurrentKey(data.nodeKey)
  if (data.nodeKey === 'folder-root') {
    moveTargetNotebookId.value = null
    return
  }
  moveTargetNotebookId.value = data.notebookId ?? null
}

async function submitMoveTarget() {
  if (!moveDialogTarget.value) return
  moveNoteSubmitting.value = true
  try {
    const targetFolderId = moveTargetNotebookId.value
    if (moveDialogTarget.value.kind === 'folder') {
      const forbidden = collectFolderSubtreeIds(treeData.value, moveDialogTarget.value.id)
      if (targetFolderId != null && forbidden.includes(targetFolderId)) {
        ElMessage.warning(t('notebook.moveFolderInvalid'))
        return
      }
      const node = findNodeByKey(treeData.value, `folder-${moveDialogTarget.value.id}`)
      if (!node) return
      await updateNotebook(moveDialogTarget.value.id, {
        name: node.name,
        parentId: targetFolderId,
      })
    } else {
      await updateNote(moveDialogTarget.value.id, { notebookId: targetFolderId })
      if (currentNote.value?.id === moveDialogTarget.value.id) {
        currentNote.value.notebookId = targetFolderId ?? undefined
      }
    }
    moveNoteDialogVisible.value = false
    ElMessage.success(t('pomodoro.common.saved'))
    await loadTree()
  } finally {
    moveNoteSubmitting.value = false
  }
}

async function pinFolderFromMenu(node: NbTreeNode) {
  if (!node.notebookId) return
  await updateNotebook(node.notebookId, {
    name: node.name,
    parentId: node.parentId ?? null,
    sortOrder: -Date.now(),
  })
  await loadTree()
  treeRef.value?.setCurrentKey(node.nodeKey)
  ElMessage.success(t('notebook.pin'))
}

async function renameFolderFromMenu(node: NbTreeNode) {
  if (!node.notebookId) return
  const { value } = await ElMessageBox.prompt(
    t('notebook.renameFolderTitle'),
    t('notebook.renameFolder'),
    {
      inputValue: node.name,
      confirmButtonText: t('pomodoro.common.save'),
      cancelButtonText: t('pomodoro.common.cancel'),
    },
  )
  const name = value.trim()
  if (!name) return
  await updateNotebook(node.notebookId, {
    name,
    parentId: node.parentId ?? null,
  })
  await loadTree()
  treeRef.value?.setCurrentKey(node.nodeKey)
}

function copyFolderToClipboard(node: NbTreeNode) {
  copiedFolderClip.value = { name: node.name }
  ElMessage.success(t('notebook.copy'))
}

async function pasteIntoFolderFromMenu(node: NbTreeNode) {
  if (copiedNoteClip.value) {
    const created = await createNoteRequest({
      notebookId: node.notebookId ?? null,
      title: copiedNoteClip.value.title,
      content: copiedNoteClip.value.content,
      tagIds: copiedNoteClip.value.tagIds,
    })
    await loadTree()
    activeNodeKey.value = `note-${created.id}`
    ensureAncestorsExpanded(activeNodeKey.value)
    treeRef.value?.setCurrentKey(activeNodeKey.value)
    beginOpenNote(created.id, created, activeNodeKey.value)
    ElMessage.success(t('notebook.paste'))
    return
  }
  if (!copiedFolderClip.value) return
  await createNotebook({
    name: `${copiedFolderClip.value.name}${t('notebook.copySuffix')}`,
    parentId: node.notebookId ?? null,
  })
  await loadTree()
  ElMessage.success(t('notebook.paste'))
}

async function deleteFolderFromMenu(node: NbTreeNode) {
  if (!node.notebookId) return
  await ElMessageBox.confirm(t('notebook.deleteFolderConfirm'), { type: 'warning' })
  await removeNotebook(node.notebookId)
  if (activeNodeKey.value === node.nodeKey) {
    selectedFolderId.value = null
    activeNodeKey.value = ''
  }
  ElMessage.success(t('pomodoro.common.deleted'))
  await loadTree(false)
}

async function copyNoteToClipboard(noteId: number) {
  const detail = await fetchNote(noteId)
  copiedNoteClip.value = {
    title: `${detail.title}${t('notebook.copySuffix')}`,
    content: detail.content,
    tagIds: detail.tags?.map((tag) => tag.id),
  }
  ElMessage.success(t('notebook.copy'))
}

async function pasteNoteFromClipboard(node: NbTreeNode) {
  if (!copiedNoteClip.value) return
  const created = await createNoteRequest({
    notebookId: node.notebookId ?? null,
    title: copiedNoteClip.value.title,
    content: copiedNoteClip.value.content,
    tagIds: copiedNoteClip.value.tagIds,
  })
  await loadTree()
  activeNodeKey.value = `note-${created.id}`
  ensureAncestorsExpanded(activeNodeKey.value)
  treeRef.value?.setCurrentKey(activeNodeKey.value)
  beginOpenNote(created.id, created, activeNodeKey.value)
  ElMessage.success(t('notebook.paste'))
}

async function deleteNoteFromMenu(node: NbTreeNode) {
  if (!node.noteId) return
  await ElMessageBox.confirm(t('notebook.deleteNoteConfirm'), { type: 'warning' })
  await removeNote(node.noteId)
  if (currentNote.value?.id === node.noteId) {
    currentNote.value = null
    savedSnapshot.value = null
    contentLoading.value = false
    activeNodeKey.value = ''
    editForm.title = ''
    editForm.content = ''
    editForm.tagIds = []
  }
  ElMessage.success(t('pomodoro.common.deleted'))
  await loadTree(false)
  void trashViewRef.value?.reload()
}

async function exportNoteFromMenu(noteId: number) {
  const detail = await fetchNote(noteId)
  exportNoteAsWord(detail.title, detail.content ?? '')
}

/** 文件夹右键「导出 PDF」：收集直属笔记 → 排序 → 并发拉正文 → 打开打印窗口另存为 PDF */
async function exportFolderToPdfFromMenu(node: NbTreeNode) {
  if (!node.notebookId) return
  // 进度消息实例：用对象属性承载，规避 TS 闭包捕获对 let 变量的收紧推断
  const progress = { msg: null as MessageHandler | null }
  try {
    const result = await exportFolderToPdf(node, {
      onProgress: (done, total) => {
        // 进度用持久消息，更新时关闭旧实例，避免消息堆积
        progress.msg?.close()
        progress.msg = ElMessage({
          message: t('notebook.exportFolderPdfProgress', { done, total }),
          duration: 0,
        })
      },
    })
    progress.msg?.close()
    if (result.exported === 0) {
      // 空文件夹或全部失败时给出可理解的提示
      ElMessage.warning(
        result.failed.length
          ? t('notebook.exportFolderPdfAllFailed')
          : t('notebook.exportFolderPdfEmpty'),
      )
      return
    }
    ElMessage.success(t('notebook.exportFolderPdfSuccess', { count: result.exported }))
    if (result.failed.length) {
      ElMessage.warning(t('notebook.exportFolderPdfFailed', { count: result.failed.length }))
    }
  } catch (error) {
    progress.msg?.close()
    // 不吞异常：留日志并提示用户
    console.error('[notebook] 文件夹导出 PDF 失败', error)
    ElMessage.error(t('notebook.exportFolderPdfAllFailed'))
  }
}

function beginOpenNote(noteId: number, stub: NbNoteDetail, nodeKey?: string) {
  const seq = ++noteLoadSeq
  if (nodeKey) {
    activeNodeKey.value = nodeKey
    selectedFolderId.value = stub.notebookId ?? null
  }
  currentNote.value = stub
  titleAtOpen = stub.title
  editForm.title = stub.title
  editForm.content = ''
  editForm.tagIds = []
  savedSnapshot.value = null
  saveState.value = 'idle'
  contentLoadBlocked.value = false
  contentLoading.value = true
  // 桌面档（>1200）打开笔记时展开目录；平板档保持折叠（可手动展开）。
  // 紧凑档（≤1300）统一收抽屉：覆盖树点击/新建/粘贴全部调用点。
  // 边界：紧凑档右键重命名 → loadTree → beginOpenNote 会收掉刚用过的抽屉，属可容忍行为。
  if (!isTabletRange.value) tocVisible.value = true
  if (isCompactRange.value) sidebarVisible.value = false
  activeTocIndex.value = -1
  void loadNoteDetail(noteId, seq)
}

async function loadNoteDetail(noteId: number, seq?: number) {
  const expectedSeq = seq ?? ++noteLoadSeq
  if (seq === undefined) {
    contentLoading.value = true
  }
  contentLoadBlocked.value = false
  suppressContentChange = true
  try {
    const detail = await fetchNote(noteId)
    if (expectedSeq !== noteLoadSeq || currentNote.value?.id !== noteId) {
      return
    }
    currentNote.value = detail
    // 仅在 API 返回了与当前树节点标题不同的标题时才覆盖，避免 API 返回相同值（如"无标题"）时自覆盖
    if (editForm.title === titleAtOpen && detail.title && detail.title !== titleAtOpen) {
      editForm.title = detail.title
    }
    const rawContent = detail.content ?? ''
    let content = rawContent
    if (isContentLoadFailure(detail, rawContent)) {
      contentLoadBlocked.value = true
      ElMessage.warning(t('notebook.contentLoadFailed'))
      content = ''
      editorRevision.value += 1
    }
    editForm.content = content
    editForm.tagIds = detail.tags?.map((tag) => tag.id) ?? []
    // 快照基于 API 返回的实际值（而非表单当前值，表单可能包含未保存的用户编辑）
    savedSnapshot.value = {
      title: detail.title ?? '',
      content: content,
      tagIds: JSON.stringify([...(detail.tags?.map(t => t.id) ?? [])].sort((a, b) => a - b)),
    }
    saveState.value = editForm.title !== (detail.title ?? '') ? 'idle' : 'saved'
  } catch {
    if (expectedSeq !== noteLoadSeq || currentNote.value?.id !== noteId) {
      return
    }
    contentLoadBlocked.value = true
    ElMessage.warning(t('notebook.contentLoadFailed'))
    editForm.content = ''
    editorRevision.value += 1
    saveState.value = 'saved'
  } finally {
    if (expectedSeq === noteLoadSeq && currentNote.value?.id === noteId) {
      contentLoading.value = false
    } else if (expectedSeq === noteLoadSeq) {
      contentLoading.value = false
    }
    await nextTick()
    suppressContentChange = false
  }
}

function onContentChanged() {
  if (suppressContentChange) return
  saveState.value = 'idle'
}

async function manualSave() {
  const noteId = currentNote.value?.id
  if (!noteId || contentLoadBlocked.value || contentLoading.value || saveState.value === 'saving') return
  if (!isDirty()) {
    if (currentNote.value?.syncStatus === 'FAILED') {
      saveState.value = 'localSavedSyncFailed'
    } else {
      saveState.value = 'saved'
    }
    return
  }
  const payload = buildSavePayload()
  saveState.value = 'saving'
  try {
    const updated = await updateNote(noteId, payload)
    if (currentNote.value?.id === noteId) {
      currentNote.value = updated
      syncSavedSnapshot()
      // 直接更新树节点标题，避免 reload 整棵树触发 beginOpenNote 等副作用
      if (activeNodeKey.value) {
        const node = findNodeByKey(treeData.value, activeNodeKey.value)
        if (node) {
          node.name = updated.title ?? ''
        }
      }
    }
    if (updated?.syncStatus === 'FAILED') {
      saveState.value = 'localSavedSyncFailed'
    } else {
      saveState.value = 'saved'
    }
    if (currentNote.value?.id === noteId && updated?.syncStatus !== 'FAILED') {
      treeRef.value?.setCurrentKey(activeNodeKey.value)
    }
  } catch {
    if (currentNote.value?.id === noteId) {
      saveState.value = 'idle'
    }
    ElMessage.error(t('notebook.saveFailed'))
  }
}

function flushSaveOnUnload() {
  if (contentLoadBlocked.value || contentLoading.value) return
  const note = currentNote.value
  if (!note?.id || !isDirty()) return
  updateNoteKeepalive(note.id, buildSavePayload())
  syncSavedSnapshot()
}

function focusSearchInput() {
  const input = searchInputRef.value?.$el?.querySelector('input') as HTMLInputElement | null
  input?.focus()
  input?.select()
}

function onSearchShortcut(event: KeyboardEvent) {
  if (event.key.toLowerCase() !== 'k') return
  const withMod = isMacPlatform ? event.metaKey : event.ctrlKey
  if (!withMod) return
  event.preventDefault()
  focusSearchInput()
}

function onSaveKeydown(event: KeyboardEvent) {
  if (event.key.toLowerCase() !== 's') return
  const withMod = isMacPlatform ? event.metaKey : event.ctrlKey
  if (!withMod) return
  event.preventDefault()
  void manualSave()
}

function onSidebarCreateCommand(command: string) {
  createDropdownVisible.value = false
  if (command === 'folder') openCreateFolder()
}

function shouldKeepCreateDropdownOpen(target: HTMLElement) {
  return (
    target.closest('.notebook-sidebar__create-more') != null ||
    target.closest('.notebook-sidebar__create-dropdown-popper') != null
  )
}

function closeCreateDropdownIfOutside(event: MouseEvent) {
  if (!createDropdownVisible.value) return
  const target = event.target as HTMLElement | null
  if (!target || shouldKeepCreateDropdownOpen(target)) return
  createDropdownVisible.value = false
}

function onDocumentClickForCreateDropdown(event: MouseEvent) {
  closeCreateDropdownIfOutside(event)
}

async function openCreateNote() {
  createDropdownVisible.value = false
  const pending = capturePendingSave()
  if (pending) {
    void saveNoteInBackground(pending.noteId, pending.payload)
  }
  const created = await createNoteRequest({
    notebookId: selectedFolderId.value,
    title: t('notebook.untitled'),
    content: '',
    tagIds: [],
  })
  await loadTree()
  activeNodeKey.value = `note-${created.id}`
  ensureAncestorsExpanded(activeNodeKey.value)
  treeRef.value?.setCurrentKey(activeNodeKey.value)
  beginOpenNote(created.id, created, activeNodeKey.value)
}

const folderNoteMeta = ref<Record<number, NoteFolderListMeta>>({})

const selectedFolderNode = computed(() => {
  if (!activeNodeKey.value || currentNote.value) return null
  const node = findNodeByKey(treeData.value, activeNodeKey.value)
  return node?.nodeType === 'FOLDER' ? node : null
})

watch(
  selectedFolderNode,
  async (node) => {
    folderNoteMeta.value = {}
    if (!node?.children?.length) return
    const noteIds = node.children
      .filter((child) => child.nodeType === 'NOTE' && child.noteId)
      .map((child) => child.noteId as number)
    if (!noteIds.length) return
    try {
      const metaList = await fetchNotesMeta(noteIds)
      folderNoteMeta.value = Object.fromEntries(
        metaList.map((item) => [
          item.id,
          {
            contentExcerpt: item.contentExcerpt,
            createTime: item.createTime,
            size: item.contentSize ?? 0,
          },
        ]),
      )
    } catch {
      folderNoteMeta.value = {}
    }
  },
  { immediate: true },
)

function openBaiduPanAuthorize() {
  redirectToBaiduAuthorize()
}

async function onFolderViewOpenFolder(node: NbTreeNode) {
  await onTreeNodeClick(node)
}

async function onFolderViewOpenNote(node: NbTreeNode) {
  await onTreeNodeClick(node)
}

const createFolderContext = computed(() => {
  if (activeNodeKey.value) {
    const node = findNodeByKey(treeData.value, activeNodeKey.value)
    if (!node) return null
    if (node.nodeType === 'FOLDER') return node
    if (node.notebookId) {
      return findNodeByKey(treeData.value, `folder-${node.notebookId}`)
    }
  }
  if (currentNote.value?.notebookId) {
    return findNodeByKey(treeData.value, `folder-${currentNote.value.notebookId}`)
  }
  return null
})

function updateUrlNoteId(noteId: number | null) {
  const query = { ...route.query }
  if (noteId) {
    query.note = String(noteId)
  } else {
    delete query.note
  }
  router.replace({ query })
}

function onTreeBlankClick() {
  activeNodeKey.value = ''
  currentNote.value = null
  selectedFolderId.value = null
  contentLoading.value = false
  treeRef.value?.setCurrentKey(undefined)
  updateUrlNoteId(null)
}

function getCreateFolderParentId(): number | null {
  if (folderForm.location === 'root') return null
  const context = createFolderContext.value
  if (!context) return null
  if (folderForm.location === 'sibling') return context.parentId ?? null
  return context.notebookId ?? null
}

function openCreateFolder() {
  folderDialogMode.value = 'create'
  editingFolderId.value = null
  folderForm.name = ''
  folderForm.location = createFolderContext.value ? 'inside' : 'root'
  folderDialogVisible.value = true
}

async function submitFolder() {
  const name = folderForm.name.trim()
  if (!name) {
    ElMessage.warning(t('notebook.folderName'))
    return
  }
  folderSubmitting.value = true
  try {
    if (folderDialogMode.value === 'create') {
      await createNotebook({
        name,
        parentId: getCreateFolderParentId(),
      })
    } else if (editingFolderId.value) {
      const node = findNodeByKey(treeData.value, `folder-${editingFolderId.value}`)
      await updateNotebook(editingFolderId.value, {
        name,
        parentId: node?.parentId ?? null,
      })
    }
    folderDialogVisible.value = false
    await loadTree()
  } finally {
    folderSubmitting.value = false
  }
}

async function togglePin() {
  if (!currentNote.value) return
  const pinned = currentNote.value.isPinned !== 1
  const updated = await updateNote(currentNote.value.id, { pinned })
  currentNote.value = updated
  await loadTree()
}

async function toggleFavorite() {
  if (!currentNote.value) return
  const favorite = currentNote.value.isFavorite !== 1
  const updated = await updateNote(currentNote.value.id, { favorite })
  currentNote.value = updated
}

async function onCreateTag() {
  const name = newTagName.value.trim()
  if (!name) return
  tagSubmitting.value = true
  try {
    await createNoteTag({ name })
    newTagName.value = ''
    await loadTags()
  } finally {
    tagSubmitting.value = false
  }
}

async function onDeleteTag(id: number) {
  await removeNoteTag(id)
  await loadTags()
  if (currentNote.value) {
    editForm.tagIds = editForm.tagIds.filter((tagId) => tagId !== id)
  }
}

function onPageHide() {
  flushSaveOnUnload()
}

function onVisibilityChange() {
  if (document.visibilityState === 'hidden') {
    flushSaveInBackground()
  }
}

onBeforeRouteLeave(() => {
  flushSaveInBackground()
})

watch(createDropdownVisible, (visible) => {
  if (visible) {
    window.setTimeout(() => {
      document.addEventListener('click', onDocumentClickForCreateDropdown, true)
    }, 0)
    return
  }
  document.removeEventListener('click', onDocumentClickForCreateDropdown, true)
})

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'trash') {
      activeTab.value = 'trash'
    }
  },
)

onMounted(async () => {
  window.addEventListener('pagehide', onPageHide)
  window.addEventListener('keydown', onSearchShortcut)
  window.addEventListener('keydown', onSaveKeydown)
  document.addEventListener('visibilitychange', onVisibilityChange)
  document.addEventListener('click', closeContextMenu)
  document.addEventListener('scroll', closeContextMenu, true)
  await Promise.all([loadTree(false), loadTags()])
  if (route.query.tab === 'trash') {
    activeTab.value = 'trash'
  }
  const restoreNoteId = route.query.note ? Number(route.query.note) : null
  if (restoreNoteId && activeTab.value !== 'trash') {
    const node = findNodeByNoteId(treeData.value, restoreNoteId)
    if (node) {
      await onTreeNodeClick(node)
    }
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('pagehide', onPageHide)
  window.removeEventListener('keydown', onSearchShortcut)
  window.removeEventListener('keydown', onSaveKeydown)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  document.removeEventListener('click', closeContextMenu)
  document.removeEventListener('click', onDocumentClickForCreateDropdown, true)
  document.removeEventListener('scroll', closeContextMenu, true)
  flushSaveOnUnload()
})
</script>

<style scoped lang="scss">
.notebook-page {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
  padding: 0;
  background: transparent;
  border: none;
  box-shadow: none;
}

.war-room-panel--notebook {
  margin: 0;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px 20px;
  background: var(--wr-card);
  border: 1px solid var(--wr-border);
  border-radius: 12px;
  box-shadow: var(--wr-shadow);
}

.notebook-tab-all {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  gap: 16px;
}

.notebook-stats {
  display: grid;
  /* 下限 185px：1024 视口（内容宽 834px）可落 4 列、769 视口落 3 列，贴合平板档验收；>1200 桌面仍 4 列 */
  grid-template-columns: repeat(auto-fit, minmax(185px, 1fr));
  gap: 12px;
  width: 100%;
  flex-shrink: 0;
}

.notebook-stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 88px;
  min-width: 0;
  padding: 16px 18px;
  box-sizing: border-box;
  border: 1px solid transparent;
  border-radius: 14px;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);

  &--blue {
    background: var(--wr-stat-blue-bg);
    border-color: #bfdbfe;

    .notebook-stat-card__value {
      color: var(--wr-stat-blue);
    }
  }

  &--green {
    background: var(--wr-stat-green-bg);
    border-color: #bbf7d0;

    .notebook-stat-card__value {
      color: var(--wr-stat-green);
    }
  }

  &--purple {
    background: var(--wr-stat-purple-bg);
    border-color: #ddd6fe;

    .notebook-stat-card__value {
      color: var(--wr-stat-purple);
    }
  }

  &--orange {
    background: var(--wr-stat-orange-bg);
    border-color: #fed7aa;

    .notebook-stat-card__value {
      color: var(--wr-stat-orange);
    }
  }
}

.notebook-stat-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  flex-shrink: 0;
  font-size: 24px;
  color: #fff;

  &--blue {
    background: #3b82f6;
  }

  &--green {
    background: #22c55e;
  }

  &--purple {
    background: #8b5cf6;
  }

  &--orange {
    background: #f59e0b;
  }
}

.notebook-stat-card__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.notebook-stat-card__label {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notebook-stat-card__value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.notebook-page__title {
  display: none;
}

.notebook-page__baidu-alert {
  margin-bottom: 12px;
  flex-shrink: 0;
}

.notebook-tabs-wrap {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* 页面头部右侧的百度网盘绑定按钮（link 样式、不悬浮） */
.notebook-bind-pan-btn {
  &.is-bound {
    color: var(--el-color-success);
  }
}

.notebook-tabs-switch {
  position: absolute;
  top: 4px;
  left: 8px;
  z-index: 10;
}

.notebook-tabs-switch__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid var(--wr-border);
  border-radius: 8px;
  background: var(--wr-card);
  color: var(--wr-text-secondary);
  cursor: pointer;
  box-shadow: var(--wr-shadow);

  &:hover {
    color: var(--wr-rail-active-color);
    background: var(--wr-stat-blue-bg);
  }
}

:deep(.notebook-tabs) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  > .el-tabs__header {
    flex-shrink: 0;
    margin-bottom: 16px;
    overflow: visible;
  }

  > .el-tabs__nav-wrap::after {
    height: 1px;
    background: var(--wr-border);
  }

  .el-tabs__item {
    font-size: 14px;
    color: var(--wr-text-secondary);

    &.is-active {
      color: var(--wr-rail-active-color);
      font-weight: 600;
    }

    &:hover {
      color: var(--wr-text);
    }
  }

  .el-tabs__active-bar {
    height: 2px;
    background: var(--wr-rail-active-color);
  }

  > .el-tabs__content {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  .el-tab-pane {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
    overflow: hidden;
  }
}

.notebook-layout {
  position: relative;
  display: flex;
  gap: 0;
  flex: 1;
  min-height: 0;
  border: 1px solid var(--wr-border);
  border-radius: 12px;
  overflow: hidden;
  background: var(--wr-card);
}

.notebook-sidebar {
  width: 280px;
  flex-shrink: 0;
  min-width: 0;
  overflow: hidden;
  border-right: 1px solid var(--wr-border);
  display: flex;
  flex-direction: column;
  background: var(--wr-card);
}

.notebook-workspace {
  flex: 1;
  display: flex;
  min-width: 0;
  min-height: 0;
  position: relative;
  overflow: hidden;
  align-items: stretch;
}

.notebook-editor {
  flex: 1;
  min-width: 0;
  min-height: 0;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px 20px;
  box-sizing: border-box;
}

.notebook-toc {
  width: 240px;
  flex-shrink: 0;
  min-width: 0;
  align-self: flex-start;
  height: auto;
  max-height: 100%;
  display: flex;
  flex-direction: column;
  margin: 12px 12px 12px 0;
  border: 1px solid var(--wr-border);
  border-radius: 12px;
  background: var(--wr-card);
  box-shadow: var(--wr-shadow);
  overflow: hidden;
  transition: width 0.2s ease, opacity 0.2s ease, margin 0.2s ease;

  &.is-collapsed {
    width: 0;
    margin-right: 0;
    opacity: 0;
    pointer-events: none;
    border: none;
    overflow: hidden;
  }

  :deep(.note-toc-panel) {
    padding: 0;
  }
}

.notebook-toc__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 14px 14px 10px;
  min-height: 44px;
  box-sizing: border-box;
  flex-shrink: 0;
  border-bottom: 1px solid var(--wr-border);
}

.notebook-toc__title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--wr-text);
}

.notebook-toc__toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--wr-text-secondary);
  cursor: pointer;

  &:hover {
    color: var(--wr-rail-active-color);
    background: var(--wr-stat-blue-bg);
  }
}

.notebook-toc-expand {
  position: absolute;
  top: 12px;
  right: 0;
  z-index: 2;
  transform: none;
  padding: 8px 6px;
  border: 1px solid var(--wr-border);
  border-right: none;
  border-radius: 8px 0 0 8px;
  background: var(--wr-card);
  box-shadow: var(--wr-shadow);
  color: var(--wr-rail-active-color);
  font-size: 12px;
  font-weight: 600;
  writing-mode: vertical-rl;
  letter-spacing: 2px;
  cursor: pointer;

  &:hover {
    background: var(--wr-stat-blue-bg);
  }
}

/* 紧凑档抽屉头部：标题 + 关闭按钮（仅抽屉模式显示） */
.notebook-sidebar__drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-shrink: 0;
  padding: 12px 12px 0;
}

.notebook-sidebar__drawer-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--wr-text);
}

.notebook-sidebar__drawer-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--wr-text-secondary);
  cursor: pointer;

  &:hover {
    color: var(--wr-rail-active-color);
    background: var(--wr-stat-blue-bg);
  }
}

.notebook-sidebar__toolbar {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 12px 10px;
  box-sizing: border-box;
}

.notebook-sidebar__search-wrap {
  width: 100%;
}

.notebook-sidebar__search {
  width: 100%;

  :deep(.el-input__wrapper) {
    min-height: 40px;
    padding: 8px 12px;
    border-radius: 12px;
    box-shadow: 0 0 0 1px var(--wr-border) inset;
    background: var(--wr-card);
  }

  :deep(.el-input__inner) {
    font-size: 14px;
    line-height: 1.4;
  }

  :deep(.el-input__prefix) {
    font-size: 16px;
  }

  :deep(.el-input) {
    width: 100%;
  }
}

.notebook-sidebar__search-kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 22px;
  padding: 0 7px;
  border: 1px solid var(--wr-border);
  border-radius: 6px;
  background: var(--wr-bg);
  color: var(--wr-text-secondary);
  font-family: inherit;
  font-size: 12px;
  line-height: 1;
  pointer-events: none;
}

.notebook-sidebar__create {
  display: flex;
  align-items: stretch;
  width: 100%;
  border: 1px solid var(--wr-rail-active-color);
  border-radius: 10px;
  overflow: hidden;
  background: var(--wr-card);
}

.notebook-sidebar__create-main {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 36px;
  padding: 0 12px;
  border: none;
  background: transparent;
  color: var(--wr-rail-active-color);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;

  &:hover {
    background: var(--wr-stat-blue-bg);
  }
}

.notebook-sidebar__create-divider {
  width: 1px;
  flex-shrink: 0;
  background: var(--wr-rail-active-color);
  opacity: 0.35;
}

.notebook-sidebar__create-more {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  flex-shrink: 0;
  border: none;
  background: transparent;
  color: var(--wr-rail-active-color);
  cursor: pointer;

  &:hover {
    background: var(--wr-stat-blue-bg);
  }
}

.notebook-sidebar__body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.notebook-sidebar__tree-wrap {
  flex: 1;
  overflow: auto;
  overscroll-behavior: contain;
  padding: 0 8px 12px;

  :deep(.el-tree) {
    background: transparent;
  }

  :deep(.el-tree-node__content) {
    display: flex;
    align-items: center;
    height: auto;
    min-height: 40px;
    padding: 2px 0;
    border-radius: 10px;
    cursor: pointer;

    .notebook-tree-node {
      flex: 1;
      min-width: 0;
    }
  }

  :deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
    background: transparent;
  }
}

.notebook-tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 6px 8px;
  border-radius: 10px;
  font-size: 13px;
  box-sizing: border-box;
  cursor: pointer;
  user-select: none;

  &.is-active {
    color: var(--wr-rail-active-color);
    background: var(--wr-rail-active-bg);
  }
}

:deep(.el-tree-node.is-current > .el-tree-node__content .notebook-tree-node) {
  color: var(--wr-rail-active-color);
  background: var(--wr-rail-active-bg);
}

.notebook-tree-node__icon {
  flex-shrink: 0;
  font-size: 16px;
  line-height: 1;

  &.is-folder {
    color: var(--wr-index-text);
    font-size: 17px;
  }

  &.is-note {
    color: var(--wr-stat-gray);
  }
}

.notebook-tree-node.is-active .notebook-tree-node__icon.is-note,
:deep(.el-tree-node.is-current > .el-tree-node__content .notebook-tree-node__icon.is-note) {
  color: var(--wr-rail-active-color);
}

.notebook-tree-node__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notebook-tree-node__pin {
  margin-left: auto;
  font-size: 12px;
  color: var(--wr-rail-active-color);
}

.notebook-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 16px 20px;
  overflow: hidden;
  background: var(--wr-card);

  &.is-editing {
    padding: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    min-height: 0;
  }

  &:not(:has(.notebook-editor)) {
    overflow: auto;
  }
}

.notebook-editor__header {
  flex-shrink: 0;
  margin-bottom: 16px;
}

.notebook-editor__title-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}

/* 左上角存在悬浮按钮（紧凑档抽屉开关 / 矮视口标签页开关）时，标题行右移避免被遮挡 */
.notebook-editor.editor-title-shifted .notebook-editor__title-row {
  padding-left: 32px;
}

.notebook-editor__title-col {
  flex: 1;
  min-width: 0;
}

.notebook-editor__actions-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  flex-shrink: 0;
  padding-top: 4px;
}

.notebook-editor__pin-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 平板档「配置」按钮：与标题同行，收纳置顶/收藏/MD模式/全屏/标签 */
.notebook-editor__config-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  margin-top: 6px;
  padding: 0;
  border: 1px solid var(--wr-border);
  border-radius: 8px;
  background: var(--wr-card);
  color: var(--wr-text-secondary);
  cursor: pointer;
  box-shadow: var(--wr-shadow);

  &:hover {
    color: var(--wr-rail-active-color);
    background: var(--wr-stat-blue-bg);
  }

  /* 目录折叠时右侧有竖条「目录」展开钮（right:0），左移避开重叠 */
  &.is-toc-collapsed {
    margin-right: 16px;
  }
}

.notebook-editor__title {
  width: 100%;
  min-width: 0;
  margin-bottom: 8px;

  :deep(.el-input__wrapper) {
    box-shadow: none !important;
    border: none;
    border-radius: 0;
    padding: 0;
    background: transparent;
  }

  :deep(.el-input__inner) {
    font-size: 28px;
    font-weight: 700;
    line-height: 1.3;
    color: var(--wr-text);
    height: auto;
  }
}

.notebook-editor__meta-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  font-size: 12px;
  line-height: 1.5;
}

.notebook-editor__meta-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--wr-text-secondary);
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;

  &.is-active {
    color: var(--wr-rail-active-color);
  }

  &:hover {
    color: var(--wr-rail-active-color);
  }
}

.notebook-editor__meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--wr-text-secondary);

  &.is-ok {
    color: var(--wr-text-secondary);
  }

  &.is-error {
    color: #dc2626;
  }

  &.is-warn {
    color: #d97706;
  }
}

.notebook-editor__meta-check {
  font-size: 14px;
  color: var(--wr-up-badge-bg);
}

.notebook-editor__meta-dot {
  margin: 0 8px;
  color: var(--wr-muted);
  user-select: none;
}

.notebook-editor__tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 4px;
}

.notebook-tag-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border: none;
  border-radius: 999px;
  font-size: 13px;
  line-height: 1.2;
  cursor: pointer;
  transition: opacity 0.15s ease, transform 0.15s ease;

  &:hover {
    opacity: 0.85;
  }

  &.is-add {
    border: 1px solid var(--wr-border);
    background: var(--wr-card);
    color: var(--wr-stat-purple, #7c3aed);
    box-shadow: 0 1px 2px rgb(0 0 0 / 4%);
  }
}

.notebook-tag-picker {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.notebook-tag-picker__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 6px 8px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;

  &:hover,
  &.is-selected {
    background: var(--wr-stat-blue-bg);
  }
}

.notebook-tag-picker__pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.notebook-tag-picker__manage {
  margin-top: 6px;
  padding: 8px 10px;
  border: none;
  border-top: 1px solid var(--wr-border);
  border-radius: 0;
  background: transparent;
  color: var(--wr-rail-active-color);
  font-size: 13px;
  text-align: left;
  cursor: pointer;

  &:hover {
    background: var(--wr-stat-blue-bg);
  }
}

.notebook-main__empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.notebook-editor__content {
  flex: 1 1 auto;
  min-height: 300px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  z-index: 2;
}

.notebook-editor__content-editor {
  flex: 1 1 auto;
  min-height: 300px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &.is-content-loading {
    pointer-events: none;
  }
}

/* MD 模式切换按钮组 */
.notebook-editor__md-mode-group {
  display: inline-flex;
  gap: 2px;
  margin-left: 4px;

  .notebook-editor__meta-action {
    padding: 4px 6px;
    font-size: 11px;
    gap: 2px;

    &.is-active {
      color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
      border-color: var(--el-color-primary);
    }
  }
}

.notebook-editor__md-split-icon {
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -2px;
}

/* MD 预览区域 — VS Code 风格 */
.notebook-editor__md-preview {
  flex: 1 1 auto;
  min-height: 300px;
  padding: 24px 32px;
  overflow-y: auto;
  line-height: 1.7;
  font-size: 15px;
  color: var(--el-text-color-primary);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe WPC', 'Segoe UI',
    system-ui, 'Ubuntu', 'Droid Sans', sans-serif;

  &.is-split {
    padding: 16px 20px;
    font-size: 14px;
  }

  /* 标题 — 带下边线（VS Code 风格） */
  :deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
    margin-top: 1.4em;
    margin-bottom: 0.7em;
    font-weight: 600;
    line-height: 1.3;
  }
  :deep(h1) {
    font-size: 1.7em;
    border-bottom: 1px solid var(--el-border-color-light);
    padding-bottom: 0.3em;
  }
  :deep(h2) {
    font-size: 1.4em;
    border-bottom: 1px solid var(--el-border-color-light);
    padding-bottom: 0.3em;
  }
  :deep(h3) { font-size: 1.2em; }
  :deep(h4) { font-size: 1.05em; }
  :deep(p) {
    margin: 0 0 0.9em;
    line-height: 1.7;
  }
  /* 列表 */
  :deep(ul), :deep(ol) {
    padding-left: 1.7em;
    margin-bottom: 0.9em;
  }
  :deep(li) { margin-bottom: 0.25em; }
  :deep(li > ul), :deep(li > ol) { margin-bottom: 0; }
  /* 代码块 */
  :deep(pre) {
    background: var(--el-fill-color-light, #f5f5f5);
    border-radius: 6px;
    padding: 14px 16px;
    overflow-x: auto;
    margin-bottom: 0.9em;
    font-size: 0.92em;
    line-height: 1.55;
  }
  :deep(code) {
    font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Consolas',
      'Liberation Mono', Menlo, monospace;
    font-size: 0.9em;
    background: var(--el-fill-color-light, #f5f5f5);
    padding: 2px 5px;
    border-radius: 3px;
  }
  :deep(pre code) {
    background: none;
    padding: 0;
    border-radius: 0;
  }
  /* 引用 */
  :deep(blockquote) {
    margin: 0 0 0.9em;
    padding: 4px 0 4px 14px;
    border-left: 4px solid var(--el-color-primary-light-5, #a0c4ff);
    color: var(--el-text-color-secondary, #616161);
    background: var(--el-fill-color-lighter, #fafafa);
    border-radius: 0 4px 4px 0;
  }
  :deep(blockquote p:last-child) { margin-bottom: 0; }
  /* 表格 */
  :deep(table) {
    border-collapse: collapse;
    width: 100%;
    margin-bottom: 0.9em;
    font-size: 0.95em;
  }
  :deep(th), :deep(td) {
    border: 1px solid var(--el-border-color, #ddd);
    padding: 8px 12px;
    text-align: left;
  }
  :deep(th) {
    background: var(--el-fill-color-light, #f0f0f0);
    font-weight: 600;
  }
  :deep(tr:nth-child(even) td) {
    background: var(--el-fill-color-lighter, #fafafa);
  }
  /* 链接 */
  :deep(a) {
    color: var(--el-color-primary, #409eff);
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }
  /* 图片 */
  :deep(img) { max-width: 100%; }
  /* 分割线 */
  :deep(hr) {
    border: none;
    border-top: 1px solid var(--el-border-color-light, #eee);
    margin: 1.5em 0;
  }
  /* 任务列表 */
  :deep(input[type='checkbox']) {
    margin-right: 6px;
    transform: translateY(1px);
  }
}

/* MD 编辑文本域 */
.notebook-editor__md-textarea {
  flex: 1 1 auto;
  min-height: 300px;
  width: 100%;
  padding: 16px 20px;
  border: none;
  outline: none;
  resize: none;
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Consolas',
    'Liberation Mono', Menlo, monospace;
  font-size: 14px;
  line-height: 1.7;
  color: var(--el-text-color-primary);
  background: transparent;

  &::placeholder {
    color: var(--el-text-color-placeholder);
  }

  &.is-split {
    min-height: 0;
    padding: 12px 14px;
    font-size: 13px;
  }
}

/* MD 分屏模式左右布局 */
.notebook-editor__md-split {
  flex: 1 1 auto;
  min-height: 300px;
  display: flex;
  flex-direction: row;
  overflow: hidden;
  border-top: 1px solid var(--el-border-color-light);

  .notebook-editor__md-textarea {
    border-right: 1px solid var(--el-border-color-light);
    min-height: 0;
    resize: horizontal;
  }

  .notebook-editor__md-preview {
    min-height: 0;
  }

  & > * {
    flex: 1 1 50%;
    width: 50%;
  }
}

.notebook-editor__content-loading {
  flex: 1;
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--el-text-color-secondary);
  font-size: 14px;

  &.notebook-editor__content-loading--overlay {
    position: absolute;
    inset: 0;
    z-index: 3;
    min-height: 0;
    background: rgb(255 255 255 / 78%);
  }
}

.notebook-editor__loading-icon {
  font-size: 28px;
  color: var(--el-color-primary);
}

.notebook-editor__save-btn {
  position: absolute;
  bottom: 20px;
  right: 20px;
  z-index: 10;
  width: 44px;
  height: 44px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: #1a7a3a;
  color: #fff;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(26, 122, 58, 0.4);
  transition: background 0.2s, transform 0.15s, box-shadow 0.2s;

  &:hover {
    background: #21904a;
    box-shadow: 0 4px 14px rgba(26, 122, 58, 0.5);
    transform: scale(1.05);
  }

  &:active {
    transform: scale(0.95);
  }

  &:disabled {
    background: #a3c9b0;
    cursor: not-allowed;
    box-shadow: none;
  }
}

.tag-manage__new {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.notebook-folder-location {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.notebook-move-hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.notebook-move-folder-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.notebook-move-tree {
  max-height: 360px;
  overflow: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 4px 0;

  :deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
    background: var(--el-color-primary-light-9);
  }

  :deep(.el-tree-node.is-current > .el-tree-node__content) {
    color: var(--el-color-primary);
  }
}

:deep(.el-tree-node__content) {
  height: 32px;
}

@media (max-width: 1200px) {
  .notebook-stat-card {
    min-height: 64px;
  }

  .notebook-stat-card__icon {
    width: 36px;
    height: 36px;
    border-radius: 9px;
    font-size: 18px;
  }

  .notebook-editor__title-row {
    flex-wrap: wrap;
    gap: 8px;
  }

  .notebook-editor__actions-col {
    flex-direction: row;
    flex-wrap: wrap;
    width: 100%;
  }
}

@media (max-width: 1300px) {
  /* 与 deviceShell.ts 的 TABLET_COMPACT_MAX_WIDTH=1300 保持一致：左侧树收为原位抽屉 */
  .notebook-sidebar {
    position: absolute;
    inset: 0 auto 0 0;
    z-index: 40;
    width: min(300px, 82vw);
    box-shadow: 0 8px 30px rgb(0 0 0 / 18%);
    transition: transform 0.25s ease;
    transform: translateX(0);

    &.is-collapsed {
      transform: translateX(-105%);
    }
  }

  .notebook-sidebar-overlay {
    position: absolute;
    inset: 0;
    z-index: 39;
    background: rgb(0 0 0 / 32%);
  }

  .notebook-sidebar-toggle {
    position: absolute;
    top: 8px;
    left: 8px;
    z-index: 30;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    padding: 0;
    border: 1px solid var(--wr-border);
    border-radius: 10px;
    background: var(--wr-card);
    box-shadow: var(--wr-shadow);
    color: var(--wr-rail-active-color);
    cursor: pointer;

    &:hover {
      background: var(--wr-stat-blue-bg);
    }
  }
}

/* 紧凑档 + 矮视口同时生效：矮视口下 tabs 头/统计隐藏、布局顶格，
   抽屉开关按钮与标签页收起按钮（notebook-tabs-switch）都会落在左上角而重叠。
   将抽屉开关按钮下移避开标签页收起按钮。 */
@media (max-height: 900px) and (max-width: 1300px) {
  .notebook-sidebar-toggle {
    top: 48px;
  }
}

/* 矮视口（平板横屏 / 笔记本小屏）：压缩固定条与留白，放大内容区可读高度 */
@media (max-height: 900px) {
  .war-room-panel--notebook {
    padding: 8px 12px;
  }

  .notebook-tab-all {
    gap: 8px;
  }

  .notebook-tabs-wrap :deep(.el-tabs__header) {
    margin-bottom: 8px;
  }

  .notebook-stats {
    gap: 8px;
  }

  .notebook-stat-card {
    height: 56px;
    gap: 10px;
    padding: 8px 12px;
  }

  .notebook-stat-card__icon {
    width: 30px;
    height: 30px;
    border-radius: 8px;
    font-size: 15px;
  }

  .notebook-stat-card__label {
    font-size: 12px;
  }

  .notebook-stat-card__value {
    font-size: 18px;
  }

  .notebook-sidebar {
    width: 220px;
  }

  .notebook-sidebar__toolbar {
    gap: 6px;
    padding: 8px 8px 6px;
  }

  .notebook-sidebar__drawer-head {
    padding: 8px 8px 0;
  }

  .notebook-sidebar__search :deep(.el-input__wrapper) {
    min-height: 34px;
  }

  .notebook-toc {
    width: 200px;
  }

  .notebook-editor {
    padding: 8px 12px;
  }

  .notebook-main {
    padding: 8px 12px;
  }

  .notebook-editor__header {
    margin-bottom: 8px;
  }

  .notebook-editor__title-row {
    gap: 12px;
    margin-bottom: 8px;
  }

  .notebook-editor__title :deep(.el-input__inner) {
    font-size: 20px;
  }

  .notebook-editor__meta-line {
    font-size: 11px;
  }

  .notebook-editor__tags {
    margin-bottom: 0;
  }

  .notebook-editor__content,
  .notebook-editor__content-editor,
  .notebook-editor__md-preview,
  .notebook-editor__md-textarea,
  .notebook-editor__md-split,
  .notebook-editor__content-loading {
    min-height: 200px;
  }
}

/* 矮视口（高度 ≤ 900px，如平板横屏）：统计、标签页收起，最大化内容区 */
@media (max-height: 900px) {
  .notebook-stats {
    display: none;
  }

  .notebook-tabs :deep(.el-tabs__header) {
    display: none;
  }

  .notebook-tab-all {
    gap: 0;
  }
}
</style>

<style lang="scss">
/* 矮视口（高度 ≤ 900px）：收起“笔记本”页面标题（仅笔记本页），放大内容区 */
@media (max-height: 900px) {
  .war-room-page:has(.notebook-page) .war-room-page__header {
    display: none;
  }
}

/* 标签页收起按钮的浮层菜单（el-popover 渲染在 body，需全局样式） */
.notebook-tabs-switch-popper {
  .notebook-tabs-switch__menu {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .notebook-tabs-switch__item {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    padding: 7px 10px;
    border: none;
    border-radius: 6px;
    background: transparent;
    color: var(--wr-text-secondary, #666666);
    font-size: 13px;
    text-align: left;
    cursor: pointer;

    &:hover,
    &.is-active {
      color: var(--wr-rail-active-color, #2563eb);
      background: var(--wr-stat-blue-bg, #eff6ff);
    }
  }

  .notebook-tabs-switch__check {
    font-size: 14px;
  }
}

/* 平板档「配置」按钮浮层菜单（el-popover 渲染在 body，需全局样式） */
.notebook-editor-config-popper {
  .notebook-editor-config {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .notebook-editor-config__item {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    padding: 7px 10px;
    border: none;
    border-radius: 6px;
    background: transparent;
    color: var(--wr-text-secondary, #666666);
    font-size: 13px;
    text-align: left;
    cursor: pointer;

    &:hover,
    &.is-active {
      color: var(--wr-rail-active-color, #2563eb);
      background: var(--wr-stat-blue-bg, #eff6ff);
    }
  }

  .notebook-editor-config__check {
    margin-left: auto;
    font-size: 14px;
  }

  .notebook-editor-config__label {
    padding: 4px 10px 2px;
    font-size: 11px;
    color: var(--wr-muted, #999999);
  }

  .notebook-editor-config__sep {
    height: 1px;
    margin: 4px 0;
    background: var(--wr-border, #e8ecef);
  }

  /* MD 模式切换：纵向堆叠整行展示，避免平板窄弹层内同一行溢出 */
  .notebook-editor-config__md {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 2px;
  }

  .notebook-editor-config__md-item {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    padding: 7px 10px;
    border: none;
    border-radius: 6px;
    background: transparent;
    color: var(--wr-text-secondary, #666666);
    font-size: 13px;
    text-align: left;
    cursor: pointer;

    &:hover,
    &.is-active {
      color: var(--el-color-primary);
      background: var(--wr-stat-blue-bg, #eff6ff);
    }
  }

  .notebook-editor-config__manage {
    margin-top: 2px;
    padding: 8px 10px;
    border: none;
    border-top: 1px solid var(--wr-border, #e8ecef);
    border-radius: 0;
    background: transparent;
    color: var(--wr-rail-active-color, #2563eb);
    font-size: 13px;
    text-align: left;
    cursor: pointer;

    &:hover {
      background: var(--wr-stat-blue-bg, #eff6ff);
    }
  }

  /* 复用标签 pill 样式（scoped 样式不达 popper，此处兜底） */
  .notebook-tag-picker__pill {
    display: inline-flex;
    align-items: center;
    padding: 4px 10px;
    border-radius: 999px;
    font-size: 12px;
  }

  .notebook-editor__md-split-icon {
    font-weight: 700;
    letter-spacing: -1px;
  }
}
</style>
