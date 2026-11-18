<script setup>
import AppIcon from '@/components/AppIcon.vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getNotificationList,
  readAllNotifications,
  readNotification
} from '@/api/notification'
import { batchQueryNoteItems } from '@/api/note'
import { getFileUrl } from '@/utils/utils'

const emit = defineEmits(['unread-change'])
const router = useRouter()

const notifications = ref([])
const cursor = ref(null)
const hasNext = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const markingAll = ref(false)

const TYPE_NOTE_LIKE = 2
const TYPE_NOTE_COLLECT = 3
const TYPE_NOTE_DOWNED = 4
const NOTE_STATUS_NORMAL = 1
const NOTE_VISIBLE_PUBLIC = 0

const localUnread = computed(() => notifications.value.filter(isUnread).length)

const isUnread = (item) => item?.isRead === false || item?.read === false
const isNoteNotification = (item) => {
  const type = Number(item?.type)
  return type === TYPE_NOTE_LIKE || type === TYPE_NOTE_COLLECT
}
const isDownedNotification = (item) => Number(item?.type) === TYPE_NOTE_DOWNED
const hasNoteTarget = (item) => isNoteNotification(item) || isDownedNotification(item)

const interactionText = (item) => {
  const type = Number(item?.type)
  if (type === TYPE_NOTE_LIKE) return '赞了你的笔记'
  if (type === TYPE_NOTE_COLLECT) return '收藏了你的笔记'
  return '关注了你'
}

const interactionMark = (item) => {
  const type = Number(item?.type)
  if (type === TYPE_NOTE_LIKE) return 'heart'
  if (type === TYPE_NOTE_COLLECT) return 'bookmark'
  return 'plus'
}

const enrichNoteNotifications = async (rows) => {
  const noteIds = [...new Set(rows
    .filter(item => isNoteNotification(item) && item?.noteId)
    .map(item => String(item.noteId)))]
  if (noteIds.length === 0) return rows

  try {
    const res = await batchQueryNoteItems(noteIds)
    const noteMap = new Map((res?.data || []).map(noteItem => {
      const detail = noteItem?.findNoteDetailResponse
      return [String(detail?.id), detail]
    }))
    return rows.map(item => {
      if (!isNoteNotification(item)) return item
      const detail = noteMap.get(String(item.noteId))
      const available = !!detail
        && Number(detail.status) === NOTE_STATUS_NORMAL
        && Number(detail.visible) === NOTE_VISIBLE_PUBLIC
      return {
        ...item,
        noteTitle: available ? (detail.title || '查看笔记') : '笔记暂不可访问',
        noteCover: available && Array.isArray(detail.imgUris) ? detail.imgUris[0] : null,
        noteAvailable: available,
        noteLookupFailed: false
      }
    })
  } catch (error) {
    console.error('获取通知关联笔记失败:', error)
    return rows.map(item => isNoteNotification(item)
      ? {
          ...item,
          noteTitle: '查看笔记',
          noteCover: null,
          noteAvailable: true,
          noteLookupFailed: true
        }
      : item)
  }
}

const applyResponse = async (res, append) => {
  const rows = (Array.isArray(res?.data) ? res.data : []).map(item =>
    isDownedNotification(item)
      ? {
          ...item,
          noteTitle: item.noteTitle || '未命名笔记',
          noteCover: null,
          noteAvailable: true
        }
      : item)
  const enrichedRows = await enrichNoteNotifications(rows)
  notifications.value = append ? [...notifications.value, ...enrichedRows] : enrichedRows
  cursor.value = res?.nextCursor ?? null
  hasNext.value = !!res?.hasNext
}

