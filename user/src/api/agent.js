import { getToken } from '@/utils/auth'
import axios from '@/axios'

const API_PREFIX = '/ai/agent'

const sseHeaders = () => {
  const token = getToken()
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream'
  }
  if (token) headers['Ymm-Sa-Token'] = `Bearer ${token}`
  return headers
}

export function createChatStream(query, conversationId, signal) {
  return fetch(`/api${API_PREFIX}/chat/stream`, {
    method: 'POST',
    headers: sseHeaders(),
    body: JSON.stringify({ query, conversationId }),
    signal
  })
}

export function createPptStream(query, conversationId, signal) {
  return fetch(`/api${API_PREFIX}/ppt/create/stream`, {
    method: 'POST',
    headers: sseHeaders(),
    body: JSON.stringify({ query, conversationId }),
    signal
  })
}

export function createImageWritingStream(data, signal) {
  return fetch(`/api${API_PREFIX}/image-writing/stream`, {
    method: 'POST',
    headers: sseHeaders(),
    body: JSON.stringify(data),
    signal
  })
}

export function createPublishAssistantStream(data, signal) {
  return fetch(`/api${API_PREFIX}/publish-assistant/stream`, {
    method: 'POST',
    headers: sseHeaders(),
    body: JSON.stringify(data),
    signal
  })
}

export function createNoteRagStream(query, conversationId, signal) {
  return fetch(`/api${API_PREFIX}/note-rag/stream`, {
    method: 'POST',
    headers: sseHeaders(),
    body: JSON.stringify({ query, conversationId }),
    signal
  })
}

export function createNoteTitleStream(data, signal) {
  return fetch(`/api${API_PREFIX}/note/title/stream`, {
    method: 'POST',
    headers: sseHeaders(),
    body: JSON.stringify(data),
    signal
  })
}

export function createNoteContentStream(data, signal) {
  return fetch(`/api${API_PREFIX}/note/content/stream`, {
    method: 'POST',
    headers: sseHeaders(),
    body: JSON.stringify(data),
    signal
  })
}

export function stopChatStream(conversationId) {
  const params = new URLSearchParams({ conversationId })
  return fetch(`/api${API_PREFIX}/chat/stop?${params.toString()}`, {
    method: 'GET',
    headers: sseHeaders()
  })
}

export function getConversations() {
  return axios.get(`${API_PREFIX}/conversation/get`).then(res => res.data)
}

export function getMessages(conversationId) {
  return axios.get(`${API_PREFIX}/message/get/${encodeURIComponent(conversationId)}`).then(res => res.data)
}

export function deleteAllConversations() {
  return axios.delete(`${API_PREFIX}/conversation/delete`)
}

export function deleteConversation(conversationId) {
  return axios.delete(`${API_PREFIX}/conversation/delete/${encodeURIComponent(conversationId)}`)
}
