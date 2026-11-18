<script setup>
import AppIcon from '@/components/AppIcon.vue'
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import logoSrc from '@/assets/logo.png'
import { ElMessage } from 'element-plus'
import { getVerificationCode, login, logout } from '@/api/auth'
import { updateUser, getUserDetail } from '@/api/user'
import { getChannels } from '@/api/channel'
import {
  getNotesByCursor,
  getCollectedNotesByCursor,
  getLikedNotesByCursor,
  getInteractionSummary,
  likeNote,
  unlikeNote,
  collectNote,
  uncollectNote,
  batchQueryNoteItems
} from '@/api/note'
import { publishComment, getCommentList, getReplyCommentList, likeComment, unlikeComment } from '@/api/comment'
import { followUser, unfollowUser } from '@/api/relation'
import { getNotificationUnreadCount } from '@/api/notification'
import { searchNotes, searchUsers } from '@/api/search'
import { createNoteRagStream } from '@/api/agent'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { getFileUrl } from '@/utils/utils'
import { useDialogMotionOrigin } from '@/utils/dialogMotion'
import { useUserStore } from '@/stores/user'
import NotificationPanel from '@/components/NotificationPanel.vue'
import AiKnowledgeMark from '@/components/AiKnowledgeMark.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
let routeSyncVersion = 0
const showLoginModal = ref(false)
const showMoreMenu = ref(false)
const showProfileSettings = ref(false)
const isUpdatingProfile = ref(false)
const { dialogMotionStyle, captureDialogOrigin, resetDialogOrigin } = useDialogMotionOrigin()

const openLoginModal = (event) => {
  captureDialogOrigin(event)
  showLoginModal.value = true
}
const notificationUnreadCount = ref(0)
const notificationPanelRef = ref(null)
let notificationPollTimer = null

const fetchNotificationUnread = async () => {
  if (!userStore.isLoggedIn) {
    notificationUnreadCount.value = 0
    return
  }
  try {
    const res = await getNotificationUnreadCount()
    notificationUnreadCount.value = Number(res?.data?.count || 0)
  } catch (error) {
    console.error('获取通知未读数失败:', error)
  }
}

const stopNotificationPolling = () => {
  if (notificationPollTimer) {
    clearInterval(notificationPollTimer)
    notificationPollTimer = null
  }
}

const startNotificationPolling = () => {
  stopNotificationPolling()
  if (!userStore.isLoggedIn) return
  fetchNotificationUnread()
  notificationPollTimer = setInterval(() => {
    if (document.hidden || !userStore.isLoggedIn) return
    fetchNotificationUnread()
    if (currentView.value === 'notification') {
      notificationPanelRef.value?.refresh()
    }
  }, 30000)
}

const handleNotificationUnreadChange = (change) => {
  if (change === 'reset') {
    notificationUnreadCount.value = 0
    return
  }
  notificationUnreadCount.value = Math.max(0, notificationUnreadCount.value + Number(change || 0))
}
const profileForm = reactive({
  nickname: '',
  birthday: '',
  sex: 1,
  introduce: '',
  avatar: null,
  backgroundImg: null,
  avatarUrl: '',
  backgroundImgUrl: ''
})

const loginMethod = ref('phone') // 'phone' or 'password'
const loginForm = reactive({
  phone: '',
  code: '',
  account: '',
  password: '',
  agreed: false
})

const countdown = ref(0)
const isCounting = ref(false)
const isLoading = ref(false)
let timer = null

const sendCode = async () => {
  if (isCounting.value) return

  const phoneRegex = /^1(3[0-9]|4[01456879]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[0-35-9])\d{8}$/;
  if (!loginForm.phone) {
    ElMessage.error('请输入手机号')
    return
  }
  if (!phoneRegex.test(loginForm.phone)) {
    ElMessage.error('请输入正确的手机号 (支持移动/联通/电信)')
    return
  }

  try {
    // 假设 verificationType 为 1 (登录/注册)
    await getVerificationCode(loginForm.phone, 1)
    ElMessage.success('验证码已发送')

    isCounting.value = true
    countdown.value = 60
    
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        timer = null
        isCounting.value = false
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  }
}

const toggleLoginMethod = () => {
  loginMethod.value = loginMethod.value === 'phone' ? 'password' : 'phone'
}

const closeModal = () => {
  showLoginModal.value = false
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  isCounting.value = false
  countdown.value = 0
}

const handleLogin = async () => {
  if (!loginForm.agreed) {
    ElMessage.warning('请先同意协议')
    return
  }

  // Phone Validation
  const phoneRegex = /^1(3[0-9]|4[01456879]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[0-35-9])\d{8}$/;
  
  if (loginMethod.value === 'phone') {
    if (!loginForm.phone) {
      ElMessage.error('请输入手机号')
      return
    }
    if (!phoneRegex.test(loginForm.phone)) {
      ElMessage.error('请输入正确的手机号 (支持移动/联通/电信)')
      return
    }
    if (!loginForm.code) {
      ElMessage.error('请输入验证码')
      return
    }
  } else {
    // Password Login Validation
    if (!loginForm.account) {
      ElMessage.error('请输入手机号')
      return
    }
    if (!phoneRegex.test(loginForm.account)) {
      ElMessage.error('请输入正确的手机号')
      return
    }
    if (!loginForm.password) {
      ElMessage.error('请输入密码')
      return
    }
    const passwordRegex = /^[A-Za-z0-9]{8,}$/;
    if (!passwordRegex.test(loginForm.password)) {
      ElMessage.error('密码必须是8位以上的数字或字母')
      return
    }
  }

  isLoading.value = true
  try {
    // 构建登录参数，如果是密码登录，后端可能需要 account 字段，如果是验证码登录，则使用 phone
    // 根据 api/auth.js 中的 login 接口定义，它接收 loginRequest 对象
    // 假设 loginRequest 结构需要根据后端要求适配，这里根据表单数据构建
    const loginRequest = {}
    if (loginMethod.value === 'phone') {
      loginRequest.phone = loginForm.phone
      loginRequest.code = loginForm.code
      // 假设 verificationType: 1 代表验证码登录/注册
      loginRequest.loginType = 1
    } else {
      loginRequest.phone = loginForm.account // 密码登录时，account 字段存的是手机号
      loginRequest.password = loginForm.password
      // 假设 type 为密码登录，具体字段需根据后端接口文档调整，这里仅作示例
      loginRequest.loginType = 2
    }

    const res = await login(loginRequest)
    // 假设返回结构中包含 token，例如 res.data.token 或直接是 token
    // 根据 axios 拦截器 response.data，res 应该是后端返回的数据体
    // 假设 token 在 res.data 中，或者 res 本身就是 token，需根据实际接口响应调整
    // 通常后端返回结构如 { code: 200, data: { token: 'xxx' }, message: 'success' }
    // 如果拦截器直接返回 response.data，那么 res 即为上述对象
    
    const token = res.data // 假设 token 直接在 data 字段，或者根据实际情况修改路径
    
    if (res && res.success === true) {
      setToken(token)
      ElMessage.success('登录成功')
      // 获取用户信息
      await userStore.setUserInfo()
      startNotificationPolling()
      closeModal()
      await syncRouteState()
    }

  } catch (error) {
    console.error('登录失败:', error)
    // 错误提示通常由 axios 拦截器统一处理，这里可不做额外 UI 提示
  } finally {
    isLoading.value = false
  }
}

const handleProfileSettings = (event) => {
  captureDialogOrigin(event)
  showProfileSettings.value = true
  showMoreMenu.value = false
  // 初始化表单数据
  profileForm.nickname = userStore.userInfo.nickname || ''
  profileForm.birthday = userStore.userInfo.birthday || ''
  profileForm.sex = userStore.userInfo.sex !== undefined ? userStore.userInfo.sex : 0
  profileForm.introduce = userStore.userInfo.introduce || ''
  profileForm.avatarUrl = userStore.userInfo.avatar || ''
  profileForm.backgroundImgUrl = userStore.userInfo.backgroundImg || ''
  profileForm.avatar = null
  profileForm.backgroundImg = null
}

const handleAvatarChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    profileForm.avatar = file
    profileForm.avatarUrl = URL.createObjectURL(file)
  }
}

const handleBackgroundChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    profileForm.backgroundImg = file
    profileForm.backgroundImgUrl = URL.createObjectURL(file)
  }
}

const saveProfile = async () => {
  if (isUpdatingProfile.value) return
  
  isUpdatingProfile.value = true
  try {
    const formData = new FormData()
    formData.append('id', userStore.userInfo.id)
    formData.append('nickname', profileForm.nickname)
    if (profileForm.birthday) formData.append('birthday', profileForm.birthday)
    formData.append('sex', profileForm.sex)
    formData.append('introduce', profileForm.introduce)
    
    if (profileForm.avatar) {
      formData.append('avatar', profileForm.avatar)
    }
    
    if (profileForm.backgroundImg) {
      formData.append('backgroundImg', profileForm.backgroundImg)
    }
    
    const res = await updateUser(formData)
    if (res && res.success) {
      ElMessage.success('更新成功')
      await userStore.setUserInfo()
      closeProfileSettings()
      
      // 如果当前在个人主页，刷新数据
      if (currentView.value === 'me') {
        const detailRes = await getUserDetail(userStore.userInfo.ymmId)
        if (detailRes && detailRes.success) {
          userDetailInfo.value = detailRes.data
        }
      }
    }
  } catch (error) {
    console.error('更新个人信息失败:', error)
    // ElMessage.error('更新失败') // Interceptor handles error messages
  } finally {
    isUpdatingProfile.value = false
  }
}

const closeProfileSettings = () => {
  showProfileSettings.value = false
}

const handleLogout = async () => {
  try {
    await logout()
    removeToken()
    userStore.logout()
    stopNotificationPolling()
    notificationUnreadCount.value = 0
    localStorage.clear() // Clear all local storage cache
    ElMessage.success('已退出登录')
    showMoreMenu.value = false
    currentView.value = 'discover' // Reset view to discover
    clearNoteDetail()
    userDetailInfo.value = null // Clear user details
    resetInteractionFeeds()
    interactionSummary.likedTotal = 0
    interactionSummary.collectedTotal = 0
    interactionSummary.loaded = false
    cards.value = []
    nextCursor.value = null
    hasNext.value = true
    fetchError.value = false
    activeCategoryId.value = 0
    if (route.name === 'notifications' || route.name === 'me' || route.name === 'note-detail') {
      await router.replace({ name: 'home' })
    } else {
      await syncRouteState()
    }
  } catch (error) {
    console.error('退出登录失败:', error)
  }
}

const categories = ref([
  { id: 0, name: '全部' }
])
const activeCategoryId = ref(0)

const fetchChannels = async () => {
  try {
    const res = await getChannels()
    if (res && res.success) {
      categories.value = [{ id: 0, name: '全部' }, ...res.data]
    }
  } catch (error) {
    console.error('获取频道失败:', error)
  }
}

// Cursor Pagination State
const cards = ref([])
const nextCursor = ref(null)
const hasNext = ref(true)
const isFetchingNotes = ref(false)
const fetchError = ref(false)
let noteFeedRequestVersion = 0

const fetchNotes = async (isLoadMore = false) => {
  if (isLoadMore && (isFetchingNotes.value || !hasNext.value)) return
  const requestVersion = isLoadMore ? noteFeedRequestVersion : ++noteFeedRequestVersion
  const requestView = currentView.value
  const profileUserId = requestView === 'me' ? userStore.userInfo?.id : null
  if (requestView === 'me' && !profileUserId) {
    cards.value = []
    nextCursor.value = null
    hasNext.value = false
    isFetchingNotes.value = false
    fetchError.value = true
    return
  }
  isFetchingNotes.value = true
  fetchError.value = false

  try {
    const reqData = {
      cursor: isLoadMore ? nextCursor.value : null,
      direction: 'next',
      limit: 20,
      channelId: activeCategoryId.value === 0 ? null : activeCategoryId.value
    }
    
    // 如果当前处于 "我" 界面，且用户已登录，则附加上 userId 参数
    if (requestView === 'me') {
      reqData.userId = profileUserId
    }
    
    const res = await getNotesByCursor(reqData)
    if (requestVersion !== noteFeedRequestVersion || requestView !== currentView.value) return
    if (res && res.success) {
      // 兼容后端返回结构: 
      // 1. data 包含 records/list 和 nextCursor 的对象结构
      // 2. data 是数组，nextCursor 在最外层
      let fetchedCards = Array.isArray(res.data)
        ? res.data
        : (res.data?.records || res.data?.list || res.data?.content || [])
      if (requestView === 'me') {
        fetchedCards = fetchedCards.filter(card =>
          String(card?.findNoteDetailResponse?.creatorId) === String(profileUserId))
      }
        
      if (isLoadMore) {
        cards.value = [...cards.value, ...fetchedCards]
      } else {
        cards.value = fetchedCards
      }
      
      nextCursor.value = res.data?.nextCursor ?? res.nextCursor ?? null
      hasNext.value = res.data?.hasNext ?? res.hasNext ?? false
      const noteTotal = res.data?.total ?? res.total
      if (requestView === 'me' && userDetailInfo.value && noteTotal !== undefined && noteTotal !== null) {
        userDetailInfo.value.noteTotal = Number(noteTotal || 0)
      }
    } else {
      fetchError.value = true
    }
  } catch (error) {
    if (requestVersion !== noteFeedRequestVersion) return
    console.error('获取笔记失败:', error)
    fetchError.value = true
  } finally {
    if (requestVersion === noteFeedRequestVersion) {
      isFetchingNotes.value = false
    }
  }
}

const handleChannelSwitch = (id) => {
  activeCategoryId.value = id
  // Reset pagination state
  cards.value = []
  nextCursor.value = null
  hasNext.value = true
  fetchError.value = false
  fetchNotes()
}

// Intersection Observer for Infinite Scroll
let observer = null

const setupObserver = () => {
  if (observer) {
    observer.disconnect()
  }
  
  observer = new IntersectionObserver((entries) => {
    const entry = entries[0]
    console.log('Sentinel intersecting:', entry.isIntersecting)
    // 当哨兵进入视口、不在加载中、还有下一页、没有发生错误时，触发加载
    if (entry.isIntersecting) {
      const profileCanLoad = currentView.value === 'me'
        && !activeProfileLoading.value
        && activeProfileHasNext.value
        && !activeProfileError.value
      const discoverCanLoad = currentView.value === 'discover'
        && !isFetchingNotes.value
        && hasNext.value
        && !fetchError.value
      if (profileCanLoad || discoverCanLoad) {
        console.log('Triggering load more...')
        if (currentView.value === 'me') {
          fetchActiveProfileFeed(true)
        } else {
          fetchNotes(true)
        }
      } else {
        console.log('Condition not met. isFetching:', isFetchingNotes.value, 'hasNext:', hasNext.value, 'error:', fetchError.value, 'view:', currentView.value)
      }
    }
  }, {
    root: null, // 默认为视口
    // 减小 rootMargin，之前 400px 可能在某些小屏幕下导致初始化时直接触发，如果初始数据不够填满屏幕+400px，
    // 可能导致状态错乱或者哨兵永远不在可视区域内。
    rootMargin: '100px', 
    threshold: 0.1 // 至少 10% 可见
  })
}

const setSentinelRef = (el) => {
  if (el) {
    if (!observer) {
       setupObserver()
    }
    // 延迟一帧观察，确保 DOM 已经完全渲染并处于文档树中
    requestAnimationFrame(() => {
      if (observer && el) {
        observer.observe(el)
        console.log('Observer is now observing the sentinel.')
      }
    })
  }
}

onMounted(() => {
  if (route.query.login === '1' && !userStore.isLoggedIn) {
    showLoginModal.value = true
  }
  fetchChannels()
  setupObserver()
  startNotificationPolling()
  syncRouteState()
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
  }
  stopSearchRag()
  stopNotificationPolling()
  document.body.style.overflow = ''
})

// Note Detail Modal State
const showNoteDetail = ref(false)
const currentNote = ref(null)
const followQueue = reactive(new Set())
const currentImageIndex = ref(0)
const comments = ref([])
const commentCursor = ref(null)
const commentHasNext = ref(false)
const commentLoading = ref(false)
const commentLoadingMore = ref(false)
const commentInput = ref('')
const commentSubmitting = ref(false)
const replyTarget = ref(null)
const commentLikeQueue = new Set()

