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
 * 获取用户详细信息
 * @param {String} ymmId
 */
export function getUserDetail(ymmId) {
  return axios.post(`${API_PREFIX}/get/detail`, { ymmId })
}

/**
 * 根据用户主键获取公开的创作者信息
 * @param {String|Number} actorId
 */
export function getCreatorById(actorId) {
  return axios.post(`${API_PREFIX}/getCreatorById`, { creatorId: actorId })
}

/**
 * 更新用户
 * @param {Object} data 用户数据
 * @returns {Promise}
 */
export function updateUser(data) {
  return axios.post(`${API_PREFIX}/update`, data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

