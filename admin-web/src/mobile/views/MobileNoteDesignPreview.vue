<template>
  <div class="note-design-preview">
    <div class="preview-header">
      <h1 class="preview-title">笔记功能设计方案</h1>
      <p class="preview-subtitle">含文件夹分类的手绘风格界面</p>
    </div>

    <div class="scheme-tabs">
      <button
        v-for="scheme in schemes"
        :key="scheme.id"
        class="scheme-tab"
        :class="{ active: activeScheme === scheme.id }"
        @click="activeScheme = scheme.id"
      >
        {{ scheme.name }}
      </button>
    </div>

    <div class="preview-container">
      <div class="mobile-mockup">
        <div class="mockup-header">
          <div class="mockup-notch"></div>
        </div>
        
        <template v-if="activeScheme === 'scheme-a'">
          <div class="scheme-a-page">
            <div class="page-header">
              <div class="page-title-wrap">
                <img :src="assets.starYellow" class="title-star" alt="" />
                <h2 class="page-title">我的笔记</h2>
                <img :src="assets.starBlue" class="title-star" alt="" />
              </div>
            </div>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索笔记..." />
              </div>
            </SchemeADoodleFrame>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starYellow" class="section-icon" alt="" />
                <h3 class="section-title">置顶笔记</h3>
              </div>

              <div class="notes-grid">
                <SchemeADoodleFrame color="#fbbf24" class="note-card note-card--pinned">
                  <img :src="assets.paperclip" class="paperclip" alt="" />
                  <div class="note-card__body">
                    <div class="note-card__title">📌 项目规划方案</div>
                    <div class="note-card__meta">📁 /工作笔记</div>
                  </div>
                  <img :src="assets.squiggleRed" class="squiggle" alt="" />
                </SchemeADoodleFrame>
              </div>
            </div>

            <div class="section" v-for="folder in folders" :key="folder.name">
              <div class="section-head">
                <img :src="assets.starBlue" class="section-icon" alt="" />
                <h3 class="section-title">📁 {{ folder.name }}</h3>
              </div>

              <SchemeADoodleFrame color="#f97316" class="folder-card" @click="toggleFolder(folder.name)">
                <div class="folder-card__body">
                  <div class="folder-card__icon">📂</div>
                  <div class="folder-card__info">
                    <div class="folder-card__name">{{ folder.name }}</div>
                    <div class="folder-card__meta">
                      {{ folder.notes.length }} 笔记 · {{ folder.subfolders }} 子文件夹
                    </div>
                  </div>
                  <div class="folder-card__arrow">{{ expandedFolders.includes(folder.name) ? '▼' : '▶' }}</div>
                </div>
              </SchemeADoodleFrame>

              <div v-if="expandedFolders.includes(folder.name)" class="notes-grid notes-grid--nested">
                <SchemeADoodleFrame color="#2563eb" class="note-card" v-for="note in folder.notes" :key="note.title" @click="handleCardClick">
                  <div class="note-card__body">
                    <div class="note-card__title">📄 {{ note.title }}</div>
                    <div class="note-card__preview">{{ note.preview }}</div>
                  </div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <SchemeADoodleFrame tag="button" shape="pill" color="#2563eb" class="create-btn" @click="handleCreateClick">
              <span class="create-btn__text">➕ 新建笔记</span>
            </SchemeADoodleFrame>
          </div>
        </template>

        <template v-else-if="activeScheme === 'scheme-b'">
          <div class="scheme-b-page">
            <div class="page-header">
              <h2 class="page-title">📝 我的笔记</h2>
            </div>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索笔记..." />
              </div>
            </SchemeADoodleFrame>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starYellow" class="section-icon" alt="" />
                <h3 class="section-title">置顶笔记</h3>
              </div>

              <div class="note-list">
                <div class="note-list-item note-list-item--pinned">
                  <div class="note-list-item__icon">📌</div>
                  <div class="note-list-item__body">
                    <div class="note-list-item__title">项目规划方案</div>
                    <div class="note-list-item__meta">📁 /工作笔记</div>
                  </div>
                </div>
              </div>
            </div>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlue" class="section-icon" alt="" />
                <h3 class="section-title">📁 工作笔记</h3>
              </div>

              <div class="note-tree">
                <div class="note-tree-item note-tree-item--folder" @click="toggleTreeFolder('work')">
                  <span class="note-tree-item__indent"></span>
                  <span class="note-tree-item__arrow">{{ expandedTreeFolders.includes('work') ? '▼' : '▶' }}</span>
                  <span class="note-tree-item__icon">📂</span>
                  <span class="note-tree-item__name">工作笔记</span>
                </div>

                <div v-if="expandedTreeFolders.includes('work')">
                  <div class="note-tree-item" v-for="n in treeWorkNotes" :key="n" @click="handleCardClick">
                    <span class="note-tree-item__indent note-tree-item__indent--1"></span>
                    <span class="note-tree-item__icon">📄</span>
                    <span class="note-tree-item__name">{{ n }}</span>
                  </div>

                  <div class="note-tree-item note-tree-item--folder" @click="toggleTreeFolder('design')">
                    <span class="note-tree-item__indent note-tree-item__indent--1"></span>
                    <span class="note-tree-item__arrow">{{ expandedTreeFolders.includes('design') ? '▼' : '▶' }}</span>
                    <span class="note-tree-item__icon">📂</span>
                    <span class="note-tree-item__name">设计文档</span>
                  </div>

                  <div v-if="expandedTreeFolders.includes('design')">
                    <div class="note-tree-item" v-for="n in treeDesignNotes" :key="n" @click="handleCardClick">
                      <span class="note-tree-item__indent note-tree-item__indent--2"></span>
                      <span class="note-tree-item__icon">📄</span>
                      <span class="note-tree-item__name">{{ n }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlue" class="section-icon" alt="" />
                <h3 class="section-title">📁 个人笔记</h3>
              </div>

              <div class="note-tree">
                <div class="note-tree-item note-tree-item--folder" @click="toggleTreeFolder('personal')">
                  <span class="note-tree-item__indent"></span>
                  <span class="note-tree-item__arrow">{{ expandedTreeFolders.includes('personal') ? '▼' : '▶' }}</span>
                  <span class="note-tree-item__icon">📂</span>
                  <span class="note-tree-item__name">个人笔记</span>
                </div>

                <div v-if="expandedTreeFolders.includes('personal')">
                  <div class="note-tree-item" v-for="n in treePersonalNotes" :key="n" @click="handleCardClick">
                    <span class="note-tree-item__indent note-tree-item__indent--1"></span>
                    <span class="note-tree-item__icon">📄</span>
                    <span class="note-tree-item__name">{{ n }}</span>
                  </div>
                </div>
              </div>
            </div>

            <img :src="assets.squiggleBlue" class="wave-divider" alt="" />

            <SchemeADoodleFrame tag="button" color="#2563eb" class="create-btn-list" @click="handleCreateClick">
              <span>➕ 新建笔记</span>
            </SchemeADoodleFrame>
          </div>
        </template>

        <template v-else-if="activeScheme === 'scheme-c'">
          <div class="scheme-c-page">
            <div class="page-header">
              <h2 class="page-title">📝 我的笔记</h2>
            </div>

            <div class="folder-nav">
              <div
                v-for="folder in navFolders"
                :key="folder.name"
                class="folder-nav__item"
                :class="{ active: currentNavFolder === folder.name }"
                @click="currentNavFolder = folder.name"
              >
                <span class="folder-nav__icon">{{ folder.icon }}</span>
                <span class="folder-nav__name">{{ folder.name }}</span>
                <span v-if="currentNavFolder === folder.name" class="folder-nav__indicator"></span>
              </div>
            </div>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索笔记..." />
              </div>
            </SchemeADoodleFrame>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlue" class="section-icon" alt="" />
                <h3 class="section-title">📂 {{ currentNavFolder }}</h3>
              </div>

              <div class="notes-grid">
                <SchemeADoodleFrame color="#2563eb" class="note-card" v-for="note in currentFolderNotes" :key="note.title" @click="handleCardClick">
                  <div class="note-card__body">
                    <div class="note-card__title">📄 {{ note.title }}</div>
                    <div class="note-card__preview">{{ note.preview }}</div>
                  </div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <div class="fab-container">
              <SchemeADoodleFrame tag="button" color="#2563eb" class="fab-btn" @click="handleCreateClick">
                <span class="fab-icon">➕</span>
              </SchemeADoodleFrame>
            </div>
          </div>
        </template>

        <div class="mockup-footer">
          <div class="mockup-home-indicator"></div>
        </div>
      </div>

      <div class="scheme-info">
        <h3>{{ currentScheme.description }}</h3>
        <ul>
          <li v-for="(feature, index) in currentScheme.features" :key="index">
            {{ feature }}
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import SchemeADoodleFrame from '@/mobile/home/themes/scheme-a/SchemeADoodleFrame.vue'
import { schemeAAssets } from '@/mobile/home/themes/scheme-a/assets'

const assets = schemeAAssets

const schemes = [
  {
    id: 'scheme-a',
    name: '方案一：卡片式',
    description: '卡片式布局 - 推荐方案',
    features: [
      '每个文件夹作为独立区块，包含文件夹卡片和下属笔记',
      '文件夹卡片使用橙色手绘边框，笔记卡片使用蓝色边框',
      '置顶笔记带有回形针和黄色装饰',
      '点击文件夹卡片可展开/收起下属内容',
      '搜索框使用胶囊形手绘边框'
    ]
  },
  {
    id: 'scheme-b',
    name: '方案二：列表式',
    description: '树状列表布局',
    features: [
      '树状列表布局，支持文件夹展开/收起',
      '缩进显示层级关系（支持多级子文件夹）',
      '手绘风格的展开箭头（▶/▼）',
      '文件夹和笔记使用不同图标区分',
      '手绘风格波浪线分隔各分类区域'
    ]
  },
  {
    id: 'scheme-c',
    name: '方案三：导航+列表',
    description: '文件夹导航 + 内容列表',
    features: [
      '顶部文件夹导航栏，横向滚动',
      '当前选中文件夹高亮显示',
      '内容区域只显示当前文件夹下的笔记',
      '支持点击文件夹快速切换',
      '新建按钮使用浮动按钮设计'
    ]
  }
]

const activeScheme = ref('scheme-a')

const currentScheme = computed(() => schemes.find(s => s.id === activeScheme.value)!)

const folders = [
  {
    name: '工作笔记',
    notes: [
      { title: '会议记录', preview: '本周项目进度讨论要点...' },
      { title: '需求文档', preview: '新功能需求分析文档...' }
    ],
    subfolders: 1
  },
  {
    name: '个人笔记',
    notes: [
      { title: '学习笔记', preview: 'Vue.js 组合式 API 学习笔记...' },
      { title: '购物清单', preview: '日用品采购清单...' }
    ],
    subfolders: 0
  }
]

const expandedFolders = ref(['工作笔记'])

function toggleFolder(folderName: string) {
  const index = expandedFolders.value.indexOf(folderName)
  if (index > -1) {
    expandedFolders.value.splice(index, 1)
  } else {
    expandedFolders.value.push(folderName)
  }
}

const treeWorkNotes = ['会议记录', '需求文档']
const treeDesignNotes = ['UI设计稿', '交互原型']
const treePersonalNotes = ['学习笔记', '购物清单']
const expandedTreeFolders = ref(['work'])

function toggleTreeFolder(folderKey: string) {
  const index = expandedTreeFolders.value.indexOf(folderKey)
  if (index > -1) {
    expandedTreeFolders.value.splice(index, 1)
  } else {
    expandedTreeFolders.value.push(folderKey)
  }
}

const navFolders = [
  { name: '全部笔记', icon: '📚' },
  { name: '工作笔记', icon: '💼' },
  { name: '个人笔记', icon: '👤' },
  { name: '设计文档', icon: '🎨' }
]

const currentNavFolder = ref('工作笔记')

const folderNotesMap: Record<string, Array<{ title: string; preview: string }>> = {
  '全部笔记': [
    { title: '项目规划方案', preview: '重要项目规划文档...' },
    { title: '会议记录', preview: '本周项目进度讨论要点...' },
    { title: '需求文档', preview: '新功能需求分析文档...' },
    { title: '学习笔记', preview: 'Vue.js 组合式 API 学习笔记...' },
    { title: '购物清单', preview: '日用品采购清单...' },
    { title: 'UI设计稿', preview: '界面设计稿说明...' }
  ],
  '工作笔记': [
    { title: '会议记录', preview: '本周项目进度讨论要点...' },
    { title: '需求文档', preview: '新功能需求分析文档...' },
    { title: '项目周报', preview: '本周工作内容总结...' }
  ],
  '个人笔记': [
    { title: '学习笔记', preview: 'Vue.js 组合式 API 学习笔记...' },
    { title: '购物清单', preview: '日用品采购清单...' },
    { title: '旅行计划', preview: '下周末旅行安排...' }
  ],
  '设计文档': [
    { title: 'UI设计稿', preview: '界面设计稿说明...' },
    { title: '交互原型', preview: '产品交互原型文档...' },
    { title: '色彩规范', preview: '设计系统色彩规范...' }
  ]
}

const currentFolderNotes = computed(() => folderNotesMap[currentNavFolder.value] || [])

function handleCardClick() {
  console.log('Card clicked')
}

function handleCreateClick() {
  console.log('Create note clicked')
}
</script>

<style scoped lang="scss">
.note-design-preview {
  min-height: 100vh;
  background: linear-gradient(135deg, #faf8f5 0%, #f0f9ff 100%);
  padding: 24px;
}

.preview-header {
  text-align: center;
  margin-bottom: 24px;
}

.preview-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 32px;
  color: #1e293b;
  margin: 0 0 8px;
}

.preview-subtitle {
  font-size: 16px;
  color: #64748b;
  margin: 0;
}

.scheme-tabs {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
}

.scheme-tab {
  padding: 10px 24px;
  border-radius: 999px;
  border: 2px solid #2563eb;
  background: white;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #2563eb;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    transform: scale(1.02);
  }

  &.active {
    background: #2563eb;
    color: white;
  }
}

