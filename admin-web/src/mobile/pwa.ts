import { registerSW } from 'virtual:pwa-register'

/** 仅移动端入口注册 Service Worker */
export function setupMobilePwa() {
  registerSW({
    immediate: true,
  })
}
