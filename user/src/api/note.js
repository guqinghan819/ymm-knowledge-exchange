import request from '@/axios'

// 发布笔记
export function publishNote(data) {
  return request({
    url: '/note/note/publish',
    method: 'post',
    data
  })
}

// 游标查询笔记
export function getNotesByCursor(data) {
  return request({
    url: '/note/note/cursor/get',
    method: 'post',
    data
  })
}

// 获取当前用户收藏的笔记
export function getCollectedNotesByCursor(data) {
  return request({
    url: '/note/note/collect/cursor/get',
    method: 'post',
    data
  })
}

// 获取当前用户点赞的笔记
export function getLikedNotesByCursor(data) {
  return request({
    url: '/note/note/like/cursor/get',
    method: 'post',
    data
  })
}

// 获取当前用户主动点赞、收藏数量
export function getInteractionSummary() {
  return request({
    url: '/note/note/interaction/summary',
    method: 'get'
  })
}

// 点赞
export function likeNote(noteId) {
  return request({
    url: '/note/note/like',
    method: 'post',
    data: { noteId }
  })
}

// 取消点赞
export function unlikeNote(noteId) {
  return request({
    url: '/note/note/unlike',
    method: 'post',
    data: { noteId }
  })
}

// 收藏
export function collectNote(noteId) {
  return request({
    url: '/note/note/collect',
    method: 'post',
    data: { noteId }
  })
}

// 取消收藏
export function uncollectNote(noteId) {
  return request({
    url: '/note/note/UnCollect',
    method: 'post',
    data: { noteId }
  })
}

// 更新笔记
export function updateNote(data) {
  return request({
    url: '/note/note/update',
    method: 'post',
    data
  })
}

// 获取当前用户的笔记管理列表（按审核状态筛选）
export function queryMyNotes(data) {
  return request({
    url: '/note/note/my/list',
    method: 'post',
    data
  })
}

// 获取笔记详情（用于编辑回显）
export function getNoteDetail(id) {
  return request({
    url: '/note/note/detail',
    method: 'post',
    data: { id }
  })
}

// 批量获取首页同构笔记卡片
export function batchQueryNoteItems(noteIds) {
  return request({
    url: '/note/note/item/batch',
    method: 'post',
    data: { noteIds }
  })
}

// 删除笔记
export function deleteNote(noteId) {
  return request({
    url: '/note/note/delete',
    method: 'post',
    data: { noteId }
  })
}
