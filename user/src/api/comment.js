import request from '@/axios'

// 发布评论
export function publishComment(data) {
  return request({
    url: '/comment/comment/publish',
    method: 'post',
    data
  })
}

// 查询一级评论
export function getCommentList(data) {
  return request({
    url: '/comment/comment/list',
    method: 'post',
    data
  })
}

// 查询二级回复
export function getReplyCommentList(data) {
  return request({
    url: '/comment/comment/reply/list',
    method: 'post',
    data
  })
}

// 点赞评论
export function likeComment(commentId) {
  return request({
    url: '/comment/comment/like',
    method: 'post',
    data: { commentId }
  })
}

// 取消点赞评论
export function unlikeComment(commentId) {
  return request({
    url: '/comment/comment/unlike',
    method: 'post',
    data: { commentId }
  })
}
