<template>
  <div class="note-management">
    <!-- Header -->
    <header class="note-header">
      <div class="header-left">
        <div class="blue-bar"></div>
        <h2 class="title">笔记管理</h2>
      </div>
      <div class="header-right">
        <div class="search-box">
          <span class="search-icon"><AppIcon name="search" :size="18" /></span>
          <input type="text" placeholder="搜索已发布笔记" v-model="searchQuery" @input="onSearch" />
        </div>
      </div>
    </header>

    <!-- Tabs -->
    <div class="tabs-container">
      <div
        class="tab-item"
        :class="{ active: currentTab === 'all' }"
        @click="switchTab('all')"
      >
        全部笔记({{ tabCounts.all }})
      </div>
      <div
        class="tab-item"
        :class="{ active: currentTab === 'published' }"
        @click="switchTab('published')"
      >
        已发布
      </div>
      <div
        class="tab-item"
        :class="{ active: currentTab === 'auditing' }"
        @click="switchTab('auditing')"
      >
        审核中
      </div>
      <div
        class="tab-item"
        :class="{ active: currentTab === 'rejected' }"
        @click="switchTab('rejected')"
      >
        未通过
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="loading-container">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="loadError" class="error-state">
      <p>加载失败，请检查网络</p>
      <button class="retry-btn" @click="fetchNotes">重试</button>
    </div>

    <!-- Note List -->
    <div v-else-if="filteredNotes.length > 0" class="note-list">
      <div class="note-table-header">
        <span class="col-cover">封面</span>
        <span class="col-title">笔记名称</span>
        <span class="col-time">发布时间</span>
        <span class="col-comment">评论数</span>
        <span class="col-like">点赞数</span>
        <span class="col-collect">收藏数</span>
        <span class="col-action">操作</span>
      </div>
      <div
        v-for="note in filteredNotes"
        :key="note.id"
        class="note-row"
        :class="{ pinned: note.isTop }"
      >
        <div class="col-cover">
          <!-- 视频笔记封面 -->
          <div v-if="note.type === 1" class="video-cover-wrapper">
            <video
              v-if="note.cover"
              :src="getFileUrl(note.cover)"
              class="note-cover video-cover"
              preload="metadata"
              muted
              @error="e => e.target.style.display = 'none'"
            />
            <div v-if="note.cover" class="video-play-icon"><AppIcon name="play" :size="22" /></div>
            <div v-else class="cover-placeholder video-placeholder">
              <svg viewBox="0 0 24 24" width="24" height="24" fill="#999"><path d="M8 5v14l11-7z"/></svg>
            </div>
          </div>
          <!-- 图文笔记封面 -->
          <div v-else class="image-cover-wrapper">
            <img
              v-if="note.cover"
              :src="getFileUrl(note.cover)"
              class="note-cover"
              @error="e => e.target.style.display = 'none'"
            />
            <div v-if="!note.cover" class="cover-placeholder">
              <svg viewBox="0 0 24 24" width="28" height="28" fill="#ccc"><path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/></svg>
            </div>
          </div>
        </div>
        <span class="col-title" :title="note.title">
          <span v-if="note.isTop" class="top-badge">置顶</span>
          <span class="note-title-text">
            <span>{{ note.title || '无标题' }}</span>
            <small v-if="note.status === 0" class="review-pending">审核中</small>
            <small v-if="note.status === 3 && note.reviewReason" class="review-reason">未通过：{{ note.reviewReason }}</small>
          </span>
        </span>
        <span class="col-time">{{ formatTime(note.createTime) }}</span>
        <span class="col-comment count">{{ note.commentCount ?? 0 }}</span>
        <span class="col-like count">{{ note.likeCount ?? 0 }}</span>
        <span class="col-collect count">{{ note.collectCount ?? 0 }}</span>
        <div class="col-action">
          <button class="action-btn edit-btn" title="编辑" @click="openEdit(note, $event)">编辑</button>
          <button class="action-btn delete-btn" title="删除" @click="handleDelete(note)">删除</button>
        </div>
      </div>

      <!-- Infinite Scroll Sentinel -->
      <div ref="sentinelRef" class="sentinel"></div>
      <div v-if="isLoadingMore" class="loading-more">
        <div class="spinner-small"></div>
        <span>加载中...</span>
      </div>
      <div v-else-if="!hasNext && notes.length > 0" class="no-more">没有更多了</div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <div class="empty-image">
        <svg width="160" height="160" viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M120 120 L150 150" stroke="#E0E0E0" stroke-width="12" stroke-linecap="round" />
          <circle cx="90" cy="90" r="50" stroke="#E0E0E0" stroke-width="4" fill="#F9F9F9"/>
          <rect x="70" y="70" width="40" height="40" rx="4" fill="#E3F2FD" />
          <text x="90" y="98" font-family="Arial" font-size="24" font-weight="bold" text-anchor="middle" fill="#3370ff">?</text>
          <circle cx="40" cy="60" r="2" fill="#E0E0E0"/>
          <circle cx="150" cy="50" r="3" fill="#E0E0E0"/>
          <path d="M50 150 Q100 160 150 150" stroke="#F0F0F0" stroke-width="2" fill="none"/>
        </svg>
      </div>
      <p class="empty-text">暂无{{ tabLabel }}的笔记</p>
    </div>

    <!-- Edit Note Modal -->
    <Transition name="dialog-motion" @after-leave="resetDialogOrigin">
      <div v-if="showEditModal" class="modal-overlay" :style="dialogMotionStyle" @click.self="closeEdit">
        <div class="edit-modal dialog-panel">
          <button class="close-btn" aria-label="关闭笔记编辑" @click="closeEdit"><AppIcon name="close" :size="22" /></button>

          <div v-if="isLoadingDetail" class="edit-loading">
            <div class="spinner"></div>
            <p>加载笔记详情...</p>
          </div>

          <template v-else-if="editingNote">
            <div class="edit-header">
              <h2 class="edit-title">编辑笔记</h2>
              <span class="edit-status" :class="'status-' + (editingNote.status || 0)">{{ statusLabel }}</span>
            </div>

            <div class="edit-body">
              <!-- Left: Media Preview & Upload -->
              <div class="edit-media">
                <div v-if="editingNote.type === 1" class="edit-video-wrap">
                  <video v-if="editingNote.videoUri" :src="getFileUrl(editingNote.videoUri)" class="edit-cover-video" controls preload="metadata" />
                  <div v-else class="edit-cover-empty">无视频</div>
                </div>
                <div v-else class="edit-image-wrap" @click="triggerImageUpload">
                  <img v-if="editingNote.imgUris?.length" :src="getFileUrl(editingNote.imgUris[0])" class="edit-cover-img" />
                  <div v-else class="edit-cover-empty">点击更换封面</div>
                  <div class="cover-upload-hint">点击更换</div>
                  <input type="file" ref="imageInput" hidden accept="image/*" @change="handleCoverChange" />
                </div>
                <!-- Image list for multi-image notes -->
                <div v-if="editingNote.type !== 1 && editingNote.imgUris?.length > 1" class="edit-thumbs">
                  <img v-for="(uri,i) in editingNote.imgUris" :key="i" :src="getFileUrl(uri)" class="edit-thumb" />
                </div>
              </div>

              <!-- Right: Editable Form -->
              <div class="edit-info">
                <div class="edit-field">
                  <label>标题</label>
                  <input v-model="editForm.title" class="form-input" placeholder="笔记标题" maxlength="100" />
                </div>
                <div class="edit-field">
                  <label>内容</label>
                  <textarea v-model="editForm.content" class="form-textarea" placeholder="笔记正文内容" maxlength="6000" rows="4"></textarea>
                </div>
                <div class="info-grid">
                  <div class="edit-field">
                    <label>类型</label>
                    <div class="field-value">{{ editForm.type === 1 ? '视频笔记' : '图文笔记' }}</div>
                  </div>
                  <div class="edit-field">
                    <label>所属频道 <span class="required">*</span></label>
                    <select v-model="editForm.channelId" class="form-select" @change="handleChannelChange">
                      <option value="" disabled>请选择频道</option>
                      <option v-for="c in channels" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
                    </select>
                  </div>
                  <div class="edit-field">
                    <label>所属话题 <span class="required">*</span></label>
                    <select v-model="editForm.topicId" class="form-select">
                      <option value="" disabled>请选择话题</option>
                      <option v-for="t in topics" :key="t.id" :value="String(t.id)">{{ t.name }}</option>
                    </select>
                  </div>
                  <div class="edit-field">
                    <label>可见范围</label>
                    <select v-model="editForm.visible" class="form-select">
                      <option :value="0">公开</option>
                      <option :value="1">仅自己可见</option>
                    </select>
                  </div>
                  <div class="edit-field">
                    <label>置顶</label>
                    <select v-model="editForm.isTop" class="form-select">
                      <option :value="false">否</option>
                      <option :value="true">是</option>
                    </select>
                  </div>
                  <div class="edit-field">
                    <label>发布时间</label>
                    <div class="field-value">{{ formatTime(editingNote.updateTime) }}</div>
                  </div>
                  <div class="edit-field">
                    <label>创作者</label>
                    <div class="field-value">{{ editingNote.creatorName || '-' }} (ID: {{ editingNote.creatorId }})</div>
                  </div>
                </div>

                <!-- Stats -->
                <div class="edit-stats">
                  <div class="edit-stat">
                    <span class="stat-num">{{ editingNote.commentCount ?? 0 }}</span>
                    <span class="stat-label">评论</span>
                  </div>
                  <div class="edit-stat">
                    <span class="stat-num">{{ editingNote.likeCount ?? 0 }}</span>
                    <span class="stat-label">点赞</span>
                  </div>
                  <div class="edit-stat">
                    <span class="stat-num">{{ editingNote.collectCount ?? 0 }}</span>
                    <span class="stat-label">收藏</span>
                  </div>
                </div>

                <div class="edit-actions">
                  <button class="edit-cancel-btn" @click="closeEdit">取消</button>
                  <button class="edit-save-btn" :disabled="isSaving" @click="handleSave">{{ isSaving ? '保存中...' : '保存修改' }}</button>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import AppIcon from '@/components/AppIcon.vue'
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { queryMyNotes, getNoteDetail, updateNote, deleteNote } from '@/api/note'
import { getTopics } from '@/api/topic'
import { getChannels } from '@/api/channel'
import { uploadFile } from '@/api/file'
import { getFileUrl } from '@/utils/utils'
import { useDialogMotionOrigin } from '@/utils/dialogMotion'
import { ElMessage, ElMessageBox } from 'element-plus'

