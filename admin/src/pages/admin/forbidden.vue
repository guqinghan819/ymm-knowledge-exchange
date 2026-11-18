<template>
  <main class="access-message">
    <el-result icon="warning" title="无权访问此页面" sub-title="请联系管理员分配相应的管理权限。">
      <template #extra>
        <el-button v-if="home !== '/admin/forbidden'" type="primary" @click="router.replace(home)">进入可用模块</el-button>
        <el-button @click="refresh">刷新权限</el-button>
        <el-button @click="logout">退出登录</el-button>
      </template>
    </el-result>
  </main>
</template>
<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { removeToken } from '@/utils/auth'
import { firstAllowedPage } from '@/utils/authorization'
const router = useRouter()
const store = useUserStore()
const home = computed(() => firstAllowedPage(store.userInfo.permissionKeys, store.userInfo.roleKeys))
async function refresh() { await store.setUserInfo(); if (home.value !== '/admin/forbidden') router.replace(home.value) }
function logout() { removeToken(); store.clearUserInfo(); router.replace('/admin/login') }
</script>
<style scoped>.access-message { min-height: 100vh; display: grid; place-items: center; background: #f5f7fa; }</style>
