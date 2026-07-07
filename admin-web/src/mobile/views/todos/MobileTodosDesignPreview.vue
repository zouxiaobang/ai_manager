<template>
  <div class="todos-design-preview">
    <div class="preview-header">
      <h1 class="preview-title">待办事项界面设计</h1>
      <p class="preview-subtitle">融合方案一 + 方案二的 3 套全新设计</p>
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

        <!-- ========== 方案 A：智能分组时间轴 ========== -->
        <template v-if="activeScheme === 'scheme-a'">
          <div class="scheme-page">
            <!-- 问候 + 统计 -->
            <div class="ha-greeting">
              <div class="ha-greeting__left">
                <h2 class="ha-greeting__title">早上好 ☀️</h2>
                <p class="ha-greeting__sub">今天还有 <strong>{{ todayCount }}</strong> 件事等着你</p>
              </div>
              <div class="ha-greeting__badge">{{ totalCount }} 项</div>
            </div>

            <!-- 环形进度条 + 关键数字 -->
            <div class="ha-stats-card">
              <div class="ha-ring">
                <svg viewBox="0 0 52 52" class="ha-ring__svg">
                  <circle cx="26" cy="26" r="22" fill="none" stroke="#e2e8f0" stroke-width="4.5" />
                  <circle
                    cx="26" cy="26" r="22"
                    fill="none" stroke="#2563eb" stroke-width="4.5"
                    stroke-linecap="round"
                    :stroke-dasharray="138.2"
                    :stroke-dashoffset="138.2 - (138.2 * doneRatio) / 100"
                    transform="rotate(-90 26 26)"
                  />
                </svg>
                <span class="ha-ring__text">{{ doneRatio }}%</span>
              </div>
              <div class="ha-stats">
                <div class="ha-stat">
                  <span class="ha-stat__value ha-stat__value--blue">{{ pendingCount }}</span>
                  <span class="ha-stat__label">待完成</span>
                </div>
                <div class="ha-stat-divider"></div>
                <div class="ha-stat">
                  <span class="ha-stat__value ha-stat__value--orange">{{ todayCount }}</span>
                  <span class="ha-stat__label">今日</span>
                </div>
                <div class="ha-stat-divider"></div>
                <div class="ha-stat">
                  <span class="ha-stat__value ha-stat__value--red">{{ overdueCount }}</span>
                  <span class="ha-stat__label">逾期</span>
                </div>
              </div>
            </div>

            <!-- 筛选标签 -->
            <div class="ha-filter-tabs">
              <button
                v-for="tab in hybridFilters"
                :key="tab.key"
                class="ha-filter-tab"
                :class="{ active: activeFilter === tab.key }"
                @click="activeFilter = tab.key"
              >
                <span class="ha-filter-tab__icon">{{ tab.icon }}</span>
                <span class="ha-filter-tab__label">{{ tab.label }}</span>
              </button>
            </div>

            <!-- 分组时间轴列表 -->
            <div class="ha-group-list">
              <div
                v-for="group in filteredHybridGroups"
                :key="group.key"
                class="ha-group"
                :class="`ha-group--${group.key}`"
              >
                <button class="ha-group__head" @click="toggleGroup(group.key)">
                  <span class="ha-group__icon">{{ group.icon }}</span>
                  <span class="ha-group__title">{{ group.title }}</span>
                  <span class="ha-group__count">{{ group.items.length }}</span>
                  <span class="ha-group__arrow" :class="{ expanded: !collapsedGroups.has(group.key) }">›</span>
                </button>
                <div v-if="!collapsedGroups.has(group.key)" class="ha-group__body">
                  <div
                    v-for="item in group.items"
                    :key="item.id"
                    class="ha-todo-card"
                    :class="{ 'is-done': item.completed }"
                  >
                    <button
                      class="ha-card-check"
                      :class="{ checked: item.completed }"
                      @click="item.completed = !item.completed"
                    >
                      <span v-if="item.completed" class="ha-card-check__inner">✓</span>
                    </button>
                    <div class="ha-card-body">
                      <span class="ha-card-body__text">{{ item.content }}</span>
                      <div class="ha-card-body__meta">
                        <span v-if="item.dueTime" class="ha-card-time" :class="{ overdue: item.overdue }">
                          ⏰ {{ item.dueTime }}
                        </span>
                        <span v-if="item.pinned" class="ha-tag ha-tag--pin">📌 置顶</span>
                        <span v-if="item.repeatType !== 'NONE'" class="ha-tag ha-tag--repeat">🔁 重复</span>
                        <span v-if="item.remindTime" class="ha-tag ha-tag--remind">🔔 提醒</span>
                      </div>
                    </div>
                  </div>
                  <div v-if="!group.items.length" class="ha-group-empty">{{ group.emptyText }}</div>
                </div>
              </div>
            </div>

            <!-- 底部快捷添加 -->
            <div class="ha-bottom-bar">
              <input class="ha-bottom-bar__input" placeholder="添加待办..." />
              <button class="ha-bottom-bar__btn">添加</button>
            </div>
          </div>
        </template>

        <!-- ========== 方案 B：极简看板风 ========== -->
        <template v-else-if="activeScheme === 'scheme-b'">
          <div class="scheme-page">
            <!-- 顶部简洁信息 -->
            <div class="hb-header">
              <h2 class="hb-header__title">待办看板</h2>
              <button class="hb-header__settings">⚙️</button>
            </div>

            <!-- 状态统计条 -->
            <div class="hb-stat-bar">
              <div class="hb-stat-item hb-stat-item--all" @click="activeCategory = 'all'">
                <span class="hb-stat-item__num">{{ totalCount }}</span>
                <span class="hb-stat-item__label">全部</span>
              </div>
              <div class="hb-stat-item hb-stat-item--today" :class="{ active: activeCategory === 'today' }" @click="activeCategory = 'today'">
                <span class="hb-stat-item__num">{{ todayCount }}</span>
                <span class="hb-stat-item__label">今日</span>
              </div>
              <div class="hb-stat-item hb-stat-item--overdue" :class="{ active: activeCategory === 'overdue' }" @click="activeCategory = 'overdue'">
                <span class="hb-stat-item__num">{{ overdueCount }}</span>
                <span class="hb-stat-item__label">逾期</span>
              </div>
              <div class="hb-stat-item hb-stat-item--done" :class="{ active: activeCategory === 'done' }" @click="activeCategory = 'done'">
                <span class="hb-stat-item__num">{{ doneCount }}</span>
                <span class="hb-stat-item__label">已完成</span>
              </div>
            </div>

            <!-- 看板列（今日提醒 / 待处理 / 已完成） -->
            <div class="hb-kanban">
              <div v-for="col in kanbanColumns" :key="col.key" class="hb-column" :class="`hb-column--${col.key}`">
                <div class="hb-column__head">
                  <span class="hb-column__title">{{ col.title }}</span>
                  <span class="hb-column__count">{{ col.items.length }}</span>
                </div>
                <div class="hb-column__body">
                  <div
                    v-for="item in col.items"
                    :key="item.id"
                    class="hb-card"
                    :class="{ 'is-done': item.completed }"
                  >
                    <div class="hb-card__top">
                      <button
                        class="hb-card-check"
                        :class="{ checked: item.completed || col.key === 'done' }"
                        @click="item.completed = !item.completed"
                      >
                        <span v-if="item.completed || col.key === 'done'" class="hb-card-check__inner">✓</span>
                      </button>
                      <span class="hb-card__text">{{ item.content }}</span>
                      <button class="hb-card__more">⋮</button>
                    </div>
                    <div class="hb-card__tags">
                      <span v-if="item.dueTime" class="hb-tag hb-tag--time" :class="{ overdue: item.overdue }">
                        {{ item.overdue ? '⚠️ 已逾期' : item.dueTime }}
                      </span>
                      <span v-if="item.pinned" class="hb-tag hb-tag--pin">📌</span>
                      <span v-if="item.repeatType !== 'NONE'" class="hb-tag hb-tag--repeat">🔁</span>
                    </div>
                  </div>
                  <div v-if="!col.items.length" class="hb-column__empty">{{ col.emptyText }}</div>
                </div>
              </div>
            </div>

            <!-- 浮动新建按钮 -->
            <button class="hb-fab">＋</button>
          </div>
        </template>

        <!-- ========== 方案 C：专注模式风 ========== -->
        <template v-else-if="activeScheme === 'scheme-c'">
          <div class="scheme-page">
            <!-- 今日焦点头部 -->
            <div class="hc-hero">
              <div class="hc-hero__bg"></div>
              <div class="hc-hero__content">
                <div class="hc-hero__top">
                  <h2 class="hc-hero__title">今日焦点</h2>
                  <span class="hc-hero__date">{{ todayMD }}</span>
                </div>
                <div class="hc-hero__progress-wrap">
                  <div class="hc-hero__progress-bar">
                    <div class="hc-hero__progress-fill" :style="{ width: todayDoneRatio + '%' }"></div>
                  </div>
                  <span class="hc-hero__progress-text">{{ todayDoneCount }}/{{ todayTotalCount }} 完成</span>
                </div>
                <p class="hc-hero__quote">{{ motivationalQuotes[todayDoneCount % motivationalQuotes.length] }}</p>
              </div>
            </div>

            <!-- 快捷筛选芯片 -->
            <div class="hc-chips">
              <button
                v-for="chip in chips"
                :key="chip.key"
                class="hc-chip"
                :class="{ active: activeChip === chip.key }"
                @click="activeChip = chip.key"
              >
                <span class="hc-chip__icon">{{ chip.icon }}</span>
                <span class="hc-chip__label">{{ chip.label }}</span>
              </button>
            </div>

            <!-- 待办列表 -->
            <div class="hc-list">
              <div
                v-for="item in filteredChipTodos"
                :key="item.id"
                class="hc-item"
                :class="{ done: item.completed }"
              >
                <label class="hc-item-check">
                  <input type="checkbox" :checked="item.completed" @change="item.completed = !item.completed" />
                  <span class="hc-item-check__box">
                    <span v-if="item.completed" class="hc-item-check__inner">✓</span>
                  </span>
                </label>
                <div class="hc-item__body">
                  <span class="hc-item__text">{{ item.content }}</span>
                  <div class="hc-item__meta">
                    <span v-if="item.dueTime" class="hc-item__time" :class="{ overdue: item.overdue }">
                      🕐 {{ item.dueTime }}
                    </span>
                    <span v-if="item.pinned && item.remindTime" class="hc-item__time">📌</span>
                  </div>
                </div>
                <div class="hc-item__right">
                  <span v-if="item.repeatType !== 'NONE'" class="hc-item__repeat" title="重复">↻</span>
                  <span v-if="item.overdue" class="hc-item__urgent">!</span>
                </div>
              </div>

              <div v-if="!filteredChipTodos.length" class="hc-empty">
                <div class="hc-empty__icon">🎉</div>
                <div class="hc-empty__text">太棒了，都完成了！</div>
              </div>
            </div>

            <!-- 底部快速添加 -->
            <div class="hc-bottom">
              <div class="hc-bottom__inner">
                <input class="hc-bottom__input" placeholder="快速记录新待办..." />
                <button class="hc-bottom__btn">发送</button>
              </div>
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
import { computed, reactive, ref } from 'vue'

