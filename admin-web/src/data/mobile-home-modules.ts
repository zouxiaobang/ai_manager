/** 移动端首页功能宫格（不含番茄钟） */
export interface MobileHomeModule {
  key: string
  icon: string
  barColor: string
  nameKey: string
  descKey: string
  route?: string
  /** 无移动端路由时跳转桌面版对应路径 */
  desktopPath?: string
  searchKeys?: string[]
}

export const mobileHomeModules: MobileHomeModule[] = [
  {
    key: 'notebook',
    icon: 'notebook',
    barColor: '#3b82f6',
    nameKey: 'functions.items.notebook.name',
    descKey: 'functions.items.notebook.desc',
    route: '/notebook',
    searchKeys: ['notebook', '笔记'],
  },
  {
    key: 'todos',
    icon: 'todos',
    barColor: '#10b981',
    nameKey: 'functions.items.todos.name',
    descKey: 'functions.items.todos.desc',
    route: '/todos',
    searchKeys: ['todos', '待办'],
  },
  {
    key: 'ecommerce',
    icon: 'ecommerce',
    barColor: '#f59e0b',
    nameKey: 'functions.items.ecommerce.name',
    descKey: 'functions.items.ecommerce.desc',
    route: '/ecommerce',
    searchKeys: ['ecommerce', '电商'],
  },
  {
    key: 'pixelDog',
    icon: 'pixel-dog',
    barColor: '#8b5cf6',
    nameKey: 'functions.items.pixelDog.name',
    descKey: 'functions.items.pixelDog.desc',
    desktopPath: '/pixel-dog',
    searchKeys: ['pixelDog', '像素'],
  },
  {
    key: 'userCenter',
    icon: 'user-center',
    barColor: '#6366f1',
    nameKey: 'functions.items.userCenter.name',
    descKey: 'functions.items.userCenter.desc',
    desktopPath: '/user-center',
    searchKeys: ['userCenter', '用户'],
  },
  {
    key: 'permission',
    icon: 'permission',
    barColor: '#0ea5e9',
    nameKey: 'functions.items.permission.name',
    descKey: 'functions.items.permission.desc',
    route: '/users',
    searchKeys: ['permission', '权限'],
  },
  {
    key: 'storage',
    icon: 'library',
    barColor: '#6b7280',
    nameKey: 'portal.menu.storage',
    descKey: 'storageCenter.subtitle',
    desktopPath: '/storage',
    searchKeys: ['storage', '存储'],
  },
  {
    key: 'settings',
    icon: 'settings',
    barColor: '#9ca3af',
    nameKey: 'portal.menu.settings',
    descKey: 'mobile.home.settingsDesc',
    route: '/settings',
    searchKeys: ['settings', '设置'],
  },
]