const PAGE_SIZE = 10
const { dialogMotionStyle, captureDialogOrigin, resetDialogOrigin } = useDialogMotionOrigin()

const currentTab = ref('all')
const searchQuery = ref('')

// 后端状态映射
const STATUS_MAP = {
  all: null,        // 全部 — 不传status查所有
  published: 1,     // 已发布
  auditing: 0,      // 审核中
  rejected: 3,      // 未通过
}

const TAB_LABELS = {
  all: '全部',
  published: '已发布',
  auditing: '审核中',
  rejected: '未通过',
}

const tabLabel = computed(() => TAB_LABELS[currentTab.value] || '')

// 笔记列表和分页状态
const notes = ref([])
const nextCursor = ref(null)
const hasNext = ref(false)
const isLoading = ref(false)
const isLoadingMore = ref(false)
const loadError = ref(false)

// 各tab数量
const tabCounts = ref({
  all: 0,
  published: 0,
  auditing: 0,
  rejected: 0,
})

// 重置状态
const resetState = () => {
  notes.value = []
  nextCursor.value = null
  hasNext.value = false
  loadError.value = false
}

// 获取笔记列表
const fetchNotes = async () => {
  if (isLoading.value) return
  isLoading.value = true
  loadError.value = false
  resetState()

  try {
    const status = STATUS_MAP[currentTab.value]
    const res = await queryMyNotes({ status, cursor: null, limit: PAGE_SIZE })
    if (res && res.success) {
      const page = res.data
      const pinned = page.pinnedNotes || []
      const normal = page.normalNotes || []
      notes.value = [...pinned, ...normal]
      nextCursor.value = page.nextCursor ?? null
      hasNext.value = page.hasNext ?? false
      if (currentTab.value === 'all') {
        tabCounts.value.all = page.total ?? 0
      }
    } else {
      loadError.value = true
    }
  } catch (e) {
    console.error('获取笔记列表失败:', e)
    loadError.value = true
  } finally {
    isLoading.value = false
    nextTick(() => observeSentinel())
  }
}

