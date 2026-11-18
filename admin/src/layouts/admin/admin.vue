<template>
    <!-- 外层容器 -->
    <el-container class="admin-layout">
    
        <!-- 左边侧边栏 -->
        <el-aside class="sidebar-container" :width="menuStore.isCollapse ? '64px' : '250px'" transition-all>
            <AdminMenu></AdminMenu>
        </el-aside>
        
        <!-- 主容器 -->
        <el-container :class="{ 'sidebar-collapsed': menuStore.isCollapse }" class="main-container">
            <!-- 顶栏容器 -->
            <el-header class="fixed-header" :class="{ 'collapsed': menuStore.isCollapse }">
                <AdminHeader></AdminHeader>
            </el-header>
            
            <el-main class="main-with-fixed-header">
                <!-- 标签导航栏 -->
                <AdminTagList></AdminTagList>
                <!-- 主内容（根据路由动态展示不同页面） -->
                <router-view v-slot="{ Component }">
                    <Transition name="fade">
                        <!-- max 指定最多缓存 10 个组件 -->
                        <KeepAlive :max="10">
                            <component :is="Component"></component>
                        </KeepAlive>
                    </Transition>
                </router-view>
            </el-main>
            
            <!-- 底栏容器 -->
            <el-footer class="!p-0 fixed-footer-placeholder">
                <AdminFooter></AdminFooter>
            </el-footer>
        </el-container>
    </el-container>
</template>

<script setup>
import { useMenuStore } from '@/stores/menu'
// 引入组件
import AdminFooter from '@/components/AdminFooter.vue';
import AdminHeader from '@/components/AdminHeader.vue';
import AdminMenu from '@/components/AdminMenu.vue';
import AdminTagList from '@/components/AdminTagList.vue';

const menuStore = useMenuStore();
</script>

<style scoped>
.admin-layout {
    height: 100vh;
    overflow: hidden;
    background: var(--ymm-canvas);
}

.sidebar-container {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 1001;
    transition: width 0.3s;
    overflow-x: hidden; /* 防止水平滚动条 */
    border-right: 1px solid rgba(255, 255, 255, 0.1);
}

.main-container {
    margin-left: 250px;
    height: 100vh;
    flex-direction: column;
    transition: margin-left 0.3s;
    overflow: hidden; /* 防止滚动条 */
}

.main-container.sidebar-collapsed {
    margin-left: 64px;
}

.el-header {
    padding: 0!important;
    overflow: hidden; /* 防止头部出现滚动条 */
}

.fixed-header {
    position: fixed;
    top: 0;
    right: 0;
    z-index: 1000;
    width: calc(100% - 250px);
    transition: all 0.3s;
    left: 250px;
    overflow: hidden; /* 防止头部内容溢出 */
}

.fixed-header.collapsed {
    width: calc(100% - 64px);
    left: 64px;
}

.main-with-fixed-header {
    margin-top: 64px;
    height: calc(100vh - 124px); /* 64px header + 60px footer */
    overflow-y: auto;
    padding: 0;
    background:
      linear-gradient(rgba(40, 89, 166, 0.032) 1px, transparent 1px),
      var(--ymm-canvas);
    background-size: 100% 38px;
}

.fixed-footer-placeholder {
    height: 60px;
    flex-shrink: 0;
    overflow: hidden; /* 防止页脚出现滚动条 */
}

/* 内容区域过渡动画：淡入淡出效果 */
/* 刚开始进入时 */
.fade-enter-from {
    /* 透明度 */
    opacity: 0;
}

/* 刚开始结束 */
.fade-enter-to {
    opacity: 1;
}

/* 刚开始离开 */
.fade-leave-from {
  opacity: 1;
}

/* 离开已结束 */
.fade-leave-to {
  opacity: 0;
}

/* 离开进行中 */
.fade-leave-active {
    transition: opacity 0.18s ease;
}

/* 进入进行中 */
.fade-enter-active {
    transition: opacity 0.2s ease;
}
</style>