const activeScheme = ref('scheme-a')
const activeFilter = ref('all')
const activeCategory = ref('all')
const activeChip = ref('all')
const collapsedGroups = reactive(new Set<string>(['done']))

const schemes = [
  {
    id: 'scheme-a',
    name: '方案A：智能分组 · 时间轴',
    description: '进度环 + 筛选标签 + 分组时间轴卡片',
    features: [
      '顶部环形进度 + 三组关键数据（待完成/今日/逾期）',
      '问候语增强代入感，底部快速添加栏',
      '水平筛选标签（全部/今日/逾期/已完成）联动分组',
      '分组支持折叠展开，逾期组默认展开，已完成默认收起',
      '每条待办卡片显示标签（置顶/重复/提醒）和时间信息'
    ]
  },
  {
    id: 'scheme-b',
    name: '方案B：极简看板 · 三列',
    description: '统计条 + 三列看板 + 浮动按钮',
    features: [
      '顶部状态统计条（全部/今日/逾期/已完成），点击可筛选',
      '三列看板布局：今日提醒 / 待处理 / 已完成',
      '每条卡片简明显示内容、时间标签和操作菜单',
      '逾期项独立着色警示，已完成自动归类',
      '浮动新建按钮不干扰内容浏览'
    ]
  },
  {
    id: 'scheme-c',
    name: '方案C：专注模式 · 今日焦点',
    description: '焦点头部 + 芯片筛选 + 简洁卡片',
    features: [
      '渐变色焦点头部，显示今日日期和完成进度条',
      '进度条实时反映当日完成比例，增加成就感',
      '每日一句随机鼓励语增加人本温度',
      '芯片筛选（全部/置顶/今天/逾期）快速聚焦',
      '红色感叹号提醒逾期项，底部固定输入栏'
    ]
  }
]

