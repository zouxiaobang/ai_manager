import type { FunctionItemKey } from './function-items'



/** 模块视觉：图标文件名（不含路径）+ 底部色条 */

export interface ModuleVisual {

  icon: string

  barColor: string

}



export const moduleVisuals: Record<FunctionItemKey, ModuleVisual> = {

  pomodoro: { icon: 'pomodoro', barColor: '#e85d4c' },

  notebook: { icon: 'notebook', barColor: '#3b82f6' },

  todos: { icon: 'todos', barColor: '#10b981' },

  aiKnowledge: { icon: 'ai-knowledge', barColor: '#22c55e' },

  library: { icon: 'library', barColor: '#6b7280' },

  ecommerce: { icon: 'ecommerce', barColor: '#f59e0b' },

  pixelDog: { icon: 'pixel-dog', barColor: '#8b5cf6' },

}



/** 首页轮播展示顺序（贴近参考图：番茄钟 / 笔记本 / 电商 / 报表 / 设置） */

export const homeModuleCarouselKeys: FunctionItemKey[] = [

  'pomodoro',

  'notebook',

  'todos',

  'ecommerce',

  'aiKnowledge',

  'library',

]



export type QuickModuleKey = FunctionItemKey | 'report' | 'settings'



export interface QuickModuleDef {

  key: QuickModuleKey

  icon: string

  barColor: string

  route?: string

  i18nKey?: FunctionItemKey

  labelKey?: string

}



/** 参考图底部 5 个模块入口（含报表、设置） */

export const quickModules: QuickModuleDef[] = [

  { key: 'pomodoro', icon: 'pomodoro', barColor: '#e85d4c', route: '/pomodoro', i18nKey: 'pomodoro' },

  { key: 'notebook', icon: 'notebook', barColor: '#3b82f6', route: '/notebook', i18nKey: 'notebook' },

  { key: 'ecommerce', icon: 'ecommerce', barColor: '#f59e0b', route: '/ecommerce', i18nKey: 'ecommerce' },

  { key: 'report', icon: 'report', barColor: '#22c55e', route: '/pomodoro', labelKey: 'portal.dashboard.warRoom.reportModule' },

  { key: 'settings', icon: 'settings', barColor: '#9ca3af', route: '/settings', labelKey: 'portal.menu.settings' },

]


