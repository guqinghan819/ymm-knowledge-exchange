<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { batchQueryNoteItems, likeNote, unlikeNote, collectNote, uncollectNote } from '@/api/note'
import { publishComment, getCommentList, getReplyCommentList, likeComment, unlikeComment } from '@/api/comment'
import { getFileUrl } from '@/utils/utils'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const noteItem = ref(null)
const loading = ref(false)
const errorText = ref('')
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

const normalizeNoteItem = (item) => ({
  ...item,
  like: !!(item.like ?? item.isLike),
  collect: !!(item.collect ?? item.isCollect),
  following: !!(item.following ?? item.isFollowing),
  noteCountResponse: item.noteCountResponse || {
    likeTotal: 0,
    collectTotal: 0,
    commentTotal: 0
  }
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

const noteId = () => noteItem.value?.findNoteDetailResponse?.id || route.params.id

const requireLogin = () => {
  if (userStore.userInfo?.id) return true
  ElMessage.warning('请先登录后再操作')
  return false
}

const syncNoteCount = (field, delta) => {
  if (!noteItem.value) return
  const counts = noteItem.value.noteCountResponse || {}
  counts[field] = Math.max(0, Number(counts[field] || 0) + delta)
  noteItem.value.noteCountResponse = counts
}

const loadNote = async () => {
  const id = route.params.id
  if (!id) {
    errorText.value = '笔记不存在'
    return
  }
  loading.value = true
  errorText.value = ''
  try {
    const res = await batchQueryNoteItems([String(id)])
    const item = res?.data?.[0]
    if (!item) {
      errorText.value = '笔记不存在或暂不可访问'
      return
    }
    noteItem.value = normalizeNoteItem(item)
    await fetchComments()
  } catch (error) {
    errorText.value = error?.message || '笔记加载失败'
  } finally {
    loading.value = false
  }
}

const toggleLike = async () => {
  if (!requireLogin() || !noteItem.value) return
  const id = String(noteId())
  if (likeQueue.has(id)) return
  likeQueue.add(id)
  const current = !!noteItem.value.like
  const next = !current
  const delta = next ? 1 : -1
  noteItem.value.like = next
  syncNoteCount('likeTotal', delta)
  try {
    const res = next ? await likeNote(id) : await unlikeNote(id)
    if (!res || res.success !== true) throw new Error(res?.message || '操作失败')
  } catch (error) {
    noteItem.value.like = current
    syncNoteCount('likeTotal', -delta)
    ElMessage.error(error?.message || '操作失败')
  } finally {
    likeQueue.delete(id)
  }
}

const toggleCollect = async () => {
  if (!requireLogin() || !noteItem.value) return
  const id = String(noteId())
  if (collectQueue.has(id)) return
  collectQueue.add(id)
  const current = !!noteItem.value.collect
  const next = !current
  const delta = next ? 1 : -1
  noteItem.value.collect = next
  syncNoteCount('collectTotal', delta)
  try {
    const res = next ? await collectNote(id) : await uncollectNote(id)
    if (!res || res.success !== true) throw new Error(res?.message || '操作失败')
  } catch (error) {
    noteItem.value.collect = current
    syncNoteCount('collectTotal', -delta)
    ElMessage.error(error?.message || '操作失败')
  } finally {
    collectQueue.delete(id)
  }
}

const fetchComments = async (loadMore = false) => {
  if (!noteId() || commentLoading.value || commentLoadingMore.value) return
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
      noteId: noteId(),
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
  if (!comment || comment.replyLoading) return
  comment.replyLoading = true
  try {
    const res = await getReplyCommentList({
      noteId: noteId(),
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
  comment.replyExpanded = !comment.replyExpanded
  if (comment.replyExpanded && (!comment.replies || comment.replies.length === 0)) {
    await loadReplyComments(comment)
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

const findParentComment = (parentId) => comments.value.find(comment => String(comment.id) === String(parentId))

const findComment = (commentId) => {
  const id = String(commentId)
  for (const comment of comments.value) {
    if (String(comment.id) === id) return comment
    const reply = (comment.replies || []).find(item => String(item.id) === id)
    if (reply) return reply
  }
  return null
}

const toggleCommentLike = async (comment) => {
  if (!requireLogin() || !comment || comment.optimistic) return
  const id = String(comment.id)
  if (commentLikeQueue.has(id)) return
  commentLikeQueue.add(id)
  const current = !!comment.isLike
  const next = !current
  const delta = next ? 1 : -1
  comment.isLike = next
  comment.likeTotal = Math.max(0, Number(comment.likeTotal || 0) + delta)
  try {
    const res = next ? await likeComment(id) : await unlikeComment(id)
    if (!res || res.success !== true) throw new Error(res?.message || '操作失败')
  } catch (error) {
    const target = findComment(id)
    if (target) {
      target.isLike = current
      target.likeTotal = Math.max(0, Number(target.likeTotal || 0) - delta)
    }
    ElMessage.error(error?.message || '操作失败')
  } finally {
    commentLikeQueue.delete(id)
  }
}

const submitComment = async () => {
  if (!requireLogin()) return
  const content = commentInput.value.trim()
  if (!content || commentSubmitting.value) return
  commentSubmitting.value = true
  const reply = replyTarget.value
  const tempId = `temp-${Date.now()}`
  const currentUser = userStore.userInfo || {}
  const optimisticComment = normalizeComment({
    id: tempId,
    noteId: noteId(),
    userId: currentUser.id,
    nickname: currentUser.nickname,
    avatar: currentUser.avatar,
    content,
    level: reply ? 2 : 1,
    parentId: reply ? reply.parentId : noteId(),
    replyCommentId: reply ? reply.replyCommentId : null,
    replyNickname: reply ? reply.replyNickname : null,
    childCommentTotal: 0,
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
  syncNoteCount('commentTotal', 1)
  commentInput.value = ''
  replyTarget.value = null

  try {
    const res = await publishComment({
      noteId: noteId(),
      content,
      replyCommentId: reply ? reply.replyCommentId : null
    })
    const list = reply ? (findParentComment(reply.parentId)?.replies || []) : comments.value
    const target = list.find(comment => comment.id === tempId)
    if (target) {
      target.id = res.data?.commentId || target.id
      target.createTime = res.data?.createTime || target.createTime
      target.optimistic = false
    }
  } catch (error) {
    if (reply) {
      const parent = findParentComment(reply.parentId)
      if (parent) {
        parent.replies = (parent.replies || []).filter(comment => comment.id !== tempId)
        parent.childCommentTotal = Math.max(0, Number(parent.childCommentTotal || 0) - 1)
      }
    } else {
      comments.value = comments.value.filter(comment => comment.id !== tempId)
    }
    syncNoteCount('commentTotal', -1)
    ElMessage.error(error?.message || '评论发布失败')
  } finally {
    commentSubmitting.value = false
  }
}

const formatTime = (time) => {
  if (!time) return ''
  if (time === '刚刚') return time
  return String(time).replace('T', ' ').slice(0, 16)
}

const prevImage = () => {
  if (currentImageIndex.value > 0) currentImageIndex.value--
}

const nextImage = () => {
  const images = noteItem.value?.findNoteDetailResponse?.imgUris || []
  if (currentImageIndex.value < images.length - 1) currentImageIndex.value++
}

onMounted(loadNote)
</script>

<template>
  <main class="note-detail-page">
    <div v-if="loading" class="note-state">加载中...</div>
    <div v-else-if="errorText" class="note-state">{{ errorText }}</div>
    <section v-else-if="noteItem" class="note-shell">
      <div class="media-pane">
        <video
          v-if="noteItem.findNoteDetailResponse.type === 1 && noteItem.findNoteDetailResponse.videoUri"
          :src="noteItem.findNoteDetailResponse.videoUri"
          controls
          autoplay
          class="detail-media">
        </video>
        <div v-else-if="noteItem.findNoteDetailResponse.imgUris?.length" class="image-carousel">
          <button v-if="currentImageIndex > 0" class="carousel-btn prev-btn" @click="prevImage">&#10094;</button>
          <img :src="getFileUrl(noteItem.findNoteDetailResponse.imgUris[currentImageIndex])" class="detail-media" />
          <button
            v-if="currentImageIndex < noteItem.findNoteDetailResponse.imgUris.length - 1"
            class="carousel-btn next-btn"
            @click="nextImage">
            &#10095;
          </button>
        </div>
        <div v-else class="no-media">无媒体内容</div>
      </div>

      <div class="content-pane">
        <div class="author-row">
          <img :src="getFileUrl(noteItem.findNoteDetailResponse.avatar) || 'https://via.placeholder.com/40'" />
          <span>{{ noteItem.findNoteDetailResponse.creatorName }}</span>
        </div>
        <h1>{{ noteItem.findNoteDetailResponse.title }}</h1>
        <p class="note-content">{{ noteItem.findNoteDetailResponse.content }}</p>
        <div v-if="noteItem.findNoteDetailResponse.topicName" class="topic"># {{ noteItem.findNoteDetailResponse.topicName }}</div>
        <div class="time">{{ formatTime(noteItem.findNoteDetailResponse.updateTime) }}</div>

        <div class="actions">
          <button :class="{ active: noteItem.like }" @click="toggleLike">赞 {{ noteItem.noteCountResponse?.likeTotal || 0 }}</button>
          <button :class="{ active: noteItem.collect }" @click="toggleCollect">藏 {{ noteItem.noteCountResponse?.collectTotal || 0 }}</button>
          <span>评 {{ noteItem.noteCountResponse?.commentTotal || 0 }}</span>
        </div>

        <section class="comments">
          <h2>评论</h2>
          <div class="comment-box">
            <img :src="getFileUrl(userStore.userInfo?.avatar) || 'https://via.placeholder.com/32'" />
            <div class="comment-input-wrap">
              <div v-if="replyTarget" class="replying-tip">
                回复 {{ replyTarget.replyNickname }}
                <button type="button" @click="replyTarget = null">取消</button>
              </div>
              <input
                v-model="commentInput"
                :placeholder="replyTarget ? `回复 ${replyTarget.replyNickname}` : '说点什么...'"
                @keyup.enter="submitComment" />
            </div>
            <button :disabled="commentSubmitting || !commentInput.trim()" @click="submitComment">发送</button>
          </div>

          <div v-if="commentLoading" class="comment-state">正在加载评论...</div>
          <div v-else-if="comments.length === 0" class="comment-state">还没有评论</div>
          <div v-else class="comment-list">
            <article v-for="comment in comments" :key="comment.id" class="comment-item">
              <img :src="getFileUrl(comment.avatar) || 'https://via.placeholder.com/32'" />
              <div>
                <div class="comment-meta">
                  <strong>{{ comment.nickname || '用户' }}</strong>
                  <span>{{ formatTime(comment.createTime) }}</span>
                </div>
                <p>{{ comment.content }}</p>
                <div class="comment-actions">
                  <button v-if="!comment.optimistic" :class="{ active: comment.isLike }" @click="toggleCommentLike(comment)">
                    赞 {{ comment.likeTotal || 0 }}
                  </button>
                  <button v-if="!comment.optimistic" @click="setReplyTarget(comment)">回复</button>
                  <button v-if="Number(comment.childCommentTotal || 0) > 0" @click="toggleReplyList(comment)">
                    {{ comment.replyExpanded ? '收起回复' : `查看 ${comment.childCommentTotal} 条回复` }}
                  </button>
                </div>
                <div v-if="comment.replyExpanded" class="reply-list">
                  <article v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                    <img :src="getFileUrl(reply.avatar) || 'https://via.placeholder.com/28'" />
                    <div>
                      <div class="comment-meta">
                        <strong>{{ reply.nickname || '用户' }}</strong>
                        <span v-if="reply.replyNickname">回复 {{ reply.replyNickname }}</span>
                        <span>{{ formatTime(reply.createTime) }}</span>
                      </div>
                      <p>{{ reply.content }}</p>
                      <div class="comment-actions">
                        <button v-if="!reply.optimistic" :class="{ active: reply.isLike }" @click="toggleCommentLike(reply)">
                          赞 {{ reply.likeTotal || 0 }}
                        </button>
                        <button v-if="!reply.optimistic" @click="setReplyTarget(reply, comment)">回复</button>
                      </div>
                    </div>
                  </article>
                  <button v-if="comment.replyHasNext" class="more-btn" :disabled="comment.replyLoading" @click="loadReplyComments(comment, true)">
                    {{ comment.replyLoading ? '加载中...' : '加载更多回复' }}
                  </button>
                </div>
              </div>
            </article>
          </div>
          <button v-if="commentHasNext" class="more-btn" :disabled="commentLoadingMore" @click="fetchComments(true)">
            {{ commentLoadingMore ? '加载中...' : '加载更多评论' }}
          </button>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.note-detail-page {
  min-height: 100vh;
  background: #f6f8fb;
  padding: 32px;
}

.note-state {
  display: grid;
  min-height: 60vh;
  place-items: center;
  color: #667085;
}

.note-shell {
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(360px, 520px);
  max-width: 1180px;
  margin: 0 auto;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e6eaf0;
  border-radius: 8px;
}

.media-pane {
  min-height: 720px;
  background: #101828;
  display: grid;
  place-items: center;
}

.detail-media {
  max-width: 100%;
  max-height: 86vh;
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
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.82);
  cursor: pointer;
}

.prev-btn { left: 16px; }
.next-btn { right: 16px; }
.no-media { color: #d0d5dd; }

.content-pane {
  min-width: 0;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 64px);
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

.author-row img { width: 40px; height: 40px; }

h1 {
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

.actions button {
  padding: 8px 14px;
  border-radius: 18px;
  background: #edf3fa;
}

.actions button.active,
.comment-actions button.active {
  color: #e84a5f;
}

.comments {
  border-top: 1px solid #edf0f5;
  padding-top: 18px;
}

.comments h2 {
  font-size: 16px;
  margin: 0 0 14px;
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

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

@media (max-width: 860px) {
  .note-detail-page {
    padding: 0;
  }

  .note-shell {
    grid-template-columns: 1fr;
    border-radius: 0;
  }

  .media-pane {
    min-height: 360px;
  }

  .content-pane {
    max-height: none;
  }
}

/* Reading-focused detail page */
.note-detail-page {
  min-height: 100vh;
  padding: 28px;
  color: var(--ymm-text);
  background: var(--ymm-canvas);
}
.note-state {
  color: var(--ymm-muted);
  font-size: 15px;
}
.note-shell {
  max-width: 1260px;
  min-height: calc(100vh - 56px);
  margin: 0 auto;
  overflow: hidden;
  border: 1px solid var(--ymm-line);
  border-radius: var(--ymm-radius-lg);
  background: #fff;
  box-shadow: var(--ymm-shadow-soft);
}
.media-pane { background: #101827; }
.carousel-btn {
  border: 1px solid rgba(255,255,255,.35);
  background: rgba(16,24,39,.58);
  backdrop-filter: blur(8px);
}
.content-pane {
  padding: 28px;
  background: #fff;
}
.author-row {
  padding-bottom: 18px;
  border-bottom: 1px solid var(--ymm-line-soft);
  color: var(--ymm-text);
}
.author-row img,
.comment-box img,
.comment-item > img,
.reply-item > img { border-color: var(--ymm-line); }
.content-pane h1 {
  margin: 24px 0 14px;
  color: var(--ymm-ink);
  font-family: var(--ymm-font-serif);
  font-size: 28px;
  line-height: 1.4;
}
.note-content { color: var(--ymm-text); line-height: 1.8; }
.topic { color: var(--ymm-primary); }
.time { color: var(--ymm-muted); }
.actions {
  padding: 16px 0;
  border-top: 1px solid var(--ymm-line-soft);
  border-bottom: 1px solid var(--ymm-line-soft);
}
.actions button {
  padding: 7px 12px;
  border-radius: 8px;
  color: var(--ymm-muted);
  background: #f6f8fb;
}
.actions button.active,
.comment-actions button.active { color: var(--ymm-danger); }
.comments h2 {
  color: var(--ymm-ink);
  font-family: var(--ymm-font-serif);
}
.comment-box {
  padding: 10px;
  border: 1px solid var(--ymm-line);
  border-radius: var(--ymm-radius-md);
  background: #f8fafd;
}
.comment-box input { color: var(--ymm-text); }
.comment-box button {
  padding: 8px 14px;
  border-radius: 8px;
  color: #fff;
  background: var(--ymm-primary);
}
.comment-actions button,
.more-btn,
.replying-tip button { color: var(--ymm-primary); }
.reply-list { border-left-color: #cedaed; }

@media (max-width: 860px) {
  .note-detail-page { padding: 0; }
  .note-shell { min-height: 100vh; border: 0; border-radius: 0; box-shadow: none; }
  .media-pane { min-height: 42vh; }
  .content-pane { padding: 22px 16px 36px; }
  .content-pane h1 { margin-top: 20px; font-size: 23px; }
}
</style>
