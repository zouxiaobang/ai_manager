import { createRouter, createWebHashHistory, type RouteLocationNormalized } from 'vue-router'
import MobileLayout from '@/mobile/layouts/MobileLayout.vue'

const scrollPositions = new Map<string, number>()

function saveScrollPosition(route: RouteLocationNormalized) {
  if (!route.path) return
  const main = document.querySelector('.mobile-app__main')
  if (main) {
    scrollPositions.set(route.path, main.scrollTop)
  }
}

function restoreScrollPosition(route: RouteLocationNormalized) {
  const saved = scrollPositions.get(route.path)
  if (saved !== undefined && saved > 0) {
    requestAnimationFrame(() => {
      const main = document.querySelector('.mobile-app__main')
      if (main) {
        main.scrollTop = saved
      }
    })
  }
}

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      component: MobileLayout,
      redirect: '/home',
      children: [
        {
          path: 'home',
          name: 'mobile-home',
          component: () => import('@/mobile/views/home/MobileHomeView.vue'),
          meta: { titleKey: 'portal.dashboard.warRoom.title', tab: 'home' },
        },
        {
          path: 'notebook',
          name: 'mobile-notebook',
          component: () => import('@/mobile/views/notebook/MobileNotebookView.vue'),
          meta: {
            titleKey: 'portal.menu.notebook',
            tab: 'notebook',
            headerAction: { icon: 'Search', to: '/notebook/search', ariaLabelKey: 'notebook.search' },
          },
        },
        {
          path: 'notebook/search',
          name: 'mobile-note-search',
          component: () => import('@/mobile/views/notebook/MobileNoteSearchView.vue'),
          meta: { titleKey: 'notebook.search', tab: 'notebook', hideAppHeader: true },
        },
        {
          path: 'notebook/folder/:key',
          name: 'mobile-note-folder',
          component: () => import('@/mobile/views/notebook/MobileNoteFolderView.vue'),
          meta: { titleKey: 'portal.menu.notebook', tab: 'notebook', hideTabBar: true },
        },
        {
          path: 'notebook/:id',
          name: 'mobile-note-detail',
          component: () => import('@/mobile/views/notebook/MobileNoteDetailView.vue'),
          meta: { titleKey: 'portal.menu.notebook', tab: 'notebook', hideTabBar: true },
        },
        {
          path: 'todos',
          name: 'mobile-todos',
          component: () => import('@/mobile/views/todos/MobileTodosView.vue'),
          meta: { titleKey: 'portal.menu.todos', tab: 'todos' },
        },
        {
          path: 'functions',
          name: 'mobile-functions',
          component: () => import('@/mobile/views/functions/MobileFunctionsView.vue'),
          meta: { titleKey: 'portal.menu.functions', tab: 'more' },
        },
        {
          path: 'settings',
          name: 'mobile-settings',
          component: () => import('@/mobile/views/settings/MobileSettingsView.vue'),
          meta: { titleKey: 'portal.menu.settings', hideTabBar: true },
        },
        {
          path: 'more',
          name: 'mobile-more',
          component: () => import('@/mobile/views/more/MobileMoreView.vue'),
          meta: { titleKey: 'mobile.nav.more', tab: 'more' },
        },
        {
          path: 'ecommerce',
          name: 'mobile-ecommerce',
          component: () => import('@/mobile/views/ecommerce/MobileEcommerceView.vue'),
          meta: { titleKey: 'ecommerce.workbenchTitle', tab: 'more' },
        },
        {
          path: 'ecommerce/orders',
          name: 'mobile-ecommerce-orders',
          component: () => import('@/mobile/views/ecommerce/MobileOrderView.vue'),
          meta: { titleKey: 'ecommerce.nav.order', hideTabBar: true },
        },
        {
          path: 'ecommerce/products',
          name: 'mobile-ecommerce-products',
          component: () => import('@/mobile/views/ecommerce/MobileProductView.vue'),
          meta: { titleKey: 'ecommerce.nav.product', hideTabBar: true },
        },
        {
          path: 'ecommerce/inventory',
          name: 'mobile-ecommerce-inventory',
          component: () => import('@/mobile/views/ecommerce/MobileInventoryView.vue'),
          meta: { titleKey: 'ecommerce.nav.inventory', hideTabBar: true },
        },
        {
          path: 'ecommerce/express',
          name: 'mobile-ecommerce-express',
          component: () => import('@/mobile/views/ecommerce/MobileExpressView.vue'),
          meta: { titleKey: 'ecommerce.nav.express', hideTabBar: true },
        },
        {
          path: 'ecommerce/shops',
          name: 'mobile-ecommerce-shops',
          component: () => import('@/mobile/views/ecommerce/MobileShopView.vue'),
          meta: { titleKey: 'ecommerce.nav.platformShop', hideTabBar: true },
        },
        {
          path: 'ecommerce/cartons',
          name: 'mobile-ecommerce-cartons',
          component: () => import('@/mobile/views/ecommerce/MobileCartonView.vue'),
          meta: { titleKey: 'ecommerce.nav.carton', hideTabBar: true },
        },
        {
          path: 'ecommerce/factory',
          name: 'mobile-ecommerce-factory',
          component: () => import('@/mobile/views/ecommerce/MobileFactoryView.vue'),
          meta: { titleKey: 'ecommerce.factory.pageTitle', hideTabBar: true },
        },
        {
          path: 'ecommerce/monthly-settlement',
          name: 'mobile-ecommerce-monthly-settlement',
          component: () => import('@/mobile/views/ecommerce/MobileMonthlySettlementView.vue'),
          meta: { titleKey: 'ecommerce.nav.monthlySettlement', hideTabBar: true },
        },
        {
          path: 'users',
          name: 'mobile-users',
          component: () => import('@/mobile/views/users/MobileUsersView.vue'),
          meta: { titleKey: 'portal.menu.permission', hideTabBar: true },
        },
        {
          path: 'pixel-dog',
          name: 'mobile-pixel-dog',
          component: () => import('@/mobile/views/pixel-dog/MobilePixelDogView.vue'),
          meta: { titleKey: 'functions.items.pixelDog.name', hideTabBar: true },
        },
        {
          path: 'pomodoro',
          name: 'mobile-pomodoro',
          component: () => import('@/mobile/views/pomodoro/MobilePomodoroView.vue'),
          meta: { titleKey: 'portal.menu.pomodoro', hideTabBar: true },
        },
        {
          path: 'ai-knowledge',
          name: 'mobile-ai-knowledge',
          component: () => import('@/mobile/views/ai-knowledge/MobileAiKnowledgeView.vue'),
          meta: { titleKey: 'aiKnowledge.title', tab: 'aiKnowledge' },
        },
        {
          path: '24hour',
          name: 'mobile-24hour',
          component: () => import('@/mobile/views/24hour/MobileTwentyFourHourView.vue'),
          meta: {
            titleKey: 'portal.menu.24hour',
            tab: '24hour',
            headerAction: { icon: 'DataAnalysis', to: '/24hour/stats', ariaLabelKey: 'portal.dashboard.navStats' },
          },
        },
        {
          path: '24hour/stats',
          name: 'mobile-24hour-stats',
          component: () => import('@/mobile/views/24hour/MobileTwentyFourHourStatsView.vue'),
          meta: { titleKey: 'portal.dashboard.navStats', hideTabBar: true, hideAppHeader: true },
        },
        {
          path: 'profile',
          name: 'mobile-profile',
          component: () => import('@/mobile/views/profile/MobileProfileEditView.vue'),
          meta: { titleKey: 'mobile.v2.profile', hideTabBar: true },
        },
      ],
    },
  ],
})

router.beforeEach((_to, from) => {
  saveScrollPosition(from)
})

router.afterEach((to) => {
  restoreScrollPosition(to)
})

router.beforeEach((to, _from, next) => {
  if (to.path === '/' || to.path === '') {
    next({ path: '/home', replace: true })
    return
  }
  next()
})

export default router
