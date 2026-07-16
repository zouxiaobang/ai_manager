import { createRouter, createWebHashHistory, type RouteLocationNormalized } from 'vue-router'
import MobileV2Layout from '@/mobile-v2/layouts/MobileV2Layout.vue'

const scrollPositions = new Map<string, number>()

function saveScrollPosition(route: RouteLocationNormalized) {
  if (!route.path) return
  const main = document.querySelector('.mobile-v2-app__main')
  if (main) {
    scrollPositions.set(route.path, main.scrollTop)
  }
}

function restoreScrollPosition(route: RouteLocationNormalized) {
  const saved = scrollPositions.get(route.path)
  if (saved !== undefined && saved > 0) {
    requestAnimationFrame(() => {
      const main = document.querySelector('.mobile-v2-app__main')
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
      component: MobileV2Layout,
      redirect: '/home',
      children: [
        {
          path: 'home',
          name: 'mobile-v2-home',
          component: () => import('@/mobile-v2/views/home/V2HomeView.vue'),
          meta: { titleKey: 'portal.dashboard.warRoom.title', tab: 'home' },
        },
        {
          path: 'notebook',
          name: 'mobile-v2-notebook',
          component: () => import('@/mobile-v2/views/notebook/V2NotebookView.vue'),
          meta: {
            titleKey: 'portal.menu.notebook',
            tab: 'notebook',
            headerAction: { icon: 'Search', to: '/notebook/search', ariaLabelKey: 'notebook.search' },
          },
        },
        {
          path: 'notebook/search',
          name: 'mobile-v2-note-search',
          component: () => import('@/mobile-v2/views/notebook/V2NoteSearchView.vue'),
          meta: { titleKey: 'notebook.search', tab: 'notebook', hideAppHeader: true },
        },
        {
          path: 'notebook/folder/:key',
          name: 'mobile-v2-note-folder',
          component: () => import('@/mobile-v2/views/notebook/V2NoteFolderView.vue'),
          meta: { titleKey: 'portal.menu.notebook', tab: 'notebook', hideTabBar: true },
        },
        {
          path: 'notebook/:id',
          name: 'mobile-v2-note-detail',
          component: () => import('@/mobile-v2/views/notebook/V2NoteDetailView.vue'),
          meta: { titleKey: 'portal.menu.notebook', tab: 'notebook', hideTabBar: true },
        },
        {
          path: 'todos',
          name: 'mobile-v2-todos',
          component: () => import('@/mobile-v2/views/todos/V2TodosView.vue'),
          meta: { titleKey: 'portal.menu.todos', tab: 'todos' },
        },
        {
          path: 'functions',
          name: 'mobile-v2-functions',
          component: () => import('@/mobile-v2/views/functions/V2FunctionsView.vue'),
          meta: { titleKey: 'portal.menu.functions', tab: 'more' },
        },
        {
          path: 'settings',
          name: 'mobile-v2-settings',
          component: () => import('@/mobile-v2/views/settings/V2SettingsView.vue'),
          meta: { titleKey: 'portal.menu.settings', hideTabBar: true },
        },
        {
          path: 'more',
          name: 'mobile-v2-more',
          component: () => import('@/mobile-v2/views/more/V2MoreView.vue'),
          meta: { titleKey: 'mobile.nav.more', tab: 'more' },
        },
        {
          path: 'ecommerce',
          name: 'mobile-v2-ecommerce',
          component: () => import('@/mobile-v2/views/ecommerce/V2EcommerceView.vue'),
          meta: { titleKey: 'ecommerce.workbenchTitle', tab: 'more' },
        },
        {
          path: 'ecommerce/orders',
          name: 'mobile-v2-ecommerce-orders',
          component: () => import('@/mobile-v2/views/ecommerce/V2OrderView.vue'),
          meta: { titleKey: 'ecommerce.nav.order', hideTabBar: true },
        },
        {
          path: 'ecommerce/products',
          name: 'mobile-v2-ecommerce-products',
          component: () => import('@/mobile-v2/views/ecommerce/V2ProductView.vue'),
          meta: { titleKey: 'ecommerce.nav.product', hideTabBar: true },
        },
        {
          path: 'ecommerce/inventory',
          name: 'mobile-v2-ecommerce-inventory',
          component: () => import('@/mobile-v2/views/ecommerce/V2InventoryView.vue'),
          meta: { titleKey: 'ecommerce.nav.inventory', hideTabBar: true },
        },
        {
          path: 'ecommerce/express',
          name: 'mobile-v2-ecommerce-express',
          component: () => import('@/mobile-v2/views/ecommerce/V2ExpressView.vue'),
          meta: { titleKey: 'ecommerce.nav.express', hideTabBar: true },
        },
        {
          path: 'ecommerce/shops',
          name: 'mobile-v2-ecommerce-shops',
          component: () => import('@/mobile-v2/views/ecommerce/V2ShopView.vue'),
          meta: { titleKey: 'ecommerce.nav.platformShop', hideTabBar: true },
        },
        {
          path: 'ecommerce/cartons',
          name: 'mobile-v2-ecommerce-cartons',
          component: () => import('@/mobile-v2/views/ecommerce/V2CartonView.vue'),
          meta: { titleKey: 'ecommerce.nav.carton', hideTabBar: true },
        },
        {
          path: 'ecommerce/factory',
          name: 'mobile-v2-ecommerce-factory',
          component: () => import('@/mobile-v2/views/ecommerce/V2FactoryView.vue'),
          meta: { titleKey: 'ecommerce.factory.pageTitle', hideTabBar: true },
        },
        {
          path: 'ecommerce/monthly-settlement',
          name: 'mobile-v2-ecommerce-monthly-settlement',
          component: () => import('@/mobile-v2/views/ecommerce/V2MonthlySettlementView.vue'),
          meta: { titleKey: 'ecommerce.nav.monthlySettlement', hideTabBar: true },
        },
        {
          path: 'users',
          name: 'mobile-v2-users',
          component: () => import('@/mobile-v2/views/users/V2UsersView.vue'),
          meta: { titleKey: 'portal.menu.permission', hideTabBar: true },
        },
        {
          path: 'pixel-dog',
          name: 'mobile-v2-pixel-dog',
          component: () => import('@/mobile-v2/views/pixel-dog/V2PixelDogView.vue'),
          meta: { titleKey: 'functions.items.pixelDog.name', hideTabBar: true },
        },
        {
          path: 'pomodoro',
          name: 'mobile-v2-pomodoro',
          component: () => import('@/mobile-v2/views/pomodoro/V2PomodoroView.vue'),
          meta: { titleKey: 'portal.menu.pomodoro', hideTabBar: true },
        },
        {
          path: '24hour',
          name: 'mobile-v2-24hour',
          component: () => import('@/mobile-v2/views/24hour/V2TwentyFourHourView.vue'),
          meta: {
            titleKey: 'portal.menu.24hour',
            tab: '24hour',
            headerAction: { icon: 'DataAnalysis', to: '/24hour/stats', ariaLabelKey: 'portal.dashboard.navStats' },
          },
        },
        {
          path: '24hour/stats',
          name: 'mobile-v2-24hour-stats',
          component: () => import('@/mobile-v2/views/24hour/V2TwentyFourHourStatsView.vue'),
          meta: { titleKey: 'portal.dashboard.navStats', hideTabBar: true, hideAppHeader: true },
        },
        {
          path: 'profile',
          name: 'mobile-v2-profile',
          component: () => import('@/mobile-v2/views/profile/V2ProfileEditView.vue'),
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
