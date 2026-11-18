<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createChatStream, createPptStream, createImageWritingStream, createPublishAssistantStream, createNoteRagStream, stopChatStream, getConversations, getMessages, deleteAllConversations, deleteConversation as deleteConversationApi } from '@/api/agent'
import { uploadFile } from '@/api/file'
import { getChannels } from '@/api/channel'
import { getTopics } from '@/api/topic'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'
import MarkdownIt from 'markdown-it'
import AiKnowledgeMark from '@/components/AiKnowledgeMark.vue'
import AiModeIcon from '@/components/AiModeIcon.vue'

const md = new MarkdownIt({ html: true, breaks: true, linkify: true })

interface Reference {
  type?: 'web' | 'note'
  url?: string
  title?: string
  abstract?: string
  noteId?: string | number
  parentId?: string
  topicName?: string
  creatorName?: string
  cover?: string
  snippet?: string
  citationNo?: number
  score?: number
  matchedQuery?: string
}

interface PublishTopicOption {
  topicId: string | number
  topicName: string
}

interface PublishChannelOption {
  channelId: string | number
  channelName: string
  topics: PublishTopicOption[]
}

interface PublishCatalogItem {
  id: string | number
  name: string
}

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  thinking: string
  references: Reference[]
  recommends: string[]
  publishOptions: PublishChannelOption[]
  publishThemeOptions: string[]
  isComplete: boolean
  error?: string
  status?: string
  imageUrls?: string[]
  fileUrl?: string
  publishOutcome?: string
  _html: string
  _thinkingHtml: string
}

interface Conversation {
  agentType?: string
  updatedTime?: string
  id: string
  title: string
  createdTime: string
}

const messages = ref<Message[]>([])
const input = ref('')
const loading = ref(false)
const stopped = ref(false)
const listRef = ref<HTMLElement | null>(null)
const controller = ref<AbortController | null>(null)
const conversationId = ref(`conv-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`)
const router = useRouter()
const userStore = useUserStore()
const isLoggedIn = ref(false)
const loginPromptVisible = ref(false)
const thinkingExpanded = ref<Record<string, boolean>>({})
const activeQuestionId = ref('')
const manualScrollLock = ref(false)
const sidebarCollapsed = ref(false)

const chatModes = [
  { key: 'webSearch', label: '联网搜索' },
  { key: 'noteRag', label: '知识库问答' },
  { key: 'ppt', label: 'PPT 生成' },
  { key: 'imageWriting', label: '识图写作' },
  { key: 'publishAssistant', label: '发布助手' }
] as const
const activeMode = ref<string>('webSearch')

interface SelectedImage {
  id: string
  file: File
  previewUrl: string
}

const MAX_IMAGE_COUNT = 3
const selectedImages = ref<SelectedImage[]>([])
const imageInputRef = ref<HTMLInputElement | null>(null)
const imageAttachVisible = computed(() => activeMode.value === 'imageWriting' || activeMode.value === 'publishAssistant')
const pendingPublishImageUrls = ref<string[]>([])
const reusePendingPublishImagesOnce = ref(false)
const publishCatalogPanelVisible = ref(true)
const publishCatalogLoading = ref(false)
const publishTopicLoading = ref(false)
const publishChannels = ref<PublishCatalogItem[]>([])
const publishTopics = ref<PublishCatalogItem[]>([])
const selectedPublishChannelId = ref<string | number>('')
const selectedPublishTopicId = ref<string | number>('')

const selectedPublishChannel = computed(() => {
  return publishChannels.value.find(item => String(item.id) === String(selectedPublishChannelId.value)) || null
})

const selectedPublishTopic = computed(() => {
  return publishTopics.value.find(item => String(item.id) === String(selectedPublishTopicId.value)) || null
})

const publishSelectionLabel = computed(() => {
  const channelName = selectedPublishChannel.value?.name
  const topicName = selectedPublishTopic.value?.name
  if (channelName && topicName) return `${channelName} / ${topicName}`
  if (channelName) return `${channelName} / 待选话题`
  return '未选择发布位置'
})

function clearPendingPublishContext() {
  pendingPublishImageUrls.value = []
  reusePendingPublishImagesOnce.value = false
}

function clearImages() {
  selectedImages.value.forEach((image) => URL.revokeObjectURL(image.previewUrl))
  selectedImages.value = []
}

function triggerImagePicker() {
  if (selectedImages.value.length >= MAX_IMAGE_COUNT) {
    ElMessage.warning(`最多只能上传 ${MAX_IMAGE_COUNT} 张图片`)
    return
  }
  imageInputRef.value?.click()
}

function handleImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  const remaining = MAX_IMAGE_COUNT - selectedImages.value.length

  if (remaining <= 0) {
    ElMessage.warning(`最多只能上传 ${MAX_IMAGE_COUNT} 张图片`)
    input.value = ''
    return
  }

  if (files.length > remaining) {
    ElMessage.warning(`最多只能上传 ${MAX_IMAGE_COUNT} 张图片，已为你保留前 ${remaining} 张`)
  }

  files.slice(0, remaining).forEach((file) => {
    if (!file.type.startsWith('image/')) {
      ElMessage.warning('只能上传图片文件')
      return
    }

    selectedImages.value.push({
      id: genId('img'),
      file,
      previewUrl: URL.createObjectURL(file)
    })
  })

  input.value = ''
}

function removeImage(id: string) {
  const index = selectedImages.value.findIndex((image) => image.id === id)
  if (index === -1) return
  const [removed] = selectedImages.value.splice(index, 1)
  URL.revokeObjectURL(removed.previewUrl)
}

function normalizeCatalogItems(raw: unknown): PublishCatalogItem[] {
  if (!Array.isArray(raw)) return []
  return raw
    .map((item: any) => ({
      id: item?.id ?? item?.channelId ?? item?.topicId ?? '',
      name: String(item?.name ?? item?.channelName ?? item?.topicName ?? '').trim()
    }))
    .filter((item: PublishCatalogItem) => item.id !== '' && item.name)
}

async function ensurePublishChannelsLoaded(force = false) {
  if (!isLoggedIn.value || publishCatalogLoading.value) return
  if (!force && publishChannels.value.length) return
  publishCatalogLoading.value = true
  try {
    const res: any = await getChannels()
    if (res?.success) {
      publishChannels.value = normalizeCatalogItems(res.data)
    } else {
      publishChannels.value = []
      ElMessage.warning(res?.message || '频道列表读取失败')
    }
  } catch {
    publishChannels.value = []
    ElMessage.error('频道列表读取失败')
  } finally {
    publishCatalogLoading.value = false
  }
}

async function loadPublishTopics(channelId: string | number) {
  publishTopics.value = []
  selectedPublishTopicId.value = ''
  if (!channelId) return
  publishTopicLoading.value = true
  try {
    const res: any = await getTopics(channelId)
    if (res?.success) {
      publishTopics.value = normalizeCatalogItems(res.data)
    } else {
      ElMessage.warning(res?.message || '话题列表读取失败')
    }
  } catch {
    ElMessage.error('话题列表读取失败')
  } finally {
    publishTopicLoading.value = false
  }
}

function handlePublishChannelChange() {
  loadPublishTopics(selectedPublishChannelId.value)
}

function clearPublishSelection() {
  selectedPublishChannelId.value = ''
  selectedPublishTopicId.value = ''
  publishTopics.value = []
}

function getPublishSelectionPayload() {
  return {
    channelId: selectedPublishChannel.value?.id || null,
    channelName: selectedPublishChannel.value?.name || null,
    topicId: selectedPublishTopic.value?.id || null,
    topicName: selectedPublishTopic.value?.name || null
  }
}

function showLoginPrompt() {
  if (loginPromptVisible.value) return
  loginPromptVisible.value = true

  ElMessageBox.confirm('登录后才能使用 AI 对话功能', '需要登录', {
    confirmButtonText: '去登录',
    cancelButtonText: '返回首页',
    type: 'warning',
    closeOnClickModal: false
  })
    .then(() => {
      router.push({ name: 'home', query: { login: '1' } })
    })
    .catch(() => {
      router.push('/')
    })
    .finally(() => {
      loginPromptVisible.value = false
    })
}

function requireLogin() {
  if (isLoggedIn.value) return true
  showLoginPrompt()
  return false
}

async function initAuth() {
  if (!getToken()) {
    isLoggedIn.value = false
    showLoginPrompt()
    return
  }

  if (!userStore.isLoggedIn) {
    try {
      await userStore.setUserInfo()
    } catch {
      // 登录态已失效，由 Axios 拦截器统一提示
    }
  }

  isLoggedIn.value = userStore.isLoggedIn
  if (!isLoggedIn.value) {
    showLoginPrompt()
  }
}

function switchMode(key: string) {
  if (!requireLogin()) return
  if (loading.value) return
  if (activeMode.value === key) return
  activeMode.value = key
  clearImages()
  clearPendingPublishContext()
  startNewConversation()
  if (key === 'publishAssistant') {
    publishCatalogPanelVisible.value = true
    ensurePublishChannelsLoaded()
  }
}

const userQuestions = computed<Message[]>(() => {
  return messages.value.filter(m => m.role === 'user' && m.content)
})

// 会话历史列表（仅元数据，消息按需加载）
const conversations = ref<Conversation[]>([])

async function loadConversations() {
  try {
    const data = await getConversations()
    if (data && data.history) {
      conversations.value = data.history.map((c: any) => ({
        id: c.conversationId,
        title: c.title || '新对话',
        createdTime: c.createdTime,
        updatedTime: c.updatedTime,
        agentType: c.agentType
      }))
    }
  } catch {
    conversations.value = []
  }
}

function switchConversation(conv: Conversation) {
  if (!requireLogin()) return
  if (loading.value) return
  if (conv.id === conversationId.value) return
  controller.value?.abort()
  conversationId.value = conv.id
  activeMode.value = modeForAgent(conv.agentType)
  input.value = ''
  clearImages()
  clearPendingPublishContext()
  clearPublishSelection()
  thinkingExpanded.value = {}
  stopped.value = false
  activeQuestionId.value = ''
  loadConversationMessages(conv.id)
}

function modeForAgent(agentType?: string) {
  const modes: Record<string, string> = {
    webSearchAgent: 'webSearch', noteRagAgent: 'noteRag', pptAgent: 'ppt',
    imageWritingAgent: 'imageWriting', publishAssistantAgent: 'publishAssistant'
  }
  return modes[agentType || ''] || 'webSearch'
}

function statusLabel(status?: string) {
  const labels: Record<string, string> = {
    GENERATING: '生成中，重新打开会话可查看最新结果', STOPPED: '已停止生成',
    DISCONNECTED: '连接已断开，已保存现有内容', FAILED: '生成失败'
  }
  return labels[status || ''] || ''
}