// 加载更多（只追加普通笔记）
const loadMore = async () => {
  if (isLoadingMore.value || !hasNext.value) return
  isLoadingMore.value = true

  try {
    const status = STATUS_MAP[currentTab.value]
    const res = await queryMyNotes({ status, cursor: nextCursor.value, limit: PAGE_SIZE })
    if (res && res.success) {
      const page = res.data
      // 加载更多时置顶笔记已在首屏展示，只追加普通笔记
      const normal = page.normalNotes || []
      notes.value = [...notes.value, ...normal]
      nextCursor.value = page.nextCursor ?? null
      hasNext.value = page.hasNext ?? false
    }
  } catch (e) {
    console.error('加载更多失败:', e)
  } finally {
    isLoadingMore.value = false
    nextTick(() => observeSentinel())
  }
}

// Intersection Observer — 滚动加载
const sentinelRef = ref(null)
let observer = null

const observeSentinel = () => {
  if (!observer) return
  if (sentinelRef.value && hasNext.value) {
    observer.observe(sentinelRef.value)
  } else {
    observer.disconnect()
  }
}

onMounted(() => {
  observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting && hasNext.value && !isLoadingMore.value && !isLoading.value) {
      loadMore()
    }
  }, { rootMargin: '100px' })
  fetchNotes()
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
})

