import { createRouter, createWebHashHistory } from 'vue-router'

import AdminLayout from '@/layouts/AdminLayout.vue'



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

          name: 'ecommerce',

          component: () => import('@/views/EcommerceView.vue'),

          meta: { titleKey: 'ecommerce.title' },

        },

        {

          path: 'notebook',

          name: 'notebook',

          component: () => import('@/views/NotebookView.vue'),

          meta: { titleKey: 'portal.menu.notebook' },

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

          component: () => import('@/views/PlaceholderView.vue'),

          meta: { titleKey: 'portal.menu.deployDocs' },

        },

        {

          path: 'storage',

          name: 'storage',

          component: () => import('@/views/PlaceholderView.vue'),

          meta: { titleKey: 'portal.menu.storage' },

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

