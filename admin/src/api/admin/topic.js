import axios from '@/axios'

// 接口前缀：网关 /topic/** -> note 服务(StripPrefix=1)，TopicAdminController 映射 /topic/admin
// 因此外部访问路径为 /topic/topic/admin/...
const API_PREFIX = '/topic/topic/admin'

/**
 * 获取话题列表（含所属频道）
 */
export function getTopicList() {
  return axios.get(`${API_PREFIX}/list`)
}

/**
 * 新增话题
 * @param {Object} data { name }
 */
export function createTopic(data) {
  return axios.post(`${API_PREFIX}/create`, data)
}

/**
 * 修改话题
 * @param {Object} data { id, name }
 */
export function updateTopic(data) {
  return axios.post(`${API_PREFIX}/update`, data)
}

/**
 * 删除话题
 * @param {String} id 话题ID
 */
export function deleteTopic(id) {
  return axios.post(`${API_PREFIX}/delete`, { id })
}