const refresh = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getNotificationList({ cursor: null, limit: 20 })
    await applyResponse(res, false)
  } catch (error) {
    console.error('获取通知失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (!hasNext.value || loadingMore.value) return
  loadingMore.value = true
  try {
    const res = await getNotificationList({ cursor: cursor.value, limit: 20 })
    await applyResponse(res, true)
  } catch (error) {
    console.error('加载更多通知失败:', error)
  } finally {
    loadingMore.value = false
  }
}

const markRead = async (item) => {
  if (!isUnread(item)) return
  try {
    await readNotification(item.id)
    item.isRead = true
    item.read = true
    emit('unread-change', -1)
  } catch (error) {
    console.error('标记通知已读失败:', error)
  }
}

const openActorProfile = async (item) => {
  if (!item?.actorId) return
  await markRead(item)
  router.push({
    name: 'user-profile',
    params: { actorId: String(item.actorId) }
  })
}

const openNoteDetail = async (item) => {
  if (!item?.noteId || item.noteAvailable === false) return
  await markRead(item)
  router.push({
    name: 'note-detail',
    params: { id: String(item.noteId) }
  })
}

const markAllRead = async () => {
  if (markingAll.value || localUnread.value === 0) return
  markingAll.value = true
  try {
    await readAllNotifications()
    notifications.value.forEach(item => {
      item.isRead = true
      item.read = true
    })
    emit('unread-change', 'reset')
    ElMessage.success('已全部标为已读')
  } catch (error) {
    console.error('全部已读失败:', error)
  } finally {
    markingAll.value = false
  }
}

const formatTime = (value) => {
  if (!value) return ''
  const date = new Date(String(value).replace(' ', 'T'))
  const seconds = Math.floor((Date.now() - date.getTime()) / 1000)
  if (!Number.isFinite(seconds) || seconds < 0) return String(value).replace('T', ' ').slice(0, 16)
  if (seconds < 60) return '刚刚'
  if (seconds < 3600) return `${Math.floor(seconds / 60)} 分钟前`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)} 小时前`
  if (seconds < 604800) return `${Math.floor(seconds / 86400)} 天前`
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(refresh)
defineExpose({ refresh })
</script>

<template>
  <section class="notification-page">
    <header class="notification-header">
      <div>
        <h1>消息中心</h1>
        <p class="notification-subtitle">互动消息和平台通知会出现在这里</p>
      </div>
      <button
        class="read-all-btn"
        :disabled="markingAll || localUnread === 0"
        @click="markAllRead">
        {{ markingAll ? '处理中...' : '全部已读' }}
      </button>
    </header>

    <div v-if="loading" class="notification-state">
      <span class="notification-spinner"></span>
      正在加载通知...
    </div>

    <div v-else-if="notifications.length === 0" class="notification-empty">
      <div class="empty-bell" aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 8.5h18C21 16 18 16 18 9Z"></path><path d="M10 21h4"></path></svg>
      </div>
      <h3>还没有通知</h3>
      <p>新的互动消息或平台通知会第一时间显示在这里</p>
    </div>

    <div v-else class="notification-list">
      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ unread: isUnread(item), system: isDownedNotification(item) }"
        role="button"
        tabindex="0"
        @keydown.enter="markRead(item)"
        @keydown.space.prevent="markRead(item)"
        @click="markRead(item)">
        <div v-if="isDownedNotification(item)" class="avatar-wrap system-avatar" aria-hidden="true">
          <span class="notification-avatar avatar-fallback">系</span>
          <span class="interaction-mark moderation"><AppIcon name="shield" :size="12" /></span>
        </div>
        <button
          v-else
          type="button"
          class="avatar-wrap avatar-link"
          :aria-label="`查看 ${item.actorNickname || '用户'} 的主页`"
          @click.stop="openActorProfile(item)">
          <img
            v-if="item.actorAvatar"
            :src="getFileUrl(item.actorAvatar)"
            :alt="item.actorNickname"
            class="notification-avatar" />
          <span v-else class="notification-avatar avatar-fallback">
            {{ item.actorNickname?.slice(0, 1) || '用' }}
          </span>
          <span
            class="interaction-mark"
            :class="{
              like: Number(item.type) === TYPE_NOTE_LIKE,
              collect: Number(item.type) === TYPE_NOTE_COLLECT
            }">
            <AppIcon :name="interactionMark(item)" :size="12" />
          </span>
        </button>
        <span class="notification-copy">
          <template v-if="isDownedNotification(item)">
            <span class="notification-message system-message">
              你的笔记《{{ item.noteTitle || '未命名笔记' }}》已被平台下架
            </span>
            <span class="notification-reason">原因：{{ item.reason || '未说明' }}</span>
          </template>
          <span v-else class="notification-message">
            <strong>{{ item.actorNickname || '一位用户' }}</strong>
            {{ interactionText(item) }}
          </span>
          <span class="notification-time">{{ formatTime(item.createTime) }}</span>
        </span>
        <div
          v-if="hasNoteTarget(item)"
          class="note-preview"
          :class="{ unavailable: item.noteAvailable === false }">
          <button
            type="button"
            class="note-cover-button"
            :disabled="item.noteAvailable === false"
            :aria-label="item.noteAvailable === false ? '笔记暂不可访问' : `查看笔记：${item.noteTitle || '笔记详情'}`"
            @click.stop="openNoteDetail(item)">
            <img
              v-if="item.noteCover"
              :src="getFileUrl(item.noteCover)"
              :alt="item.noteTitle || '笔记封面'"
              class="note-cover" />
            <span v-else class="note-cover note-cover-placeholder">
              {{ isDownedNotification(item) ? '查看' : '笔记' }}
            </span>
          </button>
          <span class="note-preview-title">{{ item.noteTitle || '查看笔记' }}</span>
        </div>
        <span v-if="isUnread(item)" class="unread-dot" aria-label="未读"></span>
      </div>
    </div>

    <button v-if="hasNext" class="load-more-notification" :disabled="loadingMore" @click="loadMore">
      {{ loadingMore ? '加载中...' : '加载更多' }}
    </button>
  </section>
</template>

<style scoped>
.notification-page {
  width: min(760px, 100%);
  margin: 28px auto 80px;
}

.notification-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 30px 32px;
  color: #fff;
  border-radius: 22px;
  background: linear-gradient(135deg, #173a67 0%, #2b5fa5 56%, #56a5c8 100%);
  box-shadow: 0 18px 45px rgba(28, 72, 124, 0.2);
}

.notification-eyebrow {
  margin: 0 0 8px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  letter-spacing: 0.18em;
}

.notification-header h1 {
  margin: 0;
  font-size: 30px;
}

.notification-subtitle {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.76);
}

.read-all-btn {
  flex: none;
  padding: 10px 18px;
  color: #173a67;
  border: 0;
  border-radius: 999px;
  background: #fff;
  cursor: pointer;
  font-weight: 600;
}

.read-all-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.notification-list {
  margin-top: 20px;
  overflow: hidden;
  border: 1px solid #e7edf4;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 10px 30px rgba(32, 63, 96, 0.07);
}

.notification-item {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  gap: 16px;
  padding: 19px 24px;
  text-align: left;
  border: 0;
  border-bottom: 1px solid #edf1f5;
  background: #fff;
  cursor: pointer;
  transition: background 0.2s ease;
}

.notification-item:last-child { border-bottom: 0; }
.notification-item:hover { background: #f8fbff; }
.notification-item.unread { background: #f2f7fd; }
.notification-item.system { border-left: 3px solid #e59b28; }

.avatar-wrap { position: relative; flex: none; }
.avatar-link {
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
}
.avatar-link:focus-visible {
  outline: 3px solid rgba(43, 95, 165, 0.24);
  outline-offset: 3px;
}
.notification-avatar {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  object-fit: cover;
  color: #fff;
  border-radius: 50%;
  background: #2b5fa5;
  font-size: 19px;
  font-weight: 700;
}

.interaction-mark {
  position: absolute;
  right: -2px;
  bottom: -2px;
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  color: #fff;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #2b5fa5;
  font-size: 13px;
}

.interaction-mark.like { background: #ef5b70; font-size: 11px; }
.interaction-mark.collect { background: #d8952d; font-size: 11px; }
.system-avatar .notification-avatar { background: #d8952d; }
.interaction-mark.moderation { background: #c96f19; font-weight: 800; }

.notification-copy { display: flex; flex: 1; flex-direction: column; gap: 6px; }
.notification-message { color: #465467; font-size: 15px; }
.notification-message strong { color: #172435; }
.system-message { color: #273548; font-weight: 650; }
.notification-reason {
  color: #9a5a16;
  font-size: 14px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}
.notification-time { color: #96a1af; font-size: 13px; }
.note-preview {
  display: flex;
  align-items: center;
  width: 178px;
  min-width: 0;
  gap: 10px;
  padding: 8px;
  text-align: left;
  border: 1px solid #e3e9f0;
  border-radius: 12px;
  background: #f8fafc;
}
.note-preview.unavailable { opacity: 0.65; }
.note-cover-button {
  flex: none;
  padding: 0;
  border: 0;
  border-radius: 9px;
  background: transparent;
  cursor: pointer;
}
.note-cover-button:hover { box-shadow: 0 0 0 3px rgba(43, 95, 165, 0.15); }
.note-cover-button:focus-visible { outline: 3px solid rgba(43, 95, 165, 0.24); }
.note-cover-button:disabled { cursor: not-allowed; }
.note-cover {
  flex: none;
  width: 52px;
  height: 52px;
  object-fit: cover;
  border-radius: 9px;
}
.note-cover-placeholder {
  display: grid;
  place-items: center;
  color: #7b8795;
  background: linear-gradient(135deg, #e7edf4, #d8e2ed);
  font-size: 12px;
}
.note-preview-title {
  display: -webkit-box;
  overflow: hidden;
  color: #465467;
  font-size: 13px;
  line-height: 1.4;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.unread-dot { width: 9px; height: 9px; border-radius: 50%; background: #e84f5f; }

.notification-state,
.notification-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 340px;
  color: #8995a4;
}

.notification-empty { flex-direction: column; }
.notification-empty h3 { margin: 16px 0 6px; color: #334155; }
.notification-empty p { margin: 0; }
.empty-bell { font-size: 46px; filter: grayscale(0.2); }

.notification-spinner {
  width: 18px;
  height: 18px;
  margin-right: 9px;
  border: 2px solid #dbe5f0;
  border-top-color: #2b5fa5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.load-more-notification {
  display: block;
  margin: 22px auto 0;
  padding: 10px 24px;
  color: #2b5fa5;
  border: 1px solid #cbd9e8;
  border-radius: 999px;
  background: #fff;
  cursor: pointer;
}

@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 640px) {
  .notification-header { align-items: flex-start; flex-direction: column; padding: 24px; }
  .notification-item { align-items: flex-start; padding: 16px; }
  .note-preview { width: 70px; justify-content: center; }
  .note-preview-title { display: none; }
}

/* Calm, readable message centre */
.notification-page {
  max-width: 1020px;
  margin: 0 auto;
  color: var(--ymm-text);
}
.notification-header {
  padding: 28px 30px;
  border: 1px solid var(--ymm-line);
  border-bottom: 0;
  border-radius: var(--ymm-radius-lg) var(--ymm-radius-lg) 0 0;
  background: #fff;
}
.notification-header h1 {
  color: var(--ymm-ink);
  font-family: var(--ymm-font-serif);
  font-size: 28px;
}
.notification-subtitle { color: var(--ymm-muted); }
.read-all-btn,
.load-more-notification {
  border: 1px solid var(--ymm-line);
  border-radius: 9px;
  color: var(--ymm-primary);
  background: #fff;
  font-weight: 650;
}
.read-all-btn:hover:not(:disabled),
.load-more-notification:hover:not(:disabled) { background: var(--ymm-primary-soft); }
.notification-list {
  overflow: hidden;
  border-color: var(--ymm-line);
  border-radius: 0 0 var(--ymm-radius-lg) var(--ymm-radius-lg);
  box-shadow: none;
}
.notification-item {
  border-bottom-color: var(--ymm-line-soft);
  background: #fff;
}
.notification-item:hover { background: #f8fafd; }
.notification-item.unread {
  background: #f2f6fc;
  box-shadow: inset 3px 0 0 var(--ymm-primary);
}
.notification-message { color: var(--ymm-text); }
.notification-message strong { color: var(--ymm-ink); }
.notification-time { color: var(--ymm-muted); }
.note-preview {
  border-color: var(--ymm-line);
  border-radius: 10px;
  background: #f8fafd;
}
.note-cover { border-radius: 7px; }
.unread-dot { background: var(--ymm-danger); }
.empty-bell {
  width: 60px;
  height: 60px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: var(--ymm-primary);
  background: var(--ymm-primary-soft);
  filter: none;
}
.empty-bell svg {
  width: 27px;
  height: 27px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.notification-spinner { border-top-color: var(--ymm-primary); }

@media (max-width: 640px) {
  .notification-header { padding: 22px 18px; }
  .notification-header h1 { font-size: 23px; }
  .notification-item { gap: 12px; padding: 16px 14px; }
  .notification-avatar { width: 42px; height: 42px; }
  .notification-message { font-size: 14px; }
}
</style>
