<script setup>
import AppIcon from '@/components/AppIcon.vue'
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { batchQueryNoteItems, likeNote, unlikeNote, collectNote, uncollectNote } from '@/api/note'
import { publishComment, getCommentList, getReplyCommentList, likeComment, unlikeComment } from '@/api/comment'
import { getFileUrl } from '@/utils/utils'
import { useDialogMotionOrigin } from '@/utils/dialogMotion'
import { useUserStore } from '@/stores/user'

const { dialogMotionStyle, captureDialogOrigin, resetDialogOrigin } = useDialogMotionOrigin()

const route = useRoute()
const userStore = useUserStore()

const RAG_REFERENCE_KEY_PREFIX = 'rag_refs_'
const RAG_REFERENCE_TTL = 30 * 60 * 1000

const keyword = ref('')
const references = ref([])
const noteCards = ref([])
const loading = ref(false)
const errorText = ref('')

const showNoteDetail = ref(false)
const currentNote = ref(null)
const currentImageIndex = ref(0)
const likeQueue = new Set()
const collectQueue = new Set()

const comments = ref([])
const commentCursor = ref(null)
const commentHasNext = ref(false)
const commentLoading = ref(false)
const commentLoadingMore = ref(false)
const commentInput = ref('')
const commentSubmitting = ref(false)
const replyTarget = ref(null)
const commentLikeQueue = new Set()

const normalizeNoteItem = (item, reference = null) => ({
  ...item,
  like: !!(item.like ?? item.isLike),
  collect: !!(item.collect ?? item.isCollect),
  following: !!(item.following ?? item.isFollowing),
  noteCountResponse: item.noteCountResponse || {
    likeTotal: 0,
    collectTotal: 0,
    commentTotal: 0
  },
  _ragReference: reference
})

const normalizeComment = (comment) => ({
  ...comment,
  replies: comment.replies || [],
  replyCursor: comment.replyCursor || null,
  replyHasNext: comment.replyHasNext || false,
  replyLoading: false,
  replyExpanded: !!comment.replyExpanded,
  likeTotal: Number(comment.likeTotal || 0),
  isLike: !!comment.isLike,
  optimistic: !!comment.optimistic
})

const cleanupReferenceCache = () => {
  const now = Date.now()
  for (let i = localStorage.length - 1; i >= 0; i--) {
    const key = localStorage.key(i)
    if (!key || !key.startsWith(RAG_REFERENCE_KEY_PREFIX)) continue
    try {
      const payload = JSON.parse(localStorage.getItem(key) || '{}')
      if (!payload.createdAt || now - Number(payload.createdAt) > RAG_REFERENCE_TTL) {
        localStorage.removeItem(key)
      }
    } catch {
      localStorage.removeItem(key)
    }
  }
}

const parseReferencePayload = () => {
  const refKey = String(route.params.refKey || '')
  if (!refKey.startsWith(RAG_REFERENCE_KEY_PREFIX)) return null
  const raw = localStorage.getItem(refKey)
  if (!raw) return null
  try {
    const payload = JSON.parse(raw)
    if (!payload.createdAt || Date.now() - Number(payload.createdAt) > RAG_REFERENCE_TTL) {
      localStorage.removeItem(refKey)
      return null
    }
    return payload
  } catch {
    localStorage.removeItem(refKey)
    return null
  }
}