const currentScheme = computed(() => schemes.find(s => s.id === activeScheme.value)!)

// ---------- 模拟数据 ----------
interface MockTodo {
  id: number
  content: string
  completed: boolean
  dueTime: string
  remindTime: string
  pinned: boolean
  overdue: boolean
  repeatType: string
  category: string
  createTime: string
}

const now = new Date()
const today = `${now.getMonth() + 1}/${now.getDate()}`
const tomorrow = `${now.getMonth() + 1}/${now.getDate() + 1}`
const yesterday = `${now.getMonth() + 1}/${now.getDate() - 1}`
const todayMD = `${now.getMonth() + 1}月${now.getDate()}日`

const motivationalQuotes = [
  '先完成，再完美 ✨',
  '每一步都算数 💪',
  '专注当下，做好每一件小事 🌱',
  '今天的努力是明天的底气 🚀',
  '少即是多，做减法 ⭐',
]

const todos = reactive<MockTodo[]>([
  { id: 1, content: '完成项目需求文档评审', completed: false, dueTime: `${today} 18:00`, remindTime: `${today} 17:00`, pinned: true, overdue: false, repeatType: 'NONE', category: 'work', createTime: '2026-07-05' },
  { id: 2, content: '购买生日礼物', completed: false, dueTime: `${today} 20:00`, remindTime: '', pinned: false, overdue: false, repeatType: 'NONE', category: 'personal', createTime: '2026-07-06' },
  { id: 3, content: '每周团队站会', completed: false, dueTime: `${today} 10:00`, remindTime: `${today} 09:45`, pinned: false, overdue: false, repeatType: 'WEEKLY', category: 'work', createTime: '2026-07-01' },
  { id: 4, content: '提交季度报告', completed: false, dueTime: `${yesterday} 17:00`, remindTime: '', pinned: false, overdue: true, repeatType: 'NONE', category: 'work', createTime: '2026-07-02' },
  { id: 5, content: '健身打卡', completed: true, dueTime: `${today} 07:30`, remindTime: '', pinned: false, overdue: false, repeatType: 'DAILY', category: 'personal', createTime: '2026-07-01' },
  { id: 6, content: '阅读系统设计面试 第二章', completed: false, dueTime: `${tomorrow} 22:00`, remindTime: `${tomorrow} 21:00`, pinned: false, overdue: false, repeatType: 'NONE', category: 'study', createTime: '2026-07-06' },
  { id: 7, content: '整理报销单据', completed: false, dueTime: '', remindTime: '', pinned: true, overdue: false, repeatType: 'NONE', category: 'work', createTime: '2026-07-04' },
  { id: 8, content: '预约牙医', completed: false, dueTime: `${tomorrow} 14:00`, remindTime: '', pinned: false, overdue: false, repeatType: 'NONE', category: 'personal', createTime: '2026-07-06' },
  { id: 9, content: '写周报', completed: true, dueTime: `${yesterday} 18:00`, remindTime: '', pinned: false, overdue: false, repeatType: 'WEEKLY', category: 'work', createTime: '2026-07-05' },
  { id: 10, content: '学习 Vue3 Composition API', completed: false, dueTime: '', remindTime: '', pinned: false, overdue: false, repeatType: 'NONE', category: 'study', createTime: '2026-07-03' },
])