// 编辑弹窗
const showEditModal = ref(false)
const editingNote = ref(null)
const isLoadingDetail = ref(false)
const isSaving = ref(false)
const topics = ref([])
const channels = ref([])
const imageInput = ref(null)

const editForm = ref({
  title: '',
  content: '',
  channelId: null,
  topicId: null,
  visible: 0,
  isTop: false,
  type: 0,
  imgUris: [],
  videoUri: '',
})

const STATUS_LABELS = { 0: '待审核', 1: '正常展示', 2: '被删除', 3: '被下架' }
const statusLabel = computed(() => STATUS_LABELS[editingNote.value?.status] || '')

// id→name 映射
const channelMap = computed(() => {
  const m = {}
  channels.value.forEach(c => { m[c.id] = c.name })
  return m
})
const topicMap = computed(() => {
  const m = {}
  topics.value.forEach(t => { m[t.id] = t.name })
  return m
})

const openEdit = async (note, event) => {
  captureDialogOrigin(event)
  showEditModal.value = true
  isLoadingDetail.value = true
  editingNote.value = null
  try {
    const res = await getNoteDetail(note.id)
    if (res && res.success) {
      const detail = res.data
      editingNote.value = {
        ...detail,
        commentCount: note.commentCount ?? 0,
        likeCount: note.likeCount ?? 0,
        collectCount: note.collectCount ?? 0,
        status: detail.status ?? note.status ?? STATUS_MAP[currentTab.value] ?? 1,
      }
      // 初始化表单
      editForm.value = {
        title: detail.title || '',
        content: detail.content || '',
        channelId: detail.channelId != null ? String(detail.channelId) : '',
        topicId: detail.topicId != null ? String(detail.topicId) : '',
        visible: detail.visible ?? 0,
        isTop: detail.isTop ?? false,
        type: detail.type ?? 0,
        imgUris: detail.imgUris || [],
        videoUri: detail.videoUri || '',
      }
      // 加载频道列表 + 对应频道下的话题
      loadChannelsAndTopics(detail.channelId)
    }
  } catch (e) {
    console.error('获取笔记详情失败:', e)
  } finally {
    isLoadingDetail.value = false
  }
}

