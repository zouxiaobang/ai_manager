import { getData, putData, postData, deleteData } from './request'

export interface PixelDogState {
  id: number
  level: number
  xp: number
  xpNext: number
  bond: number
  emotion: number
  lastInteractTs: number
  lastGreetTs: number
  status: number
  unlockedItems: number
  equippedItems: number
}

export interface PixelDogStateVO {
  id: number
  level: number
  xp: number
  xpNext: number
  bond: number
  emotion: number
  lastInteractTs: number
  lastGreetTs: number
  status: number
  unlockedItems: number
  equippedItems: number
}

export interface PixelDogUpdateRequest {
  level?: number
  xp?: number
  xpNext?: number
  bond?: number
  emotion?: number
  status?: number
  unlockedItems?: number
  equippedItems?: number
}

export interface PixelDogXpRequest {
  action: string
  amount: number
}

export function fetchDogState() {
  return getData<PixelDogStateVO>('/api/pixel-dog/state')
}

export function updateDogState(state: PixelDogUpdateRequest) {
  return putData<PixelDogStateVO>('/api/pixel-dog/state', state)
}

export function addDogXp(action: string, amount: number) {
  return postData<PixelDogStateVO>(`/api/pixel-dog/xp?action=${action}&amount=${amount}`)
}

// ========== 物品管理 ==========

export interface PixelDogItemVO {
  id: number
  name: string
  icon: string
  color: string
  requireLevel: number
  sortOrder: number
  shape: number
}

export interface PixelDogItemRequest {
  name?: string
  icon?: string
  color?: string
  requireLevel?: number
  sortOrder?: number
  shape?: number
}

export function fetchDogItems() {
  return getData<PixelDogItemVO[]>('/api/pixel-dog/items')
}

export function createDogItem(item: PixelDogItemRequest) {
  return postData<PixelDogItemVO>('/api/pixel-dog/items', item)
}

export function updateDogItem(id: number, item: PixelDogItemRequest) {
  return putData<PixelDogItemVO>(`/api/pixel-dog/items/${id}`, item)
}

export async function deleteDogItem(id: number) {
  await deleteData(`/api/pixel-dog/items/${id}`)
}
