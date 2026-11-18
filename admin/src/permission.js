import router from '@/router/index'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'
import { showMessage, showPageLoading, hidePageLoading } from '@/utils/utils'
import { canAccessPage, firstAllowedPage } from '@/utils/authorization'

router.beforeEach(async to => {
  showPageLoading()
  const store = useUserStore()
  if (!getToken()) {
    store.clearUserInfo()
    if (to.path !== '/admin/login') return '/admin/login'
    return
  }
  try {
    await store.setUserInfo()
  } catch {
    if (!getToken()) return '/admin/login'
    // Never grant access based on stale profile data after a failed refresh.
    store.clearUserInfo()
    if (to.path !== '/admin/forbidden') return '/admin/forbidden'
    return
  }
  const keys = store.userInfo.permissionKeys || []
  if (to.path === '/admin/login' || to.path === '/admin') return firstAllowedPage(keys, store.userInfo.roleKeys)
  if (to.path !== '/admin/forbidden' && !canAccessPage(to.path, keys, store.userInfo.roleKeys)) {
    showMessage('没有访问该模块的权限', 'warning')
    return '/admin/forbidden'
  }
})
router.afterEach(to => {
  document.title = `${to.meta.title || ''} - 雨绵绵`
  hidePageLoading()
})
router.onError(() => hidePageLoading())
let refreshing = null
window.addEventListener('admin-authorization-changed', () => {
  if (!getToken() || refreshing) return
  refreshing = useUserStore().setUserInfo(true).then(profile => {
    const path = router.currentRoute.value.path
    if (path !== '/admin/login' && path !== '/admin/forbidden' && !canAccessPage(path, profile.permissionKeys, profile.roleKeys)) {
      return router.replace('/admin/forbidden')
    }
  }).catch(() => {
    useUserStore().clearUserInfo()
    if (getToken()) return router.replace('/admin/forbidden')
  }).finally(() => { refreshing = null })
})
