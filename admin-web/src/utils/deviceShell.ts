const SHELL_SESSION_KEY = 'ai-manager-app-shell-session'
/** 旧版持久 key，启动时自动清除以免挡住自动识别 */
const LEGACY_STORAGE_KEYS = ['ai-manager-app-shell-pref', 'ai-manager-app-shell']

/** 视口宽度 ≤ 此值时视为移动布局 */
export const MOBILE_MAX_WIDTH = 768

export type AppShell = 'pc' | 'mobile'

const SHELL_QUERY_KEY = 'shell'

function readQueryShell(): AppShell | null {
  if (typeof window === 'undefined') return null
  const fromQuery = new URLSearchParams(window.location.search).get(SHELL_QUERY_KEY)
  if (fromQuery === 'mobile' || fromQuery === 'pc') {
    return fromQuery
  }
  return null
}

/** 本次浏览器会话内的手动切换（关闭标签页后失效） */
function readSessionShell(): AppShell | null {
  if (typeof window === 'undefined') return null
  const stored = sessionStorage.getItem(SHELL_SESSION_KEY)
  if (stored === 'mobile' || stored === 'pc') {
    return stored
  }
  return null
}

/** 清除旧版 localStorage 偏好，避免一直锁在 PC 版 */
export function purgeLegacyShellStorage() {
  if (typeof window === 'undefined') return
  for (const key of LEGACY_STORAGE_KEYS) {
    localStorage.removeItem(key)
  }
}

/** UA / Client Hints */
export function isMobileUserAgent(): boolean {
  if (typeof navigator === 'undefined') return false

  const ua = navigator.userAgent
  if (/Android|webOS|iPhone|iPod|BlackBerry|IOMobile|Opera Mini|Mobile/i.test(ua)) {
    return true
  }
  if (/iPad/i.test(ua)) {
    return true
  }
  if (navigator.maxTouchPoints > 1 && /MacIntel|Macintosh/i.test(navigator.platform)) {
    return true
  }

  const uaData = (navigator as Navigator & { userAgentData?: { mobile?: boolean } }).userAgentData
  if (uaData?.mobile) {
    return true
  }

  return false
}

export function isMobileViewport(): boolean {
  if (typeof window === 'undefined') return false
  const width = Math.min(window.innerWidth, document.documentElement.clientWidth)
  return width <= MOBILE_MAX_WIDTH || window.matchMedia(`(max-width: ${MOBILE_MAX_WIDTH}px)`).matches
}

export function isMobileDevice(): boolean {
  return isMobileUserAgent() || isMobileViewport()
}

/** 纯自动识别（不含手动偏好） */
export function resolveAutoShell(): AppShell {
  return isMobileDevice() ? 'mobile' : 'pc'
}

export function hasManualShellOverride(): boolean {
  return readQueryShell() !== null || readSessionShell() !== null
}

/**
 * 解析应加载的壳层：
 * 1. URL ?shell=
 * 2. sessionStorage（本次会话手动切换）
 * 3. UA / 视口自动识别
 */
export function resolveAppShell(): AppShell {
  purgeLegacyShellStorage()

  const fromQuery = readQueryShell()
  if (fromQuery) {
    return fromQuery
  }

  const fromSession = readSessionShell()
  if (fromSession) {
    return fromSession
  }

  return resolveAutoShell()
}

/** 手动切换（仅当前会话有效，不写入 localStorage） */
export function setAppShellPreference(shell: AppShell) {
  sessionStorage.setItem(SHELL_SESSION_KEY, shell)
}

export function clearAppShellPreference() {
  sessionStorage.removeItem(SHELL_SESSION_KEY)
  purgeLegacyShellStorage()
}

export function applyShellDocumentClass(shell: AppShell) {
  document.documentElement.classList.toggle('is-mobile-shell', shell === 'mobile')
  document.documentElement.dataset.appShell = shell
}

export function prepareMobileEntryUrl() {
  ensureHashEntryUrl()
}

/** PC / 移动统一：始终使用 Hash URL（Nginx 静态部署） */
export function preparePcEntryUrl() {
  ensureHashEntryUrl()
}

function ensureHashEntryUrl() {
  if (typeof window === 'undefined') return
  if (window.location.hash.startsWith('#/')) {
    return
  }
  let path = window.location.pathname.replace(/^\//, '') || 'home'
  if (path === 'index.html') {
    path = 'home'
  }
  window.history.replaceState(null, '', `/#/${path}${window.location.search}`)
}

/**
 * 视口 / 方向变化时自动切换壳层（需整页重载以加载另一套 main_*）
 * 存在 ?shell= 或 session 手动偏好时不自动切换
 */
export function watchAppShellAutoSwitch(activeShell: AppShell) {
  if (typeof window === 'undefined' || hasManualShellOverride()) {
    return
  }

  let reloading = false

  const maybeSwitch = () => {
    if (reloading || hasManualShellOverride()) {
      return
    }
    const next = resolveAutoShell()
    if (next === activeShell) {
      return
    }
    reloading = true
    if (next === 'mobile' || next === 'pc') {
      ensureHashEntryUrl()
    }
    window.location.reload()
  }

  const mql = window.matchMedia(`(max-width: ${MOBILE_MAX_WIDTH}px)`)
  mql.addEventListener('change', maybeSwitch)
  window.addEventListener('orientationchange', () => {
    window.setTimeout(maybeSwitch, 150)
  })
}

export function detectAppShellSync(): AppShell {
  return resolveAppShell()
}

declare global {
  interface Window {
    __detectAppShell?: () => AppShell
    __watchAppShell?: (shell: AppShell) => void
  }
}

if (typeof window !== 'undefined') {
  window.__detectAppShell = detectAppShellSync
  window.__watchAppShell = watchAppShellAutoSwitch
}
