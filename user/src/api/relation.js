import request from '@/axios'

export function followUser(followUserId) {
  return request({
    url: '/relation/relation/follow',
    method: 'post',
    data: { followUserId }
  })
}

export function unfollowUser(unfollowUserId) {
  return request({
    url: '/relation/relation/unfollow',
    method: 'post',
    data: { unfollowUserId }
  })
}
