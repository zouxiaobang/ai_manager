import { createRouter, createWebHashHistory, type RouteLocationNormalized } from 'vue-router'
import MobileLayout from '@/mobile/layouts/MobileLayout.vue'

/** 保存每个路由路径的滚动位置，用于返回时恢复 */
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
  scrollBehavior() {
    // 不在这里处理滚动，使用 beforeEach/afterEach 手动管理
  },
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
          meta: { titleKey: 'mobile.home.pageTitle', tab: 'home' },
        },
        {
          path: 'notebook',
          name: 'mobile-notebook',
          component: () => import('@/mobile/views/notebook/MobileNotebookView.vue'),
          meta: { titleKey: 'portal.menu.notebook', tab: 'notebook', hideAppHeader: true },
        },
        {
          path: 'notebook/:id',
          name: 'mobile-note-detail',
          component: () => import('@/mobile/views/notebook/MobileNoteDetailView.vue'),
          meta: { titleKey: 'notebook.title', hideTabBar: true, hideAppHeader: true },
        },
        {
          path: 'notebook/folder/:folderKey',
          name: 'mobile-notebook-folder',
          component: () => import('@/mobile/views/notebook/MobileNotebookFolderView.vue'),
          meta: { titleKey: 'mobile.notebook.folder', hideTabBar: true, hideAppHeader: true },
        },
        {
          path: 'notebook/search',
          name: 'mobile-note-search',
          component: () => import('@/mobile/views/notebook/MobileNoteSearchView.vue'),
          meta: { titleKey: 'mobile.notebook.search', hideTabBar: true, hideAppHeader: true },
        },
        {
          path: 'todos',
          name: 'mobile-todos',
          component: () => import('@/mobile/views/todos/MobileTodosView.vue'),
          meta: { titleKey: 'portal.menu.todos', tab: 'todos', hideAppHeader: true },
        },
        {
          path: 'pomodoro',
          redirect: '/home',
        },
        {
          path: 'functions',
          name: 'mobile-functions',
          component: () => import('@/mobile/views/functions/MobileFunctionsView.vue'),
          meta: { titleKey: 'portal.menu.functions', tab: 'more' },
        },
        {
          path: 'ecommerce',
          name: 'mobile-ecommerce',
          component: () => import('@/mobile/views/ecommerce/MobileEcommerceView.vue'),
          meta: { titleKey: 'ecommerce.workbenchTitle', tab: 'more', hideAppHeader: true },
        },
        {
          path: 'ecommerce/monthly-settlement',
          name: 'mobile-ecommerce-monthly-settlement',
          component: () => import('@/mobile/views/monthly-settlement/MobileMonthlySettlementView.vue'),
          meta: { titleKey: 'ecommerce.nav.monthlySettlement', hideTabBar: true, hideAppHeader: true },
        },
        {
          path: 'ecommerce/orders',
          name: 'mobile-ecommerce-orders',
          component: () => import('@/mobile/views/order/MobileOrderView.vue'),
          meta: { titleKey: 'ecommerce.nav.order', hideTabBar: true, hideAppHeader: true },
        },
        {
          path: 'ecommerce/inventory',
          name: 'mobile-ecommerce-inventory',
          component: () => import('@/mobile/views/inventory/MobileInventoryView.vue'),
          meta: { titleKey: 'ecommerce.nav.inventory', hideTabBar: true, hideAppHeader: true },
        },
        {
          path: 'ecommerce/products',
          name: 'mobile-ecommerce-products',
          component: () => import('@/mobile/views/products/MobileProductsView.vue'),
          meta: { titleKey: 'ecommerce.nav.product', hideTabBar: true, hideAppHeader: true },
        },
        {
          path: 'ecommerce/express',
          name: 'mobile-ecommerce-express',
          component: () => import('@/mobile/views/express/MobileExpressView.vue'),
          meta: { titleKey: 'ecommerce.nav.express', hideTabBar: true, hideAppHeader: true },
        },
        
        {
          path: 'ecommerce/shops',
          name: 'mobile-ecommerce-shops',
          component: () => import('@/mobile/views/shop/MobileShopView.vue'),
          meta: { titleKey: 'ecommerce.nav.platformShop', hideTabBar: true, hideAppHeader: true },
        },
        {
          path: 'ecommerce/cartons',
          name: 'mobile-ecommerce-cartons',
          component: () => import('@/mobile/views/carton/MobileCartonView.vue'),
          meta: { titleKey: 'ecommerce.nav.carton', hideTabBar: true, hideAppHeader: true },
        },
        {
          path: 'ecommerce/factory',
          name: 'mobile-ecommerce-factory',
          component: () => import('@/mobile/views/factory/MobileFactoryView.vue'),
          meta: { titleKey: 'ecommerce.factory.pageTitle', hideTabBar: true, hideAppHeader: true },
        },
        {
          path: 'ecommerce/factory/production',
          name: 'mobile-ecommerce-factory-production',
          redirect: { name: 'mobile-ecommerce-factory' },
        },
        {
          path: 'ecommerce/factory/machines',
          name: 'mobile-ecommerce-factory-machines',
          redirect: { name: 'mobile-ecommerce-factory' },
        },
        {
          path: 'ecommerce/factory/quality',
          name: 'mobile-ecommerce-factory-quality',
          redirect: { name: 'mobile-ecommerce-factory' },
        },
        {
          path: 'ecommerce/factory/inventory',
          name: 'mobile-ecommerce-factory-inventory',
          redirect: { name: 'mobile-ecommerce-factory' },
        },
        {
          path: 'ecommerce/factory/workshop',
          name: 'mobile-ecommerce-factory-workshop',
          redirect: { name: 'mobile-ecommerce-factory' },
        },
        {
          path: 'ecommerce/factory/schedule',
          name: 'mobile-ecommerce-factory-schedule',
          redirect: { name: 'mobile-ecommerce-factory' },
        },
        {
          path: 'settings',
          name: 'mobile-settings',
          component: () => import('@/mobile/views/settings/MobileSettingsView.vue'),
          meta: { titleKey: 'portal.menu.settings', tab: 'more' },
        },
        {
          path: 'more',
          name: 'mobile-more',
          component: () => import('@/mobile/views/more/MobileMoreView.vue'),
          meta: { titleKey: 'mobile.nav.more', tab: 'more' },
        },
        {
          path: 'users',
          name: 'mobile-users',
          component: () => import('@/mobile/views/users/MobileUsersView.vue'),
          meta: { titleKey: 'portal.menu.permission', hideTabBar: true },
        },
        {
          path: 'note-design-preview',
          name: 'mobile-note-design-preview',
          component: () => import('@/mobile/views/notebook/MobileNoteDesignPreview.vue'),
          meta: { titleKey: '笔记设计预览', hideTabBar: true },
        },
        {
          path: 'carton-design-preview',
          name: 'mobile-carton-design-preview',
          component: () => import('@/mobile/views/carton/MobileCartonDesignPreview.vue'),
          meta: { titleKey: '纸箱管理设计预览', hideTabBar: true },
        },
        {
          path: 'shop-design-preview',
          name: 'mobile-shop-design-preview',
          component: () => import('@/mobile/views/shop/MobileShopDesignPreview.vue'),
          meta: { titleKey: '店铺管理设计预览', hideTabBar: true },
        },
        {
          path: 'express-design-preview',
          name: 'mobile-express-design-preview',
          component: () => import('@/mobile/views/express/MobileExpressDesignPreview.vue'),
          meta: { titleKey: '快递管理设计预览', hideTabBar: true },
        },
        {
          path: 'inventory-design-preview',
          name: 'mobile-inventory-design-preview',
          component: () => import('@/mobile/views/inventory/MobileInventoryDesignPreview.vue'),
          meta: { titleKey: '库存中心设计预览', hideTabBar: true },
        },
        {
          path: 'order-design-preview',
          name: 'mobile-order-design-preview',
          component: () => import('@/mobile/views/order/MobileOrderDesignPreview.vue'),
          meta: { titleKey: '订单中心设计预览', hideTabBar: true },
        },
        {
          path: 'monthly-settlement-design-preview',
          name: 'mobile-monthly-settlement-design-preview',
          component: () => import('@/mobile/views/monthly-settlement/MobileMonthlySettlementDesignPreview.vue'),
          meta: { titleKey: '月结统计设计预览', hideTabBar: true },
        },
        {
          path: 'todos-design-preview',
          name: 'mobile-todos-design-preview',
          component: () => import('@/mobile/views/todos/MobileTodosDesignPreview.vue'),
          meta: { titleKey: '待办事项设计预览', hideTabBar: true },
        },
      ],
    },
  ],
})

// 离开页面时保存滚动位置
router.beforeEach((_to, from) => {
  saveScrollPosition(from)
})

// 进入页面时恢复滚动位置
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
