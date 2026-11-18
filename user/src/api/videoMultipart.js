import axios from '@/axios'

const API_PREFIX = '/file/file/video/multipart'

export function initializeVideoMultipart(payload) {
  return axios.post(`${API_PREFIX}/init`, payload)
}

export function signVideoPart(taskId, partNumber, config = {}) {
  return axios.post(`${API_PREFIX}/${encodeURIComponent(taskId)}/parts/${partNumber}/sign`, null, config)
}

export function getVideoMultipartStatus(taskId, config = {}) {
  return axios.get(`${API_PREFIX}/${encodeURIComponent(taskId)}/status`, config)
}

export function completeVideoMultipart(taskId, config = {}) {
  return axios.post(`${API_PREFIX}/${encodeURIComponent(taskId)}/complete`, null, {
    timeout: 60 * 1000,
    ...config
  })
}

export function abortVideoMultipart(taskId) {
  return axios.delete(`${API_PREFIX}/${encodeURIComponent(taskId)}`)
}
