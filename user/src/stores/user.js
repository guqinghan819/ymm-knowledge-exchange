import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserInfo } from '@/api/user'
import { getToken, removeToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  // 用户信息
  const userInfo = ref({})

  // 是否登录计算属性
  const isLoggedIn = computed(() => !!userInfo.value.nickname)

  // 设置用户信息
  function setUserInfo() {
    // 调用后端获取用户信息接口
    return getUserInfo().then(res => {
      if (res && res.success == true) {
        userInfo.value = res.data || {}
      }
    })
  }

  // 每个新页面都通过 Cookie 中的 Token 恢复内存里的用户信息。
  async function restoreUserInfo() {
    if (!getToken()) {
      logout()
      return
    }

    try {
      await setUserInfo()
    } catch (error) {
      logout()
      const status = error.response?.status
      const errorCode = error.errorCode ?? error.response?.data?.errorCode
      if (Number(status) === 401 || String(errorCode) === '401') {
        removeToken()
      }
      // 网络异常不删除 Token，交给启动流程兜底，后续刷新仍可重试。
      throw error
    }
  }

  // 退出登录清除状态
  function logout() {
    userInfo.value = {}
  }

  return { userInfo, isLoggedIn, setUserInfo, restoreUserInfo, logout }
})