const normalizeReferences = (rawReferences = []) => {
  const seen = new Set()
  return rawReferences
    .map(item => ({
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
    .filter(item => {
      if (!item.noteId) return false
      const id = String(item.noteId)
      if (seen.has(id)) return false
      seen.add(id)
      return true
    })
}

const loadReferences = async () => {
  cleanupReferenceCache()
  const payload = parseReferencePayload()
  if (!payload) {
    errorText.value = '参考来源已失效，请重新搜索'
    return
  }

  keyword.value = payload.keyword || ''
  references.value = normalizeReferences(payload.references || [])
  if (!references.value.length) {
    errorText.value = '参考来源已失效，请重新搜索'
    return
  }

  loading.value = true
  errorText.value = ''
  try {
    const noteIds = references.value.map(reference => reference.noteId)
    const res = await batchQueryNoteItems(noteIds)
    const itemMap = new Map((res?.data || []).map(item => [String(item.findNoteDetailResponse?.id), item]))
    noteCards.value = references.value
      .map(reference => {
        const item = itemMap.get(String(reference.noteId))
        return item ? normalizeNoteItem(item, reference) : null
      })
      .filter(Boolean)

    if (!noteCards.value.length) {
      errorText.value = '参考来源中的笔记暂不可访问'
    }
  } catch (error) {
    errorText.value = error?.message || '参考来源加载失败'
  } finally {
    loading.value = false
  }
}

const requireLogin = () => {
  if (userStore.userInfo?.id) return true
  ElMessage.warning('请先登录后再操作')
  return false
}

const getNoteId = (note) => note?.findNoteDetailResponse?.id
const getCurrentNoteId = () => getNoteId(currentNote.value)

const formatTime = (time) => {
  if (!time) return ''
  if (time === '刚刚') return time
  return String(time).replace('T', ' ').slice(0, 16)
}

const syncLikeState = (noteId, isLike, delta) => {
  const idStr = String(noteId)
  let matchedCard = null
  for (const card of noteCards.value) {
    if (String(getNoteId(card)) === idStr) {
      matchedCard = card
      card.like = isLike
      if (!card.noteCountResponse) card.noteCountResponse = {}
      const prev = Number(card.noteCountResponse.likeTotal || 0)
      card.noteCountResponse.likeTotal = Math.max(0, prev + delta)
      break
    }
  }
  if (currentNote.value && currentNote.value !== matchedCard && String(getCurrentNoteId()) === idStr) {
    currentNote.value.like = isLike
    if (!currentNote.value.noteCountResponse) currentNote.value.noteCountResponse = {}
    const prev = Number(currentNote.value.noteCountResponse.likeTotal || 0)
    currentNote.value.noteCountResponse.likeTotal = Math.max(0, prev + delta)
  }
}

const syncCollectState = (noteId, isCollect, delta) => {
  const idStr = String(noteId)
  let matchedCard = null
  for (const card of noteCards.value) {
    if (String(getNoteId(card)) === idStr) {
      matchedCard = card
      card.collect = isCollect
      if (!card.noteCountResponse) card.noteCountResponse = {}
      const prev = Number(card.noteCountResponse.collectTotal || 0)
      card.noteCountResponse.collectTotal = Math.max(0, prev + delta)
      break
    }
  }
  if (currentNote.value && currentNote.value !== matchedCard && String(getCurrentNoteId()) === idStr) {
    currentNote.value.collect = isCollect
    if (!currentNote.value.noteCountResponse) currentNote.value.noteCountResponse = {}
    const prev = Number(currentNote.value.noteCountResponse.collectTotal || 0)
    currentNote.value.noteCountResponse.collectTotal = Math.max(0, prev + delta)
  }
}

const toggleLikeById = async (noteId) => {
  if (!requireLogin()) return
  const idStr = String(noteId)
  if (likeQueue.has(idStr)) return
  const target = noteCards.value.find(card => String(getNoteId(card)) === idStr) || currentNote.value
  const currentLike = !!target?.like
  const nextLike = !currentLike
  const delta = nextLike ? 1 : -1
  likeQueue.add(idStr)
  syncLikeState(idStr, nextLike, delta)
  try {
    const res = nextLike ? await likeNote(idStr) : await unlikeNote(idStr)
    if (!res || res.success !== true) throw new Error(res?.message || '操作失败')
  } catch (error) {
    syncLikeState(idStr, currentLike, -delta)
    ElMessage.error(error?.message || '操作失败')
  } finally {
    likeQueue.delete(idStr)
  }
}

const toggleCollectById = async (noteId) => {
  if (!requireLogin()) return
  const idStr = String(noteId)
  if (collectQueue.has(idStr)) return
  const target = noteCards.value.find(card => String(getNoteId(card)) === idStr) || currentNote.value
  const currentCollect = !!target?.collect
  const nextCollect = !currentCollect
  const delta = nextCollect ? 1 : -1
  collectQueue.add(idStr)
  syncCollectState(idStr, nextCollect, delta)
  try {
    const res = nextCollect ? await collectNote(idStr) : await uncollectNote(idStr)
    if (!res || res.success !== true) throw new Error(res?.message || '操作失败')
  } catch (error) {
    syncCollectState(idStr, currentCollect, -delta)
    ElMessage.error(error?.message || '操作失败')
  } finally {
    collectQueue.delete(idStr)
  }
}

const resetCommentState = () => {
  comments.value = []
  commentCursor.value = null
  commentHasNext.value = false
  commentLoading.value = false
  commentLoadingMore.value = false
  commentInput.value = ''
  commentSubmitting.value = false
  replyTarget.value = null
}

const syncCommentTotal = (delta) => {
  const noteId = getCurrentNoteId()
  if (!noteId) return
  const idStr = String(noteId)
  for (const card of noteCards.value) {
    if (String(getNoteId(card)) === idStr) {
      if (!card.noteCountResponse) card.noteCountResponse = {}
      const prev = Number(card.noteCountResponse.commentTotal || 0)
      card.noteCountResponse.commentTotal = Math.max(0, prev + delta)
      break
    }
  }
}

const fetchComments = async (loadMore = false) => {
  const noteId = getCurrentNoteId()
  if (!noteId || commentLoading.value || commentLoadingMore.value) return

  if (loadMore) {
    commentLoadingMore.value = true
  } else {
    commentLoading.value = true
    comments.value = []
    commentCursor.value = null
    commentHasNext.value = false
  }

  try {
    const res = await getCommentList({
      noteId,
      cursor: loadMore ? commentCursor.value : null,
      limit: 10
    })
    const list = (res.data || []).map(normalizeComment)
    comments.value = loadMore ? comments.value.concat(list) : list
    commentCursor.value = res.nextCursor || null
    commentHasNext.value = !!res.hasNext
  } catch (error) {
    console.error('获取评论失败:', error)
  } finally {
    commentLoading.value = false
    commentLoadingMore.value = false
  }
}

const loadReplyComments = async (comment, loadMore = false) => {
  const noteId = getCurrentNoteId()
  if (!noteId || !comment || comment.replyLoading) return

  comment.replyLoading = true
  try {
    const res = await getReplyCommentList({
      noteId,
      parentId: comment.id,
      cursor: loadMore ? comment.replyCursor : null,
      limit: 10
    })
    const list = (res.data || []).map(normalizeComment)
    comment.replies = loadMore ? (comment.replies || []).concat(list) : list
    comment.replyCursor = res.nextCursor || null
    comment.replyHasNext = !!res.hasNext
  } catch (error) {
    console.error('获取回复失败:', error)
  } finally {
    comment.replyLoading = false
  }
}

const toggleReplyList = async (comment) => {
  if (!comment) return
  comment.replyExpanded = !comment.replyExpanded
  if (comment.replyExpanded && (!comment.replies || comment.replies.length === 0)) {
    await loadReplyComments(comment)
  }
}

const findParentComment = (parentId) => comments.value.find(comment => String(comment.id) === String(parentId))

const findCommentInDetail = (commentId) => {
  const idStr = String(commentId)
  for (const comment of comments.value) {
    if (String(comment.id) === idStr) return comment
    const reply = (comment.replies || []).find(item => String(item.id) === idStr)
    if (reply) return reply
  }
  return null
}

const syncCommentLikeState = (commentId, isLike, delta) => {
  const target = findCommentInDetail(commentId)
  if (!target) return
  target.isLike = isLike
  target.likeTotal = Math.max(0, Number(target.likeTotal || 0) + delta)
}

const toggleCommentLike = async (comment) => {
  if (!requireLogin() || !comment || comment.optimistic) return
  const idStr = String(comment.id)
  if (commentLikeQueue.has(idStr)) return
  const currentLike = !!comment.isLike
  const nextLike = !currentLike
  const delta = nextLike ? 1 : -1
  commentLikeQueue.add(idStr)
  syncCommentLikeState(idStr, nextLike, delta)

  try {
    const res = nextLike ? await likeComment(idStr) : await unlikeComment(idStr)
    if (!res || res.success !== true) throw new Error(res?.message || '操作失败')
  } catch (error) {
    syncCommentLikeState(idStr, currentLike, -delta)
    ElMessage.error(error?.message || '操作失败')
  } finally {
    commentLikeQueue.delete(idStr)
  }
}

const setReplyTarget = (comment, parentComment = null) => {
  if (!requireLogin()) return
  const parent = parentComment || comment
  replyTarget.value = {
    parentId: parent.id,
    replyCommentId: comment.id,
    replyNickname: comment.nickname || '用户'
  }
}

const clearReplyTarget = () => {
  replyTarget.value = null
}

const removeOptimisticComment = (tempId, parentId = null) => {
  if (parentId) {
    const parent = findParentComment(parentId)
    if (parent && parent.replies) {
      parent.replies = parent.replies.filter(reply => reply.id !== tempId)
      parent.childCommentTotal = Math.max(0, Number(parent.childCommentTotal || 0) - 1)
    }
    return
  }
  comments.value = comments.value.filter(comment => comment.id !== tempId)
}

const replaceOptimisticComment = (tempId, responseData, parentId = null) => {
  const list = parentId ? (findParentComment(parentId)?.replies || []) : comments.value
  const target = list.find(comment => comment.id === tempId)
  if (!target) return
  target.id = responseData.commentId || target.id
  target.createTime = responseData.createTime || target.createTime
  target.optimistic = false
}

const submitComment = async () => {
  if (!requireLogin()) return
  const content = commentInput.value.trim()
  if (!content || commentSubmitting.value) return
  const noteId = getCurrentNoteId()
  if (!noteId) return

  commentSubmitting.value = true
  const reply = replyTarget.value
  const tempId = `temp-${Date.now()}`
  const currentUser = userStore.userInfo || {}
  const optimisticComment = normalizeComment({
    id: tempId,
    noteId,
    userId: currentUser.id,
    nickname: currentUser.nickname,
    avatar: currentUser.avatar,
    content,
    imageUrl: '',
    level: reply ? 2 : 1,
    parentId: reply ? reply.parentId : noteId,
    replyCommentId: reply ? reply.replyCommentId : 0,
    replyUserId: 0,
    replyNickname: reply ? reply.replyNickname : null,
    childCommentTotal: 0,
    likeTotal: 0,
    isLike: false,
    createTime: '刚刚',
    optimistic: true
  })

  if (reply) {
    const parent = findParentComment(reply.parentId)
    if (parent) {
      parent.replyExpanded = true
      parent.replies = parent.replies || []
      parent.replies.push(optimisticComment)
      parent.childCommentTotal = Number(parent.childCommentTotal || 0) + 1
    }
  } else {
    comments.value.push(optimisticComment)
  }
  syncCommentTotal(1)
  commentInput.value = ''
  clearReplyTarget()

  try {
    const res = await publishComment({
      noteId,
      content,
      replyCommentId: reply ? reply.replyCommentId : null
    })
    replaceOptimisticComment(tempId, res.data || {}, reply ? reply.parentId : null)
    ElMessage.success('评论已发布')
  } catch (error) {
    removeOptimisticComment(tempId, reply ? reply.parentId : null)
    syncCommentTotal(-1)
    ElMessage.error(error?.message || '评论发布失败')
  } finally {
    commentSubmitting.value = false
  }
}

const openNoteDetail = (note, event) => {
  captureDialogOrigin(event)
  currentNote.value = note
  currentImageIndex.value = 0
  resetCommentState()
  showNoteDetail.value = true
  document.body.style.overflow = 'hidden'
  fetchComments()
}

const closeNoteDetail = () => {
  showNoteDetail.value = false
  currentNote.value = null
  currentImageIndex.value = 0
  resetCommentState()
  document.body.style.overflow = ''
}

const prevImage = () => {
  if (currentImageIndex.value > 0) currentImageIndex.value--
}

const nextImage = () => {
  const images = currentNote.value?.findNoteDetailResponse?.imgUris || []
  if (currentImageIndex.value < images.length - 1) currentImageIndex.value++
}

onMounted(loadReferences)
onUnmounted(() => {
  document.body.style.overflow = ''
})
</script>

<template>
  <main class="rag-reference-page">
    <header class="reference-header">
      <div>
        <p class="reference-kicker">知识库参考来源</p>
        <h1>{{ keyword ? `“${keyword}”的参考笔记` : '参考笔记' }}</h1>
      </div>
      <span v-if="noteCards.length" class="reference-count">{{ noteCards.length }} 篇可查看</span>
    </header>

    <div v-if="loading" class="reference-state">正在加载参考来源...</div>
    <div v-else-if="errorText" class="reference-state">{{ errorText }}</div>
    <section v-else class="reference-grid">
      <article
        v-for="note in noteCards"
        :key="getNoteId(note)"
        class="reference-note-card"
        @click="openNoteDetail(note, $event)">
        <div class="reference-cover">
          <video
            v-if="note.findNoteDetailResponse.type === 1 && note.findNoteDetailResponse.videoUri"
            :src="note.findNoteDetailResponse.videoUri"
            muted
            playsinline>
          </video>
          <img
            v-else-if="note.findNoteDetailResponse.imgUris?.length"
            :src="getFileUrl(note.findNoteDetailResponse.imgUris[0])"
            :alt="note.findNoteDetailResponse.title"
            loading="lazy" />
          <div v-else class="reference-cover-placeholder">笔记</div>
        </div>
        <div class="reference-body">
          <h2>{{ note.findNoteDetailResponse.title }}</h2>
          <div class="reference-meta">
            <img :src="getFileUrl(note.findNoteDetailResponse.avatar) || 'https://via.placeholder.com/32'" alt="" />
            <span>{{ note.findNoteDetailResponse.creatorName || note._ragReference?.creatorName || '未知作者' }}</span>
            <span v-if="note._ragReference?.topicName || note.findNoteDetailResponse.topicName">
              # {{ note._ragReference?.topicName || note.findNoteDetailResponse.topicName }}
            </span>
            <span v-if="note._ragReference?.citationNo">引用 [{{ note._ragReference.citationNo }}]</span>
          </div>
          <p v-if="note._ragReference?.snippet" class="reference-snippet">{{ note._ragReference.snippet }}</p>
          <p v-else class="reference-content">{{ note.findNoteDetailResponse.content }}</p>
          <div class="reference-stats">
            <button
              type="button"
              class="reference-stat-btn"
              :class="{ active: note.like }"
              :aria-label="note.like ? '取消点赞' : '点赞'"
              :title="note.like ? '取消点赞' : '点赞'"
              :disabled="likeQueue.has(String(getNoteId(note)))"
              @click.stop="toggleLikeById(getNoteId(note))">
              <svg class="interaction-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6 4 4 6.5 4c1.74 0 3.41 1 4.22 2.44C11.09 5 12.76 4 14.5 4 17 4 19 6 19 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path>
              </svg>
              <span>{{ note.noteCountResponse?.likeTotal || 0 }}</span>
            </button>
            <span class="reference-stat-item" title="评论">
              <svg class="interaction-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M21 6H3c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h4v3.5c0 .4.5.7.9.4l5.6-3.9H21c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2z"></path>
              </svg>
              <span>{{ note.noteCountResponse?.commentTotal || 0 }}</span>
            </span>
            <button
              type="button"
              class="reference-stat-btn"
              :class="{ active: note.collect }"
              :aria-label="note.collect ? '取消收藏' : '收藏'"
              :title="note.collect ? '取消收藏' : '收藏'"
              :disabled="collectQueue.has(String(getNoteId(note)))"
              @click.stop="toggleCollectById(getNoteId(note))">
              <svg class="interaction-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"></path>
              </svg>
              <span>{{ note.noteCountResponse?.collectTotal || 0 }}</span>
            </button>
          </div>
        </div>
      </article>
    </section>

    <transition name="dialog-motion" @after-leave="resetDialogOrigin">
      <div v-if="showNoteDetail" class="modal-overlay" :style="dialogMotionStyle" @click.self="closeNoteDetail">
        <div class="note-detail-modal dialog-panel">
          <button class="close-btn" type="button" aria-label="关闭笔记详情" @click="closeNoteDetail"><AppIcon name="close" :size="22" /></button>
          <div class="media-pane">
            <video
              v-if="currentNote.findNoteDetailResponse.type === 1 && currentNote.findNoteDetailResponse.videoUri"
              :src="currentNote.findNoteDetailResponse.videoUri"
              controls
              autoplay
              class="detail-media">
            </video>
            <div v-else-if="currentNote.findNoteDetailResponse.imgUris?.length" class="image-carousel">
              <button v-if="currentImageIndex > 0" class="carousel-btn prev-btn" type="button" @click="prevImage">&#10094;</button>
              <img :src="getFileUrl(currentNote.findNoteDetailResponse.imgUris[currentImageIndex])" class="detail-media" />
              <button
                v-if="currentImageIndex < currentNote.findNoteDetailResponse.imgUris.length - 1"
                class="carousel-btn next-btn"
                type="button"
                @click="nextImage">
                &#10095;
              </button>
            </div>
            <div v-else class="no-media">无媒体内容</div>
          </div>

          <div class="content-pane">
            <div class="author-row">
              <img :src="getFileUrl(currentNote.findNoteDetailResponse.avatar) || 'https://via.placeholder.com/40'" alt="" />
              <span>{{ currentNote.findNoteDetailResponse.creatorName }}</span>
            </div>
            <h1>{{ currentNote.findNoteDetailResponse.title }}</h1>
            <p class="note-content">{{ currentNote.findNoteDetailResponse.content }}</p>
            <div v-if="currentNote.findNoteDetailResponse.topicName" class="topic"># {{ currentNote.findNoteDetailResponse.topicName }}</div>
            <div class="time">{{ formatTime(currentNote.findNoteDetailResponse.updateTime) }}</div>

            <div class="actions">
              <button
                :class="{ active: currentNote.like }"
                :aria-label="currentNote.like ? '取消点赞' : '点赞'"
                :title="currentNote.like ? '取消点赞' : '点赞'"
                type="button"
                @click="toggleLikeById(getCurrentNoteId())">
                <svg class="interaction-icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6 4 4 6.5 4c1.74 0 3.41 1 4.22 2.44C11.09 5 12.76 4 14.5 4 17 4 19 6 19 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path>
                </svg>
                <span>{{ currentNote.noteCountResponse?.likeTotal || 0 }}</span>
              </button>
              <button
                :class="{ active: currentNote.collect }"
                :aria-label="currentNote.collect ? '取消收藏' : '收藏'"
                :title="currentNote.collect ? '取消收藏' : '收藏'"
                type="button"
                @click="toggleCollectById(getCurrentNoteId())">
                <svg class="interaction-icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"></path>
                </svg>
                <span>{{ currentNote.noteCountResponse?.collectTotal || 0 }}</span>
              </button>
              <span class="action-stat" title="评论">
                <svg class="interaction-icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M21 6H3c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h4v3.5c0 .4.5.7.9.4l5.6-3.9H21c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2z"></path>
                </svg>
                <span>{{ currentNote.noteCountResponse?.commentTotal || 0 }}</span>
              </span>
            </div>

            <section class="comments">
              <h2>评论</h2>
              <div class="comment-box">
                <img :src="getFileUrl(userStore.userInfo?.avatar) || 'https://via.placeholder.com/32'" alt="" />
                <div class="comment-input-wrap">
                  <div v-if="replyTarget" class="replying-tip">
                    回复 {{ replyTarget.replyNickname }}
                    <button type="button" @click="clearReplyTarget">取消</button>
                  </div>
                  <input
                    v-model="commentInput"
                    :placeholder="replyTarget ? `回复 ${replyTarget.replyNickname}` : '说点什么...'"
                    @keyup.enter="submitComment" />
                </div>
                <button :disabled="commentSubmitting || !commentInput.trim()" type="button" @click="submitComment">发送</button>
              </div>

              <div v-if="commentLoading" class="comment-state">正在加载评论...</div>
              <div v-else-if="comments.length === 0" class="comment-state">还没有评论</div>
              <div v-else class="comment-list">
                <article v-for="comment in comments" :key="comment.id" class="comment-item">
                  <img :src="getFileUrl(comment.avatar) || 'https://via.placeholder.com/32'" alt="" />
                  <div>
                    <div class="comment-meta">
                      <strong>{{ comment.nickname || '用户' }}</strong>
                      <span>{{ formatTime(comment.createTime) }}</span>
                    </div>
                    <p>{{ comment.content }}</p>
                    <div class="comment-actions">
                      <button v-if="!comment.optimistic" :class="{ active: comment.isLike }" type="button" @click="toggleCommentLike(comment)">
                        赞 {{ comment.likeTotal || 0 }}
                      </button>
                      <button v-if="!comment.optimistic" type="button" @click="setReplyTarget(comment)">回复</button>
                      <button v-if="Number(comment.childCommentTotal || 0) > 0" type="button" @click="toggleReplyList(comment)">
                        {{ comment.replyExpanded ? '收起回复' : `查看 ${comment.childCommentTotal} 条回复` }}
                      </button>
                    </div>
                    <div v-if="comment.replyExpanded" class="reply-list">
                      <article v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                        <img :src="getFileUrl(reply.avatar) || 'https://via.placeholder.com/28'" alt="" />
                        <div>
                          <div class="comment-meta">
                            <strong>{{ reply.nickname || '用户' }}</strong>
                            <span v-if="reply.replyNickname">回复 {{ reply.replyNickname }}</span>
                            <span>{{ formatTime(reply.createTime) }}</span>
                          </div>
                          <p>{{ reply.content }}</p>
                          <div class="comment-actions">
                            <button v-if="!reply.optimistic" :class="{ active: reply.isLike }" type="button" @click="toggleCommentLike(reply)">
                              赞 {{ reply.likeTotal || 0 }}
                            </button>
                            <button v-if="!reply.optimistic" type="button" @click="setReplyTarget(reply, comment)">回复</button>
                          </div>
                        </div>
                      </article>
                      <button v-if="comment.replyHasNext" class="more-btn" :disabled="comment.replyLoading" type="button" @click="loadReplyComments(comment, true)">
                        {{ comment.replyLoading ? '加载中...' : '加载更多回复' }}
                      </button>
                    </div>
                  </div>
                </article>
              </div>
              <button v-if="commentHasNext" class="more-btn" :disabled="commentLoadingMore" type="button" @click="fetchComments(true)">
                {{ commentLoadingMore ? '加载中...' : '加载更多评论' }}
              </button>
            </section>
          </div>
        </div>
      </div>
    </transition>
  </main>
</template>

<style scoped>
.rag-reference-page {
  min-height: 100vh;
  background: #f4f7fb;
  color: #1d2939;
  padding: 28px;
}

.reference-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  max-width: 1180px;
  margin: 0 auto 24px;
}

.reference-kicker {
  margin: 0 0 6px;
  color: #526f91;
  font-size: 14px;
}

.reference-header h1 {
  margin: 0;
  font-size: 28px;
  line-height: 1.25;
}

.reference-count {
  flex: 0 0 auto;
  border: 1px solid #d8e5f4;
  border-radius: 999px;
  background: #fff;
  color: #2b5fa5;
  padding: 7px 12px;
  font-size: 13px;
}

.reference-state {
  display: grid;
  min-height: 48vh;
  place-items: center;
  color: #667085;
}

.reference-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
  max-width: 1180px;
  margin: 0 auto;
}

.reference-note-card {
  overflow: hidden;
  border: 1px solid #dfe7f1;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

.reference-note-card:hover {
  border-color: #a9c3e6;
}

.reference-cover {
  aspect-ratio: 4 / 3;
  background: #101828;
}

.reference-cover img,
.reference-cover video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.reference-cover-placeholder {
  display: grid;
  width: 100%;
  height: 100%;
  place-items: center;
  color: #c9d5e5;
  font-size: 22px;
  font-weight: 700;
}

.reference-body {
  padding: 14px;
}

.reference-body h2 {
  margin: 0;
  color: #1d2939;
  font-size: 17px;
  line-height: 1.35;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.reference-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
  color: #667085;
  font-size: 12px;
}

.reference-meta img {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  object-fit: cover;
  background: #eef2f7;
}

.reference-snippet,
.reference-content {
  margin: 12px 0 0;
  color: #475467;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.reference-stats {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  color: #667085;
  font-size: 13px;
}

.reference-stat-btn,
.reference-stat-item,
.actions button,
.action-stat {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  line-height: 1;
}

.interaction-icon {
  width: 17px;
  height: 17px;
  flex: none;
  color: currentColor;
}

.interaction-icon path {
  fill: none;
  stroke: currentColor;
  stroke-width: 1.6;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.reference-stat-btn,
.actions button,
.comment-box button,
.comment-actions button,
.more-btn,
.replying-tip button {
  border: none;
  background: transparent;
  color: #2b5fa5;
  cursor: pointer;
}

.reference-stat-btn.active,
.actions button.active,
.comment-actions button.active {
  color: #e84a5f;
}

.reference-stat-btn.active .interaction-icon path,
.actions button.active .interaction-icon path {
  fill: currentColor;
  stroke: currentColor;
}

.reference-stat-btn:disabled,
button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  background: rgba(15, 23, 42, 0.62);
  padding: 24px;
}

.note-detail-modal {
  position: relative;
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(360px, 520px);
  width: min(1180px, 96vw);
  max-height: 92vh;
  overflow: hidden;
  border-radius: 8px;
  background: #fff;
}

.close-btn {
  position: absolute;
  top: 14px;
  right: 14px;
  z-index: 4;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.88);
  color: #344054;
  cursor: pointer;
  font-size: 22px;
  line-height: 1;
}

.media-pane {
  min-height: 680px;
  background: #101828;
  display: grid;
  place-items: center;
}

.detail-media {
  max-width: 100%;
  max-height: 92vh;
  object-fit: contain;
}

.image-carousel {
  position: relative;
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
}

.carousel-btn {
  position: absolute;
  z-index: 2;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.82);
  cursor: pointer;
}

