export const adminPages = [
  { path: '/admin/index', permission: 'admin:dashboard:view' },
  { path: '/admin/user/list', permission: 'admin:user:manage' },
  { path: '/admin/user/role', permission: 'admin:role:manage' },
  { path: '/admin/user/permission', permission: 'admin:permission:manage' },
  { path: '/admin/content/article', permission: 'admin:note:manage' },
  { path: '/admin/content/channel', permission: 'admin:channel:manage' },
  { path: '/admin/content/topic', permission: 'admin:topic:manage' },
]
export const builtinPermissionKeys = [...adminPages.map(p => p.permission), 'app:note:publish', 'app:comment:publish']
export function isAdmin(profile = {}) {
  return (profile.roleKeys || []).includes('admin')
}
export function canAssignRoles(profile = {}) {
  return isAdmin(profile) && ['admin:user:manage', 'admin:role:manage'].every(key => (profile.permissionKeys || []).includes(key))
}
export function canAssignPermissions(profile = {}) {
  return isAdmin(profile) && (profile.permissionKeys || []).includes('admin:role:manage')
}
export function canManageUserRow(userId, profile = {}) {
  return profile.id != null && userId != null && String(userId) !== String(profile.id)
}
export function canAccessPage(path, keys = [], roleKeys = []) {
  const page = adminPages.find(p => p.path === path)
  return Boolean(page && keys.includes(page.permission) &&
    (!['/admin/user/role', '/admin/user/permission'].includes(path) || roleKeys.includes('admin')))
}
export function firstAllowedPage(keys = [], roleKeys = []) {
  return adminPages.find(p => canAccessPage(p.path, keys, roleKeys))?.path || '/admin/forbidden'
}
export function authorizationChanged(response) {
  if (response?.success) window.dispatchEvent(new Event('admin-authorization-changed'))
  return response
}
export function permissionTree(list) {
  const nodes = new Map(list.map(item => [String(item.id), { ...item, id: String(item.id), parentId: String(item.parentId ?? 0), children: [] }]))
  const roots = []
  nodes.forEach(node => {
    const parent = nodes.get(node.parentId)
    if (parent && parent !== node) parent.children.push(node)
    else roots.push(node)
  })
  return roots
}
