import axios from '@/axios'

// 接口前缀：网关 /note/** -> note 服务(StripPrefix=1)，NoteAdminController 映射 /note/admin
// 因此外部访问路径为 /note/note/admin/...
const API_PREFIX = '/note/note/admin'

/**
 * 获取文章列表（游标分页）
 * @param {Object} params { status, creatorId, cursor, limit }
 * @returns {Promise}
 */
export function getArticleList(params) {
  return axios.post(`${API_PREFIX}/list`, params)
}

/** 获取任意状态的笔记详情。 */
export function getArticleDetail(id) {
  return axios.post(`${API_PREFIX}/detail`, { id })
}

/**
 * 审核通过
 * @param {String} noteId 文章ID（雪花id，字符串）
 */
export function checkArticlePass(noteId) {
  return axios.post(`${API_PREFIX}/check/pass`, { noteId })
}

/**
 * 审核驳回
 * @param {String} noteId 文章ID
 */
export function checkArticleReject(noteId, reason) {
  return axios.post(`${API_PREFIX}/check/reject`, { noteId, reason })
}

/**
 * 获取平台支持的下架原因。
 */
export function getDownReasonOptions() {
  return axios.get(`${API_PREFIX}/down/reasons`)
}

/**
 * 下架文章
 * @param {Object} data { noteId, reasonCode, reasonDetail }
 */
export function downArticle(data) {
  return axios.post(`${API_PREFIX}/down`, data)
}

/**
 * 恢复上架
 * @param {String} noteId 文章ID
 */
export function restoreArticle(noteId) {
  return axios.post(`${API_PREFIX}/restore`, { noteId })
}

/**
 * 强制删除
 * @param {String} noteId 文章ID
 */
export function deleteArticle(noteId) {
  return axios.post(`${API_PREFIX}/delete`, { noteId })
}
