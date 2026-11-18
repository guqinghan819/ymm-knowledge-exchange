import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo } from '@/api/admin/user'

export const useUserStore = defineStore('user', () => {
  // 用户信息
  const userInfo = ref({})
  let profileRequest = null
  let sessionVersion = 0

  // 同一次导航和登录只请求一次资料；退出后旧请求不能写回新会话。
  function setUserInfo(force = false) {
    if (profileRequest) {
      if (!force) return profileRequest
      const version = sessionVersion
      return profileRequest.catch(() => {}).then(() => {
        if (version !== sessionVersion) throw new Error('登录会话已变更')
        return setUserInfo()
      })
    }

    const requestVersion = sessionVersion
    const request = getUserInfo()
      .then(res => {
        if (!res?.success || !res.data?.id) {
          throw new Error(res?.message || '获取登录用户信息失败')
        }
        if (requestVersion === sessionVersion) userInfo.value = res.data
        return res.data
      })
      .finally(() => {
        if (profileRequest === request) profileRequest = null
      })
    profileRequest = request
    return request
  }

  function clearUserInfo() {
    sessionVersion++
    profileRequest = null
    userInfo.value = {}
  }

  return { userInfo, setUserInfo, clearUserInfo }
})
