import { afterEach } from 'vitest'
import { enableAutoUnmount } from '@vue/test-utils'

// 每个用例后自动卸载挂载的组件，避免 DOM 残留影响后续用例
enableAutoUnmount(afterEach)

// jsdom 缺失的浏览器 API，按需补齐（Element Plus / 组件常见依赖）
if (!window.matchMedia) {
  window.matchMedia = (query: string) =>
    ({
      matches: false,
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    }) as unknown as MediaQueryList
}

if (!window.ResizeObserver) {
  window.ResizeObserver = class ResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  } as unknown as typeof ResizeObserver
}
