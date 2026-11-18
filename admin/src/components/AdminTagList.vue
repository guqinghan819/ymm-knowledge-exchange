<template>
    <!-- 左边：标签导航栏 -->
    <div class="fixed top-[64px] h-[44px] px-2 right-0 z-50 flex items-center tag-navbar"
         :style="{ left: menuStore.isCollapse ? '64px' : '250px' }">
        <el-tabs v-model="activeTab" type="card" class="demo-tabs flex-1" @tab-remove="removeTab" @tab-change="tabChange" style="min-width: 10px;">
            <el-tab-pane 
                v-for="item in tabList" 
                :key="item.path" 
                :label="item.title" 
                :name="item.path" 
                :closable="item.path !== '/admin/index'">
            </el-tab-pane>
        </el-tabs>

        <!-- 右侧下拉菜单 -->
        <span class="ml-auto">
            <el-dropdown>
                <span class="el-dropdown-link tag-menu-trigger">
                    <el-icon>
                        <arrow-down />
                    </el-icon>
                </span>
                <template #dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item @click="closeOtherTabs">关闭其他</el-dropdown-item>
                        <el-dropdown-item divided @click="closeAllTabs">关闭全部</el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </span>
    </div>
    <div class="h-[44px]"></div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useMenuStore } from '@/stores/menu'
import { useRoute, useRouter } from 'vue-router'
import { setTabList, getTabList } from '@/utils/tag-list'
const route = useRoute()
const router = useRouter()
const menuStore = useMenuStore()

// 添加 Tab 标签页
function addTab(tab) {
    // 标签是否不存在
    let isTabNotExisted = tabList.value.findIndex(item => item.path == tab.path) == -1
    // 如果不存在
    if (isTabNotExisted) {
        // 添加标签
        tabList.value.push(tab)
    }
    // 存储 tabList 到 cookie 中
    setTabList(tabList.value)
}

function initTabList() {
    // 从 cookie 中获取缓存起来的标签导航栏数据
    let tabs = getTabList()
    // 若不为空，则赋值
    if (tabs) {
        tabList.value = tabs
    }
}

// 当前被选中的 tab
const activeTab = ref(route.path)
const tabList = ref([
    {
        title: '仪表盘',
        path: "/admin/index"
    }
])

// 在组件挂载时初始化标签列表
onMounted(() => {
    initTabList()
    // 确保当前路由在标签列表中
    activeTab.value = route.path
    const exists = tabList.value.some(tab => tab.path === route.path)
    if (!exists) {
        // 添加当前路由到标签列表
        tabList.value.push({
            title: route.meta?.title || route.name || route.path,
            path: route.path
        })
    }
})

// 标签页切换事件
const tabChange = (path) => {
    // 设置被激活的 Tab 标签
    activeTab.value = path
    // 路由跳转
    router.push(path)
}

// 监听路由变化，添加标签
watch(route, (to) => {
    activeTab.value = to.path
    // 检查标签是否已存在
    const exists = tabList.value.some(tab => tab.path === to.path)
    if (!exists) {
        // 添加新标签，这里可以根据路由动态获取标题
        tabList.value.push({
            title: to.meta?.title || to.name || to.path,
            path: to.path
        })
        // 保存到cookie
        setTabList(tabList.value)
    }
})

// 删除标签
const removeTab = (targetPath) => {
    // 不能关闭首页
    if (targetPath === '/admin/index') return
    
    // 找到要删除的标签索引
    const index = tabList.value.findIndex(tab => tab.path === targetPath)
    if (index === -1) return
    
    // 如果关闭的是当前激活的标签，则需要激活另一个标签
    if (activeTab.value === targetPath) {
        // 如果有下一个标签就激活下一个，否则激活上一个
        if (index < tabList.value.length - 1) {
            activeTab.value = tabList.value[index + 1].path
            router.push(tabList.value[index + 1].path)
        } else if (index > 0) {
            activeTab.value = tabList.value[index - 1].path
            router.push(tabList.value[index - 1].path)
        } else {
            // 如果这是最后一个标签，激活首页
            activeTab.value = '/admin/index'
            router.push('/admin/index')
        }
    }
    
    // 从数组中移除标签
    tabList.value.splice(index, 1)
    // 保存到cookie
    setTabList(tabList.value)
}