.preview-container {
  display: flex;
  justify-content: center;
  gap: 32px;
  flex-wrap: wrap;
}

.mobile-mockup {
  width: 375px;
  height: 812px;
  border-radius: 40px;
  background: #1e293b;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  position: relative;
}

.mockup-header {
  height: 44px;
  background: #faf8f5;
  display: flex;
  justify-content: center;
  align-items: center;
}

.mockup-notch {
  width: 150px;
  height: 30px;
  border-radius: 0 0 20px 20px;
  background: #1e293b;
}

.mockup-footer {
  height: 34px;
  background: #faf8f5;
  display: flex;
  justify-content: center;
  align-items: center;
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
}

.mockup-home-indicator {
  width: 130px;
  height: 5px;
  border-radius: 999px;
  background: #94a3b8;
}

.scheme-a-page,
.scheme-b-page,
.scheme-c-page {
  padding: 16px;
  background: #faf8f5;
  height: calc(100% - 78px);
  overflow-y: auto;
}

.page-header {
  margin-bottom: 16px;
}

.page-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 24px;
  color: #1e293b;
  margin: 0;
}

.page-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-star {
  width: 24px;
  height: 24px;
}

.search-box {
  margin-bottom: 16px;
}

.search-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
}

.search-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.search-inner input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
}