const getCurrentNoteId = () => currentNote.value?.findNoteDetailResponse?.id

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
  } catch (e) {
    console.error('获取评论失败:', e)
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
  } catch (e) {
    console.error('获取回复失败:', e)
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

const setReplyTarget = (comment, parentComment = null) => {
  if (!userStore.userInfo?.id) {
    showLoginModal.value = true
    return
  }
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

const syncCommentTotal = (delta) => {
  const noteId = getCurrentNoteId()
  if (!noteId) return
  if (!currentNote.value.noteCountResponse) currentNote.value.noteCountResponse = {}
  const currentTotal = Number(currentNote.value.noteCountResponse.commentTotal || 0)
  currentNote.value.noteCountResponse.commentTotal = Math.max(0, currentTotal + delta)

  const idStr = String(noteId)
  for (let i = 0; i < cards.value.length; i++) {
    const card = cards.value[i]
    if (String(card.findNoteDetailResponse.id) === idStr) {
      if (card === currentNote.value) break
      if (!card.noteCountResponse) card.noteCountResponse = {}
      const total = Number(card.noteCountResponse.commentTotal || 0)
      card.noteCountResponse.commentTotal = Math.max(0, total + delta)
      break
    }
  }
}

const findParentComment = (parentId) => {
  return comments.value.find(comment => String(comment.id) === String(parentId))
}

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
  if (!userStore.userInfo?.id) {
    showLoginModal.value = true
    return
  }
  if (!comment || comment.optimistic) return

  const idStr = String(comment.id)
  if (commentLikeQueue.has(idStr)) return
  commentLikeQueue.add(idStr)

  const currentLike = !!comment.isLike
  const nextLike = !currentLike
  const delta = nextLike ? 1 : -1
  syncCommentLikeState(idStr, nextLike, delta)

  try {
    const res = nextLike ? await likeComment(idStr) : await unlikeComment(idStr)
    if (!res || res.success !== true) {
      syncCommentLikeState(idStr, currentLike, -delta)
      ElMessage.error(res?.message || '操作失败')
    }
  } catch (e) {
    syncCommentLikeState(idStr, currentLike, -delta)
    ElMessage.error(e?.message || '操作失败，请稍后重试')
  } finally {
    commentLikeQueue.delete(idStr)
  }
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
  target.id = responseData.commentId
  target.createTime = responseData.createTime || target.createTime
  target.optimistic = false
}

const submitComment = async () => {
  if (!userStore.userInfo?.id) {
    showLoginModal.value = true
    return
  }
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
  } catch (e) {
    removeOptimisticComment(tempId, reply ? reply.parentId : null)
    syncCommentTotal(-1)
    ElMessage.error(e?.message || '评论发布失败')
  } finally {
    commentSubmitting.value = false
  }
}

const formatCommentTime = (time) => {
  if (!time) return ''
  if (time === '刚刚') return time
  return String(time).replace('T', ' ').slice(0, 16)
}

const clearNoteDetail = () => {
  showNoteDetail.value = false
  currentNote.value = null
  currentImageIndex.value = 0
  resetCommentState()
  document.body.style.overflow = ''
}

const showNoteDetailModal = (note) => {
  currentNote.value = note
  currentImageIndex.value = 0
  resetCommentState()
  showNoteDetail.value = true
  document.body.style.overflow = 'hidden' // Prevent background scrolling
  if (getToken() && userStore.isLoggedIn) fetchComments()
}

const openNoteDetail = (note, event) => {
  const noteId = note?.findNoteDetailResponse?.id
  if (!noteId) return
  captureDialogOrigin(event)
  showNoteDetailModal(note)
  router.push({ name: 'note-detail', params: { id: String(noteId) } })
}

const closeNoteDetail = async () => {
  clearNoteDetail()
  const backPath = window.history.state?.back
  if (typeof backPath === 'string' && backPath.startsWith('/') && !backPath.startsWith('//')) {
    router.back()
  } else {
    await router.replace({ name: 'home' })
  }
}

const syncFollowingState = (creatorId, following) => {
  const id = String(creatorId)
  const groups = [cards.value, noteSearchResults.value]
  groups.forEach(group => {
    group.forEach(card => {
      if (String(card?.findNoteDetailResponse?.creatorId) === id) {
        card.following = following
      }
    })
  })
  if (String(currentNote.value?.findNoteDetailResponse?.creatorId) === id) {
    currentNote.value.following = following
  }
}

const toggleFollowCurrentAuthor = async () => {
  if (!userStore.isLoggedIn) {
    showLoginModal.value = true
    return
  }
  const creatorId = currentNote.value?.findNoteDetailResponse?.creatorId
  if (!creatorId || followQueue.has(String(creatorId))) return

  const wasFollowing = !!currentNote.value.following
  followQueue.add(String(creatorId))
  syncFollowingState(creatorId, !wasFollowing)
  try {
    if (wasFollowing) {
      await unfollowUser(creatorId)
      ElMessage.success('已取消关注')
    } else {
      await followUser(creatorId)
      ElMessage.success('关注成功')
    }
  } catch (error) {
    syncFollowingState(creatorId, wasFollowing)
    console.error(wasFollowing ? '取消关注失败:' : '关注失败:', error)
  } finally {
    followQueue.delete(String(creatorId))
  }
}

const prevImage = () => {
  if (currentImageIndex.value > 0) {
    currentImageIndex.value--
  }
}

const nextImage = () => {
  if (currentNote.value && currentNote.value.findNoteDetailResponse.imgUris) {
    if (currentImageIndex.value < currentNote.value.findNoteDetailResponse.imgUris.length - 1) {
      currentImageIndex.value++
    }
  }
}

// Removed Edit Note Modal logic

const currentView = ref('discover') // 'discover', 'me'
const userDetailInfo = ref(null)
const userTab = ref('notes') // 'notes', 'collect', 'like'
const isMeLoading = ref(false)

const createInteractionFeedState = () => ({
  cards: [],
  nextCursor: null,
  hasNext: true,
  loading: false,
  error: false,
  loaded: false
})

const interactionFeeds = reactive({
  collect: createInteractionFeedState(),
  like: createInteractionFeedState()
})
const interactionSummary = reactive({
  likedTotal: 0,
  collectedTotal: 0,
  loaded: false
})

const resetInteractionFeeds = () => {
  Object.assign(interactionFeeds.collect, createInteractionFeedState())
  Object.assign(interactionFeeds.like, createInteractionFeedState())
}

const fetchInteractionSummary = async () => {
  try {
    const res = await getInteractionSummary()
    if (res?.success) {
      interactionSummary.likedTotal = Number(res.data?.likedTotal || 0)
      interactionSummary.collectedTotal = Number(res.data?.collectedTotal || 0)
      interactionSummary.loaded = true
    }
  } catch (error) {
    console.error('获取点赞收藏数量失败:', error)
  }
}

const fetchInteractionNotes = async (type, isLoadMore = false) => {
  const state = interactionFeeds[type]
  if (!state || state.loading || (isLoadMore && !state.hasNext)) return
  state.loading = true
  state.error = false
  try {
    const requestApi = type === 'collect' ? getCollectedNotesByCursor : getLikedNotesByCursor
    const res = await requestApi({
      cursor: isLoadMore ? state.nextCursor : null,
      direction: 'next',
      limit: 20
    })
    if (res?.success) {
      const fetchedCards = Array.isArray(res.data)
        ? res.data
        : (res.data?.records || res.data?.list || [])
      state.cards = isLoadMore ? [...state.cards, ...fetchedCards] : fetchedCards
      state.nextCursor = res.nextCursor ?? res.data?.nextCursor ?? null
      state.hasNext = res.hasNext ?? res.data?.hasNext ?? false
      state.loaded = true
      const total = Number(res.total ?? res.data?.total ?? state.cards.length)
      if (type === 'collect') {
        interactionSummary.collectedTotal = total
      } else {
        interactionSummary.likedTotal = total
      }
    } else {
      state.error = true
    }
  } catch (error) {
    console.error(type === 'collect' ? '获取收藏笔记失败:' : '获取点赞笔记失败:', error)
    state.error = true
  } finally {
    state.loading = false
  }
}

const handleUserTabChange = (tab) => {
  userTab.value = tab
  if (tab !== 'notes' && !interactionFeeds[tab].loaded) {
    fetchInteractionNotes(tab)
  }
}

const activeProfileCards = computed(() => (
  userTab.value === 'notes' ? cards.value : interactionFeeds[userTab.value]?.cards || []
))
const activeProfileLoading = computed(() => (
  userTab.value === 'notes' ? isFetchingNotes.value : !!interactionFeeds[userTab.value]?.loading
))
const activeProfileError = computed(() => (
  userTab.value === 'notes' ? fetchError.value : !!interactionFeeds[userTab.value]?.error
))
const activeProfileHasNext = computed(() => (
  userTab.value === 'notes' ? hasNext.value : !!interactionFeeds[userTab.value]?.hasNext
))
const activeProfileEmptyIcon = computed(() => ({ notes: 'note', collect: 'bookmark', like: 'heart' }[userTab.value]))
const activeProfileEmptyText = computed(() => ({
  notes: '你还没有发布任何内容哦',
  collect: '你还没有收藏任何内容哦',
  like: '你还没有点赞任何内容哦'
}[userTab.value]))

const fetchActiveProfileFeed = (isLoadMore = false) => {
  if (userTab.value === 'notes') {
    fetchNotes(isLoadMore)
  } else {
    fetchInteractionNotes(userTab.value, isLoadMore)
  }
}

const likeQueue = new Set()
const collectQueue = new Set()

const takeInteractionCard = (type, noteId) => {
  const state = interactionFeeds[type]
  const index = state.cards.findIndex(card => String(card.findNoteDetailResponse.id) === String(noteId))
  if (index < 0) return null
  const [card] = state.cards.splice(index, 1)
  const cursorAdjusted = state.nextCursor !== null && state.nextCursor !== undefined
  if (cursorAdjusted) {
    state.nextCursor = Math.max(0, Number(state.nextCursor) - 1)
  }
  return { index, card, cursorAdjusted }
}

const restoreInteractionCard = (type, removed) => {
  if (!removed) return
  const state = interactionFeeds[type]
  state.cards.splice(removed.index, 0, removed.card)
  if (removed.cursorAdjusted) {
    state.nextCursor = Number(state.nextCursor || 0) + 1
  }
}

const adjustInteractionSummary = (type, delta) => {
  if (!interactionSummary.loaded) return
  const field = type === 'like' ? 'likedTotal' : 'collectedTotal'
  interactionSummary[field] = Math.max(0, interactionSummary[field] + delta)
}

const syncLikeState = (noteId, isLike, delta) => {
  const idStr = String(noteId)
  let matchedCard = null
  for (let i = 0; i < cards.value.length; i++) {
    const c = cards.value[i]
    if (String(c.findNoteDetailResponse.id) === idStr) {
      matchedCard = c
      c.like = isLike
      if (!c.noteCountResponse) c.noteCountResponse = {}
      const prev = c.noteCountResponse.likeTotal || 0
      c.noteCountResponse.likeTotal = Math.max(0, prev + delta)
      break
    }
  }
  for (let i = 0; i < noteSearchResults.value.length; i++) {
    const c = noteSearchResults.value[i]
    if (String(c.findNoteDetailResponse.id) === idStr) {
      matchedCard = c
      c.like = isLike
      if (!c.noteCountResponse) c.noteCountResponse = {}
      const prev = c.noteCountResponse.likeTotal || 0
      c.noteCountResponse.likeTotal = Math.max(0, prev + delta)
      break
    }
  }
  Object.values(interactionFeeds).forEach(state => {
    state.cards.forEach(c => {
      if (String(c.findNoteDetailResponse.id) === idStr) {
        c.like = isLike
        if (!c.noteCountResponse) c.noteCountResponse = {}
        c.noteCountResponse.likeTotal = Math.max(0, (c.noteCountResponse.likeTotal || 0) + delta)
        if (c === currentNote.value) matchedCard = c
      }
    })
  })
  if (currentNote.value && currentNote.value !== matchedCard && String(currentNote.value.findNoteDetailResponse.id) === idStr) {
    currentNote.value.like = isLike
    if (!currentNote.value.noteCountResponse) currentNote.value.noteCountResponse = {}
    const prev2 = currentNote.value.noteCountResponse.likeTotal || 0
    currentNote.value.noteCountResponse.likeTotal = Math.max(0, prev2 + delta)
  }
}

const getCurrentStates = (noteId) => {
  const idStr = String(noteId)
  let isLike = false
  let isCollect = false
  for (let i = 0; i < cards.value.length; i++) {
    const c = cards.value[i]
    if (String(c.findNoteDetailResponse.id) === idStr) {
      isLike = !!c.like
      isCollect = !!c.collect
      break
    }
  }
  for (let i = 0; i < noteSearchResults.value.length; i++) {
    const c = noteSearchResults.value[i]
    if (String(c.findNoteDetailResponse.id) === idStr) {
      isLike = !!c.like
      isCollect = !!c.collect
      break
    }
  }
  Object.values(interactionFeeds).forEach(state => {
    const c = state.cards.find(card => String(card.findNoteDetailResponse.id) === idStr)
    if (c) {
      isLike = !!c.like
      isCollect = !!c.collect
    }
  })
  if (currentNote.value && String(currentNote.value.findNoteDetailResponse.id) === idStr) {
    isLike = !!currentNote.value.like
    isCollect = !!currentNote.value.collect
  }
  return { isLike, isCollect }
}

const toggleLikeById = async (noteId) => {
  if (!userStore.userInfo?.id) {
    showLoginModal.value = true
    return
  }
  const idStr = String(noteId)
  if (likeQueue.has(idStr)) return
  likeQueue.add(idStr)
  const { isLike: cur } = getCurrentStates(idStr)
  const nextIsLike = !cur
  const delta = nextIsLike ? 1 : -1
  syncLikeState(idStr, nextIsLike, delta)
  const removed = nextIsLike ? null : takeInteractionCard('like', idStr)
  adjustInteractionSummary('like', delta)
  try {
    if (nextIsLike) {
      const res = await likeNote(idStr)
      if (res && res.success) {
        interactionFeeds.like.loaded = nextIsLike && !removed ? false : interactionFeeds.like.loaded
        ElMessage.success('已点赞')
      } else {
        restoreInteractionCard('like', removed)
        syncLikeState(idStr, cur, -delta)
        adjustInteractionSummary('like', -delta)
        ElMessage.error(res?.message || '点赞失败')
      }
    } else {
      const res = await unlikeNote(idStr)
      if (res && res.success) {
        ElMessage.success('已取消点赞')
      } else {
        restoreInteractionCard('like', removed)
        syncLikeState(idStr, cur, -delta)
        adjustInteractionSummary('like', -delta)
        ElMessage.error(res?.message || '取消点赞失败')
      }
    }
  } catch (e) {
    const revertDelta = -delta
    restoreInteractionCard('like', removed)
    syncLikeState(idStr, cur, revertDelta)
    adjustInteractionSummary('like', revertDelta)
    ElMessage.error(e?.message || '操作失败，请稍后重试')
  } finally {
    likeQueue.delete(idStr)
  }
}

const likeById = toggleLikeById
const unlikeById = toggleLikeById

const syncCollectState = (noteId, isCollect, delta) => {
  const idStr = String(noteId)
  let matchedCard = null
  for (let i = 0; i < cards.value.length; i++) {
    const c = cards.value[i]
    if (String(c.findNoteDetailResponse.id) === idStr) {
      matchedCard = c
      c.collect = isCollect
      if (!c.noteCountResponse) c.noteCountResponse = {}
      const prev = c.noteCountResponse.collectTotal || 0
      c.noteCountResponse.collectTotal = Math.max(0, prev + delta)
      break
    }
  }
  for (let i = 0; i < noteSearchResults.value.length; i++) {
    const c = noteSearchResults.value[i]
    if (String(c.findNoteDetailResponse.id) === idStr) {
      matchedCard = c
      c.collect = isCollect
      if (!c.noteCountResponse) c.noteCountResponse = {}
      const prev = c.noteCountResponse.collectTotal || 0
      c.noteCountResponse.collectTotal = Math.max(0, prev + delta)
      break
    }
  }
  Object.values(interactionFeeds).forEach(state => {
    state.cards.forEach(c => {
      if (String(c.findNoteDetailResponse.id) === idStr) {
        c.collect = isCollect
        if (!c.noteCountResponse) c.noteCountResponse = {}
        c.noteCountResponse.collectTotal = Math.max(0, (c.noteCountResponse.collectTotal || 0) + delta)
        if (c === currentNote.value) matchedCard = c
      }
    })
  })
  if (currentNote.value && currentNote.value !== matchedCard && String(currentNote.value.findNoteDetailResponse.id) === idStr) {
    currentNote.value.collect = isCollect
    if (!currentNote.value.noteCountResponse) currentNote.value.noteCountResponse = {}
    const prev2 = currentNote.value.noteCountResponse.collectTotal || 0
    currentNote.value.noteCountResponse.collectTotal = Math.max(0, prev2 + delta)
  }
}

const toggleCollectById = async (noteId) => {
  if (!userStore.userInfo?.id) {
    showLoginModal.value = true
    return
  }
  const idStr = String(noteId)
  if (collectQueue.has(idStr)) return
  collectQueue.add(idStr)
  const { isCollect: cur } = getCurrentStates(idStr)
  const nextIsCollect = !cur
  const delta = nextIsCollect ? 1 : -1
  syncCollectState(idStr, nextIsCollect, delta)
  const removed = nextIsCollect ? null : takeInteractionCard('collect', idStr)
  adjustInteractionSummary('collect', delta)
  try {
    if (nextIsCollect) {
      await collectNote(idStr)
      interactionFeeds.collect.loaded = false
      ElMessage.success('已收藏')
    } else {
      await uncollectNote(idStr)
      ElMessage.success('已取消收藏')
    }
  } catch (e) {
    const revertDelta = -delta
    restoreInteractionCard('collect', removed)
    syncCollectState(idStr, cur, revertDelta)
    adjustInteractionSummary('collect', revertDelta)
  } finally {
    collectQueue.delete(idStr)
  }
}

