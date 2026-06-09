import type { Component } from 'vue'
import {
  Chicken,
  Collection,
  Notebook,
  ShoppingCart,
  Timer,
  FolderOpened,
} from '@element-plus/icons-vue'

export type FunctionItemKey =
  | 'pomodoro'
  | 'notebook'
  | 'aiKnowledge'
  | 'library'
  | 'ecommerce'
  | 'pixelDog'

export interface FunctionItem {
  key: FunctionItemKey
  icon: Component
  route?: string
  accent: string
}

export const functionItems: FunctionItem[] = [
  {
    key: 'pomodoro',
    icon: Timer,
    route: '/pomodoro',
    accent: '#e6a23c',
  },
  {
    key: 'notebook',
    icon: Notebook,
    route: '/notebook',
    accent: '#409eff',
  },
  {
    key: 'aiKnowledge',
    icon: Collection,
    accent: '#67c23a',
  },
  {
    key: 'library',
    icon: FolderOpened,
    accent: '#909399',
  },
  {
    key: 'ecommerce',
    icon: ShoppingCart,
    route: '/ecommerce',
    accent: '#f56c6c',
  },
  {
    key: 'pixelDog',
    icon: Chicken,
    accent: '#a855f7',
  },
]