.search-inner input::placeholder {
  color: #94a3b8;
}

.section {
  margin-bottom: 20px;
}

.section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.section-icon {
  width: 20px;
  height: 20px;
}

.section-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 18px;
  color: #1e293b;
  margin: 0;
}

.notes-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;

  &--nested {
    margin-top: 8px;
    padding-left: 16px;
  }
}

.folder-card {
  background: #fff7ed;
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }
}

.folder-card__body {
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.folder-card__icon {
  font-size: 24px;
}

.folder-card__info {
  flex: 1;
}

.folder-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
  margin: 0 0 4px;
}

.folder-card__meta {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

.folder-card__arrow {
  font-size: 12px;
  color: #f97316;
  font-weight: bold;
}

.note-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  &--pinned {
    background: #fffbeb;
  }
}

.note-card__body {
  padding: 14px;
}

.note-card__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
  margin: 0 0 6px;
}

.note-card__preview {
  font-size: 12px;
  color: #64748b;
  margin: 0 0 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-card__meta {
  font-size: 11px;
  color: #94a3b8;
  margin: 0;
}

.paperclip {
  position: absolute;
  top: -8px;
  right: 16px;
  width: 32px;
  height: 32px;
  z-index: 3;
}

.squiggle {
  position: absolute;
  bottom: -6px;
  left: 0;
  right: 0;
  height: 12px;
  width: 100%;
  z-index: 3;
}

.create-btn {
  margin-top: 8px;
  margin-bottom: 40px;
  padding: 14px;
  cursor: pointer;
  background: #2563eb;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }
}

.create-btn__text {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 18px;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.create-btn-list {
  margin-top: 8px;
  margin-bottom: 40px;
  padding: 12px 14px;
  cursor: pointer;
  background: #2563eb;
  transition: transform 0.2s ease;
  text-align: center;

  &:hover {
    transform: scale(0.98);
  }

  span {
    font-family: 'ZCOOL KuaiLe', sans-serif;
    font-size: 16px;
    color: white;
  }
}

.note-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.note-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: white;
  border-radius: 12px;
  border: 2px solid #2563eb;
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  &--pinned {
    border-color: #fbbf24;
    background: #fffbeb;
  }
}

.note-list-item__icon {
  font-size: 20px;
}

.note-list-item__body {
  flex: 1;
}

.note-list-item__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 15px;
  color: #1e293b;
  margin: 0 0 3px;
}

