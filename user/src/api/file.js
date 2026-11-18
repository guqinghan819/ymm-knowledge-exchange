import axios from '@/axios'

const API_PREFIX = '/file/file'

/**
 * 上传文件
 * @param {File} file
 * @param {import('axios').AxiosRequestConfig} config
 */
export function uploadFile(file, config = {}) {
  const formData = new FormData()
  formData.append('file', file)
  
  return axios.post(`${API_PREFIX}/upload`, formData, {
    ...config,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
