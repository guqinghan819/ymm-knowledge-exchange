import request from '@/axios'

export function getUserCount(userId) {
  return request({
    url: '/count/count/user/get',
    method: 'post',
    data: { userId }
  })
}