const loadChannelsAndTopics = async (channelId) => {
  try {
    const res = await getChannels()
    if (res?.success) channels.value = res.data || []
  } catch (e) { /* ignore */ }
  // 加载对应频道下的话题
  if (channelId) {
    try {
      const res = await getTopics(Number(channelId))
      if (res?.success) topics.value = res.data || []
    } catch (e) { /* ignore */ }
  }
}

// 切换频道时重新加载话题
const handleChannelChange = async () => {
  const chId = editForm.value.channelId
  if (!chId || chId === '') {
    topics.value = []
    editForm.value.topicId = ''
    return
  }
  editForm.value.topicId = ''
  try {
    const res = await getTopics(Number(chId))
    if (res?.success) topics.value = res.data || []
  } catch (e) { /* ignore */ }
}

const triggerImageUpload = () => {
  imageInput.value?.click()
}

const handleCoverChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  try {
    const res = await uploadFile(file)
    if (res && res.success) {
      const url = res.data
      const newList = [url, ...(editForm.value.imgUris || [])]
      editForm.value.imgUris = newList
      if (editingNote.value) editingNote.value.imgUris = newList
      ElMessage.success('封面上传成功')
    }
  } catch (e) {
    ElMessage.error('上传失败')
  }
}

const handleSave = async () => {
  if (isSaving.value) return
  if (!editForm.value.title.trim()) { ElMessage.warning('标题不能为空'); return }
  if (!editForm.value.channelId || editForm.value.channelId === '') { ElMessage.warning('请选择所属频道'); return }
  if (!editForm.value.topicId || editForm.value.topicId === '') { ElMessage.warning('请选择所属话题'); return }
  isSaving.value = true
  try {
    const params = {
      id: editingNote.value.id,
      title: editForm.value.title,
      content: editForm.value.content,
      topicId: Number(editForm.value.topicId),
      visible: editForm.value.visible,
      isTop: editForm.value.isTop,
      type: editForm.value.type,
      imgUris: editForm.value.imgUris,
      videoUri: editForm.value.videoUri,
    }
    const res = await updateNote(params)
    if (res && res.success) {
      ElMessage.success('已提交，笔记将重新审核')
      closeEdit()
      fetchNotes()
    }
  } catch (e) {
    console.error('保存失败:', e)
  } finally {
    isSaving.value = false
  }
}

const handleDelete = async (note) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除笔记「${note.title || '无标题'}」吗？删除后不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
  } catch {
    // 用户取消删除
    return
  }

  try {
    const res = await deleteNote(note.id)
    if (res && res.success) {
      ElMessage.success('删除成功')
      fetchNotes()
    }
  } catch (e) {
    console.error('删除笔记失败:', e)
  }
}

const closeEdit = () => {
  showEditModal.value = false
  editingNote.value = null
}

// 切换tab
const switchTab = (tab) => {
  if (currentTab.value === tab) return
  currentTab.value = tab
  searchQuery.value = ''
  fetchNotes()
}

// 客户端搜索过滤（置顶笔记已由后端单独返回，顺序固定）
const filteredNotes = computed(() => {
  if (!searchQuery.value.trim()) return notes.value
  const q = searchQuery.value.trim().toLowerCase()
  return notes.value.filter(n => n.title && n.title.toLowerCase().includes(q))
})

// 搜索debounce
let searchTimer = null
const onSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    // 客户端过滤，无需重新请求
  }, 300)
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  const d = new Date(time)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}
</script>

<style scoped>
.note-management {
  padding: 28px;
  background-color: #fff;
  min-height: 100%;
  box-sizing: border-box;
}

/* Header */
.note-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.blue-bar {
  width: 3px;
  height: 20px;
  background-color: #6588b2;
  border-radius: 2px;
}

.title {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  color: #999;
  font-size: 14px;
  pointer-events: none;
}

.search-box input {
  padding: 8px 12px 8px 32px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  font-size: 14px;
  width: 200px;
  outline: none;
  transition: border-color 0.2s;
  background: #fff;
}