.prev-btn {
  left: 16px;
}

.next-btn {
  right: 16px;
}

.no-media {
  color: #d0d5dd;
}

.content-pane {
  min-width: 0;
  display: flex;
  flex-direction: column;
  max-height: 92vh;
  overflow-y: auto;
  padding: 24px;
}

.author-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}

.author-row img,
.comment-box img,
.comment-item > img,
.reply-item > img {
  border-radius: 50%;
  object-fit: cover;
  background: #eef2f7;
}

.author-row img {
  width: 40px;
  height: 40px;
}

.content-pane h1 {
  margin: 24px 0 12px;
  color: #1d2939;
  font-size: 26px;
  line-height: 1.3;
}

.note-content {
  color: #344054;
  line-height: 1.75;
  white-space: pre-wrap;
}

.topic,
.time {
  margin-top: 12px;
  color: #667085;
  font-size: 13px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 22px 0;
}

.actions button {
  padding: 8px 14px;
  border-radius: 18px;
  background: #edf3fa;
}

.action-stat {
  padding: 8px 14px;
  color: #667085;
}

.comments {
  border-top: 1px solid #edf0f5;
  padding-top: 18px;
}

.comments h2 {
  margin: 0 0 14px;
  font-size: 16px;
}

.comment-box {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 20px;
}

