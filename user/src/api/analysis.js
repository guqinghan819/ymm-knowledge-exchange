import request from '@/axios'

export function getFanAnalysis() {
  return request({
    url: '/relation/fans/analysis',
    method: 'post'
  })
}

export function getNoteAnalysis() {
  return request({
    url: '/note/note/analysis',
    method: 'post'
  })
}