const doneCount = computed(() => todos.filter(t => t.completed).length)
const totalCount = computed(() => todos.length)
const pendingCount = computed(() => todos.filter(t => !t.completed).length)
const todayCount = computed(() => todos.filter(t => t.dueTime.startsWith(today) && !t.completed).length)
const overdueCount = computed(() => todos.filter(t => t.overdue && !t.completed).length)
const doneRatio = computed(() => totalCount.value ? Math.round((doneCount.value / totalCount.value) * 100) : 0)
const todayDoneCount = computed(() => todos.filter(t => t.dueTime.startsWith(today) && t.completed).length)
const todayTotalCount = computed(() => todos.filter(t => t.dueTime.startsWith(today)).length)
const todayDoneRatio = computed(() => todayTotalCount.value ? Math.round((todayDoneCount.value / todayTotalCount.value) * 100) : 0)

// ===== 方案 A =====
const hybridFilters = [
  { key: 'all', icon: '📋', label: '全部' },
  { key: 'today', icon: '📅', label: '今日' },
  { key: 'overdue', icon: '⚠️', label: '逾期' },
  { key: 'done', icon: '✅', label: '已完成' },
]

const hybridGroups = computed(() => {
  const pending = todos.filter(t => !t.completed)
  return [
    {
      key: 'overdue',
      icon: '🔴',
      title: '已逾期',
      emptyText: '暂无逾期待办 🎉',
      items: pending.filter(t => t.overdue),
    },
    {
      key: 'today',
      icon: '🟡',
      title: '今日待办',
      emptyText: '今天没有待办',
      items: pending.filter(t => !t.overdue && t.dueTime.startsWith(today)),
    },
    {
      key: 'upcoming',
      icon: '🟢',
      title: '未来安排',
      emptyText: '暂无计划',
      items: pending.filter(t => !t.overdue && !t.dueTime.startsWith(today) && t.dueTime !== ''),
    },
    {
      key: 'unscheduled',
      icon: '⚪',
      title: '未安排',
      emptyText: '都已安排上了',
      items: pending.filter(t => !t.overdue && !t.dueTime),
    },
    {
      key: 'done',
      icon: '✅',
      title: '已完成',
      emptyText: '还没有完成任何待办',
      items: todos.filter(t => t.completed),
    },
  ].filter(g => g.items.length > 0)
})

