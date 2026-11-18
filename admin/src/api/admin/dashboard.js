import axios from '@/axios'

const API_PREFIX = '/note/note/admin'

/**
 * 获取管理端分析面板数据。
 */
export function getDashboardAnalysis() {
  return axios.get(`${API_PREFIX}/analysis`)
}