const collectById = toggleCollectById
const uncollectById = toggleCollectById

// ================= 搜索相关状态 =================
const searchKeyword = ref('')
const searchTab = ref('note') // note / user
const hasSearched = ref(false)

const noteSearchForm = reactive({
  type: null,
  sort: null,
  publishTimeRange: null
})

const noteSearchResults = ref([])
const userSearchResults = ref([])
const noteSearchLoading = ref(false)
const userSearchLoading = ref(false)
let noteSearchRequestVersion = 0
let userSearchRequestVersion = 0
const searchRagAnswer = ref('')
const searchRagSteps = ref([])
const searchRagReferences = ref([])
const searchRagRecommendations = ref([])
const searchRagLoading = ref(false)
const searchRagError = ref('')
let searchRagAbortController = null
const RAG_REFERENCE_KEY_PREFIX = 'rag_refs_'
const RAG_REFERENCE_TTL = 30 * 60 * 1000

const noteSearchPage = reactive({
  pageNo: 1,
  totalCount: 0,
  pageSize: 10,
  totalPage: 0
})

const userSearchPage = reactive({
  pageNo: 1,
  totalCount: 0,
  pageSize: 10,
  totalPage: 0
})

const firstQueryValue = (value) => Array.isArray(value) ? value[0] : value

const parseQueryEnum = (value, allowedValues) => {
  const raw = firstQueryValue(value)
  if (raw === undefined || raw === null || raw === '') return null
  const parsed = Number(raw)
  return allowedValues.includes(parsed) ? parsed : null
}

const parseQueryPage = (value) => {
  const parsed = Number(firstQueryValue(value))
  return Number.isInteger(parsed) && parsed > 0 ? parsed : 1
}

const buildSearchQuery = (overrides = {}) => {
  const state = {
    q: searchKeyword.value.trim(),
    tab: searchTab.value,
    type: noteSearchForm.type,
    sort: noteSearchForm.sort,
    range: noteSearchForm.publishTimeRange,
    notePage: noteSearchPage.pageNo,
    userPage: userSearchPage.pageNo,
    ...overrides
  }
  const query = { q: state.q }
  if (state.tab === 'user') query.tab = 'user'
  if (state.type !== null && state.type !== undefined) query.type = String(state.type)
  if (state.sort !== null && state.sort !== undefined) query.sort = String(state.sort)
  if (state.range !== null && state.range !== undefined) query.range = String(state.range)
  if (Number(state.notePage) > 1) query.notePage = String(state.notePage)
  if (Number(state.userPage) > 1) query.userPage = String(state.userPage)
  return query
}

const navigateSearch = (overrides = {}) => router.push({
  name: 'search',
  query: buildSearchQuery(overrides)
})

