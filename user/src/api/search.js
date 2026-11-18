import request from '@/axios'

// 搜索笔记
export function searchNotes(data) {
  return request({
    url: '/search/search/note/list',
    method: 'post',
    data
  })
}

// 搜索用户
export function searchUsers(data) {
  return request({
    url: '/search/search/user/list',
    method: 'post',
    data
  })
}
