import { authorizationChanged } from '@/utils/authorization'
import axios from '@/axios'

// 接口前缀
const API_PREFIX = '/user/user'


/**
 * 获取用户个人资料
 */
export function getUserInfo() {
  return axios.get(`${API_PREFIX}/getLoginUser`)
}

/**
 * 获取用户列表
 * @param {Object} params 查询参数
 * @returns {Promise}
 */
export function getUserList(params) {
  return axios.post(`${API_PREFIX}/getUserList`, params)
}

/**
 * 新增用户
 * @param {Object} data 用户数据
 * @returns {Promise}
 */
export function addUser(data) {
  return axios.post('/addUser', data)
}

/**
 * 更新用户
 * @param {Object} data 用户数据
 * @returns {Promise}
 */
export function updateUser(data) {
  return axios.post(`${API_PREFIX}/admin/update`, data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }).then(authorizationChanged)
}

/**
 * 删除用户
 * @param {String} ymmId 用户YMM ID
 * @returns {Promise}
 */
export function deleteUser(ymmId) {
  return axios.post(`${API_PREFIX}/delete`, { ymmId }).then(authorizationChanged)
}

/**
 * 更新用户状态
 * @param {Number} id 用户ID
 * @param {Number} status 用户状态 0-启用 1-禁用
 * @returns {Promise}
 */
export function updateUserStatus(id, status) {
  return axios.post('/updateUserStatus', { id, status })
}