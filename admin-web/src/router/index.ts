import { createRouter, createWebHashHistory } from 'vue-router'

import AdminLayout from '@/layouts/AdminLayout.vue'
import { ecommercePathForLegacyTab } from '@/data/ecommerce-nav'



const router = createRouter({

  // 与移动端一致使用 Hash，避免 Nginx 静态部署下 /home 等直链导致 PC 端白屏
  history: createWebHashHistory(import.meta.env.BASE_URL),

  routes: [

    {

      path: '/',

      component: AdminLayout,

      redirect: '/home',

      children: [

        {

          path: 'home',

          name: 'home',

          component: () => import('@/views/HomeView.vue'),

          meta: { titleKey: 'portal.menu.home' },

        },

        {

          path: 'functions',

          name: 'functions',

          component: () => import('@/views/FunctionListView.vue'),

          meta: { titleKey: 'portal.menu.functions' },

        },

        {

          path: 'pomodoro',

          name: 'pomodoro',

          component: () => import('@/views/PomodoroView.vue'),

          meta: { titleKey: 'pomodoro.title' },

        },

        {

          path: 'ecommerce',

          component: () => import('@/layouts/EcommerceLayout.vue'),

          meta: { titleKey: 'ecommerce.title' },

          children: [

            {

              path: '',

              name: 'ecommerce',

              component: () => import('@/views/ecommerce/EcommerceHomeView.vue'),

            },

            {

              path: 'monthly-settlement',

              name: 'ecommerce-monthly-settlement',

              component: () => import('@/views/ecommerce/EcommerceModuleView.vue'),

              meta: { module: 'monthlySettlement' },

            },

            {

              path: 'orders',

              name: 'ecommerce-orders',

              component: () => import('@/views/ecommerce/EcommerceModuleView.vue'),

              meta: { module: 'order' },

            },

            {

              path: 'express',

              name: 'ecommerce-express',

              component: () => import('@/views/ecommerce/EcommerceModuleView.vue'),

              meta: { module: 'express' },

            },

            {

              path: 'inventory',

              name: 'ecommerce-inventory',

              component: () => import('@/views/ecommerce/EcommerceModuleView.vue'),

              meta: { module: 'inventory' },

            },

            {

              path: 'products',

              name: 'ecommerce-products',

              component: () => import('@/views/ecommerce/EcommerceModuleView.vue'),

              meta: { module: 'product' },

            },

            {

              path: 'shops',

              name: 'ecommerce-shops',

              component: () => import('@/views/ecommerce/EcommerceModuleView.vue'),

              meta: { module: 'platformShop' },

            },

            {

              path: 'factories',

              name: 'ecommerce-factories',

              component: () => import('@/views/ecommerce/EcommerceModuleView.vue'),

              meta: { module: 'factory' },

            },

            {

              path: 'cartons',

              name: 'ecommerce-cartons',

              component: () => import('@/views/ecommerce/EcommerceModuleView.vue'),

              meta: { module: 'carton' },

            },

            {

              path: 'reports',

              name: 'ecommerce-reports',

              component: () => import('@/views/ecommerce/EcommercePlaceholderView.vue'),

            },

            {

              path: 'settings',

              name: 'ecommerce-settings',

              component: () => import('@/views/ecommerce/EcommerceSettingsView.vue'),

            },

            {

              path: 'manage',

              redirect: (to) => ecommercePathForLegacyTab(to.query.tab),

            },

          ],

        },

        {

          path: 'pixel-dog',

          name: 'pixel-dog',

          component: () => import('@/views/PlaceholderView.vue'),

          meta: { titleKey: 'functions.items.pixelDog.name' },

        },

        {

          path: 'notebook',

          name: 'notebook',

          component: () => import('@/views/NotebookView.vue'),

          meta: { titleKey: 'portal.menu.notebook' },

          beforeEnter: (to) => {

            if (to.query.tab === 'todos') {

              const filter = to.query.filter

              return {

                path: '/todos',

                query: typeof filter === 'string' ? { filter } : undefined,

              }

            }

          },

        },

        {

          path: 'todos',

          name: 'todos',

          component: () => import('@/views/TodosView.vue'),

          meta: { titleKey: 'portal.menu.todos' },

        },

        {

          path: 'user-center',

          name: 'user-center',

          component: () => import('@/views/PlaceholderView.vue'),

          meta: { titleKey: 'portal.menu.userCenter' },

        },

        {

          path: 'users',

          name: 'users',

          component: () => import('@/views/UserListView.vue'),

          meta: { titleKey: 'portal.menu.permission' },

        },

        {

          path: 'deploy-docs',

          name: 'deploy-docs',

          component: () => import('@/views/DeployCenterView.vue'),

          meta: { titleKey: 'portal.menu.deployCenter' },

        },

        {

          path: 'storage',

          name: 'storage',

          component: () => import('@/views/StorageCenterView.vue'),

          meta: { titleKey: 'portal.menu.storage' },

        },

        {

          path: 'image-space',

          name: 'image-space',

          component: () => import('@/views/ImageSpaceView.vue'),

          meta: { titleKey: 'portal.menu.imageSpace' },

        },

        {

          path: 'settings',

          name: 'settings',

          component: () => import('@/views/SettingsView.vue'),

          meta: { titleKey: 'portal.menu.settings' },

        },

        { path: 'assets', redirect: '/functions' },

        { path: 'audit', redirect: '/deploy-docs' },

      ],

    },

  ],

})



export default router

