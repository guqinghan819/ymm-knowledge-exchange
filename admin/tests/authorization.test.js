import { test } from 'node:test'
import assert from 'node:assert/strict'
import { adminPages, canAccessPage, firstAllowedPage, permissionTree, canAssignRoles, canAssignPermissions, canManageUserRow } from '../src/utils/authorization.js'

test('ordinary users have no admin landing page', () => {
  assert.equal(firstAllowedPage(['app:note:publish']), '/admin/forbidden')
  for (const { path } of adminPages) assert.equal(canAccessPage(path, []), false)
})
test('content operator can only access notes', () => {
  const keys = ['admin:note:manage']
  assert.equal(firstAllowedPage(keys), '/admin/content/article')
  assert.equal(canAccessPage('/admin/content/article', keys), true)
  assert.equal(canAccessPage('/admin/index', keys), false)
  assert.equal(canAccessPage('/admin/user/role', keys), false)
})
test('revocation and multi-role permission union change routes immediately', () => {
  assert.equal(canAccessPage('/admin/user/role', ['admin:note:manage', 'admin:role:manage'], ['admin']), true)
  assert.equal(canAccessPage('/admin/user/role', ['admin:note:manage']), false)
  assert.equal(firstAllowedPage(adminPages.map(p => p.permission)), '/admin/index')
  assert.equal(canAccessPage('/admin/not-registered', adminPages.map(p => p.permission)), false)
})
test('tree normalizes ids without losing bigint precision or mutating the source', () => {
  const rows = [{ id: '9223372036854775806', parentId: 0, name: 'parent' }, { id: '9223372036854775807', parentId: '9223372036854775806', name: 'child', status: 1 }]
  const tree = permissionTree(rows)
  assert.equal(tree.length, 1)
  assert.equal(tree[0].children[0].id, '9223372036854775807')
  assert.equal(tree[0].children[0].status, 1)
  assert.equal(rows[0].children, undefined)
})


test('role and permission management require an admin role as well as module permissions', () => {
  const keys = ['admin:role:manage', 'admin:permission:manage', 'admin:user:manage']
  for (const path of ['/admin/user/role', '/admin/user/permission']) {
    assert.equal(canAccessPage(path, keys, ['common_user']), false)
    assert.equal(canAccessPage(path, keys, ['admin']), true)
    assert.equal(canAccessPage(path, [], ['admin']), false)
  }
  assert.equal(firstAllowedPage(['admin:role:manage'], []), '/admin/forbidden')
  assert.equal(canAssignRoles({ roleKeys: ['common_user'], permissionKeys: keys }), false)
  assert.equal(canAssignRoles({ roleKeys: ['admin'], permissionKeys: keys }), true)
  assert.equal(canAssignRoles({ roleKeys: ['admin'], permissionKeys: ['admin:user:manage'] }), false)
  assert.equal(canAssignPermissions({ roleKeys: ['common_user'], permissionKeys: keys }), false)
  assert.equal(canAssignPermissions({ roleKeys: ['admin'], permissionKeys: keys }), true)
})

test('current user and unloaded profile have no row operations; ids preserve precision', () => {
  assert.equal(canManageUserRow('9223372036854775807', { id: '9223372036854775807' }), false)
  assert.equal(canManageUserRow(42, { id: '42' }), false)
  assert.equal(canManageUserRow('9223372036854775806', { id: '9223372036854775807' }), true)
  assert.equal(canManageUserRow('42', {}), false)
})
