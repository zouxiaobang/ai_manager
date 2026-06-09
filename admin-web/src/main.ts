import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

import App from './App.vue'
import router from './router'
import i18n from './i18n'
import { useAppStore } from './stores/app'

import './styles/index.scss'
import { setupDraggableMessageBox } from './plugins/draggableDialog'

const app = createApp(App)
const pinia = createPinia()

setupDraggableMessageBox()

app.use(pinia)
app.use(router)
app.use(i18n)
app.use(ElementPlus)

const appStore = useAppStore()
appStore.initTheme()
appStore.initLocale(i18n)

app.mount('#app')