.search-box input:focus {
  border-color: #3370ff;
}

/* Tabs */
.tabs-container {
  display: flex;
  gap: 32px;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 24px;
}

.tab-item {
  padding-bottom: 12px;
  cursor: pointer;
  color: #666;
  font-size: 15px;
  position: relative;
  transition: color 0.2s;
}

.tab-item:hover {
  color: #3370ff;
}

.tab-item.active {
  color: #3370ff;
  font-weight: 500;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: #3370ff;
}

/* Loading */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #999;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #f0f0f0;
  border-top: 3px solid #3370ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Error */
.error-state {
  text-align: center;
  padding: 60px 0;
  color: #f56c6c;
  font-size: 14px;
}

.retry-btn {
  margin-top: 12px;
  padding: 6px 20px;
  border: 1px solid #f56c6c;
  background: transparent;
  color: #f56c6c;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.retry-btn:hover {
  background: #fef0f0;
}

/* Note Table */
.note-table-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #fafbfc;
  border-radius: 8px 8px 0 0;
  border: 1px solid #eef0f2;
  font-size: 13px;
  color: #999;
  font-weight: 500;
}

.note-table-header .col-cover {
  width: 90px;
  flex-shrink: 0;
}

.note-table-header .col-action {
  width: 130px;
  flex-shrink: 0;
  text-align: center;
}

.note-row {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background: #fff;
  border: 1px solid #eef0f2;
  border-top: none;
  transition: background 0.15s;
}

.note-row:last-child {
  border-radius: 0 0 8px 8px;
}

.note-row:hover {
  background: #fafbfc;
}

.note-row.pinned {
  background: #fdf8ed;
}

.note-row.pinned:hover {
  background: #faf3e0;
}

.col-cover {
  width: 90px;
  flex-shrink: 0;
}

.note-cover {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #eef0f2;
  transition: transform 0.2s;
}

.video-cover-wrapper,
.image-cover-wrapper {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 8px;
  overflow: hidden;
}

.video-cover {
  width: 100%;
  height: 100%;
}

.video-play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  pointer-events: none;
  backdrop-filter: blur(2px);
  transition: all 0.2s;
}

.note-row:hover .video-play-icon {
  background: rgba(0, 0, 0, 0.7);
  transform: translate(-50%, -50%) scale(1.1);
}

.note-row:hover .note-cover {
  transform: scale(1.04);
}

.video-placeholder {
  background: #f0f0f0;
}

.cover-placeholder {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  background: #f8f9fa;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #eef0f2;
}