const filteredHybridGroups = computed(() => {
  if (activeFilter.value === 'all') return hybridGroups.value
  if (activeFilter.value === 'done') return hybridGroups.value.filter(g => g.key === 'done')
  if (activeFilter.value === 'overdue') return hybridGroups.value.filter(g => g.key === 'overdue')
  if (activeFilter.value === 'today') return hybridGroups.value.filter(g => g.key === 'today' || g.key === 'overdue')
  return hybridGroups.value
})

function toggleGroup(key: string) {
  if (collapsedGroups.has(key)) {
    collapsedGroups.delete(key)
  } else {
    collapsedGroups.add(key)
  }
}

// ===== 方案 B =====
const kanbanColumns = computed(() => {
  const pending = todos.filter(t => !t.completed)
  const remindToday = pending.filter(t =>
    t.dueTime.startsWith(today) || t.remindTime.startsWith(today)
  )
  const otherPending = pending.filter(t =>
    !t.dueTime.startsWith(today) && !t.remindTime.startsWith(today)
  )
  const doneItems = todos.filter(t => t.completed)

  return [
    {
      key: 'remind',
      title: '今日提醒',
      emptyText: '今日暂无提醒',
      items: remindToday,
    },
    {
      key: 'pending',
      title: '待处理',
      emptyText: '全部已处理',
      items: otherPending,
    },
    {
      key: 'done',
      title: '已完成',
      emptyText: '暂未完成',
      items: doneItems,
    },
  ]
})

const chips = [
  { key: 'all', icon: '📋', label: '全部' },
  { key: 'pinned', icon: '📌', label: '置顶' },
  { key: 'today', icon: '📅', label: '今天' },
  { key: 'overdue', icon: '⚠️', label: '逾期' },
]

const filteredChipTodos = computed(() => {
  let result = [...todos]
  if (activeChip.value === 'pinned') result = result.filter(t => t.pinned)
  else if (activeChip.value === 'today') result = result.filter(t => t.dueTime.startsWith(today))
  else if (activeChip.value === 'overdue') result = result.filter(t => t.overdue)
  return result.sort((a, b) => {
    if (a.overdue && !b.overdue) return -1
    if (!a.overdue && b.overdue) return 1
    if (a.pinned && !b.pinned) return -1
    if (!a.pinned && b.pinned) return 1
    return 0
  })
})
</script>