const escapeHtml = (text = '') => {
  return String(text).replace(/[&<>"']/g, (char) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[char]))
}

const highlightKeyword = (text = '', keyword = '') => {
  const escapedText = escapeHtml(text)
  const trimmedKeyword = (keyword || '').trim()
  if (!escapedText || !trimmedKeyword) {
    return escapedText
  }

  const escapedKeyword = trimmedKeyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return escapedText.replace(
    new RegExp(`(${escapedKeyword})`, 'gi'),
    '<strong>$1</strong>'
  )
}

const getNoteTitleHtml = (note) => {
  const title = note.findNoteDetailResponse?.title || note.title || ''
  return note._searchHighlightTitle || note.highlightTitle || highlightKeyword(title, searchKeyword.value)
}

const getUserNicknameHtml = (user) => {
  return user.highlightNickname || highlightKeyword(user.nickname, searchKeyword.value)
}

const normalizeNoteItem = (item, searchHit = null) => ({
  ...item,
  like: !!(item.like ?? item.isLike),
  collect: !!(item.collect ?? item.isCollect),
  following: !!(item.following ?? item.isFollowing),
  noteCountResponse: item.noteCountResponse || {
    likeTotal: 0,
    collectTotal: 0,
    commentTotal: 0
  },
  _searchHighlightTitle: searchHit?.highlightTitle || ''
})

const hydrateSearchNotes = async (hits = []) => {
  const noteIds = hits.map(note => note.noteId).filter(Boolean)
  if (noteIds.length === 0) return []
  const detailRes = await batchQueryNoteItems(noteIds)
  const itemMap = new Map((detailRes?.data || []).map(item => [String(item.findNoteDetailResponse?.id), item]))
  return hits
    .map(hit => {
      const item = itemMap.get(String(hit.noteId))
      return item ? normalizeNoteItem(item, hit) : null
    })
    .filter(Boolean)
}

const parseSearchRagEventData = (dataText) => {
  try {
    return dataText ? JSON.parse(dataText) : {}
  } catch {
    return {}
  }
}

const normalizeSearchRagReferences = (raw) => {
  if (!raw) return []
  const list = Array.isArray(raw) ? raw : (() => {
    try {
      const parsed = JSON.parse(String(raw))
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  })()
  return list
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
    .filter(item => item.noteId || item.title || item.snippet)
}

const validSearchRagReferences = computed(() => {
  const seen = new Set()
  return searchRagReferences.value.filter(reference => {
    if (!reference?.noteId) return false
    const id = String(reference.noteId)
    if (seen.has(id)) return false
    seen.add(id)
    return true
  })
})

const stopSearchRag = () => {
  if (searchRagAbortController) {
    searchRagAbortController.abort()
    searchRagAbortController = null
  }
}

const resetSearchRag = () => {
  searchRagAnswer.value = ''
  searchRagSteps.value = []
  searchRagReferences.value = []
  searchRagRecommendations.value = []
  searchRagError.value = ''
}

const readSearchRagStream = async (stream) => {
  const reader = stream.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let pendingData = ''

  const dispatch = (payload) => {
    const type = payload.type
    if (!type) return
    if (type === 'thinking' || type === 'step') {
      searchRagSteps.value.push(payload.content || '')
    } else if (type === 'text') {
      searchRagAnswer.value += payload.content || ''
    } else if (type === 'reference') {
      searchRagReferences.value = normalizeSearchRagReferences(payload.content)
    } else if (type === 'recommend') {
      searchRagRecommendations.value = Array.isArray(payload.content) ? payload.content : []
    } else if (type === 'error') {
      searchRagError.value = payload.message || payload.content || '知识库暂时没有返回结果'
    }
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
        if (!line) {
          if (pendingData) {
            dispatch(parseSearchRagEventData(pendingData))
            pendingData = ''
          }
          continue
        }
        if (line.startsWith(':')) continue
        if (line.startsWith('data:')) pendingData += line.slice(5).trim()
      }
    }
    if (done) break
  }
  if (pendingData.trim()) dispatch(parseSearchRagEventData(pendingData))
}

const startSearchRag = async (keyword) => {
  stopSearchRag()
  resetSearchRag()
  searchRagLoading.value = true
  searchRagAbortController = new AbortController()
  const controller = searchRagAbortController
  const conversationId = `search-rag-${Date.now()}`
  try {
    const response = await createNoteRagStream(keyword, conversationId, controller.signal)
    if (!response.ok || !response.body) {
      searchRagError.value = '知识库连接失败'
      return
    }
    await readSearchRagStream(response.body)
  } catch (error) {
    if (error?.name !== 'AbortError') {
      searchRagError.value = error?.message || '知识库检索失败'
    }
  } finally {
    if (searchRagAbortController === controller) {
      searchRagLoading.value = false
      searchRagAbortController = null
    }
  }
}

const cleanupRagReferenceCache = () => {
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

const openRagReferencesPage = () => {
  const references = validSearchRagReferences.value
  if (!references.length) {
    ElMessage.warning('暂时没有可查看的参考来源')
    return
  }
  cleanupRagReferenceCache()
  const refKey = `${RAG_REFERENCE_KEY_PREFIX}${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  localStorage.setItem(refKey, JSON.stringify({
    keyword: searchKeyword.value.trim(),
    references,
    createdAt: Date.now()
  }))
  const routeUrl = router.resolve({
    name: 'rag-references',
    params: { refKey }
  })
  window.open(routeUrl.href, '_blank')
}

const fetchNoteSearch = async (page = 1) => {
  const requestVersion = ++noteSearchRequestVersion
  noteSearchLoading.value = true
  noteSearchPage.pageNo = page
  try {
    const res = await searchNotes({
      keyword: searchKeyword.value.trim(),
      pageNo: page,
      type: noteSearchForm.type,
      sort: noteSearchForm.sort,
      publishTimeRange: noteSearchForm.publishTimeRange
    })
    if (requestVersion !== noteSearchRequestVersion) return
    if (res && res.success) {
      noteSearchResults.value = await hydrateSearchNotes(res.data || [])
      if (requestVersion !== noteSearchRequestVersion) return
      noteSearchPage.totalCount = Number(res.totalCount || 0)
      noteSearchPage.pageSize = Number(res.pageSize || 10)
      noteSearchPage.totalPage = Number(res.totalPage || 0)
    }
  } catch (error) {
    console.error('搜索笔记失败:', error)
  } finally {
    if (requestVersion === noteSearchRequestVersion) {
      noteSearchLoading.value = false
    }
  }
}

const fetchUserSearch = async (page = 1) => {
  const requestVersion = ++userSearchRequestVersion
  userSearchLoading.value = true
  userSearchPage.pageNo = page
  try {
    const res = await searchUsers({
      keyword: searchKeyword.value.trim(),
      pageNo: page
    })
    if (requestVersion !== userSearchRequestVersion) return
    if (res && res.success) {
      userSearchResults.value = res.data || []
      userSearchPage.totalCount = Number(res.totalCount || 0)
      userSearchPage.pageSize = Number(res.pageSize || 10)
      userSearchPage.totalPage = Number(res.totalPage || 0)
    }
  } catch (error) {
    console.error('搜索用户失败:', error)
  } finally {
    if (requestVersion === userSearchRequestVersion) {
      userSearchLoading.value = false
    }
  }
}

const handleSearch = async () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    ElMessage.warning('请输入搜索关键字')
    return
  }
  await navigateSearch({
    q: keyword,
    tab: 'note',
    notePage: 1,
    userPage: 1
  })
}

const handleNoteFilterChange = () => {
  if (hasSearched.value) {
    navigateSearch({ notePage: 1 })
  }
}

const handleSearchTabChange = (tab) => {
  if (tab !== searchTab.value) {
    navigateSearch({ tab })
  }
}

const handleNotePageChange = (page) => {
  navigateSearch({ notePage: page })
}

const handleUserPageChange = (page) => {
  navigateSearch({ userPage: page })
}

const resetNoteFeed = () => {
  cards.value = []
  nextCursor.value = null
  hasNext.value = true
  fetchError.value = false
}

const loadDiscoverView = () => {
  currentView.value = 'discover'
  resetNoteFeed()
  fetchNotes()
}

const loadMeView = async (syncVersion) => {
  isMeLoading.value = true
  currentView.value = 'me'
  try {
    const res = await getUserDetail(userStore.userInfo.ymmId)
    if (syncVersion !== routeSyncVersion || route.name !== 'me') return
    if (res && res.success) {
      userDetailInfo.value = res.data
      resetNoteFeed()
      userTab.value = 'notes'
      resetInteractionFeeds()
      interactionSummary.loaded = false
      fetchInteractionSummary()
      fetchNotes()
    }
  } catch (error) {
    console.error('获取个人详情失败:', error)
  } finally {
    if (syncVersion === routeSyncVersion) {
      isMeLoading.value = false
    }
  }
}

const switchView = async (view) => {
  const routeNames = {
    discover: 'home',
    notification: 'notifications',
    me: 'me'
  }
  const routeName = routeNames[view]
  if (!routeName || route.name === routeName) return
  await router.push({ name: routeName })
}

const goToPublish = () => {
  if (!getToken() || !userStore.isLoggedIn) {
    openLoginModal()
    return
  }
  const routeUrl = router.resolve({
    path: '/publish'
  })
  window.open(routeUrl.href, '_blank')
}

const goToCooperate = () => {
  const cooperateUrl = new URL('../static/cooperate.html', import.meta.url).href
  window.open(cooperateUrl, '_blank')
}

const goToAiChat = () => {
  const routeUrl = router.resolve({ path: '/ai-chat' })
  window.open(routeUrl.href, '_blank')
}

let lastNoteSearchSignature = ''
let lastUserSearchSignature = ''
let lastRagKeyword = ''

const syncSearchRoute = async () => {
  const keyword = String(firstQueryValue(route.query.q) || '').trim()
  const tab = firstQueryValue(route.query.tab) === 'user' ? 'user' : 'note'
  const type = parseQueryEnum(route.query.type, [0, 1])
  const sort = parseQueryEnum(route.query.sort, [0, 1, 2, 3])
  const publishTimeRange = parseQueryEnum(route.query.range, [0, 1, 2])
  const notePage = parseQueryPage(route.query.notePage)
  const userPage = parseQueryPage(route.query.userPage)

  currentView.value = 'search'
  searchKeyword.value = keyword
  searchTab.value = tab
  noteSearchForm.type = type
  noteSearchForm.sort = sort
  noteSearchForm.publishTimeRange = publishTimeRange
  noteSearchPage.pageNo = notePage
  userSearchPage.pageNo = userPage

  if (!keyword) {
    hasSearched.value = false
    noteSearchResults.value = []
    userSearchResults.value = []
    stopSearchRag()
    resetSearchRag()
    lastNoteSearchSignature = ''
    lastUserSearchSignature = ''
    lastRagKeyword = ''
    return
  }

  hasSearched.value = true
  const noteSignature = JSON.stringify({ keyword, type, sort, publishTimeRange, notePage })
  const userSignature = JSON.stringify({ keyword, userPage })
  const requests = []

  if (noteSignature !== lastNoteSearchSignature) {
    lastNoteSearchSignature = noteSignature
    requests.push(fetchNoteSearch(notePage))
  }
  if (userSignature !== lastUserSearchSignature) {
    lastUserSearchSignature = userSignature
    requests.push(fetchUserSearch(userPage))
  }
  if (keyword !== lastRagKeyword) {
    lastRagKeyword = keyword
    startSearchRag(keyword)
  }

  await Promise.all(requests)
}

const loadRouteNote = async (syncVersion) => {
  const noteId = String(route.params.id || '')
  if (!noteId) {
    ElMessage.error('笔记不存在')
    await router.replace({ name: 'home' })
    return
  }

  if (String(getCurrentNoteId() || '') === noteId) {
    if (!showNoteDetail.value && currentNote.value) {
      showNoteDetailModal(currentNote.value)
    }
    return
  }

  try {
    const res = await batchQueryNoteItems([noteId])
    if (syncVersion !== routeSyncVersion || route.name !== 'note-detail') return
    const note = Array.isArray(res?.data) ? res.data[0] : null
    if (!note) {
      ElMessage.error('笔记不存在或暂不可访问')
      await router.replace({ name: 'home' })
      return
    }
    showNoteDetailModal(normalizeNoteItem(note))
  } catch (error) {
    if (syncVersion !== routeSyncVersion || route.name !== 'note-detail') return
    console.error('加载笔记详情失败:', error)
    clearNoteDetail()
    await router.replace({ name: 'home' })
  }
}

const syncRouteState = async () => {
  const syncVersion = ++routeSyncVersion
  const routeName = route.name

  if (routeName !== 'note-detail' && showNoteDetail.value) {
    clearNoteDetail()
  }
  if (routeName !== 'search') {
    stopSearchRag()
    lastRagKeyword = ''
  }

  if (routeName === 'home') {
    if (currentView.value !== 'discover' || cards.value.length === 0) {
      loadDiscoverView()
    }
    return
  }

  if (routeName === 'notifications') {
    if (!userStore.isLoggedIn) {
      showLoginModal.value = true
      if (currentView.value !== 'discover' || cards.value.length === 0) {
        loadDiscoverView()
      }
      return
    }
    currentView.value = 'notification'
    fetchNotificationUnread()
    return
  }

  if (routeName === 'me') {
    if (!userStore.isLoggedIn) {
      showLoginModal.value = true
      if (currentView.value !== 'discover' || cards.value.length === 0) {
        loadDiscoverView()
      }
      return
    }
    // 从笔记详情返回时保留当前选项和已加载的列表。
    if (currentView.value === 'me' && userDetailInfo.value) return
    await loadMeView(syncVersion)
    return
  }

  if (routeName === 'search') {
    await syncSearchRoute()
    return
  }

  if (routeName === 'note-detail') {
    if (currentView.value === 'discover' && cards.value.length === 0) {
      loadDiscoverView()
    }
    await loadRouteNote(syncVersion)
  }
}

watch(() => route.fullPath, () => {
  syncRouteState()
})

watch(() => userStore.isLoggedIn, (loggedIn) => {
  if (loggedIn && showNoteDetail.value) fetchComments()
})
</script>

<template>
  <div class="home-container">
    <!-- Header -->
    <header class="header">
      <div class="header-left">
        <!-- Logo -->
        <div class="logo">
           <!-- Ideally this would be the image file provided by the user -->
           <img :src="logoSrc" alt="雨绵绵" class="logo-img" @error="$event.target.style.display='none'; $event.target.nextElementSibling.style.display='flex'" />
           <!-- Fallback CSS logo matching the second image description -->
           <div class="logo-fallback" style="display:none;">
             <div class="logo-text-main">雨绵绵</div>
             <div class="logo-text-sub">致力于知识分享</div>
           </div>
        </div>
      </div>
      <div class="header-center">
        <div class="search-bar">
          <input
            v-model="searchKeyword"
            type="text"
            :placeholder="userStore.userInfo.nickname ? '搜索你感兴趣的内容' : '登录探索更多内容'"
            @keyup.enter="handleSearch"
          />
          <button class="search-icon" type="button" aria-label="搜索" @click="handleSearch">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="11" cy="11" r="6.5"></circle>
              <path d="m16 16 4 4"></path>
            </svg>
          </button>
        </div>
      </div>
      <div class="header-right">
        <a href="#" class="nav-link" @click.prevent="goToPublish">创作中心</a>
        <a href="#" class="nav-link" @click.prevent="goToCooperate">业务合作</a>
      </div>
    </header>

    <div class="main-layout">
      <!-- Sidebar -->
      <aside class="sidebar">
        <nav class="side-nav">
          <a href="#" class="side-link" :class="{ active: currentView === 'discover' }" @click.prevent="switchView('discover')">
            <span class="icon"><svg class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M4 10.5 12 4l8 6.5v8a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 18.5z"></path><path d="M9.5 20v-6h5v6"></path></svg></span>
            <span class="text">发现</span>
          </a>
  
          <a href="#" class="side-link" @click.prevent="goToPublish">
            <span class="icon"><svg class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14"></path></svg></span>
            <span class="text">发布</span>
          </a>
          <a href="#" class="side-link" :class="{ active: currentView === 'notification' }" @click.prevent="switchView('notification')">
            <span class="icon"><svg class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 8.5h18C21 16 18 16 18 9Z"></path><path d="M10 21h4"></path></svg></span>
            <span class="text">通知</span>
            <span v-if="notificationUnreadCount > 0" class="notification-badge">
              {{ notificationUnreadCount > 99 ? '99+' : notificationUnreadCount }}
            </span>
          </a>
          <a v-if="userStore.userInfo.nickname" href="#" class="side-link" :class="{ active: currentView === 'me' }" @click.prevent="switchView('me')">
             <span class="icon">
                <img v-if="userStore.userInfo.avatar" :src="userStore.userInfo.avatar" class="user-avatar-small" />
                <svg v-else class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="3.5"></circle><path d="M5.5 20a6.5 6.5 0 0 1 13 0"></path></svg>
             </span>
             <span class="text">我</span>
          </a>
        </nav>
        
        <div v-if="!userStore.userInfo.nickname" class="login-section">
          <button class="login-btn" @click="openLoginModal($event)">登录</button>
          <div class="login-text">
            <p>马上登录即可</p>
            <ul>
              <li>发现更适合你的优质内容</li>
              <li>搜索最新经验与真实评价</li>
              <li>随时找回收藏、点赞的笔记</li>
              <li>参与讨论，与创作者交流</li>
            </ul>
          </div>
        </div>

        <div v-else class="more-section">
             <div class="more-menu-container">
               <Transition name="popover-motion">
                 <div v-if="showMoreMenu" class="more-popover">
                   <div class="more-item" @click="handleProfileSettings($event)">个性设置</div>
                   <div class="more-item" @click="handleLogout">退出登录</div>
                 </div>
               </Transition>
               <a href="#" class="side-link" @click.prevent="showMoreMenu = !showMoreMenu">
                  <span class="icon"><svg class="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><circle cx="5" cy="12" r="1"></circle><circle cx="12" cy="12" r="1"></circle><circle cx="19" cy="12" r="1"></circle></svg></span>
                  <span class="text">更多</span>
               </a>
             </div>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="content">
        <template v-if="currentView === 'discover'">
          <!-- Categories -->
          <div class="categories">
            <button v-for="cat in categories" :key="cat.id" :class="['cat-btn', { active: activeCategoryId === cat.id }]" @click="handleChannelSwitch(cat.id)">
              {{ cat.name }}
            </button>
          </div>

          <div v-if="cards.length === 0 && isFetchingNotes" class="skeleton-grid">
            <div v-for="i in 8" :key="i" class="skeleton-card">
              <div class="skeleton-image"></div>
              <div class="skeleton-content">
                <div class="skeleton-title"></div>
                <div class="skeleton-title short"></div>
                <div class="skeleton-footer">
                  <div class="skeleton-author"></div>
                  <div class="skeleton-likes"></div>
                </div>
              </div>
            </div>
          </div>

          <!-- Masonry Grid -->
          <div v-else class="masonry-grid">
            <div v-for="card in cards" :key="card.findNoteDetailResponse.id" class="card" @click="openNoteDetail(card, $event)">
              <div class="card-image">
                <!-- Video Thumbnail -->
                <template v-if="card.findNoteDetailResponse.type === 1">
                  <video 
                    v-if="card.findNoteDetailResponse.videoUri"
                    :src="card.findNoteDetailResponse.videoUri" 
                    class="card-video-thumbnail" 
                    preload="metadata"
                    muted>
                  </video>
                  <div class="video-icon-overlay"><AppIcon name="play" :size="18" /></div>
                </template>
                <!-- Image Thumbnail -->
                <template v-else>
                  <img :src="getFileUrl(card.findNoteDetailResponse.imgUris?.[0]) || 'https://via.placeholder.com/300x400?text=No+Image'" :alt="card.findNoteDetailResponse.title" loading="lazy" />
                </template>
              </div>
              <div class="card-content">
                <h3 class="card-title">{{ card.findNoteDetailResponse.title }}</h3>
                <div class="card-footer">
                  <div class="author">
                    <img class="avatar" :src="getFileUrl(card.findNoteDetailResponse.avatar) || 'https://via.placeholder.com/32'" loading="lazy" />
                    <span class="name">{{ card.findNoteDetailResponse.creatorName }}</span>
                  </div>
                  <div class="card-stats">
                    <div class="stat" :class="{ active: card.like }" @click.stop="toggleLikeById(card.findNoteDetailResponse.id)" :title="card.like ? '取消点赞' : '点赞'">
                      <svg class="icon-svg" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6 4 4 6.5 4c1.74 0 3.41 1 4.22 2.44C11.09 5 12.76 4 14.5 4 17 4 19 6 19 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path>
                      </svg>
                      <span class="count">{{ card.noteCountResponse?.likeTotal || 0 }}</span>
                    </div>
                    <div class="stat" :class="{ active: card.collect }" @click.stop="toggleCollectById(card.findNoteDetailResponse.id)" :title="card.collect ? '取消收藏' : '收藏'">
                      <svg class="icon-svg" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M12 17.27L18.18 21 16.54 13.97 22 9.24 14.81 8.62 12 2 9.19 8.63 2 9.24 7.46 13.97 5.82 21z"></path>
                      </svg>
                      <span class="count">{{ card.noteCountResponse?.collectTotal || 0 }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- Infinite Scroll Sentinel & Status -->
          <div :ref="setSentinelRef" class="sentinel" style="height: 10px; width: 100%;"></div>
          
          <div v-if="isFetchingNotes && cards.length > 0" class="loading-more">
            <div class="spinner-small"></div>
            <span>加载中...</span>
          </div>
          <div v-else-if="fetchError" class="error-state">
            <span>加载失败，请检查网络</span>
            <button @click="fetchNotes(true)" class="retry-btn">重试</button>
          </div>
          <div v-else-if="!hasNext && cards.length > 0" class="no-more">没有更多内容了</div>
          
          <div v-if="!isFetchingNotes && cards.length === 0 && !fetchError" class="empty-state">
            暂无笔记
          </div>
        </template>

        <NotificationPanel
          v-else-if="currentView === 'notification'"
          ref="notificationPanelRef"
          @unread-change="handleNotificationUnreadChange" />

        <template v-else-if="currentView === 'me'">
          <div v-if="isMeLoading" class="loading-container">
            <div class="spinner"></div>
            <p>加载中...</p>
          </div>
          <div v-else-if="userDetailInfo" class="profile-page">
            <!-- Background Header -->
            <div class="profile-header" :style="{ backgroundImage: `url(${getFileUrl(userDetailInfo.backgroundImg) || ''})` }">
              <div class="profile-header-overlay">
                <div class="profile-header-content">
                  <div class="profile-main-avatar">
                    <img v-if="userDetailInfo.avatar" :src="getFileUrl(userDetailInfo.avatar)" alt="Avatar" />
                    <span v-else class="default-avatar-large"><AppIcon name="user" /></span>
                  </div>
                  <div class="profile-info-text">
                    <h1 class="profile-name">{{ userDetailInfo.nickname }}</h1>
                    <p class="profile-id">雨绵绵号: {{ userDetailInfo.ymmId }}</p>
                    <p class="profile-intro">{{ userDetailInfo.introduce || '还没有简介' }}</p>
                    
                    <div class="profile-gender">
                      <span v-if="userDetailInfo.sex === 0" class="gender-icon male" aria-label="男"><AppIcon name="male" /></span>
                      <span v-else-if="userDetailInfo.sex === 1" class="gender-icon female" aria-label="女"><AppIcon name="female" /></span>
                    </div>

                    <div class="profile-stats">
                      <div class="stat-item">
                        <span class="stat-num">{{ userDetailInfo.followingTotal ?? 0 }}</span>
                        <span class="stat-label">关注</span>
                      </div>
                      <div class="stat-item">
                        <span class="stat-num">{{ userDetailInfo.fansTotal ?? 0 }}</span>
                        <span class="stat-label">粉丝</span>
                      </div>
                      <div class="stat-item">
                        <span class="stat-num">{{ Number(userDetailInfo.likeTotal ?? 0) + Number(userDetailInfo.collectTotal ?? 0) }}</span>
                        <span class="stat-label">获赞与收藏</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Profile Tabs -->
            <div class="profile-tabs">
              <div class="tab-item" :class="{ active: userTab === 'notes' }" @click="handleUserTabChange('notes')">
                笔记 <span class="tab-count">{{ userDetailInfo.noteTotal ?? 0 }}</span>
              </div>
              <div class="tab-item" :class="{ active: userTab === 'collect' }" @click="handleUserTabChange('collect')">
                收藏 <span class="tab-count">{{ interactionSummary.collectedTotal }}</span>
              </div>
              <div class="tab-item" :class="{ active: userTab === 'like' }" @click="handleUserTabChange('like')">
                点赞 <span class="tab-count">{{ interactionSummary.likedTotal }}</span>
              </div>
            </div>

            <!-- Tab Content -->
            <div class="profile-tab-content">
              <div class="user-content">
                <div v-if="activeProfileCards.length === 0 && activeProfileLoading" class="skeleton-grid">
                  <div v-for="i in 8" :key="i" class="skeleton-card">
                    <div class="skeleton-image"></div>
                    <div class="skeleton-content">
                      <div class="skeleton-title"></div>
                      <div class="skeleton-title short"></div>
                      <div class="skeleton-footer">
                        <div class="skeleton-author"></div>
                        <div class="skeleton-likes"></div>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-else-if="activeProfileCards.length > 0" class="masonry-grid">
                  <div v-for="card in activeProfileCards" :key="card.findNoteDetailResponse.id" class="card" @click="openNoteDetail(card, $event)">
                    <div class="card-image">
                      <template v-if="card.findNoteDetailResponse.type === 1">
                        <video 
                          v-if="card.findNoteDetailResponse.videoUri"
                          :src="card.findNoteDetailResponse.videoUri" 
                          class="card-video-thumbnail" 
                          preload="metadata"
                          muted>
                        </video>
                        <div class="video-icon-overlay"><AppIcon name="play" :size="18" /></div>
                      </template>
                      <template v-else>
                        <img :src="getFileUrl(card.findNoteDetailResponse.imgUris?.[0]) || 'https://via.placeholder.com/300x400?text=No+Image'" :alt="card.findNoteDetailResponse.title" loading="lazy" />
                      </template>
                    </div>
                    <div class="card-content">
                      <h3 class="card-title">{{ card.findNoteDetailResponse.title }}</h3>
                      <div class="card-footer">
                        <div class="author">
                          <img class="avatar" :src="getFileUrl(card.findNoteDetailResponse.avatar) || 'https://via.placeholder.com/32'" loading="lazy" />
                          <span class="name">{{ card.findNoteDetailResponse.creatorName }}</span>
                        </div>
                        <div class="card-stats">
                          <div class="stat" :class="{ active: card.like }" @click.stop="toggleLikeById(card.findNoteDetailResponse.id)" :title="card.like ? '取消点赞' : '点赞'">
                            <svg class="icon-svg" viewBox="0 0 24 24" aria-hidden="true">
                              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6 4 4 6.5 4c1.74 0 3.41 1 4.22 2.44C11.09 5 12.76 4 14.5 4 17 4 19 6 19 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path>
                            </svg>
                            <span class="count">{{ card.noteCountResponse?.likeTotal || 0 }}</span>
                          </div>
                          <div class="stat" :class="{ active: card.collect }" @click.stop="toggleCollectById(card.findNoteDetailResponse.id)" :title="card.collect ? '取消收藏' : '收藏'">
                            <svg class="icon-svg" viewBox="0 0 24 24" aria-hidden="true">
                              <path d="M12 17.27L18.18 21 16.54 13.97 22 9.24 14.81 8.62 12 2 9.19 8.63 2 9.24 7.46 13.97 5.82 21z"></path>
                            </svg>
                            <span class="count">{{ card.noteCountResponse?.collectTotal || 0 }}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                
                <!-- Infinite Scroll Sentinel for User Notes -->
                <div :ref="setSentinelRef" class="sentinel" style="height: 10px; width: 100%;"></div>
                
                <div v-if="activeProfileLoading && activeProfileCards.length > 0" class="loading-more">
                  <div class="spinner-small"></div>
                  <span>加载中...</span>
                </div>
                <div v-else-if="activeProfileError" class="error-state">
                  <span>加载失败，请检查网络</span>
                  <button @click="fetchActiveProfileFeed(true)" class="retry-btn">重试</button>
                </div>
                <div v-else-if="!activeProfileHasNext && activeProfileCards.length > 0" class="no-more">没有更多内容了</div>
                
                <div v-if="!activeProfileLoading && activeProfileCards.length === 0 && !activeProfileError" class="empty-state">
                  <div class="empty-icon"><AppIcon :name="activeProfileEmptyIcon" /></div>
                  <p>{{ activeProfileEmptyText }}</p>
                </div>
              </div>
            </div>
          </div>
        </template>

        <template v-else-if="currentView === 'search'">
          <div class="search-page">
            <div class="search-page-header">
              <div class="search-tabs">
                <button
                  class="search-tab"
                  :class="{ active: searchTab === 'note' }"
                  @click="handleSearchTabChange('note')"
                >
                  笔记
                  <span class="search-tab-count">{{ noteSearchPage.totalCount }}</span>
                </button>
                <button
                  class="search-tab"
                  :class="{ active: searchTab === 'user' }"
                  @click="handleSearchTabChange('user')"
                >
                  用户
                  <span class="search-tab-count">{{ userSearchPage.totalCount }}</span>
                </button>
              </div>
            </div>

            <div v-if="searchTab === 'note'" class="search-result-panel">
              <div class="search-note-layout">
                <div class="search-note-main">
                  <div class="search-filter-bar">
                    <el-select
                      v-model="noteSearchForm.type"
                      class="search-filter-select"
                      placeholder="全部类型"
                      clearable
                      @change="handleNoteFilterChange"
                    >
                      <el-option label="全部类型" :value="null" />
                      <el-option label="图文" :value="0" />
                      <el-option label="视频" :value="1" />
                    </el-select>

                    <el-select
                      v-model="noteSearchForm.sort"
                      class="search-filter-select"
                      placeholder="排序"
                      clearable
                      @change="handleNoteFilterChange"
                    >
                      <el-option label="不限排序" :value="null" />
                      <el-option label="最新" :value="0" />
                      <el-option label="最多点赞" :value="1" />
                      <el-option label="最多评论" :value="2" />
                      <el-option label="最多收藏" :value="3" />
                    </el-select>

                    <el-select
                      v-model="noteSearchForm.publishTimeRange"
                      class="search-filter-select"
                      placeholder="发布时间"
                      clearable
                      @change="handleNoteFilterChange"
                    >
                      <el-option label="不限时间" :value="null" />
                      <el-option label="一天内" :value="0" />
                      <el-option label="一周内" :value="1" />
                      <el-option label="半年内" :value="2" />
                    </el-select>
                  </div>

                  <div v-if="noteSearchLoading" class="search-loading">
                    <div class="spinner"></div>
                    <span>搜索中...</span>
                  </div>

                  <div v-else-if="noteSearchResults.length > 0" class="search-note-grid">
                    <div
                      v-for="note in noteSearchResults"
                      :key="note.findNoteDetailResponse.id"
                      class="search-note-card"
                      @click="openNoteDetail(note, $event)"
                    >
                      <div class="search-note-cover">
                        <template v-if="note.findNoteDetailResponse.type === 1">
                          <video
                            v-if="note.findNoteDetailResponse.videoUri"
                            :src="note.findNoteDetailResponse.videoUri"
                            class="card-video-thumbnail"
                            preload="metadata"
                            muted>
                          </video>
                          <div class="video-icon-overlay"><AppIcon name="play" :size="18" /></div>
                        </template>
                        <template v-else>
                          <img
                            v-if="note.findNoteDetailResponse.imgUris?.length"
                            :src="getFileUrl(note.findNoteDetailResponse.imgUris[0])"
                            :alt="note.findNoteDetailResponse.title"
                            loading="lazy" />
                          <div v-else class="search-cover-placeholder">文</div>
                        </template>
                      </div>
                      <div class="search-note-info">
                        <h3 class="search-note-title" v-html="getNoteTitleHtml(note)"></h3>
                        <div class="search-note-meta">
                          <span class="search-note-author">
                            <img
                              v-if="getFileUrl(note.findNoteDetailResponse.avatar) && !note._avatarFailed"
                              class="avatar"
                              :src="getFileUrl(note.findNoteDetailResponse.avatar)"
                              alt=""
                              @error="note._avatarFailed = true" />
                            <span v-else class="avatar search-avatar-fallback" aria-hidden="true">
                              {{ note.findNoteDetailResponse.creatorName?.trim().slice(0, 1) || '用' }}
                            </span>
                            {{ note.findNoteDetailResponse.creatorName }}
                          </span>
                          <span>{{ note.findNoteDetailResponse.updateTime ? note.findNoteDetailResponse.updateTime.split('T')[0] : '' }}</span>
                        </div>
                        <div class="search-note-stats">
                          <button
                            type="button"
                            class="search-stat-btn"
                            :class="{ active: note.like }"
                            :aria-label="note.like ? '取消点赞' : '点赞'"
                            :title="note.like ? '取消点赞' : '点赞'"
                            :disabled="likeQueue.has(String(note.findNoteDetailResponse.id))"
                            @click.stop="toggleLikeById(note.findNoteDetailResponse.id)">
                            <svg class="icon-svg" viewBox="0 0 24 24" aria-hidden="true">
                              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6 4 4 6.5 4c1.74 0 3.41 1 4.22 2.44C11.09 5 12.76 4 14.5 4 17 4 19 6 19 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path>
                            </svg>
                            <span class="count">{{ note.noteCountResponse?.likeTotal || 0 }}</span>
                          </button>
                          <span class="search-stat-item" title="评论">
                            <svg class="icon-svg" viewBox="0 0 24 24" aria-hidden="true">
                              <path d="M21 6H3c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h4v3.5c0 .4.5.7.9.4l5.6-3.9H21c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2z"></path>
                            </svg>
                            <span class="count">{{ note.noteCountResponse?.commentTotal || 0 }}</span>
                          </span>
                          <button
                            type="button"
                            class="search-stat-btn"
                            :class="{ active: note.collect }"
                            :aria-label="note.collect ? '取消收藏' : '收藏'"
                            :title="note.collect ? '取消收藏' : '收藏'"
                            :disabled="collectQueue.has(String(note.findNoteDetailResponse.id))"
                            @click.stop="toggleCollectById(note.findNoteDetailResponse.id)">
                            <svg class="icon-svg" viewBox="0 0 24 24" aria-hidden="true">
                              <path d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"></path>
                            </svg>
                            <span class="count">{{ note.noteCountResponse?.collectTotal || 0 }}</span>
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div v-else class="empty-state">
                    <div class="empty-icon"><AppIcon name="search" /></div>
                    <p>未找到相关笔记</p>
                  </div>

                  <el-pagination
                    v-if="noteSearchPage.totalPage > 1"
                    class="search-pagination"
                    layout="prev, pager, next"
                    :current-page="noteSearchPage.pageNo"
                    :page-size="noteSearchPage.pageSize"
                    :total="noteSearchPage.totalCount"
                    @current-change="handleNotePageChange"
                  />
                </div>

                <aside class="rag-side-panel">
                  <div class="rag-panel-header">
                    <div>
                      <div class="rag-panel-title">知识库回答</div>
                      <div class="rag-panel-subtitle">基于平台笔记生成</div>
                    </div>
                    <span v-if="searchRagLoading" class="rag-live-dot">生成中</span>
                  </div>
                  <div v-if="searchRagSteps.length" class="rag-steps">
                    <span v-for="(step, index) in searchRagSteps" :key="index">{{ step }}</span>
                  </div>
                  <div v-if="searchRagError" class="rag-error">{{ searchRagError }}</div>
                  <div v-else-if="!searchRagAnswer && searchRagLoading" class="rag-answer muted">正在检索相关笔记...</div>
                  <div v-else-if="searchRagAnswer" class="rag-answer">{{ searchRagAnswer }}</div>
                  <div v-else class="rag-answer muted">搜索后会自动生成知识库回答</div>

                  <div v-if="validSearchRagReferences.length" class="rag-reference-list">
                    <div class="rag-reference-title">参考来源</div>
                    <button
                      type="button"
                      class="rag-reference-guide"
                      @click="openRagReferencesPage">
                      <div class="rag-reference-guide-main">
                        <span>已参考 {{ validSearchRagReferences.length }} 篇笔记</span>
                        <strong>查看来源</strong>
                      </div>
                      <div class="rag-reference-preview">
                        <div
                          v-for="reference in validSearchRagReferences.slice(0, 3)"
                          :key="reference.noteId"
                          class="rag-reference-preview-item">
                          <span>{{ reference.title || '未命名笔记' }}</span>
                        </div>
                      </div>
                    </button>
                  </div>

                  <div v-if="searchRagRecommendations.length" class="rag-recommend-list">
                    <button
                      v-for="item in searchRagRecommendations"
                      :key="item"
                      type="button"
                      @click="searchKeyword = item; handleSearch()">
                      {{ item }}
                    </button>
                  </div>
                </aside>
              </div>
            </div>

            <div v-else class="search-result-panel">
              <div v-if="userSearchLoading" class="search-loading">
                <div class="spinner"></div>
                <span>搜索中...</span>
              </div>

              <div v-else-if="userSearchResults.length > 0" class="search-user-list">
                <div
                  v-for="user in userSearchResults"
                  :key="user.userId"
                  class="search-user-item"
                >
                  <img
                    v-if="getFileUrl(user.avatar) && !user._avatarFailed"
                    class="search-user-avatar"
                    :src="getFileUrl(user.avatar)"
                    :alt="user.nickname"
                    @error="user._avatarFailed = true"
                  />
                  <span v-else class="search-user-avatar search-avatar-fallback" aria-hidden="true">
                    {{ user.nickname?.trim().slice(0, 1) || '用' }}
                  </span>
                  <div class="search-user-info">
                    <div class="search-user-name" v-html="getUserNicknameHtml(user)"></div>
                    <div class="search-user-id">雨绵绵号：{{ user.ymm_id }}</div>
                  </div>
                  <div class="search-user-stats">
                    <span>{{ user.fansTotal || 0 }} 粉丝</span>
                    <span>{{ user.noteTotal || 0 }} 笔记</span>
                  </div>
                </div>
              </div>

              <div v-else class="empty-state">
                <div class="empty-icon"><AppIcon name="search" /></div>
                <p>未找到相关用户</p>
              </div>

              <el-pagination
                v-if="userSearchPage.totalPage > 1"
                class="search-pagination"
                layout="prev, pager, next"
                :current-page="userSearchPage.pageNo"
                :page-size="userSearchPage.pageSize"
                :total="userSearchPage.totalCount"
                @current-change="handleUserPageChange"
              />
            </div>
          </div>
        </template>
      </main>
    </div>

    <!-- Login Modal -->
    <Transition name="dialog-motion" @after-leave="resetDialogOrigin">
      <div v-if="showLoginModal" class="modal-overlay" :style="dialogMotionStyle" @click.self="closeModal">
        <div class="login-modal dialog-panel">
        <!-- Left Side -->
        <div class="modal-left">
          <div class="modal-logo-container">
             <img :src="logoSrc" alt="雨绵绵" class="modal-logo-img" @error="$event.target.style.display='none'; $event.target.nextElementSibling.style.display='flex'" />
             <div class="modal-logo-fallback" style="display:none;">
               <div class="modal-logo-text-main">雨绵绵</div>
             </div>
          </div>
          <p class="modal-slogan">登录后推荐更懂你的笔记</p>
        </div>

        <!-- Right Side -->
        <div class="modal-right">
          <button class="close-btn" aria-label="关闭登录弹窗" @click="closeModal"><AppIcon name="close" :size="22" /></button>
          <h2 class="modal-title">{{ loginMethod === 'phone' ? '手机号登录' : '密码登录' }}</h2>
          <p class="login-register-hint">
            {{ loginMethod === 'phone'
              ? '未注册？验证手机号后将自动注册并登录。'
              : '未注册？输入手机号和密码即可自动注册并登录。已有账号请使用原密码。' }}
          </p>
          
          <div class="login-form">
            <div v-if="loginMethod === 'phone'" class="input-container">
              <div class="input-group">
                <span class="prefix">+86</span>
                <input type="text" v-model="loginForm.phone" placeholder="输入手机号" />
              </div>
              <div class="input-group">
                <input type="text" v-model="loginForm.code" placeholder="输入验证码" />
                <button class="code-btn" @click="sendCode" :disabled="isCounting">
                  {{ isCounting ? `${countdown}秒后获取` : '获取验证码' }}
                </button>
              </div>
            </div>
            
            <div v-else class="input-container">
              <div class="input-group">
                <span class="prefix">+86</span>
                <input type="text" v-model="loginForm.account" placeholder="输入手机号" />
              </div>
              <div class="input-group">
                <input type="password" v-model="loginForm.password" placeholder="输入密码（至少8位数字或字母）" />
              </div>
            </div>

            <button class="modal-login-btn" @click="handleLogin" :disabled="isLoading">
              {{ isLoading ? '登录中...' : '登录' }}
            </button>
            
            <div class="form-footer">
              <div class="agreement">
                <input type="checkbox" id="agree" v-model="loginForm.agreed" />
                <label for="agree">我已阅读并同意《用户协议》《隐私政策》</label>
              </div>
              <div class="switch-method" @click="toggleLoginMethod">
                {{ loginMethod === 'phone' ? '密码登录' : '手机号登录' }}
              </div>
            </div>
          </div>
        </div>
        </div>
      </div>
    </Transition>
    <!-- Profile Settings Modal -->
    <Transition name="dialog-motion" @after-leave="resetDialogOrigin">
      <div v-if="showProfileSettings" class="modal-overlay" :style="dialogMotionStyle" @click.self="closeProfileSettings">
        <div class="profile-modal dialog-panel">
        <button class="close-btn" aria-label="关闭个人资料" @click="closeProfileSettings"><AppIcon name="close" :size="22" /></button>
        <h2 class="modal-title">个性设置</h2>
        
        <div class="profile-info">
          <div class="info-item">
            <span class="label">背景图</span>
            <div class="value bg-value">
              <div class="bg-upload" @click="$refs.bgInput.click()">
                <img v-if="profileForm.backgroundImgUrl" :src="profileForm.backgroundImgUrl" class="profile-bg" />
                <div v-else class="upload-placeholder">点击上传背景图</div>
                <input type="file" ref="bgInput" hidden accept="image/*" @change="handleBackgroundChange" />
              </div>
            </div>
          </div>

          <div class="info-item">
            <span class="label">头像</span>
            <div class="value avatar-value">
              <div class="avatar-upload" @click="$refs.avatarInput.click()">
                <img v-if="profileForm.avatarUrl" :src="profileForm.avatarUrl" class="profile-avatar" />
                <span v-else class="default-avatar"><AppIcon name="user" /></span>
                <div class="edit-overlay">修改</div>
                <input type="file" ref="avatarInput" hidden accept="image/*" @change="handleAvatarChange" />
              </div>
            </div>
          </div>
          
          <div class="info-item">
            <span class="label">昵称</span>
            <div class="value">
              <input type="text" v-model="profileForm.nickname" class="edit-input" placeholder="请输入昵称" />
            </div>
          </div>
          
          <div class="info-item">
            <span class="label">YMM ID</span>
            <div class="value disabled-text">{{ userStore.userInfo.ymmId || '未设置' }}</div>
          </div>
          
          <div class="info-item">
            <span class="label">性别</span>
            <div class="value">
              <select v-model="profileForm.sex" class="edit-select">
                <option :value="0">男</option>
                <option :value="1">女</option>
              </select>
            </div>
          </div>
          
          <div class="info-item">
            <span class="label">生日</span>
            <div class="value">
              <input type="date" v-model="profileForm.birthday" class="edit-input" />
            </div>
          </div>
          
          <div class="info-item">
            <span class="label">手机号</span>
            <div class="value disabled-text">{{ userStore.userInfo.phone || '未设置' }}</div>
          </div>
          
          <div class="info-item">
            <span class="label">个人简介</span>
            <div class="value intro-value">
              <textarea v-model="profileForm.introduce" class="edit-textarea" placeholder="请输入个人简介"></textarea>
            </div>
          </div>

          <div class="form-actions">
            <button class="save-btn" @click="saveProfile" :disabled="isUpdatingProfile">
              {{ isUpdatingProfile ? '保存中...' : '保存修改' }}
            </button>
          </div>
        </div>
        </div>
      </div>
    </Transition>
    <button class="ai-float-btn" @click="goToAiChat" aria-label="打开雨绵绵知识助手">
      <span class="ai-float-label">知识助手</span>
      <span class="ai-float-mark"><AiKnowledgeMark /></span>
    </button>

    <!-- Note Detail Modal -->
    <Transition name="dialog-motion" @after-leave="resetDialogOrigin">
      <div v-if="showNoteDetail" class="modal-overlay note-detail-overlay" :style="dialogMotionStyle" @click.self="closeNoteDetail">
        <button class="close-detail-btn" aria-label="关闭笔记详情" @click="closeNoteDetail"><AppIcon name="close" :size="22" /></button>
        <div class="note-detail-modal dialog-panel">
        <!-- Left Side: Media -->
        <div class="note-media-section">
          <!-- Video -->
          <video 
            v-if="currentNote.findNoteDetailResponse.type === 1 && currentNote.findNoteDetailResponse.videoUri" 
            :src="currentNote.findNoteDetailResponse.videoUri" 
            controls 
            autoplay 
            class="detail-media">
          </video>
          <!-- Images -->
          <div v-else-if="currentNote.findNoteDetailResponse.imgUris && currentNote.findNoteDetailResponse.imgUris.length > 0" class="image-carousel">
            <button 
              v-if="currentNote.findNoteDetailResponse.imgUris.length > 1 && currentImageIndex > 0" 
              class="carousel-btn prev-btn" 
              @click.stop="prevImage">
              &#10094;
            </button>
            
            <img 
              :src="getFileUrl(currentNote.findNoteDetailResponse.imgUris[currentImageIndex])" 
              class="detail-media carousel-image" 
              :key="currentImageIndex"
            />
            
            <button 
              v-if="currentNote.findNoteDetailResponse.imgUris.length > 1 && currentImageIndex < currentNote.findNoteDetailResponse.imgUris.length - 1" 
              class="carousel-btn next-btn" 
              @click.stop="nextImage">
              &#10095;
            </button>

            <!-- Dots Indicator -->
            <div v-if="currentNote.findNoteDetailResponse.imgUris.length > 1" class="carousel-dots">
              <span 
                v-for="(_, idx) in currentNote.findNoteDetailResponse.imgUris" 
                :key="idx" 
                class="dot" 
                :class="{ active: currentImageIndex === idx }"
                @click.stop="currentImageIndex = idx">
              </span>
            </div>
          </div>
          <div v-else class="no-media-placeholder">无媒体内容</div>
        </div>

        <!-- Right Side: Content -->
        <div class="note-content-section">
          <div class="author-header">
            <img :src="getFileUrl(currentNote.findNoteDetailResponse.avatar) || 'https://via.placeholder.com/40'" class="author-avatar-large" />
            <span class="author-name-large">{{ currentNote.findNoteDetailResponse.creatorName }}</span>
            <button 
              v-if="userStore.userInfo?.id !== currentNote.findNoteDetailResponse.creatorId" 
              class="follow-btn" 
              :class="{ following: currentNote.following }"
              :disabled="followQueue.has(String(currentNote.findNoteDetailResponse.creatorId))"
              @click="toggleFollowCurrentAuthor">
              {{ followQueue.has(String(currentNote.findNoteDetailResponse.creatorId)) ? '处理中...' : (currentNote.following ? '已关注' : '关注') }}
            </button>
          </div>

          <div class="scrollable-content">
            <h1 class="detail-title">{{ currentNote.findNoteDetailResponse.title }}</h1>
            <p class="detail-text">{{ currentNote.findNoteDetailResponse.content }}</p>
            <div class="detail-tags" v-if="currentNote.findNoteDetailResponse.topicName">
              <span class="tag"># {{ currentNote.findNoteDetailResponse.topicName }}</span>
            </div>
            <div class="detail-time">
              {{ currentNote.findNoteDetailResponse.updateTime ? currentNote.findNoteDetailResponse.updateTime.split('T')[0] : '' }}
            </div>
            <div class="comments-section">
              <div class="comments-title">评论</div>
              <div v-if="!getToken() || !userStore.isLoggedIn" class="comment-state">登录后查看评论</div>
              <div v-else-if="commentLoading" class="comment-state">正在加载评论...</div>
              <div v-else-if="comments.length === 0" class="comment-state">还没有评论</div>
              <div v-else class="comment-list">
                <div v-for="comment in comments" :key="comment.id" class="comment-item">
                  <img :src="getFileUrl(comment.avatar) || 'https://via.placeholder.com/32'" class="comment-user-avatar" />
                  <div class="comment-main">
                    <div class="comment-meta">
                      <span class="comment-name">{{ comment.nickname || '用户' }}</span>
                      <span class="comment-time">{{ formatCommentTime(comment.createTime) }}</span>
                    </div>
                    <div class="comment-content">{{ comment.content }}</div>
                    <div class="comment-actions">
                      <button
                        v-if="!comment.optimistic"
                        type="button"
                        class="comment-like-btn"
                        :class="{ active: comment.isLike }"
                        :disabled="commentLikeQueue.has(String(comment.id))"
                        @click="toggleCommentLike(comment)">
                        <svg class="comment-like-icon" viewBox="0 0 24 24" aria-hidden="true">
                          <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6 4 4 6.5 4c1.74 0 3.41 1 4.22 2.44C11.09 5 12.76 4 14.5 4 17 4 19 6 19 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path>
                        </svg>
                        <span>{{ comment.likeTotal || 0 }}</span>
                      </button>
                      <button v-if="!comment.optimistic" type="button" @click="setReplyTarget(comment)">回复</button>
                      <button
                        v-if="Number(comment.childCommentTotal || 0) > 0"
                        type="button"
                        @click="toggleReplyList(comment)">
                        {{ comment.replyExpanded ? '收起回复' : `查看 ${comment.childCommentTotal} 条回复` }}
                      </button>
                    </div>

                    <div v-if="comment.replyExpanded" class="reply-list">
                      <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                        <img :src="getFileUrl(reply.avatar) || 'https://via.placeholder.com/28'" class="reply-avatar" />
                        <div class="reply-main">
                          <div class="comment-meta">
                            <span class="comment-name">{{ reply.nickname || '用户' }}</span>
                            <span v-if="reply.replyNickname" class="reply-to">回复 {{ reply.replyNickname }}</span>
                            <span class="comment-time">{{ formatCommentTime(reply.createTime) }}</span>
                          </div>
                          <div class="comment-content">{{ reply.content }}</div>
                          <div class="comment-actions">
                            <button
                              v-if="!reply.optimistic"
                              type="button"
                              class="comment-like-btn"
                              :class="{ active: reply.isLike }"
                              :disabled="commentLikeQueue.has(String(reply.id))"
                              @click="toggleCommentLike(reply)">
                              <svg class="comment-like-icon" viewBox="0 0 24 24" aria-hidden="true">
                                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6 4 4 6.5 4c1.74 0 3.41 1 4.22 2.44C11.09 5 12.76 4 14.5 4 17 4 19 6 19 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path>
                              </svg>
                              <span>{{ reply.likeTotal || 0 }}</span>
                            </button>
                            <button v-if="!reply.optimistic" type="button" @click="setReplyTarget(reply, comment)">回复</button>
                          </div>
                        </div>
                      </div>
                      <button
                        v-if="comment.replyHasNext"
                        type="button"
                        class="load-more-comments"
                        :disabled="comment.replyLoading"
                        @click="loadReplyComments(comment, true)">
                        {{ comment.replyLoading ? '加载中...' : '加载更多回复' }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
              <button
                v-if="commentHasNext"
                type="button"
                class="load-more-comments"
                :disabled="commentLoadingMore"
                @click="fetchComments(true)">
                {{ commentLoadingMore ? '加载中...' : '加载更多评论' }}
              </button>
            </div>
          </div>

          <!-- Interaction Footer -->
          <div class="interaction-footer">
            <div class="interaction-stats">
              <div 
                class="stat-btn" 
                :class="{ active: currentNote.like }" 
                @click="currentNote.like ? unlikeById(currentNote.findNoteDetailResponse.id) : likeById(currentNote.findNoteDetailResponse.id)" 
                :title="currentNote.like ? '取消点赞' : '点赞'">
                <svg class="icon-svg" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6 4 4 6.5 4c1.74 0 3.41 1 4.22 2.44C11.09 5 12.76 4 14.5 4 17 4 19 6 19 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path>
                </svg>
                <span class="count">{{ currentNote.noteCountResponse?.likeTotal || 0 }}</span>
              </div>
              <div 
                class="stat-btn" 
                :class="{ active: currentNote.collect }" 
                @click="currentNote.collect ? uncollectById(currentNote.findNoteDetailResponse.id) : collectById(currentNote.findNoteDetailResponse.id)" 
                :title="currentNote.collect ? '取消收藏' : '收藏'">
                <svg class="icon-svg" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 17.27L18.18 21 16.54 13.97 22 9.24 14.81 8.62 12 2 9.19 8.63 2 9.24 7.46 13.97 5.82 21z"></path>
                </svg>
                <span class="count">{{ currentNote.noteCountResponse?.collectTotal || 0 }}</span>
              </div>
              <div class="stat-btn">
                <svg class="icon-svg" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M21 6h-18c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h4v3.5c0 .4.5.7.9.4l5.6-3.9h7.5c1.1 0 2-.9 2-2v-8c0-1.1-.9-2-2-2z"></path>
                </svg>
                <span class="count">{{ currentNote.noteCountResponse?.commentTotal || 0 }}</span>
              </div>
            </div>
            <div class="comment-input-area">
              <img :src="getFileUrl(userStore.userInfo?.avatar) || 'https://via.placeholder.com/32'" class="comment-avatar" />
              <div class="comment-input-wrap">
                <div v-if="replyTarget" class="replying-tip">
                  回复 {{ replyTarget.replyNickname }}
                  <button type="button" @click="clearReplyTarget">取消</button>
                </div>
                <input
                  v-model="commentInput"
                  type="text"
                  :placeholder="replyTarget ? `回复 ${replyTarget.replyNickname}` : '说点什么...'"
                  class="comment-input"
                  @keyup.enter="submitComment" />
              </div>
              <button
                type="button"
                class="comment-submit-btn"
                :disabled="commentSubmitting || !commentInput.trim()"
                @click="submitComment">
                发送
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Transition>
  </div>
</template>

<style scoped>
.home-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f8fa;
  color: #333;
}

/* Header Styles */
.header {
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background-color: #fff;
  z-index: 100;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.logo-img {
  height: 64px; 
  object-fit: contain;
}

.logo-fallback {
  background-color: #2b5fa5; /* Blue similar to the image */
  color: white;
  padding: 5px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-family: "SimSun", serif; /* Songti style font */
  line-height: 1.2;
  height: 64px;
}

.logo-text-main {
  font-size: 20px;
  font-weight: bold;
  letter-spacing: 2px;
}

.logo-text-sub {
  font-size: 8px;
  background-color: white;
  color: #2b5fa5;
  padding: 0 4px;
  margin-top: 2px;
}

/* Loading Skeletons */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

@media (max-width: 1400px) {
  .skeleton-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 1100px) {
  .skeleton-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .skeleton-grid { grid-template-columns: 1fr; }
}

.skeleton-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  margin-bottom: 20px;
}

.skeleton-image {
  width: 100%;
  height: 200px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
}

.skeleton-content {
  padding: 12px;
}

.skeleton-title {
  height: 14px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
  border-radius: 4px;
  margin-bottom: 8px;
}

.skeleton-title.short {
  width: 60%;
}

.skeleton-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
}

.skeleton-author {
  width: 50%;
  height: 20px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
  border-radius: 10px;
}

.skeleton-likes {
  width: 20%;
  height: 14px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
  border-radius: 4px;
  align-self: center;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.loading-more {
  text-align: center;
  padding: 20px 0;
  color: #999;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.error-state {
  text-align: center;
  padding: 20px 0;
  color: #f56c6c;
  font-size: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.retry-btn {
  padding: 6px 16px;
  border: 1px solid #f56c6c;
  background: transparent;
  color: #f56c6c;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.retry-btn:hover {
  background: #fdf0f0;
}

.sentinel {
  height: 1px;
  width: 100%;
}

.no-more {
  text-align: center;
  padding: 20px 0;
  color: #999;
  font-size: 14px;
}

.spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #2b5fa5;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #999;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.search-bar {
  position: relative;
  width: 100%;
  max-width: 400px;
}

.search-bar input {
  width: 100%;
  padding: 10px 40px 10px 20px;
  border-radius: 20px;
  border: 1px solid #e0e0e0;
  background-color: #f5f5f5;
  color: #333;
  outline: none;
}

.search-icon {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: #999;
}

/* Search Result Page */
.search-page {
  padding: 12px 0 32px;
}

.search-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.search-page-title {
  font-size: 24px;
  font-weight: 700;
  color: #333;
}

.search-tabs {
  display: flex;
  gap: 12px;
}

.search-tab {
  padding: 9px 18px;
  border-radius: 20px;
  border: 1px solid #e5e5e5;
  background: #fff;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 15px;
}

.search-tab:hover {
  color: #2b5fa5;
  border-color: #b9cde8;
}

.search-tab.active {
  color: #fff;
  background-color: #2b5fa5;
  border-color: #2b5fa5;
  font-weight: 600;
}

.search-tab-count {
  margin-left: 6px;
  font-size: 12px;
  opacity: 0.75;
}

.search-result-panel {
  min-height: 320px;
}

.search-note-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 22px;
  align-items: start;
}

.search-note-main {
  min-width: 0;
}

.search-filter-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.search-filter-select {
  width: 140px;
}

.search-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 80px 0;
  color: #999;
  font-size: 14px;
}

.search-note-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

@media (max-width: 1400px) {
  .search-note-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1100px) {
  .search-note-layout {
    grid-template-columns: 1fr;
  }

  .rag-side-panel {
    position: static;
    max-height: none;
  }

  .search-note-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .search-note-grid {
    grid-template-columns: 1fr;
  }
}

.search-note-card {
  overflow: hidden;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s;
  cursor: pointer;
}

.search-note-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 14px rgba(0, 0, 0, 0.1);
}

.search-note-cover {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: #f0f0f0;
}

.search-note-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.search-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44px;
  color: #999;
}

.search-note-info {
  padding: 12px;
}

.search-note-title {
  display: -webkit-box;
  min-height: 40px;
  overflow: hidden;
  font-size: 15px;
  line-height: 1.45;
  color: #333;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.search-note-title :deep(strong),
.search-user-name :deep(strong) {
  color: #e03e2d;
  font-weight: 700;
}

.search-note-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  color: #999;
  font-size: 12px;
}

.search-note-author {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.search-note-author .avatar {
  flex: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
}

.search-avatar-fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #e7eef8;
  color: #285b96;
  font-weight: 600;
}

.search-note-author .search-avatar-fallback {
  font-size: 11px;
}

.search-note-stats {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 10px;
  color: #666;
  font-size: 12px;
}

.search-stat-btn,
.search-stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  line-height: 1;
}

.search-stat-btn {
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font-size: 12px;
  padding: 0;
}

.search-note-stats .icon-svg {
  width: 16px;
  height: 16px;
  color: currentColor;
}

.search-note-stats .count {
  line-height: 1;
}

.search-stat-btn.active {
  color: #e84a5f;
}

.search-stat-btn.active .icon-svg path {
  fill: currentColor;
  stroke: currentColor;
}

.search-stat-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.rag-side-panel {
  position: sticky;
  top: 92px;
  max-height: calc(100vh - 112px);
  overflow-y: auto;
  border: 1px solid #dfe7f2;
  border-radius: 8px;
  background: #fbfdff;
  padding: 16px;
}

.rag-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.rag-panel-title {
  color: #1f3b5c;
  font-size: 16px;
  font-weight: 700;
}

.rag-panel-subtitle {
  margin-top: 2px;
  color: #7a8da6;
  font-size: 12px;
}

.rag-live-dot {
  flex: none;
  border-radius: 999px;
  background: #e6f4ea;
  color: #1f7a3f;
  padding: 4px 9px;
  font-size: 12px;
}

.rag-steps {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.rag-steps span {
  border-radius: 999px;
  background: #edf3fa;
  color: #4b6b91;
  padding: 4px 8px;
  font-size: 12px;
}

.rag-answer {
  color: #26384d;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.rag-answer.muted,
.rag-error {
  color: #7a8da6;
}

.rag-error {
  border-radius: 8px;
  background: #fff4f3;
  color: #bd3b2f;
  padding: 10px 12px;
  font-size: 13px;
}

.rag-reference-list {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #e6edf6;
}

.rag-reference-title {
  margin-bottom: 10px;
  color: #1f3b5c;
  font-size: 14px;
  font-weight: 700;
}

.rag-reference-guide {
  width: 100%;
  border: 1px solid #d8e5f4;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
  padding: 12px;
  text-align: left;
  cursor: pointer;
}

.rag-reference-guide:hover {
  border-color: #a9c3e6;
}

.rag-reference-guide-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #1d2939;
  font-size: 14px;
  font-weight: 700;
}

.rag-reference-guide-main strong {
  flex: 0 0 auto;
  color: #2b5fa5;
  font-size: 13px;
}

.rag-reference-preview {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 10px;
}

.rag-reference-preview-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #667085;
  font-size: 12px;
}

.rag-reference-preview-item span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rag-reference-preview-item small {
  flex: 0 0 auto;
  color: #8aa2bf;
  font-size: 12px;
}

.rag-recommend-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.rag-recommend-list button {
  border: 1px solid #d5e2f2;
  border-radius: 999px;
  background: #fff;
  color: #2b5fa5;
  cursor: pointer;
  padding: 5px 9px;
  font-size: 12px;
}

.search-user-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-user-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.search-user-avatar {
  flex: none;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  background: #eee;
}

.search-user-avatar.search-avatar-fallback {
  font-size: 22px;
}

.search-user-info {
  flex: 1;
  min-width: 0;
}

.search-user-name {
  overflow: hidden;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.search-user-id {
  margin-top: 4px;
  color: #999;
  font-size: 13px;
}

.search-user-stats {
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  color: #666;
  font-size: 13px;
}

.search-pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.header-right .nav-link {
  color: #666;
  text-decoration: none;
  margin-left: 20px;
  font-size: 14px;
}

/* Main Layout */
.main-layout {
  display: flex;
  margin-top: 72px; /* Header height */
  height: calc(100vh - 72px);
}

/* Sidebar Styles */
.sidebar {
  width: 240px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 72px;
  bottom: 0;
  left: 0;
  overflow-y: auto;
  background-color: #fff;
  transition: width 0.3s ease, padding 0.3s ease;
  justify-content: space-between;
}

.side-nav {
  flex: 1;
}

.side-link {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  color: #333;
  text-decoration: none;
  border-radius: 24px;
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
}

.side-link:hover, .side-link.active {
  background-color: #edf3fa;
  color: #2b5fa5;
  font-weight: bold;
}

.side-link .icon {
  margin-right: 12px;
  font-size: 20px;
  min-width: 20px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-avatar-small {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

.side-link .text {
  transition: opacity 0.3s;
}

.notification-badge {
  min-width: 20px;
  height: 20px;
  margin-left: auto;
  padding: 0 6px;
  color: #fff;
  border-radius: 999px;
  background: #e64b5f;
  font-size: 11px;
  font-weight: 700;
  line-height: 20px;
  text-align: center;
}

.login-section {
  margin-top: 40px;
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 12px;
  border: 1px solid #eee;
  transition: opacity 0.3s, margin 0.3s, padding 0.3s, height 0.3s;
  overflow: hidden;
}

.more-section {
  margin-top: auto;
  padding-top: 20px;
}

.more-menu-container {
  position: relative;
}

.more-popover {
  position: absolute;
  bottom: 100%;
  left: 0;
  width: 140px; /* 固定宽度，避免过宽 */
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
  padding: 8px 0;
  margin-bottom: 12px;
  z-index: 10;
  border: 1px solid #eee;
  /* 优化折叠后的显示 */
  white-space: nowrap;
}

@media (max-width: 992px) {
  .more-popover {
    left: 50%;
    transform: translateX(-50%);
  }
}

.more-item {
  padding: 12px 20px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.more-item:hover {
  background-color: #f5f5f5;
  color: #2b5fa5;
}

/* Profile Modal Styles */
.profile-modal {
  width: 600px;
  background-color: #fff;
  border-radius: 20px;
  padding: 30px;
  position: relative;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  max-height: 90vh;
  overflow-y: auto;
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.label {
  color: #666;
  font-size: 15px;
  width: 80px;
}

.value {
  color: #333;
  font-weight: 500;
  font-size: 15px;
  flex: 1;
  display: flex;
  justify-content: flex-end;
}

.disabled-text {
  color: #999;
  background-color: #f5f5f5;
  padding: 5px 10px;
  border-radius: 4px;
}

.profile-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
}

.default-avatar {
  font-size: 40px;
}

.avatar-value {
  display: flex;
  align-items: center;
}

.intro-value {
  width: 100%;
}

.edit-input, .edit-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  text-align: right;
  background: transparent;
}

.edit-input:focus, .edit-select:focus, .edit-textarea:focus {
  border-color: #2b5fa5;
}

.edit-textarea {
  width: 100%;
  height: 80px;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  resize: none;
  font-family: inherit;
}

.avatar-upload {
  position: relative;
  cursor: pointer;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  overflow: hidden;
}

.edit-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  background-color: rgba(0,0,0,0.5);
  color: #fff;
  font-size: 10px;
  text-align: center;
  padding: 2px 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-upload:hover .edit-overlay {
  opacity: 1;
}

.bg-value {
  width: 100%;
  justify-content: center;
}

.bg-upload {
  width: 100%;
  height: 120px;
  background-color: #f5f5f5;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  border: 1px dashed #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bg-upload:hover {
  border-color: #2b5fa5;
}

.profile-bg {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-placeholder {
  color: #999;
  font-size: 14px;
}

.form-actions {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.save-btn {
  background-color: #2b5fa5;
  color: #fff;
  border: none;
  padding: 10px 40px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
}

.save-btn:hover {
  background-color: #234d85;
}

.save-btn:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

/* Profile Page Styles */
.profile-page {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.profile-header {
  position: relative;
  height: 300px;
  background-size: cover; /* 使用 cover 实现更好的自适应效果，保持比例不拉伸 */
  background-position: center center;
  background-repeat: no-repeat;
  border-radius: 16px 16px 0 0;
  background-color: #1a1a1a; /* Fallback for no background image */
  margin-bottom: 20px;
}

.profile-header-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(to top, rgba(0,0,0,0.8) 0%, rgba(0,0,0,0) 100%);
  display: flex;
  align-items: flex-end;
  padding: 40px;
  border-radius: 16px 16px 0 0;
}

.profile-header-content {
  display: flex;
  align-items: center;
  gap: 30px;
}

.profile-main-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 4px solid #fff;
  overflow: hidden;
  background-color: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-main-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-avatar-large {
  font-size: 60px;
}

.profile-info-text {
  color: #fff;
}

.profile-name {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 8px;
}

.profile-id {
  font-size: 14px;
  opacity: 0.8;
  margin-bottom: 12px;
}

.profile-intro {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 16px;
  max-width: 600px;
}

.profile-gender {
  margin-bottom: 16px;
}

.gender-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  font-size: 12px;
}

.gender-icon.male {
  background-color: rgba(64, 158, 255, 0.2);
  color: #409EFF;
}

.gender-icon.female {
  background-color: rgba(255, 105, 180, 0.2);
  color: #FF69B4;
}

.profile-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.stat-num {
  font-weight: bold;
  font-size: 16px;
}

.stat-label {
  opacity: 0.8;
}

.profile-tabs {
  display: flex;
  justify-content: center;
  gap: 40px;
  border-bottom: 1px solid #eee;
  padding-bottom: 16px;
  margin-bottom: 30px;
}

.tab-item {
  font-size: 16px;
  color: #666;
  cursor: pointer;
  position: relative;
  padding: 4px 8px;
  transition: color 0.3s;
}

.tab-item:hover {
  color: #333;
}

.tab-item.active {
  color: #333;
  font-weight: bold;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -17px;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 3px;
  background-color: #2b5fa5;
  border-radius: 2px;
}

.tab-count {
  font-size: 12px;
  color: #999;
  margin-left: 4px;
}

.profile-tab-content {
  min-height: 300px;
  width: 100%;
}

.user-content {
  width: 100%;
}

.no-more {
  text-align: center;
  padding: 20px 0;
  color: #999;
  font-size: 14px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.login-btn {
  width: 100%;
  padding: 10px;
  background-color: #2b5fa5;
  color: #fff;
  border: none;
  border-radius: 20px;
  font-weight: bold;
  cursor: pointer;
  margin-bottom: 20px;
}

.login-text p {
  color: #666;
  font-size: 14px;
  margin-bottom: 10px;
}

.login-text ul {
  list-style: none;
  color: #999;
  font-size: 12px;
  line-height: 1.8;
}

/* Main Content Styles */
.content {
  flex: 1;
  margin-left: 240px; /* Sidebar width */
  padding: 20px 40px;
  overflow-y: auto;
  transition: margin-left 0.3s ease;
}

.categories {
  display: flex;
  gap: 10px;
  margin-bottom: 30px;
  flex-wrap: wrap;
}

.cat-btn {
  padding: 8px 16px;
  background-color: transparent;
  color: #666;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 16px;
}

.cat-btn:hover, .cat-btn.active {
  background-color: #f0f0f0;
  color: #333;
  font-weight: bold;
}
.cat-btn.active {
  background-color: #edf3fa;
  color: #2b5fa5;
}


/* Masonry Grid Simulation with CSS Columns */
.masonry-grid {
  column-count: 4;
  column-gap: 20px;
}

.masonry-item {
  break-inside: avoid;
  margin-bottom: 20px;
}

@media (max-width: 1400px) {
  .masonry-grid {
    column-count: 3;
  }
}

@media (max-width: 1100px) {
  .masonry-grid {
    column-count: 2;
  }
}

@media (max-width: 768px) {
  .masonry-grid {
    column-count: 1;
  }
}

/* Responsive Sidebar */
@media (max-width: 992px) {
  .sidebar {
    width: 80px;
    padding: 20px 10px;
  }
  
  .content {
    margin-left: 80px;
    padding: 20px;
  }

  .side-link .text {
    opacity: 0;
    width: 0;
    display: none;
  }

  .side-link {
    justify-content: center;
    padding: 12px 0;
  }

  .side-link .icon {
    margin-right: 0;
  }

  .login-section {
    opacity: 0;
    height: 0;
    padding: 0;
    margin: 0;
    border: none;
  }
}

.card {
  break-inside: avoid;
  background-color: #fff;
  margin-bottom: 20px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  transition: transform 0.2s;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.card-image {
  position: relative;
  background-color: #f0f0f0; /* Fallback background for video/image */
  min-height: 100px;
}

.card-image img, .card-image .card-video-thumbnail {
  width: 100%;
  display: block;
  border-radius: 12px 12px 0 0;
  object-fit: cover;
}

.video-icon-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0,0,0,0.4);
  color: white;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  backdrop-filter: blur(2px);
  pointer-events: none;
}

.card-content {
  padding: 12px;
}

.card-title {
  font-size: 14px;
  margin-bottom: 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: #333;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.author {
  display: flex;
  align-items: center;
}

.avatar {
  width: 20px;
  height: 20px;
  background-color: #eee;
  border-radius: 50%;
  margin-right: 6px;
  object-fit: cover;
  border: 1px solid #f0f0f0;
}

.name {
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #666;
}

.likes {
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-stats .stat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 12px;
}

.card-stats .stat .count {
  line-height: 1;
}

.icon-svg {
  width: 18px;
  height: 18px;
  color: #666;
}

.icon-svg path {
  fill: none;
  stroke: currentColor;
  stroke-width: 1.6;
}

.card-stats .stat.active .icon-svg,
.interaction-stats .stat-btn.active .icon-svg {
  color: #2b5fa5;
}

.card-stats .stat.active .icon-svg path,
.interaction-stats .stat-btn.active .icon-svg path {
  fill: currentColor;
  stroke: currentColor;
}

.like-icon {
  font-size: 14px;
  filter: grayscale(100%);
  opacity: 0.6;
}

.like-icon.active {
  filter: none;
  opacity: 1;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.login-modal {
  display: flex;
  width: 800px;
  height: 480px;
  background-color: #fff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  position: relative;
}

@media (max-width: 850px) {
  .login-modal {
    width: 90%;
    max-width: 400px;
    height: auto;
    flex-direction: column;
    padding: 20px 0;
  }
}

.modal-left {
  flex: 1;
  background-color: #f0f6ff; /* Very light blue */
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-right: 1px solid #e6e6e6;
}

@media (max-width: 850px) {
  .modal-left {
    background-color: transparent;
    border-right: none;
    flex: none;
    padding: 20px 20px 0;
    flex-direction: column-reverse; /* Slogan above logo */
  }
}

.modal-logo-img {
  width: 200px;
  height: 200px;
  object-fit: contain;
  margin-bottom: 20px;
}

@media (max-width: 850px) {
  .modal-logo-img {
    width: 140px;
    height: 140px;
    margin-bottom: 10px;
  }
}

.modal-logo-fallback {
  width: 200px;
  height: 200px;
  background-color: #2b5fa5;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-family: "SimSun", serif;
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 20px;
}

@media (max-width: 850px) {
  .modal-logo-fallback {
    width: 140px;
    height: 140px;
    font-size: 24px;
    margin-bottom: 10px;
  }
}

.modal-slogan {
  color: #2b5fa5;
  font-size: 16px;
  font-weight: bold;
  letter-spacing: 1px;
}

@media (max-width: 850px) {
  .modal-slogan {
    background-color: #f0f6ff;
    padding: 8px 16px;
    border-radius: 20px;
    margin-bottom: 15px;
    font-size: 14px;
  }
}

.modal-right {
  flex: 1;
  padding: 40px;
  position: relative;
  display: flex;
  flex-direction: column;
}

@media (max-width: 850px) {
  .modal-right {
    padding: 10px 30px 30px;
    flex: none;
    position: static; /* Let the modal container be the relative parent */
  }
}

.close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  z-index: 10;
}

@media (max-width: 850px) {
  .close-btn {
    top: 10px;
    right: 15px;
    color: #666; /* Slightly darker for better visibility on potentially different backgrounds */
  }
}

.modal-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 30px;
  text-align: center;
}

@media (max-width: 850px) {
  .modal-title {
    display: none; /* Hide title on mobile to save space, keeping layout cleaner */
  }
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.login-register-hint {
  margin: -4px 0 16px;
  color: #667085;
  font-size: 13px;
  line-height: 1.6;
}

.input-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.input-group {
  display: flex;
  align-items: center;
  background-color: #f5f5f5;
  border-radius: 24px;
  padding: 12px 20px;
  border: 1px solid transparent;
}

.input-group:focus-within {
  border-color: #2b5fa5;
  background-color: #fff;
}

.prefix {
  color: #333;
  margin-right: 10px;
  font-weight: bold;
}

.input-group input {
  border: none;
  background: transparent;
  outline: none;
  flex: 1;
  font-size: 16px;
  color: #333;
}

.code-btn {
  border: none;
  background: none;
  color: #2b5fa5;
  font-size: 14px;
  cursor: pointer;
  margin-left: 10px;
}

.code-btn:disabled {
  color: #999;
  cursor: not-allowed;
}

.modal-login-btn {
  background-color: #2b5fa5;
  color: #fff;
  border: none;
  padding: 12px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  margin-top: 10px;
  transition: background-color 0.2s;
}

.modal-login-btn:hover {
  background-color: #234d85;
}

.modal-login-btn:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

.form-footer {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 12px;
  color: #999;
}

.agreement {
  display: flex;
  align-items: center;
  justify-content: center;
}

.agreement input {
  margin-right: 5px;
}

.switch-method {
  text-align: center;
  color: #2b5fa5;
  cursor: pointer;
  margin-top: 10px;
}

.ai-float-btn {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 300;
  width: 72px;
  height: 72px;
  border: 2px solid #fff;
  border-radius: 18px 18px 18px 7px;
  background: #2859a6;
  box-shadow: 0 12px 30px rgba(40, 89, 166, 0.28);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.24s cubic-bezier(0.16, 1, 0.3, 1), border-radius 0.24s ease, box-shadow 0.24s ease;
}

.ai-float-btn:hover {
  transform: translateY(-3px);
  border-radius: 18px 18px 18px 12px;
  box-shadow: 0 16px 34px rgba(40, 89, 166, 0.34);
}

.ai-float-mark {
  width: 40px;
  height: 40px;
  color: #fff;
  --ai-mark-node: #2859a6;
  --ai-mark-node-stroke: #fff;
  --ai-mark-core: #fff;
}

.ai-float-label {
  position: absolute;
  right: calc(100% + 10px);
  top: 50%;
  padding: 7px 10px;
  border: 1px solid var(--ymm-line);
  border-radius: 8px;
  color: var(--ymm-ink);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: var(--ymm-shadow-soft);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transform: translate(6px, -50%);
  transition: opacity 0.18s ease, transform 0.24s cubic-bezier(0.16, 1, 0.3, 1);
}

.ai-float-btn:hover .ai-float-label,
.ai-float-btn:focus-visible .ai-float-label {
  opacity: 1;
  transform: translate(0, -50%);
}

@media (max-width: 768px) {
  .ai-float-btn {
    right: 16px;
    bottom: 16px;
    width: 64px;
    height: 64px;
  }
  .ai-float-mark {
    width: 32px;
    height: 32px;
  }
}

/* Note Detail Modal Styles */
.note-detail-overlay {
  z-index: 2000;
  display: flex;
  justify-content: center;
  align-items: center;
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.7);
}

.close-detail-btn {
  position: absolute;
  top: 20px;
  right: 30px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  font-size: 30px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  cursor: pointer;
  z-index: 2010;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s;
}

.close-detail-btn:hover {
  background: rgba(255, 255, 255, 0.4);
}

.note-detail-modal {
  background: white;
  width: 80vw;
  max-width: 1000px;
  height: 85vh;
  border-radius: 12px;
  display: flex;
  overflow: hidden;
  position: relative;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
}

.note-media-section {
  flex: 1.5;
  background-color: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

.detail-media {
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.image-carousel {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.carousel-image {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0.8; }
  to { opacity: 1; }
}

.carousel-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: none;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  cursor: pointer;
  z-index: 10;
  transition: all 0.3s;
  backdrop-filter: blur(4px);
}

.carousel-btn:hover {
  background: rgba(255, 255, 255, 0.4);
  transform: translateY(-50%) scale(1.1);
}

.prev-btn {
  left: 20px;
}

.next-btn {
  right: 20px;
}

.carousel-dots {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  background: rgba(0, 0, 0, 0.3);
  padding: 6px 12px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
}

.carousel-dots .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s;
}

.carousel-dots .dot.active {
  background: white;
  width: 12px;
  border-radius: 4px;
}

.no-media-placeholder {
  color: #fff;
}

.note-content-section {
  flex: 1;
  min-width: 350px;
  display: flex;
  flex-direction: column;
  background: white;
}

.author-header {
  padding: 20px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
}

.author-avatar-large {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 12px;
  border: 1px solid #eee;
}

.author-name-large {
  font-weight: 600;
  color: #333;
  flex: 1;
  font-size: 16px;
}

.follow-btn {
  padding: 6px 16px;
  border-radius: 20px;
  border: 1px solid #2b5fa5;
  background: #2b5fa5;
  color: white;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.follow-btn.following {
  background: transparent;
  color: #999;
  border-color: #ddd;
}

.follow-btn:disabled {
  opacity: 0.6;
  cursor: wait;
}

.scrollable-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  line-height: 1.4;
}

.detail-text {
  font-size: 15px;
  color: #333;
  line-height: 1.6;
  white-space: pre-wrap;
  margin-bottom: 16px;
}

.detail-tags {
  margin-bottom: 16px;
}

.tag {
  color: #135bb5;
  font-size: 14px;
  cursor: pointer;
}

.detail-time {
  font-size: 12px;
  color: #999;
}

.comments-section {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid #f0f0f0;
}

.comments-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 14px;
}

.comment-state {
  padding: 18px 0;
  color: #999;
  font-size: 14px;
  text-align: center;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.comment-item,
.reply-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.comment-user-avatar,
.reply-avatar {
  flex-shrink: 0;
  border-radius: 50%;
  object-fit: cover;
  background: #f0f0f0;
}

.comment-user-avatar {
  width: 32px;
  height: 32px;
}

.reply-avatar {
  width: 28px;
  height: 28px;
}

.comment-main,
.reply-main {
  min-width: 0;
  flex: 1;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 5px;
}

.comment-name {
  font-size: 13px;
  font-weight: 600;
  color: #555;
}

.reply-to,
.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-content {
  color: #333;
  font-size: 14px;
  line-height: 1.55;
  word-break: break-word;
  white-space: pre-wrap;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
}

.comment-actions button,
.load-more-comments,
.replying-tip button {
  border: none;
  background: transparent;
  color: #2b5fa5;
  cursor: pointer;
  font-size: 12px;
  padding: 0;
}

.comment-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.comment-actions button:hover,
.load-more-comments:hover,
.replying-tip button:hover {
  color: #1f4a87;
}

.comment-like-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.comment-like-btn.active {
  color: #e84a5f;
}

.comment-like-btn.active .comment-like-icon {
  fill: currentColor;
}

.comment-like-icon {
  width: 13px;
  height: 13px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
}

.reply-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
  padding-left: 8px;
  border-left: 2px solid #eef2f7;
}

.load-more-comments {
  margin-top: 14px;
}

.load-more-comments:disabled {
  color: #aaa;
  cursor: not-allowed;
}

.interaction-footer {
  padding: 15px 20px;
  border-top: 1px solid #f0f0f0;
}

.interaction-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 15px;
}

.stat-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  cursor: pointer;
  font-size: 14px;
  transition: color 0.3s;
}

.stat-btn.active .icon {
  color: #2b5fa5;
}

.stat-btn .icon {
  font-size: 20px;
}

.comment-input-area {
  display: flex;
  gap: 12px;
  align-items: center;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.comment-input {
  width: 100%;
  flex: 1;
  background: #f5f5f5;
  border: none;
  border-radius: 20px;
  padding: 10px 16px;
  font-size: 14px;
  outline: none;
  color: #333;
}

.comment-input:focus {
  background: #fff;
  border: 1px solid #ddd;
}

.comment-input-wrap {
  min-width: 0;
  flex: 1;
}

.replying-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  color: #777;
  font-size: 12px;
}

.comment-submit-btn {
  flex-shrink: 0;
  border: none;
  border-radius: 18px;
  background: #2b5fa5;
  color: white;
  cursor: pointer;
  padding: 8px 16px;
  font-size: 14px;
}

.comment-submit-btn:disabled {
  background: #c8d1dc;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .note-detail-modal {
    flex-direction: column;
    height: 100vh;
    width: 100vw;
    border-radius: 0;
  }
  
  .note-media-section {
    flex: none;
    height: 50vh;
  }
  
  .note-content-section {
    flex: 1;
  }
  
  .close-detail-btn {
    top: 10px;
    right: 10px;
  }
}

/* ===== Blue-ink reading room visual system ===== */
.home-container {
  background:
    linear-gradient(rgba(40, 89, 166, 0.035) 1px, transparent 1px),
    var(--ymm-canvas);
  background-size: 100% 36px;
  color: var(--ymm-text);
}

.header {
  height: 76px;
  padding: 0 clamp(18px, 2.5vw, 40px);
  border-bottom: 1px solid rgba(40, 89, 166, 0.14);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: none;
  backdrop-filter: blur(18px);
}

.header-left {
  width: 216px;
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
}

.logo-img {
  height: 54px;
  width: auto;
}

.header-center {
  padding: 0 28px;
}

.search-bar {
  max-width: 560px;
}

.search-bar input {
  height: 44px;
  padding: 0 52px 0 18px;
  border: 1px solid var(--ymm-line);
  border-radius: var(--ymm-radius-md);
  background: #f8fafd;
  color: var(--ymm-ink);
  font-size: 14px;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.search-bar input::placeholder {
  color: #929caf;
}

.search-bar input:focus {
  border-color: var(--ymm-primary);
  background: #fff;
  box-shadow: 0 0 0 4px rgba(40, 89, 166, 0.1);
}

.search-icon {
  right: 6px;
  width: 34px;
  height: 34px;
  padding: 8px;
  border: 0;
  border-radius: 9px;
  background: var(--ymm-primary);
  color: #fff;
  display: grid;
  place-items: center;
}

.search-icon svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
}

.header-right {
  width: 216px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.header-right .nav-link {
  margin: 0;
  padding: 8px 10px;
  border-radius: var(--ymm-radius-sm);
  color: var(--ymm-text);
  font-size: 13px;
  font-weight: 600;
  transition: color 0.18s ease, background 0.18s ease;
}

.header-right .nav-link:hover {
  color: var(--ymm-primary);
  background: var(--ymm-primary-soft);
}

.main-layout {
  margin-top: 76px;
  height: calc(100vh - 76px);
}

.sidebar {
  top: 76px;
  width: 224px;
  padding: 28px 18px 22px;
  border-right: 1px solid var(--ymm-line-soft);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(14px);
}

.side-nav {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.side-link {
  min-height: 46px;
  margin: 0;
  padding: 10px 13px;
  border-radius: 10px;
  color: #4f5b70;
  font-size: 15px;
  font-weight: 500;
  transition: color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
}

.side-link:hover {
  color: var(--ymm-primary);
  background: rgba(40, 89, 166, 0.06);
}

.side-link.active {
  color: var(--ymm-primary);
  background: var(--ymm-primary-soft);
  box-shadow: inset 3px 0 0 var(--ymm-primary);
  font-weight: 700;
}

.side-link .icon {
  min-width: 22px;
  margin-right: 12px;
}

.nav-icon {
  width: 21px;
  height: 21px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.notification-badge {
  background: var(--ymm-danger);
  box-shadow: 0 0 0 3px #fff;
}

.login-section {
  margin-top: 24px;
  padding: 18px;
  border: 1px solid #cfdced;
  border-radius: var(--ymm-radius-md);
  background: #f6f9fe;
}

.login-btn,
.modal-login-btn,
.comment-submit-btn,
.follow-btn,
.retry-btn {
  border-radius: 10px;
  background: var(--ymm-primary);
  box-shadow: none;
  font-weight: 700;
}

.login-btn:hover,
.modal-login-btn:hover,
.comment-submit-btn:hover:not(:disabled),
.follow-btn:hover:not(:disabled) {
  background: var(--ymm-primary-strong);
}

.login-text p {
  color: var(--ymm-ink);
  font-weight: 700;
}

.login-text ul {
  color: var(--ymm-muted);
  line-height: 1.65;
}

.login-text li {
  position: relative;
  padding-left: 13px;
  margin-top: 7px;
}

.login-text li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.68em;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--ymm-primary);
}

.more-popover {
  border-color: var(--ymm-line);
  border-radius: var(--ymm-radius-md);
  box-shadow: var(--ymm-shadow-float);
}

.more-item {
  color: var(--ymm-text);
}

.more-item:hover {
  color: var(--ymm-primary);
  background: var(--ymm-primary-soft);
}

.content {
  margin-left: 224px;
  padding: 28px clamp(24px, 3.5vw, 56px) 48px;
}

.categories {
  gap: 6px;
  margin-bottom: 24px;
  padding-bottom: 13px;
  border-bottom: 1px solid var(--ymm-line);
}

.cat-btn {
  padding: 7px 13px;
  border-radius: 8px;
  color: var(--ymm-muted);
  font-size: 14px;
  font-weight: 600;
}

.cat-btn:hover {
  color: var(--ymm-primary);
  background: rgba(40, 89, 166, 0.06);
}

.cat-btn.active {
  color: #fff;
  background: var(--ymm-primary);
  font-weight: 700;
}

.masonry-grid {
  column-gap: 18px;
}

.card,
.search-note-card,
.skeleton-card {
  margin-bottom: 18px;
  border: 1px solid var(--ymm-line-soft);
  border-radius: var(--ymm-radius-md);
  background: var(--ymm-paper);
  box-shadow: 0 1px 0 rgba(23, 35, 59, 0.03);
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.card:hover,
.search-note-card:hover {
  transform: translateY(-3px);
  border-color: #b9c9df;
  box-shadow: var(--ymm-shadow-soft);
}

.card-image {
  background: #e9eef5;
}

.card-image img,
.card-image .card-video-thumbnail {
  border-radius: 11px 11px 0 0;
}

.card-content {
  padding: 13px 14px 14px;
}

.card-title,
.search-note-title {
  color: var(--ymm-ink);
  font-family: var(--ymm-font-serif);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.55;
}

.card-footer {
  margin-top: 11px;
  color: var(--ymm-muted);
}

.avatar {
  border-color: var(--ymm-line);
}

.name,
.card-stats .stat,
.icon-svg {
  color: #69758a;
}

.card-stats .stat {
  padding: 4px;
  border-radius: 6px;
  cursor: pointer;
}

.card-stats .stat:hover {
  color: var(--ymm-primary);
  background: var(--ymm-primary-soft);
}

.card-stats .stat.active .icon-svg,
.interaction-stats .stat-btn.active .icon-svg {
  color: var(--ymm-danger);
}

.video-icon-overlay {
  width: 42px;
  height: 42px;
  background: rgba(20, 32, 51, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.search-page-title,
.profile-name,
.detail-title,
.modal-title {
  font-family: var(--ymm-font-serif);
  color: var(--ymm-ink);
  letter-spacing: 0.01em;
}

.search-tab {
  border-color: var(--ymm-line);
  border-radius: 9px;
  color: var(--ymm-muted);
}

.search-tab.active {
  border-color: var(--ymm-primary);
  background: var(--ymm-primary);
}

.search-filter-select :deep(.el-select__wrapper) {
  border-radius: 9px;
  box-shadow: 0 0 0 1px var(--ymm-line) inset;
}

.rag-side-panel {
  border-color: #cfdced;
  border-radius: var(--ymm-radius-md);
  background: #f7faff;
}

.profile-header {
  height: 280px;
  border: 1px solid var(--ymm-line);
  border-radius: var(--ymm-radius-lg);
  background-color: #203b65;
  overflow: hidden;
}

.profile-header-overlay {
  padding: 34px;
  border-radius: var(--ymm-radius-lg);
  background: linear-gradient(to top, rgba(12, 25, 45, 0.88), rgba(12, 25, 45, 0.05) 72%);
}

.profile-main-avatar {
  width: 104px;
  height: 104px;
  border-width: 3px;
  box-shadow: 0 8px 24px rgba(9, 19, 34, 0.25);
}

.profile-name {
  color: #fff;
}

.profile-tabs {
  justify-content: flex-start;
  gap: 30px;
  padding: 0 6px 14px;
  border-bottom-color: var(--ymm-line);
}

.tab-item {
  color: var(--ymm-muted);
}

.tab-item.active {
  color: var(--ymm-primary);
}

.tab-item.active::after {
  bottom: -15px;
  width: 100%;
  height: 2px;
  background: var(--ymm-primary);
}

.empty-state,
.no-more,
.loading-more {
  color: var(--ymm-muted);
}

.modal-overlay,
.note-detail-overlay {
  background: rgba(15, 27, 46, 0.62);
  backdrop-filter: blur(8px);
}

.login-modal {
  width: min(860px, calc(100vw - 40px));
  height: 500px;
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: var(--ymm-radius-lg);
  box-shadow: var(--ymm-shadow-float);
}

.modal-left {
  border-right-color: #d7e1ef;
  background:
    linear-gradient(rgba(40, 89, 166, 0.07) 1px, transparent 1px),
    #eef4fc;
  background-size: 100% 32px;
}

.modal-logo-img {
  width: 220px;
  height: auto;
  border-radius: 4px;
  box-shadow: 0 12px 30px rgba(40, 89, 166, 0.16);
}

.modal-slogan {
  color: var(--ymm-primary);
  font-family: var(--ymm-font-serif);
  font-size: 17px;
  letter-spacing: 0.08em;
}

.modal-right {
  min-width: 0;
  padding: 46px;
}

.close-btn {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  color: var(--ymm-muted);
}

.close-btn:hover {
  color: var(--ymm-ink);
  background: var(--ymm-canvas);
}

.modal-title {
  margin-bottom: 26px;
  font-size: 26px;
}

.input-group {
  min-height: 48px;
  padding: 10px 14px;
  border: 1px solid var(--ymm-line);
  border-radius: 10px;
  background: #f8fafd;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.input-group:focus-within {
  border-color: var(--ymm-primary);
  background: #fff;
  box-shadow: 0 0 0 4px rgba(40, 89, 166, 0.09);
}

.input-group input {
  min-width: 0;
  color: var(--ymm-ink);
  font-size: 15px;
}

.code-btn {
  flex: 0 0 auto;
}

.code-btn,
.switch-method {
  color: var(--ymm-primary);
}

.modal-login-btn {
  min-height: 46px;
  padding: 10px 16px;
}

.note-detail-modal {
  width: min(1180px, 90vw);
  height: min(820px, 88vh);
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: var(--ymm-radius-lg);
  box-shadow: 0 28px 80px rgba(8, 18, 34, 0.32);
}

.note-media-section {
  background: #101827;
}

.note-content-section {
  background: #fff;
}

.author-header,
.interaction-footer {
  border-color: var(--ymm-line-soft);
}

.author-name-large,
.detail-text,
.comment-content {
  color: var(--ymm-text);
}

.detail-title {
  font-size: 22px;
  line-height: 1.45;
}

.tag,
.comment-actions button,
.load-more-comments,
.replying-tip button {
  color: var(--ymm-primary);
}

.comment-input {
  border: 1px solid var(--ymm-line);
  border-radius: 10px;
  background: #f8fafd;
}

.comment-input:focus {
  border-color: var(--ymm-primary);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(40, 89, 166, 0.08);
}

.ai-float-btn {
  width: 62px;
  height: 62px;
  border-width: 2px;
  background: var(--ymm-primary);
  box-shadow: 0 12px 30px rgba(40, 89, 166, 0.28);
  animation: none;
}

.ai-float-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 34px rgba(40, 89, 166, 0.34);
}

@media (max-width: 992px) {
  .header-left,
  .header-right {
    width: auto;
  }

  .sidebar {
    width: 82px;
    padding: 22px 10px;
  }

  .content {
    margin-left: 82px;
    padding: 24px;
  }
}

@media (max-width: 768px) {
  .home-container {
    min-height: 100vh;
    height: auto;
  }

  .header {
    height: 64px;
    padding: 0 12px;
    gap: 10px;
  }

  .logo-img {
    width: 88px;
    height: auto;
  }

  .header-center {
    min-width: 0;
    padding: 0;
  }

  .search-bar input {
    height: 40px;
    padding-left: 13px;
    padding-right: 42px;
    font-size: 13px;
  }

  .search-icon {
    width: 30px;
    height: 30px;
    right: 5px;
  }

  .header-right {
    display: none;
  }

  .main-layout {
    display: block;
    height: auto;
    min-height: calc(100vh - 64px);
    margin-top: 64px;
  }

  .sidebar {
    top: auto;
    right: 0;
    bottom: 0;
    z-index: 900;
    width: 100%;
    height: calc(64px + env(safe-area-inset-bottom));
    padding: 5px 8px env(safe-area-inset-bottom);
    border-top: 1px solid var(--ymm-line);
    border-right: 0;
    overflow: visible;
    background: rgba(255, 255, 255, 0.96);
  }

  .side-nav {
    width: 100%;
    height: 58px;
    flex-direction: row;
    align-items: center;
    justify-content: space-around;
    gap: 2px;
  }

  .side-link {
    min-width: 58px;
    min-height: 52px;
    margin: 0;
    padding: 5px 8px;
    flex: 1;
    flex-direction: column;
    justify-content: center;
    gap: 2px;
    border-radius: 8px;
    font-size: 11px;
  }

  .side-link.active {
    box-shadow: none;
    background: transparent;
  }

  .side-link .icon {
    min-width: 0;
    margin: 0;
  }

  .side-link .text {
    display: block;
    width: auto;
    opacity: 1;
  }

  .notification-badge {
    position: absolute;
    top: 2px;
    margin-left: 23px;
    min-width: 17px;
    height: 17px;
    padding: 0 4px;
    font-size: 9px;
    line-height: 17px;
  }

  .login-section,
  .more-section {
    display: none;
  }

  .content {
    margin-left: 0;
    padding: 16px 12px calc(84px + env(safe-area-inset-bottom));
    overflow: visible;
  }

  .categories {
    margin: 0 -12px 16px;
    padding: 0 12px 11px;
    flex-wrap: nowrap;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .categories::-webkit-scrollbar {
    display: none;
  }

  .cat-btn {
    flex: 0 0 auto;
    padding: 6px 11px;
    font-size: 13px;
  }

  .masonry-grid {
    column-count: 2;
    column-gap: 10px;
  }

  .card,
  .skeleton-card {
    margin-bottom: 10px;
    border-radius: 10px;
  }

  .card-content {
    padding: 10px;
  }

  .card-title {
    font-size: 13px;
    line-height: 1.45;
  }

  .card-stats {
    gap: 2px;
  }

  .card-stats .stat {
    gap: 3px;
    font-size: 10px;
  }

  .icon-svg {
    width: 15px;
    height: 15px;
  }

  .name {
    max-width: 58px;
    font-size: 11px;
  }

  .profile-header {
    height: 250px;
    border-radius: 14px;
  }

  .profile-header-overlay {
    padding: 22px 18px;
    border-radius: 14px;
  }

  .profile-header-content {
    align-items: flex-start;
    gap: 16px;
  }

  .profile-main-avatar {
    width: 72px;
    height: 72px;
  }

  .profile-name {
    font-size: 22px;
  }

  .profile-intro {
    margin-bottom: 10px;
  }

  .profile-stats {
    gap: 12px;
    flex-wrap: wrap;
  }

  .profile-tabs {
    gap: 18px;
    overflow-x: auto;
  }

  .search-page-header {
    align-items: flex-start;
  }

  .search-page-title {
    font-size: 21px;
  }

  .search-filter-bar {
    gap: 8px;
  }

  .search-filter-select {
    width: calc(50% - 4px);
  }

  .login-modal {
    width: calc(100vw - 24px);
    max-width: 430px;
    max-height: calc(100vh - 24px);
    padding: 0;
    overflow-y: auto;
  }

  .modal-left {
    display: none;
  }

  .modal-right {
    width: 100%;
    min-width: 0;
    padding: 56px 24px 28px;
  }

  .modal-title {
    display: block;
    margin-bottom: 24px;
    font-size: 24px;
  }

  .input-group {
    width: 100%;
    min-width: 0;
    min-height: 46px;
  }

  .agreement {
    align-items: flex-start;
    line-height: 1.55;
    text-align: left;
  }

  .note-detail-overlay {
    align-items: flex-end;
  }

  .note-detail-modal {
    width: 100vw;
    height: 100dvh;
    border: 0;
    border-radius: 0;
  }

  .note-media-section {
    height: 42dvh;
  }

  .author-header,
  .scrollable-content,
  .interaction-footer {
    padding-left: 16px;
    padding-right: 16px;
  }

  .ai-float-btn {
    right: 14px;
    bottom: calc(78px + env(safe-area-inset-bottom));
    width: 52px;
    height: 52px;
  }
}

@media (max-width: 340px) {
  .masonry-grid {
    column-count: 1;
  }
}
</style>