.col-title {
  flex: 1;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.note-title-text { display: flex; flex-direction: column; min-width: 0; overflow: hidden; text-overflow: ellipsis; }
.note-title-text > span, .note-title-text > small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.review-pending { color: #a66b00; margin-top: 4px; }
.review-reason { color: #c73b3b; margin-top: 4px; }

.top-badge {
  flex-shrink: 0;
  display: inline-block;
  padding: 1px 6px;
  font-size: 11px;
  font-weight: 500;
  color: #e6a23c;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 3px;
  line-height: 1.6;
}

.col-time {
  width: 150px;
  flex-shrink: 0;
  font-size: 13px;
  color: #999;
}

.col-comment,
.col-like,
.col-collect {
  width: 72px;
  flex-shrink: 0;
  text-align: center;
}

.count {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

/* Action Buttons */
.col-action {
  width: 130px;
  flex-shrink: 0;
  display: flex;
  gap: 8px;
  justify-content: center;
}

.action-btn {
  padding: 5px 14px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.edit-btn {
  background: #fff;
  color: #3370ff;
  border-color: #3370ff;
}

.edit-btn:hover {
  background: #3370ff;
  color: #fff;
}

.delete-btn {
  background: #fff;
  color: #f56c6c;
  border-color: #f56c6c;
}

.delete-btn:hover {
  background: #f56c6c;
  color: #fff;
}

/* Infinite Scroll Sentinel */
.sentinel {
  height: 1px;
  width: 100%;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 0;
  color: #999;
  font-size: 13px;
}

.spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid #eef0f2;
  border-top: 2px solid #3370ff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.no-more {
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 20px 0;
}

/* Edit Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.edit-modal {
  background: #fff;
  border-radius: 16px;
  width: 880px;
  max-width: 95vw;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  position: relative;
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 20px;
  background: none;
  border: none;
  font-size: 28px;
  color: #999;
  cursor: pointer;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  z-index: 10;
}

.close-btn:hover {
  background: #f5f5f5;
  color: #333;
}

.edit-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #999;
  gap: 16px;
}

.edit-header {
  padding: 28px 32px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.edit-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.edit-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #eef0f2;
}

.edit-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.edit-subtitle {
  font-size: 13px;
  color: #999;
  margin-top: 2px;
  display: block;
}

.edit-status {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-0 { background: #fdf6ec; color: #e6a23c; }
.status-1 { background: #f0f9eb; color: #67c23a; }
.status-2 { background: #fef0f0; color: #f56c6c; }
.status-3 { background: #fef0f0; color: #f56c6c; }

.edit-body {
  display: flex;
  gap: 32px;
  padding: 24px 32px 32px;
}

.edit-media {
  width: 360px;
  flex-shrink: 0;
}

.edit-image-wrap,
.edit-video-wrap {
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  background: #f8f9fa;
  border: 1px solid #eef0f2;
}

.edit-cover-img,
.edit-cover-video {
  width: 100%;
  display: block;
  object-fit: cover;
  max-height: 400px;
}

.edit-cover-empty {
  width: 100%;
  height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  font-size: 14px;
}

.edit-thumbs {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.edit-thumb {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  object-fit: cover;
  border: 2px solid transparent;
  cursor: pointer;
  opacity: 0.6;
  transition: all 0.2s;
}

.edit-thumb.active,
.edit-thumb:hover {
  opacity: 1;
  border-color: #3370ff;
}

.edit-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edit-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.edit-field label {
  font-size: 12px;
  color: #999;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.required {
  color: #f56c6c;
}

.field-value {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.info-grid .edit-field {
  gap: 2px;
}

.info-grid .field-value {
  font-size: 13px;
}

.content-preview {
  max-height: 100px;
  overflow-y: auto;
  background: #fafbfc;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #eef0f2;
  font-size: 13px;
  color: #666;
  white-space: pre-wrap;
}

.edit-stats {
  display: flex;
  gap: 24px;
  padding: 16px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.edit-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.edit-stat .stat-num {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
}

.edit-stat .stat-label {
  font-size: 12px;
  color: #999;
}

.edit-actions {
  padding-top: 8px;
}

.edit-image-wrap {
  cursor: pointer;
  position: relative;
}

.cover-upload-hint {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px;
  background: rgba(0,0,0,0.5);
  color: #fff;
  font-size: 12px;
  text-align: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.edit-image-wrap:hover .cover-upload-hint {
  opacity: 1;
}

.form-input,
.form-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  color: #333;
  outline: none;
  transition: border-color 0.2s;
  background: #fff;
}

.form-input:focus,
.form-select:focus {
  border-color: #3370ff;
}

.form-textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
  color: #333;
  outline: none;
  resize: vertical;
  font-family: inherit;
  transition: border-color 0.2s;
}

.form-textarea:focus {
  border-color: #3370ff;
}

.edit-actions {
  display: flex;
  gap: 12px;
  padding-top: 8px;
}

.edit-cancel-btn {
  flex: 1;
  padding: 12px;
  background: #fff;
  color: #666;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
}

.edit-cancel-btn:hover {
  background: #f5f5f5;
}

.edit-save-btn {
  flex: 2;
  padding: 12px;
  background: #3370ff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.edit-save-btn:hover:not(:disabled) {
  background: #285edb;
}

.edit-save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Empty */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}

.empty-image {
  margin-bottom: 16px;
  opacity: 0.7;
}

.empty-text {
  color: #999;
  font-size: 14px;
}
</style>