.comment-box img,
.comment-item > img {
  width: 32px;
  height: 32px;
}

.comment-input-wrap {
  flex: 1;
  min-width: 0;
}

.comment-box input {
  width: 100%;
  border: none;
  border-radius: 18px;
  background: #f2f4f7;
  padding: 9px 14px;
  outline: none;
}

.replying-tip {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  color: #667085;
  font-size: 12px;
}

.comment-state {
  text-align: center;
  color: #98a2b3;
  padding: 28px 0;
}

.comment-list,
.reply-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item,
.reply-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
}

.reply-list {
  margin-top: 12px;
  padding-left: 10px;
  border-left: 2px solid #eef2f7;
}

.reply-item > img {
  width: 28px;
  height: 28px;
}

.comment-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: #98a2b3;
  font-size: 12px;
}

.comment-meta strong {
  color: #344054;
  font-size: 13px;
}

.comment-item p,
.reply-item p {
  margin: 5px 0;
  color: #344054;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  gap: 12px;
  font-size: 12px;
}

.more-btn {
  align-self: flex-start;
  margin-top: 12px;
  font-size: 12px;
}

@media (max-width: 860px) {
  .rag-reference-page {
    padding: 18px;
  }

  .reference-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .note-detail-modal {
    grid-template-columns: 1fr;
    width: 100vw;
    max-height: 100vh;
    border-radius: 0;
  }

  .modal-overlay {
    padding: 0;
  }

  .media-pane {
    min-height: 320px;
  }

  .content-pane {
    max-height: none;
  }
}
</style>
