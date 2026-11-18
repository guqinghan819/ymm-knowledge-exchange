import axios from '@/axios'

// 接口前缀：网关 /channel/** -> note 服务(StripPrefix=1)，ChannelAdminController 映射 /channel/admin
// 因此外部访问路径为 /channel/channel/admin/...
const API_PREFIX = '/channel/channel/admin'

/**
 * 获取频道列表（含绑定话题）
 */
export function getChannelList() {
  return axios.get(`${API_PREFIX}/list`)
}

/**
 * 新增频道
 * @param {Object} data { name }
 */
export function createChannel(data) {
  return axios.post(`${API_PREFIX}/create`, data)
}

/**
 * 修改频道
 * @param {Object} data { id, name }
 */
export function updateChannel(data) {
  return axios.post(`${API_PREFIX}/update`, data)
}

/**
 * 删除频道
 * @param {String} id 频道ID
 */
export function deleteChannel(id) {
  return axios.post(`${API_PREFIX}/delete`, { id })
}

/**
 * 绑定频道下的话题（覆盖式）
 * @param {Object} data { channelId, topicIds }
 */
export function bindTopics(data) {
  return axios.post(`${API_PREFIX}/bindTopics`, data)
}
