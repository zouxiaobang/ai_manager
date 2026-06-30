import type { Component } from 'vue'
import {
  Collection,
  FolderOpened,
  List,
  Lock,
  Notebook,
  User,
} from '@element-plus/icons-vue'

export type FunctionItemKey =
  | 'notebook'
  | 'todos'
  | 'aiKnowledge'
  | 'library'
  | 'userCenter'
  | 'permission'

export interface FunctionItem {
  key: FunctionItemKey
  icon: Component
  route?: string
  accent: string
}

export const functionItems: FunctionItem[] = [
  {
    key: 'notebook',
    icon: Notebook,
    route: '/notebook',
    accent: '#409eff',
  },
  {
    key: 'todos',
    icon: List,
    route: '/todos',
    accent: '#10b981',
  },
  {
    key: 'userCenter',
    icon: User,
    route: '/user-center',
    accent: '#6366f1',
  },
  {
    key: 'permission',
    icon: Lock,
    route: '/users',
    accent: '#0ea5e9',
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
]
