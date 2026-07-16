import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

;(async () => {
  const MOBILE_UI_VERSION_KEY = 'mobile-ui-version'

  function resolveMobileUIVersion(): 'v1' | 'v2' {
    if (typeof localStorage === 'undefined') return 'v2'
    const saved = localStorage.getItem(MOBILE_UI_VERSION_KEY)
    if (saved === 'v1' || saved === 'v2') return saved
    localStorage.setItem(MOBILE_UI_VERSION_KEY, 'v2')
    return 'v2'
  }

  if (resolveMobileUIVersion() === 'v2') {
    const [{ default: V2App }, { default: V2Router }, { default: i18n }, { useAppStore }] = await Promise.all([
      import('./App_mobileV2.vue'),
      import('./router/mobile-v2'),
      import('./i18n'),
      import('./stores/app'),
    ])
    await import('./mobile-v2/styles/mobile-v2.scss')

    const app = createApp(V2App)
    app.use(createPinia())
    app.use(ElementPlus)
    app.use(i18n)
    app.use(V2Router)

    const appStore = useAppStore()
    appStore.initTheme()
    appStore.initLocale(i18n)
    appStore.initMobileUIVersion()
    appStore.initPrimaryColor()

    app.mount('#app')
  } else {
    const [{ default: App }, { default: router }, { default: i18n }, { useAppStore }] = await Promise.all([
      import('./App_mobile.vue'),
      import('./router/mobile'),
      import('./i18n'),
      import('./stores/app'),
    ])
    await Promise.all([
      import('./mobile/styles/mobile.scss'),
      import('./styles/index.scss'),
      import('./mobile/pwa').then(m => m.setupMobilePwa()),
      import('./mobile/utils/inputViewport').then(m => m.setupMobileInputViewportFix()),
      import('./mobile/utils/homeBackGuard').then(m => m.setupHomeBackGuard(router)),
    ])

    const app = createApp(App)
    const pinia = createPinia()

    app.use(pinia)
    app.use(router)
    app.use(i18n)
    app.use(ElementPlus)

    const appStore = useAppStore()
    appStore.initTheme()
    appStore.initLocale(i18n)
    appStore.initMobileHomeTheme()

    app.mount('#app')
  }
})()
