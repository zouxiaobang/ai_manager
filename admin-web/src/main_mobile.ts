import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

import App from './App_mobile.vue'
import router from './router/mobile'
import i18n from './i18n'
import { useAppStore } from './stores/app'

import './styles/index.scss'
import './mobile/styles/mobile.scss'
import { setupMobilePwa } from './mobile/pwa'
import { setupMobileInputViewportFix } from './mobile/utils/inputViewport'
import { setupHomeBackGuard } from './mobile/utils/homeBackGuard'

setupMobilePwa()
setupMobileInputViewportFix()

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(i18n)
app.use(ElementPlus)

setupHomeBackGuard(router)

const appStore = useAppStore()
appStore.initTheme()
appStore.initLocale(i18n)
appStore.initMobileHomeTheme()

app.mount('#app')
