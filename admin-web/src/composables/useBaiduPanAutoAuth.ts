import { computed, ref } from 'vue'
import type { Router } from 'vue-router'
import { ElMessage } from 'element-plus'
import i18n from '@/i18n'
import { fetchBaiduPanStatus, type BaiduPanAuthStatus } from '@/api/notebook'

const BAIDU_AUTH_REDIRECT_AT_KEY = 'baidu-pan-auth-redirect-at'
const BAIDU_AUTH_CANCELLED_KEY = 'baidu-pan-auth-cancelled'
const REDIRECT_COOLDOWN_MS = 60_000

const baiduPanStatus = ref<BaiduPanAuthStatus | null>(null)
const checking = ref(false)
let initialized = false
let routerRef: Router | null = null

const isBaiduPanAuthorized = computed(() => baiduPanStatus.value?.authorized === true)

const isBaiduAuthPending = computed(() => {
  if (isBaiduPanAuthorized.value) return false
  if (sessionStorage.getItem(BAIDU_AUTH_CANCELLED_KEY) === '1') return false
  const redirectedAt = sessionStorage.getItem(BAIDU_AUTH_REDIRECT_AT_KEY)
  if (!redirectedAt) return false
  return Date.now() - Number(redirectedAt) < REDIRECT_COOLDOWN_MS
})

function buildAuthorizeUrl(baseUrl: string, returnPath: string) {
  const state = encodeURIComponent(returnPath)
  const joiner = baseUrl.includes('?') ? '&' : '?'
  if (baseUrl.includes('state=')) return baseUrl
  return `${baseUrl}${joiner}state=${state}`
}

async function refreshBaiduPanStatus() {
  checking.value = true
  try {
    baiduPanStatus.value = await fetchBaiduPanStatus()
  } catch {
    baiduPanStatus.value = null
  } finally {
    checking.value = false
  }
  return baiduPanStatus.value
}

function clearBaiduAuthQuery() {
  if (!routerRef) return
  const route = routerRef.currentRoute.value
  const { baidu: _baidu, error: _error, ...rest } = route.query
  void routerRef.replace({ path: route.path, query: rest })
}

async function handleOAuthCallback() {
  if (!routerRef) return false
  const route = routerRef.currentRoute.value
  const { baidu, error } = route.query
  if (baidu === 'connected') {
    sessionStorage.removeItem(BAIDU_AUTH_REDIRECT_AT_KEY)
    sessionStorage.removeItem(BAIDU_AUTH_CANCELLED_KEY)
    await refreshBaiduPanStatus()
    ElMessage.success(i18n.global.t('notebook.baiduPanConnected'))
    clearBaiduAuthQuery()
    return true
  }
  if (baidu === 'error' || error) {
    sessionStorage.setItem(BAIDU_AUTH_CANCELLED_KEY, '1')
    sessionStorage.removeItem(BAIDU_AUTH_REDIRECT_AT_KEY)
    ElMessage.error(i18n.global.t('notebook.baiduPanAuthFailed'))
    clearBaiduAuthQuery()
    return true
  }
  return false
}

function redirectToBaiduAuthorize(returnPath?: string) {
  const url = baiduPanStatus.value?.authorizeUrl
  if (!url || !routerRef) return
  sessionStorage.removeItem(BAIDU_AUTH_CANCELLED_KEY)
  sessionStorage.setItem(BAIDU_AUTH_REDIRECT_AT_KEY, String(Date.now()))
  const path = returnPath ?? routerRef.currentRoute.value.fullPath
  window.location.href = buildAuthorizeUrl(url, path)
}

async function ensureBaiduPanAuth() {
  if (!routerRef) return

  if (await handleOAuthCallback()) return

  const status = await refreshBaiduPanStatus()
  if (!status) return

  if (status.authorized) {
    sessionStorage.removeItem(BAIDU_AUTH_REDIRECT_AT_KEY)
    sessionStorage.removeItem(BAIDU_AUTH_CANCELLED_KEY)
    return
  }

  if (!status.authorizeUrl) return
  if (sessionStorage.getItem(BAIDU_AUTH_CANCELLED_KEY) === '1') return

  const redirectedAt = sessionStorage.getItem(BAIDU_AUTH_REDIRECT_AT_KEY)
  if (redirectedAt && Date.now() - Number(redirectedAt) < REDIRECT_COOLDOWN_MS) {
    return
  }

  redirectToBaiduAuthorize()
}

export function initBaiduPanAutoAuth(router: Router) {
  if (initialized) return
  initialized = true
  routerRef = router

  router.afterEach(() => {
    void ensureBaiduPanAuth()
  })

  void ensureBaiduPanAuth()
}

export function useBaiduPanAutoAuth() {
  return {
    baiduPanStatus,
    checking,
    isBaiduPanAuthorized,
    isBaiduAuthPending,
    refreshBaiduPanStatus,
    redirectToBaiduAuthorize,
    ensureBaiduPanAuth,
  }
}