let historyLoadSequence = 0
async function loadConversationMessages(convId: string) {
  const sequence = ++historyLoadSequence
  messages.value = []
  try {
    const data = await getMessages(convId)
    if (convId !== conversationId.value || sequence !== historyLoadSequence) return
    if (data && data.messageVOList) {
      const mapped: Message[] = []
      const last = data.messageVOList.at(-1)
      if (last?.agentType) activeMode.value = modeForAgent(last.agentType)
      for (const m of data.messageVOList) {
        const metadata = m.metadata && typeof m.metadata === 'object' ? m.metadata : {}
        mapped.push({
          id: m.messageId + '-u',
          role: 'user',
          content: m.question || '',
          imageUrls: Array.isArray(metadata.imageUrls) ? metadata.imageUrls : [],
          thinking: '',
          references: [],
          recommends: [],
          publishOptions: [],
          publishThemeOptions: [],
          isComplete: true,
          _html: '',
          _thinkingHtml: ''
        })
        let recommends: string[] = []
        if (m.recommend) {
          try { recommends = JSON.parse(m.recommend) } catch { /* ignore */ }
        }
        const assistantId = m.messageId + '-a'
        const storedReferences = parseReferences(m.reference || '')
        mapped.push({
          id: assistantId,
          role: 'assistant',
          content: m.answer || '',
          status: m.status || 'COMPLETED',
          error: m.errorMessage || '',
          fileUrl: metadata.fileUrl || '',
          publishOutcome: metadata.publishOutcome || '',
          thinking: m.thinking || '',
          references: storedReferences,
          recommends,
          publishOptions: normalizePublishOptions(metadata.publishOptions),
          publishThemeOptions: normalizePublishThemeOptions(metadata.publishThemeOptions),
          isComplete: true,
          _html: renderMarkdown(m.answer || '', storedReferences.length > 0 ? assistantId : ''),
          _thinkingHtml: renderMarkdown(m.thinking || '')
        })
      }
      messages.value = mapped
      const metadata = last?.metadata || {}
      if (activeMode.value === 'publishAssistant') {
        // Reuse only through the existing explicit option-selection flow.
        const awaitingChoice = mapped.at(-1)
        if (awaitingChoice?.publishOptions.length || awaitingChoice?.publishThemeOptions.length) {
          pendingPublishImageUrls.value = Array.isArray(metadata.imageUrls) ? [...metadata.imageUrls] : []
        }
        if (metadata.channelId && metadata.channelName) {
          if (!publishChannels.value.some(c => String(c.id) === String(metadata.channelId))) {
            publishChannels.value.push({ id: metadata.channelId, name: metadata.channelName })
          }
          selectedPublishChannelId.value = metadata.channelId
          if (metadata.topicId && metadata.topicName) {
            publishTopics.value = [{ id: metadata.topicId, name: metadata.topicName }]
            selectedPublishTopicId.value = metadata.topicId
          }
        }
      }
    }
  } catch {
    if (convId !== conversationId.value || sequence !== historyLoadSequence) return
    ElMessage.error('加载消息失败')
  }
  nextTick(() => scrollBottom())
}

function startNewConversation() {
  if (!requireLogin()) return
  if (loading.value) return
  controller.value?.abort()
  conversationId.value = `conv-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  messages.value = []
  input.value = ''
  clearImages()
  clearPendingPublishContext()
  clearPublishSelection()
  thinkingExpanded.value = {}
  stopped.value = false
  activeQuestionId.value = ''
}

async function deleteConversation(id: string, e: Event) {
  e.stopPropagation()
  if (!requireLogin()) return
  try {
    await deleteConversationApi(id)
    const idx = conversations.value.findIndex(c => c.id === id)
    if (idx !== -1) conversations.value.splice(idx, 1)
    if (id === conversationId.value) startNewConversation()
  } catch {
    // error toast handled by axios interceptor
  }
}

function scrollToMessage(msgId: string) {
  const el = document.getElementById(`msg-${msgId}`)
  if (!el) return
  manualScrollLock.value = true
  el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  el.classList.add('msg-flash')
  setTimeout(() => el.classList.remove('msg-flash'), 1500)
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const canSend = computed(() => {
  if (!isLoggedIn.value || loading.value) return false
  if (activeMode.value === 'imageWriting') {
    return Boolean(input.value.trim() || selectedImages.value.length)
  }
  return Boolean(input.value.trim())
})

const inputPlaceholder = computed(() => {
  if (activeMode.value === 'imageWriting') {
    return '添加图片后输入写作要求，Enter 发送...'
  }
  if (activeMode.value === 'publishAssistant') {
    return '告诉我想发布什么，频道和话题写清楚会更快；正文最多 2000 字...'
  }
  if (activeMode.value === 'noteRag') {
    return '向平台知识库提问，Enter 发送，Shift+Enter 换行...'
  }
  return '输入问题，Enter 发送，Shift+Enter 换行...'
})
const showScrollButton = ref(false)
const SCROLL_THRESHOLD = 200
const FOLLOW_LOCK_DISTANCE = 120
let programmaticScrollTop: number | null = null

function onChatScroll() {
  const el = listRef.value
  if (!el) { showScrollButton.value = false; return }
  const distance = el.scrollHeight - el.scrollTop - el.clientHeight
  showScrollButton.value = distance > SCROLL_THRESHOLD
  if (programmaticScrollTop !== null && Math.abs(el.scrollTop - programmaticScrollTop) < 2) {
    programmaticScrollTop = null
    return
  }
  programmaticScrollTop = null
  if (loading.value) manualScrollLock.value = distance > FOLLOW_LOCK_DISTANCE
}

function genId(p: string) { return `${p}-${Date.now()}-${Math.random().toString(36).slice(2, 7)}` }

function pickPrompt(text: string) {
  if (!requireLogin()) return
  if (loading.value) return
  input.value = text
  send()
}

function setActiveQuestion(id: string) {
  activeQuestionId.value = id
}

function addUserMessage(text: string) {
  messages.value.push({ id: genId('u'), role: 'user', content: text, thinking: '', references: [], recommends: [], publishOptions: [], publishThemeOptions: [], isComplete: true, _html: '', _thinkingHtml: '' })
}

function addAssistantMessage(): Message {
  const msg: Message = { id: genId('a'), role: 'assistant', content: '', thinking: '', references: [], recommends: [], publishOptions: [], publishThemeOptions: [], isComplete: false, _html: '', _thinkingHtml: '' }
  messages.value.push(msg)
  return messages.value[messages.value.length - 1]
}

function renderMarkdown(text: string, citationScope = '') {
  if (!text) return ''
  const source = citationScope
    ? text.replace(/\[(\d+)]/g, `[\\[$1\\]](#rag-reference-${citationScope}-$1)`)
    : text
  try { return md.render(source) } catch { return text }
}

function finalizeMessage(msg: Message) {
  msg.isComplete = true
  msg._html = renderMarkdown(msg.content || '', msg.references.length > 0 ? msg.id : '')
  msg._thinkingHtml = renderMarkdown(msg.thinking || '')
}

function appendThinking(msg: Message, content: string, markdownLine = false) {
  if (!content) return
  msg.thinking += markdownLine ? `- ${content}\n` : content
  msg._thinkingHtml = renderMarkdown(msg.thinking)
  thinkingExpanded.value[msg.id] = true
}

function getGeneratingText(msg: Message) {
  if (msg.thinking.includes('发布助手') || msg.thinking.includes('配图') || msg.thinking.includes('预发布')) {
    return '正在准备发布预览'
  }
  if (msg.thinking.includes('润色') || msg.thinking.includes('最终结果')) {
    return '正在整理最终结果'
  }
  return '正在生成回复'
}

function toggleThinking(msgId: string) {
  thinkingExpanded.value[msgId] = !thinkingExpanded.value[msgId]
}

async function scrollBottom(smooth = false) {
  manualScrollLock.value = false
  await nextTick()
  const el = listRef.value
  if (!el) return
  if (smooth && !loading.value) {
    el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
  } else {
    el.scrollTop = el.scrollHeight
    programmaticScrollTop = el.scrollTop
  }
}

async function followStreamOutput(msg: Message) {
  await nextTick()
  const list = listRef.value
  if (!list || manualScrollLock.value) return
  const message = document.getElementById(`msg-${msg.id}`)
  if (!message) return

  const thinkingBody = message.querySelector<HTMLElement>('.ds-thinking-body')
  if (thinkingBody && !msg.content) thinkingBody.scrollTop = thinkingBody.scrollHeight

  const tail = msg.isComplete
    ? message
    : message.querySelector<HTMLElement>('.ds-cursor, .ds-generating, .ds-loading')
      || thinkingBody
      || message
  const overflow = tail.getBoundingClientRect().bottom - (list.getBoundingClientRect().bottom - 24)
  if (overflow > 0) {
    list.scrollTop += overflow
    programmaticScrollTop = list.scrollTop
  }
  showScrollButton.value = list.scrollHeight - list.scrollTop - list.clientHeight > SCROLL_THRESHOLD
}

watch(activeQuestionId, () => {})

watch(activeMode, (mode) => {
  if (mode === 'publishAssistant') {
    publishCatalogPanelVisible.value = true
    ensurePublishChannelsLoaded()
  }
})

onMounted(async () => {
  await initAuth()
  if (isLoggedIn.value) {
    loadConversations()
    if (activeMode.value === 'publishAssistant') {
      ensurePublishChannelsLoaded()
    }
  }
})

function yieldToBrowser(): Promise<void> {
  return new Promise(r => setTimeout(r, 0))
}

function extractDomain(url: string): string {
  try { return new URL(url).hostname } catch { return '' }
}

function parseEventData(dataText: string) {
  try { return dataText ? JSON.parse(dataText) : {} } catch { return {} }
}

function parseReferences(raw: unknown): Reference[] {
  if (!raw) return []
  if (Array.isArray(raw)) return normalizeNoteReferences(raw)
  const rawText = String(raw)
  const trimmed = rawText.trim()
  if (!trimmed) return []
  if (trimmed.startsWith('[')) {
    try {
      const parsed = JSON.parse(trimmed)
      if (Array.isArray(parsed)) return normalizeNoteReferences(parsed)
    } catch { /* fallback to web text parsing */ }
  }
  const refs: Reference[] = []
  const lines = rawText.split('\n')
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    const linkIdx = line.indexOf('链接：')
    if (linkIdx === -1) continue
    const url = line.substring(linkIdx + 3).trim()
    if (!url) continue
    const ref: Reference = { type: 'web', url }
    if (i > 0 && /^\d+\./.test(lines[i - 1])) {
      ref.title = lines[i - 1].replace(/^\d+\.\s*/, '').trim()
    }
    if (i + 1 < lines.length) {
      const nextLine = lines[i + 1]
      const absIdx = nextLine.indexOf('摘要：')
      if (absIdx !== -1) {
        ref.abstract = nextLine.substring(absIdx + 3).trim()
      }
    }
    refs.push(ref)
  }
  return refs
}

function normalizeNoteReferences(raw: unknown[]): Reference[] {
  return raw
    .map((item: any) => ({
      type: 'note' as const,
      noteId: item?.noteId,
      parentId: item?.parentId,
      title: String(item?.title || '未命名笔记').trim(),
      topicName: String(item?.topicName || '').trim(),
      creatorName: String(item?.creatorName || '').trim(),
      cover: String(item?.cover || '').trim(),
      snippet: String(item?.snippet || '').trim(),
      citationNo: Number.isInteger(Number(item?.citationNo)) ? Number(item.citationNo) : undefined,
      score: typeof item?.score === 'number' ? item.score : Number(item?.score),
      matchedQuery: String(item?.matchedQuery || '').trim()
    }))
    .filter((item: Reference) => item.noteId || item.parentId || item.title || item.snippet)
}

