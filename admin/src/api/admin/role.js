import { authorizationChanged } from '@/utils/authorization'
import axios from '@/axios'

// 接口前缀：网关 /user/** -> user 服务(StripPrefix=1)，RoleAdminController 映射 /role/admin
const API_PREFIX = '/user/role/admin'

/**
 * 查询角色列表
 * @param {Object} params { roleName, roleKey, status }
 */
export function getRoleList(params) {
  return axios.post(`${API_PREFIX}/list`, params)
}

/**
 * 新增角色
 * @param {Object} data { roleName, roleKey, status, sort, remark }
 */
export function createRole(data) {
  return axios.post(`${API_PREFIX}/create`, data).then(authorizationChanged)
}

/**
 * 编辑角色
 * @param {Object} data { id, roleName, roleKey, status, sort, remark }
 */
export function updateRole(data) {
  return axios.post(`${API_PREFIX}/update`, data).then(authorizationChanged)
}

/**
 * 删除角色
 * @param {String} id 角色ID
 */
export function deleteRole(id) {
  return axios.post(`${API_PREFIX}/delete`, { id }).then(authorizationChanged)
}

export const getRolePermissions = roleId => axios.post(`${API_PREFIX}/permissions/get`, { roleId })
export const assignRolePermissions = (roleId, permissionIds) => axios.post(`${API_PREFIX}/permissions/assign`, { roleId, permissionIds }).then(authorizationChanged)
export const getUserRoles = userId => axios.post('/user/user-role/admin/get', { userId })
export const assignUserRoles = (userId, roleIds) => axios.post('/user/user-role/admin/assign', { userId, roleIds }).then(authorizationChanged)