<style scoped lang="scss">
.todos-design-preview {
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
  flex-wrap: wrap;
}

.scheme-tab {
  padding: 10px 20px;
  border-radius: 999px;
  border: 2px solid #2563eb;
  background: white;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 15px;
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

.scheme-page {
  padding: 16px;
  background: #faf8f5;
  height: calc(100% - 78px);
  overflow-y: auto;
}

// =====================================================
// 方案 A：智能分组时间轴 — 融合方案A的统计环 + 方案B的筛选标签 + 带标签的分组列表
// =====================================================
.ha-greeting {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}

.ha-greeting__left {
  flex: 1;
}

.ha-greeting__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 22px;
  color: #1e293b;
  margin: 0 0 2px;
}

.ha-greeting__sub {
  font-size: 13px;
  color: #64748b;
  margin: 0;

  strong {
    color: #2563eb;
    font-weight: 700;
  }
}

.ha-greeting__badge {
  font-size: 13px;
  color: #2563eb;
  background: #dbeafe;
  padding: 4px 12px;
  border-radius: 999px;
  font-weight: 600;
  white-space: nowrap;
}

.ha-stats-card {
  display: flex;
  align-items: center;
  gap: 18px;
  background: white;
  border-radius: 16px;
  padding: 14px 18px;
  margin-bottom: 14px;
  border: 2px solid #e2e8f0;
}

.ha-ring {
  position: relative;
  width: 60px;
  height: 60px;
  flex-shrink: 0;
}

.ha-ring__svg {
  width: 100%;
  height: 100%;
}

.ha-ring__text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  color: #1e293b;
}

