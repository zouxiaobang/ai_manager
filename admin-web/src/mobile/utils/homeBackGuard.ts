import type { Router } from 'vue-router'

function isHomeRoute(name: string | symbol | null | undefined) {
  return name === 'mobile-home'
}

/** 首页不响应系统返回，避免 PWA / 浏览器后退离开工作台 */
export function setupHomeBackGuard(router: Router) {
  const pinHomeAsFirst = () => {
    if (!isHomeRoute(router.currentRoute.value.name)) return
    window.history.replaceState({ mobileHomeGuard: true }, '', window.location.href)
  }

  router.afterEach((to) => {
    if (isHomeRoute(to.name)) {
      pinHomeAsFirst()
    }
  })

  window.addEventListener('popstate', (event) => {
    if (isHomeRoute(router.currentRoute.value.name)) {
      if (!event.state || !event.state.mobileHomeGuard) {
        window.history.replaceState({ mobileHomeGuard: true }, '', window.location.href)
      }
    }
  })

  router.beforeEach((to, from) => {
    if (isHomeRoute(to.name) && isHomeRoute(from.name)) {
      return false
    }
  })
}

export function isMobileHomeRoute(routeName: string | symbol | null | undefined) {
  return isHomeRoute(routeName)
}
