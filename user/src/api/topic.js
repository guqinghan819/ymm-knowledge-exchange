import request from '@/axios'

// 根据频道获取话题列表
export function getTopics(channelId) {
  return request({
    url: '/topic/topic/get',
    method: 'post',
    data: { id: channelId }
  })
}
