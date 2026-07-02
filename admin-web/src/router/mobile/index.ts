import { createRouter, createWebHashHistory } from 'vue-router'
import MobileLayout from '@/mobile/layouts/MobileLayout.vue'

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
          component: () => import('@/mobile/views/MobileHomeView.vue'),
          meta: { titleKey: 'mobile.home.pageTitle', tab: 'home' },
        },
        {
          path: 'notebook',
          name: 'mobile-notebook',
          component: () => import('@/mobile/views/MobileNotebookView.vue'),
          meta: { titleKey: 'portal.menu.notebook', tab: 'notebook' },
        },
        {
          path: 'notebook/:id',
          name: 'mobile-note-detail',
          component: () => import('@/mobile/views/MobileNoteDetailView.vue'),
          meta: { titleKey: 'notebook.title', hideTabBar: true },
        },
        {
          path: 'notebook/folder/:folderKey',
          name: 'mobile-notebook-folder',
          component: () => import('@/mobile/views/MobileNotebookFolderView.vue'),
          meta: { titleKey: 'mobile.notebook.folder', hideTabBar: true },
        },
        {
          path: 'todos',
          name: 'mobile-todos',
          component: () => import('@/mobile/views/MobileTodosView.vue'),
          meta: { titleKey: 'portal.menu.todos', tab: 'todos' },
        },
        {
          path: 'pomodoro',
          redirect: '/home',
        },
        {
          path: 'functions',
          name: 'mobile-functions',
          component: () => import('@/mobile/views/MobileFunctionsView.vue'),
          meta: { titleKey: 'portal.menu.functions', tab: 'more' },
        },
        {
          path: 'ecommerce',
          name: 'mobile-ecommerce',
          component: () => import('@/mobile/views/MobileEcommerceView.vue'),
          meta: { titleKey: 'ecommerce.workbenchTitle', tab: 'more', hideAppHeader: true },
        },
        {
          path: 'ecommerce/monthly-settlement',
          name: 'mobile-ecommerce-monthly-settlement',
          component: () => import('@/mobile/views/MobileEcommerceModuleView.vue'),
          meta: { titleKey: 'ecommerce.nav.monthlySettlement', hideTabBar: true, module: 'monthlySettlement' },
        },
        {
          path: 'ecommerce/orders',
          name: 'mobile-ecommerce-orders',
          component: () => import('@/mobile/views/MobileEcommerceModuleView.vue'),
          meta: { titleKey: 'ecommerce.nav.order', hideTabBar: true, module: 'order' },
        },
        {
          path: 'ecommerce/inventory',
          name: 'mobile-ecommerce-inventory',
          component: () => import('@/mobile/views/MobileEcommerceModuleView.vue'),
          meta: { titleKey: 'ecommerce.nav.inventory', hideTabBar: true, module: 'inventory' },
        },
        {
          path: 'ecommerce/products',
          name: 'mobile-ecommerce-products',
          component: () => import('@/mobile/views/MobileEcommerceModuleView.vue'),
          meta: { titleKey: 'ecommerce.nav.product', hideTabBar: true, module: 'product' },
        },
        {
          path: 'ecommerce/express',
          name: 'mobile-ecommerce-express',
          component: () => import('@/mobile/views/MobileEcommerceModuleView.vue'),
          meta: { titleKey: 'ecommerce.nav.express', hideTabBar: true, module: 'express' },
        },
        
        {
          path: 'ecommerce/shops',
          name: 'mobile-ecommerce-shops',
          component: () => import('@/mobile/views/MobileEcommerceModuleView.vue'),
          meta: { titleKey: 'ecommerce.nav.platformShop', hideTabBar: true, hideAppHeader: true, module: 'platformShop' },
        },
        {
          path: 'ecommerce/cartons',
          name: 'mobile-ecommerce-cartons',
          component: () => import('@/mobile/views/MobileCartonView.vue'),
          meta: { titleKey: 'ecommerce.nav.carton', hideTabBar: true, hideAppHeader: true },
        },
        {
          path: 'ecommerce/factory',
          name: 'mobile-ecommerce-factory',
          component: () => import('@/mobile/views/MobileFactoryView.vue'),
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
          component: () => import('@/mobile/views/MobileSettingsView.vue'),
          meta: { titleKey: 'portal.menu.settings', tab: 'more' },
        },
        {
          path: 'more',
          name: 'mobile-more',
          component: () => import('@/mobile/views/MobileMoreView.vue'),
          meta: { titleKey: 'mobile.nav.more', tab: 'more' },
        },
        {
          path: 'users',
          name: 'mobile-users',
          component: () => import('@/mobile/views/MobileUsersView.vue'),
          meta: { titleKey: 'portal.menu.permission', hideTabBar: true },
        },
        {
          path: 'note-design-preview',
          name: 'mobile-note-design-preview',
          component: () => import('@/mobile/views/MobileNoteDesignPreview.vue'),
          meta: { titleKey: '笔记设计预览', hideTabBar: true },
        },
        {
          path: 'carton-design-preview',
          name: 'mobile-carton-design-preview',
          component: () => import('@/mobile/views/MobileCartonDesignPreview.vue'),
          meta: { titleKey: '纸箱管理设计预览', hideTabBar: true },
        },
        {
          path: 'shop-design-preview',
          name: 'mobile-shop-design-preview',
          component: () => import('@/mobile/views/MobileShopDesignPreview.vue'),
          meta: { titleKey: '店铺管理设计预览', hideTabBar: true },
        },
        {
          path: 'express-design-preview',
          name: 'mobile-express-design-preview',
          component: () => import('@/mobile/views/MobileExpressDesignPreview.vue'),
          meta: { titleKey: '快递管理设计预览', hideTabBar: true },
        },
        {
          path: 'inventory-design-preview',
          name: 'mobile-inventory-design-preview',
          component: () => import('@/mobile/views/MobileInventoryDesignPreview.vue'),
          meta: { titleKey: '库存中心设计预览', hideTabBar: true },
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  if (to.path === '/' || to.path === '') {
    next({ path: '/home', replace: true })
    return
  }
  next()
})

export default router
