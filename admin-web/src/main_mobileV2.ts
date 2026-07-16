import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import i18n from '@/i18n'
import router from '@/router/mobile-v2'
import App from '@/App_mobileV2.vue'
import { useAppStore } from '@/stores/app'
import '@/styles/fonts.scss'
import '@/mobile-v2/styles/mobile-v2.scss'
import '@/styles/index.scss'
import '@/styles/fonts.scss'

const app = createApp(App)

app.use(createPinia())
app.use(ElementPlus)
app.use(i18n)
app.use(router)

const appStore = useAppStore()
appStore.initTheme()
appStore.initLocale(i18n)
appStore.initMobileUIVersion()
appStore.initPrimaryColor()

app.mount('#app')