.note-list-item__meta {
  font-size: 11px;
  color: #94a3b8;
  margin: 0;
}

.wave-divider {
  width: 100%;
  height: 12px;
  margin: 16px 0;
}

.note-tree {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.note-tree-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: #f8fafc;
    transform: scale(0.99);
  }

  &--folder {
    border: 2px solid #f97316;

    &:hover {
      background: #fff7ed;
    }
  }
}

.note-tree-item__indent {
  width: 0;

  &--1 {
    width: 20px;
  }

  &--2 {
    width: 40px;
  }
}

.note-tree-item__arrow {
  font-size: 10px;
  color: #f97316;
  font-weight: bold;
  width: 14px;
}

.note-tree-item__icon {
  font-size: 16px;
}

.note-tree-item__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
}

.folder-nav {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 12px;
  margin-bottom: 16px;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.folder-nav__item {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: white;
  border-radius: 999px;
  border: 2px solid #2563eb;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;

  &:hover {
    transform: scale(0.98);
  }

  &.active {
    background: #2563eb;

    .folder-nav__name {
      color: white;
    }
  }
}

.folder-nav__icon {
  font-size: 16px;
}

.folder-nav__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #2563eb;
}

.folder-nav__indicator {
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
  border-top: 8px solid #2563eb;
}

.fab-container {
  position: fixed;
  bottom: 60px;
  right: 24px;
}

.fab-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2563eb;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4);
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.05);
  }
}

.fab-icon {
  font-size: 28px;
  color: white;
}

.scheme-info {
  max-width: 300px;
}

.scheme-info h3 {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 20px;
  color: #1e293b;
  margin: 0 0 12px;
}

.scheme-info ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.scheme-info li {
  padding: 8px 12px;
  margin-bottom: 8px;
  background: white;
  border-radius: 8px;
  border-left: 4px solid #2563eb;
  font-size: 14px;
  color: #475569;
}
</style>
