
import { createRouter, createWebHistory } from 'vue-router'
import Forbidden from '@/pages/admin/forbidden.vue'
import Login from '@/pages/admin/login.vue'
import Admin from '@/layouts/admin/admin.vue'
import Index from '@/pages/admin/index.vue'
import UserList from '@/pages/admin/user-manage.vue'
import RoleList from '@/pages/admin/role-manage.vue'
import PermissionList from '@/pages/admin/permission-manage.vue'
import ActicleList from '@/pages/admin/article-manage.vue'
import ChannelList from '@/pages/admin/channel-manage.vue'
import TopicList from '@/pages/admin/topic-manage.vue'
// 统一在这里声明所有路由
const routes = [
    { path: '/admin/forbidden', component: Forbidden, meta: { title: '无权访问' } },
    {
        path: '/admin/login', // 后台登录页
        component: Login, // 对应组件
        meta: { // meta 信息
            title: '雨绵绵 登陆' // 页面标题
        }
    },
    {
        path: "/admin", // 后台首页
        component: Admin, // 对应 admin.vue 布局文件
        // 使用到 admin.vue 布局的，都需要放置在其子路由下面
        children: [
            {
                path: "/admin/index",
                component: Index, // 主内容区域，具体需要渲染的页面
                meta: {
                    title: '仪表盘'
                }
            },
            {
                path: "/admin/user/list",
                component: UserList,
                meta: {
                    title: '用户列表'
                }
            },
            {
                path: "/admin/user/role",
                component: RoleList,
                meta: {
                    title: '角色管理'
                }
            },
            {
                path: "/admin/user/permission",
                component: PermissionList,
                meta: {
                    title: '权限管理'
                }
            },
            {
                path: "/admin/content/article",
                component: ActicleList,
                meta: {
                    title: '文章管理'
                }
            },
            {
                path: "/admin/content/channel",
                component: ChannelList,
                meta: {
                    title: '频道管理'
                }
            },
            {
                path: "/admin/content/topic",
                component: TopicList,
                meta: {
                    title: '话题管理'
                }
            },
        ]
        
    }
]

// 创建路由
const router = createRouter({
    // 指定路由的历史管理方式，history 模式
    history: createWebHistory(),
    // routes: routes 的缩写
    routes, 
})

// ES6 模块导出语句，它用于将 router 对象导出，以便其他文件可以导入和使用这个对象
export default router
