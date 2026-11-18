<template>
  <div class="fixed-sidebar">
    <!-- Logo 区域 -->
    <div class="logo-container">
      <div class="logo-wrapper">
        <img :src="adminLogo" class="admin-logo" alt="雨绵绵后台" />
        <h1 
          v-if="!isCollapse"
          :class="{
            'logo-text': true,
            'logo-expanded': !isCollapse
          }"
        >
          雨绵绵<span>管理台</span>
        </h1>
      </div>
    </div>
    
    <!-- 菜单区域 -->
    <el-menu
      class="custom-menu"
      text-color="#c8d4e6"
      active-text-color="#ffffff"
      background-color="#17233b"
      :collapse="isCollapse" 
      :collapse-transition="false"
      :default-active="activeMenuIndex"
      :default-openeds="openedSubMenus"
      unique-opened
      @select="handleMenuSelect"
    >
      <!-- 仪表盘 -->
      <el-menu-item v-if="menuList[0]?.path === '/admin/index'" :index="menuList[0].index" :route="menuList[0].path">
        <el-icon><component :is="menuList[0].icon" /></el-icon>
        <span class="menu-item-text">{{ menuList[0].title }}</span>
      </el-menu-item>
      
      <!-- 其他菜单项 -->
      <template v-for="item in (menuList[0]?.path === '/admin/index' ? menuList.slice(1) : menuList)" :key="item.index">
        <el-sub-menu 
          v-if="item.children" 
          :index="item.index"
        >
          <template #title>
            <el-icon><component :is="item.icon" /></el-icon>
            <span class="submenu-title-text">{{ item.title }}</span>
          </template>
          <el-menu-item 
            v-for="subItem in item.children" 
            :key="subItem.index" 
            :index="subItem.index"
            :route="subItem.path"
            class="submenu-item"
          >
            <span class="submenu-item-text">{{ subItem.title }}</span>
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item v-else :index="item.index" :route="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span class="menu-item-text">{{ item.title }}</span>
        </el-menu-item>
      </template>
    </el-menu>
    
    <!-- 用户信息区域 -->
    <div class="user-info">
      <el-avatar :size="30" :src="userStore.userInfo.avatar" />
      <span 
        :class="{
          'ml-2 text-sm user-name': true,
          'name-expanded': !isCollapse,
          'name-collapsed': isCollapse
        }"
      >
        {{ userStore.userInfo.nickname }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, shallowRef, computed } from 'vue'
import { House, User, Document } from '@element-plus/icons-vue'
import { useMenuStore } from '@/stores/menu'
import { useUserStore } from '@/stores/user'
import { useRoute, useRouter } from 'vue-router'
import { canAccessPage } from '@/utils/authorization'
import adminLogo from '@/assets/admin.png'

const route = useRoute()
const router = useRouter()
const menuStore = useMenuStore()
const userStore = useUserStore()

// 是否折叠
const isCollapse = computed(() => menuStore.isCollapse)

// 当前激活的菜单索引
const activeMenuIndex = computed(() => {
  const currentPath = route.path
  for (const item of menuList.value) {
    if (item.path === currentPath) {
      return item.index
    }
    if (item.children) {
      for (const child of item.children) {
        if (child.path === currentPath) {
          return child.index
        }
      }
    }
  }
  return '0' // 默认返回仪表盘索引
})

// 需要展开的子菜单
const openedSubMenus = computed(() => {
  const currentPath = route.path
  const opened = []
  for (const item of menuList.value) {
    if (item.children) {
      for (const child of item.children) {
        if (child.path === currentPath) {
          opened.push(item.index)
          break
        }
      }
    }
  }
  return opened
})

// 菜单数据
const allMenus = [
  {
    index: '0',
    title: '仪表盘',
    icon: shallowRef(House),
    path: '/admin/index'
  },
  {
    index: '1',
    title: '用户管理',
    icon: shallowRef(User),
    children: [
      { index: '1-1', title: '用户列表', path: '/admin/user/list' },
      { index: '1-2', title: '角色管理', path: '/admin/user/role' },
      { index: '1-3', title: '权限管理', path: '/admin/user/permission' }
    ]
  },
  {
    index: '2',
    title: '内容管理',
    icon: shallowRef(Document),
    children: [
      { index: '2-1', title: '文章管理', path: '/admin/content/article' },
      { index: '2-2', title: '频道管理', path: '/admin/content/channel' },
      { index: '2-3', title: '话题管理', path: '/admin/content/topic' }
    ]
  },
  // {
  //   index: '3',
  //   title: '系统管理',
  //   icon: shallowRef(Setting),
  //   children: [
  //     { index: '3-1', title: '系统设置', path: '/admin/system/config' },
  //     { index: '3-2', title: '日志管理', path: '/admin/system/log' }
  //   ]
  // }
]
const menuList = computed(() => allMenus.map(item => {
  if (!item.children) return canAccessPage(item.path, userStore.userInfo.permissionKeys, userStore.userInfo.roleKeys) ? item : null
  const children = item.children.filter(child => canAccessPage(child.path, userStore.userInfo.permissionKeys, userStore.userInfo.roleKeys))
  return children.length ? { ...item, children } : null
}).filter(Boolean))