function normalizePublishOptions(raw: unknown): PublishChannelOption[] {
  if (!Array.isArray(raw)) return []
  return raw
    .map((channel: any) => ({
      channelId: channel?.channelId ?? '',
      channelName: String(channel?.channelName || '').trim(),
      topics: Array.isArray(channel?.topics)
        ? channel.topics
          .map((topic: any) => ({
            topicId: topic?.topicId ?? '',
            topicName: String(topic?.topicName || '').trim()
          }))
          .filter((topic: PublishTopicOption) => topic.topicName)
        : []
    }))
    .filter((channel: PublishChannelOption) => channel.channelName)
}

function normalizePublishThemeOptions(raw: unknown): string[] {
  if (!Array.isArray(raw)) return []
  return raw
    .map((item) => String(item || '').trim())
    .filter(Boolean)
}

function getLastUserPrompt() {
  const lastUser = [...messages.value].reverse().find((msg) => msg.role === 'user' && msg.content)
  return (lastUser?.content || '').replace(/\n\n\[已添加\s+\d+\s+张图片\]/, '').trim()
}

function selectPublishOption(channel: PublishChannelOption, topic: PublishTopicOption) {
  if (!topic.topicName) return
  if (!publishChannels.value.some(item => String(item.id) === String(channel.channelId))) {
    publishChannels.value.push({
      id: channel.channelId,
      name: channel.channelName
    })
  }
  publishTopics.value = channel.topics.map(item => ({
    id: item.topicId,
    name: item.topicName
  }))
  selectedPublishChannelId.value = channel.channelId
  selectedPublishTopicId.value = topic.topicId
  publishCatalogPanelVisible.value = true
  const basePrompt = getLastUserPrompt()
  const prefix = basePrompt ? `继续刚才的发布需求：${basePrompt}\n` : ''
  input.value = `${prefix}发布到「${channel.channelName}」频道，话题选「${topic.topicName}」。`
  reusePendingPublishImagesOnce.value = pendingPublishImageUrls.value.length > 0
  nextTick(() => scrollBottom(true))
}

function selectPublishTheme(theme: string) {
  if (!theme) return
  const basePrompt = getLastUserPrompt()
  const prefix = basePrompt ? `继续刚才的发布需求：${basePrompt}\n` : ''
  input.value = `${prefix}文章方向选「${theme}」，具体主题是：`
  reusePendingPublishImagesOnce.value = pendingPublishImageUrls.value.length > 0
  nextTick(() => scrollBottom(true))
}

async function readSSE(stream: ReadableStream<Uint8Array>, aiMsg: Message) {
  const reader = stream.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = '', pendingData = ''
  let pendingRefs: Reference[] = []

  const dispatch = async (payload: Record<string, any>) => {
    if (stopped.value) return
    const type = payload.type as string
    if (!type) return

    switch (type) {
      case 'thinking':
        appendThinking(aiMsg, payload.content || '')
        break
      case 'step':
        appendThinking(aiMsg, payload.content || '', true)
        break
      case 'text':
        aiMsg.content += payload.content || ''
        aiMsg._html = renderMarkdown(aiMsg.content, aiMsg.references.length > 0 ? aiMsg.id : '')
        break
      case 'reference':
        pendingRefs = parseReferences(payload.content || '')
        aiMsg.references = pendingRefs
        break
      case 'recommend': aiMsg.recommends = payload.content || []; break
      case 'publishOptions':
        aiMsg.publishOptions = normalizePublishOptions(payload.content)
        break
      case 'publishThemeOptions':
        aiMsg.publishThemeOptions = normalizePublishThemeOptions(payload.content)
        break
      case 'complete':
        aiMsg.status = payload.status || 'COMPLETED'
        aiMsg.references = pendingRefs
        finalizeMessage(aiMsg)
        break
      case 'error': aiMsg.status = 'FAILED'; aiMsg.error = payload.message || payload.content || '服务异常'; finalizeMessage(aiMsg); break
    }
    await followStreamOutput(aiMsg)
    await yieldToBrowser()
  }

  while (true) {
    const { done, value } = await reader.read()
    if (value) {
      buffer += decoder.decode(value, { stream: true })
      while (true) {
        const idx = buffer.indexOf('\n')
        if (idx < 0) break
        let line = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 1)
        if (line.endsWith('\r')) line = line.slice(0, -1)
        if (!line) { if (pendingData) { await dispatch(parseEventData(pendingData)); pendingData = '' } continue }
        if (line.startsWith(':')) continue
        if (line.startsWith('data:')) pendingData += line.slice(5).trim()
      }
    }
    if (done) break
  }
  if (pendingData.trim()) await dispatch(parseEventData(pendingData))
}

async function uploadSelectedImageUrls(aiMsg: Message, signal: AbortSignal): Promise<string[]> {
  const images = [...selectedImages.value]
  const imageUrls: string[] = []
  appendThinking(aiMsg, `### 生成过程\n- 正在上传 ${images.length} 张图片...\n`)
  await followStreamOutput(aiMsg)

  for (let i = 0; i < images.length; i++) {
    if (signal.aborted) throw new DOMException('Aborted', 'AbortError')
    const image = images[i]
    const res: any = await uploadFile(image.file, { signal })
    const url = typeof res?.data === 'string' ? res.data : (typeof res === 'string' ? res : '')
    if (!url) throw new Error(`第 ${i + 1} 张图片上传失败`)
    imageUrls.push(url)
    appendThinking(aiMsg, `第 ${i + 1} 张图片上传完成。`, true)
    await followStreamOutput(aiMsg)
  }

  return imageUrls
}

async function send() {
  if (!requireLogin()) return
  const text = input.value.trim()
  const isImageWritingMode = activeMode.value === 'imageWriting'
  const isPublishAssistantMode = activeMode.value === 'publishAssistant'
  if (loading.value) return
  if (isPublishAssistantMode && !text) {
    ElMessage.warning('请先输入要发布的笔记需求')
    return
  }
  if (!text && !isImageWritingMode) return
  if (isImageWritingMode && selectedImages.value.length === 0) {
    ElMessage.warning('请先添加图片')
    return
  }

  const prompt = text || '请根据图片生成一段知识分享正文'
  const imageCount = selectedImages.value.length

  stopped.value = false
  addUserMessage(imageAttachVisible.value && imageCount > 0 ? `${prompt}\n\n[已添加 ${imageCount} 张图片]` : prompt)
  const aiMsg = addAssistantMessage()
  input.value = ''
  loading.value = true
  manualScrollLock.value = false
  await followStreamOutput(aiMsg)

  const abort = new AbortController()
  controller.value = abort

  try {
    const isPptMode = activeMode.value === 'ppt'
    let resp: Response
    if (isPptMode) {
      resp = await createPptStream(prompt, conversationId.value, abort.signal)
    } else if (isImageWritingMode) {
      const imageUrls = await uploadSelectedImageUrls(aiMsg, abort.signal)
      const userMessage = [...messages.value].reverse().find(m => m.role === 'user')
      if (userMessage) userMessage.imageUrls = imageUrls
      clearImages()
      resp = await createImageWritingStream({
        query: prompt,
        conversationId: conversationId.value,
        imageUrls
      }, abort.signal)
    } else if (isPublishAssistantMode) {
      let imageUrls: string[] = []
      if (selectedImages.value.length) {
        imageUrls = await uploadSelectedImageUrls(aiMsg, abort.signal)
        pendingPublishImageUrls.value = imageUrls
        reusePendingPublishImagesOnce.value = false
      } else if (reusePendingPublishImagesOnce.value) {
        imageUrls = [...pendingPublishImageUrls.value]
        reusePendingPublishImagesOnce.value = false
        appendThinking(aiMsg, `### 生成过程\n- 已沿用上一次上传的 ${imageUrls.length} 张图片。\n`)
      }
      const userMessage = [...messages.value].reverse().find(m => m.role === 'user')
      if (userMessage) userMessage.imageUrls = imageUrls
      clearImages()
      resp = await createPublishAssistantStream({
        query: prompt,
        conversationId: conversationId.value,
        imageUrls,
        ...getPublishSelectionPayload()
      }, abort.signal)
    } else if (activeMode.value === 'noteRag') {
      resp = await createNoteRagStream(prompt, conversationId.value, abort.signal)
    } else {
      resp = await createChatStream(prompt, conversationId.value, abort.signal)
    }
    if (!resp.ok || !resp.body) throw new Error(`请求失败: ${resp.status}`)
    await readSSE(resp.body, aiMsg)
    if (!aiMsg.content && !aiMsg.thinking && !aiMsg.error) aiMsg.content = '未获取到内容，请重试。'
    if (isPublishAssistantMode
        && (aiMsg.content.startsWith('# 发布助手预览')
          || aiMsg.content.startsWith('# 发布助手发布成功'))) {
      clearPendingPublishContext()
    }
  } catch (e) {
    const err = e as Error
    aiMsg.status = err.name === 'AbortError' ? 'STOPPED' : 'FAILED'
    aiMsg.error = err.name === 'AbortError' ? '已停止生成' : (err.message || '未知异常')
    ElMessage.error(aiMsg.error)
  } finally {
    if (!aiMsg.isComplete) finalizeMessage(aiMsg)
    loading.value = false
    controller.value = null
    await followStreamOutput(aiMsg)
    // 新会话同步侧边栏
    syncConversationList()
  }
}

async function syncConversationList() {
  await loadConversations()
}

function stop() {
  if (!loading.value) return
  stopped.value = true
  stopChatStream(conversationId.value).finally(() => {
    controller.value?.abort()
  })
}

async function clearAll() {
  if (!requireLogin()) return
  controller.value?.abort()
  try {
    await deleteAllConversations()
  } catch {
    // error toast handled by axios interceptor
  }
  conversations.value = []
  messages.value = []
  input.value = ''
  clearImages()
  clearPendingPublishContext()
  clearPublishSelection()
  thinkingExpanded.value = {}
  stopped.value = false
  conversationId.value = `conv-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function formatTime(ts: string): string {
  if (!ts) return ''
  const d = new Date(ts)
  if (isNaN(d.getTime())) return ts
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${d.getMonth() + 1}/${d.getDate()}`
}
</script>