// 关闭其他标签
const closeOtherTabs = () => {
    tabList.value = tabList.value.filter(tab => 
        tab.path === '/admin/index' || tab.path === activeTab.value
    )
    // 保存到cookie
    setTabList(tabList.value)
}

// 关闭所有标签（除了首页）
const closeAllTabs = () => {
    tabList.value = tabList.value.filter(tab => tab.path === '/admin/index')
    activeTab.value = '/admin/index'
    router.push('/admin/index')
    // 保存到cookie
    setTabList(tabList.value)
}
</script>

<style>
.tag-navbar {
    transition: left 0.3s ease;
    border-bottom: 1px solid var(--ymm-line);
    background: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(14px);
    overflow: hidden; /* 防止标签栏出现滚动条 */
}

.el-tabs__item {
    font-size: 12px;
    border: 1px solid var(--ymm-line)!important;
    border-radius: 7px!important;
    color: var(--ymm-muted);
    transition: color 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.el-tabs--card>.el-tabs__header .el-tabs__item {
    margin-left: 0.1rem!important;
    margin-right: 0.1rem!important;
}

.el-tabs__item.is-active {
    border-color: #b9c9df !important;
    background-color: var(--ymm-primary-soft)!important;
    color: var(--ymm-primary);
    font-weight: 700;
}

.el-tabs__item.is-active::before {
    content: "";
    background-color: var(--ymm-primary);
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    position: relative;
    margin-right: 4px;
}

.el-tabs__item:hover {
    color: var(--ymm-primary);
    background-color: #f5f8fc;
    border-color: #b9c9df !important;
}

.el-tabs__item.is-active:hover {
    background-color: var(--ymm-primary-soft)!important;
}

.el-tabs {
    height: 32px;
    overflow: hidden; /* 防止tabs出现滚动条 */
}

.el-tabs__header {
    margin-bottom: 0;
    overflow: hidden; /* 防止header出现滚动条 */
}

.el-tabs--card>.el-tabs__header .el-tabs__nav {
    border: 0;
}

.el-tabs--card>.el-tabs__header .el-tabs__item {
    height: 32px;
    line-height: 32px;
    border: 0;
    background: #fff;
}

.el-tabs--card>.el-tabs__header {
    border: 0;
}

.el-tabs__nav-prev, .el-tabs__nav-next {
    line-height: 35px;
}

.is-disabled {
    cursor: not-allowed;
    color: #d1d5db;
}

/* 下拉菜单样式 */
.el-dropdown-link:focus {
    outline: none;
}

.tag-menu-trigger {
    width: 32px;
    height: 32px;
    display: inline-grid;
    place-items: center;
    border-radius: 8px;
    color: var(--ymm-muted);
    cursor: pointer;
    transition: color 0.18s ease, background 0.18s ease;
}

.tag-menu-trigger:hover {
    color: var(--ymm-primary);
    background: var(--ymm-primary-soft);
}

.el-dropdown-menu {
    border-radius: 8px;
    padding: 5px 0;
}

.el-dropdown-menu__item {
    font-size: 12px;
    padding: 8px 16px;
}

.el-dropdown-menu__item:hover {
    color: var(--ymm-primary);
    background-color: var(--ymm-primary-soft);
}

.el-dropdown-menu__item:not(.is-divided):last-child {
    border-radius: 0 0 4px 4px;
}

.el-dropdown-menu__item.is-divided {
    border-top: 1px solid #ebeef5;
    margin-top: 6px;
    padding-top: 6px;
}

.el-dropdown-menu__item.is-divided:before {
    display: none;
}
</style>