// 处理菜单选择事件
const handleMenuSelect = (index) => {
  // 根据索引找到对应的路径
  const findPathByIndex = (items, targetIndex) => {
    for (const item of items) {
      if (item.index === targetIndex) {
        return item.path
      }
      if (item.children) {
        const childPath = findPathByIndex(item.children, targetIndex)
        if (childPath) {
          return childPath
        }
      }
    }
    return null
  }

  const path = findPathByIndex(menuList.value, index)
  if (path && path !== route.path) {
    router.push(path)
  }
}
</script>

<style scoped>
.fixed-sidebar {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 防止滚动条出现 */
  color: #fff;
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    #17233b;
  background-size: 100% 36px;
}

.user-info {
  flex-shrink: 0;
  min-height: 60px;
  padding: 0 17px;
  display: flex;
  align-items: center;
  border-top: 1px solid rgba(255, 255, 255, 0.11);
  color: #c8d4e6;
  background: rgba(9, 20, 42, 0.2);
}

.submenu-item {
  transition: color 0.18s ease, background-color 0.18s ease;
}

.logo-container {
  flex: 0 0 64px;
  height: 64px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.11);
}

.admin-logo {
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 9px;
  object-fit: cover;
  filter: invert(1);
  opacity: 0.92;
}

.custom-menu {
  flex: 1;
  border-right: 0;
  padding: 16px 10px;
  overflow-y: auto;
}

.custom-menu :deep(.el-menu-item),
.custom-menu :deep(.el-sub-menu__title) {
  height: 46px;
  margin-bottom: 4px;
  border-radius: 9px;
  line-height: 46px;
}

.custom-menu :deep(.el-menu-item:hover),
.custom-menu :deep(.el-sub-menu__title:hover) {
  color: #fff !important;
  background: rgba(255, 255, 255, 0.08) !important;
}

.custom-menu :deep(.el-menu-item.is-active) {
  color: #fff !important;
  background: #2859a6 !important;
  box-shadow: inset 3px 0 0 #91b4ec;
}

.custom-menu :deep(.el-sub-menu .el-menu) {
  background: rgba(8, 18, 36, 0.2) !important;
}

.custom-menu :deep(.submenu-item) {
  min-width: 0;
  padding-left: 50px !important;
}

/* Logo 文字过渡效果 */
.logo-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 11px;
  width: 100%;
  height: 100%;
  padding: 0 15px;
  overflow: hidden;
}

.logo-text {
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
  white-space: nowrap;
  text-align: center;
  transform-origin: center;
  color: #fff;
  font-family: var(--ymm-font-serif);
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.logo-text span {
  margin-left: 7px;
  color: #9fb6d8;
  font-family: var(--ymm-font-sans);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0;
}

.logo-expanded {
  opacity: 1;
  transform: scale(1);
}

.logo-collapsed {
  opacity: 1;
  transform: scale(1);
}

/* 用户名过渡效果 */
.user-name {
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
  transform-origin: left center;
  white-space: nowrap;
  overflow: hidden;
}

.name-expanded {
  opacity: 1;
  transform: translateX(0);
  width: auto;
}

.name-collapsed {
  opacity: 0;
  transform: translateX(-10px) scale(0.8);
  width: 0;
  height: 0;
  padding: 0 !important;
  margin: 0 !important;
}

/* 菜单文本过渡效果 */
.menu-item-text,
.submenu-title-text,
.submenu-item-text {
  display: inline-block;
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
  transform-origin: left center;
  white-space: nowrap;
  overflow: hidden;
}

/* 折叠时隐藏文本 */
.custom-menu.is-collapse .menu-item-text,
.custom-menu.is-collapse .submenu-title-text,
.custom-menu.is-collapse .submenu-item-text {
  opacity: 0;
  transform: translateX(-10px) scale(0.8);
  width: 0;
  height: 0;
  padding: 0 !important;
  margin: 0 !important;
}
</style>