.ha-stats {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.ha-stat {
  text-align: center;
  flex: 1;
}

.ha-stat__value {
  font-size: 20px;
  font-weight: 800;
  display: block;

  &--blue { color: #2563eb; }
  &--orange { color: #f59e0b; }
  &--red { color: #ef4444; }
}

.ha-stat__label {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 1px;
}

.ha-stat-divider {
  width: 1px;
  height: 28px;
  background: #e2e8f0;
}

.ha-filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar { display: none; }
}

.ha-filter-tab {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 7px 16px;
  border-radius: 999px;
  border: 2px solid #e2e8f0;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;

  &.active {
    border-color: #2563eb;
    background: #eff6ff;
  }
}

.ha-filter-tab__icon { font-size: 13px; }

.ha-filter-tab__label {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 13px;
  color: #1e293b;
}

.ha-group-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 56px;
}

.ha-group {
  background: white;
  border-radius: 14px;
  border: 2px solid #e2e8f0;
  overflow: hidden;
  transition: border-color 0.2s ease;

  &--overdue { border-color: #fecaca; }
  &--today { border-color: #fde68a; }
  &--upcoming { border-color: #bbf7d0; }
  &--unscheduled { border-color: #e2e8f0; }
}

.ha-group__head {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 11px 14px;
  border: none;
  background: transparent;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.ha-group__icon { font-size: 15px; }

.ha-group__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
}

.ha-group__count {
  margin-left: auto;
  font-size: 11px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 999px;
}

.ha-group__arrow {
  font-size: 18px;
  color: #94a3b8;
  margin-left: 4px;
  transition: transform 0.2s ease;

  &.expanded {
    transform: rotate(90deg);
  }
}

.ha-group__body {
  padding: 2px 14px 10px;
}

.ha-group-empty {
  padding: 16px 0;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}

.ha-todo-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;

  &:last-child { border-bottom: none; }

  &.is-done {
    opacity: 0.55;

    .ha-card-body__text {
      text-decoration: line-through;
      color: #94a3b8;
    }
  }
}

.ha-card-check {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #cbd5e1;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  margin-top: 2px;
  padding: 0;
  transition: all 0.2s ease;

  &.checked {
    background: #22c55e;
    border-color: #22c55e;
  }

  &__inner {
    color: white;
    font-size: 11px;
    font-weight: 700;
  }
}

.ha-card-body {
  flex: 1;
  min-width: 0;
}

.ha-card-body__text {
  font-size: 14px;
  color: #1e293b;
  display: block;
  line-height: 1.4;
}

.ha-card-body__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
}

.ha-card-time {
  font-size: 11px;
  color: #64748b;

  &.overdue {
    color: #ef4444;
    font-weight: 600;
  }
}

.ha-tag {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #64748b;
  line-height: 1.3;

  &--pin { background: #fef3c7; color: #d97706; }
  &--repeat { background: #dbeafe; color: #2563eb; }
  &--remind { background: #fce7f3; color: #db2777; }
}

.ha-bottom-bar {
  position: absolute;
  bottom: 50px;
  left: 16px;
  right: 16px;
  display: flex;
  gap: 8px;
  background: white;
  border: 2px solid #2563eb;
  border-radius: 999px;
  padding: 4px;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.15);
}

.ha-bottom-bar__input {
  flex: 1;
  border: none;
  outline: none;
  padding: 9px 16px;
  font-size: 14px;
  background: transparent;
  color: #1e293b;

  &::placeholder { color: #94a3b8; }
}

.ha-bottom-bar__btn {
  padding: 8px 20px;
  border: none;
  background: #2563eb;
  color: white;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

// =====================================================
// 方案 B：极简看板风 — 三列看板 + 统计条 + 浮动按钮
// =====================================================
.hb-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.hb-header__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 22px;
  color: #1e293b;
  margin: 0;
}

.hb-header__settings {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 4px;
}

.hb-stat-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.hb-stat-item {
  flex: 1;
  text-align: center;
  padding: 10px 4px;
  border-radius: 12px;
  background: white;
  border: 2px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s ease;

  &--today.active {
    border-color: #f59e0b;
    background: #fffbeb;
  }

  &--overdue.active {
    border-color: #ef4444;
    background: #fef2f2;
  }

  &--done.active {
    border-color: #22c55e;
    background: #f0fdf4;
  }

  &:hover {
    transform: scale(0.96);
  }
}

.hb-stat-item__num {
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
  display: block;
}

.hb-stat-item__label {
  font-size: 11px;
  color: #64748b;
  margin-top: 1px;
}

.hb-kanban {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 64px;
}

.hb-column {
  background: white;
  border-radius: 14px;
  border: 2px solid #e2e8f0;
  overflow: hidden;

  &--remind { border-color: #fde68a; }
  &--pending { border-color: #e2e8f0; }
  &--done { border-color: #bbf7d0; }
}

.hb-column__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.hb-column--remind .hb-column__head {
  background: #fffbeb;
}

.hb-column--done .hb-column__head {
  background: #f0fdf4;
}

.hb-column__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
}

.hb-column__count {
  font-size: 11px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 999px;
}

.hb-column__body {
  padding: 8px 14px;
}

.hb-column__empty {
  padding: 20px 0;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}

.hb-card {
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;

  &:last-child { border-bottom: none; }

  &.is-done {
    opacity: 0.5;

    .hb-card__text {
      text-decoration: line-through;
      color: #94a3b8;
    }
  }
}

.hb-card__top {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.hb-card-check {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  border: 2px solid #cbd5e1;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  margin-top: 1px;
  padding: 0;
  transition: all 0.2s ease;

  &.checked {
    background: #22c55e;
    border-color: #22c55e;
  }

  &__inner {
    color: white;
    font-size: 11px;
    font-weight: 700;
  }
}

.hb-card__text {
  flex: 1;
  font-size: 14px;
  color: #1e293b;
  line-height: 1.4;
  min-width: 0;
}

.hb-card__more {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 16px;
  cursor: pointer;
  padding: 0 2px;
  line-height: 1;
  flex-shrink: 0;
}

.hb-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
  padding-left: 30px;
}

.hb-tag {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #64748b;

  &--time {
    background: #eff6ff;
    color: #2563eb;

    &.overdue {
      background: #fef2f2;
      color: #ef4444;
      font-weight: 600;
    }
  }

  &--pin { background: #fef3c7; color: #d97706; }
  &--repeat { background: #dbeafe; color: #2563eb; }
}

.hb-fab {
  position: absolute;
  bottom: 50px;
  right: 20px;
  width: 54px;
  height: 54px;
  border-radius: 50%;
  border: none;
  background: #2563eb;
  color: white;
  font-size: 26px;
  font-weight: 300;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.05);
  }
}

// =====================================================
// 方案 C：专注模式 · 今日焦点 — 焦点头部 + 芯片筛选 + 简洁列表
// =====================================================
.hc-hero {
  position: relative;
  margin: -16px -16px 14px;
  padding: 20px 16px 18px;
  border-radius: 0 0 24px 24px;
  overflow: hidden;
  color: white;
}

.hc-hero__bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 50%, #7c3aed 100%);
  z-index: 0;
}

.hc-hero__content {
  position: relative;
  z-index: 1;
}

.hc-hero__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.hc-hero__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 22px;
  margin: 0;
}

.hc-hero__date {
  font-size: 13px;
  opacity: 0.85;
}

.hc-hero__progress-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.hc-hero__progress-bar {
  flex: 1;
  height: 6px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 999px;
  overflow: hidden;
}

.hc-hero__progress-fill {
  height: 100%;
  border-radius: 999px;
  background: #fbbf24;
  transition: width 0.4s ease;
}

.hc-hero__progress-text {
  font-size: 12px;
  opacity: 0.9;
  white-space: nowrap;
}

.hc-hero__quote {
  font-size: 13px;
  opacity: 0.8;
  margin: 0;
  font-style: italic;
}

.hc-chips {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar { display: none; }
}

.hc-chip {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 7px 14px;
  border-radius: 999px;
  border: none;
  background: #f1f5f9;
  cursor: pointer;
  transition: all 0.2s ease;

  &.active {
    background: #2563eb;

    .hc-chip__icon,
    .hc-chip__label {
      color: white;
    }
  }
}

.hc-chip__icon { font-size: 13px; }
.hc-chip__label {
  font-size: 13px;
  color: #1e293b;
  font-weight: 500;
}

.hc-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 56px;
}

.hc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  transition: all 0.2s ease;

  &.done {
    opacity: 0.5;

    .hc-item__text {
      text-decoration: line-through;
      color: #94a3b8;
    }
  }

  &:hover {
    border-color: #cbd5e1;
  }
}

.hc-item-check {
  position: relative;
  display: flex;
  align-items: center;
  flex-shrink: 0;

  input {
    position: absolute;
    opacity: 0;
  }
}

.hc-item-check__box {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  border: 2px solid #cbd5e1;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.hc-item-check input:checked + .hc-item-check__box {
  background: #22c55e;
  border-color: #22c55e;
}

.hc-item-check__inner {
  color: white;
  font-size: 12px;
  font-weight: 700;
}

.hc-item__body {
  flex: 1;
  min-width: 0;
}

.hc-item__text {
  font-size: 14px;
  color: #1e293b;
  display: block;
  line-height: 1.4;
}

.hc-item__meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 3px;
}

.hc-item__time {
  font-size: 11px;
  color: #64748b;

  &.overdue {
    color: #ef4444;
    font-weight: 600;
  }
}

.hc-item__right {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.hc-item__repeat {
  font-size: 16px;
  color: #2563eb;
  opacity: 0.6;
}

.hc-item__urgent {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fef2f2;
  color: #ef4444;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hc-empty {
  text-align: center;
  padding: 32px 0;
}

.hc-empty__icon {
  font-size: 36px;
  margin-bottom: 8px;
}

.hc-empty__text {
  font-size: 14px;
  color: #94a3b8;
}

.hc-bottom {
  position: absolute;
  bottom: 50px;
  left: 16px;
  right: 16px;
}

.hc-bottom__inner {
  display: flex;
  gap: 8px;
  background: white;
  border-radius: 14px;
  padding: 6px 8px;
  border: 2px solid #e2e8f0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.hc-bottom__input {
  flex: 1;
  border: none;
  outline: none;
  padding: 8px 6px;
  font-size: 14px;
  color: #1e293b;

  &::placeholder { color: #94a3b8; }
}

.hc-bottom__btn {
  padding: 8px 16px;
  border: none;
  background: #2563eb;
  color: white;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

// =====================================================
// 右侧方案说明
// =====================================================
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
