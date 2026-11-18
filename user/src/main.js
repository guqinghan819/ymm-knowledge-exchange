import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import 'element-plus/dist/index.css'
import 'nprogress/nprogress.css'

import App from './App.vue'
import router from './router'
import { useUserStore } from './stores/user'
import { showPageLoading, hidePageLoading } from './utils/utils'

// 用户信息只保存在运行内存中，并清理旧版本遗留的持久化数据。
localStorage.removeItem('user')

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)

async function bootstrap() {
  showPageLoading()
  try {
    await useUserStore(pinia).restoreUserInfo()
  } catch (error) {
    // Axios 已提示请求失败，仍允许页面正常打开和重新登录。
    console.error('恢复当前用户信息失败:', error)
  } finally {
    // 在恢复完成后安装路由和挂载页面，避免发布页提前读取空的 Store。
    app.use(router)
    app.mount('#app')
    hidePageLoading()
  }
}

bootstrap()
