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
          meta: { titleKey: 'portal.menu.home', tab: 'home' },
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
          path: 'todos',
          name: 'mobile-todos',
          component: () => import('@/mobile/views/MobileTodosView.vue'),
          meta: { titleKey: 'notebook.tabs.todos', tab: 'todos' },
        },
        {
          path: 'pomodoro',
          name: 'mobile-pomodoro',
          component: () => import('@/mobile/views/MobilePomodoroView.vue'),
          meta: { titleKey: 'pomodoro.title', tab: 'pomodoro' },
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
          meta: { titleKey: 'ecommerce.title', tab: 'more' },
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
      ],
    },
  ],
})

export default router