<template>
  <div class="ds-page">
    <!-- ========== 左侧边栏：平台信息 + 会话记录 ========== -->
    <aside class="ds-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <button class="ds-sidebar-brand" type="button" title="返回首页" @click="router.push('/')">
        <div class="ds-logo">
          <AiKnowledgeMark />
        </div>
        <div class="ds-sidebar-title">
          <span class="ds-sidebar-name">雨绵绵</span>
          <span class="ds-sidebar-subtitle">知识助手</span>
        </div>
      </button>

      <button class="ds-new-conv-btn" :disabled="!isLoggedIn || loading" @click="startNewConversation">
        <span class="ds-new-conv-icon"><AppIcon name="plus" :size="18" /></span>
        <span>新对话</span>
      </button>

      <div class="ds-conv-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          :class="['ds-conv-item', { active: conv.id === conversationId }]"
          @click="switchConversation(conv)"
        >
          <div class="ds-conv-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
          </div>
          <div class="ds-conv-info">
            <div class="ds-conv-title">{{ conv.title }}</div>
            <div class="ds-conv-time">{{ formatTime(conv.createdTime) }}</div>
          </div>
          <button class="ds-conv-delete" @click="deleteConversation(conv.id, $event)" title="删除会话">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
            </svg>
          </button>
        </div>
        <div v-if="!conversations.length" class="ds-conv-empty">暂无会话记录</div>
      </div>

      <div class="ds-sidebar-footer">
        <button class="ds-clear-all-btn" :disabled="!isLoggedIn || loading" @click="clearAll">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
          </svg>
          清空全部
        </button>
      </div>
    </aside>

    <!-- ========== 侧边栏折叠按钮 ========== -->
    <button class="ds-toggle-sidebar" @click="toggleSidebar" :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <polyline v-if="sidebarCollapsed" points="9 18 15 12 9 6"/>
        <polyline v-else points="15 18 9 12 15 6"/>
      </svg>
    </button>

    <!-- ========== 中间：对话区 ========== -->
    <main class="ds-main">
      <div ref="listRef" class="ds-chat" @scroll="onChatScroll">
        <div v-if="!messages.length" class="ds-welcome">
          <div class="ds-welcome-mark"><AiKnowledgeMark /></div>
          <p class="ds-welcome-kicker">雨绵绵知识助手</p>
          <h1 class="ds-welcome-title">从经验里，找到有出处的答案。</h1>
          <p class="ds-welcome-desc">检索站内知识、连接公开信息，也可以帮你把想法整理成作品。</p>
        </div>

        <div v-for="msg in messages" :key="msg.id" :id="`msg-${msg.id}`" :class="['ds-msg', msg.role]">
          <div class="ds-msg-inner">
            <template v-if="msg.role === 'user'">
              <div class="ds-user-bubble">{{ msg.content }}</div>
              <div v-if="msg.imageUrls?.length" class="ds-history-images">
                <a v-for="url in msg.imageUrls" :key="url" :href="url" target="_blank" rel="noopener noreferrer">
                  <img :src="url" alt="本轮上传的图片" loading="lazy" />
                </a>
              </div>
            </template>

            <template v-else>
              <div v-if="msg.thinking" class="ds-thinking">
                <div class="ds-thinking-bar" @click="toggleThinking(msg.id)">
                  <span class="ds-thinking-pulse" v-if="!msg.isComplete"></span>
                  <span class="ds-thinking-arrow"><AppIcon name="chevron" :size="12" :style="{ transform: thinkingExpanded[msg.id] ? 'rotate(90deg)' : undefined }" /></span>
                  <span>生成过程</span>
                  <span class="ds-thinking-state">{{ msg.isComplete ? '已完成' : '进行中' }}</span>
                </div>
                <div
                  v-if="thinkingExpanded[msg.id] || !msg.isComplete"
                  class="ds-thinking-body ds-md"
                  v-html="msg._thinkingHtml || renderMarkdown(msg.thinking)"
                ></div>
              </div>

              <div v-if="!msg.content && !msg.thinking && !msg.isComplete && !msg.error" class="ds-loading">
                <span class="ds-loading-dot"></span>
                <span class="ds-loading-dot"></span>
                <span class="ds-loading-dot"></span>
              </div>

              <div v-else-if="!msg.content && !msg.isComplete && !msg.error" class="ds-generating">
                <span class="ds-generating-spinner"></span>
                <span>{{ getGeneratingText(msg) }}</span>
                <span class="ds-generating-dots">
                  <span></span>
                  <span></span>
                  <span></span>
                </span>
              </div>

              <div v-if="msg.content" :class="['ds-content', { 'ds-result-content': msg.content.startsWith('# ') }]">
                <div class="ds-md" v-html="msg._html || renderMarkdown(msg.content, msg.references.length > 0 ? msg.id : '')"></div>
                <span v-if="!msg.isComplete" class="ds-cursor">|</span>
              </div>

              <div v-if="msg.isComplete && msg.publishThemeOptions.length" class="ds-theme-options">
                <div class="ds-theme-options-head">
                  <span class="ds-theme-options-title">选择文章方向</span>
                  <span class="ds-theme-options-subtitle">选好后继续补充具体主题</span>
                </div>
                <div class="ds-theme-option-list">
                  <button
                    v-for="theme in msg.publishThemeOptions"
                    :key="theme"
                    type="button"
                    class="ds-theme-option"
                    @click="selectPublishTheme(theme)"
                  >
                    {{ theme }}
                  </button>
                </div>
              </div>

              <div v-if="msg.isComplete && msg.publishOptions.length" class="ds-publish-options">
                <div class="ds-publish-options-head">
                  <span class="ds-publish-options-title">选择频道和话题</span>
                  <span class="ds-publish-options-subtitle">选好后以该位置继续生成</span>
                </div>
                <div class="ds-publish-option-list">
                  <div v-for="channel in msg.publishOptions" :key="channel.channelId" class="ds-publish-option-group">
                    <div class="ds-publish-channel">
                      <span class="ds-publish-channel-dot"></span>
                      <span>{{ channel.channelName }}</span>
                    </div>
                    <div v-if="channel.topics.length" class="ds-publish-topic-list">
                      <button
                        v-for="topic in channel.topics"
                        :key="topic.topicId"
                        type="button"
                        class="ds-publish-topic"
                        @click="selectPublishOption(channel, topic)"
                      >
                        {{ topic.topicName }}
                      </button>
                    </div>
                    <div v-else class="ds-publish-topic-empty">暂无话题</div>
                  </div>
                </div>
              </div>

              <div v-if="msg.references.length" class="ds-refs">
                <div class="ds-refs-title">参考资料</div>
                <ol class="ds-ref-list">
                  <li
                    v-for="(r, i) in msg.references"
                    :id="`rag-reference-${msg.id}-${r.citationNo || i + 1}`"
                    :key="i"
                    class="ds-ref-item">
                    <template v-if="r.type === 'note'">
                      <div class="ds-note-ref">
                        <img v-if="r.cover" :src="r.cover" alt="" class="ds-note-ref-cover" />
                        <div class="ds-note-ref-body">
                          <div class="ds-note-ref-main">
                            <span class="ds-note-ref-citation">[{{ r.citationNo || i + 1 }}]</span>
                            <span class="ds-note-ref-title">{{ r.title || '未命名笔记' }}</span>
                          </div>
                          <p v-if="r.snippet" class="ds-note-ref-snippet">{{ r.snippet }}</p>
                          <div class="ds-note-ref-meta">
                            <span v-if="r.topicName"># {{ r.topicName }}</span>
                            <span v-if="r.creatorName">{{ r.creatorName }}</span>
                            <span v-if="r.noteId">ID {{ r.noteId }}</span>
                          </div>
                        </div>
                      </div>
                    </template>
                    <template v-else>
                      <p v-if="r.abstract" class="ds-ref-abstract">{{ r.abstract }}</p>
                      <a v-if="r.url" :href="r.url" target="_blank" rel="noopener noreferrer" class="ds-ref-link">
                        <span class="ds-ref-title">{{ r.title || r.url }}</span>
                        <span class="ds-ref-domain">{{ extractDomain(r.url) }}</span>
                      </a>
                    </template>
                  </li>
                </ol>
              </div>

              <div v-if="msg.isComplete && msg.recommends.length" class="ds-recommends">
                <div class="ds-recommends-title">推荐问题</div>
                <div v-for="(item, i) in msg.recommends" :key="i" class="ds-recommend-item" @click="pickPrompt(item)">{{ item }}</div>
              </div>

              <a v-if="msg.fileUrl && !msg.content.includes(msg.fileUrl)" :href="msg.fileUrl" target="_blank" rel="noopener noreferrer">下载 PPT</a>
              <div v-if="statusLabel(msg.status) && !msg.error" class="ds-message-status">{{ statusLabel(msg.status) }}</div>
              <div v-if="msg.publishOutcome === 'SUBMITTING'" class="ds-message-status">发布请求已发出，请先到笔记管理确认结果，避免重复发布。</div>
              <div v-if="msg.error" class="ds-error">{{ msg.error }}</div>
            </template>
          </div>
        </div>

        <div class="ds-spacer"></div>

        <button
          v-show="showScrollButton || loading"
          class="ds-scroll-btn"
          @click="scrollBottom(true)"
        ><AppIcon name="down" :size="18" /></button>
      </div>

      <!-- ===== 底部输入区 ===== -->
      <div class="ds-footer">
        <div class="ds-mode-row">
          <button
            v-for="mode in chatModes"
            :key="mode.key"
            :class="['ds-mode-chip', { active: activeMode === mode.key }]"
            :disabled="!isLoggedIn || loading"
            @click="switchMode(mode.key)"
          >
            <span class="ds-mode-icon"><AiModeIcon :name="mode.key" /></span>
            <span>{{ mode.label }}</span>
          </button>
        </div>

        <div class="ds-input-box">
          <div v-if="activeMode === 'publishAssistant'" class="ds-publish-catalog-popover">
            <div v-if="publishCatalogPanelVisible" class="ds-publish-catalog-panel">
              <div class="ds-publish-catalog-head">
                <div class="ds-publish-catalog-title">
                  <span class="ds-publish-catalog-dot"></span>
                  <span>发布位置</span>
                  <span class="ds-publish-selection-pill">{{ publishSelectionLabel }}</span>
                </div>
                <div class="ds-publish-catalog-tools">
                  <button type="button" class="ds-publish-tool-btn" :disabled="publishCatalogLoading" @click="ensurePublishChannelsLoaded(true)">
                    {{ publishCatalogLoading ? '刷新中' : '刷新' }}
                  </button>
                  <button type="button" class="ds-publish-tool-btn" :disabled="!selectedPublishChannelId" @click="clearPublishSelection">清空</button>
                  <button type="button" class="ds-publish-tool-btn" @click="publishCatalogPanelVisible = false">收起</button>
                </div>
              </div>

              <div class="ds-publish-catalog-fields">
                <label class="ds-publish-catalog-field">
                  <span>频道</span>
                  <select
                    v-model="selectedPublishChannelId"
                    class="ds-publish-select"
                    :disabled="publishCatalogLoading"
                    @change="handlePublishChannelChange"
                  >
                    <option value="">{{ publishCatalogLoading ? '正在读取频道...' : '先选择频道' }}</option>
                    <option v-for="channel in publishChannels" :key="channel.id" :value="channel.id">{{ channel.name }}</option>
                  </select>
                </label>

                <label class="ds-publish-catalog-field">
                  <span>话题</span>
                  <select
                    v-model="selectedPublishTopicId"
                    class="ds-publish-select"
                    :disabled="!selectedPublishChannelId || publishTopicLoading || !publishTopics.length"
                  >
                    <option value="">
                      {{ publishTopicLoading ? '正在读取话题...' : (selectedPublishChannelId ? '再选择话题' : '先选频道') }}
                    </option>
                    <option v-for="topic in publishTopics" :key="topic.id" :value="topic.id">{{ topic.name }}</option>
                  </select>
                </label>
              </div>
            </div>

            <button v-else type="button" class="ds-publish-catalog-collapsed" @click="publishCatalogPanelVisible = true">
              <span>发布位置</span>
              <strong>{{ publishSelectionLabel }}</strong>
            </button>
          </div>

          <div v-if="imageAttachVisible" class="ds-image-attach">
            <div v-for="image in selectedImages" :key="image.id" class="ds-image-thumb">
              <img :src="image.previewUrl" :alt="image.file.name" />
              <button class="ds-image-remove" type="button" @click="removeImage(image.id)" title="移除图片"><AppIcon name="close" :size="12" /></button>
            </div>

            <button
              v-if="selectedImages.length < MAX_IMAGE_COUNT"
              class="ds-image-add"
              type="button"
              :disabled="!isLoggedIn"
              @click="triggerImagePicker"
            >
              <span class="ds-image-add-icon"><AppIcon name="plus" :size="18" /></span>
              <span>添加图片</span>
            </button>

            <span class="ds-image-count">{{ selectedImages.length }}/{{ MAX_IMAGE_COUNT }}</span>
            <span v-if="activeMode === 'publishAssistant' && !selectedImages.length" class="ds-image-note">不传图时会自动生成 3 张配图</span>
            <input
              ref="imageInputRef"
              type="file"
              accept="image/*"
              multiple
              hidden
              @change="handleImageChange"
            />
          </div>

          <textarea
            v-model="input"
            class="ds-textarea"
            :rows="1"
            :disabled="!isLoggedIn"
            :placeholder="inputPlaceholder"
            @keydown.enter.exact.prevent="send"
            @keydown.shift.enter.prevent="input += '\n'"
          ></textarea>
          <div class="ds-input-actions">
            <span class="ds-hint">Shift + Enter 换行</span>
            <div class="ds-btn-group">
              <button v-if="loading" class="ds-btn-stop" @click="stop">停止</button>
              <button class="ds-btn-send" :disabled="!canSend" @click="send">
                <span class="ds-send-icon"><AppIcon name="up" :size="19" /></span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- ========== 右侧悬停面板：问题定位 ========== -->
    <div class="ds-question-nav">
      <div class="ds-question-nav-trigger">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/>
        </svg>
      </div>
      <div class="ds-question-nav-body">
        <div class="ds-question-nav-body-header">对话导航</div>
        <div v-if="!userQuestions.length" class="ds-question-nav-empty">
          <span>暂无提问</span>
        </div>
        <div
          v-for="(q, idx) in userQuestions"
          :key="q.id"
          :class="['ds-question-nav-item', { active: activeQuestionId === q.id }]"
          @click="scrollToMessage(q.id); activeQuestionId = q.id"
        >
          <span class="ds-question-nav-index">{{ idx + 1 }}</span>
          <span class="ds-question-nav-text">{{ q.content.slice(0, 28) }}{{ q.content.length > 28 ? '...' : '' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ========================================
   AI Chat Page — 三栏布局
   ======================================== */

/* ===== base ===== */
.ds-page {
  display: flex;
  height: 100vh;
  background: #f5f6fa;
  color: #1a1a1a;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow: hidden;
}

/* ===== 左侧边栏 ===== */
.ds-sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #eee;
  transition: width 0.25s ease, opacity 0.25s ease, padding 0.25s ease;
  overflow: hidden;
}
.ds-sidebar.collapsed {
  width: 0;
  padding: 0;
  border-right: none;
  opacity: 0;
  pointer-events: none;
}

/* 品牌区 */
.ds-sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 16px 12px;
  flex-shrink: 0;
}
.ds-logo {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  border-radius: 12px;
  overflow: hidden;
}
.ds-logo-img { width: 100%; height: 100%; object-fit: cover; display: block; }
.ds-sidebar-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}
.ds-sidebar-name {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
  white-space: nowrap;
}
.ds-sidebar-subtitle {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

/* 新对话按钮 */
.ds-new-conv-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 4px 16px 12px;
  padding: 10px 0;
  border: 1px dashed #d0d5e0;
  border-radius: 12px;
  background: #fafbfc;
  font-size: 14px;
  color: #4f6bf7;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.ds-new-conv-btn:hover { background: #f0f2ff; border-color: #4f6bf7; }
.ds-new-conv-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.ds-new-conv-icon { font-size: 18px; font-weight: 300; }

/* 会话列表 */
.ds-conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}
.ds-conv-empty {
  text-align: center;
  padding: 40px 16px;
  font-size: 13px;
  color: #bbb;
}
.ds-conv-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
  margin-bottom: 2px;
}
.ds-conv-item:hover { background: #f5f6fa; }
.ds-conv-item.active { background: #eef0ff; }
.ds-conv-icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
}
.ds-conv-item.active .ds-conv-icon {
  background: #dde0ff;
  color: #4f6bf7;
}
.ds-conv-info {
  flex: 1;
  min-width: 0;
}
.ds-conv-title {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ds-conv-item.active .ds-conv-title { color: #1a1a1a; font-weight: 500; }
.ds-conv-time {
  font-size: 11px;
  color: #bbb;
  margin-top: 2px;
}
.ds-conv-delete {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #ccc;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.15s;
}
.ds-conv-item:hover .ds-conv-delete { opacity: 1; }
.ds-conv-delete:hover { background: #fee; color: #e74c3c; }

/* 侧边栏底部 */
.ds-sidebar-footer {
  flex-shrink: 0;
  padding: 10px 16px;
  border-top: 1px solid #f0f0f0;
}
.ds-clear-all-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 8px 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  font-size: 12px;
  color: #bbb;
  cursor: pointer;
  transition: all 0.15s;
}
.ds-clear-all-btn:hover { background: #f5f5f5; color: #e74c3c; }
.ds-clear-all-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* ===== 侧边栏折叠按钮 ===== */
.ds-toggle-sidebar {
  position: absolute;
  left: 280px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 48px;
  border: 1px solid #e0e0e0;
  border-left: none;
  border-radius: 0 8px 8px 0;
  background: #fff;
  color: #999;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20;
  transition: left 0.25s ease;
  padding: 0;
}
.ds-sidebar.collapsed ~ .ds-toggle-sidebar {
  left: 0;
}

/* ===== 中间主区域 ===== */
.ds-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.ds-chat {
  flex: 1;
  overflow-y: auto;
  padding: 20px 32px;
  overflow-anchor: none;
}
.ds-spacer { height: 40px; }

/* scroll-to-bottom button */
.ds-scroll-btn {
  position: sticky;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  width: 36px;
  height: 36px;
  border: 1px solid #e0e0e0;
  border-radius: 50%;
  background: #fff;
  color: #555;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.2s;
  z-index: 10;
}
.ds-scroll-btn:hover {
  border-color: #4f6bf7;
  color: #4f6bf7;
  box-shadow: 0 4px 12px rgba(79, 107, 247, 0.15);
}

/* ===== welcome ===== */
.ds-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 18vh;
}
.ds-welcome-title {
  font-size: 28px;
  font-weight: 600;
  color: #222;
  margin: 0 0 8px;
}
.ds-welcome-desc {
  font-size: 14px;
  color: #999;
  margin: 0;
}

/* ===== message row ===== */
.ds-msg {
  display: flex;
  justify-content: center;
  padding: 12px 0;
  scroll-margin-top: 12px;
  transition: background 0.3s;
}
.ds-msg.msg-flash {
  animation: msgFlash 1.5s ease-out;
}
@keyframes msgFlash {
  0% { background: rgba(79, 107, 247, 0.08); }
  100% { background: transparent; }
}

.ds-msg-inner {
  width: min(820px, 100%);
}

/* user */
.ds-msg.user .ds-msg-inner {
  display: flex;
  justify-content: flex-end;
}
.ds-user-bubble {
  max-width: 75%;
  padding: 10px 16px;
  border-radius: 16px 16px 4px 16px;
  background: #eef0ff;
  color: #1a1a2e;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

/* ===== thinking ===== */
.ds-thinking {
  margin-bottom: 14px;
  border: 1px solid #dfe6ff;
  border-radius: 12px;
  background: #f7f9ff;
  overflow: hidden;
}
.ds-thinking-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  background: #eef3ff;
  font-size: 12px;
  font-weight: 600;
  color: #3154b8;
  cursor: pointer;
  user-select: none;
}
.ds-thinking-bar:hover { background: #e6edff; }
.ds-thinking-pulse {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #4f6bf7;
  box-shadow: 0 0 0 0 rgba(79, 107, 247, 0.35);
  animation: thinkingPulse 1.4s ease-out infinite;
}
@keyframes thinkingPulse {
  0% { box-shadow: 0 0 0 0 rgba(79, 107, 247, 0.35); }
  100% { box-shadow: 0 0 0 8px rgba(79, 107, 247, 0); }
}
.ds-thinking-arrow { font-size: 10px; color: #6a7fbf; }
.ds-thinking-state {
  margin-left: auto;
  color: #7f8eb8;
  font-weight: 500;
}
.ds-thinking-body {
  padding: 12px 16px;
  font-size: 13px;
  color: #405070;
  line-height: 1.6;
  max-height: 260px;
  overflow-y: auto;
}
.ds-thinking-body :deep(h3) {
  margin-top: 0;
}

/* ===== loading ===== */
.ds-loading {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 4px 0;
}
.ds-loading-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #6c5ce7;
  animation: ds-bounce 1.2s ease-in-out infinite;
}
.ds-loading-dot:nth-child(1) { animation-delay: 0s; }
.ds-loading-dot:nth-child(2) { animation-delay: 0.2s; }
.ds-loading-dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes ds-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.3; }
  30% { transform: translateY(-6px); opacity: 1; }
}
.ds-generating {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 2px 0 12px;
  padding: 8px 12px;
  border: 1px solid #e3e8f8;
  border-radius: 8px;
  background: #fbfcff;
  color: #52617f;
  font-size: 13px;
}
.ds-generating-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid #d8e0fb;
  border-top-color: #4f6bf7;
  border-radius: 50%;
  animation: ds-spin 0.9s linear infinite;
}
.ds-generating-dots {
  display: inline-flex;
  gap: 3px;
  align-items: center;
}
.ds-generating-dots span {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #8ea0d8;
  animation: ds-dotFade 1.2s ease-in-out infinite;
}
.ds-generating-dots span:nth-child(2) { animation-delay: 0.18s; }
.ds-generating-dots span:nth-child(3) { animation-delay: 0.36s; }
@keyframes ds-spin {
  to { transform: rotate(360deg); }
}
@keyframes ds-dotFade {
  0%, 80%, 100% { opacity: 0.3; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-2px); }
}

