import request from '@/axios'

// 获取频道列表
export function getChannels() {
  return request({
    url: '/channel/channel/get',
    method: 'get'
  })
}
