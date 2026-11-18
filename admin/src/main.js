// 导入路由
import router from '@/router'
// 导入全局路由守卫
import '@/permission'
import {createPinia} from 'pinia'
import '@/assets/main.css'
import 'animate.css';
import 'nprogress/nprogress.css'
// 引入全局状态管理 Pinia
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

import { createApp } from 'vue'
import App from '@/App.vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 用户信息只保存在运行内存中，并清理旧版本遗留的持久化数据。
localStorage.removeItem('user')

const app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)
app.use(pinia)
// 状态管理必须先于路由和挂载注册，确保路由组件中的 Store 可用。
app.use(router)
app.mount('#app')