/* ===== content ===== */
.ds-content {
  font-size: 14px;
  line-height: 1.7;
  color: #222;
}
.ds-result-content {
  padding: 16px 18px;
  border: 1px solid #e4e8f3;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 6px 20px rgba(25, 40, 80, 0.06);
}
.ds-cursor {
  display: inline;
  color: #6c5ce7;
  font-weight: 700;
  animation: ds-blink 0.8s step-end infinite;
}
@keyframes ds-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
.ds-md { word-break: break-word; }

/* ===== references ===== */
.ds-refs {
  margin-top: 14px;
  padding: 10px 14px;
  border-radius: 10px;
  background: #f8f9fc;
  border: 1px solid #eef0f5;
}
.ds-refs-title {
  font-size: 12px;
  font-weight: 600;
  color: #999;
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.ds-ref-list { list-style: none; padding: 0; margin: 0; }
.ds-ref-item { padding: 10px 0; }
.ds-ref-item + .ds-ref-item { border-top: 1px solid #f0f0f0; }
.ds-ref-abstract {
  margin: 0 0 6px;
  font-size: 12px;
  color: #888;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ds-ref-link {
  display: flex;
  align-items: baseline;
  gap: 8px;
  text-decoration: none;
  font-size: 13px;
}
.ds-ref-title { color: #4f6bf7; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ds-ref-title:hover { text-decoration: underline; }
.ds-ref-domain { color: #bbb; font-size: 11px; white-space: nowrap; flex-shrink: 0; }
.ds-note-ref {
  display: flex;
  gap: 10px;
  min-width: 0;
}
.ds-note-ref-cover {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  border-radius: 8px;
  object-fit: cover;
  background: #eef0f5;
}
.ds-note-ref-body {
  min-width: 0;
  flex: 1;
}
.ds-note-ref-main {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.ds-note-ref-title {
  color: #2f3b66;
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ds-note-ref-citation {
  flex-shrink: 0;
  color: #4f6bf7;
  font-weight: 700;
}
.ds-note-ref-score {
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 999px;
  background: #eef4ff;
  color: #4f6bf7;
  font-size: 11px;
  font-weight: 600;
}
.ds-note-ref-snippet {
  margin: 5px 0 0;
  color: #6f778a;
  font-size: 12px;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.ds-note-ref-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 7px;
  color: #a0a7b7;
  font-size: 11px;
}

/* ===== theme options ===== */
.ds-theme-options {
  margin-top: 14px;
  padding: 12px;
  border: 1px solid #e4eadf;
  border-radius: 8px;
  background: #fcfdf9;
}
.ds-theme-options-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}
.ds-theme-options-title {
  font-size: 13px;
  font-weight: 700;
  color: #2d3a28;
}
.ds-theme-options-subtitle {
  font-size: 12px;
  color: #8d9a86;
}
.ds-theme-option-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.ds-theme-option {
  min-height: 32px;
  padding: 6px 12px;
  border: 1px solid #dce8d5;
  border-radius: 999px;
  background: #f6fbf1;
  color: #426632;
  font-size: 12px;
  line-height: 1.4;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.16s, background 0.16s, color 0.16s;
}
.ds-theme-option:hover {
  border-color: #73a85b;
  background: #eef8e8;
  color: #315922;
}
.ds-theme-option:focus-visible {
  outline: 2px solid rgba(115, 168, 91, 0.35);
  outline-offset: 2px;
}

/* ===== publish options ===== */
.ds-publish-options {
  margin-top: 14px;
  padding: 12px;
  border: 1px solid #e3e8f4;
  border-radius: 8px;
  background: #fbfcff;
}
.ds-publish-options-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}
.ds-publish-options-title {
  font-size: 13px;
  font-weight: 700;
  color: #26324a;
}
.ds-publish-options-subtitle {
  font-size: 12px;
  color: #8c96ad;
}
.ds-publish-option-list {
  display: grid;
  gap: 10px;
}
.ds-publish-option-group {
  display: grid;
  grid-template-columns: 128px 1fr;
  gap: 10px;
  align-items: start;
  padding: 10px;
  border: 1px solid #edf1f8;
  border-radius: 8px;
  background: #fff;
}
.ds-publish-channel {
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 30px;
  font-size: 13px;
  font-weight: 600;
  color: #1f2a3f;
  word-break: break-word;
}
.ds-publish-channel-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #4f6bf7;
  flex-shrink: 0;
}
.ds-publish-topic-list {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}
.ds-publish-topic {
  min-height: 30px;
  padding: 5px 10px;
  border: 1px solid #dfe6f7;
  border-radius: 999px;
  background: #f7f9ff;
  color: #3852a3;
  font-size: 12px;
  line-height: 1.4;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.16s, background 0.16s, color 0.16s;
}
.ds-publish-topic:hover {
  border-color: #4f6bf7;
  background: #eef3ff;
  color: #263da0;
}
.ds-publish-topic:focus-visible {
  outline: 2px solid rgba(79, 107, 247, 0.35);
  outline-offset: 2px;
}
.ds-publish-topic-empty {
  min-height: 30px;
  display: flex;
  align-items: center;
  color: #a7afc2;
  font-size: 12px;
}

/* ===== recommends ===== */
.ds-recommends {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.ds-recommends-title {
  width: 100%;
  font-size: 12px;
  font-weight: 600;
  color: #999;
  margin-bottom: 2px;
}
.ds-recommend-item {
  padding: 6px 14px;
  border-radius: 16px;
  border: 1px solid #e0e0e0;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  transition: all 0.15s;
}
.ds-recommend-item:hover {
  border-color: #4f6bf7;
  color: #4f6bf7;
  background: #f8f9ff;
}

/* ===== error ===== */
.ds-error { margin-top: 8px; font-size: 13px; color: #e74c3c; }

/* ===== mode selector ===== */
.ds-mode-row {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-bottom: 10px;
}
.ds-mode-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: 1px solid #e5e5e5;
  border-radius: 20px;
  background: #fff;
  font-size: 12px;
  color: #888;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.ds-mode-chip:hover:not(:disabled) {
  border-color: #c0c5e0;
  color: #555;
}
.ds-mode-chip.active {
  border-color: #4f6bf7;
  background: #eef0ff;
  color: #4f6bf7;
  font-weight: 500;
}
.ds-mode-chip:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.ds-mode-icon {
  font-size: 14px;
  line-height: 1;
}

/* ===== footer / input ===== */
.ds-footer {
  flex-shrink: 0;
  padding: 0 32px 24px;
  background: #f5f6fa;
}
.ds-input-box {
  max-width: 820px;
  margin: 0 auto;
  border: 1px solid #e0e0e0;
  border-radius: 16px;
  background: #fff;
  padding: 10px 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.ds-input-box:focus-within {
  border-color: #4f6bf7;
  box-shadow: 0 0 0 3px rgba(79, 107, 247, 0.08);
}
.ds-publish-catalog-popover {
  padding-bottom: 10px;
  margin-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}
.ds-publish-catalog-panel {
  border: 1px solid #e5ebff;
  border-radius: 8px;
  background: #fbfcff;
  padding: 10px;
}
.ds-publish-catalog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}
.ds-publish-catalog-title {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #222;
}
.ds-publish-catalog-dot {
  width: 8px;
  height: 8px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #4f6bf7;
}
.ds-publish-selection-pill {
  min-width: 0;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 3px 8px;
  border-radius: 8px;
  background: #eef2ff;
  color: #4b5db8;
  font-size: 12px;
  font-weight: 500;
}
.ds-publish-catalog-tools {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}
.ds-publish-tool-btn {
  border: 1px solid #e1e5f5;
  border-radius: 8px;
  background: #fff;
  color: #536079;
  padding: 5px 9px;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
}
.ds-publish-tool-btn:hover:not(:disabled) {
  border-color: #bfc9ff;
  color: #4f6bf7;
}
.ds-publish-tool-btn:disabled {
  color: #b8bed0;
  cursor: not-allowed;
}
.ds-publish-catalog-fields {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
}
.ds-publish-catalog-field {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 12px;
  color: #7a8196;
}
.ds-publish-select {
  width: 100%;
  min-width: 0;
  height: 34px;
  border: 1px solid #dfe4f5;
  border-radius: 8px;
  background: #fff;
  color: #222;
  font-size: 13px;
  padding: 0 10px;
  outline: none;
}
.ds-publish-select:focus {
  border-color: #4f6bf7;
  box-shadow: 0 0 0 2px rgba(79, 107, 247, 0.08);
}
.ds-publish-select:disabled {
  color: #a8afc2;
  background: #f7f8fc;
  cursor: not-allowed;
}
.ds-publish-catalog-collapsed {
  width: 100%;
  min-height: 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid #e5ebff;
  border-radius: 8px;
  background: #fbfcff;
  color: #6e7690;
  padding: 7px 10px;
  font-size: 12px;
  cursor: pointer;
}
.ds-publish-catalog-collapsed strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #4f6bf7;
  font-size: 12px;
  font-weight: 600;
}
.ds-image-attach {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 10px;
}
.ds-image-thumb {
  position: relative;
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
  background: #f0f0f0;
}
.ds-image-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.ds-image-remove {
  position: absolute;
  top: 3px;
  right: 3px;
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 13px;
  line-height: 18px;
  text-align: center;
  cursor: pointer;
}
.ds-image-add {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  border: 1px dashed #c0c5e0;
  border-radius: 10px;
  background: #fafbff;
  color: #4f6bf7;
  font-size: 11px;
  cursor: pointer;
}
.ds-image-add:hover {
  border-color: #4f6bf7;
  background: #f0f2ff;
}
.ds-image-add-icon {
  font-size: 18px;
  line-height: 1;
}
.ds-image-count {
  font-size: 12px;
  color: #999;
}
.ds-image-note {
  font-size: 12px;
  color: #7a86a8;
  background: #f4f7ff;
  border: 1px solid #e5ebff;
  border-radius: 999px;
  padding: 3px 9px;
}
.ds-textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 14px;
  line-height: 1.6;
  font-family: inherit;
  color: #1a1a1a;
  background: transparent;
  max-height: 160px;
}
.ds-textarea::placeholder { color: #bbb; }
.ds-input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid #f5f5f5;
}
.ds-hint { font-size: 11px; color: #bbb; }
.ds-btn-group { display: flex; gap: 6px; }
.ds-btn-stop {
  padding: 6px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 20px;
  background: #fff;
  font-size: 13px;
  color: #666;
  cursor: pointer;
}
.ds-btn-stop:hover { background: #f5f5f5; }
.ds-btn-send {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: #4f6bf7;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s, transform 0.15s;
}
.ds-btn-send:hover:not(:disabled) {
  background: #3d56d4;
  transform: scale(1.05);
}
.ds-btn-send:disabled {
  background: #d5d5e0;
  cursor: not-allowed;
}
.ds-send-icon { font-size: 16px; line-height: 1; }

/* ===== 右侧悬停面板：问题定位 ===== */
.ds-question-nav {
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  z-index: 30;
  display: flex;
  align-items: stretch;
  /* 默认：面板内容隐藏，只露出触发条 */
  translate: calc(100% - 36px) 0;
  transition: translate 0.35s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.ds-question-nav:hover {
  translate: 0 0;
}

/* 触发条 */
.ds-question-nav-trigger {
  width: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  background: transparent;
  border-radius: 10px 0 0 10px;
  cursor: default;
  transition: color 0.25s, background 0.25s;
}
.ds-question-nav:hover .ds-question-nav-trigger {
  color: #4f6bf7;
  background: rgba(255, 255, 255, 0.6);
}

/* 面板主体 */
.ds-question-nav-body {
  width: 200px;
  max-height: 52vh;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-right: none;
  border-radius: 12px 0 0 12px;
  box-shadow: -4px 0 32px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  opacity: 0;
  transition: opacity 0.25s ease;
}
.ds-question-nav:hover .ds-question-nav-body {
  opacity: 1;
}

.ds-question-nav-body-header {
  flex-shrink: 0;
  padding: 12px 14px 8px;
  font-size: 12px;
  font-weight: 600;
  color: #999;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.ds-question-nav-body > :not(.ds-question-nav-body-header) {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
}

.ds-question-nav-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
  font-size: 12px;
  color: #ccc;
}

.ds-question-nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  margin-bottom: 2px;
}
.ds-question-nav-item:hover { background: rgba(79, 107, 247, 0.06); }
.ds-question-nav-item.active { background: rgba(79, 107, 247, 0.1); }

.ds-question-nav-index {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(79, 107, 247, 0.08);
  color: #4f6bf7;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ds-question-nav-item.active .ds-question-nav-index {
  background: #4f6bf7;
  color: #fff;
}

.ds-question-nav-text {
  font-size: 12px;
  color: #555;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ds-question-nav-item.active .ds-question-nav-text { color: #1a1a1a; font-weight: 500; }

/* ===== responsive ===== */
@media (max-width: 1024px) {
  .ds-sidebar { width: 240px; }
  .ds-toggle-sidebar { left: 240px; }
  .ds-chat { padding: 16px 20px; }
  .ds-footer { padding: 0 20px 16px; }
}
@media (max-width: 768px) {
  .ds-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 50;
    width: 260px;
    box-shadow: 4px 0 24px rgba(0,0,0,0.15);
  }
  .ds-sidebar.collapsed { width: 0; box-shadow: none; }
  .ds-toggle-sidebar {
    left: 260px;
    z-index: 51;
  }
  .ds-sidebar.collapsed ~ .ds-toggle-sidebar { left: 0; }
  .ds-question-nav { display: none; }
  .ds-chat { padding: 12px 16px; }
  .ds-footer { padding: 0 16px 12px; }
  .ds-msg-inner { max-width: 100%; }
  .ds-input-box { max-width: 100%; }
  .ds-mode-row { flex-wrap: wrap; gap: 6px; }
  .ds-mode-item { padding: 4px 10px; font-size: 12px; }
  .ds-publish-catalog-head {
    align-items: stretch;
    flex-direction: column;
  }
  .ds-publish-catalog-tools {
    justify-content: flex-end;
  }
  .ds-publish-selection-pill {
    max-width: 180px;
  }
  .ds-publish-catalog-fields {
    grid-template-columns: 1fr;
  }
  .ds-theme-options-head {
    display: block;
  }
  .ds-theme-options-subtitle {
    display: block;
    margin-top: 2px;
  }
  .ds-publish-options-head {
    display: block;
  }
  .ds-publish-options-subtitle {
    display: block;
    margin-top: 2px;
  }
  .ds-publish-option-group {
    grid-template-columns: 1fr;
  }
}

/* ===== Blue-ink knowledge assistant visual system ===== */
.ds-page {
  --ai-primary: var(--ymm-primary);
  --ai-primary-strong: var(--ymm-primary-strong);
  --ai-primary-soft: var(--ymm-primary-soft);
  --ai-ink: var(--ymm-ink);
  --ai-text: var(--ymm-text);
  --ai-muted: var(--ymm-muted);
  --ai-line: var(--ymm-line);
  --ai-line-soft: var(--ymm-line-soft);
  color: var(--ai-text);
  background:
    linear-gradient(rgba(40, 89, 166, 0.032) 1px, transparent 1px),
    var(--ymm-canvas);
  background-size: 100% 36px;
  font-family: var(--ymm-font-sans);
}

.ds-sidebar {
  border-right-color: var(--ai-line);
  background: rgba(255, 255, 255, 0.95);
  transition: width 0.32s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.2s ease, padding 0.32s ease;
}

.ds-sidebar-brand {
  width: 100%;
  padding: 18px 16px 14px;
  border: 0;
  border-bottom: 1px solid var(--ai-line-soft);
  color: inherit;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.ds-sidebar-brand:hover {
  background: #f8fafd;
}

.ds-sidebar-brand:focus-visible {
  outline: 3px solid rgba(40, 89, 166, 0.2);
  outline-offset: -3px;
}

.ds-logo {
  width: 44px;
  height: 44px;
  padding: 6px;
  border-radius: 11px 11px 11px 4px;
  color: #fff;
  background: var(--ai-primary);
  --ai-mark-node: var(--ai-primary);
  --ai-mark-node-stroke: #fff;
  --ai-mark-core: #fff;
}

.ds-sidebar-name {
  color: var(--ai-ink);
  font-family: var(--ymm-font-serif);
  font-size: 16px;
}

.ds-sidebar-subtitle,
.ds-conv-time,
.ds-conv-empty {
  color: var(--ai-muted);
}

.ds-new-conv-btn {
  border: 1px solid var(--ai-primary);
  border-radius: 9px;
  color: #fff;
  background: var(--ai-primary);
  font-weight: 700;
  transition: background 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

.ds-new-conv-btn:hover:not(:disabled) {
  border-color: var(--ai-primary-strong);
  background: var(--ai-primary-strong);
  transform: translateY(-1px);
}

.ds-conv-item {
  border-radius: 8px;
}

.ds-conv-item:hover {
  background: #f5f8fc;
}

.ds-conv-item.active {
  background: var(--ai-primary-soft);
  box-shadow: inset 3px 0 0 var(--ai-primary);
}

.ds-conv-icon {
  color: var(--ai-muted);
  background: #edf1f6;
}

.ds-conv-item.active .ds-conv-icon {
  color: var(--ai-primary);
  background: #d8e4f5;
}

.ds-conv-title,
.ds-conv-item.active .ds-conv-title {
  color: var(--ai-ink);
}

.ds-conv-delete {
  color: #9da8b8;
}

.ds-conv-delete:hover,
.ds-clear-all-btn:hover {
  color: var(--ymm-danger);
  background: #fceff1;
}

.ds-sidebar-footer {
  border-top-color: var(--ai-line-soft);
}

.ds-clear-all-btn {
  color: var(--ai-muted);
}

.ds-toggle-sidebar {
  border-color: var(--ai-line);
  color: var(--ai-muted);
  background: #fff;
  transition: left 0.32s cubic-bezier(0.16, 1, 0.3, 1), color 0.18s ease, background 0.18s ease;
}

.ds-toggle-sidebar:hover {
  color: var(--ai-primary);
  background: var(--ai-primary-soft);
}

.ds-chat {
  padding-top: 24px;
}

.ds-scroll-btn {
  border-color: var(--ai-line);
  color: var(--ai-text);
  box-shadow: var(--ymm-shadow-soft);
}

.ds-scroll-btn:hover {
  border-color: var(--ai-primary);
  color: var(--ai-primary);
  box-shadow: 0 8px 20px rgba(40, 89, 166, 0.14);
}

.ds-welcome {
  padding-top: clamp(88px, 15vh, 160px);
}

.ds-welcome-mark {
  width: 92px;
  height: 92px;
  margin-bottom: 18px;
  padding: 13px;
  border: 1px solid #c9d8ec;
  border-radius: 24px 24px 24px 9px;
  color: var(--ai-primary);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 44px rgba(24, 46, 82, 0.1);
  --ai-mark-node: #fff;
  --ai-mark-node-stroke: var(--ai-primary);
  --ai-mark-core: var(--ai-primary);
}

.ds-welcome-kicker {
  margin: 0 0 9px;
  color: var(--ai-primary);
  font-size: 12px;
  font-weight: 700;
}

.ds-welcome-title {
  margin: 0 0 10px;
  color: var(--ai-ink);
  font-family: var(--ymm-font-serif);
  font-size: clamp(27px, 3vw, 40px);
  font-weight: 700;
  line-height: 1.35;
  text-align: center;
}

.ds-welcome-desc {
  max-width: 520px;
  color: var(--ai-muted);
  line-height: 1.75;
  text-align: center;
}

.ds-msg.msg-flash {
  animation-name: msgFlashBlueInk;
}

@keyframes msgFlashBlueInk {
  0% { background: rgba(40, 89, 166, 0.08); }
  100% { background: transparent; }
}

.ds-user-bubble {
  border: 1px solid #cfdbeb;
  border-radius: 14px 14px 4px 14px;
  color: var(--ai-ink);
  background: var(--ai-primary-soft);
}

.ds-thinking {
  border-color: #d6e1ef;
  border-radius: 9px;
  background: #f8fafd;
}

.ds-thinking-bar {
  color: var(--ai-primary);
  background: #edf3fa;
}

.ds-thinking-bar:hover {
  background: #e5edf8;
}

.ds-thinking-pulse,
.ds-loading-dot {
  background: var(--ai-primary);
}

.ds-thinking-pulse {
  box-shadow: 0 0 0 0 rgba(40, 89, 166, 0.34);
  animation-name: thinkingPulseBlueInk;
}

@keyframes thinkingPulseBlueInk {
  0% { box-shadow: 0 0 0 0 rgba(40, 89, 166, 0.34); }
  100% { box-shadow: 0 0 0 8px rgba(40, 89, 166, 0); }
}

.ds-thinking-arrow,
.ds-thinking-state,
.ds-thinking-body {
  color: #647694;
}

.ds-generating {
  border-color: var(--ai-line);
  color: #52627b;
  background: #fbfcfe;
}

.ds-generating-spinner {
  border-color: #d8e2f0;
  border-top-color: var(--ai-primary);
}

.ds-generating-dots span {
  background: #8199ba;
}

.ds-content {
  color: var(--ai-text);
}

.ds-result-content {
  border-color: var(--ai-line);
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(24, 46, 82, 0.055);
}

.ds-cursor {
  color: var(--ai-primary);
}

.ds-refs {
  border-color: #dce5f1;
  border-radius: 9px;
  background: #f7faff;
}

.ds-refs-title,
.ds-recommends-title {
  color: var(--ai-muted);
  text-transform: none;
  letter-spacing: 0;
}

.ds-ref-item + .ds-ref-item {
  border-top-color: var(--ai-line-soft);
}

.ds-ref-title,
.ds-note-ref-citation,
.ds-note-ref-score {
  color: var(--ai-primary);
}

.ds-note-ref-title {
  color: var(--ai-ink);
}

.ds-note-ref-score {
  background: var(--ai-primary-soft);
}

.ds-theme-options,
.ds-publish-options {
  border-color: var(--ai-line);
  border-radius: 9px;
  background: #fbfcfe;
}

.ds-theme-options-title,
.ds-publish-options-title,
.ds-publish-channel {
  color: var(--ai-ink);
}

.ds-theme-options-subtitle,
.ds-publish-options-subtitle {
  color: var(--ai-muted);
}

.ds-theme-option,
.ds-publish-topic {
  border-color: #cfdbeb;
  border-radius: 8px;
  color: var(--ai-primary);
  background: #f5f8fc;
}

.ds-theme-option:hover,
.ds-publish-topic:hover {
  border-color: var(--ai-primary);
  color: var(--ai-primary-strong);
  background: var(--ai-primary-soft);
}

.ds-theme-option:focus-visible,
.ds-publish-topic:focus-visible {
  outline-color: rgba(40, 89, 166, 0.3);
}

.ds-publish-option-group {
  border-color: var(--ai-line-soft);
}

.ds-publish-channel-dot {
  background: var(--ai-primary);
}

.ds-recommend-item {
  border-color: var(--ai-line);
  border-radius: 8px;
  color: var(--ai-text);
  background: rgba(255, 255, 255, 0.72);
}

.ds-recommend-item:hover {
  border-color: var(--ai-primary);
  color: var(--ai-primary);
  background: var(--ai-primary-soft);
}

.ds-error {
  color: var(--ymm-danger);
}

.ds-mode-row {
  gap: 7px;
}

.ds-mode-chip {
  min-height: 34px;
  padding: 7px 12px;
  border-color: var(--ai-line);
  border-radius: 8px;
  color: var(--ai-muted);
  background: rgba(255, 255, 255, 0.86);
}

.ds-mode-chip:hover:not(:disabled) {
  border-color: #b5c5db;
  color: var(--ai-primary);
  background: #fff;
}

.ds-mode-chip.active {
  border-color: #b6cae5;
  color: var(--ai-primary);
  background: var(--ai-primary-soft);
  font-weight: 700;
}

.ds-mode-icon {
  width: 17px;
  height: 17px;
  font-size: inherit;
}

.ds-footer {
  padding-bottom: 22px;
  background: linear-gradient(to top, var(--ymm-canvas) 72%, rgba(244, 247, 251, 0));
}

.ds-input-box {
  border-color: #cfd9e7;
  border-radius: 12px;
  box-shadow: 0 12px 34px rgba(24, 46, 82, 0.09);
}

.ds-input-box:focus-within {
  border-color: var(--ai-primary);
  box-shadow: 0 0 0 3px rgba(40, 89, 166, 0.08), 0 16px 38px rgba(24, 46, 82, 0.1);
}

.ds-publish-catalog-popover,
.ds-image-attach,
.ds-input-actions {
  border-color: var(--ai-line-soft);
}

.ds-publish-catalog-panel,
.ds-publish-catalog-collapsed {
  border-color: var(--ai-line);
  background: #f8fafd;
}

.ds-publish-catalog-title,
.ds-textarea,
.ds-publish-select {
  color: var(--ai-ink);
}

.ds-publish-catalog-dot,
.ds-btn-send {
  background: var(--ai-primary);
}

.ds-publish-selection-pill {
  color: var(--ai-primary);
  background: var(--ai-primary-soft);
}

.ds-publish-tool-btn,
.ds-publish-select {
  border-color: var(--ai-line);
}

.ds-publish-tool-btn:hover:not(:disabled),
.ds-publish-catalog-collapsed strong {
  color: var(--ai-primary);
}

.ds-publish-select:focus {
  border-color: var(--ai-primary);
  box-shadow: 0 0 0 2px rgba(40, 89, 166, 0.08);
}

.ds-image-add {
  border-color: #b8c8dc;
  border-radius: 8px;
  color: var(--ai-primary);
  background: #f8fafd;
}

.ds-image-add:hover {
  border-color: var(--ai-primary);
  background: var(--ai-primary-soft);
}

.ds-image-note {
  border-color: #d7e2f0;
  color: #667b99;
  background: #f3f7fc;
}

.ds-textarea::placeholder,
.ds-hint,
.ds-image-count {
  color: #98a4b6;
}

.ds-btn-stop {
  border-color: var(--ai-line);
  border-radius: 8px;
  color: var(--ai-text);
}

.ds-btn-send {
  border-radius: 9px 9px 9px 3px;
}

.ds-btn-send:hover:not(:disabled) {
  background: var(--ai-primary-strong);
  transform: translateY(-1px);
}

.ds-question-nav-trigger {
  color: var(--ai-muted);
}

.ds-question-nav:hover .ds-question-nav-trigger {
  color: var(--ai-primary);
  background: rgba(255, 255, 255, 0.82);
}

.ds-question-nav-body {
  border-color: rgba(40, 89, 166, 0.14);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: -8px 0 32px rgba(24, 46, 82, 0.07);
}

.ds-question-nav-body-header {
  color: var(--ai-muted);
  letter-spacing: 0;
  text-transform: none;
}

.ds-question-nav-item:hover,
.ds-question-nav-item.active {
  background: var(--ai-primary-soft);
}

.ds-question-nav-index {
  color: var(--ai-primary);
  background: rgba(40, 89, 166, 0.09);
}

.ds-question-nav-item.active .ds-question-nav-index {
  color: #fff;
  background: var(--ai-primary);
}

.ds-question-nav-text,
.ds-question-nav-item.active .ds-question-nav-text {
  color: var(--ai-text);
}

@media (max-width: 768px) {
  .ds-sidebar {
    box-shadow: 8px 0 28px rgba(24, 46, 82, 0.16);
  }

  .ds-welcome {
    padding-top: 70px;
  }

  .ds-welcome-mark {
    width: 76px;
    height: 76px;
    padding: 11px;
    border-radius: 20px 20px 20px 8px;
  }

  .ds-welcome-title {
    max-width: 340px;
    font-size: 27px;
  }

  .ds-welcome-desc {
    max-width: 340px;
    font-size: 13px;
  }

  .ds-mode-chip {
    padding: 6px 9px;
    font-size: 11px;
  }
}
</style>

<!-- markdown-rendered content (non-scoped for v-html compatibility) -->
<style>
.ds-md p { margin: 4px 0; }
.ds-md p:first-child { margin-top: 0; }
.ds-md h1 { font-size: 20px; margin: 16px 0 8px; font-weight: 700; }
.ds-md h2 { font-size: 18px; margin: 14px 0 6px; font-weight: 700; }
.ds-md h3 { font-size: 16px; margin: 12px 0 6px; font-weight: 700; }
.ds-md h4 { font-size: 15px; margin: 10px 0 4px; font-weight: 700; }
.ds-md h5 { font-size: 14px; margin: 8px 0 4px; font-weight: 700; }
.ds-md h6 { font-size: 13px; margin: 6px 0 4px; font-weight: 700; color: #666; }
.ds-md code {
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
}
.ds-md p code,
.ds-md li code {
  background: #f0f0f5;
  color: #e74c3c;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 12px;
}
.ds-md pre {
  background: #1e1e2e;
  color: #cdd6f4;
  padding: 14px 18px;
  border-radius: 10px;
  overflow-x: auto;
  margin: 10px 0;
  font-size: 13px;
  line-height: 1.6;
}
.ds-md pre code {
  background: transparent;
  color: inherit;
  padding: 0;
  border-radius: 0;
  font-size: inherit;
}
.ds-md ul,
.ds-md ol { padding-left: 24px; margin: 4px 0; }
.ds-md ul { list-style-type: disc; }
.ds-md ol { list-style-type: decimal; }
.ds-md li { margin: 2px 0; }
.ds-md li > ul,
.ds-md li > ol { margin: 0; }
.ds-md blockquote {
  border-left: 3px solid #4f6bf7;
  padding: 4px 14px;
  margin: 8px 0;
  color: #777;
  background: #fafafa;
  border-radius: 0 8px 8px 0;
}
.ds-md a { color: #4f6bf7; text-decoration: none; }
.ds-md a:hover { text-decoration: underline; }
.ds-md table {
  border-collapse: collapse;
  margin: 10px 0;
  width: 100%;
  overflow-x: auto;
  display: block;
}
.ds-md thead { display: table-header-group; }
.ds-md tbody { display: table-row-group; }
.ds-md tr { display: table-row; }
.ds-md th,
.ds-md td {
  border: 1px solid #ddd;
  padding: 8px 14px;
  text-align: left;
  font-size: 13px;
  display: table-cell;
}
.ds-md th { background: #f0f2f5; font-weight: 600; white-space: nowrap; }
.ds-md tr:nth-child(even) td { background: #fafbfc; }
.ds-md strong { font-weight: 700; color: #111; }
.ds-md em { font-style: italic; }
.ds-md del { text-decoration: line-through; color: #999; }
.ds-md hr { border: none; border-top: 1px solid #eee; margin: 14px 0; }
.ds-md img { max-width: 100%; border-radius: 6px; margin: 6px 0; }
.ds-md input[type="checkbox"] { margin-right: 6px; accent-color: #4f6bf7; }
.ds-result-content .ds-md h1 {
  font-size: 22px;
  line-height: 1.35;
  margin: 0 0 10px;
  color: #172033;
}
.ds-result-content .ds-md h2 {
  font-size: 15px;
  line-height: 1.45;
  margin: 18px 0 8px;
  color: #26324a;
}
.ds-result-content .ds-md blockquote {
  margin: 10px 0 16px;
  padding: 9px 12px;
  border-left: 3px solid #4f6bf7;
  background: #f6f8ff;
  color: #52617f;
  border-radius: 0 8px 8px 0;
}
.ds-result-content .ds-md p {
  margin: 6px 0;
  color: #2b3344;
}
.ds-result-content .ds-md ul {
  padding-left: 18px;
  margin: 6px 0 2px;
}
.ds-result-content .ds-md li {
  margin: 5px 0;
  color: #3b465d;
}
.ds-result-content .ds-md p code {
  display: inline-block;
  margin: 2px 4px 2px 0;
  padding: 3px 8px;
  border-radius: 999px;
  background: #eef3ff;
  color: #3154b8;
  font-family: inherit;
  font-size: 12px;
}
.ds-result-content .ds-md img {
  width: min(220px, 100%);
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border: 1px solid #e8edf7;
  border-radius: 8px;
  margin: 8px 8px 4px 0;
  background: #f6f8fc;
}

/* Markdown content shares the platform's blue-ink reading style. */
.ds-page .ds-md h1,
.ds-page .ds-md h2,
.ds-page .ds-md h3,
.ds-page .ds-md h4,
.ds-page .ds-md h5,
.ds-page .ds-md strong {
  color: var(--ymm-ink);
}

.ds-page .ds-md p,
.ds-page .ds-md li {
  color: var(--ymm-text);
}

.ds-page .ds-md p code,
.ds-page .ds-md li code {
  color: var(--ymm-primary);
  background: var(--ymm-primary-soft);
}

.ds-page .ds-md blockquote,
.ds-page .ds-result-content .ds-md blockquote {
  border-left-color: var(--ymm-primary);
  color: #5e6e87;
  background: #f5f8fc;
}

.ds-page .ds-md a {
  color: var(--ymm-primary);
}

.ds-page .ds-md th,
.ds-page .ds-md td {
  border-color: var(--ymm-line);
}

.ds-page .ds-md th {
  color: var(--ymm-ink);
  background: #f1f5fa;
}

.ds-page .ds-md tr:nth-child(even) td {
  background: #fafcff;
}

.ds-page .ds-md hr {
  border-top-color: var(--ymm-line);
}

.ds-page .ds-md input[type="checkbox"] {
  accent-color: var(--ymm-primary);
}
</style>

<style scoped>
.ds-history-images { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.ds-history-images img { width: 96px; height: 96px; object-fit: cover; border-radius: 8px; }
.ds-message-status { margin-top: 12px; color: #7a6d57; font-size: 13px; }
</style>
